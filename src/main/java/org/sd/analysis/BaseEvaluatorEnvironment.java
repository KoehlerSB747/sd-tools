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
package org.sd.analysis;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilder;
import org.sd.util.tree.TreeBuilderFactory;
import org.sd.xml.DataProperties;

/**
 * Base implementation of an EvaluatorEnvironment, intended to be overridden
 * with specific functionality.
 * <p>
 * Data properties managed at this level include:
 * <ul>
 * <li>commandFile -- (optional, default=null) when null an interactive reader is built;
 *                    otherwise, commands are read from the specified file.</li>
 * <li>dir -- (optional) establishes the working directory for finding commandFile
 *            and other resources.
 * </ul>
 *
 * @author Spencer Koehler
 */
public class BaseEvaluatorEnvironment implements EvaluatorEnvironment {
  
  /**
   * Command line pattern for extracting variable (group1) and value (group2)
   * data from a set command of the form: variable=value
   */
  static final Pattern SET_VARIABLE_PATTERN = Pattern.compile(
    "^\\s*(\\w+)\\s*=\\s*(.*?)\\s*$");

  /**
   * String tree builder for parsing function expressions.
   */
  static final TreeBuilder<String> TREE_BUILDER = TreeBuilderFactory.getStringTreeBuilder();


  protected final DataProperties dataProperties;
  
  private File commandFile;
  private BufferedReader reader;
  private boolean interactive;
  private final Map<String, AnalysisObject> variables;
  private final Map<String, AnalysisFunction> functions;
  private PrintStream out;
  private PrintStream err;

  public BaseEvaluatorEnvironment(String[] args) {
    this(new DataProperties(args));
  }

  public BaseEvaluatorEnvironment(DataProperties dataProperties) {
    this.dataProperties = dataProperties;
    this.variables = new TreeMap<String, AnalysisObject>();
    this.functions = new TreeMap<String, AnalysisFunction>();
    addBasicFunctions();
    addMoreFunctions();
    init(dataProperties);
  }

  private final void init(DataProperties dataProperties) {
    this.commandFile = dataProperties.getFile("commandFile", "dir");
    this.reader = null;
    this.interactive = (dataProperties.getString("commandFile", null) == null);
    this.out = System.out;
    this.err = System.err;
  }

  private final void addBasicFunctions() {
    defineFunction("echo", new AnalysisFunction() {
        public AnalysisObject execute(AnalysisObject[] args) {
          final StringBuilder result = new StringBuilder();
          if (args != null) {
            for (AnalysisObject arg : args) {
              if (result.length() > 0) result.append(' ');
              result.append(arg.toString());
            }
          }
          return new BasicAnalysisObject<String>(result.toString());
        }

        public String toString() {
          return "Echos args, space-delimited.";
        }
      });

    defineFunction("commands", new AnalysisFunction() {
        public AnalysisObject execute(AnalysisObject[] args) {
          final StringBuilder result = new StringBuilder();
          for (Map.Entry<String, AnalysisFunction> entry : functions.entrySet()) {
            if (result.length() > 0) result.append('\n');
            result.
              append(entry.getKey()).
              append('\t').
              append(entry.getValue().toString());
          }
//todo: return a map or vector AnalysisObject?
          return new BasicAnalysisObject<String>(result.toString());
        }

        public String toString() {
          return "Shows all available commands.";
        }
      });

    defineFunction("vars", new AnalysisFunction() {
        public AnalysisObject execute(AnalysisObject[] args) {
          final StringBuilder result = new StringBuilder();
          for (Map.Entry<String, AnalysisObject> entry : variables.entrySet()) {
            if (result.length() > 0) result.append('\n');
            result.
              append(entry.getKey()).
              append('=').
              append(entry.getValue().toString());
          }
//todo: return a map or vector AnalysisObject?
          return new BasicAnalysisObject<String>(result.toString());
        }

        public String toString() {
          return "Shows all available variables.";
        }
      });

    defineFunction("loadcsv", new CsvLoaderFunction(dataProperties));

    defineFunction("loadxml", new XmlLoaderFunction(dataProperties));

    //todo: add history function
  }

  public static final BufferedReader getInteractiveReader() {
    return new BufferedReader(new InputStreamReader(System.in));
  }

  /**
   * Get this instance's properties.
   */
  @Override
  public DataProperties getDataProperties() {
    return dataProperties;
  }

