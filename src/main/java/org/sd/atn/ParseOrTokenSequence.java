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
package org.sd.atn;


import java.util.ArrayList;
import java.util.List;
import org.sd.token.Token;

/**
 * Container for a sequence of ParseOrToken instances.
 * <p>
 * @author Spencer Koehler
 */
public class ParseOrTokenSequence {
  
  private List<ParseOrToken> sequence;

  public ParseOrTokenSequence() {
    this.sequence = new ArrayList<ParseOrToken>();
  }

  public List<ParseOrToken> getSequence() {
    return sequence;
  }

  public void add(ParseOrToken parseOrToken) {
    this.sequence.add(parseOrToken);
  }

  public void add(AtnParse parse) {
    this.sequence.add(new ParseOrToken(parse));
  }

  public void add(Token token) {
    this.sequence.add(new ParseOrToken(token));
  }

  public boolean hasParses() {
    boolean result = false;

    for (ParseOrToken item : sequence) {
      if (item.hasParse()) {
        result = true;
        break;
      }
    }

    return result;
  }

  public boolean hasTokens() {
    boolean result = false;

    for (ParseOrToken item : sequence) {
      if (item.hasToken()) {
        result = true;
        break;
      }
    }

    return result;
  }

  public List<AtnParse> getParses() {
    final List<AtnParse> result = new ArrayList<AtnParse>();

    for (ParseOrToken item : sequence) {
      if (item.hasParse()) {
        result.add(item.getParse());
      }
    }

    return result;
  }

  public List<Token> getTokens() {
    final List<Token> result = new ArrayList<Token>();

    for (ParseOrToken item : sequence) {
      if (item.hasToken()) {
        result.add(item.getToken());
      }
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (sequence != null) {
      int num = 1;
      for (ParseOrToken item : sequence) {
        if (result.length() > 0) result.append("\n");
        result.append(num++).append('\t').append(item.toString());
      }
    }

    return result.toString();
  }
}
