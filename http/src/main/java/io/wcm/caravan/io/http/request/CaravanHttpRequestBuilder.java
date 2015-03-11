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
import static com.google.common.collect.Iterables.toArray;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.http.impl.CaravanHttpHelper;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Builds a request to an HTTP target. Not thread safe. <br>
 * <br>
 * <br>
 * <b>relationship to JAXRS 2.0</b><br>
 * <br>
 * A combination of {@code javax.ws.rs.client.WebTarget} and {@code javax.ws.rs.client.Invocation.Builder}, ensuring you
 * can modify any
 * part of the request. However, this object is mutable, so needs to be guarded
 * with the copy constructor.
 */
@SuppressWarnings("hiding")
//CHECKSTYLE:OFF
public final class CaravanHttpRequestBuilder implements Serializable {

  private static final long serialVersionUID = -4247954761231424727L;

  private final String serviceName;
  private String method = HttpGet.METHOD_NAME;
  private final StringBuilder url = new StringBuilder();
  private final Multimap<String, String> queries = LinkedHashMultimap.create();
  private final Multimap<String, String> headers = LinkedHashMultimap.create();
  private transient Charset charset;
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
  public CaravanHttpRequestBuilder(final String serviceName) {
    this.serviceName = serviceName;
  }

  /** Copy constructor. Use this when making templates. */
  public CaravanHttpRequestBuilder(final CaravanHttpRequestBuilder toCopy) {
    checkNotNull(toCopy, "toCopy");
    this.serviceName = toCopy.serviceName;
    this.method = toCopy.method;
    this.url.append(toCopy.url);
    this.queries.putAll(toCopy.queries);
    this.headers.putAll(toCopy.headers);
    this.charset = toCopy.charset;
    this.body = toCopy.body;
    this.bodyTemplate = toCopy.bodyTemplate;
  }

  /**
   * Resolves any templated variables in the requests path, query, or headers against the supplied unencoded
   * arguments. <br>
   * <br>
   * <br>
   * <b>relationship to JAXRS 2.0</b><br>
   * <br>
   * This call is similar to {@code javax.ws.rs.client.WebTarget.resolveTemplates(templateValues, true)}, except that
   * the template values apply to any part of the request, not just the URL.
   */
  public CaravanHttpRequestBuilder resolve(final Map<String, Object> unencoded) {
    resolveUrl(unencoded);
    resolveHeaders(unencoded);
    resolveBody(unencoded);
    return this;
  }

  private void resolveUrl(final Map<String, Object> unencoded) {
    replaceQueryValues(unencoded);
    setUrl(UriTemplate.expand(url.toString(), unencoded).replace("%2F", "/"));
  }

  private void setUrl(String newUrl) {
    url.setLength(0);
    url.append(newUrl);
  }

  private void resolveHeaders(final Map<String, Object> unencoded) {
    Multimap<String, String> resolvedHeaders = LinkedHashMultimap.create();
    for (String field : headers.keySet()) {
      for (String value : headers.get(field)) {
        String expanded = UriTemplate.expand(value, unencoded);
        if (!StringUtils.isBlank(expanded)) {
          resolvedHeaders.put(field, expanded);
        }
      }
    }
    headers.clear();
    headers.putAll(resolvedHeaders);
  }

  private void resolveBody(final Map<String, Object> unencoded) {
    if (bodyTemplate != null) {
      body(CaravanHttpHelper.urlDecode(UriTemplate.expand(bodyTemplate, unencoded)));
    }
  }

  /** roughly analogous to {@code javax.ws.rs.client.Target.request()}. */
  public CaravanHttpRequest build() {
    return new CaravanHttpRequest(serviceName, method, new StringBuilder(url).append(queryLine()).toString(), headers, body, charset);
  }

  /* @see Request#method() */
  public CaravanHttpRequestBuilder method(final String method) {
    this.method = checkNotNull(method, "method");
    return this;
  }

  /* @see Request#method() */
  public String method() {
    return method;
  }

  /* @see #url() */
  public CaravanHttpRequestBuilder append(final CharSequence value) {
    String newUrl = pullAnyQueriesOutOfUrl(url.append(value));
    setUrl(newUrl);
    return this;
  }

  /* @see #url() */
  public CaravanHttpRequestBuilder insert(final int pos, final CharSequence value) {
    url.insert(pos, pullAnyQueriesOutOfUrl(new StringBuilder(value)));
    return this;
  }

  public String url() {
    return url.toString();
  }

