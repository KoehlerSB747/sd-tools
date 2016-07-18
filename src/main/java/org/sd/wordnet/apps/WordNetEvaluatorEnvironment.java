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
import org.sd.analysis.AbstractAnalysisObject;
import org.sd.analysis.AnalysisFunction;
import org.sd.analysis.AnalysisObject;
import org.sd.analysis.BaseEvaluatorEnvironment;
import org.sd.analysis.BasicAnalysisObject;
import org.sd.analysis.EvaluatorEnvironment;
import org.sd.analysis.NumericAnalysisObject;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.wordnet.lex.Synset;
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

    public List<Synset> getSynsets() {
      return synsets;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#Synsets[").append(normInput).append(']');

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of the synsets.\n").
        append("\"definition\" -- get the definitions of each synset.");
//follow[...args...] ... follow pointers ...

      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      if (hasSynsets()) {
        result.append("lookupSynsets(" + normInput + ")");
        for (Synset synset : synsets) {
          result.append("\n").append(synset.getDescription());
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

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }
  }
}
