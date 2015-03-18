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
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;

/**
 * Default implementation of {@link CaravanHttpClient}.
 */
@Component(immediate = true)
@Service(CaravanHttpClient.class)
public class CaravanHttpClientImpl implements CaravanHttpClient {

  @Reference
  private HttpClientFactory httpClientFactory;

  private static final Logger LOG = LoggerFactory.getLogger(CaravanHttpClientImpl.class);

  /** a cached map of pre-configured LoadBalancerCommand instances for every logical service name */
  private final LoadingCache<String, LoadBalancerCommand<CaravanHttpResponse>> namedLoadBalancercommands = CacheBuilder.newBuilder().build(
      new CacheLoader<String, LoadBalancerCommand<CaravanHttpResponse>>() {

        @Override
        public LoadBalancerCommand<CaravanHttpResponse> load(String key) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
          IClientConfig clientConfig = ClientFactory.getNamedConfig(key, DefaultClientConfigImpl.class);
          String loadBalancerClassName = clientConfig.get(CommonClientConfigKey.NFLoadBalancerClassName);
          ILoadBalancer loadBalancer = (ILoadBalancer)ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, clientConfig);
          IClientConfig config = ClientFactory.getNamedConfig(key, DefaultClientConfigImpl.class);

          LoadBalancerCommand<CaravanHttpResponse> command = LoadBalancerCommand.<CaravanHttpResponse>builder()
              .withLoadBalancer(loadBalancer)
              .withClientConfig(config)
              .withRetryHandler(new CaravanLoadBalancerRetryHandler(config))
              .build();

          return command;
        }
      });

  @Override
  public Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request) {
    return execute(request, null);
  }

  @Override
  public Observable<CaravanHttpResponse> execute(final CaravanHttpRequest request, final Observable<CaravanHttpResponse> fallback) {
    Observable<CaravanHttpResponse> ribbon = request.getServiceName() != null ? getRibbonObservable(request) : getHttpObservable("", request);
    Observable<CaravanHttpResponse> hystrix = new HttpHystrixCommand(StringUtils.defaultString(request.getServiceName(), "UNKNOWN"), ribbon, fallback)
    .toObservable();
    return hystrix.onErrorResumeNext(exception -> Observable.<CaravanHttpResponse>error(mapToKnownException(request, exception)));
  }

  private Observable<CaravanHttpResponse> getRibbonObservable(final CaravanHttpRequest request) {
    LoadBalancerCommand<CaravanHttpResponse> command = namedLoadBalancercommands.getUnchecked(request.getServiceName());
    ServerOperation<CaravanHttpResponse> operation = new ServerOperation<CaravanHttpResponse>() {

      @Override
      public Observable<CaravanHttpResponse> call(Server server) {
        return getHttpObservable(RequestUtil.buildUrlPrefix(server), request);
      }
    };
    return command.submit(operation);
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

  private Observable<CaravanHttpResponse> getHttpObservable(final String urlPrefix, final CaravanHttpRequest request) {
    return Observable.<CaravanHttpResponse>create(new Observable.OnSubscribe<CaravanHttpResponse>() {

      @Override
      public void call(final Subscriber<? super CaravanHttpResponse> subscriber) {
        HttpUriRequest httpRequest = RequestUtil.buildHttpRequest(urlPrefix, request);

        if (LOG.isDebugEnabled()) {
          LOG.debug("Execute: " + httpRequest.getURI() + "\n" + request.toString());
        }

        HttpAsyncClient httpClient = httpClientFactory.getAsync(httpRequest.getURI());
        httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {

          @Override
          public void completed(HttpResponse result) {
            StatusLine status = result.getStatusLine();
            HttpEntity entity = result.getEntity();
            try {
              if (status.getStatusCode() >= 500) {
                subscriber.onError(new IllegalResponseRuntimeException(request, httpRequest.getURI().toString(), status.getStatusCode(), EntityUtils
                    .toString(entity), "Executing '" + httpRequest.getURI() + "' failed: " + result.getStatusLine()));
                EntityUtils.consumeQuietly(entity);
              }
              else {
                CaravanHttpResponse response = CaravanHttpResponse.create(status.getStatusCode(), status.getReasonPhrase(),
                    RequestUtil.toHeadersMap(result.getAllHeaders()),
                    entity.getContent(), entity.getContentLength() > 0 ? (int)entity.getContentLength() : null);
                subscriber.onNext(response);
                subscriber.onCompleted();
              }
            }
            catch (Throwable ex) {
              subscriber.onError(new IOException("Reading response of '" + httpRequest.getURI() + "' failed.", ex));
              EntityUtils.consumeQuietly(entity);
            }
          }

          @Override
          public void failed(Exception ex) {
            if (ex instanceof SocketTimeoutException) {
              subscriber.onError(new IOException("Socket timeout executing '" + httpRequest.getURI() + "'.", ex));
            }
            else {
              subscriber.onError(new IOException("Executing '" + httpRequest.getURI() + "' failed.", ex));
            }
          }

          @Override
          public void cancelled() {
            subscriber.onError(new IOException("Getting " + httpRequest.getURI() + " was cancelled."));
          }

        });
      }

    });
  }

}
