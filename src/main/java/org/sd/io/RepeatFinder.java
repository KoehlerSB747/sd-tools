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
package org.sd.io;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sd.xml.DataProperties;

/**
 * Utility to find repeat files under two different directories.
 * <p>
 * @author Spencer Koehler
 */
public class RepeatFinder {
  
  private List<File> dirs;
  private Map<String, DuplicateContainer> name2files;

  public RepeatFinder(List<File> dirs) {
    this.dirs = dirs;
    this.name2files = new TreeMap<String, DuplicateContainer>();

    for (File dir : dirs) {
      init(dir);
    }
  }

  private final void init(File dir) {
    if (dir.isDirectory()) {  // recurse
      final File[] files = dir.listFiles();
      for (File file : files) {
        init(file);
      }
    }
    else {
      final String name = getName(dir);

      if (name != null) {
        DuplicateContainer dups = name2files.get(name);
        if (dups == null) {
          dups = new DuplicateContainer();
          name2files.put(name, dups);
        }
        dups.add(dir);
      }
    }
  }

  private final String getName(File file) {
    String result = null;

    final String name = file.getName();
    if (name.charAt(0) == '.') return result;

    final int lastDotPos = name.lastIndexOf(".");

    if (lastDotPos > 0) {
      result = name.substring(0, lastDotPos);
    }
    else if (lastDotPos < 0) {
      result = name;
    }

    return result;
  }

  public void showAll(PrintStream out) {
    for (DuplicateContainer dupContainer : name2files.values()) {
      dupContainer.showAll(out);
    }
  }

  public void copyOneOfEach(File newRoot) {
    for (DuplicateContainer dupContainer : name2files.values()) {
      dupContainer.copyOneOfEach(newRoot, this);
    }
  }

  public final File findRootDir(File file) {
    File result = null;

    for (File dir : dirs) {
      if (FileUtil.isParent(dir, file)) {
        result = dir;
        break;
      }
    }

    return result;
  }


  public static final class DuplicateContainer {
    public final Map<Long, TreeSet<File>> duplicates;
    
    public DuplicateContainer() {
      this.duplicates = new TreeMap<Long, TreeSet<File>>();
    }

    public void add(File file) {
      final long size = FileUtil.size(file);
      TreeSet<File> dups = duplicates.get(size);
      if (dups == null) {
        dups = new TreeSet<File>(FileUtil.getPathLengthComparator());
        duplicates.put(size, dups);
      }
      dups.add(file);
    }

    public void showAll(PrintStream out) {
      for (Map.Entry<Long, TreeSet<File>> entry : duplicates.entrySet()) {
        final Long size = entry.getKey();
        final TreeSet<File> dups = entry.getValue();
        for (File dup : dups) {
          out.println(size + "\t" + dup.getAbsolutePath());
        }
        System.out.println();
      }
    }

    public void copyOneOfEach(File newRoot, RepeatFinder repeatFinder) {
      for (TreeSet<File> dupSet : duplicates.values()) {
        // get longest path from dupSet == last value
        final File longestPath = dupSet.last();

        // get the old root
        final File oldRoot = repeatFinder.findRootDir(longestPath);

        // change to the new root
        final File newFile = FileUtil.changeRoot(oldRoot, newRoot, longestPath);

        // make directories
        if (!newFile.getParentFile().exists()) newFile.getParentFile().mkdirs();

        // copy old file to new location
        FileUtil.copyFile(longestPath, newFile);
      }
    }
  }


  public static void main(String[] args) {
    // properties:
    //   uniquesDir -- (optional) copy unique files to the given dir if specified.
    //
    // args: directories to compare
    //
    // output: stdout, if uniquesDir not specifie
    //
    final DataProperties options = new DataProperties(args);
    args = options.getRemainingArgs();

    final List<File> files = new ArrayList<File>();
    for (String arg : args) {
      files.add(new File(arg));
    }

    final RepeatFinder finder = new RepeatFinder(files);

    final String uniquesDir = options.getString("uniquesDir", null);
    if (uniquesDir == null) {
      finder.showAll(System.out);
    }
    else {
      finder.copyOneOfEach(new File(uniquesDir));
    }
  }
}
