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


import java.util.HashSet;
import java.util.Set;
import org.sd.wordnet.lex.Synset;

/**
 * Utility to select synsets.
 * <p>
 * @author Spencer Koehler
 */
public class SynsetSelector {
  
  public static final Set<String> DEFAULT_QUALIFYING_PARTS_OF_SPEECH = new HashSet<String>();
  static {
    DEFAULT_QUALIFYING_PARTS_OF_SPEECH.add("adj");
    DEFAULT_QUALIFYING_PARTS_OF_SPEECH.add("adv");
    DEFAULT_QUALIFYING_PARTS_OF_SPEECH.add("noun");
    DEFAULT_QUALIFYING_PARTS_OF_SPEECH.add("verb");
  }
  public static final Set<String> DEFAULT_DISQUALIFYING_SEMANTIC_TYPES = new HashSet<String>();
  static {
    DEFAULT_DISQUALIFYING_SEMANTIC_TYPES.add("modal");
  }


  protected Set<String> qualifyingPartsOfSpeech;
  protected Set<String> disqualifyingSemanticTypes;

  public SynsetSelector() {
    this.qualifyingPartsOfSpeech = DEFAULT_QUALIFYING_PARTS_OF_SPEECH;
    this.disqualifyingSemanticTypes = DEFAULT_DISQUALIFYING_SEMANTIC_TYPES;
  }


  public void setQualifyingPartsOfSpeech(Set<String> qualifyingPartsOfSpeech) {
    this.qualifyingPartsOfSpeech = qualifyingPartsOfSpeech;
  }

  public void setDisqualifyingSemanticTypes(Set<String> disqualifyingSemanticTypes) {
    this.disqualifyingSemanticTypes = disqualifyingSemanticTypes;
  }

  public boolean hasQualifyingPartOfSpeech(Synset synset) {
    return hasQualifyingPartOfSpeech(synset.getLexInfo());
  }

  public boolean hasQualifyingPartOfSpeech(String synsetName) {
    return hasQualifyingPartOfSpeech(synsetName.split("\\."));
  }

  public boolean hasQualifyingPartOfSpeech(String[] lexInfo) {
    boolean result = false;

    if (lexInfo != null && lexInfo.length > 0) {
      if (qualifyingPartsOfSpeech.contains(lexInfo[0])
          && (lexInfo.length > 1 && !disqualifyingSemanticTypes.contains(lexInfo[1]))) {
        result = true;
      }
    }
    
    return result;
  }
}
