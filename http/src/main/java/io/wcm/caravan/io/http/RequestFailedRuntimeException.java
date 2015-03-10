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

import io.wcm.caravan.io.http.request.CaravanHttpRequest;

/**
 * Exception is thrown when a resilient HTTP request failed before a response was received.
 */
public final class RequestFailedRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final String serviceName;
  private final CaravanHttpRequest request;

  /**
   * @param serviceName Service name
   * @param request Request
   * @param message Error message
   * @param cause Cause (may be null)
   */
  public RequestFailedRuntimeException(String serviceName, CaravanHttpRequest request, String message, Throwable cause) {
    super(serviceName + ": " + message, cause);
    this.serviceName = serviceName;
    this.request = request;
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
  public CaravanHttpRequest getRequest() {
    return this.request;
  }

}
