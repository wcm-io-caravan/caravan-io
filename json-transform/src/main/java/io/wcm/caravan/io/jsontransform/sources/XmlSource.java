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
package io.wcm.caravan.io.jsontransform.sources;

import io.wcm.caravan.io.jsontransform.domain.JsonElement;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Parses the SOAP response and transforms into JSON elements.
 */
public class XmlSource implements Source {

  private static final Logger LOG = LoggerFactory.getLogger(XmlSource.class);

  private static final char XPATH_SEPARATOR = '/';

  private static final String JSON_KEY_FOR_VALUES_WITHOUT_NAME = "value";

  private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

  private final XMLStreamReader reader;
  private final String[] roots;

  private final Queue<JsonElement> outputBuffer = Lists.newLinkedList();
  private final Stack<String> breadCrumb = new Stack<String>();

  private boolean nextHasBeenExecutedBefore;

  private JsonElement firstElement = JsonElement.DEFAULT_START_OBJECT;
  private JsonElement lastElement = JsonElement.DEFAULT_END_OBJECT;

  static {
    INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
  }

  /**
   * @param input The input stream
   * @param roots Possible XML roots
   * @throws XMLStreamException XML read error
   */
  public XmlSource(final InputStream input, final String... roots)
      throws XMLStreamException {
    reader = INPUT_FACTORY.createXMLStreamReader(input);
    this.roots = roots;
  }

  @Override
  public boolean hasNext() {
    fillOutputBufferIfNeeded();
    return !outputBuffer.isEmpty();
  }

  @Override
  public JsonElement next() {
    fillOutputBufferIfNeeded();
    return outputBuffer.isEmpty() ? null : outputBuffer.poll();
  }

  private void fillOutputBufferIfNeeded() {
    if (outputBuffer.isEmpty()) {
      handleElement();
      addLastElementToOutputBufferIfNeeded();
    }
  }

  private void handleElement() {
    seekToNextElementIfNeeded();
    if (reader.isStartElement()) {
      handleStartElement();
    }
    else if (reader.isEndElement()) {
      addToOutputBuffer(JsonElement.DEFAULT_END_OBJECT);
    }
    else if (reader.isCharacters()) {
      addToOutputBuffer(createValueElement(JSON_KEY_FOR_VALUES_WITHOUT_NAME, reader.getText()));
    }
  }

  private void seekToNextElementIfNeeded() {
    try {
      if (nextHasBeenExecutedBefore) {
        nextHasBeenExecutedBefore = false;
      }
      else if (reader.hasNext()) {
        do {
          reader.next();
        }
        while (reader.hasNext() && !isRelevantElement());
      }
    }
    catch (XMLStreamException ex) {
      LOG.error("Error reading XML source", ex);
    }
  }

  private boolean isRelevantElement() {
    if (reader.isWhiteSpace()) {
      return false;
    }
    String xpath = getXPath();
    if (reader.isStartElement()) {
      breadCrumb.add(reader.getLocalName());
      xpath = getXPath();
    }
    else if (reader.isEndElement()) {
      breadCrumb.pop();
    }
    return isInOneRoot(xpath);
  }

  private boolean isInOneRoot(final String xpath) {
    if (roots.length == 0) {
      return true;
    }
    for (String root : roots) {
      if (xpath.startsWith(root)) {
        return true;
      }
    }
    return false;
  }

  private void handleStartElement() {
    String name = reader.getLocalName();
    String key = convertName(name);
    if (reader.getAttributeCount() > 0) {
      addToOutputBuffer(JsonElement.startObject(key));
      addAttributesToOutputBuffer();
    }
    else {
      seekToNextElementIfNeeded();
      if (reader.isStartElement()) {
        nextHasBeenExecutedBefore = true;
        addToOutputBuffer(JsonElement.startObject(key));
      }
      else if (reader.isEndElement()) {
        addToOutputBuffer(JsonElement.nullValue(key));
      }
      else {
        addToOutputBuffer(createValueElement(key, reader.getText()));
        seekToNextElementIfNeeded();
      }
    }
  }

  private void addToOutputBuffer(JsonElement element) {
    addFirstElementToOutputBufferIfNeeded();
    outputBuffer.add(element);
  }

  private void addFirstElementToOutputBufferIfNeeded() {
    if (firstElement != null) {
      outputBuffer.add(firstElement);
      firstElement = null;
    }
  }

  private void addAttributesToOutputBuffer() {
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      addToOutputBuffer(JsonElement.value(convertName(reader.getAttributeLocalName(i)), reader.getAttributeValue(i)));
    }
  }

  private String convertName(final String name) {
    return StringUtils.uncapitalize(name);
  }

  private JsonElement createValueElement(final String key, final String value) {
    return JsonElement.value(key, value);
  }

  private void addLastElementToOutputBufferIfNeeded() {
    if (outputBuffer.isEmpty() && firstElement == null && lastElement != null) {
      outputBuffer.add(lastElement);
      lastElement = null;
    }
  }

  private String getXPath() {
    return StringUtils.join(breadCrumb, XPATH_SEPARATOR);
  }

  @Override
  public void close() throws IOException {
    try {
      reader.close();
    }
    catch (XMLStreamException ex) {
      throw new IOException(ex);
    }
  }

}
