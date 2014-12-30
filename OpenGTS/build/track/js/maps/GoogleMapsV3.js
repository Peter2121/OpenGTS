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
// Required funtions defined by this module:
//   new JSMap(String mapID)
//   JSClearLayers()
//   JSSetCenter(JSMapPoint center [, int zoom])
//   JSDrawPushpins(JSMapPushpin pushPin[], int recenterMode, int replay)
//   JSDrawPOI(JSMapPushpin pushPin[])
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawShape(String type, double radius, JSMapPoint points[], String color, boolean zoomTo)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSPauseReplay(int replay)
//   JSUnload() 
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added support for Geozones
//  2008/09/01  Martin D. Flynn
//     -Added replay and geozone recenter support
//  2009/08/23  Martin D. Flynn
//     -Added color argument to JSDrawRoute
//     -Added option for drawing multiple points per device on fleet map
//  2009/09/23  Martin D. Flynn
//     -Added support for displaying multipoint geozones (single point at a time)
//  2009/11/01  Juan Carlos Argueta
//     -Added route-arrows (see ROUTE_LINE_ARROWS)
//  2010/11/29  Martin D. Flynn
//     -Removed pushpins from non-editable polygon geozones
//  2011/01/28  Martin D. Flynn
//     -Apply minimum zoom (point range) when updating a single point on the map.
//     -Added support for setting map type from "map.view" property.
//  2011/07/15  Martin D. Flynn
//     -Cloned from "GoogleMapV2.js", updated for Google Maps API v3
//     -Improved Geozone editing: point-radius, polygon, corridor.
//  2012/10/16  Martin D. Flynn
//     -Clear/Remove shapes (ie. Geozones) in JSClearLayers
//     -Added support for overriding map options (see GOOGLE_MAP_OPTIONS)
//     -Added support for Traffic layer (see GOOGLE_TRAFFIC_OVERLAY)
//     -Added support for post map initialization callback (see GOOGLE_MAP_INIT_CALLBACK)
//  2012/12/24  Martin D. Flynn
//     -Added support for Weather layer (see GOOGLE_WEATHER_OVERLAY)
//     -Fixed "bearing" for V3 to convert lat/lon to radians.
//     -Added "scaleControl: true" to display scale on maps
//  2013/05/28  Martin D. Flynn
//     -Added zoom-controls for displaying the +/- button on iPhone/iPad
//  2013/12/08  Martin D. Flynn
//     -Added Z-Indexing to pushpins
// ----------------------------------------------------------------------------
// References:
//  Overlapping markers/pushpins:
//      http://stackoverflow.com/questions/3548920/google-maps-api-v3-multiple-markers-on-exact-same-spot
//      https://github.com/jawj/OverlappingMarkerSpiderfier
// ----------------------------------------------------------------------------

var DRAG_NONE               = 0;
var DRAG_RULER              = 1;
var DRAG_GEOZONE_CENTER     = 2;
var DRAG_GEOZONE_RADIUS     = 3;

var DRAW_ACCURACY_RADIUS    = false;

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// google.maps.Marker

/* set HTML content for pushpin popup */
google.maps.Marker.prototype.setInfoWindowHTML = function(html)
{
    if (html) {
        this.infoWindow = new google.maps.InfoWindow({
            content: html
        });
    } else {
        this.infoWindow = null;
    }
    this.infoWindowOpen = false;
};

/* open pushpin popup */
google.maps.Marker.prototype.openPushpinPopup = function()
{
    if (this.infoWindow && !this.infoWindowOpen && this.getMap()) {
        this.infoWindow.open(this.getMap(), this);
        this.infoWindowOpen = true;
    }
};

