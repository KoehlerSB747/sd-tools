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
from MonitorManager import MonitorManager
from MonitorManager import KeyManager
from Monitor import Monitor


class TestMonitorManager(unittest.TestCase):

    def waitForRandomMillis(self, uptomillis):
        waittime = random.uniform(1, uptomillis)
        time.sleep(waittime / 1000)
        return waittime

    def simulateMonitorProcess(self, monitorManager, monitorLabel):
        monitor = monitorManager.getMonitor(monitorLabel, createIfMissing=True, description=None)
        starttime = datetime.now()
        waittime = self.waitForRandomMillis(55)
        endtime = datetime.now()
        monitor.mark(starttime, endtime)

    def simulateMonitorEvent(self, monitorManager, eventType, eventDescription):
        monitor = monitorManager.getOrCreateMonitorByKeyType(eventType, eventDescription)
        starttime = datetime.now()
        monitor.mark(starttime)

    def testBasics(self):
        monitorManager = MonitorManager(defaultWindowWidth=100, defaultSegmentWidth=50)

        self.simulateMonitorEvent(monitorManager, 'error', 'Intermittent error 1')
        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')
        self.simulateMonitorEvent(monitorManager, 'error', 'Intermittent error 2')
        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')
        self.simulateMonitorProcess(monitorManager, 'simulatedProcess1')
        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')
        self.simulateMonitorEvent(monitorManager, 'error', 'Intermittent error 3')
        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')

        self.simulateMonitorProcess(monitorManager, 'simulatedProcess2')

        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')
        self.simulateMonitorProcess(monitorManager, 'simulatedProcess1')
        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')
        self.simulateMonitorProcess(monitorManager, 'simulatedProcess1')
        self.simulateMonitorEvent(monitorManager, 'error', 'Recurring error')
        
        # exercise building summary info dictionary, value doesn't matter
        time.sleep(0.051)  # ensure window stats is different from cumulative
        summaryInfo = monitorManager.summaryInfo()
        import json; print json.dumps(summaryInfo, indent=4)
        

    def testKeyManager(self):
        keyManager = KeyManager()
        info0 = keyManager.getKey("info", "First info description")
        warn0 = keyManager.getKey("warn", "First warn description")
        error0 = keyManager.getKey("error", "First error description")

        warn1 = keyManager.getKey("warn", "Second warn description")
        error1 = keyManager.getKey("error", "Second error description")

        error2 = keyManager.getKey("error", "Third error description")

        self.assertEqual("info-0", info0)
        self.assertEqual("info-0", keyManager.getKey("info", "First info description"))

        self.assertEqual("warn-0", warn0)
        self.assertEqual("warn-0", keyManager.getKey("warn", "First warn description"))
        self.assertEqual("warn-1", warn1)
        self.assertEqual("warn-1", keyManager.getKey("warn", "Second warn description"))

        self.assertEqual("error-0", error0)
        self.assertEqual("error-0", keyManager.getKey("error", "First error description"))
        self.assertEqual("error-1", error1)
        self.assertEqual("error-1", keyManager.getKey("error", "Second error description"))
        self.assertEqual("error-2", error2)
        self.assertEqual("error-2", keyManager.getKey("error", "Third error description"))


if __name__ == '__main__':
    unittest.main()
