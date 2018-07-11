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
package io.wcm.caravan.io.jsontransform.sink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.io.jsontransform.element.JsonElement;


public class JacksonJsonNodeSinkTest {

  @Test
  public void test_simple() throws IOException {
    JacksonJsonNodeSink sink = new JacksonJsonNodeSink(new JsonFactory());
    assertNull(sink.getJsonNode());
    sink.write(JsonElement.DEFAULT_START_OBJECT);
    sink.write(JsonElement.value("key1", "value1"));
    sink.write(JsonElement.startObject("key2"));
    sink.write(JsonElement.value("key3", new BigDecimal("1234.56")));
    sink.write(JsonElement.DEFAULT_END_OBJECT);
    sink.write(JsonElement.startArray("key4"));
    sink.write(JsonElement.value(true));
    sink.write(JsonElement.DEFAULT_START_OBJECT);
    sink.write(JsonElement.value("key5", "value5"));
    sink.write(JsonElement.DEFAULT_END_OBJECT);
    sink.write(JsonElement.DEFAULT_END_ARRAY);
    sink.write(JsonElement.DEFAULT_END_OBJECT);

    JsonNode root = sink.getJsonNode();
    assertEquals("value1", root.get("key1").asText());
    assertEquals(new BigDecimal("1234.56"), root.get("key2").get("key3").decimalValue());
    assertTrue(root.get("key4").get(0).asBoolean());
    assertEquals("value5", root.get("key4").get(1).get("key5").asText());
    sink.close();

  }

}
