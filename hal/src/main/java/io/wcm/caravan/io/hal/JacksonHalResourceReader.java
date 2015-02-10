/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.io.hal;

import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Stream;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.hal.domain.CompactUri;
import io.wcm.caravan.io.hal.domain.EmbeddedResource;
import io.wcm.caravan.io.hal.domain.HalResource;
import io.wcm.caravan.io.hal.domain.Link;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Jackson based implementation of a {@link HalResourceReader}.
 */
public class JacksonHalResourceReader implements HalResourceReader {

  private final ObjectMapper objectMapper;

  /**
   * Initializes the resource reader with a fresh {@link ObjectMapper}.
   */
  public JacksonHalResourceReader() {
    this(new ObjectMapper());
  }

  /**
   * Initializes the resource reader with the given {@link ObjectMapper}.
   * @param objectMapper The object mapper to use
   */
  public JacksonHalResourceReader(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public HalResource read(final InputStream input) throws IOException {
    JsonNode json = objectMapper.readTree(input);
    return read(json);
  }

  /**
   * Reads the HAL resource directly from a JsonNode
   * @param input The json input
   * @return The new HAL resource
   */
  public HalResource read(final JsonNode input) {
    HalResource resource = new HalResource();
    resource.setState(getState(input));
    setLinks(resource, input);
    setCuries(resource, input);
    setEmbeddedResources(resource, input);
    return resource;
  }

  private Object getState(final JsonNode input) {
    ObjectNode state = objectMapper.createObjectNode();
    getStream(input.fields())
    .filter(e -> !"_links".equals(e.getKey()) && !"_embedded".equals(e.getKey()))
    .forEach(e -> state.set(e.getKey(), e.getValue()));
    return state;
  }

  private void setLinks(final HalResource resource, final JsonNode input) {
    // get all _link children
    getStream(input.path("_links").fields())
    // filter curies
    .filter(e -> !"curies".equals(e.getKey()))
    // extract link and add
    .forEach(e -> resource.setLink(e.getKey(), new Link(e.getValue().get("href").asText())));
  }

  private void setCuries(final HalResource resource, final JsonNode input) {
    getStream(input.at("/_links/curies").iterator())
    .forEach(curi -> resource.addCuri(new CompactUri(curi.get("name").asText(), curi.get("href").asText())));
  }

  private void setEmbeddedResources(final HalResource resource, final JsonNode input) {
    getStream(input.path("_embedded").fields())
    .forEach(e -> {
      JsonNode embedded = e.getValue();
      if (embedded.isArray()) {
        List<HalResource> resources = getStream(embedded.iterator()).map(f -> read(f)).collect(Collectors.toList());
        resource.setEmbeddedResource(e.getKey(), new EmbeddedResource(resources));
      }
      else {
        resource.setEmbeddedResource(e.getKey(), new EmbeddedResource(read(embedded)));
      }
    });
  }

  private <X> Stream<X> getStream(final Iterator<X> iterator) {
    return Streams.of(Lists.newArrayList(iterator));
  }

}
