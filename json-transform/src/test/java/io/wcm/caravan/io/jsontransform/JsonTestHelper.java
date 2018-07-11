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
package io.wcm.caravan.io.jsontransform;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;

import io.wcm.caravan.io.jsontransform.element.JsonElement;
import io.wcm.caravan.io.jsontransform.element.JsonElementType;
import io.wcm.caravan.io.jsontransform.sink.JacksonStreamSink;
import io.wcm.caravan.io.jsontransform.source.JacksonStreamSource;
import io.wcm.caravan.io.jsontransform.source.Source;


public class JsonTestHelper {

  private final Source source;

  public static Source fromJson(String json) throws JsonParseException, IOException {
    return new JacksonStreamSource(new ByteArrayInputStream(json.getBytes()));
  }

  public static String toString(final Source source) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (JacksonStreamSink sink = new JacksonStreamSink(output);) {
      while (source.hasNext()) {
        sink.write(source.next());
      }
    }
    return output.toString();
  }

  public JsonTestHelper(Source processor) {
    this.source = processor;
  }

  public JsonTestHelper assertStartObject() {
    return check(JsonElement.DEFAULT_START_OBJECT);
  }

  public JsonTestHelper assertStartObject(String key) {
    return check(new JsonElement(key, null, JsonElementType.START_OBJECT));
  }

  public JsonTestHelper assertEndObject() {
    return check(JsonElement.DEFAULT_END_OBJECT);
  }

  public JsonTestHelper assertStartArray(String key) {
    return check(new JsonElement(key, null, JsonElementType.START_ARRAY));
  }

  public JsonTestHelper assertEndArray() {
    return check(JsonElement.DEFAULT_END_ARRAY);
  }

  public JsonTestHelper assertValue(Object value) {
    return assertValue(null, value);
  }

  public JsonTestHelper assertValue(String key, Object value) {
    return check(new JsonElement(key, value, JsonElementType.VALUE));
  }

  private JsonTestHelper check(JsonElement element) {
    assertEquals(element, source.next());
    return this;
  }

}
