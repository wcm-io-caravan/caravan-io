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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.io.http.impl.ribbon.CachingLoadBalancerFactory;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerFactory;

/**
 * Configures transport layer options for service access.
 * The configuration is mapped to archaius configuration internally.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Resilient Http Service Configuration",
description = "Configures transport layer options for service access.",
configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Property(name = "webconsole.configurationFactory.nameHint", value = "{serviceId}{serviceName}: {ribbonHosts}")
public class CaravanHttpServiceConfig {

  @Reference(target = "(type=" + LoadBalancerFactory.CACHING + ")")
  private LoadBalancerFactory loadBalancerFactory;

  /**
   * Service ID
   */
  @Property(label = "Service ID", description = "Internal or external service identifier.")
  public static final String SERVICE_ID_PROPERTY = "serviceId";
  private static final String DEPRECATED_SERVICE_NAME_PROPERTY = "serviceName";

  static final boolean THROW_EXCEPTION_FOR_STATUS_500_DEFAULT = true;

  /**
   * Throw exception for response status > 500
   */
  @Property(label = "Throw exception for response status > 500",
    description = "If true, responses with status > 500 will be handled as error(hystrix failures). If the value is set to false, "
      + "the caller service will has to handle the failure itself.",
    boolValue = THROW_EXCEPTION_FOR_STATUS_500_DEFAULT
  )
  public static final String THROW_EXCEPTION_FOR_STATUS_500 = "exceptionForResponseStatus500";

  /**
   * Hosts
   */
  @Property(label = "Hosts",
      description = "Ribbon: List of hostnames/IP addresses and ports to use for service (if multiple are defined software "
          + "load balancing is applied). Optionally you can add a protocol as well. If you have mutliple entries "
          + "all have to use the same protocol. Example entry: 'http://host1:8080'.",
          cardinality = Integer.MAX_VALUE)
  public static final String RIBBON_HOSTS_PROPERTY = "ribbonHosts";

  /**
   * Protocol
   */
  @Property(label = "Protocol",
      description = "Choose between HTTP and HTTPS protocol for communicating with the Hosts. "
          + "If set to 'Auto' the protocol is detected automatically from the port number (443 and 8443 = HTTPS).",
          value = CaravanHttpServiceConfig.PROTOCOL_PROPERTY_DEFAULT,
          options = {
              @PropertyOption(name = RequestUtil.PROTOCOL_AUTO, value = "Auto"),
              @PropertyOption(name = RequestUtil.PROTOCOL_HTTP, value = "HTTP"),
              @PropertyOption(name = RequestUtil.PROTOCOL_HTTPS, value = "HTTPS")
  })
  public static final String PROTOCOL_PROPERTY = "http.protocol";
  static final String PROTOCOL_PROPERTY_DEFAULT = RequestUtil.PROTOCOL_AUTO;

  /**
   * Max. Auto Retries
   */
  @Property(label = "Max. Auto Retries",
      description = "Ribbon: Max number of retries on the same server (excluding the first try).",
      intValue = CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIES_DEFAULT)
  public static final String RIBBON_MAXAUTORETRIES_PROPERTY = "ribbonMaxAutoRetries";
  static final int RIBBON_MAXAUTORETRIES_DEFAULT = 0;

  /**
   * Max. Auto Retries Next Server
   */
  @Property(label = "Max. Auto Retries Next Server",
      description = "Ribbon: Max number of next servers to retry (excluding the first server).",
      intValue = CaravanHttpServiceConfig.RIBBON_MAXAUTORETRIESONSERVER_DEFAULT)
  public static final String RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY = "ribbonMaxAutoRetriesNextServer";
  static final int RIBBON_MAXAUTORETRIESONSERVER_DEFAULT = 0;

  /**
   * Isolation Timeout
   */
  @Property(label = "Isolation Timeout",
      description = "Hystrix: Time in milliseconds after which the calling thread will timeout and walk away from the "
          + "HystrixCommand.run() execution and mark the HystrixCommand as a TIMEOUT and perform fallback logic.",
          intValue = CaravanHttpServiceConfig.HYSTRIX_TIMEOUT_MS_DEFAULT)
  public static final String HYSTRIX_TIMEOUT_MS_PROPERTY = "hystrixTimeoutMs";
  static final int HYSTRIX_TIMEOUT_MS_DEFAULT = 120000;

  /**
   * Fallback
   */
  @Property(label = "Fallback",
      description = "Hystrix: Whether HystrixCommand.getFallback() will be attempted when failure or rejection occurs.",
      boolValue = CaravanHttpServiceConfig.HYSTRIX_FALLBACK_ENABLED_DEFAULT)
  public static final String HYSTRIX_FALLBACK_ENABLED_PROPERTY = "hystrixFallbackEnabled";
  static final boolean HYSTRIX_FALLBACK_ENABLED_DEFAULT = true;

  /**
   * Circuit Breaker
   */
  @Property(label = "Circuit Breaker",
      description = "Hystrix: Whether a circuit breaker will be used to track health and short-circuit requests if it trips.",
      boolValue = CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_ENABLED_DEFAULT)
  public static final String HYSTRIX_CIRCUITBREAKER_ENABLED_PROPERTY = "hystrixCircuitBreakerEnabled";
  static final boolean HYSTRIX_CIRCUITBREAKER_ENABLED_DEFAULT = true;

  /**
   * Request Volume Threshold
   */
  @Property(label = "Request Volume Threshold",
      description = "Hystrix: Circuit Breaker - Minimum number of requests in rolling window needed before tripping the circuit will occur.",
      intValue = CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_DEFAULT)
  public static final String HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_PROPERTY = "hystrixCircuitBreakerRequestVolumeThreshold";
  static final int HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_DEFAULT = 20;

  /**
   * Sleep Window
   */
  @Property(label = "Sleep Window",
      description = "Hystrix: Circuit Breaker - After tripping the circuit how long in milliseconds to reject requests before allowing "
          + "attempts again to determine if the circuit should be closed.",
          intValue = CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_DEFAULT)
  public static final String HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_PROPERTY = "hystrixCircuitBreakerSleepWindowMs";
  static final int HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_DEFAULT = 5000;

  /**
   * Error Threshold Percentage
   */
  @Property(label = "Error Threshold Percentage",
      description = "Hystrix: Circuit Breaker - Error percentage at which the circuit should trip open and start short-circuiting "
          + "requests to fallback logic.",
          intValue = CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_DEFAULT)
  public static final String HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_PROPERTY = "hystrixCircuitBreakerErrorThresholdPercentage";
  static final int HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_DEFAULT = 50;

  /**
   * Force Open
   */
  @Property(label = "Force Open",
      description = "Hystrix: Circuit Breaker - If true the circuit breaker will be forced open (tripped) and reject all requests.",
      boolValue = CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_FORCEOPEN_DEFAULT)
  public static final String HYSTRIX_CIRCUITBREAKER_FORCEOPEN_PROPERTY = "hystrixCircuitBreakerForceOpen";
  static final boolean HYSTRIX_CIRCUITBREAKER_FORCEOPEN_DEFAULT = false;

  /**
   * Force Closed
   */
  @Property(label = "Force Closed",
      description = "Hystrix: Circuit Breaker - If true the circuit breaker will remain closed and allow requests regardless of the error percentage.",
      boolValue = CaravanHttpServiceConfig.HYSTRIX_CIRCUITBREAKER_FORCECLOSED_DEFAULT)
  public static final String HYSTRIX_CIRCUITBREAKER_FORCECLOSED_PROPERTY = "hystrixCircuitBreakerForceClosed";
  static final boolean HYSTRIX_CIRCUITBREAKER_FORCECLOSED_DEFAULT = false;

  @Property(label = "Thread Pool Name",
      description = "Hystrix: Overrides the default thread pool for the service")
  static final String HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY = "hystrixThreadPoolKeyOverride";

  static final String RIBBON_PARAM_LISTOFSERVERS = ".ribbon.listOfServers";
  static final String RIBBON_PARAM_MAXAUTORETRIES = ".ribbon.MaxAutoRetries";
  static final String RIBBON_PARAM_MAXAUTORETRIESONSERVER = ".ribbon.MaxAutoRetriesNextServer";
  static final String RIBBON_PARAM_OKTORETRYONALLOPERATIONS = ".ribbon.OkToRetryOnAllOperations";

  static final String HYSTRIX_COMMAND_PREFIX = "hystrix.command.";
  static final String HYSTRIX_PARAM_TIMEOUT_MS = ".execution.isolation.thread.timeoutInMilliseconds";
  static final String HYSTRIX_PARAM_FALLBACK_ENABLED = ".fallback.enabled";
  static final String HYSTRIX_PARAM_CIRCUITBREAKER_ENABLED = ".circuitBreaker.enabled";
  static final String HYSTRIX_PARAM_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD = ".circuitBreaker.requestVolumeThreshold";
  static final String HYSTRIX_PARAM_CIRCUITBREAKER_SLEEPWINDOW_MS = ".circuitBreaker.sleepWindowInMilliseconds";
  static final String HYSTRIX_PARAM_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE = ".circuitBreaker.errorThresholdPercentage";
  static final String HYSTRIX_PARAM_CIRCUITBREAKER_FORCEOPEN = ".circuitBreaker.forceOpen";
  static final String HYSTRIX_PARAM_CIRCUITBREAKER_FORCECLOSED = ".circuitBreaker.forceClosed";
  static final String HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE = ".threadPoolKeyOverride";

  /**
   * Custom archiaus property for protocol detection
   */
  public static final String HTTP_PARAM_PROTOCOL = ".http.protocol";

  static final String LIST_SEPARATOR = ",";

  private static final Logger log = LoggerFactory.getLogger(CaravanHttpServiceConfig.class);

  @Activate
  protected void activate(Map<String, Object> config) {
    String serviceId = getServiceId(config);
    if (CaravanHttpServiceConfigValidator.isValidServiceConfig(serviceId, config)) {
      setArchiausProperties(serviceId, config);
    }
  }

  @Deactivate
  protected void deactivate(Map<String, Object> config) {
    // clear configuration by writing empty properties
    String serviceId = getServiceId(config);
    clearArchiausProperties(serviceId);
    // remove load balancer from caching factory
    if (loadBalancerFactory != null && loadBalancerFactory instanceof CachingLoadBalancerFactory) {
      ((CachingLoadBalancerFactory)loadBalancerFactory).unregister(serviceId);
    }
  }

  private String getServiceId(Map<String, Object> config) {
    return PropertiesUtil.toString(config.get(SERVICE_ID_PROPERTY),
        PropertiesUtil.toString(config.get(DEPRECATED_SERVICE_NAME_PROPERTY), null));
  }

  /**
   * Writes OSGi configuration to archaius configuration.
   * @param serviceId Service ID
   * @param config OSGi config
   */
  private void setArchiausProperties(String serviceId, Map<String, Object> config) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();

    // ribbon parameters
    archaiusConfig.setProperty(serviceId + RIBBON_PARAM_LISTOFSERVERS,
        StringUtils.join(PropertiesUtil.toStringArray(config.get(RIBBON_HOSTS_PROPERTY), new String[0]), LIST_SEPARATOR));
    archaiusConfig.setProperty(serviceId + RIBBON_PARAM_MAXAUTORETRIES,
        PropertiesUtil.toInteger(config.get(RIBBON_MAXAUTORETRIES_PROPERTY), RIBBON_MAXAUTORETRIES_DEFAULT));
    archaiusConfig.setProperty(serviceId + RIBBON_PARAM_MAXAUTORETRIESONSERVER,
        PropertiesUtil.toInteger(config.get(RIBBON_MAXAUTORETRIESNEXTSERVER_PROPERTY), RIBBON_MAXAUTORETRIESONSERVER_DEFAULT));
    archaiusConfig.setProperty(serviceId + RIBBON_PARAM_OKTORETRYONALLOPERATIONS, "true");

    // hystrix parameters
    archaiusConfig.setProperty("hystrix.threadpool.default.maxQueueSize", CaravanHttpThreadPoolConfig.HYSTRIX_THREADPOOL_MAXQUEUESIZE_DEFAULT);
    archaiusConfig.setProperty("hystrix.threadpool.default.queueSizeRejectionThreshold",
        CaravanHttpThreadPoolConfig.HYSTRIX_THREADPOOL_QUEUESIZEREJECTIONTHRESHOLD_DEFAULT);

    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_TIMEOUT_MS,
        PropertiesUtil.toInteger(config.get(HYSTRIX_TIMEOUT_MS_PROPERTY), HYSTRIX_TIMEOUT_MS_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_FALLBACK_ENABLED,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_FALLBACK_ENABLED_PROPERTY), HYSTRIX_FALLBACK_ENABLED_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_ENABLED,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_CIRCUITBREAKER_ENABLED_PROPERTY), HYSTRIX_CIRCUITBREAKER_ENABLED_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD,
        PropertiesUtil.toInteger(config.get(HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_PROPERTY), HYSTRIX_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_SLEEPWINDOW_MS,
        PropertiesUtil.toInteger(config.get(HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_PROPERTY), HYSTRIX_CIRCUITBREAKER_SLEEPWINDOW_MS_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE,
        PropertiesUtil.toInteger(config.get(HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_PROPERTY),
            HYSTRIX_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_FORCEOPEN,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_CIRCUITBREAKER_FORCEOPEN_PROPERTY), HYSTRIX_CIRCUITBREAKER_FORCEOPEN_DEFAULT));
    archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_FORCECLOSED,
        PropertiesUtil.toBoolean(config.get(HYSTRIX_CIRCUITBREAKER_FORCECLOSED_PROPERTY), HYSTRIX_CIRCUITBREAKER_FORCECLOSED_DEFAULT));
    if (config.get(HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY) != null) {
      // thread pool name
      archaiusConfig.setProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE,
        config.get(HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY));
    }

    // others
    archaiusConfig.setProperty(serviceId + HTTP_PARAM_PROTOCOL, PropertiesUtil.toString(config.get(PROTOCOL_PROPERTY), PROTOCOL_PROPERTY_DEFAULT));
    archaiusConfig.setProperty(serviceId + THROW_EXCEPTION_FOR_STATUS_500, PropertiesUtil.toBoolean(config.get(THROW_EXCEPTION_FOR_STATUS_500),
      THROW_EXCEPTION_FOR_STATUS_500_DEFAULT));

    // update protocol to be used
    applyRibbonHostsProcotol(serviceId);
  }

  /**
   * Checks if protocols are defined in the ribbon "listOfServers" properties, which is not supported by ribbon itself.
   * If this is the case, remove them and set our custom "http.protocol" property instead to the protocol, if
   * it is set to "auto".
   */
  private void applyRibbonHostsProcotol(String serviceId) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();

    String[] listOfServers = archaiusConfig.getStringArray(serviceId + RIBBON_PARAM_LISTOFSERVERS);
    String protocolForAllServers = archaiusConfig.getString(serviceId + HTTP_PARAM_PROTOCOL);

    // get protocols defined in servers
    Set<String> protocolsFromListOfServers = Arrays.stream(listOfServers)
        .filter(server -> StringUtils.contains(server, "://"))
        .map(server -> StringUtils.substringBefore(server, "://"))
        .collect(Collectors.toSet());

    // skip further processing of no protocols defined
    if (protocolsFromListOfServers.isEmpty()) {
      return;
    }

    // ensure that only one protocol is defined. if not use the first one and write a warning to the log files.
    String protocol = new TreeSet<String>(protocolsFromListOfServers).iterator().next();
    if (protocolsFromListOfServers.size() > 1) {
      log.warn("Different protocols are defined for property {}: {}. Only protocol '{}' is used.",
          RIBBON_HOSTS_PROPERTY, StringUtils.join(listOfServers, LIST_SEPARATOR), protocol);
    }

    // if http protocol is not set to "auto" write a warning as well, because protocol is defined in server list as well
    if (!(StringUtils.equals(protocolForAllServers, RequestUtil.PROTOCOL_AUTO)
        || StringUtils.equals(protocolForAllServers, protocol))) {
      log.warn("Protocol '{}' is defined for property {}: {}, but an other protocol is defined in the server list: {}. Only protocol '{}' is used.",
        protocolForAllServers, PROTOCOL_PROPERTY, StringUtils.join(listOfServers, LIST_SEPARATOR), protocol);
    }

    // remove protocol from list of servers and store default protocol
    List<String> listOfServersWithoutProtocol = Arrays.stream(listOfServers)
        .map(server -> StringUtils.substringAfter(server, "://"))
        .collect(Collectors.toList());
    archaiusConfig.setProperty(serviceId + RIBBON_PARAM_LISTOFSERVERS, StringUtils.join(listOfServersWithoutProtocol, LIST_SEPARATOR));
    archaiusConfig.setProperty(serviceId + HTTP_PARAM_PROTOCOL, protocol);
  }

  /**
   * Removes OSGi configuration from archaius configuration.
   * @param serviceId Service ID
   */
  private void clearArchiausProperties(String serviceId) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();

    // ribbon parameters
    archaiusConfig.clearProperty(serviceId + RIBBON_PARAM_LISTOFSERVERS);
    archaiusConfig.clearProperty(serviceId + RIBBON_PARAM_MAXAUTORETRIES);
    archaiusConfig.clearProperty(serviceId + RIBBON_PARAM_MAXAUTORETRIESONSERVER);
    archaiusConfig.clearProperty(serviceId + RIBBON_PARAM_OKTORETRYONALLOPERATIONS);

    // hystrix parameters
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_TIMEOUT_MS);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_FALLBACK_ENABLED);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_ENABLED);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_SLEEPWINDOW_MS);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_FORCEOPEN);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_CIRCUITBREAKER_FORCECLOSED);
    archaiusConfig.clearProperty(HYSTRIX_COMMAND_PREFIX + serviceId + HYSTRIX_PARAM_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE);

    // others
    archaiusConfig.clearProperty(serviceId + HTTP_PARAM_PROTOCOL);
  }

}
