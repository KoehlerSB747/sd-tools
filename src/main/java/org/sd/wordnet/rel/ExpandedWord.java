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
package org.sd.wordnet.rel;


import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.sd.util.DotWriter;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeAnalyzer;
import org.sd.util.tree.Tree2Dot;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.PointerDefinition;
import org.sd.wordnet.lex.PointerInstance;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;

/**
 * Container for a word with all of its relationships.
 * <p>
 * Modeled as a tree with words mapped to nodes.
 *
 * @author Spence Koehler
 */
public class ExpandedWord {

  private LexDictionary dict;
  private Tree<PointerData> tree;
  private Map<String, Tree<PointerData>> nodeMap;
  private DotWriter _dotWriter;
  private int maxDepth;
  private String symbolConstraint;

  public ExpandedWord(Word rootWord, LexDictionary dict) {
    this(rootWord, dict, -1, null);
  }

  public ExpandedWord(Word rootWord, LexDictionary dict, int maxDepth, String symbolConstraint) {
    this.dict = dict;
    this.maxDepth = maxDepth;
    this.symbolConstraint = symbolConstraint;
    this.nodeMap = new HashMap<String, Tree<PointerData>>();
    this.tree = doAddNode(new PointerData(rootWord, null), null, dict);
  }

  private final Tree<PointerData> doAddNode(PointerData ptrData, Tree<PointerData> parent, LexDictionary dict) {
    Tree<PointerData> result = null;
    Tree<PointerData> childNode = null;
    final LinkedList<Bundle> queue = new LinkedList<Bundle>();

    while (ptrData != null) {
      final String wordName = ptrData.word.getQualifiedWordName();
      final Tree<PointerData> existing = this.nodeMap.get(wordName);
      boolean shouldAdd = true;
      if (existing != null) {
        final int existingDepth = existing.depth();
        final int newDepth = (parent == null) ? 0 : parent.depth() + 1;

        if (newDepth >= existingDepth) {
          shouldAdd = false;
        }
        else {
          existing.prune(true, true);
          childNode = existing;
        }
      }
      else {
        childNode = new Tree<PointerData>(ptrData);
      }

      if (shouldAdd) {
        int nextDepth = 1;
        if (parent != null) {
          parent.addChild(childNode);
          nextDepth = parent.depth() + 2;
        }
        else if (result == null) {
          result = childNode;
        }

        this.nodeMap.put(wordName, childNode);

        if (existing == null && (maxDepth <= 0 || nextDepth < maxDepth)) {
          final Word word = childNode.getData().word;
          for (PointerInstance ptr : dict.getForwardPointers(null, word)) {
            final PointerDefinition ptrDef = ptr.getPointerDef();
            if (symbolConstraint == null || symbolConstraint.equals(ptrDef.getPointerSymbol())) {
              final Word child = ptr.getSpecificTarget();
              if (child != null) {
                queue.add(new Bundle(new PointerData(child, ptrDef), childNode));
              }
            }
          }
        }
      }

      ptrData = null;
      while (queue.size() > 0 && ptrData == null) {
        final Bundle bundle = queue.removeFirst();
        ptrData = bundle.ptrData;
        parent = bundle.parent;
      }
    }

    return result;
  }

  public int size() {
    return nodeMap.size();
  }

  public LexDictionary getLexDictionary() {
    return dict;
  }

  public Word getRootWord() {
    return tree.getData().word;
  }

  public String getRootWordName() {
    return tree.getData().word.getQualifiedWordName();
  }

  public Tree<PointerData> getWordTree() {
    return tree;
  }

  public Map<String, Tree<PointerData>> getNodeMap() {
    return nodeMap;
  }

  public TreeSet<String> getWordNames() {
    return new TreeSet<String>(nodeMap.keySet());
  }

  public String getDotGraph() {
    final StringWriter stringWriter = new StringWriter();
    final DotWriter dotWriter = getDotWriter();
    try {
      dotWriter.writeDot(stringWriter);
    }
    catch (IOException ioe) {
      // eat this.
    }
    return stringWriter.toString();
  }

  public DotWriter getDotWriter() {
    if (_dotWriter == null) {
      _dotWriter = buildDotWriter();
    }
    return _dotWriter;
  }
  //todo: create/return a graph representing the tree

  public Tree<PointerData> getNode(String wordName) {
    return nodeMap.get(wordName);
  }

  public int getDepth(String wordName) {
    int result = -1;

    final Tree<PointerData> ptrDataNode = nodeMap.get(wordName);
    if (ptrDataNode != null) {
      result = ptrDataNode.depth();
    }

    return result;
  }

  /**
   * Get the path of pointers from root to the given wordName, or null if the
   * wordName is not present.
   */
  public List<PointerData> getPointerPath(String wordName) {
    LinkedList<PointerData> result = null;

    for (Tree<PointerData> wordNode = nodeMap.get(wordName); wordNode != null; wordNode = wordNode.getParent()) {
      if (result == null) result = new LinkedList<PointerData>();
      result.addFirst(wordNode.getData());
    }

    return result;
  }

