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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.atn.AtnParseBasedTokenizer;
import org.sd.io.FileUtil;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.Token;

/**
 * JUnit Tests for the EntityInputContext class.
 * <p>
 * @author Spence Koehler
 */
public class TestEntityInputContext extends TestCase {

  public TestEntityInputContext(String name) {
    super(name);
  }
  

  public void test1() throws IOException {
    final ExtractedEntityLoader loader = new ExtractedEntityLoader();
    final File datesFile = FileUtil.getFile(this.getClass(), "eic-dates.txt");
    final File namesFile = FileUtil.getFile(this.getClass(), "eic-names.txt");
    assertTrue(loader.load(datesFile));
    assertTrue(loader.load(namesFile));

    final List<Token> tokens = new ArrayList<Token>();

    final Map<Integer, EntityContainer> entities = loader.getEntities();
    for (Map.Entry<Integer, EntityContainer> entry : entities.entrySet()) {
      final int lineNum = entry.getKey();
      final EntityContainer entityContainer = entry.getValue();

      final EntityInputContext inputContext = new EntityInputContext(lineNum, entityContainer);
      final AtnParseBasedTokenizer tokenizer = new AtnParseBasedTokenizer(null, null, null, inputContext, StandardTokenizerFactory.DEFAULT_OPTIONS);
      int tokenNum = 0;
      for (Token token = tokenizer.getToken(0); token != null; token = tokenizer.getNextToken(token)) {
        while (!token.hasFeatures()) {
          final Token revised = token.getRevisedToken();
          if (revised == null) break;
          token = revised;
        }

        //System.out.println(String.format("lineNum=%d token[%d]=%s", lineNum, tokenNum, token.getDetailedString()));
        tokens.add(token);
        ++tokenNum;
      }
    }

    final String[] expectedTexts = new String[] {
      "631", "Ella Almeda (Kimball", "b", "Oct.-, 1849", "d", "June 25", "1854",
    };
    final String[] expectedFeatures = new String[] {
      "date", "name", null, "date", "name", "date", "date",
    };

    assertEquals(expectedTexts.length, tokens.size());
    
    int i = 0;
    for (Token token : tokens) {
      assertEquals(expectedTexts[i], token.getText());
      if (expectedFeatures[i] == null) {
        assertFalse(token.hasFeatures());
      }
      else {
        assertTrue(token.hasFeatureValue(expectedFeatures[i], null, null, "true"));
      }
      ++i;
    }
    
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestEntityInputContext.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
