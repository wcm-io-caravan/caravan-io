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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.io.http.httpclient.HttpClientConfig;

import org.junit.Test;

public class DefaultHttpClientConfigTest {

  @Test
  public void testInstance() {
    HttpClientConfig config = DefaultHttpClientConfig.INSTANCE;

    assertEquals("timeoutConnect", HttpClientConfig.CONNECT_TIMEOUT_DEFAULT, config.getConnectTimeout());
    assertEquals("timeoutResponse", HttpClientConfig.SOCKET_TIMEOUT_DEFAULT, config.getSocketTimeout());
    assertEquals("maxConnectionsPerHost", HttpClientConfig.MAX_CONNECTIONS_PER_HOST_DEFAULT, config.getMaxConnectionsPerHost());
    assertEquals("maxTotalConnections", HttpClientConfig.MAX_TOTAL_CONNECTIONS_DEFAULT, config.getMaxTotalConnections());
    assertNull("httpUser", config.getHttpUser());
    assertNull("httpPassword", config.getHttpPassword());
    assertNull("proxyHost", config.getProxyHost());
    assertEquals("proxyPort", 0, config.getProxyPort());
    assertNull("proxyUser", config.getProxyUser());
    assertNull("proxyPassword", config.getProxyPassword());
    assertTrue("hostNames", config.matchesHost("h1"));
    assertTrue("wsAddressingToUris", config.matchesWsAddressingToUri("http://uri1"));

    assertEquals("sslContextType", CertificateLoader.SSL_CONTEXT_TYPE_DEFAULT, config.getSslContextType());
    assertEquals("keyManagerType", CertificateLoader.KEY_MANAGER_TYPE_DEFAULT, config.getKeyManagerType());
    assertEquals("keyStoreType", CertificateLoader.KEY_STORE_TYPE_DEFAULT, config.getKeyStoreType());
    assertNull("keyStorePath", config.getKeyStorePath());
    assertNull("keyStorePassword", config.getKeyStorePassword());
    assertEquals("trustManagerType", CertificateLoader.TRUST_MANAGER_TYPE_DEFAULT, config.getTrustManagerType());
    assertEquals("trustStoreType", CertificateLoader.TRUST_STORE_TYPE_DEFAULT, config.getTrustStoreType());
    assertNull("trustStorePath", config.getTrustStorePath());
    assertNull("trustStorePassword", config.getTrustStorePassword());

    assertNotNull(config.toString());
  }

}
