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
package io.wcm.caravan.io.http.impl.ribbon;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.impl.ApacheHttpClient;
import io.wcm.caravan.io.http.impl.ArchaiusConfig;
import io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig;
import io.wcm.caravan.io.http.impl.CaravanHttpServiceConfigValidator;
import io.wcm.caravan.io.http.impl.RequestUtil;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import rx.Observable;

/**
 * Delegating implementation using Ribbon to determine full request URL.
 */
@Component(immediate = true)
@Service(RibbonHttpClient.class)
public class RibbonHttpClient implements CaravanHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(RibbonHttpClient.class);

  @Reference
  private LoadBalancerCommandFactory commandFactory;
  @Reference
  private ApacheHttpClient apacheHttpClient;

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request) {
    LoadBalancerCommand<CaravanHttpResponse> command = commandFactory.createCommand(request.getServiceId());
    ServerOperation<CaravanHttpResponse> operation = createServerOperation(request);
    return command.submit(operation);
  }

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request, Observable<CaravanHttpResponse> fallback) {
    return execute(request);
  }

  private ServerOperation<CaravanHttpResponse> createServerOperation(CaravanHttpRequest request) {
    return new ServerOperation<CaravanHttpResponse>() {

      @Override
      public Observable<CaravanHttpResponse> call(Server server) {
        LOG.trace("Use " + server.toString() + " to execute request for service " + request.getServiceId());
        String protocol = getProtocol();
        CaravanHttpRequest fullUrlRequest = createFullUrllRequest(server, protocol);
        return apacheHttpClient.execute(fullUrlRequest);
      }

      private String getProtocol() {

        if (StringUtils.isEmpty(request.getServiceId())) {
          return RequestUtil.PROTOCOL_AUTO;
        }
        return ArchaiusConfig.getConfiguration().getString(request.getServiceId() + CaravanHttpServiceConfig.HTTP_PARAM_PROTOCOL);

      }

      private CaravanHttpRequest createFullUrllRequest(Server server, String protocol) {

        String urlPrefix = RequestUtil.buildUrlPrefix(server, protocol);
        CaravanHttpRequestBuilder builder = new CaravanHttpRequestBuilder(request.getServiceId())
            .append(urlPrefix)
            .append(request.getUrl())
            .body(request.getBody(), request.getCharset())
            .method(request.getMethod());
        request.getHeaders().entries().stream()
            .forEach(entry -> builder.header(entry.getKey(), entry.getValue()));
        return builder.build();

      }

    };
  }

  @Override
  public boolean hasValidConfiguration(String serviceId) {
    return CaravanHttpServiceConfigValidator.hasValidConfiguration(serviceId);
  }

}
