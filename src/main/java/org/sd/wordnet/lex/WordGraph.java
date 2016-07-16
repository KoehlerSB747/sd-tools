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
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.DotWriter;

/**
 * Utility to make a graphviz graph from pointer instances.
 * <p>
 * @author Spencer Koehler
 */
public class WordGraph implements DotWriter {
  
  private List<PointerInstance> pointers;
  private Map<String, SynsetInfo> synsetName2Info;
  private Map<String, String> graphAttributes;
  private int nextSynsetId = 0;

  public WordGraph(List<PointerInstance> pointers) {
    this.pointers = pointers;
    this.synsetName2Info = new LinkedHashMap<String, SynsetInfo>();
    this.graphAttributes = new LinkedHashMap<String, String>();

    init();
  }

  private final void init() {
    for (PointerInstance pointer : pointers) {
      init(pointer.getSourceSynset());
      init(pointer.getTargetSynset());
    }

    this.graphAttributes.put("compound", "true");
    this.graphAttributes.put("rankdir", "LR");
    this.graphAttributes.put("fontsize", "8");
  }

  private final void init(Synset synset) {
    if (synset != null) {
      SynsetInfo info = synsetName2Info.get(synset.getSynsetName());
      if (info == null) {
        info = new SynsetInfo(synset, ++nextSynsetId);
        synsetName2Info.put(synset.getSynsetName(), info);
      }
    }
  }

  public final void setNodeAttribute(String nodeAttributeKey, String nodeAttributeValue) {
    graphAttributes.put(nodeAttributeKey, nodeAttributeValue);
  }

  public void writeDot(Writer writer) throws IOException {
    final StringBuilder graphData = buildGraph(null);
    writer.write(graphData.toString());
    writer.flush();
  }

  public StringBuilder buildGraph(StringBuilder result) {
    if (result == null) result = new StringBuilder();

    result.
      append("digraph G {\n");

    for (Map.Entry<String, String> attributeEntry : graphAttributes.entrySet()) {
      result.
        append("  ").
        append(attributeEntry.getKey()).
        append('=').
        append(attributeEntry.getValue()).
        append(";\n");
    }

    for (Map.Entry<String, SynsetInfo> infoEntry : synsetName2Info.entrySet()) {
      final String synsetName = infoEntry.getKey();
      final SynsetInfo info = infoEntry.getValue();

      result.
        append("  subgraph cluster").append(info.synsetId).append(" {\n").
        append("    label=\"").append(synsetName).append("\";\n");
      
      for (Map.Entry<String, Integer> wordEntry : info.wordName2Id.entrySet()) {
        final String wordName = wordEntry.getKey();
        final Integer wordId = wordEntry.getValue();

        result.
          append("    word").
          append(info.synsetId).
          append('_').
          append(wordId).
          append(" [label=\"").
          append(wordName).
          append("\"];\n");
      }

      result.append("  }\n");
    }

    for (PointerInstance pointer : pointers) {
      final SynsetInfo info1 = synsetName2Info.get(pointer.getSourceSynset().getSynsetName());
      final int wordId1 = pointer.hasSourceWord() ? info1.wordName2Id.get(pointer.getSourceWord().getWordName()) : 1;
      final SynsetInfo info2 = synsetName2Info.get(pointer.getTargetSynset().getSynsetName());
      final int wordId2 = info2.wordName2Id.get(pointer.getTargetWord().getWordName());
      result.
        append("  word").append(info1.synsetId).append("_").append(wordId1).
        append(" -> word").append(info2.synsetId).append("_").append(wordId2).
        append(" [label=\"").append(pointer.getPointerDef().getPointerSymbol()).append("\"");
      if (!pointer.hasSourceWord()) {
        result.append(", ltail=cluster").append(info1.synsetId);
      }
      result.append("];\n");
    }

    result.append("}\n");

    return result;
  }


  private static final class SynsetInfo {

    public final Synset synset;
    public final int synsetId;
    public final Map<String, Integer> wordName2Id;

    private int nextWordId = 0;

    SynsetInfo(Synset synset, int synsetId) {
      this.synset = synset;
      this.synsetId = synsetId;
      this.wordName2Id = new LinkedHashMap<String, Integer>();
      init();
    }

    private final void init() {
      if (synset.hasWords()) {
        for (Word word : synset.getWords()) {
          wordName2Id.put(word.getWordName(), ++nextWordId);
        }
      }
    }
  }


  public static void main(String[] args) throws IOException {
    // arg0: dbFileDir
    // arg1: normalizedWord

    final LexDictionary dict = new LexDictionary(new LexLoader(new File(args[0])), true, true, true);
    final List<Synset> synsets = dict.lookupSynsets(args[1]);
    if (synsets != null) {
      final List<PointerInstance> allPointers = new ArrayList<PointerInstance>();
      for (Synset synset : synsets) {
        final List<PointerInstance> pointers = dict.getAllPointers(synset);
        allPointers.addAll(pointers);
      }
      if (allPointers.size() > 0) {
        final WordGraph wordGraph = new WordGraph(allPointers);
        System.out.println(wordGraph.buildGraph(null));
      }
    }
  }
}
