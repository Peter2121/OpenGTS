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
//  2010/07/18  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;
import java.awt.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

/**
*** Parse XML location formats supported by Google (currently only GPX)
**/

public class ParseEventsXML
    implements ParseEvent.ParseEventHandler,
               GeoEvent.GeoEventHandler
{

    public static boolean   DEBUG_MODE      = false;

    // ------------------------------------------------------------------------
    // GPX tags

    private static final String TAG_gpx                 = "gpx";        // top level tag
    private static final String TAG_name                = "name";       // gpx name
    private static final String TAG_desc                = "desc";       // gpx description
    private static final String TAG_number              = "number";     // gpx number
    private static final String TAG_trk                 = "trk";        // a 'track' containing segments
    private static final String TAG_trkseg              = "trkseg";     // a 'track' segment containing points
    private static final String TAG_trkpt               = "trkpt";      // a 'track' point
    private static final String TAG_ele                 = "ele";        // a point altitude
    private static final String TAG_time                = "time";       // a point time [2010-07-11T23:44:12Z]

    private static final String ATTR_version            = "version";    // version
    private static final String ATTR_creator            = "creator";    // creator
    private static final String ATTR_lat                = "lat";        // latitude
    private static final String ATTR_lon                = "lon";        // longitude

    // ------------------------------------------------------------------------
    // KML tags

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String              accountID       = null;
    private String              deviceID        = null;
    private Account             account         = null;
    private Device              device          = null;

    private String              uniquePrefix[]  = new String[] { "gpx_" };
    private double              minSpeedKPH     = 0.0;
    private boolean             estimateOdom    = false;
    private boolean             simGeozones     = false;
    private double              minMovedMeters  = 0.0;

    /**
    *** Constructor
    **/
    public ParseEventsXML()
    {
        super();
    }

    /**
    *** Constructor
    **/
    public ParseEventsXML(String accountID, String deviceID)
    {
        super();
        this.setAccountDevice(accountID, deviceID);
    }

    // ------------------------------------------------------------------------

    public void setAccountDevice(String accountID, String deviceID)
    {
        if (!StringTools.isBlank(accountID) && !StringTools.isBlank(deviceID)) {
            this.accountID = StringTools.trim(accountID);
            this.deviceID  = StringTools.trim(deviceID);
        } else {
            this.accountID = null;
            this.deviceID  = null;
        }
        this.account = null;
        this.device  = null;
    }

    public boolean hasAccountDevice() 
    {
        return !StringTools.isBlank(this.accountID) && !StringTools.isBlank(this.deviceID);
    }

    public String getAccountID()
    {
        return this.accountID;
    }

    public String getDeviceID()
    {
        return this.deviceID;
    }

    // ------------------------------------------------------------------------

    public boolean parseStream(InputStream xmlStream, GeoEvent.GeoEventHandler gevHandler)
        throws IOException
    {

        /* get Document */
        Document xmlDoc = XMLTools.getDocument(xmlStream, false/*checkErrors*/);
        if (xmlDoc == null) {
            // errors already displayed
            return false;
        }

        /* get top-level tag */
        Element topElem = xmlDoc.getDocumentElement();
        String topLevelTagName = topElem.getTagName();

        /* GPS */
        if (topElem.getTagName().equalsIgnoreCase(TAG_gpx)) {
            return this._parse_gpx(topElem, gevHandler);
        }

        /* not supported */
        Print.logError("XML format not supported: " + topLevelTagName);
        return false;

    }

    // ------------------------------------------------------------------------

    public boolean _parse_gpx(Element topElem, GeoEvent.GeoEventHandler gevHandler)
    {

        /* top level attributes */
        String version = XMLTools.getAttribute(topElem, ATTR_version, "");
        String creator = XMLTools.getAttribute(topElem, ATTR_creator, "");

        /* tracks */
        NodeList trkNodes = XMLTools.getChildElements(topElem, TAG_trk);
        for (int trk = 0; trk < trkNodes.getLength(); trk++) {
            Element trkTag = (Element)trkNodes.item(trk);

            /* name */
            Element nameTag = XMLTools.getChildElement(trkTag, TAG_name);
            String name = (nameTag != null)? XMLTools.getNodeText(nameTag,""/*repNewline*/) : "";
            Print.logInfo("Track Name: " + name);
                       
            /* description */
            Element descTag = XMLTools.getChildElement(trkTag, TAG_desc);
            String desc = (descTag != null)? XMLTools.getNodeText(descTag,""/*repNewline*/) : "";
            Print.logInfo("Track Descrption: " + desc);

            /* number */
            Element numberTag = XMLTools.getChildElement(trkTag, TAG_number);
            int number = (numberTag != null)? StringTools.parseInt(XMLTools.getNodeText(numberTag,""),0) : 0;
            Print.logInfo("Track Number: " + number);
    
            /* track segments */
            NodeList trksegNodes = XMLTools.getChildElements(trkTag, TAG_trkseg);
            for (int seg = 0; seg < trksegNodes.getLength(); seg++) {
                Print.logInfo("Parsing Track Segment ...");
                Element trksegTag = (Element)trksegNodes.item(seg);
                NodeList trkptNodes = XMLTools.getChildElements(trksegTag, TAG_trkpt);
                for (int pt = 0; pt < trkptNodes.getLength(); pt++) {
                    Element trkptTag = (Element)trkptNodes.item(pt);
                    double latitude  = XMLTools.getAttributeDouble(trkptTag, ATTR_lat, 0.0);
                    double longitude = XMLTools.getAttributeDouble(trkptTag, ATTR_lon, 0.0);
                    Element eleTag   = XMLTools.getChildElement(trkptTag, TAG_ele);
                    double altitudeM = (eleTag != null)? StringTools.parseDouble(XMLTools.getNodeText(eleTag,""),0.0) : 0.0;
                    Element timeTag  = XMLTools.getChildElement(trkptTag, TAG_time);
                    long   timestamp = (timeTag != null)? this._parseTime(XMLTools.getNodeText(timeTag,"")) : 0L;
                    this._handleEvent(gevHandler,
                        timestamp, StatusCodes.STATUS_LOCATION,
                        latitude, longitude, altitudeM
                        );
                }
            }
            
        }
        return true;

    }
        
    // ------------------------------------------------------------------------

    protected long _parseTime(String timeStr)
    {
        // "2010-07-11T23:44:12Z"
        try {
            DateTime dt = DateTime.parseArgumentDate(timeStr);
            return dt.getTimeSec();
        } catch (DateTime.DateParseException dpe) {
            Print.logError("Date/Time parsing format error: " + timeStr);
            return 0L;
        }
    }
        
    // ------------------------------------------------------------------------

    protected void _handleEvent(GeoEvent.GeoEventHandler gevHandler,
        long timestamp, int statusCode,
        double latitude, double longitude,
        double altitudeM)
    {
        if (gevHandler != null) {
            GeoEvent gev = new GeoEvent();
            gev.setAccountID(this.getAccountID());
            gev.setDeviceID(this.getDeviceID());
            gev.setTimestamp(timestamp);
            gev.setStatusCode(statusCode);
            gev.setLatitude(latitude);
            gev.setLongitude(longitude);
            gev.setAltitudeMeters(altitudeM);
            gevHandler.handleGeoEvent(gev);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("[" + new DateTime(timestamp) + "] ");
            sb.append(StringTools.format(latitude,"0.00000"));
            sb.append("/");
            sb.append(StringTools.format(longitude,"0.00000"));
            sb.append("  ");
            sb.append(StringTools.format(altitudeM,"0") + " m");
            Print.logInfo("Point: " + sb);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback to handle event
    **/
    public int handleGeoEvent(GeoEvent gev)
    {
        int eventCount = 0;

        /* validate record identification */
        // We either have both AccountID/DeviceID, or neither
        if (gev.hasAccountID() != gev.hasDeviceID()) {
            Print.logError("Missing either Account or Device ID");
            return eventCount;
        }

        /* Account ID */
        if (gev.hasAccountID()) {
            if (!StringTools.isBlank(this.accountID) && !this.accountID.equals(gev.getAccountID())) {
                Print.logError("Mismatched AccountID!");
                return eventCount;
            }
        } else {
            if (!StringTools.isBlank(this.accountID)) {
                gev.setAccountID(this.accountID);
            }
        }
        String gevAcctID = gev.getAccountID(); // may be blank

        /* Device ID */
        if (gev.hasDeviceID()) {
            if (!StringTools.isBlank(this.deviceID) && !this.deviceID.equals(gev.getDeviceID())) {
                Print.logError("Mismatched DeviceID!");
                return eventCount;
            }
        } else {
            if (!StringTools.isBlank(this.deviceID)) {
                gev.setDeviceID(this.deviceID);
            }
        }
        String gevDevID = gev.getDeviceID(); // may be blank

        /* load device */
        String mobileID = gev.getMobileID();
        boolean validateMobileID = true;
        if (this.device == null) {
            if (!StringTools.isBlank(gevDevID)) {
                // load account record
               if (this.account == null) {
                    try {
                        this.account = Account.getAccount(gevAcctID); // may throw DBException
                        if (this.account == null) {
                            Print.logError("Account-ID does not exist: " + gevAcctID);
                            return eventCount;
                        }
                    } catch (DBException dbe) {
                        Print.logException("Error loading Account: " + gevAcctID, dbe);
                        return eventCount;
                    }
                }
                // load device record
                try {
                    this.device = Device.getDevice(this.account, gevDevID, false); // may throw DBException
                    if (this.device == null) {
                        Print.logError("Device-ID does not exist: " + gevAcctID + "/" + gevDevID);
                        return eventCount;
                    }
                } catch (DBException dbe) {
                    Print.logException("Error loading Device: " + gevAcctID + "/" + gevDevID, dbe);
                    return eventCount;
                }
            } else
            if (!StringTools.isBlank(mobileID)) {
                this.device = DCServerFactory._loadDeviceByPrefixedModemID(this.uniquePrefix, mobileID);
                if (this.device == null) {
                    // error messages already displayed
                    return eventCount;
                }
                if (this.account == null) {
                    this.account = this.device.getAccount();
                } else
                if (this.account.getAccountID().equals(this.device.getAccountID())) {
                    this.device.setAccount(this.account);
                } else {
                    Print.logError("Device AccountID does not match defined Account: " + this.device.getAccountID());
                    return eventCount;
                }
                // no need to validate mobile ID
                validateMobileID = false;
            } else {
                Print.logError("No Device/Mobile ID defined");
                return eventCount;
            }
        }

        /* validate MobileID */
        if (validateMobileID && !StringTools.isBlank(mobileID)) {
            boolean match = false;
            String uniqueID = this.device.getUniqueID();
            if (!StringTools.isBlank(uniqueID)) {
                for (String pfx : this.uniquePrefix) {
                    if (uniqueID.equals(pfx + mobileID)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                Print.logError("Unique-ID does not match Device: " + uniqueID);
                return eventCount;
            }
        }

        /* Account check */
        if (this.account == null) {
            this.account = this.device.getAccount();
        } else
        if (this.account.getAccountID().equals(this.device.getAccountID())) {
            this.device.setAccount(this.account);
        } else {
            Print.logError("Device AccountID does not match defined Account: " + this.device.getAccountID());
            return eventCount;
        }
        gev.setAccount(this.account);
        gev.setDevice(this.device);

        /* timestamp */
        long timestamp = gev.getTimestamp();
        if (timestamp <= 0L) {
            Print.logInfo("No valid Timestamp!");
            return eventCount;
        }

        /* GeoPoint */
        GeoPoint geoPoint = gev.getGeoPoint();
        boolean validGPS  = gev.isGeoPointValid();

        /* status code */
        int statusCode;
        if (!gev.hasStatusCode()) {
            statusCode = StatusCodes.STATUS_LOCATION;
            gev.setStatusCode(statusCode);
        } else {
            statusCode = gev.getStatusCode();
            if (statusCode <= 0) {
                Print.logInfo("No valid StatusCode!");
                return eventCount;
            }
        }

        /* minimum speed adjustment */
        if (!validGPS || (gev.getSpeedKPH() < this.minSpeedKPH)) {
            gev.setSpeedKPH(0.0);
            gev.setHeading(0.0);
        }

        /* odometer */
        double odometerKM = gev.hasOdometerKM()? gev.getOdometerKM() : 0.0;
        if (this.device != null) {
            if (odometerKM <= 0.0) {
                odometerKM = (this.estimateOdom && validGPS)? 
                    this.device.getNextOdometerKM(geoPoint) : 
                    this.device.getLastOdometerKM();
            } else {
                odometerKM = this.device.adjustOdometerKM(odometerKM);
            }
            gev.setOdometerKM(odometerKM);
        }

        /* simulate Geozone arrival/departure */
        if (this.simGeozones && validGPS && (this.device != null)) {
            java.util.List<Device.GeozoneTransition> zone = this.device.checkGeozoneTransitions(timestamp, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    GeoEvent zoneEv = new GeoEvent(gev); // clone
                    zoneEv.setTimestamp(z.getTimestamp());
                    zoneEv.setStatusCode(z.getStatusCode());
                    zoneEv.setGeozoneID(z.getGeozoneID());
                    zoneEv.setGeozone(z.getGeozone());
                    if (this.insertEventRecord(zoneEv)) {
                        eventCount++;
                    }
                    Print.logInfo("Geozone    : " + z);
                }
            }
        }

        /* previous event checks */
        if ((statusCode != StatusCodes.STATUS_LOCATION) || !validGPS) {
            if (this.insertEventRecord(gev)) {
                eventCount++;
            }
        } else
        if ((this.device == null) || !this.device.isNearLastValidLocation(geoPoint,this.minMovedMeters)) {
            if (this.insertEventRecord(gev)) {
                eventCount++;
            }
        }

        /* update device date */
        if (!DEBUG_MODE) {
            // TODO: optimize
            try {
                //DBConnection.pushShowExecutedSQL();
                if (this.device != null) {
                    this.device.updateChangedEventFields();
                }
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + gevAcctID + "/" + gevDevID, dbe);
            } finally {
                //DBConnection.popShowExecutedSQL();
            }
        } else {
            // TODO: reset any changes made to device record
        }

        /* return success */
        return eventCount;

    }

    protected boolean insertEventRecord(GeoEvent gev)
    {
        Print.logInfo("GeoEvent: " + gev);
        if (DEBUG_MODE) { return false; }
        // TODO:
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static String ARG_ACCOUNT[]  = new String[] { "account", "acct", "a" };
    public static String ARG_DEVICE[]   = new String[] { "device" , "dev" , "d" };
    public static String ARG_FILE[]     = new String[] { "file"                 };

    public static void main(String args[])
    {
        RTConfig.setCommandLineArgs(args);

        /* get XML (GPX) file */
        File xmlFile = RTConfig.getFile(ARG_FILE,null);
        if (xmlFile == null) {
            Print.sysPrintln("Missing '-file' specification");
            System.exit(1);
        }

        /* ParseEventsXML */
        String acctID = RTConfig.getString(ARG_ACCOUNT, null);
        String devID  = RTConfig.getString(ARG_DEVICE , null);
        ParseEventsXML pgx;
        GeoEvent.GeoEventHandler gevHandler;
        if (!StringTools.isBlank(acctID) && !StringTools.isBlank(devID)) {
            pgx = new ParseEventsXML(acctID, devID);
            gevHandler = pgx;
        } else {
            pgx = new ParseEventsXML();
            gevHandler = null;
        }

        /* read/parse XML (GPX) file */
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(xmlFile);
            pgx.parseStream(fis, gevHandler);
        } catch (IOException ioe) {
            Print.logException("IO Error", ioe);
        } finally {
            if (fis != null) { try { fis.close(); } catch (Throwable th) {} }
        }
        
    }
    
}

