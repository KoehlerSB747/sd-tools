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
 * Utility for loading all lex files.
 * <p>
 * @author Spencer Koehler
 */
public interface LexLoader {
  
  public interface EntryHandler {
    public void handleSynset(Synset synset);
    public void handleAdjectiveCluster(AdjectiveCluster adjectiveCluster);
  }


  public long getSynsetCount();

  /**
   * Load all synsets and adjective clusters within all files in the dbfiles directory.
   *
   * @param entryHandler  Handler for each synset and adjective cluster
   * @param filter  FileFilter to select files to load. If null, all files of
   *                form X.Y in dbFileDir will be loaded.
   */
  public void load(EntryHandler entryHandler);
}
