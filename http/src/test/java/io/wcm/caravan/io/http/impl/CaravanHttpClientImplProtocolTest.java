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
package io.wcm.caravan.io.http.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerCommandFactory;
import io.wcm.caravan.io.http.impl.ribbon.SimpleLoadBalancerFactory;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableMap;

/**
 * Integration tests for HTTP communcation of transport layer.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaravanHttpClientImplProtocolTest {

  private static final String URL = "/my/url";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Mock
  private HttpClientFactory httpClientFactory;
  @Mock
  private CloseableHttpClient httpClient;

  private CaravanHttpThreadPoolConfig threadPoolConfig;
  private CaravanHttpClient underTest;

  @Before
  public void setUp() {
    when(httpClientFactory.get(any(URI.class))).thenReturn(httpClient);

    ArchaiusConfig.initialize();

    threadPoolConfig = context.registerInjectActivateService(new CaravanHttpThreadPoolConfig(),
        ImmutableMap.of(CaravanHttpThreadPoolConfig.THREAD_POOL_NAME_PROPERTY, "default"));

    context.registerInjectActivateService(new SimpleLoadBalancerFactory());
    context.registerInjectActivateService(new LoadBalancerCommandFactory());
    context.registerService(HttpClientFactory.class, httpClientFactory);
    underTest = context.registerInjectActivateService(new CaravanHttpClientImpl());
  }

  @After
  public void tearDown() {
    MockOsgi.deactivate(underTest);
    MockOsgi.deactivate(threadPoolConfig);
  }

  @Test
  public void testAutoOnlyHost() throws Exception {
    assertUrl("myhost", "auto", "http://myhost/my/url");
  }

  @Test
  public void testAutoHostPort80() throws Exception {
    assertUrl("myhost:80", "auto", "http://myhost/my/url");
  }

  @Test
  public void testAutoHostPort8080() throws Exception {
    assertUrl("myhost:8080", "auto", "http://myhost:8080/my/url");
  }

  @Test
  public void testAutoHostPort443() throws Exception {
    assertUrl("myhost:443", "auto", "https://myhost/my/url");
  }

  @Test
  public void testAutoHostPort8443() throws Exception {
    assertUrl("myhost:8443", "auto", "https://myhost:8443/my/url");
  }

  @Test
  public void testHttpOnlyHost() throws Exception {
    assertUrl("myhost", "http", "http://myhost/my/url");
  }

  @Test
  public void testHttpHostPort80() throws Exception {
    assertUrl("myhost:80", "http", "http://myhost/my/url");
  }

  @Test
  public void testHttpHostPort8080() throws Exception {
    assertUrl("myhost:8080", "http", "http://myhost:8080/my/url");
  }

  @Test
  public void testHttpHostPort443() throws Exception {
    assertUrl("myhost:443", "http", "http://myhost:443/my/url");
  }

  @Test
  public void testHttpHostPort8443() throws Exception {
    assertUrl("myhost:8443", "http", "http://myhost:8443/my/url");
  }

  @Test
  public void testHttpsOnlyHost() throws Exception {
    assertUrl("myhost", "https", "https://myhost/my/url");
  }

  @Test
  public void testHttpsHostPort80() throws Exception {
    assertUrl("myhost:80", "https", "https://myhost/my/url");
  }

  @Test
  public void testHttpsHostPort8080() throws Exception {
    assertUrl("myhost:8080", "https", "https://myhost:8080/my/url");
  }

  @Test
  public void testHttpsHostPort443() throws Exception {
    assertUrl("myhost:443", "https", "https://myhost/my/url");
  }

  @Test
  public void testHttpsHostPort8443() throws Exception {
    assertUrl("myhost:8443", "https", "https://myhost:8443/my/url");
  }

  private static ImmutableMap<String, Object> getServiceConfigProperties(String serviceId, String hostAndPort, String protocol) {
    return ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_ID_PROPERTY, serviceId)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, hostAndPort)
        .put(CaravanHttpServiceConfig.PROTOCOL_PROPERTY, protocol)
        .build();
  }

  private void assertUrl(final String hostPort, final String protocol, final String expectedUrl) throws Exception {
    String serviceId = "protocolTestService_" + hostPort + "_" + protocol;
    CaravanHttpServiceConfig serviceConfig = context.registerInjectActivateService(new CaravanHttpServiceConfig(),
        getServiceConfigProperties(serviceId, hostPort, protocol));

    when(httpClient.execute(any(HttpUriRequest.class))).then(new Answer<CloseableHttpResponse>() {

      @Override
      public CloseableHttpResponse answer(InvocationOnMock invocation) {
        HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
        assertEquals(expectedUrl, request.getURI().toString());

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(statusLine.getReasonPhrase()).thenReturn("OK");
        when(response.getAllHeaders()).thenReturn(new Header[0]);
        when(response.getEntity()).thenReturn(entity);
        return response;
      }
    });

    underTest.execute(new CaravanHttpRequestBuilder(serviceId).append(URL).build()).toBlocking().first();
    MockOsgi.deactivate(serviceConfig);
  }

}
