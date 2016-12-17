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
package org.sd.token;


import java.util.Map;

/**
 * Simple implementation of the TokenInfo class.
 * <p>
 * @author Spencer Koehler
 */
public class SimpleTokenInfo extends TokenInfo {
  
  private Map<String, String> attributes;

  public SimpleTokenInfo(int tokenStart, int tokenEnd, int priority, String category, Map<String, String> attributes) {
    super(tokenStart, tokenEnd, priority, category);
    this.attributes = attributes;
  }

  public void addTokenFeatures(Token token, Object source) {

    // Add the matched grammar rule's category as a token feature
    // (NOTE: this feature is used by AtnState.tokenMatchesStepCategory to
    //        identify a token match and allows pre-defined tokens to
    //        behave as though they matched a grammar rule)
    final String category = getCategory();
    if (category != null && !"".equals(category)) {
      token.setFeature(category, new Boolean(true), source);
    }

    if (attributes != null) {
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        token.setFeature(entry.getKey(), entry.getValue(), source);
      }
    }
  }
}
