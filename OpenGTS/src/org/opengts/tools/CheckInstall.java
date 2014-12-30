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
//  This class performs post installation checks on the OpenGTS installation.
// ----------------------------------------------------------------------------
// Change History:
//  2008/02/17  Martin D. Flynn
//     -Initial release
//  2008/05/14  Martin D. Flynn
//     -Added additional 'private.xml' checks
//     -Additional changes to support Java 6.
//  2008/09/12  Martin D. Flynn
//     -Displays RuleFactory and PingDispatcher, if configured.
//  2008/12/01  Martin D. Flynn
//     -Added check for Cygwin directory symbolic links
//     -Display additional private-label Domain attributes
//  2009/01/01  Martin D. Flynn
//     -Added display of SMTP information
//  2009/01/28  Martin D. Flynn
//     -Added compile time
//     -Added character encoding information
//  2009/02/20  Martin D. Flynn
//     -Added check for initial SMTP host:port connection (3 second timeout)
//  2009/04/02  Martin D. Flynn
//     -Added check for "root" db username
//     -Added "Service Account" header.
//  2009/05/01  Martin D. Flynn
//     -Skip SMTP socket test if no SMTP host specified
//     -Added JAVA_HOME check for JRE
//     -Added CATALINA_HOME check for proper Tomcat installation
//     -Added check for 'readability' for various required library jars.
//     -Added check for running checkInstall as 'root'.
//  2009/05/24  Martin D. Flynn
//     -Added check for executable Tomcat startup/shutdown files.
//  2009/05/27  Martin D. Flynn
//     -Now insists on Java 6+
//  2009/06/01  Martin D. Flynn
//     -Removed check for 'activation.jar' (already present in Java 6)
//     -Added check for JavaMail, and SendMailArgs
//     -Perform additional checks on the comparison of JAVA_HOME vs PATH
//     -Attempt to compare private.xml with deployed track.war private.xml
//  2009/07/01  Martin D. Flynn
//     -Added cmd-line option ("localStrings") for LocalStrings_*.properties 
//      validation (validateLocalStrings).  Checks for invalid unicode-escaped
//      characters, and non-'ISO-8859-1' characters.
//     -Added ability to send a test email ("sendTestEmailTo").
//  2009/11/10  Martin D. Flynn
//     -Added check for 'private.xml' property "reportMenu.enableReportEmail".
//  2009/12/16  Martin D. Flynn
//     -Added list of defined reports.
//     -Added summary listing of warnings
//  2011/01/28  Martin D. Flynn
//     -Added symbolic link recommendations
//  2011/04/01  Martin D. Flynn
//     -Added check for non-readable files in Tomcat directory
//     -Added check for non-read/writable files in Log directory
//  2011/05/13  Martin D. Flynn
//     -Added check for MySQL "max-connections".
//  2011/06/16  Martin D. Flynn
//     -Added runtime config option to skip counting records
//  2011/07/01  Martin D. Flynn
//     -Added MobileLocationProvider information
//  2011/10/03  Martin D. Flynn
//     -Changed "max-connections" check in "my.cnf" to look for 'lastIndexOF("=")'
//      rather than 'indexOf("=")'.
//  2011/12/06  Martin D. Flynn
//     -Check for "max_connections" (and "max-connections")
//     -"java.vendor" now also scanned for "Oracle"
//  2012/02/03  Martin D. Flynn
//     -Added option for showing report limits
//     -Added display of SMS gateway configuration.
//     -Added ability to send test SMS message.
//     -Check for "max_connections" without prefixing command "#"
//  2012/05/27  Martin D. Flynn
//     -Added check for resolving the local hostname.
//  2012/12/24  Martin D. Flynn
//     -Added "db.dbConnectionPool" display (see RTKey.DB_DBCONNECTION_POOL)
//  2013/04/08  Martin D. Flynn
//     -Added check for SHA1, SHA2 hash algorithsm
//  2013/05/28  Martin D. Flynn
//     -Added support for displaying MySQL db engine type, and approximate 
//      EventData record count.
//  2013/08/06  Martin D. Flynn
//     -Group reports by type (see "reports.xml")
//  2013/08/27  Martin D. Flynn
//     -Added check for "/etc/mysql/my.cnf" (as well as "/etc/my.cnf")
//  2013/09/20  Martin D. Flynn
//     -Added "crontab" check (GTSE only)
//  2013/11/11  Martin D. Flynn
//     -Added disk utilization checks
//     -Added check for recommended DCS runtime config properties
//     -Additional checks for ReportCron required properties
//  2014/03/03  Martin D. Flynn
//     -Added list of currently loaded config files (see getLoadedURLs)
// ----------------------------------------------------------------------------
package org.opengts.tools;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import java.awt.Font;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.CompileTime;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.geocoder.ReverseGeocodeProvider;
import org.opengts.geocoder.ReverseGeocodeProviderAdapter;
import org.opengts.geocoder.GeocodeProvider;
import org.opengts.geocoder.GeocodeProviderAdapter;
import org.opengts.cellid.MobileLocationProvider;
import org.opengts.cellid.MobileLocationProviderAdapter;

import org.opengts.war.tools.*;

import org.opengts.war.report.ReportFactory;
import org.opengts.war.report.ReportConstraints;

public class CheckInstall
{

    // ------------------------------------------------------------------------

    private static final String  ARG_privateLabelDetail         = "privateLabelDetail";

    private static final String  PFX                            = "    ";

    private static final String  TRACK_CLASS_DIR                = "./build/track/WEB-INF/classes";

    private static final int     MAX_WIDTH                      = 120;
    private static final int     WRAP_WIDTH                     = 90;

    private static       boolean SHOW_REPORT_LIMITS             = false;

    private static       boolean SHOW_DB_ERRORS                 = false;

    private static final long    RECOMMENDED_MEMORY_MB          = 4096L;
    private static final double  THRESHOLD_DISK_UTIL            = 0.90;

    // ------------------------------------------------------------------------

    private static final String  CONFIG_FILES[]                 = {
        "webapp.conf",
        "default.conf",
        "common.conf",
        "system.conf",
        "authkeys.conf",
        "statusCodes.conf",
        "custom.conf",
        "custom_gts.conf",
        "config_old.conf",
        "config.conf",
        "private.xml",
        "private/private_common.xml",
        "reports.xml",
    };

    // ------------------------------------------------------------------------

    private static final String  PROP_skipDefaultEMailChecks[]  = new String[] { "checkInstall.skipDefaultEMailChecks", "skipDefaultEMailChecks", "skipEMailChecks" };
    private static final String  PROP_skipDefaultMapChecks[]    = new String[] { "checkInstall.skipDefaultMapChecks"  , "skipDefaultMapChecks"  , "skipMapChecks"   };
    private static final String  PROP_skipDBRecordCount[]       = new String[] { "checkInstall.skipDBRecordCount"     , "skipDBRecordCount"     , "noRecordCount"   };

    // ------------------------------------------------------------------------

    private static final String  ENVIRON_GTS_LINKS              = "GTS_LINKS";
    private static final String  ENVIRON_GTS_HOME               = DBConfig.env_GTS_HOME;
    private static final String  ENVIRON_GTS_CONF               = "GTS_CONF";
    private static final String  ENVIRON_GTS_CHARSET            = "GTS_CHARSET";
    private static final String  ENVIRON_JAVA_HOME              = "JAVA_HOME";
    private static final String  ENVIRON_ANT_HOME               = "ANT_HOME";
    private static final String  ENVIRON_CATALINA_HOME          = "CATALINA_HOME";
    private static final String  ENVIRON_MYSQL_HOME             = "MYSQL_HOME";
    private static final String  ENVIRON_CLASSPATH              = "CLASSPATH";
    private static final String  ENVIRON_PATH                   = "PATH";

    // ------------------------------------------------------------------------

    private static final String  REASON_DIR_NOT_EXIST           = "Java '<File>.isDirectory()' returned false";
    private static final String  REASON_FILE_NOT_EXIST          = "Java '<File>.isFile()' returned false";
    private static final String  REASON_SYSTEM_ERROR            = "Possible internal system error";

    private static final String  FIX_JAVA_VERSION               = "Please install Sun Microsystems Java version 1.6 (ie. 'Java 6')";
    private static final String  FIX_VALID_DIRECTORY            = "Please specify a valid directory path";
    private static final String  FIX_VALID_FILE                 = "Please specify a valid file path";
    private static final String  FIX_PREVIOUS_ERRORS            = "Fix previous errors, then re-run this installation check.";

    private static java.util.List<String[]> _errors = new Vector<String[]>();
    private static java.util.List<String[]> getErrors()
    {
        return _errors;
    }
    private static void clearErrors()
    {
        getErrors().clear();
    }
    private static void addError(String error, String reason, String fix, boolean fatal)
    {
        if (fatal) {
            getErrors().add(new String[] { error, reason, fix });
        } else {
            getErrors().add(new String[] { error, reason, fix, "false" });
        }
    }
    private static void addError(String error, String reason, String fix)
    {
        addError(error, reason, fix, true);
    }

    // ------------------------------------------------------------------------

    private static int                      warnCount = 0;
    private static java.util.List<String>   warnList  = new Vector<String>();
    
    private static int warnCount()
    {
        return warnCount;
    }
    
    private static java.util.List<String> getWarnings()
    {
        return warnList;
    }

    private static int countWarning(String msg)
    {
        int wc = ++warnCount;
        warnList.add(wc + ") " + msg);
        return wc;
    }

    // ------------------------------------------------------------------------
    
    public interface OutputHandler
    {
        public void checkInstallOutput(String m);
    }
    
    private static OutputHandler outputHandler = null;
    
    /* set output delegate */
    public static void setOutputHandler(final OutputHandler output)
    {
        if (output == null) {
            CheckInstall.outputHandler = null;
            BasicPrivateLabelLoader.setOutputHandler(null);
        } else {
            CheckInstall.outputHandler = output;
            BasicPrivateLabelLoader.setOutputHandler(new BasicPrivateLabelLoader.OutputHandler() {
                public void privateLabelOutput(String s) {
                    output.checkInstallOutput(s);
                }
            });
        }
    }

    /* output line to stdout */
    private static void println(String s)
    {
        if (outputHandler != null) {
            outputHandler.checkInstallOutput(s);
        } else {
            Print.sysPrintln(s);
        }
    }

    private static void wrapPrintln(String s, char sep)
    {

        /* extract prefixing spaces */
        int pfxNdx = 0;
        while (Character.isWhitespace(s.charAt(pfxNdx))) { pfxNdx++; }
        String prefix = s.substring(0, pfxNdx) + "  ";

        /* wrap */
        while (s.length() > WRAP_WIDTH) {
            int ch = WRAP_WIDTH;
            while ((ch > 0) && (s.charAt(ch) != sep)) { ch--; }
            if (ch > 0) {
                println(s.substring(0,ch+1));
                s = prefix + s.substring(ch+1).trim();
            } else {
                break;
            }
        }

        /* final line */
        if (s.length() > 0) {
            println(s);
        }

    }

    // ------------------------------------------------------------------------

    /* print a variable/key and it's value */
    private static void printVariable(String name, Object val, Object note)
    {
        int tab = 22;
        CheckInstall.printVariable(name, val, note, tab);
    }

    /* print a variable/key and it's value */
    private static void printVariable(String name, Object val, Object note, int tab)
    {
        int len = 2 + tab + 5;
        String nameFmt = "  " + StringTools.leftAlign(name,tab) + " ==> ";
        String v = (val  != null)? val.toString()  : "";
        String n = (note != null)? note.toString() : "";
        if (StringTools.isBlank(n)) {
            println(nameFmt + v);
        } else
        if (StringTools.isBlank(v)) {
            println(nameFmt + n);
        } else
        if ((nameFmt.length() + v.length() + "  ".length() + n.length()) < MAX_WIDTH) {
            println(nameFmt + v + "  " + n);
        } else {
            println(nameFmt + v);
            println(StringTools.replicateString(" ",len) + n);
        }
    }

    // ------------------------------------------------------------------------

    /* return the canonical directory for the specified environment variable */
    private static File getEnvironmentFile(String name, boolean isDirectory, boolean errorIfMissing)
    {

        /* get value */
        String val = null;
        try {
            val = System.getenv(name);
            if (StringTools.isBlank(val)) {
                if (errorIfMissing) {
                    printVariable(name, "", "(ERROR: not defined)");
                    addError("Environment variable '"+name+"' is not defined.", 
                             null,
                             "Please define the specified environment variable");
                } else {
                    printVariable(name, "", "(NOTE: not defined)");
                }
                return null;
            }
            if ((val.indexOf("\"") >= 0) || (val.indexOf("\'") >= 0)) {
                //val = StringTools.stripChars(val, '\"');
                printVariable(name, val, "(ERROR: contains quotes)");
                addError("Directory specification '"+name+"' contains quote characters.", 
                         null,
                         "Remove quotes from directory specification");
                return null;
            }
        } catch (Error err) {
            printVariable(name, "", "(ERROR: error retrieving environment variable)");
            addError("Error retrieving environment variable '"+name+"'.", 
                     "Possible invalid version of Java installed",
                     FIX_JAVA_VERSION);
            return null;
        }

        /* check for existance */
        File dir = new File(val);
        if (isDirectory) {
            if (!dir.isDirectory()) {
                // asked for a directory, but got a file
                if (FileTools.isCygwinSymlink(dir)) {
                    // file is a Cygwin symbolic link
                    File cygLink = FileTools.getCygwinSymlinkFile(dir,true);
                    if (FileTools.isDirectory(cygLink)) {
                        // Cygwin link appears to point to a directory, continue ...
                        dir = cygLink; // TODO: recursive check?
                    } else {
                        printVariable(name, val, "(ERROR: Cygwin link - "+cygLink+")");
                        addError("Environment variable '"+name+"' specifies a Cygwin symbolic link.", 
                                 "Directory appears to be a Cygwin symbolic link",
                                 "Please change environment value to a DOS absolute/canonical path");
                        return null;
                    }
                } else
                if (FileTools.isWindowsShortcut(dir)) {
                    // file is a Windows shortcut
                    File winLink = FileTools.getWindowsShortcutFile(dir,true);
                    if (FileTools.isDirectory(winLink)) {
                        // Windows symlink appears to point to a directory, continue ...
                        dir = winLink; // TODO: recursive check?
                    } else {
                        printVariable(name, val, "(ERROR: Windows shortcut - "+winLink+")");
                        addError("Environment variable '"+name+"' specifies a Windows shortcut.", 
                                 "Directory appears to be a Windows shortcut",
                                 "Please change environment value to a DOS absolute/canonical path");
                        return null;
                    }
                } else {
                    printVariable(name, val, "(ERROR: invalid directory)");
                    addError("Environment variable '"+name+"' specifies an invalid directory.", 
                             REASON_DIR_NOT_EXIST,
                             FIX_VALID_DIRECTORY);
                    return null;
                }
            }
        } else {
            if (!dir.isFile()) {
                printVariable(name, val, "(ERROR: invalid file)");
                addError("Environment variable '"+name+"' specifies an invalid file.", 
                         REASON_FILE_NOT_EXIST,
                         FIX_VALID_FILE);
                return null;
            }
        }

        /* canonical directory */
        try {
            dir = dir.getCanonicalFile();
        } catch (IOException ioe) {
            printVariable(name, val, "(ERROR: canonical error)");
            addError("Error retrieving canonical directory for environment variable '"+name+"'.", 
                     REASON_SYSTEM_ERROR,
                     null);
            return null;
        }

        /* return directory */
        return dir;
        
    }
    
    // ------------------------------------------------------------------------

    /* print all defined system properties */
    private static void printSystemProperties()
    {
        Properties props = System.getProperties();
        for (Enumeration n = props.propertyNames(); n.hasMoreElements();) {
            String key = (String)n.nextElement();
            String val = props.getProperty(key);
            println(key + " ==> " + val);
        }
    }

    // ------------------------------------------------------------------------

    /* return true if class is a proprietary GTS class */
    private static boolean isGtsClass(String className)
    {
        if (className.startsWith("org.opengts.rule.")) {
            return true; // possible
        } else
        if (className.startsWith("org.opengts.opt.")) {
            return true; // possible
        } else
        if (className.startsWith("org.opengts.priv.")) {
            return true; // unlikely
        } else {
            return false;
        }
    }

    private static String ClassName(Object clazz)
    {
        return ClassName(StringTools.className(clazz));
    }

    private static String ClassName(String className)
    {
        if (isGtsClass(className)) {
            return "GTS:" + className;
        } else {
            return className;
        }
    }

    // ------------------------------------------------------------------------

    private static File getLikelyWindowsJDK(File path)
    {
        
        /* not Windows? */
        if (!OSTools.isWindows()) {
            return null;
        }
        
        /* get search directory */
        File dir = null;
        if (path != null) {
            dir = path;
        } else
        if (FileTools.isDirectory("C:/Program Files/Java")) {
            dir = new File("C:/Program Files/Java");
        } else
        if (FileTools.isDirectory("C:/Program Files (x86)/Java")) {
            dir = new File("C:/Program Files (x86)/Java");
        } else {
            // Java directory not found
            return null;
        }
        
        /* search subdirectories */
        String fileList[] = ListTools.sort(dir.list()); // ie. jdk1.6.0_14
        if (!ListTools.isEmpty(fileList)) {
            File jdkDirPath = null;
            for (int i = 0; i < fileList.length;  i++) {
                if (fileList[i].startsWith("jdk")) {
                    jdkDirPath = new File(dir, fileList[i]);
                    //println(PFX+"Found JDK dir: '" + jdkDirPath + "'");
                }
            }
            return jdkDirPath;
        }
        
        /* "jdk*" not found */
        return null;

    }

