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
 * Iterator over synsets in a file.
 * <p>
 * @author Spencer Koehler
 */
public class SynsetFileIterator implements Iterator<Synset> {
  
  private File synsetFile;
  private BufferedReader reader;
  private String lexFileName;
  private Synset nextSynset;

  public SynsetFileIterator(File synsetFile) throws IOException {
    this.synsetFile = synsetFile;
    this.reader = FileUtil.getReader(synsetFile);
    this.lexFileName = synsetFile.getName();
    this.nextSynset = loadNext();
  }

  public File getSynsetFile() {
    return synsetFile;
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
    return nextSynset != null;
  }

  public Synset next() {
    final Synset result = nextSynset;
    this.nextSynset = loadNext();
    return result;
  }

  private final Synset loadNext() {
    Synset result = null;

    if (reader != null) {
      String line = null;
      try {
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if ("".equals(line) || line.charAt(0) != '{') continue;
          result = LexParser.parseLexString(line);
          if (result != null) {
            result.setLexFileName(lexFileName);
            break;
          }
        }
      }
      catch (IOException ioe) {
        result = null;
      }
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    for (String arg : args) {
      final File lexFile = new File(arg);

      System.out.print("Loading synsetFile: " + lexFile.getAbsolutePath());

      if (lexFile.exists()) {
        int synsetCount = 0;
        for (SynsetFileIterator iter = new SynsetFileIterator(lexFile); iter.hasNext(); ) {
          final Synset synset = iter.next();
          ++synsetCount;
        }
        System.out.println("\tSUCCESS -- loaded " + synsetCount + " synsets");
      }
      else {
        System.out.println("\tFAILED -- synsetFile doesn't exist");
      }
    }
  }

  public void remove() {
    throw new UnsupportedOperationException("Not supported");
  }
}
