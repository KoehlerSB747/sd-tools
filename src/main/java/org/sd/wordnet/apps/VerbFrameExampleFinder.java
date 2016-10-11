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


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sd.atnexec.ConfigUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.xml.DataProperties;

/**
 * Utility to find samples representing each verb frame.
 * <p>
 * @author Spencer Koehler
 */
public class VerbFrameExampleFinder {
  
  public static void main(String[] args) throws IOException {
    // Properties:
    //   numSamples -- (optional, default=2) number of samples to collect for each verb frame

    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    args = dataProperties.getRemainingArgs();

    final int numSamples = dataProperties.getInt("numSamples", 2);
    final Map<Integer, Set<String>> samples = new TreeMap<Integer, Set<String>>();

    final LexDictionary lexDictionary = WordNetLoader.loadLexDictionary(dataProperties);
    final Map<String, List<Synset>> synsets = lexDictionary.getSynsets();

    for (List<Synset> synsetsList : synsets.values()) {
      for (Synset synset : synsetsList) {
        for (Word word : synset.getWords()) {
          if (word.hasFrames()) {
            final List<Integer> frames = word.getFrames();
            for (Integer frame : frames) {
              Set<String> frameWords = samples.get(frame);
              if (frameWords == null) {
                frameWords = new TreeSet<String>();
                samples.put(frame, frameWords);
              }
              if (frameWords.size() < numSamples) {
                frameWords.add(word.getQualifiedWordName());
              }
            }
          }
        }
      }
    }

    for (Map.Entry<Integer, Set<String>> sample : samples.entrySet()) {
      System.out.println(String.format("\t%d: %s", sample.getKey(), sample.getValue()));
    }
  }
}
