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
package io.wcm.dromas.io.http.request;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

/**
 * An immutable request to an http server.
 */
public final class Request {

  private final String method;
  private final String url;
  private final Map<String, Collection<String>> headers;
  private final byte[] body;
  private final Charset charset;

  Request(String method, String url, Map<String, Collection<String>> headers, byte[] body, Charset charset) {
    this.method = checkNotNull(method, "method of %s", url);
    this.url = checkNotNull(url, "url");
    LinkedHashMap<String, Collection<String>> copyOf = new LinkedHashMap<String, Collection<String>>();
    copyOf.putAll(checkNotNull(headers, "headers of %s %s", method, url));
    this.headers = Collections.unmodifiableMap(copyOf);
    this.body = body; // nullable
    this.charset = charset; // nullable
  }

  /** Method to invoke on the server. */
  public String method() {
    return method;
  }

  /** Fully resolved URL including query. */
  public String url() {
    return url;
  }

  /** Ordered list of headers that will be sent to the server. */
  public Map<String, Collection<String>> headers() {
    return headers;
  }

  /**
   * The character set with which the body is encoded, or null if unknown or not applicable. When this is
   * present, you can use {@code new String(req.body(), req.charset())} to access the body as a String.
   */
  public Charset charset() {
    return charset;
  }

  /**
   * If present, this is the replayable body to send to the server. In some cases, this may be interpretable as text.
   * @see #charset()
   */
  public byte[] body() {
    return body;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(method).append(' ').append(url).append(" HTTP/1.1\n");
    for (String field : headers.keySet()) {
      for (String value : ObjectUtils.defaultIfNull(headers.get(field), Collections.<String>emptyList())) {
        builder.append(field).append(": ").append(value).append('\n');
      }
    }
    if (body != null) {
      builder.append('\n').append(charset != null ? new String(body, charset) : "Binary data");
    }
    return builder.toString();
  }

}