/* close pushpin popup */
google.maps.Marker.prototype.closePushpinPopup = function()
{
    if (this.infoWindow && this.infoWindowOpen && this.getMap()) {
        this.infoWindow.close();
        this.infoWindowOpen = false;
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// google.maps.Map

/* init pushpin vars */
google.maps.Map.prototype.initGTS = function()
{

    /* pushpin markers */
    this.pushpinMarkers = [];
    this.routeMarkers   = [];
    this.activePopup    = null;

    /* MapTypeId */
    try {
        if ((DEFAULT_VIEW == "aerial") || (DEFAULT_VIEW == "satellite")) {
            this.setMapTypeId(google.maps.MapTypeId.SATELLITE);
        } else
        if (DEFAULT_VIEW == "hybrid") {
            this.setMapTypeId(google.maps.MapTypeId.HYBRID); 
        } else
        if (DEFAULT_VIEW == "terrain") {
            this.setMapTypeId(google.maps.MapTypeId.TERRAIN); 
        } else
        if ((DEFAULT_VIEW == "road") || (DEFAULT_VIEW == "normal") || (DEFAULT_VIEW == "default")) {
            this.setMapTypeId(google.maps.MapTypeId.ROADMAP); 
        } else
        if (DEFAULT_VIEW == "") {
            this.setMapTypeId(google.maps.MapTypeId.ROADMAP); 
        } else {
            this.setMapTypeId(DEFAULT_VIEW);
        }
    } catch (e) {
        // ignore (this catch may not be necessary here)
    }

    /* key up/down listeners */
    var self = this;
    this.rulerDragEnabled = false;
    this.keyDownListener = google.maps.event.addDomListener(document, 'keydown', function(event) {
        event = event || window.event;
        self.rulerDragEnabled = event.ctrlKey? true : false;
        if (!self.rulerDragEnabled) {
            switch (event.keyCode) {
                case 16: // shiftKey
                    break;
                case 17: // ctrlKey
                    self.rulerDragEnabled = true;
                    break;
                case 18: // altKey
                    break;
            }
        }
    });
    this.keyUpListener = google.maps.event.addDomListener(document, 'keyup', function(event) {
        event = event || window.event;
        self.rulerDragEnabled = false;
    });

    /* traffic layer? */
    // var trafficType = new google.maps.ImageMapType({
    //  name: "Traffic",
    //  getTileUrl: function(coord, zoom) { return "http://mt3.google.com/mapstt?"+"zoom="+zoom+"&x="+coord.x+"&y="+coord.y+"&client=api"; },
    //  tileSize: new google.maps.Size(256, 256),
    // });
    // map.overlayMapTypes.insertAt(0, trafficType);
    this.trafficLayer = null;
    if (GOOGLE_TRAFFIC_OVERLAY) {
        this.trafficLayer = new google.maps.TrafficLayer();
        // - initialize
        var traffic = document.createElement('DIV');
        traffic.innerHTML = TRAFFIC_TEXT; // "Traffic";
        traffic.className = "GButton";
        traffic.index = 1;
        this.controls[google.maps.ControlPosition.TOP_RIGHT].push(traffic);
        google.maps.event.addDomListener(traffic, 'click', function() {
            if (self.isTrafficVisible()) {
                self.hideTraffic();
                traffic.className = 'GButton';
            } else {
                self.showTraffic();
                traffic.className = 'GButton-Sel';
            }
        });
    }

    /* weather layer? */
    this.weatherLayer = null;
    if (GOOGLE_WEATHER_OVERLAY) {
        var tempUnits = (WEATHER_TEMPU == "F")? 
            google.maps.weather.TemperatureUnit.FAHRENHEIT :
            google.maps.weather.TemperatureUnit.CELSIUS;
        var windspeedUnits = (WEATHER_SPEEDU == "MPH")? 
            google.maps.weather.WindSpeedUnit.MILES_PER_HOUR :
            google.maps.weather.WindSpeedUnit.KILOMETERS_PER_HOUR;
        this.weatherLayer = new google.maps.weather.WeatherLayer({
            temperatureUnits: tempUnits,
            windSpeedUnit: windspeedUnits
            });
        // - initialize
        var weather = document.createElement('DIV');
        weather.innerHTML = WEATHER_TEXT; // "Weather";
        weather.className = "GButton";
        weather.index = 5;
        this.controls[google.maps.ControlPosition.TOP_RIGHT].push(weather);
        google.maps.event.addDomListener(weather, 'click', function() {
            if (self.isWeatherVisible()) {
                self.hideWeather();
                weather.className = 'GButton';
            } else {
                self.showWeather();
                weather.className = 'GButton-Sel';
            }
        });
    }

    /* cloud layer? */
    this.cloudLayer = null;
    if (GOOGLE_CLOUD_OVERLAY) {
        this.cloudLayer = new google.maps.weather.CloudLayer();
        // - initialize
        var clouds = document.createElement('DIV');
        clouds.innerHTML = CLOUD_TEXT; // "Clouds";
        clouds.className = "GButton";
        clouds.index = 6;
        this.controls[google.maps.ControlPosition.TOP_RIGHT].push(clouds);
        google.maps.event.addDomListener(clouds, 'click', function() {
            if (self.isCloudVisible()) {
                self.hideCloud();
                clouds.className = 'GButton';
            } else {
                self.showCloud();
                clouds.className = 'GButton-Sel';
            }
        });
    }

    /* places layer? */
    this.placesService = null;
    if (GOOGLE_PLACES_OVERLAY) {
        //this.placesService = new google.maps.places.PlacesService(this.gmapGoogleMap);
    }

    /* add KML overlay */
    if (GOOGLE_KML_OVERLAY && (GOOGLE_KML_URL != "")) {
        var kmlURL = GOOGLE_KML_URL;
        var kmlLayer = new google.maps.KmlLayer(kmlURL);
        kmlLayer.setMap(this);
    }

    /* map post init */
    if (GOOGLE_MAP_INIT_CALLBACK) {
        try {
            GOOGLE_MAP_INIT_CALLBACK(this);
        } catch (e) {
            //alert("Google Map init callback error\n" + e);
        }
    }

};

// ----

/* display the specified pushpin popup */
google.maps.Map.prototype.showPushpinPopup = function(pp/*JSMapPushpin*/)
{
    this.hidePushpinPopups();
    if (pp && pp.marker) {
        pp.marker.openPushpinPopup();
        this.activePopup = pp;
        jsmHighlightDetailRow(pp.rcdNdx, true);
    }
};

/* hide all visible popups */
google.maps.Map.prototype.hidePushpinPopups = function()
{
    for (var i = 0; i < this.pushpinMarkers.length; i++) {
        this.pushpinMarkers[i].marker.closePushpinPopup();
    }
    if (this.activePopup) {
        jsmHighlightDetailRow(this.activePopup.rcdNdx, false);
        this.activePopup = null;
    }
};

/* add pushpin to map */
google.maps.Map.prototype.addPushpinMarker = function(pp/*JSMapPushpin*/)
{

    /* accuracy radius */
    if (pp.accRadius) {
        pp.accRadius.setMap(this);
    }

    /* pushpins */
    if (pp.bgMarker) {
        pp.bgMarker.setMap(this);
    }
    pp.marker.setMap(this);

    /* save */
    this.pushpinMarkers.push(pp);

    /* info-balloon 'click' listener */
    var self = this;
    google.maps.event.addListener(pp.marker, 'click', function(event) { 
        self.showPushpinPopup(pp);
    });
    
};

/* remove all pushpins from map */
google.maps.Map.prototype.removePushpinMarkers = function()
{
    for (var i = 0; i < this.pushpinMarkers.length; i++) {
        var pp = this.pushpinMarkers[i]; // JSMapPushpin
        if (pp.accRadius) {
            pp.accRadius.setMap(null);
        }
        if (pp.bgMarker) {
            pp.bgMarker.setMap(null);
        }
        pp.marker.setMap(null);
    }
    this.pushpinMarkers = [];
};

// ----

/* add route marker */
google.maps.Map.prototype.addRouteMarker = function(marker)
{
    this.routeMarkers.push(marker);
};

/* remove route markers */
google.maps.Map.prototype.removeRouteMarkers = function()
{
    for (var i = 0; i < this.routeMarkers.length; i++) {
        this.routeMarkers[i].setMap(null);
        // this.removeOverlay(this.routeMarkers[i]);
    }
    this.routeMarkers = [];
};

// ----

/* remove specified layer from this map */
google.maps.Map.prototype.removeOverlay = function(layer)
{
    if (layer != null) {
        layer.setMap(null);
    }
};

/* add specified layer to this map */
google.maps.Map.prototype.addOverlay = function(layer)
{
    if (layer != null) {
        layer.setMap(this);
    }
};

/* toggle specified layer */
google.maps.Map.prototype.toggleOverlay = function(layer)
{
    if (layer != null) {
        if (layer.getMap() == null) {
            layer.setMap(this);
        } else {
            layer.setMap(null);
        }
    }
};

// ----

/* hide traffic layer */
google.maps.Map.prototype.hideTraffic = function()
{
    if (this.trafficLayer != null) {
        this.removeOverlay(this.trafficLayer);
    }
};

/* show traffic layer */
google.maps.Map.prototype.showTraffic = function()
{
    if (this.trafficLayer != null) {
        this.addOverlay(this.trafficLayer);
    }
};

/* toggle traffic layer */
google.maps.Map.prototype.toggleTraffic = function()
{
    if (this.trafficLayer != null) {
        if (this.isTrafficVisible()) {
            this.hideTraffic();
        } else {
            this.showTraffic();
        }
    }
};

/* is traffic layer visible? */
google.maps.Map.prototype.isTrafficVisible = function()
{
    if (this.trafficLayer != null) {
        return (this.trafficLayer.getMap() != null)? true : false;
    } else {
        return false;
    }
};

// ----

/* hide weather layer */
google.maps.Map.prototype.hideWeather = function()
{
    if (this.weatherLayer != null) {
        this.removeOverlay(this.weatherLayer);
    }
};

/* show weather layer */
google.maps.Map.prototype.showWeather = function()
{
    if (this.weatherLayer != null) {
        this.addOverlay(this.weatherLayer);
    }
};

/* toggle weather layer */
google.maps.Map.prototype.toggleWeather = function()
{
    if (this.weatherLayer != null) {
        if (this.isWeatherVisible()) {
            this.hideWeather();
        } else {
            this.showWeather();
        }
    }
};

/* is weather layer visible? */
google.maps.Map.prototype.isWeatherVisible = function()
{
    if (this.weatherLayer != null) {
        return (this.weatherLayer.getMap() != null)? true : false;
    } else {
        return false;
    }
};

// ----

/* hide cloud layer */
google.maps.Map.prototype.hideCloud = function()
{
    if (this.cloudLayer != null) {
        this.removeOverlay(this.cloudLayer);
    }
};

/* show cloud layer */
google.maps.Map.prototype.showCloud = function()
{
    if (this.cloudLayer != null) {
        this.addOverlay(this.cloudLayer);
    }
};

/* toggle cloud layer */
google.maps.Map.prototype.toggleCloud = function()
{
    if (this.cloudLayer != null) {
        if (this.isCloudVisible()) {
            this.hideCloud();
        } else {
            this.showCloud();
        }
    }
};

/* is cloud layer visible? */
google.maps.Map.prototype.isCloudVisible = function()
{
    if (this.cloudLayer != null) {
        return (this.cloudLayer.getMap() != null)? true : false;
    } else {
        return false;
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Create GLatLng(...)
**/
function jsNewGLatLng(lat, lon)
{
    return new google.maps.LatLng(lat, lon);
};

/**
*** Create GLatLngBounds()
**/
function jsNewGLatLngBounds()
{
    return new google.maps.LatLngBounds();
};

/**
*** Create GSize()
**/
function jsNewGSize(W, H) 
{
    return new google.maps.Size(W, H);
};

/**
*** Create GPoint()
**/
function jsNewGPoint(X, Y) 
{
    return new google.maps.Point(X, Y);
};

/**
*** Create GPolyline()
**/
function jsNewGPolyline(latLonList, 
    borderColor, borderWidth, borderOpacity)
{
    return new google.maps.Polyline({
        path:          latLonList,
        strokeColor:   borderColor,
        strokeWeight:  borderWidth,
        strokeOpacity: borderOpacity
    });
};

/**
*** Create GPolygon()
**/
function jsNewGPolygon(latLonList/*GLatLng[]*/, 
    borderColor, borderWidth, borderOpacity, 
    fillColor, fillOpacity)
{
    return new google.maps.Polygon({
        paths:         latLonList,
        strokeColor:   borderColor,
        strokeWeight:  borderWidth,
        strokeOpacity: borderOpacity,
        fillColor:     fillColor,
        fillOpacity:   fillOpacity
    });
};

/**
*** Create GCircle()
**/
function jsNewGCircle(
    latLonCenter/*GLatLng*/, radiusM,
    borderColor, borderWidth, borderOpacity, 
    fillColor, fillOpacity)
{
    try {
        var cp = {
            center:        latLonCenter,
            radius:        radiusM,
            strokeColor:   borderColor,
            strokeWeight:  borderWidth,
            strokeOpacity: borderOpacity,
            fillColor:     fillColor,
            fillOpacity:   fillOpacity,
            clickable:     true
        };
        //alert("jsNewGCircle: center="+cp.center+", radius="+cp.radius+", strokeColor="+cp.strokeColor);
        return new google.maps.Circle(cp);
    } catch (e) {
        alert("Error creating circle: " + e);
        return null;
    }
};

/**
*** Create Pushpin Marker()
**/
function jsNewImageMarker(
    point/*GLatLng*/, 
    image, iconSize, iconAnchor, 
    shadow, shadowSize, 
    infoWindowAnchor,
    draggable,
    zIndex
    ) 
{
    var iconImage = new google.maps.MarkerImage(
        image,                          // imageURL
        iconSize,                       // iconSize
        new google.maps.Point(0,0),     // iconOrigin
        iconAnchor);                    // iconAnchor
    var marker = new google.maps.Marker({
        clickable: true,
        dragable: draggable,
        raiseOnDrag: false,
        flat: false,
        optimized: false,
        position: point,
        icon: iconImage,
      //shadow: shadowImage,
      //shape: shape,
        title: "" //,
      //zIndex: zIndex  // google.maps.Marker.MAX_ZINDEX + 1
        });
    if (shadow && shadowSize) {
        var shadowImage = new google.maps.MarkerImage(
            shadow,                         // shadowURL
            shadowSize,                     // shadowSize
            new google.maps.Point(0,0),     // shadowOrigin
            iconAnchor);                    // shadowAnchor
        marker.setShadow(shadowImage);
    }
    if (zIndex > 0) {
        marker.setZIndex(zIndex);
    }
    return marker;
};

// ----------------------------------------------------------------------------

/**
*** JSMap constructor
**/
function JSMap(element)
{
    // http://savas.me/2012/06/zoom-buttons-for-the-google-maps-control-on-the-iphone/
    //if (navigator.platform.match(/linux|bsd/i)) { _mSvgEnabled = _mSvgForced = true; }

    /* map options */
    if (!GOOGLE_MAP_OPTIONS || (GOOGLE_MAP_OPTIONS == "")) {
        GOOGLE_MAP_OPTIONS = {
            mapTypeControlOptions: {
                mapTypeIds: [ google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.HYBRID ] //, fixed 2.4.1-B30
            },
            draggable: true,
            draggableCursor: "auto", 
            draggingCursor: "move" ,
            disableDoubleClickZoom: true,
            overviewMapControl: true,
            scaleControl: true,
            // -- these zoom controls are necessary for +/- button on iPad/iPhone
            zoomControl: true,
            zoomControlOptions: {
                position: google.maps.ControlPosition.TL,
                style: google.maps.ZoomControlStyle.LARGE // DEFAULT will not work on iPad/iPhone
            } // ,
            // --
            //streetViewControl: true,
            //streetViewControl: true,
            //overviewMapControl: true
            // --
            //zoom: 8,
            //center: jsNewGLatLng(<lat>, <lon>),
            //mapTypeId: google.maps.MapTypeId.ROADMAP,  // SATELLITE, HYBRID, TERRAIN
        };
    }

    /* add Bing Maps */
    if (GOOGLE_BINGMAP) {
        GOOGLE_MAP_OPTIONS.mapTypeControlOptions.mapTypeIds.push('bing');
    }

    /* add OpenStreetMaps */
    if (GOOGLE_OPENSTREETMAP) {
        GOOGLE_MAP_OPTIONS.mapTypeControlOptions.mapTypeIds.push('osm');
    }

    /* map */
    this.gmapGoogleMap = new google.maps.Map(element, GOOGLE_MAP_OPTIONS);

    /* add Bing */
    if (GOOGLE_BINGMAP) {
        // EXPERIMENTAL (not fully supported)
        // - http://userscripts.org/scripts/review/84975
        this.gmapGoogleMap.mapTypes.set('bing', new google.maps.ImageMapType({
            name: "Bing", // Bing/VirtualEarth
            getTileUrl: function(C, Z) { 
                var sid = (((C.y&1)<<1)+(C.x&1));
                var URL = "http://ecn.t"+sid+".tiles.virtualearth.net/tiles/r";
                for (var i = Z-1; i>=0; i--) { URL += (((((C.y>>i)&1)<<1)+((C.x>>i)&1))); }
                URL += (Z >= 12)? ".png?g=41&productSet=mmOS" : "?g=550&shading=hill";
                return URL;
            },
            isPng: true,
            tileSize: new google.maps.Size(256, 256),
            maxZoom: 18
        }));
    }

    /* add OpenStreetMaps */
    if (GOOGLE_OPENSTREETMAP) {
        var OSM_URL = "http://tile.openstreetmap.org";
        //var OSM_URL = "http://otile1.mqcdn.com/tiles/1.0.0/osm";  // Mapquest-Tile
        //var OSM_URL = "http://oatile1.mqcdn.com/tiles/1.0.0/sat"; // Mapquest-Areal
        this.gmapGoogleMap.mapTypes.set('osm', new google.maps.ImageMapType({
            name: "OSM", // OpenStreetMap
            getTileUrl: function(C, Z) { return OSM_URL+"/"+Z+"/"+C.x+"/"+C.y+".png"; },
            tileSize: new google.maps.Size(256, 256),
            maxZoom: 18
        }));
    }

    /* map post init */
    this.gmapGoogleMap.initGTS();

    /* general variable definition */
    this.jsmapBounds = null;
    var self = this;

    /* set crosshair cursor */
    element.style.cursor = "crosshair"; // may not be effective

    /* replay vars */
    this.replayTimer = null;
    this.replayIndex = 0;
    this.replayInterval = (REPLAY_INTERVAL < 100)? 100 : REPLAY_INTERVAL;
    this.replayInProgress = false;
    this.replayPushpins = [];

    /* zone vars */
    this.geozoneIndex  = -1;

    /* drawn shapes */
    this.drawShapes = [];

    /* 'mousemove' to update latitude/longitude */
    var locDisp = document.getElementById(ID_LAT_LON_DISPLAY);
    if (locDisp != null) {
        google.maps.event.addListener(this.gmapGoogleMap, "mousemove", function(event) {
            if (event && event.latLng) {
                var point = event.latLng;
                jsmSetLatLonDisplay(point.lat(), point.lng());
                element.style.cursor = "crosshair";
            }
        });
        jsmSetLatLonDisplay(0,0);
    }

    /* "click" */
    google.maps.event.addListener(this.gmapGoogleMap, "click", function(event) {
        if (!event || !event.latLng) { return; }
        var point = event.latLng;
        var LL = new JSMapPoint(point.lat(), point.lng());
        if (jsvGeozoneMode && jsvZoneEditable) {
            // recenter geozone
            if (jsvZoneType == ZONE_POINT_RADIUS) {
                var radiusM = zoneMapGetRadius(false);
                var foundZoneNdx = -1;
                for (var x = 0; x < jsvZoneList.length; x++) {
                    var pt = jsvZoneList[x];
                    if (geoIsValid(pt.lat,pt.lon)) {
                        if (geoDistanceMeters(pt.lat, pt.lon, LL.lat, LL.lon) <= radiusM) {
                            foundZoneNdx = x;
                            break;
                        }
                    }
                }
                if (foundZoneNdx >= 0) { // inside an existing zone
                    // skip
                } else {
                    jsmSetPointZoneValue(LL.lat, LL.lon, radiusM);
                    mapProviderParseZones(jsvZoneList);
                }
            } else
            if (jsvZoneType == ZONE_POLYGON) {
                var count = 0; // count number of valid points
                for (var x = 0; x < jsvZoneList.length; x++) {
                    var pt = jsvZoneList[x];
                    if (geoIsValid(pt.lat,pt.lon)) { count++; }
                }
                if (count == 0) {
                    var radiusM = 450;  // no valid points - create default polygon
                    var crLat   = geoRadians(point.lat());  // radians
                    var crLon   = geoRadians(point.lng());  // radians
                    for (x = 0; x < jsvZoneList.length; x++) {
                        var deg   = x * (360.0 / jsvZoneList.length);
                        var radM  = radiusM / EARTH_RADIUS_METERS;
                        if ((deg == 0.0) || ((deg > 170.0) && (deg<  190.0))) { radM *= 0.8; }
                        var xrad  = geoRadians(deg); // radians
                        var rrLat = Math.asin(Math.sin(crLat) * Math.cos(radM) + Math.cos(crLat) * Math.sin(radM) * Math.cos(xrad));
                        var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(radM) * Math.cos(crLat), Math.cos(radM)-Math.sin(crLat) * Math.sin(rrLat));
                        _jsmSetPointZoneValue(x, geoDegrees(rrLat), geoDegrees(rrLon), 0);
                    }
                } else
                if (true) {
                    // just move the selected location
                    jsmSetPointZoneValue(LL.lat, LL.lon, 0);
                } else {
                    // move valid points to new location
                    var bounds = jsNewGLatLngBounds();
                    for (var x = 0; x < jsvZoneList.length; x++) {
                        var pt = jsvZoneList[x];
                        if (geoIsValid(pt.lat,pt.lon)) {
                            bounds.extend(jsNewGLatLng(pt.lat, pt.lon));
                        }
                    }
                    var center   = bounds.getCenter(); // GLatLng
                    var deltaLat = point.lat() - center.lat();
                    var deltaLon = point.lng() - center.lng();
                    for (var x = 0; x < jsvZoneList.length; x++) {
                        var pt = jsvZoneList[x];
                        if (geoIsValid(pt.lat,pt.lon)) {
                            _jsmSetPointZoneValue(x, (pt.lat + deltaLat), (pt.lon + deltaLon), 0);
                        }
                    }
                }
                // parse points
                mapProviderParseZones(jsvZoneList);
            } else
            if (jsvZoneType == ZONE_SWEPT_POINT_RADIUS) {
                var radiusM = zoneMapGetRadius(false);
                jsmSetPointZoneValue(LL.lat, LL.lon, radiusM);
                mapProviderParseZones(jsvZoneList);
            }
        }
    });

    /* right-click-drag to display 'ruler' */
    this.dragRulerLatLon = null;
    this.rulerOverlay = null;
    var distDisp = document.getElementById(ID_DISTANCE_DISPLAY);
    if (distDisp != null) {
        /* */
        google.maps.event.addListener(this.gmapGoogleMap, 'mousedown', function(event) { // "mousedown", "dragstart", "dragend"
            // "event.ctrlKey" is not available
            if (event && event.latLng && self.gmapGoogleMap.rulerDragEnabled) {
                // clear existing
                if (self.rulerOverlay != null) { // existing ruler
                    self.rulerOverlay.setMap(null);
                    self.rulerOverlay = null;
                }
                self.dragRulerLatLon = null;
                // disable map dragging
                //self.gmapGoogleMap.setOptions({ draggable: false });
                // enable ruler
                var LL = event.latLng; // GLatLng
                var CC = new JSMapPoint(LL.lat(),LL.lng());
                var ruler = [];
                ruler.push(jsNewGLatLng(CC.lat,CC.lon));
                ruler.push(jsNewGLatLng(LL.lat(),LL.lng())); // copy
                self.rulerOverlay = jsNewGPolyline(ruler, '#FF6422', 2, 1.0); // google.maps.Polyline
                self.rulerOverlay.setMap(self.gmapGoogleMap);
                google.maps.event.addListenerOnce(self.rulerOverlay, 'mouseup', function(event) {
                    // clear existing
                    if (self.rulerOverlay != null) { // existing ruler
                        self.rulerOverlay.setMap(null);
                        self.rulerOverlay = null;
                    }
                    self.dragRulerLatLon = null;
                    // re-enable dragging
                    //self.gmapGoogleMap.setOptions({ draggable: true });
                });
                self.dragRulerLatLon = CC;
                jsmSetDistanceDisplay(0);
            }
        });
        google.maps.event.addListener(this.gmapGoogleMap, 'mousemove', function(event) {
            if (event && event.latLng && self.dragRulerLatLon && self.rulerOverlay) {
                var CC = self.dragRulerLatLon;  // JSMapPoint
                var LL = event.latLng;          // GLatLng
                var ruler = [];
                ruler.push(jsNewGLatLng(CC.lat,CC.lon));
                ruler.push(jsNewGLatLng(LL.lat(),LL.lng())); // copy
                self.rulerOverlay.setPath(ruler);
                jsmSetDistanceDisplay(geoDistanceMeters(CC.lat, CC.lon, LL.lat(), LL.lng()));
            }
        });
        /* */
        /*
        google.maps.event.addListener(this.gmapGoogleMap, 'mouseup', function(event) {
            // is never called
            alert("MouseUp ...");
            if (self.dragRulerLatLon) {
                if (self.rulerOverlay != null) { // existing ruler
                    self.rulerOverlay.setMap(null);
                    self.rulerOverlay = null;
                }
                self.dragRulerLatLon = null;
            }
        });
        */
    }

};

// ----------------------------------------------------------------------------

/**
*** Unload/release resources
**/
JSMap.prototype.JSUnload = function()
{
    //GUnload();
};

// ----------------------------------------------------------------------------

/**
*** Clear all pushpins and drawn lines
**/
JSMap.prototype.JSClearLayers = function()
{

    /* clear all pushpins and route */
    this.gmapGoogleMap.removePushpinMarkers();
    this.gmapGoogleMap.removeRouteMarkers();

    /* remove shapes */
    this._removeShapes();

    /* reset state */
    this._clearReplay();
    this.jsmapBounds = jsNewGLatLngBounds();

    /* redraw shapes? */
    /* (not sure why shapes are being redrawn here - redrawing these shapes causes
    ** multiple geozones to be displayed on top of each other (darkening the image)
    if (this.drawShapes) {
        for (var s = 0; s < this.drawShapes.length; s++) {
            var shape = this.drawShapes[s];
            shape.setMap(this.gmapGoogleMap);
            // this.gmapGoogleMap.addLayer(shape);
        }
    }
    */

};

// ----------------------------------------------------------------------------

/**
*** Pause/Resume replay
**/
JSMap.prototype.JSPauseReplay = function(replay)
{
    /* stop replay? */
    if (!replay || (replay <= 0) || !this.replayInProgress) {
        // stopping replay
        this._clearReplay();
        return REPLAY_STOPPED;
    } else {
        // replay currently in progress
        if (this.replayTimer == null) {
            // replay is "PAUSED" ... resuming replay
            this.gmapGoogleMap.hidePushpinPopups();
            jsmHighlightDetailRow(-1, false);
            this._startReplayTimer(replay, 100);
            return REPLAY_RUNNING;
        } else {
            // replaying "RUNNING" ... pausing replay
            this._stopReplayTimer();
            return REPLAY_PAUSED;
        }
    }
};

/**
*** Start the replay timer
**/
JSMap.prototype._startReplayTimer = function(replay, interval)
{
    if (this.replayInProgress) {
        this.replayTimer = setTimeout("jsmap._replayPushpins("+replay+")", interval);
    }
    jsmSetReplayState(REPLAY_RUNNING);
};

/**
*** Stop the current replay timer
**/
JSMap.prototype._stopReplayTimer = function()
{
    if (this.replayTimer != null) { 
        clearTimeout(this.replayTimer); 
        this.replayTimer = null;
    }
    jsmSetReplayState(this.replayInProgress? REPLAY_PAUSED : REPLAY_STOPPED);
};

/**
*** Clear any current replay in process
**/
JSMap.prototype._clearReplay = function()
{
    this.replayPushpins = [];
    this.replayInProgress = false;
    this._stopReplayTimer();
    this.replayIndex = 0;
    jsmHighlightDetailRow(-1, false);
};

/**
*** Gets the current replay state
**/
JSMap.prototype._getReplayState = function()
{
    if (this.replayInProgress) {
        if (this.replayTimer == null) {
            return REPLAY_PAUSED;
        } else {
            return REPLAY_RUNNING;
        }
    } else {
        return REPLAY_STOPPED;
    }
};

// ----------------------------------------------------------------------------

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter = function(center/*JSMapPoint*/, zoom)
{
    var gpt = jsNewGLatLng(center.lat, center.lon);
    if (!zoom || (zoom == 0)) {
        this.gmapGoogleMap.setCenter(gpt);
    } else
    if (zoom > 0) {
        this.gmapGoogleMap.setCenter(gpt);
        this.gmapGoogleMap.setZoom(zoom);
    } else {
        this.gmapGoogleMap.setCenter(gpt);
        var gb = jsNewGLatLngBounds();
        gb.extend(gpt);
        this.gmapGoogleMap.fitBounds(gb);
        //var zoom = this.gmapGoogleMap.getZoom();
    }
};

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter_G = function(center/*GLatLng*/, zoom)
{
    if (zoom) {
        this.gmapGoogleMap.setCenter(center);
        this.gmapGoogleMap.setZoom(zoom);
    } else {
        this.gmapGoogleMap.setCenter(center);
    }
};

// ----------------------------------------------------------------------------

/**
*** Draw the specified pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
*** @param recenter  True to cause the map to re-center on the drawn pushpins
**/
JSMap.prototype.JSDrawPushpins = function(pushPins, recenterMode, replay)
{

    /* clear replay (may be redundant, but repeated just to make sure) */
    this._clearReplay();

    /* make sure we have a bounding box instance */
    if (!this.jsmapBounds || (this.jsmapBounds == null)) {
        this.jsmapBounds = jsNewGLatLngBounds();
    } else {
        //alert("jsmapBounds already defined ...");
    }

    /* drawn pushpins */
    var drawPushpins = [];

    /* recenter map on points */
    var pointCount = 0;
    if ((pushPins != null) && (pushPins.length > 0)) {
        // extend bounding box around displayed pushpins
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            if (pp.show && geoIsValid(pp.lat,pp.lon)) {
                pointCount++;
                this.jsmapBounds.extend(jsNewGLatLng(pp.lat, pp.lon));
                drawPushpins.push(pp); // JSMapPushpin
            }
        }
        // MinimumMapBounds: make sure points span a minimum distance top to bottom
        var rangeRadiusM = 400; // TODO: should make this a configurable options
        var cenPt = this.jsmapBounds.getCenter();
        var topPt = geoRadiusPoint(cenPt.lat(), cenPt.lng(), rangeRadiusM,   0.0); // top
        this.jsmapBounds.extend(jsNewGLatLng(topPt.lat,topPt.lon));
        var botPt = geoRadiusPoint(cenPt.lat(), cenPt.lng(), rangeRadiusM, 180.0); // bottom
        this.jsmapBounds.extend(jsNewGLatLng(botPt.lat,botPt.lon));
    }
    if (recenterMode > 0) {
        try {
            if (pointCount <= 0) {
                this.JSSetCenter(DEFAULT_CENTER, DEFAULT_ZOOM);
            } else 
            if (recenterMode == RECENTER_LAST) { // center on last point
                var pp = drawPushpins[drawPushpins.length - 1];
                this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
            } else 
            if (recenterMode == RECENTER_PAN) { // pan to last point
                var pp = drawPushpins[drawPushpins.length - 1];
                this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
            } else {
                var centerPt = this.jsmapBounds.getCenter(); // GLatLng
                //var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(this.jsmapBounds); // TODO:
                this.gmapGoogleMap.fitBounds(this.jsmapBounds);
                var zoomFactor = this.gmapGoogleMap.getZoom();
                this.JSSetCenter_G(centerPt, zoomFactor);
            }
        } catch (e) {
            //alert("Error: [JSDrawPushpins] " + e);
            return;
        }
    }
    if (pointCount <= 0) {
        return;
    }

    /* replay pushpins? */
    if (replay && (replay >= 1)) {
        this.replayIndex = 0;
        this.replayInProgress = true;
        this.replayPushpins = drawPushpins;
        this._startReplayTimer(replay, 100);
        return;
    }

    /* draw pushpins now */
    var pushpinErr = null;
    for (var i = 0; i < drawPushpins.length; i++) {
        var pp = drawPushpins[i]; // JSMapPushpin
        try {
            this._addPushpin(pp); // JSMapPushpin
        } catch (e) {
            if (pushpinErr == null) { pushpinErr = e; }
        }
    }
    if (pushpinErr != null) {
        alert("Error: adding pushpins:\n" + pushpinErr);
    }

};

/**
*** Draw the specified PointsOfInterest pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
**/
JSMap.prototype.JSDrawPOI = function(pushPins)
{

    /* draw pushpins now */
    if ((pushPins != null) && (pushPins.length > 0)) {
        var pushpinErr = null;
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            if ((pp.lat == 0.0) && (pp.lon == 0.0)) {
                continue;
            }
            try {
                this._addPushpin(pp); // JSMapPushpin
            } catch (e) {
                if (pushpinErr == null) { pushpinErr = e; }
            }
        }
        if (pushpinErr != null) {
            alert("Error: adding pushpins:\n" + pushpinErr);
        }
    }

};

/**
*** Adds a single pushpin to the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._addPushpin = function(pp) // JSMapPushpin
{
    try {
        var self = this;

        /* point pt */
        var gPT = jsNewGLatLng(pp.lat, pp.lon); // GLatLng

        /* create accuracy radius */
        var accRadM = pp.accRadM;
        if (DRAW_ACCURACY_RADIUS && (accRadM > 30/*meters*/)) {
            // point-radius shape
            var color  = pp.isCellLoc? "#FF5555" : "#5555FF";
            var circle = jsNewGCircle(gPT, accRadM, color, 1, 0.8, color, 0.1);
            pp.accRadius = circle;
        } else {
            pp.accRadius = null;
        }

        /* background marker */
        if (pp.bgUrl) {
            var bgZNdx = (pp.rcdNdx >= 0)? (google.maps.Marker.MAX_ZINDEX + pp.rcdNdx) : -1;
            var bgMarker = jsNewImageMarker(
                gPT,                                                                // GLatLng
                pp.bgUrl,                                                           // image
                pp.bgSize?   jsNewGSize(pp.bgSize[0],pp.bgSize[1])      : null,     // iconSize
                pp.bgOffset? jsNewGPoint(pp.bgOffset[0],pp.bgOffset[1]) : null,     // iconAnchor
                null,                                                               // shadow
                null,                                                               // shadowSize
                jsNewGPoint(5, 1),                                                  // infoWindowAnchor
                false,                                                              // draggable
                bgZNdx                                                              // zIndex
                );
            bgMarker.setInfoWindowHTML(null);
            pp.bgMarker = bgMarker;
        } else {
            pp.bgMarker = null;
        }

        /* pushpin marker */
        var ppZNdx = (pp.rcdNdx >= 0)? (google.maps.Marker.MAX_ZINDEX + pp.rcdNdx + 1) : -1;
        pp.marker = jsNewImageMarker(
            gPT,                                                                       // GLatLng
            pp.iconUrl,                                                                // image
            pp.iconSize?    jsNewGSize(pp.iconSize[0],pp.iconSize[1])        : null,   // iconSize
            pp.iconHotspot? jsNewGPoint(pp.iconHotspot[0],pp.iconHotspot[1]) : null,   // iconAnchor
            pp.shadowUrl,                                                              // shadow
            pp.shadowSize?  jsNewGSize(pp.shadowSize[0],pp.shadowSize[1])    : null,   // shadowSize
            jsNewGPoint(5, 1),                                                         // infoWindowAnchor
            false,                                                                     // draggable
            ppZNdx                                                                     // zIndex
            );
        pp.marker.setInfoWindowHTML(pp.getHTML());
        this.gmapGoogleMap.addPushpinMarker(pp);

    } catch(e) {
        //alert("AddPushpin ERROR: " + e);
    }
};

