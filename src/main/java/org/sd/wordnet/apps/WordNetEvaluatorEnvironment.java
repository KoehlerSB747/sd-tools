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
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.sd.wordnet.rel.ExpandedWord;
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

    final File dbFileDir = dataProperties.getFile("dbFileDir", "dir");
    try {
      this.lexDictionary = new LexDictionary(new LexLoader(dbFileDir), true, true, true);
    }
    catch (IOException ioe) {
      throw new IllegalArgumentException(ioe);
    }
  }

  @Override
  protected void addMoreFunctions() {
    defineFunction("lookup", new LookupFunction());
    // defineFunction("graph", new GraphFunction());
    // defineFunction("verify", new GraphFunction());
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
          final List<PointerInstance> pointers = lexDictionary.getAllPointers(null, synsets);
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

  public final class WordAnalysisObject extends AbstractAnalysisObject {
    public final SynsetAnalysisObject synset;
    public final WordAnalysisObject sourceWord;
    public final Word word;
    public final PointerInstance pointer;
    private List<PointerInstance> pointers;
    private ExpandedWord _expandedWord;

    public WordAnalysisObject(Word word, SynsetAnalysisObject container) {
      this(word, container, null, null);
    }

    public WordAnalysisObject(Word word, SynsetAnalysisObject synset, WordAnalysisObject sourceWord, PointerInstance pointer) {
      this.word = word;
      this.synset = synset;
      this.sourceWord = sourceWord;
      this.pointer = pointer;
      this.pointers = lexDictionary.getAllPointers(null, word);
      this._expandedWord =  null;
    }

    public ExpandedWord getExpandedWord() {
      if (_expandedWord == null) {
        _expandedWord = new ExpandedWord(word, lexDictionary);
      }
      return _expandedWord;
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
        append("\"graph\" -- show the word's graph.\n").
        append("\"follow[<ptrIdx>]\" -- follow the identified pointer to its word.\n").
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

      if ("graph".equals(ref)) {
        final ExpandedWord expandedWord = getExpandedWord();
        final DotWriter dotWriter = expandedWord.getDotWriter();
        final StringWriter stringWriter = new StringWriter();
        try {
          dotWriter.writeDot(stringWriter);
          result = new BasicAnalysisObject<String>(stringWriter.toString());
        }
        catch (IOException ioe) {
          result = new ErrorAnalysisObject("failed writing graph", ioe);
        }
      }
      else if ("back".equals(ref)) {
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
  }
}
