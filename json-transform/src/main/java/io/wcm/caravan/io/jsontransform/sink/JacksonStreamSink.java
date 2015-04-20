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

import io.wcm.caravan.io.jsontransform.element.JsonElement;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Writes the JSON stream elements into an output stream.
 */
@ProviderType
public final class JacksonStreamSink implements Sink {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();
  private final JsonGenerator generator;

  /**
   * @param output The output stream to write into
   * @throws IOException Error on writing
   */
  public JacksonStreamSink(final OutputStream output) throws IOException {
    generator = JSON_FACTORY.createGenerator(output);
  }

  @Override
  public void close() throws IOException {
    generator.close();
  }

  @Override
  public void write(JsonElement jsonElement) throws IOException {
    switch (jsonElement.getType()) {
      case START_OBJECT:
        if (jsonElement.getKey() != null) {
          generator.writeObjectFieldStart(convertFieldName(jsonElement.getKey()));
        }
        else {
          generator.writeStartObject();
        }
        break;
      case END_OBJECT:
        generator.writeEndObject();
        break;
      case START_ARRAY:
        if (jsonElement.getKey() != null) {
          generator.writeArrayFieldStart(convertFieldName(jsonElement.getKey()));
        }
        else {
          generator.writeStartArray();
        }
        break;
      case END_ARRAY:
        generator.writeEndArray();
        break;
      default:
        if (jsonElement.getKey() != null) {
          generator.writeFieldName(convertFieldName(jsonElement.getKey()));
        }
        writeValue(jsonElement.getValue());
    }
    generator.flush();
  }

  private void writeValue(final Object value) throws IOException {
    if (value == null) {
      generator.writeNull();
    }
    else if ("false".equals(value)) {
      generator.writeBoolean(false);
    }
    else if ("true".equals(value)) {
      generator.writeBoolean(true);
    }
    else if (value instanceof BigDecimal) {
      generator.writeNumber((BigDecimal)value);
    }
    else {
      generator.writeString((String)value);
    }
  }

  private String convertFieldName(final String name) {
    return name;
  }

  @Override
  public boolean hasOutput() {
    return generator.getOutputContext().getEntryCount() > 0;
  }

}
