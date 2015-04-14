/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.io.halclient.actions;

import io.wcm.caravan.commons.hal.domain.HalResource;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineActions;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

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

    JsonPipeline pipeline = context.getFactory().create(request, context.getProperties());
    if (cacheStrategy != null) {
      pipeline = pipeline.addCachePoint(cacheStrategy);
    }
    return pipeline.applyAction(JsonPipelineActions.enrichWithLowestAge(previousStepOutput)).getOutput();
  }

  private CaravanHttpRequest getRequest(JsonPipelineOutput previousStepOutput) {
    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    String href = halResource.getLinks(relation).get(index).getHref();
    return new CaravanHttpRequestBuilder(serviceName).append(href).build(parameters);
  }

}
