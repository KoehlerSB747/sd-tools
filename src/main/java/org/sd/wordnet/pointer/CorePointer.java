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
package org.sd.wordnet.pointer;


/**
 * Enumeration of core pointers.
 * <p>
 * @author Spencer Koehler
 */
public enum CorePointer {


  ANTONYM(),
  HYPONYM("HYPERNYM"),
  HYPERNYM("HYPONYM"),
  INSTANCE_HYPONYM("INSTANCE_HYPERNYM"),
  INSTANCE_HYPERNYM("INSTANCE_HYPONYM"),
  HOLONYM("MERONYM"),
  MERONYM("HOLONYM"),
  SIMILAR_TO(),
  ATTRIBUTE(),
  VERB_GROUP(),
  DERIVATIONALLY_RELATED(),
  DOMAIN_OF_SYNSET("MEMBER_OF_DOMAIN"),
  MEMBER_OF_DOMAIN("DOMAIN_OF_SYNSET");

  
  private String reflectName;
  private CorePointer _reflect;

  CorePointer() {
    this.reflectName = this.name();
    this._reflect = this;
  }

  CorePointer(String reflectName) {
    this.reflectName = reflectName;
    this._reflect = null;
  }

  public CorePointer reflect() {
    if (_reflect == null) {
      this._reflect = CorePointer.valueOf(reflectName);
    }
    return _reflect;
  }
}
