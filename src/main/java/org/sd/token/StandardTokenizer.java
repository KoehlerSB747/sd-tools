/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.token;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.InputContext;

/**
 * A standard English-language tokenizer.
 * <p>
 * @author Spence Koehler
 */
public class StandardTokenizer implements Tokenizer {
  
  private InputContext inputContext;
  public InputContext getInputContext() {
    return inputContext;
  }
  public void setInputContext(InputContext inputContext) {
    this.inputContext = inputContext;
  }


  private StandardBreakMaker breakMaker;
  private TokenFeatureAdder tokenFeatureAdder;
  private Tokenizer sourceTokenizer;
  private String text;
  private StandardTokenizerOptions options;
  private boolean computedWordCount;
  private int _wordCount;

  /**
   * Construct with a StandardBreakMaker with the given text and options.
   */
  public StandardTokenizer(String text, StandardTokenizerOptions options) {
    this(new StandardBreakMaker(text, options));
  }

  /**
   * Construct with the given StandardBreakMaker.
   */
  public StandardTokenizer(StandardBreakMaker breakMaker) {
    setBreakMaker(breakMaker);
    this.tokenFeatureAdder = null;
    this.sourceTokenizer = null;
    this.computedWordCount = false;
    this._wordCount = 0;
  }

  public StandardBreakMaker getBreakMaker() {
    return breakMaker;
  }

  public final void setBreakMaker(StandardBreakMaker breakMaker) {
    this.breakMaker = breakMaker;
    if (breakMaker != null) {
      this.text = breakMaker.getText();
      this.options = breakMaker.getOptions();
    }
  }

  public TokenFeatureAdder getTokenFeatureAdder() {
    return tokenFeatureAdder;
  }

  public final void setTokenFeatureAdder(TokenFeatureAdder tokenFeatureAdder) {
    this.tokenFeatureAdder = tokenFeatureAdder;
  }

  public final Tokenizer getSourceTokenizer() {
    return sourceTokenizer == null ? this : sourceTokenizer;
  }

  public final void setSourceTokenizer(Tokenizer sourceTokenizer) {
    this.sourceTokenizer = sourceTokenizer;
  }

  public StandardTokenizerOptions getOptions() {
    return options;
  }

  public final void setOptions(StandardTokenizerOptions options) {
    this.options = options;
    if (breakMaker != null) {
      breakMaker.setOptions(options);
    }
    this.reset();
  }

  /**
   * Reset this instance such that breaks will be recomputed.
   */
  public void reset() {
    if (this.breakMaker != null) {
      this.breakMaker.reset();
    }
    this.computedWordCount = false;
  }


  /**
   * Get the pos2break map, initializing through CreateBreaks if necessary.
   */
  protected Map<Integer, Break> getPos2Break() {
    return breakMaker.getPos2Break();
  }


  public final void addTokenFeatures(Token token) {
    // apply strategy pattern for adding token features
    // (no-op for default StandardTokenizer)
    if (tokenFeatureAdder != null) {
      tokenFeatureAdder.addTokenFeatures(token);
    }
  }

  public Token getToken(int startPosition) {
    startPosition = skipImmediateBreaks(startPosition);
    final Token result = doGetToken(options.getRevisionStrategy(), startPosition, 0);
    addTokenFeatures(result);
    return result;
  }

  public Token getNextSmallestToken(Token token) {
    int startPosition = findEndBreakForward(token.getEndIndex(), false);
    if (startPosition < 0) startPosition = token.getEndIndex();
    final Token result = getSmallestToken(startPosition);
    addTokenFeatures(result);
    return result;
  }

  public Token getSmallestToken(int startPosition) {
    final Token result = doGetNextToken(startPosition, BreakType.SOFT, options.getRevisionStrategy(), 0, 0);
    addTokenFeatures(result);
    return result;
  }

  public String getPostDelim(Token token) {
    String result = null;

    //collect all break delims after the token
    final int startPos = token.getEndIndex();
    final int endPos = findEndBreakForward(startPos, false);

    if (endPos > startPos) {
      result = text.substring(startPos, endPos);
    }

    return result == null ? "" : result;
  }

