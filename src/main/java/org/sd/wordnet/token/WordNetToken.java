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
package org.sd.wordnet.token;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.token.Token;
import org.sd.wordnet.lex.Synset;

/**
 * Container for a token matched against word net.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetToken {
  
  private String input;
  private String norm;
  private List<Synset> synsets;
  private Set<String> tags;
  private Token token;
  private Set<String> _categories;

  public WordNetToken() {
  }

  public boolean hasInput() {
    return input != null && !"".equals(input);
  }

  public String getInput() {
    return input;
  }

  public WordNetToken setInput(String input) {
    this.input = input;
    return this;
  }

  public boolean hasNorm() {
    return norm != null && !"".equals(norm);
  }

  public String getNorm() {
    return norm;
  }

  public WordNetToken setNorm(String norm) {
    this.norm = norm;
    return this;
  }

  public boolean hasSynsets() {
    return synsets != null && synsets.size() > 0;
  }

  public List<Synset> getSynsets() {
    return synsets;
  }

  public WordNetToken setSynsets(List<Synset> synsets) {
    this.synsets = synsets;
    this._categories = null;
    return this;
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

  public WordNetToken addTag(String tag) {
    if (tag != null) {
      if (tags == null) tags = new HashSet<String>();
      tags.add(tag);
      this._categories = null;
    }
    return this;
  }

  public boolean hasToken() {
    return token != null;
  }

  public Token getToken() {
    return token;
  }

  public WordNetToken setToken(Token token) {
    this.token = token;
    return this;
  }

  public boolean isUnknown() {
    return !hasSynsets() && !hasTags();
  }

  public boolean hasCategories() {
    return _categories != null || hasSynsets() || hasTags();
  }

  public Set<String> getCategories() {
    if (_categories == null && (hasSynsets() || hasTags())) {
      _categories = new HashSet<String>();
      if (hasSynsets()) {
        for (Synset synset : synsets) {
          final String lfm = synset.getLexFileName();
          final int dotPos = lfm.indexOf('.');
          _categories.add(dotPos > 0 ? lfm.substring(0, dotPos) : lfm);
        }
      }
      if (hasTags()) {
        _categories.addAll(tags);
      }
    }
    return _categories;
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
