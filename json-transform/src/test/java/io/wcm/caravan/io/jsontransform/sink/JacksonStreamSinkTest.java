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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import io.wcm.caravan.io.jsontransform.element.JsonElement;

public class JacksonStreamSinkTest {

  @Test
  public void test_simple() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    JacksonStreamSink sink = new JacksonStreamSink(output);
    assertFalse(sink.hasOutput());
    sink.write(JsonElement.DEFAULT_START_OBJECT);
    sink.write(JsonElement.value("key1", "value1"));
    sink.write(JsonElement.startObject("key2"));
    sink.write(JsonElement.value("key3", "value3"));
    sink.write(JsonElement.DEFAULT_END_OBJECT);
    sink.write(JsonElement.startArray("key3"));
    sink.write(JsonElement.value("value4"));
    sink.write(JsonElement.DEFAULT_START_OBJECT);
    sink.write(JsonElement.value("key5", "value5"));
    sink.write(JsonElement.DEFAULT_END_OBJECT);
    sink.write(JsonElement.DEFAULT_END_ARRAY);
    sink.write(JsonElement.DEFAULT_END_OBJECT);

    sink.close();
    assertEquals("{\"key1\":\"value1\",\"key2\":{\"key3\":\"value3\"},\"key3\":[\"value4\",{\"key5\":\"value5\"}]}", output.toString());
    assertTrue(sink.hasOutput());
  }

}
