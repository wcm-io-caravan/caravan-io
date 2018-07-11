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
package io.wcm.caravan.io.jsontransform.source;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import io.wcm.caravan.io.jsontransform.JsonTestHelper;


public class XmlSourceTest {

  private static final String SIMPLE_XML = "<Root><Key1>value1</Key1><Obj1><Key2>2</Key2></Obj1></Root>";
  private static final String SIMPLE_XML_WITH_COMMENT = "<Root><!-- comments --><Key1>value1</Key1></Root>";

  private XmlSource createXmlSource(String xml) throws XMLStreamException {
    return new XmlSource(new ByteArrayInputStream(xml.getBytes()), "Root");
  }

  @Test
  public void test_isUncapitalizeProperties_default() throws XMLStreamException, IOException {
    String xml = SIMPLE_XML;
    XmlSource source = createXmlSource(xml);
    assertTrue(source.isUncapitalizeProperties());
    source.close();
  }

  @Test
  public void test_setUncapitalizeProperties_true() throws XMLStreamException {
    XmlSource source = createXmlSource(SIMPLE_XML);
    source.setUncapitalizeProperties(true);
    new JsonTestHelper(source)
    .assertStartObject()
    .assertStartObject("root")
    .assertValue("key1", "value1")
    .assertStartObject("obj1")
    .assertValue("key2", "2")
    .assertEndObject()
    .assertEndObject()
    .assertEndObject();
  }

  @Test
  public void test_setUncapitalizeProperties_false() throws XMLStreamException {
    XmlSource source = createXmlSource(SIMPLE_XML);
    source.setUncapitalizeProperties(false);
    new JsonTestHelper(source)
    .assertStartObject()
    .assertStartObject("Root")
    .assertValue("Key1", "value1")
    .assertStartObject("Obj1")
    .assertValue("Key2", "2")
    .assertEndObject()
    .assertEndObject()
    .assertEndObject();
  }

  @Test
  public void commentLineShouldBeIgnored() throws XMLStreamException {
    XmlSource source = createXmlSource(SIMPLE_XML_WITH_COMMENT);
    source.setUncapitalizeProperties(false);
    new JsonTestHelper(source)
      .assertStartObject()
      .assertStartObject("Root")
      .assertValue("Key1", "value1")
      .assertEndObject()
      .assertEndObject();
  }
}