  /**
   * Reset this environment's variables and/or functions to the initial state.
   */
  @Override
  public void reset(boolean variables, boolean functions) {
    if (variables) {
      this.variables.clear();
    }
    if (functions) {
      this.functions.clear();
      addBasicFunctions();
      addMoreFunctions();
    }
  }

  protected void addMoreFunctions() {
    // inteneded for extenders to override, calling
    // "defineFunction" for custom commands
  }

  /**
   * Read the next line from the environment.
   *
   * @return the next line to be read or null if input has been exhausted or
   *         the environment has been closed.
   */
  @Override
  public String readLine() throws IOException {
    return (reader == null) ? null : reader.readLine();
  }

  /**
   * Close this environment and any open resources.
   */
  @Override
  public void close() {
    if (reader != null) {
      try {
        reader.close();
      }
      catch (Exception e) {
        //ignore
      }
      reader = null;

      if (interactive) {
        out.println("Done.");
      }
    }
  }

  /**
   * Set the output stream for this instance, returning the existing.
   */
  @Override
  public PrintStream setOut(PrintStream out) {
    final PrintStream result = out;
    this.out = out;
    return result;
  }

  protected final PrintStream getOut() {
    return out;
  }

  /**
   * Set the error output stream for this instance, returning the existing.
   */
  @Override
  public PrintStream setErr(PrintStream err) {
    final PrintStream result = err;
    this.err = err;
    return result;
  }

  protected final PrintStream getErr() {
    return err;
  }

  /**
   * Set the reader, returning the existing reader.
   */
  @Override
  public BufferedReader setReader(BufferedReader reader) {
    final BufferedReader result = this.reader;
    this.reader = reader;
    return result;
  }

  /**
   * Prepare for the next input line.
   * 
   * @param lineNum  the number (0-based) of the next input line.
   *
   * @return true to proceed to handling; false to exit the evaluation loop
   *         (which will trigger a subsequent call to prepareForExit(false)).
   */
  @Override
  public boolean prepareForInput(int lineNum) {
    boolean result = true;

    if (lineNum == 0) {
      if (this.reader == null) {
        if (interactive) {
          this.reader = getInteractiveReader();
        }
        else {
          // error out if have troubles
          try {
            this.reader = FileUtil.getReader(commandFile);
          }
          catch (Exception e) {
            throw new IllegalStateException("Bad commandFile=" + commandFile, e);
          }
        }
      }
      else {
        this.interactive = false;
      }

      //todo: setup out/err from properties if indicated and not already set
    }

    // if interactive, show prompt
    if (interactive) {
      result = showInteractivePrompt(lineNum);
    }

    return result;
  }

  /**
   * Show the interactive prompt for the given line number.
   * <p>
   * Can be overridden.
   *
   * @return value for prepareForInput's result.
   */
  protected boolean showInteractivePrompt(int lineNum) {
    boolean result = true;

    out.print("[" + lineNum + "]> ");

    return result;
  }

  /**
   * Extenders can override this and return a "handled" result to fully
   * bypass all base evaluator line handling.
   */
  protected HandledAndResult doFirstCrackHandling(int lineNum, String line) {
    return null;
  }

  /**
   * Extenders can override this to handle a line not managed by the base
   * evaluator.
   */
  protected HandledAndResult doHandleUnmanagedLine(int lineNum, String line) {

    // this should be overridden by an extending class
    err.println("WARNING: Unrecognized command line #" + lineNum + ": " + line);

    return new HandledAndResult(false, true);
  }

  /**
   * Handled the current line.
   *
   * @param lineNum  the number of the line to be handled.
   * @param line  the line to be handled.
   *
   * @return true to continue processing lines; false to exit (which will
   *              trigger a subsequent call to prepareForExit).
   */
  @Override
  public boolean handleInputLine(int lineNum, String line) {
    HandledAndResult hr = null;
    VariableAndValue varAndValue = null;

    // Call hook for extenders to fully handle the line
    hr = doFirstCrackHandling(lineNum, line);
    if (hr != null && hr.handled) {
      return hr.result;
    }

    return evaluateInputLine(lineNum, line);
  }

  /**
   * Prepare for exit after an input line's handling returned false.
   *
   * @param lineNum  the number of the line not handled.
   * @param line  the line not handled.
   *
   * @return a handled value suitable for wrapUp.
   */
  @Override
  public boolean prepareForExit(int lineNum, String line) {
    boolean result = true;

    // this should be overridden by an extending class
    if (interactive) {
      out.println("Exiting.");
    }

    return result;
  }

