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
package org.sd.wordnet.token;


/**
 * Interface for looking up a word.
 * <p>
 * @author Spencer Koehler
 */
public interface WordLookupStrategy {
  
  /**
   * Create a WordNetToken by looking it up for the given input/norm.
   * <p>
   * @param input  The input to look up
   * @param norm  The normalized input to look up
   *
   * @return a WordNetToken, with or without synsets and/or tags.
   */
  public WordNetToken lookup(String input, String norm);
}
