/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

import io.wcm.caravan.commons.httpasyncclient.HttpAsyncClientFactory;
import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import rx.Observable;
import rx.Subscriber;

/**
 * Simple implementation just executing the Apache HTTP client. Does not support a fallback.
 */
@Component(immediate = true)
@Service(ApacheHttpClient.class)
public class ApacheHttpClient implements CaravanHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(ApacheHttpClient.class);

  @Reference
  private HttpClientFactory httpClientFactory;

  @Reference
  private HttpAsyncClientFactory httpAsyncClientFactory;

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request) {
    return Observable.create(new Observable.OnSubscribe<CaravanHttpResponse>() {

      @Override
      public void call(final Subscriber<? super CaravanHttpResponse> subscriber) {
        HttpUriRequest httpRequest = RequestUtil.buildHttpRequest(request);

        if (LOG.isTraceEnabled()) {
          LOG.trace("Initiating request for {},\n{},\n{}", httpRequest.getURI(), request.toString(), request.getCorrelationId());
        }

        if (HttpHystrixCommand.getIsolationStrategy(request) == ExecutionIsolationStrategy.THREAD) {
          executeBlocking(subscriber, httpRequest);
        }
        else {
          executeAsync(subscriber, httpRequest);
        }
      }

      private void executeBlocking(final Subscriber<? super CaravanHttpResponse> subscriber, HttpUriRequest httpRequest) {

        if (LOG.isTraceEnabled()) {
          LOG.trace("Obtaining blocking http client to request " + httpRequest.getURI()
          + ", because a hystrixThreadPoolKeyOverride is configured for this serviceId");
        }

        CloseableHttpClient httpClient = (CloseableHttpClient)httpClientFactory.get(httpRequest.getURI());

        Stopwatch stopwatch = Stopwatch.createStarted();
        try (CloseableHttpResponse result = httpClient.execute(httpRequest)) {
          LOG.debug("Received response from {} in {} ms\n{}", httpRequest.getURI().toString(), stopwatch.elapsed(MILLISECONDS), request.getCorrelationId());

          processResponse(httpRequest, subscriber, result);

        }
        catch (Throwable ex) {
          LOG.info("Caught exception requesting {} after {} ms\n{}", httpRequest.getURI().toString(), stopwatch.elapsed(MILLISECONDS),
              request.getCorrelationId());

          processExeption(httpRequest, subscriber, ex);
        }
      }

      private void executeAsync(final Subscriber<? super CaravanHttpResponse> subscriber, HttpUriRequest httpRequest) {

        if (LOG.isTraceEnabled()) {
          LOG.trace("Obtaining async http client to request " + httpRequest.getURI()
          + ", because a hystrixThreadPoolKeyOverride is *not* configured for this serviceId");
        }

        CloseableHttpAsyncClient httpClient = (CloseableHttpAsyncClient)httpAsyncClientFactory.get(httpRequest.getURI());

        Stopwatch stopwatch = Stopwatch.createStarted();

        httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {

          @Override
          public void completed(HttpResponse result) {
            LOG.debug("Received response from {} in {} ms\n{}", httpRequest.getURI().toString(), stopwatch.elapsed(MILLISECONDS), request.getCorrelationId());

            processResponse(httpRequest, subscriber, result);

          }

          @Override
          public void failed(Exception ex) {
            LOG.info("Caught exception requesting {} after {} ms\n{}", httpRequest.getURI().toString(), stopwatch.elapsed(MILLISECONDS),
                request.getCorrelationId());

            processExeption(httpRequest, subscriber, ex);
          }

          @Override
          public void cancelled() {
            LOG.warn("Cancelled request for {} after {} ms\n{}", httpRequest.getURI().toString(), stopwatch.elapsed(MILLISECONDS), request.getCorrelationId());

            subscriber.onError(
                new RequestFailedRuntimeException(request, "The request was unexpectedly cancelled after " + stopwatch.elapsed(MILLISECONDS) + "ms", null));
          }

        });
      }

      void processExeption(HttpUriRequest httpRequest, Subscriber<? super CaravanHttpResponse> subscriber, Throwable ex) {
        if (ex instanceof SocketTimeoutException) {
          subscriber.onError(new IOException("Socket timeout requesting '" + httpRequest.getURI(), ex));
        }
        else if (ex instanceof IOException) {
          subscriber.onError(new IOException("Connection to '" + httpRequest.getURI() + "' failed", ex));
        }
        else {
          subscriber.onError(new IOException("Requesting '" + httpRequest.getURI() + "' failed", ex));
        }
      }

      void processResponse(HttpUriRequest httpRequest, final Subscriber<? super CaravanHttpResponse> subscriber, HttpResponse result) {

        try {
          StatusLine status = result.getStatusLine();
          HttpEntity entity = new BufferedHttpEntity(result.getEntity());
          EntityUtils.consume(entity);

          boolean throwExceptionForStatus500 = CaravanHttpServiceConfigValidator.throwExceptionForStatus500(request.getServiceId());
          if (status.getStatusCode() >= 500 && throwExceptionForStatus500) {
            IllegalResponseRuntimeException illegalResponseRuntimeException = new IllegalResponseRuntimeException(request,
                httpRequest.getURI().toString(),
                status.getStatusCode(),
                EntityUtils.toString(entity),
                "Executing '" + httpRequest.getURI() + "' failed: " + result.getStatusLine());

            subscriber.onError(illegalResponseRuntimeException);
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
        catch (IOException ex) {
          subscriber.onError(new IOException("Reading response of '" + httpRequest.getURI() + "' failed", ex));
        }
        // CHECKSTYLE:OFF - yes we really wan to catch all exceptions here
        catch (Exception ex) {
          // CHECKSTYLE:ON
          subscriber.onError(new IOException("Processing response of '" + httpRequest.getURI() + "' failed", ex));
        }
      }

    });
  }

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request, Observable<CaravanHttpResponse> fallback) {
    return execute(request);
  }

  @Override
  public boolean hasValidConfiguration(String serviceId) {
    return true;
  }

}
