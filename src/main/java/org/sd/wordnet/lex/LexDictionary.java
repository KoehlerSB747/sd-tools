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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for synsets and adjective clusters.
 * <p>
 * @author Spencer Koehler
 */
public class LexDictionary {
  
  private LexLoader lexLoader;
  private boolean loadSynsets;
  private boolean loadAdjClusters;
  private boolean loadLexNames;

  private Map<String, List<Synset>> synsets;
  private Map<String, List<AdjectiveCluster>> adjClusters;
  private Map<String, Set<String>> lexNames;

  public LexDictionary(LexLoader lexLoader, boolean loadSynsets, boolean loadAdjClusters, boolean loadLexNames) throws IOException {
    this.lexLoader = lexLoader;
    this.loadSynsets = loadSynsets;
    this.loadAdjClusters = loadAdjClusters;
    this.loadLexNames = loadLexNames;

    this.synsets = loadSynsets ? new HashMap<String, List<Synset>>() : null;
    this.adjClusters = loadAdjClusters ? new HashMap<String, List<AdjectiveCluster>>() : null;
    this.lexNames = loadLexNames ? new HashMap<String, Set<String>>() : null;

    init();
  }

  private final void init() throws IOException {
    final DictionaryEntryHandler handler = new DictionaryEntryHandler(synsets, adjClusters, lexNames);
    lexLoader.load(handler, null);
  }

  public long getSynsetCount() {
    return lexLoader.getSynsetCount();
  }

  public Set<String> lookupLexNames(String normInput) {
    return (lexNames != null) ? lexNames.get(normInput) : null;
  }

  public List<Synset> lookupSynsets(String normInput) {
    return (synsets != null) ? synsets.get(normInput) : null;
  }

  public List<AdjectiveCluster> lookupAdjectiveClusters(String normInput) {
    return (adjClusters != null) ? adjClusters.get(normInput) : null;
  }


