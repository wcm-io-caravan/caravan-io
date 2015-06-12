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
package io.wcm.caravan.io.http.request;

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


public class CaravanHttpRequestBuilderTest {

  private static final String SERVICE_ID = "test-service";
  private static final String CORRELATION_ID = "123";

  private CaravanHttpRequestBuilder builder;

  @Before
  public void setUp() {
    builder = new CaravanHttpRequestBuilder(SERVICE_ID).correlationId(CORRELATION_ID);
  }

  @Test
  public void testConstructor() {
    CaravanHttpRequest request = builder.build();
    assertEquals(SERVICE_ID, request.getServiceId());
    assertEquals(CORRELATION_ID, request.getCorrelationId());
  }

  @Test
  public void testMethod() {
    assertEquals("GET", builder.build().getMethod());
    assertEquals("POST", builder.method("POST").build().getMethod());
  }

  @Test
  public void testHeaderStringString() {
    assertEquals(ImmutableList.of("value"), builder.header("key", "value").build().getHeaders().get("key"));
  }

  @Test
  public void testHeaderStringString_placeHolder() throws Exception {
    assertEquals(ImmutableList.of("v1"), builder.header("key", "{value}").build(ImmutableMap.of("value", "v1")).getHeaders().get("key"));
  }

  @Test
  public void testHeaderStringCollection() {
    ImmutableList<String> values = ImmutableList.of("value1", "value2");
    assertEquals(values, builder.header("key", values).build().getHeaders().get("key"));
  }

  @Test
  public void testBodyString() {
    CaravanHttpRequest request = builder.body("key={value}").build(ImmutableMap.of("value", "123"));
    String body = new String(request.getBody(), request.getCharset());
    assertEquals("key=123", body);
  }

  @Test
  public void testBodyByteArrayCharset() {
    String content = "key=123";
    CaravanHttpRequest request = builder.body(content.getBytes(), Charsets.UTF_8).build();
    String body = new String(request.getBody(), request.getCharset());
    assertEquals(content, body);
  }

  @Test
  public void testAppend() {
    assertEquals("/path1/path2", builder.append("/path1").append("/path2").build().getUrl());
  }

  @Test
  public void testAppend_path() {
    assertEquals("/xyz/1", builder.append("/xyz{/path}").build(ImmutableMap.of("path", 1)).getUrl());
  }

  @Test
  public void testAppend_query() {
    assertEquals("/path?a=1", builder.append("/path{?a}").build(ImmutableMap.of("a", "1")).getUrl());
  }

  @Test
  public void testAppend_queryEndoded() throws Exception {
    assertEquals("/path?a=" + CaravanHttpHelper.urlEncode("/encoded&"), builder.append("/path{?a}").build(ImmutableMap.of("a", "/encoded&")).getUrl());
  }

  @Test
  public void testAppend_queryWithExistingQuery() {
    assertEquals("/path?a=1&b=2&c=3", builder.append("/path?c=3&b=2{&a}").build(ImmutableMap.of("a", "1")).getUrl());
  }

  @Test
  public void testAppend_continuation() {
    assertEquals("/path?a=1", builder.append("/path{&a}").build(ImmutableMap.of("a", "1")).getUrl());
  }

  @Test
  public void testAppend_multiValues() {
    assertEquals("/path?a=2&a=1", builder.append("/path?a=2&a=1").build().getUrl());
  }

  @Test
  public void testAppend_queryNoValue() {
    assertEquals("/path", builder.append("/path{?a}").build().getUrl());
  }

  @Test
  public void testAppend_queries() {
    assertEquals("/path?a=1&b=2", builder.append("/path{?a}").append("{?b}").build(ImmutableMap.of("a", 1, "b", 2)).getUrl());
  }

  @Test
  public void testAppend_queriesOrder() {
    assertEquals("/path?a=1&b=2", builder.append("/path{?b}").append("{?a}").build(ImmutableMap.of("a", 1, "b", 2)).getUrl());
  }

  @Test
  public void testAppend_encoded() throws Exception {
    String encoded = CaravanHttpHelper.urlEncode("/encoded&");
    assertEquals("/path?encoded=" + encoded, builder.append("/path?encoded=" + encoded).build().getUrl());
  }

  @Test
  public void test_query() {
    builder.append("/path{?q1,q2}").query("q4", Lists.newArrayList("v4.1", "v4.2")).append("/subPath{?q3}");
    assertEquals("/path/subPath?q4=v4.1,v4.2", builder.build().getUrl());
    assertEquals("/path/subPath?q1=v1&q3=v3&q4=v4.1,v4.2", builder.build(ImmutableMap.of("q1", "v1", "q3", "v3")).getUrl());
  }

  @Test
  public void test_complex() {
    String url = builder.append("/path1/path2?doIt=yes&z=z&var=value10&var=value1{&version}").build(ImmutableMap.of("version", 1)).getUrl();
    assertEquals("/path1/path2?doIt=yes&var=value10&var=value1&version=1&z=z", url);
  }

}
