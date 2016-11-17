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


import java.util.Map;
import java.util.TreeMap;

/**
 * Helper to align positions in text reconstructed from a conll sentence
 * with the original input.
 * <p>
 * @author Spencer Koehler
 */
public class ConllSentenceAligner {
  
  //
  // The idea here is that the letters and digits will align (including case)
  // while symbols (including spaces) will not necessarily align.
  //
  // Also, there may be segments of conll text that map back to a symbol.
  // (e.g. "-LRB- " to "(" or " -RRB-" to ")")
  //

  private Pointer conllPointer;
  private Pointer origPointer;
  private boolean aligns;
  private TreeMap<Integer, int[]> conll2orig;
  private TreeMap<Integer, int[]> orig2conll;

  public ConllSentenceAligner(String conllSentence, String origSentence) {
    this.conllPointer = new Pointer(conllSentence);
    this.origPointer = new Pointer(origSentence);
    this.conll2orig = new TreeMap<Integer, int[]>();
    this.orig2conll = new TreeMap<Integer, int[]>();
    this.aligns = init();
  }

  public boolean aligns() {
    return aligns;
  }

  public String getOrigSentence() {
    return origPointer.text;
  }

  public String getConllSentence() {
    return conllPointer.text;
  }

  /**
   * Get the bounds of the token in the conll string corresponding
   * to the character at the specified position in the original string.
   */
  public int[] getConllPos(int origPos) {
    return doGetPos(orig2conll, origPos);
  }

  /**
   * Get the bounds of the token in the original string corresponding
   * to the character at the specified position in the conll string.
   */
  public int[] getOrigPos(int conllPos) {
    return doGetPos(conll2orig, conllPos);
  }

  private final int[] doGetPos(TreeMap<Integer, int[]> map, int pos) {
    int[] result = null;

    Map.Entry<Integer, int[]> entry = map.floorEntry(pos);
    if (entry != null) {
      result = entry.getValue();
    }
    else if (map.size() > 0) {
      // pos is between the start of the string and the first token
      entry = map.firstEntry();
      result = new int[]{0, entry.getValue()[0]};
    }

    return result;
  }

  public String getOrigText(int[] origPos) {
    return getText(conllPointer, origPos);
  }

  public String getConllText(int[] conllPos) {
    return getText(origPointer, conllPos);
  }

  private final String getText(Pointer pointer, int[] pos) {
    return pointer.text.substring(pos[0], pos[1]);
  }

  public int[] getPriorOrigToken(int origPos) {
    int[] result = null;

    final int[] conllPos = getConllPos(origPos);
    if (conllPos != null) {
      final Integer prevConll = conll2orig.lowerKey(conllPos[0]);
      if (prevConll != null) {
        result = getOrigPos(prevConll);
      }
    }
    
    return result;
  }

  public int[] getPriorConllToken(int conllPos) {
    int[] result = null;

    final int[] origPos = getOrigPos(conllPos);
    if (origPos != null) {
      final Integer prevOrig = orig2conll.lowerKey(origPos[0]);
      if (prevOrig != null) {
        result = getConllPos(prevOrig);
      }
    }
    
    return result;
  }

  public int[] getNextOrigToken(int origPos) {
    int[] result = null;

    final int[] conllPos = getConllPos(origPos);
    if (conllPos != null) {
      final Integer nextConll = conll2orig.higherKey(conllPos[0]);
      if (nextConll != null) {
        result = getOrigPos(nextConll);
      }
    }

    return result;
  }

  public int[] getNextConllToken(int conllPos) {
    int[] result = null;

    final int[] origPos = getOrigPos(conllPos);
    if (origPos != null) {
      final Integer nextOrig = orig2conll.higherKey(origPos[0]);
      if (nextOrig != null) {
        result = getConllPos(nextOrig);
      }
    }

    return result;
  }


  private final boolean init() {
    boolean result = true;

    while (conllPointer.hasNext() && origPointer.hasNext()) {
      final int conllPos = conllPointer.getIdx();
      final int origPos = origPointer.getIdx();
      final String conllToken = conllPointer.nextToken();
      final String origToken = origPointer.nextToken();

      if (conllToken != null && conllToken.equals(origToken)) {
        conll2orig.put(conllPos, new int[]{origPos, origPointer.getEndIdx()});
        orig2conll.put(origPos, new int[]{conllPos, conllPointer.getEndIdx()});
      }
      else {
        result = false;
        break;
      }
    }

    return result;
  }


  static final class Pointer {
    public final String text;
    public final int len;
    private int idx;
    private int endIdx;

    Pointer(String text) {
      this.text = text;
      this.len = (text == null) ? 0 : text.length();
      this.idx = 0;
      this.endIdx = -1;

      skipSymbols();
    }

    boolean hasNext() {
      return idx < len;
    }

    String nextToken() {
      if (idx >= len) return null;

      final StringBuilder result = new StringBuilder();

      for (; idx < len; ++idx) {
        final char cur = text.charAt(idx);
        if (Character.isLetterOrDigit(cur)) {
          result.append(cur);
        }
        else {
          break;
        }
      }
      
      this.endIdx = idx;
      skipSymbols();

      return result.toString();
    }

    int getIdx() {
      return idx;
    }

    int getEndIdx() {
      return endIdx;
    }

    private final boolean skipSymbols() {
      boolean result = false;

      for (; idx < len; ++idx) {
        final char cur = text.charAt(idx);
        if (Character.isLetterOrDigit(cur)) {
          result = true;
          break;
        }
        else if (cur == '-' && (idx + 4) < len) {
          // skip over -LRB- and -RRB- as a single symbol
          if ((text.charAt(idx + 4) == '-') &&
              (text.charAt(idx + 3) == 'B') &&
              (text.charAt(idx + 2) == 'R')) {
            final char LorR = text.charAt(idx + 1);
            if (LorR == 'L' || LorR == 'R') {
              idx += 4;
            }
          }
        }
      }

      return result;
    }
  }
}