/**
*** Replays the list of pushpins on the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._replayPushpins = function(replay)
{

    /* advance to next valid point */
    while (true) {
        if (this.replayIndex >= this.replayPushpins.length) {
            this._clearReplay();
            jsmHighlightDetailRow(-1, false);
            return; // stop
        }
        var pp = this.replayPushpins[this.replayIndex]; // JSMapPushpin
        if (geoIsValid(pp.lat,pp.lon)) {
            break; // valid point
        }
        this.replayIndex++;
    }

    /* add pushpin */
    try {
        var lastNdx = this.replayIndex - 1;
        var pp = this.replayPushpins[this.replayIndex++]; // JSMapPushpin
        pp.hoverPopup = true;
        if (REPLAY_SINGLE && (lastNdx >= 0)) {
            var lastPP = this.replayPushpins[lastNdx]; // JSMapPushpin
            if (lastPP.marker) {
                //this.gmapGoogleMap.removeOverlay(lastPP.marker);
                lastPP.marker.setMap(null);
            }
            if (lastPP.bgMarker) {
                //this.gmapGoogleMap.removeOverlay(lastPP.bgMarker);
                lastPP.bgMarker.setMap(null);
            }
        }
        this._addPushpin(pp);
        if (replay && (replay >= 2)) {
            this.gmapGoogleMap.showPushpinPopup(pp);
        } else {
            jsmHighlightDetailRow(pp.rcdNdx, true);
        }
        this._startReplayTimer(replay, this.replayInterval);
    } catch (e) {
        alert("Replay error: " + e);
    }

};

