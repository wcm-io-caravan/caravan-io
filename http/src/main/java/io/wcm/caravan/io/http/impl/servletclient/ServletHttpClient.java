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
package io.wcm.caravan.io.http.impl.servletclient;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.RequestFailedRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

import com.google.common.collect.Sets;

/**
 * Client which executes {@link Servlet}s registered in the same server directly without HTTP. Ignores fallbacks.
 */
@Component
@Service(ServletHttpClient.class)
public class ServletHttpClient implements CaravanHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(ServletHttpClient.class);

  @Reference(referenceInterface = Servlet.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private final ConcurrentMap<String, Servlet> servlets = new ConcurrentSkipListMap<>();

  private final Set<String> failedServices = Sets.newHashSet();
  protected void bindServlet(Servlet servlet, Map<String, Object> config) {
    String serviceId = (String)config.get("alias");
    if (serviceId != null) {
      servlets.put(serviceId, servlet);
    }
  }

  protected void unbindServlet(Servlet servle, Map<String, Object> config) {
    String serviceId = (String)config.get("alias");
    if (serviceId != null) {
      servlets.remove(serviceId);
    }
  }

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request) {
    return Observable.just(request.getServiceId())
        .map(serviceId -> getServlet(serviceId))
        .map(servlet -> executeServlet(servlet, request));
  }

  @Override
  public Observable<CaravanHttpResponse> execute(CaravanHttpRequest request, Observable<CaravanHttpResponse> fallback) {
    return execute(request);
  }

  @Override
  public boolean hasValidConfiguration(String serviceId) {
    return servlets.containsKey(serviceId) && !failedServices.contains(serviceId);
  }

  private Servlet getServlet(String serviceId) {

    Servlet servlet = servlets.get(serviceId);
    if (servlet == null) {
      throw new IllegalStateException("No local servlet registered for " + serviceId);
    }
    return servlet;

  }

  private CaravanHttpResponse executeServlet(Servlet servlet, CaravanHttpRequest request) {

    LOG.debug("Execute: {},\n{}", request.toString(), request.getCorrelationId());
    HttpServletRequestMapper requestMapper = new HttpServletRequestMapper(request);
    HttpServletResponseMapper responseMapper = new HttpServletResponseMapper();
    try {
      servlet.service(requestMapper, responseMapper);
      return responseMapper.getResponse();
    }
    catch (NotSupportedByRequestMapperException ex) {
      failedServices.add(request.getServiceId());
      throw ex;
    }
    catch (ServletException | IOException ex) {
      throw new RequestFailedRuntimeException(request, ex.getMessage(), ex);
    }

  }

}
