package org.sd.atnexec;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.sd.atn.AtnRuleStep;
import org.sd.atn.AtnState;
import org.sd.atn.BaseClassifierTest;
import org.sd.atn.ResourceManager;
import org.sd.token.Token;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * A BaseClassifierTest that checks the current token's (_wn_cat) features progress
 * compared to the previous token's (_wn_cat) features according to a defined
 * sequence.
 * <p>
 * @author Spencer Koehler
 */
public class ProgressionTokenTest extends BaseClassifierTest {
  
  private List<Sequence> seqs;
  private boolean verbose;

  public ProgressionTokenTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.seqs = new ArrayList<Sequence>();
    this.verbose = testNode.getAttributeBoolean("verbose", false);

    final NodeList seqNodes = testNode.selectNodes("seq");
    final int nodeCount = seqNodes.getLength();
    for (int nodeNum = 0; nodeNum < nodeCount; ++nodeNum) {
      final DomNode seqNode = (DomNode)seqNodes.item(nodeNum);
      seqs.add(new Sequence(seqNode));
    }
  }

  @Override
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = true;

    final AtnTokenContainer tokenContainer = new AtnTokenContainer(token, curState);

    for (Sequence seq : seqs) {
      if (!seq.doAccept(tokenContainer, verbose)) {
        result = false;
        break;
      }
    }

    return result;
  }

  static abstract class TokenContainer {

    protected abstract int getNumTokens();
    protected abstract boolean tokenHasFeature(int tokenNum, String feature);
    protected abstract String getTokenString(int tokenNum);

    TokenContainer() {
    }

    boolean verifyOrder(FeatureBag orderBag, StringBuilder message) {
      boolean result = true;

      if (orderBag == null) return result;

      final int numTokens = getNumTokens();
      SingleFeature greatestFeature = findGreatestFeature(orderBag, numTokens - 1); // last token

      if (message != null) {
        message.append(" verifyOrder[").append(orderBag).append("](").append(numTokens).append(" tokens)");
      }

      if (greatestFeature != null) {
        // verify order holds while walking backward through tokens
        for (int tokenNum = numTokens - 2; tokenNum >= 0; --tokenNum) {
          final SingleFeature lesserFeature = findLesserFeature(orderBag, greatestFeature, tokenNum);
          if (lesserFeature == null) {
            final SingleFeature greaterFeature = findGreaterFeature(orderBag, greatestFeature, tokenNum);
            if (greaterFeature != null) {
              // no lesser but yes greater ==> need to reject
              result = false;

              if (message != null) {
                message.
                  append(" token[").append(tokenNum).append("]=").append(getTokenString(tokenNum)).
                  append(" failed w/greaterFeature:").append(greaterFeature).
                  append(" than greatestFeature:").append(greatestFeature).
                  append(" for token[").append(numTokens - 1).append("]=").append(getTokenString(numTokens - 1));
              }

              break;
            }
            // else no lesser or greater, so keep looking (not constrained)
          }
          else {
            // found a lesser feature, so accept this prev token and continue to look back
            greatestFeature = lesserFeature;
          }
        }
      }
      // else last token doesn't have any matching feature, so accept (not constrained)

      return result;
    }

    boolean verifyNotAfter(FeatureBag notBag, FeatureBag afterBag, StringBuilder message) {
      boolean result = true;

      if (notBag == null) return result;

      final int numTokens = getNumTokens();
      SingleFeature notFeature = findGreatestFeature(notBag, numTokens - 1);  // last token

      if (message != null) {
        message.
          append(" verifyNotAfter[").append(notBag).append(',').append(afterBag).
          append("](").append(numTokens).append(" tokens)");
      }

      if (notFeature != null) {
        result = false;  // reject -- last token has "reject" feature

        if (message != null) {
          message.
            append(" token[").append(numTokens - 1).append("]=").append(getTokenString(numTokens - 1)).
            append(" has notFeature=").append(notFeature);
        }

        // but, accept if doesn't come "after" a token with an after feature
        if (afterBag != null) {
          result = true;  // innocent until proven guilty
          for (int tokenNum = numTokens - 2; tokenNum >= 0; --tokenNum) {
            for (SingleFeature afterFeature : afterBag.singleFeatures) {
              if (afterFeature.appliesToToken(this, tokenNum)) {  // prove guilty
                result = false;

                if (message != null) {
                  message.
                    append(" and is after token[").append(tokenNum).append("]=").append(getTokenString(tokenNum)).
                    append(" w/afterFeature=").append(afterFeature);
                }

                break;
              }
            }
            if (!result) break;
          }

          if (message != null) {
            message.append(" but is not after '").append(afterBag).append("'");
          }
        }
      }

      return result;
    }

    private final SingleFeature findGreatestFeature(FeatureBag featureBag, int tokenNum) {
      SingleFeature result = null;

      for (int i = featureBag.size - 1; i >= 0; --i) {
        final SingleFeature singleFeature = featureBag.get(i);
        if (singleFeature.appliesToToken(this, tokenNum)) {
          result = singleFeature;
          break;
        }
      }

      return result;
    }

    private final SingleFeature findLesserFeature(FeatureBag featureBag, SingleFeature refFeature, int tokenNum) {
      SingleFeature result = null;

      for (int i = (featureBag.allowRepeats ? refFeature.order : refFeature.order - 1); i >= 0; --i) {
        final SingleFeature singleFeature = featureBag.get(i);
        if (singleFeature.appliesToToken(this, tokenNum)) {
          result = singleFeature;
          break;
        }
      }

      return result;
    }

    private final SingleFeature findGreaterFeature(FeatureBag featureBag, SingleFeature refFeature, int tokenNum) {
      SingleFeature result = null;

      for (int i = (featureBag.allowRepeats ? refFeature.order : refFeature.order + 1); i < featureBag.size; ++i) {
        final SingleFeature singleFeature = featureBag.get(i);
        if (singleFeature.appliesToToken(this, tokenNum)) {
          result = singleFeature;
          break;
        }
      }

      return result;
    }
  }

  static final class SingleFeature {
    public final String def;
    public final String feature;
    public final boolean reverse;
    public final int order;

    SingleFeature(String feature, int order) {
      this.def = feature;
      final int len = feature.length();
      this.reverse = (len > 1) ? feature.charAt(0) == '!' : false;
      this.feature = reverse ? feature.substring(1) : feature;
      this.order = order;
    }

    boolean appliesToToken(TokenContainer tokenContainer, int tokenPos) {
      boolean result = tokenContainer.tokenHasFeature(tokenPos, feature);
      if (reverse) {
        result = !result;
      }
      return result;
    }

    public String toString() {
      return def;
    }
  }

  static class FeatureBag {
    public final String def;
    public final List<SingleFeature> singleFeatures;
    public final int size;
    public final boolean allowRepeats;

    FeatureBag(String def) {
      this(def, false);
    }

    FeatureBag(String def, boolean allowRepeats) {
      this.def = def;
      this.singleFeatures = new ArrayList<SingleFeature>();

      final String[] pieces = def.split("\\s*,\\s*");
      for (int i = 0; i < pieces.length; ++i) {
        final String piece = pieces[i];
        singleFeatures.add(new SingleFeature(piece, i));
      }

      this.size = singleFeatures.size();
      this.allowRepeats = allowRepeats;
    }

    public SingleFeature get(int pos) {
      return (pos >= 0 && pos < size) ? singleFeatures.get(pos) : null;
    }

    public String toString() {
      return def;
    }

    public void toString(StringBuilder result, String label) {
      if (result.length() > 0) result.append(", ");
      result.append(label).append('=').append(def);
    }
  }

  static class Sequence {
    private FeatureBag orderBag;
    private FeatureBag rejectBag;
    private FeatureBag afterBag;
    private boolean verbose;

    Sequence(DomNode seqNode) {
      this.orderBag = buildFeatureBag(seqNode, "order");
      this.rejectBag = buildFeatureBag(seqNode, "reject");
      this.afterBag = buildFeatureBag(seqNode, "after");
      this.verbose = seqNode.getAttributeBoolean("verbose", false);
    }

    Sequence(String order, String reject, String after, boolean allowRepeats) {
      this.orderBag = (order != null && !"".equals(order)) ? new FeatureBag(order, allowRepeats) : null;
      this.rejectBag = (reject != null && !"".equals(reject)) ? new FeatureBag(reject) : null;
      this.afterBag = (after != null && !"".equals(after)) ? new FeatureBag(after) : null;
      this.verbose = false;
    }

    private FeatureBag buildFeatureBag(DomNode seqNode, String key) {
      FeatureBag result = null;

      final String value = seqNode.getAttributeValue(key, null);
      if (value != null && !"".equals(value)) {
        result = new FeatureBag(value, seqNode.getAttributeBoolean("allowRepeats", true));
      }

      return result;
    }

    final boolean doAccept(TokenContainer tokenContainer, boolean verbose) {
      boolean result = true;

      final StringBuilder message = (verbose || this.verbose) ? new StringBuilder() : null;

      if (orderBag != null) {
        result = tokenContainer.verifyOrder(orderBag, message);
      }

      if (result && rejectBag != null) {
        result = tokenContainer.verifyNotAfter(rejectBag, afterBag, message);
      }

      if (message != null) {
        message.append(" result=").append(result);
        System.out.println("\tProgressionTokenTest[" + this.toString() + "]: " + message);
      }

      return result;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      if (orderBag != null) orderBag.toString(result, "order");
      if (rejectBag != null) rejectBag.toString(result, "reject");
      if (afterBag != null) afterBag.toString(result, "after");
      
      return result.toString();
    }
  }


  static class AtnTokenContainer extends TokenContainer {

    private LinkedList<Token> tokens;

    AtnTokenContainer(Token token, AtnState curState) {
      super();

      this.tokens = new LinkedList<Token>();
      tokens.add(token);

      // add prior repeating tokens:
      //   in the same constituent with the same category (from the same ruleStep)
      final AtnRuleStep curStep = curState.getRuleStep();
      final AtnState constituentTop = curState.getConstituentTop();
      for (AtnState prevState = (curState != null) ? curState.getParentState() : null;
           prevState != null;
           prevState = prevState.getParentState()) {
        if (prevState.getRuleStep() == curStep && prevState.getMatched()) {
          this.tokens.addFirst(prevState.getInputToken());
        }
        if (prevState == constituentTop) break; //NOTE: top could also be first match
      }
    }

    protected int getNumTokens() {
      return tokens.size();
    }

    protected boolean tokenHasFeature(int tokenNum, String feature) {
      boolean result = false;

      final Token token = getToken(tokenNum);
      if (token != null) {
        result = token.hasFeatureValue(feature, null, String.class, "_wn_cat");
      }

      return result;
    }

    protected String getTokenString(int tokenNum) {
      String result = null;

      final Token token = getToken(tokenNum);
      if (token != null) {
        result = token.toString();
      }

      return result;
    }

    private final Token getToken(int tokenNum) {
      Token result = null;

      if (tokenNum >= 0 && tokenNum < tokens.size()) {
        result = tokens.get(tokenNum);
      }

      return result;
    }
  }
}
