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
package org.sd.wordnet.rel;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.atnexec.ConfigUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.util.Histogram;
import org.sd.util.LRU;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.SynsetContainer;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.wordnet.util.NormalizeUtil;
import org.sd.xml.DataProperties;

/**
 * Utility to select words from synsets that are most closely related.
 * <p>
 * @author Spencer Koehler
 */
public class RelatedWordPicker {
  
  private LexDictionary dict;
  private LRU<String, ExpandedWord> cache;

  public RelatedWordPicker(LexDictionary dict) {
    this.dict = dict;
    this.cache = new LRU<String, ExpandedWord>(100);
  }

  /**
   * Given related input tokens, select one word from each that
   * are most closely related.
   * <p>
   * If the tokens are not relatable (e.g., one is null or has no synsets),
   * then return null.
   */
  public Comparison selectWords(String token1, String token2) {
    return selectWords(getSynsetContainer(token1), getSynsetContainer(token2));
  }

  public final SynsetContainer getSynsetContainer(String token) {
    final String norm = NormalizeUtil.normalizeForLookup(token);
    final SynsetContainer result = new SynsetContainer().
      setInput(token).setNorm(norm).setSynsets(dict.lookupSynsets(norm));
    return result;
  }

  /**
   * Given related input tokens, select one word from each that
   * are most closely related.
   * <p>
   * If the tokens are not relatable (e.g., one is null or has no synsets),
   * then return null.
   */
  public Comparison selectWords(SynsetContainer token1, SynsetContainer token2) {
    Comparison min = null;

    if (token1 != null && token2 != null && token1.hasSynsets() && token2.hasSynsets()) {
      final List<Synset> synsetlist1 = token1.getSynsets();
      final List<Synset> synsetlist2 = token2.getSynsets();
      Synset[] synsets1 = synsetlist1.toArray(new Synset[synsetlist1.size()]);
      Synset[] synsets2 = synsetlist2.toArray(new Synset[synsetlist2.size()]);
      int len1 = synsets1.length;
      int len2 = synsets2.length;

      if (len2 < len1) { // swap
        int itmp = len2;
        len2 = len1;
        len1 = itmp;

        Synset[] stmp = synsets2;
        synsets2 = synsets1;
        synsets1 = stmp;
      }

      for (int idx1 = 0; idx1 < len1; ++idx1) {
        final Synset synset1 = synsets1[idx1];
        for (int idx2 = idx1; idx2 < len2; ++idx2) {
          final Synset synset2 = synsets2[idx2];
          final Comparison cmp = getClosestWords(synset1, synset2);
          if (cmp != null) {
            if (min != null && min.asSynsetComparison() != null && cmp.asSynsetComparison() != null) {
              min.asSynsetComparison().addOther(cmp.asSynsetComparison());
            }
            else if (min == null || min.getMinDepth() > cmp.getMinDepth()) {
              min = cmp;
            }
          }
        }
      }
    }

    return min;
  }

  public Comparison getClosestWords(Synset synset1, Synset synset2) {
    Comparison result = null;

    if (synset1 != null && synset2 != null && synset1.hasWords() && synset2.hasWords()) {
      if (synset1 == synset2) {
        result = new SynsetComparison(synset1, synset2);
      }
      else {
        result = getClosestWords(synset1.getWords(), synset2.getWords());
      }
    }

    return result;
  }

  public WordComparison getClosestWords(List<Word> wordlist1, List<Word> wordlist2) {
    WordComparison result = null;

    if (wordlist1 == null || wordlist2 == null || wordlist1.size() == 0 || wordlist2.size() == 0) return result;

    Word[] words1 = wordlist1.toArray(new Word[wordlist1.size()]);
    Word[] words2 = wordlist2.toArray(new Word[wordlist2.size()]);
    int len1 = words1.length;
    int len2 = words2.length;

    if (len2 < len1) {  // swap
      int itmp = len2;
      len2 = len1;
      len1 = itmp;

      Word[] wtmp = words2;
      words2 = words1;
      words1 = wtmp;
    }

    // Find the words from each with the lowest comparison (depth),
    // meaning most closely related with fewest relationship hops
    for (int idx1 = 0; idx1 < len1; ++idx1) {
      final Word word1 = words1[idx1];
      final ExpandedWord eword1 = getExpandedWord(word1);
      for (int idx2 = idx1; idx2 < len2; ++idx2) {
        final Word word2 = words2[idx2];
        final ExpandedWord eword2 = getExpandedWord(word2);
        final WordComparison wcmp = new WordComparison(eword1, eword2);
        if (wcmp.hasResult()) {
          if (result == null || result.getMinDepth() > wcmp.getMinDepth()) {
            result = wcmp;
          }
        }
      }
    }

    return result;
  }

