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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/02  Martin D. Flynn
//     -Added field formatting support for CSV output
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -Added new 'FLD_address' field
//     -Added 'FLD_thermoAverage#' fields.
//  2007/02/26  Martin D. Flynn
//     -Added 'FLD_odometerKM' table column ('FLD_distanceKM' is used for
//      'tripometer' purposes).
//  2007/02/28  Martin D. Flynn
//     -Added column 'FLD_horzAccuracy' (meters)
//     -Removed columns FLD_geofenceID2, FLD_thermoAverage#, & FLD_topSpeedKPH.  
//      For specific custom solutions these can easily be added back, but for a  
//      general solution they are not necessary.
//  2007/03/11  Martin D. Flynn
//     -Added convenience methods 'getSpeedString' and 'getHeadingString'.
//     -Added 'statusCodes[]' and 'additionalSelect' arguments to 'getRangeEvents'
//      method.
//  2007/03/25  Martin D. Flynn
//     -Changed FLD_geofenceID1 to FLD_geozoneIndex, and added FLD_geozoneID
//     -Moved to 'org.opengts.db.tables'
//  2007/05/06  Martin D. Flynn
//     -Added 'FLD_creationTime' column support.
//  2007/06/13  Martin D. Flynn
//     -Added 'FLD_subdivision' column support (state/province/etc).
//  2007/07/14  Martin D. Flynn
//     -Added various optional fields/columns
//  2007/07/27  Martin D. Flynn
//     -Added custom/optional column 'FLD_driver'
//  2007/09/16  Martin D. Flynn
//     -Added 'getFieldValueString' method to return a formatted String 
//      representation of the specified field.
//     -Integrated DBSelect
//  2007/11/28  Martin D. Flynn
//     -Added columns FLD_brakeGForce, FLD_city, FLD_postalCode
//     -"getTimestampString()" now returns a time based on the Account TimeZone.
//     -Apply 'Departed' geozone description for STATUS_GEOFENCE_DEPART events.
//  2007/01/10  Martin D. Flynn
//     -Added method 'countRangeEvents(...)' to return the number of events matching  
//      the specified criteria.
//  2008/02/04  Martin D. Flynn
//     -Added custom/optional column 'FLD_fuelTotal', 'FLD_fuelIdle', 'FLD_engineRpm'
//  2008/02/17  Martin D. Flynn
//     -Added column 'FLD_inputMask'
//  2008/02/21  Martin D. Flynn
//     -Moved J1708/J1587 encoding/decoding to 'org.opengts.dbtools.DTOBDFault'
//  2008/03/12  Martin D. Flynn
//     -Added additional date/time key values for 'getFieldValueString' method.
//  2008/03/28  Martin D. Flynn
//     -Incorporate "DBRecord.select(DBSelect,...) method
//  2008/04/11  Martin D. Flynn
//     -Added status code icon index lookup to "getMapIconIndex(...)"
//  2008/05/14  Martin D. Flynn
//     -Added FLD_country, FLD_stateProvince, FLD_streetAddress
//  2008/05/20  Martin D. Flynn
//     -Added message to assist in determining reason for lack of ReverseGeocoding
//  2008/06/20  Martin D. Flynn
//     -Moved custom field initialization to StartupInit.
//     -EventData record now ignores invalid field references (ie. no displayed errors).
//  2008/07/08  Martin D. Flynn
//     -Added field FLD_costCenter to 'CustomFieldInfo' group.
//     -Rearranged fields/columns to reduce the size of the basic record structure.
//  2008/09/12  Martin D. Flynn
//     -Added field/column FLD_satelliteCount, FLD_batteryLevel
//  2008/10/16  Martin D. Flynn
//     -Modified "getDefaultMapIconIndex" to use the 'iconKeys' table to look up the
//      custom icon index.
//  2008/12/01  Martin D. Flynn
//     -'getDefaultMapIconIndex' now returns Device pushpinID for fleet maps.
//     -Added KEY_HEADING to 'getFieldValueString(...)' support.
//  2009/02/20  Martin D. Flynn
//     -Added field FLD_vertAccuracy
//  2009/05/01  Martin D. Flynn
//     -Added fields FLD_speedLimitKPH, FLD_isTollRoad
//  2009/07/01  Martin D. Flynn
//     -Renamed "getMapIconIndex(...)" to "getPushpinIconIndex(...)"
//  2009/11/01  Martin D. Flynn
//     -Changed 'FLD_driver' to 'FLD_driverID', and 'FLD_entity' to 'FLD_entityID'
//  2009/12/16  Martin D. Flynn
//     -Added field FLD_driverMessage, FLD_jobNumber to 'CustomFieldInfo' group.
//  2010/01/29  Martin D. Flynn
//     -Added field FLD_oilPressure, FLD_ptoEngaged to 'CANBUSFieldInfo' group.
//  2010/04/11  Martin D. Flynn
//     -Modified "DeviceDescriptionComparator" to a case-insensitive sort
//     -Added FLD_fuelPTO
//  2010/09/09  Martin D. Flynn
//     -Custom 'Device' pushpins now override statusCode custom pushpins
//     -Removed FLD_topSpeedKPH
//     -Added FLD_ambientTemp, FLD_barometer, FLD_rfidTag, FLD_oilTemp
//  2010/10/25  Martin D. Flynn
//     -Added FLD_appliedPressure, FLD_sampleIndex, FLD_sampleID
//  2010/11/29  Martin D. Flynn
//     -Moved FLD_appliedPressure to WorkOrderSample
//  2011/01/28  Martin D. Flynn
//     -Fixed "getNextEventData" (bug filed on SourceForge)
//     -Fixed "setInputMask" to mask value to 32-bits
//     -Added FLD_intakeTemp, FLD_throttlePos, FLD_airPressure
//  2011/03/08  Martin D. Flynn
//     -Added FLD_driverStatus
//     -Added optional "GarminFieldInfo" fields
//  2011/04/01  Martin D. Flynn
//     -Truncate "address" data length to specified table column length
//  2011/05/13  Martin D. Flynn
//     -Added FLD_cabinTemp, FLD_airFilterPressure, FLD_engineTorque
//  2011/06/16  Martin D. Flynn
//     -Truncate "streetAddress", "stateProvince" to table column length
//     -Added FLD_fuelPressure, FLD_cellTowerID, FLD_locationAreaCode
//     -Added method "reloadAddress" to reload address columns
//  2011/07/01  Martin D. Flynn
//     -Added the field/column descriptions to the LocalStrings tables.
//     -Minor changes to the order of checking criteria for choosing a pushpin.
//     -Added FLD_batteryVolts
//     -Bounds check "gpsAge" (make sure it is >= 0)
//  2011/08/21  Martin D. Flynn
//     -Added FLD_tirePressure, FLD_tireTemp to "CANBUSFieldInfo" fields
//  2011/10/03  Martin D. Flynn
//     -Added FLD_turboPressure
//  2011/12/06  Martin D. Flynn
//     -Completed KEY_DRIVER implementation in "getFieldValueString"
//  2012/02/03  Martin D. Flynn
//     -Added support for reverse-geocoding cell-tower location, if actual GPS
//      location is not available.
//     -Feature added in DBField to ignore missing column errors:
//          db.ignoreColumnError.EventData.malfunctionLamp=true
//     -Added "temperature" to "getFieldValueString(...)"
//  2012/04/03  Martin D. Flynn
//     -Added support for command-line display of Events-per-second
//      (see ARG_EVENTS_PER_SECOND)
//     -Change "deleteOldEvents" to save last event, if it is within the
//      deletion time range.
//     -Added FLD_massAirFlowRate, FLD_fuelEngineOn
//     -Renamed "getFieldValueString" to "getKeyFieldValue", and added title support.
//  2012/04/27  Martin D. Flynn
//     -Fixed NPE in "_getKeyFieldString"
//  2012/08/01  Martin D. Flynn
//     -Added "${vehicleVolts} to EventData message replacement variables.
//  2012/09/02  Martin D. Flynn
//     -Added field FLD_odometerOffsetKM, FLD_gpsFixStatus
//  2012/10/16  Martin D. Flynn
//     -Added FLD_entityType
//  2013/03/01  Martin D. Flynn
//     -Added FLD_accelerometerXYZ
//     -Added "isInputMaskExplicitlySet()" to allow Device "insertEventData" to update
//      the event "inputMask" if unset by the DCS.
//  2013/04/08  Martin D. Flynn
//     -Added FLD_seatbeltMask
//  2013/05/28  Martin D. Flynn
//     -Added KEY_TIMEZONE
//  2013/09/20  Martin D. Flynn
//     -Added field length check when setting FLD_postalCode.
//  2014/03/03  Martin D. Flynn
//     -Made some adjustments to display warnings when unable to count InnoDB events.
//     -Added PROP_EventData_allowInnoDBCountWithWhere to allow "count(*)" with EventData
//     -Added KEY_DRIVER_PHONE [B28]
//  2014/05/05  Martin D. Flynn
//     -Added FLD_tripPtoHours, FLD_tripBrakeCount, FLD_tripClutchCount
//  2014/06/29  Martin D. Flynn
//     -Added FLD_ptoDistanceKM, FLD_workDistanceKM
//     -Changed "KEY_BATTERY_LEVEL" to adjust battery-level to proper percent.
//     -Added KEY_CREATE_DATETIME, KEY_CREATE_AGE
//  2014/09/16  Martin D. Flynn
//     -Fixed Device pushpin lookup on DeviceMap [see "2.5.7-B21"]
//      To change to previous behavior, set "trackMap.lastDevicePushpin.device=true"
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.geocoder.*;
import org.opengts.cellid.*;

import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;

import org.opengts.cellid.CellTower;

