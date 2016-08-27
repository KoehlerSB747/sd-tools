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
import org.sd.analysis.AbstractAnalysisObject;
import org.sd.analysis.AnalysisFunction;
import org.sd.analysis.AnalysisObject;
import org.sd.analysis.BaseEvaluatorEnvironment;
import org.sd.analysis.BasicAnalysisObject;
import org.sd.analysis.EvaluatorEnvironment;
import org.sd.analysis.NumericAnalysisObject;
import org.sd.atn.GenericParseResults;
import org.sd.xml.DataProperties;

/**
 * An EvaluatorEnvironment for interacting with Parser through a CommandEvaluator.
 * <p>
 * @author Spencer Koehler
 */
public class ParserEvaluatorEnvironment extends BaseEvaluatorEnvironment {
  
  private DataProperties dataProperties;
  private WordNetParser parser;

  public ParserEvaluatorEnvironment(DataProperties dataProperties) {
    super(dataProperties);

    final ConfigUtil configUtil = new ConfigUtil(dataProperties); // configures dataProperties for parser
    this.dataProperties = configUtil.getDataProperties();
    this.parser = buildParser();
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
//todo: add function to lookup a word to view definitions
//todo: add tokenization function
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

    ParseFunction() {
    }

    @Override
    public AnalysisObject execute(AnalysisObject[] args) {
      AnalysisObject result = null;
      final StringBuilder message = new StringBuilder();

      if (args != null) {
        if (args.length == 1) {
          final String input = args[0].toString();
          final GenericParseResults parseResults = parser.parseInput(input, null);
          if (parseResults != null && parseResults.size() > 0) {
            result = new ParseAnalysisObject(input, parseResults);
          }
          else {
            message.append(" FAILED: No parseResults for input=").append(input);
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

  public final class ParseAnalysisObject extends AbstractAnalysisObject {
    public final String input;
    private GenericParseResults parseResults;

    public ParseAnalysisObject(String input, GenericParseResults parseResults) {
      this.input = input;
      this.parseResults = parseResults;
    }

    public boolean hasParseResults() {
      return parseResults != null && parseResults.hasParses();
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("#ParseResults[").append(input).append("]-").append(parseResults.size());

      return result.toString();
    }

    @Override
    public String getHelpString() {
      final StringBuilder result = new StringBuilder();
      result.
        append("\"show\" -- show the content of the parseResults.");
      
      return result.toString();
    }

    /** Customization for "show" access. */
    @Override
    protected String getShowString() {
      final StringBuilder result = new StringBuilder();

      result.append("parseResults(").append(input).append(")");
        

      if (hasParseResults()) {
        result.append("\n").append(parseResults.toString());
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
  }
}
