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
//  2013/12/26  Martin D. Flynn
//      - Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;

/**
*** Temperature container
**/

public class TemperatureSet
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static OrderedMap<Long,Double[]> _MergeDataSets(boolean F, TemperatureSet TSList[])
    {

        /* merge temperature data sets */
        OrderedMap<Long,Double[]> rowMap = new OrderedMap<Long,Double[]>();
        if (!ListTools.isEmpty(TSList)) {
            for (int d = 0; d < TSList.length; d++) {
                TemperatureSet TS = TSList[d];
                if (TS != null) {
                    Collection<Temperature> TList = TS.getTemperatures();
                    for (Temperature T : TList) {
                        Long   ts    = new Long(T.getTimestamp());
                        Double tmp   = new Double(T.getTemperature(F));
                        Double row[] = rowMap.get(ts);
                        if (row == null) {
                            row = new Double[TSList.length];
                            rowMap.put(ts, row);
                        }
                        row[d] = tmp;
                    }
                }
            }
        }

        /* sort by timestamp */
        rowMap.sortKeys(new ListTools.NumberComparator<Long>());

        /* return */
        return rowMap;

    }

    /**
    *** Creates a Google DataTable containing the specified TemperatureSet Data
    *** @param F  True for Fahrenheit, false for Celsius
    *** @param TSList  The TemperatureSet data array
    *** @return The "DataTable" String.
    **/
    public static String CreateGoogleDataTableJavaScript(boolean F, TemperatureSet... TSList)
    {
        // {
        //   cols: [
        //       { id: "date" , label: "Date/Time", type: "datetime" },
        //       { id: "temp1", label: "Temp-1"   , type: "number"   },
        //       { id: "temp2", label: "Temp-2"   , type: "number"   }
        //   ],
        //   rows: [
        //       { c: [ { v: new Date(1383914237000) }, { v: -12.6 }, { v: -18.1 } ] },
        //       { c: [ { v: new Date(1384914237000) }, { v:  -5.1 }, { v:  -7.3 } ] },
        //       { c: [ { v: new Date(1385914345000) }, { v:  null }, { v:  -2.1 } ] },
        //       { c: [ { v: new Date(1386924683000) }, { v:  -2.0 }, { v:  null } ] },
        //       { c: [ { v: new Date(1387934245000) }, { v:   5.8 }, { v:   6.7 } ] }
        //   ]
        // }

        /* merge temperature data sets */
        OrderedMap<Long,Double[]> rowMap = _MergeDataSets(F, TSList);

        /* init */
        StringBuffer sb = new StringBuffer();
        sb.append("{").append("\n");

        /* "cols" */
        sb.append("  cols: [").append("\n");
        // --
        sb.append("    { id:\"date\", label:\"Date/Time\", type:\"datetime\" },").append("\n");
        // --
        for (int d = 0; d < TSList.length; d++) {
            String id    = "temp" + (d+1);
            String label = "Temp-" + (d+1);
            String type  = "number";
            sb.append("    { id:\""+id+"\", label:\""+label+"\", type:\""+type+"\" },").append("\n");
        }
        // --
        sb.append("  ],").append("\n");

        /* "rows" */
        // { c: [ { v: new Date(1383914237000) }, { v: -12.6 }, { v: -18.1 } ] },
        sb.append("  rows: [").append("\n");
        int rows = 0, rowCnt = rowMap.size();
        for (Long ts : rowMap.keySet()) {
            sb.append("    { c: [ ");
            sb.append("{v:new Date("+(ts.longValue()*1000L)+")}, ");
            Double tmp[] = rowMap.get(ts);
            for (int t = 0; t < tmp.length; t++) {
                Double D = tmp[t];
                if (t > 0) { sb.append(", "); }
                String Ds = (D!=null)? StringTools.format(D,"0.0") : "null";
                sb.append("{v:"+Ds+"}");
            }
            sb.append(" ]}");
            if (rows < (rowCnt - 1)) { sb.append(","); }
            sb.append("\n");
            rows++;
        }
        sb.append("  ]").append("\n");

        /* return */
        sb.append("}");
        return sb.toString();

    }

    private static String CreateGoogleDataTableJSON(boolean F, TemperatureSet... TSList)
    {

        /* merge temperature data sets */
        OrderedMap<Long,Double[]> rowMap = _MergeDataSets(F, TSList);

        /* create JSON "cols" Object */
        JSON._Array dataTable_cols = new JSON._Array();
        // -- DateTime
        JSON._Object col_dateTime = (new JSON._Object()).setFormatIndent(false);
        col_dateTime.addKeyValue("id"   , "date");
        col_dateTime.addKeyValue("label", "Date/Time");
        col_dateTime.addKeyValue("type" , "datetime");
        dataTable_cols.addValue(col_dateTime);
        // -- data set titles
        if (!ListTools.isEmpty(TSList)) {
            for (int d = 0; d < TSList.length; d++) {
                TemperatureSet TS = TSList[d];
                JSON._Object col_temp = (new JSON._Object()).setFormatIndent(false);
                col_temp.addKeyValue("id"   , "temp" + (d+1));
                col_temp.addKeyValue("label", "Temp-" + (d+1));
                col_temp.addKeyValue("type" , "number");
                dataTable_cols.addValue(col_temp);
            }
        }

        /* create JSON "rows" Object */
        JSON._Array dataTable_rows = new JSON._Array();
        for (Long ts : rowMap.keySet()) {
            JSON._Object col = new JSON._Object();
            // TODO
        }

        /* return */
        return null; // TODO:

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String              title       = "Temperature";
    private Vector<Temperature> tempList    = new Vector<Temperature>();
    private double              minTempC    =  9999.0;
    private double              maxTempC    = -9999.0;

    /**
    *** Constructor
    **/
    public TemperatureSet()
    {
        super();
    }

    /**
    *** Constructor
    **/
    public TemperatureSet(String title, Temperature TList[])
    {
        this.title = StringTools.trim(title);
        if (!ListTools.isEmpty(TList)) {
            for (Temperature T : TList) {
                this.addTemperature(T);
            }
        }
    }

    /**
    *** Constructor
    **/
    public TemperatureSet(String title, Collection<Temperature> TList)
    {
        this.title = StringTools.trim(title);
        if (!ListTools.isEmpty(TList)) {
            for (Temperature T : TList) {
                this.addTemperature(T);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the temperature data set title
    **/
    public void setTitle(String title)
    {
        this.title = StringTools.trim(title);
    }

    /**
    *** Gets the temperature data set title
    **/
    public String getTitle()
    {
        return this.title;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified temperature to this data set
    *** @param ts  The timestamp (in seconds)
    *** @param C   The temperature (in Celsius)
    **/
    public boolean addTemperature(long ts, double C)
    {
        return this.addTemperature(new Temperature(ts, C));
    }

    /**
    *** Adds the specified temperature to this data set
    **/
    public boolean addTemperature(Temperature T)
    {
        if (Temperature.isValid(T)) { // valid timestamp and temperature
            this.tempList.add(T);
            double tempC = T.getTemperatureC();
            if (tempC < this.minTempC) { this.minTempC = tempC; }
            if (tempC > this.maxTempC) { this.maxTempC = tempC; }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Update min/max temperatures
    **/
    private void _updateMinMaxTemperature()
    {
        this.minTempC =  9999.0;
        this.maxTempC = -9999.0;
        for (Temperature T : this.tempList) {
            double tempC = T.getTemperatureC(); // temperature guaranteed valid
            if (tempC < this.minTempC) { this.minTempC = tempC; }
            if (tempC > this.maxTempC) { this.maxTempC = tempC; }
        }
    }

    /**
    *** Get minimum Temperature
    **/
    public double getMinimumTemperature()
    {
        return this.minTempC;
    }

    /**
    *** Get maximum Temperature
    **/
    public double getMaximumTemperature()
    {
        return this.maxTempC;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Get size of TemperatureSet
    **/
    public int getSize()
    {
        return ListTools.size(this.tempList);
    }

    /**
    *** Gets the temperature data set
    **/
    public Collection<Temperature> getTemperatures()
    {
        return this.tempList; // non-null
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        long ts = DateTime.getCurrentTimeSec();
        double C = -5.6;

        TemperatureSet TSList[] = new TemperatureSet[3];
        for (int s = 0; s < TSList.length; s++) {
            TemperatureSet TS = new TemperatureSet();
            for (int i = 0; i < 10; i++) {
                long tempTS = ts + (12345L * (i + 1) * (s + 1));
                double tempC = C + (2.3 * i) + (1.1 * s);
                Print.logInfo(s + ":" + i + "] Adding " + tempTS + ", " + tempC);
                TS.addTemperature(tempTS, tempC);
            }
            TSList[s] = TS;
        }

        String t = TemperatureSet.CreateGoogleDataTableJavaScript(false, TSList);
        Print.sysPrintln(t);

    }

}
