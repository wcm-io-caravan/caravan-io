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
package io.wcm.caravan.io.jsontransform.processor;

import io.wcm.caravan.io.jsontransform.element.JsonElement;
import io.wcm.caravan.io.jsontransform.element.JsonPath;
import io.wcm.caravan.io.jsontransform.element.JsonPathCreator;
import io.wcm.caravan.io.jsontransform.source.Source;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Helping abstract class recording the bread crumb of passed JSON stream elements
 */
@ConsumerType
public abstract class AbstractPathProcessor extends AbstractBufferingProcessor {

  private final JsonPathCreator pathCreator = new JsonPathCreator();

  /**
   * @param source The JSON stream source to process
   */
  public AbstractPathProcessor(final Source source) {
    super(source);
  }

  /**
   * @param path The current JSON path
   * @param element The current JSON stream element
   * @return True if process with the given parameter should get called
   */
  protected abstract boolean isProcessable(final JsonPath path, final JsonElement element);

  /**
   * Processes the current JSON stream element
   * @param path The current JSON path
   * @param element The current JSON stream element
   */
  protected abstract void process(final JsonPath path, final JsonElement element);

  @Override
  protected void process(final JsonElement element) {
    JsonPath path = pathCreator.getJsonPathForElement(element);
    if (isProcessable(path, element)) {
      process(path, element);
    }
    else {
      outputBuffer.add(element);
    }
  }

  /**
   * Requests the next JSON stream elements from the source until the bread crumb matches the given JSON path. Passed
   * elements don't get processed or stored in the output buffer. They get lost! Calls the <code>contains</code>
   * function to evaluate the query.
   * @param path JSON path query
   * @return The JSON stream element matching the query
   */
  protected JsonElement seekToPath(final String path) {
    while (source.hasNext()) {
      JsonElement element = source.next();
      JsonPath currentPath = pathCreator.getJsonPathForElement(element);
      if (currentPath.contains(path)) {
        return element;
      }
    }
    return null;
  }

  /**
   * Requests the next JSON stream element.
   * @return The next JSON stream element
   */
  protected JsonElement seekToNext() {
    return seekToNext(1);
  }

  /**
   * Request x times the next JSON stream element defined by the <code>steps</code> parameter. Passed
   * elements don't get processed or stored in the output buffer. They get lost!
   * @param steps Number of steps to execute next; must be larger then zero
   * @return Null if source has no more elements
   */
  protected JsonElement seekToNext(final int steps) {
    JsonElement element = null;
    for (int i = 0; i < steps; i++) {
      if (!source.hasNext()) {
        return null;
      }
      element = source.next();
      pathCreator.getJsonPathForElement(element);
    }
    return element;
  }

  /**
   * Requests the next JSON stream elements from the source until the bread crumb matches the given JSON path. Passed
   * elements don't get processed but stored in the output buffer. Calls the <code>contains</code> function to evaluate
   * the query.
   * @param path JSON path query
   * @return The JSON stream element matching the query
   */
  protected JsonElement processToPath(final String path) {
    while (source.hasNext()) {
      JsonElement element = source.next();
      JsonPath currentPath = pathCreator.getJsonPathForElement(element);
      if (currentPath.contains(path)) {
        return element;
      }
      else {
        outputBuffer.add(element);
      }
    }
    return null;
  }

  /**
   * @return The current JSON path
   */
  protected JsonPath getCurrentJsonPath() {
    return pathCreator.getJsonPath();
  }

}
