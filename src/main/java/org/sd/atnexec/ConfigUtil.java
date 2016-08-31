package org.sd.atnexec;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.sd.io.FileUtil;
import org.sd.xml.DataProperties;

/**
 * Utility for loading configuration files for a project.
 * <p>
 * For default construction, project resources are assumed to reside within
 * the project's root directory in the "resources" subdirectory and the
 * project's root is assumed to be a parent to the provided class.
 * <p>
 * Note that directories above the provided class with the same name as
 * the project may cause unexpected results with the default construction
 * routines.
 * <p>
 * Default construction is intended for expediting testing and not intended
 * for production paths. The safest construction will supply dataProperties
 * with a "resourcesDir" attribute that points to the correct resources
 * directory.
 *
 * @author Spencer Koehler
 */
public class ConfigUtil {
  
  public static final String PROJECT_ROOT_NAME_KEY = "projectRootName";
  public static final String RESOURCES_DIR_KEY = "resourcesDir";
  public static final String DEFAULT_PROJECT_ROOT_NAME = "sd-tools";
  public static final String DEFAULT_RESOURCES_DIR_NAME = "resources";
  public static final String DEFAULT_PROPERTIES_NAME = "default.properties";
  public static final String DEFAULT_PROPERTIES_EXTENSION = ".properties";

  public static final Options DEFAULT_OPTIONS =
    new Options(RESOURCES_DIR_KEY, DEFAULT_RESOURCES_DIR_NAME, true,
                DEFAULT_PROPERTIES_NAME, true, DEFAULT_PROPERTIES_EXTENSION);


  private File resourcesDir;
  private Options options;
  private Properties defaultProperties;
  private DataProperties dataProperties;

  /**
   * Initialize with the given resourcesDir.
   */
  public ConfigUtil(File resourcesDir) {
    this(resourcesDir, null);
  }

  public ConfigUtil(File resourcesDir, Options options) {
    this.resourcesDir = resourcesDir;
    this.options = (options == null) ? DEFAULT_OPTIONS : options;
    this.defaultProperties = initDefaultProperties();
    this.dataProperties = adjust(null);
  }

  /**
   * Initialize with defaults for "sd-tools" project.
   */
  public ConfigUtil() {
    this(DEFAULT_PROJECT_ROOT_NAME, ConfigUtil.class);
  }

  /**
   * Initialize with defaults for the given project.
   *
   * If projectRootName is null, then the first "resources" directory found
   * from the projectClass's directory and each of its parents will be used.
   * <p>
   * If projectRootName is non-null, then the first instance of projectRootName
   * from the projectClass's directory and its parents that has a "resources"
   * directory will be used to limit the search.
   */
  public ConfigUtil(String projectRootName, Class projectClass) {
    this(projectRootName, projectClass, null);
  }

  /**
   * Initialize with values for the given project.
   *
   * If projectRootName is null, then the first "resources" directory found
   * from the projectClass's directory and each of its parents will be used.
   * <p>
   * If projectRootName is non-null, then the first instance of projectRootName
   * from the projectClass's directory and its parents that has a resources
   * directory with the given name will be used to limit the search.
   */
  public ConfigUtil(String projectRootName, Class projectClass, Options options) {
    this.options = (options == null) ? DEFAULT_OPTIONS : options;
    this.resourcesDir = getProjectResources(projectRootName, projectClass, options.getResourcesDirName());

    if (this.resourcesDir == null) {
      throw new IllegalArgumentException(
        "no 'resources' found for projectRootName=" + projectRootName +
        ", projectClass=" + ((projectClass == null) ? "null" : projectClass.getName()));
    }

    this.defaultProperties = initDefaultProperties();
    this.dataProperties = adjust(null);
  }

  /**
   * Initialize with defaults for the project of the given class.
   * <p>
   * NOTE: This uses the construction with a null projectRootName and
   * the given projectClass.
   */
  public ConfigUtil(Class projectClass) {
    this(null, projectClass);
  }

  /**
   * Initialize with defaults for the project of the given class.
   * <p>
   * NOTE: This uses the construction with a null projectRootName and
   * the given projectClass.
   */
  public ConfigUtil(Class projectClass, Options options) {
    this(null, projectClass, options);
  }

  /**
   * Initialize with the given options, where at least "resourcesDir"
   * must be defined to correctly locate resources.
   */
  public ConfigUtil(DataProperties dataProperties) {
    this(dataProperties, null);
  }
  
  /**
   * Initialize with the given command-line arguments.
   */
  public ConfigUtil(String[] args) {
    this(new DataProperties(args), null);
  }
  
  /**
   * Initialize with the given options, where at least "resourcesDir"
   * must be defined to correctly locate resources.
   */
  public ConfigUtil(DataProperties dataProperties, Options options) {
    this.options = (options == null) ? DEFAULT_OPTIONS : options;
    this.dataProperties = dataProperties;

    final String resourcesDirPath = dataProperties.getString(this.options.getResourcesDirKey(), null);
    if (resourcesDirPath != null) {
      this.resourcesDir = new File(resourcesDirPath);
    }
    else {
      // fall back to resources for *this* (sd-tools) project
      this.resourcesDir = getProjectResources(null, ConfigUtil.class, this.options.getResourcesDirName());
    }

    if (this.resourcesDir == null) {
      throw new IllegalArgumentException("dataProperties needs key '" + this.options.getResourcesDirKey() + "' to point to resourcesDir");
    }
    this.defaultProperties = initDefaultProperties();

    this.adjust(dataProperties);
  }

