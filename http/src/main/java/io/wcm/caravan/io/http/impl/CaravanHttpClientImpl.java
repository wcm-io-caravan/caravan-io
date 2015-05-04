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

import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.impl.ribbon.CachingLoadBalancerFactory;
import io.wcm.caravan.io.http.impl.ribbon.DefaultLoadBalancerFactory;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerFactory;
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

  private final LoadBalancerFactory loadBalancerFactory = new CachingLoadBalancerFactory(new DefaultLoadBalancerFactory());

  @Override
  public Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request) {
    return execute(request, null);
  }

  @Override
  public Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request, final Observable<CaravanHttpResponse> fallback) {

    String serviceName = request.getServiceName();
    Observable<CaravanHttpResponse> httpRequest = StringUtils.isEmpty(serviceName) ? createHttpRequest("", request) : createRibbonRequest(request);
    ExecutionIsolationStrategy isolationStrategy = getIsolationStrategy(serviceName);
    Observable<CaravanHttpResponse> hystrixRequest = new HttpHystrixCommand(StringUtils.defaultString(serviceName, "UNKNOWN"), isolationStrategy, httpRequest,
        fallback).toObservable();
    return hystrixRequest.onErrorResumeNext(exception -> Observable.<CaravanHttpResponse> error(mapToKnownException(request, exception)));

  }

  private ExecutionIsolationStrategy getIsolationStrategy(String serviceName) {
    return loadBalancerFactory.isLocalRequest(serviceName) ? ExecutionIsolationStrategy.SEMAPHORE : ExecutionIsolationStrategy.THREAD;
  }

  private Observable<CaravanHttpResponse> createRibbonRequest(final CaravanHttpRequest request) {
    LoadBalancerCommand<CaravanHttpResponse> command = loadBalancerFactory.createCommand(request.getServiceName());
    ServerOperation<CaravanHttpResponse> operation = new ServerOperation<CaravanHttpResponse>() {

      @Override
      public Observable<CaravanHttpResponse> call(Server server) {
        String protcol = RequestUtil.PROTOCOL_AUTO;
        if (StringUtils.isNotEmpty(request.getServiceName())) {
          protcol = ArchaiusConfig.getConfiguration().getString(request.getServiceName() + ResilientHttpServiceConfig.HTTP_PARAM_PROTOCOL);
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
          LOG.debug("Execute: " + httpRequest.getURI() + "\n" + request.toString());
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
            subscriber.onError(new IOException("Reading response of '" + httpRequest.getURI() + "' failed.", ex));
            EntityUtils.consumeQuietly(entity);
          }
        }
        catch (SocketTimeoutException ex) {
          subscriber.onError(new IOException("Socket timeout executing '" + httpRequest.getURI() + "'.", ex));
        }
        catch (IOException ex) {
          subscriber.onError(new IOException("Executing '" + httpRequest.getURI() + "' failed.", ex));
        }
        finally {
          LOG.debug("Took " + (System.currentTimeMillis() - start) + "ms to load " + httpRequest.getURI().toString());
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
  public boolean hasValidConfiguration(String serviceName) {
    return ResilientHttpServiceConfigValidator.hasValidConfiguration(serviceName);
  }

}
