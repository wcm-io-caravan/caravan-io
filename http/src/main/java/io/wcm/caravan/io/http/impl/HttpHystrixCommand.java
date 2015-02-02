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

import io.wcm.caravan.io.http.response.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;

/**
 * Hystrix command for asynchronously wrapping a HTTP request execution.
 */
class HttpHystrixCommand extends HystrixObservableCommand<Response> {

  private static final String GROUP_KEY = "transportLayer";

  private final String serviceName;
  private final Observable<Response> observable;
  private final Observable<Response> fallback;

  private static final Logger log = LoggerFactory.getLogger(HttpHystrixCommand.class);

  public HttpHystrixCommand(String serviceName, Observable<Response> observable, Observable<Response> fallback) {
    super(Setter
        .withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
        .andCommandKey(HystrixCommandKey.Factory.asKey(serviceName)));

    this.serviceName = serviceName;
    this.observable = observable;
    this.fallback = fallback;
  }

  @Override
  protected Observable<Response> construct() {
    if (fallback != null) {
      // make sure errors are logged in log and not swallowed when hystrix falls back to fallback
      return observable.doOnError(new Action1<Throwable>() {
        @Override
        public void call(Throwable ex) {
          log.warn("Service call to '" + serviceName + "' failed, returned fallback instead.", ex);
        }
      });
    }
    else {
      return observable;
    }
  }

  @Override
  protected Observable<Response> resumeWithFallback() {
    if (fallback != null) {
      return fallback;
    }
    else {
      return super.resumeWithFallback();
    }
  }

}