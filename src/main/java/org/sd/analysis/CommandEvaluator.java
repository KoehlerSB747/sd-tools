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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.io.FileUtil;
import org.sd.util.ReflectUtil;
import org.sd.xml.DataProperties;

/**
 * General utility for evaluating commands.
 * <p>
 * @author Spencer Koehler
 */
public class CommandEvaluator {
  
  private EvaluatorEnvironment env;

  public CommandEvaluator(EvaluatorEnvironment env) {
    this.env = env;
  }

  public EvaluatorEnvironment getEnv() {
    return env;
  }

  public DataProperties getDataProperties() {
    DataProperties result = null;

    if (env != null) {
      result = env.getDataProperties();
    }

    return result;
  }

  public void reset() {
    if (env != null) env.reset(true, true);
  }

  public void close() {
    if (env != null) env.close();
  }

  /**
   * Evaluate lines from the environment.
   *
   * @return the number of lines evaluated.
   */
  public int evaluate() throws IOException {
    if (env == null) return 0;

    String line = null;
    int lineNum = 0;
    boolean ready = false;

    if (ready = env.prepareForInput(lineNum)) {
      while ((line = env.readLine()) != null) {
        if (!env.handleInputLine(lineNum, line)) {
          ready = env.prepareForExit(lineNum, line);
          break;
        }
        if (!(ready = env.prepareForInput(++lineNum))) {
          break;
        }
      }
    }

    env.wrapUp(lineNum, ready);

    return lineNum;
  }

  public EvaluationWrapper evaluate(String[] commands) {
    return evaluate(commands, null, null);
  }

  public EvaluationWrapper evaluate(String[] commands, PrintStream out, PrintStream err) {
    final EvaluationWrapper result = new EvaluationWrapper(this, commands);
    result.setStreams(out, err);
    result.run();
    return result;
  }


  public static class EvaluationWrapper implements Runnable {
    private CommandEvaluator commandEvaluator;
    private String[] commands;
    private AtomicBoolean done;
    private Exception exception;
    private String[] outStrings;
    private String[] errStrings;

    private PrintStream outOverride;
    private PrintStream errOverride;

    public EvaluationWrapper(CommandEvaluator commandEvaluator, String[] commands) {
      this.commandEvaluator = commandEvaluator;
      this.commands = commands;
      this.done = new AtomicBoolean(false);
      this.outStrings = null;
      this.errStrings = null;
    }

    public void setStreams(PrintStream out, PrintStream err) {
      this.outOverride = out;
      this.errOverride = err;
    }

    /**
     * NOTE: EvaluatorEnvironment execution is *NOT* threadsafe for evaluating
     *       multiple command sets in different threads; still, this structure
     *       facilitates running a command set asynchronously.
     */
    public void run() {
      this.done.set(false);
      try {
        evaluateCommands();
      }
      catch (Exception e) {
        this.exception = e;
      }
      this.done.set(true);
    }

    public boolean isDone() {
      return done.get();
    }

    public boolean hasError() {
      return exception != null;
    }

    public Exception getError() {
      return exception;
    }
    public String[] getOutStrings() {
      return outStrings;
    }

    public String[] getErrStrings() {
      return errStrings;
    }

    private final void evaluateCommands() throws Exception {
      final ByteArrayInputStream cmdIn = new ByteArrayInputStream(buildInputBytes(commands));
      final BufferedReader reader = FileUtil.getReader(cmdIn);
      final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
      final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
      final PrintStream out = new PrintStream(outBytes);
      final PrintStream err = new PrintStream(errBytes);

      commandEvaluator.getEnv().setReader(reader);
      commandEvaluator.getEnv().setOut(out);
      commandEvaluator.getEnv().setErr(err);
      commandEvaluator.evaluate();
      commandEvaluator.close();
      cmdIn.close();
      reader.close();
      out.close();
      err.close();
    
      this.outStrings = getStrings(outBytes, outOverride);
      this.errStrings = getStrings(errBytes, errOverride);
    }

    private final String[] getStrings(ByteArrayOutputStream byteStream, PrintStream echo) throws UnsupportedEncodingException {
      final String string = byteStream.toString("UTF-8").trim();
      final String[] result = "".equals(string) ? new String[]{} : string.split("\\s*\\n\\s*");

      if (echo != null) {
        for (String line : result) {
          echo.println(line);
        }
      }

      return result;
    }

    private final byte[] buildInputBytes(String[] lines) throws UnsupportedEncodingException {
      return lines2string(lines).getBytes("UTF-8");
    }

    private final String lines2string(String[] lines) {
      final StringBuilder builder = new StringBuilder();
      for (String line : lines) {
        if (builder.length() > 0) builder.append('\n');
        builder.append(line);
      }
      return builder.toString();
    }
  }


  public static CommandEvaluator buildInstance(String[] args) throws ClassNotFoundException {
    // Properties
    //  - env -- (default=org.sd.analysis.BaseEvaluatorEnvironment) EvaluatorEnvironment classpath
    //  - commandFile -- (optional) file of non-interactive commands
    //  - dir -- (optional) establishes working directory
    return buildInstance(new DataProperties(args));
  }

  public static CommandEvaluator buildInstance(DataProperties dataProperties) throws ClassNotFoundException {
    final String envClassName = dataProperties.getString("env", "org.sd.analysis.BaseEvaluatorEnvironment");
    final EvaluatorEnvironment env = (EvaluatorEnvironment)ReflectUtil.constructInstance(Class.forName(envClassName), dataProperties);
    final CommandEvaluator cmd = new CommandEvaluator(env);
    return cmd;
  }

  public static void main(String[] args) throws Exception {
    final CommandEvaluator cmd = buildInstance(args);

    final String[] remainingArgs = cmd.getDataProperties().getRemainingArgs();
    if (remainingArgs == null || remainingArgs.length == 0) {
      // enter interactive mode or run commandFile commands
      cmd.evaluate();
    }
    else {
      // ignore comamndFile commands and evaluate the command-line args as commands
      cmd.evaluate(remainingArgs, System.out, System.err);
    }

    cmd.close();
  }
}
