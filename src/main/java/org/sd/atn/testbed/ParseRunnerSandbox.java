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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.atn.AtnParse;
import org.sd.atn.AtnParseBasedTokenizer;
import org.sd.atn.AtnParseResult;;
import org.sd.atn.AtnParseRunner;
import org.sd.atn.AtnState;
import org.sd.atn.GlobalConfig;
import org.sd.atn.ParseInterpretation;
import org.sd.atn.ParseOutputCollector;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;
import org.sd.util.Histogram;
import org.sd.util.UsageUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomDataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlStringBuilder;

/**
 * Class for creating/editing consolidated configurations.
 * <p>
 * @author Spence Koehler
 */
public class ParseRunnerSandbox {
  
//
// (1) Get running options/configs/xmls ...tie back to original resources for browse/edit
//   getOptions()  //topLevel
//   getPrimaryExtractorParams
//     ExtractorParams
//       getOptions()
//       getParseConfig()
//         resourceManager
//           getMetaData
//             XmlMetaData, ClassMetaData, FileMetaData
//         parseConfigProperties:DataProperties
//         id2compoundParsers
//           id2parserWrapper
//             AtnParseOptions
//             AtnParser
//               AtnGrammar
//             AtnParsePrequalifier
//             AtnParseSelector
//             AmbiguityResolver
//             tokenizer?
//
// (2) Browse/edit original resources
//


  public static final String[][] SPECIAL_VARIABLES = new String[][] {
    {"parser-traceflow", "set flag to trace parsing flow in parsers (\"true\" or \"false\")",},
    {"parser-trace", "set flag to trace parsing in parsers (\"true\" or \"false\")",},
    {"tokenizer-current", "set current tokenizer as identified by its focus label",},
  };


  public static final String[] BASE_RSYNC_COMMANDS = new String[] {
    // rsync -lr --delete --delete-excluded --exclude *~ --exclude \\.svn --exclude regression --exclude classifier <resourcesDir> <sandboxDir>
    "rsync",
    "-lr",
    "--delete",
    "--delete-excluded",
    "--exclude",
    "*~",
    "--exclude",
    "\\.svn",
    "--exclude",
    "regression",
    "--exclude",
    "classifier",
  };

  public static final String[] PUBLISH_RSYNC_COMMANDS = new String[] {
    // rsync -lrv --exclude *~ --exclude \\.svn <sandboxDir>/resources <resourcesDir>/..
    "rsync",
    "-lrv",
    "--exclude",
    "*~",
    "--exclude",
    "\\.svn",
  };

  public static final String[] getRsyncCommandAndArgs(String[] baseCommandAndArgs, File sourceDir, File destDir) {
    final int len = baseCommandAndArgs.length;
    final String[] result = new String[len + 2];
    for (int i = 0; i < len; ++i) {
      result[i] = baseCommandAndArgs[i];
    }
    result[len] = sourceDir.getAbsolutePath();
    result[len + 1] = destDir.getAbsolutePath();

    return result;
  }


  private PrintStream out;
  private DataProperties originalProperties;
  private DataProperties currentProperties;
  private DataProperties overrides;

  private File resourcesDir;
  private File sandboxDir;
  private File sandboxResourcesDir;

  private boolean ready;
  private boolean hasSandbox;
  private boolean interactive;
  private boolean finished;

  private AtnParseRunner _parseRunner;
  private ParserParameterManager _parameterManager;
  private ParserParameterFocus _focus;

  public ParseRunnerSandbox(DataProperties p, PrintStream out) {
    this.originalProperties = p;
    this.out = out;
    this.currentProperties = null;
    this.overrides = null;
    this.resourcesDir = null;
    this.sandboxDir = null;
    this.sandboxResourcesDir = null;
    this.ready = false;
    this.hasSandbox = false;
    this.interactive = true;
    this.finished = false;
    this._parseRunner = null;
    this._parameterManager = null;
    this._focus = null;

    init(p);
  }

  /**
   * Reset all settings to the initial state.
   */
  public void reset() {
    init(originalProperties);
  }

