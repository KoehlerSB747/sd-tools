/*
   Copyright 2008-2015 Semantic Discovery, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.sd.csv;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple DataRecord implementation.
 * <p>
 * @author Spencer Koehler
 */
public class SimpleDataRecord implements DataRecord {

  private Map<String, String> data;

  public SimpleDataRecord() {
    this.data = new LinkedHashMap<String, String>();
  }

  public void setFieldValue(String fieldName, String value) {
    this.data.put(fieldName, value);
  }

  public String getFieldValue(String fieldName) {
    return data.get(fieldName);
  }

  public String getFieldValue(String fieldName, String defaultValue) {
    final String result = data.get(fieldName);
    return (result == null) ? defaultValue : result;
  }
}
