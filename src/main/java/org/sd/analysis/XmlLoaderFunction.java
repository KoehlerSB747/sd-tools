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
package org.sd.analysis;


import java.io.File;
import java.io.IOException;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * An AnalysisFunction that loads a csv file as a RecordSetAnalysisObject.
 * <p>
 * @author Spencer Koehler
 */
public class XmlLoaderFunction implements AnalysisFunction {
  
  private DataProperties dataProperties;

  public XmlLoaderFunction(DataProperties dataProperties) {
    this.dataProperties = dataProperties;
  }

  @Override
  public AnalysisObject execute(AnalysisObject[] args) {
    AnalysisObject result = null;

    if (args != null && args.length == 1) {
      final File xmlfile = dataProperties.getWorkingFile(args[0].toString(), "dir");
      if (xmlfile.exists()) {
        DomElement xml = null;
        try {
          xml = XmlFactory.loadDocument(xmlfile, false, dataProperties).getDocumentDomElement();
          result = new XmlAnalysisObject(xml);
        }
        catch (IOException ioe) {
          result = new ErrorAnalysisObject("couldn't load xmlfile '" + xmlfile + "'", ioe);
        }
      }
      else {
        result = new ErrorAnalysisObject("XmlLoaderFunction : xmlfile '" + xmlfile.getAbsolutePath() + "' doesn't exist!");
      }
    }

    return result;
  }

  public String toString() {
    return "Loads xml file (arg1) with optional delimiter (arg2).";
  }
}
