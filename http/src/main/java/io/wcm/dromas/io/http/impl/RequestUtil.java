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
package io.wcm.dromas.io.http.impl;

import io.wcm.dromas.commons.stream.Streams;
import io.wcm.dromas.io.http.request.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import com.google.common.collect.ImmutableMap;
import com.netflix.loadbalancer.Server;

/**
 * Utility methods for preparing a request for execution.
 */
final class RequestUtil {

  private RequestUtil() {
    // static methods only
  }

  /**
   * @param server Server
   * @return URL prefix with scheme, hostname and port
   */
  public static String buildUrlPrefix(Server server) {
    StringBuilder urlPrefix = new StringBuilder();
    if (server.getPort() == 443 || server.getPort() == 8443) {
      urlPrefix.append("https");
    }
    else {
      urlPrefix.append("http");
    }
    urlPrefix.append("://").append(server.getHost());
    if (server.getPort() != 80 && server.getPort() != 443) {
      urlPrefix.append(":").append(server.getPort());
    }
    return urlPrefix.toString();
  }

  /**
   * @param urlPrefix URL prefix
   * @param request Requset
   * @return HTTP client request object
   */
  public static HttpUriRequest buildHttpRequest(String urlPrefix, Request request) {
    String url = urlPrefix + request.url();

    // http method
    HttpUriRequest httpRequest;
    String method = StringUtils.upperCase(request.method());
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
        throw new IllegalArgumentException("Unsupported HTTP method type: " + request.method());
    }

    // headers
    for (Entry<String, Collection<String>> entry : request.headers().entrySet()) {
      Streams.of(entry.getValue()).forEach(value -> httpRequest.addHeader(entry.getKey(), value));
    }

    // body
    if ((httpRequest instanceof HttpEntityEnclosingRequest) && request.body() != null) {
      HttpEntityEnclosingRequest entityHttpRequest = (HttpEntityEnclosingRequest)httpRequest;
      if (request.charset() != null) {
        entityHttpRequest.setEntity(new StringEntity(new String(request.body(), request.charset()), request.charset()));
      }
      else {
        entityHttpRequest.setEntity(new ByteArrayEntity(request.body()));
      }
    }

    return httpRequest;
  }

  /**
   * @param headerArray Http-client header array
   * @return Map with header values
   */
  public static Map<String, Collection<String>> toHeadersMap(Header[] headerArray) {
    Map<String, List<String>> headerMap = new HashMap<>();
    for (Header header : headerArray) {
      List<String> values = headerMap.get(header.getName());
      if (values == null) {
        values = new ArrayList<>();
        headerMap.put(header.getName(), values);
      }
      values.add(header.getValue());
    }
    return ImmutableMap.copyOf(headerMap);
  }

}
