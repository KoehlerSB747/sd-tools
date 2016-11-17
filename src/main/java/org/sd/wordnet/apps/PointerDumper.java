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


import java.util.List;
import java.util.Map;
import org.sd.atnexec.ConfigUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.PointerDefinition;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.xml.DataProperties;

/**
 * Utility to dump all pointers for analysis.
 * <p>
 * @author Spencer Koehler
 */
public class PointerDumper {
  
  public static void main(String[] args) {
    // Properties:
    //   ...

    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    args = dataProperties.getRemainingArgs();

    final LexDictionary lexDictionary = WordNetLoader.loadLexDictionary(dataProperties);
    final Map<String, List<Synset>> synsets = lexDictionary.getSynsets();


    // OUTPUT: symbol \t sourceWord  \t  targetWord

    for (List<Synset> synsetsList : synsets.values()) {
      for (Synset synset : synsetsList) {
        final boolean synsetHasPointers = synset.hasPointerDefinitions();
        for (Word word : synset.getWords()) {
          if (synsetHasPointers) {
            for (PointerDefinition ptrDef : synset.getPointerDefinitions()) {
              System.out.println(String.format("%s\t%s\t%s",
                                 ptrDef.getPointerSymbol(),
                                 word.getQualifiedWordName(),
                                               ptrDef.getSpecificTargetQualifiedName(synset.getLexFileName())));
            }
          }
          if (word.hasPointerDefinitions()) {
            for (PointerDefinition ptrDef : word.getPointerDefinitions()) {
              System.out.println(String.format("%s\t%s\t%s",
                                 ptrDef.getPointerSymbol(),
                                 word.getQualifiedWordName(),
                                               ptrDef.getSpecificTargetQualifiedName(synset.getLexFileName())));
            }
          }
        }
      }
    }
  }    
}
