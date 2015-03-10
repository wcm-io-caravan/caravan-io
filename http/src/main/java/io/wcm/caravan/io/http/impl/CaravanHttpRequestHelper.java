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

import static com.google.common.base.Strings.emptyToNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.google.common.base.Charsets;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper class for standard URL tasks.
 */
public final class CaravanHttpRequestHelper {

  private CaravanHttpRequestHelper() {
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
   * Parses a query and decodes the values.
   * @param queryLine Query line to parse
   * @return Parsed query
   */
  public static Multimap<String, String> parseAndDecodeQueries(final String queryLine) {
    Multimap<String, String> map = LinkedHashMultimap.create();
    if (emptyToNull(queryLine) == null) {
      return map;
    }
    if (queryLine.indexOf('&') == -1) {
      if (queryLine.indexOf('=') != -1) {
        CaravanHttpRequestHelper.putKV(queryLine, map);
      }
      else {
        map.put(queryLine, null);
      }
    }
    else {
      char[] chars = queryLine.toCharArray();
      int start = 0;
      int i = 0;
      for (; i < chars.length; i++) {
        if (chars[i] == '&') {
          CaravanHttpRequestHelper.putKV(queryLine.substring(start, i), map);
          start = i + 1;
        }
      }
      CaravanHttpRequestHelper.putKV(queryLine.substring(start, i), map);
    }
    return map;
  }

  private static void putKV(final String stringToParse, final Multimap<String, String> map) {
    String key;
    String value;
    // note that '=' can be a valid part of the value
    final int firstEq = stringToParse.indexOf('=');
    if (firstEq == -1) {
      key = urlDecode(stringToParse);
      value = null;
    }
    else {
      key = urlDecode(stringToParse.substring(0, firstEq));
      value = urlDecode(stringToParse.substring(firstEq + 1));
    }
    map.put(key, value);
  }

}
