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


import java.io.IOException;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.wordnet.token.SimpleWordLookupStrategy;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Utility for processing synsets.
 * <p>
 * @author Spencer Koehler
 */
public abstract class SynsetProcessor {
  
  public abstract void close();
  public abstract void process(String[] args) throws IOException;


  protected LexDictionary lexDictionary;
  protected SimpleWordLookupStrategy strategy;

  public SynsetProcessor(DataProperties dataProperties) {
    this.lexDictionary = WordNetLoader.loadLexDictionary(dataProperties);
    this.strategy = new SimpleWordLookupStrategy(lexDictionary);
  }

  public LexDictionary getLexDictionary() {
    return lexDictionary;
  }
}
