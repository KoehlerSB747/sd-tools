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
package org.sd.nlp.conll;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeUtil;
import org.sd.xml.DataProperties;

/**
 * Utility to iterate over sentences in CoNLL format.
 * <p>
 * See http://ilk.uvt.nl/conll/#dataformat
 *
 * @author Spencer Koehler
 */
public class ConllReader implements Iterator<ConllSentence> {
  
  public enum Field {
  }


  private BufferedReader reader;
  private ConllSentence nextSentence;

  public ConllReader(InputStream inputStream) {
    this(FileUtil.getReader(inputStream, "UTF-8"));
  }

  public ConllReader(BufferedReader reader) {
    this.reader = reader;
    try {
      this.nextSentence = readNextSentence();
    }
    catch (IOException ioe) {
      this.nextSentence = null;
    }
  }

  public boolean hasNext() {
    return nextSentence != null;
  }

  public ConllSentence next() {
    ConllSentence result = nextSentence;
    try {
      nextSentence = readNextSentence();
    }
    catch (IOException ioe) {
      result = null;
    }
    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

  private final ConllSentence readNextSentence() throws IOException {
    ConllSentence result = null;

    String line = null;
    while ((line = reader.readLine()) != null) {
      if ("".equals(line)) break;
      if (result == null) result = new ConllSentence();
      result.addTokenLine(line);
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //   inFile -- (optional, default is stdin if missing or "-") path to input conll file
    //   outFile -- (optional, default is stdout if missing or "-") path to output file
    //   outputType -- (optional, default=tree) {tree, dot}
    final DataProperties dataProperties = new DataProperties(args);
    final String inFile = dataProperties.getString("inFile", "-");
    final String outFile = dataProperties.getString("outFile", "-");
    final String outputType = dataProperties.getString("outputType", "tree");

    final BufferedReader reader = "-".equals(inFile) ? new BufferedReader(new InputStreamReader(System.in)) : FileUtil.getReader(inFile);
    final PrintStream out = "-".equals(outFile) ? System.out : new PrintStream(outFile);

    boolean didOne = false;
    for (Iterator<ConllSentence> iter = new ConllReader(reader); iter.hasNext(); ) {
      final ConllSentence sentence = iter.next();

      if (didOne) out.println();

      switch (outputType) {
        case "tree" :
          final Tree<ConllNodeData> tree = sentence.asTree();
          out.print(TreeUtil.prettyPrint(tree));
          break;
        case "dot" :
          //todo: implement this... write each sentence to a new numbered dot file?
          break;
        default :
          out.println(sentence.toString());
          break;
      }

      didOne = true;
    }
  }
}
