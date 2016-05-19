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


import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Container for a set of records.
 * <p>
 * @author Spencer Koehler
 */
public interface RecordSet {
  
  /**
   * Get this record set's name.
   */
  public String getName();

  /**
   * Get the number of records in this set.
   */
  public int size();

  /**
   * Get an iterator over this set's records.
   */
  public Iterator<DataRecord> iterator();

  /**
   * Get the field MetaData for the named field.
   * <p>
   * Note that if there are duplicate field names, this will only
   * return the MetaData for the last.
   */
  public Map<String, FieldMetaData> getFieldMetaData();

  /**
   * Get the (possibly position sensitive) field names.
   */
  public List<String> getFieldNames();

  /**
   * Get the (last) field MetaData for the named field.
   */
  public FieldMetaData getFieldMetaData(String fieldName);

  /**
   * Get all values for the given field.
   * <p>
   * If the fieldName doesn't exist in this set, return null;
   * otherwise, return a non-null (possibly empty) result.
   */
  public List<String> getFieldValues(String fieldName);

  /**
   * Get widths of field contents (e.g., for formatted output).
   */
  public FieldWidths getFieldWidths();
}
