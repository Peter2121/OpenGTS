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
// Lantrix T1800/T2000 device communication server:
//  -Includes support to overriding default status code
//  -Includes support for simulated geozone arrive/depart events
// ----------------------------------------------------------------------------
// Change History:
//  2011/07/15  Martin D. Flynn
//     -Initial release.
//  2012/03/24  Mr. Gonzalez
//     -Support Lantrix T1800/T2000
//  2012/10/16  Martin D. Flynn
//     -Replaced "DCServerFactory.loadDeviceByPrefixedModemID" with "DCServerConfig.loadDeviceUniqueID".
// ----------------------------------------------------------------------------
package org.opengts.servers.lantrix;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    public static       boolean DEBUG_MODE                  = false;

    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = null;
    public static       double  MINIMUM_SPEED_KPH           = Constants.MINIMUM_SPEED_KPH;
    public static       boolean ESTIMATE_ODOMETER           = true;
    public static       boolean SIMEVENT_GEOZONES           = false;
    public static       boolean XLATE_LOCATON_INMOTION      = true;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;
    public static       boolean PACKET_LEN_END_OF_STREAM    = false;

    // ------------------------------------------------------------------------

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone               = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/* session IP address */
    private String          ipAddress                       = null;
    private int             clientPort                      = 0;

	/* count the number of events we've parsed during this session */
    private int             eventTotalCount                 = 0;

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();
    }

    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        if (PACKET_LEN_END_OF_STREAM) {
            return ServerSocketThread.PACKET_LEN_END_OF_STREAM;
        } else {
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (ListTools.isEmpty(pktBytes)) {
            Print.logWarn("Ignoring empty/null packet");
        } else
        if (pktBytes.length < 2) {
            Print.logError("Unexpected packet length: " + pktBytes.length);
        } else {
           	String s = StringTools.toStringValue(pktBytes).trim();
            Print.logInfo("Recv: " + s); // debug message
            Print.logInfo("Hex: 0x" + StringTools.toHexString(pktBytes)); // debug message			
            return this.parseInsertRecord_lantrix(s);			
        }
        return null; // no return packets are expected
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private byte[] parseInsertRecord_lantrix(String s)
    {   
        // ASCII Data:	
        // Example:
        // -------        	
		//>RGP190805211932-3457215-058493640000000FFBF0300;ID=8247;#2122;*54<CRLF
		//   R          = [ 0, 1] Response 
        //   GP         = [ 1, 3] Global Position 
		//  190805      = [ 3, 9] GPS time-of-day DDMMAA
		//  211932      = [ 9 , 15]  GPS time-of-hours GMT HHMMSS
		//  -3457215    = [ 15, 23] Latitude 
		//  -05849364   = [ 23, 32] Longitude
		//  000         = [ 32, 35] Speed (kph 0...999)
		//  000         = [ 35, 38] Heading (degrees 0...359)
		//  0           = [ 38, 39] GPS source 3 = Position 3D, 2 = Position 2D , 0= invalid
		//  FF          = [ 39, 41] Age of the data in Hexadecimal
		//  BF          = [ 41, 43] I/O Digital 
		//  03          = [ 43, 45]  Number of events generated by the report (decimal).
		//  00          = [ 45, 47] Horizontal Accuracy HDOP (0 .. 50)
		//  ;           = Separator
		//  ID=d8247    = Number Device ID 
		//  ;           = Separator
		//  #2122       = [ 57 , 62] Sentence Number (as generated by the mobile van from # 0000 to # 7FFF and those generated by the base go # 8000 to # FFFF)
		//  ;*54        = Checksum
		//  <           = End of message
		//  CRLF        = End of line and carriage advance 

        /* pre-validate */
        if (StringTools.isBlank(s)) {
            Print.logError("String is null/blank");
            return null;
        } else
        if (s.length() < 5) {
            Print.logError("String is invalid length");
            return null;
        } else
        if (!s.startsWith(">")) {
            Print.logError("String does not start with '>'");
            return null;
        }

        /* ends with "<"? */
        int se = s.endsWith("<")? (s.length() - 1) : s.length();
        s = s.substring(1,se);

        /* split */
        String T[] = StringTools.split(s,';');        

        /* RPG record */
        if (T[0].length() < 33) {
            Print.logError("Invalid 'RPG' data length");
            return null;
        }

        /* mobile id */
        String mobileID = null;
        for (int i = 1; i < T.length; i++) {
            if (T[i].startsWith("ID=")) {
                mobileID = T[i].substring(3);
                break;
            }
        }

		/* Sentence Number */
        String Sentence_number = null;
        for (int j = 1; j < T.length; j++) {
            if (T[j].startsWith("#")) {
                Sentence_number = T[j].substring(1);
                break;
            }
        }

        //Arming the ACK frame to remove the data sent from the tracker
	    String tracker_ID = mobileID;
		String frame_data_checksum = null;		
		frame_data_checksum = ">ACK;ID=" + tracker_ID + ";#" + Sentence_number + ";*";
		String chksum = getCheckSum(frame_data_checksum);
		String frame_data_ACK = null;			
		frame_data_ACK = frame_data_checksum  + chksum + "<\r\n";

		/* parse */ 	
        long    dmy       = StringTools.parseLong(T[0].substring( 3, 9), 0L);
        long    hms       = StringTools.parseLong(T[0].substring( 9, 15), 0L);        
        long    fixtime   = this._getUTCSeconds(dmy, hms);		
        double latitude   = (double)StringTools.parseLong(T[0].substring( 15,23),0L) / 100000.0;
        double longitude  = (double)StringTools.parseLong(T[0].substring(23,32),0L) / 100000.0;
		double KPH        = StringTools.parseDouble(T[0].substring(32,35), 0.0);
        double speedKPH   = KPH; 
        double headingDeg = StringTools.parseDouble(T[0].substring(35,38), 0.0);
		int    age_data   = Integer.parseInt(T[0].substring(39,41),16);
        String decimal    = Integer.toString(age_data);
        long   inout      = Long.parseLong(T[0].substring(41,43),16);
        String binary_io  = Long.toBinaryString(inout);
		String srcStr     = T[0].substring(38,39);
        String ageStr     = decimal;
        double altitudeM  = 0.0;
        double odomKM     = 0.0;
        long   gpioInput  = 0L;        
		int    statusCode = StatusCodes.STATUS_LOCATION;

        /* I/O Digital */
		gpioInput = inout; 

		/* get time */   
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date.");
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        } 

        /* lat/lon valid? */
        boolean validGPS = true;
        if (!GeoPoint.isValid(latitude,longitude)) {
            Print.logWarn("Invalid lat/lon: " + latitude + "/" + longitude);
            validGPS   = false;
            latitude   = 0.0;
            longitude  = 0.0;
            speedKPH   = 0.0;
            headingDeg = 0.0;
        }
        GeoPoint geoPoint = new GeoPoint(latitude,longitude);

        /* adjustments to received values */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) { 
            headingDeg = 0.0;
        }
        
        /* debug */		
        Print.logInfo("MobileID  : " + mobileID);
		Print.logInfo("Sentence  : " + Sentence_number);
        Print.logInfo("Timestamp: " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GeoPoint  : " + geoPoint);
        Print.logInfo("Speed km/h: " + speedKPH + " [" + headingDeg + "]");

        /* mobile-id */
        if (StringTools.isBlank(mobileID)) {
            Print.logError("Missing MobileID");
            return null;
        }

        /* find Device */
        String accountID = "";
        String deviceID  = "";
        String uniqueID  = "";
        //Device device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, mobileID);
        Device device = DCServerConfig.loadDeviceUniqueID(Main.getServerConfig(), mobileID);
        if (device == null) {
            return null; // errors already displayed
        } else {
            accountID = device.getAccountID();
            deviceID  = device.getDeviceID();
            uniqueID  = device.getUniqueID();
            Print.logInfo("UniqueID  : " + uniqueID);
            Print.logInfo("DeviceID  : " + accountID + "/" + deviceID);
        }
        
        /* check IP address */
        DataTransport dataXPort = device.getDataTransport();
        if ((this.ipAddress != null) && !dataXPort.isValidIPAddress(this.ipAddress)) {
            DTIPAddrList validIPAddr = dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.ipAddress + " [expecting " + validIPAddr + "]");
            return null;
        }
        dataXPort.setIpAddressCurrent(this.ipAddress);    // FLD_ipAddressCurrent
        dataXPort.setRemotePortCurrent(this.clientPort);  // FLD_remotePortCurrent
        dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!dataXPort.getDeviceCode().equalsIgnoreCase(Constants.DEVICE_CODE)) {
            dataXPort.setDeviceCode(Constants.DEVICE_CODE); // FLD_deviceCode
        }

        /* reject invalid GPS fixes? */
        if (!validGPS && (statusCode == StatusCodes.STATUS_LOCATION)) {
            // ignore invalid GPS fixes that have a simple 'STATUS_LOCATION' status code
            Print.logWarn("Ignoring event with invalid latitude/longitude");
            return null;
        }

        /* estimate GPS-based odometer */
        if (odomKM <= 0.0) {
            // calculate odometer
            odomKM = (ESTIMATE_ODOMETER && validGPS)? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            // bounds-check odometer
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdometerKM: " + odomKM);

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && validGPS) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    this.insertEventRecord(device, 
                        z.getTimestamp(), z.getStatusCode(), z.getGeozone(),
                        geoPoint, gpioInput, speedKPH, headingDeg, altitudeM, odomKM);
                    Print.logInfo("Geozone    : " + z);
                    if (z.getStatusCode() == statusCode) {
                        // suppress 'statusCode' event if we just added it here
                        Print.logDebug("StatusCode already inserted: 0x" + StatusCodes.GetHex(statusCode));
                        statusCode = StatusCodes.STATUS_IGNORE;
                    }
                }
            }
        }

        /* insert event */
        if (statusCode == StatusCodes.STATUS_NONE) {
            // ignore this event
        } else
        if ((statusCode != StatusCodes.STATUS_LOCATION) || !validGPS) {
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*GeoZone*/,
                geoPoint, gpioInput, speedKPH, headingDeg, altitudeM, odomKM);
        } else
        if (!device.isNearLastValidLocation(geoPoint,MINIMUM_MOVED_METERS)) {
            if ((statusCode == StatusCodes.STATUS_LOCATION) && (speedKPH > 0.0)) {
                statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
            }
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*GeoZone*/,
                geoPoint, gpioInput, speedKPH, headingDeg, altitudeM, odomKM);
        }
            
        /* save device changes */
        try {
            // TODO: check "this.device" vs "this.dataXPort"  
            device.updateChangedEventFields();
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
        } finally {
            //
        }
        Print.logInfo("ACK: " + frame_data_ACK);
		return frame_data_ACK.getBytes();// //return required acknowledgement (ACK) back to the device

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy    Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hms    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    ***/
    private long _getUTCSeconds(long dmy, long hms)
    {
    
        /* time of day [TOD] */
        int    HH  = (int)((hms / 10000L) % 100L);
        int    MM  = (int)((hms / 100L) % 100L);
        int    SS  = (int)(hms % 100L);
        long   TOD = (HH * 3600L) + (MM * 60L) + SS;
    
        /* current UTC day */
        long DAY;
        if (dmy > 0L) {
            int    yy  = (int)(dmy % 100L) + 2000;
            int    mm  = (int)((dmy / 100L) % 100L);
            int    dd  = (int)((dmy / 10000L) % 100L);
            long   yr  = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY        = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
        } else {
            // we don't have the day, so we need to figure out as close as we can what it should be.
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= TOD)? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (tod > TOD) {
                    // tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }
        
        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + TOD;
        return sec;
        
    }
    // ------------------------------------------------------------------------

    /**
    *** Parses latitude given values from GPS device.
    *** @param  s  Latitude String from GPS device in DDmm.mmmm format.
    *** @param  d  Latitude hemisphere, "N" for northern, "S" for southern.
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         90.0 if invalid latitude provided.
    **/
    private double _parseLatitude(String s, String d)
    {
        double _lat = StringTools.parseDouble(s, 99999.0);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equals("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in DDDmm.mmmm format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    *** 180.0 if invalid longitude provided.
    **/
    private double _parseLongitude(String s, String d)
    {
        double _lon = StringTools.parseDouble(s, 99999.0);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equals("W")? -lon : lon;
        } else {
            return 180.0; // invalid longitude
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Calculates/Returns the checksum for a TAIP (Trimble ASCII Interface Protocol) formatted String.
    *** Implements TAIP checksum algorithm.
    *** Using ASCII 7 bits 2^7 = 128 characters.
    *** @param msg TAIP protocol formatted String to be checksummed.
    *** @return Checksum computed from input.
    **/ 
    private static String getCheckSum(String msg)
    {
        byte y = 0;
        String chk = null;
        for (int x = 0; x < msg.length() - 1; x++) {		
            if (msg.charAt(x) == '*') { // Detect the asterisk    
                break;
            } else {
                y ^= (msg.charAt(x) & 0X7F); // Buffer 127 bit	        		
            }
        }
        chk = Integer.toHexString(y).toUpperCase();
        if (chk.length() == 1) {
            chk = "0" + chk;
        }		
        return chk;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private EventData createEventRecord(Device device, 
        long     fixtime,
        int      statusCode,
        GeoPoint geoPoint, 
        long     gpioInput,
        double   speedKPH, double heading, 
        double   altitude,
        double   odomKM)
    {
        String accountID    = (device != null)? device.getAccountID() : "";
        String deviceID     = (device != null)? device.getDeviceID()  : "";
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeoPoint(geoPoint);
        evdb.setInputMask(gpioInput);
        evdb.setHeading(heading);
        evdb.setSpeedKPH(speedKPH);
        evdb.setAltitude(altitude);
        evdb.setOdometerKM(odomKM);
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long     fixtime, int statusCode, Geozone geozone,
        GeoPoint geoPoint,                             
        long     gpioInput,
        double   speedKPH, double heading, 
        double   altitude,
        double   odomKM)
    {

        /* create event */
        EventData evdb = createEventRecord(device, fixtime, statusCode, geoPoint, gpioInput, speedKPH, heading, altitude, odomKM);

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event     : [0x" + StringTools.toHexString(statusCode,16) + "] " + StatusCodes.GetDescription(statusCode,null));
        if (device != null) {
            device.insertEventData(evdb);            
        }
        this.eventTotalCount++;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
   
    public static void configInit() 
    {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc != null) {
    
            /* common */
            UNIQUEID_PREFIX          = dcsc.getUniquePrefix();
            MINIMUM_SPEED_KPH        = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
            ESTIMATE_ODOMETER        = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
            SIMEVENT_GEOZONES        = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
            XLATE_LOCATON_INMOTION   = dcsc.getStatusLocationInMotion(XLATE_LOCATON_INMOTION);
            MINIMUM_MOVED_METERS     = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);

            /* custom */
            PACKET_LEN_END_OF_STREAM = dcsc.getBooleanProperty(Constants.CFG_packetLenEndOfStream, PACKET_LEN_END_OF_STREAM);

        }
        
    }

}
