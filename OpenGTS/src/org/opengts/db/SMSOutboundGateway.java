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
//  Outbound SMS Gateway support
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
//  2010/11/29  Martin D. Flynn
//     -Added "httpURL" format
//  2011/03/08  Martin D. Flynn
//     -Changed "getStringProperty" to first look at the account BasicPrivateLabel
//  2011/05/13  Martin D. Flynn
//     -Look for additional replacement vars "${var}", "%{var}", and "{VAR}",
//      where "var" is "mobile", "message", and "sender".
//  2012/02/03  Martin D. Flynn
//     -Added method "RemovePrefixSMS(...)"
//  2012/04/03  Martin D. Flynn
//     -Added ability to read http-base SMS authorization from Account record
//     -Added "1mobile" replacement var which prepends "1" to "6505551212".
//  2012/09/02  Martin D. Flynn
//     -Added "%{simID}" command URL replacement variable.
//     -Added support for "SmsGatewayHandler.GWNAME.maxMessageLength" property
//  2012/10/16  Martin D. Flynn
//     -Added specific error check for "clickatell.com" in "httpURL" mode.
//  2013/11/11  Martin D. Flynn
//     -Generalized replacement variable handling in the http-based SMS gateway.
//  2014/03/03  Martin D. Flynn
//     -Fixed "EncodeUrlReplacementVars" to allow "smsPhone" to override SIM 
//      phone number specified in the "device" record [2.5.4-B14]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** Outbound SMS gateway handler
**/
public abstract class SMSOutboundGateway
{

    // ------------------------------------------------------------------------

    private static final String PROP_SmsGatewayHandler_             = "SmsGatewayHandler.";
    public  static final String PROP_defaultName                    = PROP_SmsGatewayHandler_ + "defaultName";

    /* emailBody */
    public  static final String GW_emailBody                        = "emailBody";
    public  static final String PROP_emailBody_smsEmailAddress      = PROP_SmsGatewayHandler_ + "emailBody.smsEmailAddress";
    public  static final String PROP_emailBody_maxMessageLength     = PROP_SmsGatewayHandler_ + "emailBody.maxMessageLength";

    /* emailSubject */
    public  static final String GW_emailSubject                     = "emailSubject";
    public  static final String PROP_emailSubject_smsEmailAddress   = PROP_SmsGatewayHandler_ + "emailSubject.smsEmailAddress";
    public  static final String PROP_emailSubject_maxMessageLength  = PROP_SmsGatewayHandler_ + "emailSubject.maxMessageLength";

    /* httpURL (Clickatell, Kannel, etc) */
    public  static final String GW_httpURL                          = "httpURL";
    public  static final String PROP_httpURL_url                    = PROP_SmsGatewayHandler_ + "httpURL.url";
    public  static final String PROP_httpURL_maxMessageLength       = PROP_SmsGatewayHandler_ + "httpURL.maxMessageLength";

    /* ClickATell (EMail) */
    public  static final String GW_clickatell                       = "clickatell";
    public  static final String PROP_clickatell_smsEmailAddress     = PROP_SmsGatewayHandler_ + "clickatell.smsEmailAddress";
    public  static final String PROP_clickatell_user                = PROP_SmsGatewayHandler_ + "clickatell.user";
    public  static final String PROP_clickatell_password            = PROP_SmsGatewayHandler_ + "clickatell.password";
    public  static final String PROP_clickatell_api_id              = PROP_SmsGatewayHandler_ + "clickatell.api_id";
    public  static final String PROP_clickatell_maxMessageLength    = PROP_SmsGatewayHandler_ + "clickatell.maxMessageLength";

