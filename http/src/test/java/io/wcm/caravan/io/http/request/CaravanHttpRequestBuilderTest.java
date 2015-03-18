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
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


public class CaravanHttpRequestBuilderTest {

  private CaravanHttpRequestBuilder builder;

  @Before
  public void setUp() {
    builder = new CaravanHttpRequestBuilder();
  }

  @Test
  public void test_append() {
    builder.append("/path{/pathParts*}{?q1,q2}").append("/subPath{?q3}");
    assertEquals("GET /path{/pathParts*}/subPath{?q1,q2,q3} HTTP/1.1\n", builder.toString());
    assertEquals("GET /path/subPath?q1=v1&q3=v3 HTTP/1.1\n", builder.build(ImmutableMap.of("q1", "v1", "q3", "v3")).toString());
  }

  @Test
  public void test_query() {
    builder.append("/path{?q1,q2}").query("q3", Lists.newArrayList("v3.1", "v3.2")).append("/subPath{?q4}");
    assertEquals("GET /path/subPath{?q1,q2}&q3=v3.1,v3.2{&q4} HTTP/1.1\n", builder.toString());
    assertEquals("GET /path/subPath?q3=v3.1,v3.2 HTTP/1.1\n", builder.build().toString());
    assertEquals("GET /path/subPath?q1=v1&q3=v3.1,v3.2&q4=v4 HTTP/1.1\n", builder.build(ImmutableMap.of("q1", "v1", "q4", "v4")).toString());
  }

  @Test
  public void test_method() {
    builder.append("/").method("POST");
    assertEquals("POST", builder.method());
    assertEquals("POST / HTTP/1.1\n", builder.toString());
  }

  @Test
  public void test_body() {
    builder.append("/").body("BODY".getBytes(Charsets.UTF_8), Charsets.UTF_8);
    assertEquals(Charsets.UTF_8, builder.charset());
    assertNull(builder.bodyTemplate());
    assertEquals("BODY", new String(builder.body()));
    assertEquals("GET / HTTP/1.1\n\nBODY", builder.toString());
  }

  @Test
  public void test_bodyTemplate() {
    builder.append("/").body("x={x}");
    assertEquals(Charsets.UTF_8, builder.charset());
    assertNull(builder.body());
    assertEquals("x={x}", new String(builder.bodyTemplate()));
    assertEquals("GET / HTTP/1.1\n\nx={x}", builder.toString());
    assertEquals("GET / HTTP/1.1\n\nx=", builder.build().toString());
    assertEquals("GET / HTTP/1.1\n\nx=v", builder.build(ImmutableMap.of("x", "v")).toString());
  }

  @Test
  public void test_header() {
    builder.append("/").header("Cache-Control", "public").header("Cache-Control", "max-age={maxAge}");
    assertEquals(2, builder.headers().size());
    assertEquals(2, builder.headers().get("Cache-Control").size());
    assertEquals("GET / HTTP/1.1\nCache-Control: public, max-age={maxAge}\n", builder.toString());
    assertEquals("GET / HTTP/1.1\nCache-Control: public, max-age=\n", builder.build().toString());
    assertEquals("GET / HTTP/1.1\nCache-Control: public, max-age=100\n", builder.build(ImmutableMap.of("maxAge", 100)).toString());
  }
}
