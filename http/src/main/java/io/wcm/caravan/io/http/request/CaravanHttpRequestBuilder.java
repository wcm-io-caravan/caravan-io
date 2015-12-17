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

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.osgi.annotation.versioning.ProviderType;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

/**
 * UriTemplate using HTTP request builder.
 */
@ProviderType
public final class CaravanHttpRequestBuilder {

  private final String serviceId;
  private String method = HttpGet.METHOD_NAME;
  private StringBuilder path = new StringBuilder();
  private Set<String> queryNames = Sets.newHashSet();
  private Map<String, Object> values = Maps.newHashMap();
  private final Multimap<String, String> headers = ArrayListMultimap.create();
  private Charset charset;
  private byte[] body;
  private String bodyTemplate;

  /**
   * Default constructor.
   */
  public CaravanHttpRequestBuilder() {
    serviceId = null;
  }

  /**
   * @param serviceId Logical service ID. Can be null.
   */
  public CaravanHttpRequestBuilder(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * @param correlationId Correlation Id. Can be null.
   * @return Builder
   */
  public CaravanHttpRequestBuilder correlationId(String correlationId) {
    if (correlationId != null) {
      header(CaravanHttpRequest.CORRELATION_ID_HEADER_NAME, ImmutableList.of(correlationId));
    }
    return this;
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
   * @param headerValues Header values
   * @return Builder
   */
  public CaravanHttpRequestBuilder header(String name, Collection<String> headerValues) {
    headers.putAll(name, headerValues);
    return this;
  }

  /**
   * Appends a URL fragment.
   * @param urlFragment URL fragment
   * @return Builder
   */
  public CaravanHttpRequestBuilder append(String urlFragment) {
    this.path.append(urlFragment);
    return this;
  }

  /**
   * Adds a query template expression.
   * @param name Query parameter name
   * @return Builder
   */
  public CaravanHttpRequestBuilder query(String name) {
    queryNames.add(name);
    return this;
  }

  /**
   * Adds a parameter with value to the request query
   * @see CaravanHttpRequest#getUrl()
   * @param name Parameter name
   * @param value Parameter value
   * @return Builder
   */
  public CaravanHttpRequestBuilder query(String name, Object value) {
    return query(name).value(name, value);
  }

  /**
   * Adds a value for any UriTemplate expression in URL, header or body.
   * @param name Parameter name
   * @param value Parameter value
   * @return Builder
   */
  public CaravanHttpRequestBuilder value(String name, Object value) {
    values.put(name, value);
    return this;
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
    return new CaravanHttpRequest(serviceId, method, expandedUrl, expandedHeaders, expandedBody, charset);
  }

  private String getExpandedUrl(Map<String, Object> parameters) {

    Map<String, Object> mergedParams = Maps.newHashMap(parameters);
    mergedParams.putAll(values);
    List<String> sortedQueryNames = Lists.newArrayList(queryNames);
    Collections.sort(sortedQueryNames);
    String operator = path.indexOf("?") == -1 ? "?" : "&";
    String query = queryNames.isEmpty() ? "" : ('{' + operator + StringUtils.join(queryNames, ',') + '}');
    return UriTemplate.fromTemplate(path + query).expand(mergedParams);

  }

  private Multimap<String, String> getExpandedHeaders(Map<String, Object> parameters) {
    Multimap<String, String> expandedHeaders = ArrayListMultimap.create();
    headers.entries().forEach(entry -> {
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
