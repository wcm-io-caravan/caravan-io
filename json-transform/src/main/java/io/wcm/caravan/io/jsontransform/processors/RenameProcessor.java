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
package io.wcm.caravan.io.jsontransform.processors;

import io.wcm.caravan.io.jsontransform.domain.JsonElement;
import io.wcm.caravan.io.jsontransform.sources.Source;

import java.io.IOException;
import java.util.Map;

/**
 * Renames JSON elements by a given mapping.
 */
public class RenameProcessor implements Processor {

  private final Source source;
  private final Map<String, String> nameMapping;

  /**
   * @param source The source to get JSON stream elements from
   * @param nameMapping Mapping from old to new name
   */
  public RenameProcessor(final Source source, Map<String, String> nameMapping) {
    this.source = source;
    this.nameMapping = nameMapping;
  }

  @Override
  public boolean hasNext() {
    return source.hasNext();
  }

  @Override
  public JsonElement next() {
    JsonElement next = source.next();
    if (next != null && nameMapping.containsKey(next.getKey())) {
      String newName = nameMapping.get(next.getKey());
      return new JsonElement(newName, next.getValue(), next.getType());
    }
    return next;
  }

  @Override
  public void close() throws IOException {
    source.close();
  }

}
