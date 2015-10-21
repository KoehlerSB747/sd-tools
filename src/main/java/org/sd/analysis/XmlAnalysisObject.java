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
package org.sd.analysis;


import java.util.ArrayList;
import java.util.List;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlStringBuilder;
import org.sd.xml.XmlTreeHelper;

/**
 * An analysis object that holds a record set.
 * <p>
 * @author Spencer Koehler
 */
public class XmlAnalysisObject extends AbstractAnalysisObject {
  
  private DomElement xml;
  private XPathApplicator xpathApplicator;
  private Tree<XmlLite.Data> xmlTree;

  public XmlAnalysisObject(String xmlString) {
    super();
    this.xml = new XmlStringBuilder().setXmlString(xmlString).getXmlElement();
    this.xpathApplicator = new XPathApplicator();
    this.xmlTree = xml.asTree();
  }

  public XmlAnalysisObject(DomElement xml) {
    super();
    this.xml = xml;
    this.xpathApplicator = new XPathApplicator();
    this.xmlTree = (xml == null) ? null : xml.asTree();
  }

  public DomElement getXml() {
    return xml;
  }

  /** Get a short/summary string representation of this object's data. */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("#Xml.");

    if (xml == null) {
      result.append("NULL");
    }
    else { // xml != null
      result.append(xml.getNodeName());
    }

    return result.toString();
  }

  /** Customization for "help" access. */
  @Override
  protected String getHelpString() {
    final StringBuilder result = new StringBuilder();
    result.
      append("\"show\" -- get the (pretty) xml data as a string.\n").
      append("\"text\" -- get all text under this xml node.\n").
      append("\"nodetext\" -- get just the text directly under this xml node.\n").
      append("\"compareTo[\"$other\"]\" -- compare to other xml object.\n").
      append("@attribute -- get attribute's value from this xml node.\n").
      append("xpath -- slash-delimitted xpath node(s) selector yielding an xml vector.");
      
    return result.toString();
  }

  /** Customization for "show" access. */
  @Override
  protected String getShowString() {
    return (xml != null) ? xml.asPrettyString(null, 0, 2).toString() : "NULL";
  }

  /**
   * Access components of this object according to ref.
   * <ul>
   * <li>"show" -- get the (pretty) xml data as a string</li>
   * <li>"text" -- get all text under this xml node</li>
   * <li>"nodetext" -- get just the text directly under this xml node</li>
   * <li>"compareTo["$other"]" --compare to other xml object</li>
   * <li>@attribute -- get attribute's value from this xml node</li>
   * <li>xpath -- slash-delimitted xpath node(s) selector yielding an xml vector</li>
   * </ul>
   */
  @Override
  protected AnalysisObject doAccess(String ref, EvaluatorEnvironment env) {
    AnalysisObject result = null;

    if (xml != null && ref != null && !"".equals(ref)) {
      if ("text".equals(ref)) {
        result = new BasicAnalysisObject<String>(XmlTreeHelper.getAllText(xmlTree));
      }
      else if ("nodetext".equals(ref)) {
        result = new BasicAnalysisObject<String>(XmlTreeHelper.getText(xmlTree));
      }
      else if (ref.charAt(0) == '@') {
        final String attributeValue = XmlTreeHelper.getAttribute(xmlTree, ref.substring(1));
        if (attributeValue != null) {
          result = new BasicAnalysisObject<String>(attributeValue);
        }
      }
      else if (ref.startsWith("compareTo")) {
        final AnalysisObject[] args = getArgValues(ref, env);
        if (args != null && args.length == 1) {
          result = compareTo(args[0]);
        }
      }
      else {
        // convert slashes to dots
        final String xpath = ref.replaceAll("/", ".");
        final List<Tree<XmlLite.Data>> nodes = xpathApplicator.getNodes(xpath, xmlTree);
        if (nodes != null) {
          final List<XmlAnalysisObject> values = new ArrayList<XmlAnalysisObject>();
          for (Tree<XmlLite.Data> node : nodes) {
            final DomElement elt = node.getData().asDomNode().asDomElement();
            if (elt != null) {
              values.add(new XmlAnalysisObject(elt));
            }
          }
          result = new VectorAnalysisObject<XmlAnalysisObject>(ref, values);
        }
      }
    }

    return result;
  }

  /** Get a numeric object representing this instance's value if applicable, or null. */
  @Override
  public NumericAnalysisObject asNumericAnalysisObject() {
    return null;
  }

  public ComparisonAnalysisObject compareTo(AnalysisObject other) {
    int compare = 0;
    final List<String> diffs = new ArrayList<String>();

    if (other == null) {
      compare = -1;
      diffs.add("2 is NULL");
    }
    else if (other instanceof XmlAnalysisObject) {
      final XmlAnalysisObject otherXAO = (XmlAnalysisObject)other;

      if (otherXAO.xml == null) {
        if (this.xml != null) {
          // other is null
          compare = -1;
          diffs.add("2 NULL-XML");
        }
      }
      else if (this.xml == null) {
        if (otherXAO.xml != null) {
          // this is null
          compare = 1;
          diffs.add("1 NULL-XML");
        }
      }
      else {
        // both are non-null
        if (!this.xml.matches(otherXAO.xml)) {
          // but don't match. ...todo: need to find diffs and determine compare's value
          compare = -1;
          diffs.add("Non-match");
        }
      }
    }
    else {
      // other isn't xml
      compare = -1;
      diffs.add("2 non-XML");
    }
    

    return new ComparisonAnalysisObject(this, other, compare, diffs.toArray(new String[diffs.size()]));
  }
}
