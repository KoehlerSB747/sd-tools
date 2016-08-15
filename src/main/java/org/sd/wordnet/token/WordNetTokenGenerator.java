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


import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Token;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Utility to generate WordNetToken instances.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetTokenGenerator {
  
  private LexDictionary dict;
  private StandardTokenizerOptions options;

  public WordNetTokenGenerator(LexDictionary dict) {
    this(dict, null);
  }

  public WordNetTokenGenerator(LexDictionary dict, StandardTokenizerOptions options) {
    this.dict = dict;
    this.options = TokenUtil.buildTokenizerOptions(dict, options);
  }

  public void generate(TokenHandler handler, String line, WordLookupStrategy lookupStrategy) {
    if (line == null) return;
    line = line.trim();
    if ("".equals(line)) return;

    final StandardTokenizer tokenizer = StandardTokenizerFactory.getTokenizer(line, options);
    WordNetToken wnToken = null;

    for (Token primaryToken = tokenizer.getToken(0); primaryToken != null; primaryToken = tokenizer.getNextToken(primaryToken)) {
      String text = primaryToken.getText();
      String norm = NormalizeUtil.normalizeForLookup(text);
      wnToken = lookupStrategy.lookup(text, norm);
      if (wnToken != null) wnToken.setToken(primaryToken);
      if (wnToken == null || wnToken.isUnknown()) {
        for (Token revisedToken = tokenizer.revise(primaryToken); revisedToken != null; revisedToken = tokenizer.revise(revisedToken)) {
          primaryToken = revisedToken;
          text = revisedToken.getText();
          norm = NormalizeUtil.normalizeForLookup(text);
          wnToken = lookupStrategy.lookup(text, norm);
          if (wnToken != null) wnToken.setToken(revisedToken);
          if (wnToken != null && wnToken.hasCategories()) {
            // add defined token to handler
            handler.handle(wnToken);
            wnToken = null;
            break;
          }
        }
        if (wnToken != null) {
          // add undefined token to handler
          handler.handle(wnToken);
          wnToken = null;
        }
      }
      else {
        // add defined token to handler
        handler.handle(wnToken);
        wnToken = null;
      }
    }
    if (wnToken != null) {
      // add unmanaged defined or not wnToken
      handler.handle(wnToken);
    }
  }
}
