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
package io.wcm.dromas.io.http.httpclient.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanUtils;

/**
 * Helper methods for managing java beans.
 */
final class BeanUtil {

  private BeanUtil() {
    // static methods only
  }

  /**
   * Get map with key/value pairs for properties of a java bean (using {@link BeanUtils#describe(Object)}).
   * An array of property names can be passed that should be masked with "***" because they contain sensitive
   * information.
   * @param beanObject Bean object
   * @param maskProperties List of property names
   * @return Map with masked key/value pairs
   */
  public static SortedMap<String, Object> getMaskedBeanProperties(Object beanObject, String[] maskProperties) {
    try {
      SortedMap<String, Object> configProperties = new TreeMap<String, Object>(BeanUtils.describe(beanObject));

      // always ignore "class" properties which is added by BeanUtils.describe by default
      configProperties.remove("class");

      // Mask some properties with confidential information (if set to any value)
      if (maskProperties != null) {
        for (String propertyName : maskProperties) {
          if (configProperties.containsKey(propertyName) && configProperties.get(propertyName) != null) {
            configProperties.put(propertyName, "***");
          }
        }
      }

      return configProperties;
    }
    catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
      throw new IllegalArgumentException("Unable to get properties from: " + beanObject, ex);
    }
  }

}
