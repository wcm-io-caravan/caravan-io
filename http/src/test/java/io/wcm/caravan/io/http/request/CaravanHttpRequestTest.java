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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;


public class CaravanHttpRequestTest {

  private CaravanHttpRequest request;

  @Before
  public void setUp() {
    Multimap<String, String> headers = ImmutableListMultimap.<String, String>builder()
        .putAll("Cache-Control", "public", "max-age= 100")
        .put(CaravanHttpRequest.CORRELATION_ID_HEADER_NAME, "test-id")
        .build();
    request = new CaravanHttpRequest("service", "GET", "/path?x=1&y=2", headers, "body".getBytes(Charsets.UTF_8), Charsets.UTF_8);
  }

  @Test
  public void testToString() throws Exception {
    assertEquals("GET /path?x=1&y=2 HTTP/1.1\nCache-Control: public, max-age= 100\nX-Caravan-Correlation-Id: test-id\n\nbody", request.toString());
  }

  @Test
  public void testHasParameter() throws Exception {
    assertTrue(request.hasParameter("x"));
    assertFalse(request.hasParameter("z"));
  }

  @Test
  public void testGetCorrelationId() throws Exception {
    assertEquals("test-id", request.getCorrelationId());
  }

}
