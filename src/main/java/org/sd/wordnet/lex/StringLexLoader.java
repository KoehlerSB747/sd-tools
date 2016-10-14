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
package org.sd.wordnet.lex;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility for loading all lex files.
 * <p>
 * @author Spencer Koehler
 */
public class StringLexLoader implements LexLoader {
  
  private long synsetCount;
  private Map<String, List<String>> lexFileName2strings;

  public StringLexLoader() {
    this.lexFileName2strings = new HashMap<String, List<String>>();
  }

  public void add(String lexFileName, String[] strings) {
    List<String> lexStrings = lexFileName2strings.get(lexFileName);
    if (lexStrings == null) {
      lexStrings = new ArrayList<String>();
      lexFileName2strings.put(lexFileName, lexStrings);
    }
    for (String string : strings) {
      lexStrings.add(string);
    }
  }

  public void add(String lexFileName, List<String> strings) {
    List<String> lexStrings = lexFileName2strings.get(lexFileName);
    if (lexStrings == null) {
      lexStrings = new ArrayList<String>();
      lexFileName2strings.put(lexFileName, lexStrings);
    }
    lexStrings.addAll(strings);
  }

  public long getSynsetCount() {
    return synsetCount;
  }

  /**
   * Load all synsets and adjective clusters within all files in the dbfiles directory.
   *
   * @param entryHandler  Handler for each synset and adjective cluster
   * @param filter  FileFilter to select files to load. If null, all files of
   *                form X.Y in dbFileDir will be loaded.
   */
  public void load(EntryHandler entryHandler) {
    for (Map.Entry<String, List<String>> entry : lexFileName2strings.entrySet()) {
      final String fileName = entry.getKey();
      final List<String> strings = entry.getValue();

      if (fileName.startsWith("adj.")) {
        // load AdjectiveCluster
        //todo: implement this if/when needed  see AdjectiveClusterFileIterator.loadNext
        // for (String string : strings) {
        //   final AdjectiveCluster adjCluster = ...;
        //   entryHandler.handleAdjectiveCluster(adjCluster);
        //   synsetCount += adjCluster.getSynsetCount();
      }
      else {
        // load Synset
        for (String string : strings) {
          final Synset synset = LexParser.parseLexString(string);
          if (synset != null) {
            synset.setLexFileName(fileName);
            ++synsetCount;
            entryHandler.handleSynset(synset);
          }
        }
      }
    }
  }
}
