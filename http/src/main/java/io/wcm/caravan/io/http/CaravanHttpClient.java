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
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

/**
 * Resilient HTTP transport layer that can execute any request asynchronously and
 * applying software load balancing and circuit breaking.
 */
@ProviderType
public interface CaravanHttpClient {

  /**
   * Execute request.
   * @param request Request
   * @return Response
   */
  Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request);

  /**
   * Execute request.
   * @param request Request
   * @param fallback Function that returns a fallback that is returned when the call fails.
   * @return Response
   */
  Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request, Observable<CaravanHttpResponse> fallback);

  /**
   * Checks if a valid configuration exists for the given service ID. This does not mean that the host
   * name is correct or returns correct responses, it only checks that the minimum required configuration
   * properties are set to a value.
   * @param serviceId Service ID
   * @return true if configuration is valid
   */
  boolean hasValidConfiguration(String serviceId);

}
