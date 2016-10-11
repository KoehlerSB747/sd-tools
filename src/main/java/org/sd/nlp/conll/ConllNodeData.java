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


import java.util.HashMap;
import java.util.Map;

/**
 * Data structure to hold node data for a conll sentence tree.
 * <p>
 * Note that primarily, the node holds a ConllToken, but allows for "dummy"
 * nodes that aren't based on a token, like the root node, for example.
 * <p>
 * This also enables attaching extra attributes to a node.
 *
 * @author Spencer Koehler
 */
public class ConllNodeData {
  
  private String nodeName;
  private ConllToken token;
  private Map<String, String> attributes;

  public ConllNodeData(ConllToken token) {
    this(null, token);
  }

  public ConllNodeData(String nodeName) {
    this(nodeName, null);
  }

  public ConllNodeData(String nodeName, ConllToken token) {
    this.token = token;
    this.attributes = null;
    this.setNodeName(nodeName);  // must set after token
  }

  public boolean hasToken() {
    return token != null;
  }

  public ConllToken getToken() {
    return token;
  }

  public String getNodeName() {
    return nodeName;
  }

  public final void setNodeName(String nodeName) {
    this.nodeName = buildNodeName(nodeName);
  }

  public boolean hasAttributes() {
    return attributes != null && attributes.size() > 0;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public String getAttribute(String attribute) {
    return (attributes == null) ? null : attributes.get(attribute);
  }

  public void setAttribute(String attribute, String value) {
    if (attributes == null) attributes = new HashMap<String, String>();
    attributes.put(attribute, value);
  }

  private final String buildNodeName(String nodeName) {
    final StringBuilder result = new StringBuilder();

    if (nodeName != null) {
      result.append(nodeName);
    }
    else if (token != null) {
      result.append(token.getData(ConllField.FORM));

      String pos = null;
      if (token.hasData(ConllField.POSTAG)) {
        pos = token.getData(ConllField.POSTAG);
      }
      else if (token.hasData(ConllField.CPOSTAG)) {
        pos = token.getData(ConllField.CPOSTAG);
      }
      if (pos != null) {
        result.append('.').append(pos);
      }

      if (token.hasData(ConllField.DEPREL)) {
        result.append('.').append(token.getData(ConllField.DEPREL));
      }
    }
    else {
      result.append("_EMPTY_");
    }
    
    return result.toString();
  }

  public String toString() {
    return nodeName;
  }
}
