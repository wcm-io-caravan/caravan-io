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

import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.netflix.loadbalancer.ILoadBalancer;

@RunWith(MockitoJUnitRunner.class)
public class CachingLoadBalancerFactoryTest {

  private static final String SERVICE_ID = "test-service";

  @Mock
  private LoadBalancerFactory delegate;
  @InjectMocks
  private CachingLoadBalancerFactory factory;

  @Before
  public void setUp() {
    Mockito.when(delegate.getLoadBalancer(SERVICE_ID)).thenReturn(Mockito.mock(ILoadBalancer.class));
  }

  @Test
  public void shouldCallDelegateAtFirstTime() {
    factory.getLoadBalancer(SERVICE_ID);
    Mockito.verify(delegate, times(1)).getLoadBalancer(SERVICE_ID);
  }

  @Test
  public void shouldCallDelegateOnlyOnce() {
    factory.getLoadBalancer(SERVICE_ID);
    factory.getLoadBalancer(SERVICE_ID);
    Mockito.verify(delegate, times(1)).getLoadBalancer(SERVICE_ID);
  }

  @Test
  public void shouldCallDelegateAgainAfterUnregister() {

    factory.getLoadBalancer(SERVICE_ID);
    factory.unregister(SERVICE_ID);
    factory.getLoadBalancer(SERVICE_ID);
    Mockito.verify(delegate, times(2)).getLoadBalancer(SERVICE_ID);

  }

}
