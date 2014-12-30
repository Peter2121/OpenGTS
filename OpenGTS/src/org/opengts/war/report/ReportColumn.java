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
//  2007/11/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.report.*;

public class ReportColumn
{

    // ------------------------------------------------------------------------

    private String              colKey          = "";
    private String              colArg          = "";
    private I18N.Text           colTitle        = null;
    private int                 colSpan         = 1;

    private boolean             sortable        = true;

    private String              blankFiller     = "";

    private ReportConditional   conditional     = null;

    /**
    *** Constructor
    *** @param key   Report column name
    *** @param arg   Report column parameters
    *** @param title Report column title
    **/
    public ReportColumn(String key, String arg, I18N.Text title)
    {
        this.colKey   = (key != null)? key : "";
        this.colArg   = (arg != null)? arg : "";
        this.colTitle = ((title != null) && !title.toString().equals(""))? title : null;
        //if (StringTools.isBlank(this.colKey)) { Print.logStackTrace("Report column name is blank!"); }
    }

    /**
    *** Constructor
    *** @param key     Report column name
    *** @param arg     Report column parameters
    *** @param colSpan Number of columns spanned by this column title
    *** @param title   Report column title
    **/
    public ReportColumn(String key, String arg, int colSpan, I18N.Text title)
    {
        this(key, arg, title);
        this.colSpan = colSpan;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the column name(key)<br>
    *** Same as <code>getKey()</code>
    *** @return The column name/key
    *** @see getKey
    **/
    public String getName()
    {
        return this.colKey;
    }

    /**
    *** Gets the column name(key) 
    *** Same as <code>getName()</code>
    *** @return The column name/key
    *** @see getName
    **/
    public String getKey()
    {
        return this.colKey;
    }

    /**
    *** Gets the column arguments/parameters
    *** @return The column arguments/parameters (as a String)
    **/
    public String getArg()
    {
        return this.colArg; // should never be null
    }

    /**
    *** Gets the column span value
    *** @return The column span value
    **/
    public int getColSpan()
    {
        return this.colSpan;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the localized column title
    *** @param loc   The Locale
    *** @param dft   The default title to return if no column title defined
    *** @return The localized column title
    **/
    public String getTitle(Locale loc, String dft)
    {
        return (this.colTitle != null)? this.colTitle.toString(loc) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the sortable flag for this column
    *** @param sortable True if column is sortable, false otherwise
    **/
    public void setSortable(boolean sortable)
    {
        this.sortable = sortable;
    }

    /**
    *** Gets the sortable flag for this column
    *** @return True if column is sortable, false otherwise
    **/
    public boolean isSortable()
    {
        return this.sortable;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the value to use as a blank-value for this column
    *** @param filler  The value to use as a blank-value for this column
    **/
    public void setBlankFiller(String filler)
    {
        this.blankFiller = (filler != null)? filler : "";
    }

    /**
    *** Gets the value to use as a blank-value for this column
    *** @return  The value to use as a blank-value for this column
    **/
    public String getBlankFiller()
    {
        return this.blankFiller;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the column conditional value
    *** @param colCond  The column conditional value
    **/
    public void setColumnConditional(ReportConditional colCond)
    {
        this.conditional = colCond;
    }

    /**
    *** Returns true if this column is visible
    *** @param bpl  The BasicPrivateLabel context
    *** @return True if this column is visible, false otherwise
    **/
    public boolean isVisible(BasicPrivateLabel bpl)
    {
        if (this.conditional != null) {
            return this.conditional.isTrue(bpl);
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the String representation of this column
    *** @return The String representation of this column
    **/
    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getKey()).append(":").append(this.getArg());
        String title = this.getTitle(null,"");
        if (title != null) {
            sb.append(" - ").append(title);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
