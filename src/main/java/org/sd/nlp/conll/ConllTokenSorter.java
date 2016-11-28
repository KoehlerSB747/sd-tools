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
package org.sd.nlp.conll;


import java.util.List;
import java.util.TreeSet;

/**
 * Utility to sort ConllTokens into order.
 * <p>
 * @author Spencer Koehler
 */
public class ConllTokenSorter {
  
  private TreeSet<ConllToken> tokens;

  public ConllTokenSorter() {
    this.tokens = new TreeSet<ConllToken>();
  }

  public int size() {
    return tokens.size();
  }

  public ConllTokenSorter add(ConllToken token) {
    this.tokens.add(token);
    return this;
  }

  public ConllTokenSorter add(List<ConllToken> tokens) {
    this.tokens.addAll(tokens);
    return this;
  }

  public TreeSet<ConllToken> getTokens() {
    return tokens;
  }

  public String getText() {
    final StringBuilder result = new StringBuilder();
    for (ConllToken token : tokens) {
      final String tokenText = token.getText();
      if ("-RRB-".equals(tokenText)) result.append(") ");
      else if ("-LRB-".equals(tokenText)) result.append(" (");
      else {
        if (result.length() > 0) result.append(' ');
        result.append(token.getText());
      }
    }
    return result.toString();
  }
}
