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
package io.wcm.caravan.io.http.request;

import static com.google.common.base.Preconditions.checkNotNull;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * An immutable request to a HTTP server.
 */
public final class CaravanHttpRequest {

  private final String serviceName;
  private final String method;
  private final String url;
  private final Multimap<String, String> headers;
  private final byte[] body;
  private final Charset charset;

  /**
   * @param serviceName Logical name of the request service. Used by {@link CaravanHttpClient} to resolve the real URL.
   *          If null, only {@code url} is used
   * @param method HTTP method verb
   * @param url Service request URL. Can be an absolute URL or just an path getting combined with the URL of the logical
   *          service name
   * @param headers HTTP headers
   * @param body HTTP Payload
   * @param charset Payload charset
   */
  CaravanHttpRequest(final String serviceName, final String method, final String url, final Multimap<String, String> headers, final byte[] body,
      final Charset charset) {
    this.serviceName = serviceName; // nullable
    this.method = checkNotNull(method, "method of %s", url);
    this.url = checkNotNull(url, "url");
    this.headers = ImmutableMultimap.copyOf(LinkedHashMultimap.create(checkNotNull(headers, "headers of %s %s", method, url)));
    this.body = body; // nullable
    this.charset = charset; // nullable
  }

  /**
   * Method to invoke on the server.
   * @return HTTP method
   */
  public String method() {
    return method;
  }

  /**
   * Fully resolved url including query.
   * @return URL
   */
  public String url() {
    return url;
  }

  /**
   * Ordered list of headers that will be sent to the server.
   * @return HTTP headers
   */
  public Multimap<String, String> headers() {
    return headers;
  }

  /**
   * Returns a specific header represented as a {@link Map}. Therefore splits the entries of one header by {@code :}. If
   * the entry has no value gets interpreted as a boolean and set to true.
   * @param headerName Name of the header to convert
   * @return A map representation of the header
   */
  public Map<String, Object> getHeaderAsMap(final String headerName) {
    return CaravanHttpHelper.convertHeaderToMap(headers.get(headerName));
  }

  /**
   * @param name of the query parameter
   * @return true if the parameter exists in this request's query
   */
  public boolean hasParameter(String name) {
    // TODO: is there really no easier function for this on the classpath (e.g. parse into some MultiValueMap)
    List<NameValuePair> parameters = URLEncodedUtils.parse(URI.create(url), CharEncoding.UTF_8);
    // TODO: use Streams.of(parameters).exists(p -> name.equals(p.getName()))
    return Streams.of(parameters)
        .filter(param -> param.getName().equals(name))
        .iterator().hasNext();
  }

  /**
   * The character set with which the body is encoded, or null if unknown or not applicable. When this is
   * present, you can use {@code new String(req.body(), req.charset())} to access the body as a String.
   * @return Charset
   */
  public Charset charset() {
    return charset;
  }

  /**
   * If present, this is the replayable body to send to the server. In some cases, this may be interpretable as text.
   * @see #charset()
   * @return HTTP body
   */
  public byte[] body() {
    return body;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(method).append(' ').append(url).append(" HTTP/1.1\n");
    for (String field : headers.keySet()) {
      builder.append(field).append(": ").append(StringUtils.join(headers.get(field), ", ")).append('\n');
    }
    if (body != null) {
      builder.append('\n').append(charset != null ? new String(body, charset) : "Binary data");
    }
    return builder.toString();
  }

  /**
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

}
