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
// TK102/TK103 device communication server:
//  -Includes support to overriding default status code
//  -Includes support for simulated geozone arrive/depart events
// ----------------------------------------------------------------------------
// Change History:
//  2011/07/15  Martin D. Flynn
//     -Initial release.
//  2011/08/21  Martin D. Flynn
//     -TK103: The Date column appears to be specified in a timezone local to
//      where the device was configured.  Made some changes to attempt to 
//      determine the actual GMT day, based on the time difference between the 
//      local time, and the GMT time specified elsewhere in the record.
//     -TK103: It appears that most tk103 data packets do not contain a heading
//      value.  If no heading value is present, then an approximate heading will
//      be calcualted based on the previous valid location.
//  2012/02/03  Martin D. Flynn
//     -TK103-ALT: Added support for alternate TK102/TK103 format
//     -Extract battery voltage and altitude from TK102 packet
//  2012/04/03  Martin D. Flynn
//     -Added TK103 simulated digital input change events
//  2012/10/16  Martin D. Flynn
//     -Replaced "DCServerFactory.loadDeviceByPrefixedModemID" with "DCServerConfig.loadDeviceUniqueID".
//     -Make trailing ")" optional when parsing TK103-Alt (packet terminating character removed)
//     -Added TK103 response for "##" ("LOAD") and IMEI-only ("ON") packets.
//  2013/05/28  Martin D. Flynn
//     -Update "parseInsertRecord_TK103_3" handling of GPIO and event codes ("BO01").
//     -Changed conversion of odometer/mileage units to 'meters'.
//  2013/09/20  Martin D. Flynn
//     -Various packet type parsing now calls "handleCommon(...)" to process standard packet data.
//     -Initial implementation of TKnano-1(ASCII) support (EXPERIMENTAL - not yet complete)
//  2013/11/11  Martin D. Flynn
//     -[GTSE] additional event codes.
//  2014/03/03  Martin D. Flynn
//     -Added support for TK102B protocol format.
//  2014/09/16  Martin D. Flynn
//     -Fixed issue with inserting extraneous status code events (see "v2.5.7-B10")
//     -Added support for using the last valid GPS location if the current GPS location is 
//      invalid (see "USE_LAST_VALID_GPS").
//     -Initial support for TK103-2 "OBD" record type. [v2.5.7-B18]
//     -Added cell-tower data parse to tk103_2 type events. [v2.5.7-B18]
//      Many thanks to Franjieh El Khoury for this information.
//     -Added support for VJoy device. [v2.5.7-B18]
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.servers.tk10x;

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

