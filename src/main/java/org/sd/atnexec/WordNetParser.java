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
  //  resourcesDir -- path to resources (defaults to sd-tools/resources)
  //  wordNetConfig -- (optional, default=generic-wordnet.config.xml) name of
  //                   config relative to $resourcesDir/atn/config.
  //                   NOTE: this takes precedence over parseConfig if both
  //                   are present.
  //  supplementalWordNetConfig -- (optional) name of spuplemental config
  //                               relative to $resourcesDir/atn/config
  //  parseConfig -- (optional) absolute path to wordnet-config.xml
  //  supplementalConfig -- (optional) absolute path to supplement
  //

  public WordNetParser(DataProperties properties) throws IOException {
    final boolean hasWordNetConfig = properties.hasProperty("wordNetConfig");
    final boolean hasParseConfig = properties.hasProperty("parseConfig");

    if (hasWordNetConfig || !hasParseConfig) {
      final String supplementalWordNetConfig = properties.getString("supplementalWordNetConfig", "");
      //NOTE: assuming supplement follows same convention as primary config

      final String wordNetConfig = properties.getString("wordNetConfig", "generic-wordnet.config.xml");
      if (hasParseConfig) properties = new DataProperties(properties);
      if (!"".equals(wordNetConfig) && wordNetConfig.charAt(0) == '/') {
        // absolute path
        properties.set("parseConfig", wordNetConfig);
        properties.set("supplementalConfig", supplementalWordNetConfig);
        properties.set("parseFlow", "");
      }
      else {
        // relative path (to resources/atn/config)
        final File resourcesDir = properties.getFile("resourcesDir", "workingDir");
        properties.set("parseConfig", new File(resourcesDir, "atn/config/" + wordNetConfig).getAbsolutePath());
        properties.set("supplementalConfig", "".equals(supplementalWordNetConfig) ? "" : "config/" + supplementalWordNetConfig);
        properties.set("parseFlow", "");
      }
    }

    this.genericParser = new GenericParser(properties);
  }

  public void close() {
    genericParser.close();
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
