/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.io.http.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.request.RequestTemplate;
import io.wcm.caravan.io.http.response.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import rx.Observable;
import rx.Observer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

/**
 * Integration tests for HTTP communcation of transport layer.
 */
public class ResilientHttpImplTest {

  private static final String SERVICE_NAME = "testService";

  private static final String HTTP_200_URI = "/http/200";
  private static final String HTTP_404_URI = "/http/404";
  private static final String HTTP_500_URI = "/http/500";
  private static final String CONNECT_TIMEOUT_URI = "/connect/timeout";
  private static final String RESPONSE_TIMEOUT_URI = "/connect/timeout";

  private static final String DUMMY_CONTENT = "Der Jodelkaiser aus dem \u00D6tztal ist wieder daheim.";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Rule
  public WireMockRule wireMock = new WireMockRule(0);


  private String wireMockHost;
  private ResilientHttpServiceConfig serviceConfig;
  private HttpClientFactory httpClientFactory;
  private ResilientHttp underTest;

  @Before
  public void setUp() {

    ArchaiusConfig.initialize();

    wireMockHost = "localhost:" + wireMock.port();

    serviceConfig = context.registerInjectActivateService(new ResilientHttpServiceConfig(), getServiceConfigProperties(wireMockHost));

    httpClientFactory = context.registerInjectActivateService(new HttpClientFactoryImpl());
    underTest = context.registerInjectActivateService(new ResilientHttpImpl());

    // setup wiremock
    wireMock.stubFor(get(urlEqualTo(HTTP_200_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            ));
    wireMock.stubFor(get(urlEqualTo(HTTP_404_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_NOT_FOUND)
            ));
    wireMock.stubFor(get(urlEqualTo(HTTP_500_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            ));
    wireMock.stubFor(get(urlEqualTo(CONNECT_TIMEOUT_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            ));
    wireMock.stubFor(get(urlEqualTo(RESPONSE_TIMEOUT_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            .withFixedDelay(1000)
            ));
  }

  private static ImmutableMap<String, Object> getServiceConfigProperties(String hostAndPort) {
    return ImmutableMap.<String, Object>builder()
        .put(ResilientHttpServiceConfig.SERVICE_NAME_PROPERTY, SERVICE_NAME)
        .put(ResilientHttpServiceConfig.RIBBON_HOSTS_PROPERTY, hostAndPort)
        .build();
  }

  @After
  public void tearDown() {
    MockOsgi.deactivate(underTest);
    MockOsgi.deactivate(httpClientFactory);
    MockOsgi.deactivate(serviceConfig);
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void testWithoutConfig() throws IOException {

    // remove host config - we have to pass a map with a "serviceName" key (the value being ignored), otherwise the
    // ResilientHttpServiceConfig will not be properly deactivated (see VWDBS-1787)
    MockOsgi.deactivate(serviceConfig, getServiceConfigProperties(""));

    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_200_URI).request());
    Response response = observable.toBlocking().single();
    System.out.println(response.body().asString());
  }

  @Test
  public void testHttp200() throws IOException {
    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_200_URI).request());
    Response response = observable.toBlocking().single();
    assertEquals(HttpServletResponse.SC_OK, response.status());
    assertEquals(DUMMY_CONTENT, response.body().asString());
  }

  @Test
  public void testHttp404() {
    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_404_URI).request());
    Response response = observable.toBlocking().single();
    assertEquals(HttpServletResponse.SC_NOT_FOUND, response.status());
  }

  @Test(expected = IllegalResponseRuntimeException.class)
  public void testHttp500() {
    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_500_URI).request());
    observable.toBlocking().single();
  }

  /** used by #testHttpSimultaneousRequests to detect errors */
  private static final class ResponseObserver implements Observer<Response> {

    private final int iteration;

    private Throwable error;

    private static int completedCount;

    private ResponseObserver(int iteration) {
      this.iteration = iteration;
    }

    @Override
    public void onNext(Response t) {

      try {
        assertEquals(HttpServletResponse.SC_OK, t.status());
        assertEquals(DUMMY_CONTENT, t.body().asString());

      }
      catch (IOException ex) {
        error = ex;
      }
    }

    @Override
    public void onCompleted() {
      completedCount++;
    }

    @Override
    public void onError(Throwable e) {
      error = e;

      completedCount++;
    }
  }

  // TODO: this test fails because of the default maximum of 10 simultaneous request enforced by hystrix
  //@Test
  public void testHttpSimultaneousRequests() throws InterruptedException {

    int totalNumRequests = 11;

    List<ResponseObserver> observers = new ArrayList<ResponseObserver>();

    for (int i = 0; i < totalNumRequests; i++) {

      ResponseObserver obs = new ResponseObserver(i);
      observers.add(obs);

      Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_200_URI).request());
      observable.subscribe(obs);
    }

    while (ResponseObserver.completedCount < totalNumRequests) {
      Thread.sleep(50);
    }

    for (ResponseObserver obs : observers) {
      if (obs.error != null) {
        throw new RuntimeException("Request " + obs.iteration + " failed." + obs.error);
      }
    }
  }
}
