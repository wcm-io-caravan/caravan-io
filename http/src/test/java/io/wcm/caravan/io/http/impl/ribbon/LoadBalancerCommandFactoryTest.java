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
package io.wcm.caravan.io.http.impl.ribbon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalancerCommandFactoryTest {

  private static final String SERVICE_ID = "test-service";

  @Mock
  private LoadBalancerFactory loadBalancerFactory;
  @Mock
  private ILoadBalancer loadBalancer;

  @InjectMocks
  private LoadBalancerCommandFactory factory;

  @Before
  public void setUp() {
    Mockito.when(loadBalancerFactory.getLoadBalancer(SERVICE_ID)).thenReturn(loadBalancer);
  }

  @Test
  public void isLocalRequest_shouldReturnTrueIfAllServersHaveLocalHost() {
    List<Server> servers = ImmutableList.of(new Server("localhost:8080"), new Server("localhost:8081"));
    Mockito.when(loadBalancer.getServerList(true)).thenReturn(servers);
    assertTrue(factory.isLocalRequest(SERVICE_ID));
  }

  @Test
  public void isLocalRequest_shouldReturnFalseForOneServerHavingNoLocalHost() {
    List<Server> servers = ImmutableList.of(new Server("github.com"), new Server("localhost:8081"));
    Mockito.when(loadBalancer.getServerList(true)).thenReturn(servers);
    assertFalse(factory.isLocalRequest(SERVICE_ID));
  }

}
