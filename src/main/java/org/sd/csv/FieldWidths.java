package org.sd.csv;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for tracking field widths and nicely formatting records.
 * <p>
 * @author Spencer Koehler
 */
public class FieldWidths {
  
  private Map<String, Integer> widths;

  public FieldWidths() {
    this.widths = new HashMap<String, Integer>();
  }

  public FieldWidths(Map<String, Integer> widths) {
    this.widths = widths;
  }

  public Map<String, Integer> getWidths() {
    return widths;
  }

  public int getWidth(String fieldName) {
    final Integer result = (widths == null) ? null : widths.get(fieldName);
    final int value = (result == null) ? 0 : result;
    return Math.max(value, fieldName.length());
  }

  public String format(String fieldName, String value) {
    String result = value;

    final int width = getWidth(fieldName);
    if (width > 0) {
      result = String.format("%" + width + "s", value);
    }

    return result;
  }

  public String formatHeader(String fieldName) {
    return format(fieldName, fieldName);
  }

  public String format(String fieldName, double value) {
    //NOTE: currently assuming all doubles are from 0 to 1
    return format(fieldName, String.format("%.5f", value));
  }

  public String formatHeader(String fieldName, double value) {
    return format(fieldName, String.format("%7s", fieldName));
  }

  public String format(String fieldName, long value, long maxValue) {
    final int width = (int)(Math.log(maxValue) / Math.log(10.0) + 0.5);
    return format(fieldName, String.format("%" + width + "d", value));
  }

  public String formatHeader(String fieldName, long maxValue) {
    final int width = (int)(Math.log(maxValue) / Math.log(10.0) + 0.5);
    return format(fieldName, String.format("%" + width + "s", fieldName));
  }

  public final void updateWidth(String fieldName, String value) {
    if (value != null) {
      final Integer width = widths.get(fieldName);
      final int valueWidth = value.length();

      if (width == null || valueWidth > width) {
        widths.put(fieldName, valueWidth);
      }
    }
  }

  public String buildFormattedHeaderLine(List<String> fieldNames) {
    final StringBuilder result = new StringBuilder();

    for (String fieldName : fieldNames) {
      if (result.length() > 0) result.append('\t');
      result.append(formatHeader(fieldName));
    }

    return result.toString();
  }

  public String buildFormattedDataLine(DataRecord dataRecord, List<String> fieldNames) {
    final StringBuilder result = new StringBuilder();

    for (String fieldName : fieldNames) {
      final String fieldValue = dataRecord.getFieldValue(fieldName, "");
      if (result.length() > 0) result.append('\t');
      result.append(format(fieldName, fieldValue));
    }

    return result.toString();
  }
}
