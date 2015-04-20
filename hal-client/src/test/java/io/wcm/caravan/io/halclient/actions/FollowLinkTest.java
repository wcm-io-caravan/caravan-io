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
package io.wcm.caravan.io.halclient.actions;

import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FollowLinkTest {

  private FollowLink action;

  @Mock
  private JsonPipelineOutput previousStepOutput;
  @Mock
  private JsonPipelineContext context;

  @Before
  public void setUp() {
    action = new FollowLink("test-service", "item", Collections.emptyMap(), 0);
  }

  @Test(expected = IllegalStateException.class)
  public void text_execute_linkMissing() {
    Mockito.when(previousStepOutput.getPayload()).thenReturn(HalResourceFactory.createResource("/").getModel());
    action.execute(previousStepOutput, context);
  }

}
