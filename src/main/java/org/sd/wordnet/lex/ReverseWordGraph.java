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
package org.sd.wordnet.lex;


import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 
 * <p>
 * @author Spencer Koehler
 */
public class ReverseWordGraph {
  

  public static void main(String[] args) throws IOException {
    // arg0: dbFileDir
    // arg1: wordName
    // arg2: maxDist
    // arg3: symbolConstraint

    final File dbFileDir = new File(args[0]);
    final String wordName = args[1];
    final int maxDist = (args.length > 2 ? Integer.parseInt(args[2]) : -1);
    final String symbolConstraint = (args.length > 3 ? args[3] : null);

    final LexDictionary dict = new LexDictionary(new LexLoader(dbFileDir));
    final List<PointerInstance> allPointers = dict.getReversePointers(null, wordName, null, maxDist, symbolConstraint);

    final WordGraph wordGraph = new WordGraph(null, allPointers);
    System.out.println(wordGraph.buildGraph(null));
  }
}
