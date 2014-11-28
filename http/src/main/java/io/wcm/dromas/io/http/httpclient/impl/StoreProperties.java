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
package io.wcm.dromas.io.http.httpclient.impl;

/**
 * Property bean for a key or trust store.
 */
class StoreProperties {

  private final String password;
  private final String managerType;
  private final String type;

  public StoreProperties(String password, String managerType, String type) {
    this.password = password;
    this.managerType = managerType;
    this.type = type;
  }

  /**
   * @return Returns the password.
   */
  public String getPassword() {
    return password;
  }

  /**
   * @return Returns the managerType.
   */
  public String getManagerType() {
    return managerType;
  }

  /**
   * @return Returns the type.
   */
  public String getType() {
    return type;
  }

}
