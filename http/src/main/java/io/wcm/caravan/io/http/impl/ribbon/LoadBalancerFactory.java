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
package io.wcm.caravan.io.http.impl.ribbon;

import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import com.netflix.loadbalancer.reactive.LoadBalancerCommand;

/**
 * Factory to create Ribbon LoadBalancer commands for a logical service name. Additionally checks if the Ribbon hosts pointing to localhost.
 */
public interface LoadBalancerFactory {

  /**
   * Creates LoadBalancer command.
   * @param serviceName Logical service name
   * @return LoadBalancer command
   */
  LoadBalancerCommand<CaravanHttpResponse> createCommand(String serviceName);

  /**
   * @param serviceName Logical service name
   * @return True, if hosts defined for service point to localhost
   */
  boolean isLocalRequest(String serviceName);

}
