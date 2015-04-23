//$URL: $
//$Id: $
package io.wcm.caravan.io.http.response;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Generic HTTP body.
 */
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
