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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Charsets;

/**
 * Helper class for standard URL tasks.
 */
public final class CaravanHttpHelper {

  private CaravanHttpHelper() {
    // nothing to do
  }

  /**
   * @param token Token to decode
   * @return Decoded token
   */
  public static String urlDecode(final String token) {
    try {
      return URLDecoder.decode(token, Charsets.UTF_8.name());
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param token Token to encode
   * @return Encoded token
   */
  public static String urlEncode(final Object token) {
    try {
      return URLEncoder.encode(String.valueOf(token), Charsets.UTF_8.name());
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Constructs a Map from a multi-value header (i.e. one that can have multiple values that either split up into
   * multiple lines with the same name, or given as a comma-separated list of values in a single header line)
   * For example, see the following header:
   *
   * <pre>
   * Cache-Control: public, max-age=120
   * </pre>
   *
   * That will return a map with two entries: ("public" -&gt; "true", "max-age" -&gt; "120")
   * The following header will result in the same map.
   *
   * <pre>
   * Cache-Control: public
   * Cache-Control: max-age=120
   * </pre>
   * @param header the collection of header values (one entry for each line)
   * @return Header map
   */
  public static Map<String, String> convertMultiValueHeaderToMap(final Collection<String> header) {
    // we use linked hash map, because the order of entries is important
    Map<String, String> headerMap = new LinkedHashMap<>();
    for (String line : header) {
      String[] tokens = StringUtils.split(line, ',');
      for (String token : tokens) {
        String[] keyValue = StringUtils.split(token, '=');
        headerMap.put(StringUtils.trim(keyValue[0]), keyValue.length == 1 ? "true" : StringUtils.trim(keyValue[1]));
      }
    }
    return headerMap;
  }

}
