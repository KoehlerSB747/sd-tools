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
package org.sd.token;


import java.util.HashMap;
import java.util.Map;

/**
 * Standard strategy for creating/finding/managing breaks in tokenizer text.
 * <p>
 * @author Spencer Koehler
 */
public class StandardBreakMaker {
  
  protected String text;
  private StandardTokenizerOptions options;

  /**
   * Maps text positions to breaks. Any unmapped positions is assumed
   * to be a Break.NO_BREAK.
   */
  private Map<Integer, Break> _pos2break;
  private Object pos2breakLock = new Object();
  private boolean pos2breakInit = false;

  public StandardBreakMaker(String text, StandardTokenizerOptions options) {
    this.text = text;
    this.options = options;
    this._pos2break = null;
  }

  public String getText() {
    return text;
  }

  public StandardTokenizerOptions getOptions() {
    return options;
  }

  protected void setOptions(StandardTokenizerOptions options) {
    this.options = options;
  }

  public boolean initializing() {
    return pos2breakInit;
  }

  public Map<Integer, Break> getPos2Break() {
    synchronized (pos2breakLock) {
      if (this._pos2break == null) {
        this.pos2breakInit = true;
        this._pos2break = createBreaks();
        this.pos2breakInit = false;
      }
    }
    return this._pos2break;
  }

  /**
   * Reset this instance such that breaks will be recomputed.
   */
  protected void reset() {
    this._pos2break = null;
  }

  /**
   * Get the non-null break instance for the text at the given position.
   */
  public Break getBreak(int pos) {
    Break result = null;

    if (pos == text.length()) {
      result = Break.ZERO_WIDTH_HARD_BREAK;
    }
    else {
      final Map<Integer, Break> pos2break = getPos2Break();
      result = pos2break.get(pos);
    }

    return (result == null) ? Break.NO_BREAK : result;
  }

  /**
   * Change the break at the given to position to theBreak.
   * 
   * Note that this method presents a way to change breaks initialized through CreateBreaks
   * and is not to be used by the implementation of CreateBreaks!
   *
   * @return The previous break at the position.
   */
  public Break changeBreak(int pos, Break theBreak) {
    Break result = getBreak(pos);
    final Map<Integer, Break> pos2break = getPos2Break();

    if (theBreak == null || theBreak == Break.NO_BREAK) {
      pos2break.remove(pos);
    }
    else {
      pos2break.put(pos, theBreak);
    }

    return result;
  }

  public int getBreakCount() {
    return getPos2Break().size();
  }

  protected boolean hitsTokenBreakLimit(int startIdx, int breakIdx, int curBreakCount) {
    return options.hitsTokenBreakLimit(curBreakCount);
  }

