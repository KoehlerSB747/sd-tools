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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.atn.AtnState;
import org.sd.atn.AtnParseRunner;;
import org.sd.atn.GenericParser;
import org.sd.atn.GenericParseResults;
import org.sd.xml.DataProperties;

/**
 * Utility to parse input using word net.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetParser {

  private GenericParser genericParser;

  //
  // Properties
  //
  //  Direct:
  //    parseConfig -- (required) path to parseConfig
  //    supplementalConfig -- (optional) path to supplemental parseConfig
  //    parseFlow -- (optional) constraints on parse flow
  //
  //  Indirect:
  //    dbFileDir -- path to wordnet dbFiles dir
  //    resourcesDir -- (required) path to resources (defaults to sd-tools/resources)
  //    atnDir -- path to atn directory (defaults to ${resourcesDir}/atn/atn-general)
  //    parserProperties -- path to parser properties (defaults to ${atnDir}/parser/parser.properties
  //      (ultimately defines parseConfig, supplementalConfig, parseFlow, etc.)
  // 

  public WordNetParser(DataProperties properties) throws IOException {
    if (!properties.hasProperty("parseConfig")) {
      loadIndirectProperties(properties);
    }
    this.genericParser = new GenericParser(properties);
  }

  private final void loadIndirectProperties(DataProperties properties) throws IOException {
    final File resourcesDir = properties.getFile("resourcesDir", "workingDir");

    if (!properties.hasProperty("atnDir")) {
      properties.set("atnDir", "${resourcesDir}/atn/atn-general");
    }
    if (!properties.hasProperty("parserProperties")) {
      properties.set("parserProperties", "${atnDir}/parser/parser.properties");
    }

    final File parserProperties = properties.getFile("parserProperties", "workingDir");
    properties.incorporateProperties(parserProperties, ".properties");
  }

  public void close() {
    genericParser.close();
  }

  public GenericParser getGenericParser() {
    return genericParser;
  }

  public AtnParseRunner getAtnParseRunner() {
    return genericParser.getParseRunner();
  }

  public boolean getVerbose() {
    return genericParser.getParseRunner().getVerbose();
  }

  public void setVerbose(boolean verbose) {
    genericParser.getParseRunner().setVerbose(verbose);
  }

  public boolean getTrace() {
    return AtnState.getTrace();
  }

  public void setTrace(boolean trace) {
    AtnState.setTrace(trace);
  }


  public boolean getTraceFlow() {
    return AtnState.getTraceFlow();
  }

  public void setTraceFlow(boolean traceFlow) {
    AtnState.setTraceFlow(traceFlow);
  }
  
  /**
   * Parse the general English input.
   *
   * @param input  the input to parse
   * @param die  die monitor for early termination
   *
   * @return the parse results
   */
  public GenericParseResults parseInput(String input, AtomicBoolean die) {
    final GenericParseResults results = genericParser.parse(input, null, die);
    return results;
  }


  public static void main(String[] args) throws IOException {
    final ConfigUtil configUtil = new ConfigUtil(args);
    final DataProperties properties = configUtil.getDataProperties();
    args = properties.getRemainingArgs();

    WordNetParser parser = null;
    BufferedReader in = null;

    parser = new WordNetParser(properties);

    if (args.length > 0) {
      for (String arg : args) {
        final GenericParseResults results = parser.parseInput(arg, null);
        System.out.println(results);
      }
    }
    else {
      in = new BufferedReader(new InputStreamReader(System.in));
      String line = null;

      while ((line = in.readLine()) != null) {
        final GenericParseResults results = parser.parseInput(line, null);
        System.out.println(results.toString());
      }
    }

    if (parser != null) parser.close();
    if (in != null) in.close();
  }
}
