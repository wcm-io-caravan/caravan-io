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
import java.math.BigDecimal;
import java.util.Stack;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Converts the JSON stream elements into {@link JsonNode}s.
 */
@ProviderType
public final class JacksonJsonNodeSink implements Sink {

  private final Stack<JsonNode> breadCrumb = new Stack<JsonNode>();
  private final ObjectMapper mapper;
  private JsonNode root;

  /**
   * @param factory JSON factory to initiate the Object Mapper.
   */
  public JacksonJsonNodeSink(final JsonFactory factory) {
    mapper = new ObjectMapper(factory);
  }

  /**
   * @return The root node
   */
  public JsonNode getJsonNode() {
    return root;
  }

  @Override
  public void close() throws IOException {
    // nothing to do
  }

  @Override
  public void write(JsonElement element) throws IOException {
    switch (element.getType()) {
      case START_OBJECT:
        handleStartObject(element);
        break;
      case END_OBJECT:
        breadCrumb.pop();
        break;
      case START_ARRAY:
        handleStartArray(element);
        break;
      case END_ARRAY:
        breadCrumb.pop();
        break;
      case VALUE:
        handleValue(element);
        break;
      default:
        // nothing to do
    }
  }

  private void handleStartObject(final JsonElement element) {
    if (isRootElement(element)) {
      root = mapper.createObjectNode();
      breadCrumb.add(root);
    }
    else if (isObjectProperty(element)) {
      breadCrumb.add(getCurrentObjectNode().putObject(element.getKey()));
    }
    else if (isArrayChild(element)) {
      breadCrumb.add(getCurrentArrayNode().addObject());
    }
  }

  private boolean isRootElement(final JsonElement element) {
    return breadCrumb.isEmpty() && element.getKey() == null;
  }

  private boolean isObjectProperty(final JsonElement element) {
    return breadCrumb.peek() instanceof ObjectNode && element.getKey() != null;
  }

  private boolean isArrayChild(final JsonElement element) {
    return breadCrumb.peek() instanceof ArrayNode;
  }

  private ObjectNode getCurrentObjectNode() {
    return (ObjectNode)breadCrumb.peek();
  }

  private ArrayNode getCurrentArrayNode() {
    return (ArrayNode)breadCrumb.peek();
  }

  private void handleStartArray(final JsonElement element) {
    if (isRootElement(element)) {
      root = mapper.createArrayNode();
      breadCrumb.add(root);
    }
    else if (isObjectProperty(element)) {
      breadCrumb.add(getCurrentObjectNode().putArray(element.getKey()));
    }
    else if (isArrayChild(element)) {
      breadCrumb.add(getCurrentArrayNode().addArray());
    }
  }

  private void handleValue(final JsonElement element) {
    if (isObjectProperty(element)) {
      if (element.getValue() instanceof BigDecimal) {
        getCurrentObjectNode().put(element.getKey(), (BigDecimal)element.getValue());
      }
      else if (element.getValue() instanceof Boolean) {
        getCurrentObjectNode().put(element.getKey(), (Boolean)element.getValue());
      }
      else {
        getCurrentObjectNode().put(element.getKey(), (String)element.getValue());
      }
    }
    else if (isArrayChild(element)) {
      if (element.getValue() instanceof BigDecimal) {
        getCurrentArrayNode().add((BigDecimal)element.getValue());
      }
      else if (element.getValue() instanceof Boolean) {
        getCurrentArrayNode().add((Boolean)element.getValue());
      }
      else {
        getCurrentArrayNode().add((String)element.getValue());
      }
    }
  }

  @Override
  public boolean hasOutput() {
    return root != null;
  }

}
