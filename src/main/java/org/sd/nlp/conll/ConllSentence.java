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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.Tree;


/**
 * Container for a CoNLL formatted sentence.
 * <p>
 * @author Spencer Koehler
 */
public class ConllSentence {
  
  private List<ConllToken> tokens;
  private Tree<ConllNodeData> _tree;
  private Map<Integer, ConllToken> _id2token;
    
  public ConllSentence() {
    this.tokens = new ArrayList<ConllToken>();
    this._tree = null;
    this._id2token = null;
  }

  public int size() {
    return tokens.size();
  }

  public void addTokenLine(String tokenLine) {
    addToken(new ConllToken(tokenLine));
  }

  public void addToken(ConllToken token) {
    this.tokens.add(token);
  }

  public List<ConllToken> getTokens() {
    return tokens;
  }

  public Map<Integer, ConllToken> getId2TokenMap() {
    if (_id2token == null) {
      _id2token = buildId2TokenMap();
    }
    return _id2token;
  }

  private final Map<Integer, ConllToken> buildId2TokenMap() {
    final Map<Integer, ConllToken> result = new HashMap<Integer, ConllToken>();

    for (ConllToken token : tokens) {
      result.put(token.getId(), token);
    }

    return result;
  }

  public Tree<ConllNodeData> asTree() {
    if (_tree == null) {
      _tree = buildTree();
    }
    return _tree;
  }

  private final Tree<ConllNodeData> buildTree() {
    final Tree<ConllNodeData> result = new Tree<ConllNodeData>(new ConllNodeData("TOP"));

    final List<ConllToken> headTokens = getAllTokens(ConllField.HEAD, "0");
    if (headTokens != null) {
      for (ConllToken headToken : headTokens) {
        addChild(result, headToken);
      }
    }
    else {
      for (ConllToken token : tokens) {
        result.addChild(new ConllNodeData(token));
      }
    }

    return result;
  }
  
  private final void addChild(Tree<ConllNodeData> parent, ConllToken childToken) {
    final Tree<ConllNodeData> childNode = parent.addChild(new ConllNodeData(childToken));
    final List<ConllToken> tokens = getAllTokens(ConllField.HEAD, childToken.getData(ConllField.ID));
    if (tokens != null) {  // recurse
      for (ConllToken token : tokens) {
        addChild(childNode, token);
      }
    }
  }

  public ConllToken getFirstToken(ConllField field, String value) {
    ConllToken result = null;

    for (ConllToken token : tokens) {
      if (token.matches(field, value)) {
        result = token;
        break;
      }
    }

    return result;
  }

  public List<ConllToken> getAllTokens(ConllField field, String value) {
    List<ConllToken> result = null;

    for (ConllToken token : tokens) {
      if (token.matches(field, value)) {
        if (result == null) result = new ArrayList<ConllToken>();
        result.add(token);
      }
    }

    return result;
  }

  public ConllToken getPriorToken(ConllToken token) {
    ConllToken result = null;

    if (token != null) {
      final int curid = token.getId();  //NOTE: 1-based
      if (curid > 1) {
        //NOTE: assuming tokens are always 1..N
        result = tokens.get(curid - 2);
      }
    }

    return result;
  }

  public ConllToken getNextToken(ConllToken token) {
    ConllToken result = null;

    if (token != null) {
      final int curid = token.getId();  //NOTE: 1-based
      if (curid < tokens.size()) {
        //NOTE: assuming tokens are always 1..N
        result = tokens.get(curid);
      }
    }

    return result;
  }

  public String getText() {
    final StringBuilder result = new StringBuilder();

    for (ConllToken token : tokens) {
      if (result.length() > 0) result.append(' ');
      result.append(token.getText());
    }

    return result.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (ConllToken token : tokens) {
      if (result.length() > 0) result.append('\n');
      result.append(token);
    }

    return result.toString();
  }
}
