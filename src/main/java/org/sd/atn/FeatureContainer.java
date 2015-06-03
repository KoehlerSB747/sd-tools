package org.sd.atn;


import java.util.Date;
import org.sd.token.Token;
import org.sd.xml.DomNode;

/**
 * Class for testing token features.
 * <p>
 * @author Spencer Koehler
 */
public class FeatureContainer {
  
  public static final String TYPED_FEATURE_KEY = "*";
  public static final int WHEN_EXISTS = 0;
  public static final int WHEN_EQUALS = 1;
  public static final int WHEN_MATCHES_PREV = 2;
  public static final int WHEN_MATCHES_NEXT = 3;


  private String featureName;
  private Class type;
  private int when;
  private String text;
  private boolean reverse;
  private boolean verbose;

  public FeatureContainer(DomNode featureElement) {
    this.featureName = getFeatureName(featureElement);
    this.type = getType(featureElement); //NOTE: may adjust featureName
    this.when = getWhen(featureElement);
    this.text = featureElement.getTextContent().trim();
    this.reverse = featureElement.getAttributeBoolean("reverse", false);
    this.verbose = featureElement.getAttributeBoolean("verbose", false);
  }

  public String getFeatureKey() {
    return (featureName == null && type != null) ? TYPED_FEATURE_KEY : featureName;
  }

  public String getFeatureName() {
    return featureName;
  }

  public Class getType() {
    return type;
  }

  public boolean doClassify(Token token) {
    boolean result = false;

    switch (when) {
    case WHEN_EQUALS :
      // true when text equals feature's value (.toString)
      result = token.hasFeatureValue(featureName, null, type, text);
      break;
    case WHEN_MATCHES_PREV :
      // true when previous state's token's feature value matches this token's feature value
      final Token prevToken = TokenTest.getPrevToken(token);
      // assume that if this is the first token that the prev "would" match
      result = prevToken == null ? true : token.hasMatchingFeatureValue(prevToken, featureName, null, type);
      break;
    case WHEN_MATCHES_NEXT :
      // true when next state's token's feature value matches this token's feature value
      final Token nextToken = token.getNextToken();
      // assume that if this is the last token that the next "would" match
      result = nextToken == null ? true : token.hasMatchingFeatureValue(nextToken, featureName, null, type);
      break;
    default : // WHEN_EXISTS
      // true when feature exists on token
      if (token.getFeatureValue(featureName, null, type) != null) {
        result = true;
      }
      break;
    }

    if (verbose) {
      System.out.println("FeatureContainer(" + featureName + ") token=" + token + " (pre)result=" + result + " reverse=" + reverse);
    }

    return reverse ? !result : result;
  }

  private final String getFeatureName(DomNode featureElement) {
    String result  = featureElement.hasAttributes() ? featureElement.getAttributeValue("name", null) : null;
    if (result == null) {
      result = featureElement.getTextContent().trim();
      if ("".equals(result)) result = null;
    }
    return result;
  }

  private final Class getType(DomNode featureElement) {
    Class result = null;
    final String typeString = featureElement.getAttributeValue("type", null);
    if (typeString != null && !"".equals(typeString)) {
      if ("interp".equals(typeString)) {
        result = ParseInterpretation.class;
      }
      else if ("parse".equals(typeString)) {
        //NOTE: this requires an adjustment the featureName
        this.featureName = AtnParseBasedTokenizer.SOURCE_PARSE;
        result = Parse.class;
      }
      else if ("string".equals(typeString)) {
        result = String.class;
      }
      else {
        try {
          result = Class.forName(typeString);
        }
        catch (ClassNotFoundException e) {
          System.err.println(new Date() +
                             ": RoteListClassifier bad feature type '" +
                             typeString + "' IGNORED");
          result = null;
        }
      }
    }
    return result;
  }

  private final int getWhen(DomNode featureElement) {
    int result = WHEN_EXISTS;

    // exists | equals | matches-prev | matches-next

    String when = featureElement.hasAttributes() ? featureElement.getAttributeValue("when", null) : null;
    if (when != null && !"".equals(when)) {
      when = when.toLowerCase();
      if ("equals".equals(when)) {
        result = WHEN_EQUALS;
      }
      else if (when.startsWith("matches")) {
        if (when.endsWith("prev")) {
          result = WHEN_MATCHES_PREV;
            
        }
        else if (when.endsWith("next")) {
          result = WHEN_MATCHES_NEXT;
        }
      }
    }
    //else, default to WHEN_EXISTS

    return result;
  }
}
