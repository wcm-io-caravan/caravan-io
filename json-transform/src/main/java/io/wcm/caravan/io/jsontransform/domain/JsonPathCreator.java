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

import java.util.List;
import java.util.Stack;

import com.google.common.collect.Lists;

/**
 * Creator for {@link JsonPath}s holding the intermediate state of passed elements to create the bread crumb. If a
 * closing element is passed, will return the JSON path with the opening one inside.
 */
public class JsonPathCreator {

  private final Stack<JsonElement> elements = new Stack<JsonElement>();

  /**
   * Adds the given element to the bread crumb and returns the current JSON path.
   * @param element Current passed element
   * @return Current JSON path
   */
  public JsonPath getJsonPathForElement(final JsonElement element) {
    if (element.isStartingElement()) {
      elements.add(element);
      return new JsonPath(Lists.newLinkedList(elements));
    }
    else if (element.isClosingElement()) {
      JsonPath path = new JsonPath(Lists.newLinkedList(elements));
      elements.pop();
      return path;
    }
    List<JsonElement> elementsPath = Lists.newLinkedList(elements);
    elementsPath.add(element);
    return new JsonPath(elementsPath);
  }

  /**
   * @return Current JSON path
   */
  public JsonPath getJsonPath() {
    return new JsonPath(elements);
  }

}
