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


import org.sd.util.range.IntegerRange;

/**
 * Basic analysis object holding a string.
 * <p>
 * @author Spencer Koehler
 */
public class BasicAnalysisObject <T> extends AbstractAnalysisObject {

  private T value;

  public BasicAnalysisObject(T value) {
    super();
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value == null ? null : value.toString();  //todo: maybe limit the number of chars returned here and show its size?
  }

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"length\" -- string length of the value\n").
      append("range -- substring of the string value");
      
    return result.toString();
  }

  /**
   * Access components of this object as:
   * <ul>
   * <li>"length" -- string length of the value</li>
   * <li>range -- substring of the string value</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;
    StringBuilder builder = null;

    if ("length".equals(ref)) {
      result = new NumericAnalysisObject(value == null ? 0 : value.toString().length());
    }
    else {
      if (value != null) {
        try {
          final IntegerRange range = new IntegerRange(ref);
          final String valueString = value.toString();
          for (int i = 0; i < valueString.length(); ++i) {
            if (range.includes(i)) {
              if (builder == null) builder = new StringBuilder();
              builder.append(valueString.charAt(i));
            }
          }
        }
        catch (Exception e) {
          // not a range
        }
      }
    }

    return result == null ? (builder == null ? null : new BasicAnalysisObject<String>(builder.toString())) : result;
  }

  /** Get a numeric object representing this instance's value if applicable, or null. */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    NumericAnalysisObject result = null;

    if (value != null) {
      if (value instanceof Integer) {
        result = new NumericAnalysisObject((Integer)value);
      }
      else if (value instanceof Double) {
        result = new NumericAnalysisObject((Double)value);
      }
      else {
        final String valueString = value.toString();
        if (!"".equals(valueString)) {
          try {
            final Integer i = Integer.parseInt(valueString);
            result = new NumericAnalysisObject(i);
          }
          catch (NumberFormatException nfe) {
            //ignore
          }

          if (result == null) {
            try {
              final Double d = Double.parseDouble(valueString);
              result = new NumericAnalysisObject(d, valueString);
            }
            catch (NumberFormatException nfe) {
              //ignore
            }
          }
        }
      }
    }

    return result;
  }
}
