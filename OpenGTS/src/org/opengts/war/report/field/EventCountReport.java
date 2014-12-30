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
//  2007/01/10  Martin D. Flynn
//     -Initial release
//  2012/04/16  Martin D. Flynn
//     -Added "SummarizeByDay" option
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

public class EventCountReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Summary report
    // 1 'count' record per device
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_summarizeByDay     = "summarizeByDay";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private java.util.List<FieldData>   rowData         = null;
    
    private boolean                     summarizeByDay  = false;

    // ------------------------------------------------------------------------

    /**
    *** Event Count Report Constructor
    *** @param rptEntry The ReportEntry
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventCountReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }
        //if (this.getDeviceCount() < 1) {
        //    throw new ReportException("At least 1 Device must be specified");
        //}
        // report on all authorized devices
        //this.getReportDeviceList().addAllAuthorizedDevices();
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        RTProperties rtp = this.getProperties();
        //ReportConstraints rc = this.getReportConstraints();
        //Print.logInfo("LimitType=" + rc.getSelectionLimitType() + ", Limit=" + rc.getSelectionLimit());
        this.summarizeByDay = rtp.getBoolean(PROP_summarizeByDay, false);
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

        /* init */
        this.rowData = new Vector<FieldData>();

        /* loop through devices */
        String devID = "";
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator i = devList.iterator(); i.hasNext();) {
            devID = (String)i.next();
            try {

                /* get Device record */
                Device device = devList.getDevice(devID);
                if (device == null) {
                    // unlikely
                    Print.logError("Returned DeviceList 'Device' is null: " + devID);
                    continue;
                }

                /* report date range */
                long startTime = this.getTimeStart();
                long   endTime = this.getTimeEnd();
                TimeZone    tz = this.getTimeZone();

                /* summarize type? */
                if (this.summarizeByDay) {

                    // summarize by day
                    // count events by day between date-range
                    for (long dayStart = startTime; dayStart < endTime;) {
                        // calculate start/end time for current day
                        DateTime dayDT = new DateTime(dayStart,tz);
                        long dayEnd = dayDT.getDayEnd(tz); // 23:59:59
                        if (dayEnd > endTime) { dayEnd = endTime; } // will exit on next iteration
                        // get counts
                        long rcdCount = this.countEventData(device, dayStart, dayEnd);
                        // create report record
                        FieldData fd = new FieldData();
                        fd.setDevice(device);
                        fd.setString(FieldLayout.DATA_DEVICE_ID, devID);
                        fd.setLong(  FieldLayout.DATA_DATE     , dayDT.getDayNumber(tz));
                        fd.setLong(  FieldLayout.DATA_COUNT    , rcdCount);
                        this.rowData.add(fd); // single record per device
                        // next day
                        dayStart = dayEnd + 1; // beginning of next day
                    }

                } else {

                    /* count total events for date-range */
                    long rcdCount = this.countEventData(device, startTime, endTime);
                    FieldData fd = new FieldData();
                    fd.setDevice(device);
                    fd.setString(FieldLayout.DATA_DEVICE_ID, devID);
                    fd.setLong(  FieldLayout.DATA_COUNT    , rcdCount);
                    this.rowData.add(fd); // single record per device
                    
                }

            } catch (DBException dbe) {

                // error encountered
                Print.logError("Error retrieving EventData count for Device: " + devID);

            }

        }

        /* return data iterator */
        FieldData.sortByDeviceDescription(this.rowData);
        return new ListDataIterator(this.rowData);
        
    }

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------

}
