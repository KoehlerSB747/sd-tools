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


import org.sd.token.Token;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.SynsetContainer;

/**
 * Container for a token matched against word net.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetToken extends SynsetContainer {
  
  private Token token;

  public WordNetToken() {
    super();
  }

  public boolean hasToken() {
    return token != null;
  }

  public Token getToken() {
    return token;
  }

  public WordNetToken setToken(Token token) {
    this.token = token;
    return this;
  }
}
