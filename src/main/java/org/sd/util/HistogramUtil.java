/*
   Copyright 2015 Semantic Discovery, Inc.

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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.sd.io.FileUtil;
import org.sd.util.Histogram;
import org.sd.util.PropertiesParser;
import org.sd.xml.CollapsedHistogram;

/**
 * Utility to load/view histogram data.
 * <p>
 * @author Spencer Koehler
 */
public class HistogramUtil {

  public static final Histogram<String> loadHistogram(File inFile) throws IOException {
    Histogram<String> result = null;

    final BufferedReader reader = FileUtil.getReader(inFile);

    // read key \t count
    try {
      result = loadHistogram(reader);
    }
    finally {
      if (reader != null) {
        reader.close();
      }
    }

    return result;
  }

  public static final Histogram<String> loadHistogram(BufferedReader reader) throws IOException {
    // read key \t count

    Histogram<String> result = new Histogram<String>();
    String line = null;
    while ((line = reader.readLine()) != null) {
      if ("".equals(line) || line.charAt(0) == '#') continue;
      final String[] pieces = line.split("\\t");
      if (pieces.length != 2) continue;
      result.add(pieces[0], Long.parseLong(pieces[1]));
    }
    return result;
  }

  public static final void writeHistogram(File outFile, Histogram<?> histogram) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(outFile);

    // write key \t count
    try {
      writeHistogram(writer, histogram);
    }
    finally {
      if (writer != null) {
        writer.close();
      }
    }
  }

  public static final void writeHistogram(BufferedWriter writer, Histogram<?> histogram) throws IOException {
    // write key \t count

    for (Histogram<?>.Frequency<?> freq : histogram.getFrequencies()) {
      writer.write(String.format("%s\t%d\n", freq.getElement().toString(), freq.getFrequency()));
    }
  }

  public static void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties p = pp.getProperties();
    args = pp.getArgs();

    //
    // Loads the histogram (key \t count) data from a file.
    // Emits CollapsedHistogram to stdout.
    //
    // Properties:
    // - infile -- (required) input file with key \t count pairs
    // 

    final String infile = p.getProperty("infile");
    if (infile != null && !"".equals(infile)) {
      final BufferedReader reader = FileUtil.getReader(infile);
      final Histogram<String> h = loadHistogram(reader);
      reader.close();

      final CollapsedHistogram ch = CollapsedHistogram.makeInstance(h, 5);
      System.out.println(ch.toString(0));
    }
  }
}
