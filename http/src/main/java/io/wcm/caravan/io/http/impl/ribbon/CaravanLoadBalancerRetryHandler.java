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

import java.util.List;

import com.google.common.collect.Lists;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;

import io.wcm.caravan.io.http.IllegalResponseRuntimeException;

/**
 * Adapted implementation of a {@link RetryHandler}. Extends the list of retriable exceptions for the
 * {@link DefaultLoadBalancerRetryHandler} by adding the {@link IllegalResponseRuntimeException}. This causes Ribbon to
 * repeat the request if this exception is thrown.
 */
public class CaravanLoadBalancerRetryHandler extends DefaultLoadBalancerRetryHandler {

  private final List<Class<? extends Throwable>> retriableExceptions;

  /**
   * @param clientConfig The client configuration
   */
  public CaravanLoadBalancerRetryHandler(final IClientConfig clientConfig) {
    super(clientConfig);
    retriableExceptions = Lists.newArrayList(super.getRetriableExceptions());
    retriableExceptions.add(IllegalResponseRuntimeException.class);
  }

  @Override
  protected List<Class<? extends Throwable>> getRetriableExceptions() {
    return retriableExceptions;
  }


}
