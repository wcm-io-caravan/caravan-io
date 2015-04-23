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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import joptsimple.internal.Strings;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;


public class CaravanHttpResponseBuilderTest {

  private CaravanHttpResponseBuilder builder;

  @Before
  public void setUp() {
    builder = new CaravanHttpResponseBuilder();
  }

  @Test
  public void testBody_ByteArray() throws IOException {
    CaravanHttpResponse response = builder
        .status(200)
        .reason("OK")
        .body("BODY".getBytes(Charsets.UTF_8))
        .build();
    Body body = response.body();
    assertTrue(body.isRepeatable());
    assertEquals("BODY", body.asString());
  }

  @Test
  public void testBody_InputStream() throws IOException {
    byte[] content = "BODY".getBytes(Charsets.UTF_8);
    CaravanHttpResponse response = builder
        .status(200)
        .reason("OK")
        .body(new ByteArrayInputStream(content), content.length)
        .build();
    Body body = response.body();
    assertFalse(body.isRepeatable());
    assertEquals("BODY", body.asString());
    assertEquals(new Integer(content.length), body.length());
    assertEquals(Strings.EMPTY, IOUtils.toString(body.asInputStream()));
    assertEquals(Strings.EMPTY, IOUtils.toString(body.asReader()));
  }

  @Test
  public void testBody_String() throws IOException {
    CaravanHttpResponse response = builder
        .status(200)
        .reason("OK")
        .body("BODY", Charsets.UTF_8)
        .build();
    Body body = response.body();
    assertTrue(body.isRepeatable());
    assertEquals("BODY", body.asString());
  }

}
