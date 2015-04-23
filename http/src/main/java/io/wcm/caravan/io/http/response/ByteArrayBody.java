//$URL: $
//$Id: $
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

  public ByteArrayBody(byte[] data) {
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
