#
#   Copyright 2008-2015 Semantic Discovery, Inc.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
import math

class LinearRegression:
    '''
    A low-memory helper class to collect (x,y) samples and compute the
    linear regression.
    '''

    def __init__(self):
        self._n = 0
        self._x_sum = 0.0
        self._y_sum = 0.0
        self._xy_sum = 0.0
        self._xx_sum = 0.0
        self._yy_sum = 0.0

        self._m = None
        self._b = None

    @property
    def n(self):
        return self._n

    @property
    def x_sum(self):
        return self._x_sum

    @property
    def y_sum(self):
        return self._y_sum

    @property
    def xy_sum(self):
        return self._xy_sum

    @property
    def xx_sum(self):
        return self._xx_sum

    @property
    def yy_sum(self):
        return self._yy_sum

    @property
    def m(self):
        return self.getM()

    @property
    def b(self):
        return self.getB()

    def getY(self, x):
        return self.getM() * x + self.getB()

    def add(self, x, y):
        self._m = None
        self._b = None
        self._n += 1
        self._x_sum += x
        self._y_sum += y
        self._xy_sum += (x * y)
        self._xx_sum += (x * x)
        self._yy_sum += (y * y)

    def __str__(self):
        return 'y = %.4f x + %.4f' % (self.getM(), self.getB())

    def getM(self):
        if (self._m is None):
            self._computeRegression()
        return self._m

    def getB(self):
        if (self._b is None):
            self._computeRegression()
        return self._b

    def _computeRegression(self):
        self._m = 0.0
        self._b = 0.0

        denominator = self._n * self._xx_sum - self._x_sum * self._x_sum
        if (denominator != 0):
            m_numerator = self._n * self._xy_sum - self._x_sum * self._y_sum
            b_numerator = self._y_sum * self._xx_sum - self._x_sum * self._xy_sum

            self._m = m_numerator / denominator
            self._b = b_numerator / denominator