  /**
   * Wrap up after all input lines have been processed.
   * 
   * @param lineNum  the last prepped line
   * @param handled  whether the last line was successfully prepped, handled,
   *                 and, if applicable prepped for exit.
   */
  @Override
  public void wrapUp(int lineNum, boolean handled) {
    // this should be overridden by an extending class if there is any computation
    // necessary, e.g., for finalizing the environment's "value".
    if (interactive) out.println();
  }


  protected final boolean evaluateInputLine(int lineNum, String line) {
    // handle setting a variable (var=value)
    final VariableAndValue varAndValue = getVariableAndValue(line);
    if (varAndValue != null) {
      final AnalysisObject oldValue = setVariable(varAndValue.getVarName(), varAndValue.getValue());
      if (interactive) {
        out.print("set " + varAndValue.toString());
        if (oldValue != null) {
          out.print(" (oldValue was: " + oldValue.toString() + ")");
        }
        out.println();
      }
      return true;
    }

    // parse line structure
    line = line.trim();

    // handle quit/exit
    if ("exit".equals(line) || "quit".equals(line)) {
      return false;
    }

    // handle show/help commands
    //...return true

    // evaluate as expression
    final AnalysisObject expressionResult = evaluateExpression(line, true);
    if (expressionResult != null) {
      out.println(expressionResult.toString());
      return true;
    }

    // handle other/custom commands
    final HandledAndResult hr = doHandleUnmanagedLine(lineNum, line);

    return hr == null ? true : hr.result;
  }

  protected final AnalysisObject evaluateExpression(String line, boolean acceptSingleTokenAsCommand) {
    AnalysisObject result = null;

    // echo empty line
    if ("".equals(line)) {
      result = new BasicAnalysisObject<String>(line);
    }
    else if (line.charAt(0) == '(') {  // evaluate function
      result = evaluateFunctionTree(line);
    }
    else if (line.charAt(0) == '<') {  // load xml string
      result = new XmlAnalysisObject(line);
    }
    else if (line.indexOf(" ") < 0) {  // evaluate single token
      
      // handle reference to variable
      final VariableAndValue varAndValue = lookupVariable(line);
      if (varAndValue != null) {
        result = varAndValue.getValue();
      }

      // execute single token command
      else if (acceptSingleTokenAsCommand && isCommand(line)) {
        result = executeCommand(line, null);
      }

      else {
        // echo back the single token
        result = new BasicAnalysisObject<String>(line);
      }
    }
    else {
      // evaluate function with multiple tokens of form "command arg1 arg2 ..."
      result = evaluateFunctionTree("(" + line + ")");
    }

    return result;
  }

  /**
   * Evaluate a function of the form "(function args)".
   */
  protected final AnalysisObject evaluateFunctionTree(String line) {
    AnalysisObject result = null;

    final Tree<String> functionTree = TREE_BUILDER.buildTree(line);
    if (functionTree != null) {
      result = evaluateFunctionTree(functionTree);
    }
    else {
      // error: bad tree syntax
      err.println("ERROR: Bad function syntax in '" + line + "'");
    }

    return result;
  }

  protected final AnalysisObject evaluateFunctionTree(Tree<String> functionTree) {
    AnalysisObject result = null;

    if (functionTree != null) {
      final AnalysisObject functionValue = evaluateExpression(functionTree.getData(), false);
      if (functionValue != null) {
        final String function = functionValue.toString();
        if (isCommand(function)) {
          final AnalysisObject[] args = evaluateArgs(functionTree);
          result = executeCommand(function, args);
        }
        else {
          // error: unknown command
          err.println("ERROR: Unknown command '" + function + "'");
        }
      }
      else {
        // error: unresolvable function
        err.println("ERROR: Unresolvable function '" + functionTree.getData() + "'");
      }
    }

    return result;
  }

  protected final boolean isCommand(String command) {
    return functions.containsKey(command);
  }

  protected final AnalysisObject[] evaluateArgs(Tree<String> commandTree) {
    AnalysisObject[] result = null;

    final int numChildren = commandTree.numChildren();
    if (numChildren > 0) {
      result = new AnalysisObject[numChildren];
      final List<Tree<String>> children = commandTree.getChildren();
      int childNum = 0;
      for (Tree<String> child : children) {
        if (child.hasChildren()) {
          result[childNum] = evaluateFunctionTree(child);
        }
        else {
          result[childNum] = evaluateExpression(child.getData(), false);
        }
        if (result[childNum] == null) {
          err.println("ERROR: Bad tree arg '" + commandTree + "'");
        }
        ++childNum;
      }
    }

    return result;
  }

