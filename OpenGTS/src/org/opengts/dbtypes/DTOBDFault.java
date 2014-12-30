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
//  2007/02/21  Martin D. Flynn
//     -Initial release
//  2010/06/17  Martin D. Flynn
//     -Added support for J1939, OBDII
//  2010/09/09  Martin D. Flynn
//     -Modified method used for obtaining J1587 MID/PID/SID/FMI descriptions
//  2012/04/03  Martin D. Flynn
//     -Fixed "GetPropertyString_OBDII"
//  2013/04/08  Martin D. Flynn
//     -Changed OBDII/DTC encoded bitmask to support hex fault codes, ie "P11AF" [B04]
// ----------------------------------------------------------------------------
package org.opengts.dbtypes;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

public class DTOBDFault
    extends DBFieldType
{

    // ------------------------------------------------------------------------

    private static final String PACKAGE_OPENGTS_                = "org.opengts.";
    public  static final String PACKAGE_EXTRA_                  = PACKAGE_OPENGTS_ + "extra.";     // "org.opengts.extra."
    public  static final String PACKAGE_EXTRA_DBTOOLS_          = PACKAGE_EXTRA_   + "dbtools.";   // "org.opengts.extra.dbtools."

    // ------------------------------------------------------------------------
    
    public static final String PROP_MIL[]       = new String[] { "mil"   , "MIL"    };
    public static final String PROP_TYPE[]      = new String[] { "type"  , "TYPE"   };
    public static final String PROP_MID[]       = new String[] { "mid"   , "MID"    };
    public static final String PROP_SID[]       = new String[] { "sid"   , "SID"    };
    public static final String PROP_PID[]       = new String[] { "pid"   , "PID"    };
    public static final String PROP_FMI[]       = new String[] { "fmi"   , "FMI"    };
    public static final String PROP_SPN[]       = new String[] { "spn"   , "SPN"    };
    public static final String PROP_DTC[]       = new String[] { "dtc"   , "DTC"    };
    public static final String PROP_COUNT[]     = new String[] { "count" , "COUNT"  };
    public static final String PROP_ACTIVE[]    = new String[] { "active", "ACTIVE" };

    public static final String NAME_J1708       = "J1708";
    public static final String NAME_J1939       = "J1939";
    public static final String NAME_OBDII       = "OBDII";

    public static final String NAME_MID         = "MID";
    public static final String NAME_MID_DESC    = NAME_MID + ".desc";
    public static final String NAME_PID         = "PID";
    public static final String NAME_PID_DESC    = NAME_PID + ".desc";
    public static final String NAME_SID         = "SID";
    public static final String NAME_SID_DESC    = NAME_SID + ".desc";
    public static final String NAME_SPN         = "SPN";
    public static final String NAME_FMI         = "FMI";
    public static final String NAME_FMI_DESC    = NAME_FMI + ".desc";
    public static final String NAME_DTC         = "DTC";

    public static final long   TYPE_MASK        = 0x7000000000000000L;
    public static final int    TYPE_SHIFT       = 60;
    public static final long   TYPE_J1708       = 0x0000000000000000L;
    public static final long   TYPE_J1939       = 0x1000000000000000L;
    public static final long   TYPE_OBDII       = 0x2000000000000000L;

    public static final long   ACTIVE_MASK      = 0x0100000000000000L;
    public static final int    ACTIVE_SHIFT     = 56;

    public static final long   MID_MASK         = 0x00FFFFFF00000000L;
    public static final int    MID_SHIFT        = 32;
    
    public static final long   SPID_MASK        = 0x00000000FFFF0000L;
    public static final int    SPID_SHIFT       = 16;
    public static final long   SID_MASK         = 0x0000000080000000L;

    public static final long   FMI_MASK         = 0x000000000000FF00L;
    public static final int    FMI_SHIFT        =  8;
    
    public static final long   COUNT_MASK       = 0x00000000000000FFL;
    public static final int    COUNT_SHIFT      =  0;

    // ------------------------------------------------------------------------
    // OBDII DTC code examples
    //  Example: P0171
    // 1st character identifies the system related to the trouble code.
    //  P = Powertrain
    //  B = Body
    //  C = Chassis
    //  U = Network/Undefined
    // 2nd digit identifies whether the code is a generic code (same on all 
    // OBD-II equpped vehicles), or a manufacturer specific code.
    //  0 = SAE/Generic
    //  1 = Manufacturer specific
    //  2 = SAE/Generic
    //  3 = SAE/Generic(P3400-P3499) or Manufacturer(P3000-P3399)
    // 3rd digit denotes the type of subsystem that pertains to the code
    //  0 = Fuel and Air Metering and Auxilliary Emission Controls
    //  1 = Emission Management (Fuel or Air)
    //  2 = Injector Circuit (Fuel or Air)
    //  3 = Ignition or Misfire
    //  4 = Auxilliary Emission Control
    //  5 = Vehicle Speed & Idle Control
    //  6 = Computer & Output Circuit
    //  7 = Transmission
    //  8 = Transmission
    //  9 = SAE Reserved / Transmission
    //  0 = SAE Reserved
    //  A = Hybrid Propulsion
    //  B - SAE Reserved
    //  C - SAE Reserved
    //  D - SAE Reserved
    //  E - SAE Reserved
    //  F - SAE Reserved
    // 4th/5th digits, along with the others, are variable, and relate to a 
    // particular problem. 
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String GetDTCGenericDescrption(String dtc)
    {

        /* blank dtc? */
        dtc = StringTools.trim(dtc).toUpperCase();
        if (StringTools.isBlank(dtc)) {
            return "";
        }

        /* get characters */
        StringBuffer sb = new StringBuffer();
        char ch[] = StringTools.getChars(dtc); // at least one character

        /* first character */
        switch (ch[0]) {
            case 'P': 
                sb.append("Powertrain");
                break;
            case 'B': 
                sb.append("Body");
                break;
            case 'C': 
                sb.append("Chassis");
                break;
            case 'U': 
                sb.append("Network");
                break;
            default : 
                sb.append("?["+ch[0]+"]"); 
                break;
        }

        /* ["U"] Network (special case) */
        if (ch[0] == 'U') {
            if (!dtc.startsWith("U0")) {
                sb.append(", ");
                sb.append("Manufacturer Specific");
            } else
            if (dtc.startsWith("U00")) { // U00XX
                sb.append(", ");
                sb.append("Electrical");
            } else
            if (dtc.startsWith("U01")) { // U01XX
                sb.append(", ");
                sb.append("Communication");
            } else
            if (dtc.startsWith("U02")) { // U02XX
                sb.append(", ");
                sb.append("Communication");
            } else
            if (dtc.startsWith("U03")) { // U04XX
                sb.append(", ");
                sb.append("Software");
            } else
            if (dtc.startsWith("U04")) { // U04XX
                sb.append(", ");
                sb.append("Data");
            }
            return sb.toString();
        }

        /* second character */
        if (ch.length > 1) {
            sb.append(", ");
            if (ch[1] == '0') {
                sb.append("SAE");
                // continue
            } else
            if (ch[1] == '1') {
                sb.append("Manufacturer Specific");
                return sb.toString(); // exit now
            } else
            if (ch[1] == '2') {
                sb.append("SAE");
                // continue
            } else
            if (ch[1] == '3') {
                if (ch.length > 2) {
                    if ((ch[2] >= '0') && (ch[2] <= '3')) {
                        sb.append("Manufacturer Specific");
                        return sb.toString(); // exit now
                    } else {
                        sb.append("SAE");
                        // continue
                    }
                } else {
                    sb.append("SAE");
                    // continue
                }
            } else {
                sb.append("?["+ch[1]+"]");
                return sb.toString(); // exit now
            }
        }

        /* third character ('P' only) */
        if ((ch[0] == 'P') && (ch.length > 2)) {
            sb.append(", ");
            switch (ch[2]) {
                case '0': sb.append("Fuel/Air Metering and Aux Emissions"); break;
                case '1': sb.append("Fuel/Air Metering");                   break;
                case '2': sb.append("Fuel/Air Metering");                   break;
                case '3': sb.append("Ignition/Misfire");                    break;
                case '4': sb.append("Aux Emissions");                       break;
                case '5': sb.append("Speed/Idle/Inputs");                   break;
                case '6': sb.append("Computer/Output");                     break;
                case '7': sb.append("Transmission");                        break;
                case '8': sb.append("Transmission");                        break;
                case '9': sb.append("Transmission");                        break;
                case 'A': sb.append("Hybrid Propulsion");                   break;
                case 'B': sb.append("Reserved");                            break;
                case 'C': sb.append("Reserved");                            break;
                case 'D': sb.append("Reserved");                            break;
                case 'E': sb.append("Reserved");                            break;
                case 'F': sb.append("Reserved");                            break;
            }
        }

        /* return description */
        return sb.toString();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static long EncodeActive(boolean active)
    {
        return active? ACTIVE_MASK : 0L;
    }
    
    public static boolean DecodeActive(long fault)
    {
        if (DTOBDFault.IsJ1708(fault)) {
            return ((fault & ACTIVE_MASK) != 0L);
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    public static long EncodeSystem(int sys)
    {
        return ((long)sys << MID_SHIFT) & MID_MASK;
    }

    public static long EncodeSystem(char sys)
    {
        // OBDII: 'P', 'B', 'C', 'U' Powertrain
        return ((long)sys << MID_SHIFT) & MID_MASK;
    }

    public static int DecodeSystem(long fault)
    {
        return (int)((fault & MID_MASK) >> MID_SHIFT);
    }
    
    // ------------------------------------------------------------------------

    public static long EncodeSPID(int sub)
    {
        return ((long)sub << SPID_SHIFT) & SPID_MASK;
    }
    
    public static int DecodeSPID(long fault)
    {
        return (int)((fault & SPID_MASK) >> SPID_SHIFT);
    }

    public static int DecodePidSid(long fault)
    {
        return DecodeSPID(fault) & 0x0FFF;
    }

    // ------------------------------------------------------------------------

    public static long EncodeFMI(int fmi)
    {
        return ((long)fmi << FMI_SHIFT) & FMI_MASK;
    }
    
    public static int DecodeFMI(long fault)
    {
        return (int)((fault & FMI_MASK) >> FMI_SHIFT);
    }

    // ------------------------------------------------------------------------

    public static long EncodeCount(int count)
    {
        return ((long)count << COUNT_SHIFT) & COUNT_MASK;
    }
    
    public static int DecodeCount(long fault)
    {
        return (int)((fault & COUNT_MASK) >> COUNT_SHIFT);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    //  J1708: type=j1708 mil=1 mid=123 pid=123 fmi=1 count=1 active=false
    //  J1939: type=i1939 mil=1 spn=1234 fmi=12 count=1 active=false
    //  OBDII: type=obdii mil=1 dtc=P0071

    public static String GetPropertyString_OBDII(String dtcStr)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(PROP_TYPE[0]).append("=").append(NAME_OBDII);
        sb.append(" ");
        if (!StringTools.isBlank(dtcStr)) {
            sb.append(PROP_MIL[0]).append("=").append("1"); // assumed
            sb.append(" ");
            sb.append(PROP_DTC[0]).append("=").append(dtcStr);
        } else {
            sb.append(PROP_MIL[0]).append("=").append("0");
        }
        return sb.toString();
    }

    public static String GetPropertyString_OBDII(String dtc[])
    {
        String dtcStr = !ListTools.isEmpty(dtc)? StringTools.join(dtc,",") : "";
        return GetPropertyString_OBDII(dtcStr);
    }

    public static String GetPropertyString_OBDII(java.util.List<String> dtc)
    {
        String dtcStr = !ListTools.isEmpty(dtc)? StringTools.join(dtc,",") : "";
        return GetPropertyString_OBDII(dtcStr);
    }

    public static String GetPropertyString_OBDII(long dtcFault[])
    {
        if (ListTools.isEmpty(dtcFault)) {
            return GetPropertyString_OBDII("");
        } else {
            java.util.List<String> dtc = new Vector<String>();
            for (int i = 0; i < dtcFault.length; i++) {
                if (dtcFault[i] != 0L) {
                    dtc.add(GetFaultString(dtcFault[i]));
                }
            }
            return GetPropertyString_OBDII(dtc);
        }
    }

    // ------------------------

    public static String GetPropertyString(long fault)
    {
        StringBuffer sb = new StringBuffer();
        if ((fault & TYPE_MASK) == TYPE_J1708) {
            int     mid    = DecodeSystem(fault);
            int     fmi    = DecodeFMI(fault);
            int     count  = DecodeCount(fault);
            boolean active = DecodeActive(fault);
            sb.append(PROP_TYPE[0]).append("=").append(NAME_J1708);
            sb.append(" ");
            sb.append(PROP_MIL[0]).append("=").append(active?"1":"0");
            sb.append(" ");
            sb.append(PROP_MID[0]).append("=").append(mid);
            if (DTOBDFault.IsJ1708_SID(fault)) {
                int sid    = DecodePidSid(fault);
                sb.append(" ");
                sb.append(PROP_SID[0]).append("=").append(sid);
            } else {
                int pid    = DecodePidSid(fault);
                sb.append(" ");
                sb.append(PROP_PID[0]).append("=").append(pid);
            }
            sb.append(" ").append(PROP_FMI[0]).append("=").append(fmi);
            if (count > 1) {
                sb.append(" ");
                sb.append(PROP_COUNT[0]).append("=" + count);
            }
            if (!active) {
                sb.append(" ");
                sb.append(PROP_ACTIVE[0]).append("=false");
            }
        } else
        if ((fault & TYPE_MASK) == TYPE_J1939) {
            int     spn    = DecodeSystem(fault);
            int     fmi    = DecodeFMI(fault);
            int     count  = DecodeCount(fault);
            boolean active = true;
            sb.append(PROP_TYPE[0]).append("=").append(NAME_J1939);
            sb.append(" ");
            sb.append(PROP_MIL[0]).append("=").append(active?"1":"0");
            sb.append(" ");
            sb.append(PROP_SPN[0]).append("=").append(spn);
            sb.append(" ");
            sb.append(PROP_FMI[0]).append("=").append(DecodeFMI(fault));
            if (count > 1) {
                sb.append(" ");
                sb.append(PROP_COUNT[0]).append("=" + count);
            }
            if (!active) {
                sb.append(" ");
                sb.append(PROP_ACTIVE[0]).append("=false");
            }
        } else
        if ((fault & TYPE_MASK) == TYPE_OBDII) {
            String dtc = DTOBDFault.GetFaultString(fault); // Powertrain
            sb.append(GetPropertyString_OBDII(new String[] { dtc }));
        } else {
            // unrecognized/empty
            sb.append(PROP_MIL[0]).append("=").append("0");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /* return string representation of fault code */
    public static String GetFaultString(RTProperties fault)
    {
        if (fault != null) {
            StringBuffer sb = new StringBuffer();
            String type = fault.getString(PROP_TYPE,"");
            if (type.equalsIgnoreCase(NAME_J1708)) {
                // J1708: type=j1708 mil=1 mid=123 pid=123 fmi=1 count=1 active=false
                // SID: "128/s123/1"
                // PID: "128/123/1"
                boolean active = fault.getBoolean(PROP_ACTIVE, true);
                int     mid    = fault.getInt(PROP_MID, 0);
                int     fmi    = fault.getInt(PROP_FMI, 0);
                if (!active) {
                    sb.append("[");
                }
                sb.append(mid);                     // MID
                sb.append("/");
                if (fault.hasProperty(PROP_SID)) {
                    int sid = fault.getInt(PROP_SID, 0);
                    sb.append("s").append(sid);     // SID "128/s123/1"
                } else {
                    int pid = fault.getInt(PROP_PID, 0);
                    sb.append(pid);                 // PID "128/123/1"
                }
                sb.append("/");
                sb.append(fmi);                     // FMI
                if (!active) {
                    sb.append("]");
                }
                return sb.toString();
            } else
            if (type.equalsIgnoreCase(NAME_J1939)) {
                // J1939: type=i1939 mil=1 spn=1234 fmi=12 count=1 active=false
                // SPN: "128/1"
                boolean active = fault.getBoolean(PROP_ACTIVE, true);
                int     spn    = fault.getInt(PROP_SPN, 0);
                int     fmi    = fault.getInt(PROP_FMI, 0);
                if (!active) {
                    sb.append("[");
                }
                sb.append(spn);                    // SPN
                sb.append("/");
                sb.append(fmi);                    // FMI
                if (!active) {
                    sb.append("]");
                }
                return sb.toString();
            } else
            if (type.equalsIgnoreCase(NAME_OBDII)) {
                // OBDII: type=obdii mil=1 dtc=P0071
                // DTC: "P0071" [was "024C"]
                boolean active  = fault.getBoolean(PROP_ACTIVE, true);
                String  dtcStr  = fault.getString(PROP_DTC, ""); // ie "P0123,P0321,P1234"
                if (!StringTools.isBlank(dtcStr)) {
                    if (!active) {
                        sb.append("[");
                    }
                    sb.append(dtcStr);
                    if (!active) {
                        sb.append("]");
                    }
                }
                return sb.toString();
            } else {
                // unrecognized
            }
        }
        return "";
    }

    /* return fault header */
    public static String GetFaultHeader(RTProperties fault)
    {
        if (fault != null) {
            String type = fault.getString(PROP_TYPE,"");
            if (type.equalsIgnoreCase(NAME_J1708)) {
                if (fault.hasProperty(PROP_SID)) {
                    return NAME_MID + "/" + NAME_SID + "/" + NAME_FMI;
                } else {
                    return NAME_MID + "/" + NAME_PID + "/" + NAME_FMI;
                }
            } else
            if (type.equalsIgnoreCase(NAME_J1939)) {
                return NAME_SPN + "/" + NAME_FMI;
            } else
            if (type.equalsIgnoreCase(NAME_OBDII)) {
                return NAME_DTC;
            } else {
                return "";
            }
        }
        return "";
    }

    // ------------------------------------------------------------------------

    /* return string representation of fault code */
    public static String GetFaultString(long fault)
    {
        if (fault > 0L) {
            StringBuffer sb = new StringBuffer();
            if ((fault & TYPE_MASK) == TYPE_J1708) {
                // SID: "128/s123/1"
                // PID: "128/123/1"
                boolean active = DTOBDFault.DecodeActive(fault);
                int     mid    = DTOBDFault.DecodeSystem(fault);
                int     fmi    = DTOBDFault.DecodeFMI(fault);
                if (!active) {
                    sb.append("[");
                }
                sb.append(mid);                     // MID
                sb.append("/");
                if (DTOBDFault.IsJ1708_SID(fault)) {
                    int sid = DTOBDFault.DecodePidSid(fault);
                    sb.append("s").append(sid);     // SID "128/s123/1"
                } else {
                    int pid = DTOBDFault.DecodePidSid(fault);
                    sb.append(pid);                 // PID "128/123/1"
                }
                sb.append("/");
                sb.append(fmi);                     // FMI
                if (!active) {
                    sb.append("]");
                }
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_J1939) {
                // SPN: "128/1"
                boolean active = DTOBDFault.DecodeActive(fault);
                int     spn    = DTOBDFault.DecodeSystem(fault);
                int     fmi    = DTOBDFault.DecodeFMI(fault);
                sb.append(spn);                    // SPN
                sb.append("/");
                sb.append(fmi);                    // FMI
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_OBDII) {
                // DTC: "P0071" [was "024C"]
                boolean active  = DTOBDFault.DecodeActive(fault);
                int     sysChar = DTOBDFault.DecodeSystem(fault);   // System: powertrain
                int     subSys  = DTOBDFault.DecodeSPID(fault);     // Mfg/Subsystem/Problem
                if (Character.isLetter((char)sysChar)) {
                    sb.append((char)sysChar);
                } else {
                    sb.append("?");
                }
                //sb.append(((subSys & 0x8000) != 0)? "1" : "0");
                //String subSysStr = String.valueOf(1000 + ((subSys & 0xFFF) % 1000));
                //sb.append(subSysStr.substring(1)); // skip first char
                sb.append(StringTools.toHexString((long)subSys&0xFFFFL,16));  // [2.4.9-B04] decode as HEX
                return sb.toString();
            } else {
                // unrecognized
            }
        }
        return "";
    }

    /* return fault header */
    public static String GetFaultHeader(long fault)
    {
        if ((fault & TYPE_MASK) == TYPE_J1708) {
            if (DTOBDFault.IsJ1708_SID(fault)) {
                return NAME_MID + "/" + NAME_SID + "/" + NAME_FMI;
            } else {
                return NAME_MID + "/" + NAME_PID + "/" + NAME_FMI;
            }
        } else
        if ((fault & TYPE_MASK) == TYPE_J1939) {
            return NAME_SPN + "/" + NAME_FMI;
        } else
        if ((fault & TYPE_MASK) == TYPE_OBDII) {
            return NAME_DTC;
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* encode "type=<type> ..." into long value */
    public static long EncodeFault(String faultProps)
    {
        if (!StringTools.isBlank(faultProps)) {
            return DTOBDFault.EncodeFault(new RTProperties(faultProps));
        } else {
            return 0L;
        }
    }

    /* encode "type=<type> ..." into long value */
    public static long EncodeFault(RTProperties rtp)
    {
        String type = rtp.getString(PROP_TYPE,"");
        if (type.equalsIgnoreCase(NAME_J1708)) {
            int     mid    = rtp.getInt(PROP_MID,0);
            int     sid    = rtp.getInt(PROP_SID,-1);
            int     pid    = rtp.getInt(PROP_PID,-1);
            int     pidSid = (sid >= 0)? sid : pid;
            int     fmi    = rtp.getInt(PROP_FMI,0);
            int     count  = rtp.getInt(PROP_COUNT,0);
            boolean active = rtp.getBoolean(PROP_ACTIVE,true);
            return EncodeFault_J1708(mid, (sid >= 0), pidSid, fmi, count, active);
        } else
        if (type.equalsIgnoreCase(NAME_J1939)) {
            int     spn    = rtp.getInt(PROP_SPN,0);
            int     fmi    = rtp.getInt(PROP_FMI,0);
            int     count  = rtp.getInt(PROP_COUNT,0);
            boolean active = rtp.getBoolean(PROP_ACTIVE,true);
            return EncodeFault_J1939(spn, fmi, count);
        } else
        if (type.equalsIgnoreCase(NAME_OBDII)) {
            String  dtc    = rtp.getString(PROP_DTC,""); // "P0071"
            return EncodeFault_OBDII(dtc);
        } else {
            return 0L;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* encode J1708 MID,PID/SID,FMI into long value */
    // 0x0100000000000000
    public static long EncodeFault_J1708(int mid, boolean isSID, int pidSid, int fmi, int count, boolean active)
    {
        int spid = isSID? (pidSid | 0x8000) : pidSid;
        long faultCode = TYPE_J1708;
        faultCode |= EncodeActive(active);      // [ACTIVE_MASK]    0x0100000000000000
        faultCode |= EncodeSystem(mid);         // [MID_MASK]       0x00FFFFFF00000000
        faultCode |= EncodeSPID(spid);          // [SPID_MASK]      0x00000000FFFF0000
        faultCode |= EncodeFMI(fmi);            // [FMI_MASK]       0x000000000000FF00
        faultCode |= EncodeCount(count);        // [COUNT_MASK]     0x00000000000000FF
        return faultCode;
    }

    /* return true if J1708 */
    public static boolean IsJ1708(long fault)
    {
        return ((fault & TYPE_MASK) == TYPE_J1708);
    }
    public static boolean IsJ1708(RTProperties rtpFault)
    {
        return ((rtpFault != null) && rtpFault.getString(PROP_TYPE,"").equalsIgnoreCase(NAME_J1708));
    }

    /* return true if long value represents a SID */
    public static boolean IsJ1708_SID(long fault)
    {
        return DTOBDFault.IsJ1708(fault) && ((fault & SID_MASK) != 0L);
    }
    public static boolean IsJ1708_SID(RTProperties rtpFault)
    {
        return DTOBDFault.IsJ1708(rtpFault) && rtpFault.hasProperty(PROP_SID);
    }

    /* return true if long value represents a SID */
    public static boolean IsJ1708_PID(long fault)
    {
        return DTOBDFault.IsJ1708(fault) && ((fault & SID_MASK) == 0L);
    }
    public static boolean IsJ1708_PID(RTProperties rtpFault)
    {
        return DTOBDFault.IsJ1708(rtpFault) && rtpFault.hasProperty(PROP_PID);
    }

    // ------------------------------------------------------------------------

    /* encode J1939 SPN,FMI into long value */
    public static long EncodeFault_J1939(int spn, int fmi, int count)
    {
        long faultCode = TYPE_J1939;
        faultCode |= EncodeActive(true);        // [ACTIVE_MASK]    0x0100000000000000
        faultCode |= EncodeSystem(spn);         // [MID_MASK]       0x00FFFFFF00000000
        faultCode |= EncodeFMI(fmi);            // [FMI_MASK]       0x000000000000FF00
        faultCode |= EncodeCount(count);        // [COUNT_MASK]     0x00000000000000FF
        return faultCode;
    }

    /* return true if J1939 */
    public static boolean IsJ1939(long fault)
    {
        return ((fault & TYPE_MASK) == TYPE_J1939);
    }
    public static boolean IsJ1939(RTProperties rtpFault)
    {
        return ((rtpFault != null) && rtpFault.getString(PROP_TYPE,"").equalsIgnoreCase(NAME_J1939));
    }

    // ------------------------------------------------------------------------

    /* encode OBDII DTC into long value */
    // [System][Manufacturer][SubSystem][Problem] eg. "P0071" Powertrain
    public static long EncodeFault_OBDII(String dtcStr)
    {
        long faultCode = TYPE_OBDII;

        /* trim */
        dtcStr = StringTools.trim(dtcStr);
        if (dtcStr.indexOf(",") >= 0) {
            dtcStr = dtcStr.substring(0,dtcStr.indexOf(",")).trim();
        }
        if (dtcStr.equals("")) {
            return faultCode;
        }

        /* check length */
        if (dtcStr.length() == 4) {
            dtcStr = "U" + dtcStr; // unknown
        } else
        if (dtcStr.length() != 5) {
            return faultCode;
        }

        /* active */
        faultCode |= EncodeActive(true);               // [ACTIVE_MASK]    0x0100000000000000

        /* encode system cjaracter (ie. "Powertrain") */
        faultCode |= EncodeSystem(dtcStr.charAt(0));   // [MID_MASK]       0x00FFFFFF00000000

        /* encode manufacturer specific and subsystem */
        //int mfgCode = StringTools.parseInt(dtcStr.substring(1,2),0); // .X...
        //int spid    = (mfgCode != 0)? 0x8000 : 0;
        //int subSys  = StringTools.parseInt(dtcStr.substring(2,5),0); // ..XXX   
        //spid |= (subSys & 0xFFF); // BCD encoded
        int spid = StringTools.parseHex(dtcStr.substring(1,5),0); //   [2.4.9-B04] encode to HEX
        faultCode |= EncodeSPID(spid);                 // [SPID_MASK]      0x00000000FFFF0000

        /* return fault code */
        return faultCode;

    }

    /* return true if OBDII */
    public static boolean IsOBDII(long fault)
    {
        return ((fault & TYPE_MASK) == TYPE_OBDII);
    }
    public static boolean IsOBDII(RTProperties rtpFault)
    {
        return ((rtpFault != null) && rtpFault.getString(PROP_TYPE,"").equalsIgnoreCase(NAME_OBDII));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean                  j1587DidInit        = false;
    private static J1587DescriptionProvider j1587DescProvider   = null;
    private static MethodAction             j1587GetDescription = null;

    public interface J1587DescriptionProvider
    {
        public Properties getJ1587Descriptions(long fault);
    }

    public static boolean InitJ1587DescriptionProvider()
    {
        if (!j1587DidInit) {
            j1587DidInit = true;
            try {
                j1587GetDescription = new MethodAction(PACKAGE_EXTRA_DBTOOLS_ + "J1587", "GetJ1587Description", Properties.class);
                j1587DescProvider   = new DTOBDFault.J1587DescriptionProvider() {
                    public Properties getJ1587Descriptions(long fault) {
                        if (DTOBDFault.IsJ1708(fault)) {
                            int     mid    = DTOBDFault.DecodeSystem(fault);    // MID
                            boolean isSid  = DTOBDFault.IsJ1708_SID(fault);
                            int     pidSid = DTOBDFault.DecodePidSid(fault);    // PID|SID "128/[s]123/1"
                            int     fmi    = DTOBDFault.DecodeFMI(fault);       // FMI
                            Properties p = new Properties();
                            p.setProperty("MID", String.valueOf(mid));
                            p.setProperty((isSid?"SID":"PID"), String.valueOf(pidSid));
                            p.setProperty("FMI", String.valueOf(fmi));
                            try {
                                return (Properties)j1587GetDescription.invoke(p);
                            } catch (Throwable th) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                };
                Print.logDebug("J1587 Description Provider installed ...");
            } catch (Throwable th) {
                //Print.logException("J1587 Description Provider NOT installed!", th);
            }
        }
        return (j1587DescProvider != null);
    }
    
    public static boolean HasDescriptionProvider(long fault)
    {
        if (DTOBDFault.IsJ1708(fault)) {
            return (j1587DescProvider != null);
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String GetFaultDescription(long fault, Locale locale)
    {
        if (fault != 0L) {
            String fmt = "000";
            StringBuffer sb = new StringBuffer();
            if ((fault & TYPE_MASK) == TYPE_J1708) {
                int     mid    = DTOBDFault.DecodeSystem(fault);    // MID
                boolean isSid  = DTOBDFault.IsJ1708_SID(fault);
                int     pidSid = DTOBDFault.DecodePidSid(fault);    // PID|SID "128/[s]123/1"
                int     fmi    = DTOBDFault.DecodeFMI(fault);       // FMI
                Properties p   = (j1587DescProvider != null)? j1587DescProvider.getJ1587Descriptions(fault) : new Properties();
                // MID
                sb.append(NAME_MID + "(" + StringTools.format(mid,fmt) + ") " + p.getProperty(NAME_MID_DESC,"") + "\n");
                // PID/SID
                if (isSid) {
                    sb.append(NAME_SID + "(" + StringTools.format(pidSid,fmt) + ") " + p.getProperty(NAME_SID_DESC,"") + "\n");
                } else {
                    sb.append(NAME_PID + "(" + StringTools.format(pidSid,fmt) + ") " + p.getProperty(NAME_PID_DESC,"") + "\n");
                }
                // FMI
                sb.append(NAME_FMI + "(" + StringTools.format(fmi,fmt) + ") " + p.getProperty(NAME_FMI_DESC,""));
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_J1939) {
                int spn = DTOBDFault.DecodeSystem(fault);          // SPN
                int fmi = DTOBDFault.DecodeFMI(fault);             // FMI
                Properties p = new Properties();
                // SPN
                sb.append(NAME_SPN + "(" + StringTools.format(spn,fmt) + ") " + p.getProperty(NAME_SPN,"") + "\n");
                // FMI
                sb.append(NAME_FMI + "(" + StringTools.format(fmi,fmt) + ") " + p.getProperty(NAME_FMI,""));
                return sb.toString();
            } else
            if ((fault & TYPE_MASK) == TYPE_OBDII) {
                String dtc = DTOBDFault.GetFaultString(fault);     // DTC
                Properties p = new Properties();
                p.put(NAME_DTC,GetDTCGenericDescrption(dtc));
                // DTC
                sb.append(NAME_DTC + "(" + dtc + ") " + p.getProperty(NAME_DTC,""));
                return sb.toString();
            }
        }
        return "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long    faultCode   = 0L;

    public DTOBDFault(int mid, boolean isSid, int pidSid, int fmi, int count, boolean active)
    {
        this.faultCode = DTOBDFault.EncodeFault_J1708(mid, isSid, pidSid, fmi, count, active);
    }

    public DTOBDFault(int spn, int fmi, int count)
    {
        this.faultCode = DTOBDFault.EncodeFault_J1939(spn, fmi, count);
    }

    public DTOBDFault(String dtc)
    {
        this.faultCode = DTOBDFault.EncodeFault_OBDII(dtc);
    }

    public DTOBDFault(long faultCode)
    {
        this.faultCode = faultCode;
    }

    public DTOBDFault(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        // set to default value if 'rs' is null
        this.faultCode = (rs != null)? rs.getLong(fldName) : 0L;
    }

    // ------------------------------------------------------------------------

    /* return fault code */
    public long getFaultCode()
    {
        return this.faultCode;
    }

    /* return multi-line description */
    public String getDescription()
    {
        return DTOBDFault.GetFaultDescription(this.getFaultCode(),null);
    }
    
    // ------------------------------------------------------------------------

    public boolean isJ1708()
    {
        return DTOBDFault.IsJ1708(this.getFaultCode());
    }

    public boolean isJ1939()
    {
        return DTOBDFault.IsJ1939(this.getFaultCode());
    }

    public boolean isOBDII()
    {
        return DTOBDFault.IsOBDII(this.getFaultCode());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public Object getObject()
    {
        return new Long(this.getFaultCode());
    }

    public String toString()
    {
        return "0x" + StringTools.toHexString(this.getFaultCode());
    }

    public boolean equals(Object other)
    {
        if (other instanceof DTOBDFault) {
            DTOBDFault jf = (DTOBDFault)other;
            return (this.getFaultCode() == jf.getFaultCode());
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        InitJ1587DescriptionProvider();
        RTProperties cmdLineProps = RTConfig.getCommandLineProperties();
        long fault = EncodeFault(cmdLineProps);
        Print.sysPrintln("Fault : " + fault + " [0x" + StringTools.toHexString(fault) + "]");
        Print.sysPrintln("String: " + GetPropertyString(fault)); 
        Print.sysPrintln("Desc  : " + GetFaultDescription(fault,null));
    }
    
}
