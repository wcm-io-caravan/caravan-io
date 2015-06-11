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
package io.wcm.caravan.io.http.response;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Generic HTTP body.
 */
@ProviderType
public interface Body extends Closeable {

  /**
   * length in bytes, if known. Null if not. <br>
   * <br>
   * <br>
   * <b>Note</b><br>
   * This is an integer as most implementations cannot do
   * bodies greater than 2GB. Moreover, the scope of this interface doesn't include
   * large bodies.
   * @return Length of the body
   */
  Integer length();

  /**
   * True if {@link #asInputStream()} and {@link #asReader()} can be called more than once.
   * @return True if repeatable
   */
  boolean isRepeatable();

  /**
   * It is the responsibility of the caller to close the stream.
   * @return Stream representation
   * @throws IOException Error generating Stream
   */
  InputStream asInputStream() throws IOException;

  /**
   * It is the responsibility of the caller to close the stream.
   * @return Reader representation
   * @throws IOException Error generating Reader
   */
  Reader asReader() throws IOException;

  /**
   * Returns body as string and closes the stream.
   * @return String representation
   * @throws IOException Error generating String
   */
  String asString() throws IOException;

}
