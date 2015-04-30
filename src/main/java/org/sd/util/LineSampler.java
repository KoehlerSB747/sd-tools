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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import org.sd.io.FileUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.SampleCollector;

/**
 * Utility to collect sample lines from a file.
 * <p>
 * @author Spencer Koehler
 */
public class LineSampler {
  

  public static void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties p = pp.getProperties();
    args = pp.getArgs();

    // Properties:
    // - file -- (optional, accept data from stdin if absent) file whose lines to sample
    // - numSamples -- (required) number of samples to collect
    //
    // sends sampled lines to stdout
    //

    BufferedReader in = null;

    final String file = p.getProperty("file");
    if (file != null) {
      in = FileUtil.getReader(file);
    }
    else {
      in = new BufferedReader(new InputStreamReader(System.in));
    }

    final int numSamples = PropertiesParser.getInt(p, "numSamples", "0");

    if (numSamples > 0) {
      String line = null;
      final SampleCollector<String> collector = new SampleCollector<String>(numSamples);
      while ((line = in.readLine()) != null) {
        collector.consider(line);
      }

      for (String sample : collector.getSamples()) {
        System.out.println(sample);
      }
    }
  }
}
