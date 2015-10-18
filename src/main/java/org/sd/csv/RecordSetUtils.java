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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods over record sets.
 * <p>
 * @author Spencer Koehler
 */
public class RecordSetUtils {
  
  public static <T> List<T> collectFieldValues(RecordSet recordSet, String fieldName, RecordValueCollector<T> valueCollector) {
    final List<T> result = new ArrayList<T>();

    for (Iterator<DataRecord> iter = recordSet.iterator(); iter.hasNext(); ) {
      final DataRecord dataRecord = iter.next();
      valueCollector.collect(result, dataRecord);
    }

    return result;
  }

  public static List<Double> collectNumbers(RecordSet recordSet, String fieldName) {
    return collectFieldValues(recordSet, fieldName, new NumericValueCollector(fieldName));
  }

  public static List<String> collectStrings(RecordSet recordSet, String fieldName) {
    return collectFieldValues(recordSet, fieldName, new StringValueCollector(fieldName));
  }
}
