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

import static org.junit.Assert.assertEquals;

import java.util.SortedMap;

import org.junit.Test;

public class BeanUtilTest {

  @Test
  public void testGetMaskedBeanProperties() {

    SampleBean sampleBean = new SampleBean("value1", "value2");
    SortedMap<String, Object> properties = BeanUtil.getMaskedBeanProperties(sampleBean, new String[] {
        "attribute2"
    });

    assertEquals(2, properties.size());
    assertEquals("value1", properties.get("attribute1"));
    assertEquals("***", properties.get("attribute2"));
  }

  public final class SampleBean {

    private final String mAttribute1;
    private final String mAttribute2;

    public SampleBean(String pAttribute1, String pAttribute2) {
      mAttribute1 = pAttribute1;
      mAttribute2 = pAttribute2;
    }

    public String getAttribute1() {
      return mAttribute1;
    }

    public String getAttribute2() {
      return mAttribute2;
    }

  }

}
