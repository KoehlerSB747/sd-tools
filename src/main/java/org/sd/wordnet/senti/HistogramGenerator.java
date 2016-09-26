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
package org.sd.wordnet.senti;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.sd.atnexec.ConfigUtil;
import org.sd.token.Feature;
import org.sd.token.Token;
import org.sd.util.Histogram;
import org.sd.util.HistogramUtil;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.wordnet.token.SimpleWordLookupStrategy;
import org.sd.wordnet.token.WordNetTokenizer;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Utility to generate a histogram from select corpus words.
 * <p>
 * @author Spencer Koehler
 */
public class HistogramGenerator {
  
  protected LexDictionary lexDictionary;
  private final SimpleWordLookupStrategy strategy;
  private Histogram<String> nounHistogram;
  private Histogram<String> verbHistogram;
  private Histogram<String> adjHistogram;
  private Histogram<String> advHistogram;
  private SynsetSelector synsetSelector;

  protected HistogramGenerator(LexDictionary lexDictionary) {
    this.lexDictionary = lexDictionary;
    this.strategy = new SimpleWordLookupStrategy(lexDictionary);
    this.nounHistogram = new Histogram<String>();
    this.verbHistogram = new Histogram<String>();
    this.adjHistogram = new Histogram<String>();
    this.advHistogram = new Histogram<String>();
    this.synsetSelector = new SynsetSelector();
  }

  public void process(String line) {
    final WordNetTokenizer tokenizer = new WordNetTokenizer(lexDictionary, strategy, line);
    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      final Feature normFeature = token.getFeature(WordNetTokenizer.NORM_FEATURE_CONSTRAINT, false);
      final Feature synsetsFeature = token.getFeature(WordNetTokenizer.SYNSETS_FEATURE_CONSTRAINT, false);

      if (synsetsFeature != null) {
        final String[] synsetNames = synsetsFeature.getValue().toString().split(",");
        process("noun", nounHistogram, synsetNames);
        process("verb", verbHistogram, synsetNames);
        process("adj", adjHistogram, synsetNames);
        process("adv", advHistogram, synsetNames);
      }
    }
  }

  private final void process(String prefix, Histogram<String> histogram, String[] synsetNames) {
    if (shouldInclude(prefix, synsetNames)) {
      for (String synsetName : synsetNames) {
        if (synsetName.startsWith(prefix)) {
          histogram.add(synsetName);
        }
      }
    }
  }

  public Histogram<String> getNounHistogram() {
    return nounHistogram;
  }

  public Histogram<String> getVerbHistogram() {
    return verbHistogram;
  }

  public Histogram<String> getAdjHistogram() {
    return adjHistogram;
  }

  public Histogram<String> getAdvHistogram() {
    return advHistogram;
  }

  protected boolean shouldInclude(String prefix, String[] synsetNames) {
    boolean result = false;

    if (shouldInclude2(synsetNames)) {
      for (String synsetName : synsetNames) {
        if (synsetName.startsWith(prefix)) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  // include if possibly an open-set word
  protected boolean shouldInclude1(String[] synsetNames) {
    boolean result = false;

    //todo: pare down or select from synsetNames by additional criteria

    for (String synsetName : synsetNames) {
      if (synsetSelector.hasQualifyingPartOfSpeech(synsetName)) {
        result = true;
        break;
      }
    }

    return result;
  }

  // exclude if possibly not an open-set word
  protected boolean shouldInclude2(String[] synsetNames) {
    boolean result = true;

    //todo: pare down or select from synsetNames by additional criteria

    for (String synsetName : synsetNames) {
      if (!synsetSelector.hasQualifyingPartOfSpeech(synsetName)) {
        result = false;
        break;
      }
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //    nounfile -- file for writing noun synset histogram
    //    verbfile -- file for writing verb synset histogram
    //    adjfile -- file for writing adj synset histogram
    //    advfile -- file for writing adv synset histogram

    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    final LexDictionary dict = WordNetLoader.loadLexDictionary(dataProperties);
    final HistogramGenerator generator = new HistogramGenerator(dict);
    
    args = dataProperties.getRemainingArgs();

    if (args != null && args.length > 0) {
      for (String arg : args) {
        generator.process(arg);
      }
    }
    else {
      // read from stdin
      final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String line = null;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if ("".equals(line)) continue;
        generator.process(line);
      }
    }

    final Histogram<String> nounHistogram = generator.getNounHistogram();
    final File nounFile = dataProperties.getFile("nounfile", "workingDir");
    System.out.print("Noun Histogram:");
    if (nounFile != null) System.out.print("  " + nounFile.getAbsolutePath());
    System.out.println();
    System.out.println(nounHistogram.toString(0));
    if (nounFile != null) {
      HistogramUtil.writeHistogram(nounFile, nounHistogram);
    }

    final Histogram<String> verbHistogram = generator.getVerbHistogram();
    final File verbFile = dataProperties.getFile("verbfile", "workingDir");
    System.out.print("Verb Histogram:");
    if (verbFile != null) System.out.print("  " + verbFile.getAbsolutePath());
    System.out.println();
    System.out.println(verbHistogram.toString(0));
    if (verbFile != null) {
      HistogramUtil.writeHistogram(verbFile, verbHistogram);
    }

    final Histogram<String> adjHistogram = generator.getAdjHistogram();
    final File adjFile = dataProperties.getFile("adjfile", "workingDir");
    System.out.print("Adj Histogram:");
    if (adjFile != null) System.out.print("  " + adjFile.getAbsolutePath());
    System.out.println();
    System.out.println(adjHistogram.toString(0));
    if (adjFile != null) {
      HistogramUtil.writeHistogram(adjFile, adjHistogram);
    }

    final Histogram<String> advHistogram = generator.getAdvHistogram();
    final File advFile = dataProperties.getFile("advfile", "workingDir");
    System.out.print("Adv Histogram:");
    if (advFile != null) System.out.print("  " + advFile.getAbsolutePath());
    System.out.println();
    System.out.println(advHistogram.toString(0));
    if (advFile != null) {
      HistogramUtil.writeHistogram(advFile, advHistogram);
    }
  }
}
