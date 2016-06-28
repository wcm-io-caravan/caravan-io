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


import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * Configure to integrate the metrics to graphite server(Carbon) via TCP socket connection.
 *
 */
@Component(immediate = true, metatype = true,
  label = "wcm.io Caravan Resilient Http Graphite Integration Configuration",
  description = "Configure to integrate the metrics to a graphite server(Carbon) via TCP socket connection",
  policy = ConfigurationPolicy.OPTIONAL)
@Service(GraphiteIntegrationConfig.class)
public class GraphiteIntegrationConfig {

  @Property(label = "Graphite host name",
    description = "Send metrics to the graphite server, if the graphite server host and port is set. Otherwise do nothing."
  )
  public static final String HOST_NAME = "hostName";

  @Property(label = "Carbon tcp port to receive the metrics",
    description = "Send metrics to the graphite server, if the graphite server host and port is set. Otherwise do nothing.",
    intValue = 0
  )
  public static final String PORT = "port";

  @Property(label = "Time interval in seconds to push metrics to graphite server",
    description = "How often(in seconds) should the metrics be pushed to the graphite server.",
    intValue = 5
  )
  public static final String PUSH_METRICS_INTERVAL = "pushInterval";

  private String graphiteHostName;
  private int graphiteSocketPort;
  private int pushInterval;

  @Activate
  void activate(Map<String, Object> config) {
    graphiteHostName = PropertiesUtil.toString(config.get(HOST_NAME), null);
    graphiteSocketPort = PropertiesUtil.toInteger(config.get(PORT), 0);
    pushInterval = PropertiesUtil.toInteger(config.get(PUSH_METRICS_INTERVAL), 5);
  }

  /**
   * @return true if all the necessary configuration is set.
   */
  public boolean isEnabled() {
    return StringUtils.isNotEmpty(graphiteHostName)
      && graphiteSocketPort > 0
      && pushInterval > 0;
  }

  public String getGraphiteHostName() {
    return graphiteHostName;
  }

  public int getGraphiteSocketPort() {
    return graphiteSocketPort;
  }

  public int getPushInterval() {
    return pushInterval;
  }
}
