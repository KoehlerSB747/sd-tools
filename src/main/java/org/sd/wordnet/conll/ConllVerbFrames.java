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
package org.sd.wordnet.conll;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.sd.nlp.conll.ConllField;
import org.sd.nlp.conll.ConllReader;
import org.sd.nlp.conll.ConllSentence;
import org.sd.nlp.conll.ConllToken;

/**
 * A wrapper for matching a ConllSentence against WordNet verb frames.
 * <p>
 * @author Spencer Koehler
 */
public class ConllVerbFrames {
  
  private TreeSet<Integer> frames;

  private ConllSentence sentence;
  private ConllToken root;
  private ConllToken nsubj;
  private ConllToken dobj;
  private ConllToken ccomp;
  private ConllToken xcomp;
  private ConllToken acomp;
  private ConllToken obj;
  private ConllToken prep;
  private boolean hasNsubj;
  private boolean subjIsIt;
  private boolean subjIsSomething;
  private boolean subjIsSomebody;
  private boolean hasDobj;
  private boolean dObjHasAmod;
  private boolean dObjHasNoun;
  private boolean hasObj;
  private boolean objIsSomething;
  private boolean objIsSomebody;
  private boolean rootIsGerund;
  private boolean rootHasAux;
  private boolean rootHasPP;
  private boolean rootHasAcomp;
  private boolean rootHasCcomp;
  private List<ConllToken> pobjs;
  
  public ConllVerbFrames(ConllSentence sentence) {
    this.frames = new TreeSet<Integer>();

    this.sentence = sentence;
    this.root = sentence.getRoot();
    this.nsubj = sentence.getFirstChild(root, ConllField.DEPREL, "nsubj");
    this.dobj = sentence.getFirstChild(root, ConllField.DEPREL, "dobj");
    if (dobj == null) {
      dobj = sentence.getFirstChild(root, ConllField.DEPREL, "nsubjpass");
    }
    this.ccomp = sentence.getFirstChild(root, ConllField.DEPREL, "ccomp");
    this.xcomp = sentence.getFirstChild(root, ConllField.DEPREL, "xcomp");
    this.acomp = sentence.getFirstChild(root, ConllField.DEPREL, "acomp");
    init();
  }

  public TreeSet<Integer> getFrames() {
    return frames;
  }

  public Integer getMax() {
    return frames.size() == 0 ? null : frames.last();
  }

  public Integer getMaxMatch(Set<Integer> frames) {
    Integer result = null;

    if (frames != null) {
      for (Integer value = this.frames.last(); value != null; value = this.frames.lower(value)) {
        if (frames.contains(value)) {
          result = value;
          break;
        }
      }
    }

    return result;
  }

  public ConllToken getRoot() {
    return root;
  }

  public boolean isNegated() {
    boolean result = false;

    if (root != null) {
      result = sentence.getFirstChild(root, ConllField.DEPREL, "neg") != null;
    }      

    return result;
  }

  public ConllToken getObj() {
    return obj;
  }

  public ConllToken getSubj() {
    return nsubj;
  }

  public ConllToken getObjAdj() {
    ConllToken result = null;
    if (obj != null) {
      result = sentence.getFirstChild(obj, new ConllField[]{ConllField.DEPREL}, new String[]{"amod"});
    }
    return result;
  }

  public ConllToken getRootAdv() {
    ConllToken result = null;
    if (root != null) {
      result = sentence.getFirstChild(root, new ConllField[]{ConllField.DEPREL}, new String[]{"advmod"});
    }
    return result;
  }

