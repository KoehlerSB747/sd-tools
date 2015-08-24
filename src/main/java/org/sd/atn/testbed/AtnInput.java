package org.sd.atn.testbed;


import org.sd.atn.InputOptions;
import org.sd.atn.XmlInputDecoder;
import org.sd.atn.XmlParseInputContext;
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;
import org.sd.util.SimpleInputContextIterator;
import org.sd.xml.DataProperties;

/**
 * Class to encapsulate input to the atn parser.
 * <p>
 * @author Spence Koehler
 */
public class AtnInput extends SimpleInputContextIterator {
  
  private XmlInputDecoder.Paragraph paragraph;
  private InputContext inputContext;
  private DataProperties options;
  private DataProperties overrides;
  private DataProperties _overrideOptions;

  public AtnInput(XmlInputDecoder.Paragraph paragraph, int nextID, DataProperties options, DataProperties overrides) {
    super(1);
    this.paragraph = paragraph;
    this.inputContext = new XmlParseInputContext(paragraph, nextID);
    this.options = options;
    this.overrides = overrides;
    this._overrideOptions = null;
  }

  public XmlInputDecoder.Paragraph getParagraph() {
    return paragraph;
  }

  public String getText() {
    return paragraph.getText();
  }

  public DataProperties getOptions() {
    return options;
  }

  public DataProperties getOverrides() {
    return overrides;
  }

  public DataProperties getOverrideOptions() {
    if (_overrideOptions == null) {
      _overrideOptions = buildOverrideOptions();
    }
    return _overrideOptions;
  }

  public boolean hasParagraphProperties() {
    return paragraph.hasProperties();
  }

  public String getParagraphType() {
    String result = null;

    if (hasParagraphProperties()) {
      result = paragraph.getProperty("type");
    }

    return result;
  }


  protected InputContext getItem(int itemNum) {
//    return (itemNum == 0) ? inputContext : null;
    return inputContext;
  }


  private final DataProperties buildOverrideOptions() {
    DataProperties result = overrides;

    final String type = getParagraphType();
    if (type != null && !"".equals(type)) {
      final String startRulesValue = options == null ? null : options.getString("startRules." + type, null);
      if (startRulesValue != null && !"".equals(startRulesValue)) {
        final String[] pieces = startRulesValue.split("\\s*:\\s*");
        if (pieces.length == 3) {
          result = new DataProperties(result);  // copy
          result.set(InputOptions.buildStartRulesProperty(pieces[0], pieces[1]), pieces[2]);
        }
        else {
          //todo: log warning message
        }
      }
    }

    return result;
  }
}
