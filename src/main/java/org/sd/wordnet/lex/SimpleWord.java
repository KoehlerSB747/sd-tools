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


import org.sd.wordnet.util.NormalizeUtil;

/**
 * Container for a simple word from a lexicographer file.
 * <p>
 * @author Spencer Koehler
 */
public class SimpleWord {
  
  private String formattedWord;
  private String word;
  private String marker;
  private int lexId;
  private int spaceCount;

  public SimpleWord() {
    this.formattedWord = null;
    this.word = null;
    this.marker = null;
    this.lexId = 0;
    this.spaceCount = 0;
  }

  public SimpleWord(String formattedWord) {
    this();
    
    // word (marker)? lex_id?,
    this.setWord(formattedWord);
  }

  public String getFormattedWord() {
    return formattedWord;
  }

  public String getName() {
    final StringBuilder result = new StringBuilder();
    if (word != null) {
      result.append(word);
    }
    if (lexId > 0) {
      result.append(lexId);
    }
    return result.toString();
  }

  public String getNormalizedWord() {
    String result = null;

    if (word != null) {
      result = word.toLowerCase();
    }

    return result == null ? "" : result;
  }

  public boolean hasWord() {
    return word != null && !"".equals(word);
  }

  public String getWord() {
    return word;
  }

  public final void setWord(String word) {
    this.formattedWord = word;
    this.word = fixWord(word);
  }

  private final String fixWord(String word) {
    this.spaceCount = 0;
    if (word != null && !"".equals(word)) {
      int lastPos = word.length() - 1;
      char lastChar = word.charAt(lastPos);
      if (lastChar == ',') {
        //word = word.substring(0, lastPos);
        --lastPos;
        lastChar = word.charAt(lastPos);
      }
      //NOTE: sometimes the lexId is after the parens
      if (Character.isDigit(lastChar)) {
        final StringBuilder lexIdString = new StringBuilder();
        lexIdString.append(lastChar);
        while (--lastPos >= 0) {
          lastChar = word.charAt(lastPos);
          if (Character.isDigit(lastChar)) {
            lexIdString.insert(0, lastChar);
          }
          else {
            break;
          }
        }
        this.lexId = Integer.parseInt(lexIdString.toString());
        //word = word.substring(0, lastPos + 1);
      }
      if (lastChar == ')') {
        final int lpPos = word.lastIndexOf('(', lastPos - 1);
        if (lpPos >= 0) {
          this.marker = word.substring(lpPos, lastPos + 1);
          //word = word.substring(0, lpPos);
          lastPos = lpPos - 1;
          lastChar = word.charAt(lastPos);
        }
      }
      //NOTE: sometimes the lexId is before the parens
      if (Character.isDigit(lastChar)) {
        final StringBuilder lexIdString = new StringBuilder();
        lexIdString.append(lastChar);
        while (--lastPos >= 0) {
          lastChar = word.charAt(lastPos);
          if (Character.isDigit(lastChar)) {
            lexIdString.insert(0, lastChar);
          }
          else {
            break;
          }
        }
        this.lexId = Integer.parseInt(lexIdString.toString());
        //word = word.substring(0, lastPos + 1);
      }

      // squash '"', replace '-', '_' w/space,
      // leave other delims ['/.] when between chars, otherwise squash
      // trim "'s" at end of string
      word = NormalizeUtil.normalizeForLookup(word, lastPos + 1);
    }
    return word;
  }

  public boolean hasMarker() {
    return marker != null && !"".equals(marker);
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(String marker) {
    this.marker = marker;
  }

  public int getLexId() {
    return lexId;
  }

  public void setLexId(int lexId) {
    this.lexId = lexId;
  }

  public int getSpaceCount() {
    return spaceCount;
  }
}