public class EventData
    extends DeviceRecord<EventData>
    implements EventDataProvider, GeoPointProvider
{

    // ------------------------------------------------------------------------

    /* static initializers */
    static {
        EventData.getDeviceDescriptionComparator();
    }

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String OPTCOLS_AddressFieldInfo             = "startupInit.EventData.AddressFieldInfo";
    public static final String OPTCOLS_GPSFieldInfo                 = "startupInit.EventData.GPSFieldInfo";
    public static final String OPTCOLS_CustomFieldInfo              = "startupInit.EventData.CustomFieldInfo";
    public static final String OPTCOLS_GarminFieldInfo              = "startupInit.EventData.GarminFieldInfo";
    public static final String OPTCOLS_CANBUSFieldInfo              = "startupInit.EventData.CANBUSFieldInfo";
    public static final String OPTCOLS_AtmosphereFieldInfo          = "startupInit.EventData.AtmosphereFieldInfo";
    public static final String OPTCOLS_ThermoFieldInfo              = "startupInit.EventData.ThermoFieldInfo";
    public static final String OPTCOLS_AnalogFieldInfo              = "startupInit.EventData.AnalogFieldInfo";
    public static final String OPTCOLS_AutoIncrementIndex           = "startupInit.EventData.AutoIncrementIndex";
    public static final String OPTCOLS_TripSummary                  = "startupInit.EventData.TripSummary";
    public static final String OPTCOLS_EndOfDaySummary              = "startupInit.EventData.EndOfDaySummary";
    public static final String OPTCOLS_ServingCellTowerData         = "startupInit.EventData.ServingCellTowerData";
    public static final String OPTCOLS_NeighborCellTowerData        = "startupInit.EventData.NeighborCellTowerData";
    public static final String OPTCOLS_WorkZoneGridData             = "startupInit.EventData.WorkZoneGridData";
    public static final String OPTCOLS_LeaseRentalData              = "startupInit.EventData.LeaseRentalData";
    public static final String OPTCOLS_CreationTimeMillisecond      = "startupInit.EventData.CreationTimeMillisecond";
    
    // ------------------------------------------------------------------------

    public enum LimitType {
        FIRST,
        LAST
    };

    // ------------------------------------------------------------------------

    public  static final double  INVALID_TEMPERATURE    = -9999.0;
    public  static final double  TEMPERATURE_LIMIT_LO   = -273.15;   // degrees C (Kelvin)
    public  static final double  TEMPERATURE_LIMIT_HI   = 200.0;     // degrees C

    // ------------------------------------------------------------------------
    // standard map icons (see "getPushpinIconIndex")

    /* color pushpins */
    // This represents the standard color "position" for the first 10 pushpin icons
    // The ordering is established in the method "PushpinIcon._DefaultPushpinIconMap()"
    public  static final int     ICON_PUSHPIN_BLACK     = 0;
    public  static final int     ICON_PUSHPIN_BROWN     = 1;
    public  static final int     ICON_PUSHPIN_RED       = 2;
    public  static final int     ICON_PUSHPIN_ORANGE    = 3;
    public  static final int     ICON_PUSHPIN_YELLOW    = 4;
    public  static final int     ICON_PUSHPIN_GREEN     = 5;
    public  static final int     ICON_PUSHPIN_BLUE      = 6;
    public  static final int     ICON_PUSHPIN_PURPLE    = 7;
    public  static final int     ICON_PUSHPIN_GRAY      = 8;
    public  static final int     ICON_PUSHPIN_WHITE     = 9;
 
    public static int _getPushpinIconIndex(String val, OrderedSet<String> iconKeys, int dft)
    {

        if (val == null) { 
            // skip to default below
        } else
        if (iconKeys != null) {
            int ndx = iconKeys.indexOf(val);
            if (ndx >= 0) {
                return ndx;
            }
            // skip to default below
        } else {
            // 'iconKeys' should not be null, however, if it is, this will return the index
            // of the standard colors.
            if (val.equalsIgnoreCase("black" )) { return ICON_PUSHPIN_BLACK ; }
            if (val.equalsIgnoreCase("brown" )) { return ICON_PUSHPIN_BROWN ; }
            if (val.equalsIgnoreCase("red"   )) { return ICON_PUSHPIN_RED   ; }
            if (val.equalsIgnoreCase("orange")) { return ICON_PUSHPIN_ORANGE; }
            if (val.equalsIgnoreCase("yellow")) { return ICON_PUSHPIN_YELLOW; }
            if (val.equalsIgnoreCase("green" )) { return ICON_PUSHPIN_GREEN ; }
            if (val.equalsIgnoreCase("blue"  )) { return ICON_PUSHPIN_BLUE  ; }
            if (val.equalsIgnoreCase("purple")) { return ICON_PUSHPIN_PURPLE; }
            if (val.equalsIgnoreCase("gray"  )) { return ICON_PUSHPIN_GRAY  ; }
            if (val.equalsIgnoreCase("white" )) { return ICON_PUSHPIN_WHITE ; }
            // skip to default below
        }
        
        /* return default */
        //Print.logInfo("Returning default pushpin: " + dft);
        return dft;
        
    }

    // ------------------------------------------------------------------------
    // GPS fix type

    public enum GPSFixType implements EnumTools.StringLocale, EnumTools.IntValue {
        UNKNOWN             (0, I18N.getString(EventData.class,"EventData.gpsFix.unknown", "Unknown")),
        NONE                (1, I18N.getString(EventData.class,"EventData.gpsFix.none"   , "None"   )),
        n2D                 (2, I18N.getString(EventData.class,"EventData.gpsFix.2D"     , "2D"     )),
        n3D                 (3, I18N.getString(EventData.class,"EventData.gpsFix.3D"     , "3D"     ));
        private int         vv = 0;
        private I18N.Text   aa = null;
        GPSFixType(int v, I18N.Text a)          { vv=v; aa=a; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString(); }
        public String  toString(Locale loc)     { return aa.toString(loc); }
    }

    public static GPSFixType getGPSFixType(EventData e)
    {
        return (e != null)? EnumTools.getValueOf(GPSFixType.class,e.getGpsFixType()) : EnumTools.getDefault(GPSFixType.class);
    }

    // ------------------------------------------------------------------------

    public static final EventData   EMPTY_ARRAY[]       = new EventData[0];

    public static       int         AddressColumnLength = -1;
    public static       int         StreetColumnLength  = -1;
    public static       int         CityColumnLength    = -1;
    public static       int         StateColumnLength   = -1;
    public static       int         PostalColumnLength  = -1;

    // ------------------------------------------------------------------------

    private static final String  ALTKEY_pushkey         = "pushkey";
    private static final String  ALTKEY_adtkey          = "adtkey";

    public  static final String  ALTKEY_eq_pushkey      = "altkey=" + ALTKEY_pushkey;

    //public  static boolean ENABLE_DATA_PUSH()
    //{
    //    return RTConfig.getBoolean(OPTCOLS_CreationTimeMillisecond, DBConfig.hasOptPackage());
    //}

    /**
    *** Returns "adtkey" if "EventData.keyedAccountDeviceTime" property is true
    *** @return "adtkey" if "EventData.keyedAccountDeviceTime" property is true
    **/
    private static String ADT_adtkey()
    {
        // EventData.keyedAccountDeviceTime=true
        boolean dftVal = false;
        return RTConfig.getBoolean(DBConfig.PROP_EventData_keyedAccountDeviceTime,false)?ALTKEY_adtkey:"";
    }

    /**
    *** Returns the configured "altkey=" value for the AccountID key field
    **/
    private static String Altkey_AccountID()
    {
        // was: (ENABLE_DATA_PUSH()?ALTKEY_eq_pushkey:"")
        StringBuffer AK = new StringBuffer();
        // "adtkey"
        String adtkey = ADT_adtkey();
        if (!StringTools.isBlank(adtkey)) {
            if (AK.length() > 0) { AK.append(","); }
            AK.append(adtkey);
        }
        // "pushkey"
        String pushkey = RTConfig.getBoolean(OPTCOLS_CreationTimeMillisecond,false)?ALTKEY_pushkey:"";;
        if (!StringTools.isBlank(pushkey)) {
            if (AK.length() > 0) { AK.append(","); }
            AK.append(pushkey);
        }
        // return
        if (AK.length() <= 0) {
            return "";
        } else {
            return AK.insert(0,"altkey=").toString();
        }
    }

    /**
    *** Returns the configured "altkey=" value for the DeviceID key field
    **/
    private static String Altkey_DeviceID()
    {
        return Altkey_AccountID();
    }

    /**
    *** Returns the configured "altkey=" value for the Timestamp key field
    **/
    private static String Altkey_Timestamp()
    {
        String adtkey = ADT_adtkey();
        return !StringTools.isBlank(adtkey)? ("altkey="+adtkey) : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "EventData";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* pseudo field definition */
    // currently only used by EventUtil
    public static final String PFLD_deviceDesc          = DBRecord.PSEUDO_FIELD_CHAR + "deviceDesc";
    public static final String PFLD_Account_            = DBRecord.PSEUDO_FIELD_CHAR + "Account.";
    public static final String PFLD_Device_             = DBRecord.PSEUDO_FIELD_CHAR + "Device.";

    /* field definition */
    // Standard fields
    public static final String FLD_timestamp            = "timestamp";              // Unix Epoch time
    public static final String FLD_statusCode           = "statusCode";
    public static final String FLD_latitude             = "latitude";
    public static final String FLD_longitude            = "longitude";
    public static final String FLD_gpsAge               = "gpsAge";                 // fix age (seconds)
    public static final String FLD_speedKPH             = "speedKPH";
    public static final String FLD_heading              = "heading";
    public static final String FLD_altitude             = "altitude";               // meters
    public static final String FLD_transportID          = Transport.FLD_transportID;
    public static final String FLD_inputMask            = "inputMask";              // bitmask
    public static final String FLD_outputMask           = "outputMask";             // bitmask
    public static final String FLD_seatbeltMask         = "seatbeltMask";           // bitmask
    // Address fields
    public static final String FLD_address              = "address";                // custom or reverse-geocoded address
    // Misc fields
    public static final String FLD_dataSource           = "dataSource";             // gprs, satellite, etc.
    public static final String FLD_rawData              = "rawData";                // optional
    public static final String FLD_distanceKM           = "distanceKM";             // tripometer
    public static final String FLD_odometerKM           = "odometerKM";             // vehicle odometer
    public static final String FLD_odometerOffsetKM     = "odometerOffsetKM";       // offset to reported odometer at time of event
    public static final String FLD_geozoneIndex         = "geozoneIndex";           // Geozone Index
    public static final String FLD_geozoneID            = Geozone.FLD_geozoneID;    // Geozone ID
    private static final DBField StandardFieldInfo[] = {
        // Key fields
        newField_accountID(true,Altkey_AccountID()),
        newField_deviceID(true,Altkey_DeviceID()),
        new DBField(FLD_timestamp        , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.timestamp"          , "Timestamp"             ), "key=true " + Altkey_Timestamp()),
        new DBField(FLD_statusCode       , Integer.TYPE  , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.statusCode"         , "Status Code"           ), "key=true editor=statusCode format=X2"),
        // Standard fields
        new DBField(FLD_latitude         , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.latitude"           , "Latitude"              ), "format=#0.00000"),
        new DBField(FLD_longitude        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.longitude"          , "Longitude"             ), "format=#0.00000"),
        new DBField(FLD_gpsAge           , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.gpsAge"             , "GPS Fix Age"           ), ""),
        new DBField(FLD_speedKPH         , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.speedKPH"           , "Speed"                 ), "format=#0.0 units=speed"),
        new DBField(FLD_heading          , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.heading"            , "Heading"               ), "format=#0.0"),
        new DBField(FLD_altitude         , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.altitude"           , "Altitude"              ), "format=#0.0"),
        new DBField(FLD_transportID      , String.class  , DBField.TYPE_XPORT_ID()  , I18N.getString(EventData.class,"EventData.fld.transportID"        , "Transport ID"          ), ""),
        new DBField(FLD_inputMask        , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.inputMask"          , "Input Mask"            ), "format=X4"),
        new DBField(FLD_outputMask       , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.outputMask"         , "Output Mask"           ), "format=X4"),
        new DBField(FLD_seatbeltMask     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.seatbeltMask"       , "Seatbelt Mask"         ), "format=X4"),
        // Address fields
        new DBField(FLD_address          , String.class  , DBField.TYPE_ADDRESS()   , I18N.getString(EventData.class,"EventData.fld.address"            , "Full Address"          ), "utf8=true"),
        // Misc fields
        new DBField(FLD_dataSource       , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.dataSource"         , "Data Source"           ), ""),
        new DBField(FLD_rawData          , String.class  , DBField.TYPE_TEXT        , I18N.getString(EventData.class,"EventData.fld.rawData"            , "Raw Data"              ), ""),
        new DBField(FLD_distanceKM       , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.distanceKM"         , "Distance KM"           ), "format=#0.0 units=distance"),
        new DBField(FLD_odometerKM       , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.odometerOffsetKM"   , "Odometer Offset KM"    ), "format=#0.0 units=distance"),
        new DBField(FLD_odometerOffsetKM , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.odometerKM"         , "Odometer KM"           ), "format=#0.0 units=distance"),
        new DBField(FLD_geozoneIndex     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.geozoneIndex"       , "Geozone Index"         ), ""),
        new DBField(FLD_geozoneID        , String.class  , DBField.TYPE_ZONE_ID()   , I18N.getString(EventData.class,"EventData.fld.geozoneID"          , "Geozone ID"            ), ""),
        // Common fields
        newField_creationTime(RTConfig.getBoolean(DBConfig.PROP_EventData_keyedCreationTime,false)),
    };
    
    // Extra Address fields 
    // startupInit.EventData.AddressFieldInfo=true
    public static final String FLD_streetAddress        = "streetAddress";          // reverse-geocoded street address
    public static final String FLD_city                 = "city";                   // reverse-geocoded city
    public static final String FLD_stateProvince        = "stateProvince";          // reverse-geocoded state
    public static final String FLD_postalCode           = "postalCode";             // reverse-geocoded postal code
    public static final String FLD_country              = "country";                // reverse-geocoded country
    public static final String FLD_subdivision          = "subdivision";            // reverse-geocoded subdivision (ie "US/CA")
    public static final String FLD_speedLimitKPH        = "speedLimitKPH";          // reverse-geocoded speed-limit ('0' for unavailable)
    public static final String FLD_isTollRoad           = "isTollRoad";             // reverse-geocoded toll-road indicator
    public static final DBField AddressFieldInfo[] = {
        new DBField(FLD_streetAddress    , String.class  , DBField.TYPE_STRING(90)  , I18N.getString(EventData.class,"EventData.fld.streetAddress"      , "Street Address"        ), "utf8=true"),
        new DBField(FLD_city             , String.class  , DBField.TYPE_STRING(40)  , I18N.getString(EventData.class,"EventData.fld.city"               , "City"                  ), "utf8=true"),
        new DBField(FLD_stateProvince    , String.class  , DBField.TYPE_STRING(40)  , I18N.getString(EventData.class,"EventData.fld.stateProvince"      , "State/Privince"        ), "utf8=true"), 
        new DBField(FLD_postalCode       , String.class  , DBField.TYPE_STRING(16)  , I18N.getString(EventData.class,"EventData.fld.postalCode"         , "Postal Code"           ), "utf8=true"),
        new DBField(FLD_country          , String.class  , DBField.TYPE_STRING(40)  , I18N.getString(EventData.class,"EventData.fld.country"            , "Country"               ), "utf8=true"), 
        new DBField(FLD_subdivision      , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.subdivision"        , "Subdivision"           ), "utf8=true"),
        new DBField(FLD_speedLimitKPH    , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.speedLimitKPH"      , "Speed Limit"           ), "format=#0.0 units=speed"),
        new DBField(FLD_isTollRoad       , Boolean.TYPE  , DBField.TYPE_BOOLEAN     , I18N.getString(EventData.class,"EventData.fld.isTollRoad"         , "Toll Road"             ), ""),
    };

    // Device/Modem/GPS fields
    // startupInit.EventData.GPSFieldInfo=true
    public static final String FLD_gpsFixType           = "gpsFixType";             // fix type (0/1=None, 2=2D, 3=3D)
    public static final String FLD_gpsFixStatus         = "gpsFixStatus";           // fix status (bitmask interpreted by the specific DCS)
    public static final String FLD_horzAccuracy         = "horzAccuracy";           // horizontal accuracy (meters)
    public static final String FLD_vertAccuracy         = "vertAccuracy";           // vertical accuracy (meters)
    public static final String FLD_HDOP                 = "HDOP";                   // HDOP
    public static final String FLD_satelliteCount       = "satelliteCount";         // number of satellites
    public static final String FLD_batteryLevel         = "batteryLevel";           // battery level %
    public static final String FLD_batteryVolts         = "batteryVolts";           // battery volts
    public static final String FLD_batteryTemp          = "batteryTemp";            // battery temperature-C
    public static final String FLD_signalStrength       = "signalStrength";         // signal strength (RSSI)
    public static final DBField GPSFieldInfo[] = {
        new DBField(FLD_gpsFixType       , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.gpsFixType"         , "GPS Fix Type"          ), "enum=EventData$GPSFixType"),
        new DBField(FLD_gpsFixStatus     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.gpsFixStatus"       , "GPS Fix Status"        ), ""),
        new DBField(FLD_horzAccuracy     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.horzAccuracy"       , "Horizontal Accuracy"   ), "format=#0.0"),
        new DBField(FLD_vertAccuracy     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.vertAccuracy"       , "Vertical Accuracy"     ), "format=#0.0"),
        new DBField(FLD_HDOP             , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.HDOP"               , "HDOP"                  ), "format=#0.0"),
        new DBField(FLD_satelliteCount   , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.satelliteCount"     , "Number of Satellites"  ), ""),
        new DBField(FLD_batteryLevel     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.batteryLevel"       , "Battery Level %"       ), "format=#0.0 units=percent"),
        new DBField(FLD_batteryVolts     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.batteryVolts"       , "Battery Volts"         ), "format=#0.0"),
        new DBField(FLD_batteryTemp      , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.batteryTempC"       , "Battery Temperature C" ), "format=#0.0 units=temp"),
        new DBField(FLD_signalStrength   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.signalStrength"     , "Signal Strength (RSSI)"), "format=#0.0"),
    };

    // Misc custom fields
    // startupInit.EventData.CustomFieldInfo=true
    public static final String FLD_priority             = "priority";               // priority
    public static final String FLD_entityID             = "entityID";               // entity id
    public static final String FLD_entityType           = "entityType";             // entity type
    public static final String FLD_driverID             = "driverID";               // user/driver
    public static final String FLD_driverStatus         = "driverStatus";           // driver status
    public static final String FLD_driverMessage        = "driverMessage";          // driver message
    public static final String FLD_emailRecipient       = "emailRecipient";         // recipient email address(es)
    public static final String FLD_sensorLow            = "sensorLow";              // digital analog
    public static final String FLD_sensorHigh           = "sensorHigh";             // digital analog
    public static final String FLD_costCenter           = "costCenter";             // associated cost center
    public static final String FLD_jobNumber            = "jobNumber";              // associated job number
    public static final String FLD_rfidTag              = "rfidTag";                // RFID/BarCode Tag (iButton ID)
    public static final String FLD_attachType           = "attachType";             // event attachment type (image:jpeg,gif,png,etc)
    public static final String FLD_attachData           = "attachData";             // event attachment data (image, etc)
    public static final DBField CustomFieldInfo[] = {
        // (may be externally accessed by DBConfig.DBInitialization)
        // Custom fields (may also need to be supported by "org.opengts.servers.gtsdmtp.DeviceDBImpl")
        new DBField(FLD_priority         , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.priority"           , "Priority"              ), ""),
        new DBField(FLD_entityID         , String.class  , DBField.TYPE_ENTITY_ID() , I18N.getString(EventData.class,"EventData.fld.entityID"           , "Entity ID"             ), "utf8=true"),
        new DBField(FLD_entityType       , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.entityType"         , "Entity Type"           ), "enum=EntityManager$EntityType"),
        new DBField(FLD_driverID         , String.class  , DBField.TYPE_DRIVER_ID() , I18N.getString(EventData.class,"EventData.fld.driverID"           , "Driver/User"           ), "utf8=true"),
        new DBField(FLD_driverStatus     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.driverStatus"       , "Driver Status"         ), ""),
        new DBField(FLD_driverMessage    , String.class  , DBField.TYPE_STRING(200) , I18N.getString(EventData.class,"EventData.fld.driverMessage"      , "Driver Message"        ), "utf8=true"),
      //new DBField(FLD_emailRecipient   , String.class  , DBField.TYPE_STRING(200) , I18N.getString(EventData.class,"EventData.fld.emailRecipient"     , "EMail Recipients"      ), ""),
        new DBField(FLD_sensorLow        , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.sensorLow"          , "Sensor Low"            ), "format=X4"),
        new DBField(FLD_sensorHigh       , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.sensorHigh"         , "Sensor High"           ), "format=X4"),
        new DBField(FLD_costCenter       , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.costCenter"         , "Cost Center"           ), ""),
        new DBField(FLD_jobNumber        , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.jobNumber"          , "Job Number"            ), ""),
        new DBField(FLD_rfidTag          , String.class  , DBField.TYPE_STRING(32)  , I18N.getString(EventData.class,"EventData.fld.rfidTag"            , "RFID/BarCode Tag"      ), ""),
        new DBField(FLD_attachType       , String.class  , DBField.TYPE_STRING(64)  , I18N.getString(EventData.class,"EventData.fld.attachType"         , "Attachment MIME Type"  ), ""),
        new DBField(FLD_attachData       , byte[].class  , DBField.TYPE_BLOB        , I18N.getString(EventData.class,"EventData.fld.attachData"         , "Attachment Data"       ), ""),
    };

    // OBD fields
    // startupInit.EventData.J1708FieldInfo=true (OBSOLETE)
    // startupInit.EventData.CANBUSFieldInfo=true
  //public static final String FLD_obdType              = "obdType";                // OBD type [0,1=J1708, 2=J1939, 3=OBDII]
    public static final String FLD_fuelTotal            = "fuelTotal";              // liters
    public static final String FLD_engineRpm            = "engineRpm";              // rpm
    public static final String FLD_engineHours          = "engineHours";            // hours
    public static final String FLD_engineOnHours        = "engineOnHours";          // hours
    public static final String FLD_engineLoad           = "engineLoad";             // %
    public static final String FLD_engineTorque         = "engineTorque";           // %
    public static final String FLD_idleHours            = "idleHours";              // hours
    public static final String FLD_workHours            = "workHours";              // hours
    public static final String FLD_workDistanceKM       = "workDistanceKM";         // km
    public static final String FLD_transOilTemp         = "transOilTemp";           // C
    public static final String FLD_coolantLevel         = "coolantLevel";           // %
    public static final String FLD_coolantTemp          = "coolantTemp";            // C
    public static final String FLD_intakeTemp           = "intakeTemp";             // C
    public static final String FLD_brakeGForce          = "brakeGForce";            // G (9.80665 m/s/s)
    public static final String FLD_acceleration         = "acceleration";           // m/s/s
    public static final String FLD_accelerometerXYZ     = "accelerometerXYZ";       // X,Y,Z
    public static final String FLD_oilPressure          = "oilPressure";            // kPa
    public static final String FLD_oilLevel             = "oilLevel";               // %
    public static final String FLD_oilTemp              = "oilTemp";                // C
    public static final String FLD_airPressure          = "airPressure";            // kPa
    public static final String FLD_airFilterPressure    = "airFilterPressure";      // kPa
    public static final String FLD_turboPressure        = "turboPressure";          // kPa
    public static final String FLD_ptoEngaged           = "ptoEngaged";             // boolean
    public static final String FLD_ptoHours             = "ptoHours";               // hours
    public static final String FLD_ptoDistanceKM        = "ptoDistanceKM";          // km
    public static final String FLD_throttlePos          = "throttlePos";            // %
    public static final String FLD_brakePos             = "brakePos";               // %
    public static final String FLD_j1708Fault           = "j1708Fault";             // 
    public static final String FLD_faultCode            = "faultCode";              // J1708/J1939/OBDII fault code
    public static final String FLD_malfunctionLamp      = "malfunctionLamp";        // J1939/OBDII 
    // --- miscellaneous fuel
    public static final String FLD_fuelLevel            = "fuelLevel";              // %
    public static final String FLD_fuelLevel2           = "fuelLevel2";             // %
    public static final String FLD_fuelRemain           = "fuelRemain";             // liters
    public static final String FLD_fuelTrip             = "fuelTrip";               // liters
    public static final String FLD_fuelIdle             = "fuelIdle";               // liters
    public static final String FLD_fuelPTO              = "fuelPTO";                // liters
    public static final String FLD_fuelEngineOn         = "fuelEngineOn";           // liters (fuel used since engine on)
    public static final String FLD_vBatteryVolts        = "vBatteryVolts";          // vehicle battery voltage
    public static final String FLD_fuelPressure         = "fuelPressure";           // kPa
    public static final String FLD_fuelUsage            = "fuelUsage";              // liters per hour (fuelRate)
    public static final String FLD_fuelTemp             = "fuelTemp";               // C
    public static final String FLD_fuelEconomy          = "fuelEconomy";            // kilometer per liter (average)
    // --- other OBD fields
    public static final String FLD_brakePressure        = "brakePressure";          // kPa
    public static final String FLD_massAirFlowRate      = "massAirFlowRate";        // g/sec
    // --- tire (tyre) temperature/pressure
    public static final String FLD_tirePressure         = "tirePressure";           // kPa
    public static final String FLD_tireTemp             = "tireTemp";               // C
    // ---
    public static final String FLD_tankLevel            = "tankLevel";              // %
    // ---
    public static final DBField CANBUSFieldInfo[] = { 
        // (may be externally accessed by DBConfig.DBInitialization)
        // Custom fields (may also need to be supported by "org.opengts.servers.gtsdmtp.DeviceDBImpl")
      //new DBField(FLD_obdType          , Integer.TYPE  , DBField.TYPE_UINT16     , I18N.getString(EventData.class,"EventData.fld.obdType"            , "OBD Type"              ), ""),
        new DBField(FLD_fuelPressure     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelPressure"       , "Fuel Pressure"         ), "format=#0.00 units=pressure"),
        new DBField(FLD_fuelUsage        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelUsage"          , "Fuel Usage"            ), "format=#0.00"),
        new DBField(FLD_fuelTemp         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelTemp"           , "Fuel Temp"             ), "format=#0.00 units=temp"),
        new DBField(FLD_fuelLevel        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelLevel"          , "Fuel Level"            ), "format=#0.0 units=percent"),
        new DBField(FLD_fuelLevel2       , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelLevel2"         , "Fuel Level 2"          ), "format=#0.0 units=percent"),
        new DBField(FLD_fuelEconomy      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelEconomy"        , "Fuel Economy"          ), "format=#0.0 units=econ"),
        new DBField(FLD_fuelTotal        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelTotal"          , "Total Fuel Used"       ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelRemain       , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelRemain"         , "Fuel Remaining"        ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelTrip         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelTrip"           , "Trip Fuel Used"        ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelIdle         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelIdle"           , "Idle Fuel Used"        ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelPTO          , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelPTO"            , "PTO Fuel Used"         ), "format=#0.0 units=volume"),
        new DBField(FLD_fuelEngineOn     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.fuelEngineOn"       , "Fuel Since Engine On"  ), "format=#0.0 units=volume"),
        new DBField(FLD_engineRpm        , Long.TYPE     , DBField.TYPE_UINT32     , I18N.getString(EventData.class,"EventData.fld.engineRpm"          , "Engine RPM"            ), ""),
        new DBField(FLD_engineHours      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineHours"        , "Engine Hours"          ), "format=#0.0"),
        new DBField(FLD_engineOnHours    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineOnHours"      , "Engine 'On' Hours"     ), "format=#0.0"),
        new DBField(FLD_engineLoad       , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineLoad"         , "Engine Load"           ), "format=#0.00 units=percent"),
        new DBField(FLD_engineTorque     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.engineTorque"       , "Engine Torque %"       ), "format=#0.00 units=percent"),
        new DBField(FLD_idleHours        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.idleHours"          , "Idle Hours"            ), "format=#0.0"),
        new DBField(FLD_workHours        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.workHours"          , "Work Hours"            ), "format=#0.0"),
        new DBField(FLD_workDistanceKM   , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.workDistanceKM"     , "Work Distance KM"      ), "format=#0.0 units=distance"),
        new DBField(FLD_transOilTemp     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.transOilTemp"       , "Transmission Oil Temp" ), "format=#0.00 units=temp"),
        new DBField(FLD_coolantLevel     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.coolantLevel"       , "Coolant Level"         ), "format=#0.00 units=percent"),
        new DBField(FLD_coolantTemp      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.coolantTemp"        , "Coolant Temperature"   ), "format=#0.00 units=temp"),
        new DBField(FLD_intakeTemp       , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.intakeTemp"         , "Intake Temperature"    ), "format=#0.00 units=temp"),
        new DBField(FLD_brakeGForce      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.brakeGForce"        , "Brake G Force"         ), "format=#0.00"),
        new DBField(FLD_acceleration     , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.acceleration"       , "Acceleration"          ), "format=#0.00"),
        new DBField(FLD_accelerometerXYZ , String.class  , DBField.TYPE_STRING(32) , I18N.getString(EventData.class,"EventData.fld.accelerometerXYZ"   , "Accelerometer XYZ"     ), ""),
        new DBField(FLD_brakePressure    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.brakePressure"      , "Brake Pressure"        ), "format=#0.00 units=pressure"),
        new DBField(FLD_massAirFlowRate  , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.massAirFlowRate"    , "Mass Air Flow Rate"    ), "format=#0.00"), // g/sec
        new DBField(FLD_oilPressure      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.oilPressure"        , "Oil Pressure"          ), "format=#0.00 units=pressure"),
        new DBField(FLD_oilLevel         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.oilLevel"           , "Oil Level"             ), "format=#0.00"),
        new DBField(FLD_oilTemp          , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.oilTemp"            , "Oil Temperature"       ), "format=#0.00 units=temp"),
        new DBField(FLD_airPressure      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.airPressure"        , "Air Supply Pressure"   ), "format=#0.00 units=pressure"),
        new DBField(FLD_airFilterPressure, Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.airFilterPressure"  , "Air Filter Pressure"   ), "format=#0.00 units=pressure"),
        new DBField(FLD_turboPressure    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.turboPressure"      , "Turbo Pressure"        ), "format=#0.00 units=pressure"),
        new DBField(FLD_ptoEngaged       , Boolean.TYPE  , DBField.TYPE_BOOLEAN    , I18N.getString(EventData.class,"EventData.fld.ptoEngaged"         , "PTO Engaged"           ), ""),
        new DBField(FLD_ptoHours         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.ptoHours"           , "PTO Hours"             ), "format=#0.0"),
        new DBField(FLD_ptoDistanceKM    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.ptoDistanceKM"      , "PTO Distance KM"       ), "format=#0.0 units=distance"),
        new DBField(FLD_throttlePos      , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.throttlePos"        , "Throttle Position"     ), "format=#0.0 units=percent"),
        new DBField(FLD_brakePos         , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.brakePos"           , "Brake Position"        ), "format=#0.0 units=percent"),
        new DBField(FLD_vBatteryVolts    , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.vBatteryVolts"      , "Vehicle Battery Volts" ), "format=#0.0"),
        new DBField(FLD_j1708Fault       , Long.TYPE     , DBField.TYPE_UINT64     , I18N.getString(EventData.class,"EventData.fld.j1708Fault"         , "Fault Code"            ), ""),
        new DBField(FLD_faultCode        , String.class  , DBField.TYPE_STRING(96) , I18N.getString(EventData.class,"EventData.fld.faultCode"          , "Fault String"          ), ""),
        new DBField(FLD_malfunctionLamp  , Boolean.TYPE  , DBField.TYPE_BOOLEAN    , I18N.getString(EventData.class,"EventData.fld.malfunctionLamp"    , "Malfunction Lamp"      ), ""),
        new DBField(FLD_tirePressure     , String.class  , DBField.TYPE_STRING(140), I18N.getString(EventData.class,"EventData.fld.tirePressure"       , "Tire Pressure"         ), "format=#0.00 units=pressure"),
        new DBField(FLD_tireTemp         , String.class  , DBField.TYPE_STRING(140), I18N.getString(EventData.class,"EventData.fld.tireTemp"           , "Tire Temperature"      ), "format=#0.00 units=temp"),
        new DBField(FLD_tankLevel        , Double.TYPE   , DBField.TYPE_DOUBLE     , I18N.getString(EventData.class,"EventData.fld.tankLevel"          , "Tank Level"            ), "format=#0.0 units=percent"),
    };

    // Garmin (GFMI) fields
    // startupInit.EventData.GarminFieldInfo=true
    public static final String FLD_etaTimestamp         = "etaTimestamp";           // ETA time
    public static final String FLD_etaUniqueID          = "etaUniqueID";            // ETA ID
    public static final String FLD_etaDistanceKM        = "etaDistanceKM";          // ETA distance
    public static final String FLD_etaLatitude          = "etaLatitude";            // ETA latitude
    public static final String FLD_etaLongitude         = "etaLongitude";           // ETA longitude
    public static final String FLD_stopID               = "stopID";                 // STOP ID
    public static final String FLD_stopStatus           = "stopStatus";             // STOP Status
    public static final String FLD_stopIndex            = "stopIndex";              // STOP Index
    public static final DBField GarminFieldInfo[] = {
        new DBField(FLD_etaTimestamp     , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.etaTimestamp"       , "ETA Time"              ), "format=time"),
        new DBField(FLD_etaUniqueID      , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.etaUniqueID"        , "ETA Unique ID"         ), ""),
        new DBField(FLD_etaDistanceKM    , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.etaDistanceKM"      , "ETA Distance KM"       ), "format=#0.0 units=distance"),
        new DBField(FLD_etaLatitude      , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.etaLatitude"        , "ETA Latitude"          ), "format=#0.00000"),
        new DBField(FLD_etaLongitude     , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.etaLongitude"       , "ETA Longitude"         ), "format=#0.00000"),
        new DBField(FLD_stopID           , Long.TYPE     , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.stopID"             , "STOP ID"               ), ""),
        new DBField(FLD_stopStatus       , Integer.TYPE  , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.stopStatus"         , "STOP Status"           ), ""),
        new DBField(FLD_stopIndex        , Integer.TYPE  , DBField.TYPE_STOP_INDEX(), I18N.getString(EventData.class,"EventData.fld.stopIndex"          , "STOP Index"            ), ""),
    };

    // Atmosphere fields
    // startupInit.EventData.AtmosphereFieldInfo=true
    public static final String FLD_barometer            = "barometer";              // kPa
    public static final String FLD_ambientTemp          = "ambientTemp";            // C
    public static final String FLD_cabinTemp            = "cabinTemp";              // C
    public static final DBField AtmosphereFieldInfo[] = {
        new DBField(FLD_barometer        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.barometer"          , "Barometric Pressure"   ), "format=#0.00 units=pressure"),
        new DBField(FLD_ambientTemp      , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.ambientTemp"        , "Ambient Temperature"   ), "format=#0.0 units=temp"),
        new DBField(FLD_cabinTemp        , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.cabinTemp"          , "Cabin Temperature"     ), "format=#0.0 units=temp"),
    };

    // Temperature fields
    // startupInit.EventData.ThermoFieldInfo=true
    public static final String FLD_thermoAverage0       = "thermoAverage0";         // C
    public static final String FLD_thermoAverage1       = "thermoAverage1";         // C
    public static final String FLD_thermoAverage2       = "thermoAverage2";         // C
    public static final String FLD_thermoAverage3       = "thermoAverage3";         // C
    public static final String FLD_thermoAverage4       = "thermoAverage4";         // C
    public static final String FLD_thermoAverage5       = "thermoAverage5";         // C
    public static final String FLD_thermoAverage6       = "thermoAverage6";         // C
    public static final String FLD_thermoAverage7       = "thermoAverage7";         // C
    public static final DBField ThermoFieldInfo[] = {
        new DBField(FLD_thermoAverage0   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage0"     , "Temperature Average 0" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage1   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage1"     , "Temperature Average 1" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage2   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage2"     , "Temperature Average 2" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage3   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage3"     , "Temperature Average 3" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage4   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage4"     , "Temperature Average 4" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage5   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage5"     , "Temperature Average 5" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage6   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage6"     , "Temperature Average 6" ), "format=#0.0 units=temp"),
        new DBField(FLD_thermoAverage7   , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.thermoAverage7"     , "Temperature Average 7" ), "format=#0.0 units=temp"),
    };

    // Analog sensor fields
    // startupInit.EventData.AnalogFieldInfo=true
    public static final String FLD_analog0              = "analog0";                // 
    public static final String FLD_analog1              = "analog1";                // 
    public static final String FLD_analog2              = "analog2";                // 
    public static final String FLD_analog3              = "analog3";                // 
    public static final String FLD_pulseCount           = "pulseCount";             // # (* gain)
    public static final DBField AnalogFieldInfo[] = {
        new DBField(FLD_pulseCount       , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.pulseCount"         , "Pulse Count/Gain"      ), "format=#0.0"),
        new DBField(FLD_analog0          , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog0"            , "Analog 0"              ), "format=#0.0"),
        new DBField(FLD_analog1          , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog1"            , "Analog 1"              ), "format=#0.0"),
        new DBField(FLD_analog2          , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog2"            , "Analog 2"              ), "format=#0.0"),
        new DBField(FLD_analog3          , Double.TYPE   , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.analog3"            , "Analog 3"              ), "format=#0.0"),
    };

    // GPRS PCell data (still under development)
    // startupInit.EventData.ServingCellTowerData=true
    public static final String FLD_cellTowerID          = "cellTowerID";            // CID Integer
    public static final String FLD_mobileCountryCode    = "mobileCountryCode";      // MCC Integer
    public static final String FLD_mobileNetworkCode    = "mobileNetworkCode";      // MNC Integer
    public static final String FLD_cellTimingAdvance    = "cellTimingAdvance";      // TAV Integer
    public static final String FLD_locationAreaCode     = "locationAreaCode";       // LAC Integer
    public static final String FLD_cellServingInfo      = "cellServingInfo";        // CellTower "-cid=123 -lac=1341 -arfcn=123 -rxlev=123"
    public static final String FLD_cellLatitude         = "cellLatitude";           // Double
    public static final String FLD_cellLongitude        = "cellLongitude";          // Double
    public static final String FLD_cellAccuracy         = "cellAccuracy";           // Double (meters)
    public static final DBField ServingCellTowerData[] = {
        new DBField(FLD_cellTowerID      , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.cellTowerID"        , "Cell Tower ID"         ), ""),
        new DBField(FLD_mobileCountryCode, Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.mobileCountryCode"  , "Mobile Country Code"   ), ""),
        new DBField(FLD_mobileNetworkCode, Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.mobileNetworkCode"  , "Mobile Network Code"   ), ""),
        new DBField(FLD_cellTimingAdvance, Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.cellTimingAdvance"  , "Cell Timing Advance"   ), ""),
        new DBField(FLD_locationAreaCode , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.locationAreaCode"   , "Location Area Code"    ), ""),
        new DBField(FLD_cellServingInfo  , String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellServingInfo"    , "Serving Cell Info"     ), ""),
        new DBField(FLD_cellLatitude     , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.cellLatitude"       , "Cell Tower Latitude"   ), "format=#0.00000"),
        new DBField(FLD_cellLongitude    , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.cellLongitude"      , "Cell Tower Longitude"  ), "format=#0.00000"),
        new DBField(FLD_cellAccuracy     , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.cellAccuracy"       , "Cell GPS Accuracy M"   ), "format=#0.0"),
    };
    // arfcn, tav, lac, cid
    // startupInit.EventData.NeighborCellTowerData=true
    public static final String FLD_cellNeighborInfo0    = "cellNeighborInfo0";  // CellTower
    public static final String FLD_cellNeighborInfo1    = "cellNeighborInfo1";  // CellTower
    public static final String FLD_cellNeighborInfo2    = "cellNeighborInfo2";  // CellTower
    public static final String FLD_cellNeighborInfo3    = "cellNeighborInfo3";  // CellTower
    public static final String FLD_cellNeighborInfo4    = "cellNeighborInfo4";  // CellTower
    public static final String FLD_cellNeighborInfo5    = "cellNeighborInfo5";  // CellTower
    public static final DBField NeighborCellTowerData[] = { 
        new DBField(FLD_cellNeighborInfo0, String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo0"  , "Neighbor Cell Info #0" ), ""),
        new DBField(FLD_cellNeighborInfo1, String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo1"  , "Neighbor Cell Info #1" ), ""),
        new DBField(FLD_cellNeighborInfo2, String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo2"  , "Neighbor Cell Info #2" ), ""),
        new DBField(FLD_cellNeighborInfo3, String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo3"  , "Neighbor Cell Info #3" ), ""),
        new DBField(FLD_cellNeighborInfo4, String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo4"  , "Neighbor Cell Info #4" ), ""),
        new DBField(FLD_cellNeighborInfo5, String.class , DBField.TYPE_STRING(80), I18N.getString(EventData.class,"EventData.fld.cellNeighborInfo5"  , "Neighbor Cell Info #5" ), ""),
    };
    private static final int COUNT_cellNeighborInfo = 6;

    // Vehicle lease/rental (should be in the Device record)
    // startupInit.EventData.LeaseRentalData=true
    public static final String FLD_leaseStartDate       = "leaseStartDate";
    public static final String FLD_leaseEndDate         = "leaseEndDate";
    public static final String FLD_leaseStartOdomKM     = "leaseStartOdomKM";
    public static final String FLD_leaseAllowedDistKM   = "leaseAllowedDistKM";
    public static final String FLD_leaseContractID      = "leaseContractID";
    public static final DBField LeaseRentalData[] = {
        new DBField(FLD_leaseStartDate    , Long.TYPE    , DBField.TYPE_UINT32    , I18N.getString(EventData.class,"EventData.fld.leaseStartDay"      , "Lease Start Date"      ), "format=date"),
        new DBField(FLD_leaseEndDate      , Long.TYPE    , DBField.TYPE_UINT32    , I18N.getString(EventData.class,"EventData.fld.leaseEndDay"        , "Lease End Date"        ), "format=date"),
        new DBField(FLD_leaseStartOdomKM  , Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.leaseStartOdomKM"   , "Lease Start Odom KM"   ), "format=#0.0 units=distance"),
        new DBField(FLD_leaseAllowedDistKM, Double.TYPE  , DBField.TYPE_DOUBLE    , I18N.getString(EventData.class,"EventData.fld.leaseAllowedDistKM" , "Lease Allowed Dist KM" ), "format=#0.0 units=distance"),
        new DBField(FLD_leaseContractID   , String.class , DBField.TYPE_STRING(32), I18N.getString(EventData.class,"EventData.fld.sampleID"           , "Sample ID"             ), ""),
    };

    // WorkZone Grid data (still under development)
    // startupInit.EventData.WorkZoneGridData=true
    public static final String FLD_sampleIndex          = "sampleIndex";            // #
    public static final String FLD_sampleID             = "sampleID";               // #
  //public static final String FLD_appliedPressure      = "appliedPressure";        // kPa
    public static final DBField WorkZoneGridData[] = {
        new DBField(FLD_sampleIndex      , Integer.TYPE , DBField.TYPE_INT32     , I18N.getString(EventData.class,"EventData.fld.sampleIndex"        , "Sample Index"          ), ""),
        new DBField(FLD_sampleID         , String.class , DBField.TYPE_STRING(32), I18N.getString(EventData.class,"EventData.fld.sampleID"           , "Sample ID"             ), ""),
      //new DBField(FLD_appliedPressure  , Double.TYPE  , DBField.TYPE_DOUBLE    , "Applied Pressure"          , "format=#0.00 units=pressure"),
    };

    // Keyed creation time with millisecond resolution  
    // startupInit.EventData.EventPushData=true
    // startupInit.EventData.CreationTimeMillisecond=true
    public static final String FLD_dataPush             = "dataPush";               //
    public static final DBField CreationTimeMillisecond[] = {
        newField_creationMillis(ALTKEY_eq_pushkey),
        new DBField(FLD_dataPush         , Boolean.TYPE , DBField.TYPE_BOOLEAN   , I18N.getString(EventData.class,"EventData.fld.dataPush"           , "Data Push Indicator"   ), ALTKEY_eq_pushkey),
    };

    // Trip summary (ATrack)
    // startupInit.EventData.TripSummary=true
    // tripFuelUsed ==> see FLD_fuelTrip
    public static final String FLD_tripStartTime        = "tripStartTime";    
    public static final String FLD_tripStopTime         = "tripStopTime";    
  //public static final String FLD_tripHours            = "tripHours";
    public static final String FLD_tripDistanceKM       = "tripDistanceKM";
    public static final String FLD_tripIdleHours        = "tripIdleHours";
  //public static final String FLD_tripMovingHours      = "tripMovingHours";
    public static final String FLD_tripPtoHours         = "tripPtoHours";
    public static final String FLD_tripMaxSpeedKPH      = "tripMaxSpeedKPH";
    public static final String FLD_tripMaxRpm           = "tripMaxRpm";
    public static final String FLD_tripStartLatitude    = "tripStartLatitude";
    public static final String FLD_tripStartLongitude   = "tripStartLongitude";
    public static final String FLD_tripElapsedSeconds   = "tripElapsedSeconds";
    public static final String FLD_tripBrakeCount       = "tripBrakeCount";
    public static final String FLD_tripClutchCount      = "tripClutchCount";
    public static final DBField TripSummary[] = {
        new DBField(FLD_tripStartTime     , Long.TYPE    , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.tripStartTime"     , "Trip Start Time"       ), "format=time"),
        new DBField(FLD_tripStopTime      , Long.TYPE    , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.tripStopTime"      , "Trip Stop Time"        ), "format=time"),
        new DBField(FLD_tripDistanceKM    , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripDistanceKM"    , "Trip Distance KM"      ), "format=#0.0 units=distance"),
        new DBField(FLD_tripIdleHours     , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripIdleHours"     , "Trip Idle Hours"       ), "format=#0.0"),
      //new DBField(FLD_tripMovingHours   , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripMovingHours"   , "Trip Moving Hours"     ), "format=#0.0"),
        new DBField(FLD_tripPtoHours      , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripPtoHours"      , "Trip PTO Hours"        ), "format=#0.0"),
        new DBField(FLD_tripMaxSpeedKPH   , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripMaxSpeedKPH"   , "Trip Max Speed"        ), "format=#0.0 units=speed"),
        new DBField(FLD_tripMaxRpm        , Long.TYPE    , DBField.TYPE_UINT32      , I18N.getString(EventData.class,"EventData.fld.tripMaxRPM"        , "Trip Max RPM"          ), ""),
        new DBField(FLD_tripStartLatitude , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripStartLatitude" , "Trip Start Latitude"   ), "format=#0.00000"),
        new DBField(FLD_tripStartLongitude, Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.tripStartLongitude", "Trip Start Longitude"  ), "format=#0.00000"),
        new DBField(FLD_tripElapsedSeconds, Long.TYPE    , DBField.TYPE_INT32       , I18N.getString(EventData.class,"EventData.fld.tripElapsedSeconds", "Trip Elapsed Seconds"  ), ""),
        new DBField(FLD_tripBrakeCount    , Long.TYPE    , DBField.TYPE_INT32       , I18N.getString(EventData.class,"EventData.fld.tripBrakeCount"    , "Trip Brake Presses"    ), ""),
        new DBField(FLD_tripClutchCount   , Long.TYPE    , DBField.TYPE_INT32       , I18N.getString(EventData.class,"EventData.fld.tripClutchCount"   , "Trip Clutch Presses"   ), ""),
    };

    // End-Of-Day summary (Antx)
    // startupInit.EventData.EndOfDaySummary=true
    public static final String FLD_dayEngineStarts      = "dayEngineStarts";    
    public static final String FLD_dayIdleHours         = "dayIdleHours";
    public static final String FLD_dayFuelIdle          = "dayFuelIdle";
    public static final String FLD_dayWorkHours         = "dayWorkHours";
    public static final String FLD_dayFuelWork          = "dayFuelWork";
    public static final String FLD_dayFuelPTO           = "dayFuelPTO";
    public static final String FLD_dayDistanceKM        = "dayDistanceKM";
    public static final String FLD_dayFuelTotal         = "dayFuelTotal";
    public static final DBField EndOfDaySummary[] = {
        new DBField(FLD_dayEngineStarts  , Integer.TYPE , DBField.TYPE_UINT16      , I18N.getString(EventData.class,"EventData.fld.dayEngineStarts"    , "# Engine Starts"       ), ""),
        new DBField(FLD_dayIdleHours     , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayIdleHours"       , "Day Idle Hours"        ), "format=#0.0"),
        new DBField(FLD_dayFuelIdle      , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelIdle"        , "Day Idle Fuel"         ), "format=#0.0 units=volume"),
        new DBField(FLD_dayWorkHours     , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayWorkHours"       , "Day Work Hours"        ), "format=#0.0"),
        new DBField(FLD_dayFuelWork      , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelWork"        , "Day Work Fuel"         ), "format=#0.0 units=volume"),
        new DBField(FLD_dayFuelPTO       , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelPTO"         , "Day PTO Fuel"          ), "format=#0.0 units=volume"),
        new DBField(FLD_dayDistanceKM    , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayDistanceKM"      , "Day Distance KM"       ), "format=#0.0 units=distance"),
        new DBField(FLD_dayFuelTotal     , Double.TYPE  , DBField.TYPE_DOUBLE      , I18N.getString(EventData.class,"EventData.fld.dayFuelTotal"       , "Day Total Fuel"        ), "format=#0.0 units=volume"),
    };

    // Auto increment [
    // startupInit.EventData.AutoIncrementIndex=true
    //  - setting this to 'true' will require rebuilding the entire EventData table
    public static final String FLD_autoIndex            = DBRecordKey.FLD_autoIndex;
    public static final DBField AutoIncrementIndex[] = {
        new DBField(FLD_autoIndex        , Long.TYPE     , DBField.TYPE_INT64       , I18N.getString(EventData.class,"EventData.fld.autoIndex"          , "Auto Increment Index"  ), "key=true auto=true"),
    };

    /* key class */
    public static class Key
        extends DeviceKey<EventData>
    {
        public Key() {
            this.getFieldValues().setIgnoreInvalidFields(true);
        }
        public Key(String acctId, String devId, long timestamp, int statusCode) {
            super.setKeyValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setKeyValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setKeyValue(FLD_timestamp , timestamp);
            super.setKeyValue(FLD_statusCode, statusCode);
            this.getFieldValues().setIgnoreInvalidFields(true);
        }
        public Key(String acctId, String devId, long timestamp, int statusCode, String entity) {
            super.setKeyValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setKeyValue(FLD_deviceID  , ((devId  != null)? devId.toLowerCase()  : ""));
            super.setKeyValue(FLD_timestamp , timestamp);
            super.setKeyValue(FLD_statusCode, statusCode);
            super.setKeyValue(FLD_entityID  , ((entity != null)? entity.toLowerCase() : ""));
            this.getFieldValues().setIgnoreInvalidFields(true);
        }
        public DBFactory<EventData> getFactory() {
            return EventData.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<EventData> factory = null;
    public static DBFactory<EventData> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                EventData.TABLE_NAME(),
                EventData.StandardFieldInfo,
                DBFactory.KeyType.PRIMARY,
                EventData.class, 
                EventData.Key.class,
                false/*editable*/,false/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
          //factory.setLogMissingColumnWarnings(RTConfig.getBoolean(DBConfig.PROP_EventData_logMissingColumns,true));
            // FLD_address max length
            DBField addrFld = factory.getField(FLD_address);
            EventData.AddressColumnLength = (addrFld   != null)? addrFld.getStringLength()   : 0;
            // FLD_streetAddress max length
            DBField streetFld = factory.getField(FLD_streetAddress);
            EventData.StreetColumnLength  = (streetFld != null)? streetFld.getStringLength() : 0;
            // FLD_city max length
            DBField cityFld = factory.getField(FLD_city);
            EventData.CityColumnLength    = (cityFld   != null)? cityFld.getStringLength()   : 0;
            // FLD_stateProvince max length
            DBField stateFld = factory.getField(FLD_stateProvince);
            EventData.StateColumnLength   = (stateFld  != null)? stateFld.getStringLength()  : 0;
            // FLD_postalCode max length
            DBField postalFld = factory.getField(FLD_postalCode);
            EventData.PostalColumnLength  = (postalFld != null)? postalFld.getStringLength() : 0;
            // "COUNT(*)" not allowed if InnoDB
            boolean countOK = RTConfig.getBoolean(DBConfig.PROP_EventData_allowInnoDBCountWithWhere,false);
            factory.setAllowInnoDBCOUNT(countOK);
        }
        return factory;
    }

    /* Bean instance */
    public EventData()
    {
        super();
    }

    /* database record */
    public EventData(EventData.Key key)
    {
        super(key);
        // init?
    }

    // ------------------------------------------------------------------------

    /* copy specified EventData record with new statusCode */
    public static EventData copySynthesizedEvent(EventData evdb, int sc)
    {
        return EventData.copySynthesizedEvent(evdb, sc, 0L);
    }

    /* copy specified EventData record with new statusCode and timestamp */
    public static EventData copySynthesizedEvent(EventData evdb, int sc, long ts)
    {

        /* no event? */
        if ((evdb == null) || (sc <= 0)) { // omit STATUS_NONE, STATUS_IGNORE
            return null;
        }

        /* copy event */
        try {
            String acctID     = evdb.getAccountID();
            String devID      = evdb.getDeviceID();
            long   timestamp  = (ts > 0L)? ts : evdb.getTimestamp();
            int    statCode   = sc;
            EventData.Key evk = new EventData.Key(acctID, devID, timestamp, statCode);
            EventData copyEv  = evk.getDBRecord();
            copyEv.setAllFieldValues(evdb); // copy all non-key fields from original event
            copyEv.setSynthesizedEvent(true);
            return copyEv; // not yet saved
        } catch (DBException dbe) {
            Print.logException("Unable to copy EventData record", dbe);
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(EventData.class, loc);
        return i18n.getString("EventData.description", 
            "This table contains " +
            "events which have been generated by all client devices."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified field.<br>
    *** @param fldName  The field name to retrieve
    *** @return The field value
    **/
    /*
    public Object getValue(String fldName)
    {
        if (fldName == null) {
            return super.getValue(fldName); // pass it on
        } else
        if (fldName.equals(FLD_odometerOffsetKM)) {
            return new Double(this.getOdometerOffsetKM(null));
        } else
        if (fldName.equals("reportedOdometer")) {
            return new Double(this.getOdometerKM() + this.getOdometerOffsetKM(null));
        } else {
            return super.getValue(fldName);
        }
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Common Bean access fields below
    
    // EventDataProvider interface. (defined in DeviceRecord.java)
    // public final String getDeviceVIN() { return super.getDeviceVIN(); }

    // ------------------------------------------------------------------------

    /**
    *** Gets the timestamp of this event in Unix/Epoch time
    *** @return The timestamp of this event
    **/
    public long getTimestamp()
    {
        return this.getFieldValue(FLD_timestamp, 0L);
    }

    /**
    *** Sets the timestamp of this event in Unix/Epoch time
    *** @param v The timestamp of this event
    **/
    public void setTimestamp(long v)
    {
        this.setFieldValue(FLD_timestamp, v);
    }

    /**
    *** Gets the TimeZone of this event
    *** @return The TimeZone for this event (does not return null)
    **/
    public TimeZone getTimeZone()
    {
        Account a = this.getAccount();
        Device  d = this.getDevice();
        if (d != null) {
            return d.getAssignedUserTimeZone(); // non-null
        } else
        if (a != null) {
            return a.getTimeZone(null); // non-null
        } else {
            return DateTime.getGMTTimeZone();
        }
    }

    // --------------------------

    /**
    *** Gets the String representation of the timestamp of this event
    *** @param timestamp  The timestamp
    *** @param account    The account
    *** @param tz         The TimeZone
    *** @param bpl        The BasicPrivateLabel instance
    *** @return The String representation of the timestamp of this event
    **/
    public static String getTimestampString(long timestamp, Account account, TimeZone tz, BasicPrivateLabel bpl)
    {
        Account a      = account;
        String dateFmt = (a != null)? a.getDateFormat() : (bpl != null)? bpl.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (a != null)? a.getTimeFormat() : (bpl != null)? bpl.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();
        TimeZone tmz   = (tz != null)? tz : (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return dt.format(dateFmt + " " + timeFmt + " z");
    }

    /**
    *** Gets the String representation of the timestamp time-of-day of this event
    *** @param timestamp  The timestamp
    *** @param account    The account
    *** @param tz         The TimeZone
    *** @param bpl        The BasicPrivateLabel instance
    *** @return The String representation of the timestamp time-of-day of this event
    **/
    public static String getTimestampTime(long timestamp, Account account, TimeZone tz, BasicPrivateLabel bpl)
    {
        Account a      = account;
        String timeFmt = (a != null)? a.getTimeFormat() : (bpl != null)? bpl.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();
        TimeZone tmz   = (tz != null)? tz : (a != null)? a.getTimeZone(null) : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return dt.format(timeFmt);
    }

    /**
    *** Gets the String representation of the timestamp year of this event
    *** @param timestamp  The timestamp
    *** @param tz         The TimeZone
    *** @return The String representation of the timestamp year of this event
    **/
    public static String getTimestampYear(long timestamp, TimeZone tz)
    {
        TimeZone tmz = (tz != null)? tz : DateTime.getGMTTimeZone();
        DateTime dt  = new DateTime(timestamp, tmz);
        return String.valueOf(dt.getYear());
    }

    /**
    *** Gets the String representation of the timestamp month-number (1..12) of this event
    *** @param timestamp  The timestamp
    *** @param tz         The TimeZone
    *** @return The String representation of the timestamp month-number of this event
    **/
    public static String getTimestampMonthNumber(long timestamp, TimeZone tz)
    {
        TimeZone tmz = (tz != null)? tz : DateTime.getGMTTimeZone();
        DateTime dt  = new DateTime(timestamp, tmz);
        return String.valueOf(dt.getMonth1());
    }

    /**
    *** Gets the String representation of the timestamp month of this event
    *** @param timestamp  The timestamp
    *** @param abbrev     True to return the month abbreviation, false to return the full month name
    *** @param tz         The TimeZone
    *** @param locale     The locale
    *** @return The String representation of the timestamp month of this event
    **/
    public static String getTimestampMonth(long timestamp, boolean abbrev, TimeZone tz, Locale locale)
    {
        TimeZone tmz = (tz != null)? tz : DateTime.getGMTTimeZone();
        DateTime dt  = new DateTime(timestamp, tmz);
        return DateTime.getMonthName(dt.getMonth1(), abbrev);
    }

    /**
    *** Gets the String representation of the timestamp day-of-month of this event
    *** @param timestamp  The timestamp
    *** @param tz         The TimeZone
    *** @return The String representation of the timestamp day-of-month of this event
    **/
    public static String getTimestampDayOfMonth(long timestamp, TimeZone tz)
    {
        TimeZone tmz   = (tz != null)? tz : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return String.valueOf(dt.getDayOfMonth());
    }

    /**
    *** Gets the String representation of the timestamp day-of-week of this event
    *** @param timestamp  The timestamp
    *** @param abbrev     True to return the day abbreviation, false to return the full day name
    *** @param tz         The TimeZone
    *** @param locale     The locale
    *** @return The String representation of the timestamp day-of-week of this event
    **/
    public static String getTimestampDayOfWeek(long timestamp, boolean abbrev, TimeZone tz, Locale locale)
    {
        TimeZone tmz   = (tz != null)? tz : DateTime.getGMTTimeZone();
        DateTime dt    = new DateTime(timestamp, tmz);
        return DateTime.getDayName(dt.getDayOfWeek(), abbrev);
    }

    // --------------------------

    /**
    *** Gets the String representation of the timestamp of this event
    *** @param bpl     The BasicPrivateLabel instance
    *** @return The String representation of the timestamp of this event
    **/
    public String getTimestampString(BasicPrivateLabel bpl)
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampString(ts, acct, tmz, bpl);
    }

    /**
    *** Gets the String representation of the timestamp of this event
    *** @return The String representation of the timestamp of this event
    **/
    public String getTimestampString()
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampString(ts, acct, tmz, null);
    }

    /**
    *** Gets the String representation of the timestamp time-of-day of this event
    *** @return The String representation of the timestamp time-of-day of this event
    **/
    public String getTimestampTime()
    {
        long    ts   = this.getTimestamp();
        Account acct = this.getAccount();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampTime(ts, acct, tmz, null);
    }

    /**
    *** Gets the String representation of the timestamp year of this event
    *** @return The String representation of the timestamp year of this event
    **/
    public String getTimestampYear()
    {
        long     ts  = this.getTimestamp();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampYear(ts, tmz);
    }

    /**
    *** Gets the String representation of the timestamp month of this event
    *** @param abbrev  True to return the month abbreviation, false to return the full month name
    *** @param locale  The locale
    *** @return The String representation of the timestamp month of this event
    **/
    public String getTimestampMonth(boolean abbrev, Locale locale)
    {
        long     ts  = this.getTimestamp();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampMonth(ts, abbrev, tmz, locale);
    }

    /**
    *** Gets the String representation of the timestamp month-number (1..12) of this event
    *** @return The String representation of the timestamp month-number of this event
    **/
    public String getTimestampMonthNumber()
    {
        long     ts  = this.getTimestamp();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampMonthNumber(ts, tmz);
    }

    /**
    *** Gets the String representation of the timestamp day-of-month of this event
    *** @return The String representation of the timestamp day-of-month of this event
    **/
    public String getTimestampDayOfMonth()
    {
        long     ts  = this.getTimestamp();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampDayOfMonth(ts, tmz);
    }

    /**
    *** Gets the String representation of the timestamp day-of-week of this event
    *** @param abbrev  True to return the day abbreviation, false to return the full day name
    *** @param locale  The locale
    *** @return The String representation of the timestamp day-of-week of this event
    **/
    public String getTimestampDayOfWeek(boolean abbrev, Locale locale)
    {
        long     ts  = this.getTimestamp();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampDayOfWeek(ts, abbrev, tmz, locale);
    }

    // --------------------------

    /**
    *** Gets the String representation of the creation timestamp of this event
    *** @return The String representation of the creation timestamp of this event
    **/
    public String getCreationTimeString()
    {
        long    ts   = this.getCreationTime();
        Account acct = this.getAccount();
        TimeZone tmz = this.getTimeZone();
        return EventData.getTimestampString(ts, acct, tmz, null);
    }

    /**
    *** Gets the time difference (in seconds) between time the event was generated by the
    *** device, and the time the server received the event.
    *** @return The elapsed time, in seconds, between the time the event was generated on
    ***         the device, and when it was received by the server.
    **/
    public long getCreationAge()
    {
        long ts = this.getTimestamp();
        long ca = this.getCreationTime();
        return (ca > ts)? (ca - ts) : 0L;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the status code of this event
    *** @return The status code of this event
    **/
    public int getStatusCode()
    {
        return this.getFieldValue(FLD_statusCode, 0);
    }

    /**
    *** Gets the String representation of the status code foregound color<br>
    *** (may return null if this event status code is not pre-defined).
    *** @return The String representation of the status code foregound color
    **/
    public StatusCodeProvider getStatusCodeProvider(BasicPrivateLabel bpl)
    {
        // see also "getStatusCodeDescription"
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getStatusCodeProvider(dev, code, bpl, null/*dftSCP*/);
    }

    /**
    *** Gets the String representation of the status code foregound color
    *** @return The String representation of the status code foregound color
    **/
    public String getStatusCodeForegroundColor(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getForegroundColor(dev, code, bpl, null/*dftColor*/);
    }

    /**
    *** Gets the String representation of the status code backgound color
    *** @return The String representation of the status code backgound color
    **/
    public String getStatusCodeBackgroundColor(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getBackgroundColor(dev, code, bpl, null/*dftColor*/);
    }

    /**
    *** Gets the Hex String representation of the status code of this event
    *** @return The Hex String representation of the status code of this event
    **/
    public String getStatusCodeHex()
    {
        return StatusCodes.GetHex(this.getStatusCode());
    }

    /**
    *** Gets the String representation of the status code of this event
    *** @return The String representation of the status code of this event
    **/
    public String getStatusCodeDescription(BasicPrivateLabel bpl)
    {
        // see also "getStatusCodeProvider"
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getDescription(dev, code, bpl, null/*dftDesc*/);
    }

    /**
    *** Gets the map icon-selector for the status code of this event
    *** @return The map icon-selector for the status code of this event
    **/
    public String getStatusCodeIconSelector(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getIconSelector(dev, code, bpl);
    }

    /**
    *** Gets the icon-name for the status code of this event
    *** @param bpl  The domain BasicPrivateLabel
    *** @return The icon-name for the status code of this event
    **/
    public String getStatusCodeIconName(BasicPrivateLabel bpl)
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getIconName(dev, code, bpl);
    }

    /**
    *** Sets the status code of this event
    *** @param v The status code of this event
    **/
    public void setStatusCode(int v)
    {
        this.setFieldValue(FLD_statusCode, ((v >= 0)? v : StatusCodes.STATUS_NONE));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the data source for this event.  The data source is an optional field defined by the 
    *** remote client tracking device.  
    *** @return The event data source
    **/
    public String getDataSource()
    {
        return this.getFieldValue(FLD_dataSource, "");
    }
    
    /**
    *** Sets the data source for this event.
    *** @param v  The data source
    **/
    public void setDataSource(String v)
    {
        this.setFieldValue(FLD_dataSource, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the transport-id for this event.  This is the 'transportID' from the Transport 
    *** record used to identify this Device.
    *** @return The transport-id used to identify this device.
    **/
    public String getTransportID()
    {
        return this.getFieldValue(FLD_transportID, "");
    }
    
    /**
    *** Sets the transport-id for this event.
    *** @param v  The transport-id used to identify this device.
    **/
    public void setTransportID(String v)
    {
        this.setFieldValue(FLD_transportID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Raw Packet data for this event (if available)
    *** @return The Raw Packet data for this event (for blank if not available)
    **/
    public String getRawData()
    {
        return this.getFieldValue(FLD_rawData, "");
    }

    /**
    *** Sets the Raw Packet data for this event (if available)
    *** @param v The Raw Packet data for this event
    **/
    public void setRawData(String v)
    {
        this.setFieldValue(FLD_rawData, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the best Latitude for this event
    *** @return The best Latitude for this event
    **/
    public double getBestLatitude()
    {
        if (this.isValidGeoPoint()) {
            return this.getLatitude();
        } else {
            return this.getCellLatitude();
        }
    }

    /**
    *** Gets the best Longitude for this event
    *** @return The best Longitude for this event
    **/
    public double getBestLongitude()
    {
        if (this.isValidGeoPoint()) {
            return this.getLongitude();
        } else {
            return this.getCellLongitude();
        }
    }

    /**
    *** Gets the best GeoPoint for this event
    *** (does not return null)
    *** @return The best GeoPoint for this event
    **/
    public GeoPoint getBestGeoPoint()
    {
        if (this.isValidGeoPoint()) {
            return this.getGeoPoint();
        } else {
            return this.getCellGeoPoint();
        }
    }
    
    /**
    *** Gets the accuracy radius, in meters
    *** @return The Accuracy radius, in meters
    **/
    public double getBestAccuracy()
    {
        if (this.isValidGeoPoint()) {
            return this.getHorzAccuracy();
        } else {
            return this.getCellAccuracy();
        }
    }

    // ------------------------------------------------------------------------

    // GeoPoint optimization
    private GeoPoint geoPoint = null;
    
    /**
    *** Gets the GeoPoint for this event
    *** @return The GeoPoint for this event
    **/
    public GeoPoint getGeoPoint()
    {
        if (this.geoPoint == null) {
            if (this.isValidGeoPoint()) {
                this.geoPoint = new GeoPoint(this.getLatitude(), this.getLongitude());
            } else {
                this.geoPoint = GeoPoint.INVALID_GEOPOINT;
            }
        }
        return this.geoPoint;
    }

    /**
    *** Sets the latitude/longitude for this event
    *** @param lat The latitude
    *** @param lng The longitude
    **/
    public void setGeoPoint(double lat, double lng)
    {
        this.setLatitude(lat);
        this.setLongitude(lng);
    }

    /**
    *** Sets the latitude/longitude for this event instance
    *** @param gp The latitude/longitude
    **/
    public void setGeoPoint(GeoPoint gp)
    {
        if (gp == null) {
            // assume called expected 0/0
            this.setLatitude(0.0);
            this.setLongitude(0.0);
        } else
        if (!gp.isValid()) {
            if (!GeoPoint.isOrigin(gp)) {
                // not at origin, display invalid point
                Print.logInfo("GeoPoint is invalid: " + gp);
            }
            this.setLatitude(0.0);
            this.setLongitude(0.0);
        } else {
            // set point
            this.setLatitude(gp.getLatitude());
            this.setLongitude(gp.getLongitude());
        }
    }
    
    /** 
    *** Returns true if the GeoPoint represented by this event is valid
    *** @return True if the GeoPoint represented by this event is valid
    **/
    public boolean isValidGeoPoint()
    {
        return GeoPoint.isValid(this.getLatitude(), this.getLongitude());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the latitude for this event
    *** @return The latitude for this event
    **/
    public double getLatitude()
    {
        return this.getFieldValue(FLD_latitude, 0.0);
    }

    /**
    *** Sets the latitude for this event
    *** @param v The latitude for this event
    **/
    public void setLatitude(double v)
    {
        this.setFieldValue(FLD_latitude, v);
        this.geoPoint = null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the longitude for this event
    *** @return The longitude for this event
    **/
    public double getLongitude()
    {
        return this.getFieldValue(FLD_longitude, 0.0);
    }

    /**
    *** Sets the longitude for this event
    *** @param v The longitude for this event
    **/
    public void setLongitude(double v)
    {
        this.setFieldValue(FLD_longitude, v);
        this.geoPoint = null;
    }

    // ------------------------------------------------------------------------

    public long getGpsAge()
    {
        return this.getFieldValue(FLD_gpsAge, 0L);
    }
    
    public void setGpsAge(long v)
    {
        this.setFieldValue(FLD_gpsAge, ((v >= 0L)? v : 0L));
    }

    // ------------------------------------------------------------------------

    public double getSpeedKPH()
    {
        return this.getFieldValue(FLD_speedKPH, 0.0);
    }
    
    public void setSpeedKPH(double v)
    {
        this.setFieldValue(FLD_speedKPH, v);
    }

    public double getSpeedMPH()
    {
        return this.getSpeedKPH() * GeoPoint.MILES_PER_KILOMETER;
    }

    // ------------------------------------------------------------------------

    public double getHeading()
    {
        return this.getFieldValue(FLD_heading, 0.0);
    }

    public void setHeading(double v)
    {
        this.setFieldValue(FLD_heading, v);
    }
    
    // ------------------------------------------------------------------------

    public double getAltitude() // meters
    {
        return this.getFieldValue(FLD_altitude, 0.0);
    }

    public String getAltitudeString(boolean inclUnits, Locale locale)
    {
        I18N i18n = I18N.getI18N(EventData.class, locale);
        Account.AltitudeUnits altUnits = Account.getAltitudeUnits(this.getAccount());
        double alt = altUnits.convertFromMeters(this.getAltitude());
        String distUnitsStr = "?";
        if (altUnits.isFeet()) {
            distUnitsStr = i18n.getString("EventData.units.feet", "feet");
        } else
        if (altUnits.isMeters()) {
            distUnitsStr = i18n.getString("EventData.units.meters", "meters");
        } else {
            distUnitsStr = altUnits.toString(locale);
        }
        String altStr = StringTools.format(alt,"0");
        return inclUnits? (altStr + " " + distUnitsStr) : altStr;
    }

    public void setAltitude(double v) // meters
    {
        this.setFieldValue(FLD_altitude, v);
    }

    // ------------------------------------------------------------------------

    private boolean isActualOdometer = false; // is actual vehicle odometer

    public double getOdometerKM()
    {
        return this.getFieldValue(FLD_odometerKM, 0.0);
    }

    public void setOdometerKM(double v)
    {
        this.setFieldValue(FLD_odometerKM, v);
    }

    public void setOdometerKM(double v, boolean actualOdom)
    {
        this.setFieldValue(FLD_odometerKM, v);
        this.isActualOdometer = actualOdom;
    }

    public boolean isActualOdometer()
    {
        return this.isActualOdometer;
    }

    // ------------------------------------------------------------------------
    
    public boolean hasOdometerOffsetKM()
    {
        if (!super.hasOptionalFieldValue(FLD_odometerOffsetKM)) {
            return false;
        } else {
            double ofs = this.getOdometerOffsetKM();
            return (ofs != 0.0)? true : false;
        }
    }

    public double getOdometerOffsetKM()
    {
        return this.getFieldValue(FLD_odometerOffsetKM, 0.0);
    }

    public void setOdometerOffsetKM(double v)
    {
        this.setFieldValue(FLD_odometerOffsetKM, v);
    }

    // ------------------------------------------------------------------------

    public enum OdometerOffsetType {
        NONE,
        DEVICE_ONLY,
        EVENT_ONLY,
        BEST
    };
    
    private static OdometerOffsetType DefaultOdometerOffsetType = null;

    /**
    *** Returns the odometer value, adjusted with the specified offset type
    *** @return The offset adjusted odometer value
    **/
    public double getOdometerOffsetKM(OdometerOffsetType offsType)
    {

        /* set default odometer offset type */
        //offsType = OdometerOffsetType.DEVICE_ONLY; // uncomment to force to DEVICE_ONLY
        if (offsType == null) {
            if (DefaultOdometerOffsetType == null) {
                String T = StringTools.trim(RTConfig.getString(DBConfig.PROP_EventData_odometerOffsetType,"")).toUpperCase();
                if (T.startsWith("NONE")) { 
                    // NONE
                    DefaultOdometerOffsetType = OdometerOffsetType.NONE;
                    Print.logInfo("Default Odometer Offset Type: " + DefaultOdometerOffsetType);
                    Print.logWarn("(Odometer offset will not be applied!)");
                } else
                if (T.startsWith("DEVICE")) { 
                    // DEVICE, DEVICE_ONLY, DEVICE-ONLY
                    DefaultOdometerOffsetType = OdometerOffsetType.DEVICE_ONLY;
                    Print.logInfo("Default Odometer Offset Type: " + DefaultOdometerOffsetType);
                    Print.logInfo("(Odometer offset obtained from the Device record only)");
                } else
                if (T.startsWith("EVENT")) { 
                    // EVENT, EVENT_ONLY, EVENT-ONLY, EVENTDATA
                    DefaultOdometerOffsetType = OdometerOffsetType.EVENT_ONLY;
                    Print.logInfo("Default Odometer Offset Type: " + DefaultOdometerOffsetType);
                    Print.logInfo("(Odometer offset obtained from the EventData record only)");
                } else {
                    // (default) BEST
                    DefaultOdometerOffsetType = OdometerOffsetType.BEST;
                    Print.logInfo("Default Odometer Offset Type: " + DefaultOdometerOffsetType);
                    Print.logInfo("(Odometer offset obtained from the EventData or Device records)");
                }
            }
            offsType = DefaultOdometerOffsetType;
        }

        /* return odometer offset based on type */
        switch (offsType) {

            case NONE :
                return 0.0;

            case DEVICE_ONLY : {
                Device dev = this.getDevice();
                if (dev != null) {
                    double ofs = dev.getOdometerOffsetKM();
                    //Print.logInfo("Device] Using Device Odometer Offset: " + ofs);
                    return ofs;
                } else {
                    //Print.logInfo("Device] No Device Odometer Offset: 0.0");
                    return 0.0;
                }
                }

            case EVENT_ONLY: {
                double ofs = this.getOdometerOffsetKM();
                //Print.logInfo("Event] Using EventData Odometer Offset: " + ofs);
                return ofs;
                }

            case BEST:
            default  :
                if (this.hasOdometerOffsetKM()) {
                    double ofs = this.getOdometerOffsetKM();
                    //Print.logInfo("Best] Using EventData Odometer Offset: " + ofs);
                    return ofs;
                } else {
                    Device dev = this.getDevice();
                    if (dev != null) {
                        double ofs = dev.getOdometerOffsetKM();
                        //Print.logInfo("Best] Using Device Odometer Offset: " + ofs);
                        return ofs;
                    } else {
                        //Print.logInfo("Best] No Device Odometer Offset: 0.0");
                        return 0.0;
                    }
                }

        }

    }

    /**
    *** Returns the odometer value, already adjusted with the EventData/Device offset
    *** @return The offset adjusted odometer value
    **/
    public double getOdometerWithOffsetKM()
    {
        double odom = this.getOdometerKM();
        odom += this.getOdometerOffsetKM(null);
        return odom;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the distance/trip odometer value in kilometers.
    *** @return The distance/trip odometer value in kilometers.
    **/
    public double getDistanceKM()
    {
        return this.getFieldValue(FLD_distanceKM, 0.0);
    }

    /**
    *** Sets the distance/trip odometer value in kilometers.
    *** @param v The distance/trip odometer value in kilometers.
    **/
    public void setDistanceKM(double v)
    {
        this.setFieldValue(FLD_distanceKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Geozone index provided by the device
    *** @return The Geozone index
    **/
    public long getGeozoneIndex()
    {
        return this.getFieldValue(FLD_geozoneIndex, 0L);
    }

    /**
    *** Sets the Geozone index provided by the device
    *** @param v The Geozone index
    **/
    public void setGeozoneIndex(long v)
    {
        this.setFieldValue(FLD_geozoneIndex, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this event defines a GeozoneID
    *** @return True if this event defines a GeozoneID
    */
    public boolean hasGeozoneID()
    {
        return !StringTools.isBlank(this.getGeozoneID());
    }

    /**
    *** Gets the GeozoneID
    *** @return The GeozoneID
    **/
    public String getGeozoneID()
    {
        return this.getFieldValue(FLD_geozoneID, "");
    }

    /**
    *** Sets the GeozoneID
    *** @param v The GeozoneID
    **/
    public void setGeozoneID(String v)
    {
        this.setFieldValue(FLD_geozoneID, StringTools.trim(v));
    }

    private Geozone geozone = null;  // Geozone cache optimization
    /**
    *** Sets the Geozone, and GeozoneID
    *** @param zone  The Geozone instance
    **/
    public void setGeozone(Geozone zone)
    {
        this.geozone = zone;
        this.setGeozoneID((zone != null)? zone.getGeozoneID() : "");
    }

    /**
    *** Returns true if this EventData record has a previously defined Geozone instance
    *** (Note: this EventData record may still have a valid GeozoneID)
    *** @return True is this EventData record has a defined Geozone
    **/
    public boolean hasGeozone()
    {
        return (this.geozone != null);
    }

    /**
    *** Loads and returns the Geozone for the GeozoneID held by this EventData record.
    *** Returns null if this EventData record does not contain a valid GeozoneID.
    *** @return The Geozone, or null if this EventData record has no valid GeozoneID
    **/
    public Geozone getGeozone()
    {
        if ((this.geozone == null) && this.hasGeozoneID()) {
            String gid = this.getGeozoneID();
            try {
                Geozone gz[] = Geozone.getGeozone(this.getAccount(), gid);
                this.geozone = !ListTools.isEmpty(gz)? gz[0] : null;
            } catch (DBNotFoundException dbnfe) {
                Print.logWarn("Geozone not found: " + this.getAccountID() + "/" + gid);
                this.geozone = null;
            } catch (DBException dbe) {
                Print.logException("Error reading Geozone: " + this.getAccountID() + "/" + gid, dbe);
                this.geozone = null;
            }
        } 
        return this.geozone;
    }

    /**
    *** Gets the Geozone description
    *** @return The Geozone description, or an empty String if this EventData record does
    ***         not define a valid GeozoneID
    **/
    public String getGeozoneDescription()
    {
        Geozone zone = this.getGeozone();
        return (zone != null)? zone.getDescription() : "";
    }

    /**
    *** Gets the Geozone display name 
    *** @return The Geozone display name, or an empty String if this EventData record does
    ***         not define a valid GeozoneID
    **/
    public String getGeozoneDisplayName()
    {
        Geozone zone = this.getGeozone();
        return (zone != null)? zone.getDisplayName() : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last motion change timestamp
    *** @return The last motion change timestamp
    **/
    /*
    public long getMotionChangeTime()
    {
        return this.getFieldValue(FLD_motionChangeTime, 0L);
    }
    */

    /**
    *** Sets the last motion change timestamp
    *** @param v The last motion change timestamp
    **/
    /*
    public void setMotionChangeTime(long v)
    {
        this.setFieldValue(FLD_motionChangeTime, ((v >= 0L)? v : 0L));
    }
    */

    /**
    *** Returns the elapsed number of stopped seconds <br>
    *** Returns '-1' if moving, or if unable to calculate stopped-time.<br>
    *** Returns '0' if this event represents the first stopped event.
    *** @return The elapsed number of stopped seconds
    **/
    /*
    public long getElapsedStoppedSeconds()
    {

        // moving?
        if (this.getSpeedKPH() > 0.0) {
            // moving
            return -1L;
        }

        // calculate elapsed stop time
        long timestamp  = this.getTimestamp();
        long motionTime = this.getMotionChangeTime();
        if (motionTime <= 0L) {
            // motion-change not initialized
            return -1L;
        } else
        if (motionTime > timestamp) {
            // invalid motion-change time (unlikely)
            return -1L;
        } else {
            // number of seconds stopped (may still be '0')
            return (timestamp - motionTime); 
        }

    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Gets the event priority
    *** @return The event priority
    **/
    public int getPriority()
    {
        return this.getFieldValue(FLD_priority, 0);
    }

    /**
    *** Sets the event priority
    *** @param v The event priority
    **/
    public void setPriority(int v)
    {
        this.setFieldValue(FLD_priority, v);
    }

    // ------------------------------------------------------------------------

    public String getEntityID()
    {
        return this.getFieldValue(FLD_entityID, "");
    }
    
    public void setEntityID(String v)
    {
        this.setFieldValue(FLD_entityID, StringTools.trim(v));
    }

    public String[] getEntityIDList(String altID)
    {

        /* create alternate list of entity IDs */
        Set<String> entitySet = null;
        String entityID = this.getEntityID();
        if (!StringTools.isBlank(altID) || !StringTools.isBlank(entityID)) {
            entitySet = new HashSet<String>();
            if (!StringTools.isBlank(altID   )) { entitySet.add(altID.trim()   ); }
            if (!StringTools.isBlank(entityID)) { entitySet.add(entityID.trim()); }
        }

        /* no "Entity" attached data? */
        String attachType = this.getAttachType();
        if (!attachType.equalsIgnoreCase(HTMLTools.CONTENT_TYPE_ENTITY_CSV)) {
            return ListTools.isEmpty(entitySet)? null : entitySet.toArray(new String[entitySet.size()]); 
        }

        /* check attach data */
        byte data[] = this.getAttachData();
        if (ListTools.isEmpty(data)) {
            return ListTools.isEmpty(entitySet)? null : entitySet.toArray(new String[entitySet.size()]); 
        }

        /* extract listed entities */
        String entCSV = StringTools.trim(StringTools.toStringValue(data,' '));
        if (entCSV != null) {
            if (entitySet == null) { entitySet = new HashSet<String>(); }
            String entIDList[] = StringTools.split(entCSV, ',');
            for (String entID : entIDList) {
                // this will remove duplicates
                if (!StringTools.isBlank(entID)) { entitySet.add(entID.trim()); }
            }
        }
        return ListTools.isEmpty(entitySet)? null : entitySet.toArray(new String[entitySet.size()]); 

    }

    // ------------------------------------------------------------------------

    private static final EntityManager.EntityType DEFAULT_ENTITY_TYPE = EntityManager.EntityType.GENERAL;

    private int entityTypeInt = -1; // undefined

    /**
    *** Gets the Entity type
    *** @param dft The default EntityType if unable to determine the actual type
    *** @return The Entity type
    **/
    public EntityManager.EntityType _getEntityType(EntityManager.EntityType dft)
    {

        /* get entity type from record */
        Integer v = (Integer)this.getFieldValue(FLD_entityType);
        int entTypeInt = (v != null)? v.intValue() : this.entityTypeInt;
        if (entTypeInt > 0) {
            return EnumTools.getValueOf(EntityManager.EntityType.class,entTypeInt);
        }

        /* return best entity type based on status code */
        return EntityManager.getEntityTypeForStatusCode(this.getStatusCode(),dft);

    }

    /**
    *** Gets the Entity type
    *** @return The Entity type
    **/
    public int getEntityType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_entityType);
        int ETI = (v != null)? v.intValue() : this.entityTypeInt;
        return (ETI >= 0)? ETI : DEFAULT_ENTITY_TYPE.getIntValue();
    }

    /**
    *** Sets the Entity type
    *** @param v The Entity type
    **/
    public void setEntityType(int v)
    {
        int entType = EnumTools.getValueOf(EntityManager.EntityType.class,v).getIntValue();
        this.setFieldValue(FLD_entityType, entType);
        this.entityTypeInt = v;
    }

    /**
    *** Sets the Entity type
    *** @param v The Entity type
    **/
    public void setEntityType(EntityManager.EntityType v)
    {
        int entType = EnumTools.getValueOf(EntityManager.EntityType.class,v).getIntValue();
        this.setEntityType(entType);
    }

    /**
    *** Sets the Entity type
    *** @param v The Entity type
    **/
    public void setEntityType(String v, Locale locale)
    {
        int entType = EnumTools.getValueOf(EntityManager.EntityType.class,v,locale).getIntValue();
        this.setEntityType(entType);
    }

    /**
    *** Gets the Entity type description text
    *** @param loc The Locale for which the text is returned.
    **/
    public String getEntityTypeDescription(Locale loc)
    {
        EntityManager.EntityType entType = this._getEntityType(null);
        if (entType != null) {
            return entType.toString(loc);
        } else {
            return "?";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the GPS fix type
    *** @return The GPS fix type
    **/
    public int getGpsFixType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_gpsFixType);
        return (v != null)? v.intValue() : EnumTools.getDefault(GPSFixType.class).getIntValue();
    }

    /**
    *** Sets the GPS fix type
    *** @param v The GPS fix type
    **/
    public void setGpsFixType(int v)
    {
        this.setFieldValue(FLD_gpsFixType, EnumTools.getValueOf(GPSFixType.class,v).getIntValue());
    }

    /**
    *** Sets the GPS fix type
    *** @param v The GPS fix type
    **/
    public void setGpsFixType(GPSFixType v)
    {
        this.setFieldValue(FLD_gpsFixType, EnumTools.getValueOf(GPSFixType.class,v).getIntValue());
    }

    /**
    *** Sets the GPS fix type
    *** @param v The GPS fix type
    **/
    public void setGpsFixType(String v, Locale locale)
    {
        this.setFieldValue(FLD_gpsFixType, EnumTools.getValueOf(GPSFixType.class,v,locale).getIntValue());
    }

    /**
    *** Gets the GPS fix type description text
    *** @param loc The Locale for which the text is returned.
    **/
    public String getGpsFixTypeDescription(Locale loc)
    {
        GPSFixType fixType = EventData.getGPSFixType(this);
        if (fixType.equals(GPSFixType.UNKNOWN)) { // 2.4.6-B01
            if (this.isValidGeoPoint()) { 
                //return GPSFixType.UNKNOWN.toString(loc);
                return GPSFixType.n2D.toString(loc);
            } else {
                return GPSFixType.NONE.toString(loc);
            }
        } else {
            return fixType.toString(loc);
        }
    }

    // ------------------------------------------------------------------------

    /* GPS fix status (as defined by specific DCS */
    public long getGpsFixStatus()
    {
        Long v = (Long)this.getFieldValue(FLD_gpsFixStatus);
        return (v != null)? v.longValue() : 0L;
    }
    
    public void setGpsFixStatus(long v)
    {
        this.setFieldValue(FLD_gpsFixStatus, ((v >= 0L)? v : 0L));
    }

    // ------------------------------------------------------------------------

    /* horizontal accuracy (meters) */
    public double getHorzAccuracy()
    {
        return this.getFieldValue(FLD_horzAccuracy, 0.0);
    }
    
    public void setHorzAccuracy(double v)
    {
        this.setFieldValue(FLD_horzAccuracy, v);
    }

    // ------------------------------------------------------------------------

    /* vertical accuracy (meters) */
    public double getVertAccuracy()
    {
        return this.getFieldValue(FLD_vertAccuracy, 0.0);
    }
    
    public void setVertAccuracy(double v)
    {
        this.setFieldValue(FLD_vertAccuracy, v);
    }

    // ------------------------------------------------------------------------

    public double getHDOP()
    {
        return this.getFieldValue(FLD_HDOP, 0.0);
    }
    
    public void setHDOP(double v)
    {
        this.setFieldValue(FLD_HDOP, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Satellite count
    *** @return The Satellite count
    **/
    public int getSatelliteCount()
    {
        return this.getFieldValue(FLD_satelliteCount, 0);
    }

    /**
    *** Sets the Satellite count
    *** @param v The Satellite count
    **/
    public void setSatelliteCount(int v)
    {
        this.setFieldValue(FLD_satelliteCount, ((v < 0)? 0 : v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current battery level (as a percent)
    *** @return The current battery level
    **/
    public double getBatteryLevel()
    {
        // Note: Battery Voltage can be converted to Battery Level using the following example algorithm:
        //   double BATTERY_VOLTS_MAX;
        //   double BATTERY_VOLTS_MIN;
        //   double currentBattVolts;
        //   double battLevelPercent;
        //   battLevelPercent = (currentBattVolts - BATTERY_VOLTS_MIN) / (BATTERY_VOLTS_MAX - BATTERY_VOLTS_MIN);
        //   if (battLevelPercent > 1.0) { battLevelPercent = 1.0; }
        //   if (battLevelPercent < 0.0) { battLevelPercent = 0.0; }
        double V = this.getFieldValue(FLD_batteryLevel, 0.0);
        return V;
    }
    
    /**
    *** Sets the current battery level
    *** @param v The current battery level
    **/
    public void setBatteryLevel(double v)
    {
        //if (v > 1.1) { v = v / 100.0; }
        this.setFieldValue(FLD_batteryLevel, v);
    }

    // ------------------------------------------------------------------------


    /**
    *** Gets the current battery voltage
    *** @return The current battery voltage
    **/
    public double getBatteryVolts()
    {
        return this.getFieldValue(FLD_batteryVolts, 0.0);
    }
    
    /**
    *** Sets the current battery voltage
    *** @param v The current battery voltage
    **/
    public void setBatteryVolts(double v)
    {
        this.setFieldValue(FLD_batteryVolts, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the battery temperature (C)
    *** @return The battery temperature (C)
    **/
    public double getBatteryTemp()
    {
        return this.getFieldValue(FLD_batteryTemp, INVALID_TEMPERATURE);
    }

    /**
    *** Sets the battery temperature (C)
    *** @param v The battery temperature (C)
    **/
    public void setBatteryTemp(double v)
    {
        this.setFieldValue(FLD_batteryTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current signal strength
    *** @return The current signal strength
    **/
    public double getSignalStrength()
    {
        return this.getFieldValue(FLD_signalStrength, 0.0);
    }
    
    /**
    *** Sets the current signal strength
    *** @param v The current signal strength
    **/
    public void setSignalStrength(double v)
    {
        this.setFieldValue(FLD_signalStrength, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if an address has been defined
    *** @return True if an address has been defined
    **/
    public boolean hasAddress()
    {
        return !this.getAddress().equals("");
    }

    public String getAddress()
    {
        String v = (String)this.getFieldValue(FLD_address);
        return StringTools.trim(v);
    }

    public String getAddress(boolean lazyUpdate)
    {
        String v = this.getAddress();
        if (lazyUpdate && v.equals("")) {
            try {
                Set<String> updFlds = this.updateAddress(true/*fastOnly*/, false/*force*/);
                if (!ListTools.isEmpty(updFlds)) {
                    this.update(updFlds);
                }
            } catch (SlowOperationException soe) {
                // will occur if reverse-geocoder is not a fast operation
                // leave 'v' as-is
            } catch (DBException dbe) {
                dbe.printException();
            }
            v = this.getAddress();
        }
        return v;
    }

    public void setAddress(String v)
    {
        String addr = StringTools.trim(v);
        if ((EventData.AddressColumnLength > 0)             &&
            (addr.length() >= EventData.AddressColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.AddressColumnLength - 1; 
            addr = addr.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_address, addr);
    }

    // ------------------------------------------------------------------------

    /**
    *** Reload all reverse-geocoded address fields
    **/
    public void reloadAddress()
    {
        this.reload(
            FLD_address,
            FLD_streetAddress,
            FLD_city,
            FLD_stateProvince,
            FLD_postalCode,
            FLD_country,
            FLD_subdivision,
            FLD_speedLimitKPH,
            FLD_isTollRoad
            );
    }
    
    // ------------------------------------------------------------------------

    public Set<String> updateAddress(boolean fastOnly)
        throws SlowOperationException
    {
        return this.updateAddress(fastOnly, false/*force*/);
    }

    public Set<String> updateAddress(boolean fastOnly, boolean force)
        throws SlowOperationException
    {
        // If the caller does not want to wait for a time-consuming operation, specifying 
        // 'fastOnly==true' will cause this method to throw a 'SlowOperationException' if 
        // it determines that the reverse-geocoding will take too long.  The reason that 
        // reverse-geocoding  might take a while is because it might be using an outside 
        // service (ie. linking to a remote web-based service) to perform it's function.
        // (SlowOperationException is not thrown if 'fastOnly' is false.)

        /* already have an address? */
        if (!force && this.hasAddress()) {
            // we already have an address 
            // (and 'force' did not indicate we should update the address)
            return null;
        }

        /* get Account */
        Account acct = this.getAccount();
        if (acct == null) {
            // -- no account, not reverse-geocoding
            Print.logError("Account is null");
            return null;
        }

        /* get Device */
        Device dev = this.getDevice();
        if (dev == null) {
            // -- no device, not reverse-geocoding
            Print.logError("Device is null");
            return null;
        }

        /* get geocoder mode */
        Account.GeocoderMode geocoderMode = Account.getGeocoderMode(acct);
        if (geocoderMode.isNone()) {
            // no geocoding is performed for this account
            Print.logDebug("GeocoderMode disabled for this Account: " + acct.getAccountID());
            return null;
        }

        /* set "Departed" geozone description for STATUS_GEOFENCE_DEPART events */
        int statusCode = this.getStatusCode();
        if (statusCode == StatusCodes.STATUS_GEOFENCE_DEPART) {
            // -- On departure events, get the departed Geozone description
            Geozone gz[] = null;
            // first try clientID
            if ((gz == null) || (gz.length == 0)) {
                long clientId = this.getGeozoneIndex(); // ClientID of departed geozone
                if (clientId > 0L) {
                    gz = Geozone.getClientIDZones(acct.getAccountID(), clientId);
                }
            }
            // -- next try geozoneID
            if ((gz == null) || (gz.length == 0)) {
                String geozoneID = this.getGeozoneID();
                if (!StringTools.isBlank(geozoneID)) {
                    try {
                        gz = Geozone.getGeozone(acct, geozoneID);
                    } catch (DBNotFoundException dbnfe) {
                        Print.logWarn("Geozone not found: " + acct.getAccountID() + "/" + geozoneID);
                    } catch (DBException dbe) {
                        Print.logException("Error reading Geozone: " + acct.getAccountID() + "/" + geozoneID, dbe);
                    }
                }
            }
            // -- update and return if we found the geozone
            if ((gz != null) && (gz.length > 0) && gz[0].isReverseGeocode(this.getDeviceID())) {
                Set<String> updFields = new HashSet<String>();
                if (gz[0].isClientUpload()) {
                    this.setGeozoneIndex(gz[0].getClientID());      // FLD_geozoneIndex
                    updFields.add(EventData.FLD_geozoneIndex);
                }
                this.setGeozoneID(gz[0].getGeozoneID());            // FLD_geozoneID
                updFields.add(EventData.FLD_geozoneID);
                this.setAddress(gz[0].getDescription());            // FLD_address
                updFields.add(EventData.FLD_address);
                this.setStreetAddress(gz[0].getStreetAddress());    // FLD_streetAddress
                updFields.add(EventData.FLD_streetAddress);
                this.setCity(gz[0].getCity());                      // FLD_city
                updFields.add(EventData.FLD_city);
                this.setStateProvince(gz[0].getStateProvince());    // FLD_stateProvince
                updFields.add(EventData.FLD_stateProvince);
                this.setPostalCode(gz[0].getPostalCode());          // FLD_postalCode
                updFields.add(EventData.FLD_postalCode);
                this.setCountry(gz[0].getCountry());                // FLD_country
                updFields.add(EventData.FLD_country);
                this.setSubdivision(gz[0].getSubdivision());        // FLD_subdivision
                updFields.add(EventData.FLD_subdivision);
                return updFields;
            }
        } else
        if (statusCode == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
            // -- On arrival events, get the arrival Geozone description
            // -  (due to rounding error, the server may think we are not yet within a zone)
            Geozone gz[] = null;
            // -- first try clientID
            if ((gz == null) || (gz.length == 0)) {
                long clientId = this.getGeozoneIndex(); // ClientID of arrival geozone
                if (clientId > 0L) {
                    gz = Geozone.getClientIDZones(acct.getAccountID(), clientId);
                }
            }
            // -- next try geozoneID
            if ((gz == null) || (gz.length == 0)) {
                String geozoneID = this.getGeozoneID();
                if (!StringTools.isBlank(geozoneID)) {
                    try {
                        gz = Geozone.getGeozone(acct, geozoneID);
                    } catch (DBNotFoundException dbnfe) {
                        Print.logWarn("Geozone not found: " + acct.getAccountID() + "/" + geozoneID);
                    } catch (DBException dbe) {
                        Print.logException("Error reading Geozone: " + acct.getAccountID() + "/" + geozoneID, dbe);
                    }
                }
            }
            // -- update and return if we found the geozone
            if ((gz != null) && (gz.length > 0) && gz[0].isReverseGeocode(this.getDeviceID())) {
                Set<String> updFields = new HashSet<String>();
                if (gz[0].isClientUpload()) {
                    this.setGeozoneIndex(gz[0].getClientID());      // FLD_geozoneIndex
                    updFields.add(EventData.FLD_geozoneIndex);
                }
                this.setGeozoneID(gz[0].getGeozoneID());            // FLD_geozoneID
                updFields.add(EventData.FLD_geozoneID);
                this.setAddress(gz[0].getDescription());            // FLD_address
                updFields.add(EventData.FLD_address);
                this.setStreetAddress(gz[0].getStreetAddress());    // FLD_streetAddress
                updFields.add(EventData.FLD_streetAddress);
                this.setCity(gz[0].getCity());                      // FLD_city
                updFields.add(EventData.FLD_city);
                this.setStateProvince(gz[0].getStateProvince());    // FLD_stateProvince
                updFields.add(EventData.FLD_stateProvince);
                this.setPostalCode(gz[0].getPostalCode());          // FLD_postalCode
                updFields.add(EventData.FLD_postalCode);
                this.setCountry(gz[0].getCountry());                // FLD_country
                updFields.add(EventData.FLD_country);
                this.setSubdivision(gz[0].getSubdivision());        // FLD_subdivision
                updFields.add(EventData.FLD_subdivision);
                return updFields;
            }
        }

        /* GeoPoint required after this point */
        GeoPoint addrGP = null;
        double    evLat = this.getLatitude();   // .getCellLatitude();
        double    evLon = this.getLongitude();  // .getCellLongitude();
        if (GeoPoint.isValid(evLat,evLon)) {
            addrGP = new GeoPoint(evLat, evLon);
        } else {
            double cellLat = this.getCellLatitude();
            double cellLon = this.getCellLongitude();
            if (GeoPoint.isValid(cellLat,cellLon)) {
                addrGP = new GeoPoint(cellLat, cellLon);
            } else {
                // -- Can't reverse-geocode an invalid point
                return null;
            }
        }

        /* (at least GeocoderMode.GEOZONE) get address from Geozone */
        Geozone gzone = Geozone.getGeozone(acct.getAccountID(), null/*zoneID*/, addrGP, true); // <Geozone>.getReverseGeocode() == true
        if ((gzone != null) && !gzone.isDeviceInGroup(this.getDeviceID())) { gzone = null; }
        if (gzone != null) {
            Set<String> updFields = new HashSet<String>();
            Print.logInfo("Found Geozone : " + gzone.getGeozoneID() + " - " + gzone.getDescription());
            if (gzone.isClientUpload() && (this.getGeozoneIndex() <= 0L)) {
                this.setGeozoneIndex(gzone.getClientID());              // FLD_geozoneIndex
                updFields.add(EventData.FLD_geozoneIndex);
            }
            this.setGeozoneID(gzone.getGeozoneID());                    // FLD_geozoneID
            updFields.add(EventData.FLD_geozoneID);
            this.setAddress(gzone.getDescription());                    // FLD_address
            updFields.add(EventData.FLD_address);
            this.setStreetAddress(gzone.getStreetAddress());            // FLD_streetAddress
            updFields.add(EventData.FLD_streetAddress);
            this.setCity(gzone.getCity());                              // FLD_city
            updFields.add(EventData.FLD_city);
            this.setStateProvince(gzone.getStateProvince());            // FLD_stateProvince
            updFields.add(EventData.FLD_stateProvince);
            this.setPostalCode(gzone.getPostalCode());                  // FLD_postalCode
            updFields.add(EventData.FLD_postalCode);
            this.setCountry(gzone.getCountry());                        // FLD_country
            updFields.add(EventData.FLD_country);
            this.setSubdivision(gzone.getSubdivision());                // FLD_subdivision
            updFields.add(EventData.FLD_subdivision);
            return updFields;
        }

        /* reverse-geocoding iff FULL, or PARTIAL with high-priority status code */
        // -- "NONE" has already been filtered above
        BasicPrivateLabel privLabel = acct.getPrivateLabel();
        if (geocoderMode.okFull()) {
            // -- FULL reverse-geocoding, continue below ...
        } else
        if (StatusCodes.IsHighPriority(statusCode,privLabel)) {
            // -- PARTIAL: high-priority statusCode, continue below ...
        } else {
            // -- PARTIAL: not a high-priority status code
            boolean overridePartial = false;
            // -- check simple conditions for overriding "partial" reverse-geocode
            // -   TODO: ie ((this.speedKPH() <= 0.0) && (dev.getCurrentIgnitionState() == 1))
            // -- skip reverse-geocode if we are not overriding PARTIAL
            if (!overridePartial) {
                Print.logDebug("Skipping reverse-geocode per Account geocoderMode: " + acct.getAccountID());
                return null;
            }
        }

        /* get reverse-geocoder */
        ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
        if (rgp == null) {
            // -- no ReverseGeocodeProvider, no reverse-geocoding
            String acctID = this.getAccountID();
            if (acct.hasPrivateLabel()) {
                Print.logInfo("[Account '%s'] PrivateLabel '%s' does not define a ReverseGeocodeProvider", acctID, privLabel); 
            } else {
                Print.logInfo("No PrivateLabel (thus no ReverseGeocodeProvider) for Account '%s'", acctID); 
            }
            return null;
        } else
        if (!rgp.isEnabled()) {
            Print.logInfo("ReverseGeocodeProvider disabled: " + rgp.getName());
            return null;
        }

        /* fast operations only? */
        if (fastOnly && !rgp.isFastOperation()) {
            // We've requested a fast operation only, and this operation is slow.
            // It's up to the caller to see that this operation is queued in a background thread.
            throw new SlowOperationException("'fast' requested, and this operation is 'slow'");
        }

        /* finally, get the address for this point */
        ReverseGeocode rg = null;
        try {
            // make sure the Domain properties are available to RTConfig
            privLabel.pushRTProperties();   // stack properties (may be redundant in servlet environment)
            privLabel.getLocaleString();    // ??? redundent?
            boolean cache = (this.getSpeedKPH() <= 0.0)? true : false; // cache if not moving
            rg = rgp.getReverseGeocode(addrGP, privLabel.getLocaleString(), cache); // get the reverse-geocode
        } catch (Throwable th) {
            // ignore
        } finally {
            privLabel.popRTProperties();    // remove from stack
        }
        if (rg != null) {
            Set<String> updFields = new HashSet<String>();
            if (rg.hasFullAddress()) {
                this.setAddress(rg.getFullAddress());                   // FLD_address
                updFields.add(EventData.FLD_address);
            }
            if (rg.hasStreetAddress()) {
                this.setStreetAddress(rg.getStreetAddress());           // FLD_streetAddress
                updFields.add(EventData.FLD_streetAddress);
            }
            if (rg.hasCity()) {
                this.setCity(rg.getCity());                             // FLD_city
                updFields.add(EventData.FLD_city);
            }
            if (rg.hasStateProvince()) {
                this.setStateProvince(rg.getStateProvince());           // FLD_stateProvince
                updFields.add(EventData.FLD_stateProvince);
            }
            if (rg.hasPostalCode()) {
                this.setPostalCode(rg.getPostalCode());                 // FLD_postalCode
                updFields.add(EventData.FLD_postalCode);
            }
            if (rg.hasCountryCode()) {
                this.setCountry(rg.getCountryCode());                   // FLD_country
                updFields.add(EventData.FLD_country);
            }
            if (rg.hasSubdivision()) {
                this.setSubdivision(rg.getSubdivision());               // FLD_subdivision
                updFields.add(EventData.FLD_subdivision);
            }
            if (rg.hasSpeedLimitKPH()) {
                this.setSpeedLimitKPH(rg.getSpeedLimitKPH());           // FLD_speedLimitKPH
                updFields.add(EventData.FLD_speedLimitKPH);
            }
            if (rg.hasIsTollRoad()) {
                this.setIsTollRoad(rg.getIsTollRoad());                 // FLD_isTollRoad
                updFields.add(EventData.FLD_isTollRoad);
            }
            return !updFields.isEmpty()? updFields : null;
        }

        /* still no address after all of this */
        Print.logInfo("No RG Address found ["+rgp.getName()+"]: " + addrGP);
        return null;
        
    }

    // ------------------------------------------------------------------------

    public String getStreetAddress()
    {
        String v = (String)this.getFieldValue(FLD_streetAddress);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setStreetAddress(String v)
    {
        String street = StringTools.trim(v);
        if ((EventData.StreetColumnLength > 0)              &&
            (street.length() >= EventData.StreetColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.StreetColumnLength - 1; 
            street = street.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_streetAddress, street);
    }

    // ------------------------------------------------------------------------

    public String getCity()
    {
        String v = (String)this.getFieldValue(FLD_city);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }

    public void setCity(String v)
    {
        String city = StringTools.trim(v);
        if ((EventData.CityColumnLength > 0)              &&
            (city.length() >= EventData.CityColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.CityColumnLength - 1; 
            city = city.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_city, city);
    }

    // ------------------------------------------------------------------------

    public String getStateProvince()
    {
        String v = (String)this.getFieldValue(FLD_stateProvince);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setStateProvince(String v)
    {
        String state = StringTools.trim(v); 
        if ((EventData.StateColumnLength > 0)              &&
            (state.length() >= EventData.StateColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.StateColumnLength - 1; 
            state = state.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_stateProvince, state);
    }

    // ------------------------------------------------------------------------

    public String getPostalCode()
    {
        String v = (String)this.getFieldValue(FLD_postalCode);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setPostalCode(String v)
    {
        String postal = StringTools.trim(v);
        if ((EventData.PostalColumnLength > 0)              &&
            (postal.length() >= EventData.PostalColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = EventData.PostalColumnLength - 1; 
            postal = postal.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the record if the data length
            // is greater than the table column length.
        }
        this.setFieldValue(FLD_postalCode, postal);
    }

    // ------------------------------------------------------------------------

    public String getCountry()
    {
        String v = (String)this.getFieldValue(FLD_country);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    public void setCountry(String v)
    {
        this.setFieldValue(FLD_country, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return subdivision */
    public String getSubdivision()
    {
        String v = (String)this.getFieldValue(FLD_subdivision);
        if ((v == null) || v.equals("")) {
            // should we try to go get the reverse-geocode?
            v = ""; // in case it was null
        }
        return v;
    }
    
    /* set subdivision */
    public void setSubdivision(String v)
    {
        this.setFieldValue(FLD_subdivision, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get posted speed limit */
    public double getSpeedLimitKPH()
    {
        return this.getFieldValue(FLD_speedLimitKPH, 0.0);
    }
    
    /* set posted speed limit */
    public void setSpeedLimitKPH(double v)
    {
        this.setFieldValue(FLD_speedLimitKPH, ((v > 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    public boolean getIsTollRoad()
    {
        return this.getFieldValue(FLD_isTollRoad, false);
    }

    public void setIsTollRoad(boolean v)
    {
        this.setFieldValue(FLD_isTollRoad, v);
    }

    public boolean isTollRoad()
    {
        return this.getIsTollRoad();
    }

    // ------------------------------------------------------------------------

    private boolean inputMaskExplicitlySet = false;

    /* get digital input mask */
    public boolean isInputMaskExplicitlySet()
    {
        return this.inputMaskExplicitlySet;
    }

    /* get digital input mask */
    public long getInputMask()
    {
        Long v = (Long)this.getFieldValue(FLD_inputMask);
        return (v != null)? v.intValue() : 0L;
    }

    /* return state of input mask bit */
    public boolean getInputMaskBitState(int bit)
    {
        long m = this.getInputMask();
        return (((1L << bit) & m) != 0L);
    }
    
    /* set digital input mask */
    public void setInputMask(long v)
    {
        if (v < 0L) {
            // FLD_inputMask is unsigned, set to zero
            this.setFieldValue(FLD_inputMask, 0L);
            this.inputMaskExplicitlySet = false; // <0 is undefined, do not indicate set
        } else {
            // FLD_inputMask is currently 32-bit max 
            this.setFieldValue(FLD_inputMask, (v & 0xFFFFFFFFL)); // 32-bits only
            this.inputMaskExplicitlySet = true;
        }
    }

    // ------------------------------------------------------------------------

    private boolean outputMaskExplicitlySet = false;

    /* get digital output mask */
    public boolean isOutputMaskExplicitlySet()
    {
        return this.outputMaskExplicitlySet;
    }

    /* get digital output mask */
    public long getOutputMask()
    {
        Long v = (Long)this.getFieldValue(FLD_outputMask);
        return (v != null)? v.intValue() : 0L;
    }
    
    /* return state of output mask bit */
    public boolean getOutputMaskBitState(int bit)
    {
        long m = this.getOutputMask();
        return (((1L << bit) & m) != 0L);
    }
    
    /* set digital output mask */
    public void setOutputMask(long v)
    {
        if (v < 0L) {
            // FLD_outputMask is unsigned
            this.setFieldValue(FLD_outputMask, 0L);
            this.outputMaskExplicitlySet = false; // <0 is undefined, do not indicate set
        } else {
            // FLD_outputMask is currently 32-bit max 
            this.setFieldValue(FLD_outputMask, (v & 0xFFFFFFFFL)); // 32-bits only
            this.outputMaskExplicitlySet = true;
        }
    }

    // ------------------------------------------------------------------------

    /* get seatbelt mask */
    public long getSeatbeltMask()
    {
        Long v = (Long)this.getFieldValue(FLD_seatbeltMask);
        return (v != null)? v.intValue() : 0L;
    }
    
    /* return state of seatbelt mask bit */
    public boolean getSeatbeltMaskBitState(int bit)
    {
        long m = this.getSeatbeltMask();
        return (((1L << bit) & m) != 0L);
    }
    
    /* set digital seatbelt mask */
    public void setSeatbeltMask(long v)
    {
        if (v < 0L) {
            // FLD_seatbeltMask is unsigned
            this.setFieldValue(FLD_seatbeltMask, 0L);
        } else {
            // FLD_seatbeltMask is currently 32-bit max 
            this.setFieldValue(FLD_seatbeltMask, (v & 0xFFFFFFFFL)); // 32-bit
        }
    }

    // Common Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Temerature Bean access fields below

    public double getBarometer()
    {
        return this.getFieldValue(FLD_barometer, 0.0); // kPa
    }
    
    public void setBarometer(double v)
    {
        this.setFieldValue(FLD_barometer, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ambient air temperature (C)
    *** @return The ambient air temperature (C)
    **/
    public double getAmbientTemp()
    {
        return this.getFieldValue(FLD_ambientTemp, INVALID_TEMPERATURE);
    }

    /**
    *** Sets the ambient air temperature (C)
    *** @param v The ambient air temperature (C)
    **/
    public void setAmbientTemp(double v)
    {
        this.setFieldValue(FLD_ambientTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the cabin temperature (C)
    *** @return The cabin temperature (C)
    **/
    public double getCabinTemp()
    {
        return this.getFieldValue(FLD_cabinTemp, INVALID_TEMPERATURE);
    }

    /**
    *** Sets the cabin temperature (C)
    *** @param v The cabin air temperature (C)
    **/
    public void setCabinTemp(double v)
    {
        this.setFieldValue(FLD_cabinTemp, v);
    }

    // ------------------------------------------------------------------------
    
    private static int    Thermo_COUNT      = -1;
    private static Object Thermo_COUNT_lock = new Object();
    
    public static int getThermoCount()
    {
        // count defined temperature values
        if (Thermo_COUNT < 0) {
            synchronized (Thermo_COUNT_lock) {
                if (Thermo_COUNT < 0) {
                    DBFactory<EventData> dbFact = EventData.getFactory();
                    Thermo_COUNT = 0;
                    for (int i = 0; i < ThermoFieldInfo.length; i++) {
                        if (dbFact.hasField(ThermoFieldInfo[i]._getName())) {
                            Thermo_COUNT++;
                        } else {
                            break; // we're done
                        }
                    }
                }
            }
            //Print.logWarn("Found " + Thermo_COUNT + " defined Temperature fields");
        }
        // return count
        return Thermo_COUNT;
    }

    public static boolean isValidTemperature(double t)
    {
        // IE. ((T >= -126) && (T <= 200))
        return ((t >= TEMPERATURE_LIMIT_LO) && (t <= TEMPERATURE_LIMIT_HI));
    }

    public double getThermoAverage(int ndx)
    {
        switch (ndx) {
            case 0: return this.getThermoAverage0();
            case 1: return this.getThermoAverage1();
            case 2: return this.getThermoAverage2();
            case 3: return this.getThermoAverage3();
            case 4: return this.getThermoAverage4();
            case 5: return this.getThermoAverage5();
            case 6: return this.getThermoAverage6();
            case 7: return this.getThermoAverage7();
        }
        return INVALID_TEMPERATURE;
    }

    public void setThermoAverage(int ndx, double v)
    {
        switch (ndx) {
            case 0: this.setThermoAverage0(v); break;
            case 1: this.setThermoAverage1(v); break;
            case 2: this.setThermoAverage2(v); break;
            case 3: this.setThermoAverage3(v); break;
            case 4: this.setThermoAverage4(v); break;
            case 5: this.setThermoAverage5(v); break;
            case 6: this.setThermoAverage6(v); break;
            case 7: this.setThermoAverage7(v); break;
        }
    }

    public void clearThermoAverage()
    {
        for (int n = 0; n <= 7; n++) {
            this.setThermoAverage(n, INVALID_TEMPERATURE);
        }
    }

    public double getThermoAverage0()
    {
        return this.getFieldValue(FLD_thermoAverage0, INVALID_TEMPERATURE);
    }
    public void setThermoAverage0(double v)
    {
        this.setFieldValue(FLD_thermoAverage0, v);
    }

    public double getThermoAverage1()
    {
        return this.getFieldValue(FLD_thermoAverage1, INVALID_TEMPERATURE);
    }
    public void setThermoAverage1(double v)
    {
        this.setFieldValue(FLD_thermoAverage1, v);
    }

    public double getThermoAverage2()
    {
        return this.getFieldValue(FLD_thermoAverage2, INVALID_TEMPERATURE);
    }
    public void setThermoAverage2(double v)
    {
        this.setFieldValue(FLD_thermoAverage2, v);
    }

    public double getThermoAverage3()
    {
        return this.getFieldValue(FLD_thermoAverage3, INVALID_TEMPERATURE);
    }
    public void setThermoAverage3(double v)
    {
        this.setFieldValue(FLD_thermoAverage3, v);
    }

    public double getThermoAverage4()
    {
        return this.getFieldValue(FLD_thermoAverage4, INVALID_TEMPERATURE);
    }
    public void setThermoAverage4(double v)
    {
        this.setFieldValue(FLD_thermoAverage4, v);
    }

    public double getThermoAverage5()
    {
        return this.getFieldValue(FLD_thermoAverage5, INVALID_TEMPERATURE);
    }
    public void setThermoAverage5(double v)
    {
        this.setFieldValue(FLD_thermoAverage5, v);
    }

    public double getThermoAverage6()
    {
        return this.getFieldValue(FLD_thermoAverage6, INVALID_TEMPERATURE);
    }
    public void setThermoAverage6(double v)
    {
        this.setFieldValue(FLD_thermoAverage6, v);
    }

    public double getThermoAverage7()
    {
        return this.getFieldValue(FLD_thermoAverage7, INVALID_TEMPERATURE);
    }
    public void setThermoAverage7(double v)
    {
        this.setFieldValue(FLD_thermoAverage7, v);
    }

    // Temerature Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Analog fields below

    public double getAnalog(int ndx)
    {
        switch (ndx) {
            case 0: return this.getAnalog0();
            case 1: return this.getAnalog1();
            case 2: return this.getAnalog2();
            case 3: return this.getAnalog3();
        }
        return 0.0;
    }

    public void setAnalog(int ndx, double v)
    {
        switch (ndx) {
            case 0: this.setAnalog0(v); break;
            case 1: this.setAnalog1(v); break;
            case 2: this.setAnalog2(v); break;
            case 3: this.setAnalog3(v); break;
        }
    }

    public double getAnalog0()
    {
        return this.getFieldValue(FLD_analog0, 0.0);
    }
    public void setAnalog0(double v)
    {
        this.setFieldValue(FLD_analog0, v);
    }

    public double getAnalog1()
    {
        return this.getFieldValue(FLD_analog1, 0.0);
    }
    public void setAnalog1(double v)
    {
        this.setFieldValue(FLD_analog1, v);
    }

    public double getAnalog2()
    {
        return this.getFieldValue(FLD_analog2, 0.0);
    }
    public void setAnalog2(double v)
    {
        this.setFieldValue(FLD_analog2, v);
    }

    public double getAnalog3()
    {
        return this.getFieldValue(FLD_analog3, 0.0);
    }
    public void setAnalog3(double v)
    {
        this.setFieldValue(FLD_analog3, v);
    }

    public double getPulseCount()
    {
        return this.getFieldValue(FLD_pulseCount, 0.0);
    }
    public void setPulseCount(double v)
    {
        this.setFieldValue(FLD_pulseCount, v);
    }

    // Analog fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Trip Summary fields below

    /* Trip Start timestamp */
    public long getTripStartTime()
    {
        return this.getFieldValue(FLD_tripStartTime, 0L);
    }
    public void setTripStartTime(long v)
    {
        this.setFieldValue(FLD_tripStartTime, ((v >= 0L)? v : 0L));
    }

    /* Trip Stop timestamp */
    public long getTripStopTime()
    {
        return this.getFieldValue(FLD_tripStopTime, 0L);
    }
    public void setTripStopTime(long v)
    {
        this.setFieldValue(FLD_tripStopTime, ((v >= 0L)? v : 0L));
    }

    /* Trip Distance */
    public double getTripDistanceKM()
    {
        return this.getFieldValue(FLD_tripDistanceKM, 0.0);
    }
    public void setTripDistanceKM(double v)
    {
        this.setFieldValue(FLD_tripDistanceKM, ((v >= 0.0)? v : 0.0));
    }

    /* Trip Idle Hours */
    public double getTripIdleHours()
    {
        return this.getFieldValue(FLD_tripIdleHours, 0.0);
    }
    public void setTripIdleHours(double v)
    {
        this.setFieldValue(FLD_tripIdleHours, ((v >= 0.0)? v : 0.0));
    }

    /* Trip Moving Hours */
    /*
    public double getTripMovingHours()
    {
        return this.getFieldValue(FLD_tripMovingHours, 0.0);
    }
    public void setTripMovingHours(double v)
    {
        this.setFieldValue(FLD_tripMovingHours, ((v >= 0.0)? v : 0.0));
    }
    */

    /* Trip PTO Hours */
    public double getTripPtoHours()
    {
        return this.getFieldValue(FLD_tripPtoHours, 0.0);
    }
    public void setTripPtoHours(double v)
    {
        this.setFieldValue(FLD_tripPtoHours, ((v >= 0.0)? v : 0.0));
    }

    /* Trip Max Speed km/h */
    public double getTripMaxSpeedKPH()
    {
        return this.getFieldValue(FLD_tripMaxSpeedKPH, 0.0);
    }
    public void setTripMaxSpeedKPH(double v)
    {
        this.setFieldValue(FLD_tripMaxSpeedKPH, ((v >= 0.0)? v : 0.0));
    }

    /* Trip Max RPM */
    public long getTripMaxRpm()
    {
        return this.getFieldValue(FLD_tripMaxRpm, 0L);
    }
    public void setTripMaxRpm(long v)
    {
        this.setFieldValue(FLD_tripMaxRpm, ((v >= 0L)? v : 0L));
    }

    /* Trip Start Latitude */
    public double getTripStartLatitude()
    {
        return this.getFieldValue(FLD_tripStartLatitude, 0.0);
    }
    public void setTripStartLatitude(double v)
    {
        this.setFieldValue(FLD_tripStartLatitude, v);
    }

    /* Trip Start Longitude */
    public double getTripStartLongitude()
    {
        return this.getFieldValue(FLD_tripStartLongitude, 0.0);
    }
    public void setTripStartLongitude(double v)
    {
        this.setFieldValue(FLD_tripStartLongitude, v);
    }

    /* Trip Elapsed Seconds */
    public long getTripElapsedSeconds()
    {
        return this.getFieldValue(FLD_tripElapsedSeconds, 0L);
    }
    public void setTripElapsedSeconds(long v)
    {
        this.setFieldValue(FLD_tripElapsedSeconds, ((v >= 0L)? v : 0L));
    }

    /* Trip Brake Count */
    public long getTripBrakeCount()
    {
        return this.getFieldValue(FLD_tripBrakeCount, 0L);
    }
    public void setTripBrakeCount(long v)
    {
        this.setFieldValue(FLD_tripBrakeCount, ((v >= 0L)? v : 0L));
    }

    /* Trip Clutch Count */
    public long getTripClutchCount()
    {
        return this.getFieldValue(FLD_tripClutchCount, 0L);
    }
    public void setTripClutchCount(long v)
    {
        this.setFieldValue(FLD_tripClutchCount, ((v >= 0L)? v : 0L));
    }

    // Trip Summary fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Day Summary fields below

    /* EOD number of engines start */
    public int getDayEngineStarts()
    {
        return this.getFieldValue(FLD_dayEngineStarts, 0);
    }
    public void setDayEngineStarts(int v)
    {
        this.setFieldValue(FLD_dayEngineStarts, v);
    }

    /* EOD idle hours */
    public double getDayIdleHours()
    {
        return this.getFieldValue(FLD_dayIdleHours, 0.0);
    }
    public void setDayIdleHours(double v)
    {
        this.setFieldValue(FLD_dayIdleHours, v);
    }

    /* EOD idle fuel */
    public double getDayFuelIdle()
    {
        return this.getFieldValue(FLD_dayFuelIdle, 0.0);
    }
    public void setDayFuelIdle(double v)
    {
        this.setFieldValue(FLD_dayFuelIdle, v);
    }

    /* EOD work hours */
    public double getDayWorkHours()
    {
        return this.getFieldValue(FLD_dayWorkHours, 0.0);
    }
    public void setDayWorkHours(double v)
    {
        this.setFieldValue(FLD_dayWorkHours, v);
    }

    /* EOD work fuel */
    public double getDayFuelWork()
    {
        return this.getFieldValue(FLD_dayFuelWork, 0.0);
    }
    public void setDayFuelWork(double v)
    {
        this.setFieldValue(FLD_dayFuelWork, v);
    }

    /* EOD PTO fuel */
    public double getDayFuelPTO()
    {
        return this.getFieldValue(FLD_dayFuelPTO, 0.0);
    }
    public void setDayFuelPTO(double v)
    {
        this.setFieldValue(FLD_dayFuelPTO, v);
    }

    /* EOD distance travelled */
    public double getDayDistanceKM()
    {
        return this.getFieldValue(FLD_dayDistanceKM, 0.0);
    }
    public void setDayDistanceKM(double v)
    {
        this.setFieldValue(FLD_dayDistanceKM, v);
    }

    /* EOD total fuel */
    public double getDayFuelTotal()
    {
        return this.getFieldValue(FLD_dayFuelTotal, 0.0);
    }
    public void setDayFuelTotal(double v)
    {
        this.setFieldValue(FLD_dayFuelTotal, v);
    }

    // Day Summary fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // PCell data fields below

    public boolean canUpdateCellTowerLocation()
    {

        /* CellID info check */
        if (this.getCellTowerID() <= 0) {
            return false;
        } else
        if (this.getMobileCountryCode() < 0) { // could be 0
            return false;
        } else
        if (this.getMobileNetworkCode() < 0) {
            return false;
        }

        /* account check */
        Account acct = this.getAccount();
        if (acct == null) {
            return false;
        }

        /* MobileLocationProvider check */
        MobileLocationProvider mlp = acct.getPrivateLabel().getMobileLocationProvider(); // test for existance
        if ((mlp == null) || !mlp.isEnabled()) {
            return false;
        }

        /* passed */
        return true;

    }
    
    /* update cell tower location ("updateAddress") */
    public Set<String> updateCellTowerLocation()
    {

        /* get Account */
        Account acct = this.getAccount();
        if (acct == null) {
            return null;
        }
        String acctID = this.getAccountID();

        /* MobileLocationProvider */
        BasicPrivateLabel privLabel = acct.getPrivateLabel(); // never null
        MobileLocationProvider mlp = privLabel.getMobileLocationProvider();
        if (mlp == null) {
            if (acct.hasPrivateLabel()) {
                Print.logInfo("[Account '%s'] PrivateLabel '%s' does not define a MobileLocationProvider", acctID, privLabel); 
            } else {
                Print.logInfo("No PrivateLabel (thus no MobileLocationProvider) for Account '%s'", acctID); 
            }
            return null;
        } else
        if (!mlp.isEnabled()) {
            Print.logInfo("MobileLocationProvider disabled: " + mlp.getName());
            return null;
        }

        /* get cell tower location */
        Set<String> updFields = new HashSet<String>();
        CellTower servCT   = this.getServingCellTower();
        CellTower nborCT[] = this.getNeighborCellTowers();
        Print.logInfo("Getting CellTower location: " + mlp.getName());
        MobileLocation ml = mlp.getMobileLocation(servCT, nborCT); // may return null
        if ((ml != null) && ml.hasGeoPoint()) {
            GeoPoint gp = ml.getGeoPoint();
            this.setCellLatitude( gp.getLatitude());
            updFields.add(EventData.FLD_cellLatitude);
            this.setCellLongitude(gp.getLongitude());
            updFields.add(EventData.FLD_cellLongitude);
            if (ml.hasAccuracy()) {
                double accuracyM = ml.getAccuracy();
                this.setCellAccuracy(accuracyM);
                updFields.add(EventData.FLD_cellAccuracy);
                Print.logInfo("CellTower location ["+acctID+"]: " + gp + " [+/- " + accuracyM + " meters]");
            } else {
                Print.logInfo("CellTower location ["+acctID+"]: " + gp);
            }
        } else {
            Print.logInfo("Unable to retrieve CellTower location ["+acctID+"]");
        }

        /* success (but MobileLocationProvider may still not have a valid location) */
        return updFields;

    }
    
    // -------------------------------

    /* Cell Tower Latitude */
    public double getCellLatitude()
    {
        return this.getFieldValue(FLD_cellLatitude, 0.0);
    }
    public void setCellLatitude(double v)
    {
        this.setFieldValue(FLD_cellLatitude, v);
    }

    /* Cell Tower Longitude */
    public double getCellLongitude()
    {
        return this.getFieldValue(FLD_cellLongitude, 0.0);
    }
    public void setCellLongitude(double v)
    {
        this.setFieldValue(FLD_cellLongitude, v);
    }
    
    /* Set Cell Tower GeoPoint */
    public void setCellGeoPoint(GeoPoint gp)
    {
        if (GeoPoint.isValid(gp)) {
            this.setCellLatitude( gp.getLatitude());
            this.setCellLongitude(gp.getLongitude());
        } else {
            this.setCellLatitude( 0.0);
            this.setCellLongitude(0.0);
        }
    }

    /* Get Cell Tower GeoPoint */
    public GeoPoint getCellGeoPoint()
    {
        double lat = this.getCellLatitude();
        double lon = this.getCellLongitude();
        if (GeoPoint.isValid(lat,lon)) {
            return new GeoPoint(lat,lon);
        } else {
            return GeoPoint.INVALID_GEOPOINT;
        }
    }

    /* has cell tower location */
    public boolean hasCellLocation()
    {
        double lat = this.getCellLatitude();
        double lon = this.getCellLongitude();
        return GeoPoint.isValid(lat,lon);
    }

    // -------------------------------

    /* Cell Tower GPS Location Accuracy (meters) */
    public double getCellAccuracy()
    {
        return this.getFieldValue(FLD_cellAccuracy, 0.0);
    }
    public void setCellAccuracy(double v)
    {
        this.setFieldValue(FLD_cellAccuracy, ((v >= 0.0)? v : 0.0));
    }

    // -------------------------------

    /* Gets the Mobile Country Code */
    public int getMobileCountryCode()
    {
        return this.getFieldValue(FLD_mobileCountryCode, 0);
    }
    public void setMobileCountryCode(int v)
    {
        this.setFieldValue(FLD_mobileCountryCode, v);
    }

    /* Mobile Network Code */
    public int getMobileNetworkCode()
    {
        return this.getFieldValue(FLD_mobileNetworkCode, 0);
    }
    public void setMobileNetworkCode(int v)
    {
        this.setFieldValue(FLD_mobileNetworkCode, v);
    }

    /* Cell Timing Advance */
    public int getCellTimingAdvance()
    {
        return this.getFieldValue(FLD_cellTimingAdvance, 0);
    }
    public void setCellTimingAdvance(int v)
    {
        this.setFieldValue(FLD_cellTimingAdvance, v);
    }

    /* Location Area Code */
    public int getLocationAreaCode()
    {
        return this.getFieldValue(FLD_locationAreaCode, 0);
    }
    public void setLocationAreaCode(int v)
    {
        this.setFieldValue(FLD_locationAreaCode, v);
    }

    /* Cell Tower ID */
    public int getCellTowerID()
    {
        return this.getFieldValue(FLD_cellTowerID, 0);
    }
    public void setCellTowerID(int v)
    {
        this.setFieldValue(FLD_cellTowerID, v);
    }

    /* Serving cell proterty information */
    public String getCellServingInfo()
    {
        return this.getFieldValue(FLD_cellServingInfo, "");
    }
    public void setCellServingInfo(String v)
    {
        this.setFieldValue(FLD_cellServingInfo, StringTools.trim(v));
    }

    /* get Serving CellTower instance */
    public CellTower getServingCellTower()
    {
        RTProperties ctp = new RTProperties(this.getCellServingInfo());
        ctp.setInt(CellTower.ARG_MCC, this.getMobileCountryCode());
        ctp.setInt(CellTower.ARG_MNC, this.getMobileNetworkCode());
        ctp.setInt(CellTower.ARG_TAV, this.getCellTimingAdvance());
        ctp.setInt(CellTower.ARG_LAC, this.getLocationAreaCode());
        ctp.setInt(CellTower.ARG_CID, this.getCellTowerID());
        if (ctp.getInt(CellTower.ARG_CID) <= 0) {
            return null;
        } else {
            CellTower ct = new CellTower(ctp);
            if (this.hasCellLocation()) {
                ct.setMobileLocation(this.getCellGeoPoint(), this.getCellAccuracy());
            }
            return ct;
        }
    }

    /* get Serving CellTower instance */
    public void setServingCellTower(CellTower cti)
    {
        if (cti != null) {
            this.setMobileCountryCode(cti.getMobileCountryCode());
            this.setMobileNetworkCode(cti.getMobileNetworkCode());
            this.setCellTimingAdvance(cti.getTimingAdvance()    );
            this.setLocationAreaCode( cti.getLocationAreaCode() );
            this.setCellTowerID(      cti.getCellTowerID()      );
            this.setCellServingInfo(  cti.toString()            );
        } else {
            this.setMobileCountryCode(0);
            this.setMobileNetworkCode(0);
            this.setCellTimingAdvance(0);
            this.setLocationAreaCode( 0);
            this.setCellTowerID(      0);
            this.setCellServingInfo(  null);
        }
    }
    
    // -------------------------------
    
    /* Nehghbor #0 cell proterty information */
    public String getCellNeighborInfo0()
    {
        return this.getFieldValue(FLD_cellNeighborInfo0, "");
    }
    public void setCellNeighborInfo0(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo0, StringTools.trim(v));
    }

    /* Nehghbor #1 cell proterty information */
    public String getCellNeighborInfo1()
    {
        return this.getFieldValue(FLD_cellNeighborInfo1, "");
    }
    public void setCellNeighborInfo1(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo1, StringTools.trim(v));
    }

    /* Nehghbor #2 cell proterty information */
    public String getCellNeighborInfo2()
    {
        return this.getFieldValue(FLD_cellNeighborInfo2, "");
    }
    public void setCellNeighborInfo2(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo2, StringTools.trim(v));
    }

    /* Nehghbor #3 cell proterty information */
    public String getCellNeighborInfo3()
    {
        return this.getFieldValue(FLD_cellNeighborInfo3, "");
    }
    public void setCellNeighborInfo3(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo3, StringTools.trim(v));
    }

    /* Nehghbor #4 cell proterty information */
    public String getCellNeighborInfo4()
    {
        return this.getFieldValue(FLD_cellNeighborInfo4, "");
    }
    public void setCellNeighborInfo4(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo4, StringTools.trim(v));
    }

    /* Nehghbor #5 cell proterty information */
    public String getCellNeighborInfo5()
    {
        return this.getFieldValue(FLD_cellNeighborInfo5, "");
    }
    public void setCellNeighborInfo5(String v)
    {
        this.setFieldValue(FLD_cellNeighborInfo5, StringTools.trim(v));
    }

    /* get Neighbor CellTower instance */
    public CellTower getNeighborCellTower(int ndx)
    {
        String cts = null;
        switch (ndx) {
            case 0: cts = this.getCellNeighborInfo0();  break;
            case 1: cts = this.getCellNeighborInfo1();  break;
            case 2: cts = this.getCellNeighborInfo2();  break;
            case 3: cts = this.getCellNeighborInfo3();  break;
            case 4: cts = this.getCellNeighborInfo4();  break;
            case 5: cts = this.getCellNeighborInfo5();  break;
        }
        if (!StringTools.isBlank(cts)) {
            return new CellTower(new RTProperties(cts));
        } else {
            return null;
        }
    }
    public void setNeighborCellTower(int ndx, CellTower cti)
    {
        String cts = (cti != null)? cti.toString() : null;
        switch (ndx) {
            case 0: this.setCellNeighborInfo0(cts);  break;
            case 1: this.setCellNeighborInfo1(cts);  break;
            case 2: this.setCellNeighborInfo2(cts);  break;
            case 3: this.setCellNeighborInfo3(cts);  break;
            case 4: this.setCellNeighborInfo4(cts);  break;
            case 5: this.setCellNeighborInfo5(cts);  break;
        }
    }

    /* get all Neighbor CellTower instances (if any) */
    public CellTower[] getNeighborCellTowers()
    {
        Collection<CellTower> nctList = null;
        for (int n = 0; n < COUNT_cellNeighborInfo; n++) {
            CellTower ct = this.getNeighborCellTower(n);
            if (ct != null) {
                if (nctList == null) { nctList = new Vector<CellTower>(); }
                nctList.add(ct);
            }
        }
        return (nctList != null)? nctList.toArray(new CellTower[nctList.size()]) : null;
    }
    public void setNeighborCellTowers(CellTower nct[])
    {
        int nctLen = ListTools.size(nct);
        for (int n = 0; n < COUNT_cellNeighborInfo; n++) {
            CellTower ct = (nctLen > n)? nct[n] : null;
            this.setNeighborCellTower(n, ct);
        }
    }
    public void setNeighborCellTowers(java.util.List<CellTower> nct)
    {
        int nctLen = ListTools.size(nct);
        for (int n = 0; n < COUNT_cellNeighborInfo; n++) {
            CellTower ct = (nctLen > n)? nct.get(n) : null;
            this.setNeighborCellTower(n, ct);
        }
    }

    // PCell data fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Custom Bean access fields below

    private Driver driver = null;

    /**
    *** Returns true if thie event has a defined driver-id
    *** @return True if thie event has a defined driver-id
    **/
    public boolean hasDriverID()
    {
        return !StringTools.isBlank(this.getDriverID());
    }

    /**
    *** Gets the driver id
    *** @return The driver id
    **/
    public String getDriverID()
    {
        return this.getFieldValue(FLD_driverID, "");
    }

    /**
    *** Sets the driver id
    *** @param v The driver id
    **/
    public void setDriverID(String v)
    {
        this.setFieldValue(FLD_driverID, StringTools.trim(v));
        this.driver = null;
    }

    /**
    *** Gets the driver id
    *** @param chkDevIfBlank  If true, and the event driver-id is blank, then this method
    ***                       will also check the parent device record.
    *** @return The driver id
    **/
    public String getDriverID(boolean chkDevIfBlank)
    {

        /* EventData DriverID? */
        String drvID = this.getDriverID();
        if (!StringTools.isBlank(drvID)) {
            return drvID;
        }

        /* get Device DriverID */
        if (chkDevIfBlank) {
            Device dev = this.getDevice();
            if (dev != null) {
                return dev.getDriverID();
            }
        }

        /* DriverID not found */
        return "";

    }

    /**
    *** Gets the driver record instance
    *** @param chkDevIfBlank  If true, and the event driver-id is blank, then this method
    ***                       will also check the parent device record.
    *** @return The driver record
    **/
    public Driver getDriver(boolean chkDevIfBlank)
    {
        if (this.driver == null) {
            String driverID = this.getDriverID(chkDevIfBlank);
            if (!StringTools.isBlank(driverID)) {
                try {
                    this.driver = Driver.getDriver(this.getAccount(), driverID);
                } catch (DBException dbe) {
                    //Print.logWarn("Error reading Driver: " + dbe);
                    this.driver = null;
                }
                if (this.driver != null) { 
                    //Print.logInfo("Found Driver record: " + this.getAccountID() + "/" + driverID); 
                } else {
                    //Print.logWarn("Driver not found: " + this.getAccountID() + "/" + driverID); 
                }
            }
        } else {
            // update?
        }
        return this.driver;
    }

    // ------------------------------------------------------------------------
    // Driver status

    /**
    *** Returns true if this event has a defined driver/duty-status (value > 0)
    *** @return True if this event has a defined driver/duty-status
    **/
    public boolean hasDriverStatus()
    {
        return (this.getDriverStatus() > Driver.DutyStatus_UNKNOWN)? true : false;
    }

    /**
    *** Gets the driver/duty status
    *** @return The driver/duty status
    **/
    public long getDriverStatus()
    {
        return this.getFieldValue(FLD_driverStatus, Driver.DutyStatus_UNKNOWN);
    }

    /**
    *** Sets the driver/duty status
    *** @param v The driver/duty status
    **/
    public void setDriverStatus(long v)
    {
        long ds = (v >= Driver.DutyStatus_UNKNOWN)? v : Driver.DutyStatus_UNKNOWN;
        this.setFieldValue(FLD_driverStatus, ds);
    }

    // ------------------------------------------------------------------------
    // Driver message

    /**
    *** Returns true if a driver message is defined
    *** @return True if a driver message is defined
    **/
    public boolean hasDriverMessage()
    {
        return !StringTools.isBlank(this.getDriverMessage());
    }

    /**
    *** Gets the driver message
    *** @return The driver message
    **/
    public String getDriverMessage()
    {
        return this.getFieldValue(FLD_driverMessage, "");
    }
    
    /**
    *** Sets the driver message
    *** @param v The driver message
    **/
    public void setDriverMessage(String v)
    {
        this.setFieldValue(FLD_driverMessage, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    // EMail recipient

    /**
    *** Gets the email recipient
    *** @return The email recipient
    **/
    public String getEmailRecipient()
    {
        String v = (String)this.getFieldValue(FLD_emailRecipient);
        return StringTools.trim(v);
    }

    /**
    *** Sets the email recipient
    *** @param v The email recipient
    **/
    public void setEmailRecipient(String v)
    {
        this.setFieldValue(FLD_emailRecipient, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the low sensor value
    *** @return The low sensor value
    **/
    public long getSensorLow()
    {
        return this.getFieldValue(FLD_sensorLow, 0L);
    }
    
    /**
    *** Sets the low sensor value
    *** @param v The low sensor value
    **/
    public void setSensorLow(long v)
    {
        this.setFieldValue(FLD_sensorLow, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the high sensor value
    *** @return The high sensor value
    **/
    public long getSensorHigh()
    {
        return this.getFieldValue(FLD_sensorHigh, 0L);
    }

    /**
    *** Sets the high sensor value
    *** @param v The high sensor value
    **/
    public void setSensorHigh(long v)
    {
        this.setFieldValue(FLD_sensorHigh, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the sample index
    *** @return The sample index
    **/
    public int getSampleIndex()
    {
        return this.getFieldValue(FLD_sampleIndex, 0);
    }

    /**
    *** Sets the sample index
    *** @param v The sample index
    **/
    public void setSampleIndex(int v)
    {
        this.setFieldValue(FLD_sampleIndex, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the sample ID
    *** @return The sample ID
    **/
    public String getSampleID()
    {
        String v = (String)this.getFieldValue(FLD_sampleID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the sample ID
    *** @param v The sample ID
    **/
    public void setSampleID(String v)
    {
        this.setFieldValue(FLD_sampleID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* Gets the Sample Index */
    /*
    public double getAppliedPressure()
    {
        return this.getFieldValue(FLD_appliedPressure, 0.0); // kPa
    }
    
    public void setAppliedPressure(double v)
    {
        this.setFieldValue(FLD_appliedPressure, v);
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Gets the lease starting day number
    *** @return The lease starting day number
    **/
    public long getLeaseStartDate()
    {
        // DayNumber startDate = new DayNumber(evd.getLeaseStartDate());
        return this.getFieldValue(FLD_leaseStartDate, 0L);
    }

    /**
    *** Sets the lease starting day number
    *** @param v The lease starting day number
    **/
    public void setLeaseStartDate(long v)
    {
        this.setFieldValue(FLD_leaseStartDate, ((v >= 0L)? v : 0L));
    }

    /**
    *** Sets the lease starting date
    *** @param year  The lease starting year
    *** @param month The lease starting month (1..12)
    *** @param day   The lease starting day of month
    **/
    public void setLeaseStartDate(int year, int month, int day)
    {
        this.setLeaseStartDate(DateTime.getDayNumberFromDate(year, month, day));
    }

    /**
    *** Sets the lease starting day number
    *** @param dn The lease starting day number
    **/
    public void setLeaseStartDate(DayNumber dn)
    {
        this.setLeaseStartDate((dn != null)? dn.getDayNumber() : 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the lease ending day number
    *** @return The lease ending day number
    **/
    public long getLeaseEndDate()
    {
        // DayNumber endDate = new DayNumber(evd.getLeaseEndDate());
        return this.getFieldValue(FLD_leaseEndDate, 0L);
    }

    /**
    *** Sets the lease ending day number
    *** @param v The lease ending day number
    **/
    public void setLeaseEndDate(long v)
    {
        this.setFieldValue(FLD_leaseEndDate, ((v >= 0L)? v : 0L));
    }

    /**
    *** Sets the lease ending date
    *** @param year  The lease ending year
    *** @param month The lease ending month (1..12)
    *** @param day   The lease ending day of month
    **/
    public void setLeaseEndDate(int year, int month, int day)
    {
        this.setLeaseEndDate(DateTime.getDayNumberFromDate(year, month, day));
    }

    /**
    *** Sets the lease ending day number
    *** @param dn The lease ending day number
    **/
    public void setLeaseEndDate(DayNumber dn)
    {
        this.setLeaseEndDate((dn != null)? dn.getDayNumber() : 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the starting odometer at the beginning of the lease
    *** @return The starting odometer at the beginning of the lease
    **/
    public double getLeaseStartOdomKM()
    {
        return this.getFieldValue(FLD_leaseStartOdomKM, 0.0);
    }

    /**
    *** Sets the starting odometer at the beginning of the lease
    *** @param v The starting odometer at the beginning of the lease
    **/
    public void setLeaseStartOdomKM(double v)
    {
        this.setFieldValue(FLD_leaseStartOdomKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the allowed distance over the term of the lease
    *** @return The allowed distance over the term of the lease
    **/
    public double getLeaseAllowedDistKM()
    {
        return this.getFieldValue(FLD_leaseAllowedDistKM, 0.0);
    }

    /**
    *** Sets the allowed distance over the term of the lease
    *** @param v The allowed distance over the term of the lease
    **/
    public void setLeaseAllowedDistKM(double v)
    {
        this.setFieldValue(FLD_leaseAllowedDistKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the lease contract id/number
    *** @return The lease contract id/number
    **/
    public String getLeaseContractID()
    {
        String v = (String)this.getFieldValue(FLD_leaseContractID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the lease contract id/number
    *** @param v The lease contract id/number
    **/
    public void setLeaseContractID(String v)
    {
        this.setFieldValue(FLD_leaseContractID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the brake G-force
    *** @return The brake G-force
    **/
    public double getBrakeGForce()
    {
        return this.getFieldValue(FLD_brakeGForce, 0.0);
    }

    /**
    *** Sets the brake G-force
    *** @param v The brake G-force
    **/
    public void setBrakeGForce(double v)
    {
        this.setFieldValue(FLD_brakeGForce, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the acceleration (meters-per-second-squared)
    *** @return The acceleration (m/s/s)
    **/
    public double getAcceleration()
    {
        return this.getFieldValue(FLD_acceleration, 0.0);
    }

    /**
    *** Sets the acceleration (meters-per-second-squared)
    *** @param v The acceleration (m/s/s)
    **/
    public void setAcceleration(double v)
    {
        this.setFieldValue(FLD_acceleration, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the accelerometer XYZ-axis values (meters-per-second-squared)
    *** @return The accelerometer XYZ-axis values (m/s/s)
    **/
    public String getAccelerometerXYZ()
    {
        String v = (String)this.getFieldValue(FLD_accelerometerXYZ);
        return StringTools.trim(v);
    }

    /**
    *** Sets the accelerometer XYZ-axis values (meters-per-second-squared)
    *** @param v The accelerometer XYZ-axis values (m/s/s)
    **/
    public void setAccelerometerXYZ(String v)
    {
        this.setFieldValue(FLD_accelerometerXYZ, StringTools.trim(v));
    }

    /**
    *** Gets the accelerometer XYZ-axis values (meters-per-second-squared)
    *** @return The accelerometer XYZ-axis values (m/s/s)
    **/
    public Accelerometer getAccelerometer()
    {
        String xyz = this.getAccelerometerXYZ();
        return new Accelerometer(xyz);
    }

    /**
    *** Gets the accelerometer XYZ-axis values (meters-per-second-squared)
    *** @param xyz  The accelerometer XYZ-axis values (m/s/s)
    **/
    public void setAccelerometer(Accelerometer xyz)
    {
        String v = (xyz != null)? xyz.toString() : "";
        this.setAccelerometerXYZ(v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the brake pressure
    *** @return The brake pressure
    **/
    public double getBrakePressure()
    {
        return this.getFieldValue(FLD_brakePressure, 0.0); // kPa
    }

    /**
    *** Sets the brake pressure
    *** @param v The brake pressure
    **/
    public void setBrakePressure(double v)
    {
        this.setFieldValue(FLD_brakePressure, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the mass air flow rate (g/sec)
    *** @return The mass air flow rate (g/sec)
    **/
    public double getMassAirFlowRate()
    {
        return this.getFieldValue(FLD_massAirFlowRate, 0.0); // grams/sec
    }

    /**
    *** Sets the mass air flow rate (g/sec)
    *** @param v The mass air flow rate (g/sec)
    **/
    public void setMassAirFlowRate(double v)
    {
        this.setFieldValue(FLD_massAirFlowRate, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the tire pressure list as a comma separated String
    *** @return The tire pressure string
    **/
    public String getTirePressure()
    {
        return this.getFieldValue(FLD_tirePressure, ""); // kPa
    }

    /**
    *** Gets an array of tire pressures (kPa)
    *** @return An array of tire pressures (kPa)
    **/
    public double[] getTirePressure_kPa()
    {
        String kPaStrArr = this.getTirePressure();
        if (StringTools.isBlank(kPaStrArr)) {
            return new double[0];
        } else {
            String kPaStr[] = StringTools.split(kPaStrArr,',');
            double kPaArr[] = new double[kPaStr.length];
            for (int i = 0; i < kPaStr.length; i++) {
                kPaArr[i] = StringTools.parseDouble(kPaStr[i],0.0);
            }
            return kPaArr;
        }
    }

    /**
    *** Gets an array of tire pressures (PSI)
    *** @return An array of tire pressures (PSI)
    **/
    public double[] getTirePressure_psi()
    {
        String kPaStrArr = this.getTirePressure();
        if (StringTools.isBlank(kPaStrArr)) {
            return new double[0];
        } else {
            String kPaStr[] = StringTools.split(kPaStrArr,',');
            double psiArr[] = new double[kPaStr.length];
            for (int i = 0; i < kPaStr.length; i++) {
                double kPa = StringTools.parseDouble(kPaStr[i],0.0);
                psiArr[i] = kPa * Account.PSI_PER_KPA;
            }
            return psiArr;
        }
    }

    /**
    *** Gets an array of tire pressures in the specified units
    *** @return An array of tire pressures in the specified units
    **/
    public double[] getTirePressure_units(Account.PressureUnits pu)
    {
        String kPaStrArr = this.getTirePressure();
        if (StringTools.isBlank(kPaStrArr)) {
            return new double[0];
        } else {
            if (pu == null) { pu = EnumTools.getDefault(Account.PressureUnits.class); }
            String kPaStr[] = StringTools.split(kPaStrArr,',');
            double uniArr[] = new double[kPaStr.length];
            for (int i = 0; i < kPaStr.length; i++) {
                double kPa = StringTools.parseDouble(kPaStr[i],0.0);
                uniArr[i] = pu.convertFromKPa(kPa);
            }
            return uniArr;
        }
    }

    /**
    *** Sets the tire pressure list as a comma separated String
    *** @param v The tire pressure string
    **/
    public void setTirePressure(String v)
    {
        this.setFieldValue(FLD_tirePressure, v);
    }

    /**
    *** Sets an array of tire pressures (kPa)
    *** @param v An array of tire pressures (kPa)
    **/
    public void setTirePressure_kPa(double v[])
    {
        StringBuffer sb = new StringBuffer();
        if (!ListTools.isEmpty(v)) {
            for (int i = 0; i < v.length; i++) {
                double kPa = v[i];  // kPa ==> kPa
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(kPa,"0.0"));
            }
        }
        this.setTirePressure(sb.toString());
    }

    /**
    *** Sets an array of tire pressures (PSI)
    *** @param v An array of tire pressures (PSI)
    **/
    public void setTirePressure_psi(double v[])
    {
        StringBuffer sb = new StringBuffer();
        if (!ListTools.isEmpty(v)) {
            for (int i = 0; i < v.length; i++) {
                double kPa = v[i] / Account.PSI_PER_KPA; // psi * kPa/psi ==> kPa
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(kPa,"0.0"));
            }
        }
        this.setTirePressure(sb.toString());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the tire temperature list as a comma separated String
    *** @return The tire temperature string
    **/
    public String getTireTemp()
    {
        return this.getFieldValue(FLD_tireTemp, ""); // C
    }

    /**
    *** Gets an array of tire temperatures (C)
    *** @return An array of tire temperatures (C)
    **/
    public double[] getTireTemp_C()
    {
        String cStrArr = this.getTireTemp();
        if (StringTools.isBlank(cStrArr)) {
            return new double[0];
        } else {
            String cStr[] = StringTools.split(cStrArr, ',');
            double cArr[] = new double[cStr.length];
            for (int i = 0; i < cStr.length; i++) {
                double C = StringTools.parseDouble(cStr[i],0.0);
                cArr[i] = C;
            }
            return cArr;
        }
    }

    /**
    *** Gets an array of tire temperatures in the specified units
    *** @return An array of tire temperatures in the specified units
    **/
    public double[] getTireTemp_units(Account.TemperatureUnits tu)
    {
        String cStrArr = this.getTireTemp();
        if (StringTools.isBlank(cStrArr)) {
            return new double[0];
        } else {
            if (tu == null) { tu = EnumTools.getDefault(Account.TemperatureUnits.class); }
            String cStr[] = StringTools.split(cStrArr,',');
            double uArr[] = new double[cStr.length];
            for (int i = 0; i < cStr.length; i++) {
                double C = StringTools.parseDouble(cStr[i],0.0);
                uArr[i] = tu.convertFromC(C);
            }
            return uArr;
        }
    }

    /**
    *** Sets the tire temperature list as a comma separated String
    *** @param v The tire temperature string
    **/
    public void setTireTemp(String v)
    {
        this.setFieldValue(FLD_tireTemp, v);
    }

    /**
    *** Sets an array of tire temperatures (C)
    *** @param v An array of tire temperatures (C)
    **/
    public void setTireTemp_C(double v[])
    {
        StringBuffer sb = new StringBuffer();
        if (!ListTools.isEmpty(v)) {
            for (int i = 0; i < v.length; i++) {
                double C = v[i];  // C ==> C
                if (sb.length() > 0) { sb.append(","); }
                sb.append(StringTools.format(C,"0.0"));
            }
        }
        this.setTireTemp(sb.toString());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the general tank level
    *** @return The general tank level
    **/
    public double getTankLevel()
    {
        return this.getFieldValue(FLD_tankLevel, 0.0);
    }

    /**
    *** Sets the general tank level
    *** @param v The general tank level
    **/
    public void setTankLevel(double v)
    {
        this.setFieldValue(FLD_tankLevel, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the data-push flag
    *** @return The data-push flag
    **/
    public boolean getDataPush()
    {
        return this.getFieldValue(FLD_dataPush, true);
    }

    /**
    *** Sets the data-push flag
    *** @param v The data-push flag
    **/
    public void setDataPush(boolean v)
    {
        this.setFieldValue(FLD_dataPush, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the cost center
    *** @return The cost center
    **/
    public long getCostCenter()
    {
        return this.getFieldValue(FLD_costCenter, 0L);
    }

    /**
    *** Sets the cost center
    *** @param v The cost center
    **/
    public void setCostCenter(long v)
    {
        this.setFieldValue(FLD_costCenter, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the job number
    *** @return The job number
    **/
    public String getJobNumber()
    {
        return this.getFieldValue(FLD_jobNumber, "");
    }

    /**
    *** Sets the job number
    *** @param v The job number
    **/
    public void setJobNumber(String v)
    {
        this.setFieldValue(FLD_jobNumber, StringTools.trim(v));
    }

    /**
    *** Returns true if Job number is defined
    *** @return True if Job number is defined
    **/
    public boolean hasJobNumber()
    {
        return !StringTools.isBlank(this.getJobNumber());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the stored RFID tag
    *** @return The stored RFID tag
    **/
    public String getRfidTag()
    {
        return this.getFieldValue(FLD_rfidTag, "");
    }

    /**
    *** Sets the stored RFID tag
    *** @param v The stored RFID tag
    **/
    public void setRfidTag(String v)
    {
        this.setFieldValue(FLD_rfidTag, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    
    private RTProperties attachRTProp = null;

    /**
    *** Gets the attachment MIME type (used with "attachData")
    *** @return The attachment MIME type
    **/
    public String getAttachType() // MIME type
    {
        return this.getFieldValue(FLD_attachType, ""); // image: jpeg, gif, png, etc
    }

    /**
    *** Sets the attachment MIME type (used with "attachData")
    *** @param v The attachment MIME type
    **/
    public void setAttachType(String v)
    {
        this.setFieldValue(FLD_attachType, StringTools.trim(v));
        this.attachRTProp = null;
    }
    
    /** 
    *** Returns true if the attachment type matches the specified type
    *** @param type  The target attachment type
    *** @return True if match, false otherwise
    **/
    public boolean isAttachType(String type)
    {
        String attachType = this.getAttachType();
        return attachType.equalsIgnoreCase(type);
    }

    /**
    *** Returns true if this event record has attached data
    *** @return True if this event record has attached data
    **/
    public boolean hasAttachData()
    {
        byte v[] = (byte[])this.getFieldValue(FLD_attachData); // image bytes, etc
        return !ListTools.isEmpty(v);
    }

    /**
    *** Gets the attachment data
    *** @return The attachment data
    **/
    public byte[] getAttachData()
    {
        byte v[] = (byte[])this.getFieldValue(FLD_attachData); // image bytes, etc
        return (v != null)? v : new byte[0];
    }

    /**
    *** Sets the attachment data
    *** @param v The attachment data
    **/
    public void setAttachData(byte[] v)
    {
        this.setFieldValue(FLD_attachData, ((v != null)? v : new byte[0]));
        this.attachRTProp = null;
    }

    /**
    *** Sets the attachment data
    *** @param mimeType The MIME type
    *** @param data     The attachment data
    **/
    public void setAttachment(String mimeType, byte data[])
    {
        if (ListTools.isEmpty(data)) {
            this.setAttachType(null);
            this.setAttachData(null);
        } else {
            this.setAttachType(StringTools.isBlank(mimeType)? 
                mimeType : HTMLTools.getMimeTypeFromData(data,null));
            this.setAttachData(data);
        }
    }

    /**
    *** Gets the attachment as an RTProperties instance.
    *** @return The attachment data converted to an RTProperties instance, or null if
    ***         unable to convert attachment to an RTProperties instance.
    **/
    public RTProperties getAttachRTProperties()
    {
        if (this.attachRTProp != null) {
            // -- already cached
            return this.attachRTProp;
        } else
        if (this.hasAttachData() && this.isAttachType(HTMLTools.CONTENT_TYPE_RTPROP)) {
            // -- convert to RTProperties
            byte   rtpB[] = this.getAttachData();
            String rtpS   = StringTools.toStringValue(rtpB);
            this.attachRTProp = new RTProperties(rtpS);
            return this.attachRTProp;
        } else {
            // -- invalid attachement type for RTProperties
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA timestamp in Unix/Epoch time
    *** @return The ETA timestamp of this event
    **/
    public long getEtaTimestamp()
    {
        return this.getFieldValue(FLD_etaTimestamp, 0L);
    }

    /**
    *** Sets the ETA timestamp in Unix/Epoch time
    *** @param v The ETA timestamp
    **/
    public void setEtaTimestamp(long v)
    {
        this.setFieldValue(FLD_etaTimestamp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA unique-id 
    *** @return The ETA unique-id 
    **/
    public long getEtaUniqueID()
    {
        return this.getFieldValue(FLD_etaUniqueID, 0L);
    }

    /**
    *** Sets the ETA unique-id
    *** @param v The ETA unique-id
    **/
    public void setEtaUniqueID(long v)
    {
        this.setFieldValue(FLD_etaUniqueID, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA distance in kilometers
    *** @return The ETA distance in kilometers
    **/
    public double getEtaDistanceKM()
    {
        return this.getFieldValue(FLD_etaDistanceKM, 0.0);
    }

    /**
    *** Sets the ETA distance in kilometers
    *** @param v The ETA distance in kilometers
    **/
    public void setEtaDistanceKM(double v)
    {
        this.setFieldValue(FLD_etaDistanceKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA Latitude
    *** @return The ETA Latitude
    **/
    public double getEtaLatitude()
    {
        return this.getFieldValue(FLD_etaLatitude, 0.0);
    }
    
    /**
    *** Sets the ETA Latitude
    *** @param v The ETA Latitude
    **/
    public void setEtaLatitude(double v)
    {
        this.setFieldValue(FLD_etaLatitude, v);
    }
    
    /**
    *** Sets the ETA GeoPoint
    *** @param gp The ETA GeoPoint
    **/
    public void setEtaGeoPoint(GeoPoint gp)
    {
        if ((gp != null) && gp.isValid()) {
            this.setEtaLatitude( gp.getLatitude());
            this.setEtaLongitude(gp.getLongitude());
        } else {
            this.setEtaLatitude( 0.0);
            this.setEtaLongitude(0.0);
        }
    }

    /**
    *** Gets the ETA GeoPoint
    *** @return The ETA GeoPoint
    **/
    public GeoPoint getEtaGeoPoint()
    {
        return new GeoPoint(this.getEtaLatitude(), this.getEtaLongitude());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ETA Longitude
    *** @return The ETA Longitude
    **/
    public double getEtaLongitude()
    {
        return this.getFieldValue(FLD_etaLongitude, 0.0);
    }
    
    /**
    *** Sets the ETA Longitude
    *** @param v The ETA Longitude
    **/
    public void setEtaLongitude(double v)
    {
        this.setFieldValue(FLD_etaLongitude, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the STOP id 
    *** @return The STOP id 
    **/
    public long getStopID()
    {
        return this.getFieldValue(FLD_stopID, 0L);
    }

    /**
    *** Sets the STOP id
    *** @param v The STOP id
    **/
    public void setStopID(long v)
    {
        this.setFieldValue(FLD_stopID, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the STOP Status 
    *** @return The STOP Status 
    **/
    public int getStopStatus()
    {
        return this.getFieldValue(FLD_stopStatus, 0);
    }

    /**
    *** Sets the STOP Status
    *** @param v The STOP Status
    **/
    public void setStopStatus(int v)
    {
        this.setFieldValue(FLD_stopStatus, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the STOP Index 
    *** @return The STOP Index 
    **/
    public int getStopIndex()
    {
        return this.getFieldValue(FLD_stopIndex, 0);
    }

    /**
    *** Sets the STOP Index
    *** @param v The STOP Index
    **/
    public void setStopIndex(int v)
    {
        if ((v < 0) && this.isFieldUnsigned(FLD_stopIndex)) {
            v = 0; // < 0 not allowed for unsigned
        }
        this.setFieldValue(FLD_stopIndex, v);
    }

    // Common Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // J1708 Bean access fields below

    //public int getObdType()
    //{
    //    return this.getFieldValue(FLD_obdType, 0);
    //}
    
    //public void setObdType(int v)
    //{
    //    this.setFieldValue(FLD_obdType, v);
    //}

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel pressure
    *** @return The fuel pressure
    **/
    public double getFuelPressure()
    {
        return this.getFieldValue(FLD_fuelPressure, 0.0); // kPa
    }

    /**
    *** Sets the fuel pressure
    *** @param v The fuel pressure
    **/
    public void setFuelPressure(double v)
    {
        this.setFieldValue(FLD_fuelPressure, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel usage (rate of use) in Litres/Hour
    *** @return The fuel usage (rate of use)
    **/
    public double getFuelUsage()
    {
        return this.getFieldValue(FLD_fuelUsage, 0.0);
    }

    /**
    *** Sets the fuel usage (rate of use) in Litres/Hour
    *** @param v The fuel usage (rate of use)
    **/
    public void setFuelUsage(double v)
    {
        this.setFieldValue(FLD_fuelUsage, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel temperature
    *** @return The fuel temperature
    **/
    public double getFuelTemp()
    {
        return this.getFieldValue(FLD_fuelTemp, 0.0);
    }

    /**
    *** Sets the fuel temperature
    *** @param v The fuel temperature
    **/
    public void setFuelTemp(double v)
    {
        this.setFieldValue(FLD_fuelTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel level
    *** @return The fuel level
    **/
    public double getFuelLevel()
    {
        return this.getFieldValue(FLD_fuelLevel, 0.0);
    }

    /**
    *** Sets the fuel level
    *** @param v The fuel level
    **/
    public void setFuelLevel(double v)
    {
        this.setFieldValue(FLD_fuelLevel, v);
    }

    /**
    *** Gets the fuel-level
    *** @param estimate True to estimate the fuel level based on other criteria
    ***                 available to this event (ie. fuel-remaining, etc)
    *** @return The estimated fuel-level
    **/
    public double getFuelLevel(boolean estimate)
    {
        double fuelLevel = this.getFuelLevel(); // no-estimate
        if (!estimate) {
            return fuelLevel;
        } else
        if (fuelLevel > 0.0) {
            Device device = this.getDevice(); // should not be null
            return (device != null)? device.getActualFuelLevel(fuelLevel) : fuelLevel;
        } else {
            Device device     = this.getDevice(); // should not be null
            double fuelRemain = this.getFuelRemain(false/*noEstimate*/);
            double fuelCapL   = (device != null)? device.getFuelCapacity() : 0.0;
            if ((fuelRemain > 0.0) && (fuelCapL > 0.0)) {
                fuelLevel = fuelRemain / fuelCapL;
                if (fuelLevel > 1.0) { fuelLevel = 1.0; }
                return fuelLevel;
            } else {
                return 0.0;
            }
        }
    }

    /**
    *** Gets the fuel volume in Liters, 
    *** based on the level and estimated fuel capacity
    *** @return The fuel volume
    **/
    public double getFuelLevelVolume_Liters()
    {
        Device device = this.getDevice();
        if (device != null) {
            return this.getFuelLevel(true/*estimate*/) * device.getFuelCapacity();
        } else {
            return 0.0;
        }
    }

    /**
    *** Gets the fuel volume in the Account preferred units, 
    *** based on the level and estimated fuel capacity
    *** @return The fuel volume
    **/
    public double getFuelLevelVolume_Units()
    {
        Account.VolumeUnits vu = Account.getVolumeUnits(this.getAccount());
        return vu.convertFromLiters(this.getFuelLevelVolume_Liters());
    }

    //public String getFuelLevelVolume_Title(Locale locale)
    //{
    //    I18N i18n = I18N.getI18N(EventData.class, locale);
    //    return i18n.getString("EventData.fuelLevelVolume", "Fuel Volume");
    //}

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel level in the alternate/second tank
    *** @return The fuel level in the alternate/second tank
    **/
    public double getFuelLevel2()
    {
        return this.getFieldValue(FLD_fuelLevel2, 0.0);
    }

    /**
    *** Sets the fuel level in the alternate/second tank
    *** @param v The fuel level in the alternate/second tank
    **/
    public void setFuelLevel2(double v)
    {
        this.setFieldValue(FLD_fuelLevel2, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the instantaneous fuel economy
    *** @return The instantaneous fuel economy
    **/
    public double getFuelEconomy()
    {
        // Instantaneous fuel economy.
        // (this value is not as useful as calculating a fuel-economy over a
        // specific period of time or distance travelled).
        return this.getFieldValue(FLD_fuelEconomy, 0.0);
    }

    /**
    *** Sets the instantaneous fuel economy
    *** @param v The instantaneous fuel economy
    **/
    public void setFuelEconomy(double v)
    {
        this.setFieldValue(FLD_fuelEconomy, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the total fuel used
    *** @return The total fuel used
    **/
    public double getFuelTotal()
    {
        return this.getFieldValue(FLD_fuelTotal, 0.0);
    }

    /**
    *** Sets the total fuel used
    *** @param v The total fuel used
    **/
    public void setFuelTotal(double v)
    {
        this.setFieldValue(FLD_fuelTotal, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel remaining in the tank
    *** @return The fuel remaining
    **/
    public double getFuelRemain()
    {
        return this.getFieldValue(FLD_fuelRemain, 0.0);
    }

    /**
    *** Sets the fuel remaining in the tank
    *** @param v The fuel remaining
    **/
    public void setFuelRemain(double v)
    {
        this.setFieldValue(FLD_fuelRemain, v);
    }

    /**
    *** Gets the fuel remaining in the tank
    *** @param estimate True to estimate fuel remaining based on device tank capacity and fuel level
    *** @return The fuel remaining
    **/
    public double getFuelRemain(boolean estimate)
    {
        double fuelRemain = this.getFuelRemain();
        if (!estimate || (fuelRemain > 0.0)) {
            return fuelRemain;
        } else {
            Device device     = this.getDevice(); // should not be null
            double fuelLevel  = this.getFuelLevel(false/*noEstimate*/);
            double fuelCapL   = (device != null)? device.getFuelCapacity() : 0.0;
            if ((fuelLevel > 0.0) && (fuelCapL > 0.0)) {
                fuelRemain = fuelCapL * device.getActualFuelLevel(fuelLevel);
                return fuelRemain;
            } else {
                return 0.0;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the trip fuel used
    *** @return The trip fuel used
    **/
    public double getFuelTrip()
    {
        return this.getFieldValue(FLD_fuelTrip, 0.0);
    }

    /**
    *** Sets the trip fuel used
    *** @param v The trip fuel used
    **/
    public void setFuelTrip(double v)
    {
        this.setFieldValue(FLD_fuelTrip, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the idle fuel used
    *** @return The idle fuel used
    **/
    public double getFuelIdle()
    {
        return this.getFieldValue(FLD_fuelIdle, 0.0);
    }

    /**
    *** Sets the idle fuel used
    *** @param v The idle fuel used
    **/
    public void setFuelIdle(double v)
    {
        this.setFieldValue(FLD_fuelIdle, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the PTO fuel used
    *** @return The PTO fuel used
    **/
    public double getFuelPTO()
    {
        return this.getFieldValue(FLD_fuelPTO, 0.0);
    }

    /**
    *** Sets the PTO fuel used
    *** @param v The PTO fuel used
    **/
    public void setFuelPTO(double v)
    {
        this.setFieldValue(FLD_fuelPTO, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the fuel used since last engine-on
    *** @return The fuel used since last engine-on
    **/
    public double getFuelEngineOn()
    {
        return this.getFieldValue(FLD_fuelEngineOn, 0.0);
    }

    /**
    *** Sets the fuel used since last engine-on
    *** @param v The fuel used since last engine-on
    **/
    public void setFuelEngineOn(double v)
    {
        this.setFieldValue(FLD_fuelEngineOn, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine RPM
    *** @return The engine RPM
    **/
    public long getEngineRpm()
    {
        return this.getFieldValue(FLD_engineRpm, 0L);
    }

    /**
    *** Sets the engine RPM
    *** @param v The engine RPM
    **/
    public void setEngineRpm(long v)
    {
        this.setFieldValue(FLD_engineRpm, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine hours
    *** @return The engine hours
    **/
    public double getEngineHours()
    {
        return this.getFieldValue(FLD_engineHours, 0.0);
    }

    /**
    *** Sets the engine hours
    *** @param v The engine hours
    **/
    public void setEngineHours(double v)
    {
        this.setFieldValue(FLD_engineHours, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine hours since last engine on
    *** @return The engine hours since last engine on
    **/
    public double getEngineOnHours()
    {
        return this.getFieldValue(FLD_engineOnHours, 0.0);
    }

    /**
    *** Sets the engine hours since last engine on
    *** @param v The engine hours since last engine on
    **/
    public void setEngineOnHours(double v)
    {
        this.setFieldValue(FLD_engineOnHours, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine load (percent)
    *** @return The engine load (percent)
    **/
    public double getEngineLoad()
    {
        return this.getFieldValue(FLD_engineLoad, 0.0);
    }

    /**
    *** Sets the engine load (percent)
    *** @param v The engine load (percent)
    **/
    public void setEngineLoad(double v)
    {
        this.setFieldValue(FLD_engineLoad, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine torque
    *** @return The engine torque
    **/
    public double getEngineTorque()
    {
        return this.getFieldValue(FLD_engineTorque, 0.0);
    }

    /**
    *** Sets the engine torque
    *** @param v The engine torque
    **/
    public void setEngineTorque(double v)
    {
        this.setFieldValue(FLD_engineTorque, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine idle hours
    *** @return The engine idle hours
    **/
    public double getIdleHours()
    {
        return this.getFieldValue(FLD_idleHours, 0.0);
    }

    /**
    *** Sets the engine idle hours
    *** @param v The engine idle hours
    **/
    public void setIdleHours(double v)
    {
        this.setFieldValue(FLD_idleHours, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine work hours
    *** @return The engine work hours
    **/
    public double getWorkHours()
    {
        return this.getFieldValue(FLD_workHours, 0.0);
    }

    /**
    *** Sets the engine work hours
    *** @param v The engine work hours
    **/
    public void setWorkHours(double v)
    {
        this.setFieldValue(FLD_workHours, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the work distance tripometer value in kilometers.
    *** @return The work distance tripometer value in kilometers.
    **/
    public double getWorkDistanceKM()
    {
        return this.getFieldValue(FLD_workDistanceKM, 0.0);
    }

    /**
    *** Sets the work distance tripometer value in kilometers.
    *** @param v The work distance tripometer value in kilometers.
    **/
    public void setWorkDistanceKM(double v)
    {
        this.setFieldValue(FLD_workDistanceKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the transmission oil temperature
    *** @return The transmission oil temperature
    **/
    public double getTransOilTemp()
    {
        return this.getFieldValue(FLD_transOilTemp, 0.0);
    }

    /**
    *** Sets the transmission oil temperature
    *** @param v The transmission oil temperature
    **/
    public void setTransOilTemp(double v)
    {
        this.setFieldValue(FLD_transOilTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the coolant level (percent)
    *** @return The coolant level (percent)
    **/
    public double getCoolantLevel()
    {
        return this.getFieldValue(FLD_coolantLevel, 0.0);
    }

    /**
    *** Sets the coolant level (percent)
    *** @param v The coolant level (percent)
    **/
    public void setCoolantLevel(double v)
    {
        this.setFieldValue(FLD_coolantLevel, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the coolant temperature (C)
    *** @return The coolant temperature (C)
    **/
    public double getCoolantTemp()
    {
        return this.getFieldValue(FLD_coolantTemp, 0.0);
    }

    /**
    *** Sets the coolant temperature (C)
    *** @param v The coolant temperature (C)
    **/
    public void setCoolantTemp(double v)
    {
        this.setFieldValue(FLD_coolantTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the intake temperature (C)
    *** @return The intake temperature (C)
    **/
    public double getIntakeTemp()
    {
        return this.getFieldValue(FLD_intakeTemp, 0.0);
    }

    /**
    *** Sets the intake temperature (C)
    *** @param v The intake temperature (C)
    **/
    public void setIntakeTemp(double v)
    {
        this.setFieldValue(FLD_intakeTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the oil pressure
    *** @return The oil pressure
    **/
    public double getOilPressure()
    {
        return this.getFieldValue(FLD_oilPressure, 0.0); // kPa
    }

    /**
    *** Sets the oil pressure
    *** @param v The oil pressure
    **/
    public void setOilPressure(double v)
    {
        this.setFieldValue(FLD_oilPressure, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the oil level (percent)
    *** @return The oil level (percent)
    **/
    public double getOilLevel()
    {
        return this.getFieldValue(FLD_oilLevel, 0.0);
    }

    /**
    *** Sets the oil level (percent)
    *** @param v The oil level (percent)
    **/
    public void setOilLevel(double v)
    {
        this.setFieldValue(FLD_oilLevel, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the oil temperature (C)
    *** @return The oil temperature (C)
    **/
    public double getOilTemp()
    {
        return this.getFieldValue(FLD_oilTemp, 0.0);
    }

    /**
    *** Sets the oil temperature (C)
    *** @param v The oil temperature (C)
    **/
    public void setOilTemp(double v)
    {
        this.setFieldValue(FLD_oilTemp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the air pressure (kPa)
    *** @return The air pressure (kPa)
    **/
    public double getAirPressure()
    {
        return this.getFieldValue(FLD_airPressure, 0.0); // kPa
    }

    /**
    *** Sets the air pressure (kPa)
    *** @param v The air pressure (kPa)
    **/
    public void setAirPressure(double v)
    {
        this.setFieldValue(FLD_airPressure, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the air filter pressure (kPa)
    *** @return The air filter pressure (kPa)
    **/
    public double getAirFilterPressure()
    {
        return this.getFieldValue(FLD_airFilterPressure, 0.0); // kPa
    }

    /**
    *** Sets the air filter pressure (kPa)
    *** @param v The air filter pressure (kPa)
    **/
    public void setAirFilterPressure(double v)
    {
        this.setFieldValue(FLD_airFilterPressure, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the turbo pressure (kPa)
    *** @return The turbo pressure (kPa)
    **/
    public double getTurboPressure()
    {
        return this.getFieldValue(FLD_turboPressure, 0.0); // kPa
    }

    /**
    *** Sets the turbo pressure (kPa)
    *** @param v The turbo pressure (kPa)
    **/
    public void setTurboPressure(double v)
    {
        this.setFieldValue(FLD_turboPressure, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the PTO engaged state
    *** @return The PTO engaged state
    **/
    public boolean getPtoEngaged()
    {
        return this.getFieldValue(FLD_ptoEngaged, true);
    }

    /**
    *** Sets the PTO engaged state
    *** @param v The PTO engaged state
    **/
    public void setPtoEngaged(boolean v)
    {
        this.setFieldValue(FLD_ptoEngaged, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the PTO hours
    *** @return The PTO hours
    **/
    public double getPtoHours()
    {
        return this.getFieldValue(FLD_ptoHours, 0.0);
    }

    /**
    *** Sets the PTO hours
    *** @param v The PTO hours
    **/
    public void setPtoHours(double v)
    {
        this.setFieldValue(FLD_ptoHours, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the PTO distance tripometer value in kilometers.
    *** @return The PTO distance tripometer value in kilometers.
    **/
    public double getPtoDistanceKM()
    {
        return this.getFieldValue(FLD_ptoDistanceKM, 0.0);
    }

    /**
    *** Sets the PTO distance tripometer value in kilometers.
    *** @param v The PTO distance tripometer value in kilometers.
    **/
    public void setPtoDistanceKM(double v)
    {
        this.setFieldValue(FLD_ptoDistanceKM, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the throttle position (percent)
    *** @return The throttle position (percent)
    **/
    public double getThrottlePos()
    {
        return this.getFieldValue(FLD_throttlePos, 0.0);
    }

    /**
    *** Sets the throttle position (percent)
    *** @param v The throttle position (percent)
    **/
    public void setThrottlePos(double v)
    {
        this.setFieldValue(FLD_throttlePos, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the brake position (percent)
    *** @return The brake position (percent)
    **/
    public double getBrakePos()
    {
        return this.getFieldValue(FLD_brakePos, 0.0);
    }

    /**
    *** Sets the brake position (percent)
    *** @param v The brake position (percent)
    **/
    public void setBrakePos(double v)
    {
        this.setFieldValue(FLD_brakePos, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle battery volts
    *** @return The vehicle battery volts
    **/
    public double getVBatteryVolts()
    {
        return this.getFieldValue(FLD_vBatteryVolts, 0.0);
    }

    /**
    *** Sets the vehicle battery volts
    *** @param v The vehicle battery volts
    **/
    public void setVBatteryVolts(double v)
    {
        this.setFieldValue(FLD_vBatteryVolts, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the OBD(DTC)/J1708 fault code (encoded)
    *** @return The OBD(DTC)/J1708 fault code (encoded)
    **/
    public long getJ1708Fault()
    {
        return this.getFieldValue(FLD_j1708Fault, 0L);
    }

    /**
    *** Sets the OBD(DTC)/J1708 fault code (encoded)
    *** @param v The OBD(DTC)/J1708 fault code (encoded)
    **/
    public void setJ1708Fault(long v)
    {
        this.setFieldValue(FLD_j1708Fault, v);
    }

    /**
    *** Gets the OBD(DTC)/J1708 fault code (encoded)
    *** @return The OBD(DTC)/J1708 fault code (encoded)
    *** @see EventData#getJ1708Fault
    **/
    public long getOBDFault()
    {
        // see DTOBDFault
        return this.getJ1708Fault();
    }

    /**
    *** Sets the OBD(DTC)/J1708 fault code (encoded)
    *** @param v The OBD(DTC)/J1708 fault code (encoded)
    *** @see EventData#setJ1708Fault
    **/
    public void setOBDFault(long v)
    {
        // see DTOBDFault
        this.setJ1708Fault(v);
    }

    // ------------------------------------------------------------------------
    //  J1708: type=j1708 mil=1 mid=123 pid=123 fmi=1 count=1 active=false
    //  J1939: type=i1939 mil=1 spn=1234 fmi=12 count=1 active=false
    //  OBDII: type=obdii mil=1 dtc=P0071,P0143

    /**
    *** Returns true if the fault code string is supported
    *** @return True if the fault code string is supported
    **/
    public static boolean supportsFaultCode()
    {
        return EventData.getFactory().hasField(FLD_faultCode);
    }

    /**
    *** Gets the fault code string
    *** @return The fault code string
    **/
    public String getFaultCode()
    {
        return this.getFieldValue(FLD_faultCode, "");
    }

    /**
    *** Sets the fault code string<br>
    *** IE. "OBDII: type=obdii mil=1 dtc=P0071"
    *** @param v The fault code string
    **/
    public void setFaultCode(String v)
    {
        this.setFieldValue(FLD_faultCode, StringTools.trim(v));
    }

    /** 
    *** Returns true if this event contains a non-blank fault code string
    *** @return True if this event contains a non-blank fault code string
    **/
    public boolean hasFaultCode()
    {
        return !StringTools.isBlank(this.getFaultCode());
    }

    /**
    *** Gets the fault code string value as an RTProperties instance
    *** @return The fault code string value as an RTProperties instance
    **/
    public RTProperties getFaultCodeRTProperties()
    {
        String fc = this.getFaultCode();
        return new RTProperties(fc);
    }

    /** 
    *** Sets the OBDII fault codes 
    *** @param dtc  An array of DTC value codes
    **/
    public void setFaultCode_OBDII(String dtc[])
    {
        if (!ListTools.isEmpty(dtc)) {
            this.setFaultCode(DTOBDFault.GetPropertyString_OBDII(dtc));
        } else {
            this.setFaultCode(null);
        }
    }

    // ------------------------------------------------------------------------

    private static boolean explicitSetMIL = false;

    /**
    *** Returns true if the Malfunction-Indicator-Lamp is on
    *** @return True if the Malfunction-Indicator-Lamp is on
    **/
    public boolean getMalfunctionLamp()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_malfunctionLamp);
        return (v != null)? v.booleanValue() : false;
    }

    /**
    *** Sets the Malfunction-Indicator-Lamp state
    *** @param v The Malfunction-Indicator-Lamp state
    **/
    public void setMalfunctionLamp(boolean v)
    {
        this.setFieldValue(FLD_malfunctionLamp, v);
        this.explicitSetMIL = true;
    }

    /**
    *** Returns true if the MalfunctionLamp was explicitly set (using "setMalfunctionLam(...)")
    *** @return True if the MalfunctionLamp was explicitly set
    **/
    public boolean hasMalfunctionLamp()
    {
        return this.explicitSetMIL;
    }

    // J1708/J1939 Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Called at new record creation time to initialize default values
    **/
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /* event index */
    private int eventIndex = -1;

    /**
    *** Sets the Event index (used for reporting)
    *** @param ndx The event index
    **/
    public void setEventIndex(int ndx) 
    {
        this.eventIndex = ndx;
    }

    /**
    *** Gets the Event index (used for reporting)
    *** @return The event index
    **/
    public int getEventIndex()
    {
        return this.eventIndex;
    }

    /**
    *** Returns true if this event is the first event 
    *** @return True if this event is the first event 
    **/
    public boolean getIsFirstEvent()
    {
        return (this.getEventIndex() == 0);
    }

    // ------------------------------------------------------------------------

    /* last event in list */
    private boolean isLastEventInList = false;

    /**
    *** Sets the last event state for this event
    *** @param isLast The last event state
    **/
    public void setIsLastEvent(boolean isLast)
    {
        this.isLastEventInList = isLast;
    }

    /**
    *** Returns true if this event is the last event 
    *** @return True if this event is the last event 
    **/
    public boolean getIsLastEvent()
    {
        return this.isLastEventInList;
    }

    /**
    *** Returns true if the Device pushpin should be shown for the last event
    *** @param isFleet  True for the group/fleet map
    *** @param bpl      The BasicPrivateLabel instance
    *** @return True if the Device pushpin should be shown for the last event
    **/
    public boolean showLastEventDevicePushpin(boolean isFleet, BasicPrivateLabel bpl)
    {
        if (isFleet) {
            boolean dft = false;
            if (bpl != null) {
                RTProperties bpRTP = bpl.getRTProperties();
                String  key = BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_fleet;
                boolean rtn = bpRTP.getBoolean(key, dft);
                //Print.logStackTrace("Key '" + key + "' value = " + rtn);
                return rtn;
            } else {
                return dft;
            }
        } else {
            boolean dft = false;
            if (bpl != null) {
                RTProperties bpRTP = bpl.getRTProperties();
                String  key = BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_device;
                boolean rtn = bpRTP.getBoolean(key, dft);
                //Print.logStackTrace("Key '" + key + "' value = " + rtn);
                return rtn;
            } else {
                return dft;
            }
        }
    }

    // ------------------------------------------------------------------------

    private double   rptDistKM = 0.0;
    private GeoPoint rptDistGP = null;

    /**
    *** Gets the Report distance (kilometers)
    *** @return The Report distance (kilometers)
    **/
    public double getReportDistanceKM()
    {
        return this.rptDistKM;
    }

    /**
    *** Gets the Report location (GeoPoint)
    *** @return The Report location (GeoPoint)
    **/
    public GeoPoint getReportDistanceGP()
    {
        return (this.rptDistGP != null)? this.rptDistGP : this.getGeoPoint();
    }

    /**
    *** Sets the report distance and location (kilometers)
    *** @param km  The report distance (kilometers)
    *** @param gp  The report location
    **/
    public void setReportDistanceKM(double km, GeoPoint gp)
    {
        this.rptDistKM = km;
        this.rptDistGP = GeoPoint.isValid(gp)? gp : this.getGeoPoint();
    }

    /**
    *** Calculates the report distance for this EventData instance
    *** @param lastEV  The prior event
    *** @return True if the report distance/location is set
    **/
    public boolean calculateReportDistance(EventData lastEV)
    {

        /* invalid last EventData? */
        if (lastEV == null) {
            Print.logWarn("Last EventData is null!!");
            this.setReportDistanceKM(0.0, null);
            return false;
        }

        /* last EventData location */
        GeoPoint lastGP = lastEV.getReportDistanceGP();
        double   distKM = lastEV.getReportDistanceKM();
        if (!GeoPoint.isValid(lastGP)) {
            Print.logWarn("Last EventData report GeoPoint is invalid!");
            this.setReportDistanceKM(distKM, null);
            return false;
        }

        /* this EventData location */
        GeoPoint thisGP = this.getGeoPoint();
        if (!GeoPoint.isValid(thisGP)) {
            this.setReportDistanceKM(distKM, lastGP);
            return false;
        }

        /* calculate/set */
        double deltaKM = lastGP.kilometersToPoint(thisGP);
        this.setReportDistanceKM(distKM + deltaKM, thisGP);
        return true;

    }

    /**
    *** Calculates the report distance for the specified array of EventData instances
    *** @param evList  The EventData array
    **/
    public static void calculateReportDistance(EventData evList[])
    {
        if (!ListTools.isEmpty(evList)) {
            EventData lastEV = null;
            for (EventData ev : evList) {
                if (ev != null) {
                    ev.calculateReportDistance(lastEV);
                    lastEV = ev;
                }
            }
        }
    }

    /**
    *** Calculates the report distance for the specified list of EventData instances
    *** @param evList  The EventData list
    **/
    public static void calculateReportDistance(Collection<EventData> evList)
    {
        if (!ListTools.isEmpty(evList)) {
            EventData lastEV = null;
            for (EventData ev : evList) {
                if (ev != null) {
                    ev.calculateReportDistance(lastEV);
                    lastEV = ev;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @param dft  The default "Start/Stop StatusCode supported" state
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public boolean getStartStopSupported(boolean dft)
    {
        Device dev = this.getDevice();
        return (dev != null)? dev.getStartStopSupported() : dft;
    }

    /**
    *** Returns the start/stop status codes defined in the Device record
    *** @return The start/stop status codes
    **/
    public int[] getStartStopStatusCodes()
    {
        Device dev = this.getDevice();
        int ssc[] = (dev != null)? dev.getStartStopStatusCodes() : null;
        return ssc;
    }

    // ----------------

    /**
    *** Returns true if this event represents a start event
    *** @param checkSpeed  True to use the speed of the event in the start event determination
    *** @return True if this event represents a start event
    **/
    public boolean isStartEvent(boolean checkSpeed)
    {
        return this.isStartEvent(this.getStartStopStatusCodes(),checkSpeed);
    }

    /**
    *** Returns true if this event represents a start event
    *** @param ssc A 2-element array of status-codes (element 0 contains the start-status-code)
    ***            used to check this event for start-event status.
    *** @param checkSpeed  True to use the speed of the event in the start event determination
    *** @return True if this event represents a start event
    **/
    public boolean isStartEvent(int ssc[], boolean checkSpeed)
    {
        if (ssc != null) {
            int sc = this.getStatusCode();
            return (sc == ssc[0])? true : false;
        } else
        if (checkSpeed) {
            return (this.getSpeedKPH() > 0.0)? true : false;
        } else {
            return false;
        }
    }

    // ----------------

    /**
    *** Returns true if this event represents a stop event
    *** @param checkSpeed  True to use the speed of the event in the stop event determination
    *** @return True if this event represents a stop event
    **/
    public boolean isStopEvent(boolean checkSpeed)
    {
        return this.isStopEvent(this.getStartStopStatusCodes(),checkSpeed);
    }

    /**
    *** Returns true if this event represents a stop event
    *** @param ssc A 2-element array of status-codes (element 1 contains the stop-status-code)
    ***            used to check this event for stop-event status.
    *** @param checkSpeed  True to use the speed of the event in the stop event determination
    *** @return True if this event represents a stop event
    **/
    public boolean isStopEvent(int ssc[], boolean checkSpeed)
    {
        if (ssc != null) {
            int sc = this.getStatusCode();
            return (sc == ssc[1])? true : false;
        } else
        if (checkSpeed) {
            return (this.getSpeedKPH() <= 0.0)? true : false;
        } else {
            return false;
        }
    }

    // ----------------

    private boolean isStopped = true;

    /**
    *** Sets the vehicle 'stopped' state
    *** @param stopped  The current vehicle stopped state
    **/
    public void setStopped(boolean stopped)
    {
        this.isStopped = stopped;
    }

    /**
    *** Gets the vehicle 'stopped' state
    *** @return true if stopped
    **/
    public boolean isStopped()
    {
        return this.isStopped;
    }

    // ------------------------------------------------------------------------

    /* rule trigger event */
    private boolean isSynthesizedEvent = false;

    /**
    *** Sets the synthesized event state (ie. an event that was generated based
    *** on other analyzed criteria)
    *** @param isSynthesized  True to indicate a synthesized event
    **/
    public void setSynthesizedEvent(boolean isSynthesized)
    {
        this.isSynthesizedEvent = isSynthesized;
    }

    /**
    *** Returns the synthesized event state
    *** @return The synthesized event state
    **/
    public boolean getIsSynthesizedEvent()
    {
        return this.isSynthesizedEvent;
    }

    // ------------------------------------------------------------------------
    
    /* PushpinIconIndexProvider */
    private PushpinIconIndexProvider iconIndexProvider = null;
    
    /**
    *** Sets the Pushpin Icon Index Provider
    *** @param piip  The PushpinIconIndexProvider instance
    **/
    public void setPushpinIconIndexProvider(PushpinIconIndexProvider piip)
    {
        this.iconIndexProvider = piip;
    }

    // ------------------------------------------------------------------------
    
    /* preset explicit icon index */
    private int explicitPushpinIconIndex = -1;
    
    /**
    *** Sets the explicit Pushpin Icon Index
    *** @param epii  The PushpinIconIndexProvider instance
    **/
    public void setPushpinIconIndex(int epii)
    {
        this.explicitPushpinIconIndex = epii;
    }
    
    /**
    *** Sets the explicit Pushpin Icon Index
    *** @param iconName  The icon name
    *** @param iconKeys  The list of icon keys from which the index is derived, based on the position of
    ***                  icon name in this list.
    **/
    public void setPushpinIconIndex(String iconName, OrderedSet<String> iconKeys)
    {
        this.setPushpinIconIndex(EventData._getPushpinIconIndex(iconName, iconKeys, ICON_PUSHPIN_GREEN));
    }

    // ------------------------------------------------------------------------

    // add "&DebugPushpins=true" to the login URL to turn this on for the current login session
    public static final String DEBUG_PUSHPINS[]     = new String[] { "DebugPushpins", "PushpinDebug", "debugPP" };
    //public static boolean DebugPushpins = RTConfig.getBoolean(EventData.DEBUG_PUSHPINS,false);
    
    /* standard pushpin names */
    public static final String PPNAME_last          = "last";       // last event in list
    public static final String PPNAME_fleet         = "fleet";      // fleet map
    public static final String PPNAME_heading       = "heading";    // generic heading (default)
    public static final String PPNAME_stop          = "stop";       // specific "STOP" event
    public static final String PPNAME_moving        = "moving";     // speed > 0
    public static final String PPNAME_all           = "all";        // general pushpin
    public static final String PPNAME_statusCode    = "statusCode"; // StatusCode override

    /**
    *** Displays the selected pushpin (debug purposes only)
    *** @param msg  The header message
    *** @param iconKeys  The list of defined icons
    *** @param ndx  The selected icon index
    **/
    private static void PushpinDebug(String msg, OrderedSet<String> iconKeys, int ndx)
    {
        String iconName = ((ndx >= 0) && (ndx < ListTools.size(iconKeys)))? iconKeys.get(ndx) : "?";
        Print.logInfo("Pushpin Selection - " + msg + ": #" + ndx + " \"" + iconName + "\"");
    }

    /**
    *** Gets the default map icon index
    *** @param iconSelector  An icon 'selector' to be analyzed by the installed 'RuleFactory' to
    ***         determine the icon index. Currently, this value is obtained only from the
    ***         contents of the MapProvider property "iconSelector" or "iconSelector.fleet", and
    ***         will likely be null if neither of these properties are defined.
    *** @param iconKeys The defined icon keys (the returned index must be within the
    ***                 the range of this list).
    *** @param isFleet  True if obtaining an icon index for a 'fleet' map
    *** @return An icon index
    **/
    public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys,
        boolean isFleet, BasicPrivateLabel bpl)
    {
        Device dev = this.getDevice();
        // Properties effecting how pushpins are chosen:
        //  - DebugPushpins
        //  - BasicPrivateLabel.PROP_TrackMap_showFleetMapDevicePushpin
        //  - BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_fleet
        //  - BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_device
        //  - MapProvider.PROP_iconSel_fleet
        //  - MapProvider.PROP_iconSelector
        boolean debugPP = RTConfig.getBoolean(EventData.DEBUG_PUSHPINS,false);
            // || ((bpl != null) && bpl.getBooleanProperty(EventData.DEBUG_PUSHPINS,false));

        // ---------------------------------------
        // 1) Explicit pushpin chooser
        //    Check explicit pushpin set by "setPushpinIconIndex(...)"

        /* ExplicitIndex: do we have an explicit icon index? */
        if (this.explicitPushpinIconIndex >= 0) {
            if (debugPP) PushpinDebug("Explicit index [#1]", iconKeys, this.explicitPushpinIconIndex);
            return this.explicitPushpinIconIndex;
        }

        // ---------------------------------------
        // 2) Explicit pushpin provider
        //    Check explicit pushpin provided by "setPushpinIconIndexProvider(...)"

        /* ExplicitProvider: do we have an explicit icon index provider? */
        if (this.iconIndexProvider != null) {
            int iconNdx = this.iconIndexProvider.getPushpinIconIndex(this, iconSelector, iconKeys, isFleet, bpl);
            if (iconNdx >= 0) {
                if (debugPP) PushpinDebug("IconIndexProvider [#2]", iconKeys, iconNdx);
                return iconNdx;
            }
        }

        // ---------------------------------------
        // 3) MapProvider ==> IconSelector pushpin chooser
        //    Check selected by "iconSelector" (typically from the MapProvider property "iconSelector" or "iconSelector.fleet")

        /* IconSelector: check rule factory */
        if (!StringTools.isBlank(iconSelector)) {
            RuleFactory rf = Device.getRuleFactory();
            if (rf == null) {
                // -- no RuleFactory, skip
            } else
            if (!rf.checkRuntime()) {
                Print.logWarn("StatusCode RuleFactory runtime check failed: " + this.getStatusCodeHex());
            } else {
                int iconNdx = this._getPushpinIndexFromRuleSelector(rf, "IconSelector [#3]", iconSelector, iconKeys, 
                    debugPP);
                if (iconNdx >= 0) {
                    // -- already displays Pushpin debug
                    return iconNdx;
                }
            }
        }

        // ---------------------------------------
        // 4) Fleet icon checks

        /* FleetPushpin: fleet map - custom Device pushpin? */
        if (isFleet) {
            // -- BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_fleet
            boolean showDevPP = this.showLastEventDevicePushpin(isFleet,bpl); 
            String devPP = (showDevPP && (dev != null))? dev.getPushpinID() : "";

            // ---------------------------------------
            // 4.a) If Device "pushpinID" is "statusCode", use status code override

            /* StatusCode: show StatusCode pushpin if Device "pushpinID" is "statusCode" */
            if (showDevPP && devPP.equalsIgnoreCase(PPNAME_statusCode)) {
                int sciNdx = this._getStatusCodePushpinIndex(iconKeys, bpl, debugPP, "#4a");
                if (sciNdx >= 0) {
                    // -- already displays Pushpins debug
                    return sciNdx;
                }
                // -- no StatusCode pushpin?
            }

            // ---------------------------------------
            // 4.b) If "trackMap.lastDevicePushpin.fleet=true", use Device pushpin

            /* DevicePushpin: show Device pushpinID for Event? */
            if (showDevPP && !StringTools.isBlank(devPP) && !devPP.equalsIgnoreCase(PPNAME_statusCode)) { 
                int diNdx = EventData._getPushpinIconIndex(devPP, iconKeys, -1);
                if (diNdx >= 0) {
                    if (debugPP) PushpinDebug("Device ("+dev.getDeviceID()+") fleet [#4b]", iconKeys, diNdx);
                    return diNdx;
                }
            }

            // ---------------------------------------
            // 4.c) Use "fleet" pushpin

            /* FleetPushpin: show "fleet" pushpin, if defined, for Event */
            int fleetIconNdx = EventData._getPushpinIconIndex(PPNAME_fleet, iconKeys, -1/*ICON_PUSHPIN_BLUE*/);
            if (fleetIconNdx >= 0) {
                if (debugPP) PushpinDebug("\""+PPNAME_fleet+"\" name [#4c]", iconKeys, fleetIconNdx);
                return fleetIconNdx;
            }

        }

        // ---------------------------------------
        // 5) Use "all" pushpin

        /* AllPushpin: "all" pushpins? */
        int allIconNdx = EventData._getPushpinIconIndex(PPNAME_all, iconKeys, -1);
        if (allIconNdx >= 0) { // '0' is a valid index
            if (debugPP) PushpinDebug("\""+PPNAME_all+"\" name [#5]", iconKeys, allIconNdx);
            return allIconNdx;
        }

        // ---------------------------------------
        // 6) Use pushpin indicated for status code
        //    Check custom status code pushpins

        /* StatusCode: status code defined pushpin */
        int scIconNdx = this._getStatusCodePushpinIndex(iconKeys, bpl, debugPP, "#6");
        if (scIconNdx >= 0) {
            // -- already displays Pushpins debug
            return scIconNdx;
        }

        // ---------------------------------------
        // 7) Last pushpin in list for Device (non-fleet)
        //    Check custom status code pushpins

        /* LastPushpin: last event? */
        if (!isFleet && this.getIsLastEvent()) {
            // -- DeviceMap and last Event
            // BasicPrivateLabel.PROP_TrackMap_lastDevicePushpin_device
            boolean showDevPP = this.showLastEventDevicePushpin(isFleet,bpl); // <-- fixed [2.5.7-B21] (was "!isFleet")
            String devPP = (showDevPP && (dev != null))? dev.getPushpinID() : "";

            // ---------------------------------------
            // 7.a) If "trackMap.lastDevicePushpin.device=true", use Device pushpin

            /* DevicePushpin: show Device pushpinID for last event? */
            if (showDevPP && !StringTools.isBlank(devPP)) { 
                int diNdx = EventData._getPushpinIconIndex(devPP, iconKeys, -1);
                if (diNdx >= 0) {
                    if (debugPP) PushpinDebug("Device ["+dev.getDeviceID()+"] last [#7a]" , iconKeys, diNdx);
                    return diNdx;
                }
            }

            // ---------------------------------------
            // 7.b) Use "last" pushpin

            /* DeviceLast: standard "last" event */
            int lastIconNdx = EventData._getPushpinIconIndex(PPNAME_last, iconKeys, -1);
            if (lastIconNdx >= 0) { // '0' is a valid index
                if (debugPP) PushpinDebug("\""+PPNAME_last+"\" name [#7b]", iconKeys, lastIconNdx);
                return lastIconNdx;
            }

        }

        // ---------------------------------------
        // 8) Use "stop" pushpin, if device is stopped

        /* StopPushpin: "stop" icon index */
        if (this.isStopEvent(false)) { // check for specific "STOP" status code
            int stopIconNdx = EventData._getPushpinIconIndex(PPNAME_stop, iconKeys, -1);
            if (stopIconNdx >= 0) { // '0' is a valid index
                if (debugPP) PushpinDebug("\""+PPNAME_stop+"\" name [#8]", iconKeys, stopIconNdx);
                return stopIconNdx;
            }
        }

        // ---------------------------------------
        // 9) Use "moving" pushpin, if device is moving

        /* MovingPushpin: "moving" icon index */
        if (this.getSpeedKPH() > 0.0) { // check for non-zero speed
            int movingIconNdx = EventData._getPushpinIconIndex(PPNAME_moving, iconKeys, -1);
            if (movingIconNdx >= 0) { // '0' is a valid index
                if (debugPP) PushpinDebug("\""+PPNAME_moving+"\" name [#9]", iconKeys, movingIconNdx);
                return movingIconNdx;
            }
        }

        // ---------------------------------------
        // 10) Check the default pushpin specified by "EventData.defaultPushpinName"

        /* DefaultPushpin: default icon index */
        String dftIconName = RTConfig.getString("EventData.defaultPushpinName",PPNAME_heading);
        int dftIconNdx = EventData._getPushpinIconIndex(dftIconName, iconKeys, ICON_PUSHPIN_GREEN);
        if (dftIconNdx >= 0) {
            if (debugPP) PushpinDebug("default(\""+dftIconName+"\") name [#10]", iconKeys, dftIconNdx);
            return dftIconNdx;
        }

        // ---------------------------------------
        // 11) No pushpin found, use "black"

        /* NoPushpin: no pushpin found! */
        if (debugPP) PushpinDebug("Not Found [#11]", iconKeys, ICON_PUSHPIN_BLACK);
        return ICON_PUSHPIN_BLACK;

    }

    /**
    *** Gets the pushpin index based on the various specified criteria
    *** @param ruleFact  The RuleFactory instance
    *** @param type      The pushpin type (debug purposes)
    *** @param iconSel   The icon rule-selector
    *** @param iconKey   The list of available pushpin icons
    *** @param debugPP   True to display debug information regarding icon selection
    *** @return The selected icon index
    **/
    private int _getPushpinIndexFromRuleSelector(RuleFactory ruleFact, 
        String type, String iconSel, OrderedSet<String> iconKeys,
        boolean debugPP)
    {
        if ((ruleFact != null) && !StringTools.isBlank(iconSel)) {
            try {
                //Print.logInfo("iconSel: " + iconSel);
                Object result = ruleFact.evaluateSelector(iconSel,this);
                if (result instanceof Number) {
                    int iconNdx = ((Number)result).intValue();
                    if (iconNdx >= iconKeys.size()) {
                        Print.logWarn("Pushpin index invalid: " + iconNdx);
                        if (debugPP) PushpinDebug(type+" Rule ["+iconSel+"] (INVALID)", iconKeys, iconNdx);
                        return iconNdx;
                    } else
                    if (iconNdx >= 0) { // '0' is a valid index
                        if (debugPP) PushpinDebug(type+" Rule ["+iconSel+"]", iconKeys, iconNdx);
                        return iconNdx;
                    } else {
                        // no pushpin chosen
                        return -1;
                    }
                } else
                if (result instanceof String) {
                    String iconName = (String)result;
                    if (!StringTools.isBlank(iconName)) {
                        int iconNdx = EventData._getPushpinIconIndex(iconName, iconKeys, -1);
                        if (iconNdx >= 0) {
                            if (debugPP) PushpinDebug(type+" Rule ["+iconSel+"]", iconKeys, iconNdx);
                            return iconNdx;
                        } else {
                            // iconName specified, but not found
                            Print.logWarn("Pushpin not found: " + iconName);
                            iconNdx = ICON_PUSHPIN_BLACK;
                            if (debugPP) PushpinDebug(type+" Rule ["+iconSel+"] (INVALID)", iconKeys, iconNdx);
                            return iconNdx;
                        }
                    } else {
                        // no pushpin chosen
                        return -1;
                    }
                } else {
                    // An object other than a Number or String was returned.
                    Print.logError("Pushpin selector invalid result type: " + StringTools.className(result));
                    int iconNdx = ICON_PUSHPIN_BLACK;
                    if (debugPP) PushpinDebug(type+" Rule ["+iconSel+"] (INVALID)", iconKeys, iconNdx);
                    return iconNdx;
                }
            } catch (RuleParseException rpe) {
                Print.logError("Pushpin selector parse error: " + rpe.getMessage());
                int iconNdx = ICON_PUSHPIN_BLACK;
                if (debugPP) PushpinDebug(type+" Rule ["+iconSel+"] (ERROR)", iconKeys, iconNdx);
                return iconNdx;
            }
        } else {
            // no pushpin chosen
            return -1;
        }
    }

    /**
    *** Gets the icon index based on the status code
    *** @param iconKeys  The list of available pushpins icon
    *** @param bpl       The BasicPrivateLabel instance
    *** @param debugPP   True to debug pushpin icon selection
    *** @return The selected pushpin icon selection
    **/
    private int _getStatusCodePushpinIndex(OrderedSet<String> iconKeys, BasicPrivateLabel bpl,
        boolean debugPP, String dbgMsg)
    {

        /* StatusPushpin: device map? - statusCode icon name */
        String scIconName = this.getStatusCodeIconName(bpl);
        if (!StringTools.isBlank(scIconName)) {
            int iconNdx = EventData._getPushpinIconIndex(scIconName, iconKeys, -1);
            if (iconNdx >= 0) {
                if (debugPP) PushpinDebug("StatusCode name ["+dbgMsg+"]", iconKeys, iconNdx);
                return iconNdx;
            }
        }

        /* StatusRulePushpin: status code icon selector */
        RuleFactory rf = Device.getRuleFactory();
        if (rf != null) {
            String scIconSel = this.getStatusCodeIconSelector(bpl);
            if (StringTools.isBlank(scIconSel)) {
                // -- no status code icon selector, skip ...
            } else
            if (!rf.checkRuntime()) {
                Print.logWarn("StatusCode RuleFactory runtime check failed: " + this.getStatusCodeHex());
            } else {
                int iconNdx = this._getPushpinIndexFromRuleSelector(rf, "StatusCode selector ["+dbgMsg+"]", scIconSel, iconKeys, 
                    debugPP);
                if (iconNdx >= 0) {
                    // -- already displays Pushpins debug
                    return iconNdx;
                }
            }
        }

        /* no pushpin chosen */
        return -1;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Format this events as a comma-separated-value list
    *** @param fields  The array of EventData fields to include in the CSV format
    *** @return The event record in CSV format
    **/
    public String formatAsCSVRecord(String fields[])
    {
        String csvSep = ",";
        StringBuffer sb = new StringBuffer();
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) { sb.append(csvSep); }
                DBField dbFld = this.getRecordKey().getField(fields[i]);
                Object val = (dbFld != null)? this.getFieldValue(fields[i]) : null;
                if (val != null) {
                    Class typeClass = dbFld.getTypeClass();
                    if (fields[i].equals(FLD_statusCode)) {
                        int code = ((Integer)val).intValue();
                        StatusCodes.Code c = StatusCodes.GetCode(code,Account.getPrivateLabel(this.getAccount()));
                        if (c != null) {
                            sb.append("\"" + c.getDescription(null) + "\"");
                        } else {
                            sb.append("\"0x" + StringTools.toHexString(code,16) + "\"");
                        }
                    } else 
                    if ((typeClass == Double.class) || (typeClass == Double.TYPE)) {
                        double d = ((Double)val).doubleValue();
                        String fmt = dbFld.getFormat();
                        if ((fmt != null) && !fmt.equals("")) {
                            sb.append("\"" + StringTools.format(d,fmt) + "\"");
                        } else {
                            sb.append("\"" + String.valueOf(d) + "\"");
                        }
                    } else 
                    if ((typeClass == Float.class) || (typeClass == Float.TYPE)) {
                        float d = ((Float)val).floatValue();
                        String fmt = dbFld.getFormat();
                        if ((fmt != null) && !fmt.equals("")) {
                            sb.append("\"" + StringTools.format(d,fmt) + "\"");
                        } else {
                            sb.append("\"" + String.valueOf(d) + "\"");
                        }
                    } else {
                        sb.append(StringTools.quoteCSVString(val.toString()));
                    }
                }
            }
        }
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

    private EventData previousEventData          = null;
    private EventData previousEventData_validGPS = null;

    /**
    *** Sets the previous EventData record
    *** @param ev  The previous EventData record
    **/
    public void setPreviousEventData(EventData ev)
    {
        if (ev != null) {
            this.previousEventData = ev;
            if (this.previousEventData.isValidGeoPoint()) {
                this.previousEventData_validGPS = this.previousEventData;
            }
        }
    }

    /**
    *** Gets the previous EventData record
    *** @param validGPS  True to check for previous events with a valid GPS location only
    *** @return  The previous EventData record
    *** @throws DBException
    **/
    public EventData getPreviousEventData(boolean validGPS)
        throws DBException
    {
        return this.getPreviousEventData(null, validGPS);
    }

    /**
    *** Gets the previous EventData record
    *** @param statusCodes  A list of status codes to search for 
    *** @param validGPS  True to check for previous events with a valid GPS location only
    *** @return  The previous EventData record
    *** @throws DBException
    **/
    public EventData getPreviousEventData(int statusCodes[], boolean validGPS)
        throws DBException
    {

        /* check previous event cache */
        if (statusCodes == null) {
            // check for cached previous event
            if (!validGPS && (this.previousEventData != null)) {
                return this.previousEventData;
            } else
            if (validGPS && (this.previousEventData_validGPS != null)) {
                return this.previousEventData_validGPS;
            }
        }

        /* get previous event */
        // 'endTime' should be this events timestamp, 
        // and 'additionalSelect' should be (statusCode != this.getStatusCode())
        long startTime = -1L; // start of time
        long endTime   = this.getTimestamp() - 1L; // previous to this event
        EventData ed[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            startTime, endTime,
            statusCodes,
            validGPS,
            EventData.LimitType.LAST, 1/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        if (!ListTools.isEmpty(ed)) {
            EventData ev = ed[0];
            if (statusCodes == null) {
                // cache event
                if (validGPS) {
                    this.previousEventData_validGPS = ev;
                } else {
                    this.previousEventData = ev;
                    if (this.previousEventData.isValidGeoPoint()) {
                        this.previousEventData_validGPS = this.previousEventData;
                    }
                }
            }
            return ev;
        } else {
            return null;
        }
        
    }

    /**
    *** Gets the previous EventData record
    *** @param accountID   The Account ID
    *** @param deviceID    The Device ID
    *** @param timestamp   The starting timestamp
    *** @param statusCodes The status codes to look for
    *** @param validGPS    True to look for events with a valid GPS location only
    *** @return The previous EventData record
    **/
    public static EventData getPreviousEventData(
        String accountID, String deviceID,
        long timestamp, int statusCodes[], 
        boolean validGPS)
        throws DBException
    {
        long startTime = -1L;
        long endTime   = timestamp - 1L;
        EventData ed[] = EventData.getRangeEvents(
            accountID, deviceID,
            startTime, endTime,
            statusCodes,
            validGPS,
            EventData.LimitType.LAST, 1/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        return !ListTools.isEmpty(ed)? ed[0] : null;
    }
    
    // ------------------------------------------------------------------------

    private EventData nextEventData          = null;
    private EventData nextEventData_validGPS = null;
    
    /**
    *** Gets the next EventData record
    *** @param validGPS  True to check for next events with a valid GPS location only
    *** @return  The next EventData record
    *** @throws DBException
    **/
    public EventData getNextEventData(boolean validGPS)
        throws DBException
    {

        if ((!validGPS && (this.nextEventData == null)) ||
            ( validGPS && (this.nextEventData_validGPS == null))) {
            // 'startTime' should be this events timestamp, 
            // and 'additionalSelect' should be (statusCode != this.getStatusCode())
            long startTime   = this.getTimestamp() + 1L;
            long endTime     = -1L;
            EventData ed[] = EventData.getRangeEvents(
                this.getAccountID(), this.getDeviceID(),
                startTime, endTime,
                null/*statusCodes[]*/,
                validGPS,
                EventData.LimitType.FIRST, 1/*limit*/, true/*ascending*/,
                null/*additionalSelect*/);
            if ((ed != null) && (ed.length > 0)) {
                if (validGPS) {
                    this.nextEventData_validGPS = ed[0];
                } else {
                    this.nextEventData = ed[0];
                    if (this.nextEventData.isValidGeoPoint()) {
                        this.nextEventData_validGPS = this.nextEventData;
                    }
                }
            }
        }

        return validGPS? this.nextEventData_validGPS : this.nextEventData;
    }

    /**
    *** Gets the next EventData record
    *** @param accountID   The Account ID
    *** @param deviceID    The Device ID
    *** @param timestamp   The starting timestamp
    *** @param statusCodes The status codes to look for
    *** @param validGPS    True to look for events with a valid GPS location only
    *** @return The next EventData record
    **/
    public static EventData getNextEventData(
        String accountID, String deviceID,
        long timestamp, int statusCodes[], 
        boolean validGPS)
        throws DBException
    {
        long startTime = timestamp + 1L;
        long endTime   = -1L;
        EventData ed[] = EventData.getRangeEvents(
            accountID, deviceID,
            startTime, endTime,
            statusCodes,
            validGPS,
            EventData.LimitType.FIRST, 1/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        return !ListTools.isEmpty(ed)? ed[0] : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Overrides <code>DBRecord.getFieldValue(...)</code>
    *** @param fldName  The EventData fieldname
    *** @return The field value
    **/
    public Object getFieldValue(String fldName)
    {
        //if ((fldName != null) && fldName.startsWith(DBRecord.PSEUDO_FIELD_CHAR)) {
        //    if (fldName.equals(EventData.PFLD_deviceDesc)) {
        //        return this.getDeviceDescription();
        //    } else {
        //        return null;
        //    }
        //} else {
            return super.getFieldValue(fldName);
        //}
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback when record is about to be inserted into the table
    **/
    protected void recordWillInsert()
    {
        // overriden to optimize 
        // (DBRecordListnener not allowed, to prevent excessive backlogging)
    }

    /**
    *** Callback after record has been be inserted into the table
    **/
    protected void recordDidInsert()
    {
        // overriden to optimize
        // (DBRecordListnener not allowed, to prevent excessive backlogging)
        // ----
        // TODO: Queue JMS EventData message?
    }

    /**
    *** Callback when record is about to be updated in the table
    **/
    protected void recordWillUpdate()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    /**
    *** Callback after record has been be updated in the table
    **/
    protected void recordDidUpdate()
    {
        // override to optimize (DBRecordListnener not allowed)
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified record exists
    *** @param acctID  The Account ID
    *** @param devID   The Device ID
    *** @return True if the record exists
    **/
    public static boolean exists(String acctID, String devID, long time, int stCode)
        throws DBException // if error occurs while testing existence
    {
        if (StringTools.isBlank(acctID)) {
            return false;
        } else
        if (StringTools.isBlank(devID)) {
            return false;
        } else
        if (time < 0L) {
            return false;
        } else
        if (stCode < 0) {
            return false;
        } else {
            EventData.Key evKey = new EventData.Key(acctID, devID, time, stCode);
            return evKey.exists();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates an EventData record from the specified GeoEvent
    *** @param gev  The GeoEvent
    **/
    public static EventData createEventDataRecord(GeoEvent gev)
    {

        /* invalid event */
        if (gev == null) {
            Print.logError("GeoEvent is null");
            return null;
        }
                
        /* create key */
        String acctID = gev.getAccountID();
        String devID  = gev.getDeviceID();
        long   time   = gev.getTimestamp();
        int    stCode = gev.getStatusCode();
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID) || (time <= 0L) || (stCode <= 0)) {
            Print.logError("Invalid key specification");
            return null;
        }
        EventData.Key evKey = new EventData.Key(acctID, devID, time, stCode);
        
        /* fill record */
        EventData evdb = evKey.getDBRecord();
        for (String fldn : gev.getFieldKeys()) {
            Object val = gev.getFieldValue(fldn,null);
            evdb.setFieldValue(fldn, val);
        }
        
        /* return event */
        return evdb;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // [DB]WHERE ( <Condition...> )
    public static String getWhereClause(long autoIndex)
    {
        DBWhere dwh = new DBWhere(EventData.getFactory());
        dwh.append(dwh.EQ(EventData.FLD_autoIndex,autoIndex));
        return dwh.WHERE(dwh.toString());
    }
    
    // [DB]WHERE ( <Condition...> )
    public static String getWhereClause(
        String acctId, String devId,
        long timeStart, long timeEnd, 
        int statCode[], 
        boolean gpsRequired, 
        String andSelect)
    {
        DBFactory<EventData> dbFact = EventData.getFactory();
        DBWhere dwh = new DBWhere(EventData.getFactory());

        /* Account/Device */
        // ( (accountID='acct') AND (deviceID='dev') )
        if (!StringTools.isBlank(acctId)) {
            dwh.append(dwh.EQ(EventData.FLD_accountID, acctId));
            if (!StringTools.isBlank(devId) && !devId.equals("*")) {
                dwh.append(dwh.AND_(dwh.EQ(EventData.FLD_deviceID , devId)));
            }
        }

        /* status code(s) */
        // AND ( (statusCode=2) OR (statusCode=3) [OR ...] )
        if ((statCode != null) && (statCode.length > 0)) {
            dwh.append(dwh.AND_(dwh.INLIST(EventData.FLD_statusCode,statCode)));
        }

        /* gps required */
        if (gpsRequired) {
            // AND ( (latitude!=0) OR (longitude!=0) )
            // This section states that if both the latitude/longitude are '0',
            // then do not include the record in the select.
            if (!dbFact.hasField(EventData.FLD_cellLatitude)) {
                // -- no celltower location
                dwh.append(dwh.AND_(
                    dwh.OR(
                        dwh.NE(EventData.FLD_latitude     ,0L),
                        dwh.NE(EventData.FLD_longitude    ,0L)
                    )
                ));
            } else {
                // -- also check cell tower location
                dwh.append(dwh.AND_(
                    dwh.OR(
                        dwh.NE(EventData.FLD_latitude     ,0L),
                        dwh.NE(EventData.FLD_longitude    ,0L),
                        dwh.NE(EventData.FLD_cellLatitude ,0L),
                        dwh.NE(EventData.FLD_cellLongitude,0L)
                    )
                ));
            }
        }

        /* event time */
        if (timeStart >= 0L) {
            // AND (timestamp>=123436789)
            dwh.append(dwh.AND_(dwh.GE(EventData.FLD_timestamp,timeStart)));
        }
        if ((timeEnd >= 0L) && (timeEnd >= timeStart)) {
            // AND (timestamp<=123456789)
            dwh.append(dwh.AND_(dwh.LE(EventData.FLD_timestamp,timeEnd)));
        }
        
        /* additional selection */
        if (!StringTools.isBlank(andSelect)) {
            // AND ( ... )
            dwh.append(dwh.AND_(andSelect));
        }
        
        /* end of where */
        return dwh.WHERE(dwh.toString());
        
    }

    // ------------------------------------------------------------------------

    /* return the EventData record for the specified 'autoIndex' value */
    public static EventData getAutoIndexEvent(long autoIndex)
        throws DBException
    {
        DBFactory<EventData> dbFact = EventData.getFactory();

        /* has FLD_autoIndex? */
        if (!dbFact.hasField(EventData.FLD_autoIndex)) {
            return null;
        }
        
        /* create key */
        //DBFactory dbFact = EventData.getFactory();
        //DBRecordKey<EventData> evKey = dbFact.createKey();
        //evKey.setFieldValue(EventData.FLD_autoIndex, autoIndex);

        /* create selector */
        DBSelect<EventData> dsel = new DBSelect<EventData>(dbFact);
        dsel.setWhere(EventData.getWhereClause(autoIndex));

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, null); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }

        /* return result */
        return !ListTools.isEmpty(ed)? ed[0] : null;

    }

    // ------------------------------------------------------------------------

    /* return the EventData record for the specified 'autoIndex' value */
    public static EventData[] getSelectedEvents(DBSelect<EventData> dsel, DBRecordHandler<EventData> rcdHandler)
        throws DBException
    {

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, rcdHandler);
        } finally {
            DBProvider.unlockTables();
        }

        /* return result */
        return !ListTools.isEmpty(ed)? ed : null;

    }

    // ------------------------------------------------------------------------

    /* create range event selector */
    private static DBSelect<EventData> _createRangeEventSelector(
        String acctId, String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit, boolean ascending,
        String addtnlSelect)
    {

        /* invalid account/device */
        if (StringTools.isBlank(acctId)) {
            //Print.logWarn("No AccountID specified ...");
            return null;
        } else
        if (StringTools.isBlank(devId)) {
            //Print.logWarn("No DeviceID specified ...");
            return null;
        }

        /* invalid time range */
        if ((timeStart > 0L) && (timeEnd > 0L) && (timeStart > timeEnd)) {
            //Print.logWarn("Invalid time range specified ...");
            return null;
        }

        /* ascending/descending */
        boolean isAscending = ascending;
        if ((limit > 0L) && ((limitType == null) || EventData.LimitType.LAST.equals(limitType))) {
            // NOTE: records will be in descending order (will need to reorder)
            isAscending = false;
        }

        /* create/return DBSelect */
        // DBSelect: [SELECT * FROM EventData] <Where> ORDER BY <FLD_timestamp> [DESC] LIMIT <Limit>
        DBSelect<EventData> dsel = new DBSelect<EventData>(EventData.getFactory());
        dsel.setWhere(EventData.getWhereClause(
            acctId, devId,
            timeStart, timeEnd,
            statCode,
            validGPS,
            addtnlSelect));
        dsel.setOrderByFields(FLD_timestamp);
        dsel.setOrderAscending(isAscending);
        dsel.setLimit(limit);
        return dsel;
        
    }

    /* get a specific EventData record */
    public static EventData getEventData(
        String acctId, String devId,
        long timestamp, int statusCode)
        throws DBException
    {
        EventData ed[] = EventData.getRangeEvents(
            acctId, devId,
            timestamp, timestamp,
            new int[] { statusCode },
            false/*validGPS*/,
            EventData.LimitType.LAST, 1L/*limit*/, true/*ascending*/,
            null/*addtnlSelect*/,
            null/*rcdHandler*/);
        return (ed.length > 0)? ed[0] : null;
    }

    /* get range of EventData records (does not return null) */
    public static EventData[] getRangeEvents(
        String acctId, String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit, boolean ascending,
        String addtnlSelect)
        throws DBException
    {
        return EventData.getRangeEvents(
            acctId, devId, 
            timeStart, timeEnd,
            statCode,
            validGPS, 
            limitType, limit, ascending,
            addtnlSelect,
            null/*rcdHandler*/);
    }

    /* get range of EventData records (does not return null) */
    public static EventData[] getRangeEvents(
        String acctId, 
        String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit, boolean ascending,
        String addtnlSelect,
        DBRecordHandler<EventData> rcdHandler)
        throws DBException
    {

        /* get record selector */
        DBSelect<EventData> dsel = EventData._createRangeEventSelector(
            acctId, devId, 
            timeStart, timeEnd,
            statCode,
            validGPS, 
            limitType, limit, ascending,
            addtnlSelect);

        /* invalid arguments? */
        if (dsel == null) {
            return EMPTY_ARRAY;
        }

        /* debug: compare SQL selections */
        if (DBConnection.getShowExecutedSQL()) {
            Print.logInfo("SQL Select comparison:");
            Print.logInfo(" DBSelect[MySQL/MyISAM]: " + dsel.toString(DBProvider.MySQL_MyISAM_Name));
            Print.logInfo(" DBSelect[MySQL/InnoDB]: " + dsel.toString(DBProvider.MySQL_InnoDB_Name));
            Print.logInfo(" DBSelect[PostgreSQL  ]: " + dsel.toString(DBProvider.PostgreSQL_Name  ));
            Print.logInfo(" DBSelect[MS/SQLServer]: " + dsel.toString(DBProvider.MS_SQLServer_Name));
        }

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            //ed = (EventData[])DBRecord.select(EventData.getFactory(), dsel.toString(false), rcdHandler);
            ed = DBRecord.select(dsel, rcdHandler); // select:DBSelect
            // 'ed' _may_ be empty if (rcdHandler != null)
        } finally {
            DBProvider.unlockTables();
        }
        if (ed == null) {
            // no records
            return EMPTY_ARRAY;
        } else
        if (dsel.isOrderAscending() == ascending) {
            // records are in requested order, return as-is
            return ed;
        } else {
            // records are in descending order
            // reorder to ascending order
            int lastNdx = ed.length - 1;
            for (int i = 0; i < ed.length / 2; i++) {
                EventData edrcd = ed[i];
                ed[i] = ed[lastNdx - i];
                ed[lastNdx - i] = edrcd;
            }
            return ed;
        }

    }

    /* return count in range of EventData records */
    public static long countRangeEvents(
        String acctId, String devId,
        long timeStart, long timeEnd,
        int statCode[],
        boolean validGPS,
        EventData.LimitType limitType, long limit,
        String addtnlSelect)
        throws DBException
    {

        /* get record selector */
        DBSelect<EventData> dsel = EventData._createRangeEventSelector(
            acctId, devId, 
            timeStart, timeEnd,
            statCode,
            validGPS, 
            limitType, limit, true/*ascending*/,
            addtnlSelect);

        /* invalid arguements? */
        if (dsel == null) {
            return 0L;
        }

        /* count events */
        long recordCount = 0L;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            recordCount = DBRecord.getRecordCount(dsel);
        } finally {
            DBProvider.unlockTables();
        }
        return recordCount;

    }

    /** 
    *** Gets the number of EventData records for the specified Account/Device
    *** within the specified range.
    *** @param acctId     The Account ID
    *** @param devId      The Device ID
    *** @param timeStart  The starting time range (inclusive)
    *** @param timeEnd    The ending time range (inclusive)
    *** @return The number of records within the specified range
    **/
    public static long getRecordCount(
        String acctId, String devId,
        long timeStart, long timeEnd)
        throws DBException
    {
        StringBuffer wh = new StringBuffer();
        wh.append(EventData.getWhereClause(
            acctId, devId,
            timeStart, timeEnd,
            null  /*statCode[]*/ ,
            false /*gpsRequired*/,
            null  /*andSelect*/  ));
        return DBRecord.getRecordCount(EventData.getFactory(), wh);
    }

    // ------------------------------------------------------------------------

    /* get EventData records by "creationMillis" (does not return null) */
    public static EventData[] getEventsByCreationMillis(
        String acctId, 
        String devId,
        long createStartMS, long createEndMS,
        long limit)
        throws DBException
    {
        DBFactory<EventData> dbFact = EventData.getFactory();

        /* invalid account/device */
        if (StringTools.isBlank(acctId)) {
            return EMPTY_ARRAY;
        } else
        if (StringTools.isBlank(devId)) {
            return EMPTY_ARRAY;
        }

        /* invalid time range */
        if ((createStartMS > 0L) && (createEndMS > 0L) && (createStartMS > createEndMS)) {
            return EMPTY_ARRAY;
        }

        /* does "creationMillis" exist? */
        if (!dbFact.hasField(EventData.FLD_creationMillis)) {
            Print.logError("EventData table does not contain field '"+EventData.FLD_creationMillis+"'");
            return EMPTY_ARRAY;
        }

        /* create/return DBSelect */
        // DBSelect: [SELECT * FROM EventData] <Where> ORDER BY <FLD_creationMillis> LIMIT <Limit>
        DBSelect<EventData> dsel = new DBSelect<EventData>(dbFact);
        DBWhere dwh = new DBWhere(dbFact);
        dwh.append(dwh.EQ(EventData.FLD_accountID, acctId));
        if (!StringTools.isBlank(devId) && !devId.equals("*")) {
            dwh.append(dwh.AND_(dwh.EQ(EventData.FLD_deviceID, devId)));
        }
        if (createStartMS >= 0L) {
            // AND (creationMillis>=123436789000)
            dwh.append(dwh.AND_(dwh.GE(EventData.FLD_creationMillis,createStartMS)));
        }
        if ((createEndMS >= 0L) && (createEndMS >= createStartMS)) {
            // AND (creationMillis<=123456789000)
            dwh.append(dwh.AND_(dwh.LE(EventData.FLD_creationMillis,createEndMS)));
        }
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(FLD_creationMillis,FLD_timestamp);
        dsel.setOrderAscending(true);
        dsel.setLimit(limit);

        /* get events */
        EventData ed[] = null;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            ed = DBRecord.select(dsel, null/*rcdHandler*/); // select:DBSelect
        } finally {
            DBProvider.unlockTables();
        }
        if (ed == null) {
            // no records
            return EMPTY_ARRAY;
        } else {
            return ed;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Delete events which are in the future
    *** @param device      The Device record for which EventData records will be deleted
    *** @param futureTime  The time in the future after which events will be deleted.  
    ***                    This time must be more than 60 seconds beyond the current system clock time.
    *** @return The number of events deleted.
    **/
    public static long deleteFutureEvents(
        Device device,
        long futureTime)
        throws DBException
    {

        /* valid Device */
        if (device == null) {
            throw new DBException("Device not specified");
        }
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();

        /* delete future events */
        return EventData.deleteFutureEvents(acctID, devID, futureTime);

    }

    /**
    *** Delete events which are in the future
    *** @param acctID      The Account ID
    *** @param devID       The Device ID
    *** @param futureTime  The time in the future after which events will be deleted.  
    ***                    This time must be more than 60 seconds beyond the current system clock time.
    *** @return The number of events deleted.
    **/
    public static long deleteFutureEvents(
        String acctID, String devID,
        long futureTime)
        throws DBException
    {

        /* valid Device */
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID)) {
            throw new DBException("AccountID/DeviceID not specified");
        }

        /* validate specified time */
        // protection against deleting previous events
        long minTime = DateTime.getCurrentTimeSec() + 60L;
        if (futureTime <= minTime) {
            throw new DBException("Invalid future time specified");
        }

        /* delete */
        return EventData.deleteEventsAfterTimestamp(acctID, devID, futureTime, true);

    }

    /**
    *** Delete events which are after the specified timestamp (exclusive)
    *** @param acctID      The Account ID
    *** @param devID       The Device ID
    *** @param timestamp   The time after which all events will be deleted.  
    *** @param inclusive   True to include 'timestamp', false to exclude
    *** @return The number of events deleted.
    **/
    public static long deleteEventsAfterTimestamp(
        String acctID, String devID,
        long timestamp, boolean inclusive)
        throws DBException
    {
        long delFromTime = inclusive? timestamp : (timestamp + 1L);

        /* valid Device */
        if (StringTools.isBlank(acctID) || StringTools.isBlank(devID)) {
            throw new DBException("AccountID/DeviceID not specified");
        }

        /* count events in range */
        long count = EventData.getRecordCount(acctID,devID,delFromTime,-1L); // -1 for InnoDB?
        if (count == 0L) {
            // -- already empty range
            return 0L;
        } else
        if (count < 0L) { // InnoDB
            // -- unable to count (InnoDB?)
            Print.logWarn("Unable to count events (InnoDB?) ... continuing ...");
        }

        /* SQL statement */
        // DBDelete: DELETE FROM EventData WHERE ((accountID='acct) AND (deviceID='dev') AND (timestamp>delFromTime))
        DBDelete ddel = new DBDelete(EventData.getFactory());
        DBWhere dwh = ddel.createDBWhere();
        ddel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(EventData.FLD_accountID,acctID),
                dwh.EQ(EventData.FLD_deviceID ,devID),
                dwh.GE(EventData.FLD_timestamp,delFromTime)  // greater-than-or-equals-to
            )
        ));

        /* delete */
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting future EventData records", sqe);
        } finally {
            DBConnection.release(dbc);
        }

        /* return count */
        return count;

    }

    // ------------------------------------------------------------------------
    
    /**
    *** Delete events which are in the future.<br>
    *** Note: Will return -1 if EventData table is InnoDB.  
    ***       Old events will still be deleted, however it will still go through the
    ***       motions of attempting to delete events, event if the range is empty.
    *** @param acctID      The Account ID
    *** @param devID       The Device ID
    *** @param oldTimeSec  The time in the past before which (exclusive) events will be deleted.  
    *** @return The number of events deleted.
    **/
    public static long deleteOldEvents(
        Device device,
        long oldTimeSec, 
        StringBuffer msg)
        throws DBException
    {

        /* valid Device */
        if (device == null) {
            throw new DBException("Device not specified");
        }

        /* Account */
        Account account = device.getAccount();
        if (account == null) {
            throw new DBException("Account is null"); // unlikely
        }

        /* compare oldTimeSec against retained age */
        long delOldTimeSec = account.adjustRetainedEventTime(oldTimeSec);
        if (delOldTimeSec != oldTimeSec) {
            oldTimeSec = delOldTimeSec;
            if (msg != null) {
                if (msg.length() > 0) { msg.append(", "); }
                msg.append("Using retained-date"); 
            }
        }

        /* "oldTimeSec" must not be less than "1" */
        if (oldTimeSec < 1L) {
            oldTimeSec = 1L;
        }

        /* get time of very last event for this device */
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();
        EventData ev[] = EventData.getRangeEvents(
            acctID, devID,
            -1L/*timeStart*/, -1L/*timeEnd*/,
            null/*statusCodes*/,
            false/*validGPS*/,
            EventData.LimitType.LAST, 1L/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        boolean savingLastEvent = false;
        long lastTimestamp = !ListTools.isEmpty(ev)? ev[0].getTimestamp() : 0L;
        if (lastTimestamp <= 0L) {
            // no events found for this device
            if (msg != null) {
                if (msg.length() > 0) { msg.append(", "); }
                msg.append("No events");
            }
        } else
        if (lastTimestamp <= oldTimeSec) {
            // last event is prior to delete time threashold.  Move 'oldTimeSec" to
            // just prior to last timestamp in order to save at elast one event
            // for this device.
            oldTimeSec = lastTimestamp; // deletion is non-inclusive
            savingLastEvent = true;
            if (msg != null) {
                if (msg.length() > 0) { msg.append(", "); }
                msg.append("Saved last event");
            }
        }

        /* count events in range */
        long count = EventData.getRecordCount(acctID,devID,-1L,(oldTimeSec - 1L)); // -1 for InnoDB?
        if (count == 0L) {
            // already empty range
            if (msg != null) {
                if (msg.length() > 0) { msg.append(", "); }
                if (savingLastEvent) {
                    // an empty-range is normal/expected if savingLastEvent is true.
                    msg.append("Nothing to delete");
                } else {
                    msg.append("Empty range");
                }
            }
            return 0L;
        } else
        if (count < 0L) { // InnoDB
            // -- unable to count (InnoDB?)
            Print.logWarn("Unable to count events (InnoDB?) ... continuing ...");
        }

        /* SQL statement */
        // DBDelete: DELETE FROM EventData WHERE ((accountID='acct) AND (deviceID='dev') AND (timestamp<oldTimeSec))
        DBDelete ddel = new DBDelete(EventData.getFactory());
        DBWhere dwh = ddel.createDBWhere();
        ddel.setWhere(dwh.WHERE_(
            dwh.AND(
                dwh.EQ(EventData.FLD_accountID,acctID),
                dwh.EQ(EventData.FLD_deviceID ,devID),
                dwh.LT(EventData.FLD_timestamp,oldTimeSec) // non-inclusive
            )
        ));

        /* delete */
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDefaultConnection();
            dbc.executeUpdate(ddel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting old EventData records", sqe);
        } finally {
            DBConnection.release(dbc);
        }

        /* return count */
        return count; // -1 for InnoDB

    }

    // ------------------------------------------------------------------------

    private static class GPSDistanceAccumulator
        implements DBRecordHandler<EventData>
    {
        private double accumKM = 0.0;
        private GeoPoint startingGP = null;
        private EventData lastEvent = null;
        public GPSDistanceAccumulator() {
        }
        public GPSDistanceAccumulator(GeoPoint startingGP, double startingOdomKM) {
            this();
            this.startingGP = startingGP;
            this.accumKM = startingOdomKM;
        }
        public int handleDBRecord(EventData rcd) throws DBException 
        {
            EventData ev = rcd;
            if (this.lastEvent != null) {
                GeoPoint lastGP = this.lastEvent.getGeoPoint();
                GeoPoint thisGP = ev.getGeoPoint();
                double distKM = lastGP.kilometersToPoint(thisGP);
                this.accumKM += distKM;
            } else
            if (this.startingGP != null) {
                GeoPoint thisGP = ev.getGeoPoint();
                double distKM = this.startingGP.kilometersToPoint(thisGP);
                this.accumKM += distKM;
            }
            this.lastEvent = ev;
            return DBRH_SKIP;
        }
        public void clearGPSDistanceTraveled() {
            this.accumKM = 0.0;
        }
        public double getGPSDistanceTraveledKM() {
            return this.accumKM;
        }
    }

    public static double getGPSDistanceTraveledKM(String acctId, String devId,
        long timeStart, long timeEnd,
        GeoPoint startingGP, double startingOdomKM)
    {

        /* record handler */
        GPSDistanceAccumulator rcdHandler = new GPSDistanceAccumulator(startingGP, startingOdomKM);

        /* look through events */
        try {
            EventData.getRangeEvents(
                acctId, devId,
                timeStart, timeEnd,
                null/*StatusCodes*/,
                true/*validGPS*/,
                EventData.LimitType.LAST, -1L/*limit*/, true/*ascending*/,
                null/*addtnlSelect*/,
                rcdHandler);
        } catch (DBException dbe) {
            Print.logException("Calculating GPS distance traveled", dbe);
        }

        /* return distance */
        return rcdHandler.getGPSDistanceTraveledKM();
        
    }
 
    // ------------------------------------------------------------------------

    public static DateTime parseDate(String dateStr, TimeZone tz)
    {
        // Formats:
        //   YYYY/MM[/DD[/hh[:mm[:ss]]]]
        //   eeeeeeeeeee
        String dateFld[] = StringTools.parseStringArray(dateStr, "/:");
        if ((dateFld == null) || (dateFld.length == 0)) {
            return null; // no date specified
        } else
        if (dateFld.length == 1) {
            // parse as 'Epoch' time
            long epoch = StringTools.parseLong(dateFld[0], -1L);
            return (epoch > 0L)? new DateTime(epoch,tz) : null;
        } else {
            // (dateFld.length >= 2)
            int YY = StringTools.parseInt(dateFld[0], -1); // 1900..2007+
            int MM = StringTools.parseInt(dateFld[1], -1); // 1..12
            if ((YY < 1900) || (MM < 1) || (MM > 12)) {
                return null;
            } else {
                int DD = 1;
                int hh = 0, mm = 0, ss = 0;    // default to beginning of day
                if (dateFld.length >= 3) {
                    // at least YYYY/MM/DD provided
                    DD = StringTools.parseInt(dateFld[2], -1);
                    if (DD < 1) {
                        DD = 1;
                    } else
                    if (DD > DateTime.getDaysInMonth(tz,MM,YY)) {
                        DD = DateTime.getDaysInMonth(tz,MM,YY);
                    } else {
                        if (dateFld.length >= 4) { hh = StringTools.parseInt(dateFld[3], 0); }
                        if (dateFld.length >= 5) { mm = StringTools.parseInt(dateFld[4], 0); }
                        if (dateFld.length >= 6) { ss = StringTools.parseInt(dateFld[5], 0); }
                    }
                } else {
                    // only YYYY/MM provided
                    DD = 1; // first day of month
                }
                return new DateTime(tz, YY, MM, DD, hh, mm, ss);
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This section support a method for obtaining human readable information from
    // the EventData record for reporting, or email purposes.  This is currently
    // used by the 'rules' engine when generating notification emails, and any
    // optional fields added to the map info-balloon

    public  static final String KEY_ACCOUNT[]         = new String[] { "account"          , "accountDesc"     };  // "opendmtp"
    public  static final String KEY_DEVICE_COUNT[]    = new String[] { "deviceCount"      , "devCount"        };  // "123"
    public  static final String KEY_DEVICE[]          = new String[] { "device"           , "deviceDesc"      };  // "mobile"
    public  static final String KEY_EVENT_COUNT24[]   = new String[] { "eventCount24"                         };  // "1017" (arg <statusCode>)
    public  static final String KEY_DEVICE_LINK[]     = new String[] { "deviceLink"       , "devLink"         };  // "<a href='http://...'>Description</a>"
    public  static final String KEY_DEV_CONN_AGE[]    = new String[] { "devConnectAge"    , "connectAge"      };  // "00:13:45"
    public  static final String KEY_DEV_TRAILERS[]    = new String[] { "deviceEntities"   , "deviceTrailers"  };  // "Trailer 1234, Trailer 4321"

    public  static final String KEY_DATETIME[]        = new String[] { "dateTime"         , "date"            };  // "2007/08/09 03:02:51 GMT"
    public  static final String KEY_DATE_YEAR[]       = new String[] { "dateYear"         , "year"            };  // "2007"
    public  static final String KEY_DATE_MONTH[]      = new String[] { "dateMonth"        , "month"           };  // "January"
    public  static final String KEY_DATE_DAY[]        = new String[] { "dateDay"        , "dayOfMonth", "day" };  // "23"
    public  static final String KEY_DATE_DOW[]        = new String[] { "dateDow"          , "dayOfWeek"       };  // "Monday"
    public  static final String KEY_TIMEZONE[]        = new String[] { "timeZone"         , "tmz"             };  // "US/Pacific"
    public  static final String KEY_TIME[]            = new String[] { "time"                                 };  // "03:02:51"

    public  static final String KEY_CREATE_DATETIME[] = new String[] { "creationDateTime" , "creationDate"    };  // "2007/08/09 03:02:51 GMT"
    public  static final String KEY_CREATE_AGE[]      = new String[] { "creationAge"                          };  // "00:13:45"

    private static final String KEY_STATUSDESC[]      = new String[] { "status"                               };  // "Location"
    private static final String KEY_GEOPOINT[]        = new String[] { "geopoint"                             };  // "39.12345,-142.12345"
    private static final String KEY_LATITUDE[]        = new String[] { "latitude"                             };  // "39.12345"
    private static final String KEY_LONGITUDE[]       = new String[] { "longitude"                            };  // "-142.12345"
    private static final String KEY_GPS_AGE[]         = new String[] { "gpsAge"                               };  // "00:13:45"
    private static final String KEY_SPEED[]           = new String[] { "speed"                                };  // "34.9 mph"
    private static final String KEY_SPEED_LIMIT[]     = new String[] { "speedLimit"                           };  // "45.0 mph"
    private static final String KEY_DIRECTION[]       = new String[] { "direction"        , "compass"         };  // "SE"
    private static final String KEY_HEADING[]         = new String[] { "heading"        , "bearing", "course" };  // "123.4"
    private static final String KEY_ODOMETER[]        = new String[] { "odometer"                             };  // "1234 Miles"
    private static final String KEY_DISTANCE[]        = new String[] { "distance"                             };  // "1234 Miles"
    private static final String KEY_ALTITUDE[]        = new String[] { "alt"              , "altitude"        };  // "12345 feet"
    private static final String KEY_RPT_DISTANCE[]    = new String[] { "reportDistance"                       };  // "37 Miles"

    private static final String KEY_BATTERY_LEVEL[]   = new String[] { "batteryLevel"                         };  // "75%"
    private static final String KEY_BATTERY_VOLTS[]   = new String[] { "batteryVolts"                         };  // "4.5"
    private static final String KEY_VEH_BATTERY_V[]   = new String[] { "vBatteryVolts"    , "vehicleVolts"    };  // "12.8"
    private static final String KEY_FUEL_LEVEL[]      = new String[] { "fuelLevel"                            };  // "25.0 %"
    private static final String KEY_FUEL_LEVEL2[]     = new String[] { "fuelLevel2"                           };  // "25.0 %"
    private static final String KEY_FUEL_VOLUME[]     = new String[] { "fuelLevelVolume"  , "fuelVolume"      };  // "12 gal"
    private static final String KEY_TIRE_TEMP[]       = new String[] { "tireTemperature"  , "tireTemp"        };  // "32.0,31.3,37.2,30.0 C"
    private static final String KEY_TIRE_PRESSURE[]   = new String[] { "tirePressure"     , "tirePress"       };  // "32.0,31.3,37.2,30.0 psi"

    private static final String KEY_ADDRESS[]         = new String[] { "fullAddress"      , "address"         };  // "1234 Somewhere Lane, Smithsville, CA 99999"
    private static final String KEY_STREETADDR[]      = new String[] { "streetAddress"    , "street"          };  // "1234 Somewhere Lane"
    private static final String KEY_CITY[]            = new String[] { "city"                                 };  // "Smithsville"
    private static final String KEY_STATE[]           = new String[] { "state"            , "province"        };  // "CA"
    private static final String KEY_POSTALCODE[]      = new String[] { "postalCode"       , "zipCode"         };  // "98765"
    private static final String KEY_SUBDIVISION[]     = new String[] { "subdivision"      , "subdiv"          };  // "US/CA"

    public  static final String KEY_FAULT_CODE[]      = new String[] { "faultCode"                            };  // 
    public  static final String KEY_FAULT_CODES[]     = new String[] { "faultCodes"                           };  // 
    public  static final String KEY_FAULT_HEADER[]    = new String[] { "faultHeader"                          };  // 
    public  static final String KEY_FAULT_DESC[]      = new String[] { "faultDesc"                            };  // 

    private static final String KEY_GEOZONEID[]       = new String[] { "geozoneID"                            };  // "home"
    private static final String KEY_GEOZONE[]         = new String[] { "geozone"          , "geozoneDesc"     };  // "Home Base"
    private static final String KEY_GEOZONENAME[]     = new String[] { "geozoneName"                          };  // "Home"
    private static final String KEY_ENTITYID[]        = new String[] { "entityID"                             };  // "t1234"
    private static final String KEY_ENTITY[]          = new String[] { "entity"           , "entityDesc"      };  // "Trailer 1234"
    private static final String KEY_SERVICE_NOTES[]   = new String[] { "serviceNotes"     ,                   };  // (Device field)
    private static final String KEY_MAPLINK[]         = new String[] { "maplink"          , "mapurl"          };  // "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q=39.12345,-142.12345"

    public  static final String KEY_DRIVERID[]        = new String[] { "driverID"                             };  // "smith"
    public  static final String KEY_DRIVER_DESC[]     = new String[] { "driverDesc" , "driver", "driverName"  };  // "Joe Smith"
    public  static final String KEY_DRIVER_BADGE[]    = new String[] { "driverBadgeID"    , "driverBadge"     };  // "X123"
    public  static final String KEY_DRIVER_LICENSE[]  = new String[] { "driverLicense"                        };  // "N123456789"
    public  static final String KEY_DRIVER_PHONE[]    = new String[] { "driverPhone"                          };  // "9015551212"

    private static final String KEY_INPUT_MASK[]      = new String[] { "inputMask"                            };  // "0001010"
    private static final String KEY_INPUT_BIT[]       = new String[] { "inputBit"                             };  // "Off"
    private static final String KEY_OUTPUT_MASK[]     = new String[] { "outputMask"                           };  // "0001010"
    private static final String KEY_OUTPUT_BIT[]      = new String[] { "outputBit"                            };  // "On"
    private static final String KEY_SEATBELT_MASK[]   = new String[] { "seatbeltMask"                         };  // "0001010"
    private static final String KEY_SEATBELT_BIT[]    = new String[] { "seatbeltBit"                          };  // "On"

    private static final String KEY_TEMPERATURE[]     = new String[] { "temperature", "temp", "thermoAverage" };  // 47.3F
  //private static final String KEY_TEMP_0[]          = new String[] { "temp0", "thermoAverage0"              };  // 47.3F
  //private static final String KEY_TEMP_1[]          = new String[] { "temp1", "thermoAverage1"              };  // 47.3F
  //private static final String KEY_TEMP_2[]          = new String[] { "temp2", "thermoAverage2"              };  // 47.3F
  //private static final String KEY_TEMP_3[]          = new String[] { "temp3", "thermoAverage3"              };  // 47.3F
  //private static final String KEY_TEMP_4[]          = new String[] { "temp4", "thermoAverage4"              };  // 47.3F
  //private static final String KEY_TEMP_5[]          = new String[] { "temp5", "thermoAverage5"              };  // 47.3F
  //private static final String KEY_TEMP_6[]          = new String[] { "temp6", "thermoAverage6"              };  // 47.3F
  //private static final String KEY_TEMP_7[]          = new String[] { "temp7", "thermoAverage7"              };  // 47.3F
    private static final String KEY_TEMP_ALL[]        = new String[] { "temp0", "temp1", "temp2", "temp3", "temp4", "temp5", "temp6", "temp7" };

    private static final String KEY_ETA_DATETIME[]    = new String[] { "etaDateTime"                          };  // 
    private static final String KEY_ETA_UNIQUE_ID[]   = new String[] { "etaUniqueID"      , "etaID"           };  // 
    private static final String KEY_ETA_DISTANCE[]    = new String[] { "etaDistanceKM"                        };  // 
    private static final String KEY_ETA_GEOPOINT[]    = new String[] { "etaGeoPoint"                          };  // 
    private static final String KEY_STOP_ID[]         = new String[] { "stopUniqueID"     , "stopID"          };  // 
    private static final String KEY_STOP_STATUS[]     = new String[] { "stopStatus"                           };  // 
    private static final String KEY_STOP_INDEX[]      = new String[] { "stopIndex"                            };  // 

    private static final String KEY_STOPPED_TIME[]    = new String[] { "elapsedTimeStopped", "stoppedTimeSec" }; //

    public static boolean _keyMatch(String key, String keyMatch)
    {
        if (key.equalsIgnoreCase(keyMatch)) {
            return true;
        }
        return false;
    }

    public static boolean _keyMatch(String key, String keyList[])
    {
        for (int i = 0; i < keyList.length; i++) {
            if (key.equalsIgnoreCase(keyList[i])) {
                return true;
            }
        }
        return false;
    }

    public static String getKeyFieldTitle(String key, String arg, Locale locale)
    {
        return EventData._getKeyFieldString(
            true/*title*/, key, arg, 
            locale, null/*BasicPrivateLabel*/, null/*EventData*/);
    }

    // getFieldValueString
    public String getKeyFieldValue(String key, String arg, BasicPrivateLabel bpl)
    {
        Locale locale = (bpl != null)? bpl.getLocale() : null;
        return EventData._getKeyFieldString(
            false/*value*/, key, arg, 
            locale, bpl, this);
    }

    public static String _getKeyFieldString(
        boolean getTitle, String key, String arg, 
        Locale locale, BasicPrivateLabel bpl, EventData ed)
    {

        /* check for valid field name */
        if (key == null) {
            return null;
        } else
        if ((ed == null) && !getTitle) {
            return null;
        }
        if ((locale == null) && (bpl != null)) { locale = bpl.getLocale(); }
        I18N i18n = I18N.getI18N(EventData.class, locale);
        long now = DateTime.getCurrentTimeSec();
        if (arg == null) { arg = ""; }

        /* Account/Device values */
        if (EventData._keyMatch(key,EventData.KEY_ACCOUNT)) {
            if (getTitle) {
                return i18n.getString("EventData.key.accountDescription", "Account");
            } else {
                Account account = ed.getAccount();
                if (arg.equalsIgnoreCase("id") || (account == null)) {
                    return ed.getAccountID();
                } else {
                    // "desc"
                    return account.getDescription();
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE_COUNT)) {
            if (getTitle) {
                return i18n.getString("EventData.key.deviceCount", "Device Count");
            } else {
                Account account = ed.getAccount();
                return (account != null)? String.valueOf(account.getDeviceCount()) : "?";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.deviceDescription", "Device");
            } else {
                Device device = ed.getDevice();
                if (arg.equalsIgnoreCase("id") || (device == null)) {
                    // -- device-id
                    return ed.getDeviceID();
                } else {
                    // -- description
                    String desc = device.getDescription();
                    if (!StringTools.isBlank(desc)) {
                        // -- display the device id
                        return desc;
                    } else {
                        // -- description is blank, at least display the device-id
                        return "(" + ed.getDeviceID() + ")";
                    }
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_EVENT_COUNT24)) {
            // EXPERIMENTAL
            if (getTitle) {
                return i18n.getString("EventData.key.24HourEventCount", "24Hr Event Count");
            } else {
                // arg: <PrevHours>,<StatusCode>
                String a[]       = StringTools.split(arg,',');
                int sinceHH      = (a.length > 1)? StringTools.parseInt(a[0],24) : 24;
                int statCodes[]  = ((a.length > 2) && !StringTools.isBlank(a[1]))? 
                    new int[] { StringTools.parseInt(a[1],StatusCodes.STATUS_NONE) } : 
                    null;
                    long timeStart = now - DateTime.HourSeconds((sinceHH > 0)? sinceHH : 24);
                long timeEnd     = -1L;
                long recordCount = -1L;
                try {
                    recordCount = EventData.countRangeEvents(
                        ed.getAccountID(), ed.getDeviceID(),
                        timeStart, timeEnd,
                        statCodes,
                        false/*validGPS*/,
                        EventData.LimitType.LAST/*limitType*/, -1L/*limit*/, // no limit
                        null/*where*/);
                } catch (DBException dbe) {
                    Print.logError("Unable to obtain EventData record count [" + dbe);
                }
                return String.valueOf(recordCount);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE_LINK)) {
            if (getTitle) {
                return i18n.getString("EventData.key.deviceLink", "Device Link");
            } else {
                Device device = ed.getDevice();
                if (device != null) {
                    String url = device.getLinkURL();
                    String dsc = StringTools.blankDefault(device.getLinkDescription(),
                        i18n.getString("EventData.key.link", "Link"));
                    if (StringTools.isBlank(url)) {
                        return "";
                    } else
                    if (StringTools.isBlank(arg)    || 
                        arg.equalsIgnoreCase("a")   ||  // "anchor"
                        arg.equalsIgnoreCase("html")  ) {
                        if (!StringTools.isBlank(url)) {
                            return EventUtil.MAP_ESCAPE_HTML+"<a href='"+url+"' target='_blank'>"+dsc+"</a>";
                        } else {
                            return EventUtil.MAP_ESCAPE_HTML+"<a>"+dsc+"</a>";
                        }
                    } else
                    if (arg.equalsIgnoreCase("plain") ||
                        arg.equalsIgnoreCase("desc")    ) {
                        return dsc + ": " + url;
                    } else { // "url"
                        return url;
                    }
                } else {
                    return "";
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEV_CONN_AGE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.sinceLastConnect", "Since Connection");
            } else {
                Device device = ed.getDevice();
                if (device != null) {
                    long lastConnectTime = device.getLastTotalConnectTime();
                    if (lastConnectTime <= 0L) {
                        return "--:--:--";
                    }
                    long ageSec = DateTime.getCurrentTimeSec() - lastConnectTime;
                    if (ageSec < 0L) { ageSec = 0L; }
                    long hours  = (ageSec        ) / 3600L;
                    long min    = (ageSec % 3600L) /   60L;
                    long sec    = (ageSec %   60L);
                    StringBuffer sb = new StringBuffer();
                    sb.append(hours).append(":");
                    if (min   < 10) { sb.append("0"); }
                    sb.append(min  ).append(":");
                    if (sec   < 10) { sb.append("0"); }
                    sb.append(sec  );
                    return sb.toString();
                } else {
                    return "--:--:--";
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEV_TRAILERS)) {
            if (getTitle) {
                return i18n.getString("EventData.key.attachedTrailers", "Attached Trailers");
            } else {
                Device device = ed.getDevice();
                if (device != null) {
                    String e[] = device.getAttachedEntityDescriptions(EntityManager.EntityType.TRAILER);
                    if ((e != null) && (e.length > 0)) {
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < e.length; i++) { 
                            if (i > 0) { sb.append(","); }
                            sb.append(e[i]);
                        }
                        return sb.toString();
                    } else {
                        return "";
                    }
                } else {
                    return "";
                }
            }
        }

        /* Date/Time values */
        if (EventData._keyMatch(key,EventData.KEY_DATETIME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.dateTime", "Date/Time");
            } else {
                return ed.getTimestampString();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_YEAR)) {
            if (getTitle) {
                return i18n.getString("EventData.key.dateYear", "Year");
            } else {
                return ed.getTimestampYear();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_MONTH)) {
            if (getTitle) {
                return i18n.getString("EventData.key.dateMonth", "Month");
            } else {
                if (StringTools.isBlank(arg)     ||
                    arg.equalsIgnoreCase("name") ||
                    arg.equalsIgnoreCase("text")   ) {
                    return ed.getTimestampMonth(false, locale);
                } else
                if (arg.equalsIgnoreCase("abbr")  ||
                    arg.equalsIgnoreCase("abbrev")  ) {
                    return ed.getTimestampMonth(true, locale);
                } else {
                    // "#", "num"
                    return ed.getTimestampMonthNumber();
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_DAY)) {
            if (getTitle) {
                return i18n.getString("EventData.key.dateDay", "Day");
            } else {
                return ed.getTimestampDayOfMonth();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DATE_DOW)) {
            if (getTitle) {
                return i18n.getString("EventData.key.dayOfWeek", "Day Of Week");
            } else {
                return ed.getTimestampDayOfWeek(false, locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIMEZONE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.timeZone", "TimeZone");
            } else {
                TimeZone tmz = ed.getTimeZone(); // non-null
                return tmz.getDisplayName(locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.time", "Time");
            } else {
                return ed.getTimestampTime();
            }
        }

        /* Creation Date/Time values */
        if (EventData._keyMatch(key,EventData.KEY_CREATE_DATETIME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.creationDateTime", "Creation\nDate/Time");
            } else {
                return ed.getCreationTimeString();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_CREATE_AGE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.creationAge", "Creation\nAge");
            } else {
                long ageSec = ed.getCreationAge();
                if (ageSec > 0L) {
                    long hours  = (ageSec        ) / 3600L;
                    long min    = (ageSec % 3600L) /   60L;
                    long sec    = (ageSec %   60L);
                    StringBuffer sb = new StringBuffer();
                    sb.append(hours).append(":");
                    if (min < 10) { sb.append("0"); }
                    sb.append(min  ).append(":");
                    if (sec < 10) { sb.append("0"); }
                    sb.append(sec  );
                    return sb.toString();
                } else {
                    return "--:--:--";
                }
            }
        }

        /* Event/GPS Values */
        if (EventData._keyMatch(key,EventData.KEY_STATUSDESC)) {
            if (getTitle) {
                return i18n.getString("EventData.key.statusCode", "Status Code");
            } else {
                return ed.getStatusCodeDescription(bpl);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_GEOPOINT)) {
            if (getTitle) {
                return i18n.getString("EventData.key.latitudeLongitude", "Latitude/Longitude");
            } else {
                Account.LatLonFormat latlonFmt = Account.getLatLonFormat(ed.getAccount());
                double lat = ed.getLatitude();
                double lon = ed.getLongitude();
                String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
                String latStr = GeoPoint.formatLatitude( lat, fmt, locale);
                String lonStr = GeoPoint.formatLongitude(lon, fmt, locale);
                return latStr + GeoPoint.PointSeparator + lonStr;
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_LATITUDE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.latitude", "Latitude");
            } else {
                Account.LatLonFormat latlonFmt = Account.getLatLonFormat(ed.getAccount());
                double lat = ed.getLatitude();
                String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
                return GeoPoint.formatLatitude(lat, fmt, locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_LONGITUDE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.longitude", "Longitude");
            } else {
                Account.LatLonFormat latlonFmt = Account.getLatLonFormat(ed.getAccount());
                double lon = ed.getLongitude();
                String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
                return GeoPoint.formatLongitude(lon, fmt, locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_GPS_AGE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.gpsAge", "GPS Age");
            } else {
                long ageSec = ed.getGpsAge(); // gps fix age relative to the event timestamp
                if (ageSec < 0L) { ageSec = 0L; }
                long hours  = (ageSec        ) / 3600L;
                long min    = (ageSec % 3600L) /   60L;
                long sec    = (ageSec %   60L);
                StringBuffer sb = new StringBuffer();
                sb.append(hours).append(":");
                if (min   < 10) { sb.append("0"); }
                sb.append(min  ).append(":");
                if (sec   < 10) { sb.append("0"); }
                sb.append(sec  );
                return sb.toString();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_SPEED)) {
            if (getTitle) {
                return i18n.getString("EventData.key.speed", "Speed");
            } else {
                double kph = ed.getSpeedKPH();
                Account account = ed.getAccount();
                if (account != null) {
                    return account.getSpeedString(kph,true,locale);
                } else {
                    return StringTools.format(kph,"0") + " " + Account.SpeedUnits.KPH.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_SPEED_LIMIT)) {
            if (getTitle) {
                return i18n.getString("EventData.key.speedLimit", "Speed Limit");
            } else {
                double kph = ed.getSpeedLimitKPH();
                if (kph <= 0.0) {
                    Device device = ed.getDevice();
                    if (device != null) {
                        kph = device.getSpeedLimitKPH();
                        if (kph <= 0.0) { // still <= 0.0
                            return i18n.getString("EventData.notAvailable", "n/a");
                        }
                    }
                }
                Account account = ed.getAccount();
                if (account != null) {
                    return account.getSpeedString(kph,true,locale);
                } else {
                    return StringTools.format(kph,"0") + " " + Account.SpeedUnits.KPH.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DIRECTION)) {
            if (getTitle) {
                return i18n.getString("EventData.key.direction", "Heading");
            } else {
                double deg = ed.getHeading();
                if (arg.equalsIgnoreCase("deg")    ||
                    arg.equalsIgnoreCase("degrees")  ) {
                    return StringTools.format(deg,"0.0");
                } else {
                    // "desc"
                    return GeoPoint.GetHeadingString(deg,locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_HEADING)) {
            if (getTitle) {
                return i18n.getString("EventData.key.heading", "Heading");
            } else {
                double deg = ed.getHeading();
                if (arg.equalsIgnoreCase("desc")) {
                    return GeoPoint.GetHeadingString(deg,locale);
                } else {
                    // "deg", "degrees"
                    return StringTools.format(deg,"0.0");
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ODOMETER)) {
            if (getTitle) {
                return i18n.getString("EventData.key.odometer", "Odometer");
            } else {
                double  odomKM  = ed.getOdometerWithOffsetKM();
                Account account = ed.getAccount();
                if (account != null) {
                    return account.getDistanceString(odomKM, true, locale);
                } else {
                    return StringTools.format(odomKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DISTANCE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.distance", "Distance");
            } else {
                double distKM = ed.getDistanceKM();
                Account account = ed.getAccount();
                if (account != null) {
                    return account.getDistanceString(distKM, true, locale);
                } else {
                    return StringTools.format(distKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ALTITUDE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.altitude", "Altitude");
            } else {
                return ed.getAltitudeString(true, locale);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_RPT_DISTANCE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.reportDistance", "Report\nDistance");
            } else {
                double distKM = ed.getReportDistanceKM();
                Account account = ed.getAccount();
                if (account != null) {
                    return account.getDistanceString(distKM, true, locale);
                } else {
                    return StringTools.format(distKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                }
            }
        } 

        /* OBD, etc */
        if (EventData._keyMatch(key,EventData.KEY_BATTERY_LEVEL)) {
            if (getTitle) {
                return i18n.getString("EventData.key.batteryLevel", "Battery Level");
            } else {
                double BL = ed.getBatteryLevel();
                double P  = (BL < 0.0)? 0.0 : (BL <= 1.0)? (BL * 100.0) : BL;
                return Math.round(P) + "%";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_BATTERY_VOLTS)) {
            if (getTitle) {
                return i18n.getString("EventData.key.batteryVolts", "Battery Volts");
            } else {
                return StringTools.format(ed.getBatteryVolts(), "0.0");
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_VEH_BATTERY_V)) {
            if (getTitle) {
                return i18n.getString("EventData.key.vehBatteryVolts", "Veh. Batt Volts");
            } else {
                return StringTools.format(ed.getVBatteryVolts(), "0.0");
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_FUEL_LEVEL)) {
            if (getTitle) {
                return i18n.getString("EventData.key.fuelLevel", "Fuel Level");
            } else {
                double level = ed.getFuelLevel(true/*estimate*/);
                if (level < 0.0) {
                    return i18n.getString("EventData.notAvailable", "n/a");
                } else {
                    long pct = Math.round(level * 100.0);
                    return pct + "%";
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_FUEL_LEVEL2)) {
            if (getTitle) {
                return i18n.getString("EventData.key.fuelLevel2", "Fuel Level 2");
            } else {
                double level = ed.getFuelLevel2(); // device.getActualFuelLevel(ed.getFuelLevel2());
                if (level < 0.0) {
                    return i18n.getString("EventData.notAvailable", "n/a");
                } else {
                    long pct = Math.round(level * 100.0);
                    return pct + "%";
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_FUEL_VOLUME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.fuelVolume", "Fuel Volume");
            } else {
                Device device = ed.getDevice();
                if (device == null) {
                    return "";
                } else {
                    Account.VolumeUnits vu = Account.getVolumeUnits(ed.getAccount());
                    double L = device.getFuelCapacity() * ed.getFuelLevel(true/*estimate*/);
                    double V = vu.convertFromLiters(L);
                    return StringTools.format(V,"0.0") + " " + vu.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIRE_TEMP)) {
            if (getTitle) {
                return i18n.getString("EventData.key.tireTemp", "Tire Temp");
            } else {
                Account.TemperatureUnits tu = Account.getTemperatureUnits(ed.getAccount());
                double T[] = ed.getTireTemp_units(tu);
                StringBuffer sb = new StringBuffer();
                for (int t = 0; t < T.length; t++) {
                    if (sb.length() > 0) { sb.append(","); }
                    sb.append(StringTools.format(T[t],"0.0"));
                }
                sb.append(" ");
                sb.append(tu.toString(locale));
                return sb.toString();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_TIRE_PRESSURE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.tirePressure", "Tire Pressure");
            } else {
                Account.PressureUnits pu = Account.getPressureUnits(ed.getAccount());
                double P[] = ed.getTirePressure_units(pu);
                StringBuffer sb = new StringBuffer();
                for (int p = 0; p < P.length; p++) {
                    if (sb.length() > 0) { sb.append(","); }
                    sb.append(StringTools.format(P[p],"0.0"));
                }
                return sb + " " + pu.toString(locale);
            }
        } 

        /* Address values */
        if (EventData._keyMatch(key,EventData.KEY_ADDRESS)) {
            if (getTitle) {
                return i18n.getString("EventData.key.address", "Address");
            } else {
                return ed.getAddress();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_STREETADDR)) {
            if (getTitle) {
                return i18n.getString("EventData.key.streetAddress", "Street");
            } else {
                return ed.getStreetAddress();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_CITY)) {
            if (getTitle) {
                return i18n.getString("EventData.key.city", "City");
            } else {
                return ed.getCity();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_STATE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.state", "State");
            } else {
                return ed.getStateProvince();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_POSTALCODE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.postalCode", "Postal Code");
            } else {
                return ed.getPostalCode();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_SUBDIVISION)) {
            if (getTitle) {
                return i18n.getString("EventData.key.subdivision", "Country/State");
            } else {
                return ed.getSubdivision();
            }
        } 

        /* OBD fault values */
        if (EventData._keyMatch(key,EventData.KEY_FAULT_CODES)) {
            if (getTitle) {
                return i18n.getString("EventData.key.faultCodes", "Fault Codes");
            } else {
                String evFault = ed.getFieldValue(EventData.FLD_faultCode,"").toUpperCase();
                if (StringTools.isBlank(evFault)) {
                    // "faultCode" is blank, try "j1708Fault"
                    long fault = ed.getFieldValue(EventData.FLD_j1708Fault, 0L);
                    return DTOBDFault.GetFaultString(fault);
                } else {
                    RTProperties rtpFault = new RTProperties(evFault);
                    return DTOBDFault.GetFaultString(rtpFault);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_FAULT_CODE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.faultCode", "Fault Code");
            } else {
                long fault = ed.getFieldValue(EventData.FLD_j1708Fault, 0L);
                return DTOBDFault.GetFaultString(fault);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_FAULT_HEADER)) {
            if (getTitle) {
                return i18n.getString("EventData.key.faultHeader", "Fault Header");
            } else {
                long fault = ed.getFieldValue(EventData.FLD_j1708Fault, 0L);
                return DTOBDFault.GetFaultHeader(fault);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_FAULT_DESC)) {
            if (getTitle) {
                return i18n.getString("EventData.key.faultDescr", "Fault Description");
            } else {
                long fault = ed.getFieldValue(EventData.FLD_j1708Fault, 0L);
                return DTOBDFault.GetFaultDescription(fault, locale);
            }
        }

        /* Geozone */
        if (EventData._keyMatch(key,EventData.KEY_GEOZONEID)) {
            if (getTitle) {
                return i18n.getString("EventData.key.geozoneID", "Geozone ID");
            } else {
                return ed.getGeozoneID();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_GEOZONE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.geozoneDescription", "Geozone");
            } else {
                if (arg.equalsIgnoreCase("id")) {
                    return ed.getGeozoneID();
                } else
                if (arg.equalsIgnoreCase("name")) {
                    return ed.getGeozoneDisplayName();
                } else {
                    // "desc"
                    return ed.getGeozoneDescription();
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_GEOZONENAME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.geozoneName", "Geozone Name");
            } else {
                return ed.getGeozoneDisplayName();
            }
        }

        /* Entity */
        if (EventData._keyMatch(key,EventData.KEY_ENTITYID)) {
            if (getTitle) {
                return i18n.getString("EventData.key.entityID", "Entity ID");
            } else {
                return ed.getEntityID();
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ENTITY)) {
            if (getTitle) {
                return i18n.getString("EventData.key.entityDescription", "Entity");
            } else {
                if (arg.equalsIgnoreCase("id")) {
                    return ed.getEntityID();
                } else {
                    // "desc"
                    String aid  = ed.getAccountID();
                    String eid  = ed.getEntityID();
                    int    type = ed.getEntityType();
                    return Device.getEntityDescription(aid, eid, type);
                }
            }
        }

        /* Driver */
        if (EventData._keyMatch(key,EventData.KEY_DRIVERID)) {
            if (getTitle) {
                return i18n.getString("EventData.key.driverID", "Driver ID");
            } else {
                return ed.getDriverID(true);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DRIVER_DESC)) {
            if (getTitle) {
                return i18n.getString("EventData.key.driverDescription", "Driver");
            } else {
                // first check DriverID
                String driverID = ed.getDriverID(true);
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = ed.getDriver(true);
                    if (driver != null) {
                        return driver.getDescription();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return driverID;
                    }
                }
                // next try EntityID(DRIVER)
                int    entityType = ed.getEntityType();
                String entityID   = (entityType == EntityManager.EntityType.DRIVER.getIntValue())? ed.getEntityID() : null;
                if (!StringTools.isBlank(entityID)) {
                    String desc = Device.getEntityDescription(ed.getAccountID(), entityID, entityType);
                    if (!StringTools.isBlank(desc)) {
                        // return DriverID description from EntityManager
                        return desc;
                    } else {
                        // return EntityID
                        return entityID;
                    }
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DRIVER_BADGE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.driverBadge", "Driver Badge");
            } else {
                String driverID = ed.getDriverID(true); // fix 2.5.3-B34
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = ed.getDriver(true);
                    if (driver != null) {
                        return driver.getBadgeID();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return driverID;
                    }
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DRIVER_LICENSE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.driverLicense", "Driver License");
            } else {
                String driverID = ed.getDriverID(true); // fix 2.5.3-B34
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = ed.getDriver(true);
                    if (driver != null) {
                        return driver.getLicenseNumber();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return driverID;
                    }
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DRIVER_PHONE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.driverPhone", "Driver Phone");
            } else {
                String driverID = ed.getDriverID(true); // fix 2.5.4-B28
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = ed.getDriver(true);
                    if (driver != null) {
                        return driver.getContactPhone();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return "";
                    }
                }
                // return blank
                return "";
            }
        }

        /* digital input values */
        if (EventData._keyMatch(key,EventData.KEY_INPUT_MASK)) {
            if (getTitle) {
                return i18n.getString("EventData.key.inputMask", "Input Mask");
            } else {
                int input = (int)ed.getInputMask(); // bit mask
                String s = StringTools.toBinaryString(input);
                int slen = s.length();
                int blen = StringTools.parseInt(arg,8);
                int len  = (slen >= blen)? (slen - blen) : 0;
                return s.substring(len, slen);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_INPUT_BIT)) {
            int argBit = StringTools.parseInt(arg,0);
            if (getTitle) {
                return i18n.getString("EventData.key.inputBit", "Input Bit {0}", String.valueOf(argBit));
            } else {
                int input = (int)ed.getInputMask(); // bit mask
                if ((input & (1 << argBit)) != 0) {
                    return i18n.getString("EventData.bitTrue" ,"On" );
                } else {
                    return i18n.getString("EventData.bitFalse","Off");
                }
            }
        }

        /* digital output values */
        if (EventData._keyMatch(key,EventData.KEY_OUTPUT_MASK)) {
            if (getTitle) {
                return i18n.getString("EventData.key.outputMask", "Output Mask");
            } else {
                int output = (int)ed.getOutputMask(); // bit mask
                String s = StringTools.toBinaryString(output);
                int slen = s.length();
                int blen = StringTools.parseInt(arg,8);
                int len  = (slen >= blen)? (slen - blen) : 0;
                return s.substring(len, slen);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_OUTPUT_BIT)) {
            int argBit = StringTools.parseInt(arg,0);
            if (getTitle) {
                return i18n.getString("EventData.key.outputBit", "Output Bit {0}", String.valueOf(argBit));
            } else {
                int output = (int)ed.getOutputMask(); // bit mask
                if ((output & (1 << argBit)) != 0) {
                    return i18n.getString("EventData.bitTrue" ,"On" );
                } else {
                    return i18n.getString("EventData.bitFalse","Off");
                }
            }
        }

        /* seatbelt values */
        if (EventData._keyMatch(key,EventData.KEY_SEATBELT_MASK)) {
            if (getTitle) {
                return i18n.getString("EventData.key.seatbeltMask", "Seatbelt Mask");
            } else {
                int seatbelt = (int)ed.getSeatbeltMask(); // bit mask
                String s = StringTools.toBinaryString(seatbelt);
                int slen = s.length();
                int blen = StringTools.parseInt(arg,8);
                int len  = (slen >= blen)? (slen - blen) : 0;
                return s.substring(len, slen);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_SEATBELT_BIT)) {
            int argBit = StringTools.parseInt(arg,0);
            if (getTitle) {
                if (argBit == 0) {
                    return i18n.getString("EventData.key.seatbeltDrver", "Seatbelt Driver");
                } else {
                    return i18n.getString("EventData.key.seatbeltBit", "Seatbelt #{0}", String.valueOf(argBit));
                }
            } else {
                int seatbelt = (int)ed.getSeatbeltMask(); // bit mask
                if ((argBit >= 0) && ((seatbelt & (1 << argBit)) != 0)) {
                    return i18n.getString("EventData.bitTrue" ,"On" );
                } else {
                    return i18n.getString("EventData.bitFalse","Off");
                }
            }
        }

        /* temperature values (see also KEY_TIRE_TEMP) */
        if (EventData._keyMatch(key,EventData.KEY_TEMPERATURE)) {
            int argNdx = StringTools.parseInt(arg,0);
            if (getTitle) {
                return i18n.getString("EventData.key.temperature", "Temperature {0}", String.valueOf(argNdx));
            } else {
                Account.TemperatureUnits tu = Account.getTemperatureUnits(ed.getAccount());
                //Print.logInfo("Temperature Index: " + argNdx);
                double tempC = ed.getThermoAverage(argNdx);
                if (EventData.isValidTemperature(tempC)) {
                    double temp  = tu.convertFromC(tempC);
                    StringBuffer sb = new StringBuffer();
                    sb.append(StringTools.format(temp,"0.0"));
                    sb.append(" ");
                    sb.append(tu.toString(locale));
                    return sb.toString();
                } else {
                    return i18n.getString("EventData.notAvailable","n/a");
                }
            }
        }
        if (EventData._keyMatch(key,EventData.KEY_TEMP_ALL)) {
            // Also handles:
            //  KEY_TEMP_0
            //  KEY_TEMP_1
            //  KEY_TEMP_2
            //  KEY_TEMP_3
            //  KEY_TEMP_4
            //  KEY_TEMP_5
            //  KEY_TEMP_6
            //  KEY_TEMP_7
            int argNdx = StringTools.parseInt(key.substring(key.length()-1),-1); // last digit character
            if (argNdx < 0) { argNdx = StringTools.parseInt(arg,0); }
            if (getTitle) {
                return i18n.getString("EventData.key.temperature", "Temperature {0}", String.valueOf(argNdx));
            } else {
                Account.TemperatureUnits tu = Account.getTemperatureUnits(ed.getAccount());
                //Print.logInfo("Temperature Index: " + argNdx);
                double tempC = ed.getThermoAverage(argNdx);
                if (EventData.isValidTemperature(tempC)) {
                    double temp  = tu.convertFromC(tempC);
                    StringBuffer sb = new StringBuffer();
                    sb.append(StringTools.format(temp,"0.0"));
                    sb.append(" ");
                    sb.append(tu.toString(locale));
                    return sb.toString();
                } else {
                    return i18n.getString("EventData.notAvailable","n/a");
                }
            }
        }

        /* Misc */
        if (EventData._keyMatch(key,EventData.KEY_SERVICE_NOTES)) {
            if (getTitle) {
                return i18n.getString("EventData.key.serviceNotes", "Service Notes");
            } else {
                Device device = ed.getDevice();
                return (device != null)? device.getMaintNotes() : "";
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_MAPLINK)) {
            if (getTitle) {
                return i18n.getString("EventData.key.mapLink", "Map Link");
            } else {
                Account.LatLonFormat latlonFmt = Account.getLatLonFormat(ed.getAccount());
                double lat = ed.getLatitude();
                double lon = ed.getLongitude();
                String fmt = "5";
                String latStr = GeoPoint.formatLatitude( lat, fmt, locale);
                String lonStr = GeoPoint.formatLongitude(lon, fmt, locale);
                StringBuffer url = new StringBuffer();
                url.append("http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q=");
                url.append(latStr).append(",").append(lonStr);
                if (arg.equalsIgnoreCase("a")   ||
                    arg.equalsIgnoreCase("html")  ) {
                    String dsc = i18n.getString("EventData.key.map", "Map");
                    String a = EventUtil.MAP_ESCAPE_HTML+"<a href='"+url+"' target='_blank'>"+dsc+"</a>";
                    return a;
                } else {
                    // "url"
                    return url.toString();
                }
            }
        }
        /*
        else
        if (EventData._keyMatch(key,EventData.KEY_STOPPED_TIME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.stoppedTime", "Stopped Time");
            } else {
                double speedKPH = ed.getSpeedKPH();
                if (speedKPH > 0.0) {
                    // currently moving, no elapsed stop time
                    return "";
                } else {
                    // currently stopped
                    long motionChangeTime = ed.getMotionChangeTime();
                    if (motionChangeTime <= 0L) {
                        // last motion change not initialized
                        return "";
                    } else {
                        long deltaSec = ed.getTimestamp() - motionChangeTime;
                        if (deltaSec < 0L) {
                            return "";
                        }
                        return StringTools.formatElapsedSeconds(deltaSec,StringTools.ELAPSED_FORMAT_HHMMSS);
                    }
                }
            }
        }
        */

        /* Garmin values */
        if (EventData._keyMatch(key,EventData.KEY_ETA_DATETIME)) {
            if (getTitle) {
                return i18n.getString("EventData.key.etaDateTime", "ETA Date/Time");
            } else {
                long    ts   = ed.getEtaTimestamp();
                Account acct = ed.getAccount();
                TimeZone tmz = ed.getTimeZone();
                return EventData.getTimestampString(ts, acct, tmz, null);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ETA_UNIQUE_ID)) {
            if (getTitle) {
                return i18n.getString("EventData.key.etaUniqueID", "ETA Unique ID");
            } else {
                long id = ed.getEtaUniqueID();
                return String.valueOf(id);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ETA_DISTANCE)) {
            if (getTitle) {
                return i18n.getString("EventData.key.etaDistance", "ETA Distance");
            } else {
                double distKM = ed.getEtaDistanceKM();
                Account account = ed.getAccount();
                if (account != null) {
                    return account.getDistanceString(distKM, true, locale);
                } else {
                    return StringTools.format(distKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_ETA_GEOPOINT)) {
            if (getTitle) {
                return i18n.getString("EventData.key.etaLatLon", "ETA Latitude/Longitude");
            } else {
                Account.LatLonFormat latlonFmt = Account.getLatLonFormat(ed.getAccount());
                double lat = ed.getEtaLatitude();
                double lon = ed.getEtaLongitude();
                String fmt = latlonFmt.isDegMinSec()? GeoPoint.SFORMAT_DMS : latlonFmt.isDegMin()? GeoPoint.SFORMAT_DM : GeoPoint.SFORMAT_DEC_5;
                String latStr = GeoPoint.formatLatitude( lat, fmt, locale);
                String lonStr = GeoPoint.formatLongitude(lon, fmt, locale);
                return latStr + GeoPoint.PointSeparator + lonStr;
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_STOP_ID)) {
            if (getTitle) {
                return i18n.getString("EventData.key.stopID", "Stop ID");
            } else {
                long id = ed.getStopID();
                return String.valueOf(id);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_STOP_STATUS)) {
            if (getTitle) {
                return i18n.getString("EventData.key.stopStatus", "Stop Status");
            } else {
                int status = ed.getStopStatus();
                return String.valueOf(status);
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_STOP_INDEX)) {
            if (getTitle) {
                return i18n.getString("EventData.key.stopIndex", "Stop Index");
            } else {
                int ndx = ed.getStopIndex();
                return String.valueOf(ndx);
            }
        }

        /* EventData fields */
        if (getTitle) {
            DBField dbFld = EventData.getFactory().getField(key);
            if (dbFld != null) {
                return dbFld.getTitle(locale);
            }
        } else {
            String fldName = ed.getFieldName(key); // this gets the field name with proper case
            DBField dbFld = (fldName != null)? ed.getField(fldName) : null;
            if (dbFld != null) {
                Object val = ed.getFieldValue(fldName); // straight from table
                if (val == null) { val = dbFld.getDefaultValue(); }
                Account account = ed.getAccount();
                if (account != null) {
                    val = account.convertFieldUnits(dbFld, val, true/*inclUnits*/, locale);
                    return StringTools.trim(val);
                } else {
                    return dbFld.formatValue(val);
                }
            }
        }
        // EventData field not found

        /* try device */
        if (getTitle) {
            return Device._getKeyFieldString(
                true/*title*/, key, arg,
                locale, null/*BasicPrivateLabel*/, null/*Device*/);
        } else {
            Device device = ed.getDevice();
            if (device != null) {
                return Device._getKeyFieldString(
                    false/*value*/, key, arg,
                    locale, bpl, device);
            }
        }

        /* try account */
        if (getTitle) {
            return Account._getKeyFieldString(
                true/*title*/, key, arg,
                locale, null/*BasicPrivateLabel*/, null/*Account*/);
        } else {
            return Account._getKeyFieldString(
                false/*value*/, key, arg,
                locale, bpl, ed.getAccount());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* singleton instance of DeviceDescriptionComparator */
    private static Comparator<EventData> devDescComparator = null;
    public static Comparator<EventData> getDeviceDescriptionComparator()
    {
        if (devDescComparator == null) {
            devDescComparator = new DeviceDescriptionComparator(); // ascending
        }
        return devDescComparator;
    }
    
    /* Comparator optimized for EventData device description */
    public static class DeviceDescriptionComparator
        implements Comparator<EventData>
    {
        private boolean ascending = true;
        public DeviceDescriptionComparator() {
            this(true);
        }
        public DeviceDescriptionComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(EventData ev1, EventData ev2) {
            // assume we are comparing EventData records
            if (ev1 == ev2) {
                return 0; // exact same object (or both null)
            } else 
            if (ev1 == null) {
                return this.ascending? -1 :  1; // null < non-null
            } else
            if (ev2 == null) {
                return this.ascending?  1 : -1; // non-null > null
            } else {
                String D1 = ev1.getDeviceDescription().toLowerCase(); // ev1.getDeviceID();
                String D2 = ev2.getDeviceDescription().toLowerCase(); // ev2.getDeviceID();
                return this.ascending? D1.compareTo(D2) : D2.compareTo(D1);
            }
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceDescriptionComparator) {
                DeviceDescriptionComparator ddc = (DeviceDescriptionComparator)other;
                return (this.ascending == ddc.ascending);
            }
            return false;
        }
    }

    /* generic field comparator */
    // Note: This comparator has not been tested yet
    public static class FieldComparator
        implements Comparator<EventData>
    {
        private boolean ascending = true;
        private String  fieldName = "";
        public FieldComparator(String fldName) {
            super();
            this.ascending = true;
            this.fieldName = (fldName != null)? fldName : "";
        }
        public int compare(EventData o1, EventData o2) {
            EventData ed1 = o1;
            EventData ed2 = o2;
            if (ed1 == ed2) {
                return 0;
            } else
            if (ed1 == null) {
                return this.ascending? -1 : 1;
            } else
            if (ed2 == null) {
                return this.ascending? 1 : -1;
            }
            Object v1 = ed1.getFieldValue(this.fieldName);
            Object v2 = ed2.getFieldValue(this.fieldName);
            if (v1 == v2) {
                return 0;
            } else
            if (v1 == null) {
                return this.ascending? -1 : 1;
            } else 
            if (v2 == null) {
                return this.ascending? 1 : -1;
            } else 
            if (v1.equals(v2)) {
                return 0;
            } else
            if ((v1 instanceof Number) && (v2 instanceof Number)) {
                double d = ((Number)v2).doubleValue() - ((Number)v1).doubleValue();
                if (d > 0.0) {
                    return this.ascending? 1 : -1;
                } else
                if (d < 0.0) {
                    return this.ascending? -1 : 1;
                } else {
                    return 0;
                }
            } else {
                String s1 = v1.toString();
                String s2 = v2.toString();
                return this.ascending? s1.compareTo(s2) : s2.compareTo(s1);
            }
        }
        public boolean equals(Object other) {
            if (other instanceof FieldComparator) {
                FieldComparator edc = (FieldComparator)other;
                if (this.ascending != edc.ascending) {
                    return false;
                } else
                if (!this.fieldName.equals(edc.fieldName)) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]           = new String[] { "account"          , "a"   };
    private static final String ARG_DEVICE[]            = new String[] { "device"           , "d"   };
    private static final String ARG_EVENTS_PER_SECOND[] = new String[] { "eventsPerSecond"  , "eps" };
    private static final String ARG_QUERY_ALL[]         = new String[] { "queryAll"                 };
    private static final String ARG_ADDR_LEN[]          = new String[] { "addressLen"               };

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + EventData.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -eps=<HOURS>   Display event-per-second over the last specified HOURS");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        String accountID = RTConfig.getString(ARG_ACCOUNT,"");
        String deviceID  = RTConfig.getString(ARG_DEVICE ,"");

        /* events per second */
        if (RTConfig.hasProperty(ARG_EVENTS_PER_SECOND)) {
            long deltaSec = RTConfig.getLong(ARG_EVENTS_PER_SECOND,24L) * DateTime.HourSeconds(1);
            if (deltaSec <= 0L) { deltaSec = DateTime.HourSeconds(24); }
            Print.sysPrintln("");
            try {
                long nowTime = DateTime.getCurrentTimeSec();
                long count = EventData.getRecordCount( // -1 for InnoDB?
                    null, null,
                    nowTime - deltaSec, nowTime);
                if (count >= 0L) {
                    // -- count obtained
                    String fmtEPS   = StringTools.format(((double)count/(double)deltaSec), "0.0000000");
                    String fmtHours = StringTools.format(((double)deltaSec/3600.0), "0.00");
                    Print.sysPrintln("Event Time Range : " + fmtHours + " hours");
                    Print.sysPrintln("Total Events     : " + count);
                    Print.sysPrintln("Events Per Second: " + fmtEPS);
                } else { // InnoDB
                    // -- unable to obtain count
                    Print.sysPrintln("Unable to count events (InnoDB?)");
                }
            } catch (DBException dbe) {
                Print.logException("Retrieving record count for EventData table", dbe);
            }
            Print.sysPrintln("");
            System.exit(0);
        }

        /* address length */
        if (RTConfig.getBoolean(ARG_QUERY_ALL,false)) {
            String addrSizeKey = "db.typeSize.address";
            Print.logInfo(addrSizeKey + " = " + RTConfig.getInt(addrSizeKey,-1) + " [expected]");
            DBField addrFld = EventData.getFactory().getField(FLD_address);
            if (addrFld != null) {
                Print.sysPrintln("EventData address field length = " + addrFld.getStringLength() + " [found]");
            } else {
                Print.sysPrintln("EROR: EventData."+FLD_address + " not found");
            }
            System.exit(0);
        }

        /* EventData query */
        if (RTConfig.getBoolean(ARG_QUERY_ALL,false)) {
            DBSelect<EventData> dsel = EventData._createRangeEventSelector(
                accountID, deviceID, 
                -1L, -1L,
                null,
                false, 
                EventData.LimitType.FIRST, 0L, true,
                null);
            try {
                DBRecordIterator<EventData> dbi = new DBRecordIterator<EventData>(dsel);
                for (int rc = 1; dbi.hasNext(); rc++) {
                    EventData ed = dbi.next();
                    StringBuffer sb = new StringBuffer();
                    sb.append(rc).append(") ");
                    sb.append(ed.getTimestamp()).append("  ");
                    sb.append((new DateTime(ed.getTimestamp())).toString()).append("  ");
                    sb.append(ed.getGeoPoint().toString());
                    Print.sysPrintln(sb.toString());
                }
            } catch (DBException dbe) {
                Print.logException("", dbe);
                System.exit(99);
            }
            System.exit(0);
        }
        
        /* no options specified */
        Print.sysPrintln("No command-line options specified");
        usage();

    }

}
