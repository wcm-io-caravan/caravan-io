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
package io.wcm.caravan.io.halclient;

import io.wcm.caravan.io.halclient.actions.EmbedLinks;
import io.wcm.caravan.io.halclient.actions.FollowLink;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.Collections;
import java.util.Map;

/**
 * Factory for HAL specific {@link JsonPipelineAction}s.
 */
public class HalClientFactory {

  private final String serviceName;
  private final CacheStrategy cacheStrategy;

  /**
   * @param serviceName Service name
   * @param cacheStrategy Used cache strategy for all actions
   */
  public HalClientFactory(String serviceName, CacheStrategy cacheStrategy) {
    this.serviceName = serviceName;
    this.cacheStrategy = cacheStrategy;
  }

  /**
   * Creates a JSON pipeline for a service entry point.
   * @param factory Pipeline factory
   * @return JSON pipeline
   */
  public JsonPipeline create(JsonPipelineFactory factory) {
    return create(factory, "/");
  }

  /**
   * Creates a JSON pipeline for a service and URL.
   * @param factory Pipeline factory
   * @param url URL to start
   * @return JSON pipeline
   */
  public JsonPipeline create(JsonPipelineFactory factory, String url) {
    return create(factory, new CaravanHttpRequestBuilder(serviceName).append(url).build());
  }

  /**
   * Creates a JSON pipeline for a HTTP request.
   * @param factory Pipeline factory
   * @param request Pre-configured HTTP request
   * @return JSON pipeline
   */
  public JsonPipeline create(JsonPipelineFactory factory, CaravanHttpRequest request) {
    JsonPipeline entryPoint = factory.create(request);
    if (cacheStrategy != null) {
      entryPoint = entryPoint.addCachePoint(cacheStrategy);
    }
    return entryPoint;
  }

  /**
   * Creates a follow link action for the first relation specific link
   * @param relation Link relation
   * @return Follow link action
   */
  public FollowLink follow(String relation) {
    return follow(relation, Collections.emptyMap(), 0);
  }

  /**
   * Creates a follow link action for the first relation specific link with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @return Follow link action
   */
  public FollowLink follow(String relation, Map<String, Object> parameters) {
    return follow(relation, parameters, 0);
  }

  /**
   * Creates a follow link action for the {@code index} specified link.
   * @param relation Link relation
   * @param index Link index
   * @return Follow link action
   */
  public FollowLink follow(String relation, int index) {
    return follow(relation, Collections.emptyMap(), index);
  }

  /**
   * Creates a follow link action for the {@code index} specified link with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @param index Link index
   * @return Follow link action
   */
  public FollowLink follow(String relation, Map<String, Object> parameters, int index) {
    return new FollowLink(serviceName, relation, parameters, index).setCacheStrategy(cacheStrategy);
  }

  /**
   * Creates an embed links action for all relation specific links.
   * @param relation Link relation
   * @return Embed links action
   */
  public EmbedLinks embed(String relation) {
    return embed(relation, Collections.emptyMap(), Integer.MIN_VALUE);
  }

  /**
   * Creates an embed links action for all relation specific links with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @return Embed links action
   */
  public EmbedLinks embed(String relation, Map<String, Object> parameters) {
    return embed(relation, parameters, Integer.MIN_VALUE);
  }

  /**
   * Creates an embed links action for the {@code index} specified link.
   * @param relation Link relation
   * @param index Link index
   * @return Embed links action
   */
  public EmbedLinks embed(String relation, int index) {
    return embed(relation, Collections.emptyMap(), index);
  }

  /**
   * Creates an embed links action for the {@code index} specified link with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @param index Link index
   * @return Embed links action
   */
  public EmbedLinks embed(String relation, Map<String, Object> parameters, int index) {
    return new EmbedLinks(serviceName, relation, parameters, index).setCacheStrategy(cacheStrategy);
  }

}
