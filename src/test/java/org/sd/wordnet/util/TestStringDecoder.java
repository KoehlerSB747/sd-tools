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


import java.util.TreeMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the StringDecoder class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringDecoder extends TestCase {

  public TestStringDecoder(String name) {
    super(name);
  }
  

  public void testSegmenting() {
    doSegmentingTest(
      "{ [ darter, verb.motion:dart2,+ ] noun.Tops:animal,@ (a person or other animal that moves abruptly and rapidly; \"squirrels are darters\") }",
      new int[][] {
        {0, 137}, {2, 32}, {4, 11}, {12, 31}, {34, 52}, {53, 135}, {54, 55},
        {56, 62}, {63, 65}, {66, 71}, {72, 78}, {79, 83}, {84, 89}, {90, 98},
        {99, 102}, {103, 111}, {112, 122}, {123, 126}, {127, 134},
      });

    doSegmentingTest(
      "{ dog, }",
      new int[][] {
        {0, 7}, {2, 6},
      });

    doSegmentingTest(
      "{ [ confuse, clarify,! frames: 1 ] blur, obscure, frames: 8, 10 }",
      new int[][] {
        {0, 64}, {2, 33}, {4, 12}, {13, 22}, {23, 30}, {31, 32}, {35, 40}, {41, 49}, {50, 57}, {58, 60}, {61, 63},
      });

    doSegmentingTest(
      "{ [ HOT, COLD,! ] lukewarm(a), TEPID,^ (hot to the touch) }",
      new int[][] {
        {0, 58}, {2, 16}, {4, 8}, {9, 15}, {18, 30}, {26, 28}, {31, 38}, {39, 56}, {40, 43}, {44, 46}, {47, 50}, {51, 55},
      });

    doSegmentingTest(
      "{ [ basically, adj.all:essential^basic,\\ ] [ essentially, adj.all:basic^fundamental,\\ ] ( by one's very nature )}",
      new int[][] {
        {0, 112}, {2, 41}, {4, 14}, {15, 40}, {43, 86}, {45, 57}, {58, 85}, {88, 111}, {90, 92}, {93, 98}, {99, 103}, {104, 110},
      });
  }


  private final void doSegmentingTest(String lexLine, int[][] expectedSegmentBounds) {
    final StringDecoder decoder = new StringDecoder(lexLine);
    final TreeMap<Integer, StringDecoder.SegmentInfo> segments = decoder.getSegments();

    if (expectedSegmentBounds == null) {
      System.out.print("\tlexLine=" + lexLine + "\n\t\t");
    }

    int num = 0;
    for (StringDecoder.SegmentInfo segment : segments.values()) {
      if (expectedSegmentBounds != null) {
        assertEquals(expectedSegmentBounds[num][0], segment.getStartPos());
        assertEquals(expectedSegmentBounds[num][1], segment.getEndPos());
      }
      else {
        System.out.print("{" + segment.getStartPos() + ", " + segment.getEndPos() + "}, ");
      }
      ++num;
    }

    if (expectedSegmentBounds == null) {
      System.out.println();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringDecoder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
