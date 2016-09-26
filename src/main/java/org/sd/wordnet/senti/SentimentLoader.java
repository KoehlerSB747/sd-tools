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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.sd.io.FileUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Utility to load sentiment into synsets.
 * <p>
 * @author Spencer Koehler
 */
public class SentimentLoader {
  
  private LexDictionary lexDictionary;
  private boolean verbose;

  public SentimentLoader(LexDictionary lexDictionary) {
    this.lexDictionary = lexDictionary;
    this.verbose = false;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void loadSentiWordNet(File sentiWordNet) {
    BufferedReader reader = null;
    String line = null;
    try {
      reader = FileUtil.getReader(sentiWordNet);
      while ((line = reader.readLine()) != null) {
        if ("".equals(line) || line.charAt(0) == '#') continue;
        processSentiWordNetLine(line);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    finally {
      if (reader != null) {
        try {
          reader.close();
        }
        catch (Exception ioe) {
          throw new IllegalStateException("Unabled to close reader for " + sentiWordNet.getAbsolutePath(), ioe);
        }
      }
    }
  }

  private final void processSentiWordNetLine(String sentiWordNetLine) {
    final String[] fields = sentiWordNetLine.split("\\t");

    // pos id posScore negScore synsetTerms gloss
    if ("0".equals(fields[2]) && "0".equals(fields[3])) return;  // skip undefined entries

    final String pos = fields[0];
    final String synsetTerms = fields[4];
    final String gloss = fields.length > 5 ? fields[5] : "";

    final Synset synset = findSynset(pos, synsetTerms, gloss);

    if (synset == null) {
      if (verbose) {
        System.out.println("WARNING: SentimentLoader can't find synset for line=" + sentiWordNetLine);
      }
    }
    else {
      synset.setSentiment(Double.parseDouble(fields[2]), Double.parseDouble(fields[3]));
    }
  }

  private final Synset findSynset(String pos, String synsetTerms, String gloss) {
    Synset result = null;

    if ("".equals(synsetTerms)) return result;

    final Set<String> terms = new LinkedHashSet<String>();
    final String[] termStrings = synsetTerms.split("\\s+");
    for (String termString : termStrings) {
      if ("#".equals(termString)) continue;
      terms.add(NormalizeUtil.normalizeForLookup(termString.split("#")[0]));
    }

    TreeSet<Alignment> bestAlignments = null;
    for (String term : terms) {
      final List<Synset> synsets = lexDictionary.lookupSynsets(term, false);
      if (synsets != null) {
        if (synsets.size() == 1) {
          // if only one synset, it's the match
          result = synsets.get(0);
        }
        else {
          // find the best matching synset
          TreeSet<Alignment> alignments = null;
          for (Synset synset : synsets) {
            final Alignment alignment = new Alignment(synset, terms, pos, gloss);
            if (alignment.isExact()) {
              result = alignment.synset;
              break;
            }
            else {
              if (alignments == null) alignments = new TreeSet<Alignment>();
              alignments.add(new Alignment(synset, terms, pos, gloss));
            }
          }
          if (result != null) break;
          
          if (alignments != null) {
            final Alignment best = alignments.first();
            if (best.matches()) {
              if (bestAlignments == null) bestAlignments = new TreeSet<Alignment>();
              bestAlignments.add(best);
            }
          }
        }
      }
    }

    if (result == null && bestAlignments != null) {
      result = bestAlignments.first().synset;
    }

    //         if (gloss.equalsIgnoreCase(synset.getGloss())) {
    //           //todo: match "pos" if warranted?
    //           result = synset;
    //           break;
    //         }
    //       }

    //       if (result == null) {
    //         // didn't match gloss, see if other terms match
    //       }
    //     }
    //     if (result != null) break;
    //   }
    // }

    return result;
  }

  private static final class Alignment implements Comparable<Alignment> {
    public final Synset synset;
    public final Set<String> terms;
    public final String pos;
    public final String gloss;

    private String[] glossWords;
    private String[] synsetWords;

    private boolean exact;
    private int score;

    public Alignment(Synset synset, Set<String> terms, String pos, String gloss) {
      this.synset = synset;
      this.terms = terms;
      this.pos = pos;
      this.gloss = gloss;

      this.glossWords = normalizeAndSplit(gloss);
      this.synsetWords = normalizeAndSplit(synset.getGloss());

      this.exact = isExactMatch();
      this.score = computeScore();
    }
    
    private final boolean isExactMatch() {
      boolean result = false;

      if (glossesMatch()) {
        result = true;
      }

      return result;
    }

    private final int computeScore() {
      if (isExactMatch()) return Integer.MAX_VALUE;

      // the higher the better
      return glossMatchCount() + termMatchScore();
    }

    /** @return true if the match is exact */
    public boolean isExact() {
      return exact;
    }

    /** @return true if there is at least a partial match */
    public boolean matches() {
      return score > 0;
    }

    public int compareTo(Alignment other) {
      // sort from highest to lowest scores
      return other.score - this.score;
    }

    private final boolean glossesMatch() {
      boolean result = false;

      if (glossWords.length == synsetWords.length) {
        result = true;
        for (int i = 0; i < glossWords.length; ++i) {
          if (!glossWords[i].equals(synsetWords[i])) {
            result = false;
            break;
          }
        }
      }

      return result;
    }

    private final int glossMatchCount() {
      int result = 0;

      String[] s1 = glossWords;
      String[] s2 = synsetWords;

      if (s1.length > s2.length) {
        s1 = synsetWords;
        s2 = glossWords;
      }

      // create a base set from that with more words
      final Set<String> base = new HashSet<String>();
      for (String word : s2) base.add(word);

      // count the number of fewer words in the base
      for (String word : s1) {
        if (base.contains(word)) ++result;
      }

      return result;
    }

    private final int termMatchScore() {
      int result = 0;

      // add 10 if we have the same number of terms as synset words
      if (synset.size() == terms.size()) result += 10;

      // add 10 for each synset word / term match beyond the first match
      // subtract 5 for each synset word not matching a term
      // subtract 5 for each term not matching a synset word
      boolean foundMatch = false;
    
      int numMatches = 0;
      for (Word word : synset.getWords()) {
        final String text = word.getNormalizedWord();
        if (terms.contains(text)) {
          ++numMatches;
          if (!foundMatch) {
            foundMatch = true;
          }
          else result += 10;  // synset word beyond first matches term
        }
        else {
          // synset word not matching a term
          result -= 5;
        }
      }

      // terms not matching synset words
      result -= ((terms.size() - numMatches) * 5);

      return result;
    }

    private final String[] normalizeAndSplit(String string) {
      return string.toLowerCase().split("\\W+");
    }
  }
}
