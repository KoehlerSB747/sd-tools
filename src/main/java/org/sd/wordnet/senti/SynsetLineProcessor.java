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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.sd.atn.AtnParse;
import org.sd.atn.GenericParseResults;
import org.sd.atn.GenericParseResultsAsync;
import org.sd.atnexec.VerbFrameCheck;
import org.sd.atnexec.WordNetParser;
import org.sd.token.Token;
import org.sd.util.ThreadPoolUtil;
import org.sd.util.tree.Tree;
import org.sd.wordnet.loader.WordNetLoader;
import org.sd.wordnet.token.SimpleWordLookupStrategy;
import org.sd.wordnet.token.WordNetTokenizer;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.xml.DataProperties;

/**
 * Utility for processing synsets found on lines.
 * <p>
 * @author Spencer Koehler
 */
public class SynsetLineProcessor {
  
  public interface SynsetLineHandler {
    public void startLine(String line);
    public void processSynsets(LexDictionary lexDictionary, Collection<String> synsetNames, AtnParse atnParse, Tree<String> tokenNode);
    public void endLine(String line, boolean fromParse);
  }

  public static final int DEFAULT_TIME_LIMIT = 1000;
  public static final int DEFAULT_DIE_WAIT = 1;


  private SynsetLineHandler synsetLineHandler;
  private LexDictionary lexDictionary;
  private SimpleWordLookupStrategy strategy;
  private long timeLimit;
  private long dieWait;

  private WordNetParser parser;
  private ExecutorService threadPool;

  public SynsetLineProcessor(SynsetLineHandler synsetLineHandler, DataProperties dataProperties) throws IOException {
    this.synsetLineHandler = synsetLineHandler;

    this.lexDictionary = WordNetLoader.loadLexDictionary(dataProperties);
    this.strategy = new SimpleWordLookupStrategy(lexDictionary);

    this.timeLimit = dataProperties.getInt("timeLimit", DEFAULT_TIME_LIMIT);
    this.dieWait = dataProperties.getInt("dieWait", DEFAULT_DIE_WAIT);

    this.parser = new WordNetParser(dataProperties);
    this.threadPool = ThreadPoolUtil.createThreadPool("SynsetLineProcessorParser-", 1);
  }

  public void close() {
    ThreadPoolUtil.shutdownGracefully(threadPool, 1L);
  }

  public LexDictionary getLexDictionary() {
    return lexDictionary;
  }

  public void process(String[] args) throws IOException {
    if (args != null && args.length > 0) {
      for (String arg : args) {
        process(arg);
      }
    }
    else {
      // read from stdin
      final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String line = null;
      while ((line = in.readLine()) != null) {
        line = line.trim();
        if ("".equals(line)) continue;
        process(line);
      }
      in.close();
    }
  }

  public void process(String line) {
    synsetLineHandler.startLine(line);

    boolean fromParse = false;
    if (!parseLine(line)) {
      tokenizeLine(line);
    }
    else {
      fromParse = true;
    }

    synsetLineHandler.endLine(line, fromParse);
  }

  private final boolean parseLine(String line) {
    boolean result = false;

    final GenericParseResultsAsync parseResultsAsync = parser.getGenericParser().parseAsync(threadPool, line, null);
    final GenericParseResults parseResults = parseResultsAsync.getParseResults(dieWait, timeLimit);
//    final GenericParseResults parseResults = parser.parseInput(line, null);
    if (parseResults != null && parseResults.hasSequence() && parseResults.getSequence().hasParses()) {
      for (AtnParse atnParse : parseResults.getSequence().getParses()) {
        final List<WordNetParser.TokenData> tokenDatas = WordNetParser.getTokenData(atnParse);
        for (WordNetParser.TokenData tokenData : tokenDatas) {
          if (tokenData.selectedSynsetNames != null && tokenData.selectedSynsetNames.size() > 0) {
            synsetLineHandler.processSynsets(lexDictionary, tokenData.selectedSynsetNames, atnParse, tokenData.tokenNode);
            result = true;
          }
        }
      }
    }

    return result;
  }

  private final boolean tokenizeLine(String line) {
    boolean result = false;
    final WordNetTokenizer tokenizer = new WordNetTokenizer(lexDictionary, strategy, line);
    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      final List<String> tokenSynsetNames = WordNetParser.getTokenSynsetNames(token);
      if (tokenSynsetNames != null) {
        synsetLineHandler.processSynsets(lexDictionary, tokenSynsetNames, null, null);
        result = true;
      }
    }
    return result;
  }
}