  public String getPreDelim(Token token) {
    String result = null;

    //collect all break delims before the token
    final int endPos = token.getStartIndex();
    final int startPos = findEndBreakReverse(endPos);

    if (endPos > startPos && endPos > 0) {
      result = text.substring(Math.max(startPos, 0), endPos);
    }

    return result == null ? "" : result;
  }

  /**
   * Determine whether there is a hard break from startPos (incl) to endPos (incl).
   */
  public boolean hasHardBreakBefore(int pos) {
    boolean result = false;

    final int startPos = findEndBreakReverse(pos);
    if (startPos <= 0) {
      // consider beginning of string as a hard break
      result = true;
    }
    else {
      if (pos > startPos && startPos >= 0) {
        for (int curPos = startPos; curPos <= pos; ++curPos) {
          final Break curBreak = breakMaker.getBreak(curPos);
          if (curBreak.isHard()) {
            result = true;
            break;
          }
        }
      }
    }

    return result;
  }

  public boolean followsHardBreak(Token token) {
    return hasHardBreakBefore(token.getStartIndex());
  }

  public Token revise(Token token) {
    if (token == null) return null;

    Token result = null;

    if (token.getRevisionStrategy() != TokenRevisionStrategy.LO && token.getRevisionStrategy() != TokenRevisionStrategy.SO) {
      // revising possible
      int nextRevisionNumber = token.getRevisionNumber() + 1;

      if (token.getRevisionStrategy() == TokenRevisionStrategy.LSL && token.getRevisionNumber() == 0) {
        // get shortest token
        final int[] nextBreak = findNextBreak(token.getStartIndex() + 1, BreakType.SOFT, 0);
        final int endPosition = nextBreak[0];
        if (endPosition < token.getEndIndex()) {
          result = buildToken(token.getStartIndex(), endPosition, token.getRevisionStrategy(), nextRevisionNumber, token.getSequenceNumber(), 1, nextBreak[1]);
        }
      }
      else if (token.getRevisionStrategy() == TokenRevisionStrategy.LS) {
        // get shorter token
        int endPosition = findPriorBreak(token.getEndIndex());
        if (endPosition > token.getStartIndex() && endPosition < token.getEndIndex()) {
          result = buildToken(token.getStartIndex(), endPosition, token.getRevisionStrategy(), nextRevisionNumber, token.getSequenceNumber(), token.getWordCount() - 1, token.getBreakCount() - 1);
        }
      }
      else {
        // get longer token
        final Break curBreak = breakMaker.getBreak(token.getEndIndex());
        int breakCount = token.getBreakCount();
        if (!curBreak.isHard() && !breakMaker.hitsTokenBreakLimit(token.getStartIndex(), token.getEndIndex(), breakCount)) {
          // can go longer as long as we don't revisit the longest when using LSL strategy
          // and as long as there isn't a hard break directly following the soft(s).
          final int[] endBreak = findEndBreakForwardWithBreakCount(token.getEndIndex(), true);
          int endBreakPos = endBreak[0];
          breakCount += endBreak[1];
          if (endBreakPos >= 0) {
            if (curBreak.getBWidth() == 0) ++endBreakPos;
            final int[] nextBreak = findNextBreak(endBreakPos, BreakType.SOFT, breakCount);
            final int endPosition = nextBreak[0];
            if (!(token.getRevisionStrategy() == TokenRevisionStrategy.LSL && findEndBreakForward(endPosition, true) == -1)) {
              result = buildToken(token.getStartIndex(), endPosition, token.getRevisionStrategy(), nextRevisionNumber, token.getSequenceNumber(), token.getWordCount() + 1, nextBreak[1]);
            }
          }
        }
      }
    }

    // make sure revision didn't generate the input token
    if (result != null && result.getEndIndex() == token.getEndIndex()) {
      result = null;
    }

    if (result != null) {
      addTokenFeatures(result);
    }

    return result;
  }

  /**
   * Broaden the token's start position to an already established token with
   * the same end but an earlier start, if possible.
   * <p>
   * NOTE: For the StandardTokenizer, this is not a relevant operation and
   *       null will always be returned.
   */
  public Token broadenStart(Token token) {
    // NOTE: not relevant for the standard tokenizer.
    return null;
  }

