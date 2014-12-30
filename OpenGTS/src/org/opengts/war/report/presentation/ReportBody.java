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
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/06/30  Martin D. Flynn
//     -Added total row ('getTotalsDataIterator')
//  2007/12/13  Martin D. Flynn
//     -Added indication of partial data (ie. when 'limit' has been exceeded)
//  2007/01/10  Martin D. Flynn
//     -Fixed 'partial' indication when limit is '-1'
//  2009/05/01  Martin D. Flynn
//     -Removed "Totals" line from CSV generated output
//  2012/10/16  Martin D. Flynn
//     -"getRecordCount" and "isPartial" now thread-safe if ReportData defined.
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class ReportBody
{

    // ------------------------------------------------------------------------

    public static final String  PROP_ReportBody_debugXLS    = "ReportBody.debugXLS";

    // ------------------------------------------------------------------------

    private ReportTable         reportTable     = null;

    private BodyRowTemplate     bodyRow         = null;
    
    private boolean             debugXLS        = false;

    // ------------------------------------------------------------------------

    public ReportBody(ReportTable rptTable) 
    {
        this(rptTable, null);
    }

    protected ReportBody(ReportTable rptTable, BodyRowTemplate br) 
    {
        this.reportTable = rptTable;
        this.bodyRow     = (br != null)? br : new BodyRowTemplate(this.reportTable);
        this.debugXLS    = RTConfig.getBoolean(PROP_ReportBody_debugXLS,false);
    }

    // ------------------------------------------------------------------------

    private int        rcdCount  = 0;       // Not thread-safe
    private boolean    isPartial = false;   // Not thread-safe

    public int getRecordCount(ReportData rd)
    {
        if (rd != null) {
            return rd.getReportRecordCount();
        } else {
            // Not thread-safe, used only if "ReportData" is null
            return this.rcdCount;
        }
    }

    public boolean isPartial(ReportData rd)
    {
        if (rd != null) {
            return rd.getReportIsPartial();
        } else {
            // Not thread-safe, used only if "ReportData" is null
            return this.isPartial;
        }
    }

    private void _setRecordCount(ReportData rd, int count)
    {
        boolean partial = (count > 0)? this._overLimit(rd,(long)count) : false;
        if (rd != null) {
            rd.setReportRecordCount(count, partial);
        }
        // Not thread-safe, used only if "ReportData" is null
        this.rcdCount   = count;
        this.isPartial  = partial;
    }

    private boolean _overLimit(ReportData rd, long rptCount)
    {

        /* check report limit */
        long rptLimit = rd.getReportLimit();
        if ((rptLimit > 0L) && (rptCount >= rptLimit)) {
            Print.logInfo("Partial report data (RecordCount): " + rptCount + " >= " + rptLimit);
            return true;
        }
        //Print.logInfo("RecordCount: %d/%d", rptCount, rptLimit);

        /* check selection limit (applicable only for EventData queries) */
        long selLimit = rd.getSelectionLimit();
        if (selLimit > 10L) { // don't count small selection limit (other "1 >= 1" will cause a partial indication)
            long selCount = rd.getMaximumEventDataCount();
            if ((selLimit > 0L) && (selCount >= selLimit)) {
                Print.logInfo("Partial report data (maxSelectionCount): " + selCount + " >= " + selLimit);
                return true;
            }
            //Print.logInfo("SelectionCount: %d/%d", selCount, selLimit);
        }

        /* not over limit */
        return false;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {

        /* resulting state */
        this._setRecordCount(report, 0);

        /* HTML table body begin */
        out.print("<tbody>\n");

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            int RC = 0;
            for (RC = 0; data.hasNext(); RC++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeHTML(out, level+1, RC, false/*totals*/, dr);
                }
            }
            this._setRecordCount(report, RC);
        }

        /* report totals */
        DBDataIterator totals = report.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            for (int r = 0; totals.hasNext(); r++) {
                DBDataRow dr = totals.next();
                if (dr != null) {
                    this.bodyRow.writeHTML(out, level+1, r, true/*totals*/, dr);
                }
            }
        }

        /* HTML table body end */
        out.print("</tbody>\n");
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeXML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {
        boolean isSoapRequest = report.isSoapRequest();
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);

        /* resulting state */
        this._setRecordCount(report, 0);

        /* HTML table body begin */
        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"ReportBody","",false,true));

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            int RC = 0;
            for (RC = 0; data.hasNext(); RC++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeXML(out, level+1, RC, false/*totals*/, dr);
                }
            }
            this._setRecordCount(report, RC);
        }

        /* report totals */
        DBDataIterator totals = report.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            for (int r = 0; totals.hasNext(); r++) {
                DBDataRow dr = totals.next();
                if (dr != null) {
                    this.bodyRow.writeXML(out, level+1, r, true/*totals*/, dr);
                }
            }
        }

        /* HTML table body end */
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"ReportBody",true));

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final boolean INCLUDE_CSV_TOTALS = false;

    public void writeCSV(PrintWriter out, int level, ReportData report) 
        throws ReportException
    {

        /* resulting state */
        this._setRecordCount(report, 0);

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            int RC = 0;
            for (RC = 0; data.hasNext(); RC++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    this.bodyRow.writeCSV(out, level+1, RC, false/*totals*/, dr);
                }
            }
            this._setRecordCount(report, RC);
        }

        /* report totals */
        if (INCLUDE_CSV_TOTALS) {
            DBDataIterator totals = report.getTotalsDataIterator();
            if (totals != null) {
                for (int r = 0; totals.hasNext(); r++) {
                    DBDataRow dr = totals.next();
                    if (dr != null) {
                        this.bodyRow.writeCSV(out, level+1, r, true/*totals*/, dr);
                    }
                }
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void writeXLS(ReportSpreadsheet rptSS, int level, ReportData report) 
        throws ReportException
    {

        /* resulting state */
        this._setRecordCount(report, 0);

        /* report body */
        DBDataIterator data = report.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            int RC = 0;
            for (RC = 0; data.hasNext(); RC++) {
                DBDataRow dr = data.next();
                if (dr != null) {
                    if (this.debugXLS) Print.logInfo("XLS: Writing Report Body Row #" + RC);
                    this.bodyRow.writeXLS(rptSS, level+1, RC, dr);
                }
            }
            this._setRecordCount(report, RC);
        }

        /* report totals */
        DBDataIterator totals = report.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            for (int r = 0; totals.hasNext(); r++) {
                DBDataRow dr = totals.next();
                if (dr != null) {
                    if (this.debugXLS) Print.logInfo("XLS: Writing Report Total Row #" + r);
                    this.bodyRow.writeXLS(rptSS, level+1, r, dr);
                }
            }
        }

    }

    /**
    *** Sends the first line of the report to the callback method
    *** @param out     The stream to which the output is written
    *** @param level   The recursion level
    *** @param rd      The Report attributes
    **/
    public void writeCallback(OutputProvider out, int level, ReportData rd) 
        throws ReportException
    {

        /* resulting state */
        this._setRecordCount(rd, 0);

        /* get callback method */
        ReportCallback rptCB = rd.getReportCallback();
        if (rptCB == null) {
            return;
        }

        /* report body */
        DBDataIterator data = rd.getBodyDataIterator();
        if ((data != null) && data.hasNext()) {
            int RC = rptCB.reportBody(out, level+1, data);
            this._setRecordCount(rd, RC);
        }

        /* report totals */
        DBDataIterator totals = rd.getTotalsDataIterator();
        if ((totals != null) && totals.hasNext()) {
            rptCB.reportTotals(out, level+1, totals);
        }

    }

    // ------------------------------------------------------------------------

}
