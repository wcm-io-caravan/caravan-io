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

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.hal.domain.CompactUri;
import io.wcm.caravan.io.hal.domain.EmbeddedResource;
import io.wcm.caravan.io.hal.domain.HalResource;
import io.wcm.caravan.io.hal.domain.Link;
import io.wcm.caravan.io.hal.mapper.JsonMapper;
import io.wcm.caravan.io.hal.mapper.ResourceMapper;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


public class JacksonHalResourceWriterTest {

  private static final Dummy DUMMY = new Dummy(1, "dummy1");

  private JacksonHalResourceWriter underTest = new JacksonHalResourceWriter();

  @Test
  public void test_simple() {
    HalResource resource = createHalResource();
    ObjectNode json = underTest.toObjectNode(resource);
    assertEquals(2, json.at("/_embedded/friends").size());
  }

  protected HalResource createHalResource() {
    List<Dummy> friends = Lists.newArrayList(new Dummy(3, "dummy3"), new Dummy(4, "dummy4"));
    HalResource resource = new HalResource()
    .setState(DUMMY)
    .setLink("self", new Link("/dummy-service/1"))
    .setLinks("others", ImmutableList.of(new Link("/others/1"), new Link("/others/2")))
    .setEmbeddedResource("friend", new EmbeddedResource(new HalResource().setState(new Dummy(2, "dummy2")).setLink("self", new Link("/dummy-service/2"))))
    .setEmbeddedResource("friends", HalResourceFactory.createEmbeddedResources(friends, new DummyMapper()))
    .addCuri(new CompactUri("documentation", "http://localhost/documentation"));
    return resource;
  }

  @Test
  public void test_performance() throws JsonParseException, JsonMappingException, IOException {
    int runs = 10000;
    String json = "";

    HalResource objectResource = createHalResource();
    long start = System.currentTimeMillis();
    for (int i = 0; i < runs; i++) {
      json = underTest.toString(objectResource);
    }
    double timePerSerializationByObject = Double.valueOf(System.currentTimeMillis() - start) / runs;

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode input = objectMapper.readValue(json.getBytes(), JsonNode.class);
    DummyJsonMapper mapper = new DummyJsonMapper(objectMapper);
    HalResource jsonResource = HalResourceFactory.createResource(input, mapper)
        .setLinks("others", ImmutableList.of(new Link("/others/1"), new Link("/others/2")))
        .setEmbeddedResource("friend", HalResourceFactory.createEmbeddedResource(input.at("/_embedded/friend"), mapper))
        .setEmbeddedResource("friends", HalResourceFactory.createEmbeddedResources(input.at("/_embedded/friends"), mapper));
    start = System.currentTimeMillis();
    for (int i = 0; i < runs; i++) {
      json = underTest.toString(jsonResource);
    }
    double timePerSerializationByJson = Double.valueOf(System.currentTimeMillis() - start) / runs;

    LoggerFactory.getLogger(getClass()).info(
        String.format("Took %fms for object and %fms for json serialization", timePerSerializationByObject, timePerSerializationByJson));
  }

  public static class DummyJsonMapper extends JsonMapper {

    public DummyJsonMapper(final ObjectMapper objectMapper) {
      super(objectMapper, "/test-service/%s", "/id", "/name");
    }

    @Override
    public ObjectNode getEmbeddedResource(JsonNode resource) {
      return super.getEmbeddedResource(resource).put("id", getId(resource));
    }

    @Override
    public ObjectNode getResource(JsonNode resource) {
      return getEmbeddedResource(resource);
    }


  }

  public static class Dummy {

    public int id;
    public String name;

    /**
     * @param id
     * @param name
     */
    public Dummy(int id, String name) {
      this.id = id;
      this.name = name;
    }

  }

  public static class DummyMapper implements ResourceMapper<Dummy, ObjectNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getHref(Dummy resource) {
      return "/test-service/" + resource.id;
    }

    @Override
    public ObjectNode getEmbeddedResource(Dummy resource) {
      return objectMapper.createObjectNode().put("id", resource.id).put("name", resource.name);
    }

    @Override
    public ObjectNode getResource(Dummy resource) {
      return objectMapper.convertValue(resource, ObjectNode.class);
    }

  }

}
