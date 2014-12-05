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
import io.wcm.caravan.io.http.request.RequestTemplate;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import com.google.common.collect.ImmutableMap;
import com.netflix.loadbalancer.Server;

public class RequestUtilTest {

  @Test
  public void testBuildUrlPrefix() {
    assertEquals("http://host", RequestUtil.buildUrlPrefix(new Server("host", 80)));
    assertEquals("https://host", RequestUtil.buildUrlPrefix(new Server("host", 443)));
    assertEquals("http://host:8000", RequestUtil.buildUrlPrefix(new Server("host", 8000)));
    assertEquals("https://host:8443", RequestUtil.buildUrlPrefix(new Server("host", 8443)));
  }

  @Test
  public void testBuildHttpRequest_Get() {
    RequestTemplate template = new RequestTemplate().method("get").append("/path")
        .header("header1", "value1")
        .header("header2", "value2", "value3");
    HttpUriRequest request = RequestUtil.buildHttpRequest("http://host", template.request());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpGet.METHOD_NAME, request.getMethod());

    Map<String, Collection<String>> expected = ImmutableMap.<String, Collection<String>>of(
        "header1", ImmutableList.of("value1"),
        "header2", ImmutableList.of("value2", "value3")
        );
    assertEquals(expected, RequestUtil.toHeadersMap(request.getAllHeaders()));
  }

  @Test
  public void testBuildHttpRequest_Post() throws ParseException, IOException {
    RequestTemplate template = new RequestTemplate().method("post").append("/path")
        .body("string body");
    HttpUriRequest request = RequestUtil.buildHttpRequest("http://host", template.request());

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
    RequestTemplate template = new RequestTemplate().method("put").append("/path")
        .body(data, null);
    HttpUriRequest request = RequestUtil.buildHttpRequest("http://host", template.request());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpPut.METHOD_NAME, request.getMethod());

    HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest)request;
    assertArrayEquals(data, EntityUtils.toByteArray(entityRequest.getEntity()));
  }

  @Test
  public void testBuildHttpRequest_Delete() {
    RequestTemplate template = new RequestTemplate().method("delete").append("/path");
    HttpUriRequest request = RequestUtil.buildHttpRequest("http://host", template.request());

    assertEquals("http://host/path", request.getURI().toString());
    assertEquals(HttpDelete.METHOD_NAME, request.getMethod());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildHttpRequest_Invalid() {
    RequestUtil.buildHttpRequest("http://host", new RequestTemplate().method("invalid").append("/path").request());
  }

  @Test
  public void testToHeadersMap() {
    List<Header> headers = ImmutableList.<Header>of(
        new BasicHeader("header1", "value1"),
        new BasicHeader("header2", "value2"),
        new BasicHeader("header2", "value3"));

    Map<String, Collection<String>> expected = ImmutableMap.<String, Collection<String>>of(
        "header1", ImmutableList.of("value1"),
        "header2", ImmutableList.of("value2", "value3")
        );

    assertEquals(expected, RequestUtil.toHeadersMap(headers.toArray(new Header[headers.size()])));
  }

}
