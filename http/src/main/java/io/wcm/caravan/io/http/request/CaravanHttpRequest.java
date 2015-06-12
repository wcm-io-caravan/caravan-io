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
import io.wcm.caravan.common.performance.PerformanceMetrics;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * An immutable request to a HTTP server.
 */
@ProviderType
public final class CaravanHttpRequest {

  /**
   * Correlation ID Header name
   */
  public static final String CORRELATION_ID_HEADER_NAME = "Caravan-Correlation-Id";

  private final String serviceId;
  private final String method;
  private final String url;
  private final Multimap<String, String> headers;
  private final byte[] body;
  private final Charset charset;
  private PerformanceMetrics performanceMetrics;

  /**
   * @param serviceId Logical name of the request service. Used by {@link CaravanHttpClient} to resolve the real URL.
   *          If null, only {@code url} is used
   * @param method HTTP method verb
   * @param url Service request URL. Can be an absolute URL or just an path getting combined with the URL of the logical
   *          service ID
   * @param headers HTTP headers
   * @param body HTTP Payload
   * @param charset Payload charset
   */
  CaravanHttpRequest(final String serviceId, final String method, final String url, final Multimap<String, String> headers, final byte[] body,
      final Charset charset) {
    this.serviceId = serviceId; // nullable
    this.method = checkNotNull(method, "method of %s", url);
    this.url = checkNotNull(url, "url");
    this.headers = ImmutableMultimap.copyOf(LinkedHashMultimap.create(checkNotNull(headers, "headers of %s %s", method, url)));
    this.body = body; // nullable
    this.charset = charset; // nullable
    this.performanceMetrics = PerformanceMetrics.createNew(
        StringUtils.defaultString(serviceId, "UNKNOWN SERVICE") + " : " + StringUtils.defaultString(method, "UNKNOWN METHOD"), url, getCorrelationId());
  }

  /**
   * Method to invoke on the server.
   * @return HTTP method
   */
  public String getMethod() {
    return method;
  }

  /**
   * Fully resolved url including query.
   * @return URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * Ordered list of headers that will be sent to the server.
   * @return HTTP headers
   */
  public Multimap<String, String> getHeaders() {
    return headers;
  }

  /**
   * Collects all "Cache-Control" directives from the response headers into a single map. The keys in the map are the
   * directive names (e.g. "max-age", "no-cache"), and everything after the "=" is taken as value. For directives that
   * don't have a value "true" is used as a value instead.
   * @return the map of Cache-Control directives
   */
  public Map<String, String> getCacheControl() {
    return CaravanHttpHelper.convertMultiValueHeaderToMap(headers.get("Cache-Control"));
  }

  /**
   * @param name of the query parameter
   * @return true if the parameter exists in this request's query
   */
  public boolean hasParameter(String name) {
    return Pattern.compile("[\\?|\\&](" + name + "\\=[^\\&]).*$").matcher(url).find();
  }

  /**
   * The character set with which the body is encoded, or null if unknown or not applicable. When this is present, you
   * can use {@code new String(req.body(), req.charset())} to access the body as a String.
   * @return Charset
   */
  public Charset getCharset() {
    return charset;
  }

  /**
   * If present, this is the replayable body to send to the server. In some cases, this may be interpretable as text.
   * @see #getCharset()
   * @return HTTP body
   */
  public byte[] getBody() {
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
   * @return the service ID
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * @return the service ID
   * @deprecated Please use {@link #getServiceId()}
   */
  @Deprecated
  public String getServiceName() {
    return serviceId;
  }

  /**
   * @return the value of the correlation-id header or null if it wasn't set
   */
  public String getCorrelationId() {
    Collection<String> correlationHeaders = getHeaders().get(CaravanHttpRequest.CORRELATION_ID_HEADER_NAME);
    return correlationHeaders.isEmpty() ? null : correlationHeaders.iterator().next();
  }

  public PerformanceMetrics getPerformanceMetrics() {
    return this.performanceMetrics;
  }

}
