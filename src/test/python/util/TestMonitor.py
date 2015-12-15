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
from datetime import datetime
from Monitor import Monitor


class TestMonitor(unittest.TestCase):

    def waitForRandomMillis(self, uptomillis):
        waittime = random.uniform(1, uptomillis)
        time.sleep(waittime / 1000)
        return waittime

    def testBasicsWithProcessing(self):
        monitor = Monitor("testBasics", defaultWindowWidth=100, defaultSegmentWidth=50)
        for i in range(5):
            starttime = datetime.now()
            waittime = self.waitForRandomMillis(55)
            endtime = datetime.now()
            monitor.mark(starttime, endtime)

        # Default stats are processingCumulativeStats
        self.assertEqual(monitor.processingCumulativeStats, monitor.getStats())

        # Make sure getStats behaves as advertised
        self.assertEqual(monitor.processingCumulativeStats, monitor.getStats(access=False, window=False))
        self.assertNotEqual(monitor.processingCumulativeStats, monitor.getStats(access=False, window=True))
        self.assertEqual(monitor.accessCumulativeStats, monitor.getStats(access=True, window=False))
        self.assertNotEqual(monitor.accessCumulativeStats, monitor.getStats(access=True, window=True))

        cumulativeProcessingStats = monitor.getStats()
        self.assertEqual(5, cumulativeProcessingStats.n)

        # exercise building summary info dictionary, value doesn't matter
        time.sleep(0.051)  # ensure window stats is different from cumulative
        summaryInfo = monitor.summaryInfo()
        import json; print json.dumps(summaryInfo, indent=4)
        

    def testBasicsWithAccessOnly(self):
        monitor = Monitor("testAccess")
        for i in range(5):
            starttime = datetime.now()
            waittime = self.waitForRandomMillis(55)
            monitor.mark(starttime)

        self.assertIsNone(monitor.processingCumulativeStats)
        self.assertIsNone(monitor.processingWindowStats)
        self.assertIsNotNone(monitor.accessCumulativeStats)
        self.assertIsNotNone(monitor.accessWindowStats)

        cumulativeAccessStats = monitor.getStats(True, False)
        self.assertEqual(4, cumulativeAccessStats.n)

        # exercise building summary info dictionary, value doesn't matter
        time.sleep(0.051)  # ensure window stats is different from cumulative
        summaryInfo = monitor.summaryInfo()
        import json; print json.dumps(summaryInfo, indent=4)


if __name__ == '__main__':
    unittest.main()
