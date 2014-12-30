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
//  Create compile-time contant source module
// ----------------------------------------------------------------------------
// Change History:
//  2009/01/28  Martin D. Flynn
//     -Initial release
//  2011/04/01  Martin D. Flynn
//     -Updated "daysUntil"/"secondsUntil" to support past dates/times
//  2013/08/27  Martin D. Flynn
//     -Updated standard template
//  2013/11/11  Martin D. Flynn
//     -Added support for /*{...}*/ replacement variables.
//     -Added support of //#if...//#else...//#endif
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
*** Create compile-time contant source module
**/

public class CompiletimeVars
{

    // ------------------------------------------------------------------------

    /* standard default template */
    private static final String TEMPLATE_                   = "@";
    private static final String TEMPLATE_DEFAULT            = "@default";

    /**
    *** Compile-time variable delimiters (should be unique to this module)<br>
    *** If these delimters change, make sure that the standard template delimiters 
    *** used below also change accordingly.<br>
    *** IE. %{var=value}
    **/
    private static final String STR_DELIM                   = "%{";
    private static final String END_DELIM                   = "}";
    private static final String DFT_DELIM                   = "=";

    /**
    *** Compile-time variable delimiters that allow allow placing replacement 
    *** variables within comments in the code (so that it could still be compiled
    *** without running through the replacement process).
    *** IE. /#{var=value}#/
    **/
    private static final String STR_CDELIM                  = "/*{";
    private static final String END_CDELIM                  = "}*/";
    private static final String DFT_CDELIM                  = "=";

    /**
    *** Conditional inclusion
    **/
    private static final String COND_                       = "//#";
    private static final String COND_U_IF                   = COND_ + "IF";
    private static final String COND_L_IF                   = COND_U_IF.toLowerCase();
    private static final String COND_U_ELSE                 = COND_ + "ELSE";
    private static final String COND_L_ELSE                 = COND_U_ELSE.toLowerCase();
    private static final String COND_U_ENDIF                = COND_ + "ENDIF";
    private static final String COND_L_ENDIF                = COND_U_ENDIF.toLowerCase();
    private static final String COND_COMMENT                = "##";

