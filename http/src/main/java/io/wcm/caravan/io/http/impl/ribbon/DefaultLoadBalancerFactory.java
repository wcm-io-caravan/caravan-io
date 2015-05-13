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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;

/**
 * Default implementation for the LoadBalancer factory. Offers no caching or optimization.
 */
public class DefaultLoadBalancerFactory implements LoadBalancerFactory {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLoadBalancerFactory.class);

  @Override
  public LoadBalancerCommand<CaravanHttpResponse> createCommand(String key) {

    ILoadBalancer loadBalancer = createLoadBalancer(key);
    if (loadBalancer == null) {
      return null;
    }

    IClientConfig config = ClientFactory.getNamedConfig(key, DefaultClientConfigImpl.class);

    return LoadBalancerCommand.<CaravanHttpResponse>builder()
        .withLoadBalancer(loadBalancer)
        .withClientConfig(config)
        .withRetryHandler(new CaravanLoadBalancerRetryHandler(config))
        .build();

  }

  @Override
  public boolean isLocalRequest(String key) {

    ILoadBalancer loadBalancer = createLoadBalancer(key);
    if (loadBalancer == null) {
      return false;
    }

    List<Server> serverList = loadBalancer.getServerList(true);
    return serverList.size() == 1 && StringUtils.equals(serverList.get(0).getHost(), "localhost");
  }

  private ILoadBalancer createLoadBalancer(String key) {

    try {
      IClientConfig clientConfig = ClientFactory.getNamedConfig(key, DefaultClientConfigImpl.class);
      String loadBalancerClassName = clientConfig.get(CommonClientConfigKey.NFLoadBalancerClassName);
      return (ILoadBalancer)ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, clientConfig);
    }
    catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
      LOG.error("Can't create LoadBalancerCommand for " + key, ex);
      return null;
    }

  }

}
