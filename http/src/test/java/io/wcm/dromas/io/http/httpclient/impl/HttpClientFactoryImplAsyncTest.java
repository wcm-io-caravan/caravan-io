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

import static io.wcm.dromas.io.http.httpclient.impl.HttpClientConfigImpl.CONNECT_TIMEOUT_PROPERTY;
import static io.wcm.dromas.io.http.httpclient.impl.HttpClientConfigImpl.HOST_PATTERNS_PROPERTY;
import static io.wcm.dromas.io.http.httpclient.impl.HttpClientConfigImpl.WS_ADDRESSINGTO_URIS_PROPERTY;
import static org.junit.Assert.assertEquals;
import io.wcm.dromas.io.http.httpclient.HttpClientConfig;
import io.wcm.dromas.io.http.httpclient.HttpClientFactory;

import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class HttpClientFactoryImplAsyncTest {

  @Rule
  public OsgiContext context = new OsgiContext();

  @Test
  public void testClientSelection() {

    context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 55)
        .put(HOST_PATTERNS_PROPERTY, new String[] {
            "host1"
        })
        .put(Constants.SERVICE_RANKING, 10)
        .build());

    context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 66)
        .put(HOST_PATTERNS_PROPERTY, new String[] {
            "host2"
        })
        .put(Constants.SERVICE_RANKING, 20)
        .build());

    HttpClientFactory underTest = context.registerInjectActivateService(new HttpClientFactoryImpl());

    HttpAsyncClient client1 = underTest.getHttpAsyncClient("http://host1/xyz");
    assertEquals("client1.timeout", 55, HttpClientTestUtils.getConnectTimeout(client1));

    HttpAsyncClient client2 = underTest.getHttpAsyncClient("http://host2/xyz");
    assertEquals("client2.timeout", 66, HttpClientTestUtils.getConnectTimeout(client2));

    HttpAsyncClient client3 = underTest.getHttpAsyncClient("http://host3/xyz");
    assertEquals("client3.timeout", HttpClientConfig.CONNECT_TIMEOUT_DEFAULT, HttpClientTestUtils.getConnectTimeout(client3));

    MockOsgi.deactivate(underTest);
  }

  @Test
  public void testClientSelectionWithAllMatchesConfigIncluded() {

    context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 55)
        .put(HOST_PATTERNS_PROPERTY, new String[] {
            "host1"
        })
        .put(Constants.SERVICE_RANKING, 10)
        .build());

    context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 66)
        .put(Constants.SERVICE_RANKING, 20)
        .build());

    HttpClientFactory underTest = context.registerInjectActivateService(new HttpClientFactoryImpl());

    HttpAsyncClient client1 = underTest.getHttpAsyncClient("http://host1/xyz");
    assertEquals("client1.timeout", 55, HttpClientTestUtils.getConnectTimeout(client1));

    HttpAsyncClient client2 = underTest.getHttpAsyncClient("http://host2/xyz");
    assertEquals("client2.timeout", 66, HttpClientTestUtils.getConnectTimeout(client2));

    HttpAsyncClient client3 = underTest.getHttpAsyncClient("http://host3/xyz");
    assertEquals("client3.timeout", 66, HttpClientTestUtils.getConnectTimeout(client3));

    MockOsgi.deactivate(underTest);
  }

  @Test
  public void testGetHttpClientConfigForWebservice() {

    context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 55)
        .put(HOST_PATTERNS_PROPERTY, new String[] {
            "host1"
        })
        .put(WS_ADDRESSINGTO_URIS_PROPERTY, new String[] {
            "http://uri1"
        })
        .put(Constants.SERVICE_RANKING, 10)
        .build());

    context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 66)
        .put(HOST_PATTERNS_PROPERTY, new String[] {
            "host2"
        })
        .put(Constants.SERVICE_RANKING, 20)
        .build());

    HttpClientFactory underTest = context.registerInjectActivateService(new HttpClientFactoryImpl());

    HttpAsyncClient client1a = underTest.getWsHttpAsyncClient("http://host1/xyz", "http://uri1");
    assertEquals("client1a.timeout", 55, HttpClientTestUtils.getConnectTimeout(client1a));

    HttpAsyncClient client1b = underTest.getWsHttpAsyncClient("http://host1/xyz", "http://uri2");
    assertEquals("client1b.timeout", 15000, HttpClientTestUtils.getConnectTimeout(client1b));

    HttpAsyncClient client1c = underTest.getWsHttpAsyncClient("http://host1/xyz", null);
    assertEquals("client1c.timeout", 15000, HttpClientTestUtils.getConnectTimeout(client1c));

    HttpAsyncClient client2a = underTest.getWsHttpAsyncClient("http://host2/xyz", "http://uri1");
    assertEquals("client2a.timeout", 66, HttpClientTestUtils.getConnectTimeout(client2a));

    HttpAsyncClient client2b = underTest.getWsHttpAsyncClient("http://host2/xyz", "http://uri2");
    assertEquals("client2b.timeout", 66, HttpClientTestUtils.getConnectTimeout(client2b));

    HttpAsyncClient client2c = underTest.getWsHttpAsyncClient("http://host2/xyz", null);
    assertEquals("client2c.timeout", 66, HttpClientTestUtils.getConnectTimeout(client2c));

    MockOsgi.deactivate(underTest);
  }

}
