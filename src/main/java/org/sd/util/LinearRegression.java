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


/**
 * A helper class to collect (x,y) samples and compute the linear regression.
 *
 * @author Spence Koehler
 */
public class LinearRegression {

  private long n;
  private double x_sum;
  private double y_sum;
  private double xy_sum;
  private double xx_sum;
  private double yy_sum;

  private Double _m;
  private Double _b;

  /**
   * Default constructor.
   */
  public LinearRegression() {
    this.n = 0L;
    this.x_sum = 0.0;
    this.y_sum = 0.0;
    this.xy_sum = 0.0;
    this.xx_sum = 0.0;
    this.yy_sum = 0.0;

    this._m = null;
    this._b = null;
  }

  public double getY(double x) {
    return getM() * x + getB();
  }

  public double getM() {
    if (this._m == null) {
      computeRegression();
    }
    return this._m;
  }

  public double getB() {
    if (this._b == null) {
      computeRegression();
    }
    return this._b;
  }

  public String toString() {
    return String.format("y = %.4f x + %.4f", getM(), getB());
  }

  /**
   * Add a sampled value to this instance.
   */
  public void add(double x, double y) {
    ++n;
    x_sum += x;
    y_sum += y;
    xy_sum += (x * y);
    xx_sum += (x * x);
    yy_sum += (y * y);

    _m = null;
    _b = null;
  }

  /**
   * Get the number of sampled values.
   */
  public long getN() {
    return n;
  }

  /**
   * Get the sum of all "x" values.
   */
  public double getXSum() {
    return x_sum;
  }

  /**
   * Get the sum of all "y" values.
   */
  public double getYSum() {
    return y_sum;
  }
  
  /**
   * Get the sum of all "xy" values.
   */
  public double getXYSum() {
    return xy_sum;
  }

  /**
   * Get the sum of all "xx" values.
   */
  public double getXXSum() {
    return xx_sum;
  }

  /**
   * Get the sum of all "yy" values.
   */
  public double getYYSum() {
    return yy_sum;
  }

  private final void computeRegression() {
    this._m = 0.0;
    this._b = 0.0;

    final double denominator = n * xx_sum - x_sum * x_sum;
    if (denominator != 0) {
      final double m_numerator = n * xy_sum - x_sum * y_sum;
      final double b_numerator = y_sum * xx_sum - x_sum * xy_sum;

      this._m = m_numerator / denominator;
      this._b = b_numerator / denominator;
    }
  }
}
