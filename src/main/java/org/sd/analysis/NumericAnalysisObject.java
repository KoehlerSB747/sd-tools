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
public class NumericAnalysisObject extends AbstractAnalysisObject {
  
  private Integer intValue;
  private Long longValue;
  private Double doubleValue;
  private String stringForm;

  public NumericAnalysisObject(Integer intValue) {
    this(intValue, null);
  }

  public NumericAnalysisObject(Integer intValue, String stringForm) {
    super();
    this.intValue = intValue;
    this.stringForm = (stringForm == null) ? (intValue == null ? "" : intValue.toString()) : stringForm;
    this.doubleValue = null;
  }

  public NumericAnalysisObject(Long longValue) {
    this(longValue, null);
  }

  public NumericAnalysisObject(Long longValue, String stringForm) {
    super();
    this.longValue = longValue;
    this.stringForm = (stringForm == null) ? (longValue == null ? "" : longValue.toString()) : stringForm;
    this.doubleValue = null;
  }

  public NumericAnalysisObject(Double doubleValue) {
    this(doubleValue, null);
  }

  public NumericAnalysisObject(Double doubleValue, String stringForm) {
    super();
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

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"subtract[$other]\" -- subtract the other from this.\n").
      append("\"add[$other]\" -- add the other to this.");
      
    return result.toString();
  }
  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"subtract[$other]" -- subtract the other from this</li>
   * <li>"add[$other]" -- add the other to this</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;

    if (ref.startsWith("subtract")) {
      final AnalysisObject[] args = getArgValues(ref, env);
      NumericAnalysisObject retval = this;
      if (args != null) {
        for (AnalysisObject arg : args) {
          retval = retval.subtract(arg.asNumericAnalysisObject());
        }
      }
      result = retval;
    }
    else if (ref.startsWith("add")) {
      final AnalysisObject[] args = getArgValues(ref, env);
      NumericAnalysisObject retval = this;
      if (args != null) {
        for (AnalysisObject arg : args) {
          retval = retval.add(arg.asNumericAnalysisObject());
        }
      }
      result = retval;
    }

    return result;
  }

  /** Get a numeric object representing this instance's value if applicable, or null. */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    return this;
  }


  double asDouble() {
    double result = 0.0;

    if (this.isDouble()) {
      result = doubleValue;
    }
    else if (this.isLong()) {
      result = (double)longValue;
    }
    else if (this.isInteger()) {
      result = (double)intValue;
    }

    return result;
  }

  NumericAnalysisObject subtract(NumericAnalysisObject other) {
    NumericAnalysisObject result = this;

    if (other != null) {
      final double value = this.asDouble() - other.asDouble();
      result = buildLikeObject(value);
    }

    return result;
  }

  NumericAnalysisObject add(NumericAnalysisObject other) {
    NumericAnalysisObject result = this;

    if (other != null) {
      final double value = this.asDouble() + other.asDouble();
      result = buildLikeObject(value);
    }

    return result;
  }

  private final NumericAnalysisObject buildLikeObject(double value) {
    NumericAnalysisObject result = this;

    if (this.isDouble()) {
      result = new NumericAnalysisObject(value);
    }
    else if (this.isLong()) {
      result = new NumericAnalysisObject((long)(value + 0.5));
    }
    else if (this.isInteger()) {
      result = new NumericAnalysisObject((int)(value + 0.5));
    }

    return result;
  }
}
