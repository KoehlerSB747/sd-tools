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


import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the LexParser class.
 * <p>
 * @author Spence Koehler
 */
public class TestLexParser extends TestCase {

  public TestLexParser(String name) {
    super(name);
  }
  

  public void testParseVerbFrames() {
    final String lexString = "{ [ rethink, noun.cognition:rethink,+ frames: 8 ] think1,@ frames: 2 (change one's mind; \"He rethought his decision to take a vacation\") }";
    final Synset synset = LexParser.parseLexString(lexString);

    assertTrue(synset.hasFrames());
    assertEquals(1, synset.getFrames().size());
    assertTrue(synset.getFrames().contains(2));

    assertEquals(1, synset.size());
    final Word word = synset.getWords().get(0);
    assertTrue(word.hasFrames());
    assertEquals(1, word.getFrames().size());
    assertTrue(word.getFrames().contains(8));

    final Set<Integer> allFrames = word.getAllFrames();
    assertEquals(2, allFrames.size());
    assertTrue(allFrames.contains(2));
    assertTrue(allFrames.contains(8));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestLexParser.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
