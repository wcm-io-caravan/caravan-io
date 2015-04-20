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
package io.wcm.caravan.io.jsontransform.source;

import io.wcm.caravan.io.jsontransform.JsonTestHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;


public class JacksonStreamSourceTest {

  @Test
  public void test_simple() throws JsonParseException, IOException {
    String json = "{\"key1\":\"value1\",\"obj1\":{\"key2\":2},\"array1\":[\"value3\"]}}";
    JacksonStreamSource source = new JacksonStreamSource(new ByteArrayInputStream(json.getBytes()));
    new JsonTestHelper(source)
    .assertStartObject()
    .assertValue("key1", "value1")
    .assertStartObject("obj1")
    .assertValue("key2", new BigDecimal(2))
    .assertEndObject()
    .assertStartArray("array1")
    .assertValue("value3")
    .assertEndArray()
    .assertEndObject();
  }

}
