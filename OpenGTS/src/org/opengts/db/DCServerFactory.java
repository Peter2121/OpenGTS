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
//  Device Communication Server configuration factory
// ----------------------------------------------------------------------------
// Change History:
//  2009/09/23  Martin D. Flynn
//     -Initial release
//  2009/11/01  Martin D. Flynn
//     - Discard duplicate server names (first entry encountered is saved)
//  2010/01/29  Martin D. Flynn
//     - "DCServer" and "Include" tags are now processed sequentially
//     - Added ability to query RTKey.MAIN_CLASS for a specific DCServer to load.
//  2010/05/24  Martin D. Flynn
//     -Added check for memory usage ("checkMemoryUsage")
//  2010/06/17  Martin D. Flynn
//     -Added "getServerConfigDescription"
//  2011/01/28  Martin D. Flynn
//     -Added 'data' argument to "UnassignedDevices.add(...)" method invocation.
//  2011/05/13  Martin D. Flynn
//     -Added "CONFIG_maximumHDOP"
//  2011/07/15  Martin D. Flynn
//     -Added runtime config property override check for "Attributes", 
//      "Properties", and "Commands".
//  2011/08/21  Martin D. Flynn
//     -Added "DCServer." prefix to various property lookups.
//     -Added ability to set Command State bit mask (see TAG_State)
//     -Added "CONFIG_ignoreDeviceOdometer"
//  2013/11/11  Martin D. Flynn
//     -Added "CONFIG_maximumAccuracyMeters"
//     -Added parsing "ConfigProperties" tag
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public class DCServerFactory
{

    // ------------------------------------------------------------------------

    public  static final String  DCSERVERS_DIR                  = "dcservers";
    public  static final String  DCSERVER_XML                   = "dcservers.xml";
    
    private static       File    LoadedDCServerXMLFile          = null;

    // ------------------------------------------------------------------------
    
    public  static       boolean DEFAULT_WARN_PORT_CONFLICT     = true;
    
    public  static       String  DCSNAME_SERVLET_PREFIX         = "w-";

    // ------------------------------------------------------------------------
    // Attribute properties
    
    public  static final String  PROP_Attribute_                = "Attribute.";
    public  static final String  PROP_Attribute_InputOffset     = PROP_Attribute_ + "inputOffset";
    public  static final String  PROP_Attribute_OutputOffset    = PROP_Attribute_ + "outputOffset";

    // ------------------------------------------------------------------------

    public  static final String  PROP_DCServer_                 = "DCServer.";
    public  static final String  PROP_dcs_                      = "dcs.";
    public  static final String  PROP_Command_                  = "Command.";
    public  static final String  PROP_command_                  = "command.";
    public  static final String  PROP_Properties_               = "Properties.";

    // -------

    /**
    *** Runtime Configuration Property<br>
    *** The specific DCServer name to load (all others will be ignored).<br>
    *** Type: DCServer name
    **/
    public  static final String PROP_DCServer_name              = "DCServer.name";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Command types:
    
    public  static final String  CMDTYPE_ALL                    = "all";
    public  static final String  CMDTYPE_MAP                    = "map";
    public  static final String  CMDTYPE_ADMIN                  = "admin";
    public  static final String  CMDTYPE_GARMIN                 = "garmin";
    public  static final String  CMDTYPE_SYSADMIN               = "sysadmin";
    
    public static boolean isCommandTypeAll(String type)
    {
        if (StringTools.isBlank(type)) {
            return true;
        } else {
            return type.equalsIgnoreCase(CMDTYPE_ALL);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Registry of Device Communication Server IDs 
    // Device communication servers listed here must also have a corresponding
    // "<server>.jar" file in the OpenGTS "build/lib/" directory in order to
    // be supported.  If this "<server>.jar" file does not exist, then the
    // corresponding device communication server port entry is not used.

    public static final String  NONE_NAME                       = "none";           // reserved for "No Server"

    /* OpenGTS */
    // already supported by the open-source OpenGTS
    public static final String  OPENDMTP_NAME                   = "gtsdmtp";        // [31000,3x100,3x200] OpenDMTP
    public static final String  ASTRA_NAME                      = "astra";          // [31090] Astra
    public static final String  LANTRIX_NAME                    = "lantrix";        // [31097] Lantrix T1800/T2000
    public static final String  ICARE_NAME                      = "icare";          // [31160] GX3300
    public static final String  SIPGEAR_NAME                    = "sipgear";        // [31170] 3x170
    public static final String  TEMPLATE_NAME                   = "template";       // [31200]
    public static final String  ASPICORE_NAME                   = "aspicore";       // [31265] 3x265
    public static final String  TK10X_NAME                      = "tk10x";          // [31272] 3x272
    public static final String  TAIP_NAME                       = "taip";           // [31275] 3x275

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Garmin codes

    /* return Garmin description for "Stop Status" */
    public static String Garmin_getStopStatusDescription(Locale locale, int stopStatus)
    {
        I18N i18n = I18N.getI18N(DCServerFactory.class, locale);
        switch (stopStatus) {
            case 100: // Active
                return i18n.getString("DCServerFactory.garminStopStatus.active"         , "Active");
            case 101: // Done
                return i18n.getString("DCServerFactory.garminStopStatus.done"           , "Done");
            case 102: // Unread Inactive
                return i18n.getString("DCServerFactory.garminStopStatus.unreadInactive" , "Unread Inactive");
            case 103: // Read Inactive
                return i18n.getString("DCServerFactory.garminStopStatus.readInactive"   , "Read Inactive");
            case 104: // Deleted
                return i18n.getString("DCServerFactory.garminStopStatus.deleted"        , "Deleted");
            default :
                return i18n.getString("DCServerFactory.garminStopStatus.unknown"        , "Unknown");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This DCServerFactory will check the class returned from "RTConfig.getProperty(RTKey.MAIN_CLASS,null)"
    // to see if it reaponds to the method "DCServerFactory_LoadName".  If it does, it calls this method
    // to see if there is a specific DCServer which it is to load exclusively.  Compliant DCServer
    // implementations should include a static method like the following:
    //   public static String DCServerFactory_LoadName() { return DCServerFactory.SERVER_NAME; }
    // In the absense of this method, or if this method return null/blank, then all available DCServers
    // will be loaded.
    
    private static final String StaticMethod_LoadName           = "DCServerFactory_LoadName";
    
    // ------------------------------------------------------------------------

    private static       String LoadServerNameOnly              = null;

    /**
    *** Gets the specific DCServerConfig name, or null if there is no specific DCServerConfig.
    *** @return  The name of the specific DCServerConfig entry
    **/
    public static String GetSpecificDCServerName()
    {
        return LoadServerNameOnly; // may be null/blank
    }
   
    /**
    *** Returns true if a specific DCServerConfig name is defined.
    *** @return True if a specific DCServerConfig name is defined.
    **/
    public static boolean HasSpecificDCServerName()
    {
        return !StringTools.isBlank(LoadServerNameOnly);
    }

    /**
    *** Sets the specific DCServerConfig name
    *** @param name  The name of the DCServerConfig entry
    **/
    public static void SetSpecificDCServerName(String name)
    {
        String n = StringTools.trim(name);
        LoadServerNameOnly = (StringTools.isBlank(n) || n.equals("*"))? null : n;
    }

    /**
    *** Initialize the specific DCServerConfig name
    **/
    public static void InitSpecificDCServerName()
    {

        /* already initialized? */
        if (DCServerFactory.HasSpecificDCServerName()) {
            return;
        }

        /* check runtime property */
        String dcsName = RTConfig.getString(DCServerFactory.PROP_DCServer_name,null);
        if (!StringTools.isBlank(dcsName)) {
            DCServerFactory.SetSpecificDCServerName(dcsName);
            return;
        }

        /* check main class for specific name */
        Object mainClass = RTConfig.getProperty(RTKey.MAIN_CLASS, null);
        if (mainClass instanceof Class) {
            //Print.logInfo("Checking main class for DCServer: " + StringTools.className(mainClass));
            try {
                MethodAction ma = new MethodAction((Class)mainClass, StaticMethod_LoadName);
                Object rtn = ma.invoke();
                if (rtn == null) {
                    // skip
                } else
                if (rtn instanceof String[]) {
                    String dcsn[] = (String[])rtn;
                    if (!ListTools.isEmpty(dcsn)) {
                        DCServerFactory.SetSpecificDCServerName(dcsn[0]);
                        return;
                    }
                } else
                if (rtn instanceof String) {
                    DCServerFactory.SetSpecificDCServerName((String)rtn);
                    return;
                } else {
                    Print.logInfo("Unrecognized return type: " + StringTools.className(rtn));
                }
            } catch (Throwable th) {
                // skip
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified port is valid
    *** @param port The port to validate
    *** @return True if the specified port is valid
    **/
    public static boolean isValidPort(int port)
    {
        return ((port > 0) && (port <= 65535));
    }

    /**
    *** Returns true if the specified port array is valid
    *** @param port The ports to validate
    *** @return True if the specified port array is valid
    **/
    public static boolean isValidPort(int port[])
    {
        if (ListTools.isEmpty(port)) {
            return false;
        } else {
            for (int i = 0; i < port.length; i++) {
                if (!DCServerFactory.isValidPort(port[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static File getServerJarFile(String name)
    {
        if (StringTools.isBlank(name)) {
            return null;
        } else {
            String gtsHome = !StringTools.isBlank(GTS_HOME)? GTS_HOME : ".";
            String jarPath = gtsHome + File.separator + "build" + File.separator + "lib" + File.separator;
            File serverJar = new File(jarPath + name + ".jar");
            return serverJar.isFile()? serverJar : null;
        }
    }

    /**
    *** Returns true if the named server is defined
    **/
    public static boolean serverJarExists(String name)
    {
        File serverJar = DCServerFactory.getServerJarFile(name);
        if (serverJar != null) {
            return true;
        } else {
            if (StringTools.isBlank(GTS_HOME)) {
                //Print.logWarn("GTS_HOME not defined (unable to find "+serverJar+")");
            } else {
                //Print.logDebug("Missing " + serverJar);
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  CMDARG_ACCOUNT                  = "account";
    public static final String  CMDARG_DEVICE                   = "device";
    public static final String  CMDARG_USER                     = "user";
    public static final String  CMDARG_UNIQUE                   = "unique";
    public static final String  CMDARG_CMDTYPE                  = "cmdtype";
    public static final String  CMDARG_CMDNAME                  = "cmdname";
    public static final String  CMDARG_ARG                      = DCServerConfig.DEFAULT_ARG_NAME;
    public static final String  CMDARG_SERVER                   = "server";

    public static final String  RESPONSE_SERVER                 = CMDARG_SERVER;
    public static final String  RESPONSE_RESULT                 = "result";
    public static final String  RESPONSE_MESSAGE                = "message";

    /**
    *** ResultCode enumeration for server command responses
    **/
    public enum ResultCode implements EnumTools.StringLocale, EnumTools.StringValue {
        SUCCESS          ("OK000",I18N.getString(DCServerFactory.class,"DCServerFactory.result.successful"      ,"Successful")),
        COMMAND_QUEUED   ("OK001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.commandQueued"   ,"Command Queued")),
        INVALID_ACCOUNT  ("AC001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidAccount"  ,"Invalid Account")),
        INVALID_DEVICE   ("DV001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidDevice"   ,"Invalid Device")),
        INVALID_SERVER   ("SR001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidServer"   ,"Invalid Server")),
        NOT_AUTHORIZED   ("AU001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.notAuthorized"   ,"Not Authorized")),
        OVER_LIMIT       ("AU002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.overLimit"       ,"Over Limit")),
        INVALID_COMMAND  ("CM001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidCommand"  ,"Invalid command")),
        INVALID_ARG      ("CM002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidArgument" ,"Invalid command/argument")),
        INVALID_TYPE     ("CM003",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidType"     ,"Invalid command type")),
        EMPTY_REQUEST    ("CM004",I18N.getString(DCServerFactory.class,"DCServerFactory.result.emptyRequest"    ,"Imvalid/Empty request")),
        NOT_SUPPORTED    ("CM005",I18N.getString(DCServerFactory.class,"DCServerFactory.result.notSupported"    ,"Not Supported by Device")),
        UNKNOWN_HOST     ("HP001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidHost"     ,"Invalid host")),
        TRANSMIT_FAIL    ("TX001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.transmitFail"    ,"Transmit failure")),
        NO_SESSION       ("TX002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.noActiveSession" ,"No Active Session Found")),
        OFFLINE          ("TX007",I18N.getString(DCServerFactory.class,"DCServerFactory.result.deviceOffline"   ,"Device Offline")),
        AGED_ROUTE       ("TX011",I18N.getString(DCServerFactory.class,"DCServerFactory.result.agedRoute"       ,"Return route too old (UDP)")),
        INVALID_PROTO    ("PR001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidProtocol" ,"Invalid Protocol")),
        INVALID_SMS      ("PR002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidSmsSpec"  ,"Invalid SMS specification")),
        INVALID_PACKET   ("PK001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidPacket"   ,"Invalid Packet")),
        INVALID_EMAIL_FR ("EM001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidEMailFrom","Invalid EMail 'From' address")),
        INVALID_EMAIL_TO ("EM002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidEMailTo"  ,"Invalid EMail 'To' address")),
        INTERNAL_ERROR   ("XX001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.internalError"   ,"Internal Error")),
        GATEWAY_ERROR    ("GW001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayError"    ,"Gateway Error")),
        GATEWAY_CONFIG   ("GW010",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayConfig"   ,"Gateway Config")),
        GATEWAY_ACCOUNT  ("GW011",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayAccount"  ,"Gateway Account")),
        GATEWAY_USER     ("GW012",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayUser"     ,"Gateway User")),
        GATEWAY_DEVICE   ("GW013",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayDevice"   ,"Gateway Device")),
        GATEWAY_HOST     ("GW014",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayHost"     ,"Gateway Host")),
        GATEWAY_PORT     ("GW015",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayPort"     ,"Gateway Port")),
        GATEWAY_CONNECT  ("GW021",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayConnect"  ,"Gateway Connect")),
        GATEWAY_AUTH     ("GW031",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayAuth"     ,"Gateway Authentication Failed")),
        GATEWAY_SERVICE  ("GW032",I18N.getString(DCServerFactory.class,"DCServerFactory.result.gatewayService"  ,"Gateway Service Failed")),
        UNKNOWN          ("UN000",I18N.getString(DCServerFactory.class,"DCServerFactory.result.unknownResult"   ,"Unknown Result"));
        // ---
        private String      cc = null;
        private I18N.Text   aa = null;
        ResultCode(String c, I18N.Text a)           { cc = c; aa = a; }
        public String  getStringValue()             { return cc; }
        public String  getCode()                    { return cc; }
        public String  getMessage()                 { return aa.toString(); }
        public String  getMessage(Locale loc)       { return aa.toString(loc); }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(SUCCESS); }
        public boolean isSuccess()                  { return this.equals(SUCCESS) || this.equals(COMMAND_QUEUED); }
    };

    /**
    *** Gets the ResultCode for the specified code value.
    *** @param code  The name of the result code
    *** @param dft   The default ResultCode to return if the specified code is undefined.
    *** @return The ResultCode instance
    **/
    public static ResultCode GetResultCode(String code, ResultCode dft)
    {
        if (StringTools.isBlank(code)) {
            return dft;
        } else {
            return EnumTools.getValueOf(ResultCode.class,code,dft);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String TAG_DCServerConfig      = "DCServerConfig";
    private static final String TAG_DCServer            = "DCServer";
    private static final String TAG_Description         = "Description";
    private static final String TAG_ModelNames          = "ModelNames";
    private static final String TAG_Attributes          = "Attributes";
    private static final String TAG_UniqueIDPrefix      = "UniqueIDPrefix";
    private static final String TAG_GlobalProperties    = "GlobalProperties";

    private static final String TAG_Properties          = "Properties";
    private static final String TAG_Property            = "Property";

    private static final String TAG_ConfigProperties    = "ConfigProperties";

    private static final String TAG_EventCodeMap        = "EventCodeMap";
    private static final String TAG_Code                = "Code";

    private static final String TAG_Commands            = "Commands";
    private static final String TAG_Command             = "Command";
    private static final String TAG_Type                = "Type";
    private static final String TAG_AclName             = "AclName";
    private static final String TAG_String              = "String";
    private static final String TAG_StatusCode          = "StatusCode";
    private static final String TAG_ExpectAckCode       = "ExpectAckCode";
    private static final String TAG_ListenPorts         = "ListenPorts";
    private static final String TAG_Include             = "Include";
    private static final String TAG_Arg                 = "Arg";
    private static final String TAG_State               = "State";

    private static final String ATTR_remoteLogging      = "remoteLogging";
    public  static final String ATTR_bindAddress        = "bindAddress";
    private static final String ATTR_backlog            = "backlog";
  //private static final String ATTR_portOffset         = "portOffset";
    private static final String ATTR_name               = "name";
    private static final String ATTR_save               = "save";
    private static final String ATTR_readOnly           = "readOnly";
    private static final String ATTR_id                 = "id";
    private static final String ATTR_key                = "key";
    private static final String ATTR_trim               = "trim";
    public  static final String ATTR_enabled            = "enabled";
    private static final String ATTR_dispatchHost       = "dispatchHost";
    private static final String ATTR_dispatchPort       = "dispatchPort";
    private static final String ATTR_ssl                = "ssl";
    private static final String ATTR_tcpPort            = "tcpPort";
    private static final String ATTR_udpPort            = "udpPort";
    private static final String ATTR_satPort            = "satPort";
    private static final String ATTR_active             = "active";
    private static final String ATTR_dir                = "dir";
    private static final String ATTR_file               = "file";
    private static final String ATTR_optional           = "optional";
    private static final String ATTR_data               = "data";
    private static final String ATTR_protocol           = "protocol";
    private static final String ATTR_udpMaxAge          = "udpMaxAge";
    private static final String ATTR_queue              = "queue";
    private static final String ATTR_arg                = "arg";
    private static final String ATTR_expectAck          = "expectAck";
    private static final String ATTR_acl                = "acl";
    private static final String ATTR_hasArgs            = "hasArgs";
    private static final String ATTR_includeDir         = "includeDir";
    private static final String ATTR_sessionVar         = "sessionVar";
    private static final String ATTR_defaultValue       = "defaultValue";
    private static final String ATTR_length             = "length";
    private static final String ATTR_warnPortConflict   = "warnPortConflict";
    private static final String ATTR_default            = "default";
    public  static final String ATTR_rtPropPrefix       = "rtPropPrefix";
    public  static final String ATTR_mask               = "mask";
    public  static final String ATTR_index              = "index";

    /**
    *** Gets the full path name of the 'dcservers.xml' file based on the location
    *** of the loaded rutime configuration file.
    **/
    private static File _getDCServerXMLFile()
    {
        File cfgFile = RTConfig.getLoadedConfigFile();
        if (cfgFile != null) {
            return new File(cfgFile.getParentFile(), DCSERVER_XML);
        } else {
            return null;
        }
    }

    /**
    *** Returns an XML Document for the 'dcservers.xml' config file 
    **/
    private static Document _getDocument(File xmlFile)
    {

        /* valid file specified? */
        if (xmlFile == null) {
            Print.logError("DCServer XML file not specified: " + xmlFile);
            return null;
        } else
        if (!xmlFile.exists()) {
            Print.logError("DCServer XML file does not exist: " + xmlFile);
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlFile);
        } catch (ParserConfigurationException pce) {
            Print.logException("Parse error: " + xmlFile, pce);
        } catch (SAXException se) {
            Print.logException("Parse error: " + xmlFile, se);
        } catch (IOException ioe) {
            Print.logException("Parse error: " + xmlFile, ioe);
        }
        
        /* return */
        return doc;
        
    }

    private static RTProperties                 GlobalProperties    = null;
    private static Map<Integer,DCServerConfig>  TCPPortMap          = null;
    private static Map<Integer,DCServerConfig>  UDPPortMap          = null;

    /**
    *** Load the 'dcservers.xml' file 
    *** @param xmlFile  The path/name of the 'dcservers.xml' file to load
    **/
    public static void loadDCServerXML(File xmlFile)
    {

        /* Global properties */
        GlobalProperties = new RTProperties();

        /* port conflict check */
        TCPPortMap = new OrderedMap<Integer,DCServerConfig>();
        UDPPortMap = new OrderedMap<Integer,DCServerConfig>();

        /* load XML */
        try {
            OrderedSet<DCServerConfig> dcsSet = DCServerFactory._loadDCServerXML(xmlFile, 0);
            if (!ListTools.isEmpty(dcsSet)) {
                for (DCServerConfig dcs : dcsSet) {
                    DCServerFactory.addDCS(dcs);
                }
            }
        } catch (Throwable t) {
            Print.logException("Unable to load DCServerFactory XML", t);
        }

        /* global properties */
        if (GlobalProperties != null) {
            //GlobalProperties.printProperties("Setting Global Properties:");
            RTConfig.setProperties(GlobalProperties);
        }

        /* list ports */
        /*
        for (Integer p : TCPPortMap.keySet()) {
            DCServerConfig dcs = TCPPortMap.get(p);
            Print.logInfo("TCP Port " + p + " ==> " + dcs.getName());
        }
        for (Integer p : UDPPortMap.keySet()) {
            DCServerConfig dcs = UDPPortMap.get(p);
            Print.logInfo("UDP Port " + p + " ==> " + dcs.getName());
        }
        */

        /* clear for garbage collection (no longer needed) */
        GlobalProperties = null;
        TCPPortMap = null;
        UDPPortMap = null;

    }

    /**
    *** Loads the 'dcservers.xml' file 
    **/
    private static OrderedSet<DCServerConfig> _loadDCServerXML(File xmlFile, int recurseLvl)
    {

        /* xml file specified? */
        if (xmlFile == null) {
            // get default file
            xmlFile = DCServerFactory._getDCServerXMLFile(); 
            // 'xmlFile' may still be null
        }

        /* save top-level loaded file name */
        if (recurseLvl == 0) {
            DCServerFactory.LoadedDCServerXMLFile = xmlFile;
        }

        /* look for specific DCServer name */
        if ((xmlFile == null) || !xmlFile.isFile()) {
            File origFile = xmlFile;

            /* look for the specific DCS name (if applicable) */
            File cfgDir = RTConfig.getLoadedConfigDir();
            String dcsName = DCServerFactory.GetSpecificDCServerName();
            if (!StringTools.isBlank(dcsName) && (cfgDir != null)) {
                String dcsFileName = "dcserver_" + dcsName + ".xml";
                xmlFile = FileTools.findFile(cfgDir, new String[] {
                    dcsFileName,
                    DCSERVERS_DIR + "/" + dcsFileName
                }); // may still return null if not found
                if ((xmlFile == null) && !dcsName.startsWith("w-")) {
                    Print.logWarn("DCServerConfig XML file not found: " + dcsFileName);
                }
            }

            /* if still not found, then display message and return */
            if ((xmlFile == null) || !xmlFile.isFile()) {
                if (RTConfig.isWebApp() && !BasicPrivateLabel.isTrackServlet()) {
                    // -- this is a servlet (but not "track.war")
                    Print.logInfo("DCServerConfig XML file not loaded: " + origFile);
                } else {
                    // -- this is either "track.war", or it is not a servlet
                    Print.logError("DCServerConfig XML file does not exist: " + origFile);
                }
                return null;
            }

        }

        /* get XML document */
        //Print.logDebug("Loading " + xmlFile);
        Document xmlDoc = DCServerFactory._getDocument(xmlFile);
        if (xmlDoc == null) {
            Print.logError("Unable to create DCServerConfig XML 'Document' (syntax errors?)");
            return null;
        }

        /* get top-level tag */
        Element dcsDef = xmlDoc.getDocumentElement();
        if (!dcsDef.getTagName().equalsIgnoreCase(TAG_DCServerConfig)) {
            Print.logError("["+xmlFile+"] Invalid root tag ID: " + dcsDef.getTagName());
            return null;
        }

        /* top-level attributes */
        if (recurseLvl == 0) {
            // -- global values
            BIND_ADDRESS   = XMLTools.getAttribute(   dcsDef, ATTR_bindAddress, BIND_ADDRESS  , true);
            BIND_ADDRESS   = StringTools.blankDefault(RTConfig.getString(ATTR_bindAddress,null),BIND_ADDRESS);
            LISTEN_BACKLOG = XMLTools.getAttributeInt(dcsDef, ATTR_backlog    , LISTEN_BACKLOG, true);
          //PORT_OFFSET    = XMLTools.getAttributeInt(dcsDef, ATTR_portOffset , PORT_OFFSET   , true);
            INCLUDE_DIR    = XMLTools.getAttribute(   dcsDef, ATTR_includeDir , INCLUDE_DIR   , true);
        } else {
            // -- local values
        }

        /* prepare for include: XML parent dir */
        File parentDir = xmlFile.getParentFile();
        if (parentDir != null) {
            try {
                File dir = parentDir.getCanonicalFile();
                parentDir = dir;
            } catch (Throwable th) {
                // 
            }
        }

        /* parse DCServerConfig tags */
        OrderedSet<DCServerConfig> dcserverSet = new OrderedSet<DCServerConfig>(true);
        Set<String> dcserverNames = new HashSet<String>();
        NodeList dcsChildNodes = dcsDef.getChildNodes();
        for (int dcsn = 0; dcsn < dcsChildNodes.getLength(); dcsn++) {

            /* get Node (only interested in 'Element's) */
            Node dcsChildNode = dcsChildNodes.item(dcsn);
            if (!(dcsChildNode instanceof Element)) {
                continue;
            }

            /* parse node */
            String dcsNodeName = dcsChildNode.getNodeName();
            Element dcsChildElem = (Element)dcsChildNode;
            if (dcsNodeName.equalsIgnoreCase(TAG_DCServer)) {
                Element dcsTag   = dcsChildElem;
                String  dcsName  = XMLTools.getAttribute(dcsTag,ATTR_name         ,null,false);
                String  dcsProto = XMLTools.getAttribute(dcsTag,ATTR_protocol     ,null,false);
                String  dcsRLog  = XMLTools.getAttribute(dcsTag,ATTR_remoteLogging,null,false);

                /* load specific DCServer only? */
                boolean setGlobalProperties = false;
                String specDCN = DCServerFactory.GetSpecificDCServerName();
                if (!StringTools.isBlank(specDCN)) {
                    // a specific DCServerConfig name has been specified
                    if (!specDCN.equals(dcsName)) {
                        // current DCServerConfig does NOT match name
                        //Print.logDebug("Skipping this DCServer: " + dcsName);
                        continue;
                    }
                    // current DCServerConfig DOES not match name
                    setGlobalProperties = true;
                }

                /* already added */
                if (dcserverNames.contains(dcsName)) {
                    Print.logInfo("Ignoring duplicate DCServer ["+recurseLvl+"]: " + dcsName);
                    continue;
                }
                dcserverNames.add(dcsName);

                /* active? */
                boolean active = XMLTools.getAttributeBoolean(dcsTag, ATTR_active, true, false);
                if (!active) {
                    Print.logDebug("Inactive DCServer ["+recurseLvl+"]: " + dcsName);
                    continue;
                }
                Print.logDebug("Parsing DCServer ["+recurseLvl+"]: " + dcsName);

                /* create new DCServerConfig */
                DCServerConfig dcs = new DCServerConfig();
                dcs.setName(dcsName);
                dcs.setConfigFile(xmlFile);
                dcs.setCommandProtocol(dcsProto);
                dcs.setRemoteLogging(dcsRLog);
                boolean warnPortConflict = DEFAULT_WARN_PORT_CONFLICT;
                NodeList childNodes = dcsTag.getChildNodes();
                for (int c = 0; c < childNodes.getLength(); c++) {
    
                    /* get Node (only interested in 'Element's) */
                    Node dcsNode = childNodes.item(c);
                    if (!(dcsNode instanceof Element)) {
                        continue;
                    }
    
                    /* parse nodes */
                    String nodeName = dcsNode.getNodeName();
                    Element dcsElem = (Element)dcsNode;

                    /* Description */
                    if (nodeName.equalsIgnoreCase(TAG_Description)) {
                        String desc = XMLTools.getNodeText(dcsElem," ",false);
                        //Print.logInfo("Old Description: " + dcsName + " - " + desc);
                        desc = RTConfig.getString(PROP_DCServer_ + dcsName + ".description", desc);
                        //Print.logInfo("New Description: " + dcsName + " - " + desc);
                        dcs.setDescription(desc);
                        continue;
                    }

                    /* ModelNames */
                    if (nodeName.equalsIgnoreCase(TAG_ModelNames)) {
                        String modelNames[] = StringTools.split(XMLTools.getNodeText(dcsElem,"\n",false),'\n');
                        // TODO:
                        continue;
                    }

                    /* Attributes */
                    if (nodeName.equalsIgnoreCase(TAG_Attributes)) {
                        RTProperties attr = new RTProperties(XMLTools.getNodeText(dcsElem," ",false));
                        long flagAttr = DCServerConfig.GetAttributeFlags(attr);
                        dcs.setAttributeFlags(flagAttr);
                        RTProperties rtProps = dcs.getDefaultProperties();
                        for (Object rtk : attr.getPropertyKeys()) {
                            Object rtv = attr.getProperty(rtk,null);
                            String attrKey = PROP_Attribute_ + rtk;
                            // check "DCServer.DCSNAME.Attribute.ATTRKEY" override
                            String dcsAttrKey[] = new String[] {
                                PROP_DCServer_ + dcsName + "." + attrKey, // DCServer.DCSNAME.Attribute.ATTRKEY
                                PROP_dcs_      + dcsName + "." + attrKey  // dcs.DCSNAME.Attribute.ATTRKEY
                            };
                            if (RTConfig.hasProperty(dcsAttrKey)) {
                                // override
                                rtv = RTConfig.getProperty(dcsAttrKey, rtv);
                            }
                            /* set Attribute key value */
                            rtProps.setProperty(attrKey, rtv);
                        }
                        continue;
                    }

                    /* UniqueIDPrefix */
                    if (nodeName.equalsIgnoreCase(TAG_UniqueIDPrefix)) {
                        String uidPfx = XMLTools.getNodeText(dcsElem,",",false);
                        dcs.setUniquePrefix(StringTools.parseStringArray(uidPfx,','));
                        continue;
                    }

                    /* ListenPorts */
                    if (nodeName.equalsIgnoreCase(TAG_ListenPorts)) {
                        warnPortConflict = XMLTools.getAttributeBoolean(dcsElem,ATTR_warnPortConflict,DEFAULT_WARN_PORT_CONFLICT,false);
                        InetAddress bindAddr = null;
                        String bindAddrName = XMLTools.getAttribute(dcsElem,ATTR_bindAddress,null,false);
                        try {
                            if (!StringTools.isBlank(bindAddrName)) {
                                bindAddr = InetAddress.getByName(bindAddrName);
                            }
                        } catch (Throwable th) {
                            Print.logException("Unable to locate Bind Address: " + bindAddrName, th);
                            bindAddr = null;
                        }
                        dcs.setUseSSL(XMLTools.getAttributeBoolean(dcsElem,ATTR_ssl,false));
                        dcs.setTcpPorts(bindAddr,DCServerFactory.parsePorts(XMLTools.getAttribute(dcsElem,ATTR_tcpPort,null,false)),true);
                        dcs.setUdpPorts(bindAddr,DCServerFactory.parsePorts(XMLTools.getAttribute(dcsElem,ATTR_udpPort,null,false)),true);
                        dcs.setSatPorts(bindAddr,DCServerFactory.parsePorts(XMLTools.getAttribute(dcsElem,ATTR_satPort,null,false)),true);
                        continue;
                    }

                    /* GlobalProperties */
                    if (nodeName.equalsIgnoreCase(TAG_GlobalProperties)) {
                        if (setGlobalProperties && (GlobalProperties != null)) {
                            RTProperties rtProps = GlobalProperties;
                            NodeList propList = XMLTools.getChildElements(dcsElem,TAG_Property);
                            for (int p = 0; p < propList.getLength(); p++) {
                                Node propNode = propList.item(p);
                                if (propNode instanceof Element) {
                                    Element propElem = (Element)propNode;
                                    String key = XMLTools.getAttribute(propElem,ATTR_key,null,false);
                                    boolean valTrim = XMLTools.getAttributeBoolean(propElem,ATTR_trim,true,false);
                                    if (!StringTools.isBlank(key)) {
                                        String val = StringTools.trim(XMLTools.getNodeText(propElem,(valTrim?" ":null),false));
                                        //Print.logInfo("Setting global property " + key + " ==> " + val);
                                        rtProps.setProperty(key, val);
                                    } else {
                                        Print.logWarn("Undefined Property key ignored.");
                                    }
                                }
                            }
                        }
                        continue;
                    }

                    /* Properties */
                    if (nodeName.equalsIgnoreCase(TAG_Properties)) {
                        // dcs, dcsElem, dcsName
                        String propID = XMLTools.getAttribute(dcsElem,ATTR_id,null,false);
                        RTProperties rtProps = dcs.getProperties(propID,true);
                        NodeList propList = XMLTools.getChildElements(dcsElem,TAG_Property);
                        for (int p = 0; p < propList.getLength(); p++) {
                            Node propNode = propList.item(p);
                            if (propNode instanceof Element) {
                                Element propElem = (Element)propNode;
                                String key = XMLTools.getAttribute(propElem,ATTR_key,null,false);
                                boolean valTrim = XMLTools.getAttributeBoolean(propElem,ATTR_trim,true,false);
                                if (!StringTools.isBlank(key)) {

                                    /* EG: convert "PROPKEY" to "DCSNAME.PROPKEY" */
                                    String dcsPropKey = null;
                                    if (key.startsWith(dcsName+".")) {
                                        dcsPropKey = key;
                                        key = key.substring((dcsName+".").length());
                                    } else {
                                        dcsPropKey = dcsName + "." + key;
                                    }

                                    /* get property value */
                                    String dcsPropVal = StringTools.trim(XMLTools.getNodeText(propElem,(valTrim?" ":null),false));

                                    /* check override: "DCServer.DCSNAME.Properties.PROPKEY" */
                                    String rtPropKey[] = new String[] {
                                        PROP_DCServer_ + dcsName + "."                    + key, // DCServer.DCSNAME.PROPKEY
                                        PROP_DCServer_ + dcsName + "." + PROP_Properties_ + key, // DCServer.DCSNAME.Properties.PROPKEY
                                        PROP_dcs_      + dcsName + "."                    + key, // dcs.DCSNAME.PROPKEY
                                        PROP_dcs_      + dcsName + "." + PROP_Properties_ + key, // DCServer.DCSNAME.Properties.PROPKEY
                                    };
                                    if (RTConfig.hasProperty(rtPropKey)) {
                                        // override
                                        dcsPropVal = RTConfig.getString(rtPropKey, dcsPropVal);
                                    }

                                    /* add to RTProperties */
                                    rtProps.setProperty(dcsPropKey, dcsPropVal);

                                } else {
                                    Print.logWarn("Undefined Property key ignored.");
                                }
                            }
                        }
                        continue;
                    }

                    /* EventCodeMap */
                    if (nodeName.equalsIgnoreCase(TAG_EventCodeMap)) {
                        // dcs, dcsElem, dcsName
                        boolean ecEnabled = XMLTools.getAttributeBoolean(dcsElem,ATTR_enabled,true/*dft*/,false/*rtp*/); // property key
                        Map<Object,DCServerConfig.EventCode> codeMap = new HashMap<Object,DCServerConfig.EventCode>();
                        NodeList codeList = XMLTools.getChildElements(dcsElem,TAG_Code);
                        for (int eci = 0; eci < codeList.getLength(); eci++) {
                            Node codeNode = codeList.item(eci);
                            if (codeNode instanceof Element) {
                                Element codeElem = (Element)codeNode;
                                String dataStr = XMLTools.getAttribute(codeElem,ATTR_data,null,false); // property key
                                String keyStr  = StringTools.trim(XMLTools.getAttribute(codeElem,ATTR_key,"",false));
                                if (!StringTools.isBlank(keyStr)) {
                                    int    keyInt  = StringTools.isInt(keyStr,true)? StringTools.parseInt(keyStr,-1) : -1;
                                    Object keyObj  = (keyInt >= 0)? new Integer(keyInt) : keyStr.toLowerCase();
                                    String valStr  = XMLTools.getNodeText(codeElem," ",false);
                                    int    sc      = StatusCodes.STATUS_IGNORE;
                                    if (StringTools.isBlank(valStr) || valStr.equalsIgnoreCase("ignore")) {
                                        sc = StatusCodes.STATUS_IGNORE;
                                    } else
                                    if (valStr.equalsIgnoreCase("default") || valStr.equalsIgnoreCase("none")) {
                                        sc = StatusCodes.STATUS_NONE;
                                    } else {
                                        int sci = StringTools.parseInt(valStr, StatusCodes.STATUS_IGNORE);
                                        if (sci < 0) {
                                            sc = StatusCodes.STATUS_IGNORE; // ignore
                                        } else
                                        if (sci == 0) {
                                            sc = StatusCodes.STATUS_NONE;
                                        } else {
                                            sc = sci;
                                        }
                                    }
                                    //Print.logDebug("Code translate " + keyStr + " ==> " + StatusCodes.GetHex(sc));
                                    codeMap.put(keyObj, new DCServerConfig.EventCode(keyInt,sc,dataStr));
                                } else {
                                    Print.logWarn("Invalid Code key ignored: " + keyStr);
                                }
                            }
                        }
                        dcs.setEventCodeEnabled(ecEnabled);
                        dcs.setEventCodeMap(codeMap);
                        continue;
                    }

                    /* Commands */
                    if (nodeName.equalsIgnoreCase(TAG_Commands)) {
                        // dcs, dcsElem, dcsName
                        String  cmdHost  = XMLTools.getAttribute(dcsElem, ATTR_dispatchHost,null,false);
                        String  cmdPortS = XMLTools.getAttribute(dcsElem, ATTR_dispatchPort,  "",false);
                        int     cmdPort  = "sms".equalsIgnoreCase(cmdPortS)? -1 : StringTools.parseInt(cmdPortS,-1);
                        dcs.setCommandDispatcherHost(cmdHost);
                        dcs.setCommandDispatcherPort(cmdPort,true); // also checks RTP override
                        cmdPort = dcs.getCommandDispatcherPort(); // update port (from overrides)
                        NodeList commandsNodes = dcsElem.getChildNodes();
                        for (int cmi = 0; cmi < commandsNodes.getLength(); cmi++) {
                            Node cmdNode = commandsNodes.item(cmi);
                            if (!(cmdNode instanceof Element)) { continue; }
                            Element cmdElem = (Element)cmdNode;
                            String cmdTagName = cmdNode.getNodeName();
                            if (cmdTagName.equalsIgnoreCase(TAG_AclName)) {
                                String aclName = XMLTools.getNodeText(cmdElem,"");
                                String aclDftStr = XMLTools.getAttribute(cmdElem,ATTR_default,null,false);
                                AclEntry.AccessLevel aclDft = AclEntry.parseAccessLevel(aclDftStr, AclEntry.AccessLevel.WRITE);
                                dcs.setCommandsAclName(aclName, aclDft);
                            } else
                            if (cmdTagName.equalsIgnoreCase(TAG_Command)) {
                                String  cmdName    = XMLTools.getAttribute(cmdElem,ATTR_name,null,false);
                                String  cmdEnableS = XMLTools.getAttribute(cmdElem,ATTR_enabled,""/*dft*/,false/*dft*/).toLowerCase();
                                boolean cmdEnable  = cmdEnableS.equals("")? true : StringTools.parseBoolean(cmdEnableS, false);
                                boolean cmdDiscard = !cmdEnable && !cmdEnableS.equals("hidden");
                              //boolean cmdEnable  = XMLTools.getAttributeBoolean(cmdElem,ATTR_enabled,true/*dft*/,false/*dft*/);
                                String enabledKey1 = PROP_Command_ + cmdName + "." + ATTR_enabled; // Command.CMDNAME.enabled
                                String enabledKey2 = PROP_command_ + cmdName + "." + ATTR_enabled; // command.CMDNAME.enabled
                                String rtPropKey[] = new String[] {
                                    PROP_DCServer_ + dcsName + "." + enabledKey1, // DCServer.DCSNAME.Command.CMDNAME.enabled
                                    PROP_DCServer_ + dcsName + "." + enabledKey2, // DCServer.DCSNAME.command.CMDNAME.enabled
                                    PROP_dcs_      + dcsName + "." + enabledKey1, // dcs.DCSNAME.Command.CMDNAME.enabled
                                    PROP_dcs_      + dcsName + "." + enabledKey2, // dcs.DCSNAME.command.CMDNAME.enabled
                                };
                                //Print.logInfo("Checking property: " + rtPropKey[0]);
                                if (BasicPrivateLabelLoader.SAVE_I18N_STRINGS) {
                                    // for LocalStrings_XX.properties generation
                                    cmdEnable  = true;
                                    cmdDiscard = false;
                                    //Print.logInfo("Enable Command for LocalStrings_X.properties generation");
                                } else
                                if (RTConfig.hasProperty(rtPropKey)) {
                                    cmdEnable = RTConfig.getBoolean(rtPropKey, cmdEnable);
                                    if (cmdEnable) { cmdDiscard = false; }
                                    //Print.logInfo("Overriding Command: " + rtPropKey[0] + " ==> " + cmdEnable);
                                } else
                                if (dcs.getDefaultProperties().hasProperty(enabledKey1)) {
                                    // Local Props  : "Command.CMDNAME.enabled"
                                    cmdEnable = dcs.getDefaultProperties().getBoolean(enabledKey1, cmdEnable);
                                    if (cmdEnable) { cmdDiscard = false; }
                                } else
                                if (dcs.getDefaultProperties().hasProperty(enabledKey2)) {
                                    // Local Props  : "command.CMDNAME.enabled"
                                    cmdEnable = dcs.getDefaultProperties().getBoolean(enabledKey2, cmdEnable);
                                    if (cmdEnable) { cmdDiscard = false; }
                                }
                                if (cmdEnable || !cmdDiscard) {
                                    boolean hasArgs     = XMLTools.getAttributeBoolean(cmdElem,ATTR_hasArgs,false,false);
                                    boolean expectAck   = XMLTools.getAttributeBoolean(cmdElem,ATTR_expectAck,false,false);
                                    String  aclName     = XMLTools.getAttribute(cmdElem,ATTR_acl,null,false);
                                    String  cmdTypes[]  = null;
                                    String  cmdDesc     = null;
                                    String  cmdAclName  = !StringTools.isBlank(aclName)? aclName : AclEntry.CreateAclName(dcs.getCommandsAclName(),cmdName);
                                    AclEntry.AccessLevel cmdAclDft = AclEntry.AccessLevel.WRITE;
                                    String  cmdString   = "";
                                    String  cmdProto    = "";
                                    long    maxUdpAge   = -1L;
                                    boolean allowQueue  = false;
                                    int     cmdSCode    = StatusCodes.STATUS_NONE;
                                    int     expAckCode  = StatusCodes.STATUS_NONE;
                                    long    stateMask   = 0x0000;
                                    boolean stateVal    = false;
                                    java.util.List<DCServerConfig.CommandArg> cmdArgList = new Vector<DCServerConfig.CommandArg>();
                                    NodeList cmdSubNodes = cmdElem.getChildNodes();
                                    for (int s = 0; s < cmdSubNodes.getLength(); s++) {
                                        Node cmdSubNode = cmdSubNodes.item(s);
                                        if (!(cmdSubNode instanceof Element)) { continue; }
                                        String cmdNodeName = cmdSubNode.getNodeName();
                                        Element cmdSubElem = (Element)cmdSubNode;
                                        if (cmdNodeName.equalsIgnoreCase(TAG_Type)) {
                                            // where the command should be available (ie. "map,admin")
                                            String typ = XMLTools.getNodeText(cmdSubElem,",");
                                            cmdTypes = StringTools.parseStringArray(typ,',');
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_Description)) {
                                            // command description
                                            cmdDesc = XMLTools.getNodeText(cmdSubElem," ");
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_AclName)) {
                                            // command ACL name
                                            cmdAclName = XMLTools.getNodeText(cmdSubElem,"");
                                            String aclDftStr = XMLTools.getAttribute(cmdSubElem,ATTR_default,null,false);
                                            cmdAclDft = AclEntry.parseAccessLevel(aclDftStr, AclEntry.AccessLevel.WRITE);
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_String)) {
                                            // the command string to send to the device
                                            cmdProto   = XMLTools.getAttribute(cmdSubElem,ATTR_protocol,"",false);
                                            maxUdpAge  = XMLTools.getAttributeInt(cmdSubElem,ATTR_udpMaxAge,0,false);
                                            allowQueue = XMLTools.getAttributeBoolean(cmdSubElem,ATTR_queue,false,false);
                                            cmdString  = XMLTools.getNodeText(cmdSubElem,"");
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_StatusCode)) {
                                            // generate an audit event with this status code
                                            String scStr = XMLTools.getNodeText(cmdSubElem,"");
                                            int sc = StringTools.parseInt(scStr,-1);
                                            cmdSCode = (sc > 0)? sc : StatusCodes.STATUS_NONE;
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_ExpectAckCode)) {
                                            // expect an acknowledgement event with this status code
                                            String scStr = XMLTools.getNodeText(cmdSubElem,"");
                                            int sc = StringTools.parseInt(scStr,-1);
                                            if (sc >= 0) { // >= StatusCodes.STATUS_NONE)
                                                expAckCode = sc;
                                                expectAck  = true;
                                            } else {
                                                expAckCode = StatusCodes.STATUS_NONE;
                                                // leave "expectAck" as-is
                                            }
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_Arg)) {
                                            // command string argument/parameter
                                            String  argName   = XMLTools.getAttribute(cmdSubElem,ATTR_name,null,false);
                                            String  argVar    = XMLTools.getAttribute(cmdSubElem,ATTR_sessionVar,null,false);
                                            String  argDftVal = XMLTools.getAttribute(cmdSubElem,ATTR_defaultValue,null,false);
                                            boolean argROnly  = XMLTools.getAttributeBoolean(cmdSubElem,ATTR_readOnly,false,false);
                                            String  argDesc   = StringTools.trim(XMLTools.getNodeText(cmdSubElem," "));
                                            DCServerConfig.CommandArg cmdArg;
                                            cmdArg = new DCServerConfig.CommandArg(argName,argDesc,argROnly,argVar,argDftVal);
                                            String  argLen    = XMLTools.getAttribute(cmdSubElem,ATTR_length,null,false);
                                            if (!StringTools.isBlank(argLen)) {
                                                int len[] = StringTools.parseInt(StringTools.split(argLen,','),0);
                                                int dispLen = ((len.length > 0) && (len[0] > 0))? len[0] : 70;
                                                int maxLen  = ((len.length > 1) && (len[1] > 0))? len[1] : dispLen;
                                                if (maxLen < dispLen) { maxLen = dispLen; }
                                                cmdArg.setLength(dispLen, maxLen);
                                            }
                                            cmdArgList.add(cmdArg);
                                            hasArgs = true;
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_State)) {
                                            // command device state
                                            stateMask = XMLTools.getAttributeLong(cmdSubElem,ATTR_mask,0x0000L,false);
                                            if (stateMask == 0x0000L) {
                                                int bitNdx = XMLTools.getAttributeInt(cmdSubElem,ATTR_index,-1,false);
                                                if (bitNdx >= 0) {
                                                    stateMask = (bitNdx << 1L);
                                                }
                                            }
                                            stateVal = StringTools.parseBoolean(XMLTools.getNodeText(cmdSubElem,"",false),false);
                                        } else {
                                            // unrecognized tag
                                            Print.logWarn("Unrecognized Command sub-tag: " + cmdNodeName);
                                        }
                                    }
                                    if ((cmdPort > 0) || 
                                        "sms".equalsIgnoreCase(cmdProto) || 
                                        StringTools.startsWithIgnoreCase(cmdProto,"sms:")) {
                                        dcs.addCommand(
                                            cmdName   , cmdDesc, cmdEnable,
                                            cmdTypes  , 
                                            cmdAclName, cmdAclDft,
                                            cmdString , hasArgs, cmdArgList,
                                            cmdProto  , maxUdpAge, allowQueue,
                                            expectAck , expAckCode,
                                            stateMask , stateVal,
                                            cmdSCode);
                                        Print.logDebug("Added command [" + dcs.getName() + "]: " + cmdName + " (enabled=" + cmdEnable + ")");
                                    } else {
                                        Print.logWarn("["+dcs.getName()+"] Command ignored (not an SMS protocol command): " + cmdName);
                                    }
                                } else {
                                    Print.logDebug("Command disabled [" + dcs.getName() + "]: " + cmdName);
                                }
                            } else {
                                // unrecognized tag
                            }
                        }
                        continue;
                    }

                    /* ConfigProperties */
                    if (nodeName.equalsIgnoreCase(TAG_ConfigProperties)) {
                        NodeList propList = XMLTools.getChildElements(dcsElem,TAG_Property);
                        for (int p = 0; p < propList.getLength(); p++) {
                            Node propNode = propList.item(p);
                            if (propNode instanceof Element) {
                                Element propElem = (Element)propNode;
                                String key = XMLTools.getAttribute(propElem,ATTR_key,null,false);
                                if (!StringTools.isBlank(key)) {
                                    dcs.addRecommendedConfigPropertyKey(key);
                                }
                            }
                        }
                        continue;
                    }

                    /* should not reach here */
                    Print.logError("["+xmlFile+"] Unrecognized tag name: " + nodeName);
                    continue;

                } // for (int c = 0; c < childNodes.getLength(); c++) ...

                /* TCP check ports */
                if (TCPPortMap != null) {
                    int tcpPorts[] = dcs.getTcpPorts();
                    if (!ListTools.isEmpty(tcpPorts)) {
                        for (int p = 0; p < tcpPorts.length; p++) {
                            Integer port = new Integer(tcpPorts[p]);
                            if (TCPPortMap.containsKey(port)) {
                                DCServerConfig s = TCPPortMap.get(port);
                                if (warnPortConflict && !s.getName().equals(dcs.getName())) {
                                    Print.logWarn("["+xmlFile.getName()+":"+dcs.getName()+"] TCP Port Conflict: "+port+" (see '"+s.getName()+"')");
                                }
                            } else {
                                TCPPortMap.put(port,dcs);
                            }
                        }
                    }
                } else {
                    Print.logWarn("TCPPortMap is null!");
                }
    
                /* UDP check ports */
                if (UDPPortMap != null) {
                    int udpPorts[] = dcs.getUdpPorts();
                    if (!ListTools.isEmpty(udpPorts)) {
                        for (int p = 0; p < udpPorts.length; p++) {
                            Integer port = new Integer(udpPorts[p]);
                            if (UDPPortMap.containsKey(port)) {
                                DCServerConfig s = UDPPortMap.get(port);
                                if (warnPortConflict && !s.getName().equals(dcs.getName())) {
                                    Print.logWarn("["+xmlFile.getName()+":"+dcs.getName()+"] UDP Port Conflict: "+port+" (see '"+s.getName()+"')");
                                }
                            } else {
                                UDPPortMap.put(port,dcs);
                            }
                        }
                    }
                } else {
                    Print.logWarn("UDPPortMap is null!");
                }
    
                /* save dcs */
                //Print.logInfo("Saving " + dcs.getName());
                dcserverSet.add(dcs);

            } else
            if (dcsNodeName.equalsIgnoreCase(TAG_Include)) {
                Element inclTag = dcsChildElem;
              //int     portOffset = XMLTools.getAttributeInt(    inclTag, ATTR_portOffset,     0, false);
                boolean optional   = XMLTools.getAttributeBoolean(inclTag, ATTR_optional  , false, false);

                /* get file(s) */
                String inclFileStr = inclTag.getAttribute(ATTR_file);
                if (StringTools.isBlank(inclFileStr)) {
                    Print.logError("Invalid 'Include' (blank file)");
                    continue;
                }

                /* directory override */
                String inclDirStr = inclTag.getAttribute(ATTR_dir);
                File inclDir = null;
                if (!StringTools.isBlank(inclDirStr)) {
                    inclDir = new File(inclDirStr);
                } else
                if (!StringTools.isBlank(INCLUDE_DIR)) {
                    inclDir = new File(INCLUDE_DIR);
                }
        
                /* locate file(s) */
                boolean isGlob = (inclFileStr.indexOf("*") >= 0)? true : false;
                Vector<File> inclFileList = new Vector<File>();
                if ((inclDir != null) && inclDir.isAbsolute()) {
                    // absolute include directory location
                    if (isGlob) {
                        File files[] = FileTools.getFiles(inclDir, inclFileStr, false);
                        if (ListTools.size(files) > 0) {
                            ListTools.toList(files, inclFileList);
                        }
                    } else {
                        File file = new File(inclDir, inclFileStr);
                        if (file.isFile()) {
                            inclFileList.add(file);
                        }
                    }
                    // do not continue looking for the include file
                } else
                if (parentDir != null) {
                    if (isGlob) {
                        // relative parent/include directory
                        if (inclDir != null) {
                            File dir = new File(parentDir, inclDir.toString());
                            File files[] = FileTools.getFiles(dir, inclFileStr, false);
                            if (ListTools.size(files) > 0) {
                                ListTools.toList(files, inclFileList);
                            }
                        }
                        // relative parent directory
                        File dir = parentDir;
                        File files[] = FileTools.getFiles(dir, inclFileStr, false);
                        if (ListTools.size(files) > 0) {
                            ListTools.toList(files, inclFileList);
                        }
                    } else {
                        File inclFile = null;
                        // relative parent/include directory
                        if ((inclFile == null) && (inclDir != null)) {
                            File dir  = new File(parentDir, inclDir.toString());
                            File file = new File(dir, inclFileStr);
                            if (file.isFile()) {
                                inclFileList.add(file);
                            }
                        }
                        // relative parent directory
                        if (inclFile == null) {
                            File dir  = parentDir;
                            File file = new File(dir, inclFileStr);
                            if (file.isFile()) {
                                inclFileList.add(file);
                            }
                        }
                        if (FileTools.isFile(inclFile)) {
                            inclFileList.add(inclFile);
                        }
                    }
                }

                /* include */
                //Print.logInfo("Including["+recurseLvl+"]: " + inclFile + "  [optional="+optional+"]");
                if (ListTools.size(inclFileList) > 0) {
                    for (File inclFile : inclFileList) {
                        //Print.logInfo("DCServer file: " + inclFile);
                        try {
                            if (inclFile.getCanonicalPath().equals(xmlFile.getCanonicalPath())) {
                                Print.logWarn("Recursive Include ignored: " + inclFile.getCanonicalPath());
                            } else {
                                OrderedSet<DCServerConfig> inclDscSet = DCServerFactory._loadDCServerXML(inclFile, recurseLvl+1);
                                if (!ListTools.isEmpty(inclDscSet)) {
                                    for (DCServerConfig dcs : inclDscSet) {
                                        if (!dcserverSet.contains(dcs)) {
                                            //Print.logDebug("Adding: " + dcs);
                                            dcserverSet.add(dcs);
                                        }
                                    }
                                    //if (recurseLvl==0){for(DCServerConfig dcs:inclDscSet){Print.logInfo("Included["+recurseLvl+"]: "+dcs);}}
                                } else {
                                    //Print.logInfo("Nothing included ...");
                                }
                            }
                        } catch (Throwable th) {
                            Print.logException("Error while including file: " + inclFile, th);
                        }
                    }
                } else
                if (!optional) {
                    Print.logError("Include file not found: " + inclFileStr);
                } else {
                    // optional include ignored
                }

            }

        }

        /* success */
        return dcserverSet;

    }

    /** 
    *** Parses specified String containing a comma-separated list of port into
    *** an array of port numbers.
    *** @param portStr  A String containing a comma-separated list of port numbers.
    *** @return An integer array of parsed port numbers.
    **/
    private static int[] parsePorts(String portStr)
    {
        if (!StringTools.isBlank(portStr)) {
            String portArr[] = StringTools.split(portStr,',');
            int    portInt[] = StringTools.parseInt(portArr,-1);
            if (!ListTools.isEmpty(portInt)) {
                for (int i = 0; i < portInt.length; i++) { 
                    if (portInt[i] > 0) { 
                        portInt[i] += PORT_OFFSET; 
                    } else {
                        Print.logError("Invalid port specification: " + portStr);
                        return null;
                    }
                }
                return portInt;
            } else {
                Print.logError("Invalid port specification: " + portStr);
                return null;
            }
        }
        return null;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // bind address
    // (may ne necessary if this system has multiple IP addresses)
    public static       String  BIND_ADDRESS                = null;

    // listen backlog
    // (how many pending connections are allowed before they start being rejected)
    public static       int     LISTEN_BACKLOG              = -1; // use default (50)

    // port offset (used by 'getPort' only)
    // (used to change to general location of all server ports as a group)
    public static       int     PORT_OFFSET                 = 0;

    // include directory
    // (directory where included files may be placed)
    public static       String  INCLUDE_DIR                 = null;

    // 'GTS_HOME' environment variable
    public static       String  GTS_HOME                    = null;

    // DCServerConfig map
    private static HashMap<String,DCServerConfig> dcServerMap; // do not pre-initialize

    // "missing server" list
    private static      Vector<String> missingServerList    = null;

    /**
    *** Static class initialization
    **/
    static {
        // should not be initialized here!
        //DCServerFactory._startupInit();
    }
    
    /**
    *** Returns the global bind-address
    **/
    public static String getBindAddress()
    {
        return BIND_ADDRESS;
    }

    /**
    *** Returns a map of loaded DCServerConfig instances
    **/
    private static HashMap<String,DCServerConfig> _DCServerMap(boolean dispError)
    {
        if (DCServerFactory.dcServerMap == null) {
            if (dispError && (BasicPrivateLabel.isTrackServlet() || RTConfig.isCommandLine())) {
                if (DCServerFactory.HasSpecificDCServerName()) {
                    String dcsName = DCServerFactory.GetSpecificDCServerName();
                    Print.logStackTrace("DCServerConfig not initialized! ['"+dcsName+"' not found?]");
                } else {
                    Print.logStackTrace("DCServerConfig not initialized!");
                }
            }
            DCServerFactory.dcServerMap = new HashMap<String,DCServerConfig>();
        }
        return DCServerFactory.dcServerMap;
    }

    private static boolean _didInit = false;
    /**
    *** Runtime startup initialization
    **/
    public static void init()
    {
        if (!_didInit) {
            DCServerFactory._startupInit();
            SMSOutboundGateway._startupInit();
        }
    }

    /**
    *** Startup initialization
    **/
    private static void _startupInit()
    {
        Print.logDebug("DCServerFactory initializing ...");
        _didInit = true;

        // This must be called _after_ the command-line runtime initialization has occurred.
        if (!RTConfig.isInitialized()) {
            Print.logError("**** DCServerFactory init: Runtime configuration has not been properly initialized ***");
        }

        /* 'GTS_HOME' */
        GTS_HOME = System.getenv(DBConfig.env_GTS_HOME);
        if (StringTools.isBlank(GTS_HOME)) {
            Print.logWarn("Environment variable 'GTS_HOME' not defined");
        }

        /* specific DCServer? */
        DCServerFactory.InitSpecificDCServerName();
        if (DCServerFactory.HasSpecificDCServerName()) {
            Print.logDebug("Loading only DCServer name: " + DCServerFactory.GetSpecificDCServerName());
        }

        /* load 'dcserver.xml' (which recursively loads all other dcserver_xxxx.xml files) */
        DCServerFactory.loadDCServerXML(null);

        /* ServerSocketThread global bind interface */
        String bindAddress = BIND_ADDRESS;
        if (StringTools.isBlank(bindAddress)) {
            // -- bind to any/all available address(es)
            ServerSocketThread.setBindAddress(null);
        } else {
            // -- bind to a specific IP address (network interface)
            try {
                InetAddress localBA = InetAddress.getByName(bindAddress);
                Print.logDebug("ServerSocketThread Local Bind Address: " + localBA);
                ServerSocketThread.setBindAddress(localBA);
            } catch (UnknownHostException uhe) {
                //Print.logException("Setting local bind interface", uhe);
                Print.logError("Local Bind Address Unknown Host: " + bindAddress);
                ServerSocketThread.setBindAddress(null);
            }
        }

        /* ServerSocketThread listen backlog */
        if (LISTEN_BACKLOG > 0) {
            Print.logDebug("ServerSocketThread Listen Backlog: " + LISTEN_BACKLOG);
            ServerSocketThread.setListenBacklog(LISTEN_BACKLOG);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the named server to the list of registered servers
    **/
    public static DCServerConfig addDCS(
        String name, String desc, 
        int tcpPorts[], int udpPorts[], 
        int commandPort, 
        long flags, 
        String... uniqPfx)
    {

        // ignore if no name
        if (StringTools.isBlank(name)) {
            // quietly ignore
            return null;
        }

        // already added?
        if (DCServerFactory._DCServerMap(false).containsKey(name)) {
            // ignore duplicate entries
            Print.logDebug("Ignoring duplicate DCServer: " + name);
            return null;
        }

        // port keys
        if (!DCServerFactory.serverJarExists(name)) {
            Set<String> serverKeys = RTConfig.getPropertyKeys(name + ".", false);
            if (!ListTools.isEmpty(serverKeys)) {
                if (DCServerFactory.missingServerList == null) {
                    DCServerFactory.missingServerList = new Vector<String>();
                }
                DCServerFactory.missingServerList.add(name);
            }
            Print.logDebug("Server jar not found: %s", name);
            if (!RTConfig.isWebApp()) {
                // skip this server if running from command-line
                return null;
            }
        }

        // add configuration
        DCServerConfig dcs = new DCServerConfig(
            name, desc, 
            tcpPorts, udpPorts, commandPort, 
            flags, uniqPfx);
        return DCServerFactory.addDCS(dcs);

    }

    /**
    *** Adds the named server to the list of registered servers
    **/
    private static DCServerConfig addDCS(DCServerConfig dcs)
    {

        /* null server? */
        if (dcs == null) {
            // quietly ignore
            //Print.logDebug("Ignoring null DCServer ...");
            return null;
        }

        /* DCS name */
        String name = dcs.getName();
        if (StringTools.isBlank(name)) {
            // quietly ignore
            //Print.logDebug("Ignoring DCServer with blank name ...");
            return null;
        }

        /* already added? */
        if (DCServerFactory._DCServerMap(false).containsKey(name)) {
            // ignore duplicate entries
            Print.logDebug("Ignoring duplicate DCServer: " + name);
            return null;
        }

        /* add */
        //Print.logInfo("Adding: " + dcs);
        DCServerFactory._DCServerMap(false).put(name, dcs);
        return dcs;

    }
    
    /**
    *** Returns the DCServerConfig instance for the specified device communication server name
    *** @param name  The name of the device communication server
    *** @return The DCServerConfig instance
    **/
    public static DCServerConfig _getServerConfig(String name)
    {
        return DCServerFactory._DCServerMap(true).get(name);
    }

    /**
    *** Returns the DCServerConfig instance for the specified device communication server name
    *** @param name  The name of the device communication server
    *** @return The DCServerConfig instance
    **/
    public static DCServerConfig getServerConfig(String name)
    {
        return DCServerFactory.getServerConfig(name, false);
    }

    /**
    *** Returns the DCServerConfig instance for the specified device communication server name
    *** @param name  The name of the device communication server
    *** @param warn  If true, a warning message will be displayed if "name" does not exist.
    *** @return The DCServerConfig instance
    **/
    public static DCServerConfig getServerConfig(String name, boolean warn)
    {
        if (StringTools.isBlank(name)) {
            if (warn) {
                Print.logError("DCServerConfig name is blank");
            }
            return null;
        } else {
            DCServerConfig dcs = DCServerFactory._getServerConfig(name);
            if (dcs == null) {
                if (warn) {
                    //Print.logStackTrace("DCServerConfig not found: " + name);
                    StringBuffer m = new StringBuffer();
                    m.append("DCServerConfig not found: ").append(name);
                    if (DCServerFactory.LoadedDCServerXMLFile != null) {
                        m.append(" [Not defined in ");
                        m.append(DCServerFactory.LoadedDCServerXMLFile);
                        m.append("?]");
                    }
                    Print.logWarn(m.toString());
                }
                return null;
            } else {
                return dcs;
            }
        }
    }

    private static String lastNotFoundDCS = null;
    /**
    *** Gets the server config description
    *** @param serverName The server name
    *** @return The server config description
    **/
    public static String getServerConfigDescription(String serverName)
    {
        if (!StringTools.isBlank(serverName)) {
            DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
            if (dcs != null) {
                return dcs.getDescription();
            } else {
                if ((lastNotFoundDCS == null) || !lastNotFoundDCS.equals(serverName)) {
                    // attempt to cut down on the number of displayed error messages
                    Print.logWarn("DCS not found: " + serverName);
                    lastNotFoundDCS = serverName;
                }
                return "(" + serverName + ")";
            }
        } else {
            return "";
        }
    }

    /**
    *** Returns True if the named DCServerConfig has been registered
    *** @param name  The name of the device communication server
    *** @return True if the named DCServerConfig has been registered
    **/
    public static boolean hasServerConfig(String name)
    {
        if (StringTools.isBlank(name)) {
            return false;
        } else {
            //return (DCServerFactory._DCServerMap(true).get(name) != null);
            return DCServerFactory._DCServerMap(false).containsKey(name);
        }
    }

    /**
    *** Returns a list of all DCServerConfig instances
    *** @param inclAll True to include all DCS modules register, false to only include 
    ***             DCS modules for which the DCS jar file was also found.
    *** @return A list of DCServerConfig instances (does not return null)
    **/
    public static java.util.List<DCServerConfig> getServerConfigList(boolean inclAll)
    {
        java.util.List<DCServerConfig> list = new Vector<DCServerConfig>();
        for (DCServerConfig dcs : DCServerFactory._DCServerMap(true/*dispErrors*/).values()) {
            String n = dcs.getName();
            if (inclAll || DCServerFactory.serverJarExists(n)) {
                list.add(dcs);
            }
        }
        return list;
    }

    /**
    *** (used by CheckInstall) Return the number of refererenced servers which are undefined
    **/
    public static boolean hasUndefinedServers()
    {
        return !ListTools.isEmpty(DCServerFactory.missingServerList);
    }

    /**
    *** (used by CheckInstall) Returns the list of referenced, but undefined, servers
    **/
    public static java.util.List<String> getUndefinedServerList()
    {
        return DCServerFactory.missingServerList;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the named server supports a command port
    **/
    public static boolean supportsCommandDispatcher(String serverName)
    {
        return (DCServerFactory.getCommandDispatcherPort(serverName) > 0);
    }

    /**
    *** Returns true if the server for the specified Device supports a command port
    **/
    public static boolean supportsCommandDispatcher(Device device)
    {
        if (device == null) {
            return false;
        } else {
            return DCServerFactory.supportsCommandDispatcher(device.getDeviceCode());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this device supports digital inputs
    *** @return True if this device supports digital inputs
    **/
    public static boolean hasDigitalInputs(String serverName)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        return (dcs != null)? dcs.hasDigitalInputs() : false;
    }

    /**
    *** Returns true if this device supports digital outputs
    *** @return True if this device supports digital outputs
    **/
    public static boolean hasDigitalOutputs(String serverName)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        return (dcs != null)? dcs.hasDigitalOutputs() : false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the server port (with constant offset applied)
    *** @param port  server port (without offset applied)
    *** @return The server port
    **/
    public static int getPort(int port)
    {
        return (port > 0)? (port + PORT_OFFSET) : 0;
    }

    /**
    *** Returns an array of server ports (with constant offset applied)
    *** @param ports  array of server ports (without offset applied)
    *** @return The server port array
    **/
    public static int[] getPorts(int... ports)
    {
        if (!ListTools.isEmpty(ports)) {
            int newPorts[] = new int[ports.length];
            for (int i = 0; i < ports.length; i++) {
                newPorts[i] = getPort(ports[i]);
            }
            return newPorts;
        } else {
            return new int[0];
        }
    }

    /**
    *** Private abbreviation for 'getPort'
    *** @param port  server port (without offset applied)
    *** @return The server port array
    **/
    private static int GP(int port)
    {
        return getPort(port);
    }

    /**
    *** Private abbreviation for 'getPorts'
    *** @param ports  array of server ports (without offset applied)
    *** @return The server port array
    **/
    private static int[] GP(int... ports)
    {
        return getPorts(ports);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Command ResultCode ID
    **/
    public static String getCommandResultID(RTProperties r)
    {
        return (r != null)? r.getString(RESPONSE_RESULT,"") : null;
    }

    /**
    *** Gets the Command ResultCode.
    *** Does not return null.
    **/
    public static ResultCode getCommandResultCode(RTProperties r)
    {

        /* null properties */
        if (r == null) {
            return ResultCode.UNKNOWN;
        }

        /* ResultCode ID */
        String rid = DCServerFactory.getCommandResultID(r);
        if (StringTools.isBlank(rid)) {
            // ResultCode ID is blank, assume SUCCESS
            return ResultCode.SUCCESS;
        }

        /* ResultCode */
        ResultCode rc = GetResultCode(rid,null);
        if (rc == null) {
            // a non-blank ResultCode was specified, but unrecognized 
            return ResultCode.UNKNOWN;
        }

        /* result code found */
        return rc;
            
    }

    /**
    *** Return true if the ResultCode represents a successful request/operation
    **/
    public static boolean isCommandResultOK(RTProperties r)
    {
        if (r != null) {
            String rid = DCServerFactory.getCommandResultID(r);
            if (StringTools.isBlank(rid)) {
                return true;
            } else {
                ResultCode rsID = GetResultCode(rid,null);
                return (rsID != null)? rsID.isSuccess() : false;
            }
        } else {
            return false;
        }
    }

    /**
    *** Return true if the ResultCode represents a successful request/operation
    **/
    public static boolean isCommandResultOK(ResultCode rid)
    {
        if (rid != null) {
            return rid.isSuccess();
        } else {
            return false;
        }
    }

    /**
    *** Gets the Command ResultCode Message
    **/
    public static String getCommandResultMessage(RTProperties r)
    {
        return (r != null)? r.getString(RESPONSE_MESSAGE,"") : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the current process context to a running DCS module
    **/
    public static void __setRunningDCS(String name)
    {
        if (!DCServerFactory.__isRunningDCS()) {
            String n = (name != null)? (": " + name) : "";
            //Print.logInfo("Setting current process context to running DCS module" + n);
            RTConfig.getRuntimeConstantProperties().setBoolean(DBConfig.PROP_DCSServer_isRunningInDCS,true);
        } else {
            // already set as running DCS
        }
    }

    /**
    *** Returns true if the current process context is a running DCS module
    **/
    public static boolean __isRunningDCS()
    {
        return RTConfig.getRuntimeConstantProperties().getBoolean(DBConfig.PROP_DCSServer_isRunningInDCS,false);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Send SMS command to device
    *** @param handlerName  The name of the SMS gateway handler
    *** @param device       The device to which the SMS command is sent
    *** @param commandStr   The SMS command which is sent
    *** @return The ResultCode
    **/
    private static DCServerFactory.ResultCode _SendSMSCommand(String handlerName, Device device, String commandStr)
    {

        /* invalid device? */
        if (device == null) {
            return DCServerFactory.ResultCode.INVALID_DEVICE;
        }

        /* Account SMS enabled? */
        Account account = device.getAccount();
        if ((account == null) || !account.getSmsEnabled()) {
            return DCServerFactory.ResultCode.NOT_AUTHORIZED;
        }

        /* default handler name */
        String smsHandler = StringTools.blankDefault(handlerName, SMSOutboundGateway.GetDefaultGatewayName());

        /* get handler */
        SMSOutboundGateway smsgw = SMSOutboundGateway.GetSMSGateway(smsHandler);
        if (smsgw == null) {
            Print.logError("SMS gateway handler not found: " + smsHandler);
            return DCServerFactory.ResultCode.INVALID_PROTO;
        }

        /* send */
        Print.logDebug("CommandString: ["+device.getAccountID()+"/"+device.getDeviceID()+"] " + commandStr);
        DCServerFactory.ResultCode result = smsgw.sendSMSCommand(device, commandStr);

        /* return result */
        return (result != null)? result : DCServerFactory.ResultCode.INVALID_COMMAND;

    }

    /**
    *** Send SMS command to device
    *** @param handlerName  The name of the SMS gateway handler
    *** @param device       The device to which the SMS command is sent
    *** @param commandStr   The SMS command which is sent
    *** @return The ResultCode
    **/
    public static DCServerFactory.ResultCode SendSMSCommand(String handlerName, Device device, String commandStr)
    {
        boolean isDCS = DCServerFactory.__isRunningDCS();
        String dcsCtx = isDCS? "(DCS Context) " : "(UI Context) ";

        /* invalid device? */
        if (device == null) {
            return DCServerFactory.ResultCode.INVALID_DEVICE;
        }

        /* Account/Device: exceeds max 'ping' */
        if (device.exceedsMaxPingCount()) {
            Print.logWarn(dcsCtx + "Account/Device exceeded maximum allowed pings.");
            return DCServerFactory.ResultCode.OVER_LIMIT;
        }

        /* Account SMS enabled? */
        Account account = device.getAccount();
        if ((account == null) || !account.getSmsEnabled()) {
            return DCServerFactory.ResultCode.NOT_AUTHORIZED;
        }

        /* send SMS command */
        DCServerFactory.ResultCode resp = DCServerFactory._SendSMSCommand(handlerName, device, commandStr);

        /* SMS: increment ping count */
        if (DCServerFactory.__isRunningDCS()) {
            Print.logInfo(dcsCtx + "SMS Response: " + resp);
            // running in DCS, skip ping count increment
        } else
        if ((device != null) && DCServerFactory.isCommandResultOK(resp)) {
            // This section must be skipped if running in the context of the DCS itself.
            Print.logInfo(dcsCtx + "Incrementing Device PingCount (SMS) ...");
            device.incrementPingCount(DateTime.getCurrentTimeSec(), true/*reload*/, true/*update*/); // SMS
            String acctID = RTConfig.getString(RTKey.SESSION_ACCOUNT  ,device.getAccountID());
            String userID = RTConfig.getString(RTKey.SESSION_USER     ,"");
            String devID  = device.getDeviceID();
            String ipAddr = RTConfig.getString(RTKey.SESSION_IPADDRESS,"");
            String cmdStr = "Command: " + commandStr;
          //if (!ListTools.isEmpty(cmdArgs)) { cmdStr += "(" + StringTools.join(cmdArgs,",") + ")"; }
            Audit.deviceCommand(acctID, userID, devID, ipAddr, cmdStr);
        } else {
            Print.logInfo(dcsCtx + "SMS Response: " + resp);
        }
        
        /* return response */
        return resp;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the server 'command' port
    *** @param serverName The server name
    *** @return The server command port, or '0' if not supported
    **/
    public static int getCommandDispatcherPort(String serverName)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        return (dcs != null)? dcs.getCommandDispatcherPort() : 0;
    }

    /**
    *** Send a command request to the command port for the specified server
    **/
    private static RTProperties _sendServerCommand(
        String serverName,
        Device device,
        String cmdType, String cmdName, String cmdArgs[])
    {

        /* blank account/device */
        if ((device == null) && 
            !cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_INTERNAL)) {
            Print.logError("Device is null");
            return null;
        }
        String accountID = (device != null)? device.getAccountID() : "";
        String deviceID  = (device != null)? device.getDeviceID()  : "";
        String uniqueID  = (device != null)? device.getUniqueID()  : "";

        /* DCS */
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        if (dcs == null) {
            Print.logError("DCServerConfig not found: %s", serverName);
            return null;
        }

        /* command */
        RTProperties rtCmd = DCServerFactory.createRTProperties(
            accountID, deviceID, uniqueID, 
            cmdType, cmdName, cmdArgs);
        String   cmdStr    = rtCmd.toString();
        byte     cmdData[] = (cmdStr + "\n").getBytes();

        /* get command dispatch host:port */
        String cmdHost = dcs.getCommandDispatcherHost(device);
        int    cmdPort = dcs.getCommandDispatcherPort();

        /* send command via SMS if no port defined */
        if (cmdPort <= 0) {
            DCServerConfig.Command command = dcs.getCommand(cmdName);
            if (command == null) {
                Print.logError("[%s] Command not supported: %s", serverName, cmdName);
                return null;
            } else
            if (!command.isCommandProtocolSMS()) {
                Print.logError("[%s] Command port not supported", serverName);
                return null;
            }
            // send command via SMS (SmsOutboudGatweay)
            String commandStr = command.getCommandString(device, cmdArgs);
            String protoHndlr = command.getCommandProtocolHandler();
            DCServerFactory.ResultCode result = DCServerFactory._SendSMSCommand(protoHndlr, device, commandStr);
            CommandPacketHandler.setResult(rtCmd,result);
            return rtCmd;
        }

        /* send */
        Print.logInfo("[%s] Sending command to '%s:%d' ==> %s", serverName, cmdHost, cmdPort, cmdStr);
        RTProperties response = null;
        ClientSocketThread cst = new ClientSocketThread(cmdHost, cmdPort);
        try {
            cst.openSocket();
            cst.socketWriteBytes(cmdData);
            String resp = cst.socketReadLine();
            Print.logInfo("[%s] Command Response: %s", serverName, resp);
            response = new RTProperties(resp);
        } catch (ConnectException ce) { // "Connection refused"
            Print.logError("[" + serverName + "] Unable to connect to server: " + ce.getMessage());
        } catch (Throwable t) {
            Print.logException("[" + serverName + "] Server Command Error", t);
        } finally {
            cst.closeSocket();
        }
        
        /* return response */
        return response;

    }
    
    /**
    *** Send a command request to the server command port for the specified Device
    **/
    public static RTProperties sendServerCommand(
        Device device,
        String cmdType, String cmdName, String cmdArgs[])
    {
        boolean isDCS = DCServerFactory.__isRunningDCS();
        String dcsCtx = isDCS? "(DCS Context) " : "(UI Context) ";

        /* quick checks */
        if (device == null) {
            Print.logWarn(dcsCtx + "Device is null");
            return null;
        } 
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();
        String unqID  = device.getUniqueID();

        /* DCServerConfig */
        String server = device.getDeviceCode();
        if (StringTools.isBlank(server)) {
            Print.logWarn(dcsCtx + "DeviceCode is null/blank");
            ResultCode result = ResultCode.INVALID_SERVER;
            RTProperties resp = DCServerFactory.createRTProperties(
                acctID, devID, unqID, 
                cmdType, cmdName, cmdArgs);
            resp.setString(DCServerFactory.RESPONSE_RESULT , result.getCode() );
            resp.setString(DCServerFactory.RESPONSE_MESSAGE, result.toString());
            return resp;
        }

        /* Account/Device: exceeds max 'ping' */
        if (device.exceedsMaxPingCount()) {
            Print.logWarn(dcsCtx + "Account/Device exceeded maximum allowed pings.");
            ResultCode result = ResultCode.OVER_LIMIT;
            RTProperties resp = DCServerFactory.createRTProperties(
                acctID, devID, unqID, 
                cmdType, cmdName, cmdArgs);
            resp.setString(DCServerFactory.RESPONSE_RESULT , result.getCode() );
            resp.setString(DCServerFactory.RESPONSE_MESSAGE, result.toString());
            return resp;
        }

        /* send command */
        RTProperties resp = DCServerFactory._sendServerCommand(server, device, cmdType, cmdName, cmdArgs);

        /* increment ping count */
        if ((device != null) && DCServerFactory.isCommandResultOK(resp)) {
            Print.logInfo(dcsCtx + "Incrementing Device PingCount ...");
            device.incrementPingCount(DateTime.getCurrentTimeSec(), true/*reload*/, true/*update*/); // Server delegate
          //String acctID = RTConfig.getString(RTKey.SESSION_ACCOUNT  ,device.getAccountID());
            String userID = RTConfig.getString(RTKey.SESSION_USER     ,"");
          //String devID  = device.getDeviceID();
            String ipAddr = RTConfig.getString(RTKey.SESSION_IPADDRESS,"");
            String cmdStr = "Command: " + cmdName;
            if (!ListTools.isEmpty(cmdArgs)) { cmdStr += "(" + StringTools.join(cmdArgs,",") + ")"; }
            Audit.deviceCommand(acctID, userID, devID, ipAddr, cmdStr);
        } else {
            Print.logInfo(dcsCtx + "Response: " + resp);
        }

        /* return response */
        return resp;

    }

    /* create result RTProperties */
    public static RTProperties createRTProperties(
        String accountID, String deviceID, String uniqueID,
        String cmdType, String cmdName, String cmdArgs[])
    {
        RTProperties rtCmd = new RTProperties();
        rtCmd.setString(DCServerFactory.CMDARG_ACCOUNT, accountID);
        rtCmd.setString(DCServerFactory.CMDARG_DEVICE , deviceID);
        rtCmd.setString(DCServerFactory.CMDARG_UNIQUE , uniqueID);
        rtCmd.setString(DCServerFactory.CMDARG_CMDTYPE, cmdType);
        rtCmd.setString(DCServerFactory.CMDARG_CMDNAME, cmdName);
        if (!ListTools.isEmpty(cmdArgs)) {
            for (int i = 0; (i < cmdArgs.length) && (cmdArgs[i] != null); i++) {
                String argKey = CMDARG_ARG + i;
                String argVal = cmdArgs[i]; // may be null (null values create an undefined key)
                rtCmd.setString(argKey, argVal);
            }
        }
        return rtCmd;
    }

    // ------------------------------------------------------------------------

    /* create TCP session ID */
    public static String createTcpSessionID(Device device)
    {
        if (device == null) {
            return null;
        } else {
            String tsi = device.getAccountID() + "/" + device.getDeviceID();
            device.setLastTcpSessionID(tsi);
            return tsi;
        }
    }
    
    public static String getTcpSessionID(Device device)
    {
        if (device == null) {
            return null;
        } else 
        if (device.hasLastTcpSessionID()) {
            return device.getLastTcpSessionID();
        } else {
            return DCServerFactory.createTcpSessionID(device);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Calculates/returns the next odometer value 
    **/
    public static double calculateOdometerKM(EventData prevEvent, GeoPoint toPoint)
    {
        if ((prevEvent != null) && prevEvent.isValidGeoPoint() && GeoPoint.isValid(toPoint)) {
            double deltaKM = toPoint.kilometersToPoint(prevEvent.getGeoPoint());
            return prevEvent.getOdometerKM() + deltaKM;
        } else {
            return 0.0;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* "UnassignedDevices" class */
    private static boolean    ClassUnassignedDevices_init = false;
    private static Class      ClassUnassignedDevices      = null;

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static boolean addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        double lat, double lon,
        String data)
    {

        /* get the "UnassignedDevices" class */
        if (!ClassUnassignedDevices_init) {
            ClassUnassignedDevices_init = true; // TODO: threads?
            try {
                ClassUnassignedDevices = Class.forName(DBConfig.PACKAGE_EXTRA_TABLES_ + "UnassignedDevices");
            } catch (Throwable th) { // Class not found exception
                return false;
            }
        }
        if (ClassUnassignedDevices == null) {
            return false; // not supported
        }

        /* add entry */
        try {
            MethodAction unasDev = new MethodAction(
                ClassUnassignedDevices,
                "add",
                String.class/*serverID*/, String.class/*mobileID*/, 
                String.class/*ipAddr*/, Boolean.TYPE/*isDuplex*/, 
                Double.TYPE/*latitude*/, Double.TYPE/*longitude*/, 
                String.class/*data*/);
            unasDev.invoke(dcName, mobID, ipAddr, isDuplex, lat, lon, data);
            return true;
        } catch (Throwable th) {
            Print.logException("Adding UnassignedDevices", th);
            return false;
        }

    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static boolean addUnassignedDevice(
        String dcName, String mobID, 
        GeoPoint geoPoint)
    {
        return DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, geoPoint, null/*data*/);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static boolean addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        GeoPoint geoPoint)
    {
        return DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, geoPoint, null/*data*/);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static boolean addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        GeoPoint geoPoint,
        String data)
    {
        double lat = (geoPoint != null)? geoPoint.getLatitude()  : 0.0;
        double lon = (geoPoint != null)? geoPoint.getLongitude() : 0.0;
        return DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, lat, lon, data);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static boolean addUnassignedDevice(
        String dcName, String mobID, 
        double lat, double lon)
    {
        return DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, lat, lon, null/*data*/);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static boolean addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        double lat, double lon)
    {
        return DCServerFactory.addUnassignedDevice(dcName, mobID, ipAddr, isDuplex, lat, lon, null/*data*/);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the EventData record previous to the specified fixtime
    *** @param device  The Device record handle
    *** @param fixtime The current event fixtime
    *** @return The previous event, or null if there is no previous event
    **/
    public static EventData getPreviousEventData(Device device, long fixtime)
    {
        if (device != null) {
            try {
                long startTime = -1L;
                long endTime   = fixtime - 1L;
                EventData ed[] = EventData.getRangeEvents(
                    device.getAccountID(), device.getDeviceID(),
                    startTime, endTime,
                    null/*statusCodes*/,
                    true/*validGPS*/,
                    EventData.LimitType.LAST, 1, true,
                    null/*additionalSelect*/);
                if ((ed != null) && (ed.length > 0)) {
                    return ed[0];
                } else {
                    return null;
                }
            } catch (DBException dbe) {
                return null;
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Analyzes/Prints the current memory usage.
    *** (see OSTools.checkMemoryUsage) 
    **/
    public static void checkMemoryUsage()
    {
        OSTools.checkMemoryUsage(false/*reset*/);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Load device record from account-id/device-id
    *** @param accountID  The Account ID
    *** @param deviceID   The DeviceID
    *** @return The Device record
    **/
    public static Device loadDeviceByAccountDeviceID(String accountID, String deviceID)
    {
        try {

            /* get Account */
            Account account = Account.getAccount(accountID);
            if (account == null) {
                Print.logWarn("!!!AccountID not found!: " + accountID);
                return null;
            } else
            if (!account.isActive()) {
                Print.logWarn("Account is inactive: " + accountID);
                return null;
            }

            /* get Device */
            Device device = Device.getDevice(account, deviceID); // null if non-existent
            if (device == null) {
                Print.logWarn("!!!DeviceID not found!: " + accountID + "/" + deviceID);
                return null;
            } else
            if (!device.isActive()) {
                Print.logWarn("Device is inactive: " + accountID + "/" + deviceID);
                return null;
            }

            /* return device */
            return device;

        } catch (Throwable dbe) { // DBException
            Print.logError("Exception getting Device: " + accountID + "/" + deviceID + " [" + dbe + "]");
            return null;
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Load device record from unique-id
    *** @param prefix     An array of unique-id prefixes
    *** @param modemID    The unique modem ID (IMEI, ESN, etc)
    *** @return The Device record
    **/
    public static Device _loadDeviceByPrefixedModemID(String prefix[], String modemID)
    {
        return DCServerFactory._loadDeviceByPrefixedModemID(prefix, modemID, 
            false/*saveUnassigned*/, null, null, true, null);
    }

    /**
    *** Load device record from unique-id
    *** @param prefix           An array of unique-id prefixes
    *** @param modemID          The unique modem ID (IMEI, ESN, etc)
    *** @param serverID         The server-id (also 'device code'), used for UnassignedDevice entries
    *** @param ipAddress        The inbound IP address, used for UnassignedDevice entries
    *** @param isDuplex         True if duplex, false if simplex, used for UnassignedDevice entries
    *** @param geoPoint         The GPS location of the device, used for UnassignedDevice entries
    *** @return The Device record
    **/
    public static Device _loadDeviceByPrefixedModemID(String prefix[], String modemID, 
        String serverID, String ipAddress, boolean isDuplex, GeoPoint geoPoint)
    {
        return DCServerFactory._loadDeviceByPrefixedModemID(prefix, modemID, 
            true/*saveUnassigned*/, serverID, ipAddress, isDuplex, geoPoint);
    }

    /**
    *** Load device record from unique-id
    *** @param prefix           An array of unique-id prefixes
    *** @param modemID          The unique modem ID (IMEI, ESN, etc)
    *** @param saveUnassigned   True to save Device to UnassignedDevices, if device is not found
    *** @param serverID         The server-id (also 'device code'), used for UnassignedDevice entries
    *** @param ipAddress        The inbound IP address, used for UnassignedDevice entries
    *** @param isDuplex         True if duplex, false if simplex, used for UnassignedDevice entries
    *** @param geoPoint         The GPS location of the device, used for UnassignedDevice entries
    *** @return The Device record
    **/
    public static Device _loadDeviceByPrefixedModemID(String prefix[], String modemID, 
        boolean saveUnassigned, String serverID, String ipAddress, boolean isDuplex, GeoPoint geoPoint)
    {
        Device device = null;

        /* blank ModemID? */
        if (StringTools.isBlank(modemID)) {
            Print.logWarn("!!!Specified MobileID is blank/null");
            return null;
        }

        /* find Device */
        String uniqueID = "";
        try {

            /* load device record */
            if (ListTools.isEmpty(prefix)) {
                uniqueID = modemID;
                //Print.logDebug("Looking for UniqueID: " + uniqueID);
                device = Transport.loadDeviceByUniqueID(uniqueID);
            } else {
                uniqueID = prefix[0] + modemID;
                for (int u = 0; u < prefix.length; u++) {
                    String pfxid = prefix[u] + modemID;
                    //Print.logDebug("Looking for UniqueID: " + pfxid);
                    device = Transport.loadDeviceByUniqueID(pfxid);
                    if (device != null) {
                        uniqueID = pfxid;
                        break;
                    }
                }
            }

            /* not found? */ 
            if (device == null) {
                Print.logWarn("!!!UniqueID not found!: " + uniqueID + " [" + StringTools.join(prefix,",")+ "]");
                if (saveUnassigned) {
                    DCServerFactory.addUnassignedDevice(serverID, modemID, ipAddress, isDuplex, geoPoint, null/*data*/);
                }
                return null;
            }

            /* inactive? */
            if (!device.getAccount().isActive() || !device.isActive()) {
                String a = device.getAccountID();
                String d = device.getDeviceID();
                Print.logWarn("Account/Device is inactive: " + a + "/" + d + " [" + uniqueID + "]");
                return null;
            }

            /* return device */
            device.setModemID(modemID);
            return device;

        } catch (Throwable dbe) { // DBException
            Print.logError("Exception getting Device: " + uniqueID + " [" + dbe + "]");
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Lookup the specified mobile ID in the Transport/Device tables using
    *** the specified DC server prefixes.
    **/
    private static void _lookupUniqueID(java.util.List<Device> devList, 
        DCServerConfig dcs,
        String uniqueID, Set<String> uidTried)
    {
        String uidPfx[] = (dcs != null)? dcs.getUniquePrefix() : new String[] { "" };
        for (String pfx : uidPfx) {
            String uid = pfx + uniqueID;
            if ((uidTried == null) || !uidTried.contains(uid)) {
                try {
                    //Print.logInfo("Checking: " + uid);
                    uidTried.add(uid);
                    Device device = Transport.loadDeviceByUniqueID(uid);
                    if (device != null) {
                        devList.add(device);
                    }
                } catch (DBException dbe) {
                    Print.logException("Error retrieving Device by UniqueID", dbe);
                }
            }
        }
    }
    
    /**
    *** Lookup the specified mobile-id in the Transport/Device tables, use
    *** all available DC servers unique-id prefixes.
    *** @param mobileID  The mobile ID to search for
    *** @return An array of matching Devices, or null if no Device was found
    **/
    public static Device[] lookupUniqueID(String mobileID)
    {
        
        /* ignore blank mobile-id */
        if (StringTools.isBlank(mobileID)) {
            return new Device[0];
        }
        
        /* list of found devices */
        java.util.List<Device> devList = new Vector<Device>();

        /* set of all unique id's we've tried (for optimization) */
        Set<String> uidTried = new HashSet<String>();

        /* try blank prefix first */
        DCServerFactory._lookupUniqueID(devList, null, mobileID, uidTried);

        /* scan all loaded DCServerConfig instances */
        HashMap<String,DCServerConfig> dcsMap = DCServerFactory._DCServerMap(false);
        for (String dcsName : dcsMap.keySet()) {
            DCServerFactory._lookupUniqueID(devList, dcsMap.get(dcsName), mobileID, uidTried);
        }

        /* return array of matching Devices */
        return devList.toArray(new Device[devList.size()]);

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of the unique-id prefix array
    *** @param pfx  The UniqueID prefixes
    *** @return A String representation of the unique-id prefix array
    **/
    public static String getUniquePrefixString(String pfx[])
    {
        if (ListTools.isEmpty(pfx)) {
            return "<blank>";
        } else {
            StringBuffer sb = new StringBuffer();
            String list[] = new String[pfx.length];
            for (int i = 0; i < pfx.length; i++) {
                if (sb.length() > 0) { sb.append(", "); }
                if (StringTools.isBlank(pfx[i]) || pfx[i].equals("*")) {
                    sb.append("<blank>");
                } else {
                    sb.append("\"").append(pfx[i]).append("\"");
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Runtime property name suffixes
    
    public static final String  ARG_udpPort[]                   = new String[] { "udpPort", "udp", "port" };
    public static final String  ARG_tcpPort[]                   = new String[] { "tcpPort", "tcp", "port" };
    public static final String  ARG_satPort[]                   = new String[] { "satPort", "sat"         };
    public static final String  ARG_commandPort[]               = new String[] { "commandPort", "command", "cmd" };

    public static final String  CFG_TrackServer_class           = ".TrackServer.class";          // String
    public static final String  CFG_CommandPacketHandler_class  = ".CommandPacketHandler.class"; // String
    public static final String  CFG_ClientPacketHandler_class   = ".ClientPacketHandler.class";  // String

    public static final String  CFG_uniquePrefix                = ".uniquePrefix";               // String array
    public static final String  CFG_uniqueIdPrefix              = ".uniqueIdPrefix";             // String array
    public static final String  CFG_tcpPort                     = ".tcpPort";                    // int
    public static final String  CFG_udpPort                     = ".udpPort";                    // int
    public static final String  CFG_satPort                     = ".satPort";                    // int
    public static final String  CFG_port                        = ".port";                       // int
    public static final String  CFG_commandPort                 = ".commandPort";                // int
    public static final String  CFG_commandProtocol             = ".commandProtocol";            // String [udp|tcp|sms]
    public static final String  CFG_ackResponsePort             = ".ackResponsePort";            // int
    public static final String  CFG_clientCommandPort           = ".clientCommandPort";          // int
    public static final String  CFG_clientCommandPort_udp       = ".clientCommandPort.udp";      // int
    public static final String  CFG_clientCommandPort_tcp       = ".clientCommandPort.tcp";      // int
    public static final String  CFG_tcpIdleTimeoutMS            = ".tcpIdleTimeoutMS";           // long
    public static final String  CFG_tcpPacketTimeoutMS          = ".tcpPacketTimeoutMS";         // long
    public static final String  CFG_tcpSessionTimeoutMS         = ".tcpSessionTimeoutMS";        // long
    public static final String  CFG_udpIdleTimeoutMS            = ".udpIdleTimeoutMS";           // long
    public static final String  CFG_udpPacketTimeoutMS          = ".udpPacketTimeoutMS";         // long
    public static final String  CFG_udpSessionTimeoutMS         = ".udpSessionTimeoutMS";        // long
    public static final String  CFG_minimumSpeedKPH             = ".minimumSpeedKPH";            // double
    public static final String  CFG_estimateOdometer            = ".estimateOdometer";           // boolean
    public static final String  CFG_ignoreDeviceOdometer        = ".ignoreDeviceOdometer";       // boolean
    public static final String  CFG_maximumHDOP                 = ".maximumHDOP";                // double
    public static final String  CFG_maximumAccuracyMeters       = ".maximumAccuracyMeters";      // double
    public static final String  CFG_checkLastOdometer           = ".checkLastOdometer";          // boolean
    public static final String  CFG_simulateGeozones            = ".simulateGeozones";           // boolean
    public static final String  CFG_geozoneSkipOldEvents        = ".geozoneSkipOldEvents";       // boolean
    public static final String  CFG_simulateDigitalInputs       = ".simulateDigitalInputs";      // boolean
    public static final String  CFG_minimumMovedMeters          = ".minimumMovedMeters";         // double
    public static final String  CFG_saveRawDataPackets          = ".saveRawDataPackets";         // boolean
    public static final String  CFG_startStopSupported          = ".startStopSupported";         // boolean
    public static final String  CFG_statusLocationInMotion      = ".statusLocationInMotion";     // Translate Location to InMotion [Boolean]
    public static final String  CFG_ignoreInvalidGPSFlag        = ".ignoreInvalidGPSFlag";       // boolean
    public static final String  CFG_ignoreEventsWithInvalidGPS  = ".ignoreEventsWithInvalidGPS"; // boolean
    public static final String  CFG_useLastValidGPSLocation     = ".useLastValidGPSLocation";    // boolean
    public static final String  CFG_useAltCurrentTimestamp      = ".useAltCurrentTimestamp";     // boolean
    public static final String  CFG_initialPacket               = ".initialPacket";              // String/Bytes
    public static final String  CFG_finalPacket                 = ".finalPacket";                // String/Bytes
    public static final String  CFG_saveSessionStatistics       = ".saveSessionStatistics";      // boolean
    public static final String  CFG_batteryLevelRange           = ".batteryLevelRange";          // double,double
    public static final String  CFG_startStopStatusCodes        = ".startStopStatusCodes";       // String
    public static final String  CFG_debugMode                   = ".debugMode";                  // boolean
    public static final String  CFG_showURL                     = ".showURL";                    // boolean
    public static final String  CFG_commandAckBit_              = ".commandAckBit.";             // integer

    // --------------------------------

    /**
    *** Return an array of "TrackServerAdapter class" property names
    *** @param name  The server name
    *** @return An array of "TrackServerAdapter class" property names
    **/
    public static String[] CONFIG_TrackServerAdapterClass(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_TrackServer_class,
            name + CFG_TrackServer_class 
        };
    }

    /**
    *** Return an array of "CommandPacketHandler class" property names
    *** @param name  The server name
    *** @return An array of "CommandPacketHandler class" property names
    **/
    public static String[] CONFIG_CommandPacketHandlerClass(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_CommandPacketHandler_class,
            name + CFG_CommandPacketHandler_class 
        };
    }

    /**
    *** Return an array of "ClientPacketHandler class" property names
    *** @param name  The server name
    *** @return An array of "ClientPacketHandler class" property names
    **/
    public static String[] CONFIG_ClientPacketHandlerClass(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_ClientPacketHandler_class,
            name + CFG_ClientPacketHandler_class 
        };
    }

    // --------------------------------

    /** 
    *** Return an array of "TCP port" property names
    *** @param name  The server name
    *** @return An array of "TCP port" property names
    **/
    public static String[] CONFIG_tcpPort(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_tcpPort,
            PROP_DCServer_ + name + CFG_port,
            name + CFG_tcpPort, 
            name + CFG_port 
        };
    }

    /**
    *** Return an array of "UDP port" property names
    *** @param name  The server name
    *** @return An array of "UDP port" property names
    **/
    public static String[] CONFIG_udpPort(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_udpPort, 
            PROP_DCServer_ + name + CFG_port,
            name + CFG_udpPort, 
            name + CFG_port 
        };
    }

    /** 
    *** Return an array of "SAT port" property names
    *** @param name  The server name
    *** @return An array of "SAT port" property names
    **/
    public static String[] CONFIG_satPort(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_satPort,
            name + CFG_satPort, 
        };
    }

    // --------------------------------

    /**
    *** Return an array of "Command port" property names
    *** @param name  The server name
    *** @return An array of "Command port" property names
    **/
    public static String[] CONFIG_commandPort(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_commandPort,
            name + CFG_commandPort 
        };
    }

    /**
    *** Return an array of "Command Protocol" property names
    *** Return command protocol to used when communicating with remote devices
    *** @param name  The server name
    *** @return An array of "Command Protocol" property names
    **/
    public static String[] CONFIG_commandProtocol(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_commandProtocol,
            name + CFG_commandProtocol 
        };
    }

    /**
    *** Return an array of "Client Command port" property names
    *** @param name  The server name
    *** @return An array of "Client Command port" property names
    **/
    public static String[] CONFIG_clientCommandPort_udp(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_clientCommandPort_udp, 
            PROP_DCServer_ + name + CFG_clientCommandPort,
            name + CFG_clientCommandPort_udp, 
            name + CFG_clientCommandPort 
        };
    }

    /**
    *** Return an array of "Client Command port" property names
    *** @param name  The server name
    *** @return An array of "Client Command port" property names
    **/
    public static String[] CONFIG_clientCommandPort_tcp(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_clientCommandPort_tcp, 
            PROP_DCServer_ + name + CFG_clientCommandPort,
            name + CFG_clientCommandPort_tcp, 
            name + CFG_clientCommandPort 
        };
    }

    /**
    *** Return an array of "ACK Response port" property names
    *** @param name  The server name
    *** @return An array of "ACK Response port" property names
    **/
    public static String[] CONFIG_ackResponsePort(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_ackResponsePort,
            name + CFG_ackResponsePort 
        };
    }

    // --------------------------------

    /**
    *** Return an array of "TCP idle timeout" property names
    *** @param name  The server name
    *** @return An array of "TCP idle timeout" property names
    **/
    public static String[] CONFIG_tcpIdleTimeoutMS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_tcpIdleTimeoutMS,
            name + CFG_tcpIdleTimeoutMS 
        };  // int
    }

    /**
    *** Return an array of "TCP packet timeout" property names
    *** @param name  The server name
    *** @return An array of "TCP packet timeout" property names
    **/
    public static String[] CONFIG_tcpPacketTimeoutMS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_tcpPacketTimeoutMS,
            name + CFG_tcpPacketTimeoutMS 
        };
    }

    /**
    *** Return an array of "TCP session timeout" property names
    *** @param name  The server name
    *** @return An array of "TCP session timeout" property names
    **/
    public static String[] CONFIG_tcpSessionTimeoutMS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_tcpSessionTimeoutMS,
            name + CFG_tcpSessionTimeoutMS 
        };
    }

    // --------------------------------

    /**
    *** Return an array of "UDP idle timeout" property names
    *** @param name  The server name
    *** @return An array of "UDP idle timeout" property names
    **/
    public static String[] CONFIG_udpIdleTimeoutMS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_udpIdleTimeoutMS 
        };  // int
    }

    /**
    *** Return an array of "UDP packet timeout" property names
    *** @param name  The server name
    *** @return An array of "UDP packet timeout" property names
    **/
    public static String[] CONFIG_udpPacketTimeoutMS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_udpPacketTimeoutMS 
        };  // int
    }

    /**
    *** Return an array of "UDP session timeout" property names
    *** @param name  The server name
    *** @return An array of "UDP session timeout" property names
    **/
    public static String[] CONFIG_udpSessionTimeoutMS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_udpSessionTimeoutMS
        };  // int
    }

    // --------------------------------

    /**
    *** Return an array of UniquID prefix property names
    *** @param name  The server name
    *** @return An array of UniquID prefix property names
    **/
    public static String[] CONFIG_uniquePrefix(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_uniquePrefix,
            PROP_DCServer_ + name + CFG_uniqueIdPrefix,
            name + CFG_uniquePrefix,
            name + CFG_uniqueIdPrefix
        };
    }

    // --------------------------------

    /**
    *** Return an array of "Minimum Moved Meters" property names
    *** @param name  The server name
    *** @return An array of "Minimum Moved Meters" names
    **/
    public static String[] CONFIG_minimumMovedMeters(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_minimumMovedMeters,
            name + CFG_minimumMovedMeters
        };
    }

    /**
    *** Return an array of "Minimum SpeedKPH" property names
    *** @param name  The server name
    *** @return An array of "Minimum SpeedKPH" names
    **/
    public static String[] CONFIG_minimumSpeedKPH(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_minimumSpeedKPH,
            name + CFG_minimumSpeedKPH
        };
    }

    /**
    *** Return an array of "Estimate Odometer" property names
    *** @param name  The server name
    *** @return An array of "Estimate Odometer" names
    **/
    public static String[] CONFIG_estimateOdometer(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_estimateOdometer,
            name + CFG_estimateOdometer
        };
    }

    /**
    *** Return an array of "Ignore Device Odometer" property names
    *** @param name  The server name
    *** @return An array of "Ignore Device Odometer" names
    **/
    public static String[] CONFIG_ignoreDeviceOdometer(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_ignoreDeviceOdometer,
            name + CFG_ignoreDeviceOdometer
        };
    }

    /**
    *** Return an array of "Simulate Geozone Arrival/Departure" property names
    *** @param name  The server name
    *** @return An array of "Simulate Geozone Arrival/Departure" names
    **/
    public static String[] CONFIG_simulateGeozones(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_simulateGeozones,
            name + CFG_simulateGeozones 
        };
    }

    /**
    *** Return an array of "Geozone Skip Old Events" property names
    *** @param name  The server name
    *** @return An array of "Geozone Skip Old Events" names
    **/
    public static String[] CONFIG_geozoneSkipOldEvents(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_geozoneSkipOldEvents,
            name + CFG_geozoneSkipOldEvents 
        };
    }

    /**
    *** Return an array of "Maximum HDOP" property names
    *** @param name  The server name
    *** @return An array of "Maximum HDOP" names
    **/
    public static String[] CONFIG_maximumHDOP(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_maximumHDOP,
            name + CFG_maximumHDOP
        };
    }

    /**
    *** Return an array of "Maximum Accuracy Meters" property names
    *** @param name  The server name
    *** @return An array of "Maximum Accuracy Meters" names
    **/
    public static String[] CONFIG_maximumAccuracyMeters(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_maximumAccuracyMeters,
            name + CFG_maximumAccuracyMeters
        };
    }

    /**
    *** Return an array of "Check Last Odometer" property names
    *** @param name  The server name
    *** @return An array of "Check Last Odometer" names
    **/
    public static String[] CONFIG_checkLastOdometer(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_checkLastOdometer,
            name + CFG_checkLastOdometer
        };
    }

    /**
    *** Return an array of "Simulate Digital Inputs" property names
    *** @param name  The server name
    *** @return An array of "Simulate Digital Inputs" names
    **/
    public static String[] CONFIG_simulateDigitalInputs(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_simulateDigitalInputs,
            name + CFG_simulateDigitalInputs
        };
    }
    /**
    *** Return an array of "Start/Stop StatusCode supported" property names
    *** @param name  The server name
    *** @return An array of "Start/Stop StatusCode supported" property names
    **/
    public static String[] CONFIG_startStopSupported(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_startStopSupported,
            name + CFG_startStopSupported 
        };
    }

    /**
    *** Return an array of "Save Raw Data Packet" property names
    *** @param name  The server name
    *** @return An array of "Save Raw Data Packet" property names
    **/
    public static String[] CONFIG_saveRawDataPackets(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_saveRawDataPackets,
            name + CFG_saveRawDataPackets 
        };
    }

    /**
    *** Return an array of "Status Location/InMotion Translation" property names
    *** @param name  The server name
    *** @return An array of "Status Location/InMotion Translation" property names
    **/
    public static String[] CONFIG_statusLocationInMotion(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_statusLocationInMotion,
            name + CFG_statusLocationInMotion
        };
    }

    /**
    *** Return an array of "Ignore Invalid GPS Flag" property names
    *** @param name  The server name
    *** @return An array of "Ignore Invalid GPS Flag" property names
    **/
    public static String[] CONFIG_ignoreInvalidGPSFlag(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_ignoreInvalidGPSFlag,
            name + CFG_ignoreInvalidGPSFlag
        };
    }

    /**
    *** Return an array of "Ignore Events with Invalid GPS" property names
    *** @param name  The server name
    *** @return An array of "Ignore Events with Invalid GPS" property names
    **/
    public static String[] CONFIG_ignoreEventsWithInvalidGPS(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_ignoreEventsWithInvalidGPS,
            name + CFG_ignoreEventsWithInvalidGPS
        };
    }

    /**
    *** Return an array of "Use Last Valid GPS Location" property names
    *** @param name  The server name
    *** @return An array of "Use Last Valid GPS Location" property names
    **/
    public static String[] CONFIG_useLastValidGPSLocation(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_useLastValidGPSLocation,
            name + CFG_useLastValidGPSLocation
        };
    }

    /**
    *** Return an array of "Use Alternate Current Timestamp for Event" property names
    *** @param name  The server name
    *** @return An array of "Use Alternate Current Timestamp for Event" property names
    **/
    public static String[] CONFIG_useAltCurrentTimestamp(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_useAltCurrentTimestamp,
            name + CFG_useAltCurrentTimestamp
        };
    }

    /**
    *** Return an array of "Initial Packet" property names
    *** @param name  The server name
    *** @return An array of "Initial Packet" property names
    **/
    public static String[] CONFIG_initialPacket(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_initialPacket,
            name + CFG_initialPacket
        };
    }

    /**
    *** Return an array of "Final Packet" property names
    *** @param name  The server name
    *** @return An array of "Final Packet" property names
    **/
    public static String[] CONFIG_finalPacket(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_finalPacket,
            name + CFG_finalPacket
        };
    }

    /**
    *** Return an array of "Save Session Statistics" property names
    *** @param name  The server name
    *** @return An array of "Save Session Statistics" property names
    **/
    public static String[] CONFIG_saveSessionStatistics(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_saveSessionStatistics,
            name + CFG_saveSessionStatistics
        };
    }

    /**
    *** Return an array of "Battery Level Range" property names
    *** @param name  The server name
    *** @return An array of "Battery Level Range" property names
    **/
    public static String[] CONFIG_batteryLevelRange(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_batteryLevelRange,
            name + CFG_batteryLevelRange
        };
    }

    /**
    *** Return an array of "Start/Stop StatusCodes" property names
    *** @param name  The server name
    *** @return An array of "Start/Stop StatusCodes" property names
    **/
    public static String[] CONFIG_startStopStatusCodes(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_startStopStatusCodes,
            name + CFG_startStopStatusCodes
        };
    }

    /**
    *** Return an array of "Debug Mode" property names
    *** @param name  The server name
    *** @return An array of "Debug Mode" property names
    **/
    public static String[] CONFIG_debugMode(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_debugMode,
            name + CFG_debugMode
        };
    }

    /**
    *** Return an array of "Show URL" property names
    *** @param name  The server name
    *** @return An array of "Show URL" property names
    **/
    public static String[] CONFIG_showURL(String name)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_showURL,
            name + CFG_showURL
        };
    }

    /**
    *** Return an array of "Command ACK bit" property names
    *** @param name  The server name
    *** @return An array of "Command ACK bit" property names
    **/
    public static String[] CONFIG_commandAckBit(String name, String bitName)
    {
        return new String[] { 
            PROP_DCServer_ + name + CFG_commandAckBit_ + bitName,
            name + CFG_commandAckBit_ + bitName
        };
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // test/debug entry point

    private static final String ARG_LIST[]              = new String[] { "list"                        };
    private static final String ARG_LOOKUP[]            = new String[] { "lookup"       , "find"       };
    private static final String ARG_SERVER[]            = new String[] { "server"       , "dcs"        };
    private static final String ARG_ACCOUNT[]           = new String[] { "account"      , "acct"       };
    private static final String ARG_DEVICE[]            = new String[] { "device"       , "dev"        };
    private static final String ARG_CMDTYPE[]           = new String[] { "cmdType"      , "ct"         };
    private static final String ARG_CMDNAME[]           = new String[] { "cmdName"      , "cn"         };
    private static final String ARG_ARG[]               = new String[] { "arg"          , "a"          };
    private static final String ARG_RESULT_CODE[]       = new String[] { "resultCode"   , "rc"         };
    private static final String ARG_EVENT_CODE_MAP[]    = new String[] { "eventCodeMap" , "ecm"        };
    private static final String ARG_CALC_ANALOG[]       = new String[] { "calcAnalog"   , "gainOffset" };

    /**
    *** Command-Line usage
    **/
    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + DCServerFactory.class.getName() + " {options}");
        Print.logInfo("'Lookup' Options:");
        Print.logInfo("  -lookup=<mobileID>      A device mobile-id");
        Print.logInfo("'Send' Options:");
        Print.logInfo("  -server=<serverID>      The DCS id");
        Print.logInfo("  -account=<accountID>    The account id");
        Print.logInfo("  -device=<deviceID>      The device id");
        Print.logInfo("  -cmdType=<command>      The command [send|query|ping|output|config]");
        Print.logInfo("  -cmdName=<name>         The command name (if any)");
        Print.logInfo("  -arg=<arg>              The command argument");
        System.exit(1);
    }

    /**
    *** Command-line main entry point
    **/
    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args, true);
        String server    = RTConfig.getString(ARG_SERVER     , "");
        String accountID = RTConfig.getString(ARG_ACCOUNT    , "");
        String deviceID  = RTConfig.getString(ARG_DEVICE     , "");
        String cmdType   = RTConfig.getString(ARG_CMDTYPE    , "");
        String cmdName   = RTConfig.getString(ARG_CMDNAME    , "");
        String cmdArg0   = RTConfig.getString(ARG_ARG        , "");
        String resCode   = RTConfig.getString(ARG_RESULT_CODE, "");
        String cmdArgs[] = new String[] { cmdArg0 };

        /* calculate Analog Gain/Offset for a percent(%) result */
        if (RTConfig.hasProperty(ARG_CALC_ANALOG)) {
            double R[] = RTConfig.getDoubleArray(ARG_CALC_ANALOG,null);
            if (ListTools.size(R) < 2) {
                Print.sysPrintln("ERROR: invalid voltage range specification");
            } else
            if (R[0] == R[1]) {
                Print.sysPrintln("ERROR: Invalid voltage range specified");
            } else {
                // PERCENT = (GAIN * VALUE) + OFFSET
                double EMPTY  = R[0];
                double FULL   = R[1];
                double GAIN   = 1.0 / (FULL - EMPTY);
                double OFFSET = -(EMPTY / (FULL - EMPTY));
                Print.sysPrintln("GAIN="+GAIN + ", OFFSET="+OFFSET);
            }
            System.exit(0);
        }

        /* ResultCode test */
        if (RTConfig.hasProperty(ARG_RESULT_CODE)) {
            String code = RTConfig.getString(ARG_RESULT_CODE,"");
            ResultCode rc = GetResultCode(code,null);
            Print.sysPrintln("Result Code: " + rc);
            System.exit(0);
        }

        /* event code map */
        if (RTConfig.hasProperty(ARG_EVENT_CODE_MAP)) {
            String dcsName = !StringTools.isBlank(server)? server : RTConfig.getString(ARG_EVENT_CODE_MAP,"");
            DCServerConfig dcs = DCServerFactory.getServerConfig(dcsName);
            if (dcs == null) {
                Print.sysPrintln("ERROR - DCS not found: " + dcsName);
                System.exit(1);
            }
            dcs.printEventCodeMap();
            System.exit(0);
        }

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            java.util.List<DCServerConfig> list = getServerConfigList(true);
            for (DCServerConfig dcs : list) {
                Print.sysPrintln(dcs.getName() + ":");
                File jarPath[] = dcs.getRunningJarPath(); 
                if (!ListTools.isEmpty(jarPath)) {
                    if (jarPath.length == 1) {
                        Print.sysPrintln("  Jar: " + jarPath[0]);
                        File logPath = DCServerConfig.getLogFilePath(jarPath[0]);
                        Print.sysPrintln("  Log: " + logPath + (logPath.isFile()?"  [exists]":"  [not-found]"));
                    } else {
                        for (int i = 0; i < jarPath.length; i++) {
                            Print.sysPrintln("  "+(i+1)+") Jar: " + jarPath[i]);
                            File logPath = DCServerConfig.getLogFilePath(jarPath[i]);
                            Print.sysPrintln("     Log: " + logPath + (logPath.isFile()?"  [exists]":"  [not-found]"));
                        }
                    }
                } else {
                    Print.sysPrintln("  not running");
                }
            }
            System.exit(0);
        }

        /* lookup IMEI# */
        if (RTConfig.hasProperty(ARG_LOOKUP)) {
            String uid = RTConfig.getString(ARG_LOOKUP,null);
            if (!StringTools.isBlank(uid)) {
                Device dev[] = DCServerFactory.lookupUniqueID(uid);
                Print.sysPrintln("");
                Print.sysPrintln("Device UniqueID Lookup:");
                if (!ListTools.isEmpty(dev)) {
                    for (Device d : dev) {
                        Print.sysPrintln("   Found: ["+d.getUniqueID()+"] " + d.getAccountID() + "/" + d.getDeviceID());
                    }
                } else {
                    Print.sysPrintln("   None Found");
                }
                Print.sysPrintln("");
            } else {
                Print.sysPrintln("Missing IMEI/Mobile ID");
                usage();
            }
            System.exit(0);
        }

        // -------------------------------------------------------

        /* pre-check DCS existance */
        if (StringTools.isBlank(server)) {
            Print.sysPrintln("ERROR: server missing");
            usage();
        }
        Print.logDebug("Checking DCServerConfig existance: %s", server);
        DCServerConfig dcs = DCServerFactory.getServerConfig(server);
        if (dcs == null) {
            Print.sysPrintln("ERROR: Invalid server id: %s", server);
            usage();
        }

        /* command type blank? */
        if (StringTools.isBlank(cmdType)) {
            Print.sysPrintln("ERROR: command type missing");
            usage();
        }

        // -------------------------------------------------------

        /* internal/stacktrace */
        if (cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_INTERNAL)) {
            //  bin/exeJava org.opengts.db.DCServerFactory -cmdType=internal -cmdName='%COMMANDS%' -dcs=qgv300
            //or
            //  (echo 'cmdtype=internal cmdname=%COMMANDS%'; sleep 1) | telnet dcserver 30500
            //or
            //  (echo 'cmdtype=internal cmdname=%COMMANDS%') | nc dcserver 30500
            // --
            RTProperties stResp = DCServerFactory._sendServerCommand(server,null,cmdType,cmdName,cmdArgs);
            if (stResp == null) {
                Print.sysPrintln("Unable to send internal command");
                System.exit(2);
            }
            Print.sysPrintln("Internal Command Response: " + stResp);
            System.exit(0);
        }

        // -------------------------------------------------------

        /* blank account/device? */
        if (StringTools.isBlank(accountID) || StringTools.isBlank(deviceID)) {
            Print.sysPrintln("ERROR: account/device missing");
            usage();
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID); // may throw DBException
            if (account == null) {
                Print.logError("Account-ID does not exist: " + accountID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + accountID, dbe);
            System.exit(99);
        }

        /* get device */
        Device device = null;
        try {
            device = Device.getDevice(account, deviceID, false); // may throw DBException
            if (device == null) {
                Print.logError("Device-ID does not exist: " + accountID + "/" + deviceID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error getting Device: " + accountID + "/" + deviceID, dbe);
            System.exit(99);
        }

        // -------------------------------------------------------

        /* send */
        RTProperties resp = DCServerFactory._sendServerCommand(server, device, cmdType, cmdName, cmdArgs);
        if (resp == null) {
            Print.sysPrintln("Unable to send command");
            System.exit(2);
        }
        Print.sysPrintln("Command Response: " + resp);
        
    }
    // ------------------------------------------------------------------------

}
    
