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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures thread pool options for the transport layer.
 * The configuration is mapped to archaius configuration internally.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Resilient Http Thread Pool Configuration",
description = " Configures thread pool options for the transport layer.",
configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Property(name = "webconsole.configurationFactory.nameHint", value = "{threadPoolName}: {hystrixThreadpoolCoresize}")
public class ResilientHttpThreadPoolConfig {

  private static final Logger log = LoggerFactory.getLogger(ResilientHttpThreadPoolConfig.class);

  @Property(label = "Thread Pool Name", description = "Internal thread pool name.")
  static final String THREAD_POOL_NAME_PROPERTY = "threadPoolName";

  @Property(label = "Threadpool size",
      description = "Hystrix: Maximum number of HystrixCommands that can execute concurrently.")
  static final String HYSTRIX_THREADPOOL_CORESIZE_PROPERTY = "hystrixThreadpoolCoresize";
  static final int HYSTRIX_THREADPOOL_CORESIZE_DEFAULT = 10;

  @Property(label = "Max. Threadpool Queue Size",
      description = "Hystrix: Maximum queue size at which rejections will occur. Note: This property only applies at initialization time.")
  static final String HYSTRIX_THREADPOOL_MAXQUEUESIZE_PROPERTY = "hystrixThreadpoolMaxqueuesize";
  static final int HYSTRIX_THREADPOOL_MAXQUEUESIZE_DEFAULT = 4096;

  @Property(label = "Dynamic Threadpool Queue Size",
      description = "Hystrix: Artificial maximum queue size at which rejections will occur even if hystrixThreadpoolDefaultMaxqueuesize has not been reached.")
  static final String HYSTRIX_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD_PROPERTY = "hystrixThreadpoolQueuesizerejectionthreshold";
  static final int HYSTRIX_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD_DEFAULT = 4096;

  private static final String HYSTRIX_THREADPOOL_PREFIX = "hystrix.threadpool.";
  private static final String HYSTRIX_PARAM_THREADPOOL_CORESIZE = ".coreSize";
  private static final String HYSTRIX_PARAM_THREADPOOL_MAXQUEUESIZE = ".maxQueueSize";
  private static final String HYSTRIX_PARAM_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD = ".queueSizeRejectionThreshold";

  @Activate
  protected void activate(Map<String, Object> config) {
    String threadPoolName = getThreadPoolName(config);
    if (validateConfig(threadPoolName, config)) {
      setArchiausProperties(threadPoolName, config);
    }
  }

  @Deactivate
  protected void deactivate(Map<String, Object> config) {
    // clear configuration by writing empty properties
    String serviceName = getThreadPoolName(config);
    clearArchiausProperties(serviceName);
  }

  private String getThreadPoolName(Map<String, Object> config) {
    return PropertiesUtil.toString(config.get(THREAD_POOL_NAME_PROPERTY), null);
  }

  /**
   * Validates configuration
   * @param threadPoolName Thread pool name
   * @param config OSGi config
   */
  private boolean validateConfig(String threadPoolName, Map<String, Object> config) {
    if (StringUtils.isBlank(threadPoolName)) {
      log.warn("Invalid http thread pool configuration without thread pool name, ignoring.", threadPoolName);
      return false;
    }
    return true;
  }

  /**
   * Writes OSGi configuration to archaius configuration.
   * @param serviceName Service name
   * @param config OSGi config
   */
  private void setArchiausProperties(String serviceName, Map<String, Object> config) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();
    // thread pool size
    archaiusConfig.setProperty(HYSTRIX_THREADPOOL_PREFIX + serviceName + HYSTRIX_PARAM_THREADPOOL_CORESIZE,
        PropertiesUtil.toInteger(config.get(HYSTRIX_THREADPOOL_CORESIZE_PROPERTY), HYSTRIX_THREADPOOL_CORESIZE_DEFAULT));
    // maximum thread queue size
    archaiusConfig.setProperty(HYSTRIX_THREADPOOL_PREFIX + serviceName + HYSTRIX_PARAM_THREADPOOL_MAXQUEUESIZE,
        PropertiesUtil.toInteger(config.get(HYSTRIX_THREADPOOL_MAXQUEUESIZE_PROPERTY), HYSTRIX_THREADPOOL_MAXQUEUESIZE_DEFAULT));
    // dynamic thread queue size
    archaiusConfig.setProperty(HYSTRIX_THREADPOOL_PREFIX + serviceName + HYSTRIX_PARAM_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD,
        PropertiesUtil.toInteger(config.get(HYSTRIX_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD_PROPERTY), HYSTRIX_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD_DEFAULT));
  }

  /**
   * Removes OSGi configuration from archaius configuration.
   * @param serviceName Service name
   */
  private void clearArchiausProperties(String serviceName) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();
    archaiusConfig.clearProperty(HYSTRIX_THREADPOOL_PREFIX + serviceName + HYSTRIX_PARAM_THREADPOOL_CORESIZE);
    archaiusConfig.clearProperty(HYSTRIX_THREADPOOL_PREFIX + serviceName + HYSTRIX_PARAM_THREADPOOL_MAXQUEUESIZE);
    archaiusConfig.clearProperty(HYSTRIX_THREADPOOL_PREFIX + serviceName + HYSTRIX_PARAM_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD);
  }

}