  private final void init(DataProperties p) {
    this.currentProperties = p == null ? new DataProperties() : new DataProperties(p);

    this.resourcesDir = makeFile(p.getString("resourcesDir", null));
    this.sandboxDir = makeFile(p.getString("sandboxDir", null));

    if (resourcesDir == null) {
      // infer resourcesDir from parseConfig, if present
      final String parseConfigName = p.getString("parseConfig", null);
      if (parseConfigName != null) {
        for (File file = new File(parseConfigName); file != null && file.exists(); file = file.getParentFile()) {
          if ("resources".equals(file.getName())) {
            this.resourcesDir = file;
            currentProperties.set("resourcesDir", file.getAbsolutePath());
            break;
          }
        }
      }
    }

    if (resourcesDir != null && sandboxDir != null) {
      if (out != null) {
        out.println(new Date() + " : ParseRunnerSandbox resourcesDir=" + resourcesDir.getAbsolutePath());
        out.println(new Date() + " : ParseRunnerSandbox sandboxDir=" + sandboxDir.getAbsolutePath() +
                    " (exists=" + sandboxDir.exists() + ")");
      }

      this.sandboxResourcesDir = new File(sandboxDir, resourcesDir.getName());
      this.hasSandbox = !resourcesDir.equals(sandboxResourcesDir);
      if (hasSandbox) initializeSandbox();
    }
    else {
      this.sandboxResourcesDir = null;
      this.hasSandbox = false;
    }

    this._parseRunner = null;
    this._parameterManager = null;
    this._focus = null;

    this.finished = false;
    this.ready = true;

    initializeSettings(SPECIAL_VARIABLES);
  }

  private final File makeFile(String filename) {
    File result = null;

    if (filename != null && !"".equals(filename)) {
      result = new File(filename);
    }

    return result;
  }

  private final void initializeSandbox() {
    // initialize sandbox if its directory doesn't exist
    boolean needsInit = !sandboxDir.exists();

    if (!needsInit) {
      // initialize sandbox if its directory is empty
      final File[] files = sandboxDir.listFiles();
      needsInit = files == null || files.length == 0;
    }

    if (needsInit) {
      if (!sandboxDir.exists()) sandboxDir.mkdirs();
      initSandbox();
    }
  }

  private final void initializeSettings(String[][] vars) {
    for (String[] var : vars) {
      setVar(var[0], null);
    }
  }

  private final void resetLazyLoadedVars() {
    this._parseRunner = null;
    this._parameterManager = null;
    this._focus = null;
  }

  public final void setInteractive(boolean interactive) {
    this.interactive = interactive;
  }

