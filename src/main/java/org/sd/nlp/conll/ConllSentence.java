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
  private StringBuilder text;
    
  public ConllSentence() {
    this.tokens = new ArrayList<ConllToken>();
    this._tree = null;
    this._id2token = null;
    this.text = new StringBuilder();
  }

  public int size() {
    return tokens.size();
  }

  public void addTokenLine(String tokenLine) {
    addToken(new ConllToken(tokenLine));
  }

  public void addToken(ConllToken token) {
    if (token != null) {
      final String tokenText = token.getText();
      if (tokenText != null && !"".equals(tokenText)) {
        if (token.hasLetterOrDigit() && text.length() > 0) {
          text.append(' ');
        }
        token.setStartPos(text.length());
        text.append(tokenText);
        token.setEndPos(text.length());
      }
      this.tokens.add(token);
    }
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

  public ConllToken getRoot() {
    return getFirstToken(ConllField.HEAD, "0");
  }

  public List<ConllToken> getChildren(ConllToken parent) {
    List<ConllToken> result = null;

    if (parent != null) {
      final String parentId = parent.getData(ConllField.ID);
      result = getAllTokens(ConllField.HEAD, parentId);
    }

    return result;
  }

  public List<ConllToken> getChildren(ConllToken parent, ConllField field, String value) {
    List<ConllToken> result = null;

    if (parent != null) {
      final String parentId = parent.getData(ConllField.ID);
      result = getAllTokens(new ConllField[]{ConllField.HEAD, field}, new String[]{parentId, value});
    }

    return result;
  }

  public List<ConllToken> getChildren(ConllToken parent, ConllField[] fields, String[] values) {
    List<ConllToken> result = null;

    if (parent != null) {
      final String parentId = parent.getData(ConllField.ID);
      result = getAllTokens(fieldsArray(ConllField.HEAD, fields), valuesArray(parentId, values));
    }

    return result;
  }

  public ConllToken getFirstChild(ConllToken parent, ConllField field, String value) {
    ConllToken result = null;

    if (parent != null) {
      final String parentId = parent.getData(ConllField.ID);
      result = getFirstToken(new ConllField[]{ConllField.HEAD, field}, new String[]{parentId, value});
    }

    return result;
  }

  public ConllToken getFirstChild(ConllToken parent, ConllField[] fields, String[] values) {
    ConllToken result = null;

    if (parent != null) {
      final String parentId = parent.getData(ConllField.ID);
      result = getFirstToken(fieldsArray(ConllField.HEAD, fields), valuesArray(parentId, values));
    }

    return result;
  }

  public List<ConllToken> getDeepChildren(ConllToken parent, ConllField[] fields, String[] values) {
    List<ConllToken> result = null;

    final List<ConllToken> candidates = getAllTokens(fields, values);
    if (candidates != null) {
      for (ConllToken candidate : candidates) {
        if (isParent(parent, candidate)) {
          if (result == null) result = new ArrayList<ConllToken>();
          result.add(candidate);
        }
      }
    }

    return result;
  }

  public ConllToken getFirstDeepChild(ConllToken parent, ConllField[] fields, String[] values) {
    ConllToken result = null;

    final List<ConllToken> candidates = getAllTokens(fields, values);
    if (candidates != null) {
      for (ConllToken candidate : candidates) {
        if (isParent(parent, candidate)) {
          result = candidate;
          break;
        }
      }
    }

    return result;
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

  public ConllToken getFirstToken(ConllField[] fields, String[] values) {
    ConllToken result = null;

    for (ConllToken token : tokens) {
      if (token.matches(fields, values)) {
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

  public List<ConllToken> getAllTokens(ConllField[] fields, String[] values) {
    List<ConllToken> result = null;

    for (ConllToken token : tokens) {
      if (token.matches(fields, values)) {
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
    return text.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (ConllToken token : tokens) {
      if (result.length() > 0) result.append('\n');
      result.append(token);
    }

    return result.toString();
  }

  public boolean isParent(ConllToken parent, ConllToken child) {
    boolean result = false;

    if (parent != null) {
      final String parentId = parent.getData(ConllField.ID);
      while (child != null) {
        final String head = child.getData(ConllField.HEAD);
        if (parentId.equals(head)) {
          result = true;
          break;
        }
        child = getFirstToken(ConllField.ID, head);
      }
    }

    return result;
  }

  public ConllToken getParent(ConllToken child) {
    ConllToken result = null;

    if (child != null) {
      final String head = child.getData(ConllField.HEAD);
      result = getFirstToken(ConllField.ID, head);
    }

    return result;
  }

  private final ConllField[] fieldsArray(ConllField field, ConllField[] fields) {
    final ConllField[] result = new ConllField[fields.length + 1];
    int idx = 0;
    result[idx++] = field;
    for (ConllField f : fields) result[idx++] = f;
    return result;
  }

  private final String[] valuesArray(String value, String[] values) {
    final String[] result = new String[values.length + 1];
    int idx = 0;
    result[idx++] = value;
    for (String v : values) result[idx++] = v;
    return result;
  }
}