  protected final AnalysisObject executeCommand(String command, AnalysisObject[] args) {
    AnalysisObject result = null;
    boolean validArgs = true;

    // NOTE: error if args[i] == null, but ok if args[i].toString is null
    if (args != null) {
      // check for errors (null args)
      int argNum = 1;
      for (AnalysisObject arg : args) {
        if (arg == null) {
          validArgs = false;
          err.println("ERROR: Bad command '" + command + "' arg #" + argNum);
        }
      }
    }

    if (validArgs) {
      final AnalysisFunction function = retrieveFunction(command);
      if (function != null) {
        result = function.execute(args);
      }
      else {
        // error: undefined command
        err.println("ERROR: Undefined command '" + command + "'");
      }
    }

    return result;
  }

  /**
   * Given a variable reference of the form $var, lookup the the variable's
   * value (possibly null).
   * <p>
   * Note that var may be of the form: $var.ref, in which case the ref should
   * be accessed from var's value.
   * <p>
   * @return null if var isn't a variable reference, or the retrieved
   *         VariableAndValue whose value may be null if the variable
   *         or its reference doesn't have a value.
   */
  protected final VariableAndValue lookupVariable(String var) {
    VariableAndValue result = null;

    final char c0 = var.charAt(0);
    if (c0 == '$' && var.length() > 1) {
      final String[] refAccess = splitRefAccess(var.substring(1));
      final AnalysisObject varEval = evaluateExpression(refAccess[0], false);
      if (varEval != null) {
        final String varEvalString = varEval.toString();
        AnalysisObject value = variables.get(varEvalString);
        if (refAccess.length == 2) {
          final String[] refs = refAccess[1].split("\\.");
          for (String ref : refs) {
            if (value == null) break;
            value = value.access(ref);
          }
        }
        //NOTE: "variable" portion of result includes ref
        result = new VariableAndValue(varEvalString, value);
      }
    }

    return result;
  }

  /**
   * Split a string of the form X.Y into {X, Y}. If just X, then {X}.
   */
  protected static final String[] splitRefAccess(String refEvalString) {
    String[] result = null;

    final int dotPos = refEvalString.indexOf('.');
    if (dotPos < 0) {
      result = new String[]{refEvalString};  // no splitting necessary
    }
    else {
      result = new String[] {
        refEvalString.substring(0, dotPos),
        refEvalString.substring(dotPos + 1),
      };
    }

    return result;
  }

  /**
   * Set the variable to the new value.
   * 
   * @return the old value, if any, or null.
   */
  protected final AnalysisObject setVariable(String varName, AnalysisObject newValue) {
    final AnalysisObject oldValue = variables.get(varName);
    variables.put(varName, newValue);
    return oldValue;
  }

  /**
   * Get the variable and value for the line if it is of the form var=val,
   * or null.
   */
  private final VariableAndValue getVariableAndValue(String line) {
    VariableAndValue result = null;

    final Matcher m = SET_VARIABLE_PATTERN.matcher(line);
    if (m.matches()) {
      final String varName = m.group(1);
      final AnalysisObject varValue = evaluateExpression(m.group(2), false);

      result = new VariableAndValue(varName, varValue);
    }

    return result;
  }

  /**
   * Set the function to the given value.
   *
   * @return the old function, if any, or null.
   */
  protected final AnalysisFunction defineFunction(String name, AnalysisFunction function) {
    AnalysisFunction result = functions.get(name);
    functions.put(name, function);
    return result;
  }

  protected final AnalysisFunction retrieveFunction(String name) {
    return functions.get(name);
  }


  public final class VariableAndValue {
    private String varName;
    private AnalysisObject value;

    public VariableAndValue(String varName, AnalysisObject value) {
      this.varName = varName;
      this.value = value;
    }

    public String getVarName() {
      return varName;
    }

    public AnalysisObject getValue() {
      return value;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append('$').append(varName).append('=').append(value);

      return result.toString();
    }
  }

  /**
   * Container for "handled" and "result" booleans.
   */
  final class HandledAndResult {
    public final boolean handled;
    public final boolean result;

    HandledAndResult(boolean handled, boolean result) {
      this.handled = handled;
      this.result = result;
    }
  }
}
