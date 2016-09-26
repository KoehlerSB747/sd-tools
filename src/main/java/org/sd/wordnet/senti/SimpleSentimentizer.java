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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.sd.atnexec.ConfigUtil;
import org.sd.token.Token;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.wordnet.token.SimpleWordLookupStrategy;
import org.sd.wordnet.token.WordNetTokenizer;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Simple sentiment generator.
 * <p>
 * @author Spencer Koehler
 */
public class SimpleSentimentizer {
  
  private LexDictionary lexDictionary;
  private SimpleWordLookupStrategy strategy;

  public SimpleSentimentizer(LexDictionary lexDictionary) {
    this.lexDictionary = lexDictionary;
    this.strategy = new SimpleWordLookupStrategy(lexDictionary);
  }

  public SentimentCollector sentimentize(String text) {
    final SentimentCollector result = new SentimentCollector(lexDictionary);

    final WordNetTokenizer tokenizer = new WordNetTokenizer(lexDictionary, strategy, text);
    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      //todo: recognize negation terms and sentence boundaries
      result.addWord(token.getText(), null, false);
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties dataProperties = configUtil.getDataProperties();
    final LexDictionary dict = WordNetLoader.loadLexDictionary(dataProperties);
    final SimpleSentimentizer sentimentizer = new SimpleSentimentizer(dict);
    
    args = dataProperties.getRemainingArgs();

    if (args != null && args.length > 0) {
      for (String arg : args) {
        final SentimentCollector sentiment = sentimentizer.sentimentize(arg);
        System.out.println(arg + "\t" + sentiment);
      }
    }
    else {
      // read from stdin
      final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String line = null;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if ("".equals(line)) continue;
        final SentimentCollector sentiment = sentimentizer.sentimentize(line);
        System.out.println(line + "\t" + sentiment);
      }
    }
  }
}
