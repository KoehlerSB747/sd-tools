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
package org.sd.util;


/**
 * Class to track processing and/or access times and rates for some function.
 * <p>
 * @author Spencer Koehler
 */
public class Monitor {
  
  private long aliveSince;
  private String description;
  private long lastAccessTime;
  private RollingStats accessTimes;
  private RollingStats processingTimes;
  private int defaultWindowWidth;
  private int defaultSegmentWidth;

  private final Object markMutex = new Object();

  public Monitor(String description) {
    this.aliveSince = System.currentTimeMillis();
    this.description = description;
    this.lastAccessTime = 0L;
    this.accessTimes = null;
    this.processingTimes = null;
    this.defaultWindowWidth = RollingStats.DEFAULT_WINDOW_WIDTH;
    this.defaultSegmentWidth = RollingStats.DEFAULT_SEGMENT_WIDTH;
  }

  public void setAliveSince(long aliveSince) {
    this.aliveSince = aliveSince;
  }

  public long getAliveSince() {
    return this.aliveSince;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }
  
  public void setLastAccessTime(long lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }

  public long getLastAccessTime() {
    return this.lastAccessTime;
  }
  
  public void setAccessTimes(RollingStats accessTimes) {
    this.accessTimes = accessTimes;
  }

  public RollingStats getAccessTimes() {
    return this.accessTimes;
  }
  
  public void setProcessingTimes(RollingStats processingTimes) {
    this.processingTimes = processingTimes;
  }

  public RollingStats getProcessingTimes() {
    return this.processingTimes;
  }
  
  public void setDefaultWindowWidth(int defaultWindowWidth) {
    this.defaultWindowWidth = defaultWindowWidth;
  }

  public int getDefaultWindowWidth() {
    return this.defaultWindowWidth;
  }
  
  public void setDefaultSegmentWidth(int defaultSegmentWidth) {
    this.defaultSegmentWidth = defaultSegmentWidth;
  }

  public int getDefaultSegmentWidth() {
    return this.defaultSegmentWidth;
  }
  

  public void mark(long starttime) {
    this.mark(starttime, 0L);
  }

  public void mark(long starttime, long endtime) {
    synchronized (this.markMutex) {
      if (this.lastAccessTime > 0) {
        if (this.accessTimes == null) {
          this.accessTimes = new RollingStats(this.defaultWindowWidth, this.defaultSegmentWidth);
        }
        this.accessTimes.add(starttime - this.lastAccessTime);
      }

      if (endtime > 0) {
        if (this.processingTimes == null) {
          this.processingTimes = new RollingStats(this.defaultWindowWidth, this.defaultSegmentWidth);
        }
        this.processingTimes.add(endtime - starttime);
      }

      this.lastAccessTime = starttime;
    }
  }

  public StatsAccumulator getStats(boolean access, boolean window) {
    // Get window/cumulative access/processing stats
    StatsAccumulator result = null;

    final RollingStats rollingStats = access ? this.accessTimes : this.processingTimes;
    if (rollingStats != null) {
      result = window ? rollingStats.getWindowStats() : rollingStats.getCumulativeStats();
    }

    return result;
  }

  public StatsAccumulator getAccessWindowStats() {
    return getStats(true, true);
  }

  public StatsAccumulator getAccessCumulativeStats() {
    return getStats(true, false);
  }

  public StatsAccumulator getProcessingWindowStats() {
    return getStats(false, true);
  }

  public StatsAccumulator getProcessingCumulativeStats() {
    return getStats(false, false);
  }
}
