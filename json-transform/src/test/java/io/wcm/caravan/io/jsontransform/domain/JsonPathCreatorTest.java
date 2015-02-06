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
package io.wcm.caravan.io.jsontransform.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class JsonPathCreatorTest {

  @Test
  public void testIsInPath() throws Exception {
    String path = "$.Envelope.Body";
    JsonPathCreator underTest = new JsonPathCreator();
    assertFalse(underTest.getJsonPathForElement(JsonElement.DEFAULT_START_OBJECT).startsWith(path));
    assertFalse(underTest.getJsonPathForElement(JsonElement.startObject("Envelope")).startsWith(path));
    assertFalse(underTest.getJsonPathForElement(JsonElement.startObject("Test1")).startsWith(path));
    assertFalse(underTest.getJsonPathForElement(JsonElement.value("Test2", "Value2")).startsWith(path));
    assertFalse(underTest.getJsonPathForElement(JsonElement.DEFAULT_END_OBJECT).startsWith(path));
    assertTrue(underTest.getJsonPathForElement(JsonElement.startObject("Body")).startsWith(path));
    assertTrue(underTest.getJsonPathForElement(JsonElement.startObject("Test3")).startsWith(path));
    assertTrue(underTest.getJsonPathForElement(JsonElement.value("Test4", "Value4")).startsWith(path));
    assertTrue(underTest.getJsonPathForElement(JsonElement.DEFAULT_END_OBJECT).startsWith(path));
    assertTrue(underTest.getJsonPathForElement(JsonElement.DEFAULT_END_OBJECT).startsWith(path));
    assertFalse(underTest.getJsonPathForElement(JsonElement.value("Test5", "Value5")).startsWith(path));
  }

}
