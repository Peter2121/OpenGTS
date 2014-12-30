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
//  2007/03/25  Martin D. Flynn
//     -Initial release
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2007/11/28  Martin D. Flynn
//     -Added start 'address' to go with start geoPoint
//     -Added stop geopoint/address to available report fields
//  2008/03/28  Martin D. Flynn
//     -Added limited reporting support for devices that do not support OpenDMTP.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/06/20  Martin D. Flynn
//     -Added support for displaying a report 'map'.
//  2009/01/01  Martin D. Flynn
//     -Added totals for drive/idle time and distance driven.
//     -Added 'minimumStoppedTime' property (for simulated start/stop events only).
//     -Added 'hasStartStopCodes' property to force simulated start/stop events.
//  2009/05/01  Martin D. Flynn
//     -Added support for "idle" elapsed time (ignition on and not moving).
//  2009/08/07  Martin D. Flynn
//     -Changed 'hasStartStopCode' to 'tripStartType'
//  2009/11/01  Martin D. Flynn
//     -Added property 'stopOnIgnitionOff'
//  2010/05/24  Martin D. Flynn
//     -Added idle accumulation to TRIP_ON_SPEED
//  2012/04/03  Martin D. Flynn
//     -Added check for valid odometer (use previous valid odometer if current
//      odometer is not valid).  See "lastValidOdometerKM"
//     -Added TRIP_ON_ENGINE (still being tested)
//  2013/08/06  Martin D. Flynn
//     -Added check for Device specified WorkHours.
//     -Fixed idle-time stop when ignition-off and "stopOnIgnitionOff" is true
//  2014/01/01  Martin D. Flynn
//     -Added Fleet Detail group report support.
//  2014/09/16  Martin D. Flynn
//     -Added column sort feature (see PROP_fleetSortByField)
//     -Apply offset to start/stop odometer values (in "_addRecord" method). [2.5.7-B28]
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class MotionReport
    extends ReportData
    implements DBRecordHandler<EventData>
{

    // ------------------------------------------------------------------------
    // Detail report
    // Multiple FieldData records per device
    // 'From'/'To' date
    // ------------------------------------------------------------------------
    // Columns:
    //   index startDateTime movingElapse stopDateTime idleElapse
    // ------------------------------------------------------------------------
    // It would be helpful if the following items were available from the device:
    //  - "minimumStoppedTime"
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_alwaysReadAllEvents    = "alwaysReadAllEvents";
    private static final String PROP_isFleetSummaryReport   = "isFleetSummaryReport";
    private static final String PROP_fleetSortByField       = "fleetSortByField";
    private static final String PROP_showMapLink            = "showMapLink";
    private static final String PROP_tripStartType          = "tripStartType";
    private static final String PROP_minimumStoppedTime     = "minimumStoppedTime";
    private static final String PROP_minimumSpeedKPH        = "minimumSpeedKPH";
    private static final String PROP_stopOnIgnitionOff      = "stopOnIgnitionOff";
    private static final String PROP_tabulateByWorkHours    = "tabulateByWorkHours";
    private static final String PROP_WorkHours_             = "WorkHours.";

    // ------------------------------------------------------------------------
    // Trip start types
    
    private static final String MOTION_DEFAULT[]            = new String[] { "default"  };
    private static final String MOTION_SPEED[]              = new String[] { "speed"    , "motion" };
    private static final String MOTION_IGNITION[]           = new String[] { "ignition" };
    private static final String MOTION_ENGINE[]             = new String[] { "engine"   };
    private static final String MOTION_STARTSTOP[]          = new String[] { "start"    , "startstop" };

    private static final int    TRIP_ON_SPEED               = 0; // idle time if ignition present
    private static final int    TRIP_ON_IGNITION            = 1; // no idle time
    private static final int    TRIP_ON_ENGINE              = 2; // no idle time
    private static final int    TRIP_ON_START               = 3; // idle time if ignition present
    
    private static String TripTypeName(int type)
    {
        switch (type) {
            case TRIP_ON_SPEED      : return "Speed";
            case TRIP_ON_IGNITION   : return "Ignition";
            case TRIP_ON_ENGINE     : return "Engine";
            case TRIP_ON_START      : return "Start/Stop";
            default                 : return "Unknown";
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** True to show map link, false otherwise
    **/
    private static final boolean SHOW_MAP_LINK              = true;
    /** TRIP_ON_SPEED only
    *** Minimum speed used for determining in-motion when the device does not
    *** support start/stop events
    **/
    private static final double  MIN_SPEED_KPH              = 5.0;

    /** TRIP_ON_SPEED only
    *** Default mimimum stopped elapsed time to be considered stopped
    **/
    private static final long    MIN_STOPPED_TIME_SEC       = DateTime.MinuteSeconds(5);

    /**
    *** Default to delimit stop with ignition off (if this occurs before the minimum stopped time)
    **/
    private static final boolean STOP_ON_IGNITION_OFF       = false;

    /**
    *** Default to tabulate driving time/distance by work hours
    **/
    private static final boolean TABULATE_BY_WORK_HOURS     = false;

    // ------------------------------------------------------------------------

    // During TRIP_ON_SPEED trip delimiters, set this value to 'true' to reset the
    // elapsed stop time accumulation to start at the point of the defined 'stop'
    // which is after the minimum elapsed stopped time has passed.  This does cause
    // some user confustion, so if the above is unclear, leave this value 'false'.
    private static final boolean SPEED_RESET_STOP_TIME      = false;

    // ------------------------------------------------------------------------

    private static final int    STATE_UNKNOWN               = 0;
    private static final int    STATE_START                 = 1;
    private static final int    STATE_STOP                  = 2;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    
    public static class DeviceDetailComparator
        implements Comparator<FieldData>
    {
        private String fieldName = "";
        public DeviceDetailComparator(String fieldName) {
            this.fieldName = StringTools.trim(fieldName);
        }
        public int compare(FieldData fd1, FieldData fd2) {
            Object v1 = (fd1 != null)? fd1.getValue(this.fieldName,null) : null;
            Object v2 = (fd2 != null)? fd2.getValue(this.fieldName,null) : null;
            if ((v1 == null) && (v2 == null)) {
                // -- both v1/v2 are null
                return 0; // equal
            } else
            if (v1 == null) {
                // -- v1==null, v2 is non-null
                return -1; // null < non-null
            } else
            if (v2 == null) {
                // -- v1 is non-null, v2==null
                return 1; // non-null > null
            } else {
                // -- both v1/v2 are non-null
                if (v1 instanceof String) {
                    // -- compare as Strings
                    return ((String)v1).compareTo(v2.toString());
                } else
                if (v1 instanceof Number) {
                    // -- compare as Doubles
                    double d1 = StringTools.parseDouble(v1,0.0);
                    double d2 = StringTools.parseDouble(v2,0.0);
                    return (d1 < d2)? -1 : 1;
                } else
                if (v1 instanceof Boolean) {
                    // -- compare as Booleans
                    boolean b1 = StringTools.parseBoolean(v1,false);
                    boolean b2 = StringTools.parseBoolean(v2,false);
                    if (b1 == b2) {
                        return 0;
                    } else {
                        return b1? 1 : -1; // true > false
                    }
                } else
                if (v1 instanceof GeoPoint) {
                    // -- all GeoPoints are equal
                    return 0; 
                } else {
                    // -- unrecognized type
                    return 0;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                         deviceCount         = 0;
    private long                        deviceEventIndex    = 0L;

    private boolean                     isFleetReport       = false;
    private String                      fleetSortByField    = null;
    private boolean                     fleetSortAscending  = true;
    private int                         fleetSortLimit      = -1;
    private boolean                     fleetSortTrim       = false;
    private boolean                     alwaysReadAllEvents = false;

    private boolean                     showMapLink         = true;

    private int                         tripStartType       = TRIP_ON_SPEED;
    private boolean                     tripTypeDefault     = true;

    private int                         ignitionCodes[]     = null;
    private boolean                     isIgnitionOn        = false;
    private EventData                   lastIgnitionEvent   = null;
    
    private EventData                   idleStartEvent      = null;
    private EventData                   idleStopEvent       = null;
    private long                        idleAccumulator     = 0L;       // seconds

    private boolean                     isInMotion          = false;
    private EventData                   lastMotionEvent     = null;
    private EventData                   pendingStopEvent    = null;                 // TRIP_ON_SPEED only

    private double                      minSpeedKPH         = MIN_SPEED_KPH;        // TRIP_ON_SPEED only
    private long                        minStoppedTimeSec   = MIN_STOPPED_TIME_SEC; // TRIP_ON_SPEED only
    private boolean                     stopOnIgnitionOff   = STOP_ON_IGNITION_OFF;
    private boolean                     tabulateByWorkHours = TABULATE_BY_WORK_HOURS;
    
    private TimeZone                    timeZone            = null;
    private WorkHours                   workHours           = null;

    private double                      lastValidOdometerKM = 0.0;
    private double                      lastValidOdomOfsKM  = 0.0;

    private int                         lastStateChange     = STATE_UNKNOWN;

    private long                        lastStartTime       = 0L;
    private GeoPoint                    lastStartPoint      = null;
    private String                      lastStartAddress    = "";
    private double                      lastStartOdometer   = 0.0;
    private double                      lastStartOdomOfs    = 0.0;
    private double                      lastStartFuelUsed   = 0.0;
    private double                      lastStartFuelLevel  = 0.0;
    private double                      lastStartFuelRemain = 0.0;

    private long                        lastStopTime        = 0L;
    private GeoPoint                    lastStopPoint       = null;
    private String                      lastStopAddress     = "";
    private double                      lastStopOdometer    = 0.0;
    private double                      lastStopOdomOfs     = 0.0;
    private double                      lastStopFuelUsed    = 0.0;
    private double                      lastStopFuelLevel   = 0.0;
    private double                      lastStopFuelRemain  = 0.0;

    private Vector<FieldData>           deviceDetailData    = null;
    private Vector<FieldData>           deviceTotalData     = null;
    private Vector<FieldData>           fleetTotalData      = null;

    /* device totals */
    private double                      totalOdomKM         = 0.0;
    private long                        totalDriveSec       = 0L;
    private double                      totalDriveFuel      = 0.0;
    private int                         totalStopCount      = 0;
    private long                        totalStopSec        = 0L;
    private long                        totalIdleSec        = 0L;
    private double                      totalIdleFuel       = 0.0;
    
    /* workhour totals */
    private double                      tworkOdomKM         = 0.0;
    private long                        tworkDriveSec       = 0L;
    private double                      tworkDriveFuel      = 0.0;
    private int                         tworkStopCount      = 0;
    private double                      tworkIdleFuel       = 0.0;

    // ------------------------------------------------------------------------

    /**
    *** Motion Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public MotionReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);

        /* Account check */
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }

        /* Device check */
        this.deviceCount = this.getDeviceCount();
        if (this.deviceCount <= 0) {
            throw new ReportException("No Devices specified");
        }
        // Detail  report if "isFleetSummaryReport" is false (device count == 1)
        // Summary report if "isFleetSummaryReport" is true  (device count  > 1)

        /* Timezone */
        this.timeZone = reqState.getTimeZone(); // not null

    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {

        /* properties */
        RTProperties rtp = this.getProperties();
        this.alwaysReadAllEvents = rtp.getBoolean(PROP_alwaysReadAllEvents , false);
        this.isFleetReport       = rtp.getBoolean(PROP_isFleetSummaryReport, false);
        this.showMapLink         = rtp.getBoolean(PROP_showMapLink         , SHOW_MAP_LINK);
        this.minSpeedKPH         = rtp.getDouble( PROP_minimumSpeedKPH     , MIN_SPEED_KPH);
        this.minStoppedTimeSec   = rtp.getLong(   PROP_minimumStoppedTime  , MIN_STOPPED_TIME_SEC);
        this.stopOnIgnitionOff   = rtp.getBoolean(PROP_stopOnIgnitionOff   , STOP_ON_IGNITION_OFF);

        /* fieldSortByField ascending/descending */
        String _fleetSortByField = rtp.getString( PROP_fleetSortByField    , "");
        if (this.isFleetReport && !StringTools.isBlank(_fleetSortByField)) {
            // -- "fleetSortByField" specified:
            // -    "idleHours"
            // -    "idleHours,[desc|asc],{LIMIT}"
            String sbf[] = StringTools.split(_fleetSortByField.trim(),',');
            this.fleetSortByField   = (sbf.length > 0)? StringTools.trim(sbf[0])                 : null;
            this.fleetSortAscending = (sbf.length > 1)? !sbf[1].toLowerCase().startsWith("desc") : true;
            this.fleetSortLimit     = (sbf.length > 2)? StringTools.parseInt(sbf[2],-1)          : -1;
            this.fleetSortTrim      = (sbf.length > 3)? sbf[3].toLowerCase().startsWith("trim")  : false;
        }

        /* default work hours */
        this.tabulateByWorkHours = rtp.getBoolean(PROP_tabulateByWorkHours , TABULATE_BY_WORK_HOURS);
        if (this.tabulateByWorkHours) {
            this.workHours = new WorkHours(this.getProperties(), PROP_WorkHours_);
            //Print.logInfo("WorkHours:\n" + this.workHours);
        } else {
            //Print.logInfo("Not tabulating by work hours");
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return this.isFleetReport? false : true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Override 'getEventData' to reset selected status codes
    *** @param device       The Device for which EventData records will be selected
    *** @param rcdHandler   The DBRecordHandler
    *** @return An array of EventData records for the device
    **/
    protected EventData[] getEventData(Device device, DBRecordHandler<EventData> rcdHandler)
    {

        /* Device */
        if (device == null) {
            return EventData.EMPTY_ARRAY;
        }

        /* report constraints */
        ReportConstraints rc = this.getReportConstraints();

        /* adjust report constraints */
        if (this.alwaysReadAllEvents) {
            // debug purposes, should be "false" for production
            // return all status codes
            //Print.logInfo("Reading all events ...");
            rc.setStatusCodes(null);
            rc.setValidGPSRequired(false);
        } else
        if (this.tripStartType == TRIP_ON_START) {
            // return only start/stop events
            if (this.ignitionCodes != null) {
                //Print.logInfo("Reading motion start/stop & ignition events ...");
                rc.setStatusCodes(new int[] {
                    StatusCodes.STATUS_MOTION_START,
                    StatusCodes.STATUS_MOTION_STOP,
                    this.ignitionCodes[0],              // ignition OFF
                    this.ignitionCodes[1]               // ignition ON
                });
            } else {
                //Print.logInfo("Reading motion start/stop events ...");
                rc.setStatusCodes(new int[] {
                    StatusCodes.STATUS_MOTION_START,
                    StatusCodes.STATUS_MOTION_STOP
                });
            }
            rc.setValidGPSRequired(false); // don't need just valid gps events
        } else
        if (this.tripStartType == TRIP_ON_IGNITION) {
            // return only IgnitionOn/IgnitionOff events (this.ignitionCodes is non-null)
            //Print.logInfo("Reading ignition events ...");
            rc.setStatusCodes(new int[] {
                this.ignitionCodes[0],                  // ignition OFF
                this.ignitionCodes[1]                   // ignition ON
            });
            rc.setValidGPSRequired(false); // don't need just valid gps events
        } else
        if (this.tripStartType == TRIP_ON_ENGINE) {
            // return only EngineStart/EngineStop events
            if (this.ignitionCodes != null) {
                //Print.logInfo("Reading engine start/stop & ignition events ...");
                rc.setStatusCodes(new int[] {
                    StatusCodes.STATUS_ENGINE_STOP,
                    StatusCodes.STATUS_ENGINE_START,
                    this.ignitionCodes[0],              // ignition OFF
                    this.ignitionCodes[1]               // ignition ON
                });
            } else {
                //Print.logInfo("Reading engine start/stop events ...");
                rc.setStatusCodes(new int[] {
                    StatusCodes.STATUS_ENGINE_STOP,
                    StatusCodes.STATUS_ENGINE_START
                });
            }
            rc.setValidGPSRequired(false); // don't need just valid gps events
        } else {
            // TRIP_ON_SPEED
            // return all status codes
            //Print.logInfo("Reading all (speed) events ...");
            rc.setStatusCodes(null);
            rc.setValidGPSRequired((this.ignitionCodes == null)? true : false); // GPS only if no ignition codes
        }

        /* report selection limits */
        long rptLimit = rc.getReportLimit();
        if (rptLimit > 0L) {
            rc.setSelectionLimit(Math.max(rc.getSelectionLimit(), (rptLimit * 4L)));
        }

        /* get data */
        return super.getEventData(device, rcdHandler);

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay()
    {
        return this.showMapLink;
    }

    /**
    *** Returns true if this report supports displaying KML
    *** @return True if this report supports displaying KML, false otherwise
    **/
    public boolean getSupportsKmlDisplay()
    {
        return this.hasReportColumn(FieldLayout.DATA_STOP_GEOPOINT);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public static ReportLayout GetReportLayout()
    {
        // bind the report format to this data
        return FieldLayout.getReportLayout();
    }

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return GetReportLayout();
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {


        /* total totals */
        double grandTotalOdomKM    = 0.0;
        long   grandTotalDriveSec  = 0L;
        double grandTotalDriveFuel = 0.0;
        int    grandTotalStopCount = 0;
        long   grandTotalStopSec   = 0L;
        long   grandTotalIdleSec   = 0L;
        double grandTotalIdleFuel  = 0.0;
        // Work Hours
        double gworkTotalOdomKM    = 0.0;
        long   gworkTotalDriveSec  = 0L;
        // After Hours
        double gafterTotalOdomKM   = 0.0;
        long   gafterTotalDriveSec = 0L;

        /* device total data list */
        this.deviceTotalData = new Vector<FieldData>();

        /* device list */
        Account account   = this.getAccount();
        String  accountID = account.getAccountID();
        ReportDeviceList devList = this.getReportDeviceList();

        /* loop through devices */
        deviceListIterator:
        for (Iterator i = devList.iterator(); i.hasNext();) {
            String devID = (String)i.next();
            //Print.logInfo("Processing events for device: " + devID);

            /* init detail data iterator */
            this.deviceDetailData    = new Vector<FieldData>();

            /* reset device totals */
            this.totalOdomKM         = 0.0;
            this.totalDriveSec       = 0L ;
            this.totalDriveFuel      = 0.0;
            this.totalStopCount      = 0;
            this.totalStopSec        = 0L ;
            this.totalIdleSec        = 0L ;
            this.totalIdleFuel       = 0.0;
            this.tworkOdomKM         = 0.0;
            this.tworkDriveSec       = 0L;
            this.tworkDriveFuel      = 0.0;
            this.tworkStopCount      = 0;
            this.tworkIdleFuel       = 0.0;

            // reset ignition state
            this.isIgnitionOn        = false;
            this.lastIgnitionEvent   = null;
            this.ignitionCodes       = null;
            // reset idle state
            this.idleStartEvent      = null;
            this.idleStopEvent       = null;
            this.idleAccumulator     = 0L;
            // reset motion
            this.isInMotion          = false;
            this.lastMotionEvent     = null;
            // reset start
            this.lastStartTime       = 0L;
            this.lastStartPoint      = null;
            this.lastStartAddress    = "";
            this.lastStartOdometer   = 0.0;
            this.lastStartOdomOfs    = 0.0;
            this.lastStartFuelUsed   = 0.0;
            this.lastStartFuelLevel  = 0.0;
            this.lastStartFuelRemain = 0.0;
            // reset stop
            this.lastStopTime        = 0L;
            this.lastStopPoint       = null;
            this.lastStopAddress     = "";
            this.lastStopOdometer    = 0.0;
            this.lastStopOdomOfs     = 0.0;
            this.lastStopFuelUsed    = 0.0;
            this.lastStopFuelLevel   = 0.0;
            this.lastStopFuelRemain  = 0.0;
            // reset state
            this.lastStateChange     = STATE_UNKNOWN;
            // reset last valid odometer
            this.lastValidOdometerKM = 0.0;
            this.lastValidOdomOfsKM  = 0.0;

            try {

                /* get device */
                Device device = devList.getDevice(devID);
                if (device == null) {
                    continue; // deviceListIterator
                }

                // Device ignition statusCodes
                this.ignitionCodes = device.getIgnitionStatusCodes();
                boolean hasIgnition = (this.ignitionCodes != null);

                // -- trip start/stop type
                RTProperties rtp = this.getProperties();
                String tt = rtp.getString(PROP_tripStartType,MOTION_SPEED[0]).toLowerCase();
                //Print.logInfo("Trip type: " + tt);
                if (ListTools.contains(MOTION_DEFAULT,tt)) {
                    // -- "default" (TRIP_ON_ENGINE not selected when using "default")
                    String devCode = device.getDeviceCode();
                    DCServerConfig dcs = DCServerFactory.getServerConfig(devCode);
                    if ((dcs == null) && StringTools.isBlank(devCode) && Account.IsDemoAccount(accountID)) {
                        // -- special case for "demo" account when 'deviceCode' is blank
                        dcs = DCServerFactory.getServerConfig(DCServerFactory.OPENDMTP_NAME);
                        if (dcs == null) {
                            Print.logWarn("Account 'demo' DCServerConfig not found: " + DCServerFactory.OPENDMTP_NAME);
                        }
                    }
                    if (dcs != null) {
                        // -- DCServerConfig found
                        if (dcs.getStartStopSupported(false)) {
                            // Device supports start/stop
                            this.tripStartType = TRIP_ON_START;
                        } else
                        if (hasIgnition) {
                            // -- Device supports ignition state
                            this.tripStartType = TRIP_ON_IGNITION;
                        } else {
                            // -- Default to speed
                            this.tripStartType = TRIP_ON_SPEED;
                        }
                    } else {
                        // -- DCServerConfig not found ('deviceCode' is either blank or invalid)
                        if (hasIgnition) {
                            // -- Device supports ignition state
                            this.tripStartType = TRIP_ON_IGNITION;
                        } else {
                            // -- Default
                            this.tripStartType = TRIP_ON_SPEED;
                        }
                    }
                    this.tripTypeDefault = true;
                } else
                if (ListTools.contains(MOTION_STARTSTOP,tt)) {
                    // "startstop"
                    this.tripStartType = TRIP_ON_START;
                    this.tripTypeDefault = false;
                } else
                if (ListTools.contains(MOTION_IGNITION,tt)/* && hasIgnition */) {
                    // "ignition"
                    this.tripStartType   = TRIP_ON_IGNITION;
                    this.tripTypeDefault = false;
                    if (!hasIgnition) {
                        this.ignitionCodes = new int[] { StatusCodes.STATUS_IGNITION_OFF, StatusCodes.STATUS_IGNITION_ON };
                        hasIgnition = true;
                    }
                } else
                if (ListTools.contains(MOTION_ENGINE,tt)) {
                    // "ignition"
                    this.tripStartType   = TRIP_ON_ENGINE;
                    this.tripTypeDefault = false;
                } else {
                    // "speed", "motion"
                    this.tripStartType   = TRIP_ON_SPEED;
                    this.tripTypeDefault = true;
                }

                /* debug */
                if (RTConfig.isDebugMode()) {
                    Print.logDebug("Trip Start Type: [" + this.tripStartType + "] " + TripTypeName(this.tripStartType));
                    if (hasIgnition) {
                        String ignOff = StatusCodes.GetHex(this.ignitionCodes[0]);
                        String ignOn  = StatusCodes.GetHex(this.ignitionCodes[1]);
                        Print.logDebug("Device Ignition Codes "+ignOff+":"+ignOn+" [" + accountID + "/" + devID + "]");
                    } else {
                        Print.logDebug("No defined Device ignition codes [" + accountID + "/" + devID + "]");
                    }
                }

                // get events
                // this.lastValidOdometerKM = 0.0; <-- already reset above
                this.deviceEventIndex = 0L; // provide an index to all events read
                this.getEventData(device, this); // <== callback to 'handleDBRecord'
                //Print.logInfo("Total Accumulated Idle Time: " + this.totalIdleSec + " seconds");

                // handle final record here
                if (this.lastStopTime > 0) {
                    // we are stopped
                    long   driveTime = (this.lastStartTime > 0L)? (this.lastStopTime     - this.lastStartTime    ) : -1L;
                    double driveDist = (this.lastStartTime > 0L)? (this.lastStopOdometer - this.lastStartOdometer) : -1.0; // kilometers
                    double fuelTrip  = (this.lastStartTime > 0L)? (this.lastStopFuelUsed - this.lastStartFuelUsed) : -1.0; // liter
                    double driveEcon = (fuelTrip > 0.0)? (driveDist / fuelTrip) : 0.0; // kilometers per liter
                    Device.FuelEconomyType driveEconType = Device.FuelEconomyType.FUEL_CONSUMED;
                    long   stopElaps = -1L;
                    long   idleElaps = (this.idleAccumulator > 0L)? this.idleAccumulator : -1L;
                    double fuelIdle  = -1.0;
                    this._addRecord(accountID, devID, device,
                        this.lastStartTime  , this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartOdomOfs, this.lastStartFuelUsed,
                        this.lastStopTime   , this.lastStopPoint , this.lastStopAddress , this.lastStopOdometer , this.lastStopOdomOfs , this.lastStopFuelUsed ,
                        driveTime, driveDist, fuelTrip, driveEcon, driveEconType,
                        stopElaps, idleElaps, fuelIdle);
                } else
                if (this.lastStartTime > 0) {
                    // we haven't stopped during the range of this report
                    long   driveTime = -1L;
                    double driveDist = -1.0; // kilometers
                    double fuelTrip  = -1.0; // liters
                    double driveEcon = -1.0; // kilometers per liter
                    Device.FuelEconomyType driveEconType = Device.FuelEconomyType.UNKNOWN;
                    long   stopElaps = -1L;
                    long   idleElaps = -1L;
                    double fuelIdle  = -1.0;
                    this._addRecord(accountID, devID, device,
                        this.lastStartTime  , this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartOdomOfs, this.lastStartFuelUsed,
                        -1L                 , null               , ""                   , -1.0                  , 0.0                  , -1.0                  ,
                        driveTime, driveDist, fuelTrip, driveEcon, driveEconType,
                        stopElaps, idleElaps, fuelIdle);
                }
                
                /* fuel economy */
                double driveEcon = (this.totalDriveFuel > 0.0)? (this.totalOdomKM / this.totalDriveFuel) : 0.0;
                Device.FuelEconomyType driveEconType = Device.FuelEconomyType.FUEL_CONSUMED;

                /* device total record */
                FieldData fd = new FieldData();
                fd.setRowType(DBDataRow.RowType.TOTAL);
                long   idleElaps = (this.totalIdleSec > 0L)? this.totalIdleSec : -1L;
                fd.setAccount(account);
                fd.setDevice(device);
                fd.setString(FieldLayout.DATA_ACCOUNT_ID        , this.getAccountID());
                fd.setString(FieldLayout.DATA_DEVICE_ID         , devID);
                fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA    , this.totalOdomKM); // odomDelta
                fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED   , this.totalDriveSec);
                fd.setDouble(FieldLayout.DATA_FUEL_TRIP         , this.totalDriveFuel);
                fd.setDouble(FieldLayout.DATA_FUEL_ECONOMY      , driveEcon);
                fd.setValue( FieldLayout.DATA_FUEL_ECONOMY_TYPE , driveEconType);
                fd.setLong(  FieldLayout.DATA_STOP_COUNT        , this.totalStopCount);
                fd.setLong(  FieldLayout.DATA_STOP_ELAPSED      , this.totalStopSec);
                fd.setLong(  FieldLayout.DATA_IDLE_ELAPSED      , idleElaps);
                fd.setDouble(FieldLayout.DATA_FUEL_IDLE         , this.totalIdleFuel);
                // Work Hours
                fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED_WH, this.tworkDriveSec);
                fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA_WH , this.tworkOdomKM);
                fd.setDouble(FieldLayout.DATA_FUEL_TRIP_WH      , this.tworkDriveFuel);
                fd.setDouble(FieldLayout.DATA_FUEL_IDLE_WH      , this.tworkIdleFuel);
                fd.setLong(  FieldLayout.DATA_STOP_COUNT_WH     , this.tworkStopCount);
                // After Hours
                fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED_AH, (this.totalDriveSec - this.tworkDriveSec));
                fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA_AH , (this.totalOdomKM - this.tworkOdomKM));
                this.deviceTotalData.add(fd);

                /* grand totals */
                grandTotalOdomKM    += this.totalOdomKM;
                grandTotalDriveSec  += this.totalDriveSec;
                grandTotalDriveFuel += this.totalDriveFuel;
                grandTotalStopCount += this.totalStopCount;
                grandTotalStopSec   += this.totalStopSec;
                grandTotalIdleSec   += this.totalIdleSec;
                grandTotalIdleFuel  += this.totalIdleFuel;
                // Work Hours
                gworkTotalOdomKM    += this.tworkOdomKM;
                gworkTotalDriveSec  += this.tworkDriveSec;
                // After Hours
                gafterTotalOdomKM   += (this.totalOdomKM - this.tworkOdomKM);
                gafterTotalDriveSec += (this.totalDriveSec - this.tworkDriveSec);

            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
            }

        } // Device list iterator

        /* return row iterator */
        if (this.isFleetReport) {
            // -- prepare fleet-total data
            double avgEcon = (grandTotalDriveFuel > 0.0)? (grandTotalOdomKM / grandTotalDriveFuel) : 0.0;
            FieldData fd = new FieldData();
            fd.setRowType(DBDataRow.RowType.TOTAL);
            fd.setAccount(account);
            fd.setString(FieldLayout.DATA_ACCOUNT_ID         , this.getAccountID());
            fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA     , grandTotalOdomKM); // odomDelta
            fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED    , grandTotalDriveSec);
            fd.setDouble(FieldLayout.DATA_FUEL_TRIP          , grandTotalDriveFuel);
          //fd.setDouble(FieldLayout.DATA_FUEL_ECONOMY       , avgEcon);
          //fd.setValue( FieldLayout.DATA_FUEL_ECONOMY_TYPE  , avgEconType);
            fd.setLong(  FieldLayout.DATA_STOP_COUNT         , grandTotalStopCount);
            fd.setLong(  FieldLayout.DATA_STOP_ELAPSED       , grandTotalStopSec);
            fd.setLong(  FieldLayout.DATA_IDLE_ELAPSED       , grandTotalIdleSec);
            fd.setDouble(FieldLayout.DATA_FUEL_IDLE          , grandTotalIdleFuel);
            // -- Work Hours
            fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA_WH  , gworkTotalOdomKM);
            fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED_WH , gworkTotalDriveSec);
            // -- After Hours
            fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA_AH  , gafterTotalOdomKM);
            fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED_AH , gafterTotalDriveSec);
            // -- totals list
            this.fleetTotalData = new Vector<FieldData>();
            this.fleetTotalData.add(fd);
            // -- sort device-TOTAL data?
            if (!StringTools.isBlank(this.fleetSortByField)) {
                // -- sort data by field column
                DeviceDetailComparator ddc = new DeviceDetailComparator(this.fleetSortByField);
                ListTools.sort(this.deviceTotalData, ddc, this.fleetSortAscending);
                // -- trim excess items beyond limit
                if ((this.fleetSortLimit > 0) && (this.deviceTotalData.size() > this.fleetSortLimit)) {
                    // -- remove all entries above limit
                    this.deviceTotalData.setSize(this.fleetSortLimit);
                }
                // -- if descending, trim trailing items which are zero
                if (this.fleetSortTrim && !this.fleetSortAscending && (this.deviceTotalData.size() > 1)) {
                    //Print.logInfo("Trimming descending device total data ...");
                    for (int i = this.deviceTotalData.size() - 1; i > 0; i--) {
                        FieldData dfd = this.deviceTotalData.get(i);
                        Object fvl = (dfd != null)? dfd.getValue(this.fleetSortByField,null) : null;
                        //Print.logInfo("Testing value: " + fvl);
                        if ( (fvl == null                                                   ) ||
                            ((fvl instanceof String) && StringTools.isBlank((String)fvl)    ) ||
                            ((fvl instanceof Long  ) && (((Long)fvl).longValue() <= 0L)     ) ||
                            ((fvl instanceof Number) && (((Number)fvl).doubleValue() == 0.0))   ) {
                            this.deviceTotalData.remove(i);
                        }
                    }
                }
            }
            // -- return device-TOTAL data
            return new ListDataIterator(this.deviceTotalData);
        } else {
            // -- return device-DETAIL data
            return new ListDataIterator(this.deviceDetailData);
        }
        
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        if (this.isFleetReport) {
            if (this.fleetTotalData != null) {
                return new ListDataIterator(this.fleetTotalData);
            } else {
                return null;
            }
        } else {
            return new ListDataIterator(this.deviceTotalData);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Adds a record to the body database iterator
    *** @param startTime     The motion start time
    *** @param startGP       The motion start GeoPoint
    *** @param startAddress  The motion start Address
    *** @param startOdom     The motion start Odometer
    *** @param startFuel     The motion start Fuel Usage
    *** @param stopTime      The motion stop time
    *** @param stopGP        The motion stop GeoPoint
    *** @param stopAddress   The motion stop Address
    *** @param stopOdom      The motion stop Odometer
    *** @param stopFuel      The motion stop Fuel Usage
    *** @param driveTime     The driving elapsed time
    *** @param driveDist     The distance driven
    *** @param fuelTrip      The fuel used
    *** @param driveEcon     The fuel economy
    *** @param driveEconType The fuel economy type
    *** @param stopElapse    The elapsed stop time
    *** @param idleElapse    The elapsed idle time (ignition-on, not moving)
    *** @param fuelIdle      The fuel used while idling (<='0.0' if unavailable)
    **/
    private void _addRecord(String acctID, String devID, Device device,
        long startTime , GeoPoint startGP, String startAddress, double startOdom, double startOdomOfs, double startFuel,
        long stopTime  , GeoPoint stopGP , String stopAddress , double stopOdom , double stopOdomOfs , double stopFuel ,
        long driveTime , double driveDist, double fuelTrip    , double driveEcon, Device.FuelEconomyType driveEconType,
        long stopElapse, long idleElapse , double fuelIdle
        )
    {

        /* standard fields */
        FieldData fd = new MotionFieldData();
        fd.setString(  FieldLayout.DATA_ACCOUNT_ID       , acctID);
        fd.setString(  FieldLayout.DATA_DEVICE_ID        , devID);
        fd.setGeoPoint(FieldLayout.DATA_GEOPOINT         , startGP);         // may be null
        fd.setString(  FieldLayout.DATA_ADDRESS          , startAddress);    // may be null/blank
        fd.setLong(    FieldLayout.DATA_START_TIMESTAMP  , startTime);       // may be 0L
        fd.setLong(    FieldLayout.DATA_DRIVING_ELAPSED  , driveTime);
        fd.setDouble(  FieldLayout.DATA_ODOMETER         , startOdom);
        fd.setDouble(  FieldLayout.DATA_ODOMETER_DELTA   , driveDist);       // odomDelta
        fd.setLong(    FieldLayout.DATA_STOP_TIMESTAMP   , stopTime);
        fd.setGeoPoint(FieldLayout.DATA_STOP_GEOPOINT    , stopGP);          // may be null
        fd.setString(  FieldLayout.DATA_STOP_ADDRESS     , stopAddress);     // may be null/blank
        fd.setDouble(  FieldLayout.DATA_STOP_ODOMETER    , stopOdom);
        fd.setDouble(  FieldLayout.DATA_ODOMETER_OFFSET  , stopOdomOfs);     // use stop odometer offset
        fd.setDouble(  FieldLayout.DATA_FUEL_TOTAL       , startFuel);
        fd.setDouble(  FieldLayout.DATA_FUEL_TRIP        , fuelTrip);        // stopFuel - startFuel
        fd.setDouble(  FieldLayout.DATA_FUEL_ECONOMY     , driveEcon);       // driveDist / fuelTrip
        fd.setValue(   FieldLayout.DATA_FUEL_ECONOMY_TYPE, driveEconType);
        fd.setLong(    FieldLayout.DATA_STOP_ELAPSED     , stopElapse);
        fd.setLong(    FieldLayout.DATA_IDLE_ELAPSED     , idleElapse);
        fd.setDouble(  FieldLayout.DATA_FUEL_IDLE        , fuelIdle);

        /* work hours fields */
        boolean whStart     = false;
        boolean whStop      = false;
        long    whDriveTime = 0L;
        double  whDriveDist = 0.0;
        double  whFuelTrip  = 0.0;
        double  whFuelIdle  = 0.0;
        if (this.tabulateByWorkHours && (this.workHours != null)) {
            WorkHours WH = (device != null)? device.getWorkHours(this.workHours) : this.workHours; // not null
            whStart = (startTime > 0L)? WH.isMatch(startTime, this.timeZone) : false;
            whStop  = (stopTime  > 0L)? WH.isMatch(stopTime , this.timeZone) : false;
            //Print.logInfo("Checking WorkHours: " + (new DateTime(startTime)) + " - to - " + (new DateTime(stopTime)));
            if ((startTime <= 0L) && (stopTime <= 0L)) {
                // unlikely, ignore
            } else
            if ((startTime <= 0L) && (stopTime > 0L)) {
                // no start time
                boolean stopWH = WH.isMatch(stopTime , this.timeZone);
                if (stopWH) {
                    // all time/distance attributed to WorkHours
                    whDriveTime  = driveTime;
                    whDriveDist  = driveDist;
                    whFuelTrip   = fuelTrip;
                    whFuelIdle   = fuelIdle;
                } else {
                    // all time/distance attributed to non-WorkHours
                }
            } else
            if ((startTime > 0L) && (stopTime <= 0L)) {
                // no stop time
                boolean startWH = WH.isMatch(startTime, this.timeZone);
                if (startWH) {
                    // all time/distance attributed to WorkHours
                    whDriveTime = driveTime;
                    whDriveDist = driveDist;
                    whFuelTrip  = fuelTrip;
                    whFuelIdle  = fuelIdle;
                } else {
                    // all time/distance attributed to non-WorkHours
                }
            } else
            if (startTime < stopTime) {
                double totalHR   = (double)(stopTime - startTime) / 3600.0;
                DateTime startDT = new DateTime(startTime, this.timeZone);
                DateTime stopDT  = new DateTime(stopTime , this.timeZone);
                double accumHR = WH.countWorkHours(startDT, stopDT, this.timeZone);
                if (accumHR > 0.0) {
                    double fracHR = accumHR / totalHR;
                    if (fracHR > 1.0) {
                        // all time/distance attributed to WorkHours
                        whDriveTime = driveTime;
                        whDriveDist = driveDist;
                        whFuelTrip  = fuelTrip;
                        whFuelIdle  = fuelIdle;
                    } else {
                        whDriveTime = (long)(fracHR * (double)driveTime);
                        whDriveDist = fracHR * driveDist;
                        whFuelTrip  = fracHR * fuelTrip;
                        whFuelIdle  = fracHR * fuelIdle;
                    }
                }
            }
        }
        // Work Hours
        fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED_WH, whDriveTime);
        fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA_WH , whDriveDist);
        fd.setDouble(FieldLayout.DATA_FUEL_TRIP_WH      , whFuelTrip);
        fd.setDouble(FieldLayout.DATA_FUEL_IDLE_WH      , whFuelIdle);
        // After Hours
        fd.setLong(  FieldLayout.DATA_DRIVING_ELAPSED_AH, (driveTime - whDriveTime));
        fd.setDouble(FieldLayout.DATA_ODOMETER_DELTA_AH , (driveDist - whDriveDist));
        fd.setDouble(FieldLayout.DATA_FUEL_TRIP_AH      , (fuelTrip  - whFuelTrip ));
        fd.setDouble(FieldLayout.DATA_FUEL_IDLE_AH      , (fuelIdle  - whFuelIdle ));

        /* add to data iterator */
        this.deviceDetailData.add(fd);

        /* accumulate device totals */
        if (driveTime   >  0L) { this.totalDriveSec  += driveTime   ; }
        if (driveDist   > 0.0) { this.totalOdomKM    += driveDist   ; }
        if (fuelTrip    > 0.0) { this.totalDriveFuel += fuelTrip    ; }
        if (stopTime    >  0L) { this.totalStopCount += 1           ; }
        if (stopElapse  >  0L) { this.totalStopSec   += stopElapse  ; }
        if (idleElapse  >  0L) { this.totalIdleSec   += idleElapse  ; }
        if (fuelIdle    > 0.0) { this.totalIdleFuel  += fuelIdle    ; }

        /* accumulate device workhours */
        if (whDriveTime >  0L) { this.tworkDriveSec  += whDriveTime ; }
        if (whDriveDist > 0.0) { this.tworkOdomKM    += whDriveDist ; }
        if (whFuelTrip  > 0.0) { this.tworkDriveFuel += whFuelTrip  ; }
        if (whStop           ) { this.tworkStopCount += 1           ; }
        if (whFuelIdle  > 0.0) { this.tworkIdleFuel  += whFuelIdle  ; }

    }

    /**
    *** Custom DBRecord callback handler class
    *** @param rcd  The EventData record
    *** @return The returned status indicating whether to continue, or stop
    **/
    public int handleDBRecord(EventData rcd)
        throws DBException
    {
        EventData evRcd = rcd;
        Device device   = evRcd.getDevice(); // should be non-null
        int statusCode  = evRcd.getStatusCode();

        /* count event */
        this.deviceEventIndex++;
        //Print.logInfo("EventData["+this.deviceEventIndex+"]: " + evRcd.getTimestamp() + " 0x" + StringTools.toHexString(evRcd.getStatusCode(),16));

        /* ignition state change for non-ignition trips */
        boolean ignitionChange = false;
        if (this.tripStartType != TRIP_ON_IGNITION) {
            if (this.ignitionCodes != null) {
                // has ignition codes
                if (this.isIgnitionOff(statusCode)) {
                    // ignition OFF
                    if ((this.lastIgnitionEvent == null) || this.isIgnitionOn) {
                        ignitionChange         = true;
                        this.isIgnitionOn      = false;
                      //this.lastIgnOffEvent   = evRcd;
                        this.lastIgnitionEvent = evRcd;
                    } else {
                        // ignition is already off
                    }
                } else
                if (this.isIgnitionOn(statusCode)) {
                    // ignition ON
                    if ((this.lastIgnitionEvent == null) || !this.isIgnitionOn) {
                        ignitionChange         = true;
                        this.isIgnitionOn      = true;
                      //this.lastIgnOnEvent    = evRcd;
                        this.lastIgnitionEvent = evRcd;
                    } else {
                        // ignition is already on
                    }
                } else {
                    // leave ignition state as-is
                }
            } else {
                // no ignition codes
            }
        }

        /* trip delimiter */
        boolean isMotionStart = false;
        boolean isMotionStop  = false;
        boolean isIdleStart   = false;
        boolean isIdleStop    = false;
        if (this.tripStartType == TRIP_ON_IGNITION) {
            // TRIP_ON_IGNITION
            if (this.isIgnitionOn(statusCode)) {
                // I've started moving
                if ((this.lastIgnitionEvent == null) || !this.isIgnitionOn) {
                    // ignition was off, ignition state changed to on
                    ignitionChange              = true;
                    this.isIgnitionOn           = true;
                  //this.lastIgnOnEvent         = evRcd;
                    this.lastIgnitionEvent      = evRcd;
                    isMotionStart               = true;
                    this.isInMotion             = true;
                  //this.lastStartEvent         = evRcd;
                    this.lastMotionEvent        = evRcd;
                  //isIdleStop                  = true; <== no idle for TRIP_ON_IGNITION
                    this.idleStopEvent          = null;
                } else {
                    // ignition was already on
                }
            } else
            if (this.isIgnitionOff(statusCode)) {
                // I've stopped moving
                if ((this.lastIgnitionEvent == null) || this.isIgnitionOn) {
                    ignitionChange              = true;
                    this.isIgnitionOn           = false;
                  //this.lastIgnOffEvent        = evRcd;
                    this.lastIgnitionEvent      = evRcd;
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                  //isIdleStart                 = true;  <== no idle for TRIP_ON_IGNITION
                    this.idleStartEvent         = null;
                } else {
                    // ignition is already off
                }
            } else {
                // not a motion state change event
            }
        } else
        if (this.tripStartType == TRIP_ON_ENGINE) {
            // TRIP_ON_ENGINE
            if (this.isEngineStart(statusCode)) {
                // engine started
                if (!this.isInMotion) {
                    // I was stopped, I've now started moving (stop idle clock)
                    isMotionStart                   = true;
                    this.isInMotion                 = true;
                  //this.lastStartEvent             = evRcd;
                    this.lastMotionEvent            = evRcd;
                    if (!this.isIgnitionOn) {
                        // force ignition ON when engine on
                        this.isIgnitionOn           = true; 
                      //this.lastIgnOnEvent         = evRcd;
                        this.lastIgnitionEvent      = evRcd;
                    }
                    isIdleStop                      = true;     // in TRIP_ON_ENGINE
                    this.idleStopEvent              = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already moving
                }
            } else
            if (this.isEngineStop(statusCode)) {
                // engine stopped
                if (this.isInMotion) {
                    // I've stopped moving (start idle clock)
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                    if (this.isIgnitionOn && (this.ignitionCodes == null)) {
                        // force ignition off if device does not have ignition codes
                        this.isIgnitionOn       = false;
                      //this.lastIgnOffEvent    = evRcd
                        this.lastIgnitionEvent  = evRcd;
                    }
                    isIdleStart                 = true;     // in TRIP_ON_ENGINE
                    this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already stopped
                }
            } else
            if (this.isIgnitionOff(statusCode) && this.stopOnIgnitionOff) {
                // ignition off
                if (this.isInMotion) {
                    // Likely a "Stop" event was not found, force stop 
                    // I've stopped moving (start idle clock)
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                } else {
                    // I'm already stopped
                    //Print.logInfo("(Ignition Off) I'm already stopped");
                }
                // stop idle clock 
              //isIdleStart                 = true;     // in TRIP_ON_ENGINE
              //this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                //Print.logInfo("Idle Stop at index: " + this.deviceEventIndex);
                isIdleStop                  = true;     // in TRIP_ON_ENGINE
                this.idleStopEvent          = (this.ignitionCodes != null)? evRcd : null;
            } else {
                // not a motion state change event
                // check for idle change events while not moving
                if (!this.isInMotion) {
                    if (this.isIgnitionOn(statusCode)) {
                        // ignition on while not moving, start idle clock
                        isIdleStart             = true;     // in TRIP_ON_ENGINE
                        this.idleStartEvent     = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (this.isIgnitionOff(statusCode)) {
                        isIdleStop              = true;     // in TRIP_ON_ENGINE
                        this.idleStopEvent      = (this.ignitionCodes != null)? evRcd : null;
                    }
                }
            }
        } else
        if (this.tripStartType == TRIP_ON_START) {
            // TRIP_ON_START
            if (this.isMotionStart(statusCode)) {
                if (!this.isInMotion) {
                    // I was stopped, I've now started moving (stop idle clock)
                    isMotionStart                   = true;
                    this.isInMotion                 = true;
                  //this.lastStartEvent             = evRcd;
                    this.lastMotionEvent            = evRcd;
                    if (!this.isIgnitionOn) {
                        // force ignition ON when moving
                        this.isIgnitionOn           = true; 
                      //this.lastIgnOnEvent         = evRcd;
                        this.lastIgnitionEvent      = evRcd;
                    }
                    //Print.logInfo("Idle Stop at index: " + this.deviceEventIndex);
                    isIdleStop                      = true;     // in TRIP_ON_START
                    this.idleStopEvent              = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already moving
                    //Print.logInfo("(Start) I'm already moving");
                }
            } else
            if (this.isMotionStop(statusCode)) {
                if (this.isInMotion) {
                    // I've stopped moving (start idle clock)
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                    if (this.isIgnitionOn && (this.ignitionCodes == null)) {
                        // force ignition off if device does not have ignition codes
                        this.isIgnitionOn       = false;
                      //this.lastIgnOffEvent    = evRcd
                        this.lastIgnitionEvent  = evRcd;
                    }
                    //Print.logInfo("Idle Start at index: " + this.deviceEventIndex);
                    isIdleStart                 = true;     // in TRIP_ON_START
                    this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm already stopped
                    //Print.logInfo("(Stop) I'm already stopped");
                }
            } else
            if (this.isIgnitionOff(statusCode) && this.stopOnIgnitionOff) {
                // ignition off
                if (this.isInMotion) {
                    // Likely a "Stop" event was not found, force stop 
                    // I was moving, I've now stopped moving
                    isMotionStop                = true;
                    this.isInMotion             = false;
                  //this.lastStopEvent          = evRcd;
                    this.lastMotionEvent        = evRcd;
                } else {
                    // I'm already stopped
                    //Print.logInfo("(Ignition Off) I'm already stopped");
                }
                // stop idle clock 
                //isIdleStart                 = true;     // in TRIP_ON_START
                //this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                //Print.logInfo("Idle Stop at index: " + this.deviceEventIndex);
                isIdleStop              = true;     // in TRIP_ON_START
                this.idleStopEvent      = (this.ignitionCodes != null)? evRcd : null;
            } else {
                // not a motion state change event
                // check for idle change events while not moving
                if (!this.isInMotion) {
                    // not moving
                    if (this.isIgnitionOn(statusCode)) {
                        // ignition on while not moving, start idle clock
                        //Print.logInfo("Idle Start at index: " + this.deviceEventIndex);
                        isIdleStart             = true;     // in TRIP_ON_START
                        this.idleStartEvent     = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (this.isIgnitionOff(statusCode)) {
                        // ignition off while not moving, stop idle clock
                        //Print.logInfo("Idle Stop at index: " + this.deviceEventIndex);
                        isIdleStop              = true;     // in TRIP_ON_START
                        this.idleStopEvent      = (this.ignitionCodes != null)? evRcd : null;
                    } else {
                        // neighter ignition on or off
                    }
                } else {
                    // I'm still moving
                }
            }
        } else
        if (this.tripStartType == TRIP_ON_SPEED) {
            if (evRcd.getSpeedKPH() >= this.minSpeedKPH) {
                // I am moving
                this.pendingStopEvent           = null; // always reset (for min stop time below)
                if (!this.isInMotion) {
                    // I wasn't moving before, now I've started moving
                    isMotionStart               = true;
                    this.isInMotion             = true;
                  //this.lastStartEvent         = evRcd;
                    this.lastMotionEvent        = evRcd; // start of motion
                    if (this.isIgnitionOn) {
                        // ignition is already on.
                        //Print.logInfo("Start of motion (ignition is ON)");
                    } else {
                        // force ignition on (since were now moving)
                        //Print.logInfo("Start of motion (force ignition ON)");
                        this.isIgnitionOn       = true; 
                      //this.lastIgnOnEvent     = evRcd;
                        this.lastIgnitionEvent  = evRcd;
                    }
                    isIdleStop                  = true;     // in TRIP_ON_SPEED
                    this.idleStopEvent          = (this.ignitionCodes != null)? evRcd : null;
                } else {
                    // I'm still moving
                    if (ignitionChange) {
                        // ignition on/off while moving?
                    }
                }
            } else {
                // I am not moving
                if (this.isInMotion) {
                    // I was moving, now I've stopped moving - maybe
                    if (this.minStoppedTimeSec <= 0L) {
                        // no minimum stopped-time, and we haven't already stopped
                        //Print.logInfo("Stopped motion (no minimum stopped time)");
                        isMotionStop                = true;
                        this.isInMotion             = false;
                      //this.lastStopEvent          = evRcd;
                        this.lastMotionEvent        = evRcd; // stop motion
                        this.pendingStopEvent       = null;
                        isIdleStart                 = true;     // in TRIP_ON_SPEED
                        this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (ignitionChange && !this.isIgnitionOn && this.stopOnIgnitionOff) {
                        // ignition off while not moving, and we want to consider this as a stop
                        //Print.logInfo("Stopped motion (forced by ignition OFF)");
                        isMotionStop                = true;
                        this.isInMotion             = false;
                      //this.lastStopEvent          = evRcd;
                        this.lastMotionEvent        = (this.pendingStopEvent != null)? this.pendingStopEvent : evRcd; // stop motion
                        this.pendingStopEvent       = null;
                      //isIdleStart                 = true;     // in TRIP_ON_SPEED
                      //this.idleStartEvent         = (this.ignitionCodes != null)? evRcd : null;
                        isIdleStop                  = true;     // in TRIP_ON_SPEED
                        this.idleStopEvent          = (this.ignitionCodes != null)? evRcd : null;
                    } else {
                        // minimum stopped time in effect
                        if (this.pendingStopEvent == null) {
                            // start the stopped-time clock
                            this.pendingStopEvent   = evRcd;
                        } else {
                            // check to see if we've met the minimum stopped time
                            long deltaTimeSec = evRcd.getTimestamp() - this.pendingStopEvent.getTimestamp();
                            if (deltaTimeSec >= this.minStoppedTimeSec) {
                                // elapsed stop time exceeded limit
                                //Print.logInfo("Stopped motion (elapsed minimum stop time)");
                                isMotionStop         = true;
                                this.isInMotion      = false;
                              //this.lastStopEvent   = evRcd;
                                if (SPEED_RESET_STOP_TIME) {
                                    // if we reset the stop event here, then the minimum stopped time will
                                    // not be counted. (this does cause some user confusion, so this reset
                                    // should not occur).
                                    this.lastMotionEvent = evRcd; // stop motion
                                } else {
                                    this.lastMotionEvent = this.pendingStopEvent;
                                }
                                this.pendingStopEvent    = null;
                                isIdleStart              = true;     // in TRIP_ON_SPEED
                                this.idleStartEvent      = (this.ignitionCodes != null)? evRcd : null;
                            } else {
                                // assume I'm still moving (ie. temporarily stopped)
                            }
                        }
                    }
                } else {
                    // I'm still not moving
                    // check for idle change events while not moving
                    if (this.isIgnitionOn(statusCode)) {
                        // ignition on while not moving, start idle clock
                        isIdleStart             = true;     // in TRIP_ON_SPEED
                        this.idleStartEvent     = (this.ignitionCodes != null)? evRcd : null;
                    } else
                    if (this.isIgnitionOff(statusCode)) {
                        isIdleStop              = true;     // in TRIP_ON_SPEED
                        this.idleStopEvent      = (this.ignitionCodes != null)? evRcd : null;
                    }
                }
            }
        }
        // isMotionStart            - true if motion changed from stop==>start
        // isMotionStop             - true if motion changed from start==>stop
        // this.isInMotion          - current motion state
        // this.lastMotionEvent     - last motion delimiter event
        // this.lastIgnitionEvent   - last ignition delimiter event
        // isIdleStart              - true if idle changed from stop==>start
        // isIdleStop               - true if idle changed from start==>stop
        // this.idleStartEvent      - last idle start event
        // this.idleStopEvent       - last idle stop event
        // ignitionChange           - true if ignition changed state
        // this.isIgnitionOn        - current ignition state

        /* accrue idle time */
        if (this.ignitionCodes != null) {
            // 'idle' only valid if we have ignition codes
            if (isIdleStart) {
                // just wait for 'stop'
            } else
            if (isIdleStop) {
                // 'this.idleStopEvent' is non-null
                if (this.idleStartEvent != null) {
                    long idleTime = this.idleStopEvent.getTimestamp() - this.idleStartEvent.getTimestamp();
                    //Print.logInfo("["+this.deviceEventIndex+"] Added Idle Time: " + idleTime + " seconds");
                    this.idleAccumulator += idleTime;
                } else {
                    // 'this.idleStartEvent' not yet initialized (likely first occurance in report)
                }
                //Print.logInfo("Accumulated Idle time: " + this.idleAccumulator);
                this.idleStartEvent = null;
                this.idleStopEvent  = null;
            }
        }

        // lastStart -> lastStop -> start
        if (isMotionStart) {
            EventData ev = this.lastMotionEvent; // start of motion
            // 'this.isIgnitionOn' is 'true'

            if (this.lastStateChange == STATE_START) {
                // abnormal start ==> start
                // we already have a 'start', we're missing an interleaving  'stop'
                // the driving-time is not valid
                // ('this.lastStopTime' will already be '0' here, since we didn't get an interleaving 'stop')
                // ('this.lastStartTime' will be > 0 here, since we did get a previous 'start')
                // We treat this START event as a STOP event
                long     stopTime    = ev.getTimestamp();
                GeoPoint stopPoint   = ev.getGeoPoint();
                String   stopAddr    = ev.getAddress();
                double   stopOdom    = ev.getOdometerKM();
                double   stopOdomOfs = ev.getOdometerOffsetKM(null);
                if (stopOdom <= 0.0) { 
                    stopOdom = ev.getDistanceKM(); 
                    if (stopOdom <= 0.0) {
                        // we do not have a valid stop odometer, use last valid odometer
                        stopOdom    = this.lastValidOdometerKM;
                        stopOdomOfs = this.lastValidOdomOfsKM;
                        if (stopOdom <= 0.0) {
                            // we still do not have a valid odometer
                        }
                    }
                }
                double   stopFuel  = ev.getFieldValue(EventData.FLD_fuelTotal, 0.0);
                long     driveTime = (this.lastStartTime > 0L)? (stopTime  - this.lastStartTime)     : 0L;
                double   driveDist = (this.lastStartTime > 0L)? (stopOdom  - this.lastStartOdometer) : 0.0; // kilometers
                double   fuelTrip  = (this.lastStartTime > 0L)? (stopFuel  - this.lastStartFuelUsed) : 0.0; // liters
                double   driveEcon = (fuelTrip > 0.0)? (driveDist / fuelTrip) : -1.0; // kilometers per liter
                Device.FuelEconomyType driveEconType = Device.FuelEconomyType.FUEL_CONSUMED;
                long     stopElaps = 0L;
                long     idleElaps = 0L;
                double   fuelIdle  = -1.0;
                this._addRecord(ev.getAccountID(), ev.getDeviceID(), device,
                    this.lastStartTime  , this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartOdomOfs, this.lastStartFuelUsed,
                    stopTime            , stopPoint          , stopAddr             , stopOdom              , stopOdomOfs          , stopFuel              ,
                    driveTime, driveDist, fuelTrip, driveEcon, driveEconType,
                    stopElaps, idleElaps, fuelIdle);
                // continue with 'START'
            } else
            if (this.lastStopTime > 0) {
                // normal start --> stop ==> start
                long     driveTime = (this.lastStartTime > 0L)? (this.lastStopTime     - this.lastStartTime)     : 0L;
                double   driveDist = (this.lastStartTime > 0L)? (this.lastStopOdometer - this.lastStartOdometer) : -1.0; // kilometers
                double   fuelTrip  = (this.lastStartTime > 0L)? (this.lastStopFuelUsed - this.lastStartFuelUsed) : -1.0; // liters
                double   driveEcon = (fuelTrip > 0.0)? (driveDist / fuelTrip) : 0.0; // kilometers per liter
                Device.FuelEconomyType driveEconType = Device.FuelEconomyType.FUEL_CONSUMED;
                long     stopElaps = ev.getTimestamp() - this.lastStopTime;
                long     idleElaps = (this.idleAccumulator > 0L)? this.idleAccumulator : -1L;
                double   fuelIdle  = -1.0;
                this._addRecord(ev.getAccountID(), ev.getDeviceID(), device,
                    this.lastStartTime  , this.lastStartPoint, this.lastStartAddress, this.lastStartOdometer, this.lastStartOdomOfs, this.lastStartFuelUsed,
                    this.lastStopTime   , this.lastStopPoint , this.lastStopAddress , this.lastStopOdometer , this.lastStopOdomOfs , this.lastStopFuelUsed ,
                    driveTime, driveDist, fuelTrip, driveEcon, driveEconType,
                    stopElaps, idleElaps, fuelIdle);
            }

            this.lastStartTime      = ev.getTimestamp();
            this.lastStartPoint     = ev.getGeoPoint();
            this.lastStartAddress   = ev.getAddress();
            this.lastStartOdometer  = ev.getOdometerKM();
            this.lastStartOdomOfs   = ev.getOdometerOffsetKM(null);
            if (this.lastStartOdometer <= 0.0) { 
                this.lastStartOdometer = ev.getDistanceKM(); 
                if (this.lastStartOdometer <= 0.0) {
                    // we do not have a valid stop odometer, use last valid odometer
                    this.lastStartOdometer = this.lastValidOdometerKM;
                    this.lastStartOdomOfs  = this.lastValidOdomOfsKM;
                    if (this.lastStartOdometer <= 0.0) {
                        // we still do not have a valid odometer
                    }
                }
            }
            this.lastStartFuelUsed   = ev.getFieldValue(EventData.FLD_fuelTotal , 0.0);
            this.lastStartFuelLevel  = ev.getFieldValue(EventData.FLD_fuelLevel , 0.0);
            this.lastStartFuelRemain = ev.getFieldValue(EventData.FLD_fuelRemain, 0.0);

            this.lastStopTime        = 0L;
            this.lastStopPoint       = null;
            this.lastStopAddress     = null;
            this.lastStopOdometer    = 0.0;
            this.lastStopOdomOfs     = 0.0;
            this.lastStopFuelUsed    = 0.0;
            this.lastStopFuelLevel   = 0.0;
            this.lastStopFuelRemain  = 0.0;
            this.lastStateChange     = STATE_START;

            /* clear idle accrual */
            this.idleAccumulator     = 0L;

        } else
        if (isMotionStop) {
            EventData ev = this.lastMotionEvent; // stop motion

            if (this.lastStateChange == STATE_STOP) {
                // abnormal start --> stop ==> stop
                // we already have a 'stop', we're missing a 'start'.
                // this condition can only occur for TRIP_ON_START or TRIP_ON_IGNITION
                if ((this.lastStopTime > 0) && (this.lastIgnitionEvent != null) && (this.lastIgnitionEvent.getTimestamp() > this.lastStopTime)) {
                    // inject a START at the last ignition event (no additional idle accural calculations)
                    long     startTime    = this.lastIgnitionEvent.getTimestamp();
                    GeoPoint startPoint   = this.lastIgnitionEvent.getGeoPoint();
                    String   startAddr    = this.lastIgnitionEvent.getAddress();
                    double   startOdom    = this.lastIgnitionEvent.getOdometerKM();
                    double   startOdomOfs = this.lastIgnitionEvent.getOdometerOffsetKM(null);
                    if (startOdom <= 0.0) { 
                        startOdom = this.lastIgnitionEvent.getDistanceKM(); 
                        if (startOdom <= 0.0) {
                            // we do not have a valid stop odometer, use last valid odometer
                            startOdom    = this.lastValidOdometerKM;
                            startOdomOfs = this.lastValidOdomOfsKM;
                            if (startOdom <= 0.0) {
                                // we still do not have a valid odometer
                            }
                        }
                    }
                    double   startFuel = this.lastIgnitionEvent.getFieldValue(EventData.FLD_fuelTotal, 0.0);
                    long     driveTime = this.lastStopTime     - startTime;
                    double   driveDist = this.lastStopOdometer - startOdom; // kilometers
                    double   fuelTrip  = this.lastStopFuelUsed - startFuel; // liters
                    double   driveEcon = (fuelTrip > 0.0)? (driveDist / fuelTrip) : 0.0; // kilometers per liter
                    Device.FuelEconomyType driveEconType = Device.FuelEconomyType.FUEL_CONSUMED;
                    long     stopElaps = this.lastIgnitionEvent.getTimestamp() - this.lastStopTime;
                    long     idleElaps = (this.idleAccumulator > 0L)? this.idleAccumulator : -1L;
                    double   fuelIdle  = -1.0;
                    this._addRecord(ev.getAccountID(), ev.getDeviceID(), device,
                        startTime           , startPoint         , startAddr            , startOdom            , startOdomOfs        , startFuel             ,
                        this.lastStopTime   , this.lastStopPoint , this.lastStopAddress , this.lastStopOdometer, this.lastStopOdomOfs, this.lastStopFuelUsed ,
                        driveTime, driveDist, fuelTrip, driveEcon, driveEconType,
                        stopElaps, idleElaps, fuelIdle);
                    this.isIgnitionOn = true; // force to true, since we simulated a 'START'
                    // 'this.lastIgnitionEvent' stays as-is
                    // Continue with STOP
                } else {
                    // no interleaving ignition events
                    // ignore the previous 'STOP'
                }
            }

            this.lastStopTime       = ev.getTimestamp();
            this.lastStopPoint      = ev.getGeoPoint();
            this.lastStopAddress    = ev.getAddress();
            this.lastStopOdometer   = ev.getOdometerKM();
            this.lastStopOdomOfs    = ev.getOdometerOffsetKM(null);
            if (this.lastStopOdometer <= 0.0) { 
                this.lastStopOdometer = ev.getDistanceKM(); 
                if (this.lastStopOdometer <= 0.0) {
                    // we do not have a valid stop odometer, use last valid odometer
                    this.lastStopOdometer = this.lastValidOdometerKM;
                    this.lastStopOdomOfs  = this.lastValidOdomOfsKM;
                    if (this.lastStopOdometer <= 0.0) {
                        // we still do not have a valid odometer
                    }
                }
            }
            this.lastStopFuelUsed   = ev.getFieldValue(EventData.FLD_fuelTotal , 0.0);
            this.lastStopFuelLevel  = ev.getFieldValue(EventData.FLD_fuelLevel , 0.0);
            this.lastStopFuelRemain = ev.getFieldValue(EventData.FLD_fuelRemain, 0.0);
            this.lastStateChange    = STATE_STOP;

            /* start idle accrual */
            this.idleAccumulator    = 0L;

        }

        /* cache previous valid odometer */
        double thisEventOdometerKM = evRcd.getOdometerKM();
        double thisEventOdomOfsKM  = evRcd.getOdometerOffsetKM(null);
        if (thisEventOdometerKM > 0.0) {
            this.lastValidOdometerKM = thisEventOdometerKM;
            this.lastValidOdomOfsKM  = thisEventOdomOfsKM;
        } else {
            thisEventOdometerKM = evRcd.getDistanceKM();
            if (thisEventOdometerKM > 0.0) {
                this.lastValidOdometerKM = thisEventOdometerKM;
                this.lastValidOdomOfsKM  = thisEventOdomOfsKM;
            }
        }

        /* return record limit status */
        return (this.deviceDetailData.size() < this.getReportLimit())? DBRH_SKIP : DBRH_STOP;

    } // handleDBRecord

    // ------------------------------------------------------------------------

    private boolean isIgnitionOn(int statusCode)
    {
        if (this.ignitionCodes != null) {
            return (statusCode == this.ignitionCodes[1]);
        } else {
            return false;
        }
    }
    
    private boolean isIgnitionOff(int statusCode)
    {
        if (this.ignitionCodes != null) {
            return (statusCode == this.ignitionCodes[0]);
        } else {
            return false;
        }
    }

    private boolean isEngineStart(int statusCode)
    {
        return (statusCode == StatusCodes.STATUS_ENGINE_START);
    }
    
    private boolean isEngineStop(int statusCode)
    {
        return (statusCode == StatusCodes.STATUS_ENGINE_STOP);
    }

    private boolean isMotionStart(int statusCode)
    {
        return (statusCode == StatusCodes.STATUS_MOTION_START);
    }

    private boolean isMotionStop(int statusCode)
    {
        return (statusCode == StatusCodes.STATUS_MOTION_STOP);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom MotionFieldData class
    **/
    private static class MotionFieldData
        extends FieldData
        implements EventDataProvider
    {
        // Available fields:
        //   FieldLayout.DATA_ACCOUNT_ID        
        //   FieldLayout.DATA_DEVICE_ID         
        //   FieldLayout.DATA_GEOPOINT          
        //   FieldLayout.DATA_ADDRESS           
        //   FieldLayout.DATA_START_TIMESTAMP   
        //   FieldLayout.DATA_DRIVING_ELAPSED   
        //   FieldLayout.DATA_ODOMETER          
        //   FieldLayout.DATA_ODOMETER_DELTA    (odomDelta)
        //   FieldLayout.DATA_STOP_TIMESTAMP    
        //   FieldLayout.DATA_STOP_GEOPOINT     
        //   FieldLayout.DATA_STOP_ADDRESS      
        //   FieldLayout.DATA_STOP_ODOMETER     
        //   FieldLayout.DATA_FUEL_TOTAL        
        //   FieldLayout.DATA_FUEL_TRIP         
        //   FieldLayout.DATA_FUEL_ECONOMY      
        //   FieldLayout.DATA_FUEL_ECONOMY_TYPE 
        //   FieldLayout.DATA_IDLE_ELAPSED      
        public MotionFieldData() {
            super();
        }
        public String getAccountID() {
            return super.getString(FieldLayout.DATA_ACCOUNT_ID,"");
        }
        public String getDeviceID() {
            return super.getDeviceID();
        }
        public String getDeviceDescription() {
            return super.getDeviceDescription();
        }
        public String getDeviceVIN() {
            return super.getDeviceVIN();
        }
        public long getTimestamp() {
            return super.getLong(FieldLayout.DATA_STOP_TIMESTAMP, 0L);
        }
        public int getStatusCode() {
            return StatusCodes.STATUS_MOTION_STOP;
        }
        public String getStatusCodeDescription(BasicPrivateLabel bpl) {
            Device dev  = null;
            int    code = this.getStatusCode();
            return StatusCode.getDescription(dev, code, bpl, "Stop");
        }
        public StatusCodeProvider getStatusCodeProvider(BasicPrivateLabel bpl) {
            Device dev  = null;
            int    code = this.getStatusCode();
            return StatusCode.getStatusCodeProvider(dev, code, bpl, null/*dftSCP*/);
        }
        public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys, 
            boolean isFleet, BasicPrivateLabel bpl) {
            return EventData.ICON_PUSHPIN_RED;
        }
        public boolean isValidGeoPoint() {
            return GeoPoint.isValid(this.getLatitude(), this.getLongitude());
        }
        public double getLatitude() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_STOP_GEOPOINT, null);
            return (gp != null)? gp.getLatitude() : 0.0;
        }
        public double getLongitude() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_STOP_GEOPOINT, null);
            return (gp != null)? gp.getLongitude() : 0.0;
        }
        public GeoPoint getGeoPoint() {
            return new GeoPoint(this.getLatitude(), this.getLongitude());
        }
        public long getGpsAge() {
            return 0L; // not available
        }
        public long getCreationAge() {
            return 0L; // not available
        }
        public double getHorzAccuracy() {
            return -1.0; // not available
        }
        public GeoPoint getBestGeoPoint() {
            return this.getGeoPoint();
        }
        public double getBestAccuracy() {
            return this.getHorzAccuracy();
        }
        public int getSatelliteCount() {
            return 0;
        }
        public double getBatteryLevel() {
            return 0.0;
        }
        public double getSpeedKPH() {
            return 0.0;
        }
        public double getHeading() {
            return 0.0;
        }
        public double getAltitude() {
            return 0.0;
        }
        public double getOdometerKM() {
            return 0.0;
        }
        public String getGeozoneID() {
            return "";
        }
        public String getAddress() {
            return super.getString(FieldLayout.DATA_STOP_ADDRESS, "");
        }
        public long getInputMask() {
            return 0L;
        }
        public void setEventIndex(int ndx)
        {
            super.setInt(FieldLayout.DATA_EVENT_INDEX,ndx);
        }
        public int getEventIndex()
        {
            return super.getInt(FieldLayout.DATA_EVENT_INDEX,-1);
        }
        public boolean getIsFirstEvent()
        {
            return (this.getEventIndex() == 0);
        }
        public void setIsLastEvent(boolean isLast) {
            super.setBoolean(FieldLayout.DATA_LAST_EVENT,isLast);
        }
        public boolean getIsLastEvent() {
            return super.getBoolean(FieldLayout.DATA_LAST_EVENT,false);
        }
    }

}
