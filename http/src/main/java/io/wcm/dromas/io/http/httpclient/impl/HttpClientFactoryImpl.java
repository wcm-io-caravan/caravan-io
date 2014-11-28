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

import io.wcm.dromas.commons.stream.Streams;
import io.wcm.dromas.io.http.httpclient.HttpClientConfig;
import io.wcm.dromas.io.http.httpclient.HttpClientFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.HttpClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.framework.BundleContext;

/**
 * Default implementation of {@link HttpClientFactory}.
 */
@Component(immediate = true)
@Service(HttpClientFactory.class)
public class HttpClientFactoryImpl implements HttpClientFactory {

  @Reference(name = "httpClientConfig", referenceInterface = HttpClientConfig.class,
      cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private final ConcurrentMap<Comparable<Object>, HttpClientItem> factoryItems = new ConcurrentSkipListMap<>();

  private HttpClientItem defaultFactoryItem;

  @Activate
  private void activate(BundleContext context) {
    defaultFactoryItem = new HttpClientItem(DefaultHttpClientConfig.INSTANCE);
  }

  @Deactivate
  private void deactivate() {
    Streams.of(factoryItems.values()).forEach(item -> item.close());
    factoryItems.clear();
    defaultFactoryItem.close();
    defaultFactoryItem = null;
  }

  protected void bindHttpClientConfig(HttpClientConfig httpClientConfig, Map<String, Object> config) {
    factoryItems.put(ServiceUtil.getComparableForServiceRanking(config), new HttpClientItem(httpClientConfig));
  }

  protected void unbindHttpClientConfig(HttpClientConfig httpClientConfig, Map<String, Object> config) {
    HttpClientItem removed = factoryItems.remove(ServiceUtil.getComparableForServiceRanking(config));
    if (removed != null) {
      removed.close();
    }
  }

  @Override
  public HttpClient getHttpClient(String targetUrl) {
    return getFactoryItem(toUri(targetUrl), null).getHttpClient();
  }

  @Override
  public HttpClient getHttpClient(URI targetUrl) {
    return getFactoryItem(targetUrl, null).getHttpClient();
  }

  @Override
  public HttpAsyncClient getHttpAsyncClient(String targetUrl) {
    return getFactoryItem(toUri(targetUrl), null).getHttpAsyncClient();
  }

  @Override
  public HttpAsyncClient getHttpAsyncClient(URI targetUrl) {
    return getFactoryItem(targetUrl, null).getHttpAsyncClient();
  }

  @Override
  public HttpClient getWsHttpClient(String targetUrl, String wsAddressingToUri) {
    return getFactoryItem(toUri(targetUrl), wsAddressingToUri).getHttpClient();
  }

  @Override
  public HttpClient getWsHttpClient(URI targetUrl, URI wsAddressingToUri) {
    return getFactoryItem(targetUrl, wsAddressingToUri.toString()).getHttpClient();
  }

  @Override
  public HttpAsyncClient getWsHttpAsyncClient(String targetUrl, String wsAddressingToUri) {
    return getFactoryItem(toUri(targetUrl), wsAddressingToUri).getHttpAsyncClient();
  }

  @Override
  public HttpAsyncClient getWsHttpAsyncClient(URI targetUrl, URI wsAddressingToUri) {
    return getFactoryItem(targetUrl, wsAddressingToUri.toString()).getHttpAsyncClient();
  }

  private HttpClientItem getFactoryItem(URI targetUrl, String wsAddressingToUri) {
    for (HttpClientItem item : factoryItems.values()) {
      if (item.matches(targetUrl.getHost(), wsAddressingToUri)) {
        return item;
      }
    }
    return defaultFactoryItem;
  }

  private URI toUri(String uri) {
    if (StringUtils.isEmpty(uri)) {
      return null;
    }
    else {
      try {
        return new URI(uri);
      }
      catch (URISyntaxException ex) {
        throw new IllegalArgumentException("Invalid URI: " + ex.getMessage(), ex);
      }
    }
  }

}
