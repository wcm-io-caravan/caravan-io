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

import io.wcm.caravan.io.http.httpclient.HttpClientConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper class for loading certificates for SSL communication.
 */
final class CertificateLoader {

  /**
   * Default SSL context type
   */
  public static final String SSL_CONTEXT_TYPE_DEFAULT = "TLS";

  /**
   * Default key manager type
   */
  public static final String KEY_MANAGER_TYPE_DEFAULT = "SunX509";

  /**
   * Default key store type
   */
  public static final String KEY_STORE_TYPE_DEFAULT = "PKCS12";

  /**
   * Default trust manager type
   */
  public static final String TRUST_MANAGER_TYPE_DEFAULT = "SunX509";

  /**
   * Default trust store type
   */
  public static final String TRUST_STORE_TYPE_DEFAULT = "JKS";


  private CertificateLoader() {
    // static methods only
  }

  /**
   * Build SSL Socket factory.
   * @param config Http client configuration
   * @return SSL socket factory.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static SSLContext buildSSLContext(HttpClientConfig config) throws IOException, GeneralSecurityException {

    KeyManagerFactory kmf = null;
    if (isSslKeyManagerEnabled(config)) {
      kmf = getKeyManagerFactory(config.getKeyStorePath(),
          new StoreProperties(config.getKeyStorePassword(), config.getKeyManagerType(), config.getKeyStoreType()));
    }
    TrustManagerFactory tmf = null;
    if (isSslTrustStoreEnbaled(config)) {
      StoreProperties storeProperties = new StoreProperties(config.getTrustStorePassword(),
          config.getTrustManagerType(), config.getTrustStoreType());
      tmf = getTrustManagerFactory(config.getTrustStorePath(), storeProperties);
    }

    SSLContext sslContext = SSLContext.getInstance(config.getSslContextType());
    sslContext.init(kmf != null ? kmf.getKeyManagers() : null,
        tmf != null ? tmf.getTrustManagers() : null,
            null);

    return sslContext;
  }

  /**
   * Get key manager factory
   * @param keyStoreFilename Keystore file name
   * @param storeProperties store properties
   * @return Key manager factory
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static KeyManagerFactory getKeyManagerFactory(String keyStoreFilename, StoreProperties storeProperties)
      throws IOException, GeneralSecurityException {
    InputStream is = getResourceAsStream(keyStoreFilename);
    if (is == null) {
      throw new FileNotFoundException("Certificate file not found: " + getFilenameInfo(keyStoreFilename));
    }
    try {
      return getKeyManagerFactory(is, storeProperties);
    }
    finally {
      try {
        is.close();
      }
      catch (IOException ex) {
        // ignore
      }
    }
  }

  /**
   * Get key manager factory
   * @param keyStoreStream Keystore input stream
   * @param storeProperties store properties
   * @return Key manager factory
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private static KeyManagerFactory getKeyManagerFactory(InputStream keyStoreStream, StoreProperties storeProperties)
      throws IOException, GeneralSecurityException {
    KeyStore ts = KeyStore.getInstance(storeProperties.getType());
    ts.load(keyStoreStream, storeProperties.getPassword().toCharArray());
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(storeProperties.getManagerType());
    kmf.init(ts, storeProperties.getPassword().toCharArray());
    return kmf;
  }

  /**
   * Build TrustManagerFactory.
   * @param trustStoreFilename Truststore file name
   * @param storeProperties store properties
   * @return TrustManagerFactory
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static TrustManagerFactory getTrustManagerFactory(String trustStoreFilename, StoreProperties storeProperties)
      throws IOException, GeneralSecurityException {
    InputStream is = getResourceAsStream(trustStoreFilename);
    if (is == null) {
      throw new FileNotFoundException("Certificate file not found: " + getFilenameInfo(trustStoreFilename));
    }
    try {
      return getTrustManagerFactory(is, storeProperties);
    }
    finally {
      try {
        is.close();
      }
      catch (IOException ex) {
        // ignore
      }
    }
  }

  /**
   * Build TrustManagerFactory.
   * @param trustStoreStream Truststore input stream
   * @param storeProperties store properties
   * @return TrustManagerFactory
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private static TrustManagerFactory getTrustManagerFactory(InputStream trustStoreStream, StoreProperties storeProperties)
      throws IOException, GeneralSecurityException {
    KeyStore jks = KeyStore.getInstance(storeProperties.getType());
    jks.load(trustStoreStream, storeProperties.getPassword().toCharArray());
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(storeProperties.getManagerType());
    tmf.init(jks);
    return tmf;
  }

  /**
   * Tries to load the given resource as file, or if no file exists as classpath resource.
   * @param path Filesystem or classpath path
   * @return InputStream or null if neither file nor classpath resource is found
   * @throws IOException
   */
  private static InputStream getResourceAsStream(String path) throws IOException {
    if (StringUtils.isEmpty(path)) {
      return null;
    }

    // first try to load as file
    File file = new File(path);
    if (file.exists() && file.isFile()) {
      return new FileInputStream(file);
    }

    // if not a file fallback to classloder resource
    return CertificateLoader.class.getResourceAsStream(path);
  }

  /**
   * Generate filename info for given path for error messages.
   * @param path Path
   * @return Absolute path
   */
  private static String getFilenameInfo(String path) {
    if (StringUtils.isEmpty(path)) {
      return null;
    }
    try {
      return new File(path).getCanonicalPath();
    }
    catch (IOException ex) {
      return new File(path).getAbsolutePath();
    }
  }

  /**
   * Checks whether a SSL key store is configured.
   * @param config Http client configuration
   * @return true if client certificates are enabled
   */
  public static boolean isSslKeyManagerEnabled(HttpClientConfig config) {
    return StringUtils.isNotEmpty(config.getSslContextType())
        && StringUtils.isNotEmpty(config.getKeyManagerType())
        && StringUtils.isNotEmpty(config.getKeyStoreType())
        && StringUtils.isNotEmpty(config.getKeyStorePath());
  }

  /**
   * Checks whether a SSL trust store is configured.
   * @param config Http client configuration
   * @return true if client certificates are enabled
   */
  public static boolean isSslTrustStoreEnbaled(HttpClientConfig config) {
    return StringUtils.isNotEmpty(config.getSslContextType())
        && StringUtils.isNotEmpty(config.getTrustManagerType())
        && StringUtils.isNotEmpty(config.getTrustStoreType())
        && StringUtils.isNotEmpty(config.getTrustStorePath());
  }

}
