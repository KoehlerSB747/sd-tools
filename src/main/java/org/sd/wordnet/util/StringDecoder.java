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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility to decode lexicographer strings.
 * <p>
 * @author Spencer Koehler
 */
public class StringDecoder {
  
  public static final Map<Character, Character> BRACKETS = new HashMap<Character, Character>();
  static {
    BRACKETS.put('{', '}');
    BRACKETS.put('[', ']');
    BRACKETS.put('(', ')');
  }


  private String lexString;
  private TreeMap<Integer, SegmentInfo> segments;
  private SegmentInfo segmentInfo;

  public StringDecoder(String lexString) {
    this.lexString = lexString;
    this.segments = analyze(lexString);
    this.segmentInfo = null;

    if (segments.size() > 0) {
      this.segmentInfo = segments.firstEntry().getValue();
    }
  }

  public String getLexString() {
    return lexString;
  }

  public boolean hasSegmentInfo() {
    return segmentInfo != null;
  }

  public SegmentInfo getSegmentInfo() {
    return segmentInfo;
  }

  TreeMap<Integer, SegmentInfo> getSegments() {
    return segments;
  }

  private final TreeMap<Integer, SegmentInfo> analyze(String lexString) {
    final TreeMap<Integer, SegmentInfo> result = new TreeMap<Integer, SegmentInfo>();

    final LinkedList<SegmentInfo> segmentStack = new LinkedList<SegmentInfo>();
    
    final int len = lexString.length();
    char prevC = (char)0;
    boolean startedBracketedSegment = false;
    for (int pos = 0; pos < len; ++pos) {
      final char c = lexString.charAt(pos);
      final Character e = BRACKETS.get(c);

      if (e != null) {
        // at the start of a bracket 'c' that will end as 'e'
        segmentStack.addFirst(new SegmentInfo(c, e, pos));
        startedBracketedSegment = true;
      }
      else if (c == ' ') {
        // end of a word
        if (segmentStack.size() > 0 && !startedBracketedSegment) {
          final SegmentInfo curSegmentInfo = segmentStack.getFirst();
          if (!curSegmentInfo.isBracketed()) {
            segmentStack.removeFirst();
            curSegmentInfo.setEndPos(pos);
            if (!curSegmentInfo.isEmpty()) {
              result.put(curSegmentInfo.getStartPos(), curSegmentInfo);
            }
          }
        }
      }
      else {
        int endThrough = -1;
        for (int i = 0; i < segmentStack.size(); ++i) {
          final SegmentInfo curSegmentInfo = segmentStack.get(i);
          if (curSegmentInfo.isEnd(c)) {
            // at the end of the current segment's bracket started by 'c'
            endThrough = i;
            break;
          }
          // else, content belongs in current segment(s)
        }

        if (endThrough >= 0) {
          // pop all ending segments off the stack
          for (int i = 0; i <= endThrough; ++i) {
            final SegmentInfo curSegmentInfo = segmentStack.removeFirst();
            curSegmentInfo.setEndPos(i < endThrough ? pos - 1 : pos);
            if (!curSegmentInfo.isEmpty()) {
              result.put(curSegmentInfo.getStartPos(), curSegmentInfo);
            }
          }
          startedBracketedSegment = false;
        }
        else if (prevC == ' ' || startedBracketedSegment) {
          // start of a word segment
          segmentStack.addFirst(new SegmentInfo(c, (char)0, pos));
          startedBracketedSegment = false;
        }
        else if (prevC == ',' && Character.isLetter(c)) {
          // start of a new word segment, end of previous (split)
          final SegmentInfo curSegmentInfo = segmentStack.removeFirst();
          curSegmentInfo.setEndPos(pos - 1);
          if (!curSegmentInfo.isEmpty()) {
            result.put(curSegmentInfo.getStartPos(), curSegmentInfo);
          }
          segmentStack.addFirst(new SegmentInfo(c, (char)0, pos));
        }
      }
      prevC = c;
    }

    return result;
  }


