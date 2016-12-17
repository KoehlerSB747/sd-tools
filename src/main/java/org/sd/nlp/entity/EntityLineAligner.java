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
package org.sd.nlp.entity;


import org.sd.nlp.conll.ConllSentenceAligner;

/**
 * Utility to align entity lines of potentially differing formats.
 * <p>
 * @author Spencer Koehler
 */
public class EntityLineAligner {

  private String baseLine;
  private String altLine;
  private ConllSentenceAligner _conllSentenceAligner; //NOTE: treats "base" as "orig"

  public EntityLineAligner(String baseLine) {
    this.baseLine = baseLine;
    this.altLine = null;
    this._conllSentenceAligner = null;
  }

  public String getBaseLine() {
    return baseLine;
  }

  public EntityLineAligner setAltLine(String altLine) {
    this.altLine = altLine;
    this._conllSentenceAligner = null;
    return this;
  }

  public boolean hasAltLine() {
    return altLine != null && !"".equals(altLine) && !altLine.equals(baseLine);
  }

  public String getAltLine() {
    return altLine;
  }

  public boolean aligns() {
    return getConllSentenceAligner().aligns();
  }

//todo: add accessors for getting tokens [(positions, text); (current, prev, next)]

  public int[] getBasePos(int altPos) {
    final ConllSentenceAligner aligner = getConllSentenceAligner();
    return aligner.getConllPos(altPos);
  }

  ConllSentenceAligner getConllSentenceAligner() {
    if (_conllSentenceAligner == null) {
      this._conllSentenceAligner = new ConllSentenceAligner(hasAltLine() ? altLine : baseLine, baseLine);
    }
    return _conllSentenceAligner;
  }
}
