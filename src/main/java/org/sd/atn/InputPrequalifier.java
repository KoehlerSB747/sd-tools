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
package org.sd.atn;


import java.util.List;
import org.sd.atn.ResourceManager;
import org.sd.token.Tokenizer;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A general AtnParsePrequalifier implementation.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.AtnParsePrequalifier implementation that\n" +
       "qualifies input for submission to a parser.\n" +
       "Definition format is of the form:\n" +
       "\n" +
       "  <prequalifier>\n" +
       "    <jclass>org.sd.atn.InputPrequalifier</jclass>\n" +
       "    <regexes>\n" +
       "      <regex type='find'>:</regex>\n" +
       "    </regexes>\n" +
       "      <wordPattern keyLabels='' labelChars='' squashFlags=''>\n" +
       "        <regexes>\n" +
       "          <regex type='find'>...</regex>\n" +
       "        </regexes>\n" +
       "      </wordPattern>\n" +
       "  </prequalifier>"
  )
public class InputPrequalifier implements AtnParsePrequalifier {
  
  private boolean verbose;
  private RegexDataContainer regexes;
  private TokenWordPatternData wordPatternData;

  //
  //  <prequalifier>
  //    <jclass>org.sd.atn.InputPrequalifier</jclass>
  //    <regexes>
  //      <regex type='find'>:</regex>
  //    </regexes>
  //    <wordPattern keyLabels="" labelChars="" squashFlags="">
  //      <regexes>
  //        <regex type='find'>...</regex>
  //      </regexes>
  //    </wordPattern>
  //  </prequalifier>
  //

  public InputPrequalifier(DomNode domNode, ResourceManager resourceManager) {
    final DomElement domElement = (DomElement)domNode;

    this.verbose = domElement.getAttributeBoolean("verbose", false);
    this.regexes = null;
    this.wordPatternData = null;

    final NodeList childNodes = domElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNodeNum = 0; childNodeNum < numChildNodes; ++childNodeNum) {
      final Node childNode = childNodes.item(childNodeNum);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

      final DomElement childElement = (DomElement)childNode;
      final String childNodeName = childNode.getLocalName();

      if ("regexes".equalsIgnoreCase(childNodeName)) {
        this.regexes = new RegexDataContainer(childElement);
      }
      else if ("wordPattern".equals(childNodeName)) {
        this.wordPatternData = new TokenWordPatternData(childElement);
      }
    }
  }

  public RegexDataContainer getRegexes() {
    return regexes;
  }

  public TokenWordPatternData getWordPatternData() {
    return wordPatternData;
  }

  public boolean prequalify(Tokenizer tokenizer) {
    boolean result = false;

    final String input = tokenizer.getText();

    if (!result && regexes != null) {
      if (regexes.matches(input) != null) {
        result = true;

        if (verbose) {
          System.out.println("InputPrequalifier regexes matched '" + input + "'");
        }
      }
      else {
        if (verbose) {
          System.out.println("InputPrequalifier regexes don't match '" + input + "'");
        }
      }
    }

    if (!result && wordPatternData != null) {
      result = wordPatternData.prequalify(tokenizer);
    }

    if (verbose) {
      System.out.println("InputPrequalifier " + (result ? "allowing" : "disallowing") + " '" + input + "'");
    }

    return result;
  }
}
