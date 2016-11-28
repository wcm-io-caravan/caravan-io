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
package io.wcm.caravan.io.http.impl;

import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.HYSTRIX_COMMAND_PREFIX;
import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixObservableCommand;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import rx.Observable;
import rx.functions.Action1;

/**
 * Hystrix command for asynchronously wrapping a HTTP request execution.
 */
class HttpHystrixCommand extends HystrixObservableCommand<CaravanHttpResponse> {

  private static final Logger log = LoggerFactory.getLogger(HttpHystrixCommand.class);
  private static final String GROUP_KEY = "transportLayer";

  private final CaravanHttpRequest request;
  private final Observable<CaravanHttpResponse> observable;
  private final Observable<CaravanHttpResponse> fallback;

  /**
   * @param request the request to execute
   * @param observable the observable that emits the response for this request (created with one of the
   *          CaravanHttpClient implementations)
   * @param fallback the fallback response to emit if the original request fails
   */
  public HttpHystrixCommand(CaravanHttpRequest request, Observable<CaravanHttpResponse> observable, Observable<CaravanHttpResponse> fallback) {

    super(Setter
        .withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
        .andCommandKey(HystrixCommandKey.Factory.asKey(StringUtils.defaultString(request.getServiceId(), "UNKNOWN")))
        .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
            .withExecutionIsolationStrategy(getIsolationStrategy(request))
            .withExecutionIsolationSemaphoreMaxConcurrentRequests(10000)));

    this.request = request;
    this.observable = observable;
    this.fallback = fallback;
  }

  @Override
  protected Observable<CaravanHttpResponse> construct() {
    if (fallback != null) {
      // make sure errors are logged in log and not swallowed when hystrix falls back to fallback
      return observable.doOnError(new Action1<Throwable>() {

        @Override
        public void call(Throwable ex) {
          log.warn("Service call to '" + request.getServiceId() + "' failed, returned fallback instead.", ex);
        }
      });
    }
    else {
      return observable;
    }
  }

  @Override
  protected Observable<CaravanHttpResponse> resumeWithFallback() {
    if (fallback != null) {
      return fallback;
    }
    else {
      return super.resumeWithFallback();
    }
  }

  /**
   * Check which hystrix isolation strategy is configured for the target service of the given request
   * @param request the request to execute
   * @return {@link ExecutionIsolationStrategy#THREAD} if there is a hystrixThreadPoolKeyOverride configured for the
   *         target serviceId of the given request, or {@link ExecutionIsolationStrategy#SEMAPHORE} otherwise
   */
  public static ExecutionIsolationStrategy getIsolationStrategy(CaravanHttpRequest request) {
    String threadPoolConfigKey = HYSTRIX_COMMAND_PREFIX + request.getServiceId() + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE;
    String configuredThreadPool = ArchaiusConfig.getConfiguration().getString(threadPoolConfigKey);
    return isBlank(configuredThreadPool) ? ExecutionIsolationStrategy.SEMAPHORE : ExecutionIsolationStrategy.THREAD;
  }

}
