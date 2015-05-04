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

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;

/**
 * Caching implementation for the LoadBalancer factory. Stores the delegated results temporary for one minute.
 */
public class CachingLoadBalancerFactory implements LoadBalancerFactory {

  private final LoadBalancerFactory delegate;

  private final LoadingCache<String, LoadBalancerCommand<CaravanHttpResponse>> loadBalancerCommandCache = CacheBuilder.newBuilder()
      .expireAfterWrite(1, TimeUnit.MINUTES).build(
          new CacheLoader<String, LoadBalancerCommand<CaravanHttpResponse>>() {

            @Override
            public LoadBalancerCommand<CaravanHttpResponse> load(String key) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
              return delegate.createCommand(key);
            }
          });

  private final LoadingCache<String, Boolean> isLocalCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
      .build(new CacheLoader<String, Boolean>() {

        @Override
        public Boolean load(String key) throws Exception {
          return delegate.isLocalRequest(key);
        }

      });

  /**
   * @param delegate
   */
  public CachingLoadBalancerFactory(LoadBalancerFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public LoadBalancerCommand<CaravanHttpResponse> createCommand(String serviceName) {
    if (serviceName == null) {
      return null;
    }
    return loadBalancerCommandCache.getUnchecked(serviceName);
  }

  @Override
  public boolean isLocalRequest(String serviceName) {
    if (serviceName == null) {
      return false;
    }
    return isLocalCache.getUnchecked(serviceName);
  }

}
