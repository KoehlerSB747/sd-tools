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


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sd.token.FeatureConstraint;
import org.sd.token.Token;
import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.TokenFeatureAdder;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.wordnet.util.NormalizeUtil;
import org.sd.xml.DataProperties;

/**
 * Implementation of the Tokenizer interface linked with WordNet info.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetTokenizer extends StandardTokenizer {
  
  public static final String NORM_FEATURE = "_wn_norm";
  public static final String SYNSETS_FEATURE = "_wn_synsets";
  public static final String TAGS_FEATURE = "_wn_tags";
  public static final String CATEGORY_VALUE = "_wn_cat";


  /**
   * FeatureConstraint for finding the norm feature set by this class.
   */
  public static final FeatureConstraint NORM_FEATURE_CONSTRAINT =
    FeatureConstraint.getInstance(NORM_FEATURE, WordNetTokenizer.class, String.class);

  /**
   * FeatureConstraint for finding the synsets feature set by this class.
   */
  public static final FeatureConstraint SYNSETS_FEATURE_CONSTRAINT =
    FeatureConstraint.getInstance(SYNSETS_FEATURE, WordNetTokenizer.class, String.class);

  /**
   * FeatureConstraint for finding the tags feature set by this class.
   */
  public static final FeatureConstraint TAGS_FEATURE_CONSTRAINT =
    FeatureConstraint.getInstance(TAGS_FEATURE, WordNetTokenizer.class, String.class);

  /**
   * FeatureConstraint for finding all category features set by this class.
   */
  public static final FeatureConstraint CATEGORY_VALUE_CONSTRAINT =
    new FeatureConstraint();
  static {
    CATEGORY_VALUE_CONSTRAINT.setValue(CATEGORY_VALUE);
    CATEGORY_VALUE_CONSTRAINT.setClassType(WordNetTokenizer.class);
  }


  private LexDictionary dict;
  private WordLookupStrategy lookupStrategy;
  private Map<Integer, Token> pos2token;

  public WordNetTokenizer(LexDictionary dict, WordLookupStrategy lookupStrategy, String text) {
    this(dict, lookupStrategy, text, null);
  }

  public WordNetTokenizer(LexDictionary dict, WordLookupStrategy lookupStrategy, String text, StandardTokenizerOptions options) {
    super(text, TokenUtil.buildTokenizerOptions(dict, options));
    this.dict = dict;
    this.lookupStrategy = lookupStrategy;
    this.pos2token = new HashMap<Integer, Token>();

    super.setTokenFeatureAdder(new MyTokenFeatureAdder());
  }

  public LexDictionary getLexDictionary() {
    return dict;
  }

  /**
   * Get the token that starts at the given position. To get the first
   * token, use GetToken(0).
   *
   * @return The token at the position or null.
   */
  public Token getToken(int startPosition) {
    Token result = pos2token.get(startPosition);

    if (result != null) {
      // use cached result, if available
      return result;
    }

    // Get the token, but automatically revise until either the text is defined
    // or we've reached the last revision. The resulting token should behave as
    // if though it is the first revision.

    final Token standardToken = super.getToken(startPosition);
    result = narrowToDefined(standardToken, 0, 0);

    if (result != null) {
      // cache the result to re-use next time
      pos2token.put(startPosition, result);
    }

    return result;
  }

  /**
   * Revise the token if possible.
   *
   * @return A revised token or null.
   */
  public Token revise(Token token) {
    Token result = null;

    final Token revisedToken = super.revise(token);
    if (revisedToken != null) {
      result = narrowToDefined(revisedToken, token.getSequenceNumber(), token.getRevisionNumber() + 1);
    }
    
    return result;
  }

  /**
   * Get the next token after the given token if possible.
   *
   * @return The next token or null.
   */
  public Token getNextToken(Token token) {
    int startPosition = super.findEndBreakForward(token.getEndIndex(), false);
    if (startPosition < 0) startPosition = token.getEndIndex();
    final Token result = this.getToken(startPosition);  // utilize cache
    if (result != null) {
      // update sequence number
      result.setSequenceNumber(token.getSequenceNumber() + 1);
    }
    return result;
  }

  /**
   * Determine whether the given token has custom token features from this tokenizer.
   */
  public static final boolean hasCustomTokenFeatures(Token token) {
    return token.hasFeatures() && token.getFeatures().hasFeature(NORM_FEATURE_CONSTRAINT);
  }

  /**
   * Determine whether the given token has found categories.
   */
  public static final boolean hasCategories(Token token) {
    return token.hasFeatures() && token.getFeatures().hasFeature(CATEGORY_VALUE_CONSTRAINT);
  }

  /**
   * Determine whether the given token has found synsets.
   */
  public static final boolean hasSynsets(Token token) {
    return token.hasFeatures() && token.getFeatures().hasFeature(SYNSETS_FEATURE_CONSTRAINT);
  }

  /**
   * Determine whether the given token has found tags.
   */
  public static final boolean hasTags(Token token) {
    return token.hasFeatures() && token.getFeatures().hasFeature(TAGS_FEATURE_CONSTRAINT);
  }

  

  private final class MyTokenFeatureAdder implements TokenFeatureAdder {
    public void addTokenFeatures(Token token) {
      // if the wnToken feature doesn't exist, create and add it for this token
      doAddTokenFeatures(token, null);
    }
  }

  private final Token narrowToDefined(Token standardToken, int seqNum, int revNum) {
    Token result = standardToken;
    WordNetToken wnToken = null;

    if (standardToken != null) {
      // build WordNetToken from standardToken
      wnToken = buildWordNetToken(standardToken);

      if (lookupStrategy != null) {
        // auto-revise until we have a definition or finish revising
        if (wnToken == null || wnToken.isUnknown()) {
          // need to revise to find a defined token
          for (Token revisedToken = super.revise(standardToken);
               revisedToken != null;
               revisedToken = super.revise(revisedToken)) {
            wnToken = buildWordNetToken(revisedToken);
            if (wnToken != null && wnToken.hasCategories()) {
              // found defined token -- can stop revising now
              break;
            }
          }
        }
      }

      // Update token with correct seqNum, revNum, and features
      if (wnToken != null) {
        final Token theToken = wnToken.getToken();
        if (theToken != null) {
          theToken.setRevisionNumber(revNum);
          theToken.setSequenceNumber(seqNum);
          result = theToken;
        }

        // stow wnToken data as a feature(s) on theToken
        doAddTokenFeatures(theToken, wnToken);
      }
    }

    return result;
  }

  private final WordNetToken buildWordNetToken(Token token) {
    WordNetToken wnToken = null;

    //todo: consider creating a cache of these for performance

    final String text = token.getText();
    final String norm = NormalizeUtil.normalizeForLookup(text);
    if (lookupStrategy == null) {
      wnToken = new WordNetToken().setInput(text).setNorm(norm);
      wnToken.setToken(token);
    }
    else {
      wnToken = lookupStrategy.lookup(text, norm);
      if (wnToken != null) wnToken.setToken(token);
    }

    return wnToken;
  }

  private final WordNetToken doAddTokenFeatures(Token token, WordNetToken wnToken) {
    //NOTE: Only add features if we haven't already done so
    if (token == null || hasCustomTokenFeatures(token)) return wnToken;

    if (wnToken == null) {
      wnToken = buildWordNetToken(token);
    }

    if (wnToken != null) {
      
      // _wn_norm="..."
      token.setFeature(NORM_FEATURE, wnToken.getNorm(), this);

      // _wn_synsets="sn1,sn2,..."
      if (wnToken.hasSynsets()) {
        token.setFeature(SYNSETS_FEATURE, wnToken.getSynsetNames(), this);
      }

      // _wn_tags="tag1,tag2,..."
      if (wnToken.hasTags()) {
        token.setFeature(TAGS_FEATURE, wnToken.getTagNames(), this);
      }

      // cat="_wn_cat" for each category
      if (wnToken.hasCategories()) {
        for (String category : wnToken.getCategories()) {
          token.setFeature(category, CATEGORY_VALUE, this);
        }
      }
    }

    return wnToken;
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //   dbFileDir --  (required) path to dbFileDir for building LexDictionary
    //
    // Args:
    //   string(s) to tokenize
    //

    final DataProperties dataProperties = new DataProperties(args);
    args = dataProperties.getRemainingArgs();

    final File dbFileDir = new File(dataProperties.getString("dbFileDir"));
    final LexDictionary dict = new LexDictionary(new LexLoader(dbFileDir));
    final SimpleWordLookupStrategy lookupStrategy = new SimpleWordLookupStrategy(dict);

    for (String arg : args) {
      final WordNetTokenizer tokenizer = new WordNetTokenizer(dict, lookupStrategy, arg);
      System.out.println("\nComplete Tokenization of '" + arg + "':\n");
      org.sd.token.TokenUtil.doMain(tokenizer, StandardTokenizerFactory.completeTokenization(tokenizer));
    }
  }
}
