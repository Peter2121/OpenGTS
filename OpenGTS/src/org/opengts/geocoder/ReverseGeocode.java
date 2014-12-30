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
//  2007/06/13  Martin D. Flynn
//     -Initial release
//  2007/11/28  Martin D. Flynn
//     -Added Street#/City/PostalCode getter/setter methods.
//  2008/03/28  Martin D. Flynn
//     -Added CountryCode methods.
//  2008/05/14  Martin D. Flynn
//     -Added StateProvince methods
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import org.opengts.util.*;

public class ReverseGeocode
{
 
    // ------------------------------------------------------------------------

    public static final String COUNTRY_US               = "US";
    public static final String SUBDIVISION_SEPARATOR    = "/";
    public static final String COUNTRY_US_              = COUNTRY_US + SUBDIVISION_SEPARATOR;

    // ------------------------------------------------------------------------

    public  static final String TAG_Provider[]          = { "RG", "Provider"      };
    public  static final String TAG_Latitude[]          = { "LA", "Latitude"      };
    public  static final String TAG_Longitude[]         = { "LO", "Longitude"     };

    public  static final String TAG_FullAddress[]       = { "FA", "FullAddress"   };
    public  static final String TAG_StreetAddress[]     = { "SA", "StreetAddress" };
    public  static final String TAG_City[]              = { "CI", "City"          };
    public  static final String TAG_StateProvince[]     = { "SP", "StateProvince" };
    public  static final String TAG_PostalCode[]        = { "PC", "PostalCode"    };
    public  static final String TAG_CountryCode[]       = { "CC", "CountryCode"   };
    public  static final String TAG_Subdivision[]       = { "SD", "Subdivision"   };
    public  static final String TAG_SpeedLimit[]        = { "SL", "SpeedLimit"    };
    public  static final String TAG_TollRoad[]          = { "TR", "TollRoad"      };

    // ------------------------------------------------------------------------

    public  static final String REQ_RGLength            = "RGLength:";
    public  static final String REQ_RGSignature         = "RGSignature:";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String  fullAddress     = null; // "FA", "FullAddress"
    private String  streetAddr      = null; // "SA", "StreetAddress"
    private String  city            = null; // "CI", "City"
    private String  stateProvince   = null; // "SP", "StateProvince"
    private String  postalCode      = null; // "PC", "PostalCode"
    private String  countryCode     = null; // "CC", "CountryCode"
    private String  subdivision     = null; // "SD", "Subdivision"
    private double  speedLimitKPH   = 0.0;  // "SL", "SpeedLimit"
    private int     isTollRoad      = -1;   // "TR", "TollRoad"

    /**
    *** Default constructor
    **/
    public ReverseGeocode()
    {
        super();
    }

    /**
    *** JSON constructor
    **/
    public ReverseGeocode(JSON._Object jsonObj)
    {
        this();
        if (jsonObj != null) {
            this.setFullAddress(  jsonObj.getStringForName(TAG_FullAddress  ,null));
            this.setStreetAddress(jsonObj.getStringForName(TAG_StreetAddress,null));
            this.setCity(         jsonObj.getStringForName(TAG_City         ,null));
            this.setStateProvince(jsonObj.getStringForName(TAG_StateProvince,null));
            this.setPostalCode(   jsonObj.getStringForName(TAG_PostalCode   ,null));
            this.setCountryCode(  jsonObj.getStringForName(TAG_CountryCode  ,null));
            this.setSubdivision(  jsonObj.getStringForName(TAG_Subdivision  ,null));
            this.setSpeedLimitKPH(jsonObj.getDoubleForName(TAG_SpeedLimit   , 0.0));
            this.setIsTollRoad(   jsonObj.getIntForName(   TAG_TollRoad     ,  -1));
        }
    }

    /**
    *** JSON constructor
    **/
    public ReverseGeocode(JSON json)
    {
        this((json != null)? json.getObject() : null);
    }

    // ------------------------------------------------------------------------
    // Full address

