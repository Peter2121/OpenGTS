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
//  Device Communication Server configuration (central registry for port usage)
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release
//  2009/07/01  Martin D. Flynn
//     -Added support for sending commands to the appropriate DCS.
//  2009/08/23  Martin D. Flynn
//     -Added several additional common runtime property methods.
//  2009/09/23  Martin D. Flynn
//     -Changed 'getSimulateDigitalInputs' to return a mask
//  2011/05/13  Martin D. Flynn
//     -Added "getMaximumHDOP"
//  2011/08/21  Martin D. Flynn
//     -Added "getIgnoreDeviceOdometer()"
//  2013/05/28  Martin D. Flynn
//     -Added aditional argument adjustment types (see "_adjustKeyArg")
//     -Changed CommandProtocol.SMS to value '9' (was '2').
//  2013/11/11  Martin D. Flynn
//     -Added "getMaximumAccuracyMeters"
//     -Added "addRecommendedConfigPropertyKey"/"getRecommendedConfigPropertyKeys"
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

public class DCServerConfig
    implements Comparable
{

    // ------------------------------------------------------------------------
    // flags

    // default property group id
    public static final String  DEFAULT_PROP_GROUP_ID           = "default";

    // Boolean Properties
    public static final String  P_NONE                          = "none";
    public static final String  P_HAS_INPUTS                    = "hasInputs";
    public static final String  P_HAS_OUTPUTS                   = "hasOutputs";
    public static final String  P_COMMAND_SMS                   = "commandSms";
    public static final String  P_COMMAND_UDP                   = "commandUdp";
    public static final String  P_COMMAND_TCP                   = "commandTcp";
    public static final String  P_XMIT_TCP                      = "transmitTcp";
    public static final String  P_XMIT_UDP                      = "transmitUdp";
    public static final String  P_XMIT_SMS                      = "transmitSms";
    public static final String  P_XMIT_SAT                      = "transmitSat";
    public static final String  P_JAR_OPTIONAL                  = "jarOptional";

    public static final long    F_NONE                          = 0x00000000L;
    public static final long    F_HAS_INPUTS                    = 0x00000002L; // hasInputs
    public static final long    F_HAS_OUTPUTS                   = 0x00000004L; // hasOutputs
    public static final long    F_COMMAND_TCP                   = 0x00000100L; // commandTcp
    public static final long    F_COMMAND_UDP                   = 0x00000200L; // commandUdp
    public static final long    F_COMMAND_SMS                   = 0x00000400L; // commandSms
    public static final long    F_XMIT_TCP                      = 0x00001000L; // transmitTcp
    public static final long    F_XMIT_UDP                      = 0x00002000L; // transmitUdp
    public static final long    F_XMIT_SMS                      = 0x00004000L; // transmitSms
    public static final long    F_XMIT_SAT                      = 0x00008000L; // transmitSat
    public static final long    F_JAR_OPTIONAL                  = 0x00010000L; // jarOptional

    public static final long    F_STD_VEHICLE                   = F_HAS_INPUTS | F_HAS_OUTPUTS | F_XMIT_TCP | F_XMIT_UDP;
    public static final long    F_STD_PERSONAL                  = F_XMIT_TCP | F_XMIT_UDP;

    /**
    *** Gets the attribute flag mask for the specified RTProperties instance.
    *** @param rtp The RTProperties instance constructed from the "Attributes" tag
    ***            in the DCS "dcserver_<i>XXXX</i>.xml" file.
    *** @return The attribute flag mask
    **/
    public static long GetAttributeFlags(RTProperties rtp)
    {
        long flags = 0L;
        if (rtp.getBoolean(P_HAS_INPUTS  ,false)) { flags |= F_HAS_INPUTS  ; }
        if (rtp.getBoolean(P_HAS_OUTPUTS ,false)) { flags |= F_HAS_OUTPUTS ; }
        if (rtp.getBoolean(P_COMMAND_SMS ,false)) { flags |= F_COMMAND_SMS ; }
        if (rtp.getBoolean(P_COMMAND_UDP ,false)) { flags |= F_COMMAND_UDP ; }
        if (rtp.getBoolean(P_COMMAND_TCP ,false)) { flags |= F_COMMAND_TCP ; }
        if (rtp.getBoolean(P_XMIT_TCP    ,false)) { flags |= F_XMIT_TCP    ; }
        if (rtp.getBoolean(P_XMIT_UDP    ,false)) { flags |= F_XMIT_UDP    ; }
        if (rtp.getBoolean(P_XMIT_SMS    ,false)) { flags |= F_XMIT_SMS    ; }
        if (rtp.getBoolean(P_XMIT_SAT    ,false)) { flags |= F_XMIT_SAT    ; }
        if (rtp.getBoolean(P_JAR_OPTIONAL,false)) { flags |= F_JAR_OPTIONAL; }
        return flags;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public enum CommandProtocol implements EnumTools.IsDefault, EnumTools.IntValue {
        UDP(0,"udp"),
        TCP(1,"tcp"),
        SMS(9,"sms");
        // ---
        private int         vv = 0;
        private String      ss = "";
        CommandProtocol(int v, String s) { vv = v; ss = s; }
        public int getIntValue()   { return vv; }
        public String toString()   { return ss; }
        public boolean isDefault() { return this.equals(UDP); }
        public boolean isSMS()     { return this.equals(SMS); }
    };

    /**
    *** Gets the CommandProtocol Enum value, based on the value of the specified String 
    *** @param v The command protocol name
    *** @return The CommandProtocol Enum value
    **/
    public static CommandProtocol getCommandProtocol(String v)
    {
        // returns 'null' if protocol value is invalid
        return EnumTools.getValueOf(CommandProtocol.class, v);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Device event code to status code translation
    **/
    public static class EventCode
    {
        private int    oldCode     = 0;
        private int    statusCode  = StatusCodes.STATUS_NONE;
        private String dataString  = null;
        private long   dataLong    = Long.MIN_VALUE;
        public EventCode(int oldCode, int statusCode, String data) {
            this.oldCode    = oldCode;
            this.statusCode = statusCode;
            this.dataString = data;
            this.dataLong   = StringTools.parseLong(data, Long.MIN_VALUE);
        }
        public int getCode() {
            return this.oldCode;
        }
        public int getStatusCode() {
            return this.statusCode;
        }
        public String getDataString(String dft) {
            return !StringTools.isBlank(this.dataString)? this.dataString : dft;
        }
        public long getDataLong(long dft) {
            return (this.dataLong != Long.MIN_VALUE)? this.dataLong : dft;
        }
        public String toString() {
            return StringTools.toHexString(this.getCode(),16) + " ==> 0x" + StringTools.toHexString(this.getStatusCode(),16);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Perl 'psjava' command (relative to GTS_HOME) */
    private static final String PSJAVA_PERL  = File.separator + "bin" + File.separator + "psjava";

    /**
    *** Returns the "psjava" command relative to GTS_HOME
    *** @return The "psjava" command relative to GTS_HOME
    **/
    public static String getPSJavaCommand()
    {
        File psjava = FileTools.toFile(DBConfig.get_GTS_HOME(), new String[] {"bin","psjava"});
        if (psjava != null) {
            return psjava.toString();
        } else {
            return null;
        }
    }

    /**
    *** Returns the "psjava" command relative to GTS_HOME, and returning the 
    *** specified information for the named jar file
    *** @param name    The DCServerConfig name
    *** @param display The type of information to return ("pid", "name", "user")
    *** @return The returned 'display' information for the specified DCServerConfig
    **/
    public static String getPSJavaCommand_jar(String name, String display)
    {
        String psjava = DCServerConfig.getPSJavaCommand();
        if (!StringTools.isBlank(psjava)) {
            StringBuffer sb = new StringBuffer();
            sb.append(psjava);
            if (OSTools.isWindows()) {
                sb.append(" \"-jar=").append(name).append(".jar\"");
                if (!StringTools.isBlank(display)) {
                    sb.append(" \"-display="+display+"\"");
                }
            } else {
                sb.append(" -jar=").append(name).append(".jar");
                if (!StringTools.isBlank(display)) {
                    sb.append(" -display="+display+"");
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }
    
    /**
    *** Returns the file path for the named running DCServerConfig jar files.<br>
    *** This method will return 'null' if no DCServerConfig jar files with the specified
    *** name are currently running.<br>
    *** All matching running DCServerConfig entries will be returned.
    *** @param name  The DCServerConfig name
    *** @return The matching running jar file paths, or null if no matching server enteries are running.
    **/
    public static File[] getRunningJarPath(String name)
    {
        if (OSTools.isLinux() || OSTools.isMacOS()) {
            try {
                String cmd = DCServerConfig.getPSJavaCommand_jar(name,"name");
                Process process = (cmd != null)? Runtime.getRuntime().exec(cmd) : null;
                if (process != null) {
                    BufferedReader procReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    for (;;) {
                        String line = procReader.readLine();
                        if (line == null) { break; }
                        sb.append(StringTools.trim(line));
                    }
                    process.waitFor();
                    procReader.close();
                    int exitVal = process.exitValue();
                    if (exitVal == 0) {
                        String jpath[] = StringTools.split(sb.toString(), '\n');
                        java.util.List<File> jpl = new Vector<File>();
                        for (int i = 0; i < jpath.length; i++) {
                            if (StringTools.isBlank(jpath[i])) { continue; }
                            File jarPath = new File(sb.toString());
                            try {
                                jpl.add(jarPath.getCanonicalFile());
                            } catch (Throwable th) {
                                jpl.add(jarPath);
                            }
                        }
                        if (!ListTools.isEmpty(jpl)) {
                            return jpl.toArray(new File[jpl.size()]);
                        }
                    }
                } else {
                    if (StringTools.isBlank(cmd)) {
                        Print.logWarn("Unable to create 'psjava' command for '"+name+"'");
                    } else {
                        Print.logError("Unable to execute command: " + cmd);
                    }
                }
            } catch (Throwable th) {
                Print.logException("Unable to determine if Tomcat is running:", th);
            }
            return null;
        } else {
            // not supported on Windows
            return null;
        }
    }

    /**
    *** Return log file paths from jar file paths
    **/
    public static File getLogFilePath(File jarPath)
    {
        if (jarPath != null) {
            // "/usr/local/GTS_1.2.3/build/lib/enfora.jar"
            String jarName = jarPath.getName();
            if (jarName.endsWith(".jar")) {
                String name = jarName.substring(0,jarName.length()-4);
                File logDir = new File(jarPath.getParentFile().getParentFile().getParentFile(), "logs");
                if (logDir.isDirectory()) {
                    return new File(logDir, name + ".log");
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class EventDataAnalogField
    {
        private int         index       = 0;
        private double      gain        = 1.0 / (double)(1L << 12);  // default 12-bit analog
        private double      offset      = 0.0;
        private DBField     dbField     = null;
        public EventDataAnalogField(int ndx, double gain, double offset) {
            this(ndx, gain, offset, null);
        }
        public EventDataAnalogField(int ndx, double gain, double offset, String fieldN) {
            this.index  = ndx;
            this.gain   = gain;
            this.offset = offset;
            this.setFieldName(fieldN);
            Print.logInfo("New AnalogField["+this.index+"]: " + this);
        }
        public EventDataAnalogField(int ndx, String gof, double dftGain, double dftOffset) {
            this.index  = ndx;
            String  v[] = StringTools.split(gof,',');
            this.gain   = (v.length >= 1)? StringTools.parseDouble(v[0],dftGain  ) : dftGain;
            this.offset = (v.length >= 2)? StringTools.parseDouble(v[1],dftOffset) : dftOffset;
            this.setFieldName((v.length >= 3)? StringTools.blankDefault(v[2],null) : null);
            Print.logInfo("New AnalogField["+this.index+"]: " + this);
        }
        public int getIndex() {
            return this.index;
        }
        public void setGain(double gain) {
            this.gain = gain;
        }
        public double getGain() {
            return this.gain;
        }
        public void setOffset(double offset) {
            this.offset = offset;
        }
        public double getOffset() {
            return this.offset;
        }
        public double convert(long value) {
            return this.convert((double)value);
        }
        public double convert(double value) {
            return (value * this.gain) + this.offset;
        }
        public void setFieldName(String fieldN) {
            this.dbField = null;
            if (!StringTools.isBlank(fieldN)) {
                DBField dbf = EventData.getFactory().getField(fieldN);
                if (dbf == null) {
                    Print.logError("**** EventData analog field does not exist: " + fieldN);
                } else {
                    this.dbField = dbf;
                    Object val = this.getValueObject(0.0);
                    if (val == null) {
                        Print.logError("**** EventData field type not supported: " + fieldN);
                        this.dbField = null;
                    }
                }
            }
        }
        public DBField getDBField() {
            return this.dbField;
        }
        public String getFieldName() {
            return (this.dbField != null)? this.dbField.getName() : null;
        }
        public Object getValueObject(double value) {
            // DBField
            DBField dbf = this.getDBField();
            if (dbf == null) {
                return null;
            }
            // convert to type
            Class dbfc = dbf.getTypeClass();
            if (dbfc == String.class) {
                return String.valueOf(value);
            } else
            if ((dbfc == Integer.class) || (dbfc == Integer.TYPE)) {
                return new Integer((int)value);
            } else
            if ((dbfc == Long.class)    || (dbfc == Long.TYPE   )) {
                return new Long((long)value);
            } else
            if ((dbfc == Float.class)   || (dbfc == Float.TYPE  )) {
                return new Float((float)value);
            } else
            if ((dbfc == Double.class)  || (dbfc == Double.TYPE )) {
                return new Double(value);
            } else
            if ((dbfc == Boolean.class) || (dbfc == Boolean.TYPE)) {
                return new Boolean(value != 0.0);
            }
            return null;
        }
        public boolean saveEventDataFieldValue(EventData evdb, double value) {
            if (evdb != null) {
                Object objVal = this.getValueObject(value); // null if no dbField
                if (objVal != null) {
                    String fn = this.getFieldName();
                    boolean ok = evdb.setFieldValue(fn, objVal);
                    Print.logInfo("Set AnalogField["+this.getIndex()+"]: "+fn+" ==> " + (ok?evdb.getFieldValue(fn):"n/a"));
                    return ok;
                }
            }
            return false;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("index="  ).append(this.getIndex());
            sb.append(" gain="  ).append(this.getGain());
            sb.append(" offset=").append(this.getOffset());
            sb.append(" field=" ).append(this.getFieldName());
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String DecodeEscapeChars(String s)
    {
        if (s == null) {
            return "";
        } else {
            s = StringTools.replace(s,"\\n","\n");
            s = StringTools.replace(s,"\\r","\r");
            return s;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  COMMAND_CONFIG                  = "config";     // arg=deviceCommandString
    public static final String  COMMAND_PING                    = "ping";       // arg=commandID
  //public static final String  COMMAND_OUTPUT                  = "output";     // arg=gpioOutputState
    public static final String  COMMAND_GEOZONE                 = "geozone";    // arg=""
    public static final String  COMMAND_GFMI                    = "gfmi";       // arg=deviceCommandString
    public static final String  COMMAND_DIAGNOSTIC              = "diagnostic"; // arg=diagCommandString
    public static final String  COMMAND_INTERNAL                = "internal";   // arg=""

    public static final String  DIAG_LOG[]                      = { "log", "active", "sessions" };

    public static final String  DEFAULT_ARG_NAME                = "arg";
    
    public static final String  KEYARG_HEX8[]                   = { "h8"   , "hex8"    };               // return arg as 8-bit hex value
    public static final String  KEYARG_HEX16[]                  = { "h16"  , "hex16"   };               // return arg as 16-bit hex value
    public static final String  KEYARG_HEX32[]                  = { "h32"  , "hex32"   };               // return arg as 32-bit hex value
    public static final String  KEYARG_HEX64[]                  = { "h64"  , "hex64"   };               // return arg as 64-bit hex value
    public static final String  KEYARG_INT[]                    = { "#"    , "int"     , "long"      }; // arg is an integer
    public static final String  KEYARG_QUOTE[]                  = { "q"    , "quote"   };               // quote arg
    public static final String  KEYARG_GPS[]                    = { "gp"   , "gps"     };               // format arg as lat/lon
    public static final String  KEYARG_GPLAT[]                  = { "gplat", "lat"     , "latitude"  }; // extract latitude from arg
    public static final String  KEYARG_GPLON[]                  = { "gplon", "lon"     , "longitude" }; // extract longitude from arg
    public static final String  KEYARG_NOSPACE[]                = { "ns"   , "nospace" };               // remove spaces from arg

    public static final String  JSONKey_DCS                     = "DCS";
    public static final String  JSONKey_Commands                = "Commands";
    public static final String  JSONKey_Name                    = "Name";
    public static final String  JSONKey_Description             = "Description";
    public static final String  JSONKey_ReadOnly                = "ReadOnly";
    public static final String  JSONKey_ResourceName            = "ResourceName";
    public static final String  JSONKey_DefaultValue            = "DefaultValue";
    public static final String  JSONKey_DisplayLength           = "DisplayLength";
    public static final String  JSONKey_MaximumLength           = "MaximumLength";
    public static final String  JSONKey_Enabled                 = "Enabled";
    public static final String  JSONKey_Types                   = "Types";
    public static final String  JSONKey_AclName                 = "AclName";
    public static final String  JSONKey_AclDefault              = "AclDefault";
    public static final String  JSONKey_Protocol                = "Protocol";
    public static final String  JSONKey_CommandString           = "CommandString";
    public static final String  JSONKey_MaximumRouteAge         = "MaximumRouteAge";
    public static final String  JSONKey_AllowQueue              = "AllowQueue";
    public static final String  JSONKey_ExpectAck               = "ExpectAck";
    public static final String  JSONKey_ExpectAckCode           = "ExpectAckCode";
    public static final String  JSONKey_StateBitMask            = "StateBitMask";
    public static final String  JSONKey_StateBitValue           = "StateBitValue";
    public static final String  JSONKey_AuditCode               = "AuditCode";
    public static final String  JSONKey_Args                    = "Args";

    public static final char    GP_SEP[]                        = { '/', ',', ' ' };
    public static final char    CSV_SEP[]                       = { ',' };

    public static class Command
    {
        private String                  dcsName         = "";
        private String                  cmdName         = "";
        private String                  cmdDesc         = "";
        private boolean                 isEnabled       = true;
        private String                  types[]         = null;
        private String                  aclName         = "";
        private AclEntry.AccessLevel    aclDft          = AclEntry.AccessLevel.WRITE;
        private String                  cmdStr          = "";
        private boolean                 hasArgs         = false;
        private String                  cmdProto        = "";
        private String                  protoHandlr     = null;
        private long                    maxRouteAgeSec  = -1L;
        private boolean                 allowQueue      = false;
        private boolean                 expectAck       = false;
        private int                     expAckCode      = StatusCodes.STATUS_NONE;
        private long                    stateMask       = 0x0000L;
        private boolean                 stateVal        = false;
        private int                     cmdStCode       = StatusCodes.STATUS_NONE; // auditCode
        private OrderedMap<String,CommandArg> argMap = null;
        public Command(String dcsName, JSON._Object obj) throws JSON.JSONParsingException {
            // This is used for populating Command configuration from the specific DCS 
            if (obj == null) {
                throw new JSON.JSONParsingException("JSON Object is null", null);
            }
            this.dcsName        = StringTools.trim(dcsName);
            this.cmdName        = obj.getStringForName(     JSONKey_Name       , "");
            this.cmdDesc        = obj.getStringForName(     JSONKey_Description, "");
            this.isEnabled      = obj.getBooleanForName(    JSONKey_Enabled    , true);
            this.types          = obj.getStringArrayForName(JSONKey_Types      , null);
            this.aclName        = obj.getStringForName(     JSONKey_AclName    , "");
            this.aclDft         = AclEntry.parseAccessLevel(obj.getStringForName(JSONKey_AclDefault,null),AclEntry.AccessLevel.WRITE);
            String cmdProtoH    = obj.getStringForName(     JSONKey_Protocol   , "");
            if (cmdProtoH.indexOf(":") >= 0) {
                int p = cmdProtoH.indexOf(":");
                this.cmdProto    = StringTools.trim(cmdProtoH.substring(0,p));
                this.protoHandlr = StringTools.trim(cmdProtoH.substring(p+1));
            } else {
                this.cmdProto    = cmdProtoH;
                this.protoHandlr = null;
            }
            this.cmdStr         = DecodeEscapeChars(obj.getStringForName(JSONKey_CommandString,""));
            this.maxRouteAgeSec = obj.getLongForName(   JSONKey_MaximumRouteAge, 0L);
            this.allowQueue     = obj.getBooleanForName(JSONKey_AllowQueue     , false);
            this.expectAck      = obj.getBooleanForName(JSONKey_ExpectAck      , false);
            this.expAckCode     = obj.getIntForName(    JSONKey_ExpectAckCode  , StatusCodes.STATUS_NONE);
            this.stateMask      = obj.getLongForName(   JSONKey_StateBitMask   , 0L);
            this.stateVal       = obj.getBooleanForName(JSONKey_StateBitValue  , true);
            this.cmdStCode      = obj.getIntForName(    JSONKey_AuditCode      , StatusCodes.STATUS_NONE);
            JSON._Array args    = obj.getArrayForName(  JSONKey_Args           , null);
            if (!ListTools.isEmpty(args)) {
                this.hasArgs    = true;
                this.argMap     = new OrderedMap<String,CommandArg>();
                for (int i = 0; i < args.size(); i++) {
                    JSON._Object arg = args.getObjectValueAt(i,null);
                    if (arg != null) {
                        CommandArg cmdArg = new CommandArg(arg);
                        cmdArg.setCommand(this);
                        this.argMap.put(cmdArg.getName(),cmdArg);
                    } else {
                        throw new JSON.JSONParsingException("Command '"+this.cmdName+"': Invalid Arg at #" + i, null);
                    }
                }
            } else {
                this.hasArgs    = (this.cmdStr.indexOf("${") >= 0)? true : false;
            }
            // TODO
        }
        public Command(
            String  dcsName  , 
            String  cmdName  , String desc, boolean enable,
            String  types[]  , String aclName, AclEntry.AccessLevel aclDft,
            String  cmdStr   , boolean hasArgs, Collection<CommandArg> cmdArgs,
            String  cmdProtoH, long maxRteAge, boolean allowQueue,
            boolean expectAck, int expAckCode,
            long    stateMask, boolean stateVal,
            int     cmdStCode) {
            this.dcsName        = StringTools.trim(dcsName);
            this.cmdName        = StringTools.trim(cmdName);
            this.cmdDesc        = StringTools.trim(desc);
            this.isEnabled      = enable; // cmdEnable
            this.types          = (types != null)? types : new String[0];
            this.aclName        = StringTools.trim(aclName);
            this.aclDft         = (aclDft != null)? aclDft : AclEntry.AccessLevel.WRITE;
            //this.cmdStr    = (cmdStr != null)? cmdStr : ""; // DecodeEscapeChars
            this.cmdStr         = DecodeEscapeChars(cmdStr);
            //if (!this.cmdStr.equals(cmdStr)) {Print.logInfo("\""+cmdStr+"\" ==> \""+this.cmdStr+"\"");}
            this.hasArgs        = hasArgs || (this.cmdStr.indexOf("${") >= 0);
            cmdProtoH           = StringTools.trim(cmdProtoH);
            if (cmdProtoH.indexOf(":") >= 0) {
                int p = cmdProtoH.indexOf(":");
                this.cmdProto    = StringTools.trim(cmdProtoH.substring(0,p));
                this.protoHandlr = StringTools.trim(cmdProtoH.substring(p+1));
            } else {
                this.cmdProto    = cmdProtoH;
                this.protoHandlr = null;
            }
            this.maxRouteAgeSec = maxRteAge;
            this.allowQueue     = allowQueue;
            this.expectAck      = expectAck;
            this.expAckCode     = (expAckCode > 0)? expAckCode : StatusCodes.STATUS_NONE;
            this.stateMask      = stateMask;
            this.stateVal       = stateVal;
            this.cmdStCode      = (cmdStCode  > 0)? cmdStCode  : StatusCodes.STATUS_NONE;
            if (!ListTools.isEmpty(cmdArgs) && this.hasArgs) {
                this.argMap = new OrderedMap<String,CommandArg>();
                for (CommandArg arg : cmdArgs) {
                    arg.setCommand(this);
                    this.argMap.put(arg.getName(),arg);
                }
            }
            /*
            if (this.cmdName.equals("GFMITextMessage")) {
                Print.logInfo("Name   : " + this.cmdName);
                Print.logInfo("CmdStr : " + this.cmdStr);
                Print.logInfo("HasArgs: " + this.hasArgs);
                Print.logInfo("Args[] : " + StringTools.join(cmdArgs,","));
            }
            */
        }
        public String getServerName() {
            return this.dcsName;
        }
        public String getName() {
            return this.cmdName; // not null
        }
        public String getDescription() {
            return this.cmdDesc; // not null
        }
        public boolean isEnabled(BasicPrivateLabel privLabel) {
            String dcsn = this.getServerName();
            if ((privLabel != null) && !StringTools.isBlank(dcsn)) {
                String rtKey = // DCServer.DCSNAME.Command.CMDNAME.enabled
                    DCServerFactory.PROP_DCServer_ + dcsn + "." + DCServerFactory.PROP_Command_ + 
                    this.getName() + "." + DCServerFactory.ATTR_enabled;
                if (privLabel.hasProperty(rtKey)) {
                    // IE:
                    //  DCServer.calamp.Command.StarterDisableSMS.enabled=true
                    //  DCServer.calamp.Command.StarterEnableSMS.enabled=true
                    boolean cmdEna = privLabel.getBooleanProperty(rtKey, true);
                    //Print.logInfo("Found key ["+privLabel.getName()+"]: " + rtKey + " ==> " + cmdEna);
                    return cmdEna;
                } else {
                    //Print.logInfo("Key not found ["+privLabel.getName()+"]: " + rtKey);
                }
            }
            return this.isEnabled;
        }
        public String[] getTypes() {
            return this.types;
        }
        public boolean isType(String type) {
            if (DCServerFactory.isCommandTypeAll(type)) {
                return true;
            } else
            if (ListTools.isEmpty(this.types)) {
                return true;
            } else {
                return ListTools.contains(this.types, type);
            }
        }
        public String getAclName() {
            return this.aclName; // not null
        }
        public AclEntry.AccessLevel getAclAccessLevelDefault() {
            return this.aclDft; // not null
        }
        public String getCommandString() {
            return this.cmdStr; // not null (may contain replacement variables)
        }
        //public String getCommandString(String cmdArgs[]) {
        //    return this.getCommandString(null, cmdArgs);
        //}
        private String _adjustKeyArg(String V, String keyArg) {
            if (!StringTools.isBlank(keyArg)) {
                String ka = keyArg.trim().toLowerCase();
                if (ListTools.contains(KEYARG_HEX8,ka)) {
                    // convert numeric value to hex (1 byte)
                    // 16 ==> 0x10
                    long v = StringTools.parseLong(V,0L) & 0xFFL;
                    return "0x" + StringTools.toHexString(v,8);
                } else
                if (ListTools.contains(KEYARG_HEX16,ka)) {
                    // convert numeric value to hex (2 bytes)
                    // 16 ==> 0x0010
                    long v = StringTools.parseLong(V,0L) & 0xFFFFL;
                    return "0x" + StringTools.toHexString(v,16);
                } else
                if (ListTools.contains(KEYARG_HEX32,ka)) {
                    // convert numeric value to hex (4 bytes)
                    // 16 ==> 0x00000010
                    long v = StringTools.parseLong(V,0L) & 0xFFFFFFFFL;
                    return "0x" + StringTools.toHexString(v,32);
                } else
                if (ListTools.contains(KEYARG_HEX64,ka)) {
                    // convert numeric value to hex (4 bytes)
                    // 16 ==> 0x0000000000000010
                    long v = StringTools.parseLong(V,0L); // already 64 bit
                    return "0x" + StringTools.toHexString(v,64);
                } else
                if (ListTools.contains(KEYARG_INT,ka)) {
                    // extract the leading digits 
                    // 123ABC ==> 123
                    return String.valueOf(StringTools.parseLong(V,0L));
                } else
                if (ListTools.contains(KEYARG_NOSPACE,ka)) {
                    // remove embedded spaces from arg [gps=${gps:ns}]
                    // Hello World ==> HelloWorld
                    return StringTools.stripChars(V," \t\r\n");
                } else
                if (ListTools.contains(KEYARG_QUOTE,ka)) {
                    // quote argument [message=${text:quote}]
                    // Hello "World" ==> "Hello \"World\""
                    return StringTools.quoteString(V); 
                } else
                if (ListTools.contains(KEYARG_GPS,ka)) {
                    // format arg as GPS [point=${point:gps}]
                    // 39.1234, -142.1234 ==> 39.1234/-142.1234
                    GeoPoint gp = new GeoPoint(V, GP_SEP);
                    return gp.toString();
                } else
                if (ListTools.contains(KEYARG_GPLAT,ka)) {
                    // extract latitude from "latitude/longitude" [latitude=${point:lat}]
                    GeoPoint gp = new GeoPoint(V, GP_SEP);
                    return gp.getLatitudeString();
                } else
                if (ListTools.contains(KEYARG_GPLON,ka)) {
                    // extract longitude from "latitude/longitude" [longitude=${point:lon}]
                    GeoPoint gp = new GeoPoint(V, GP_SEP);
                    return gp.getLongitudeString();
                } else
                if (StringTools.isNumeric(ka)) {
                    // extract indexed value from "A,B,C,D" [value=${arg0:1}]
                    int p = StringTools.indexOf(V, CSV_SEP); // find first separator
                    int n = StringTools.parseInt(ka, -1);
                    if (n < 0) {
                        // argument index is invalid
                        return V; // leave as-is
                    } else
                    if (p < 0) {
                        // "V" contains only one value (no separators found)
                        return (n == 0)? V : ""; // return as first element only
                    } else {
                        // return indexed element
                        String f[] =  StringTools.split(V, V.charAt(p)); // split on first separator
                        return (n < f.length)? f[n] : "";
                    }
                }
            }
            return V; // leave as-is
        }
        public String getCommandString(Device device, String cmdArgs[]) {
            String cs = this.getCommandString();
            // insert command arguments
            if (this.hasCommandArgs()) {
                // insert replacement variables
                final String args[] = (cmdArgs != null)? cmdArgs : new String[0];
                String rcs = StringTools.replaceKeys(cs, new StringTools.KeyValueMap() {
                    public String getKeyValue(String key, String keyArg, String dft) {
                        int argNdx = (Command.this.argMap != null)? Command.this.argMap.indexOfKey(key) : -1;
                        if ((argNdx >= 0) && (argNdx < args.length)) { 
                            // ${text[:{ns|int|quote|gps|gplat|gplon}][=DFT]}
                            String V = !StringTools.isBlank(args[argNdx])? args[argNdx] : dft;
                            return Command.this._adjustKeyArg(V,keyArg);
                        } else
                        if (key.equals(DEFAULT_ARG_NAME)) { 
                            // ${arg[:{ns|int|quote|gps|gplat|gplon}][=DFT]}
                            String V = ((args.length > 0) && !StringTools.isBlank(args[0]))? args[0] : dft; 
                            return Command.this._adjustKeyArg(V,keyArg);
                        } else {
                            // ${argX[:{ns|int|quote|gps|gplat|gplon}][=DFT]}
                            for (int i = 0; i < args.length; i++) {
                                if (key.equals(DEFAULT_ARG_NAME+i)) { // "arg0", "arg1", ...
                                    String V = !StringTools.isBlank(args[i])? args[i] : dft; 
                                    return Command.this._adjustKeyArg(V,keyArg);
                                }
                            }
                            return Command.this._adjustKeyArg(dft,keyArg);
                        }
                    }
                });
                cs = rcs;
            }
            // insert fixed arguments into command string
            if (device != null) {
                String dataKey  = device.getDataKey();      // %{dataKey} , {DATAKEY}
                String uniqueID = device.getUniqueID();     // %{uniqueID}, {UNIQUEID}
                String modemID  = device.getModemID();      // %{modemID} , {MODEMID}
                String imeiNum  = device.getImeiNumber();   // %{imei}    , {IMEI}
                String serial   = device.getSerialNumber(); // %{serial}  , {SERIAL}
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_dataKey , dataKey );
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_uniqueID, uniqueID);
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_modemID , modemID );
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_imei    , imeiNum );
                cs = SMSOutboundGateway.REPLACE(cs, SMSOutboundGateway.REPL_serial  , serial  );
            }
            // return command string
            return cs;
        }
        public boolean hasCommandArgs() {
            //return (this.cmdStr != null)? (this.cmdStr.indexOf("${arg") >= 0) : false;
            return this.hasArgs;
        }
        public int getArgCount() {
            if (!this.hasArgs) {
                return 0;
            } else
            if (this.argMap != null) {
                return this.argMap.size();
            } else {
                return 1;
            }
        }
        public CommandArg getCommandArg(int argNdx) {
            if (this.hasArgs && (this.argMap != null)) {
                return this.argMap.getValue(argNdx);
            } else {
                return null;
            }
        }
        public CommandProtocol getCommandProtocol() {
            return this.getCommandProtocol(null);
        }
        public CommandProtocol getCommandProtocol(CommandProtocol dftProto) {
            if (!StringTools.isBlank(this.cmdProto)) {
                // may return null if cmdProto is not one of the valid values
                return DCServerConfig.getCommandProtocol(this.cmdProto);
            } else {
                return dftProto;
            }
        }
        public boolean isCommandProtocolSMS() {
            CommandProtocol proto = this.getCommandProtocol(null);
            return ((proto != null) && proto.isSMS());
        }
        public String getCommandProtocolHandler() {
            return this.protoHandlr; // may be null
        }
        public boolean hasMaximumRouteAge() {
            return (this.maxRouteAgeSec > 0L)? true : false;
        }
        public long getMaximumRouteAge() {
            return this.maxRouteAgeSec;
        }
        public boolean queueIfUndelivered() {
            return this.allowQueue;
        }
        public boolean getExpectAck() {
            return this.expectAck;
        }
        public int getExpectAckCode() {
            return this.expAckCode;
        }
        public boolean hasStateBitMask() {
            return (this.stateMask != 0x0000L)? true : false;
        }
        public long getStateBitMask() {
            return this.stateMask;
        }
        public boolean getStateBitValue() {
            return this.stateVal;
        }
        public boolean hasAuditStatusCode() {
            return (this.cmdStCode != StatusCodes.STATUS_NONE)? true : false;
        }
        public int getAuditStatusCode() {
            return this.cmdStCode;
        }
        public boolean setDeviceCommandAttributes(
            DCServerFactory.ResultCode result, 
            Device device, String cmdStr, boolean update) {
            if (DCServerFactory.isCommandResultOK(result) && (device != null)) {
                return device.postCommandHandling(this, cmdStr, update);
            } else {
                return false;
            }
        }
        public JSON._Object toJsonObject() {
            JSON._Object obj = new JSON._Object();
            obj.addKeyValue(JSONKey_Name       , this.getName());
            obj.addKeyValue(JSONKey_Description, this.getDescription());
            obj.addKeyValue(JSONKey_Enabled    , this.isEnabled(null));
            obj.addKeyValue(JSONKey_Types      , new JSON._Array(this.getTypes()).setFormatIndent(false));
            obj.addKeyValue(JSONKey_AclName    , this.getAclName());
            obj.addKeyValue(JSONKey_AclDefault , this.getAclAccessLevelDefault().getIntValue());
            if (this.hasMaximumRouteAge()) {
                obj.addKeyValue(JSONKey_MaximumRouteAge, this.getMaximumRouteAge());
            } else {
                // defaults to "MaximumRouteAge=0"
            }
            obj.addKeyValue(JSONKey_AllowQueue, this.queueIfUndelivered());
            if (this.getExpectAck()) {
                obj.addKeyValue(JSONKey_ExpectAck    , this.getExpectAck());
                obj.addKeyValue(JSONKey_ExpectAckCode, this.getExpectAckCode());
            } else {
                // default: ExpectAck=false
            }
            if (this.hasStateBitMask()) {
                obj.addKeyValue(JSONKey_StateBitMask , this.getStateBitMask());
                obj.addKeyValue(JSONKey_StateBitValue, this.getStateBitValue());
            } else {
                // default: StateBitMask=0
            }
            if (this.hasAuditStatusCode()) {
                obj.addKeyValue(JSONKey_AuditCode, this.getAuditStatusCode());
            }
            obj.addKeyValue(JSONKey_Protocol     , StringTools.trim(this.getCommandProtocol()));
            obj.addKeyValue(JSONKey_CommandString, this.getCommandString());
            if (this.hasCommandArgs() && !ListTools.isEmpty(this.argMap)) {
                JSON._Array jsonArgArray = new JSON._Array();
                for (int i = 0; i < this.getArgCount(); i++) {
                    CommandArg cmdArg = this.getCommandArg(i);
                    if (cmdArg != null) {
                        jsonArgArray.addValue(cmdArg.toJsonObject());
                    } else
                    if ((this.argMap != null) && (i < this.argMap.size())) {
                        //Print.logWarn("Command '"+this.getName()+"' Arg #" + i + " is null");
                        JSON._Object argObj = new JSON._Object();
                        argObj.addKeyValue(JSONKey_Name, "arg" + i);
                        jsonArgArray.addValue(argObj);
                    }
                }
                obj.addKeyValue(JSONKey_Args, jsonArgArray);
            }
            return obj;
        }
    }

    private static final int ARG_DISPLAY_LENGTH     =  70;
    private static final int ARG_MAXIMUM_LENGTH     = 500;

    public static class CommandArg
    { // static because the 'Args' are initialized before the 'Command' is
        private Command command   = null;   // The command that owns this arg
        private String  argName   = "";   // This argument name
        private String  argDesc   = "";   // This argument description
        private boolean readOnly  = false;
        private String  resKey    = null;
        private String  dftVal    = "";
        private int     lenDisp   = ARG_DISPLAY_LENGTH;
        private int     lenMax    = ARG_MAXIMUM_LENGTH;
        public CommandArg(JSON._Object arg) {
            this.argName   = arg.getStringForName( JSONKey_Name         , "");
            this.argDesc   = arg.getStringForName( JSONKey_Description  , "");
            this.readOnly  = arg.getBooleanForName(JSONKey_ReadOnly     , false);
            this.resKey    = arg.getStringForName( JSONKey_ResourceName , null);
            this.dftVal    = arg.getStringForName( JSONKey_DefaultValue , "");
            this.lenDisp   = arg.getIntForName(    JSONKey_DisplayLength, ARG_DISPLAY_LENGTH);
            this.lenMax    = arg.getIntForName(    JSONKey_MaximumLength, ARG_MAXIMUM_LENGTH);
        }
        public CommandArg(String name, String desc, boolean readOnly, String resKey, String dftVal) {
            this.argName   = StringTools.trim(name);
            this.argDesc   = StringTools.trim(desc);
            this.readOnly  = readOnly;
            this.resKey    = !StringTools.isBlank(resKey)? resKey : null;
            this.dftVal    = StringTools.trim(dftVal);
            this.lenDisp   = ARG_DISPLAY_LENGTH;
            this.lenMax    = ARG_MAXIMUM_LENGTH;
        }
        public String getName() {
            return this.argName;
        }
        public String getDescription() {
            return this.argDesc;
        }
        public boolean isReadOnly() {
            return this.readOnly;
        }
        public void setCommand(Command cmd) {
            this.command = cmd;
        }
        public Command getCommand() {
            return this.command;
        }
        public String getResourceName() {
            return this.resKey;
            // ie. "DCServerConfig.enfora.DriverMessage.arg"
            /*
            StringBuffer sb = new StringBuffer();
            sb.append("DCServerConfig.");
            sb.append(cmd.getDCServerConfig().getName());
            sb.append(".");
            sb.append(cmd.getName());
            sb.append(".");
            sb.append(this.getName());
            return sb.toString();
            */
        }
        public String getDefaultValue() {
            return this.dftVal;
        }
        public void setLength(int dispLen, int maxLen) {
            this.lenDisp = (dispLen > 0)? dispLen : 70;
            this.lenMax  = (maxLen  > 0)? maxLen  : (this.lenDisp * 2);
        }
        public int getDisplayLength() {
            return this.lenDisp;
        }
        public int getMaximumLength() {
            return this.lenMax;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getName()).append("/").append(StringTools.quoteString(this.getDescription()));
            return sb.toString();
        }
        public JSON._Object toJsonObject() {
            JSON._Object obj = new JSON._Object();
            obj.addKeyValue(JSONKey_Name         , this.getName());
            obj.addKeyValue(JSONKey_Description  , this.getDescription());
            obj.addKeyValue(JSONKey_ReadOnly     , this.isReadOnly());
            obj.addKeyValue(JSONKey_ResourceName , this.getResourceName());
            obj.addKeyValue(JSONKey_DefaultValue , this.getDefaultValue());
            obj.addKeyValue(JSONKey_DisplayLength, this.getDisplayLength());
            obj.addKeyValue(JSONKey_MaximumLength, this.getMaximumLength());
            return obj;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                          bindAddress             = null;

    private String                          dcName                  = "";
    private String                          dcDesc                  = "";

    private File                            dcConfigFile            = null;

    private String                          remoteLoggingAttr       = null;

    private String                          uniquePrefix[]          = null;

    private boolean                         useSSL                  = false;
    private OrderedMap<Integer,InetAddress> tcpPortMap              = null;
    private OrderedMap<Integer,InetAddress> udpPortMap              = null;
    private OrderedMap<Integer,InetAddress> satPortMap              = null;

    private int                             startStopCodes[]        = null;
    private boolean                         startStopCodes_init     = false;

    private boolean                         customCodeEnabled       = true;
    private Map<Object,EventCode>           customCodeMap           = new HashMap<Object,EventCode>();

    private String                          commandHost             = null;
    private int                             commandPort             = 0;
    private CommandProtocol                 commandProtocol         = null;

    private long                            attrFlags               = F_NONE;

    private Map<String,RTProperties>        rtPropsMap              = new OrderedMap<String,RTProperties>();

    private String                          commandsAclName         = null;
    private AclEntry.AccessLevel            commandsAccessLevelDft  = null;

    private OrderedMap<String,Command>      commandMap              = null;

    private Set<String>                     cfgPropKeys             = null;

    /**
    *** Blank Constructor
    **/
    public DCServerConfig()
    {
        this.getDefaultProperties();
        this.setBindAddress(null);
        this.setName("unregistered");
        this.setDescription("Unregistered DCS");
        this.setConfigFile(null);
        this.setAttributeFlags(F_NONE);
        this.setUseSSL(false);
        this.setTcpPorts(null, null, false);
        this.setUdpPorts(null, null, false);
        this.setSatPorts(null, null, false);
        this.setCommandDispatcherPort(0);
        this.setUniquePrefix(null);
        this._postInit();
    }

    /**
    *** Constructor
    **/
    public DCServerConfig(
        String name, String desc, 
        int tcpPorts[], int udpPorts[], int commandPort, 
        long flags, String... uniqPfx)
    {
        this.getDefaultProperties();
        this.setBindAddress(null);
        this.setName(name);
        this.setDescription(desc);
        this.setConfigFile(null);
        this.setAttributeFlags(flags);
        this.setUseSSL(false);
        this.setTcpPorts(null, tcpPorts, true);
        this.setUdpPorts(null, udpPorts, true);
        this.setSatPorts(null, null    , true); // TODO:
        this.setCommandDispatcherPort(commandPort, true);
        this.setUniquePrefix(uniqPfx);
        this._postInit();
    }

    private void _postInit()
    {
        // etc.
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server bind address
    **/
    protected void setBindAddress(String b)
    {
        this.bindAddress = StringTools.trim(b);
    }

    /**
    *** Gets the server bind address
    **/
    public String getBindAddress()
    {
        return this.bindAddress;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server name/id
    **/
    protected void setName(String n)
    {
        this.dcName = StringTools.trim(n);
    }

    /**
    *** Gets the server name/id
    **/
    public String getName()
    {
        return this.dcName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server description
    **/
    public void setDescription(String d)
    {
        this.dcDesc = StringTools.trim(d);
    }

    /**
    *** Gets the server description
    **/
    public String getDescription()
    {
        return this.dcDesc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the config-file from which this DCServerConfig was created
    **/
    public void setConfigFile(File xmlFile)
    {
        this.dcConfigFile = xmlFile;
    }

    /**
    *** Gets the config-file from this which this DCServerConfig was created
    **/
    public File getConfigFile()
    {
        return this.dcConfigFile;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the remote logging attributes for this DCS
    *** @param logAttr The remote log attributes in the form "[[HOST]:]PORT[/BUFFERSIZE]"
    **/
    public void setRemoteLogging(String logAttr)
    {
        this.remoteLoggingAttr = logAttr;
    }

    /**
    *** Gets the remote logging attribute string for this DCS 
    *** @return The remote logging attribute string for this DCS 
    **/
    public String getRemoteLogging()
    {
        return this.remoteLoggingAttr;
    }

    /**
    *** Starts the RemoteLogServer if remote logging is enabled
    *** @return True if started, false otherwise
    **/
    public boolean startRemoteLogging()
    {
        String logAttr = this.getRemoteLogging();
        if (!StringTools.isBlank(logAttr)) {
            boolean ok = Print.setRemoteLogging(logAttr);
            if (ok) {
                //Print.sysPrintln("Enabled cached remote logging on port " + port);
                return true;
            } else {
                //Print.sysPrintln("Unable to Enabled cached remote logging");
                return false;
            }
        } else {
            //Print.sysPrintln("Remote logging not enabled");
            return false;
        }
    }

    /**
    *** Starts the RemoteLogServer if remote logging is enabled
    *** @param dcsc  The DCServerConfig instance
    *** @return True if started, false otherwise
    **/
    public static boolean startRemoteLogging(DCServerConfig dcsc)
    {
        if (dcsc != null) {
            return dcsc.startRemoteLogging();
        } else {
            //Print.sysPrintln("Remote logging not enabled");
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server attribute flags
    **/
    public void setAttributeFlags(long f)
    {
        this.attrFlags = f;
    }

    /**
    *** Gets the server attribute flags
    **/
    public long getAttributeFlags()
    {
        return this.attrFlags;
    }

    /**
    *** Returns true if the indicate mask is non-zero
    **/
    public boolean isAttributeFlag(long mask)
    {
        return ((this.getAttributeFlags() & mask) != 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets an array of server ports from the specified runtime keys.
    *** (first check command-line, then config file, then default) 
    *** @param name The server name
    *** @param rtPropKey  The runtime key names
    *** @param dft  The default array of server ports if not defined otherwise
    *** @return The array of server ports
    **/
    private int[] _getServerPorts(
        String cmdLineKey[],
        String rtPropKey[], 
        int dft[])
    {
        String portStr[] = null;

        /* check command-line override first */
        RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
        if ((cmdLineProps != null) && cmdLineProps.hasProperty(cmdLineKey)) {
            portStr = cmdLineProps.getStringArray(cmdLineKey, null);
        }

        /* check runtime config override */
        if (ListTools.isEmpty(portStr)) {
            String ak[] = this.normalizeKeys(rtPropKey); // "tcpPort" ==> "enfora.tcpPort"
            if (!this.hasProperty(ak,false)) { // exclude 'defaults'
                // no override defined
                return dft;
            }
            portStr = this.getStringArrayProperty(ak, null);
            if (ListTools.isEmpty(portStr)) {
                // ports explicitly removed
                //Print.logInfo(name + ": Returning 'null' ports");
                return null;
            }
        }

        /* parse/return port numbers */
        int p = 0;
        int srvPorts[] = new int[portStr.length];
        for (int i = 0; i < portStr.length; i++) {
            int port = StringTools.parseInt(portStr[i], 0);
            if (ServerSocketThread.isValidPort(port)) {
                srvPorts[p++] = port;
            }
        }
        if (p < srvPorts.length) {
            // list contains invalid port numbers
            int newPorts[] = new int[p];
            System.arraycopy(srvPorts, 0, newPorts, 0, p);
            srvPorts = newPorts;
        }
        if (!ListTools.isEmpty(srvPorts)) {
            //Print.logInfo(name + ": Returning server ports: " + StringTools.join(srvPorts,","));
            return srvPorts;
        } else {
            //Print.logInfo(name + ": Returning 'null' ports");
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets whether to use SSL on TCP connections
    **/
    public void setUseSSL(boolean useSSL)
    {
        this.useSSL = useSSL;
    }

    /**
    *** Gets whether to use SSL on TCP connections
    **/
    public boolean getUseSSL()
    {
        return this.useSSL;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the default TCP port for this server
    **/
    public void setTcpPorts(InetAddress bindAddr, int tcp[], boolean checkRTP)
    {
        if (checkRTP) {
            tcp = this._getServerPorts(
                DCServerFactory.ARG_tcpPort,
                DCServerFactory.CONFIG_tcpPort(this.getName()), 
                tcp);
        }
        if (!ListTools.isEmpty(tcp)) {
            if (this.tcpPortMap == null) { this.tcpPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < tcp.length; i++) {
                Integer p = new Integer(tcp[i]);
                if (this.tcpPortMap.containsKey(p)) {
                    Print.logWarn("TCP port already defined ["+this.getName()+"]: " + p);
                }
                this.tcpPortMap.put(p, bindAddr);
            }
        }
    }

    /**
    *** Get TCP Port bind address
    **/
    public InetAddress getTcpPortBindAddress(int port)
    {
        InetAddress bind = (this.tcpPortMap != null)? this.tcpPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default TCP port for this server
    **/
    public int[] getTcpPorts()
    {
        if (ListTools.isEmpty(this.tcpPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.tcpPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.tcpPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create TCP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_TCP(int port)
        throws SocketException, IOException
    {
        boolean useSSL = this.getUseSSL();
        return this.createServerSocketThread_TCP(port, useSSL);
    }

    /**
    *** Create TCP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_TCP(int port, boolean useSSL)
        throws SocketException, IOException // BindException
    {
        InetAddress bindAddr = this.getTcpPortBindAddress(port);
        String bindAddrS = (bindAddr != null)? bindAddr.toString() : "ALL";
        Print.logInfo("Binding TCP listener to " + bindAddrS + ":" + port);
        ServerSocketThread sst = new ServerSocketThread(bindAddr, port, useSSL);
        sst.setName("TCPListener_" + port);
        return sst;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default UDP port for this server
    **/
    public void setUdpPorts(InetAddress bindAddr, int udp[], boolean checkRTP)
    {
        if (checkRTP) {
            udp = this._getServerPorts(
                DCServerFactory.ARG_udpPort,
                DCServerFactory.CONFIG_udpPort(this.getName()), 
                udp);
        }
        if (!ListTools.isEmpty(udp)) {
            if (this.udpPortMap == null) { this.udpPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < udp.length; i++) {
                Integer p = new Integer(udp[i]);
                if (this.udpPortMap.containsKey(p)) {
                    Print.logWarn("UDP port already defined ["+this.getName()+"]: " + p);
                }
                this.udpPortMap.put(p, bindAddr);
                //Print.logInfo("Setting UDP listener at " + StringTools.blankDefault(bindAddr,"<ALL>") + " : " + p);
            }
        }
    }
    
    /**
    *** Get UDP Port bind address
    **/
    public InetAddress getUdpPortBindAddress(int port)
    {
        InetAddress bind = (this.udpPortMap != null)? this.udpPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default UDP port for this server
    **/
    public int[] getUdpPorts()
    {
        if (ListTools.isEmpty(this.udpPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.udpPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.udpPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create UDP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_UDP(int port)
        throws SocketException, IOException // BindException
    {
        if (port > 0) {
            InetAddress bindAddr = this.getUdpPortBindAddress(port);
            String bindAddrS = (bindAddr != null)? bindAddr.toString() : "ALL";
            Print.logInfo("Binding UDP listener to " + bindAddrS + ":" + port); // +" [bound="+dgs.isBound()+"]");
            DatagramSocket dgs = ServerSocketThread.createDatagramSocket(bindAddr, port); // BindException
          //Print.logInfo("Binding UDP listener to "+dgs.getLocalAddress()+":"+dgs.getLocalPort()+" [bound="+dgs.isBound()+"]");
            ServerSocketThread sst = new ServerSocketThread(dgs);
            sst.setName("UDPListener_" + port);
            return sst;
        } else {
            Print.logInfo("Created unbound UDP listener");
            ServerSocketThread sst = new ServerSocketThread((DatagramSocket)null);
            sst.setName("UDPListener_unbound");
            return sst;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the default SAT port for this server
    **/
    public void setSatPorts(InetAddress bindAddr, int sat[], boolean checkRTP)
    {
        if (checkRTP) {
            sat = this._getServerPorts(
                DCServerFactory.ARG_satPort,
                DCServerFactory.CONFIG_satPort(this.getName()), 
                sat);
        }
        if (!ListTools.isEmpty(sat)) {
            if (this.satPortMap == null) { this.satPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < sat.length; i++) {
                Integer p = new Integer(sat[i]);
                if (this.satPortMap.containsKey(p)) {
                    Print.logWarn("SAT port already defined ["+this.getName()+"]: " + p);
                }
                this.satPortMap.put(p, bindAddr);
            }
        }
    }

    /**
    *** Get SAT Port bind address
    **/
    public InetAddress getSatPortBindAddress(int port)
    {
        InetAddress bind = (this.satPortMap != null)? this.satPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default SAT port for this server
    **/
    public int[] getSatPorts()
    {
        if (ListTools.isEmpty(this.satPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.satPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.satPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create SAT ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_SAT(int port)
        throws SocketException, IOException
    {
        boolean useSSL = this.getUseSSL();
        return this.createServerSocketThread_SAT(port, useSSL);
    }

    /**
    *** Create SAT ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_SAT(int port, boolean useSSL)
        throws SocketException, IOException
    {
        InetAddress bindAddr = this.getSatPortBindAddress(port);
        String bindAddrS = (bindAddr != null)? bindAddr.toString() : "ALL";
        Print.logInfo("Binding SAT listener to " + bindAddrS + ":" + port);
        ServerSocketThread sst = new ServerSocketThread(bindAddr, port, useSSL);
        sst.setName("SATListener_" + port);
        return sst;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final int STARTSTOP_STARTSTOP[] = { StatusCodes.STATUS_MOTION_START, StatusCodes.STATUS_MOTION_STOP  };
    public static final int STARTSTOP_IGNITION[]  = { StatusCodes.STATUS_IGNITION_ON , StatusCodes.STATUS_IGNITION_OFF };
    public static final int STARTSTOP_ENGINE[]    = { StatusCodes.STATUS_ENGINE_START, StatusCodes.STATUS_ENGINE_STOP  };
    public static final int STARTSTOP_PARK[]      = { StatusCodes.STATUS_UNPARKED    , StatusCodes.STATUS_PARKED       };

    /**
    *** Sets the start and stop status codes.
    *** @param ssKey   The start status code key ("ignition", "parked", "startStop")
    **/
    public void setStartStopStatusCodes(String ssKey)
    {
        // <Property key="startStopStatusCodes">default</Property>
        // <Property key="startStopStatusCodes">ignition</Property>
        // <Property key="startStopStatusCodes">engine</Property>
        // <Property key="startStopStatusCodes">parked</Property>
        // <Property key="startStopStatusCodes">startstop</Property>
        String ssk = StringTools.trim(ssKey).toLowerCase();
        if (StringTools.isBlank(ssk)) {
            this.startStopCodes = null;
            return;
        } else
        if (ssk.equals("default")) {
            if (this.getStartStopSupported(false)) {
                this.startStopCodes = STARTSTOP_STARTSTOP;
            } else {
                this.startStopCodes = null;
            }
            return;
        } else
        if (ssk.equals("ign") || ssk.equals("ignition")) {
            this.startStopCodes = STARTSTOP_IGNITION;
            return;
        } else
        if (ssk.equals("eng") || ssk.equals("engine")) {
            this.startStopCodes = STARTSTOP_ENGINE;
            return;
        } else
        if (ssk.equals("park") || ssk.equals("parked")) {
            this.startStopCodes = STARTSTOP_PARK;
            return;
        } else
        if (ssk.equals("ss") || ssk.equals("startstop")) {
            this.startStopCodes = STARTSTOP_STARTSTOP;
            return;
        } else
        if (ssk.indexOf(',') > 0) {
            String sc[] = StringTools.split(ssk,',');
            if (ListTools.size(sc) >= 2) {
                int startSC = StringTools.parseInt(sc[0],0);
                int stopSC  = StringTools.parseInt(sc[1],0);
                if ((startSC > 0) && (stopSC > 0)) {
                    this.startStopCodes = new int[] { startSC, stopSC };
                    return;
                }
            }
        }
        Print.logWarn("Invalid Start/Stop Status Code specification: " + ssKey);
        //Print.logStackTrace("here");
        this.startStopCodes = null;
    }

    /**
    *** Sets the start and stop status codes.
    *** @param startCode  The start status code
    *** @param stopCode   The stop status code
    **/
    public void setStartStopStatusCodes(int startCode, int stopCode)
    {
        if ((startCode > 0) && (stopCode > 0)) {
            this.startStopCodes = new int[] { startCode, stopCode };
        } else {
            this.startStopCodes = null;
        }
    }

    /**
    *** Sets the start and stop status codes.
    *** @param startStopCodes  An array containing 2 elements.  The first element
    ***                        is the start status code, and the second element
    ***                        is the stop status code.
    **/
    public void setStartStopStatusCodes(int startStopCodes[])
    {
        if (ListTools.size(startStopCodes) >= 2) {
            this.setStartStopStatusCodes(startStopCodes[0], startStopCodes[1]);
        } else {
            this.startStopCodes = null;
        }
    }

    /**
    *** Gets the start/stop status codes, or null if no start/stop status codes
    *** have been defined.  If non-null, element '0' will always contain the 
    *** start code, and element '1' will contain the stop code.
    *** @return The start/stop status codes, or null if no codes defined.
    **/
    public int[] getStartStopStatusCodes()
    {
        if (!this.startStopCodes_init) {
            String ssk = this.getStringProperty(DCServerFactory.CONFIG_startStopStatusCodes(this.getName()), null);
            this.setStartStopStatusCodes(ssk);
            this.startStopCodes_init = true;
        }
        return this.startStopCodes;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Start Command Listener
    *** @param port   The listen port
    *** @param handler  The command handler class
    **/
    public static ServerSocketThread startCommandHandler(int port, Class<? extends CommandPacketHandler> handler)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* create server socket */
        try {
            sst = new ServerSocketThread(port);
            sst.setName("CommandListener_" + port);
        } catch (BindException be) {
            Print.logException("(Command) Unable to bind to port [" + port + "]", be);
            throw be;
        } catch (Throwable t) { // trap any server exception
            Print.logException("(Command) ServerSocket error [port " + port + "]", t);
            throw t;
        }

        /* initialize */
        sst.setTextPackets(true);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(new int[] { '\r', '\n' });
        sst.setIgnoreChar(null);
        sst.setMaximumPacketLength(1024);       // safety net
        sst.setMinimumPacketLength(1);
        sst.setIdleTimeout(1000L);              // time between packets
        sst.setPacketTimeout(1000L);            // time from start of packet to packet completion
        sst.setSessionTimeout(5000L);           // time for entire session
        sst.setLingerTimeoutSec(5);
        sst.setTerminateOnTimeout(true);
        sst.setClientPacketHandlerClass(handler);

        /* start thread */
        DCServerConfig.startServerSocketThread(sst,"Command");
        return sst;

    }

    /**
    *** Start ServerSocketThread 
    *** @param sst    The ServerSocketThread to start
    *** @param type   The short 'type' name of the socket listener 
    **/
    public static void startServerSocketThread(ServerSocketThread sst, String type)
    {
        if (sst != null) {

            /* message header */
            String  m        = StringTools.trim(type);
            int     port     = sst.getLocalPort();
            boolean isBound  = true;
            String  bindAddr = StringTools.blankDefault(sst.getBindAddress(), "(ALL)");
            if (bindAddr.startsWith("/")) { bindAddr = bindAddr.substring(1); }
            if (sst.getServerSocket() != null) {
                // TCP
                long tmo = sst.getSessionTimeout();
                Print.logInfo("Starting "+m+" Listener (TCP) - " +port+ " [" +bindAddr+ "] timeout="+tmo+"ms ...");
                isBound = true;
            } else
            if (sst.getDatagramSocket() != null) {
                // UDP
                Print.logInfo("Starting "+m+" Listener (UDP) - " +port+ " [" +bindAddr+ "] ...");
                isBound = true;
            } else {
                Print.logInfo("Initialized "+m+" Listener (non-socket)");
                isBound = false;
            }

            /* set DCS indicator */
            DCServerFactory.__setRunningDCS(null);

            /* start thread */
            if (isBound) {
                sst.start();
            }

        } else {
            Print.logStackTrace("ServerSocketThread is null!");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Load device record from unique-id.
    *** @param dcsc    The DCS DCServerConfig instance.  If null, this method
    ***                will return null.
    *** @param modemID The unique modem ID (IMEI, ESN, etc)
    *** @return The Device record, or null if "dcsc" is null, or if the modemID
    ***         is not found.
    **/
    public static Device loadDeviceUniqueID(DCServerConfig dcsc, String modemID)
    {
        if (dcsc != null) {
            return dcsc.loadDeviceUniqueID(modemID);
        } else {
            Print.logError("DCServerConfig is null");
            return null;
        }
    }

    /**
    *** Load device record from unique-id.  
    *** This is the main method used by most DCS modules to load the Device record 
    *** instance from the specified modemID.
    *** @param modemID The unique modem ID (IMEI, ESN, etc)
    *** @return The Device record, or null if "dcsc" is null, or if the modemID
    ***         is not found.
    **/
    public Device loadDeviceUniqueID(String modemID)
    {
        return DCServerFactory._loadDeviceByPrefixedModemID(this.getUniquePrefix(), modemID);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the named server is defined in the current installation.
    **/
    public boolean serverJarExists()
    {
        return DCServerFactory.serverJarExists(this.getName());
    }
    
    /**
    *** Returns true if this DCS requires a Jar file
    **/
    public boolean isJarOptional()
    {
        return this.isAttributeFlag(F_JAR_OPTIONAL);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Loads/Returns the named class.
    *** @param cn   The class name
    *** @param SC   The required subclass (may be null)
    *** @param dftC The default class if unable to return named class
    *** @return The loaded named class
    **/
    private Class<?> LoadClassForName(String cn, Class<?> SC, Class<?> dftC)
    {
        if (!StringTools.isBlank(cn)) {
            try {
                Class<?> C = Class.forName(cn);
                if ((SC == null) || SC.isAssignableFrom(C)) {
                    return C;
                } else {
                    Print.logError("Invalid '"+StringTools.className(SC)+"' class: " + cn);
                    return dftC;
                }
            } catch (ClassNotFoundException cnfe) {
                Print.logWarn("Class not found: " + cn);
                return dftC;
            } catch (Throwable th) {
                Print.logError("Error loading class: " + cn + " [" + th + "]");
                return dftC;
            }
        } else {
            return dftC;
        }
    }

    /**
    *** Gets the "TrackServerAdapter" subclass 
    *** @param tsaC  The "TrackServerAdapter" class 
    *** @param dftC  The default "TrackServerAdapter" subclass 
    *** @return The "TrackServerAdapter" class 
    **/
    @SuppressWarnings("unchecked")
    public Class<?> getTrackServerAdapterClass(Class<?> tsaC, Class<?> dftC)
    {
        String cn = this.getStringProperty(DCServerFactory.CONFIG_TrackServerAdapterClass(this.getName()),null);
        return LoadClassForName(cn, tsaC, dftC);
    }

    /**
    *** Gets the "CommandPacketHandler" subclass 
    *** @param dftC  The default "CommandPacketHandler" subclass 
    *** @return The "CommandPacketHandler" subclass 
    **/
    @SuppressWarnings("unchecked")
    public Class<? extends CommandPacketHandler> getCommandPacketHandlerClass(Class<? extends CommandPacketHandler> dftC)
    {
        String cn = this.getStringProperty(DCServerFactory.CONFIG_CommandPacketHandlerClass(this.getName()),null);
        return (Class<CommandPacketHandler>)LoadClassForName(cn, CommandPacketHandler.class, dftC);
    }

    /**
    *** Gets the "ClientPacketHandler" subclass 
    *** @param dftC  The default "ClientPacketHandler" subclass 
    *** @return The "ClientPacketHandler" subclass 
    **/
    @SuppressWarnings("unchecked")
    public Class<? extends ClientPacketHandler> getClientPacketHandlerClass(Class<? extends ClientPacketHandler> dftC)
    {
        String cn = this.getStringProperty(DCServerFactory.CONFIG_ClientPacketHandlerClass(this.getName()),null);
        return (Class<CommandPacketHandler>)LoadClassForName(cn, ClientPacketHandler.class, dftC);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the command protocol to use when communicating with remote devices
    *** @param proto  The CommandProtocol
    **/
    public void setCommandProtocol(String proto)
    {
        this.commandProtocol = DCServerConfig.getCommandProtocol(proto);
    }

    /**
    *** Sets the command protocol to use when communicating with remote devices
    *** @param proto  The CommandProtocol
    **/
    public void setCommandProtocol(CommandProtocol proto)
    {
        this.commandProtocol = proto;
    }

    /**
    *** Gets the command protocol to use when communicating with remote devices
    *** @return The Command Protocol
    **/
    public CommandProtocol getCommandProtocol()
    {
        return (this.commandProtocol != null)? this.commandProtocol : CommandProtocol.UDP;
    }

    /**
    *** Gets the "Client Command Port" 
    *** @param dft  The default Client Command Port
    *** @return The Client Command Port
    **/
    public int getClientCommandPort_udp(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_clientCommandPort_udp(this.getName()), dft);
    }

    /**
    *** Gets the "Client Command Port" 
    *** @param dft  The default Client Command Port
    *** @return The Client Command Port
    **/
    public int getClientCommandPort_tcp(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_clientCommandPort_tcp(this.getName()), dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the "ACK Response Port" 
    *** @param dft  The ACK response port
    *** @return The ack response port
    **/
    public int getAckResponsePort(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_ackResponsePort(this.getName()), dft);
    }

    /**
    *** Gets the "ACK Response Port" 
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The ACK response port
    *** @return The ack response port
    **/
    public static int getAckResponsePort(DCServerConfig dcsc, int dft)
    {
        return (dcsc != null)? dcsc.getAckResponsePort(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "TCP idle timeout" 
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpIdleTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_tcpIdleTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "TCP packet timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpPacketTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_tcpPacketTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "TCP session timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpSessionTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_tcpSessionTimeoutMS(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "UDP idle timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpIdleTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_udpIdleTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "UDP packet timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpPacketTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_udpPacketTimeoutMS(this.getName()), dft);
    }

    /**
    *** Gets the "UDP session timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpSessionTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_udpSessionTimeoutMS(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the array of allowed UniqueID prefixes
    *** @param dftPfx  The default list of prefixes
    *** @return The array of allowed UniqueID prefixes
    **/
    public String[] getUniquePrefix(String dftPfx[])
    {
        if (ListTools.isEmpty(this.uniquePrefix)) {
            // set non-empty default
            this.uniquePrefix = !ListTools.isEmpty(dftPfx)? dftPfx : new String[] { "" };
        }
        return this.uniquePrefix;
    }

    /**
    *** Gets the array of allowed UniqueID prefixes
    *** @return The array of allowed UniqueID prefixes
    **/
    public String[] getUniquePrefix()
    {
        return this.getUniquePrefix(null);
    }

    /**
    *** Sets the array of allowed UniqueID prefixes
    *** @param pfx  The default UniqueID prefixes
    **/
    public void setUniquePrefix(String pfx[])
    {
        if (!ListTools.isEmpty(pfx)) {
            for (int i = 0; i < pfx.length; i++) {
                String p = pfx[i].trim();
                if (p.equals("<blank>") || p.equals("*")) { 
                    p = ""; 
                } else
                if (p.endsWith("*")) {
                    p = p.substring(0, p.length() - 1);
                }
                pfx[i] = p;
            }
            this.uniquePrefix = pfx;
        } else {
            this.uniquePrefix = new String[] { "" };;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Minimum Moved Meters"
    *** @param dft  The default minimum distance
    *** @return The Minimum Moved Meters
    **/
    public double getMinimumMovedMeters(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_minimumMovedMeters(this.getName()), dft);
    }

    /**
    *** Gets the "Minimum Moved Meters"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default minimum distance
    *** @return The Minimum Moved Meters
    **/
    public static double getMinimumMovedMeters(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMinimumMovedMeters(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Minimum Speed KPH"
    *** @param dft  The default minimum speed
    *** @return The Minimum Speed KPH
    **/
    public double getMinimumSpeedKPH(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_minimumSpeedKPH(this.getName()), dft);
    }

    /**
    *** Gets the "Minimum Speed KPH"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default minimum speed
    *** @return The Minimum Speed KPH
    **/
    public static double getMinimumSpeedKPH(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMinimumSpeedKPH(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Estimate Odometer" flag
    *** @param dft  The default estimate odometer flag
    *** @return The Estimate Odometer flag
    **/
    public boolean getEstimateOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_estimateOdometer(this.getName()), dft);
    }

    /**
    *** Gets the "Estimate Odometer" flag
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default estimate odometer flag
    *** @return The Estimate Odometer flag
    **/
    public static boolean getEstimateOdometer(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getEstimateOdometer(dft) : dft;
    }
 
    // ------------------------------------------------------------------------

    /**
    *** Gets the "Ignore Device Odometer" flag
    *** @param dft  The default ignore device odometer flag
    *** @return The Ignore Device Odometer flag
    **/
    public boolean getIgnoreDeviceOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_ignoreDeviceOdometer(this.getName()), dft);
    }

    /**
    *** Gets the "Ignore Device Odometer" flag
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default ignore device odometer flag
    *** @return The ignore device Odometer flag
    **/
    public static boolean getIgnoreDeviceOdometer(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getIgnoreDeviceOdometer(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Simulate Geozones" state
    *** @param dft  The default Simulate Geozones state
    *** @return The Simulate Geozones state
    **/
    public boolean getSimulateGeozones(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_simulateGeozones(this.getName()), dft);
    }

    /**
    *** Gets the "Simulate Geozones" state
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default Simulate Geozones state
    *** @return The Simulate Geozones state
    **/
    public static boolean getSimulateGeozones(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getSimulateGeozones(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Geozone Skip Old Events" state
    *** @param dft  The default Geozone Skip Old Events state
    *** @return The Geozone Skip Old Events state
    **/
    public boolean getGeozoneSkipOldEvents(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_geozoneSkipOldEvents(this.getName()), dft);
    }

    /**
    *** Gets the "Geozone Skip Old Events" state
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default Geozone Skip Old Events state
    *** @return The Geozone Skip Old Events state
    **/
    public static boolean getGeozoneSkipOldEvents(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getGeozoneSkipOldEvents(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Maximum HDOP"
    *** @param dft  The default maximum HDOP
    *** @return The Maximum HDOP
    **/
    public double getMaximumHDOP(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_maximumHDOP(this.getName()), dft);
    }

    /**
    *** Gets the "Maximum HDOP"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default maximum HDOP
    *** @return The Maximum HDOP
    **/
    public static double getMaximumHDOP(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMaximumHDOP(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Maximum Accuracy Meters"
    *** @param dft  The default maximum accuracy meters
    *** @return The Maximum accuracy meters
    **/
    public double getMaximumAccuracyMeters(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_maximumAccuracyMeters(this.getName()), dft);
    }

    /**
    *** Gets the "Maximum Accuracy Meters"
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default maximum accuracy meters
    *** @return The Maximum accuracy meters
    **/
    public static double getMaximumAccuracyMeters(DCServerConfig dcsc, double dft)
    {
        return (dcsc != null)? dcsc.getMaximumAccuracyMeters(dft) : dft;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the "Check Last Odometer" config 
    *** @param dft  The default "Check Last Odometer" state
    *** @return The "Check Last Odometer" state
    **/
    public boolean getCheckLastOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_checkLastOdometer(this.getName()), dft);
    }

    /**
    *** Gets the "Check Last Odometer" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Check Last Odometer" state
    *** @return The "Check Last Odometer" state
    **/
    public static boolean getCheckLastOdometer(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getCheckLastOdometer(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Save Raw Data Packet" config
    *** @param dft  The default "Save Raw Data Packet" state
    *** @return The "Save Raw Data Packet" state
    **/
    public boolean getSaveRawDataPackets(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_saveRawDataPackets(this.getName()), dft);
    }

    /**
    *** Gets the "Save Raw Data Packet" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Save Raw Data Packet" state
    *** @return The "Save Raw Data Packet" state
    **/
    public static boolean getSaveRawDataPackets(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getSaveRawDataPackets(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @param dft  The default "Start/Stop StatusCode supported" state
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public boolean getStartStopSupported(boolean dft)
    {
        String n = this.getName();
        if (n.equals(DCServerFactory.OPENDMTP_NAME)) {
            return true;
        } else {
            return this.getBooleanProperty(DCServerFactory.CONFIG_startStopSupported(this.getName()), dft);
        }
    }

    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Start/Stop StatusCode supported" state
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public static boolean getStartStopSupported(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getStartStopSupported(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Status Location/InMotion Translation" config
    *** @param dft  The default "Status Location/InMotion Translation" state
    *** @return The "Status Location/InMotion Translation" state
    **/
    public boolean getStatusLocationInMotion(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_statusLocationInMotion(this.getName()), dft);
    }

    /**
    *** Gets the "Status Location/InMotion Translation" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Status Location/InMotion Translation" state
    *** @return The "Status Location/InMotion Translation" state
    **/
    public static boolean getStatusLocationInMotion(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getStatusLocationInMotion(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Ignore Invalid GPS Location Flag" config
    *** @param dft  The default "Ignore Invalid GPS Location Flag" state
    *** @return The "Ignore Invalid GPS Location Flag" state
    **/
    public boolean getIgnoreInvalidGPSFlag(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_ignoreInvalidGPSFlag(this.getName()), dft);
    }

    /**
    *** Gets the "Ignore Invalid GPS Location Flag" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Ignore Invalid GPS Location Flag" state
    *** @return The "Ignore Invalid GPS Location Flag" state
    **/
    public static boolean getIgnoreInvalidGPSFlag(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getIgnoreInvalidGPSFlag(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Ignore Events with Invalid GPS" config
    *** @param dft  The default "Ignore Events with Invalid GPS" state
    *** @return The "Ignore Events with Invalid GPS" state
    **/
    public boolean getIgnoreEventsWithInvalidGPS(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_ignoreEventsWithInvalidGPS(this.getName()), dft);
    }

    /**
    *** Gets the "Ignore Events with Invalid GPS" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Ignore Events with Invalid GPS" state
    *** @return The "Ignore Events with Invalid GPS" state
    **/
    public static boolean getIgnoreEventsWithInvalidGPS(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getIgnoreEventsWithInvalidGPS(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Use Last Valid GPS Location" config
    *** @param dft  The default "Use Last Valid GPS Location" state
    *** @return The "Use Last Valid GPS Location" state
    **/
    public boolean getUseLastValidGPSLocation(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_useLastValidGPSLocation(this.getName()), dft);
    }

    /**
    *** Gets the "Use Last Valid GPS Location" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Use Last Valid GPS Location" state
    *** @return The "Use Last Valid GPS Location" state
    **/
    public static boolean getUseLastValidGPSLocation(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getUseLastValidGPSLocation(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Use Alternate Current Timestamp for Event" config
    *** @param dft  The default "Use Alternate Current Timestamp for Event" state
    *** @return The "Use Alternate Current Timestamp for Event" state
    **/
    public boolean getUseAltCurrentTimestamp(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_useAltCurrentTimestamp(this.getName()), dft);
    }

    /**
    *** Gets the "Use Alternate Current Timestamp for Event" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Use Alternate Current Timestamp for Event" state
    *** @return The "Use Alternate Current Timestamp for Event" state
    **/
    public static boolean getUseAltCurrentTimestamp(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getUseAltCurrentTimestamp(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Save Session Statistics" config
    *** @param dft  The default "Save Session Statistics" state
    *** @return The "Save Session Statistics" state
    **/
    public boolean getSaveSessionStatistics(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_saveSessionStatistics(this.getName()), dft);
    }

    /**
    *** Gets the "Save Session Statistics" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Save Session Statistics" state
    *** @return The "Save Session Statistics" state
    **/
    public static boolean getSaveSessionStatistics(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getSaveSessionStatistics(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Debug Mode" config
    *** @param dft  The default "Debug Mode" state
    *** @return The "Debug Mode" state
    **/
    public boolean getDebugMode(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_debugMode(this.getName()), dft);
    }

    /**
    *** Gets the "Debug Mode" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Debug Mode" state
    *** @return The "Debug Mode" state
    **/
    public static boolean getDebugMode(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getDebugMode(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Show URL" config
    *** @param dft  The default "Show URL" state
    *** @return The "Show URL" state
    **/
    public boolean getShowURL(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_showURL(this.getName()), dft);
    }

    /**
    *** Gets the "Show URL" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Show URL" state
    *** @return The "Show URL" state
    **/
    public static boolean getShowURL(DCServerConfig dcsc, boolean dft)
    {
        return (dcsc != null)? dcsc.getShowURL(dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Command ACK bit" config
    *** @param dft  The default "Command ACK bit"
    *** @return The "Command ACK bit"
    **/
    public int getCommandAckBit(String bitName, int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_commandAckBit(this.getName(),bitName), dft);
    }

    /**
    *** Gets the "Command ACK bit" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Command ACK bit"
    *** @return The "Command ACK bit"
    **/
    public static int getCommandAckBit(DCServerConfig dcsc, String bitName, int dft)
    {
        return (dcsc != null)? dcsc.getCommandAckBit(bitName,dft) : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Battery Level Range" config
    *** @param dft  The default "Battery Level Range" min/max values
    *** @return The "Battery Level Range" min/max values
    **/
    public double[] getBatteryLevelRange(double dft[])
    {
        String propKeys[] = DCServerFactory.CONFIG_batteryLevelRange(this.getName());

        /* get property string */
        String blrS = this.getStringProperty(propKeys, null);
        if (StringTools.isBlank(blrS)) {
            return dft;
        }

        /* parse */
        double blr[] = StringTools.parseDouble(StringTools.split(blrS,','),0.0);
        double min   = 0.0;
        double max   = 0.0;
        if (ListTools.size(blr) <= 0) {
            min = (ListTools.size(dft) > 0)? dft[0] : 11.4;
            max = (ListTools.size(dft) > 1)? dft[1] : 12.8;
        } else
        if (ListTools.size(blr) == 1) {
            // <Property key="...">12.0</Property>
            min = blr[0];
            max = blr[0];
        } else {
            min = blr[0];
            max = blr[1];
        }

        /* adjust */
        if (min < 0.0) { min = 0.0; }
        if (max < 0.0) { max = 0.0; }
        if (max <= min) {
            // <Property>12.8,11.4</Property>
            double tmp = max;
            max = min;
            min = tmp;
        }

        /* return */
        return new double[] { min, max };

    }

    /**
    *** Gets the "Battery Level Range" config
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default "Battery Level Range" min/max values
    *** @return The "Battery Level Range" min/max values
    **/
    public static double[] getBatteryLevelRange(DCServerConfig dcsc, double dft[])
    {
        return (dcsc != null)? dcsc.getBatteryLevelRange(dft) : dft;
    }

    /**
    *** Calculates/returns the battery level based on the specified voltage range
    *** @param voltage  The current battery voltage
    *** @param range    The allowed voltage range
    *** @return The battery level percent
    **/
    public static double CalculateBatteryLevel(double voltage, double range[])
    {

        /* no specified voltage? */
        if (voltage <= 0.0) {
            return 0.0;
        }

        /* no specified range? */
        int rangeSize = ListTools.size(range);
        if ((rangeSize < 1) || (voltage <= range[0])) {
            return 0.0;
        } else
        if ((rangeSize < 2) || (voltage >= range[1])) {
            return 1.0;
        }

        /* get percent */
        // Note: the above filters out (range[1] == range[0])
        double percent = (voltage - range[0]) / (range[1] - range[0]);
        if (percent < 0.0) {
            return 0.0;
        } else
        if (percent > 1.0) {
            return 1.0;
        } else {
            return percent;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Simulate Geozones" mask
    *** @param dft  The default Simulate Geozones mask
    *** @return The Simulate Geozones mask
    **/
    public long getSimulateDigitalInputs(long dft)
    {
        String maskStr = this.getStringProperty(DCServerFactory.CONFIG_simulateDigitalInputs(this.getName()), null);
        if (StringTools.isBlank(maskStr)) {
            // not specified (or blank)
            return dft;
        } else
        if (maskStr.equalsIgnoreCase("default")) {
            // explicit "default"
            return dft;
        } else
        if (maskStr.equalsIgnoreCase("true")) {
            // explicit "true"
            return 0xFFFFFFFFL;
        } else
        if (maskStr.equalsIgnoreCase("false")) {
            // explicit "false"
            return 0x00000000L;
        } else {
            // mask specified
            long mask = StringTools.parseLong(maskStr, -1L);
            return (mask >= 0L)? mask : dft;
        }
    }

    /**
    *** Gets the "Simulate Geozones" mask
    *** @param dcsc The DCServerConfig instance
    *** @param dft  The default Simulate Geozones mask
    *** @return The Simulate Geozones mask
    **/
    public static long getSimulateDigitalInputs(DCServerConfig dcsc, long dft)
    {
        return (dcsc != null)? dcsc.getSimulateDigitalInputs(dft) : dft;
    }

    /**
    *** Returns true if this device supports digital inputs
    *** @return True if this device supports digital inputs
    **/
    public boolean hasDigitalInputs()
    {
        return this.isAttributeFlag(F_HAS_INPUTS);
    }

    /**
    *** Returns true if this device supports digital outputs
    *** @return True if this device supports digital outputs
    **/
    public boolean hasDigitalOutputs()
    {
        return this.isAttributeFlag(F_HAS_OUTPUTS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Convenience for converting the initial/final packet to a byte array.
    *** If the string begins with "0x" the the remain string is assumed to be hex
    *** @return The byte array
    **/
    public static byte[] convertToBytes(String s)
    {
        if (s == null) {
            return null;
        } else
        if (s.startsWith("0x")) {
            byte b[] = StringTools.parseHex(s,null);
            if (b != null) {
                return b;
            } else {
                return null;
            }
        } else {
            return StringTools.getBytes(s);
        }
    }

    /**
    *** Gets the "Initial Packet" byte array
    *** @param dft  The default "Initial Packet" byte array
    *** @return The "Initial Packet" byte array
    **/
    public byte[] getInitialPacket(byte[] dft)
    {
        String s = this.getStringProperty(DCServerFactory.CONFIG_initialPacket(this.getName()), null);
        if (s == null) {
            return dft;
        } else 
        if (s.startsWith("0x")) {
            return StringTools.parseHex(s,dft);
        } else {
            return s.getBytes();
        }
    }

    /**
    *** Gets the "Final Packet" byte array
    *** @param dft  The default "Final Packet" byte array
    *** @return The "Final Packet" byte array
    **/
    public byte[] getFinalPacket(byte[] dft)
    {
        String s = this.getStringProperty(DCServerFactory.CONFIG_finalPacket(this.getName()), null);
        if (s == null) {
            return dft;
        } else 
        if (s.startsWith("0x")) {
            return StringTools.parseHex(s,dft);
        } else {
            return s.getBytes();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the "Event Code Map Enable" config
    *** @param enabled  The "Event Code Map Enable" state
    **/
    public void setEventCodeEnabled(boolean enabled)
    {
        this.customCodeEnabled = enabled;
        //Print.logDebug("[" + this.getName() + "] EventCode translation enabled=" + this.customCodeEnabled);
    }

    /**
    *** Gets the "Event Code Map Enable" config
    *** @return The "Event Code Map Enable" state
    **/
    public boolean getEventCodeEnabled()
    {
        return this.customCodeEnabled;
    }
    
    /**
    *** Sets the EventCodeMap
    **/
    public void setEventCodeMap(Map<Object,EventCode> codeMap)
    {
        this.customCodeMap = (codeMap != null)? codeMap : new HashMap<Object,EventCode>();
    }
    
    /**
    *** Prints the EventCodeMap
    **/
    public boolean printEventCodeMap()
    {
        if (ListTools.isEmpty(this.customCodeMap)) {
            return false;
        }
        Vector<Object> keyList = new Vector<Object>(this.customCodeMap.keySet());
        int maxKeyLen = 0;
        for (Object keyObj : keyList) {
            int len = keyObj.toString().length();
            if (len > maxKeyLen) { maxKeyLen = len; }
        }
        ListTools.sort(keyList,null);
        for (Object keyObj : keyList) {
            StringBuffer sb = new StringBuffer();
            EventCode evc = this.customCodeMap.get(keyObj);
            int sc = evc.getStatusCode();
            sb.append(StringTools.rightAlign(keyObj.toString(),maxKeyLen));
            sb.append(" ==> ");
            sb.append(StatusCodes.ToString(evc.getStatusCode()));
            sb.append("\n");
            Print.sysPrint(sb.toString());
        }
        return true;
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(int code)
    {
        if (!this.customCodeEnabled) {
            return null;
        } else {
            Object keyObj = new Integer(code);
            return this.customCodeMap.get(keyObj);
        }
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(long code)
    {
        if (!this.customCodeEnabled) {
            return null;
        } else {
            Object keyObj = new Integer((int)code);
            return this.customCodeMap.get(keyObj);
        }
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(String code)
    {
        if (!this.customCodeEnabled || (code == null)) {
            return null;
        } else {
            Object keyObj = StringTools.trim(code).toLowerCase();
            return this.customCodeMap.get(keyObj);
        }
    }

    /**
    *** Translates the specified device status code into a GTS status code
    *** @param code           The code to translate
    *** @param dftStatusCode  The default code returned if no translation is defined
    *** @return The translated GTS status code
    **/
    public int translateStatusCode(int code, int dftStatusCode)
    {
        EventCode sci = this.getEventCode(code);
        return (sci != null)? sci.getStatusCode() : dftStatusCode;
    }

    /**
    *** Translates the specified device status code into a GTS status code
    *** @param code           The code to translate
    *** @param dftStatusCode  The default code returned if no translation is defined
    *** @return The translated GTS status code
    **/
    public int translateStatusCode(String code, int dftStatusCode)
    {
        EventCode sci = this.getEventCode(code);
        return (sci != null)? sci.getStatusCode() : dftStatusCode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the device command listen host (may be null to use default bind-address)
    *** @param cmdHost The device command listen host
    **/
    public void setCommandDispatcherHost(String cmdHost)
    {
        this.commandHost = cmdHost;
    }

    /**
    *** Gets the device command listen host
    *** @return The device command listen host
    **/
    public String getCommandDispatcherHost(Device device)
    {
        if (!StringTools.isBlank(this.commandHost)) {
            return this.commandHost;
        } else
        if ((device != null) && device.hasDcsCommandHost()) {
            return device.getDcsCommandHost();
        } else
        if (!StringTools.isBlank(DCServerFactory.BIND_ADDRESS)) {
            return DCServerFactory.BIND_ADDRESS;
        } else {
            // DCServer.DCSNAME.bindAddress
            String bindKey  = DCServerFactory.PROP_DCServer_ + this.getName() + "." + DCServerFactory.ATTR_bindAddress;
            String bindAddr = this.getStringProperty(bindKey, null);
            return !StringTools.isBlank(bindAddr)? bindAddr : "localhost";
        }
    }

    /**
    *** Sets the device command listen port
    *** @param cmdPort  The device command listen port
    *** @param checkRTP True to allow the RTConfig propertiesto override this value
    **/
    public void setCommandDispatcherPort(int cmdPort, boolean checkRTP)
    {
        if (checkRTP) {
            int port = 0;
            // First try command-line override
            RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
            if ((cmdLineProps != null) && cmdLineProps.hasProperty(DCServerFactory.ARG_commandPort)) {
                port = cmdLineProps.getInt(DCServerFactory.ARG_commandPort, 0);
            }
            // then try standard runtime config override
            if (port <= 0) {
                port = this.getIntProperty(DCServerFactory.CONFIG_commandPort(this.getName()), 0);
            }
            // change port if overridden
            if (port > 0) {
                cmdPort = port;
            }
        }
        this.commandPort = cmdPort;
    }

    /**
    *** Sets the device command listen port
    *** @param cmdPort  The device command listen port
    **/
    public void setCommandDispatcherPort(int cmdPort)
    {
        this.setCommandDispatcherPort(cmdPort,false);
    }

    /**
    *** Gets the device command listen port (returns '0' if not supported)
    *** @return The device command listen port
    **/
    public int getCommandDispatcherPort()
    {
        return this.commandPort;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Commands Acl name
    *** @param aclName The Commands Acl name
    **/
    public void setCommandsAclName(String aclName, AclEntry.AccessLevel dft)
    {
        this.commandsAclName        = StringTools.trim(aclName);
        this.commandsAccessLevelDft = dft;
    }

    /**
    *** Gets the Commands Acl name
    *** @return The Commands Acl name
    **/
    public String getCommandsAclName()
    {
        return this.commandsAclName;
    }

    /**
    *** Gets the Commands Acl AccessLevel default
    *** @return The Commands Acl AccessLevel default
    **/
    public AclEntry.AccessLevel getCommandsAccessLevelDefault()
    {
        return this.commandsAccessLevelDft;
    }

    /**
    *** Returns True if the specified user has access to the named command
    **/
    public boolean userHasAccessToCommand(BasicPrivateLabel privLabel, User user, String commandName)
    {

        /* BasicPrivateLabel must be specified */
        if (privLabel == null) {
            return false;
        }

        /* get command */
        Command command = this.getCommand(commandName);
        if (command == null) {
            return false;
        }

        /* has access to commands */
        if (privLabel.hasWriteAccess(user, this.getCommandsAclName())) {
            return false;
        }

        /* has access to specific command? */
        if (privLabel.hasWriteAccess(user, command.getAclName())) {
            return false;
        }

        /* access granted */
        return true;

    }

    // ------------------------------------------------------------------------

    public void addCommand(
        String  cmdName   , String cmdDesc, boolean cmdEnable,
        String  cmdTypes[], 
        String  cmdAclName, AclEntry.AccessLevel cmdAclDft,
        String  cmdString , boolean hasArgs, Collection<DCServerConfig.CommandArg> cmdArgList,
        String  cmdProto  , long maxRteAge, boolean allowQueue,
        boolean expectAck , int expAckCode,
        long    stateMask , boolean stateVal,
        int     cmdSCode)
    {
        if (StringTools.isBlank(cmdName)) {
            Print.logError("Ignoring blank command name ["+this.getName()+"]");
        } else
        if ((this.commandMap != null) && this.commandMap.containsKey(cmdName)) {
            Print.logError("Command already defined ["+this.getName()+"]: " + cmdName);
        } else {
            Command cmd = new Command(this.getName(),
                cmdName   , cmdDesc, cmdEnable,
                cmdTypes  , 
                cmdAclName, cmdAclDft,
                cmdString , hasArgs, cmdArgList,
                cmdProto  , maxRteAge, allowQueue,
                expectAck , expAckCode,
                stateMask , stateVal,
                cmdSCode);
            /*
            if (false) {
                Print.sysPrintln("---------------------------------------");
                try {
                    JSON._Object cmdJson = cmd.toJsonObject();
                    String Cmd1 = cmd.toJsonObject().toString();
                    Print.sysPrintln("1) Command JSON:\n" + Cmd1);
                    Print.sysPrintln("-------");
                    Command newCmd = new Command(this.getName(),cmdJson);
                    String Cmd2 = newCmd.toJsonObject().toString();
                    Print.sysPrintln("2) Command JSON:\n" + Cmd2);
                    Print.sysPrintln("-------");
                    if (Cmd1.equals(Cmd2)) {
                        Print.sysPrintln("Match: true");
                    } else {
                        Print.sysPrintln("***** Match: FALSE *****");
                    }
                } catch (JSON.JSONParsingException jpe) {
                    Print.logError("Error parsing Command JSON: " + jpe);
                }
                Print.sysPrintln("---------------------------------------");
            }
            */
            if (this.commandMap == null) {
                this.commandMap = new OrderedMap<String,Command>();
            }
            this.commandMap.put(cmdName, cmd);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a list of command names
    *** @return The list of command names
    **/
    public String[] getCommandList()
    {
        if (ListTools.isEmpty(this.commandMap)) {
            return null;
        } else {
            return this.commandMap.keyArray(String.class);
        }
    }

    /**
    *** Gets the command's (name,description) map
    *** @param type The description type 
    *** @return The command's (name,description) map
    **/
    public Map<String,Command> getCommandMap(BasicPrivateLabel privLabel, User user, String type)
    {
        boolean inclReplCmds = true; // for now, include all commands
        String cmdList[] = this.getCommandList();
        if (!ListTools.isEmpty(cmdList)) {
            Map<String,Command> cmdMap = new OrderedMap<String,Command>();
            for (Command cmd : this.commandMap.values()) {
                if (!DCServerFactory.isCommandTypeAll(type) && !cmd.isType(type)) {
                    // ignore this command
                    Print.logDebug("Command '%s' is not property type '%s'", cmd.getName(), type);
                } else
                if ((privLabel != null) && !privLabel.hasWriteAccess(user,cmd.getAclName())) {
                    // user does not have access to this command
                    Print.logDebug("User does not have access to command '%s'", cmd.getName());
                } else
                if (cmd.isEnabled(privLabel)) {
                    String key  = cmd.getName();
                    String desc = cmd.getDescription();
                    String cstr = cmd.getCommandString();
                    //Print.logInfo("Command enabled: " + key);
                    if (StringTools.isBlank(cstr)) {
                        // skip blank commands
                        Print.logWarn("Command is blank: " + key);
                        continue;
                    } else
                    if (StringTools.isBlank(desc)) {
                        // skip commands with blank description
                        Print.logWarn("Command does not have a descripton: " + key);
                        continue;
                    } else
                    if (!inclReplCmds) {
                        if (cstr.indexOf("${") >= 0) { //}
                            // should not occur ('type' should not include commands that require parameters)
                            // found "${text}"
                            Print.logInfo("Skipping command string containing '${...}': " + key + "  " + cstr);
                            continue;
                        }
                    }
                    cmdMap.put(key,cmd);
                }
            }
            return cmdMap;
        } else {
            Print.logInfo("Command list is empty: " + this.getName());
            return null;
        }
    }

    /**
    *** Gets the command's (name,description) map
    *** @param privLabel The context PrivateLabel instance
    *** @param user      The context login User
    *** @param type      The description type 
    *** @return The command's (name,description) map
    **/
    public Map<String,String> getCommandDescriptionMap(BasicPrivateLabel privLabel, User user, String type)
    {
        Map<String,Command> cmdMap = this.getCommandMap(privLabel, user, type);
        if (!ListTools.isEmpty(cmdMap)) {
            Map<String,String> cmdDescMap = new OrderedMap<String,String>();
            for (Command cmd : cmdMap.values()) {
                String key  = cmd.getName();
                String desc = cmd.getDescription();
                cmdDescMap.put(key,desc); // Commands are pre-qualified
            }
            return cmdDescMap;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the commands, based on the specified JSON object
    *** @param cmdObj  The JSON command object
    **/
    public static OrderedMap<String,Command> ParseCommandMap_JSON(JSON._Object commands)
    {

        /* no commands specified? */
        if (commands == null) {
            Print.logWarn("JSON object is null");
            return null;
        }

        /* command array */
        String dcsName = commands.getStringForName(JSONKey_DCS, null);
        if (StringTools.isBlank(dcsName)) {
            Print.logWarn("JSON object does not specify the 'DCS' name");
            return null;
        }

        /* command array */
        JSON._Array cmdArray = commands.getArrayForName(JSONKey_Commands, null);
        if (cmdArray == null) {
            Print.logWarn("JSON object does not contain a 'Commands' array");
            return null;
        }

        /* parse JSON */
        try {
            OrderedMap<String,Command> map = new OrderedMap<String,Command>(); // this.commandMap
            for (int i = 0; i < cmdArray.size(); i++) {
                JSON._Object cmdObj = cmdArray.getObjectValueAt(i, null);
                if (cmdObj != null) {
                    Command cmd = new Command(dcsName, cmdObj);
                    map.put(cmd.getName(), cmd);
                }
            }
            return map;
        } catch (JSON.JSONParsingException jpe) {
            Print.logError("Command parse error: " + jpe);
            return null;
        }

    }

    /**
    *** Gets the commands as a JSON object
    *** @param privLabel The context PrivateLabel instance
    *** @param user      The context login User
    *** @param type      The description type 
    *** @return The JSON object containing this DCS commands
    **/
    public JSON._Object getCommands_JSON(BasicPrivateLabel privLabel, User user, String type)
    {

        /* create returned object */
        JSON._Object obj = new JSON._Object();
        obj.addKeyValue(JSONKey_DCS, this.getName());
        JSON._Array cmdArray = new JSON._Array();
        obj.addKeyValue(JSONKey_Commands, cmdArray);

        /* populate array */
        Map<String,Command> cmdMap = this.getCommandMap(privLabel, user, type);
        if (!ListTools.isEmpty(cmdMap)) {
            for (String cmdName : cmdMap.keySet()) {
                Command cmd = cmdMap.get(cmdName);
                JSON._Object cmdObj = cmd.toJsonObject();
                cmdArray.addValue(cmdObj);
            }
        }

        /* return object */
        return obj;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the named command
    *** @param name  The name of the command to return
    *** @return The Command instance (or null if not found)
    **/
    public Command getCommand(String name)
    {
        return (this.commandMap != null)? this.commandMap.get(name) : null;
    }

    /**
    *** Gets the "Command Description" for the specified command
    *** @param dft  The default "Command Description"
    *** @return The "Command Description" for the specified command
    **/
    public String getCommandDescription(String cmdName, String dft)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getDescription() : dft;
    }

    /**
    *** Gets the "Command String" for the specified command
    *** @param dft  The default "Command String"
    *** @return The "Command String" for the specified command
    **/
    public String getCommandString(String cmdName, String dft)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getCommandString() : dft;
    }

    /**
    *** Gets the status-code for the specified command.  An event with this 
    *** status code will be inserted into the EventData table when this command
    *** is sent to the device.
    *** @param code  The default status-code
    *** @return The status-code for the specified command
    **/
    public int getCommandAuditStatusCode(String cmdName, int code)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getAuditStatusCode() : code;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds a recommended runtime configiuration property key to this DCS
    *** @param key The recommended configuration property key
    **/
    public void addRecommendedConfigPropertyKey(String key)
    {
        if (!StringTools.isBlank(key)) {
            if (this.cfgPropKeys == null) {
                this.cfgPropKeys = new HashSet<String>();
            }
            this.cfgPropKeys.add(key.trim());
        }
    }

    /**
    *** Returns the recommended runtime configuration property key set
    *** @return The recommended runtime configuration property key set
    **/
    public Set<String> getRecommendedConfigPropertyKeys()
    {
        return this.cfgPropKeys;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the default RTProperties instance
    *** @return The default RTProperties instance
    **/
    public RTProperties getDefaultProperties()
    {
        RTProperties rtp = this.rtPropsMap.get(DEFAULT_PROP_GROUP_ID);
        if (rtp == null) {
            rtp = new RTProperties();
            this.rtPropsMap.put(DEFAULT_PROP_GROUP_ID, rtp);
        }
        return rtp;
    }

    /**
    *** Returns a set of RTProperties group names
    *** @return A set of RTProperties group names
    **/
    public Set<String> getPropertyGroupNames()
    {
        this.getDefaultProperties(); // make sure the detault properties are cached
        return this.rtPropsMap.keySet();
    }

    /** 
    *** Gets the RTProperties instance for the specified group name
    *** @param grpID  The property group name
    *** @return The matching group RTProperties instance
    **/
    public RTProperties getProperties(String grpID)
    {
        return this.getProperties(grpID, false);
    }

    /** 
    *** Gets the RTProperties instance for the specified group name
    *** @param grpID  The property group name
    *** @param createNewGroup  True to create this group if not already present.
    *** @return The matching group RTProperties instance
    **/
    public RTProperties getProperties(String grpID, boolean createNewGroup)
    {
        if (StringTools.isBlank(grpID) || grpID.equalsIgnoreCase(DEFAULT_PROP_GROUP_ID)) {
            // blank group, return default
            return this.getDefaultProperties();
        } else {
            RTProperties rtp = this.rtPropsMap.get(grpID);
            if (rtp != null) {
                // found, return properties group
                return rtp;
            } else
            if (createNewGroup) {
                // not found, create
                rtp = new RTProperties();
                this.rtPropsMap.put(grpID, rtp);
                return rtp;
            } else {
                // do not create, return default
                return this.getDefaultProperties();
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Prepend DCS name to key
    **/
    public String normalizeKey(String key)
    {
        if (StringTools.isBlank(key)) {
            return "";
        } else
        if (key.indexOf(this.getName() + ".") >= 0) {
            // "enfora.tcpPort"
            // "DCServer.enfora.tcpPort"
            return key;
        } else {
            // "tcpPort" ==> "enfora.tcpPort"
            return this.getName() + "." + key;
        }
    }

    /**
    *** Prepend DCS name to keys
    **/
    public String[] normalizeKeys(String key[])
    {
        if (!ListTools.isEmpty(key)) {
            for (int i = 0; i < key.length; i++) {
                key[i] = this.normalizeKey(key[i]);
            }
        }
        return key;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true is the property key has been defined.
    *** @param key  An array of property keys (true if any one of these keys are defined).
    *** @param inclDft  True to also test default property key definitions.
    *** @return True if the property key has been defined.
    **/
    public boolean hasProperty(String key[], boolean inclDft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return true;
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return true;
            } else {
                return RTConfig.hasProperty(k, inclDft);
            }
        }
    }

    /**
    *** Returns a set of all property keys that match the specified prefix.
    *** @param prefix  The property key prefix
    *** @return The set of matching property keys.
    **/
    public Set<String> getPropertyKeys(String prefix)
    {
        RTProperties rtp = this.getDefaultProperties();
        Set<String> propKeys = new HashSet<String>();

        /* regualr keys */
        propKeys.addAll(rtp.getPropertyKeys(prefix));
        propKeys.addAll(RTConfig.getPropertyKeys(prefix));

        /* normalized keys */
        String pfx = this.normalizeKey(prefix);
        propKeys.addAll(rtp.getPropertyKeys(pfx));
        propKeys.addAll(RTConfig.getPropertyKeys(pfx));

        return propKeys;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the String-array value for the specified property key 
    *** @param key  The property key
    *** @param dft  The default value if the property key is not defined
    *** @return The String-array value of the property
    **/
    public String[] getStringArrayProperty(String key, String dft[])
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getStringArray(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getStringArray(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getStringArray(k, dft);
            } else {
                // global original key
                return RTConfig.getStringArray(key, dft);
            }
        }
    }

    /**
    *** Gets the String-array value for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default value if the property key is not defined
    *** @return The String-array value of the property
    **/
    public String[] getStringArrayProperty(String key[], String dft[])
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getStringArray(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getStringArray(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getStringArray(k, dft);
            } else {
                // global original key
                return RTConfig.getStringArray(key, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the String value for the specified property key 
    *** @param key  The property key
    *** @param dft  The default value if the property key is not defined
    *** @return The String value of the property
    **/
    public String getStringProperty(String key, String dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getString(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getString(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getString(k, dft);
            } else {
                // global original key
                return RTConfig.getString(key, dft);
            }
        }
    }

    /**
    *** Gets the String value for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default value if the property key is not defined
    *** @return The String value of the property
    **/
    public String getStringProperty(String key[], String dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getString(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getString(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getString(k, dft);
            } else {
                // global original key
                return RTConfig.getString(key, dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Class for the specified property key 
    *** @param key  The property key
    *** @param dft  The default class if the property key is not defined
    *** @return The Class of the property
    **/
    public Class<?> getClassProperty(String key, Class<?> dft)
    {
        String cn = this.getStringProperty(key, null);
        return LoadClassForName(cn, null, dft);
    }

    /**
    *** Gets the Class for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default Class if the property key is not defined
    *** @return The Class of the property
    **/
    public Class<?> getClassProperty(String key[], Class<?> dft)
    {
        String cn = this.getStringProperty(key, null);
        return LoadClassForName(cn, null, dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a new CustomParser instance of the specified CustomParser class name
    *** @param cpClassName  The name of the CustomParser class
    *** @param dft          The default CustomParser instance returned if undefined
    *** @return The CustomParser instance
    **/
    private CustomParser _newCustomParserInstance(String cpClassName, CustomParser dft)
    {
        if (!StringTools.isBlank(cpClassName)) {
            try {
                Class cpClass = Class.forName(cpClassName);
                return (CustomParser)cpClass.newInstance();
            } catch (ClassNotFoundException cnfe) {
                Print.logError("CustomParser class not found: " + cpClassName);
            } catch (Throwable th) {
                Print.logException("Error instantiating CustomParser class", th);
            }
        }
        return dft;
    }

    /**
    *** Returns the CustomParser instance specified by the property key
    *** @param key  The property key
    *** @param dft  The default CustomParser instance if the property key is not defined
    *** @return The CustomParser instance
    **/
    public CustomParser getCustomParserInstance(String key, CustomParser dft)
    {
        String cpClassName = this.getStringProperty(key, null);
        return this._newCustomParserInstance(cpClassName, dft);
    }

    /**
    *** Returns the CustomParser instance specified by the property key
    *** @param key  An array of property keys
    *** @param dft  The default CustomParser instance if the property key is not defined
    *** @return The CustomParser instance
    **/
    public CustomParser getCustomParserInstance(String key[], CustomParser dft)
    {
        String cpClassName = this.getStringProperty(key, null);
        return this._newCustomParserInstance(cpClassName, dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Integer value for the specified property key 
    *** @param key  The property key
    *** @param dft  The default value if the property key is not defined
    *** @return The Integer value of the property
    **/
    public int getIntProperty(String key, int dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getInt(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getInt(k, dft);
            } else {
                return RTConfig.getInt(k, dft);
            }
        }
    }

    /**
    *** Gets the Integer value for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default value if the property key is not defined
    *** @return The Integer value of the property
    **/
    public int getIntProperty(String key[], int dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getInt(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getInt(k, dft);
            } else {
                return RTConfig.getInt(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Integer array for the specified property key 
    *** @param key  The property key
    *** @param dft  The default array if the property key is not defined
    *** @return The Integer array of the property
    **/
    public int[] getIntArrayProperty(String key, int dft[])
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getIntArray(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getIntArray(k, dft);
            } else {
                return RTConfig.getIntArray(k, dft);
            }
        }
    }

    /**
    *** Gets the Integer array for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default array if the property key is not defined
    *** @return The Integer array of the property
    **/
    public int[] getIntArrayProperty(String key[], int dft[])
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getIntArray(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getIntArray(k, dft);
            } else {
                return RTConfig.getIntArray(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Long value for the specified property key 
    *** @param key  The property key
    *** @param dft  The default value if the property key is not defined
    *** @return The Long value of the property
    **/
    public long getLongProperty(String key, long dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getLong(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getLong(k, dft);
            } else {
                return RTConfig.getLong(k, dft);
            }
        }
    }

    /**
    *** Gets the Long value for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default value if the property key is not defined
    *** @return The Long value of the property
    **/
    public long getLongProperty(String key[], long dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getLong(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getLong(k, dft);
            } else {
                return RTConfig.getLong(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the Double value for the specified property key 
    *** @param key  The property key
    *** @param dft  The default value if the property key is not defined
    *** @return The Double value of the property
    **/
    public double getDoubleProperty(String key, double dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getDouble(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getDouble(k, dft);
            } else {
                return RTConfig.getDouble(k, dft);
            }
        }
    }

    /**
    *** Gets the Double value for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default value if the property key is not defined
    *** @return The Double value of the property
    **/
    public double getDoubleProperty(String key[], double dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getDouble(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getDouble(k, dft);
            } else {
                return RTConfig.getDouble(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Boolean value for the specified property key 
    *** @param key  The property key
    *** @param dft  The default value if the property key is not defined
    *** @return The Boolean value of the property
    **/
    public boolean getBooleanProperty(String key, boolean dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getBoolean(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getBoolean(k, dft);
            } else {
                return RTConfig.getBoolean(k, dft);
            }
        }
    }

    /**
    *** Gets the Boolean value for the specified property key 
    *** @param key  An array of property keys
    *** @param dft  The default value if the property key is not defined
    *** @return The Boolean value of the property
    **/
    public boolean getBooleanProperty(String key[], boolean dft)
    {
        RTProperties rtp = this.getDefaultProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getBoolean(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getBoolean(k, dft);
            } else {
                return RTConfig.getBoolean(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the state of the indicated input bit within the mask for this device type.
    *** @param mask  The input mask from the device
    *** @param bit   The bit to test
    **/
    public boolean getDigitalInputState(long mask, int bit)
    {
        int ofs = this.getIntProperty(DCServerFactory.PROP_Attribute_InputOffset, -1);
        int b   = (ofs > 0)? (ofs - bit) : bit;
        return (b >= 0)? ((mask & (1L << b)) != 0L) : false;
    }

    /**
    *** Returns the state of the indicated output bit within the mask for this device type.
    *** @param mask  The output mask from the device
    *** @param bit   The bit to test
    **/
    public boolean getDigitalOutputState(long mask, int bit)
    {
        int ofs = this.getIntProperty(DCServerFactory.PROP_Attribute_OutputOffset, -1);
        int b   = (ofs > 0)? (ofs - bit) : bit;
        return (b >= 0)? ((mask & (1L << b)) != 0L) : false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the 'other' DCServerCOnfig is equal to this DCServerConfig
    *** based on the name.
    *** @param other  The other DCServerConfig instance.
    *** @return True if the other DCServerConfig as the same name as this DCServerConfig
    **/
    public boolean equals(Object other)
    {
        if (other instanceof DCServerConfig) {
            String thisName  = this.getName();
            String otherName = ((DCServerConfig)other).getName();
            return thisName.equals(otherName);
        } else {
            return false;
        }
    }

    /**
    *** Compares another DCServerConfig instance to this instance.
    *** @param other  The other DCServerConfig instance.
    *** @return 'compareTo' operator on DCServerConfig names.
    **/
    public int compareTo(Object other)
    {
        if (other instanceof DCServerConfig) {
            String thisName  = this.getName();
            String otherName = ((DCServerConfig)other).getName();
            return thisName.compareTo(otherName);
        } else {
            return -1;
        }
    }

    /**
    *** Return hashCode based on the DCServerConfig name
    *** @return this.getNmae().hashCoe()
    **/
    public int hashCode()
    {
        return this.getName().hashCode();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation
    **/
    public String toString()
    {
        return this.toString(true);
    }

    /**
    *** Returns a String representation of this instance
    *** @param inclName True to include the name in the returnsed String representation
    *** @return A String representation
    **/
    public String toString(boolean inclName)
    {
        // "(opendmtp) OpenDMTP Server [TCP=31000 UDP=31000 CMD=30050]
        StringBuffer sb = new StringBuffer();

        /* name/description */
        if (inclName) {
            sb.append("(").append(this.getName()).append(") ");
        }
        sb.append(this.getDescription()).append(" ");

        /* ports */
        sb.append("[");
        this.getPortsString(sb);
        sb.append("]");
        
        /* String representation */
        return sb.toString();

    }

    /**
    *** Returns a StringBuffer representation of the ports utilized.
    *** @param sb The StringBuffer to populate
    *** @return The input StringBuffer
    **/
    public StringBuffer getPortsString(StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        int p = 0;
        // TCP
        int tcp[] = this.getTcpPorts();
        if (!ListTools.isEmpty(tcp)) {
            if (p > 0) { sb.append(" "); }
            sb.append("TCP=" + StringTools.join(tcp,","));
            p++;
        }
        // UDP
        int udp[] = this.getUdpPorts();
        if (!ListTools.isEmpty(udp)) {
            if (p > 0) { sb.append(" "); }
            sb.append("UDP=" + StringTools.join(udp,","));
            p++;
        }
        // SAT
        int sat[] = this.getSatPorts();
        if (!ListTools.isEmpty(sat)) {
            if (p > 0) { sb.append(" "); }
            sb.append("SAT=" + StringTools.join(sat,","));
            p++;
        }
        // Command
        int cmd = this.getCommandDispatcherPort();
        if (cmd > 0) {
            if (p > 0) { sb.append(" "); }
            sb.append("CMD=" + cmd);
            p++;
        }
        // no ports?
        if (p == 0) {
            sb.append("no-ports");
        }
        return sb;
    }

    /**
    *** Returns a String representation of the ports utilized.
    *** @return The String representation
    **/
    public String getPortsString()
    {
        return this.getPortsString(null).toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return running jar file path
    **/
    public File[] getRunningJarPath()
    {
        return DCServerConfig.getRunningJarPath(this.getName());
    }

}
