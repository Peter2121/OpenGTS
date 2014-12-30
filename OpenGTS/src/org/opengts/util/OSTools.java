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
//  General OS specific tools
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/06/20  Martin D. Flynn
//     -Added method 'getProcessID()'
//  2010/05/24  Martin D. Flynn
//     -Added "getMemoryUsage", "printMemoryUsage"
//  2011/08/21  Martin D. Flynn
//     -Added "getOSTypeName"
//  2012/04/20  Martin D. Flynn
//     -Updated "isSunJava"
//  2012/10/16  Martin D. Flynn
//     -Added "writePidFile(...)"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.io.*;
import java.net.*;

public class OSTools
{

    // ------------------------------------------------------------------------

    private static final Object LockObject          = new Object();
    private static final Object MemoryLock          = new Object();
    private static final Object DiskLock            = new Object();
    private static final Object CpuLock             = new Object();

    // ------------------------------------------------------------------------
    // OS and JVM specific tools
    // ------------------------------------------------------------------------

    // Note: these values may change from release to release
    // Reference: http://lopica.sourceforge.net/os.html
    private static final int OS_INITIALIZE          = -1;
    public  static final int OS_TYPE_MASK           = 0x00FFFF00;
    public  static final int OS_SUBTYPE_MASK        = 0x000000FF;

    public  static final int OS_UNKNOWN             = 0x00000000;
    
    public  static final int OS_LINUX               = 0x00010100;
    public  static final int OS_LINUX_FEDORA        = 0x00000001; // not detected
    public  static final int OS_LINUX_CENTOS        = 0x00000002; // not detected
    public  static final int OS_LINUX_UBUNTU        = 0x00000003; // not detected
    public  static final int OS_LINUX_DEBIAN        = 0x00000004; // not detected

    public  static final int OS_UNIX                = 0x00010200;
    public  static final int OS_UNIX_SOLARIS        = 0x00000001;
    public  static final int OS_UNIX_SUNOS          = 0x00000002;
    public  static final int OS_UNIX_AIX            = 0x00000004;
    public  static final int OS_UNIX_DIGITAL        = 0x00000005;
    public  static final int OS_UNIX_HPUX           = 0x00000006;
    public  static final int OS_UNIX_IRIX           = 0x00000007;

    public  static final int OS_BSD                 = 0x00010300;
    public  static final int OS_BSD_FREEBSD         = 0x00000001;

    public  static final int OS_MACOS               = 0x00010500;
    public  static final int OS_MACOS_X             = 0x00000001;

    public  static final int OS_WINDOWS             = 0x00000700;
    public  static final int OS_WINDOWS_9X          = 0x00000001; // 95/98/ME
    public  static final int OS_WINDOWS_XP          = 0x00000002;
    public  static final int OS_WINDOWS_VISTA       = 0x00000003;
    public  static final int OS_WINDOWS_7           = 0x00000004;
    public  static final int OS_WINDOWS_CE          = 0x00000010;
    public  static final int OS_WINDOWS_NT          = 0x00000011;
    public  static final int OS_WINDOWS_2000        = 0x00000012;
    public  static final int OS_WINDOWS_2003        = 0x00000013;
    public  static final int OS_WINDOWS_CYGWIN      = 0x000000C0; // not detected

    private static       int OSType                 = OS_INITIALIZE;

    /**
    *** Returns the known OS type as an integer bitmask
    *** @return The OS type
    **/
    public static int getOSType()
    {
        if (OSType == OS_INITIALIZE) {
            String osName = System.getProperty("os.name").toLowerCase();
            //Print.logInfo("OS: " + osName);
            if (osName.startsWith("windows")) {
                OSType = OS_WINDOWS;
                if (osName.startsWith("windows xp")) {
                    OSType |= OS_WINDOWS_XP;
                } else
                if (osName.startsWith("windows 9") || osName.startsWith("windows m")) {
                    OSType |= OS_WINDOWS_9X;
                } else
                if (osName.startsWith("windows 7")) {
                    OSType |= OS_WINDOWS_7;
                } else
                if (osName.startsWith("windows vista")) {
                    OSType |= OS_WINDOWS_VISTA;
                } else
                if (osName.startsWith("windows nt")) {
                    OSType |= OS_WINDOWS_NT;
                } else
                if (osName.startsWith("windows 2000")) {
                    OSType |= OS_WINDOWS_2000;
                } else
                if (osName.startsWith("windows 2003")) {
                    OSType |= OS_WINDOWS_2003;
                } else
                if (osName.startsWith("windows ce")) {
                    OSType |= OS_WINDOWS_CE;
                }
            } else
            if (osName.startsWith("mac")) {
                // "Max OS X"
                OSType = OS_MACOS;
                if (osName.startsWith("mac os x")) {
                    OSType |= OS_MACOS_X;
                }
            } else
            if (osName.startsWith("linux")) {
                // "Linux"
                OSType = OS_LINUX;
            } else
            if (osName.startsWith("solaris")) {
                // "Solaris"
                OSType = OS_UNIX | OS_UNIX_SOLARIS;
            } else
            if (osName.startsWith("sunos")) {
                // "Solaris"
                OSType = OS_UNIX | OS_UNIX_SUNOS;
            } else
            if (osName.startsWith("hp ux") || osName.startsWith("hp-ux")) {
                // "HP UX"
                OSType = OS_UNIX | OS_UNIX_HPUX;
            } else
            if (osName.startsWith("digital unix")) {
                // "Digital Unix"
                OSType = OS_UNIX | OS_UNIX_DIGITAL;
            } else
            if (osName.startsWith("aix")) {
                // "AIX"
                OSType = OS_UNIX | OS_UNIX_AIX;
            } else
            if (osName.startsWith("irix")) {
                // "Irix"
                OSType = OS_UNIX | OS_UNIX_IRIX;
            } else
            if (osName.startsWith("freebsd")) {
                // "FreeBSD"
                OSType = OS_BSD | OS_BSD_FREEBSD;
            } else
            if (osName.indexOf("unix") >= 0) {
                // "*Unix*"
                OSType = OS_UNIX;
            } else
            if (osName.indexOf("linux") >= 0) {
                // "*Linux*"
                OSType = OS_LINUX;
            } else
            if (File.separatorChar == '/') {
                // "Linux"
                OSType = OS_LINUX;
            } else {
                OSType = OS_UNKNOWN;
            }
        }
        return OSType;
    }

