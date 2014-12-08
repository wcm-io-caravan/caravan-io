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
package io.wcm.caravan.io.http;

import io.wcm.caravan.io.http.request.Request;

/**
 * Exception is thrown when a resilient HTTP response was received, but rated illegal (e.g. beause of status code).
 */
public final class IllegalResponseRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final String serviceName;
  private final Request request;
  private final String requestUri;
  private final int responseStatusCode;
  private final String responseBody;

  /**
   * @param serviceName Service name
   * @param request Request
   * @param requestUri Full request URI
   * @param responseStatusCode Response satus code
   * @param responseBody Response bdoy
   * @param message Error message
   */
  public IllegalResponseRuntimeException(String serviceName, Request request, String requestUri,
      int responseStatusCode, String responseBody, String message) {
    super(serviceName + ": " + message);
    this.serviceName = serviceName;
    this.request = request;
    this.requestUri = requestUri;
    this.responseStatusCode = responseStatusCode;
    this.responseBody = responseBody;
  }

  /**
   * @return Service name
   */
  public String getServiceName() {
    return this.serviceName;
  }

  /**
   * @return Request
   */
  public Request getRequest() {
    return this.request;
  }

  /**
   * @return Full request URI
   */
  public String getRequestUri() {
    return this.requestUri;
  }

  /**
   * @return Response status code
   */
  public int getResponseStatusCode() {
    return this.responseStatusCode;
  }

  /**
   * @return Response body
   */
  public String getResponseBody() {
    return this.responseBody;
  }

}
