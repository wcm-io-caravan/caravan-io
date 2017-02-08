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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.commons.httpasyncclient.impl.HttpAsyncClientFactoryImpl;
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerCommandFactory;
import io.wcm.caravan.io.http.impl.ribbon.RibbonHttpClient;
import io.wcm.caravan.io.http.impl.ribbon.SimpleLoadBalancerFactory;
import io.wcm.caravan.io.http.impl.servletclient.ServletHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import rx.Observable;
import rx.Observer;

/**
 * Integration tests for HTTP communcation of transport layer.
 */
public class CaravanHttpClientImplIntegrationTest {

  private static final String SERVICE_NAME = "/test/service";

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
  private CaravanHttpServiceConfig serviceConfig;
  private CaravanHttpClientImpl client;

  @Before
  public void setUp() {
    ArchaiusConfig.initialize();
    ArchaiusConfig.getConfiguration().setProperty(SERVICE_NAME + CaravanHttpServiceConfig.THROW_EXCEPTION_FOR_STATUS_500, true);
    wireMockHost = "localhost:" + wireMock.port();

    context.registerInjectActivateService(new SimpleLoadBalancerFactory());
    serviceConfig = context.registerInjectActivateService(new CaravanHttpServiceConfig(), getServiceConfigProperties(wireMockHost, "auto"));
    context.registerInjectActivateService(new CaravanHttpThreadPoolConfig(),
        ImmutableMap.of(CaravanHttpThreadPoolConfig.THREAD_POOL_NAME_PROPERTY, "default"));

    context.registerInjectActivateService(new LoadBalancerCommandFactory());
    context.registerInjectActivateService(new HttpAsyncClientFactoryImpl());
    context.registerInjectActivateService(new HttpClientFactoryImpl());

    context.registerInjectActivateService(new CaravanHttpClientConfig(),
        Collections.singletonMap(CaravanHttpClientConfig.SERVLET_CLIENT_ENABLED, true));
    context.registerInjectActivateService(new ServletHttpClient());
    context.registerInjectActivateService(new ApacheHttpClient());
    context.registerInjectActivateService(new RibbonHttpClient());

    client = context.registerInjectActivateService(new CaravanHttpClientImpl());

    // setup wiremock
    wireMock.stubFor(get(urlEqualTo(HTTP_200_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)));
    wireMock.stubFor(get(urlEqualTo(HTTP_404_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_NOT_FOUND)));
    wireMock.stubFor(get(urlEqualTo(HTTP_500_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)));
    wireMock.stubFor(get(urlEqualTo(CONNECT_TIMEOUT_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)));
    wireMock.stubFor(get(urlEqualTo(RESPONSE_TIMEOUT_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            .withFixedDelay(1000)));

    assertTrue(client.hasValidConfiguration(SERVICE_NAME));


  }

  private static ImmutableMap<String, Object> getServiceConfigProperties(String hostAndPort, String protocol) {
    return ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, hostAndPort)
        .put(CaravanHttpServiceConfig.PROTOCOL_PROPERTY, protocol)
        .build();
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void testWithoutConfig() {

    // remove host config - service ID is required to clear archaius properties
    MockOsgi.deactivate(serviceConfig, context.bundleContext(), getServiceConfigProperties("", ""));

    assertFalse(client.hasValidConfiguration(SERVICE_NAME));

    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build());
    observable.toBlocking().single();
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void testMissingServiceId() {
    client.execute(new CaravanHttpRequestBuilder().append(HTTP_200_URI).build()).toBlocking().first();
  }

  @Test
  public void testAbsolutUrl() throws IOException {
    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder().append("http://" + wireMockHost + HTTP_200_URI).build());
    CaravanHttpResponse response = observable.toBlocking().first();
    assertEquals(HttpServletResponse.SC_OK, response.status());
    assertEquals(DUMMY_CONTENT, response.body().asString());
  }

  @Test
  public void testHttp200() throws IOException {
    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build());
    CaravanHttpResponse response = observable.toBlocking().single();
    assertEquals(HttpServletResponse.SC_OK, response.status());
    assertEquals(DUMMY_CONTENT, response.body().asString());
  }

  @Test
  public void testHttp404() {
    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_404_URI).build());
    CaravanHttpResponse response = observable.toBlocking().single();
    assertEquals(HttpServletResponse.SC_NOT_FOUND, response.status());
  }

  @Test(expected = IllegalResponseRuntimeException.class)
  public void testHttp500() {
    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_500_URI).build());
    observable.toBlocking().single();
  }

  /** used by #testHttpSimultaneousRequests to count how many requests habe been completed, and detect errors */
  private static final class ResponseObserver implements Observer<CaravanHttpResponse> {

    private Throwable error;
    private AtomicInteger completedCount = new AtomicInteger();

    @Override
    public synchronized void onNext(CaravanHttpResponse t) {
      try {
        assertEquals(HttpServletResponse.SC_OK, t.status());
        assertEquals(DUMMY_CONTENT, t.body().asString());

      }
      catch (IOException ex) {
        error = ex;
      }
    }

    @Override
    public synchronized void onCompleted() {
      completedCount.incrementAndGet();
    }

    @Override
    public synchronized void onError(Throwable e) {
      error = e;
      completedCount.incrementAndGet();
    }
  }

  @Test
  public void testHttpSimultaneousRequests() throws InterruptedException {

    int totalNumRequests = 100;

    ResponseObserver obs = new ResponseObserver();

    for (int i = 0; i < totalNumRequests; i++) {
      Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build());
      observable.subscribe(obs);
    }

    while (obs.completedCount.get() < totalNumRequests) {
      Thread.sleep(50);
    }

    if (obs.error != null) {
      throw new RuntimeException("At least one of the requests failed with an error ", obs.error);
    }
  }
}
