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
//  Template data packet 'business' logic.
//  This module is an *example* of how client data packets can be parsed and 
//  inserted into the EventData table.  Since every Device protocol is different,
//  significant changes will likely be necessary to support the protocol used by
//  your chosen Device.
// ----------------------------------------------------------------------------
// Notes:
// - See the OpenGTS_Config.pdf document for additional information regarding the
//   implementation of a Device Communication Server.
// - Implementing a Device Communication Server for your chosen Device may take a 
//   signigicant and substantial amount of programming work to accomplish, depending 
//   on the Device protocol.  To implement a server, you will likely need an in-depth 
//   understanding of TCP/UDP based communication, and a good understanding of Java 
//   programming techniques, including socket communication and multi-threading. 
// - The first and most important step when starting to implement a Device 
//   communication server for your chosen Device is to obtain and fully understand  
//   the protocol documentation from the manufacturer of the Device.  Attempting to 
//   reverse-engineer a raw-socket base protocol can prove extremely difficult, if  
//   not impossible, without proper protocol documentation.
// ----------------------------------------------------------------------------
// Change History:
//  2006/06/30  Martin D. Flynn
//     -Initial release
//  2007/07/27  Martin D. Flynn
//     -Moved constant information to 'Constants.java'
//  2007/08/09  Martin D. Flynn
//     -Added additional help/comments.
//     -Now uses "imei_" as the primary IMEI prefix for the unique-id when
//      looking up the Device record (for data format example #1)
//     -Added a second data format example (#2) which includes the parsing of a
//      standard $GPRMC NMEA-0183 record.
//  2008/02/17  Martin D. Flynn
//     -Added additional help/comments.
//  2008/03/12  Martin D. Flynn
//     -Added ability to compute a GPS-based odometer value
//     -Added ability to update the Device record with IP-address and last connect time.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/06/20  Martin D. Flynn
//     -Added some additional comments regarding the use of 'terminate' and 'getTerminateSession()'.
//  2008/12/01  Martin D. Flynn
//     -Added entry point for parsing GPS packet data store in a flat file.
//  2009/04/02  Martin D. Flynn
//     -Changed default for 'INSERT_EVENT' to true
//  2009/05/27  Martin D. Flynn
//     -Added changes for estimated odometer calculations, and simulated geozones
//  2009/06/01  Martin D. Flynn
//     -Updated to utilize Device gerozone checks
//  2009/08/07  Martin D. Flynn
//     -Updated to use "DCServerConfig" and "GPSEvent"
//  2009/10/02  Martin D. Flynn
//     -Modified to describe how to return ACK packets back to the Device.
//     -Added parser for RTProperties String (format #3)
//  2011/01/28  Martin D. Flynn
//     -Moved RTProperty type format to #9
//     -Added an additional example format for #3
//  2011/07/15  Martin D. Flynn
//     -Removed references to local "this.isDuplex" var.
//  2011/10/03  Martin D. Flynn
//     -Include GeozoneID with Geozone arrive/depart events
//  2011/12/06  Martin D. Flynn
//     -Added additional record format support (see "parseInsertRecord_Device_1")
//  2012/04/03  Martin D. Flynn
//     -Modified "parseInsertRecord_ASCII_02" to allow using AccountID as MobileID.
//  2012/12/24  Martin D. Flynn
//     -Consolidated handling of "new GPSEvent(...)"
//  2013/05/28  Martin D. Flynn
//     -Fixed NPE in "createGPSEvent(mobileID)"
//     -Fixed "$POS,..." longitude parsing
//  2013/11/25  Martin D. Flynn
//     -Set "this.lastModemID" to fix recurring issue in "createGPSEvent" [B28]
//      (thanks to Marjan Sudic for this fix)
// ----------------------------------------------------------------------------
package org.opengts.servers.template;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.servers.*;

