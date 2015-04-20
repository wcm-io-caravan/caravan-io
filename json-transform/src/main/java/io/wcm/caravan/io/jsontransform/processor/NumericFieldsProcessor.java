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
import io.wcm.caravan.io.jsontransform.element.JsonElementType;
import io.wcm.caravan.io.jsontransform.source.Source;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Converts the values of JSON elements identified by key into {@link BigDecimal}s.
 */
@ProviderType
public final class NumericFieldsProcessor implements Processor {

  private final Source source;
  private final Set<String> numericFields;

  /**
   * @param source The source to get JSON stream elements from
   * @param numericFields Name of fields having a numeric value
   */
  public NumericFieldsProcessor(final Source source, final Set<String> numericFields) {
    this.source = source;
    this.numericFields = numericFields;
  }

  @Override
  public boolean hasNext() {
    return source.hasNext();
  }

  @Override
  public JsonElement next() {
    JsonElement next = source.next();
    if (next != null && numericFields.contains(next.getKey()) && JsonElementType.VALUE.equals(next.getType())) {
      return JsonElement.value(next.getKey(), new BigDecimal(next.getValue().toString()));
    }
    return next;
  }

  @Override
  public void close() throws IOException {
    source.close();
  }

}
