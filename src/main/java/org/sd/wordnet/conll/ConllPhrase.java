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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.sd.nlp.conll.ConllSentence;
import org.sd.nlp.conll.ConllToken;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Word;

/**
 * Container for a ConllSentence with its ConllWords (and Synsets).
 * <p>
 * @author Spencer Koehler
 */
public class ConllPhrase {

  private LexDictionary lexDictionary;
  private ConllSentence sentence;
  private ConllVerbFrames verbFrames;
  private Map<Integer, ConllWord> conllWords;
  private List<Word> rootWords;
  private Summary _summary;

  public ConllPhrase(LexDictionary lexDictionary, ConllSentence sentence) {
    this.lexDictionary = lexDictionary;
    this.sentence = sentence;
    this.verbFrames = new ConllVerbFrames(sentence);
    this.conllWords = buildWords();
    this.rootWords = findRootWords();
  }

  public LexDictionary getLexDictionary() {
    return lexDictionary;
  }

  public ConllSentence getSentence() {
    return sentence;
  }

  public ConllVerbFrames getVerbFrames() {
    return verbFrames;
  }

  public Map<Integer, ConllWord> getConllWords() {
    return conllWords;
  }

  public List<Word> getRootWords() {
    return rootWords;
  }

  public Summary getSummary() {
    if (_summary == null) {
      _summary = new Summary(
        getConllWord(verbFrames.getRoot()),
        getConllWord(verbFrames.getSubj()),
        getConllWord(verbFrames.getObj()),
        getConllWord(verbFrames.getObjAdj()),
        getConllWord(verbFrames.getRootAdv()),
        verbFrames.isNegated());
    }
    return _summary;
  }

  private final Map<Integer, ConllWord> buildWords() {
    final Map<Integer, ConllWord> result = new HashMap<Integer, ConllWord>();
    for (ConllToken token : sentence.getTokens()) {
      final ConllWord word = ConllWord.getInstance(lexDictionary, sentence, token);
      if (word != null) {
        result.put(token.getId(), word);
      }
    }
    return result;
  }

  public ConllWord getConllWord(ConllToken conllToken) {
    ConllWord result = null;

    if (conllToken != null) {
      result = conllWords.get(conllToken.getId());
    }

    return result;
  }

  private final List<Word> findRootWords() {
    List<Word> result = null;
    
    final ConllToken rootToken = verbFrames.getRoot();
    final ConllWord rootWord = getConllWord(rootToken);
    if (rootWord != null && rootWord.hasSynsets()) {
      final TreeMap<Integer, List<Word>> frame2words = rootWord.getFrame2Words();
      final Integer maxFrame = verbFrames.getMaxMatch(frame2words.keySet());
      if (maxFrame != null) {
        result = frame2words.get(maxFrame);

        // store these as ConllWord names
        rootWord.setWords(result);
      }
    }

    return result;
  }


  public static final class Summary {
    public final ConllWord rootWord;
    public final ConllWord subjWord;
    public final ConllWord objWord;
    public final ConllWord objAdjWord;
    public final ConllWord rootAdvWord;
    public final boolean negated;

    public Summary(ConllWord rootWord, ConllWord subjWord, ConllWord objWord, ConllWord objAdjWord, ConllWord rootAdvWord, boolean negated) {
      this.rootWord = rootWord;
      this.subjWord = subjWord;
      this.objWord = objWord;
      this.objAdjWord = objAdjWord;
      this.rootAdvWord = rootAdvWord;
      this.negated = negated;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      // roots\tobjs\tobjAdjs\trootAdvs\tnegFlag\tsubjs
      if (rootWord != null) {
        result.append(buildWordString(rootWord));
      }
      result.append("\t");
      if (objWord != null) {
        result.append(buildWordString(objWord));
      }
      result.append("\t");
      if (objAdjWord != null) {
        result.append(buildWordString(objAdjWord));
      }
      result.append("\t");
      if (rootAdvWord != null) {
        result.append(buildWordString(rootAdvWord));
      }
      result.append("\t");
      result.append(negated ? "-" : "+");
      result.append("\t");
      if (subjWord != null) {
        result.append(buildWordString(subjWord));
      }

      return result.toString();
    }

    private final String buildWordString(ConllWord word) {
      // if wordNames.size() > 2, just give the token text
      final StringBuilder result = new StringBuilder();

      if (word != null) {
        final List<String> wordNames = word.getWordNames();
        if (wordNames.size() > 2) {
          result.append(word.getInput());
        }
        else {
          result.append(asString(wordNames));
        }
      }

      return result.toString();
    }

    private final String asString(List<String> strings) {
      final StringBuilder result = new StringBuilder();

      for (String string : strings) {
        if (result.length() > 0) result.append(',');
        result.append(string);
      }

      return result.toString();
    }
  }
}
