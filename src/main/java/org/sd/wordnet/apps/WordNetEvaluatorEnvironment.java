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
package org.sd.wordnet.apps;


import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.analysis.AbstractAnalysisObject;
import org.sd.analysis.AnalysisFunction;
import org.sd.analysis.AnalysisObject;
import org.sd.analysis.BaseEvaluatorEnvironment;
import org.sd.analysis.BasicAnalysisObject;
import org.sd.analysis.ErrorAnalysisObject;
import org.sd.analysis.EvaluatorEnvironment;
import org.sd.analysis.NumericAnalysisObject;
import org.sd.util.DotWriter;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.wordnet.lex.PointerInstance;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.lex.WordGraph;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.wordnet.rel.ExpandedWord;
import org.sd.wordnet.rel.GraphHelper;
import org.sd.wordnet.rel.WordRelation;
import org.sd.wordnet.util.NormalizeUtil;
import org.sd.xml.DataProperties;

/**
 * An EvaluatorEnvironment for interacting with WordNet through a CommandEvaluator.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetEvaluatorEnvironment extends BaseEvaluatorEnvironment {
  
  private LexDictionary lexDictionary;

  public WordNetEvaluatorEnvironment(DataProperties dataProperties) {
    super(dataProperties);
    this.lexDictionary = WordNetLoader.loadLexDictionary(dataProperties);
  }

  @Override
  protected void addMoreFunctions() {
    defineFunction("lookup", new LookupFunction());
    defineFunction("find", new FindFunction());
    // defineFunction("verify", new VerifyFunction());
    // defineFunction("anagrams", new AnagramFunction());  -- do WordUnscrambler
  }


  final class LookupFunction implements AnalysisFunction {

    LookupFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      SynsetLookupAnalysisObject result = null;

      if (args != null) {
        if (args.length >= 1) {
          final String input = args[0].toString();
          final String normInput = NormalizeUtil.normalizeForLookup(input);
          final List<Synset> synsets = lexDictionary.lookupSynsets(normInput);
          result = new SynsetLookupAnalysisObject(input, normInput, synsets);
        }
      }

      return result;
    }
  }

  final class FindFunction implements AnalysisFunction {

    FindFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      AnalysisObject result = null;

      if (args != null) {
        if (args.length >= 1) {
          final String input = args[0].toString();
          final List<Word> words = lexDictionary.findWords(input, null);
          if (words != null && words.size() == 1) {
            result = new WordAnalysisObject(words.get(0), null);
          }
          else {
            result = new WordsAnalysisObject(input, words);
          }
        }
      }

      return result;
    }
  }

  public final class SynsetLookupAnalysisObject extends AbstractAnalysisObject {
    public final String input;
    public final String normInput;
    private List<Synset> synsets;

    public SynsetLookupAnalysisObject(String input, String normInput, List<Synset> synsets) {
      this.input = input;
      this.normInput = normInput;
      this.synsets = synsets;
    }

    public boolean hasSynsets() {
      return synsets != null && synsets.size() > 0;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#Synsets[").append(normInput).append("]-").append(hasSynsets() ? synsets.size() : 0);

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of the synsets.\n").
        append("\"definition\" -- get the definitions of each synset.\n").
        append("\"graph\" -- show the synsets' graph.\n").
        append("\"synset[<synsetIdx>]\" -- get the identified synset.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      if (hasSynsets()) {
        result.append("lookupSynsets(" + normInput + ")");
        int synsetIdx = 0;
        for (Synset synset : synsets) {
          result.append("\n").append(++synsetIdx).append(": ").append(synset.getDescription());
        }
      }
      else {
        result.append("lookupSynsets(" + normInput + ") -- NO DATA");
      }

      return result.toString();
    }

    @Override
    protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
      AnalysisObject result = null;

      if ("definition".equals(ref)) {
        //todo: create and return this as a DataRecordAnalysisObject instead?
        final Map<String, String> map = new LinkedHashMap<String, String>();
        if (hasSynsets()) {
          for (Synset synset : synsets) {
            map.put(synset.getSynsetName(), synset.getGloss());
          }
        }
        result = new BasicAnalysisObject<Map<String, String>>(map);
      }
      else if ("graph".equals(ref)) {
        final StringBuilder graph = new StringBuilder();

        if (hasSynsets()) {
          final List<PointerInstance> pointers = lexDictionary.getForwardPointers(null, synsets);
          final WordGraph wordGraph = new WordGraph(synsets, pointers);
          wordGraph.buildGraph(graph);
        }

        result = new BasicAnalysisObject<String>(graph.toString());
      }
      else if (ref.startsWith("synset")) {
        if (hasSynsets()) {
          Synset synset = null;
          final AnalysisObject[] argValues = getArgValues(ref, env);
          final int[] args = asIntValues(argValues, 0);
          if (args != null && args.length > 0) {
            final int idx = Math.max(0, args[0] - 1);
            if (idx < synsets.size()) {
              synset = synsets.get(idx);
            }
          }
          else {
            synset = synsets.get(0);
          }

          if (synset != null) {
            result = new SynsetAnalysisObject(synset, this);
          }
          else {
            result = new ErrorAnalysisObject("No such elt");
          }
        }
      }

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }
  }

  public final class SynsetAnalysisObject extends AbstractAnalysisObject {
    public final SynsetLookupAnalysisObject container;
    public final Synset synset;
    private List<PointerInstance> pointers;

    public SynsetAnalysisObject(Synset synset, SynsetLookupAnalysisObject container) {
      this.synset = synset;
      this.container = container;
      this.pointers = lexDictionary.getSynsetPointers(null, synset);
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#Synset[").append(synset.getSynsetName()).append(']');

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of this synset.\n").
        append("\"word[<wordIdx>]\" -- get the identified word.\n").
        append("\"follow[<ptrIdx>]\" -- follow the identified pointer to its word.\n").
        append("\"back\" -- go back to (get) the synset lookup.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      return synset.getDescription();
    }

    @Override
    protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
      AnalysisObject result = null;

      if ("back".equals(ref)) {
        result = container;
      }
      else if (ref.startsWith("word")) {
        // get the word at idx=args[0]
        Word word = null;
        final AnalysisObject[] argValues = getArgValues(ref, env);
        final int[] args = asIntValues(argValues, 0);
        if (args != null && args.length > 0) {
          final int idx = Math.max(0, args[0] - 1);
          if (idx >= 0 && idx < synset.size()) {
            word = synset.getWords().get(idx);
          }
        }
        else if (synset.size() > 0) {
          word = synset.getWords().get(0);
        }

        if (word != null) {
          result = new WordAnalysisObject(word, this);
        }
        else {
          result = new ErrorAnalysisObject("No such elt");
        }
      }
      else if (ref.startsWith("follow")) {
        // get the word pointed to by synset ptr at idx=args[0]
        PointerInstance ptr = null;
        final AnalysisObject[] argValues = getArgValues(ref, env);
        final int[] args = asIntValues(argValues, 0);
        if (args != null && args.length > 0) {
          final int idx = Math.max(0, args[0] - 1);
          if (idx >= 0 && idx < pointers.size()) {
            ptr = pointers.get(idx);
          }
        }
        else if (pointers.size() > 0) {
          ptr = pointers.get(0);
        }

        if (ptr != null) {
          result = new WordAnalysisObject(ptr.getSpecificTarget(), this, null, ptr);
        }
        else {
          result = new ErrorAnalysisObject("No such elt");
        }
      }

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }
  }

  public final class WordsAnalysisObject extends AbstractAnalysisObject {
    public final String input;
    public final List<Word> words;

    public WordsAnalysisObject(String input, List<Word> words) {
      this.input = input;
      this.words = words;
    }

    public boolean hasWords() {
      return words != null && words.size() > 0;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#Words[").append(input).append("]-").append(hasWords() ? words.size() : 0);
      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of the words.\n").
        append("\"word[<wordIdx>]\" -- get the identified word.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      if (hasWords()) {
        result.append("findords(" + input + ")");
        int wordIdx = 0;
        for (Word word : words) {
          result.append("\n").append(++wordIdx).append(": ").append(word.getSynset().getGloss());
        }
      }
      else {
        result.append("lookupWords(" + input + ") -- NO DATA");
      }

      return result.toString();
    }

    @Override
    protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
      AnalysisObject result = null;

      if (ref.startsWith("word")) {
        // get the word at idx=args[0]
        Word word = null;
        final AnalysisObject[] argValues = getArgValues(ref, env);
        final int[] args = asIntValues(argValues, 0);
        if (args != null && args.length > 0) {
          final int idx = Math.max(0, args[0] - 1);
          if (idx >= 0 && idx < words.size()) {
            word = words.get(idx);
          }
        }
        else if (words.size() > 0) {
          word = words.get(0);
        }

        if (word != null) {
          result = new WordAnalysisObject(word, null);
        }
        else {
          result = new ErrorAnalysisObject("No such elt");
        }
      }

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }
  }


  public final class WordAnalysisObject extends AbstractAnalysisObject {
    public final SynsetAnalysisObject synset;
    public final WordAnalysisObject sourceWord;
    public final Word word;
    public final PointerInstance pointer;
    private GraphHelper graphHelper;
    private List<PointerInstance> pointers;

    public WordAnalysisObject(Word word, SynsetAnalysisObject container) {
      this(word, container, null, null);
    }

    public WordAnalysisObject(Word word, SynsetAnalysisObject synset, WordAnalysisObject sourceWord, PointerInstance pointer) {
      this.word = word;
      this.synset = synset;
      this.sourceWord = sourceWord;
      this.pointer = pointer;
      this.graphHelper = new GraphHelper(lexDictionary, word);
      this.pointers = graphHelper.getPointers();
    }

    public ExpandedWord getExpandedWord() {
      return graphHelper.getExpandedWord();
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#Word[").append(word.getQualifiedWordName()).append(']');

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of this word.\n").
        append("\"follow[<ptrIdx>]\" -- follow the identified pointer to its word.\n").
        append("\"ix[<word>]\" -- find the intersection with the other word.\n").
        append("\"graph[<maxDepth>,<symbolConstraint>,<onlyNames>]\" -- get the word's (dot) graph.\n").
        append("\"rgraph[<maxDepth>,<symbolConstraint>,<onlyNames>]\" -- get the word's reverse (dot) graph.\n").
        append("\"hypernyms\" -- get the word's hypernyms.\n").
        append("\"hyponyms\" -- get the word's hyponyms.\n").
        append("\"back\" -- go back to (get) the synset or word leading here, or get self.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      result.
        append(getTrail()).
        append("\n");

      if (word.hasSynset() && word.getSynset().hasGloss()) {
        result.
          append("\tgloss: ").
          append(word.getSynset().getGloss()).
          append("\n");
      }

      if (pointers.size() > 0) {
        int ptrNum = 1;
        for (PointerInstance ptr : pointers) {
          result.
            append("\t\t").
            append(ptrNum++).
            append(": ").
            append(ptr.getPointerDef().getFormattedPointerDefinition()).
            append("\n");
        }
      }

      if (word.hasFrames()) {
        int frmNum = 0;
        result.append("\tFrames: ");
        for (Integer frame : word.getFrames()) {
          if (frmNum > 0) result.append(", ");
          result.append(frame);
          ++frmNum;
        }
        result.append("\n");
      }

      return result.toString();
    }

    @Override
    protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
      AnalysisObject result = null;

      if ("back".equals(ref)) {
        if (sourceWord != null) {
          result = sourceWord;
        }
        else if (synset != null) {
          result = synset;
        }
        else {
          result = this;
        }
      }
      else if ("hypernyms".equals(ref)) {
        final Set<String> hypernyms = graphHelper.getHypernyms();
        result = new BasicAnalysisObject<Set<String>>(hypernyms);
      }
      else if ("hyponyms".equals(ref)) {
        final Set<String> hyponyms = graphHelper.getHyponyms();
        result = new BasicAnalysisObject<Set<String>>(hyponyms);
      }
      else if (ref.startsWith("ix")) {
        final AnalysisObject[] argValues = getArgValues(ref, env);
        if (argValues != null && argValues.length > 0 && argValues[0] instanceof WordAnalysisObject) {
          final WordAnalysisObject other = (WordAnalysisObject)argValues[0];
          final ExpandedWord otherExpandedWord = other.getExpandedWord();
/*
          final ExpandedWord myExpandedWord = this.getExpandedWord();
          final Set<String> ix = myExpandedWord.getIntersection(otherExpandedWord);
          result = new BasicAnalysisObject<Set<String>>(ix);
*/
/*
          final String graph = graphHelper.getIntersectionDotGraph(otherExpandedWord);
          result = new BasicAnalysisObject<String>(graph);
*/
          final Set<WordRelation> relData = graphHelper.getRelationsWith(otherExpandedWord);
          result = new BasicAnalysisObject<Set<WordRelation>>(relData);
        }
      }
      else if (ref.startsWith("graph") || ref.startsWith("rgraph")) {
        final boolean doGraph = ref.startsWith("graph");

        int maxDepth = -1;
        String symbolConstraint = null;
        boolean onlyNamesFlag = false;

        final AnalysisObject[] argValues = getArgValues(ref, env);
        if (argValues != null) {
          // maxDepth, symbolConstraint, onlyNamesFlag
          if (argValues.length > 0) {
            try {
              maxDepth = Integer.parseInt(argValues[0].toString());
            }
            catch (NumberFormatException nfe) {
              System.err.println("Error w/maxDepth=" + argValues[0].toString() + ": " + nfe.toString());
            }
          }
          if (argValues.length > 1) {
            symbolConstraint = argValues[1].toString();
            if ("".equals(symbolConstraint) || "null".equals(symbolConstraint)) {
              symbolConstraint = null;
            }
          }
          if (argValues.length > 2) {
            onlyNamesFlag = "true".equals(argValues[3].toString());
          }
        }

        result = doGraphAccess(doGraph, maxDepth, symbolConstraint, onlyNamesFlag, 250);
      }
      else if (ref.startsWith("follow")) {
        // get the word pointed to by synset ptr at idx=args[0]
        PointerInstance ptr = null;
        final AnalysisObject[] argValues = getArgValues(ref, env);
        final int[] args = asIntValues(argValues, 0);
        if (args != null && args.length > 0) {
          final int idx = Math.max(0, args[0] - 1);
          if (idx >= 0 && idx < pointers.size()) {
            ptr = pointers.get(idx);
          }
        }
        else if (pointers.size() > 0) {
          ptr = pointers.get(0);
        }

        if (ptr != null) {
          result = new WordAnalysisObject(ptr.getSpecificTarget(), this.synset, this, ptr);
        }
        else {
          result = new ErrorAnalysisObject("No such elt");
        }
      }

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }

    private String getTrail() {
      final StringBuilder result = new StringBuilder();

      result.append(word.getQualifiedWordName());
      WordAnalysisObject prevWord = this;
      for (; prevWord.sourceWord != null; prevWord = prevWord.sourceWord) {
        if (prevWord.pointer != null) {
          result.insert(0, ")-> ");
          result.insert(0, prevWord.pointer.getPointerDef().getFormattedPointerDefinition());
          result.insert(0, " -(");
        }
        else {
          result.insert(0, " --> ");
        }
        result.insert(0, prevWord.sourceWord.word.getQualifiedWordName());
      }
      if (prevWord.synset != null) {
        result.insert(0, " --> ");
        result.insert(0, synset.synset.toString());
      }

      return result.toString();
    }

    private final AnalysisObject doGraphAccess(boolean doForwardGraph, int maxDepth, String symbolConstraint, boolean onlyNamesFlag, int ptrLimit) {
      AnalysisObject result = null;

      if (onlyNamesFlag) {
        final Set<String> names = graphHelper.getNames(maxDepth, symbolConstraint, !doForwardGraph);
        result = new BasicAnalysisObject<Set<String>>(names);
      }
      else {
        final String dotGraph = graphHelper.getDotGraph(maxDepth, symbolConstraint, ptrLimit, !doForwardGraph);
        result = new BasicAnalysisObject<String>(dotGraph);
      }

      return result;
    }
  }
}
