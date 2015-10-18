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


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the BaseEvaluatorEnvironment class.
 * <p>
 * @author Spence Koehler
 */
public class TestBaseEvaluatorEnvironment extends TestCase {

  public TestBaseEvaluatorEnvironment(String name) {
    super(name);
  }
  

  public void testSetVariablePattern() {
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foo=bar", new String[]{"foo", "bar"});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, " foo = bar ", new String[]{"foo", "bar"});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foobar ", new String[]{});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foo bar ", new String[]{});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foo bar ", new String[]{});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foo = bar baz ", new String[]{"foo", "bar baz"});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foo =  ", new String[]{"foo", ""});
    doRegexMatchesTest(BaseEvaluatorEnvironment.SET_VARIABLE_PATTERN, "foo =",  new String[]{"foo", ""});
  }


  private final void doRegexMatchesTest(Pattern p, String input, String[] expectedGroups) {
    //NOTE:
    // - if expectedGroups.length == 0, expect input doesn't match pattern
    // - if expectedGroups == null, then show actual match information

    final Matcher m = p.matcher(input);
    final boolean matches = m.matches();

    if (expectedGroups == null) {
      System.out.println("matches(" + input + ")=" + matches);
      if (matches) {
        System.out.println("\twith " + m.groupCount() + " matches:");
        for (int i = 0; i <= m.groupCount(); ++i) {
          System.out.println("\t\t" + i + ": " + m.group(i));
        }
      }
    }
    else if (expectedGroups.length == 0) {
      assertFalse("failed to not match", matches);
    }
    else {
      assertTrue("failed to match", matches);
      assertEquals(expectedGroups.length, m.groupCount());
      for (int i = 1; i <= expectedGroups.length; ++i) {
        assertEquals("group " + i + " mismatch", expectedGroups[i - 1], m.group(i));
      }
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestBaseEvaluatorEnvironment.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
