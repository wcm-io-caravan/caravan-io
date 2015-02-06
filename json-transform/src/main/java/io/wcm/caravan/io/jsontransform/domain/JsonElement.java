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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A simple bean representing a JSON stream element.
 */
public class JsonElement {

  /**
   * The default JSON object start element
   */
  public static final JsonElement DEFAULT_START_OBJECT = new JsonElement(null, null, Type.START_OBJECT);

  /**
   * The default JSON array start element
   */
  public static final JsonElement DEFAULT_START_ARRAY = new JsonElement(null, null, Type.START_ARRAY);

  /**
   * The default JSON object end element
   */
  public static final JsonElement DEFAULT_END_OBJECT = new JsonElement(null, null, Type.END_OBJECT);

  /**
   * The default JSON array end element
   */
  public static final JsonElement DEFAULT_END_ARRAY = new JsonElement(null, null, Type.END_ARRAY);

  /**
   * Possible JSON stream element type.
   */
  public enum Type {
    /**
     * All JSON stream element types.
     */
    START_OBJECT, END_OBJECT, START_ARRAY, END_ARRAY, VALUE
  }

  /**
   * The key of the JSON stream element. If {@link Type} is END_OBJECT or END_ARRAY will be null. Otherwise can be null.
   */
  private final String key;

  /**
   * The value of the JSON stream element. Only set for VALUE {@link Type}.
   */
  private final Object value;

  /**
   * The JSON stream element type.
   */
  private final Type type;

  /**
   * Creator for JSON value element with value being NULL.
   * @param key JSON element name
   * @return JSON value element with value NULL
   */
  public static JsonElement nullValue(String key) {
    return new JsonElement(key, null, Type.VALUE);
  }

  /**
   * Creator for JSON value element with given value and no key.
   * @param value JSON element value
   * @return JSON value element
   */
  public static JsonElement value(Object value) {
    return new JsonElement(null, value, Type.VALUE);
  }

  /**
   * Creator for JSON value element with given key and value
   * @param key JSON element name
   * @param value JSON element value
   * @return JSON value element
   */
  public static JsonElement value(String key, Object value) {
    return new JsonElement(key, value, Type.VALUE);
  }

  /**
   * Creator for JSON object start element with given key
   * @param key JSON element name
   * @return JSON object start element
   */
  public static JsonElement startObject(String key) {
    return new JsonElement(key, null, Type.START_OBJECT);
  }

  /**
   * Creator for JSON array start element with given key
   * @param key JSON element name
   * @return JSON array start element
   */
  public static JsonElement startArray(String key) {
    return new JsonElement(key, null, Type.START_ARRAY);
  }

  /**
   * @param key JSON element name
   * @param value JSON element value
   * @param type JSON element type
   */
  public JsonElement(String key, Object value, Type type) {
    this.key = key;
    this.value = value;
    this.type = type;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return this.key;
  }

  /**
   * @return the value
   */
  public Object getValue() {
    return this.value;
  }

  /**
   * @return the type
   */
  public Type getType() {
    return this.type;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj, false);
  }

  /**
   * @return True if is a starting element
   */
  public boolean isStartingElement() {
    return Type.START_ARRAY.equals(type) || Type.START_OBJECT.equals(type);
  }

  /**
   * @return True if is a closing element
   */
  public boolean isClosingElement() {
    return Type.END_ARRAY.equals(type) || Type.END_OBJECT.equals(type);
  }

}
