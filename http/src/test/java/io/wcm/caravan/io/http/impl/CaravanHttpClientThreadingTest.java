/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2016 wcm.io
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
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerCommandFactory;
import io.wcm.caravan.io.http.impl.ribbon.SimpleLoadBalancerFactory;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import rx.Observable;
import rx.schedulers.Schedulers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

/**
 * Some tests to verify in which threads HTTP requests are executed
 * (and if you can control on which thread the repsonse gets emitted
 */
public class CaravanHttpClientThreadingTest {

  private static final String HYSTRIX_THREADPOOL_NAME = "test-threadpool";
  private static final String SERVICE_NAME = "/test/hystrix/threading";
  private static final String HTTP_200_URI = "/request";

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

    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, host)
        .put(CaravanHttpServiceConfig.HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY, HYSTRIX_THREADPOOL_NAME)
        .build());
  }

  @Test
  public void test_responseEmissionOnHystrixThread() throws InterruptedException {

    CaravanHttpRequest request = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();

    // we don't call #observeOn here, so the response should be emitted with the hystrix threadpool
    // that we have specified in the CaravanHttpServiceConfig
    Observable<CaravanHttpResponse> rxResponse = underTest.execute(request);

    Thread emissionThread = subscribeWaitAndGetEmissionThread(rxResponse);

    // check that the response was emitted on a hystrix- thread
    assertTrue(emissionThread.getName().startsWith("hystrix-" + HYSTRIX_THREADPOOL_NAME));
  }

  @Test
  public void test_responseEmissionOnComputationThread() throws InterruptedException {

    CaravanHttpRequest request = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();

    // we want the HTTP request to be executed using the hystrix threadpool, but the response
    // should be emitted on the computations thread
    Observable<CaravanHttpResponse> rxResponse = underTest.execute(request)
        .observeOn(Schedulers.computation());

    Thread emissionThread = subscribeWaitAndGetEmissionThread(rxResponse);

    // check that the response was emitted on a rx computation thread
    assertTrue(emissionThread.getName().toLowerCase().contains("computation"));
  }


  private Thread subscribeWaitAndGetEmissionThread(Observable<CaravanHttpResponse> rxResponse) throws InterruptedException {

    AtomicReference<Thread> observerThread = new AtomicReference<>();
    AtomicReference<Throwable> error = new AtomicReference<>();

    rxResponse.subscribe(
        response -> observerThread.set(Thread.currentThread()),
        ex -> error.set(ex));

    while (observerThread.get() == null && error.get() == null) {
      Thread.sleep(1);
    }

    if (error.get() != null) {
      throw new RuntimeException("The response observable emitted an exception.", error.get());
    }

    return observerThread.get();
  }


}