  private final ExpandedWord getExpandedWord(Word word) {
    if (word == null) return null;
    final String wordName = word.getQualifiedWordName();
    ExpandedWord result = cache.get(wordName);
    if (result == null) {
      result = new ExpandedWord(word, dict);
      cache.put(wordName, result);
    }
    return result;
  }


  public static interface Comparison {
    public boolean hasResult();
    public int getMinDepth();
    public Word[] getWords();

    public WordComparison asWordComparison();
    public SynsetComparison asSynsetComparison();
  }

  public static final class WordComparison implements Comparison {
    public final ExpandedWord word1;
    public final ExpandedWord word2;
    public final int depth1;  // word2's depth in word1's tree
    public final int depth2;  // word1's depth in word2's tree
    private int minDepth;
    private Word[] words;

    public WordComparison(ExpandedWord word1, ExpandedWord word2) {
      this.word1 = word1;
      this.word2 = word2;
      this.depth1 = (word1 != null && word2 != null) ? word1.getDepth(word2.getRootWordName()) : -1;
      this.depth2 = (word1 != null && word2 != null) ? word2.getDepth(word1.getRootWordName()) : -1;
      this.minDepth = depth1 <= depth2 ? depth1 : depth2;
      this.words = (word1 != null && word2 != null) ? new Word[]{word1.getRootWord(), word2.getRootWord()} : null;
    }

    public boolean hasResult() {
      return minDepth >= 0;
    }

    public int getMinDepth() {
      return minDepth;
    }

    public Word[] getWords() {
      return words;
    }

    public WordComparison asWordComparison() {
      return this;
    }

    public SynsetComparison asSynsetComparison() {
      return null;
    }
  }

  public static final class SynsetComparison implements Comparison {
    private Set<Word> words;

    public SynsetComparison(Synset synset1, Synset synset2) {
      this.words = new HashSet<Word>();
      addSynset(synset1);
      addSynset(synset2);
    }

    private final void addSynset(Synset synset) {
      if (synset != null && synset.hasWords()) {
        words.add(synset.getSynsetWord());
      }
    }

    public void addOther(SynsetComparison comparison) {
      this.words.addAll(comparison.words);
    }

    public boolean hasResult() {
      return words.size() > 0;
    }

    public int getMinDepth() {
      return 0;
    }

    public Word[] getWords() {
      return words.size() > 0 ? words.toArray(new Word[words.size()]) : null;
    }

    public WordComparison asWordComparison() {
      return null;
    }

    public SynsetComparison asSynsetComparison() {
      return this;
    }
  }


  private static final String buildOutputLine(int dist, String input1, String input2, Word[] words) {
    final StringBuilder result = new StringBuilder();

    result.
      append(dist).append('\t').
      append(input1).append('\t').
      append(input2);

    if (words != null) {
      for (Word word : words) {
        result.append('\t').append(word.getQualifiedWordName());
      }
    }

    return result.toString();
  }

  public static void main(String[] args) {
    // Properties:
    //   ...
    // Args: words to compare

    //
    // Proposed word sense disambiguation algorithm:
    // - Train word2vec on a corpus (e.g. survey responses)
    //   - cat *.corpus | grep -v\# | java ....W2VModelBuilder > ... .model
    // - Run corpus through syntaxnet to generate parses
    //   - cat *.corpus | grep -v\# | syntaxnet --mode conll > ... .conll
    // - Run .conll through ConllTopicGenerator, which  *** I'm here !!!
    //   - uses word2vec to find N(=10?) nearest words
    //   - selects synset as top histogram result(s) as generated below
    //     - conceptually,
    //       - syntaxnet identifies important topic words
    //       - word2vec connects those words that are closely (word embedding vector)related to each other
    //       - wordnet relationship are used to narrow word meanings to common/(wordnet pointer)related word senses
    //


    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    args = dataProperties.getRemainingArgs();

    final LexDictionary lexDictionary = WordNetLoader.loadLexDictionary(dataProperties);
    final RelatedWordPicker picker = new RelatedWordPicker(lexDictionary);
    final Histogram<String> histogram = new Histogram<String>();

    for (int i = 1; i < args.length; ++i) {
      final Comparison selectedWords = picker.selectWords(args[0], args[i]);
      Word[] words = null;
      int dist = -1;
      if (selectedWords != null) {
        words = selectedWords.getWords();
        dist = selectedWords.getMinDepth();
      }
      System.out.println(buildOutputLine(dist, args[0], args[i], words));

      if (words != null) {
        for (Word word : words) histogram.add(word.getQualifiedWordName());
      }
    }

    if (histogram.getNumRanks() > 0) {
      System.out.println("\n" + histogram.toString());
    }
  }
}
