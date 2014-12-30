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
//  Tools for obtaining static Google Maps for mobile devices
//  http://code.google.com/apis/maps/documentation/staticmaps/
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

import org.opengts.util.*;

/**
*** Tools for obtaining static Google Maps for mobile devices
**/

public class GoogleStaticMap
{

    // ------------------------------------------------------------------------

    public static String    GOOGLE_MAP_URL          = "http://maps.googleapis.com/maps/api/staticmap";

    // ------------------------------------------------------------------------

    public static String    MAPTYPE_MOBILE          = "mobile";
    public static String    MAPTYPE_ROADMAP         = "roadmap";
    public static String    MAPTYPE_SATELLITE       = "satellite";
    public static String    MAPTYPE_HYBRID          = "hybrid";
    public static String    MAPTYPE_TERRAIN         = "terrain";

    // ------------------------------------------------------------------------

    public static String    PUSHPIN_SIZE_TINY       = "tiny";
    public static String    PUSHPIN_SIZE_MID        = "mid";
    public static String    PUSHPIN_SIZE_SMALL      = "small";
    public static String    PUSHPIN_SIZE[]          = new String[] {
        PUSHPIN_SIZE_TINY, 
        PUSHPIN_SIZE_MID,
        PUSHPIN_SIZE_SMALL
    };

    public static String    PUSHPIN_COLOR_BLACK     = "black";
    public static String    PUSHPIN_COLOR_BROWN     = "brown";
    public static String    PUSHPIN_COLOR_RED       = "red";
    public static String    PUSHPIN_COLOR_ORANGE    = "orange";
    public static String    PUSHPIN_COLOR_YELLOW    = "yellow";
    public static String    PUSHPIN_COLOR_GREEN     = "green";
    public static String    PUSHPIN_COLOR_BLUE      = "blue";
    public static String    PUSHPIN_COLOR_PURPLE    = "purple";
    public static String    PUSHPIN_COLOR_GRAY      = "gray";
    public static String    PUSHPIN_COLOR_WHITE     = "white";
    public static String    PUSHPIN_COLOR[]         = new String[] {
        PUSHPIN_COLOR_BLACK,
        PUSHPIN_COLOR_BROWN,
        PUSHPIN_COLOR_RED,
        PUSHPIN_COLOR_ORANGE,
        PUSHPIN_COLOR_YELLOW,
        PUSHPIN_COLOR_GREEN,
        PUSHPIN_COLOR_BLUE,
        PUSHPIN_COLOR_PURPLE,
        PUSHPIN_COLOR_GRAY,
        PUSHPIN_COLOR_WHITE,
    };

    /**
    *** Creates a pushpin name based on the specified size, color, and tag
    *** @param size  The pushpin size ("tiny", "mid", "small")
    *** @param color The pushpin color ("red", "green", ...)
    *** @param tag   Alphanumeric letter/digit tag
    *** @return The composite pushpin name
    **/
    public static String CreatePushpinIcon(String size, String color, String tag)
    {
        String S = StringTools.blankDefault(size , PUSHPIN_SIZE_MID );
        String C = StringTools.blankDefault(color, PUSHPIN_COLOR_RED);
        if (PUSHPIN_SIZE_TINY.equals(S)) {
            return S + C;
        } else {
            String L = StringTools.blankDefault(tag,"").toLowerCase();
            return S + C + L;
        }
    }

    public static String DEFAULT_PUSHPIN = CreatePushpinIcon(PUSHPIN_SIZE_MID,PUSHPIN_COLOR_RED,"o");

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private java.util.List<String>  pushpins    = new Vector<String>();
    private java.util.List<String>  pathLine    = new Vector<String>();

    private GeoBounds               bounds      = new GeoBounds();
    private GeoPoint                center      = null;

    private int                     width       = 200;
    private int                     height      = 250;
    private String                  googleKey   = "";
    private String                  mapType     = MAPTYPE_MOBILE;
    private int                     zoom        = 8;
    private boolean                 sensor      = false;
    
    private ColorTools.RGB          pathColor   = null;
    private int                     pathWeight  = 2;

    public GoogleStaticMap()
    {
        //
    }
    
