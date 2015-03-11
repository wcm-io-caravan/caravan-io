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
package io.wcm.caravan.io.http.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CaravanHttpRequestBuilderTest {

  private CaravanHttpRequestBuilder builder;

  @Before
  public void setUp() {
    builder = new CaravanHttpRequestBuilder("test-service");
  }

  @Test
  public void test_append() {
    assertEquals("http://localhost:8080/service", builder.append("http://localhost:8080/").append("service").url());
    assertEquals("http://localhost:8080/service", builder.append("?x={x}&y=y").url());
    assertEquals(ImmutableListMultimap.of("x", "{x}", "y", "y"), builder.queries());
  }

  @Test
  public void test_body() {
    assertNull(builder.body());
    assertEquals("BODY", new String(builder.body("BODY").body()));
    assertEquals(Charsets.UTF_8, builder.charset());
    assertEquals("BODY2", new String(builder.body("BODY2".getBytes(), Charsets.UTF_8).body()));
    assertEquals(Charsets.UTF_8, builder.charset());
  }

  @Test
  public void test_bodyTemplate() {
    assertNull(builder.bodyTemplate("x={x}").body());
    assertEquals("x=1", new String(builder.resolve(ImmutableMap.of("x", "1")).body()));
  }

  @Test
  public void test_build() {
    CaravanHttpRequest request = builder.method("POST")
        .append("http://localhost:8080/service?test=1")
        .headers(ImmutableListMultimap.of("x", "y"))
        .body("BODY").build();
    assertEquals("POST", request.method());
    assertEquals("http://localhost:8080/service?test=1", request.url());
    assertEquals(ImmutableListMultimap.of("x", "y"), request.headers());
    assertEquals("BODY", new String(request.body()));
  }

  @Test
  public void test_headers() {
    assertEquals(ImmutableListMultimap.of("x", "y"),
        builder.headers(ImmutableListMultimap.of("x", "y")).headers());
  }

  @Test
  public void test_header() {
    assertEquals(ImmutableListMultimap.builder().putAll("x", "1", "2").build(),
        builder.header("x", Lists.newArrayList("1", "2")).headers());
  }

  @Test
  public void test_insert() throws Exception {
    assertEquals("http://localhost:8080", builder.append("httplocalhost:8080").insert(4, "://").url());
  }

  @Test
  public void test_method() {
    assertEquals("POST", builder.method("POST").method());
  }

  @Test
  public void test_queries() {
    assertEquals(ImmutableListMultimap.of("x", "1"),
        builder.queries(ImmutableListMultimap.of("x", "1")).queries());
    assertEquals(ImmutableListMultimap.of("x", "2"),
        builder.queries(ImmutableListMultimap.of("x", "2")).queries());
    assertTrue(builder.queries(null).queries().isEmpty());
  }

  @Test
  public void test_query() {
    assertEquals(ImmutableListMultimap.builder().putAll("x", "1", "2").build(),
        builder.query("x", Lists.newArrayList("1", "2")).queries());
    assertEquals(ImmutableListMultimap.builder().putAll("x", "3", "4").build(),
        builder.query("x", Lists.newArrayList("3", "4")).queries());
    assertTrue(builder.query("x", Lists.newArrayList()).queries().isEmpty());
  }

  @Test
  public void test_replaceQueryValues() {
    assertEquals(ImmutableListMultimap.of("x", "1"),
        builder.queries(ImmutableListMultimap.of("x", "{x}")).replaceQueryValues(ImmutableMap.of("x", "1")).queries());
  }

  @Test
  public void resolve_templateWithParameterizedPathSkipsEncodingSlash() {

    CaravanHttpRequestBuilder template = builder.append("{zoneId}");
    assertEquals("GET {zoneId} HTTP/1.1\n", template.toString());

    template.resolve(ImmutableMap.of("zoneId", "/hostedzone/Z1PA6795UKMFR9"));
    assertEquals("GET /hostedzone/Z1PA6795UKMFR9 HTTP/1.1\n", template.toString());

    template.insert(0, "https://route53.amazonaws.com/2012-12-12");

    assertEquals(template.build().toString(), ""//
        + "GET https://route53.amazonaws.com/2012-12-12/hostedzone/Z1PA6795UKMFR9 HTTP/1.1\n");
  }

  @Test
  public void resolve_templateWithBaseAndParameterizedQuery() {
    CaravanHttpRequestBuilder template = builder.method("GET")
        .append("/?Action=DescribeRegions").query("RegionName.1", "{region}");

    assertEquals(template.queries(),
        ImmutableListMultimap.of("Action", "DescribeRegions", "RegionName.1", "{region}"));
    assertEquals(template.toString(), ""//
        + "GET /?Action=DescribeRegions&RegionName.1={region} HTTP/1.1\n");

    template.resolve(ImmutableMap.of("region", "eu-west-1"));
    assertEquals(template.queries(),
        ImmutableListMultimap.of("Action", "DescribeRegions", "RegionName.1", "eu-west-1"));

    assertEquals(template.toString(), ""//
        + "GET /?Action=DescribeRegions&RegionName.1=eu-west-1 HTTP/1.1\n");

    template.insert(0, "https://iam.amazonaws.com");

    assertEquals(template.build().toString(), ""//
        + "GET https://iam.amazonaws.com/?Action=DescribeRegions&RegionName.1=eu-west-1 HTTP/1.1\n");
  }

  @Test
  public void resolveTemplateWithBaseAndParameterizedIterableQuery() {
    CaravanHttpRequestBuilder template = builder.method("GET")
        .append("/?Query=one").query("Queries", "{queries}");

    template.resolve(ImmutableMap.of("queries", Arrays.asList("us-east-1", "eu-west-1")));
    assertEquals(template.queries(),
        ImmutableListMultimap.<String, String>builder()
        .put("Query", "one")
        .putAll("Queries", "us-east-1", "eu-west-1")
        .build());

    assertEquals(template.toString(), "GET /?Query=one&Queries=us-east-1&Queries=eu-west-1 HTTP/1.1\n");
  }

  @Test
  public void resolveTemplateWithMixedRequestLineParams() throws Exception {
    CaravanHttpRequestBuilder template = builder.method("GET")//
        .append("/domains/{domainId}/records")//
        .query("name", "{name}")//
        .query("type", "{type}");

    template = template.resolve(ImmutableMap.<String, Object>builder()//
        .put("domainId", 1001)//
        .put("name", "denominator.io")//
        .put("type", "CNAME")//
        .build()
        );

    assertEquals(template.toString(), ""//
        + "GET /domains/1001/records?name=denominator.io&type=CNAME HTTP/1.1\n");

    template.insert(0, "https://dns.api.rackspacecloud.com/v1.0/1234");

    assertEquals(template.build().toString(), ""//
        + "GET https://dns.api.rackspacecloud.com/v1.0/1234"//
        + "/domains/1001/records?name=denominator.io&type=CNAME HTTP/1.1\n");
  }

  @Test
  public void insertHasQueryParams() throws Exception {
    CaravanHttpRequestBuilder template = builder.method("GET")//
        .append("/domains/{domainId}/records")//
        .query("name", "{name}")//
        .query("type", "{type}");

    template = template.resolve(ImmutableMap.<String, Object>builder()//
        .put("domainId", 1001)//
        .put("name", "denominator.io")//
        .put("type", "CNAME")//
        .build()
        );

    assertEquals(template.toString(), ""//
        + "GET /domains/1001/records?name=denominator.io&type=CNAME HTTP/1.1\n");

    template.insert(0, "https://host/v1.0/1234?provider=foo");

    assertEquals(template.build().toString(), ""//
        + "GET https://host/v1.0/1234/domains/1001/records?provider=foo&name=denominator.io&type=CNAME HTTP/1.1\n");
  }

  @Test
  public void resolveTemplateWithBodyTemplateSetsBodyAndContentLength() {
    CaravanHttpRequestBuilder template = builder.method("POST")
        .bodyTemplate("%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", " +
            "\"password\": \"{password}\"%7D");

    template = template.resolve(ImmutableMap.<String, Object>builder()//
        .put("customer_name", "netflix")//
        .put("user_name", "denominator")//
        .put("password", "password")//
        .build()
        );

    assertEquals("POST  HTTP/1.1\n\n{\"customer_name\": \"netflix\", \"user_name\": \"denominator\", \"password\": \"password\"}", template.toString());

    template.insert(0, "https://api2.dynect.net/REST");

    assertEquals(template.build().toString(), ""//
        + "POST https://api2.dynect.net/REST HTTP/1.1\n" //
        + "\n" //
        + "{\"customer_name\": \"netflix\", \"user_name\": \"denominator\", \"password\": \"password\"}");
  }

  @Test
  public void skipUnresolvedQueries() throws Exception {
    CaravanHttpRequestBuilder template = builder.method("GET")//
        .append("/domains/{domainId}/records")//
        .query("optional", "{optional}")//
        .query("name", "{nameVariable}");

    template = template.resolve(ImmutableMap.<String, Object>builder()//
        .put("domainId", 1001)//
        .put("nameVariable", "denominator.io")//
        .build()
        );

    assertEquals(template.toString(), ""//
        + "GET /domains/1001/records?name=denominator.io HTTP/1.1\n");
  }

  @Test
  public void allQueriesUnresolvable() throws Exception {
    CaravanHttpRequestBuilder template = builder.method("GET")//
        .append("/domains/{domainId}/records")//
        .query("optional", "{optional}")//
        .query("optional2", "{optional2}");

    template = template.resolve(ImmutableMap.<String, Object>builder()//
        .put("domainId", 1001)//
        .build()
        );

    assertEquals(template.toString(), ""//
        + "GET /domains/1001/records HTTP/1.1\n");
  }

  @Test
  public void test_resolveHeaders() {
    assertEquals(ImmutableListMultimap.of("x", "x1", "y", "y1"),
        builder.headers(ImmutableListMultimap.of("x", "{x}", "y", "y1", "z", "{z}")).resolve(ImmutableMap.of("x", "x1")).headers());
  }

  @Test
  public void test_resolvePartial() {
    builder.append("/{path1}{/path2}")
    .bodyTemplate("{body1}\n{body2}")
    .header("header-key", "{header1}", "{header2}")
    .resolve(ImmutableMap.of("path1", "p1", "body1", "b1", "header1", "h1"));
    assertEquals("GET /p1 HTTP/1.1\nheader-key: h1\n\nb1\n", builder.toString());
  }
}
