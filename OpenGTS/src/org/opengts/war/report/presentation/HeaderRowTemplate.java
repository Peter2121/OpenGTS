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
//  2007/11/28  Martin D. Flynn
//     -Integrated use of 'ReportColumn'
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class HeaderRowTemplate
{

    // ------------------------------------------------------------------------

    public static final String STYLE_CLASS      = "rptHdrRow";

    public static final String GROUP_ROW_CLASS  = "rptGrpRow";
    public static final String GROUP_COL_CLASS  = "rptGrpCol";

    // ------------------------------------------------------------------------

    private ReportTable reportTable = null;

    public HeaderRowTemplate(ReportTable rptTable) 
    {
        super();
        this.reportTable = rptTable;
    }

    // ------------------------------------------------------------------------

    public void writeHTML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {

        /* report data columns */
        ReportColumn rptCols[] = report.getReportColumns(); // ReportData
        if (ListTools.isEmpty(rptCols)) {
            String rptName = report.getReportFactory().getReportName();
            throw new ReportException("No report columns defined: " + rptName);
        }

        /* data row template */
        DataRowTemplate rdp = report.getDataRowTemplate();

        /* display column group titles */
        ReportHeaderGroup rptHdrGrp[] = report.getReportHeaderGroups();
        if (!ListTools.isEmpty(rptHdrGrp)) {
            String pfx1 = StringTools.replicateString(" ", level * ReportTable.INDENT);
            out.print("<tr");
            out.print(" class=\"" + GROUP_ROW_CLASS + "\"");
            out.print(">\n");
            for (ReportHeaderGroup rhg : rptHdrGrp) {
                String title = report.expandHeaderText(rhg.getTitle(report.getLocale()));
                out.print(pfx1);
                out.print("<th");
                out.print(" class=\"" + GROUP_COL_CLASS + "\"");
                out.print(" nowrap");
                out.print(" colSpan=\"" + rhg.getColSpan() + "\"");
                out.print(">");
                out.print(ReportTable.FilterText(title));
                out.print("</th>\n");
            }
            out.print("</tr>\n");
        }

        /* begin table header row */
        out.print("<tr");
        out.print(" class=\"" + STYLE_CLASS + "\"");
        out.print(">\n");

        /* display data columns */
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                hct.writeHTML(out, level+1, report, rptCols[i]);
            } else {
                String rptName = report.getReportFactory().getReportName();
                if (StringTools.isBlank(colName)) {
                    Print.logError("Column name not found: "+rptName+".#"+i);
                } else {
                    Print.logError("Column name not found: "+rptName+"."+colName +" [#"+i+"]");
                }
            }
        }

        /* end table header row */
        out.print("</tr>\n");

    }

    // ------------------------------------------------------------------------

    public void writeXML(PrintWriter out, int level, ReportData report)
        throws ReportException
    {
        boolean isSoapRequest = report.isSoapRequest();
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);

        /* report data columns */
        ReportColumn rptCols[] = report.getReportColumns(); // ReportData
        if (ListTools.isEmpty(rptCols)) {
            throw new ReportException("No report columns defined");
        }

        /* data row template */
        DataRowTemplate rdp = report.getDataRowTemplate();

        /* begin XML tag */
        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"HeaderRow",
            XMLTools.ATTR("class",STYLE_CLASS),
            false,true));

        /* display data columns */
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                hct.writeXML(out, level+1, report, rptCols[i]);
            } else {
                //Print.logError("Column name not found: " + rptCols[i]);
                Print.logStackTrace("Column name not found: " + rptCols[i].getKey());
            }
        }

        /* end XML tag */
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"HeaderRow",true));

    }

    // ------------------------------------------------------------------------

    public void writeXLS(ReportSpreadsheet rptSS, int level, ReportData report)
        throws ReportException
    {

        /* report data columns */
        ReportColumn rptCols[] = report.getReportColumns(); // ReportData
        if ((rptCols == null) || (rptCols.length == 0)) {
            throw new ReportException("No report columns defined");
        }

        /* data row template */
        DataRowTemplate rdp = report.getDataRowTemplate();

        /* display column group titles */
        ReportHeaderGroup rptHdrGrp[] = report.getReportHeaderGroups();
        if (!ListTools.isEmpty(rptHdrGrp)) {
            for (ReportHeaderGroup rhg : rptHdrGrp) {
                String title = report.expandHeaderText(rhg.getTitle(report.getLocale()));
                rptSS.addHeaderColumn(rhg.getColSpan(), title, 25/*charWidth*/);
            }
            rptSS.incrementRowIndex();
        }

        /* display data columns */
        for (int i = 0; i < rptCols.length; i++) {
            String colName = rptCols[i].getKey();
            DataColumnTemplate dct = rdp.getColumnTemplate(colName);
            if (dct != null) {
                HeaderColumnTemplate hct = this.reportTable.getHeaderColumnTemplate(dct);
                hct.writeXLS(rptSS, level+1, report, rptCols[i]);
            } else {
                //Print.logError("Column name not found: " + rptCols[i]);
                Print.logStackTrace("Column name not found: " + rptCols[i].getKey());
            }
        }
        rptSS.incrementRowIndex();

    }
    
    // ------------------------------------------------------------------------

}
