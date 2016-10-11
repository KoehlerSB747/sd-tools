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
import org.sd.nlp.conll.ConllField;
import org.sd.nlp.conll.ConllReader;
import org.sd.nlp.conll.ConllSentence;
import org.sd.nlp.conll.ConllToken;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.util.NormalizeUtil;
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
    final ConllToken rootToken = sentence.getFirstToken(ConllField.DEPREL, "ROOT");

    if (rootToken != null) {

      ConllToken verbToken = null;  // root
      ConllToken nounToken = null;  // dobj or root
      ConllToken adjToken = null;
      ConllToken advToken = null;
      boolean neg = false;


      // get closest adv before root and note if it is negated
      for (ConllToken prev = sentence.getPriorToken(rootToken); prev != null; prev = sentence.getPriorToken(prev)) {
        if (prev.matches(ConllField.CPOSTAG, "ADV")) {
          if (prev.matches(ConllField.DEPREL, "neg")) {
            neg = true;
            break;
          }
          else if (advToken == null) {
            advToken = prev;
          }
        }
        else break;
      }

      // get noun
      if (rootToken.matches(ConllField.CPOSTAG, "VERB")) {
        verbToken = rootToken;

        // get (first) direct object
        nounToken = sentence.getFirstToken(ConllField.DEPREL, "dobj");
      }
      else if (rootToken.matches(ConllField.CPOSTAG, "NOUN")) {
        nounToken = rootToken;
      }

      // get adj
      if (nounToken != null) {
        adjToken = sentence.getPriorToken(nounToken);
        if (adjToken != null && !adjToken.matches(ConllField.CPOSTAG, "ADJ")) {
          adjToken = null;
        }
      }

      final String nounSynsets = getSynsets(nounToken);
      final String verbSynsets = getSynsets(verbToken);
      final String adjSynsets = getSynsets(adjToken);
      final String advSynsets = getSynsets(advToken);

      System.out.println(String.format("%s\t%s\t%s\t%s\t%s\t%s",
                                       sentence.getText(),
                                       verbSynsets, nounSynsets, adjSynsets, advSynsets,
                                       (neg ? "-" : "+")));
    }
  }

  private final String getSynsets(ConllToken token) {
    final StringBuilder result = new StringBuilder();

    if (token != null) {
      final List<Synset> synsets = lexDictionary.lookupSynsets(NormalizeUtil.normalizeForLookup(token.getText()));
      if (synsets != null) {
        for (Synset synset : synsets) {
          if (posMatch(token.getData(ConllField.CPOSTAG), synset)) {
            if (false) {  // show synsets
              if (result.length() > 0) result.append(",");
              result.append(synset.getSynsetName());
            }
            else {  // show text
              result.append(token.getText());
              break;
            }
          }
        }
      }
    }

    return result.toString();
  }

  private final boolean posMatch(String lemma, Synset synset) {
    boolean result = false;

    if (lemma != null && synset != null) {
      result = synset.getLexFileName().startsWith(lemma.toLowerCase());
    }

    return result;
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