  public Token getNextToken(Token token) {
    int startPosition = findEndBreakForward(token.getEndIndex(), false);
    if (startPosition < 0) startPosition = token.getEndIndex();
    final Token result = doGetToken(token.getRevisionStrategy(), startPosition, token.getSequenceNumber() + 1);
    addTokenFeatures(result);
    return result;
  }

  public Token getPriorToken(Token token) {
    Token result = null;

    final int tokenStart = token.getStartIndex();
    int breakCount = 0;

    int[] tokenPos = getPriorSmallestTokenBoundaries(tokenStart);

    if (tokenPos != null) {
      final int endPosition = tokenPos[1];
      int startPosition = tokenPos[0];
      int priorStartPosition = tokenStart;

      final TokenRevisionStrategy revStrategy = token.getRevisionStrategy();

      if (startPosition > 0) {
        // if strategy is for longest *and* we're just after a hard break
        if ((revStrategy == TokenRevisionStrategy.LSL ||
             revStrategy == TokenRevisionStrategy.LO ||
             revStrategy == TokenRevisionStrategy.LS) &&
            hasHardBreakBefore(tokenStart)) {
          // then go back to the prior hard break, counting breaks and enforcing break limits
          do {
            // enforce break limit
            if (options.hitsTokenBreakLimit(breakCount)) break;

            tokenPos = getPriorSmallestTokenBoundaries(startPosition);
            if (tokenPos != null) {
              priorStartPosition = startPosition;
              startPosition = tokenPos[0];
              ++breakCount;
            }
            else break;
          } while (!hasHardBreakBefore(priorStartPosition));
        }
        // otherwise, the first prior break is sufficient
      }

      if (startPosition < endPosition) {
        result = buildToken(startPosition, endPosition, revStrategy, 0, Math.max(token.getSequenceNumber() - 1, 0),
                            computeWordCount(startPosition, endPosition), breakCount);
      }
    }

    if (result != null) {
      addTokenFeatures(result);
    }

    return result;
  }

  public String getText() {
    return text;
  }

  protected void setText(String text) {
    this.text = text;
  }

  public int getWordCount() {
    if (!computedWordCount) {
      _wordCount = computeWordCount(0, text.length());
      computedWordCount = true;
    }
    return _wordCount;
  }

  public String getNextText(Token token) {
    int startPos = findEndBreakForward(token.getEndIndex(), false);
    if (startPos < 0) startPos = token.getEndIndex();
    return (startPos < text.length()) ? text.substring(startPos) : "";
  }

  public String getPriorText(Token token) {
    final int endPos = findEndBreakReverse(token.getStartIndex());
    return (endPos > 0) ? text.substring(0, endPos) : "";
  }

  public Token buildToken(int startPosition, int endPosition) {
    Token result = null;

    if (startPosition >= 0) {
      endPosition = Math.min(endPosition, text.length());
      result = buildToken(startPosition, endPosition, options.getRevisionStrategy(),
                          0, -1, computeWordCount(startPosition, endPosition),
                          computeBreakCount(startPosition, endPosition));
    }

    return result;
  }

  /**
   * Split the text from start to end position into words based on breaks.
   */
  public String[] getWords(int startPosition, int endPosition) {
    final List<String> result = buildWords(startPosition, endPosition);
    return result.toArray(new String[result.size()]);
  }

  /** Determine whether this instance is currently initializing. */
  public boolean initializing() {
    return breakMaker.initializing();
  }

  /** Safely cast this as a StandardTokenizer or get one suitable for limited use. */
  public StandardTokenizer asStandardTokenizer() {
    return this;
  }

