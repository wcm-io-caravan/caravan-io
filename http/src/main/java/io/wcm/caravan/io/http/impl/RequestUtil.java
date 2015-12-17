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
package io.wcm.caravan.io.http.impl;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.netflix.loadbalancer.Server;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;

/**
 * Utility methods for preparing a request for execution.
 */
final class RequestUtil {

  static final String PROTOCOL_AUTO = "auto";
  static final String PROTOCOL_HTTP = "http";
  static final String PROTOCOL_HTTPS = "https";

  private RequestUtil() {
    // static methods only
  }

  /**
   * @param server Server
   * @return URL prefix with scheme, hostname and port
   */
  public static String buildUrlPrefix(Server server, String protocol) {
    StringBuilder urlPrefix = new StringBuilder();
    if (StringUtils.equals(protocol, PROTOCOL_HTTP)) {
      urlPrefix.append("http://");
      if (server.getPort() == 80) {
        urlPrefix.append(server.getHost());
      }
      else {
        urlPrefix.append(server.getHost()).append(":").append(server.getPort());
      }
    }
    else if (StringUtils.equals(protocol, PROTOCOL_HTTPS)) {
      urlPrefix.append("https://");
      if (server.getPort() == 443 || server.getPort() == 80) {
        urlPrefix.append(server.getHost());
      }
      else {
        urlPrefix.append(server.getHost()).append(":").append(server.getPort());
      }
    }
    else {
      if (server.getPort() == 443 || server.getPort() == 8443) {
        urlPrefix.append("https");
      }
      else {
        urlPrefix.append("http");
      }
      urlPrefix.append("://").append(server.getHost());
      if (server.getPort() != 80 && server.getPort() != 443) {
        urlPrefix.append(':').append(server.getPort());
      }
    }
    return urlPrefix.toString();
  }

  /**
   * @param urlPrefix URL prefix
   * @param request Requset
   * @return HTTP client request object
   */
  public static HttpUriRequest buildHttpRequest(String urlPrefix, CaravanHttpRequest request) {
    String url = urlPrefix + request.getUrl();

    // http method
    HttpUriRequest httpRequest;
    String method = StringUtils.upperCase(request.getMethod());
    switch (method) {
      case HttpGet.METHOD_NAME:
        httpRequest = new HttpGet(url);
        break;
      case HttpPost.METHOD_NAME:
        httpRequest = new HttpPost(url);
        break;
      case HttpPut.METHOD_NAME:
        httpRequest = new HttpPut(url);
        break;
      case HttpDelete.METHOD_NAME:
        httpRequest = new HttpDelete(url);
        break;
      default:
        throw new IllegalArgumentException("Unsupported HTTP method type: " + request.getMethod());
    }

    // headers
    request.getHeaders().entries().forEach(e -> httpRequest.addHeader(e.getKey(), e.getValue()));

    // body
    if ((httpRequest instanceof HttpEntityEnclosingRequest) && request.getBody() != null) {
      HttpEntityEnclosingRequest entityHttpRequest = (HttpEntityEnclosingRequest)httpRequest;
      if (request.getCharset() != null) {
        entityHttpRequest.setEntity(new StringEntity(new String(request.getBody(), request.getCharset()), request.getCharset()));
      }
      else {
        entityHttpRequest.setEntity(new ByteArrayEntity(request.getBody()));
      }
    }

    return httpRequest;
  }

  /**
   * @param headerArray Http-client header array
   * @return Map with header values
   */
  public static Multimap<String, String> toHeadersMap(Header... headerArray) {
    LinkedHashMultimap<String, String> headerMap = LinkedHashMultimap.create();
    Arrays.stream(headerArray).forEach(h -> headerMap.put(h.getName(), h.getValue()));
    return ImmutableListMultimap.copyOf(headerMap);
  }

}