  /**
   * Build the path of pointer instances from root to the given wordName, or null if the
   * wordName is not present.
   */
  public List<PointerInstance> buildPointerPath(String wordName) {
    List<PointerInstance> result = null;

    final List<PointerData> pointerPath = getPointerPath(wordName);
    if (pointerPath != null) {
      result = new ArrayList<PointerInstance>();
      Word sourceWord = getRootWord();
      Synset sourceSynset = sourceWord.getSynset();
      for (PointerData ptrData : pointerPath) {
        if (ptrData.sourcePtr != null) {
          dict.findPointers(result, sourceSynset, sourceWord, ptrData.sourcePtr);
          final PointerInstance last = result.get(result.size() - 1);
          sourceWord = last.getSpecificTarget();
          sourceSynset = sourceWord.getSynset();
        }
      }
    }

    return result;
  }

  /**
   * Find the closest (to root) wordNames that intersect between this and
   * other, or null.
   */
  public Set<String> getIntersection(ExpandedWord other) {
    Set<String> result = null;

    Map<String, Tree<PointerData>> a = this.nodeMap;
    Map<String, Tree<PointerData>> b = other.nodeMap;

    if (this.size() >= other.size()) {
      a = other.nodeMap;
      b = this.nodeMap;
    }

    // find common keys
    for (String key : a.keySet()) {
      if (b.containsKey(key)) {
        if (result == null) {
          result = new HashSet<String>();
          result.add(key);
        }
        else {
          // add or keep only the most shallow keys
          boolean shouldAdd = true;
          List<String> shouldRemoveIfAdd = null;

          for (String curKey : result) {
            final int myCmp = this.compareAncestry(key, curKey);
            if (myCmp < 0) {
              // new key is an ancestor to existing. remove current key
              if (shouldRemoveIfAdd == null) shouldRemoveIfAdd = new ArrayList<String>();
              shouldRemoveIfAdd.add(curKey);
            }
            else if (myCmp > 0) {
              shouldAdd = false;
            }
          }

          if (shouldAdd) {
            if (shouldRemoveIfAdd != null) result.removeAll(shouldRemoveIfAdd);
            result.add(key);
          }
        }
      }
    }

    return (result == null || result.size() == 0) ? null : result;
  }

  /**
   * Compare the ancestry of the two keys in the tree. Note that equal words
   * do not have ancestry to each other and will have a compare result of 0.
   *
   * @param wordName1  first wordName
   * @param wordName2  second wordName
   *
   * @return -1 if wordName1 is an ancestor to wordName2, 1 if wordName2 is an
   *         ancestor to wordName1, or 0 if there is no ancestry relationship.
   */
  public final int compareAncestry(String wordName1, String wordName2) {
    int result = 0;

    final Tree<PointerData> node1 = nodeMap.get(wordName1);
    if (node1 != null) {
      final Tree<PointerData> node2 = nodeMap.get(wordName2);
      if (node2 != null) {
        final int depth1 = node1.depth();
        final int depth2 = node2.depth();

        if (depth1 < depth2) {
          if (node1.isAncestor(node2)) {
            result = -1;
          }
        }
        else if (depth2 < depth1) {
          if (node2.isAncestor(node1)) {
            result = 1;
          }
        }
      }
    }

    return result;
  }

  private final DotWriter buildDotWriter() {
    final Tree2Dot.LabelMaker<PointerData> nodeLabelMaker = new Tree2Dot.LabelMaker<PointerData>() {
        public String makeLabel(Tree<PointerData> node, TreeAnalyzer<PointerData> treeAnalyzer) {
          return node.getData().word.getQualifiedWordName();
        }
      };
    final Tree2Dot.LabelMaker<PointerData> edgeLabelMaker = new Tree2Dot.LabelMaker<PointerData>() {
        public String makeLabel(Tree<PointerData> node, TreeAnalyzer<PointerData> treeAnalyzer) {
          String result = null;
          final PointerDefinition ptrDef= node.getData().sourcePtr;
          if (ptrDef != null) {
            result = node.getData().sourcePtr.getDotNormalizedPointerSymbol();
          }
          return result;
        }
      };
    final Tree2Dot<PointerData> result = new Tree2Dot<PointerData>(tree, null, nodeLabelMaker, edgeLabelMaker);

    result.setAttribute("rankdir", "LR");

    return result;
  }


  public static final class PointerData {
    public final Word word;
    public final PointerDefinition sourcePtr;  // the pointer leading to this word (node)

    public PointerData(Word word, PointerDefinition sourcePtr) {
      this.word = word;
      this.sourcePtr = sourcePtr;
    }

    public boolean equals(Object other) {
      boolean result = (this == other);

      if (!result && other != null && other instanceof PointerData) {
        final PointerData otherPointerData = (PointerData)other;

        if (this.sourcePtr != null && otherPointerData.sourcePtr != null) {
          result = this.sourcePtr.equals(otherPointerData.sourcePtr);
        }
        else if (this.sourcePtr == null && otherPointerData.sourcePtr == null) {
          result = (this.word == otherPointerData.word);

          if (!result) {
            if (this.word != null && otherPointerData.word != null) {
              result = this.word.getQualifiedWordName().equals(otherPointerData.word.getQualifiedWordName());
            }
          }
        }
      }

      return result;
    }

    public int hashCode() {
      int result = 11;

      if (word != null) result = result * 11 + word.getQualifiedWordName().hashCode();
      if (sourcePtr != null) result = result * 11 + sourcePtr.hashCode();

      return result;
    }
  }

  public static final class Bundle {
    public final PointerData ptrData;
    public final Tree<PointerData> parent;

    public Bundle(PointerData ptrData, Tree<PointerData> parent) {
      this.ptrData = ptrData;
      this.parent = parent;
    }
  }
}
