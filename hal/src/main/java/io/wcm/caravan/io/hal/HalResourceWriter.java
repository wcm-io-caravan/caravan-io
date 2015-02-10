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
package io.wcm.caravan.io.hal;

import io.wcm.caravan.io.hal.domain.HalResource;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Serializes a HAL {@link HalResource} into a JSON string.
 */
public interface HalResourceWriter {

  /**
   * @param output The output stream
   * @param resource Resource to serialize
   * @throws IOException Error on writing JSON to stream
   */
  void write(final OutputStream output, final HalResource resource) throws IOException;

}
