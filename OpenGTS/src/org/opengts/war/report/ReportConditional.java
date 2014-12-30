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
//  2014/01/01  Martin D. Flynn
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

public class ReportConditional
{

    // ------------------------------------------------------------------------

    /**
    *** Creates a new ReportConditional if either "trueProp" or "falseProp" is specified (non-blank)
    *** Returns null if both "trueProp" and "falseProp" are null/blank.
    *** @param trueProp  The properties to check for "true"
    *** @param falseProp The properties to check for "false"
    *** @return The new <code>ReportCondition</code> instance, or null if both "trueProp" and
    ***         "falseProp" are null/blank.
    **/
    public static ReportConditional createReportConditional(String trueProp, String falseProp)
    {
        String tp = !StringTools.isBlank(trueProp )? trueProp.trim()  : null;
        String fp = !StringTools.isBlank(falseProp)? falseProp.trim() : null;
        if ((tp != null) || (fp != null)) {
            return new ReportConditional(tp, fp);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private OrderedSet<String>  trueProps     = null;
    private OrderedSet<String>  falseProps    = null;

    /**
    *** Constructor
    **/
    public ReportConditional()
    {
        super();
    }

    /**
    *** Constructor
    **/
    public ReportConditional(String trueProp, String falseProp)
    {
        this();
        this.addTruePropertyKey(trueProp);
        this.addFalsePropertyKey(falseProp);
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified property key to the list of keys to check for "true"
    *** Undefined property keys will be skipped.  The propery key test will stop at the first
    *** defined property key found.
    *** @param prop  The property key to add
    **/
    public void addTruePropertyKey(String prop)
    {
        if (!StringTools.isBlank(prop)) {
            if (this.trueProps == null) { this.trueProps = new OrderedSet<String>(); }
            this.trueProps.add(prop);
        }
    }

    /**
    *** Adds the specified list of property keys to the list of keys to check for "true"
    *** Undefined property keys will be skipped.  The propery key test will stop at the first
    *** defined property key found.
    *** @param props  The property key list to add
    **/
    public void addTruePropertyKeys(String props[])
    {
        if (!ListTools.isEmpty(props)) {
            for (String prop : props) {
                this.addTruePropertyKey(prop);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified property key to the list of keys to check for "false"
    *** Undefined property keys will be skipped.  The propery key test will stop at the first
    *** defined property key found.
    *** @param prop  The property key to add
    **/
    public void addFalsePropertyKey(String prop)
    {
        if (!StringTools.isBlank(prop)) {
            if (this.falseProps == null) { this.falseProps = new OrderedSet<String>(); }
            this.falseProps.add(prop);
        }
    }

    /**
    *** Adds the specified list of property keys to the list of keys to check for "false"<br>
    *** Undefined property keys will be skipped.  The propery key test will stop at the first
    *** defined property key found.
    *** @param props  The property key list to add
    **/
    public void addFalsePropertyKeys(String props[])
    {
        if (!ListTools.isEmpty(props)) {
            for (String prop : props) {
                this.addFalsePropertyKey(prop);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the property value for the specified property key
    *** @param bpl     The PrivateLabel context
    *** @param propKey The property key to retrieve.
    *** @return The String value of the property key
    **/
    private String _getPropertyValue(BasicPrivateLabel bpl, OrderedSet<String> propKeys)
    {

        /* no property keys? */
        if (ListTools.isEmpty(propKeys)) {
            return null;
        }

        /* PrivateLabel check */
        if (bpl != null) {
            for (String prop : propKeys) {
                if (bpl.hasProperty(prop)) {
                    String val = bpl.getStringProperty(prop,null);
                    if (!StringTools.isBlank(val)) {
                        return val;
                    }
                }
            }
        }

        /* RTConfig check */
        for (String prop : propKeys) {
            if (RTConfig.hasProperty(prop)) {
                String val = RTConfig.getString(prop,null);
                if (!StringTools.isBlank(val)) {
                    return val;
                }
            }
        }

        /* not found */
        return null;

    }

    /**
    *** Returns the conditional value for the PrivateLabel context
    *** @return The value of the property conditions
    **/
    public boolean isTrue(BasicPrivateLabel bpl)
    {

        /* check 'true' props */
        String truePropVal = this._getPropertyValue(bpl, this.trueProps);
        if (truePropVal != null) {
            // property is explicitly defined
            return StringTools.parseBoolean(truePropVal,false);
        }

        /* check 'false' props */
        String falsePropVal = this._getPropertyValue(bpl, this.falseProps);
        if (falsePropVal != null) {
            // property is defined
            return !StringTools.parseBoolean(falsePropVal,true);
        }

        /* true, if we reach here */
        return true;

    }

}
