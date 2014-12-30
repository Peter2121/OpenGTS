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
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/03/30  Martin D. Flynn
//     -Added support for 'User' login (in addition to 'Account' login)
//  2007/05/20  Martin D. Flynn
//     -Added support for "admin" user
//     -Added check for login to authorized host domain
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27 Martin D. Flynn
//     -Now set per-thread runtime properties for locale and 'From' email address.
//  2007/01/10 Martin D. Flynn
//     -Added customizable 'BASE_URI()' method
//  2008/02/27 Martin D. Flynn
//     -Added methods '_resolveFile' and '_writeFile' to resolve Image/JavaScript files.
//  2008/05/14 Martin D. Flynn
//     -Added runtime property for enabling cookie support
//  2008/09/01 Martin D. Flynn
//     -Added support for User 'getFirstLoginPageID()'
//  2009/02/20 Martin D. Flynn
//     -"RELOAD:XML" now also reloads the runtime config file.
//  2009/09/23 Martin D. Flynn
//     -An encoded hash of the password is now saved as a session attribute
//  2009/10/02  Martin D. Flynn
//     -Added support for creating pushpin markers with embedded text.
//  2009/12/16  Martin D. Flynn
//     -Added support to "ZONEGEOCODE" handler to allow using the GeocodeProvider
//      defined in the 'private.xml' file.
//  2010/09/09  Martin D. Flynn
//     -Added support for forwarding 'http' to 'https' (see "forwardToSecureAccess")
//  2010/11/29  Martin D. Flynn
//     -Look up main domain when subdomain is specified (see PrivateLabelLoader.getPrivateLabel)
//  2012/10/16  Martin D. Flynn
//     -Check "PROP_track_updateLastLoginTime_[user|account]" before updating lastLoginTime.
//  2013/08/27  Martin D. Flynn
//     -Added support for returning to SysAdmin/Manager originator (if applicable)
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;
import java.io.*;
import java.net.*;
import java.math.*;
import java.security.*;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.geocoder.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.page.AccountLogin;
import org.opengts.war.track.page.TrackMap;

