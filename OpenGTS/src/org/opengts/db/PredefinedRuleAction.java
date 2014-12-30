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
//  2011/12/06  Martin D. Flynn
//     -Initial release (EXPERIMENTAL)
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
                        
import org.opengts.util.*;

import org.opengts.db.tables.*;

public class PredefinedRuleAction
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String      action  = "";
    private String      arg     = "";
    private I18N.Text   desc    = null;

    public PredefinedRuleAction(String action, String arg, I18N.Text desc) 
    {
        this.action = StringTools.trim(action);
        this.arg    = StringTools.trim(arg);
        this.desc   = desc;
    }

    // ------------------------------------------------------------------------

    public String getAction() 
    {
        return this.action;
    }

    public String getArg() 
    {
        return this.arg;
    }

    public String getDescription(Locale loc) 
    {
        return this.desc.toString(loc);
    }

    // ------------------------------------------------------------------------

    public String toString() 
    {
        return StringTools.isBlank(this.arg)? this.action : (this.action + ":" + this.arg);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}

