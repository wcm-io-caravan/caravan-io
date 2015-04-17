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
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures transport layer options for service access.
 * The configuration is mapped to archaius configuration internally.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Resilient Http Service Configuration",
description = "Configures transport layer options for service access.",
configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Property(name = "webconsole.configurationFactory.nameHint", value = "{serviceName}: {ribbonHosts}")
public class ResilientHttpServiceConfig {

  @Property(label = "Service Name", description = "Internal or external service name.")
  static final String SERVICE_NAME_PROPERTY = "serviceName";

  @Property(label = "Hosts",
      description = "Ribbon: List of hostnames/IP addresses and ports to use for service (if multiple are defined software " +
          "load balancing is applied). Example entry: 'host1:80'.",
          cardinality = Integer.MAX_VALUE)
  static final String RIBBON_HOSTS_PROPERTY = "ribbonHosts";

  @Property(label = "Protocol",
      description = "Choose between HTTP and HTTPS protocol for communicating with the Hosts. "
          + "If set to 'Auto' the protocol is detected automatically from the port number (443 and 8443 = HTTPS).",
          value = ResilientHttpServiceConfig.PROTOCOL_PROPERTY_DEFAULT,
          options = {
      @PropertyOption(name = RequestUtil.PROTOCOL_AUTO, value = "Auto"),
      @PropertyOption(name = RequestUtil.PROTOCOL_HTTP, value = "HTTP"),
      @PropertyOption(name = RequestUtil.PROTOCOL_HTTPS, value = "HTTPS")
  }
      )
  static final String PROTOCOL_PROPERTY = "http.protocol";
  static final String PROTOCOL_PROPERTY_DEFAULT = RequestUtil.PROTOCOL_AUTO;

  @Property(label = "Max. Auto Retries",
      description = "Ribbon: Max number of retries on the same server (excluding the first try).",
      intValue = ResilientHttpServiceConfig.RIBBON_MAXAUTORETRIES_DEFAULT)
  static final String RIBBON_MAXAUTORETRIES_PROPERTY = "ribbonMaxAutoRetries";
  static final int RIBBON_MAXAUTORETRIES_DEFAULT = 0;

  @Property(label = "Max. Auto Retries Next Server",
      description = "Ribbon: Max number of next servers to retry (excluding the first server).",
      intValue = ResilientHttpServiceConfig.RIBBON_MAXAUTORETRIESONSERVER_DEFAULT)
  static final String RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY = "ribbonMaxAutoRetriesNextServer";
  static final int RIBBON_MAXAUTORETRIESONSERVER_DEFAULT = 0;

  @Property(label = "Isolation Timeout",
      description = "Hystrix: Time in milliseconds after which the calling thread will timeout and walk away from the "
          + "HystrixCommand.run() execution and mark the HystrixCommand as a TIMEOUT and perform fallback logic.",
          intValue = ResilientHttpServiceConfig.HYSTRIX_TIMEOUT_MS_DEFAULT)
  static final String HYSTRIX_TIMEOUT_MS_PROPERTY = "hystrixTimeoutMs";
  static final int HYSTRIX_TIMEOUT_MS_DEFAULT = 120000;

  @Property(label = "Fallback",
      description = "Hystrix: Whether HystrixCommand.getFallback() will be attempted when failure or rejection occurs.",
      boolValue = ResilientHttpServiceConfig.HYSTRIX_FALLBACK_ENABLED_DEFAULT)
  static final String HYSTRIX_FALLBACK_ENABLED_PROPERTY = "hystrixFallbackEnabled";
  static final boolean HYSTRIX_FALLBACK_ENABLED_DEFAULT = true;

  @Property(label = "Circuit Breaker",
      description = "Hystrix: Whether a circuit breaker will be used to track health and short-circuit requests if it trips.",
      boolValue = ResilientHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_ENABLED_DEFAULT)
  static final String HYSTRIX_CIRCUITBREAKER_ENABLED_PROPERTY = "hystrixCircuitBreakerEnabled";
  static final boolean HYSTRIX_CIRCUITBREAKER_ENABLED_DEFAULT = true;

  @Property(label = "Request Volume Threshold",
      description = "Hystrix: Circuit Breaker - Minimum number of requests in rolling window needed before tripping the circuit will occur.",
      intValue = ResilientHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_DEFAULT)
  static final String HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_PROPERTY = "hystrixCircuitBreakerRequestVolumeThreshold";
  static final int HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_DEFAULT = 20;

