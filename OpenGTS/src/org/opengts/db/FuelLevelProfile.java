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
// Description:
//  Fuel level profile
// ----------------------------------------------------------------------------
// Change History:
//  2014/06/29  Martin D. Flynn
//     -Initial release (extracted from DCServerConfig.java)
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;

import org.opengts.util.*;

public class FuelLevelProfile
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Linear profile */
    public  static String FLP_LINEAR_ID = "LINEAR";
    private static ProfileItem LINEAR[] = {
        // LINEAR:0,0|100,100
        new ProfileItem(0.000,0.0000000),
        new ProfileItem(1.000,1.0000000)
    };
    private static FuelLevelProfile FLP_LINEAR = new FuelLevelProfile(
        FLP_LINEAR_ID, 1.0, LINEAR, 
        I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear","Linear profile"));

    /* Cylinder profile */
    // double A = ((Math.PI/2.0) - Math.asin(1.0-(2.0*E)) - (1.0-(2.0*E)) * Math.sqrt((2.0*E)*(2.0-(2.0*E)))) / Math.PI;
    public  static String FLP_CYLINDER_ID = "CYLINDER";
    private static ProfileItem CYLINDER[] = {
        // CYLINDER:0.0,0.0|0.125,0.072|0.250,0.196|0.375,0.343|0.500,0.500|0.625,0.657|0.750,0.804|0.875,0.928|1.0,1.0
        new ProfileItem(0.000,0.0000000),
        new ProfileItem(0.125,0.0721468),
        new ProfileItem(0.250,0.1955011),
        new ProfileItem(0.375,0.3425188),
        new ProfileItem(0.500,0.5000000),
        new ProfileItem(0.625,0.6574812),
        new ProfileItem(0.750,0.8044989),
        new ProfileItem(0.875,0.9278532),
        new ProfileItem(1.000,1.0000000)
    };
    private static FuelLevelProfile FLP_CYLINDER = new FuelLevelProfile(
        FLP_CYLINDER_ID, 1.0, CYLINDER, 
        I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder","Horizontal Cylinder"));

    /* Generic profile */
    public  static String FLP_PROFILE_ID = "PROFILE";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final HashMap<String,FuelLevelProfile> FuelLevelProfileMap = new HashMap<String,FuelLevelProfile>();

    /**
    *** Returns a list of FuelLevelProfile IDs
    **/
    public static OrderedMap<String,String> GetFuelLevelProfiles(Locale locale)
    {
        OrderedMap<String,String> profList = new OrderedMap<String,String>();
        profList.put(FLP_LINEAR.getID()  ,FLP_LINEAR.getDescription(locale));
        profList.put(FLP_CYLINDER.getID(),FLP_CYLINDER.getDescription(locale));
        for (String profID : FuelLevelProfileMap.keySet()) {
            FuelLevelProfile flp = FuelLevelProfileMap.get(profID);
            if (!profList.containsKey(profID)) {
                profList.put(profID, flp.getDescription(locale));
            }
        }
        return profList;
    }

    /**
    *** Add a FuelLevelProfile 
    **/
    public static FuelLevelProfile AddFuelLevelProfile(String ID, String profile, I18N.Text desc)
    {
        FuelLevelProfile flp = new FuelLevelProfile(profile, desc);
        if (flp.isValid()) {
            if (!StringTools.isBlank(ID)) {
                // -- specified ID overrides any ID found in "profile"
                flp.setID(ID);
            } else
            if (StringTools.isBlank(flp.getID())) {
                // -- "profile" did not contain an ID, assign an ID
                String pid = "profile_" + ListTools.size(FuelLevelProfileMap);
                flp.setID(pid);
            }
            FuelLevelProfileMap.put(flp.getID(), flp);
            return flp;
        } else {
            return null;
        }
    }

    /**
    *** Gets the FuelLevelProfile for the specified name.
    *** Does not return null.  If the profile name is not found, the LINEAR profile will be returned.
    **/
    public static FuelLevelProfile GetFuelLevelProfile(String profID)
    {

        /* default LINEAR */
        if (StringTools.isBlank(profID)) {
            return FLP_LINEAR;
        }

        /* get saved profile? */
        FuelLevelProfile flp = FuelLevelProfileMap.get(profID);
        if (flp != null) {
            return flp;
        }

        /* LINEAR? */
        if (profID.equalsIgnoreCase(FLP_LINEAR_ID)) {
            return FLP_LINEAR;
        }

        /* CYLINDER */
        if (profID.equalsIgnoreCase(FLP_CYLINDER_ID)) {
            return FLP_CYLINDER;
        }

        /* default to LINEAR if not found */
        return FLP_LINEAR;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** FuelLevelProfile class<br>
    **/
    private static class ProfileItem
    {
        // 0,0|1,1
        private double  evtLevel = 0.0;
        private double  actLevel = 0.0;
        private boolean isValid  = true;
        public ProfileItem(String lvl) {
            // "0.25,0.25" or "25,25
            if (!StringTools.isBlank(lvl)) {
                String L[] = StringTools.split(lvl,',');
                if (ListTools.size(L) >= 2) {
                    // -- event level
                    double evLvl = StringTools.parseDouble(L[0],-1.0);
                    this.evtLevel = (L[0].indexOf(".") >= 0)? evLvl : (evLvl / 100.0);
                    // -- actual level
                    double acLvl = StringTools.parseDouble(L[1],-1.0);
                    this.actLevel = (L[1].indexOf(".") >= 0)? acLvl : (acLvl / 100.0);
                    // -- invalid level?
                    if ((this.evtLevel < 0.0) || (this.evtLevel > 1.0) ||
                        (this.actLevel < 0.0) || (this.actLevel > 1.0)   ) {
                        Print.logError("Invalid FuelLevelProfile value: " + lvl);
                        this.evtLevel = 0.0;
                        this.actLevel = 0.0;
                        this.isValid  = false;
                    } else {
                        this.isValid  = true;
                    }
                }
            }
        }
        public ProfileItem(double evLvl, double acLvl) {
            this.evtLevel = evLvl;
            this.actLevel = acLvl;
        }
        public boolean isValid() {
            return this.isValid;
        }
        public double getEventLevel() {
            return this.evtLevel;
        }
        public double getActualLevel() {
            return this.actLevel;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String      profID     = "";
    private I18N.Text   profDesc   = null;
    private double      profScale  = 1.0;
    private ProfileItem profItem[] = null;

    /**
    *** Constructor
    **/
    public FuelLevelProfile(String profile, I18N.Text desc)
    {
        String P[]   = StringTools.split(StringTools.trim(profile).toUpperCase(),':');
        String ID    = (P.length > 0)? P[0] : "";
        double SCALE = (P.length > 1)? StringTools.parseDouble(P[1],1.0) : 1.0;
        String PROF  = (P.length > 2)? P[2] : "";

        /* save ID/Scale/description */
        this.setID(!StringTools.isBlank(ID)? ID : FLP_LINEAR_ID);
        this.setScale((SCALE > 0.0)? SCALE : 1.0);
        this.setDescription(desc);

        /* profile */
        if (!StringTools.isBlank(PROF)) {
            String rp[] = StringTools.split(PROF,'|');
            Vector<ProfileItem> flp = new Vector<ProfileItem>();
            for (int i = 0; i < rp.length; i++) {
                ProfileItem pi = new ProfileItem(rp[i]);
                if (pi.isValid()) {
                    flp.add(pi);
                }
            }
            this.setProfile(flp.toArray(new ProfileItem[flp.size()]));
        } else
        if (StringTools.isBlank(ID) || ID.equalsIgnoreCase(FLP_LINEAR_ID)) {
            // -- LINEAR:1.0
            this.setProfile(LINEAR);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear","Linear profile"));
        } else
        if (ID.equalsIgnoreCase(FLP_CYLINDER_ID)) {
            // -- CYLINDER:1.0
            this.setProfile(CYLINDER);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder","Horizontal Cylinder"));
        } else
        if (ID.equalsIgnoreCase(FLP_PROFILE_ID)) {
            // -- PROFILE:1.0:0.00,0.00|0.25,0.50|1.00,1.00
            // -  Should NOT be here! (profile should have been set above
            this.setProfile(LINEAR);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear","Linear profile"));
        } else {
            // -- unrecognized
            // -  Should NOT be here! (profile should have been set above
            this.setProfile(LINEAR);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.unknown","Unrecognized profile"));
        }

    }

    /**
    *** Constructor
    **/
    private FuelLevelProfile(String ID, double scale, ProfileItem prof[], I18N.Text desc)
    {
        this.setID(ID);
        this.setScale(scale);
        this.setProfile(prof);
        this.setDescription(desc);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ID of this FuelLevelProfile
    **/
    public void setID(String ID)
    {
        this.profID = StringTools.trim(ID);
    }

    /**
    *** Gets the ID of this FuelLevelProfile
    **/
    public String getID()
    {
        return this.profID;
    }

    /**
    *** Gets the ID of this FuelLevelProfile
    **/
    public String getName()
    {
        return this.getID();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Description of this FuelLevelProfile
    **/
    public void setDescription(I18N.Text desc)
    {
        this.profDesc = desc;
    }

    /**
    *** Gets the Description of this FuelLevelProfile
    **/
    public String getDescription(Locale locale)
    {
        if (this.profDesc != null) {
            return this.profDesc.toString(locale);
        } else {
            return this.getName();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the profile scale
    **/
    private void setScale(double scale)
    {
        this.profScale = (scale <= 0.0)? 1.0 : scale;
    }
    
    /**
    *** Gets the profile scale
    **/
    public double getScale()
    {
        return (this.profScale <= 0.0)? 1.0 : this.profScale;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Profile list 
    **/
    private void setProfile(ProfileItem flp[])
    {
        this.profItem = flp;
    }

    /**
    *** Gets the Profile list 
    **/
    private ProfileItem[] getProfile()
    {
        return this.profItem;
    }

    /**
    *** Returns true if this FuelLevelProfile is valid
    **/
    public boolean isValid()
    {
        if (ListTools.size(this.getProfile()) < 2) {
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the actual fuel level from the event specified fuel level
    *** @param fuelLevel  The event specified fuel level
    *** @return The actual fuel level
    **/
    public double getActualFuelLevel(double fuelLevelVal)
    {
        final double FL;
        double FV = fuelLevelVal / this.getScale();

        /* no profile? */
        ProfileItem flp[] = this.getProfile();
        if (ListTools.size(flp) < 2) {
            FL = FV;
            return FL;
        }

        /* get high/low values */
        ProfileItem hi = null;
        ProfileItem lo = null;
        for (int i = 0; i < flp.length; i++) {
            double flpv = flp[i].getEventLevel();
            if (flpv == FV) {
                // -- exact match
                hi = flp[i];
                lo = flp[i];
                break;
            } else
            if (flpv >= FV) {
                // -- found range
                hi = flp[i];
                lo = (i > 0)? flp[i - 1] : null;
                break;
            } else {
                // -- FV < flp[i].getEventLevel()
                lo = flp[i];
                // continue;
            }
        }

        /* calculate linear interpolation between points */
        if ((hi != null) && (lo != null)) {
            // -- interprolate
            double evHi = hi.getEventLevel();
            double evLo = lo.getEventLevel();
            if (evHi != evLo) {
                // -- hi/lo differ, interprolate
                double evD  = (FV - evLo) / (evHi - evLo);
                double acHi = hi.getActualLevel();
                double acLo = lo.getActualLevel();
                FL = acLo + (evD * (acHi - acLo));
            } else {
                // -- hi/lo are the same
                double acLo = lo.getActualLevel();
                FL = acLo;
            }
        } else
        if (lo != null) {
            // -- hi is null
            FL = lo.getActualLevel();
        } else 
        if (hi != null) {
            // -- lo is null
            FL = hi.getActualLevel();
        } else {
            // -- hi/lo are null
            FL = (FV < 0.0)? 0.0 : (FV > 1.0)? 1.0 : FV;
        }
        return FL;

    }

    // ------------------------------------------------------------------------

}
