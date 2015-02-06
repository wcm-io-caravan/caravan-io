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

import org.osgi.framework.BundleContext;

/**
 * Initializes Netflix Archaius configuration context.
 */
public class BundleActivator implements org.osgi.framework.BundleActivator {

  @Override
  public void start(BundleContext context) throws Exception {
    ArchaiusConfig.initialize();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    // nothing to do
  }

}
