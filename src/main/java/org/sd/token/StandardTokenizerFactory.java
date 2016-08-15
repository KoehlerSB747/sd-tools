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


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sd.util.tree.Tree;

/**
 * A factory for StandardTokenizer instances and tokens.
 * <p>
 * @author Spence Koehler
 */
public class StandardTokenizerFactory {
  
  /**
   * Default StandardTokenizerOptions.
   */
  public static final StandardTokenizerOptions DEFAULT_OPTIONS = new StandardTokenizerOptions();

  /**
   * Get a StandardTokenizer using the Default StandardTokenizerOptions.
   */
  public static StandardTokenizer getTokenizer(String text) {
    return getTokenizer(text, DEFAULT_OPTIONS);
  }

  /**
   * Get a StandardTokenizer for the given text using the given options.
   */
  public static StandardTokenizer getTokenizer(String text, StandardTokenizerOptions options) {
    return new StandardTokenizer(text, (options == null) ? DEFAULT_OPTIONS : options);
  }

  /**
   * Get the first token in the text using Default StandardTokenizerOptions.
   * 
   * Note that this token will contain the newly built tokenizer instance for
   * further tokenization of the text.
   */
  public static Token getFirstToken(String text) {
    return getFirstToken(text, DEFAULT_OPTIONS);
  }

  /**
   * Get the first token in the text using the given options.
   * 
   * Note that this token will contain the newly built tokenizer instance for
   * further tokenization of the text.
   */
  public static Token getFirstToken(String text, StandardTokenizerOptions options) {
    StandardTokenizer tokenizer = getTokenizer(text, (options == null) ? DEFAULT_OPTIONS : options);
    return tokenizer.getToken(0);
  }

  /**
   * Get the primary token strings from the text using Default StandardTokenizerOptions.
   */
  public static String[] tokenize(String text) {
    return tokenize(text, DEFAULT_OPTIONS);
  }

  /**
   * Get the primary token strings from the text using the given options.
   */
  public static String[] tokenize(String text, StandardTokenizerOptions options) {
    final List<String> result = new ArrayList<String>();
    if (options == null) options = DEFAULT_OPTIONS;

    for (Token token = getFirstToken(text, options); token != null; token = token.getNextToken()) {
      result.add(token.getText());
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Create a tree of tokens where the root contains the full input text (as
   * a token), the second level contains primary tokenizations (according to
   * Default StandardTokenizerOptions,) and the third level contains the
   * token revisions for each of their (parent) primary tokenizations.
   */
  public static Tree<Token> fullTokenization(String text) {
    return fullTokenization(text, DEFAULT_OPTIONS);
  }

  /**
   * Create a tree of tokens where the root contains the full input text (as
   * a token), the second level contains primary tokenizations (according to
   * the given options,) and the third level contains the token revisions for
   * each of their (parent) primary tokenizations.
   */
  public static Tree<Token> fullTokenization(String text, StandardTokenizerOptions options) {
    final StandardTokenizer tokenizer = getTokenizer(text, (options == null) ? DEFAULT_OPTIONS : options);
    return fullTokenization(tokenizer);
  }

  /**
   * Create a tree of tokens where the root contains the full input text (as
   * a token), the second level contains primary tokenizations (according to
   * the given options,) and the third level contains the token revisions for
   * each of their (parent) primary tokenizations.
   */
  public static Tree<Token> fullTokenization(StandardTokenizer tokenizer) {
    final Tree<Token> result = new Tree<Token>(new Token(tokenizer, tokenizer.getText(), 0, tokenizer.getOptions().getRevisionStrategy(), 0, 0, tokenizer.getWordCount(), -1));

    doAddFullTokenizationAndNext(result, tokenizer, tokenizer.getToken(0));

    return result;
  }

  private static final void doAddFullTokenizationAndNext(Tree<Token> result, StandardTokenizer tokenizer, Token curToken) {
    if (curToken == null) return;

    final Tree<Token> curTokenNode = result.addChild(curToken);

    // add all revisions as children to curTokenNode
    for (Token revisedToken = tokenizer.revise(curToken); revisedToken != null; revisedToken = tokenizer.revise(revisedToken)) {
      doAddFullTokenizationAndNext(curTokenNode, tokenizer, revisedToken);
    }

    // and add next node to result
    final Token nextToken = tokenizer.getNextToken(curToken);
    if (nextToken != null) {
      doAddFullTokenizationAndNext(result, tokenizer, nextToken);
    }
  }

  public static Tree<Token> showTokenization(PrintStream out, String text, StandardTokenizerOptions options) {
    final Tree<Token> result = fullTokenization(text, (options ==  null) ? DEFAULT_OPTIONS : options);
    showTokenization(out, result);
    return result;
  }

  public static Tree<Token> showTokenization(PrintStream out, StandardTokenizer tokenizer) {
    final Tree<Token> result = fullTokenization(tokenizer);
    showTokenization(out, result);
    return result;
  }

  public static void showTokenization(PrintStream out, Tree<Token> tokens) {
    if (out != null) {
      for (Iterator<Tree<Token>> iter = tokens.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
        final Tree<Token> curNode = iter.next();
        for (int indentPos = 0; indentPos < curNode.depth(); ++indentPos) {
          out.print("  ");
        }
        out.println(curNode.getData().toString());
      }
    }
  }

  /**
   * Create a tree of token strings where the root contains the full input
   * text, the second level contains primary tokenizations (according to
   * the default options,) and the third level contains the token revision
   * texts for each of their (parent) primary tokenizations.
   */
  public static Tree<String> fullyTokenize(String text) {
    return fullyTokenize(text, DEFAULT_OPTIONS);
  }

  /**
   * Create a tree of token strings where the root contains the full input
   * text, the second level contains primary tokenizations (according to
   * the given options,) and the third level contains the token revision
   * texts for each of their (parent) primary tokenizations.
   */
  public static Tree<String> fullyTokenize(String text, StandardTokenizerOptions options) {
    final Tree<String> result = new Tree<String>(text);
    if (options == null) options = DEFAULT_OPTIONS;

    for (Token primaryToken = getFirstToken(text, options); primaryToken != null; primaryToken = primaryToken.getNextToken()) {
      final Tree<String> primaryTokenNode = result.addChild(primaryToken.getText());

      for (Token revisedToken = primaryToken.getRevisedToken(); revisedToken != null; revisedToken = revisedToken.getRevisedToken()) {
        primaryTokenNode.addChild(revisedToken.getText());
      }
    }

    return result;
  }

  /**
   * Get all of the revision texts of the given token, including the token
   * itself.
   */
  public static List<String> fullyRevise(Token token) {
    final List<String> result = new ArrayList<String>();

    while (token != null) {
      result.add(token.getText());
      token = token.getRevisedToken();
    }

    return result;
  }
}