    /* TextAnywhere: mail2txt, mail2txt160, mail2txtid, mail2txt160id */
    public  static final String GW_mail2txt                         = "mail2txt";
    public  static final String PROP_mail2txt_smsEmailAddress       = PROP_SmsGatewayHandler_ + "mail2txt.smsEmailAddress";
    public  static final String PROP_mail2txt_maxMessageLength      = PROP_SmsGatewayHandler_ + "mail2txt.maxMessageLength";
    public  static final String GW_mail2txt160                      = "mail2txt160";
    public  static final String PROP_mail2txt160_smsEmailAddress    = PROP_SmsGatewayHandler_ + "mail2txt160.smsEmailAddress";
    public  static final String PROP_mail2txt160_maxMessageLength   = PROP_SmsGatewayHandler_ + "mail2txt160.maxMessageLength";
    public  static final String GW_mail2txtid                       = "mail2txtid";
    public  static final String PROP_mail2txtid_smsEmailAddress     = PROP_SmsGatewayHandler_ + "mail2txtid.smsEmailAddress";
    public  static final String PROP_mail2txtid_from                = PROP_SmsGatewayHandler_ + "mail2txtid.from";
    public  static final String PROP_mail2txtid_maxMessageLength    = PROP_SmsGatewayHandler_ + "mail2txtid.maxMessageLength";
    public  static final String GW_mail2txt160id                    = "mail2txt160id";
    public  static final String PROP_mail2txt160id_smsEmailAddress  = PROP_SmsGatewayHandler_ + "mail2txt160id.smsEmailAddress";
    public  static final String PROP_mail2txt160id_from             = PROP_SmsGatewayHandler_ + "mail2txt160id.from";
    public  static final String PROP_mail2txt160id_maxMessageLength = PROP_SmsGatewayHandler_ + "mail2txt160id.maxMessageLength";

    /* ozekisms */
    public  static final String GW_ozekisms                         = "ozekisms";
    public  static final String PROP_ozekisms_hostPort              = PROP_SmsGatewayHandler_ + "ozekisms.hostPort";
    public  static final String PROP_ozekisms_originator            = PROP_SmsGatewayHandler_ + "ozekisms.originator";
    public  static final String PROP_ozekisms_user                  = PROP_SmsGatewayHandler_ + "ozekisms.user";
    public  static final String PROP_ozekisms_password              = PROP_SmsGatewayHandler_ + "ozekisms.password";
    public  static final String PROP_ozekisms_maxMessageLength      = PROP_SmsGatewayHandler_ + "ozekisms.maxMessageLength";

    // ------------------------------------------------------------------------

