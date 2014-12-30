--------------------------------------------------------------------------------------
Description: Mapstraction support
--------------------------------------------------------------------------------------

Download/Unzip the Mapstraction support JavaScript into this directory:
  1) Download "mxn-2.0.18.zip" from "http://www.mapstraction.com".
  2) Unzip the contents of the file into this directory.
  3) Configure "private.xml" to use Mapstraction as the MapProvider.
  4) Rebuild/Redeploy the "track.war" file.

Mapstraction has been tested for the GTS with the following map providers:
  - "googlev3"   : Google V3 api
  - "openlayers" : OpenLayers
The GTS may work with the other Mapstraction supported map providers, however full
testing may not be complete.  See "src/org/opengts/war/maps/jsmap/Mapstraction.java"
for additional information on supported map providers.
