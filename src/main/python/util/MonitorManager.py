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
from StatsAccumulator import StatsAccumulator
from Monitor import Monitor

class MonitorManager:
    '''
    Class to manage a set of monitors by label, providing rollup views across.
    '''

    def __init__(self, defaultWindowWidth=300000, defaultSegmentWidth=5000):
        self._monitors = {}
        self._keyManager = KeyManager()

        # Defaults for when creating a Monitor from within this MonitorManager
        self._defaultWindowWidth = defaultWindowWidth
        self._defaultSegmentWidth = defaultSegmentWidth


    @property
    def defaultWindowWidth(self):
        return self._defaultWindowWidth

    @defaultWindowWidth.setter
    def defaultWindowWidth(self, val):
        self._defaultWindowWidth = val

    @property
    def defaultSegmentWidth(self):
        return self._defaultSegmentWidth

    @defaultSegmentWidth.setter
    def defaultSegmentWidth(self, val):
        self._defaultSegmentWidth = val


    def getMonitors(self):
        return self._monitors

    def getMonitor(self, label, createIfMissing=False, description=None):
        result = None

        if label in self._monitors:
            result = self._monitors[label]

        elif createIfMissing:
            result = Monitor(description=description, defaultWindowWidth=self.defaultWindowWidth, defaultSegmentWidth=self.defaultSegmentWidth)
            self._monitors[label] = result

        return result

    def getOrCreateMonitorByKeyType(self, keytype, description):
        key = self._keyManager.getKey(keytype, description)
        return self.getMonitor(key, createIfMissing=True, description=description)

    def setMonitor(self, label, monitor):
        self._monitors[label] = monitor

    def getStats(self, label=None, access=False, window=False):
        '''
        Get window/cumulative access/processing stats for label or all.
        '''
        result = None

        if label is None:
            result = StatsAccumulator('rollup')

            # Combine access stats across all monitors
            for key, monitor in self._monitors.iteritems():
                result.incorporate(monitor.getStats(access, window))

        elif label in self._monitors:
            result = self._monitors[label].getStats(access, window)

        return result

    def summaryInfo(self):
        '''
        Get a dictionary containing a summary of this instance's information.
        '''
        result = {}

        # add overall processing/access, cumulative/window stats
        result['overallStats'] = self.getOverallStats()

        # add summaryInfo for each individual monitor
        for key, monitor in self._monitors.iteritems():
            result[key] = monitor.summaryInfo()

        return result

    def __str__(self):
        return json.dumps(self.summaryInfo(), sort_keys=True)

    def getOverallStats(self):
        result = {}

        cumulativeProcessing = self.getStats(access=False, window=False)
        if cumulativeProcessing is not None:
            result['cumulativeProcessing'] = cumulativeProcessing.summaryInfo()

        cumulativeAccess = self.getStats(access=True, window=False)
        if cumulativeAccess is not None:
            result['cumulativeAccess'] = cumulativeAccess.summaryInfo()

        windowProcessing = self.getStats(access=False, window=True)
        if windowProcessing is not None:
            result['windowProcessing'] = windowProcessing.summaryInfo()

        windowAccess = self.getStats(access=True, window=True)
        if windowAccess is not None:
            result['windowAccess'] = windowAccess.summaryInfo()

        return result

class KeyManager:
    '''
    Class to turn descriptions of key types into keys of the form type-N.
    '''

    def __init__(self):
        self._keytype2descriptions = {}

    def getKey(self, keytype, description):

        if keytype in self._keytype2descriptions:
            descriptions = self._keytype2descriptions[keytype]
        else:
            descriptions = []
            self._keytype2descriptions[keytype] = descriptions

        if description in descriptions:
            index = descriptions.index(description)
        else:
            index = len(descriptions)
            descriptions.append(description)

        result = '%s-%d' % (keytype, index)
        return result
