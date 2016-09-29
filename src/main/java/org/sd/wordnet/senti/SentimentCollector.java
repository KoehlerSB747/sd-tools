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


import java.util.List;
import java.util.Set;
import org.sd.util.StatsAccumulator;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.PointerDefinition;
import org.sd.wordnet.lex.PointerInstance;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;

/**
 * Utility to collect postivie/negative word sentiments.
 * <p>
 * @author Spencer Koehler
 */
public class SentimentCollector {
  
  public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL, UNKNOWN };


  private StatsAccumulator posScores;
  private StatsAccumulator negScores;
  private SynsetSelector synsetSelector;

  public SentimentCollector() {
    this.posScores = new StatsAccumulator("positive");
    this.negScores = new StatsAccumulator("negative");
    this.synsetSelector = new SynsetSelector();
  }

  /**
   * Get this instance's synsetSelector e.g., for adjustments.
   */
  public SynsetSelector getSynsetSelector() {
    return synsetSelector;
  }

  public void reset() {
    this.posScores.clear();
    this.negScores.clear();
  }

  /**
   * Add word(s) matching (case-insensitive) the given wordName, which can be
   * of the form: &lt;word&gt;&lt;lexId&gt; or &lt;lexFileName&gt;:&lt;word&gt;&lt;lexId&gt;, for lexId &gt; 0.
   * <p>
   * When lexFileName is used in the wordName, it overrides any lexFileNameHint.
   *
   * @param wordName  The word name to find.
   * @param lexFileNameHint  Hint to narrow the scope for finding the right word
   *                         (okay if null).
   * @param negate  true to negate the sentiment of the given word
   *
   * @return true if the word yielded sentiment information; otherwise, false.
   */
  public boolean addWord(LexDictionary lexDictionary, String wordName, String lexFileNameHint, boolean negate) {
    boolean result = false;

    final List<Word> words = lexDictionary.findWords(wordName, lexFileNameHint);
    if (words != null) {
      for (Word word : words) {
        result |= doAddWord(lexDictionary, word, negate);
      }
    }

    return result;
  }

  private final boolean doAddWord(LexDictionary lexDictionary, Word word, boolean negate) {
    boolean result = false;

    final Synset synset = word.getSynset();
    if (synset != null && synsetSelector.hasQualifyingPartOfSpeech(synset)) {

      if (synset.hasSentiment()) {
        if (negate) {
          result = addPositiveScore(synset.getNegScore());
          result |= addNegativeScore(synset.getPosScore());
        }
        else {
          result = addPositiveScore(synset.getPosScore());
          result |= addNegativeScore(synset.getNegScore());
        }
      }
      else {
        final List<PointerInstance> pointers = lexDictionary.getForwardPointers(null, word);
        if (pointers != null) {
          for (PointerInstance pointer : pointers) {
            // check isa(hpyernym) word/synset for sentiment
            final PointerDefinition ptrDef = pointer.getPointerDef();
            if ("@".equals(ptrDef.getPointerSymbol())) {
              final Word hypernym = pointer.getSpecificTarget();
              if (hypernym != null && doAddWord(lexDictionary, hypernym, negate)) {
                result = true;

                // copy the parent's sentiment into this word's synset
                synset.setSentiment(hypernym.getSynset().getSentiment());

                break;
              }
            }
          }
        }
      }
    }

    return result;
  }

  public boolean addPositiveScore(double posScore) {
    boolean result = false;

    if (posScore > 0) {
      posScores.add(posScore);
      result = true;
    }

    return result;
  }

  public boolean addNegativeScore(double negScore) {
    boolean result = false;

    if (negScore > 0) {
      negScores.add(negScore);
      result = true;
    }

    return result;
  }

  public Sentiment calculateSentiment() {
    Sentiment result = Sentiment.UNKNOWN;

    if (posScores.getN() > 0 || negScores.getN() > 0) {
      if (negScores.getN() == 0) {
        result = Sentiment.POSITIVE;
      }
      else if (posScores.getN() == 0) {
        result = Sentiment.NEGATIVE;
      }
      else {
        result = Sentiment.NEUTRAL;

        final double posStdDev = posScores.getN() == 1 ? 0 : posScores.getStandardDeviation();
        final double posMax = posScores.getMean() + posStdDev;
        final double posMin = posScores.getMean() - posStdDev;

        final double negStdDev = negScores.getN() == 1 ? 0 : negScores.getStandardDeviation();
        final double negMax = negScores.getMean() + negStdDev;
        final double negMin = negScores.getMean() - negStdDev;

        if (posMax > negMax) {
          if (posMin >= negMin) {
            result = Sentiment.POSITIVE;
          }
        }
        else if (negMax > posMax) {
          if (negMin >= posMin) {
            result = Sentiment.NEGATIVE;
          }
        }
      }
    }

    return result;
  }

  public StatsAccumulator getPositiveScores() {
    return posScores;
  }

  public StatsAccumulator getNegativeScores() {
    return negScores;
  }

  public String getPositiveSummary() {
    // [min, ave +/- stddev, max](n)
    return getStatsSummary(posScores);
  }

  public String getNegativeSummary() {
    return getStatsSummary(negScores);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append(calculateSentiment()).
      append(" pos=").
      append(getPositiveSummary()).
      append(" neg=").
      append(getNegativeSummary());

    return result.toString();
  }

  private final String getStatsSummary(StatsAccumulator stats) {
    String result = null;

    switch ((int)(stats.getN())) {
      case 0 :
        result = "0";
        break;
      case 1 :
        result =
          String.format("[%1.4f, %1.4f, %1.4f](%d)",
                        stats.getMin(),
                        stats.getMean(),
                        stats.getMax(),
                        stats.getN());
        break;
      default :
        result =
          String.format("[%1.4f, %1.4f +/- %1.4f, %1.4f](%d)",
                        stats.getMin(),
                        stats.getMean(),
                        stats.getStandardDeviation(),
                        stats.getMax(),
                        stats.getN());
        break;
    }

    return result;
  }
}
