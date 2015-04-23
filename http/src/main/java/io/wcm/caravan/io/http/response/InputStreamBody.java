//$URL: $
//$Id: $
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