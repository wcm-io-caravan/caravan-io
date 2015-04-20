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
package io.wcm.caravan.io.jsontransform.sink;

import io.wcm.caravan.io.jsontransform.element.JsonElement;

import java.io.Closeable;
import java.io.IOException;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Sinks represent the end of a pipeline and consumes the data flow.
 */
@ConsumerType
public interface Sink extends Closeable {

  /**
   * Takes an JSON element and writes it some where in any format.
   * @param jsonElement JSON element
   * @throws IOException Writing error
   */
  void write(JsonElement jsonElement) throws IOException;

  /**
   * @return True if any output was written
   */
  boolean hasOutput();

}
