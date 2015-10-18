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


//import java.util.LinkedHashSet;
//import java.util.Set;

/**
 * Container for MetaData for a field.
 * <p>
 * @author Spencer Koehler
 */
public class FieldMetaData {

//  public enum DataType { NUMERIC, DATE, STRING };
  
  private String fieldName;
//  private Set<DataType> dataTypes;

  public FieldMetaData(String fieldName) {
    this.fieldName = fieldName;
//    this.dataTypes = null;
  }

  public String getFieldName() {
    return fieldName;
  }

  // public Set<DataType> getDataTypes() {
  //   return dataTypes;
  // }

  // public boolean hasDataType() {
  //   return dataTypes != null && dataTypes.size() > 0;
  // }

  // public boolean hasDataType(DataType dataType) {
  //   return dataTypes.contains(dataType);
  // }

  // public FieldMetaData addDataType(DataType dataType) {
  //   if (this.dataTypes == null) this.dataTypes = new LinkedHashSet<DataType>();
  //   this.dataTypes.add(dataType);
  // }
}
