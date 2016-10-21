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
import java.util.List;
import org.sd.atnexec.ConfigUtil;
import org.sd.nlp.conll.ConllSentence;
import org.sd.wordnet.conll.ConllPhrase;
import org.sd.xml.DataProperties;

/**
 * Utility to generate a topics from Conll-formatted parsed sentences.
 * <p>
 * @author Spencer Koehler
 */
public class ConllTopicGenerator extends SynsetConllProcessor {

  protected ConllTopicGenerator(DataProperties dataProperties) {
    super(dataProperties);
  }

  @Override
  protected void process(ConllSentence sentence) {
    final ConllPhrase phrase = new ConllPhrase(lexDictionary, sentence);
    System.out.println(String.format("%s\t%s",
                                     sentence.getText(),
                                     phrase.getSummary().toString()));
  }


  public static void main(String[] args) throws IOException {
    // Properties: None
    // Args: conll filenames --or-- stdin conll file data

    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    args = dataProperties.getRemainingArgs();

    final ConllTopicGenerator topicGenerator = new ConllTopicGenerator(dataProperties);
    topicGenerator.process(args);
    topicGenerator.close();
  }
}
