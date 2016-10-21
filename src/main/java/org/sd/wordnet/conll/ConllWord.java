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
package org.sd.wordnet.conll;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sd.nlp.conll.ConllField;
import org.sd.nlp.conll.ConllSentence;
import org.sd.nlp.conll.ConllToken;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.SynsetContainer;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Container for a ConllToken and its associated Synsets.
 * <p>
 * @author Spencer Koehler
 */
public class ConllWord extends SynsetContainer {
  
  public static final ConllWord getInstance(LexDictionary lexDictionary, ConllToken token) {
    if (token == null) return null;
    
    final ConllWord result = new ConllWord();

    final String input = token.getText();
    final String norm = NormalizeUtil.normalizeForLookup(input);

    result.setToken(token).setInput(input).setNorm(norm);

    if (lexDictionary != null) {
      result.setSynsets(lexDictionary.lookupSynsets(norm));

      if (result.hasSynsets()) {
        result.constrain(new PosConstraintFunction(token));
      }
    }

    return result;
  }

  public static final ConllWord getInstance(LexDictionary lexDictionary, ConllSentence sentence, ConllToken token) {
    if (token == null) return null;
    if (sentence == null) return getInstance(lexDictionary, token);

    final ConllWord result = new ConllWord();

    // check for compound words
    final Set<ConllToken> tokens = new TreeSet<ConllToken>();
    tokens.add(token);

    // collect multi-word expressions (mwe), compounds, names, and particles (prt)
    final boolean isCompound = collectAll(tokens, sentence, token, new String[]{"mwe", "compound", "name", "prt"});

    // build content
    List<Synset> synsets = null;
    final StringBuilder input = new StringBuilder();
    for (ConllToken ctoken : tokens) {
      if (input.length() > 0) input.append(' ');
      input.append(ctoken.getText());
    }
    String norm = NormalizeUtil.normalizeForLookup(input.toString());

    if (lexDictionary != null) {
      synsets = lexDictionary.lookupSynsets(norm);
      if (synsets == null && isCompound) {
        // fallback to the single token
        input.setLength(0);
        input.append(token.getText());
        norm = NormalizeUtil.normalizeForLookup(input.toString());
        synsets = lexDictionary.lookupSynsets(norm);
      }
    }

    result.setTokens(tokens).setSentence(sentence).setInput(input.toString()).setNorm(norm).setSynsets(synsets);

    if (result.hasSynsets()) {
      result.constrain(new PosConstraintFunction(token));
    }

    return result;
  }

  private static final boolean collectAll(Set<ConllToken> tokens, ConllSentence sentence, ConllToken token, String[] deprelValues) {
    boolean result = false;

    for (String value : deprelValues) {
      final List<ConllToken> children = sentence.getChildren(token, ConllField.DEPREL, value);
      if (children != null) {
        tokens.addAll(children);
        result = true;
      }
    }

    return result;
  }


  private ConllSentence sentence;
  private List<ConllToken> tokens;
  private TreeMap<Integer, List<Word>> _frame2words;
  private List<String> _wordNames;

  public ConllWord() {
    super();
  }

  public boolean hasSentence() {
    return sentence != null;
  }

  public ConllSentence getSentence() {
    return sentence;
  }

  public ConllWord setSentence(ConllSentence sentence) {
    this.sentence = sentence;
    return this;
  }

  public boolean hasToken() {
    return tokens != null && tokens.size() > 0;
  }

  public ConllToken getToken() {
    return tokens == null || tokens.size() == 0 ? null : tokens.get(0);
  }

  public ConllWord setToken(ConllToken token) {
    if (token == null) {
      this.tokens = null;
    }
    else {
      if (this.tokens == null) this.tokens = new ArrayList<ConllToken>();
      else this.tokens.clear();
      this.tokens.add(token);
    }
    return this;
  }

  public boolean hasTokens() {
    return tokens != null && tokens.size() > 1;
  }

  public List<ConllToken> getTokens() {
    return new ArrayList<ConllToken>(tokens);
  }

  public ConllWord setTokens(Collection<ConllToken> tokens) {
    this.tokens = new ArrayList<ConllToken>(tokens);
    return this;
  }

  /** Get verb frame numbers mapped to corresponding words. */
  public TreeMap<Integer, List<Word>> getFrame2Words() {
    if (_frame2words == null) {
      _frame2words = new TreeMap<Integer, List<Word>>();
      if (hasSynsets()) {
        for (Synset synset : getSynsets()) {
          if (synset.hasWords()) {
            for (Word word : synset.getWords()) {
              if (word.hasFrames()) {
                for (Integer frame : word.getAllFrames()) {
                  List<Word> words = _frame2words.get(frame);
                  if (words == null) {
                    words = new ArrayList<Word>();
                    _frame2words.put(frame, words);
                  }
                  words.add(word);
                }
              }
            }
          }
        }
      }
    }
    return _frame2words;
  }

  public ConllWord setWordNames(List<String> wordNames) {
    this._wordNames = wordNames;
    return this;
  }

  public ConllWord setWords(List<Word> words) {
    this._wordNames = new ArrayList<String>();
    if (words != null) {
      for (Word word : words) {
        this._wordNames.add(word.getQualifiedWordName());
      }
    }
    return this;
  }

  public List<String> getWordNames() {
    if (_wordNames == null) {
      this._wordNames = new ArrayList<String>();
      if (hasSynsets()) {
        for (Synset synset : getSynsets()) {
          this._wordNames.add(synset.getSynsetName());
        }
      }
    }
    return _wordNames;
  }


  public static final class PosConstraintFunction implements ConstraintFunction {
    private final ConllToken token;

    public PosConstraintFunction(ConllToken token) {
      this.token = token;
    }

    public boolean accept(Synset synset) {
      return posMatch(token.getData(ConllField.CPOSTAG), synset);
    }

    private final boolean posMatch(String lemma, Synset synset) {
      boolean result = false;

      if (lemma != null && synset != null) {
        result = synset.getLexFileName().startsWith(lemma.toLowerCase());
      }

      return result;
    }
  }
}