    /**
    *** Sets the full address
    **/
    public void setFullAddress(String address)
    {
        this.fullAddress = (address != null)? address.trim() : null;
    }

    /**
    *** Gets the full address
    **/
    public String getFullAddress()
    {
        return this.fullAddress;
    }

    /**
    *** Returns true if the full address is defined
    **/
    public boolean hasFullAddress()
    {
        return !StringTools.isBlank(this.fullAddress);
    }

    // ------------------------------------------------------------------------
    // Street address

    /**
    *** Sets the street address
    **/
    public void setStreetAddress(String address)
    {
        this.streetAddr = (address != null)? address.trim() : null;
    }

    /**
    *** Gets the street address
    **/
    public String getStreetAddress()
    {
        return this.streetAddr;
    }

    /**
    *** Returns true if the street address is defined
    **/
    public boolean hasStreetAddress()
    {
        return !StringTools.isBlank(this.streetAddr);
    }

    // ------------------------------------------------------------------------
    // City

    /**
    *** Sets the city
    **/
    public void setCity(String city)
    {
        this.city = (city != null)? city.trim() : null;
    }

    /**
    *** Gets the city
    **/
    public String getCity()
    {
        return this.city;
    }

    /**
    *** Returns true if the city is defined
    **/
    public boolean hasCity()
    {
        return !StringTools.isBlank(this.city);
    }

    // ------------------------------------------------------------------------
    // State/Province

    /**
    *** Sets the state/province
    **/
    public void setStateProvince(String state)
    {
        this.stateProvince = (state != null)? state.trim() : null;
    }

    /**
    *** Gets the state/province
    **/
    public String getStateProvince()
    {
        return this.stateProvince;
    }

    /**
    *** Returns true if the state/province is defined
    **/
    public boolean hasStateProvince()
    {
        return !StringTools.isBlank(this.stateProvince);
    }

    // ------------------------------------------------------------------------
    // Postal code

    /**
    *** Sets the postal code
    **/
    public void setPostalCode(String zip)
    {
        this.postalCode = (zip != null)? zip.trim() : null;
    }

    /**
    *** Gets the postal code
    **/
    public String getPostalCode()
    {
        return this.postalCode;
    }

    /**
    *** Returns true if the postal code is defined
    **/
    public boolean hasPostalCode()
    {
        return !StringTools.isBlank(this.postalCode);
    }

    // ------------------------------------------------------------------------
    // Country

    /**
    *** Sets the country code
    **/
    public void setCountryCode(String countryCode)
    {
        this.countryCode = (countryCode != null)? countryCode.trim() : null;
    }

    /**
    *** Gets the country code
    **/
    public String getCountryCode()
    {
        return this.hasCountryCode()? this.countryCode : null;
    }

    /**
    *** Returns true if the country is defined
    **/
    public boolean hasCountryCode()
    {
        return !StringTools.isBlank(this.countryCode);
    }

    // ------------------------------------------------------------------------
    // Subdivision

    /**
    *** Sets the country/state subdivision
    **/
    public void setSubdivision(String subdiv)
    {
        this.subdivision = (subdiv != null)? subdiv.trim() : null;
    }

    /**
    *** Gets the country/state subdivision
    **/
    public String getSubdivision()
    {
        return this.hasSubdivision()? this.subdivision : null;
    }

    /**
    *** Returns true if the country/state subdivision is defined
    **/
    public boolean hasSubdivision()
    {
        return !StringTools.isBlank(this.subdivision);
    }

    // ------------------------------------------------------------------------
    // Speed Limit

    /**
    *** Sets the speed limit at the reverse-geocoded location
    **/
    public void setSpeedLimitKPH(double limitKPH)
    {
        //Print.logInfo("Set Speed Limit %f", limitKPH);
        this.speedLimitKPH = limitKPH;
    }

    /**
    *** Gets the speed limit at the reverse-geocoded location
    **/
    public double getSpeedLimitKPH()
    {
        return this.speedLimitKPH;
    }

