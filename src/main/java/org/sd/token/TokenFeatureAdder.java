package org.sd.token;


/**
 * Interface for adding token features in a StandardTokenizer.
 * <p>
 * @author Spencer Koehler
 */
public interface TokenFeatureAdder {
  
  /** Add features to the given token. */
  public void addTokenFeatures(Token token);

}