/**
*** <code>TrackClientPacketHandler</code> - This module contains the general
*** "business logic" for parsing incoming data packets from the remote tracking
*** Device.
**/

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------
    // This data parsing template contains *examples* of 2 different ASCII data formats:
    //
    // Format # 1: (see "parseInsertRecord_ASCII_01")
    //   <MobileID>,<YYYY/MM/DD>,<HH:MM:SS>,<Latitude>,<Longitude>,<Speed>,<Heading>
    //
    // Format # 2: (see "parseInsertRecord_ASCII_02")
    //   <AccountID>/<DeviceID>/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
    //   /<MobileID>/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
    //   <MobileID>// $GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
    //   <MobileID>/_mid_/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
    //
    // Format # 3: (see "parseInsertRecord_ASCII_03")
    //   <Seq>,<Code>,<MobileID>,<Format>,<YYYYMMDD>,<HHMMSS>,<GPSValid>,<HDOP>,<Lat>,<Lon>,<Heading>,<Speed>,<Alt>
    //
    // Format # 9: (see "parseInsertRecord_RTProps")
    //   mid=<MobileID> ts=<EpochTS> code=<EventCode> gps=<Lat>/<Lat> kph=<Speed> dir=<Heading> odom=<OdomKM>
    //
    // Format #11: (see "parseInsertRecord_Device_1")
    //   $<EventCode>,<MobileID>,<HHMMSS>,<GPSValid>,<NMEALat>,<N|S>,<NMEALon>,<E|W>,<SpeedKnots>,<Heading>,<DDMMYY>
    //
    // These are only *examples* of an ASCII encoded data protocol.  Since this 'template'
    // cannot anticipate every possible ASCII/Binary protocol that may be encounted, this
    // module should only be used as an *example* of how a Device Communication Server might
    // be implemented.  The implementation of a Device Communication Server for your chosen
    // Device may take a signigicant and substantial amount of programming work to accomplish, 
    // depending on the Device protocol.
    // ------------------------------------------------------------------------
    public  static int          DATA_FORMAT_OPTION          = 1;

    // ------------------------------------------------------------------------

    /* estimate GPS-based odometer */
    // (enable to include estimated GPS-based odometer values on EventData records)
    // Note:
    //  - Enabling this feature may cause an extra query to the EventData table to obtain
    //    the previous EventData record, from which it will calculate the distance between
    //    this prior point and the current point.  This means that the GPS "dithering",
    //    which can occur when a vehicle is stopped, will cause the calculated odometer 
    //    value to increase even when the vehicle is not moving.  You may wish to add some
    //    additional logic to mitigate this particular behavior.  
    //  - The accuracy of a GPS-based odometer calculation varies greatly depending on 
    //    factors such as the accuracy of the GPS receiver (ie. WAAS, DGPS, etc), the time
    //    interval between generated "in-motion" events, and how straight or curved the
    //    road is.  Typically, a GPS-based odometer tends to under-estimate the actual
    //    vehicle value.
    public  static       boolean ESTIMATE_ODOMETER          = false;
    
    /* simulate geozone arrival/departure */
    // (enable to insert simulated Geozone arrival/departure EventData records)
    public  static       boolean SIMEVENT_GEOZONES          = false;
    
    /* simulate digital input changes */
    public  static       long    SIMEVENT_DIGITAL_INPUTS    = 0x0000L; // 0xFFFFL;

    /* flag indicating whether data should be inserted into the DB */
    // should be set to 'true' for production.
    private static       boolean DFT_INSERT_EVENT           = true;
    private static       boolean INSERT_EVENT               = DFT_INSERT_EVENT;

    /* update Device record */
    // (enable to update Device record with current IP address and last connect time)
  //private static       boolean UPDATE_DEVICE              = false;
    
    /* minimum acceptable speed value */
    // Speeds below this value should be considered 'stopped'
    public  static       double  MINIMUM_SPEED_KPH          = 0.0;

    /* Knot/Kilometer conversions */
    public static final double  KILOMETERS_PER_KNOT     = 1.85200000;
    public static final double  KNOTS_PER_KILOMETER     = 1.0 / KILOMETERS_PER_KNOT;

    // ------------------------------------------------------------------------

    /* Ingore $GPRMC checksum? */
    // (only applicable for data formats that include NMEA-0183 formatted event records)
    private static       boolean IGNORE_NMEA_CHECKSUM       = false;

    // ------------------------------------------------------------------------

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone               = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------

    /* GTS status codes for Input-On events */
    private static final int InputStatusCodes_ON[] = new int[] {
        StatusCodes.STATUS_INPUT_ON_00,
        StatusCodes.STATUS_INPUT_ON_01,
        StatusCodes.STATUS_INPUT_ON_02,
        StatusCodes.STATUS_INPUT_ON_03,
        StatusCodes.STATUS_INPUT_ON_04,
        StatusCodes.STATUS_INPUT_ON_05,
        StatusCodes.STATUS_INPUT_ON_06,
        StatusCodes.STATUS_INPUT_ON_07,
        StatusCodes.STATUS_INPUT_ON_08,
        StatusCodes.STATUS_INPUT_ON_09,
        StatusCodes.STATUS_INPUT_ON_10,
        StatusCodes.STATUS_INPUT_ON_11,
        StatusCodes.STATUS_INPUT_ON_12,
        StatusCodes.STATUS_INPUT_ON_13,
        StatusCodes.STATUS_INPUT_ON_14,
        StatusCodes.STATUS_INPUT_ON_15
    };

    /* GTS status codes for Input-Off events */
    private static final int InputStatusCodes_OFF[] = new int[] {
        StatusCodes.STATUS_INPUT_OFF_00,
        StatusCodes.STATUS_INPUT_OFF_01,
        StatusCodes.STATUS_INPUT_OFF_02,
        StatusCodes.STATUS_INPUT_OFF_03,
        StatusCodes.STATUS_INPUT_OFF_04,
        StatusCodes.STATUS_INPUT_OFF_05,
        StatusCodes.STATUS_INPUT_OFF_06,
        StatusCodes.STATUS_INPUT_OFF_07,
        StatusCodes.STATUS_INPUT_OFF_08,
        StatusCodes.STATUS_INPUT_OFF_09,
        StatusCodes.STATUS_INPUT_OFF_10,
        StatusCodes.STATUS_INPUT_OFF_11,
        StatusCodes.STATUS_INPUT_OFF_12,
        StatusCodes.STATUS_INPUT_OFF_13,
        StatusCodes.STATUS_INPUT_OFF_14,
        StatusCodes.STATUS_INPUT_OFF_15
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* TCP session ID */
    private String          sessionID                   = null;

    /* common GPSEvent instance */
    private GPSEvent        gpsEvent                    = null;

    /* Device record */
    private Device          gpsDevice                   = null;
    private String          lastModemID                 = null;

    /* Session 'terminate' indicator */
    // This value should be set to 'true' when this server has determined that the
    // session should be terminated.  For instance, if this server finishes communication
    // with the Device or if parser finds a fatal error in the incoming data stream 
    // (ie. invalid Account/Device, or unrecognizable data).
    private boolean         terminate                   = false;

    /* session IP address */
    // These values will be set for you by the incoming session to indicate the 
    // originating IP address.
    private String          ipAddress                   = null;
    private int             clientPort                  = 0;

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
        //Print.logStackTrace("new TrackClientPacketHandler ...");
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    // this method is called at the beginning of a communication session
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();

        /* init */
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;
        this.clientPort       = this.getSessionInfo().getRemotePort();

    }

    /* callback when session is terminating */
    // this method is called at the end of a communication session
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------

    /* callback to return the TCP session id */
    public String getSessionID()
    {
        if (!StringTools.isBlank(this.sessionID)) {
            return this.sessionID;
        } else
        if (this.gpsDevice != null) {
            return CreateTcpSessionID(this.gpsDevice);
        } else
        if (this.gpsEvent != null) {
            return CreateTcpSessionID(this.gpsEvent.getDevice());
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public static final boolean USE_STANDARD_TCP_SESSION_ID = true;

    /* get TCP session ID */
    public static String GetTcpSessionID(Device dev)
    {
        if (USE_STANDARD_TCP_SESSION_ID || (dev == null)) {
            return DCServerFactory.getTcpSessionID(dev);
        } else {
            // Custom example for extracting the session ID from the Device 
            // UniqueID (instead of the DeviceID)
            String uid = dev.getUniqueID();
            int    u   = uid.lastIndexOf("_");
            if (u < 0) {
                // no "_" character, fallback to standard TCP sessionID
                return DCServerFactory.getTcpSessionID(dev);
            } else {
                String sessID = dev.getAccountID() + "/" + uid.substring(0,u+1);
                return sessID;
            }
        }
    }

    /* create TCP session ID */
    public static String CreateTcpSessionID(Device dev)
    {
        if (USE_STANDARD_TCP_SESSION_ID || (dev == null)) {
            return DCServerFactory.createTcpSessionID(dev);
        } else {
            String uid = dev.getUniqueID();
            int    u   = uid.lastIndexOf("_");
            if (u < 0) {
                // no "_" character, fallback to standard TCP sessionID
                return DCServerFactory.createTcpSessionID(dev);
            } else {
                String sessID = dev.getAccountID() + "/" + uid.substring(0,u+1);
                dev.setLastTcpSessionID(sessID);
                return sessID;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Create GPSEvent from the AccountID/DeviceID
    *** @return The created GPSEvent instance, or null if the Account/Device is invalid
    **/
    private GPSEvent createGPSEvent(String accountID, String deviceID)
    {
        DCServerConfig dcserver = Main.getServerConfig(null);

        /* create/load device */
        if (this.gpsDevice != null) {
            if (!this.gpsDevice.getAccountID().equals(accountID) || 
                !this.gpsDevice.getDeviceID().equals(deviceID)     ) {
                Print.logError("New AccountID/DeviceID does not match previously loaded Device");
                return null;
            }
            this.gpsEvent  = new GPSEvent(dcserver, this.ipAddress, this.clientPort, this.gpsDevice);
        } else {
            // If AccountID is blank, then DeviceID will be used as MobileID
            if (StringTools.isBlank(deviceID)) {
                Print.logWarn("DeviceID not specified!");
                return null;
            }
            this.gpsEvent  = new GPSEvent(dcserver, this.ipAddress, this.clientPort, accountID, deviceID);
            this.gpsDevice = this.gpsEvent.getDevice(); // may still be null
        }

        /* no Device? */
        if (this.gpsDevice == null) {
            // errors already displayed
            return null;
        }

        /* create session ID */
        this.sessionID = CreateTcpSessionID(this.gpsDevice);

        /* return GPSEvent */
        return this.gpsEvent; // non-null

    }

    /**
    *** Create GPSEvent from the ModemID
    *** @return The created GPSEvent instance, or null if the ModemID is invalid
    **/
    private GPSEvent createGPSEvent(String modemID)
    {
        DCServerConfig dcserver = Main.getServerConfig(null);

        /* create/load device */
        if (this.gpsDevice != null) {
            if (StringTools.isBlank(modemID) || modemID.equals("*")) {
                // -- we don't care about the previous modemID, continue ...
            } else
            if ((this.lastModemID == null) || !this.lastModemID.equals(modemID)) { // fix [B10]
                // -- device has been previously loaded, but does not match current modemID
                Print.logError("New MobileID does not match previously loaded Device");
                return null;
            }
            this.gpsEvent = new GPSEvent(dcserver, this.ipAddress, this.clientPort, this.gpsDevice);
        } else {
            if (StringTools.isBlank(modemID) || modemID.equals("*")) {
                // -- we need a valid modemID to load the device record
                Print.logWarn("ModemID not specified!");
                return null;
            }
            this.gpsEvent  = new GPSEvent(dcserver, this.ipAddress, this.clientPort, modemID);
            this.gpsDevice = this.gpsEvent.getDevice(); // may still be null
            if (this.gpsDevice != null) {
                // -- save last modemID to check for match above
                this.lastModemID = modemID; // fix [B28]
            }
        }

        /* no Device? */
        if (this.gpsDevice == null) {
            // errors already displayed
            return null;
        }

        /* create session ID */
        this.sessionID = CreateTcpSessionID(this.gpsDevice);

        /* return GPSEvent */
        return this.gpsEvent; // non-null

    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        // (This method is only called if "Constants.ASCII_PACKETS" is false!)
        //
        // This method is possibly the most important part of a server protocol implementation.
        // The length of the incoming client packet must be correctly identified in order to 
        // know how many incoming packet bytes should be read.
        //
        // 'packetLen' will be the value specified by Constants.MIN_PACKET_LENGTH, and should
        // be the minimum number of bytes required (but not more) to accurately determine what
        // the total length of the incoming client packet will be.  After analyzing the initial 
        // bytes of the packet, this method should return what it beleives to be the full length 
        // of the client data packet, including the length of these initial bytes.
        //
        // For example:
        //   Assume that all client packets have the following binary format:
        //      Byte  0    - packet type
        //      Byte  1    - payload length (ie packet data)
        //      Bytes 2..X - payload data (as determined by the payload length byte)
        // In this case 'Constants.ASCII_PACKETS' should be set to 'false', and 
        // 'Constants.MIN_PACKET_LENGTH' should be set to '2' (the minimum number of bytes
        // required to determine the actual packet length).  This method should then return
        // the following:
        //      return 2 + ((int)packet[1] & 0xFF);
        // Which is the packet header length (2 bytes) plus the remaining length of the data
        // payload found in the second byte of the packet header. 
        // 
        // Note that the integer cast and 0xFF mask is very important.  'byte' values in
        // Java are signed, thus the byte 0xFF actually represents a '-1'.  So if the packet
        // payload length is 128, then without the (int) cast and mask, the returned value
        // would end up being -126.
        // IE:
        //    byte b = (byte)128;  // this is actually a signed '-128'
        //    System.out.println("1: " + (2+b)); // this casts -128 to an int, and adds 2
        //    System.out.println("2: " + (2+((int)b&0xFF)));
        // The above would print the following:
        //    1: -126
        //    2: 130
        //
        // Once the full client packet is read, it will be delivered to the 'getHandlePacket'
        // method below.
        //
        // WARNING: If a packet length value is returned here that is greater than what the
        // client Device will actually be sending, then the server will receive a read timeout,
        // and this error may cause the socket connection to be closed.  If you happen to see
        // read timeouts occuring during testing/debugging, then it is likely that this method
        // needs to be adjusted to properly identify the client packet length.
        //
        if (Constants.ASCII_PACKETS) {
            // (this actually won't be called if 'Constants.ASCII_PACKETS' is true).
            // ASCII packets - look for line terminator [see Constants.ASCII_LINE_TERMINATOR)]
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;  // read until line termination character
            //return ServerSocketThread.PACKET_LEN_END_OF_STREAM;  // read until end of stream, or maxlen
        } else {
            // BINARY packet - need to analyze 'packet[]' and determine actual packet length
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR; // <-- change this for binary packets
        }
        
    }

    // ------------------------------------------------------------------------

    /* set session terminate after next packet handling */
    private void setTerminate()
    {
        this.terminate = true;
    }
    
    /* indicate that the session should terminate */
    // This method is called after each return from "getHandlePacket" to check to see
    // the current session should be closed.
    public boolean getTerminateSession()
    {
        return this.terminate;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the initial packet sent to the Device after session is open */
    public byte[] getInitialPacket() 
        throws Exception
    {
        // At this point a connection from the client to the server has just been
        // initiated, and we have not yet received any data from the client.
        // If the client is expecting to receive an initial packet from the server at
        // the time that the client connects, then this is where the server can return
        // a byte array that will be transmitted to the client Device.
        return null;
        // Note: any returned response for "getInitialPacket()" is ignored for simplex/udp connections.
        // Returned UDP packets may be sent from "getHandlePacket" or "getFinalPacket".
    }

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {

        // After determining the length of a client packet (see method 'getActualPacketLength'),
        // this method is called with the single packet which has been read from the client.
        // It is the responsibility of this method to determine what type of packet was received
        // from the client, parse/insert any event data into the tables, and return any expected 
        // response that the client may be expected in the form of a byte array.
        if ((pktBytes != null) && (pktBytes.length > 0)) {
            
            /* (debug message) display received data packet */
            Print.logInfo("Recv[HEX]: " + StringTools.toHexString(pktBytes));
            String s = StringTools.toStringValue(pktBytes).trim(); // remove leading/trailing spaces
            Print.logInfo("Recv[TXT]: " + s); // debug message
            
            /* parse/insert event */
            byte rtn[] = null;
            switch (DATA_FORMAT_OPTION) {
                case  1 : rtn = this.parseInsertRecord_ASCII_01(s); break;
                case  2 : rtn = this.parseInsertRecord_ASCII_02(s); break;
                case  3 : rtn = this.parseInsertRecord_ASCII_03(s); break;
                case  9 : rtn = this.parseInsertRecord_RTProps (s); break;
                case 11 : rtn = this.parseInsertRecord_Device_1(s); break;
                default: Print.logError("Unspecified data format"); break;
            }
            // Note:
            // The above examples assume ASCII data.  If the data arrives as a binary data packet,
            // the utility class "org.opengts.util.Payload" can be used to parse the binary data:
            // For example:
            //   Assume 'pktBytes' contains the following binary hex data:
            //      01 02 03 04 05 06 07 08 09 0A 0B
            //   One way to parse this binary data would be as follows:
            //      Payload p = new Payload(pktBytes);
            //      int fld_1 = (int)p.readLong(3,0L);   // parse 0x010203   into 'fld_1'
            //      int fld_2 = (int)p.readLong(4,0L);   // parse 0x04050607 into 'fld_2'
            //      int fld_3 = (int)p.readLong(2,0L);   // parse 0x00809    into 'fld_2'
            //      int fld_4 = (int)p.readLong(2,0L);   // parse 0x0A0B     into 'fld_2'

            /* return response */
            // If the client is expecting to receive a response from the server (such as an
            // acknowledgement), this is where the server should compose a returned response
            // in the form of an array of bytes which should be returned here.  This byte array
            // will then be transmitted back to the client.
            return rtn; // no return packets are expected

        } else {

            /* no packet date received */
            Print.logInfo("Empty packet received ...");
            return null; // no return packets are expected

        }

        // when this method returns, the server framework then starts the process over again
        // attempting to read another packet from the client Device (see method 'getActualPacketLength').
        // If this server determines that communicqtion with the client Device has completed, then
        // the above "terminateSession" method should return true [the method "setTerminate()" is 
        // provided to facilitate session termination - see "setTerminate" above].

    }

    /* final packet sent to Device before session is closed */
    public byte[] getFinalPacket(boolean hasError) 
        throws Exception
    {
        // If the server wishes to send a final packet to the client just before the connection
        // is closed, then this is where the server should compose the final packet, and return
        // this packet in the form of a byte array.  This byte array will then be transmitted
        // to the client Device before the session is closed.
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record (common) */
    private boolean parseInsertRecord_Common(GPSEvent gpsEv)
    {
        long   fixtime    = gpsEv.getTimestamp();
        int    statusCode = gpsEv.getStatusCode();
        Device dev        = gpsEv.getDevice(); // guaranteed non-null here

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date/time");
            fixtime = DateTime.getCurrentTimeSec(); // default to now
            gpsEv.setTimestamp(fixtime);
        }
                
        /* valid lat/lon? */
        if (!gpsEv.isValidGeoPoint()) {
            Print.logWarn("Invalid lat/lon: " + gpsEv.getLatitude() + "/" + gpsEv.getLongitude());
            gpsEv.setLatitude(0.0);
            gpsEv.setLongitude(0.0);
        }
        GeoPoint geoPoint = gpsEv.getGeoPoint();

        /* minimum speed */
        if (gpsEv.getSpeedKPH() < MINIMUM_SPEED_KPH) {
            gpsEv.setSpeedKPH(0.0);
            gpsEv.setHeading(0.0);
        }

        /* estimate GPS-based odometer */
        double odomKM = 0.0; // set to available odometer from event record
        //if (this.gpsDevice.getLastEventTimestamp() < fixtime) {
        if (odomKM <= 0.0) {
            odomKM = (ESTIMATE_ODOMETER && geoPoint.isValid())? 
                this.gpsDevice.getNextOdometerKM(geoPoint) : 
                this.gpsDevice.getLastOdometerKM();
        } else {
            odomKM = dev.adjustOdometerKM(odomKM);
        }
        //}
        Print.logInfo("Odometer KM: " + odomKM);
        gpsEv.setOdometerKM(odomKM);

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && geoPoint.isValid()) {
            java.util.List<Device.GeozoneTransition> zone = dev.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    gpsEv.insertEventData(z.getTimestamp(), z.getStatusCode(), z.getGeozone());
                    Print.logInfo("Geozone    : " + z);
                }
            }
        }

        /* digital input change events */
        if (gpsEv.hasInputMask() && (gpsEv.getInputMask() >= 0L)) {
            long gpioInput = gpsEv.getInputMask();
            if (SIMEVENT_DIGITAL_INPUTS > 0L) {
                // The current input state is compared to the last value stored in the Device record.
                // Changes in the input state will generate a synthesized event.
                long chgMask = (dev.getLastInputState() ^ gpioInput) & SIMEVENT_DIGITAL_INPUTS;
                if (chgMask != 0L) {
                    // an input state has changed
                    for (int b = 0; b <= 15; b++) {
                        long m = 1L << b;
                        if ((chgMask & m) != 0L) {
                            // this bit changed
                            int  inpCode = ((gpioInput & m) != 0L)? InputStatusCodes_ON[b] : InputStatusCodes_OFF[b];
                            long inpTime = fixtime;
                            gpsEv.insertEventData(inpTime, inpCode);
                            Print.logInfo("GPIO : " + StatusCodes.GetDescription(inpCode,null));
                        }
                    }
                }
            }
            dev.setLastInputState(gpioInput & 0xFFFFL); // FLD_lastInputState
        }

        /* create/insert standard event */
        gpsEv.insertEventData(fixtime, statusCode);

        /* save Device changes */
        gpsEv.updateDevice();

        /* return success */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private byte[] parseInsertRecord_ASCII_01(String s)
    {
        // This is an example showing how the server might parse one type of ASCII encoded data.
        // Since every Device utilizes a different data format, this will likely not match the
        // format coming from your chosen Device and may need some significant changes to support
        // the format provided by your evice (assuming that the format is even ASCII).
        //
        // This parsing method assumes the data format appears as follows:
        //   <MobileID>,<YYYY/MM/DD>,<HH:MM:SS>,<Latitude>,<Longitude>,<Speed>,<Heading>,<AltitudeM>
        //   0--------- 1----------- 2--------- 3--------- 4---------- 5------ 6-------- 7----------
        //   |          |            |          |          |           |       |> Heading (degrees)
        //   |          |            |          |          |           |> Speed (km/h)
        //   |          |            |          |          |> Longitude (degrees)
        //   |          |            |          |> Latitude (degrees)
        //   |          |            |> Time [GMT]
        //   |          |> Date [GMT]
        //   |> MobileID/IMEI
        //
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s,',');
        if ((fld == null) || (fld.length < 5)) {
            Print.logWarn("Invalid number of fields");
            return null;
        }

        /* parse individual fields */
        String   modemID    = fld[0].toLowerCase();
        long     fixtime    = this._parseDate(fld[1],fld[2]);
        int      statusCode = StatusCodes.STATUS_LOCATION;
        double   latitude   = StringTools.parseDouble(fld[3],0.0);
        double   longitude  = StringTools.parseDouble(fld[4],0.0);
        double   speedKPH   = (fld.length > 5)? StringTools.parseDouble(fld[5],0.0) : 0.0;
        double   heading    = (fld.length > 6)? StringTools.parseDouble(fld[6],0.0) : 0.0;
        double   altitudeM  = (fld.length > 7)? StringTools.parseDouble(fld[7],0.0) : 0.0;

        /* GPS Event */
        this.gpsEvent = this.createGPSEvent(modemID);
        if (this.gpsEvent == null) {
            // errors already displayed
            return null;
        }

        /* populate GPS event fields */
        this.gpsEvent.setTimestamp(fixtime);
        this.gpsEvent.setStatusCode(statusCode);
        this.gpsEvent.setLatitude(latitude);
        this.gpsEvent.setLongitude(longitude);
        this.gpsEvent.setSpeedKPH(speedKPH);
        this.gpsEvent.setHeading(heading);
        this.gpsEvent.setAltitude(altitudeM);
        
        /* insert/return */
        if (this.parseInsertRecord_Common(this.gpsEvent)) {
            // change this to return any required acknowledgement (ACK) packets back to the Device
            return null;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /* parse the specified date into unix 'epoch' time */
    private long _parseDate(String yyyymmdd, String hhmmss)
    {
        // "YYYY/MM/DD", "hh:mm:ss"
        String d[] = StringTools.parseStringArray(yyyymmdd,"/");
        String t[] = StringTools.parseStringArray(hhmmss  ,":");
        if ((d.length != 3) && (t.length != 3)) {
            //Print.logError("Invalid date: " + ymd + ", " + hms);
            return 0L;
        } else {
            int YY = StringTools.parseInt(d[0],0); // 07 year
            int MM = StringTools.parseInt(d[1],0); // 04 month
            int DD = StringTools.parseInt(d[2],0); // 18 day
            int hh = StringTools.parseInt(t[0],0); // 01 hour
            int mm = StringTools.parseInt(t[1],0); // 48 minute
            int ss = StringTools.parseInt(t[2],0); // 04 second
            if (YY < 100) { YY += 2000; }
            DateTime dt = new DateTime(gmtTimezone,YY,MM,DD,hh,mm,ss);
            return dt.getTimeSec();
        }
    }

    /* parse the specified date into unix 'epoch' time */
    private long _parseDate(long yyyymmdd, long hhmmss)
    {
        if ((yyyymmdd <= 0L) || (hhmmss < 0L)) {
            return 0L;
        } else {
            //                      YYYYMMDD
            int YY = (int)((yyyymmdd / 10000L)       ); // 2011 year
            int MM = (int)((yyyymmdd /   100L) % 100L); // 04 month
            int DD = (int)((yyyymmdd /     1L) % 100L); // 18 day
            //                        HHMMSS
            int hh = (int)((hhmmss   / 10000L)       ); // 01 hour
            int mm = (int)((hhmmss   /   100L) % 100L); // 48 minute
            int ss = (int)((hhmmss   /     1L) % 100L); // 04 second
            // in case "YYYYMMDD" was specified as "YYMMDD"
            if (YY < 100) { YY += 2000; }
            DateTime dt = new DateTime(gmtTimezone,YY,MM,DD,hh,mm,ss);
            return dt.getTimeSec();
        }
    }

    /* parse the specified date into unix 'epoch' time */
    private long _parseDate(long yyyymmddhhmmss)
    {
        if (yyyymmddhhmmss <= 0L) {
            return 0L;
        } else {
            //                            YYYYMMDDhhmmss
            int YY = (int)((yyyymmddhhmmss / 10000000000L)       ); // 2011 year
            int MM = (int)((yyyymmddhhmmss /   100000000L) % 100L); // 04 month
            int DD = (int)((yyyymmddhhmmss /     1000000L) % 100L); // 18 day
            int hh = (int)((yyyymmddhhmmss /       10000L) % 100L); // 01 hour
            int mm = (int)((yyyymmddhhmmss /         100L) % 100L); // 48 minute
            int ss = (int)((yyyymmddhhmmss /           1L) % 100L); // 04 second
            if (YY < 100) { YY += 2000; }
            DateTime dt = new DateTime(gmtTimezone,YY,MM,DD,hh,mm,ss);
            return dt.getTimeSec();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private byte[] parseInsertRecord_ASCII_02(String s)
    {
        // This is an example showing how the server might parse one type of ASCII encoded data.
        // Since every Device utilizes a different data format, this will likely not match the
        // format coming from your chosen Device and may need some significant changes to support
        // the format provided by your Device (assuming that the format is even ASCII).
        //
        // This parsing method assumes the data format appears as follows:
        //   <AccountID>/<DeviceID>/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
        //   0---------- 1--------- 2-----------------------------------------------------------------------
        //   |           |          |> $GPRMC record ...
        //   |           |> DeviceID
        //   |> AccountID
        //
        // If "<AccountID>" is blank, then "<DeviceID>" will be used as the "<MobileID>"
        // If "<DeviceID>" is blank, or "_mid_", then "<AccountID>" will be used as the "<MobileID>"
        // IE:
        //   /<MobileID>/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
        //   <MobileID>// $GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
        //   <MobileID>/_mid_/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
        //
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s, '/');
        if ((fld == null) || (fld.length < 3)) {
            Print.logWarn("Invalid number of fields");
            return null;
        }

        /* parse Account/Device */
        String   accountID  = fld[0].toLowerCase(); // if blank, DeviceID is used as MobileID
        String   deviceID   = fld[1].toLowerCase(); // if blank or "_mid_", AccountID is used as MobileID

        /* parse GPRMC record */
        Nmea0183 gprmc      = new Nmea0183(fld[2], IGNORE_NMEA_CHECKSUM);
        long     fixtime    = gprmc.getFixtime();
        int      statusCode = StatusCodes.STATUS_LOCATION;
        double   latitude   = gprmc.getLatitude();
        double   longitude  = gprmc.getLongitude();
        double   speedKPH   = gprmc.getSpeedKPH();
        double   heading    = gprmc.getHeading();
        double   altitudeM  = 0.0;  // 

        /* hack to allow specifying the MobileID in the AcountID field */
        if (deviceID.equals("") || deviceID.equals("_mid_")) { // "", "_mid_"
            // swap locations
            deviceID  = accountID;
            accountID = ""; // blank to force MobileID lookup below
        }

        /* GPS Event */
        this.gpsEvent = this.createGPSEvent(accountID, deviceID);
        if (this.gpsEvent == null) {
            // errors already displayed
            return null;
        }

        /* populate GPS event fields */
        this.gpsEvent.setTimestamp(fixtime);
        this.gpsEvent.setStatusCode(statusCode);
        this.gpsEvent.setLatitude(latitude);
        this.gpsEvent.setLongitude(longitude);
        this.gpsEvent.setSpeedKPH(speedKPH);
        this.gpsEvent.setHeading(heading);
        this.gpsEvent.setAltitude(altitudeM);

        /* insert/return */
        if (this.parseInsertRecord_Common(this.gpsEvent)) {
            // change this to return any required acknowledgement (ACK) packets back to the Device
            return null;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private byte[] parseInsertRecord_ASCII_03(String s)
    {
        // Another example showing how the server might parse one type of ASCII encoded data.
        //
        // This parsing method assumes the data format appears as follows:
        //   <Seq>,<Code>,<MobileID>,<Format>,<YYYYMMDD>,<HHMMSS>,<GPSValid>,<HDOP>,<Lat>,<Lon>,<Heading>,<Speed>,<Altitude>
        //   0---- 1----- 2--------- 3------- 4--------- 5------- 6--------- 7----- 8---- 9---- A-------- B------ C---------
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        }
        
        /* separate key|value from rest of packet */
        String kv = null;
        int kvPos = s.indexOf(';');
        if (kvPos >= 0) {
            kv = s.substring(kvPos+1);
            s  = s.substring(0,kvPos);
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s, ',');
        if ((fld == null) || (fld.length < 10)) {
            Print.logWarn("Invalid number of fields");
            return null;
        }

        //   0     1      2        3        4        5    6        7    8 9        A         B      ,C
        //   Seq,Code,MobileID,Format,YYYYMMDD,HHMMSS,GPSValid,HDOP,Latitude,Longitude,Heading,Speed,Altitude
        //   1  ,123 ,12345678,11    ,20101222,110723,1       ,2.10, 37.1234,-142.1234,235    ,34.7 ,1820
        /* parse individual fields */
        int      statusCode = StatusCodes.STATUS_LOCATION;
        int      sequence   = StringTools.parseInt(fld[0],0);
        int      eventCode  = StringTools.parseInt(fld[1],0);
        String   modemID    = fld[2].toLowerCase();
        int      format     = StringTools.parseInt(fld[3],0);
        long     yyyymmdd   = StringTools.parseLong(fld[4],0L);
        long     hhmmss     = StringTools.parseLong(fld[5],0L);
        long     fixtime    = this._parseDate(yyyymmdd,hhmmss);
        boolean  validGPS   = fld[6].equals("1");
        double   latitude   = validGPS? StringTools.parseDouble(fld[ 8],0.0) : 0.0;
        double   longitude  = validGPS? StringTools.parseDouble(fld[ 9],0.0) : 0.0;
        double   heading    = validGPS && (fld.length > 10)? StringTools.parseDouble(fld[10],0.0) : 0.0;
        double   speedKPH   = validGPS && (fld.length > 11)? StringTools.parseDouble(fld[11],0.0) : 0.0;
        double   altitudeM  = validGPS && (fld.length > 12)? StringTools.parseDouble(fld[12],0.0) : 0.0;

        /* GPS Event */
        this.gpsEvent = this.createGPSEvent(modemID);
        if (this.gpsEvent == null) {
            // errors already displayed
            return null;
        }

        /* populate GPS event fields */
        this.gpsEvent.setTimestamp(fixtime);
        this.gpsEvent.setStatusCode(statusCode);
        this.gpsEvent.setLatitude(latitude);
        this.gpsEvent.setLongitude(longitude);
        this.gpsEvent.setSpeedKPH(speedKPH);
        this.gpsEvent.setHeading(heading);
        this.gpsEvent.setAltitude(altitudeM);

        /* insert/return */
        if (this.parseInsertRecord_Common(this.gpsEvent)) {
            // change this to return any required acknowledgement (ACK) packets back to the Device
            return null;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static String RTP_ACCOUNT[]     = new String[] { "acct" , "accountid"    };
    private static String RTP_DEVICE[]      = new String[] { "dev"  , "deviceid"     };
    private static String RTP_MODEMID[]     = new String[] { "mid"  , "modemid"      , "uniqueid"    , "imei" };
    private static String RTP_TIMESTAMP[]   = new String[] { "ts"   , "timestamp"    , "time"        };
    private static String RTP_STATUSCODE[]  = new String[] { "code" , "statusCode"   };
    private static String RTP_GEOPOINT[]    = new String[] { "gps"  , "geopoint"     };
    private static String RTP_GPSAGE[]      = new String[] { "age"  , "gpsAge"       };
    private static String RTP_SATCOUNT[]    = new String[] { "sats" , "satCount"     };
    private static String RTP_SPEED[]       = new String[] { "kph"  , "speed"        , "speedKph"    };
    private static String RTP_HEADING[]     = new String[] { "dir"  , "heading"      };
    private static String RTP_ALTITUDE[]    = new String[] { "alt"  , "altm"         , "altitude"    };
    private static String RTP_ODOMETER[]    = new String[] { "odom" , "odometer"     };
    private static String RTP_INPUTMASK[]   = new String[] { "gpio" , "inputMask"    };
    private static String RTP_SERVERID[]    = new String[] { "dcs"  , "serverid"     };
    private static String RTP_ACK[]         = new String[] { "ack"  };
    private static String RTP_NAK[]         = new String[] { "nak"  };

    /* parse and insert data record */
    private byte[] parseInsertRecord_RTProps(String s)
    {
        // This is an example showing how another parsing server might transfer data to this
        // server, using the following simple (and extensible) format:
        //   mid=123456789012345 ts=1254100914 code=0xF020 gps=39.1234/-142.1234 kph=45.6 dir=123 alt=1234 odom=1234.5

        // The following data field are supported:
        //   mid   = Mobile-ID (typically the IMEI#)
        //   ts    = Timestamp (in Unix Epoch format)
        //   code  = The status code 
        //   gps   = the latitude/logitude
        //   age   = age of GPS fix in seconds
        //   sats  = number of satellites
        //   kph   = Vehicle speed in km/h
        //   dir   = Vehicle heading in degrees
        //   alt   = Altitude in meters
        //   odom  = Vehicle odometer (if available)
        //   gpio  = Input mask
        //   ack   = Acknowledgement to return to the Device on successful parsing
        //   nak   = Negative-acknowledgement to return to the Device on error
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (StringTools.isBlank(s)) {
            Print.logError("Packet string is blank/null");
            return null;
        }

        /* parse */
        RTProperties rtp = new RTProperties(s);
        String   accountID  = rtp.getString(RTP_ACCOUNT,   null);
        String   deviceID   = rtp.getString(RTP_DEVICE,    null);
        String   mobileID   = rtp.getString(RTP_MODEMID,   null);
        long     fixtime    = rtp.getLong(  RTP_TIMESTAMP, 0L);
        int      statusCode = rtp.getInt(   RTP_STATUSCODE,StatusCodes.STATUS_LOCATION);
        String   gpsStr     = rtp.getString(RTP_GEOPOINT,  null);
        long     gpsAge     = rtp.getLong(  RTP_GPSAGE,    0L);
        int      satCount   = rtp.getInt(   RTP_SATCOUNT,  0);
        double   speedKPH   = rtp.getDouble(RTP_SPEED,     0.0);
        double   heading    = rtp.getDouble(RTP_HEADING,   0.0);
        double   altitudeM  = rtp.getDouble(RTP_ALTITUDE,  0.0);
        double   odomKM     = rtp.getDouble(RTP_ODOMETER,  0.0);
        long     gpioInput  = rtp.getLong(  RTP_INPUTMASK, -1L);
        String   dcsid      = rtp.getString(RTP_SERVERID,  null);
        String   ack        = rtp.getString(RTP_ACK,       null);
        String   nak        = rtp.getString(RTP_NAK,       null);
        GeoPoint geoPoint   = new GeoPoint(gpsStr);

        /* no mobileID? */
        if (StringTools.isBlank(mobileID)) {
            Print.logError("UniqueID/ModemID not specified!");
            return (nak != null)? (nak+"\n").getBytes() : null;
        }

        /* DCServer */
        String dcsName = !StringTools.isBlank(dcsid)? dcsid : Main.getServerName();
        DCServerConfig dcserver = DCServerFactory.getServerConfig(dcsName);
        if (dcserver == null) {
            Print.logWarn("DCServer name not registered: " + dcsName);
        }

        /* validate IDs */
        boolean hasAcctDevID = false;
        if (!StringTools.isBlank(accountID)) {
            if (StringTools.isBlank(deviceID)) {
                Print.logError("'deviceID' required if 'accountID' specified");
                return (nak != null)? (nak+"\n").getBytes() : null;
            } else
            if (!StringTools.isBlank(mobileID)) {
                Print.logError("'mobileID' not allowed if 'accountID' specified");
                return (nak != null)? (nak+"\n").getBytes() : null;
            }
            hasAcctDevID = true;
        } else
        if (!StringTools.isBlank(deviceID)) {
            Print.logError("'accountID' required if 'deviceID' specified");
            return (nak != null)? (nak+"\n").getBytes() : null;
        } else
        if (StringTools.isBlank(mobileID)) {
            Print.logError("'mobileID' not specified");
            return (nak != null)? (nak+"\n").getBytes() : null;
        }

        /* GPS Event */
        this.gpsEvent = hasAcctDevID?
            this.createGPSEvent(accountID, deviceID) :
            this.createGPSEvent(mobileID);
        if (this.gpsEvent == null) {
            // errors already displayed
            return (nak != null)? (nak+"\n").getBytes() : null;
        }

        /* populate GPS event fields */
        this.gpsEvent.setTimestamp(fixtime);
        this.gpsEvent.setStatusCode(statusCode);
        this.gpsEvent.setGeoPoint(geoPoint);
        this.gpsEvent.setGpsAge(gpsAge);
        this.gpsEvent.setSatelliteCount(satCount);
        this.gpsEvent.setSpeedKPH(speedKPH);
        this.gpsEvent.setHeading(heading);
        this.gpsEvent.setAltitude(altitudeM);
        this.gpsEvent.setOdometerKM(odomKM);
        if (gpioInput >= 0L) { this.gpsEvent.setInputMask(gpioInput); }

        /* insert/return */
        if (this.parseInsertRecord_Common(this.gpsEvent)) {
            return (ack != null)? (ack+"\n").getBytes() : null;
        } else {
            return (nak != null)? (nak+"\n").getBytes() : null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    // this format supports some Bofan Devices
    private byte[] parseInsertRecord_Device_1(String s)
    {
        // Format:
        //   $<EventCode>,<MobileID>,<HHMMSS>,<GPSValid>,<NMEALat>,<N|S>,<NMEALon>,<E|W>,<SpeedKnots>,<Heading>,<DDMMYY>
        // Example:
        //   $POS,10000,102215.000,V,2233.8171,N,14205.6367,W,0.0,0.0,250411
        //   0--- 1---- 2--------- 3 4-------- 5 6--------- 7 8-- 9-- A-----
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (StringTools.isBlank(s)) {
            Print.logError("Packet string is blank/null");
            return null;
        } else 
        if (!s.startsWith("$")) {
            Print.logError("Packet string does not start with '$'");
            return null;
        }

        /* separate into fields */
        String fld[] = StringTools.parseStringArray(s.substring(1), ',');
        if ((fld == null) || (fld.length < 11)) {
            Print.logWarn("Invalid number of fields");
            return null;
        }

        /* parse individual fields */
        String   eventCode  = fld[0];
        String   modemID    = fld[1].toLowerCase();
        long     fixtime    = Nmea0183.parseFixtime  (fld[10], fld[2], true);
        boolean  validGPS   = fld[3].equalsIgnoreCase("A");
        double   latitude   = validGPS? Nmea0183.ParseLatitude (fld[ 4], fld[5],  90.0) : 0.0;
        double   longitude  = validGPS? Nmea0183.ParseLongitude(fld[ 6], fld[7], 180.0) : 0.0;
        double   speedKnot  = validGPS? StringTools.parseDouble(fld[ 8], 0.0) : 0.0;
        double   speedKPH   = validGPS? (speedKnot * KILOMETERS_PER_KNOT) : 0.0;
        double   heading    = validGPS? StringTools.parseDouble(fld[ 9], 0.0) : 0.0;
        double   altitudeM  = 0.0;  // 

        /* status code */
        int      statusCode = StatusCodes.STATUS_LOCATION;
        if (eventCode.equalsIgnoreCase("POS")) {
            // "Position" event
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (eventCode.equalsIgnoreCase("IN1")) {
            // "SOS" button
            statusCode = StatusCodes.STATUS_WAYMARK_1;
        } else
        if (eventCode.equalsIgnoreCase("IN2")) {
            // "KEY2" alarm
            statusCode = StatusCodes.STATUS_WAYMARK_2;
        } else
        if (eventCode.equalsIgnoreCase("IN3")) {
            // "KEY2" alarm
            statusCode = StatusCodes.STATUS_WAYMARK_3;
        } else
        if (eventCode.equalsIgnoreCase("LPA")) {
            // "Low Power" alarm
            statusCode = StatusCodes.STATUS_LOW_BATTERY;
        } else
        if (eventCode.equalsIgnoreCase("CPA")) {
            // "Cut Power" alarm
            statusCode = StatusCodes.STATUS_POWER_FAILURE;
        } else
        if (eventCode.equalsIgnoreCase("SPD")) {
            // "Speeding" alarm
            statusCode = StatusCodes.STATUS_MOTION_EXCESS_SPEED;
        } else
        if (eventCode.equalsIgnoreCase("GOF")) {
            // "Geofence" alarm
            statusCode = StatusCodes.STATUS_GEOFENCE_VIOLATION;
        } else {
            // leave as-is
        }

        /* GPS Event */
        this.gpsEvent = this.createGPSEvent(modemID);
        if (this.gpsEvent == null) {
            // errors already displayed
            return null;
        }

        /* populate GPS event fields */
        this.gpsEvent.setTimestamp(fixtime);
        this.gpsEvent.setStatusCode(statusCode);
        this.gpsEvent.setLatitude(latitude);
        this.gpsEvent.setLongitude(longitude);
        this.gpsEvent.setSpeedKPH(speedKPH);
        this.gpsEvent.setHeading(heading);
        this.gpsEvent.setAltitude(altitudeM);

        /* insert/return */
        if (this.parseInsertRecord_Common(this.gpsEvent)) {
            // change this to return any required acknowledgement (ACK) packets back to the Device
            return null;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initialize runtime configuration
    **/
    public static void configInit() 
    {
        DCServerConfig dcsc     = Main.getServerConfig(null);
        if (dcsc == null) {
            Print.logWarn("DCServer not found: " + Main.getServerName());
            return;
        }

        /* custom */
        DATA_FORMAT_OPTION      = dcsc.getIntProperty(Main.ARG_FORMAT, DATA_FORMAT_OPTION);

        /* common */
        MINIMUM_SPEED_KPH       = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
        ESTIMATE_ODOMETER       = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
        SIMEVENT_GEOZONES       = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
        SIMEVENT_DIGITAL_INPUTS = dcsc.getSimulateDigitalInputs(SIMEVENT_DIGITAL_INPUTS) & 0xFFFFL;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Once you have modified this example 'template' server to parse your particular
    // Device packets, you can also use this source module to load GPS data packets
    // which have been saved in a file.  To run this module to load your save GPS data
    // packets, start this command as follows:
    //   java -cp <classpath> org.opengts.servers.template.TrackClientPacketHandler {options}
    // Where your options are one or more of 
    //   -insert=[true|false]    Insert parse records into EventData
    //   -format=[1|2]           Data format
    //   -debug                  Parse internal sample data
    //   -parseFile=<file>       Parse data from specified file

    private static int _usage()
    {
        String cn = StringTools.className(TrackClientPacketHandler.class);
        Print.sysPrintln("Test/Load Device Communication Server");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  $JAVA_HOME/bin/java -classpath <classpath> %s {options}", cn);
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -insert=[true|false]    Insert parsed records into EventData");
        Print.sysPrintln("  -format=[1|2]           Data format");
        Print.sysPrintln("  -debug                  Parse internal sample/debug data (if any)");
        Print.sysPrintln("  -parseFile=<file>       Parse data from specified file");
        return 1;
    }

    /**
    *** Main entry point used for debug purposes only
    **/
    public static int _main(boolean fromMain)
    {

        /* default options */
        INSERT_EVENT = RTConfig.getBoolean(Main.ARG_INSERT, DFT_INSERT_EVENT);
        if (!INSERT_EVENT) {
            Print.sysPrintln("Warning: Data will NOT be inserted into the database");
        }

        /* create client packet handler */
        TrackClientPacketHandler tcph = new TrackClientPacketHandler();

        /* DEBUG sample data */
        if (RTConfig.getBoolean(Main.ARG_DEBUG,false)) {
            String data[] = null;
            switch (DATA_FORMAT_OPTION) {
                case  1: data = new String[] {
                    "123456789012345,2006/09/05,07:47:26,35.3640,-142.2958,27.0,224.8",
                }; break;
                case  2: data = new String[] {
                    "account/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,12.09,108.52,200505,,*2E",
                    "/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,12.09,108.52,200505,,*2E",
                }; break;
                case  3: data = new String[] {
                    "2,123,1234567890,0,20101223,110819,1,2.1,39.1234,-142.1234,33,227,1800",
                }; break;
                case  9: data = new String[] {
                    "mid=123456789012345 lat=39.12345 lon=-142.12345 kph=123.0"
                }; break;
                default:
                    Print.sysPrintln("Unrecognized Data Format: %d", DATA_FORMAT_OPTION);
                    return _usage();
            }
            for (int i = 0; i < data.length; i++) {
                tcph.getHandlePacket(data[i].getBytes());
            }
            return 0;
        }

        /* 'parseFile' specified? */
        if (RTConfig.hasProperty(Main.ARG_PARSEFILE)) {

            /* get input file */
            File parseFile = RTConfig.getFile(Main.ARG_PARSEFILE,null);
            if ((parseFile == null) || !parseFile.isFile()) {
                Print.sysPrintln("Data source file not specified, or does not exist.");
                return _usage();
            }

            /* open file */
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(parseFile);
            } catch (IOException ioe) {
                Print.logException("Error openning input file: " + parseFile, ioe);
                return 2;
            }

            /* loop through file */
            try {
                // records are assumed to be terminated by CR/NL 
                for (;;) {
                    String data = FileTools.readLine(fis);
                    if (!StringTools.isBlank(data)) {
                        tcph.getHandlePacket(data.getBytes());
                    }
                }
            } catch (EOFException eof) {
                Print.sysPrintln("");
                Print.sysPrintln("***** End-Of-File *****");
            } catch (IOException ioe) {
                Print.logException("Error reaading input file: " + parseFile, ioe);
            } finally {
                try { fis.close(); } catch (Throwable th) {/* ignore */}
            }

            /* done */
            return 0;

        }

        /* no options? */
        return _usage();

    }

    /**
    *** Main entry point used for debug purposes only
    **/
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,false);
        TrackClientPacketHandler.configInit();
        System.exit(TrackClientPacketHandler._main(false));
    }
    
}
