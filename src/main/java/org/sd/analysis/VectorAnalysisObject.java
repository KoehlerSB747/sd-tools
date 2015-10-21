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
import org.sd.util.StatsAccumulator;
import org.sd.util.range.IntegerRange;

/**
 * An analysis object that holds a vector.
 * <p>
 * @author Spencer Koehler
 */
public class VectorAnalysisObject <T> extends AbstractAnalysisObject {
  
  private String name;
  private List<T> values;

  public VectorAnalysisObject(String name, List<T> values) {
    super();
    this.name = name;
    this.values = values;
  }

  public int size() {
    return (values == null) ? 0 : values.size();
  }

  public String getName() {
    return name;
  }

  public List<T> getValues() {
    return values;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("#Vector.").
      append(name == null ? "" : name).
      append("[").append(values == null ? 0 : values.size()).append("]");
    return result.toString();
  }

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"length\" -- number of values in vector.\n").
      append("\"values\" -- toString version of values.\n").
      append("\"stats\" -- stats of numeric values in vector's value's toStrings.\n").
      append("\"text\" -- space-concatenated text of values.\n").
      append("\"text[\"delim\"]\" -- delim-concatenated text of values.\n").
      append("\"range\" -- subset of values in vector by index.");
      
    return result.toString();
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"length" -- number of values in vector</li>
   * <li>"values" -- tostring version of values</li>
   * <li>"stats" -- stats of numeric values in vector's value's toStrings</li>
   * <li>"text" -- space-concatenated text of values</li>
   * <li>"text["delim"]" -- delim-concatenated text of values</li>
   * <li>range -- subset of values in vector by index</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;
    List<T> selected = null;

    if ("length".equals(ref)) {
      result = new NumericAnalysisObject(values == null ? 0 : values.size());
    }
    else if ("values".equals(ref)) {
      result = new BasicAnalysisObject<String>(values == null ? "[]" : values.toString());
    }
    else if ("stats".equals(ref)) {
      if (values != null) {
        final StatsAccumulator stats = new StatsAccumulator(name);
        for (T value : values) {
          try {
            final double d = Double.parseDouble(value.toString());
            stats.add(d);
          }
          catch (NumberFormatException nfe) {
            // ignore
          }
        }
        if (stats.getN() > 0) {
          result = new StatsAnalysisObject(stats);
        }
      }
    }
    else if (ref.startsWith("text")) {
      String delim = " ";
      final int len = ref.length();
      if (len > 4 && ref.charAt(4) == '[' && ref.charAt(len - 1) == ']') {
        delim = (ref.length() == 4) ? " " : ref.substring(5, ref.length() - 1);
      }
      final StringBuilder builder = new StringBuilder();
      for (T value : values) {
        if (builder.length() > 0) builder.append(delim);
        builder.append(value.toString());
      }
      result = new BasicAnalysisObject<String>(builder.toString());
    }
    else {
      if (values != null) {
        try {
          final IntegerRange range = new IntegerRange(ref);
          for (int i = 0; i < values.size(); ++i) {
            if (range.includes(i)) {
              if (selected == null) selected = new ArrayList<T>();
              selected.add(values.get(i));
            }
          }
        }
        catch (Exception e) {
          // not a range
        }
      }
    }

    if (result == null && selected != null) {
      if (selected.size() == 1) {
        final T single = selected.get(0);
        if (single instanceof AnalysisObject) {
          result = (AnalysisObject)single;
        }
        else {
          result = new BasicAnalysisObject<T>(single);
        }
      }
    }

    return result;
  }

  /**
   * Get a numeric object representing this instance's value if applicable, or null.
   *
   * If there is a single numeric value in the vector, return it; otherwise, null.
   */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    NumericAnalysisObject result = null;

    if (values != null && values.size() == 1) {
      final T value = values.get(0);
      if (value instanceof AnalysisObject) {
        result = ((AnalysisObject)value).asNumericAnalysisObject();
      }
      else {
        final BasicAnalysisObject<String> single = new BasicAnalysisObject<String>(value.toString());
        result = single.asNumericAnalysisObject();
      }
    }

    return result;
  }
}
