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

import static io.wcm.caravan.io.http.request.CaravanHttpRequest.CORRELATION_ID_HEADER_NAME;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineActions;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Action to load a HAL link and replace the current resource by the loaded one.
 */
public class FollowLink implements JsonPipelineAction {

  private final String serviceName;
  private final String relation;
  private final Map<String, Object> parameters;
  private final int index;

  private CacheStrategy cacheStrategy;

  /**
   * @param relation
   * @param parameters
   * @param index
   */
  public FollowLink(String serviceName, String relation, Map<String, Object> parameters, int index) {
    this.serviceName = serviceName;
    this.relation = relation;
    this.parameters = parameters;
    this.index = index;
  }

  /**
   * @param newCacheStrategy Caching strategy
   * @return Follow link action
   */
  public FollowLink setCacheStrategy(CacheStrategy newCacheStrategy) {
    this.cacheStrategy = newCacheStrategy;
    return this;
  }

  @Override
  public String getId() {
    return "FOLLOW-LINK(" + relation + '-' + parameters.hashCode() + '-' + index + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {
    CaravanHttpRequest request = getRequest(previousStepOutput);
    JsonPipeline pipeline = createPipeline(context, request);
    return pipeline.applyAction(JsonPipelineActions.enrichWithLowestAge(previousStepOutput)).getOutput();
  }

  private CaravanHttpRequest getRequest(JsonPipelineOutput previousStepOutput) {
    String href = getHref(previousStepOutput);
    Collection<String> cacheControlHeader = getCacheControlHeader(previousStepOutput);
    // create follow-up request, and main cache-control headers from previous request
    CaravanHttpRequestBuilder builder = new CaravanHttpRequestBuilder(serviceName)
    .append(href)
    .header("Cache-Control", cacheControlHeader);

    // also make sure that the correlation-id is passed on to the follow-up requests
    if (previousStepOutput.getCorrelationId() != null) {
      builder.header(CORRELATION_ID_HEADER_NAME, previousStepOutput.getCorrelationId());
    }

    return builder.build(parameters);
  }

  private String getHref(JsonPipelineOutput previousStepOutput) {
    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    List<Link> links = halResource.getLinks(relation);
    if (links.size() <= index) {
      throw new IllegalStateException("HAL resource has no link with relation " + relation + " and index " + index);
    }
    return links.get(index).getHref();
  }

  private Collection<String> getCacheControlHeader(JsonPipelineOutput previousStepOutput) {
    CaravanHttpRequest previousRequest = previousStepOutput.getRequests().get(0);
    return previousRequest.headers().get("Cache-Control");
  }

  private JsonPipeline createPipeline(JsonPipelineContext context, CaravanHttpRequest request) {
    JsonPipeline pipeline = context.getFactory().create(request, context.getProperties());
    if (cacheStrategy != null) {
      pipeline = pipeline.addCachePoint(cacheStrategy);
    }
    return pipeline;
  }

}