  /**
   * Replaces queries with the specified {@code configKey} with url decoded {@code values} supplied. <br>
   * When the {@code value} is {@code null}, all queries with the {@code configKey} are removed. <br>
   * <br>
   * <br>
   * <b>relationship to JAXRS 2.0</b><br>
   * <br>
   * Like {@code WebTarget.query}, except the values can be templatized. <br>
   * ex. <br>
   *
   * <pre>
   * template.query(&quot;Signature&quot;, &quot;{signature}&quot;);
   * </pre>
   * @param configKey the configKey of the query
   * @param values can be a single null to imply removing all values. Else no
   *          values are expected to be null.
   * @see #queries()
   */
  public CaravanHttpRequestBuilder query(final String configKey, final String... values) {
    queries.removeAll(checkNotNull(configKey, "configKey"));
    if (values != null && values.length > 0 && values[0] != null) {
      String encodedKey = encodeIfNotVariable(configKey);
      Streams.of(values)
      .map(v -> encodeIfNotVariable(v))
      .forEach(v -> queries.put(encodedKey, v));
    }
    return this;
  }

  private String encodeIfNotVariable(final String in) {
    if (in == null || in.indexOf('{') == 0) {
      return in;
    }
    return CaravanHttpHelper.urlEncode(in);
  }

  /* @see #query(String, String...) */
  public CaravanHttpRequestBuilder query(final String configKey, final Iterable<String> values) {
    if (values != null) {
      return query(configKey, toArray(values, String.class));
    }
    return query(configKey, (String[])null);
  }

  /**
   * Replaces all existing queries with the newly supplied url decoded
   * queries. <br>
   * <br>
   * <br>
   * <b>relationship to JAXRS 2.0</b><br>
   * <br>
   * Like {@code WebTarget.queries}, except the values can be templatized. <br>
   * ex. <br>
   *
   * <pre>
   * template.queries(ImmutableMultimap.of(&quot;Signature&quot;, &quot;{signature}&quot;));
   * </pre>
   * @param queries if null, remove all queries. else value to replace all queries
   *          with.
   * @see #queries()
   */
  public CaravanHttpRequestBuilder queries(final Multimap<String, String> queries) {
    if (queries == null || queries.isEmpty()) {
      this.queries.clear();
    }
    else {
      Streams.of(queries.keySet())
      .forEach(key -> query(key, toArray(queries.get(key), String.class)));
    }
    return this;
  }

  /**
   * Returns an immutable copy of the url decoded queries.
   * @see CaravanHttpRequest#url()
   */
  public Multimap<String, String> queries() {
    Multimap<String, String> decoded = LinkedHashMultimap.create();
    for (String field : queries.keySet()) {
      String decodedField = CaravanHttpHelper.urlDecode(field);
      Streams.of(queries.get(field))
      .map(v -> v != null ? CaravanHttpHelper.urlDecode(v) : null)
      .forEach(v -> decoded.put(decodedField, v));
    }
    return ImmutableMultimap.copyOf(decoded);
  }

  /**
   * Replaces headers with the specified {@code configKey} with the {@code values} supplied. <br>
   * When the {@code value} is {@code null}, all headers with the {@code configKey} are removed. <br>
   * <br>
   * <br>
   * <b>relationship to JAXRS 2.0</b><br>
   * <br>
   * Like {@code WebTarget.queries} and {@code javax.ws.rs.client.Invocation.Builder.header},
   * except the values can be templatized. <br>
   * ex. <br>
   *
   * <pre>
   * template.query(&quot;X-Application-Version&quot;, &quot;{version}&quot;);
   * </pre>
   * @param configKey the configKey of the header
   * @param values can be a single null to imply removing all values. Else no
   *          values are expected to be null.
   * @see #headers()
   */
  public CaravanHttpRequestBuilder header(final String configKey, final String... values) {
    checkNotNull(configKey, "header configKey");
    if (values == null || (values.length == 1 && values[0] == null)) {
      headers.removeAll(configKey);
    }
    else {
      headers.replaceValues(configKey, Arrays.asList(values));
    }
    return this;
  }

  /* @see #header(String, String...) */
  public CaravanHttpRequestBuilder header(final String configKey, final Iterable<String> values) {
    if (values != null) {
      return header(configKey, toArray(values, String.class));
    }
    return header(configKey, (String[])null);
  }

