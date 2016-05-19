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
package org.sd.csv;


import java.util.List;
import org.sd.util.range.IntegerRange;

/**
 * A RecordValueCollector to collect DataRecords as counted starting from 0
 * from the start of a range, inclusive, to the end of the range, exclusive.
 * <p>
 * @author Spencer Koehler
 */
public class RangeDataRecordCollector implements RecordValueCollector<DataRecord> {
  
  private IntegerRange range;
  private int recNum;

  RangeDataRecordCollector(IntegerRange range) {
    this.range = range;
    this.recNum = 0;
  }

  public boolean collect(List<DataRecord> collector, DataRecord dataRecord) {
    boolean result = true;

    if (range == null || range.includes(recNum)) {
      collector.add(dataRecord);
    }

    ++recNum;

    return result;
  }
}
