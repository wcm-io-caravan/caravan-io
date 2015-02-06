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
package io.wcm.caravan.io.jsontransform.processors;

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.jsontransform.JsonTestHelper;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.Sets;


public class ArrayProcessorTest {

  @Test
  public void test_simple() throws IOException {
    String input = "{\"a\":{\"a1\":\"value1\",\"a2\":{\"a21\":\"value21\"}}}";
    ArrayProcessor processor = new ArrayProcessor(JsonTestHelper.fromJson(input), Sets.newHashSet("a"));
    String output = JsonTestHelper.toString(processor);
    assertEquals("{\"a\":[\"value1\",{\"a21\":\"value21\"}]}", output);
  }

}
