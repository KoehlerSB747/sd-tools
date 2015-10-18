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
package org.sd.text;


import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the PhraseIndex class.
 * <p>
 * @author Spence Koehler
 */
public class TestPhraseIndex extends TestCase {

  public TestPhraseIndex(String name) {
    super(name);
  }
  

  public void testBasics() {
    final PhraseIndex<Integer> phraseIndex = new PhraseIndex<Integer>();
    
    final String[] phrases = new String[] {
      "north dakota",
      "north carolina",
      "south dakota",
      "south carolina",
    };

    for (int i = 0; i < phrases.length; ++i) {
      phraseIndex.put(phrases[i].split("\\s+"), i);
    }

    final List<PhraseIndex.RetrievalResult<Integer>> north = phraseIndex.get("north", new String[]{"north"});
    assertEquals(2, north.size());
    assertEquals(0, north.get(0).getScore());
    assertEquals(0, north.get(1).getScore());
    assertFalse(north.get(0).isExactMatch());
    assertFalse(north.get(1).isExactMatch());

    final List<PhraseIndex.RetrievalResult<Integer>> northDakota = phraseIndex.get("north dakota", new String[]{"north", "dakota"});
    assertEquals(1, northDakota.size());
    assertEquals(2, northDakota.get(0).getScore());
    assertTrue(northDakota.get(0).isExactMatch());

    final List<PhraseIndex.RetrievalResult<Integer>> dakotaNorth = phraseIndex.get("dakota north", new String[]{"dakota", "north"});
    assertEquals(1, dakotaNorth.size());
    assertEquals(0, dakotaNorth.get(0).getScore());
    assertFalse(dakotaNorth.get(0).isExactMatch());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestPhraseIndex.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
