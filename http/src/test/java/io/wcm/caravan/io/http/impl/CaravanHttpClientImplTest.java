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
package io.wcm.caravan.io.http.impl;

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.impl.ribbon.RibbonHttpClient;
import io.wcm.caravan.io.http.impl.servletclient.NotSupportedByRequestMapperException;
import io.wcm.caravan.io.http.impl.servletclient.ServletHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class CaravanHttpClientImplTest {

  private static final String SERVICE_ID = "/test/service";
  private static final CaravanHttpRequest REQUEST = new CaravanHttpRequestBuilder(SERVICE_ID).build();
  private static final CaravanHttpResponse RESPONSE = new CaravanHttpResponseBuilder().status(200).reason("OK").build();
  private static final CaravanHttpResponse FALLBACK = new CaravanHttpResponseBuilder().status(200).reason("OK").build();

  @Rule
  public OsgiContext osgiCtx = new OsgiContext();

  @Mock
  private ServletHttpClient localhostClient;
  @Mock
  private ApacheHttpClient apacheClient;
  @Mock
  private RibbonHttpClient ribbonClient;

  private CaravanHttpClientImpl client;

  @Before
  public void setUp() {
    osgiCtx.registerService(ServletHttpClient.class, localhostClient);
    osgiCtx.registerService(ApacheHttpClient.class, apacheClient);
    osgiCtx.registerService(RibbonHttpClient.class, ribbonClient);
    client = osgiCtx.registerInjectActivateService(new CaravanHttpClientImpl());
  }

  @Test
  public void shouldExecuteApacheClientForRequestsWithoutServiceId() {

    CaravanHttpRequest request = new CaravanHttpRequestBuilder().append("http://localhost/test").build();
    Mockito.when(apacheClient.execute(request)).thenReturn(Observable.just(RESPONSE));
    CaravanHttpResponse response = client.execute(request).toBlocking().single();
    assertEquals(RESPONSE, response);
  }

  @Test
  public void shouldExecuteLocalhostClientForLocalRequests() {
    setLocalclientCanHandleRequest(true);
    setClientResponse(localhostClient, Observable.just(RESPONSE));
    assertEquals(RESPONSE, getResponse());
  }

  @Test
  public void shouldExecuteRibbonClientForNonLocalRequests() {
    setLocalclientCanHandleRequest(false);
    setClientResponse(ribbonClient, Observable.just(RESPONSE));
    assertEquals(RESPONSE, getResponse());
  }

  @Test
  public void shouldExecuteRibbonClientIfLocalClientFailsByNotSupportedError() {

    setLocalclientCanHandleRequest(true);
    setClientResponse(localhostClient, Observable.error(new NotSupportedByRequestMapperException()));
    setClientResponse(ribbonClient, Observable.just(RESPONSE));
    assertEquals(RESPONSE, getResponse());

  }

  @Test
  public void shouldReturnFallbackIfApacheClientFails() {

    CaravanHttpRequest request = new CaravanHttpRequestBuilder().append("http://localhost/test").build();
    Mockito.when(apacheClient.execute(request)).thenReturn(Observable.error(new IllegalStateException()));
    CaravanHttpResponse response = client.execute(request, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals(FALLBACK, response);

  }

  @Test
  public void shouldReturnFallbackIfLocalClientFails() {
    setLocalclientCanHandleRequest(true);
    setClientResponse(localhostClient, Observable.error(new IllegalStateException()));
    assertEquals(FALLBACK, getResponse());
  }

  @Test
  public void shouldReturnFallbackIfRibbonClientFails() {
    setLocalclientCanHandleRequest(false);
    setClientResponse(ribbonClient, Observable.error(new IllegalStateException()));
    assertEquals(FALLBACK, getResponse());
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void shouldMapLocalhostClientErrorToRequestFailedRuntimeException() {
    setLocalclientCanHandleRequest(true);
    setClientResponse(localhostClient, Observable.error(new IllegalStateException()));
    getResponseWithoutFallback();
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void shouldMapRibbonClientErrorToRequestFailedRuntimeException() {
    setLocalclientCanHandleRequest(false);
    setClientResponse(ribbonClient, Observable.error(new IllegalStateException()));
    getResponseWithoutFallback();
  }

  private void setLocalclientCanHandleRequest(boolean canHandle) {
    Mockito.when(localhostClient.hasValidConfiguration(SERVICE_ID)).thenReturn(canHandle);
  }

  private void setClientResponse(CaravanHttpClient subClient, Observable<CaravanHttpResponse> response) {
    Mockito.when(subClient.execute(REQUEST)).thenReturn(response);
  }

  private CaravanHttpResponse getResponse() {
    return client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
  }

  private CaravanHttpResponse getResponseWithoutFallback() {
    return client.execute(REQUEST).toBlocking().single();
  }

}
