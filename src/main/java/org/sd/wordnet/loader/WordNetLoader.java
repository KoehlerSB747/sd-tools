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
package org.sd.wordnet.loader;


import java.io.File;
import java.io.IOException;
import org.sd.atnexec.ConfigUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.wordnet.senti.SentimentLoader;
import org.sd.xml.DataProperties;

/**
 * Utility to load wordnet files.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetLoader {
  
  /**
   * Load lexDictionary using default config data.
   */
  public static final LexDictionary loadLexDictionary() {
    final ConfigUtil configUtil = new ConfigUtil();
    return doLoadLexDictionary(configUtil.getDataProperties());
  }

  /**
   * Load lexDictionary using the given properties augmented with config data.
   */
  public static final LexDictionary loadLexDictionary(File propertiesFile) {
    final DataProperties dataProperties = new DataProperties();
    try {
      dataProperties.incorporateProperties(propertiesFile, null);
    }
    catch (IOException ioe) {
      throw new IllegalArgumentException("Unable to load propertiesFile=" + propertiesFile.getAbsolutePath(), ioe);
    }
    return doLoadLexDictionary(dataProperties);
  }

  public static final LexDictionary loadLexDictionary(String[] args) {
    final ConfigUtil configUtil = new ConfigUtil(args);
    return doLoadLexDictionary(configUtil.getDataProperties());
  }

  /**
   * Load lexDictionary using the given data properties augmented with config data.
   */
  public static final LexDictionary loadLexDictionary(DataProperties dataProperties) {
    final ConfigUtil configUtil = new ConfigUtil(dataProperties);
    return doLoadLexDictionary(configUtil.getDataProperties());
  }

  /**
   * Load lexDictionary using the given data properties.
   */
  public static final LexDictionary doLoadLexDictionary(DataProperties dataProperties) {
    LexDictionary result = null;

    // Properties:
    //   dbFileDir -- path to dbFileDir
    //   sentiWordNet -- path to sentiWordNet file

    final File dbFileDir = dataProperties.getFile("dbFileDir", "workingDir");
    if (dbFileDir != null) {
      try {
        result = new LexDictionary(new LexLoader(dbFileDir));
      }
      catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }

      final File sentiWordNet = dataProperties.getFile("sentiWordNet", "workingDir");
      if (sentiWordNet != null) {
        final SentimentLoader sentimentLoader = new SentimentLoader(result);
        sentimentLoader.setVerbose(dataProperties.getBoolean("sentimentLoaderVerbose", false));
        sentimentLoader.loadSentiWordNet(sentiWordNet);
      }
    }

    return result;
  }

}
