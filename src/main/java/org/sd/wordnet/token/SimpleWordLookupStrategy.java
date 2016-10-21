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
package org.sd.wordnet.token;


import java.util.List;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;

/**
 * A simple WordNetLookupStrategy implementation.
 * <p>
 * @author Spencer Koehler
 */
public class SimpleWordLookupStrategy implements WordLookupStrategy {
  
  private LexDictionary dict;

  public SimpleWordLookupStrategy(LexDictionary dict) {
    this.dict = dict;
  }

  public LexDictionary getLexDictionary() {
    return dict;
  }

  /**
   * Create a WordNetToken by looking it up using the given dictionary.
   * <p>
   * @param dict  The word net dictionary to use
   * @param input  The input to look up
   * @param norm  The normalized input to look up
   *
   * @return a WordNetToken, with or without synsets and/or tags.
   */
  public WordNetToken lookup(String input, String norm) {
    final WordNetToken result = new WordNetToken();

    result.setInput(input).setNorm(norm).setSynsets(dict.lookupSynsets(norm));

    if (!result.hasSynsets()) {
      addTags(result, dict, input, norm);
    }

    return result;
  }

  /**
   * Given that no synsets were found for the input (norm) through the dict,
   * attempt to add tags to define the given token result.
   */
  protected void addTags(WordNetToken result, LexDictionary dict, String input, String norm) {
    // check for number format and tag with "Numeric"
    if (isNumeric(input)) {
      result.addTag("Numeric");
    }
    // check for ProperNoun format and tag with "ProperNoun"
    else if (isProperNoun(dict, input, norm)) {
      result.addTag("ProperNoun");
    }
  }

  /**
   * Determine whether the input is numeric.
   *
   * @param input  the input to test
   *
   * @return true if the input is numeric.
   */
  protected boolean isNumeric(String input) {
    if ("".equals(input)) return false;

    boolean result = true;
    final int len = input.length();

    for (int i = 0; i < len; ++i) {
      final char c = input.charAt(i);
      if (Character.isLetter(c)) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether the input is a proper noun.
   *
   * @param dict  the wordnet dictionary to use
   * @param multiword  the input to test
   * @param norm  the normalized input
   *
   * @return true if the input is numeric.
   */
  protected boolean isProperNoun(LexDictionary dict, String multiword, String norm) {
    boolean result = false;
    boolean sawCap = false;

    final String[] words = multiword.split("\\s+");

    if (words.length > 1 && dict != null) {
      // Account for the case where the first word is capitalized because it
      // starts a sentence
      final int spos = norm.indexOf(' ');
      if (spos > 0) {
        final String firstnorm = norm.substring(0, spos);
        final List<Synset> synsets = dict.lookupSynsets(firstnorm);
        if (synsets != null && synsets.size() > 0 && !hasPersonSynset(synsets)) {
          return result;  // false -- detected non-person word at start of input
        }
      }
    }

    // searching backwards as a performance ploy
    for (int w = words.length - 1; w >= 0; --w) {
      result = false;
      final String word = words[w];
      final int len = word.length();

      if (len > 1) {
        final char first = word.charAt(0);
        if (Character.isUpperCase(first)) {
          sawCap = true;
          for (int i = 1; i < len; ++i) {
            final char c = word.charAt(i);
            if (c == '-') continue;
            if (Character.isLowerCase(c)) {
              result = true;
              break;
            }
            else {
              break;
            }
          }
        }
        else if (len == 2 && w > 0 && (w + 1) < words.length && "of".equals(word)) {
          result = true;
        }
      }

      if (!result) {
        break;
      }
    }

    if (!result && words.length == 1 && sawCap) {
      // consider a single all-caps word to be a ProperNoun
      result = true;

      final String word = words[0];
      final int len = word.length();
      for (int i = 1; i < len; ++i) {
        final char c = word.charAt(i);
        if (Character.isLowerCase(c)) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  protected static boolean hasPersonSynset(List<Synset> synsets) {
    boolean result = false;

    if (synsets != null) {
      for (Synset synset : synsets) {
        if ("noun.person".equals(synset.getLexFileName())) {
          for (Word word : synset.getWords()) {
            final String wordName = word.getWordName();
            if (wordName != null && !"".equals(wordName) && Character.isUpperCase(wordName.charAt(0))) {
              result = true;
              break;
            }
          }
        }
        if (result) break;
      }
    }

    return result;
  }
}
