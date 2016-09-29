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
package org.sd.atnexec;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.atn.AtnState;
import org.sd.atn.BaseClassifierTest;
import org.sd.atn.ParseInterpretationUtil;
import org.sd.atn.ResourceManager;
import org.sd.token.Token;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.Synset;
import org.sd.wordnet.lex.Word;
import org.sd.wordnet.token.WordNetTokenizer;
import org.sd.xml.DomNode;

/**
 * A BaseClassifierTest that checks the current token's verb synsets
 * for specific frames.
 * <p>
 * Attributes:
 * <ul>
 * <li>"wn-dict" -- identifies the resource name of the word net (lex) dictionary</li>
 * <li>"frames" -- identifies (comma-delimited) set of valid frames.
 * </ul>
 *
 * @author Spencer Koehler
 */
public class VerbFrameCheck extends BaseClassifierTest {
  
  private String framesString;
  private Set<Integer> frames;
  private LexDictionary lexDictionary;
  private boolean verbose;

  public VerbFrameCheck(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.verbose = testNode.getAttributeBoolean("verbose", false);
    this.lexDictionary = (LexDictionary)resourceManager.getResource(testNode.getAttributeValue("wn-dict"));

    this.framesString = testNode.getAttributeValue("frames");
    final String[] frameStrings = framesString.split("\\s*,\\s*");

    this.frames = new HashSet<Integer>();
    for (String frameString : frameStrings) {
      frames.add(new Integer(frameString));
    }
  }
  

  @Override
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    final StringBuilder message = (verbose ? new StringBuilder() : null);

    // accept if the token has a "verb" synset that has a matching frame
    final String synsetNamesString = WordNetTokenizer.getSynsets(token);
    if (synsetNamesString != null) {
      final String[] synsetNames = synsetNamesString.split("\\s*,\\s*");
      for (String synsetName : synsetNames) {
        if (synsetName.startsWith("verb")) {
          final List<Word> words = lexDictionary.findWords(synsetName, null);
          if (words != null) {
            for (Word word : words) {
              if (word.hasFrames()) {
                for (Integer frame : word.getFrames()) {
                  if (this.frames.contains(frame)) {
                    result = true;

                    // preserve matching verb frames
                    // NOTE: other rule steps may match other frames and be added to the
                    //       token, but the one that applies to *this* match will be
                    //       associated by this state's ruleStep's label and will enable
                    //       narrowing the possible synsets.
                    ParseInterpretationUtil.setTokenFeature(token, curState, "frames", framesString, this);

                    if (message != null) {
                      message.
                        append(" word[").
                        append(word.getQualifiedWordName()).
                        append("].frame[").
                        append(frame).
                        append("] WORD-FRAME matches");
                    }

                    break;
                  }
                }
              }
              if (result) break;

              if (!result) {
                final Synset synset = word.getSynset();
                if (synset != null && synset.hasFrames()) {
                  for (Integer frame : synset.getFrames()) {
                    if (this.frames.contains(frame)) {
                      result = true;

                      // preserve matching verb frames
                      ParseInterpretationUtil.setTokenFeature(token, curState, "frames", framesString, this);

                      if (message != null) {
                        message.
                          append(" word[").
                          append(word.getQualifiedWordName()).
                          append("].frame[").
                          append(frame).
                          append("] SYNSET-FRAME matches");
                      }

                      break;
                    }
                  }
                }
              }
            }
            if (result) break;
          }
          else {
            if (message != null) {
              message.append(" NO WORDS for ").append(synsetName);
            }
          }
        }
      }
    }
    else {
      if (message != null) {
        message.append(" NO SYNSETS for ").append(token.toString());
      }
    }

    if (message != null) {
      message.append(" result=").append(result);
      System.out.println("\tVerbFrameCheck[" + this.framesString + "]: " + message);
    }

    return result;
  }
}
