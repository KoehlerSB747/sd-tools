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


/**
 * Container for a pointer instance, including source and target.
 * <p>
 * @author Spencer Koehler
 */
public class PointerInstance {
  
  private Synset sourceSynset;
  private Word sourceWord;  //NOTE: this'll be null if pointer is from synset as a whole
  private PointerDefinition pointerDef;
  private Synset targetSynset;
  private Word targetWord;
  private Word targetSatelliteWord;

  public PointerInstance() {
    this.sourceSynset = null;
    this.sourceWord = null;
    this.pointerDef = null;
    this.targetSynset = null;
    this.targetWord = null;
    this.targetSatelliteWord = null;
  }

  public PointerInstance(Synset sourceSynset, Word sourceWord, PointerDefinition pointerDef, Synset targetSynset, Word targetWord, Word targetSatelliteWord) {
    this.sourceSynset = sourceSynset;
    this.sourceWord = sourceWord;
    this.pointerDef = pointerDef;
    this.targetSynset = targetSynset;
    this.targetWord = targetWord;
    this.targetSatelliteWord = targetSatelliteWord;
  }

  public boolean hasSourceSynset() {
    return sourceSynset != null;
  }

  public Synset getSourceSynset() {
    return sourceSynset;
  }

  public void setSourceSynset(Synset sourceSynset) {
    this.sourceSynset = sourceSynset;
  }

  public boolean hasSourceWord() {
    return sourceWord != null;
  }

  public Word getSourceWord() {
    return sourceWord;
  }

  public void setSourceWord(Word sourceWord) {
    this.sourceWord = sourceWord;
  }

  public boolean hasPointerDef() {
    return pointerDef != null;
  }

  public PointerDefinition getPointerDef() {
    return pointerDef;
  }

  public void setPointerDef(PointerDefinition pointerDef) {
    this.pointerDef = pointerDef;
  }

  public boolean hasTargetSynset() {
    return targetSynset != null;
  }

  public Synset getTargetSynset() {
    return targetSynset;
  }

  public void setTargetSynset(Synset targetSynset) {
    this.targetSynset = targetSynset;
  }

  public boolean hasTargetWord() {
    return targetWord != null;
  }

  public Word getTargetWord() {
    return targetWord;
  }

  public void setTargetWord(Word targetWord) {
    this.targetWord = targetWord;
  }

  public boolean hasTargetSatelliteWord() {
    return targetSatelliteWord != null;
  }

  public Word getTargetSatelliteWord() {
    return targetSatelliteWord;
  }

  public void setTargetSatelliteWord(Word targetSatelliteWord) {
    this.targetSatelliteWord = targetSatelliteWord;
  }
}
