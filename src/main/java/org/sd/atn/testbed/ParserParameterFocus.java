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
