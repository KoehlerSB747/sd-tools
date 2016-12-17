/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


/**
 * Container and utility class for adding information to tokens.
 * <p>
 * @author Spence Koehler
 */
public abstract class TokenInfo {
  
  /**
   * Add this instance's info as features to the given token.
   */
  public abstract void addTokenFeatures(Token token, Object source);


  private int tokenStart;
  private int tokenEnd;
  private int priority;
  private String category;
  private int endIndex;

  public TokenInfo() {
    this(0, 0, 0, null);
  }

  public TokenInfo(int tokenStart, int tokenEnd, int priority, String category) {
    this.tokenStart = tokenStart;
    this.tokenEnd = tokenEnd;
    this.priority = priority;
    this.category = category;
    this.endIndex = tokenEnd;
  }

  public int getTokenStart() {
    return tokenStart;
  }

  public void setTokenStart(int tokenStart) {
    this.tokenStart = tokenStart;
  }

  public int getTokenEnd() {
    return tokenEnd;
  }

  public void setTokenEnd(int tokenEnd) {
    this.tokenEnd = tokenEnd;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public boolean hasPriority() {
    return priority > 0;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public int updateEndIndex(int offset) {
    this.endIndex = this.tokenEnd + offset;
    return this.endIndex;
  }

  public int getEndIndex() {
    return endIndex;
  }

  public int getStartIndex() {
    return this.tokenStart + (this.endIndex - this.tokenEnd);
  }
}
