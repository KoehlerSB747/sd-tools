/*
   Copyright 2008-2015 Semantic Discovery, Inc.

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
package org.sd.csv;


import java.io.File;
import java.io.IOException;

/**
 * Utility to load Csv Files.
 * <p>
 * @author Spencer Koehler
 */
public class CsvFileLoader {
  
  public static CsvRecordSet loadCsvFile(File csvFile) throws IOException {
    return new CsvRecordSet(csvFile.getName()).load(csvFile);
  }

  public static CsvRecordSet loadCsvFile(File csvFile, String fieldDelimiter) throws IOException {
    return new CsvRecordSet(csvFile.getName()).load(csvFile, fieldDelimiter);
  }
}
