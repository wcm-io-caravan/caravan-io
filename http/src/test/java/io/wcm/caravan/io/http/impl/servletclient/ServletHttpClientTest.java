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
package io.wcm.caravan.io.http.impl.servletclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.impl.ArchaiusConfig;
import io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class ServletHttpClientTest {

  private static final String SERVICE_ID = "/test/service/id";
  private static final CaravanHttpRequest REQUEST = new CaravanHttpRequestBuilder(SERVICE_ID)
    .append("/path/ro/resource")
    .query("param1", 1)
    .query("param2", 2)
    .build();
  private static final CaravanHttpResponse FALLBACK = new CaravanHttpResponseBuilder()
    .status(200)
    .reason("OK")
    .build();

  @Rule
  public OsgiContext osgiCtx = new OsgiContext();

  @Mock
  private Servlet servlet;

  private ServletHttpClient client;

  @Before
  public void setUp() {
    osgiCtx.registerService(Servlet.class, servlet, ImmutableMap.of("alias", SERVICE_ID));
    client = osgiCtx.registerInjectActivateService(new ServletHttpClient());
    throwExceptionForResponse500(true);
  }

  @Test
  public void hasValidConfiguration_shouldReturnTrueIfServletIsRegistered() {
    assertTrue(client.hasValidConfiguration(SERVICE_ID));
  }

  @Test
  @Ignore("MockBundleContext throws NullPointerException")
  public void hasValidConfiguration_shouldReturnFalseIfNoServletIsRegistered() {
    assertFalse(client.hasValidConfiguration("/unknown/service/id"));
  }

  @Test(expected = NotSupportedByRequestMapperException.class)
  public void execute_shouldThrowNotSupportedExceptionForRequestError() throws ServletException, IOException {
    Mockito.doThrow(new NotSupportedByRequestMapperException()).when(servlet).service(any(), any());
    CaravanHttpResponse response = client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals(FALLBACK, response);
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void execute_shouldThrowRequestFailedRuntimeExceptionForServerError() throws ServletException, IOException {
    Mockito.doThrow(new ServletException()).when(servlet).service(any(), any());
    CaravanHttpResponse response = client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals(FALLBACK, response);
  }

  @Test
  public void execute_shouldReturnResponse() throws ServletException, IOException {
    mockResponse("Some content", 200);
    CaravanHttpResponse response = client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals("Some content", response.body().asString());
  }

  @Test(expected = IllegalResponseRuntimeException.class)
  public void execute_shouldIllegalResponseRuntimeExceptionForStatus500() throws ServletException, IOException {
    mockResponse("", 500);
    client.execute(REQUEST).toBlocking().single();
  }

  @Test
  public void execute_shouldNotThrowExceptionForStatus500BecauseOfConfiguration() throws ServletException, IOException {
    mockResponse("", 500);
    // service configuration: disable exception for response 500
    throwExceptionForResponse500(false);
    CaravanHttpResponse response = client.execute(REQUEST).toBlocking().single();
    assertThat(response.status(), CoreMatchers.equalTo(500));
  }

  private void mockResponse(String responseBody, int responseStatus) throws ServletException, IOException {
    Mockito.doAnswer(invocation -> {
      HttpServletResponse response = invocation.getArgument(1);
      response.setStatus(responseStatus);
      response.getWriter().append(responseBody).flush();
      return null;
    }).when(servlet).service(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private void throwExceptionForResponse500(boolean value) {
    ArchaiusConfig.getConfiguration().setProperty(SERVICE_ID + CaravanHttpServiceConfig.THROW_EXCEPTION_FOR_STATUS_500, value);
  }

}