    // ------------------------------------------------------------------------

    private static final String LS_FILE_PFX     = "  ";
    private static final String LS_ERROR_PFX    = "    ==> ERROR: ";

    private static void validateLocalStrings(File dir)
    {
        Print.sysPrintln("Verifying 'LocalStrings_XX.properties' files ...");
        if (dir == null) {
            Print.sysPrintln(LS_ERROR_PFX + "Specified file/directory does not exist: null");
        } else
        if (dir.isFile()) {
            int count = _validateLocalStrings(new File[] { dir }, null);
            if (count <= 0) {
                Print.sysPrintln(LS_ERROR_PFX + "Not a 'LocalStrings_XX.properties' file");
            }
       } else
        if (dir.isDirectory()) {
            Print.sysPrintln("Directory: " + dir);
            int count = _validateLocalStrings(new File[] { dir }, null);
            if (count <= 0) {
                Print.sysPrintln(LS_ERROR_PFX + "No LocalStrings files found");
            }
        } else {
            Print.sysPrintln(LS_ERROR_PFX + "File/Directory does not exist: " + dir);
        }
    }

    private static int _validateLocalStrings(File files[], java.util.List<File> badPropFiles)
    {
        int count = 0;
        boolean verbose = (badPropFiles == null);

        /* look for LocalStrings_XX.properties in list */
        for (int i = 0; i < files.length; i++) {
            if ((files[i] == null) || !files[i].isFile()) { continue; }
            String n = files[i].getName();
            if (n.startsWith("LocalStrings_") && n.endsWith(".properties")) {
                count++;
                if (verbose) {
                    Print.sysPrintln(LS_FILE_PFX + files[i] + " ...");
                }
                // check for invalid unicode-escaped chars
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(files[i]);
                    Properties props = new Properties();
                    props.load(fis); // "ISO-8859-1" only
                } catch (Throwable th) {
                    if (badPropFiles != null) {
                        badPropFiles.add(files[i]);
                    } else
                    if (verbose) {
                        Print.sysPrintln(LS_ERROR_PFX + th.getMessage());
                    }
                } finally {
                    try { fis.close(); } catch (Throwable th) {/*ignore*/}
                }
                // check for invalid chars
                try {
                    byte data[] = FileTools.readFile(files[i]);
                    if (data != null) {
                        int line = 1;
                        StringBuffer lineBuff = new StringBuffer();
                        boolean badChar = false;
                        for (int b = 0; b < data.length; b++) {
                            int ch = (int)data[b] & 0xFF;
                            if (ch == '\n') {
                                if (badChar) {
                                    // display error at end of line
                                    Print.sysPrintln(LS_ERROR_PFX + "Invalid characters at line #" + line);
                                }
                                badChar = false;
                                lineBuff.setLength(0);
                                line++;
                            } else
                            if (ch == '\r') {
                                // allowed space characters
                            } else
                            if (ch == '\t') {
                                // allowed space characters
                                lineBuff.append((char)ch);
                            } else
                            if ((ch >= ' ') && (ch <= '~')) {
                                // allowed ascii characters
                                lineBuff.append((char)ch);
                            } else {
                                // invalid chars
                                badChar = true;
                            }
                        }
                    }
                } catch (Throwable th) {
                    if (badPropFiles != null) {
                        badPropFiles.add(files[i]);
                    } else
                    if (verbose) {
                        Print.sysPrintln(LS_ERROR_PFX + th.getMessage());
                    }
                } finally {
                    try { fis.close(); } catch (Throwable th) {/*ignore*/}
                }
            }
        }

        /* drop into subdirectories */
        for (int i = 0; i < files.length; i++) {
            if ((files[i] == null) || !files[i].isDirectory()) { continue; }
            File subFiles[] = ListTools.sort(files[i].listFiles(),null);
            count += _validateLocalStrings(subFiles, badPropFiles);
        }

        /* return number of LocalStrings_XX.properties files found */
        return count;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Command-Line Options:
    //  -localStrings=<dir>
    //      Recursively descends through the specified directory validating all 
    //      "LocalStrings_XX.properties" files found.  Useful when creating or
    //      modifying your own localized language files.
    //  -sendTestEmailTo=<addr>
    //      If specified, this will indicate to CheckInstall that it should also
    //      attempt to send a test email to the specified email address.
    //  -sendTestSmsTo=<phone#>
    //      If specified, this will indicate to CheckInstall that it should also
    //      attempt to send a test SMS message to the specified phone number.

    private static final String ARG_HELP[]              = new String[] { "help"                                     };
    private static final String ARG_LOCAL_STRINGS[]     = new String[] { "localStrings"   , "ls"                    };
    private static final String ARG_SEND_TEST_EMAIL[]   = new String[] { "sendTestEmailTo", "sendMail", "sendEmail" };
    private static final String ARG_EMAIL_LOAD_TEST[]   = new String[] { "sendEmailLoadTestCount"                   };
    private static final String ARG_SMTP_PROPERTIES[]   = new String[] { "smtpProperties" , "smtp"                  };
    private static final String ARG_SEND_TEST_SMS[]     = new String[] { "sendTestSmsTo"  , "sendSMS"               };
    private static final String ARG_SHOW_REPORT_LIMIT[] = new String[] { "showReportLimit", "showReportLimits"      };
    private static final String ARG_SHOW_DB_ERRORS[]    = new String[] { "showDBErrors"   , "dbErrors"              };

    /* usage */
    private static void usage()
    {
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -help                        This help");
        Print.sysPrintln("");
        Print.sysPrintln("  -localStrings                Validate LocalStrings");
        Print.sysPrintln("");
        Print.sysPrintln("  -sendMail=<EMailAddr>        Send test email to <EMailAddr>");
        Print.sysPrintln("  -smtp=<PrivateLabelName>     Use SMTP properties from specified PrivateLabel name");
        Print.sysPrintln("  -sendSMS=<Phone#>            Send test SMS to <Phone#>");
        Print.sysPrintln("  -showReportLimits            Show Report limits");
        Print.sysPrintln("  -showDBErrors                Show DB table validation errors");
        System.exit(1);
    }

