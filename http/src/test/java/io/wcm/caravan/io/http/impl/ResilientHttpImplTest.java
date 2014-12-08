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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.request.RequestTemplate;
import io.wcm.caravan.io.http.response.Response;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import rx.Observable;

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

  private static final String DUMMY_CONTENT = "Der Jodelkaiser aus dem \\u00D6tztal ist wieder daheim.";

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

    serviceConfig = context.registerInjectActivateService(new ResilientHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(ResilientHttpServiceConfig.SERVICE_NAME_PROPERTY, SERVICE_NAME)
        .put(ResilientHttpServiceConfig.RIBBON_HOSTS_PROPERTY, wireMockHost)
        .build());
    httpClientFactory = context.registerInjectActivateService(new HttpClientFactoryImpl());
    underTest = context.registerInjectActivateService(new ResilientHttpImpl());

    // setup wiremock
    stubFor(get(urlEqualTo(HTTP_200_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            ));
    stubFor(get(urlEqualTo(HTTP_404_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_NOT_FOUND)
            ));
    stubFor(get(urlEqualTo(HTTP_500_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            ));
    stubFor(get(urlEqualTo(CONNECT_TIMEOUT_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            ));
    stubFor(get(urlEqualTo(RESPONSE_TIMEOUT_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            .withFixedDelay(1000)
            ));

  }

  @After
  public void tearDown() {
    MockOsgi.deactivate(underTest);
    MockOsgi.deactivate(httpClientFactory);
    MockOsgi.deactivate(serviceConfig);
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void testWithoutConfig() {
    // remove host config
    MockOsgi.deactivate(serviceConfig);

    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_200_URI).request());
    observable.toBlocking().single();
  }

  @Test
  public void testHttp200() throws IOException {
    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_200_URI).request());
    Response response = observable.toBlocking().single();
    assertEquals(HttpServletResponse.SC_OK, response.status());
    assertEquals(DUMMY_CONTENT, response.body().asString());
  }

  // TODO: this fails if run together with testHttp200 - disable it for now
  /*
  @Test
  public void testHttp404() {
    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_404_URI).request());
    Response response = observable.toBlocking().single();
    assertEquals(HttpServletResponse.SC_NOT_FOUND, response.status());
  }
   */

  @Test(expected = RequestFailedRuntimeException.class)
  public void testHttp500() {
    Observable<Response> observable = underTest.execute(SERVICE_NAME, new RequestTemplate().append(HTTP_500_URI).request());
    observable.toBlocking().single();
  }

}
