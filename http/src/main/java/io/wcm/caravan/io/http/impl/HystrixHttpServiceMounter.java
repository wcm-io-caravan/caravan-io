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

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

/**
 * Registers hystrix dashboard and metrix stream in HTTP service.
 */
@Component
public class HystrixHttpServiceMounter {

  private static final String HYSTRIX_URI_PREFIX = "/system/hystrix";
  private static final String HYSTRIX_DASHBOARD_URI = HYSTRIX_URI_PREFIX + "/dashboard";
  private static final String HYSTRIX_METRICS_STREAM_URI = HYSTRIX_URI_PREFIX + "/metricsStream";

  @Reference
  private HttpService httpService;

  @Activate
  protected void activate() throws NamespaceException, ServletException {
    // Mount some static resources from classpath of this bundle.
    httpService.registerResources(HYSTRIX_DASHBOARD_URI, "/hystrix-dashboard", null);

    // Mount hystrix metrix stream servlet to be used by hystrix dashboard.
    httpService.registerServlet(HYSTRIX_METRICS_STREAM_URI, new HystrixMetricsStreamServlet(), null, null);
  }

  @Deactivate
  protected void deactivate() {
    httpService.unregister(HYSTRIX_DASHBOARD_URI);
    httpService.unregister(HYSTRIX_METRICS_STREAM_URI);
  }

}
