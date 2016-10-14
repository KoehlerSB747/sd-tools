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
package org.sd.wordnet.apps;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.AnagramGenerator;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.FileLexLoader;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Application for unscrambling words using WordNet data to validate.
 * <p>
 * @author Spencer Koehler
 */
public class WordUnscrambler implements AnagramGenerator.WordValidator {
  
  private LexDictionary lexDictionary;
  private Map<String, List<Synset>> synsets;

  public WordUnscrambler(File dbFileDir) throws IOException {
    this.lexDictionary = new LexDictionary(new FileLexLoader(dbFileDir), false, false, true, false);
    this.synsets = null;
  }

  public WordUnscrambler(LexDictionary lexDictionary) {
    this.lexDictionary = lexDictionary;
    this.synsets = lexDictionary.loadSynsets ? new HashMap<String, List<Synset>>() : null;
  }

  public boolean isValid(String word) {
    boolean result = false;
    final String norm = NormalizeUtil.normalizeForLookup(word);

    if (synsets != null && lexDictionary.loadSynsets) {
      final List<Synset> lookup = lexDictionary.lookupSynsets(norm);
      result = (lookup != null);

      if (result) {
        synsets.put(norm, lookup);
      }
    }
    else {
      result = (lexDictionary.lookupLexNames(norm) != null);
    }

    return result;
  }

  public Map<String, List<Synset>> getSynsets() {
    return synsets;
  }


  public static void main(String[] args) throws IOException {
    //arg0: dbFileDir
    //args1+: strings for anagrams
    final WordUnscrambler descrambler = new WordUnscrambler(new File(args[0]));
    final AnagramGenerator agen = new AnagramGenerator();
    agen.setWordValidator(descrambler);

    agen.doMain(args, 1);
  }
}
