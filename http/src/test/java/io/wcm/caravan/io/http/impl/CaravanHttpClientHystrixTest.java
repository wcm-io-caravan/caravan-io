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
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerCommandFactory;
import io.wcm.caravan.io.http.impl.ribbon.SimpleLoadBalancerFactory;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import rx.Observable;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;

public class CaravanHttpClientHystrixTest {

  private static final String SERVICE_NAME = "/test/hystrix/service";
  private static final String HTTP_200_URI = "/request";
  private static final String HTTP_404_URI = "/http/404";
  private static final String HTTP_500_URI = "/http/500";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Rule
  public WireMockRule server = new WireMockRule(0);
  private String host;
  private CaravanHttpClient underTest;

  @Before
  public void setUp() {

    ArchaiusConfig.initialize();
    context.registerInjectActivateService(new SimpleLoadBalancerFactory());
    context.registerInjectActivateService(new LoadBalancerCommandFactory());
    context.registerInjectActivateService(new HttpClientFactoryImpl());
    underTest = context.registerInjectActivateService(new CaravanHttpClientImpl());

    host = "localhost:" + server.port();

    server.stubFor(get(urlEqualTo(HTTP_200_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody("success")
            ));
    server.stubFor(get(urlEqualTo(HTTP_404_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_NOT_FOUND)
            ));
    server.stubFor(get(urlEqualTo(HTTP_500_URI))
        .willReturn(aResponse()
            .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            ));

    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, host)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIES_PROPERTY, 0)
        .put(CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY, 0)
        .put(CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_PROPERTY, 20)
        .put(CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_PROPERTY, 50)
        .build());
  }

  @Test
  public void test_http500() throws InterruptedException {
    // error threshold = 50%, requests = 10/20 => CircuitBreaker needs to be closed
    execRequestsSync(5, HTTP_200_URI);
    execRequestsSync(5, HTTP_500_URI);
    assertFalse(getCircuitBreaker().isOpen());

    // error threshold = 40%, requests = 25/20 => CircuitBreaker needs to be closed
    execRequestsSync(10, HTTP_200_URI);
    execRequestsSync(5, HTTP_500_URI);
    assertFalse(getCircuitBreaker().isOpen());

    // error threshold = 57%, requests = 35/20 => CircuitBreaker needs to be open
    execRequestsSync(10, HTTP_500_URI);
    assertTrue(getCircuitBreaker().isOpen());
  }

  @Test
  public void test_http400() throws InterruptedException {
    execRequestsSync(5, HTTP_404_URI);
    assertEquals(0, getMetrics().getHealthCounts().getErrorCount());
  }

  private void execRequestsSync(final int times, final String url) throws InterruptedException {
    HystrixCommandMetrics metrics = getMetrics();
    long before = metrics == null ? 0 : metrics.getHealthCounts().getTotalRequests();
    for (int i = 0; i < times; i++) {
      try {
        Observable<CaravanHttpResponse> observable = underTest.execute(new CaravanHttpRequestBuilder(SERVICE_NAME).append(url).build());
        observable.toBlocking().single();
      }
      catch (IllegalResponseRuntimeException ex) {
        // nothing to do
      }
    }
    while (getMetrics().getHealthCounts().getTotalRequests() < before + times) {
      Thread.sleep(50);
    }
  }

  private HystrixCommandMetrics getMetrics() {
    HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(SERVICE_NAME);
    return HystrixCommandMetrics.getInstance(commandKey);
  }

  protected HystrixCircuitBreaker getCircuitBreaker() {
    return HystrixCircuitBreaker.Factory.getInstance(HystrixCommandKey.Factory.asKey(SERVICE_NAME));
  }

}
