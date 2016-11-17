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
package org.sd.nlp.conll.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import org.sd.nlp.conll.ConllField;
import org.sd.nlp.conll.ConllNodeData;
import org.sd.nlp.conll.ConllNodeDataMatcher;
import org.sd.nlp.conll.ConllReader;
import org.sd.nlp.conll.ConllSentence;
import org.sd.nlp.conll.ConllToken;
import org.sd.nlp.conll.ConllTokenSorter;
import org.sd.nlp.conll.TreeUtils;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlStringBuilder;


/**
 * Utility to identify named entities in conll text.
 * <p>
 * @author Spencer Koehler
 */
public class NamedEntityFinder {
  
  private ConllSentence sentence;

  public NamedEntityFinder(ConllSentence sentence) {
    this.sentence = sentence;
  }

  public static final List<NamedEntity> findNamedEntities(ConllSentence sentence) {
    final NamedEntityFinder finder = new NamedEntityFinder(sentence);
    return finder.findNamedEntities();
  }

  public List<NamedEntity> findNamedEntities() {
    List<NamedEntity> result = null;

    if (sentence == null) return result;

    final Tree<ConllNodeData> root = sentence.asTree();
    if (root != null) {
      result = findNamedEntities(result, root);
    }

    return result;
  }


  public static final ConllNodeDataMatcher PROPER_NOUN_MATCHER =
    ConllNodeData.getMatcher(ConllField.POSTAG, new String[]{"NNP", "NNPS"});

  public static final ConllNodeDataMatcher CONJ_MATCHER =
    ConllNodeData.getMatcher(ConllField.DEPREL, new String[]{"conj", "cc"});

  public static final ConllNodeDataMatcher PREP_MATCHER =
    ConllNodeData.getMatcher(ConllField.DEPREL, new String[]{"prep"});

  public static final ConllNodeDataMatcher APPOS_MATCHER =
    ConllNodeData.getMatcher(ConllField.DEPREL, new String[]{"appos"});

  public static final ConllNodeDataMatcher PUNCT_MATCHER =
    ConllNodeData.getMatcher(ConllField.DEPREL, new String[]{"punct"});



  private final List<NamedEntity> findNamedEntities(List<NamedEntity> result, Tree<ConllNodeData> root) {

    //
    // Traverse the sentence tree breadth-first,
    //   finding NNP and NNPS root tokens and adding their children.
    //   A new entity is triggered by:
    //     A node with children OR
    //     A node without the right parent OR
    //     Encountering a conj
    //   

    NamedEntity currentNamedEntity = null;
    for (Iterator<Tree<ConllNodeData>> iter = root.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<ConllNodeData> node = iter.next();
      final ConllNodeData data = node.getData();

      if (PROPER_NOUN_MATCHER.matches(data)) {
        boolean added = false;
        if (currentNamedEntity != null) {
          added = currentNamedEntity.addChild(node);
        }
        if (!added) {  // start a new named entity
          currentNamedEntity = new NamedEntity(sentence, node);
          if (result == null) result = new ArrayList<NamedEntity>();
          result.add(currentNamedEntity);
        }
      }
      else if (CONJ_MATCHER.matches(data)) {
        currentNamedEntity = null;
      }
    }

    return result;
  }


  public static final class NamedEntity {
    private ConllSentence sentence;
    private Tree<ConllNodeData> head;
    private List<Tree<ConllNodeData>> children;
    private ConllTokenSorter modifier;
    private ConllTokenSorter nameSorter;
    private ConllTokenSorter _preSorter;

    public NamedEntity(ConllSentence sentence, Tree<ConllNodeData> head) {
      this.sentence = sentence;
      this.head = head;
      this.children = null;
      this.modifier = null;
      this.nameSorter = new ConllTokenSorter().add(head.getData().getToken());
      this._preSorter = null;

      Tree<ConllNodeData> parent = head.getParent();
      if (parent != null && PREP_MATCHER.matches(parent.getData())) {
        modifier = new ConllTokenSorter().add(parent.getData().getToken());
        
        parent = parent.getParent();
        if (parent != null && APPOS_MATCHER.matches(parent.getData())) {
          modifier.add(parent.getData().getToken());
        }
      }
    }

