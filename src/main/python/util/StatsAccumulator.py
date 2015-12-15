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
import json
import math
from threading import Lock

class StatsAccumulator:
    '''
    A low-memory helper class to collect statistical samples and provide
    summary statistics.
    '''

    def __init__(self, label='', other=None):
        self._modlock = Lock()
        self.clear(label)

        if not other == None:
            if not label == '':
                self._label = other._label
            self._n = other._n
            self._minimum = other._minimum
            self._maximum = other._maximum
            self._sum = other._sum
            self._sos = other._sos

    @property
    def label(self):
        return self._label

    @label.setter
    def label(self, val):
        self._label = val

    @property
    def n(self):
        return self._n

    @property
    def minimum(self):
        return self._minimum

    @property
    def maximum(self):
        return self._maximum

    @property
    def sum(self):
        return self._sum

    @property
    def sumOfSquares(self):
        return self._sos

    @property
    def mean(self):
        return self.getMean()

    @property
    def standardDeviation(self):
        return self.getStandardDeviation()

    @property
    def variance(self):
        return self.getVariance()


    def clear(self, label=''):
        self._modlock.acquire()
        try:
            self._label = label
            self._n = 0
            self._minimum = 0.0
            self._maximum = 0.0
            self._sum = 0.0
            self._sos = 0.0
        finally:
            self._modlock.release()

    def initialize(self, label='', n=0, minimum=0, maximum=0, mean=0, stddev=0, summaryInfo=None):
        '''
        Initialize with the given values, preferring existing values from the dictionary.
        '''
        if summaryInfo is not None:
            if 'label' in summaryInfo:
                label = summaryInfo['label']
            if 'n' in summaryInfo:
                n = summaryInfo['n']
            if 'minimum' in summaryInfo:
                minimum = summaryInfo['minimum']
            if 'maximum' in summaryInfo:
                maximum = summaryInfo['maximum']
            if 'mean' in summaryInfo:
                mean = summaryInfo['mean']
            if 'stddev' in summaryInfo:
                stddev = summaryInfo['stddev']

        self._modlock.acquire()
        try:
            self._label = label
            self._n = n
            self._minimum = minimum
            self._maximum = maximum
            self._sum = mean * n
            self._sos = 0 if n == 0 else stddev * stddev * (n - 1.0) + self._sum * self._sum / n
        finally:
            self._modlock.release()

    def summaryInfo(self):
        '''
        Get a dictionary containing a summary of this instance's information.
        '''
        result = {
            'label': self.label,
            'n': self.n,
            'minimum': self.minimum,
            'maximum': self.maximum,
            'mean': self.mean,
            'stddev': self.standardDeviation
        }
        return result

    def __str__(self):
        return json.dumps(self.summaryInfo(), sort_keys=True)

    def add(self, *values):
        for value in values:
            self._doAdd(value)

    def _doAdd(self, value):
        self._modlock.acquire()
        try:
           if self._n == 0:
                self._minimum = value
                self._maximum = value
           else:
                if value < self._minimum:
                    self._minimum = value
                if value > self._maximum:
                    self._maximum = value

           self._n += 1
           self._sos += (value * value)
           self._sum += value
        finally:
           self._modlock.release()

    def getMean(self):
        return 0 if self._n == 0 else self._sum / self._n

    def getStandardDeviation(self):
        return 0 if self._n < 2 else math.sqrt(self.variance)

    def getVariance(self):
        return 0 if self._n < 2 else (1.0 / (self._n - 1.0)) * (self._sos - (1.0 / self._n) * self._sum * self._sum)

    @staticmethod
    def combine(label, *statsAccumulators):
        '''
        Create a new statsAccumulator as if it had accumulated all data from
        the given list of stats accumulators.
        '''
        result = StatsAccumulator(label)
        for stats in statsAccumulators:
            result.incorporate(stats)

        return result

    def incorporate(self, other):
        '''
        Incorporate the other statsAccumulator's data into this as if this had
        accumulated the other's along with its own.
        '''
        if other is None:
            return
        
        self._modlock.acquire()
        try:
            if self._n == 0:
                self._minimum = other._minimum
                self._maximum = other._maximum
            else:
                if other._minimum < self._minimum:
                    self._minimum = other._minimum
                if other._maximum > self._maximum:
                    self._maximum = other._maximum
                        
            self._n += other._n
            self._sos += other._sos
            self._sum += other._sum
        finally:
            self._modlock.release()
