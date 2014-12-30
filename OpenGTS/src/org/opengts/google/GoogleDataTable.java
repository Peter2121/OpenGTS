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
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

/**
*** Google DataTable wrapper
**/

public class GoogleDataTable
{

    // ------------------------------------------------------------------------

    /**
    *** Column class
    **/
    public static class Column
    {
        private String id    = "";
        private String label = "";
        private String type  = "";
        public Column(String id, String label, String type) {
            this.id    = StringTools.trim(id);
            this.label = StringTools.trim(label);
            this.type  = StringTools.trim(type).toLowerCase();
            if (this.type.equals("date")) {
                // 
            } else
            if (this.type.equals("datetime")) {
                // 
            } else
            if (this.type.equals("number")) {
                // 
            } else
            if (this.type.equals("string")) {
                // 
            } else {
                this.type = "string";
            }
        }
        // --
        public boolean isTypeDate() {
            return this.type.equals("date") || this.type.equals("datetime");
        }
        public boolean isTypeNumber() {
            return this.type.equals("number");
        }
        public boolean isTypeString() {
            return this.type.equals("string");
        }
        // --
        public String toJavaScriptObject() {
            // { id: "date" , label: "Date/Time", type: "datetime" },
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            sb.append("id:").append("\"").append(id).append("\"");
            sb.append(", ");
            sb.append("label:").append("\"").append(label).append("\"");
            sb.append(", ");
            sb.append("type:").append("\"").append(type).append("\"");
            sb.append(" }");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Row class
    **/
    public static class Row
    {
        private Object data[] = null;
        public Row(int size) {
            this.data = (size >= 0)? new Object[size] : null;
        }
        public Row(Object data[]) {
            this.data = data;
        }
        // --
        public Object[] getData() {
            return this.data;
        }
        // --
        public void setValue(int ndx, String val) {
            if ((this.data != null) && (ndx >= 0) && (ndx < this.data.length)) {
                this.data[ndx] = val;
            }
        }
        public void setValue(int ndx, Number val) {
            if ((this.data != null) && (ndx >= 0) && (ndx < this.data.length)) {
                this.data[ndx] = val;
            }
        }
        public void setValue(int ndx, long val) {
            if ((this.data != null) && (ndx >= 0) && (ndx < this.data.length)) {
                this.data[ndx] = new Long(val);
            }
        }
        public void setValue(int ndx, double val) {
            if ((this.data != null) && (ndx >= 0) && (ndx < this.data.length)) {
                this.data[ndx] = new Double(val);
            }
        }
        // --
        public Object getValue(int ndx) {
            if ((this.data != null) && (ndx >= 0) && (ndx < this.data.length)) {
                return this.data[ndx]; // may be null;
            } else {
                return null;
            }
        }
        // --
        public String toJavaScriptObject(Column cols[]) {
            // { c: [ { v: new Date(1383914237000) }, { v: -12.6 }, { v: -18.1 } ] },
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            sb.append("c: ").append("[ ");
            for (int c = 0; c < cols.length; c++) {
                Column C = cols[c];
                if (c > 0) { sb.append(", "); }
                sb.append("{");
                sb.append("v:");
                Object val = this.getValue(c);
                if (val == null) {
                    sb.append("null");
                } else
                if (C.isTypeDate()) {
                    sb.append("new Date(");
                    if (val instanceof DateTime) {
                        sb.append(((DateTime)val).getTimeMillis());
                    } else
                    if (val instanceof java.util.Date) {
                        sb.append(((java.util.Date)val).getTime());
                    } else {
                        sb.append(StringTools.parseLong(val,0L) * 1000L);
                    }
                    sb.append(")");
                } else
                if (C.isTypeNumber()) {
                    if (val instanceof Number) {
                        sb.append(val.toString());
                    } else
                    if (val instanceof String) {
                        if (((String)val).indexOf(".") >= 0) {
                            sb.append(StringTools.parseDouble(val,0.0));
                        } else {
                            sb.append(StringTools.parseLong(val,0L));
                        }
                    } else {
                        sb.append("null");
                    }
                } else {
                    sb.append("\"");
                    sb.append(val.toString()); // assume no embedded quotes
                    sb.append("\"");
                }
                sb.append("}");
            }
            sb.append(" ]");
            sb.append(" }");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          title   = "";
    private Vector<Column>  columns = new Vector<Column>();
    private Vector<Row>     rows    = new Vector<Row>();

    /**
    *** Constructor
    **/
    public GoogleDataTable() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /**
    *** Set title
    **/
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
    *** Get title
    **/
    public String getTitle()
    {
        return this.title;
    }

    // ------------------------------------------------------------------------

    /**
    *** Add column
    **/
    public void addColumn(Column col)
    {
        if (col != null) {
            this.columns.add(col);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Add row
    **/
    public void addRow(Row row) 
    {
        if (row != null) {
            this.rows.add(row);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Class RowComparator
    **/
    public static class RowComparator
        implements Comparator<Row>
    {
        public RowComparator() {
            super();
        }
        public int compare(Row r1, Row r2) {
            Object v1 = r1.getValue(0);
            Object v2 = r2.getValue(0);
            if (v1 == null) {
                if (v2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else
            if (v2 == null) {
                return 1;
            } else
            if ((v1 instanceof Long) && (v2 instanceof Long)) {
                // both are Long (Number class does not have a "compare" method)
                long n1 = ((Number)v1).longValue();
                long n2 = ((Number)v2).longValue();
                return (n1 == n2)? 0 : (n1 < n2)? -1 : 1;
            } else
            if ((v1 instanceof Double) && (v2 instanceof Double)) {
                // both are Double (Number class does not have a "compare" method)
                double n1 = ((Number)v1).doubleValue();
                double n2 = ((Number)v2).doubleValue();
                return (n1 == n2)? 0 : (n1 < n2)? -1 : 1;
            } else
            if ((v1 instanceof DateTime) && (v2 instanceof DateTime)) {
                return ((DateTime)v1).compareTo(v2);
            } else
            if ((v1 instanceof Date) && (v2 instanceof Date)) {
                return ((Date)v1).compareTo((Date)v2);
            } else
            if ((v1 instanceof Number) != (v2 instanceof Number)) {
                // one of these is a Number, and the other is not-a-Number
                return (v1 instanceof Number)? -1 : 1; // numbers sort before non-numbers
            } else {
                // both are not-a-Number
                String s1 = v1.toString();
                String s2 = v2.toString();
                return s1.compareTo(s2);
            }
        }
        public boolean equals(Object other) {
            return (other instanceof RowComparator);
        }
    }

    /**
    *** Sort the row data by the first column
    **/
    public void sortByFirstColumn() 
    {
        ListTools.sort(this.rows, new RowComparator(), true);
    }

    // ------------------------------------------------------------------------

}
