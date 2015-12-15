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

import random
import time
import unittest
from RollingStats import RollingStats
from StatsAccumulator import StatsAccumulator


class TestRollingStats(unittest.TestCase):

    def test_simple1(self):
        rollingStats = RollingStats(100, 50)
#        import pdb; pdb.set_trace()

        stats1 = StatsAccumulator()
        seg2value = -1

        # add stats during segment 0
        while rollingStats.lastSegment == 0:
            value = random.uniform(1, 1000)
            segNum = rollingStats.add(value)
            stats1.add(value)
            if segNum != 0:
                seg2value = value
                break

        window0Stats = rollingStats.windowStats

        # don't add anything during segment1
        while rollingStats.getCurrentSegment() != 0:
            time.sleep(0.001)

        # get window stats after rolling beyond segment1. (should match seg2value)
        window1Stats = rollingStats.windowStats

        # don't add anything during segment0
        while rollingStats.curSegment == 0:
            time.sleep(0.001)

        # window stats should now be empty
        emptyStats = rollingStats.windowStats

        # do checks
        self.assertEquals(2, rollingStats.numSegments)

        # window0Stats should match stats1
        self.assertEqual(stats1.n, window0Stats.n)
        self.assertEqual(stats1.mean, window0Stats.mean, 0.005)

        # window1Stats should match seg2value
        if seg2value < 0:
            # window1Stats should be empty
            self.assertEqual(0, window1Stats.n)
        else:
            # window1Stats should have 1 value: seg2value
            self.assertEqual(1, window1Stats.n)
            self.assertEqual(seg2value, window1Stats.mean, 0.005)

        # emptyStats should be empty
        self.assertEqual(0, emptyStats.n)

        # cumulativeStats should match stats1
        cumulativeStats = rollingStats.cumulativeStats
        self.assertEqual(stats1.n, cumulativeStats.n)
        self.assertEqual(stats1.mean, cumulativeStats.mean, 0.005)

        # exercise building summary info dictionary, value doesn't matter
        summaryInfo = rollingStats.summaryInfo()
        import json; print json.dumps(summaryInfo, indent=4)

if __name__ == '__main__':
    unittest.main()
