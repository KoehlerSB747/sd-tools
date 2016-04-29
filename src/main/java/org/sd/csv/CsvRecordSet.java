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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sd.io.FileUtil;

/**
 * Load a CsvRecordSet from a csv file where the first line is a header row for
 * the columns.
 *
 * @author Spencer Koehler
 */
public class CsvRecordSet implements RecordSet {
  
  private String name;
  private String fieldDelimiter;
  private String splitDelimiter;
  private Map<String, FieldMetaData> fieldMetaData;
  private List<String> fieldNames;
  private List<DataRecord> dataRecords;
  private Map<String, List<String>> fieldValues;

  public CsvRecordSet() {
    this(null);
  }

  public CsvRecordSet(String name) {
    this.name = name;
    this.fieldDelimiter = null;
    this.splitDelimiter = null;
    this.fieldMetaData = null;
    this.fieldNames = null;
    this.dataRecords = new ArrayList<DataRecord>();
    this.fieldValues = null;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int size() {
    return dataRecords.size();
  }

  @Override
  public Iterator<DataRecord> iterator() {
    return this.dataRecords.iterator();
  }

  @Override
  public Map<String, FieldMetaData> getFieldMetaData() {
    return fieldMetaData;
  }

  @Override
  public List<String> getFieldNames() {
    return fieldNames == null ? null : fieldNames;
  }

  @Override
  public FieldMetaData getFieldMetaData(String fieldName) {
    return fieldMetaData == null ? null : fieldMetaData.get(fieldName);
  }

  /**
   * Get the field values associated with the fieldName.
   * <p>
   * If the fieldName doesn't exist in this set, return null;
   * otherwise, return a non-null (possibly empty) result.
   */
  @Override
  public List<String> getFieldValues(String fieldName) {
    List<String> result = null;

    if (!fieldMetaData.containsKey(fieldName)) return null;

    if (fieldValues == null) {
      fieldValues = new HashMap<String, List<String>>();
    }
    else {
      result = fieldValues.get(fieldName);
    }

    if (result == null) {
      result = RecordSetUtils.collectStrings(this, fieldName);
      fieldValues.put(fieldName, result);
    }

    return result;
  }

  public void setFieldDelimiter(String fieldDelimiter) {
    this.fieldDelimiter = fieldDelimiter;
    this.splitDelimiter = "[\\s\"]*" + fieldDelimiter + "[\\s\"]*";
  }

  /** Get the delimiter for emitting a record. */
  public String getFieldDelimiter() {
    return fieldDelimiter == null ? "\\t" : fieldDelimiter;
  }

  /**
   * Load a CsvRecordSet from a csv file where the first line is a header row for
   * the columns and the fieldDelimiter is presumed to be this instance's value
   * if set or will be inferred if not.
   * <p>
   * Lines beginning with "#" will be ignored.
   */
  public CsvRecordSet load(File file) throws IOException {
    return load(file, this.fieldDelimiter);
  }

  /**
   * Load a CsvRecordSet from a csv file where the first line is a header row
   * for the columns and the given fieldDelimiter (which, if null this
   * instance's value will be used if set or the value will be inferred.
   * <p>
   * Lines beginning with "#" will be ignored.
   */
  public CsvRecordSet load(File file, String fieldDelimiter) throws IOException {
    if (this.fieldDelimiter == null && fieldDelimiter != null) {
      setFieldDelimiter(fieldDelimiter);
    }
    String line = null;
    final BufferedReader reader = FileUtil.getReader(file);
    try {
      while ((line = reader.readLine()) != null) {
        if ("".equals(line) || line.charAt(0) == '#') continue;
        if (this.fieldMetaData == null) {
          loadFieldMetaData(line);
        }
        else {
          loadField(line);
        }
      }
    }
    finally {
      reader.close();
    }

    return this;
  }

  public final CsvRecordSet loadFieldMetaData(String headerLine) {
    this.fieldMetaData = new HashMap<String, FieldMetaData>();
    this.fieldNames = new ArrayList<String>();
    if (fieldDelimiter == null) {
      setFieldDelimiter(guessFieldDelimiter(headerLine));
    }
    final String[] pieces = headerLine.split(splitDelimiter);
    for (String piece : pieces) {
      final FieldMetaData fieldMetaData = new FieldMetaData(piece);
      this.fieldMetaData.put(piece, fieldMetaData);
      this.fieldNames.add(piece);
    }
    return this;
  }

  public final CsvRecordSet loadField(String recordLine) {
    final SimpleDataRecord dataRecord = new SimpleDataRecord();

    if (fieldDelimiter == null) {
      setFieldDelimiter(guessFieldDelimiter(recordLine));
    }

    final String[] pieces = recordLine.split(splitDelimiter);

    for (int i = 0; i < pieces.length; ++i) {
      final String fieldName = getFieldName(i);
      final String fieldValue = pieces[i];
      dataRecord.setFieldValue(fieldName, fieldValue);
    }

    this.dataRecords.add(dataRecord);

    return this;
  }

  /**
   * Get the fieldName for the pos, creating (as pos+1) if necessary.
   */
  private final String getFieldName(int pos) {
    String result = null;

    if (fieldNames == null) fieldNames = new ArrayList<String>();
    if (fieldNames.size() <= pos) {
      for (int i = fieldNames.size(); i <= pos; ++i) {
        fieldNames.add(Integer.toString(i + 1));
        result = Integer.toString(pos + 1);
      }
    }
    else {
      result = fieldNames.get(pos);
    }

    return result;
  }

  /**
   * Protected for JUnit access.
   */
  protected String guessFieldDelimiter(String line) {
    String result = null;

    //
    // Algorithm:
    // - Until we find X such that line.indexOf(X) > 0, check:
    //   - tab
    //   - vertical bar
    //   - comma
    //
    // - Default to tab if nothing is found.
    //

    if (line.indexOf('\t') >= 0) {
      result = "\t";
    }
    else if (line.indexOf('|') >= 0) {
      result = "\\|";
    }
    else if (line.indexOf(',') >= 0) {
      result = ",";
    }

    return (result == null) ? "\t" : result;
  }
}
