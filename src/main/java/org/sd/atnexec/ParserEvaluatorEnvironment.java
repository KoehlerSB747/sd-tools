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


import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.sd.atn.GenericParseResults;
import org.sd.atn.GenericParseResultsAsync;
import org.sd.atn.ParseOrToken;
import org.sd.atn.ParseOrTokenSequence;
import org.sd.atn.ResourceManager;
import org.sd.analysis.AbstractAnalysisObject;
import org.sd.analysis.AnalysisFunction;
import org.sd.analysis.AnalysisObject;
import org.sd.analysis.BaseEvaluatorEnvironment;
import org.sd.analysis.BasicAnalysisObject;
import org.sd.analysis.EvaluatorEnvironment;
import org.sd.analysis.NumericAnalysisObject;
import org.sd.token.Feature;
import org.sd.token.Token;
import org.sd.util.ThreadPoolUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.token.SimpleWordLookupStrategy;
import org.sd.wordnet.token.WordNetTokenizer;
import org.sd.wordnet.util.TransformUtil;
import org.sd.xml.DataProperties;

/**
 * An EvaluatorEnvironment for interacting with Parser through a CommandEvaluator.
 * <p>
 * @author Spencer Koehler
 */
public class ParserEvaluatorEnvironment extends BaseEvaluatorEnvironment {
  
  private DataProperties dataProperties;
  private WordNetParser parser;
  private ExecutorService threadPool;

  public ParserEvaluatorEnvironment(DataProperties dataProperties) {
    super(dataProperties);

    final ConfigUtil configUtil = new ConfigUtil(dataProperties); // configures dataProperties for parser
    this.dataProperties = configUtil.getDataProperties();
    this.parser = buildParser();

    // create one thread for background parsing, with plan to stop prev parse
    // if not done when next is invoked
    this.threadPool = ThreadPoolUtil.createThreadPool("WordNetParser-", 1);
  }

  private final WordNetParser buildParser() {
    WordNetParser result = null;

    try {
      result = new WordNetParser(this.dataProperties);
    }
    catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }

