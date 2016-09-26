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
package org.sd.atnexec;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the Sequence class.
 * <p>
 * @author Spence Koehler
 */
public class TestProgressionTokenTest extends TestCase {

  public TestProgressionTokenTest(String name) {
    super(name);
  }
  

  public void testSimpleOrder() {
    final ProgressionTokenTest.Sequence mySequence = new ProgressionTokenTest.Sequence("det,adj,noun", null, null, true);
    // allows progression of det(s) to adj(s) to noun(s) without backtracking

    // test accept conforming tokens with all known tokens
    doSequenceTest(mySequence, new String[]{"det", "adj"}, true);
    doSequenceTest(mySequence, new String[]{"adj", "noun"}, true);
    doSequenceTest(mySequence, new String[]{"det", "noun"}, true);
    doSequenceTest(mySequence, new String[]{"det", "det"}, true);
    doSequenceTest(mySequence, new String[]{"adj", "adj"}, true);
    doSequenceTest(mySequence, new String[]{"noun", "noun"}, true);

    // test reject noncomforming tokens with all known tokens
    doSequenceTest(mySequence, new String[]{"noun", "det"}, false);
    doSequenceTest(mySequence, new String[]{"noun", "adj"}, false);
    doSequenceTest(mySequence, new String[]{"adj", "det"}, false);

    // test with unknown tokens
    doSequenceTest(mySequence, new String[]{"conj", "det"}, true);
    doSequenceTest(mySequence, new String[]{"det", "conj"}, true);
    doSequenceTest(mySequence, new String[]{"conj", "adj"}, true);
    doSequenceTest(mySequence, new String[]{"adj", "conj"}, true);
    doSequenceTest(mySequence, new String[]{"conj", "noun"}, true);
    doSequenceTest(mySequence, new String[]{"noun", "conj"}, true);
  }

  public void testComplexOrder() {
    final ProgressionTokenTest.Sequence mySequence = new ProgressionTokenTest.Sequence("det,adj,noun", null, null, true);
    // allows progression of det(s) to adj(s) to noun(s) without backtracking

    // test accept conforming tokens with all known tokens
    doSequenceTest(mySequence, new String[]{"det,noun", "adj,noun"}, true);
    doSequenceTest(mySequence, new String[]{"adj,det", "noun"}, true);
    doSequenceTest(mySequence, new String[]{"det", "noun,adj"}, true);
    doSequenceTest(mySequence, new String[]{"det,adj,noun", "det,adj,noun"}, true);

    // test reject noncomforming tokens with all known tokens
    doSequenceTest(mySequence, new String[]{"noun,adj", "det"}, false);
    doSequenceTest(mySequence, new String[]{"noun", "adj,det"}, false);
  }

  public void testSimpleNotAfter() {
    final ProgressionTokenTest.Sequence mySequence = new ProgressionTokenTest.Sequence(null, "prep", "noun", true);
    // disallows progression of noun *immediately* back to prep

    // test accept conforming tokens
    doSequenceTest(mySequence, new String[]{"det", "noun"}, true);
    doSequenceTest(mySequence, new String[]{"conj", "noun"}, true);
    doSequenceTest(mySequence, new String[]{"prep", "noun"}, true);

    // test reject disallowed tokens
    doSequenceTest(mySequence, new String[]{"noun", "prep"}, false);


    doSequenceTest(mySequence, new String[]{"noun", "conj"}, true);
    doSequenceTest(mySequence, new String[]{"conj", "prep"}, true);
    doSequenceTest(mySequence, new String[]{"noun", "conj", "prep"}, false);
  }

  public void testNegatedFeature() {
    final ProgressionTokenTest.Sequence mySequence = new ProgressionTokenTest.Sequence(null, "auxv", "!auxv", true);
    
    // test accept conforming tokens
    doSequenceTest(mySequence, new String[]{"aux", "verb"}, true);
    doSequenceTest(mySequence, new String[]{"auxv", "auxv"}, true);
    doSequenceTest(mySequence, new String[]{"auxv,verb", "auxv"}, true);
    doSequenceTest(mySequence, new String[]{"verb,auxv", "auxv"}, true);

    // test reject non-conforming tokens
    doSequenceTest(mySequence, new String[]{"verb", "auxv"}, false);
    doSequenceTest(mySequence, new String[]{"adj,verb,noun", "verb,auxv"}, false);
  }


  private final void doSequenceTest(ProgressionTokenTest.Sequence sequence, String[] tokens, boolean expectedAccept) {
    final MyTokenContainer tokenContainer = new MyTokenContainer(tokens);

    final boolean accept = sequence.doAccept(tokenContainer, false);

    assertEquals(sequence.toString() + " failed acceptance=" + expectedAccept + " for tokens=" + tokenContainer,
                 expectedAccept, accept);
  }
  

  private static final class MyToken {
    public final String value;
    public final Set<String> features;

    MyToken(String value) {
      this.value = value;
      this.features = new HashSet<String>();

      final String[] pieces = value.split("\\s*,\\s*");
      for (String piece : pieces) {
        features.add(piece);
      }
    }
  }

  private static final class MyTokenContainer extends ProgressionTokenTest.TokenContainer {

    private ArrayList<MyToken> tokens;  // tokens are defined by features
    private StringBuilder stringBuilder;

    MyTokenContainer(String[] tokens) {
      super();

      this.tokens = new ArrayList<MyToken>();
      this.stringBuilder = new StringBuilder();

      for (String token : tokens) {
        this.tokens.add(new MyToken(token));

        if (stringBuilder.length() > 0) stringBuilder.append(' ');
        stringBuilder.append('[').append(token).append(']');
      }
    }

    protected int getNumTokens() {
      return tokens.size();
    }

    protected boolean tokenHasFeature(int tokenNum, String feature) {
      return tokens.get(tokenNum).features.contains(feature);
    }

    public String getTokenString(int tokenNum) {
      return tokens.get(tokenNum).value;
    }

    public String toString() {
      return stringBuilder.toString();
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestProgressionTokenTest.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
