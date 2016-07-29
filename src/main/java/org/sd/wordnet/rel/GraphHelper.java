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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.PointerInstance;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.lex.WordGraph;

/**
 * Utilities for working with graphs.
 * <p>
 * @author Spencer Koehler
 */
public class GraphHelper {
  
  private LexDictionary dict;
  private Word word;
  private ExpandedWord _expandedWord;
  private Set<String> _hypernyms;
  private Set<String> _hyponyms;
  private List<PointerInstance> _pointers;
  private List<PointerInstance> _revPointers;
  private ExpandedWord _hyperWord;
  private List<PointerInstance> _hypoPointers;

  public GraphHelper(LexDictionary dict, Word word) {
    this.dict = dict;
    this.word = word;
    this._expandedWord = null;
    this._hypernyms = null;
    this._hyponyms = null;
    this._pointers = null;
    this._revPointers = null;
    this._hyperWord = null;
    this._hypoPointers = null;
  }

  public LexDictionary getDictionary() {
    return dict;
  }

  public Word getWord() {
    return word;
  }

  public ExpandedWord getExpandedWord() {
    if (_expandedWord == null) {
      // build unconstrained expanded word
      _expandedWord = new ExpandedWord(word, dict);
    }
    return _expandedWord;
  }

  public Set<String> getHypernyms() {
    if (_hypernyms == null) {
      final ExpandedWord hyperWord = getHyperWord();
      _hypernyms = hyperWord.getWordNames();
    }
    return _hypernyms;
  }

  public Set<String> getHyponyms() {
    if (_hyponyms == null) {
      final List<PointerInstance> hypoPointers = getHypoPointers();
      _hyponyms = collectPointerNames(hypoPointers);
    }
    return _hyponyms;
  }

  public List<PointerInstance> getPointers() {
    if (_pointers == null) {
      // compute/store unrestricted (forward) pointers
      _pointers = dict.getForwardPointers(null, word);
    }
    return _pointers;
  }

  public List<PointerInstance> getReversePointers() {
    if (_revPointers == null) {
      // compute/store unrestricted reverse pointers
      _revPointers = dict.getReversePointers(null, word.getQualifiedWordName(), word, -1, null);
    }
    return _revPointers;
  }

  public ExpandedWord getHyperWord() {
    if (_hyperWord == null) {
      _hyperWord = new ExpandedWord(word, dict, -1, "@");
    }
    return _hyperWord;
  }

  public List<PointerInstance> getHypoPointers() {
    if (_hypoPointers == null) {
      _hypoPointers = dict.getReversePointers(null, word.getQualifiedWordName(), word, -1, "@");
    }
    return _hypoPointers;
  }

  public String getDotGraph(int maxDepth, String symbolConstraint, int ptrLimit, boolean reverse) {
    String result = null;

    if (!reverse) {
      final ExpandedWord expandedWord = getExpandedWord(maxDepth, symbolConstraint);
      result = expandedWord.getDotGraph();  //todo: apply ptrLimit here?      
    }
    else {
      final List<PointerInstance> revPointers = getReversePointers(maxDepth, symbolConstraint);
      final WordGraph wordGraph = new WordGraph(null, revPointers, ptrLimit);
      final StringBuilder graph = wordGraph.buildGraph(null);
      result = graph.toString();
    }

    return result;
  }

  public Set<String> getNames(int maxDepth, String symbolConstraint, boolean reverse) {
    Set<String> result = null;

    if (!reverse) {
      final ExpandedWord expandedWord = getExpandedWord(maxDepth, symbolConstraint);
      result = expandedWord.getWordNames();
    }
    else {
      final List<PointerInstance> revPointers = getReversePointers(maxDepth, symbolConstraint);
      result = collectPointerNames(revPointers);
    }

    return result;
  }

  public Set<String> getIntersectionWith(ExpandedWord otherWord) {
    final ExpandedWord expandedWord = getExpandedWord();
    return expandedWord.getIntersection(otherWord);
  }

  public Set<WordRelation> getRelationsWith(ExpandedWord otherWord) {
    Set<WordRelation> result = null;

    final Set<String> commonWords = getIntersectionWith(otherWord);
    if (commonWords != null && commonWords.size() > 0) {
      result = new TreeSet<WordRelation>();    
      final ExpandedWord myWord = getExpandedWord();
      for (String commonWord : commonWords) {
        final List<ExpandedWord.PointerData> path1 = myWord.getPointerPath(commonWord);
        final List<ExpandedWord.PointerData> path2 = otherWord.getPointerPath(commonWord);
        final WordRelation wordRelation = new WordRelation(commonWord, path1, path2);
        result.add(wordRelation);
      }
    }

    return result;
  }

  public List<PointerInstance> getIntersectionPointers(ExpandedWord otherWord) {
    final List<PointerInstance> result = new ArrayList<PointerInstance>();

    final Set<String> commonWords = getIntersectionWith(otherWord);
    final ExpandedWord expandedWord = getExpandedWord();
    for (String commonWord : commonWords) {
      final List<PointerInstance> myPath = expandedWord.buildPointerPath(commonWord);
      if (myPath != null) {
        result.addAll(myPath);
      }
      final List<PointerInstance> otherPath = otherWord.buildPointerPath(commonWord);
      if (otherPath != null) {
        result.addAll(otherPath);
      }
    }

    return result;
  }

  public String getIntersectionDotGraph(ExpandedWord otherWord) {
    final List<PointerInstance> ptrs = getIntersectionPointers(otherWord);
    final WordGraph wordGraph = new WordGraph(null, ptrs, -1);
    final StringBuilder graph = wordGraph.buildGraph(null);
    return graph.toString();
  }

  public static final Set<String> collectPointerNames(List<PointerInstance> pointers) {
    final Set<String> result = new TreeSet<String>();

    for (PointerInstance pointer : pointers) {
      if (pointer.hasSourceSynset()) {
        // non-null source synset implies synset pointer, so add all of the synset's words
        for (Word sourceWord : pointer.getSourceSynset().getWords()) {
          result.add(sourceWord.getQualifiedWordName());
        }
      }
      else if (pointer.hasSourceWord()) {
        // just add the specific source word of the pointer
        result.add(pointer.getSourceWord().getQualifiedWordName());
      }
      final Word target = pointer.getSpecificTarget();
      if (target != null) {
        result.add(target.getQualifiedWordName());
      }
    }

    return result;
  }

  private final ExpandedWord getExpandedWord(int maxDepth, String symbolConstraint) {
    ExpandedWord expandedWord = null;

    if (maxDepth <= 0 && (symbolConstraint == null || "".equals(symbolConstraint))) {
      // can use the cached values
      expandedWord = getExpandedWord();
    }
    else {
      // create temporary with constraints
      expandedWord = new ExpandedWord(word, dict, maxDepth, symbolConstraint);
    }

    return expandedWord;
  }

  private final List<PointerInstance> getReversePointers(int maxDepth, String symbolConstraint) {
    List<PointerInstance> revPointers = null;

    if (maxDepth <= 0 && (symbolConstraint == null || "".equals(symbolConstraint))) {
      // can use the cached values
      revPointers = getReversePointers();
    }
    else {
      // create temporary with constraints
      revPointers = dict.getReversePointers(null, word.getQualifiedWordName(), word, maxDepth, symbolConstraint);
    }

    return revPointers;
  }
}
