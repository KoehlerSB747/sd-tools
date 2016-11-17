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
package org.sd.nlp.conll;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the ConllSentenceAligner class.
 * <p>
 * @author Spence Koehler
 */
public class TestConllSentenceAligner extends TestCase {

  public TestConllSentenceAligner(String name) {
    super(name);
  }
  

  public void test1() {
    final String conllString = "``. Robert Thompkins.";
    final String origString = "«. Robert Thompkins.";

    final ConllSentenceAligner aligner = new ConllSentenceAligner(conllString, origString);
    assertTrue(aligner.aligns());
  }

  public void test2() {
    final String conllString = "Section 7. -RRB- Elizabeth Dabney was bora June 18, 1751.";
    final String origString = "Section 7.) Elizabeth Dabney was bora June 18, 1751.";

    final ConllSentenceAligner aligner = new ConllSentenceAligner(conllString, origString);
    assertTrue(aligner.aligns());

    final int origPtr = 38;  // June
    final int conllPtr = 43;  // June

    final int[] c2o = aligner.getOrigPos(conllPtr);
    final int[] o2c = aligner.getConllPos(origPtr);
    assertEquals(origPtr, c2o[0]);
    assertEquals(conllPtr, o2c[0]);

    // Show conll, orig can be swapped
    final ConllSentenceAligner reverse = new ConllSentenceAligner(origString, conllString);
    assertTrue(reverse.aligns());

    final int[] c2oR = reverse.getOrigPos(origPtr);
    final int[] o2cR = reverse.getConllPos(conllPtr);
    assertEquals(conllPtr, c2oR[0]);
    assertEquals(origPtr, o2cR[0]);

    // Show conll, orig can be the same
    final ConllSentenceAligner sameO = new ConllSentenceAligner(origString, origString);
    assertTrue(sameO.aligns());
    final int[] o2o = sameO.getOrigPos(origPtr);
    assertEquals(origPtr, o2o[0]);

    final ConllSentenceAligner sameC = new ConllSentenceAligner(conllString, conllString);
    assertTrue(sameC.aligns());
    final int[] c2c = sameO.getConllPos(conllPtr);
    assertEquals(conllPtr, c2c[0]);
}


  public static Test suite() {
    TestSuite suite = new TestSuite(TestConllSentenceAligner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
