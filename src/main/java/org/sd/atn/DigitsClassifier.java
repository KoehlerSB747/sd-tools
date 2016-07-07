/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.range.IntegerRange;
import org.sd.util.Usage;
import org.sd.xml.DomElement;

/**
 * Classifier for recognizing digits (for improved efficiency over regexes).
 * <p>
 * Attributes:
 * <ul>
 * <li>feature -- (optional, default='value') specifies name of feature holding
 *                 recognized value.</li>
 * <li>range -- (optional, default unbounded) specifies the range of acceptable
 *               values</li>
 * <li>acceptUnknowns -- (optional, default=false) specifies whether to
 *                       accept '?' as an unknown digit</li>
 * <li>requireTrueDigit -- (optional, default=true) specifies whether to accept
 *                         letters that look like digits without finding any
 *                         true digits</li>
 * <li>minLength -- (optional, default=0 [unbounded]) specifies the minimum
 *                  acceptable input text length (e.g. at least 2 digits).</li>
 * <li>ignoreLetters -- (optional, default=false) specifies whether to ignore
 *                      letters and accept any digits found.</li>
 * <li>add -- (optional) specifies a value to add to the extracted value for the stored feature.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "org.sd.atn.RoteListClassifier for recognizing digits\n" +
       "(for improved efficiency over regexes).\n" +
       "\n" +
       "Attributes:\n" +
       "\n" +
       "  feature -- (optional, default='value') specifies name of feature holding\n" +
       "             recognized value.\n" +
       "  range -- (optional, default unbounded) specifies the range of acceptable\n" +
       "           values\n" +
       "  acceptUnknowns -- (optional, default=false) specifies whether to\n" +
       "                    accept '?' as an unknown digit\n" +
       "  requireTrueDigit -- (optional, default=true) specifies whether to accept\n" +
       "                      letters that look like digits without finding any\n" +
       "                      true digits\n" +
       "  minLength -- (optional, default=0 [unbounded]) specifies the minimum\n" +
       "               acceptable input text length (e.g. at least 2 digits).\n" +
       "  ignoreLetters -- (optional, default=false) specifies whether to ignore\n" +
       "                   letters and accept any digits found.\n" +
       "  add -- (optional) specifies a value to add to the extracted value for the\n" +
       "         stored feature."
  )
public class DigitsClassifier extends RoteListClassifier {

  private String featureName;
  private IntegerRange range;
  private boolean acceptUnknowns;
  private boolean requireTrueDigit;
  private int minLength;
  private int maxLength;
  private boolean ignoreLetters;
  private int addValue;

  public DigitsClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, resourceManager, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    getTokenClassifierHelper().setMaxWordCount(1);

    this.featureName = classifierIdElement.getAttributeValue("feature", "value");

    this.range = null;
    final String rangeString = classifierIdElement.getAttributeValue("range", null);
    if (rangeString != null) {
      this.range = new IntegerRange(rangeString);
    }

    this.acceptUnknowns = classifierIdElement.getAttributeBoolean("acceptUnknowns", false);
    this.requireTrueDigit = classifierIdElement.getAttributeBoolean("requireTrueDigit", true);

    this.minLength = classifierIdElement.getAttributeInt("minLength", 0);
    this.maxLength = classifierIdElement.getAttributeInt("maxLength", Integer.MAX_VALUE);

    this.ignoreLetters = classifierIdElement.getAttributeBoolean("ignoreLetters", false);
    this.addValue = classifierIdElement.getAttributeInt("add", 0);
  }
  
  protected final String getFeatureName() {
    return featureName;
  }

  protected final void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  protected final IntegerRange getRange() {
    return range;
  }

  protected final void setRange(String rangeString) {
    this.range = new IntegerRange(rangeString);
  }

  protected final boolean acceptUnknowns() {
    return acceptUnknowns;
  }

  protected final void setAcceptUnknowns(boolean acceptUnknowns) {
    this.acceptUnknowns = acceptUnknowns;
  }

  protected final boolean requireTrueDigit() {
    return requireTrueDigit;
  }

  protected final void setRequireTrueDigit(boolean requireTrueDigit) {
    this.requireTrueDigit = requireTrueDigit;
  }

  protected final int getMinLength() {
    return minLength;
  }

  protected final void setMinLength(int minLength) {
    this.minLength = minLength;
  }

  protected final int getMaxLength() {
    return maxLength;
  }

  protected final void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  protected final boolean ignoreLetters() {
    return ignoreLetters;
  }

  protected final void setIgnoreLetters(boolean ignoreLetters) {
    this.ignoreLetters = ignoreLetters;
  }


  public boolean doClassify(Token token, AtnState atnState) {
    boolean result = false;

    TextAndFeatures textAndFeatures = null;

    if (!doClassifyStopword(token, atnState)) {
      result = doClassifyTerm(token, atnState);

      if (!result) {
        textAndFeatures = getDigitsTextAndFeatures(token, atnState);
        if (textAndFeatures == null || textAndFeatures.fail()) {
          result = false;
        }
        else {
          if (textAndFeatures.done()) {
            result = true;
          }
          else { // !textAndFeatures.done()
            final String text = textAndFeatures.getText();

            if (text != null && text.length() >= minLength && text.length() <= maxLength) {
              result = verify(text);

              if (!result && acceptUnknowns) {
                final String unknownEnhancedText = enhanceUnknowns(text);
                if (unknownEnhancedText != null) {
                  result = verify(unknownEnhancedText);
                }
              }
            }
          }
        }
      }
    }

    if (result && featureName != null && !"".equals(featureName) && textAndFeatures != null) {
      // set the digits text as "featureName" feature
      if (textAndFeatures.hasText() && !token.hasFeatures() || !token.getFeatures().hasFeatureType(featureName)) {
        String theText = textAndFeatures.getText();
        if (addValue != 0) {
          try {
            final int value = Integer.parseInt(theText);
            theText = Integer.toString(value + addValue);
          }
          catch (NumberFormatException nfe) {
            // ignore -- fail to add
          }
        }
        token.setFeature(featureName, theText, this);
      }
      // set other features if present
      if (textAndFeatures.hasFeatures()) {
        for (Map.Entry<String, String> feature : textAndFeatures.getFeatures().entrySet()) {
          final String key = feature.getKey();
          final String value = feature.getValue();
          token.setFeature(key, value, this);
        }
      }
    }

    return result;
  }

  /**
   * Get the text from the token for digit testing along with any
   * features that should be added (above and beyond the "featureName" for
   * the digits text) if the test is successful.
   * <p>
   * Note that this is triggered after checking for rote list stopwords and
   * terms, but before applying primary digit classification, including:
   * <ul>
   * <li>text length (min/max) constraints</li>
   * <li>verifying digit and range constraints</li>
   * <li>accepting/enhancing unknowns (e.g., "?" chars)</li>
   * </ul>
   * The primary digit classification can be overridden (or bypassed)
   * by an extending classifier by setting textAndFeatures.done(true).
   */
  protected TextAndFeatures getDigitsTextAndFeatures(Token token, AtnState atnState) {
    return new TextAndFeatures(token.getText(), null);
  }

  private final boolean verify(String text) {
    boolean result = false;

    final int[] intValue = new int[]{0};

    if (text != null && !"".equals(text) && verifyDigits(text, intValue)) {
      if (range == null || range.includes(intValue[0])) {
        result = true;
      }
    }

    return result;
  }

  private final boolean verifyDigits(String text, int[] intValue) {
    boolean result = false;

    if (!ignoreLetters) {
      result = getTokenClassifierHelper().isDigits(text, intValue, requireTrueDigit);
    }
    else {
      result = getTokenClassifierHelper().hasDigits(text, intValue, requireTrueDigit);
    }

    return result;
  }

  private final String enhanceUnknowns(String text) {
    final StringBuilder result = new StringBuilder();

    boolean foundUnknown = false;
    final int len = text.length();

    for (int i = 0; i < len; ++i) {
      char c = text.charAt(i);
      if (c == '?') {
        // If a single question mark is at the end of the string,
        // then ignore it but report that we found an unknown.
        // (e.g., "1869?")

        // Conversely, if a question mark is embedded in the text or multiple
        // question marks exist, then pretend the '?' is a '1'
        // for digit verification purposes.
        // (e.g. "18??")

        if (foundUnknown || i == 0 || i < len - 1) {
          c = '1';
          foundUnknown = true;
        }
        else {
          foundUnknown = true;
          break;
        }
      }
      result.append(c);
    }

    return foundUnknown ? result.toString() : null;
  }

  public static class TextAndFeatures {
    private String text;
    private Map<String, String> features;
    private boolean fail;
    private boolean done;

    public TextAndFeatures(String text, Map<String, String> features) {
      this.text = text;
      this.features = features;
      this.fail = false;
      this.done = false;
    }

    public boolean hasText() {
      return text != null && !"".equals(text);
    }
    
    public String getText() {
      return text;
    }

    public boolean hasFeatures() {
      return features != null && features.size() > 0;
    }

    public Map<String, String> getFeatures() {
      return features;
    }

    public void setFail(boolean fail) {
      this.fail = fail;
    }

    public boolean fail() {
      return fail;
    }

    /**
     * Mark classification as finished, bypassing:
     * <ul>
     * <li>text length (min/max) constraints</li>
     * <li>verifying digit and range constraints</li>
     * <li>accepting/enhancing unknowns (e.g., "?" chars)</li>
     * </ul>
     */
    public void setDone(boolean done) {
      this.done = done;
    }

    public boolean done() {
      return done;
    }
  }
}
