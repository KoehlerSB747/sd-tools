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


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to manage a set of monitors by label, providing rollup views across.
 * <p>
 * @author Spencer Koehler
 */
public class MonitorManager {
  
  private Map<String, Monitor> monitors;
  private int defaultWindowWidth;
  private int defaultSegmentWidth;

  public MonitorManager() {
    this.monitors = new LinkedHashMap<String, Monitor>();
    this.defaultWindowWidth = RollingStats.DEFAULT_WINDOW_WIDTH;
    this.defaultSegmentWidth = RollingStats.DEFAULT_SEGMENT_WIDTH;
  }
  
  public Map<String, Monitor> getMonitors() {
    return monitors;
  }

  public Monitor getMonitor(String label) {
    return getMonitor(label, false, null);
  }

  public Monitor getMonitor(String label, boolean createIfMissing, String descriptionIfCreate) {
    Monitor result = this.monitors.get(label);

    if (result == null && createIfMissing) {
      result = new Monitor(descriptionIfCreate);
      result.setDefaultWindowWidth(this.defaultWindowWidth);
      result.setDefaultSegmentWidth(this.defaultSegmentWidth);
      this.monitors.put(label, result);
    }

    return result;
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


  public StatsAccumulator getRollupStats(boolean access, boolean window) {
    final StatsAccumulator result = new StatsAccumulator("rollup");

    for (Monitor monitor : monitors.values()) {
      final StatsAccumulator stats = monitor.getStats(access, window);
      if (stats != null) {
        result.combineWith(stats);
      }
    }

    return result;
  }

  public StatsAccumulator getRollupAccessWindowStats() {
    return getRollupStats(true, true);
  }

  public StatsAccumulator getRollupAccessCumulativeStats() {
    return getRollupStats(true, false);
  }

  public StatsAccumulator getRollupProcessingWindowStats() {
    return getRollupStats(false, true);
  }

  public StatsAccumulator getRollupProcessingCumulativeStats() {
    return getRollupStats(false, false);
  }
}
