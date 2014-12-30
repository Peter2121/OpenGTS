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
// References:
//  - http://www.mapstraction.com/
//  - http://mapstraction.com/svn/source/
//  - http://www.koders.com/javascript/fid85B6D94DE9D67A1D5648D53ED9E2BC05FFA5CD42.aspx
// ----------------------------------------------------------------------------
// Required supporting JavaScript:
//  - http://mapstraction.com/svn/source/mapstraction-geocode.js
//  - http://mapstraction.com/svn/source/mapstraction.js
//  - http://www.koders.com/javascript/fid85B6D94DE9D67A1D5648D53ED9E2BC05FFA5CD42.aspx
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Anthony George, Peter Jonas, Martin D. Flynn
//     -Initial release (extracted from "org/opengts/war/maps/ms/Mapstraction.java")
//  2008/08/08  Martin D. Flynn
//     -Included support for OpenSpace.
//     -Added limited Geozone support (OpenLayers only)
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class Mapstraction
    extends JSMap
{

    // ------------------------------------------------------------------------

    /* initial testing performed */
    public  static final String PROVIDER_OPENLAYERS     = "openlayers";
    public  static final String PROVIDER_GOOGLEV3       = "googlev3";   // V3 only

    /* tested, but display issues remain */
    public  static final String PROVIDER_CLOUDMADE      = "cloudmade";
    public  static final String PROVIDER_MICROSOFT      = "microsoft";  // V6
    public  static final String PROVIDER_MICROSOFT7     = "microsoft7"; // V7
    public  static final String PROVIDER_MAPQUEST       = "mapquest";
    public  static final String PROVIDER_OPENMQ         = "openmq";
    public  static final String PROVIDER_ESRI           = "esri";       // ArcGIS
    public  static final String PROVIDER_OPENSPACE      = "openspace";
    public  static final String PROVIDER_LEAFLET        = "leaflet";

    /* not fully tested */
    public  static final String PROVIDER_OVI            = "ovi";

    /* no longer supported */
    public  static final String PROVIDER_GOOGLE         = "google";     // V2
    public  static final String PROVIDER_YAHOO          = "yahoo";
    public  static final String PROVIDER_MULTIMAP       = "multimap";
    public  static final String PROVIDER_MAP24          = "map24";
    public  static final String PROVIDER_OPENSTREETMAP  = "openstreetmap";
    public  static final String PROVIDER_FREEEARTH      = "freeearth";

    /* default provider */
    public  static final String DEFAULT_PROVIDER        = PROVIDER_OPENLAYERS; 

    // ------------------------------------------------------------------------

    private static final String PROP_version[]          = { "mapstractionVersion"  };
    public  static final String PROP_provider[]         = { "mapstractionProvider" };
    public  static final String PROP_LOCAL_JS[]         = { "mapstractionLocalJS"  };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private String  apiVersion      = null;
    private boolean didInitFeatures = false;

    /* Mapstraction instance */ 
    public Mapstraction(String name, String key) 
    {
        super(name, key); 
        this.addSupportedFeature(FEATURE_DETAIL_REPORT);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the API version
    *** @return The API version
    **/
    public String getApiVersion()
    {
        if (this.apiVersion == null) {
            // "maps/MapstractionV1.js"
            // "maps/MapstractionV2.js"
            String apiV = this.getProperties().getString(PROP_version,"").trim().toLowerCase();
            if (StringTools.isBlank(apiV)) {
                apiV = "1"; // assume default V1
            } else
            if (apiV.equals("2")) {
                apiV = "2";
            } else {
                apiV = "1"; // default to "1"
            }
            this.apiVersion = apiV;
        }
        return this.apiVersion;
    }

    /**
    *** Return true if using Mapstraction API v1
    **/
    public boolean isVersion1()
    {
        return this.getApiVersion().equals("1");
    }

    /**
    *** Return true if using Google API v3
    **/
    public boolean isVersion2()
    {
        return this.getApiVersion().equals("2");
    }

    // ------------------------------------------------------------------------

    public boolean isFeatureSupported(long feature)
    {
        if (!this.didInitFeatures) {
            // lazy feature support initialization
            String mapProvider = this.getProperties().getString(PROP_provider, DEFAULT_PROVIDER); 
            boolean isOpenLayers = mapProvider.equals(PROVIDER_OPENLAYERS);
            if (isOpenLayers) {
                this.addSupportedFeature(FEATURE_LATLON_DISPLAY);
                this.addSupportedFeature(FEATURE_DISTANCE_RULER);
            }
            this.didInitFeatures = true;
        }
        return super.isFeatureSupported(feature);
    }

    // ------------------------------------------------------------------------

    /* write mapping support JS to stream */ 
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        super.writeJSVariables(out, reqState);
        String mapProvider = this.getProperties().getString(PROP_provider, DEFAULT_PROVIDER); 
        out.write("// Mapstraction custom vars ("+mapProvider+")\n");
        // custom icons currently only work on OpenLayers
        JavaScriptTools.writeJSVar(out, "SHOW_CUSTOM_ICON", mapProvider.equals(PROVIDER_OPENLAYERS));
        // provider specific vars
        if (mapProvider.equals(PROVIDER_CLOUDMADE)) {
            String key = this._getAuthKey(mapProvider,"");
            JavaScriptTools.writeJSVar(out, "cloudmade_key", key);
        } else 
        if (mapProvider.equals(PROVIDER_MICROSOFT7)) {
            String key = this._getAuthKey(mapProvider,"AlneHdcKOFDot4FjwyuLH8ZSUIz5rv_X22vULKa7H5ia0JnsxiykdO8y"); 
            JavaScriptTools.writeJSVar(out, "microsoft_key", key);
        }
    }

    // ------------------------------------------------------------------------

    /* write css to stream */
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        super.writeStyle(out, reqState);
        String mapProvider = this.getProperties().getString(PROP_provider, DEFAULT_PROVIDER); 
        if (mapProvider.equals(PROVIDER_ESRI)) {
            // - 2013/08/23: ArcGIS (does not appear to be fully supported by Mapstraction)
            WebPageAdaptor.writeCssLink(out, reqState, "http://serverapi.arcgisonline.com/jsapi/arcgis/3.2/js/esri/css/esri.css", null);
            WebPageAdaptor.writeCssLink(out, reqState, "http://serverapi.arcgisonline.com/jsapi/arcgis/3.2/js/dojo/dijit/themes/claro/claro.css", null);
        } else
        if (mapProvider.equals(PROVIDER_LEAFLET)) {
            // - 2013/08/23: 
            WebPageAdaptor.writeCssLink(out, reqState, "http://leaflet.cloudmade.com/dist/leaflet.css", null);
        } else
        {
            // ignore
        }
    }

    // ------------------------------------------------------------------------

    protected String _getAuthKey(String provider, String dftKey)
    {
        String mapKey = this.getAuthorization(); 
        if (StringTools.isBlank(mapKey)) { 
            Print.logError("No '%s' key!", provider); 
            mapKey = (dftKey != null)? dftKey : "";  
        }
        return mapKey;
    }

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState) 
        throws IOException 
    {
        java.util.List<String> jsList = new Vector<String>();

        /* main mapping support javascript */
        jsList.add(JavaScriptTools.qualifyJSFileRef("maps/jsmap.js"));

        /* specific Mapstraction provider */
        String mapProvider = this.getProperties().getString(PROP_provider, DEFAULT_PROVIDER); 
        // ------
        // tested
        if (mapProvider.equals(PROVIDER_OPENLAYERS)) {
            // - 2013/09/23
            jsList.add("http://dev.openlayers.org/releases/OpenLayers-2.9.1/OpenLayers.js");
            // - old version below
            //jsList.add("http://openlayers.org/api/OpenLayers.js");
        } else
        if (mapProvider.equals(PROVIDER_GOOGLEV3)) {
            // - 2013/09/23
            jsList.add("http://maps.google.com/maps/api/js?sensor=false");
            // - TODO: update for "gme-KEY"
            //String key = this._getAuthKey(mapProvider,"INVALID_KEY"); 
            //jsList.add("http://maps.google.com/maps?file=api&v=2&key="+key);
            // -
            jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/labeledmarker.js"));
        } else
        // ------
        // not yet tested
        if (mapProvider.equals(PROVIDER_OPENSPACE)) {
            // - 2013/09/23: 
            String key = this._getAuthKey(mapProvider,""); 
            jsList.add("http://openspace.ordnancesurvey.co.uk/osmapapi/openspace.js?key="+key);
        } else 
        // ------
        // tested, issues remain
        if (mapProvider.equals(PROVIDER_OPENMQ)) {
            // - 2013/09/23: Mapquest Open
            // TODO: unable to zoom
            jsList.add("http://open.mapquestapi.com/sdk/js/v7.0.s/mqa.toolkit.js");
        } else
        if (mapProvider.equals(PROVIDER_CLOUDMADE)) {
            // - 2013/09/23: key is required for map display
            jsList.add("http://tile.cloudmade.com/wml/latest/web-maps-lite.js");
        } else
        if (mapProvider.equals(PROVIDER_MICROSOFT)) {
            // TODO: map does not display properly within the "DIV"
            // - 2013/09/23: Bing Maps v6
            jsList.add("http://ecn.dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.3&mkt=en-us");
            // - old version below
            //jsList.add("http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=5");
        } else
        if (mapProvider.equals(PROVIDER_MICROSOFT7)) {
            // TODO: map does not display properly within the "DIV"
            // TODO: key is required for map display
            // - 2013/09/23: Bing Maps v7
            jsList.add("http://ecn.dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=7.0");
        } else
        if (mapProvider.equals(PROVIDER_MAPQUEST)) {
            // - 2013/08/23: does not appear to be fully supported by Mapstraction
            String key = this._getAuthKey(mapProvider,""); 
            jsList.add("http://www.mapquestapi.com/sdk/js/v7.0.s/mqa.toolkit.js?key="+key+"");
            // - old version below
            //String key = this._getAuthKey(mapProvider,"mjtd%7Clu6t210anh%2Crn%3Do5-labwu"); 
            //jsList.add("http://btilelog.access.mapquest.com/tilelog/transaction?transaction=script&key="+key+"&ipr=true&itk=true&v=5.1");
        } else
        if (mapProvider.equals(PROVIDER_ESRI)) {
            // - 2013/08/23: ArcGIS (does not appear to be fully supported by Mapstraction)
            jsList.add("http://serverapi.arcgisonline.com/jsapi/arcgis/?v=3.2");
        } else 
        if (mapProvider.equals(PROVIDER_LEAFLET)) {
            // - 2013/08/23: zoom not displayed
            jsList.add("http://leaflet.cloudmade.com/dist/leaflet.js");
        } else 
        if (mapProvider.equals(PROVIDER_OVI)) {
            // - 2013/08/23: "ovi.mapsapi.map is undefined"
            jsList.add("http://api.maps.ovi.com/jsl.js");
        } else
        // ------
        // not fully supported
        //
        // ------
        // no longer supported
        if (mapProvider.equals(PROVIDER_FREEEARTH)) {
            // - 2013/08/23: no longer supported
            Print.logError("Maptraction map provider no longer supported: %s", mapProvider); 
            // - old version below
            //jsList.add("http://freeearth.poly9.com/api.js");
        } else
        if (mapProvider.equals(PROVIDER_MAP24)) {
            // - 2013/08/23: no longer supported
            Print.logError("Maptraction map provider no longer supported: %s", mapProvider); 
            // - old version below
            //String key = this._getAuthKey(mapProvider,"FJXe1b9e7b896f8cf70534ee0c69ecbfX16");
            //jsList.add("http://api.maptp.map24.com/ajax?appkey="+key);
        } else
        if (mapProvider.equals(PROVIDER_MULTIMAP)) {
            // - 2013/08/23: no longer supported
            Print.logError("Maptraction map provider no longer supported: %s", mapProvider); 
            // - old version below
            //String key = this._getAuthKey(mapProvider,"");
            //jsList.add("http://developer.multimap.com/API/maps/1.2/"+key);
        } else
        if (mapProvider.equals(PROVIDER_YAHOO)) {
            // - 2013/08/23: no longer supported
            Print.logError("Maptraction map provider no longer supported: %s", mapProvider); 
            // - old version below
            //String key = this._getAuthKey(mapProvider,"MapstractionDemo"); 
            //jsList.add("http://api.maps.yahoo.com/ajaxymap?v=3.0&appid="+key);
        } else
        // ------
        // unrecognized
        {
            Print.logError("Unrecognized map provider specified: %s", mapProvider); 
        }

        /* include 'mapstraction.js' */
        if (this.getProperties().getBoolean(PROP_LOCAL_JS,false)) {
            // - 2013/09/30
            jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/mxn/mxn.js?("+mapProvider+")"));
            // - old version below
            //jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/mapstraction-geocode.js"));
            //jsList.add(JavaScriptTools.qualifyJSFileRef("mapstraction/mapstraction.js"));
        } else {
            // - 2013/09/30
            jsList.add("http://mapstraction.com/mxn/build/latest/mxn.js?("+mapProvider+")");
            // - old version below
            //jsList.add("http://mapstraction.com/svn/source/mapstraction-geocode.js");
            //jsList.add("http://mapstraction.com/svn/source/mapstraction.js");
        }

        /* include OpenGTS mapping support for Mapstraction */
        // - 2013/09/30
        jsList.add(JavaScriptTools.qualifyJSFileRef("maps/MapstractionMXN2.js"));
        // - older version below
        //if (this.isVersion2()) {
        //    jsList.add(JavaScriptTools.qualifyJSFileRef("maps/MapstractionV2.js"));
        //} else {
        //    jsList.add(JavaScriptTools.qualifyJSFileRef("maps/MapstractionV1.js"));
        //}

        /* write out script html */
        super.writeJSIncludes(out, reqState, jsList.toArray(new String[jsList.size()]));

    }

    // ------------------------------------------------------------------------
    
    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        I18N i18n = I18N.getI18N(Mapstraction.class, loc);
        return new String[] {
            i18n.getString("Mapstraction.geozoneInstructions", "")
        };
    }

    // ------------------------------------------------------------------------

}
