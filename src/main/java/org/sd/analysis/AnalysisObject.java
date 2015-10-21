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
 * Interface for any analysis object, which essentially is/holds an untyped
 * value. Typed instances of values when referenced from a generic context
 * will exhibit their custom behaviors.
 * <p>
 * @author Spencer Koehler
 */
public interface AnalysisObject {
  
  /** Get a short/summary string representation of this object's data. */
  public String toString();

  /** Access components of this object according to ref. */
  public AnalysisObject access(String ref, EvaluatorEnvironment env);

  /** Get a numeric object representing this instance's value if applicable, or null. */
  public NumericAnalysisObject asNumericAnalysisObject();

}
