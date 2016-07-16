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


import java.util.ArrayList;
import java.util.List;
import org.sd.wordnet.util.StringDecoder;

/**
 * Container for a lexicographer synset.
 * <p>
 * @author Spencer Koehler
 */
public class Synset {

  private StringDecoder decoder;
  private List<Word> words;
  private List<PointerDefinition> pointers;
  private List<Integer> frames;
  private String gloss;
  private String lexFileName;

  public Synset() {
    this.decoder = null;
    this.words = new ArrayList<Word>();
    this.pointers = null;
    this.frames = null;
    this.gloss = null;
    this.lexFileName = null;
  }

  public String getSynsetName() {
    final StringBuilder result = new StringBuilder();

    if (lexFileName != null) {
      result.append(lexFileName).append(':');
    }
    for (Word word : words) {
      final String wordName = word.getWordName();
      if (wordName != null) {
        result.append(wordName);
        break;
      }
    }

    return result.toString();
  }

  public boolean hasStringDecoder() {
    return decoder != null;
  }

  public StringDecoder getStringDecoder() {
    return decoder;
  }

  public void setStringDecoder(StringDecoder decoder) {
    this.decoder = decoder;
  }

  public int size() {
    return words.size();
  }

  public boolean hasWords() {
    return words.size() > 0;
  }

  public List<Word> getWords() {
    return words;
  }

  public void addWord(Word word) {
    if (word != null) {
      words.add(word);
    }
  }

  public void addWord(SimpleWord simpleWord) {
    if (simpleWord != null) {
      words.add(new Word(simpleWord));
    }
  }

  public Word findWord(SimpleWord simpleWord) {
    Word result = null;

    if (this.hasWords()) {
      for (Word word : words) {
        final Word.Match match = word.matches(simpleWord);

        switch (match) {
          case WORD_SENSE :
            result = word;
            break;
          case WORD :
            if (result == null) {
              result = word;
            }
            break;
        }

        if (match == Word.Match.WORD_SENSE) {
          break;
        }
      }
    }

    return result;
  }

  public boolean hasPointerDefinitions() {
    return pointers != null && pointers.size() > 0;
  }

  public List<PointerDefinition> getPointerDefinitions() {
    return pointers;
  }

  public void addPointerDefinition(PointerDefinition pointer) {
    if (pointer != null) {
      if (pointers == null) pointers = new ArrayList<PointerDefinition>();
      pointers.add(pointer);
    }
  }

  public boolean hasFrames() {
    return frames != null && frames.size() > 0;
  }

  public List<Integer> getFrames() {
    return frames;
  }

  public void addFrame(int frame) {
    if (frames == null) frames = new ArrayList<Integer>();
    frames.add(frame);
  }

  public boolean hasGloss() {
    return gloss != null && !"".equals(gloss);
  }

  public String getGloss() {
    return gloss;
  }

  public void setGloss(String gloss) {
    this.gloss = gloss;
  }

  public boolean hasLexFileName() {
    return lexFileName != null && !"".equals(lexFileName);
  }

  public String getLexFileName() {
    return lexFileName;
  }

  public void setLexFileName(String lexFileName) {
    this.lexFileName = lexFileName;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("Synset[").append(getSynsetName()).append(']');
    return result.toString();
  }
}
