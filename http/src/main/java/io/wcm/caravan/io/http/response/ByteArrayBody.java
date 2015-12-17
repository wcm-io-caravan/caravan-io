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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

import com.google.common.base.Charsets;

final class ByteArrayBody implements Body {

  static Body orNull(byte[] data) {
    if (data == null) {
      return null;
    }
    return new ByteArrayBody(data);
  }

  static Body orNull(String text, Charset charset) {
    if (text == null) {
      return null;
    }
    checkNotNull(charset, "charset");
    return new ByteArrayBody(text.getBytes(charset));
  }

  private final byte[] data;

  ByteArrayBody(byte[] data) {
    this.data = data;
  }

  @Override
  public Integer length() {
    return data.length;
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  @Override
  public InputStream asInputStream() throws IOException {
    return new ByteArrayInputStream(data);
  }

  @Override
  public Reader asReader() throws IOException {
    return new InputStreamReader(asInputStream(), Charsets.UTF_8);
  }

  @Override
  public String asString() throws IOException {
    return IOUtils.toString(data, CharEncoding.UTF_8);
  }

  @Override
  public void close() throws IOException {
    // nothing to do
  }

  @Override
  public String toString() {
    return decodeOrDefault(data, Charsets.UTF_8, "Binary data");
  }

  private static String decodeOrDefault(byte[] data, Charset charset, String defaultValue) {
    if (data == null) {
      return defaultValue;
    }
    checkNotNull(charset, "charset");
    try {
      return charset.newDecoder().decode(ByteBuffer.wrap(data)).toString();
    }
    catch (CharacterCodingException ex) {
      return defaultValue;
    }
  }

}
