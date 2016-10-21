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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Container for synsets.
 * <p>
 * @author Spencer Koehler
 */
public class SynsetContainer {
  
  public interface ConstraintFunction {
    public boolean accept(Synset synset);
  }


  private String input;
  private String norm;
  private List<Synset> synsets;
  private Set<String> tags;          // externally added/managed tags, manual categories
  private Set<String> _categories;   // actual categories of current synsets

  public SynsetContainer() {
  }

  public boolean hasInput() {
    return input != null && !"".equals(input);
  }

  public String getInput() {
    return input;
  }

  public SynsetContainer setInput(String input) {
    this.input = input;
    return this;
  }

  public boolean isMultiWord() {
    return norm != null && norm.indexOf(' ') > 0;
  }

  public boolean hasNorm() {
    return norm != null && !"".equals(norm);
  }

  public String getNorm() {
    return norm;
  }

  public SynsetContainer setNorm(String norm) {
    this.norm = norm;
    return this;
  }

  public boolean hasSynsets() {
    return synsets != null && synsets.size() > 0;
  }

  public List<Synset> getSynsets() {
    return new ArrayList<Synset>(synsets);
  }

  public SynsetContainer setSynsets(List<Synset> synsets) {
    this.synsets = (synsets == null) ? null : new ArrayList<Synset>(synsets);
    this._categories = null;
    return this;
  }

  public String getSynsetNames() {
    // get a comma-delimited list of synset names, or empty string
    final StringBuilder result = new StringBuilder();

    if (hasSynsets()) {
      for (Synset synset : synsets) {
        if (result.length() > 0) result.append(',');
        result.append(synset.getSynsetName());
      }
    }

    return result.toString();
  }

  public boolean hasTags() {
    return tags != null && tags.size() > 0;
  }

  public boolean hasTag(String tag) {
    return tags != null && tags.contains(tag);
  }

  public Set<String> getTags() {
    return tags;
  }

  public String getTagNames() {
    // get a comma-delimited list of tag names, or empty string
    final StringBuilder result = new StringBuilder();

    if (hasTags()) {
      for (String tag : tags) {
        if (result.length() > 0) result.append(',');
        result.append(tag);
      }
    }

    return result.toString();
  }

  public SynsetContainer addTag(String tag) {
    if (tag != null) {
      if (tags == null) tags = new HashSet<String>();
      tags.add(tag);
      this._categories = null;
    }
    return this;
  }

  public boolean isUnknown() {
    return !hasSynsets() && !hasTags();
  }

  public boolean hasCategories() {
    return _categories != null || hasSynsets() || hasTags();
  }

  public Set<String> getCategories() {
    if (_categories == null) {
      _categories = new HashSet<String>();
      if (hasSynsets() || hasTags()) {
        if (hasSynsets()) {
          for (Synset synset : synsets) {
            final String[] lfmPieces = synset.getLexFileName().split("\\.");
            for (String lfmPiece : lfmPieces) {
              _categories.add(lfmPiece);
            }
          }
        }
        if (hasTags()) {
          _categories.addAll(tags);
        }
      }
      else {
        _categories.add("unknown");
      }
    }
    return _categories;
  }

  public int constrain(ConstraintFunction constraintFunction) {
    int result = 0;

    if (synsets != null) {
      for (Iterator<Synset> iter = synsets.iterator(); iter.hasNext(); ) {
        final Synset synset = iter.next();
        if (!constraintFunction.accept(synset)) {
          iter.remove();          
          ++result;
        }
      }

      if (result > 0) _categories = null;
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    // "word"{defs/tags}
    result.append('"');
    if (hasSynsets()) result.append(norm);
    else result.append(input);
    result.append('"');

    if (hasCategories()) {
      result.append('{');
      boolean didOne = false;
      for (String category : getCategories()) {
        if (didOne) {
          result.append(',');
        }
        else {
          didOne = true;
        }
        result.append(category);
      }
      result.append('}');
    }

    return result.toString();
  }
}