  public boolean interactive() {
    return interactive;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public boolean finished() {
    return finished;
  }

  public boolean hasSettings() {
    return currentProperties != null && !currentProperties.isEmpty();
  }

  public DataProperties getSettings() {
    return currentProperties;
  }

  public String getSetting(String var) {
    return currentProperties.getString(var, null);
  }

  public final boolean setVar(String var, String val) {
    boolean result = false;

    if ("parser-traceflow".equals(var)) {
      AtnState.setTraceFlow("true".equals(val));
      result = true;
    }
    else if ("parser-trace".equals(var)) {
      AtnState.setTrace("true".equals(val));
      result = true;
    }
    else if ("tokenizer-current".equals(var)) {
      result = setCurrentTokenizer(val);
    }
    else {
      // check whether change will require us to reload the parseRunner
      if (("parseConfig".equals(var) || "supplementalParseConfig".equals(var)) &&
          !matches(currentProperties.getString(var, null), val)) {
        resetLazyLoadedVars();

        // don't set result here so we'll drop through and set the property
      }
    }

    if (!result) {
      currentProperties.set(var, val);
      result = true;
    }

    return result;
  }

  private final boolean matches(String string1, String string2) {
    boolean result = (string1 == string2);

    if (!result) {
      if (string1 != null) {
        result = string1.equals(string2);
      }
      // else, result is false because string1 is null and string2 isn't (==)
    }

    return result;
  }

  /**
   * Make a complete copy of resourcesDir under sandboxDir;
   * <p>
   * @return true if successful; false if failed.
   */
  public final boolean initSandbox() {
    if (!hasSandbox) return true;

    boolean result = false;

    System.out.println(new Date() + ": ParseRunnerSandbox.initSandbox()");

    final String[] commandAndArgs = getRsyncCommandAndArgs(BASE_RSYNC_COMMANDS, resourcesDir, sandboxDir);
    final ExecUtil.ExecResult execResult = ExecUtil.executeProcess(commandAndArgs);

    if (!execResult.failed()) {
      resetLazyLoadedVars();
      result = true;
    }

    return result;
  }

  /**
   * Publish the current sandboxDir changes back to resourcesDir
   */
  public final boolean publishSandbox() {
    if (!hasSandbox) return true;

    boolean result = false;

    System.out.println(new Date() + ": ParseRunnerSandbox.publishSandbox()");

    final String[] commandAndArgs = getRsyncCommandAndArgs(PUBLISH_RSYNC_COMMANDS, new File(sandboxDir, "resources"), resourcesDir.getParentFile());
    final ExecUtil.ExecResult execResult = ExecUtil.executeProcess(commandAndArgs);
    System.out.println(execResult.output);

    if (!execResult.failed()) {
      result = true;
    }

    return result;
  }

  public final AtnParseRunner getParseRunner() {
    AtnParseRunner result = _parseRunner;

    if (_parseRunner == null) {
      result = _parseRunner = buildParseRunner();
    }

    return result;
  }

  public final ParserParameterManager getParameterManager() {
    if (_parameterManager == null) {
      final AtnParseRunner parseRunner = getParseRunner();
      if (parseRunner != null) {
        _parameterManager = new ParserParameterManager(parseRunner);
        currentProperties.set("tokenizer-current", null);
      }
    }
    return _parameterManager;
  }

  public final boolean setCurrentTokenizer(String currentTokenizerLabel) {
    boolean result = false;

    if (currentTokenizerLabel != null) {
      final ParserParameterManager parameterManager = getParameterManager();

      if (parameterManager != null && parameterManager.getTokenizerContainers().containsKey(currentTokenizerLabel)) {
        final ParserParameterContainer parameterContainer = parameterManager.getTokenizerContainer(currentTokenizerLabel);
        if (parameterContainer != null) {
          final StandardTokenizerOptions curTokenizer = parameterContainer.getTokenizerOptions();
          if (curTokenizer != null) {
            this.currentProperties.set("tokenizer-current", currentTokenizerLabel);
            result = true;
          }
        }
      }
    }

    if (!result) {
      this.currentProperties.set("tokenizer-current", null);      
    }

    return result;
  }

  public String buildPrompt(String suffix) {
    if (!hasFocus()) return suffix;

    final StringBuilder result = new StringBuilder();

    final ParserParameterFocus focus = getFocus();
    result.append(focus.getBreadCrumb()).append(suffix);

    return result.toString();
  }

  public boolean hasFocus() {
    return (_focus != null);
  }

  public final ParserParameterFocus getFocus() {
    if (_focus == null) {
      final ParserParameterManager parameterManager = getParameterManager();
      if (parameterManager != null) {
        _focus = new ParserParameterFocus(parameterManager.getParamTree());
      }
    }
    return _focus;
  }

  public final ParserParameterFocus setFocus(String path) {
    ParserParameterFocus focus = getFocus();

    if (focus != null) {
      final Tree<ParserParameterContainer> node = focus.getFocus().getRoot();

      // make "root" optional
      final String rootLabel = node.getData().getLabel();
      if (path != null && path.startsWith(rootLabel + ".")) {
        path = path.substring(rootLabel.length() + 1);
      }

      if (node != focus.getFocus()) {
        this._focus = new ParserParameterFocus(node, focus.getBreadCrumbDelim());
      }

      if (path != null) {
        moveFocusDown(path);
      }
    }

    return _focus;
  }

  public final ParserParameterFocus moveFocusUp(String parentLabel) {
    ParserParameterFocus focus = getFocus();

    if (focus != null) {
      for (Tree<ParserParameterContainer> parent = focus.getFocus().getParent(); parent != null; parent = parent.getParent()) {
        if (parentLabel == null || parentLabel.equals(parent.getData().getLabel())) {
          focus = new ParserParameterFocus(parent, focus.getBreadCrumbDelim());
          break;
        }
      }

      this._focus = focus;
    }

    return focus;
  }

  public final ParserParameterFocus moveFocusDown(String childLabel) {
    ParserParameterFocus focus = getFocus();

    if (focus != null) {
      if (focus.hasChildren()) {
        final String[] labels = childLabel == null ? new String[]{null} : childLabel.split("\\.");
        Tree<ParserParameterContainer> node = focus.getFocus();
        for (String label : labels) {
          final Tree<ParserParameterContainer> child = findChild(node, label);
          node = child;
          if (child == null) {
            break;
          }
        }

        if (node != null) {
          focus = new ParserParameterFocus(node, focus.getBreadCrumbDelim());
        }
      }

      this._focus = focus;
    }

    return focus;
  }

  private final Tree<ParserParameterContainer> findChild(Tree<ParserParameterContainer> parent, String label) {
    Tree<ParserParameterContainer> result = null;

    if (parent.hasChildren()) {
      for (Tree<ParserParameterContainer> child : parent.getChildren()) {
        if (label == null || "".equals(label) || label.equals(child.getData().getLabel())) {
          result = child;
          break;
        }
      }
    }

    return result;
  }

  public void close() {
    if (_parseRunner != null) {
      _parseRunner.close();
    }
  }

  public String buildFocusTree() {
    final StringBuilder result = new StringBuilder();

    final ParserParameterManager parameterManager = getParameterManager();
    if (parameterManager != null) {
      final Tree<ParserParameterContainer> paramTree = parameterManager.getParamTree();
      buildFocusTree(result, paramTree, 2);
    }

    return result.toString();
  }

  public String buildFocusDescendants() {
    final StringBuilder result = new StringBuilder();

    final ParserParameterFocus parameterFocus = getFocus();
    if (parameterFocus != null) {
      final Tree<ParserParameterContainer> paramNode = parameterFocus.getFocus();
      buildFocusTree(result, paramNode, 2);
    }

    return result.toString();
  }

  private final void buildFocusTree(StringBuilder result, Tree<ParserParameterContainer> node, int indentLevel) {
    for (int i = 0; i < indentLevel; ++i) result.append(' ');
    final ParserParameterContainer parameterContainer = node.getData();

    result.append(parameterContainer.getLabel());

    // parser
    if (parameterContainer.isParser()) {
      result.append(" (parser");
      if (parameterContainer.getLabel().equals(currentProperties.getString("parser-current", null))) {
        result.append("-current");
      }
      result.append(")");
    }

    // tokenizer
    if (parameterContainer.isTokenizer()) {
      result.append(" (tokenizer");
      if (parameterContainer.getLabel().equals(currentProperties.getString("tokenizer-current", null))) {
        result.append("-current");
      }
      result.append(")");
    }

    result.append('\n');

    if (node.hasChildren()) {
      for (Tree<ParserParameterContainer> child : node.getChildren()) {
        buildFocusTree(result, child, indentLevel + 2);
      }
    }
  }

  private final AtnParseRunner buildParseRunner() {
    AtnParseRunner result = null;

    final DataProperties options = new DataProperties(currentProperties);

    try {
      result = new AtnParseRunner(options);
    }
    catch (Exception e) {
      System.err.println(new Date() + ": ParseRunnerSandbox.buildParseRunner initialization failure");
      e.printStackTrace(System.err);
    }

    return result;
  }

  public final StandardTokenizerOptions getCurrentTokenizerOptions() {
    final ParserParameterManager parameterManager = getParameterManager(); // make sure this is initialized
    return parameterManager == null ? null : getTokenizerOptions(currentProperties.getString("tokenizer-current", null));
  }

  /**
   * Parse the input using the current parser, passing each input segment with
   * its parse output to the given parse handler.
   *
   * @return the parseRunner instance used.
   */
  public AtnParseRunner parseInput(String input, ParseHandler parseHandler, boolean parseRunnerVerbose) throws IOException {
    final InputAdapter inputAdapter = buildInputAdapter(input);
    return parseInput(inputAdapter, parseHandler, parseRunnerVerbose);
  }

  private final InputAdapter buildInputAdapter(String input) {
    return buildInputAdapter(input, currentProperties, overrides);
  }

  private final void showParseOutput(ParseTokenizerHandler parseHandler, AtnParseRunner parseRunner) {
    for (AtnParseBasedTokenizer outputTokenizer : parseHandler.outputTokenizers) {
      final List<AtnParse> parses =
        outputTokenizer.getParses(parseRunner == null ? null :
                                  parseRunner.getParseConfig().getCompoundParserId2RankMap());
      if (out != null) {
        out.println("\tFinal parses (" + (parses == null ? 0 : parses.size()) + "):\n\t\t" + outputTokenizer.getText());
      }
      if (parses != null) {
        int parseNum = 0;
        for (AtnParse parse : parses) {
          if (out != null) {
            out.println("\t" + (++parseNum) + ": " + parse);
          }
        }
      }
      if (out != null) {
        out.println();
      }
    }
  }

  public static final InputAdapter buildInputAdapter(String input, DataProperties options, DataProperties overrides) {
    InputAdapter inputAdapter = null;

    if (input.indexOf("<text") >= 0) {
      try {
        final DomElement textElt = XmlFactory.buildDomNode(input, false).asDomElement();
        inputAdapter = new InputAdapter(textElt, options, overrides);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    else {
      inputAdapter = new InputAdapter(input, options, overrides);
    }

    return inputAdapter;
  }

  /**
   * Parse the input using the current parser, passing each input segment with
   * its parse output to the given parse handler.
   *
   * @return the parseRunner instance used.
   */
  public AtnParseRunner parseInput(InputAdapter inputAdapter, ParseHandler parseHandler, boolean parseRunnerVerbose) throws IOException {
    final AtnParseRunner parseRunner = getParseRunner();
    doParseInput(inputAdapter, parseHandler, parseRunnerVerbose, parseRunner);
    return parseRunner;
  }

  private final void doParseInput(InputAdapter inputAdapter, ParseHandler parseHandler, boolean parseRunnerVerbose, AtnParseRunner parseRunner) throws IOException {
    if (parseRunner != null) {

      if (parseRunnerVerbose) {
        parseRunner.setVerbose(true);
      }

      int pNum = 0;
      while (inputAdapter.hasNext()) {
        final AtnInput atnInput = inputAdapter.next();

        if (out != null) {
          out.println("Parsing input #" + (++pNum) + ": " + atnInput.getText());
        }

        final ParseOutputCollector parseOutput = parseRunner.parseInput(atnInput, atnInput.getOverrideOptions(), null);
        //final ParseSourceInfo sourceInfo = new ParseSourceInfo(atnInput.getText(), false, false, false, null, null, null);
        //output.setParseSourceInfo(sourceInfo);

        if (out != null) {
          out.println();
        }

        parseHandler.handleParse(atnInput.getText(), parseOutput);
      }

      if (parseRunnerVerbose) {
        parseRunner.setVerbose(false);
      }
    }
    else if (out != null) {
      out.println("ERROR: No parseRunner.");
    }
  }

  private final StandardTokenizerOptions getTokenizerOptions(String label) {
    StandardTokenizerOptions result = null;

    if (label != null) {
      final ParserParameterManager parameterManager = getParameterManager();
      if (parameterManager != null) {
        final ParserParameterContainer parameterContainer = parameterManager.getTokenizerContainer(label);
        if (parameterContainer != null) {
          result = parameterContainer.getTokenizerOptions();
        }
      }
    }

    return result;
  }

  private final void showDataProperties(DataProperties dataProperties) {
    if (dataProperties != null && out != null) {
      final Properties p = dataProperties.getProperties();
      if (p != null) {
        for (String key : p.stringPropertyNames()) {
          out.println("\t\t" + key + "=" + p.getProperty(key));
        }
      }
      for (DomDataProperties ddp : dataProperties.getAllDomDataProperties()) {
        final DomElement elt = ddp.getDomElement();
        System.out.println("\n" + elt.asPrettyString(null, 4, 2));
      }
    }
  }

  /**
   * Add "x=y" value to overrides.
   */
  private final void addOverride(String text) {
    if (text != null && !"".equals(text)) {
      if (overrides == null) overrides = new DataProperties();
      doAddProperty(overrides, text);
    }
  }

  /**
   * Add "x=y" value to currentProperties.
   */
  private final void addCurrentProperties(String text) {
    if (text != null && !"".equals(text)) {
      if (currentProperties == null) currentProperties = new DataProperties();
      doAddProperty(currentProperties, text);
    }
  }

  /**
   * Add "x=y" value to given dataProperties.
   */
  private final void doAddProperty(DataProperties dataProperties, String arg) {
    String key = null;
    String value = null;

    final int eqPos = arg.indexOf('=');

    if (eqPos < 0) {
      // no "=", set key's value to null (effectively removing key)
      key = arg.trim();
      value = null;
    }
    else {
      key = arg.substring(0, eqPos).trim();
      value = arg.substring(eqPos + 1).trim();
    }

    dataProperties.set(key, value);
  }

  private final void showAllSettings() {
    showDataProperties(currentProperties);
    if (overrides != null) {
      out.println("\toverrides:");
      showDataProperties(overrides);
    }
  }

  public boolean handleCommand(String line) {

    boolean handled = false;

    try {
      boolean showCommands = false;
      final String lineLC = line.trim().toLowerCase();

      if ("q".equals(lineLC) || "quit".equals(lineLC) || "exit".equals(lineLC)) {
        if (out != null) {
          out.println("Exiting...");
          setFinished(true);
        }
        return true;
      }
      else if ("h".equals(lineLC) || "?".equals(line) || "help".equals(lineLC)) {
        showCommands = true;
        handled = true;
      }
      else if ("settings".equals(lineLC)) {
        if (out != null) {
          out.println("Settings:");
          if (resourcesDir != null) {
            out.println("\tresourcesDir=" + resourcesDir.getAbsolutePath());
          }
          if (sandboxDir != null) {
            out.println("\tsandboxDir=" + sandboxDir.getAbsolutePath());
          }
          out.println("\tcurrentProperties:");
          showAllSettings();
        }

        handled = true;
      }
      else if (lineLC.startsWith("verboseload")) {
        final String text = line.substring(11).trim();
        if ("".equals(text)) {
          // toggle
          GlobalConfig.setVerboseLoad(!GlobalConfig.verboseLoad());
        }
        else if ("true".equals(text) || "on".equals(text)) {
          GlobalConfig.setVerboseLoad(true);
        }
        else {
          GlobalConfig.setVerboseLoad(false);
        }

        if (out != null) {
          out.println("verboseLoad is " + (GlobalConfig.verboseLoad() ? "ON" : "OFF"));
        }

        handled = true;
      }
      else if (lineLC.equals("reset")) {
        reset();
        if (out != null) {
          out.println(new Date() + ": RESET to reload all resources");
        }
        handled = true;
      }
      else if (lineLC.startsWith("parse ")) {
        final String text = line.substring(6);

        final ParseTokenizerHandler parseHandler = new ParseTokenizerHandler();
        final AtnParseRunner parseRunner = parseInput(text, parseHandler, true);

        if (parseRunner != null) {
          showParseOutput(parseHandler, parseRunner);
        }
        else {
          final String currentParserLabel = getSettings().getString("parser-current", null);
          if (out != null) {
            out.println("WARNING: parser-current (" + currentParserLabel + ") is not valid");
          }
        }

        handled = true;
      }
      else if (lineLC.startsWith("interp ")) {
        final String text = line.substring(6);

        final ParseInterpHandler parseHandler = new ParseInterpHandler(out);
        final AtnParseRunner parseRunner = parseInput(text, parseHandler, false);

        if (parseRunner == null) {
          final String currentParserLabel = getSettings().getString("parser-current", null);
          if (out != null) {
            out.println("WARNING: parser-current (" + currentParserLabel + ") is not valid");
          }
        }

        handled = true;
      }
      else if (lineLC.startsWith("tokenize ")) {
        final String text = line.substring(9);

        final StandardTokenizerOptions tokenizerOptions = getCurrentTokenizerOptions();

        final String currentTokenizerLabel = getSettings().getString("tokenizer-current", null);
        if (tokenizerOptions != null) {
          if (out != null) {
            out.println("Tokenizing (via " + currentTokenizerLabel + ") input: " + text);
          }

          final Tree<Token> tokens = StandardTokenizerFactory.fullTokenization(text, tokenizerOptions);

          if (out != null) {
            for (Iterator<Tree<Token>> iter = tokens.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
              final Tree<Token> curNode = iter.next();
              for (int indentPos = 0; indentPos < curNode.depth(); ++indentPos) {
                out.print("  ");
              }
              out.println(curNode.getData().toString());
            }
          }
        }
        else {
          if (out != null) {
            out.println("WARNING: tokenizer-current (" + currentTokenizerLabel + ") is not valid");
          }
        }

        handled = true;
      }
      else if (lineLC.startsWith("sandbox ")) {
        final String sandboxCmd = line.substring(8);
        if ("reset".equals(sandboxCmd)) {
          if (out != null) {
            out.println("Resetting sandbox to initial state...");
          }
          initSandbox();
          handled = true;
        }
        else if ("publish".equals(sandboxCmd)) {
          if (out != null) {
            out.println("Publishing sandbox resources back to source controlled area...");
          }
          publishSandbox();
          handled = true;
        }
      }
      else if ("reload-resources".equals(lineLC)) {
        if (out != null) {
          out.println("Marking sandbox resources for reload.");
        }
        resetLazyLoadedVars();
        handled = true;
      }
      else if (lineLC.startsWith("focus")) {
        final String[] focusCmd = line.split("\\s+");

        boolean showChildren = false;

        // focus [show tree]
        if (focusCmd.length == 1 || (focusCmd.length == 3 && "show".equals(focusCmd[1]) && "tree".equals(focusCmd[2]))) {
          final String focusTree = buildFocusTree();
          if (out != null) {
            out.println("FocusTree:\n" + focusTree);
          }
          handled = true;
        }

        // focus set path
        else if ("set".equals(focusCmd[1])) {
          setFocus((focusCmd.length > 2) ? focusCmd[2] : null);
          showChildren = true;
          handled = true;
        }

        // focus up [parent-label]
        else if ("up".equals(focusCmd[1])) {
          moveFocusUp((focusCmd.length > 2) ? focusCmd[2] : null);
          showChildren = true;
          handled = true;
        }

        // focus down [child-label]
        else if ("down".equals(focusCmd[1])) {
          moveFocusDown((focusCmd.length > 2) ? focusCmd[2] : null);
          showChildren = true;
          handled = true;
        }

        // focus show children|[data]
        else if ("show".equals(focusCmd[1])) {
          // focus show [data]
          if (focusCmd.length == 2 || "data".equals(focusCmd[2])) {
            final String data = getFocus().getFocus().getData().getDataAsString();
            if (out != null) {
              out.print(data);
            }
            handled = true;
          }

          // focus show children
          else if ("children".equals(focusCmd[2])) {
            showChildren = true;
            handled = true;
          }

          // focus show descendants
          else if ("descendants".equals(focusCmd[2])) {
            final String focusTree = buildFocusDescendants();
            if (out != null) {
              out.println("FocusDescendants:\n" + focusTree);
            }
            handled = true;
          }
        }

        if (showChildren) {
          final ParserParameterFocus focus = getFocus();
          if (out != null) {
            out.println("\t" + focus.getBreadCrumb());

            if (focus.hasChildren()) {
              for (Tree<ParserParameterContainer> child : focus.getFocus().getChildren()) {
                out.println("\t\t" + child.getData().getLabel());
              }
            }
            else {
              out.println("\t\t<terminal>");
            }
          }
        }
      }
      else if (lineLC.startsWith("overrides")) {
        final String text = line.substring(9).trim();

        // overrides [show]
        if ("".equals(text) || "show".equals(text)) {
          showDataProperties(overrides);
        }

        // overrides clear
        else if ("clear".equals(text)) {
          if (out != null) {
            out.println("Clearing overrides.");
          }
          overrides = null;
        }

        // overrides args
        else {
          if (out != null) {
            out.println("Adding overrides '" + text + "'");
          }
          addOverride(text);
        }

        handled = true;
      }
      else if (lineLC.startsWith("usage ")) {
        final String text = line.substring(6).trim();
        final String[] cmdargs = text.split("\\s+");
        final boolean immediateOnly = cmdargs.length > 1 ? "true".equals(cmdargs[1]) : true;
        final String classpath = cmdargs[0];
        final Map<String, List<String>> usageNotes = UsageUtil.getUsageNotes(classpath, immediateOnly);

        if (out != null) {
          if (usageNotes == null) {
            out.println("WARNING: No class found named '" + classpath + "'");
          }
          else if (usageNotes.size() == 0) {
            out.println("No usage notes exist for class named '" + classpath + "'");
          }
          else {
            out.println(UsageUtil.asString(usageNotes));
          }
        }

        handled = true;
      }
      else {
        final int eqPos = line.indexOf('=');
        if (eqPos >= 0) {
          final String var = line.substring(0, eqPos).trim();
          final String val = eqPos == line.length() - 1 ? null : line.substring(eqPos + 1).trim();
          handled = setVar(var, val);
          if (out != null) {
            if (handled) {
              out.println("Setting " + var + " to '" + val + "'");
            }
            else {
              out.println("WARNING: Failed to set var '" + var + "' to value '" + val + "'");
              handled = true;
            }
          }
        }
      }

      if (!handled) {
        if (out != null) {
          out.println("Unrecognized command.");
        }
      }

      if (showCommands && out != null) {
        out.println("Commands:");
        out.println("\th, ?, help: This help message");
        out.println("\tq, quit, exit: quit");
        out.println("\tsettings: show settings (variables) and overrides");
        out.println("\tverboseLoad [true/false]: enable/disable verbose loading");
        out.println("\treset: force reload of resources");
        out.println("\tparse <text to parse>: parse the text (no quotes or angle brackets) using the current parser");
        out.println("\tinterp <text to parse and interpret>: parse and interpret the text using the current parser");
        out.println("\ttokenize <text to tokenize>: tokenize the text (no quotes or angle brackets) using the current tokenizer");
        out.println("\tsandbox reset: restore sandbox to initial state (from version controlled location)");
        out.println("\tsandbox publish: publish sandbox changes back to version controlled location");
        out.println("\treload-resources: reload resources from sandbox (e.g. due to changes)");
        out.println("\tfocus [show tree]: show focus tree");
        out.println("\tfocus set path: set current focus to the given path");
        out.println("\tfocus up [parent-label]: move focus up one level or to the optional parent");
        out.println("\tfocus down [child-label]: move focus down to the optional (or first) child");
        out.println("\tfocus show [data]: show current focus data");
        out.println("\tfocus show children: show children of current focus");
        out.println("\tfocus show descendants: show all descendants of current focus");
        out.println("\tusage classpath [true|false]: show usage notes for the given classpath w/optional immediateOnly flag value.");
        out.println("\toverrides [show]: show (when no args) or set overrides");
        out.println("\toverrides clear: clear all overrides");
        out.println("\toverrides args: add args (of form att=val) to overrides");

        out.println("\nSpecial Variables:");
        for (String[] specialVar : SPECIAL_VARIABLES) {
          String delim = "\t";
          for (String x : specialVar) {
            out.print(delim + x);
            delim = ": ";
          }
          out.println();
        }
      }
    }
    catch (Exception e) {
      final PrintStream eOut = out != null ? out : System.err;
      eOut.println("\nERROR:");
      e.printStackTrace(eOut);
    }

    return handled;
  }


  /**
   * Interface for handling parse output generated over an input segment.
   */
  public static interface ParseHandler {

    /** Hook to handle parse output generated over the input segment. */
    public void handleParse(String inputSegment, ParseOutputCollector parseOutput);
  }

  public static final class ParseTokenizerHandler implements ParseHandler {
    public final List<AtnParseBasedTokenizer> outputTokenizers;

    public ParseTokenizerHandler() {
      this.outputTokenizers = new ArrayList<AtnParseBasedTokenizer>();
    }

    public void handleParse(String inputSegment, ParseOutputCollector parseOutput) {
      final AtnParseBasedTokenizer outputTokenizer = (AtnParseBasedTokenizer)parseOutput.getOutputTokenizer();
      if (outputTokenizer != null) {
        outputTokenizers.add(outputTokenizer);
      }
    }
  }

  public static final class ParseInterpHandler implements ParseHandler {
    public final List<ParseInterpretation> interps;
    private PrintStream out;

    public ParseInterpHandler(PrintStream out) {
      this.interps = new ArrayList<ParseInterpretation>();
      this.out = out;
    }

    public void handleParse(String inputSegment, ParseOutputCollector parseOutput) {
      // show interps for each parse
      final List<AtnParseResult> parseResults = parseOutput.getParseResults();
      if (parseResults != null) {
        for (AtnParseResult parseResult : parseResults) {
          final int parseCount = parseResult.getNumParses();
          for (int parseNum = 0; parseNum < parseCount; ++parseNum) {
            final AtnParse parse = parseResult.getParse(parseNum);
            if (parse.getSelected()) {

              // show parse
              if (out != null) {
                final String ruleId = parse.getStartRule().getRuleId();
                final String ruleText = (ruleId == null) ? "" : " [" + ruleId + "]";
                out.println(" Parse #" + (parseNum + 1) + ": \"" +
                            parse.getParsedText() + "\" == " +
                            parse.getParseTree().toString() + ruleText);
              }

              final List<ParseInterpretation> parseInterps = parse.getParseInterpretations();
              if (parseInterps != null) {
                interps.addAll(parseInterps);

                // show interps
                if (out != null) {
                  int interpNum = 1;
                  for (ParseInterpretation interp : parseInterps) {
                    // show interp
                    out.println("   Interp #" + interpNum + ":\n" + interp);
                    ++interpNum;
                  }
                }
              }
            }
          }
        }
      }
    }
  }


  public static void doMain(String[] args, File cmdFile, File outFile, boolean interactive, String[] usage)
    throws IOException {

    // Properties:
    //
    //   ParseRunnerSandbox :
    //   - sandboxDir -- directory to which resources are copied for testing environment
    // 
    //   AtnParseRunner :
    //   - resourcesDir -- top parsing resource directory 
    //   - parseConfig -- config for parser to test
    //   - supplementalParseConfig -- supplemental parse config
    //   - ...
    //
    // NOTE: if arg is X.properties, then properties file's properties are loaded.
    //

    final DataProperties dataProperties = new DataProperties(args);
    final PrintStream out = outFile == null ? System.out : FileUtil.getPrintStream(outFile, false);

    final ParseRunnerSandbox mgr = new ParseRunnerSandbox(dataProperties, out);
    mgr.setInteractive(interactive);

    String line = null;
    String prompt = mgr.buildPrompt("> ");
    final BufferedReader in = cmdFile == null ? new BufferedReader(new InputStreamReader(System.in)) : FileUtil.getReader(cmdFile);
    out.print(prompt);

    while ((line = in.readLine()) != null) {
      if ("".equals(line) || line.charAt(0) == '#') continue;

      if (cmdFile != null) out.println(line);  // echo command line in headless mode

      if (!mgr.handleCommand(line)) {
        if (!mgr.interactive()) {
          System.err.println("*** QUITTING! Unable to handle line: " + line);
          break;
        }
      }

      if (mgr.finished()) {
        break;
      }

      prompt = mgr.buildPrompt("> ");
      out.print(prompt);
    }

    in.close();
    mgr.close();
    out.println("\nGoodBye.");

    if (outFile != null) out.close();
  }

  public static void main(String[] args) throws IOException {
    doMain(args, null, null, true, new String[] {
        "[X.properties]*",
        "[key=value]*",
      });
  }
}
