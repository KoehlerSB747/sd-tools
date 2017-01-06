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
package org.sd.nlp.entity;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.sd.io.FileUtil;

/**
 * Utility for loading multiple files with the same input lines of the form:
 *   inputLine \t entitiesXml
 * <p>
 * Note that files are grouped by having matching prefix names up to the first
 * non-alphanumeric character.
 *
 * @author Spencer Koehler
 */
public class ExtractedEntityLoader {

  public static final boolean FAIL_ON_MISMATCH = true;


  private List<File> files;
  private String fileGroupName;
  private TreeMap<Long, String> inputLines;     // mapped by lineNum
  private Map<Long, EntityContainer> entities;  // mapped by lineNum

  public ExtractedEntityLoader() {
    this.files = null;
    this.fileGroupName = null;
    this.inputLines = new TreeMap<Long, String>();
    this.entities = new HashMap<Long, EntityContainer>();
  }

  /**
   * Load the file if it belongs to this loader's group, meaning either
   * it is the first file to be loaded or its group name matches that of the
   * first file loaded.
   *
   * @param file  The file to load.
   *
   * @return true if loaded or false if the file doesn't belong to this loader's group
   */
  public boolean load(File file) throws IOException {
    boolean result = false;

    final String curGroupName = getGroupName(file);
    if (fileGroupName == null || curGroupName.equals(fileGroupName)) {
      if (fileGroupName == null) fileGroupName = curGroupName;
      doLoad(file);
      result = true;
    }

    return result;
  }

  /**
   * Get the (possibly null) files loaded in this loader.
   */
  public List<File> getFiles() {
    return files;
  }

  /**
   * Get the (possibly null) file group name common to all files in this loader.
   */
  public String getFileGroupName() {
    return fileGroupName;
  }

  /**
   * Set the file group name, but only if it hasn't yet been established by
   * loading the first file.
   */
  public ExtractedEntityLoader setFileGroupName(String fileGroupName) {
    if (this.fileGroupName == null) {
      this.fileGroupName = fileGroupName;
    }
    return this;
  }    

  /**
   * Get all (possibly null) input lines, referenced by line number.
   */
  public TreeMap<Long, String> getInputLines() {
    return inputLines;
  }

  /**
   * Get all (possibly null) entities referenced by line number.
   */
  public Map<Long, EntityContainer> getEntities() {
    return entities;
  }

  public List<Entity> addLine(long lineNum, String inputLine, String entitiesXmlString) {
    List<Entity> result = null;

    final EntityLineAligner aligner = new EntityLineAligner(inputLine);
    //NOTE: inputLine must be aligner's baseLine for correct entity positional data

    // check/add lineNum to inputLine mapping
    synchronized (inputLines) {
      String curline = inputLines.get(lineNum);
      if (curline == null) {
        inputLines.put(lineNum, inputLine);
      }
      else {
        aligner.setAltLine(curline);
        if (FAIL_ON_MISMATCH) {
          if (!aligner.aligns()) {
            if (entitiesXmlString != null && !"".equals(entitiesXmlString)) {
              throw new IllegalStateException("ERROR: mismatched lines '" + curline + "' -vs- '" + inputLine + "'");
            }
          }
        }
      }
    }

    // add entities
    if (entitiesXmlString != null && !"".equals(entitiesXmlString)) {
      synchronized (entities) {
        EntityContainer entityContainer = entities.get(lineNum);
        if (entityContainer == null) {
          entityContainer = new EntityContainer();
          entities.put(lineNum, entityContainer);
        }
        result = entityContainer.add(lineNum, entitiesXmlString, aligner);
      }
    }
    
    return result;
  }

  private final void doLoad(File file) throws IOException {
    if (files == null) {
      this.files = new ArrayList<File>();
    }
    this.files.add(file);

    long lineNum = 0;
    String line = null;
    final BufferedReader reader = FileUtil.getReader(file);
    try {
      while ((line = reader.readLine()) != null) {
        if ("".equals(line) || line.startsWith("#")) continue;

        final String[] parts = line.split("\\t");
        final String inputLine = parts[0];
        final String entitiesString = (parts.length > 1) ? parts[1] : null;

        addLine(lineNum, inputLine, entitiesString);

        ++lineNum;
      }
    }
    finally {
      reader.close();
    }
  }
  
  /**
   * Compute the group name for the file, where files with a matching group name
   * all contain the same lines but with perhaps different or no extracted entities.
   * <p>
   * This can be overridden by extenders. The default implementation uses the
   * file name from its first character to its first non-alpha-numeric character.
   *
   * @param file  The file whose group name to get
   *
   * @return the group name for the file.
   */
  protected String getGroupName(File file) {
    final String filename = file.getName();
    return filename.split("\\W")[0];
  }
}
