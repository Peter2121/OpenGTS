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
// Astra Telematics device communication server with support for:
//   Protocol C
//   Protocol K
//   Protocol M
// ----------------------------------------------------------------------------
// Change History:
//  2012/11/27  Richard R. Patel
//     -Initial release
//  2014/02/04  Richard R. Patel
//     -Added support for Protocol C
//     -Added check for more packet data whilst parsing reports
// ----------------------------------------------------------------------------
package org.opengts.servers.astra;

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

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = new String[] { "astra_", "imei_" };;
    public static       double  MINIMUM_SPEED_KPH           = 5.0;
    public static       boolean ESTIMATE_ODOMETER           = false;
    public static       boolean SIMEVENT_GEOZONES           = false;
    public static       boolean XLATE_LOCATON_INMOTION      = false;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;

    public static       boolean DEBUG_MODE                  = false;

    // ------------------------------------------------------------------------

    /* Lookup table required for checksum generation */
    private static final int[] laCrc16LookUpTable = 
    {
        0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241, 0xC601, 0x06C0, 0x0780, 0xC741, 0x0500,
        0xC5C1, 0xC481, 0x0440, 0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40, 0x0A00, 0xCAC1,
        0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841, 0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81,
        0x1A40, 0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41, 0x1400, 0xD4C1, 0xD581, 0x1540, 
        0xD701, 0x17C0, 0x1680, 0xD641, 0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040, 0xF001,
        0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240, 0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0,
        0x3480, 0xF441, 0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41, 0xFA01, 0x3AC0, 0x3B80, 
        0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840, 0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41, 
        0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40, 0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 
        0xE7C1, 0xE681, 0x2640, 0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041, 0xA001, 0x60C0, 
        0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240, 0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 
        0xA441, 0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41, 0xAA01, 0x6AC0, 0x6B80, 0xAB41, 
        0x6900, 0xA9C1, 0xA881, 0x6840, 0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41, 0xBE01, 
        0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40, 0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 
        0xB681, 0x7640, 0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041, 0x5000, 0x90C1, 0x9181, 
        0x5140, 0x9301, 0x53C0, 0x5280, 0x9241, 0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440, 
        0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40, 0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 
        0x59C0, 0x5880, 0x9841, 0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40, 0x4E00, 0x8EC1, 
        0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41, 0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 
        0x8641, 0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
    };

    /* Protocol identifiers */
    private static byte PROTOCOL_C                      = 0x43;
    private static byte PROTOCOL_K                      = 0x4B;
    private static byte PROTOCOL_M                      = 0x4D;

    /* Report lengths */
    private static int PROTOCOL_C_BASIC_LEN             = 33;
    private static int PROTOCOL_K_BASIC_LEN             = 38;
    private static int PROTOCOL_K_START_STOP_LEN        = 50;
    private static int PROTOCOL_M_BASIC_LEN             = 41;
    private static int PROTOCOL_M_START_STOP_LEN        = 53;

    /* Reason code bits */
    private static int REASON_TIME_ELAPSED              = 0x01;
    private static int REASON_DIST_TRAVELLED            = 0x02;
    private static int REASON_POS_ON_DEMAND             = 0x04;
    private static int REASON_GEO_FENCE                 = 0x08;
    private static int REASON_PANIC_SWITCH              = 0x10;
    private static int REASON_EXT_INPUT                 = 0x20;
    private static int REASON_JOURNEY_START             = 0x40;
    private static int REASON_JOURNEY_STOP              = 0x80;
    private static int REASON_HEADING_CHANGE            = 0x100;
    private static int REASON_LOW_BATTERY               = 0x200;
    private static int REASON_EXT_POWER_EVENT           = 0x400;
    private static int REASON_IDLING_START              = 0x800;
    private static int REASON_IDLING_END                = 0x1000;
    private static int REASON_IDLING_ONGOING            = 0x2000;
    private static int REASON_POWER_ON                  = 0x4000;
    private static int REASON_SPEED_OVER_THRESHOLD      = 0x8000;
    private static int REASON_TOWING_ALARM              = 0x10000;
    private static int REASON_UNAUTHORISED_DRIVER       = 0x20000;
    private static int REASON_COLLISION                 = 0x40000;
    private static int REASON_ACCEL_MAX                 = 0x80000;
    private static int REASON_CORNERING_MAX             = 0X100000;
    private static int REASON_DECEL_MAX                 = 0x200000;
    private static int REASON_GPS_REACQUIRED            = 0x400000;
    private static int REASON_CANBUS_EVENT              = 0x800000;

    /* Status code bits */
    private static int STATUS_IGNITION_ON               = 0x01;
    private static int STATUS_REPORTS_TO_FOLLOW         = 0x10;
    private static int STATUS_EXTRA_DATA                = 0x100;

    /* Geo fence event type indicator */
    private static int GEOFENCE_ENTERED                 = 0x80;

    // ------------------------------------------------------------------------

    /* session IP address */
    private String          ipAddress                   = null;
    private int             clientPort                  = 0;

    /* current device */
    private Device          device                      = null;

    /* Protocol identifier */
    private byte            protocol                    = 0;

    /* Extra report data added to rawData field if true */
    private boolean         addRawData                  = true;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

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

        /* init */
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;
        this.clientPort       = this.getSessionInfo().getRemotePort();
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
        int packetLength = 0;

        /* Check the protocol identifier */
        if ((packet[0] == PROTOCOL_C) || (packet[0] == PROTOCOL_K) || (packet[0] == PROTOCOL_M))
        {
            protocol = packet[0];

            /* Determine the full packet length */
            Payload p = new Payload(packet, 1, 2);
            packetLength = p.readUInt(2, 0, true);

            /* (debug message) log protocol for received packet */
            if (DEBUG_MODE)
            {
                Print.logInfo("Protocol: " + (char)protocol);
            }
        }
        
        /* (debug message) log received packet length */
        if (DEBUG_MODE)
        {
            Print.logInfo("Packet length: " + packetLength);
        }

        /* Let 'getHandlePacket' method process the complete packet */
        return packetLength;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if ((pktBytes != null) && (pktBytes.length > 0))
        {
            byte rtn[] = null;

            /* (debug message) display received data packet */
            if (DEBUG_MODE)
            {
                Print.logInfo("Recv[HEX]: " + StringTools.toHexString(pktBytes));
            }

            /* Parse packet contents and insert data into database */
            if (protocol == PROTOCOL_C)
            {
                if (parseInsertRecord_C(pktBytes))
                {
                    /* If packet parsed successfully return acknowledgment back to client device */
                    rtn = new byte[1];
                    rtn[0] = 0x06; // ACK
                }
            }
            else if (protocol == PROTOCOL_K)
            {
                if (parseInsertRecord_K(pktBytes))
                {
                    /* If packet parsed successfully return acknowledgment back to client device */
                    rtn = new byte[1];
                    rtn[0] = 0x06; // ACK
                }
            }
            else if (protocol == PROTOCOL_M)
            {
                if (parseInsertRecord_M(pktBytes))
                {
                    /* If packet parsed successfully return acknowledgment back to client device */
                    rtn = new byte[1];
                    rtn[0] = 0x06; // ACK
                }
            }

            return rtn;
        }
        else
        {
            /* no packet received */
            Print.logInfo("Empty packet received ...");
            return null; // no return packets are expected
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean parseInsertRecord_C(byte pktBytes[])
    {
        int i;
        int index;
        int checksum;
        int packetChecksum;
        int sequenceNumber;
        int intLatitude;
        int intLongitude;
        int intSpeed;
        int intHeading;
        int intAltitude;
        int intJourneyDist;
        int journeyIdleTime;
        int repReason;
        int repStatus;
        int statusCode;
        int reportLen;
        int digitals;
        int digitalChanges;
        int intAdc1;
        int intAdc2;
        int battLevel;
        int intExtPwr;
        int maxSpeed;
        int[] accelData = new int[2];
        int geoFence;
        long fixtime;
        double latitude;
        double longitude;
        double speed;
        double heading;
        double altitude;
        double journeyDist;
        double adc1;
        double adc2;
        double extPwr;
        GeoPoint geoPoint;
        boolean reportsToFollow = false;
        String rawData = "";

        /* Generate 2 byte packet checksum for all bytes except the last 2 */
        Payload p = new Payload(pktBytes, (pktBytes.length - 2), 2);
        packetChecksum = p.readUInt(2, 0);
        checksum = generateCheckSum (pktBytes, pktBytes.length);

        if (checksum != packetChecksum)
        {
            Print.logInfo("ERROR: Calculated checksum does not match packet checksum");
            return false;
        }

        /* Parse the header to extract the IMEI */
        index = 3;
        p = new Payload(pktBytes, index, 7);
        long tac = p.readULong(4, 0L, true);
        int msn = p.readUInt(3, 0, true);
        String imei = String.valueOf(tac) + String.valueOf(msn);
        Print.logInfo("IMEI: " + imei);

        /* Set the index to the start of the report data */
        index += 7;

        /* Find the device in the database */
        this.device = DCServerFactory._loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, imei);
        //this.device = DCServerConfig.loadDeviceUniqueID(Main.getServerConfig(), imei);
        if (this.device == null)
        {
            /* Error already logged */
            return false;
        }

        /* Parse each report in the packet */
        do
        {
            reportLen = PROTOCOL_C_BASIC_LEN;

            p = new Payload(pktBytes, index, PROTOCOL_K_BASIC_LEN);
            sequenceNumber = p.readUInt(1, 0);
            intLatitude = p.readInt(4, 0);
            intLongitude = p.readInt(4, 0);
            latitude = (double)intLatitude / 1000000f;
            longitude = (double)intLongitude / 1000000f;
            geoPoint = new GeoPoint(latitude, longitude);

            // Read Julian time - time and date in GPS seconds and convert to 
            // seconds from Unix epoch by adding the difference between
            // 00:00:00 6 January 1980 and 00:00:00 1 January 1970
            // The stored time will be GMT
            fixtime = p.readULong(4, 0L, true);
            fixtime += 315964800L;

            /* Convert speed stored as km/h divided by 2 to km/h */
            intSpeed = p.readUInt(1, 0);
            speed = (double)intSpeed * 2f;

            /* Convert heading stored as degress divided by 2 to degrees */
            intHeading = p.readUInt(1, 0);
            heading = (double)intHeading * 2f;

            /* Altitude in metres divided by 20 - conver to meters */
            intAltitude = p.readUInt(1, 0);
            altitude = (double)intAltitude * 20f;

            /* Read the reason code and status code */
            repReason = p.readUInt(2, 0);
            repStatus = p.readUInt(1, 0);

            /* Geofence: Bit 7=1 for entry, Bit 7=0 for exit. Bits 6-0=geofence index */
            geoFence = p.readUInt(1, 0);
            
            /* Digitial input/output states */
            digitals = p.readUInt(1, 0);

            /* Digitial input/output state changes */
            digitalChanges = p.readUInt(1, 0);

            /* ADC2 0-2V - convert to voltage with resolution of 0.0078125V */
            intAdc2 = p.readUInt(1, 0);
            adc2 = (double)intAdc2 / 128f;
            adc2 = adc2 / 1000f;

            /* ADC1 0-2V - convert to voltage with resolution of 0.0078125V */
            intAdc1 = p.readUInt(1, 0);
            adc1 = (double)intAdc1 / 128f;
            adc1 = adc1 / 1000f;

            /* Battery level as a percentage */
            battLevel = p.readUInt(1, 0);

            /* External input voltage to 0.2V resolution - convert to actual input voltage */
            intExtPwr = p.readUInt(1, 0);
            extPwr = (double)intExtPwr / 5f;

            // Convert max speed stored as km/h divided by 2 to km/h
            // During a journey this is max speed since last report
            // At the end of a journey it is the max speed during the entire journey
            maxSpeed = p.readUInt(1, 0);
            maxSpeed = maxSpeed * 2;

            /* Read the min and max accelerometer data for X, Y and Z axes */
            for (i = 0; i < 2; i++)
            {
                accelData[i] = p.readUInt(1, 0);
            }

            /* Journey distance to 0.1km resolution - convert to actual distance */
            intJourneyDist = p.readUInt(2, 0);
            journeyDist = (double)intJourneyDist / 10f;

            /* Time in seconds that the vehicle is stationary with the ignition on */
            journeyIdleTime = p.readUInt(2, 0);

            /* (debug message) log report data */
            if (DEBUG_MODE)
            {
                Print.logInfo("Seq Num: " + sequenceNumber);
                Print.logInfo("GPS: " + geoPoint);
                Print.logInfo("Time: " + fixtime);
                Print.logInfo("Speed: " + speed);
                Print.logInfo("Heading: " + heading);
                Print.logInfo("Journey Dist: " + journeyDist);
                Print.logInfo("Altitude: " + altitude);
            }

            /* One status code per report so determine which one is most relevant */
            statusCode = determineGTSStatusCode(repReason, repStatus, extPwr, geoFence);

            /* Extra report data to be added to rawData field ? */
            if (addRawData)
            {
                rawData = "R=" + StringTools.toHexString(repReason, 16) +
                          ";S=" + StringTools.toHexString(repStatus, 8) +
                          ";P=" + extPwr + "V" +
                          ";B=" + battLevel + "%" +
                          ";D=" + StringTools.toHexString(digitals, 8) + "," + StringTools.toHexString(digitalChanges, 8) +
                          ";A1=" + adc1 + "V" +
                          ";A2=" + adc2 + "V" +
                          ";M=" + maxSpeed + "km/h" +
                          ";X=" + accelData[0] + "," + accelData[1] +
                          ";I=" + journeyIdleTime + "s" +
                          ";G=" + StringTools.toHexString(geoFence, 8);
            }

            /* Insert record into EventData table (odometer=0) */
            insertEventRecord(device, 
                    fixtime, statusCode, geoPoint,
                    speed, heading, altitude,
                    journeyDist, 0.0, rawData);

            /* Check for more reports in the packet */
            if ((repStatus & STATUS_REPORTS_TO_FOLLOW) > 0)
            {
                reportsToFollow = true;

                /* Advance the index accordingly */
                index += reportLen;
            }
            else
            {
                reportsToFollow = false;
            }
        }
        while (reportsToFollow && ((pktBytes.length - index) > 2));

        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean parseInsertRecord_K(byte pktBytes[])
    {
        int i;
        int index;
        int checksum;
        int packetChecksum;
        int sequenceNumber;
        int intLatitude;
        int intLongitude;
        int intSpeed;
        int intHeading;
        int intAltitude;
        int intJourneyDist;
        int journeyIdleTime;
        int intOdometer;
        int repReason;
        int repStatus;
        int statusCode;
        int reportLen;
        int digitals;
        int intAdc1;
        int battLevel;
        int intExtPwr;
        int maxSpeed;
        int[] accelData = new int[6];
        int signalQuality;
        int geoFence;
        long fixtime;
        double latitude;
        double longitude;
        double speed;
        double heading;
        double altitude;
        double journeyDist;
        double odometer;
        double adc1;
        double extPwr;
        GeoPoint geoPoint;
        boolean reportsToFollow = false;
        String rawData = "";

        /* Generate 2 byte packet checksum for all bytes except the last 2 */
        Payload p = new Payload(pktBytes, (pktBytes.length - 2), 2);
        packetChecksum = p.readUInt(2, 0);
        checksum = generateCheckSum (pktBytes, pktBytes.length);

        if (checksum != packetChecksum)
        {
            Print.logInfo("ERROR: Calculated checksum does not match packet checksum");
            return false;
        }

        /* Parse the header to extract the IMEI */
        index = 3;
        p = new Payload(pktBytes, index, 7);
        long tac = p.readULong(4, 0L, true);
        int msn = p.readUInt(3, 0, true);
        String imei = String.valueOf(tac) + String.valueOf(msn);
        Print.logInfo("IMEI: " + imei);

        /* Set the index to the start of the report data */
        index += 7;

        /* Find the device in the database */
        this.device = DCServerFactory._loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, imei);
        //this.device = DCServerConfig.loadDeviceUniqueID(Main.getServerConfig(), imei);
        if (this.device == null)
        {
            /* Error already logged */
            return false;
        }

        /* Parse each report in the packet */
        do
        {
            p = new Payload(pktBytes, index, PROTOCOL_K_BASIC_LEN);
            sequenceNumber = p.readUInt(1, 0);
            intLatitude = p.readInt(4, 0);
            intLongitude = p.readInt(4, 0);
            latitude = (double)intLatitude / 1000000f;
            longitude = (double)intLongitude / 1000000f;
            geoPoint = new GeoPoint(latitude, longitude);

            // Read Julian time - time and date in GPS seconds and convert to 
            // seconds from Unix epoch by adding the difference between
            // 00:00:00 6 January 1980 and 00:00:00 1 January 1970
            // The stored time will be GMT
            fixtime = p.readULong(4, 0L, true);
            fixtime += 315964800L;

            /* Convert speed stored as km/h divided by 2 to km/h */
            intSpeed = p.readUInt(1, 0);
            speed = (double)intSpeed * 2f;

            /* Convert heading stored as degress divided by 2 to degrees */
            intHeading = p.readUInt(1, 0);
            heading = (double)intHeading * 2f;

            /* Read the reason code and status code */
            repReason = p.readUInt(3, 0);
            repStatus = p.readUInt(2, 0);

            if ((repStatus & STATUS_EXTRA_DATA) > 0)
            {
                reportLen = PROTOCOL_K_START_STOP_LEN;
            }
            else
            {
                reportLen = PROTOCOL_K_BASIC_LEN;
            }
            
            /* Digitial input/output status and state changes */
            digitals = p.readUInt(1, 0);

            /* ADC1 0-5V - convert to voltage with resolution of 0.02V */
            intAdc1 = p.readUInt(1, 0);
            adc1 = (double)intAdc1 * 20;
            adc1 = adc1 / 1000f;

            /* Battery level as a percentage */
            battLevel = p.readUInt(1, 0);

            /* External input voltage to 0.2V resolution - convert to actual input voltage */
            intExtPwr = p.readUInt(1, 0);
            extPwr = (double)intExtPwr / 5f;

            // Convert max speed stored as km/h divided by 2 to km/h
            // During a journey this is max speed since last report
            // At the end of a journey it is the max speed during the entire journey
            maxSpeed = p.readUInt(1, 0);
            maxSpeed = maxSpeed * 2;

            /* Read the min and max accelerometer data for X, Y and Z axes */
            for (i = 0; i < 6; i++)
            {
                accelData[i] = p.readUInt(1, 0);
            }

            /* Journey distance to 0.1km resolution - convert to actual distance */
            intJourneyDist = p.readUInt(2, 0);
            journeyDist = (double)intJourneyDist / 10f;

            /* Time in seconds that the vehicle is stationary with the ignition on */
            journeyIdleTime = p.readUInt(2, 0);

            /* Altitude in metres divided by 20 - conver to meters */
            intAltitude = p.readUInt(1, 0);
            altitude = (double)intAltitude * 20f;

            // Most significant nibble is GSM signal strength: scale 0-15
            // Least significant nibble is the number of GPS satellites in use
            signalQuality = p.readUInt(1, 0);

            /* Geofence: Bit 7=1 for entry, Bit 7=0 for exit. Bits 6-0=geofence index */
            geoFence = p.readUInt(1, 0);

            if ((repStatus & STATUS_EXTRA_DATA) > 0)
            {
                /* For start/stop reports read the lifetime odometer in km */
                p = new Payload(pktBytes, (index + 45), 3);
                intOdometer = p.readUInt(3, 0);
                odometer = (double)intOdometer;
            }
            else
            {
                /* For basic reports the odometer reading is not present - set it to 0 */
                odometer = 0f;
            }

            /* (debug message) log report data */
            if (DEBUG_MODE)
            {
                Print.logInfo("Seq Num: " + sequenceNumber);
                Print.logInfo("GPS: " + geoPoint);
                Print.logInfo("Time: " + fixtime);
                Print.logInfo("Speed: " + speed);
                Print.logInfo("Heading: " + heading);
                Print.logInfo("Journey Dist: " + journeyDist);
                Print.logInfo("Altitude: " + altitude);
                Print.logInfo("Odometer: " + odometer);
            }

            /* One status code per report so determine which one is most relevant */
            statusCode = determineGTSStatusCode(repReason, repStatus, extPwr, geoFence);

            /* Extra report data to be added to rawData field ? */
            if (addRawData)
            {
                rawData = "R=" + StringTools.toHexString(repReason, 24) +
                          ";S=" + StringTools.toHexString(repStatus, 16) +
                          ";P=" + extPwr + "V" +
                          ";B=" + battLevel + "%" +
                          ";D=" + StringTools.toHexString(digitals, 8) +
                          ";A1=" + adc1 + "V" +
                          ";M=" + maxSpeed + "km/h" +
                          ";X=" + accelData[0] + "," + accelData[1] +
                          ";Y=" + accelData[2] + "," + accelData[3] +
                          ";Z=" + accelData[4] + "," + accelData[5] +
                          ";I=" + journeyIdleTime + "s" +
                          ";Q=" + StringTools.toHexString(signalQuality, 8) +
                          ";G=" + StringTools.toHexString(geoFence, 8);
            }

            /* Insert record into EventData table */
            insertEventRecord(device, 
                    fixtime, statusCode, geoPoint,
                    speed, heading, altitude,
                    journeyDist, odometer, rawData);

            /* Check for more reports in the packet */
            if ((repStatus & STATUS_REPORTS_TO_FOLLOW) > 0)
            {
                reportsToFollow = true;

                /* Advance the index accordingly */
                index += reportLen;
            }
            else
            {
                reportsToFollow = false;
            }
        }
        while (reportsToFollow && ((pktBytes.length - index) > 2));

        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean parseInsertRecord_M(byte pktBytes[])
    {
        int i;
        int index;
        int checksum;
        int packetChecksum;
        int sequenceNumber;
        int intLatitude;
        int intLongitude;
        int intSpeed;
        int intHeading;
        int intAltitude;
        int intJourneyDist;
        int journeyIdleTime;
        int intOdometer;
        int repReason;
        int repStatus;
        int statusCode;
        int reportLen;
        int digitals;
        int intAdc1;
        int intAdc2;
        int battLevel;
        int intExtPwr;
        int maxSpeed;
        int[] accelData = new int[6];
        int signalQuality;
        int geoFence;
        long fixtime;
        double latitude;
        double longitude;
        double speed;
        double heading;
        double altitude;
        double journeyDist;
        double odometer;
        double adc1;
        double adc2;
        double extPwr;
        GeoPoint geoPoint;
        boolean reportsToFollow = false;
        String rawData = "";

        /* Generate 2 byte packet checksum for all bytes except the last 2 */
        Payload p = new Payload(pktBytes, (pktBytes.length - 2), 2);
        packetChecksum = p.readUInt(2, 0);
        checksum = generateCheckSum (pktBytes, pktBytes.length);

        if (checksum != packetChecksum)
        {
            Print.logInfo("ERROR: Calculated checksum does not match packet checksum");
            return false;
        }

        /* Parse the header to extract the IMEI */
        index = 3;
        p = new Payload(pktBytes, index, 7);
        long tac = p.readULong(4, 0L, true);
        int msn = p.readUInt(3, 0, true);
        String imei = String.valueOf(tac) + String.valueOf(msn);
        Print.logInfo("IMEI: " + imei);
        
        /* Set the index to the start of the report data */
        index += 7;

        /* Find the device in the database */
        this.device = DCServerFactory._loadDeviceByPrefixedModemID(UNIQUEID_PREFIX, imei);
        //this.device = DCServerConfig.loadDeviceUniqueID(Main.getServerConfig(), imei);
        if (this.device == null)
        {
            /* Error already logged */
            return false;
        }

        /* Parse each report in the packet */
        do
        {
            p = new Payload(pktBytes, index, PROTOCOL_M_BASIC_LEN);
            sequenceNumber = p.readUInt(1, 0);
            intLatitude = p.readInt(4, 0);
            intLongitude = p.readInt(4, 0);
            latitude = (double)intLatitude / 1000000f;
            longitude = (double)intLongitude / 1000000f;
            geoPoint = new GeoPoint(latitude, longitude);

            // Read Julian time - time and date in GPS seconds and convert to 
            // seconds from Unix epoch by adding the difference between
            // 00:00:00 6 January 1980 and 00:00:00 1 January 1970
            // The stored time will be GMT
            fixtime = p.readULong(4, 0L, true);
            fixtime += 315964800L;

            /* Convert speed stored as km/h divided by 2 to km/h */
            intSpeed = p.readUInt(1, 0);
            speed = (double)intSpeed * 2f;

            /* Convert heading stored as degress divided by 2 to degrees */
            intHeading = p.readUInt(1, 0);
            heading = (double)intHeading * 2f;

            /* Read the reason code and status code */
            repReason = p.readUInt(3, 0);
            repStatus = p.readUInt(2, 0);

            if ((repStatus & STATUS_EXTRA_DATA) > 0)
            {
                reportLen = PROTOCOL_M_START_STOP_LEN;
            }
            else
            {
                reportLen = PROTOCOL_M_BASIC_LEN;
            }
            
            // Digitial input/output status and state changes
            // Read as little-endian so that Digitals #1 is the least significant byte
            digitals = p.readUInt(3, 0, false);

            /* ADC1 0-5V - convert to voltage with resolution of 0.02V */
            intAdc1 = p.readUInt(1, 0);
            adc1 = (double)intAdc1 * 20;
            adc1 = adc1 / 1000f;

            /* ADC2 0-15V - convert to voltage with resolution of 0.059V */
            intAdc2 = p.readUInt(1, 0);
            adc2 = (double)intAdc2 * 59;
            adc2 = adc2 / 1000f;

            /* Battery level as a percentage */
            battLevel = p.readUInt(1, 0);

            /* External input voltage to 0.2V resolution - convert to actual input voltage */
            intExtPwr = p.readUInt(1, 0);
            extPwr = (double)intExtPwr / 5f;

            // Convert max speed stored as km/h divided by 2 to km/h
            // During a journey this is max speed since last report
            // At the end of a journey it is the max speed during the entire journey
            maxSpeed = p.readUInt(1, 0);
            maxSpeed = maxSpeed * 2;

            /* Read the min and max accelerometer data for X, Y and Z axes */
            for (i = 0; i < 6; i++)
            {
                accelData[i] = p.readUInt(1, 0);
            }

            /* Journey distance to 0.1km resolution - convert to actual distance */
            intJourneyDist = p.readUInt(2, 0);
            journeyDist = (double)intJourneyDist / 10f;

            /* Time in seconds that the vehicle is stationary with the ignition on */
            journeyIdleTime = p.readUInt(2, 0);

            /* Altitude in metres divided by 20 - conver to meters */
            intAltitude = p.readUInt(1, 0);
            altitude = (double)intAltitude * 20f;

            // Most significant nibble is GSM signal strength: scale 0-15
            // Least significant nibble is the number of GPS satellites in use
            signalQuality = p.readUInt(1, 0);

            /* Geofence: Bit 7=1 for entry, Bit 7=0 for exit. Bits 6-0=geofence index */
            geoFence = p.readUInt(1, 0);

            if ((repStatus & STATUS_EXTRA_DATA) > 0)
            {
                /* For start/stop reports read the lifetime odometer in km */
                p = new Payload(pktBytes, (index + 48), 3);
                intOdometer = p.readUInt(3, 0);
                odometer = (double)intOdometer;
            }
            else
            {
                /* For basic reports the odometer reading is not present - set it to 0 */
                odometer = 0f;
            }

            /* (debug message) log report data */
            if (DEBUG_MODE)
            {
                Print.logInfo("Seq Num: " + sequenceNumber);
                Print.logInfo("GPS: " + geoPoint);
                Print.logInfo("Time: " + fixtime);
                Print.logInfo("Speed: " + speed);
                Print.logInfo("Heading: " + heading);
                Print.logInfo("Journey Dist: " + journeyDist);
                Print.logInfo("Altitude: " + altitude);
                Print.logInfo("Odometer: " + odometer);
            }

            /* One status code per report so determine which one is most relevant */
            statusCode = determineGTSStatusCode(repReason, repStatus, extPwr, geoFence);

            /* Extra report data to be added to rawData field ? */
            if (addRawData)
            {
                rawData = "R=" + StringTools.toHexString(repReason, 24) +
                          ";S=" + StringTools.toHexString(repStatus, 16) +
                          ";P=" + extPwr + "V" +
                          ";B=" + battLevel + "%" +
                          ";D=" + StringTools.toHexString(digitals, 24) +
                          ";A1=" + adc1 + "V" +
                          ";A2=" + adc2 + "V" +
                          ";M=" + maxSpeed + "km/h" +
                          ";X=" + accelData[0] + "," + accelData[1] +
                          ";Y=" + accelData[2] + "," + accelData[3] +
                          ";Z=" + accelData[4] + "," + accelData[5] +
                          ";I=" + journeyIdleTime + "s" +
                          ";Q=" + StringTools.toHexString(signalQuality, 8) +
                          ";G=" + StringTools.toHexString(geoFence, 8);
            }

            /* Insert record into EventData table */
            insertEventRecord(device, 
                    fixtime, statusCode, geoPoint,
                    speed, heading, altitude,
                    journeyDist, odometer, rawData);

            /* Check for more reports in the packet */
            if ((repStatus & STATUS_REPORTS_TO_FOLLOW) > 0)
            {
                reportsToFollow = true;

                /* Advance the index accordingly */
                index += reportLen;
            }
            else
            {
                reportsToFollow = false;
            }
        }
        while (reportsToFollow && ((pktBytes.length - index) > 2));

        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int determineGTSStatusCode(int repReason, int repStatus, double extPwr, int geoFence)
    {
        int statusCode = 0;

        /* Select an OpenGTS status code based on the report reason flags and status code */
        if ((repReason & REASON_JOURNEY_START) > 0)
        {
            statusCode = StatusCodes.STATUS_IGNITION_ON;
        }
        else if ((repReason & REASON_JOURNEY_STOP) > 0)
        {
            statusCode = StatusCodes.STATUS_IGNITION_OFF;
        }
        else if ((repReason & REASON_TIME_ELAPSED) > 0)
        {
            if ((repStatus & STATUS_IGNITION_ON) > 0)
            {
                if ((repReason & REASON_IDLING_ONGOING) > 0)
                {
                    statusCode = StatusCodes.STATUS_MOTION_EXCESS_IDLE;
                }
                else
                {
                    statusCode = StatusCodes.STATUS_MOTION_IN_MOTION;
                }
            }
            else
            {
                statusCode = StatusCodes.STATUS_MOTION_DORMANT;
            }
        }
        else if ((repReason & REASON_IDLING_START) > 0)
        {
            statusCode = StatusCodes.STATUS_MOTION_IDLE;
        }
        else if ((repReason & REASON_IDLING_END) > 0)
        {
            statusCode = StatusCodes.STATUS_MOTION_START;
        }
        else if ((repReason & REASON_DIST_TRAVELLED) > 0)
        {
            statusCode = StatusCodes.STATUS_MOTION_MOVING;
        }
        else if ((repReason & REASON_HEADING_CHANGE) > 0)
        {
            statusCode = StatusCodes.STATUS_MOTION_HEADING;
        }
        else if ((repReason & REASON_SPEED_OVER_THRESHOLD) > 0)
        {
            statusCode = StatusCodes.STATUS_MOTION_EXCESS_SPEED;
        }
        else if ((repReason & REASON_PANIC_SWITCH) > 0)
        {
            statusCode = StatusCodes.STATUS_PANIC_ON;
        }
        else if ((repReason & REASON_EXT_POWER_EVENT) > 0)
        {
            if (extPwr > 7.0f)
            {
                statusCode = StatusCodes.STATUS_POWER_ON;
            }
            else
            {
                statusCode = StatusCodes.STATUS_POWER_OFF;
            }
        }
        else if ((repReason & REASON_EXT_INPUT) > 0)
        {
            statusCode = StatusCodes.STATUS_INPUT_STATE;
        }
        else if ((repReason & REASON_LOW_BATTERY) > 0)
        {
            statusCode = StatusCodes.STATUS_LOW_BATTERY;
        }
        else if ((repReason & REASON_ACCEL_MAX) > 0)
        {
            statusCode = StatusCodes.STATUS_EXCESS_ACCEL;
        }
        else if ((repReason & REASON_DECEL_MAX) > 0)
        {
            statusCode = StatusCodes.STATUS_EXCESS_BRAKING;
        }
        else if ((repReason & REASON_CORNERING_MAX) > 0)
        {
            statusCode = StatusCodes.STATUS_EXCESS_CORNERING;
        }
        else if ((repReason & REASON_COLLISION) > 0)
        {
            statusCode = StatusCodes.STATUS_IMPACT;
        }
        else if ((repReason & REASON_TOWING_ALARM) > 0)
        {
            statusCode = StatusCodes.STATUS_TOWING_START;
        }
        else if ((repReason & REASON_GEO_FENCE) > 0)
        {
            if ((geoFence & GEOFENCE_ENTERED ) > 0)
            {
                statusCode = StatusCodes.STATUS_GEOFENCE_ARRIVE;
            }
            else
            {
                statusCode = StatusCodes.STATUS_GEOFENCE_DEPART;
            }
        }
        else if ((repReason & REASON_POWER_ON) > 0)
        {
            statusCode = StatusCodes.STATUS_INITIALIZED;
        }
        else if ((repReason & REASON_GPS_REACQUIRED) > 0)
        {
            statusCode = StatusCodes.STATUS_HEARTBEAT;
        }
        else if ((repReason & REASON_POS_ON_DEMAND) > 0)
        {
            statusCode = StatusCodes.STATUS_QUERY;
        }
        else if ((repReason & REASON_UNAUTHORISED_DRIVER) > 0)
        {
            statusCode = StatusCodes.STATUS_BREACH_ON;
        }
        else
        {
            statusCode = StatusCodes.STATUS_NOTIFY;
        }

        return statusCode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int generateCheckSum (byte[] bytPacketDatam, int packetLen)
    {
        int llcrc = 0xFFFF;
        int llChar;
        int lnPos; 		  

        for (lnPos = 0; lnPos < (packetLen - 2); lnPos++)
        {
            llChar = (llcrc ^ bytPacketDatam[lnPos]) & 0x00ff;     
            llChar = laCrc16LookUpTable[llChar];
            llcrc = (llcrc >> 8) ^ llChar;
        }

        return (llcrc);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private EventData createEventRecord(Device device, 
        long     gpsTime, int statusCode, GeoPoint geoPoint,
        double speedKPH, double heading, double altitudeM,
        double distanceKM, double odomKM, String rawData)
    {
        String accountID    = device.getAccountID();
        String deviceID     = device.getDeviceID();
        EventData.Key evKey = new EventData.Key(accountID, deviceID, gpsTime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        evdb.setGeoPoint(geoPoint);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(heading);
        evdb.setAltitude(altitudeM);
        evdb.setDistanceKM(distanceKM);
        evdb.setOdometerKM(odomKM);
        evdb.setRawData(rawData);
        return evdb;
    }

    /* create and insert an event record */
    private void insertEventRecord(Device device, 
        long     gpsTime, int statusCode, GeoPoint geoPoint,
        double speedKPH, double heading, double altitudeM,
        double distanceKM, double odomKM, String rawData)
    {
        /* create event */
        EventData evdb = createEventRecord(device, 
            gpsTime, statusCode,
            geoPoint,
            speedKPH, heading, altitudeM,
            distanceKM, odomKM, rawData);

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

    public static void configInit() 
    {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc != null) {
            UNIQUEID_PREFIX         = dcsc.getUniquePrefix();
            MINIMUM_SPEED_KPH       = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
            ESTIMATE_ODOMETER       = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
            SIMEVENT_GEOZONES       = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
            XLATE_LOCATON_INMOTION  = dcsc.getStatusLocationInMotion(XLATE_LOCATON_INMOTION);
            MINIMUM_MOVED_METERS    = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);
        }
    }

    // ------------------------------------------------------------------------

}
