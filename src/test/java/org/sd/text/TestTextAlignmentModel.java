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
package org.sd.text;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the TextAlignmentModel class.
 * <p>
 * @author Spence Koehler
 */
public class TestTextAlignmentModel extends TestCase {

  public TestTextAlignmentModel(String name) {
    super(name);
  }
  

  public static void testSimple1() {
    final String text = "This is a test. A test, this is. Rally to the test!";
    final String[] subTexts1 = new String[] {
      "This is a test.",
      "A test, this is.",
      "Rally to the test!",
    };
    final String[] subTexts2 = new String[] {
      "This is a test. A test",
      "is",
      "Rally to the test!",
    };
    final String[] subTexts3 = new String[] {
      "a test", "A test", "the test",
    };

    final TextAlignmentModel model1 = new TextAlignmentModel(text, TextAlignmentModel.buildSimpleSpans(text, Arrays.asList(subTexts1)));
    final TextAlignmentModel model2 = new TextAlignmentModel(text, TextAlignmentModel.buildSimpleSpans(text, Arrays.asList(subTexts2)));
    final TextAlignmentModel model3 = new TextAlignmentModel(text, TextAlignmentModel.buildSimpleSpans(text, Arrays.asList(subTexts3)));

    final List<? extends TextAlignmentModel.Span> spans1 = model1.getSpans();
    final List<? extends TextAlignmentModel.Span> spans2 = model2.getSpans();
    final List<? extends TextAlignmentModel.Span> spans3 = model3.getSpans();


    final int[] _c1 = {0, 1};  // subTexts3[0] and subTexts3[1] are both within subTexts2[0]
    final int[] _c2 = {};      // subTexts2[1] ("is") is not found in subTexts3

    //                            {  model0.align  }  {  model1.align }  {  model2.align  }
    //                              m0s0 m0s1 m0s2      m1s0 m1s1 m1s2     m2s0 m2s1 m2s2
    final int[][][] expected1 = { { {0}, {1}, {2}, }, { {0}, {0}, {2} }, { {0}, {1}, {2}, }, };  // spans0
    final int[][][] expected2 = { { {0}, {1}, {2}, }, { {0}, {1}, {2} }, { _c1, _c2, {2}, }, };  // spans1
    final int[][][] expected3 = { { {0}, {1}, {2}, }, { {0}, {0}, {2} }, { {0}, {1}, {2}, }, };  // spans2
    final int[][][][] expected = { expected1, expected2, expected3 };

    final TextAlignmentModel[] models = new TextAlignmentModel[] {
      model1, model2, model3,
    };

    final List<List<? extends TextAlignmentModel.Span>> spans = new ArrayList<List<? extends TextAlignmentModel.Span>>();
    spans.add(spans1); spans.add(spans2); spans.add(spans3);


    for (int spansIdx = 0; spansIdx < spans.size(); ++spansIdx) {
      final List<? extends TextAlignmentModel.Span> curSpans = spans.get(spansIdx);
      for (int spanIdx = 0; spanIdx < curSpans.size(); ++ spanIdx) {
        final TextAlignmentModel.Span curSpan = curSpans.get(spanIdx);
        for (int curModelIdx = 0; curModelIdx < models.length; ++curModelIdx) {
          final TextAlignmentModel curModel = models[curModelIdx];

          final TextAlignmentModel.Overlap overlap = curModel.getBestAlignedTo(curSpan);

          final int[] curExpected = expected[spansIdx][curModelIdx][spanIdx];
          if (curExpected == null) {
            //todo: show overlap
          }
          else if (curExpected.length == 0) {
            assertNull(String.format("spansIdx=%d, spanIdx=%d, curModelIdx=%d, given=%s",
                                     spansIdx, spanIdx, curModelIdx,
                                     curModel.getSpanText(curSpan)),
                       overlap);
          }
          else {
            final List<TextAlignmentModel.Span> expectedSpans = new ArrayList<TextAlignmentModel.Span>();
            int maxSpanLen = 0;
            for (int expectedSpanIdx : curExpected) {
              final TextAlignmentModel.Span s = spans.get(curModelIdx).get(expectedSpanIdx);
              final int slen = s.getEndIdx() - s.getStartIdx();
              if (slen > maxSpanLen) maxSpanLen = slen;
              expectedSpans.add(s);
            }
            final int expectedSize = Math.min(maxSpanLen, curSpan.getEndIdx() - curSpan.getStartIdx());

            if (overlap == null) {
              final boolean stopHere = true;
            }
            assertNotNull(String.format("spansIdx=%d, spanIdx=%d, curModelIdx=%d", spansIdx, spanIdx, curModelIdx),
                          overlap);
            // assertEquals(String.format("spansIdx=%d, spanIdx=%d, curModelIdx=%d", spansIdx, spanIdx, curModelIdx),
            //              expectedSize, overlap.getOverlapAmount());
            assertEquals(String.format("spansIdx=%d, spanIdx=%d, curModelIdx=%d", spansIdx, spanIdx, curModelIdx),
                         curExpected.length, overlap.getNumOverlappingSpans());

            int sIdx = 0;
            for (TextAlignmentModel.Span oSpan : overlap.getOverlappingSpans()) {
              if (!expectedSpans.get(sIdx).equals(oSpan)) {
                final boolean stopHere = true;
              }
              assertEquals(String.format("spansIdx=%d, spanIdx=%d, curModelIdx=%d, sIdx=%d, given=%s, expect=%s, got=%s",
                                         spansIdx, spanIdx, curModelIdx, sIdx,
                                         curModel.getSpanText(overlap.getOtherSpan()),
                                         curModel.getSpanText(expectedSpans.get(sIdx)),
                                         curModel.getSpanText(oSpan)),
                           expectedSpans.get(sIdx), oSpan);
              ++sIdx;
            }
          }
        }
      }
    }


/*
    for (TextAlignmentModel.Span span1 : spans1) {
      final int spanSize = span1.getEndPos() - span1.getStartPos();

      // spans1 should align perfectly with model1
      final TextAlignmentModel.Overlap overlap1 = model1.getBestAlignedTo(span1);
      assertNotNull(overlap1);
      assertEquals(spanSize, overlap1.getOverlapAmount());
      assertEquals(1, overlap1.getNumOverlappingSpans());
      assertEquals(span1, overlap1.getSingleOverlappingSpan());

      // check spans1 alignments with model2
      final TextAlignmentModel.Overlap overlap2 = model2.getBestAlignedTo(span1);
      final boolean stopHere = true;

      // check spans1 alignments with model3
    }

    for (TextAlignmentModel.Span span2 : spans2) {
      // check spans2 alignments with model1

      // spans2 should align perfectly with model2

      // check spans2 alignments with model3
    }

    for (TextAlignmentModel.Span span1 : spans1) {
      // check spans3 alignments with model1

      // check spans3 alignments with model2

      // spans3 should align perfectly with model3
    }
*/
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestTextAlignmentModel.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
