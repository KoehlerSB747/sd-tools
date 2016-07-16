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

/**
 * Container for head and optional satellite synsets that are conceptually
 * similar to the head synset's meaning.
 * <p>
 * @author Spencer Koehler
 */
public class SynsetGroup {

  private Synset headSynset;
  private List<Synset> satelliteSynsets;
  private boolean isLast;

  public SynsetGroup() {
    this.headSynset = null;
    this.satelliteSynsets = null;
    this.isLast = false;
  }

  public long getSynsetCount() {
    long result = 0L;

    if (headSynset != null) ++result;
    if (satelliteSynsets != null) {
      result += satelliteSynsets.size();
    }

    return result;
  }

  public String getSynsetGroupName() {
    String result = null;

    if (this.hasHeadSynset()) {
      result = headSynset.getSynsetName();
    }

    return result;
  }

  public boolean hasHeadSynset() {
    return headSynset != null && headSynset.hasWords();
  }

  public Synset getHeadSynset() {
    return headSynset;
  }

  public void setHeadSynset(Synset headSynset) {
    this.headSynset = headSynset;
    updateIsLast(headSynset);
  }

  public boolean hasSatelliteSynsets() {
    return satelliteSynsets != null && satelliteSynsets.size() > 0;
  }

  public List<Synset> getSatelliteSynsets() {
    return satelliteSynsets;
  }

  public void addSatelliteSynset(Synset satelliteSynset) {
    if (satelliteSynset != null) {
      if (satelliteSynsets == null) satelliteSynsets = new ArrayList<Synset>();
      satelliteSynsets.add(satelliteSynset);
      updateIsLast(satelliteSynset);
    }
  }

  public boolean isLast() {
    return isLast;
  }

  public void setIsLast(boolean isLast) {
    this.isLast = isLast;
  }

  private final void updateIsLast(Synset synset) {
    if (synset != null && synset.hasStringDecoder()) {
      final String lexString = synset.getStringDecoder().getLexString();
      if (lexString != null && !"".equals(lexString) && lexString.charAt(lexString.length() - 1) == ']') {
        this.isLast = true;
      }
    }
  }
}
