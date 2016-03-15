/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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

import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.HTTP_PARAM_PROTOCOL;
import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.PROTOCOL_PROPERTY;
import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY;
import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.RIBBON_PARAM_LISTOFSERVERS;
import static io.wcm.caravan.io.http.impl.CaravanHttpServiceConfig.SERVICE_ID_PROPERTY;
import static io.wcm.caravan.io.http.impl.RequestUtil.PROTOCOL_AUTO;
import static io.wcm.caravan.io.http.impl.RequestUtil.PROTOCOL_HTTP;
import static io.wcm.caravan.io.http.impl.RequestUtil.PROTOCOL_HTTPS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.http.impl.ribbon.LoadBalancerCommandFactory;
import io.wcm.caravan.io.http.impl.ribbon.SimpleLoadBalancerFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class CaravanHttpServiceConfigProtocolTest {

  private static final String SERVICE_ID = "/my/service";

  @Rule
  public OsgiContext context = new OsgiContext();

  private Configuration archaiusConfig;

  @Before
  public void setUp() {
    ArchaiusConfig.initialize();
    archaiusConfig = ArchaiusConfig.getConfiguration();
    context.registerInjectActivateService(new SimpleLoadBalancerFactory());
    context.registerInjectActivateService(new LoadBalancerCommandFactory());
  }

  @Test
  public void testAutoSimpleServerList() {
    registerConfig(PROTOCOL_AUTO, "a:8000", "b");
    assertRibbonConfig(RequestUtil.PROTOCOL_AUTO, "a:8000", "b");
  }

  @Test
  public void testAutoServerListWithProtocols() {
    registerConfig(PROTOCOL_AUTO, "http://a:8000", "http://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  @Test
  public void testAutoServerListWithProtocolsHttps() {
    registerConfig(PROTOCOL_AUTO, "https://a:8000", "https://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTPS, "a:8000", "b");
  }

  @Test
  public void testAutoServerListWithDifferentProtocols() {
    registerConfig(PROTOCOL_AUTO, "https://a:8000", "http://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  @Test
  public void testHttpSimpleServerList() {
    registerConfig(PROTOCOL_HTTP, "a:8000", "b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  @Test
  public void testHttpServerListWithProtocols() {
    registerConfig(PROTOCOL_HTTP, "http://a:8000", "http://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  @Test
  public void testHttpServerListWithProtocolsHttps() {
    registerConfig(PROTOCOL_HTTP, "https://a:8000", "https://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTPS, "a:8000", "b");
  }

  @Test
  public void testHttpServerListWithDifferentProtocols() {
    registerConfig(PROTOCOL_HTTP, "https://a:8000", "http://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  @Test
  public void testHttpsSimpleServerList() {
    registerConfig(PROTOCOL_HTTPS, "a:8000", "b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTPS, "a:8000", "b");
  }

  @Test
  public void testHttpsServerListWithProtocols() {
    registerConfig(PROTOCOL_HTTPS, "http://a:8000", "http://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  @Test
  public void testHttpsServerListWithProtocolsHttps() {
    registerConfig(PROTOCOL_HTTPS, "https://a:8000", "https://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTPS, "a:8000", "b");
  }

  @Test
  public void testHttpsServerListWithDifferentProtocols() {
    registerConfig(PROTOCOL_HTTPS, "https://a:8000", "http://b");
    assertRibbonConfig(RequestUtil.PROTOCOL_HTTP, "a:8000", "b");
  }

  private void registerConfig(String defaultProtocol, String... servers) {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(),
        ImmutableMap.<String, Object>builder()
            .put(SERVICE_ID_PROPERTY, SERVICE_ID)
            .put(RIBBON_HOSTS_PROPERTY, servers)
            .put(PROTOCOL_PROPERTY, defaultProtocol)
            .build());
  }

  private void assertRibbonConfig(String defaultProtocol, String... servers) {
    assertArrayEquals(servers, archaiusConfig.getStringArray(SERVICE_ID + RIBBON_PARAM_LISTOFSERVERS));
    assertEquals(defaultProtocol, archaiusConfig.getString(SERVICE_ID + HTTP_PARAM_PROTOCOL));
  }

}
