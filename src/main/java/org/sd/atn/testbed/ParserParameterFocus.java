/*
   Copyright 2008-2015 Semantic Discovery, Inc.

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
package org.sd.atn.testbed;


import org.sd.util.tree.Tree;

/**
 * Container class for a single parameter focus (or ParameterManager paramTree node).
 * <p>
 * @author Spence Koehler
 */
public class ParserParameterFocus {
  
  private Tree<ParserParameterContainer> focus;

  private String breadCrumbDelim;
  private String _breadcrumb;

  public ParserParameterFocus(Tree<ParserParameterContainer> focus) {
    this(focus, null);
  }

  public ParserParameterFocus(Tree<ParserParameterContainer> focus, String breadCrumbDelim) {
    this.focus = focus;
    this.breadCrumbDelim = breadCrumbDelim == null ? "." : breadCrumbDelim;
    this._breadcrumb = null;
  }

  public Tree<ParserParameterContainer> getFocus() {
    return focus;
  }

  public String getLabel() {
    return focus.getData().getLabel();
  }

  public String getBreadCrumbDelim() {
    return breadCrumbDelim;
  }

  public void setBreadCrumbDelim(String breadCrumbDelim) {
    this.breadCrumbDelim = breadCrumbDelim;
    this._breadcrumb = null;
  }

  public boolean hasParent() {
    return (focus.getParent() != null);
  }

  public boolean hasChildren() {
    return focus.hasChildren();
  }

  public String getBreadCrumb() {
    if (_breadcrumb == null) {
      _breadcrumb = focus.buildPathString(false, breadCrumbDelim);
    }
    return _breadcrumb;
  }
}
