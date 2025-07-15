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

import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY;
import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.RIBBON_PARAM_LISTOFSERVERS;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates http service configuration for service IDs.
 */
public final class CaravanHttpServiceConfigValidator {

  private static final Logger log = LoggerFactory.getLogger(CaravanHttpServiceConfigValidator.class);

  private CaravanHttpServiceConfigValidator() {
    // static methods only
  }

  /**
   * Checks if a valid configuration exists for the given service ID. This does not mean that the host
   * name is correct or returns correct responses, it only checks that the minimum required configuration
   * properties are set to a value.
   * @param serviceId Service ID
   * @return true if configuration is valid
   */
  public static boolean hasValidConfiguration(String serviceId) {
    Configuration archaiusConfig = ArchaiusConfig.getConfiguration();
    return StringUtils.isNotEmpty(archaiusConfig.getString(serviceId + RIBBON_PARAM_LISTOFSERVERS));
  }

  /**
   * Validates service configuration when reading the configuration.
   * @param serviceId Service ID
   * @param config OSGi config
   */
  public static boolean isValidServiceConfig(String serviceId, Map<String, Object> config) {
    if (StringUtils.isBlank(serviceId)) {
      log.warn("Invalid transport layer service configuration without service ID, ignoring.", serviceId);
      return false;
    }
    String[] hosts = PropertiesUtil.toStringArray(config.get(RIBBON_HOSTS_PROPERTY));
    if (hosts == null || hosts.length == 0) {
      log.warn("Invalid transport layer service configuration for '{}' without hosts, ignoring.", serviceId);
      return false;
    }
    return true;
  }

  /**
   * get configuration for "THROW_EXCEPTION_FOR_STATUS_500"
   * @param serviceId
   * @return Configured value
   */
  public static boolean throwExceptionForStatus500(String serviceId) {
    return ArchaiusConfig.getConfiguration().getBoolean(serviceId + CaravanHttpServiceConfig.THROW_EXCEPTION_FOR_STATUS_500,
        CaravanHttpServiceConfig.THROW_EXCEPTION_FOR_STATUS_500_DEFAULT);
  }

}
