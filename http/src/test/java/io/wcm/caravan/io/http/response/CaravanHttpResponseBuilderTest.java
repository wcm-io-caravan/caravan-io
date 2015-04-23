//$URL: $
//$Id: $
package io.wcm.caravan.io.http.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import joptsimple.internal.Strings;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;


public class CaravanHttpResponseBuilderTest {

  private CaravanHttpResponseBuilder builder;

  @Before
  public void setUp() {
    builder = new CaravanHttpResponseBuilder();
  }

  @Test
  public void testBody_ByteArray() throws IOException {
    CaravanHttpResponse response = builder
        .status(200)
        .reason("OK")
        .body("BODY".getBytes(Charsets.UTF_8))
        .build();
    Body body = response.body();
    assertTrue(body.isRepeatable());
    assertEquals("BODY", body.asString());
  }

  @Test
  public void testBody_InputStream() throws IOException {
    byte[] content = "BODY".getBytes(Charsets.UTF_8);
    CaravanHttpResponse response = builder
        .status(200)
        .reason("OK")
        .body(new ByteArrayInputStream(content), content.length)
        .build();
    Body body = response.body();
    assertFalse(body.isRepeatable());
    assertEquals("BODY", body.asString());
    assertEquals(new Integer(content.length), body.length());
    assertEquals(Strings.EMPTY, IOUtils.toString(body.asInputStream()));
    assertEquals(Strings.EMPTY, IOUtils.toString(body.asReader()));
  }

  @Test
  public void testBody_String() throws IOException {
    CaravanHttpResponse response = builder
        .status(200)
        .reason("OK")
        .body("BODY", Charsets.UTF_8)
        .build();
    Body body = response.body();
    assertTrue(body.isRepeatable());
    assertEquals("BODY", body.asString());
  }

}
