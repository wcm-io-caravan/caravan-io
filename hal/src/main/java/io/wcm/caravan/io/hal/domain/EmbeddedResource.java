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

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Bean representation of an embedded resource.
 */
public class EmbeddedResource {

  private final List<HalResource> resources;
  private final boolean single;

  /**
   * @param resources Resources of the embedded resource
   */
  public EmbeddedResource(final List<HalResource> resources) {
    this(resources, false);
  }

  /**
   * @param resource Only one resource of the embedded resource
   */
  public EmbeddedResource(final HalResource resource) {
    this(ImmutableList.of(resource), true);
  }

  /**
   * @param resources Resources of the embedded resource
   * @param single True if only one resource
   */
  public EmbeddedResource(final List<HalResource> resources, final boolean single) {
    this.single = single;
    this.resources = resources;
  }

  /**
   * @return True if embedded resource has only one resource
   */
  public boolean isSingle() {
    return this.single;
  }

  /**
   * @return the resources
   */
  public List<HalResource> getResources() {
    return this.resources;
  }

}
