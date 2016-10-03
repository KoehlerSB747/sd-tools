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
import java.util.Map;
import org.sd.nlp.NormalizedString;
import org.sd.text.TermFinder;

/**
 * Utilities for transforming text.
 * <p>
 * @author Spencer Koehler
 */
public class TransformUtil {
  
  public static final Map<String, String> XNT_EXPANSION_EXCEPTIONS = new HashMap<String, String>();
  static {
    XNT_EXPANSION_EXCEPTIONS.put("can't", "can not");
    XNT_EXPANSION_EXCEPTIONS.put("won't", "will not");
    XNT_EXPANSION_EXCEPTIONS.put("shan't", "shall not");
  }

  public static final Map<String, String> REPLACEMENTS = new HashMap<String, String>();
  static {
    REPLACEMENTS.put("cannot", "can not");
  }

  public static final TermFinder REPLACEMENT_TERM_FINDER =
    new TermFinder("WordNetReplacements", false,
                   new ArrayList<String>(REPLACEMENTS.keySet()).toArray(new String[REPLACEMENTS.size()]));

  /**
   * Apply transformation to the text to enhance lookups.
   * <p>
   * For example, expand contractions, etc.
   */
  public static final String applyTransformations(String text) {
    String result = text;

    // replace "Xn't" with "X not", controlling for exceptions
    result = replaceXnt(result);

    // replace terms
    result = replaceTerms(result);

    return result;
  }

  public static final String replaceTerms(String text) {
    String result = text;

    final NormalizedString input = REPLACEMENT_TERM_FINDER.normalize(text);
    final NormalizedString[] pieces = REPLACEMENT_TERM_FINDER.split(input, TermFinder.FULL_WORD, true);
    if (pieces != null && pieces.length > 1) {
      final StringBuilder builder = new StringBuilder();
      for (int i = 0; i < pieces.length; ++i) {
        if (pieces[i] == null) continue;
        final String piece = pieces[i].getOriginal().trim();
        if (builder.length() > 0) builder.append(' ');
        if ((i % 2) == 0) {
          builder.append(piece);
        }
        else {
          final String replacement = REPLACEMENTS.get(pieces[i].getNormalized());
          builder.append(replacement == null ? piece : replacement);
        }
      }
      result = builder.toString();
    }

    return result;
  }

  public static final String replaceXnt(String text) {
    String result = text;

    final int ntPos = text.indexOf("n't");
    if (ntPos >= 0 && isWordEndBoundary(text, ntPos + 3)) {
      final StringBuilder fixed = replaceXnt(text, 0, ntPos, null);
      result = fixed.toString();
    }

    return result;
  }

  static final StringBuilder replaceXnt(String text, int startPos, int ntPos, StringBuilder result) {
    if (result == null) result = new StringBuilder();

    final int prevSpacePos = ntPos == 0 ? -1 : text.lastIndexOf(' ', ntPos - 1);
    int wordStartPos = startPos;
    if (prevSpacePos >= startPos) {
      wordStartPos = prevSpacePos + 1;
    }

    if (wordStartPos > startPos) {
      // grab stuff from startPos up to the word
      result.append(text.substring(startPos, wordStartPos));
    }

    final int textlen = text.length();
    final int wordEndPos = ntPos + 3;

    // extract Xn't word
    final String xntWord = text.substring(wordStartPos, wordEndPos);

    // calculate and add expansion
    final String expansion = expandXnt(xntWord);
    result.append(expansion);

    // determine whether to recurse or end
    final int nextNtPos = (wordEndPos < textlen) ? text.indexOf("n't", wordEndPos) : -1;
    if (nextNtPos < 0 || !isWordEndBoundary(text, nextNtPos + 3)) {
      // end
      if (textlen > wordEndPos) {
        result.append(text.substring(wordEndPos, textlen));
      }
    }
    else {
      // recurse
      replaceXnt(text, wordEndPos, nextNtPos, result);
    }

    return result;
  }

  public static final String expandXnt(String xntWord) {
    String result = XNT_EXPANSION_EXCEPTIONS.get(xntWord.toLowerCase());

    if (result == null) {
      final int len = xntWord.length();
      final StringBuilder builder = new StringBuilder();
      builder.append(xntWord.substring(0, len - 3)).append(" not");
      result = builder.toString();
    }      

    return result;
  }

  public static final boolean isWordEndBoundary(String text, int pos) {
    boolean result = true;

    final int len = text.length();
    if (pos < len) {
      final char c = text.charAt(pos);
      if (Character.isLetterOrDigit(c)) {
        result = false;
      }
    }

    return result;
  }
}
