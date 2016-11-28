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
import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

/**
 * An immutable response to an http invocation which only returns string content.
 */
@ProviderType
public final class CaravanHttpResponse {

  private final int status;
  private final String reason;
  private final Multimap<String, String> headers;
  private final Body body;

  CaravanHttpResponse(int status, String reason, Multimap<String, String> headers, Body body) {
    checkState(status >= 200, "Invalid status code: %s", status);
    this.status = status;
    this.reason = checkNotNull(reason, "reason");
    Multimap<String, String> copyOf = LinkedHashMultimap.create(checkNotNull(headers, "headers"));
    this.headers = ImmutableMultimap.copyOf(copyOf);
    this.body = body; // nullable
  }

  /**
   * status code. ex {@code 200} See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html" >rfc2616</a>
   * @return HTTP status code
   */
  public int status() {
    return status;
  }

  /**
   * @return HTTP status reason
   */
  public String reason() {
    return reason;
  }

  /**
   * @return HTTP headers
   */
  public Multimap<String, String> headers() {
    return headers;
  }

  /**
   * Collects all "Cache-Control" directives from the response headers into a single map. The keys in the map are the
   * directive names (e.g. "max-age", "no-cache"), and everything after the "=" is taken as value. For directives that
   * don't have a value "true" is used as a value instead.
   * @return the map of Cache-Control directice
   */
  public Map<String, String> getCacheControl() {

    // http headers are case-insensitive, and even if we write all our headers in upper-case,
    // any proxy in-between can turn them into lower-case variants.
    // We need to make sure that we don't miss the lower-case variants here
    List<String> cacheControlHeaders = new LinkedList<>();
    cacheControlHeaders.addAll(headers.get("Cache-Control"));
    cacheControlHeaders.addAll(headers.get("cache-control"));

    return CaravanHttpHelper.convertMultiValueHeaderToMap(cacheControlHeaders);
  }

  /**
   * if present, the response had a body
   * @return HTTP body
   */
  public Body body() {
    return body;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("HTTP/1.1 ").append(status).append(' ').append(reason).append('\n');
    for (String field : headers.keySet()) {
      builder.append(field).append(": ").append(Joiner.on(", ").join(headers.get(field))).append('\n');
    }
    if (body != null) {
      builder.append('\n').append(body);
    }
    return builder.toString();
  }

}
