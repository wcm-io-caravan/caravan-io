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

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;


public class CaravanHttpRequestTest {

  @Test
  public void test_toString() throws Exception {
    ImmutableListMultimap<String, String> headers = ImmutableListMultimap.<String, String>builder()
        .putAll("Cache-Control", "public", "max-age: 100")
        .build();
    CaravanHttpRequest request = new CaravanHttpRequest("test-service", "POST", "/test-path", headers,
        "{}".getBytes(), Charsets.UTF_8);
    assertEquals("POST /test-path HTTP/1.1\nCache-Control: public, max-age: 100\n\n{}", request.toString());
  }

}
