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
import org.sd.token.Token;

/**
 * A TokenHandler for collecting token information.
 * <p>
 * @author Spencer Koehler
 */
public class TokenCollectionHandler implements TokenHandler {
  
  private List<WordNetToken> tokens;

  public TokenCollectionHandler() {
    this.tokens = new ArrayList<WordNetToken>();
  }

  public void handle(WordNetToken wnToken) {
    this.tokens.add(wnToken);
  }

  public List<WordNetToken> getTokens() {
    return tokens;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (WordNetToken token : tokens) {
      if (result.length() > 0) result.append("_");
      result.append(token.toString());
    }

    return result.toString();
  }
}
