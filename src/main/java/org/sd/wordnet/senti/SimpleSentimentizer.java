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
import java.util.Collection;
import org.sd.atn.AtnParse;
import org.sd.atnexec.ConfigUtil;
import org.sd.util.tree.Tree;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Simple sentiment generator.
 * <p>
 * @author Spencer Koehler
 */
public class SimpleSentimentizer implements SynsetLineProcessor.SynsetLineHandler {
  
  private SentimentCollector sentimentCollector;

  public SimpleSentimentizer() {
    this.sentimentCollector = new SentimentCollector();
  }

  @Override
  public void startLine(String line) {
    this.sentimentCollector.reset();
  }

  @Override
  public void processSynsets(LexDictionary lexDictionary, Collection<String> synsetNames, AtnParse atnParse, Tree<String> tokenNode) {
    for (String synsetName : synsetNames) {
      //todo: recognize negation terms and sentence boundaries
      sentimentCollector.addWord(lexDictionary, synsetName, null, false);
    }
  }

  @Override
  public void endLine(String line, boolean fromParse) {
    System.out.println(line + "\t" + sentimentCollector.toString());
  }


  public static void main(String[] args) throws IOException {
    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    args = dataProperties.getRemainingArgs();

    final SimpleSentimentizer sentimentizer = new SimpleSentimentizer();
    final SynsetLineProcessor processor = new SynsetLineProcessor(sentimentizer, dataProperties);
    processor.process(args);
    processor.close();
  }
}
