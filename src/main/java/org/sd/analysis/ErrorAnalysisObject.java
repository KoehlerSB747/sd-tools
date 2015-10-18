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


import org.sd.io.FileUtil;

/**
 * An AnalysisObject that holds an error result.
 * <p>
 * @author Spencer Koehler
 */
public class ErrorAnalysisObject implements AnalysisObject {

  private String message;
  private Exception e;

  public ErrorAnalysisObject(String message) {
    this(message, null);
  }

  public ErrorAnalysisObject(String message, Exception e) {
    this.message = message;
    this.e = e;
  }

  public String getMessage() {
    return message;
  }

  public Exception getException() {
    return e;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("ERROR: ").
      append(message);

    if (e != null) {
      result.
        append(": ").
        append(e.toString());
    }

    return result.toString();
  }

  /** Get a detailed string representation of this object's data. */
  @Override
  public String getDetailedString() {
    return toString();  //todo showStackTrace?
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"stacktrace" -- show the exception's stack trace.</li>
   * </ul>
   */
  @Override
  public AnalysisObject access(String ref) {
    AnalysisObject result = null;

    if (e != null) {
      if ("stacktrace".equals(ref)) {
        result = new BasicAnalysisObject<String>(FileUtil.getStackTrace(e));
      }
    }

    return result;
  }

  /** Get a numeric object representing this instance's value if applicable, or null. */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    return null;
  }
}
