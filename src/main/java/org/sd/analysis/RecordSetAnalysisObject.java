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


import java.util.ArrayList;
import java.util.List;
import org.sd.csv.DataRecord;
import org.sd.csv.FieldWidths;
import org.sd.csv.RecordSet;
import org.sd.csv.RecordSetUtils;
import org.sd.util.range.IntegerRange;

/**
 * An analysis object that holds a record set.
 * <p>
 * @author Spencer Koehler
 */
public class RecordSetAnalysisObject extends AbstractAnalysisObject {
  
  private String name;
  private RecordSet recordSet;

  public RecordSetAnalysisObject(String name, RecordSet recordSet) {
    super();
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

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"size\" -- number of records in set\n").
      append("\"fields\" -- vector of field names in set\n").
      append("\"display[range]\" -- formatted display string of lines in the range\n").
      append("field -- vector of field's values");
      
    return result.toString();
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"size" -- number of records in set</li>
   * <li>"fields" -- vector of field names in set</li>
   * <li>"display[range]" -- formatted display of lines in the range</li>
   * <li>field -- vector of field's values</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;

    if ("size".equals(ref)) {
      result = new NumericAnalysisObject(recordSet == null ? 0 : recordSet.size());
    }
    else if ("fields".equals(ref)) {
      if (recordSet != null) {
        result = new VectorAnalysisObject<String>(name, recordSet.getFieldNames());
      }
    }
    else if (ref.startsWith("display")) {
      if (recordSet != null) {
        final AnalysisObject[] args = getArgValues(ref, env);
        IntegerRange range = null;
        if (args != null && args.length > 0) {
          final List<String> values = new ArrayList<String>();
          for (AnalysisObject arg : args) {
            values.add(arg.toString());
          }
          range = new IntegerRange(values);
        }
        final String formattedData = getFormattedData(recordSet, range);
        result = new BasicAnalysisObject<String>(formattedData);
      }
    }
    else if (recordSet != null) {
      // ref is name of field whose values to get
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

  public final String getFormattedData(IntegerRange range) {
    String result = null;

    if (recordSet != null) {
      result = getFormattedData(recordSet, range);
    }

    return (result == null) ? "" : result;
  }

  private final String getFormattedData(RecordSet recordSet, IntegerRange range) {
    final StringBuilder result = new StringBuilder();

    final FieldWidths fieldWidths = recordSet.getFieldWidths();

    final List<String> fieldNames = recordSet.getFieldNames();
    final String headerLine = fieldWidths.buildFormattedHeaderLine(fieldNames);
    result.append(headerLine);

    final List<DataRecord> dataRecords = RecordSetUtils.collectDataRecords(recordSet, range);
    for (DataRecord dataRecord : dataRecords) {
      final String dataLine = fieldWidths.buildFormattedDataLine(dataRecord, fieldNames);
      result.append('\n');
      result.append(dataLine);
    }

    return result.toString();
  }
}
