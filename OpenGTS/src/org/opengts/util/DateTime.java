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
// Description:
//  This class provides many Date/Time utilities
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/01/10  Martin D. Flynn
//     -Added 'getMonthStart'/'getMonthEnd' methods.
//  2008/02/17  Martin D. Flynn
//     -Added date format constants
//  2008/02/21  Martin D. Flynn
//     -Replaced date format constants with 'default' date/time formats
//  2008/03/12  Martin Dl Flynn
//     -Added method 'parseArgumentDate'
//  2008/04/11  Martin Dl Flynn
//     -Modified 'parseArgumentDate' to use the argument timezone when calculating
//      the default year/month/day.
//  2008/05/20  Martin Dl Flynn
//     -Modified 'parseArgumentDate' to support additional date/time formats
//  2008/07/20  Martin Dl Flynn
//     -Extracted 'parseDateTime' from 'parseArgumentDate'
//     -Added methods for returning the days since October 15, 1582 (Gregorian calendar)
//  2009/01/28  Martin D. Flynn
//     -Fixed bug in ParsedDateTime that didn't correctly initialize the timezone.
//  2009/11/01  Martin D. Flynn
//     -Changed Second/Millisecond threshold in DateTime(long) constructor from
//      3000000000 to 9000000000.  This sets the lower limit to 9000001 seconds,
//      or "Apr 15 04:00:01 GMT 1970", and the upper limit to 9000000000 seconds,
//      or "Mar 14 16:00:00 GMT 2255" (well beyond reasonable expectation).
//  2010/07/04  Martin D. Flynn
//     -Added ability to parse date string format which specifies a delta number of
//      days into the future or past.  IE "-4d", "-5", "+2,12:34:56"
//  2011/07/15  Martin D. Flynn
//     -Added '|' to date/time group separator when parsing date/time strings
//     -Added "CompactDateTimeFormat" method
//  2011/08/21  Martin D. Flynn
//     -Modified "setDate(...,year,mon1,day)" to retain current time.
//  2011/10/03  Martin D. Flynn
//     -Support parsing DST timezones (previously parsing "PDT" would fail).
//  2011/12/06  Martin D. Flynn
//     -Added support for "{+|-}DAY" parsing to "parseDateTime"
//  2012/02/03  Martin D. Flynn
//     -Added "getSecondOfDay"
//  2012/04/26  Martin D. Flynn
//     -Modified "YYYY-MM-DDThh:mm:ssZZZ" format (ISO-8601) to handle general timzone ids
//  2012/12/24  Martin D. Flynn
//     -Added "YYYYMMDDhhmmss" date/time format to "parseDateTime"
//     -Fixed detection of valid GMT+0 timezones.
//  2013/04/08  Martin D. Flynn
//     -Added "getCurrentDayNumber"
//  2013/05/28  Martin D. Flynn
//     -Added "parseDateTime" support for parsing delta months/years (ie. "+2m", "-1y", etc)
//  2013/08/06  Martin D. Flynn
//     -Fixed NPE in "parseDateTime" (when Epoch time specification is invalid).
//  2013/09/20  Martin D. Flynn
//     -Support case-insensitive timezone name lookup (see LOWERCASE_TIMEZONE_LOOKUP).
//  2014/03/03  Martin D. Flynn
//     -Added "isFuture(..)"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
*** Performs every manner of function imaginable based on date/time values
**/

