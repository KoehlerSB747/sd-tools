/*
    Copyright 2009-2017 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the LinearRegression class.
 * <p>
 * @author Spence Koehler
 */
public class TestLinearRegression extends TestCase {

  public TestLinearRegression(String name) {
    super(name);
  }
  

  public void testSimple() {
    final LinearRegression lreg = new LinearRegression();
    lreg.add(0.0, 0.0);
    lreg.add(1.0, 1.0);
    assertEquals(2.0, lreg.getY(2.0), 0.001);
    lreg.add(2.0, 2.0);
    assertEquals(3.0, lreg.getY(3.0), 0.001);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestLinearRegression.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
