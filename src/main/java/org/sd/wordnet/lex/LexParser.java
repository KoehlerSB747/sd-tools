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


import java.util.ArrayList;
import java.util.List;
import org.sd.wordnet.util.StringDecoder;

/**
 * Lexicographer file line parser.
 * <p>
 * @author Spencer Koehler
 */
public class LexParser {
  
  public static final Synset parseLexString(String lexString) {
    Synset result = null;

    final StringDecoder decoder = new StringDecoder(lexString);
    if (decoder.hasSegmentInfo()) {
      result = new Synset();
      result.setStringDecoder(decoder);

      final StringDecoder.SegmentInfo segmentInfo = decoder.getSegmentInfo();
      if (segmentInfo.getStartChar() == '{' && segmentInfo.hasInnerSegment()) {  // segment has {synset}
        StringBuilder pointerString = null;
        boolean doingFrames = false;

        // { word+ pointer* frame* ( gloss )? }
        for (StringDecoder.SegmentInfo innerSegInfo : segmentInfo.getInnerSegments()) {
          if (pointerString != null) {
            // this case covers when file has a space after "lex_filename: "
            pointerString.append(innerSegInfo.getAllText());
            final PointerDefinition pointer = buildPointerDefinition(pointerString.toString());
            result.addPointerDefinition(pointer);
            pointerString = null;
          }
          else if (innerSegInfo.getStartChar() == '(') {  // segment has (gloss)
            result.setGloss(innerSegInfo.getInnerText());
          }
          else if (doingFrames) {
            final List<Integer> frameNums = getFrameNums(innerSegInfo);
            for (Integer frameNum : frameNums) {
              result.addFrame(frameNum);
            }
            doingFrames = (innerSegInfo.getLastChar() == ',');
          }
          else if (innerSegInfo.getStartChar() == '[') {  // segment has [word/pointer]
            // [ word (marker)? lex_id?, pointer* frame* ]
            final Word word = buildWordWithPointer(innerSegInfo);
            word.setSynset(result);  // backpointer
            result.addWord(word);
          }
          else if (innerSegInfo.getAllText().endsWith(",")) {  // segment has simple word
            // word (marker)? lex_id?,
            final String[] texts = innerSegInfo.getAllText().split(",");
            for (String text : texts) {
              final SimpleWord word = buildSimpleWord(text);
              result.addWord(word);
            }
          }
          else if (innerSegInfo.getAllText().startsWith("frames:")) { // segment has frames
            // frames: f_num, f_num, ..., f_num
            doingFrames = true;
          }
          else {  // segment has pointer
            // word,? lex_filename:? headWord lex_id? ^? satelliteWord? lex_id? , pointer_symbol
            final String text = innerSegInfo.getAllText();
            if (text.charAt(text.length() - 1) == ':') {
              // don't have full pointer, pull in next segment to somplete it
              pointerString = new StringBuilder();
              pointerString.append(text);
            }
            else {
              final PointerDefinition pointer = buildPointerDefinition(text);
              result.addPointerDefinition(pointer);
            }
          }
        }
      }
    }

    return result;
  }

  static final Word buildWordWithPointer(StringDecoder.SegmentInfo segmentInfo) {
    final Word result = new Word();

    // [ word (marker)? lex_id?, pointer* frame* ]

    StringBuilder pointerString = null;
    boolean doingFrames = false;
    for (StringDecoder.SegmentInfo innerSegInfo : segmentInfo.getInnerSegments()) {
      if (pointerString != null) {
        // this case covers when file has a space after "lex_filename: "
        pointerString.append(innerSegInfo.getAllText());
        final PointerDefinition pointer = buildPointerDefinition(pointerString.toString());
        result.addPointerDefinition(pointer);
        pointerString = null;
      }
      else if (doingFrames) {
        final List<Integer> frameNums = getFrameNums(innerSegInfo);
        for (Integer frameNum : frameNums) {
          result.addFrame(frameNum);
        }
        doingFrames = (innerSegInfo.getLastChar() == ',');
      }
      else if (!result.hasSimpleWord() && innerSegInfo.getAllText().endsWith(",")) {  // segment has simple word
        final SimpleWord word = buildSimpleWord(innerSegInfo.getAllText());
        result.setSimpleWord(word);
      }
      else if (innerSegInfo.getAllText().startsWith("frames:")) {  // segment has frames
        doingFrames = true;
      }
      else {  // segment has pointer
        final String text = innerSegInfo.getAllText();
        if (text.charAt(text.length() - 1) == ':') {
          // don't have full pointer, pull in next segment to complete it
          pointerString = new StringBuilder();
          pointerString.append(text);
        }
        else {
          final PointerDefinition pointer = buildPointerDefinition(text);
          result.addPointerDefinition(pointer);
        }
      }
    }

    return result;
  }

  static final SimpleWord buildSimpleWord(String text) {
    return new SimpleWord(text);
  }

  static final PointerDefinition buildPointerDefinition(String text) {
    return new PointerDefinition(text);
  }

  static final List<Integer> getFrameNums(StringDecoder.SegmentInfo segmentInfo) {
    final List<Integer> result = new ArrayList<Integer>();
    final String allText = segmentInfo.getAllText();
    final String[] parts = allText.split(",");

    for (String part : parts) {
      if (!"".equals(part)) {
        try {
          result.add(new Integer(part));
        }
        catch (NumberFormatException nfe) {
          final boolean stopHere = true;
        }
      }
    }

    return result;
  }
}
