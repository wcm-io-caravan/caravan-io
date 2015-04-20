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

import io.wcm.caravan.io.jsontransform.element.JsonElement;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * JSON source using the Jackson streaming parser.
 */
@ProviderType
public final class JacksonStreamSource implements Source {

  private static final Logger LOG = LoggerFactory.getLogger(JacksonStreamSource.class);

  private static final JsonFactory JSON_FACTORY = new JsonFactory();
  private final JsonParser parser;

  private JsonElement current;

  /**
   * @param input The input stream
   * @throws JsonParseException Error reading json
   * @throws IOException Error reading stream
   */
  public JacksonStreamSource(final InputStream input) throws JsonParseException, IOException {
    parser = JSON_FACTORY.createParser(input);
  }

  @Override
  public boolean hasNext() {
    parseJsonElementIfNeeded();
    return current != null;
  }

  @Override
  public JsonElement next() {
    parseJsonElementIfNeeded();
    JsonElement next = current;
    current = null;
    return next;
  }

  private void parseJsonElementIfNeeded() {
    if (current == null) {
      try {
        JsonToken token = parser.nextToken();
        if (token != null) {
          current = parseJsonElement(token);
        }
      }
      catch (IOException ex) {
        LOG.error("Error reading JSON stream", ex);
      }
    }
  }

  private JsonElement parseJsonElement(final JsonToken token) throws IOException {
    String key = parser.getCurrentName();
    if (JsonToken.START_OBJECT.equals(token)) {
      return JsonElement.DEFAULT_START_OBJECT;
    }
    else if (JsonToken.END_OBJECT.equals(token)) {
      return JsonElement.DEFAULT_END_OBJECT;
    }
    else if (JsonToken.START_ARRAY.equals(token)) {
      return JsonElement.DEFAULT_START_ARRAY;
    }
    else if (JsonToken.END_ARRAY.equals(token)) {
      return JsonElement.DEFAULT_END_ARRAY;
    }
    else if (JsonToken.FIELD_NAME.equals(token)) {
      return handleFieldName(key);
    }
    return JsonElement.value(key, parseValue(token));
  }

  private JsonElement handleFieldName(String key) throws IOException, JsonParseException {
    JsonToken next = parser.nextToken();
    if (JsonToken.START_OBJECT.equals(next)) {
      return JsonElement.startObject(key);
    }
    else if (JsonToken.START_ARRAY.equals(next)) {
      return JsonElement.startArray(key);
    }
    else {
      return JsonElement.value(key, parseValue(next));
    }
  }

  private Object parseValue(final JsonToken token) throws IOException {
    if (token.isBoolean()) {
      return parser.getBooleanValue();
    }
    else if (token.isNumeric()) {
      return new BigDecimal(parser.getText());
    }
    else {
      return parser.getText();
    }
  }

  @Override
  public void close() throws IOException {
    parser.close();
  }

}
