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
 * JUnit Tests for the TransformUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestTransformUtil extends TestCase {

  public TestTransformUtil(String name) {
    super(name);
  }
  

  public void testApplyTransformations() {
    doApplyTransformsTest("xn'ttl", "xn'ttl");
    doApplyTransformsTest("He cannot have known I didn't do it", "He can not have known I did not do it");
    doApplyTransformsTest("Cannot be done.", "can not be done");
  }

  private final void doApplyTransformsTest(String input, String expected) {
    final String got = TransformUtil.applyTransformations(input);
    assertEquals(expected, got);
  }


  public void testReplaceXnt() {
    doReplaceXntTest("He couldn't have known that I hadn't done it.", "He could not have known that I had not done it.");
    doReplaceXntTest("Couldn't shouldn't wouldn't", "Could not should not would not");
    doReplaceXntTest("He cannot have known I didn't do it", "He cannot have known I did not do it");
    doReplaceXntTest("shan't", "shall not");
    doReplaceXntTest("Haven't", "Have not");
    doReplaceXntTest("Can't", "can not");
    doReplaceXntTest("xn'ttl", "xn'ttl");
  }

  private final void doReplaceXntTest(String input, String expected) {
    final String got = TransformUtil.replaceXnt(input);
    assertEquals(expected, got);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestTransformUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
