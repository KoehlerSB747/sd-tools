/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Container for organizing TokenInfo instances by their endPos.
 * <p>
 * @author Spence Koehler
 */
public class TokenInfoContainer<T extends TokenInfo> {
  
  private TreeMap<Integer, List<T>> tokenInfoList;
  public TreeMap<Integer, List<T>> getTokenInfoList() {
    return tokenInfoList;
  }

  public TokenInfoContainer() {
    this.tokenInfoList = new TreeMap<Integer, List<T>>();
  }

  public List<T> getAll() {
    final List<T> result = new ArrayList<T>();
    for (List<T> tokenInfos : tokenInfoList.values()) {
      result.addAll(tokenInfos);
    }
    return result;
  }

  public void add(T tokenInfo, int offset) {
    boolean doAdd = true;

    if (tokenInfo.hasPriority()) {
      final List<T> overlaps = getPrioritizedOverlapsWith(tokenInfo, offset);
      if (overlaps != null && overlaps.size() > 0) {
        doAdd = false;
        int keepVotes = 0;
        int discardVotes = 0;
        List<T> overlapsToRemove = null;

        for (T overlap : overlaps) {
          if (overlap.getPriority() > tokenInfo.getPriority()) {
            // remove overlap, keep tokenInfo
            if (overlapsToRemove == null) overlapsToRemove = new ArrayList<T>();
            overlapsToRemove.add(overlap);
            ++keepVotes;
          }
          else if (overlap.getPriority() < tokenInfo.getPriority()) {
            // keep overlap, discard tokenInfo
            ++discardVotes;
          }
          else {   // equal priority ==> keep both
            // keep overlap, keep tokenInfo
            ++keepVotes;
          }
        }

        // make add decision to keep or discard new tokenInfo
        if (keepVotes > 0 && discardVotes > 0) {
          // new tokenInfo conflicts with currently prioritized, don't add
          doAdd = false;
        }
        else if (keepVotes > 0) {
          doAdd = true;

          if (overlapsToRemove != null) {
            for (T overlap : overlapsToRemove) {
              remove(overlap);
            }
          }
        }
      }
    }

    if (doAdd) {
      doAdd(tokenInfo, offset);
    }
  }

  private final void doAdd(T tokenInfo, int offset) {
    final int endIndex = tokenInfo.updateEndIndex(offset);
    List<T> tokenInfos = tokenInfoList.get(endIndex);
    if (tokenInfos == null) {
      tokenInfos = new ArrayList<T>();
      tokenInfoList.put(endIndex, tokenInfos);
    }
    tokenInfos.add(tokenInfo);
  }

  private final void remove(T tokenInfo) {
    final int endIndex = tokenInfo.getEndIndex();
    final List<T> tokenInfos = tokenInfoList.get(endIndex);
    if (tokenInfos != null) {
      tokenInfos.remove(tokenInfo);
      if (tokenInfos.size() == 0) {
        tokenInfoList.remove(endIndex);
      }
    }
  }

  private final List<T> getPrioritizedOverlapsWith(T tokenInfo, int offset) {
    List<T> result = null;

    final int startIndex = tokenInfo.getTokenStart() + offset;
    final int endIndex = tokenInfo.getTokenEnd() + offset;

    // for those that end after tokenInfo's start
    for (Map.Entry<Integer, List<T>> entry = tokenInfoList.ceilingEntry(endIndex);
         entry != null;
         entry = tokenInfoList.higherEntry(entry.getKey())) {
      final List<T> tokenInfos = entry.getValue();
      for (T curTokenInfo : tokenInfos) {
        if (!curTokenInfo.hasPriority()) continue;
        final int curStartIndex = curTokenInfo.getStartIndex();
        if (curStartIndex >= endIndex) continue;
        final int curEndIndex = curTokenInfo.getEndIndex();
        if ((curStartIndex >= startIndex && curStartIndex < endIndex) ||
            (curEndIndex >= startIndex && curEndIndex < endIndex)) {
          if (result == null) result = new ArrayList<T>();
          result.add(curTokenInfo);
        }
      }
    }

    return result;
  }

  public T getFirst(int endPos) {
    T result = null;

    final List<T> tokenInfos = tokenInfoList.get(endPos);
    if (tokenInfos != null && tokenInfos.size() > 0) {
      result = tokenInfos.get(0);
    }

    return result;
  }

  public List<T> getAll(int endPos) {
    return tokenInfoList.get(endPos);
  }

  public void adjustEnd(int curEnd, int updatedEnd) {
    final List<T> tokenInfos = tokenInfoList.get(curEnd);
    if (tokenInfos != null) {
      for (T tokenInfo : tokenInfos) {
        tokenInfo.setTokenEnd(updatedEnd);
      }
      tokenInfoList.remove(curEnd);
      tokenInfoList.put(updatedEnd, tokenInfos);
    }
  }
}
