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
//  Remote Log Query Server (EXPERIMENTAL)
// ----------------------------------------------------------------------------
// Change History:
//  2012/12/24  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.net.*;

/**
*** Remote Log server
**/

public class RemoteLogServer
{


    public  static final String     VERSION             = "0.1.1";
    
    public  static final String     LOG_OUTPUT_BEGIN    = "<LogOutput>\n";
    public  static final String     LOG_OUTPUT_END      = "</LogOutput>\n";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the InetAddress for the specified 'bindHost'
    *** @param bindHost  The bind host name
    *** @return The InetAddress for the specified host, or null if the specified bind host name
    ***         is not found in the local NetworkInterface addresses.
    **/
    public static InetAddress GetLocalBindAddress(String bindHost)
    {
        InetAddress bindAddr = null;
        if (!StringTools.isBlank(bindHost)) {
            try {
                bindAddr = InetAddress.getByName(bindHost);
                if (!ServerSocketThread.isLocalInterfaceAddress(bindAddr)) {
                    System.err.println("ERROR: BindAddress not found in NetworkInterface - " + bindAddr);
                }
            } catch (IOException ioe) {
                System.err.println("ERROR: BindAddress not found - " + bindHost);
                bindAddr = null;
            }
        }
        return bindAddr;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private CachedLogOutputStream   logOutputStream     = null;
    
    private InetAddress             bindAddr            = null;
    private int                     listenPort          = 0;

    private ServerSocketThread      logSST              = null;

    private String                  header              = null;

    /**
    *** Constructor
    *** @param port  The port on which this server listens for incoming connection requests.
    *** @param clos  The <code>CachedLogOutputStream</code> instance that contains the log buffer
    **/
    public RemoteLogServer(int port, CachedLogOutputStream clos)
    {
        this((InetAddress)null, port, clos);
    }

    /**
    *** Constructor
    *** @param bindHost The local network interface on which this server will be bound.
    *** @param port     The port on which this server listens for incoming connection requests.
    *** @param clos     The <code>CachedLogOutputStream</code> instance that contains the log buffer
    **/
    public RemoteLogServer(String bindHost, int port, CachedLogOutputStream clos)
    {
        this(GetLocalBindAddress(bindHost), port, clos);
    }

    /**
    *** Constructor
    *** @param bindAddr The local network interface on which this server will be bound.
    *** @param port     The port on which this server listens for incoming connection requests.
    *** @param clos     The <code>CachedLogOutputStream</code> instance that contains the log buffer
    **/
    public RemoteLogServer(InetAddress bindAddr, int port, CachedLogOutputStream clos)
    {

        /* validate parameters */
        if (port <= 0) {
            throw new IllegalArgumentException("Invalid port specification: " + port);
        } else
        if (clos == null) {
            throw new IllegalArgumentException("Specified CachedLogOutputStream is null");
        }

        /* save args */
        this.bindAddr        = bindAddr;
        this.listenPort      = port;
        this.logOutputStream = clos;

    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the local network interface on which this server will be bound.
    *** May be null to indicate that the default bind address should be used.
    *** @return The local network interface on which this server will be bound.
    **/
    public InetAddress getBindAddress()
    {
        return this.bindAddr;
    }

    /** 
    *** Gets the port on which this server listens for incoming connection requests.
    *** @return The port on which this server listens for incoming connection requests.
    **/
    public int getListenPort()
    {
        return this.listenPort;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the header to use for remote access
    *** @param h  The header string
    **/
    public void setHeader(String h) 
    {
        this.header = h;
    }

    /** 
    *** Gets the header to use for remote access
    *** @return  The header string
    **/
    public String getHeader()
    {
        return this.header;
    }

    /**
    *** Returns true if a header is defined
    *** @return  True if a header is defined
    **/
    public boolean hasHeader()
    {
        return !StringTools.isBlank(this.header);
    }

    // ------------------------------------------------------------------------

    /**
    *** Starts the remote log server
    **/
    public boolean startServer()
    {

        /* already started? */
        if (this.logSST != null) {
            Print.sysPrintln("ERROR: RemoteLogServer already started");
            return false;
        }

        /* create/init ServerSocketThread */
        InetAddress baddr = this.getBindAddress(); // may be null
        String      bhost = (baddr != null)? baddr.getHostName() : "";
        int         lport = this.getListenPort();
        try {
            this.logSST = new ServerSocketThread(baddr, lport);
            this.logSST.setTextPackets(true);               // ASCII
            this.logSST.setPrompt(">> ");                   // prompt
            this.logSST.setBackspaceChar(new int[] { '\b' });
            this.logSST.setLineTerminatorChar(new int[] { '\r', '\n' });
            this.logSST.setIgnoreChar(new int[] { '\n' });
            this.logSST.setMaximumPacketLength(1000);       // safety net
            this.logSST.setMinimumPacketLength(1);
            this.logSST.setIdleTimeout(600000);             // time between packets
            this.logSST.setPacketTimeout(600000);           // time from start of packet to packet completion
            this.logSST.setSessionTimeout(1200000);         // time for entire session
            this.logSST.setLingerTimeoutSec(0);             // SOLINGER not necessary
            this.logSST.setTerminateOnTimeout(true);        // stop the client on timeout
            this.logSST.setClientPacketHandler(new RemoteLogHandler());
            this.logSST.setLoggingEnabled(false);           // don't perform 'logDebug' or 'logInfo' logging
        } catch (java.net.BindException be) {
            String msg = be.getMessage();
            Print.sysPrintln("Unable to bind to '"+bhost+":"+lport+"' - " + msg);
            return false;
        } catch (java.io.IOException ioe) {
            if (ioe.getCause() instanceof java.net.BindException) {
                String msg = ioe.getCause().getMessage();
                Print.sysPrintln("Unable to bind to '"+bhost+":"+lport+"' - " + msg);
            } else {
                Print.sysPrintln("Error initializing ServerSocketThread: " + ioe);
            }
            return false;
        } catch (Throwable th) {
            Print.sysPrintln("Unknown error: " + th);
            th.printStackTrace();
            return false;
        }

        /* start ServerSocketThread */
        Print.sysPrintln("Starting RemoteLogServer listener - '"+bhost+":"+lport+"'");
        this.logSST.start();
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static CachedLogOutputStream  testLogStream = null;

    public static CachedLogOutputStream GetCachedLogOutputStream(boolean allowTest)
    {
        //CachedLogOutputStream out = null;
        OutputStream out = Print.getRedirectedOutputStream();
        if (out instanceof CachedLogOutputStream) {
            return (CachedLogOutputStream)out;
        } else {
            if ((RemoteLogServer.testLogStream == null) && allowTest) {
                RemoteLogServer.testLogStream = new CachedLogOutputStream();
            }
            return RemoteLogServer.testLogStream;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public class RemoteLogHandler
        extends AbstractClientPacketHandler
    {
        public RemoteLogHandler() {
            super();
            super.setTerminateSession(false);
        }
        public byte[] getInitialPacket() {
            if (RemoteLogServer.this.hasHeader()) {
                String h = RemoteLogServer.this.getHeader();
                StringBuffer sb = new StringBuffer();
                sb.append("-------------------------------------------------\n");
                sb.append(h);
                if (!h.endsWith("\n")) {
                    sb.append("\n");
                }
                sb.append("-------------------------------------------------\n");
                return sb.toString().getBytes();
            } else {
                return null;
            }
        }
        public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText) {
            super.sessionStarted(inetAddr, isTCP, isText);
            Print.sysPrintln("Session started");
        }
        public void sessionTerminated(Throwable err, long readCount, long writeCount) {
            super.sessionTerminated(err, readCount, writeCount);
            Print.sysPrintln("Session terminated");
        }
        public int getActualPacketLength(byte packet[], int packetLen) {
            return ServerSocketThread.PACKET_LEN_LINE_TERMINATOR;
        }
        public byte[] getHandlePacket(byte pkt[]) throws Exception {
            try {
                return this._getHandlePacket(pkt);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw ex;
            } catch (Error er) {
                er.printStackTrace();
                throw er;
            }
        }
        private byte[] _getHandlePacket(byte pkt[]) throws Exception {
            String s = StringTools.toStringValue(pkt).trim();
            //Print.sysPrintln("Command: " + s);
            CachedLogOutputStream clos = GetCachedLogOutputStream(false);
            if (clos == null) {
                return "ERROR: CachedLogOutputStream is not defined.".getBytes();
            }
            String cmd = StringTools.toStringValue(pkt).trim();
            if (cmd.equals("")) {
                return null; // no command specified
            } else
            if (cmd.equalsIgnoreCase("get")) {
                if (clos.size() > 0) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(LOG_OUTPUT_BEGIN);
                    for (;;) {
                        String r = clos.readLine();
                        if (r != null) {
                            sb.append(r);
                            continue;
                        }
                        break;
                    }
                    sb.append(LOG_OUTPUT_END);
                    return sb.toString().getBytes();
                } else {
                    return null;
                }
            } else
            if (cmd.equalsIgnoreCase("reset") || cmd.equalsIgnoreCase("clear")) {
                clos.reset();
                return "Reset\n\n".getBytes();
            } else
            if (StringTools.startsWithIgnoreCase(cmd,"sample")) {
                int p = cmd.indexOf(" ");
                final int count = (p < 0)? 3 : StringTools.parseInt(cmd.substring(p+1),3);
                Runnable sampleThread = new Runnable() {
                    public void run() {
                        int C = (count < 30)? count : 30;
                        for (int i = 0; i < C; i++) {
                            switch (i % 3) {
                                case 0: Print.logInfo( i + "] Sample INFO message" ); break;
                                case 1: Print.logWarn( i + "] Sample WARN message" ); break;
                                case 2: Print.logError(i + "] Sample ERROR message"); break;
                            }
                            try { Thread.sleep(1000L); } catch (Throwable th) {/*ignore*/}
                        }
                    }
                };
                (new Thread(sampleThread)).start();
                return "Loading log samples\n\n".getBytes();
            } else
            if (StringTools.startsWithIgnoreCase(cmd,"prompt")) {
                int p = cmd.indexOf(" ");
                if (p < 0) {
                    this.setPromptEnabled(true);
                    return null;
                } else {
                    String state = cmd.substring(p+1).trim();
                    boolean enabled = StringTools.parseBoolean(state,true);
                    this.setPromptEnabled(enabled);
                    return null;
                }
            }
            //else 
            //if (cmd.equalsIgnoreCase("help")) {
            //    StringBuffer sb = new StringBuffer();
            //    sb.append("Help:\n");
            //    sb.append("  get    Gets/Resets the latest log contents\n");
            //    sb.append("  reset  Clears/Resets the log contents\n");
            //    sb.append("  help   This help\n");
            //    sb.append("\n");
            //    return sb.toString().getBytes();
            //}
            return ("Unknown Command: " + cmd + "\n\n").getBytes();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        if (!Print.isRemoteLogging()) {
            //System.out.println("Starting RemoteLogServer ...");
            String host = RTConfig.getString("host",null);
            int    port = RTConfig.getInt("port",12345);
            Print.closeRedirectedOutputStream();
            Print.setRedirectedOutput(new CachedLogOutputStream());
            CachedLogOutputStream clos = GetCachedLogOutputStream(true);
            if (clos == null) {
                Print.sysPrintln("'Print' does not define CachedLogOutputStream");
            }
            RemoteLogServer rls = new RemoteLogServer(host,port,clos);
            rls.setHeader("RemoteLogServer test [Version " + VERSION + "]");
            rls.startServer();
        } else {
            System.out.println("RemoteLogServer already running ...");
        }
        Print.sysPrintln("Waiting ...");

    }

}
