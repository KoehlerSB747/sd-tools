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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.sd.io.FileUtil;

/**
 * Iterator over adjective clusters in a file.
 * <p>
 * @author Spencer Koehler
 */
public class AdjectiveClusterFileIterator implements Iterator<AdjectiveCluster> {
  
  private File adjectiveClusterFile;
  private BufferedReader reader;
  private String lexFileName;
  private AdjectiveCluster nextAdjectiveCluster;

  public AdjectiveClusterFileIterator(File adjectiveClusterFile) throws IOException {
    this.adjectiveClusterFile = adjectiveClusterFile;
    this.reader = FileUtil.getReader(adjectiveClusterFile);
    this.lexFileName = adjectiveClusterFile.getName();
    this.nextAdjectiveCluster = loadNext();
  }

  public File getAdjectiveClusterFile() {
    return adjectiveClusterFile;
  }

  public void close() {
    if (reader != null) {
      try {
        reader.close();
      }
      catch (IOException ioe) {
        // eat this
      }
    }
    reader = null;
  }

  public boolean hasNext() {
    return nextAdjectiveCluster != null;
  }

  public AdjectiveCluster next() {
    final AdjectiveCluster result = nextAdjectiveCluster;
    this.nextAdjectiveCluster = loadNext();
    return result;
  }

  private final AdjectiveCluster loadNext() {
    AdjectiveCluster result = null;

    if (reader != null) {
      String line = null;
      while ((line = loadNextLine()) != null) {
        if ("".equals(line)) continue;

        final char first = line.charAt(0);
        if (first == '[') {
          result = loadAdjectiveCluster(line.substring(1).trim());
        }
        else if (first == '{') {
          final Synset synset = LexParser.parseLexString(line);
          if (synset != null) {
            synset.setLexFileName(lexFileName);
            final SynsetGroup group = new SynsetGroup();
            group.setHeadSynset(synset);
            result = new AdjectiveCluster();
            result.addSynsetGroup(group);
          }
        }
        if (result != null) break;
      }
    }

    return result;
  }

  private final AdjectiveCluster loadAdjectiveCluster(String headSynsetLine) {
    AdjectiveCluster result = null;

    for (SynsetGroup synsetGroup = loadSynsetGroup(headSynsetLine);
         synsetGroup != null;
         synsetGroup = loadSynsetGroup("")) {
      if (result == null) result = new AdjectiveCluster();
      result.addSynsetGroup(synsetGroup);
      if (synsetGroup.isLast()) break;
    }

    return result;
  }

  private final SynsetGroup loadSynsetGroup(String headSynsetLine) {
    SynsetGroup result = null;

    while (headSynsetLine != null && "".equals(headSynsetLine)) headSynsetLine = loadNextLine();
    if (headSynsetLine == null) return result;

    final Synset headSynset = LexParser.parseLexString(headSynsetLine);
    if (headSynset != null) {
      headSynset.setLexFileName(lexFileName);
      result = new SynsetGroup();
      result.setHeadSynset(headSynset);

      if (!result.isLast()) {
        String line = null;
        while ((line = loadNextLine()) != null) {
          if ("".equals(line)) continue;
          if (line.charAt(0) == '-') {
            // done with synsetGroup, but not done with AdjectiveCluster
            break;
          }
          else if (line.charAt(0) == ']') {
            // done with synsetGroup and with AdjectiveCluster
            result.setIsLast(true);
            break;
          }
          else {
            // add satellite synset to this group
            final Synset satelliteSynset = LexParser.parseLexString(line);
            if (satelliteSynset != null) {
              satelliteSynset.setLexFileName(lexFileName);
              result.addSatelliteSynset(satelliteSynset);
            }
            if (result.isLast()) break;  // done with synsetGroup and AdjectiveCluster
          }
        }
      }
    }

    return result;
  }

  private final String loadNextLine() {
    String result = null;

    if (reader != null) {
      try {
        result = reader.readLine();
        if (result != null) result = result.trim();
      }
      catch (IOException ioe) {
        // eat this
      }
    }
      
    return result;
  }


  public static void main(String[] args) throws IOException {
    for (String arg : args) {
      final File adjFile = new File(arg);

      System.out.println("Loading adjClusterFile: " + adjFile.getAbsolutePath());

      if (adjFile.exists()) {
        int adjClusterCount = 0;
        for (AdjectiveClusterFileIterator iter = new AdjectiveClusterFileIterator(adjFile); iter.hasNext(); ) {
          final AdjectiveCluster adjCluster = iter.next();
          ++adjClusterCount;
          System.out.println("[" + adjCluster.getClusterName() + "]");
        }
        System.out.println("\tSUCCESS -- loaded " + adjClusterCount + " adjClusters");
      }
      else {
        System.out.println("\tFAILED -- adjFile doesn't exist");
      }
    }
  }

  public void remove() {
    throw new UnsupportedOperationException("Not supported");
  }
}
