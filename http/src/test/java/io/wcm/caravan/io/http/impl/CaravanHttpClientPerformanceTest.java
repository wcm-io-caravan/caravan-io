/*
<dependency>
      <groupId>io.wcm.caravan</groupId>
      <artifactId>io.wcm.caravan.commons.metrics</artifactId>
      <version>0.2.0-SNAPSHOT</version>
      <scope>compile</scope>
    </dependency> * #%L
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
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.common.performance.PerformanceMetrics;
import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

/**
 * Integration tests for HTTP communcation of transport layer.
 */
public class CaravanHttpClientPerformanceTest {

  private static final String SERVICE_NAME = "testService";

  private static final String HTTP_200_URI = "/http/200";

  private static final String DUMMY_CONTENT = "Der Jodelkaiser aus dem \u00D6tztal ist wieder daheim.";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Rule
  public WireMockRule wireMock = new WireMockRule(0);


  private String wireMockHost;
  private CaravanHttpServiceConfig serviceConfig;
  private HttpClientFactory httpClientFactory;
  private CaravanHttpClient underTest;

  @Before
  public void setUp() {

    ArchaiusConfig.initialize();

    wireMockHost = "localhost:" + wireMock.port();

    serviceConfig = context.registerInjectActivateService(new CaravanHttpServiceConfig(), getServiceConfigProperties(wireMockHost, "auto"));

    httpClientFactory = context.registerInjectActivateService(new HttpClientFactoryImpl());
    underTest = context.registerInjectActivateService(new CaravanHttpClientImpl());

    // setup wiremock
    wireMock.stubFor(get(urlEqualTo(HTTP_200_URI))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain;charset=" + CharEncoding.UTF_8)
            .withBody(DUMMY_CONTENT)
            ));

  }

  private static ImmutableMap<String, Object> getServiceConfigProperties(String hostAndPort, String protocol) {
    return ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_NAME_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, hostAndPort)
        .put(CaravanHttpServiceConfig.PROTOCOL_PROPERTY, protocol)
        .build();
  }

  @After
  public void tearDown() {
    MockOsgi.deactivate(underTest);
    MockOsgi.deactivate(httpClientFactory);
    MockOsgi.deactivate(serviceConfig);
  }

  @Ignore
  @Test
  public void testOnNext100() {
    CaravanHttpRequest httpRequest = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();
    Observable<CaravanHttpResponse> observable = underTest.execute(httpRequest);

    observable.doOnNext(new Action1<CaravanHttpResponse>() {

      @Override
      public void call(CaravanHttpResponse t) {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    }).toBlocking().single();

    PerformanceMetrics metrics = httpRequest.getPerformanceMetrics();
    assertTrue(metrics.getTakenTimeByStep() >= 100);
    assertTrue(metrics.getTakenTimeByStep() < 300);
  }

  @Ignore
  @Test
  public void testOnNext10() {
    CaravanHttpRequest httpRequest = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();
    Observable<CaravanHttpResponse> observable = underTest.execute(httpRequest);

    observable.doOnNext(new Action1<CaravanHttpResponse>() {

      @Override
      public void call(CaravanHttpResponse t) {
        try {
          Thread.sleep(10);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    }).toBlocking().single();

    PerformanceMetrics metrics = httpRequest.getPerformanceMetrics();
    assertTrue(metrics.getTakenTimeByStep() >= 10);
    assertTrue(metrics.getTakenTimeByStep() < 200);
  }

  @Ignore
  @Test
  public void testOnSubscribe() {
    CaravanHttpRequest httpRequest = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();
    Observable<CaravanHttpResponse> observable = underTest.execute(httpRequest);

    observable.doOnSubscribe((new Action0() {

      @Override
      public void call() {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    })).toBlocking().single();


    PerformanceMetrics metrics = httpRequest.getPerformanceMetrics();
    assertTrue(metrics.getTakenTimeByStep() >= 0);
    assertTrue(metrics.getTakenTimeByStep() < 200);
  }

  @Ignore
  @Test
  public void testOnTerminate() {
    CaravanHttpRequest httpRequest = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();
    Observable<CaravanHttpResponse> observable = underTest.execute(httpRequest);

    observable.doOnTerminate((new Action0() {

      @Override
      public void call() {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    })).toBlocking().single();

    PerformanceMetrics metrics = httpRequest.getPerformanceMetrics();
    assertTrue(metrics.getTakenTimeByStep() >= 0);
    assertTrue(metrics.getTakenTimeByStep() < 200);
  }

  @Ignore
  @Test
  public void testOnCompleted() {
    CaravanHttpRequest httpRequest = new CaravanHttpRequestBuilder(SERVICE_NAME).append(HTTP_200_URI).build();
    Observable<CaravanHttpResponse> observable = underTest.execute(httpRequest);

    observable.doOnCompleted((new Action0() {

      @Override
      public void call() {
        try {
          Thread.sleep(0);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    })).toBlocking().single();

    PerformanceMetrics metrics = httpRequest.getPerformanceMetrics();
    assertTrue(metrics.getTakenTimeByStep() >= 0);
  }
}