    /**
    *** Returns the String representation of the specified OS type
    *** @param type The OS type
    *** @return The OS type name (never null)
    **/
    public static String getOSTypeName(int type, boolean inclSubtype)
    {
        switch (type & OS_TYPE_MASK) {
            case OS_LINUX   : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_LINUX_FEDORA    : return "LINUX_FEDORA";
                        case OS_LINUX_CENTOS    : return "LINUX_CENTOS";
                        case OS_LINUX_UBUNTU    : return "LINUX_CENTOS";
                        case OS_LINUX_DEBIAN    : return "LINUX_DEBIAN";
                        default                 : return "LINUX";
                    }
                } else {
                    return "LINUX";
                }
            case OS_UNIX    : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_UNIX_SOLARIS    : return "UNIX_SOLARIS";
                        case OS_UNIX_SUNOS      : return "UNIX_SUNOS";
                        case OS_UNIX_AIX        : return "UNIX_AIX";
                        case OS_UNIX_DIGITAL    : return "UNIX_DIGITAL";
                        case OS_UNIX_HPUX       : return "UNIX_HPUX";
                        case OS_UNIX_IRIX       : return "UNIX_IRIX";
                        default                 : return "UNIX";
                    }
                } else {
                    return "UNIX";
                }
            case OS_BSD     : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_BSD_FREEBSD     : return "BSD_FREEBSD";
                        default                 : return "BSD";
                    }
                } else {
                    return "BSD";
                }
            case OS_MACOS   : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_MACOS_X         : return "MACOS_X";
                        default                 : return "MACOS";
                    }
                } else {
                    return "MACOS";
                }
            case OS_WINDOWS : 
                if (inclSubtype) {
                    switch (type & OS_SUBTYPE_MASK) {
                        case OS_WINDOWS_9X      : return "WINDOWS_9X";
                        case OS_WINDOWS_XP      : return "WINDOWS_XP";
                        case OS_WINDOWS_VISTA   : return "WINDOWS_VISTA";
                        case OS_WINDOWS_7       : return "WINDOWS_7";
                        case OS_WINDOWS_2000    : return "WINDOWS_2000";
                        case OS_WINDOWS_NT      : return "WINDOWS_NT";
                        default                 : return "WINDOWS";
                    }
                } else {
                    return "WINDOWS";
                }
            default         : 
                return "UNKNOWN";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is the specified type
    *** @return True if the OS is the specified type
    **/
    public static boolean isOSType(int type)
    {
        int osType = getOSType();
        return ((osType & OS_TYPE_MASK) == type);
    }

    /**
    *** Returns true if the OS is the specified type
    *** @return True if the OS is the specified type
    **/
    public static boolean isOSType(int type, int subType)
    {
        int osType = getOSType();
        if ((osType & OS_TYPE_MASK) != type) {
            // type mismatch
            return false;
        } else
        if (subType <= 0) {
            // subtype not specified
            return true;
        } else {
            // test subtype
            return ((osType & OS_SUBTYPE_MASK & subType) != 0);
        }
    }

    /**
    *** Returns true if the OS is unknown
    *** @return True if the OS is unknown
    **/
    public static boolean isUnknown()
    {
        return (getOSType() == OS_UNKNOWN);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is a flavor of Windows
    *** @return True if the OS is a flavor of Windows
    **/
    public static boolean isWindows()
    {
        return isOSType(OS_WINDOWS);
    }

    /**
    *** Returns true if the OS is Windows XP
    *** @return True if the OS is Windows XP
    **/
    public static boolean isWindowsXP()
    {
        return isOSType(OS_WINDOWS, OS_WINDOWS_XP);
    }

    /**
    *** Returns true if the OS is Windows 95/98
    *** @return True if the OS is Windows 95/98
    **/
    public static boolean isWindows9X()
    {
        return isOSType(OS_WINDOWS, OS_WINDOWS_9X);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is Unix/Linux
    *** @return True if the OS is Unix/Linux
    **/
    public static boolean isLinux()
    {
        return isOSType(OS_LINUX) || isOSType(OS_UNIX);
    }

    /**
    *** Returns true if the OS is Apple Mac OS
    *** @return True if the OS is Apple Mac OS
    **/
    public static boolean isMacOS()
    {
        return isOSType(OS_MACOS);
    }

    /**
    *** Returns true if the OS is Apple Mac OS X
    *** @return True if the OS is Apple Mac OS X
    **/
    public static boolean isMacOSX()
    {
        return isOSType(OS_MACOS, OS_MACOS_X);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the current host name
    *** @return The current hostname
    **/
    public static String getHostName()
    {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            //Print.logException("Error", uhe);
            return "";
        }
    }

    /**
    *** Gets the current host IP address
    *** @return The current IP address
    **/
    public static String getHostIP()
    {
        try {
            String ip = StringTools.trim(InetAddress.getByName(InetAddress.getLocalHost().getHostName()));
            int    h  = ip.indexOf("/");
            return (h >= 0)? ip.substring(h+1) : ip;
        } catch (UnknownHostException uhe) {
            //Print.logException("Error", uhe);
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Attempts to get the computer serial number by executing specific 
    *** commands to obtain the value from the operating system.
    *** Returns null if unable to read the system serial number.
    **/
    public static String getSerialNumber()
    {
        String serialNum = null;
        if (isMacOS()) {
            serialNum = OSTools._MacOS_GetSerialNumber();
        } else
        if (isWindows()) {
            serialNum = OSTools._Windows_GetSerialNumber();
        } else
        if (isLinux()) {
            serialNum = OSTools._Linux_GetSerialNumber();
        }
        return serialNum;
    }

    private static String _MacOS_GetSerialNumber()
    {
        StringBuffer outSB = new StringBuffer();
        StringBuffer errSB = new StringBuffer();
        String cmd[] = { "/usr/sbin/system_profiler", "SPHardwareDataType" };
        OSTools.exec(cmd, outSB, errSB);
        if (outSB.length() > 0) {
            // look for "Serial Number (system): XXXXXXXXXXX"
            int outL = outSB.length();
            int sn = outSB.indexOf("Serial Number");
            if (sn >= 0) { sn = outSB.indexOf(":",sn); }
            if (sn > 0) {
                StringBuffer SN = new StringBuffer();
                sn++; // skip ":"
                while ((sn < outL)  && Character.isWhitespace(outSB.charAt(sn))) { sn++; }
                while ((sn < outL) && !Character.isWhitespace(outSB.charAt(sn))) { SN.append(outSB.charAt(sn++)); }
                if (SN.length() > 0) {
                    return SN.toString();
                }
            }
        }
        return null;
    }

    private static String _Linux_GetSerialNumber()
    {
        StringBuffer outSB = new StringBuffer();
        StringBuffer errSB = new StringBuffer();
        String cmd[] = { "lshal" }; // { "dmidecode", "-t", "system" };
        OSTools.exec(cmd, outSB, errSB);
        if (outSB.length() > 0) {
            // look for "Serial Number (system): XXXXXXXXXXX"
            int outL = outSB.length();
            int sn = outSB.indexOf("system.hardware.serial");
            if (sn >= 0) { sn = outSB.indexOf("'",sn); }
            if (sn > 0) {
                StringBuffer SN = new StringBuffer();
                sn++; // skip "'"
                while ((sn < outL)  && Character.isWhitespace(outSB.charAt(sn))) { sn++; }
                while ((sn < outL) && !Character.isWhitespace(outSB.charAt(sn))) { SN.append(outSB.charAt(sn++)); }
                if (SN.length() > 0) {
                    return SN.toString();
                }
            }
        }
        return null;
    }

    private static String _Windows_GetSerialNumber()
    {
        StringBuffer outSB = new StringBuffer();
        StringBuffer errSB = new StringBuffer();
        String cmd[] = { "wmic", "bios", "get", "serialnumber" };
        OSTools.exec(cmd, outSB, errSB);
        if (outSB.length() > 0) {
            // look for "SerialNumber\nXXXXXXX"
            int outL = outSB.length();
            int sn = outSB.indexOf("SerialNumber");
            if (sn >= 0) { sn += "SerialNumber".length(); }
            if (sn > 0) {
                StringBuffer SN = new StringBuffer();
                while ((sn < outL)  && Character.isWhitespace(outSB.charAt(sn))) { sn++; }
                while ((sn < outL) && !Character.isWhitespace(outSB.charAt(sn))) { SN.append(outSB.charAt(sn++)); }
                if (SN.length() > 0) {
                    return SN.toString();
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Attempts to get a unique system ID.<br>
    *** This ID may be suitable for a GPS tracking "UniqueID", however absolute
    *** uniqueness may not be guaranteed.<br>
    *** Currently, the returned ID is based on the localhost MAC address, if available.
    *** Otherwise it will return the local hostname (sans any space/'.' characters).
    *** @return A unique system ID
    **/
    public static String getUniqueSystemID()
    {
        StringBuffer sb = new StringBuffer();

        /* OS type */
        String os = OSTools.getOSTypeName(getOSType(),false);
        sb.append((os.length() > 3)? os.substring(0,3) : os);

        /* try reading the platform serial# */
        String serialNum = OSTools.getSerialNumber();
        if (!StringTools.isBlank(serialNum)) {
            sb.append(serialNum);
            return sb.toString();
        }

        /* try MAC-address from local network interfaces */
        // WARNING: "InetAddress.getLocalHost()" changes, depending on when the computer
        //  is attached to a local network, or not!!!
        try {
            InetAddress ia = InetAddress.getLocalHost(); // throws UnknownHostException
            NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
            byte   mac[]  = ni.getHardwareAddress();
            String macHex = StringTools.toHexString(mac);
            if (!macHex.equalsIgnoreCase("0B0B0B0B0B0B")) {
                sb.append(StringTools.toHexString(mac));
                return sb.toString();
            } else {
                // "0B0B0B0B0B0B" appears to be a generic ID for a non-real NetworkInterface
                // continue below ...
            }
        } catch (Throwable th) {
            // ignore
        }

        /* default */
        //return StringTools.stripChars(OSTools.getHostName(), new char[] { ' ', '.' });
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class CpuUsage
    {
        private int    availCores    = -1;
        private double procCpuLoad   = -1.0;
        private long   procCpuTime   = -1L;
        private double sysCpuLoad    = -1.0;
        private double sysCpuLoadAvg = -1.0;
        public CpuUsage() {
            super();
        }
        // ---
        public void setAvailableProcessors(int v) {
            this.availCores = v;
        }
        public int getAvailableProcessors() {
            return this.availCores;
        }
        // ---
        public void setProcessCpuLoad(double v) {
            this.procCpuLoad = v;
        }
        public double getProcessCpuLoad() {
            return this.procCpuLoad;
        }
        // ---
        public void setProcessCpuTimeNS(long v) {
            this.procCpuTime = v;
        }
        public long getProcessCpuTimeNS() {
            return this.procCpuTime;
        }
        // ---
        public void setSystemCpuLoad(double v) {
            this.sysCpuLoad = v;
        }
        public double getSystemCpuLoad() {
            return this.sysCpuLoad;
        }
        // ---
        public void setSystemLoadAverage(double v) {
            this.sysCpuLoadAvg = v;
        }
        public double getSystemLoadAverage() {
            return this.sysCpuLoadAvg;
        }
        // ---
        public String toString(boolean longFmt) {
            StringBuffer sb = new StringBuffer();
            // cpu values
            String fmt = longFmt? "0.0" : "0";
            String AP  = String.valueOf(this.getAvailableProcessors());
            String PCL = StringTools.format(this.getProcessCpuLoad()*100.0,fmt);
            String PCT = String.valueOf(this.getProcessCpuTimeNS());
            String SCL = StringTools.format(this.getSystemCpuLoad()*100.0,fmt);
            String SLA = StringTools.format(this.getSystemLoadAverage()*100.0,fmt);
            // header
            if (longFmt) {
                sb.append("CPU Usage: ");
            }
            // assemble
            sb.append("Cores="   ).append(AP ).append(", ");
            sb.append("ProcLoad=").append(PCL).append("%, ");
            sb.append("SysLoad/Avg=" ).append(SCL).append("%/").append(SLA).append("%, ");
            // return
            return sb.toString();
        }
    }

    /**
    *** Gets the CPU usage information
    *** @param cpu  The CpuUsage instance into which the CPU information is written.
    *** @return The specified CpuUsage instance, or a new instance if no CpuUsage
    ***         instance is provided.  Returns null if unable to obtain the CPU usage.
    **/
    public static OSTools.CpuUsage getCpuUsage(OSTools.CpuUsage cpu)
    {
        synchronized (OSTools.CpuLock) {
            // may not be supported on all platforms
            // com.sun.management.UnixOperatingSystem (OperatingSystemMXBean) may only contain the following:
            //  long getProcessCpuTime()
            //  long getTotalPhysicalMemorySize()
            //  long getFreePhysicalMemorySize()
            //  long getCommittedVirtualMemorySize()
            //  long getTotalSwapSpaceSize()
            //  long getFreeSwapSpaceSize()
            //  long getOpenFileDescriptorCount()
            //  long getMaxFileDescriptorCount()
            try {
                java.lang.management.OperatingSystemMXBean opSysMXBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                // available processors/cores
                //Method ap = opSysMXBean.getClass().getDeclaredMethod("getAvailableProcessors");
                //ap.setAccessible(true);
                //int numCores = StringTools.parseInt(ap.invoke(opSysMXBean),0);
                int numCores = Runtime.getRuntime().availableProcessors();
                // process cpu load
                Method pcl = opSysMXBean.getClass().getDeclaredMethod("getProcessCpuLoad");
                pcl.setAccessible(true);
                double procCpuLoad = StringTools.parseDouble(pcl.invoke(opSysMXBean),0.0);
                // process cpu time (nanoseconds)
                Method pct = opSysMXBean.getClass().getDeclaredMethod("getProcessCpuTime");
                pct.setAccessible(true);
                long procCpuTimeNS = StringTools.parseLong(pct.invoke(opSysMXBean),0L);
                // system cpu load
                Method scl = opSysMXBean.getClass().getDeclaredMethod("getSystemCpuLoad");
                scl.setAccessible(true);
                double sysCpuLoad = StringTools.parseDouble(scl.invoke(opSysMXBean),0.0);
                // system cpu load average
                Method scla = opSysMXBean.getClass().getDeclaredMethod("getSystemLoadAverage");
                scla.setAccessible(true);
                double sysCpuLoadAvg = StringTools.parseDouble(scla.invoke(opSysMXBean),0.0);
                // set cpu usage
                if (cpu == null) { cpu = new OSTools.CpuUsage(); }
                cpu.setAvailableProcessors(numCores);
                cpu.setProcessCpuLoad(procCpuLoad);
                cpu.setProcessCpuTimeNS(procCpuTimeNS);
                cpu.setSystemCpuLoad(sysCpuLoad);
                cpu.setSystemLoadAverage(sysCpuLoadAvg);
            } catch (Throwable th) {
                Print.logError("Unable to obtain system cpu usage- " + th);
                //Method M[] = opSysMXBean.getClass().getDeclaredMethods();
                //for (Method m : M) { Print.logInfo("Method: " + m); }
                cpu = null;
            }
        }
        return cpu;
    }

    /**
    *** Get the current CPU usage String<br>
    *** Note: may not be supported on all platforms
    **/
    public static String getCpuUsageString(boolean longFmt)
    {
        OSTools.CpuUsage cpu = OSTools.getCpuUsage(null);
        if (cpu != null) {
            return cpu.toString(longFmt);
        } else {
            return longFmt? "Cpu: n/a" : "";
        }
    }

    /**
    *** Get the current CPU usage String<br>
    *** Note: may not be supported on all platforms
    **/
    public static String getCpuUsageString()
    {
        return OSTools.getCpuUsageString(true);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final long K_BYTES    = 1024L;
    public static final long M_BYTES    = K_BYTES * K_BYTES;
    public static final long G_BYTES    = K_BYTES * K_BYTES * K_BYTES;
    public static String getMemoryUnits(long D)
    {
        if (D == 1L) {
            // bytes
            return "bytes";
        } else
        if (D == 1000L) {
            // metric Kb
            return "(Kb)";
        } else 
        if (D == K_BYTES) {
            // binary Kb
            return "Kb";
        } else 
        if (D == (1000L * 1000L)) {
            // metric Mb
            return "(Mb)";
        } else 
        if (D == M_BYTES) {
            // binary Mb
            return "Mb";
        } else 
        if (D == (1000L * 1000L * 1000L)) {
            // metric Gb
            return "(Gb)";
        } else 
        if (D == G_BYTES) {
            // binary Gb
            return "Gb";
        } else {
            // unknown divisor
            return "("+D+")";
        }
    }

    public static class MemoryUsage
    {
        private long memMax   = 0L;
        private long memTotal = 0L;
        private long memFree  = 0L;
        public MemoryUsage() {
            super();
        }
        public MemoryUsage(long max, long total, long free) {
            this.set(max, total, free);
        }
        public MemoryUsage(long total, long free) {
            this.set(total, free);
        }
        // ---
        public String getName() {
            return "Memory";
        }
        // ---
        public void set(long max, long total, long free) {
            this.memMax   = max;
            this.memTotal = total;
            this.memFree  = free;
        }
        public void set(long total, long free) {
            this.set(total, total, free);
        }
        // ---
        public long getMaximum() {
            return this.memMax;
        }
        public long getTotal() {
            return this.memTotal;
        }
        public long getFree() {
            return this.memFree;
        }
        public long getUsed() {
            if ((this.memTotal >= 0L) && (this.memFree >= 0L)) {
                return this.memTotal - this.memFree;
            } else {
                return -1L;
            }
        }
        // ---
        public double getMaximum(double D) {
            double M = (double)this.memMax;
            return ((M >= 0.0) && (D > 0.0))? (M / D) : -1.0;
        }
        public double getTotal(double D) {
            double M = (double)this.memTotal;
            return ((M >= 0.0) && (D > 0.0))? (M / D) : -1.0;
        }
        public double getFree(double D) {
            double M = (double)this.memFree;
            return ((M >= 0.0) && (D > 0.0))? (M / D) : -1.0;
        }
        public double getUsed(double D) {
            double T = this.getTotal(D);
            double F = this.getFree(D);
            return ((T >= 0.0) && (T >= F))? (T - F) : -1.0;
        }
        // ---
        public double getMaximum(long D) {
            return this.getMaximum((double)D);
        }
        public double getTotal(long D) {
            return this.getTotal((double)D);
        }
        public double getFree(long D) {
            return this.getFree((double)D);
        }
        public double getUsed(long D) {
            return this.getUsed((double)D);
        }
        // ---
        public double getMaximum_Kb() {
            return this.getMaximum(K_BYTES);
        }
        public double getTotal_Kb() {
            return this.getTotal(K_BYTES);
        }
        public double getFree_Kb() {
            return this.getFree(K_BYTES);
        }
        public double getUsed_Kb() {
            return this.getUsed(K_BYTES);
        }
        // ---
        public double getMaximum_Mb() {
            return this.getMaximum(M_BYTES);
        }
        public double getTotal_Mb() {
            return this.getTotal(M_BYTES);
        }
        public double getFree_Mb() {
            return this.getFree(M_BYTES);
        }
        public double getUsed_Mb() {
            return this.getUsed(M_BYTES);
        }
        // ---
        public double getMaximum_Gb() {
            return this.getMaximum(G_BYTES);
        }
        public double getTotal_Gb() {
            return this.getTotal(G_BYTES);
        }
        public double getFree_Gb() {
            return this.getFree(G_BYTES);
        }
        public double getUsed_Gb() {
            return this.getUsed(G_BYTES);
        }
        // ---
        public double getUtilization() {
            long max  = Math.max(this.getMaximum(),this.getTotal());
            long used = this.getUsed();
            if ((max > 0L) && (used >= 0L)) {
                return (double)used / (double)max;
            } else {
                return -1.0;
            }
        }
        public double getUsage() {
            return this.getUtilization();
        }
        public String getUsagePercent() {
            return Math.round(this.getUtilization() * 100.0) + "%";
        }
        // ---
        public int getTotalFieldLength(long D) {
            // used only for output formatting hints
            long val = (long)Math.ceil(this.getTotal(D));
            int digLen = String.valueOf(val).length();
            return digLen;
        }
        public int getTotalFieldLength(long D, MemoryUsage mainMu) {
            return (mainMu != null)? mainMu.getTotalFieldLength(D) : this.getTotalFieldLength(D);
        }
        // ---
        public String toString(long D, boolean longFmt) {
            StringBuffer sb = new StringBuffer();
            // memory values
            String fmt   = longFmt? "0.0" : "0";
            String Max   = (D == 1L)? String.valueOf(this.getMaximum()) : StringTools.format(this.getMaximum(D),fmt);
            String Total = (D == 1L)? String.valueOf(this.getTotal()  ) : StringTools.format(this.getTotal(D)  ,fmt);
            String Free  = (D == 1L)? String.valueOf(this.getFree()   ) : StringTools.format(this.getFree(D)   ,fmt);
            String Used  = (D == 1L)? String.valueOf(this.getUsed()   ) : StringTools.format(this.getUsed(D)   ,fmt);
            // header
            if (longFmt) {
                sb.append(this.getName());
                sb.append("-");
                sb.append(OSTools.getMemoryUnits(D)).append(": ");
            } else {
                //sb.append(OSTools.getMemoryUnits(D)).append(": ");
            }
            // assemble
            sb.append("Max="  ).append(Max  ).append(", ");
            sb.append("Total=").append(Total).append(", ");
            sb.append("Free=" ).append(Free ).append(", ");
            sb.append("Used=" ).append(Used ).append(" ");
            sb.append("[").append(this.getUsagePercent()).append("]");
            // return
            return sb.toString();
        }
        public String toString(long D) {
            return this.toString(D, true/*longFmt*/);
        }
        public String toString(boolean longFmt) {
            return this.toString(M_BYTES, longFmt);
        }
        public String toString() {
            return this.toString(M_BYTES, true/*longFmt*/);
        }
    }

    public static class DiskUsage
        extends MemoryUsage
    {
        public DiskUsage() {
            super();
        }
        public DiskUsage(long max, long total, long free) {
            super(max, total, free);
        }
        public DiskUsage(long total, long free) {
            super(total, free);
        }
        // ---
        public String getName() {
            return "Disk";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the System Memory usage information
    *** @param mu  The MemoryUsage instance into which the memory information is written
    *** @return The specified MemoryUsage instance, or a new instance if no MemoryUsage
    ***         instance is provided.  Returns null if unable to obtain the memory usage.
    **/
    public static OSTools.MemoryUsage getSystemMemoryUsage(OSTools.MemoryUsage mu)
    {

        /* get memory info */
        synchronized (OSTools.MemoryLock) {
            try {
                java.lang.management.OperatingSystemMXBean opSysMXBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                // total memory 
                Method tpms = opSysMXBean.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
                tpms.setAccessible(true);
                long totalB = StringTools.parseLong(tpms.invoke(opSysMXBean),0L);
                // free memory
                Method fpms = opSysMXBean.getClass().getDeclaredMethod("getFreePhysicalMemorySize");
                fpms.setAccessible(true);
                long freeB = StringTools.parseLong(fpms.invoke(opSysMXBean),0L);
                // set memory usage
                if ((totalB >= 0L) || (freeB >= 0L)) {
                    if (mu == null) { mu = new OSTools.MemoryUsage(); }
                    mu.set(totalB, freeB);
                } else {
                    Print.logWarn("Setting MemoryUsage to null");
                    mu = null;
                }
            } catch (Throwable th) {
                Print.logError("Unable to obtain system memory usage - " + th);
                mu = null;
            }
        }

        /* return */
        return mu;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the System Disk usage information
    *** @param mu  The DiskUsage instance into which the disk information is written
    *** @return The specified DiskUsage instance, or a new instance if no DiskUsage
    ***         instance is provided.  Returns null if unable to obtain the disk usage.
    **/
    public static OSTools.DiskUsage getSystemDiskUsage(File dir, OSTools.DiskUsage du)
    {

        /* partition directory */
        File partitionDir = null;
        if ((dir != null) && dir.isDirectory()) {
            partitionDir = dir;
        } else {
            partitionDir = RTConfig.getLoadedConfigDir();
            if (partitionDir == null) {
                return null;
            }
        }

        /* get disk info */
        synchronized (OSTools.DiskLock) {
            try {
                long totalB = partitionDir.getTotalSpace();  // bytes
                long freeB  = partitionDir.getUsableSpace(); // bytes
                if ((totalB >= 0L) || (freeB >= 0L)) {
                    if (du == null) { du = new OSTools.DiskUsage(); }
                    du.set(totalB, freeB);
                } else {
                    Print.logWarn("Setting MemoryUsage to null");
                    du = null;
                }
            } catch (Throwable th) {
                Print.logError("Unable to obtain system disk usage - " + th);
                du = null;
            }
        }
        return du;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the current memory utilization (as a percent/100).
    *** Returns a negative value if the memory utilization cannot be obtained.
    **/
    public static double getMemoryUtilization()
    {
        OSTools.MemoryUsage mem = OSTools.getMemoryUsage(null);
        return (mem != null)? mem.getUtilization() : -1.0;
    }
    
    /**
    *** Gets the current memory usage (in number of bytes) for the current process
    *** @param mu  Am OSTools.MemoryUsage instance that will be updates with the memory usage values.  
    ***           If 'null', a new MemoryUsage instance will be created.
    *** @return A MemoryUsage instance containing the memory usage values, or null if 
    ***         unable to obtain the memory usage values.
    **/
    public static OSTools.MemoryUsage getMemoryUsage(OSTools.MemoryUsage mu)
    {
        Runtime rt = Runtime.getRuntime();
        synchronized (OSTools.MemoryLock) {
            long max   = rt.maxMemory();
            long total = rt.totalMemory();
            long free  = rt.freeMemory();
            if ((max >= 0L) || (total >= 0L) || (free >= 0L)) {
                if (mu == null) { mu = new OSTools.MemoryUsage(); }
                mu.set(max, total, free);
            } else {
                Print.logWarn("Setting MemoryUsage to null");
                mu = null;
            }
        }
        return mu;
    }

    /**
    *** Get the current memory usage String
    **/
    public static String getMemoryUsageStringMb()
    {
        return OSTools.getMemoryUsageStringMb(true);
    }

    /**
    *** Get the current memory usage String
    **/
    public static String getMemoryUsageStringMb(boolean longFmt)
    {
        OSTools.MemoryUsage mem = OSTools.getMemoryUsage(null);
        return (mem != null)? mem.toString(M_BYTES,longFmt) : "Memory: n/a";
    }

    /**
    *** Prints the current memory usage to the log file
    **/
    public static void printMemoryUsage()
    {
        Print.logInfo(OSTools.getMemoryUsageStringMb());
    }
    
    /**
    *** Prints the current memory usage to the log file
    **/
    public static void printMemoryUsageMXBean()
    {

        /* Heap/Non-Heap */
        java.lang.management.MemoryMXBean memory = java.lang.management.ManagementFactory.getMemoryMXBean();
        java.lang.management.MemoryUsage heapUsage    = memory.getHeapMemoryUsage();
        java.lang.management.MemoryUsage nonHeapUsage = memory.getNonHeapMemoryUsage();
        Print.logInfo("Heap Memory Usage    : " + _formatMemoryUsage(heapUsage   ));
        Print.logInfo("Non-Heap Memory Usage: " + _formatMemoryUsage(nonHeapUsage)); 

        /* Pools */
        java.util.List<java.lang.management.MemoryPoolMXBean> memPool = java.lang.management.ManagementFactory.getMemoryPoolMXBeans();
        for (java.lang.management.MemoryPoolMXBean mp : memPool) {
            String      name      = mp.getName();
            java.lang.management.MemoryType  type      = mp.getType();
            java.lang.management.MemoryUsage estUsage  = mp.getUsage();
            java.lang.management.MemoryUsage peakUsage = mp.getPeakUsage();
            java.lang.management.MemoryUsage collUsage = mp.getCollectionUsage();
            Print.logInfo("Pool Usage: " + name + " [" + type + "]");
            Print.logInfo("  Estimate  : "  + _formatMemoryUsage(estUsage ));
            Print.logInfo("  Peak      : "  + _formatMemoryUsage(peakUsage));
            Print.logInfo("  Collection: "  + _formatMemoryUsage(collUsage));
        }

    }
    
    /**
    *** Formats a MemoryUsage instance
    **/
    private static String _formatMemoryUsage(java.lang.management.MemoryUsage u)
    {
        if (u != null) {
            long comm = u.getCommitted() / K_BYTES;
            long init = u.getInit()      / K_BYTES;
            long max  = u.getMax()       / K_BYTES;
            long used = u.getUsed()      / K_BYTES;
            StringBuffer sb = new StringBuffer();
            sb.append("[K]");
            sb.append(" Committed=").append(comm);
            sb.append(" Init=").append(init);
            sb.append(" Max=").append(max);
            sb.append(" Used=").append(used);
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Object  memoryCheckLock         = new Object();
    private static long    firstMem_maxB           = 0L;
    private static long    firstMem_usedB          = 0L;
    private static long    firstMem_time           = 0L;
    private static long    memoryCheckCount        = 0L;
    private static long    averMem_usedB           = 0L;
    private static long    lastMem_usedB           = 0L;

    /**
    *** Analyzes/Prints the current memory usage.<br>
    *** (This method only analyzes/prints memory usage if the current usage is less than 
    *** the previous usage, implying that a garbage collection has recently occured)<br>
    *** Useful for determining <b>IF</b> there are memory leaks, and how much it is leaking, 
    *** but useless for determining <b>WHERE</b> the leak is occurring.
    *** @param reset  True to reset the memory growth-rate checks.
    **/
    public static void checkMemoryUsage(boolean reset)
    {
        // http://olex.openlogic.com/wazi/2009/how-to-fix-memory-leaks-in-java/
        // http://java.sun.com/docs/hotspot/gc1.4.2/faq.html
        // http://java.dzone.com/articles/letting-garbage-collector-do-c

        /* memory check enabled? */
        if (!RTConfig.getBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE)) {
            return;
        }

        /* get current memory usage */
        long nowTime = DateTime.getCurrentTimeSec();
        long maxB, usedB;
        long averUsedB = 0L, firstUsedB = 0L, firstTime = 0L;
        long count = 0L;
        Runtime rt = Runtime.getRuntime();
        synchronized (OSTools.memoryCheckLock) {
            // reset?
            if (reset) {
                // start over
                OSTools.firstMem_maxB    = 0L;
                OSTools.firstMem_usedB   = 0L;
                OSTools.firstMem_time    = 0L;
                OSTools.memoryCheckCount = 0L;
                OSTools.averMem_usedB    = 0L;
                OSTools.lastMem_usedB    = 0L;
            }
            // get memory usage
            maxB  = rt.maxMemory();
            usedB = rt.totalMemory() - rt.freeMemory();
            if (usedB <= 0L) {
                // unlikely, but we need to check anyway
                Print.logWarn("Memory usage <= 0? " + usedB + " bytes");
            } else {
                Print.sysPrintln("UsedB="+usedB +", lastUsedB="+OSTools.lastMem_usedB);
                if (usedB < OSTools.lastMem_usedB) {
                    // garbage collection has occurred
                    if ((OSTools.firstMem_time <= 0L) || (usedB < OSTools.firstMem_usedB)) {
                        // store results after first garbage collection
                        OSTools.firstMem_maxB    = maxB;      // should never change
                        OSTools.firstMem_usedB   = usedB;
                        OSTools.firstMem_time    = nowTime;
                        OSTools.memoryCheckCount = 0L;
                        OSTools.averMem_usedB    = 0L;
                    }
                    firstUsedB = OSTools.firstMem_usedB; // cache for use outside synchronized section
                    firstTime  = OSTools.firstMem_time;  // cache for use outside synchronized section
                    // average "trend"
                    if (OSTools.averMem_usedB <= 0L) {
                        // initialize average
                        OSTools.averMem_usedB = usedB;
                    } else
                    if (usedB <= OSTools.averMem_usedB) {
                        // always reset to minimum used (ie. 100% downward trend)
                        OSTools.averMem_usedB = usedB;
                    } else {
                        // upward "trend" determined by weighting factor
                        double trendWeight = RTConfig.getDouble(RTKey.OSTOOLS_MEMORY_TREND_WEIGHT);
                        OSTools.averMem_usedB = OSTools.averMem_usedB + (long)((double)(usedB - OSTools.averMem_usedB) * trendWeight);
                    }
                    averUsedB = OSTools.averMem_usedB; // cache for use outside synchronized section
                    // count
                    count = ++OSTools.memoryCheckCount; // increment and cache count for use outside synchronized section
                }
                OSTools.lastMem_usedB = usedB; // save last used
            }
        } // synchronized

        /* return if a garbage collection has not just occurred */
        if (count <= 0L) {
            return;
        }

        /* analyze */
        double deltaHours = (double)(nowTime - firstTime) / 3600.0;
        long   deltaUsedB = averUsedB - firstUsedB; // could be <= 0
        long   grwBPH     = (deltaHours > 0.0)? (long)(deltaUsedB / deltaHours) : 0L; // bytes/hour
        long   grwBPC     = deltaUsedB / count; // bytes/hour

        /* message */
        long maxK  = maxB      / 1024;
        long usedK = usedB     / 1024;
        long averK = averUsedB / 1024;
        String s = "["+count+"] Memory-K max "+maxK+ ", used "+usedK+ " (trend "+averK+ " K "+grwBPH+" b/h "+ grwBPC+" b/c)";

        /* display */
        double maxPercent = RTConfig.getDouble(RTKey.OSTOOLS_MEMORY_USAGE_WARN);
        if (usedB >= (long)((double)maxB * maxPercent)) {
            Print.logWarn("**** More than "+(maxPercent*100.0)+"% of max memory has been used!! ****");
            Print.logWarn(s);
        } else {
            Print.logInfo(s);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Instrumentation instrumentation = null;

    /**
    *** Sets the Instrumentation instance.
    *** The Instrumentation instance is typically obtained from a call to "premain".
    **/
    public static void setInstrumentation(Instrumentation inst)
    {
        OSTools.instrumentation = inst;
    }

    /**
    *** Gets the Instrumentation instance
    **/
    public static Instrumentation getInstrumentation()
    {
        return OSTools.instrumentation;
    }

    /**
    *** Gets the byte size of the specified Object.
    *** Note: Requires a valid Instramentation instance.
    **/
    public static long getObjectSize(Object obj) 
    {
        if (obj != null) {
            try {
                Instrumentation I = OSTools.getInstrumentation();
                if (I != null) {
                    return I.getObjectSize(obj);
                }
            } catch (Throwable th) {
                Print.logStackTrace("Instramentation 'getObjectSize' error", th);
                return -2L;
            }
        }
        return -1L;
    }

    /**
    *** "premain" entry point used to obtain an Instramentation instance
    **/
    public static void premain(String agentArgs, Instrumentation inst)
    {
        OSTools.setInstrumentation(inst);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this implementation has a broken 'toFront' Swing implementation.<br>
    *** (may only be applicable on Java v1.4.2)
    *** @return True if this implementation has a broken 'toFront' Swing implementation.
    **/
    public static boolean isBrokenToFront()
    {
        return isWindows();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String PROPERTY_JAVA_HOME                   = "java.home";
    public static final String PROPERTY_JAVA_VENDOR                 = "java.vendor";
    public static final String PROPERTY_JAVA_SPECIFICATION_VERSION  = "java.specification.version";

    /**
    *** Returns true if executed from a Sun Microsystems JVM.
    *** @return True is executed from a Sun Microsystems JVM.
    **/
    public static boolean isSunJava()
    {
        String javaVendVal = System.getProperty(PROPERTY_JAVA_VENDOR); // "Sun Microsystems Inc."
        if ((javaVendVal == null) || 
            ((javaVendVal.indexOf("Sun Microsystems") < 0) && 
             (javaVendVal.indexOf("Oracle")           < 0) && 
             (javaVendVal.indexOf("Apple")            < 0)    )) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    @SuppressWarnings("proprietary")  // <-- does not work to supress the "Sun proprietary API" warning
    private static Class _getCallerClass(int frame)
        throws Throwable
    {
        return sun.reflect.Reflection.getCallerClass(frame + 1); // <== ignore any warnings
    }

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    public static Class getCallerClass(int frame)
    {
        try {
            // sun.reflect.Reflection.getCallerClass(0) == sun.reflect.Reflection
            // sun.reflect.Reflection.getCallerClass(1) == OSTools
            Class clz = OSTools._getCallerClass(frame + 1);
            //Print._println("" + (frame + 1) + "] class " + StringTools.className(clz));
            return clz;
        } catch (Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java (or other non-Sun version).
            Print.logException("Sun Microsystems version of Java is not in use", th);
            return null;
        }
    }

    /**
    *** Returns true if 'sun.reflect.Reflection' is present in the runtime libraries.<br>
    *** (will return true when running with the Sun Microsystems version of Java)
    *** @return True if 'getCallerClass' is available.
    **/
    public static boolean hasGetCallerClass()
    {
        try {
            OSTools._getCallerClass(0);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    /**
    *** Prints the class of the caller (debug purposes only)
    **/
    public static void printCallerClasses()
    {
        try {
            for (int i = 0;; i++) {
                Class clz = OSTools._getCallerClass(i);
                Print.logInfo("" + i + "] class " + StringTools.className(clz));
                if (clz == null) { break; }
            }
        } catch (Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java.
            Print.logException("Sun Microsystems version of Java is not in use", th);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Process-ID of this JVM invocation.<br>
    *** IMPORTANT: This implementation relies on a "convention", rather that a documented method
    *** of obtaining the process-id of this JVM within the OS.  <b>Caveat Emptor!</b><br>
    *** (On Windows, this returns the 'WINPID' which is probably useless anyway)
    *** @return The Process-ID
    **/
    public static int getProcessID()
    {
        // References:
        //  - http://blog.igorminar.com/2007/03/how-java-application-can-discover-its.html
        if (OSTools.isSunJava()) {
            try {
                // by convention, returns "<PID>@<host>" (until something changes, and it doesn't)
                String n = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                int pid = StringTools.parseInt(n,-1); // parse PID
                return pid;
            } catch (Throwable th) {
                Print.logException("Unable to obtain Process ID", th);
                return -1;
            }
        } else {
            return -1;
        }
    }

    /** 
    *** Gets the Process-ID of this JVM invocation.<br>
    *** Attempts to obtain the PID of this process by executing a subprocess that requests
    *** the PID of its parent.  This method does not work on Windows, and seems to return the
    *** wong parent PID on Linux.
    *** @return The PID of this process
    **/
    private static int _getProcessID()
    {
        try {
            String cmd[] = new String[] { "bash", "-c", "echo $PPID" };
            Process ppidExec = Runtime.getRuntime().exec(cmd);
            BufferedReader ppidReader = new BufferedReader(new InputStreamReader(ppidExec.getInputStream()));
            StringBuffer sb = new StringBuffer();
            for (;;) {
                String line = ppidReader.readLine();
                if (line == null) { break; }
                sb.append(StringTools.trim(line));
            }
            int pid = StringTools.parseInt(sb.toString(),-1);
            int exitVal = ppidExec.waitFor();
            Print.logDebug("Exit value: %d [%s]", exitVal, sb.toString());
            ppidReader.close();
            return pid;
        } catch (Throwable th) {
            Print.logException("Unable to obtain PID", th);
            return -1;
        }
    }

    /**
    *** Writes the current PID into the specified file
    *** @param pidFile  The pid file to write
    *** @param overwrite True to overwrite if file already exists
    *** @return True if the file was successfully written
    **/
    public static boolean writePidFile(File pidFile, boolean overwrite)
    {

        /* pid file specified? */
        if (pidFile == null) {
            // not specified
            return false;
        }

        /* pid file already exists */
        if (pidFile.exists()) {
            if (!overwrite) {
                // do not overwrite
                return false;
            } else
            if (!pidFile.isFile()) {
                // exist, but it isn't a file
                return false;
            } else
            if (!FileTools.getExtension(pidFile).equalsIgnoreCase("pid")) {
                // existing file does not end with ".pid" 
                // (checked to prevent accidentally deleting other types of files)
                return false;
            }
            // exists and will be overwritten
        }

        /* get PID */
        int pid = OSTools.getProcessID();
        if (pid <= 0) {
            // unable to obtain PID
            return false;
        }

        /* write new pid file */
        try {
            byte pidB[] = (String.valueOf(pid)+"\n").getBytes();
            boolean ok = FileTools.writeFile(pidB, pidFile, false); // overwrite 
            return ok;
        } catch (IOException ioe) {
            Print.logError("Unable to write pid file: " + ioe);
            return false;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a Java command set up to be executed by Runtime.getRuntime().exec(...)
    *** @param classpath The classpath 
    *** @param className The main Java class name (or .jar file)
    *** @param args      The command line arguments
    *** @return A command to call and it's arguments
    **/
    public static String[] createJavaCommand(String classpath[], String className, String args[])
    {
        java.util.List<String> execCmd = new Vector<String>();
        execCmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

        /* classpath */
        execCmd.add("-classpath");
        if (ListTools.isEmpty(classpath)) {
            execCmd.add(System.getProperty("java.class.path"));
        } else {
            StringBuffer sb = new StringBuffer();
            for (String p : classpath) {
                if (sb.length() > 0) { sb.append(File.pathSeparator); }
                sb.append(p);
            }
            execCmd.add(sb.toString());
        }

        /* classname */
        if (StringTools.endsWithIgnoreCase(className,".jar")) {
            execCmd.add("-jar");
            execCmd.add(className);
        } else
        if (!StringTools.isBlank(className)) {
            execCmd.add(className);
        } else {
            execCmd.add("Main");
        }

        /* arguments */
        if (!ListTools.isEmpty(args)) {
            for (String a : args) {
                execCmd.add(a);
            }
        }

        /* array */
        return execCmd.toArray(new String[execCmd.size()]);

    }

    // ------------------------------------------------------------------------

    /**
    *** Reads the specified input string to stdout
    *** @param in  The InputStream to read/print
    *** @param sb  A StringBuffer used to accumulate stream bytes
    *** @return True if any data was read from the stream
    **/
    private static boolean _readOutput(InputStream in, StringBuffer sb)
        throws IOException
    {
        boolean didRead = false;
        for (;;) {
            int avail = in.available();
            if (avail > 0) {
                didRead = true;
                for (; avail > 0; avail--) {
                    int b = in.read();
                    if (b >= 0) {
                        sb.append((char)b);
                    } else {
                        // error?
                        return didRead;
                    }
                }
            } else {
                break;
            }
        }
        return didRead;
    }

    /**
    *** Executes the specified command in a separate process
    *** @param cmdArgs  The command and arguments to execute
    *** @param outSB    The StringBuffer into which the stdOut output is placed
    *** @param errSB    The StringBuffer into which the stdErr output is placed
    *** @return The process exit status code
    **/
    public static int exec(String cmdArgs[], StringBuffer outSB, StringBuffer errSB)
    {
        int exitStat = -1;
        if (!ListTools.isEmpty(cmdArgs)) {
            Process process = null;
            try {

                /* start process */
                if (cmdArgs.length > 1) {
                    process = Runtime.getRuntime().exec(cmdArgs);
                } else {
                    String cmd = StringTools.join(cmdArgs,' ');
                    process = Runtime.getRuntime().exec(cmd);
                }

                /* read available stdout/stderr */
                InputStream  stdout   = new BufferedInputStream(process.getInputStream());
                StringBuffer stdoutSB = (outSB != null)? outSB : new StringBuffer();
                InputStream  stderr   = new BufferedInputStream(process.getErrorStream());
                StringBuffer stderrSB = (errSB != null)? errSB : new StringBuffer();
                for (;;) {
                    try { Thread.sleep(100L); } catch (Throwable th) { /*ignore*/ }
                    boolean didReadOut = OSTools._readOutput(stdout, stdoutSB);
                    boolean didReadErr = OSTools._readOutput(stderr, stderrSB);
                    if (!didReadOut && !didReadErr) {
                        try {
                            process.exitValue();
                            break;
                        } catch (Throwable th) {
                            //Print.logDebug("Process not yet complete: " + th);
                            // continue
                        }
                    }
                }

                /* wait and get process exit value */
                for (;;) {
                    try {
                        process.waitFor();
                        return process.exitValue();
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
                
            } catch (Throwable th) {
                Print.logException("Job process failed", th);
                if (process != null) {
                    process.destroy();
                }
                return -1;
            }

        }
        return -1;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified object is an instance of (or equal to)
    *** the specified class name.
    **/
    public static boolean instanceOf(Object obj, String className)
    {
        if ((obj == null) || StringTools.isBlank(className)) {
            return false;
        } else {
            return StringTools.className(obj).equals(className);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sleeps for the specified number of milliseconds
    *** @param ms  Number of milliseconds to sleep
    *** @return True if sleep was performed without interruption, false otherwise
    **/
    public static boolean sleepMS(long ms)
    {
        if (ms < 0L) {
            return false;
        } else
        if (ms == 0L) {
            return true;
        } else {
            try {
                Thread.sleep(ms);
                return true;
            } catch (Throwable th) {
                return false;
            }
        }
    }

    /**
    *** Sleeps for the specified number of seconds
    *** @param sec  Number of milliseconds to sleep
    *** @return True if sleep was performed without interruption, false otherwise
    **/
    public static boolean sleepSec(long sec)
    {
        if (sec < 0L) {
            return false;
        } else
        if (sec == 0L) {
            return true;
        } else {
            try {
                Thread.sleep(sec * 1000L);
                return true;
            } catch (Throwable th) {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_WAIT[] = { "wait", "sleep" };

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        RTConfig.setBoolean(RTKey.LOG_FILE_ENABLE,false);
        Print.setAllOutputToStdout(true);
        
        /* OSTools.getMemoryUtilization performance */
        if (RTConfig.getBoolean("memUtilPerformance")) {
            long startMS = System.currentTimeMillis();
            long count = 0L;
            long duraSec = 4L;
            long duraMS = duraSec * 1000L;
            for (;;) {
                double util = getMemoryUtilization();
                count++;
                if ((System.currentTimeMillis() - startMS) > duraMS) {
                    break;
                }
            }
            long endMS = System.currentTimeMillis();
            long deltaMS = endMS - startMS;
            double perMS = (double)count / (double)deltaMS;
            double perSec = perMS * 1000.0;
            Print.sysPrintln("Total calls to 'getMemoryUtilization': " + count);
            Print.sysPrintln("Total milliseconds: " + deltaMS);
            Print.sysPrintln("'getMemoryUtilization'/sec = " + perSec);
            System.exit(0);
        }

        /* display current memory attributes */
        // -- 
        Print.sysPrintln("");
        Print.sysPrintln("Host ...");
        Print.sysPrintln("Host Name   : " + getHostName());
        Print.sysPrintln("Host IP     : " + getHostIP());
        Print.sysPrintln("Unique ID   : " + getUniqueSystemID());
        Print.sysPrintln("Serial#     : " + getSerialNumber());
        // -- 
        Print.sysPrintln("");
        Print.sysPrintln("OS Type ...");
        Print.sysPrintln("Is Windows  : " + isWindows());
        Print.sysPrintln("Is Windows9X: " + isWindows9X());
        Print.sysPrintln("Is WindowsXP: " + isWindowsXP());
        Print.sysPrintln("Is Linux    : " + isLinux());
        Print.sysPrintln("Is MacOS    : " + isMacOS());
        Print.sysPrintln("Is MacOSX   : " + isMacOSX());
        // -- 
        Print.sysPrintln("");
        Print.sysPrintln("PID ...");
        Print.sysPrintln("PID #1      : " +  getProcessID() + " (using Java ManagementFactory)");
        Print.sysPrintln("PID #2      : " + _getProcessID() + " (using subprocess 'echo $PPID')");
        // -- 
        Print.sysPrintln("");
        Print.sysPrintln("Memory (K=1024) ...");
        OSTools.MemoryUsage jvmMem = OSTools.getMemoryUsage(null);
        if (jvmMem != null) {
            Print.sysPrintln("JVM Memory:");
            Print.sysPrintln("   Max   : " + StringTools.rightAlign(StringTools.format(jvmMem.getMaximum_Mb(),"0.000"),8) + " Mb");
            Print.sysPrintln("   Total : " + StringTools.rightAlign(StringTools.format(jvmMem.getTotal_Mb()  ,"0.000"),8) + " Mb");
            Print.sysPrintln("   Free  : " + StringTools.rightAlign(StringTools.format(jvmMem.getFree_Mb()   ,"0.000"),8) + " Mb");
        }
        OSTools.MemoryUsage sysMem = OSTools.getSystemMemoryUsage(null);
        if (sysMem != null) {
            Print.sysPrintln("System Memoy:");
            Print.sysPrintln("   Total : " + StringTools.rightAlign(StringTools.format(sysMem.getTotal_Mb()  ,"0.000"),8) + " Mb");
            Print.sysPrintln("   Free  : " + StringTools.rightAlign(StringTools.format(sysMem.getFree_Mb()   ,"0.000"),8) + " Mb");
        }
        // -- 
        RTConfig.setBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE,true);
        OSTools.lastMem_usedB = 99999999L;
        Print.sysPrintln("MemoryCheck : " + RTConfig.getBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE));
        OSTools.checkMemoryUsage(false);
        Print.sysPrintln("");
        // -- 
        Print.sysPrintln("Network Interface MAC addresses:");
        try {
            Enumeration<NetworkInterface> netEnum = NetworkInterface.getNetworkInterfaces();
            for (;netEnum.hasMoreElements();) {
                NetworkInterface ni = netEnum.nextElement();
                byte mac[] = ni.getHardwareAddress();
                Print.sysPrintln("  "+StringTools.toHexString(mac)+" - "+ni.getDisplayName()+" ["+ni.getName()+"]");
            }
        } catch (Throwable th) { // SocketException
            Print.sysPrintln("  Error: " + th);
        }
        Print.sysPrintln("");

        /* sleep (allows time to test "tasklist"/"taskkill" on Windows */
        // Note: tasklist/taskkill on Windows appears to only work from the "standard" command-prompt.
        // It does not work in the Cygwin shell.
        if (RTConfig.hasProperty(ARG_WAIT)) {
            long MS = RTConfig.getLong(ARG_WAIT,0L) * 1000L;
            Print.sysPrintln("Sleeping for "+MS+" milliseconds before terminating ...");
            try { Thread.sleep(MS); } catch (Throwable th) {/*ignore*/}
        }

    }

}