  @Property(label = "Sleep Window",
      description = "Hystrix: Circuit Breaker - After tripping the circuit how long in milliseconds to reject requests before allowing "
          + "attempts again to determine if the circuit should be closed.",
          intValue = ResilientHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_DEFAULT)
  static final String HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_PROPERTY = "hystrixCircuitBreakerSleepWindowMs";
  static final int HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_DEFAULT = 5000;

  @Property(label = "Error Threshold Percentage",
      description = "Hystrix: Circuit Breaker - Error percentage at which the circuit should trip open and start short-circuiting "
          + "requests to fallback logic.",
          intValue = ResilientHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_DEFAULT)
  static final String HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_PROPERTY = "hystrixCircuitBreakerErrorThresholdPercentage";
  static final int HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_DEFAULT = 50;

  @Property(label = "Force Open",
      description = "Hystrix: Circuit Breaker - If true the circuit breaker will be forced open (tripped) and reject all requests.",
      boolValue = ResilientHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_FORCEOPEN_DEFAULT)
  static final String HYSTRIX_CIRCUITBREAKER_FORCEOPEN_PROPERTY = "hystrixCircuitBreakerForceOpen";
  static final boolean HYSTRIX_CIRCUITBREAKER_FORCEOPEN_DEFAULT = false;

  @Property(label = "Force Closed",
      description = "Hystrix: Circuit Breaker - If true the circuit breaker will remain closed and allow requests regardless of the error percentage.",
      boolValue = ResilientHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_FORCECLOSED_DEFAULT)
  static final String HYSTRIX_CIRCUITBREAKER_FORCECLOSED_PROPERTY = "hystrixCircuitBreakerForceClosed";
  static final boolean HYSTRIX_CIRCUITBREAKER_FORCECLOSED_DEFAULT = false;

  @Property(label = "Thread Pool Name",
      description = "Hystrix: Overrides the default thread pool for the service")
  static final String HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY = "hystrixThreadPoolKeyOverride";

  private static final Logger log = LoggerFactory.getLogger(ResilientHttpServiceConfig.class);

  private static final String RIBBON_PARAM_LISTOFSERVERS = ".ribbon.listOfServers";
  private static final String RIBBON_PARAM_MAXAUTORETRIES = ".ribbon.MaxAutoRetries";
  private static final String RIBBON_PARAM_MAXAUTORETRIESONSERVER = ".ribbon.MaxAutoRetriesNextServer";
  private static final String RIBBON_PARAM_OKTORETRYONALLOPERATIONS = ".ribbon.OkToRetryOnAllOperations";

  private static final String HYSTRIX_COMMAND_PREFIX = "hystrix.command.";
  private static final String HYSTRIX_PARAM_TIMEOUT_MS = ".execution.isolation.thread.timeoutInMilliseconds";
  private static final String HYSTRIX_PARAM_FALLBACK_ENABLED = ".fallback.enabled";
  private static final String HYSTRIX_PARAM_CIRCUITBREAKER_ENABLED = ".circuitBreaker.enabled";
  private static final String HYSTRIX_PARAM_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD = ".circuitBreaker.requestVolumeThreshold";
  private static final String HYSTRIX_PARAM_CIRCUITBREAKER_SLEEPWINDOW_MS = ".circuitBreaker.sleepWindowInMilliseconds";
  private static final String HYSTRIX_PARAM_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE = ".circuitBreaker.errorThresholdPercentage";
  private static final String HYSTRIX_PARAM_CIRCUITBREAKER_FORCEOPEN = ".circuitBreaker.forceOpen";
  private static final String HYSTRIX_PARAM_CIRCUITBREAKER_FORCECLOSED = ".circuitBreaker.forceClosed";
  private static final String HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE = ".threadPoolKeyOverride";

  /**
   * Custom archiaus property for protocol detection
   */
  public static final String HTTP_PARAM_PROTOCOL = ".http.protocol";

  @Activate
  protected void activate(Map<String, Object> config) {
    String serviceName = getServiceName(config);
    if (validateConfig(serviceName, config)) {
      setArchiausProperties(serviceName, config);
    }
  }

  @Deactivate
  protected void deactivate(Map<String, Object> config) {
    // clear configuration by writing empty properties
    String serviceName = getServiceName(config);
    clearArchiausProperties(serviceName);
  }

  private String getServiceName(Map<String, Object> config) {
    return PropertiesUtil.toString(config.get(SERVICE_NAME_PROPERTY), null);
  }

