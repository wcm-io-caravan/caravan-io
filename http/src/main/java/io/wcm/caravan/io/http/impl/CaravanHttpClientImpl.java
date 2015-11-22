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
import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerCommandFactory;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;

import com.netflix.client.ClientException;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;

/**
 * Default implementation of {@link CaravanHttpClient}.
 */
@Component(immediate = true)
@Service(CaravanHttpClient.class)
public class CaravanHttpClientImpl implements CaravanHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(CaravanHttpClientImpl.class);

  @Reference
  private HttpClientFactory httpClientFactory;
  @Reference
  private LoadBalancerCommandFactory commandFactory;

  @Override
  public Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request) {
    return execute(request, null);
  }

  @Override
  public Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request, final Observable<CaravanHttpResponse> fallback) {

    String serviceId = request.getServiceId();
    Observable<CaravanHttpResponse> httpRequest = StringUtils.isEmpty(serviceId) ? createHttpRequest("", request) : createRibbonRequest(request);
    ExecutionIsolationStrategy isolationStrategy = getIsolationStrategy(serviceId);
    Observable<CaravanHttpResponse> hystrixRequest = new HttpHystrixCommand(StringUtils.defaultString(serviceId, "UNKNOWN"), isolationStrategy, httpRequest,
        fallback).toObservable();

    PerformanceMetrics metrics = request.getPerformanceMetrics();
    return hystrixRequest.onErrorResumeNext(exception -> Observable.<CaravanHttpResponse>error(mapToKnownException(request, exception)))
        .doOnSubscribe(metrics.getStartAction()).doOnNext(metrics.getOnNextAction()).doOnTerminate(metrics.getEndAction());

  }

  private ExecutionIsolationStrategy getIsolationStrategy(String serviceId) {
    String threadPoolConfigKey = HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE;
    String configuredThreadPool = ArchaiusConfig.getConfiguration().getString(threadPoolConfigKey);

    return isBlank(configuredThreadPool) ? ExecutionIsolationStrategy.SEMAPHORE : ExecutionIsolationStrategy.THREAD;
  }

  private Observable<CaravanHttpResponse> createRibbonRequest(final CaravanHttpRequest request) {
    LoadBalancerCommand<CaravanHttpResponse> command = commandFactory.createCommand(request.getServiceId());
    ServerOperation<CaravanHttpResponse> operation = new ServerOperation<CaravanHttpResponse>() {

      @Override
      public Observable<CaravanHttpResponse> call(Server server) {
        String protcol = RequestUtil.PROTOCOL_AUTO;
        if (StringUtils.isNotEmpty(request.getServiceId())) {
          protcol = ArchaiusConfig.getConfiguration().getString(request.getServiceId() + CaravanHttpServiceConfig.HTTP_PARAM_PROTOCOL);
        }
        return createHttpRequest(RequestUtil.buildUrlPrefix(server, protcol), request);
      }
    };
    return command.submit(operation);
  }

  private Observable<CaravanHttpResponse> createHttpRequest(final String urlPrefix, final CaravanHttpRequest request) {
    return Observable.create(new Observable.OnSubscribe<CaravanHttpResponse>() {

      @Override
      public void call(final Subscriber<? super CaravanHttpResponse> subscriber) {
        HttpUriRequest httpRequest = RequestUtil.buildHttpRequest(urlPrefix, request);

        if (LOG.isDebugEnabled()) {
          LOG.debug("Execute: {},\n{},\n{}", httpRequest.getURI(), request.toString(), request.getCorrelationId());
        }

        HttpClient httpClient = httpClientFactory.get(httpRequest.getURI());

        long start = System.currentTimeMillis();
        try {
          HttpResponse result = httpClient.execute(httpRequest);

          StatusLine status = result.getStatusLine();
          HttpEntity entity = result.getEntity();

          try {
            if (status.getStatusCode() >= 500) {
              subscriber.onError(new IllegalResponseRuntimeException(request, httpRequest.getURI().toString(), status.getStatusCode(), EntityUtils
                  .toString(entity), "Executing '" + httpRequest.getURI() + "' failed: " + result.getStatusLine()));
              EntityUtils.consumeQuietly(entity);
            }
            else {

              CaravanHttpResponse response = new CaravanHttpResponseBuilder()
              .status(status.getStatusCode())
              .reason(status.getReasonPhrase())
              .headers(RequestUtil.toHeadersMap(result.getAllHeaders()))
              .body(entity.getContent(), entity.getContentLength() > 0 ? (int)entity.getContentLength() : null)
              .build();

              subscriber.onNext(response);
              subscriber.onCompleted();
            }
          }
          catch (Throwable ex) {
            subscriber.onError(new IOException("Reading response of '" + httpRequest.getURI() + "' failed", ex));
            EntityUtils.consumeQuietly(entity);
          }
        }
        catch (SocketTimeoutException ex) {
          subscriber.onError(new IOException("Socket timeout executing '" + httpRequest.getURI(), ex));
        }
        catch (IOException ex) {
          subscriber.onError(new IOException("Executing '" + httpRequest.getURI() + "' failed", ex));
        }
        finally {
          LOG.debug("Took {} ms to load {},\n{}", (System.currentTimeMillis() - start), httpRequest.getURI().toString(),
              request.getCorrelationId());
        }
      }
    });
  }

  private Throwable mapToKnownException(final CaravanHttpRequest request, final Throwable ex) {
    if (ex instanceof RequestFailedRuntimeException || ex instanceof IllegalResponseRuntimeException) {
      return ex;
    }
    if ((ex instanceof HystrixRuntimeException || ex instanceof ClientException) && ex.getCause() != null) {
      return mapToKnownException(request, ex.getCause());
    }
    throw new RequestFailedRuntimeException(request, StringUtils.defaultString(ex.getMessage(), ex.getClass().getSimpleName()), ex);
  }

  @Override
  public boolean hasValidConfiguration(String serviceId) {
    return CaravanHttpServiceConfigValidator.hasValidConfiguration(serviceId);
  }

}
