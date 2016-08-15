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


import java.util.HashMap;
import java.util.Map;
import org.sd.token.Token;
import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerOptions;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Implementation of the Tokenizer interface linked with WordNet info.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetTokenizer extends StandardTokenizer {
  
  private LexDictionary dict;
  private WordLookupStrategy lookupStrategy;
  private Map<Integer, Token> pos2token;

  public WordNetTokenizer(LexDictionary dict, WordLookupStrategy lookupStrategy, String text) {
    this(dict, text, null);
  }

  public WordNetTokenizer(LexDictionary dict, String text, StandardTokenizerOptions options) {
    super(text, TokenUtil.buildTokenizerOptions(dict, options));
    this.dict = dict;
    this.pos2token = new HashMap<Integer, Token>();
  }

  public LexDictionary getLexDictionary() {
    return dict;
  }

  /**
   * Get the token that starts at the given position. To get the first
   * token, use GetToken(0).
   *
   * @return The token at the position or null.
   */
  public Token getToken(int startPosition) {
    Token result = pos2token.get(startPosition);

    if (result != null) {
      // use cached result, if available
      return result;
    }

    // Get the token, but automatically revise until either the text is defined
    // or we've reached the last revision. The resulting token should behave as
    // if though it is the first revision.

    final Token standardToken = super.getToken(startPosition);
    result = narrowToDefined(standardToken, 0, 0);

    if (result != null) {
      // cache the result to re-use next time
      pos2token.put(startPosition, result);
    }

    return result;
  }

  /**
   * Revise the token if possible.
   *
   * @return A revised token or null.
   */
  public Token revise(Token token) {
    final Token standardToken = super.revise(token);
    final Token result = narrowToDefined(standardToken, token.getSequenceNumber(), token.getRevisionNumber() + 1);
    return result;
  }

  /**
   * Get the next token after the given token if possible.
   *
   * @return The next token or null.
   */
  public Token getNextToken(Token token) {
    int startPosition = super.findEndBreakForward(token.getEndIndex(), false);
    if (startPosition < 0) startPosition = token.getEndIndex();
    final Token result = this.getToken(startPosition);  // utilize cache
    if (result != null) {
      // update sequence number
      result.setSequenceNumber(token.getSequenceNumber() + 1);
    }
    return result;
  }

  protected void addTokenFeatures(Token token) {
    // if the wnToken feature doesn't exist, create and add it for this token
//...todo: I'm here
  }

  private final Token narrowToDefined(Token standardToken, int seqNum, int revNum) {
    Token result = standardToken;
    WordNetToken wnToken = null;

    if (standardToken != null) {
      String text = standardToken.getText();
      String norm = NormalizeUtil.normalizeForLookup(text);

      if (lookupStrategy == null) {
        wnToken = new WordNetToken().setInput(text).setNorm(norm);
        wnToken.setToken(standardToken);
      }
      else {
        wnToken = lookupStrategy.lookup(text, norm);
        if (wnToken != null) wnToken.setToken(standardToken);
        if (wnToken == null || wnToken.isUnknown()) {
          // need to revise to find a defined token
          for (Token revisedToken = super.revise(standardToken); revisedToken != null; revisedToken = super.revise(revisedToken)) {
            text = revisedToken.getText();
            norm = NormalizeUtil.normalizeForLookup(text);
            wnToken = lookupStrategy.lookup(text, norm);
            if (wnToken != null) wnToken.setToken(revisedToken);
            if (wnToken != null && wnToken.hasCategories()) {
              // found defined token -- can stop revising now
              break;
            }
          }
        }
      }

      // Update token with correct seqNum, revNum, and features
      if (wnToken != null) {
        final Token theToken = wnToken.getToken();
        if (theToken != null) {
          theToken.setRevisionNumber(revNum);
          theToken.setSequenceNumber(seqNum);
          result = theToken;
        }

        // stow wnToken as a feature on theToken
//...todo: I'm here...
      }
    }

    return result;
  }
}
