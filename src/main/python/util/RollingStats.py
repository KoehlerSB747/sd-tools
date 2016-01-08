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
from datetime import timedelta
from threading import Lock
from StatsAccumulator import StatsAccumulator

class RollingStats:
    '''
    Implementation for a collection of stats through a rolling window of time.
    '''

    def __init__(self, windowWidth=300000, segmentWidth=5000):
        self._modlock = Lock()
        self._windowWidth = windowWidth
        self._segmentWidth = segmentWidth
        self._windowDelta = timedelta(milliseconds=windowWidth)
        self._cumulativeStats = StatsAccumulator("cumulative")

        self._numSegments = int(round(windowWidth / segmentWidth))
        self._segmentStats = [StatsAccumulator("segment-" + str(i)) for i in range(self._numSegments)]
        self._starttime = datetime.now()
        self._reftime = self._starttime
        self._curSegment = 0

    @property
    def windowWidth(self):
        return self._windowWidth

    @property
    def numSegments(self):
        return self._numSegments

    @property
    def curSegment(self):
        return self.getCurrentSegment()

    @property
    def lastSegment(self):
        return self._curSegment

    @property
    def startTime(self):
        return self._starttime

    @property
    def refTime(self):
        return self._reftime

    @property
    def cumulativeStats(self):
        return self._cumulativeStats

    @property
    def windowStats(self):
        return self.getWindowStats()

    @property
    def currentLabel(self):
        return self.getCurrentLabel()


    def summaryInfo(self):
        '''
        Get a dictionary containing a summary of this instance's information.
        '''
        result = {'now': str(datetime.now())}

        cumulativeInfo = {'since': str(self.startTime)}
        self._addStatsInfo(self.cumulativeStats, cumulativeInfo)
        result['cumulative'] = cumulativeInfo

        windowInfo = {'widthMillis': self.windowWidth}
        currentWindowStats = self.getWindowStats()
        self._addStatsInfo(currentWindowStats, windowInfo)
        result['window'] = windowInfo

        return result

    def __str__(self):
        return json.dumps(self.summaryInfo(), sort_keys=True)

    def reset(self):
        self._modlock.acquire()
        try:
            self._starttime = datetime.now()
            self._reftime = starttime
            self._curSegment = 0
            for segment in self._segmentStats:
                segment.clear()
            self._cumulativeStats.clear()
        finally:
            self._modlock.release()

    def add(self, *values):
        '''
        Add the current value(s), returning the segment number to which the
        value as added (useful for testing).
        '''
        self._modlock.acquire()
        try:
            self._incToCurrentSegment()
            self._segmentStats[self._curSegment].add(*values)
            result = self._curSegment
            self._cumulativeStats.add(*values)
        finally:
            self._modlock.release()

        return result

    def hasWindowActivity(self):
        '''
        Determines whether the current window has activity, returning (hasActivity, currentWindowStats)
        '''
        windowStats = self.getWindowStats()
        return (windowStats.n > 0, windowStats)

    def getWindowStats(self):
        '''
        Get the stats for the current window.
        '''
        self._modlock.acquire()
        try:
            self._incToCurrentSegment()
            result = StatsAccumulator.combine(self.getCurrentLabel(), *self._segmentStats)
        finally:
            self._modlock.release()
        return result

    def getCurrentLabel(self):
        return('Window-%s-%s' % (str(self._reftime), str(self._windowDelta)))

    def getCurrentSegment(self):
        result = -1
        self._modlock.acquire()
        try:
            self._incToCurrentSegment()
            result = self._curSegment
        finally:
            self._modlock.release()
        return result

    def _incToCurrentSegment(self):
        result = datetime.now()
        segNum = int((self._getMillis(result, self._starttime) % self._windowWidth) / self._segmentWidth)
        
        diff = self._getMillis(result, self._reftime)

#        import pdb; pdb.set_trace()

        # if advanced to new segment or wrapped around current
        if segNum != self._curSegment or diff > self._segmentWidth:
            if self._numSegments == 1:
                # special case: wrapped around one and only segment in window
                self._segmentStats[0].clear()
            elif diff > self._windowWidth:
                # wrapped around the entire window, need to clear all
                for stats in self._segmentStats:
                    stats.clear()
                self._curSegment = segNum
            else:
                # walk up to and including new current segment, clearing each
                nextSegNum = (segNum + 1) % self._numSegments
                i = (self._curSegment + 1) % self._numSegments
                while i != nextSegNum:
                    self._segmentStats[i].clear()
                    i = (i + 1) % self._numSegments
                self._curSegment = segNum

        self._reftime = result
    
    def _getMillis(self, laterdatetime, earlierdatetime):
        return int((laterdatetime - earlierdatetime).total_seconds() * 1000)

    def _addStatsInfo(self, stats, info):
        '''
        Add stats summary information to the 'info' dict.
        '''
        if stats.n > 0:
            info['status'] = 'active'
            info['stats'] = stats.summaryInfo()
            info['millisPerItem'] = self.getMillisPerItem(stats)
            info['itemsPerMilli'] = self.getItemsPerMilli(stats)
        else:
            info['status'] = 'inactive'

    @staticmethod
    def getMillisPerItem(stats):
        '''
        Extract the (average) number of milliseconds per item from the stats.
        '''
        result = None

        if stats is not None:
            if stats.n > 0:
                result = stats.mean

        return result

    @staticmethod
    def getItemsPerMilli(stats):
        '''
        Extract the (average) number of items per millisecond from the stats.
        '''
        result = None

        if stats is not None:
            if stats.mean > 0:
                result = 1.0 / stats.mean

        return result
