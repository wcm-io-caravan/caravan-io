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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;

final class InputStreamBody implements Body {

  static Body orNull(InputStream inputStream, Integer length) {
    if (inputStream == null) {
      return null;
    }
    return new InputStreamBody(inputStream, length);
  }

  private final InputStream inputStream;
  private final Integer length;

  private InputStreamBody(InputStream inputStream, Integer length) {
    this.inputStream = inputStream;
    this.length = length;
  }

  @Override
  public Integer length() {
    return length;
  }

  @Override
  public boolean isRepeatable() {
    return false;
  }

  @Override
  public InputStream asInputStream() throws IOException {
    return inputStream;
  }

  @Override
  public Reader asReader() throws IOException {
    return new InputStreamReader(inputStream, Charsets.UTF_8);
  }

  @Override
  public String asString() throws IOException {
    try {
      return IOUtils.toString(inputStream, Charsets.UTF_8);
    }
    finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

}
