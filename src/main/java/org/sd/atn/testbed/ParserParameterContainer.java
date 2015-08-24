package org.sd.atn.testbed;


import java.util.LinkedHashMap;
import java.util.Map;
import org.sd.atn.AtnParseRunner;
import org.sd.atn.ResourceManager;
import org.sd.token.StandardTokenizerOptions;

/**
 * Container for parameter data.
 * <p>
 * @author Spence Koehler
 */
public class ParserParameterContainer {
  
  private String label;
  private ResourceManager.MetaData metaData;
  private Map<String, String> resourceName2Path;
  private String _asString;

  private AtnParseRunner parseRunner;
  private StandardTokenizerOptions tokenizerOptions;

  public ParserParameterContainer(String label, ResourceManager.MetaData metaData, String[][] resourcePaths) {
    this.label = label;
    this.metaData = metaData;
    this.resourceName2Path = null;
    this.parseRunner = null;
    this.tokenizerOptions = null;

    if (resourcePaths != null) {
      this.resourceName2Path = new LinkedHashMap<String, String>();
      for (String[] namePath : resourcePaths) {
        resourceName2Path.put(namePath[0], namePath[1]);
      }
    }
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public ResourceManager.MetaData getMetaData() {
    return metaData;
  }

  public boolean hasResourcePaths() {
    return resourceName2Path != null && resourceName2Path.size() > 0;
  }

  public Map<String, String> getResourcePaths() {
    return resourceName2Path;
  }

  public String getResourcePath(String resourceName) {
    return (resourceName2Path == null) ? null : resourceName2Path.get(resourceName);
  }

  public boolean isParser() {
    return parseRunner != null;
  }

  public void setParseRunner(AtnParseRunner parseRunner) {
    this.parseRunner = parseRunner;
  }

  public AtnParseRunner getParseRunner() {
    return parseRunner;
  }

  public boolean isTokenizer() {
    return tokenizerOptions != null;
  }

  public void setTokenizerOptions(StandardTokenizerOptions tokenizerOptions) {
    this.tokenizerOptions = tokenizerOptions;
  }

  public StandardTokenizerOptions getTokenizerOptions() {
    return tokenizerOptions;
  }

  public String toString() {
    return label;
  }

  public String getDataAsString() {
    if (_asString == null) {
      _asString = buildString();
    }
    return _asString;
  }

  private final String buildString() {
    final StringBuilder result = new StringBuilder();

    result.append(label).append(":\n");

    if (resourceName2Path != null) {
      result.append("\tResources:\n");

      for (Map.Entry<String, String> name2path : resourceName2Path.entrySet()) {
        result.
          append("\t\t").
          append(name2path.getKey()).
          append(" : ").
          append(name2path.getValue()).
          append('\n');
      }
    }

    if (metaData != null) {
      if (metaData.hasExtraArgs()) {
        result.append("\tArgs:\n");
        for (Object arg : metaData.getExtraArgs()) {
          result.
            append("\t\t").
            append(arg == null ? "<null>" : arg.toString()).
            append('\n');
        }
      }
      if (metaData.hasProperties()) {
        result.append("\tProperties:\n");
        for (Map.Entry<Object, Object> entry : metaData.getProperties().entrySet()) {
          final Object key = entry.getKey();
          final Object value = entry.getValue();

          result.
            append("\t\t").
            append(key == null ? "<null>" : key.toString()).
            append('=').
            append(value == null ? "<null>" : value.toString()).
            append('\n');
        }
      }

      final ResourceManager.XmlMetaData xmlMetaData = metaData.asXmlMetaData();
      if (xmlMetaData != null && xmlMetaData.getResourceElement() != null) {
        result.append("\tXML:\n");
        xmlMetaData.getResourceElement().asPrettyString(result, 4, 2);
        result.append('\n');
      }

      final ResourceManager.ClassMetaData classMetaData = metaData.asClassMetaData();
      if (classMetaData != null) {
        result.
          append("\tClass: ").
          append(classMetaData == null ? "<null>" : classMetaData).
          append('\n');
      }

      final ResourceManager.FileMetaData fileMetaData = metaData.asFileMetaData();
      if (fileMetaData != null) {
        result.
          append("\tFile: ").
          append(fileMetaData.getFileName()).
          append("\n\t\t").
          append(fileMetaData.getFile().getAbsolutePath()).
          append('\n');
      }
    }

    return result.toString();
  }
}