  public final class SegmentInfo {
    private char startChar;
    private char endChar;
    private int startPos;
    private int endPos;

    SegmentInfo(char startChar, char endChar, int startPos) {
      this.startChar = startChar;
      this.endChar = endChar;
      this.startPos = startPos;
      this.endPos = -1;
    }

    private boolean isEnd(char endChar) {
      return this.endChar == endChar;
    }

    private void setEndPos(int endPos) {
      this.endPos = endPos;
    }

    public boolean isEmpty() {
      final String allText = getAllText();
      final int emptySize = isBracketed() ? 2 : 0;
      return allText.length() <= emptySize;
    }

    public boolean isBracketed() {
      return endChar > (char)0;
    }

    public char getStartChar() {
      return startChar;
    }

    public char getLastChar() {
      char result = (char)0;
      int pos = endPos;

      while (pos > startPos) {
        result = lexString.charAt(pos--);
        if (result != ' ') break;
      }

      return result;
    }

    public int getStartPos() {
      return startPos;
    }

    public int getEndPos() {
      return endPos;
    }

    public String getAllText() {
      return endPos <= startPos ? "" : lexString.substring(startPos, endPos + 1).trim();
    }

    public String getInnerText() {
      String result = null;

      if (endPos > startPos) {
        if (isBracketed()) {
          result = lexString.substring(startPos + 1, endPos).trim();
        }
        else {
          result = lexString.substring(startPos, endPos + 1).trim();
        }
      }

      return result == null ? "" : result;
    }

    public boolean hasInnerSegment() {
      boolean result = false;

      final Map.Entry<Integer, SegmentInfo> nextSegmentEntry = segments.higherEntry(startPos);
      if (nextSegmentEntry != null && this.endPos > nextSegmentEntry.getValue().getEndPos()) {
        result = true;
      }

      return result;
    }

    public SegmentInfo getFirstInnerSegment() {
      SegmentInfo result = null;

      final Map.Entry<Integer, SegmentInfo> nextSegmentEntry = segments.higherEntry(startPos);
      if (nextSegmentEntry != null && this.endPos > nextSegmentEntry.getValue().getEndPos()) {
        result = nextSegmentEntry.getValue();
      }

      return result;
    }

    public List<SegmentInfo> getInnerSegments() {
      List<SegmentInfo> result = null;

      for (Map.Entry<Integer, SegmentInfo> nextSegmentEntry = segments.higherEntry(this.startPos);
           nextSegmentEntry != null && this.endPos > nextSegmentEntry.getValue().getEndPos();
           nextSegmentEntry = segments.higherEntry(nextSegmentEntry.getValue().getEndPos())) {
        if (result == null) result = new ArrayList<SegmentInfo>();
        result.add(nextSegmentEntry.getValue());
      }

      return result;
    }

    public boolean hasNextSegment() {
      boolean result = false;

      if (this.endPos > this.startPos) {
        final Map.Entry<Integer, SegmentInfo> nextSegmentEntry = segments.higherEntry(endPos);
        if (nextSegmentEntry != null) {
          result = true;
        }
      }

      return result;
    }

    public SegmentInfo getNextSegment() {
      SegmentInfo result = null;

      if (this.endPos > this.startPos) {
        final Map.Entry<Integer, SegmentInfo> nextSegmentEntry = segments.higherEntry(endPos);
        if (nextSegmentEntry != null) {
          result = nextSegmentEntry.getValue();
        }
      }

      return result;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(startPos).append('-').append(endPos).append(':').append(getAllText());
      return result.toString();
    }
  }


  public static void main(String[] args) {
    final String x =
      (args.length == 0)
      ?
      "{ [ darter, verb.motion:dart2,+ ] noun.Tops:animal,@ (a person or other animal that moves abruptly and rapidly; \"squirrels are darters\") }"
      :
      args[0];

    final StringDecoder decoder = new StringDecoder(x);
    final boolean stopHere = true;
    System.out.println(decoder.segments);
  }
}
