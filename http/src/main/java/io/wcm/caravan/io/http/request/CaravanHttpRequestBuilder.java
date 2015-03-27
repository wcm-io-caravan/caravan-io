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
package io.wcm.caravan.io.http.request;

import static com.google.common.base.Preconditions.checkNotNull;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.methods.HttpGet;

import com.damnhandy.uri.template.Expression;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.impl.Operator;
import com.damnhandy.uri.template.impl.VarSpec;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * UriTemplate using HTTP request builder.
 */
public class CaravanHttpRequestBuilder {

  private final String serviceName;
  private String method = HttpGet.METHOD_NAME;
  private String path = "";
  private final List<VarSpec> queryExpressions = Lists.newArrayList();
  private final Map<String, Object> queryValues = Maps.newHashMap();
  private final Multimap<String, String> headers = ArrayListMultimap.create();
  private Charset charset;
  private byte[] body;
  private String bodyTemplate;

  /**
   * Default constructor.
   */
  public CaravanHttpRequestBuilder() {
    serviceName = null;
  }

  /**
   * @param serviceName Logical service name. Can be null
   */
  public CaravanHttpRequestBuilder(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @see CaravanHttpRequest#method()
   * @param newMethod HTTP method
   * @return Builder
   */
  public CaravanHttpRequestBuilder method(String newMethod) {
    this.method = checkNotNull(newMethod, "method");
    return this;
  }

  /**
   * @see CaravanHttpRequest#method()
   * @return HTTP method
   */
  public String method() {
    return method;
  }

  /**
   * Adds a header to the HTTP request
   * @see CaravanHttpRequest#headers()
   * @param name Header name
   * @param value Header value
   * @return Builder
   */
  public CaravanHttpRequestBuilder header(String name, String value) {
    headers.put(name, value);
    return this;
  }

  /**
   * Sets and replaces a header for the HTTP request
   * @see CaravanHttpRequest#headers()
   * @param name Header name
   * @param values Header values
   * @return Builder
   */
  public CaravanHttpRequestBuilder header(String name, Collection<String> values) {
    headers.putAll(name, values);
    return this;
  }

  /**
   * Returns an immutable copy of the current headers.
   * @see CaravanHttpRequest#headers()
   * @return HTTP headers
   */
  public Multimap<String, String> headers() {
    return ImmutableMultimap.copyOf(headers);
  }

  /**
   * @see CaravanHttpRequest#url()
   * @param urlFragment In URI template format
   * @return Builder
   */
  public CaravanHttpRequestBuilder append(String urlFragment) {
    UriTemplate template = UriTemplate.fromTemplate(urlFragment);
    String slicedPath = urlFragment;
    for (Expression expression : template.getExpressions()) {
      if (Operator.QUERY.equals(expression.getOperator())) {
        StringBuffer temp = new StringBuffer(slicedPath.substring(0, expression.getStartPosition()));
        if (expression.getEndPosition() != -1 && expression.getEndPosition() < slicedPath.length()) {
          temp.append(slicedPath.substring(expression.getEndPosition()));
        }
        slicedPath = temp.toString();
        for (VarSpec spec : expression.getVarSpecs()) {
          queryExpressions.add(spec);
        }
      }
    }
    path += slicedPath;
    return this;
  }

  /**
   * Adds a parameter with value to the request query
   * @see CaravanHttpRequest#url()
   * @param key Parameter name
   * @param value Parameter value
   * @return Builder
   */
  public CaravanHttpRequestBuilder query(String key, Object value) {
    queryValues.put(key, value);
    return append("{?" + key + '}');
  }

  /**
   * Sets a template for the HTTP body.
   * @see CaravanHttpRequest#body()
   * @param template Body template
   * @return Builder
   */
  public CaravanHttpRequestBuilder body(String template) {
    body = null;
    charset = Charsets.UTF_8;
    bodyTemplate = template;
    return this;
  }

  /**
   * Sets the HTTP body and charset.
   * @see CaravanHttpRequest#body()
   * @param newBody HTTP body
   * @param newCharset HTTP charset
   * @return Builder
   */
  public CaravanHttpRequestBuilder body(byte[] newBody, Charset newCharset) {
    body = newBody;
    charset = newCharset;
    bodyTemplate = null;
    return this;
  }

  /**
   * @see CaravanHttpRequest#body()
   * @return Body
   */
  public byte[] body() {
    return body;
  }

  /**
   * @see CaravanHttpRequest#body()
   * @return Body template
   */
  public String bodyTemplate() {
    return bodyTemplate;
  }

  /**
   * The character set with which the body is encoded, or null if unknown or not applicable. When this is
   * present, you can use {@code new String(req.body(), req.charset())} to access the body as a String.
   * @return Charset
   */
  public Charset charset() {
    return charset;
  }

  /**
   * @see CaravanHttpRequest#url()
   * @return Template URL
   */
  public String url() {
    StringBuilder query = new StringBuilder();
    AtomicBoolean inTemplate = new AtomicBoolean();
    Streams.of(queryExpressions).forEach(spec -> {
      String name = spec.getVariableName();
      if (queryValues.containsKey(name)) {
        if (inTemplate.getAndSet(false)) {
          query.append('}');
        }
        String value = UriTemplate.fromTemplate('{' + spec.getValue() + '}').expand(queryValues);
        query.append(query.length() == 0 ? '?' : '&').append(name).append('=').append(value);
      }
      else {
        if (!inTemplate.getAndSet(true)) {
          query.append('{').append(query.length() == 1 ? '?' : '&');
        }
        else {
          query.append(',');
        }
        query.append(name);
      }
    });
    if (inTemplate.get()) {
      query.append('}');
    }
    return path + query.toString();
  }

  @Override
  public String toString() {
    return new CaravanHttpRequest(serviceName, method, url(), headers, bodyTemplate == null ? body : bodyTemplate.getBytes(charset), charset).toString();
  }

  /**
   * Creates the request object with no values for the templates.
   * @return Request
   */
  public CaravanHttpRequest build() {
    return build(Collections.emptyMap());
  }

  /**
   * Creates the request object with given values for the templates.
   * @param parameters Template values
   * @return Request
   */
  public CaravanHttpRequest build(Map<String, Object> parameters) {
    String expandedUrl = getExpandedUrl(parameters);
    Multimap<String, String> expandedHeaders = getExpandedHeaders(parameters);
    byte[] expandedBody = getExpandedBody(parameters);
    return new CaravanHttpRequest(serviceName, method, expandedUrl, expandedHeaders, expandedBody, charset);
  }

  private String getExpandedUrl(Map<String, Object> parameters) {
    Map<String, Object> mergedParams = Maps.newHashMap(parameters);
    mergedParams.putAll(queryValues);
    List<String> expressions = Streams.of(queryExpressions).map(expression -> expression.getValue()).collect(Collectors.toList());
    String query = expressions.isEmpty() ? "" : ("{?" + Joiner.on(",").join(expressions) + "}");
    return UriTemplate.fromTemplate(path + query).expand(mergedParams);
  }

  private Multimap<String, String> getExpandedHeaders(Map<String, Object> parameters) {
    Multimap<String, String> expandedHeaders = ArrayListMultimap.create();
    Streams.of(headers.entries()).forEach(entry -> {
      String template = entry.getValue();
      String expanded = UriTemplate.expand(template, parameters);
      if (!Strings.isNullOrEmpty(expanded)) {
        expandedHeaders.put(entry.getKey(), expanded);
      }
    });
    return expandedHeaders;
  }

  private byte[] getExpandedBody(Map<String, Object> parameters) {
    if (bodyTemplate == null) {
      return body;
    }
    return CaravanHttpHelper.urlDecode(UriTemplate.expand(bodyTemplate, parameters)).getBytes(Charsets.UTF_8);
  }

}