// ----------------------------------------------------------------------------

/**
*** This method should cause the info-bubble popup for the specified pushpin to display
*** @param pushPin   The JSMapPushpin object which popup its info-bubble
**/
JSMap.prototype.JSShowPushpin = function(pp, center)
{
    if (pp) {
        if (center) {
            this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
        }
        this.gmapGoogleMap.showPushpinPopup(pp);
    }
};

// ----------------------------------------------------------------------------

/**
*** Draws a line between the specified points on the map.
*** @param points   An array of JSMapPoint objects
**/
JSMap.prototype.JSDrawRoute = function(points, color)
{

    /* remove existing route */
    this.gmapGoogleMap.removeRouteMarkers();
    
    /* no route? */
    if (color == "none") {
        return;
    }

    /* draw new route */
    var LL = []; // GLatLng[] (google.maps.LatLng[])
    for (var i = 0; i < points.length; i++) {
        var pt = jsNewGLatLng(points[i].lat, points[i].lon);
        LL.push(pt);
    }
    var polyLine = jsNewGPolyline(LL, color, 2, 1.0);
    polyLine.setMap(this.gmapGoogleMap); // this.gmapGoogleMap.addOverlay(polyLine); // "#003399"
    this.gmapGoogleMap.addRouteMarker(polyLine);
    
    /* draw mid arrows */
    if (ROUTE_LINE_ARROWS) {
        this.midArrows(LL);
    }
    
};

