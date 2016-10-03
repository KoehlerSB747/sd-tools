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
package org.sd.atnexec;


import java.io.File;
import java.io.IOException;
import org.sd.atn.ResourceManager;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.wordnet.senti.SentimentLoader;
import org.sd.xml.DomElement;

/**
 * Wrapper around a WordNet LexDictionary to use as an ATN parser resource.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetDictionaryResource extends LexDictionary {
  
  public WordNetDictionaryResource(DomElement resourceElt, ResourceManager resourceManager) throws IOException {
    super(buildLexLoader(resourceElt, resourceManager));

    final File sentiWordNet = resourceManager.getOptions().getFile("sentiWordNet", "workingDir");
    if (sentiWordNet != null) {
      final SentimentLoader sentimentLoader = new SentimentLoader(this);
      sentimentLoader.loadSentiWordNet(sentiWordNet);
    }
  }

  private static final LexLoader buildLexLoader(DomElement resourceElt, ResourceManager resourceManager) {
    final File dbFileDir = resourceManager.getOptions().getFile("dbFileDir", "workingDir");
    final LexLoader lexLoader = new LexLoader(dbFileDir);
    return lexLoader;
  }

}