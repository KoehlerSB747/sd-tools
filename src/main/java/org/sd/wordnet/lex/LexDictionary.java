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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Container for synsets and adjective clusters.
 * <p>
 * @author Spencer Koehler
 */
public class LexDictionary {
  
  private LexLoader lexLoader;
  private MorphTool morphTool;
  public final boolean loadSynsets;
  public final boolean loadAdjClusters;
  public final boolean loadLexNames;
  public final boolean loadReversePointers;
  private int maxSpaceCount;

  private Map<String, List<Synset>> synsets;
  private Map<String, List<AdjectiveCluster>> adjClusters;
  private Map<String, Set<String>> lexNames;
  private Map<String, List<ReversePointer>> revPtrs; // simpleWord.name -> reversePointer

  // for morphological derivations
  private Map<String, List<Synset>> dsynsets;
  private Map<String, Set<String>> dlexNames;

  public LexDictionary(LexLoader lexLoader) throws IOException {
    this(lexLoader, true, true, true, true);
  }

  public LexDictionary(LexLoader lexLoader, boolean loadSynsets, boolean loadAdjClusters, boolean loadLexNames, boolean loadReversePointers) throws IOException {
    this.lexLoader = lexLoader;
    this.morphTool = new MorphTool(lexLoader.getDbFileDir().getParentFile());
    this.morphTool.setArchaic(true);  //todo: parameterize this
    this.loadSynsets = loadSynsets;
    this.loadAdjClusters = loadAdjClusters;
    this.loadLexNames = loadLexNames;
    this.loadReversePointers = loadReversePointers;
    this.maxSpaceCount = 0;

    this.synsets = loadSynsets ? new HashMap<String, List<Synset>>() : null;
    this.adjClusters = loadAdjClusters ? new HashMap<String, List<AdjectiveCluster>>() : null;
    this.lexNames = loadLexNames ? new HashMap<String, Set<String>>() : null;
    this.revPtrs = loadReversePointers ? new HashMap<String, List<ReversePointer>>() : null;

    this.dsynsets = null;
    this.dlexNames = null;

    init();
  }

  Map<String, List<Synset>> getSynsets() { return synsets; }
  Map<String, List<AdjectiveCluster>> getAdjClusters() { return adjClusters; }
  Map<String, Set<String>> getLexNames() { return lexNames; }
  Map<String, List<ReversePointer>> getRevPtrs() { return revPtrs; }

  private final void init() throws IOException {
    final DictionaryEntryHandler handler = new DictionaryEntryHandler(synsets, adjClusters, lexNames, revPtrs);
    lexLoader.load(handler, null);
    this.maxSpaceCount = handler.getMaxSpaceCount();
  }

  public long getSynsetCount() {
    return lexLoader.getSynsetCount();
  }

  public int getMaxSpaceCount() {
    return maxSpaceCount;
  }

  public Set<String> lookupLexNames(String normInput) {
    return lookupLexNames(normInput, true);
  }

  public Set<String> lookupLexNames(String normInput, boolean morph) {
    if (lexNames == null) return null;

    Set<String> result = lexNames.get(normInput);

    if (morph) {
      final Set<String> dresult = lookupDerivedLexNames(normInput);
      if (dresult != null) {
        if (result != null) {
          final Set<String> combined = new HashSet<String>(result);
          combined.addAll(dresult);
          result = combined;
        }
        else {
          result = dresult;
        }
      }
    }

    return result;
  }

  public Set<String> lookupDerivedLexNames(String normInput) {
    Set<String> result = dlexNames == null ? null : dlexNames.get(normInput);

    if (result == null) {
      final Collection<MorphTool.Derivation> derivations = morphTool.deriveBaseForms(normInput);
      if (derivations != null) {
        for (MorphTool.Derivation derivation : derivations) {
          final Set<String> dLexNames = lexNames.get(derivation.baseForm);
          if (dLexNames != null) {
            if (result != null) result = new HashSet<String>(result);
            for (String dLexName : dLexNames) {
              // only add valid, if we have the synsets to verify; else add all
              if (synsets == null || synsets.containsKey(dLexName)) {
                if (result == null) result = new HashSet<String>();
                result.add(dLexName);
              }
            }

            if (result != null) {
              // preserve computation for subsequent lookups
              if (dlexNames == null) dlexNames = new HashMap<String, Set<String>>();
              dlexNames.put(normInput, result);
            }
          }
        }
      }
    }

    return result;
  }

