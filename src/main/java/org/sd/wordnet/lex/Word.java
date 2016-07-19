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
import java.util.Set;
import java.util.TreeSet;

/**
 * Container for a word with its pointers.
 * <p>
 * @author Spencer Koehler
 */
public class Word {
  
  private Synset synset;
  private SimpleWord simpleWord;
  private List<PointerDefinition> pointers;
  private List<Integer> frames;

  public Word() {
    this.synset = null;
    this.simpleWord = null;
    this.pointers = null;
    this.frames = null;
  }

  public Word(Synset synset, SimpleWord simpleWord) {
    this.synset = synset;
    this.simpleWord = simpleWord;
    this.pointers = null;
    this.frames = null;
  }

  public String getWordName() {
    String result = null;

    if (simpleWord != null && simpleWord.hasWord()) {
      result = simpleWord.getName();
    }

    return result;
  }

  public String getNormalizedWord() {
    String result = null;
                        
    if (simpleWord != null) {
      result = simpleWord.getNormalizedWord();
    }

    return result == null ? "" : result;
  }

  public boolean matches(Word otherWord) {
    boolean result = false;

    if (otherWord != null) {
      result = this.matches(otherWord.getSimpleWord());
    }

    return result;
  }

  public boolean matches(SimpleWord simpleWord) {
    boolean result = false;

    if (simpleWord != null && simpleWord.hasWord() && this.hasSimpleWord() && this.simpleWord.hasWord()) {
      if (this.simpleWord.getLexId() == simpleWord.getLexId() &&
          this.simpleWord.getNormalizedWord().equals(simpleWord.getNormalizedWord())) {
        result = true;
      }
    }

    return result;
  }

  public boolean hasSynset() {
    return synset != null;
  }

  public void setSynset(Synset synset) {
    this.synset = synset;
  }

  public Synset getSynset() {
    return synset;
  }

  public boolean hasSimpleWord() {
    return simpleWord != null;
  }

  public SimpleWord getSimpleWord() {
    return simpleWord;
  }

  public void setSimpleWord(SimpleWord simpleWord) {
    this.simpleWord = simpleWord;
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

  public Set<String> getPointerNormalizedWords() {
    final Set<String> result = new TreeSet<String>();
    if (pointers != null) {
      for (PointerDefinition pointer : pointers) {
        if (pointer.hasHeadWord()) {
          result.add(pointer.getHeadWord().getNormalizedWord());
        }
      }
    }
    return result;
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

  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("Word[").append(getWordName()).append(']');
    return result.toString();
  }
}
