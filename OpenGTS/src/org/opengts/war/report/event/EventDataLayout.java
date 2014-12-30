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
//  Report definition based on EventData table
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/01/10  Martin D. Flynn
//     -Added fields 'checkinDateTime', 'checkinAge'
//  2008/02/04  Martin D. Flynn
//     -Added fields 'engineRpm', 'fuelUsed'
//  2008/03/12  Martin D. Flynn
//     -Added additional decimal point options to various fields
//  2008/04/11  Martin D. Flynn
//     -Added color indicator to lat/lon when gps age is over a given threshold
//     -Added field 'gpsAge'
//  2008/05/14  Martin D. Flynn
//     -Added City/State/Country/Subdivision fields
//  2008/10/16  Martin D. Flynn
//     -Added battery level field.
//     -Added input mask field.
//  2009/01/01  Martin D. Flynn
//     -Added arguments to "heading" to allow displaying in degrees
//  2010/09/09  Martin D. Flynn
//     -Added "ambientTemp", "barometer", "deviceBattery"
//  2011/03/08  Martin D. Flynn
//     -Added "driverStatus"
//  2011/06/16  Martin D. Flynn
//     -Added status code/description coloring option
//     -Added "mapLink" feature to "index" column to support displaying the map
//      with a specific pushpin info-balloon displayed.
//  2011/07/15  Martin D. Flynn
//     -Added "analog#" fields.  Fixed "batteryVolts" column to use "getBatteryVolts()".
//  2011/08/21  Martin D. Flynn
//     -Added "tirePressure", "tireTemp"
//  2011/10/03  Martin D. Flynn
//     -Added "turboPressure", "day*"
//  2012/02/03  Martin D. Flynn
//     -Added "fuelRate"
//  2012/04/03  Martin D. Flynn
//     -Added "formatElapsedTime"
//     -Added DATA_FAULT_CODES to display multiple DTC codes
//  2012/08/01  Martin D. Flynn
//     -Added DATA_REPORT_DISTANCE
//  2012/09/02  Martin D. Flynn
//     -Added DATA_CREATE_AGE, DATA_CREATE_MILLIS
//     -Modified DATA_STATUS_CODE to support displaying a decimal status code.
//     -Added support for decimal status code display (see DATA_STATUS_CODE)
//     -Changed Double value formatting to using "EventDataLayout.formatDouble"
//      instead of "StringTools.format".
//     -Added DATA_GPSFIX_TYPE and DATA_GPSFIX_STATUS
//  2012/12/24  Martin D. Flynn
//     -Added proper sort ordering for temperature fields
//  2013/04/08  Martin D. Flynn
//     -Added DATA_DEVICE_VIN
//     -Change massAirFlow units to grams/sec.
//  2014/05/05  Martin D. Flynn
//     -Added DATA_BATTERY_TEMP, DATA_ATTACHMENT_PROP
//  2014/06/29  Martin D. Flynn
//     -Added DATA_WORK_HOURS, DATA_WORK_DISTANCE, DATA_PTO_DISTANCE
// ----------------------------------------------------------------------------
package org.opengts.war.report.event;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.RequestProperties;
import org.opengts.war.tools.PushpinIcon;
import org.opengts.war.tools.WebPageAdaptor;
import org.opengts.war.tools.ComboOption;
import org.opengts.war.tools.MapDimension;

import org.opengts.war.report.*;

