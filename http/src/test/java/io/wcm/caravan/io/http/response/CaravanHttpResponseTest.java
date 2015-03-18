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
package io.wcm.caravan.io.http.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.io.http.response.CaravanHttpResponse.Body;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import joptsimple.internal.Strings;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;


public class CaravanHttpResponseTest {

  @Test
  public void test_getHeaderAsMap() throws Exception {
    ImmutableListMultimap<String, String> headers = ImmutableListMultimap.<String, String>builder()
        .putAll("Cache-Control", "public", "max-age: 1", "no-cache").build();
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", headers, new byte[0]);

    assertEquals(3, response.headers().size());

    Map<String, Object> cacheControl = response.getHeaderAsMap("Cache-Control");
    assertTrue((Boolean)cacheControl.get("public"));
    assertFalse(cacheControl.containsKey("private"));
    assertEquals("1", cacheControl.get("max-age"));

    assertTrue(response.getHeaderAsMap("not-found").isEmpty());
  }

  @Test
  public void test_ByteArrayBody() throws IOException {
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", ImmutableMultimap.of(), "BODY".getBytes(Charsets.UTF_8));
    Body body = response.body();
    assertTrue(body.isRepeatable());
    assertEquals("BODY", body.asString());
  }

  @Test
  public void test_InputStreamBody() throws IOException {
    byte[] content = "BODY".getBytes(Charsets.UTF_8);
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", ImmutableMultimap.of(), new ByteArrayInputStream(content), content.length);
    Body body = response.body();
    assertFalse(body.isRepeatable());
    assertEquals("BODY", body.asString());
    assertEquals(new Integer(content.length), body.length());
    assertEquals(Strings.EMPTY, IOUtils.toString(body.asInputStream()));
    assertEquals(Strings.EMPTY, IOUtils.toString(body.asReader()));
  }

  @Test
  public void test_StringBody() throws IOException {
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", ImmutableMultimap.of(), "BODY", Charsets.UTF_8);
    Body body = response.body();
    assertTrue(body.isRepeatable());
    assertEquals("BODY", body.asString());
  }

  @Test(expected = IllegalStateException.class)
  public void test_invalidStatusCode() {
    CaravanHttpResponse.create(100, "?", ImmutableMultimap.of(), new byte[0]);
  }

  @Test
  public void test_status() {
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", ImmutableMultimap.of(), new byte[0]);
    assertEquals(200, response.status());
    assertEquals("OK", response.reason());
  }

  @Test
  public void test_toString() {
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", ImmutableMultimap.of("Header-Control", "public", "Header-Control", "max-age=100"),
        "BODY", Charsets.UTF_8);
    assertEquals("HTTP/1.1 200 OK\nHeader-Control: public, max-age=100\n\nBODY", response.toString());

  }

}