  /**
   * Default break initialization. Extenders may override.
   */
  protected Map<Integer, Break> createBreaks() {
    final Map<Integer, Break> result = new HashMap<Integer, Break>();

    int increment = 1;
    for (int charPos = 0; charPos < text.length(); charPos += increment) {
      Break curBreak = Break.NO_BREAK;
      increment = 1;  // reset

      final int curChar = text.codePointAt(charPos);
      if (options.isWhitespace(curChar)) {
        curBreak = options.getWhitespaceBreak();
      }
      else if (options.isLetterOrDigit(curChar)) {
        if (charPos > 0 && options.isLetterOrDigit(text.codePointAt(charPos - 1))) {
          // previous char was also a letter or digit
          final int prevChar = text.codePointAt(charPos - 1);

          if (options.isDigit(curChar)) {
            if (options.isDigit(prevChar)) {
              // digit digit
              curBreak = Break.NO_BREAK;
            }
            else if (options.isUpperCase(prevChar)) {
              // upper digit
              curBreak = options.getUpperDigitBreak();
            }
            else {
              // lower digit
              curBreak = options.getLowerDigitBreak();
            }
          }
          else if (options.isUpperCase(curChar)) {
            if (options.isDigit(prevChar)) {
              // digit upper
              curBreak = options.getDigitUpperBreak();
            }
            else if (options.isUpperCase(prevChar)) {
              // upper upper
              curBreak = Break.NO_BREAK;
            }
            else {
              // lower upper
              curBreak = options.getLowerUpperBreak();
            }
          }
          else {  // options.isLower(curChar)
            if (options.isDigit(prevChar)) {
              // digit lower
              curBreak = options.getDigitLowerBreak();
            }
            else if (options.isUpperCase(prevChar)) {
              // upper lower
              curBreak = options.getUpperLowerBreak();
            }
            else {
              // lower lower
              curBreak = Break.NO_BREAK;
            }
          }
        }
        else {
          // first letter or digit is always non-breaking.
          curBreak = Break.NO_BREAK;
        }
      }
      else {
        // char is punctuation or a symbol

        final int nextChar = (charPos + 1 < text.length()) ? text.codePointAt(charPos + 1) : 0;

        if (curChar == '-') {
          if (nextChar == '-') {
            // there is more than one consecutive dash

            // check for 3+ dashes, treat all as hard breaks
            if (charPos + 2 < text.length() && text.codePointAt(charPos + 2) == '-') {
              while (charPos + increment < text.length() && text.codePointAt(charPos + increment) == '-') {
                setBreak(result, charPos + increment, Break.SINGLE_WIDTH_HARD_BREAK);
                ++increment;
              }
              curBreak = Break.SINGLE_WIDTH_HARD_BREAK;
            }
            else {
              // just 2 dashes
              if ((charPos > 0 && options.isLetterOrDigit(text.codePointAt(charPos - 1))) && (charPos + 2 < text.length() && options.isLetterOrDigit(text.codePointAt(charPos + 2)))) {
                // embedded double dash
                curBreak = options.getEmbeddedDoubleDashBreak();
              }
              else {
                // non-embedded double dash
                curBreak = options.getNonEmbeddedDoubleDashBreak();
              }

              // apply the break to the second dash
              final Break secondDashBreak = (curBreak == Break.SINGLE_WIDTH_SOFT_BREAK) ? Break.NO_BREAK : curBreak;
              setBreak(result, charPos + 1, secondDashBreak);

              // skip the second dash
              ++increment;
            }
          }
          else {
            // this is a single dash
            if (charPos > 0 && !options.isWhitespace(text.codePointAt(charPos - 1))) {
              if (nextChar > 0 && !options.isWhitespace(nextChar)) {
                // embedded dash
                curBreak = options.getEmbeddedDashBreak();
              }
              else {
                // left-bordered dash
                curBreak = options.getLeftBorderedDashBreak();
              }
            }
            else if (nextChar > 0 && !options.isWhitespace(nextChar)) {
              // right-bordered dash
              curBreak = options.getRightBorderedDashBreak();
            }
            else {
              // free-standing dash
              curBreak = options.getFreeStandingDashBreak();
            }
          }
        }
        else if (nextChar == curChar) {
          // punctuation/symbol repeats consecutively

          curBreak = options.getRepeatingSymbolBreak();

          // set this break on all consecutive repeats
          while (charPos + increment < text.length() && text.codePointAt(charPos + increment) == curChar) {
            setBreak(result, charPos + increment, curBreak);
            ++increment;
          }
        }
        else if (nextChar > 0 && options.isLetterOrDigit(nextChar)) {
          // symbol immediately precedes a non-white, non-symbol char as part of a token

          if (charPos > 0 && options.isLetterOrDigit(text.codePointAt(charPos - 1)) && curChar != '/' && curChar != '\\') {
            // symbol is embedded between non-white, non-symbol chars e.g. "don't" or "3.14"
            if (isSymbol(curChar)) {
              curBreak = options.getSymbolBreak();
            }
            else {
              if (curChar == '\'') {
                // embedded apostrophe e.g. "don't"
                curBreak = options.getEmbeddedApostropheBreak();
              }
              else {
                //e.g. embedded non-symbol punctuation
                curBreak = options.getEmbeddedPunctuationBreak();
              }
            }
          }
          else if (curChar == '"' || curChar == '(' || curChar == '[' || curChar == '{' || curChar == '<' || curChar == '\'') {
            // symbol is open quote, paren, or slash
            curBreak = options.getQuoteAndParenBreak();
          }
          else if (curChar == '/' || curChar == '\\') {
            curBreak = options.getSlashBreak();
          }
          // else if (isPunctuation(curChar)) {
          //   // calling "non-char + punct + char" (right-bordered punctuation) embedded
          //   curBreak = options.getEmbeddedPunctuationBreak();
          // }
          else {
            // e.g. "$24.99"
            curBreak = options.getSymbolBreak();
          }
        }
        else if (curChar == '%' && charPos > 0 && options.isDigit(text.codePointAt(charPos - 1))) {
          // e.g. "99.9%"
          curBreak = Break.NO_BREAK;
        }
        else if (curChar == '/' || curChar == '\\') {
          curBreak = options.getSlashBreak();
        }
        else if (isPunctuation(curChar)) {
          //todo: apply other heuristics for recognizing a punctuation char as a part of a token

          // calling char + punct + non-char (left-bordered punctuation) embedded

          curBreak = Break.SINGLE_WIDTH_HARD_BREAK;
        }
        else if (isSymbol(curChar)) {
          // keep other symbols like copyright, registered trademark, mathematical symbols, etc.
          curBreak = options.getSymbolBreak();
        }
      }

      // set curBreak
      setBreak(result, charPos, curBreak);
    }
//...
    //note: any non-letter-digit-or-white immediately following a break repeats the break


    return result;
  }


