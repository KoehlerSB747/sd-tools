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
import java.util.Arrays;
import java.util.List;

/**
 * An analysis object that holds a comparison of two AnalysisObjects.
 * <p>
 * @author Spencer Koehler
 */
public class ComparisonAnalysisObject extends AbstractAnalysisObject {
  
  private AnalysisObject object1;
  private AnalysisObject object2;
  private int compare;
  private String[] diffs;

  public ComparisonAnalysisObject(AnalysisObject object1, AnalysisObject object2, int compare, String[] diffs) {
    super();
    this.object1 = object1;
    this.object2 = object2;
    this.compare = compare;
    this.diffs = diffs;
  }

  public AnalysisObject getObject1() {
    return object1;
  }

  public AnalysisObject getObject2() {
    return object2;
  }

  public int getCompare() {
    return compare;
  }

  public boolean matches() {
    return compare == 0;
  }

  public String[] getDiffs() {
    return diffs;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("#Comparison.").
      append(compare).
      append(".").
      append(object1 == null ? "NULL" : object1.toString()).
      append("-vs-").
      append(object2 == null ? "NULL" : object2.toString());
    return result.toString();
  }

  /** Customization for "show" access. */
  protected String getShowString() {
    final StringBuilder result = new StringBuilder();

    if (diffs != null) {
      for (String diff : diffs) {
        if (result.length() > 0) result.append("\n");
        result.append(diff);
      }
    }

    return result.toString();
  }

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"1\" -- get the first compare object.\n").
      append("\"2\" -- get the second compare object.\n").
      append("\"compare\" -- get the numeric compare value.\n").
      append("\"diffs\" -- get the diffs vector.");
      
    return result.toString();
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"1" -- get the first compare object.</li>
   * <li>"2" -- get the second compare object.</li>
   * <li>"compare" -- get the numeric compare value.</li>
   * <li>"diffs" -- get the diffs vector.</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;

    if ("1".equals(ref)) {
      result = object1;
    }
    else if ("2".equals(ref)) {
      result = object2;
    }
    else if ("compare".equals(ref)) {
      result = new NumericAnalysisObject(compare);
    }
    else if ("diffs".equals(ref)) {
      result = new VectorAnalysisObject<String>("diffs", diffs == null ? new ArrayList<String>() : Arrays.asList(diffs));
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
    return new NumericAnalysisObject(compare);
  }
}
