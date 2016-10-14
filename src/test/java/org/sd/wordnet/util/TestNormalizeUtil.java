/*
   Copyright 2008-2016 Semantic Discovery, Inc.

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
package org.sd.wordnet.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the NormalizeUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestNormalizeUtil extends TestCase {

  public TestNormalizeUtil(String name) {
    super(name);
  }
  

  public void testNormalize() {
    doNormalizeTest("This  is  a @#$%! Test", "this is a test");
    doNormalizeTest("This's  'a' \"Test\"", "this's a test");
    doNormalizeTest("sanders'", "sanders");
    doNormalizeTest("'a", "'a");
    doNormalizeTest("3.14159", "3.14159");
    doNormalizeTest(".14159", ".14159");
  }

  public void testTrimPossessive() {
    doNormalizeTest("Jacob's", "jacob");
    doNormalizeTest("brother's", "brother");
    doNormalizeTest("MAJESTY'S", "majesty");
    doNormalizeTest("ass's", "ass");
  }

  public void testTrimDigits() {
    doTrimDigitsTest("order1", "order");
    doTrimDigitsTest("say9", "say");
    doTrimDigitsTest("99", "99");
    doTrimDigitsTest("okay", "okay");
    doTrimDigitsTest("fall8", "fall");
    doTrimDigitsTest("light12", "light");
    doTrimDigitsTest("3.14159", "3.14159");
    doTrimDigitsTest(".14159", ".14159");
  }

  private final void doNormalizeTest(String input, String  expected) {
    final String norm = NormalizeUtil.normalizeForLookup(input);
    assertEquals(expected, norm);
  }

  private final void doTrimDigitsTest(String input, String expected) {
    final String trimmed = NormalizeUtil.trimDigits(input);
    assertEquals(expected, trimmed);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestNormalizeUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
