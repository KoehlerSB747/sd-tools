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
from datetime import datetime
from threading import Lock
from RollingStats import RollingStats

class Monitor:
    '''
    A monitor tracks processing and/or access times and rates for some function.
    '''

    def __init__(self, description=None, accessTimes=None, processingTimes=None, defaultWindowWidth=300000, defaultSegmentWidth=5000):
        '''
        NOTE: accessTimes, processingTimes should be RollingStats instances.
        '''
        self._modlock = Lock()
        self._aliveSince = datetime.now()
        self._description = description
        self._lastStartTime = None
        self._accessTimes = accessTimes
        self._processingTimes = processingTimes

        # Defaults for when creating RollingStats from within this Monitor
        self._defaultWindowWidth = defaultWindowWidth
        self._defaultSegmentWidth = defaultSegmentWidth


    @property
    def aliveSince(self):
        return self._aliveSince

    @property
    def description(self):
        return self._description

    @description.setter
    def description(self, val):
        self._description = val

    @property
    def lastAccessTime(self):
        '''
        Get the time at which access was last recorded.
        '''
        return self._lastStartTime

    @property
    def accessTimes(self):
        '''
        Get the accessTimes (RollingStats).
        '''
        return self._accessTimes

    @property
    def processingTimes(self):
        '''
        Get the processingTimes (RollingStats).
        '''
        return self._processingTimes

    @property
    def accessCumulativeStats(self):
        return None if self._accessTimes is None else self._accessTimes.cumulativeStats

    @property
    def accessWindowStats(self):
        return None if self._accessTimes is None else self._accessTimes.windowStats

    @property
    def accessWindowWidth(self):
        return None if self._accessTimes is None else self._accessTimes.windowWidth

    @property
    def processingCumulativeStats(self):
        return None if self._processingTimes is None else self._processingTimes.cumulativeStats

    @property
    def processingWindowStats(self):
        return None if self._processingTimes is None else self._processingTimes.windowStats

    @property
    def processingWindowWidth(self):
        return None if self._processingTimes is None else self._processingTimes.windowWidth

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


    def summaryInfo(self):
        '''
        Get a dictionary containing a summary of this instance's information.
        '''
        result = {}

        result['aliveSince'] = str(self.aliveSince)
        if self.description is not None:
            result['description'] = self.description
        if self.lastAccessTime is not None:
            result['lastMark'] = str(self.lastAccessTime)
        if self.accessTimes is not None:
            result['accessStats'] = self.accessTimes.summaryInfo()
        if self.processingTimes is not None:
            result['processingStats'] = self.processingTimes.summaryInfo()

        return result

    def __str__(self):
        return json.dumps(self.summaryInfo(), sort_keys=True)

    def getStats(self, access=False, window=False):
        '''
        Get window/cumulative access/processing stats.
        '''
        result = None

        rollingStats = self._accessTimes if access else self._processingTimes
        if rollingStats is not None:
            result = rollingStats.windowStats if window else rollingStats.cumulativeStats

        return result

    def mark(self, starttime, endtime=None):
        '''
        Mark another access time, and processing time if endTime is not None.
        '''
        self._modlock.acquire()
        try:
            if self._lastStartTime is not None:
                if self._accessTimes is None:
                    # initialize if needed
                    self._accessTimes = RollingStats(self.defaultWindowWidth, self.defaultSegmentWidth)

                self._accessTimes.add(self._getMillis(starttime, self._lastStartTime))

            if endtime is not None:
                if self._processingTimes is None:
                    # initialize if needed
                    self._processingTimes = RollingStats(self.defaultWindowWidth, self.defaultSegmentWidth)

                self._processingTimes.add(self._getMillis(endtime, starttime))
                    
            self._lastStartTime = starttime

        finally:
            self._modlock.release()

    def _getMillis(self, laterdatetime, earlierdatetime):
        return int((laterdatetime - earlierdatetime).microseconds / 1000)

