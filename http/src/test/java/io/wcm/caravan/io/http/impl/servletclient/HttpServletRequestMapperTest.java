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

import org.junit.Before;
import org.junit.Test;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;


public class HttpServletRequestMapperTest {

  private static final String SERVICE_ID = "/test%3F";
  private static final CaravanHttpRequest REQUEST = new CaravanHttpRequestBuilder(SERVICE_ID)
  .append(SERVICE_ID)
  .append("/resource%3f+b/42")
  .query("a", 1)
  .query("c", 2)
  .build();

  private HttpServletRequestMapper mapper;

  @Before
  public void setUp() {
    mapper = new HttpServletRequestMapper(REQUEST);
  }

  @Test
  public void shouldHaveEmptyContext() {
    assertEquals("", mapper.getContextPath());
  }

  @Test
  public void shouldReturnEncodedResourcepathAsPathInfo() {
    assertEquals("/resource?+b/42", mapper.getPathInfo());
  }

  @Test
  public void shouldReturnQueryString() {
    assertEquals("a=1&c=2", mapper.getQueryString());
  }

  @Test
  public void shouldReturnUndecodedRequestUri() {
    assertEquals(SERVICE_ID + "/resource%3f+b/42", mapper.getRequestURI());
  }

  @Test
  public void shouldReturnUndecodedRequestUrl() {
    assertEquals("http://localhost:8080" + SERVICE_ID + "/resource%3f+b/42", mapper.getRequestURL().toString());
  }

  @Test
  public void shouldReturnDecodedServiceIdAsServletPath() {
    assertEquals("/test?", mapper.getServletPath());
  }

}
