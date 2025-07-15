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
package io.wcm.caravan.io.http.impl.ribbon;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.io.http.impl.ApacheHttpClient;
import io.wcm.caravan.io.http.impl.ArchaiusConfig;
import io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig;
import io.wcm.caravan.io.http.impl.CaravanHttpThreadPoolConfig;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import rx.Observable;

/**
 * Integration tests for HTTP communcation of transport layer.
 */
@RunWith(MockitoJUnitRunner.class)
public class RibbonHttpClientProtocolTest {

  private static final String URL = "/my/url";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Mock
  private ApacheHttpClient apacheClient;

  private RibbonHttpClient underTest;

  @Before
  public void setUp() {

    ArchaiusConfig.initialize();

    context.registerInjectActivateService(new CaravanHttpThreadPoolConfig(),
        ImmutableMap.of(CaravanHttpThreadPoolConfig.THREAD_POOL_NAME_PROPERTY, "default"));

    context.registerInjectActivateService(new SimpleLoadBalancerFactory());
    context.registerInjectActivateService(new CachingLoadBalancerFactory());
    context.registerInjectActivateService(new LoadBalancerCommandFactory());
    context.registerService(ApacheHttpClient.class, apacheClient);

    underTest = context.registerInjectActivateService(new RibbonHttpClient());
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

    when(apacheClient.execute(any())).then(new Answer<Observable<CaravanHttpResponse>>() {

      @Override
      public Observable<CaravanHttpResponse> answer(InvocationOnMock invocation) {
        CaravanHttpRequest request = invocation.getArgument(0);
        assertEquals(expectedUrl, request.getUrl().toString());
        CaravanHttpResponse response = new CaravanHttpResponseBuilder().status(200).reason("OK").build();
        return Observable.just(response);
      }
    });

    underTest.execute(new CaravanHttpRequestBuilder(serviceId).append(URL).build()).toBlocking().first();
    MockOsgi.deactivate(serviceConfig, context.bundleContext());
  }

}
