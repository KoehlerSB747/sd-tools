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
package org.sd.nlp.conll;


/**
 * Container for a CoNLL formatted token.
 * <p>
 * @author Spencer Koehler
 */
public class ConllToken implements Comparable<ConllToken> {
  
  private String[] fields;
  private int id;
  private int startPos;  // char position of start in sentence, inclusive
  private int endPos;    // char position of end in sentence, exclusive

  public ConllToken(String tokenLine) {
    this.fields = tokenLine.split("\\t");
    this.id = 0;
    this.startPos = -1;
    this.endPos = -1;
  }

  public int getId() {
    if (this.id == 0) {
      this.id = -1;
      if (fields != null && fields.length > 0) {
        try {
          this.id = Integer.parseInt(fields[0]);
        }
        catch (NumberFormatException nfe) {
        }
      }
    }
    return this.id;
  }

  public boolean hasLetterOrDigit() {
    boolean result = false;

    final String text = getText();
    final int len = text.length();
    for (int idx = 0; idx < len; ++idx) {
      final char c = text.charAt(idx);
      if (Character.isLetterOrDigit(c)) {
        result = true;
        break;
      }
    }

    return result;
  }

  public String getText() {
    String result = null;

    if (fields != null && fields.length > 1) {
      result = fields[1];
    }

    return result;
  }

  public void setStartPos(int startPos) {
    this.startPos = startPos;
  }

  public boolean hasStartPos() {
    return startPos >= 0;
  }    

  public int getStartPos() {
    return startPos;
  }

  public void setEndPos(int endPos) {
    this.endPos = endPos;
  }

  public boolean hasEndPos() {
    return endPos >= 0;
  }    

  public int getEndPos() {
    return endPos;
  }

  public boolean hasData(ConllField field) {
    boolean result = false;

    if (field != null && fields.length > field.getIdx()) {
      final String value = fields[field.getIdx()];
      result = !isEmpty(value);
    }

    return result;
  }

  public String getData(ConllField field) {
    String result = null;

    if (field != null && fields.length > field.getIdx()) {
      result = fields[field.getIdx()];
    }

    return result;
  }

  public boolean matches(ConllField field, String value) {
    boolean result = false;

    final String data = getData(field);
    if (isEmpty(data)) {
      result = isEmpty(value);
    }
    else {
      result = data.equals(value);
    }

    return result;
  }

  public boolean matches(ConllField[] fields, String[] values) {
    boolean result = true;

    for (int i = 0; i < fields.length; ++i) {
      if (i < values.length) {
        if (!this.matches(fields[i], values[i])) {
          result = false;
          break;
        }
      }
      else {
        if (!isEmpty(this.getData(fields[i]))) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  public int compareTo(ConllToken other) {
    return this.getId() - other.getId();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (String field : fields) {
      if (result.length() > 0) result.append('\t');
      result.append(isEmpty(field) ? "_" : field);
    }

    return result.toString();
  }

  private final boolean isEmpty(String value) {
    return value == null || "".equals(value) || "_".equals(value);
  }
}
