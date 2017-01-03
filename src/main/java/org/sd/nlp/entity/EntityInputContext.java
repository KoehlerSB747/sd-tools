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
import java.util.List;
import java.util.Map;
import org.sd.atn.ParseInputContext;
import org.sd.token.SimpleTokenInfo;
import org.sd.token.TokenInfo;
import org.sd.token.TokenInfoContainer;
import org.sd.util.InputContext;

/**
 * A ParseInputContext derived from extracted entities.
 * <p>
 * @author Spencer Koehler
 */
public class EntityInputContext implements ParseInputContext {
  
  private long lineNum;
  private EntityContainer entityContainer;
  private List<TokenInfo> _tokenInfos;
  private List<TokenInfo> _hardBreaks;

  public EntityInputContext(long lineNum, EntityContainer entityContainer) {
    this.lineNum = lineNum;
    this.entityContainer = entityContainer;
    this._tokenInfos = null;
    this._hardBreaks = null;
  }

  /**
   * Get this context's text.
   */
  @Override
  public String getText() {
    return entityContainer.getBaseLine();
  }

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  @Override
  public long getId() {
    return lineNum;
  }

  /**
   * Get the character startPosition of the other context's text within
   * this context or return false if the other context is not contained
   * within this context.
   *
   * @param other  The other input context
   * @param startPosition a single element array holding the return value
   *        of the start position -- only set when returning 'true'.
   *
   * @return true and startPosition[0] holds the value or false.
   */
  @Override
  public boolean getPosition(InputContext other, int[] startPosition) {
    boolean result = false;

    if (other == this) {
      result = true;
      startPosition[0] = 0;
    }

    return result;
  }

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  @Override
  public InputContext getContextRoot() {
    return this;  //todo: refer to context of e.g., page from which this context's line came
  }

  /**
   * Get the tokenInfo instances for building an AtnParseBasedTokenizer for
   * this input, if available.
   *
   * @return the tokenInfo instances or null.
   */
  @Override
  public List<TokenInfo> getTokenInfos() {
    if (_tokenInfos == null) {
      _tokenInfos = buildTokenInfos();
    }
    return _tokenInfos;
  }

  private final List<TokenInfo> buildTokenInfos() {
    final TokenInfoContainer<TokenInfo> container = new TokenInfoContainer<TokenInfo>();

    // final List<TokenInfo> result = new ArrayList<TokenInfo>();

    // Create an instance for each entity
    for (Map.Entry<String, List<Entity>> entityEntry : entityContainer.getEntities().entrySet()) {
      final String type = entityEntry.getKey();
      final List<Entity> entities = entityEntry.getValue();
      for (Entity entity : entities) {
        final int startPos = getStartPos(entity);
        final int endPos = getEndPos(entity);
        final int priority = entity.getPriority();
        if (startPos >= 0 && endPos >= 0) {
          final TokenInfo tokenInfo = new SimpleTokenInfo(startPos, endPos, priority, type, entity.getAttributes());
          container.add(tokenInfo, 0);
        }
      }
    }

    return container.getAll();
  }

  protected int getStartPos(Entity entity) {
    int result = entity.getStartPos();

    // convert startPos to entityContainer's baseLine
    result = entity.getRefPosition(entityContainer.getBaseLine(), result, false);

    return result;
  }

  protected int getEndPos(Entity entity) {
    int result = entity.getEndPos();

    // convert endPos to entityContainer's baseLine
    result = entity.getRefPosition(entityContainer.getBaseLine(), result, true);

    return result;
  }

  /**
   * Get the spans of tokens that are designated as hard breaks.
   */
  @Override
  public List<TokenInfo> getHardBreaks() {
    if (_hardBreaks == null) {
      _hardBreaks = buildHardBreaks();
    }
    return _hardBreaks;
  }

  private final List<TokenInfo> buildHardBreaks() {
    List<TokenInfo> result = null;

//todo: implement this... consider breaks before and after entities as hard?

    return result;
  }
}
