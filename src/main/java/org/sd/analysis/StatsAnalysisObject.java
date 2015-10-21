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


import org.sd.util.StatsAccumulator;

/**
 * An analysis object that holds basic summary stats.
 * <p>
 * @author Spencer Koehler
 */
public class StatsAnalysisObject extends AbstractAnalysisObject {
  
  private StatsAccumulator stats;

  public StatsAnalysisObject(StatsAccumulator stats) {
    super();
    this.stats = stats;
  }

  public String getName() {
    return (stats == null) ? null : stats.getLabel();
  }

  public StatsAccumulator getStats() {
    return stats;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("#Stats.");

    if (stats == null) {
      result.append("EMPTY");
    }
    else {
      result.append(stats.toString());
    }

    return result.toString();
  }

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"n\" -- get the number of items collected\n").
      append("\"mean\" -- get the mean of the values.\n").
      append("\"stddev\" -- get the standard deviation of the values.\n").
      append("\"variance\" -- get the variance of the values.\n").
      append("\"min\" -- get the min of the values.\n").
      append("\"max\" -- get the max of the values.\n").
      append("\"sum\" -- get the sum of the values.\n").
      append("\"sumOfSquares\" -- get the sum of each value squared.");
      
    return result.toString();
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"n" -- get the number of items collected.</li>
   * <li>"mean" -- get the mean of the values.</li>
   * <li>"stddev" -- get the standard deviation of the values.</li>
   * <li>"variance" -- get the variance of the values.</li>
   * <li>"min" -- get the min of the values.</li>
   * <li>"max" -- get the max of the values.</li>
   * <li>"sum" -- get the sum of the values.</li>
   * <li>"sumOfSquares" -- get the sum of each value squared.</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;

    if (stats != null) {
      if ("n".equals(ref)) {
        result = new NumericAnalysisObject(stats.getN());
      }
      else if ("mean".equals(ref)) {
        result = new NumericAnalysisObject(stats.getMean());
      }
      else if ("stddev".equals(ref)) {
        result = new NumericAnalysisObject(stats.getStandardDeviation());
      }
      else if ("variance".equals(ref)) {
        result = new NumericAnalysisObject(stats.getVariance());
      }
      else if ("min".equals(ref)) {
        result = new NumericAnalysisObject(stats.getMin());
      }
      else if ("max".equals(ref)) {
        result = new NumericAnalysisObject(stats.getMax());
      }
      else if ("sum".equals(ref)) {
        result = new NumericAnalysisObject(stats.getSum());
      }
      else if ("sumOfSquares".equals(ref)) {
        result = new NumericAnalysisObject(stats.getSumOfSquares());
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
