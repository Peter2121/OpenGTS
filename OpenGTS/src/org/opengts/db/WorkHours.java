// ----------------------------------------------------------------------------
// Copyright 2007-2014, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2009/11/10  Martin D. Flynn
//     -Initial release
//  2011/12/06  Martin D. Flynn
//     -Added method "countWorkHours"
//  2013/04/08  Martin D. Flynn
//     -Added support for discontiguous time-range (ie. "18:00-06:00)
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class WorkHours
{

    // ------------------------------------------------------------------------
    
    public  static final int    DAYS_IN_WEEK    = 7;

    public  static final String DFT             = "dft";

    public  static final String SUN             = "sun";
    public  static final String MON             = "mon";
    public  static final String TUE             = "tue";
    public  static final String WED             = "wed";
    public  static final String THU             = "thu";
    public  static final String FRI             = "fri";
    public  static final String SAT             = "sat";
    
    public  static final String DOW_NAME[] = {
        SUN,
        MON,
        TUE,
        WED,
        THU,
        FRI,
        SAT
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String PROP_WorkHours_minuteInterval             = "WorkHours.minuteInterval";
    public  static final String PROP_WorkHours_includeHourMinuteSeparator = "WorkHours.includeHourMinuteSeparator";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final int    MINUTES_PER_HOUR              = 60;
    private static final int    MINUTES_PER_DAY               = 24 * MINUTES_PER_HOUR; // 1440
    private static final int    DEFAULT_MINUTE_INTERVAL       = 15; // divisor of 60

    private static       int    MINUTE_INTERVAL               = -1; // initialized later

    /**
    *** Gets the configured Minute Interval.<br>
    *** The returned value is guaranteed to be a divisor of 60.
    *** (ie. 1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60)
    *** @return The Minute interval
    **/
    private static int GetMinuteInterval()
    {
        if (MINUTE_INTERVAL <= 0) {
            int mi = RTConfig.getInt(PROP_WorkHours_minuteInterval, DEFAULT_MINUTE_INTERVAL);
            MINUTE_INTERVAL = CalculateBestMinuteInterval(mi);
            // MINUTE_INTERVAL will be one of [1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60]
        }
        return MINUTE_INTERVAL;
    }

    /**
    *** Returns the minute-interval less-than-or-equals-to the specified interval, such
    *** that ((60 % Interval) == 0) is true.
    **/
    private static int CalculateBestMinuteInterval(int interval)
    {
        // returned value will be one of [1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60]
        if (interval <= 0) {
            // return default
            return DEFAULT_MINUTE_INTERVAL;
        } else
        if (interval >= 60) {
            // must be <= 1 hour
            return 60;  
        } else
        if ((60 % interval) != 0) {
            // find the next smaller value of interval, such that (60 % interval) == 0)
            //return 60 / (int)Math.ceil(60.0/(double)interval);  // close, but not quite, fails for 7,8,9
            //return 60 / ((60 / interval) + 1);
            while ((60 % --interval) != 0);
            return interval;
        } else {
            // we are fine as-is
            return interval;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final char    TIMERANGE_SEPARATOR_CHAR    = '-'; // "-"
    public static final char    HOUR_MINUTE_SEPARATOR_CHAR  = ':'; // ":"

    /**
    *** Parse specified "HH:MM" string and return the integer value for the 
    *** minute of the day.  "HH" should be in the range 00 to 24, and "MM" should
    *** be in the range 00 to 59.  The "MM" specification is optional, and will 
    *** be assumed to be 00 if omitted.
    *** @param hhmm  The "HH:MM" String to parse.
    *** @param endTime  True if parsing the end/stop time (only used if "HH:MM"
    ***    does not contain a fully parsable value).
    *** @return The minute of the day, in the range 0 to 1440.
    **/
    private static int convertHourMinuteToTOD(String hhmm, boolean endTime)
    {

        /* blank time specification? */
        if (StringTools.isBlank(hhmm)) {
            return endTime? MINUTES_PER_DAY : 0;
        }

        /* separate HH/MM components */
        String HH, MM;
        if (StringTools.isInt(hhmm,true)) {
            // "HHMM"
            int L = hhmm.length();
            if (L >= 4) { 
                // HHMM... ==> HH:MM
                HH = hhmm.substring(0,2);
                MM = hhmm.substring(2,4);
            } else
            if (L == 3) { 
                // HHM ==> HH:0M
                HH = hhmm.substring(0,2); // (0,1)
                MM = hhmm.substring(2,3); // (1,3)
            } else { 
                // HH ==> HH:00
                HH = hhmm;
                MM = "";
            }
        } else {
            // "HH:MM"
            int p = hhmm.indexOf(HOUR_MINUTE_SEPARATOR_CHAR);
            HH = (p >= 0)? hhmm.substring(0,p) : hhmm;
            MM = (p >= 0)? hhmm.substring(p+1) : "";
        }

        /* adjust hour */
        int hh = StringTools.parseInt(HH, (endTime?24:0));
        if (hh < 0) { 
            // negative value specified? set to 0
            hh =  0;
        } else
        if (hh > 24) {
            // invalid hour specified. set to maximum 24
            hh = 24;
        }

        /* adjust minute */
        int mm = StringTools.parseInt(MM, 0);
        if (mm < 0) { 
            // negative value specified? set to 0
            mm =  0;
        } else
        if (mm > 59) {
            // invalid minute specified. set to maximum 59
            mm = 59;
        }

        /* calculate/return time-of-day */
        int tod = (hh * 60) + mm;
        if (tod <= 0) { 
            return endTime? MINUTES_PER_DAY : 0; // startTime=00:00, endTime=24:00
        } else
        if (tod >= MINUTES_PER_DAY) { 
            return endTime? MINUTES_PER_DAY : 0; // startTime=00:00, endTime=24:00
        } else {
            return tod;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return formated String for specified time-of-day
    **/
    public static String formatTOD(int tod, boolean inclSep) 
    {
        return WorkHours.formatTOD(tod,inclSep,new StringBuffer()).toString();
    }

    /**
    *** Return formated String for specified time-of-day
    **/
    private static StringBuffer formatTOD(int tod, boolean inclSep, StringBuffer sb) 
    {
        if (tod >= 0) {
            int H = tod / 60; // hour of the day
            int M = tod % 60; // minute within the hour
            sb.append(StringTools.format(H,"00"));
            if (inclSep) { sb.append(HOUR_MINUTE_SEPARATOR_CHAR); }
            sb.append(StringTools.format(M,"00"));
        }
        return sb;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean INCLUDE_HOUR_MIN_SEPARATOR   = false;

    /**
    *** Day
    **/
    public static class Day
    {
        private int  minuteInterval;
        private byte todSeg[];
        // default constructor
        public Day() {
            this.minuteInterval = WorkHours.GetMinuteInterval();
            // ((MINUTES_PER_DAY % this.minuteInterval) == 0) guaranteed
            this.todSeg = new byte[MINUTES_PER_DAY / this.minuteInterval];
            for (int i = 0; i < this.todSeg.length; i++) { this.todSeg[i] = (byte)0; }
        }
        // time range constructor
        public Day(int todStartMin, int todEndMin) {
            this();
            if (todStartMin == todEndMin) {
                // assume 24 hours
                for (int i = 0      ; i < this.todSeg.length; i++) { this.todSeg[i] = (byte)1; }
            } else 
            if (todEndMin >= todStartMin) {
                // contiguous time during current day (ie. 06:00-18:00)
                int tsNdx = this.getIntervalIndex(todStartMin);
                int teNdx = this.getIntervalIndex(todEndMin - 1);
                for (int i = 0      ; i <  tsNdx            ; i++) { this.todSeg[i] = (byte)0; }
                for (int i = tsNdx  ; i <= teNdx            ; i++) { this.todSeg[i] = (byte)1; }
                for (int i = teNdx+1; i < this.todSeg.length; i++) { this.todSeg[i] = (byte)0; }
            } else {
                // discontiguous time during current day (ie. 18:00-06:00)
                int tsNdx = this.getIntervalIndex(todStartMin);
                int teNdx = this.getIntervalIndex(todEndMin - 1);
                for (int i = 0      ; i <= teNdx            ; i++) { this.todSeg[i] = (byte)1; }
                for (int i = teNdx+1; i <  tsNdx            ; i++) { this.todSeg[i] = (byte)0; }
                for (int i = tsNdx  ; i < this.todSeg.length; i++) { this.todSeg[i] = (byte)1; }
            }
        }
        // copy constructor
        public Day(Day day) {
            this();
            if (day != null) {
                System.arraycopy(day.todSeg, 0, this.todSeg, 0, this.todSeg.length);
            }
        }
        // gets the minute-interval used by this day instance
        private int getMinuteInterval() {
            // returns one of [1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60]
            return this.minuteInterval;
        }
        // convert time-of-day minute to segment interval index
        private int getIntervalIndex(int todMin) {
            int ndx = todMin / this.getMinuteInterval();
            if (ndx < 0) { 
                return 0; 
            } else
            if (ndx >= this.todSeg.length) { 
                return this.todSeg.length - 1; 
            } else {
                return ndx;
            }
        }
        // check for work-hour match
        public boolean isMatch(int todMin) {
            int todNdx = this.getIntervalIndex(todMin);
            return (this.todSeg[todNdx] != (byte)0);
        }
        // find the next TOD index that matches 'working'
        public int getNextMatch(int todMin, boolean working)
        {
            int todNdx = this.getIntervalIndex(todMin);
            for (int i = todNdx; i < this.todSeg.length; i++) {
                boolean match = (this.todSeg[todNdx] != (byte)0);
                if (working == match) {
                    int newTodNdx = i * this.getMinuteInterval();
                    return (newTodNdx > todNdx)? newTodNdx : todNdx;
                }
            }
            return -1;
        }
        // count work minutes for current day
        public int countWorkMinutes(int todStartMin, int todEndMin) {
            // validate values
            if (todStartMin < 0) { todStartMin = 0; }
            if (todEndMin > MINUTES_PER_DAY) { todEndMin = MINUTES_PER_DAY; }
            if (todEndMin < todStartMin) {
                return 0;
            }
            // starting interval index
            int accumMin = 0;
            int startNdx = this.getIntervalIndex(todStartMin);
            //Print.logInfo("Starting at index: " + startNdx);
            for (int N = startNdx; N < this.todSeg.length; N++) {
                if (this.todSeg[N] != (byte)0) { // work-hour segment
                    int todS = N * this.getMinuteInterval();
                    int todE = todS + this.getMinuteInterval();
                    if (todE > MINUTES_PER_DAY) { todE = MINUTES_PER_DAY; } // beyond end of day
                    if (todS < todStartMin) { 
                        todS = todStartMin; // move segment start min up to start time
                    }
                    if (todE >= todEndMin) { 
                        accumMin += (todEndMin - todS); // we are at the end of the time period
                        break;
                    }
                    accumMin += (todE - todS); // count full interval
                }
            }
            return accumMin;
        }
        // TOD time range (ie. "0600-1800", "1800-0600"
        public String getTimeRange() {
            boolean inclHourMinSep = RTConfig.getBoolean(PROP_WorkHours_includeHourMinuteSeparator,INCLUDE_HOUR_MIN_SEPARATOR);
            // Note: currently only supports a single time-range (ie. "0600-1800" or "1800-0600")
            int i = 0;
            if ((this.todSeg[i] == (byte)0) || (this.todSeg[this.todSeg.length-1] == (byte)0)) {
                // [010,01,10] contiguous work hours during current day
                // - start time
                int startTOD = -1;
                for (;i < this.todSeg.length; i++) {
                    if (this.todSeg[i] != (byte)0) {
                        startTOD = i * this.getMinuteInterval();
                        break;
                    }
                }
                // - end time
                int endTOD = -1;
                for (;i < this.todSeg.length; i++) {
                    if (this.todSeg[i] == (byte)0) {
                        endTOD = i * this.getMinuteInterval();
                        break;
                    }
                }
                // - assemble
                if (startTOD < 0) {
                    return ""; // not operating today
                } else {
                    StringBuffer sb = new StringBuffer();
                    WorkHours.formatTOD(startTOD, inclHourMinSep, sb);
                    sb.append(TIMERANGE_SEPARATOR_CHAR);
                    if (endTOD >= startTOD) {
                        WorkHours.formatTOD(endTOD, inclHourMinSep, sb);
                    } else {
                        WorkHours.formatTOD(MINUTES_PER_DAY, inclHourMinSep, sb);
                    }
                    return sb.toString();
                }
            } else {
                // [101,11] discontiguous work hours during current day
                // - end time
                int endTOD = -1;
                for (;i < this.todSeg.length; i++) {
                    if (this.todSeg[i] == (byte)0) {
                        endTOD = i * this.getMinuteInterval();
                        break;
                    }
                }
                // - start time
                int startTOD = -1;
                for (;i < this.todSeg.length; i++) {
                    if (this.todSeg[i] != (byte)0) {
                        startTOD = i * this.getMinuteInterval();
                        break;
                    }
                }
                // - assemble
                StringBuffer sb = new StringBuffer();
                if (endTOD < 0) {
                    // operating all day
                    WorkHours.formatTOD(0, inclHourMinSep, sb);
                    sb.append(TIMERANGE_SEPARATOR_CHAR);
                    WorkHours.formatTOD(MINUTES_PER_DAY, inclHourMinSep, sb);
                } else {
                    // operating a portion of the day
                    if (startTOD >= 0) {
                        WorkHours.formatTOD(startTOD, inclHourMinSep, sb);
                    } else {
                        WorkHours.formatTOD(0, inclHourMinSep, sb);
                    }
                    sb.append(TIMERANGE_SEPARATOR_CHAR);
                    WorkHours.formatTOD(endTOD, inclHourMinSep, sb);
                }
                return sb.toString();
            }
        }
        // string representation
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < this.todSeg.length; i++) {
                sb.append((this.todSeg[i] != (byte)0)? "1" : "-");
            }
            return sb.toString();
        }
    }
   
    // ------------------------------------------------------------------------

    private Day dowDay[] = new Day[DAYS_IN_WEEK];

    /**
    *** Constructor
    **/
    public WorkHours(Day day[])
    {
        Day last = !ListTools.isEmpty(day)? day[day.length - 1] : null;
        int dlen = !ListTools.isEmpty(day)? day.length : 0;
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            this.dowDay[i] = new Day((i < dlen)? day[i] : last);
        }
    }

    /**
    *** Constructor
    **/
    // Commented to encourage specifying the specific RTProperty source
    //public WorkHours(String keyPrefix)
    //{
    //    this(null, keyPrefix);
    //}

    /**
    *** Constructor
    **/
    public WorkHours(RTConfig.PropertyGetter dayRTP)
    {
        this(dayRTP, null);
    }

    /**
    *** Constructor
    **/
    public WorkHours(RTConfig.PropertyGetter dayRTP, String keyPrefix)
    {

        /* init */
        keyPrefix = StringTools.trim(keyPrefix);
        if (dayRTP == null) { 
            // default runtime-property getter source
            dayRTP = RTConfig.getPropertyGetter(); // not null
        }

        /* default time range */
        String dftTR = StringTools.trim(dayRTP.getProperty((keyPrefix+DFT),""));

        /* init days */
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            // sun=08:00-17:00
            String dowKey = keyPrefix + DOW_NAME[i]; // DateTime.getDayName(i,1).toLowerCase();
            String TR     = StringTools.trim(dayRTP.getProperty(dowKey,dftTR));
            if (!StringTools.isBlank(TR)) {
                int p = TR.indexOf(TIMERANGE_SEPARATOR_CHAR);
                String frTm = (p >= 0)? TR.substring(0,p) : TR;
                String toTm = (p >= 0)? TR.substring(p+1) : "";
                int   frTod = this.convertHourMinuteToTOD(frTm,false);
                int   toTod = this.convertHourMinuteToTOD(toTm,true );
                this.dowDay[i] = new Day(frTod, toTod);
            } else {
                this.dowDay[i] = new Day();
            }
        }

    }

    /**
    *** Constructor
    *** @param keyPrefix Property key prefix
    *** @deprecated (currently only required for legacy ENRE interface)
    **/
    @Deprecated
    public WorkHours(String keyPrefix)
    {
        this(RTConfig.getPropertyGetter(), keyPrefix);
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the requested Day
    **/
    public Day getDay(int dow)
    {
        if ((dow < 0) || (dow >= DAYS_IN_WEEK)) {
            return null;
        } else {
            return this.dowDay[dow];
        }
    }

    /**
    *** Return the requested Day
    **/
    public Day getDay(DateTime dateTime)
    {
        return this.getDay(dateTime, null);
    }

    /**
    *** Return the requested Day
    **/
    public Day getDay(DateTime dateTime, TimeZone tz)
    {
        if (dateTime == null) {
            return null;
        } else {
            int dow = dateTime.getDayOfWeek(tz);
            return this.getDay(dow);
        }
    }

    /**
    *** Return the requested Day
    **/
    public Day getDay(long timestamp, TimeZone tz)
    {
        if (timestamp <= 0L) {
            return null;
        } else {
            int dow = (new DateTime(timestamp,tz)).getDayOfWeek(tz);
            return this.getDay(dow);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** True if the specified DateTime is within the current 'WorkingHours'
    **/
    public boolean isMatch(DateTime dateTime)
    {
        return this.isMatch(dateTime, null);
    }

    /**
    *** True if the specified DateTime is within the current 'WorkingHours'
    **/
    public boolean isMatch(DateTime dateTime, TimeZone tz)
    {
        if (dateTime != null) {
            int dow = dateTime.getDayOfWeek(tz);
            Day day = this.dowDay[dow];
            int tod = (dateTime.getHour24(tz) * 60) + dateTime.getMinute(tz);
            //Print.logInfo("WorkHours: " + this.getProperties());
            //Print.logInfo("Test TOD : " + day.formatTOD(tod,true));
            return day.isMatch(tod);
        } else {
            return false;
        }
    }

    /**
    *** True if the specified DateTime is within the current 'WorkingHours'
    **/
    public boolean isMatch(long timestamp, TimeZone tz)
    {
        if (timestamp > 0L) {
            return this.isMatch(new DateTime(timestamp,tz), tz);
        } else {
            return false;
        }
    }

    /**
    *** True if the specified time-of-day is within the current 'WorkingHours'
    **/
    public boolean isMatch(int dow, int tod)
    {
        if ((dow >= 0) && (dow < this.dowDay.length)) {
            Day day = this.dowDay[dow];
            return day.isMatch(tod);
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Accumulate all work-hour time between the specified dates
    **/
    public double countWorkHours(DateTime startDT, DateTime stopDT, TimeZone tz)
    {

        /* invalid time specification? */
        if (startDT == null) {
            Print.logWarn("Start date is null");
            return 0.0;
        } else
        if (stopDT == null) {
            Print.logWarn("Stop date is null");
            return 0.0;
        } else
        if (startDT.isAfter(stopDT,true)) {
            Print.logWarn("Start date is after stop date");
            Print.logInfo("Start date: " + startDT);
            Print.logInfo("Stop  date: " + stopDT);
            return 0.0;
        }

        /* timezone */
        if (tz == null) {
            tz = startDT.getTimeZone();
        }

        /* start */
        long startSec   = startDT.getTimeSec();
        long startDS    = startDT.getDayStart(tz);
        long startDE    = startDT.getDayEnd(tz);
        int  startMOD   = startDT.getSecondOfDay(tz) / 60; // round down
        int  startDOW   = startDT.getDayOfWeek(tz);
        Day  startDay   = this.getDay(startDOW);

        /* stop */
        long stopSec    = stopDT.getTimeSec();
        long stopDS     = stopDT.getDayStart(tz);
        long stopDE     = stopDT.getDayEnd(tz);
        int  stopMOD    = (stopDT.getSecondOfDay(tz) + 59) / 60; // round up

        /* simple case: same day? */
        if (startDE >= stopSec) {
            //Print.logInfo("Same Day ...");
            //Print.logInfo("Start MOD: " + startMOD);
            //Print.logInfo("Stop  MOD: " + stopMOD);
            return (double)startDay.countWorkMinutes(startMOD, stopMOD) / 60.0;
        }

        /* count work minutes */
        long accumMin = 0L;
        long timeSec = startDS + DateTime.DaySeconds(1); // next day
        for (;;) {
            Day day = this.getDay(timeSec,tz);
            long DE = timeSec + DateTime.DaySeconds(1) - 1; // end of day
            if (DE >= stopSec) {
                // last day
                accumMin += day.countWorkMinutes(0,stopMOD);
                break;
            }
            accumMin += day.countWorkMinutes(0,MINUTES_PER_DAY);
            timeSec += DateTime.DaySeconds(1);
        }
        return (double)accumMin / 60.0;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the WorkHours instance as an RTProperties
    *** @return The WorkHours instance as an RTProperties
    **/
    public RTProperties getProperties()
    {
        // sun= mon=06:00-18:00 tue=06:00-18:00 wed=06:00-18:00 thu=06:00-18:00 fri=06:00-18:00 sat=
        RTProperties rtp = new RTProperties();
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            String dow = (i < DAYS_IN_WEEK)? DOW_NAME[i] : "x";
            String tmr = this.dowDay[i].getTimeRange();
            rtp.setString(dow,tmr);
        }
        return rtp;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String repreentation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19  20  21  22  23  \n");
        sb.append("---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+\n");
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            sb.append(this.dowDay[i].toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_LIST[]          = new String[] { "list"     };
    private static final String ARG_RTP[]           = new String[] { "rtp"      };
    private static final String ARG_TIME[]          = new String[] { "time"     };
    private static final String ARG_MODTEST[]       = new String[] { "modtest"  };
    private static final String ARG_DAYTEST[]       = new String[] { "daytest"  };
    private static final String ARG_START_TIME[]    = new String[] { "startTime", "beginTime", "st" };
    private static final String ARG_STOP_TIME[]     = new String[] { "stopTime" , "endTime"  , "et" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        WorkHours wh = new WorkHours(RTConfig.getPropertyGetter(),RuleFactory.PROP_rule_workHours_);
        // sun= mon= tue= wed= thu= fri=13:00-14:00 sat=

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            Print.sysPrintln(wh.toString());
            System.exit(0);
        }

        /* rtp */
        if (RTConfig.hasProperty(ARG_RTP)) {
            Print.sysPrintln(wh.getProperties().toString());
            System.exit(0);
        }

        /* mod test */
        if (RTConfig.hasProperty(ARG_MODTEST)) {
            for (int i = 0; i <= 60; i++) {
                Print.sysPrintln(i + " ==> " + CalculateBestMinuteInterval(i));
            }
            System.exit(0);
        }

        /* day test */
        if (RTConfig.hasProperty(ARG_DAYTEST)) {
            TimeZone pstTZ = DateTime.getTimeZone("US/Pacific");
            DateTime dt = new DateTime(pstTZ);
            int dow = RTConfig.getInt(ARG_DAYTEST,dt.getDayOfWeek());
            int lastMatch = -1;
            Print.sysPrintln("DayOfWeek: " + DateTime.getDayName(dow,0));
            for (int todMin = 0; todMin <= MINUTES_PER_DAY + 1; todMin++) {
                boolean match = wh.isMatch(dow,todMin);
                if ((lastMatch < 0) || ((lastMatch == 1) != match)) {
                    Print.sysPrintln(" " + WorkHours.formatTOD(todMin,true) + " ==> " + match);
                    lastMatch = match? 1 : 0;
                }
            }
            System.exit(0);
        }

        /* time check */
        if (RTConfig.hasProperty(ARG_TIME)) {
            String time = RTConfig.getString(ARG_TIME,"");
            try {
                DateTime dt = !StringTools.isBlank(time)? DateTime.parseArgumentDate(RTConfig.getString(ARG_TIME,""),null,false) : new DateTime();
                Print.sysPrintln("Time     : " + dt.toString());
                Print.sysPrintln("IsMatch  : " + wh.isMatch(dt));
                Print.sysPrintln("WorkHours:");
                Print.sysPrintln(wh.toString());
            } catch (DateTime.DateParseException dpe) {
                Print.sysPrintln("Error: Unable to parse time - " + time);
            }
            System.exit(0);
        }

        /* time range */
        if (RTConfig.hasProperty(ARG_START_TIME)) {
            try{
                DateTime startDT = DateTime.parseArgumentDate(RTConfig.getString(ARG_START_TIME,""), null, false);
                DateTime stopDT  = DateTime.parseArgumentDate(RTConfig.getString(ARG_STOP_TIME ,""), null, false);
                double hrs = wh.countWorkHours(startDT, stopDT, null);
                Print.sysPrintln("Start Date/Time  : " + startDT);
                Print.sysPrintln("Stop Date/Time   : " + stopDT);
                Print.sysPrintln("Counted WorkHours: " + hrs);
                System.exit(0);
            } catch (DateTime.DateParseException dpe) {
                Print.logException("Invalid Date format",dpe);
                System.exit(99);
            }
        }

    }
    
}
