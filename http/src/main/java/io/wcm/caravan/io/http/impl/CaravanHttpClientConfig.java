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

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * Caravan HTTP client configuration.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Resilient Http client configuration",
description = "Configures the Caravan Http client")
@Service(CaravanHttpClientConfig.class)
@Property(name = "webconsole.configurationFactory.nameHint", value = "Servlet Client enabled: {servletClientEnabled}")
public class CaravanHttpClientConfig {

  /**
   * ServletClient toggle
   */
  @Property(label = "Servlet client enabled",
      description = "Enables a faster HTTP client for services on the same application server by directly calling the service.",
      boolValue = CaravanHttpClientConfig.SERVLET_CLIENT_ENABLED_DEFAULT)
  public static final String SERVLET_CLIENT_ENABLED = "servletClientEnabled";
  private static final boolean SERVLET_CLIENT_ENABLED_DEFAULT = false;
  private boolean servletClientEnabled;

  @Activate
  protected void activate(Map<String, Object> config) {
    servletClientEnabled = PropertiesUtil.toBoolean(config.get(SERVLET_CLIENT_ENABLED), SERVLET_CLIENT_ENABLED_DEFAULT);
  }

  public boolean isServletClientEnabled() {
    return this.servletClientEnabled;
  }

}
