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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

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
   * Converts a multi value header to a {@link Map}. If header segment has no value gets stored as boolean.
   * @param header Multi value header to parse
   * @return Header map
   */
  public static Map<String, Object> convertHeaderToMap(final Collection<String> header) {
    Map<String, Object> headerMap = Maps.newHashMap();
    for (String line : header) {
      String[] tokens = line.split(":", 2);
      headerMap.put(tokens[0], tokens.length == 1 ? true : StringUtils.trim(tokens[1]));
    }
    return headerMap;
  }

}
