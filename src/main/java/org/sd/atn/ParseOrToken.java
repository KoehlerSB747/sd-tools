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


import org.sd.token.Token;

/**
 * Container for either an AtnParse or a Token.
 * <p>
 * @author Spencer Koehler
 */
public class ParseOrToken {
  
  private AtnParse parse;
  private Token token;

  public ParseOrToken(AtnParse parse) {
    this.parse = parse;
    this.token = null;
  }

  public ParseOrToken(Token token) {
    this.parse = null;
    this.token = token;
  }

  public boolean hasParse() {
    return parse != null;
  }

  public AtnParse getParse() {
    return parse;
  }

  public boolean hasToken() {
    return token != null;
  }

  public Token getToken() {
    return token;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (parse != null) {
      result.append(parse.toString());
    }
    else {
      result.append(token.getDetailedString());
    }
    
    return result.toString();
  }
}
