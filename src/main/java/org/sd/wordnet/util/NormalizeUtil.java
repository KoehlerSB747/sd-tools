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


/**
 * Utilities for normalizing strings for WordNet.
 * <p>
 * @author Spencer Koehler
 */
public class NormalizeUtil {
  
  public static final String normalizeForLookup(String input) {
    return normalizeForLookup(input, (input == null) ? 0 : input.length());
  }

  public static final String normalizeForLookup(String input, int len) {
    final StringBuilder result = new StringBuilder();

    if (input != null && !"".equals(input)) {
      char lastC = (char)0;
      char embedC = (char)0;
      boolean sawAPOS = false;
      for (int pos = 0; pos < len; ++pos) {
        char c = input.charAt(pos);

        // leave delims ['/.] if embedded; otherwise, replace w/space
        // replace all other delims with a space
        // squash/trim extra whitespace
        if (Character.isLetterOrDigit(c)) {
          if (lastC == ' ') {
            if (result.length() > 0) {
              result.append(' ');
            }
          }
          else if (embedC > 0) {
            result.append(embedC);
            embedC = (char)0;
          }
          result.append(Character.toLowerCase(c));
        }
        else {
          embedC = (char)0;
          if (c == '\'' || c == '/' || c == '.') {
            if (lastC != ' ') {
              embedC = c;
              if (c == '\'') sawAPOS = true;
            }
            else {
              c = ' ';
            }
          }
          else {
            c = ' ';
          }
        }
        lastC = c;
      }

      // trim off "'s" at end of string
      if (sawAPOS) {
        final int nlen = result.length();
        if (nlen > 2 && result.charAt(nlen - 2) == '\'' && result.charAt(nlen - 1) == 's') {
          result.setLength(result.length() - 2);
        }
      }
    }

    return result.toString();
  }
}
