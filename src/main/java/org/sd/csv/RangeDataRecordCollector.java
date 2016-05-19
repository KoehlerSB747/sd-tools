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
