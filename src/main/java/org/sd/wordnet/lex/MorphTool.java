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


import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.wordnet.util.NormalizeUtil;

/**
 * Utility for morphological analysis and mutation.
 * <p>
 * @author Spencer Koehler
 */
public class MorphTool {

  public static final int NOUN_BIT = 0x01;
  public static final int VERB_BIT = 0x02;
  public static final int ADJ_BIT = 0x04;
  public static final int ADV_BIT = 0x08;

  private static final Map<String, Integer> POS_MAP = new HashMap<String, Integer>();
  static {
    POS_MAP.put("noun", NOUN_BIT);
    POS_MAP.put("n", NOUN_BIT);
    POS_MAP.put("verb", VERB_BIT);
    POS_MAP.put("vrb", VERB_BIT);
    POS_MAP.put("v", VERB_BIT);
    POS_MAP.put("adj", ADJ_BIT);
    POS_MAP.put("adv", ADV_BIT);
  }

  public static final boolean isNoun(int bitmask) {
    return (bitmask & NOUN_BIT) == NOUN_BIT;
  }

  public static final boolean isVerb(int bitmask) {
    return (bitmask & VERB_BIT) == VERB_BIT;
  }

  public static final boolean isAdjective(int bitmask) {
    return (bitmask & ADJ_BIT) == ADJ_BIT;
  }

  public static final boolean isAdverb(int bitmask) {
    return (bitmask & ADV_BIT) == ADV_BIT;
  }

  public static final int getPosBit(File file) {
    return file == null ? 0 : findPosBit(file.getName());
  }

  public static final int findPosBit(String filename) {
    int result = 0;

    if (filename != null && !"".equals(filename)) {
      final String[] namePieces = filename.split("\\.");
      for (String namePiece : namePieces) {
        result = getPosBit(namePiece);
        if (result != 0) break;
      }
    }

    return result;
  }

  public static final int getPosBit(String posName) {
    final Integer bit = POS_MAP.get(posName.toLowerCase());
    return (bit == null) ? 0 : bit;
  }


  private File dictDir;
  private Map<String, List<BaseForm>> exceptions;

  public MorphTool(File dictDir) throws IOException {
    this.dictDir = dictDir;
    this.exceptions = new HashMap<String, List<BaseForm>>();
    loadExceptions();
  }

  /**
   * Get potential derived base forms according to exceptions lists and
   * morphology rules.
   * <p>
   * Note that the candidates returned will not have been validated against
   * actual existing words.
   */
  public Collection<Derivation> deriveBaseForms(String normInput) {
    Map<String, Derivation> base2derivation = new HashMap<String, Derivation>();

    // add derivations from exceptions lists
    addDerivationsFromExceptions(base2derivation, normInput);

    // compute derivations from morphology rules
    addMorphologicalDerivations(base2derivation, normInput);

    return base2derivation.values();
  }

  final Map<String, Derivation> addDerivationsFromExceptions(Map<String, Derivation> base2derivation, String normInput) {
    if (base2derivation == null) base2derivation = new HashMap<String, Derivation>();

    // add derivations from exceptions lists
    final List<BaseForm> exceptionsList = exceptions.get(normInput);
    if (exceptionsList != null) {
      for (BaseForm baseForm : exceptionsList) {
        addDerivation(base2derivation, normInput, baseForm.base, "", baseForm.posBit);
      }
    }    

    return base2derivation;
  }

