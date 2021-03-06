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
package org.sd.wordnet.lex;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * JUnit Tests for the SimpleWord class.
 * <p>
 * @author Spence Koehler
 */
public class TestSimpleWord extends TestCase {

  public TestSimpleWord(String name) {
    super(name);
  }
  

  public void testFixWord1() {
    final SimpleWord simpleWord = new SimpleWord();
    simpleWord.setWord("assault1");

    assertEquals("assault1", simpleWord.getFormattedWord());
    assertEquals("assault", simpleWord.getWord());
    assertEquals(1, simpleWord.getLexId());
    assertEquals("assault1", simpleWord.getName());
    assertEquals("assault", simpleWord.getNormalizedWord());
    assertFalse(simpleWord.hasMarker());
  }

  public void testFixWord2() {
    final SimpleWord simpleWord = new SimpleWord();
    simpleWord.setWord("assault1(p)");

    assertEquals("assault1(p)", simpleWord.getFormattedWord());
    assertEquals("assault", simpleWord.getWord());
    assertEquals(1, simpleWord.getLexId());
    assertEquals("assault1", simpleWord.getName());
    assertEquals("assault", simpleWord.getNormalizedWord());
    assertTrue(simpleWord.hasMarker());
    assertEquals("(p)", simpleWord.getMarker());
  }

  public void testFixWord3() {
    final SimpleWord simpleWord = new SimpleWord();
    simpleWord.setWord("assault(p)1");

    assertEquals("assault(p)1", simpleWord.getFormattedWord());
    assertEquals("assault", simpleWord.getWord());
    assertEquals(1, simpleWord.getLexId());
    assertEquals("assault1", simpleWord.getName());
    assertEquals("assault", simpleWord.getNormalizedWord());
    assertTrue(simpleWord.hasMarker());
    assertEquals("(p)", simpleWord.getMarker());
  }

  public void testNormalization() {
    doNormalizationTest("mr._moto");
    doNormalizationTest("mr.moto");
    doNormalizationTest("st._elias_range");
    doNormalizationTest("st._elmo's_fire");
    doNormalizationTest("pigs'_knuckles");
    doNormalizationTest("court_of_saint_james's");
    doNormalizationTest("Achilles'_heel");
    doNormalizationTest("shoot-'em-up");
    doNormalizationTest("old_wives'_tale");
  }

  private final void doNormalizationTest(String wordText) {
    final SimpleWord simpleWord = new SimpleWord();
    simpleWord.setWord(wordText);
    final String normalizedWord = NormalizeUtil.normalizeForLookup(wordText);
    final String simpleNorm = simpleWord.getNormalizedWord();

    // if (!simpleNorm.equals(normalizedWord)) {
    //   System.out.println(wordText + "\t" + simpleNorm + "\t" + normalizedWord);
    // }
    assertEquals(wordText, simpleNorm, normalizedWord);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSimpleWord.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