public class EventDataLayout
    extends ReportLayout
{

    // ------------------------------------------------------------------------

    // TODO: these colors/thresholds should be configurable at runtime
    //private static final long   GPS_AGE_THRESHOLD_1     = DateTime.MinuteSeconds(60);
    //private static final String GPS_AGE_COLOR_1         = "#BB0000";  // lighter red
    //private static final long   GPS_AGE_THRESHOLD_2     = DateTime.MinuteSeconds(20);
    //private static final String GPS_AGE_COLOR_2         = "#550000";  // darker red

    // ------------------------------------------------------------------------

    private static final long  MINIMUM_REASONABLE_TIMESTAMP = (new DateTime(null,2000,1,1)).getTimeSec();

    // ------------------------------------------------------------------------
    // Data keys
    // - These define what data is available (see 'EventDataRow') and what columns will be 
    //   displayed in the table.
    // - Column names must only contain <alpha>/<numeric>/'_' characters
    
    public static final String  DATA_INDEX              = "index";
    
    public static final String  DATA_ATTACHMENT_URL     = "attachURL";
    public static final String  DATA_ATTACHMENT_PROP    = "attachProp";             // ev.getAttachData()

    public static final String  DATA_DEVICE_ID          = "deviceId";
    public static final String  DATA_DEVICE_DESC        = "deviceDesc";             // Device record
    public static final String  DATA_DEVICE_BATTERY     = "deviceBattery";          // Device record
    public static final String  DATA_DEVICE_VIN         = "deviceVehicleID";        // Device record
    public static final String  DATA_DEVICE_CREATION    = "deviceCreateDateTime";   // Device record

    public static final String  DATA_DATE               = "date";
    public static final String  DATA_TIME               = "time";
    public static final String  DATA_DATETIME           = "dateTime";
    public static final String  DATA_TIMESTAMP          = "timestamp";

    public static final String  DATA_GROUP_ID           = "groupId";
    public static final String  DATA_STATUS_CODE        = "statusCode";
    public static final String  DATA_STATUS_DESC        = "statusDesc";
    public static final String  DATA_PUSHPIN            = "pushpin";
    public static final String  DATA_GPS_AGE            = "gpsAge";
    public static final String  DATA_LATITUDE           = "latitude";
    public static final String  DATA_LONGITUDE          = "longitude";
    public static final String  DATA_GEOPOINT           = "geoPoint";
    public static final String  DATA_GPSFIX_TYPE        = "gpsFixType";
    public static final String  DATA_GPSFIX_STATUS      = "gpsFixStatus";
    public static final String  DATA_ACCURACY           = "accuracy";
    public static final String  DATA_ALTITUDE           = "altitude";
    public static final String  DATA_SATELLITES         = "satellites";
    public static final String  DATA_SPEED_LIMIT        = "speedLimit";
    public static final String  DATA_SPEED              = "speed";
    public static final String  DATA_SPEED_HEADING      = "speedH";
    public static final String  DATA_SPEED_UNITS        = "speedU";
    public static final String  DATA_HEADING            = "heading";
    public static final String  DATA_DISTANCE           = "distance";
    public static final String  DATA_ODOMETER           = "odometer";
    public static final String  DATA_REPORT_DISTANCE    = "reportDistance";
  //public static final String  DATA_STOP_DATETIME      = "stopDateTime";
  //public static final String  DATA_STOP_ELAPSED       = "stopElapsed";

    public static final String  DATA_CELL_LATITUDE      = "cellLatitude";       
    public static final String  DATA_CELL_LONGITUDE     = "cellLongitude";      
    public static final String  DATA_CELL_GEOPOINT      = "cellGeoPoint";       
    public static final String  DATA_CELL_ACCURACY      = "cellAccuracy";
    
    public static final String  DATA_BEST_LATITUDE      = "bestLatitude";       
    public static final String  DATA_BEST_LONGITUDE     = "bestLongitude";      
    public static final String  DATA_BEST_GEOPOINT      = "bestGeoPoint";       
    public static final String  DATA_BEST_ACCURACY      = "bestAccuracy";

    public static final String  DATA_BATTERY            = "battery";
    public static final String  DATA_BATTERY_PERCENT    = "batteryPercent";
    public static final String  DATA_BATTERY_VOLTS      = "batteryVolts";
    public static final String  DATA_BATTERY_TEMP       = "batteryTemp";

    public static final String  DATA_INPUT_STATE        = "inputState";
    public static final String  DATA_INPUT_BIT          = "inputBit";
    public static final String  DATA_OUTPUT_STATE       = "outputState";
    public static final String  DATA_OUTPUT_BIT         = "outputBit";
    public static final String  DATA_SEATBELT_STATE     = "seatbeltState";
    public static final String  DATA_SEATBELT_BIT       = "seatbeltBit";

    public static final String  DATA_ANALOG_0           = "analog0";
    public static final String  DATA_ANALOG_1           = "analog1";
    public static final String  DATA_ANALOG_2           = "analog2";
    public static final String  DATA_ANALOG_3           = "analog3";
    public static final String  DATA_PULSE_COUNT        = "pulseCount";

    public static final String  DATA_ADDRESS            = "address";
    public static final String  DATA_CITY               = "city";
    public static final String  DATA_STATE              = "state";
    public static final String  DATA_COUNTRY            = "country";
    public static final String  DATA_SUBDIVISION        = "subdivision";
    
    public static final String  DATA_GEOZONE_ID         = "geozoneId";
    public static final String  DATA_GEOZONE_DESC       = "geozoneDesc";

    public static final String  DATA_ENTITY_ID          = "entityId";
    public static final String  DATA_ENTITY_DESC        = "entityDesc";
    
    public static final String  DATA_DRIVER_ID          = "driverId";
    public static final String  DATA_DRIVER_DESC        = "driverDesc";
    public static final String  DATA_DRIVER_STATUS      = "driverStatus";
    public static final String  DATA_DRIVER_MESSAGE     = "driverMessage";
    
    public static final String  DATA_JOB_NUMBER         = "jobNumber";
    
    public static final String  DATA_RFID_TAG           = "rfidTag";
    
    public static final String  DATA_SAMPLE_INDEX       = "sampleIndex";
    public static final String  DATA_SAMPLE_ID          = "sampleId";
  //public static final String  DATA_APPLIED_PRESSURE   = "appliedPressure";

    public static final String  DATA_ETA_DATETIME       = "etaDateTime";
    public static final String  DATA_ETA_TIMESTAMP      = "etaTimestamp";
    public static final String  DATA_ETA_UNIQUE_ID      = "etaUniqueID";            // ETA ID
    public static final String  DATA_ETA_DISTANCE       = "etaDistance";            // ETA distance
    public static final String  DATA_ETA_LATITUDE       = "etaLatitude";            // ETA latitude
    public static final String  DATA_ETA_LONGITUDE      = "etaLongitude";           // ETA longitude
    public static final String  DATA_ETA_GEOPOINT       = "etaGeoPoint";            // ETA lat/lon
    
    public static final String  DATA_STOP_UNIQUE_ID     = "stopUniqueID";           // STOP ID
    public static final String  DATA_STOP_STATUS        = "stopStatus";             // STOP Status
    public static final String  DATA_STOP_STATUS_DESC   = "stopStatusDesc";         // STOP Status Description
    public static final String  DATA_STOP_INDEX         = "stopIndex";              // STOP Index

    public static final String  DATA_BAROMETER          = "barometer";
    public static final String  DATA_AMBIENT_TEMP       = "ambientTemp";

    public static final String  DATA_THERMO_1           = "thermo1";
    public static final String  DATA_THERMO_2           = "thermo2";
    public static final String  DATA_THERMO_3           = "thermo3";
    public static final String  DATA_THERMO_4           = "thermo4";
    public static final String  DATA_THERMO_5           = "thermo5";
    public static final String  DATA_THERMO_6           = "thermo6";
    public static final String  DATA_THERMO_7           = "thermo7";
    public static final String  DATA_THERMO_8           = "thermo8";

    public static final String  DATA_FUEL_LEVEL         = "fuelLevel";
    public static final String  DATA_FUEL_LEVEL_VOL     = "fuelLevelVolume";
    public static final String  DATA_FUEL_ECONOMY       = "fuelEconomy";
    public static final String  DATA_FUEL_ECONOMY_TYPE  = "fuelEconomyType";
    public static final String  DATA_FUEL_TOTAL         = "fuelTotal";
    public static final String  DATA_FUEL_REMAIN        = "fuelRemain";
    public static final String  DATA_FUEL_TRIP          = "fuelTrip";
    public static final String  DATA_FUEL_IDLE          = "fuelIdle";
    public static final String  DATA_FUEL_ENGINE_ON     = "fuelEngineOn";
    public static final String  DATA_FUEL_PRESSURE      = "fuelPressure";
    public static final String  DATA_FUEL_RATE          = "fuelRate";
    public static final String  DATA_FUEL_LEVEL_2       = "fuelLevel2";
    public static final String  DATA_FUEL_LEVEL_VOL_2   = "fuelLevelVolume2";

    public static final String  DATA_FAULT_CODES        = "faultCodes";
    public static final String  DATA_FAULT_CODE         = "faultCode";
    public static final String  DATA_MALFUNCTION_LAMP   = "malfunctionLamp";
    public static final String  DATA_OIL_LEVEL          = "oilLevel";
    public static final String  DATA_OIL_PRESSURE       = "oilPressure";
    public static final String  DATA_OIL_TEMP           = "oilTemp";
    public static final String  DATA_ENGINE_RPM         = "engineRpm";
    public static final String  DATA_ENGINE_HOURS       = "engineHours";
    public static final String  DATA_ENGINE_ON_HOURS    = "engineOnHours";
    public static final String  DATA_ENGINE_LOAD        = "engineLoad";             // %
    public static final String  DATA_IDLE_HOURS         = "idleHours";              // hours
    public static final String  DATA_TRANS_OIL_TEMP     = "transOilTemp";           // C
    public static final String  DATA_COOLANT_LEVEL      = "coolantLevel";           // %
    public static final String  DATA_COOLANT_TEMP       = "coolantTemp";            // C
    public static final String  DATA_BRAKE_G_FORCE      = "brakeGForce";            // G
    public static final String  DATA_BRAKE_FORCE        = "brakeForce";
    public static final String  DATA_BRAKE_PRESSURE     = "brakePressure";          // kPa
    public static final String  DATA_BRAKE_POSITION     = "brakePos";               // %
    public static final String  DATA_ACCELERATION       = "acceleration";           // 
    public static final String  DATA_PTO_ENGAGED        = "ptoEngaged";
    public static final String  DATA_PTO_HOURS          = "ptoHours";
    public static final String  DATA_PTO_DISTANCE       = "ptoDistance";
    public static final String  DATA_WORK_HOURS         = "workHours";
    public static final String  DATA_WORK_DISTANCE      = "workDistance";
    public static final String  DATA_VEH_BATTERY_VOLTS  = "vBatteryVolts";
    public static final String  DATA_THROTTLE_POSITION  = "throttlePos";            // %
    public static final String  DATA_INTAKE_TEMP        = "intakeTemp";             // C
    public static final String  DATA_AIR_PRESSURE       = "airPressure";            // kPa
    public static final String  DATA_AIR_FILTER_PRESS   = "airFilterPressure";      // kPa
    public static final String  DATA_MASS_AIR_FLOW      = "massAirFlow";            // g/sec
    public static final String  DATA_TURBO_PRESS        = "turboPressure";          // kPa
    public static final String  DATA_TIRE_PRESSURE      = "tirePressure";           // kPa
    public static final String  DATA_TIRE_TEMPERATURE   = "tireTemp";               // C
    public static final String  DATA_TANK_LEVEL         = "tankLevel";              // %

    public static final String  DATA_EVENT_FIELD        = "eventField";

    public static final String  DATA_CHECKIN_DATETIME   = "checkinDateTime";        // Device record
    public static final String  DATA_CHECKIN_AGE        = "checkinAge";             // Device record
    public static final String  DATA_CUSTOM_FIELD       = "customField";            // Device record
    public static final String  DATA_LAST_BATTERY_PCT   = "lastBatteryPercent";     // Device record
    public static final String  DATA_LAST_FAULT_CODES   = "lastFaultCodes";         // Device record
    public static final String  DATA_FUEL_CAPACITY      = "fuelCapacity";           // Device record

    public static final String  DATA_DATA_SOURCE        = "dataSource";
    public static final String  DATA_RAW_DATA           = "rawData";

    public static final String  DATA_CREATE_DATE        = "createDate";
    public static final String  DATA_CREATE_TIME        = "createTime";
    public static final String  DATA_CREATE_DATETIME    = "createDateTime";
    public static final String  DATA_CREATE_TIMESTAMP   = "createTimestamp";
    public static final String  DATA_CREATE_AGE         = "createAge";
    public static final String  DATA_CREATE_MILLIS      = "createMillis";

    public static final String  DATA_TRIP_START_DATETIME= "tripStartDateTime";
    public static final String  DATA_TRIP_STOP_DATETIME = "tripStopDateTime";
    public static final String  DATA_TRIP_DISTANCE      = "tripDistance";
    public static final String  DATA_TRIP_IDLE_HOURS    = "tripIdleHours";
    public static final String  DATA_TRIP_MAX_SPEED     = "tripMaxSpeed";
    public static final String  DATA_TRIP_MAX_RPM       = "tripMaxRPM";
    public static final String  DATA_TRIP_START_LAT     = "tripStartLatitude";
    public static final String  DATA_TRIP_START_LON     = "tripStartLongitude";
    public static final String  DATA_TRIP_ELAPSED       = "tripElapsed";

    public static final String  DATA_DAY_ENGINE_STARTS  = "dayEngineStarts";
    public static final String  DATA_DAY_IDLE_HOURS     = "dayIdleHours";
    public static final String  DATA_DAY_FUEL_IDLE      = "dayFuelIdle";
    public static final String  DATA_DAY_WORK_HOURS     = "dayWorkHours";
    public static final String  DATA_DAY_FUEL_WORK      = "dayFuelWork";
    public static final String  DATA_DAY_FUEL_PTO       = "dayFuelPTO";
    public static final String  DATA_DAY_FUEL_TOTAL     = "dayFuelTotal";
    public static final String  DATA_DAY_DISTANCE       = "dayDistance";

    // ------------------------------------------------------------------------
    // EventDataLayout is a singleton

    private static EventDataLayout reportDef = null;

    /**
    *** Gets the EventDataLayout singleton instance
    *** @return The EventDataLayout singleton instance
    **/
    public static ReportLayout getReportLayout()
    {
        if (reportDef == null) {
            reportDef = new EventDataLayout();
        }
        return reportDef;
    }

    /**
    *** Standard singleton constructor
    **/
    private EventDataLayout()
    {
        super();
        this.setDataRowTemplate(new EventDataRow());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* format double value */
    protected static String formatDouble(double value, String arg, String dftFmt)
    {
        String fmt = dftFmt;
        if (StringTools.isBlank(arg)) {
            // default format
            fmt = dftFmt;
        } else
        if (arg.startsWith("#")) {
            // explicit format (ie. "#0.0000")
            fmt = arg.substring(1);
        } else {
            // format alias, "arg" represents number of decimal points
            switch (arg.charAt(0)) {
                case '0': fmt = "0"          ; break;
                case '1': fmt = "0.0"        ; break;
                case '2': fmt = "0.00"       ; break;
                case '3': fmt = "0.000"      ; break;
                case '4': fmt = "0.0000"     ; break;
                case '5': fmt = "0.00000"    ; break;
                case '6': fmt = "0.000000"   ; break;
                case '7': fmt = "0.0000000"  ; break;
                case '8': fmt = "0.00000000" ; break;
                case '9': fmt = "0.000000000"; break;
                default : fmt = dftFmt       ; break;
            }
        }
        return StringTools.format(value, fmt);
    }
    
    // ------------------------------------------------------------------------

    /* format temperatures */
    protected static String formatTemperature(double thermoC, String arg, ReportData rd, String dft)
    {
        if (EventData.isValidTemperature(thermoC)) {
            Account a       = rd.getAccount();
            double thermo   = Account.getTemperatureUnits(a).convertFromC(thermoC);
            String unitAbbr = Account.getTemperatureUnits(a).toString(rd.getLocale());
            return EventDataLayout.formatDouble(thermo, arg, "0.0") + unitAbbr;
        } else
        if (dft != null) {
            return dft;
        } else {
            I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
            String na = i18n.getString("EventDataLayout.notAvailable","n/a");
            return "--";
        }
    }

    /* format kilometer distance */
    protected static String formatKM(double km, String arg, ReportData rd)
    {
        if (km > 0.0) {
            double dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(km);
            return EventDataLayout.formatDouble(dist, arg, "0");
        } else {
            return "";
        }
    }

    protected static String formatSpeed(double kph, String arg, ReportData rd)
    {
        double speed = Account.getSpeedUnits(rd.getAccount()).convertFromKPH(kph);
        return EventDataLayout.formatDouble(speed, arg, "0");
    }

    protected static String formatEconomy(double kpl, String arg, ReportData rd)
    {
        double econ = Account.getEconomyUnits(rd.getAccount()).convertFromKPL(kpl);
        return EventDataLayout.formatDouble(econ, arg, "0");
    }

    protected static String formatVolume(double L, String arg, ReportData rd)
    {
        double vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(L);
        return EventDataLayout.formatDouble(vol, arg, "0");
    }

    protected static String formatPressure(double kPa, String arg, ReportData rd)
    {
        double press = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
        return EventDataLayout.formatDouble(press, arg, "0.0");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static class EventDataRow
        extends DataRowTemplate
    {
        public EventDataRow() {
            super();

            // Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    if (rowNdx >= 0) {
                        String arg = rc.getArg();
                        if (StringTools.isBlank(arg)) {
                            return String.valueOf(rowNdx + 1);
                        } else
                        if (arg.equalsIgnoreCase("map")    ||
                            arg.equalsIgnoreCase("mapLink")  ) {
                            String ndxVal = String.valueOf(rowNdx + 1);
                            URIArg mapURL = rd.getMapURL();
                            if (mapURL != null) {
                                mapURL.addArg("showpp",rowNdx+1);
                                mapURL.addArg("zoompp",rowNdx+1);
                                RequestProperties reqState = rd.getRequestProperties();
                                MapDimension sz = rd.getMapWindowSize();
                                int W = sz.getWidth();
                                int H = sz.getHeight();
                                String encMapURL = WebPageAdaptor.EncodeURL(reqState,mapURL);
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(" "+ndxVal+" ");
                                cv.setLinkURL("javascript:openResizableWindow('"+encMapURL+"','ReportMap',"+W+","+H+");",null/*target*/);
                                return cv;
                            }
                            return ndxVal;
                        } else
                        if (arg.charAt(0) == '0') {
                            return String.valueOf(rowNdx);
                        } else {
                            return String.valueOf(rowNdx + 1);
                        }
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "#";
                }
            });

            // Attachment URL (pictures/images, etc)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ATTACHMENT_URL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    RequestProperties reqState = rd.getRequestProperties();
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    if (ed.hasAttachData()) {
                        PixelDimension dim = HTMLTools.getImageDimension(ed.getAttachData());
                        Print.logInfo("Image Dim: " + dim);
                        String extn   = HTMLTools.getExtensionFromMimeType(ed.getAttachType(), "txt");
                        String dftURI = "." + "/Attach"/*Constants.DEFAULT_ATTACH_URI*/ + "." + extn;
                        URIArg attURL = new URIArg(dftURI, true); // EncodeURL
                        attURL.addArg("d" , ed.getDeviceID());
                        attURL.addArg("ts", ed.getTimestamp());
                        attURL.addArg("sc", "0x" + StringTools.toHexString(ed.getStatusCode(),16));
                        int W = (dim != null)? dim.getWidth()  : 600;
                        int H = (dim != null)? dim.getHeight() : 400;
                        if ((W > 1024) || (H > 1024)) {
                            if ((W > 1024) && (W > H)) {
                                double scale = (double)H/(double)W;
                                W = 1024;
                                H = (int)Math.round((double)W * scale);
                                if (H <= 0) { H = 1; }
                            } else {
                                double scale = (double)W/(double)H;
                                H = 1024;
                                W = (int)Math.round((double)H * scale);
                                if (W <= 0) { W = 1; }
                            }
                        }
                        String encURL = WebPageAdaptor.EncodeURL(reqState,attURL);
                        ColumnValue cv = new ColumnValue();
                        cv.setValue(" "+extn+" ");
                        cv.setLinkURL("javascript:openResizableWindow('"+encURL+"','Attachment',"+W+","+H+");",null/*target*/);
                        return cv;
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.attachment","Attachment");
                }
            });

            // Attachment properties
            this.addColumnTemplate(new DataColumnTemplate(DATA_ATTACHMENT_PROP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = StringTools.trim(rc.getArg()); // "varName:fmt"
                    int p = arg.indexOf(":");
                    String var = (p >= 0)? arg.substring(0,p).trim() : arg;
                    String fmt = (p >= 0)? arg.substring(p+1).trim().toUpperCase() : "";
                    if (!StringTools.isBlank(var)) {
                        Account acct = rd.getAccount();
                        EventData ed = (EventData)obj;
                        RTProperties rtp = ed.getAttachRTProperties();
                        if (rtp != null) {
                            // Supported data types:
                            //  $ = String
                            //  N = Generic Number (double/long)
                            //  K = Kilometers (km)
                            //  S = Speed (km/h)
                            //  E = Fuel Economy (km/L)
                            //  T = Temperature (C)
                            //  V = Volume (Liters)
                            //  P = Pressure (kPa)
                            //  L = Latitude/Longitude (GeoPoint)
                            //  A = Altitude (meters)
                            //  H - Hours/seconds (hh:mm:ss)
                            // Unsupported data types:
                            //  M = Mass
                            //  F = Force
                            //  R = Area
                            //  C = Currency
                            if (StringTools.isBlank(fmt) || fmt.startsWith("$")) {
                                // -- String
                                return rtp.getString(var,"");
                            } else
                            if (fmt.startsWith("N")) {
                                // -- Generic number (Long/Double)
                                String dec = fmt.substring(1);
                                if (StringTools.isBlank(dec)) {
                                    // -- assume Integer/Long
                                    long val = rtp.getLong(var,0L); // parse as Long
                                    return String.valueOf(val);   // back to a String
                                } else {
                                    // -- assume Float/Double
                                    double val = rtp.getDouble(var,0.0); // parse as Double
                                    return EventDataLayout.formatDouble(val, dec, "0");
                                }
                            } else 
                            if (fmt.startsWith("K")) {
                                // -- Kilometers
                                String dec = fmt.substring(1);
                                double km  = rtp.getDouble(var,0.0); // parse as Double
                                // -- valid km (unlikely, but allow negative)
                                return EventDataLayout.formatKM(km, dec, rd);
                            } else 
                            if (fmt.startsWith("S")) {
                                // -- SpeedKPH
                                String dec = fmt.substring(1);
                                double kph = rtp.getDouble(var,0.0); // parse as Double
                                // -- valid km/h (unlikely, but allow negative)
                                return EventDataLayout.formatSpeed(kph, dec, rd);
                            } else 
                            if (fmt.startsWith("E")) {
                                // -- Economy
                                String dec = fmt.substring(1);
                                double kpl = rtp.getDouble(var,0.0); // parse as Double
                                // -- valid economy (unlikely, but allow negative)
                                return EventDataLayout.formatEconomy(kpl, dec, rd);
                            } else 
                            if (fmt.startsWith("T")) {
                                // -- Temperature
                                String dec = fmt.substring(1);
                                double C   = rtp.getDouble(var,0.0); // parse as Double
                                if (EventData.isValidTemperature(C)) {
                                    // -- valid temperature
                                    String tempS = EventDataLayout.formatTemperature(C, dec, rd, null);
                                    return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                                } else {
                                    // -- invalid temperature
                                    return rc.getBlankFiller();
                                }
                            } else 
                            if (fmt.startsWith("V")) {
                                // -- Volume
                                String dec = fmt.substring(1);
                                double L   = rtp.getDouble(var,0.0); // parse as Double
                                // -- valid volume (unlikely, but allow negative)
                                return EventDataLayout.formatVolume(L, dec, rd);
                            } else 
                            if (fmt.startsWith("P")) {
                                // -- Pressure
                                String dec = fmt.substring(1);
                                double kPa = rtp.getDouble(var,0.0); // parse as Double
                                // -- valid pressure (can be negative)
                                return EventDataLayout.formatPressure(kPa, dec, rd);
                            } else 
                            if (fmt.startsWith("L")) {
                                // -- GeoPoint (lat/lon)
                                String   dec = fmt.substring(1);
                                GeoPoint gp  = new GeoPoint(rtp.getString(var,""));
                                if (gp.isValid()) {
                                    // -- valid GeoPoint
                                    return gp.toString(dec, '/', rd.getLocale());
                                } else {
                                    // -- invalid GeoPoint
                                    return rc.getBlankFiller();
                                }
                            } else 
                            if (fmt.startsWith("A")) {
                                // -- Altitude (meters)
                                String dec = fmt.substring(1);
                                double M   = rtp.getDouble(var,0.0); // parse as Double
                                Account.AltitudeUnits altUnits = Account.getAltitudeUnits(acct);
                                double alt = altUnits.convertFromMeters(M);
                                return EventDataLayout.formatDouble(alt, dec, "0");
                            } else
                            if (fmt.startsWith("H")) {
                                // -- Hours/Seconds (hh:mm:ss)
                                String dec = fmt.substring(1);
                                int ef = EventDataLayout.getElapsedFormat(dec, StringTools.ELAPSED_FORMAT_HHMMSS);
                                String valS = rtp.getString(var,"");
                                long sec = (valS.indexOf(".") < 0)? 
                                    StringTools.parseLong(valS,0L) :              // assume seconds
                                    Math.round(StringTools.parseDouble(valS,0.0) * 3600.0); // assume hours
                                return new ColumnValue(EventDataLayout.formatElapsedTime(sec,ef)).setSortKey(sec);
                            } else
                            if (fmt.startsWith("M") ||   // Mass
                                fmt.startsWith("F") ||   // Force
                                fmt.startsWith("R") ||   // Area
                                fmt.startsWith("C")   ) {// Currency
                                // -- Generic Double
                                String dec = fmt.substring(1);
                                double val = rtp.getDouble(var,0.0); // parse as Double
                                return EventDataLayout.formatDouble(val, dec, "0");
                            } else {
                                // -- Unknown, assume String
                                return rtp.getString(var,"");
                            }
                        } else {
                            Print.logWarn("Not an RTProperties instance");
                        }
                    }
                    return "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg = StringTools.trim(rc.getArg()); // "varName:fmt"
                    int p = arg.indexOf(":");
                    String var = (p >= 0)? arg.substring(0,p).trim() : arg;
                    String fmt = (p >= 0)? arg.substring(p+1).trim() : "";
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    if (!StringTools.isBlank(var)) {
                        return var;
                    } else {
                        return i18n.getString("EventDataLayout.attachmentValue","Value");
                    }
                }
            });

            // Device-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    String deviceID = ed.getDeviceID();
                    if (arg.startsWith("admin")) { // "admin", "adminView", "adminEdit"
                        // wrap in link to Device Admin page
                        //Print.logInfo("Found admin arg: '" + arg + "'");
                        ColumnValue cv = new ColumnValue(deviceID);
                        RequestProperties reqState = rd.getRequestProperties();
                        URIArg devAdminURL = WebPageAdaptor.MakeURL(reqState.getBaseURI(),"dev.info"); // Constants.PAGE_DEVICE_INFO);
                        devAdminURL.addArg("device"   , deviceID);    // Constants.PARM_DEVICE, devID);
                        devAdminURL.addArg("page_cmd" , "selectDev"); // CommonServlet.PAGE_COMMAND, DeviceInfo.COMMAND_INFO_SEL_DEVICE);
                        if (arg.equalsIgnoreCase("adminEdit")) {
                            devAdminURL.addArg("d_subedit", "edit");      // DeviceInfo.PARM_SUBMIT_EDIT, "edit");
                        } else {
                            devAdminURL.addArg("d_subview", "view");      // DeviceInfo.PARM_SUBMIT_VIEW, "view");
                        }
                        cv.setLinkURL(devAdminURL.toString(),null/*target*/);
                        return cv;
                    } else {
                        return deviceID;
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceID","Device-ID");
                }
            });

            // Device Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        return dev.getDescription();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceDescription","Device\nDescription");
                }
            });

            // Device Battery-Level
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_BATTERY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        double level = dev.getLastBatteryLevel();
                        if (level <= 0.0) {
                            // -- not specified
                            return rc.getBlankFiller();
                        } else
                        if (level <= 1.0) {
                            // -- percent 0..1
                            return Math.round(level*100.0) + "%";           // percent
                        } else {
                            // -- >1.0  Assume volts? or integer percent?
                            boolean assumeVolts = false;
                            if (assumeVolts) {
                                return EventDataLayout.formatDouble(level, arg, "0.0") + "v";   // volts
                            } else {
                                return Math.round(level) + "%";       // percent
                            }
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceBattery","Last\nBattery");
                }
            });

            // Device VehicleID
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_VIN) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        return dev.getVehicleID();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceVehicleID","Vehicle ID");
                }
            });

            // Device creation date/time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_CREATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    long ts = dev.getCreationTime();
                    if (ts > MINIMUM_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                        return cv;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.deviceCreationTime","Device Create\nTime");
                }
            });

            // (Preferred) Group-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GROUP_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    if (dev != null) {
                        String gid = dev.getGroupID();
                        return gid;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.groupID","Group-ID");
                }
            });

            // Event timestamp Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.date","Date");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        return dt.format(rl.getTimeFormat(rd.getPrivateLabel()), tz);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.time","Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dateTime","Date/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTimestamp();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.timestamp","Timestamp") + "\n(Epoch)";
                }
            });

            // Event creation Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()), tz);
                        long ca = ed.getCreationAge(); // (creationTime - timestamp)
                        ReportLayout.AgeColorRange acr = rd.getCreationAgeColorRange(ca);
                        ColumnValue ccv = (new ColumnValue(dtFmt)).setSortKey(ts);
                        return ReportLayout.SetColumnValueAgeColor(ccv,acr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createDate","Insert\nDate");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getTimeFormat(rd.getPrivateLabel()), tz);
                        long ca = ed.getCreationAge(); // (creationTime - timestamp)
                        ReportLayout.AgeColorRange acr = rd.getCreationAgeColorRange(ca);
                        ColumnValue ccv = (new ColumnValue(dtFmt)).setSortKey(ts);
                        return ReportLayout.SetColumnValueAgeColor(ccv,acr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createTime","insert\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        long ca = ed.getCreationAge(); // (creationTime - timestamp)
                        ReportLayout.AgeColorRange acr = rd.getCreationAgeColorRange(ca);
                        ColumnValue ccv = (new ColumnValue(dtFmt)).setSortKey(ts);
                        return ReportLayout.SetColumnValueAgeColor(ccv,acr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createDateTime","Insert\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationTime();
                    long ca = ed.getCreationAge(); // (creationTime - timestamp)
                    ReportLayout.AgeColorRange acr = rd.getCreationAgeColorRange(ca);
                    ColumnValue ccv = (new ColumnValue(ts)).setSortKey(ts);
                    return ReportLayout.SetColumnValueAgeColor(ccv,acr);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createTimestamp","Insert\nTimestamp") + "\n(Epoch)";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long      ca = ed.getCreationAge(); // (creationTime - timestamp)
                    int      fmt = EventDataLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                    String caFmt = EventDataLayout.formatElapsedTime(ca,fmt);
                    ReportLayout.AgeColorRange acr = rd.getCreationAgeColorRange(ca);
                    ColumnValue ccv = (new ColumnValue(caFmt)).setSortKey(ca);
                    return ReportLayout.SetColumnValueAgeColor(ccv,acr);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createAge","Creation Age");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_MILLIS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getCreationMillis();
                    if (StringTools.isBlank(arg)) {
                        // default as-is
                    } else
                    if (arg.equalsIgnoreCase("frac") || arg.equalsIgnoreCase("fraction")) {
                        ts = ts % 1000L; // milliseconds only
                    } else 
                    if (arg.equalsIgnoreCase("sec")  || arg.equalsIgnoreCase("seconds")) {
                        ts = ts / 1000L; // seconds only
                    }
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.createMillis","Creation\nMillisecond");
                }
            });

            // Status Code/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_CODE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = StringTools.trim(rc.getArg()).toLowerCase();
                    EventData ed = (EventData)obj;
                    int       sc = ed.getStatusCode();
                    if (StringTools.isBlank(arg)   || 
                        (arg.indexOf("color") >= 0)  ) { // color, decColor, decimalColor
                        String scCode = arg.startsWith("dec")? 
                            String.valueOf(sc) : // decColor, decimalColor
                            ("0x" + StringTools.toHexString((long)sc,16)); // color, hexColor
                        StatusCodeProvider scp = ed.getStatusCodeProvider(rd.getPrivateLabel());
                        if (scp == null) {
                            return scCode;
                        } else
                        if (StringTools.isBlank(scp.getForegroundColor()) && 
                            StringTools.isBlank(scp.getBackgroundColor())   ) {
                            return scCode;
                        } else {
                            ColumnValue cv = new ColumnValue();
                            cv.setValue(scCode);
                            cv.setForegroundColor(scp.getForegroundColor());
                            cv.setBackgroundColor(scp.getBackgroundColor());
                            return cv;
                        }
                    } else
                    if (arg.startsWith("dec")) {
                        return String.valueOf(sc);
                    } else {
                        // arg.equalsIgnoreCase("noColor")
                        return "0x" + StringTools.toHexString((long)sc,16);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.statusCode","Status#");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                        StatusCodeProvider scp = ed.getStatusCodeProvider(rd.getPrivateLabel());
                        if (scp == null) {
                            String scDesc = "0x" + StringTools.toHexString((long)ed.getStatusCode(),16);
                            return scDesc;
                        } else {
                            String scDesc = scp.getDescription(rd.getLocale());
                            String scFG   = scp.getForegroundColor();
                            String scBG   = scp.getBackgroundColor();
                            if (StringTools.isBlank(scFG) && 
                                StringTools.isBlank(scBG)   ) {
                                return scDesc;
                            } else {
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(scDesc);
                                cv.setForegroundColor(scFG);
                                cv.setBackgroundColor(scBG);
                                return cv;
                            }
                        }
                    } else { 
                        // arg.equalsIgnoreCase("noColor")
                        String scDesc = ed.getStatusCodeDescription(rd.getPrivateLabel());
                        return scDesc;
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.statusDescription","Status");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PUSHPIN) {
                // EXPERIMENTAL! (the icons produced by this code section may not exactly match
                // those produced on the actual map by the JavaScript functions.
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    RequestProperties  reqState = rd.getRequestProperties();
                    BasicPrivateLabel  bpl      = rd.getPrivateLabel();
                    OrderedSet<String> iconKeys = reqState.getMapProviderIconKeys();
                    int       ppNdx = ed.getPushpinIconIndex(null/*iconSelector*/, iconKeys, false/*isFleet*/, bpl);
                    PushpinIcon ppi = reqState.getPushpinIcon(ppNdx);
                    String  iconURL = (ppi != null)? ppi.getIconEvalURL(ed,rowNdx) : "";
                    ColumnValue  cv = new ColumnValue().setImageURL(iconURL);
                    return cv;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.pushpin","Pushpin");
                }
            });

            // Entity ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getEntityID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.entityID","Entity-ID");
                }
            });

            // Entity Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    String aid = ed.getAccountID();
                    String eid = ed.getEntityID();
                    return Device.getEntityDescription(aid, eid, EntityManager.EntityType.TRAILER.getIntValue());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.entityDescription","Entity\nDescription");
                }
            });

            // Driver
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getDriverID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverID","Driver-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    // Driver ID
                    String drvID = ed.getDriverID();
                    if (StringTools.isBlank(drvID)) {
                        Device dev = ed.getDevice();
                        drvID = (dev != null)? dev.getDriverID() : null;
                    }
                    // Driver Description
                    String desc = drvID;
                    if (!StringTools.isBlank(drvID)) {
                        try {
                            Driver driver = Driver.getDriver(ed.getAccount(),drvID);
                            desc = (driver != null)? driver.getDescription() : drvID;
                        } catch (DBException dbe) {
                            desc = drvID;
                        }
                    }
                    return desc;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverDescription","Driver\nDescription");
                }
            });

            // Driver Status
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_STATUS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = StringTools.trim(rc.getArg());
                    EventData ed = (EventData)obj;
                    long drvStat = ed.getDriverStatus();
                    if (drvStat <= Driver.DutyStatus_UNKNOWN) {
                        return rc.getBlankFiller();
                    } else
                    if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("desc")) {
                        Driver.DutyStatus ds = Driver.getDutyStatus(drvStat);
                        if (ds != null) {
                            return ds.toString(rd.getLocale());
                        } else {
                            return String.valueOf(drvStat);
                        }
                    } else {
                        return String.valueOf(drvStat);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverStatus","Driver\nStatus");
                }
            });

            // Driver Message
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_MESSAGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getDriverMessage(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.driverMessage","Driver\nMessage");
                }
            });

            // JobNumber
            this.addColumnTemplate(new DataColumnTemplate(DATA_JOB_NUMBER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getJobNumber(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.jobNumber","Job\nNumber");
                }
            });

            // RFID Tag (Bar Code)
            this.addColumnTemplate(new DataColumnTemplate(DATA_RFID_TAG) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getRfidTag(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.rfidTag","RFID/Bar\nCode");
                }
            });
            
            // Sample ID/Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_SAMPLE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getSampleID(); // may be blank
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.sampleID","Sample\nID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SAMPLE_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return String.valueOf(ed.getSampleIndex());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.sampleIndex","Sample\nIndex");
                }
            });

            // ETA date/time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getEtaTimestamp();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaDateTime","ETA\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getEtaTimestamp();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaTimestamp","ETA\nTimestamp") + "\n(Epoch)";
                }
            });

            // ETA Unique ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_UNIQUE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getEtaUniqueID();
                    return String.valueOf(ts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaUniqueID","ETA\nUniqueID");
                }
            });

            // ETA Distance
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getEtaDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaDistance","ETA\nDistance") + "\n${distanceUnits}";
                }
            });

            // ETA Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getEtaLatitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaLat","ETA\nLat");
                }
            });
            
            // ETA Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getEtaLongitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaLon","ETA Lon");
                }
            });
            
            // ETA Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ETA_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getEtaLatitude();
                    double lon = ed.getEtaLongitude();
                    if (GeoPoint.isValid(lat,lon)) {
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else {
                            String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        }
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.etaLatLon","ETA Lat/Lon");
                }
            });

            // Stop Unique ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_UNIQUE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long id = ed.getStopID();
                    return String.valueOf(id);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopUniqueID","Stop\nUniqueID");
                }
            });

            // Stop Status
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_STATUS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long sst = ed.getStopStatus();
                    return String.valueOf(sst);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopStatus","Stop\nStatus");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_STATUS_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    int sst = ed.getStopStatus();
                    return DCServerFactory.Garmin_getStopStatusDescription(locale, sst);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopStatusDesc","Stop\nStatus");
                }
            });

            // Stop Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int ndx = ed.getStopIndex();
                    return String.valueOf(ndx);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopIndex","Stop\nIndex");
                }
            });

            // General applied pressure
            /*
            this.addColumnTemplate(new DataColumnTemplate(DATA_APPLIED_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getAppliedPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa > 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.applyPressure","Appled Press.") + "\n${pressureUnits}";
                }
            });
            */

            // GPS Age
            this.addColumnTemplate(new DataColumnTemplate(DATA_GPS_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long gpsAge = ed.getGpsAge();
                    //if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                    //    return (new ColumnValue(gpsAge)).setForegroundColor(GPS_AGE_COLOR_1); // .setFontStyleItalic();
                    //} else
                    //if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                    //    return (new ColumnValue(gpsAge)).setForegroundColor(GPS_AGE_COLOR_2);
                    //} else {
                    ReportLayout.AgeColorRange acr = rd.getGpsAgeColorRange(gpsAge);
                    if (acr != null) {
                        ColumnValue gcv = new ColumnValue(gpsAge);
                        return ReportLayout.SetColumnValueAgeColor(gcv,acr);
                    } else {
                        return String.valueOf(gpsAge);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.gpsAge","GPS\nAge");
                }
            });

            // Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getLatitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                    }
                    long gpsAge = ed.getGpsAge();
                    //if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                    //    return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_1); // .setFontStyleItalic();
                    //} else
                    //if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                    //    return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_2);
                    //} else
                    ReportLayout.AgeColorRange acr = rd.getGpsAgeColorRange(gpsAge);
                    if (acr != null) {
                        ColumnValue gcv = new ColumnValue(valStr);
                        return ReportLayout.SetColumnValueAgeColor(gcv,acr);
                    } else
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lat","Lat");
                }
            });
            
            // Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getLongitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                    }
                    long gpsAge = ed.getGpsAge();
                    //if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                    //    return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_1); // .setFontStyleItalic();
                    //} else
                    //if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                    //    return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_2);
                    //} else
                    ReportLayout.AgeColorRange acr = rd.getGpsAgeColorRange(gpsAge);
                    if (acr != null) {
                        ColumnValue gcv = new ColumnValue(valStr);
                        return ReportLayout.SetColumnValueAgeColor(gcv,acr);
                    } else
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lon","Lon");
                }
            });
            
            // Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getLatitude();
                    double lon = ed.getLongitude();
                    if (GeoPoint.isValid(lat,lon)) {
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else {
                            String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        }
                        long gpsAge = ed.getGpsAge();
                        //if (gpsAge >= GPS_AGE_THRESHOLD_1) {
                        //    return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_1); // .setFontStyleItalic();
                        //} else
                        //if (gpsAge >= GPS_AGE_THRESHOLD_2) {
                        //    return (new ColumnValue(valStr)).setForegroundColor(GPS_AGE_COLOR_2);
                        //} else
                        ReportLayout.AgeColorRange acr = rd.getGpsAgeColorRange(gpsAge);
                        if (acr != null) {
                            ColumnValue gcv = new ColumnValue(valStr);
                            return ReportLayout.SetColumnValueAgeColor(gcv,acr);
                        } else
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.latLon","Lat/Lon");
                }
            });

            // GPS Fix Type ("Unknown", "None", "2D", "3D")
            this.addColumnTemplate(new DataColumnTemplate(DATA_GPSFIX_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    return ed.getGpsFixTypeDescription(locale);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.gpsFixType","Fix Type");
                }
            });

            // GPS Fix Status (bitmask defined by DCS)
            this.addColumnTemplate(new DataColumnTemplate(DATA_GPSFIX_STATUS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = StringTools.trim(rc.getArg()).toLowerCase();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    long fixStat = ed.getGpsFixStatus();
                    if (arg.equalsIgnoreCase("dec")) {
                        return String.valueOf(fixStat);
                    } else
                    if (arg.equalsIgnoreCase("hex")) {
                        return "0x" + StringTools.toHexString(fixStat,16);
                    } else
                    if (arg.equalsIgnoreCase("bin")) {
                        return "b" + StringTools.toBinaryString(fixStat,32,null).toString();
                    } else {
                        return String.valueOf(fixStat);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.gpsFixStatus","Fix Status");
                }
            });

            // Accuracy
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCURACY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double acc = ed.getHorzAccuracy(); // meters
                    if (acc > 0) {
                        Account.AltitudeUnits altUnits = Account.getAltitudeUnits(rd.getAccount());
                        acc = altUnits.convertFromMeters(acc);
                        return EventDataLayout.formatDouble(acc, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.accuracy","Accuracy") + "\n${accuracyUnits}";
                }
            });

            // Altitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ALTITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Account.AltitudeUnits altUnits = Account.getAltitudeUnits(rd.getAccount());
                    double alt = altUnits.convertFromMeters(ed.getAltitude());
                    return EventDataLayout.formatDouble(alt, arg, "0");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.altitude","Altitude") + "\n${altitudeUnits}";
                }
            });

            // Speed limit (posted speed)
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_LIMIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedLimitKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        double speed = Account.getSpeedUnits(a).convertFromKPH(kph);
                        return EventDataLayout.formatDouble(speed, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speedLimit","Speed Limit") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        double speed = Account.getSpeedUnits(a).convertFromKPH(kph);
                        return EventDataLayout.formatDouble(speed, arg, "0");
                    } else {
                        return "0   ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a       = rd.getAccount();
                        double speed    = Account.getSpeedUnits(a).convertFromKPH(kph);
                        String speedStr = EventDataLayout.formatDouble(speed, arg, "0");
                        String headStr  = GeoPoint.GetHeadingString(ed.getHeading(),rd.getLocale()).toUpperCase();
                        if (headStr.length() == 1) {
                            headStr += " ";
                        }
                        return speedStr + " " + headStr;
                    } else {
                        return "0   ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_UNITS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        String unitAbbr = Account.getSpeedUnits(a).toString(rd.getLocale());
                        double speed    = Account.getSpeedUnits(a).convertFromKPH(kph);
                        String speedStr = EventDataLayout.formatDouble(speed, arg, "0");
                        String headStr  = GeoPoint.GetHeadingString(ed.getHeading(),rd.getLocale()).toUpperCase();
                        if (headStr.length() == 1) {
                            headStr += " ";
                        }
                        return speedStr + unitAbbr + " " + headStr;
                    } else {
                        return "0    ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.speed","Speed");
                }
            });

            // Heading
            this.addColumnTemplate(new DataColumnTemplate(DATA_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        double heading = ed.getHeading();
                        if (!StringTools.isBlank(arg)) {
                            return EventDataLayout.formatDouble(heading, arg, "0");
                        } else {
                            return GeoPoint.GetHeadingString(heading,rd.getLocale()).toUpperCase();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return GeoPoint.GetHeadingTitle(rd.getLocale());
                }
            });

            // #Satellites
            this.addColumnTemplate(new DataColumnTemplate(DATA_SATELLITES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int satCount = ed.getSatelliteCount();
                    if (satCount > 0) {
                        return String.valueOf(satCount);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.satelliteCount","Sat\nCount");
                }
            });

            // Distance
            this.addColumnTemplate(new DataColumnTemplate(DATA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.distance","Distance") + "\n${distanceUnits}";
                }
            });

            // Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double  odom = ed.getOdometerKM(); // kilometers
                    if (odom > 0.0) {
                        // has odometer value
                        odom = ed.getOdometerWithOffsetKM();
                    } else {
                        // use distance as odometer value
                        odom = ed.getDistanceKM(); // may still be 0.0
                        odom += ed.getOdometerOffsetKM(null);
                    }
                    return EventDataLayout.formatKM(odom, arg, rd);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.odometer","Odometer") + "\n${distanceUnits}";
                }
            });

            // Report distance
            this.addColumnTemplate(new DataColumnTemplate(DATA_REPORT_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double dist = ed.getReportDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.reportDistance","Report\nDistance") + "\n${distanceUnits}";
                }
            });

            // Stopped time/elapsed
            /*
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    if (ed.isStopped()) {
                        long ts = ed.getMotionChangeTime();
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone  tz = rd.getTimeZone();
                            DateTime  dt = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                            return new ColumnValue(dtFmt).setSortKey(ts);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopDateTime","Stop\nDate/Time") + "\n${timezone}";
                }
            });
            */
            /*
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    if (ed.isStopped()) {
                        // stopped
                        long ts = ed.getMotionChangeTime();
                        if (ts > 0L) {
                            long ds = ed.getTimestamp() - ts;
                            if (ds >= 0L) {
                                int fmt = EventDataLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                                return new ColumnValue(EventDataLayout.formatElapsedTime(ds,fmt)).setSortKey(ds);
                            }
                        }
                        // last stopped time not initialized or invalid
                        return rc.getBlankFiller();
                    } else {
                        // moving
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stopElapsed","Stop Elapsed");
                }
            });
            */

            // Cell Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getCellLatitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellLat","Cell\nLat");
                }
            });
            
            // Cell Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getCellLongitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellLon","Cell\nLon");
                }
            });

            // Cell Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getCellLatitude();
                    double lon = ed.getCellLongitude();
                    if (GeoPoint.isValid(lat,lon)) {
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else {
                            String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        }
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellLatLon","Cell\nLat/Lon");
                }
            });

            // Cell Accuracy
            this.addColumnTemplate(new DataColumnTemplate(DATA_CELL_ACCURACY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double acc = ed.getCellAccuracy(); // meters
                    if (acc > 0) {
                        // convert to same units used for altitude
                        Account.AltitudeUnits altUnits = Account.getAltitudeUnits(rd.getAccount());
                        acc = altUnits.convertFromMeters(acc);
                        return EventDataLayout.formatDouble(acc, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.cellAccuracy","Cell Accuracy") + "\n${accuracyUnits}";
                }
            });

            // Best Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getBestLatitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestLat","Lat");
                }
            });
            
            // Best Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getBestLongitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestLon","Lon");
                }
            });

            // Best Latitude/Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getBestLatitude();
                    double lon = ed.getBestLongitude();
                    if (GeoPoint.isValid(lat,lon)) {
                        arg = StringTools.trim(arg);
                        String valStr = "";
                        Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DMS, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else
                        if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM) || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                            String latStr = GeoPoint.formatLatitude( lat, GeoPoint.SFORMAT_DM , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        } else {
                            String fmt    = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                            String latStr = GeoPoint.formatLatitude( lat, fmt  , locale);
                            String lonStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                            valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        }
                        if (!StringTools.isBlank(valStr)) {
                            return valStr;
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestLatLon","Lat/Lon");
                }
            });

            // Best Accuracy
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_ACCURACY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double acc = ed.getBestAccuracy(); // meters
                    if (acc > 0) {
                        Account.AltitudeUnits altUnits = Account.getAltitudeUnits(rd.getAccount());
                        acc = altUnits.convertFromMeters(acc);
                        return EventDataLayout.formatDouble(acc, arg, "0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.bestAccuracy","Accuracy") + "\n${accuracyUnits}";
                }
            });

            // Input Mask/State
            this.addColumnTemplate(new DataColumnTemplate(DATA_INPUT_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int input = (int)ed.getInputMask();  // bit mask
                    String s = StringTools.toBinaryString(input);
                    int slen = s.length();
                    int blen = StringTools.parseInt(arg,8);
                    int len  = (slen >= blen)? (slen - blen) : 0;
                    return s.substring(len, slen);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.inputBitMask","Inputs\n(BitMask)");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_INPUT_BIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    int argBit = StringTools.parseInt(rc.getArg(),0);
                    EventData ed = (EventData)obj;
                    int input = (int)ed.getInputMask(); // bit mask
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    if ((input & (1 << argBit)) != 0) {
                        return i18n.getString("EventDataLayout.bitTrue" ,"On" );
                    } else {
                        return i18n.getString("EventDataLayout.bitFalse","Off");
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    int argBit = StringTools.parseInt(rc.getArg(),0);
                    String bitStr = String.valueOf(argBit);
                    return i18n.getString("EventDataLayout.inputBitValue","Input\n#{0}", bitStr);
                }
            });

            // Output Mask/State
            this.addColumnTemplate(new DataColumnTemplate(DATA_OUTPUT_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int output = (int)ed.getOutputMask(); // bit mask
                    String s = StringTools.toBinaryString(output);
                    int slen = s.length();
                    int blen = StringTools.parseInt(arg,8);
                    int len  = (slen >= blen)? (slen - blen) : 0;
                    return s.substring(len, slen);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.outputBitMask","Outputs\n(BitMask)");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_OUTPUT_BIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    int argBit = StringTools.parseInt(rc.getArg(),0);
                    EventData ed = (EventData)obj;
                    int output = (int)ed.getOutputMask(); // bit mask
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    if ((output & (1 << argBit)) != 0) {
                        return i18n.getString("EventDataLayout.bitTrue" ,"On" );
                    } else {
                        return i18n.getString("EventDataLayout.bitFalse","Off");
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    int argBit = StringTools.parseInt(rc.getArg(),0);
                    String bitStr = String.valueOf(argBit);
                    return i18n.getString("EventDataLayout.outputBitValue","Output\n#{0}", bitStr);
                }
            });

            // Seatbelt Mask/State
            this.addColumnTemplate(new DataColumnTemplate(DATA_SEATBELT_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int seatbelt = (int)ed.getSeatbeltMask(); // bit mask
                    String s = StringTools.toBinaryString(seatbelt);
                    int slen = s.length();
                    int blen = StringTools.parseInt(arg,4);
                    int len  = (slen >= blen)? (slen - blen) : 0;
                    return s.substring(len, slen);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.seatbeltBitMask","Seatbelts\n(BitMask)");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SEATBELT_BIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    int argBit = StringTools.parseInt(rc.getArg(),0);
                    EventData ed = (EventData)obj;
                    int seatbelt = (int)ed.getSeatbeltMask(); // bit mask
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    if ((seatbelt & (1 << argBit)) != 0) {
                        return i18n.getString("EventDataLayout.bitTrue" ,"On" );
                    } else {
                        return i18n.getString("EventDataLayout.bitFalse","Off");
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    int argBit = StringTools.parseInt(rc.getArg(),0);
                    String bitStr = String.valueOf(argBit);
                    return i18n.getString("EventDataLayout.seatbeltBitValue","Seatbelt\n#{0}", bitStr);
                }
            });

            // Geozone-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getGeozoneID();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.geozoneID","Geozone-ID");
                }
            });

            // Geozone Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getGeozoneDescription();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.geozoneDescription","Geozone\nDescription");
                }
            });

            // Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getAddress();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    RequestProperties reqState = rd.getRequestProperties();
                    String addrTitles[] = (reqState != null)? reqState.getAddressTitles() : null;
                    String addrTitle    = (ListTools.size(addrTitles) > 0)? addrTitles[0] : null;
                    if (!StringTools.isBlank(addrTitle)) {
                        return addrTitle;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.address","Address");
                    }
                }
            });
            
            // City
            this.addColumnTemplate(new DataColumnTemplate(DATA_CITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getCity();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.city","City");
                }
            });
            
            // State/Province
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getStateProvince();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.stateProvince","State\nProvince");
                }
            });

            // Country
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNTRY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getCountry();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.country","Country");
                }
            });

            // Subdivision
            this.addColumnTemplate(new DataColumnTemplate(DATA_SUBDIVISION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getSubdivision();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.subdivision","Subdivision");
                }
            });
            
            // Atmosphere
            this.addColumnTemplate(new DataColumnTemplate(DATA_BAROMETER) {
                // Barometric pressure
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getBarometer(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa > 0.0) {
                        //double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        double pressure = Account.PressureUnits.MMHG.convertFromKPa(kPa); // always convert to mmHg
                        //return StringTools.format(pressure, "#0.00");
                        return EventDataLayout.formatDouble(pressure, arg, "0.00");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.barometer","Barometer") + "\nmmHg";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AMBIENT_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getAmbientTemp(); // degrees 'C'
                    if (EventData.isValidTemperature(C)) {
                        String tempS = EventDataLayout.formatTemperature(C, arg, rd, null);
                        return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ambientTemp","Ambient\nTemp");
                }
            });

            // Temperature (report index starts at '1')
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(0); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#1";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(1); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#2";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_3) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(2); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#3";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_4) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(3); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#4";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_5) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(4); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#5";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_6) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(5); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#6";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_7) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(6); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#7";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_THERMO_8) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tempC = ed.getThermoAverage(7); // degrees 'C'
                    String tempS = EventDataLayout.formatTemperature(tempC, arg, rd, null);
                    return new ColumnValue(tempS).setSortKey((long)(tempC * 100.0));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.temperature","Temp") + "\n#8";
                }
            });

            // Battery level (% or volts?)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getBatteryLevel();
                    double volts = ed.getBatteryVolts();
                    if (level > 0.0) {
                        if (level <= 1.0) {
                            // -- assume range 0..1
                            return Math.round(level*100.0) + "%";           // percent
                        } else {
                            // -- assume range 0..100
                            return EventDataLayout.formatDouble(level, arg, "0.0") + "v";   // volts
                        }
                    } else
                    if (volts > 0.0) {
                        return EventDataLayout.formatDouble(volts, arg, "0.0") + "v";   // volts
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.BatteryLevel","Battery\nLevel");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY_VOLTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getBatteryVolts();
                    if (level > 0.0) {
                        return EventDataLayout.formatDouble(level, arg, "0.0");  // volts
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.BatteryVolts","Battery\nVolts");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY_PERCENT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getBatteryLevel();
                    if (level > 0.0) {
                        // -- adjust to range 0..1
                        double pct100 = (level <= 1.0)? (level*100.0) : level;
                        return Math.round(pct100) + "%";    // integer percent
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.BatteryPercent","Battery\n%");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_BATTERY_PCT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    double level = dev.getLastBatteryLevel();
                    if (level > 0.0) {
                        double pct100 = (level <= 1.0)? (level*100.0) : level;
                        return Math.round(pct100) + "%";    // integer percent
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.latestBatteryPercent","Latest\nBatt %");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BATTERY_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getBatteryTemp();
                    if (EventData.isValidTemperature(C)) {
                        String tempS = EventDataLayout.formatTemperature(C, arg, rd, null);
                        return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.batteryTemp","Battery\nTemp.");
                }
            });

            // Analog
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog0();
                    return EventDataLayout.formatDouble(level, arg, "0.0");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog0","Analog 0");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog1();
                    return EventDataLayout.formatDouble(level, arg, "0.0");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog1","Analog 1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog2();
                    return EventDataLayout.formatDouble(level, arg, "0.0");  // volts
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog2","Analog 2");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ANALOG_3) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double level = ed.getAnalog3();
                    return EventDataLayout.formatDouble(level, arg, "0.0");  // volts
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.analog3","Analog 3");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PULSE_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double pc = ed.getPulseCount();
                    return EventDataLayout.formatDouble(pc, arg, "0.0");  // count * gain
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.pulseCount","Pulse\nCount");
                }
            });

            // Fuel Capacity (device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_CAPACITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double   vol = dev.getFuelCapacity(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelCapacity","Fuel Capacity") + "\n${volumeUnits}";
                }
            });

            // Fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double fuelLevel = ed.getFuelLevel(true/*estimate*/);
                    if (fuelLevel > 0.0) {
                        return Math.round(fuelLevel*100.0) + "%";
                    } else
                    if (fuelLevel < 0.0) {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.notAvailable","n/a");
                    } else {
                        return rc.getBlankFiller(); // "n/a"
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelPercent","Fuel%");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL_VOL) { // see also DATA_FUEL_CAPACITY
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double capacity = dev.getFuelCapacity(); // liters
                    double percent  = ed.getFuelLevel(true/*estimate*/); // estimate
                    if ((percent <= 0.0) && (capacity <= 0.0)) {
                        return rc.getBlankFiller();
                    } else
                    if (capacity <= 0.0) {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.notAvailable","n/a");
                    } else {
                        double liters = capacity * percent; // liters
                        double vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(liters);
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } 
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelLevelVolume","Fuel Vol") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getFuelTotal(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelTotal","Total Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_REMAIN) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getFuelRemain(true/*estimate*/); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelRemain","Remaining Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TRIP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getFuelTrip(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelTrip","Trip Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getFuelIdle(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelIdle","Idle Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ENGINE_ON) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getFuelEngineOn(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelEngineOn","Engine On Fuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ECONOMY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double  econ = ed.getFuelEconomy(); // kilometers per liter
                    if (econ > 0.0) {
                        econ = Account.getEconomyUnits(rd.getAccount()).convertFromKPL(econ);
                        return EventDataLayout.formatDouble(econ, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelEcon","Fuel Econ") + "\n${economyUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ECONOMY_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = StringTools.trim(rc.getArg());
                    EventData ed = (EventData)obj;
                    Device.FuelEconomyType econType = Device.FuelEconomyType.EVENT_ECONOMY;
                    Locale locale = rd.getPrivateLabel().getLocale();
                    String typeStr = arg.equalsIgnoreCase("abbr")?
                        econType.getAbbrev(locale) :
                        econType.toString(locale);
                    return typeStr;
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelEconType","Fuel Econ\nType");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getFuelPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        //return StringTools.format(pressure, "#0.0");
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelPressure","Fuel Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_RATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   lph = ed.getFuelUsage(); // Litre per Hour
                    if (lph != 0.0) {
                        double rate = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(lph);
                        //return StringTools.format(rate, "#0.0");
                        return EventDataLayout.formatDouble(rate, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelRate","Fuel Rate") + "\n${volumeUnits}/Hr";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double fuelLevel = ed.getFuelLevel2(); // device.getActualFuelLevel(ed.getFuelLevel2());
                    if (fuelLevel > 0.0) {
                        return Math.round(fuelLevel*100.0) + "%";
                    } else
                    if (fuelLevel < 0.0) {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.notAvailable","n/a");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelPercent2","Fuel2%");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL_VOL_2) { // see also DATA_FUEL_CAPACITY
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double capacity = dev.getFuelCapacity(); // liters
                    double percent  = ed.getFuelLevel2(); // device.getActualFuelLevel(ed.getFuelLevel2());
                    if ((percent <= 0.0) && (capacity <= 0.0)) {
                        return rc.getBlankFiller();
                    } else
                    if (capacity <= 0.0) {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.notAvailable","n/a");
                    } else {
                        double liters = capacity * percent; // liters
                        double vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(liters);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } 
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.fuelLevelVolume2","Fuel Vol 2") + "\n${volumeUnits}";
                }
            });

            // PTO
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_ENGAGED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    boolean pto = ed.getPtoEngaged();
                    return ComboOption.getYesNoText(rd.getLocale(), pto);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ptoEngaged","PTO\nEngaged");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getPtoHours();
                    if (hours > 0.0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ptoHours","PTO\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getPtoDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.ptoDistance","PTO Distance") + "\n${distanceUnits}";
                }
            });

            // Work
            this.addColumnTemplate(new DataColumnTemplate(DATA_WORK_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getWorkHours();
                    if (hours > 0.0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.workHours","Work\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_WORK_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getWorkDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.workDistance","Work Distance") + "\n${distanceUnits}";
                }
            });

            // Vehicle Battery Volts
            this.addColumnTemplate(new DataColumnTemplate(DATA_VEH_BATTERY_VOLTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double volts = ed.getVBatteryVolts();
                    if (volts > 0.0) {
                        //return StringTools.format(volts,"#0.0");
                        return EventDataLayout.formatDouble(volts, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.vBatteryVolts","Veh Batt.\nVolts");
                }
            });

            // Throttle Position
            this.addColumnTemplate(new DataColumnTemplate(DATA_THROTTLE_POSITION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double throttlePos = ed.getThrottlePos();
                    if (throttlePos > 0.0) {
                        return Math.round(throttlePos*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.throttlePosition","Throttle\nPosition");
                }
            });

            // Air
            this.addColumnTemplate(new DataColumnTemplate(DATA_INTAKE_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getIntakeTemp(); // degrees 'C'
                    if (EventData.isValidTemperature(C)) {
                        String tempS = EventDataLayout.formatTemperature(C, arg, rd, null);
                        return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.intakeTemp","Intake\nTemp.");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AIR_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getAirPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        //return StringTools.format(pressure, "#0.0");
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.airPressure","Air Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AIR_FILTER_PRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getAirFilterPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        //return StringTools.format(pressure, "#0.0");
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.airFilterPressure","Air Filt\nPress.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_MASS_AIR_FLOW) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double  rate = ed.getMassAirFlowRate(); // Grams/Second
                    if (rate != 0.0) {
                        return EventDataLayout.formatDouble(rate, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.massAirFlow","Mass Air Flow") + "\ng/sec";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TURBO_PRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getTurboPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        //return StringTools.format(pressure, "#0.0");
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.turboPressure","Turbo\nPress.") + "\n${pressureUnits}";
                }
            });

            // Tire pressure
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIRE_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kPa[] = ed.getTirePressure_kPa();
                    if (!ListTools.isEmpty(kPa)) {
                        Account.PressureUnits pu = Account.getPressureUnits(rd.getAccount());
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < kPa.length; i++) {
                            double pressure = pu.convertFromKPa(kPa[i]);
                            if (sb.length() > 0) { sb.append(","); }
                            sb.append(EventDataLayout.formatDouble(pressure, arg, "0"));
                        }
                        return sb.toString();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tirePressure","Tire Press.") + "\n${pressureUnits}";
                }
            });

            // Tire temperature
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIRE_TEMPERATURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C[] = ed.getTireTemp_C();
                    if (!ListTools.isEmpty(C)) {
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < C.length; i++) {
                            if (sb.length() > 0) { sb.append(","); }
                            String tempFmt = EventDataLayout.formatTemperature(C[i], arg, rd, "?");
                            sb.append(tempFmt);
                        }
                        return sb.toString();
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tireTemperature","Tire Temp.");
                }
            });

            // Tank
            this.addColumnTemplate(new DataColumnTemplate(DATA_TANK_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double tankLevel = ed.getTankLevel();
                    if (tankLevel > 0.0) {
                        return Math.round(tankLevel*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tankPercent","Tank%");
                }
            });

            // Fault Code(s)
            this.addColumnTemplate(new DataColumnTemplate(DATA_FAULT_CODES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = StringTools.trim(rc.getArg());
                    EventData ed = (EventData)obj;
                    String faultStr = ed.getFaultCode(); // RTProperties String
                    if (StringTools.isBlank(faultStr)) {
                        long fault = ed.getOBDFault(); // same as "getJ1708Fault()"
                        if (fault == 0L) {
                            return rc.getBlankFiller();
                        } else {
                            return DTOBDFault.GetFaultString(fault);
                        }
                    } else {
                        return DTOBDFault.GetFaultString(new RTProperties(faultStr));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.j1708Faults","OBD Faults");
                }
            });
           this.addColumnTemplate(new DataColumnTemplate(DATA_FAULT_CODE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = StringTools.trim(rc.getArg());
                    EventData ed = (EventData)obj;
                    long fault = ed.getOBDFault(); // same as "getJ1708Fault()"
                    if (fault == 0L) {
                        // not defined
                        return rc.getBlankFiller();
                    } else 
                    if (DTOBDFault.IsJ1708(fault)) {
                        // J1708/J1587
                        if (DTOBDFault.HasDescriptionProvider(fault)) {
                            Locale locale = rd.getLocale();
                            if (arg.equalsIgnoreCase("link")) {
                                ColumnValue cv = new ColumnValue(DTOBDFault.GetFaultString(fault));
                                RequestProperties reqState = rd.getRequestProperties();
                                URIArg j1587URL = WebPageAdaptor.MakeURL(reqState.getBaseURI(),"j1587.show"); // Constants.PAGE_J1587_SHOW);
                                j1587URL.addArg("mid" , DTOBDFault.DecodeSystem(fault));
                                j1587URL.addArg("spid", DTOBDFault.DecodeSPID(fault));
                                j1587URL.addArg("fmi" , DTOBDFault.DecodeFMI(fault));
                                cv.setLinkURL("javascript:openResizableWindow('"+j1587URL+"','J1587Desc',320,100);",null/*target*/);
                                return cv;
                            } else 
                            if (arg.equalsIgnoreCase("desc")) {
                                String desc = DTOBDFault.GetFaultDescription(fault, locale);
                                return desc;
                            } else {
                                return DTOBDFault.GetFaultString(fault);
                            }
                        } else {
                            return DTOBDFault.GetFaultString(fault);
                        }
                    } else 
                    if (DTOBDFault.IsOBDII(fault)) {
                        // OBDII DTC
                        if (DTOBDFault.HasDescriptionProvider(fault)) {
                            // TODO: get DTC description
                            return DTOBDFault.GetFaultString(fault);
                        } else {
                            return DTOBDFault.GetFaultString(fault);
                        }
                    } else {
                        return DTOBDFault.GetFaultString(fault);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.j1708Fault","OBD Fault"); // + "\nMID/PID/FMI";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_MALFUNCTION_LAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    boolean mil = ed.getMalfunctionLamp();
                    return ComboOption.getOnOffText(rd.getLocale(), mil);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.malfunctionLamp","Malfunction\nLamp");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_FAULT_CODES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    return dev.getLastFaultCode().toUpperCase();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.latestFaultCodes","Fault Codes");
                }
            });

            // Engine oil level
            this.addColumnTemplate(new DataColumnTemplate(DATA_OIL_LEVEL) {
                // Oil pressure (http://en.wikipedia.org/wiki/KPa)
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double oilLvl = ed.getOilLevel(); // %
                    if (oilLvl > 0.0) {
                        return Math.round(oilLvl*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.oilLevel","Oil Level");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_OIL_PRESSURE) {
                // Oil pressure (http://en.wikipedia.org/wiki/KPa)
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getOilPressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa > 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        //return StringTools.format(pressure, "#0.0");
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.oilPressure","Oil Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_OIL_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getOilTemp(); // degrees 'C'
                    if (EventData.isValidTemperature(C)) {
                        String tempS = EventDataLayout.formatTemperature(C, arg, rd, null); 
                        return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.oilTemp","Oil\nTemp.");
                }
            });

            // Engine
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_RPM) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long rpm = ed.getEngineRpm();
                    if (rpm >= 0L) {
                        return String.valueOf(rpm);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineRpm","Engine\nRPM");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    double hours = ed.getEngineHours();
                    hours += dev.getEngineHoursOffset();
                    if (hours > 0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineHours","Engine\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_ON_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getEngineOnHours();
                    if (hours > 0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineOnHours","Engine On\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_LOAD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double engineLoad = ed.getEngineLoad();
                    if (engineLoad > 0.0) {
                        return Math.round(engineLoad*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.engineLoad","Engine\nLoad %");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getIdleHours();
                    if (hours > 0.0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.idleHours","Idle\nHours");
                }
            });

            // Transmission Oil
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRANS_OIL_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getTransOilTemp(); // degrees 'C'
                    if (EventData.isValidTemperature(C)) {
                        String tempS = EventDataLayout.formatTemperature(C, arg, rd, null); 
                        return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.transOilTemp","Trans Oil\nTemp.");
                }
            });

            // Coolant
            this.addColumnTemplate(new DataColumnTemplate(DATA_COOLANT_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double coolantLevel = ed.getCoolantLevel();
                    if (coolantLevel > 0.0) {
                        return Math.round(coolantLevel*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.coolantLevel","Coolant\nLevel");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_COOLANT_TEMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double C = ed.getCoolantTemp(); // degrees 'C'
                    if (EventData.isValidTemperature(C)) {
                        String tempS = EventDataLayout.formatTemperature(C, arg, rd, null);
                        return new ColumnValue(tempS).setSortKey((long)(C * 100.0));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.coolantTemp","Coolant\nTemp.");
                }
            });

            // Brake
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_G_FORCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   bgf = ed.getBrakeGForce();
                    if (bgf != 0.0) {
                        return EventDataLayout.formatDouble(bgf, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakeGForce","Braking\nG-force");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_FORCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg   = rc.getArg();
                    EventData ed = (EventData)obj;
                    double bgf   = ed.getBrakeGForce();
                    double kphs  = ((Account.MPSS_PER_G_FORCE * bgf) / 1000.0) * 3600.0; // km/hr/sec
                    if (kphs != 0.0) {
                        return EventDataLayout.formatKM(kphs, arg, rd);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakeForce","Braking\n${distanceUnits}/hr/sec");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_PRESSURE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   kPa = ed.getBrakePressure(); // kPa (kilopascals = 1000 Newtons per Square-Meter)
                    if (kPa != 0.0) {
                        double pressure = Account.getPressureUnits(rd.getAccount()).convertFromKPa(kPa);
                        //return StringTools.format(pressure, "#0.0");
                        return EventDataLayout.formatDouble(pressure, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakePressure","Brake Press.") + "\n${pressureUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BRAKE_POSITION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double throttlePos = ed.getBrakePos();
                    if (throttlePos > 0.0) {
                        return Math.round(throttlePos*100.0) + "%";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.brakePosition","Brake\nPosition");
                }
            });

            // Acceleration/Deceleration (meters per seconds-squared)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCELERATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   mss = ed.getAcceleration();
                    if (mss != 0.0) {
                        //return StringTools.format(mss, "#0.0");
                        return EventDataLayout.formatDouble(mss, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.acceleration","Accel\nm/s/s");
                }
            });

            // last connect/checkin date/time (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device   dev = ed.getDevice();
                    long      ts = dev.getLastTotalConnectTime();
                    if (ts <= 0L) {
                        try {
                            EventData lastEv = dev.getLastEvent(-1L, false);
                            if (lastEv != null) {
                                ts = lastEv.getTimestamp();
                            }
                        } catch (DBException dbe) {
                            // error retrieving event record
                        }
                    }
                    if (ts > MINIMUM_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        //Account a = rd.getAccount();
                        //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        if (ageSec >= DateTime.HourSeconds(24)) {
                            cv.setForegroundColor("red");
                        }
                        return cv;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lastCheckinTime","Last Check-In\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev = ed.getDevice();
                    long ts = dev.getLastTotalConnectTime();
                    if (ts <= 0L) {
                        try {
                            EventData lastEv = dev.getLastEvent(-1L, false);
                            if (lastEv != null) {
                                ts = lastEv.getTimestamp();
                            }
                        } catch (DBException dbe) {
                            // error retrieving event record
                        }
                    }
                    if (ts > MINIMUM_REASONABLE_TIMESTAMP) {
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        long days   = (ageSec / DateTime.DaySeconds(1));
                        long hours  = (ageSec % DateTime.DaySeconds(1)) / DateTime.HourSeconds(1);
                        long min    = (ageSec % DateTime.HourSeconds(1)) / DateTime.MinuteSeconds(1);
                        StringBuffer sb = new StringBuffer();
                        sb.append(days ).append("d ");
                        if (hours < 10) { sb.append("0"); }
                        sb.append(hours).append("h ");
                        if (min   < 10) { sb.append("0"); }
                        sb.append(min  ).append("m");
                        ColumnValue cv = new ColumnValue(sb.toString()).setSortKey(ageSec);
                        if (ageSec >= DateTime.HourSeconds(24)) {
                            cv.setForegroundColor("red");
                        }
                        return cv;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.lastCheckinAge","Since Last\nCheck-In");
                }
            });

            // custom field value (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CUSTOM_FIELD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Device dev   = ed.getDevice();
                    String value = (dev != null)? dev.getCustomAttribute(arg) : "";
                    if (!StringTools.isBlank(value)) {
                        return value;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg = rc.getArg();
                    String desc = rd.getPrivateLabel().getStringProperty(BasicPrivateLabel.PROP_DeviceInfo_custom_ + arg, null);
                    if (!StringTools.isBlank(desc)) {
                        if (desc.length() > 12) {
                            int p = desc.lastIndexOf(" ");
                            if (p > 0) {
                                desc = desc.substring(0,p) + "\n" + desc.substring(p+1);
                            }
                        }
                        return desc;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        return i18n.getString("EventDataLayout.customAttribute","Custom\nAttribute");
                    }
                }
            });

            // DataSource/RawData (unparsed event packet)
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATA_SOURCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getDataSource();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dataSource","Data Source");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_RAW_DATA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    return ed.getRawData();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.rawData","Raw Data");
                }
            });

            // "Trip" values
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_START_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTripStartTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripStartDateTime","Trip Start\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_STOP_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long ts = ed.getTripStopTime();
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone tz  = rd.getTimeZone();
                        DateTime dt  = new DateTime(ts);
                        String dtFmt = dt.format(rl.getDateTimeFormat(rd.getPrivateLabel()), tz);
                        return new ColumnValue(dtFmt).setSortKey(ts);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripStopDateTime","Trip Stop\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getTripDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripDistance","Trip\nDistance") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getTripIdleHours();
                    if (hours > 0.0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripIdleHours","Trip Idle\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_MAX_SPEED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double kph = ed.getTripMaxSpeedKPH(); // KPH
                    if (kph > 0.0) {
                        Account a = rd.getAccount();
                        double speed = Account.getSpeedUnits(a).convertFromKPH(kph);
                        return EventDataLayout.formatDouble(speed, arg, "0");
                    } else {
                        return "0   ";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripMaxSpeed","Trip Max\nSpeed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_MAX_RPM) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long rpm = ed.getTripMaxRpm();
                    if (rpm >= 0L) {
                        return String.valueOf(rpm);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripMaxRpm","Trip Max\nRPM");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_START_LAT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lat = ed.getTripStartLatitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLatitude(lat, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLatitude(lat, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripStartLatitude","Trip Start\nLatitude");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_START_LON) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    Locale locale = rd.getLocale();
                    double lon = ed.getTripStartLongitude();
                    arg = StringTools.trim(arg);
                    String valStr = "";
                    Account.LatLonFormat latlonFmt = Account.getLatLonFormat(rd.getAccount());
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DMS) || (StringTools.isBlank(arg) && latlonFmt.isDegMinSec())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DMS, locale);
                    } else
                    if (arg.equalsIgnoreCase(GeoPoint.SFORMAT_DM)  || (StringTools.isBlank(arg) && latlonFmt.isDegMin())) {
                        valStr = GeoPoint.formatLongitude(lon, GeoPoint.SFORMAT_DM , locale);
                    } else {
                        String fmt = StringTools.isBlank(arg)? GeoPoint.SFORMAT_DEC_4 : arg;
                        valStr = GeoPoint.formatLongitude(lon, fmt  , locale);
                    }
                    if (!StringTools.isBlank(valStr)) {
                        return valStr;
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripStopLatitude","Trip Start\nLongitude");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    long tripSec = ed.getTripElapsedSeconds();
                    if (tripSec > 0L) {
                        int fmt = EventDataLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                        return new ColumnValue(EventDataLayout.formatElapsedTime(tripSec,fmt)).setSortKey(tripSec);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.tripElapsed","Trip Elapsed");
                }
            });

            // "Day" values
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_ENGINE_STARTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    int engStarts = ed.getDayEngineStarts();
                    return String.valueOf(engStarts);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayEngineStarts","Day Engine\nStarts");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getDayIdleHours();
                    if (hours > 0.0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayIdleHours","Day Idle\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getDayFuelIdle(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelIdle","Day Idle\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_WORK_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double hours = ed.getDayWorkHours();
                    if (hours > 0.0) {
                        return EventDataLayout.formatDouble(hours, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayWorkHours","Day Work\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_WORK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getDayFuelWork(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelWork","Day Work\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_PTO) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getDayFuelPTO(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelPTO","Day PTO\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String   arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double   vol = ed.getDayFuelTotal(); // liters
                    if (vol > 0.0) {
                        vol = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(vol);
                        //return StringTools.format(vol, "#0.0");
                        return EventDataLayout.formatDouble(vol, arg, "0.0");
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayFuelTotal","Day Total\nFuel") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DAY_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    String arg = rc.getArg();
                    EventData ed = (EventData)obj;
                    double dist = ed.getDayDistanceKM(); // kilometers
                    if (dist > 0) {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    } else {
                        return EventDataLayout.formatKM(dist, arg, rd);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                    return i18n.getString("EventDataLayout.dayDistance","Day\nDistance") + "\n${distanceUnits}";
                }
            });

            // General Event field
            this.addColumnTemplate(new DataColumnTemplate(DATA_EVENT_FIELD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object obj) {
                    EventData ed = (EventData)obj;
                    String fldName = rc.getArg();
                    DBField edFld = EventData.getFactory().getField(fldName);
                    if (edFld != null) {
                        Object val = ed.getFieldValue(fldName);
                        return StringTools.trim(edFld.formatValue(val));
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale  = rd.getLocale();
                    String fldName = rc.getArg();
                    DBField  edFld = EventData.getFactory().getField(fldName);
                    if (edFld != null) {
                        String title = edFld.getTitle(locale);
                        return rc.getTitle(locale, title);
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(EventDataLayout.class);
                        String title = i18n.getString("EventDataLayout.eventField","Event\nField");
                        return rc.getTitle(locale, title);
                    }
                }
            });

        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static int getElapsedFormat(String arg, int dft)
    {

        /* blank */
        if (StringTools.isBlank(arg)) {
            return dft;
        }

        /* numeric formats */
        if (arg.startsWith("-1")) {
            return StringTools.ELAPSED_FORMAT_SS;     // SSSSS
        } else
        if (arg.startsWith("0")) {
            return StringTools.ELAPSED_FORMAT_HHMMSS; // HH:MM:SS
        } else
        if (arg.startsWith("1")) {
            return StringTools.ELAPSED_FORMAT_HHMM  ; // HH:MM
        } else
        if (arg.startsWith("2")) {
            return StringTools.ELAPSED_FORMAT_HHHhh ; // HHH.hh
        } else
        if (arg.startsWith("3")) {
            return StringTools.ELAPSED_FORMAT_HHHh;   // HHH.h
        }

        /* String formats */
        arg = arg.toLowerCase();
        if (arg.equals("ss")     || arg.equals("s")     || arg.startsWith("sec" )) {
            return StringTools.ELAPSED_FORMAT_SS    ;
        } else
        if (arg.equals("hhmmss") || arg.equals("hms"  ) || arg.equals("hh:mm:ss")) {
            return StringTools.ELAPSED_FORMAT_HHMMSS;
        } else
        if (arg.equals("hhmm"  ) || arg.equals("hm"   ) || arg.equals("hh:mm"   )) {
            return StringTools.ELAPSED_FORMAT_HHMM  ;
        } else
        if (arg.equals("hhh.hh") || arg.equals("hh.hh") || arg.equals("h.hh"    )) {
            return StringTools.ELAPSED_FORMAT_HHHhh ;
        } else
        if (arg.equals("hhh.h" ) || arg.equals("hh.h" ) || arg.equals("h.h"     )) {
            return StringTools.ELAPSED_FORMAT_HHHh  ;
        }

        /* else return default */
        return dft;

    }

    protected static String formatElapsedTime(long elapsedSec, int fmt)
    {
        return StringTools.formatElapsedSeconds(elapsedSec, fmt);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
