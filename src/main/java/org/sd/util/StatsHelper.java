/*
   Copyright 2008-2017 Semantic Discovery, Inc.

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
package org.sd.util;


/**
 * Statsitical helper utilities.
 * <p>
 * @author Spencer Koehler
 */
public class StatsHelper {
  
  /**
   * Given 2 binomial random variables estimated by a count and a total,
   * determine the z value for the null hypothesis that p1 == p2 versus the
   * alternative hypothesis that p1 != p2, where estimates are
   * p1 = count1 / n1 and p2 = count2 / n2
   * <p>
   * Reject the null hypothesis for a 2-tailed test at 95% confidence
   * if e.g. z > z(alpha/2)=1.96; or 2.75 for 99.7% confidence.
   *
   * @param count1  The number of successful observations for variable1
   * @param n1  The total number of trials for variable1
   * @param count2  The number of successful observations for variable2
   * @param n2  The total number of trials for variable2
   *
   * @return null if there is not enough data to compute the test;
   *         otherwise, the z value.
   */
  public static final Double binomialSignificanceTest(long count1, long n1, long count2, long n2) {
    if (!meetsBinomialSignificanceAssumptions(count1, n1, count2, n2)) {
      return null;
    }

    final double p1 = ((double)count1) / ((double)n1);
    final double p2 = ((double)count2) / ((double)n2);

    // z = (p1 - p2) / sqrt( p(1-p)(1/n1 + 1/n2) ), p = (n1p1+n2p2)/(n1+n2)
    final double p = ((double)(count1 + count2)) / ((double)(n1 + n2));
    final double z = ((double)(p1 - p2)) / Math.sqrt((p * (1.0 - p)) * ((1.0 / ((double)n1)) + (1.0 / ((double)n2))));

    return z;
  }

  public static final boolean meetsBinomialSignificanceAssumptions(long count1, long n1, long count2, long n2) {
    boolean result = false;

    if (count1 > 5 && count2 > 5) {  // given that n1p1 > 5  &&  n2p2 > 5
      // make sure also  n1(1-p1) > 5 && n2(1-p2) > 5
      result = (n1 - count1) > 5 && (n2 - count2) > 5;
    }

    return result;
  }
}
