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

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Subscriber;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class EmbedLinksTest {

  private EmbedLinks action;

  @Mock
  private JsonPipelineOutput previousStepOutput;
  @Mock
  private JsonPipelineContext context;
  @Mock
  private JsonPipelineFactory factory;

  private List<String> requests;

  @Before
  public void setUp() {
    action = new EmbedLinks("test-service", "item", Collections.emptyMap(), Integer.MIN_VALUE);
    requests = Lists.newArrayList();

    HalResource hal = HalResourceFactory.createResource("/href")
        .addLinks("item", ImmutableList.of(
            HalResourceFactory.createLink("/item-1"),
            HalResourceFactory.createLink("/item-2")));
    Mockito.when(previousStepOutput.getPayload()).thenReturn(hal.getModel());

    Mockito.when(previousStepOutput.getRequests()).thenReturn(ImmutableList.of(
        new CaravanHttpRequestBuilder().build()));

    Mockito.when(context.getFactory()).thenReturn(factory);

    Mockito.when(factory.create(Matchers.any(CaravanHttpRequest.class), Matchers.anyMapOf(String.class, String.class))).then(new Answer<JsonPipeline>() {

      @Override
      public JsonPipeline answer(InvocationOnMock invocation) throws Throwable {
        JsonPipeline pipeline = Mockito.mock(JsonPipeline.class);
        Mockito.when(pipeline.getOutput()).thenReturn(Observable.create((Subscriber<? super JsonPipelineOutput> s) -> {
          CaravanHttpRequest request = invocation.getArgumentAt(0, CaravanHttpRequest.class);
          if ("/item-1".equals(request.url())) {
            try {
              Thread.sleep(10);
            }
            catch (InterruptedException ex) {
              ex.printStackTrace();
            }
          }
          requests.add(request.url());
          JsonPipelineOutput output = Mockito.mock(JsonPipelineOutput.class);
          HalResource itemHal = HalResourceFactory.createResource(request.url());
          Mockito.when(output.getPayload()).thenReturn(itemHal.getModel());
          s.onNext(output);
          s.onCompleted();
        }));
        return pipeline;
      }
    });
  }

  @Test
  public void test_execute() {
    action.execute(previousStepOutput, context).toBlocking().single();
    assertEquals(ImmutableList.of("/item-1", "/item-2"), requests);
  }

}
