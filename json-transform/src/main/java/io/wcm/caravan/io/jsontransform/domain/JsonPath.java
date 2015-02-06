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
package io.wcm.caravan.io.jsontransform.domain;

import java.util.Collections;
import java.util.List;

/**
 * Minimal implementation of the JSON path API.
 */
public class JsonPath {

  private final List<JsonElement> elements;
  private final String breadCrumb;

  /**
   * @param elements Passed elements
   */
  public JsonPath(List<JsonElement> elements) {
    this.elements = Collections.unmodifiableList(elements);
    breadCrumb = getPathAsBuffer().toString();
  }

  private StringBuffer getPathAsBuffer() {
    StringBuffer buffer = new StringBuffer();
    for (JsonElement element : elements) {
      if (JsonElement.DEFAULT_START_OBJECT.equals(element)) {
        buffer.append(buffer.length() == 0 ? "$" : "[*]");
      }
      else {
        if (element.getKey() != null) {
          if (buffer.length() > 0) {
            buffer.append('.');
          }
          buffer.append(element.getKey());
        }
      }
    }
    return buffer;
  }

  /**
   * @param query JSON path query
   * @return True if bread crumb contains query
   */
  public boolean contains(final String query) {
    return breadCrumb.contains(query);
  }

  /**
   * @param query JSON path query
   * @return True if bread crumb starts with query
   */
  public boolean startsWith(final String query) {
    return breadCrumb.startsWith(query);
  }

  /**
   * @param query JSON path query
   * @return True if bread crumb ends with query
   */
  public boolean endsWith(final String query) {
    return breadCrumb.endsWith(query);
  }

  /**
   * @param query JSON path query
   * @return True if bread crumb equals query
   */
  public boolean isEqualTo(final String query) {
    return breadCrumb.equals(query);
  }

  @Override
  public String toString() {
    return breadCrumb;
  }

  /**
   * @return The last passed element in the bread crumb
   */
  public JsonElement getLast() {
    return getLast(0);
  }

  /**
   * @param steps Number of steps
   * @return The top-<code>steps</code> last passed element in the bread crumb
   */
  public JsonElement getLast(int steps) {
    int index = elements.size() - 1 - steps;
    return index >= 0 ? elements.get(index) : null;
  }

  /**
   * @return The number of passed elements in the bread crumb
   */
  public int size() {
    return elements.size();
  }

}
