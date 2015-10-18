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
package org.sd.analysis;


import java.util.List;
import org.sd.csv.RecordSet;

/**
 * An analysis object that holds a record set.
 * <p>
 * @author Spencer Koehler
 */
public class RecordSetAnalysisObject implements AnalysisObject {
  
  private String name;
  private RecordSet recordSet;

  public RecordSetAnalysisObject(String name, RecordSet recordSet) {
    this.name = name;
    this.recordSet = recordSet;
  }

  public String getName() {
    return name;
  }

  public RecordSet getRecordSet() {
    return recordSet;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("#RecordSet.").
      append(name == null ? "" : name).
      append(recordSet == null ? "[]" : recordSet.getFieldNames().toString());
    return result.toString();
  }

  /** Get a detailed string representation of this object's data. */
  @Override
  public String getDetailedString() {
    return toString();
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"size" -- number of records in set</li>
   * <li>"fields" -- vector of field names in set</li>
   * <li>field -- vector of field's values</li>
   * </ul>
   */
  @Override
  public AnalysisObject access(String ref) {
    AnalysisObject result = null;

    if ("size".equals(ref)) {
      result = new NumericAnalysisObject(recordSet == null ? 0 : recordSet.size());
    }
    else if ("fields".equals(ref)) {
      if (recordSet != null) {
        result = new VectorAnalysisObject<String>(name, recordSet.getFieldNames());
      }
    }
    else if (recordSet != null) {
      final List<String> fieldValues = recordSet.getFieldValues(ref);
      if (fieldValues != null) {
        result = new VectorAnalysisObject<String>(ref, fieldValues);
      }
    }

    return result;
  }

  /** Get a numeric object representing this instance's value if applicable, or null. */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    return null;
  }
}