  final Map<String, Derivation> addMorphologicalDerivations(Map<String, Derivation> base2derivation, String normInput) {
    if (base2derivation == null) base2derivation = new HashMap<String, Derivation>();

    // compute derivations from morphology rules
    final int len = normInput.length();
    char lm0 = (char)0;  // last minus 0
    char lm1 = (char)0;  // last minus 1
    char lm2 = (char)0;  // last minus 2

    if (len > 1) {
      lm0 = normInput.charAt(len - 1);

      // apply all suffix changes of length 1: "s"->""(noun,verb)
      if (lm0 == 's') {
        final String stem = normInput.substring(0, len - 1);
        addDerivation(base2derivation, normInput, stem, "s", NOUN_BIT | VERB_BIT);
      }
    }

    if (len > 2) {
      lm1 = normInput.charAt(len - 2);

      // apply all suffix changes of length 2:
      //   "es"->"e"(verb), "es"->""(verb), "ed"->"e"(verb), "ed"->""(verb), "er"->""(adj), "er"->"e"(adj)
      if (lm1 == 'e') {
        if (lm0 == 's' || lm0 == 'd' || lm0 == 'r') {
          final String stem = normInput.substring(0, len - 2);
          final String suffix = normInput.substring(len - 2);
          final int pos = (lm0 == 'r') ? ADJ_BIT : VERB_BIT;
          addDerivation(base2derivation, normInput, stem, suffix, pos);
          addDerivation(base2derivation, normInput, stem + "e", suffix, pos);
        }
      }
    }

    if (len > 3) {
      lm2 = normInput.charAt(len - 3);

      // apply all suffix changes of length 3 and 4:
      //   "ses"->"s"(noun), "xes"-"x"(noun), "zes"->"z"(noun), "ies"->"y"(noun,verb), "ches"->"ch"(noun), "shes"->"sh"(noun)
      if (lm1 == 'e' && lm0 == 's') { // "es"
        if (lm2 == 's' || lm2 == 'x' || lm2 == 'z') {  // "ses", "xes", "zes"
          final String suffix = normInput.substring(len - 3);
          final String stem = normInput.substring(0, len - 2);  // actually, this is the base form
          addDerivation(base2derivation, normInput, stem, suffix, NOUN_BIT);
        }
        else if (lm2 == 'i') {  // "ies"->"y"(noun,verb)
          final String suffix = normInput.substring(len - 3);
          final String stem = normInput.substring(0, len - 3);
          addDerivation(base2derivation, normInput, stem + "y", suffix, NOUN_BIT | VERB_BIT);
        }
        else if (lm2 == 'h' && len > 4) {  // "hes"
          final char lm3 = normInput.charAt(len - 4);
          // apply all suffix changes of length 4:
          //  "ches"->"ch"(noun), "shes"->"sh"(noun)

          if (lm3 == 'c' || lm3 == 's') {  // "ches", "shes"
            final String suffix = normInput.substring(len - 4);
            final String stem = normInput.substring(0, len - 2);  // actually, this is the base form
            addDerivation(base2derivation, normInput, stem, suffix, NOUN_BIT);
          }
        }
      }

      //   "ing"->""(verb), "ing"->"e"(verb)
      else if (lm0 == 'g' && lm1 == 'n' && lm2 == 'i') {  // "ing"
        final String suffix = normInput.substring(len - 3);
        final String stem = normInput.substring(0, len - 3);
        addDerivation(base2derivation, normInput, stem, suffix, VERB_BIT);
        addDerivation(base2derivation, normInput, stem + "e", suffix, VERB_BIT);
      }

      //   "est"->""(adj), "est"->"e"(adj)
      else if (lm0 == 't' && lm1 == 's' && lm2 == 'e') {  // "est"
        final String suffix = normInput.substring(len - 3);
        final String stem = normInput.substring(0, len - 3);
        addDerivation(base2derivation, normInput, stem, suffix, ADJ_BIT);
        addDerivation(base2derivation, normInput, stem + "e", suffix, ADJ_BIT);
      }

      //   "men"->"man"(noun)
      else if (lm0 == 'n' && lm1 == 'e' && lm2 == 'm') { // "men"
        final String suffix = normInput.substring(len - 3);
        final String stem = normInput.substring(0, len - 3);
        addDerivation(base2derivation, normInput, stem + "man", suffix, NOUN_BIT);
      }
    }

    return base2derivation;
  }

  private final Derivation addDerivation(Map<String, Derivation> base2derivation, String normInput, String base, String suffix, int posMask) {
    Derivation derivation = base2derivation.get(base);
    if (derivation == null) {
      derivation = new Derivation(normInput, base);
      base2derivation.put(base, derivation);
    }
    derivation.updateWith(posMask, suffix);
    return derivation;
  }

  private final void loadExceptions() throws IOException {
    final File[] exceptionFiles = this.dictDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".exc");
        }
      });

    if (exceptionFiles != null) {
      for (File exceptionFile : exceptionFiles) {
        BufferedReader reader = null;
        try {
          reader = FileUtil.getReader(exceptionFile);
          final int posBit = getPosBit(exceptionFile);
          if (posBit == 0) {
            continue;
          }
          String line = null;
          while ((line = reader.readLine()) != null) {
            if (!"".equals(line)) {
              final String[] pieces = line.split(" +");
              if (pieces.length == 2) {
                for (int i = 0; i < pieces.length; ++i) {
                  pieces[i] = NormalizeUtil.normalizeForLookup(pieces[i]);
                }
                List<BaseForm> baseForms = exceptions.get(pieces[0]);
                if (baseForms == null) {
                  baseForms = new ArrayList<BaseForm>();
                  exceptions.put(pieces[0], baseForms);
                }
                baseForms.add(new BaseForm(posBit, pieces[1]));
              }
            }
          }
        }
        finally {
          if (reader != null) reader.close();
        }
      }
    }
  }


  public static final class BaseForm {
    public int posBit;
    public String base;

    public BaseForm(int posBit, String base) {
      this.posBit = posBit;
      this.base = base;
    }
  }

  public static final class Derivation {
    public final String original;
    public final String baseForm;
    private int posMask;
    private Set<String> suffixes;

    public Derivation(String original, String baseForm) {
      this.original = original;
      this.posMask = 0;
      this.baseForm = baseForm;
      this.suffixes = new LinkedHashSet<String>();
    }

    public void updateWith(int pos, String suffix) {
      this.posMask |= pos;
      this.suffixes.add(suffix);
    }

    public boolean matchesPOS(String posName) {
      boolean result = false;

      final int posBit = findPosBit(posName);
      if (posBit > 0) {
        result = ((posMask & posBit) == posBit);
      }

      return result;
    }
  }
}
