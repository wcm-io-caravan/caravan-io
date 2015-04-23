//$URL: $
//$Id: $
package io.wcm.caravan.io.http.response;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Builder for {@link CaravanHttpResponse}.
 */
@ProviderType
public final class CaravanHttpResponseBuilder {

  private int status;
  private String reason;
  private Multimap<String, String> headers = ArrayListMultimap.create();
  private Body body;

  /**
   * @param newStatus HTTP status code
   * @return Builder
   */
  public CaravanHttpResponseBuilder status(int newStatus) {
    this.status = newStatus;
    return this;
  }

  /**
   * @param newReason HTTP status reason
   * @return Builder
   */
  public CaravanHttpResponseBuilder reason(String newReason) {
    this.reason = newReason;
    return this;
  }

  /**
   * @param name Header name
   * @param value Header value
   * @return Builder
   */
  public CaravanHttpResponseBuilder header(String name, String value) {
    headers.put(name, value);
    return this;
  }

  /**
   * @param name Header name
   * @param values Header values
   * @return Builder
   */
  public CaravanHttpResponseBuilder header(String name, Collection<String> values) {
    headers.putAll(name, values);
    return this;
  }

  /**
   * @param headersToAdd Map of headers getting added
   * @return Builder
   */
  public CaravanHttpResponseBuilder headers(Multimap<String, String> headersToAdd) {
    headers.putAll(headersToAdd);
    return this;
  }

  /**
   * @param data HTTP body
   * @return Builder
   */
  public CaravanHttpResponseBuilder body(byte[] data) {
    body = ByteArrayBody.orNull(data);
    return this;
  }

  /**
   * @param inputStream HTTP body
   * @param length HTTP body length
   * @return Builder
   */
  public CaravanHttpResponseBuilder body(InputStream inputStream, Integer length) {
    body = InputStreamBody.orNull(inputStream, length);
    return this;
  }

  /**
   * @param text HTTP body
   * @param charset HTTP body charset
   * @return Builder
   */
  public CaravanHttpResponseBuilder body(String text, Charset charset) {
    body = ByteArrayBody.orNull(text, charset);
    return this;
  }

  /**
   * Builds the Caravan HTTP response
   * @return HTTP response
   */
  public CaravanHttpResponse build() {
    return new CaravanHttpResponse(status, reason, headers, body);
  }

}
