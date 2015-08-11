/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.atn.interp;


import java.util.ArrayList;
import java.util.List;
import org.sd.atn.Parse;
import org.sd.atn.ParseInterpretation;
import org.sd.atn.ParseInterpretationUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XmlLite;

/**
 * NodeExtractor for Interp's.
 * <p>
 * @author Spence Koehler
 */
public class InterpNodeExtractor extends AbstractNodeExtractor {
  
  private enum SubType {
    TREE(true), TOSTRING(false), INPUTTEXT(false);

    private boolean useInterpTree;

    private SubType(boolean useInterpTree) {
      this.useInterpTree = useInterpTree;
    }

    boolean useInterpTree() { return useInterpTree; }
  };


  private String classification;
  private SubType subType;

  private String xpathText;
  private String[] xpathPatternAndAttribute;
  private boolean getAttribute;
  private XPathApplicator xpathApplicator;
  private String select;
  private boolean selectFirst;
  private boolean selectLast;

  InterpNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, DomElement extractElement, String classification) {
    super(fieldTemplate, resources);
    this.classification = classification;

    // subTypes: 'tree' (default), 'toString', 'inputText'
    final String subTypeText = extractElement.getAttributeValue("subType", "tree");
    this.subType = SubType.valueOf(subTypeText.toUpperCase());

    this.xpathText = null;
    this.xpathPatternAndAttribute = null;
    this.getAttribute = false;
    this.xpathApplicator = null;
    this.select = null;
    this.selectFirst = false;
    this.selectLast = false;

    if (subType.useInterpTree()) {
      this.xpathText = extractElement.getAttributeValue("xpath", null);
      if (xpathText != null) {
        this.xpathPatternAndAttribute = XPathApplicator.splitPatternAttribute(xpathText);
        this.getAttribute = (xpathPatternAndAttribute.length > 1);
        this.xpathApplicator = new XPathApplicator();
        this.select = extractElement.getAttributeValue("select", "first");

        this.selectFirst = "first".equals(select);
        this.selectLast = "last".equals(select);
      }
    }
  }

  public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode, DataProperties overrides, InterpretationController controller) {
    List<Tree<XmlLite.Data>> result = null;

    final List<ParseInterpretation> interps = ParseInterpretationUtil.getInterpretations(parseNode, classification);
    if (interps != null) {

//todo: apply a disambiguation function to the interps here (e.g. fix 2-digit years) using full context of parse

      result = new ArrayList<Tree<XmlLite.Data>>();
      for (ParseInterpretation interp : interps) {
        final List<Tree<XmlLite.Data>> content = getContent(interp);
        if (content != null) {
          result.addAll(content);
        }
      }
    }

    return cleanup(result, parse, parseNode, false);
  }

  public String extractString(Parse parse, Tree<String> parseNode) {
    return null;
  }

  private final List<Tree<XmlLite.Data>> getContent(ParseInterpretation interp) {
    List<Tree<XmlLite.Data>> result = null;
    Tree<XmlLite.Data> singleResult = null;

    if (subType.useInterpTree()) {
      singleResult = interp.getInterpTree();

      if (singleResult != null && xpathText != null) {
        if (getAttribute) {  // get the attribute value(s)
          final List<String> values =
            xpathApplicator.getXPath(xpathPatternAndAttribute[0]).getText(singleResult, xpathPatternAndAttribute[1], true, false);

          if (values != null && values.size() > 0) {
            if (selectFirst) {
              singleResult = buildSingleResult(xpathPatternAndAttribute[1], values.get(0), null);
            }
            else if (selectLast) {
              singleResult = buildSingleResult(xpathPatternAndAttribute[1], values.get(values.size() - 1), null);
            }
            else {
              result = new ArrayList<Tree<XmlLite.Data>>();
              for (String value : values) {
                result.add(buildSingleResult(xpathPatternAndAttribute[1], value, null));
              }
            }
          }
          else {
            singleResult = null;
          }
        }
        else {  // get interp node(s)
          final List<Tree<XmlLite.Data>> matches = xpathApplicator.getNodes(xpathText, singleResult);

          if (matches != null && matches.size() > 0) {
            if (selectFirst) {
              singleResult = matches.get(0);
            }
            else if (selectLast) {
              singleResult = matches.get(matches.size() - 1);
            }
            else {
              result = matches;
            }
          }
          else {
            singleResult = null;
          }
        }
      }
    }
    else {
      final String inputText = interp.getInputText();
      final String toString = interp.toString();

      switch (subType) {
        case TOSTRING :
          singleResult = buildSingleResult(interp.getClassification(), toString, inputText);
          break;
        case INPUTTEXT :
          singleResult = buildSingleResult(interp.getClassification(), inputText, toString);
          break;
      }
    }

    if (result == null && singleResult != null) {
      result = new ArrayList<Tree<XmlLite.Data>>();
      result.add(singleResult);
    }

    return result;
  }

  private final Tree<XmlLite.Data> buildSingleResult(String tag, String text, String altText) {
    final Tree<XmlLite.Data> result = XmlLite.createTagNode(tag);
    result.addChild(XmlLite.createTextNode(text));

    if (altText != null && !"".equals(altText)) {
      result.getData().asTag().setAttribute("altText", altText);
    }

    return result;
  }
}
