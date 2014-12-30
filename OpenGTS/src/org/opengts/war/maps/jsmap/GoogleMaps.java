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
// Reverse-Geocoding possibilities:
//  - http://blog.programmableweb.com/2008/10/24/google-maps-api-gets-reverse-geocoding/
//  - http://gmaps-samples.googlecode.com/svn/trunk/geocoder/reverse.html
//  - http://groups.google.com/group/Google-Maps-API/web/resources-non-google-geocoders
//  - http://www.freereversegeo.com/
//  - http://mapperz.blogspot.com/2007/08/exclusive-reverse-geocoding-using.html
//  - http://nicogoeminne.googlepages.com/reversegeocode.html
// Dual Maps:
//  - http://www.mapchannels.com/dualmaps.aspx
// Register for Google Map keys:
//  - http://www.google.com/apis/maps/signup.html
// Usage examples:
//  - http://mapstraction.com/demo-filters.php
//  - http://econym.org.uk/gmap/
// Scale/Zoom/Meters-per-pixel
//  - http://slappy.cs.uiuc.edu/fall06/cs492/Group2/example.html
// Misc (many useful examples)
//  - http://www.bdcc.co.uk/Gmaps/BdccGmapBits.htm
//  - http://groups.google.com/group/Google-Maps-API/web/examples-tutorials-gpolygon-gpolyline
//  - http://code.nosvamosdetapas.com/googlemaps/test5.html
//  - http://maps.huge.info/examples.htm
//      - http://maps.huge.info/dragcircle2.htm
//      - http://maps.huge.info/dragpoly.htm
//  - http://wolfpil.googlepages.com/polygon.html 
//  - http://maps.forum.nu/gm_plot.html
//  - http://wolfpil.googlepages.com/switch-polies.html
// Google Pushpins:
//  - http://labs.google.com/ridefinder/images/mm_20_${color}.png   (no longer valid)
//  - http://labs.google.com/ridefinder/images/mm_20_shadow.png     (no longer valid)
//  - http://gmaps-utility-library.googlecode.com/svn/trunk/mapiconmaker/1.1/docs/examples.html
//  - http://www.powerhut.co.uk/googlemaps/custom_markers.php
//  - http://groups.google.com/group/google-maps-api/web/examples-tutorials-custom-icons-for-markers
//  - http://thydzik.com/dynamic-google-maps-markersicons-with-php/
// Google Search Bar:
//  - http://code.google.com/apis/maps/documentation/services.html#LocalSearch
// V3 Examples:
//  - http://code.google.com/apis/maps/documentation/javascript/maptypes.html
//  - http://code.google.com/apis/maps/documentation/javascript/examples/index.html
//  - http://www.wolfpil.de/
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added Geozone support
//  2009/08/07  Martin D. Flynn
//     -Added "google.mapcontrol" and "google.sensor" properties.
//  2009/12/16  Martin D. Flynn
//     -Added support for client-id (ie. "&client=gme-...")
//  2011/06/16  Martin D. Flynn
//     -Initial support for Google API v3 (requires supporting JS as well)
//  2012/04/03  Martin D. Flynn
//     -Added support for Google map layer integration
//  2012/12/24  Martin D. Flynn
//     -Added support for Google weather/cloud layers (V3 only)
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.db.DBConfig;
import org.opengts.db.tables.Geozone;
import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class GoogleMaps
    extends JSMap
{

    // ------------------------------------------------------------------------

    private static final String  PROP_version[]             = { "google.version"          };
    private static final String  PROP_mapcontrol[]          = { "google.mapcontrol"       };
    private static final String  PROP_useSSL[]              = { "google.useSSL"           };
    private static final String  PROP_sensor[]              = { "google.sensor"           };
    private static final String  PROP_channel[]             = { "google.channel"          };
    private static final String  PROP_enableRuler[]         = { "google.enableRuler"      }; // V3 only
    private static final String  PROP_mapOptions[]          = { "google.mapOptions"       }; // V3 only
    private static final String  PROP_mapInitCallback[]     = { "google.mapInitCallback"  }; // V3 only
    private static final String  PROP_addTrafficOverlay[]   = { "google.addTrafficOverlay", "addTrafficOverlay" }; // V3 only
    private static final String  PROP_addWeatherOverlay[]   = { "google.addWeatherOverlay", "addWeatherOverlay" }; // V3 only
    private static final String  PROP_addCloudOverlay[]     = { "google.addCloudOverlay"  , "addCloudOverlay"   }; // V3 only
    private static final String  PROP_addPlacesOverlay[]    = { "google.addPlacesOverlay" , "addPlacesOverlay"   }; // V3 only
    private static final String  PROP_addOpenStreetMap[]    = { "google.addOpenStreetMap" , "addOpenStreetMap"  }; // V3 only
    private static final String  PROP_addBingMap[]          = { "google.addBingMap"       , "addBingMap"        }; // V3 only
    private static final String  PROP_kmlOverlay[]          = { "google.kmlOverlay"       , "kmlOverlay"        }; // V3 only

    private static final String  PremierPrefix_             = "gme-";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String apiVersion   = null;
    private String mapOptions   = null;

    /* GoogleMaps instance */ 
    public GoogleMaps(String name, String key) 
    {
        super(name, key);
        this.addSupportedFeature(FEATURE_LATLON_DISPLAY);
        this.addSupportedFeature(FEATURE_GEOZONES);
        this.addSupportedFeature(FEATURE_DETAIL_REPORT);
        this.addSupportedFeature(FEATURE_DETAIL_INFO_BOX);
        this.addSupportedFeature(FEATURE_REPLAY_POINTS);
        this.addSupportedFeature(FEATURE_CENTER_ON_LAST);
        this.addSupportedFeature(FEATURE_CORRIDORS);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the API version
    *** @return The API version
    **/
    public String getApiVersion()
    {
        if (this.apiVersion == null) {
            // "maps/GoogleMapsV2.js"
            // "maps/GoogleMapsV3.js" - may not be present
            String apiV = this.getProperties().getString(PROP_version,"").trim().toLowerCase();
            if (StringTools.isBlank(apiV)) {
                apiV = "2"; // assume default V2
            }
            this.apiVersion = apiV;
        }
        return this.apiVersion;
    }

    /**
    *** Return true if using Google API v2
    **/
    public boolean isVersion2()
    {
        return this.getApiVersion().equals("2");
    }

    /**
    *** Return true if using Google API v3
    **/
    public boolean isVersion3()
    {
        return this.getApiVersion().equals("3");
    }

    /**
    *** Return true if using Google API "js"
    **/
    public boolean isVersionJS()
    {
        return this.getApiVersion().equals("js");
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the override map options
    *** @return The overriden map options
    **/
    public String getMapOptions()
    {
        if (this.mapOptions == null) {
            this.mapOptions = this.getProperties().getString(PROP_mapOptions,"");
        }
        return this.mapOptions;
    }

    /**
    *** Gets the map initialization callback (may be null)
    *** @return The map initialization callback (may be null)
    **/
    public String getMapInitCallback()
    {
        String cb = this.getProperties().getString(PROP_mapInitCallback,null);
        return cb;
    }

    // ------------------------------------------------------------------------

    /**
    *** Called after initialization of this MapProvider.  This allows the MapProvider
    *** to perform any required initialization after all attributes have been set 
    **/
    public void postInit()
    {
        if (this.isVersion3()) {
            if (this.getProperties().getBoolean(PROP_enableRuler,false)) {
                this.addSupportedFeature(FEATURE_DISTANCE_RULER); // V3 only (maybe)
            }
        }
    }

    // ------------------------------------------------------------------------

    /* validate */
    public boolean validate()
    {

        /* check authorization key */
        if (this.isVersion2()) { // V2 only
            String authKey = this.getAuthorization();
            if ((authKey == null) || authKey.startsWith("*")) {
                Print.logError("Google Map key not specified");
                return false;
            } else
            if (!authKey.startsWith(PremierPrefix_) && (authKey.length() < 30)) {
                Print.logError("Invalid Google Map key specified");
                return false;
            }
        } else 
        if (this.isVersion3()) {
            //
        } else
        if (this.isVersionJS()) {
            //
        }

        /* valid */
        return true;

    }

    // ------------------------------------------------------------------------

    /* write mapping support JS to stream */ 
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState) 
        throws IOException
    {
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(GoogleMaps.class);
        Locale       locale      = reqState.getLocale();
        RTProperties rtp         = this.getProperties();
        String       trafficText = i18n.getString("GoogleMaps.traffic","Traffic");
        String       weatherText = i18n.getString("GoogleMaps.weather","Weather");
        String       cloudText   = i18n.getString("GoogleMaps.clouds" ,"Clouds" );
        String       placesText  = i18n.getString("GoogleMaps.places" ,"Places" );

        /* weather temperature/speed units */
        String tempU = "F";
        switch (reqState.getTemperatureUnits()) {
            case C    : tempU  = "C"    ; break;
            case F    : tempU  = "F"    ; break;
        }
        String speedU = "MPH";
        switch (reqState.getSpeedUnits()) {
            case MPH  : speedU = "MPH"  ; break;
            case KPH  : speedU = "KPH"  ; break;
            case KNOTS: speedU = "KNOTS"; break;
        }

        /* Google vars */
        out.write("// --- Google specific vars ["+this.getName()+"]\n");
        JavaScriptTools.writeJSVar(out, "GOOGLE_API"              , this.getApiVersion());
        JavaScriptTools.writeJSVar(out, "GOOGLE_API_V2"           , this.isVersion2());
        JavaScriptTools.writeJSVar(out, "GOOGLE_MAP_OPTIONS"      , this.getMapOptions(), false);
        JavaScriptTools.writeJSVar(out, "GOOGLE_MAP_INIT_CALLBACK", this.getMapInitCallback(), false);
        JavaScriptTools.writeJSVar(out, "GOOGLE_BINGMAP"          , rtp.getBoolean(PROP_addBingMap,false));
        JavaScriptTools.writeJSVar(out, "GOOGLE_OPENSTREETMAP"    , rtp.getBoolean(PROP_addOpenStreetMap,false));
        // traffic
        JavaScriptTools.writeJSVar(out, "GOOGLE_TRAFFIC_OVERLAY"  , rtp.getBoolean(PROP_addTrafficOverlay,false));
        JavaScriptTools.writeJSVar(out, "TRAFFIC_TEXT"            , trafficText);
        // weather
        JavaScriptTools.writeJSVar(out, "GOOGLE_WEATHER_OVERLAY"  , rtp.getBoolean(PROP_addWeatherOverlay,false));
        JavaScriptTools.writeJSVar(out, "WEATHER_TEMPU"           , tempU);
        JavaScriptTools.writeJSVar(out, "WEATHER_SPEEDU"          , speedU);
        JavaScriptTools.writeJSVar(out, "WEATHER_TEXT"            , weatherText);
        // clouds
        JavaScriptTools.writeJSVar(out, "GOOGLE_CLOUD_OVERLAY"    , rtp.getBoolean(PROP_addCloudOverlay,false));
        JavaScriptTools.writeJSVar(out, "CLOUD_TEXT"              , cloudText);
        // places
        JavaScriptTools.writeJSVar(out, "GOOGLE_PLACES_OVERLAY"   , rtp.getBoolean(PROP_addPlacesOverlay,false));
        JavaScriptTools.writeJSVar(out, "PLACES_TEXT"             , placesText);
        // KML
        String kmlURL = rtp.getString(PROP_kmlOverlay,null);
        JavaScriptTools.writeJSVar(out, "GOOGLE_KML_OVERLAY"      , !StringTools.isBlank(kmlURL));
        JavaScriptTools.writeJSVar(out, "GOOGLE_KML_URL"          , kmlURL);

        /* general JS vars */
        super.writeJSVariables(out, reqState);

    }

    // ------------------------------------------------------------------------

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {

        /* map provider properties */
        MapProvider  mp   = reqState.getMapProvider(); // "(mp == this)" should be true
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;

        /* authorization key */
        String mapCtlURL = (mrtp != null)? mrtp.getString(PROP_mapcontrol,null) : null;
        if (StringTools.isBlank(mapCtlURL)) {
            // Initialize URL
            //  - http://maps.google.com/maps?file=api&v=2
            //  - http://maps.google.com/maps/api/js?v=3
            //  - http://maps.googleapis.com/maps/api/js
            StringBuffer sb = new StringBuffer();
            // -- "https://" or "http://"
            String useSSLStr = mrtp.getString(PROP_useSSL, null);
            boolean useSSL = false; // mrtp.getBoolean(PROP_useSSL, false);
            if (StringTools.isBlank(useSSLStr)) {
                // -- default: follow parent URL secure protocol
                useSSL = reqState.isSecure()? true : false;
            } else 
            if (useSSLStr.equalsIgnoreCase("auto")) {
                // -- auto: follow parent URL secure protocol
                useSSL = reqState.isSecure()? true : false;
            } else {
                // -- explicit: use specified ssl mode
                useSSL = StringTools.parseBoolean(useSSLStr, false);
            }
            sb.append(useSSL? "https://" : "http://");
            // -- API version
            if (this.isVersion3()) {
                sb.append("maps.google.com/maps/api/js?v=3");
                StringBuffer libs = new StringBuffer();
                StringBuffer keys = new StringBuffer();
                // -- weather/cloud
                RTProperties rtp = this.getProperties();
                boolean weather = 
                    rtp.getBoolean(PROP_addWeatherOverlay,false) || 
                    rtp.getBoolean(PROP_addCloudOverlay  ,false);
                if (weather) {
                    if (libs.length() > 0) { libs.append(","); }
                    libs.append("weather");
                }
                // -- places
                boolean places =
                    rtp.getBoolean(PROP_addPlacesOverlay ,false);
                if (places) {
                    if (libs.length() > 0) { libs.append(","); }
                    libs.append("places");
                }
                // -- has libraries?
                if (libs.length() > 0) {
                    sb.append("&libraries=").append(libs);
                }
            } else
            if (this.isVersion2()) {
                // -- OBSOLETE
                sb.append("maps.google.com/maps?file=api&v=2");
            } else
            if (this.isVersionJS()) {
                // -- generic JavaScript
                sb.append("maps.googleapis.com/maps/api/js?sensor=false");
            } else {
                // -- default to V3 (weather,places not loaded)
                sb.append("maps.google.com/maps/api/js?v=3");
              //sb.append("maps.google.com/maps?file=api&v=2");
            }
            // -- "&key="
            String channelVal = (mrtp != null)? mrtp.getString(PROP_channel,"") : "";
            String authKey    = this.getAuthorization();
            if (!StringTools.isBlank(authKey) && !authKey.startsWith("*")) {
                // -- a Google API key has been specified
                if (authKey.startsWith(PremierPrefix_)) {
                    sb.append("&client=").append(authKey);
                    if (StringTools.isBlank(channelVal)) {
                        channelVal = DBConfig.getServiceAccountID(null);
                    }
                } else
                if (this.isVersion2() && (authKey.length() < 25)) {
                    Print.logError("Invalid Google Map V2 key specified");
                } else {
                    sb.append("&key=").append(authKey);
                }
            } else {
                // -- no Google API key specified
                if (this.isVersion2()) {
                    Print.logError("Google Map V2 key not specified");
                }
            }
            // -- "&channel="
            if (!StringTools.isBlank(channelVal)) {
                sb.append("&channel=").append(channelVal);
            }
            // -- "&sensor="
            String sensorVal = (mrtp != null)? mrtp.getString(PROP_sensor,"true") : "true";
            if (!StringTools.isBlank(sensorVal)) {
                sb.append("&sensor=").append(sensorVal);
            }
            // -- "&oe=" character encoding
            sb.append("&oe=").append("utf-8");
            // -- "&hl=" localization
            String localStr = reqState.getPrivateLabel().getLocaleString();
            if (!StringTools.isBlank(localStr)) {
                sb.append("&hl=").append(localStr);
            }
            // -- URL
            mapCtlURL = sb.toString();
        }

        /* display Javascript */
        //Print.logInfo("Writing GoogleMaps JavaScript includes ...");
        super.writeJSIncludes(out, reqState, new String[] {
            JavaScriptTools.qualifyJSFileRef("maps/jsmap.js"),
            mapCtlURL,
            ((this.isVersion3() || this.isVersionJS())?
                JavaScriptTools.qualifyJSFileRef("maps/GoogleMapsV3.js") :
                JavaScriptTools.qualifyJSFileRef("maps/GoogleMapsV2.js") )
        });
        //Print.logInfo("... Done writing GoogleMaps JavaScript includes.");

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the number of supported points for the specified Geozone type
    *** @param type  The Geozone type
    *** @return The number of supported points for the specified Geozone type
    **/
    public int getGeozoneSupportedPointCount(int type)
    {

        /* Geozone type supported? */
        Geozone.GeozoneType gzType = Geozone.getGeozoneType(type);
        if (!Geozone.IsGeozoneTypeSupported(gzType)) {
            return 0;
        }

        /* return supported point count */
        RTProperties rtp = this.getProperties();
        switch (gzType) {
            case POINT_RADIUS        : return rtp.getBoolean(PROP_zone_map_multipoint,false)? Geozone.GetMaxVerticesCount() : 1;
            case BOUNDED_RECT        : return 0; // not yet supported
            case SWEPT_POINT_RADIUS  : return rtp.getBoolean(PROP_zone_map_corridor  ,false)? Geozone.GetMaxVerticesCount() : 0;
            case POLYGON             : return rtp.getBoolean(PROP_zone_map_polygon   ,false)? Geozone.GetMaxVerticesCount() : 0;
        }
        return 0;

    }

    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        I18N i18n = I18N.getI18N(GoogleMaps.class, loc);
        if (type == Geozone.GeozoneType.POINT_RADIUS.getIntValue()) {
            return new String[] { 
                i18n.getString("GoogleMaps.geozoneNotes.1", "Click to reset center."),
                i18n.getString("GoogleMaps.geozoneNotes.2", "Click-drag center to move."),
                i18n.getString("GoogleMaps.geozoneNotes.3", "Click-drag radius to resize."),
            };
        } else
        if (type == Geozone.GeozoneType.POLYGON.getIntValue()) {
            return new String[] { 
                i18n.getString("GoogleMaps.geozoneNotes.1", "Click to reset center."),
                i18n.getString("GoogleMaps.geozoneNotes.2", "Click-drag center to move."),
                i18n.getString("GoogleMaps.geozoneNotes.4", "Click-drag corner to resize."),
            };
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

}
