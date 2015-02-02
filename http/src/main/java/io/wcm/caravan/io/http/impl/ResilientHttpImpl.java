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
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.response.Response;

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
 * Default implementation of {@link ResilientHttp}.
 */
@Component(immediate = true)
@Service(ResilientHttp.class)
public class ResilientHttpImpl implements ResilientHttp {

  @Reference
  private HttpClientFactory httpClientFactory;

  private static final Logger log = LoggerFactory.getLogger(ResilientHttpImpl.class);

  // replaces ClientFactory#getNamedLoadBalancer() - we don't want to use the static map of ILoadBalancerInstances
  // defined within ClientFactory, because that list will outlive our ResilientHttpImpl instance, and may
  // contain an outdated server list (especially in unit-tests)
  private LoadingCache<String, ILoadBalancer> namedLoadBalancers = CacheBuilder.newBuilder().build(new CacheLoader<String, ILoadBalancer>() {
    @Override
    public ILoadBalancer load(String key) throws Exception {
      IClientConfig clientConfig = ClientFactory.getNamedConfig(key, DefaultClientConfigImpl.class);
      String loadBalancerClassName = clientConfig.get(CommonClientConfigKey.NFLoadBalancerClassName);
      return (ILoadBalancer)ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, clientConfig);
    }
  });

  @Override
  public Observable<Response> execute(String serviceName, Request request) {
    return execute(serviceName, request, null);
  }

  @Override
  public Observable<Response> execute(String serviceName, Request request, Observable<Response> fallback) {
    return getHystrixObservable(serviceName, request, fallback)
        .onErrorResumeNext(exception -> Observable.<Response>error(mapToKnownException(serviceName, request, exception)));
  }

  private Throwable mapToKnownException(String serviceName, Request request, Throwable ex) {
    if (ex instanceof RequestFailedRuntimeException || ex instanceof IllegalResponseRuntimeException) {
      return ex;
    }
    if (ex instanceof HystrixRuntimeException && ex.getCause() != null) {
      return mapToKnownException(serviceName, request, ex.getCause());
    }
    throw new RequestFailedRuntimeException(serviceName, request,
        StringUtils.defaultString(ex.getMessage(), ex.getClass().getSimpleName()), ex);
  }

  private Observable<Response> getHystrixObservable(String serviceName, Request request, Observable<Response> fallback) {

    Observable<Response> ribbonObservable = getRibbonObservable(serviceName, request);

    // calling HttpHystrixCommand#observe() will immediately start the request, while #toObservable() will return a "lazy" Observable
    // that only initiates the request when a subscriber subscribes
    return new HttpHystrixCommand(serviceName, ribbonObservable, fallback).toObservable();
  }

  private Observable<Response> getRibbonObservable(String serviceName, Request request) {
    ILoadBalancer loadBalancer = namedLoadBalancers.getUnchecked(serviceName);

    LoadBalancerCommand<Response> command = LoadBalancerCommand.<Response>builder()
        .withLoadBalancer(loadBalancer)
        .build();

    ServerOperation<Response> operation = new ServerOperation<Response>() {
      @Override
      public Observable<Response> call(Server server) {
        return getHttpObservable(serviceName, RequestUtil.buildUrlPrefix(server), request);
      }
    };

    return command.submit(operation);
  }

  private Observable<Response> getHttpObservable(String serviceName, String urlPrefix, Request request) {
    return Observable.<Response>create(new Observable.OnSubscribe<Response>() {

      @Override
      public void call(final Subscriber<? super Response> subscriber) {
        HttpUriRequest httpRequest = RequestUtil.buildHttpRequest(urlPrefix, request);

        if (log.isDebugEnabled()) {
          log.debug("Execute: " + httpRequest.getURI() + "\n" + request.toString());
        }

        HttpAsyncClient httpClient = httpClientFactory.getAsync(httpRequest.getURI());
        httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {

          @Override
          public void completed(HttpResponse result) {
            StatusLine status = result.getStatusLine();
            HttpEntity entity = result.getEntity();
            try {
              if (status.getStatusCode() >= 500) {
                subscriber.onError(new IllegalResponseRuntimeException(serviceName, request,
                    httpRequest.getURI().toString(), status.getStatusCode(), EntityUtils.toString(entity),
                    "Executing '" + httpRequest.getURI() + "' failed: " + result.getStatusLine()));
                EntityUtils.consumeQuietly(entity);
              }
              else {
                Response response = Response.create(status.getStatusCode(), status.getReasonPhrase(),
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
