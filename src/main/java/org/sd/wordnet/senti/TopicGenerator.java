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


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.atn.AtnParse;
import org.sd.atnexec.ConfigUtil;
import org.sd.util.Histogram;
import org.sd.util.HistogramUtil;
import org.sd.util.tree.Tree;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Utility to generate a topics from sentence lines and their pre-generated histograms.
 * <p>
 * @author Spencer Koehler
 */
public class TopicGenerator implements SynsetLineProcessor.SynsetLineHandler {
  
  private Histogram<String> nounHistogram;
  private Histogram<String> verbHistogram;
  private Histogram<String> adjHistogram;
  private Histogram<String> advHistogram;

  private final Set<String> synsetNames = new HashSet<String>();

  protected TopicGenerator(File nounfile, File verbfile, File adjfile, File advfile) throws IOException {
    this.nounHistogram = HistogramUtil.loadHistogram(nounfile);
    this.verbHistogram = HistogramUtil.loadHistogram(verbfile);
    this.adjHistogram = HistogramUtil.loadHistogram(adjfile);
    this.advHistogram = HistogramUtil.loadHistogram(advfile);
  }

  @Override
  public void startLine(String line) {
    this.synsetNames.clear();
  }

  @Override
  public void processSynsets(LexDictionary lexDictionary, Collection<String> synsetNames, AtnParse atnParse, Tree<String> tokenNode) {
    this.synsetNames.addAll(synsetNames);
  }

  @Override
  public void endLine(String line, boolean fromParse) {
    final String topNounSynsets = getTopSynsets(nounHistogram, synsetNames);
    final String topVerbSynsets = getTopSynsets(verbHistogram, synsetNames);
    final String topAdjSynsets = getTopSynsets(adjHistogram, synsetNames);
    final String topAdvSynsets = getTopSynsets(advHistogram, synsetNames);
    System.out.println(String.format("%s\t%s\t%s\t%s\t%s\t%s", line, topNounSynsets, topVerbSynsets, topAdjSynsets, topAdvSynsets, fromParse ? "PARSE" : "TOKENS"));
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
    args = dataProperties.getRemainingArgs();

    final File nounFile = dataProperties.getFile("nounfile", "workingDir");
    final File verbFile = dataProperties.getFile("verbfile", "workingDir");
    final File adjFile = dataProperties.getFile("adjfile", "workingDir");
    final File advFile = dataProperties.getFile("advfile", "workingDir");

    final TopicGenerator topicGenerator = new TopicGenerator(nounFile, verbFile, adjFile, advFile);
    final SynsetLineProcessor processor = new SynsetLineProcessor(topicGenerator, dataProperties);
    processor.process(args);
    processor.close();
  }
}
