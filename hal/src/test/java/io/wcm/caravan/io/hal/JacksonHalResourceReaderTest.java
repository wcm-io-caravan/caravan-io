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
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.io.hal.domain.EmbeddedResource;
import io.wcm.caravan.io.hal.domain.HalResource;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JacksonHalResourceReaderTest {

  private final JacksonHalResourceReader underTest = new JacksonHalResourceReader();

  @Test
  public void test_simple() throws JsonProcessingException, IOException {
    String json = "{\"att1\":\"value1\",\"_links\":{\"self\":{\"href\":\"/resource\"},\"more\":{\"href\":\"/more\"}},\"_embedded\":{\"friend\":{\"att2\":\"value2\"},\"friends\":[{\"att3\":\"value3\"},{\"att4\":\"value4\"}]}}";
    HalResource resource = underTest.read(new ObjectMapper().readTree(json));

    assertEquals("value1", ((JsonNode)resource.getState()).get("att1").asText());
    assertEquals(2, resource.getLinks().size());
    EmbeddedResource friend = resource.getEmbeddedResources().get("friend");
    assertTrue(friend.isSingle());
    assertEquals("value2", ((JsonNode)friend.getResources().get(0).getState()).get("att2").asText());
    EmbeddedResource friends = resource.getEmbeddedResources().get("friends");
    assertEquals(2, friends.getResources().size());
  }
}