public class Track
    extends CommonServlet
    implements Constants
{

    // ------------------------------------------------------------------------
    // TODO: 
    //  - HttpSessionBindingListener (interface) // Only applicable to HttpSession setAttribute/removeAttribute
    //      void valueBound(HttpSessionBindingEvent event)
    //      void valueUnbound(HttpSessionBindingEvent event)
    //  - HttpSessionListener (interface)
    //      void sessionCreated(HttpSessionEvent se)
    //      void sessionDestroyed(HttpSessionEvent se)

    private static class TrackSessionListener
        implements HttpSessionListener
    {
        private int sessionCount = 0;
        public TrackSessionListener() {
            this.sessionCount = 0;
        }
        public void sessionCreated(HttpSessionEvent se) {
            HttpSession session = se.getSession();
            synchronized (this) {
                this.sessionCount++;
            }
        }
        public void sessionDestroyed(HttpSessionEvent se) {
            HttpSession session = se.getSession();
            synchronized (this) {
                this.sessionCount--;
            }
        }
        public int getSessionCount() {
            int count = 0;
            synchronized (this) {
                count = this.sessionCount;
            }
            return count;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Debug: display incoming request */
    private static      boolean DISPLAY_REQUEST             = false;
    public static void setDisplayRequest(boolean display)
    {
        DISPLAY_REQUEST = display;
    }

    // ------------------------------------------------------------------------

    /* these must match the data response values in "jsmap.js" */
    public  static final String DATA_RESPONSE_LOGOUT        = "LOGOUT";
    public  static final String DATA_RESPONSE_ERROR         = "ERROR";
    public  static final String DATA_RESPONSE_PING_OK       = "PING:OK";
    public  static final String DATA_RESPONSE_PING_ERROR    = "PING:ERROR";

    // ------------------------------------------------------------------------

    /* global enable cookies */
    private static boolean REQUIRE_COOKIES                  = true;

    // ------------------------------------------------------------------------
    // Commands

    public static final String COMMAND_DEVICE_LIST          = "devlist";

    // ------------------------------------------------------------------------
    // custom page requests

    public static final String PAGE_LOGINFRAME              = "LOGINFRAME";
    public static final String PAGE_RELOAD                  = "RELOAD:XML";
    public static final String PAGE_COPYRIGHT               = "COPYRIGHT";
    public static final String PAGE_VERSION                 = "VERSION";
    public static final String PAGE_RUNSTATE                = "RUNSTATE";
    public static final String PAGE_STATISTICS              = "STATISTICS";
    public static final String PAGE_REVERSEGEOCODE          = "REVERSEGEOCODE";
    public static final String PAGE_GEOCODE                 = "ZONEGEOCODE";
    public static final String PAGE_AUTHENTICATE            = "AUTHENTICATE";
    public static final String PAGE_RULE_EVAL               = "RULE_EVAL";
    public static final String PAGE_DEBUG_PUSHPINS          = "DEBUG_PUSHPINS";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static long InitializedTime = 0L;

    /* get initialized time */
    public static long GetInitializedTime()
    {
        return InitializedTime;
    }

    /* static initializer */
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

        /* pre-init Base URI var */
        RequestProperties.TRACK_BASE_URI();

        /* enable cookies? "track.requireCookies" */
        if (RTConfig.hasProperty(DBConfig.PROP_track_requireCookies)) {
            REQUIRE_COOKIES = RTConfig.getBoolean(DBConfig.PROP_track_requireCookies,true);
        }

        /* initialized */
        InitializedTime = DateTime.getCurrentTimeSec();

    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String GetBaseURL(RequestProperties reqState)
    {
        return WebPageAdaptor.EncodeMakeURL(reqState, RequestProperties.TRACK_BASE_URI());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String DIRECTORY_CSS           = "/css/";
    private static final String DIRECTORY_JS            = "/js/";
    private static final String DIRECTORY_IMAGES        = "/images/";
    private static final String DIRECTORY_OPT           = "/opt/";

    private static boolean  trackRootDirInit = false;
    private static File     trackRootDir = null;

    /* resolve the specified file relative to the configFile directory */
    protected static File _resolveFile(String fileName)
    {

        /* initialize trackRootDir */
        if (!trackRootDirInit) {
            trackRootDirInit = true;
            trackRootDir = RTConfig.getServletContextPath(); // may return null
        }

        /* have a directory? */
        if (trackRootDir == null) {
            Print.logWarn("No Servlet Context Path!");
            return null;
        }

        /* have a file? */
        if (StringTools.isBlank(fileName)) {
            Print.logWarn("Specified file name is blank/null");
            return null;
        }

        /* remove prefixing '/' from filename */
        if (fileName.startsWith("/")) { 
            fileName = fileName.substring(1); 
        }

        /* assemble file path */
        File filePath = new File(trackRootDir, fileName);
        if (!filePath.isFile()) {
            Print.logInfo("File not found: " + filePath);
            return null;
        }

        /* return resolved file */
        return filePath;

    }
    
    /* write the specified file to the output stream */
    protected static void _writeFile(HttpServletResponse response, File file)
        throws IOException
    {
        //Print.logInfo("Found file: " + file);

        /* content mime type */
        CommonServlet.setResponseContentType(response, HTMLTools.getMimeTypeFromExtension(FileTools.getExtension(file)));

        /* copy file to output */
        OutputStream output = response.getOutputStream();
        InputStream input = null;
        try {
            input = new FileInputStream(file);
            FileTools.copyStreams(input, output);
        } catch (IOException ioe) {
            Print.logError("Error writing file: " + file + " " + ioe);
        } finally {
            if (input != null) { try{input.close();}catch(IOException err){/*ignore*/} }
        }
        output.close();
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // GET/POST entry point

    /* clear all session attributes, except HOST_PROPERTIES */
    private static void clearSessionAttributes(HttpServletRequest request)
    {
        RTProperties hp = (RTProperties)AttributeTools.getSessionAttribute(request, CommonServlet.HOST_PROPERTIES, null);
        AttributeTools.clearSessionAttributes(request); // invalidate
        if (hp != null) {
            AttributeTools.setSessionAttribute(request, CommonServlet.HOST_PROPERTIES, hp);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // GET/POST entry point

    /* GET request */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try {
            this._doWork_wrapper(false, request, response);
        } catch (Throwable th) {
            Print.logException("Unexpected error",th);
        }
    }

    /* POST request */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try {
            this._doWork_wrapper(true, request, response);
        } catch (Throwable th) {
            Print.logException("Unexpected error",th);
        }
    }

    /* handle POST/GET request */
    private void _doWork_wrapper(boolean isPost, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        AttributeTools.parseRTP(request);
        AttributeTools.parseMultipartFormData(request);

        /* get PrivateLabel instance for this URL */
        PrivateLabel privLabel = null;
        URL    requestURL      = null;
        String requestHostName = null;
        String requestUrlPath  = null;
        try {
            requestURL      = new URL(request.getRequestURL().toString());
            requestHostName = requestURL.getHost();
            requestUrlPath  = requestURL.getPath();
            privLabel       = (PrivateLabel)PrivateLabelLoader.getPrivateLabelForURL(requestURL);
        } catch (MalformedURLException mfue) {
            // invalid URL? (unlikely to occur)
            Print.logWarn("Invalid URL? " + request.getRequestURL());
            privLabel = (PrivateLabel)PrivateLabelLoader.getDefaultPrivateLabel();
        }

        /* PrivateLabel not found, look for other options */
        if (privLabel == null) {
            Print.logError("PrivateLabel not defined or contains errors!");
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML()); // MIME_PLAIN());
            PrintWriter out = response.getWriter();
            String pageName = AttributeTools.getRequestString(request, CommonServlet.PARM_PAGE, "");
            out.println("<HTML>");
            if (pageName.equals(PAGE_RELOAD)) {
                RTConfig.reload();
                PrivateLabelLoader.loadPrivateLabelXML();
                out.println("RELOADED ...");
            } else
            if (BasicPrivateLabelLoader.hasParsingErrors()) {
                out.println("ERROR encountered:<br>");
                out.println("'private.xml' contains syntax/parsing errors<br>");
                out.println("(see log files for additional information)<br>");
                out.println("<br>");
                out.println("Please contact the System Administrator<br>");
            } else
            if (!BasicPrivateLabelLoader.hasDefaultPrivateLabel()) {
                out.println("The specified URL is not allowed on this server:<br>");
                out.println("(Unrecognized URL and no default Domain has been defined)<br>");
            } else {
                // should not occur
                out.println("ERROR encountered:<br>");
                out.println("Invalid 'private.xml' configuration<br>");
                out.println("<br>");
                out.println("Please contact the System Administrator<br>");
            }
            out.println("</HTML>");
            out.close();
            return;
        }

        /* overriding host properties? */
        RTProperties hostProps = null;
        // -- check for "&lfid=" host properties id
        String hostPropID = AttributeTools.getRequestString(request, CommonServlet.HOST_PROPERTIES_ID, null); // "lfid"/"laf"?
        if (StringTools.isBlank(hostPropID)) {
            // -- get previous host properties
            hostProps = (RTProperties)AttributeTools.getSessionAttribute(request, CommonServlet.HOST_PROPERTIES, null);
        } else
        if (hostPropID.equalsIgnoreCase(CommonServlet.DEFAULT_HOST_PROPERTIES_ID) || 
            hostPropID.equalsIgnoreCase("x") ) {
            // -- explicit reset to default
            hostProps = null;
            AttributeTools.setSessionAttribute(request, CommonServlet.HOST_PROPERTIES, null); // clear
        } else {
            // -- get specified host properties from ID
            hostProps = Resource.getPrivateLabelPropertiesForHost(hostPropID, null); // may return null
            if (hostProps != null) {
                hostProps.setString(CommonServlet.HOST_PROPERTIES_ID, hostPropID);
            }
            AttributeTools.setSessionAttribute(request, CommonServlet.HOST_PROPERTIES, hostProps); // clears if null
        }
        // -- if no explicit host properties, try resources
        if (hostProps == null) {
            //Print.logInfo("Looking up host properties by host/url: " + requestHostName + ", " + requestUrlPath);
            hostProps = Resource.getPrivateLabelPropertiesForHost(requestHostName, requestUrlPath);
        }
        // -- found?
        //if (hostProps != null) { Print.logInfo("Found host/url host properties"); }

        /* check for DebugPushpins */
        boolean debugPP = false;
        if (AttributeTools.hasRequestAttribute(request,EventData.DEBUG_PUSHPINS)) {
            debugPP = AttributeTools.getRequestBoolean(request,EventData.DEBUG_PUSHPINS, false);
            AttributeTools.setSessionBoolean(request, EventData.DEBUG_PUSHPINS[0], debugPP);
        } else
        if (AttributeTools.getSessionBoolean(request,EventData.DEBUG_PUSHPINS,false)) {
            debugPP = true;
        }
        if (debugPP) {
            if (hostProps == null) {
                hostProps = new RTProperties();
            }
            hostProps.setBoolean(EventData.DEBUG_PUSHPINS[0], true);
            Print.logInfo("Debugging Pushpins ...");
        } else {
            //Print.logInfo("Not Debugging Pushpins ...");
        }

        /* display PrivateLabel */
        try {
            privLabel.pushRTProperties();
            if (hostProps != null) {
                RTConfig.pushTemporaryProperties(hostProps);
            }
            this._doWork(isPost, request, response, privLabel);
        } finally {
            //if (hostProps != null) {
            //    RTConfig.popTemporaryProperties(hostProps);
            //}
            //privLabel.popRTProperties();
            RTConfig.popAllTemporaryProperties();
        }

    }

    /* handle POST/GET request */
    private void _doWork(
        boolean isPost, HttpServletRequest request, 
        HttpServletResponse response, 
        PrivateLabel privLabel)
        throws ServletException, IOException
    {

        /* URL query string */
        String requestURL  = StringTools.trim(request.getRequestURL().toString());
        String queryString = StringTools.trim(request.getQueryString());

        /* was logged-in from SysAdmin? */
        // cache beore session attributes are cleared
        boolean saLogin = RequestProperties.isLoggedInFromSysAdmin(request,privLabel);
        String saAcctID = saLogin? AttributeTools.getSessionString(request,Constants.PARM_SA_RELOGIN_ACCT,"") : null;
        String saUserID = saLogin? AttributeTools.getSessionString(request,Constants.PARM_SA_RELOGIN_USER,"") : null;
        long  saLoginTS = saLogin? AttributeTools.getSessionLong(  request,Constants.PARM_SA_RELOGIN_SESS,0L) : 0L;

        /* secure (SSL) only? */
        boolean isSecure = request.isSecure();
        if (!isSecure && privLabel.getBooleanProperty(PrivateLabel.PROP_Track_forwardToSecureAccess,false)) {
            if (StringTools.startsWithIgnoreCase(requestURL,"http:")) {
                // Create new URL
                URIArg uri = new URIArg(requestURL);
                uri.setProtocol("https");
                int uriPort = uri.getPort();
                if (uriPort == 8080) {
                    uri.setPort(8443);
                } else
                if (uriPort == 80) {
                    uri.setPort(443);
                }
                // append query string
                String secureURL = uri.toString();
                if (!StringTools.isBlank(queryString)) {
                    secureURL += "?" + queryString;
                }
                // Write forward HTML:
                //  <html>
                //  <meta HTTP-EQUIV="refresh" CONTENT="1; URL=http://example.com/link.html"></meta>
                //  </html>
                Print.logInfo("HTTPS Forward: " + secureURL);
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
                PrintWriter out = response.getWriter();
                out.println("<HTML>");
                out.println("<META HTTP-EQUIV=\"refresh\" CONTENT=\"1; URL="+secureURL+"\"></META>");
                out.println("</HTML>");
                out.close();
                return;
            } else {
                // nut secure, but URL does not start with "http:"
                Print.logError("Access is not secure, however link is not prefixed with 'http'!");
                return;
            }
        }

        /* response character set */
        ////response.setCharacterEncoding("UTF-8");         

        /* action request */
        String pageName     = AttributeTools.getRequestString(request, CommonServlet.PARM_PAGE    , ""); // query only
        String cmdName      = AttributeTools.getRequestString(request, CommonServlet.PARM_COMMAND , ""); // query only ("page_cmd")
        String cmdArg       = AttributeTools.getRequestString(request, CommonServlet.PARM_ARGUMENT, ""); // query only

        /* adjust page request */
        if (cmdName.equals(Constants.COMMAND_LOGOUT) || pageName.equals(PAGE_LOGIN)) {
            Track.clearSessionAttributes(request); // start with a clean slate
            pageName = PAGE_LOGIN;
        }

        /* currently logged-in account/user */
        String loginAcctID  = (String)AttributeTools.getSessionAttribute(request, Constants.PARM_ACCOUNT  , "");   // session only
        String loginUserID  = (String)AttributeTools.getSessionAttribute(request, Constants.PARM_USER     , "");   // session only
        boolean isLoggedIn  = !StringTools.isBlank(loginAcctID);

        /* account/user */
        String userEmail    = (String)AttributeTools.getRequestAttribute(request, Constants.PARM_USEREMAIL, "");   // session or query
        String accountID    = (String)AttributeTools.getRequestAttribute(request, Constants.PARM_ACCOUNT  , "");   // session or query
        String userID       = (String)AttributeTools.getRequestAttribute(request, Constants.PARM_USER     , "");   // session or query
        String enteredPass  =         AttributeTools.getRequestString   (request, Constants.PARM_PASSWORD , null); // query only
        String entEncPass   =         AttributeTools.getRequestString   (request, Constants.PARM_ENCPASS  , null); // query only

        /* DemoLogin? */
        if (StringTools.isBlank(accountID) && requestURL.endsWith(Constants.DEFAULT_DEMO_LOGIN_URI)) {
            accountID   = Account.GetDemoAccountID();
            userID      = "";
            enteredPass = null;
            entEncPass  = null;
            Print.logInfo("Demo Login ["+accountID+"] ...");
        }

        /* validate AccountID/UserID */
        // the following prevents HTML/JavaScript injection via "accountID" or "userID"
        if ((privLabel == null) || privLabel.globalValidateIDs()) {
            //Print.logInfo("Validating account/user IDs");
            if (!AccountRecord.isValidID(accountID)) {
                accountID = "";
            }
            if (!AccountRecord.isValidID(userID)) {
                userID = "";
            }
        } else {
            Print.logWarn("account/user ID validation disabled!!");
        }

        /* encoded password */
        if (!StringTools.isBlank(entEncPass)) {
            // already have encoded password
            enteredPass = null; // clear entered password in favor of previously encoded password
        } else {
            entEncPass = Account.encodePassword(privLabel,enteredPass); // may be null
            if (entEncPass == null) { entEncPass = ""; }
        }

        /* store/restore password (PARM_ENCPASS) */
        if (StringTools.isBlank(entEncPass)) {
            // blank password, check for restore
            // (this doesn't really provide the feature expected.  what is wanted is an auto-re-login
            // if the session expires, but the following cannot provide that since if the session has
            // expired, the 'PARM_RESTOREPW' attribute will also be gone.)
            if (AttributeTools.getSessionBoolean(request,Constants.PARM_RESTOREPW,false)) {
                //entEncPass = (String)AttributeTools.getSessionAttribute(request, Constants.PARM_ENCPASS, "");
            }
        } else {
            // non-blank password, check to see if we should indicate to subsequently reload the pwd from the session
            if (queryString.indexOf("&password=") > 0) {
                // the password was explicity specified on the query string
                // TODO: this should really indicate that the "password=x" should always be included on
                // the subsequent URL traversal links (this is currently not supported).
                AttributeTools.setSessionBoolean(request, Constants.PARM_RESTOREPW, true); // save in session
                // Should this indicate that the password should be stored in a client cookie?
            }
        }

        /* session cached device/group */
        String deviceID = (String)AttributeTools.getRequestAttribute(request, Constants.PARM_DEVICE   , "");
        String groupID  = (String)AttributeTools.getRequestAttribute(request, Constants.PARM_GROUP    , "");
        // the following prevents HTML/JavaScript injection
        if ((privLabel == null) || privLabel.globalValidateIDs()) {
            //Print.logInfo("Validating device/group IDs");
            if (!AccountRecord.isValidID(deviceID)) {
                deviceID = "";
            }
            if (!AccountRecord.isValidID(groupID)) {
                groupID = "";
            }
        } else {
            Print.logWarn("device/group ID validation disabled!!");
        }

        /* log-in ip address */
        String ipAddr = request.getRemoteAddr();

        // Options for looking up an AccountID/UserID
        //  1) "accountID"/"userID" explicitly supplied.
        //  2) "accountID" supplied, UserID implied (blank)
        //  3) "userEmail" supplied, from which AccountID/UserID is implied

        /* debug info */
        if (DISPLAY_REQUEST || RTConfig.isDebugMode()) {
            try {
                URL url = new URL(requestURL);
                Print.logInfo("Request : " + (isPost?"POST":"GET"));
                Print.logInfo("URL Path: " + url.getPath());
                Print.logInfo("URL File: " + url.getFile());
            } catch (MalformedURLException mfue) {
                //
            }
            Print.logInfo("Context Path: " + request.getContextPath());
            Print.logInfo("[" + ipAddr + "] URL: " + requestURL + " " + queryString);
            for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                String val = request.getParameter(key);
                Print.logInfo("Request Key:" + key + ", Value:" + val);
            }
        }

        /* check for file resource requests */
        String servletPath = request.getServletPath();
        if ((servletPath != null) && (
             servletPath.startsWith(DIRECTORY_CSS)          || 
             servletPath.startsWith(DIRECTORY_JS)           || 
             servletPath.startsWith(DIRECTORY_IMAGES)       ||
             servletPath.startsWith(DIRECTORY_OPT)   
             )) {
            // If this occurs, it means that the servlet context redirected a request for a file 
            // resource to this servlet.  We attempt here to find the requested file, and write 
            // it to the output stream.
            File filePath = Track._resolveFile(servletPath);
            if (filePath != null) {
                // file was found, write it to the output stream
                Print.logInfo("Serving file: " + filePath);
                Track._writeFile(response, filePath);
            } else {
                // the file was not found
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                PrintWriter out = response.getWriter();
                out.println("Invalid file specification");
                out.close();
            }
            return;
        }

        // --- initialize the RequestProperties 
        RequestProperties reqState = new RequestProperties();
        reqState.setBaseURI(RequestProperties.TRACK_BASE_URI());
        reqState.setHttpServletResponse(response);
        reqState.setHttpServletRequest(request);
        reqState.setIPAddress(ipAddr);
        reqState.setCommandName(cmdName);
        reqState.setCommandArg(cmdArg);
        reqState.setCookiesRequired(REQUIRE_COOKIES);  /* enable cookies? */

        /* locale override */
        String localeStr = AttributeTools.getRequestString(request, Constants.PARM_LOCALE, null);           // query only
        if (StringTools.isBlank(localeStr)) {
            localeStr = (String)AttributeTools.getSessionAttribute(request, Constants.PARM_LOCALE, "");     // session only
        }
        reqState.setLocaleString(localeStr);

        /* store the RequestProperties instance in the current request to allow access from TagLibs */
        request.setAttribute(PARM_REQSTATE, reqState);

        /* content only? (obsolete?) */
        int contentOnly = 0;
        if (AttributeTools.hasRequestAttribute(request,PARM_CONTENT)) {
            // key explicitly specified in the request (obsolete)
            contentOnly = AttributeTools.getRequestInt(request,PARM_CONTENT, 0);
            AttributeTools.setSessionInt(request,PARM_CONTENT,contentOnly); // save in session
        } else {
            // get last value stored in the session
            contentOnly = AttributeTools.getSessionInt(request,PARM_CONTENT, 0);
        }
        reqState.setPageFrameContentOnly(contentOnly != 0);

        /* invalid url (no private label defined for this URL host) */
        if (privLabel == null) {
            // This would be an abnormal situation, English should be acceptable here
            if (pageName.equals(PAGE_AUTHENTICATE)) {
                Track._writeAuthenticateXML(response, false);
            } else {
                String invalidURL = "Invalid URL";
                Track.writeErrorResponse(reqState, invalidURL);
            }
            return;
        }
        reqState.setPrivateLabel(privLabel);
        I18N i18n = privLabel.getI18N(Track.class);
        String bplName = privLabel.getName();
        long nowTimeSec = DateTime.getCurrentTimeSec();

        /* authentication request */
        boolean authRequest = (pageName.equals(PAGE_AUTHENTICATE) && 
            privLabel.getBooleanProperty(PrivateLabel.PROP_Track_enableAuthenticationService,false));

        /* display Version info */
        if (pageName.equals(PAGE_COPYRIGHT)) {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
            PrintWriter out = response.getWriter();
            out.println(org.opengts.Version.getInfo());
            out.close();
            return;
        }

        /* display Version */
        if (pageName.equals(PAGE_VERSION)) {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
            PrintWriter out = response.getWriter();
            out.println(org.opengts.Version.getVersion());
            out.close();
            return;
        }

        /* display run-state */ // 2.5.2-B40
        // -- This feature is used for testing the run status of the servlet container
        if (pageName.equals(PAGE_RUNSTATE)) {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
            PrintWriter out = response.getWriter();
            out.println("OK");
            // TODO: other runtime status could be displayed here
            out.close();
            return;
        }

        /* reload PrivateLabel XML */
        // This allow making changes directly within the "$CATALINA_HOME/webapps/track/" to 
        // various 'private.xml' or runtime config files (ie. custom.conf, etc), then force a
        // reload of the changes without having to start/stop Tomcat. Any currently logged-in
        // users will still remain logged-in.
        // WARNING: This has been known to fail in some cases and not properly reload the 
        // runtime configuration files.  This should only be used for development purposes.
        if (pageName.equals(PAGE_RELOAD)) {
            RTConfig.reload();
            PrivateLabelLoader.loadPrivateLabelXML();
            if (cmdName.equalsIgnoreCase("short")) {
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                PrintWriter out = response.getWriter();
                out.println("RELOADED");
                out.close();
            } else {
                Track.writeMessageResponse(reqState, "PrivateLabel XML reloaded"); // English only
            }
            return;
        }

        /* special "loginFrame" request */
        if (requestURL.endsWith(RequestProperties._HTML_LOGIN_FRAME) || 
            requestURL.endsWith(RequestProperties._HTML_LOGIN)       ||
            pageName.equalsIgnoreCase(PAGE_LOGINFRAME)                  ) {
            String indexJSP = "/jsp/loginFrame.jsp";
            Print.logInfo("Dispatching to " + indexJSP);
            RequestDispatcher rd = request.getRequestDispatcher(indexJSP);
            if (rd != null) {
                try {
                    rd.forward(request, response);
                    return;
                } catch (ServletException se) {
                    Print.logException("JSP error: " + indexJSP, se);
                    // continue below
                }
            } else {
                Print.logError("******* RequestDispatcher not found for URI: " + indexJSP);
            }
        }

        /* custom text marker */
        // Example:
        //   Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,8&text=Demo2&rc=red&fc=yellow
        // Options:
        //   icon=<PathToImageIcon>                                 - icon URI (relative to "/track/")
        //   frame=<XOffset>,<YOffset>,<Width>,<Height>,<FontPt>    - frame definition
        //   fill=<FillColor>                                       - frame background color
        //   border=<BorderColor>                                   - frame border color
        //   text=<ShortText>                                       - string text
        //   arrow=<Heading>                                        - double heading
        if (isLoggedIn && requestURL.endsWith(Constants.DEFAULT_MARKER_URI)) { // make sure someone is logged-in
            // box28_white.png  : 3,2,21,16,8
            // label47_fill.png : 3,2,42,13,9
            String icon  = StringTools.blankDefault(AttributeTools.getRequestString(request,PushpinIcon.MARKER_ARG_ICON,""),"/images/pp/label47_fill.png");
            String frStr = AttributeTools.getRequestString(request, PushpinIcon.MARKER_ARG_FRAME, "");
            String fs[]  = StringTools.split(frStr,',');
            int    fr[]  = StringTools.parseInt(fs,0); 
            Color  fillC = ColorTools.parseColor(AttributeTools.getRequestString(request,PushpinIcon.MARKER_ARG_FILL_COLOR  ,""),(Color)null);
            Color  bordC = ColorTools.parseColor(AttributeTools.getRequestString(request,PushpinIcon.MARKER_ARG_BORDER_COLOR,""),(Color)null);
            Color  foreC = ColorTools.parseColor(AttributeTools.getRequestString(request,PushpinIcon.MARKER_ARG_COLOR       ,""),(Color)null);
            int    X     = ((fr != null) && (fr.length > 0) && (fr[0] > 0))? fr[0] :  3; // X-Offset [ 1] 
            int    Y     = ((fr != null) && (fr.length > 1) && (fr[1] > 0))? fr[1] :  2; // Y-Offset [14]
            int    W     = ((fr != null) && (fr.length > 2) && (fr[2] > 0))? fr[2] : 42; // Width    [29]
            int    H     = ((fr != null) && (fr.length > 3) && (fr[3] > 0))? fr[3] : 13; // Height   [ 9]
            int    F     = ((fr != null) && (fr.length > 4) && (fr[4] > 0))? fr[4] :  9; // FontSize [10]
            String Fname = ((fs != null) && (fs.length > 5) && !StringTools.isBlank(fs[5]))? fs[5] :  null; // FontName [SanSerif]
            String text  = AttributeTools.getRequestString(request, PushpinIcon.MARKER_ARG_TEXT , "");
            double arrow = AttributeTools.getRequestDouble(request, PushpinIcon.MARKER_ARG_ARROW, -1.0); 
            File iconPath = Track._resolveFile(icon);
            if (iconPath != null) {
                //Print.logInfo("Loading Marker Icon: " + iconPath);
                PushpinIcon.TextIcon ti = new PushpinIcon.TextIcon(iconPath, X, Y, W, H, F, Fname);
                if (fillC != null) { ti.setFillColor(fillC); }
                if (bordC != null) { ti.setBorderColor(bordC); }
                if (foreC != null) { ti.setForegroundColor(foreC); }
                RenderedImage image = ti.createImage(text, arrow);
                if (image != null) {
                    response.setContentType(HTMLTools.MIME_PNG());
                    OutputStream out = response.getOutputStream();
                    ImageIO.write(image, "png", out);
                    out.close();
                    return;
                } else {
                    // error?
                    Print.logError("Unable to render image: " + icon);
                }
            } else {
                Print.logError("Unable to resolve image file: " + icon);
            }
            // the file was not found
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
            PrintWriter out = response.getWriter();
            out.println("Invalid file: " + icon);
            out.close();
            return;
        }

        /* return EventData attachment */
        // Example:
        //   Attach.jpg?d=DEVICE&ts=TIMSTAMP&sc=STATUSCODE
        // Options:
        //   d=<Device>         - device
        //   ts=<Timestamp>     - timestamp
        //   sc=<StatusCode>    - status code
        int att = isLoggedIn? requestURL.indexOf(Constants.DEFAULT_ATTACH_URI+".") : -1; // make sure someone is logged-in
        if (att >= 0) {
            String ext = requestURL.substring(att + (Constants.DEFAULT_ATTACH_URI+".").length());
            String acc = loginAcctID;
            String dev = AttributeTools.getRequestString(request, "d" , "");
            long   ts  = AttributeTools.getRequestLong  (request, "ts", 0L);
            int    sc  = AttributeTools.getRequestInt   (request, "sc", StatusCodes.STATUS_NONE);
            // get EventData record
            EventData ev = null;
            try {
                EventData.Key evKey = new EventData.Key(acc, dev, ts, sc);
                ev = evKey.exists()? evKey.getDBRecord(true) : null;
            } catch (DBException dbe) {
                Print.logError("Unable to update EventData with attachment: " + dbe);
            }
            // EventData record not found
            if (ev == null) {
                // EventData not found
                Print.logError("EventData record not found: " + acc + "," + dev + "," + ts + "," + sc);
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                PrintWriter out = response.getWriter();
                out.println("EventData record not found.");
                out.close();
                return;
            }
            // EventData record does not contain an attachment
            if (!ev.hasAttachData()) {
                // no attachment (empty response)
                String mimeType = HTMLTools.getMimeTypeFromExtension(ext, HTMLTools.MIME_PLAIN());
                response.setContentType(mimeType);
                OutputStream out = response.getOutputStream();
                // write nothing (empty response)
                out.close();
                return;
            }
            // return EventData attachment
            String attachType   = ev.getAttachType();
            byte   attachData[] = ev.getAttachData();
            String mimeType     = HTMLTools.getMimeTypeFromExtension(ext, attachType);
            response.setContentType(mimeType);
            OutputStream out = response.getOutputStream();
            out.write(attachData);
            out.close();
            return;
        }

        /* statistics */
        if (pageName.equals(PAGE_STATISTICS) && !cmdName.equals("") && !cmdArg.equals("")) {
            try{
                MethodAction ma = new MethodAction(DBConfig.PACKAGE_OPT_AUDIT_ + "Statistics", "getStats", String.class, String.class);
                String stats = (String)ma.invoke(cmdName, cmdArg);
                if (stats != null) {
                    CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                    PrintWriter out = response.getWriter();
                    out.println(stats);
                    out.close();
                    return;
                }
            } catch (Throwable th) {
                // ignore
                //Print.logException("Statistics",th);
            }
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
            PrintWriter out = response.getWriter();
            out.println("");
            out.close();
            return;
        }

        /* offline? */
        String glbOflMsg  = PrivateLabel.GetGlobalOfflineMessage();
        String ctxOflMsg  = PrivateLabel.GetContextOfflineMessage();
        boolean isOffline = (glbOflMsg != null) || ((ctxOflMsg != null) && !AccountRecord.isSystemAdminAccountID(accountID));
        if (isOffline || pageName.equals(PAGE_OFFLINE)) {
            if (isOffline) {
                Print.logWarn("Domain Offline: " + request.getRequestURL());
            }
            Track.clearSessionAttributes(request);
            String offlineMsg = (ctxOflMsg != null)? ctxOflMsg : glbOflMsg;
            pageName = PAGE_OFFLINE;
            WebPage offlinePage = privLabel.getWebPage(pageName); // may return null
            if (offlinePage != null) {
                reqState.setPageName(pageName);
                reqState.setPageNavigationHTML(offlinePage.getPageNavigationHTML(reqState));
                offlinePage.writePage(reqState, offlineMsg);
            } else {
                if (StringTools.isBlank(offlineMsg)) {
                    offlineMsg = i18n.getString("Track.offline","The system is currently offline for maintenance\nPlease check back later.");
                }
                Track.writeMessageResponse(reqState, offlineMsg);
            }
            return;
        }

        /* explicit logout? */
        if (pageName.equals(PAGE_LOGIN)) {
            // check for SysAdmin/Manager Accounts-Login return
            try {
                for (;;) {
                    // Originally logged-in from SysAdnin/Manager? (EXPERIMENTAL)
                    // Used to support returning to the "System Accounts" page if the 
                    // SysAdmin/Manager had logged-in to this account using the "Login" 
                    // button on the "System Accounts" page.
                    if (!saLogin) {
                        // not logged-in from SysAdmin/Manager
                        //Print.logInfo("Not logged in from SysAdmin/Manager");
                        break;
                    } else
                    if (StringTools.isBlank(saAcctID)) {
                        // not logged-in from SysAdmin/Manager
                        Print.logWarn("Originator SysAdmin/Manager AccountID id blsnk");
                        break;
                    }
                    // return enabled?
                    if (!privLabel.isSystemAccountsLoginReturnEnabled(saAcctID)) {
                        // return from SysAccountsLogin not enabled
                        //Print.logWarn("SysAdmin/Manager Account login return disabled");
                        break;
                    }
                    // return timeout
                    long rtnTimeoutSec = privLabel.getSystemAccountsLoginReturnTimeout(saAcctID);
                    if ((rtnTimeoutSec > 0L) && ((saLoginTS + rtnTimeoutSec) < DateTime.getCurrentTimeSec())) {
                        // timeout
                        Print.logWarn("SysAdmin/Manager Account login return timeout");
                        break;
                    }
                    // get account
                    Account saAcct = Account.getAccount(saAcctID);
                    if (saAcct == null) {
                        Print.logWarn("Account not found: " + saAcctID);
                        break;
                    }
                    // get user
                    User saUser = null;
                    if (!StringTools.isBlank(saUserID)) {
                        saUser = User.getUser(saAcct, saUserID);
                        if ((saUser == null) && !User.isAdminUser(saUserID)) {
                            Print.logWarn("User not found: " + saAcctID + "/" + saUserID);
                            break; // UserID specified, but not found
                        }
                    }
                    // get password
                    String saPasswd = (saUser != null)? 
                        saUser.getDecodedPassword(null) : 
                        saAcct.getDecodedPassword(null);
                    // construct URL
                    URIArg url = new URIArg(reqState.getBaseURI());
                    url.addArg(Constants.PARM_ACCOUNT , saAcctID);
                    url.addArg(Constants.PARM_USER    , saUserID);
                    url.addArg(Constants.PARM_PASSWORD, StringTools.blankDefault(saPasswd,""));
                    url.addArg(CommonServlet.PARM_PAGE, Constants.PAGE_SYSADMIN_ACCOUNTS);
                    Print.logInfo("ReLogin URL: " + url);
                    // dispatch
                    RequestDispatcher rd = request.getRequestDispatcher(url.toString());
                    rd.forward(request, response);
                    return;
                }
            } catch (Throwable th) {
                Print.logError("Unable to dispatch to '"+saAcctID+"' login: " + th);
            }
            // standard logout
            if (!loginAcctID.equals("")) { 
                Print.logInfo("Logout: " + loginAcctID); 
            }
            String pleaseLogin = i18n.getString("Track.pleaseLogin","Please Login"); // UserErrMsg
            Track._displayLogin(reqState, pleaseLogin, false);
            return;
        }

        /* get requested page */
        WebPage trackPage = privLabel.getWebPage(pageName); // may return null
        if (trackPage != null) {
            reqState.setPageName(pageName);
        }

        /* check for page that does not require a login */
        if ((trackPage != null) && !trackPage.isLoginRequired()) {
            reqState.setPageNavigationHTML(trackPage.getPageNavigationHTML(reqState));
            trackPage.writePage(reqState, "");
            return;
        }

        // --- verify that user is logged in
        // If not logged-in, the login page will be displayed

        /* email id */
        String emailID = null;
        if (privLabel.getAllowEmailLogin()) {
            String email = !StringTools.isBlank(userEmail)? userEmail : userID;
            if (email.indexOf("@") > 0) {
                emailID = email;
            } else
            //if (!privLabel.getAccountLogin() && Account.isSystemAdminAccountID(email))
            if (!privLabel.getAccountLogin() && StringTools.isBlank(accountID)) {
                // TODO: may want to make this a configurable option
                emailID   = null;
                accountID = email;
                if (!privLabel.getUserLogin() /*&& StringTools.isBlank(userID)*/) {
                    userID = User.getAdminUserID(); // TODO: default account user?
                }
            }
        }

        /* account-id not specified */
        if (StringTools.isBlank(accountID)) {
            String dftAcctID = privLabel.getDefaultLoginAccount(); // may be blank
            if (!StringTools.isBlank(dftAcctID)) {
                accountID = dftAcctID;
            } else
            if (!privLabel.getAccountLogin() || !StringTools.isBlank(emailID)) {
                // attempt to look up account/user from user contact email address
                try {
                    // look up in User table
                    User user = User.getUserForContactEmail(null,emailID);
                    if (user != null) {
                        emailID   = null;
                        accountID = user.getAccountID();
                        userID    = user.getUserID();
                        // account/user revalidated below ...
                        Print.logInfo("Found contact email User: " + emailID + " ==> " + accountID + "/" + userID);
                    } else {
                        // not in User table, try Account table
                        java.util.List<String> acctList = Account.getAccountIDsForContactEmail(emailID);
                        if (!ListTools.isEmpty(acctList)) {
                            emailID   = null;
                            accountID = acctList.get(0);
                            userID    = User.getAdminUserID();
                            Print.logInfo("Found contact email Account: " + emailID + " ==> " + accountID + "/" + userID);
                        } else {
                            Print.logWarn("User Email address not found: " + emailID);
                        }
                    }
                } catch (Throwable th) { // DBException
                    // ignore
                    Print.logError("EMail lookup error: ["+emailID+"] " + th);
                }
            }
            if (StringTools.isBlank(accountID)) { // still blank?
                // This is NOT an error, only an indication that the account session may have expired!
                //Print.logInfo("[%s] Account/User not logged in (session expired?) ...", privLabel.getName());
                Track.clearSessionAttributes(request);
                if (authRequest) {
                    Track._writeAuthenticateXML(response, false);
                } else
                if ((trackPage instanceof TrackMap)                 &&
                    (cmdName.equals(TrackMap.COMMAND_MAP_UPDATE) || 
                     cmdName.equals(TrackMap.COMMAND_DEVICE_PING)  )  ) {
                    // A map has requested an update, and the user is not logged-in
                    PrintWriter out = response.getWriter();
                    CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                    out.println(DATA_RESPONSE_LOGOUT);
                } else {
                    String enterAcctPass = i18n.getString("Track.enterAccount","Please enter Login/Authentication"); // UserErrMsg
                    Track._displayLogin(reqState, enterAcctPass, false);
                }
                return;
            }
        } else
        if (!StringTools.isBlank(emailID)) {
            // attempt to look up user from account and contact email address
            try {
                User user = User.getUserForContactEmail(accountID,emailID);
                if (user != null) {
                    emailID   = null;
                    userID    = user.getUserID();
                    // account/user revalidated below ...
                } else {
                    Print.logWarn("User Email address not found: " + accountID + "/" + emailID);
                }
            } catch (Throwable th) { // DBException
                // ignore
                Print.logError("EMail lookup error: " + th);
            }
        }

        /* login from "sysadmin"? */
        String relogin = AttributeTools.getRequestString(request, Constants.PARM_SA_RELOGIN, "");
        if (!StringTools.isBlank(relogin)) {
            AttributeTools.setSessionAttribute(request, Constants.PARM_SA_RELOGIN, relogin);
        }
        boolean isSysadminRelogin = RequestProperties.isLoggedInFromSysAdmin(request,privLabel);

        /* load account */
        Account account = null;
        try {

            account = Account.getAccount(accountID);
            if (account == null) {
                Print.logInfo("Account does not exist: " + accountID);
                Track.clearSessionAttributes(request);
                if (authRequest) {
                    Track._writeAuthenticateXML(response, false);
                } else {
                    String invLoginText = reqState.getShowPassword()?
                        i18n.getString("Track.invalidLoginPass","Invalid Login/Password") :  // UserErrMsg
                        i18n.getString("Track.invalidLogin","Invalid Login");  // UserErrMsg
                    Track._displayLogin(reqState, invLoginText, true);
                }
                Print.logInfo("Login[Failed]: Domain="+bplName + " Account="+accountID + " User="+userID + " Time="+nowTimeSec + " IPAddr="+ipAddr);
                return;
            } else
            if (!account.isActive() && !isSysadminRelogin) {
                Print.logInfo("Account is inactive: " + accountID);
                Track.clearSessionAttributes(request);
                if (authRequest) {
                    Track._writeAuthenticateXML(response, false);
                } else {
                    Track._displayLogin(reqState, i18n.getString("Track.inactiveAccount","Inactive Account"), true);  // UserErrMsg
                }
                Print.logInfo("Login[Inactive]: Domain="+bplName + " Account="+accountID + " User="+userID + " Time="+nowTimeSec + " IPAddr="+ipAddr);
                return;
            } else
            if (account.isExpired() && !isSysadminRelogin) {
                Print.logInfo("Account has expired: " + accountID);
                Track.clearSessionAttributes(request);
                if (authRequest) {
                    Track._writeAuthenticateXML(response, false);
                } else {
                    Track._displayLogin(reqState, i18n.getString("Track.expiredAccount","Expired Account"), true);  // UserErrMsg
                }
                Print.logInfo("Login[Inactive]: Domain="+bplName + " Account="+accountID + " User="+userID + " Time="+nowTimeSec + " IPAddr="+ipAddr);
                return;
            }

            // check that the Account is authorized for this host domain
            if (privLabel.isRestricted() && !isSysadminRelogin) {
                String acctDomain = account.getPrivateLabelName();
                String domainName = privLabel.getDomainName();
                String hostName   = privLabel.getHostName();  // never null (may be PrivateLabel.DEFAULT_HOST)
                for (;;) {
                    if (!StringTools.isBlank(acctDomain)) {
                        if ((domainName != null) && acctDomain.equals(domainName)) {
                            break; // account domain matches alias name
                        } else
                        if ((hostName != null) && acctDomain.equals(hostName)) {
                            break; // account domain matches host name
                        } else
                        if (acctDomain.equals(BasicPrivateLabel.ALL_HOSTS)) {
                            break; // account domain explicitly states it is valid in all domains
                        }
                    }
                    // if we get here, we've failed the above tests
                    Print.logInfo("Account not authorized for this host: " + accountID + " ==> " + hostName);
                    if (authRequest) {
                        Track._writeAuthenticateXML(response, false);
                    } else {
                        Track._displayLogin(reqState, i18n.getString("Track.invalidHost","Invalid Login Host"), true); // UserErrMsg
                    }
                    return;
                }
            }

        } catch (DBException dbe) {

            // Internal error
            Track.clearSessionAttributes(request);
            Print.logException("Error reading Account: " + accountID, dbe);
            if (authRequest) {
                Track._writeAuthenticateXML(response, false);
            } else {
                Track.writeErrorResponse(reqState, i18n.getString("Track.errorAccount","Error reading Account"));
            }
            return;

        }

        /* default to 'admin' user */
        if (StringTools.isBlank(userID)) {
            userID = account.getDefaultUser();
            if (StringTools.isBlank(userID)) { 
                userID = (privLabel != null)? privLabel.getDefaultLoginUser() : User.getAdminUserID(); 
            }
        }

        /* different account (or not yet logged in) */
        boolean wasLoggedIn = (loginAcctID.equals(accountID) && loginUserID.equals(userID));
        if (!wasLoggedIn && !isSysadminRelogin) {
            // clear old session variables and continue
            Track.clearSessionAttributes(request);
        }

        /* validate account/user/password */
        User user = null;
        try {

            /* lookup specified UserID */
            boolean loginOK = true;
            user = User.getUser(account, userID);
            if (user != null) {
                // we found a valid user
                //Print.logInfo("Found User: " + userID);
            } else
            if (User.isAdminUser(userID)) {
                // logging in as the 'admin' user, and we don't have an explicit user record
                //Print.logInfo("Explicit Admin user not found: " + userID);
            } else {
                Print.logInfo("Invalid User: " + accountID + "/" + userID);
                account = null;
                loginOK = false;
            }

            // if we found the account/user, check the password (if not already logged in)
            if (loginOK && !wasLoggedIn) {
                if (user != null) {
                    // standard password check
                    boolean validLogin = false;
                    if (enteredPass != null) {
                        String decPass = enteredPass;
                        validLogin = user.checkPassword(privLabel,decPass);
                    } else
                    if (entEncPass != null) {
                        // TODO: should support checking MD5 encoded passwords
                        String decPass = Account.decodePassword(privLabel,entEncPass); // may be null
                        validLogin = user.checkPassword(privLabel,decPass);
                    } else {
                        validLogin = false;
                    }
                    if (!validLogin) {
                        Print.logInfo("Invalid Password for User: " + accountID + "/" + userID);
                        account = null;
                        user = null;
                        loginOK = false;
                    }
                } else {
                    // standard password check
                    boolean validLogin = false;
                    if (enteredPass != null) {
                        String decPass = enteredPass;
                        validLogin = account.checkPassword(privLabel,decPass);
                    } else
                    if (entEncPass != null) {
                        // TODO: should support checking MD5 encoded passwords
                        String decPass = Account.decodePassword(privLabel,entEncPass); // may be null
                        validLogin = account.checkPassword(privLabel,decPass);
                    } else {
                        validLogin = false;
                    }
                    if (!validLogin) {
                        Print.logInfo("Invalid Password for Account: " + accountID);
                        account = null;
                        loginOK = false;
                    }
                }
            } 

            /* invalid account, user, or password, displays the same error */
            if (!loginOK) {
                Track.clearSessionAttributes(request);
                if (authRequest) {
                    Track._writeAuthenticateXML(response, false);
                } else
                if (AccountRecord.isSystemAdminAccountID(accountID) && StringTools.isBlank(enteredPass)) {
                    String enterAcctPass = i18n.getString("Track.enterAccount","Please enter Login/Authentication"); // UserErrMsg
                    Track._displayLogin(reqState, enterAcctPass, false);
                } else
                if (request.getParameter(AttributeTools.ATTR_RTP) != null) {
                    String enterAcctPass = i18n.getString("Track.enterAccount","Please enter Login/Authentication"); // UserErrMsg
                    Track._displayLogin(reqState, enterAcctPass, false);
                } else {
                    String invLoginText = reqState.getShowPassword()?
                        i18n.getString("Track.invalidLoginPass","Invalid Login/Password") : // UserErrMsg
                        i18n.getString("Track.invalidLogin","Invalid Login"); // UserErrMsg
                    Track._displayLogin(reqState, invLoginText, true);
                }
                Print.logInfo("Login[Failed]: Domain="+bplName + " Account="+accountID + " User="+userID + " Time="+nowTimeSec + " IPAddr="+ipAddr);
                return;
            }

            /* inactive/expired user */
            if ((user != null) && !user.isActive()) {
                Print.logInfo("User is inactive: " + accountID + "/" + userID);
                account = null;
                user = null;
                if (authRequest) {
                    Track._writeAuthenticateXML(response, false);
                } else {
                    String inactiveUser = i18n.getString("Track.inactiveUser","Inactive User"); // UserErrMsg
                    Track._displayLogin(reqState, inactiveUser, true);
                }
                Print.logInfo("Login[Inactive]: Domain="+bplName + " Account="+accountID + " User="+userID + " Time="+nowTimeSec + " IPAddr="+ipAddr);
                return;
            }

            /* log login message */
            if (!wasLoggedIn) {
                // Account/User was not previously logged-in
                if (account.isSystemAdmin()) {
                    Print.logInfo("Login SysAdmin: " + accountID + "/" + userID + " [From " + ipAddr + "]");
                } else {
                    Print.logInfo("Login Account/User: " + accountID + "/" + userID + " [From " + ipAddr + "]");
                }
            }

            /* save current context user in Account */
            account.setCurrentUser(user);

        } catch (DBException dbe) {

            // Internal error
            Track.clearSessionAttributes(request);
            Print.logException("Error reading Account/User: " + accountID + "/" + userID, dbe);
            if (authRequest) {
                Track._writeAuthenticateXML(response, false);
            } else {
                String errorAccount = i18n.getString("Track.errorUser","Error reading Account/User");
                Track.writeErrorResponse(reqState, errorAccount);
            }
            return;

        }
        reqState.setCurrentAccount(account); // never null
        reqState.setCurrentUser(user); // may be null

        // --------------------------------------
        // -- login successful after this point

        /* account specific Look&Feel ID */
        // Resource.getPrivateLabelPropertiesForHost
        {
            RTProperties acctLafProps = null;
            // -- first try account specific lafID
            //if (account.hasLookAndFeelID()) {
            //    acctLafProps = Resource.getPrivateLabelPropertiesForHost(account.getLookAndFeelID(), null);
            //}
            // -- check for account resource lafID
            if (acctLafProps == null) {
                acctLafProps = Resource.getPrivateLabelPropertiesForHost("-"+accountID, null);
            }
            // -- set Account LAF props if found
            if (acctLafProps != null) {
                RTConfig.pushTemporaryProperties(acctLafProps);
            }
        }

        /* Authenticate test */
        // -- experimental
        if (authRequest) {
            Track._writeAuthenticateXML(response, true);
            return;
        }

        /* password expired? */
        boolean passwordExpired = false;
        if (user != null) {
            // logged in via User password
            passwordExpired = user.hasPasswordExpired();
        } else {
            // logged in via Account password
            passwordExpired = account.hasPasswordExpired();
        }

        /* set SESSION_ACCOUNT/SESSION_USER */
        RTProperties sessionProps = new RTProperties();
        String sessionAcctID = account.getAccountID();
        String sessionUserID = (user != null)? user.getUserID() : User.getAdminUserID();
        sessionProps.setString(RTKey.SESSION_ACCOUNT  , sessionAcctID);
        sessionProps.setString(RTKey.SESSION_USER     , sessionUserID);
        sessionProps.setString(RTKey.SESSION_IPADDRESS, ipAddr);
        RTConfig.pushTemporaryProperties(sessionProps);

        /* save locale override (after "clearSessionAttributes") */
        if (!StringTools.isBlank(localeStr)) {
            AttributeTools.setSessionAttribute(request, Constants.PARM_LOCALE, localeStr);
            RTProperties localeProps = sessionProps; // new RTProperties();
            localeProps.setString(RTKey.SESSION_LOCALE, localeStr); // SESSION_USER?
            localeProps.setString(RTKey.LOCALE        , localeStr);
            //RTConfig.pushTemporaryProperties(localeProps);
            //Print.logInfo("PrivateLabel Locale: " + localeStr + " [" + privLabel.getLocaleString() + "]");
        }

        /* Account temporary properties? */
        try {
            Resource resource = Resource.getResource(account, Resource.RESID_TemporaryProperties);
            if (resource != null) {
                RTProperties rtp = resource.getRTProperties();
                //Print.logInfo("Pushing '"+Resource.RESID_TemporaryProperties+"': " + rtp);
                RTConfig.pushTemporaryProperties(rtp);
            }
        } catch (DBException dbe) {
            Print.logError("Error reading Account resource: " + dbe);
        }

        /* Reverse Geocode */
        if (pageName.equals(PAGE_REVERSEGEOCODE) && !reqState.isDemoAccount() && 
            RTConfig.getBoolean("enableReverseGeocodeTest",false)) {
            String rgCache[] = null;
            ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
            if (rgp != null) {
                boolean cache = AttributeTools.getRequestBoolean(request, "cache", false); // from query only
                String gpStr = AttributeTools.getRequestString(request, "gp", "0/0"); // from query only
                GeoPoint gp = new GeoPoint(gpStr);
                ReverseGeocode rg = rgp.getReverseGeocode(gp, localeStr, cache);
                String charSet = StringTools.getCharacterEncoding();
                Print.logInfo("ReverseGeocode: ["+charSet+"]\n"+rg);
                rgCache = new String[] { gp.toString(), rg.getFullAddress() };
                Track.writeMessageResponse(reqState, "ReverseGeocode: ["+charSet+"]\n"+rg); // English only
            } else {
                Track.writeMessageResponse(reqState, "ReverseGeocodeProvider not available"); // English only
            }
            // Save last cached geocode
            AttributeTools.setSessionAttribute(request, Constants.LAST_REVERSEGEOCODE, rgCache);
            return;
        }

        /* Address/PostalCode Geocode */
        if (pageName.equals(PAGE_GEOCODE)) {
            //Print.logInfo("Found 'PAGE_GEOCODE' request ...");
            String rgCache[] = null; // { "Address", "Country", "Latitude/Longitude" }
            GeoPoint rgPoint = null;
            String   rgAddrs = null;
            String     gpXML = null;
            // PrivateLabel.PROP_ZoneInfo_enableGeocode
            String addr    = AttributeTools.getRequestString(request, "addr", ""); // zip/address
            String country = AttributeTools.getRequestString(request, "country", "");
            GeocodeProvider geocodeProv = privLabel.getGeocodeProvider();
            if (geocodeProv != null) {
                GeoPoint gp = geocodeProv.getGeocode(addr, country);
                //Print.logInfo("GeocodeProvider ["+geocodeProv.getName()+"] "+addr+" ==> " + gp);
                if ((gp != null) && gp.isValid()) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    sb.append("<geocode>\n");
                    sb.append(" <lat>").append(gp.getLatitude() ).append("</lat>\n");
                    sb.append(" <lng>").append(gp.getLongitude()).append("</lng>\n");
                    sb.append("</geocode>\n");
                    gpXML = sb.toString();
                    rgAddrs = addr;
                    rgPoint = gp;
                    rgCache = new String[] { addr, country, gp.toString() };
                } else {
                    // no GeoPoint found
                    gpXML = "";
                    Print.logInfo("No Geocode point found ...");
                }
            } else
            if (StringTools.isBlank(addr)) {
                // zip/address not specified
                gpXML = "";
                Print.logInfo("Address not specified ...");
            } else
            if (StringTools.isNumeric(addr)) {
                // all numeric, US zip code only
                int timeoutMS = 5000;
                String zip = addr;
                gpXML = org.opengts.geocoder.geonames.GeoNames.getPostalCodeLocation_xml(zip, country, timeoutMS);
                // already included "<?xml version="1.0" encoding="UTF-8" standalone="no"?>"
                // TODO: extract GeoPoint
                Print.logInfo("Geonames zip geocode ...");
            } else {
                // city, state
                int timeoutMS = 5000;
                String a[] = StringTools.split(addr,',');
                if (ListTools.isEmpty(a)) {
                    gpXML = "";
                } else
                if (a.length >= 2) {
                    String state = a[a.length - 1];
                    String city  = a[a.length - 2];
                    gpXML = org.opengts.geocoder.geonames.GeoNames.getCityLocation_xml(city, state, country, timeoutMS);
                    // already included "<?xml version="1.0" encoding="UTF-8" standalone="no"?>"
                    // TODO: extract GeoPoint
                } else {
                    String state = "";
                    String city  = a[0];
                    gpXML = org.opengts.geocoder.geonames.GeoNames.getCityLocation_xml(city, state, country, timeoutMS);
                    // already included "<?xml version="1.0" encoding="UTF-8" standalone="no"?>"
                    // TODO: extract GeoPoint
                }
                Print.logInfo("Geonames city/state geocode ...");
            }
            // Save last cached geocode
            if (rgCache == null) {
                Print.logInfo("No cached GeoPoint for address: "+rgAddrs);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_CACHE    , null);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_ADDRESS  , null);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_LATITUDE , null);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_LONGITUDE, null);
            } else {
                Print.logInfo("Geocode: "+rgAddrs+" ==> " + rgPoint);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_CACHE    , rgCache);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_ADDRESS  , rgAddrs);
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_LATITUDE , rgPoint.getLatitudeString(null,null));
                AttributeTools.setSessionAttribute(request, Constants.LAST_GEOCODE_LONGITUDE, rgPoint.getLongitudeString(null,null));
            }
            // write Geocode XML to client
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML(), StringTools.CharEncoding_UTF_8);
            PrintWriter out = response.getWriter();
            out.println(
                !StringTools.isBlank(gpXML)? gpXML : 
                    ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<geonames></geonames>\n"));
            out.close();
            return;
        }

        /* Check Rule Trigger */
        if (pageName.equals(PAGE_RULE_EVAL)) {
            Object result = null;
            String ruleID = AttributeTools.getRequestString(request, Constants.PARM_RULE, "");
            if (StringTools.isBlank(ruleID)) {
                // undefined
            } else
            if (ruleID.startsWith("@")) {
                // pre-defined function
                if (ruleID.equalsIgnoreCase("@devnotify")) {
                    // account
                    // user     (may be null)
                    try {
                        long ageSec = -1L;
                        long sinceTime = (ageSec >= 0L)? (DateTime.getCurrentTimeSec() - ageSec) : 1L; // 1 ==> Jan 1, 1970 12:01am
                        result = account.hasDeviceLastNotifySince(sinceTime,user)? "1" : "0";
                    } catch (DBException dbe) {
                        Print.logError("Account/Device DBException: " + accountID + " [" + dbe);
                        result = null;
                    }
                } else {
                    // undefined
                }
            } else
            if (Device.hasRuleFactory()) {
                RuleFactory ruleFact = Device.getRuleFactory();
                String selector = ruleFact.getRuleSelector(account, ruleID);
                if (!StringTools.isBlank(selector)) {
                    try {
                        Object r = ruleFact.evaluateSelector(selector, account);
                        result = (r != null)? r : "";
                    } catch (RuleParseException rpe) {
                        Print.logException("Rule Selector: " + selector, rpe);
                        result = null;
                    }
                } else {
                    // undefined
                }
            } else {
                // undefined
            }
            // write Rule match state XML to client
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML(), StringTools.CharEncoding_UTF_8);
            PrintWriter out = response.getWriter();
            //Print.logInfo("Rule selector result: " + result);
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            if (result != null) {
                out.println("<Result>"+result+"</Result>");
            } else {
                out.println("<Result/>");
            }
            out.close();
            return;
        }

        /* do we have a 'trackPage'? */
        if (trackPage == null) {
            // occurs when 'pageName' is either blank or invalid

            /* check user preference for first page after login */
            if (!wasLoggedIn && (user != null) && user.hasFirstLoginPageID()) {
                pageName = user.getFirstLoginPageID();
            } else {
                //pageName = PAGE_MENU_TOP;
                pageName = privLabel.getStringProperty(PrivateLabel.PROP_Track_firstLoginPageID,PAGE_MENU_TOP);
            }

            /* get page */
            trackPage = privLabel.getWebPage(pageName); // should not be null
            if (trackPage == null) {
                if (pageName.equals(PAGE_MENU_TOP)) {
                    Print.logError("[CRITICAL] Missing required page specification: '%s'", pageName);
                    String invalidPage = i18n.getString("Track.invalidPage","Unrecognized page requested: {0}",pageName);
                    Track.writeErrorResponse(reqState, invalidPage);
                    return;
                } else {
                    Print.logError("Invalid page requested [%s], defaulting to '%s'", pageName, PAGE_MENU_TOP);
                    pageName = PAGE_MENU_TOP;
                    trackPage = privLabel.getWebPage(pageName);
                    if (trackPage == null) {
                        Print.logError("[CRITICAL] Missing required page specification: '%s'", pageName);
                        String invalidPage = i18n.getString("Track.invalidPage","Unrecognized page requested: {0}",pageName);
                        Track.writeErrorResponse(reqState, invalidPage);
                        return;
                    }
                }
            }
            reqState.setPageName(pageName);

        }

        /* invalid page for ACL? */
        if (!privLabel.hasReadAccess(user,trackPage.getAclName())) {
            String userName = (user != null)? user.getUserID() : (User.getAdminUserID() + "?");
            String aclName = trackPage.getAclName();
            //Print.logWarn("User=%s, ACL=%s, Access=%s", userName, aclName, reqState.getAccessLevel(aclName));
            String unauthPage = i18n.getString("Track.unauthPage","Unauthorized page requested: {0}",pageName);
            Track.writeErrorResponse(reqState, unauthPage);
            return;
        }

        /* default selected device group (if any) */
        //if (StringTools.isBlank(groupID)) {
        //    groupID = DeviceGroup.DEVICE_GROUP_ALL;
        //}

        /* default selected device (if any) */
        if ((user != null) && !StringTools.isBlank(deviceID)) {
            try {
                if (!user.isAuthorizedDevice(deviceID)) {
                    deviceID = null;
                }
            } catch (DBException dbe) {
                deviceID = null;
            }
        }
        if (!StringTools.isBlank(deviceID)) {
            reqState.setSelectedDeviceID(deviceID, true);
        } else {
            try {
                if (user != null) {
                    deviceID = user.getDefaultDeviceID(false); // already authorized
                } else {
                    OrderedSet<String> d = Device.getDeviceIDsForAccount(accountID, null, false, 1);
                    deviceID = !ListTools.isEmpty(d)? d.get(0) : null;
                }
            } catch (DBException dbe) {
                deviceID = null;
            }
            reqState.setSelectedDeviceID(deviceID, false);
        }

        /* default selected group (if any) */
        if (User.isAdminUser(user)) {
            // admin user
            OrderedSet<String> g = reqState.getDeviceGroupIDList(true/*include'ALL'*/);
            if (StringTools.isBlank(groupID)) {
                groupID = !ListTools.isEmpty(g)? g.get(0) : DeviceGroup.DEVICE_GROUP_ALL;
            } else
            if (!ListTools.contains(g,groupID)) {
                Print.logWarn("DeviceGroup list does not contain ID '%s'", groupID);
                groupID = !ListTools.isEmpty(g)? g.get(0) : DeviceGroup.DEVICE_GROUP_ALL;
            } else {
                // groupID is fine as-is
            }
        } else {
            // specific user
            OrderedSet<String> g = reqState.getDeviceGroupIDList(true/*include'ALL'*/);
            if (StringTools.isBlank(groupID)) {
                groupID = !ListTools.isEmpty(g)? g.get(0) : null;
            } else
            if (!ListTools.contains(g,groupID)) {
                Print.logWarn("DeviceGroup list does not contain ID '%s'", groupID);
                groupID = !ListTools.isEmpty(g)? g.get(0) : null;
            } else {
                // groupID is fine as-is
            }
        }
        reqState.setSelectedDeviceGroupID(groupID);

        /* Account/User checks out, marked as logged in */
        AttributeTools.setSessionAttribute(request, Constants.PARM_ACCOUNT , accountID);
        AttributeTools.setSessionAttribute(request, Constants.PARM_USER    , userID);
        AttributeTools.setSessionAttribute(request, Constants.PARM_GROUP   , groupID);
        AttributeTools.setSessionAttribute(request, Constants.PARM_DEVICE  , deviceID);
        AttributeTools.setSessionAttribute(request, Constants.PARM_ENCPASS , entEncPass);

        /* make sure we have session sequence ID cached */
        AttributeTools.GetSessionSequence(request);

        /* login from "sysadmin"? */
        //String relogin = AttributeTools.getRequestString(request, Constants.PARM_SA_RELOGIN, "");
        //if (!StringTools.isBlank(relogin)) {
        //    AttributeTools.setSessionAttribute(request, Constants.PARM_SA_RELOGIN, relogin);
        //}
        //boolean isSysadminRelogin = RequestProperties.isLoggedInFromSysAdmin(request,privLabel);

        /* set login times */
        if (!wasLoggedIn) {
            boolean updLastLogin_user    = RTConfig.getBoolean(DBConfig.PROP_track_updateLastLoginTime_user   ,true);
            boolean updLastLogin_account = RTConfig.getBoolean(DBConfig.PROP_track_updateLastLoginTime_account,true);

            // Last User login time
            if (updLastLogin_user && (user != null)) {
                try {
                    user.setLastLoginTime(nowTimeSec);
                    user.update(User.FLD_lastLoginTime); // anything else?
                } catch (DBException dbe) {
                    Print.logException("Error saving LastLoginTime for user: " + accountID + "/" + userID, dbe);
                    // continue
                }
            }

            // Last Account login time
            if (updLastLogin_account) {
                try {
                    account.setLastLoginTime(nowTimeSec);
                    account.update(Account.FLD_lastLoginTime);
                } catch (DBException dbe) {
                    Print.logException("Error saving LastLoginTime for account: " + accountID, dbe);
                    // continue
                }
            }

            // login audit
            Audit.userLoginOK(accountID, userID, ipAddr, bplName);

        }

        /* dispatch to page */
        reqState.setPageNavigationHTML(trackPage.getPageNavigationHTML(reqState));
        trackPage.writePage(reqState, "");
        return;

    }

    /* display authentication state */
    private static void _writeAuthenticateXML(
        HttpServletResponse response,
        boolean state)
        throws IOException
    {
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML(), StringTools.CharEncoding_UTF_8);
        PrintWriter out = response.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.print("<authenticate>");
        out.print(state?"true":"false");
        out.print("</authenticate>");
        out.print("\n");
        out.close();
    }

    /* display login */
    private static void _displayLogin(
        RequestProperties reqState,
        String msg,
        boolean alert)
        throws IOException
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        WebPage loginPage = privLabel.getWebPage(PAGE_LOGIN);
        if ((loginPage instanceof AccountLogin) && ((AccountLogin)loginPage).hasCustomLoginURL()) {
            HttpServletResponse response = reqState.getHttpServletResponse();
            HttpServletRequest  request  = reqState.getHttpServletRequest();
            String loginURL = ((AccountLogin)loginPage).getCustomLoginURL();
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
            PrintWriter out = response.getWriter();
            out.println("<HTML>");
            out.println("<META HTTP-EQUIV=\"refresh\" CONTENT=\"1; URL="+loginURL+"\"></META>");
            out.println("</HTML>");
            out.close();
        } else
        if (loginPage != null) {
            reqState.setPageName(PAGE_LOGIN);
            reqState.setPageNavigationHTML(loginPage.getPageNavigationHTML(reqState));
            if (alert) {
                reqState._setLoginErrorAlert();
            }
            //Print.logInfo("Writing login page ... " + StringTools.className(loginPage));
            loginPage.writePage(reqState, msg);
        } else {
            Print.logError("Login page '"+PAGE_LOGIN+"' not found!");
            I18N i18n = privLabel.getI18N(Track.class);
            String noLogin = i18n.getString("Track.noLogin","Login page not available");
            Track.writeErrorResponse(reqState, noLogin);
        }
    }

    // ------------------------------------------------------------------------
    // Simple error response

    /* write message to stream */
    public static void writeMessageResponse(
        final RequestProperties reqState,
        final String msg)
        throws IOException
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        WebPage loginPage = privLabel.getWebPage(PAGE_LOGIN);
        if (loginPage != null) {
            //reqState.setPageName(PAGE_LOGIN);
            reqState.setPageNavigationHTML(loginPage.getPageNavigationHTML(reqState));
        } else {
            //Print.logWarn("Login page not found.");
        }
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_MESSAGE, "") {  // Content
            public void write(PrintWriter out) throws IOException {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                I18N i18n = privLabel.getI18N(Track.class);
                String baseURL = WebPageAdaptor.EncodeURL(reqState,new URIArg(RequestProperties.TRACK_BASE_URI()));
                out.println(StringTools.replace(msg,"\n",StringTools.HTML_BR));
                out.println("<hr>");
                out.println("<a href=\"" + baseURL + "\">" + i18n.getString("Track.back","Back") + "</a>");
            }
        };
        CommonServlet.writePageFrame(
            reqState,
            null,null,          // onLoad/onUnload
            HTMLOutput.NOOP,    // Style sheets
            HTMLOutput.NOOP,    // JavaScript
            null,               // Navigation
            HTML_CONTENT);      // Content
    }

    /* write error response to stream */
    public static void writeErrorResponse(
        RequestProperties reqState,
        String errMsg)
        throws IOException
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(Track.class);
        String error = i18n.getString("Track.error","ERROR:");
        Track.writeMessageResponse(reqState, error + "\n" + errMsg);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* debug: test page */
    public static void main(String argv[])
        throws IOException
    {
        //RTConfig.setCommandLineArgs(argv);
        
        TimeZone tz = null;
        DateTime fr = new DateTime(tz, 2008, RTConfig.getInt("m",6), 1);
        DateTime to = new DateTime(tz, 2008, RTConfig.getInt("m",7), 1);
        String acctId = "opendmtp";
        String devId  = "mobile";
        
        // OutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        
        //writePage_Map(
        //  out, 
        //  privLabel, 
        //  acctId, 
        //  devId, new String[] { devId }, 
        //  fr, to, 
        //  Account.GetDefaultTimeZone(),
        //  lastEvent);
        
        out.close(); // close (flush PrintWriter)
        String s = new String(baos.toByteArray());
        System.err.print(s);

    }

}
