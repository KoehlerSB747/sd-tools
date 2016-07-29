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
  
  public static final int DEFAULT_POINTER_LIMIT = 250;

  private List<Synset> synsets;
  private List<PointerInstance> pointers;
  private Map<String, List<SynsetInfo>> synsetName2Infos;
  private Map<String, String> graphAttributes;
  private Map<String, String> nodeAttributes;
  private Map<String, String> edgeAttributes;
  private int nextSynsetId = 0;

  public WordGraph(List<Synset> synsets, List<PointerInstance> pointers) {
    this(synsets, pointers, DEFAULT_POINTER_LIMIT);
  }

  public WordGraph(List<Synset> synsets, List<PointerInstance> pointers, int pointerLimit) {
    this.synsets = synsets;
    this.pointers = (pointerLimit > 0 && pointers.size() > pointerLimit) ? new ArrayList<PointerInstance>(pointers.subList(0, pointerLimit)) : pointers;
    this.synsetName2Infos = new LinkedHashMap<String, List<SynsetInfo>>();
    this.graphAttributes = new LinkedHashMap<String, String>();
    this.nodeAttributes = new LinkedHashMap<String, String>();
    this.edgeAttributes = new LinkedHashMap<String, String>();

    init();
  }

  private final void init() {
    if (synsets != null) {
      for (Synset synset : synsets) {
        init(synset);
      }
    }

    if (pointers != null) {
      for (PointerInstance pointer : pointers) {
        init(pointer.getSourceSynset());
        init(pointer.getTargetSynset());
      }
    }

    setAttribute("compound", "true");
    setAttribute("rankdir", "LR");
    setAttribute("fontsize", "9");
    setAttribute("node:fontsize", "12");
    setAttribute("edge:fontsize", "11");
  }

  private final void init(Synset synset) {
    if (synset != null) {
      List<SynsetInfo> infos = synsetName2Infos.get(synset.getSynsetName());
      if (infos == null) {
        infos = new ArrayList<SynsetInfo>();
        synsetName2Infos.put(synset.getSynsetName(), infos);
      }
      if (findSynset(infos, synset) == null) {
        final SynsetInfo info = new SynsetInfo(synset, ++nextSynsetId);
        infos.add(info);
      }
    }
  }

  private final SynsetInfo findSynset(List<SynsetInfo> infos, Synset synset) {
    SynsetInfo result = null;

    for (SynsetInfo info : infos) {
      if (info.synset == synset) {
        result = info;
        break;
      }
    }

    return result;
  }

  private final SynsetInfo findInfo(Synset synset) {
    SynsetInfo result = null;

    final List<SynsetInfo> infos = synsetName2Infos.get(synset.getSynsetName());
    if (infos != null) {
      result = findSynset(infos, synset);
    }

    return result;
  }

  public final void setAttribute(String key, String value) {
    // key=value       -- graphAttribute
    // node:key=value  -- nodeAttribute
    // edge:key=value  -- edgeAttribute
    if (key.startsWith("node:")) {
      nodeAttributes.put(key.substring(5), value);
    }
    else if (key.startsWith("edge:")) {
      edgeAttributes.put(key.substring(5), value);
    }
    else {
      graphAttributes.put(key, value);
    }
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

    if (nodeAttributes.size() > 0) {
      result.append("  node [");
      boolean didFirst = false;
      for (Map.Entry<String, String> nodeEntry : nodeAttributes.entrySet()) {
        if (didFirst) result.append(", ");
        result.append(nodeEntry.getKey()).append('=').append(nodeEntry.getValue());
      }
      result.append("];\n");
    }

    if (edgeAttributes.size() > 0) {
      result.append("  edge [");
      boolean didFirst = false;
      for (Map.Entry<String, String> edgeEntry : edgeAttributes.entrySet()) {
        if (didFirst) result.append(", ");
        result.append(edgeEntry.getKey()).append('=').append(edgeEntry.getValue());
      }
      result.append("];\n");
    }

    for (Map.Entry<String, List<SynsetInfo>> infoEntry : synsetName2Infos.entrySet()) {
      final String synsetName = infoEntry.getKey();
      final List<SynsetInfo> infos = infoEntry.getValue();

      for (SynsetInfo info : infos) {
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
    }

    for (PointerInstance pointer : pointers) {
      final SynsetInfo info1 = findInfo(pointer.getSourceSynset());
      final int wordId1 = pointer.hasSourceWord() ? info1.wordName2Id.get(pointer.getSourceWord().getWordName()) : 1;
      final SynsetInfo info2 = findInfo(pointer.getTargetSynset());
      final int wordId2 = info2.wordName2Id.get(pointer.getTargetWord().getWordName());
      result.
        append("  word").append(info1.synsetId).append("_").append(wordId1).
        append(" -> word").append(info2.synsetId).append("_").append(wordId2).
        append(" [label=\"").append(escapedPtrSymbol(pointer)).append("\"");
      if (!pointer.hasSourceWord()) {
        result.append(", ltail=cluster").append(info1.synsetId);
      }
      result.append("];\n");
    }

    result.append("}\n");

    return result;
  }

  private final String escapedPtrSymbol(PointerInstance pointer) {
    return pointer.getPointerDef().getDotNormalizedPointerSymbol();
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

    final LexDictionary dict = new LexDictionary(new LexLoader(new File(args[0])));
    final List<Synset> synsets = dict.lookupSynsets(args[1]);
    final List<PointerInstance> allPointers = dict.getForwardPointers(null, synsets);

    final WordGraph wordGraph = new WordGraph(synsets, allPointers);
    System.out.println(wordGraph.buildGraph(null));
  }
}