  /**
   * Validates configuration
   * @param serviceName Service name
   * @param config OSGi config
   */
  private boolean validateConfig(String serviceName, Map<String, Object> config) {
    if (StringUtils.isBlank(serviceName)) {
      log.warn("Invalid transport layer service configuration without service name, ignoring.", serviceName);
      return false;
    }
    String[] hosts = PropertiesUtil.toStringArray(config.get(RIBBON_HOSTS_PROPERTY));
    if (hosts == null || hosts.length == 0) {
      log.warn("Invalid transport layer service configuration for '{}' without hosts, ignoring.", serviceName);
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

    // ribbon parameters
    archaiusConfig.setProperty(serviceName + RIBBON_PARAM_LISTOFSERVERS,
        StringUtils.join(PropertiesUtil.toStringArray(config.get(RIBBON_HOSTS_PROPERTY), new String[0]), ","));
    archaiusConfig.setProperty(serviceName + RIBBON_PARAM_MAXAUTORETRIES,
        PropertiesUtil.toInteger(config.get(RIBBON_MAXAUTORETRIES_PROPERTY), RIBBON_MAXAUTORETRIES_DEFAULT));
    archaiusConfig.setProperty(serviceName + RIBBON_PARAM_MAXAUTORETRIESONSERVER,
        PropertiesUtil.toInteger(config.get(RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY), RIBBON_MAXAUTORETRIESONSERVER_DEFAULT));
    archaiusConfig.setProperty(serviceName + RIBBON_PARAM_OKTORETRYONALLOPERATIONS, "true");

    // hystrix parameters
    archaiusConfig.setProperty("hystrix.threadpool.default.maxQueueSize", ResilientHttpThreadPoolConfig.HYSTRIX_THREADPOOL_MAXQUEUESIZE_DEFAULT);
    archaiusConfig.setProperty("hystrix.threadpool.default.queueSizeRejectionThreshold",
        ResilientHttpThreadPoolConfig.HYSTRIX_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD_DEFAULT);

    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_TIMEOUT_MS,
        PropertiesUtil.toInteger(config.get(HYSTRIX_TIMEOUT_MS_PROPERTY), HYSTRIX_TIMEOUT_MS_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_FALLBACK_ENABLED,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_FALLBACK_ENABLED_PROPERTY), HYSTRIX_FALLBACK_ENABLED_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_ENABLED,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_CIRCUITBREAKER_ENABLED_PROPERTY), HYSTRIX_CIRCUITBREAKER_ENABLED_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD,
        PropertiesUtil.toInteger(config.get(HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_PROPERTY), HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_SLEEPWINDOW_MS,
        PropertiesUtil.toInteger(config.get(HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_PROPERTY), HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE,
        PropertiesUtil.toInteger(config.get(HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_PROPERTY),
            HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_FORCEOPEN,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_CIRCUITBREAKER_FORCEOPEN_PROPERTY), HYSTRIX_CIRCUITBREAKER_FORCEOPEN_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_FORCECLOSED,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_CIRCUITBREAKER_FORCECLOSED_PROPERTY), HYSTRIX_CIRCUITBREAKER_FORCECLOSED_DEFAULT));
    if (config.get(HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY) != null) {
      // thread pool name
      archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE,
          config.get(HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY));
    }

    // others
    archaiusConfig.setProperty(serviceName + HTTP_PARAM_PROTOCOL, PropertiesUtil.toString(config.get(PROTOCOL_PROPERTY), PROTOCOL_PROPERTY_DEFAULT));
  }

  /**
   * Removes OSGi configuration from archaius configuration.
   * @param serviceName Service name
   */
  private void clearArchiausProperties(String serviceName) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();

    // ribbon parameters
    archaiusConfig.clearProperty(serviceName + RIBBON_PARAM_LISTOFSERVERS);
    archaiusConfig.clearProperty(serviceName + RIBBON_PARAM_MAXAUTORETRIES);
    archaiusConfig.clearProperty(serviceName + RIBBON_PARAM_MAXAUTORETRIESONSERVER);
    archaiusConfig.clearProperty(serviceName + RIBBON_PARAM_OKTORETRYONALLOPERATIONS);

    // hystrix parameters
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_TIMEOUT_MS);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_FALLBACK_ENABLED);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_ENABLED);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_SLEEPWINDOW_MS);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_FORCEOPEN);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_CIRCUITBREAKER_FORCECLOSED);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceName + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE);

    // others
    archaiusConfig.clearProperty(serviceName + HTTP_PARAM_PROTOCOL);
  }

}
