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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import io.wcm.caravan.io.http.RequestInstantiationRuntimeException;

/**
 * Simple implementation for the Ribbon load balancer factory. Uses Netflix OSS configuration infrastructure.
 */
@Component
@Service(LoadBalancerFactory.class)
@Property(name = "type", value = LoadBalancerFactory.SIMPLE)
public class SimpleLoadBalancerFactory implements LoadBalancerFactory {

  @Override
  public ILoadBalancer getLoadBalancer(String serviceId) {

    try {
      IClientConfig clientConfig = ClientFactory.getNamedConfig(serviceId, DefaultClientConfigImpl.class);
      String loadBalancerClassName = clientConfig.get(CommonClientConfigKey.NFLoadBalancerClassName);
      return (ILoadBalancer)ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, clientConfig);
    }
    catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
      throw new RequestInstantiationRuntimeException("Can't create LoadBalancer for " + serviceId, ex);
    }
  }

}