    private static boolean _startsWith(String S, String T) 
    {
        /*
        int sl = S.length();
        int tl = T.length();
        int ii = 0;
        for (; (ii < sl) && (ii < tl); i++) {
            char sc = Character.toUpperCase(S.charAt(ii));
            char tc = Character.toUpperCase(T.charAt(ii));
            if (tc != sc) { break; }
        }
        return (ii == tl)? true : false;
        */
        if (S.startsWith(T)) {
            return true;
        } else
        if (S.startsWith(T.toUpperCase())) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Exception for file exists, but overwrite not specified
    **/
    public static class NoOverwriteException
        extends IOException
    {
        public NoOverwriteException(String msg) {
            super(msg);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String JAVA_PACKAGE                = "package";
    private static final String JAVA_PACKAGE_               = JAVA_PACKAGE + " ";

    /**
    *** Returns a "package" line for the specified package name
    **/
    private static String packageLine(String pkgName)
    {
        if (StringTools.isBlank(pkgName)) {
            return "// no package";
        } else
        if (pkgName.startsWith(JAVA_PACKAGE_)) {
            return pkgName + ";";
        } else {
            return JAVA_PACKAGE_ + pkgName + ";";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return the standard "CompileTime.java" template
    **/
    private static String standardTemplate(String tn, String pkgName)
    {

        /* not a standard template specification? */
        if (!StringTools.isBlank(tn) && !tn.startsWith(TEMPLATE_)) {
            Print.errPrintln("\nNot a Standard Template specification: " + tn);
            return null;
        }

        /* set default standard template name */
        if (StringTools.isBlank(tn) || tn.equals(TEMPLATE_)) {
            tn = TEMPLATE_DEFAULT;
        }

        /* default/CompileTimestamp template */
        if (tn.equalsIgnoreCase(TEMPLATE_DEFAULT)) {
            StringBuffer sb = new StringBuffer();
            sb.append(CompiletimeVars.packageLine(pkgName)).append("\n");
            sb.append("public class CompileTime\n");
            sb.append("{\n");
            sb.append("    // %{datetime=0000/00/00 00:00:00 GMT}\n");
            sb.append("    public static final long   COMPILE_TIMESTAMP    = %{timestamp=0}L;\n");
            sb.append("    public static final String COMPILE_DATETIME     = \"%{date=0000/00/00} %{time=00:00:00} %{timezone=GMT}\";\n");
            sb.append("    public static final String SERVICE_ACCOUNT_ID   = \"%{ServiceAccount.ID=}\";\n");
            sb.append("    public static final String SERVICE_ACCOUNT_NAME = \"%{ServiceAccount.Name=}\";\n");
            sb.append("    public static final String SERVICE_ACCOUNT_KEY  = \"%{ServiceAccount.Key=}\";\n");
            sb.append("}\n");
            return sb.toString();
        }

        /* standard template not found */
        Print.errPrintln("\nStandard Template name not found: " + tn);
        return null;

    }

    /**
    *** Reads/returns the specified CompileTime template file
    **/
    private static String readTemplate(File tf, String pkgName)
    {

        /* read template data */
        byte templData[] = FileTools.readFile(tf);
        if (templData == null) {
            Print.errPrintln("\nUnable to read Input/Template file: " + tf);
            return null;
        } else
        if (templData.length == 0) {
            Print.errPrintln("\nInput/Template file is empty: " + tf);
            return null;
        }

        /* return template String */
        String templateText = StringTools.toStringValue(templData);
        if (!StringTools.isBlank(pkgName) && !StringTools.isBlank(templateText)) {
            String lines[] = StringTools.split(templateText,'\n',false);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().startsWith(JAVA_PACKAGE_)) {
                    lines[i] = CompiletimeVars.packageLine(pkgName);
                    return StringTools.join(lines,'\n') + "\n";
                }
            }
            StringBuffer sb = new StringBuffer();
            sb.append(CompiletimeVars.packageLine(pkgName)).append("\n");
            sb.append(templateText);
            return sb.toString();
        } else {
            return templateText;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Apply conditionals to specified source
    **/
    public static String getConditionalSource(String outputText)
    {
        // - must have at least one "//#" in this template
        // - This conditional line-inclusion checker is not designed to be a comprehensive 
        //   precompiler, but just a simple single-level pre-compile-time template processor.
        // - Will not process conditionals if no 'if' is found
        // - The String " #" is not allow in a specified value (ambiguous with invalid comment)
        // - Does NOT currently support nested IF..ELSE..ENDIF !!!
        // - Prefixing spaces are allowed
        // - If anything other than a spece preceeds the '//#' the conditional will be ignored

        /* exit now if "//#" is not present in template */
        if (outputText.indexOf(COND_) < 0) {
            return outputText;
        }

        /* convert to lines */
        String s[] = StringTools.parseStringArray(outputText,"\n",false); // "\r\n"

        /* loop through code, replacing conditional code */
        StringBuffer sb = new StringBuffer();
        int ifLevel = 0;
        boolean saveLine = true;
        boolean okSave   = true;
        checkConditional:
        for (int i = 0; i < s.length; i++) {

            /* current line contains conditional? */
            if (s[i].indexOf(COND_) >= 0) {
                // -- this line contains "//#"
                String S = s[i].trim(); // remove leading space
                // -- remove any trailing comments; ## this is a comment
                int c = S.indexOf(COND_COMMENT);
                if (c > 0) {
                    S = S.substring(0,c).trim();
                }
                // -- look for IF..ELSE..ENDIF
                if (S.startsWith(COND_U_IF) || S.startsWith(COND_L_IF)) {
                    // //#IF [key[=value]]
                    if (ifLevel > 0) {
                        // -- already encounted an "if"
                        Print.errPrintln("\nNested 'if' encountered");
                        okSave = false;
                        break checkConditional;
                    }
                    String cond = S.substring(COND_U_IF.length()).trim();
                    int p = cond.indexOf("=");
                    String key = (p>=0)? cond.substring(0,p).trim() : cond.trim();
                    String val = (p>=0)? cond.substring(p+1).trim() : null;
                    if (StringTools.isBlank(key)) {
                        // //#IF
                        // -- no condition, assume "true"
                        saveLine = true;
                    } else
                    if (val == null) {
                        // //#IF [!]key
                        // //#IF [!]true
                        // //#IF [!]false
                        // -- check for prefixing "!"
                        boolean not = false;
                        if (key.startsWith("!")) {
                            not = true;
                            key = key.substring(1);
                        }
                        // -- parse boolean
                        boolean bool = false;
                        if (key.equalsIgnoreCase("true")) {
                            bool = true;
                        } else
                        if (key.equalsIgnoreCase("false")) {
                            bool = false;
                        } else {
                            bool = RTConfig.getBoolean(key,false);
                        }
                        // -- save line?
                        saveLine = not? !bool : bool;
                    } else {
                        // //#IF key=value
                        // -- compare key=value (case insensitive)
                        // -- compare key==value (case sensitive)
                        String rtpVal = RTConfig.getString(key,"").trim();
                        if (val.indexOf(" #") >= 0) {
                            Print.errPrintln("\nValue contains invalid comment specification: " + val);
                            okSave = false;
                            break checkConditional;
                        }
                        if (val.startsWith("=")) { 
                            // -- case-sensitive compare
                            saveLine = val.substring(1).equals(rtpVal);
                        } else {
                            // -- case-insensitive compare
                            saveLine = val.equalsIgnoreCase(rtpVal);
                        }
                    }
                    ifLevel++;
                    continue;
                } else
                if (S.startsWith(COND_U_ELSE) || S.startsWith(COND_L_ELSE)) {
                    // //#ELSE
                    if (ifLevel <= 0) {
                        // -- found an 'ELSE' without a previous 'IF'
                        Print.errPrintln("\n'ELSE' without 'IF' encountered");
                        okSave = false;
                        break checkConditional;
                    }
                    saveLine = !saveLine;
                    continue;
                } else
                if (S.startsWith(COND_U_ENDIF) || S.startsWith(COND_L_ENDIF)) {
                    // //#ENDIF
                    if (ifLevel <= 0) {
                        // -- found an 'ENDIF' without a previous 'IF'
                        Print.errPrintln("\n'ENDIF' without 'IF' encountered");
                        okSave = false;
                        break checkConditional;
                    }
                    ifLevel--;
                    saveLine = true;
                    continue;
                } else
                if (S.startsWith(COND_)) {
                    // //#???
                    Print.errPrintln("\nUnrecognized conditional: " + S);
                    continue;
                } else {
                    // -- embedded '//#'
                }
            }

            /* save current line? */
            if (saveLine) {
                sb.append(s[i]).append("\n");
            }

        }

        /* save if no errors */
        if (okSave) {
            outputText = sb.toString();
        } else {
            Print.errPrintln("\nConditional code ignored due to previous errors");
        }
        return outputText;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the output file
    **/
    public static File getOutputFile(File directory, String outputName, boolean overwrite)
        throws NoOverwriteException, IOException
    {

        /* no output name, assume stdout */
        if (StringTools.isBlank(outputName)) {
            return null;
        }

        /* get File */
        File outputFile = (directory != null)? 
            new File(directory, outputName) : 
            new File(outputName);

        /* file exists? */
        if (outputFile.exists()) {
            // -- output file specified, and exists
            if (!overwrite) {
                throw new NoOverwriteException("Output file already exists (overwrite not specified).");
            } else
            if (!outputFile.isFile()) {
                throw new IOException("Overwrite specified, but specified existing output is not a file.");
            }
        }

        /* return file */
        return outputFile;

    }

    /**
    *** Apply replacements/conditionals and write output
    **/
    public static void writeOutputSource(String inputSource, File outputFile)
        throws IOException
    {
        String outputSource = inputSource;
        if (StringTools.isBlank(outputSource)) {
            // throw new IOException("Output Source is empty");
            return;
        }

        /* replace standard runtime vars in text (ie. %{var=value}) */
        // -- replace %{var=value}
        outputSource = RTConfig.insertKeyValues(outputSource, STR_DELIM, END_DELIM, DFT_DELIM);
        // -- replace /#{var=value}#/
        outputSource = RTConfig.insertKeyValues(outputSource, STR_CDELIM, END_CDELIM, DFT_CDELIM);

        /* conditional code? */
        outputSource = CompiletimeVars.getConditionalSource(outputSource);

        /* write output */
        if (outputFile != null) {
            Print.sysPrintln("Output to file: " + outputFile);
            boolean didWrite = FileTools.writeFile(outputSource.getBytes(), outputFile);
            if (!didWrite) {
                throw new IOException("Unable to write output file.");
            }
        } else {
            // -- write to stdout
            Print.sysPrintln(outputSource);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_HELP[]          = new String[] { "help"     , "h"    };
    private static final String ARG_EXTRA[]         = new String[] { "extra"    , "args" };
    private static final String ARG_DIR[]           = new String[] { "directory", "dir"  };
    private static final String ARG_OPTIONAL[]      = new String[] { "template?",        };
    private static final String ARG_TEMPLATE[]      = new String[] { "template" ,        };
    private static final String ARG_PACKAGE[]       = new String[] { "package"  , "pkg"  };
    private static final String ARG_OUTPUT[]        = new String[] { "output"   , "out"  };
    private static final String ARG_OVERWRITE[]     = new String[] { "overwrite"         };
 
    /**
    *** Print usage and exit
    **/
    private static void _usage()
    {
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + CompiletimeVars.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -template=@default      Create default 'CompileTime' template");
        Print.sysPrintln("  -template=<file>        Input Java 'template' file (must exist)");
        Print.sysPrintln("  -template?=<file>       Optional input Java 'template' file (may exist)");
        Print.sysPrintln("  -package=<packageName>  Optional package name");
        Print.sysPrintln("  -output=<file>          Output Java file");
        Print.sysPrintln("  -overwrite=true         Overwrite output file, if it exists");
        Print.sysPrintln("");
        System.exit(1);
    }
    
    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* help */
        if (RTConfig.hasProperty(ARG_HELP)) {
            _usage();
            System.exit(1); // control does not reach here
        }

        /* extra args */
        String extraArgs = RTConfig.getString(ARG_EXTRA, "");
        if (!StringTools.isBlank(extraArgs)) {
            //Print.logInfo("Extra: " + extraArgs);
            RTProperties cfgProps = RTConfig.getConfigFileProperties();
            if (extraArgs.indexOf(",") > 0) {
                cfgProps.setProperties(extraArgs,',');
            } else {
                cfgProps.setProperties(extraArgs);
            }
            //cfgProps.printProperties("");
        }

        /* special case "expire" */
        String ARG_EXPIRE = "expire";
        RTProperties expireRTP = RTConfig.getPropertiesForKey(ARG_EXPIRE,false); 
        if (expireRTP != null) {
            Print.errPrintln("\n'expire' cannot be defined.");
            System.exit(1);
        }

        /* args */
        File    directory    = RTConfig.getFile(ARG_DIR,null);
        String  templateName = StringTools.trim(RTConfig.getString(ARG_OPTIONAL,RTConfig.getString(ARG_TEMPLATE,"")));
        boolean isOptional   = RTConfig.hasProperty(ARG_OPTIONAL);
        String  packageName  = StringTools.trim(RTConfig.getString(ARG_PACKAGE, null));
        String  outputName   = RTConfig.getString(ARG_OUTPUT, "");
        boolean overwrite    = RTConfig.getBoolean(ARG_OVERWRITE, false);

        /* cannot specify "-output=inline" if internal template specified */
        if (StringTools.isBlank(outputName)      || 
            outputName.equalsIgnoreCase("stdout")  ) {
            // -- output will be to "stdout"
            outputName = "";
        } else
        if (outputName.equalsIgnoreCase("inline")) {
            // -- output will be to input file
            if (templateName.startsWith(TEMPLATE_)) {
                // -- @template not allowed with "inline"
                Print.errPrintln("\nError: Cannot specify '-output=inline' with internal template");
                _usage();
                System.exit(1); // control does not reach here
            } else 
            if (isOptional) {
                // -- optional not allowed with "inline"
                Print.errPrintln("\nError: Cannot specify '-output=inline' with optional template");
                _usage();
                System.exit(1); // control does not reach here
            } else
            if (!templateName.startsWith("*")) {
                // -- output to input file
                outputName = templateName;
            }
        } else {
            // -- output to specified file
            if (templateName.startsWith("*")) {
                // -- template specifies a file-glob
                Print.errPrintln("\nError: Cannot specify '-output=FILE' for recursize template specification");
                _usage();
                System.exit(1); // control does not reach here
            }
        }

        /* template name file? */
        if (templateName.startsWith("*")) {
            if (isOptional) {
                // -- optional not allowed with glob file specification
                Print.errPrintln("\nError: Cannot specify optional '-template?=[**/]*.java'");
                _usage();
                System.exit(1); // control does not reach here
            }
        } else {
            File file = (directory != null)?
                new File(directory, templateName) :
                new File(templateName);
            if (!file.isFile() && isOptional) {
                // -- templateName doesn't exist and is optional
                templateName = TEMPLATE_DEFAULT;
            }
        }

        /* set current time (subject to change) */
        String tzStr = RTConfig.getString("timezone",null);
        TimeZone tz  = !StringTools.isBlank(tzStr)? DateTime.getTimeZone(tzStr) : DateTime.getDefaultTimeZone();
        DateTime now = new DateTime(tz);
        if (!RTConfig.hasProperty("timetamp"))  { RTConfig.setLong  ("timestamp", now.getTimeSec()); }
        if (!RTConfig.hasProperty("datetime"))  { RTConfig.setString("datetime" , now.format("yyyy/MM/dd HH:mm:ss z")); }
        if (!RTConfig.hasProperty("date"    ))  { RTConfig.setString("date"     , now.format("yyyy/MM/dd")); }
        if (!RTConfig.hasProperty("time"    ))  { RTConfig.setString("time"     , now.format("HH:mm:ss")); }
        if (!RTConfig.hasProperty("timezone"))  { RTConfig.setString("timezone" , now.format("z")); }

        /* special case "daysUntil" */
        // %{daysUntil=2012:02:20}  <== fixed time
        String ARG_daysUntil_ = "daysUntil";
        Set<String> daysUntil_keys = RTConfig.getPropertyKeys(ARG_daysUntil_, false);
        for (String daysUntil_key : daysUntil_keys) {
            String daysUntil_key_date = daysUntil_key + ".date";
            RTProperties daysUntilRTP = RTConfig.getPropertiesForKey(daysUntil_key, false); 
            if (daysUntilRTP != null) {
                // -- get/update the RTProperties where "daysUntil" is defined
                String daysUntil = daysUntilRTP.getString(daysUntil_key,"");
                if (StringTools.isBlank(daysUntil)) {
                    // -- remove keys
                    daysUntilRTP.removeProperty(daysUntil_key     );
                    daysUntilRTP.removeProperty(daysUntil_key_date);
                    //Print.sysPrintln(daysUntil_key      + " ==> <removed>");
                    //Print.sysPrintln(daysUntil_key_date + " ==> <removed>");
                } else
                if ((daysUntil.indexOf("/") >= 0) || (daysUntil.indexOf(":") >= 0)) {
                    // -- Change "yyyy:mm:dd" to "DD"
                    // Note: The ':' separator should be used instead of '/', because "2010/10/01" is
                    // syntactically correct (ie. division) and can be compiled into a valid value, 
                    // while "2010:10:01" is not, and will be caught by the compiler.
                    if (daysUntil.startsWith("'") || daysUntil.startsWith("\"")) {
                        daysUntil = daysUntil.substring(1); // remove prefixing quote
                    }
                    if (daysUntil.endsWith("'") || daysUntil.endsWith("\"")) {
                        daysUntil = daysUntil.substring(0,daysUntil.length()-1); // remove trailing quote
                    }
                    try {
                        DateTime nowDT  = new DateTime(DateTime.getGMTTimeZone());
                        DateTime futDT  = DateTime.parseArgumentDate(daysUntil,null,true);
                        long     nowDay = DateTime.getDayNumberFromDate(nowDT);
                        long     futDay = DateTime.getDayNumberFromDate(futDT);
                        long     deltaD = futDay - nowDay;
                        if (deltaD == 0L) {
                            // -- today
                            deltaD = 1L; // make it tomorrow
                        } else
                        if (deltaD < 0L) {
                            // -- this means that the date has already passed
                            //deltaD = -1L; // already negative
                        } else {
                            deltaD += 1L; // add one more day
                        }
                        daysUntilRTP.setString(daysUntil_key     , String.valueOf(deltaD));
                        daysUntilRTP.setString(daysUntil_key_date, futDT.format(DateTime.DEFAULT_DATE_FORMAT));
                    } catch (DateTime.DateParseException dpe) {
                        Print.logException("Unable to parse Date: " + daysUntil, dpe);
                        System.exit(1);
                    }
                    //Print.sysPrintln(daysUntil_key      + " ==> " + daysUntilRTP.getString(daysUntil_key     ,"?"));
                    //Print.sysPrintln(daysUntil_key_date + " ==> " + daysUntilRTP.getString(daysUntil_key_date,"?"));
                } else {
                    long futSec = DateTime.getCurrentTimeSec() + DateTime.DaySeconds(StringTools.parseLong(daysUntil,0L));
                    daysUntilRTP.setString(daysUntil_key_date, (new DateTime(futSec)).format(DateTime.DEFAULT_DATE_FORMAT));
                    //Print.sysPrintln(daysUntil_key      + " ==> " + daysUntilRTP.getString(daysUntil_key     ,"?"));
                    //Print.sysPrintln(daysUntil_key_date + " ==> " + daysUntilRTP.getString(daysUntil_key_date,"?"));
                }
            }
        }

        /* special case "daysFromNow" */
        // %{daysFromNow=30}  <== 30 days from now
        String ARG_daysFromNow_ = "daysFromNow";
        Set<String> daysFromNow_keys = RTConfig.getPropertyKeys(ARG_daysFromNow_, false);
        for (String daysFromNow_key : daysFromNow_keys) {
            String daysFromNow_key_date = daysFromNow_key + ".date";
            RTProperties daysFromNowRTP = RTConfig.getPropertiesForKey(daysFromNow_key, false); 
            if (daysFromNowRTP != null) {
                // -- get/update the RTProperties where "daysFromNow" is defined
                String daysFromNow = daysFromNowRTP.getString(daysFromNow_key,"");
                if (StringTools.isBlank(daysFromNow)) {
                    // -- remove keys
                    daysFromNowRTP.removeProperty(daysFromNow_key     );
                    daysFromNowRTP.removeProperty(daysFromNow_key_date);
                    //Print.sysPrintln(daysFromNow_key      + " ==> <removed>");
                    //Print.sysPrintln(daysFromNow_key_date + " ==> <removed>");
                } else {
                    long futSec = DateTime.getCurrentTimeSec() + DateTime.DaySeconds(StringTools.parseLong(daysFromNow,0L));
                    daysFromNowRTP.setString(daysFromNow_key     , String.valueOf(futSec));
                    daysFromNowRTP.setString(daysFromNow_key_date, (new DateTime(futSec)).format(DateTime.DEFAULT_DATE_FORMAT));
                    //Print.sysPrintln(daysFromNow_key      + " ==> " + daysFromNowRTP.getString(daysFromNow_key     ,"?"));
                    //Print.sysPrintln(daysFromNow_key_date + " ==> " + daysFromNowRTP.getString(daysFromNow_key_date,"?"));
                }
            }
        }

        /* special case "secondsUntil" */
        // %{secondsUntil_abc=2012:02:20} <== fixed time
        // %{secondsUntil_abc=86400}      <== relative time (86400 seconds from now)
        String ARG_secondsUntil_ = "secondsUntil";
        Set<String> secUntil_keys = RTConfig.getPropertyKeys(ARG_secondsUntil_, false);
        for (String secUntil_key : secUntil_keys) {
            String secUntil_key_date = secUntil_key + ".date";
            RTProperties secUntilRTP = RTConfig.getPropertiesForKey(secUntil_key, false); 
            if (secUntilRTP != null) {
                // -- get/update the RTProperties where "secondsUntil" is defined
                String secUntil = secUntilRTP.getString(secUntil_key,"");
                if (StringTools.isBlank(secUntil)) {
                    // remove keys
                    secUntilRTP.removeProperty(secUntil_key     );
                    secUntilRTP.removeProperty(secUntil_key_date);
                    //Print.sysPrintln(secUntil_key      + " ==> <removed>");
                    //Print.sysPrintln(secUntil_key_date + " ==> <removed>");
                } else
                if ((secUntil.indexOf("/") >= 0) || (secUntil.indexOf(":") >= 0)) {
                    // -- Change "yyyy:mm:dd:HH:MM:SS" to "ssssss"
                    // Note: The ':' separator should be used instead of '/', because "2010/10/01" is
                    // syntactically correct (ie. division) and can be compiled into a valid value, 
                    // while "2010:10:01" is not, and will be caught by the compiler.
                    if (secUntil.startsWith("'") || secUntil.startsWith("\"")) {
                        secUntil = secUntil.substring(1); // remove prefixing quote
                    }
                    if (secUntil.endsWith("'") || secUntil.endsWith("\"")) {
                        secUntil = secUntil.substring(0,secUntil.length()-1); // remove trailing quote
                    }
                    try {
                        long     nowSec = DateTime.getCurrentTimeSec();
                        DateTime futDT  = DateTime.parseArgumentDate(secUntil,null,true);
                        long     futSec = futDT.getTimeSec();
                        long     deltaS = futSec - nowSec;
                        if (deltaS == 0L) {
                            // -- now
                            deltaS = 1L; // make it 1 second from now
                        } else
                        if (deltaS < 0L) {
                            // -- this means that the time has already passed
                            //deltaS = -1L; // already negative
                        } else {
                            deltaS += 1L; // add one more second
                        }
                        secUntilRTP.setString(secUntil_key     , String.valueOf(deltaS));
                        secUntilRTP.setString(secUntil_key_date, futDT.toString());
                    } catch (DateTime.DateParseException dpe) {
                        Print.logException("Unable to parse Date: " + secUntil, dpe);
                        System.exit(1);
                    }
                    //Print.sysPrintln(secUntil_key      + " ==> " + secUntilRTP.getString(secUntil_key     ,"?"));
                    //Print.sysPrintln(secUntil_key_date + " ==> " + secUntilRTP.getString(secUntil_key_date,"?"));
                } else {
                    long futSec = DateTime.getCurrentTimeSec() + StringTools.parseLong(secUntil,0L);
                    secUntilRTP.setString(secUntil_key_date, (new DateTime(futSec)).toString());
                    //Print.sysPrintln(secUntil_key      + " ==> " + secUntilRTP.getString(secUntil_key     ,"?"));
                    //Print.sysPrintln(secUntil_key_date + " ==> " + secUntilRTP.getString(secUntil_key_date,"?"));
                }
            }
        }

        /* special case "secondsFromNow" */
        // %{secondsFromNow=30}  <== 30 seconds from now
        String ARG_secondsFromNow_ = "secondsFromNow";
        Set<String> secondsFromNow_keys = RTConfig.getPropertyKeys(ARG_secondsFromNow_, false);
        for (String secondsFromNow_key : secondsFromNow_keys) {
            String secondsFromNow_key_date = secondsFromNow_key + ".date";
            RTProperties secondsFromNowRTP = RTConfig.getPropertiesForKey(secondsFromNow_key, false); 
            if (secondsFromNowRTP != null) {
                // -- get/update the RTProperties where "secondsFromNow" is defined
                String secondsFromNow = secondsFromNowRTP.getString(secondsFromNow_key,"");
                if (StringTools.isBlank(secondsFromNow)) {
                    // -- remove keys
                    secondsFromNowRTP.removeProperty(secondsFromNow_key     );
                    secondsFromNowRTP.removeProperty(secondsFromNow_key_date);
                    //Print.sysPrintln(secondsFromNow_key      + " ==> <removed>");
                    //Print.sysPrintln(secondsFromNow_key_date + " ==> <removed>");
                } else {
                    long futSec = DateTime.getCurrentTimeSec() + StringTools.parseLong(secondsFromNow,0L);
                    secondsFromNowRTP.setString(secondsFromNow_key     , String.valueOf(futSec));
                    secondsFromNowRTP.setString(secondsFromNow_key_date, (new DateTime(futSec)).format(DateTime.DEFAULT_DATE_FORMAT));
                    //Print.sysPrintln(secondsFromNow_key      + " ==> " + secondsFromNowRTP.getString(secondsFromNow_key     ,"?"));
                    //Print.sysPrintln(secondsFromNow_key_date + " ==> " + secondsFromNowRTP.getString(secondsFromNow_key_date,"?"));
                }
            }
        }

        /* special case "limit" */
        String ARG_limit_ = "limit";
        Set<String> limit_keys = RTConfig.getPropertyKeys(ARG_limit_, false);
        for (String limit_key : limit_keys) {
            RTProperties limitRTP = RTConfig.getPropertiesForKey(limit_key, false); 
            if (limitRTP != null) {
                String limit = limitRTP.getString(limit_key,"");
                if (StringTools.isBlank(limit)) {
                    limitRTP.removeProperty(limit_key);
                    //Print.sysPrintln(limit_key + " ==> <removed>");
                } else {
                    //Print.sysPrintln(limit_key + " ==> " + limit);
                }
            }
        }

        /* adjust packageName */
        if (packageName.equals(JAVA_PACKAGE)) {
            Print.errPrintln("\nWarning: 'package' argument cannot equal \"package\" (setting to empty string).");
            packageName = "";
        }

        // --------------------------------------

        /* internal template (single pass only) */
        if (StringTools.isBlank(templateName) || 
            templateName.startsWith(TEMPLATE_)  ) {
            try {
                // -- input source
                String inputSource = standardTemplate(templateName, packageName);
                if (StringTools.isBlank(inputSource)) {
                    if (isOptional) {
                        // -- optional "templateName" not defined, use default template
                        inputSource = standardTemplate(null, packageName); // non-blank
                    } else {
                        throw new IOException("Standard template not found: " + templateName);
                    }
                }
                // -- write template output
                File outputFile = CompiletimeVars.getOutputFile(directory, outputName, overwrite);
                CompiletimeVars.writeOutputSource(inputSource, outputFile);
            } catch (NoOverwriteException noe) {
                // -- outputFile exists, quietly ignore
                Print.sysPrintln(noe.getMessage());
                System.exit(0);
            } catch (IOException ioe) {
                // -- error writin file
                Print.errPrintln("\nError writing template: " + ioe.getMessage());
                System.exit(1);
            }
            System.exit(0);
        }

        // --------------------------------------

        /* get input file(s) */
        File inputFileArray[] = null;
        if (templateName.startsWith("**/*.")) {
            // -- all files, in all directories (recursive)
            String fileGlob = templateName.substring(3);
            File files[] = FileTools.getFiles(directory, fileGlob, true);
            inputFileArray = files;
        } else
        if (templateName.startsWith("*.")) {
            // -- all files in specified directory
            String fileGlob = templateName;
            File files[] = FileTools.getFiles(directory, fileGlob, false);
            inputFileArray = files;
        } else {
            // -- single specific file
            File file = (directory != null)?
                new File(directory, templateName) :
                new File(templateName);
            inputFileArray = new File[] { file };
        }

        /* loop through input files */
        boolean singlePass = false;
        for (File inputFile : inputFileArray) {

            /* file must exist here */
            if (!inputFile.isFile()) {
                // -- not a file
                continue;
            }

            /* get input source from template file */
            String inputSource = CompiletimeVars.readTemplate(inputFile, packageName);
            if (StringTools.isBlank(inputSource)) {
                // -- inputSource not available?
                continue;
            }
    
            /* precheck output file */
            File outputFile = null;
            try {
                if (StringTools.isBlank(outputName) || outputName.equalsIgnoreCase("stdout")) {
                    // -- output to stdout (multi-pass ok)
                } else
                if (outputName.equalsIgnoreCase("inline")) {
                    // -- write back to template file (multi-pass ok)
                    overwrite  = true; // assume implied overwrite
                    outputFile = inputFile;
                } else {
                    // -- output to specified file (single-pass only)
                    outputFile = getOutputFile(directory, outputName, overwrite);
                    singlePass = true;
                }
            } catch (NoOverwriteException noe) {
                // -- outputFile exists, quietly ignore
                Print.sysPrintln(noe.getMessage());
                System.exit(0);
            } catch (IOException ioe) {
                Print.errPrintln("\nInvalid output file: " + ioe.getMessage());
                System.exit(1);
            }

            /* adjust source and write output */
            try {
                CompiletimeVars.writeOutputSource(inputSource,outputFile);
                if (singlePass) {
                    break;
                }
            } catch (IOException ioe) {
                Print.errPrintln("\nError writing file: " + ioe.getMessage());
                System.exit(1);
            }

        }

        /* success */
        System.exit(0);

    }
    
}
