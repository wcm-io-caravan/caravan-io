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
package io.wcm.caravan.io.hal.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Bean representation of a HAL resource.
 */
public class HalResource {

  /**
   * The mime content type
   */
  public static final String CONTENT_TYPE = "application/hal+json";

  private Object state;
  private final Map<String, List<Link>> allLinks = Maps.newHashMap();
  private final Map<String, EmbeddedResource> embeddedResources = Maps.newHashMap();
  private final List<CompactUri> curies = Lists.newArrayList();

  /**
   * Sets the payload for the resource. Can be of any type.
   * @param payload The state of the resource
   * @return The HAL resource
   */
  public HalResource setState(final Object payload) {
    this.state = payload;
    return this;
  }

  /**
   * Sets a link for the resource identified by the relation
   * @param relation The link relation
   * @param link The resource link
   * @return The HAL resource
   */
  public HalResource setLink(final String relation, final Link link) {
    return setLinks(relation, ImmutableList.of(link));
  }

  /**
   * Sets links for the resource identified by the relation
   * @param relation The link relation
   * @param links The resource links
   * @return The HAL resource
   */
  public HalResource setLinks(final String relation, final List<Link> links) {
    this.allLinks.put(relation, links);
    return this;
  }

  /**
   * Adds an embedded resource to the HAL resource. Existing embedded resource(s) with same name get overwritten.
   * @param name The name of the embedded resource
   * @param embeddedResource The embedded resource
   * @return The HAL resource
   */
  public HalResource setEmbeddedResource(final String name, final EmbeddedResource embeddedResource) {
    embeddedResources.put(name, embeddedResource);
    return this;
  }

  /**
   * Adds a compact URI for documentation to the HAL resource.
   * @param curi The compact URI
   * @return The HAL resource
   */
  public HalResource addCuri(final CompactUri curi) {
    curies.add(curi);
    return this;
  }

  /**
   * @return the links
   */
  public Map<String, List<Link>> getLinks() {
    return Collections.unmodifiableMap(allLinks);
  }

  /**
   * @return the embeddedResources
   */
  public Map<String, EmbeddedResource> getEmbeddedResources() {
    return Collections.unmodifiableMap(embeddedResources);
  }

  /**
   * @return the state
   */
  public Object getState() {
    return this.state;
  }

  /**
   * @return the curies
   */
  public Collection<CompactUri> getCuries() {
    return Collections.unmodifiableList(curies);
  }

  /**
   * @return The link for the resource
   */
  public Link getResourceLink() {
    return allLinks.containsKey("self") && allLinks.get("self") != null && !allLinks.get("self").isEmpty() ? allLinks.get("self").get(0) : null;
  }

}
