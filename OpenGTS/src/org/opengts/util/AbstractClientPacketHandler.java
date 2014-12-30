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
//  Partial implementation of a ClientPacketHandler
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2009/04/02  Martin D. Flynn
//     -Added 'getMinimumPacketLength' and 'getMaximumPacketLength'
//  2011/05/13  Martin D. Flynn
//     -Added several convenience functions.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.net.*;
import javax.net.*;

//import javax.net.ssl.*;

/**
*** An abstract implementation of the <code>ClientPacketHandler</code> interface
**/

public abstract class AbstractClientPacketHandler
    implements ClientPacketHandler
{

    // ------------------------------------------------------------------------

    public  static final int      PACKET_LEN_LINE_TERMINATOR = ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
    public  static final int      PACKET_LEN_END_OF_STREAM   = ServerSocketThread.PACKET_LEN_END_OF_STREAM;

    /* GMT/UTC timezone */
    public  static final TimeZone GMT_Timezone               = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean DebugMode    = false;

    /**
    *** Sets the global debug mode 
    **/
    public static void SetDebugMode(boolean debug)
    {
        AbstractClientPacketHandler.DebugMode = debug;
    }
    
    /**
    *** Gets the global debug mode 
    **/
    public static boolean GetDebugMode()
    {
        return AbstractClientPacketHandler.DebugMode;
    }
    
    /**
    *** Gets the global debug mode 
    **/
    public static boolean IsDebugMode()
    {
        return AbstractClientPacketHandler.DebugMode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static long                     SequenceCount   = 1L;
    private static Object                   SequenceLock    = new Object();

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long                            sequenceID      = 0L;

    private long                            sessStartTimeMS = 0L;
    private long                            sessStartTime   = 0L;
    private InetAddress                     inetAddr        = null;
    private String                          hostAddress     = null;
    private boolean                         isDuplex        = true;  // tcp
    private boolean                         isTextPackets   = false;
    private boolean                         promptEnabled   = true;

    private boolean                         terminateSess   = true; // always terminate by default

    private ServerSocketThread.SessionInfo  sessionInfo     = null;

    private int                             savedEventCount = 0; // DCS use only

    public AbstractClientPacketHandler()
    {
        super();
        synchronized (AbstractClientPacketHandler.SequenceLock) {
            this.sequenceID = AbstractClientPacketHandler.SequenceCount++;
        }
    }

    // ------------------------------------------------------------------------

    public long getSequenceID() 
    {
        return this.sequenceID;
    }

    public boolean equals(Object other) 
    {
        if (other instanceof AbstractClientPacketHandler) {
            return (this.getSequenceID() == ((AbstractClientPacketHandler)other).getSequenceID());
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the session info handler
    *** @param sessionInfo An implementation of the ServerSocketThread.SessionInfo interface
    **/
    public void setSessionInfo(ServerSocketThread.SessionInfo sessionInfo)
    {
        this.sessionInfo = sessionInfo;
    }
    
    /**
    *** Gets a reference to the ClientPacketHandler's session info implementation
    *** @return Reference to the session info object
    **/
    public ServerSocketThread.SessionInfo getSessionInfo()
    {
        return this.sessionInfo;
    }

    /**
    *** Returns name of the thread handling this client session
    *** @return The name of the thread handling this client session
    **/
    public String getThreadName()
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        Thread st = (si != null)? si.getSessionThread() : null;
        return (st != null)? st.getName() : null;
    }
    
    /**
    *** Gets the local port to which this socket is bound
    *** @return The local port to which this socket is bound
    **/
    public int getLocalPort()
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        return (si != null)? si.getLocalPort() : -1;
    }

    /**
    *** Gets the remote/client port used by the client to send the received packet
    *** @return The client remote port
    **/
    public int getRemotePort()
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        return (si != null)? si.getRemotePort() : -1;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the prompt enabled state
    *** @param enable  True to enable prompt, false to disable
    **/
    public void setPromptEnabled(boolean enable)
    {
        this.promptEnabled = enable;
    }

    /**
    *** Gets the prompt enabled state
    *** @return  True to enable prompt, false to disable
    **/
    public boolean getPromptEnabled()
    {
        return this.promptEnabled;
    }

    // ------------------------------------------------------------------------

    /**
    *** Write bytes to TCP output stream
    *** @param data  The data bytes to write
    *** @return True if bytes were written, false otherwise
    **/
    public boolean tcpWrite(byte data[])
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        return (si != null)? si.tcpWrite(data) : false;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Called when the session has started
    **/
    public void sessionStarted(InetAddress inetAddr, boolean isDuplex, boolean isText) 
    {
        this.sessStartTimeMS = DateTime.getCurrentTimeMillis();
        this.sessStartTime   = this.sessStartTimeMS / 1000L;
        this.inetAddr        = inetAddr;
        this.isDuplex        = isDuplex;
        this.isTextPackets   = isText;
        this.clearSavedEventCount();
        this.printSessionStart();
    }

    /**
    *** Displays the sesion startup message.
    *** (override to disable)
    **/
    protected void printSessionStart()
    {
        // Begin TCP session (ClientSession_000): xxx.xxx.xxx.xxx
        String sessType = this.getSessionType();
        String name     = StringTools.blankDefault(this.getThreadName(),"?");
        String host     = StringTools.trim(this.getHostAddress());
        StringBuffer sb = new StringBuffer();
        sb.append("Begin ");
        sb.append(sessType).append(" session (").append(name).append("): ").append(host);
        Print.logInfo(sb.toString());
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified session ID matches the current session ID
    *** @param sessionID  The session ID to test (specifying null should always return false)
    *** @return True if the session IDs match, false otherwise
    **/
    public boolean equalsSessionID(String sessionID)
    {

        /* no session ID specified? */
        if (sessionID == null) {
            Print.logWarn("No target SessionID");
            return false;
        }

        /* get current session ID */
        String thisSessID = this.getSessionID();
        if (thisSessID == null) {
            //Print.logWarn("Current SessionID is null (looking for " + sessionID + ")");
            return false;
        }

        /* compare */
        //Print.logInfo("Compare SessionIDs: " + sessionID + " ==? " + thisSessID);
        return thisSessID.equals(sessionID);

    }

    /**
    *** Returns the session ID (override only)
    *** @return The session ID
    **/
    protected String getSessionID()
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the session start time (milliseconds)
    **/
    public long getSessionStartTimeMS()
    {
        return this.sessStartTimeMS;
    }

    /**
    *** Returns the session start time (seconds)
    **/
    public long getSessionStartTime()
    {
        return this.sessStartTime;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the packets are text
    *** @return True if the packets are text
    **/
    protected boolean isTextPackets() 
    {
        return this.isTextPackets;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this session is duplex (ie TCP)
    **/
    public boolean isDuplex()
    {
        return this.isDuplex;
    }

    /**
    *** Returns true if this session is TCP
    **/
    public boolean isTCP()
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        return (si != null)? si.isTCP() : this.isDuplex;
    }

    /**
    *** Returns true if this session is UDP
    **/
    public boolean isUDP()
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        return (si != null)? si.isUDP() : !this.isTCP();
    }

    /**
    *** Returns true if this session is InputStream
    **/
    public boolean isInputStream()
    {
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        return (si != null)? si.isInputStream() : false/*unknown*/;
    }

    /**
    *** Gets the current session type name (ie. TCP, UDP)
    *** @return The current session type name
    **/
    public String getSessionType()
    {
        if (this.isTCP()) {
            return "TCP";
        } else
        if (this.isUDP()) {
            return "UDP";
        } else
        if (this.isInputStream()) {
            return "InputStream";
        } else {
            return "UNKNOWN";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the IP adress of the remote host
    *** @return The IP adress of the remote host
    **/
    public InetAddress getInetAddress()
    {
        return this.inetAddr;
    }

    /**
    *** Gets the IP adress of the remote host
    *** @return The IP adress of the remote host
    **/
    public String getHostAddress()
    {
        if ((this.hostAddress == null) && (this.inetAddr != null)) {
            this.hostAddress = this.inetAddr.getHostAddress();
        }
        return this.hostAddress;
    }

    /**
    *** Returns true if a remote host address is available
    *** @return True if a remote host address is available
    **/
    public boolean hasHostAddress()
    {
        return (this.getHostAddress() != null);
    }

    /**
    *** Gets the IP adress of the remote host
    *** @return The IP adress of the remote host
    **/
    public String getIPAddress()
    {
        return this.getHostAddress();
    }

    /**
    *** Returns true if a remote host address is available
    *** @return True if a remote host address is available
    **/
    public boolean hasIPAddress()
    {
        return (this.getHostAddress() != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the event count state to the specified value
    **/
    public void setSavedEventCount(int count)
    {
        this.savedEventCount = count;
    }

    /**
    *** Clears the event count state
    **/
    public void clearSavedEventCount()
    {
        this.setSavedEventCount(0);
    }

    /**
    *** Increments the event count state
    **/
    public void incrementSavedEventCount()
    {
        this.savedEventCount++;
    }

    /**
    *** Returns true if the current value of the event count state is greater-than zero
    **/
    public boolean hasSavedEvents()
    {
        return (this.savedEventCount > 0);
    }

    /**
    *** Gets the current value of the event count state
    **/
    public int getSavedEventCount()
    {
        return this.savedEventCount;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the client response port#
    **/
    public int getResponsePort()
    {
        return 0;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the minimum packet length
    **/
    public int getMinimumPacketLength()
    {
        // '-1' indicates that 'ServerSocketThread' should be used
        return -1;
    }

    /**
    *** Returns the maximum packet length
    **/
    public int getMaximumPacketLength()
    {
        // '-1' indicates that 'ServerSocketThread' should be used
        return -1;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the initial packet that should be sent to the device upon openning 
    *** the socket connection .
    **/
    public byte[] getInitialPacket() 
        throws Exception
    {
        return null;
    }

    /** 
    *** Returns the final packet that should be sent to the device before closing 
    *** the socket connection.
    **/
    public byte[] getFinalPacket(boolean hasError) 
        throws Exception
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Callback to obtain the length of the next packet, based on the provided partial
    *** packet data.
    **/
    public int getActualPacketLength(byte packet[], int packetLen) 
    {
        return this.isTextPackets? PACKET_LEN_LINE_TERMINATOR : packetLen;
    }

    /**
    *** Parse the provided packet information, and return any response that should
    *** be sent back to the remote device
    **/
    public abstract byte[] getHandlePacket(byte cmd[]) 
        throws Exception;

    /**
    *** Callback: timeout interrupt
    *** Called periodically during an idle read.  The periodic timeout is based on the value
    *** specified on the call to "<code>ServerSocketThread.setMinimuTimeoutIntervalMS</code>"
    **/
    public void idleTimeoutInterrupt()
    {
        // override
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the terminate-session state to the specified value
    **/
    public void forceCloseTCPSession()
    {
        this.setTerminateSession();
        ServerSocketThread.SessionInfo si = this.getSessionInfo();
        if (si != null) {
            si.forceCloseTCPSession();
        }
    }


    /**
    *** Sets the terminate-session state to the specified value
    **/
    public void setTerminateSession(boolean term)
    {
        this.terminateSess = term;
    }

    /**
    *** Sets the terminate-session state to true
    **/
    public void setTerminateSession()
    {
        this.setTerminateSession(true);
    }

    /**
    *** Clears the terminate-session state to false
    **/
    public void clearTerminateSession()
    {
        this.setTerminateSession(false);
    }

    /**
    *** Callback to determine if the current session should be terminated
    **/
    public boolean getTerminateSession() 
    {
        return this.terminateSess;
    }

    /**
    *** Callback to determine if the current session should be terminated
    **/
    public boolean terminateSession() // OBSOLETE
    {
        return this.getTerminateSession();
    }

    /**
    *** Callback just before the session is terminated
    **/
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        long sessStopTimeMS = DateTime.getCurrentTimeMillis();
        this.printSessionTerminated(sessStopTimeMS - this.sessStartTimeMS);
    }

    /**
    *** Displays the sesion startup message.
    *** (override to disable)
    **/
    protected void printSessionTerminated(long deltaMS)
    {
        // End TCP session (ClientSession_000): xxx.xxx.xxx.xxx [123 ms]
        String sessType = this.getSessionType();
        String name     = StringTools.blankDefault(this.getThreadName(),"?");
        String host     = StringTools.trim(this.getHostAddress());
        StringBuffer sb = new StringBuffer();
        sb.append("End ");
        sb.append(sessType).append(" session (").append(name).append("): ").append(host);
        if (deltaMS > 0L) {
            sb.append(" [").append(deltaMS).append(" ms]");
        }
        Print.logInfo(sb.toString());
    }

    // ------------------------------------------------------------------------

}