  /**
   * Replaces all existing headers with the newly supplied headers. <br>
   * <br>
   * <br>
   * <b>relationship to JAXRS 2.0</b><br>
   * <br>
   * Like {@code Invocation.Builder.headers(MultivaluedMap)}, except the
   * values can be templatized. <br>
   * ex. <br>
   *
   * <pre>
   * template.headers(ImmutableMultimap.of(&quot;X-Application-Version&quot;, &quot;{version}&quot;));
   * </pre>
   * @param headers if null, remove all headers. else value to replace all headers
   *          with.
   * @see #headers()
   */
  public CaravanHttpRequestBuilder headers(final Multimap<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      this.headers.clear();
    }
    else {
      this.headers.putAll(headers);
    }
    return this;
  }

  /**
   * Returns an immutable copy of the current headers.
   * @see CaravanHttpRequest#headers()
   */
  public Multimap<String, String> headers() {
    return ImmutableMultimap.copyOf(headers);
  }

  /**
   * @see CaravanHttpRequest#body()
   */
  public CaravanHttpRequestBuilder body(final byte[] bodyData, final Charset charset) {
    this.bodyTemplate = null;
    this.charset = charset;
    this.body = bodyData;
    return this;
  }

  /**
   * @see CaravanHttpRequest#body()
   */
  public CaravanHttpRequestBuilder body(final String bodyText) {
    byte[] bodyData = bodyText != null ? bodyText.getBytes(Charsets.UTF_8) : null;
    return body(bodyData, Charsets.UTF_8);
  }

  /**
   * The character set with which the body is encoded, or null if unknown or not applicable. When this is
   * present, you can use {@code new String(req.body(), req.charset())} to access the body as a String.
   */
  public Charset charset() {
    return charset;
  }

  /**
   * @see CaravanHttpRequest#body()
   */
  public byte[] body() {
    return body;
  }

  /**
   * @see CaravanHttpRequest#body()
   */
  public CaravanHttpRequestBuilder bodyTemplate(final String bodyTemplate) {
    this.bodyTemplate = bodyTemplate;
    this.charset = null;
    this.body = null;
    return this;
  }

  /**
   * @see CaravanHttpRequest#body()
   */
  public String bodyTemplate() {
    return bodyTemplate;
  }

  /**
   * if there are any query params in the URL, this will extract them out.
   */
  private String pullAnyQueriesOutOfUrl(final StringBuilder url) {
    // parse out queries
    final int queryIndex = url.indexOf("?");
    if (queryIndex != -1) {
      String queryLine = url.substring(queryIndex + 1);
      Multimap<String, String> firstQueries = CaravanHttpHelper.parseAndDecodeQueries(queryLine);
      if (!queries.isEmpty()) {
        firstQueries.putAll(queries);
        queries.clear();
      }
      //Since we decode all queries, we want to use the
      //query()-method to re-add them to ensure that all
      //logic (such as url-encoding) are executed, giving
      //a valid queryLine()
      for (String key : firstQueries.keySet()) {
        Collection<String> values = firstQueries.get(key);
        if (allValuesAreNull(values)) {
          //Queryies where all values are null will
          //be ignored by the query(key, value)-method
          //So we manually avoid this case here, to ensure that
          //we still fulfill the contract (ex. parameters without values)
          queries.putAll(CaravanHttpHelper.urlEncode(key), values);
        }
        else {
          query(key, values);
        }

      }
      return url.substring(0, queryIndex);
    }
    return url.toString();
  }

  private boolean allValuesAreNull(final Collection<String> values) {
    if (values.isEmpty()) {
      return true;
    }
    // TODO: replace by Streams#exist
    for (String val : values) {
      if (val != null) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return build().toString();
  }

  /**
   * Replaces query values which are templated with corresponding values from the {@code unencoded} map.
   * Any unresolved queries are removed.
   */
  public CaravanHttpRequestBuilder replaceQueryValues(final Map<String, ?> unencoded) {

    Multimap<String, String> newQueries = LinkedHashMultimap.create();
    for (String key : queries.keySet()) {
      for (String value : queries.get(key)) {
        if (value.indexOf('{') == 0 && value.indexOf('}') == value.length() - 1) {
          Object variableValue = unencoded.get(value.substring(1, value.length() - 1));
          // only add non-null expressions
          if (variableValue == null) {
            continue;
          }
          if (variableValue instanceof Iterable) {
            for (Object val : Iterable.class.cast(variableValue)) {
              newQueries.put(key, CaravanHttpHelper.urlEncode(String.valueOf(val)));
            }
          }
          else {
            newQueries.put(key, CaravanHttpHelper.urlEncode(String.valueOf(variableValue)));
          }
        }
        else {
          newQueries.put(key, value);
        }
      }
    }
    queries.clear();
    queries.putAll(newQueries);
    return this;
  }

  public String queryLine() {
    if (queries.isEmpty()) {
      return "";
    }
    StringBuilder queryBuilder = new StringBuilder();
    for (String field : queries.keySet()) {
      for (String value : queries.get(field)) {
        queryBuilder.append('&').append(field);
        if (value != null) {
          queryBuilder.append('=');
          if (!value.isEmpty()) {
            queryBuilder.append(value);
          }
        }
      }
    }
    queryBuilder.deleteCharAt(0);
    return queryBuilder.insert(0, '?').toString();
  }
}
