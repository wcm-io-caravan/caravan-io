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
package io.wcm.caravan.io.hal;

import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.hal.domain.CompactUri;
import io.wcm.caravan.io.hal.domain.EmbeddedResource;
import io.wcm.caravan.io.hal.domain.HalResource;
import io.wcm.caravan.io.hal.domain.Link;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Jackson based implementation for {@link HalResourceWriter}. Uses {@link ObjectMapper} to convert the generic state
 * objects of the resources.
 */
public class JacksonHalResourceWriter implements HalResourceWriter {

  private final ObjectMapper objectMapper;

  /**
   * Initializes the writer with a fresh {@link ObjectMapper} ignoring null values.
   */
  public JacksonHalResourceWriter() {
    this(new ObjectMapper().setSerializationInclusion(Include.NON_NULL));
  }

  /**
   * Initializes the writer with the given object mapper.
   * @param objectMapper The object mapper to use
   */
  public JacksonHalResourceWriter(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void write(final OutputStream output, final HalResource resource) throws IOException {
    ObjectNode json = toObjectNode(resource);
    objectMapper.writeValue(output, json);
  }

  /**
   * Converts the HAL resource into a JSON object node.
   * @param resource The resource to convert
   * @return JSON object node representation of the given resource
   */
  public ObjectNode toObjectNode(final HalResource resource) {
    ObjectNode jsonResource = stateToObjectNode(resource.getState());
    setLinks(jsonResource, resource.getLinks());
    setCuries(jsonResource, resource.getCuries());
    setEmbeddedResources(jsonResource, resource.getEmbeddedResources());
    return jsonResource;
  }

  /**
   * Converts the HAL resource into a String.
   * @param resource The HAL resource to convert
   * @return JSON representation of the given resource
   */
  public String toString(final HalResource resource) {
    return toObjectNode(resource).toString();
  }

  private ObjectNode stateToObjectNode(final Object state) {
    if (state == null) {
      return objectMapper.createObjectNode();
    }
    return state instanceof ObjectNode ? ((ObjectNode)state) : objectMapper.convertValue(state, ObjectNode.class);
  }

  private void setLinks(final ObjectNode jsonResource, final Map<String, List<Link>> linksMap) {
    if (!linksMap.isEmpty()) {
      ObjectNode jsonLinks = jsonResource.putObject("_links");
      for (Entry<String, List<Link>> relatedLinks : linksMap.entrySet()) {
        String relation = relatedLinks.getKey();
        List<Link> links = relatedLinks.getValue();
        if (links.size() == 1) {
          jsonLinks.set(relation, createJsonLink(links.get(0)));
        }
        else {
          ArrayNode jsonArray = jsonLinks.putArray(relation);
          Streams.of(links).forEach(link -> jsonArray.add(createJsonLink(link)));
        }
      }
    }
  }

  private JsonNode createJsonLink(final Link link) {
    ObjectNode jsonLink = objectMapper.convertValue(link, ObjectNode.class);
    if (!jsonLink.get("templated").asBoolean(true)) {
      jsonLink.remove("templated");
    }
    return jsonLink;
  }

  private void setCuries(final ObjectNode jsonResource, final Collection<CompactUri> curies) {
    if (!curies.isEmpty()) {
      ObjectNode jsonLinks = jsonResource.has("_links") ? (ObjectNode)jsonResource.get("_links") : jsonResource.putObject("_links");
      jsonLinks.set("curies", objectMapper.convertValue(curies, JsonNode.class));
    }
  }

  private void setEmbeddedResources(final ObjectNode jsonResource, final Map<String, EmbeddedResource> embeddedResources) {
    if (!embeddedResources.isEmpty()) {
      ObjectNode jsonEmbedded = jsonResource.putObject("_embedded");
      for (Entry<String, EmbeddedResource> entry : embeddedResources.entrySet()) {
        EmbeddedResource embeddedResource = entry.getValue();
        if (embeddedResource.isSingle() && embeddedResource.getResources().size() == 1) {
          jsonEmbedded.set(entry.getKey(), toObjectNode(embeddedResource.getResources().get(0)));
        }
        else {
          ArrayNode container = jsonEmbedded.putArray(entry.getKey());
          for (HalResource resource : embeddedResource.getResources()) {
            container.add(toObjectNode(resource));
          }
        }
      }
    }
  }
}