    public static final int     MAX_TEXT_LENGTH                     = 160;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the default SMS Gateway name
    **/
    public static String GetDefaultGatewayName()
    {
        return RTConfig.getString(PROP_defaultName,GW_emailBody);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String SMS_Prefix = "SMS:";
    
    /**
    *** Return true if string starts with "SMS:"
    *** @param val  The string to test
    *** @return True is string starts with "SMS:", false otherwise
    **/
    public static boolean StartsWithSMS(String val)
    {
        return StringTools.startsWithIgnoreCase(val,SMS_Prefix);
    }

    /**
    *** Removes any prefixing "SMS:" from the specified string.
    *** @param val  The string from which the prefixing "SMS:" is removed.
    *** @return The specified string, sans the prefixing "SMS:"
    **/
    public static String RemovePrefixSMS(String val)
    {
        val = StringTools.trim(val);
        if (SMSOutboundGateway.StartsWithSMS(val)) {
            return val.substring(SMSOutboundGateway.SMS_Prefix.length()).trim();
        } else {
            return val; // leave as-is
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the default maximum text message length
    **/
    public static int getMaximumTextMessageLength()
    {
        return MAX_TEXT_LENGTH;
    }

    /**
    *** Truncates the specified message to the maximum text message length
    **/
    public static String truncateTextMessageToMaximumLength(String msg)
    {
        msg = StringTools.trim(msg);
        if (StringTools.isBlank(msg)) {
            return "";
        } else
        if (msg.length() > SMSOutboundGateway.getMaximumTextMessageLength()) {
            msg = msg.substring(0,SMSOutboundGateway.getMaximumTextMessageLength()).trim();
            return msg;
        } else {
            return msg;
        }
    }

    /**
    *** Truncates the specified message to the specified length
    **/
    public static String truncateMessage(String message, int length)
    {

        /* return empty string if null */
        if (message == null) {
            return "";
        }

        /* trim */
        message = message.trim();

        /* check minimum length */
        int len = (length > 0)? Math.min(length,MAX_TEXT_LENGTH) : MAX_TEXT_LENGTH;
        if (message.length() > len) {
            return message.substring(0,len);
        } else {
            return message;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if device is authorized to send SMS messages
    **/
    public static boolean isDeviceAuthorized(Device device)
    {

        /* invalid device */
        if (device == null) {
            return false;
        }

        /* invalid account */
        Account account = device.getAccount();
        if (account == null) {
            return false;
        }

        /* SMS enabled? */
        if (!account.getSmsEnabled()) {
            return false;
        }

        /* authorized */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    //  %{mobile}   - replaced with the Device "SIM Phone Number" value
    //  %{1mobile}  - replaced with the Device "SIM Phone Number" value (checks for "1" prefix)
    //  %{sender}   - SmsRTProperties: "sender="
    //  %{dataKey}  - replaced with the Device "Data Key" value
    //  %{uniqueID} - replaced with the Device "Unique-ID"
    //  %{modemID}  - replaced with the Device Modem-ID (ie. Unique-ID with prefix removed)
    //  %{imei}     - replaced with the Device "IMEI #" value
    //  %{serial}   - replaced with the Device "Serial Number" value
    //  %{user}     - SmsRTProperties: "user="
    //  %{password} - SmsRTProperties: "password="
    //  %{authID}   - SmsRTProperties: "authID="
    //  %{message}  - replaced with the SMS text to send to the device
    public  static final String REPL_mobile[]   = new String[] { "%{mobile}"  , "{MOBILE}"  , "${mobile}"   };
    public  static final String REPL_1mobile[]  = new String[] { "%{1mobile}" , "{1MOBILE}" , "${1mobile}"  };
    public  static final String REPL_sender[]   = new String[] { "%{sender}"  , "{SENDER}"  , "${sender}"   };
    public  static final String REPL_dataKey[]  = new String[] { "%{dataKey}" , "{DATAKEY}" , "${dataKey}"  };
    public  static final String REPL_uniqueID[] = new String[] { "%{uniqueID}", "{UNIQUEID}", "${uniqueID}" };
    public  static final String REPL_modemID[]  = new String[] { "%{modemID}" , "{MODEMID}" , "${modemID}"  };
    public  static final String REPL_simID[]    = new String[] { "%{simID}"   , "{SIMID}"   , "${simID}"    };
    public  static final String REPL_imei[]     = new String[] { "%{imei}"    , "{IMEI}"    , "${imei}"     };
    public  static final String REPL_serial[]   = new String[] { "%{serial}"  , "{SERIAL}"  , "${serial}"   };
    public  static final String REPL_user[]     = new String[] { "%{user}"    , "{USER}"    , "${user}"     };
    public  static final String REPL_password[] = new String[] { "%{password}", "{PASSWORD}", "${password}" };
    public  static final String REPL_authID[]   = new String[] { "%{authID}"  , "{AUTHID}"  , "${authID}"   };
    public  static final String REPL_message[]  = new String[] { "%{message}" , "{MESSAGE}" , "${message}"  };

    /**
    *** Replace specified variables with replacement text
    *** @param s  Target String
    *** @param r  Replacement variable names
    *** @param m  Replacement text
    *** @return The new string with variables replaced with specified text
    **/
    public static String REPLACE(String s, String r[], String m)
    {
        for (int i = 0; i < r.length; i++) {
            s = StringTools.replace(s, r[i], m);
        }
        return s;
    }

    /**
    *** Replace variables in the specified string with the specified property values.
    *** @param s   Target String
    *** @param rtp Replacement property/value list
    *** @return The new string with variables replaced with specified text
    **/
    public static String REPLACE(String s, RTProperties rtp)
    {
        return StringTools.insertKeyValues(s, "%{", "}", "=", rtp);
    }

    // ------------------------------------------------------------------------

    /**
    *** Add properties to replacement variable list
    **/
    private static void AddUrlRtpArgs(RTProperties urlRTP, RTProperties otherRTP)
    {
        if (otherRTP != null) {
            Set<?> keys = otherRTP.getPropertyKeys();
            for (Object K : keys) {
                Object V = otherRTP.getProperty(K,null);
                if ((K instanceof String) && (V instanceof String) && !StringTools.isBlank((String)V)) {
                    String key = (String)K;
                    String val = (String)V;
                    SMSOutboundGateway.AddUrlRtpArg(urlRTP, key, val);
                }
            }
        }
    }

    /**
    *** Add a key/value to the replacement variable list
    **/
    private static void AddUrlRtpArg(RTProperties urlRTP, String key, String val)
    {
        if (!StringTools.isBlank(val)) {
            urlRTP.setString(key, URIArg.encodeArg(val));
        }
    }

    /**
    *** Encode the specified URL with the standard replacement variables
    **/
    private static String EncodeUrlReplacementVars(
        Account account, 
        Device device, String smsPhone,
        String httpURL, 
        String messageStr, int maxMsgLen)
    {
        RTProperties urlRTP = new RTProperties();

        /* no Account/Device? */
        if ((account == null) && (device == null)) {
            return httpURL; // return as-is
        } else
        if (account == null) {
            account = device.getAccount();
        }

        /* set default "sender" */
        String sender = null;
        if (account != null) {
            sender = account.getContactPhone();
            AddUrlRtpArg(urlRTP, "sender", sender);
        }

        /* Account-based replacement vars */
        String user     = null;
        String password = null;
        String authID   = null;
        if (account != null) {
            RTProperties smsRTP = account.getSmsRTProperties(); // never null
            // - extract specific properties (for "REPLACE" below)
            sender   = smsRTP.getString("sender"  ,sender);
            user     = smsRTP.getString("user"    ,""); // SMS authorization
            password = smsRTP.getString("password","");
            authID   = smsRTP.getString("authID"  ,"");
            // - add all properties contained in 'smsRTP'
            AddUrlRtpArgs(urlRTP, smsRTP);
        } else {
            // - add default property values
            /*
            AddUrlRtpArg(urlRTP, "sender"  , sender  );
            AddUrlRtpArg(urlRTP, "user"    , user    );
            AddUrlRtpArg(urlRTP, "password", password);
            AddUrlRtpArg(urlRTP, "authID"  , authID  );
            */
        }

        /* SIM phone number [v2.5.4-B14] */
        String devSPN   = (device != null)? device.getSimPhoneNumber() : "";
        String mobile   = !StringTools.isBlank(smsPhone)? StringTools.trim(smsPhone) : devSPN;
        String mobile_1 = ((mobile==null)||mobile.startsWith("1")||(mobile.length()!=10))? mobile : ("1"+mobile);
        AddUrlRtpArg(urlRTP, "mobile"  , mobile  );
        AddUrlRtpArg(urlRTP, "1mobile" , mobile_1); // "1" + areaCode(3) + prefix(3) + suffix(4)

        /* Device-based replacement values */
        String dataKey  = (device != null)? device.getDataKey()      : null;
        String uniqueID = (device != null)? device.getUniqueID()     : null;
        String modemID  = (device != null)? device.getModemID()      : null;
        String simID    = (device != null)? device.getSimID()        : null;
        String imeiNum  = (device != null)? device.getImeiNumber()   : null;
        String serial   = (device != null)? device.getSerialNumber() : null;
        AddUrlRtpArg(urlRTP, "dataKey" , dataKey );
        AddUrlRtpArg(urlRTP, "uniqueID", uniqueID);
        AddUrlRtpArg(urlRTP, "modemID" , modemID );
        AddUrlRtpArg(urlRTP, "simID"   , simID   );
        AddUrlRtpArg(urlRTP, "imei"    , imeiNum );
        AddUrlRtpArg(urlRTP, "serial"  , serial  );

        /* encode/truncate message */
        String message = StringTools.trim(messageStr); // trim whitespace
        message = StringTools.insertKeyValues(message, "%{", "}", "=", urlRTP);
        if (StringTools.length(message) > maxMsgLen) {
            // exceeds maximum allowed SMS text length
            Print.logWarn("Invalid SMS text command length: " + StringTools.length(message));
            message = SMSOutboundGateway.truncateMessage(message, maxMsgLen);
        }
        AddUrlRtpArg(urlRTP, "message" , message );

        /* apply replacement vars to URL */
        httpURL = StringTools.insertKeyValues(httpURL, "%{", "}", "=", urlRTP);

        /* OBSOLETE: set URL replacement vars */
        // only necessary if the obsolete non %{VAR} format has been used
        httpURL = REPLACE(httpURL, REPL_mobile  , URIArg.encodeArg(mobile  ));  // SIM phone number
        httpURL = REPLACE(httpURL, REPL_1mobile , URIArg.encodeArg(mobile_1));  // 1 + SIM phone number
        httpURL = REPLACE(httpURL, REPL_message , URIArg.encodeArg(message ));  // command string
        httpURL = REPLACE(httpURL, REPL_sender  , URIArg.encodeArg(sender  ));  // Account contact phone number
        httpURL = REPLACE(httpURL, REPL_user    , URIArg.encodeArg(user    ));  // SMS auth user
        httpURL = REPLACE(httpURL, REPL_password, URIArg.encodeArg(password));  // SMS auth password
        httpURL = REPLACE(httpURL, REPL_authID  , URIArg.encodeArg(authID  ));  // SMS auth ID
        httpURL = REPLACE(httpURL, REPL_dataKey , URIArg.encodeArg(dataKey ));  // Device data key
        httpURL = REPLACE(httpURL, REPL_uniqueID, URIArg.encodeArg(uniqueID));  // Device unique ID
        httpURL = REPLACE(httpURL, REPL_modemID , URIArg.encodeArg(modemID ));  // Device modem ID
        httpURL = REPLACE(httpURL, REPL_simID   , URIArg.encodeArg(simID   ));  // Device SIM ID
        httpURL = REPLACE(httpURL, REPL_imei    , URIArg.encodeArg(imeiNum ));  // Device IMEI number
        httpURL = REPLACE(httpURL, REPL_serial  , URIArg.encodeArg(serial  ));  // Device serial number

        /* return URL */
        return httpURL;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Map<String,SMSOutboundGateway> SmsGatewayHandlerMap = null;

    /**
    *** Add SMS Gateway support provider
    **/
    public static void AddSMSGateway(String name, SMSOutboundGateway smsGW)
    {

        /* validate name */
        if (StringTools.isBlank(name)) {
            Print.logWarn("SMS Gateway name is blank");
            return;
        } else
        if (smsGW == null) {
            Print.logWarn("SMS Gateway handler is null");
            return;
        }
        smsGW.setName(name);

        /* initialize map? */
        if (SmsGatewayHandlerMap == null) { 
            SmsGatewayHandlerMap = new HashMap<String,SMSOutboundGateway>(); 
        }

        /* save handler */
        SmsGatewayHandlerMap.put(name.toLowerCase(), smsGW);
        Print.logDebug("Added SMS Gateway Handler: " + name);

    }

    /**
    *** Gets the SMSoutboubdGateway for the specified name
    **/
    public static SMSOutboundGateway GetSMSGateway(String name)
    {

        /* get handler */
        if (StringTools.isBlank(name)) {
            return null;
        } else {
            if (name.equalsIgnoreCase("body")) { 
                name = GW_emailBody; 
            } else
            if (name.equalsIgnoreCase("subject")) { 
                name = GW_emailSubject; 
            }
            return SmsGatewayHandlerMap.get(name.toLowerCase());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initialize outbound SMS gateway handlers
    **/
    public static void _startupInit()
    {
        Print.logDebug("SMSOutboundGateway initializing ...");

        /* already initialized? */
        if (SmsGatewayHandlerMap != null) {
            return;
        }

        // -----------------------------------------------
        // The following shows several example of outbound SMS gateway support.
        // The only method that needs to be overridden and implemented is
        //   public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr)
        // The "device" is the Device record instance to which the SMS message should be sent,
        // and "commandStr" is the SMS text (device command) which is to be sent to the device.
        // -----------------------------------------------

        /* EMail: standard "Body" command */
        // Property:
        //   SmsGatewayHandler.emailBody.smsEmailAddress=@example.com
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message body to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailBody.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailBody, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                // ---
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getStringProperty(device,PROP_emailBody_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                // ---
                String message = commandStr;
                int maxLen = RTConfig.getInt(PROP_emailBody_maxMessageLength, MAX_TEXT_LENGTH);
                if (StringTools.length(message) > maxLen) {
                    // exceeds maximum allowed SMS text length
                    Print.logWarn("Invalid SMS text command length: " + StringTools.length(message));
                }
                // ---
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, message);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                // ---
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = RTConfig.getString(PROP_emailBody_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                // ---
                int    maxLen   = RTConfig.getInt(PROP_emailBody_maxMessageLength, MAX_TEXT_LENGTH);
                String message  = SMSOutboundGateway.truncateMessage(smsMessage, maxLen);
                // ---
                return this.sendEmail(frEmail, toEmail, ""/*subject*/, message);
            }
        });

        /* EMail: standard "Subject" command */
        // Property:
        //   SmsGatewayHandler.emailSubject.smsEmailAddress=
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message subject to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailSubject.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailSubject, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                // ---
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getStringProperty(device,PROP_emailSubject_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                // ---
                String message = commandStr;
                int maxLen = RTConfig.getInt(PROP_emailSubject_maxMessageLength, MAX_TEXT_LENGTH);
                if (StringTools.length(message) > maxLen) {
                    // exceeds maximum allowed SMS text length
                    Print.logWarn("Invalid SMS text command length: " + StringTools.length(message));
                }
                // ---
                return this.sendEmail(frEmail, toEmail, message, ""/*body*/);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                // ---
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = RTConfig.getString(PROP_emailSubject_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                // ---
                int    maxLen   = RTConfig.getInt(PROP_emailSubject_maxMessageLength, MAX_TEXT_LENGTH);
                String message  = SMSOutboundGateway.truncateMessage(smsMessage, maxLen);
                // ---
                return this.sendEmail(frEmail, toEmail, message, ""/*body*/);
            }
        });

        /* HTTP: URL */
        // Property:
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/smsredirector/sendsms?flash=0&acctuser=user&tracking_Pwd=pass&source=5551212&destination=${mobile}&message=${message}
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/sendsms?user=%{user=pass}&pass=%{password=pass}&source=%{sender}&dest=${mobile}&text=${message}
        // Notes:
        //   - This outbound SMS method sends the SMS text in an HTTP "GET" request to the URL 
        //     specified on the property "SmsGatewayHandler.httpURL.url".  The following replacement
        //     variables may be specified in the URL string:
        //       %{sender}  - replaced with the Account "contactPhone" field contents
        //       %{mobile}  - replaced with the Device "simPhoneNumber" field contents
        //       %{message} - replaced with the SMS text/command to be sent to the device.
        //     It is expected that the server handling the request understands how to parse and
        //     interpret the various fields in the URL.
        //   - The "httpURL" outbound SMS gateway handler will also work for Clickatell http-mode.
        //     The repsonse will normally be an acknoeledgement ID, however an error response may
        //     look like the following:  "ERR: 113, Max message parts exceeded"
        SMSOutboundGateway.AddSMSGateway(GW_httpURL, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!isDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                // --- create URL
                String httpURL = this.getStringProperty(device, PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                httpURL = SMSOutboundGateway.EncodeUrlReplacementVars(
                    null/*account*/, device, null/*smsPhone*/, 
                    httpURL, 
                    commandStr, RTConfig.getInt(PROP_httpURL_maxMessageLength,MAX_TEXT_LENGTH));
                // --- send SMS
                return this._sendSMS(httpURL);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone) {
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                // create URL
                String httpURL = RTConfig.getString(PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                httpURL = SMSOutboundGateway.EncodeUrlReplacementVars(
                    account, device, smsPhone, 
                    httpURL, 
                    smsMessage, RTConfig.getInt(PROP_httpURL_maxMessageLength,MAX_TEXT_LENGTH));
                // --- send SMS
                return this._sendSMS(httpURL);
            }
            private DCServerFactory.ResultCode _sendSMS(String httpURL) {
                try {
                    // Clickatell:
                    //  - Success: ID: 3b912995fb87962fea42596099a3ecbb
                    //  - Failed : ERR: 001, Authentication failed
                    //  - Failed : ERR: 113, Max message parts exceeded
                    //  - Failed : ERR: 121, Destination mobile number blocked
                    Print.logInfo("SMS Gateway URL: " + httpURL);
                    byte response[] = HTMLTools.readPage_GET(httpURL, 10000);
                    String resp = StringTools.toStringValue(response);
                    int maxRespLen = 60;
                    if (resp.length() <= maxRespLen) {
                        Print.logInfo("SMS Gateway response (httpURL): " + resp);
                    } else {
                        String R = resp.substring(0, maxRespLen);
                        Print.logInfo("SMS Gateway response (httpURL): " + R + " ..."); // partial
                    }
                    if (httpURL.indexOf("clickatell.com") > 0) {
                        if (resp.startsWith("ID:")) {
                            // Clickatell indicated success
                            return DCServerFactory.ResultCode.SUCCESS;
                        } else
                        if (resp.indexOf("Authentication") > 0) {
                            Print.logError("SMS Gateway 'clickatell' Authentication Failure");
                            return DCServerFactory.ResultCode.GATEWAY_AUTH;
                        } else {
                            Print.logError("SMS Gateway 'clickatell' General Error");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        }
                    } else {
                        resp = resp.toUpperCase();
                        if (resp.indexOf("ERROR") >= 0) {
                            Print.logError("SMS Gateway 'ERROR' found");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if (resp.indexOf("FAIL") >= 0) {
                            Print.logError("SMS Gateway 'FAIL' found");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else {
                            return DCServerFactory.ResultCode.SUCCESS;
                        }
                    }
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
        });

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public String gwName = "";

    public SMSOutboundGateway()
    {
        // override
    }

    // ------------------------------------------------------------------------

    public void setName(String n)
    {
        this.gwName = StringTools.trim(n);
    }
    
    public String getName()
    {
        return this.gwName;
    }

    public String toString()
    {
        return "SMSGateway: " + this.getName();
    }

    // ------------------------------------------------------------------------

    public abstract DCServerFactory.ResultCode sendSMSCommand(Device device, String command);
    public abstract DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected String getFromEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(device.getAccount());
    }

    protected String getFromEmailAddress(Account account) 
    {
        if (account == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(account);
    }
    
    // ------------------------------------------------------------------------

    protected String getSmsEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        String toEmail = device.getSmsEmail();
        return toEmail;
    }

    protected String getSmsPhoneNumber(Device device) 
    {
        if (device == null) { return null; }
        String smsPhone = device.getSimPhoneNumber();
        return smsPhone;
    }

    // ------------------------------------------------------------------------

    protected String getStringProperty(Device device, String key, String dft) 
    {

        /* 1) check the BasicPrivateLabel properties */
        Account account = (device != null)? device.getAccount() : null;
        BasicPrivateLabel bpl = Account.getPrivateLabel(account);
        if (bpl != null) {
            String prop = bpl.getStringProperty(key,null);
            if (prop != null) {
                return prop;
            }
        }

        /* 2) check the DCServerConfig properties */
        DCServerConfig dcs = (device != null)? DCServerFactory.getServerConfig(device.getDeviceCode()) : null;
        if (dcs != null) {
            String prop = dcs.getStringProperty(key, dft); // will already check RTConfig properties
            Print.logInfo("DCServerConfig property '"+key+"' ==> " + prop);
            if (StringTools.isBlank(prop) && RTConfig.hasProperty(key)) {
                Print.logInfo("(RTConfig property '"+key+"' ==> " + RTConfig.getString(key,"") + ")");
            }
            return prop;
        }

        /* 3) check RTConfig properties */
        String prop = RTConfig.getString(key, dft);
        Print.logInfo("RTConfig property '"+key+"' ==> " + prop);
        return prop;

    }

    // ------------------------------------------------------------------------

    protected DCServerFactory.ResultCode sendEmail(String frEmail, String toEmail, String subj, String body) 
    {
        if (StringTools.isBlank(frEmail)) {
            Print.logError("'From' SMS Email address not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_FR;
        } else
        if (StringTools.isBlank(toEmail) || !CommandPacketHandler.validateAddress(toEmail)) {
            Print.logError("'To' SMS Email address invalid, or not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_TO;
        } else
        if (StringTools.isBlank(subj) && StringTools.isBlank(body)) {
            Print.logError("SMS Subject/Body string not specified");
            return DCServerFactory.ResultCode.INVALID_ARG;
        } else {
            try {
                Print.logInfo ("SMS email: From <" + frEmail + ">, To <" + toEmail + ">");
                Print.logDebug("  From   : " + frEmail);
                Print.logDebug("  To     : " + toEmail);
                Print.logDebug("  Subject: " + subj);
                Print.logDebug("  Message: " + body);
                SendMail.SmtpProperties smtpProps = null; // TODO:
                SendMail.send(frEmail,toEmail,null,null,subj,body,null,smtpProps);
                return DCServerFactory.ResultCode.SUCCESS;
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logWarn("SendMail error: " + t);
                return DCServerFactory.ResultCode.TRANSMIT_FAIL;
            }
        }
    }

    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = { "account", "a" };
    private static final String ARG_DEVICE[]    = { "device" , "d" };
    private static final String ARG_PHONE[]     = { "phone"  , "p" };
    private static final String ARG_URL[]       = { "url"    , "u" };
    private static final String ARG_MESSAGE[]   = { "message", "m", "msg" };
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        String accountID = RTConfig.getString(ARG_ACCOUNT,null);
        String deviceID  = RTConfig.getString(ARG_DEVICE,null);
        String smsPhone  = RTConfig.getString(ARG_PHONE,null);
        String httpURL   = RTConfig.getString(ARG_URL,"");
        String message   = RTConfig.getString(ARG_MESSAGE,"");

        /* get Account */
        Account account = null;
        if (!StringTools.isBlank(accountID)) {
            try {
                account = Account.getAccount(accountID); // may throw DBException
                if (account == null) {
                    Print.logError("Account-ID does not exist: " + accountID);
                    System.exit(99);
                }
            } catch (DBException dbe) {
                Print.logException("Error loading Account: " + accountID, dbe);
                System.exit(99);
            }
        }

        /* get Device */
        Device device = null;
        if (!StringTools.isBlank(deviceID)) {
            try {
                device = Device.getDevice(account, deviceID); // null if non-existent
                if (device == null) {
                    Print.logError("Device-ID does not exist: " + deviceID);
                    System.exit(99);
                }
            } catch (DBException dbe) {
                Print.logException("Error loading Device: " + deviceID, dbe);
                System.exit(99);
            }
        }

        /* encode */
        if ((account != null) && !StringTools.isBlank(httpURL)) {
            String newURL = SMSOutboundGateway.EncodeUrlReplacementVars(
                account, device, smsPhone,
                httpURL,
                message, 160);
            Print.sysPrintln("Encoded URL: " + newURL);
        }

    }
}
