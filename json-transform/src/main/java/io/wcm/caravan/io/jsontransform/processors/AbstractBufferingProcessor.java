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
import java.util.Queue;

import com.google.common.collect.Lists;

/**
 * Helping abstract processor with an output buffer to process multiple incoming JSON stream elements.
 */
public abstract class AbstractBufferingProcessor implements Processor {

  protected final Source source;
  protected final Queue<JsonElement> outputBuffer = Lists.newLinkedList();

  /**
   * @param source The source to get JSON stream elements from
   */
  public AbstractBufferingProcessor(final Source source) {
    this.source = source;
  }

  /**
   * Has to process the incoming JSON stream element.
   * @param element Current JSON element
   */
  protected abstract void process(final JsonElement element);

  @Override
  public boolean hasNext() {
    return !outputBuffer.isEmpty() || source.hasNext();
  }

  @Override
  public JsonElement next() {
    if (!outputBuffer.isEmpty()) {
      return outputBuffer.poll();
    }
    do {
      process(source.next());
    }
    while (outputBuffer.isEmpty() && hasNext());
    return outputBuffer.isEmpty() ? null : outputBuffer.poll();
  }

  @Override
  public void close() throws IOException {
    source.close();
    outputBuffer.clear();
  }

}
