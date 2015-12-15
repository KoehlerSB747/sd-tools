#!/usr/bin/python
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

import sys
import os
sys.path.append(os.path.dirname(os.path.realpath(__file__).replace('test', 'main')))

import unittest
from StatsAccumulator import StatsAccumulator


class TestStatsAccumulator(unittest.TestCase):

    def test_empty(self):
        s = StatsAccumulator("empty")
        self.assertEqual({"label": "empty", "maximum": 0.0, "mean": 0, "minimum": 0.0, "n": 0, "stddev": 0},
                         s.summaryInfo());

    def test_basics(self):
        s = StatsAccumulator("basics")
        s.add(1, 2, 3)

        self.assertEqual("basics", s.label)
        self.assertEqual(3, s.n)
        self.assertEqual(1, s.minimum)
        self.assertEqual(3, s.maximum)
        self.assertEqual(2.0, s.mean)
        self.assertEqual(1.0, s.standardDeviation)
        self.assertEqual(1.0, s.variance)

    def test_combine(self):
        s1 = StatsAccumulator("1")
        s1.add(1, 2, 3)
        s2 = StatsAccumulator("2")
        s2.add(4, 5, 6)
        s3 = StatsAccumulator("3")
        s3.add(1, 2, 3, 4, 5, 6)

        s = StatsAccumulator.combine("3", s1, s2)
        self.assertEqual(s3.summaryInfo(), s.summaryInfo())

    def test_copy_constructor(self):
        s1 = StatsAccumulator("1")
        s1.add(1, 2, 3)
        s_copy = StatsAccumulator("1", s1)
        self.assertEqual(s1.summaryInfo(), s_copy.summaryInfo())

    def test_initialize_from_dict(self):
        s1 = StatsAccumulator("1")
        s1.add(1, 2, 3)
        summaryInfo = s1.summaryInfo()

        s_other = StatsAccumulator()
        s_other.initialize(summaryInfo=summaryInfo)
        
        self.assertEqual("1", s_other.label)
        self.assertEqual(3, s_other.n)
        self.assertEqual(1, s_other.minimum)
        self.assertEqual(3, s_other.maximum)
        self.assertEqual(2.0, s_other.mean)
        self.assertEqual(1.0, s_other.standardDeviation)
        self.assertEqual(1.0, s_other.variance)

    def test_floating_point(self):
        s = StatsAccumulator("basics")
        s.add(1.0, 2.0, 3.0)

        self.assertEqual("basics", s.label)
        self.assertEqual(3.0, s.n)
        self.assertEqual(1.0, s.minimum)
        self.assertEqual(3.0, s.maximum)
        self.assertEqual(2.0, s.mean)
        self.assertEqual(1.0, s.standardDeviation)
        self.assertEqual(1.0, s.variance)

if __name__ == '__main__':
    unittest.main()
