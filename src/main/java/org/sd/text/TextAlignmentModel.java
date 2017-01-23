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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Utility for aligning spans within text to each other.
 * <p>
 * That is, given a block of text with marked up spans from algorithm A
 * and the same text with marked up spans from algorithm B, find the
 * best A-span / B-span alignments.
 *
 * @author Spencer Koehler
 */
public class TextAlignmentModel {
  
  public interface Span extends Comparable<Span> {
    public int getStartIdx();
    public int getEndIdx();
    public boolean agreesWith(Span other);
  }

  public static List<Span> buildSimpleSpans(String text, List<String> subTexts) {
    final List<Span> result = new ArrayList<Span>();

    int idx = 0;
    for (String subText : subTexts) {
      final int startIdx = text.indexOf(subText, idx);
      if (startIdx >= 0) {
        final int endIdx = startIdx + subText.length();
        result.add(new SimpleSpan(startIdx, endIdx));
        idx = endIdx;
      }
    }

    return result;
  }


  private String text;
  private List<? extends Span> spans;
  private TreeMap<Integer, TreeMap<Integer, Set<Span>>> start2end2spans;

  public TextAlignmentModel(String text, List<? extends Span> spans) {
    this.text = text;
    this.spans = spans;
    this.start2end2spans = new TreeMap<Integer, TreeMap<Integer, Set<Span>>>();
    if (spans != null) {
      for (Span span : spans) {
        this.add(span);
      }
    }
  }

  public String getText() {
    return text;
  }

  public List<? extends Span> getSpans() {
    return spans;
  }

  public String getSpanText(Span span) {
    String result = null;

    if (span != null) {
      result = text.substring(span.getStartIdx(), span.getEndIdx());
    }

    return result;
  }

  private final void add(Span span) {
    final int startIdx = span.getStartIdx();
    final int endIdx = span.getEndIdx();

    TreeMap<Integer, Set<Span>> end2spans = start2end2spans.get(startIdx);
    if (end2spans == null) {
      end2spans = new TreeMap<Integer, Set<Span>>();
      start2end2spans.put(startIdx, end2spans);
    }
    Set<Span> curSpans = end2spans.get(endIdx);
    if (curSpans == null) {
      curSpans = new TreeSet<Span>();
      end2spans.put(endIdx, curSpans);
    }
    curSpans.add(span);
  }

  /**
   * Get the spans in this model that best align with the given otherSpan.
   *
   * @param otherSpan  The other span to find a match to in this model.
   *
   * @return the amount of overlap (number of characters) of the matching spans
   *         (key) and those spans (value), or null
   */
  public Overlap getBestAlignedTo(Span otherSpan) {
    Map.Entry<Integer, Set<Span>> result = null;

    // Find
    //   the longest overlapping span
    //   that starts before other's end
    //   and ends after the other's start
    //   with a matching type

    final TreeMap<Integer, Set<Span>> overlap2existing = new TreeMap<Integer, Set<Span>>();

    final int otherStartIdx = otherSpan.getStartIdx();
    final int otherEndIdx = otherSpan.getEndIdx();

    for (Integer existingStartIdx = start2end2spans.lowerKey(otherEndIdx);
         existingStartIdx != null;
         existingStartIdx = start2end2spans.lowerKey(existingStartIdx)) {

      final TreeMap<Integer, Set<Span>> end2spans = start2end2spans.get(existingStartIdx);

      for (Integer existingEndIdx = end2spans.higherKey(otherStartIdx);
           existingEndIdx != null;
           existingEndIdx = end2spans.higherKey(existingEndIdx)) {

        final Set<Span> existingSpans = end2spans.get(existingEndIdx);
      
        for (Span existingSpan : existingSpans) {
          // skip if existingSpan lacks agreement with otherSpan
          if (!existingSpan.agreesWith(otherSpan)) continue;

          final int overlap =
            Math.min(otherEndIdx, existingSpan.getEndIdx()) -
            Math.max(otherStartIdx, existingSpan.getStartIdx());

          Set<Span> existing = overlap2existing.get(overlap);
          if (existing == null) {
            existing = new TreeSet<Span>();
            overlap2existing.put(overlap, existing);
          }
          existing.add(existingSpan);
        }
      }
    }

    if (overlap2existing.size() > 0) {
      result = overlap2existing.lastEntry();
    }

    return result == null ? null : new Overlap(otherSpan, this, result.getKey(), result.getValue());
  }


  /**
   * Data structure holding overlapping spans for a model.
   */
  public static class Overlap {
    private Span otherSpan;
    private TextAlignmentModel model;
    private int overlapAmount;
    private Set<Span> overlappingSpans;

    public Overlap(Span otherSpan, TextAlignmentModel model, int overlapAmount, Set<Span> overlappingSpans) {
      this.otherSpan = otherSpan;
      this.model = model;
      this.overlapAmount = overlapAmount;
      this.overlappingSpans = overlappingSpans;
    }

    public Span getOtherSpan() {
      return otherSpan;
    }

    public TextAlignmentModel getModel() {
      return model;
    }

    public int getOverlapAmount() {
      return overlapAmount;
    }

    public Set<Span> getOverlappingSpans() {
      return overlappingSpans;
    }

    public int getNumOverlappingSpans() {
      return (overlappingSpans == null) ? 0 : overlappingSpans.size();
    }

    public boolean hasSingleOvlerappingSpan() {
      return (overlappingSpans == null) ? false : overlappingSpans.size() == 1;
    }

    public Span getSingleOverlappingSpan() {
      return (overlappingSpans != null && overlappingSpans.size() == 1) ? overlappingSpans.iterator().next() : null;
    }
  }

  public static class SimpleSpan implements Span {

    private int startIdx;
    private int endIdx;

    public SimpleSpan(int startIdx, int endIdx) {
      this.startIdx = startIdx;
      this.endIdx = endIdx;
    }

    public int getStartIdx() {
      return startIdx;
    }

    public int getEndIdx() {
      return endIdx;
    }

    /** A SimpleSpan agrees with all other spans */
    public boolean agreesWith(Span other) {
      return true;
    }

    public int compareTo(Span other) {
      int result = (this.startIdx - other.getStartIdx());

      if (result == 0) {
        result = this.endIdx - other.getEndIdx();
      }

      return result;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);

      if (!result && other instanceof Span) {
        final Span otherSpan = (Span)other;
        result = (this.startIdx == otherSpan.getStartIdx() && this.endIdx == otherSpan.getEndIdx());
      }

      return result;
    }

    public int hashCode() {
      int result = 13;

      result = result * 13 + startIdx;
      result = result * 13 + endIdx;

      return result;
    }
  }
}