    return result;
  }

  @Override
  protected void addMoreFunctions() {
    defineFunction("reset", new ResetFunction());
    defineFunction("parse", new ParseFunction());
    defineFunction("watch", new WatchFunction());  //watch {verbose, trace, traceflow} {true, false}
    defineFunction("tokenize", new TokenizeFunction());
//todo: add function to lookup a word to view definitions
  }

  /**
   * Close this environment and any open resources.
   */
  @Override
  public void close() {
    ThreadPoolUtil.shutdownGracefully(threadPool, 1L);
    super.close();
  }

  final class ResetFunction implements AnalysisFunction {

    private final AnalysisObject result = new BasicAnalysisObject<Boolean>(true);

    ResetFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      if (parser != null) parser.close();
      parser = buildParser();
      return result;
    }
  }

  final class WatchFunction implements AnalysisFunction {

    private final AnalysisObject result = new BasicAnalysisObject<Boolean>(true);

    WatchFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      if (parser != null) {
        Boolean valueToSet = null;
        String watchType = "verbose";

        // arg1: verbose, trace, traceflow; or true, false to set just verbose
        // arg2: (optional, missing toggles current value) true, false

        if (args != null && args.length > 0) {
          final String arg1 = args[0].toString().toLowerCase();

          if ("true".equals(arg1)) {
            valueToSet = true;
          }
          else if ("false".equals(arg1)) {
            valueToSet = false;
          }
          else {
            watchType = arg1;
            if (args.length > 1) {
              valueToSet = "true".equals(args[1].toString());
            }
          }
        }

        if (valueToSet == null) {
          // toggle current value
          switch (watchType) {
            case "trace" :
              valueToSet = !parser.getTrace();
              break;
            case "traceflow" :
              valueToSet = !parser.getTraceFlow();
              break;
            default :  // verbose
              valueToSet = !parser.getVerbose();
              break;
          }
        }

        // set the value
        switch (watchType) {
          case "trace" :
            parser.setTrace(valueToSet);
            break;
          case "traceflow" :
            parser.setTraceFlow(valueToSet);
            break;
          default :  // verbose
            parser.setVerbose(valueToSet);
            break;
        }
      }

      return result;
    }
  }

  final class ParseFunction implements AnalysisFunction {

    private GenericParseResultsAsync prevResults;

    ParseFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      AnalysisObject result = null;
      final StringBuilder message = new StringBuilder();

      if (args != null) {
        if (args.length == 1) {
          final String input = args[0].toString();

          if (prevResults != null) {
            // abort previous parsing if not done
            prevResults.close();
          }

          final GenericParseResultsAsync parseResultsAsync = parser.parseInputAsync(threadPool, input);
          result = new ParseAnalysisObject(input, parseResultsAsync);

          prevResults = parseResultsAsync;
        }
        else {
          message.append(" FAILED: Bad number of args (").append(args.length).append(").");
        }
      }
      else {
        message.append(" FAILED: No args.");
      }

      if (result == null) {
        message.append(" arg1=Input to parse");
        result = new BasicAnalysisObject<String>(message.toString());
      }

      return result;
    }
  }

  public final class ParseAnalysisObject extends AbstractAnalysisObject {
    public final String input;
    private GenericParseResultsAsync parseResultsAsync;
    private GenericParseResults _parseResults;
    private String mode;

    public ParseAnalysisObject(String input, GenericParseResultsAsync parseResultsAsync) {
      this.input = input;
      this.parseResultsAsync = parseResultsAsync;
      this._parseResults = _parseResults;
      this.mode = "parses";
    }

    public boolean hasParseResults() {
      boolean result = false;
      final GenericParseResults parseResults = getParseResults();

      if (parseResults != null && parseResults.hasParses()) {
        result = true;
      }

      return result;
    }

    public boolean hasSequence() {
      boolean result = false;
      final GenericParseResults parseResults = getParseResults();

      if (parseResults != null && parseResults.hasSequence()) {
        result = true;
      }

      return result;
    }

    public GenericParseResults getParseResults() {
      if (_parseResults == null && parseResultsAsync != null) {
        _parseResults = parseResultsAsync.getParseResults();
      }
      return _parseResults;
    }

    public String getResultSize() {
      String result = null;

      final GenericParseResults parseResults = getParseResults();
      if (parseResults != null) {
        result = Integer.toString(parseResults.size());
      }

      return (result == null) ? "?" : result;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#ParseResults[").append(input).append("]-").append(getResultSize());

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of the parseResults.\n").
        append("\"mode[parses(default)|seq]\" -- show the parses or the parse-or-token sequence.\n").
        append("\"stop\" -- stop async parsing.\n").
        append("\"wait[<waitToDieMillis(default=100)>, <timeoutMillis(default=10000)>]\" -- wait for async parsing.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      result.append("parseResults(").append(input).append(")");
        
      boolean hasData = false;

      if (hasParseResults() || hasSequence()) {
        final GenericParseResults parseResults = getParseResults();
        if (mode.startsWith("seq")) {
          if (parseResults.hasSequence()) {
            final ParseOrTokenSequence seq = parseResults.getSequence();
            result.append("\n").append(buildShowString(seq));
            hasData = true;
          }
        }
        else {
          if (hasParseResults()) {
            result.append("\n").append(parseResults.toString());
            hasData = true;
          }
        }
      }

      if (!hasData) {
        result.append(" -- NO DATA");

        if (parseResultsAsync != null) {
          result.
            append("  done=").append(parseResultsAsync.isDone()).
            append("  completed=").append(parseResultsAsync.completed()).
            append("  stopped=").append(parseResultsAsync.stopped());
        }
      }

      return result.toString();
    }

    @Override
    protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
      AnalysisObject result = null;

      if ("stop".equals(ref)) {
        if (parseResultsAsync != null) parseResultsAsync.stopParsing();
        result = new BasicAnalysisObject<Boolean>(hasParseResults());
      }
      else if (ref.startsWith("mode")) {
        final AnalysisObject[] argValues = getArgValues(ref, env);
        if (argValues.length > 0) {
          this.mode = argValues[0].toString().toLowerCase();
        }
        result = new BasicAnalysisObject<String>("mode=" + this.mode);
      }
      else if (ref.startsWith("wait")) {
        if (_parseResults == null && parseResultsAsync != null) {
          int waitToDieMillis = 100;
          int timeoutMillis = 10000;

          final AnalysisObject[] argValues = getArgValues(ref, env);
          final int[] args = asIntValues(argValues, 0);
          if (args != null && args.length > 0) {
            waitToDieMillis = (args.length > 0) ? args[0] : waitToDieMillis;
            timeoutMillis = (args.length > 1) ? args[1] : timeoutMillis;
          }
          _parseResults = parseResultsAsync.getParseResults(waitToDieMillis, timeoutMillis);
        }
        result = new BasicAnalysisObject<Boolean>(hasParseResults());
      }

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }

    private final String buildShowString(ParseOrTokenSequence seq) {
      final StringBuilder result = new StringBuilder();

      if (seq != null) {
        int num = 1;
        for (ParseOrToken item : seq.getSequence()) {
          if (result.length() > 0) result.append("\n");
          result.append(num++).append('\t').append(buildShowString(item));
        }
      }

      return result.toString();
    }

    private final String buildShowString(ParseOrToken item) {
      final StringBuilder result = new StringBuilder();

      if (item.hasParse()) {
        result.append(item.getParse().toString());

        final List<WordNetParser.TokenData> tokenDatas = WordNetParser.getTokenData(item.getParse());
        for (WordNetParser.TokenData tokenData : tokenDatas) {
          result.append("\n\t").append(tokenData.toString());
        }
      }
      else {
        result.append(item.getToken().getDetailedString());
      }

      return result.toString();
    }
  }

  final class TokenizeFunction implements AnalysisFunction {

    TokenizeFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      AnalysisObject result = null;
      final StringBuilder message = new StringBuilder();

      if (args != null) {
        if (args.length == 1) {
          final String input = TransformUtil.applyTransformations(args[0].toString());

          if (parser != null) {
            final ResourceManager resourceManager =
              parser.getAtnParseRunner().getParseConfig().getResourceManager();
            final LexDictionary dict = (LexDictionary)resourceManager.getResource("wn-dict");
            if (dict != null) {
              final SimpleWordLookupStrategy strategy = new SimpleWordLookupStrategy(dict);
              final WordNetTokenizer tokenizer = new WordNetTokenizer(dict, strategy, input);
              result = new TokenizerAnalysisObject(input, tokenizer);
            }
            else {
              message.append(" FAILED: ResourceManager has no wn-dict");
            }
          }
          else {
            message.append(" FAILED: No parser.");
          }
        }
        else {
          message.append(" FAILED: Bad number of args (").append(args.length).append(").");
        }
      }
      else {
        message.append(" FAILED: No args.");
      }

      if (result == null) {
        message.append(" arg1=Input to parse");
        result = new BasicAnalysisObject<String>(message.toString());
      }

      return result;
    }
  }

  public final class TokenizerAnalysisObject extends AbstractAnalysisObject {
    public final String input;
    private WordNetTokenizer tokenizer;

    public TokenizerAnalysisObject(String input, WordNetTokenizer tokenizer) {
      this.input = input;
      this.tokenizer = tokenizer;
    }

    public boolean hasTokenizer() {
      return tokenizer != null;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#Tokenizer[").append(input).append("]");

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the tokenization.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      result.append("tokenizer(").append(input).append(")");
        

      if (hasTokenizer()) {
        for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
          result.append("\n");
          showToken(result, token, 2);
        }
      }
      else {
        result.append(" -- NO DATA");
      }

      return result.toString();
    }

    @Override
    protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
      AnalysisObject result = null;

      //todo: implement accessors here
      // if ("".equals(ref)) {
      // }

      return result;
    }

    /** Get a numeric object representing this instance's value if applicable, or null. */
    @Override
    public NumericAnalysisObject asNumericAnalysisObject() {
      return null;
    }

    private final void showToken(StringBuilder result, Token token, int indent) {
      // show token and its revisions
      addIndent(result, indent);
      result.append(token.toString());
      if (token.hasFeatures()) {
        result.append("\n");
        addIndent(result, indent + 1);
        result.append("Features: ");

        for (Feature feature : token.getFeatures().getFeatures()) {
          result.append("  ").append(feature.getType()).append('=').append(feature.getValue());
        }        
      }

      final Token revisedToken = token.getRevisedToken();
      if (revisedToken != null) {
        result.append("\n");
        showToken(result, revisedToken, indent + 2);

        final Token nextToken = revisedToken.getNextToken();
        if (nextToken != null) {
          showToken(result, revisedToken, indent + 4);
        }
      }
    }

    private final void addIndent(StringBuilder result, int indent) {
      for (int i = 0; i < indent; ++i) result.append(' ');
    }
  }
}
