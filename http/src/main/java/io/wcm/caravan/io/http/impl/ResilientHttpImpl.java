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

import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.ResilientHttpRuntimeException;
import io.wcm.caravan.io.http.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.response.Response;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.servlet.http.HttpServletResponse;

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

import com.netflix.client.ClientFactory;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.CommandBuilder;
import com.netflix.loadbalancer.reactive.LoadBalancerObservable;
import com.netflix.loadbalancer.reactive.LoadBalancerObservableCommand;

/**
 * Default implementation of {@link ResilientHttp}.
 */
@Component(immediate = true)
@Service(ResilientHttp.class)
public class ResilientHttpImpl implements ResilientHttp {

  @Reference
  private HttpClientFactory httpClientFactory;

  private static final Logger log = LoggerFactory.getLogger(ResilientHttpImpl.class);

  @Override
  public Observable<Response> execute(String serviceName, Request request) {
    return execute(serviceName, request, null);
  }

  @Override
  public Observable<Response> execute(String serviceName, Request request, Observable<Response> fallback) {
    return getHystrixObservable(serviceName, request, fallback)
        .onErrorResumeNext(exception -> Observable.<Response>error(mapToTransportLayerRuntimeException(serviceName, request, exception)));
  }

  private ResilientHttpRuntimeException mapToTransportLayerRuntimeException(String serviceName, Request request, Throwable ex) {
    if (ex instanceof HystrixRuntimeException) {
      return mapToTransportLayerRuntimeException(serviceName, request, ex.getCause());
    }
    throw new ResilientHttpRuntimeException(serviceName, request, ex.getMessage(), ex);
  }

  private Observable<Response> getHystrixObservable(String serviceName, Request request, Observable<Response> fallback) {

    // calling HttpHystrixCommand#observe() will immediately start the request,
    // which is not what we want in the context of the JsonPipeline. Instead all network activity should be postponed
    // until someone subscribes to the observable returned by #execute 
    //  - that's why we add another layer of indirection by wrapping the hystrix observable's in another simple Observable

    return Observable.create(subscriber -> {
      Observable<Response> ribbonObservable = getRibbonObservable(serviceName, request);
      new HttpHystrixCommand(serviceName, ribbonObservable, fallback).observe().subscribe(subscriber);
    });
  }

  private Observable<Response> getRibbonObservable(String serviceName, Request request) {
    ILoadBalancer loadBalancer = ClientFactory.getNamedLoadBalancer(serviceName);
    LoadBalancerObservableCommand<Response> command = CommandBuilder.<Response>newBuilder()
        .withLoadBalancer(loadBalancer)
        .build(new LoadBalancerObservable<Response>() {
          @Override
          public Observable<Response> call(Server server) {
            return getHttpObservable(RequestUtil.buildUrlPrefix(server), request);
          }
        });
    return command.toObservable();
  }

  private Observable<Response> getHttpObservable(String urlPrefix, Request request) {
    return Observable.<Response>create(new Observable.OnSubscribe<Response>() {

      @Override
      public void call(final Subscriber<? super Response> subscriber) {
        HttpUriRequest httpRequest = RequestUtil.buildHttpRequest(urlPrefix, request);

        if (log.isDebugEnabled()) {
          log.debug("Execute: " + httpRequest.getURI() + "\n" + request.toString());
        }

        HttpAsyncClient httpClient = httpClientFactory.getHttpAsyncClient(httpRequest.getURI());
        httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {

          @Override
          public void completed(HttpResponse result) {
            StatusLine status = result.getStatusLine();
            HttpEntity entity = result.getEntity();
            try {
              if (status.getStatusCode() >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                subscriber.onError(new IOException("Executing '" + httpRequest.getURI() + "' failed: " + result.getStatusLine() + "\n"
                    + EntityUtils.toString(entity)));
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
            catch (IOException ex) {
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