  private final List<String> buildWords(int startPosition, int endPosition) {
    final List<String> result = new ArrayList<String>();

    int curStart = skipImmediateBreaks(startPosition);
    int curEnd = curStart + 1;
    boolean hasText = true;

    final Map<Integer, Break> pos2break = getPos2Break();
    if (endPosition > text.length()) endPosition = text.length();
    for (; curEnd < endPosition; ++curEnd) {
      final Break posBreak = pos2break.get(curEnd);
      if (posBreak != null) {
        result.add(text.substring(curStart, curEnd));
        curStart = curEnd = skipImmediateBreaks(curEnd);
        hasText = false;
      }
      else {
        hasText = true;
      }
    }
    if (hasText && curStart < endPosition) {
      result.add(text.substring(curStart, endPosition));
    }

    return result;
  }

  public final int computeWordCount(Token startToken, Token endToken) {
    return computeWordCount(startToken.getStartIndex(), endToken.getEndIndex());
  }

  private final int computeWordCount(int startIndex, int endIndex) {
    int result = 0;

    int priorBreakPos = startIndex;
    int breakPos = findNextBreak(startIndex, BreakType.SOFT, -1)[0];
    while (breakPos <= endIndex) {
      if (breakPos > priorBreakPos) ++result;

      if (breakPos >= endIndex) break;

      breakPos = findEndBreakForward(breakPos, false);
      priorBreakPos = breakPos;
      breakPos = findNextBreak(breakPos + 1, BreakType.SOFT, -1)[0];
    }

    return result;
  }

  private final int computeBreakCount(Token startToken, Token endToken) {
    return computeBreakCount(startToken.getStartIndex(), endToken.getEndIndex());
  }

  private final int computeBreakCount(int startIndex, int endIndex) {
    int result = 0;

    final Map<Integer, Break> pos2break = getPos2Break();
    if (endIndex > text.length()) endIndex = text.length();
    for (int pos = startIndex; pos < endIndex; ++pos) {
      final Break posBreak = pos2break.get(pos);
      if (posBreak != null) {
        ++result;
        if (posBreak.getBWidth() > 1) {
          pos += posBreak.getBWidth() - 1;
        }
      }
    }

    return result;
  }

  private final Token doGetToken(TokenRevisionStrategy revisionStrategy, int startPosition, int sequenceNumber) {
    final BreakType breakToFind = getBreakToFind(revisionStrategy);
    return doGetNextToken(startPosition, breakToFind, revisionStrategy, 0, sequenceNumber);
  }

  private final BreakType getBreakToFind(TokenRevisionStrategy revisionStrategy) {
    BreakType breakToFind = BreakType.HARD;
    switch (revisionStrategy) {
      case LSL:
      case LO:
      case LS:
        breakToFind = BreakType.HARD;
        break;
      case SO:
      case SL:
        breakToFind = BreakType.SOFT;
        break;
    }
    return breakToFind;
  }

  private final Token doGetNextToken(int startPosition, BreakType breakToFind, TokenRevisionStrategy revisionStrategy, int revisionNumber, int sequenceNumber) {
    Token result = null;

    final int[] nextBreak = findNextBreak(startPosition + 1, breakToFind, 0);
    int endPosition = nextBreak[0];
    final int breakCount = nextBreak[1];

    // verify that if breakToFind is hard, we back up over any immediate soft breaks
    if (breakToFind == BreakType.HARD) {
      endPosition = findEndBreakReverse(endPosition);
    }

    result = buildToken(startPosition, endPosition, revisionStrategy, revisionNumber, sequenceNumber, computeWordCount(startPosition, endPosition), breakCount);

    return result;
  }

  private final Token buildToken(int startPosition, int endPosition, TokenRevisionStrategy revisionStrategy, int revisionNumber, int sequenceNumber, int wordCount, int breakCount) {
    Token result = null;

    if (endPosition > startPosition) {
      final String tokenText = text.substring(startPosition, endPosition);
      result = new Token(getSourceTokenizer(), tokenText, startPosition, revisionStrategy, revisionNumber, sequenceNumber, wordCount, breakCount);
    }

    return result;
  }

