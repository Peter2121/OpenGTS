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
// This module provides generic support for a vide variety of HTTP-based device 
// communication protocols.   This includes devices that send the NMEA-0183 $GPRMC 
// record in the requres URL.
//
// Here are the configurable properties that may be set in 'webapp.conf' to customize
// for a specific device type:
//    gprmc.logName           - Name used in logging output [default "gprmc"]
//    gprmc.uniquePrefix      - Prefix used on uniqueID when lookup up Device [defaults to 'gprmc.logName']
//    gprmc.defaultAccountID  - Default account id [default "gprmc"]
//    gprmc.minimumSpeedKPH   - Minimum acceptable speed
//    gprmc.dateFormat        - Date format for 'date' parameter (NONE|EPOCH|YMD|DMY|MDY) [default "YMD"]
//    gprmc.response.ok       - Response on successful data [default ""]
//    gprmc.response.error    - Response on error data [default ""]
//    gprmc.parm.mobile       - Mobile-ID parameter key [default "id"]
//    gprmc.parm.account      - Account-ID parameter key [default "acct"]
//    gprmc.parm.device       - Device-ID parameter key [default "dev"]
//    gprmc.parm.auth         - Auth/Password parameter key (not used)
//    gprmc.parm.status       - StatusCode parameter key [default "code"]
//    gprmc.parm.gprmc        - $GPRMC parameter key [default "gprmc"]
//    gprmc.parm.date         - Date parameter key (ignored if 'gprmc' is used) [default "date"]
//    gprmc.parm.time         - Time parameter key (ignored if 'gprmc' is used) [default "time"]
//    gprmc.parm.latitude     - Latitude parameter key (ignored if 'gprmc' is used) [default "lat"]
//    gprmc.parm.longitude    - Longitude parameter key (ignored if 'gprmc' is used) [default "lon"]
//    gprmc.parm.speed"       - Speed(kph) parameter key (ignored if 'gprmc' is used) [default "speed"]
//    gprmc.parm.heading      - Heading(degrees) parameter key (ignored if 'gprmc' is used) [default "head"]
//    gprmc.parm.altitude     - Altitude(meters) parameter key [default "alt"]
//    gprmc.parm.odometer     - Odometer(kilometers) parameter key [default "odom"]
//    gprmc.parm.address      - Reverse-Geocode parameter key [default "addr"]
//    gprmc.parm.driver       - DriverID parameter key [default "drv"]
//    gprmc.parm.message      - Message parameter key [default "msg"]
//    gprmc.parm.mcc          - Mobile Country Code [default 'mcc']
//    gprmc.parm.mnc          - Mobile Network Code [default 'mnc']
//    gprmc.parm.lac          - Location Area Code [default 'lac']
//    gprmc.parm.cid          - Cell Tower ID [default 'cid']
//    gprmc.parm.tav          - Timing Advance [default 'tav']
//    gprmc.parm.rat          - Radio Access Technology [default 'rat']
//    gprmc.parm.rxlev        - Reception Level [default 'rxlev']
//    gprmc.parm.arfcn        - Absolutie Radio-Freq Channel Number [default 'arfcn']
//
// Note: Do not rely on the property defaults always remaining the same as they are
// currently in this module.  This module is still under development and is subject to
// change, which includes the default values.
//
// Default 'webapp.conf' properties:
//      gprmc.logName=gprmc
//      gprmc.uniquePrefix=gprmc_
//      gprmc.defaultAccountID= 
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.estimateOdometer=false
//      gprmc.simulateGeozones=false
//      gprmc.dateFormat=YMD
//      gprmc.response.ok=OK
//      gprmc.response.error=
//      gprmc.parm.mobile=id,imei,un
//      gprmc.parm.account=acct,account,a
//      gprmc.parm.device=dev,device,d
//      gprmc.parm.auth=pass,password,pw
//      gprmc.parm.status=code,sc,pointType
//      gprmc.parm.gprmc=gprmc,rmc,cds,nmea
//      gprmc.parm.date=date,dt
//      gprmc.parm.time=time,tm
//      gprmc.parm.latitude=lat,latitude
//      gprmc.parm.longitude=lon,longitude,lng,long
//      gprmc.parm.speed=speed,kph
//      gprmc.parm.heading=head,heading,direction
//      gprmc.parm.altitude=alt,altitude
//      gprmc.parm.sats=sat,sats,numsats
//      gprmc.parm.odometer=odom,odometer
//      gprmc.parm.address=addr,address
//      gprmc.parm.driver=drv,driver
//      gprmc.parm.message=msg,message
//      gprmc.parm.battlevel=batl,battl,batt,battlevel
//      gprmc.parm.battvolts=batv,battv,battvolts
//      gprmc.parm.batttemp=batc,battc,batttemp
//      gprmc.parm.temperature=temp,tempC,temperature
//      gprmc.parm.email=email,contact
//
// Default sample Data:
//   http://track.example.com:8080/gprmc/Data?
//      acct=myaccount&
//      dev=mydevice&
//      gprmc=$GPRMC,065954,V,3244.2749,N,14209.9369,W,21.6,0.0,211202,11.8,E,S*07
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=gprmc
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.parm.account=acct
//      gprmc.parm.device=dev
//      gprmc.parm.gprmc=gprmc
//
// NetGPS configuration: [http://www.gpsvehiclenavigation.com/GPS/netgps.php]
//   http://track.example.com:8080/gprmc/Data?
//      un=deviceid&
//      cds=$GPRMC,140159.435,V,3244.2749,N,14209.9369,W,,,200807,,*13&
//      pw=anypass
//   'webapp.conf' properties:
//      gprmc.logName=netgps
//      gprmc.defaultAccountID=netgps
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.parm.account=acct
//      gprmc.parm.device=un
//      gprmc.parm.auth=pw
//      gprmc.parm.gprmc=cds
//      gprmc.response.ok=GPSOK
//      gprmc.response.error=GPSERROR:
//
// GC-101 configuration:
//   http://track.example.com:8080/gprmc/Data?
//      imei=471923002250245&
//      rmc=$GPRMC,023000.000,A,3130.0577,N,14271.7421,W,0.53,208.37,210507,,*19,AUTO
//   'webapp.conf' properties:
//      gprmc.logName=gc101
//      gprmc.uniquePrefix=gc101
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=NONE
//      gprmc.parm.mobile=imei
//      gprmc.parm.gprmc=rmc
//
// GPS2OpenGTS configuration:
//   http://track.example.com:8080/gprmc/Data?
//      acct=test&
//      dev=test01&
//      gprmc=$GPRMC,204852,A,3909.0952,N,12107.936,W,0,000.0,191112,,*27
//   'webapp.conf' properties: Account/Device
//      (for account/device specification, configuration will work as-is)
//      gprmc.parm.mobile=
//      gprmc.parm.account=acct
//      gprmc.parm.device=dev
//   'webapp.conf' properties: MobileID (to use "acct" as mobile-id, and ignore "dev")
//      gprmc.parm.mobile=acct
//      gprmc.parm.account=
//      gprmc.parm.device=
//
// Mologogo configuration:
//   http://track.example.com:8080/gprmc/Data?
//      id=dad&
//      lat=39.251811&
//      lon=-142.132341&
//      accuracy=35949&
//      direction=-1&
//      speed=0&
//      speedUncertainty=0&
//      altitude=519&
//      altitudeUncertainty=49390&
//      pointType=GPS
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=mologogo
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=NONE
//      gprmc.parm.account=acct
//      gprmc.parm.device=id
//      gprmc.parm.gprmc=
//      gprmc.parm.latitude=lat
//      gprmc.parm.longitude=lon
//      gprmc.parm.speed=speed
//      gprmc.parm.heading=direction
//      gprmc.parm.status=pointType
//
// GPSGate configuration:
//   http://track.example.com/gprmc/Data?
//      longitude=113.38063&
//      latitude=22.53922&
//      altitude=22.4&
//      speed=0.0&
//      heading=0.0&
//      date=20100526&
//      time=142252.000&
//      username=123123456789&
//      pw=0111111
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=YMD
//      gprmc.parm.mobile=username
//      gprmc.parm.account=
//      gprmc.parm.device=
//      gprmc.parm.gprmc=
//      gprmc.parm.latitude=latitude
//      gprmc.parm.longitude=longitude
//      gprmc.parm.date=date
//      gprmc.parm.time=time
//      gprmc.parm.speed=speed
//      gprmc.parm.heading=heading
//      gprmc.parm.altitude=altitude
//      gprmc.parm.status=code
//
// "Locator", by Viking Informatics Ltd. (for Nokia E71 phones)
// http://store.handango.com/ampp/store/PlatformProductDetail.jsp?siteId=1521&osId=1989&jid=66298B43EF4E9X5X4F3893E45AEX1B1X&platformId=4&productType=2&productId=131062&sectionId=6166&catalog=20&topSectionId=1009
//   http://track.example.com/gprmc/Data?
//      imei=351112222233333&
//      cell=12345&
//      mcc=216&
//      mnc=1&
//      lac=120&
//      lat=4710.1058N&
//      long=01945.1212E
//
// Another example configuration:
//   http://track.example.com/gprmc/Data?
//      acct=myacct&
//      dev=mydev&
//      lon=32.1234&
//      lat=-142.1234&
//      date=20070819&
//      time=225446&
//      speed=45.4&
//      code=1
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=undefined
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=YMD
//      gprmc.parm.account=acct
//      gprmc.parm.device=dev
//      gprmc.parm.gprmc=
//      gprmc.parm.latitude=lat
//      gprmc.parm.longitude=lon
//      gprmc.parm.date=date
//      gprmc.parm.time=time
//      gprmc.parm.speed=speed
//      gprmc.parm.heading=heading
//      gprmc.parm.status=code
//
// ----------------------------------------------------------------------------
// Change History:
//  2007/08/09  Martin D. Flynn
//     -Initial release. 
//     -Note: this module is new for this release and has not yet been fully tested.
//  2007/09/16  Martin D. Flynn
//     -Additional optional parameters to allow for more flexibility in defining data
//      format types.  This module should now be able to be configured for a wide variety
//      of HTTP base communication protocols from various types of remote devices.
//     -Note: this module has still not yet been fully tested.
//  2007/11/28  Martin D. Flynn
//     -Added 'gprmc.uniquePrefix' property
//  2008/02/10  Martin D. Flynn
//     -Added additional logging messages when lat/lon is invalid
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/08/15  Martin D. Flynn
//     -Make sure 'isValidGPS' is set for non-GPRMC parsed records.
//  2010/04/11  Martin D. Flynn
//     -Various changes
//  2011/03/08  Martin D. Flynn
//     -Added support for NMEA-0183 lat/lon formats "4210.1234N"/"14234.1234W"
//     -Added support for text "headingDeg" values: N, NE, E, SE, S, SW, W, NW
//     -Added simulated geozone arrive/depart event generation.
//  2011/03/14  Martin D. Flynn
//     -Fixed YMD date parsing issue
//  2011/05/13  Martin D. Flynn
//     -Added additional displayed header information
//     -Fixed Geozone transition check (geozone check needs to come before other
//      device 'insertEventData' calls).
//  2011/06/16  Martin D. Flynn
//     -Changed display of incoming URL from "logDebug" to "logInfo".
//  2012/04/03  Martin D. Flynn
//     -Updated to support check multiple argument keys for the same data type.
//     -The default AccountID (DefaultAccountID) was changed to blank.  This can
//      be overridden in the "webapp.conf" file with the "gprc.defaultAccountID"
//      property.
//     -Added "gprmc.parm.mobile" config property (if specified, this will take 
//      precidence over "gprmc.parm.unique").
//  2012/08/01  Martin D. Flynn
//     -Fixed NPE when no date/time is specified in the URL.
//  2012/12/24  Martin D. Flynn
//     -If 'mobileID' and 'accountID' are blank, use 'deviceID' as 'mobileID'.
//     -Support multiple unique-id prefixes.
//  2013/08/06  Martin D. Flynn / STefan Mayer
//     -Support for battery level/volts and #satellites (thanks to STefan Mayer)
//  2013/09/20  Martin D. Flynn / STefan Mayer
//     -Fixed issue with "dateStr" or "timeStr" blank (changed to 'and')
//  2014/01/01  Martin D. Flynn / STefan Mayer
//     -Added CellTower support (MCC,MNC,LAC,CID,TAV,etc.)
//  2014/03/03  Martin D. Flynn
//     -Fixed packets sent via POST
//  2014/05/05  Martin D. Flynn
//     -Added support for a temperature value
//     -Added support for battery-temperature (version OpenGTS_2.5.5+ only)
//     -Battery-level adjusted to be saved as a % (range 0 to 1).
//  2014/06/29  Martin D. Flynn
//     -Fix NPE caused by check for decimal point in "_battLevel"
//  2014/10/08  Martin D. Flynn
//     -Added support for saving "&version=XXXX" value into Device "codeVersion".
//     -Added support for "&distance=" option to record trip distance (km).
//     -Initial support for SourceForge "GpsTracker" app project.
// ----------------------------------------------------------------------------
package org.opengts.war.gprmc;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

