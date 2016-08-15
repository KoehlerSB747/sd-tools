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


import java.util.ArrayList;
import java.util.List;

/**
 * Container for a sequence of word net tokens.
 * <p>
 * @author Spencer Koehler
 */
public class TokenSequence {

  private List<WordNetToken> tokens;

  public TokenSequence(List<WordNetToken> tokens) {
    this.tokens = (tokens == null) ? new ArrayList<WordNetToken>() : new ArrayList<WordNetToken>(tokens);
  }
  
  public int size() {
    return (tokens == null) ? 0 : tokens.size();
  }

  public List<WordNetToken> getTokens() {
    return tokens;
  }

  public TokenSequence add(WordNetToken token) {
    this.tokens.add(token);
    return this;
  }

  public TokenSequence addAll(List<WordNetToken> tokens) {
    this.tokens.addAll(tokens);
    return this;
  }

  public TokenSequence addAll(TokenSequence tokenseq) {
    this.tokens.addAll(tokenseq.tokens);
    return this;
  }
}