  protected void clearBreaks(Map<Integer, Break> result, int startPos, int endPos) {
    for (int breakIndex = startPos; breakIndex < endPos; ++breakIndex) {
      result.remove(breakIndex);
    }
  }

  protected void setBreak(Map<Integer, Break> result, int pos, boolean goLeft, boolean setHard) {
    if (pos >= text.length()) return;

    final Break curBreak = result.containsKey(pos) ? result.get(pos) : null;
    Break theBreak = setHard ? Break.SINGLE_WIDTH_HARD_BREAK : Break.SINGLE_WIDTH_SOFT_BREAK;

    if (curBreak != null && curBreak.breaks() && curBreak.getBWidth() == 0) {
      theBreak = setHard ? Break.ZERO_WIDTH_HARD_BREAK : Break.ZERO_WIDTH_SOFT_BREAK;
    }
    else if (goLeft) --pos;

    if (pos < 0) return;

    result.put(pos, theBreak);
  }

  protected void setBreak(Map<Integer, Break> pos2break, int pos, Break theBreak) {
    if (theBreak != null && theBreak != Break.NO_BREAK) {
      pos2break.put(pos, theBreak);
    }
    else {
      pos2break.remove(pos);
    }
  }

  /** Auxiliary to findEndBreakForward */
  protected final int doFindEndBreakForward(Map<Integer, Break> pos2break, int startPosition, boolean softOnly) {
    int result = startPosition;
    if (pos2break == null) return result;  // still initializing

    while (result < text.length()) {
      final Break posBreak = pos2break.get(result);

      if (posBreak != null) {
        if (posBreak.breaks()) {
          if (softOnly && posBreak.isHard()) {
            result = -1;
            break;
          }
          else if (posBreak.getBWidth() > 0) {
            result += posBreak.getBWidth();
          }
          else break;
        }
        else break;
      }
      else break;
    }

    if (softOnly && result == text.length()) {
      // end of text is like a hardBreak
      result = -1;
    }

    return result;
  }


  public static final boolean isPunctuation(int codePoint) {
    boolean result = false;

    final int charType = Character.getType(codePoint);
    result = (charType == Character.CONNECTOR_PUNCTUATION ||
              charType == Character.DASH_PUNCTUATION ||
              charType == Character.START_PUNCTUATION ||
              charType == Character.END_PUNCTUATION ||
              charType == Character.INITIAL_QUOTE_PUNCTUATION ||
              charType == Character.FINAL_QUOTE_PUNCTUATION ||
              charType == Character.OTHER_PUNCTUATION);

    return result;
  }


  public static final boolean isSymbol(int codePoint) {
    boolean result = false;

    final int charType = Character.getType(codePoint);
    result = (charType == Character.MATH_SYMBOL ||
              charType == Character.CURRENCY_SYMBOL ||
              charType == Character.MODIFIER_SYMBOL ||
              charType == Character.OTHER_SYMBOL);

    return result;
  }

}
