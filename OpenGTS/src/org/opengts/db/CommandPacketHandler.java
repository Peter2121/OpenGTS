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
//  2009/07/01  Martin D. Flynn
//     -Initial release
//  2013/09/25  Martin D. Flynn
//     -Added support for "%COMMANDS%", "%SESSIONS%", and "%STACKTRACE%"
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public abstract class CommandPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  ARG_ACCOUNT             = DCServerFactory.CMDARG_ACCOUNT;
    public static final String  ARG_DEVICE              = DCServerFactory.CMDARG_DEVICE;
    public static final String  ARG_USER                = DCServerFactory.CMDARG_USER;

    public static final String  ARG_CMDTYPE             = DCServerFactory.CMDARG_CMDTYPE;   // "cmdtype"
    public static final String  ARG_CMDNAME             = DCServerFactory.CMDARG_CMDNAME;   // "cmdname"
    public static final String  ARG_ARG0[]              = new String[] { (DCServerFactory.CMDARG_ARG+"0"), DCServerFactory.CMDARG_ARG };
    public static final String  ARG_ARG1                = DCServerFactory.CMDARG_ARG + "1";
    public static final String  ARG_ARG2                = DCServerFactory.CMDARG_ARG + "2";
    public static final String  ARG_ARG3                = DCServerFactory.CMDARG_ARG + "3";
    public static final String  ARG_ARG4                = DCServerFactory.CMDARG_ARG + "4";
    public static final String  ARG_ARG5                = DCServerFactory.CMDARG_ARG + "5";
    public static final String  ARG_ARG6                = DCServerFactory.CMDARG_ARG + "6";
    public static final String  ARG_ARG7                = DCServerFactory.CMDARG_ARG + "7";
    public static final String  ARG_ARG8                = DCServerFactory.CMDARG_ARG + "8";
    public static final String  ARG_ARG9                = DCServerFactory.CMDARG_ARG + "9";
    public static final String  CMD_ARGS[] = {
        ARG_ARG0[0],
        ARG_ARG1,
        ARG_ARG2,
        ARG_ARG3,
        ARG_ARG4,
        ARG_ARG5,
        ARG_ARG6,
        ARG_ARG7,
        ARG_ARG8,
        ARG_ARG9,
    };

    public static final String  ARG_SERVER              = DCServerFactory.CMDARG_SERVER;
    
    public static final String  ARG_IP                  = "ip";
    public static final String  ARG_PHONE               = "phone";
    public static final String  ARG_LASTCONNECT         = "lastConnect";

    public static final String  ARG_RESULT              = DCServerFactory.RESPONSE_RESULT;
    public static final String  ARG_MESSAGE             = DCServerFactory.RESPONSE_MESSAGE;

    // ------------------------------------------------------------------------
    // internal commands
    
    public static final String  INTERNCMD_COMMANDS      = "%COMMANDS%";
    public static final String  INTERNCMD_STACKTRACE    = "%STACKTRACE%";
    public static final String  INTERNCMD_SESSIONS      = "%SESSIONS%";

    // ------------------------------------------------------------------------

    public static RTProperties setResult(RTProperties rtp, DCServerFactory.ResultCode result)
    {
        return CommandPacketHandler._setResult(1, rtp, result); // caller's frame
    }

    private static RTProperties _setResult(int frame, RTProperties rtp, DCServerFactory.ResultCode result)
    {
        if ((rtp != null) && (result != null)) {
            rtp.setString(ARG_RESULT , result.getCode());
            rtp.setString(ARG_MESSAGE, result.toString());
            if (!result.isSuccess()) {
                String msg = "Command Error: " + result.getCode() + " - " + result.getMessage();
                Print._log(Print.LOG_ERROR, frame + 1, msg);
            }
        }
        return rtp;
    }

    protected static byte[] RESULT(RTProperties rtp, DCServerFactory.ResultCode result)
    {
        CommandPacketHandler._setResult(1, rtp, result); // caller's frame
        return (rtp.toString() + "\n").getBytes();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Session 'terminate' indicator */
    private boolean         terminate           = false;

    /* session start time */
    private long            sessionStartTime    = 0L;

    /* session IP address */
    private InetAddress     inetAddress         = null;
    private String          ipAddress           = null;

    /* packet handler constructor */
    public CommandPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    public abstract String getServerName();

    public abstract DCServerFactory.ResultCode handleCommand(Device device, 
        String cmdType, String cmdName, String args[]);

    // ------------------------------------------------------------------------

    /* UDP response port */
    public int getResponsePort()
    {
        return super.getLocalPort();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);

        /* init */
        this.sessionStartTime = DateTime.getCurrentTimeSec();
        this.inetAddress      = inetAddr;
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;

        /* debug message */
        Print.logInfo("---- Begin Command Packet Handler: " + this.ipAddress);

    }
    
    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        
        Print.logInfo("---- End Command Packet Handler: " + this.ipAddress);
        try { Thread.sleep(10L); } catch (Throwable t) {}
        
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        return PACKET_LEN_LINE_TERMINATOR;
    }
            
    // ------------------------------------------------------------------------

    /* indicate that the session should terminate */
    public void setTerminateSession()
    {
        this.terminate = true;
    }

    /* indicate that the session should terminate */
    public boolean getTerminateSession()
    {
        return this.terminate;
    }

    /* indicate that the session should terminate */
    public boolean terminateSession() // OBSOLETE
    {
        return this.getTerminateSession();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (!ListTools.isEmpty(pktBytes)) {
            String cmd = StringTools.toStringValue(pktBytes);
            this.setTerminateSession();
            return this.parseCommand(cmd);
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Encode the specified command into an RTProperties instance
    *** @param cmdType  The command type
    *** @param cmdName  The command name
    *** @param cmdArgs  The array of command arguments
    *** @return The encoded RTProperties instance
    **/
    public static RTProperties EncodeCommand(String cmdType, String cmdName, String cmdArgs[])
    {
        RTProperties cmdRTP = new RTProperties();
        cmdRTP.setName("Command");
        cmdRTP.setString(ARG_CMDTYPE, cmdType);
        cmdRTP.setString(ARG_CMDNAME, cmdName);
        if (!ListTools.isEmpty(cmdArgs)) {
            for (int i = 0; i < cmdArgs.length; i++) {
                if (cmdArgs[i] != null) {
                    cmdRTP.setString(CMD_ARGS[i], cmdArgs[i]);
                }
            }
        }
        return cmdRTP;
    }

    // ------------------------------------------------------------------------
    
    private static Thread.State ThreadStateSortOrder[] = {
        Thread.State.RUNNABLE,
        Thread.State.NEW,
        Thread.State.WAITING,
        Thread.State.TIMED_WAITING,
        Thread.State.BLOCKED,
        Thread.State.TERMINATED,
    };

    /* parse and insert data record */
    // account=myaccount device=mydevice cmdType=config command="someCommand" arg="someArg"
    private byte[] parseCommand(String cmd)
    {
        RTProperties rtCmd = new RTProperties(cmd);
        Print.logInfo("Command: " + rtCmd);

        /* standard arguments */
        String accountID = rtCmd.getString(ARG_ACCOUNT,null);
        String userID    = rtCmd.getString(ARG_USER   ,null);
        String devIDList = rtCmd.getString(ARG_DEVICE ,null);

        /* command type/name/args */
        String cmdType   = rtCmd.getString(ARG_CMDTYPE,null);
        String cmdName   = rtCmd.getString(ARG_CMDNAME,null);
        String cmdArg0   = rtCmd.getString(ARG_ARG0   ,null);
        String cmdArg1   = rtCmd.getString(ARG_ARG1   ,null);
        String cmdArg2   = rtCmd.getString(ARG_ARG2   ,null);
        String cmdArg3   = rtCmd.getString(ARG_ARG3   ,null);
        String cmdArg4   = rtCmd.getString(ARG_ARG4   ,null);
        String cmdArg5   = rtCmd.getString(ARG_ARG5   ,null);
        String cmdArg6   = rtCmd.getString(ARG_ARG6   ,null);
        String cmdArg7   = rtCmd.getString(ARG_ARG7   ,null);
        String cmdArg8   = rtCmd.getString(ARG_ARG8   ,null);
        String cmdArg9   = rtCmd.getString(ARG_ARG9   ,null);

        /* command argument array */
        String cmdArgs[] = new String[] { 
            cmdArg0, cmdArg1, cmdArg2, cmdArg3, cmdArg4, cmdArg5, cmdArg6, cmdArg7, cmdArg8, cmdArg9 
            };
        int maxNdx = cmdArgs.length - 1;
        for (;(maxNdx >= 0) && (cmdArgs[maxNdx] == null); maxNdx--); // 'maxNdx' will point to the last non-null entry
        if (maxNdx < 0) {
            cmdArgs = new String[0];
        } else
        if (maxNdx < (cmdArgs.length - 1)) {
            String newArgs[] = new String[maxNdx + 1];
            System.arraycopy(cmdArgs, 0, newArgs, 0, newArgs.length);
            cmdArgs = newArgs;
        }

        /* get account record */
        Account account = null;
        if (StringTools.isBlank(accountID)) {
            //Print.logDebug("Account not specified");
        } else {
            accountID = accountID.trim(); // not blank
            try {
                account = Account.getAccount(accountID);
            } catch (DBException dbe) {
                // for some reason the MySQL "CommunicationsException" appears to
                // be a common occurance here.  Specifically check for this and try again.
                if (dbe.isCauseCommunicationsException()) {
                    Print.logWarn("CommunicationsException while reading Account (trying again) ...");
                    try {
                        account = Account.getAccount(accountID);
                    } catch (DBException dbe2) {
                        Print.logError("Unable to read Account: " + dbe2);
                        account = null;
                    }
                } else {
                    Print.logError("Unable to read Account: " + dbe);
                    account = null;
                }
            }
            if (account == null) {
                // -- AccountID specified, but not found
                Print.logError("Account not found: '%s'", accountID);
                return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_ACCOUNT);
            } else {
                Print.logDebug("Found Account: [%s] %s", account.getAccountID(), account.getDescription());
            }
        }

        /* get user record */
        User user = null;
        if (account == null) {
            //
        } else
        if (StringTools.isBlank(userID)) {
            //Print.logDebug("User not specified");
        } else {
            userID = userID.trim();
            try {
                user = User.getUser(account, userID);
            } catch (DBException dbe) {
                if (dbe.isCauseCommunicationsException()) {
                    Print.logWarn("CommunicationsException while reading Device (trying again) ...");
                    try {
                        user = User.getUser(account, userID);
                    } catch (DBException dbe2) {
                        Print.logError("Unable to read User: " + dbe2);
                        user = null;
                    }
                } else {
                    Print.logError("Unable to read User: " + dbe);
                    user = null;
                }
            }
        }

        /* check special commands */
        if ((cmdType != null) && cmdType.equalsIgnoreCase(DCServerConfig.COMMAND_INTERNAL)) {
            cmdName = StringTools.trim(cmdName);
            String dcsName = this.getServerName();
            DCServerConfig dcs = DCServerFactory.getServerConfig(dcsName);
            /* invalid command */
            Print.logError("Invalid Internal Command: Type=" + cmdType + ", Name=" + cmdName);
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_COMMAND);

        }

        /* no command type? */
        if (StringTools.isBlank(cmdType)) {
            Print.logError("Invalid Command: Type=" + cmdType + ", Name=" + cmdName);
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_COMMAND);
        }

        /* Account required beyond this point */
        if (account == null) {
            Print.logError("Account not specified");
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_ACCOUNT);
        }

        /* list of deviceIDs */
        String deviceIDs[] = StringTools.split(devIDList,',');
        if (ListTools.isEmpty(deviceIDs)) {
            Print.logError("No Device specified");
            return RESULT(rtCmd, DCServerFactory.ResultCode.INVALID_DEVICE);
        }

        // --------------------------------------------------
        // (Account account, User user, String deviceIDs[], 
        //  String cmdType, String cmdName, String cmdArgs[])

        /* loop through devices */
        int totalCount = 0;
        int sentCount  = 0;
        DCServerFactory.ResultCode lastSuccess = null;
        DCServerFactory.ResultCode lastFailure = null;
        Map<String,DCServerFactory.ResultCode> resultMap = new HashMap<String,DCServerFactory.ResultCode>();
        for (String deviceID : deviceIDs) {

            /* skip blank deviceIDs */
            if (StringTools.isBlank(deviceID)) {
                // quietly ignore blank deviceIDs
                continue;
            }
            deviceID = deviceID.trim();
            totalCount++;

            /* get device record */
            Device device = null;
            try {
                device = Device.getDevice(account, deviceID); // null if non-existent
            } catch (DBException dbe) {
                if (dbe.isCauseCommunicationsException()) {
                    Print.logWarn("CommunicationsException while reading Device (trying again) ...");
                    try {
                        device = Device.getDevice(account, deviceID); // null if non-existent
                    } catch (DBException dbe2) {
                        Print.logError("Unable to read Device: " + dbe2);
                        device = null;
                    }
                } else {
                    Print.logError("Unable to read Device: " + dbe);
                    device = null;
                }
            }
            if (device == null) {
                Print.logError("Device not found: '%s/%s'", accountID, deviceID);
                lastFailure = DCServerFactory.ResultCode.INVALID_DEVICE;
                resultMap.put(deviceID, lastFailure);
                continue;
            } else {
                Print.logDebug("Found Device: [%s:%s] %s", device.getDeviceID(), device.getUniqueID(), device.getDescription());
            }

            /* set device properties */
            rtCmd.setString(ARG_SERVER, device.getDeviceCode());
            //rtCmd.setString(ARG_IP          , StringTools.trim(device.getIpAddressCurrent()));
            //rtCmd.setString(ARG_PHONE       , device.getSimPhoneNumber());
            //rtCmd.setLong(  ARG_LASTCONNECT , device.getLastTotalConnectTime());

            /* handle command */
            DCServerFactory.ResultCode result = this.handleCommand(device, cmdType, cmdName, cmdArgs);
            if (result != null) {
                if (result.isSuccess()) { 
                    sentCount++; // count success
                    lastSuccess = result;
                } else {
                    lastFailure = result;
                }
                resultMap.put(deviceID, result);
            } else {
                lastFailure = DCServerFactory.ResultCode.INVALID_COMMAND;
                resultMap.put(deviceID, lastFailure);
            }

        } // loop through devices
        Print.logInfo("Commands sent: "+sentCount+" of "+totalCount);

        /* return result */
        int resultCount = ListTools.size(resultMap);
        if (resultCount <= 0) {
            // ((totalCount == 0) && (lastSuccess == null) && (lastFailure == null))
            // -- nothing sent, no result 
            Print.logError("No commands sent");
            return RESULT(rtCmd, DCServerFactory.ResultCode.EMPTY_REQUEST);
        } else
        if (lastSuccess != null) {
            // (resultCount > 0)
            // -- at least one success, ignore failures
            return RESULT(rtCmd, lastSuccess);
        } else
        if (lastFailure != null) {
            // ((resultCount > 0) && (lastSuccess == null))
            // -- we had only failures
            return RESULT(rtCmd, lastFailure);
        } else {
            // -- will not occur
            return RESULT(rtCmd, DCServerFactory.ResultCode.UNKNOWN);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* validate email address */
    public static boolean validateAddress(String addr)
    {
        try {
            return SendMail.validateAddress(addr);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return false;
        }
    }

    /* validate the syntax of the specified list of multiple email addresses */
    public static boolean validateAddresses(String addrs)
    {
        try {
            return SendMail.validateAddresses(addrs);
        } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
            // this will fail if JavaMail support for SendMail is not available.
            Print.logWarn("SendMail error: " + t);
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /* return command 'From' email address */
    public static String getFromEmailCommand(Account account)
    {
        return CommandPacketHandler.getFromEmailCommand(Account.getPrivateLabel(account));
    }

    /* return command 'From' email address */
    public static String getFromEmailCommand(BasicPrivateLabel bpl)
    {
        if (bpl != null) {
            String email = bpl.getEMailAddress(BasicPrivateLabel.EMAIL_TYPE_COMMAND);
            return bpl.getSmtpProperties().getUserEmail(email);
        } else {
            return SendMail.getDefaultUserEmail(null); // BPL Default
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