  /**
   * Collect all of the pointers from the synsets and their words to other words.
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param synsets  The synsets whose synset and word pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getAllPointers(List<PointerInstance> result, List<Synset> synsets) {
    if (result == null) result = new ArrayList<PointerInstance>();
    if (synsets != null) {
      for (Synset synset : synsets) {
        getAllPointers(result, synset);
      }
    }
    return result;
  }

  /**
   * Collect all of the pointers from the synset and its words to other words.
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param synset  The synset whose synset and word pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getAllPointers(List<PointerInstance> result, Synset synset) {
    if (result == null) result = new ArrayList<PointerInstance>();

    // compute pointers from synset as a whole
    getSynsetPointers(result, synset);

    // compute pointers from each synset word
    getWordPointers(result, synset);

    return result;
  }

  /**
   * Collect all of the pointers from the synset (not its words) to other words.
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param synset  The synset whose synset pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getSynsetPointers(List<PointerInstance> result, Synset synset) {
    if (result == null) result = new ArrayList<PointerInstance>();

    // compute pointers from synset as a whole
    if (synset != null && synset.hasPointerDefinitions()) {
      for (PointerDefinition pointerDef : synset.getPointerDefinitions()) {
        findPointers(result, synset, null, pointerDef);
      }
    }

    return result;
  }

  /**
   * Collect all of the pointers from words within the synset.
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param synset  The synset whose word pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getWordPointers(List<PointerInstance> result, Synset synset) {
    if (result == null) result = new ArrayList<PointerInstance>();

    // compute pointers from each synset word
    if (synset.hasWords()) {
      for (Word word : synset.getWords()) {
        getPointers(result, word);
      }
    }
    
    return result;
  }

  /**
   * Get the pointers from the given word (not including pointers from the
   * word's synset).
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param word  The word whose pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getPointers(List<PointerInstance> result, Word word) {
    if (result == null) result = new ArrayList<PointerInstance>();

    if (word != null && word.hasPointerDefinitions()) {
      for (PointerDefinition pointerDef : word.getPointerDefinitions()) {
        findPointers(result, word.getSynset(), word, pointerDef);
      }
    }

    return result;
  }

  /**
   * Get all pointers from the given word, including pointers from the
   * word's synset.
   *
   * @param result  Container to which pointers will be added (ok if null)
   * @param word  The word whose pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getAllPointers(List<PointerInstance> result, Word word) {
    if (result == null) result = new ArrayList<PointerInstance>();

    if (word != null && word.hasSynset()) {
      getSynsetPointers(result, word.getSynset());
    }
    getPointers(result, word);

    return result;
  }

  /**
   * Get all pointers (synset and word) from the satellite or target of the
   * given pointer.
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param word  The word whose pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getNextPointers(List<PointerInstance> result, PointerInstance pointer) {
    if (result == null) result = new ArrayList<PointerInstance>();

    getAllPointers(result, pointer.getSpecificTarget());

    return result;
  }

  public final List<PointerInstance> findPointers(List<PointerInstance> result, Synset sourceSynset, Word sourceWord, PointerDefinition pointerDef) {
    if (result == null) result = new ArrayList<PointerInstance>();

    if (pointerDef != null && pointerDef.hasHeadWord()) {
      if (sourceSynset == null) sourceSynset = sourceWord.getSynset();
      final List<Synset> targetSynsets = lookupSynsets(pointerDef.getHeadWord().getNormalizedWord());
      if (targetSynsets != null) {
        final String targetLexFileName =
          pointerDef.hasLexFileName() ? pointerDef.getLexFileName() :
          sourceSynset != null ? sourceSynset.getLexFileName() : null;
        for (Synset targetSynset : targetSynsets) {
          boolean gotit = false;
          if (targetLexFileName.equals(targetSynset.getLexFileName())) {
            final Word targetWord = targetSynset.findWord(pointerDef.getHeadWord());
            if (targetWord != null) {
              Word satelliteWord = null;
              if (pointerDef.hasSatelliteWord()) {
                satelliteWord = targetSynset.findWord(pointerDef.getSatelliteWord());
              }
              final PointerInstance pointerInst = new PointerInstance(sourceSynset, sourceWord, pointerDef, targetSynset, targetWord, satelliteWord);
              result.add(pointerInst);
              gotit = true;
            }
          }
          if (gotit) break;  // found the right synset, no need to consider others
        }
      }
    }

    return result;
  }


  Map<String, List<Synset>> getSynsets() { return synsets; }
  Map<String, List<AdjectiveCluster>> getAdjClusters() { return adjClusters; }
  Map<String, Set<String>> getLexNames() { return lexNames; }


  private static final class DictionaryEntryHandler implements LexLoader.EntryHandler {
    private Map<String, List<Synset>> synsets;
    private Map<String, List<AdjectiveCluster>> adjClusters;
    private Map<String, Set<String>> lexNames;

    public DictionaryEntryHandler(Map<String, List<Synset>> synsets,
                                  Map<String, List<AdjectiveCluster>> adjClusters,
                                  Map<String, Set<String>> lexNames) {
      this.synsets = synsets;
      this.adjClusters = adjClusters;
      this.lexNames = lexNames;
    }

    public void handleSynset(Synset synset) {
      if (synset.hasWords()) {
        for (Word word : synset.getWords()) {
          final String normWord = word.getNormalizedWord();

          if ("".equals(normWord)) {
            final boolean stopHere = true;
          }

          // Add to synsets
          if (synsets != null) {
            List<Synset> synsetList = synsets.get(normWord);
            if (synsetList == null) {
              synsetList = new ArrayList<Synset>();
              synsets.put(normWord, synsetList);
            }
            synsetList.add(synset);
          }

          // Add to lexNames
          if (lexNames != null) {
            Set<String> lexNameSet = lexNames.get(normWord);
            if (lexNameSet == null) {
              lexNameSet = new HashSet<String>();
              lexNames.put(normWord, lexNameSet);
            }
            final String lexName = synset.getLexFileName();
            if (lexName == null) {
              final boolean stopHere = true;
            }
            lexNameSet.add(lexName);
          }
        }
      }
    }

    public void handleAdjectiveCluster(AdjectiveCluster adjectiveCluster) {
      if (adjectiveCluster.hasSynsetGroups()) {
        for (SynsetGroup synsetGroup : adjectiveCluster.getSynsetGroups()) {
          if (synsetGroup.hasHeadSynset()) {
            final Synset headSynset = synsetGroup.getHeadSynset();
            handleSynset(headSynset);
            addAdjectiveCluster(adjectiveCluster, headSynset);
          }
          if (synsetGroup.hasSatelliteSynsets()) {
            for (Synset satelliteSynset : synsetGroup.getSatelliteSynsets()) {
              handleSynset(satelliteSynset);
              addAdjectiveCluster(adjectiveCluster, satelliteSynset);
            }
          }
        }
      }
    }

    private final void addAdjectiveCluster(AdjectiveCluster adjCluster, Synset synset) {
      if (adjClusters == null) return;

      if (synset.hasWords()) {
        for (Word word : synset.getWords()) {
          final String normWord = word.getNormalizedWord();

          List<AdjectiveCluster> adjClusterList = adjClusters.get(normWord);
          if (adjClusterList == null) {
            adjClusterList = new ArrayList<AdjectiveCluster>();
            adjClusters.put(normWord, adjClusterList);
          }
          adjClusterList.add(adjCluster);
        }
      }
    }
  }


  public static void main(String[] args) throws IOException {
    // arg0: dbFileDir
    
    final long startTime = System.currentTimeMillis();
    final LexDictionary dict = new LexDictionary(new LexLoader(new File(args[0])), true, true, true);
    final long loadTime = System.currentTimeMillis() - startTime;
    //System.out.println("Loaded " + dict.getSynsetCount() + " synsets in " + loadTime + "ms");

    boolean dump = true;
    final boolean stopHere = true;

    if (dump) {
      for (Map.Entry<String, Set<String>> lexEntry : dict.lexNames.entrySet()) {
        final String normInput = lexEntry.getKey();
        final Set<String> lexNames = lexEntry.getValue();

        System.out.print(normInput);
        for (String lexName : lexNames) {
          System.out.print("\t" + lexName);
        }
        System.out.println();
      }
    }
  }
}
