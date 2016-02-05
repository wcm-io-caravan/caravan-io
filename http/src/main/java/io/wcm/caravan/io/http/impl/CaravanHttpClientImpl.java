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
import io.wcm.caravan.common.performance.PerformanceMetrics;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.impl.ribbon.RibbonHttpClient;
import io.wcm.caravan.io.http.impl.servletclient.NotSupportedByRequestMapperException;
import io.wcm.caravan.io.http.impl.servletclient.ServletHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;

import com.netflix.client.ClientException;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * Default implementation of {@link CaravanHttpClient}.
 */
@Component(immediate = true)
@Service(CaravanHttpClient.class)
public class CaravanHttpClientImpl implements CaravanHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(CaravanHttpClientImpl.class);

  @Reference
  private CaravanHttpClientConfig config;
  @Reference
  private ServletHttpClient servletClient;
  @Reference
  private RibbonHttpClient ribbonClient;
  @Reference
  private ApacheHttpClient apacheHttpClient;

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request) {
    Context ctx = new Context(request, null);
    return execute(ctx);
  }

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request, Observable<CaravanHttpResponse> fallback) {
    Context ctx = new Context(request, fallback);
    return execute(ctx);
  }

  private Observable<CaravanHttpResponse> execute(Context ctx) {

    if (isRequestWithoutServiceId(ctx)) {
      return createApacheResponse(ctx);
    }

    Observable<CaravanHttpResponse> ribbonResponse = createRibbonResponse(ctx);
    if (isServletClientPossible(ctx)) {
      return createServletClientResponse(ctx, ribbonResponse);
    }
    return ribbonResponse;

  }

  private boolean isRequestWithoutServiceId(Context ctx) {
    return StringUtils.isEmpty(ctx.request.getServiceId());
  }

  private Observable<CaravanHttpResponse> createApacheResponse(Context ctx) {
    Observable<CaravanHttpResponse> response = apacheHttpClient.execute(ctx.request);
    return addHystrixAndErrorMapperAndMetrics(ctx, response);
  }

  private Observable<CaravanHttpResponse> createRibbonResponse(Context ctx) {
    Observable<CaravanHttpResponse> response = ribbonClient.execute(ctx.request);
    return addHystrixAndErrorMapperAndMetrics(ctx, response);
  }

  private boolean isServletClientPossible(Context ctx) {
    return config.isServletClientEnabled() && servletClient.hasValidConfiguration(ctx.request.getServiceId());
  }

  private Observable<CaravanHttpResponse> createServletClientResponse(Context ctx, Observable<CaravanHttpResponse> ribbonResponse) {
    Observable<CaravanHttpResponse> localhostResponse = servletClient.execute(ctx.request)
        .lift(new ErrorDisassembleroperator(ctx, ribbonResponse));
    return addHystrixAndErrorMapperAndMetrics(ctx, localhostResponse);
  }

  private Observable<CaravanHttpResponse> addHystrixAndErrorMapperAndMetrics(Context requestAndFallback,
      Observable<CaravanHttpResponse> clientResponse) {
    Observable<CaravanHttpResponse> hystrixResponse = wrapWithHystrix(requestAndFallback, clientResponse);
    Observable<CaravanHttpResponse> exceptionMapperResponse = wrapWithExceptionMapper(requestAndFallback, hystrixResponse);
    return addMetrics(requestAndFallback, exceptionMapperResponse);
  }

  private Observable<CaravanHttpResponse> wrapWithHystrix(Context ctx, Observable<CaravanHttpResponse> response) {
    ExecutionIsolationStrategy isolationStrategy = getIsolationStrategy(ctx);
    return new HttpHystrixCommand(ctx.request, isolationStrategy, response, ctx.fallback).toObservable();
  }

  private ExecutionIsolationStrategy getIsolationStrategy(Context ctx) {
    String threadPoolConfigKey = HYSTRIX_COMMAND_PREFIX + ctx.request.getServiceId() + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE;
    String configuredThreadPool = ArchaiusConfig.getConfiguration().getString(threadPoolConfigKey);
    return isBlank(configuredThreadPool) ? ExecutionIsolationStrategy.SEMAPHORE : ExecutionIsolationStrategy.THREAD;
  }

  private Observable<CaravanHttpResponse> wrapWithExceptionMapper(Context ctx, Observable<CaravanHttpResponse> response) {
    return response.onErrorResumeNext(ex -> Observable.<CaravanHttpResponse>error(mapToKnownException(ctx.request, ex)));
  }

  private Throwable mapToKnownException(CaravanHttpRequest request, Throwable ex) {
    if (ex instanceof RequestFailedRuntimeException || ex instanceof IllegalResponseRuntimeException) {
      return ex;
    }
    if ((ex instanceof HystrixRuntimeException || ex instanceof ClientException) && ex.getCause() != null) {
      return mapToKnownException(request, ex.getCause());
    }
    throw new RequestFailedRuntimeException(request, StringUtils.defaultString(ex.getMessage(), ex.getClass().getSimpleName()), ex);
  }

  private Observable<CaravanHttpResponse> addMetrics(Context ctx, Observable<CaravanHttpResponse> response) {
    PerformanceMetrics metrics = ctx.request.getPerformanceMetrics();
    return response.doOnSubscribe(metrics.getStartAction())
        .doOnNext(metrics.getOnNextAction())
        .doOnTerminate(metrics.getEndAction());
  }

  @Override
  public boolean hasValidConfiguration(String serviceId) {
    return CaravanHttpServiceConfigValidator.hasValidConfiguration(serviceId);
  }

  private class Context {

    private final CaravanHttpRequest request;
    private final Observable<CaravanHttpResponse> fallback;

    public Context(CaravanHttpRequest request, Observable<CaravanHttpResponse> fallback) {
      this.request = request;
      this.fallback = fallback;
    }

  }

  private class ErrorDisassembleroperator implements Operator<CaravanHttpResponse, CaravanHttpResponse> {

    private final Context ctx;
    private final Observable<CaravanHttpResponse> nonLocalResponse;

    public ErrorDisassembleroperator(Context ctx, Observable<CaravanHttpResponse> nonLocalResponse) {
      this.ctx = ctx;
      this.nonLocalResponse = nonLocalResponse;
    }

    @Override
    public Subscriber<? super CaravanHttpResponse> call(Subscriber<? super CaravanHttpResponse> subscriber) {
      return new Subscriber<CaravanHttpResponse>() {

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable ex) {
          if (ex instanceof NotSupportedByRequestMapperException) {
            LOG.warn("Could not execute request with localhost client for service " + ctx.request.getServiceId());
            nonLocalResponse.subscribe(subscriber);
          }
          else {
            subscriber.onError(ex);
          }
        }

        @Override
        public void onNext(CaravanHttpResponse next) {
          subscriber.onNext(next);
        }

      };
    }

  }

}