  /**
   * Find the next break position at or after startPosition that agrees with
   * the given breakToFind.
   *
   * @return {nextBreakPos, totalBreakCount}
   */
  private final int[] findNextBreak(int startPosition, BreakType breakTypeToFind, int breakCount) {
    int result = text.length();

    final boolean enforceBreakLimit = (breakCount >= 0);
    if (!enforceBreakLimit) {
      breakCount = 0;
    }

    final Map<Integer, Break> pos2break = getPos2Break();
    int priorBreakPos = -1;
    for (int pos = startPosition; pos < text.length(); ++pos) {
      final Break posBreak = pos2break.get(pos);
      if (posBreak != null) {
        if (pos > priorBreakPos + 1) {
          // only count non-consecutive breaks toward breakLimit
          ++breakCount;
        }
        if (posBreak.agreesWith(breakTypeToFind) || (enforceBreakLimit && breakMaker.hitsTokenBreakLimit(startPosition, pos, breakCount))) {
          result = pos;
          break;
        }
        priorBreakPos = pos;
      }
    }

    return new int[]{result, breakCount};
  }

  private final int findPriorBreak(int endPosition) {
    int result = -1;

    final Map<Integer, Break> pos2break = getPos2Break();
    for (int pos = endPosition - 1; pos >= 0; --pos) {
      final Break posBreak = pos2break.get(pos);
      if (posBreak != null) {
        if (posBreak.breaks()) {
          result = findEndBreakReverse(pos);
          break;
        }
      }
    }

    return result;
  }

  /**
   * Find the first non-break position from startPosition (inclusive).
   */
  private final int skipImmediateBreaks(int startPosition) {
    int result = startPosition;
    final Map<Integer, Break> pos2break = getPos2Break();

    while (result < text.length()) {
      final Break posBreak = pos2break.get(result);

      if (posBreak != null && posBreak.breaks() && posBreak.getBWidth() > 0) {
        result += posBreak.getBWidth();
      }
      else break;
    }
    
    return result;
  }

  /**
   * Find the position after all breaks starting at startPosition.
   * 
   * If softOnly and a Hard Break is found, then return -1 to
   * indicate that the sequence of breaks should be considered as
   * hard.
   */
  protected final int findEndBreakForward(int startPosition, boolean softOnly) {
    final Map<Integer, Break> pos2break = getPos2Break();
    return breakMaker.doFindEndBreakForward(pos2break, startPosition, softOnly);
  }

  private final int[] findEndBreakForwardWithBreakCount(int startPosition, boolean softOnly) {
    int result = startPosition;
    final Map<Integer, Break> pos2break = getPos2Break();
    if (pos2break == null) return new int[]{result, 0};  // still initializing

    int breakCount = 0;

    while (result < text.length()) {
      final Break posBreak = pos2break.get(result);

      if (posBreak != null) {
        if (posBreak.breaks()) {
          ++breakCount;
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


    return new int[]{result, breakCount};
  }


  /**
   * Find the position of the start of breaks ending before startPosition.
   */
  private final int findEndBreakReverse(int startPosition) {
    int result = startPosition;
    final Map<Integer, Break> pos2break = getPos2Break();
    if (pos2break == null) return result;  // still initializing

    if (!pos2break.containsKey(startPosition)) --result;

    while (result >= 0) {
      final Break posBreak = pos2break.get(result);
      if (posBreak != null) {
        if (posBreak.breaks() && posBreak.getBWidth() == 0) {
          // time to stop
          break;
        }
        else {
          if (posBreak.breaks() && posBreak.getBWidth() > 0) {
            --result;
          }
          else break;
        }
      }
      else {
        // no longer at a breaking character. move forward to the break.
        if (result < startPosition)	++result;
        break;
      }
    }

    return result;
  }

  /**
   * Find the boundaries of the smallest token preceding tokenStart.
   *
   * @return {priorStart, priorEnd} or null.
   */
  private final int[] getPriorSmallestTokenBoundaries(int tokenStart) {
    int[] result = null;

    final int endPosition = tokenStart > 0 ? findEndBreakReverse(tokenStart) : 0;
    if (endPosition > 0) {
      final Map<Integer, Break> pos2break = getPos2Break();
      int startPosition = endPosition;
      while (startPosition > 0 && !pos2break.containsKey(startPosition - 1)) --startPosition;

      if (startPosition >= 0 && startPosition < endPosition) {
        result = new int[]{startPosition, endPosition};
      }
    }

    return result;
  }
}
