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
 * Container for an adjective cluster.
 * <p>
 * @author Spencer Koehler
 */
public class AdjectiveCluster {
  
  private List<SynsetGroup> synsetGroups;

  public AdjectiveCluster() {
    this.synsetGroups = null;
  }

  public String getClusterName() {
    String result = null;

    if (synsetGroups != null) {
      for (SynsetGroup synsetGroup : synsetGroups) {
        result = synsetGroup.getSynsetGroupName();
        if (result != null) break;
      }
    }

    return result;
  }

  public long getSynsetCount() {
    long result = 0L;

    if (synsetGroups != null) {
      for (SynsetGroup synsetGroup : synsetGroups) {
        result += synsetGroup.getSynsetCount();
      }
    }

    return result;
  }

  public boolean hasSynsetGroups() {
    return synsetGroups != null && synsetGroups.size() > 0;
  }

  public List<SynsetGroup> getSynsetGroups() {
    return synsetGroups;
  }

  public void addSynsetGroup(SynsetGroup synsetGroup) {
    if (synsetGroup != null) {
      if (synsetGroups == null) synsetGroups = new ArrayList<SynsetGroup>();
      synsetGroups.add(synsetGroup);
    }
  }
}
