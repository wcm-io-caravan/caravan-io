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
package io.wcm.caravan.io.http.impl.servletclient;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import rx.Observable;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;

/**
 * Mapper from {@link CaravanHttpRequest} to {@link HttpServletRequest}.
 */
public class HttpServletRequestMapper implements HttpServletRequest {

  private final CaravanHttpRequest request;
  private final URI uri;
  private final String serviceId;
  private final Map<String, Object> attributes = Maps.newHashMap();

  /**
   * @param request
   */
  public HttpServletRequestMapper(CaravanHttpRequest request) throws NotSupportedByRequestMapperException {
    this.request = request;
    try {
      uri = new URI(request.getUrl());
      serviceId = URLDecoder.decode(request.getServiceId(), Charsets.UTF_8.toString());
    }
    catch (URISyntaxException | UnsupportedEncodingException ex) {
      throw new NotSupportedByRequestMapperException();
    }
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  @Override
  public String getCharacterEncoding() {
    return request.getCharset().toString();
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    // do nothing
  }

  @Override
  public int getContentLength() {
    return request.getBody().length;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new ServletInputStream() {

      private int index;

      @Override
      public int read() throws IOException {
        return index < request.getBody().length ? -1 : request.getBody()[index++];
      }

    };

  }

  @Override
  public String getParameter(String name) {
    String[] values = getParameterValues(name);
    return ArrayUtils.isEmpty(values) ? null : values[0];
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(getParameterMap().keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return getParameterMap().get(name);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    try {
      List<NameValuePair> pairs = URLEncodedUtils.parse(new URI(request.getUrl()), Charsets.UTF_8.toString());
      Map<String, Collection<String>> multiMap = Observable.from(pairs).toMultimap(NameValuePair::getName, NameValuePair::getValue).toBlocking().single();
      Builder<String, String[]> builder = ImmutableMap.builder();
      multiMap.entrySet().stream().forEach(entry -> {
        String[] arrayValue = entry.getValue().toArray(new String[entry.getValue().size()]);
        builder.put(entry.getKey(), arrayValue);
      });
      return builder.build();
    }
    catch (URISyntaxException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  @Override
  public String getProtocol() {
    return "HTTP/1.1";
  }

  @Override
  public String getScheme() {
    return "http";
  }

  @Override
  public String getServerName() {
    return "localhost";
  }

  @Override
  public int getServerPort() {
    return 8080;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request.getBody())));
  }

  @Override
  public String getRemoteAddr() {
    return "localhost";
  }

  @Override
  public String getRemoteHost() {
    return "localhost";
  }

  @Override
  public void setAttribute(String name, Object o) {
    attributes.put(name, o);
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public Locale getLocale() {
    return Locale.getDefault();
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return Collections.enumeration(Collections.singletonList(getLocale()));
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return getServletContext().getRequestDispatcher(path);
  }

  @Override
  public String getRealPath(String path) {
    throw new NotSupportedByRequestMapperException();
  }

  @Override
  public int getRemotePort() {
    return 0;
  }

  @Override
  public String getLocalName() {
    return "localhost";
  }

  @Override
  public String getLocalAddr() {
    return "localhost";
  }

  @Override
  public int getLocalPort() {
    return 8080;
  }

  @Override
  public ServletContext getServletContext() {
    throw new NotSupportedByRequestMapperException();
  }

  @Override
  public AsyncContext startAsync() {
    return startAsync(this, null);
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
    throw new NotSupportedByRequestMapperException();

  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new NotSupportedByRequestMapperException();
  }

  @Override
  public DispatcherType getDispatcherType() {
    return DispatcherType.REQUEST;
  }

  @Override
  public String getAuthType() {
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    return new Cookie[0];
  }

  @Override
  public long getDateHeader(String name) {
    String value = getHeader(name);
    return value == null ? -1 : Long.parseLong(value);
  }

  @Override
  public String getHeader(String name) {
    Collection<String> values = request.getHeaders().get(name);
    return values.isEmpty() ? null : values.iterator().next();
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    return Collections.enumeration(request.getHeaders().get(name));
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(request.getHeaders().keySet());
  }

  @Override
  public int getIntHeader(String name) {
    String value = getHeader(name);
    return value == null ? -1 : Integer.parseInt(value);
  }

  @Override
  public String getMethod() {
    return request.getMethod();
  }

  @Override
  public String getPathInfo() {
    return uri.getPath().substring(serviceId.length());
  }

  @Override
  public String getPathTranslated() {
    return null;
  }

  @Override
  public String getContextPath() {
    return "";
  }

  @Override
  public String getQueryString() {
    return uri.getRawQuery();
  }

  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    return null;
  }

  @Override
  public String getRequestURI() {
    return uri.getRawPath();
  }

  @Override
  public StringBuffer getRequestURL() {
    return new StringBuffer("http://localhost:8080").append(getRequestURI());
  }

  @Override
  public String getServletPath() {
    return serviceId;
  }

  @Override
  public HttpSession getSession(boolean create) {
    throw new NotSupportedByRequestMapperException();
  }

  @Override
  public HttpSession getSession() {
    return getSession(true);
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return true;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    throw new NotSupportedByRequestMapperException();
  }

  @Override
  public void login(String username, String password) throws ServletException {
    throw new NotSupportedByRequestMapperException();
  }

  @Override
  public void logout() throws ServletException {
    // nothing to do
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return Collections.emptyList();
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return null;
  }

}