    /**
    *** Returns true if the speed limit is defined
    **/
    public boolean hasSpeedLimitKPH()
    {
        return (this.speedLimitKPH > 0.0);
    }

    // ------------------------------------------------------------------------
    // Toll-Road

    /**
    *** Sets the toll-road state
    **/
    public void setIsTollRoad(int tollRoadState)
    {
        switch (tollRoadState) {
            case 0 : this.isTollRoad =  0; break;
            case 1 : this.isTollRoad =  1; break;
            default: this.isTollRoad = -1; break;
        }
    }

    /**
    *** Sets the toll-road state
    **/
    public void setIsTollRoad(boolean tollRoad)
    {
        this.isTollRoad = tollRoad? 1 : 0;
    }

    /**
    *** Gets the toll-road state
    **/
    public boolean getIsTollRoad()
    {
        return (this.isTollRoad == 1);
    }

    /**
    *** Returns true if the toll-road state is defined
    **/
    public boolean hasIsTollRoad()
    {
        return (this.isTollRoad >= 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    **/
    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        if (this.hasFullAddress()) {
            sb.append(this.getFullAddress());
        }
        if (this.hasSubdivision()) {
            if (sb.length() > 0) { 
                sb.append(" ["); 
                sb.append(this.getSubdivision());
                sb.append("]"); 
            } else {
                sb.append(this.getSubdivision());
            }
        }
        if (this.hasSpeedLimitKPH()) {
            double limitKPH = this.getSpeedLimitKPH();
            if (limitKPH >= 900.0) {
                sb.append(" (unlimited speed)");
            } else {
                sb.append(" (limit ");
                sb.append(StringTools.format(limitKPH,"0.0"));
                sb.append(" km/h, ");
                sb.append(StringTools.format(limitKPH*GeoPoint.MILES_PER_KILOMETER,"0.0"));
                sb.append(" mph)");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a JSON String containing this ReverseGeocode information
    **/
    public JSON toJSON()
    {
        return this.toJSON(false);
    }

    /**
    *** Returns a JSON String containing this ReverseGeocode information
    **/
    public JSON toJSON(boolean longFmt)
    {
        int ndx = longFmt? 1 : 0;
        JSON._Object jsonObj = new JSON._Object();
        // -- FullAddress
        if (this.hasFullAddress()) {
            jsonObj.addKeyValue(TAG_FullAddress[ndx]  , this.getFullAddress());
        }
        // -- StreetAddress
        if (this.hasStreetAddress()) {
            jsonObj.addKeyValue(TAG_StreetAddress[ndx], this.getStreetAddress());
        }
        // -- City
        if (this.hasCity()) {
            jsonObj.addKeyValue(TAG_City[ndx]         , this.getCity());
        }
        // -- StateProvince
        if (this.hasStateProvince()) {
            jsonObj.addKeyValue(TAG_StateProvince[ndx], this.getStateProvince());
        }
        // -- PostalCode
        if (this.hasPostalCode()) {
            jsonObj.addKeyValue(TAG_PostalCode[ndx]   , this.getPostalCode());
        }
        // -- CountryCode
        if (this.hasCountryCode()) {
            jsonObj.addKeyValue(TAG_CountryCode[ndx]  , this.getCountryCode());
        }
        // -- Subdivision
        if (this.hasSubdivision()) {
            jsonObj.addKeyValue(TAG_Subdivision[ndx]  , this.getSubdivision());
        }
        // -- SpeedLimit
        if (this.hasSpeedLimitKPH()) {
            jsonObj.addKeyValue(TAG_SpeedLimit[ndx]   , this.getSpeedLimitKPH());
        }
        // -- TollRoad
        if (this.hasIsTollRoad()) {
            jsonObj.addKeyValue(TAG_TollRoad[ndx]     , this.getIsTollRoad());
        }
        // -- return JSON
        return new JSON(jsonObj);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