    public GoogleStaticMap(int width, int height, String key)
    {
        this.setSize(width, height);
        this.setGoogleKey(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** clears all pushpins
    **/
    public void resetPushpins(boolean resetZoom)
    {
        this.pushpins.clear();
        this.pathLine.clear();
        this.bounds.reset();
        this.setCenter(null);
        if (resetZoom) {
            this.setZoom(-1);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the preferred map size
    *** @return The map width
    **/
    public void setSize(int W, int H)
    {
        this.width  = W;
        this.height = H;
    }

    /**
    *** Gets the preferred map width
    *** @return The map width
    **/
    public int getWidth()
    {
        return this.width;
    }

    /**
    *** Gets the preferred map height
    *** @return The map height
    **/
    public int getHeight()
    {
        return this.height;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the Google Map key has been defined
    *** @return True if the Google Map key has been defined
    **/
    public boolean hasGoogleKey()
    {
        return !StringTools.isBlank(this.googleKey);
    }
    
    /**
    *** Gets the Google map authorization key
    *** @return The Google map authorization key
    **/
    public String getGoogleKey()
    {
        return this.googleKey;
    }
    
    /**
    *** Sets the Google map authorization key
    *** @param key The Google map authorization key
    **/
    public void setGoogleKey(String key)
    {
        this.googleKey = StringTools.trim(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the map type (mobile, roadmap, satellite, terrain, hybrid, ...)
    *** @param mapType  The map type
    **/
    public void setMapType(String mapType)
    {
        this.mapType = StringTools.trim(mapType);
        if (StringTools.isBlank(this.mapType)) {
            this.mapType = MAPTYPE_MOBILE;
        }
    }
    
    /**
    *** Gets the map type
    *** @return The map type
    **/
    public String getMapType()
    {
        return this.mapType;
    }

    // ------------------------------------------------------------------------
    
    private static final int ZOOM_METHOD = 1;

    /**
    *** Sets the current zoom level [from 0(lowest) to 19(highest)]
    *** @param zoom  The current zoom level
    **/
    public void setZoom(int zoom)
    {
        this.zoom = zoom;
    }

    /**
    *** Gets the current zoom level
    *** @return The current zoom level
    **/
    public int getZoom()
    {
        if (this.zoom < 0) {
            this.zoom = this.calculateZoom();
        }
        return this.zoom;
    }

    /**
    *** Calculates the best zoom of the map based on added points
    *** @return The calculated zoom of the map
    **/
    public int calculateZoom()
    {
        if (ZOOM_METHOD == 1) {
            // Reference:
            //  http://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
            // --
            double NLat = this.bounds.getTop();
            double SLat = this.bounds.getBottom();
            double ELon = this.bounds.getRight();
            double WLon = this.bounds.getLeft();
            double W    = (double)this.getWidth();
            double H    = (double)this.getHeight();
            double wrlW = 256.0; // World view width
            double wrlH = 256.0; // World view height
            int    maxZ = this.getMaxZoom();
            // --
            double NLatRad; {
                double sin   = Math.sin(NLat * Math.PI / 180.0);
                double radX2 = Math.log((1.0 + sin) / (1.0 - sin)) / 2.0;
                NLatRad = Math.max(Math.min(radX2,Math.PI),-Math.PI) / 2.0;
            }
            // --
            double SLatRad; {
                double sin   = Math.sin(SLat * Math.PI / 180.0);
                double radX2 = Math.log((1.0 + sin) / (1.0 - sin)) / 2.0;
                SLatRad = Math.max(Math.min(radX2,Math.PI),-Math.PI) / 2.0;
            }
            // --
            double latFrac = (NLatRad - SLatRad) / Math.PI;
            // --
            double lonDiff = ELon - WLon;
            double lonFrac = ((lonDiff < 0.0)? (lonDiff + 360.0) : lonDiff) / 360.0;
            // --
            int    latZoom = (int)Math.floor(Math.log(H / wrlH / latFrac) / GeoPoint.LN2);
            int    lonZoom = (int)Math.floor(Math.log(W / wrlW / lonFrac) / GeoPoint.LN2);
            // -- 
            int    zoom    = Math.min(Math.min(latZoom,lonZoom),maxZ);
            return zoom;
        } else {
            // Reference:
            //  http://slappy.cs.uiuc.edu/fall06/cs492/Group2/example.html
            //    zoom_level = log(ppd_lon/(256/360)) / log(2)
            //    m/px = cos(lat) * (1 / 2^zoom) * (40075017 / 256)
            double ppd_lat = this.getHeight() / this.bounds.getDeltaLatitude();
            double ppd_lon = this.getWidth()  / this.bounds.getDeltaLongitude();
            double ppd = (ppd_lon < ppd_lat)? ppd_lon : ppd_lat;
            double zoom = Math.log(ppd_lon/(256.0/360.0)) / Math.log(2.0);
            int z = (int)Math.floor(zoom - 1.95);
            return (z >= 0)? z : 0;
            // Other References:
            //  http://blogs.esri.com/Support/blogs/mappingcenter/archive/2009/03/19/How-can-you-tell-what-map-scales-are-shown-for-online-maps_3F00_.aspx
            //  http://squall.nrel.colostate.edu/cwis438/DisplayHTML.php?FilePath=D:/WebContent/Jim/GoogleMapsProjection.html&WebSiteID=9
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum zoom level
    **/
    public int getMaxZoom()
    {
        return 21;
    }

    /**
    *** Gets the minimum zoom level
    **/
    public int getMinZoom()
    {
        return 0;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the map center point
    *** @param cp  The map center
    **/
    public void setCenter(GeoPoint cp)
    {
        this.center = ((cp != null) && cp.isValid())? cp : null;
    }
    
    /**
    *** Gets the center of the map (may return null)
    *** @return The map center (may be null)
    **/
    public GeoPoint getCenter()
    {
        return this.center;
    }
    
    /**
    *** Calculates the best center of the map based on added points
    *** @return The calculated center of the map
    **/
    public GeoPoint calculateCenter()
    {
        return this.bounds.getCenter();
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the 'sensor generated' state of the included pushpins
    *** @param sensor True if lat/lon is autogenerated
    **/
    public void setSensorState(boolean sensor)
    {
        this.sensor = sensor;
    }

    /**
    *** Should return true if latitude/longitude is auto-generated
    *** @return True if lat/lon is autogenerated
    **/
    public boolean getSensorState()
    {
        return this.sensor;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the route path color and weight
    *** @param rgb  The route path color
    *** @param weight The route path weight
    **/
    public void setPath(ColorTools.RGB rgb, int weight)
    {
        this.pathColor  = rgb;
        this.pathWeight = (weight < 1)? 1 : (weight > 10)? 10 : weight;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified pushpin to the map 
    *** @param gpLat The latitude
    *** @param gpLon The lonfitude
    *** @param icon  The pushpin icon name
    **/
    public void addPushpin(double gpLat, double gpLon, String icon, boolean shadow)
    {
        if (GeoPoint.isValid(gpLat,gpLon)) {
            this.addPushpin(new GeoPoint(gpLat,gpLon), icon, '\0', shadow);
        }
    }

    /**
    *** Adds the specified pushpin to the map 
    *** @param gpLat  The latitude
    *** @param gpLon  The lonfitude
    *** @param color  The pushpin icon name
    **/
    public void addPushpin(double gpLat, double gpLon, String color, char label, boolean shadow)
    {
        if (GeoPoint.isValid(gpLat,gpLon)) {
            this.addPushpin(new GeoPoint(gpLat,gpLon), color, label, shadow);
        }
    }

    /**
    *** Adds the specified pushpin to the map 
    *** @param gp     The pushpin location
    *** @param color  The pushpin icon name
    **/
    public void _addPushpin(GeoPoint gp, String icon, boolean shadow)
    {
        // https://developers.google.com/chart/image/docs/gallery/dynamic_icons#icon_markers
        this.addPushpin(gp, icon, '\0', shadow);
    }

    /**
    *** Adds the specified pushpin to the map 
    *** @param gp     The pushpin location
    *** @param color  The pushpin icon name
    **/
    public void addPushpin(GeoPoint gp, String colorIcon, char label, boolean shadow)
    {
        if (GeoPoint.isValid(gp)) {
            String lat = GeoPoint.formatLatitude( gp.getLatitude());
            String lon = GeoPoint.formatLongitude(gp.getLongitude());
            StringBuffer pp = new StringBuffer();
            if (StringTools.startsWithIgnoreCase(colorIcon,"icon:")) {
                // -- explicit icon specification
                pp.append(colorIcon);
            } else
            if (StringTools.startsWithIgnoreCase(colorIcon,"color:")) {
                // -- explicit color specification
                pp.append(colorIcon);
            } else
            if (StringTools.startsWithIgnoreCase(colorIcon,"http:")) {
                // -- implicit icon specification
                pp.append("icon:").append(colorIcon);
            } else {
                // -- implicit color specification
                pp.append("color:").append(StringTools.trim(colorIcon));
            }
            pp.append("|");
            pp.append("shadow:").append(shadow).append("|");
            if (Character.isLetterOrDigit(label)) {
                pp.append("label:").append(Character.toUpperCase(label));
            } else {
                //pp.append("label:A"); // may be required?
            }
            pp.append("|");
            pp.append(lat).append(",").append(lon);
            this.pushpins.add(pp.toString());
            String rt  = lat + "," + lon;
            this.pathLine.add(rt);
            this.bounds.extendByCircle(200.0, gp);
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the number of pushpins currently on this map
    *** @return The current number of pushpins
    **/
    public int getPushpinCount()
    {
        return this.pushpins.size();
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the Google Map URL for retrieving the map image data
    *** @return A String representation of the Google map URL
    **/
    public String toString()
    {
        URIArg url = new URIArg(GOOGLE_MAP_URL);

        /* center */
        if (this.center != null) {
            url.addArg("center" , this.center.getLatitude() + "," + this.center.getLongitude());
        }

        /* common arguments */
        url.addArg("zoom"   , this.getZoom());
        url.addArg("size"   , this.getWidth() + "x" + this.getHeight());
        url.addArg("maptype", this.getMapType());
        url.addArg("sensor" , String.valueOf(this.getSensorState()));
        url.addArg("format" , "png");

        /* path */
        if ((this.pathColor != null) && (this.pathLine.size() >= 2)) {
            StringBuffer sb = new StringBuffer();
            sb.append("rgb:0x").append(this.pathColor.toString());
            sb.append(",weight:").append(this.pathWeight);
            for (String pt : this.pathLine) {
                sb.append("|");
                sb.append(pt);
            }
            url.addArg("path", sb.toString());
        }

        /* markers */
        if (!this.pushpins.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            for (String pp : this.pushpins) {
                //if (sb.length() > 0) { sb.append("|"); /*%7C*/ }
                //sb.append(pp);
                url.addArg("markers", pp);
            }
            //url.addArg("markers", sb.toString());
        }

        /* google key at the end */
        if (this.hasGoogleKey()) {
            url.addArg("key", this.getGoogleKey());
        }

        /* return URL */
        //Print.logInfo("StaticMap: " + url);
        return url.toString();

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets an array of bytes containing the Google Map for the specified location
    *** @param timeoutMS Timeout milliseconds
    *** @return A byte array containing the PNG map image
    **/
    public byte[] getMap(int timeoutMS)
        throws Throwable
    {
        String url = this.toString();
        return HTMLTools.readPage_GET(url, timeoutMS);
        // UnknownHostException
        // NoRouteToHostException
        // ConnectException
        // SocketException
        // FileNotFoundException
        // HttpIOException
        // IOException
        // Throwable
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Debug entry point
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        GoogleStaticMap gsm = new GoogleStaticMap();
        gsm.setPath(ColorTools.COLOR_RED, 2);
        gsm.setSize(640,480);
        gsm.setGoogleKey(RTConfig.getString("key",null));

        /* points */
        String gps[] = StringTools.parseStringArray(RTConfig.getString("gp",""),',');
        for (String g : gps) {
            GeoPoint gp = new GeoPoint(g);
            if (gp.isValid()) {
                gsm.addPushpin(gp, "red", 'S', true);
            }
        }
        if (gsm.getPushpinCount() <= 0) {
            Print.sysPrintln("Missing '-gp=<lat>/<lon>,<lat>/<lon>'");
            System.exit(99);
        }
        
        gsm.setZoom(gsm.calculateZoom());
        Print.sysPrintln(gsm.toString());
        
    }
    
}