public class DateTime
    implements Comparable, Cloneable
{

    // ------------------------------------------------------------------------

    public static final String   GMT_TIMEZONE               = "GMT";
    public static final TimeZone GMT                        = DateTime.getGMTTimeZone();
    
    public static final boolean  LOWERCASE_TIMEZONE_LOOKUP  = true;

    // ------------------------------------------------------------------------

    public static final String   DEFAULT_DATE_FORMAT        = "yyyy/MM/dd";
    public static final String   DEFAULT_TIME_FORMAT        = "HH:mm:ss";
    public static final String   DEFAULT_TIMEZONE_FORMAT    = "zzz"; // short format
    
    public static final String   ISO8601_DATETIME_FORMAT    = "yyyy-MM-dd'T'HH:mm:ssZ"; // ISO-8601
    
    public static final String   DEFAULT_DATETIME_FORMAT    = DEFAULT_DATE_FORMAT + " " + DEFAULT_TIME_FORMAT;
    public static final String   DEFAULT_DATETIME_TZ_FORMAT = DEFAULT_DATETIME_FORMAT + " " + DEFAULT_TIMEZONE_FORMAT;

    /**
    *** Returns a compact date/time format<br>
    *** This format can be parsed by the "parseArgumentDate" method if the specified
    *** separator is one of ",", " ", or "|".
    *** @param sep The date|time|zone separator String.
    *** @return The date|time|zone format string
    **/
    public static String CompactDateTimeFormat(String sep)
    {
        // ie. "yyyy/MM/dd|HH:mm:ss|zzz"
        String s = (sep != null)? sep : "|";
        StringBuffer sb = new StringBuffer();
        sb.append("yyyy/MM/dd");
        sb.append(sep);
        sb.append("HH:mm:ss");
        sb.append(sep);
        sb.append("zzz");
        return sb.toString();
    }

    /**
    *** Returns a compact date/time format<br>
    *** This format can be parsed by the "parseArgumentDate" method if the specified
    *** separator is one of ",", " ", or "|".
    *** @param sep The date|time|zone separator character.
    *** @return The date|time|zone format string
    **/
    public static String CompactDateTimeFormat(char sep)
    {
        return CompactDateTimeFormat(String.valueOf(sep));
    }

    // ------------------------------------------------------------------------

    public static final DateTime INVALID_DATETIME           = new DateTime(0L);
    public static final DateTime MIN_DATETIME               = DateTime.getMinDate();
    public static final DateTime MAX_DATETIME               = DateTime.getMaxDate();

    public static final long     MIN_TIMESEC                = MIN_DATETIME.getTimeSec();
    public static final long     MAX_TIMESEC                = MAX_DATETIME.getTimeSec();

    // ------------------------------------------------------------------------
    // DateParseException
    
    /**
    *** DateParseException class
    **/
    public static class DateParseException
        extends Exception
    {
        public DateParseException(String msg) {
            super(msg);
        }
        public DateParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Analyzes the specified date format to determine the date separator characters
    *** @param dateFormat The date format to analyze
    *** @return The date separator characters
    **/
    public static char[] GetDateSeparatorChars(String dateFormat)
    {
        char sep[] = new char[] { '/', '/' };
        if (dateFormat != null) {
            for (int i = 0, c = 0; (i < dateFormat.length()) && (c < sep.length); i++) {
                char ch = dateFormat.charAt(i);
                if (!Character.isLetterOrDigit(ch)) {
                    sep[c++] = ch;
                }
            }
        }
        return sep;
    }

    // ------------------------------------------------------------------------

    /**
    *** Enum to indicate a date string format and to convert from one format to another
    **/
    public enum DateStringFormat implements EnumTools.StringLocale, EnumTools.IntValue {
        YMD    ( 0, "yyyy/mm/dd" ), // default
        MDY    ( 1, "mm/dd/yyyy" ),
        DMY    ( 1, "dd/mm/yyyy" );
        // ---
        private int         vv = 0;
        private String      dd = null;
        DateStringFormat(int v, String d)     { vv = v; dd = d; }
        public int     getIntValue()          { return vv; }
        public String  toString()             { return dd; }
        public String  toString(Locale loc)   { return dd; }
        public String  convertToYMD(String vvv) {
            // - already YMD?
            if (this.equals(YMD)) {
                //Print.logInfo("Already YMD: " + vvv);
                return vvv;
            }
            // - null value?
            if (vvv == null) {
                //Print.logInfo("Value is null");
                return vvv;
            }
            // - save time value
            int    c = vvv.indexOf(",");
            String d = (c >= 0)? vvv.substring(0,c) : vvv;
            String t = (c >= 0)? vvv.substring(c)   :  ""; // including comma
            // - parse date
            String f[] = StringTools.split(d,'/');
            if (f.length != 3) {
                //Print.logInfo("Invalid number of fields: " + d);
                return vvv;
            }
            // - convert
            switch (this) {
                case MDY: return f[2] + "/" + f[0] + "/" + f[1] + t; // MDY ==> YMD
                case DMY: return f[2] + "/" + f[1] + "/" + f[0] + t; // DMY ==> YMD
                default : return vvv; // control will not reach here
            }
        }
        public String  convertFromYMD(String ymd) {
            // yyyy/mm/dd,HH:MM:SS ==> mm/dd/yyyy,HH:MM:SS
            // yyyy-mm-dd,HH:MM:SS ==> mm/dd/yyyy,HH:MM:SS
            // yyyy-mm-ddTHH:MM:SS ==> mm/dd/yyyyTHH:MM:SS
            // - already YMD?
            if (this.equals(YMD)) {
                //Print.logInfo("Already YMD: " + vvv);
                return ymd;
            }
            // - null value?
            if (ymd == null) {
                //Print.logInfo("Value is null");
                return ymd;
            }
            // - save time value
            int c = ymd.indexOf(",");
            if (c < 0) { c = ymd.indexOf("T"); }
            String d = (c >= 0)? ymd.substring(0,c) : ymd;
            String t = (c >= 0)? ymd.substring(c)   :  ""; // including comma
            // - parse date
            String f[] = (d.indexOf('/') >= 0)? StringTools.split(d,'/') : StringTools.split(d,'-');
            if (f.length != 3) {
                //Print.logInfo("Invalid number of fields: " + d);
                return ymd;
            }
            // - convert
            switch (this) {
                case MDY: return f[1] + "/" + f[2] + "/" + f[0] + t; // YMD ==> MDY
                case DMY: return f[2] + "/" + f[1] + "/" + f[0] + t; // YMD ==> DMY
                default : return ymd; // control will not reach here
            }
        }
    };

    /**
    *** Returns the defined DateStringFormat for the specified account.
    *** @param dsfStr  The String value of the format (ie. "YMD", "MDY", "DMY")
    *** @return The DateStringFormat
    **/
    public static DateStringFormat getDateStringFormat(String dsfStr)
    {
        DateStringFormat dsf = null;
        if (dsfStr == null) {
            dsf = EnumTools.getDefault(DateStringFormat.class);
        } else {
            String fmt = dsfStr.toUpperCase();
            dsf = EnumTools.getValueOf(DateStringFormat.class, fmt);
            Print.logInfo("Looking for: " + fmt + " [Found " + dsf);
        }
        return dsf;
    }

    // ------------------------------------------------------------------------

    public static final long    HOURS_PER_DAY           = 24L;
    public static final long    SECONDS_PER_MINUTE      = 60L;
    public static final long    MINUTES_PER_HOUR        = 60L;
    public static final long    DAYS_PER_WEEK           = 7L;
    public static final long    SECONDS_PER_HOUR        = SECONDS_PER_MINUTE * MINUTES_PER_HOUR; //   3600
    public static final long    MINUTES_PER_DAY         = HOURS_PER_DAY * MINUTES_PER_HOUR;      //   1440
    public static final long    SECONDS_PER_DAY         = MINUTES_PER_DAY * SECONDS_PER_MINUTE;  //  86400
    public static final long    MINUTES_PER_WEEK        = DAYS_PER_WEEK * MINUTES_PER_DAY;       //  10080
    public static final long    SECONDS_PER_WEEK        = MINUTES_PER_WEEK * SECONDS_PER_MINUTE; // 604800

    public static final double  MINUTES_PER_SECOND      = 1.0 / (double)SECONDS_PER_MINUTE;      // 1/60
    public static final double  HOURS_PER_MINUTE        = 1.0 / (double)MINUTES_PER_HOUR;        // 1/60
    public static final double  HOURS_PER_SECOND        = 1.0 / (double)SECONDS_PER_HOUR;        // 1/3600

    /** 
    *** Returns the number of seconds in the specified number of days
    *** @param days  The number of days to convert to seconds
    *** @return The number of seconds
    **/
    public static long DaySeconds(long days)
    {
        return days * SECONDS_PER_DAY;
    }

    /** 
    *** Returns the number of seconds in the specified number of days
    *** @param days  The number of days to convert to seconds
    *** @return The number of seconds
    **/
    public static long DaySeconds(double days)
    {
        return Math.round(days * (double)SECONDS_PER_DAY);
    }
    
    /** 
    *** Returns the number of seconds in the specified number of hours
    *** @param hours  The number of hours to convert to seconds
    *** @return The number of seconds
    **/
    public static long HourSeconds(long hours)
    {
        return hours * SECONDS_PER_HOUR;
    }
    
    /** 
    *** Returns the number of seconds in the specified number of minutes
    *** @param minutes  The number of minutes to convert to seconds
    *** @return The number of seconds
    **/
    public static long MinuteSeconds(long minutes)
    {
        return minutes * SECONDS_PER_MINUTE;
    }

    // ------------------------------------------------------------------------

    public static final int  MONTHS_PER_YEAR    = 12;

    public static final int  JAN                = 0;
    public static final int  FEB                = 1;
    public static final int  MAR                = 2;
    public static final int  APR                = 3;
    public static final int  MAY                = 4;
    public static final int  JUN                = 5;
    public static final int  JUL                = 6;
    public static final int  AUG                = 7;
    public static final int  SEP                = 8;
    public static final int  OCT                = 9;
    public static final int  NOV                = 10;
    public static final int  DEC                = 11;
    
    public static final int  JANUARY            = JAN;
    public static final int  FEBRUARY           = FEB;
    public static final int  MARCH              = MAR;
    public static final int  APRIL              = APR;
  //public static final int  MAY                = MAY;
    public static final int  JUNE               = JUN;
    public static final int  JULY               = JUL;
    public static final int  AUGUST             = AUG;
    public static final int  SEPTEMBER          = SEP;
    public static final int  OCTOBER            = OCT;
    public static final int  NOVEMBER           = NOV;
    public static final int  DECEMBER           = DEC;
    
    // I18N?
    private static final String MONTH_NAME[][] = {
        { "January"  , "Jan" },
        { "February" , "Feb" },
        { "March"    , "Mar" },
        { "April"    , "Apr" },
        { "May"      , "May" },
        { "June"     , "Jun" },
        { "July"     , "Jul" },
        { "August"   , "Aug" },
        { "September", "Sep" },
        { "October"  , "Oct" },
        { "November" , "Nov" },
        { "December" , "Dec" },
    };

    /**
    *** Gets the 0-based index for the specified month abbreviation
    *** @param month  The month abbreviation
    *** @return The 0-based index [0..11] of the specified month appreviation
    **/
    public static int getMonthIndex0(String month, int dft)
    {
        String m = (month != null)? month.toLowerCase().trim() : null;
        if ((m != null) && !m.equals("")) {
            if (Character.isDigit(m.charAt(0))) {
                int v = StringTools.parseInt(m,-1);
                if ((v >= 0) && (v < MONTHS_PER_YEAR)) {
                    return v;
                }
            } else {
                for (int i = 0; i < MONTH_NAME.length; i++) {
                    if (m.startsWith(MONTH_NAME[i][1].toLowerCase())) {
                        return i;
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Gets the 1-based index for the specified month abbreviation
    *** @param month  The month abbreviation
    *** @return The 1-based index [1..12] of the specified month appreviation
    **/
    public static int getMonthIndex1(String month, int dft)
    {
        return DateTime.getMonthIndex0(month, dft - 1) + 1;
    }

    /**
    *** Gets the month name/abbreviation for the specified 1-based month index
    *** @param mon1  A 1-based month index [1..12]
    *** @param abbrev  True to return the month abbreviation, false to return the name
    *** @return The month name/abbreviation
    **/
    public static String getMonthName(int mon1, boolean abbrev)
    {
        int mon0 = mon1 - 1;
        if ((mon0 >= JANUARY) && (mon0 <= DECEMBER)) {
            return abbrev? MONTH_NAME[mon0][1] : MONTH_NAME[mon0][0];
        } else {
            return "";
        }
    }

    /**
    *** Returns all month names/appreviations
    *** @param abbrev  True to return month abbreviations, false to return month names
    *** @return  An array of month names/abbreviations
    **/
    public static String[] getMonthNames(boolean abbrev)
    {
        String mo[] = new String[MONTH_NAME.length];
        for (int i = 0; i < MONTH_NAME.length; i++) {
            mo[i] = DateTime.getMonthName(i, abbrev);
        }
        return mo;
    }
    
    /**
    *** Returns a Map object containing a map of month names/abbreviations and it's
    *** 0-based month index [0..11]
    *** @param abbrev  True to create the Map object with abbreviations, false for names
    *** @return The Map object
    **/
    public static Map<String,Integer> getMonthNameMap(boolean abbrev)
    {
        Map<String,Integer> map = new OrderedMap<String,Integer>();
        for (int i = 0; i < MONTH_NAME.length; i++) {
            map.put(DateTime.getMonthName(i, abbrev), new Integer(i));
        }
        return map;
    }

    // ------------------------------------------------------------------------

    private static final int MONTH_DAYS[] = {
        31, // Jan
        29, // Feb
        31, // Mar
        30, // Apr
        31, // May
        30, // Jun
        31, // Jul
        31, // Aug
        30, // Sep
        31, // Oct
        30, // Nov        
        31, // Dec
    };
    
    /**
    *** Gets the number of days in the specified month
    *** @param tz   The TimeZone
    *** @param mon1 The 1-based month index [1..12]
    *** @param year The year [valid for all AD years]
    *** @return The number of days in the specified month
    **/
    public static int getDaysInMonth(TimeZone tz, int mon1, int year)
    {
        int yy = (year > mon1)? year : mon1; // larger of the two
        int mm = (year > mon1)? mon1 : year; // smaller of the two
        return DateTime.getMaxMonthDayCount(mm, DateTime.isLeapYear(yy));
    }
    
    /**
    *** Gets the maximum number of days in the specified month
    *** @param mon1  The 1-based month index [1..12]
    *** @param isLeapYear  True for leap-year, false otherwise
    *** @return The maximum number of days in the specified month
    **/
    public static int getMaxMonthDayCount(int mon1, boolean isLeapYear)
    {
        int m0 = mon1 - 1;
        int d = ((m0 >= 0) && (m0 < DateTime.MONTH_DAYS.length))? DateTime.MONTH_DAYS[m0] : 31;
        return ((m0 != FEBRUARY) || isLeapYear)? d : 28;
    }
    
    /** 
    *** Returns true if the specified year represents a leap-year
    *** @param year  The year [valid for all AD years]
    *** @return True if the year is a leap-year, false otherwise
    **/
    public static boolean isLeapYear(int year)
    {
        return (new GregorianCalendar()).isLeapYear(year);
    }
    
    // ------------------------------------------------------------------------
    
    public static final int SUN       = 0;
    public static final int MON       = 1;
    public static final int TUE       = 2;
    public static final int WED       = 3;
    public static final int THU       = 4;
    public static final int FRI       = 5;
    public static final int SAT       = 6;
    
    public static final int SUNDAY    = SUN;
    public static final int MONDAY    = MON;
    public static final int TUESDAY   = TUE;
    public static final int WEDNESDAY = WED;
    public static final int THURSDAY  = THU;
    public static final int FRIDAY    = FRI;
    public static final int SATURDAY  = SAT;
    
    // I18N?
    private static final String DAY_NAME[][] = {
        // 0            1      2
        { "Sunday"   , "Sun", "Su" },
        { "Monday"   , "Mon", "Mo" },
        { "Tuesday"  , "Tue", "Tu" },
        { "Wednesday", "Wed", "We" },
        { "Thursday" , "Thu", "Th" },
        { "Friday"   , "Fri", "Fr" },
        { "Saturday" , "Sat", "Sa" },
    };
    
    /**
    *** Gets the day-of-week number for the specified day short abbreviation
    *** @param day  The day short abbreviation
    *** @return The 0-based day index [0..6]
    **/
    public static int getDayIndex(String day, int dft)
    {
        String d = (day != null)? day.toLowerCase().trim() : null;
        if (!StringTools.isBlank(d)) {
            if (Character.isDigit(d.charAt(0))) {
                int v = StringTools.parseInt(d,-1);
                if ((v >= 0) && (v < 7)) {
                    return v;
                }
            } else {
                for (int i = 0; i < DAY_NAME.length; i++) {
                    if (d.startsWith(DAY_NAME[i][2].toLowerCase())) {
                        return i;
                    }
                }
            }
        }
        return dft;
    }

    /**
    *** Gets the day-of-week name for the specified day number/index
    *** @param day  A 0-based day number/index [0..6]
    *** @param abbrev  0 for full name, 1 for abbreviation, 2 for short abbreviation
    *** @return  The day-of-week name/abbreviation
    **/
    public static String getDayName(int day, int abbrev)
    {
        if ((day >= SUNDAY) && (day <= SATURDAY) && (abbrev >= 0) && (abbrev <= 2)) {
            return DAY_NAME[day][abbrev];
        } else {
            return "";
        }
    }
    
    /**
    *** Gets the day-of-week name for the specified day number/index
    *** @param day  A 0-based day number/index [0..6]
    *** @param abbrev  True for abbreviation, false for full name
    *** @return  The day-of-week name/abbreviation
    **/
    public static String getDayName(int day, boolean abbrev)
    {
        return DateTime.getDayName(day, abbrev? 1 : 0);
    }

    /**
    *** Returns an array of day-of-week names
    *** @param abbrev  0 for full name, 1 for abbreviation, 2 for short abbreviation
    *** @return An array of day-of-week names/abbreviations
    **/
    public static String[] getDayNames(int abbrev)
    {
        String dy[] = new String[DAY_NAME.length];
        for (int i = 0; i < DAY_NAME.length; i++) {
            dy[i] = DateTime.getDayName(i, abbrev);
        }
        return dy;
    }

    /**
    *** Returns an array of day-of-week names
    *** @param abbrev  True abbreviations, false for full names
    *** @return An array of day-of-week names/abbreviations
    **/
    public static String[] getDayNames(boolean abbrev)
    {
        return DateTime.getDayNames(abbrev? 1 : 0);
    }

    /**
    *** Returns a Map object of day-of-week names to their 0-based number/index
    *** (used as a VComboBox item list)
    *** @param abbrev 0 for full name, 1 for abbreviation, 2 for short abbreviation
    *** @return The Map object
    **/
    public static Map<String,Integer> getDayNameMap(int abbrev)
    {
        Map<String,Integer> map = new OrderedMap<String,Integer>();
        for (int i = 0; i < DAY_NAME.length; i++) {
            map.put(DateTime.getDayName(i, abbrev), new Integer(i));
        }
        return map;
    }

    /**
    *** Returns a Map object of day-of-week names to their 0-based number/index
    *** (used as a VComboBox item list)
    *** @param abbrev True for abbreviations, false for full names
    *** @return The Map object
    **/
    public static Map<String,Integer> getDayNameMap(boolean abbrev)
    {
        return DateTime.getDayNameMap(abbrev? 1 : 0);
    }
    
    /**
    *** Returns the day of the week for the specified year/month/day
    *** @param year  The year [valid for dates after 1582/10/15]
    *** @param mon1  The month [1..12]
    *** @param day   The day [1..31]
    *** @return The day of the week (0=Sunday, 6=Saturday)
    **/
    public static int getDayOfWeek(int year, int mon1, int day)
    {
        int mon0 = mon1 - 1;
        GregorianCalendar cal = new GregorianCalendar(year, mon0, day);
        return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }
    
    /**
    *** Returns the day of the year for the specified year/month/day
    *** @param year  The year [valid for dates after 1582/10/15]
    *** @param mon1  The month [1..12]
    *** @param day   The day [1..31]
    *** @return The day of the year
    **/
    public static int getDayOfYear(int year, int mon1, int day)
    {
        int mon0 = mon1 - 1;
        GregorianCalendar cal = new GregorianCalendar(year, mon0, day);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String array of hours in a day
    *** (used as a VComboBox item list)
    *** @param hr24  True for 24 hour clock, false for 12 hour clock
    *** @return A String array of hours in a day
    **/
    public static String[] getHours(boolean hr24)
    {
        String hrs[] = new String[hr24? 24 : 12];
        for (int i = 0; i < hrs.length; i++) {
            hrs[i] = String.valueOf(i);
        }
        return hrs;
    }

    /**
    *** Gets a String array of minutes in a day
    *** (used as a VComboBox item list)
    *** @return A String array of minutes in a day
    **/
    public static String[] getMinutes()
    {
        String min[] = new String[60];
        for (int i = 0; i < min.length; i++) {
            min[i] = String.valueOf(i);
        }
        return min;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of the specified epoch time seconds
    *** @param timeSec  The Epoch time seconds
    *** @return The String representation
    **/
    public static String toString(long timeSec)
    {
        return (new java.util.Date(timeSec * 1000L)).toString();
    }

    /**
    *** Returns the current Epoch time in seconds since January 1, 1970 00:00:00 GMT
    *** @return The current Epoch time in seconds
    **/
    public static long getCurrentTimeSec()
    {
        // Number of seconds since the 'epoch' January 1, 1970, 00:00:00 GMT
        return getCurrentTimeMillis() / 1000L;
    }

    /**
    *** Returns the current Epoch time in milliseconds since January 1, 1970 00:00:00 GMT
    *** @return The current Epoch time in milliseconds
    **/
    public static long getCurrentTimeMillis()
    {
        // Number of milliseconds since the 'epoch' January 1, 1970, 00:00:00 GMT
        return System.currentTimeMillis();
    }
    
    /**
    *** Returns true if the specified time is within the specified lapsed time
    *** @param timeSec  An Epoch time in seconds
    *** @param lapseSec The time duration to test
    *** @return True if the specified time is within the specified lapsed time
    **/
    public static boolean isRecentSec(long timeSec, long lapseSec)
    {
        Print.logDebug("timeSec: " + timeSec);
        Print.logDebug("getCurrentTimeSec: " + DateTime.getCurrentTimeSec());
        return (timeSec >= (DateTime.getCurrentTimeSec() - lapseSec));
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if the specified date is valid
    *** @param dt   The DateTime to check
    *** @return True if the specified date is valid
    **/
    public static boolean isValid(DateTime dt)
    {
        long tm = (dt != null)? dt.getTimeSec() : -1L;
        if (tm < MIN_TIMESEC) {
            return false;
        } else
        if (tm > MAX_TIMESEC) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the minimum acceptable DateTime
    *** @return The minimum acceptable DateTime
    **/
    public static DateTime getMinDate()
    {
        return DateTime.getMinDate(null);
    }

    /** 
    *** Gets the minimum acceptable DateTime
    *** @param tz  The TimeZone (not that it really matters)
    *** @return The minimum acceptable DateTime
    **/
    public static DateTime getMinDate(TimeZone tz)
    {
        return new DateTime(1L, tz); // Jan 01 00:00:01 GMT 1970
    }

    /** 
    *** Gets the maximum acceptable DateTime
    *** @return The maximum acceptable DateTime
    **/
    public static DateTime getMaxDate()
    {
        return DateTime.getMaxDate(null);
    }

    /**
    *** Gets the maximum acceptable DateTime
    *** @param tz  The TimeZone (not that it really matters)
    *** @return The maximum acceptable DateTime
    **/
    public static DateTime getMaxDate(TimeZone tz)
    {
        int YYYY = 2050; // arbitrary year
        int MM1  = DECEMBER + 1; // 12
        int DD   = 31; // DateTime.getDaysInMonth(tz,MM1,YYYY);
        // Do NOT call "DateTime.getDaysInMonth(...)" here! Startup init will fail.
        return new DateTime(tz, YYYY, MM1, DD);
    }

    // ------------------------------------------------------------------------

    /**
    *** Enum indicating how to handle parsed dates when no time is specified<br>
    *** CurrentTime  - Use current time<br>
    *** ContextStart - Use start of time context (ie. day, month, year)<br>
    *** ContextEnd   - Use end of time context (ie. day, month, year)<br>
    *** DayStart     - Use start of day<br>
    *** DayEnd       - Use end day<br>
    **/
    public enum DefaultParsedTime {
        CurrentTime,    // use current time
        ContextStart,   // use start of time context (ie. day, month, year)
        ContextEnd,     // use end of time context (ie. day, month, year)
        DayStart,       // use start of day
        DayEnd,         // use end of day
    };

    /**
    *** Class/Structure which holds date/time information
    **/
    public static class ParsedDateTime
    {
        public TimeZone timeZone    = null;
        public long     epoch       = -1L;
        public int      year        = -1;
        public int      month1      = -1;
        public int      day         = -1;
        public int      hour24      = -1;
        public int      minute      = -1;
        public int      second      = -1;
        public ParsedDateTime(TimeZone tz, long epoch) { 
            this.timeZone = tz;
            this.epoch = epoch; 
        }
        public ParsedDateTime(TimeZone tz, int year, int month1, int day) { 
            this.timeZone = tz;
            this.year     = year; 
            this.month1   = month1; 
            this.day      = day; 
        }
        public ParsedDateTime(TimeZone tz, int year, int month1, int day, int hour, int minute, int second) { 
            this(tz, year, month1, day);
            this.hour24   = hour; 
            this.minute   = minute; 
            this.second   = second; 
        }
        // --
        public int getYear() {
            return this.year;
        }
        public void setYear(int YYYY) {
            this.year = YYYY;
        }
        // --
        public int getMonth1() {
            return this.month1;
        }
        public void setMonth1(int MM) {
            this.month1 = MM;
        }
        // --
        public int getDayOfMonth() {
            return this.day;
        }
        public void setDayOfMonth(int DD) {
            this.day = DD;
        }
        // --
        public int getHour24() {
            return this.hour24;
        }
        public void setHour24(int hh) {
            this.hour24 = hh;
        }
        // --
        public int getMinute() {
            return this.minute;
        }
        public void setMinute(int mm) {
            this.minute = mm;
        }
        // --
        public int getSecond() {
            return this.second;
        }
        public void setSecond(int ss) {
            this.second = ss;
        }
        // --
        public void setDayNumber(long dayNumber) {
            long N      = dayNumber + 578042L;
            long C      = ((N * 1000L) - 200L) / 36524250L;
            long N1     = N + C - (C / 4L);
            long Y1     = ((N1 * 1000L) - 200L) / 365250L;
            long N2     = N1 - ((365250L * Y1) / 1000L);
            long M1     = ((N2 * 1000L) - 500L) / 30600L;
            int  day    = (int)(((N2 * 1000L) - (30600L * M1) + 500L) / 1000L);
            int  month1 = (int)((M1 <= 9L)? (M1 + 3L) : (M1 - 9L));
            int  year   = (int)((M1 <= 9L)? Y1 : (Y1 + 1));
            // --
            this.day    = day;
            this.month1 = month1;
            this.year   = year;
        }
        // --
        public void setMinuteOfDay(int mod) {
            // (hh * 60) + mm
            this.setHour24(mod / 60);
            this.setMinute(mod % 60);
            this.setSecond(-1);
        }
        // --
        public void setSecondOfDay(int sod) {
            // (hh * 3600) + (mm * 60) + ss
            this.setHour24(sod / 3600);
            this.setMinute((sod / 60) % 60);
            this.setSecond((sod     ) % 60);
        }
        // --
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.epoch >= 0L) {
                sb.append(this.epoch);
            } else {
                sb.append(StringTools.format(this.year  ,"%04d")).append("/");
                sb.append(StringTools.format(this.month1,"%02d")).append("/");
                sb.append(StringTools.format(this.day   ,"%02d"));
                if (this.hour24 >= 0) {
                    sb.append(",");
                    sb.append(StringTools.format(this.hour24,"%02d")).append(":");
                    sb.append(StringTools.format(this.minute,"%02d"));
                    if (this.second >= 0) {
                        sb.append(":");
                        sb.append(StringTools.format(this.second,"%02d"));
                    }
                }
            }
            return sb.toString();
        }
        // --
        public DateTime createDateTime(TimeZone tzone) {
            // only valid for years beyond 1970
            TimeZone tmz = (tzone != null)? tzone : this.timeZone;
            if (this.epoch >= 0L) {
                return new DateTime(this.epoch, tmz);
            } else {
                return new DateTime(tmz, this.year, this.month1, this.day, this.hour24, this.minute, this.second);
            }
        }
        public DateTime createDateTime() {
            // only valid for years beyond 1970
            return this.createDateTime(null);
        }
        public long getEpochTime() {
            if (this.epoch >= 0L) {
                return this.epoch;
            } else {
                return this.createDateTime().getTimeSec();
            }
        }
        public long getDayNumber() {
            return DateTime.getDayNumberFromDate(this);
        }
    }

    /**
    *** Parse specified String into a ParsedDateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.
    *** Allowable Date Formats:<br>
    ***   YYYY-MM-DDThh:mm:ssZ               (ie. "2012-03-08T06:49:58Z" ISO-8601 - GPX date format)<br>
    ***   YYYY-MM-DDThh:mm:ss+XX:XX          (ie. "2012-03-08T06:49:58+00:00")<br>
    ***   YYYY/MM[/DD[/hh[:mm[:ss]]]][,ZZZ]  (ie. "2010/07/04/12:57:32,PST")<br>
    ***   YYYY/MM/DD,hh[:mm[:ss]]]][,ZZZ]    (ie. "2010/07/04,12:57:32,PST")<br>
    ***   EEEEEEEE[,ZZZ]                     (ie. "1277704868,PST")<br>
    ***   -Dh[,ZZZ]                          (ie. "-5h,PST" => "5 hours ago")<br>
    ***   -Dd[,hh[:mm[:ss]]][,ZZZ]           (ie. "-3d,PST" => "3 days ago")<br>
    ***   +Dd[,hh[:mm[:ss]]][,ZZZ]           (ie. "+4d,PST" => "4 days from now")<br>
    ***   -Dm[,hh[:mm[:ss]]][,ZZZ]           (ie. "-2m,PST" => "2 months ago")<br>
    ***   +Dm[,hh[:mm[:ss]]][,ZZZ]           (ie. "+3m,PST" => "3 months from now")<br>
    ***   -Dy[,hh[:mm[:ss]]][,ZZZ]           (ie. "-1y,PST" => "1 year ago")<br>
    ***   +Dy[,hh[:mm[:ss]]][,ZZZ]           (ie. "+5y,PST" => "5 years from now")<br>
    *** Examples:
    ***   "2010/07/04/12:57:32,PST"
    ***   "2010/07/04,12:57,EST"
    ***   "2010/07/04|12:57|EST"
    ***   "1277704868,PST"
    ***   "-0d"         (today)
    ***   "-1d,PST"     (yesterday)
    ***   "-3d,WST"     (3 days ago)
    ***   "+4d,CST"     (4 days from now)
    ***   "-1m"         (last month)
    ***   "-0m"         (this month)
    ***   "+1m"         (next month)
    ***   "-Sun,MST"    (last Sunday)
    ***   "+WED,PST"    (next Wednesday)
    ***   "-1y"         (last year)
    ***   "-0y"         (this year)
    ***   "+1y"         (next year)
    *** @param dateStr  The date String to parse
    *** @param dftTZ    The default TimeZone
    *** @param dftTime  Enum indicating what 'time' to use if a time is not specified
    *** @return The parsed ParsedDateTime instance, or null if unable to parse the specified date/time string
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static ParsedDateTime parseDateTime(String dateStr, TimeZone dftTZ, DateTime.DefaultParsedTime dftTime)
        throws DateParseException
    {

        /* default DefaultParsedTime [isToDate] */
        if (dftTime == null) {
            dftTime = DefaultParsedTime.CurrentTime;
        }
        boolean useCurrTime  = DefaultParsedTime.CurrentTime.equals(dftTime);
        boolean useDayStart  = DefaultParsedTime.DayStart.equals(dftTime);
        boolean useDayEnd    = DefaultParsedTime.DayEnd.equals(dftTime);
        boolean useCtxStart  = DefaultParsedTime.ContextStart.equals(dftTime);
        boolean useCtxEnd    = DefaultParsedTime.ContextEnd.equals(dftTime);

        /* no date string specified? */
        dateStr = StringTools.trim(dateStr);
        if (StringTools.isBlank(dateStr)) {
            throw new DateParseException("'dateStr' argument is null/empty");
        }

        /* parse fields */
        String dateGrp[] = StringTools.parseStringArray(dateStr, ", |"); // ", T"

        /* check for alternate compact formats */
        if (dateGrp.length == 1) {
            int dlen = dateStr.length();
            if ((dlen >= 20) && (dateStr.charAt(10) == 'T')) {
                // GPX format:
                //  YYYY-MM-DDThh:mm:ssZ
                //  YYYY-MM-DDThh:mm:ss+00:00
                String ds = dateStr.substring( 0,10); // YYYY-MM-DD
                String ts = dateStr.substring(11,19); // hh:mm:ss
                String tz = dateStr.substring(19);    // Z, +00:00
                dateGrp = new String[] { ds, ts, tz };
            }
            //else 
            //if ((dlen == 14) && (dateStr.charAt(0) == '2') && StringTools.isNumeric(dateStr)) {
            //    // "YYYYMMDDhhmmss" format
            //    // TODO:
            //}
        }
        

        /* validate fields */
        int grpLen = dateGrp.length;
        if ((dateGrp.length < 1) || (dateGrp.length > 3)) {
            throw new DateParseException("Invalid number of ',' separated groups: " + dateStr);
        } else
        if (dateGrp[0].length() == 0) {
            throw new DateParseException("Invalid Date/Time specification[#1]: " + dateStr);
        }

        /* extract timezone (if any) */
        TimeZone timeZone = dftTZ;
        if (grpLen == 3) {
            String tmzs = dateGrp[2];
            if (StringTools.isBlank(tmzs)) {
                // default timezone
                grpLen--;
            } else
            if (!Character.isDigit(tmzs.charAt(0))) {
                // Z, PST, +00:00
                timeZone = DateTime.getTimeZone(tmzs, null);
                if (timeZone == null) {
                    throw new DateParseException("Invalid TimeZone[#2]: ["+tmzs+"] " + dateStr);
                }
                grpLen--;
            } else {
                throw new DateParseException("Invalid TimeZone[#3]: ["+tmzs+"] " + dateStr);
            }
        } else
        if (grpLen == 2) {
            String tmzs = dateGrp[1];
            if (StringTools.isBlank(tmzs)) {
                // default timezone
                grpLen--;
            } else
            if (!Character.isDigit(tmzs.charAt(0))) {
                timeZone = DateTime.getTimeZone(tmzs, null);
                if (timeZone == null) {
                    throw new DateParseException("Invalid TimeZone[#4]: ["+tmzs+"] " + dateStr);
                }
                grpLen--;
            } else {
                // leave as-is
            }
        }
        DateTime now = new DateTime(timeZone);

        /* parse date/time fields */
        char s = dateGrp[0].charAt(0);
        String dateFld[] = null;
        if (grpLen == 1) {
            // -Dd
            // -DAY
            // EEEEEEEEEE[mmm]
            // YYYY/MM[/DD[/hh[:mm[:ss]]]]
            if ((s == '+') || (s == '-')) {
                // {+|-}####[d]
                // {+|-}DAY (sun|mon|tue|wed|thu|fri|sat)
                String D = dateGrp[0];
                int DLen = D.length();
                long ofsSec;
                if (((DLen == 4) || (DLen == 3)) && !Character.isDigit(D.charAt(1))) {
                    // {+|-}DAY (sun|mon|tue|wed|thu|fri|sat)
                    int prsDOW = DateTime.getDayIndex(D.substring(1),-1);
                    if (prsDOW < 0) {
                        throw new DateParseException("Invalid DOW specified: " + D);
                    }
                    int nowDOW = now.getDayOfWeek();
                    if (s == '-') {
                        int deltaDays = (((nowDOW + 7) - prsDOW) % 7);
                        ofsSec = -DateTime.DaySeconds((deltaDays <= 0)? 7 : deltaDays);
                    } else {
                        int deltaDays = (((prsDOW + 7) - nowDOW) % 7);
                        ofsSec = DateTime.DaySeconds((deltaDays <= 0)? 7 : deltaDays);
                    }
                    DateTime dt = new DateTime(now.getTimeSec() + ofsSec, timeZone);
                    int YYYY = dt.getYear();
                    int MM0  = dt.getMonth0();
                    int DD   = dt.getDayOfMonth();
                    int hh   = now.getHour24();
                    int mm   = now.getMinute();
                    int ss   = now.getSecond();
                    if (useCtxStart || useDayStart) {
                        hh = 0;
                        mm = 1;
                        ss = 1;
                    } else
                    if (useCtxEnd || useDayEnd) {
                        hh = 23;
                        mm = 59;
                        ss = 59;
                    }
                    DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                    return new ParsedDateTime(timeZone, pdt.getTimeSec());
                } else {
                    // {+|-}####[d]
                    char lastCh = Character.toLowerCase(D.charAt(DLen - 1));
                    switch (lastCh) {
                        case 's':  // seconds (should not be specified, but allowed)
                        case 'h':  // hours (should not be specified, but allowed)
                        case 'd':  // days
                        case 'm':  // months (possibly ambiguous with 'minutes')
                        case 'y':  // years
                            D = D.substring(0, DLen - 1);
                            break;
                        default :  // seconds?
                            // leave as-is
                            break;
                    }
                    if (!StringTools.isInt(D,true)) {
                        throw new DateParseException("Non-numeric Delta time: " + dateStr);
                    }
                    long delta = StringTools.parseLong(D, 0L); // may be negative
                    switch (lastCh) {
                        case 's': { // seconds
                            DateTime dt = new DateTime(now.getTimeSec() + delta, timeZone);
                            int YYYY = dt.getYear();
                            int MM0  = dt.getMonth0();
                            int DD   = dt.getDayOfMonth();
                            int hh   = dt.getHour24();
                            int mm   = dt.getMinute();
                            int ss   = dt.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /*break; */
                        case 'h': { // bours
                            DateTime dt = new DateTime(now.getTimeSec() + DateTime.HourSeconds(delta), timeZone);
                            int YYYY = dt.getYear();
                            int MM0  = dt.getMonth0();
                            int DD   = dt.getDayOfMonth();
                            int hh   = dt.getHour24();
                            int mm   = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : dt.getMinute();
                            int ss   = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : dt.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /*break; */
                        case 'd': { // days
                            DateTime dt = new DateTime(now.getTimeSec() + DateTime.DaySeconds(delta), timeZone);
                            int YYYY = dt.getYear();
                            int MM0  = dt.getMonth0();
                            int DD   = dt.getDayOfMonth();
                            int hh   = (useDayStart||useCtxStart)? 0 : (useDayEnd||useCtxEnd)? 23 : dt.getHour24();
                            int mm   = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : dt.getMinute();
                            int ss   = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : dt.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /* break; */
                        case 'm': { // months (ambiguous with 'minutes')
                            int YYYY = now.getYear(); // YYYY
                            int MM0  = now.getMonth0() + (int)delta;
                            if (MM0 < 0) {
                                // previous year(s)
                                YYYY -= (Math.abs(MM0) / MONTHS_PER_YEAR) + 1;
                                MM0   = (MONTHS_PER_YEAR - (Math.abs(MM0) % MONTHS_PER_YEAR)) % MONTHS_PER_YEAR;
                            } else
                            if (MM0 >= MONTHS_PER_YEAR) {
                                YYYY += (MM0 / MONTHS_PER_YEAR);
                                MM0   = MM0 % MONTHS_PER_YEAR;
                            }
                            int DD;
                            if (useCtxStart) {
                                // first day of month
                                DD = 1;
                            } else
                            if (useCtxEnd) {
                                // last day of month
                                DD = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                            } else {
                                // matching current day of month
                                DD = now.getDayOfMonth();
                                int maxDD = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                                if (DD > maxDD) {
                                    DD = maxDD;
                                }
                            }
                            int hh = (useDayStart||useCtxStart)? 0 : (useDayEnd||useCtxEnd)? 23 : now.getHour24();
                            int mm = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : now.getMinute();
                            int ss = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : now.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /* break; */
                        case 'y': { // years
                            int YYYY = now.getYear() + (int)delta;
                            int MM0;
                            int DD;
                            if (useCtxStart) {
                                // first month, first day of month
                                MM0 = JANUARY;
                                DD  = 1;
                            } else
                            if (useCtxEnd) {
                                // last month, last day of month
                                MM0 = DECEMBER;
                                DD  = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                            } else {
                                // same month/day
                                MM0 = now.getMonth0();
                                DD  = now.getDayOfMonth();
                                int maxDD = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                                if (DD > maxDD) {
                                    DD = maxDD;
                                }
                            }
                            int hh = (useDayStart||useCtxStart)? 0 : (useDayEnd||useCtxEnd)? 23 : now.getHour24();
                            int mm = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : now.getMinute();
                            int ss = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : now.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /* break; */
                        default: { // seconds
                            DateTime dt = new DateTime(now.getTimeSec() + delta, timeZone);
                            int YYYY = dt.getYear();
                            int MM0  = dt.getMonth0();
                            int DD   = dt.getDayOfMonth();
                            int hh   = dt.getHour24();
                            int mm   = dt.getMinute();
                            int ss   = dt.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /*break; */
                    } // switch
                } // else
                // control does not reach here
                // ---------------------------
            } else {
                String d[] = StringTools.parseStringArray(dateGrp[0], "/:-");
                if ((d.length < 1) || (d.length > 6)) {
                    throw new DateParseException("Invalid number of Date/Time fields: " + dateStr);
                } else
                if (d.length == 1) {
                    int d0len = d[0].length();
                    if (d0len <= 13) {
                        // EEEEEEEEEE[mmm]
                        if (!StringTools.isLong(d[0],true)) {
                            throw new DateParseException("Non-numeric Epoch time: " + dateStr);
                        }
                        long epoch = StringTools.parseLong(d[0], DateTime.getCurrentTimeSec());
                        return new ParsedDateTime(timeZone,epoch);
                    } else 
                    if ((d0len == 14) && (d[0].charAt(0) == '2') && StringTools.isNumeric(d[0])) {
                        // YYYYMMDDhhmmss
                        int  YYYY  = StringTools.parseInt(d[0].substring( 0, 4),0);
                        int  MM    = StringTools.parseInt(d[0].substring( 4, 6),0);
                        int  DD    = StringTools.parseInt(d[0].substring( 6, 8),0);
                        int  hh    = StringTools.parseInt(d[0].substring( 8,10),0);
                        int  mm    = StringTools.parseInt(d[0].substring(10,12),0);
                        int  ss    = StringTools.parseInt(d[0].substring(12,14),0);
                        long epoch = (new DateTime(timeZone,YYYY,MM,DD,hh,mm,ss)).getTimeSec();
                        return new ParsedDateTime(timeZone,epoch);
                    } else {
                        throw new DateParseException("Invalid Epoch time specification: " + dateStr);
                    }
                    // control does not reach here
                    // ---------------------------
                } else {
                    dateFld = d; // (length >= 2) && (length <= 6)
                    // dateFld: YYYY,MM[,DD[,hh[,mm[,ss]]]]
                }
            }
        } else
        if (grpLen == 2) {
            // -Dd,hh[:mm[:ss]]]]
            // -DAY,hh[:mm[:ss]]]]
            // YYYY/MM/DD,hh[:mm[:ss]]]]
            // ---
            // Time
            String t[] = StringTools.parseStringArray(dateGrp[1], "/:-");
            if ((t.length < 1) || (t.length > 3)) {
                throw new DateParseException("Invalid number of Time fields: " + dateStr);
            }
            for (String n : t) {
                if (!StringTools.isInt(n,true)) {
                    throw new DateParseException("Non-numeric Time specification: " + dateStr);
                }
            }
            int thh = StringTools.parseInt(t[0],0); // guaranteed
            int tmm = (t.length > 1)? StringTools.parseInt(t[1],1) : -1;
            int tss = (t.length > 2)? StringTools.parseInt(t[2],1) : -1;
            // Date
            String d[];
            if ((s == '+') || (s == '-')) {
                // {+|-}####[d],hh[:mm[:ss]]]]
                // {+|-}DAY,hh[:mm[:ss]]]] (sun|mon|tue|wed|thu|fri|sat)
                String D = dateGrp[0];
                int DLen = D.length();
                long ofsSec;
                if (((DLen == 4) || (DLen == 3)) && !Character.isDigit(D.charAt(1))) {
                    int prsDOW = DateTime.getDayIndex(D.substring(1),-1);
                    if (prsDOW < 0) {
                        throw new DateParseException("Invalid DOW specified: " + D);
                    }
                    int nowDOW = now.getDayOfWeek();
                    if (s == '-') {
                        int deltaDays = (((nowDOW + 7) - prsDOW) % 7);
                        ofsSec = -DateTime.DaySeconds((deltaDays <= 0)? 7 : deltaDays);
                    } else {
                        int deltaDays = (((prsDOW + 7) - nowDOW) % 7);
                        ofsSec = DateTime.DaySeconds((deltaDays <= 0)? 7 : deltaDays);
                    }
                    DateTime dt = new DateTime(now.getTimeSec() + ofsSec, timeZone);
                    int YYYY = dt.getYear();
                    int MM0  = dt.getMonth0();
                    int DD   = dt.getDayOfMonth();
                    int hh   = thh; // guaranteed
                    int mm   = (tmm >= 1)? tmm : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : dt.getMinute();
                    int ss   = (tss >= 1)? tss : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : dt.getSecond();
                    DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                    return new ParsedDateTime(timeZone, pdt.getTimeSec());
                } else {
                    char lastCh = Character.toLowerCase(D.charAt(DLen - 1));
                    switch (lastCh) {
                        case 'd':  // days
                        case 'm':  // months (ambiguous with 'minutes')
                        case 'y':  // years
                            D = D.substring(0, DLen - 1);
                            break;
                        default :  // seconds
                            throw new DateParseException("Missing 'd|m|y' on delta days|months|years: " + dateStr);
                    }
                    if (!StringTools.isInt(D,true)) {
                        throw new DateParseException("Non-numeric Delta time: " + dateStr);
                    }
                    long delta = StringTools.parseLong(D, 0L); // may be negative
                    switch (lastCh) {
                        case 'd': { // days
                            DateTime dt = new DateTime(now.getTimeSec() + DateTime.DaySeconds(delta), timeZone);
                            int YYYY = dt.getYear();
                            int MM0  = dt.getMonth0();
                            int DD   = dt.getDayOfMonth();
                            int hh   = thh; // guaranteed
                            int mm   = (tmm >= 1)? tmm : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : dt.getMinute();
                            int ss   = (tss >= 1)? tss : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : dt.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /* break; */
                        case 'm': { // months (ambiguous with 'minutes')
                            int YYYY = now.getYear(); // YYYY
                            int MM0  = now.getMonth0() + (int)delta;
                            if (MM0 < 0) {
                                // previous year(s)
                                YYYY -= (Math.abs(MM0) / MONTHS_PER_YEAR) + 1;
                                MM0   = (MONTHS_PER_YEAR - (Math.abs(MM0) % MONTHS_PER_YEAR)) % MONTHS_PER_YEAR;
                            } else
                            if (MM0 >= MONTHS_PER_YEAR) {
                                YYYY += (MM0 / MONTHS_PER_YEAR);
                                MM0   = MM0 % MONTHS_PER_YEAR;
                            }
                            int DD;
                            if (useCtxStart) {
                                // first day of month
                                DD = 1;
                            } else
                            if (useCtxEnd) {
                                // last day of month
                                DD = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                            } else {
                                // matching current day of month
                                DD = now.getDayOfMonth();
                                int maxDD = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                                if (DD > maxDD) {
                                    DD = maxDD;
                                }
                            }
                            int hh   = thh; // guaranteed
                            int mm   = (tmm >= 1)? tmm : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : now.getMinute();
                            int ss   = (tss >= 1)? tss : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : now.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /* break; */
                        case 'y': { // years
                            int YYYY = now.getYear() + (int)delta;
                            int MM0;
                            int DD;
                            if (useCtxStart) {
                                // first month, first day of month
                                MM0 = JANUARY;
                                DD  = 1;
                            } else
                            if (useCtxEnd) {
                                // last month, last day of month
                                MM0 = DECEMBER;
                                DD  = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                            } else {
                                // same month/day
                                MM0 = now.getMonth0();
                                DD  = now.getDayOfMonth();
                                int maxDD = DateTime.getDaysInMonth(timeZone, MM0+1, YYYY);
                                if (DD > maxDD) {
                                    DD = maxDD;
                                }
                            }
                            int hh   = thh; // guaranteed
                            int mm   = (tmm >= 1)? tmm : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : now.getMinute();
                            int ss   = (tss >= 1)? tss : (useCtxStart||useDayStart)? 1 : (useCtxEnd||useDayEnd)? 59 : now.getSecond();
                            DateTime pdt = new DateTime(timeZone, YYYY, MM0+1, DD, hh, mm, ss);
                            return new ParsedDateTime(timeZone, pdt.getTimeSec());
                        } /* break; */
                        default:  // seconds (will not occur here)
                            throw new DateParseException("Missing 'd|m|y' on delta days|months|years: " + dateStr);
                    } // switch
                } // else
                // control does not reach here
                // ---------------------------
            } else {
                // "YYYY/MM/DD"
                d = StringTools.parseStringArray(dateGrp[0], "/:-");
                if (d.length != 3) {
                    throw new DateParseException("Invalid number of Date fields: " + dateStr);
                }
                for (String n : d) {
                    if (!StringTools.isInt(n,true)) {
                        throw new DateParseException("Non-numeric Date specification: " + dateStr);
                    }
                }
            }
            // combine date/time
            dateFld = new String[d.length + t.length];  // (length >= 3) && (length <= 6)
            System.arraycopy(d, 0, dateFld, 0, d.length);
            System.arraycopy(t, 0, dateFld, d.length, t.length);
            // dateFld: YYYY,MM[,DD[,hh[,mm[,ss]]]]
        } else {
            throw new DateParseException("Invalid number of Date/Time/TMZ groups: " + dateStr);
        }

        /* evaluate date/time fields (dateFld.length >= 2) */
        // dateFld: YYYY,MM[,DD[,hh[,mm[,ss]]]]
        int YY    = StringTools.parseInt(dateFld[0], now.getYear());
        int MM    = StringTools.parseInt(dateFld[1], now.getMonth1());
        int maxDD = DateTime.getDaysInMonth(timeZone, MM, YY);
        int DD    = 0; // day set below
        int hh    = (useDayStart||useCtxStart)? 0 : (useDayEnd||useCtxEnd)? 23 : now.getHour24();
        int mm    = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : now.getMinute();
        int ss    = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? 59 : now.getSecond();
        if (dateFld.length >= 3) {
            // at least YYYY/MM/DD provided
            DD = StringTools.parseInt(dateFld[2], now.getDayOfMonth());
            if (DD > maxDD) { DD = maxDD; }
            if (dateFld.length >= 4) { hh = StringTools.parseInt(dateFld[3], hh); }
            if (dateFld.length >= 5) { mm = StringTools.parseInt(dateFld[4], mm); }
            if (dateFld.length >= 6) { ss = StringTools.parseInt(dateFld[5], ss); }
        } else {
            // only YYYY/MM provided
            DD = (useDayStart||useCtxStart)? 1 : (useDayEnd||useCtxEnd)? maxDD : now.getDayOfMonth();
            if (DD > maxDD) { DD = maxDD; } // <== unlikely
        }

        /* return new ParsedDateTime instance */
        return new ParsedDateTime(timeZone, YY, MM, DD, hh, mm, ss);

    }

    /**
    *** Parse specified String into a DateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.<br>
    *** (see "parseDateTime" for allowable Date Formats)
    *** @param dateStr  The date String to parse
    *** @param dftTZ    The default TimeZone
    *** @param dftTime  Enum indicating what 'time' to use if a time is not specified
    *** @return The parsed DateTime instance
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static DateTime parseArgumentDate(String dateStr, TimeZone dftTZ, DateTime.DefaultParsedTime dftTime)
        throws DateParseException
    {
        return DateTime.parseDateTime(dateStr, dftTZ, dftTime).createDateTime();
    }

    /**
    *** Parse specified String into a DateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.<br>
    *** (see "parseDateTime" for allowable Date Formats)
    *** @param dateStr  The date String to parse
    *** @param dftTZ    The default TimeZone
    *** @param isToDate True to default to an end-of-day time if the time is not specified
    *** @return The parsed DateTime instance
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static DateTime parseArgumentDate(String dateStr, TimeZone dftTZ, boolean isToDate)
        throws DateParseException
    {
        DefaultParsedTime dftTime = isToDate? DefaultParsedTime.DayEnd : DefaultParsedTime.DayStart;
        return DateTime.parseDateTime(dateStr, dftTZ, dftTime).createDateTime();
    }

    /**
    *** Parse specified String into a DateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.
    *** (see "parseDateTime" for allowable Date Formats)
    *** @param dateStr  The date String to parse
    *** @param dftTZ    The default TimeZone
    *** @return The parsed DateTime instance
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static DateTime parseArgumentDate(String dateStr, TimeZone dftTZ)
        throws DateParseException
    {
        DefaultParsedTime dftTime = DefaultParsedTime.DayStart;
        return DateTime.parseDateTime(dateStr, dftTZ, dftTime).createDateTime();
    }

    /**
    *** Parse specified String into a DateTime instance.
    *** This method parses a date which has been provided as an argument to a command-line tool
    *** or in a URL request string.
    *** (see "parseDateTime" for allowable Date Formats)
    *** @param dateStr  The date String to parse
    *** @return The parsed DateTime instance
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public static DateTime parseArgumentDate(String dateStr)
        throws DateParseException
    {
        DefaultParsedTime dftTime = DefaultParsedTime.DayStart;
        return DateTime.parseDateTime(dateStr, DateTime.getGMTTimeZone(), dftTime).createDateTime();
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    *** @param year   The year [1582..4000]
    *** @param month1 The month [1..12]
    *** @param day    The day [1..31]
    *** @return The number of days since October 15, 1582.
    **/
    public static long getDayNumberFromDate(int year, int month1, int day)
    {
        long yr = ((long)year * 1000L) + (long)(((month1 - 3) * 1000) / MONTHS_PER_YEAR);
        return ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
               + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
               + (long)day - 578042L; // October 15, 1582, beginning of Gregorian Calendar
    }

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    **/
    public static long getDayNumberFromDate(ParsedDateTime pdt)
    {
        if (pdt == null) {
            return 0L;
        } else
        if (pdt.epoch >= 0L) {
            DateTime dt = pdt.createDateTime();
            return DateTime.getDayNumberFromDate(dt.getYear(), dt.getMonth1(), dt.getDayOfMonth());
        } else {
            return DateTime.getDayNumberFromDate(pdt.year, pdt.month1, pdt.day);
        }
    }

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    **/
    public static long getDayNumberFromDate(DateTime dt, TimeZone tz)
    {
        if (dt == null) {
            return 0L;
        } else {
            return DateTime.getDayNumberFromDate(dt.getYear(tz), dt.getMonth1(tz), dt.getDayOfMonth(tz));
        }
    }

    /** 
    *** Returns the day number of days since October 15, 1582 (the first day of the Gregorian Calendar)
    **/
    public static long getDayNumberFromDate(DateTime dt)
    {
        if (dt == null) {
            return 0L;
        } else {
            return DateTime.getDayNumberFromDate(dt.getYear(), dt.getMonth1(), dt.getDayOfMonth());
        }
    }

    /**
    *** Returns the current (today) day number
    *** @param tz The current timezone
    *** @return The current day number
    **/
    public static long getCurrentDayNumber(TimeZone tz)
    {
        DateTime dt = new DateTime(tz);
        return DateTime.getDayNumberFromDate(dt, tz);
    }

    /** 
    *** Returns the year/month/day from the day number (days since October 15, 1582)
    *** @param dayNumber  The number of days since October 15, 1582
    *** @return ParsedDateTime structure containing the year/month/day.
    **/
    public static ParsedDateTime getDateFromDayNumber(long dayNumber)
    {
        return DateTime.getDateFromDayNumber(dayNumber, null/*TimeZone*/);
    }
    
    /** 
    *** Returns the year/month/day from the day number (days since October 15, 1582)
    *** @param dayNumber  The number of days since October 15, 1582
    *** @return ParsedDateTime structure containing the year/month/day.
    **/
    public static ParsedDateTime getDateFromDayNumber(long dayNumber, TimeZone tmz)
    {
        long N      = dayNumber + 578042L;
        long C      = ((N * 1000L) - 200L) / 36524250L;
        long N1     = N + C - (C / 4L);
        long Y1     = ((N1 * 1000L) - 200L) / 365250L;
        long N2     = N1 - ((365250L * Y1) / 1000L);
        long M1     = ((N2 * 1000L) - 500L) / 30600L;
        int  day    = (int)(((N2 * 1000L) - (30600L * M1) + 500L) / 1000L);
        int  month1 = (int)((M1 <= 9L)? (M1 + 3L) : (M1 - 9L));
        int  year   = (int)((M1 <= 9L)? Y1 : (Y1 + 1));
        return new ParsedDateTime(tmz, year, month1, day);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private TimeZone timeZone   = null;
    private long     timeMillis = 0L; // ms since January 1, 1970, 00:00:00 GMT

    /**
    *** Default constructor.  
    *** Initialize with the current Epoch time.
    **/
    public DateTime()
    {
        this.setTimeMillis(getCurrentTimeMillis());
    }

    /**
    *** Constructor.  
    *** Initialize with the current Epoch time.
    *** @param tz  The TimeZone
    **/
    public DateTime(TimeZone tz)
    {
        this();
        this.setTimeZone(tz);
    }

    /**
    *** Constructor.  
    *** @param date  The Date object used to initialize this DateTime
    **/
    public DateTime(Date date)
    {
        this.setTimeMillis((date != null)? date.getTime() : 0L);
    }

    /**
    *** Constructor.  
    *** @param date  The Date object used to initialize this DateTime
    *** @param tz  The TimeZone
    **/
    public DateTime(Date date, TimeZone tz)
    {
        this.setTimeMillis((date != null)? date.getTime() : 0L);
        this.setTimeZone(tz);
    }
    
    /**
    *** Constructor (time is set to Noon)
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    **/
    public DateTime(TimeZone tz, int year, int month1, int day)
    {
        this(tz, year, month1, day, 12, 0, 0); // Noon
    }
    
    /**
    *** Constructor.  
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    *** @param hour24  The hour of the day [24 hour clock]
    *** @param minute  The minute of the hour
    *** @param second  The second of the minute
    **/
    public DateTime(TimeZone tz, int year, int month1, int day, int hour24, int minute, int second)
    {
        this.setDate(tz, year, month1, day, hour24, minute, second);
    }

    /**
    *** Constructor.  
    *** @param timeSec  The Epoch time in seconds
    **/
    public DateTime(long timeSec)
    {
        if (timeSec > 9000000000L) {
            this.setTimeMillis(timeSec);
        } else {
            this.setTimeSec(timeSec);
        }
    }

    /**
    *** Constructor.  
    *** @param timeSec  The Epoch time in seconds
    *** @param tz       The TimeZone
    **/
    public DateTime(long timeSec, TimeZone tz)
    {
        this(timeSec);
        this.setTimeZone(tz);
    }

    /**
    *** Constructor.  
    *** @param d  A date/time String representation
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public DateTime(String d)
        throws DateParseException
    {
        this.setDate(d);
    }
    
    /**
    *** Copy constructor
    *** @param dt  Another DateTime instance 
    **/
    public DateTime(DateTime dt)
    {
        this.timeMillis = dt.timeMillis;
        this.timeZone   = dt.timeZone;
    }
    
    /**
    *** Copy constructor with delta offset time
    *** @param dt  Another DateTime instance 
    *** @param deltaOffsetSec  +/- offset from time specified in DateTime instance
    **/
    public DateTime(DateTime dt, long deltaOffsetSec)
    {
        this.timeMillis = dt.timeMillis + (deltaOffsetSec * 1000L);
        this.timeZone   = dt.timeZone;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets a Date object based on the time in this DateTime object
    *** @return A Date instance
    **/
    public java.util.Date getDate()
    {
        return new java.util.Date(this.getTimeMillis());
    }

    /**
    *** Sets the current date of this instance (time is left unchanged)
    *** @param tz      The TimeZone
    *** @param year    The year
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day
    **/
    public void setDate(TimeZone tz, int year, int month1, int day)
    {
        int hour24 = this.getHour24(tz);
        int minute = this.getMinute(tz);
        int second = this.getSecond(tz);
        this.setDate(tz, year, month1, day, hour24, minute, second);
    }

    /**
    *** Sets the current time of this instance
    *** @param tz      The TimeZone
    *** @param year    The year (1970+)
    *** @param month1  The 1-based month index [1..12]
    *** @param day     The day of the month
    *** @param hour24  The hour of the day [24 hour clock]
    *** @param minute  The minute of the hour
    *** @param second  The second of the minute
    **/
    public void setDate(TimeZone tz, int year, int month1, int day, int hour24, int minute, int second)
    {
        int month0 = month1 - 1;

        /* quick validate year */
        if (year   < 0) {
            year   = 1970;
        }

        /* quick validate month */
        if (month0 < 0) {
            month0 = 0;
        }

        /* quick validate day */
        if (day    < 1) {
            day    = 1;
        }

        /* quick validate time */
        if (hour24 < 0) {
            hour24 = 0;
            minute = 0;
            second = 0;
        } else
        if (minute < 0) {
            minute = 0;
            second = 0;
        } else
        if (second < 0) {
            second = 0;
        }

        /* set */
        this.setTimeZone(tz);
        Calendar cal = new GregorianCalendar(this._timeZone(tz));
        cal.set(year, month0, day, hour24, minute, second);
        Date date = cal.getTime();
        this.setTimeMillis(date.getTime());

    }

    /**
    *** Sets the current time of this instance
    *** @param d  The String representation of a date/time
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public void setDate(String d)
        throws DateParseException
    {
        this.setDate(d, (TimeZone)null);
    }

    /**
    *** Sets the current time of this instance
    *** @param d  The String representation of a date/time
    *** @param dftTMZ The TimeZone used if no timezone was parsed from the String
    *** @throws DateParseException if an invalid date/time format was specified.
    **/
    public void setDate(String d, TimeZone dftTMZ)
        throws DateParseException
    {
        String ds[] = StringTools.parseStringArray(d, " _");
        String dt = (ds.length > 0)? ds[0] : "";
        String tm = (ds.length > 1)? ds[1] : "";
        String tz = (ds.length > 2)? ds[2] : "";
        // Valid format: "YYYY/MM/DD [HH:MM:SS] [PST]"
        //Print.logInfo("Parsing " + dt + " " + tm + " " + tz);

        /* time-zone */
        TimeZone timeZone = null;
        if (ds.length > 2) {
            if (tz.equals("+0000") || tz.equals("-0000") || tz.equals("+00:00")) {
                timeZone = DateTime.getGMTTimeZone();
            } else
            if (DateTime.isValidTimeZone(tz)) {
                timeZone = DateTime.getTimeZone(tz);
            } else {
                throw new DateParseException("Invalid TimeZone[#1]: " + tz);
            }
        } else
        if ((ds.length > 1) && DateTime.isValidTimeZone(tm)) {
            tz = tm;
            tm = "";
            timeZone = DateTime.getTimeZone(tz);
        } else {
            timeZone = (dftTMZ != null)? dftTMZ : this.getTimeZone();
        }
        //Print.logInfo("Timezone = " + timeZone);
        this.setTimeZone(timeZone);
        Calendar calendar = new GregorianCalendar(timeZone);
        
        /* date */
        int yr = -1, mo = -1, dy = -1;
        int d1 = dt.indexOf('/'), d2 = (d1 > 0)? dt.indexOf('/', d1 + 1) : -1;
        if ((d1 > 0) && (d2 > d1)) {
            
            /* year */
            String YR = dt.substring(0, d1);
            yr = StringTools.parseInt(YR, -1);
            if ((yr >=  0) && (yr <= 49)) { yr += 2000; } else
            if ((yr >= 50) && (yr <= 99)) { yr += 1900; }
            if ((yr < 1900) || (yr > 2100)) {
                throw new DateParseException("Date/Year out of range: " + YR);
            }
            
            /* month */
            String MO = dt.substring(d1 + 1, d2);
            mo = StringTools.parseInt(MO, -1) - 1; // 0 indexed
            if ((mo < 0) || (mo > 11)) {
                throw new DateParseException("Date/Month out of range: " + MO);
            }
            
            /* seconds */
            String DY = dt.substring(d2 + 1);
            dy = StringTools.parseInt(DY, -1);
            if ((dy < 1) || (dy > 31)) {
                throw new DateParseException("Date/Day out of range: " + DY);
            }
            
        } else {
            
            throw new DateParseException("Invalid date format (Y/M/D): " + dt);
            
        }
        
        /* time */
        if (tm.equals("")) {
            
            //Print.logInfo("Just using YMD:" + yr + "/" + mo + "/" + dy);
            calendar.set(yr, mo, dy);
            
        } else {
            
            int hr = -1, mn = -1, sc = -1;
            int t1 = tm.indexOf(':'), t2 = (t1 > 0)? tm.indexOf(':', t1 + 1) : -1;
            if (t1 > 0) {
                
                /* hour */
                String HR = tm.substring(0, t1);
                hr = StringTools.parseInt(HR, -1);
                if ((hr < 0) || (hr > 23)) {
                    throw new DateParseException("Time/Hour out of range: " + HR);
                }
                if (t2 > t1) {
                    
                    /* minute */
                    String MN = tm.substring(t1 + 1, t2);
                    mn = StringTools.parseInt(MN, -1);
                    if ((mn < 0) || (mn > 59)) {
                        throw new DateParseException("Time/Minute out of range: " + MN);
                    }
                    
                    /* second */
                    String SC = tm.substring(t2 + 1);
                    sc = StringTools.parseInt(SC, -1);
                    if ((sc < 0) || (sc > 59)) {
                        throw new DateParseException("Time/Second out of range: " + SC);
                    }
                    
                    //Print.logInfo("Setting YMDHMS:" + yr + "/" + mo + "/" + dy+ " " + hr + ":" + mn + ":" + sc);
                    calendar.set(yr, mo, dy, hr, mn, sc);
                    
                } else {
                    
                    /* minute */
                    String MN = tm.substring(t1 + 1);
                    mn = StringTools.parseInt(MN, -1);
                    if ((mn < 0) || (mn > 59)) {
                        throw new DateParseException("Time/Minute out of range: " + MN);
                    }
                    
                    //Print.logInfo("Setting YMDHM:" + yr + "/" + mo + "/" + dy+ " " + hr + ":" + mn);
                    calendar.set(yr, mo, dy, hr, mn);
                    
                }
            } else {
                
                throw new DateParseException("Invalid time format (H:M:S): " + tm);
                
            }
        }
        
        /* ok */
        this.setTimeMillis(calendar.getTime().getTime());
        
    }

    /**
    *** Does not work, do not use
    **/
    private boolean setParsedDate_formatted(String d) // does not work!
    {
        try {
            java.util.Date date = DateFormat.getDateInstance().parse(d);
            this.setTimeMillis(date.getTime());
            return true;
        } catch (java.text.ParseException pe) {
            Print.logStackTrace("Unable to parse date: " + d, pe);
            this.setTimeMillis(0L);
            return false;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets a value from a GregorianCalendar based on the Epoch time of this instance
    *** @param tz    The TimeZone
    *** @param value The value number to return
    **/
    private int _get(TimeZone tz, int value)
    {
        return this.getCalendar(tz).get(value);
    }

    /**
    *** Gets a GregorianCalendar calendar instance
    *** @param tz  The TimeZone
    *** @return The Calendar object
    **/
    public Calendar getCalendar(TimeZone tz)
    {
        Calendar c = new GregorianCalendar(this._timeZone(tz));
        c.setTimeInMillis(this.getTimeMillis());
        return c;
    }

    /**
    *** Gets a GregorianCalendar calendar instance
    *** @return The Calendar object
    **/
    public Calendar getCalendar()
    {
        return this.getCalendar(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 0-based index of the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the 0-based month index
    *** @return The 0-based month index
    **/
    public int getMonth0(TimeZone tz)
    {
        // return 0..11
        return this._get(tz, Calendar.MONTH); // 0 indexed
    }

    /**
    *** Gets the 0-based index of the month represented by this DateTime instance
    *** @return The 0-based month index
    **/
    public int getMonth0()
    {
        return this.getMonth0(null);
    }

    /**
    *** Gets the 1-based index of the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the 0-based month index
    *** @return The 1-based month index
    **/
    public int getMonth1(TimeZone tz)
    {
        // return 1..12
        return this.getMonth0(tz) + 1;
    }

    /**
    *** Gets the 1-based index of the month represented by this DateTime instance
    *** @return The 1-based month index
    **/
    public int getMonth1()
    {
        return this.getMonth1(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the day of the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the day of the month
    *** @return The day of the month
    **/
    public int getDayOfMonth(TimeZone tz)
    {
        // return 1..31
        return this._get(tz, Calendar.DAY_OF_MONTH);
    }

    /**
    *** Gets the day of the month represented by this DateTime instance
    *** @return The day of the month
    **/
    public int getDayOfMonth()
    {
        return this.getDayOfMonth(null);
    }

    /**
    *** Gets the number of days in the month represented by this DateTime instance
    *** @param tz  The TimeZone used when calculating the number of days in the month
    *** @return The number of days in the month
    **/
    public int getDaysInMonth(TimeZone tz)
    {
        return DateTime.getDaysInMonth(tz, this.getMonth1(tz), this.getYear(tz));
    }

    /**
    *** Gets the number of days in the month represented by this DateTime instance
    *** @return The number of days in the month
    **/
    public int getDaysInMonth()
    {
        return this.getDaysInMonth(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the day of week for this DateTime instance
    *** @param tz  The TimeZone used when calculating the day of week
    *** @return The day of week (0=Sunday ... 6=Saturday)
    **/
    public int getDayOfWeek(TimeZone tz)
    {
        // return 0..6
        return this._get(tz, Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }

    /**
    *** Returns the day of week for this DateTime instance
    *** @return The day of week (0=Sunday ... 6=Saturday)
    **/
    public int getDayOfWeek()
    {
        return this.getDayOfWeek(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the year for this DataTime instance
    *** @param tz  The TimeZone used when calculating the year
    *** @return The year.
    **/
    public int getYear(TimeZone tz)
    {
        return this._get(tz, Calendar.YEAR);
    }
   
    /**
    *** Returns the year for this DataTime instance
    *** @return The year.
    **/
    public int getYear()
    {
        return this.getYear(null);
    }

    /** 
    *** Returns true if this DateTime instance represents a leap-year
    *** @param tz  The TimeZone used when calculating the leap-year
    *** @return True if the year is a leap-year, false otherwise
    **/
    public boolean isLeapYear(TimeZone tz)
    {
        GregorianCalendar gc = (GregorianCalendar)this.getCalendar(tz);
        return gc.isLeapYear(gc.get(Calendar.YEAR));
    }
 
    /** 
    *** Returns true if this DateTime instance represents a leap-year
    *** @return True if the year is a leap-year, false otherwise
    **/
    public boolean isLeapYear()
    {
        return this.isLeapYear(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the hour of the day (24-hour clock)
    *** @param tz  The TimeZone used when calculating the hour of the day
    *** @return The hour of the day
    **/
    public int getHour24(TimeZone tz)
    {
        return this._get(tz, Calendar.HOUR_OF_DAY);
    }

    /**
    *** Returns the hour of the day (24-hour clock)
    *** @return The hour of the day
    **/
    public int getHour24()
    {
        return this.getHour24(null);
    }

    /**
    *** Returns the hour of the day (12-hour clock)
    *** @param tz  The TimeZone used when calculating the hour of the day
    *** @return The hour of the day
    **/
    public int getHour12(TimeZone tz)
    {
        return this._get(tz, Calendar.HOUR);
    }

    /**
    *** Returns the hour of the day (12-hour clock)
    *** @return The hour of the day
    **/
    public int getHour12()
    {
        return this.getHour12(null);
    }

    /**
    *** Returns true if AM, false if PM
    *** @param tz  The TimeZone used when calculating AM/PM
    *** @return True if AM, false if PM
    **/
    public boolean isAM(TimeZone tz)
    {
        return this._get(tz, Calendar.AM_PM) == Calendar.AM;
    }

    /**
    *** Returns true if AM, false if PM
    *** @return True if AM, false if PM
    **/
    public boolean isAM()
    {
        return this.isAM(null);
    }

    /**
    *** Returns true if PM, false if AM
    *** @param tz  The TimeZone used when calculating AM/PM
    *** @return True if PM, false if AM
    **/
    public boolean isPM(TimeZone tz)
    {
        return this._get(tz, Calendar.AM_PM) == Calendar.PM;
    }

    /**
    *** Returns true if PM, false if AM
    *** @return True if PM, false if AM
    **/
    public boolean isPM()
    {
        return this.isPM(null);
    }

    /**
    *** Returns the minute of the hour
    *** @param tz  The TimeZone used when calculating the minute
    *** @return The minute of the hour
    **/
    public int getMinute(TimeZone tz)
    {
        return this._get(tz, Calendar.MINUTE);
    }

    /**
    *** Returns the minute of the hour
    *** @return The minute of the hour
    **/
    public int getMinute()
    {
        return this.getMinute(null);
    }

    /**
    *** Returns the minute of the current day
    *** @param tz  The TimeZone used when calculating the minute
    *** @return The minute of the current day
    **/
    public int getMinuteOfDay(TimeZone tz)
    {
        int h = this.getHour24(tz);
        int m = this.getMinute(tz);
        return (h * 60) + m;
    }

    /**
    *** Returns the minute of the current day
    *** @return The minute of the current day
    **/
    public int getMinuteOfDay()
    {
        return this.getMinuteOfDay(null);
    }

    /**
    *** Returns the second of the minute
    *** @param tz  The TimeZone used when calculating the second
    *** @return The second of the minute
    **/
    public int getSecond(TimeZone tz)
    {
        return this._get(tz, Calendar.SECOND);
    }

    /**
    *** Returns the second of the minute
    *** @return The second of the minute
    **/
    public int getSecond()
    {
        return this.getSecond(null);
    }

    /**
    *** Returns the second of the current day (Time of day)
    *** @param tz  The TimeZone used when calculating the second
    *** @return The second of the current day
    **/
    public int getSecondOfDay(TimeZone tz)
    { // TimeOfDay, TOD
        int h = this.getHour24(tz);
        int m = this.getMinute(tz);
        int s = this.getSecond(tz);
        return (h * 3600) + (m * 60) + s;
    }

    /**
    *** Returns the second of the current day
    *** @return The second of the current day
    **/
    public int getSecondOfDay()
    {
        return this.getSecondOfDay(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this DateTime instance currently represents a daylight savings time.
    *** @param tz  The TimeZone used when calculating daylight savings time.
    *** @return True if this DateTime instance currently represents a daylight savings time.
    **/
    public boolean isDaylightSavings(TimeZone tz)
    {
        return _timeZone(tz).inDaylightTime(this.getDate());
    }

    /**
    *** Returns true if this DateTime instance currently represents a daylight savings time.
    *** @return True if this DateTime instance currently represents a daylight savings time.
    **/
    public boolean isDaylightSavings()
    {
        return this.isDaylightSavings(null);
    }
 
    // ------------------------------------------------------------------------

    /**
    *** Returns the DayNumber long, for this DateTime instance
    *** @param tz the Timezone
    *** @return The long day number
    **/
    public long getDayNumber(TimeZone tz)
    {
        return DateTime.getDayNumberFromDate(this, tz);
    }

    /**
    *** Returns the DayNumber long, for this DateTime instance
    *** @return The long day number
    **/
    public long getDayNumber()
    {
        return this.getDayNumber(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Epoch time in seconds represented by this instance
    *** @return The Epoch time in seconds
    **/
    public long getTimeSec()
    {
        return this.getTimeMillis() / 1000L;
    }

    /** 
    *** Sets the Epoch time in seconds represented by this instance
    *** @param timeSec  Epoch time in seconds
    **/
    public void setTimeSec(long timeSec)
    {
        this.timeMillis = timeSec * 1000L;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Epoch time in milliseconds represented by this instance
    *** @return The Epoch time in milliseconds
    **/
    public long getTimeMillis()
    {
        return this.timeMillis;
    }

    /** 
    *** Sets the Epoch time in milliseconds represented by this instance
    *** @param timeMillis  Epoch time in milliseconds
    **/
    public void setTimeMillis(long timeMillis)
    {
        this.timeMillis = timeMillis;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns an Epoch time in seconds which is the beginning of the day
    *** represented by this instance.
    *** @param tz  The TimeZone
    *** @return An Epoch time which is at the beginning of the day represented
    ***         by this instance.
    **/
    public long getDayStart(TimeZone tz)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        Calendar nc = new GregorianCalendar(tz);
        nc.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return nc.getTime().getTime() / 1000L;
    }
    
    /**
    *** Returns an Epoch time in seconds which is the beginning of the day
    *** represented by this instance.
    *** @return An Epoch time which is at the beginning of the day represented
    ***         by this instance.
    **/
    public long getDayStart()
    {
        return this.getDayStart(null);
    }
    
    /**
    *** Returns an Epoch time in seconds which is the end of the day
    *** represented by this instance.
    *** @param tz  The TimeZone
    *** @return An Epoch time which is at the end of the day represented
    ***         by this instance.
    **/
    public long getDayEnd(TimeZone tz)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        Calendar nc = new GregorianCalendar(tz);
        nc.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return nc.getTime().getTime() / 1000L;
    }
    
    /**
    *** Returns an Epoch time in seconds which is the end of the day
    *** represented by this instance.
    *** @return An Epoch time which is at the end of the day represented
    ***         by this instance.
    **/
    public long getDayEnd()
    {
        return this.getDayEnd(null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param tz  The overriding TimeZone
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthStart(TimeZone tz, int deltaMo)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        int YY = c.get(Calendar.YEAR);
        int MM = c.get(Calendar.MONTH); // 0..11
        if (deltaMo != 0) {
            MM += deltaMo;
            if (MM < 0) {
                YY += (MM - (MONTHS_PER_YEAR - 1)) / MONTHS_PER_YEAR;
                MM %= MONTHS_PER_YEAR;
                if (MM < 0) {
                    MM += MONTHS_PER_YEAR;
                }
            } else {
                YY += MM / MONTHS_PER_YEAR;
                MM %= MONTHS_PER_YEAR;
            }
        }
        int DD = 1;
        Calendar nc = new GregorianCalendar(tz);
        nc.set(YY, MM, DD, 0, 0, 0);
        return nc.getTime().getTime() / 1000L;
    }

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance.
    *** @param tz  The overriding TimeZone
    *** @return The Epoch timestamp
    **/
    public long getMonthStart(TimeZone tz)
    {
        return this.getMonthStart(tz, 0);
    }

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthStart(int deltaMo)
    {
        return this.getMonthStart(null, deltaMo);
    }

    /**
    *** Returns the Epoch timestamp representing the beginning of the month
    *** represented by this DateTime instance.
    *** @return The Epoch timestamp
    **/
    public long getMonthStart()
    {
        return this.getMonthStart(null, 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param tz  The overriding TimeZone
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd(TimeZone tz, int deltaMo)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        int YY = c.get(Calendar.YEAR);
        int MM = c.get(Calendar.MONTH); // 0..11
        if (deltaMo != 0) {
            MM += deltaMo;
            if (MM < 0) {
                YY += (MM - (MONTHS_PER_YEAR - 1)) / MONTHS_PER_YEAR;
                MM %= MONTHS_PER_YEAR;
                if (MM < 0) {
                    MM += MONTHS_PER_YEAR;
                }
            } else {
                YY += MM / MONTHS_PER_YEAR;
                MM %= MONTHS_PER_YEAR;
            }
        }
        int DD = DateTime.getDaysInMonth(tz, MM + 1, YY);
        Calendar nc = new GregorianCalendar(tz);
        nc.set(YY, MM, DD, 23, 59, 59);
        return nc.getTime().getTime() / 1000L;
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance.
    *** @param tz  The overriding TimeZone
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd(TimeZone tz)
    {
        return this.getMonthEnd(tz, 0);
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance, plus the specified delta month offset.
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd(int deltaMo)
    {
        return this.getMonthEnd(null, deltaMo);
    }

    /**
    *** Returns the Epoch timestamp representing the end of the month
    *** represented by this DateTime instance.
    *** @return The Epoch timestamp
    **/
    public long getMonthEnd()
    {
        return this.getMonthEnd(null, 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Epoch timestamp representing the current date, plus the number of
    *** months offset.
    *** @param tz       The overriding TimeZone
    *** @param deltaMo  The delta monnths (added to the month represented by this DateTime instance)
    *** @return The Epoch timestamp
    **/
    public long getMonthDelta(TimeZone tz, int deltaMo)
    {
        if (tz == null) { tz = _timeZone(tz); }
        Calendar c  = this.getCalendar(tz);
        int YY = c.get(Calendar.YEAR);
        int MM = c.get(Calendar.MONTH); // 0..11
        if (deltaMo != 0) {
            MM += deltaMo;
            if (MM < 0) {
                YY += (MM - (MONTHS_PER_YEAR - 1)) / MONTHS_PER_YEAR;
                MM %= MONTHS_PER_YEAR;
                if (MM < 0) {
                    MM += MONTHS_PER_YEAR;
                }
            } else {
                YY += MM / MONTHS_PER_YEAR;
                MM %= MONTHS_PER_YEAR;
            }
        }
        int DD = c.get(Calendar.DAY_OF_MONTH); // 1..31
        int maxDays = DateTime.getDaysInMonth(tz, MM + 1, YY);
        if (DD > maxDays) { DD = maxDays; }
        Calendar nc = new GregorianCalendar(tz);
        if (deltaMo <= 0) {
            nc.set(YY, MM, DD,  0,  0,  0);
        } else {
            nc.set(YY, MM, DD, 23, 59, 59);
        }
        return nc.getTime().getTime() / 1000L;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the Epoch timestamp representing the start of the current day represented
    *** by this DateTime instance, based on the GMT TimeZone.
    *** @return The Epoch timestamp
    **/
    public long getDayStartGMT()
    {
        // GMT TimeZone
        return (this.getTimeSec() / SECONDS_PER_DAY) * SECONDS_PER_DAY;
    }

    /**
    *** Returns the Epoch timestamp representing the end of the current day represented
    *** by this DateTime instance, based on the GMT TimeZone.
    *** @return The Epoch timestamp
    **/
    public long getDayEndGMT()
    {
        // GMT TimeZone
        return this.getDayStartGMT() + SECONDS_PER_DAY - 1L;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the this DateTime is more than the specified seconds into the future.
    *** @param deltaSec The number of seconds into the future.
    *** @return True if this DateTime is more than the specified seconds into the future.
    **/
    public boolean isFuture(long deltaSec)
    {
        TimeZone tz = this.getTimeZone();
        DateTime dt = new DateTime(DateTime.getCurrentTimeSec() + deltaSec, tz);
        return this.isAfter(dt, false);
    }

    /**
    *** Returns true if the this DateTime is <b>after</b> the specified DateTime instance
    *** @param dt the other DateTime instance
    *** @return True if this DateTime is after the specified DateTime instance.
    **/
    public boolean isAfter(DateTime dt)
    {
        return this.isAfter(dt, false);
    }

    /**
    *** Returns true if the this DateTime is <b>after</b> the specified DateTime instance
    *** @param dt the other DateTime instance
    *** @param inclusive  True to test for this "equal-to or after" the specified date, 
    ***         false to test only for "after".
    *** @return True if this DateTime is after the specified DateTime instance (or "equal-to" if
    ***         'inclusive' is true).
    **/
    public boolean isAfter(DateTime dt, boolean inclusive)
    {
        if (dt == null) {
            return true; // arbitrary
        } else
        if (inclusive) {
            return (this.getTimeMillis() >= dt.getTimeMillis());
        } else {
            return (this.getTimeMillis() > dt.getTimeMillis());
        }
    }

    /**
    *** Returns true if this DateTime is <b>before</b> the specified DateTime instance
    *** @param dt the other DateTime instance
    *** @return True if this DateTime is <b>before</b> the specified DateTime instance
    **/
    public boolean isBefore(DateTime dt)
    {
        return this.isBefore(dt, false);
    }

    /**
    *** Returns true if this DateTime is <b>before</b> the specified DateTime instance
    *** @param dt the other DateTime instance
    *** @param inclusive  True to test for "equal-to or before", false to test only for "before".
    *** @return True if this DateTime is before the specified DateTime instance (or "equal-to" if
    ***         'inclusive' is true).
    **/
    public boolean isBefore(DateTime dt, boolean inclusive)
    {
        if (dt == null) {
            return false; // arbitrary
        } else
        if (inclusive) {
            return (this.getTimeMillis() <= dt.getTimeMillis());
        } else {
            return (this.getTimeMillis() < dt.getTimeMillis());
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Return the number of years since specified date (year/month/day).
    *** @param priorDate  Prior date
    *** @return Number of years (and fractional years)
    **/
    public double getYearsSince(DateTime priorDate)
    {
        return this.getYearsSince(priorDate, null);
    }

    /**
    *** Return the number of years since specified date (year/month/day).
    *** @param priorDate  Prior date
    *** @return Number of years (and fractional years)
    **/
    public double getYearsSince(DateTime priorDate, TimeZone tz)
    {
        long priorDayNumber = DateTime.getDayNumberFromDate(priorDate, tz);
        return this.getYearsSince(priorDayNumber);
    }

    /**
    *** Return the number of years since specified date (year/month/day).
    *** @param year   Prior year
    *** @param month1 Month of prior year (1..12)
    *** @param day    Day of month
    *** @return Number of years (and fractional years)
    **/
    public double getYearsSince(int year, int month1, int day)
    {
        long priorDayNumber = DateTime.getDayNumberFromDate(year, month1, day);
        return this.getYearsSince(priorDayNumber, null);
    }

    /**
    *** Return the number of years since specified date (year/month/day).
    *** @param priorDayNumber  Prior day-number
    *** @return Number of years (and fractional years)
    **/
    public double getYearsSince(long priorDayNumber)
    {
        return this.getYearsSince(priorDayNumber, null);
    }
    
    /**
    *** Return the number of years since specified date (year/month/day).
    *** @param priorDayNumber  Prior day-number
    *** @param tz              TimeZone
    *** @return Number of years (and fractional years)
    **/
    public double getYearsSince(long priorDayNumber, TimeZone tz)
    {

        /* get day numbers */
        boolean reverse = false;
        long thisDayNumber = DateTime.getDayNumberFromDate(this, tz);
        long thatDayNumber = priorDayNumber;
        if (thisDayNumber == thatDayNumber) {
            return 0.0;
        } else
        if (thisDayNumber < thatDayNumber) {
            long x = thisDayNumber;
            thisDayNumber = thatDayNumber;
            thatDayNumber = x;
            reverse = true;
        }
        ParsedDateTime thisPDT = DateTime.getDateFromDayNumber(thisDayNumber, tz);
        ParsedDateTime thatPDT = DateTime.getDateFromDayNumber(thatDayNumber, tz);

        /* days in year */
        int daysInYear = 365;
        int deltaYear = thisPDT.year - thatPDT.year;
        int thisDOY = DateTime.getDayOfYear(thisPDT.year, thisPDT.month1, thisPDT.day);
        int thatDOY = DateTime.getDayOfYear(thatPDT.year, thatPDT.month1, thatPDT.day);
        boolean thisLeap = DateTime.isLeapYear(thisPDT.year);
        boolean thatLeap = DateTime.isLeapYear(thatPDT.year);
        if (thisLeap && thatLeap) {
            daysInYear = 366; // 366 days in both years
        } else
        if (thisLeap && !thatLeap) {
            if (thatDOY > (31 + 28)) { // Jan + Feb
                thatDOY++; // adjust non-leap year
                daysInYear = 366;
            }
        } else
        if (!thisLeap && thatLeap) {
            if (thisDOY > (31 + 28)) { // Jan + Feb
                thisDOY++; // adjust non-leap year
                daysInYear = 366;
            }
        }

        /* age in years */
        double ageYears = (thisDOY >= thatDOY)?
            ((double)deltaYear + ((double)(thisDOY - thatDOY) / (double)daysInYear)) :
            (((double)deltaYear - 1.0) + (((double)thisDOY + daysInYear) - (double)thatDOY) / (double)daysInYear);
        return reverse? -ageYears : ageYears;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified DateTime is <b>equal-to</b> this DateTime instance
    *** @param obj the other DateTime instance
    *** @return True if the specified DateTime is <b>equal-to</b> this DateTime instance
    **/
    public boolean equals(Object obj) 
    {
        if (obj instanceof DateTime) {
            return (this.getTimeMillis() == ((DateTime)obj).getTimeMillis());
        } else {
            return false;
        }
    }
    
    /**
    *** Compares another DateTime instance to this instance.
    *** @param other  The other DateTime instance.
    *** @return &lt;0 of the other DateTime instance is before this instance, 0 if the other DateTime
    ***         instance is equal to this instance, and &gt;0 if the other DateTime instance is
    ***         after this instance.
    **/
    public int compareTo(Object other)
    {
        if (other instanceof DateTime) {
            long otherTime = ((DateTime)other).getTimeMillis();
            long thisTime  = this.getTimeMillis();
            if (thisTime < otherTime) { return -1; }
            if (thisTime > otherTime) { return  1; }
            return 0;
        } else {
            return -1;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** TimeZoneProvider interface
    **/
    public interface TimeZoneProvider
    {
        public TimeZone getTimeZone();
    }

    /**
    *** Returns a non-null TimeZone
    *** @param tz The TimeZone returned (if non-null)
    *** @return The specified TimeZone, or the default TimeZone if the specified TimeZone is null
    **/
    protected TimeZone _timeZone(TimeZone tz)
    {
        return (tz != null)? tz : this.getTimeZone();
    }

    /**
    *** Returns the default (or current) TimeZone
    *** @return The default (or current) TimeZone
    **/
    public TimeZone getTimeZone()
    {
        return (this.timeZone != null)? this.timeZone : DateTime.getDefaultTimeZone();
    }

    /**
    *** Returns the ID of the current TimeZone
    *** @return The ID of the current TimeZone
    **/
    public String getTimeZoneID()
    {
        return this.getTimeZone().getID();
    }

    /**
    *** Returns the short-name of the current TimeZone
    *** @return The short-name of the current TimeZone
    **/
    public String getTimeZoneShortName()
    {
        boolean dst = this.isDaylightSavings();
        return this.getTimeZone().getDisplayName(dst, TimeZone.SHORT);
    }
    
    /**
    *** Sets the current TimeZone
    *** @param tz  The TimeZone to set
    **/
    public void setTimeZone(TimeZone tz)
    {
        this.timeZone = tz;
    }
    
    /**
    *** Sets the current TimeZone
    *** @param tz  The TimeZone ID to set
    **/
    public void setTimeZone(String tz)
    {
        this.setTimeZone(DateTime.getTimeZone(tz, null));
    }

    /**
    *** Reads the available TimeZone IDs from the specified file.
    *** @param tmzFile  The file from which TimeZone IDs are read.
    *** @return A String array of read TimeZone IDs.
    **/
    public static String[] readTimeZones(File tmzFile)
    {
        if ((tmzFile != null) && tmzFile.exists()) {
            java.util.List<String> tzList = new Vector<String>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(tmzFile));
                for (;;) {
                    String rline = br.readLine();
                    if (rline == null) { break; }
                    String tline = rline.trim();
                    if (!tline.equals("") && !tline.startsWith("#")) {
                        tzList.add(tline);
                    }
                }
                return tzList.toArray(new String[tzList.size()]);
            } catch (IOException ioe) {
                Print.logError("Unable to read file: " + tmzFile + " [" + ioe + "]");
                return null;
            } finally {
                if (br != null) { try { br.close(); } catch (IOException ioe) {/*ignore*/} }
            }
        }
        return null;
    }
    
    // ------------------------------------------------------------------------

    private static Object TimeZoneDSTMap_initLock = new Object();
    private static Map<String,String> TimeZoneDSTMap = null;
    private static Map<String,String> _GetTimeZoneDSTMap()
    {
        if (TimeZoneDSTMap == null) {
            // -- Lazy init of the DST TimeZone lookup table---
            synchronized (TimeZoneDSTMap_initLock) {
                if (TimeZoneDSTMap == null) { // try again after lock
                    HashMap<String,String> dstMap = new HashMap<String,String>();
                    for (String tzi : TimeZone.getAvailableIDs()) {
                        TimeZone tz = TimeZone.getTimeZone(tzi); // case sensitive
                        // daylight savings time short-name lookup
                        String dst = tz.getDisplayName(true,TimeZone.SHORT); // "PDT
                        if (!StringTools.isBlank(dst)) {
                            String dstName = LOWERCASE_TIMEZONE_LOOKUP? dst.toLowerCase() : dst;
                            dstMap.put(dstName, tzi); // insert "PDT" or "pdt"
                        }
                        // lowercase lookup ("US/Pacific"==>"us/pacific")
                        if (LOWERCASE_TIMEZONE_LOOKUP) {
                            // insert other key lookup values in lowercase
                            dstMap.put(tzi.toLowerCase(), tzi); // insert "us/pacific"
                            String sdn = tz.getDisplayName(false,TimeZone.SHORT); // "PST"
                            if (!StringTools.isBlank(sdn)) {
                                dstMap.put(sdn.toLowerCase(), tzi); // insert "pst"
                            }
                        }
                    }
                    TimeZoneDSTMap = dstMap;
                }
            }
        }
        return TimeZoneDSTMap;
    }

    private static TimeZone _lookupTimeZone(String tzid, TimeZone dft)
    {

        /* blank TimeZone ID */
        if (StringTools.isBlank(tzid)) {
            return dft;
        }

        /* GMT/UTC/Zulu timezones */
        if (tzid.equalsIgnoreCase(GMT_TIMEZONE) || 
            tzid.equalsIgnoreCase("UTC")        || 
            tzid.equalsIgnoreCase("Zulu")         ) {
            return TimeZone.getTimeZone(tzid); // case sensitive
        } else
        if (tzid.equalsIgnoreCase("Z")) {
            return TimeZone.getTimeZone("Zulu"); // case sensitive
        }

        /* prefixed with +/-? */
        if (tzid.startsWith("+")) {
            if (tzid.equals("+0") || tzid.equals("+0000") || tzid.equals("+00:00")) {
                return TimeZone.getTimeZone(GMT_TIMEZONE); // case sensitive
            }
            tzid = GMT_TIMEZONE + tzid;
        } else
        if (tzid.startsWith("-")) {
            if (tzid.equals("-0") || tzid.equals("-0000") || tzid.equals("-00:00")) {
                return TimeZone.getTimeZone(GMT_TIMEZONE); // case sensitive
            }
            tzid = GMT_TIMEZONE + tzid;
        }

        /* lookup TimeZone ID/name */
        //Print.logInfo("Looking up Timezone: " + tzid);
        // "TimeZone.getTimeZone" returns GMT for invalid timezones
        TimeZone tmz = TimeZone.getTimeZone(tzid); // case sensitive
        if (tmz.getRawOffset() != 0) { // ie. !(GMT+0)
            return tmz; // must be a valid time-zone
        }
        if (!tmz.getID().equals(GMT_TIMEZONE)) {
            return tmz;
        }

        /* ID/name lookup failed, try looking up the DST name */
        // If the above specified TimeZone ID/name is a short DST name (such 
        // as "PDT", "CDT", etc), the lookup will likely fail.  This section  
        // attempts to create a cross referenced map of DST names to the 
        // non-DST ID.
        String dstTmzID = _GetTimeZoneDSTMap().get(LOWERCASE_TIMEZONE_LOOKUP? tzid.toLowerCase() : tzid);
        TimeZone dstTmz = (dstTmzID != null)? TimeZone.getTimeZone(dstTmzID) : null; // case sensitive
        return ((dstTmz != null) && (dstTmz.getRawOffset() != 0))? dstTmz : dft;

    }

    /**
    *** Returns true if the specified TimeZone is valid
    *** @param tzid  The String representation of a TimeZone
    *** @return True if the TimeZone is valid, false otherwise
    **/
    public static boolean isValidTimeZone(String tzid)
    {
        if (StringTools.isBlank(tzid)) {
            return false;
        } else
        if (tzid.equalsIgnoreCase(GMT_TIMEZONE) || 
            tzid.equalsIgnoreCase("UTC")        || 
            tzid.equalsIgnoreCase("Zulu")         ) {
            return true;
        } else {
            TimeZone tmz = DateTime._lookupTimeZone(tzid,null);
            return (tmz != null)? true : false;
        }
    }

    /**
    *** Gets the TimeZone instance for the specified TimeZone id
    *** @param tzid  The TimeZone id
    *** @param dft   The default TimeZone to return if the specified TimeZone id
    ***              does not exist.
    *** @return The TimZone
    **/
    public static TimeZone getTimeZone(String tzid, TimeZone dft)
    {
        if (StringTools.isBlank(tzid)) {
            return dft;
        } else
        if (tzid.equalsIgnoreCase(GMT_TIMEZONE) || 
            tzid.equalsIgnoreCase("UTC")        || 
            tzid.equalsIgnoreCase("Zulu")         ) {
            return TimeZone.getTimeZone(tzid); // case sensitive
        } else {
            return DateTime._lookupTimeZone(tzid,dft);
        }
    }

    /**
    *** Gets the TimeZone instance for the specified TimeZone id
    *** @param tzid  The TimeZone id
    *** @return The TimeZone
    **/
    public static TimeZone getTimeZone(String tzid)
    {
        if (StringTools.isBlank(tzid)) {
            return TimeZone.getDefault(); // local system default time-zone
        } else {
            TimeZone tmz = DateTime._lookupTimeZone(tzid,null);
            return (tmz != null)? tmz : TimeZone.getDefault(); // DateTime.getGMTTimeZone();
        }
    }

    /**
    *** Returns the default TimeZone
    *** @return The default TimeZone
    **/
    public static TimeZone getDefaultTimeZone()
    {
        return DateTime.getTimeZone(null);
    }

    /**
    *** Returns the GMT TimeZone
    *** @return The GMT TimeZone
    **/
    public static TimeZone getGMTTimeZone()
    {
        return TimeZone.getTimeZone(GMT_TIMEZONE); // case sensitive
    }

    // ------------------------------------------------------------------------

    private static SimpleDateFormat simpleFormatter = null;

    /**
    *** Returns a String representation of this DateTime instance
    **/
    public String toString() 
    {
        return this.toString(null);
    }

    /**
    *** Returns a String representation of this DateTime instance
    **/
    public String toString(TimeZone tmz) 
    {
        if (simpleFormatter == null) {
            // eg. "Sun Mar 26 12:38:12 PST 2006"
            simpleFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        }
        synchronized (simpleFormatter) {
            simpleFormatter.setTimeZone(this._timeZone(tmz));
            return simpleFormatter.format(this.getDate());
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Formats the specified Date instance.
    *** @param date The Date instance
    *** @param tz   The TimeZone
    *** @param dtFmt  The Date/Time format
    *** @return The String representation of the StringBuffer destination.
    **/
    public static String format(java.util.Date date, TimeZone tz, String dtFmt)
    {
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat sdf = null;
        try {
            String f = (dtFmt != null)? dtFmt : DEFAULT_DATETIME_FORMAT;
            sdf = new SimpleDateFormat(f);
        } catch (IllegalArgumentException iae) {
            Print.logException("Invalid date/time format: " + dtFmt, iae);
            sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT); // assumed to be valid
        }
        sdf.setTimeZone((tz != null)? tz : DateTime.getDefaultTimeZone()); 
        sdf.format(date, sb, new FieldPosition(0));
        return sb.toString();
    }

    /** 
    *** Formats the current DateTime instance.
    *** @param dtFmt  The Date/Time format
    *** @param tz   The overriding TimeZone
    *** @param sb   The StringBuffer where the formatted Date/Time is placed (may be null)
    *** @return The String representation of the StringBuffer destination.
    **/
    public String format(String dtFmt, TimeZone tz, StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        SimpleDateFormat sdf = null;
        try {
            String f = (dtFmt != null)? dtFmt : DEFAULT_DATETIME_FORMAT;
            sdf = new SimpleDateFormat(f);
        } catch (IllegalArgumentException iae) {
            Print.logError("Invalid date/time format: " + dtFmt + " [" + iae.getMessage() + "]");
            sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT); // assumed to be valid
        }
        sdf.setTimeZone(this._timeZone(tz)); 
        sdf.format(this.getDate(), sb, new FieldPosition(0));
        return sb.toString();
    }

    /** 
    *** Formats the current DateTime instance.
    *** @param fmt  The Date/Time format
    *** @param tz   The overriding TimeZone
    *** @return The formatted Date/Time String
    **/
    public String format(String fmt, TimeZone tz)
    {
        return this.format(fmt, tz, null);
    }

    /** 
    *** Formats the current DateTime instance (using format "MMM dd, yyyy HH:mm:ss z")
    *** @param tz   The overriding TimeZone
    *** @return The formatted Date/Time String
    **/
    public String shortFormat(TimeZone tz)
    {   // ie. "2003/10/23 7:23:18 PST"
        return this.format("yyyy/MM/dd HH:mm:ss zzz", tz, null);
    }

    /** 
    *** Formats the current DateTime instance (using format "MMM dd, yyyy HH:mm:ss z")
    *** @param tz   The overriding TimeZone
    *** @return The formatted Date/Time String
    **/
    public String format(TimeZone tz)
    {
        return this.format("MMM dd, yyyy HH:mm:ss z", tz, null);
        //DateFormat dateFmt = DateFormat.getDateTimeInstance();
        //dateFmt.setTimeZone(tz);
        //return dateFmt.format(new java.util.Date(this.getTimeMillis()));
    }

    /** 
    *** Formats the current DateTime instance.
    *** @param fmt  The Date/Time format
    *** @return The formatted Date/Time String
    **/
    public String format(String fmt)
    {
        return this.format(fmt, null, null);
    }

    /** 
    *** Formats the current DateTime instance, based on the GMT TimeZone.
    *** @param fmt  The Date/Time format
    *** @return The formatted Date/Time String
    **/
    public String gmtFormat(String fmt)
    {
        return this.format(fmt, DateTime.getGMTTimeZone(), null);
    }

    /** 
    *** Formats the current DateTime instance (using format "yyyy/MM/dd HH:mm:ss 'GMT'"), based on
    *** the GMT TimeZone.
    *** @return The formatted Date/Time String
    **/
    public String gmtFormat()
    {
        return this.gmtFormat("yyyy/MM/dd HH:mm:ss 'GMT'");
    }

    /** 
    *** Formats the current DateTime instance, based on the GMT TimeZone.
    *** @param fmt  The Date/Time format
    *** @param sb   The StringBuffer where the formatted Date/Time is placed (may be null)
    *** @return The String representation of the StringBuffer destination.
    **/
    public String gmtFormat(String fmt, StringBuffer sb)
    {
        return this.format(fmt, DateTime.getGMTTimeZone(), sb);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this DateTime instance
    *** @return A clone of this DateTime instance
    **/
    public Object clone()
    {
        return new DateTime(this);
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Formats the time-of-day, based on the specified format.
    *** @param tod  The time-of-day
    *** @param fmt  The format
    *** @return The formatted time-of-day
    **/
    public static String encodeHourMinuteSecond(long tod, String fmt)
    {
        StringBuffer sb = new StringBuffer();
        int h = (int)(tod / (60L * 60L)), m = (int)((tod / 60L) % 60), s = (int)(tod % 60);
        if (fmt != null) {
            // format: "00:00:00", "00:00"
            String f[] = StringTools.parseStringArray(fmt, ':');
            if (f.length > 0) { sb.append(StringTools.format(h,f[0])); }              // hours
            if (f.length > 1) { sb.append(':').append(StringTools.format(m,f[1])); }  // minutes
            if (f.length > 2) { sb.append(':').append(StringTools.format(s,f[2])); }  // seconds
        } else {
            sb.append(h);
            sb.append(':').append(StringTools.format(m,"00"));
            if (s > 0) {
                sb.append(':').append(StringTools.format(s,"00"));
            }
        }
        return sb.toString();
    }

    /**
    *** Parses the specified "hhhh:mm:ss" String into the number of represented seconds
    *** @param hms  The String containing the "hhhh:mm:ss" to parse
    *** @return The number of seconds represented by the specified String format
    **/
    public static int parseHourMinuteSecond(String hms)
    {
        return DateTime.parseHourMinuteSecond(hms, 0);
    }

    /**
    *** Parses the specified "hhhh:mm:ss" String into the number of represented seconds
    *** @param hms  The String containing the "hhhh:mm:ss" to parse
    *** @param dft  The default seconds returned if unable to parse the specified String.
    *** @return The number of seconds represented by the specified String format
    **/
    public static int parseHourMinuteSecond(String hms, int dft)
    {
        String a[] = StringTools.parseStringArray(hms,":");
        if (a.length <= 1) {
            // assume all seconds
            return StringTools.parseInt(hms, dft);
        } else
        if (a.length == 2) {
            // assume "hhhh:mm"
            int h = StringTools.parseInt(a[0], -1);
            int m = StringTools.parseInt(a[1], -1);
            return ((h >= 0) && (m >= 0))? (((h * 60) + m) * 60) : dft;
        } else { // (a.length >= 3)
            // assume "hhhh:mm:ss"
            int h = StringTools.parseInt(a[0], -1);
            int m = StringTools.parseInt(a[1], -1);
            int s = StringTools.parseInt(a[2], -1);
            return ((h >= 0) && (m >= 0) && (s >= 0))? ((((h * 60) + m) * 60) + s) : dft;
        }
    }

    /** TODO:
    *** Parse a time interval from the specified string.
    *** Formats include:
    ***   123ms - 123 milliseconds,       123 milliseconds
    ***   123s  - 123 seconds,         123000 milliseconds
    ***   123h  - 123 hours,        442800000 milliseconds
    ***   123d  - 123 days,       10627200000 milliseconds
    **/

    // ------------------------------------------------------------------------

    private static final String ARG_NOW_SEC             = "-now_sec";
    private static final String ARG_NOW_MS              = "-now_ms";
    private static final String ARG_COMPILE_TIME        = "-compileTime";
    private static final String ARG_HELP                = "-help";

    private static final String ARG_TIMEZONE_NAME[]     = new String[]{ "tzname", "tmzname" };
    private static final String ARG_TIMEZONE_LIST[]     = new String[]{ "tzlist", "timezones" };
    private static final String ARG_TIMEZONE_FILE[]     = new String[]{ "tmzfile" };
    private static final String ARG_TIMEZONE_TIME[]     = new String[]{ "tmztime" };

    private static final String ARG_TO_YMD[]            = new String[]{ "toymd" };

    private static final String ARG_TIMEZONE[]          = new String[]{ "tmz", "tz" };
    private static final String ARG_TIME[]              = new String[]{ "time" };
    private static final String ARG_PLUS[]              = new String[]{ "plus" };
    private static final String ARG_FORMAT[]            = new String[]{ "format" };
    
    private static void usage(int exitCode)
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + DateTime.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -now_sec                            Current time in seconds");
        Print.sysPrintln("  -now_ms                             Current time in milliseconds");
        Print.sysPrintln("  -compileTime                        Output 'CompileTime.java' source");
        Print.sysPrintln("  -timezones                          List available TimeZones");
        Print.sysPrintln("  -tmzname=TIMEZONE                   Display Timzone info");
        Print.sysPrintln("  -tmzfile=FILE                       Display Timzone from file");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -time=DATETIME [-plus=#{d|h|m|s}]   Display Date/Time");
        Print.sysPrintln("  -tmz=TIMEZONE                       Date/Time timezone");
        Print.sysPrintln("  -format=FORMAT                      Date/Time format");
        Print.sysPrintln("  -toymd=DATETIME                     Calculate delta time");
        System.exit(exitCode);
    }

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {

        /* precheck for current epoch time */
        if (argv.length > 0) {
            if (argv[0].equals(ARG_NOW_SEC)) {
                // "-now_sec" Display current Epoch time in seconds
                System.out.println(String.valueOf(DateTime.getCurrentTimeSec()));
                System.exit(0);
            } else 
            if (argv[0].equals(ARG_NOW_MS)) {
                // "-now_ms" Display current Epoch time in milliseconds
                System.out.println(String.valueOf(DateTime.getCurrentTimeMillis()));
                System.exit(0);
            } else
            if (argv[0].startsWith(ARG_COMPILE_TIME)) {
                // "-compileTime" Create/Display "CompileTime.java" source module
                int p = argv[0].indexOf("=");
                String pkg = "";
                DateTime now = new DateTime();
                StringBuffer sb = new StringBuffer();
                if (p > 0) {
                    sb.append("package "+argv[0].substring(p+1)+";\n");
                }
                sb.append("public class CompileTime {\n");
                sb.append("    public static final long   COMPILE_TIMESTAMP = " + now.getTimeSec() + "L;\n");
                sb.append("    public static final String COMPILE_DATETIME  = \""+now.format("yyyy/MM/dd HH:mm:ss zzz")+"\";\n");
                sb.append("}\n");
                System.out.println(sb.toString());
                System.exit(0);
            } else
            if (argv[0].startsWith(ARG_HELP)) {
                // "-help" Display help
                DateTime.usage(0);
                System.exit(0);
            }
        }

        /* command-line args */
        RTConfig.setCommandLineArgs(argv);

        /* Display timezone name */
        if (RTConfig.hasProperty(ARG_TIMEZONE_NAME)) {
            // "-tmzname=TMZ" Display timezone name
            String tmzName = RTConfig.getString(ARG_TIMEZONE_NAME,"");
            TimeZone tz = TimeZone.getTimeZone(tmzName); // case sensitive
            Print.sysPrintln("Timezone: " + tz);
            System.exit(0);
        }

        /* List all available TimeZones */
        if (RTConfig.getBoolean(ARG_TIMEZONE_LIST,false)) {
            String tzid[] = ListTools.sort(TimeZone.getAvailableIDs(), null);
            for (int i = 0; i < tzid.length; i++) {
                TimeZone tz         = TimeZone.getTimeZone(tzid[i]); // case sensitive
                String id           = tz.getID();                               // ie. "US/Pacific"
                String name         = tz.getDisplayName();
                String shortName    = tz.getDisplayName(false, TimeZone.SHORT); // ie. "PST"
                String shortNameDST = tz.getDisplayName(true , TimeZone.SHORT); // ie. "PDT"
                String longName     = tz.getDisplayName(false, TimeZone.LONG);  // ie. "Pacific Standard Time"
                //String testName   = TimeZone.getTimeZone(shortName).getDisplayName(false, TimeZone.SHORT);
                Print.sysPrintln(tzid[i] + " ID=" +id + ", SHORT=" + shortName+ "/"+shortNameDST + ", LONG=" + longName); // + " [" + testName + "]");
            }
            System.exit(0);
        }

        /* Read TimeZones from file */
        File tmzFile = RTConfig.getFile(ARG_TIMEZONE_FILE, null);
        if (tmzFile != null) {
            long nowTime = DateTime.getCurrentTimeSec();
            String TMZ_NAME[] = DateTime.readTimeZones(tmzFile);
            if (!ListTools.isEmpty(TMZ_NAME)) {
                //                Africa/Abidjan               [GMT+00:00]: * , GMT   , Greenwich Mean Time
                Print.sysPrintln("TimeZone name                 GMT Offset: ID, Short , Long Name");
                for (int i = 0; i < TMZ_NAME.length; i++) {
                    String tzname    = StringTools.leftJustify(TMZ_NAME[i], 28);
                    TimeZone tz      = TimeZone.getTimeZone(TMZ_NAME[i]); // case sensitive
                  //TimeZone tz      = DateTime.getTimeZone(TMZ_NAME[i], null);
                    boolean isGMT    = (tz.getRawOffset() == 0)? true : false;
                    String idName    = tz.getID().equals(TMZ_NAME[i])? "*" : tz.getID();
                    String shortName = StringTools.leftJustify(tz.getDisplayName(false, TimeZone.SHORT), 5);
                    String longName  = tz.getDisplayName(false, TimeZone.LONG);
                    int rawOfsMIN    = tz.getOffset(nowTime) / (1000 * 60);
                    int rawOfsHH     = Math.abs(rawOfsMIN) / 60;
                    int rawOfsMM     = Math.abs(rawOfsMIN) % 60;
                    String gmtStr    = GMT_TIMEZONE+((rawOfsMIN>=0)?"+":"-")+StringTools.format(rawOfsHH,"00")+":"+StringTools.format(rawOfsMM,"00");
                    StringBuffer sb = new StringBuffer();
                    sb.append(tzname).append(" ").append(isGMT?"*":" ");
                    sb.append("[").append(gmtStr).append("]: ");
                    sb.append(idName).append(" , ").append(shortName).append(" , ").append(longName);
                    Print.sysPrintln(sb.toString());
                }
            } else {
                Print.sysPrintln("ERROR: Unable to read file - " + tmzFile);
            }
            System.exit(0);
        }

        /* TimeZone */
        String tmz = RTConfig.getString(ARG_TIMEZONE_TIME, null);
        if (!StringTools.isBlank(tmz)) {
            long nowTime = DateTime.getCurrentTimeSec();
            TimeZone tz = TimeZone.getTimeZone(tmz); // case sensitive
            int rawOfsMIN  = (tz.getOffset(nowTime) + tz.getDSTSavings()) / (1000 * 60);
            int rawOfsHH   = Math.abs(rawOfsMIN) / 60;
            int rawOfsMM   = Math.abs(rawOfsMIN) % 60;
            String gmtStr  = GMT_TIMEZONE+((rawOfsMIN>=0)?"+":"-")+StringTools.format(rawOfsHH,"00")+":"+StringTools.format(rawOfsMM,"00");
            Print.sysPrintln(tmz + " [" + gmtStr + "]: " + tz);
            Print.sysPrintln("Time : " + (new DateTime(nowTime,tz)));
            Print.sysPrintln("GMT  : " + (new DateTime(nowTime,DateTime.getGMTTimeZone())));
            System.exit(0);
        }

        /* time zones */
        String tzStr   = RTConfig.getString(ARG_TIMEZONE, null);
        TimeZone tz    = null;
        if (StringTools.isBlank(tzStr)) {
            tz = DateTime.getDefaultTimeZone();
        } else {
            tz = DateTime.getTimeZone(tzStr);
        }
        TimeZone pstTZ = DateTime.getTimeZone("US/Pacific");
        TimeZone gmtTZ = DateTime.getGMTTimeZone();

        /* date format conversion */
        if (RTConfig.hasProperty(ARG_TO_YMD)) {
            Print.sysPrintln("");
            String dateStr = RTConfig.getString(ARG_TO_YMD,null);
            int p = dateStr.indexOf(":");
            if (p <= 0) {
                Print.sysPrintln("Missing 'from' date format (ie. 'mdy:12/23/2012')");
                System.exit(99);
            }
            String fmt = dateStr.substring(0,p).trim();
            String vvv = dateStr.substring(p+1).trim();
            DateStringFormat dsf = DateTime.getDateStringFormat(fmt);
            Print.sysPrintln("Converting from " + dsf + " to YMD ==> " + dsf.convertToYMD(vvv));
            System.exit(0);
        }

        // -------------------------------------
        // Display time formats below this line:

        /* time */
        Print.sysPrintln("");
        DateTime timeDT = new DateTime(tz);
        String argTime = null;
        if (RTConfig.hasProperty(ARG_TIME)) {
            argTime = RTConfig.getString(ARG_TIME,null);
            try {
                timeDT = DateTime.parseArgumentDate(argTime, tz, DefaultParsedTime.CurrentTime);
                Print.sysPrintln("Custom Time: " + argTime);
            } catch (DateParseException dpe) {
                Print.logError("Unable to parse date: " + argTime + " [" + dpe.getMessage() + "]");
                Print.logWarn("Using current time");
            }
        } else {
            Print.sysPrintln("Current Time:");
        }

        /* plus */
        String plusStr = RTConfig.getString(ARG_PLUS, null);
        if (!StringTools.isBlank(plusStr) && StringTools.isLong(plusStr,false)) {
            long ofs = StringTools.parseLong(plusStr,0L);
            if (StringTools.endsWithIgnoreCase(plusStr,"d")) {
                // plus offset Days
                ofs *= DateTime.DaySeconds(1);
            } else
            if (StringTools.endsWithIgnoreCase(plusStr,"h")) {
                // plus offset Hours
                ofs *= DateTime.HourSeconds(1);
            } else
            if (StringTools.endsWithIgnoreCase(plusStr,"m")) {
                // plus offset Minutes
                ofs *= DateTime.MinuteSeconds(1);
            } else
            if (StringTools.endsWithIgnoreCase(plusStr,"s")) {
                // plus offset Seconds
                //ofs *= 1L;
            } else
            if (StringTools.endsWithIgnoreCase(plusStr,"ms")) {
                // plus offset Milliseconds
                ofs /= 1000L;
            } else {
                // assume offset Seconds
            }
            timeDT = new DateTime(timeDT.getTimeSec() + ofs, tz);
            Print.sysPrintln("(Plus "+ofs+" seconds)");
        }

        /* display dates */
        DateTime tmzTimeDt = timeDT;
        long     timeSec   = timeDT.getTimeSec();
        DateTime gmtTimeDt = new DateTime(timeSec,gmtTZ);
        DateTime pstTimeDt = new DateTime(timeSec,pstTZ);
        Print.sysPrintln("GMT time     : " + gmtTimeDt + " [" + timeSec + ":0x" + StringTools.toHexString(timeSec,32) + "]");
        Print.sysPrintln("TZ  time     : " + tmzTimeDt);
        Print.sysPrintln("PST time     : " + pstTimeDt);

        /* per command-line specified format */
        String fmt = RTConfig.getString(ARG_FORMAT, null);
        if (!StringTools.isBlank(fmt)) {
            Print.sysPrintln("Formatted    : " + tmzTimeDt.format(fmt));
        }

        /* display in different formats */
        if (RTConfig.isDebugMode()) {
            Print.sysPrintln("ISO-8601     : " + gmtTimeDt.format(DateTime.ISO8601_DATETIME_FORMAT));
            Print.sysPrintln("PST format 1 : " + pstTimeDt.format("yyyy/MM/dd HH:mm:ss z"));
            Print.sysPrintln("PST format 2 : " + pstTimeDt.format("yyyy/MM/dd HH:mm:ss zz"));
            Print.sysPrintln("PST format 3 : " + pstTimeDt.format("yyyy/MM/dd HH:mm:ss zzz"));
            Print.sysPrintln("PST format 4 : " + pstTimeDt.format("yyyy/MM/dd HH:mm:ss zzzz"));
            Print.sysPrintln("PST format 5 : " + pstTimeDt.format("yyyy/MM/dd HH:mm:ss zzzzzzzzzzz"));
            Print.sysPrintln("PST format 6 : " + pstTimeDt.format("yyyy/MM/dd HH:mm:ss Z"));
        }

        /* parse with different defaults */
        if (!StringTools.isBlank(argTime)) {
            try {
                DateTime tmzStrDt = DateTime.parseArgumentDate(argTime,tz,DefaultParsedTime.DayStart    );
                DateTime tmzEndDt = DateTime.parseArgumentDate(argTime,tz,DefaultParsedTime.DayEnd      );
                DateTime tmzStrTm = DateTime.parseArgumentDate(argTime,tz,DefaultParsedTime.ContextStart);
                DateTime tmzEndTm = DateTime.parseArgumentDate(argTime,tz,DefaultParsedTime.ContextEnd  );
                Print.sysPrintln("Default DayS : " + tmzStrDt + " [" + tmzStrDt.getTimeSec() + "]");
                Print.sysPrintln("Default DayE : " + tmzEndDt + " [" + tmzEndDt.getTimeSec() + "]");
                Print.sysPrintln("Default CtxS : " + tmzStrTm + " [" + tmzStrTm.getTimeSec() + "]");
                Print.sysPrintln("Default CtxE : " + tmzEndTm + " [" + tmzEndTm.getTimeSec() + "]");
            } catch (DateParseException dpe) {
                // will not occur (prevalidated above)
            }
        }

        /* day start/end */
        DateTime tmzDayStart = new DateTime(tmzTimeDt.getDayStart(),tmzTimeDt.getTimeZone());
        DateTime tmzDayEnd   = new DateTime(tmzTimeDt.getDayEnd()  ,tmzTimeDt.getTimeZone());
        Print.sysPrintln("TZ DayStart  : " + tmzDayStart + " [" + tmzDayStart.getTimeSec() + "]");
        Print.sysPrintln("TZ DayEnd    : " + tmzDayEnd   + " [" + tmzDayEnd  .getTimeSec() + "]");

        long tzDayN    = DateTime.getDayNumberFromDate(tmzTimeDt);
        long tzNowDayN = (new DateTime(tz)).getDayNumber(); // DateTime.getDayNumberFromDate(new DateTime(tz));
        long tzDOW     = ((tzDayN + 5) % 7);
        long tzMOD     = tmzTimeDt.getMinuteOfDay();
        long deltaDN   = tzDayN - tzNowDayN;
        Print.sysPrintln("TZ DayNumber : "+tzDayN+" [DOW="+tzDOW+"] DeltaDays="+deltaDN +" DeltaSec="+DateTime.DaySeconds(deltaDN));
        Print.sysPrintln("TZ MinOfDay  : "+tzMOD+" [0x" +StringTools.toHexString(tzMOD,16)+ "]");
        Print.sysPrintln("");

        /* previous time-of-day test */
        DateTime edDT  = tmzTimeDt;
        long     edTS  = edDT.getTimeSec();
        long     edDN  = DateTime.getDayNumberFromDate(edDT);
        // --
        int      frHM  = RTConfig.getInt("hhmm", 0);
        int      frMOD = (((frHM / 100) % 24) * 60) + ((frHM % 100) % 60);
        ParsedDateTime frPDT = DateTime.getDateFromDayNumber(edDN, tz);
        frPDT.setMinuteOfDay(frMOD);
        if (frPDT.getEpochTime() >= edTS) {
            frPDT.setDayNumber(edDN - 1);
        }
        Print.sysPrintln("TOD test: " + frPDT.createDateTime());

        /* second-of-day test */
        // (hh * 3600) + (mm * 60) + ss
        /*{
            int sod = RTConfig.getInt("sod", 0);
            int hh  =  sod / 3600;
            int mm  = (sod / 60) % 60;
            int ss  = (sod     ) % 60;
            Print.sysPrintln("hh="+hh +" mm="+mm +" ss="+ss +" sod="+((hh*3600)+(mm*60)+ss));
        }*/

        /* minute-of-day test */
        // (hh * 60) + mm
        /*{
            int mod = RTConfig.getInt("mod", 0);
            int hh  = mod / 60;
            int mm  = mod % 60;
            Print.sysPrintln("hh="+hh +" mm="+mm +" mod="+((hh*60)+mm));
        }*/

    }
    
}