    /* main entry point */
    public static void main(String argv[])
    {
        RTConfig.setWebApp(true);
        BasicPrivateLabelLoader.setTrackServlet_debugOnly();
        DBConfig.cmdLineInit(argv, true);
        Print.setLogLevel(Print.LOG_WARN, false/*inclDate*/, false/*inclFrame*/);
        boolean isEnterprise = DBConfig.hasExtraPackage();
        boolean isWindows = OSTools.isWindows();
        StringBuffer recommendations = new StringBuffer();
        File configDir = RTConfig.getLoadedConfigDir();

        /* help */
        if (RTConfig.getBoolean(ARG_HELP,false)) {
            CheckInstall.usage();
            System.exit(1);
        }

        /* special check for 'LocalStrings_XX.properties' validation */
        if (RTConfig.hasProperty(ARG_LOCAL_STRINGS)) {
            File dir = RTConfig.getFile(ARG_LOCAL_STRINGS,null);
            validateLocalStrings(dir);
            System.exit(0);
        }

        /* check for showing report limits */
        SHOW_REPORT_LIMITS = RTConfig.getBoolean(ARG_SHOW_REPORT_LIMIT, SHOW_REPORT_LIMITS);

        /* check for showing report limits */
        SHOW_DB_ERRORS = RTConfig.getBoolean(ARG_SHOW_DB_ERRORS, SHOW_DB_ERRORS);

        /* check for sending a test email message */
        String sendTestEmailTo = RTConfig.getString(ARG_SEND_TEST_EMAIL, null);
        String smtpPropBPL     = RTConfig.getString(ARG_SMTP_PROPERTIES, null);

        /* check for sending a test SMS message */
        String sendTestSMSTo   = RTConfig.getString(ARG_SEND_TEST_SMS, null);

        /* environment vars */
        File env_GTS_LINKS     = null;  // $GTS_LINKS
        File env_GTS_HOME      = null;  // $GTS_HOME
        File env_GTS_CONF      = null;  // $GTS_CONF
        File env_JAVA_HOME     = null;  // $JAVA_HOME
        File env_ANT_HOME      = null;  // $ANT_HOME
        File env_CATALINA_HOME = null;  // $CATALINA_HOME

        /* clear errors */
        clearErrors();

        /* track.war build time (last modified time) */
        File trackWar_file = new File(configDir,"build/track.war");
        long trackWar_lastModMS = trackWar_file.lastModified(); // '0' if does not exist

        /* begin */
        println("");
        int sepWidth = WRAP_WIDTH;
        String eqSep = StringTools.replicateString("=",sepWidth);

        /* print all system properties? */
        if (RTConfig.hasProperty("props")) {
            printSystemProperties();
            System.exit(0);
        }

        /* separator */
        println(eqSep);

        /* ServiceAccount ID/Name */
        if (RTConfig.hasProperty(DBConfig.PROP_ServiceAccount_ID)) {
            String srvID   = DBConfig.getServiceAccountID("?");
            String srvName = DBConfig.getServiceAccountName("?");
            println("Service Account: [" + srvID + "] " + srvName);
            println(eqSep);
        }

        /* Java vendor/version */
        println("");
        println(isEnterprise? "GTS Enterprise:" : "OpenGTS:");
        {
            String trackWar_build = (trackWar_lastModMS > 0L)?
                (new DateTime(trackWar_lastModMS/1000L)).toString() : " n/a";
            // Version
            printVariable("(Version)", DBConfig.getVersion(), (isEnterprise?"(enterprise)":""));
            // Compiletime
            printVariable("(Compiled Time)", (new DateTime(CompileTime.COMPILE_TIMESTAMP)).toString(), "");
            // Compiletime
            printVariable("('track.war' Build)", trackWar_build, "");
            // Current time
            printVariable("(Current Time)", (new DateTime()).toString(), "");
            // Current user
            String userName = System.getProperty("user.name","?");
            if (userName.equalsIgnoreCase("root")) {
                printVariable("(Current User)", userName, "(ERROR: should not be 'root')");
                addError("This application is being run as superuser 'root'.",
                         "This application should be run under a user other than 'root'.",
                         "Change to a different user when running GTS/OpenGTS.");
            } else {
                printVariable("(Current User)", userName, "");
            }
            // ServiceAccount.ID
            String saIDKey = DBConfig.PROP_ServiceAccount_ID;
            printVariable(saIDKey, RTConfig.getString(saIDKey,"?"), "");
            // ServiceAccount.Name
            String saNameKey = DBConfig.PROP_ServiceAccount_Name;
            printVariable(saNameKey, RTConfig.getString(saNameKey,"?"), "");
            // ServiceAccount.Type
            String saTypeKey = DBConfig.PROP_ServiceAccount_Attr;
            if (RTConfig.hasProperty(saTypeKey)) {
            printVariable(saTypeKey, RTConfig.getString(saTypeKey,"?"), "");
            }
            // ServiceAccount.Key
            String saKeyKey = DBConfig.PROP_ServiceAccount_Key;
            if (RTConfig.hasProperty(saKeyKey)) {
            printVariable(saKeyKey, RTConfig.getString(saKeyKey,"?"), "");
            }
        }

        /* System info */
        println("");
        println("System Information:");
        {
            // hostname
            try {
                String hostName = InetAddress.getLocalHost().getHostName();
                InetAddress hostIP = InetAddress.getByName(hostName);
                String hostIPStr = (hostIP != null)? hostIP.toString() : "";
                int h = hostIPStr.indexOf("/");
                if (h >= 0) { hostIPStr = hostIPStr.substring(h+1).trim(); }
                printVariable("(Hostname)", hostName, "[" + hostIPStr + "]");
            } catch (UnknownHostException uhe) {
                String hostName = uhe.getMessage().trim();
                int h = hostName.lastIndexOf(":");
                if (h >= 0) { hostName = hostName.substring(h+1).trim(); }
                printVariable("(Hostname)", "("+hostName+")", "(ERROR: unable to resolve local host name)");
                addError("Unable to resolve local host name '"+hostName+"'.",
                         "Specified host name may be missing from the '/etc/hosts' file.",
                         "Add '"+hostName+"' to localhost entries in '/etc/hosts' file.");
            }
            // os.arch
            String osArchKey = "os.arch";
            printVariable(osArchKey, System.getProperty(osArchKey,"?"), "");
            // os.name
            String osNameKey = "os.name";
            printVariable(osNameKey, System.getProperty(osNameKey,"?"), "");
            // os.version
            String osVersKey = "os.version";
            printVariable(osVersKey, System.getProperty(osVersKey,"?"), "");
            // "/etc/issue"
            //   Fedora release 12 (Constantine)
            //   Kernel \r on an \m (\l)
            File issueFile = new File("/etc/issue");
            if (issueFile.isFile()) {
                String issue = StringTools.toStringValue(FileTools.readFile(issueFile));
                String I[]   = StringTools.parseStringArray(issue,"\r\n"); 
                printVariable(issueFile.toString(), I[0], "");
            } else {
                //printVariable(issueFile.toString(), "(not present)", "");
            }
            // "/usr/bin/free | grep Mem:"
            /*
            long memMeg = 0L;
            try {
                File linuxFreeCmd = new File("/usr/bin/free");
                if (linuxFreeCmd.isFile()) {
                    Process ppidExec = Runtime.getRuntime().exec("/usr/bin/free -m | grep Mem:");
                    BufferedReader ppidReader = new BufferedReader(new InputStreamReader(ppidExec.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    for (;;) {
                        String line = ppidReader.readLine();
                        if (line == null) { break; }
                        sb.append(StringTools.trim(line));
                    }
                    String M[] = StringTools.parseStringArray(sb.toString()," \r\n"); 
                    memMeg = StringTools.parseLong(M[1],0L);
                    int exitVal = ppidExec.waitFor();
                    ppidReader.close();
                }
            } catch (Throwable th) {
                memMeg = -1L;
            }
            if (memMeg > 0L) {
                printVariable("Total Memory", StringTools.format((double)memMeg,"0.0")+" mb", "");
            } else
            if (memMeg < 0L) {
                printVariable("Total Memory", "(unable to obtain)", "");
            } else {
                printVariable("Total Memory", "(not available)", "");
            }
            */
            // Disk/Memory usage
            OSTools.MemoryUsage sysMem = OSTools.getSystemMemoryUsage(null);
            OSTools.DiskUsage  sysDisk = OSTools.getSystemDiskUsage(configDir,null);
            if (sysMem != null) {
                String  fmt    = "0"; // "0.0"
                int     digLen = sysMem.getTotalFieldLength(OSTools.M_BYTES, sysDisk) + fmt.length() - 1;
                //Print.sysPrintln("MemoryUsage digLen: " + digLen);
                double  _tot   = sysMem.getTotal_Mb();
                double  _used  = sysMem.getUsed_Mb();
                double  _free  = sysMem.getFree_Mb();
                double  _usage = sysMem.getUsage();
                String  totMb  = StringTools.format(_tot ,fmt);
                String  useMb  = StringTools.format(_used,fmt);
                String  freMb  = StringTools.format(_free,fmt);
                String  usage  = String.valueOf(Math.round(_usage*100.0)) + "%";
                totMb = StringTools.rightAlign(totMb,digLen);
                useMb = StringTools.rightAlign(useMb,digLen);
                freMb = StringTools.rightAlign(freMb,digLen);
                String  memStr = "Total:"+totMb+"  Used:"+useMb+" ["+usage+"]"+"  Free:"+freMb;
                if (_tot < (double)(RECOMMENDED_MEMORY_MB - 1L)) {
                    int WC = countWarning("Memory below recommended value");
                    //printVariable("(Total Memory)", totMb + " Mb", "(WARNING["+WC+"]: below recommended value)");
                    //printVariable("(Used Memory)" , useMb + " Mb", "[" + usage + "]");
                    //printVariable("(Free Memory)" , freMb + " Mb", "");
                    printVariable("(Memory Usage Mb)", memStr, "(WARNING["+WC+"]: below recommended value)");
                    recommendations.append("- Highly recommend increasing memory to at least "+RECOMMENDED_MEMORY_MB+" Mb for a production environment.\n");
                } else {
                    //printVariable("(Total Memory)", totMb + " Mb", "");
                    //printVariable("(Used Memory)" , useMb + " Mb", "[" + usage + "]");
                    //printVariable("(Free Memory)" , freMb + " Mb", "");
                    printVariable("(Memory Usage Mb)", memStr, "");
                }
            } else {
                printVariable("(Memory Usage)", "(not available)", "");
            }
            // Disk usage
            if (sysDisk != null) {
                String  fmt    = "0"; // "0.0"
                int     digLen = sysDisk.getTotalFieldLength(OSTools.M_BYTES) + fmt.length() - 1;
                double  _tot   = sysDisk.getTotal_Mb();
                double  _used  = sysDisk.getUsed_Mb();
                double  _free  = sysDisk.getFree_Mb();
                double  _usage = sysDisk.getUsage();
                String  totMb  = StringTools.format(_tot ,fmt);
                String  useMb  = StringTools.format(_used,fmt);
                String  freMb  = StringTools.format(_free,fmt);
                String  usage  = String.valueOf(Math.round(_usage*100.0)) + "%";
                totMb = StringTools.rightAlign(totMb,digLen);
                useMb = StringTools.rightAlign(useMb,digLen);
                freMb = StringTools.rightAlign(freMb,digLen);
                String  memStr = "Total:"+totMb+"  Used:"+useMb+" ["+usage+"]"+"  Free:"+freMb;
                if (_usage > THRESHOLD_DISK_UTIL) {
                    int WC = countWarning("Disk utilization above recommended threshold");
                    printVariable("(Disk Usage Mb)", memStr, "(WARNING["+WC+"]: utilization above recommended threshold)");
                    recommendations.append("- Highly recommend increasing available disk space.\n");
                } else {
                    printVariable("(Disk Usage Mb)", memStr, "");
                }
            } else {
                printVariable("(Disk Usage)", "(not available)", "");
            }
        }

        /* Java vendor/version */
        boolean isJava6plus = false;
        File javaInstallDir = null;
        File mostLikelyWinJDK = null;
        println("");
        println("Java Version (the JRE running this program):");
        {
            // Check Java vendor
            String javaVendKey = "java.vendor";
            String javaVendVal = System.getProperty(javaVendKey);                  // "Sun Microsystems Inc."
            if ((javaVendVal == null) || 
	            ((javaVendVal.indexOf("Sun Microsystems") < 0) && 
	             (javaVendVal.indexOf("Oracle")           < 0) && 
	             (javaVendVal.indexOf("Apple")            < 0)    )) {
                // On the Mac (OS X), this String may be "Apple Inc.", which appears to work fine.
                printVariable("(Vendor)", javaVendVal, "(ERROR: not a Sun Microsystems version!)");
                addError("This is not a 'Sun Microsystems, Inc' version of Java.",
                         "Sun Microsystems Java not installed, or not referenced in executable path",
                         FIX_JAVA_VERSION);
            } else {
                printVariable("(Vendor)", javaVendVal, "");
            }
            // Display Java version
            //String javaVersKey = "java.version";
            //String javaVersVal = System.getProperty(javaVersKey);    // "1.5.0_06"
            //printVariable(javaVersKey, javaVersVal, "");
            // Check specification version
            String javaSpecKey = "java.specification.version";
            String javaSpecVal = StringTools.trim(System.getProperty(javaSpecKey)); // "1.6" / "1.7"
            if (javaSpecVal.startsWith("1.5")) {
                printVariable("(Version)", javaSpecVal, "(ERROR: requires 1.6+ to run properly)");
                addError("This Java version may no longer be supported ("+javaSpecVal+").",
                         "Supported version of Java is not installed, or is not referenced in executable path",
                         FIX_JAVA_VERSION);
            } else
            if (javaSpecVal.startsWith("1.6")) {
                printVariable("(Version)", javaSpecVal, ""); // recommended version
                isJava6plus = true;
            } else
            if (javaSpecVal.startsWith("1.7")) {
                //int WC = countWarning("Not fully tested with Java 1.7");
                //printVariable("(Version)", javaSpecVal, "(WARNING["+WC+"]: not yet fully tested with 1.7)");
                printVariable("(Version)", javaSpecVal, "");
                isJava6plus = true;
            } else
            if (javaSpecVal.startsWith("1.8")) {
                int WC = countWarning("Not fully tested with Java 1.8");
                printVariable("(Version)", javaSpecVal, "(WARNING["+WC+"]: not yet fully tested with 1.8)");
                isJava6plus = true;
            } else {
                printVariable("(Version)", javaSpecVal, "(ERROR: invalid version)");
                addError("This Java version is not supported ("+javaSpecVal+").",
                         "Supported version of Java is not installed, or is not referenced in executable path",
                         FIX_JAVA_VERSION);
            }
            // Check installation directory (System property "java.home")
            String javaHomeKey = "java.home";
            String javaHomeVal = System.getProperty(javaHomeKey,"");
            try {
                File javaHomeDir = !javaHomeVal.equals("")? (new File(javaHomeVal)).getCanonicalFile() : null;
                if (javaHomeDir != null) {
                    javaInstallDir = javaHomeDir.getName().equals("jre")? javaHomeDir.getParentFile() : javaHomeDir;
                    String javaInstallDirStr = javaInstallDir.toString(); // + "jre"; // <-- testing
                    boolean isJavaPathJRE = (StringTools.indexOfIgnoreCase(javaInstallDirStr, "jre") >= 0);
                    if (isJavaPathJRE) {
                        printVariable("(Install dir)", javaInstallDir, "(ERROR: 'PATH' points to the JRE, rather than the JDK)");
                        //String envPATH = StringTools.blankDefault(System.getenv(ENVIRON_PATH),"?");
                        //wrapPrintln(PFX+ENVIRON_PATH+"="+envPATH, File.pathSeparatorChar);
                        if (isWindows) {
                            mostLikelyWinJDK = getLikelyWindowsJDK(javaInstallDir.getParentFile());
                            if (mostLikelyWinJDK != null) {
                                String JavaHome = System.getenv(ENVIRON_JAVA_HOME);
                                if  ((JavaHome != null) && JavaHome.equals(mostLikelyWinJDK.toString())) {
                                    println(PFX+"('PATH' should be prefixed with '%JAVA_HOME%\\bin')");
                                } else {
                                    println(PFX+"('PATH' should likely be prefixed with '" + mostLikelyWinJDK + "\\bin')");
                                }
                            }
                        }
                        addError("The 'PATH' environment variable points to the JRE, rather than the JDK.",
                                 "The 'PATH' environment variable points to the JRE (Java Runtime Environment), rather than " + 
                                 "the JDK (Java Developer Kit).  The JDK already contains the JRE, so a separate JRE insallation " +
                                 " is not necessary.",
                                 "Set the 'PATH' environment variable to point to the JDK installation bin directory.");
                    } else {
                        printVariable("(Install dir)", javaInstallDir.toString(), "");
                    }
                } else {
                    javaInstallDir = null;
                }
            } catch (IOException ioe) {
                javaInstallDir = null;
            }
            if (javaInstallDir == null) {
                printVariable(javaHomeKey, javaHomeVal, "(ERROR: unable to determine Java installation dir)");
                addError("Unable to resolve the Java installation directory from '"+javaHomeVal+"'.",
                         "Error encountered while attempting to determine the Java installation directory",
                         null);
            }
            // Check java.awt.headless
            String javaHeadKey = "java.awt.headless";
            String javaHeadVal = System.getProperty(javaHeadKey,"false");
            printVariable(javaHeadKey, javaHeadVal, "");
            // Font check
            try {
                Font font = new Font(PushpinIcon.DEFAULT_TEXT_FONT, Font.PLAIN, 10);
                printVariable("(Has Fonts)", "true", "");
            } catch (Throwable th) {
                int WC = countWarning("Unable to load Fonts");
                printVariable("(Has Fonts)", "false", "(WARNING["+WC+"]: unable to load fonts)");
            }
            // Hash Algorithms  : (Hash Algorithms)  : MD5,SHA1,[SHA256]
            StringBuffer hashSB = new StringBuffer();
            try { // MD5
                java.security.MessageDigest.getInstance("MD5");
                hashSB.append("MD5");
            } catch (java.security.NoSuchAlgorithmException nsae) {
                hashSB.append("[MD5]");
            }
            hashSB.append(", ");
            try { // SHA-1
                java.security.MessageDigest.getInstance("SHA-1");
                hashSB.append("SHA-1");
            } catch (java.security.NoSuchAlgorithmException nsae) {
                hashSB.append("[SHA-1]");
            }
            hashSB.append(", ");
            try { // SHA-256
                javax.crypto.Mac.getInstance("HmacSHA256");
                hashSB.append("SHA-2");
            } catch (java.security.NoSuchAlgorithmException nsae) {
                hashSB.append("[SHA-2]");
            }
            printVariable("(Hash Algorithms)", hashSB.toString(), "");
        }

        /* environment directories */
        println("");
        println("Environment variable paths (canonical):");
        {
            // GTS_HOME
            // -- GTS installation directory
            env_GTS_HOME = getEnvironmentFile(ENVIRON_GTS_HOME, true, true);
            if (env_GTS_HOME != null) {
                String userDirPath = System.getProperty("user.dir","");
                try {
                    File userDir = !userDirPath.equals("")? (new File(userDirPath)).getCanonicalFile() : null;
                    if (!env_GTS_HOME.equals(userDir)) {
                        printVariable(ENVIRON_GTS_HOME, env_GTS_HOME, "(ERROR: does not match the current directory)");
                        addError("'GTS_HOME' does not match the current directory '"+userDir+"'.",
                                 "This installation check must be executed from directory '"+env_GTS_HOME+"'",
                                 "Change the environment variable 'GTS_HOME', or cd to '"+env_GTS_HOME+"'.");
                    } else {
                        printVariable(ENVIRON_GTS_HOME, env_GTS_HOME, "");
                    }
                } catch (IOException ioe) {
                    printVariable(ENVIRON_GTS_HOME, env_GTS_HOME, "(ERROR: unable to determine current directory)");
                    addError("Unable to resolve the current directory from '"+userDirPath+"'.",
                             "Error encountered while attempting to determine current directory",
                             null);
                }
            }
            // GTS_LINKS
            // -- Location of pre-requisite package symbolic links (ie. "gts", "java", "tomcat")
            env_GTS_LINKS = getEnvironmentFile(ENVIRON_GTS_LINKS, true, false);
            if (env_GTS_LINKS == null) {
                if (!OSTools.isWindows()) {
                    // non-Windows: default to "/usr/local/"
                    env_GTS_LINKS = new File("/usr/local");
                } else
                if (env_GTS_HOME != null) {
                    // Windows: default to GTS_HOME parent dir
                    //env_GTS_LINKS = env_GTS_HOME.getParentFile();
                }
            }
            // GTS_CONF
            // -- Location of initially loaded "default.conf" (defaults to "$GTS_HOME/default.conf")
            env_GTS_CONF = getEnvironmentFile(ENVIRON_GTS_CONF, false, false);
            if (env_GTS_CONF != null) {
                // TODO: check to make sure that 'env_GTS_HOME' is the parent of 'env_GTS_CONF'
                printVariable(ENVIRON_GTS_CONF, env_GTS_CONF, "");
            }
            // JAVA_HOME
            // -- Location of JDK installation
            env_JAVA_HOME = getEnvironmentFile(ENVIRON_JAVA_HOME, true, true);        // "/opt/sun-jdk-1.5.0.06"
            if (env_JAVA_HOME != null) {
                String env_JAVA_HOME_name = env_JAVA_HOME.getName();
                boolean isJavaEnvJRE = (StringTools.indexOfIgnoreCase(env_JAVA_HOME_name, "jre") >= 0);
                if (isJavaEnvJRE) {
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "(ERROR: points to the JRE, rather than the JDK)");
                    if (isWindows) {
                        File likelyJDK = (mostLikelyWinJDK != null)? mostLikelyWinJDK : getLikelyWindowsJDK(null);
                        if (likelyJDK != null) {
                            println(PFX+"('JAVA_HOME' should likely be set to '" + likelyJDK + "')");
                        }
                    }
                    addError("'JAVA_HOME' points to the JRE, rather than the JDK.",
                             "The 'JAVA_HOME' environment variable points to the JRE (Java Runtime Environment), rather than " + 
                             "the JDK (Java Developer Kit).  The JDK already contains the JRE, so a separate JRE insallation " +
                             " is not necessary.",
                             "Set JAVA_HOME to point to the JDK installation directory.");
                } else
                if (javaInstallDir == null) {
                    int WC = countWarning("Cannot compare JAVA_HOME to Java Install directory");
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "(WARNING["+WC+"]: could not compare to Java install dir)");
                } else
                if (!javaInstallDir.equals(env_JAVA_HOME)) {
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "(ERROR: does not match Java install dir)");
                    addError("'JAVA_HOME' does not match the Java installation 'PATH' directory '"+javaInstallDir+"'.",
                             "The version of Java referenced in the executable 'PATH' environment variable does not match 'JAVA_HOME'.",
                             "Make sure both the 'JAVA_HOME' and 'PATH' environment variables point to the same installed JDK.");
                } else {
                    printVariable(ENVIRON_JAVA_HOME, env_JAVA_HOME, "");
                }
            } else {
                if (isWindows) {
                    File likelyJDK = (mostLikelyWinJDK != null)? mostLikelyWinJDK : getLikelyWindowsJDK(null);
                    if (likelyJDK != null) {
                        println(PFX+"('JAVA_HOME' should likely be set to '" + likelyJDK + "')");
                    }
                }
            }
            // ANT_HOME
            // -- Location of Apache Ant
            env_ANT_HOME = getEnvironmentFile(ENVIRON_ANT_HOME, true, false);
            if (env_ANT_HOME != null) {
                printVariable(ENVIRON_ANT_HOME, env_ANT_HOME, "");
            }
            // CATALINA_HOME
            // -- Location of Apache Tomcat
            env_CATALINA_HOME = getEnvironmentFile(ENVIRON_CATALINA_HOME, true, true);    // "/opt/jakarta-tomcat-5.0.28"
            if (env_CATALINA_HOME == null) {
                // error already printed
                //printVariable(ENVIRON_CATALINA_HOME, "", "(Note: not defined)");
            } else
            if (!env_CATALINA_HOME.isDirectory()) {
                int WC = countWarning("'CATALINA_HOME' does not specify a directory");
                printVariable(ENVIRON_CATALINA_HOME, env_CATALINA_HOME, "(WARNING["+WC+"]: does not exist)");
                env_CATALINA_HOME = null;
            } else {
                printVariable(ENVIRON_CATALINA_HOME, env_CATALINA_HOME, "");
                // check for non-executable files in "$CATALINA_HOME/bin"
                if (!isWindows && isJava6plus) {
                    String ext = isWindows? ".bat" : ".sh";
                    String sh[] = new String[] { "startup", "shutdown", "catalina" };
                    File tomcatBin = new File(env_CATALINA_HOME, "bin");
                    int notExecutableCount = 0;
                    /* (Java 6+) not supported on Java 5 */
                    try {
                        for (int i = 0; i < sh.length; i++) {
                            File shFile = new File(tomcatBin, sh[i]+ext);
                            MethodAction canExecMeth = new MethodAction(shFile, "canExecute");
                            boolean canExec = ((Boolean)canExecMeth.invoke()).booleanValue();
                            if (!canExec) {
                                notExecutableCount++;
                                break;
                            }
                        }
                    } catch (Throwable th) { // NoSuchMethodException
                        int WC = countWarning("Unable to check for exectuable Tomcat scripts");
                        println(PFX+"WARNING["+WC+"]: Unable to check for executable Tomcat scripts: " + th);
                    }
                    if (notExecutableCount > 0) {
                        println(PFX+"ERROR: Tomcat '$CATALINA_HOME/bin' directory contains non-executable '"+ext+"' files!");
                        addError("Tomcat contains non-executable '"+ext+"' files",
                                 "Some Tomcat '"+ext+"' commands do not have the 'execute' permission bit set.",
                                 "Run 'chmod a+x $CATALINA_HOME"+File.separator+"bin"+File.separator+"*"+ext+"' to set the execute bit");
                    }
                }
                // check for non-readable files in "$CATALINA_HOME/"
                if (!isWindows && isJava6plus) {
                    /* (Java 6+) not supported on Java 5 */
                    final AccumulatorLong accumCantRead = new AccumulatorLong(0L);
                    final long maxListedFiles = 4L;
                    try {
                        FileTools.traverseAllFiles(env_CATALINA_HOME, new FileFilter() {
                            public boolean accept(File f) {
                                if (!FileTools.canRead(f)) {
                                    accumCantRead.increment();
                                    if (accumCantRead.get() < maxListedFiles) {
                                        println(PFX+"ERROR: Cannot read - " + f);
                                    } else 
                                    if (accumCantRead.get() == maxListedFiles) {
                                        println(PFX+"ERROR: ... (additional non-readable files omitted) ...");
                                    }
                                } else {
                                    // OK
                                }
                                return true;
                            }
                        });
                    } catch (Throwable th) { // NoSuchMethodException
                        Print.logException("Readable files error", th);
                        int WC = countWarning("Unable to check for (non-)readable Tomcat files");
                        println(PFX+"WARNING["+WC+"]: Unable to check for (non-)readable Tomcat files: " + th);
                    }
                    if (accumCantRead.get() > 0L) {
                        println(PFX+"ERROR: Tomcat '$CATALINA_HOME/' directory contains non-readable files!");
                        addError("Tomcat contains non-readable files",
                                 "Some Tomcat files do not have the 'read' permission bit set or are owned by a different user.",
                                 "Run 'chmod' to set the read bit, or 'chown' to change to the proper user.");
                    }
                }
                // check for "$CATALINA_HOME/[common/]lib/servlet-api.jar" file
                File servletApiJarFile1 = new File(new File(new File(env_CATALINA_HOME,"common"),"lib"),"servlet-api.jar");
                boolean foundServletApiJar1 = servletApiJarFile1.isFile();
                if (!foundServletApiJar1) {
                    File servletApiJarFile2 = new File(new File(env_CATALINA_HOME,"lib"),"servlet-api.jar");
                    boolean foundServletApiJar2 = servletApiJarFile2.isFile();
                    if (!foundServletApiJar2) {
                        String saj = (isWindows? "%CATALINA_HOME%\\common\\lib\\" : "$CATALINA_HOME/common/lib/") + servletApiJarFile1.getName();
                        println(PFX+"ERROR: Tomcat '"+saj+"' file not found!");
                        addError("Tomcat '"+saj+"' file not found",
                                 "CATALINA_HOME is likely pointing to an invalid Tomcat installation",
                                 "Check directory referenced by CATALINA_HOME");
                    }
                }
            }
            // MYSQL_HOME
            // -- Location of MySQL
            //File envMysqlHome  = getEnvironmentFile(ENVIRON_MYSQL_HOME, true, false);
            //if (envMysqlHome != null) {
            //    printVariable(ENVIRON_MYSQL_HOME, envMysqlHome, "");
            //}
        }

        /* "$JAVA_HOME/jre/lib/ext" jars */
        println("");
        println("Extended library Jar files: 'java.ext.dirs'");
        String javaExtDirs[] = StringTools.split(System.getProperty("java.ext.dirs",""),File.pathSeparatorChar);
        if ((javaExtDirs == null) || (javaExtDirs.length == 0)) {
            println(PFX+"ERROR: System property 'java.ext.dirs' is null/empty!");
            addError("Extended library jar directory property 'java.ext.dirs' is null/empty.",
                     "'java.ext.dirs' is not defined",
                     null);
        } else {
            String reqJars[] = new String[] { 
                /*"activation.jar",*/       // no longer needed
                "*mail.jar",                // "mail.jar" or "javax.mail.jar"
                "mysql-connector-java-*" 
                };
            for (int xd = 0; xd < javaExtDirs.length; xd++) {
                File prpExtLibHome = null;
                String fileList[] = null;
                try {
                    prpExtLibHome = (new File(javaExtDirs[xd])).getCanonicalFile();
                    fileList = prpExtLibHome.list();
                    if (fileList == null) { 
                        fileList = new String[0]; 
                    }
                } catch (IOException ioe) {
                    println(PFX+"ERROR: Unable to resolve extended library jar directory: " + javaExtDirs[xd]);
                    println(PFX+" [" + ioe.getMessage() + "]");
                    addError("Unable to resolve Java extended library directory.",
                             "Error resolving the System property 'java.ext.dirs' directory: "+javaExtDirs[xd],
                             null);
                    break;
                }
                printVariable("(Ext dir)", prpExtLibHome, "");
                for (int j = 0; j < reqJars.length; j++) {
                    if (reqJars[j] == null) { continue; }
                    String foundJarName = null;
                    for (int i = 0; i < fileList.length; i++) {
                        if (!StringTools.endsWithIgnoreCase(fileList[i],".jar")) { continue; }
                        if (reqJars[j].endsWith("*")) {
                            String pattern = reqJars[j].substring(0, reqJars[j].length() - 1); // remove trailing '*'
                            if (StringTools.startsWithIgnoreCase(fileList[i],pattern)) {
                                foundJarName = fileList[i];
                                break;
                            }
                        } else
                        if (reqJars[j].startsWith("*")) {
                            String pattern = reqJars[j].substring(1); // remove leading '*'
                            if (StringTools.endsWithIgnoreCase(fileList[i],pattern)) {
                                foundJarName = fileList[i];
                                break;
                            }
                        } else
                        if (fileList[i].equalsIgnoreCase(reqJars[j])) {
                            foundJarName = fileList[i];
                            break;
                        }
                    }
                    if (foundJarName != null) {
                        File foundJar = new File(prpExtLibHome, foundJarName);
                        if (foundJar.canRead()) {
                            printVariable(reqJars[j], "Found '" + foundJarName + "'", "");
                        } else {
                            printVariable(reqJars[j], "Found '" + foundJarName + "'", "(ERROR: not readable!)");
                            addError("Jar file '"+reqJars[j]+"' is not readable by this application.",
                                     "The jar file permissions may restrict the ability to read this file.",
                                     "Make sure this jar file permissions is set to world-readable.");
                        }
                        reqJars[j] = null;
                    }
                }
            }
            for (int j = 0; j < reqJars.length; j++) {
                if (reqJars[j] != null) {
                    printVariable(reqJars[j], "", "(ERROR: not found!)");
                    addError("Jar file '"+reqJars[j]+"' was not found.",
                             "The jar file is not installed in the extended library directory",
                             "Please install the jar file in the extended library directory");
                }
            }
        }

        /* Runtime configuration */
        println("");
        println("Runtime Configuration:");
        // 'default.conf'
        File defaultConfigFile = null;
        try { 
            defaultConfigFile = FileTools.toFile(RTConfig.getLoadedConfigURL()); 
        } catch (Throwable th) {
            int WC = countWarning("Error converting URL to File: " + RTConfig.getLoadedConfigURL());
            println(PFX+"WARNING["+WC+"]: Unable to convert URL to File: " + RTConfig.getLoadedConfigURL());
        }
        if (defaultConfigFile == null) {
            printVariable("(Default cfg dir)" , (configDir != null)? configDir.toString() : "(ERROR: not found!)", "");
            printVariable("(Default cfg file)", "", "(ERROR: not found!)");
            addError("Runtime configuration file not found.",
                     "Possible missing configuration file, or not found in CLASSPATH.",
                     "Please include configuration file directory in CLASSPATH.");
        } else
        if (configDir == null) {
            printVariable("(Default cfg dir)" , "", "(ERROR: not found!)");
            printVariable("(Default cfg file)", defaultConfigFile, "");
            addError("Runtime configuration directory not found.",
                     "Possible CLASSPATH and/or GTS_HOME configuration issue.",
                     "Please repair CLASSPATH and/or GTS_HOME configuration.");
        } else {
            printVariable("(Default cfg dir)" , configDir, "");
            printVariable("(Default cfg file)", defaultConfigFile, "");
        }
        // default properties
        File defaultFile = defaultConfigFile; // (configDir != null)? new File(configDir,"default.conf") : null;
        RTProperties defaultProps = (defaultFile != null)? new RTProperties(defaultFile) : null;
        // 'webapp.conf'
        File webappFile = (configDir != null)? new File(configDir,"webapp.conf") : null;
        RTProperties webappProps = null;
        if ((webappFile == null) || !webappFile.isFile()) {
            printVariable("(WebApp cfg URL)", "", "(ERROR: not found!)");
            addError("WebApp configuration file not found.",
                     "Possible missing configuration file, or not found in CLASSPATH.",
                     "Please include configuration file directory in CLASSPATH.");
        } else {
            //String webappURL = null;
            //try {
            //    webappURL = FileTools.toURL(webappFile).toString();
            //} catch (MalformedURLException mue) {
            //    webappURL = webappFile.toString();
            //}
            try { 
                webappProps = new RTProperties();
                webappProps.setKeyReplacementMode(RTProperties.KEY_REPLACEMENT_LOCAL);
                webappProps.setConfigLogMessagesEnabled(false);
                webappProps.setProperties(webappFile, true);
                printVariable("(WebApp cfg file)", webappFile, "");
            } catch (IOException ioe) {
                webappProps = null; // did not load
                Print.logError("Unable to load config file: " + webappFile + " [" + ioe + "]");
                printVariable("(WebApp cfg file)", webappFile, "(ERROR: unable to load!)");
                addError("Unable to load WebApp configuration file.",
                         "Possible invalid/unreadable configuration file.",
                         "Please check that configuration exists and is readable.");
            }
        }
        // Show currently loaded config files
        {
            // -- list loaded config files
            RTProperties cfgFileProps = RTConfig.getConfigFileProperties();
            java.util.List<URL> loadedURLs = (cfgFileProps != null)? cfgFileProps.getLoadedURLs() : null;
            int cnt = 1, WC = -1;
            java.util.List<String> allConfigFiles = ListTools.toList(CONFIG_FILES,new Vector<String>());
            printVariable("(Loaded Config Files)", "(last modified date)", "");
            // -- list loaded files
            if (!ListTools.isEmpty(loadedURLs)) {
                for (URL url : loadedURLs) {
                    String dispFile = url.toString();
                    String modWarn = "";
                    String modDate = "";
                    if (url.getProtocol().equalsIgnoreCase("file")) {
                        try {
                            File cfgFile = new File(url.toURI()); // may throw exception
                            dispFile = cfgFile.toString();
                            if ((configDir != null) && dispFile.startsWith(configDir.toString())) {
                                dispFile = dispFile.substring(configDir.toString().length());
                                if (dispFile.startsWith("/") || dispFile.startsWith("\\")) {
                                    dispFile = dispFile.substring(1);
                                }
                            }
                            long cfgLastModMS = cfgFile.lastModified();
                            DateTime modDT = new DateTime(cfgLastModMS/1000L);
                            modDate = modDT.shortFormat(null);
                            if ((trackWar_lastModMS > 0L) && (cfgLastModMS > trackWar_lastModMS)) {
                                if (WC < 0) {
                                    WC = countWarning("Config file found that was modified after the 'track.war' file was built.");
                                    recommendations.append("- Recommend rebuilding 'track.war' file.\n");
                                }
                                modWarn = "(WARNING["+WC+"]: Modified AFTER 'track.war' was built)";
                            }
                        } catch (Throwable th) {
                            dispFile = url.toString();
                        }
                    }
                    printVariable("  "+(cnt++)+") "+dispFile, modDate, modWarn);
                    allConfigFiles.remove(dispFile);
                }
            }
            // -- list remaining files
            if (!ListTools.isEmpty(allConfigFiles)) {
                for (String dispFile : allConfigFiles) {
                    File cfgFile = (configDir != null)? new File(configDir,dispFile) : new File(dispFile);
                    if (cfgFile.isFile()) { // exists
                        long cfgLastModMS = cfgFile.lastModified();
                        DateTime modDT = new DateTime(cfgLastModMS/1000L);
                        String modDate = modDT.shortFormat(null);
                        String modWarn = "";
                        if ((trackWar_lastModMS > 0L) && (cfgLastModMS > trackWar_lastModMS)) {
                            if (WC < 0) {
                                WC = countWarning("Config file found that was modified after the 'track.war' file was built.");
                                recommendations.append("- Recommend rebuilding 'track.war' file.\n");
                            }
                            modWarn = "(WARNING["+WC+"]: Modified AFTER 'track.war' was built)";
                        }
                        printVariable("  -) "+dispFile, modDate, modWarn);
                    }
                }
            }
        }
        // log directory
        {
            File logDir = RTConfig.getFile(RTKey.LOG_DIR,null);
            if ((logDir == null) || StringTools.isBlank(logDir.toString())) {
                printVariable(RTKey.LOG_DIR, "", "(ERROR: not specified!)");
                addError("The '"+RTKey.LOG_DIR+"' appears to be missing from the runtime configuration.",
                         "Missing '"+RTKey.LOG_DIR+"' specification in 'default.conf' (or included files).",
                         "Please include '"+RTKey.LOG_DIR+"' specification in 'default.conf' (or included files).");
            } else
            if (!logDir.isDirectory()) {
                printVariable(RTKey.LOG_DIR, logDir, "(ERROR: does not exist!)");
                addError("The specified '"+RTKey.LOG_DIR+"' directory does not exist.",
                         "The specified '"+RTKey.LOG_DIR+"' directory does not exist.",
                         "Please make sure '"+RTKey.LOG_DIR+"' specifies an existing directory.");
            } else {
                printVariable(RTKey.LOG_DIR, logDir, "");
                final AccumulatorLong accumCantReadWrite = new AccumulatorLong(0L);
                final long maxListedFiles = 4L;
                try {
                    FileTools.traverseAllFiles(logDir, new FileFilter() {
                        public boolean accept(File f) {
                            if (!FileTools.canRead(f) || !FileTools.canWrite(f)) {
                                accumCantReadWrite.increment();
                                if (accumCantReadWrite.get() < maxListedFiles) {
                                    println(PFX+"ERROR: Cannot read/write - " + f);
                                } else 
                                if (accumCantReadWrite.get() == maxListedFiles) {
                                    println(PFX+"ERROR: ... (additional non-read/writable files omitted) ...");
                                }
                            } else {
                                // OK
                            }
                            return true;
                        }
                    });
                } catch (Throwable th) { // NoSuchMethodException
                    Print.logException("Read/Writable files error", th);
                    int WC = countWarning("Unable to check for (non-)read/writable Log files");
                    println(PFX+"WARNING["+WC+"]: Unable to check for (non-)read/writable Log files: " + th);
                }
                if (accumCantReadWrite.get() > 0L) {
                    println(PFX+"ERROR: Log directory contains non-read/writable files!");
                    addError("Log directory contains non-read/writable files",
                             "Some Log files do not have the 'read/write' permission bits set or are owned by a different user.",
                             "Run 'chmod' to set the read/write bits, or 'chown' to change to the proper user.");
                }
            }
        }
        // DBPrivider
        String dbProv = "";
        {
            dbProv = RTConfig.getString(RTKey.DB_PROVIDER,"");
            if (StringTools.isBlank(dbProv)) {
                printVariable(RTKey.DB_PROVIDER, "", "(ERROR: not specified!)");
                addError("The DB provider has not been specified.",
                         "Missing '"+RTKey.DB_PROVIDER+"' specification in 'default.conf' (or included files).",
                         "Please include '"+RTKey.DB_PROVIDER+"' specification in 'default.conf' (or included files).");
            } else
            if ((webappProps != null) && !dbProv.equals(webappProps.getString(RTKey.DB_PROVIDER,""))) {
                printVariable(RTKey.DB_PROVIDER, dbProv, "(ERROR: does not match 'webapp.conf'!)");
                addError("The DB provider in 'default.conf' does not match specification in 'webapp.conf'.",
                         "Invalid '"+RTKey.DB_PROVIDER+"' specification in 'webapp.conf'.",
                         "Please include proper '"+RTKey.DB_PROVIDER+"' specification in 'webapp.conf'.");
            } else {
                printVariable(RTKey.DB_PROVIDER, dbProv, "");
            }
        }
        // DB Host
        {
            String dftHost = (defaultProps != null)? defaultProps.getString(RTKey.DB_HOST,"") : "";
            String dbHost  = RTConfig.getString(RTKey.DB_HOST,"");
            if (StringTools.isBlank(dftHost)) {
                // no host defined
                printVariable(RTKey.DB_HOST, "", "(ERROR: not specified!)");
                addError("The DB host has not been specified.",
                         "Missing '"+RTKey.DB_HOST+"' specification in 'default.conf'.",
                         "Please include '"+RTKey.DB_HOST+"' specification in 'default.conf'.");
            } else
            if (!dftHost.equals(dbHost)) {
                // host mispatch
                int WC = countWarning("DB host does not match host in 'default.conf'");
                printVariable(RTKey.DB_HOST, dftHost, "(WARNING["+WC+"]: does not match default host ["+dbHost+"])");
            } else
            if ((webappProps != null) && !dftHost.equals(webappProps.getString(RTKey.DB_HOST,""))) {
                // host mismatch
                int WC = countWarning("DB host does not match host in 'webapp.conf'");
                printVariable(RTKey.DB_HOST, dftHost, "(WARNING["+WC+"]: does not match 'webapp.conf')");
            } else
            if (!IPTools.isLocalhost(dftHost)) {
                // not localhost
                String dftHostIP = IPTools.getIPAddress(dftHost);
                int WC = countWarning("DB host ["+dftHostIP+"] does not match 'localhost'");
                printVariable(RTKey.DB_HOST, dftHost, "(WARNING["+WC+"]: is not 'localhost')");
            } else {
                // no warnings if localhost
                printVariable(RTKey.DB_HOST, dftHost, "");
            }
            //printVariable(RTKey.DB_PORT, String.valueOf(DBProvider.getDBPort()), "");
        }
        // DB Name
        {
            String dbName = RTConfig.getString(RTKey.DB_NAME,"");
            if (StringTools.isBlank(dbName)) {
                printVariable(RTKey.DB_NAME, "", "(ERROR: not specified!)");
                addError("The DB name has not been specified.",
                         "Missing '"+RTKey.DB_NAME+"' specification in 'default.conf'.",
                         "Please include '"+RTKey.DB_NAME+"' specification in 'default.conf'.");
            } else
            if ((webappProps != null) && !dbName.equals(webappProps.getString(RTKey.DB_NAME,""))) {
                String waName = webappProps.getString(RTKey.DB_NAME,"");
                printVariable(RTKey.DB_NAME, dbName, "(ERROR: does not match 'webapp.conf'!)");
                addError("The DB name in 'default.conf' does not match specification in 'webapp.conf'.",
                         "Invalid '"+RTKey.DB_NAME+"' specification in 'webapp.conf' ["+waName+"].",
                         "Please include proper '"+RTKey.DB_NAME+"' specification in 'webapp.conf'.");
            } else {
                printVariable(RTKey.DB_NAME, dbName, "");
            }
        }
        // DB User
        {
            String dbUser = RTConfig.getString(RTKey.DB_USER,"");
            if (StringTools.isBlank(dbUser)) {
                printVariable(RTKey.DB_USER, "", "(ERROR: not specified!)");
                addError("The DB user has not been specified.",
                         "Missing '"+RTKey.DB_USER+"' specification in 'default.conf' (or included files).",
                         "Please include '"+RTKey.DB_USER+"' specification in 'default.conf' (or included files).");
            } else
            if ((webappProps != null) && !dbUser.equals(webappProps.getString(RTKey.DB_USER,""))) {
                String waUser = webappProps.getString(RTKey.DB_USER,"");
                printVariable(RTKey.DB_USER, dbUser, "(ERROR: does not match 'webapp.conf'!)");
                addError("The DB user in 'default.conf' does not match specification in 'webapp.conf'.",
                         "Invalid '"+RTKey.DB_USER+"' specification in 'webapp.conf'.",
                         "Please include proper '"+RTKey.DB_USER+"' specification in 'webapp.conf' ["+waUser+"].");
            } else
            if (dbUser.equals("root")) {
                int WC = countWarning("DB user should not be 'root'");
                printVariable(RTKey.DB_USER, dbUser, "(WARNING["+WC+"]: should not be 'root')");
            } else {
                printVariable(RTKey.DB_USER, dbUser, "");
            }
        }
        // DB utf8
        {
            boolean dbUTF8 = RTConfig.getBoolean(RTKey.DB_UTF8,false);
            printVariable(RTKey.DB_UTF8, String.valueOf(dbUTF8), "");
        }
        // DB url
        {
            printVariable(RTKey.DB_URL   , RTConfig.getString(RTKey.DB_URL   ,""), "");
            printVariable(RTKey.DB_URL_DB, RTConfig.getString(RTKey.DB_URL_DB,""), "");
        }
        // Connection pooling
        {
            boolean dbcp = RTConfig.getBoolean(RTKey.DB_DBCONNECTION_POOL);
            //printVariable("(DB Connection Pool)", (dbcp?"enabled":"disabled"), "");
            printVariable(RTKey.DB_DBCONNECTION_POOL, String.valueOf(dbcp), "");
        }
        // MySQL
        if (StringTools.containsIgnoreCase(dbProv,"mysql")) {
            // (MySQL MaxConnections) ==>
            int recommendedMaxConn = 300;
            String maxConnTitle = "(MySQL MaxConnections)";
            File myCnfFile = new File("/etc/my.cnf"); // Linux
            if (!myCnfFile.isFile()) { myCnfFile = new File("/etc/mysql/my.cnf"); } // Linux
            if (myCnfFile.isFile()) {
                // format could be one of the following:
                //    #max_connections=500
                //    max_connections=500
                //    set-variable=max_connections=500
                java.util.List<String> maxConn = FileTools.findPatternInFile(myCnfFile,"max_connections",true);
                if (ListTools.size(maxConn) <= 0) {
                    maxConn = FileTools.findPatternInFile(myCnfFile,"max-connections",true);
                }
                if (ListTools.size(maxConn) > 0) {
                    // Found "max_connections"
                    for (String maxConnLine : maxConn) {
                        // skip commented lines
                        maxConnLine = StringTools.trim(maxConnLine);
                        if (maxConnLine.startsWith("#")) {
                            // ignore commented lines
                            continue;
                        }
                        // extract value
                        int eqSepPos = maxConnLine.lastIndexOf("="); // last occurance of "="
                        if (eqSepPos < 0) {
                            // missing "=" (invalid syntax)
                            printVariable(maxConnTitle, "unknown", "(unable to find specified max conn)");
                        } else {
                            String maxStr = maxConnLine.substring(eqSepPos+1).trim();
                            int    maxInt = StringTools.parseInt(maxStr,-1);
                            if (maxInt < 0) {
                                // invalid value
                                printVariable(maxConnTitle, "unknown", "(unable to parse specified max conn)");
                            } else
                            if (maxInt < recommendedMaxConn) {
                                // less than recommended value
                                printVariable(maxConnTitle, maxStr, "(Recommend at least "+recommendedMaxConn+")");
                                recommendations.append("- Recommend setting MySQL 'max_connections' to at least "+recommendedMaxConn+":\n");
                                recommendations.append("     see \"http://www.opengts.org/FAQ.html#faq_mysqlConn\"\n");
                            } else {
                                printVariable(maxConnTitle, maxStr, "");
                            }
                        }
                    }
                } else {
                    // "max_connections" not found
                    printVariable(maxConnTitle, "default", "(Recommend setting to at least "+recommendedMaxConn+")");
                    recommendations.append("- Recommend setting MySQL 'max_connections' to at least "+recommendedMaxConn+".\n");
                    recommendations.append("     see \"http://www.opengts.org/FAQ.html#faq_mysqlConn\"\n");
                }
            } else {
                printVariable(maxConnTitle, "unknown", "('"+myCnfFile+"' not found)");
            }
        }
        // StartupInit class
        {
            String startupInitClass = RTConfig.getString(DBConfig.PROP_StartupInit_class,"");
            String waStartupInitClass = (webappProps != null)? webappProps.getString(DBConfig.PROP_StartupInit_class,"") : "";
            if (StringTools.isBlank(startupInitClass) && StringTools.isBlank(waStartupInitClass)) {
                printVariable(DBConfig.PROP_StartupInit_class, "(default)", "");
            } else {
                String initClass = !StringTools.isBlank(startupInitClass)? startupInitClass : waStartupInitClass;
                printVariable(DBConfig.PROP_StartupInit_class, ClassName(initClass), "");
                Object startupInit = null;
                if (!startupInitClass.equals(waStartupInitClass)) {
                    println(PFX+"ERROR: 'webapp.conf' does not match 'default.conf'!");
                    addError("webapp.conf '"+DBConfig.PROP_StartupInit_class+"' does not match default.conf",
                             null,
                             "Change 'webapp.conf' to match 'default.conf'.");
                }
                try {
                    Class cfgClass = Class.forName(initClass);
                    startupInit = cfgClass.newInstance();
                } catch (ClassNotFoundException cnfe) {
                    println(PFX+"ERROR: Class not found!");
                    addError("Unable to load class '"+initClass+".",
                             "Class '"+initClass+"' was not found.",
                             "Fix class definition.");
                } catch (Throwable th) { // NoSuchMethodException, etc
                    println(PFX+"ERROR: Unable to load instance!");
                    addError("Unable to load class '"+initClass+".",
                             "Due to error '" + th.toString() + "'",
                             "Fix class definition.");
                }
            }
        }
        // RuleFactory
        {
            RuleFactory ruleFact = Device.getRuleFactory();
            if (ruleFact != null) {
                long compileTime = 0L;
                try {
                    MethodAction ma = new MethodAction(ruleFact,"getCompileTime");
                    Long ct = (Long)ma.invoke();
                    compileTime = (ct != null)? ct.longValue() : 0L;
                } catch (Throwable th) {
                    compileTime = 0L;
                }
                StringBuffer v = new StringBuffer();
                v.append("[").append(ruleFact.getName()).append(" ").append(ruleFact.getVersion());
                if (compileTime > 0L) {
                    v.append(" ").append((new DateTime(compileTime)).gmtFormat("yyyy/MM/dd HH:mm:ss"));
                }
                v.append("] ").append(ClassName(ruleFact));
                printVariable("(RuleFactory)", v.toString(), "");
                try {
                    // check required runtime support compments
                    MethodAction ma = new MethodAction(ruleFact,"checkRuntime");
                    Boolean rt = (Boolean)ma.invoke();
                    if ((rt != null) && !rt.booleanValue()) {
                        // RuleFactory has indicate an error
                        recommendations.append("- Recommend checking RuleFactory runtime support components.\n");
                    }
                } catch (Throwable th) {
                    // ignore
                    //Print.logException("RuleFactory",th);
                }
            } else {
                printVariable("(RuleFactory)", "(not installed)", "");
            }
        }
        // PingDispatcher
        {
            PingDispatcher pingDisp = Device.getPingDispatcher();
            if (pingDisp != null) {
                printVariable("(PingDispatcher)", ClassName(pingDisp), "");
            } else {
                //printVariable("(PingDispatcher)", "(not installed)", "");
            }
        }
        // SMS
        int smsIsFunctional = -1; // -1=no, 0=maybe, 1=yes
        if (defaultProps != null) {
            String   none    = "<none>";
            String   smsType = defaultProps.getString(SMSOutboundGateway.PROP_defaultName, none);
            StringBuffer sms = new StringBuffer();
            if (smsType.equals(none)) {
                //
                smsIsFunctional = -1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_emailBody)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_emailBody_smsEmailAddress,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_emailSubject)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_emailSubject_smsEmailAddress,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_httpURL)) {
                String s = defaultProps.getString(SMSOutboundGateway.PROP_httpURL_url,"");
                if (s.length() > 45) { s = s.substring(0,46) + " ..."; }
                sms.append(s);
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_clickatell)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_clickatell_smsEmailAddress,""));
                sms.append(", User=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_clickatell_user,""));
                sms.append(", Pass=");
                sms.append("xxxxxx"); //sms.append(defaultProps.getString(SMSOutboundGateway.PROP_clickatell_password,""));
                sms.append(", API=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_clickatell_api_id,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_mail2txt)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_mail2txt_smsEmailAddress,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_mail2txt160)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_mail2txt160_smsEmailAddress,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_mail2txtid)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_mail2txtid_smsEmailAddress,""));
                sms.append(", From=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_mail2txtid_from,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_mail2txt160id)) {
                sms.append("EMail=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_mail2txt160id_smsEmailAddress,""));
                sms.append(", From=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_mail2txt160id_from,""));
                smsIsFunctional = 1;
            } else
            if (smsType.equals(SMSOutboundGateway.GW_ozekisms)) {
                sms.append("HostPort=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_ozekisms_hostPort,""));
                sms.append(", Orig=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_ozekisms_originator,""));
                sms.append(", User=");
                sms.append(defaultProps.getString(SMSOutboundGateway.PROP_ozekisms_user,""));
                sms.append(", Pass=");
                sms.append("xxxxxx"); //v.append(defaultProps.getString(SMSOutboundGateway.PROP_ozekisms_password,""));
                smsIsFunctional = 1;
            } else {
                sms.append("<unrecognized>");
                smsIsFunctional = 0;
            }
            printVariable("(SMS Gateway)", smsType, "[" + sms + "]");
            if (!StringTools.isBlank(sendTestSMSTo) && (smsIsFunctional == 1)) {
                String smsAccount = Account.getSystemAdminAccountID(); // "sysadmin" AccountID
                println(PFX+"Sending test SMS message to '"+sendTestSMSTo+"' (via \""+smsAccount+"\") ...");
                try {
                    Account sysadmin = Account.getAccount(smsAccount);
                    SMSOutboundGateway gw = SMSOutboundGateway.GetSMSGateway(smsType);
                    if (sysadmin == null) {
                        println(PFX+"ERROR: Unable to send SMS (no '"+smsAccount+"' account)");
                        addError("Unable to send a test SMS message.",
                                 "No '"+smsAccount+"' account found.",
                                 "Please fix displayed errors and re-run CheckInstall.");
                    } else
                    if (gw == null) {
                        println(PFX+"ERROR: Unable to send SMS (SMSOutboundGateway not found)");
                        addError("Unable to send a test SMS message.",
                                 "Invalid SMSOutboundGateway specified.",
                                 "Please fix displayed errors and re-run CheckInstall.");
                    } else {
                        String smsText = "CheckInstall test message ["+DBConfig.getVersion()+"]";
                        SendMail.SetThreadModel(SendMail.THREAD_CURRENT); 
                        //Print.setLogLevel(Print.LOG_DEBUG, false/*inclDate*/, false/*inclFrame*/);
                        DCServerFactory.ResultCode rtn = gw.sendSMSMessage(sysadmin, null/*Device*/, smsText, sendTestSMSTo);
                        //Print.setLogLevel(Print.LOG_WARN , false/*inclDate*/, false/*inclFrame*/);
                        if (!DCServerFactory.ResultCode.SUCCESS.equals(rtn)) {
                            println(PFX+"ERROR: Unable to send SMS (SMSOutboundGateway returned error)");
                            addError("Unable to send a test SMS message.",
                                    "SMSOutboundGateway return error: " + rtn,
                                     "Please fix displayed errors and re-run CheckInstall.");
                        } else {
                            println(PFX+"... Test SMS message successfully sent.");
                        }
                    }
                } catch (Throwable th) {
                    println(PFX+"ERROR: Unable to send SMS (Exception occurred)");
                    addError("Unable to send a test SMS message.",
                             "Exception - " + th,
                             "Please fix displayed errors and re-run CheckInstall.");
                }
            }
        }
        // SMTP
        int emailIsFunctional = -1; // -1=no, 0=maybe, 1=yes
        boolean hasSMTPHost = true;
        {
            // SMTP properties
            SendMail.SmtpProperties smtpProps = null;
            BasicPrivateLabel bpl = null;
            if (!StringTools.isBlank(smtpPropBPL)) {
                bpl = BasicPrivateLabelLoader.getPrivateLabel(smtpPropBPL);
                if (bpl == null) {
                    println(PFX+"WARN: Invalid PrivateLabel name specified: " + smtpPropBPL);
                } else {
                    smtpProps = bpl.getSmtpProperties();
                }
            }
            if (smtpProps == null) {
                smtpProps = new SendMail.SmtpProperties();
                smtpPropBPL = "<default>";
            }
            //smtpProps.printProperties("SMTP Properties: " + smtpPropBPL);
            String  none      = "<none>";
            String  smtpHost  = StringTools.blankDefault(smtpProps.getHost(),none);
            int     smtpPort  = smtpProps.getPort();
            String  smtpUser  = StringTools.blankDefault(smtpProps.getUser(),none);
            String  smtpEmail = smtpProps.getUserEmail();
            String  smtpSSL   = smtpProps.getEnableSSL();
            String  smtpTLS   = smtpProps.getEnableTLS();
            int     smtpTMO   = smtpProps.getTimeoutMS();
            hasSMTPHost = !StringTools.isBlank(smtpHost) && !smtpHost.equals(none);
            // display
            String SMTPDesc = "(SMTP:"+smtpPropBPL+")";
            printVariable(SMTPDesc, smtpHost+":"+smtpPort, "[user="+smtpUser+", ssl="+smtpSSL+", tls="+smtpTLS+", timeout="+smtpTMO+"]");
            if (!hasSMTPHost) {
                printVariable("(SMTP Connection)", "", "SMTP service disabled (no host specified)");
            } else {
                // Socket connection to SMTP service
                boolean SMTP_port_ok = false;
                Socket socket = null;
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(smtpHost, smtpPort), 3000); // 3 seconds
                    printVariable("(SMTP Connection)", "Successful connection (does not guarantee service)", "");
                    SMTP_port_ok = true;
                } catch (SocketTimeoutException ste) {
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: connection timeout)");
                    addError("Unable to connect to the SMTP host:port '"+smtpHost+":"+smtpPort+"'.",
                             "Possible slow connection, or possible invalid SMTP host:port specification.",
                             "Please check proper SMTP specification, and re-run CheckInstall.", 
                             false);
                } catch (ConnectException ce) {
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: connection refused)");
                    addError("Unable to connect to the SMTP host:port '"+smtpHost+":"+smtpPort+"'.",
                             "Invalid SMTP host:port specified.",
                             "Please set valid SMTP host:port specification.",
                             false);
                } catch (UnknownHostException uhe) {
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: unknown host)");
                    addError("Unable to connect to the SMTP host '"+smtpHost+"'.",
                             "Invalid SMTP host specified in 'default.conf' (or included files).",
                             "Please set valid SMTP host specification.",
                             false);
                } catch (Throwable th) {
                    Print.logException("SMTP server connect error",th);
                    printVariable("(SMTP Connection)", "Failed", "(ERROR: unexpected error)");
                    addError("Unable to connect to the SMTP host:port '"+smtpHost+":"+smtpPort+"'.",
                             "Unexpected error received [" + th + "]",
                             "Please fix and re-run this CheckInstall.",
                             false);
                } finally {
                    try { if (socket != null) { socket.close(); } } catch (Throwable th) {/*ignore*/}
                    socket = null;
                }
                // JavaMail check
                String SMTP_session = "javax.mail.Session";
                boolean found_JavaMail = false;
                try {
                    Class.forName(SMTP_session);
                    //printVariable("(JavaMail)", "JavaMail present (found '"+SMTP_session+"')", "");
                    found_JavaMail = true;
                } catch (Throwable th) { // ClassNotFoundException
                    //printVariable("(JavaMail)", "", "ERROR: Unable to locate '" + SMTP_session + "'");
                    println(PFX+"ERROR: JavaMail not installed, unable to locate '" + SMTP_session + "'");
                    addError("Unable to locate JavaMail support (class '"+SMTP_session+"').",
                             "'mail.jar' may not be installed in a Java extended library directory.",
                             "Please install JavaMail, and re-run CheckInstall.");
                    found_JavaMail = false;
                }
                // SendMailArgs check (only if JavaMail was found)
                boolean SendMail_functional = false;
                if (found_JavaMail) {
                    try {
                        Class.forName(SendMail.SendMailArgs_className);
                        SendMail_functional = true;
                    } catch (Throwable th) { // ClassNotFoundException
                        println(PFX+"ERROR: JavaMail may not have been installed at compile time!");
                        addError("JavaMail was not installed at the time this code was compiled.",
                                 "'mail.jar' was not installed in a Java extended library directory.",
                                 "Please install JavaMail, recompile, and re-run CheckInstall.");
                    }
                }
                emailIsFunctional = !SendMail_functional? -1 : !SMTP_port_ok? 0 : 1;
                if (!StringTools.isBlank(sendTestEmailTo) && SMTP_port_ok && SendMail_functional) {
                    String toAddr   = sendTestEmailTo;
                    String fromAddr = smtpProps.getUserEmail();
                    if (StringTools.isBlank(fromAddr)) {
                        println(PFX+"ERROR: Unable to send email ('"+RTKey.SMTP_SERVER_USER_EMAIL+"' not defined)");
                        addError("Unable to send a test email.",
                                 "Property '"+RTKey.SMTP_SERVER_USER_EMAIL+"' has not been defined in the runtime config file.",
                                 "Please initialize this property to a valid 'from' email address, and re-run CheckInstall.");
                    } else
                    if (StringTools.isBlank(toAddr) || toAddr.endsWith("example.com")) {
                        println(PFX+"ERROR: Unable to send email (Invalid 'To' address)");
                        addError("Unable to send a test email.",
                                 "Invalid 'To' address specified.",
                                 "Please specify a valid 'To' address, and re-run CheckInstall.");
                    } else {
                        String subj = 
                            "CheckInstall test email ["+DBConfig.getVersion()+"]";
                        String body = 
                            "CheckInstall test email sent successfully.\n" + 
                            "";
                        SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
                        int loadTestCnt = 1; // RTConfig.getInt(ARG_EMAIL_LOAD_TEST,1);
                        if (loadTestCnt > 20) { loadTestCnt = 20; }
                        println(PFX+"Sending test email to '"+toAddr+"' ["+loadTestCnt+"] ...");
                        emailLoadTest:
                        for (int e = 0; e < loadTestCnt; e++) {
                            if (SendMail.send(fromAddr,toAddr,subj,body,null,smtpProps,false)) {
                                println(PFX+"... Test email successfully sent: ["+(e+1)+"/"+loadTestCnt+"]");
                                if (e == 0) {
                                    println(PFX+"    From   : " + fromAddr);
                                    println(PFX+"    To     : " + toAddr);
                                    println(PFX+"    Subject: " + subj);
                                    println(PFX+"    Body   : " + body);
                                }
                                //try {
                                //println(PFX+"    BodyHex: 0x" + StringTools.toHexString(body.getBytes(StringTools.CharEncoding_UTF_8)));
                                //} catch (Throwable th) {/* ignore */}
                            } else {
                                println(PFX+"ERROR: Unable to queue/send email ('SendMail' failed)");
                                addError("Unable to send a test email.",
                                         "'SendMail' failed (see previous errors).",
                                         "Please fix displayed errors and re-run CheckInstall.");
                                break; // emailLoadTest:
                            }
                        }
                    }
                }
            }
        }

        /* character encodings */
        println("");
        println("Character Encodings:");
        // Check Character Encoding
        {
            printVariable("(Default Encoding)", StringTools.getCharacterEncoding(), "");
        }
        // "file.encoding"
        {
            String propEncoding = "file.encoding";
            String fileEncoding = System.getProperty(propEncoding,null);
            if (fileEncoding != null) {
                printVariable(propEncoding, fileEncoding, "");
            } else {
                printVariable(propEncoding, "(not specified?)", "");
            }
        }
        // GTS_CHARSET
        {
            String envGtsCharset = System.getenv(ENVIRON_GTS_CHARSET);
            if (envGtsCharset != null) {
                try {
                    byte b[] = "hello".getBytes(envGtsCharset); // may throw exception
                    printVariable(ENVIRON_GTS_CHARSET, envGtsCharset, "");
                } catch (UnsupportedEncodingException uce) {
                    printVariable(ENVIRON_GTS_CHARSET, envGtsCharset, "(ERROR: invalid character encoding)");
                    addError("'"+ENVIRON_GTS_CHARSET+"' specifies an invalid character encoding.",
                             "Character encoding specified by '"+ENVIRON_GTS_CHARSET+"' is invalid",
                             FIX_PREVIOUS_ERRORS);
                }
            } else {
                //
            }
        }
        // DBProvider
        {
            String dbCharset = null;
            try {
                dbCharset = DBProvider.getDefaultCharacterSet();
            } catch (Throwable th) {
                // ignore
            }
            String dbcs = !StringTools.isBlank(dbCharset)? dbCharset : "?";
            printVariable("DBProvider:"+DBProvider.getProviderName(), dbcs, "");
        }

        /* Tables */
        println("");
        println("Tables ["+DBProvider.getDBUri(true)+"]");
        boolean skipTableChecks = false;
        if (defaultConfigFile != null) {
            String driver = DBProvider.loadJDBCDriver();
            if (driver == null) {
                println(PFX+"ERROR: JDBC driver not found or cannot be loaded!");
                addError("JDBC driver not found, or cannot be loaded.",
                         "The database JDBC driver has not been installed, or cannot be loaded.",
                         "Please install the appropriate JDBC driver with world-readable permissions.");
                // a missing JDBC driver would cause db access errors, skip tables
                skipTableChecks = true;
            } else {
                OrderedMap factMap = DBAdmin.getTableFactoryMap();
                boolean dispErr = SHOW_DB_ERRORS;
                for (Iterator i = factMap.keyIterator(); i.hasNext();) {
                    String tn = (String)i.next();
                    DBFactory<? extends DBRecord> f = (DBFactory<? extends DBRecord>)factMap.get(tn);
                    try {
                        if (!f.tableExists()) {
                            printVariable(f.getUntranslatedTableName(), "", "(ERROR: table does not exist!)");
                            addError("Table '"+f.getUntranslatedTableName()+"' does not exist.",
                                     "Database may not have been initialized.",
                                     "Please initialize the database.");
                        } else
                        if (!f.validateColumns(0x0000)) {
                            printVariable(f.getUntranslatedTableName(), "", "(ERROR: column validation failed!)");
                            addError("Table '"+f.getUntranslatedTableName()+"' failed column validation.",
                                     "Table may be missing columns, or have columns which have changed types.",
                                     "Run 'bin/dbAdmin.pl -tables' (or 'bin/dbconfig.bat -tables') for details.");
                            if (dispErr && (Print.getLogLevel() < Print.LOG_INFO)) {
                                // validate again, this time display validate errors
                                Print.setLogLevel(Print.LOG_INFO, false/*inclDate*/, false/*inclFrame*/);
                                f.validateColumns(DBAdmin.VALIDATE_DISPLAY_ERRORS); // again
                                Print.setLogLevel(Print.LOG_WARN, false/*inclDate*/, false/*inclFrame*/);
                            }
                        } else {
                            String dbEng = f.getIndexType();
                            StringBuffer sb = new StringBuffer();
                            sb.append("[").append(dbEng).append("] ");
                            if (RTConfig.getBoolean(PROP_skipDBRecordCount,false)) {
                                sb.append("Exists");
                            } else {
                                boolean actual  = false;
                                long    rcdCnt  = f.getRecordCount("",actual);
                                String  rcdCntS = String.valueOf(rcdCnt);
                                if (!actual && (rcdCnt > 0L) && dbEng.equalsIgnoreCase("InnoDB")) {
                                    rcdCntS = "~" + rcdCntS;  // InnoDB is estimated
                                }
                                //sb.append("RecordCount ");
                                sb.append(rcdCntS);
                            }
                            printVariable(f.getUntranslatedTableName(), sb.toString(), "");
                        }
                    } catch (DBException dbe) {
                        if (dbe.isSQLException()) {
                            SQLException sqle = (SQLException)dbe.getCause();
                            String sqlMsg = sqle.getMessage().toLowerCase();
                            if (sqlMsg.indexOf("access denied") >= 0) {
                                printVariable(tn, "", "(ERROR: SQL database access denied!)");
                                addError("Database access denied.",
                                         "Possible invalid user/password, or database name, specified in runtime config file",
                                         "Please specify a valid database name/user/password in the runtime config file");
                                //dbe.printException();
                            } else
                            if (sqlMsg.indexOf("communications link failure") >= 0) {
                                printVariable(tn, "", "(ERROR: SQL database connection failure!)");
                                addError("Database connection failure.",
                                         "Database may not be running on expected port",
                                         "Please start database service on expected port");
                                //dbe.printException();
                            } else
                            if (sqlMsg.indexOf("no suitable driver") >= 0) {
                                printVariable(tn, "", "(ERROR: Invalid JDBC driver!)");
                                addError("JDBC driver not found, or invalid.",
                                         "The JDBC driver is not installed, or is invalid for the specified database provider",
                                         "Please install the appropriate JDBC driver for the specified database provider");
                                //dbe.printException();
                            } else {
                                printVariable(tn, "", "(ERROR: SQL exception!)");
                                addError("SQL database exception while checking table '"+f.getUntranslatedTableName()+"' existance.",
                                         "Refer to above stacktrace for a detailed description",
                                         null);
                                dbe.printException();
                            }
                        } else {
                            printVariable(tn, "", "(ERROR: database exception!)");
                            addError("Database exception while checking table '"+f.getUntranslatedTableName()+"' existance.",
                                     "Refer to above stacktrace for a detailed description",
                                     null);
                            dbe.printException();
                        }
                        // the previous errors would be repeated for all tables, skip remaining tables
                        skipTableChecks = true;
                        break;
                    }
                }
            }
        } else {
            // The runtime config contains DB access information, if it isn't available, skip the table checks
            skipTableChecks = true;
        }
        if (skipTableChecks) {
            println(PFX+"ERROR: Skipping table checks due to previous errors");
            addError("Database table checks not performed.",
                     "Table checks ignored due to previous errors",
                     FIX_PREVIOUS_ERRORS);
        }

        /* [Basic]PrivateLabel (reports.xml) */
        println("");
        println("reports.xml:");
        // 'reports.xml' file path
        {
            File reportsXMLFile = ReportFactory._getReportXMLFile();
            if ((reportsXMLFile == null) || !reportsXMLFile.isFile()) {
                printVariable("(XML file)", "", "(ERROR: XML file not found)");
                addError("'reports.xml' file not found.",
                         "Unable to locate 'reports.xml' file.",
                         "Make sure that the 'reports.xml' file is available, then re-run this installation check");
            } else
            if (ReportFactory.hasParsingWarnings()) {
                printVariable("(XML file)", reportsXMLFile.toString(), "(ERROR: Has parsing errors)");
                addError("'reports.xml' has parsing errors.",
                         "The 'reports.xml' lokely has invalid XML syntax or other parsing errors.",
                         "Fix errors in 'reports.xml', then re-run this installation check");
            } else {
                printVariable("(XML file)", reportsXMLFile.toString(), "");
            }
            Collection<ReportFactory> rptFactList = ReportFactory.getReportFactories();
            printVariable("Total report count", String.valueOf(ListTools.size(rptFactList)), "");
            if (ListTools.isEmpty(rptFactList)) {
                // No reports found
                if (BasicPrivateLabelLoader.isTrackServlet()) {
                    int WC = countWarning("'reports.xml' does not define any reports");
                    println(PFX+"WARNING["+WC+"]: 'reports.xml' does not define any reports.");
                } else {
                    int WC = countWarning("'reports.xml' might not define any reports");
                    println(PFX+"WARNING["+WC+"]: 'reports.xml' might not define any reports.");
                }
            } else {
                // sort by ReportType/ReportName
                java.util.List<ReportFactory> rfList = ListTools.toList(rptFactList,new Vector<ReportFactory>());
                ListTools.sort(rfList, new Comparator<ReportFactory>() {
                    public int compare(ReportFactory rf1, ReportFactory rf2) {
                        // handle simple cases
                        if (rf1 == rf2) {
                            return 0;
                        } else
                        if (rf1 == null) {
                            return 1; // null sorts last
                        } else
                        if (rf2 == null) {
                            return -1; // null sorts last
                        }
                        // ReportFactory()
                        String rt1 = rf1.getReportType();
                        String rt2 = rf1.getReportType();
                        if (rt1.equalsIgnoreCase(rt2)) {
                            return rf1.getReportName().compareTo(rf1.getReportName());
                        } else {
                            return rt1.compareTo(rt2);
                        }
                    }
                });
                // iterate through ReportFactory list
                String lastReportType = null;
                for (ReportFactory rf : rfList) {
                    // Report type/name/etc
                    String rptType  = rf.getReportType();
                    String rptName  = rf.getReportName();
                    String rptTitle = rf.getReportTitle(null, "");
                    String rptLimit = "";
                    if (SHOW_REPORT_LIMITS && rf.hasReportConstraints()) {
                        ReportConstraints rc = rf.getReportConstraints();
                        long selLim = rc.getSelectionLimit();
                        long rptLim = rc.getReportLimit();
                        rptLimit = "(limits=" + selLim + "/" + rptLim + ")";
                    }
                    // Header break
                    if ((lastReportType == null) || !lastReportType.equalsIgnoreCase(rptType)) {
                        String rtDesc = ReportFactory.getReportTypeDescription(null, rptType);
                        printVariable("("+rptType+")", "--- " + rtDesc, "");
                        lastReportType = rptType;
                    }
                    // Detail
                    printVariable(" "+rptName, rptTitle, rptLimit, 26);
                }
            }
        }

        /* [Basic]PrivateLabel (private.xml) */
        println("");
        println("private.xml:");
        {
            // 'private.xml' file path
            File privLblXMLFile = BasicPrivateLabelLoader.getPrivateXMLFile();
            if ((privLblXMLFile == null) || !privLblXMLFile.isFile()) {
                printVariable("(XML file)", "", "(ERROR: XML file not found)");
                addError("'private.xml' file not found.",
                         "Unable to locate 'private.xml' file.",
                         "Make sure that the 'private.xml' file is available, then re-run this installation check");
            } else {
                printVariable("(XML file)", privLblXMLFile.toString(), "");
                if (env_CATALINA_HOME != null) {
                    String trackXMLName = "/webapps/track/WEB-INF/private.xml".replace('/',File.separatorChar);
                    File   trackXMLFile = new File(env_CATALINA_HOME, trackXMLName);
                    if (trackXMLFile.isFile()) {
                        byte T[] = FileTools.readFile(trackXMLFile);
                        byte G[] = FileTools.readFile(privLblXMLFile);
                        if ((T != null) && (G != null)) {
                            int diff = StringTools.compare(T, G, G.length);
                            if (diff != 0) {
                                String CH = isWindows? "%CATALINA_HOME%" : "$CATALINA_HOME";
                                int WC = countWarning("'private.xml' file does not match deployed version");
                                println(PFX+"WARNING["+WC+"]: does not match "+CH+trackXMLName);
                            }
                        }
                    }
                }
            }
            // BasicPrivateLabelLoader subclass
            Class loaderClass = BasicPrivateLabelLoader.getInstanceClass();
            printVariable("(Class)", ClassName(loaderClass), "");
            boolean isPrivateLabelLoader = false;
            try {
                isPrivateLabelLoader = PrivateLabelLoader.class.isAssignableFrom(loaderClass);
            } catch (Throwable th) { // NoClassDefFoundError
                isPrivateLabelLoader = false;
            }
            if (!isPrivateLabelLoader) {
                if (env_CATALINA_HOME == null) {
                    println(PFX+"ERROR: CATALINA_HOME not defined, unable to perform Servlet level validation.");
                    addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                             "CATALINA_HOME has not been defined",
                             "Define CATALINA_HOME");
                } else {
                    try {
                        Class.forName(BasicPrivateLabelLoader.CLASS_Track);
                        try {
                            Class.forName(BasicPrivateLabelLoader.CLASS_PrivateLabelLoader);
                            println(PFX+"ERROR: Unexpected 'PrivateLabelLoader' error, unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "Some unexpected error has occurred while loading 'PrivateLabelLoader'",
                                     null);
                        } catch (Throwable th) { // ClassNotFoundException
                            println(PFX+"ERROR: Unable to load 'PrivateLabelLoader.class', unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "Error loading 'PrivateLabelLoader': " + th,
                                     null);
                        }
                    } catch (NoClassDefFoundError ncdfe) {
                        String errMsg = StringTools.trim(ncdfe.getMessage());
                        if (errMsg.startsWith("javax/servlet")) {
                            println(PFX+"ERROR: Invalid CATALINA_HOME definition, unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "CATALINA_HOME is likely pointing to an invalid Tomcat installation",
                                     "Check directory referenced by CATALINA_HOME");
                        } else {
                            println(PFX+"ERROR: Required class not found, unable to perform Servlet level validation.");
                            addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                     "Unable to load class " + errMsg,
                                     "Check directory referenced by CATALINA_HOME");
                        }
                    } catch (Throwable th) {
                        println(PFX+"ERROR: Unable to load 'Track.class', unable to perform Servlet level validation.");
                        addError("Servlet level PrivateLabelLoader checks on 'private.xml' were not performed.",
                                 "Error loading 'Track': " + th,
                                 null);
                    }
                }
            }
            // load 'private.xml'
            String privateXML = "private.xml";
            ReportFactory.setIgnoreMissingReports(false);
            BasicPrivateLabelLoader.loadPrivateLabelXML();
            if (BasicPrivateLabelLoader.hasParsingErrors()) {
                println(PFX+"ERROR: Errors were encountered while parsing '"+privateXML+"'.");
                addError("Full '"+privateXML+"' checks were not performed.",
                         "Errors were encountered while parsing '"+privateXML+"'",
                         FIX_PREVIOUS_ERRORS);
            } else {
                // track servlet?
                if (!BasicPrivateLabelLoader.isTrackServlet()) {
                    println(PFX+"ERROR: '"+privateXML+"' not fully loaded (possible classpath issue?)");
                    addError("Full '"+privateXML+"' checks may not be performed due to possible classpath issues.",
                             "Possible incorrect command execution directory, or missing '"+TRACK_CLASS_DIR+"' directory.  " + 
                                 "This condition may cause false errors/warnings to be reported",
                             "Make sure '"+TRACK_CLASS_DIR+"' exists, then re-run this installation check from the " +
                                 "OpenGTS installation directory.");
                }
                OrderedSet<BasicPrivateLabel> privLabelSet = new OrderedSet<BasicPrivateLabel>(true);
                // has warnings?
                if (BasicPrivateLabelLoader.hasParsingWarnings()) {
                    int WC = countWarning("Warnings were encountered while parsing '"+privateXML+"'");
                    println(PFX+"WARNING["+WC+"]: Warnings were encountered while parsing '"+privateXML+"'");
                }
                // default Domain?
                BasicPrivateLabel defaultPrivLabel = BasicPrivateLabelLoader.getDefaultPrivateLabel();
                if (defaultPrivLabel != null) {
                    privLabelSet.add(defaultPrivLabel); // default first
                } else {
                    int WC = countWarning("'"+privateXML+"' does not define a default 'Domain'");
                    println(PFX+"WARNING["+WC+"]: '"+privateXML+"' does not define a default 'Domain'.");
                }
                // populate a set of BasicPrivateLabel's to test
                Collection<String> privLabelNames = BasicPrivateLabelLoader.getPrivateLabelNames();
                for (String privLabelName : privLabelNames) {
                    BasicPrivateLabel privLabel = BasicPrivateLabelLoader.getPrivateLabel(privLabelName);
                    if (privLabel != null) {
                        if (!privLabelSet.contains(privLabel)) {
                            privLabelSet.add(privLabel);
                        }
                    } else {
                        println(PFX+"ERROR: Unexpected error PrivateLabelName not found: " + privLabelName);
                        addError("Unexpected error PrivateLabelName not found: " + privLabelName,
                                 "Errors were encountered while parsing 'private.xml'",
                                 FIX_PREVIOUS_ERRORS);
                    }
                }
                // number of BasicPrivateLabels
                printVariable("(Domain count)", String.valueOf(privLabelSet.size()), "");
                int domainNdx = 1;
                for (BasicPrivateLabel privLabel : privLabelSet) {
                    String name  = privLabel.getDomainName();
                    String host  = privLabel.getHostName();
                    String alias = StringTools.join(privLabel.getHostAliasNames(),", ");
                    boolean isDefault = name.equals("default");
                    boolean skipDefaultEMailChecks = isDefault && RTConfig.getBoolean(PROP_skipDefaultEMailChecks,false);
                    boolean skipDefaultMapChecks   = isDefault && RTConfig.getBoolean(PROP_skipDefaultMapChecks,false);
                    StringBuffer nameInfo = new StringBuffer();
                    nameInfo.append(privLabel.getLocale().toString());
                    if (privLabel.getAccountLogin()) { 
                        nameInfo.append(", accountLogin"); 
                        String da = privLabel.getDefaultLoginAccount();
                        if (!StringTools.isBlank(da)) {
                            nameInfo.append("[\"").append(da).append("\"]");
                        }
                    }
                    if (privLabel.getUserLogin()) { 
                        nameInfo.append(", userLogin");
                        String du = privLabel.getDefaultLoginUser();
                        if (!StringTools.isBlank(du)) {
                            nameInfo.append("[\"").append(du).append("\"]");
                        }
                    }
                    if (privLabel.getAllowEmailLogin()) { 
                        nameInfo.append(", emailLogin"); 
                    }
                    if (privLabel.getEnableDemo()) { 
                        nameInfo.append(", demo");       
                    }
                    if (privLabel.isRestricted()) { 
                        nameInfo.append(", restricted"); 
                    }
                    printVariable((domainNdx++) + ") "+name, nameInfo.toString(), "");
                    printVariable("   (host)" , " "+host , "");
                    if (!StringTools.isBlank(alias)) {
                        printVariable("   (alias)", " "+alias, "");
                    }
                    // PasswordHandler check
                    try {
                        PasswordHandler pwh = privLabel.getPasswordHandler();
                        if (pwh != null) {
                            String pwhCN = StringTools.className(pwh);
                            if (pwh instanceof GeneralPasswordHandler) {
                                GeneralPasswordHandler gph = (GeneralPasswordHandler)pwh;
                                pwhCN += ":" + gph.getEncodingString();
                            }
                            printVariable("   (password handler)", " "+pwh.getName(), "("+pwhCN+")");
                        }
                    } catch (Throwable th) {
                        // ignore
                    }
                    // EMail checks
                    if (!skipDefaultEMailChecks) {
                        // EMail check
                        String email[] = privLabel.getEMailAddresses();
                        int emailErrors = 0;
                        for (int e = 0; e < email.length; e++) {
                            if (email[e].endsWith(SendMail.EXAMPLE_DOT_COM)) {
                                if (hasSMTPHost) {
                                    println(PFX+"ERROR: EMail address has not been customized: "+email[e]);
                                    if (emailErrors == 0) {
                                        addError("EMail addresses for Domain '"+name+"' have not been customized.",
                                                 null,
                                                 "Customize EMail address, then re-run this installation check.");
                                    }
                                } else {
                                    int WC = countWarning("EMail address has not been customized: " + email[e]);
                                    println(PFX+"WARNING["+WC+"]: EMail address has not been customized: " + email[e]);
                                }
                                emailErrors++;
                            }
                        }
                        if (privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_enableReportEmail,true)) {
                            String frEmail = privLabel.getEventNotificationFrom();
                            if (emailIsFunctional == -1) {
                                // error
                                //addError("Property 'reportMenu.enableReportEmail' enabled, but SMTP has not been configured.",
                                //         null,
                                //         "Configure outbound SMTP, then re-run this installation check.");
                                int WC = countWarning("Report email defined, but SMTP not configured");
                                println(PFX+"WARNING["+WC+"]: Property 'reportMenu.enableReportEmail' defined, but SMTP has not been configured.");
                            } else
                            if (StringTools.isBlank(frEmail)) {
                                // error
                                //addError("Property 'reportMenu.enableReportEmail' defined, but no 'From' configured.",
                                //         null,
                                //         "Configure outbound SMTP, then re-run this installation check.");
                                int WC = countWarning("Report email defined, but no 'From' address configured");
                                println(PFX+"WARNING:["+WC+"] Property 'reportMenu.enableReportEmail' defined, but no 'From' configured.");
                            } else
                            if (emailIsFunctional == 0) {
                                // warning
                                int WC = countWarning("Report email defined, but SMTP port not accessible");
                                println(PFX+"WARNING["+WC+"]: Property 'reportMenu.enableReportEmail' defined, but SMTP port not accessible.");
                            } else {
                                // ok
                            }
                        }
                    }
                    // ACL check
                    /*
                    String aclList[] = AclEntry.ACL_RESERVED_LIST;
                    int aclErrors = 0;
                    for (int a = 0; a < aclList.length; a++) {
                        if (!privLabel.hasAclEntry(aclList[a])) {
                            println(PFX+"ERROR: Missing reserved ACL entry: "+aclList[a]);
                            if (aclErrors++ == 0) {
                                addError("ACL list for Domain '"+name+"' is missing a reserved entry.",
                                         null,
                                         "Define reserved ACL entry, then re-run this installation check.");
                            }
                        }
                    }
                    */
                    // MapProvider check
                    try {
                        if (privLabel instanceof PrivateLabel) {
                            PrivateLabel pl = (PrivateLabel)privLabel;
                            // MapProvider check
                            MapProvider mp = pl.getMapProvider();
                            if (mp == null) {
                                if (BasicPrivateLabelLoader.isTrackServlet()) {
                                    println(PFX+"ERROR: No active MapProvider defined ["+name+"]");
                                    addError("Domain '"+name+"' is missing an active MapProvider declaration.",
                                             null,
                                             "Add a MapProvider declaration to this Domain, then re-run this installation check");
                                } else {
                                    int WC = countWarning("Make sure Domain '"+name+"' has an active MapProvider");
                                    println(PFX+"WARNING["+WC+"]: Make sure this Domain has an active MapProvider declaration.");
                                }
                            } else {
                                String mpDesc = mp.getName();
                                if (mp instanceof MapProviderAdapter) {
                                    MapProviderAdapter mpa = (MapProviderAdapter)mp;
                                    if (!StringTools.isBlank(mpa.getAuthorization())) {
                                        mpDesc += "(key)";
                                    }
                                }
                                //printVariable("(MapProvider)", " "+ClassName(mp), "");
                                printVariable("   (map provider)", " "+mpDesc, "");
                                if (!skipDefaultMapChecks && (mp instanceof MapProviderAdapter) && !((MapProviderAdapter)mp).validate()) {
                                    String mpName = mp.getName();
                                    println(PFX+"ERROR: MapProvider '"+mpName+"' returned a validation error");
                                    addError("MapProvider '"+mpName+"' returned a validation error for Domain '"+name+"'",
                                             null,
                                             FIX_PREVIOUS_ERRORS);
                                }
                            }
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // ReverseGeogoceProvider check
                    try {
                        ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
                        if (rgp != null) {
                            String rgDesc = rgp.getName();
                            if (rgp instanceof ReverseGeocodeProviderAdapter) {
                                ReverseGeocodeProviderAdapter rgpa = (ReverseGeocodeProviderAdapter)rgp;
                                if (!StringTools.isBlank(rgpa.getAuthorization())) {
                                    rgDesc += "(key)";
                                }
                            }
                            ReverseGeocodeProvider frgp = rgp.getFailoverReverseGeocodeProvider();
                            if (frgp != null) {
                                rgDesc += " [failover=" + frgp.getName() + "]";
                            }
                            printVariable("   (reverse-geocoder)", " "+rgDesc, "");
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // GeocodeProvider check
                    try {
                        GeocodeProvider gcp = privLabel.getGeocodeProvider();
                        if (gcp != null) {
                            String gcDesc = gcp.getName();
                            if (gcp instanceof GeocodeProviderAdapter) {
                                GeocodeProviderAdapter gcpa = (GeocodeProviderAdapter)gcp;
                                if (!StringTools.isBlank(gcpa.getAuthorization())) {
                                    gcDesc += "(key)";
                                }
                            }
                            printVariable("   (geocoder)", " "+gcDesc, "");
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // MobileLocationProvider check
                    try {
                        MobileLocationProvider mlp = privLabel.getMobileLocationProvider();
                        if (mlp != null) {
                            String mlpDesc = mlp.getName();
                            if (mlp instanceof MobileLocationProviderAdapter) {
                                MobileLocationProviderAdapter mlpa = (MobileLocationProviderAdapter)mlp;
                                if (!StringTools.isBlank(mlpa.getAuthorization())) {
                                    mlpDesc += "(key)";
                                }
                            }
                            printVariable("   (mobile location)", " "+mlpDesc, "");
                        }
                    } catch (Throwable th) { // NoClassDefFoundError
                        // ignore
                    }
                    // "privateLabelDetail"
                    if (RTConfig.getBoolean(ARG_privateLabelDetail,false)) {
                        privLabel.pushRTProperties();
                        printVariable("   "+RTKey.HTTP_USER_AGENT, " "+RTConfig.getString(RTKey.HTTP_USER_AGENT,"<default>"), "");
                        privLabel.popRTProperties();
                    }
                    // DefaultLoginAccount
                    String dftAcctID = privLabel.getDefaultLoginAccount();
                    boolean dftAcctExists = false;
                    if (!StringTools.isBlank(dftAcctID)) {
                        try {
                            dftAcctExists = Account.exists(dftAcctID);
                        } catch (DBException dbe) {
                            int WC = countWarning("DB Error when checking DefaultLoginAccount '"+dftAcctID+"' existence");
                            println(PFX+"WARNING["+WC+"]: DB Error determining if 'DefaultLoginAccount' exists.");
                            dftAcctExists = true;
                        }
                    }
                    if (!privLabel.getAccountLogin() && !privLabel.getAllowEmailLogin()) {
                        // 'User' login only (no accountId, and no emailAddress)
                        if (dftAcctExists) {
                            // normal state, all is ok
                        } else
                        if (StringTools.isBlank(dftAcctID)) {
                            println(PFX+"ERROR: 'accountLogin' is false, and DefaultLoginAccount is blank.");
                            addError("'accountLogin' is false, and DefaultLoginAccount is blank.",
                                     "'accountLogin' is false, 'emailLogin' is false, and no account-id has been " +
                                     "specified on the 'DefaultLoginAccount' tag",
                                     FIX_PREVIOUS_ERRORS);
                        } else {
                            println(PFX+"ERROR: 'accountLogin' is false, and account '"+dftAcctID+"' does not exist.");
                            addError("accountLogin='false', and account '"+dftAcctID+"' does not exist.",
                                     "accountLogin='false', emailLogin='false', and account-id '"+dftAcctID+"' " +
                                     "specified on the 'DefaultLoginAccount' tag does not exist",
                                     FIX_PREVIOUS_ERRORS);
                        }
                    } else
                    if (!privLabel.getAccountLogin() && privLabel.getAllowEmailLogin() && !StringTools.isBlank(dftAcctID)) {
                        // EMailAddress login allowed and a DefaultLoginAccount has been specified
                        println(PFX+"ERROR: DefaultLoginAccount specified when emailLogin='true'.");
                        addError("DefaultLoginAccount specified when emailLogin='true' and accountLogin='false'",
                                 "emailLogin='true', accountLogin='false', and a non-blank account-id has been " +
                                 "specified on the 'DefaultLoginAccount' tag",
                                 FIX_PREVIOUS_ERRORS);
                    }
                    if (!dftAcctExists && !StringTools.isBlank(dftAcctID)) {
                        // DefaultLoginAccount has been specified, which does not exist
                        int WC = countWarning("DefaultLoginAccount '"+dftAcctID+"' does not exist");
                        println(PFX+"WARNING["+WC+"]: DefaultLoginAccount '"+dftAcctID+"' does not exist");
                    }
                }
            }
        } // [Basic]PrivateLabel (private.xml)

        /* Initialized/Registered Device Communication Servers */
        println("");
        println("Device Communication Servers (registered):");
        {
            Set<String> dcUndefSet = ListTools.toSet(DCServerFactory.getUndefinedServerList());
            java.util.List<DCServerConfig> dcServerList = DCServerFactory.getServerConfigList(true/*inclAll*/);
            if (dcServerList.isEmpty()) {
                printVariable("   (none)", "", "");
            } else {
                int ndx = 1;
                String gtsHomeStr = (env_GTS_HOME != null)? env_GTS_HOME.toString() : "";
                for (DCServerConfig dcs : dcServerList) {
                    String name = dcs.getName();
                    if (dcs.serverJarExists()) {
                        String ndxStr   = StringTools.padLeft(String.valueOf(ndx++), ' ', 2);
                        File jarPath[]  = dcs.getRunningJarPath();
                        boolean running = !ListTools.isEmpty(jarPath);
                        String dcsDesc  = dcs.getDescription();
                        String dcsPorts = dcs.getPortsString();
                        printVariable(ndxStr+") " + name, "[" + dcsPorts + "] " + dcsDesc + (running?" (running)":""), "");
                        if (running) {
                            for (int d = 0; d < jarPath.length; d++) {
                                String jarPathStr = jarPath[d].toString();
                                printVariable("     (running)", " " + jarPathStr, "");
                                if (!StringTools.isBlank(gtsHomeStr) && !jarPathStr.startsWith(gtsHomeStr)) {
                                    int WC = countWarning("DCServer jar path is not in the current GTS_HOME path: " + name);
                                    println(PFX+"WARNING["+WC+"]: DCServer jar path is not in the current GTS_HOME path" );
                                    //Print.sysPrintln("GTS_HOME: " + gtsHomeStr);
                                    //Print.sysPrintln("JAR Path: " + jarPathStr);
                                }
                                File logFile = DCServerConfig.getLogFilePath(jarPath[d]);
                                boolean logExists = ((logFile != null) && logFile.isFile());
                                if (logExists) {
                                    printVariable("     (logfile)", " " + logFile.toString(), "");
                                }
                            }
                            String cmds[] = dcs.getCommandList();
                            if (!ListTools.isEmpty(cmds)) {
                                String cmdList = StringTools.join(cmds,", ");
                                printVariable("     (commands)", " " + cmdList, "");
                            }
                        }
                        Set<String> cfgPropKeys = dcs.getRecommendedConfigPropertyKeys();
                        if (!ListTools.isEmpty(cfgPropKeys)) {
                            int WC = 0;
                            for (String key : cfgPropKeys) {
                                String val = RTConfig.getString(key,"");
                                // -- data type? boolean, integer, etc.?
                                boolean isDefined = false;
                                if (StringTools.isBlank(val)) {
                                    // blank is undefined
                                    isDefined = false;
                                } else
                                if (StringTools.isBoolean(val,true)) {
                                    // (boolean == true)?
                                    isDefined = StringTools.parseBoolean(val,false);
                                } else
                                if (StringTools.isLong(val,true)) {
                                    // (long > 0)?
                                    isDefined = (StringTools.parseLong(val,0L) > 0L)? true : false;
                                } else {
                                    // not a boolean nor integer
                                    isDefined = false;
                                }
                                // -- not defined?
                                if (!isDefined) {
                                    if (WC <= 0) {
                                        // Required 'calamp' DCS properties undefined
                                        // Undefined properties found for 'calamp' DCS
                                        // Found undefined properties required for DCS: calamp
                                        WC = countWarning("Found undefined properties required for DCS: "+name);
                                    }
                                    println(PFX+"WARNING["+WC+"]: Property undefined: " + key);
                                }
                            }
                        }
                    } else
                    if (dcs.isJarOptional()) {
                        String ndxStr   = StringTools.padLeft(String.valueOf(ndx++), ' ', 2);
                        String dcsDesc  = dcs.getDescription();
                        String dcsPorts = dcs.getPortsString();
                        printVariable(ndxStr+") " + name, "[" + dcsPorts + "] " + dcsDesc + " (no jar)", "");
                    } else
                    if (dcUndefSet.contains(name)) {
                        String ndxStr = StringTools.padLeft(String.valueOf(ndx++), ' ', 2);
                        printVariable(ndxStr+") " + name, dcs.toString(false), "");
                        int WC = countWarning("Referenced server jar does not exist: " + name);
                        println(PFX+"WARNING["+WC+"]: Server jar referenced in runtime config not found: "+name);
                    } else {
                        // ignore these
                    }
                }
            }
        } // Device Communication Servers

        /* crontab.xml/cronRuleFactoryLite.xml */
        {
            Vector<String> cronRuleCron = null;
            Vector<String> cronReportCron = null;
            boolean cronBorderCrossing = false;
            String rptJobAdminK = "Domain.WebPages.ReportJobAdmin";
            boolean rptJobAdmin = RTConfig.getBoolean(rptJobAdminK,false);
            // "Cron" available?
            Object cron = null;
            try {
                MethodAction ma = new MethodAction("org.opengts.extra.util.Cron");
                cron = ma.invoke(); // new Cron();
                // found
            } catch (Throwable th) {
                // not found
            }
            // find "crontab" file
            if (cron != null) {
                println("");
                println("Crontab Configuration:");
                String gtsHomeStr      = (env_GTS_HOME != null)? env_GTS_HOME.toString() : ".";
                String crontabFileName = null;
                File   crontabFile     = null;
                String crontabList[]   = new String[] { 
                    "crontab/crontab.xml",
                    "crontab/cronRuleFactoryLite.xml" 
                    };
                for (int ctf = 0; ctf < crontabList.length; ctf++) {
                    File crontab = new File(gtsHomeStr, crontabList[ctf]);
                    if (FileTools.isFile(crontab)) {
                        crontabFileName = crontabList[ctf];
                        crontabFile     = crontab;
                        break;
                    }
                }
                if (!FileTools.isFile(crontabFile)) {
                    printVariable("Crontab file", "(not found)", "");
                } else {
                    printVariable("Crontab file", crontabFileName, "");
                    try {
                        // cron.load(crontabFile);
                        MethodAction maLoad = new MethodAction(cron,"load",File.class);
                        maLoad.invoke(crontabFile);  // may throw IOException
                        // java.util.List<CronJob> jobList = cron.getJobList();
                        MethodAction maJobList = new MethodAction(cron,"getJobNames");
                        String jobNames[] = (String[])maJobList.invoke();
                        printVariable("Active Job Count", String.valueOf(ListTools.size(jobNames)), "");
                        MethodAction maJobDesc = new MethodAction(cron,"getJobDescription",String.class);
                        for (String jn : jobNames) {
                            // -- abbreviate job description
                            String jobDesc = StringTools.trim((String)maJobDesc.invoke(jn));
                            if (jobDesc.startsWith("org.opengts.rule.")) {
                                jobDesc = "..." + jobDesc.substring("org.opengts.rule.".length());
                            } else
                            if (jobDesc.startsWith("org.opengts.extra.tables.")) {
                                jobDesc = "..." + jobDesc.substring("org.opengts.extra.tables.".length());
                            } else
                            if (jobDesc.startsWith("org.opengts.bcross.tables.")) {
                                jobDesc = "..." + jobDesc.substring("org.opengts.bcross.tables.".length());
                            } else 
                            if (jobDesc.startsWith("org.opengts.")) {
                                jobDesc = "..." + jobDesc.substring("org.opengts.".length());
                            } 
                            // -- display
                            printVariable("  " + jn, jobDesc, "");
                            // -- extract RuleCron tags
                            if (StringTools.startsWithIgnoreCase(jn,"RuleCron")) {
                                if (cronRuleCron == null) { cronRuleCron = new Vector<String>(); }
                                int p = jobDesc.indexOf("-tag=");
                                if (p >= 0) {
                                    p += "-tag=".length();
                                    int s = jobDesc.indexOf(" ",p);
                                    String cronTags = (s > p)? jobDesc.substring(p,s) : jobDesc.substring(p);
                                    if (!StringTools.isBlank(cronTags)) {
                                        for (String rt : StringTools.split(cronTags,',')) {
                                            cronRuleCron.add(rt);
                                        }
                                    }
                                }
                            }
                            // -- extract ReportCron tags
                            if (StringTools.startsWithIgnoreCase(jn,"ReportCron")) {
                                if (cronReportCron == null) { cronReportCron = new Vector<String>(); }
                                int p = jobDesc.indexOf("-tag=");
                                if (p >= 0) {
                                    p += "-tag=".length();
                                    int s = jobDesc.indexOf(" ",p);
                                    String cronTags = (s > p)? jobDesc.substring(p,s) : jobDesc.substring(p);
                                    if (!StringTools.isBlank(cronTags)) {
                                        for (String rt : StringTools.split(cronTags,',')) {
                                            cronReportCron.add(rt);
                                        }
                                    }
                                }
                            }
                            // -- indicate that BorderCrossing cron is active
                            if (StringTools.startsWithIgnoreCase(jn,"BorderCrossing")) {
                                cronBorderCrossing = true;
                            }
                        } // loop through Cron jobs
                    } catch (Throwable th) {
                        // fail
                        int WC = countWarning("Unable to load crontab file: "+crontabFileName);
                        println(PFX+"WARNING["+WC+"]: Unable to load crontab file: "+crontabFileName);
                        Print.logException("Cron query",th);
                    }
                }
                // - RuleCron
                if (cronRuleCron != null) {
                    printVariable("RuleCron tags", StringTools.join(cronRuleCron,","), "");
                }
                // - ReportCron
                if (cronReportCron != null) {
                    // ReportJob  : The user-selectable options that will be executed by ReportCron
                    // ReportCron : The Cron Job entries for executing selected reports
                    boolean trackEnServ = RTConfig.getBoolean(DBConfig.PROP_track_enableService,false);
                    String  gtsReqURL   = RTConfig.getString(DBConfig.PROP_GTSRequest_url,"");
                    // -- get ReportJob tags
                    Vector<String> rptJobTags = new Vector<String>();
                    try {
                        MethodAction maTagMap = new MethodAction("org.opengts.extra.tables.ReportJob","GetIntervalTagMap");
                        Map iTagMap = (Map)maTagMap.invoke();
                        if (iTagMap != null) {
                            for (Object tn : iTagMap.keySet()) {
                                rptJobTags.add(tn.toString());
                            }
                        }
                    } catch (Throwable th) {
                        //
                    }
                    // -- ReportJob/ReportCron
                    printVariable("ReportJob tags"     , StringTools.join(rptJobTags,","), "");
                    printVariable("ReportCron tags"    , StringTools.join(cronReportCron,","), "");
                    for (String rjt : rptJobTags) {
                        // make sure that all ReportJob options are defined in ReportCron entries.
                        if (rjt.equalsIgnoreCase("none")) { continue; }
                        if (!ListTools.containsIgnoreCase(cronReportCron,rjt)) {
                            int WC = countWarning("Define 'ReportCron_"+rjt+"' (tag '"+rjt+"') in '"+crontabFileName+"'");
                            println(PFX+"WARNING["+WC+"]: ReportCron does not define tag: " + rjt);
                        }
                    }
                    // - track_enableService=true
                    printVariable("track.enableService", String.valueOf(trackEnServ), "");
                    if (!trackEnServ) {
                        //int WC = countWarning("'"+DBConfig.PROP_track_enableService+"' required for ReportCron use");
                        //println(PFX+"WARNING["+WC+"]: Enable '"+DBConfig.PROP_track_enableService+"' for ReportCron use");
                        println(PFX+"ERROR: ReportCron in use, but '"+DBConfig.PROP_track_enableService+"' not defined");
                        addError("ReportCron in use, but '"+DBConfig.PROP_track_enableService+"' not defined",
                                 "'"+DBConfig.PROP_track_enableService+"' is undefined, or set to 'false'",
                                 "Set '"+DBConfig.PROP_track_enableService+"' to 'true'");
                    }
                    // - GTSRequest.url=http://localhost:8080/track/Service
                    printVariable("GTSRequest.url"     , gtsReqURL, "");
                    if (StringTools.isBlank(gtsReqURL)) {
                        //int WC = countWarning("'"+DBConfig.PROP_GTSRequest_url+"' required for ReportCron use");
                        //println(PFX+"WARNING["+WC+"]: Set '"+DBConfig.PROP_GTSRequest_url+"' for ReportCron use");
                        println(PFX+"ERROR: ReportCron in use, but '"+DBConfig.PROP_GTSRequest_url+"' not defined");
                        addError("ReportCron in use, but '"+DBConfig.PROP_GTSRequest_url+"' not defined",
                                 "'"+DBConfig.PROP_GTSRequest_url+"' is not set to the web-service URL",
                                 "Set '"+DBConfig.PROP_GTSRequest_url+"' to proper web-service URL");
                    }
                    // - Domain.WebPages.ReportJobAdmin=true
                    printVariable("ReportJobAdmin"     , String.valueOf(rptJobAdmin), "("+rptJobAdminK+")");
                    if (!rptJobAdmin) {
                        //int WC = countWarning("'"+rptJobAdminK+"' required for ReportCron use");
                        //println(PFX+"WARNING["+WC+"]: Enable '"+rptJobAdminK+"' for ReportCron use");
                        println(PFX+"ERROR: ReportCron in use, but '"+rptJobAdminK+"' not defined");
                        addError("ReportCron in use, but '"+rptJobAdminK+"' not defined",
                                 "'"+rptJobAdminK+"' is undefined, or set to 'false'",
                                 "Set '"+rptJobAdminK+"' to 'true'");
                    }
                } else {
                    // no active ReportCron tasks
                    if (rptJobAdmin) {
                        println(PFX+"ERROR: '"+rptJobAdminK+"' is set 'true', but no ReportCron tasks are defined");
                        addError("'"+rptJobAdminK+"' is set 'true', but no ReportCron tasks are defined",
                                 "'"+rptJobAdminK+"' is set 'true', but no ReportCron tasks are defined",
                                 "Define at least one ReportCron task, or set '"+rptJobAdminK+"' to 'false'");
                    }
                }
                // - BorderCrossing
                if (cronBorderCrossing) {
                    printVariable("BorderCrossingCron", "true", "");
                    String startupBorderCrossing = "startupInit.Device.BorderCrossingFieldInfo";
                    if (!RTConfig.getBoolean(startupBorderCrossing,false)) {
                        //int WC = countWarning("'"+startupBorderCrossing+"' required for BorderCrossing use");
                        //println(PFX+"WARNING["+WC+"]: Enable '"+startupBorderCrossing+"' for BorderCrossing use");
                        println(PFX+"ERROR: '"+startupBorderCrossing+"' required for BorderCrossingCron use");
                        addError("'"+startupBorderCrossing+"' required for BorderCrossingCron use",
                                 "BorderCrossingCron in use, but '"+startupBorderCrossing+"' is set to 'false'",
                                 "Set '"+startupBorderCrossing+"' to 'true', then reinitialize Device table with " +
                                 "BorderCrossing columns");
                    }
                }
            } // Cron class found
        }

        /* Recommended symbolic links */
        if ((env_GTS_LINKS != null) && env_GTS_LINKS.isDirectory()) {
            println("");
            println("Recommended symbolic links:");
            // - "/usr/local/gts" to $GTS_HOME
            {
                File link     = new File(env_GTS_LINKS, "gts");
                File target   = FileTools.getRealFile(link);
                File expect   = env_GTS_HOME;
                String envVar = OSTools.isWindows()? "%GTS_HOME%" : "$GTS_HOME";
                if (OSTools.isWindows() && FileTools.isCygwinSymlink(target)) {
                    target = FileTools.getCygwinSymlinkFile(target,true);
                }
                if (target == null) {
                    printVariable(link.toString(), "?", "");
                    int WC = countWarning("Symbolic Link does not exist: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link does not exist, or is invalid: "+link.toString());
                    recommendations.append("- Recommend creating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!target.equals(expect)) {
                    printVariable(link.toString(), target.toString(), "");
                    int WC = countWarning("Symbolic Link is not up to date: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link is not up to date: "+link.toString());
                    recommendations.append("- Recommend recreating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     rm " + link + "\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!link.toString().equals(target.toString())) {
                    printVariable(link.toString(), target.toString(), "");
                } else
                if (OSTools.isWindows()) {
                    // Note: "file.getCanonicalFile()" does not work for Window "Junction" links.
                    printVariable(link.toString(), "(Windows link?)", "");
                } else {
                    printVariable(link.toString(), "--", "");
                }
            }
            // - "/usr/local/java" to $JAVA_HOME
            {
                File link     = new File(env_GTS_LINKS, "java");
                File target   = FileTools.getRealFile(link);
                File expect   = env_JAVA_HOME;
                String envVar = OSTools.isWindows()? "%JAVA_HOME%" : "$JAVA_HOME";
                if (OSTools.isWindows() && FileTools.isCygwinSymlink(target)) {
                    target = FileTools.getCygwinSymlinkFile(target,true);
                }
                if (target == null) {
                    printVariable(link.toString(), "?", "");
                    int WC = countWarning("Symbolic Link does not exist: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link does not exist, or is invalid: "+link.toString());
                    recommendations.append("- Recommend creating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!target.equals(expect)) {
                    printVariable(link.toString(), target.toString(), "");
                    int WC = countWarning("Symbolic Link is not up to date: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link is not up to date: "+link.toString());
                    recommendations.append("- Recommend recreating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     rm " + link + "\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!link.toString().equals(target.toString())) {
                    printVariable(link.toString(), target.toString(), "");
                } else
                if (OSTools.isWindows()) {
                    // Note: "file.getCanonicalFile()" does not work for Window "Junction" links.
                    printVariable(link.toString(), "(Windows link?)", "");
                } else {
                    printVariable(link.toString(), "--", "");
                }
            }
            // - "/usr/local/tomcat" to $CATALINA_HOME
            {
                // - "/usr/local/tomcat" to $CATALINA_HOME
                File link     = new File(env_GTS_LINKS, "tomcat");
                File target   = FileTools.getRealFile(link);
                File expect   = env_CATALINA_HOME;
                String envVar = OSTools.isWindows()? "%CATALINA_HOME%" : "$CATALINA_HOME";
                if (OSTools.isWindows() && FileTools.isCygwinSymlink(target)) {
                    target = FileTools.getCygwinSymlinkFile(target,true);
                }
                if (target == null) {
                    printVariable(link.toString(), "?", "");
                    int WC = countWarning("Symbolic Link does not exist, or is invalid: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link does not exist: "+link.toString());
                    recommendations.append("- Recommend creating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!target.equals(expect)) {
                    printVariable(link.toString(), target.toString(), "");
                    int WC = countWarning("Symbolic Link is not up to date: " + link.toString());
                    println(PFX+"WARNING["+WC+"]: Symbolic link is not up to date: "+link.toString());
                    recommendations.append("- Recommend recreating "+link+" symbolic link to point to "+envVar+":\n");
                    recommendations.append("     rm " + link + "\n");
                    recommendations.append("     ln -s " + expect + " " + link + "\n");
                } else
                if (!link.toString().equals(target.toString())) {
                    printVariable(link.toString(), target.toString(), "");
                } else
                if (OSTools.isWindows()) {
                    // Note: "file.getCanonicalFile()" does not work for Window "Junction" links.
                    printVariable(link.toString(), "(Windows link?)", "");
                } else {
                    printVariable(link.toString(), "--", "");
                }
            }
        }
        
        /* separator */
        println("");
        println(eqSep);

        /* display summary of errors */
        int rtnCode = 0;
        println("");
        if (!getErrors().isEmpty()) {
            println("** Found " + getErrors().size() + " Error(s)!");
            println(StringTools.replicateString("*",sepWidth));
            int ndx = 1;
            for (Iterator<String[]> i = getErrors().iterator(); i.hasNext();) {
                String err[] = i.next();
                println((ndx++) + ") " + err[0]);
                if (!StringTools.isBlank(err[1])) {
                    wrapPrintln("   [Reason: " + err[1] + "]", ' ');
                }
                if ((err.length > 2) && !StringTools.isBlank(err[2])) {
                    wrapPrintln("   [Fix: " + err[2] + "]", ' ');
                }
                if ((err.length <= 3) || !err[3].equals("false")) {
                    rtnCode = 1;
                }
            }
            println(StringTools.replicateString("*",sepWidth));
        } else {
            println("No errors reported");
        }

        /* display warning count */
        println("");
        if (warnCount > 0) {
            println("-- Found " + warnCount + " Warning(s):");
            for (Iterator<String> i = getWarnings().iterator(); i.hasNext();) {
                String warnMsg = i.next();
                wrapPrintln(warnMsg, ' ');
            }
        } else {
            println("No warnings reported");
        }
        
        /* display recommendations */
        if (recommendations.length() > 0) {
            println("");
            println("-- Recommendations:");
            println(recommendations.toString().trim());
        }

        /* done */
        println("");
        println(eqSep);
        println(eqSep);
        System.exit(rtnCode);
        

    }
    
    private static StringBuffer getLinkRecommendation(
        StringBuffer sb, AccumulatorLong index, 
        File link, File target, File expected, String envVarName)
    {
        if (link == null) {
            // ignore this recommendation
        } else
        if (target == null) {
            int n = (int)index.next();
            sb.append(n + ") Symbolic link: "+link+" (does not exist)\n");
            sb.append("   Recommend creating symbolic link to point to "+envVarName+":\n");
            sb.append("     ln -s " + expected + " " + link + "\n");
        } else
        if (!target.equals(expected)) {
            int n = (int)index.next();
            sb.append(n + ") Symbolic link: "+link+" (exists, but does not point to the current installation)\n");
            sb.append("   Recommend recreating symbolic link to point to "+envVarName+":\n");
            sb.append("     rm " + link + "\n");
            sb.append("     ln -s " + expected + " " + link + "\n");
        } else {
            int n = (int)index.next();
            sb.append(n + ") Symbolic link: "+link+" (exists, and is up to date)\n");
        }
        return sb;
    }
    
    // ------------------------------------------------------------------------
    
}
