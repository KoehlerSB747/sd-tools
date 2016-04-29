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


/**
 * Abstract implementation of an analysis object that manages common
 * functionality across classes.
 * <p>
 * @author Spencer Koehler
 */
public abstract class AbstractAnalysisObject implements AnalysisObject {
  
  protected abstract AnalysisObject doAccess(String ref, EvaluatorEnvironment env);


  protected AbstractAnalysisObject() {
  }

  /** Customization for "show" access. */
  protected String getShowString() {
    return this.toString();
  }

  /** Customization for "help" access. */
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.append("Help (getHelpString) for '").append(this.getClass().getName()).append("' is unimplemented");
    return result.toString();
  }

  @Override
  public final AnalysisObject access(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;

    if ("?".equals(ref) || "help".equals(ref)) {
      result = new BasicAnalysisObject<String>(getHelpString());
    }
    else if ("show".equals(ref)) {
      result = new BasicAnalysisObject<String>(getShowString());
    }
    else {
      result = doAccess(ref, env);
    }

    return result;
  }

  protected final AnalysisObject[] getArgValues(String ref, EvaluatorEnvironment env) {
    AnalysisObject[] result = null;

    // [arg1,arg2,...,argN] -- split on comma, each arg gets evaluated as an expression
    // [{arg1,arg2,...,argN}] -- same, but no arg gets evaluated as an expression

    int start = ref.indexOf('[') + 1;
    if (start > 0) {
      int end = ref.lastIndexOf(']');
      if (end > start) {

        // determine whether args should be evaluated
        boolean evaluate = true;
        if (ref.charAt(start) == '{') {
          ++start;
          evaluate = false;
          if (ref.charAt(end - 1) == '}') --end;
        }
        
        final String argsString = ref.substring(start, end);
        //todo: do smart parsing to separate into args. for now, just split on ","
        final String[] args =argsString.split("\\s*,\\s*");
        result = new AnalysisObject[args.length];
        for (int i = 0; i < args.length; ++i) {
          result[i] = evaluate ? env.evaluateExpression(args[i]) : new BasicAnalysisObject<String>(args[i]);
        }
      }
    }

    return result;
  }
}
