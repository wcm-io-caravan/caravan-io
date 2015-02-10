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

import java.util.regex.Pattern;

/**
 * Bean representation of a compact URI giving documentation to a relation.
 */
public class CompactUri {

  /**
   * Pattern that will hit an RFC 6570 URI template.
   */
  private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("\\{.+\\}");

  private final String name;
  private final String href;
  private final boolean templated;

  /**
   * @param name The name of the compact URI
   * @param href The link to the documentation
   */
  public CompactUri(String name, String href) {
    this.name = name;
    this.href = href;
    this.templated = href != null && URI_TEMPLATE_PATTERN.matcher(href).find();
  }

  /**
   * @return the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return the href
   */
  public String getHref() {
    return this.href;
  }

  /**
   * @return the templated
   */
  public boolean isTemplated() {
    return this.templated;
  }

}