import org.opengts.cellid.CellTower;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    public static       boolean DEBUG_MODE                  = false;

    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = null;
    public static       double  MINIMUM_SPEED_KPH           = Constants.MINIMUM_SPEED_KPH;
    public static       boolean ESTIMATE_ODOMETER           = true;
    public static       boolean SIMEVENT_GEOZONES           = false;
    public static       long    SIMEVENT_DIGITAL_INPUTS     = 0xFFL;
    public static       boolean XLATE_LOCATON_INMOTION      = true;
    public static       boolean USE_LAST_VALID_GPS          = false;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;
    public static       boolean PACKET_LEN_END_OF_STREAM    = false;

    // ------------------------------------------------------------------------

    /* convenience for converting knots to kilometers */
    public static final double  KILOMETERS_PER_KNOT         = 1.85200000;

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

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------

    /**
    *** Enumerated type: TK device type
    **/
    public enum TKDeviceType {
        UNKNOWN  (0,"Unknown" ),
        TK102    (1,"TK102"   ),
        TK102_2  (2,"TK102-2" ),
        TK102B   (3,"TK102B"  ),
        TK103_1  (4,"TK103-1" ),
        TK103_2  (5,"TK103-2" ),
        TK103_3  (6,"TK103-3" ),
        TKnano_1 (7,"TKnano-1"), // EXPERIMENTAL: may not be supported
        TKnano_2 (8,"TKnano-2"), // EXPERIMENTAL: may not be supported
        VJOY     (9,"VJoy"    ); // EXPERIMENTAL: may not be supported
        // ---
        private int    vv = 0;
        private String dd = null;
        TKDeviceType(int v, String d) { vv = v; dd = d; }
        public String  toString()     { return dd; }
        public boolean isUnknown()    { return this.equals(UNKNOWN); }
        public boolean isTK102()      { return this.equals(TK102); }
        public boolean isTKnano()     { return this.equals(TKnano_1) || this.equals(TKnano_2); }
    };

    /**
    *** Returns true if TKDeviceType is null or UKNOWN
    **/
    private static boolean IsDeviceTypeUnknown(TKDeviceType dt)
    {
        return ((dt == null) || dt.isUnknown());
    }

    /**
    *** Returns true if TKDeviceType is non-null and TK102
    **/
    private static boolean IsDeviceTypeTK102(TKDeviceType dt)
    {
        return ((dt != null) && dt.isTK102());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private TKDeviceType tkDeviceType = TKDeviceType.UNKNOWN;
    private String       tkModemID    = null;
    private Device       tkDevice     = null;

    /** 
    *** Packet handler constructor 
    **/
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /**
    *** Callback when session is starting 
    **/
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);
        super.clearTerminateSession();
        this.clearSavedEventCount();
        this.tkDeviceType = TKDeviceType.UNKNOWN;
        this.tkModemID    = null;
        this.tkDevice     = null;
    }

    /**
    *** Callback when session is terminating 
    **/
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        super.sessionTerminated(err, readCount, writeCount);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the size of the packet in the queue.
    **/
    public int getActualPacketLength(byte packet[], int packetLen)
    {

        /* minimum number of bytes */
        int minBytes = 1; // set to expected minimum number of bytes
        if (packetLen < minBytes) {
            return minBytes | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
        }

        /* TK102B: check for packet */
        if (packet[0] == '[') {
            // TK102B: "[!0000000001.(...)]"
            // -- at least 14 bytes, to include the "(" character
            if (packetLen < 14) {
                // -- read at least up to "(" char
                // - required to skip past binary length byte at index-12
                return 14 | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
            }
            // -- indicated packet length
            // - the packet lengths appear to be specified incorrectly in the packet
            //   so do not rely on this stated packet length
            //int pktLen = (int)packet[12] & 0xFF;
            //if (packetLen < (13 + pktLen)) {
            //    return (13 + pktLen) | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
            //}
            // -- check for end of packet
            if (packet[packetLen - 1] == ']') {
                // -- found end-of-packet
                this.tkDeviceType = TKDeviceType.TK102B;
                return packetLen;
            } else {
                // -- read next byte
                return (packetLen + 1) | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
            }
        }

        /* TK103-3/VJoy: check for packet */
        if (packet[0] == '(') {
            // TK103-3: "(...)"
            // VJoy   : "(...,...,...)"
            // PacketTerminator: ')'
            if (packetLen == 1) {
                // -- read next byte
                return (packetLen + 1) | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
            } else
            if (packet[packetLen - 1] == ')') {
                // -- found end-of-packet, default to TK103_3
                if (IsDeviceTypeUnknown(this.tkDeviceType)) {
                    this.tkDeviceType = TKDeviceType.TK103_3;
                }
                // -- end-of-packet
                return packetLen;
            } else {
                // -- check for VJoy comma
                if ((packet[packetLen - 1] == ',') && IsDeviceTypeUnknown(this.tkDeviceType)) {
                    // -- found comma, set to type VJoy
                    // -  (comment this line to force this packet type to parse as TK103-3)
                    this.tkDeviceType = TKDeviceType.VJOY;
                }
                // -- read next byte
                return (packetLen + 1) | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
            }
        }

        /* TKnano: check for packet */
        // EXPERIMENTAL: may not be fully supported
        if (packet[0] == '*') { // *HQ,...
            // TKnano-1: "*HQ,...#"
            // PacketTerminator: #
            this.tkDeviceType = TKDeviceType.TKnano_1;
            if ((packetLen > 1) && (packet[packetLen - 1] == '#')) {
                // -- found end of packet
                return packetLen;
            } else {
                // -- read next byte
                return (packetLen + 1) | ServerSocketThread.PACKET_LEN_INCREMENTAL_;
            }
        } else
        if (packet[0] == '$') {
            // TKnano-2: may be a TKnano-2 binary packet
            this.tkDeviceType = TKDeviceType.TKnano_2;
            return 32; // fixed length
        }

        /* TK103-2: check for packet */
        if (packet[0] == '#') {
            // TK103-2: may be a TK103-2 header packet
            // PacketTerminator: ';'
            // IE: "##,imei:123451042191239,A;"
            this.tkDeviceType = TKDeviceType.TK103_2;
            if (PACKET_LEN_END_OF_STREAM) {
                return ServerSocketThread.PACKET_LEN_END_OF_STREAM;
            } else {
                // -- should instead explicitly look for the ';' terminator?
                return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
            }
        } else
        if ((packet[0] == 'i') || (packet[0] == 'I')) {
            // TK103-2: may be a TK103-2 data packet
            // PacketTerminator: ';'
            // IE: "imei:123451042191239,tracker,1107090553,9735551234,F,215314.000,A,4103.7641,N,14244.9450,W,0.08,;"
            this.tkDeviceType = TKDeviceType.TK103_2;
            if (PACKET_LEN_END_OF_STREAM) {
                return ServerSocketThread.PACKET_LEN_END_OF_STREAM;
            } else {
                // -- should instead explicitly look for the ';' terminator?
                return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
            }
        }

        /* consume unrecognized single bytes */
        if (packetLen == 1) {
            byte b = packet[0];
            if (b <= ' ') {
                // -- discard single space/control char
                return 1; // consume/ignore
            }
        }

        /* TK102: default */
        // "Character.isDigit((char)packet[0])" should be true at this point
        this.tkDeviceType = TKDeviceType.TK102; // could also be TK103_1
        if (PACKET_LEN_END_OF_STREAM) {
            return ServerSocketThread.PACKET_LEN_END_OF_STREAM;
        } else {
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Workhorse of the packet handler.  Parse/insert event data.
    **/
    public byte[] getHandlePacket(byte pktBytes[]) 
    {

        /* empty packet */
        if (ListTools.isEmpty(pktBytes)) {
            Print.logWarn("Ignoring empty/null packet");
            return null;
        }

        /* device model type */
        //Print.logDebug("Device Type: " + this.tkDeviceType);

        /* invalid length */
        if (pktBytes.length < 11) {
            if ((pktBytes.length == 1) && !IsDeviceTypeUnknown(this.tkDeviceType)) {
                // quietly consume single byte for known device types
                // (likely garbage characters between packets)
                //Print.logWarn("Ignoring byte: 0x" + StringTools.toHexString(pktBytes));
            } else {
                Print.logError("Unexpected packet length ("+this.tkDeviceType+"): " + pktBytes.length);
            }
            return null;
        }

        /* reset event count (necessary when receiving multiple records via TCP) */
        this.clearSavedEventCount();

        /* debug/header */
        Print.logInfo("Recv: " + StringTools.toStringValue(pktBytes,'.'));
        if (!StringTools.isPrintableASCII(pktBytes)) {
        Print.logInfo("Hex : 0x" + StringTools.toHexString(pktBytes)); 
        }
        String s = StringTools.toStringValue(pktBytes).trim();

        /* TK103-2: keep-alive packet? */
        if (s.startsWith("##")) {
            // TK103-2: keep-alive packet?
            //   ##,imei:123451042191239,A;
            Print.logInfo("TK103-2 Header: " + s); // debug message
            return "LOAD".getBytes(); // ACK "Load"
        } else
        if (s.startsWith("imei:")) {
            // TK103-2: data packet
            //   imei:123451042191239,tracker ,1107090553,9735551234,F,215314.000,A,4103.7641,N,14244.9450,W,0.08,;
            return this.parseInsertRecord_TK103_2(s); // TK103-2
        }

        /* TK103-3/VJoy: data packet? */
        if (s.startsWith("(")) {
            // -- make sure 'tkDeviceType' is set
            if (this.tkDeviceType == null) {
                this.tkDeviceType = (s.indexOf(",") >= 0)? 
                    TKDeviceType.VJOY :     // contains commas
                    TKDeviceType.TK103_3;   // no commas
            }
            // -- parse for TK103-3 or VJoy
            if (this.tkDeviceType.equals(TKDeviceType.VJOY)) {
                return this.parseInsertRecord_VJoy(s); // VJoy
            } else {
                return this.parseInsertRecord_TK103_3(s); // TK103-3
            }
        }

        /* TKnano-1/2: data packet? */
        // EXPERIMENTAL: may not be supported
        if (s.startsWith("*")) { // *HQ,...
            // assume  TKnano-1 ASCII format
            byte nanoACK[] = null;
            nanoACK = this.parseInsertRecord_TKnano_1(s); // TKnano ASCII
            // may not be fully supported
            return nanoACK;
        } else
        if (s.startsWith("$")) { // 0x24...
            // assume TKnano-2 Binary format
            byte nanoACK[] = null;
            // may not be fully supported
            return nanoACK;
        }

        /* TK103-1 "ON" response */
        if ((s.length() == 15) && StringTools.isNumeric(s)) {
            if (IsDeviceTypeUnknown(this.tkDeviceType) || IsDeviceTypeTK102(this.tkDeviceType)) {
                // we know this device is likely a TK103-1
                this.tkDeviceType = TKDeviceType.TK103_1;
            }
            Print.logInfo("Sending TK103-1 IMEI# response 'ON' ...");
            return "ON".getBytes(); // ACK "ON"
        }

        /* TK102B */
        if (s.startsWith("[")) {
            this.tkDeviceType = TKDeviceType.TK102B;
            return this.parseInsertRecord_TK102B(s); // TK102B
        }

        /* default to TK102 or TK103-1 */
        return this.parseInsertRecord_TK102(s); // TK102

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // TK103-2:
    /**
    *** TK103-2: parse and insert data record 
    **/
    private byte[] parseInsertRecord_TK103_2(String s) // handleCommon
    {
        Print.logInfo("Parsing: TK103-2 ("+this.tkDeviceType+") ...");

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s, ',');
        if (fld == null) {
            // -- will not occur
            Print.logWarn("Fields are null");
            return null;
        } else
        if (fld.length < 2) {
            // -- must have at least "imei:" and eventCode at this point
            Print.logWarn("Invalid number of fields: " + fld.length);
            return null;
        }


        /* get "imei:" */
        if (fld[0].startsWith("imei:")) {
            this.tkModemID = fld[0].substring("imei:".length()).trim();
        }
        if (StringTools.isBlank(this.tkModemID)) {
            Print.logError("'imei:' value is missing");
            return null;
        }

        /* event code */
        String eventCode = StringTools.trim(fld[1]);
        if (eventCode.endsWith("!")) { 
            // -- remove trailing "!", if present
            eventCode = eventCode.substring(0,eventCode.length()-1); 
            // -- IE: "help me!" ==> "help me", etc.
        }

        /* minimum field length */
        if (eventCode.equalsIgnoreCase("OBD")) {
            if (fld.length < 13) { // DTC codes are optional
                Print.logWarn("Invalid number of fields: " + fld.length);
                return null;
            }
        } else {
            if (fld.length < 12) {
                Print.logWarn("Invalid number of fields: " + fld.length);
                return null;
            }
        }

        /* get time */
        long fixtime = 0L;
        {
            // -- local scope
            long locYMDhms = 0L;
            if (fld[2].length() >= 12) {
                locYMDhms = StringTools.parseLong(fld[2].substring(0,12),0L);
            } else
            if (fld[2].length() >= 10) {
                locYMDhms = StringTools.parseLong(fld[2].substring(0,10),0L);
                locYMDhms *= 100L;
            } else {
                locYMDhms = 0L;
            }
            if (eventCode.equalsIgnoreCase("OBD")) {
                fixtime = this._parseDate_YYMMDDhhmmss(locYMDhms);
            } else {
                long gmtHMS = StringTools.parseLong(fld[5],-1L);
                if (gmtHMS >= 0L) {
                    // -- GPS time appears to be available
                    fixtime = this._parseDate_YMDhms_HMS(locYMDhms, gmtHMS);
                } else {
                    // -- GPS time is not available
                    fixtime = this._parseDate_YYMMDDhhmmss(locYMDhms);
                }
            }
        }
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[2] + "/" + fld[5] + " (using current time)");
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* GPS values */
        boolean   validGPS    = false;
        double    latitude    = 0.0;
        double    longitude   = 0.0;
        double    headingDeg  = -1.0;
        double    speedKPH    = -1.0;
        double    altitudeM   = 0.0;
        double    odomKM      = 0.0;
        double    batteryV    = 0.0;
        double    engTempC    = 0.0;
        String    engDTC[]    = null;
        long      gpsAge      = 0L;
        double    HDOP        = 0.0;
        int       numSats     = 0;
        long      gpioInput   = -1L;
        CellTower servingCell = null;

        /* parse event */
        if (eventCode.equalsIgnoreCase("OBD")) {
            // -- parse custom OBD data (TODO)
        } else 
        if (fld[4].equals("L")) {
            // -- parse Mobile location information
            // -  Many thanks to Franjieh El Khoury for this information.
            int MCC = 0; // -- this may need to be manually filled in
            int MNC = 0; // -- this may need to be manually filled in
            int LAC = (fld.length > 7)? StringTools.parseInt(fld[7],0) : -1;
            int CID = (fld.length > 9)? StringTools.parseInt(fld[9],0) : 0;
            if ((MCC >= 0) && (MNC >= 0) && (LAC >= 0) && (CID > 0)) {
                servingCell = new CellTower();
                servingCell.setMobileCountryCode(MCC);
                servingCell.setMobileNetworkCode(MNC);
                servingCell.setLocationAreaCode(LAC);
                servingCell.setCellTowerID(CID);
            }
        } else { // (fld[4].equals("F"))
            // -- parse GPS information
            validGPS    = fld[6].equalsIgnoreCase("A");
            latitude    = validGPS? this._parseLatitude( fld[7], fld[ 8])  : 0.0;
            longitude   = validGPS? this._parseLongitude(fld[9], fld[10])  : 0.0;
            double knts = (validGPS && (fld.length > 11))? StringTools.parseDouble(fld[11], -1.0) : -1.0;
            headingDeg  = (validGPS && (fld.length > 12))? StringTools.parseDouble(fld[12], -1.0) : -1.0;
            speedKPH    = (knts >= 0.0)? (knts * KILOMETERS_PER_KNOT)   : -1.0;
        }

        /* valid lat/lon? */
        if (validGPS && !GeoPoint.isValid(latitude,longitude)) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            validGPS  = false;
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        /* adjust speed, calculate approximate heading if not available in packet */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            //Print.logInfo("Actual Speed: " + speedKPH);
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            //headingDeg = 0.0;   <== leave as '-1'
        }

        /* status code */
        int statusCode = StatusCodes.STATUS_LOCATION;
        DCServerConfig dcs = Main.getServerConfig();
        if ((dcs != null) && !StringTools.isBlank(eventCode)) {
            int code = dcs.translateStatusCode(eventCode, -9999);
            if (code == -9999) {
                // -- default 'statusCode' is StatusCodes.STATUS_LOCATION
            } else {
                statusCode = code;
            }
        }

        /* timestamp adjustments based on the event code */
        // -- may be necessary if the "acc on"/"acc off" occur at the same GPS time
        if (statusCode == StatusCodes.STATUS_IGNITION_OFF) {
            // -- subtract one second to make sure it comes before any following "acc on"
            fixtime--;
        }

        // ------------------------------------------------
        // TK103-2: Common data handling below 
        this.handleCommon(this.tkModemID, 
            fixtime, statusCode, null,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, headingDeg, altitudeM, odomKM,
            gpioInput, batteryV, 0.0/*battLvl*/, 
            engTempC, engDTC,
            servingCell);

        /* return ACK */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // TK103-3:
    /**
    *** TK103-3: parse and insert data record 
    **/
    private byte[] parseInsertRecord_TK103_3(String s) // handleCommon
    {
        Print.logInfo("Parsing: TK103-3 ("+this.tkDeviceType+") ...");

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        } else
        if (!s.startsWith("(")) {
            Print.logError("Does not start with '('");
            return null;
        }

        /* trailing ")" */
        int RP = s.endsWith(")")? 1 : 0;

        /* parse header */
        String runNumStr = s.substring( 1,13);
        String msgType   = s.substring(13,17);

        /* event code */
        String eventCode = "";

        /* ACK */
        boolean rtnACK = false;
        String  ack    = "(" + runNumStr + "AP01HSO)";
        byte    ackB[] = ack.getBytes();

        /* parse TK103-alt packet type */
        int G;
        if (msgType.equals("BP05")) {
            this.tkModemID = s.substring(17,32);
            G = 32; // BP05
        } else 
        if (msgType.equals("BR00") || 
            msgType.equals("BR02") || 
            msgType.equals("BP04")) {
            // modem-id should have been specified in a previous packet
            if (StringTools.isBlank(this.tkModemID)) {
                Print.logWarn("No ModemID provided by previous BP05 record.");
            }
            G = 17;
        } else
        if (msgType.equals("BP00")) {
            Print.logWarn("Returning ACK: " + ack);
            return ackB; // always return ACK here
        } else
        if (msgType.equals("BO01")) {
            // modem-id should have been specified in a previous packet
            if (StringTools.isBlank(this.tkModemID)) {
                Print.logWarn("No ModemID provided by previous BP05 record.");
            }
            eventCode = s.substring(17,18);
            G = 18;
        } else {
            Print.logWarn("Unsupported message type: " + msgType);
            return rtnACK? ackB : null;
        }

        // recheck packet length
        int expectedLen = G + 62 + RP;
        if (s.length() < expectedLen) {
            Print.logError("Unexpected packet length: " + s.length() + " [expected " + expectedLen + "]");
            return rtnACK? ackB : null;
        }

        /* GPS Data */
        String   dateStr    = s.substring(G+ 0,G+ 6);                                              //  0, 6  [YYMMDD]
        boolean  validGPS   = s.substring(G+ 6,G+ 7).equalsIgnoreCase("A");                        //  6, 7  [A/V]
        double   latitude   = this._parseLatitude( s.substring(G+ 7,G+16),s.substring(G+16,G+17)); //  7,16, 16:17
        double   longitude  = this._parseLongitude(s.substring(G+17,G+27),s.substring(G+27,G+28)); // 17,27, 27:28
        double   speedKPH   = StringTools.parseDouble(s.substring(G+28,G+33),0.0);                 // 28,33
        String   timeStr    = s.substring(G+33,G+39);                                              // 33,39
        long     fixtime    = this._getUTCSeconds_YMD_HMS(dateStr, timeStr); // UTC
        double   headingDeg = StringTools.parseDouble(s.substring(G+39,G+45),0.0);                 // 39,45
        String   gpioStr    = s.substring(G+45,G+53);                                              // 45,53
        String   odomUnits  = s.substring(G+53,G+54);                                              // 53,54 "L"
        long     odomVal    = StringTools.parseHexLong(s.substring(G+54,G+62),0L);                 // 54,62 Miles?
        double   altitudeM  = 0.0;
        GeoPoint geoPoint   = new GeoPoint(latitude,longitude);
        long     gpsAge     = 0L;
        double   batteryV   = 0.0;
        double   engTempC   = 0.0;
        double   HDOP       = 0.0;
        int      numSats    = 0;

        /* validate GPS */
        if (validGPS && !geoPoint.isValid()) {
            validGPS = false;
        }

        /* GPIO input */
        long gpioInput = -1L;
        if (gpioStr.length() >= 8) {
            // According to the doc, theese characters should all be "0" or "1",
            // however, the following were observed as possible values:
            //  01100530
            //  00000007
            gpioInput = 0L;
            for (int i = 0; i <= 7; i++) {
                char B = gpioStr.charAt(7 - i); // 
                if (B != '0') { // should be '1' or '0'
                    gpioInput |= (1L << i);
                }
            }
            // Power on/off : 0x80 (0=On, 1=Off)  (yes, this is what the doc says)
            // Acc on/off   : 0x40 (0=Off, 1=On)
        }

        /* odometer */
        double odomKM = 0.0;
        if (odomUnits.equalsIgnoreCase("L")) {
            odomKM = (double)odomVal / 1000.0;
        }

        /* adjustments to speed/heading */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            //Print.logInfo("Actual Speed: " + speedKPH);
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* status code */
        int statusCode = StatusCodes.STATUS_LOCATION;
        DCServerConfig dcs = Main.getServerConfig();
        if ((dcs != null) && !StringTools.isBlank(eventCode)) {
            // 0=PowerOff, 1=Arrive, 2=SOS, 3=AntiTheft, 4=LowerSpeed?, 5=Overspeed, 6=Depart
            int code = dcs.translateStatusCode(eventCode, -9999); // String
            if (code == -9999) {
                switch (eventCode.charAt(0)) {
                    case '0': statusCode = StatusCodes.STATUS_POWER_OFF;           break;
                    case '1': statusCode = StatusCodes.STATUS_GEOBOUNDS_ENTER;     break;
                    case '2': statusCode = StatusCodes.STATUS_PANIC_ON;            break;
                    case '3': statusCode = StatusCodes.STATUS_INTRUSION_ON;        break;
                    case '4': statusCode = StatusCodes.STATUS_LOCATION;            break;
                    case '5': statusCode = StatusCodes.STATUS_MOTION_EXCESS_SPEED; break;
                    case '6': statusCode = StatusCodes.STATUS_GEOBOUNDS_EXIT;      break;
                }
            } else {
                statusCode = code;
            }
        }

        // ------------------------------------------------
        // TK103-3: Common data handling below 
        this.handleCommon(this.tkModemID, 
            fixtime, statusCode, null,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, headingDeg, altitudeM, odomKM,
            gpioInput, batteryV, 0.0/*battLvl*/, 
            engTempC, null/*engDTC*/,
            null/*CellTower*/);

        /* return ACK */
        return rtnACK? ackB : null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // VJoy: ASCII (EXPERIMENTAL: may not be fully supported)
    /**
    *** VJoy-1: (ASCII) parse and insert data record <br>
    *** (EXPERIMENTAL: may not be fully supported)
    **/
    private byte[] parseInsertRecord_VJoy(String s) // handleCommon
    {
        Print.logInfo("Parsing: VJoy ("+this.tkDeviceType+") ...");

        /* pre-validate */
        if (StringTools.isBlank(s)) {
            //Print.logError("String is blank/null");
            return null;
        } else
        if (!s.startsWith("(")) {
            Print.logError("Does not start with '('");
            return null;
        }

        /* remove prefixing '(' and trailing ')' (if present) */
        if (s.endsWith(")")) {
            s = s.substring(1,s.length()-1);
        } else {
            s = s.substring(1);
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s, ',');
        if (fld == null) {
            // -- will not occur
            Print.logWarn("Fields are null");
            return null;
        } else
        if (fld.length < 2) {
            Print.logWarn("Invalid number of fields: " + fld.length);
            return null;
        }

        /* mobile-id */
        this.tkModemID = StringTools.trim(fld[0]);
        
        /* ACK */
        // -- document indicates this should be returned to indicate "Upload succeeded"
        byte ackPkt[] = "ok".getBytes();

        /* event code */
        int statusCode = StatusCodes.STATUS_LOCATION;
        String  evCode = StringTools.trim(fld[1]).toUpperCase();
        if (evCode.equals("BP05")) {
            // -- LLC format
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (evCode.equals("ZC07")) {
            // -- LLC format
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (evCode.equals("ZC08")) {
            // -- Stoptrack format
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (evCode.equals("ZC09")) {
            // -- Intervaltrack format
            statusCode = StatusCodes.STATUS_LOCATION;
        } else
        if (evCode.equals("ZC11")) {
            // -- Alarm
            statusCode = StatusCodes.STATUS_ALARM_ON;
        } else
        if (evCode.equals("ZC12")) {
            // -- Low Battery
            statusCode = StatusCodes.STATUS_LOW_BATTERY;
        } else
        if (evCode.equals("ZC13")) {
            // -- Power Cut Alert
            statusCode = StatusCodes.STATUS_POWER_ALARM;
        } else {
            // -- not supported
            Print.logWarn("EventCode not supported: " + evCode);
            return ackPkt;
        }

        /* check minimum length for GPS */
        // (013632651491,ZC13,040613,A,2234.0297N,11405.9101E,000.0,040137,178.48)
        //  0----------- 1--- 2----- 3 4--------- 5---------- 6---- 7----- 8-----
        if (fld.length < 9) {
            Print.logWarn("Invalid number of fields: " + fld.length);
            return ackPkt;
        }

        /* date/time */
        long ddmmyy  = StringTools.parseLong(fld[2],0L);   // DDMMYY
        long hhmmss  = StringTools.parseLong(fld[7],0L);   // hhmmss
        long fixtime = this._parseDate_ddmmyy_hhmmss(ddmmyy, hhmmss);
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[2] + "/" + fld[7]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* latitude/longitude */
        boolean  validGPS  = fld[3].equalsIgnoreCase("A");
        double   latitude  = Nmea0183.ParseLatitude( fld[4]); // , 0.0);
        double   longitude = Nmea0183.ParseLongitude(fld[5]); // , 0.0);
        GeoPoint geoPoint  = validGPS? new GeoPoint(latitude,longitude) : GeoPoint.INVALID_GEOPOINT;
        long     gpsAge    = 0L;
        double   HDOP      = 0.0;
        int      numSats   = 0;
        double   altitudeM = 0.0;

        /* speed/heading */
        double speedKPH   = StringTools.parseDouble(fld[6],0.0); // km/h
        double headingDeg = StringTools.parseDouble(fld[8],0.0); // degrees
        if (speedKPH < MINIMUM_SPEED_KPH) {
            //Print.logInfo("Actual Speed: " + speedKPH);
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* misc */
        double odomKM    = 0.0;
        long   gpioInput = -1L;

        // ------------------------------------
        // VJoy: Common data handling below 
        this.handleCommon(this.tkModemID, 
            fixtime, statusCode, null/*statusCodeSet*/,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, headingDeg, altitudeM, odomKM,
            gpioInput, 0.0/*batteryV*/, 0.0/*battLvl*/, 
            0.0/*engTempC*/, null/*engDTC*/,
            null/*CellTower*/);

        /* return ACK */
        return ackPkt;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // TKnano: (EXPERIMENTAL: may not be fully supported)

    /* reserved GPIO for the TK-nano (if implemented) */
    public static       long    TKNANO_GPIO_ACC             = 0x0001L;
    public static       long    TKNANO_GPIO_ENGINE          = 0x0002L;
    public static       long    TKNANO_GPIO_ON_BATTERY      = 0x0008L;
    public static       long    TKNANO_GPIO_SENSOR_1        = 0x0010L;
    public static       long    TKNANO_GPIO_SENSOR_2        = 0x0020L;

    // ----------------------------------------------------------
    // TKnano-1: ASCII (EXPERIMENTAL: may not be fully supported)
    /**
    *** TKnano-1: (ASCII) parse and insert data record <br>
    *** (EXPERIMENTAL: may not be supported)
    **/
    private byte[] parseInsertRecord_TKnano_1(String s) // handleCommon
    {
        Print.logInfo("Parsing: TKnano-1 ("+this.tkDeviceType+") ...");

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        } else
        if (!s.startsWith("*")) {
            Print.logError("Does not start with '*'");
            return null;
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s, ',');
        if (fld == null) {
            // will not occur
            Print.logWarn("Fields are null");
            return null;
        } else
        if (fld.length < 13) {
            Print.logWarn("Invalid number of fields: " + fld.length);
            return null;
        }

        /* Mobile ID */
        this.tkModemID = fld[1];

        /* event code (Command) */
        String eventCode = StringTools.trim(fld[2]); // Command "V1"

        /* date/time */
        long hhmmss  = StringTools.parseLong(fld[ 3],0L);   // hhmmss
        long ddmmyy  = StringTools.parseLong(fld[11],0L);   // ddmmyy
        long fixtime = this._parseDate_ddmmyy_hhmmss(ddmmyy, hhmmss);
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[3] + "/" + fld[11]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* latitude/longitude */
        boolean  validGPS  = fld[4].equalsIgnoreCase("A");
        double   latitude  = Nmea0183.ParseLatitude( fld[ 5], fld[ 6], 0.0);
        double   longitude = Nmea0183.ParseLongitude(fld[ 7], fld[ 8], 0.0);
        GeoPoint geoPoint  = validGPS? new GeoPoint(latitude,longitude) : GeoPoint.INVALID_GEOPOINT;
        long     gpsAge    = 0L;
        double   HDOP      = 0.0;
        int      numSats   = 0;
        double   altitudeM = 0.0;

        /* speed/heading */
        double speedKPH   = StringTools.parseDouble(fld[ 9],0.0) * KILOMETERS_PER_KNOT;
        double headingDeg = StringTools.parseDouble(fld[10],0.0);
        if (speedKPH < MINIMUM_SPEED_KPH) {
            //Print.logInfo("Actual Speed: " + speedKPH);
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* vehicle status */
        long vehStatusNeg = StringTools.parseHexLong(fld[12],0L);

        // -------------------------------------------
        // ASCII parsing complete, handle values below

        /* misc fields (not available) */
        double batteryV  = 0.0;
        double engTempC  = 0.0;
        double odomKM    = 0.0;

        /* parse default status codes */
        long gpioInput = -1L;
        int statusCode = StatusCodes.STATUS_LOCATION;
        HashSet<Integer> statCodeSet = null;
        // handle "vehStatusNeg" here
        /* eventCode=>statusCode map */
        DCServerConfig dcs = Main.getServerConfig();
        if ((dcs != null) && !StringTools.isBlank(eventCode)) {
            // "V1" = General Message
            // "V4" = Confirmation Message
            int code = dcs.translateStatusCode(eventCode, -9999); // String
            if (code == -9999) {
                // leave as default
            } else {
                statusCode = code;
            }
        }

        // ------------------------------------
        // TKnano-1: Common data handling below 
        this.handleCommon(this.tkModemID, 
            fixtime, statusCode, statCodeSet,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, headingDeg, altitudeM, odomKM,
            gpioInput, batteryV, 0.0/*battLvl*/, 
            engTempC, null/*engDTC*/,
            null/*CellTower*/);

        /* return ACK */
        return null;

    }

    // -----------------------------------------------------
    // TKnano-2: Binary (EXPERIMENTAL: may not be supported)
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // TK102B/TK102-3: (TCP)
    /**
    *** TK102-2: parse and insert data record 
    **/
    private byte[] parseInsertRecord_TK102B(String s) // handleCommon
    {
        Print.logInfo("Parsing: TK102B ("+this.tkDeviceType+") ...");

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        } else
        if (!s.startsWith("[") || !s.endsWith(")]")) {
            Print.logError("Unrecognized packet header/trailer");
            return null;
        } else
        if ((s.length() <= 13) || (s.charAt(13) != '(')) {
            Print.logError("Expected '(' at column 13 not found");
            return null;
        }

        /* second character */
        char header = s.charAt(1);

        /* sequence number */
        String seqStr = s.substring(2,12);

        /* packet length */
        // int pktLen = (byte)s.charAt(12);

        /* IMEI packet? */
        if (header == '!') { // 0x21
            // assume IMEI starts at column 14
            int imeiS = 14;
            int imeiE = s.indexOf(",",imeiS);
            int imeiL = imeiE - imeiS;
            if ((imeiL <= 0) || imeiL > 15) {
                Print.logError("Invalid IMEI found");
                return null;
            }
            this.tkModemID = s.substring(imeiS,imeiE);
            StringBuffer ack = new StringBuffer();
            ack.append("[\"0000000001");
            ack.append(s.substring(13));
            return ack.toString().getBytes();
        }

        /* "Quit request" */
        if (header == '#') { // 0x23
            // -- Quit request
            StringBuffer ack = new StringBuffer(s);
            ack.setCharAt(1,'$'); // 0x24
            return ack.toString().getBytes();
        }

        /* skip unsupported packet headers */
        if (header == '%') { // 0x25
            return null; // 0x26
        } else
        if (header == 'J') { // 0x4A
            return null; // 0x4A
        }

        /* accept only '=' ';' here */
        if (header != ';') { // 0x3B
            // Ok, continue
        } else
        if (header != '=') { // 0x3D
            // Ok, continue
        } else {
            Print.logWarn("Ignoring unrecognized packet header");
            return null;
        }

        /* skip unrecognized packets */
        if (s.length() < 66) {
            Print.logError("Insufficient packet length (expect 66): " + s.length());
            return null;
        }

        /* parse */
        String   timeStr    = s.substring(17,23); // hhmmss
        boolean  validGPS   = s.substring(23,24).equalsIgnoreCase("A");
        double   latitude   = this._parseLatitude( s.substring(24,33),s.substring(33,34));
        double   longitude  = this._parseLongitude(s.substring(34,44),s.substring(44,45));
        double   speedKPH   = StringTools.parseDouble(s.substring(45,50),0.0) * KILOMETERS_PER_KNOT;
        double   headingDeg = StringTools.parseDouble(s.substring(50,52),0.0) * 10.0;
        String   dateStr    = s.substring(52,58); // DDMMYY
        double   battLvl    = StringTools.parseDouble(s.substring(58,61),0.0) / 100.0;
        double   batteryV   = 0.0;
        double   engTempC   = 0.0;
        long     fixtime    = this._getUTCSeconds_DMY_HMS(dateStr, timeStr);
        double   altitudeM  = 0.0;
        GeoPoint geoPoint   = new GeoPoint(latitude,longitude);
        long     gpsAge     = 0L;
        double   odomKM     = 0.0;
        double   HDOP       = 0.0;
        int      numSats    = 0;
        long     gpioInput  = -1L;

        /* validate GPS */
        if (validGPS && !geoPoint.isValid()) {
            validGPS = false;
        }

        /* adjustments to speed/heading */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            //Print.logInfo("Actual Speed: " + speedKPH);
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* status code */
        int statusCode = StatusCodes.STATUS_LOCATION;

        // ------------------------------------------------
        // TK102B: Common data handling below 
        this.handleCommon(this.tkModemID, 
            fixtime, statusCode, null,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, headingDeg, altitudeM, odomKM,
            gpioInput, batteryV, battLvl, 
            engTempC, null/*engDTC*/,
            null/*CellTower*/);

        /* no ack? */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // TK102: (TCP)
    /**
    *** TK102: parse and insert data record 
    **/
    private byte[] parseInsertRecord_TK102(String s) // handleCommon
    {
        Print.logInfo("Parsing: TK102 ("+this.tkDeviceType+") ...");

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        }

        /* parse to fields */
        String fld[] = StringTools.parseStringArray(s, ',');
        if (fld == null) {
            // will not occur
            Print.logWarn("Fields are null");
            return null;
        } else
        if (fld.length < 15) {
            Print.logWarn("Invalid number of fields: " + fld.length);
            return null;
        }

        /* find "imei:" */
        int imeiNdx = -1;
        for (int f = 0; f < fld.length; f++) {
            if (fld[f].startsWith("imei:")) {
                this.tkModemID = fld[f].substring("imei:".length()).trim();
                imeiNdx = f;
                break;
            }
        }
        if (StringTools.isBlank(this.tkModemID)) {
            Print.logError("'imei:' value is missing");
            return null;
        }

        /* find "GPRMC" */
        int gpx = 0;
        for (; (gpx < fld.length) && !fld[gpx].equalsIgnoreCase("GPRMC"); gpx++);
        if (gpx >= fld.length) {
            Print.logError("'GPRMC' not found");
            return null;
        } else
        if ((gpx + 12) >= fld.length) {
            Print.logError("Insufficient 'GPRMC' fields");
            return null;
        }

        /* parse data following GPRMC */
        long    hms         = StringTools.parseLong(fld[gpx + 1], 0L);
        long    dmy         = StringTools.parseLong(fld[gpx + 9], 0L);
        long    fixtime     = this._getUTCSeconds_DMY_HMS(dmy, hms);
        boolean validGPS    = fld[gpx + 2].equalsIgnoreCase("A");
        double  latitude    = validGPS? this._parseLatitude( fld[gpx + 3], fld[gpx + 4])  : 0.0;
        double  longitude   = validGPS? this._parseLongitude(fld[gpx + 5], fld[gpx + 6])  : 0.0;
        double  knots       = validGPS? StringTools.parseDouble(fld[gpx + 7], -1.0) : 0.0;
        double  headingDeg  = validGPS? StringTools.parseDouble(fld[gpx + 8], -1.0) : 0.0;
        double  speedKPH    = (knots >= 0.0)? (knots * KILOMETERS_PER_KNOT)   : -1.0;
        double  odomKM      = 0.0;
        long    gpsAge      = 0L;
        double  HDOP        = 0.0;
        long    gpioInput   = -1L;
        double  engTempC    = 0.0;

        /* number of satellites */
        int     numSats     = (fld.length > (imeiNdx+1))? StringTools.parseInt(fld[imeiNdx+1],0) : 0;
        if (numSats > 13) { numSats = 0; } // I've seen "100" in this field

        /* altitude */
        String  altMStr     = (fld.length > (imeiNdx+2))? fld[imeiNdx+2] : "";
        double  altitudeM   = StringTools.parseDouble(altMStr,0.0); // meters?

        /* battery voltage */
        // imei:353451042083525, 04, 259.7, F:4.06V, 0, 138,64306,310,26,99E8,343B
        String battVStr = (fld.length > (imeiNdx+3))? fld[imeiNdx+3] : "";
        double batteryV = 0.0;
        if (battVStr.startsWith("F:") || battVStr.startsWith("L:")) {
            batteryV = StringTools.parseDouble(battVStr.substring(2), 0.0);
        }
        boolean charging = (fld.length > (imeiNdx+4))? fld[imeiNdx+4].equals("1") : false;

        /* GPRS string length? */
        int GPRSLen = (fld.length > (imeiNdx+5))? StringTools.parseInt(fld[imeiNdx+5],0) : 0;

        /* CRC? */
        int CRC = (fld.length > (imeiNdx+6))? StringTools.parseInt(fld[imeiNdx+6],0) : 0;

        /* MCC,MNC,LAC,CID */
        int MCC = (fld.length > (imeiNdx+ 7))? StringTools.parseInt(   fld[imeiNdx+ 7],0) : 0;
        int MNC = (fld.length > (imeiNdx+ 8))? StringTools.parseInt(   fld[imeiNdx+ 8],0) : 0;
        int LAC = (fld.length > (imeiNdx+ 9))? StringTools.parseHexInt(fld[imeiNdx+ 9],0) : 0;
        int CID = (fld.length > (imeiNdx+10))? StringTools.parseHexInt(fld[imeiNdx+10],0) : 0;
        CellTower servingCell = null;
        if ((MCC >= 0) && (MNC >= 0) && (LAC >= 0) && (CID > 0)) {
            servingCell = new CellTower();
            servingCell.setMobileCountryCode(MCC);
            servingCell.setMobileNetworkCode(MNC);
            servingCell.setLocationAreaCode(LAC);
            servingCell.setCellTowerID(CID);
        }

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[gpx + 9] + "/" + fld[gpx + 1]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* event code */
        String eventCode = ((gpx + 14) < fld.length)? StringTools.trim(fld[gpx + 14]) : "";

        /* valid lat/lon? */
        boolean gpxValid = ((gpx + 13) < fld.length)? fld[gpx + 13].equalsIgnoreCase("F") : false;
        if (validGPS && !GeoPoint.isValid(latitude,longitude)) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            validGPS  = false;
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        /* Oil Level/Temperature/RFID */
        // -- if supported
        // all fields have been extracted from the packet at this point 
        // ------------------------------------------------------------

        /* adjustments to received values */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            //Print.logInfo("Actual Speed: " + speedKPH);
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* status code */
        int statusCode = StatusCodes.STATUS_LOCATION;
        DCServerConfig dcs = Main.getServerConfig();
        if ((dcs != null) && !StringTools.isBlank(eventCode)) {
            int code = dcs.translateStatusCode(eventCode, -9999);
            if (code == -9999) {
                // default 'statusCode' is StatusCodes.STATUS_LOCATION
            } else {
                statusCode = code;
            }
        }

        /* CellTower location? */
        if (!validGPS && (statusCode == StatusCodes.STATUS_LOCATION) && (servingCell != null)) {
            statusCode = StatusCodes.STATUS_CELL_LOCATION;
        }

        // ------------------------------------------------
        // TK102: Common data handling below 
        this.handleCommon(this.tkModemID, 
            fixtime, statusCode, null,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, headingDeg, altitudeM, odomKM,
            gpioInput, batteryV, 0.0/*battLvl*/, 
            engTempC, null/*engDTC*/,
            servingCell);

        /* return ACK */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param yymmdd Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hhmmss Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    **/
    private long _getUTCSeconds_YMD_HMS(String yymmdd, String hhmmss)
    {
        // 100406  021359
        if ((yymmdd.length() < 6) || (hhmmss.length() < 6)) {
            return 0L;
        } else {
            int  YY = StringTools.parseInt(yymmdd.substring(0,2),-1);
            int  MM = StringTools.parseInt(yymmdd.substring(2,4),-1);
            int  DD = StringTools.parseInt(yymmdd.substring(4,6),-1);
            int  hh = StringTools.parseInt(hhmmss.substring(0,2),-1);
            int  mm = StringTools.parseInt(hhmmss.substring(2,4),-1);
            int  ss = StringTools.parseInt(hhmmss.substring(4,6),-1);
            if ((YY < 0) || (MM < 1) || (MM > 12) || (DD < 1) || (DD > 31) || 
                (hh < 0) || (mm < 0) || (ss < 0)) {
                return 0L;
            } else {
                long fixtime  = (new DateTime(gmtTimezone,YY+2000,MM,DD,hh,mm,ss)).getTimeSec();
                return fixtime;
            }
        }
    }

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy    Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hms    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    **/
    private long _getUTCSeconds_DMY_HMS(String ddmmyy, String hhmmss)
    {
        if ((ddmmyy.length() < 6) || (hhmmss.length() < 6)) {
            return 0L;
        } else {
            long dmy = StringTools.parseLong(ddmmyy,0L);
            long hms = StringTools.parseLong(hhmmss,0L);
            return this._getUTCSeconds_DMY_HMS(dmy, hms);
        }
    }

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy    Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hms    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    **/
    private long _getUTCSeconds_DMY_HMS(long dmy, long hms)
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

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param locYMDhms Date received from packet in YYMMDDhhmmss format, where DD is day, MM is month,
    ***                  YY is year, hh is the hour, mm is the minutes, and ss is the seconds.  
    ***                  Unfortunately, the device is allowed to be configured to specify this time 
    ***                  relative to the local timezone, rather than GMT.  This makes determining the
    ***                  actual time of the event more difficult.
    *** @param gmtHMS    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***                  and SS is second.  This time is assumed to be relative to GMT.
    *** @return Time in UTC seconds.
    **/
    private long _parseDate_YMDhms_HMS(long locYMDhms, long gmtHMS)
    {

        /* GMT time of day */
        int    gmtHH  = (int)((gmtHMS / 10000L) % 100L);
        int    gmtMM  = (int)((gmtHMS /   100L) % 100L);
        int    gmtSS  = (int)((gmtHMS         ) % 100L);
        long   gmtTOD = (gmtHH * 3600L) + (gmtMM * 60L) + gmtSS; // seconds of day
        //Print.logInfo("GMT HHMMSS: " + gmtHMS);
        //Print.logInfo("GMT HH="+gmtHH + " MM="+gmtMM + " SS="+gmtSS +" TOD="+gmtTOD);

        /* local time of day */
        int    locHH  = (int)((locYMDhms / 10000L) % 100L);
        int    locMM  = (int)((locYMDhms /   100L) % 100L);
        int    locSS  = (int)((locYMDhms         ) % 100L);
        long   locTOD = (locHH * 3600L) + (locMM * 60L) + locSS; // seconds of day
        //Print.logInfo("Loc HHMMSS: " + locYMDhms);
        //Print.logInfo("Loc HH="+locHH + " MM="+locMM + " SS="+locSS +" TOD="+locTOD);

        /* current day */
        long ymd = locYMDhms / 1000000L; // remove hhmmss
        long DAY;
        if (ymd > 0L) {
            int    dd = (int)( ymd           % 100L);
            int    mm = (int)((ymd /   100L) % 100L);
            int    yy = (int)((ymd / 10000L) % 100L) + 2000;
            long   yr = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY       = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
            long  dif = (locTOD >= gmtTOD)? (locTOD - gmtTOD) : (gmtTOD - locTOD); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (locTOD > gmtTOD) {
                    // locTOD > gmtTOD likely represents the next day
                    // ie 2011/07/10 23:00 ==> 01:00 (2011/07/11)
                    DAY++;
                } else {
                    // locTOD < gmtTOD likely represents the previous day
                    // ie 2011/07/10 01:00 ==> 23:00 (2011/07/09)
                    DAY--;
                }
            }
        } else {
            // we don't have the day, so we need to figure out as close as we can what it should be.
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= gmtTOD)? (tod - gmtTOD) : (gmtTOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (tod > gmtTOD) {
                    // tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }

        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + gmtTOD;
        return sec;

    }

    /** 
    *** Parses the specified date into unix 'epoch' time 
    **/
    private long _parseDate_ddmmyy_hhmmss(long ddmmyy, long hhmmss)
    {
        if ((ddmmyy <= 0L) || (hhmmss < 0L)) {
            return 0L;
        } else {
            //                      DDMMYY
            int DD = (int)((ddmmyy / 10000L) % 100L); // 18 day
            int MM = (int)((ddmmyy /   100L) % 100L); // 04 month
            int YY = (int)((ddmmyy /     1L) % 100L); // 11 year
            //                      hhmmss
            int hh = (int)((hhmmss / 10000L) % 100L); // 01 hour
            int mm = (int)((hhmmss /   100L) % 100L); // 48 minute
            int ss = (int)((hhmmss /     1L) % 100L); // 04 second
            DateTime dt = new DateTime(gmtTimezone,YY+2000,MM,DD,hh,mm,ss);
            return dt.getTimeSec();
        }
    }

    /** 
    *** Parses the specified date into unix 'epoch' time 
    **/
    private long _parseDate_YYMMDDhhmmss(long YYMMDDhhmmss)
    {
        if (YYMMDDhhmmss <= 0L) {
            return 0L;
        } else {
            //                            YYMMDDhhmmss
            int YY = (int)((YYMMDDhhmmss / 10000000000L) % 100L); // 14 year
            int MM = (int)((YYMMDDhhmmss /   100000000L) % 100L); // 04 month
            int DD = (int)((YYMMDDhhmmss /     1000000L) % 100L); // 11 day
            int hh = (int)((YYMMDDhhmmss /       10000L) % 100L); // 01 hour
            int mm = (int)((YYMMDDhhmmss /         100L) % 100L); // 48 minute
            int ss = (int)((YYMMDDhhmmss /           1L) % 100L); // 04 second
            DateTime dt = new DateTime(gmtTimezone,YY+2000,MM,DD,hh,mm,ss);
            return dt.getTimeSec();
        }
    }

    // ------------------------------------------------------------------------
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
    *** Common event data handler for all TKDeviceType's
    **/
    private boolean handleCommon(String modemID, // this.tkModemID
        long      fixtime, int statusCode, HashSet<Integer> statCodeSet,
        GeoPoint  geoPoint, long gpsAge, double HDOP, int numSats,
        double    speedKPH, double headingDeg, double altitudeM, double odomKM,
        long      gpioInput, double batteryV, double battLvl, 
        double    engTempC, String engDTC[],
        CellTower servingCell)
    {

        /* parsed data */
        Print.logInfo("IMEI     : " + this.tkModemID);
        Print.logInfo("Timestamp: " + fixtime + " [" + new DateTime(fixtime) + "]");

        /* find Device */
        //Device device = DCServerFactory.loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, this.tkModemID);
        Device device = DCServerConfig.loadDeviceUniqueID(Main.getServerConfig(), this.tkModemID);
        if (device == null) {
            return false; // errors already displayed
        }
        String accountID = device.getAccountID();
        String deviceID  = device.getDeviceID();
        String uniqueID  = device.getUniqueID();
        Print.logInfo("UniqueID : " + uniqueID);
        Print.logInfo("DeviceID : " + accountID + "/" + deviceID);

        /* check IP address */
        DataTransport dataXPort = device.getDataTransport();
        if (this.hasIPAddress() && !dataXPort.isValidIPAddress(this.getIPAddress())) {
            DTIPAddrList validIPAddr = dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.getIPAddress() + 
                " [expecting " + validIPAddr + "]");
            return false;
        }
        dataXPort.setIpAddressCurrent(this.getIPAddress());    // FLD_ipAddressCurrent
        dataXPort.setRemotePortCurrent(this.getRemotePort());  // FLD_remotePortCurrent
        dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!dataXPort.getDeviceCode().equalsIgnoreCase(Main.getServerName())) {
            dataXPort.setDeviceCode(Main.getServerName()); // FLD_deviceCode
        }

        /* valid GeoPoint? */
        boolean validGPS;
        if (geoPoint == null) {
            geoPoint = GeoPoint.INVALID_GEOPOINT;
            validGPS = false;
        } else {
            validGPS = geoPoint.isValid();
        }

        /* use last GPS location? */
        if (!validGPS) {
            if (USE_LAST_VALID_GPS) {
                // -- use last valid GPS location
                GeoPoint lastGPS = device.getLastValidLocation();
                if (GeoPoint.isValid(lastGPS)) {
                    // -- we have a last valid GPS location
                    geoPoint   = lastGPS;
                    speedKPH   = device.getLastValidSpeedKPH();
                    headingDeg = device.getLastValidHeading();
                    validGPS   = true;
                    gpsAge     = DateTime.getCurrentTimeSec() - device.getLastGPSTimestamp();
                    if (gpsAge < 0L) { gpsAge = 0L; }
                    if (statusCode == StatusCodes.STATUS_LOCATION) {
                        statusCode = StatusCodes.STATUS_LAST_LOCATION;
                    }
                } else {
                    //Print.logInfo("No last valid GPS location");
                }
            } else
            if (servingCell != null) {
                // -- we have a possible cell-tower location
                statusCode = StatusCodes.STATUS_CELL_LOCATION;
                // -- "validGPS" left as "false"
            }
        }

        /* calculate heading from last location */
        if (headingDeg < 0.0) {
            // -- try to calculate heading based on last valid GPS location
            headingDeg = 0.0;
            if (validGPS && (speedKPH > 0.0)) {
                GeoPoint lastGP = device.getLastValidLocation();
                if (GeoPoint.isValid(lastGP)) {
                    // -- calculate heading from last point to this point
                    headingDeg = lastGP.headingToPoint(geoPoint);
                }
            }
        }

        /* estimate GPS-based odometer */
        if (odomKM <= 0.0) {
            // -- calculate odometer
            odomKM = (ESTIMATE_ODOMETER && validGPS)? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            // -- bounds-check odometer
            odomKM = device.adjustOdometerKM(odomKM);
        }

        /* log parsed data */
        Print.logInfo("GPS      : " + geoPoint);
        if (altitudeM != 0.0) {
        Print.logInfo("Altitude : " + StringTools.format(altitudeM,"#0.0") + " meters");
        }
        Print.logInfo("Speed    : " + StringTools.format(speedKPH ,"#0.0") + " kph " + headingDeg);
        if (batteryV > 0.0) {
        Print.logInfo("Battery V: " + StringTools.format(batteryV ,"#0.0") + " Volts");
        }
        if (battLvl > 0.0) {
        Print.logInfo("Battery %: " + StringTools.format(battLvl*100.0,"#0.0") + " %");
        }
        if (odomKM > 0.0) {
        Print.logInfo("Odometer : " + odomKM + " km");
        }
        if (engTempC > 0.0) {
        Print.logInfo("EngTemp C: " + StringTools.format(engTempC ,"#0.0") + " C");
        }
        if ((engDTC != null) && (engDTC.length > 0)) {
        Print.logInfo("OBD DTC  : " + StringTools.join(engDTC,","));
        }
        if (servingCell != null) {
        Print.logInfo("CellTower: " + servingCell);
        }

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && validGPS) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    int zsc = z.getStatusCode(); // STATUS_GEOFENCE_ARRIVE / STATUS_GEOFENCE_DEPART
                    this.insertEventRecord(device, 
                        z.getTimestamp(), zsc, z.getGeozone(),
                        geoPoint, gpsAge, HDOP, numSats,
                        speedKPH, headingDeg, altitudeM, odomKM,
                        gpioInput, batteryV, battLvl, 
                        engTempC, engDTC,
                        servingCell);
                    Print.logInfo("Geozone    : " + z);
                }
            }
        }

        /* digital input change events */
        if (gpioInput >= 0L) {
            if (SIMEVENT_DIGITAL_INPUTS > 0L) {
                // The current input state is compared to the last value stored in the Device record.
                // Changes in the input state will generate a synthesized event.
                long chgMask = (device.getLastInputState() ^ gpioInput) & SIMEVENT_DIGITAL_INPUTS;
                if (chgMask != 0L) {
                    // an input state has changed
                    for (int b = 0; b <= 7; b++) {
                        long m = 1L << b;
                        if ((chgMask & m) != 0L) {
                            // this bit changed
                            long inpTime = fixtime;
                            int  inpCode = ((gpioInput & m) != 0L)? InputStatusCodes_ON[b] : InputStatusCodes_OFF[b];
                            Print.logInfo("GPIO input : " + StatusCodes.GetDescription(inpCode,null));
                            this.insertEventRecord(device, 
                                inpTime, inpCode, null/*geozone*/,
                                geoPoint, gpsAge, HDOP, numSats,
                                speedKPH, headingDeg, altitudeM, odomKM,
                                gpioInput, batteryV, battLvl, 
                                engTempC, engDTC,
                                servingCell);
                        }
                    }
                }
            }
            device.setLastInputState(gpioInput & 0xFFFFL); // FLD_lastInputState
        }

        /* insert all status code in 'statCodeSet' */
        if (!ListTools.isEmpty(statCodeSet)) {
            // "statCodeSet" should not contain a STATUS_LOCATION code
            for (Integer sci : statCodeSet) {
                int sc = sci.intValue();
                this.insertEventRecord(device, 
                    fixtime, sc, null/*geozone*/,
                    geoPoint, gpsAge, HDOP, numSats,
                    speedKPH, headingDeg, altitudeM, odomKM,
                    gpioInput, batteryV, battLvl, 
                    engTempC, engDTC,
                    servingCell);
                if (statusCode == sc) {
                    // unlikely, but check anyway
                    statusCode = StatusCodes.STATUS_IGNORE;
                }
            }
            if (statusCode == StatusCodes.STATUS_LOCATION) {
                // we already have other events, skip default STATUS_LOCATION
                statusCode = StatusCodes.STATUS_IGNORE;
            }
        }

        /* Event insertion: status code checks */
        if (statusCode < 0) { // StatusCodes.STATUS_IGNORE
            // -- skip (event ignored)
            Print.logDebug("Ignoring Event (per EventCodeMap)");
        } else
        if (statusCode == StatusCodes.STATUS_IGNORE) {
            // -- skip (event ignored)
            Print.logDebug("Ignoring Event (per EventCodeMap)");
        } else
        if (this.hasSavedEvents()                           &&
            ((statusCode == StatusCodes.STATUS_LOCATION) ||
             (statusCode == StatusCodes.STATUS_NONE)       )  ) {
            // -- skip (already inserted an event above)
        } else
        if (statusCode == StatusCodes.STATUS_NONE) {
            // -- STATUS_NONE and no inserted events ==> convert to "InMotion" or "Location"
            int sc = (speedKPH > 0.0)? StatusCodes.STATUS_MOTION_IN_MOTION : StatusCodes.STATUS_LOCATION;
            this.insertEventRecord(device, 
                fixtime, sc, null/*geozone*/,
                geoPoint, gpsAge, HDOP, numSats,
                speedKPH, headingDeg, altitudeM, odomKM,
                gpioInput, batteryV, battLvl, 
                engTempC, engDTC,
                servingCell);
        } else
        if (statusCode != StatusCodes.STATUS_LOCATION) {
            // -- Not a "Location" event
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*geozone*/,
                geoPoint, gpsAge, HDOP, numSats,
                speedKPH, headingDeg, altitudeM, odomKM,
                gpioInput, batteryV, battLvl, 
                engTempC, engDTC,
                servingCell);
        } else
        if (XLATE_LOCATON_INMOTION && (speedKPH > 0.0)) {
            // -- Traslate "Location" to "InMotion"
            int sc = StatusCodes.STATUS_MOTION_IN_MOTION;
            this.insertEventRecord(device, 
                fixtime, sc, null/*geozone*/,
                geoPoint, gpsAge, HDOP, numSats,
                speedKPH, headingDeg, altitudeM, odomKM,
                gpioInput, batteryV, battLvl, 
                engTempC, engDTC,
                servingCell);
        } else // <-- fixed v2.5.7-B10
        if (validGPS && !device.isNearLastValidLocation(geoPoint,MINIMUM_MOVED_METERS)) {
            // Only include "Location" if not nearby previous event
            this.insertEventRecord(device, 
                fixtime, statusCode, null/*geozone*/,
                geoPoint, gpsAge, HDOP, numSats,
                speedKPH, headingDeg, altitudeM, odomKM,
                gpioInput, batteryV, battLvl, 
                engTempC, engDTC,
                servingCell);
        }

        /* save device changes */
        if (!DEBUG_MODE) {
            try {
                //DBConnection.pushShowExecutedSQL();
                device.updateChangedEventFields();
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
            } finally {
                //DBConnection.popShowExecutedSQL();
            }
        }

        /* return ok */
        return true;

    }

    /**
    *** Create EventData record
    **/
    private EventData createEventRecord(Device device, 
        long      gpsTime, int statusCode, Geozone geozone,
        GeoPoint  geoPoint, long gpsAge, double HDOP, int numSats,
        double    speedKPH, double heading, double altitudeM, double odomKM,
        long      gpioInput, double batteryV, double battLvl, 
        double    engTempC, String engDTC[],
        CellTower servingCell)
    {
        String accountID    = device.getAccountID();
        String deviceID     = device.getDeviceID();
        EventData.Key evKey = new EventData.Key(accountID, deviceID, gpsTime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeozone(geozone);
        evdb.setGeoPoint(geoPoint);
        evdb.setGpsAge(gpsAge);
        evdb.setHDOP(HDOP);                     // <-- requires "GPSFieldInfo" optional fields
        evdb.setSatelliteCount(numSats);        // <-- requires "GPSFieldInfo" optional fields
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(heading);
        evdb.setAltitude(altitudeM);
        evdb.setOdometerKM(odomKM);
        evdb.setInputMask(gpioInput);
        evdb.setBatteryVolts(batteryV);         // <-- requires "GPSFieldInfo" optional fields
        evdb.setBatteryLevel(battLvl);          // <-- requires "GPSFieldInfo" optional fields
        evdb.setServingCellTower(servingCell);  // <-- requires "ServingCellTowerData" optional fields
        evdb.setCoolantTemp(engTempC);          // <-- requires "CANBUSFieldInfo" optional fields
        evdb.setFaultCode_OBDII(engDTC);        // <-- requires "CANBUSFieldInfo" optional fields
        return evdb;
    }

    /**
    *** Create/Insert a EventData record 
    **/
    private void insertEventRecord(Device device, 
        long      gpsTime, int statusCode, Geozone geozone,
        GeoPoint  geoPoint, long gpsAge, double HDOP, int numSats,
        double    speedKPH, double heading, double altitudeM, double odomKM,
        long      gpioInput, double batteryV, double battLvl, 
        double    engTempC, String engDTC[],
        CellTower servingCell)
    {

        /* create event */
        EventData evdb = createEventRecord(device, 
            gpsTime, statusCode, geozone,
            geoPoint, gpsAge, HDOP, numSats,
            speedKPH, heading, altitudeM, odomKM,
            gpioInput, batteryV, battLvl, 
            engTempC, engDTC,
            servingCell);

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event: [0x" + StringTools.toHexString(statusCode,16) + "] " + 
            StatusCodes.GetDescription(statusCode,null));
        if (!DEBUG_MODE) {
            device.insertEventData(evdb);
            this.incrementSavedEventCount();
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Startup configuration initialization
    **/
    public static void configInit() 
    {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc != null) {

            /* common */
            UNIQUEID_PREFIX          = dcsc.getUniquePrefix();
            MINIMUM_SPEED_KPH        = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
            ESTIMATE_ODOMETER        = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
            SIMEVENT_GEOZONES        = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
            SIMEVENT_DIGITAL_INPUTS  = dcsc.getSimulateDigitalInputs(SIMEVENT_DIGITAL_INPUTS) & 0xFFL;
            XLATE_LOCATON_INMOTION   = dcsc.getStatusLocationInMotion(XLATE_LOCATON_INMOTION);
            USE_LAST_VALID_GPS       = dcsc.getUseLastValidGPSLocation(USE_LAST_VALID_GPS);
            MINIMUM_MOVED_METERS     = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);

            /* custom */
            PACKET_LEN_END_OF_STREAM = dcsc.getBooleanProperty(Constants.CFG_packetLenEndOfStream, PACKET_LEN_END_OF_STREAM);

        }
        
    }

}
