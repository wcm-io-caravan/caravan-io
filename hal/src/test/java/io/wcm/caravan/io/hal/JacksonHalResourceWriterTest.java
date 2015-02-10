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
import io.wcm.caravan.io.hal.mapper.ResourceMapper;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


public class JacksonHalResourceWriterTest {

  private static final Dummy DUMMY = new Dummy(1, "dummy1");

  private JacksonHalResourceWriter underTest = new JacksonHalResourceWriter();

  @Test
  public void test_simple() {
    List<Dummy> friends = Lists.newArrayList(new Dummy(3, "dummy3"), new Dummy(4, "dummy4"));
    HalResource resource = new HalResource()
    .setState(DUMMY)
    .setLink("self", new Link("/dummy-service/1"))
    .setLinks("others", ImmutableList.of(new Link("/others/1"), new Link("/others/2")))
    .setEmbeddedResource("friend", new EmbeddedResource(new HalResource().setState(new Dummy(2, "dummy2")).setLink("self", new Link("/dummy-service/2"))))
    .setEmbeddedResource("friends", HalResourceFactory.createEmbeddedResources(friends, new DummyMapper()))
    .addCuri(new CompactUri("documentation", "http://localhost/documentation"));
    ObjectNode json = underTest.toObjectNode(resource);
    System.out.println(json);
    assertEquals(2, json.at("/_embedded/friends").size());
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
      return objectMapper.createObjectNode().put("name", resource.name);
    }

    @Override
    public ObjectNode getResource(Dummy resource) {
      return objectMapper.convertValue(resource, ObjectNode.class);
    }

  }

}
