/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.io.hal;

import io.wcm.caravan.io.hal.domain.HalResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a HAL {@link HalResource} from an input source.
 */
public interface HalResourceReader {

  /**
   * @param input The input source
   * @return The HAL resource
   * @throws IOException Error reading JSON
   */
  HalResource read(final InputStream input) throws IOException;

}
