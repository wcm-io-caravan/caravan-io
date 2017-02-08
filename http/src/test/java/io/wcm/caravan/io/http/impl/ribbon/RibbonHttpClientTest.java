/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.io.http.impl.ribbon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.netflix.client.ClientException;

import io.wcm.caravan.commons.httpasyncclient.impl.HttpAsyncClientFactoryImpl;
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.impl.ApacheHttpClient;
import io.wcm.caravan.io.http.impl.ArchaiusConfig;
import io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import rx.Observable;


public class RibbonHttpClientTest {

  private static final String SERVICE_NAME = "/test/ribbon/service";
  private static final String HTTP_200_URI = "/request";
  private static final String HTTP_404_URI = "/invalid";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Rule
  public WireMockRule workingServer = new WireMockRule(0);

  @Rule
  public WireMockRule defectServer1 = new WireMockRule(0);

  @Rule
  public WireMockRule defectServer2 = new WireMockRule(0);

  private String defectServer1Host;
  private String defectServer2Host;
  private String workingServerHost;
  private RibbonHttpClient client;

  @Before
  public void setUp() {

    ArchaiusConfig.initialize();
    context.registerInjectActivateService(new SimpleLoadBalancerFactory());
    context.registerInjectActivateService(new LoadBalancerCommandFactory());
    context.registerInjectActivateService(new HttpClientFactoryImpl());
    context.registerInjectActivateService(new HttpAsyncClientFactoryImpl());
    context.registerInjectActivateService(new ApacheHttpClient());
    client = context.registerInjectActivateService(new RibbonHttpClient());

    defectServer1Host = "localhost:" + defectServer1.port();
    defectServer2Host = "localhost:" + defectServer2.port();
    workingServerHost = "localhost:" + workingServer.port();

    workingServer.stubFor(get(urlEqualTo(HTTP_200_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody("success")
            ));
    workingServer.stubFor(get(urlEqualTo(HTTP_404_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_NOT_FOUND)
            ));

    defectServer1.stubFor(get(urlMatching(".*"))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            ));
    defectServer2.stubFor(get(urlMatching(".*"))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            ));
  }

  @Test(expected = ClientException.class)
  public void test_retryOnOneServerThrowing500() throws ClientException {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, defectServer1Host)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIES_PROPERTY, 4)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY, 0)
        .build());
    try {
      client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build()).toBlocking().single();
    }
    catch (Throwable ex) {
      defectServer1.verify(5, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_200_URI)));
      throw (ClientException)ex.getCause();
    }
  }

  @Test(expected = ClientException.class)
  public void test_retryOnMultipleServersThrowing500() throws ClientException {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, Lists.newArrayList(defectServer1Host, defectServer2Host))
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIES_PROPERTY, 0)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY, 9)
        .build());
    try {
      client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build()).toBlocking().single();
    }
    catch (Throwable ex) {
      defectServer1.verify(5, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_200_URI)));
      defectServer2.verify(5, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_200_URI)));
      throw (ClientException)ex.getCause();
    }
  }

  @Test(expected = ClientException.class)
  public void test_retryOnMultipleServerThrowing500WithoutChangingTheServer() throws ClientException {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, Lists.newArrayList(defectServer1Host, defectServer2Host))
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIES_PROPERTY, 4)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY, 0)
        .build());
    try {
      Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build());
      observable.toBlocking().single();
    }
    catch (Throwable ex) {
      int defectServer1count = defectServer1.countRequestsMatching(RequestPattern.everything()).getCount();
      int defectServer2count = defectServer2.countRequestsMatching(RequestPattern.everything()).getCount();
      assertEquals(5, defectServer1count + defectServer2count);
      assertTrue(defectServer1count == 0 || defectServer2count == 0);
      throw (ClientException)ex.getCause();
    }
  }

  @Test
  public void test_retryOnMultipleServersOnlyOne200() {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, Lists.newArrayList(workingServerHost, defectServer1Host, defectServer2Host))
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIES_PROPERTY, 1)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY, 9)
        .build());
    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build());
    CaravanHttpResponse response = observable.toBlocking().single();

    workingServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_200_URI)));
    assertEquals(HttpServletResponse.SC_OK, response.status());
    // the following assertions can fail as Ribbons internal choosing-the-server-strategy can change
    defectServer1.verify(2, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_200_URI)));
    defectServer2.verify(2, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_200_URI)));
  }

  @Test
  public void test_noRetryOn404() {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, Lists.newArrayList(workingServerHost, defectServer1Host, defectServer2Host))
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY, 9)
        .build());
    Observable<CaravanHttpResponse> observable = client.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_404_URI).build());
    CaravanHttpResponse response = observable.toBlocking().single();
    workingServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(HTTP_404_URI)));
    assertEquals(HttpServletResponse.SC_NOT_FOUND, response.status());
  }

}
