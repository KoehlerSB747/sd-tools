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


import org.sd.token.StandardTokenizerOptions;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Token utilities.
 * <p>
 * @author Spencer Koehler
 */
public class TokenUtil {
  
  private static final String[] DEFAULT_TOKENIZER_OPTIONS_ARGS = new String[] {
    "revisionStrategy=LS",
    "lowerUpperBreak=ZERO_WIDTH_SOFT_BREAK",
    "upperLowerBreak=NO_BREAK",
    "upperDigitBreak=NO_BREAK",
    "lowerDigitBreak=ZERO_WIDTH_SOFT_BREAK",
    "digitUpperBreak=NO_BREAK",
    "digitLowerBreak=NO_BREAK",
    "nonEmbeddedDoubleDashBreak=SINGLE_WIDTH_HARD_BREAK",
    "embeddedDoubleDashBreak=SINGLE_WIDTH_HARD_BREAK",
    "leftBorderedDashBreak=SINGLE_WIDTH_HARD_BREAK",
    "rightBorderedDashBreak=SINGLE_WIDTH_HARD_BREAK",
    "freeStandingDashBreak=SINGLE_WIDTH_HARD_BREAK",
    "whitespaceBreak=SINGLE_WIDTH_SOFT_BREAK",
    "quoteAndParenBreak=SINGLE_WIDTH_HARD_BREAK",
    "symbolBreak=SINGLE_WIDTH_HARD_BREAK",
    "repeatingSymbolBreak=SINGLE_WIDTH_HARD_BREAK",
    "slashBreak=SINGLE_WIDTH_SOFT_BREAK",
    "embeddedApostropheBreak=NO_BREAK",
    "embeddedPunctuationBreak=SINGLE_WIDTH_HARD_BREAK",
  };

  public static final StandardTokenizerOptions DEFAULT_TOKENIZER_OPTIONS =
    new StandardTokenizerOptions(new DataProperties(DEFAULT_TOKENIZER_OPTIONS_ARGS));

  public static final StandardTokenizerOptions buildTokenizerOptions(LexDictionary dict) {
    return buildTokenizerOptions(dict, null);
  }

  public static final StandardTokenizerOptions buildTokenizerOptions(LexDictionary dict, StandardTokenizerOptions baseOptions) {
    final StandardTokenizerOptions result = new StandardTokenizerOptions(baseOptions == null ? DEFAULT_TOKENIZER_OPTIONS : baseOptions);
    result.setTokenBreakLimit(dict.getMaxSpaceCount());
    return result;
  }

}
