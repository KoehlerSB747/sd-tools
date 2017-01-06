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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlStringBuilder;
import org.w3c.dom.NodeList;

/**
 * Container for entities in a line of text, organized by type.
 * <p>
 * @author Spencer Koehler
 */
public class EntityContainer {
  
  private Map<String, List<Entity>> entities;  // by type
  private Map<String, List<Entity>> line2entities;  // by input line text
  private String _baseLine;

  public EntityContainer() {
    this.entities = new HashMap<String, List<Entity>>();
    this.line2entities = new HashMap<String, List<Entity>>();
    this._baseLine = null;
  }

  public int getNumTypes() {
    return entities.size();
  }

  public boolean hasType(String type) {
    return entities.containsKey(type);
  }

  public Map<String, List<Entity>> getEntities() {
    return entities;
  }

  public List<Entity> getEntities(String type) {
    return entities.get(type);
  }
  
  public List<Entity> add(long id, String entitiesString, EntityLineAligner aligner) {
    //assumptions: <container><entityName ...attributes...>entityText</entityName></container>
    final XmlStringBuilder xml = new XmlStringBuilder().setXmlString(entitiesString);
    final DomElement elt = xml.getXmlElement();
    return add(id, elt, aligner);
  }

  public List<Entity> add(long id, DomElement elt, EntityLineAligner aligner) {
    List<Entity> result = null;

    final NodeList childNodes = elt.getChildNodes();
    final int numChildNodes = childNodes == null ? 0 : childNodes.getLength();
    for (int childNum = 0; childNum < numChildNodes; ++childNum) {
      final DomNode child = (DomNode)childNodes.item(childNum);
      if (child.getNodeType() == DomNode.ELEMENT_NODE) {
        final Entity entity = new Entity(id, child.asDomElement(), aligner);
        this.addEntity(entity);
        entity.setEntityContainer(this);

        if (result == null) result = new ArrayList<Entity>();
        result.add(entity);
      }
    }

    return result;
  }

  public final void addEntity(Entity entity) {
    final String type = entity.getType();
    List<Entity> curEntities = entities.get(type);
    if (curEntities == null) {
      curEntities = new ArrayList<Entity>();
      entities.put(type, curEntities);
    }
    curEntities.add(entity);
    updateReferenceLines(entity);
  }

  private final void updateReferenceLines(Entity entity) {
    // this container's baseLine needs to be in every entity (either as baseLine or altLine)
    // while the altLine need not be in every entity

    final EntityLineAligner aligner = entity.getAligner();
    final String baseLine = aligner.getBaseLine();
    doAddLine(baseLine, entity);

    if (aligner.hasAltLine()) {
      doAddLine(aligner.getAltLine(), entity);
    }
  }

  private final void doAddLine(String line, Entity entity) {
    _baseLine = null;
    List<Entity> entities = line2entities.get(line);
    if (entities == null) {
      entities = new ArrayList<Entity>();
      line2entities.put(line, entities);
    }
    entities.add(entity);
  }

  /**
   * Get the form of the input line common to all entities in this container.
   */
  public final String getBaseLine() {
    if (_baseLine == null) {
      _baseLine = computeBaseLine();
    }
    return _baseLine;
  }

  private final String computeBaseLine() {
    // The baseLine of the container is the input line that all entities share

    String result = null;
    int maxCount = 0;
    for (Map.Entry<String, List<Entity>> entry : line2entities.entrySet()) {
      final String line = entry.getKey();
      final List<Entity> entities = entry.getValue();
      if (maxCount == 0 || entities.size() > maxCount) {
        maxCount = entities.size();
        result = line;
      }
    }

    //todo: assert that maxCount == total number of entities?
    //note: currently only 2 forms of the input lines exist.
    //  If more exist,
    //    then EntityLineAligner's model will need to accommodate multiple altLines
    //    and all altLines will need to be added when constructing line2entities
    //    and the possibility of two entities not sharing the same common lines as all entities will need to be accounted for

    return result;
  }
}
