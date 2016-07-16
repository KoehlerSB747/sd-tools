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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility to verify that lexicographer resources are being processed as expected.
 * <p>
 * @author Spencer Koehler
 */
public class LexChecker {
  
  private LexDictionary dict;
  private Map<String, List<Synset>> synsets;
  private Map<String, List<AdjectiveCluster>> adjClusters;
  private Map<String, Set<String>> lexNames;

  
  public LexChecker(LexDictionary dict) {
    this.dict = dict;
    this.synsets = dict.getSynsets();
    this.adjClusters = dict.getAdjClusters();
    this.lexNames = dict.getLexNames();
  }

  public int verify() {
    int warnings = 0;
    
    // check that all entries have a valid head
    if (synsets.containsKey("")) {
      final List<Synset> synsetsWithNoHead = synsets.get("");
      System.out.println("WARNING: found " + synsetsWithNoHead.size() + " synsets with no head");
      ++warnings;
    }

    // check that all lexNames are non-null
    for (Map.Entry<String, Set<String>> lexNameEntry : lexNames.entrySet()) {
      final String normInput = lexNameEntry.getKey();
      final Set<String> lexNameValues = lexNameEntry.getValue();
      if (lexNameValues == null) {
        System.out.println("WARNING: " + normInput + " has null lexNames");
        ++warnings;
      }
      else if (lexNameValues.contains(null)) {
        System.out.println("WARNING: " + normInput + " has null lexName " + lexNameValues);
        ++warnings;
      }
    }

    // check that all pointers have a valid headWord
    for (Map.Entry<String, List<Synset>> synsetEntry : synsets.entrySet()) {
      final String normInput = synsetEntry.getKey();
      final List<Synset> synsetValues = synsetEntry.getValue();

      for (Synset synset : synsetValues) {
        if (synset.hasPointerDefinitions()) {
          for (PointerDefinition pointer : synset.getPointerDefinitions()) {
            warnings += doPointerChecks(synset, null, pointer);
          }
        }
        // check each word's pointer definitions
        if (synset.hasWords()) {
          for (Word word : synset.getWords()) {
            if (word.hasPointerDefinitions()) {
              for (PointerDefinition pointer : word.getPointerDefinitions()) {
                warnings += doPointerChecks(synset, word, pointer);
              }
            }
          }
        }
      }
    }

    return warnings;
  }

  private final int doPointerChecks(Synset synset, Word word, PointerDefinition pointer) {
    int warnings = 0;
    
    final String id = (word != null) ? ("word:" + word.getWordName()) : ("synset:" + synset.getSynsetName());
    final String ptrStr = "'" + pointer.getFormattedPointerDefinition() + "'";

    if (!pointer.hasHeadWord()) {
      System.out.println("WARNING: " + id + " pointer " + ptrStr + " has no headWord");
      ++warnings;
    }
    else {
      final SimpleWord headWord = pointer.getHeadWord();
      final String normHeadWord = headWord.getNormalizedWord();
      if (!synsets.containsKey(normHeadWord)) {
        System.out.println("WARNING: " + id + " pointer " + ptrStr + " headWord:" + normHeadWord + " has no synset entry");
        ++warnings;
      }
      else {
        // check that each synset pointerDefinition leads to a pointerInstance
        final List<PointerInstance> ptrs = dict.findPointers(null, synset, word, pointer);
        if (ptrs.size() == 0) {
          System.out.println("WARNING: " + id + " pointer " + ptrStr + " instance not found");
          // stopHere (at the next line), stepping in to find the problem
          dict.findPointers(null, synset, word, pointer);
          //
          ++warnings;
        }
      }
    }

    return warnings;
  }

  public static void main(String[] args) throws IOException {
    // arg0: dbFileDir

    final long startTime = System.currentTimeMillis();
    final LexDictionary dict = new LexDictionary(new LexLoader(new File(args[0])), true, true, true);
    final long loadTime = System.currentTimeMillis() - startTime;
    System.out.println("Loaded " + dict.getSynsetCount() + " synsets in " + loadTime + "ms");

    final LexChecker lexChecker = new LexChecker(dict);
    final int warnings = lexChecker.verify();

    System.out.println((warnings == 0) ? "Success" : "Failed");

    System.exit(warnings);
  }
}
