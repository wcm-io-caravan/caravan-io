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
package io.wcm.caravan.io.http.httpclient.impl;

import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.CONNECT_TIMEOUT_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.HOST_PATTERNS_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.HTTP_PASSWORD_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.HTTP_USER_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.KEYMANAGER_TYPE_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.KEYSTORE_PASSWORD_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.KEYSTORE_PATH_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.KEYSTORE_TYPE_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.MAX_CONNECTIONS_PER_HOST_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.MAX_TOTAL_CONNECTIONS_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.PROXY_HOST_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.PROXY_PASSWORD_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.PROXY_PORT_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.PROXY_USER_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.SOCKET_TIMEOUT_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.SSL_CONTEXT_TYPE_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.TRUSTMANAGER_TYPE_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.TRUSTSTORE_PASSWORD_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.TRUSTSTORE_PATH_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.TRUSTSTORE_TYPE_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.WS_ADDRESSINGTO_URIS_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.io.http.httpclient.HttpClientConfig;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class HttpClientConfigImplTest {

  @Rule
  public OsgiContext context = new OsgiContext();

  @Test
  public void testDefaultValues() {
    HttpClientConfigImpl config = context.registerInjectActivateService(new HttpClientConfigImpl());

    assertEquals(CONNECT_TIMEOUT_PROPERTY, HttpClientConfig.CONNECT_TIMEOUT_DEFAULT, config.getConnectTimeout());
    assertEquals(SOCKET_TIMEOUT_PROPERTY, HttpClientConfig.SOCKET_TIMEOUT_DEFAULT, config.getSocketTimeout());
    assertEquals(MAX_CONNECTIONS_PER_HOST_PROPERTY, HttpClientConfig.MAX_CONNECTIONS_PER_HOST_DEFAULT, config.getMaxConnectionsPerHost());
    assertEquals(MAX_TOTAL_CONNECTIONS_PROPERTY, HttpClientConfig.MAX_TOTAL_CONNECTIONS_DEFAULT, config.getMaxTotalConnections());
    assertNull(HTTP_USER_PROPERTY, config.getHttpUser());
    assertNull(HTTP_PASSWORD_PROPERTY, config.getHttpPassword());
    assertNull(PROXY_HOST_PROPERTY, config.getProxyHost());
    assertEquals(PROXY_PORT_PROPERTY, 0, config.getProxyPort());
    assertNull(PROXY_USER_PROPERTY, config.getProxyUser());
    assertNull(PROXY_PASSWORD_PROPERTY, config.getProxyPassword());
    assertTrue(HOST_PATTERNS_PROPERTY, config.matchesHost("h1"));
    assertTrue(WS_ADDRESSINGTO_URIS_PROPERTY, config.matchesWsAddressingToUri("http://uri1"));

    assertEquals(SSL_CONTEXT_TYPE_PROPERTY, CertificateLoader.SSL_CONTEXT_TYPE_DEFAULT, config.getSslContextType());
    assertEquals(KEYMANAGER_TYPE_PROPERTY, CertificateLoader.KEY_MANAGER_TYPE_DEFAULT, config.getKeyManagerType());
    assertEquals(KEYSTORE_TYPE_PROPERTY, CertificateLoader.KEY_STORE_TYPE_DEFAULT, config.getKeyStoreType());
    assertNull(KEYSTORE_PATH_PROPERTY, config.getKeyStorePath());
    assertNull(KEYSTORE_PASSWORD_PROPERTY, config.getKeyStorePassword());
    assertEquals(TRUSTMANAGER_TYPE_PROPERTY, CertificateLoader.TRUST_MANAGER_TYPE_DEFAULT, config.getTrustManagerType());
    assertEquals(TRUSTSTORE_TYPE_PROPERTY, CertificateLoader.TRUST_STORE_TYPE_DEFAULT, config.getTrustStoreType());
    assertNull(TRUSTSTORE_PATH_PROPERTY, config.getTrustStorePath());
    assertNull(TRUSTSTORE_PASSWORD_PROPERTY, config.getTrustStorePassword());

    assertNotNull(config.toString());
  }

  @Test
  public void testReadFromConfig() {
    HttpClientConfigImpl config = context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(CONNECT_TIMEOUT_PROPERTY, 1)
        .put(SOCKET_TIMEOUT_PROPERTY, 2)
        .put(MAX_CONNECTIONS_PER_HOST_PROPERTY, 3)
        .put(MAX_TOTAL_CONNECTIONS_PROPERTY, 4)
        .put(HTTP_USER_PROPERTY, "httpUsr")
        .put(HTTP_PASSWORD_PROPERTY, "httpPwd")
        .put(PROXY_HOST_PROPERTY, "abc")
        .put(PROXY_PORT_PROPERTY, 5)
        .put(PROXY_USER_PROPERTY, "def")
        .put(PROXY_PASSWORD_PROPERTY, "ghi")
        .put(HOST_PATTERNS_PROPERTY, new String[] {
            "h1",
            "h2",
            "h3"
        })
        .put(WS_ADDRESSINGTO_URIS_PROPERTY, new String[] {
            "http://uri1",
            "http://uri2"
        })
            .put(SSL_CONTEXT_TYPE_PROPERTY, "ssltype")
            .put(KEYMANAGER_TYPE_PROPERTY, "keymantype")
            .put(KEYSTORE_TYPE_PROPERTY, "keystoretype")
            .put(KEYSTORE_PATH_PROPERTY, "keypath")
            .put(KEYSTORE_PASSWORD_PROPERTY, "keypasswd")
            .put(TRUSTMANAGER_TYPE_PROPERTY, "trustmantype")
            .put(TRUSTSTORE_TYPE_PROPERTY, "truststoretype")
            .put(TRUSTSTORE_PATH_PROPERTY, "trustpath")
            .put(TRUSTSTORE_PASSWORD_PROPERTY, "trustpasswd")
        .build());

    assertEquals(CONNECT_TIMEOUT_PROPERTY, 1, config.getConnectTimeout());
    assertEquals(SOCKET_TIMEOUT_PROPERTY, 2, config.getSocketTimeout());
    assertEquals(MAX_CONNECTIONS_PER_HOST_PROPERTY, 3, config.getMaxConnectionsPerHost());
    assertEquals(MAX_TOTAL_CONNECTIONS_PROPERTY, 4, config.getMaxTotalConnections());
    assertEquals(HTTP_USER_PROPERTY, "httpUsr", config.getHttpUser());
    assertEquals(HTTP_PASSWORD_PROPERTY, "httpPwd", config.getHttpPassword());
    assertEquals(PROXY_HOST_PROPERTY, "abc", config.getProxyHost());
    assertEquals(PROXY_PORT_PROPERTY, 5, config.getProxyPort());
    assertEquals(PROXY_USER_PROPERTY, "def", config.getProxyUser());
    assertEquals(PROXY_PASSWORD_PROPERTY, "ghi", config.getProxyPassword());
    assertTrue("hostNames.0", config.matchesHost("h1"));
    assertTrue("hostNames.1", config.matchesHost("h2"));
    assertTrue("hostNames.2", config.matchesHost("h3"));
    assertFalse("hostNames.false", config.matchesHost("h4"));
    assertTrue("wsAddressingToUris.0", config.matchesWsAddressingToUri("http://uri1"));
    assertTrue("wsAddressingToUris.1", config.matchesWsAddressingToUri("http://uri2"));
    assertFalse("wsAddressingToUris.false", config.matchesWsAddressingToUri("http://uri3"));

    assertEquals(SSL_CONTEXT_TYPE_PROPERTY, "ssltype", config.getSslContextType());
    assertEquals(KEYMANAGER_TYPE_PROPERTY, "keymantype", config.getKeyManagerType());
    assertEquals(KEYSTORE_TYPE_PROPERTY, "keystoretype", config.getKeyStoreType());
    assertEquals(KEYSTORE_PATH_PROPERTY, "keypath", config.getKeyStorePath());
    assertEquals(KEYSTORE_PASSWORD_PROPERTY, "keypasswd", config.getKeyStorePassword());
    assertEquals(TRUSTMANAGER_TYPE_PROPERTY, "trustmantype", config.getTrustManagerType());
    assertEquals(TRUSTSTORE_TYPE_PROPERTY, "truststoretype", config.getTrustStoreType());
    assertEquals(TRUSTSTORE_PATH_PROPERTY, "trustpath", config.getTrustStorePath());
    assertEquals(TRUSTSTORE_PASSWORD_PROPERTY, "trustpasswd", config.getTrustStorePassword());

    assertNotNull(config.toString());
  }

}