  public List<Synset> lookupSynsets(String normInput) {
    return lookupSynsets(normInput, true);
  }

  public List<Synset> lookupSynsets(String normInput, boolean morph) {
    if (synsets == null) return null;

    List<Synset> result = synsets.get(normInput);

    if (morph) {
      final List<Synset> dresult = lookupDerivedSynsets(normInput);
      if (dresult != null) {
        if (result != null) {
          final List<Synset> combined = new ArrayList<Synset>(result);
          combined.addAll(dresult);
          result = combined;
        }
        else {
          result = dresult;
        }
      }
    }

    return result;
  }

  public List<Synset> lookupDerivedSynsets(String normInput) {
    List<Synset> result = dsynsets == null ? null : dsynsets.get(normInput);

    if (result == null) {
      final Collection<MorphTool.Derivation> derivations = morphTool.deriveBaseForms(normInput);
      if (derivations != null) {
        for (MorphTool.Derivation derivation : derivations) {
          final List<Synset> dSynsets = synsets.get(derivation.baseForm);
          if (dSynsets != null) {
            for (Synset dSynset : dSynsets) {
              if (derivation.matchesPOS(dSynset.getLexFileName())) {
                if (result == null) result = new ArrayList<Synset>();
                result.add(dSynset);
              }
            }

            if (result != null) {
              // preserve computation for subsequent lookups
              if (dsynsets == null) dsynsets = new HashMap<String, List<Synset>>();
              dsynsets.put(normInput, result);
            }
          }
        }
      }
    }
    
    return result;
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
  public List<PointerInstance> getForwardPointers(List<PointerInstance> result, List<Synset> synsets) {
    if (result == null) result = new ArrayList<PointerInstance>();
    if (synsets != null) {
      for (Synset synset : synsets) {
        getForwardPointers(result, synset);
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
  public List<PointerInstance> getForwardPointers(List<PointerInstance> result, Synset synset) {
    if (result == null) result = new ArrayList<PointerInstance>();

    // compute pointers from synset as a whole
    getSynsetPointers(result, synset);

    // compute pointers from each synset word
    getWordPointers(result, synset);

    return result;
  }

  /**
   * Collect all of the forward pointers from the synset (not its words) to other words.
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
   * Collect all of the forward pointers from words within the synset.
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
   * Get the forward pointers from the given word (not including pointers from the
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
  public List<PointerInstance> getForwardPointers(List<PointerInstance> result, Word word) {
    if (result == null) result = new ArrayList<PointerInstance>();

    if (word != null && word.hasSynset()) {
      getSynsetPointers(result, word.getSynset());
    }
    getPointers(result, word);

    return result;
  }

  /**
   * Get all forward pointers (synset and word) from the satellite or target of the
   * given pointer.
   * 
   * @param result  Container to which pointers will be added (ok if null)
   * @param word  The word whose pointers to follow
   *
   * @return the result
   */
  public List<PointerInstance> getNextPointers(List<PointerInstance> result, PointerInstance pointer) {
    if (result == null) result = new ArrayList<PointerInstance>();

    getForwardPointers(result, pointer.getSpecificTarget());

    return result;
  }

  public final List<String> getQualifiedWordNames(String wordName) {
    final List<String> result = new ArrayList<String>();

    if (wordName.indexOf(':') < 0) {
      final List<Word> words = findWords(wordName, null);
      if (words != null) {
        for (Word word : words) {
          result.add(word.getQualifiedWordName());
        }
      }
    }

    if (result.size() == 0) {
      result.add(wordName);
    }

    return result;
  }

  /**
   * Get all pointers that point to the given word.
   */
  public List<PointerInstance> getReversePointers(List<PointerInstance> result, String wordName, Word theWord, int maxDist, String symbolConstraint) {
    if (revPtrs == null) return null;

    if (result == null) result = new ArrayList<PointerInstance>();

    if (theWord != null) {
      doGetReversePointers(result, theWord.getQualifiedWordName(), theWord, maxDist, symbolConstraint);
    }
    else {
      final List<String> qualifiedWordNames = getQualifiedWordNames(wordName);

      for (String qualifiedWordName : qualifiedWordNames) {
        doGetReversePointers(result, qualifiedWordName, null, maxDist, symbolConstraint);
      }
    }
          
    return result;
  }

  private final void doGetReversePointers(List<PointerInstance> result, String qualifiedWordName, Word nextTargetWord, int maxDist, String symbolConstraint) {
    final LinkedList<PointerInstance> queue = new LinkedList<PointerInstance>();
    PointerInstance ptrInstance = null;

    final Set<Long> seenPtrUids = new HashSet<Long>();

    while (qualifiedWordName != null) {

      if (ptrInstance == null || maxDist <= 0 || ptrInstance.getChainLength() <= maxDist) {
        final List<ReversePointer> revPtrList = revPtrs.get(qualifiedWordName);
        if (revPtrList != null) {
          for (ReversePointer revPtr : revPtrList) {
            final PointerDefinition ptrDef = revPtr.getSourcePointerDefinition();
            if (seenPtrUids.contains(ptrDef.getUID())) continue;
            seenPtrUids.add(ptrDef.getUID());
            if (symbolConstraint == null || symbolConstraint.equals(ptrDef.getPointerSymbol())) {
              final Word sourceWord = revPtr.hasSourceWord() ? revPtr.getSourceWord() :
                revPtr.getSourceSynset().getWords().get(0);  // add first synset word
              final List<PointerInstance> ptrInstances =
                buildReversePointerInstances(revPtr, sourceWord, null, nextTargetWord, ptrInstance);
              if (ptrInstances != null) queue.addAll(ptrInstances);
            }
          }
        }
      }

      qualifiedWordName = null;
      while (queue.size() > 0 && qualifiedWordName == null) {
        ptrInstance = queue.removeFirst();
        result.add(ptrInstance);

        nextTargetWord = ptrInstance.getSourceWord();
        if (nextTargetWord != null) {
          qualifiedWordName = nextTargetWord.getQualifiedWordName();
        }
      }
    }
  }

  private final List<PointerInstance> buildReversePointerInstances(ReversePointer revPtr, Word sourceWord, Synset sourceSynset, Word targetWord, PointerInstance nextPtrInstance) {
    List<PointerInstance> result = null;

    final PointerDefinition ptrDef = revPtr.getSourcePointerDefinition();
    if (sourceSynset == null && sourceWord != null) sourceSynset = sourceWord.getSynset();

    if (targetWord != null) {
      final PointerInstance ptrInst = new PointerInstance(sourceSynset, sourceWord, ptrDef, targetWord.getSynset(), targetWord, null);
      ptrInst.setNextPointerInstance(nextPtrInstance);
      if (result == null) result = new ArrayList<PointerInstance>();
      result.add(ptrInst);
    }
    else {
      final List<Word> targetWords =
        findWords(ptrDef.getSpecificTarget(), ptrDef.hasLexFileName() ?
                  ptrDef.getLexFileName() :
                  sourceWord.getSynset().getLexFileName());

      if (targetWords != null) {
        for (Word word : targetWords) {
          final PointerInstance ptrInst = new PointerInstance(sourceSynset, sourceWord, ptrDef, word.getSynset(), word, null);  //todo: handle head -vs- satellite words better here
          ptrInst.setNextPointerInstance(nextPtrInstance);
          if (result == null) result = new ArrayList<PointerInstance>();
          result.add(ptrInst);
        }
      }
    }

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

  /**
   * Find word(s) matching (case-insensitive) the given wordName, which can be
   * of the form: <word><lexId> or <lexFileName>:<word><lexId>, for lexId &gt; 0.
   * <p>
   * When lexFileName is used in the wordName, it overrides any lexFileNameHint.
   *
   * @param wordName  The word name to find.
   * @param lexFileNameHint  Hint to narrow the scope for finding the right word
   *                         (okay if null).
   *
   * @return the found word(s)
   */
  public final List<Word> findWords(String wordName, String lexFileNameHint) {
    if (synsets == null) return null;

    List<Word> result = null;

    final int cPos = wordName.indexOf(':');
    if (cPos >= 0) {
      lexFileNameHint = wordName.substring(0, cPos);
      wordName = wordName.substring(cPos + 1);
    }

    final String norm = NormalizeUtil.normalizeForLookup(wordName);
    final List<Synset> synsets = lookupSynsets(norm);

    if (synsets != null) {
      for (Synset synset : synsets) {
        if (lexFileNameHint == null || lexFileNameHint.equalsIgnoreCase(synset.getLexFileName())) {
          for (Word word : synset.getWords()) {
            if (wordName.equals(word.getWordName())) {
              if (result == null) result = new ArrayList<Word>();
              result.add(word);
              break;
            }
          }
          if (lexFileNameHint != null && result != null) break;
        }
      }
    }

    return result;
  }

  public final List<Word> findWords(SimpleWord simpleWord, String lexFileNameHint) {
    if (synsets == null) return null;

    List<Word> result = null;

    final List<Synset> synsets = lookupSynsets(simpleWord.getNormalizedWord());

    if (synsets != null) {
      for (Synset synset : synsets) {
        if (lexFileNameHint == null || lexFileNameHint.equalsIgnoreCase(synset.getLexFileName())) {
          final Word word = synset.findWord(simpleWord);
          if (word != null) {
            if (result == null) result = new ArrayList<Word>();
            result.add(word);
          }
          if (lexFileNameHint != null) break;
        }
      }
    }

    return result;
  }


  private static final class DictionaryEntryHandler implements LexLoader.EntryHandler {
    private Map<String, List<Synset>> synsets;
    private Map<String, List<AdjectiveCluster>> adjClusters;
    private Map<String, Set<String>> lexNames;
    private Map<String, List<ReversePointer>> revPtrs;
    private int maxSpaceCount;

    public DictionaryEntryHandler(Map<String, List<Synset>> synsets,
                                  Map<String, List<AdjectiveCluster>> adjClusters,
                                  Map<String, Set<String>> lexNames,
                                  Map<String, List<ReversePointer>> revPtrs) {
      this.synsets = synsets;
      this.adjClusters = adjClusters;
      this.lexNames = lexNames;
      this.revPtrs = revPtrs;
      this.maxSpaceCount = 0;
    }

    public int getMaxSpaceCount() {
      return maxSpaceCount;
    }

    public void handleSynset(Synset synset) {
      if (revPtrs != null && synset.hasPointerDefinitions()) {
        // add to revPtrs
        addReversePointers(synset.getPointerDefinitions(), synset, null);
      }

      if (synset.hasWords()) {
        for (Word word : synset.getWords()) {
          final String normWord = word.getNormalizedWord();

          if ("".equals(normWord)) {
            final boolean stopHere = true;
          }

          // Update maxSpaceCount
          final int spaceCount = word.getSpaceCount();
          if (spaceCount > maxSpaceCount) {
            maxSpaceCount = spaceCount;
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

          // Add to revPtrs
          if (revPtrs != null && word.hasPointerDefinitions()) {
            addReversePointers(word.getPointerDefinitions(), null, word);
          }
        }
      }
    }

    private final void addReversePointers(List<PointerDefinition> ptrDefs, Synset synset, Word word) {
      for (PointerDefinition ptrDef : ptrDefs) {
        if (word == null) {
          // add synset pointers from each synset word to the pointer target
          for (Word synsetWord : synset.getWords()) {
            doAddReversePointer(ptrDef, synset, synsetWord);
          }
        }
        else {
          // add word pointer from word to the pointer target
          doAddReversePointer(ptrDef, synset, word);
        }
      }
    }

    private final void doAddReversePointer(PointerDefinition ptrDef, Synset synset, Word word) {
      final String qualifiedName = ptrDef.getSpecificTargetQualifiedName(word != null ? word.getSynset().getLexFileName() : synset.getLexFileName());
      List<ReversePointer> revPtrList = revPtrs.get(qualifiedName);
      if (revPtrList == null) {
        revPtrList = new ArrayList<ReversePointer>();
        revPtrs.put(qualifiedName, revPtrList);
      }
      revPtrList.add(new ReversePointer(ptrDef, synset, word));
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
    final LexDictionary dict = new LexDictionary(new LexLoader(new File(args[0])));
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
