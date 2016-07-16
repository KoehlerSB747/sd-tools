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


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Utility for loading all lex files.
 * <p>
 * @author Spencer Koehler
 */
public class LexLoader {
  
  public interface EntryHandler {
    public void handleSynset(Synset synset);
    public void handleAdjectiveCluster(AdjectiveCluster adjectiveCluster);
  }


  private File dbFileDir;
  private long synsetCount;

  public LexLoader(File dbFileDir) {
    this.dbFileDir = dbFileDir;
    this.synsetCount = 0L;
  }

  public File getDbFileDir() {
    return dbFileDir;
  }

  public long getSynsetCount() {
    return synsetCount;
  }

  /**
   * Load all synsets and adjective clusters within all files in the dbfiles directory.
   *
   * @param entryHandler  Handler for each synset and adjective cluster
   * @param filter  FileFilter to select files to load. If null, all files of
   *                form X.Y in dbFileDir will be loaded.
   */
  public void load(EntryHandler entryHandler, FileFilter filter) throws IOException {
    for (File file : (filter == null ? dbFileDir.listFiles() : dbFileDir.listFiles(filter))) {
      final String fileName = file.getName();
      if (filter != null || (file.isFile() && fileName.indexOf('.') > 0)) {
        if (fileName.startsWith("adj.")) {
          // load AdjectiveCluster
          for (AdjectiveClusterFileIterator iter = new AdjectiveClusterFileIterator(file); iter.hasNext(); ) {
            final AdjectiveCluster adjCluster = iter.next();
            entryHandler.handleAdjectiveCluster(adjCluster);
            synsetCount += adjCluster.getSynsetCount();
          }
        }
        else {
          // load Synset
          for (SynsetFileIterator iter = new SynsetFileIterator(file); iter.hasNext(); ) {
            final Synset synset = iter.next();
            ++synsetCount;
            entryHandler.handleSynset(synset);
          }
        }
      }
    }
  }
}
