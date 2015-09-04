/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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

import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.http.RequestInstantiationRuntimeException;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.netflix.client.ClientFactory;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;

/**
 * Factory for Hystrix {@link LoadBalancerCommand}s configuring the load balancer and retry handler.
 */
@Component
@Service(LoadBalancerCommandFactory.class)
public class LoadBalancerCommandFactory {

  @Reference(target = "(type=" + LoadBalancerFactory.CACHING + ")")
  private LoadBalancerFactory loadBalancerFactory;

  /**
   * @param serviceId Logical name of the HTTP service
   * @return Hystrix command to execute a HTTP request with load balancer
   */
  public LoadBalancerCommand<CaravanHttpResponse> createCommand(String serviceId) {

    ILoadBalancer loadBalancer = loadBalancerFactory.getLoadBalancer(serviceId);
    IClientConfig config = ClientFactory.getNamedConfig(serviceId, DefaultClientConfigImpl.class);

    return LoadBalancerCommand.<CaravanHttpResponse>builder()
        .withLoadBalancer(loadBalancer)
        .withClientConfig(config)
        .withRetryHandler(new CaravanLoadBalancerRetryHandler(config))
        .build();

  }

  /**
   * Determines if the HTTP request will be a local request by checking hosts of the configured servers.
   * @param serviceId Logical name of the HTTP service
   * @return True if load balancer has only local servers
   */
  public boolean isLocalRequest(String serviceId) {

    try {
      ILoadBalancer loadBalancer = loadBalancerFactory.getLoadBalancer(serviceId);
      List<Server> serverList = loadBalancer.getServerList(true);
      return Streams.of(serverList)
          .filter(server -> !StringUtils.equals(server.getHost(), "localhost"))
          .count() == 0;
    }
    catch (RequestInstantiationRuntimeException ex) {
      return false;
    }

  }

}
