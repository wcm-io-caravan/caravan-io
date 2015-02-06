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
import io.wcm.caravan.io.http.response.Response;
import rx.Observable;

/**
 * Resilient transport layer that can execute any request asynchronously and
 * applying software load balancing and circuit breaking.
 */
public interface ResilientHttp {

  /**
   * Execute request.
   * @param serviceName Logical service name (is mapped to configured real host names).
   * @param request Request
   * @return Response
   */
  Observable<Response> execute(String serviceName, Request request);

  /**
   * Execute request.
   * @param serviceName Logical service name (is mapped to configured real host names).
   * @param request Request
   * @param fallback Function that returns a fallback that is returned when the call fails.
   * @return Response
   */
  Observable<Response> execute(String serviceName, Request request, Observable<Response> fallback);

}
