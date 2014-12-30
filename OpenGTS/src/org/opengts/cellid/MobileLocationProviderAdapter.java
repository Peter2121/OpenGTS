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
//  2011/07/01  Martin D. Flynn
//      -Initial release
// ----------------------------------------------------------------------------
package org.opengts.cellid;

import org.opengts.util.*;

import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.Account;

/**
*** Adapter class for obtaining the location of a Cell Tower
**/
public abstract class MobileLocationProviderAdapter
{

    // ------------------------------------------------------------------------

    public static final String PROP_MobileLocationProvider_ = "MobileLocationProvider.";
    public static final String _PROP_isEnabled              = ".isEnabled";

    // ------------------------------------------------------------------------

    private String       name           = null;
    private TriState     isEnabled      = TriState.UNKNOWN;
    
    private String       accessKey      = null;
    private RTProperties properties     = null;

    /**
    *** Constructor
    *** @param name  The name of this reverse-geocode provider
    *** @param key     The access key (may be null)
    *** @param rtProps The properties (may be null)
    **/
    public MobileLocationProviderAdapter(String name, String key, RTProperties rtProps)
    {
        super();
        this.setName(name);
        this.setAuthorization(key);
        this.setProperties(rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this MobileLocationProvider
    *** @param name  The name of this MobileLocationProvider
    **/
    public void setName(String name)
    {
        this.name = (name != null)? name : "";
    }

    /**
    *** Gets the name of this MobileLocationProvider
    *** @return The name of this MobileLocationProvider
    **/
    public String getName()
    {
        return (this.name != null)? this.name : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this MobileLocationProvider is enabled
    *** @return True if this MobileLocationProvider is enabled, false otherwise
    **/
    public boolean isEnabled()
    {
        if (this.isEnabled.isUnknown()) {
            String key = PROP_MobileLocationProvider_ + this.getName() + _PROP_isEnabled;
            if (RTConfig.getBoolean(key,true)) {
                this.isEnabled = TriState.TRUE;
            } else {
                this.isEnabled = TriState.FALSE;
                Print.logWarn("MobileLocationProvider disabled: " + this.getName());
            }
        }
        return this.isEnabled.isTrue();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the authorization key of this MobileLocationProvider
    *** @param key  The key of this MobileLocationProvider
    **/
    public void setAuthorization(String key)
    {
        this.accessKey = key;
    }
    
    /**
    *** Gets the authorization key of this MobileLocationProvider
    *** @return The access key of this MobileLocationProvider
    **/
    public String getAuthorization()
    {
        return this.accessKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the properties for this ReverseGeocodeProvider
    *** @param rtProps  The properties for this reverse-geocode provider
    **/
    public void setProperties(RTProperties rtProps)
    {
        this.properties = rtProps;
    }

    /**
    *** Gets the properties for this ReverseGeocodeProvider
    *** @return The properties for this reverse-geocode provider
    **/
    public RTProperties getProperties()
    {
        if (this.properties == null) {
            this.properties = new RTProperties();
        }
        return this.properties;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb= new StringBuffer();
        sb.append(this.getName());
        String auth = this.getAuthorization();
        if (!StringTools.isBlank(auth)) {
            sb.append(" [");
            sb.append(auth);
            sb.append("]");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the location of Cell Tower indicated by the attributes
    *** specified in the CellTower instance.
    *** @param servCT  The serving Cell Tower information
    *** @param nborCT  Neightbor Cell Tower information
    *** @return The Mobile location of the Cell Tower, or null if no
    ***     location could be determined.
    **/
    public abstract MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_HELP[]      = new String[] { "helo"             };
    private static final String ARG_ACCOUNT[]   = new String[] { "account"          , "acct"       };
    private static final String ARG_PLN[]       = new String[] { "privateLabelName" , "pln" , "pl" };
    private static final String ARG_CID[]       = new String[] { "cid" , "cellID"   };
    private static final String ARG_MCC[]       = new String[] { "mcc"              };
    private static final String ARG_MNC[]       = new String[] { "mnc"              };
    private static final String ARG_LAC[]       = new String[] { "lac"              };

    private static void usage()
    {
        String n = MobileLocationProviderAdapter.class.getName();
        Print.sysPrintln("");
        Print.sysPrintln("Description:");
        Print.sysPrintln("   Mobile Location Testing Tool ...");
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("   java ... " + n + " -pln=<name> -cid=<CID> -mnc=<MNC> -mcc=<MCC> -lac=<LAC>");
        Print.sysPrintln("");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("   -pln=<name>    PrivateLabel name/host");
        Print.sysPrintln("   -cid=<CID>     Cell Tower ID");
        Print.sysPrintln("   -mcc=<id>      Mobile Country Code");
        Print.sysPrintln("   -mnc=<id>      Mobile Network Code");
        Print.sysPrintln("   -lac=<id>      Location Area Code");
        Print.sysPrintln("");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        if (RTConfig.hasProperty(ARG_HELP)) {
            usage();
        }

        /* cell-tower info */
        int cid = RTConfig.getInt(ARG_CID,565110);
        int mcc = RTConfig.getInt(ARG_MCC,   240);
        int mnc = RTConfig.getInt(ARG_MNC,     8);
        int lac = RTConfig.getInt(ARG_LAC,   318);
        CellTower ct = new CellTower();
        ct.setCellTowerID(      cid);
        ct.setMobileCountryCode(mcc);
        ct.setMobileNetworkCode(mnc);
        ct.setLocationAreaCode( lac);

        /* get PrivateLabel */
        BasicPrivateLabel privLabel = null;
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        if (!StringTools.isBlank(accountID)) {
            Account acct = null;
            try {
                acct = Account.getAccount(accountID); // may throw DBException
                if (acct == null) {
                    Print.sysPrintln("ERROR: Account-ID does not exist: " + accountID);
                    usage();
                }
                privLabel = acct.getPrivateLabel();
            } catch (DBException dbe) {
                Print.logException("Error loading Account: " + accountID, dbe);
                //dbe.printException();
                System.exit(99);
            }
        } else {
            String pln = RTConfig.getString(ARG_PLN,"default");
            if (StringTools.isBlank(pln)) {
                Print.sysPrintln("ERROR: Must specify '-account=<Account>'");
                usage();
            } else {
                privLabel = BasicPrivateLabelLoader.getPrivateLabel(pln);
                if (privLabel == null) {
                    Print.sysPrintln("ERROR: PrivateLabel name not found: %s", pln);
                    usage();
                }
            }
        }

        /* get mobile location provider */
        MobileLocationProvider mlp = privLabel.getMobileLocationProvider();
        if (mlp == null) {
            Print.sysPrintln("ERROR: No MobileLocationProvider for PrivateLabel: %s", privLabel.getName());
            System.exit(99);
        } else
        if (!mlp.isEnabled()) {
            Print.sysPrintln("WARNING: MobileLocationProvider disabled: " + mlp.getName());
            System.exit(0);
        }

        /* get location */
        MobileLocation ml = mlp.getMobileLocation(ct, null/*CellTower[]*/);
        Print.logInfo("Mobile Location: " + ml);


    }

}
