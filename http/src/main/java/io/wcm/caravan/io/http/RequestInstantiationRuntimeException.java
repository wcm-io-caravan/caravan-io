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
package io.wcm.caravan.io.http;

/**
 * Thrown if there was an error by creating a request.
 */
public class RequestInstantiationRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 5633707101601214674L;

  /**
   * @param message Error message
   * @param cause Cause (may be null)
   */
  public RequestInstantiationRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

}
