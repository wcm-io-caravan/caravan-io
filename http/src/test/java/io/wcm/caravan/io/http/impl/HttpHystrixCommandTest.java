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

import static org.junit.Assert.assertEquals;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;


public class HttpHystrixCommandTest {

  private static final String SERVICE_NAME = "testHystrixService";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Before
  public void setUp() {
    ArchaiusConfig.initialize();
  }

  @Test
  public void test_defaultThreadPool() {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_NAME_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, "localhost")
        .build());
    HttpHystrixCommand underTest = new HttpHystrixCommand(SERVICE_NAME, ExecutionIsolationStrategy.THREAD, null, null);
    assertEquals("transportLayer", underTest.getThreadPoolKey().name());
  }

  @Test
  public void test_customThreadPool() {
    context.registerInjectActivateService(new CaravanHttpServiceConfig(), ImmutableMap.<String, Object>builder()
        .put(CaravanHttpServiceConfig.SERVICE_NAME_PROPERTY, SERVICE_NAME)
        .put(CaravanHttpServiceConfig.RIBBON_HOSTS_PROPERTY, "localhost")
        .put(CaravanHttpServiceConfig.HYSTRIX_EXECUTIONISOLATIONTHREADPOOLKEY_OVERRIDE_PROPERTY, "testThreadPool")
        .build());
    HttpHystrixCommand underTest = new HttpHystrixCommand(SERVICE_NAME, ExecutionIsolationStrategy.THREAD, null, null);
    assertEquals("testThreadPool", underTest.getThreadPoolKey().name());
  }

}
