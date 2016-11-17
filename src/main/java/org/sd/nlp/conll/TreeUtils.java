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
import java.util.Iterator;
import java.util.List;
import org.sd.util.tree.Tree;

/**
 * Utilities for working with ConllNodeData trees.
 * <p>
 * @author Spencer Koehler
 */
public class TreeUtils {
  
  public static final Tree<ConllNodeData> findFirst(Tree<ConllNodeData> root, ConllNodeDataMatcher matcher) {
    return findFirst(root, matcher, true, Tree.Traversal.BREADTH_FIRST);
  }

  public static final Tree<ConllNodeData> findFirst(Tree<ConllNodeData> root, ConllNodeDataMatcher matcher, boolean skipRoot, Tree.Traversal traversal) {
    Tree<ConllNodeData> result = null;

    if (root != null) {
      for (Iterator<Tree<ConllNodeData>> iter = root.iterator(traversal); iter.hasNext(); ) {
        final Tree<ConllNodeData> node = iter.next();
        if (skipRoot && node == root) continue;
        if (matcher.matches(node.getData())) {
          result = node;
          break;
        }
      }
    }

    return result;
  }

  public static final List<Tree<ConllNodeData>> findAll(Tree<ConllNodeData> root, ConllNodeDataMatcher matcher) {
    return findAll(root, matcher, true, Tree.Traversal.BREADTH_FIRST);
  }

  public static final List<Tree<ConllNodeData>> findAll(Tree<ConllNodeData> root, ConllNodeDataMatcher matcher, boolean skipRoot, Tree.Traversal traversal) {
    List<Tree<ConllNodeData>> result = null;

    if (root != null) {
      for (Iterator<Tree<ConllNodeData>> iter = root.iterator(traversal); iter.hasNext(); ) {
        final Tree<ConllNodeData> node = iter.next();
        if (skipRoot && node == root) continue;
        if (matcher.matches(node.getData())) {
          if (result == null) result = new ArrayList<Tree<ConllNodeData>>();
          result.add(node);
        }
      }
    }

    return result;
  }

}
