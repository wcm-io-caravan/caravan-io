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
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rx.Observable;

import com.google.common.collect.ImmutableMap;

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
    Mockito.doThrow(new NotSupportedByRequestMapperException()).when(servlet).service(Matchers.any(), Matchers.any());
    CaravanHttpResponse response = client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals(FALLBACK, response);
  }

  @Test(expected = RequestFailedRuntimeException.class)
  public void execute_shouldThrowRequestFailedRuntimeExceptionForServerError() throws ServletException, IOException {
    Mockito.doThrow(new ServletException()).when(servlet).service(Matchers.any(), Matchers.any());
    CaravanHttpResponse response = client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals(FALLBACK, response);
  }

  @Test
  public void execute_shouldReturnResponse() throws ServletException, IOException {

    Mockito.doAnswer(new Answer() {

      @Override
      public Object answer(InvocationOnMock invocation) throws IOException {
        HttpServletResponse response = invocation.getArgumentAt(1, HttpServletResponse.class);
        response.getWriter().append("Some content").flush();
        return null;
      }

    }).when(servlet).service(Matchers.any(), Matchers.any());
    CaravanHttpResponse response = client.execute(REQUEST, Observable.just(FALLBACK)).toBlocking().single();
    assertEquals("Some content", response.body().asString());

  }

}
