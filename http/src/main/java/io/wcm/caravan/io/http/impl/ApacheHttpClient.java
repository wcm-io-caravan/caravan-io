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

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
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

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request) {
    return Observable.create(new Observable.OnSubscribe<CaravanHttpResponse>() {

      @Override
      public void call(final Subscriber<? super CaravanHttpResponse> subscriber) {
        HttpUriRequest httpRequest = RequestUtil.buildHttpRequest(request);

        if (LOG.isDebugEnabled()) {
          LOG.debug("Execute: {},\n{},\n{}", httpRequest.getURI(), request.toString(), request.getCorrelationId());
        }

        CloseableHttpClient httpClient = (CloseableHttpClient)httpClientFactory.get(httpRequest.getURI());

        long start = System.currentTimeMillis();
        try (CloseableHttpResponse result = httpClient.execute(httpRequest)) {

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
        catch (SocketTimeoutException ex) {
          subscriber.onError(new IOException("Socket timeout executing '" + httpRequest.getURI(), ex));
        }
        catch (IOException ex) {
          subscriber.onError(new IOException("Executing '" + httpRequest.getURI() + "' failed", ex));
        }
        catch (Throwable ex) {
          subscriber.onError(new IOException("Reading response of '" + httpRequest.getURI() + "' failed", ex));
        }
        finally {
          LOG.debug("Took {} ms to load {},\n{}", (System.currentTimeMillis() - start), httpRequest.getURI().toString(),
              request.getCorrelationId());
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
