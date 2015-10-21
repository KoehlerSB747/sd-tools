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
import java.io.IOException;
import java.io.PrintStream;
import org.sd.xml.DataProperties;

/**
 * Interface for an environment for evaluating commands in the CommandEvaluator.
 * <p>
 * NOTE: Generic type, T, reflects the type of the core environment value.
 *
 * @author Spencer Koehler
 */
public interface EvaluatorEnvironment {
  
  /**
   * Get this instance's properties.
   */
  public DataProperties getDataProperties();

  /**
   * Reset this environment's variables and/or functions to the initial state.
   */
  public void reset(boolean variables, boolean functions);

  /**
   * Read the next line from the environment.
   *
   * @return the next line to be read or null if input has been exhausted or
   *         the environment has been closed.
   */
  public String readLine() throws IOException;

  /**
   * Set the output stream for this instance, returning the existing.
   */
  public PrintStream setOut(PrintStream out);

  /**
   * Set the error output stream for this instance, returning the existing.
   */
  public PrintStream setErr(PrintStream err);

  /**
   * Set the reader for this instance, returning the existing reader.
   */
  public BufferedReader setReader(BufferedReader reader);

  /**
   * Close this environment and any open resources.
   */
  public void close();

  /**
   * Prepare for the next input line.
   * 
   * @param lineNum  the number (0-based) of the next input line.
   *
   * @return true to proceed to handling; false to exit the evaluation loop
   *         (which will trigger a subsequent call to prepareForExit(false)).
   */
  public boolean prepareForInput(int lineNum);

  /**
   * Handled the current line.
   *
   * @param lineNum  the number of the line to be handled.
   * @param line  the line to be handled.
   *
   * @return true to continue processing lines; false to exit (which will
   *              trigger a subsequent call to prepareForExit).
   */
  public boolean handleInputLine(int lineNum, String line);

  /**
   * Prepare for exit after an input line's handling returned false.
   *
   * @param lineNum  the number of the line not handled.
   * @param line  the line not handled.
   *
   * @return a handled value suitable for wrapUp.
   */
  public boolean prepareForExit(int lineNum, String line);

  /**
   * Wrap up after all input lines have been processed.
   * 
   * @param lineNum  the last prepped line
   * @param handled  whether the last line was successfully prepped, handled,
   *                 and, if applicable prepped for exit.
   *
   * @return the final enviornment value.
   */
  public void wrapUp(int lineNum, boolean handled);

  /**
   * Evaluate the given expression in this environment.
   * <p>
   * If the expr is an uncrecognized single token, then the token
   * will be returned wrapped in a BasicAnalysisObject<String>.
   * <p>
   * Unrecognized multi-token expressions will yield a null result.
   */
  public AnalysisObject evaluateExpression(String expr);
}
