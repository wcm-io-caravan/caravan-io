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
package io.wcm.caravan.io.jsontransform.source;

import java.io.Closeable;
import java.util.Iterator;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.io.jsontransform.element.JsonElement;

/**
 * A source is the beginning of a processing pipeline delivering {@link JsonElement}s in an {@link Iterator} fashion.
 */
@ConsumerType
public interface Source extends Iterator<JsonElement>, Closeable {

  // just a combination of iterator and closeable

}
