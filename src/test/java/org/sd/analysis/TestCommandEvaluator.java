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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.io.FileUtil;

/**
 * JUnit Tests for the CommandEvaluator class.
 * <p>
 * @author Spence Koehler
 */
public class TestCommandEvaluator extends TestCase {

  public TestCommandEvaluator(String name) {
    super(name);
  }
  

  public void testSetGetVariable() throws Exception {
    final CommandEvaluator commandEvaluator = buildCommandEvaluator(new String[] {});

    final String[] commands = new String[] {
      "hello=world",
      "echo hello $hello",
    };
    
    final String[] expectedResult = new String[] {
      "hello world",
    };

    validate(commandEvaluator, commands, expectedResult, null);
  }

  public void testVariableDereferencing() throws Exception {
    final CommandEvaluator commandEvaluator = buildCommandEvaluator(new String[] {
        getDirArg("resources"),
      });

    final String[] commands = new String[] {
      "x=(loadcsv sample1.csv)",
      "$x",
      "$x.b",
      "$x.b.values",
      "$x.b.stats",
      "$x.b.stats.mean",
    };
    
    final String[] expectedResult = new String[] {
      "#RecordSet.sample1.csv[a, b, c]",
      "#Vector.b[3]",
      "[2, 5, 8]",
      "#Stats.b[n=3,mean=5.0,min=2.0,max=8.0,sum=15.0]",
      "5.0",
    };

    validate(commandEvaluator, commands, expectedResult, null);
  }

  public void testXmlDereferencing() throws Exception {
    final CommandEvaluator commandEvaluator = buildCommandEvaluator(new String[] {
      });

    final String[] commands = new String[] {
      "xml=<hello><a>world</a><b alt=\"fadda\">mudda</b><c>nobody</c></hello>",
      "echo hello $xml.hello/b.0.text",
      "echo hello $xml.hello/b.0.@alt",
    };
    
    final String[] expectedResult = new String[] {
      "hello mudda",
      "hello fadda",
    };

    validate(commandEvaluator, commands, expectedResult, null);
  }

  private final CommandEvaluator buildCommandEvaluator(String[] args) throws Exception {
    return CommandEvaluator.buildInstance(args);
  }

  private final String getDirArg(String relativeResourcesDirPath) {
    final String resourcesDir = FileUtil.getFilename(this.getClass(), "resources");
    return "dir=" + resourcesDir;
  }

  private final void validate(CommandEvaluator commandEvaluator, String[] commands,
                              String[] expectedOut, String[] expectedErr) {

    final CommandEvaluator.EvaluationWrapper evalWrapper = commandEvaluator.evaluate(commands);

    assertTrue(evalWrapper.isDone());
    assertFalse(evalWrapper.hasError());
    
    final String[] gotOut = evalWrapper.getOutStrings();
    final String[] gotErr = evalWrapper.getErrStrings();

    if (expectedOut == null) {
      System.out.println("out:");
      for (String out : gotOut) {
        System.out.println("\t" + out);
      }
      System.err.println("err:");
      for (String err : gotErr) {
        System.err.println("\t" + err);
      }
    }
    else {
      assertEquals(expectedOut.length, gotOut.length);
      for (int i = 0; i < expectedOut.length; ++i) {
        assertEquals("out[" + i + "]=" + gotOut[i], expectedOut[i], gotOut[i]);
      }

      assertEquals((expectedErr == null) ? 0 : expectedErr.length, gotErr.length);
      if (expectedErr != null) {
        for (int i = 0; i < expectedErr.length; ++i) {
          assertEquals("err[" + i + "]=" + gotErr[i], expectedErr[i], gotErr[i]);
        }
      }
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestCommandEvaluator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
