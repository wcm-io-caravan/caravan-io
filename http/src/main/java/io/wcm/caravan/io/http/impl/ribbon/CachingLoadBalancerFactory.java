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

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.google.common.collect.Maps;
import com.netflix.loadbalancer.ILoadBalancer;

/**
 * Caching implementation for the Ribbon load balancer factory storing items in a {@link ConcurrentMap}. Offers the
 * ability to remove a load balancer from cache store.
 */
@Component
@Service(LoadBalancerFactory.class)
@Property(name = "type", value = LoadBalancerFactory.CACHING)
public class CachingLoadBalancerFactory implements LoadBalancerFactory {

  @Reference(target = "(type=" + LoadBalancerFactory.SIMPLE + ")")
  private LoadBalancerFactory delegate;

  private final ConcurrentMap<String, ILoadBalancer> store = Maps.newConcurrentMap();

  @Override
  public ILoadBalancer getLoadBalancer(String serviceId) {

    if (store.containsKey(serviceId)) {
      return store.get(serviceId);
    }

    ILoadBalancer loadBalancer = delegate.getLoadBalancer(serviceId);
    ILoadBalancer old = store.putIfAbsent(serviceId, loadBalancer);
    return ObjectUtils.defaultIfNull(old, loadBalancer);

  }

  /**
   * Removes the load balancer for the service ID from the cache store.
   * @param serviceId Logical name of the HTTP service
   */
  public void unregister(String serviceId) {
    store.remove(serviceId);
  }

}
