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
package io.wcm.caravan.io.hal.mapper;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Extracts resource relevant data from JSON to JSON.
 */
public class JsonMapper implements ResourceMapper<JsonNode, ObjectNode> {

  private final ObjectMapper objectMapper;
  private final String hrefPattern;
  private final JsonPointer idPointer;
  private final JsonPointer namePointer;

  /**
   * @param objectMapper The Jackson object mapper
   * @param hrefPattern The URI pattern
   * @param idPointer JSON path to the ID field
   * @param namePointer JSON path to the name field
   */
  public JsonMapper(final ObjectMapper objectMapper, final String hrefPattern, final String idPointer, final String namePointer) {
    this.objectMapper = objectMapper;
    this.hrefPattern = hrefPattern;
    this.idPointer = JsonPointer.valueOf(idPointer);
    this.namePointer = JsonPointer.valueOf(namePointer);
  }

  @Override
  public String getHref(final JsonNode resource) {
    return String.format(hrefPattern, getId(resource));
  }

  /**
   * @param resource JSON input
   * @return The resource ID
   */
  protected String getId(final JsonNode resource) {
    return resource.at(idPointer).asText();
  }

  @Override
  public ObjectNode getEmbeddedResource(final JsonNode resource) {
    return objectMapper.createObjectNode().put("name", getName(resource));
  }

  /**
   * @param resource JSON input
   * @return The resource name
   */
  protected String getName(final JsonNode resource) {
    return resource.at(namePointer).asText();
  }

  @Override
  public ObjectNode getResource(final JsonNode resource) {
    return objectMapper.convertValue(resource, ObjectNode.class);
  }

  /**
   * @return The HREF pattern
   */
  protected String getHrefPattern() {
    return this.hrefPattern;
  }

}
