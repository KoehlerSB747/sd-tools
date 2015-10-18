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


/**
 * An AnalysisObject for numeric values.
 * <p>
 * @author Spencer Koehler
 */
public class NumericAnalysisObject implements AnalysisObject {
  
  private Integer intValue;
  private Long longValue;
  private Double doubleValue;
  private String stringForm;

  public NumericAnalysisObject(Integer intValue) {
    this(intValue, null);
  }

  public NumericAnalysisObject(Integer intValue, String stringForm) {
    this.intValue = intValue;
    this.stringForm = (stringForm == null) ? (intValue == null ? "" : intValue.toString()) : stringForm;
    this.doubleValue = null;
  }

  public NumericAnalysisObject(Long longValue) {
    this(longValue, null);
  }

  public NumericAnalysisObject(Long longValue, String stringForm) {
    this.longValue = longValue;
    this.stringForm = (stringForm == null) ? (longValue == null ? "" : longValue.toString()) : stringForm;
    this.doubleValue = null;
  }

  public NumericAnalysisObject(Double doubleValue) {
    this(doubleValue, null);
  }

  public NumericAnalysisObject(Double doubleValue, String stringForm) {
    this.doubleValue = doubleValue;
    this.stringForm = (stringForm == null) ? (doubleValue == null ? "" : doubleValue.toString()) : stringForm;
    this.intValue = null;
  }

  public boolean isInteger() {
    return intValue != null;
  }

  public Integer getIntegerValue() {
    return intValue;
  }

  public boolean isLong() {
    return longValue != null;
  }

  public Long getLongValue() {
    return longValue;
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public boolean isDouble() {
    return doubleValue != null;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    return stringForm;
  }

  /** Get a detailed string representation of this object's data. */
  @Override
  public String getDetailedString() {
    return stringForm;
  }

  /** Access components of this object according to ref. */
  @Override
  public AnalysisObject access(String ref) {
    return null;
  }

  /** Get a numeric object representing this instance's value if applicable, or null. */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    return this;
  }
}
