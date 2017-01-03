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
package org.sd.nlp.entity;


import java.util.Map;
import org.sd.xml.DomElement;

/**
 * Container for an extracted entity.
 * <p>
 * @author Spencer Koehler
 */
public class Entity {
  
  private long id;
  private String type;
  private Map<String, String> attributes;
  private DomElement domElt;
  private String text;  //NOTE: just the entity text
  private EntityLineAligner aligner;
  private EntityContainer _entityContainer;
  private Integer _startPos;
  private Integer _endPos;
  private Integer _priority;

  public Entity(long id, DomElement domElt) {
    this(id, domElt, null);
  }

  public Entity(long id, DomElement domElt, EntityLineAligner aligner) {
    this.id = id;
    this.domElt = domElt;
    this.aligner = aligner;
    this.type = domElt.getNodeName();
    this.attributes = domElt.getDomAttributes().getAttributes();
    this.text = domElt.getTextContent();
    this._entityContainer = null;
    this._startPos = null;
    this._endPos = null;
    this._priority = null;

    if (aligner == null) {
      final String baseLine = domElt.getAttribute("baseLine");
      final String altLine = domElt.getAttribute("altLine");
      this.aligner = new EntityLineAligner(baseLine).setAltLine(altLine);
    }
    else {
      domElt.setAttribute("baseLine", aligner.getBaseLine());
      if (aligner.hasAltLine()) {
        domElt.setAttribute("altLine", aligner.getAltLine());
      }
    }

    if (!domElt.hasAttribute("id")) domElt.setAttribute("id", Long.toString(id));
  }

  public long getId() {
    return id;
  }

  public DomElement getDomElement() {
    return domElt;
  }      

  public String getType() {
    return type;
  }

  public boolean hasAttributes() {
    return attributes != null && attributes.size() > 0;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public boolean hasAttribute(String key) {
    return attributes != null && attributes.containsKey(key);
  }

  public String getAttribute(String key) {
    return attributes != null ? attributes.get(key) : null;
  }

  public String getText() {
    return text;
  }

  /** Set or override this entity's text. */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Get the start position of this entity's text (through the "startPos"
   * attribute)  within its base line; default to -1 if not available.
   */
  public int getStartPos() {
    if (_startPos == null) {
      _startPos = getStartPos("startPos", -1);
    }
    return _startPos;
  }

  /** Set or override the default startPos */
  public void setStartPos(int startPos) {
    this._startPos = startPos;
  }

  /**
   * Get the start position of this entity's text within its base line,
   * referencing the given startPosAttribute and defaulting to the given
   * defaultValue if not available.
   */
  public int getStartPos(String startPosAttribute, int defaultValue) {
    int result = defaultValue;

    if (domElt != null) {
      result = domElt.getAttributeInt(startPosAttribute, defaultValue);
    }

    return result;
  }

  /** Set or override the default endPos */
  public void setEndPos(int endPos) {
    this._endPos = endPos;
  }

  /**
   * Get the end position of this entity's text (through the "endPos"
   * attribute)  within its base line; default to -1 if not available.
   */
  public int getEndPos() {
    if (_endPos == null) {
      _endPos = getEndPos("endPos", -1);
    }
    return _endPos;
  }

  /**
   * Get the end position of this entity's text within its base line,
   * referencing the given endPosAttribute and defaulting to the given
   * defaultValue if not available.
   */
  public int getEndPos(String endPosAttribute, int defaultValue) {
    int result = defaultValue;

    if (domElt != null) {
      result = domElt.getAttributeInt(endPosAttribute, defaultValue);
    }

    return result;
  }

  /**
   * Get this entity's priority (where lower priority values are preferred over
   * higher priority values, but values lte 0 are ignored), referencing the
   * "priority" attribute and defaulting to -1 if not available.
   */
  public int getPriority() {
    if (_priority == null) {
      _priority = getPriority("priority", -1);
    }
    return _priority;
  }

  /** Set or override the default priority */
  public void setPriority(int priority) {
    this._priority = priority;
  }

  /**
   * Get this entity's priority (where lower priority values are preferred over
   * higher priority values, but values lte 0 are ignored), referencing the
   * given priorityAttribute and defaulting to -1 if not available.
   */
  public int getPriority(String priorityAttribute, int defaultValue) {
    int result = defaultValue;

    if (domElt != null) {
      result = domElt.getAttributeInt(priorityAttribute, defaultValue);
    }

    return result;
  }

  public EntityLineAligner getAligner() {
    return aligner;
  }

  public boolean hasEntityContainer() {
    return _entityContainer != null;
  }

  public EntityContainer getEntityContainer() {
    return _entityContainer;
  }

  public void setEntityContainer(EntityContainer entityContainer) {
    this._entityContainer = entityContainer;
  }

  public int getRefPosition(String refLine, int basePos, boolean endPos) {
    int result = -1;

    if (aligner != null) {
      if (refLine.equals(aligner.getBaseLine())) {
        result = basePos;
      }
      else if (aligner.hasAltLine() && refLine.equals(aligner.getAltLine())) {
        final int[] pos = aligner.getBasePos(basePos);
        result = pos[endPos ? 1 : 0];
      }
      else {
        final EntityLineAligner refAligner = new EntityLineAligner(refLine).setAltLine(this.aligner.getBaseLine());
        if (refAligner.aligns()) {
          final int[] refPos = refAligner.getBasePos(basePos);
          result = refPos[endPos ? 1 : 0];
        }
      }
    }

    return result;
  }
}
