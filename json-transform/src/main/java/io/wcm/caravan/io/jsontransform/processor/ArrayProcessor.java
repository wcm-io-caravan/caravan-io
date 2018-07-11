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

import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.io.jsontransform.element.JsonElement;
import io.wcm.caravan.io.jsontransform.element.JsonPath;
import io.wcm.caravan.io.jsontransform.source.Source;

/**
 * Converts elements identified by passed keys to JSON array elements and the children into JSON array values.
 */
@ProviderType
public final class ArrayProcessor extends AbstractPathProcessor {

  private final Set<String> keys;

  /**
   * @param source The JSON stream source to process
   * @param keys Name of the elements to convert
   */
  public ArrayProcessor(final Source source, final Set<String> keys) {
    super(source);
    this.keys = keys;
  }

  @Override
  protected boolean isProcessable(final JsonPath path, final JsonElement element) {
    return isParentElement(path, element) || isChildElement(path);
  }

  private boolean isParentElement(final JsonPath path, final JsonElement element) {
    return keys.contains(element.getKey()) || keys.contains(path.getLast().getKey());
  }

  private boolean isChildElement(final JsonPath path) {
    return path.size() > 1 && keys.contains(path.getLast(1).getKey());
  }

  @Override
  protected void process(final JsonPath path, final JsonElement element) {
    if (isParentElement(path, element)) {
      outputBuffer.add(element.isStartingElement() ? JsonElement.startArray(element.getKey()) : JsonElement.DEFAULT_END_ARRAY);
    }
    else {
      outputBuffer.add(new JsonElement(null, element.getValue(), element.getType()));
    }
  }


}