//  [Juan Carlos Argueta] returns the bearing in degrees between two points.
JSMap.prototype.bearing = function(from, to) { // google.maps.LatLng
    // ----- Returns the bearing in degrees between two points. -----
    // ----- North = 0, East = 90, South = 180, West = 270.
    // ----- var degreesPerRadian = 180.0 / Math.PI;

    // ----- Convert to radians.
    var lat1 = geoRadians(from.lat()); // from.latRadians();
    var lon1 = geoRadians(from.lng()); // from.lngRadians();
    var lat2 = geoRadians(to.lat());   // to.latRadians();
    var lon2 = geoRadians(to.lng());   // to.lngRadians();

    // -----Compute the angle.
    var angle = -Math.atan2(Math.sin(lon1-lon2)*Math.cos(lat2),Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon1-lon2));
    if (angle < 0.0) { angle += Math.PI * 2.0; }

    // ----- And convert result to degrees.
    angle = angle * (180.0 / Math.PI);
    angle = angle.toFixed(1);

    return angle;
};
       
// [Juan Carlos Argueta]  A function to create the arrow head at the end of the polyline ===
//  http://www.google.com/intl/en_ALL/mapfiles/dir_0.png
//  http://www.google.com/intl/en_ALL/mapfiles/dir_3.png
//  http://www.google.com/intl/en_ALL/mapfiles/dir_6.png
//  ...
JSMap.prototype.arrowHead = function(points) {	  // GLatLng[]
    // ----- obtain the bearing between the last two points
    if (!points || (points.length < 2)) { return; }
    var p1 = points[points.length-1];
    var p2 = points[points.length-2];
    // ----- round heading to a multiple of 3 and cast out 120s
    var dir = this.bearing(p2,p1);
    dir = Math.round(dir/3) * 3;
    while (dir >= 120) { dir -= 120; }
    // ----- use the corresponding triangle marker 
    var arrowMarker = jsNewImageMarker(
        p1,                                                             // GLatLng
        "http://www.google.com/intl/en_ALL/mapfiles/dir_"+dir+".png",   // image
        jsNewGSize(14,14),                                              // iconSize
        jsNewGPoint(7,7),                                               // iconAnchor
        null,                                                           // shadow
        jsNewGSize(1,1),                                                // shadowSize
        jsNewGPoint(0,0),                                               // infoWindowAnchor
        false,                                                          // draggable
        -1                                                              // zIndex
        );
    // ----- add arrow marker
    arrowMarker.setMap(this.gmapGoogleMap);  // this.gmapGoogleMap.addOverlay(arrowMarker);
};
      
// [Juan Carlos Argueta]  A function to put arrow heads at intermediate points
JSMap.prototype.midArrows = function(points) {	// GLatLng[]	  
    if (!points || (points.length < 2)) { return; }
    for (var i = 1; i < points.length - 1; i++) {  
        var p1 = points[i-1];
        var p2 = points[i+1];
        // ----- round it to a multiple of 3 and cast out 120s
        var dir = this.bearing(p1,p2);
        dir = Math.round(dir/3) * 3;
        while (dir >= 120) { dir -= 120; }
        // ----- use the corresponding triangle marker 
        var arrowMarker = jsNewImageMarker(
            points[i],                                                      // GLatLng
            "http://www.google.com/intl/en_ALL/mapfiles/dir_"+dir+".png",   // image
            jsNewGSize(14,14),                                              // iconSize
            jsNewGPoint(7,7),                                               // iconAnchor
            null,                                                           // shadow
            jsNewGSize(1,1),                                                // shadowSize
            jsNewGPoint(0,0),                                               // infoWindowAnchor
            false,                                                          // draggable
            -1                                                              // zIndex
            );
        // ----- add arrow marker
        arrowMarker.setMap(this.gmapGoogleMap);  // this.gmapGoogleMap.addOverlay(arrowMarker);
        this.gmapGoogleMap.addRouteMarker(arrowMarker);
    }
};

// ----------------------------------------------------------------------------

/**
*** Remove previously drawn shapes 
**/
JSMap.prototype._removeShapes = function()
{
    if (this.drawShapes) {
        for (var s = 0; s < this.drawShapes.length; s++) {
            this.drawShapes[s].setMap(null);
        }
    }
    this.drawShapes = [];
};