  private final void init() {
    this.hasNsubj = (nsubj != null);
    if (nsubj != null) {
      if ("it".equalsIgnoreCase(nsubj.getText())) {
        this.subjIsIt = true;
        this.subjIsSomething = true;
        this.subjIsSomebody = false;
      }
      else {
        this.subjIsSomething = !nsubj.matches(ConllField.POSTAG, "NNP");
        this.subjIsSomebody = !nsubj.matches(ConllField.POSTAG, "NN");
      }
    }
    else {
      this.subjIsSomething = true;
      this.subjIsSomebody = true;
    }
    this.hasDobj = (dobj != null);
    this.dObjHasAmod = (sentence.getFirstChild(dobj, ConllField.DEPREL, "amod") != null);
    this.dObjHasNoun = (sentence.getFirstChild(dobj, ConllField.DEPREL, "nn") != null);

    this.obj = dobj;
    if (obj == null) obj = ccomp;
    if (obj == null) obj = xcomp;
    if (obj == null) obj = acomp;
    this.hasObj = (obj != null);

    this.objIsSomebody = this.hasDobj;  // assume dobj can be somebody unless otherwise indicated
    this.objIsSomething = true;
    if (hasDobj) {
      this.objIsSomething = !dobj.matches(ConllField.POSTAG, "NNP");
      this.objIsSomebody = !dobj.matches(ConllField.POSTAG, "NN");
    }

    this.prep = sentence.getFirstDeepChild(root, new ConllField[]{ConllField.DEPREL}, new String[]{"prep"});

    this.rootIsGerund = root.matches(ConllField.POSTAG, "VBG");
    this.rootHasAux = (sentence.getFirstChild(root, ConllField.DEPREL, "aux") != null);
    this.rootHasPP = (prep != null);
    this.rootHasAcomp = (sentence.getFirstChild(root, ConllField.DEPREL, "acomp") != null);
    this.rootHasCcomp = (sentence.getFirstChild(root, ConllField.DEPREL, "ccomp") != null);  // 8 (instead of dobj)

    if (rootHasPP) {
      pobjs = sentence.getDeepChildren(root, new ConllField[]{ConllField.DEPREL}, new String[]{"pobj"});
    }


    if (!hasObj) {
      if (subjIsSomething) frames.add(1);
      if (subjIsSomebody) frames.add(2);
      if (subjIsIt && rootIsGerund) frames.add(3);
      if (subjIsSomething && rootIsGerund && rootHasAux && rootHasPP) frames.add(4);
      if (rootHasPP) {
        if (subjIsSomething && hasPrep("to", false, true)) frames.add(12);
        if (subjIsSomebody && hasPrep("on", true, false)) frames.add(13);
        if (subjIsSomebody) frames.add(22);
        if (subjIsSomebody && hasPrep("to", false, true)) frames.add(27);
      }
      if (nsubj != null && sentence.getFirstChild(nsubj, ConllField.DEPREL, "poss") != null) frames.add(23);
    }
    else {
      if (dObjHasAmod || dObjHasNoun) frames.add(5);
      if (rootHasAcomp && subjIsSomething) frames.add(6);
      if (rootHasAcomp && subjIsSomebody) frames.add(7);
      if (subjIsSomebody) {
        if (objIsSomething) frames.add(8);
        if (objIsSomebody) frames.add(9);
      }
      if (subjIsSomething) {
        if (objIsSomebody) frames.add(10);
        if (objIsSomething) frames.add(11);
      }
      if (subjIsSomebody && hasDobj) {
        if (sentence.getFirstChild(root, ConllField.DEPREL, "dep") != null) frames.add(14);
        if (rootHasPP) {
          if (hasPrep("to", false, false)) frames.add(15);
          if (hasPrep("from", false, false)) frames.add(16);
          if (hasPrep("with", false, false)) frames.add(17);
          if (hasPrep("of", false, false)) frames.add(18);
          if (hasPrep("on", false, false)) frames.add(19);
          if (objIsSomebody) frames.add(20);
          if (objIsSomething) frames.add(21);

          if ("into".equalsIgnoreCase(prep.getText())) {
            final ConllToken verbing = sentence.getFirstChild(prep, ConllField.POSTAG, "VBG");
            if (verbing != null) {
              frames.add(30);  //todo: check if verbing's dobj child is something -vs- somebody?
            }
          }

          if (hasPrep("with", true, false)) frames.add(31);

        }
      }

      final ConllToken vbObj = obj.matches(ConllField.POSTAG, "VB") ? obj : sentence.getFirstDeepChild(obj, new ConllField[]{ConllField.POSTAG}, new String[]{"VB"});
      if (vbObj != null) {
        final boolean hasTo = (sentence.getFirstChild(vbObj, new ConllField[]{ConllField.FORM, ConllField.POSTAG, ConllField.DEPREL}, new String[]{"to", "TO", "aux"}) != null);  // to.TO.aux
        final boolean hasWhether = (sentence.getFirstChild(vbObj, new ConllField[]{ConllField.FORM, ConllField.POSTAG, ConllField.DEPREL}, new String[]{"whether", "IN", "mark"}) != null); // whether.IN.mark
          
        if (hasDobj || sentence.getFirstChild(vbObj, ConllField.DEPREL, "nsubj") != null) {
          if (hasTo) frames.add(24);
          else frames.add(25);
        }
        else {
          if (hasTo) frames.add(28);
          else {
            if (subjIsSomebody) frames.add(32);
            if (subjIsSomething) frames.add(35);
          }
          if (hasWhether) frames.add(29);
        }
      }
      if (sentence.getFirstChild(obj, new ConllField[]{ConllField.POSTAG, ConllField.DEPREL}, new String[]{"IN", "mark"}) != null) {  // that.IN.mark
        if (subjIsIt) frames.add(34);
        else frames.add(26);
      }
      if (hasDobj && dobj.getText().endsWith("ing")) frames.add(33);
    }
  }

  private final boolean hasPrep(String ptext, boolean objMustBeSomething, boolean objMustBeSomebody) {
    boolean result = false;

    if (pobjs != null) {
      for (ConllToken pobj : pobjs) {
        if (prepMatches(pobj, ptext, objMustBeSomething, objMustBeSomebody)) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private final boolean prepMatches(ConllToken pobj, String ptext, boolean objMustBeSomething, boolean objMustBeSomebody) {
    boolean result = true;

    if (ptext != null) {
      final ConllToken prep = sentence.getParent(pobj);
      result = (prep != null && ptext.equalsIgnoreCase(prep.getText()));
    }

    if (result && objMustBeSomebody) {
      result = !pobj.matches(ConllField.POSTAG, "NN");
    }

    if (result && objMustBeSomething) {
      result = !pobj.matches(ConllField.POSTAG, "NNP");
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    for (Iterator<ConllSentence> iter = new ConllReader(reader); iter.hasNext(); ) {
      final ConllSentence sentence = iter.next();
      final ConllVerbFrames verbFrames = new ConllVerbFrames(sentence);
      System.out.println(sentence.getText() + "\t" + verbFrames.getFrames());
    }
  }
}
