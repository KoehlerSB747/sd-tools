/*
   Copyright 2008-2015 Semantic Discovery, Inc.

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
package org.sd.atn.testbed;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.sd.atn.AtnParse;
import org.sd.atn.AtnParseBasedTokenizer;
import org.sd.atn.AtnParseResult;
import org.sd.atn.ParseInterpretation;
import org.sd.atn.ParseOutputCollector;
import org.sd.xml.DataProperties;

/**
 * Utility for running a ParseRunnerSandbox in a headless (non-interactive)
 * mode (e.g. for testing, debugging).
 * <p>
 * @author Spence Koehler
 */
public class ParseRunnerSandboxHeadless extends ParseRunnerSandbox {
  
  public ParseRunnerSandboxHeadless(DataProperties p, PrintStream out) {
    super(p, out);
    super.setInteractive(false);
  }

  /**
   * Collect parse results generated through the current parser.
   */
  public ParseCollectorHandler collectParseResults(String inputString) throws IOException {
    final ParseCollectorHandler result = new ParseCollectorHandler();
    parseInput(inputString, result, false);
    return result;
  }


  public static final class ParseCollectorHandler implements ParseHandler {

    public final List<String> inputSegments;
    public final List<ParseOutputCollector> parseOutputs;
    public final List<AtnParseBasedTokenizer> tokenizers;
    public final List<AtnParse> selectedParses;
    public final List<ParseInterpretation> interps;

    public ParseCollectorHandler() {
      this.inputSegments = new ArrayList<String>();
      this.parseOutputs = new ArrayList<ParseOutputCollector>();
      this.tokenizers = new ArrayList<AtnParseBasedTokenizer>();
      this.selectedParses = new ArrayList<AtnParse>();
      this.interps = new ArrayList<ParseInterpretation>();
    }

    public void handleParse(String inputSegment, ParseOutputCollector parseOutput) {
      this.inputSegments.add(inputSegment);
      this.parseOutputs.add(parseOutput);

      final AtnParseBasedTokenizer tokenizer = (AtnParseBasedTokenizer)parseOutput.getOutputTokenizer();
      if (tokenizer != null) {
        this.tokenizers.add(tokenizer);
      }

      final List<AtnParseResult> parseResults = parseOutput.getParseResults();
      if (parseResults != null) {
        for (AtnParseResult parseResult : parseResults) {
          final int parseCount = parseResult.getNumParses();
          for (int parseNum = 0; parseNum < parseCount; ++parseNum) {
            final AtnParse parse = parseResult.getParse(parseNum);
            if (parse.getSelected()) {
              selectedParses.add(parse);
              final List<ParseInterpretation> parseInterps = parse.getParseInterpretations();
              if (parseInterps != null) {
                interps.addAll(parseInterps);
              }
            }
          }
        }
      }
    }

    /**
     * Determine whether the given parse is a direct result from the identified
     * parser.
     */
    public boolean isFromParser(AtnParse parse, String compoundParserId, String parserId) {
      boolean result = false;

      if (parse != null) {
        final AtnParseResult parseResult = parse.getParseResult();
        result = isFromParser(parseResult, compoundParserId, parserId);
      }

      return result;
    }

    /**
     * Determine whether the given interp is a direct result from the identified
     * parser.
     */
    public boolean isFromParser(ParseInterpretation interp, String compoundParserId, String parserId) {
      boolean result = false;

      if (interp != null) {
        final AtnParse sourceParse = interp.getSourceParse();
        result = isFromParser(sourceParse, compoundParserId, parserId);
      }

      return result;
    }

    /**
     * Determine whether the given interp is a direct result from the identified
     * parser.
     */
    public boolean isFromParser(AtnParseResult parseResult, String compoundParserId, String parserId) {
      boolean result = false;

      if (parseResult != null) {
        result =
          (compoundParserId == null || "".equals(compoundParserId) || compoundParserId.equals(parseResult.getCompoundParserId())) &&
          (parserId == null || "".equals(parserId) || parserId.equals(parseResult.getParserId()));
      }

      return result;
    }
  }


  public static void main(String[] args) throws IOException {

    // arg1: cmdFile -- identifies sequence of commands to submit
    // arg2: outFile -- identifies file to capture output
    // args3+: X.properties or key=value

    if (args.length >= 2) {
      final File cmdFile = new File(args[0]);
      final File outFile = new File(args[1]);

      ParseRunnerSandbox.doMain(args, cmdFile, outFile, false, new String[] {
        "cmdFile",
        "outFile",
        "[X.properties]*",
        "[key=value]*",
        });
    }
  }
}
