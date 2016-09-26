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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class TopicGenerator {
  
  protected LexDictionary lexDictionary;
  private final SimpleWordLookupStrategy strategy;
  private Histogram<String> nounHistogram;
  private Histogram<String> verbHistogram;
  private Histogram<String> adjHistogram;
  private Histogram<String> advHistogram;
  private SynsetSelector synsetSelector;

  protected TopicGenerator(LexDictionary lexDictionary, File nounfile, File verbfile, File adjfile, File advfile) throws IOException {
    this.lexDictionary = lexDictionary;
    this.strategy = new SimpleWordLookupStrategy(lexDictionary);
    this.nounHistogram = HistogramUtil.loadHistogram(nounfile);
    this.verbHistogram = HistogramUtil.loadHistogram(verbfile);
    this.adjHistogram = HistogramUtil.loadHistogram(adjfile);
    this.advHistogram = HistogramUtil.loadHistogram(advfile);
    this.synsetSelector = new SynsetSelector();
  }

  public void process(String line) {
    final Set<String> synsetNames = new HashSet<String>();
    
    final WordNetTokenizer tokenizer = new WordNetTokenizer(lexDictionary, strategy, line);
    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      final Feature synsetsFeature = token.getFeature(WordNetTokenizer.SYNSETS_FEATURE_CONSTRAINT, false);

      if (synsetsFeature != null) {
        synsetNames.addAll(Arrays.asList(synsetsFeature.getValue().toString().split(",")));
      }
    }

    final String topNounSynsets = getTopSynsets(nounHistogram, synsetNames);
    final String topVerbSynsets = getTopSynsets(verbHistogram, synsetNames);
    final String topAdjSynsets = getTopSynsets(adjHistogram, synsetNames);
    final String topAdvSynsets = getTopSynsets(advHistogram, synsetNames);
    System.out.println(String.format("%s\t%s\t%s\t%s\t%s", line, topNounSynsets, topVerbSynsets, topAdjSynsets, topAdvSynsets));
  }

  private final String getTopSynsets(Histogram<String> histogram, Set<String> synsetNames) {
    final StringBuilder result = new StringBuilder();

    final List<Histogram<String>.Frequency<String>> freqs = histogram.getFrequencies(synsetNames);
    if (freqs != null) {
      long maxFreq = 0L;
      for (Histogram<String>.Frequency<String> freq : freqs) {
        final long curFreq = freq.getFrequency();

        if (maxFreq == 0 || curFreq == maxFreq) {
          if (result.length() > 0) result.append(",");
          result.append(freq.getElement());
        }
        else {
          break;
        }

        maxFreq = curFreq;
      }
    }

    return result.toString();
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


  public static void main(String[] args) throws IOException {
    // Properties:
    //    nounfile -- file for writing noun synset histogram
    //    verbfile -- file for writing verb synset histogram
    //    adjfile -- file for writing adj synset histogram
    //    advfile -- file for writing adv synset histogram

    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    final LexDictionary dict = WordNetLoader.loadLexDictionary(dataProperties);
    final File nounFile = dataProperties.getFile("nounfile", "workingDir");
    final File verbFile = dataProperties.getFile("verbfile", "workingDir");
    final File adjFile = dataProperties.getFile("adjfile", "workingDir");
    final File advFile = dataProperties.getFile("advfile", "workingDir");
    final TopicGenerator generator = new TopicGenerator(dict, nounFile, verbFile, adjFile, advFile);
    
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
  }
}