/**
*** Draws a Shape on the map at the specified location
*** @param type     The Geozone shape type ("line", "circle", "rectangle", "polygon", "center")
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param zoomTo   rue to zoom to drawn shape
*** @return True if shape was drawn, false otherwise
**/
JSMap.prototype.JSDrawShape = function(type, radiusM, verticePts, color, zoomTo)
{

    /* no type? */
    if (!type || (type == "") || (type == "!")) {
        //alert("Removing shapes only ...");
        this._removeShapes();
        return false;
    }

    /* clear existing shapes? */
    if (type.startsWith("!")) { 
        this._removeShapes();
        type = type.substr(1); 
    }

    /* no geopoints? */
    if (!verticePts || (verticePts.length == 0)) {
        alert("No points in shape! ...");
        return false;
    }

    /* color */
    if (!color || (color == "")) {
        color = "#0000FF";
    }

    /* zoom bounds */
    var mapBounds = zoomTo? jsNewGLatLngBounds() : null;

    /* draw shape */
    var didDrawShape = false;
    if (type == "circle") { // ZONE_POINT_RADIUS

        for (var p = 0; p < verticePts.length; p++) {
            var jPT = verticePts[p]; // JSMapPoint
            var gPT = jsNewGLatLng(jPT.lat,jPT.lon); // GLatLng
            //alert("Drawing circle: " + jPT.lat+"/"+jPT.lon +" rad=("+radiusM + ") color="+color);

            /* draw circle */
            var circle = jsNewGCircle(gPT, radiusM, color, 2, 0.9, color, 0.1);
            if (circle != null) {
                circle.setMap(this.gmapGoogleMap); // this.gmapGoogleMap.addOverlay(crPoly);
                this.drawShapes.push(circle);
                didDrawShape = true;
            }

            /* map bounds */
            if (mapBounds) {
                var pt000 = geoRadiusPoint(jPT.lat, jPT.lon, radiusM,   0.0);
                mapBounds.extend(jsNewGLatLng(pt000.lat,pt000.lon));
                var pt090 = geoRadiusPoint(jPT.lat, jPT.lon, radiusM,  90.0);
                mapBounds.extend(jsNewGLatLng(pt090.lat,pt090.lon));
                var pt180 = geoRadiusPoint(jPT.lat, jPT.lon, radiusM, 180.0);
                mapBounds.extend(jsNewGLatLng(pt180.lat,pt180.lon));
                var pt270 = geoRadiusPoint(jPT.lat, jPT.lon, radiusM, 270.0);
                mapBounds.extend(jsNewGLatLng(pt270.lat,pt270.lon));
            }
            
        }

    } else
    if (type == "rectangle") { // ZONE_BOUNDED_RECT
        
        if (verticePts.length >= 2) {

            /* create rectangle */
            var vp0   = verticePts[0];
            var vp1   = verticePts[1];
            var TL    = jsNewGLatLng(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var TR    = jsNewGLatLng(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var BL    = jsNewGLatLng(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var BR    = jsNewGLatLng(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var crPts = [ TL, TR, BR, BL, TL ];
    
            /* draw rectangle */
            var crPoly = jsNewGPolygon(crPts, color, 2, 0.9, color, 0.1);
            crPoly.setMap(this.gmapGoogleMap); // this.gmapGoogleMap.addOverlay(crPoly);
            this.drawShapes.push(crPoly);
            didDrawShape = true;

            /* map bounds */
            if (mapBounds) { 
                for (var b = 0; b < crPts.length; b++) { mapBounds.extend(crPts[b]); } 
            }

        }

    } else
    if (type == "polygon") { // ZONE_POLYGON
        
        if (verticePts.length >= 3) {

            /* accumulate polygon vertices */
            var crPts = [];
            for (var p = 0; p < verticePts.length; p++) {
                var gPT = jsNewGLatLng(verticePts[p].lat, verticePts[p].lon);
                crPts.push(gPT);
                if (mapBounds) { mapBounds.extend(gPT); }
            }
            crPts.push(crPts[0]); // close polygon

            /* draw polygon */
            var crPoly = jsNewGPolygon(crPts, color, 2, 0.9, color, 0.1);
            crPoly.setMap(this.gmapGoogleMap); // this.gmapGoogleMap.addOverlay(crPoly);
            this.drawShapes.push(crPoly);
            didDrawShape = true;

        }

    } else
    if (type == "corridor") { // ZONE_SWEPT_POINT_RADIUS

        // TODO: 
        
    } else
    if (type == "center") {

        if (mapBounds) {
            for (var p = 0; p < verticePts.length; p++) {
                var gPT = jsNewGLatLng(verticePts[p].lat, verticePts[p].lon);
                mapBounds.extend(gPT);
            }
            didDrawShape = true;
        }

    } else {

        alert("Unrecognized shape type: " + type);

    }

    /* center on shape */
    if (didDrawShape && zoomTo && mapBounds) {
        var centerPt = mapBounds.getCenter(); // GLatLng
        this.gmapGoogleMap.fitBounds(mapBounds);
        var zoomFactor = this.gmapGoogleMap.getZoom();
        this.JSSetCenter_G(centerPt, zoomFactor);
    }

    /* shape not supported */
    return didDrawShape;

};

// ----------------------------------------------------------------------------

var GlobalGeozoneList = [];

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param primNdx  Index of point on which to center
*** @return An object representing the Circle.
**/
JSMap.prototype.JSDrawGeozone = function(type, radiusM, points, color, primNdx)
{
    // type:
    //   0 - ZONE_POINT_RADIUS
    //   1 - ZONE_BOUNDED_RECT
    //   2 - ZONE_SWEPT_POINT_RADIUS
    //   3 - ZONE_POLYGON
    // (type ZONE_POINT_RADIUS may only be currently supported)

    /* Geozone mode */
    jsvGeozoneMode = true;

    /* remove old geozones */
    for (var i = 0; i < GlobalGeozoneList.length; i++) {
        GlobalGeozoneList[i].remove();
    }
    GlobalGeozoneList = [];

    /* no points? */
    if ((points == null) || (points.length <= 0)) {
        return null;
    }
    this.geozoneIndex  = ((primNdx >= 0) && (primNdx < points.length))? primNdx : 0;

    /* draw geozone */
    var maxLat = -90.0;
    var minLat =  90.0;
    var pointCount = 0;
    var mapBounds  = jsNewGLatLngBounds();
    if (type == ZONE_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 5000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;

        /* draw points */
        for (var i = 0; i < points.length; i++) {
            var pt = points[i];
            if (geoIsValid(pt.lat,pt.lon)) {
                var isPrimary = (i == this.geozoneIndex);
                var zColor = isPrimary? jsvZoneColor : "#55AA55";
                var prg = new PointRadiusGeozone(this.gmapGoogleMap, i,
                    pt.lat, pt.lon, radiusM, zColor, (jsvZoneEditable && isPrimary));
                mapBounds.extend(jsNewGLatLng(pt.lat,pt.lon));
                mapBounds.extend(prg.calcRadiusPoint(  0.0)); // North
                mapBounds.extend(prg.calcRadiusPoint(180.0)); // South
                GlobalGeozoneList.push(prg);
                pointCount++;
            }
        }

        /* adjust minimum map bounds */
        // TODO:

        /* center on geozone */
        /*
        var centerPt = mapBounds.getCenter(); // GLatLng
        this.gmapGoogleMap.fitBounds(mapBounds);
        var zoomFactor = this.gmapGoogleMap.getZoom();
        this.JSSetCenter_G(centerPt, zoomFactor);
        */

    } else
    if (type == ZONE_POLYGON) {

        /* draw points */
        // "radiusM" is not used
        //radiusM = 500; // may be used later for setting minimum map bounds
        var prg = new PolygonGeozone(this.gmapGoogleMap, points, jsvZoneColor, jsvZoneEditable)
        for (var i = 0; i < prg.verticeMarkers.length; i++) {
            var vm  = prg.verticeMarkers[i];
            var vpt = vm.getPosition(); // GLatLng
            if (geoIsValid(vpt.lat(),vpt.lng())) {
                mapBounds.extend(vm.getPosition());
                pointCount++;
            }
        }
        GlobalGeozoneList.push(prg);

    } else
    if (type == ZONE_SWEPT_POINT_RADIUS) {

        var zoneNdx = ((primNdx >= 0) && (primNdx < points.length))? primNdx : 0;
        var zoneCenter = points[zoneNdx]; // JSMapPoint
        if (isNaN(radiusM))              { radiusM = 1000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;

        /* draw points */
        var prg = new CorridorGeozone(this.gmapGoogleMap, points, radiusM, jsvZoneColor, jsvZoneEditable);
        for (var i = 0; i < prg.verticeMarkers.length; i++) {
            var vm = prg.verticeMarkers[i];
            if (vm.isVisible) { // point-radius vertice
                var vpt = vm.getPosition(); // GLatLng
                if (vpt.lat() < minLat) { minLat = vpt.lat(); }
                if (vpt.lat() > maxLat) { maxLat = vpt.lat(); }
                var pt000 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM,   0.0);
                mapBounds.extend(jsNewGLatLng(pt000.lat,pt000.lon));
                var pt090 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM,  90.0);
                mapBounds.extend(jsNewGLatLng(pt090.lat,pt090.lon));
                var pt180 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM, 180.0);
                mapBounds.extend(jsNewGLatLng(pt180.lat,pt180.lon));
                var pt270 = geoRadiusPoint(vpt.lat(), vpt.lng(), radiusM, 270.0);
                mapBounds.extend(jsNewGLatLng(pt270.lat,pt270.lon));
                pointCount++;
            }
        }
        GlobalGeozoneList.push(prg);

        /* center on geozone */
        //var centerPt = mapBounds.getCenter(); // GLatLng
        ////var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(mapBounds);
        //this.gmapGoogleMap.fitBounds(mapBounds);
        //var zoomFactor = this.gmapGoogleMap.getZoom();
        //this.JSSetCenter_G(centerPt, zoomFactor);

    } else {

        alert("Geozone type not supported: " + type);
        
    }

    /* center on geozone */
    if (pointCount > 0) {

        // MinimumMapBounds: make sure points span a minimum distance top to bottom
        if (maxLat >= minLat) {
            var rangeRadiusM = radiusM * 3;
            var cenPt = mapBounds.getCenter();
            var topPt = geoRadiusPoint(maxLat, cenPt.lng(), rangeRadiusM,   0.0); // top
            mapBounds.extend(jsNewGLatLng(topPt.lat,topPt.lon));
            var botPt = geoRadiusPoint(minLat, cenPt.lng(), rangeRadiusM, 180.0); // bottom
            mapBounds.extend(jsNewGLatLng(botPt.lat,botPt.lon));
        }

        /* center on points */
        var centerPt = mapBounds.getCenter(); // GLatLng
        //var zoomFactor = this.gmapGoogleMap.getBoundsZoomLevel(mapBounds);
        this.gmapGoogleMap.fitBounds(mapBounds);
        var zoomFactor = this.gmapGoogleMap.getZoom();
        this.JSSetCenter_G(centerPt, zoomFactor);

    } else {

        /* default center/zoom */
        this.JSSetCenter(DEFAULT_CENTER, DEFAULT_ZOOM);

    }

    return null;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

var pointRadiusRadiusHeading = 60.0;

/* this draws a single point-circle geozone */
function PointRadiusGeozone(gMap, zNdx,
    lat, lon, radiusM, color, editable)
{
    var self = this;

    /* circle attributes */
    this.googleMap        = gMap;
    this.zoneIndex        = zNdx;
    this.radiusMeters     = (radiusM <= MAX_ZONE_RADIUS_M)? Math.round(radiusM) : MAX_ZONE_RADIUS_M;
    this.radiusPoint      = null;
    this.centerMarker     = null;
    this.radiusMarker     = null;
    this.circlePolygon    = null;
    this.shapeColor       = (color && (color != ""))? color : "#0000FF";
    this.centerPoint      = jsNewGLatLng(lat, lon); // GLatLng

    /* center Icon/marker */
    if (editable) {

        /* create draggable center marker */
        this.centerMarker = jsNewImageMarker(
            this.centerPoint,                                               // GLatLng
            "http://labs.google.com/ridefinder/images/mm_20_blue.png",      // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            true,                                                           // draggable
            -1                                                              // zIndex
            );
        this.centerMarker.setMap(this.googleMap);
        this.centerMarker.setDraggable(true);
        google.maps.event.addListener(this.centerMarker, "dragend", function(event) {
            var oldCP = self.centerPoint; // GLatLng
            var oldRP = self.radiusMarker.getPosition(); // GLatLng
            var newCP = self.centerMarker.getPosition(); // GLatLng
            var newRP = self.calcRadiusPoint(geoHeading(oldCP.lat(),oldCP.lng(),oldRP.lat(),oldRP.lng()));
            self.centerPoint = newCP;  // GLatLng
            self.radiusMarker.setPosition(newRP);
            self.drawCircle(); 
            jsmSetPointZoneValue(newCP.lat(), newCP.lng(), self.radiusMeters);
        });

        /* radius Icon/Marker */
        this.radiusPoint  = this.calcRadiusPoint(/*Global*/pointRadiusRadiusHeading); // GLatLng
        this.radiusMarker = jsNewImageMarker(
            this.radiusPoint,                                               // GLatLng
            "http://labs.google.com/ridefinder/images/mm_20_gray.png",      // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            true,                                                           // draggable
            -1                                                              // zIndex
            );
        this.radiusMarker.setMap(this.googleMap); // this.googleMap.addOverlay(this.radiusMarker);
        this.radiusMarker.setDraggable(true); // enableDragging();
        google.maps.event.addListener(this.radiusMarker, "dragend", function(event) {
            var oldCP = self.centerMarker.getPosition();
            var newRP = self.radiusMarker.getPosition();
            var radM  = Math.round(geoDistanceMeters(oldCP.lat(),oldCP.lng(),newRP.lat(),newRP.lng()));
            self.radiusMeters = radM;
            pointRadiusRadiusHeading = geoHeading(oldCP.lat(),oldCP.lng(),newRP.lat(),newRP.lng());
            if (self.radiusMeters < MIN_ZONE_RADIUS_M) {
                self.radiusMeters = MIN_ZONE_RADIUS_M;
                newRP = self.calcRadiusPoint(pointRadiusRadiusHeading);
                self.radiusMarker.setPosition(newRP);
            } else
            if (self.radiusMeters > MAX_ZONE_RADIUS_M) {
                self.radiusMeters = MAX_ZONE_RADIUS_M;
                newRP = self.calcRadiusPoint(pointRadiusRadiusHeading);
                self.radiusMarker.setPosition(newRP); // .setPoint(newRP);
            }
            //self.drawCircle();  <-- redrawn during zoneReset below
            jsmSetPointZoneValue(oldCP.lat(), oldCP.lng(), self.radiusMeters);
            // need to redraw other point-radius zones as well
            _zoneReset();
        });

    } else {

        this.centerMarker = null;

    }

    /* draw circle */
    this.drawCircle();

};

PointRadiusGeozone.prototype.type = function()
{
    return ZONE_POINT_RADIUS;
};

PointRadiusGeozone.prototype.calcRadiusPoint = function(heading)
{
    var cpt = (this.centerMarker != null)? this.centerMarker.getPosition() : this.centerPoint; // GLatLng
    var rp  = geoRadiusPoint(cpt.lat(), cpt.lng(), this.radiusMeters, heading);
    return jsNewGLatLng(rp.lat,  rp.lon);
};

PointRadiusGeozone.prototype.drawCircle = function()
{

    /* remove old circle */
    if (this.circlePolygon != null) {
        this.circlePolygon.setMap(null);
    }

    /* draw circle */
    var color    = this.shapeColor;
    var circPoly = jsNewGCircle(
        this.centerPoint, this.radiusMeters,
        color, 2, 0.9, color, 0.1);
    circPoly.setMap(this.googleMap);
    this.circlePolygon = circPoly;

    /* "click" */
    var self = this;
    google.maps.event.addListener(this.circlePolygon, "click", function(event) {
        if (!event || !event.latLng) { return; }
        var point = event.latLng;
        zoneMapSetIndex(self.zoneIndex,false);
        _zoneReset();
    });

};

//PointRadiusGeozone.prototype.getCenter = function()
//{
//    return this.centerPoint; // GLatLng
//};

PointRadiusGeozone.prototype.getRadiusMeters = function()
{
    return this.radiusMeters;
};

PointRadiusGeozone.prototype.remove = function()
{
    if (this.radiusMarker != null) {
        this.radiusMarker.setMap(null);
    }
    if (this.centerMarker != null) {
        this.centerMarker.setMap(null);
    }
    if (this.circlePolygon != null) {
        this.circlePolygon.setMap(null);
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

function PolygonGeozone(gMap, points/*JSMapPoint[]*/, color, edit)
{
    var self = this;

    /* polygon attributes */
    this.googleMap        = gMap;
    this.verticeMarkers   = [];
    this.centerMarker     = null;
    this.shapeColor       = (color && (color != ""))? color : "#0000FF";
    this.polyBounds       = null;
    this.polygon          = null;
    this.editable         = edit;

    /* create vertices */
    var count  = 0;
    var bounds = jsNewGLatLngBounds();
    for (var i = 0; i < points.length; i++) {
        var p = points[i]; // JSMapPoint
        if (!geoIsValid(p.lat,p.lon)) { continue; }

        /* vertice Icon/marker */
        var vertPoint = jsNewGLatLng(p.lat, p.lon);
        var vertPushpin = (count == 0)? 
            "http://labs.google.com/ridefinder/images/mm_20_green.png" :
            "http://labs.google.com/ridefinder/images/mm_20_blue.png";
        var vertMarker = jsNewImageMarker(
            vertPoint,                                                      // GLatLng
            vertPushpin,                                                    // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            this.editable,                                                  // draggable
            -1                                                              // zIndex
            );
        vertMarker.pointIndex = i;
        vertMarker.isEditable = this.editable;
        this.verticeMarkers.push(vertMarker);
        bounds.extend(vertPoint);
        if (geoIsValid(p.lat,p.lon)) {
            vertMarker.setMap(this.googleMap);
            vertMarker.isVisible = true;     // polygon vertice
            vertMarker.isValid   = true;
            count++;
        } else {
            vertMarker.isVisible = false;    // polygon vertice
            vertMarker.isValid   = false;
        }

    }

    /* editable? */
    if (this.editable) {

        /* enable vertice dragging */
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            this._polygonVerticeDrag(this.verticeMarkers[i], i);
        }

        /* center point */
        var center = bounds.getCenter();
        this.centerMarker = jsNewImageMarker(
            center,                                                         // GLatLng
            "http://labs.google.com/ridefinder/images/mm_20_red.png",       // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            true,                                                           // draggable
            -1                                                              // zIndex
            );
        this.centerMarker.lastPoint = center;
        this.centerMarker.setDraggable(true);
        google.maps.event.addListener(this.centerMarker, "dragend", function(event) {
            var thisPoint = self.centerMarker.getPosition(); // GLatLng
            var lastPoint = self.centerMarker.lastPoint;  // GLatLng
            var deltaLat  = thisPoint.lat() - lastPoint.lat();
            var deltaLon  = thisPoint.lng() - lastPoint.lng();
            for (var i = 0; i < self.verticeMarkers.length; i++) {
                var vm  = self.verticeMarkers[i]; // G
                var vpt = vm.getPosition();
                if (geoIsValid(vpt.lat(),vpt.lng())) {
                    var npt = jsNewGLatLng(vpt.lat() + deltaLat, vpt.lng() + deltaLon);
                    vm.setPosition(npt);
                    _jsmSetPointZoneValue(vm.pointIndex, npt.lat(), npt.lng(), 0);
                } else {
                    _jsmSetPointZoneValue(vm.pointIndex, 0.0, 0.0, 0);
                }
            }
            self.centerMarker.lastPoint = thisPoint;
            self.drawPolygon();
        });
        if (count > 0) {
            this.centerMarker.setMap(this.googleMap);
            this.centerMarker.isVisible = true;     // polygon center
        } else {
            this.centerMarker.isVisible = false;    // polygon center
        }
        
    }

    /* draw polygon */
    this.drawPolygon();

};

PolygonGeozone.prototype._polygonVerticeDrag = function(marker, iNdx)
{
    // due to JavaScript closure rules, this must be in a separate function
    var self = this;
    var vertMarker = marker;
    vertMarker.setDraggable(true);
    google.maps.event.addListener(vertMarker, "dragend", function(event) {
        var vNdx  = vertMarker.pointIndex;
        var point = vertMarker.getPosition();
        zoneMapSetIndex(vNdx, false);
        _jsmSetPointZoneValue(vNdx, point.lat(), point.lng(), 0);
        self.drawPolygon(); 
        self.polyBounds = jsNewGLatLngBounds();
        for (var x = 0; x < self.verticeMarkers.length; x++) {
            var vpt = self.verticeMarkers[x].getPosition();
            if (geoIsValid(vpt.lat(),vpt.lng())) {
                self.polyBounds.extend(vpt);
            }
        }
        var polyCenter = self.polyBounds.getCenter(); // GLatLng
        self.centerMarker.setPosition(polyCenter);
        self.centerMarker.lastPoint = polyCenter;
    });
};

PolygonGeozone.prototype.type = function()
{
    return ZONE_POLYGON;
};

PolygonGeozone.prototype.drawPolygon = function()
{

    /* points */
    var points = []; // GLatLng[]
    var bounds = jsNewGLatLngBounds();
    if (this.verticeMarkers.length > 0) {
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            var vm  = this.verticeMarkers[i];
            var vpt = vm.getPosition(); 
            if (vm.isVisible) { // polygon vertice
                vm.setMap(null);
                vm.isVisible = false;  // polygon vertice
            }
            if (geoIsValid(vpt.lat(),vpt.lng())) {
                if (vm.isEditable) {
                    vm.setMap(this.googleMap);
                    vm.isVisible = true;  // polygon vertice
                }
                points.push(vpt);  // GLatLng
                bounds.extend(vpt);
            }
        }
        if (points.length > 0) {
            points.push(points[0]); // GLatLng close polygon
        }
    }

    /* center marker */
    if (this.centerMarker != null) {
        var center = bounds.getCenter();
        if (points.length > 0) {
            this.centerMarker.setPosition(center);
            this.centerMarker.lastPoint = center;
            if (!this.centerMarker.isVisible) {  // polygon center
                this.centerMarker.setMap(this.googleMap);
                this.centerMarker.isVisible = true; // polygon center
            }
        } else {
            if (this.centerMarker.isVisible) { // polygon center
                this.centerMarker.setMap(null);
                this.centerMarker.isVisible = false; // polygon center
            }
        }
    }

    /* draw polygon */
    if (points.length >= 3) {
        if (this.polygon == null) {
            var color = this.shapeColor;
            this.polygon = jsNewGPolygon(points, color, 2, 0.9, color, 0.1);
        } else {
            this.polygon.setPath(points);
        }
        this.polygon.setMap(this.googleMap);
    } else 
    if (this.polygon != null) {
        this.polygon.setMap(null);
        this.polygon = null;
    }

};

PolygonGeozone.prototype.remove = function()
{
    if (this.centerMarker != null) { // polygon center
        this.centerMarker.setMap(null);
        this.centerMarker.isVisible = false;
    }
    if (this.verticeMarkers != null) {
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            this.verticeMarkers[i].setMap(null);
            this.verticeMarkers[i].isVisible = false;
        }
    }
    if (this.polygon != null) {
        this.polygon.setMap(null);
    }
};

//PointRadiusGeozone.prototype.getCenter = function()
//{
//    return this.centerMarker.getPosition();  // GLatLng
//};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

function CorridorGeozone(gMap, points, radiusM, color, editable)
{
    var self = this;

    /* circle attributes */
    this.googleMap        = gMap;
    this.radiusMeters     = (radiusM <= MAX_ZONE_RADIUS_M)? Math.round(radiusM) : MAX_ZONE_RADIUS_M;
    this.verticeMarkers   = []; // GMarker[]
    this.shapeColor       = (color && (color != ""))? color : "#0000FF";
    this.corridor         = [];
    this.corrBounds       = null;

    /* create vertices */
    var count  = 0;
    var bounds = jsNewGLatLngBounds();
    for (var i = 0; i < points.length; i++) {
        var p = points[i]; // JSMapPoint
        if (!geoIsValid(p.lat,p.lon)) { continue; }

        /* vertice Icon/marker */
        var vertPoint  = jsNewGLatLng(p.lat, p.lon);
        var vertMarker = jsNewImageMarker(
            vertPoint,                                                      // GLatLng
            "http://labs.google.com/ridefinder/images/mm_20_blue.png",      // image
            jsNewGSize(12,20),                                              // iconSize
            jsNewGPoint(6,20),                                              // iconAnchor
            "http://labs.google.com/ridefinder/images/mm_20_shadow.png",    // shadow
            jsNewGSize(22,20),                                              // shadowSize
            null,                                                           // infoWindowAnchor
            editable,                                                       // draggable
            -1                                                              // zIndex
            );
        vertMarker.pointIndex = i;
        this.verticeMarkers.push(vertMarker); // GMarker
        bounds.extend(vertPoint);
        if (geoIsValid(p.lat,p.lon)) {
            vertMarker.setMap(this.googleMap); // this.googleMap.addOverlay(vertMarker);
            vertMarker.isVisible = true; // corridor vertice
            vertMarker.isValid   = true;
            count++;
        } else {
            vertMarker.isVisible = false; // corridor vertice
            vertMarker.isValid   = false;
        }

    }

    /* editable? */
    if (editable) {

        /* enable vertice dragging */
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            this._corridorVerticeDrag(this.verticeMarkers[i], i);
        }
        
    }
    
    /* draw corridor */
    this.drawCorridor();

};

CorridorGeozone.prototype.type = function()
{
    return ZONE_SWEPT_POINT_RADIUS;
};

CorridorGeozone.prototype._corridorVerticeDrag = function(marker, iNdx)
{
    var self = this;
    var vertMarker = marker;
    vertMarker.setDraggable(true);
    google.maps.event.addListener(vertMarker, "dragend", function(event) {
        var vNdx  = vertMarker.pointIndex;
        var point = vertMarker.getPosition();  // GLatLng
        zoneMapSetIndex(vNdx, false);
        _jsmSetPointZoneValue(vNdx, point.lat(), point.lng(), self.radiusMeters);
        self.drawCorridor(); 
        //self.corrBounds = jsNewGLatLngBounds();
        //for (var i = 0; i < self.verticeMarkers.length; i++) {
        //    var vpt = self.verticeMarkers[i].getPosition(); 
        //    if (geoIsValid(vpt.lat(),vpt.lng())) {
        //        self.corrBounds.extend(vpt);
        //    }
        //}
        // --
        //self.centerMarker.setPosition(self.corrBounds.getCenter()); // .setPoint(self.corrBounds.getCenter());
        //self.centerMarker.lastPoint = self.corrBounds.getCenter();
    });
};

CorridorGeozone.prototype.drawCorridor = function()
{

    /* remove old corridor */
    if (this.corridor != null) {
        for (var i = 0; i < this.corridor.length; i++) {
            this.corridor[i].setMap(null);
        };
    }
    this.corridor = [];

    /* vertices */
    var points = [];
    var bounds = jsNewGLatLngBounds();
    if (this.verticeMarkers.length > 0) {
        var lastPT = null;
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            var vm     = this.verticeMarkers[i];
            var vPT    = vm.getPosition(); // vertice GLatLng
            var radM   = this.radiusMeters;

            /* draw vertice circle */
            var circle = jsNewGCircle(vPT, radM, this.shapeColor, 1, 0.9, this.shapeColor, 0.1);
            this.corridor.push(circle);
            circle.setMap(this.googleMap); // this.googleMap.addOverlay(circlePoly);
            bounds.extend(vPT);
            
            /* draw connecting corridor */
            if (lastPT != null) {
                var ptA = lastPT; // GLatLng
                var ptB = vPT;    // GLatLng
                var hAB = geoHeading(ptA.lat(), ptA.lng(), ptB.lat(), ptB.lng()) - 90.0; // perpendicular
                var rp1 = geoRadiusPoint(ptA.lat(), ptA.lng(), radM, hAB        ); // JSMapPoint
                var rp2 = geoRadiusPoint(ptB.lat(), ptB.lng(), radM, hAB        ); // JSMapPoint
                var rp3 = geoRadiusPoint(ptB.lat(), ptB.lng(), radM, hAB + 180.0); // JSMapPoint
                var rp4 = geoRadiusPoint(ptA.lat(), ptA.lng(), radM, hAB + 180.0); // JSMapPoint
                var rectPts = [];
                rectPts.push(jsNewGLatLng(rp1.lat,rp1.lon));
                rectPts.push(jsNewGLatLng(rp2.lat,rp2.lon));
                rectPts.push(jsNewGLatLng(rp3.lat,rp3.lon));
                rectPts.push(jsNewGLatLng(rp4.lat,rp4.lon));
                rectPts.push(jsNewGLatLng(rp1.lat,rp1.lon));
                var rectPoly = jsNewGPolygon(rectPts, this.shapeColor, 1, 0.9, this.shapeColor, 0.1);
                this.corridor.push(rectPoly);
                rectPoly.setMap(this.googleMap); // this.googleMap.addOverlay(rectPoly);
            }
            lastPT = vPT; // GLatLng
            
        }
    }

};

CorridorGeozone.prototype.remove = function()
{
    if (this.verticeMarkers != null) {
        for (var i = 0; i < this.verticeMarkers.length; i++) {
            if (this.verticeMarkers[i].isVisible) { // corridor vertice
                this.verticeMarkers[i].setMap(null);
                this.verticeMarkers[i].isVisible = false;
            }
        }
    }
    if (this.corridor != null) {
        for (var i = 0; i < this.corridor.length; i++) {
            this.corridor[i].setMap(null);
        }
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
