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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.osgi.annotation.versioning.ProviderType;

import com.damnhandy.uri.template.Expression;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.impl.Operator;
import com.damnhandy.uri.template.impl.VarSpec;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * UriTemplate using HTTP request builder.
 */
@ProviderType
public final class CaravanHttpRequestBuilder {

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
   * @param serviceName Logical service name. Can be null
   * @param correlationId Correlation ID. Can be null
   */
  public CaravanHttpRequestBuilder(String serviceName, String correlationId) {
    this.serviceName = serviceName;
    if (correlationId != null) {
      header(CaravanHttpRequest.CORRELATION_ID_HEADER_NAME, ImmutableList.of(correlationId));
    }
  }

  /**
   * @see CaravanHttpRequest#getMethod()
   * @param newMethod HTTP method
   * @return Builder
   */
  public CaravanHttpRequestBuilder method(String newMethod) {
    this.method = checkNotNull(newMethod, "method");
    return this;
  }

  /**
   * Adds a header to the HTTP request
   * @see CaravanHttpRequest#getHeaders()
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
   * @see CaravanHttpRequest#getHeaders()
   * @param name Header name
   * @param values Header values
   * @return Builder
   */
  public CaravanHttpRequestBuilder header(String name, Collection<String> values) {
    headers.putAll(name, values);
    return this;
  }

  /**
   * @see CaravanHttpRequest#getUrl()
   * @param urlFragment In URI template format
   * @return Builder
   */
  public CaravanHttpRequestBuilder append(String urlFragment) {
    String slicedPath = parseTemplateExpressions(urlFragment);
    slicedPath = parseQueries(slicedPath);
    path += slicedPath;
    return this;
  }

  private String parseTemplateExpressions(String urlFragment) {
    UriTemplate template = UriTemplate.fromTemplate(urlFragment);
    String slicedPath = urlFragment;
    for (Expression expression : template.getExpressions()) {
      if (Operator.QUERY.equals(expression.getOperator()) || Operator.CONTINUATION.equals(expression.getOperator())) {
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
    return slicedPath;
  }

  private String parseQueries(String urlFragment) {
    String slicedPath = urlFragment;
    String queries = "";
    if (urlFragment.contains("?")) {
      String[] tokens = StringUtils.split(urlFragment, "?", 2);
      slicedPath = tokens[0];
      queries = tokens[1];
    }
    Multimap<String, String> multiMap = LinkedListMultimap.create();
    for (String query : StringUtils.split(queries, "&")) {
      String[] tokens = StringUtils.split(query, "=", 2);
      String key = tokens[0];
      String value = tokens.length == 2 ? CaravanHttpHelper.urlDecode(tokens[1]) : null;
      multiMap.put(key, value);
    }
    for (String key : multiMap.keySet()) {
      Collection<String> values = multiMap.get(key);
      if (values.size() > 1) {
        append("{?" + key + "*}");
        queryValues.put(key, values);
      }
      else {
        query(key, values.iterator().next());
      }
    }
    return slicedPath;
  }

  /**
   * Adds a parameter with value to the request query
   * @see CaravanHttpRequest#getUrl()
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
   * @see CaravanHttpRequest#getBody()
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
   * @see CaravanHttpRequest#getBody()
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
    Collections.sort(expressions);
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
