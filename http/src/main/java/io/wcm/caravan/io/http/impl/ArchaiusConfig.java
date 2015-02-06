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

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;

/**
 * Initialize static archaius context.
 */
final class ArchaiusConfig {

  private static final String DEFAULT_CONFIGURATION = "/config.properties";

  private static final ConcurrentMapConfiguration OSGI_CONFIG = new ConcurrentMapConfiguration();

  private static volatile boolean initialized;

  private static final Logger log = LoggerFactory.getLogger(ArchaiusConfig.class);

  private ArchaiusConfig() {
    // static methods only
  }

  /**
   * Initialize archaius configuration. This is done only once, if called again nothing is done.
   */
  public static void initialize() {
    if (initialized) {
      return;
    }
    synchronized (ArchaiusConfig.class) {
      if (initialized) {
        return;
      }
      try {
        initializeArchaius();
        initialized = true;
      }
      catch (ConfigurationException ex) {
        log.error("Initializing archaius configuration failed.", ex);
      }
    }
  }

  private static void initializeArchaius() throws ConfigurationException {
    // Default configuration from classpath of this bundle
    AbstractConfiguration defaultConfig = new PropertiesConfiguration(ArchaiusConfig.class.getResource(DEFAULT_CONFIGURATION));

    ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();

    // inject OSGi config which is filled via {@link ArchaiusOsgiProperties}
    finalConfig.addConfiguration(OSGI_CONFIG);

    finalConfig.addConfiguration(defaultConfig);

    ConfigurationManager.install(finalConfig);

    log.debug("Initialized archaius configuration.");
  }

  /**
   * @return Archaius configuration
   */
  public static Configuration getConfiguration() {
    return OSGI_CONFIG;
  }

}
