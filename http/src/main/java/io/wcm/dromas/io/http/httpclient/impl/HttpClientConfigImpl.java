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

import io.wcm.dromas.commons.stream.Collectors;
import io.wcm.dromas.commons.stream.Streams;
import io.wcm.dromas.io.http.httpclient.HttpClientConfig;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Default implementation of {@link HttpClientConfig}.
 */
@Component(metatype = true, immediate = true,
    label = "HTTP Client Configuration",
description = "Allows to configure special HTTP client settings for target hosts",
configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service(HttpClientConfig.class)
@Property(name = "webconsole.configurationFactory.nameHint", value = "{hostPatterns} {wsAddressingToUris}")
public class HttpClientConfigImpl extends AbstractHttpClientconfig {

  @Property(label = "Host pattern", description = "Regular expressions for matching the host name(s)",
      cardinality = Integer.MAX_VALUE)
  static final String HOST_PATTERNS_PROPERTY = "hostPatterns";
  @Property(label = "WS Uri", description = "List of WS Addressing To URIs for SOAP calls",
      cardinality = Integer.MAX_VALUE)
  static final String WS_ADDRESSINGTO_URIS_PROPERTY = "wsAddressingToUris";

  @Property(label = "Connect timeout", description = "Max. timeout to wait for HTTP connection (ms)",
      intValue = HttpClientConfig.CONNECT_TIMEOUT_DEFAULT)
  static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
  @Property(label = "Socket timeout", description = "Max. timeout to wait for a HTTP response (ms)",
      intValue = HttpClientConfig.SOCKET_TIMEOUT_DEFAULT)
  static final String SOCKET_TIMEOUT_PROPERTY = "socketTimeout";
  @Property(label = "Max per host", description = "Max connections per host",
      intValue = HttpClientConfig.MAX_CONNECTIONS_PER_HOST_DEFAULT)
  static final String MAX_CONNECTIONS_PER_HOST_PROPERTY = "maxConnectionsPerHost";
  @Property(label = "Max total", description = "Max total connections",
      intValue = HttpClientConfig.MAX_TOTAL_CONNECTIONS_DEFAULT)
  static final String MAX_TOTAL_CONNECTIONS_PROPERTY = "maxTotalConnections";
  @Property(label = "Http user", description = "User name for basic HTTP authentication")
  static final String HTTP_USER_PROPERTY = "httpUser";
  @Property(label = "Http password", description = "Password for basic HTTP authentication")
  static final String HTTP_PASSWORD_PROPERTY = "httpPassword";
  @Property(label = "Proxy host", description = "Proxy hostname")
  static final String PROXY_HOST_PROPERTY = "proxyHost";
  @Property(label = "Proxy port", description = "Proxy port", intValue = 0)
  static final String PROXY_PORT_PROPERTY = "proxyPort";
  @Property(label = "Proxy user", description = "Proxy user name")
  static final String PROXY_USER_PROPERTY = "proxyUser";
  @Property(label = "Proxy password", description = "Proxy password")
  static final String PROXY_PASSWORD_PROPERTY = "proxyPassword";

  @Property(label = "SSL context type", description = "SSL context type",
      value = CertificateLoader.SSL_CONTEXT_TYPE_DEFAULT)
  static final String SSL_CONTEXT_TYPE_PROPERTY = "sslContextType";
  @Property(label = "KeyManager type", description = "KeyManager type",
      value = CertificateLoader.KEY_MANAGER_TYPE_DEFAULT)
  static final String KEYMANAGER_TYPE_PROPERTY = "keyManagerType";
  @Property(label = "KeyStore type", description = "KeyStore type",
      value = CertificateLoader.KEY_STORE_TYPE_DEFAULT)
  static final String KEYSTORE_TYPE_PROPERTY = "keyStoreType";
  @Property(label = "KeyStore path", description = "KeyStore path")
  static final String KEYSTORE_PATH_PROPERTY = "keyStorePath";
  @Property(label = "KeyStore password", description = "KeyStore password")
  static final String KEYSTORE_PASSWORD_PROPERTY = "keyStorePassword";
  @Property(label = "TrustManager type", description = "TrustManager type",
      value = CertificateLoader.TRUST_MANAGER_TYPE_DEFAULT)
  static final String TRUSTMANAGER_TYPE_PROPERTY = "trustManagerType";
  @Property(label = "TrustStore type", description = "TrustStore type",
      value = CertificateLoader.TRUST_STORE_TYPE_DEFAULT)
  static final String TRUSTSTORE_TYPE_PROPERTY = "trustStoreType";
  @Property(label = "TrustStore path", description = "TrustStore path")
  static final String TRUSTSTORE_PATH_PROPERTY = "trustStorePath";
  @Property(label = "TrustStore password", description = "TrustStore password")
  static final String TRUSTSTORE_PASSWORD_PROPERTY = "trustStorePassword";

  @Property(label = "Enabled", description = "Enable this HTTP client configuration",
      boolValue = HttpClientConfigImpl.ENABLED_DEFAULT)
  static final String ENABLED_PROPERTY = "enabled";
  static final boolean ENABLED_DEFAULT = true;
  @Property(label = "Service Ranking", description = "Allows to define an order in which the HTTP client configurations are evaluated. Lower value = higher ranking.",
      propertyPrivate = false)
  private static final String SERVICE_RANKING = Constants.SERVICE_RANKING;

  private boolean enabled;

  private int connectTimeout;
  private int socketTimeout;
  private int maxConnectionsPerHost;
  private int maxTotalConnections;
  private String httpUser;
  private String httpPassword;
  private String proxyHost;
  private int proxyPort;
  private String proxyUser;
  private String proxyPassword;
  private Set<Pattern> hostPatterns;
  private Set<String> wsAddressingToUris;

  private String sslContextType;
  private String keyManagerType;
  private String keyStoreType;
  private String keyStorePath;
  private String keyStorePassword;
  private String trustManagerType;
  private String trustStoreType;
  private String trustStorePath;
  private String trustStorePassword;

  private static final Logger log = LoggerFactory.getLogger(HttpClientConfigImpl.class);

  @Activate
  private void activate(Map<String, Object> config) {
    enabled = PropertiesUtil.toBoolean(config.get(ENABLED_PROPERTY), ENABLED_DEFAULT);

    connectTimeout = PropertiesUtil.toInteger(config.get(CONNECT_TIMEOUT_PROPERTY), HttpClientConfig.CONNECT_TIMEOUT_DEFAULT);
    socketTimeout = PropertiesUtil.toInteger(config.get(SOCKET_TIMEOUT_PROPERTY), HttpClientConfig.SOCKET_TIMEOUT_DEFAULT);
    maxConnectionsPerHost = PropertiesUtil.toInteger(config.get(MAX_CONNECTIONS_PER_HOST_PROPERTY), HttpClientConfig.MAX_CONNECTIONS_PER_HOST_DEFAULT);
    maxTotalConnections = PropertiesUtil.toInteger(config.get(MAX_TOTAL_CONNECTIONS_PROPERTY), HttpClientConfig.MAX_TOTAL_CONNECTIONS_DEFAULT);
    httpUser = PropertiesUtil.toString(config.get(HTTP_USER_PROPERTY), null);
    httpPassword = PropertiesUtil.toString(config.get(HTTP_PASSWORD_PROPERTY), null);
    proxyHost = PropertiesUtil.toString(config.get(PROXY_HOST_PROPERTY), null);
    proxyPort = PropertiesUtil.toInteger(config.get(PROXY_PORT_PROPERTY), 0);
    proxyUser = PropertiesUtil.toString(config.get(PROXY_USER_PROPERTY), null);
    proxyPassword = PropertiesUtil.toString(config.get(PROXY_PASSWORD_PROPERTY), null);
    hostPatterns = Streams.of(ImmutableSet.copyOf(PropertiesUtil.toStringArray(config.get(HOST_PATTERNS_PROPERTY), new String[0])))
        .filter(StringUtils::isNotBlank)
        .map(
            patternString -> {
              try {
                return Pattern.compile(patternString);
              }
              catch (PatternSyntaxException ex) {
                log.warn("Invalid host name pattern '" + patternString + "': " + ex.getMessage(), ex);
                this.enabled = false;
                return null;
              }
            })
            .collect(Collectors.toSet());
    wsAddressingToUris = Streams.of(ImmutableSet.copyOf(PropertiesUtil.toStringArray(config.get(WS_ADDRESSINGTO_URIS_PROPERTY), new String[0])))
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());

    sslContextType = PropertiesUtil.toString(config.get(SSL_CONTEXT_TYPE_PROPERTY), CertificateLoader.SSL_CONTEXT_TYPE_DEFAULT);
    keyManagerType = PropertiesUtil.toString(config.get(KEYMANAGER_TYPE_PROPERTY), CertificateLoader.KEY_MANAGER_TYPE_DEFAULT);
    keyStoreType = PropertiesUtil.toString(config.get(KEYSTORE_TYPE_PROPERTY), CertificateLoader.KEY_STORE_TYPE_DEFAULT);
    keyStorePath = PropertiesUtil.toString(config.get(KEYSTORE_PATH_PROPERTY), null);
    keyStorePassword = PropertiesUtil.toString(config.get(KEYSTORE_PASSWORD_PROPERTY), null);
    trustManagerType = PropertiesUtil.toString(config.get(TRUSTMANAGER_TYPE_PROPERTY), CertificateLoader.TRUST_MANAGER_TYPE_DEFAULT);
    trustStoreType = PropertiesUtil.toString(config.get(TRUSTSTORE_TYPE_PROPERTY), CertificateLoader.TRUST_STORE_TYPE_DEFAULT);
    trustStorePath = PropertiesUtil.toString(config.get(TRUSTSTORE_PATH_PROPERTY), null);
    trustStorePassword = PropertiesUtil.toString(config.get(TRUSTSTORE_PASSWORD_PROPERTY), null);
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public int getConnectTimeout() {
    return connectTimeout;
  }

  @Override
  public int getSocketTimeout() {
    return socketTimeout;
  }

  @Override
  public int getMaxConnectionsPerHost() {
    return maxConnectionsPerHost;
  }

  @Override
  public int getMaxTotalConnections() {
    return maxTotalConnections;
  }

  @Override
  public String getHttpUser() {
    return httpUser;
  }

  @Override
  public String getHttpPassword() {
    return httpPassword;
  }

  @Override
  public String getProxyHost() {
    return proxyHost;
  }

  @Override
  public int getProxyPort() {
    return proxyPort;
  }

  @Override
  public String getProxyUser() {
    return proxyUser;
  }

  @Override
  public String getProxyPassword() {
    return proxyPassword;
  }

  @Override
  public boolean matchesHost(String host) {
    if (hostPatterns.isEmpty()) {
      return true;
    }
    if (StringUtils.isEmpty(host)) {
      return false;
    }
    for (Pattern hostPattern : hostPatterns) {
      if (hostPattern.matcher(host).matches()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matchesWsAddressingToUri(String addressingToUri) {
    if (wsAddressingToUris.isEmpty()) {
      return true;
    }
    if (StringUtils.isEmpty(addressingToUri)) {
      return false;
    }
    return wsAddressingToUris.contains(addressingToUri);
  }

  @Override
  public String getSslContextType() {
    return sslContextType;
  }

  @Override
  public String getKeyManagerType() {
    return keyManagerType;
  }

  @Override
  public String getKeyStoreType() {
    return keyStoreType;
  }

  @Override
  public String getKeyStorePath() {
    return keyStorePath;
  }

  @Override
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  @Override
  public String getTrustManagerType() {
    return trustManagerType;
  }

  @Override
  public String getTrustStoreType() {
    return trustStoreType;
  }

  @Override
  public String getTrustStorePath() {
    return trustStorePath;
  }

  @Override
  public String getTrustStorePassword() {
    return trustStorePassword;
  }

}
