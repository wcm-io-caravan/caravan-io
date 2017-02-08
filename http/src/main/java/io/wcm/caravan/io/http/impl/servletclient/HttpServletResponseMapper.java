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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;

/**
 * Mapper from {@link CaravanHttpResponse} to {@link HttpServletResponse}.
 */
public class HttpServletResponseMapper implements HttpServletResponse {

  private String characterEncoding = Charsets.UTF_8.toString();
  private String contentType;
  private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private int bufferSize = 4096;
  private Locale locale;
  private final List<Cookie> cookies = Lists.newArrayList();
  private final Multimap<String, String> headers = HashMultimap.create();
  private int status = HttpServletResponse.SC_OK;
  private String reason = "OK";

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {

    return new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        outputStream.write(b);
      }
    };
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return new PrintWriter(outputStream);
  }

  @Override
  public void setCharacterEncoding(String charset) {
    characterEncoding = charset;
  }

  @Override
  public void setContentLength(int len) {
    setIntHeader("Content-Length", len);

    // if the content length is known in advance then resize the ByteArrayOutputSteram accordingly
    // (but only if nothing has yet been written to the output stream)
    if (len > 0 && outputStream.size() == 0) {
      outputStream = new ByteArrayOutputStream(len);
    }
  }

  @Override
  public void setContentType(String type) {
    contentType = type;
  }

  @Override
  public void setBufferSize(int size) {
    bufferSize = size;
  }

  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  @Override
  public void flushBuffer() throws IOException {
    outputStream.flush();
  }

  @Override
  public void resetBuffer() {
    outputStream.reset();
  }

  @Override
  public boolean isCommitted() {
    return false;
  }

  @Override
  public void reset() {
    resetBuffer();
    this.characterEncoding = null;
    this.contentType = null;
    this.locale = null;
    this.cookies.clear();
    this.headers.clear();
    this.status = HttpServletResponse.SC_OK;
    this.reason = null;
  }

  @Override
  public void setLocale(Locale loc) {
    locale = loc;
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public void addCookie(Cookie cookie) {
    cookies.add(cookie);
  }

  @Override
  public boolean containsHeader(String name) {
    return headers.containsKey(name);
  }

  @Override
  public String encodeURL(String url) {
    return url;
  }

  @Override
  public String encodeRedirectURL(String url) {
    return encodeURL(url);
  }

  @Override
  public String encodeUrl(String url) {
    return encodeURL(url);
  }

  @Override
  public String encodeRedirectUrl(String url) {
    return encodeRedirectURL(url);
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    status = sc;
    reason = msg;
  }

  @Override
  public void sendError(int sc) throws IOException {
    status = sc;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    setHeader("Location", location);
    setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }

  @Override
  public void setDateHeader(String name, long date) {
    String value = formatDate(date);
    setHeader(name, value);
  }

  private String formatDate(long date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat.format(new Date(date));
  }

  @Override
  public void addDateHeader(String name, long date) {
    String value = formatDate(date);
    addHeader(name, value);
  }

  @Override
  public void setHeader(String name, String value) {
    headers.replaceValues(name, Collections.singletonList(value));
  }

  @Override
  public void addHeader(String name, String value) {
    headers.put(name, value);
  }

  @Override
  public void setIntHeader(String name, int value) {
    setHeader(name, String.valueOf(value));
  }

  @Override
  public void addIntHeader(String name, int value) {
    addHeader(name, String.valueOf(value));
  }

  @Override
  public void setStatus(int sc) {
    status = sc;
  }

  @Override
  public void setStatus(int sc, String sm) {
    setStatus(sc);
    reason = sm;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public String getHeader(String name) {
    Collection<String> values = headers.get(name);
    return values.isEmpty() ? null : values.iterator().next();
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return headers.get(name);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  public CaravanHttpResponse getResponse() {
    return new CaravanHttpResponseBuilder()
    .body(outputStream.toByteArray())
    .headers(headers)
    .reason(reason)
    .status(status)
    .build();
  }

}
