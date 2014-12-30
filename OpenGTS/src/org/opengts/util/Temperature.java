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
//  2013/12/26  Martin D. Flynn
//      - Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;

/**
*** Temperature container
**/

public class Temperature
{

    // ------------------------------------------------------------------------

    public  static final long    MINIMUM_TIMESTAMP      = 1L;

    // ------------------------------------------------------------------------

    public  static final double  INVALID_TEMPERATURE    = -9999.0;
    public  static final double  TEMPERATURE_LIMIT_LO   = -273.15;   // degrees C (Kelvin)
    public  static final double  TEMPERATURE_LIMIT_HI   = 1000.0;    // degrees C

    /**
    *** Returns true if the specified Celsius temperature is within a valid range
    **/
    public static boolean isValidTemperature(double C)
    {
        return ((C >= TEMPERATURE_LIMIT_LO) && (C <= TEMPERATURE_LIMIT_HI));
    }

    /**
    *** Returns true if the specified Celsius temperature is within a valid range
    **/
    public static boolean isValidTemperature(Temperature T)
    {
        if (T == null) {
            return false;
        } else {
            return T.isValidTemperature();
        }
    }

    /**
    *** Returns true if the specified Celsius temperature is within a valid range
    **/
    public static boolean isValid(Temperature T)
    {
        if (T == null) {
            return false;
        } else {
            return T.isValid();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Fahrenheit to Celsius
    *** @param F Fahrenheit temperature
    *** @return Celsius temperature
    **/
    public static double F2C(double F)
    {
        return (F - 32.0) * 5.0 / 9.0;
    }

    /**
    *** Celsius to Fahrenheit
    *** @param C Celsius temperature
    *** @return Fahrenheit temperature
    **/
    public static double C2F(double C)
    {
        return (C * 9.0 / 5.0) + 32.0;
    }

    // ------------------------------------------------------------------------

    /**
    *** Enum: Temperature Units 
    **/
    public enum TemperatureUnits implements EnumTools.StringLocale, EnumTools.IntValue {
        F  (0, I18N.getString(Temperature.class,"Temperature.f","F")),  // Fahrenheit
        C  (1, I18N.getString(Temperature.class,"Temperature.c","C"));  // Celsius (default)
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        TemperatureUnits(int v, I18N.Text a)  { vv=v; aa=a; }
        public int     getIntValue()          { return vv; }
        public String  toString()             { return aa.toString(); }
        public String  toString(Locale loc)   { return aa.toString(loc); }
        public double  convertFromC(double c) { return this.equals(F)? C2F(c) : c; }
        public double  convertToC(double c)   { return this.equals(F)? F2C(c) : c; }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long    timestamp = 0L;
    private double  tempC     = -999.0;

    /**
    *** Temperature constructor
    *** @param ts The timestamp (in seconds)
    *** @param C  The temperature (in Celsius)
    **/
    public Temperature(long ts, double C) 
    {
        this.timestamp = ts;
        this.tempC = C;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the temperature in Celsius units
    **/
    public double getTemperatureC() 
    {
        return this.tempC;
    }

    /**
    *** Gets the temperature in Fahrenheit units
    **/
    public double getTemperatureF() 
    {
        return C2F(this.tempC);
    }

    /**
    *** Gets the temperature 
    *** @param F  true for Fahrenheit, false for Celsius
    *** @return The temperature
    **/
    public double getTemperature(boolean F) 
    {
        return F? this.getTemperatureF() : this.getTemperatureC();
    }

    /**
    *** Returns true if the temperature is valid
    **/
    public boolean isValidTemperature()
    {
        return Temperature.isValidTemperature(this.getTemperatureC());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the timestamp (in seconds)
    **/
    public long getTimestamp() 
    {
        return this.timestamp;
    }

    /**
    *** Gets the timestamp (in milliseconds)
    **/
    public long getTimestampMillis() 
    {
        return this.timestamp * 1000L;
    }

    /**
    *** Returns true if the timestamp is valid
    **/
    public boolean isValidTimestamp()
    {
        long ts = this.getTimestamp();
        return (ts >= MINIMUM_TIMESTAMP);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the timestamp and temperature are valid
    **/
    public boolean isValid()
    {
        return (this.isValidTimestamp() && this.isValidTemperature());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the String representation of this instance
    **/
    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getTimestamp());
        sb.append(",");
        sb.append(this.getTemperatureC());
        return sb.toString();
    }

}
