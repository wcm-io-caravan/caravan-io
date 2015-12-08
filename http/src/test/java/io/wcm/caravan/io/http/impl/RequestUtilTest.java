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
package io.wcm.caravan.io.http.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;

import java.io.IOException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.netflix.loadbalancer.Server;

public class RequestUtilTest {

  @Test
  public void testBuildUrlPrefix() {
    assertEquals("http://host", RequestUtil.buildUrlPrefix(new Server("host", 80), RequestUtil.PROTOCOL_AUTO));
    assertEquals("https://host", RequestUtil.buildUrlPrefix(new Server("host", 443), RequestUtil.PROTOCOL_AUTO));
    assertEquals("http://host:8000", RequestUtil.buildUrlPrefix(new Server("host", 8000), RequestUtil.PROTOCOL_AUTO));
    assertEquals("https://host:8443", RequestUtil.buildUrlPrefix(new Server("host", 8443), RequestUtil.PROTOCOL_AUTO));
  }

  @Test
  public void testBuildHttpRequest_Get() {
    CaravanHttpRequestBuilder template = new CaravanHttpRequestBuilder("test-service").method("get").append("http://host").append("/path")
        .header("header1", "value1")
        .header("header2", "value2")
        .header("header2", "value3");
    HttpUriRequest request = RequestUtil.buildHttpRequest(template.build());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpGet.METHOD_NAME, request.getMethod());

    Multimap<String, String> expected = ImmutableListMultimap.<String, String>builder().put("header1", "value1").putAll("header2", "value2", "value3").build();
    assertEquals(expected, RequestUtil.toHeadersMap(request.getAllHeaders()));
  }

  @Test
  public void testBuildHttpRequest_Post() throws ParseException, IOException {
    CaravanHttpRequestBuilder template = new CaravanHttpRequestBuilder("test-service").method("post").append("http://host").append("/path")
        .body("string body");
    HttpUriRequest request = RequestUtil.buildHttpRequest(template.build());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpPost.METHOD_NAME, request.getMethod());

    HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest)request;
    assertEquals("string body", EntityUtils.toString(entityRequest.getEntity()));
  }

  @Test
  public void testBuildHttpRequest_Put() throws IOException {
    byte[] data = new byte[] {
        0x01, 0x02, 0x03, 0x04, 0x05
    };
    CaravanHttpRequestBuilder template = new CaravanHttpRequestBuilder("test-service").method("put").append("http://host").append("/path")
        .body(data, null);
    HttpUriRequest request = RequestUtil.buildHttpRequest(template.build());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpPut.METHOD_NAME, request.getMethod());

    HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest)request;
    assertArrayEquals(data, EntityUtils.toByteArray(entityRequest.getEntity()));
  }

  @Test
  public void testBuildHttpRequest_Delete() {
    CaravanHttpRequestBuilder template = new CaravanHttpRequestBuilder("test-service").method("delete").append("http://host").append("/path");
    HttpUriRequest request = RequestUtil.buildHttpRequest(template.build());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpDelete.METHOD_NAME, request.getMethod());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildHttpRequest_Invalid() {
    RequestUtil.buildHttpRequest(new CaravanHttpRequestBuilder("test-service").method("invalid").append("http://host").append("/path").build());
  }

  @Test
  public void testToHeadersMap() {
    List<Header> headers = ImmutableList.<Header>of(
        new BasicHeader("header1", "value1"),
        new BasicHeader("header2", "value2"),
        new BasicHeader("header2", "value3"));

    Multimap<String, String> expected = ImmutableListMultimap.<String, String>builder().put("header1", "value1").putAll("header2", "value2", "value3").build();

    assertEquals(expected, RequestUtil.toHeadersMap(headers.toArray(new Header[headers.size()])));
  }
}