  private final Properties initDefaultProperties() {
    Properties result = null;
    if (options.doAutoLoadDefaultProperties()) {
      final File dpFile = new File(resourcesDir, options.getDefaultPropertiesName());
      if (dpFile.exists()) {
        try {
          result = FileUtil.loadProperties(null, dpFile, null);
        }
        catch (IOException ioe) {
          throw new IllegalStateException("Failed to load default properties '" + dpFile.getAbsolutePath() + "'", ioe);
        }
      }
    }
    return result;
  }

  /**
   * Get this configuration's resources dir.
   */
  public File getResourcesDir() {
    return resourcesDir;
  }

  /**
   * Get the default properties.
   */
  public Properties getDefaultProperties() {
    return defaultProperties;
  }

  public boolean hasDataProperties() {
    return dataProperties != null;
  }

  public void setDataProperties(DataProperties dataProperties) {
    this.dataProperties = dataProperties;
  }

  public DataProperties getDataProperties() {
    return dataProperties;
  }


  /**
   * Load the given properties file, adding the resourcesDir as the result's
   * workingDir if indicated.
   */
  public DataProperties loadProperties(File propertiesFile, boolean adjust) throws IOException {
    final DataProperties result = addProperties(null, propertiesFile, adjust);
    return result;
  }

  /**
   * Load the properties, adding to the given dataProperties if present or
   * creating if null and adjusting if indicated.
   * <p>
   * Add the ".properties" extension to propertiesFile if needed.
   */
  public DataProperties addProperties(DataProperties dataProperties, File propertiesFile, boolean adjust) throws IOException {
    if (dataProperties == null) {
      dataProperties = new DataProperties();
    }

    final Properties properties = dataProperties.getProperties();
    FileUtil.loadProperties(properties, propertiesFile, options.getDefaultPropertiesExtension());

    if (adjust) adjust(dataProperties);

    return dataProperties;
  }

  /**
   * Adjust the given dataProperties according to this instance's options,
   * creating if null.
   * <p>
   * e.g., add resourcesDir, set workingDir, load default properties, etc.
   */
  public final DataProperties adjust(DataProperties dataProperties) {
    // create if needed
    if (dataProperties == null) dataProperties = new DataProperties();

    // set resourcesDir
    dataProperties.set(options.getResourcesDirKey(), resourcesDir.getAbsolutePath());

    // set working dir
    if (options.doSetResourcesDirAsWorkingDir()) {
      dataProperties.set("workingDir", resourcesDir.getAbsolutePath());
    }

    // add default properties
    if (defaultProperties != null) {
      dataProperties.incorporate(defaultProperties);
    }

    return dataProperties;
  }


  public static final File getProjectResources(String projectRootName, Class projectClass, String resourcesDirName) {
    return getProjectResources(projectRootName, new File(projectClass.getResource(".").getFile()), resourcesDirName);
  }

  public static final File getProjectResources(String projectRootName, File deepRef, String resourcesDirName) {
    File result = null;

    for (; deepRef != null; deepRef = deepRef.getParentFile()) {
      if (projectRootName == null || projectRootName.equals(deepRef.getName())) {
        final File resourcesDir = new File(deepRef, resourcesDirName);
        if (resourcesDir.exists()) {
          result = resourcesDir;
          break;
        }
      }
    }

    return result;
  }


  public static class Options {
    private String resourcesDirKey;
    private String resourcesDirName;
    private boolean setWorkingDir;
    private String defaultPropertiesName;
    private boolean autoLoadDefaultProperties;
    private String defaultPropertiesExtension;
    
    public Options(String resourcesDirKey, String resourcesDirName, boolean setWorkingDir,
                   String defaultPropertiesName, boolean autoLoadDefaultProperties,
                   String defaultPropertiesExtension) {
      this.resourcesDirKey = resourcesDirKey;
      this.resourcesDirName = resourcesDirName;
      this.setWorkingDir = setWorkingDir;
      this.defaultPropertiesName = defaultPropertiesName;
      this.autoLoadDefaultProperties = autoLoadDefaultProperties;
      this.defaultPropertiesExtension = defaultPropertiesExtension;
    }

    public String getResourcesDirKey() {
      return resourcesDirKey;
    }

    public Options setResourcesDirKey(String resourcesDirKey) {
      this.resourcesDirKey = resourcesDirKey;
      return this;
    }

    public String getResourcesDirName() {
      return resourcesDirName;
    }

    public Options setResourcesDirName(String resourcesDirName) {
      this.resourcesDirName = resourcesDirName;
      return this;
    }

    public boolean doSetResourcesDirAsWorkingDir() {
      return setWorkingDir;
    }

    public Options setResourcesDirAsWorkingDir(boolean setWorkingDir) {
      this.setWorkingDir = setWorkingDir;
      return this;
    }

    public String getDefaultPropertiesName() {
      return defaultPropertiesName;
    }

    public Options setDefaultPropertiesName(String defaultPropertiesName) {
      this.defaultPropertiesName = defaultPropertiesName;
      return this;
    }

    public boolean doAutoLoadDefaultProperties() {
      return autoLoadDefaultProperties;
    }

    public Options setAutoLoadDefaultProperties(boolean autoLoadDefaultProperties) {
      this.autoLoadDefaultProperties = autoLoadDefaultProperties;
      return this;
    }

    public String getDefaultPropertiesExtension() {
      return defaultPropertiesExtension;
    }

    public Options setDefaultPropertiesExtension(String defaultPropertiesExtension) {
      this.defaultPropertiesExtension = defaultPropertiesExtension;
      return this;
    }
  }
}
