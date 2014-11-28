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

import io.wcm.dromas.io.http.httpclient.HttpClientConfig;

/**
 * Default http client configuration.
 */
final class DefaultHttpClientConfig extends AbstractHttpClientconfig {

  public static final HttpClientConfig INSTANCE = new DefaultHttpClientConfig();

  private DefaultHttpClientConfig() {
    // singleton only
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public int getConnectTimeout() {
    return HttpClientConfig.CONNECT_TIMEOUT_DEFAULT;
  }

  @Override
  public int getSocketTimeout() {
    return HttpClientConfig.SOCKET_TIMEOUT_DEFAULT;
  }

  @Override
  public int getMaxConnectionsPerHost() {
    return HttpClientConfig.MAX_CONNECTIONS_PER_HOST_DEFAULT;
  }

  @Override
  public int getMaxTotalConnections() {
    return HttpClientConfig.MAX_TOTAL_CONNECTIONS_DEFAULT;
  }

  @Override
  public String getHttpUser() {
    return null;
  }

  @Override
  public String getHttpPassword() {
    return null;
  }

  @Override
  public String getProxyHost() {
    return null;
  }

  @Override
  public int getProxyPort() {
    return 0;
  }

  @Override
  public String getProxyUser() {
    return null;
  }

  @Override
  public String getProxyPassword() {
    return null;
  }

  @Override
  public boolean matchesHost(String host) {
    return true;
  }

  @Override
  public boolean matchesWsAddressingToUri(String addressingToUri) {
    return true;
  }

  @Override
  public String getSslContextType() {
    return CertificateLoader.SSL_CONTEXT_TYPE_DEFAULT;
  }

  @Override
  public String getKeyManagerType() {
    return CertificateLoader.KEY_MANAGER_TYPE_DEFAULT;
  }

  @Override
  public String getKeyStoreType() {
    return CertificateLoader.KEY_STORE_TYPE_DEFAULT;
  }

  @Override
  public String getKeyStorePath() {
    return null;
  }

  @Override
  public String getKeyStorePassword() {
    return null;
  }

  @Override
  public String getTrustManagerType() {
    return CertificateLoader.TRUST_MANAGER_TYPE_DEFAULT;
  }

  @Override
  public String getTrustStoreType() {
    return CertificateLoader.TRUST_STORE_TYPE_DEFAULT;
  }

  @Override
  public String getTrustStorePath() {
    return null;
  }

  @Override
  public String getTrustStorePassword() {
    return null;
  }

}
