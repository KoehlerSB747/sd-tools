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


/**
 * Container for a pointer definition.
 * <p>
 * @author Spencer Koehler
 */
public class PointerDefinition {
  
  private static long nextUID = 0;
  private static final Object uidMutex = new Object();
  private static final long getNextUID() {
    long result = 0L;
    synchronized (uidMutex) {
      result = nextUID++;
    }
    return result;
  }

  private long uid;
  private String lexFileName;
  private SimpleWord headWord;
  private SimpleWord satelliteWord;
  private String pointerSymbol;
  private String _formattedPointerDefinition;

  public PointerDefinition() {
    this.uid = getNextUID();
    this.lexFileName = null;
    this.headWord = null;
    this.satelliteWord = null;
    this.pointerSymbol = null;
    this._formattedPointerDefinition = null;
  }

  public PointerDefinition(String formattedPtr) {
    this();

    // lex_filename:? headWord lex_id? ^? satelliteWord? lex_id? , pointer_symbol
    final int commaPos = formattedPtr.indexOf(',');
    if (commaPos < 0) {
      final boolean stopHere = true;
    }

    // decode pointerSymbol
    this.pointerSymbol = formattedPtr.substring(commaPos + 1);
    formattedPtr = formattedPtr.substring(0, commaPos);

    // decode satelliteWord
    final int caretPos = formattedPtr.indexOf('^');
    if (caretPos >= 0) {
      final String satelliteText = formattedPtr.substring(caretPos + 1);
      this.satelliteWord = new SimpleWord(satelliteText);

      formattedPtr = formattedPtr.substring(0, caretPos);
    }

    // decode lexFileName
    final int colonPos = formattedPtr.indexOf(':');
    if (colonPos >= 0) {
      final String lexFileName = formattedPtr.substring(0, colonPos);
      this.lexFileName = lexFileName;

      formattedPtr = formattedPtr.substring(colonPos + 1);
    }

    // decode headWord
    this.headWord = new SimpleWord(formattedPtr);
  }

  /**
   * Get a globally unique ID for this pointer definition.
   */
  public long getUID() {
    return uid;
  }

  public boolean hasLexFileName() {
    return lexFileName != null;
  }

  public String getLexFileName() {
    return lexFileName;
  }

  public void setLexFileName(String lexFileName) {
    this.lexFileName = lexFileName;
  }

  public boolean hasHeadWord() {
    return headWord != null && headWord.hasWord();
  }

  public SimpleWord getHeadWord() {
    return headWord;
  }

  public void setHeadWord(SimpleWord headWord) {
    this.headWord = headWord;
  }

  public boolean hasSatelliteWord() {
    return satelliteWord != null && satelliteWord.hasWord();
  }

  public SimpleWord getSatelliteWord() {
    return satelliteWord;
  }

  public void setSatelliteWord(SimpleWord satelliteWord) {
    this.satelliteWord = satelliteWord;
  }

  public boolean hasPointerSymbol() {
    return pointerSymbol != null && !"".equals(pointerSymbol);
  }

  public String getPointerSymbol() {
    return pointerSymbol;
  }

  public void setPointerSymbol(String pointerSymbol) {
    this.pointerSymbol = pointerSymbol;
  }

  public String getFormattedPointerDefinition() {
    if (_formattedPointerDefinition == null) {
      final StringBuilder builder = new StringBuilder();
      if (lexFileName != null) {
        builder.append(lexFileName).append(':');
      }
      if (headWord != null) {
        builder.append(headWord.getName());  //<word><lexId>
      }
      if (satelliteWord != null) {
        builder.append('^');
        builder.append(satelliteWord.getName());  //<word><lexId>
        final int lexId = satelliteWord.getLexId();
        if (lexId > 0) {
          builder.append(lexId);
        }
      }
      builder.append(',');
      if (pointerSymbol != null && !"".equals(pointerSymbol)) {
        builder.append(pointerSymbol);
      }
      _formattedPointerDefinition = builder.toString();
    }
    return _formattedPointerDefinition;
  }

  public String toString() {
    return getFormattedPointerDefinition();
  }
}
