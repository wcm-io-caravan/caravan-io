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

import io.wcm.caravan.commons.hal.domain.HalResource;
import io.wcm.caravan.commons.hal.domain.Link;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.util.JsonPipelineOutputUtil;

import java.util.List;
import java.util.Map;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Action to load a HAL link and insert the content as embedded resource.
 */
public class EmbedLinks implements JsonPipelineAction {

  private final String serviceName;
  private final String relation;
  private final Map<String, Object> parameters;
  private final int index;

  private CacheStrategy cacheStrategy;


  /**
   * @param serviceName
   * @param relation
   * @param parameters
   * @param index
   */
  public EmbedLinks(String serviceName, String relation, Map<String, Object> parameters, int index) {
    this.serviceName = serviceName;
    this.relation = relation;
    this.parameters = parameters;
    this.index = index;
  }

  /**
   * Sets the cache strategy for this action.
   * @param newCacheStrategy Caching strategy
   * @return Embed links action
   */
  public EmbedLinks setCacheStrategy(CacheStrategy newCacheStrategy) {
    this.cacheStrategy = newCacheStrategy;
    return this;
  }

  @Override
  public String getId() {
    return "EMBED-LINKS(" + relation + '-' + parameters.hashCode() + (index == Integer.MIN_VALUE ? "" : ('-' + index)) + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {

    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());

    List<Observable<JsonPipelineOutput>> observablesToEmbed = getPipelineOutputs(previousStepOutput, context);

    // create one new zipping Observable that will call the given lambda when all resources to embed are available
    return Observable.zip(observablesToEmbed, pipelineOutputsToEmbed -> {

      // unfortunately, FuncN can only give us an Object[], so we have to manually cast it
      for (Object o : pipelineOutputsToEmbed) {
        JsonPipelineOutput output = (JsonPipelineOutput)o;

        HalResource resourceToEmbed = new HalResource((ObjectNode)output.getPayload());
        halResource.addEmbedded(relation, resourceToEmbed);
      }

      // the links can be removed after the resources have been embeded
      removeLinks(halResource);

      // replace the pipeline output with the new HalResource containing the embedded resources
      JsonPipelineOutput[] casted = new JsonPipelineOutput[pipelineOutputsToEmbed.length];
      System.arraycopy(pipelineOutputsToEmbed, 0, casted, 0, pipelineOutputsToEmbed.length);
      return JsonPipelineOutputUtil.enrichWithLowestAge(previousStepOutput, casted);
    });
  }

  private List<Observable<JsonPipelineOutput>> getPipelineOutputs(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {
    List<CaravanHttpRequest> requests = getRequests(previousStepOutput);
    return Streams.of(requests)
        // create pipeline
        .map(request -> context.getFactory().create(request, context.getProperties()))
        // add Caching
        .map(pipeline -> cacheStrategy == null ? pipeline : pipeline.addCachePoint(cacheStrategy))
        // get output
        .map(pipeline -> pipeline.getOutput())
        .collect(Collectors.toList());
  }

  private List<CaravanHttpRequest> getRequests(JsonPipelineOutput previousStepOutput) {

    CaravanHttpRequest previousRequest = previousStepOutput.getRequests().get(0);

    List<Link> links = getLinks(previousStepOutput);
    return Streams.of(links)
        // create request, and main cache-control headers from previous request
        .map(link -> {

          return new CaravanHttpRequestBuilder(serviceName)
            .append(link.getHref())
            .header("Cache-Control",previousRequest.headers().get("Cache-Control"))
            .build(parameters);
        })
        .collect(Collectors.toList());
  }

  private List<Link> getLinks(JsonPipelineOutput previousStepOutput) {
    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    List<Link> links = halResource.getLinks(relation);
    return index == Integer.MIN_VALUE ? links : Lists.newArrayList(links.get(index));
  }

  private void removeLinks(HalResource halResource) {
    if (index == Integer.MIN_VALUE) {
      halResource.removeLinks(relation);
    }
    else {
      halResource.removeLink(relation, index);
    }
  }

}
