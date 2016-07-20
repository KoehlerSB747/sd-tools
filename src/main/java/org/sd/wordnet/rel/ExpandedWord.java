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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.util.DotWriter;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeAnalyzer;
import org.sd.util.tree.Tree2Dot;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.PointerDefinition;
import org.sd.wordnet.lex.PointerInstance;
import org.sd.wordnet.lex.Word;

/**
 * Container for a word with all of its relationships.
 * <p>
 * Modeled as a tree with words mapped to nodes.
 *
 * @author Spence Koehler
 */
public class ExpandedWord {

  private Tree<PointerData> tree;
  private Map<String, Tree<PointerData>> nodeMap;
  private DotWriter _dotWriter;

  public ExpandedWord(Word rootWord, LexDictionary dict) {
    this.nodeMap = new HashMap<String, Tree<PointerData>>();
    this.tree = doAddNode(new PointerData(rootWord, null), null, dict);
  }

  private final Tree<PointerData> doAddNode(PointerData ptrData, Tree<PointerData> parent, LexDictionary dict) {
    Tree<PointerData> result = null;

    final String wordName = ptrData.word.getWordName();
    final Tree<PointerData> existing = this.nodeMap.get(wordName);
    if (existing != null) {
      final int existingDepth = existing.depth();
      final int newDepth = (parent == null) ? 0 : parent.depth() + 1;

      if (newDepth >= existingDepth) {
        return existing;
      }
    }

    result = new Tree<PointerData>(ptrData);
    if (parent != null) {
      parent.addChild(result);
    }
    if (existing != null) {
      existing.prune(true, true);
    }

    this.nodeMap.put(wordName, result);
    expand(result, dict);

    return result;
  }

  private final void expand(Tree<PointerData> ptrNode, LexDictionary dict) {
    final Word word = ptrNode.getData().word;
    for (PointerInstance ptr : dict.getAllPointers(null, word)) {
      final Word child = ptr.getSpecificTarget();
      if (child != null) {
        doAddNode(new PointerData(child, ptr.getPointerDef()), ptrNode, dict);
      }
    }
  }

  public int size() {
    return nodeMap.size();
  }

  public Word getRootWord() {
    return tree.getData().word;
  }

  public Tree<PointerData> getWordTree() {
    return tree;
  }

  public Map<String, Tree<PointerData>> getNodeMap() {
    return nodeMap;
  }

  public DotWriter getDotWriter() {
    if (_dotWriter == null) {
      _dotWriter = buildDotWriter();
    }
    return _dotWriter;
  }
  //todo: create/return a graph representing the tree

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
            if (key.equals(curKey)) {
              shouldAdd = false;
              break;
            }
            else {
              final int myCmp = this.compareAncestry(key, curKey);
              if (myCmp < 0) {
                // new key is an ancestor to existing. remove current key
                if (shouldRemoveIfAdd == null) shouldRemoveIfAdd = new ArrayList<String>();
                shouldRemoveIfAdd.add(curKey);
              }
              else if (myCmp > 0) {
                shouldAdd = false;
                break;
              }

              final int otherCmp = other.compareAncestry(key, curKey);
              if (otherCmp < 0) {
                // new key is an ancestor to existing. remove current key
                if (shouldRemoveIfAdd == null) shouldRemoveIfAdd = new ArrayList<String>();
                shouldRemoveIfAdd.add(curKey);
              }
              else if (otherCmp > 0) {
                shouldAdd = false;
                break;
              }
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
    return new Tree2Dot<PointerData>(tree, null, nodeLabelMaker, edgeLabelMaker);
  }


  public static final class PointerData {
    public final Word word;
    public final PointerDefinition sourcePtr;  // the pointer leading to this word (node)

    public PointerData(Word word, PointerDefinition sourcePtr) {
      this.word = word;
      this.sourcePtr = sourcePtr;
    }
  }
}
