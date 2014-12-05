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

import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.KEYSTORE_PASSWORD_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.KEYSTORE_PATH_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.TRUSTSTORE_PASSWORD_PROPERTY;
import static io.wcm.caravan.io.http.httpclient.impl.HttpClientConfigImpl.TRUSTSTORE_PATH_PROPERTY;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class CertificateLoaderTest {

  @Rule
  public OsgiContext context = new OsgiContext();

  static final String KEYSTORE_PATH = "/certificates/testcert.p12";
  static final String KEYSTORE_PASSWORD = "test-certificate";
  static final String TRUSTSTORE_PATH = "/certificates/trust.jks";
  static final String TRUSTSTORE_PASSWORD = "test-keystore";

  private static final StoreProperties STORE_PROPERTIES = new StoreProperties(TRUSTSTORE_PASSWORD,
      CertificateLoader.TRUST_MANAGER_TYPE_DEFAULT, CertificateLoader.TRUST_STORE_TYPE_DEFAULT);

  @Test
  public void testGetKeyManagerFactory() throws IOException, GeneralSecurityException {
    KeyManagerFactory keyManagerFactory = CertificateLoader.getKeyManagerFactory(
        KEYSTORE_PATH,
        new StoreProperties(KEYSTORE_PASSWORD, CertificateLoader.KEY_MANAGER_TYPE_DEFAULT, CertificateLoader.KEY_STORE_TYPE_DEFAULT));
    assertNotNull(keyManagerFactory);
  }

  @Test(expected = FileNotFoundException.class)
  public void testGetKeyManagerFactoryInvalidPath() throws IOException, GeneralSecurityException {
    CertificateLoader.getKeyManagerFactory("/invalid/path", new StoreProperties(KEYSTORE_PASSWORD, CertificateLoader.KEY_MANAGER_TYPE_DEFAULT,
        CertificateLoader.KEY_STORE_TYPE_DEFAULT));
  }

  @Test(expected = FileNotFoundException.class)
  public void testGetKeyManagerFactoryNullPath() throws IOException, GeneralSecurityException {
    CertificateLoader.getKeyManagerFactory(null, new StoreProperties(KEYSTORE_PASSWORD, CertificateLoader.KEY_MANAGER_TYPE_DEFAULT,
        CertificateLoader.KEY_STORE_TYPE_DEFAULT));
  }

  @Test
  public void testGetTrustManagerFactory() throws IOException, GeneralSecurityException {
    TrustManagerFactory trustManagerFactory = CertificateLoader.getTrustManagerFactory(TRUSTSTORE_PATH, STORE_PROPERTIES);
    assertNotNull(trustManagerFactory);
  }

  @Test(expected = FileNotFoundException.class)
  public void testGetTrustManagerFactoryInvalidPath() throws IOException, GeneralSecurityException {
    CertificateLoader.getTrustManagerFactory("/invalid/path", STORE_PROPERTIES);
  }

  @Test(expected = FileNotFoundException.class)
  public void testGetTrustManagerFactoryNullPath() throws IOException, GeneralSecurityException {
    CertificateLoader.getTrustManagerFactory(null, STORE_PROPERTIES);
  }

  @Test
  public void testBuildSSLContext() throws IOException, GeneralSecurityException {

    HttpClientConfigImpl config = context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(KEYSTORE_PATH_PROPERTY, KEYSTORE_PATH)
        .put(KEYSTORE_PASSWORD_PROPERTY, KEYSTORE_PASSWORD)
        .put(TRUSTSTORE_PATH_PROPERTY, TRUSTSTORE_PATH)
        .put(TRUSTSTORE_PASSWORD_PROPERTY, TRUSTSTORE_PASSWORD)
        .build());

    SSLContext sslContext = CertificateLoader.buildSSLContext(config);
    assertNotNull(sslContext);
  }

  @Test
  public void testBuildSSLContextWithoutKeyStore() throws IOException, GeneralSecurityException {

    HttpClientConfigImpl config = context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(TRUSTSTORE_PATH_PROPERTY, TRUSTSTORE_PATH)
        .put(TRUSTSTORE_PASSWORD_PROPERTY, TRUSTSTORE_PASSWORD)
        .build());

    SSLContext sslContext = CertificateLoader.buildSSLContext(config);
    assertNotNull(sslContext);
  }

  @Test
  public void testBuildSSLContextWithoutTrustSTore() throws IOException, GeneralSecurityException {

    HttpClientConfigImpl config = context.registerInjectActivateService(new HttpClientConfigImpl(),
        ImmutableMap.<String, Object>builder()
        .put(KEYSTORE_PATH_PROPERTY, KEYSTORE_PATH)
        .put(KEYSTORE_PASSWORD_PROPERTY, KEYSTORE_PASSWORD)
        .build());

    SSLContext sslContext = CertificateLoader.buildSSLContext(config);
    assertNotNull(sslContext);
  }

}