//import javax.mail.Session;
//import javax.mail.Message;
//import javax.mail.internet.MimeMessage;
//import javax.mail.internet.InternetAddress;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.cellid.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public class Data 
    extends CommonServlet
{

    // ------------------------------------------------------------------------

    /* DCS version */
    public static final String VERSION                      = "2.2.7"; // 2.5.6-B18

    /* name used to tag logged messaged */
    public static       String LOG_NAME                     = "gprmc";

    /* default UniqueID prefix */
    public static final String DEFAULT_UNIQUEID_PREFIX[]    = { "gprmc_", "ct_", "uid_", "imei_", "*" };

    // ------------------------------------------------------------------------

    /* unique id prefix */
    public  static String   UniqueIDPrefix[]                = DEFAULT_UNIQUEID_PREFIX;

    /* default account id */
    // The URL variable "?dev=<id>" is used to allow this module the ability to uniquely
    // identify a reporting phone.  The reported "<id>" provided by the phone is used
    // as the "DeviceID" for the following AccountID.  This value can be overridden in
    // the Servlet 'webapp.conf' file.
    public  static String   DefaultAccountID                = "";

    /* minimum required speed */
    // GPS receivers have a tendency to appear 'moving', even when sitting stationary
    // on your desk.  This filter is a 'speed' threshold which is used to force the 
    // reported speed to '0' if it falls below this value.  This value can be overridden
    // in the Servlet 'webapp.conf' file.
    public  static double   MinimumReqSpeedKPH              = 4.0;

    /* estimate GPS-based odometer */
    public  static boolean  ESTIMATE_ODOMETER               = false;

    /* simulate geozones */
    public  static boolean  SIMEVENT_GEOZONES               = false;

    /* Default time zone */
    private static TimeZone gmtTimeZone                     = DateTime.getGMTTimeZone();
   
    /* Maximum timestamp */
    private static long     MaxTimestamp                    = DateTime.getMaxDate().getTimeSec();

    /* Status code map */
    private static Map<String,Integer> StatusCodeMap        = null;
    
    /* Compass headings */
    private static String   COMPASS_HEADING[]               = new String[] { 
        "N", "NE", "E", "SE", "S", "SW", "W", "NW" 
    };
    private static double   COMPASS_INCREMENT               = 45.0;
    
    /* Invalid Lat/Lon */
    private static double   INVALID_LATLON                  = -999.0;
    
    /* Invalid temperature */
    private static double   BAD_TEMP                        = EventData.INVALID_TEMPERATURE;

    // ------------------------------------------------------------------------

    /* date format constants */
    public static final int DATE_FORMAT_NONE                = 0; // Current time will be used
    public static final int DATE_FORMAT_EPOCH               = 1; // <epochTime>
    public static final int DATE_FORMAT_YMD                 = 2; // "YYYYMMDD" or "YYMMDD"
    public static final int DATE_FORMAT_MDY                 = 3; // "MMDDYYYY" or "MMDDYY"
    public static final int DATE_FORMAT_DMY                 = 4; // "DDMMYYYY" or "DDMMYY"
    public static final int DATE_FORMAT_YMDhms              = 5; // "YYYYMMDDhhmmss" or EPOCH

    /* date format */
    // The date format must be specified here
    public static int     DateFormat                        = DATE_FORMAT_YMD; // DATE_FORMAT_YMDhms;

    public static String GetDateFormatString()
    {
        switch (DateFormat) {
            case DATE_FORMAT_NONE  :  return "NONE";
            case DATE_FORMAT_EPOCH :  return "EPOCH";
            case DATE_FORMAT_YMD   :  return "YMD";
            case DATE_FORMAT_MDY   :  return "MDY";
            case DATE_FORMAT_DMY   :  return "DMY";
            case DATE_FORMAT_YMDhms:  return "YMDhms";
            default                :  return "???";
        }
    }
    
    // ------------------------------------------------------------------------

    // -- common parameter keys (lookups are case insensitive) */
    private static String PARM_COMMAND[]                    = { "co"   , "cmd", "command"            };  // Command
    private static String PARM_VERSION[]                    = { "ve"   , "ver", "version"            };  // Version
    private static String PARM_MOBILE[]                     = { "id"   , "imei", "un", "mobileid", "appid" };  // MobileID
    private static String PARM_PHONE[]                      = { "phone", "phonenumber"               };  // PhoneNumber
    private static String PARM_ACCOUNT[]                    = { "acct" , "ac","account","a","accountid" };  // AccountID
    private static String PARM_DEVICE[]                     = { "dev"  , "device", "d", "deviceid"   };  // DeviceID
    private static String PARM_PASSWORD[]                   = { "pass" , "password", "pw"            };  // authorization/password
    private static String PARM_AUTH[]                       = { "auth" , "pin"                       };  // authorization/password
    private static String PARM_STATUS[]                     = { "code" , "sc", "pointType"           };  // event/status code
    private static String PARM_ALTITUDE[]                   = { "alt"  , "altitude"                  };  // altitude (meters)
    private static String PARM_ODOMETER_KM[]                = { "odom" , "odometer", "odo"           };  // odometer (kilometers)
    private static String PARM_ODOMETER_MI[]                = { "mile" , "miles", "odomi"            };  // odometer (miles)
    private static String PARM_DISTANCE_KM[]                = { "dist" , "distance", "trip"          };  // distance (kilometers)
    private static String PARM_DISTANCE_MI[]                = { "tripm", "distmiles", "tripmi"       };  // distance (kilometers)
    private static String PARM_ADDRESS[]                    = { "addr" , "address"                   };  // reverse-geocoded address
    private static String PARM_DRIVER[]                     = { "drv"  , "driver"                    };  // driver
    private static String PARM_MESSAGE[]                    = { "msg"  , "message"                   };  // message
    private static String PARM_EMAIL[]                      = { "email", "contact"                   };  // email
    private static String PARM_HDOP[]                       = { "hdop"                               };  // horizontal dilution of precision
    private static String PARM_HORZ_ACC[]                   = { "hacc" , "accuracy", "acc"           };  // horizontal accuracy (meters)
    private static String PARM_VERT_ACC[]                   = { "vacc"                               };  // vertical accuracy (meters)
    private static String PARM_NUM_SATS[]                   = { "sat"  , "sats", "numsats"           };  // number of satellite
    private static String PARM_BATT_LEVEL[]                 = { "batl" , "battl", "battlevel", "batt"};  // battery level (%)
    private static String PARM_BATT_VOLTS[]                 = { "batv" , "battv", "battvolts"        };  // battery voltage
    private static String PARM_BATT_TEMPC[]                 = { "batc" , "battc", "batttemp"         };  // Battery tempC
    private static String PARM_TEMPERATURE[]                = { "temp" , "tempc", "temperature"      };  // Temperature

    // -- cell-tower data
    private static String PARM_CELLID_MCC[]                 = { "mcc"                                };  // CellID: mcc
    private static String PARM_CELLID_MNC[]                 = { "mnc"                                };  // CellID: mnc
    private static String PARM_CELLID_LAC[]                 = { "lac"                                };  // CellID: lac
    private static String PARM_CELLID_CID[]                 = { "cid"                                };  // CellID: cid
    private static String PARM_CELLID_TAV[]                 = { "tav"                                };  // CellID: tav
    private static String PARM_CELLID_RAT[]                 = { "rat"                                };  // CellID: rat
    private static String PARM_CELLID_RXLEV[]               = { "rxlev", "rxlvl", "rxl"              };  // CellID: rxlev
    private static String PARM_CELLID_ARFCN[]               = { "arfcn", "arf"                       };  // CellID: arfcn

    // -- $GPRMC field key
    private static String PARM_GPRMC[]                      = { "gprmc", "rmc", "cds", "nmea"        };  // $GPRMC data

    // -- these are ignored if PARM_GPRMC is defined
    private static String PARM_DATE[]                       = { "date" , "datetime", "dt"            };  // date (YYYYMMDD)
    private static String PARM_TIME[]                       = { "time" , "ts", "tm"                  };  // time (HHMMSS)
    private static String PARM_LATITUDE[]                   = { "lat"  , "latitude"                  };  // latitude
    private static String PARM_LONGITUDE[]                  = { "lon"  , "longitude","lng","long"    };  // longitude
    private static String PARM_SPEED_KPH[]                  = { "speed", "kph"                       };  // speed (km/h)
    private static String PARM_SPEED_MPH[]                  = { "mph"  , "speedmph"                  };  // speed (mph)
    private static String PARM_HEADING[]                    = { "head" , "heading","direction","dir" };  // heading (degrees)

    /* returned response */
    private static String RESPONSE_OK                       = "OK";
    private static String RESPONSE_ERROR                    = "ERROR";
    private static String RESPONSE_NOT_AUTH                 = RESPONSE_ERROR;

    // ------------------------------------------------------------------------

    /* configuration name (TODO: update based on specific servlet configuration) */
    public static final String  DEVICE_CODE                 = "gprmc";

    /* runtime config */
    public static final String  CONFIG_LOG_NAME             = DEVICE_CODE + ".logName";
    public static final String  CONFIG_UNIQUE_PREFIX        = DEVICE_CODE + ".uniquePrefix";
    public static final String  CONFIG_DFT_ACCOUNT          = DEVICE_CODE + ".defaultAccountID";
    public static final String  CONFIG_MIN_SPEED            = DEVICE_CODE + ".minimumSpeedKPH";
    public static final String  CONFIG_ESTIMATE_ODOMETER    = DEVICE_CODE + ".estimateOdometer";
    public static final String  CONFIG_SIMEVENT_GEOZONES    = DEVICE_CODE + ".simulateGeozones";
    public static final String  CONFIG_DATE_FORMAT          = DEVICE_CODE + ".dateFormat";       // "YMD", "DMY", "MDY"
    public static final String  CONFIG_RESPONSE_OK          = DEVICE_CODE + ".response.ok";
    public static final String  CONFIG_RESPONSE_ERROR       = DEVICE_CODE + ".response.error";
    public static final String  CONFIG_RESPONSE_NOT_AUTH    = DEVICE_CODE + ".response.notAuth";

    public static final String  CONFIG_PARM_MOBILE          = DEVICE_CODE + ".parm.mobile";
    public static final String  CONFIG_PARM_PHONE           = DEVICE_CODE + ".parm.phone";
    public static final String  CONFIG_PARM_UNIQUE          = DEVICE_CODE + ".parm.unique";
    public static final String  CONFIG_PARM_ACCOUNT         = DEVICE_CODE + ".parm.account";
    public static final String  CONFIG_PARM_DEVICE          = DEVICE_CODE + ".parm.device";
    public static final String  CONFIG_PARM_PASSWORD        = DEVICE_CODE + ".parm.password";
    public static final String  CONFIG_PARM_AUTH            = DEVICE_CODE + ".parm.auth";
    public static final String  CONFIG_PARM_STATUS          = DEVICE_CODE + ".parm.status";
    
    public static final String  CONFIG_PARM_GPRMC           = DEVICE_CODE + ".parm.gprmc";       // $GPRMC

    public static final String  CONFIG_PARM_DATE            = DEVICE_CODE + ".parm.date";        // epoch, YYYYMMDD, DDMMYYYY, MMDDYYYY
    public static final String  CONFIG_PARM_TIME            = DEVICE_CODE + ".parm.time";        // HHMMSS
    public static final String  CONFIG_PARM_LATITUDE        = DEVICE_CODE + ".parm.latitude";
    public static final String  CONFIG_PARM_LONGITUDE       = DEVICE_CODE + ".parm.longitude";
    public static final String  CONFIG_PARM_SPEED_KPH       = DEVICE_CODE + ".parm.speed";       // km/h
    public static final String  CONFIG_PARM_SPEED_MPH       = DEVICE_CODE + ".parm.speedmph";    // mph
    public static final String  CONFIG_PARM_HEADING         = DEVICE_CODE + ".parm.heading";     // degrees
    public static final String  CONFIG_PARM_ALTITUDE        = DEVICE_CODE + ".parm.altitude";    // meters
    public static final String  CONFIG_PARM_ODOMETER_KM     = DEVICE_CODE + ".parm.odometer";    // kilometers
    public static final String  CONFIG_PARM_ODOMETER_MI     = DEVICE_CODE + ".parm.odometermiles";// miles
    public static final String  CONFIG_PARM_DISTANCE_KM     = DEVICE_CODE + ".parm.distance";    // kilometers
    public static final String  CONFIG_PARM_DISTANCE_MI     = DEVICE_CODE + ".parm.distancemiles";// kilometers
    public static final String  CONFIG_PARM_ADDRESS         = DEVICE_CODE + ".parm.address";     // reverse-geocode
    public static final String  CONFIG_PARM_DRIVER          = DEVICE_CODE + ".parm.driver";      // driverId
    public static final String  CONFIG_PARM_MESSAGE         = DEVICE_CODE + ".parm.message";     // message
    public static final String  CONFIG_PARM_EMAIL           = DEVICE_CODE + ".parm.email";       // email address
    public static final String  CONFIG_PARM_HDOP            = DEVICE_CODE + ".parm.hdop";        // HDOP
    public static final String  CONFIG_PARM_HORZ_ACC        = DEVICE_CODE + ".parm.hacc";        // horizontal accuracy
    public static final String  CONFIG_PARM_VERT_ACC        = DEVICE_CODE + ".parm.vacc";        // vertical accuracy
    public static final String  CONFIG_PARM_NUM_SATS        = DEVICE_CODE + ".parm.sats";        // number satellites
    public static final String  CONFIG_PARM_BATT_LEVEL      = DEVICE_CODE + ".parm.battlevel";   // battery level
    public static final String  CONFIG_PARM_BATT_VOLTS      = DEVICE_CODE + ".parm.battvolts";   // battery voltage
    public static final String  CONFIG_PARM_BATT_TEMPC      = DEVICE_CODE + ".parm.batttemp";    // battery temp-C
    public static final String  CONFIG_PARM_TEMPERATURE     = DEVICE_CODE + ".parm.temperature"; // temperature
    public static final String  CONFIG_PARM_CELLID_MCC      = DEVICE_CODE + ".parm.mcc";         // CellID: mcc
    public static final String  CONFIG_PARM_CELLID_MNC      = DEVICE_CODE + ".parm.mnc";         // CellID: mnc
    public static final String  CONFIG_PARM_CELLID_LAC      = DEVICE_CODE + ".parm.lac";         // CellID: lac
    public static final String  CONFIG_PARM_CELLID_CID      = DEVICE_CODE + ".parm.cid";         // CellID: cid
    public static final String  CONFIG_PARM_CELLID_TAV      = DEVICE_CODE + ".parm.tav";         // CellID: tav
    public static final String  CONFIG_PARM_CELLID_RAT      = DEVICE_CODE + ".parm.rat";         // CellID: rat
    public static final String  CONFIG_PARM_CELLID_RXLEV    = DEVICE_CODE + ".parm.rxlev";       // CellID: rxlev
    public static final String  CONFIG_PARM_CELLID_ARFCN    = DEVICE_CODE + ".parm.arfcn";       // CellID: arfcn

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static void printHeaderLine(String desc, String opt[], String conf)
    {
        String val = StringTools.join(opt,",");
        Data.printHeaderLine(desc, val, conf);
    }

    private static void printHeaderLine(String desc, String val, String conf)
    {
        int dLen = 23;
        int vLen = 23;
        String d = StringTools.trim(desc);
        String v = StringTools.trim(val);
        String c = StringTools.trim(conf);
        StringBuffer sb = new StringBuffer();
        sb.append(StringTools.leftAlign(d,dLen)).append(": ");
        if (StringTools.isBlank(c)) {
            sb.append(v);
        } else {
            String va = (!StringTools.isBlank(v) && !v.equals("&="))? v : "---";
            sb.append(StringTools.leftAlign(va,vLen));
            sb.append(" [").append(c).append("]");
        }
        Data.logInfo(sb.toString());
    }

    /* static initializer */
    // Only initialized once (per JVM)
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

        /* log name */
        LOG_NAME = RTConfig.getString(CONFIG_LOG_NAME, LOG_NAME);

        /* unique-id prefix */
        UniqueIDPrefix = RTConfig.getStringArray(CONFIG_UNIQUE_PREFIX, null);
        if (ListTools.isEmpty(UniqueIDPrefix)) {
            UniqueIDPrefix = DEFAULT_UNIQUEID_PREFIX;
            String logName_ = LOG_NAME + "_";
            if (!ListTools.contains(UniqueIDPrefix, logName_)) {
                UniqueIDPrefix = ListTools.insert(UniqueIDPrefix, logName_, 0);
            }
        }
        for (int i = 0; i < UniqueIDPrefix.length; i++) {
            if (StringTools.isBlank(UniqueIDPrefix[i]) || UniqueIDPrefix[i].equals("*")) {
                UniqueIDPrefix[i] = ""; // no prefix
            } else
            if (!UniqueIDPrefix[i].endsWith("_") && !UniqueIDPrefix[i].endsWith("-")) {
                UniqueIDPrefix[i] += "_";
            }
        }

        /* set configuration */
        DefaultAccountID   = RTConfig.getString( CONFIG_DFT_ACCOUNT      , DefaultAccountID);
        MinimumReqSpeedKPH = RTConfig.getDouble( CONFIG_MIN_SPEED        , MinimumReqSpeedKPH);
        ESTIMATE_ODOMETER  = RTConfig.getBoolean(CONFIG_ESTIMATE_ODOMETER, ESTIMATE_ODOMETER);
        SIMEVENT_GEOZONES  = RTConfig.getBoolean(CONFIG_SIMEVENT_GEOZONES, SIMEVENT_GEOZONES);
        String dateFmt     = RTConfig.getString( CONFIG_DATE_FORMAT      , "YMD");
        if (dateFmt.equalsIgnoreCase("NONE")) {
            DateFormat = DATE_FORMAT_NONE;
        } else
        if (dateFmt.equalsIgnoreCase("EPOCH")) {
            DateFormat = DATE_FORMAT_EPOCH;
        } else
        if (dateFmt.equalsIgnoreCase("YMD")) {
            DateFormat = DATE_FORMAT_YMD;
        } else
        if (dateFmt.equalsIgnoreCase("MDY")) {
            DateFormat = DATE_FORMAT_MDY;
        } else
        if (dateFmt.equalsIgnoreCase("DMY")) {
            DateFormat = DATE_FORMAT_DMY;
        } else
        if (dateFmt.equalsIgnoreCase("YMDhms")) {
            DateFormat = DATE_FORMAT_YMDhms;
        } else {
            DateFormat = DATE_FORMAT_YMD;
            Data.logError(null, "Invalid date format: " + dateFmt);
        }

        /* parameters */
        PARM_MOBILE        = RTConfig.getStringArray(CONFIG_PARM_UNIQUE      , PARM_MOBILE      );
        PARM_MOBILE        = RTConfig.getStringArray(CONFIG_PARM_MOBILE      , PARM_MOBILE      );
        PARM_PHONE         = RTConfig.getStringArray(CONFIG_PARM_PHONE       , PARM_PHONE       );
        PARM_ACCOUNT       = RTConfig.getStringArray(CONFIG_PARM_ACCOUNT     , PARM_ACCOUNT     );
        PARM_DEVICE        = RTConfig.getStringArray(CONFIG_PARM_DEVICE      , PARM_DEVICE      );
        PARM_PASSWORD      = RTConfig.getStringArray(CONFIG_PARM_PASSWORD    , PARM_PASSWORD    );
        PARM_AUTH          = RTConfig.getStringArray(CONFIG_PARM_AUTH        , PARM_AUTH        );
        PARM_STATUS        = RTConfig.getStringArray(CONFIG_PARM_STATUS      , PARM_STATUS      );
        PARM_GPRMC         = RTConfig.getStringArray(CONFIG_PARM_GPRMC       , PARM_GPRMC       );
        PARM_DATE          = RTConfig.getStringArray(CONFIG_PARM_DATE        , PARM_DATE        );
        PARM_TIME          = RTConfig.getStringArray(CONFIG_PARM_TIME        , PARM_TIME        );
        PARM_LATITUDE      = RTConfig.getStringArray(CONFIG_PARM_LATITUDE    , PARM_LATITUDE    );
        PARM_LONGITUDE     = RTConfig.getStringArray(CONFIG_PARM_LONGITUDE   , PARM_LONGITUDE   );
        PARM_SPEED_KPH     = RTConfig.getStringArray(CONFIG_PARM_SPEED_KPH   , PARM_SPEED_KPH   );
        PARM_SPEED_MPH     = RTConfig.getStringArray(CONFIG_PARM_SPEED_MPH   , PARM_SPEED_MPH   );
        PARM_HEADING       = RTConfig.getStringArray(CONFIG_PARM_HEADING     , PARM_HEADING     );
        PARM_ALTITUDE      = RTConfig.getStringArray(CONFIG_PARM_ALTITUDE    , PARM_ALTITUDE    );
        PARM_ODOMETER_KM   = RTConfig.getStringArray(CONFIG_PARM_ODOMETER_KM , PARM_ODOMETER_KM );
        PARM_ODOMETER_MI   = RTConfig.getStringArray(CONFIG_PARM_ODOMETER_MI , PARM_ODOMETER_MI );
        PARM_DISTANCE_KM   = RTConfig.getStringArray(CONFIG_PARM_DISTANCE_KM , PARM_DISTANCE_KM );
        PARM_DISTANCE_MI   = RTConfig.getStringArray(CONFIG_PARM_DISTANCE_MI , PARM_DISTANCE_MI );
        PARM_ADDRESS       = RTConfig.getStringArray(CONFIG_PARM_ADDRESS     , PARM_ADDRESS     );
        PARM_DRIVER        = RTConfig.getStringArray(CONFIG_PARM_DRIVER      , PARM_DRIVER      );
        PARM_MESSAGE       = RTConfig.getStringArray(CONFIG_PARM_MESSAGE     , PARM_MESSAGE     );
        PARM_EMAIL         = RTConfig.getStringArray(CONFIG_PARM_EMAIL       , PARM_EMAIL       );
        PARM_HDOP          = RTConfig.getStringArray(CONFIG_PARM_HDOP        , PARM_HDOP        );
        PARM_HORZ_ACC      = RTConfig.getStringArray(CONFIG_PARM_HORZ_ACC    , PARM_HORZ_ACC    );
        PARM_VERT_ACC      = RTConfig.getStringArray(CONFIG_PARM_VERT_ACC    , PARM_VERT_ACC    );
        PARM_NUM_SATS      = RTConfig.getStringArray(CONFIG_PARM_NUM_SATS    , PARM_NUM_SATS    );
        PARM_BATT_LEVEL    = RTConfig.getStringArray(CONFIG_PARM_BATT_LEVEL  , PARM_BATT_LEVEL  );
        PARM_BATT_VOLTS    = RTConfig.getStringArray(CONFIG_PARM_BATT_VOLTS  , PARM_BATT_VOLTS  );
        PARM_BATT_TEMPC    = RTConfig.getStringArray(CONFIG_PARM_BATT_TEMPC  , PARM_BATT_TEMPC  );
        PARM_TEMPERATURE   = RTConfig.getStringArray(CONFIG_PARM_TEMPERATURE , PARM_TEMPERATURE );
        PARM_CELLID_MNC    = RTConfig.getStringArray(CONFIG_PARM_CELLID_MNC  , PARM_CELLID_MNC  );
        PARM_CELLID_MCC    = RTConfig.getStringArray(CONFIG_PARM_CELLID_MCC  , PARM_CELLID_MCC  );
        PARM_CELLID_LAC    = RTConfig.getStringArray(CONFIG_PARM_CELLID_LAC  , PARM_CELLID_LAC  );
        PARM_CELLID_CID    = RTConfig.getStringArray(CONFIG_PARM_CELLID_CID  , PARM_CELLID_CID  );
        PARM_CELLID_TAV    = RTConfig.getStringArray(CONFIG_PARM_CELLID_TAV  , PARM_CELLID_TAV  );
        PARM_CELLID_RAT    = RTConfig.getStringArray(CONFIG_PARM_CELLID_RAT  , PARM_CELLID_RAT  );
        PARM_CELLID_RXLEV  = RTConfig.getStringArray(CONFIG_PARM_CELLID_RXLEV, PARM_CELLID_RXLEV);
        PARM_CELLID_ARFCN  = RTConfig.getStringArray(CONFIG_PARM_CELLID_ARFCN, PARM_CELLID_ARFCN);

        /* return errors */
        RESPONSE_OK        = RTConfig.getString(     CONFIG_RESPONSE_OK      , RESPONSE_OK);
        RESPONSE_ERROR     = RTConfig.getString(     CONFIG_RESPONSE_ERROR   , RESPONSE_ERROR);
        RESPONSE_NOT_AUTH  = RTConfig.getString(     CONFIG_RESPONSE_NOT_AUTH, RESPONSE_ERROR);

        /* header */
        Data.logInfo("-----------------------------------------------------------------------");
        Data.printHeaderLine("Version"                , VERSION               , null);
        Data.printHeaderLine("Default AccountID"      , DefaultAccountID      , CONFIG_DFT_ACCOUNT);
        Data.printHeaderLine("Minimum speed (km/h)"   , MinimumReqSpeedKPH+"" , CONFIG_MIN_SPEED);
        Data.printHeaderLine("Simulate Geozones"      , SIMEVENT_GEOZONES+""  , CONFIG_SIMEVENT_GEOZONES);
        Data.printHeaderLine("Estimate Odometer"      , ESTIMATE_ODOMETER+""  , CONFIG_ESTIMATE_ODOMETER);
        Data.printHeaderLine("Date Format"            , GetDateFormatString() , CONFIG_DATE_FORMAT);
        Data.printHeaderLine("UniqueID prefix"        , UniqueIDPrefix        , CONFIG_UNIQUE_PREFIX);
        Data.printHeaderLine("MobileID parameter"     , PARM_MOBILE           , CONFIG_PARM_MOBILE);
        Data.printHeaderLine("PhoneID parameter"      , PARM_PHONE            , CONFIG_PARM_PHONE);
        Data.printHeaderLine("Account parameter"      , PARM_ACCOUNT          , CONFIG_PARM_ACCOUNT);
        Data.printHeaderLine("Device parameter"       , PARM_DEVICE           , CONFIG_PARM_DEVICE);
        Data.printHeaderLine("Status parameter"       , PARM_STATUS           , CONFIG_PARM_STATUS);
        Data.printHeaderLine("$GPRMC parameter"       , PARM_GPRMC            , CONFIG_PARM_GPRMC);
        Data.printHeaderLine("Date parameter"         , PARM_DATE             , CONFIG_PARM_DATE);
        Data.printHeaderLine("Time parameter"         , PARM_TIME             , CONFIG_PARM_TIME);
        Data.printHeaderLine("Latitude parameter"     , PARM_LATITUDE         , CONFIG_PARM_LATITUDE);
        Data.printHeaderLine("Longitude parameter"    , PARM_LONGITUDE        , CONFIG_PARM_LONGITUDE);
        Data.printHeaderLine("SpeedKPH parameter"     , PARM_SPEED_KPH        , CONFIG_PARM_SPEED_KPH);
        Data.printHeaderLine("SpeedMPH parameter"     , PARM_SPEED_MPH        , CONFIG_PARM_SPEED_MPH);
        Data.printHeaderLine("Heading parameter"      , PARM_HEADING          , CONFIG_PARM_HEADING);
        Data.printHeaderLine("Altitude parameter"     , PARM_ALTITUDE         , CONFIG_PARM_ALTITUDE);
        Data.printHeaderLine("OdometerKM parameter"   , PARM_ODOMETER_KM      , CONFIG_PARM_ODOMETER_KM);
        Data.printHeaderLine("OdometerMiles parameter", PARM_ODOMETER_MI      , CONFIG_PARM_ODOMETER_MI);
        Data.printHeaderLine("DistanceKM parameter"   , PARM_DISTANCE_KM      , CONFIG_PARM_DISTANCE_KM);
        Data.printHeaderLine("DistanceMiles parameter", PARM_DISTANCE_MI      , CONFIG_PARM_DISTANCE_MI);
        Data.printHeaderLine("Address parameter"      , PARM_ADDRESS          , CONFIG_PARM_ADDRESS);
        Data.printHeaderLine("Driver parameter"       , PARM_DRIVER           , CONFIG_PARM_DRIVER);
        Data.printHeaderLine("Message parameter"      , PARM_MESSAGE          , CONFIG_PARM_MESSAGE);
        Data.printHeaderLine("Battery Level parameter", PARM_BATT_LEVEL       , CONFIG_PARM_BATT_LEVEL);
        Data.printHeaderLine("Battery Volts parameter", PARM_BATT_VOLTS       , CONFIG_PARM_BATT_VOLTS);
        Data.printHeaderLine("Battery TempC parameter", PARM_BATT_TEMPC       , CONFIG_PARM_BATT_TEMPC);
        Data.printHeaderLine("Temperature parameter"  , PARM_TEMPERATURE      , CONFIG_PARM_TEMPERATURE);
        Data.printHeaderLine("CellID: MCC parameter"  , PARM_CELLID_MCC       , CONFIG_PARM_CELLID_MCC);
        Data.printHeaderLine("CellID: MNC parameter"  , PARM_CELLID_MNC       , CONFIG_PARM_CELLID_MNC);
        Data.printHeaderLine("CellID: LAC parameter"  , PARM_CELLID_LAC       , CONFIG_PARM_CELLID_LAC);
        Data.printHeaderLine("CellID: CID parameter"  , PARM_CELLID_CID       , CONFIG_PARM_CELLID_CID);
        Data.printHeaderLine("CellID: TAV parameter"  , PARM_CELLID_TAV       , CONFIG_PARM_CELLID_TAV);
        Data.printHeaderLine("CellID: RAT parameter"  , PARM_CELLID_RAT       , CONFIG_PARM_CELLID_RAT);
        Data.printHeaderLine("CellID: RXLEV parameter", PARM_CELLID_RXLEV     , CONFIG_PARM_CELLID_RXLEV);
        Data.printHeaderLine("CellID: ARFCN parameter", PARM_CELLID_ARFCN     , CONFIG_PARM_CELLID_ARFCN);
        Data.logInfo("-----------------------------------------------------------------------");

        /* status code map */
        StatusCodeMap = new HashMap<String,Integer>();
        StatusCodeMap.put("GPS"      , new Integer(StatusCodes.STATUS_LOCATION));
        StatusCodeMap.put("PANIC"    , new Integer(StatusCodes.STATUS_PANIC_ON));
        StatusCodeMap.put("SOS"      , new Integer(StatusCodes.STATUS_PANIC_ON));
        StatusCodeMap.put("ASSIST"   , new Integer(StatusCodes.STATUS_ASSIST_ON));
        StatusCodeMap.put("HELP"     , new Integer(StatusCodes.STATUS_ASSIST_ON));
        StatusCodeMap.put("NOTIFY"   , new Integer(StatusCodes.STATUS_NOTIFY));
        StatusCodeMap.put("MEDICAL"  , new Integer(StatusCodes.STATUS_MEDICAL_ON));
        StatusCodeMap.put("NURSE"    , new Integer(StatusCodes.STATUS_MEDICAL_ON));
        StatusCodeMap.put("DOCTOR"   , new Integer(StatusCodes.STATUS_MEDICAL_ON));
        StatusCodeMap.put("IMPACT"   , new Integer(StatusCodes.STATUS_IMPACT));
        StatusCodeMap.put("ACCIDENT" , new Integer(StatusCodes.STATUS_IMPACT));
        StatusCodeMap.put("WAYMARK"  , new Integer(StatusCodes.STATUS_WAYMARK_0));
        StatusCodeMap.put("JOBSTART" , new Integer(StatusCodes.STATUS_JOB_ARRIVE));
        StatusCodeMap.put("JOBARRIVE", new Integer(StatusCodes.STATUS_JOB_ARRIVE));
        StatusCodeMap.put("JOBEND"   , new Integer(StatusCodes.STATUS_JOB_DEPART));
        StatusCodeMap.put("JOBDEPART", new Integer(StatusCodes.STATUS_JOB_DEPART));
        StatusCodeMap.put("LOGIN"    , new Integer(StatusCodes.STATUS_LOGIN));
        StatusCodeMap.put("LOGOUT"   , new Integer(StatusCodes.STATUS_LOGOUT));

        /* enable Device event logging */
        Device.SetLogEventDataInsertion(Print.LOG_INFO);

    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* translate status code from device */
    private static int TranslateStatusCode(String statusCodeStr)
    {

        /* blank status */
        if (StringTools.isBlank(statusCodeStr)) {
            // no status code specified
            return StatusCodes.STATUS_LOCATION;
        }
        String sc = statusCodeStr.toUpperCase();

        /* check status code name */
        StatusCodes.Code code = StatusCodes.GetCode(sc,null);
        if (code != null) {
            return code.getCode();
        }

        /* status code number? */
        if (StringTools.isInt(statusCodeStr,true)) {
            return StringTools.parseInt(statusCodeStr, StatusCodes.STATUS_NONE);
        }

        /* check default codes */
        Integer sci = StatusCodeMap.get(sc);
        if (sci != null) {
            return sci.intValue();
        }

        // TODO: Status code translations are device dependent, thus this section will 
        //       needs to be customized to the specific device using this server.
        //
        // For instance, the Mologogo would use the following code translation:
        //    "GPS"    => StatusCodes.STATUS_LOCATION
        //    "CELL"   => StatusCodes.STATUS_LOCATION
        //    "MANUAL" => StatusCodes.STATUS_WAYMARK_0
        //
        // For now, just return the generic StatusCodes.STATUS_LOCATION
        return StatusCodes.STATUS_LOCATION;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Load Device record from MobileID/AccountID/DeviceID
    **/
    private Device loadDevice(String ipAddr, 
        String mobileID,
        String accountID, String deviceID,
        String authCode)
    {
        Device device = null;

        /* DeviceID used as MobileID? */
        if (StringTools.isBlank(mobileID) && StringTools.isBlank(accountID) && 
            !StringTools.isBlank(deviceID)) {
            mobileID = deviceID;
            deviceID = null;
        }

        /* unique id? */
        if (!StringTools.isBlank(mobileID)) {

            /* get Device by UniqueID */
            for (int i = 0; (i < UniqueIDPrefix.length) && (device == null); i++) {
                String uid = UniqueIDPrefix[i] + mobileID; // ie: "gprmc_123456789012345"
                try {
                    device = Transport.loadDeviceByUniqueID(uid);
                } catch (DBException dbe) {
                    Data.logError(null, "Exception getting Device: " + uid + " [" + dbe + "]");
                    return null;
                }
            }
            if (device == null) {
                String uid = UniqueIDPrefix[0] + mobileID;
                Data.logWarn("Mobile ID not found!: "+mobileID + " ["+uid+"]");
                //Print.sysPrintln("NotFound: now=%d id=%s", DateTime.getCurrentTimeSec(), uid);
                DCServerFactory.addUnassignedDevice(LOG_NAME, uid, ipAddr, true, null);
                return null;
            }

        } else {

            /* account id? */
            if (StringTools.isBlank(accountID)) {
                accountID = DefaultAccountID;
                if (StringTools.isBlank(accountID)) {
                    Data.logError(null, "Unable to identify Account");
                    Data.logError(null, "(has '"+CONFIG_PARM_ACCOUNT + "' or '"+CONFIG_PARM_MOBILE+"' been properly configured in 'webapp.conf'?)");
                    return null;
                }
            }

            /* device id? */
            if (StringTools.isBlank(deviceID)) {
                Data.logError(null, "Unable to identify Device");
                Data.logError(null, "(has '"+CONFIG_PARM_DEVICE+"' been properly configured in 'webapp.conf'?)");
                return null;
            }

            /* read the device */
            try {
                device = Transport.loadDeviceByTransportID(Account.getAccount(accountID), deviceID);
            } catch (DBException dbe) {
                // Error while reading Device
                String uid = accountID + "/" + deviceID;
                Data.logException(null, "Error reading Device: " + uid, dbe);
                return null;
            }
            if (device == null) {
                // Device was not found
                String uid = accountID + "/" + deviceID;
                Data.logError(null, "Device not found AccountID/DeviceID: " + uid);
                //Print.sysPrintln("NotFound: now=%d id=%s", DateTime.getCurrentTimeSec(), uid);
                DCServerFactory.addUnassignedDevice(LOG_NAME, uid, ipAddr, true, null);
                return null;
            }

        }
        // "device" is non-null at this point

        /* validate auth code */
        if (!device.validateDataKey(authCode)) {
            // 'authCode' does not match device 'dataKey'
            Data.logError(null, "Invalid PIN for device");
            return null;
        }

        /* validate source IP address */
        // This may be used to prevent rogue hackers from spoofing data coming from the phone
        DataTransport dataXPort = device.getDataTransport();
        if (!dataXPort.isValidIPAddress(ipAddr)) {
            // 'ipAddr' does not match allowable device IP addresses
            Data.logError(null, "Invalid IP Address for device");
            return null;
        }

        /* set transport attributes */
        dataXPort.setIpAddressCurrent(ipAddr);      // FLD_ipAddressCurrent
        dataXPort.setDeviceCode(DEVICE_CODE);       // FLD_deviceCode
        device.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime

        /* return device */
        return device;

    }

    // ------------------------------------------------------------------------

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this._doWork(true, request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // http://localhost:8080/gprmc/Data?id=12345&code=0xF011&lat=39.1234&lon=-142.1234
        this._doWork(false, request, response);
    }
    
    private void _doWork(final boolean isPost, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String ipAddr     = request.getRemoteAddr();
        String phoneID    = AttributeTools.getRequestString(request, PARM_PHONE     , null);
        String mobileID   = AttributeTools.getRequestString(request, PARM_MOBILE    , phoneID);
        String accountID  = AttributeTools.getRequestString(request, PARM_ACCOUNT   , null);
        String deviceID   = AttributeTools.getRequestString(request, PARM_DEVICE    , "");
        String password   = AttributeTools.getRequestString(request, PARM_PASSWORD  , "");  // not currently used
        String authCode   = AttributeTools.getRequestString(request, PARM_AUTH      , "");  
        String driverID   = AttributeTools.getRequestString(request, PARM_DRIVER    , "");
        String uniqueID   = null;

        /* URL */
        StringBuffer reqURL = request.getRequestURL();
        String queryStr = StringTools.blankDefault(request.getQueryString(),"(n/a)");
        if (isPost) {
            // 'queryStr' is likely not available
            StringBuffer postSB = new StringBuffer();
            for (java.util.Enumeration ae = request.getParameterNames(); ae.hasMoreElements();) {
                if (postSB.length() > 0) { postSB.append("&"); }
                String ak = (String)ae.nextElement();
                String av = request.getParameter(ak);
                postSB.append(ak + "=" + av);
            }
            Data.logInfo("[" + ipAddr + "] POST: " + reqURL + " " + queryStr + " [" + postSB + "]");
        } else {
            Data.logInfo("[" + ipAddr + "] GET: "  + reqURL + " " + queryStr);
        }

        /* "&cmd=version"? */
        if (isPost) {
            String cmd = AttributeTools.getRequestString(request, PARM_COMMAND, "");
            if (StringTools.isBlank(cmd)) {
                // -- no command requested (fixed 2.5.4-B10)
            } else
            if (ListTools.contains(PARM_VERSION,cmd)) {
                // -- version requested
                String vers = DEVICE_CODE+"-"+VERSION;
                Print.logInfo("Version Command: " + vers);
                this.plainTextResponse(response, "OK:version:ver="+vers+";");
                return;
            } else
            if (cmd.equalsIgnoreCase("mobileid") ||
                cmd.equalsIgnoreCase("id")         ) {
                // -- MobileID exists
                Print.logInfo("Command not supported: " + cmd);
                this.plainTextResponse(response, "ERROR:command:"+cmd);
                return;
            } else {
                // -- unknown/unsupported command
                Print.logInfo("Command not supported: " + cmd);
                this.plainTextResponse(response, "ERROR:command:"+cmd);
                return;
            }
        }

        /* Device */
        Device device = this.loadDevice(ipAddr, mobileID, accountID, deviceID, authCode);
        if (device == null) {
            this.plainTextResponse(response, RESPONSE_NOT_AUTH);
            return;
        }

        /* Device vars */
        uniqueID  = device.getUniqueID();
        accountID = device.getAccountID();
        deviceID  = device.getDeviceID();
        Account account = device.getAccount();
        if (account == null) {
            Data.logError(null, "Account record not found!");
            this.plainTextResponse(response, RESPONSE_ERROR);
            return;
        }

        // ---------------------------------------------------------------------------------
        // --- Parse data below --- 

        /* message */
        String message     = AttributeTools.getRequestString(request, PARM_MESSAGE     , "");
        String emailAddr   = AttributeTools.getRequestString(request, PARM_EMAIL       , "");

        /* extract fields from URL arguments */
        String _version    = AttributeTools.getRequestString(request, PARM_VERSION     , null);
        String _statusStr  = AttributeTools.getRequestString(request, PARM_STATUS      , null);
        String _address    = AttributeTools.getRequestString(request, PARM_ADDRESS     , null);
        String _altitudeM  = AttributeTools.getRequestString(request, PARM_ALTITUDE    , null);
        String _odometerKM = AttributeTools.getRequestString(request, PARM_ODOMETER_KM , null);
        String _odometerMI = AttributeTools.getRequestString(request, PARM_ODOMETER_MI , null);
        String _distanceKM = AttributeTools.getRequestString(request, PARM_DISTANCE_KM , null);
        String _distanceMI = AttributeTools.getRequestString(request, PARM_DISTANCE_MI , null);
        String _gprmcStr   = AttributeTools.getRequestString(request, PARM_GPRMC       , null);
        String _dateStr    = AttributeTools.getRequestString(request, PARM_DATE        , null);
        String _timeStr    = AttributeTools.getRequestString(request, PARM_TIME        , null);
        String _latitude   = AttributeTools.getRequestString(request, PARM_LATITUDE    , null);
        String _longitude  = AttributeTools.getRequestString(request, PARM_LONGITUDE   , null);
        String _speedKPH   = AttributeTools.getRequestString(request, PARM_SPEED_KPH   , null);
        String _speedMPH   = AttributeTools.getRequestString(request, PARM_SPEED_MPH   , null);
        String _headingDeg = AttributeTools.getRequestString(request, PARM_HEADING     , null);
        String _hdop       = AttributeTools.getRequestString(request, PARM_HDOP        , null);
        String _horzAcc    = AttributeTools.getRequestString(request, PARM_HORZ_ACC    , null);
        String _vertAcc    = AttributeTools.getRequestString(request, PARM_VERT_ACC    , null);
        String _numSats    = AttributeTools.getRequestString(request, PARM_NUM_SATS    , null);
        String _battLevel  = AttributeTools.getRequestString(request, PARM_BATT_LEVEL  , null);
        String _battVolts  = AttributeTools.getRequestString(request, PARM_BATT_VOLTS  , null);
        String _battTempC  = AttributeTools.getRequestString(request, PARM_BATT_TEMPC  , null);
        String _tempC      = AttributeTools.getRequestString(request, PARM_TEMPERATURE , null);
        String _cellMCC    = AttributeTools.getRequestString(request, PARM_CELLID_MCC  , null);
        String _cellMNC    = AttributeTools.getRequestString(request, PARM_CELLID_MNC  , null);
        String _cellLAC    = AttributeTools.getRequestString(request, PARM_CELLID_LAC  , null);
        String _cellCID    = AttributeTools.getRequestString(request, PARM_CELLID_CID  , null);
        String _cellTAV    = AttributeTools.getRequestString(request, PARM_CELLID_TAV  , null);
        String _cellRAT    = AttributeTools.getRequestString(request, PARM_CELLID_RAT  , null);
        String _cellRXLEV  = AttributeTools.getRequestString(request, PARM_CELLID_RXLEV, null);
        String _cellARFCN  = AttributeTools.getRequestString(request, PARM_CELLID_ARFCN, null);

        /* no location data specified? */
        if (StringTools.isBlank(_gprmcStr)  && 
            StringTools.isBlank(_statusStr) &&
            StringTools.isBlank(_latitude)  &&
            StringTools.isBlank(_longitude)   ) {
            this.plainTextResponse(response, RESPONSE_OK);
            return;
        }

        /* parse fields */
        String version     = _version;
        String statusStr   = _statusStr;
        String address     = _address;
        double altitudeM   = StringTools.parseDouble(_altitudeM , 0.0);         // meters
        double odometerKM  = StringTools.parseDouble(_odometerKM, 0.0);         // kilometers
        double odometerMI  = StringTools.parseDouble(_odometerMI, 0.0);         // miles
        double distanceKM  = StringTools.parseDouble(_distanceKM, 0.0);         // kilometers
        double distanceMI  = StringTools.parseDouble(_distanceMI, 0.0);         // miles
        String gprmcStr    = (_gprmcStr != null)? _gprmcStr.toUpperCase() : null;// $GPRMC
        String dateStr     = _dateStr;
        String timeStr     = _timeStr;
        double latitude    = this._parseLatitude( _latitude );
        double longitude   = this._parseLongitude(_longitude);
        double speedKPH    = StringTools.parseDouble(_speedKPH  , 0.0);         // km/h
        double speedMPH    = StringTools.parseDouble(_speedMPH  , 0.0);         // mph
        double headingDeg  = this._parseHeading(_headingDeg);                   // degrees
        double hdop        = StringTools.parseDouble(_hdop      , 0.0);         // HDOP
        double horzAcc     = StringTools.parseDouble(_horzAcc   , 0.0);         // meters
        double vertAcc     = StringTools.parseDouble(_vertAcc   , 0.0);         // meters
        int    numSats     = StringTools.parseInt(   _numSats   , 0);           // #satellites

        /* convert MPH to KM if necessary */
        // -- this specifically tries to identify the SourceForge "GpsTracker" format,
        // -  which uses distance units in Feet/Miles rather than Meters/Kilometers.
        // -  http://localhost:8080/gprmc/Data?latitude=40.1234&longitude=-142.1234&speed=23.4&direction=127&date=2014-10-10%2012:34:56&&locationmethod=aaa&distance=1234&username=smith&phonenumber=555-1212&sessionid=12345678&accuracy=200&extrainfo=1234&eventtype=android
        boolean isUSUnits = AttributeTools.hasRequestAttribute(request,"usunits") ||
            (AttributeTools.hasRequestAttribute(request,"sessionid") &&
             AttributeTools.hasRequestAttribute(request,"extrainfo") &&
             AttributeTools.hasRequestAttribute(request,"eventtype")   );
        // -- odometer (miles ==> km)
        if ((odometerKM <= 0.0) && (odometerMI > 0.0)) {
            odometerKM = odometerMI * GeoPoint.KILOMETERS_PER_MILE;
        } else
        if (isUSUnits && (odometerKM > 0.0)) {
            Print.logInfo("Converting Odometer US units from miles to kilometers ...");
            odometerKM = odometerKM * GeoPoint.KILOMETERS_PER_MILE;
        }
        // -- distance/trip (miles ==> km)
        if ((distanceKM <= 0.0) && (distanceMI > 0.0)) {
            distanceKM = distanceMI * GeoPoint.KILOMETERS_PER_MILE;
        } else
        if (isUSUnits && (distanceKM > 0.0)) {
            Print.logInfo("Converting Distance US units from miles to kilometers ...");
            distanceKM = distanceKM * GeoPoint.KILOMETERS_PER_MILE;
        }
        // -- speed (mph ==> km/h)
        if ((speedKPH <= 0.0) && (speedMPH > 0.0)) {
            speedKPH = speedMPH * GeoPoint.KILOMETERS_PER_MILE;
        } else
        if (isUSUnits && (speedKPH > 0.0)) {
            Print.logInfo("Converting Speed US units from mph to km/h ...");
            speedKPH = speedKPH * GeoPoint.KILOMETERS_PER_MILE;
        }
        // -- accuracy (feet ==> meters)
        if (isUSUnits && (horzAcc > 0.0)) {
            Print.logInfo("Converting Acurracy units from feet to meters ...");
            horzAcc = horzAcc * GeoPoint.METERS_PER_FOOT;
        }
        // -- altitude (feet ==> meters)
        if (isUSUnits && (altitudeM > 0.0)) {
            Print.logInfo("Converting Altitude US units from feet to meters ...");
            altitudeM = altitudeM * GeoPoint.METERS_PER_FOOT;
        }

        /* battery */
        double battLevel   = StringTools.parseDouble(_battLevel , 0.0);         // %
        double battVolts   = StringTools.parseDouble(_battVolts , 0.0);         // volts
        double battTempC   = StringTools.parseDouble(_battTempC , BAD_TEMP);    // degrees C
        if ((_battLevel != null) && _battLevel.indexOf(".") < 0) { // fix NPE 2.5.6-B18
            // -- "&batt=50": no decimal-point in battery level, assume integer range 0..100
            battLevel /= 100.0;
        } else
        if (battLevel > 1.0) {
            // -- "&batt=50.3": found decimal-point, but greater than "1.0", assume decimal range 1.0..100.0
            // -  this may incorrectly re-adjust levels on devices that show a battery-level grater than 100%
            // -  where "1.05" can be interpreted as 105%.
            battLevel /= 100.0;
        }

        /* temperature */
        double tempC       = StringTools.parseDouble(_tempC     , BAD_TEMP);// degrees C

        /* cell-tower */
        int    cellMCC     = StringTools.parseInt(_cellMCC  , 0); 
        int    cellMNC     = StringTools.parseInt(_cellMNC  , 0); 
        int    cellLAC     = StringTools.parseInt(_cellLAC  , 0); 
        int    cellCID     = StringTools.parseInt(_cellCID  , 0); 
        int    cellTAV     = StringTools.parseInt(_cellTAV  , 0); 
        int    cellRAT     = StringTools.parseInt(_cellRAT  , 0); 
        int    cellRXLEV   = StringTools.parseInt(_cellRXLEV, 0); 
        int    cellARFCN   = StringTools.parseInt(_cellARFCN, 0); 
        CellTower servingCell = null;
        if ((cellMCC >= 0) && (cellMNC >= 0) && (cellLAC >= 0) && (cellCID > 0)) {
            servingCell = new CellTower();
            servingCell.setMobileCountryCode(cellMCC);
            servingCell.setMobileNetworkCode(cellMNC);
            servingCell.setLocationAreaCode(cellLAC);
            servingCell.setCellTowerID(cellCID);
            servingCell.setTimingAdvance(cellTAV);
            servingCell.setRadioAccessTechnology(cellRAT);
            servingCell.setReceptionLevel(cellRXLEV);
            servingCell.setAbsoluteRadioFrequencyChannelNumber(cellARFCN);
        }

        /* status code translation */
        int statusCode = Data.TranslateStatusCode(statusStr);

        /* latitude, longitude, speed, heading, ... */
        boolean isValidGPS = false;
        long    fixtime    = 0L;
        if (!StringTools.isBlank(gprmcStr)) {
            if (gprmcStr.startsWith("GPRMC") || 
                gprmcStr.startsWith("GPGGA")   ) {
                // allow "GPRMC,....", adjust to "$GPRMC,...."
                gprmcStr = "$" + gprmcStr;
            } else
            if (gprmcStr.startsWith("$GPRMC") || 
                gprmcStr.startsWith("$GPGGA")   ) {
                // ok as-is
            } else {
                Data.logError(null, "Missing/Invalid $GPRMC: " + gprmcStr);
                Data.logError(null, "(is '"+CONFIG_PARM_GPRMC+"' properly configured in 'webapp.conf'?)");
                this.plainTextResponse(response, RESPONSE_ERROR);
                return;
            }
            boolean ignoreChecksum = (gprmcStr.indexOf("*") >= 0)?  // ignore checksum if not present
                false : // found, do not ignore
                true  ; // not found, ignore
            Nmea0183 gprmc = new Nmea0183(gprmcStr, ignoreChecksum);
            fixtime    = gprmc.getFixtime();
            isValidGPS = gprmc.isValidGPS();
            latitude   = isValidGPS? gprmc.getLatitude()           : 0.0;
            longitude  = isValidGPS? gprmc.getLongitude()          : 0.0;
            speedKPH   = isValidGPS? gprmc.getSpeedKPH()           : 0.0;
            headingDeg = isValidGPS? gprmc.getHeading()            : 0.0;
            numSats    = isValidGPS? gprmc.getNumberOfSatellites() :   0;
            altitudeM  = isValidGPS? gprmc.getAltitudeMeters()     : 0.0;
            if (!isValidGPS) {
                Data.logWarn("Invalid latitude/longitude");
            }
        } else {
            fixtime    = this._parseFixtime(dateStr, timeStr);
            if ((latitude == INVALID_LATLON) || (longitude == INVALID_LATLON)) {
                Data.logError(null, "Missing/Invalid latitude/longitude");
                Data.logError(null, "(is '"+CONFIG_PARM_LATITUDE+"'/'"+CONFIG_PARM_LONGITUDE+"' properly configured in 'webapp.conf'?)");
                isValidGPS = false;
                latitude   = 0.0;
                longitude  = 0.0;
            } else
            if ((latitude  >=  90.0) || (latitude  <=  -90.0) ||
                (longitude >= 180.0) || (longitude <= -180.0) ||
                ((latitude == 0.0) && (longitude == 0.0)    )   ) {
                Data.logWarn("Invalid latitude/longitude: " + latitude + "/" + longitude);
                isValidGPS = false;
                latitude   = 0.0;
                longitude  = 0.0;
            } else {
                isValidGPS = true;
            }
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                
        /* adjustments to speed/heading */
        if (!isValidGPS || (speedKPH < MinimumReqSpeedKPH)) {
            // Say we're not moving if the value is <= our desired threshold
            speedKPH = 0.0;
        }
        if ((speedKPH <= 0.0) || (headingDeg < 0.0)) {
            // We're either not moving, or the GPS receiver doesn't know the heading
            headingDeg = 0.0; // to be consistent, set the heading to North
        }

        /* estimate GPS-based odometer */
        String estAct;
        if (odometerKM <= 0.0) {
            // calculate odometer
            odometerKM = (ESTIMATE_ODOMETER && isValidGPS)? 
                device.getNextOdometerKM(new GeoPoint(latitude,longitude)) : 
                device.getLastOdometerKM();
            estAct = "(estimated)";
        } else {
            // bounds-check odometer
            odometerKM = device.adjustOdometerKM(odometerKM);
            estAct = "(actual)";
        }

        /* debug info */
        //if (RTConfig.isDebugMode()) {
        if (!StringTools.isBlank(version)) {
            Data.logInfo("Version  : " + version);
        }
        Data.logInfo("Fixtime  : ["+fixtime+"] " + (new DateTime(fixtime)));
        Data.logInfo("Status   : ["+StatusCodes.GetHex(statusCode)+"] " + StatusCodes.GetDescription(statusCode,null));
        Data.logInfo("Device   : " + accountID + "/" + deviceID + " [" + uniqueID + "]");
        Data.logInfo("GeoPoint : " + latitude + "/" + longitude + " [" + numSats + "]");
        Data.logInfo("SpeedKPH : " + speedKPH + " km/h  [" + headingDeg + "]");
        if (!StringTools.isBlank(message)) {
            Data.logInfo("Message  : " + message);
        }
        if (altitudeM != 0.0) {
            Data.logInfo("Altitude : " + altitudeM + " meters");
        }
        if (odometerKM > 0.0) {
            Data.logInfo("Odometer : " + odometerKM + " km " + estAct);
        }
        if (distanceKM > 0.0) {
            Data.logInfo("Distance : " + distanceKM + " km");
        }
        if (hdop > 0.0) {
            Data.logInfo("HDOP     : " + hdop);
        }
        if (horzAcc > 0.0) {
            Data.logInfo("Hor Acc  : " + horzAcc + " meters");
        }
        if (vertAcc > 0.0) {
            Data.logInfo("Vert Acc : " + vertAcc + " meters");
        }
        if (battLevel > 0.0) {
            Data.logInfo("BattLevel: " + battLevel + " %");
        }
        if (battVolts > 0.0) {
            Data.logInfo("BattVolts: " + battVolts + " V");
        }
        if (EventData.isValidTemperature(battTempC)) {
            Data.logInfo("BattTempC: " + battTempC + " C");
        }
        if (EventData.isValidTemperature(tempC)) {
            Data.logInfo("Temp C   : " + tempC + " C");
        }
        if (servingCell != null) {
            Data.logInfo("CellTower: " + servingCell.toString());
        }
        //}

        /* reject invalid GPS fixes? */
        if (!isValidGPS && (statusCode == StatusCodes.STATUS_LOCATION)) {
            // ignore invalid GPS fixes that have a simple 'STATUS_LOCATION' status code
            Data.logWarn("Ignoring event with invalid latitude/longitude");
            this.plainTextResponse(response, RESPONSE_OK); // "");
            return;
        }

        /* simulate geozones */
        if (SIMEVENT_GEOZONES && isValidGPS) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    EventData.Key zoneKey = new EventData.Key(accountID, deviceID, z.getTimestamp(), z.getStatusCode());
                    EventData zoneEv = zoneKey.getDBRecord();
                    zoneEv.setGeozone(z.getGeozone());
                    zoneEv.setLatitude(latitude);
                    zoneEv.setLongitude(longitude);
                    zoneEv.setSpeedKPH(speedKPH);
                    zoneEv.setHeading(headingDeg);
                    zoneEv.setAltitude(altitudeM);
                    zoneEv.setOdometerKM(odometerKM);
                    zoneEv.setDistanceKM(distanceKM);
                    zoneEv.setAddress(address);
                    zoneEv.setDriverID(driverID);
                    zoneEv.setDriverMessage(message);
                    zoneEv.setHDOP(hdop);
                    zoneEv.setHorzAccuracy(horzAcc);
                    zoneEv.setVertAccuracy(vertAcc);
                    zoneEv.setSatelliteCount(numSats);
                    zoneEv.setBatteryLevel(battLevel);
                    zoneEv.setBatteryVolts(battVolts);
                    if (EventData.isValidTemperature(battTempC)) {
                        // --  BatteryTemp defined only in OpenGTS_2.5.5+
                        zoneEv.setBatteryTemp(battTempC); // Comment this line if a compiler error occurs
                    } 
                    if (EventData.isValidTemperature(tempC)) {
                        zoneEv.setThermoAverage0(tempC);
                    }
                    zoneEv.setServingCellTower(servingCell);
                    if (device.insertEventData(zoneEv)) {
                        Print.logInfo("Geozone    : " + z);
                    }
                }
            }
        }
    
        /* create/insert new event record */
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb = evKey.getDBRecord();
        evdb.setLatitude(latitude);
        evdb.setLongitude(longitude);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(headingDeg);
        evdb.setAltitude(altitudeM);
        evdb.setOdometerKM(odometerKM);
        evdb.setDistanceKM(distanceKM);
        evdb.setAddress(address);
        evdb.setDriverID(driverID);
        evdb.setDriverMessage(message);
        evdb.setHDOP(hdop);
        evdb.setHorzAccuracy(horzAcc);
        evdb.setVertAccuracy(vertAcc);
        evdb.setSatelliteCount(numSats);
        evdb.setBatteryLevel(battLevel);
        evdb.setBatteryVolts(battVolts);
        if (EventData.isValidTemperature(battTempC)) {
            // --  BatteryTemp defined only in OpenGTS_2.5.5+
            evdb.setBatteryTemp(battTempC); // Comment this line if a compiler error occurs
        } 
        if (EventData.isValidTemperature(tempC)) {
            evdb.setThermoAverage0(tempC);
        }
        evdb.setServingCellTower(servingCell);
        //evdb.setEmailRecipient(emailAddr);
        // -- this will display an error if it was unable to store the event
        if (device.insertEventData(evdb)) {
            //Data.logDebug("Event inserted: "+accountID+"/"+deviceID+" - " + evdb.getGeoPoint());
        }

        /* update device version */

        /* save device changes */
        try {
            // -- TODO: check "this.device" vs "this.dataXPort"
            if (!StringTools.isBlank(version)) {
                String V = (version.length() > 30)? version.substring(0,30) : version;
                device.setCodeVersion(V);
                device.updateChangedEventFields(Device.FLD_codeVersion);
            } else {
                device.updateChangedEventFields();
            }
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + 
                device.getAccountID() + "/" + device.getDeviceID(), dbe);
        }

        /* write success response */
        this.plainTextResponse(response, RESPONSE_OK);

    }

    private long _parseFixtime(String dateStr, String timeStr)
    {
        // Examples:
        // 0) if (DateFormat == DATE_FORMAT_NONE):
        //      return current time
        // 1) if (DateFormat == DATE_FORMAT_EPOCH):
        //      &date=1187809084
        // 2) if (DateFormat == DATE_FORMAT_YMD):
        //      &date=2007/08/21&time=17:59:23
        //      &date=20070821&time=175923
        //      &date=070821&time=175923
        // 3) if (DateFormat == DATE_FORMAT_MDY):
        //      &date=08/21/2007&time=17:59:23
        //      &date=08212007&time=175923
        //      &date=082107&time=175923
        // 4) if (DateFormat == DATE_FORMAT_DMY):
        //      &date=21/08/2007&time=17:59:23
        //      &date=21082007&time=175923
        //      &date=210807&time=175923
        // 5) if (DateFormat == DATE_FORMAT_YMDhms):
        //      &date=1351798496                 <-- Epoch supported
        //      &date=20121104123432             <-- YYYYMMDDhhmmss
        //      &date=2012/11/04,12:34:32        <-- YYYY/MM/DD,hh:mm:ss
        //      &date=2012-11-04T12:34:32+00:00  <-- YYYY-MM-DDThh:mm:ssZ (GPX format)
        //      &date=2012-11-24 12:34:32        <-- YYYY-MM-DD hh:mm:ss (TODO:)

        /* no date/time specification? */
        if (DateFormat == DATE_FORMAT_NONE) {
            return DateTime.getCurrentTimeSec();
        }

        /* no date/time? */
        if (StringTools.isBlank(dateStr) &&   // fixed 2.5.2-B28 
            StringTools.isBlank(timeStr)   ) {
            // both "dateStr" and "timeStr" are blank, use current time
            return DateTime.getCurrentTimeSec();
        }

        /* unix 'Epoch' time? */
        if (DateFormat == DATE_FORMAT_EPOCH) {
            String epochStr = !StringTools.isBlank(dateStr)? dateStr : timeStr;
            if (StringTools.isBlank(epochStr)) {
                return DateTime.getCurrentTimeSec();
            } else {
                long timestamp = StringTools.parseLong(epochStr, 0L);
                if (timestamp > MaxTimestamp) { // (long)Integer.MAX_VALUE * 2)
                    timestamp /= 1000L; // timestamp was specified in milliseconds
                }
                return (timestamp > 0L)? timestamp : DateTime.getCurrentTimeSec();
            }
        }

        /* DateTime YYYYMMDDhhmmss time format (also supports EPOCH) */
        if (((DateFormat == DATE_FORMAT_YMDhms) || (DateFormat == DATE_FORMAT_YMD)) &&
            (StringTools.isBlank(timeStr) || StringTools.isBlank(dateStr))) {
            String dtStr = null;
            if (StringTools.isBlank(timeStr)) {
                // &date=1351798496                 <-- Epoch supported
                // &date=20121104123432             <-- YYYYMMDDhhmmss
                // &date=2012/11/04,12:34:32        <-- YYYY/MM/DD,hh:mm:ss
                // &date=2012-11-04T12:34:32+00:00  <-- YYYY-MM-DDThh:mm:ssZ (GPX format)
                // &date=2012-11-24 12:34:32+00:00  <-- YYYY-MM-DD hh:mm:ssZ (TODO:)
                dtStr = dateStr;
            } else {
                // &time=1351798496                 <-- Epoch supported
                // &time=20121104123432             <-- YYYYMMDDhhmmss
                // &time=2012/11/04,12:34:32        <-- YYYY/MM/DD,hh:mm:ss
                // &time=2012-11-04T12:34:32+00:00  <-- YYYY-MM-DDThh:mm:ssZ (GPX format)
                // &time=2012-11-24 12:34:32+00:00  <-- YYYY-MM-DD hh:mm:ssZ (TODO:)
                dtStr = timeStr;
            }
            try {
                DateTime dt = DateTime.parseArgumentDate(dtStr, gmtTimeZone,
                    DateTime.DefaultParsedTime.CurrentTime);
                return dt.getTimeSec();
            } catch (DateTime.DateParseException dpe) {
                Print.logWarn("Unable to parse date/time: " + dtStr);
                return DateTime.getCurrentTimeSec();
            }
        }

        /* time */
        if (timeStr.indexOf(":") >= 0) {
            // Convert "HH:MM:SS" to "HHMMSS"
            timeStr = StringTools.stripChars(timeStr,':');
        }
        if (timeStr.length() < 6) {
            // invalid time length, expecting at least "HHMMSS"
            // (ignoring "dateStr" value)
            return DateTime.getCurrentTimeSec();
        }
        // timeStr may be "HHMMSS" or "HHMMSS.000"

        /* date */
        if (dateStr.indexOf("/") >= 0) {
            // Convert "YYYY/MM/DD" to "YYYYMMDD"
            dateStr = StringTools.stripChars(dateStr,'/');
        }
        int dateLen = dateStr.length();
        if ((dateLen != 8) && (dateLen != 6)) {
            // invalid date length
            return DateTime.getCurrentTimeSec();
        }

        /* parse date */
        int YYYY = 0;
        int MM   = 0;
        int DD   = 0;
        if (DateFormat == DATE_FORMAT_YMD) {
            if (dateLen == 8) {
                YYYY = StringTools.parseInt(dateStr.substring(0,4), 0);
                MM   = StringTools.parseInt(dateStr.substring(4,6), 0);
                DD   = StringTools.parseInt(dateStr.substring(6,8), 0);
            } else { // datalen == 6
                YYYY = StringTools.parseInt(dateStr.substring(0,2), 0) + 2000;
                MM   = StringTools.parseInt(dateStr.substring(2,4), 0);
                DD   = StringTools.parseInt(dateStr.substring(4,5), 0); // fixed 2011/03/14
            }
        } else
        if (DateFormat == DATE_FORMAT_MDY) {
            if (dateLen == 8) {
                MM   = StringTools.parseInt(dateStr.substring(0,2), 0);
                DD   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,8), 0);
            } else { // datalen == 6
                MM   = StringTools.parseInt(dateStr.substring(0,2), 0);
                DD   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,6), 0) + 2000;
            }
        } else
        if (DateFormat == DATE_FORMAT_DMY) {
            if (dateLen == 8) {
                DD   = StringTools.parseInt(dateStr.substring(0,2), 0);
                MM   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,8), 0);
            } else { // datalen == 6
                DD   = StringTools.parseInt(dateStr.substring(0,2), 0);
                MM   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,6), 0) + 2000;
            }
        } else {
            // invalid date format specification
            return DateTime.getCurrentTimeSec();
        }

        /* parse time */
        int hh = StringTools.parseInt(timeStr.substring(0,2), 0);
        int mm = StringTools.parseInt(timeStr.substring(2,4), 0);
        int ss = StringTools.parseInt(timeStr.substring(4,6), 0);

        /* return epoch time */
        DateTime dt = new DateTime(gmtTimeZone, YYYY, MM, DD, hh, mm, ss);
        return dt.getTimeSec();

    }

    private double _parseLatitude(String latStr)
    {
        // Possible formats:
        //  -39.12345    - Decimal
        //  4710.1058N   - NMEA-0183 format
        //  4710.1058,N  - NMEA-0183 format
        if (StringTools.isBlank(latStr)) {
            return INVALID_LATLON;
        } else 
        if ((latStr.length() >= 6) && Character.isDigit(latStr.charAt(0)) && (latStr.charAt(4) == '.')) {
            // assume "4710.1058N", "4710.1058,N", ...
            // also will parse "4710.1" as "47^10.1' North"
            double _lat = StringTools.parseDouble(latStr, 9001.0);
            if (_lat < 9000.0) {
                double lat = (double)((long)_lat / 100L); // _lat is always positive here
                lat += (_lat - (lat * 100.0)) / 60.0;
                char d = Character.toUpperCase(latStr.charAt(latStr.length() - 1)); // last character N/S
                return (d == 'S')? -lat : lat;
            } else {
                return INVALID_LATLON; // invalid latitude
            }
        } else {
            // assume "-39.12345", "47.12345", ...
            return StringTools.parseDouble(latStr, INVALID_LATLON);
        }
    }

    private double _parseLongitude(String lonStr)
    {
        // Possible formats:
        //  142.12345       - Decimal
        //  01945.1212E     - NMEA-0183 format
        //  01945.1212,E    - NMEA-0183 format
        if (StringTools.isBlank(lonStr)) {
            return INVALID_LATLON;
        } else 
        if ((lonStr.length() >= 7) && Character.isDigit(lonStr.charAt(0)) && (lonStr.charAt(5) == '.')) {
            // assume "01945.1212E", "01945.1212,E", ...
            // also will parse "01945.1" as "19^45.1' East"
            double _lon = StringTools.parseDouble(lonStr, 18001.0);
            if (_lon < 18000.0) {
                double lon = (double)((long)_lon / 100L); // _lon is always positive here
                lon += (_lon - (lon * 100.0)) / 60.0;
                char d = Character.toUpperCase(lonStr.charAt(lonStr.length() - 1)); // last character E/W
                return (d == 'W')? -lon : lon;
            } else {
                return INVALID_LATLON;
            }
        } else {
            // assume "142.12345", "-137.12345", ...
            return StringTools.parseDouble(lonStr, INVALID_LATLON);
        }
    }

    private double _parseHeading(String headingStr)
    {
        if (StringTools.isBlank(headingStr)) {
            return 0.0;
        } else
        if (Character.isLetter(headingStr.charAt(0))) {
            // assume "N", "NE", "E", "SE", "S", "SW", "W", "NW"
            int ndx = ListTools.indexOfIgnoreCase(COMPASS_HEADING, headingStr);
            if (ndx >= 0) {
                return (double)ndx * COMPASS_INCREMENT;
            } else {
                return 0.0; // assume North
            }
        } else {
            // assume "45", "276", ...
            return StringTools.parseDouble(headingStr, 0.0); // degrees
        }
    }

    // ------------------------------------------------------------------------

    /* send plain text response */
    private void plainTextResponse(HttpServletResponse response, String errMsg)
        throws ServletException, IOException
    {
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
        PrintWriter out = response.getWriter();
        out.println(errMsg);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // message logging
   
    private static void logDebug(String msg)
    {
        Print.logDebug(LOG_NAME + ": " + msg);
    }

    private static void logInfo(String msg)
    {
        Print.logInfo(LOG_NAME + ": " + msg);
    }

    private static void logWarn(String msg)
    {
        Print.logWarn(LOG_NAME + ": " + msg);
    }

    private static void logError(String URL, String msg)
    {
        if (URL != null) {
            Print.logError(LOG_NAME + ": " + URL);
        }
        Print.logError(LOG_NAME + ": " + msg);
    }

    private static void logException(String URL, String msg, Throwable th)
    {
        if (URL != null) {
            Print.logError(LOG_NAME + ": " + URL);
        }
        Print.logException(LOG_NAME + ": " + msg, th);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
