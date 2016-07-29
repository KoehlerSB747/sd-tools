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
package org.sd.wordnet.rel;


import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.sd.wordnet.lex.PointerDefinition;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.util.FormatHelper;

/**
 * Container for relationship information between two words.
 * <p>
 * @author Spencer Koehler
 */
public class WordRelation implements Comparable<WordRelation> {
  
  private String common;
  private List<ExpandedWord.PointerData> path1;  // path from word1 to common
  private List<ExpandedWord.PointerData> path2;  // path from word2 to common
  
  public WordRelation(String common, List<ExpandedWord.PointerData> path1, List<ExpandedWord.PointerData> path2) {
    this.common = common;
    this.path1 = path1;
    this.path2 = path2;
  }

  public String getCommon() {
    return common;
  }

  public List<ExpandedWord.PointerData> getPath1() {
    return path1;
  }

  public List<ExpandedWord.PointerData> getPath2() {
    return path2;
  }

  public String buildString(FormatHelper<Word> wordFmt, boolean htmlEscape) {
    final StringBuilder result = new StringBuilder();

    // common
    //   <-- ptrSym -- word <-- ptrSym -- ... -- word1 [path1]
    //   <-- ptrSym -- word <-- ptrSym -- ... -- word2 [path2]
    if (wordFmt == null) {
      result.append(common);
    }
    else {
      // last word is the same as common, by definition
      result.append(wordFmt.format(path1.get(path1.size() - 1).word));
    }

    result.
      append("\n ").
      append(buildPathString("path1", path1, wordFmt, htmlEscape)).
      append("\n ").
      append(buildPathString("path2", path2, wordFmt, htmlEscape));

    return result.toString();
  }

  private final StringBuilder buildPathString(String pathLabel,
                                              List<ExpandedWord.PointerData> path,
                                              FormatHelper<Word> wordFmt,
                                              boolean htmlEscape) {
    final StringBuilder builder = new StringBuilder();
    PointerDefinition ptrDef = null;
    final String arrow = buildArrow(htmlEscape);

    final int lastIdx = path.size() - 1;
    for (int i = lastIdx; i >= 0; --i) {
      final ExpandedWord.PointerData ptrData = path.get(i);

      // NOTE: last entry leads to common and we just need to grab its ptrDef
      if (i < lastIdx) {
        // " <-- lastPtr -- thisWord"
        // NOTE: first entry is "root" and doesn't have a ptrDef
        if (ptrDef != null) {
          builder.append(' ').append(arrow);
          if (htmlEscape) {
            builder.append(StringEscapeUtils.escapeHtml(ptrDef.getPointerSymbol()));
          }
          else {
            builder.append(ptrDef.getPointerSymbol());
          }
          builder.append(" --");
        }
        if (ptrData.word != null) {
          builder.append(' ');
          if (wordFmt == null) {
            builder.append(ptrData.word.getQualifiedWordName());
          }
          else {
            builder.append(wordFmt.format(ptrData.word));
          }
        }
      }

      ptrDef = ptrData.sourcePtr;
    }

    // add "[pathLabel]" to the end
    if (builder.length() > 0) builder.append(' ');
    builder.
      append('[').
      append(pathLabel).
      append('(').
      append(path.size()).
      append(')').
      append(']');
    
    return builder;
  }

  private final String buildArrow(boolean htmlEscape) {
    final StringBuilder result = new StringBuilder();

    //  "<-- "

    if (htmlEscape) {
      result.append("&lt;");
    }
    else {
      result.append("<");
    }

    result.append("-- ");

    return result.toString();
  }

  public String toString() {
    return buildString(null, false);
  }

  public int compareTo(WordRelation other) {

    // that with smaller overall path size comes first
    final int mySize = path1.size() + path2.size();;
    final int otherSize = other.path1.size() + other.path2.size();

    int result = mySize - otherSize;

    if (result == 0) {
      // that with smaller min path size comes first
      final int myMinSize = Math.min(path1.size(), path2.size());
      final int otherMinSize = Math.min(other.path1.size(), other.path2.size());

      result = myMinSize - otherMinSize;
      if (result == 0) {
        // that with smaller max path size comes first
        final int myMaxSize = Math.max(path1.size(), path2.size());
        final int otherMaxSize = Math.max(other.path1.size(), other.path2.size());

        result = myMaxSize - otherMaxSize;

        if (result == 0) {
          // alphabetize by common word
          final int cPos1 = common.indexOf(':');
          final int cPos2 = other.common.indexOf(':');

          result = common.substring(cPos1 + 1).compareTo(other.common.substring(cPos2 + 1));
        }
      }
    }

    return result;
  }

  public boolean equals(Object other) {
    boolean result = (this == other);

    if (!result && other != null && other instanceof WordRelation) {
      final WordRelation otherWordRelation = (WordRelation)other;
      if (this.common.equals(otherWordRelation.common) &&
          this.path1.size() == otherWordRelation.path1.size() &&
          this.path2.size() == otherWordRelation.path2.size()) {

        result = true;

        for (int i = 0; result && i < path1.size(); ++i) {
          final ExpandedWord.PointerData myPD = path1.get(i);
          final ExpandedWord.PointerData otherPD = otherWordRelation.path1.get(i);
          result = myPD.equals(otherPD);
        }

        for (int i = 0; result && i < path2.size(); ++i) {
          final ExpandedWord.PointerData myPD = path2.get(i);
          final ExpandedWord.PointerData otherPD = otherWordRelation.path2.get(i);
          result = myPD.equals(otherPD);
        }
      }
    }

    return result;
  }

  public int hashCode() {
    int result = 11;

    if (common != null) result = result * 11 + common.hashCode();
    if (path1 != null) result = result * 11 + path1.hashCode();
    if (path2 != null) result = result * 11 + path2.hashCode();

    return result;
  }
}
