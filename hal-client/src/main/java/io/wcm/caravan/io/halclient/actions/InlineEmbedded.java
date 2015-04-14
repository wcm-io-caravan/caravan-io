/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.io.halclient.actions;

import io.wcm.caravan.commons.hal.domain.HalResource;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Moves the state of embedded resources into the state of the current resource.
 * The embedded resources get and the self link of the current resource get deleted.
 */
public class InlineEmbedded implements JsonPipelineAction {

  private final String[] relations;

  /**
   * @param relations
   */
  public InlineEmbedded(String... relations) {
    this.relations = relations;
  }

  @Override
  public String getId() {
    return "INLINE-EMBEDDED(" + StringUtils.join(relations, '-') + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {
    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    for (String relation : relations) {
      moveEmbeddedResourceState(halResource, relation);
      // delete embedded resource
      halResource.removeEmbedded(relation);
    }
    // delete self-HREF for resource
    halResource.removeLinks("self");

    return Observable.just(previousStepOutput);
  }

  private void moveEmbeddedResourceState(HalResource halResource, String relation) {
    List<HalResource> embeddedResources = halResource.getEmbedded(relation);
    ObjectNode model = halResource.getModel();
    if (embeddedResources.size() == 1) {
      model.set(relation, embeddedResources.get(0).removeEmbedded().removeLinks().getModel());
    }
    else {
      ArrayNode container = model.putArray(relation);
      Streams.of(embeddedResources).forEach(e -> container.add(e.removeEmbedded().removeLinks().getModel()));
    }
  }

}
