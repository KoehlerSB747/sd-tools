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
package org.sd.wordnet.senti;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.sd.io.FileUtil;
import org.sd.nlp.conll.ConllReader;
import org.sd.nlp.conll.ConllSentence;
import org.sd.xml.DataProperties;

/**
 * A SynsetProcessor to work with CoNLL formatted parsed sentences.
 * <p>
 * @author Spencer Koehler
 */
public abstract class SynsetConllProcessor extends SynsetProcessor {
  
  protected abstract void process(ConllSentence sentence);


  public SynsetConllProcessor(DataProperties dataProperties) {
    super(dataProperties);
  }

  @Override
  public void close() {
    // no-op
  }

  @Override
  public void process(String[] args) throws IOException {
    InputStream is = null;

    if (args != null && args.length > 0) {
      // args are conll filenames to process
      for (String arg : args) {
        is = FileUtil.getInputStream(arg);
        try {
          process(is);
        }
        finally {
          if (is != null) is.close();
        }
      }
    }
    else {
      // read conll from stdin
      final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      try {
        process(reader);
      }
      finally {
        reader.close();
      }
    }
  }

  public void process(InputStream is) {
    process(FileUtil.getReader(is, "UTF-8"));
  }

  public void process(BufferedReader reader) {
    final ConllReader conllReader = new ConllReader(reader);
    while (conllReader.hasNext()) {
      final ConllSentence sentence = conllReader.next();
      process(sentence);
    }
  }
}