    public boolean addChild(Tree<ConllNodeData> child) {
      boolean result = false;

      if (child.getParent() == head && !child.hasChildren()) {
        if (this.children == null) children = new ArrayList<Tree<ConllNodeData>>();
        children.add(child);
        this.nameSorter.add(child.getData().getToken());
        this._preSorter = null;
        result = true;

        // Preserve prev punctuation
        for (Tree<ConllNodeData> prev = child.getPrevSibling();
             prev != null && PUNCT_MATCHER.matches(prev.getData());
             prev = prev.getPrevSibling()) {
          children.add(prev);
          this.nameSorter.add(prev.getData().getToken());
          this._preSorter = null;
        }
      }

      return result;
    }

    public boolean hasChildren() {
      return children != null && children.size() > 0;
    }

    public boolean hasModifier() {
      return modifier != null;
    }

    public String getModifier() {
      return (modifier == null) ? null : modifier.getText();
    }

    public String getPreText() {
      if (_preSorter == null) {
        if (nameSorter.size() > 0) {
          final ConllTokenSorter sorter = new ConllTokenSorter();
          for (ConllToken preToken = sentence.getPriorToken(nameSorter.getTokens().first());
               preToken != null;
               preToken = sentence.getPriorToken(preToken)) {
            sorter.add(preToken);
            if (preToken.hasLetterOrDigit()) {
              _preSorter = sorter;
              break;
            }
          }
        }
        if (_preSorter == null) _preSorter = new ConllTokenSorter();
      }
      return _preSorter.getText();
    }

    public String getName() {
      return nameSorter.getText();
    }

    public XmlStringBuilder asXml() {
      final StringBuilder xml = new StringBuilder();
      xml.append("<name");
      if (hasModifier()) {
        xml.append(" mod=\"").append(getModifier()).append("\"");
      }
      final String preText = getPreText();
      if (preText != null && !"".equals(preText)) {
        xml.append(" pre=\"").append(preText).append("\"");
      }

      // add character position information of name
      if (nameSorter.size() > 0) {
        final ConllToken firstNameToken = nameSorter.getTokens().first();
        final ConllToken lastNameToken = nameSorter.getTokens().last();
        if (firstNameToken.hasStartPos() && lastNameToken.hasEndPos()) {
          xml.
            append(" startPos=\"").
            append(firstNameToken.getStartPos()).
            append("\" endPos=\"").
            append(lastNameToken.getEndPos()).
            append("\"");
        }
      }

      xml.append(">").append(getName()).append("</name>");

      final XmlStringBuilder result = new XmlStringBuilder().setXmlString(xml.toString());
      return result;
    }
  }


  public static final XmlStringBuilder asXml(String topTagAndAttributes, List<NamedEntity> namedEntities) {
    final XmlStringBuilder result = new XmlStringBuilder(topTagAndAttributes);
    addEntities(result, namedEntities);
    return result;
  }

  public static final void addEntities(XmlStringBuilder result, List<NamedEntity> namedEntities) {
    if (namedEntities != null) {
      for (NamedEntity namedEntity : namedEntities) {
        result.addElement(namedEntity.asXml().getXmlElement());
      }
    }
  }


  private static final String getOutput(ConllSentence sentence, List<NamedEntity> namedEntities) {
    final StringBuilder result = new StringBuilder();

    result.append(sentence.getText());
    if (namedEntities != null) {
      result.append('\t');
      final XmlStringBuilder xml = asXml("entities", namedEntities);
      xml.getXmlElement().asFlatString(result);
    }

    return result.toString();
  }


  public static void main(String[] args) throws IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    for (Iterator<ConllSentence> iter = new ConllReader(reader); iter.hasNext(); ) {
      final ConllSentence sentence = iter.next();
      final List<NamedEntity> namedEntities = findNamedEntities(sentence);
      System.out.println(getOutput(sentence, namedEntities));
    }
  }
}
