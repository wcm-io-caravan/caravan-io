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

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;


public class CaravanHttpHelperTest {

  @Test
  public void testUrlDecode() throws Exception {
    assertEquals("&", CaravanHttpHelper.urlDecode("%26"));
  }

  @Test
  public void testUrlEncode() throws Exception {
    assertEquals("%26", CaravanHttpHelper.urlEncode("&"));
  }


  @Test
  public void testConvertCacheControlToMap_singleEntry() throws Exception {
    List<String> header = ImmutableList.of("max-age=1234");
    Map<String, String> map = CaravanHttpHelper.convertMultiValueHeaderToMap(header);

    assertEquals("map contains a single entry?", 1, map.size());
    assertEquals("max-age extracted correctly?", map.get("max-age"), "1234");
  }

  @Test
  public void testConvertCacheControlToMap_twoEntries() throws Exception {
    List<String> header = ImmutableList.of("public,max-age=1234");
    Map<String, String> map = CaravanHttpHelper.convertMultiValueHeaderToMap(header);

    assertEquals("map contains two entries?", 2, map.size());
    assertEquals("max-age extracted correctly?", map.get("max-age"), "1234");
    assertEquals("public extracted correctly?", map.get("public"), "true");
  }

  @Test
  public void testConvertCacheControlToMap_twoLines() throws Exception {
    List<String> header = ImmutableList.of("public", "max-age=1234");
    Map<String, String> map = CaravanHttpHelper.convertMultiValueHeaderToMap(header);

    assertEquals("map contains two entries?", 2, map.size());
    assertEquals("max-age extracted correctly?", map.get("max-age"), "1234");
    assertEquals("public extracted correctly?", map.get("public"), "true");
  }

  @Test
  public void testConvertCacheControlToMap_maintainOrder() throws Exception {
    List<String> header = ImmutableList.of("1,2,3,4", "5,6,7", "8,9,10,11", "12", "13,14,15", "16");
    Map<String, String> map = CaravanHttpHelper.convertMultiValueHeaderToMap(header);

    assertEquals("map contains all entries?", 16, map.size());

    Iterator<String> keyIterator = map.keySet().iterator();
    for (int i = 1; i <= map.size(); i++) {
      assertEquals(String.valueOf(i), keyIterator.next());
    }
  }
}
