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


import java.util.List;

/**
 * A RecordValueCollector to collect string values for a field.
 * <p>
 * @author Spencer Koehler
 */
public class StringValueCollector implements RecordValueCollector<String> {
  
  private String fieldName;

  public StringValueCollector(String fieldName) {
    this.fieldName = fieldName;
  }

  public boolean collect(List<String> collector, DataRecord dataRecord) {
    boolean result = false;

    if (dataRecord != null) {
      final String value = dataRecord.getFieldValue(fieldName);
      if (value != null) {
        result = true;
        if (collector != null) {
          collector.add(value);
        }
      }
    }

    return result;
  }
}