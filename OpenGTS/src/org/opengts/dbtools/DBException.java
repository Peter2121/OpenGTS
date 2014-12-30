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
//  2006/04/08  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2012/09/02  Martin D. Flynn
//     -Added "isCauseCommunicationsException(...)"
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBException</code> is the general exception thrown for various encountered
*** SQL database errors.
**/

public class DBException
    extends Exception
{

    private static String CreateDBxceptionMessage(String msg, Throwable cause)
    {
        if (cause instanceof SQLException) {
            int errCode = ((SQLException)cause).getErrorCode();
            if (errCode > 0) {
                return msg + " [SQLErr=" + errCode + "]";
            }
        }
        return msg;
    }
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param msg  The message associated with this exception
    **/
    public DBException(String msg)
    {
        super(msg);
    }

    /**
    *** Constructor
    *** @param msg    The message associated with this exception
    *** @param cause  The reason for this exception
    **/
    public DBException(String msg, Throwable cause)
    {
        super(CreateDBxceptionMessage(msg,cause), cause);
    }
    
    // ----------------------------------------------------------------------------

    /**
    *** Returns true if the cause of this exception is an SQLException
    *** @return True if the cause of this exception is an SQLException
    **/
    public boolean isSQLException()
    {
        return (this.getCause() instanceof SQLException);
    }

    /**
    *** Returns true if the cause of this exception is a MySQL CommunicationsException
    *** IE. "com.mysql.jdbc.exceptions.jdbc4.CommunicationsException"
    *** @return True if the cause is a MySQL CommunicationsException
    **/
    public boolean isCauseCommunicationsException()
    {
        // com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure
        return DBException.isCommunicationsException(this.getCause());
    }

    /**
    *** Returns true if the specified exception is a MySQL CommunicationsException
    *** IE. "com.mysql.jdbc.exceptions.jdbc4.CommunicationsException"
    *** @param th  The Exception/Throwable to test
    *** @return True if the cause is a MySQL CommunicationsException
    **/
    public static boolean isCommunicationsException(Throwable th)
    {
        // Possible exceptions/messages:
        //  com.mysql.jdbc.CommunicationsException
        //  com.mysql.jdbc.exceptions.jdbc4.CommunicationsException
        // Notes:
        //  - CommunicationsException may be due to small values for "wait_timeout"
        if (th == null) {
            return false;
        } else {
            String cn  = StringTools.className(th);     // com.mysql.jdbc.exceptions.jdbc4.CommunicationsException
            String msg = th.getMessage().toLowerCase(); // Communications link failure
            if (( cn.indexOf("CommunicationsException"    ) >= 0) ||    // exception class name
                (msg.indexOf("CommunicationsException"    ) >= 0) ||    // part of message?
                (msg.indexOf("communications link failure") >= 0)   ) { // lower-case compare
                return true;
            } else {
                return false;
            }
        }
    }

    /**
    *** Returns true if the cause of this exception is an java.lang.OutOfMemoryError
    *** @return True if the cause of this exception is an java.lang.OutOfMemoryError
    **/
    public boolean isOutOfMemoryError()
    {
        return (this.getCause() instanceof java.lang.OutOfMemoryError);
    }

    // ----------------------------------------------------------------------------

    /**
    *** Gets the exception message
    *** @return The exception message
    **/
    public String getMessage()
    {
        return super.getMessage();
    }

    /**
    *** Gets the exception message, including the cause
    *** @return The exception message, including the cause
    **/
    public String getCauseMessage()
    {
        String superMsg = super.getMessage();
        Throwable cause = this.getCause();
        if (cause == null) {
            return superMsg;
        } else {
            return "[" + superMsg + "] " + cause.getMessage();
        }
    }

    // ----------------------------------------------------------------------------

    /**
    *** Prints a description of this exception to the logging output
    **/
    public void printException()
    {
        Throwable cause = this.getCause();
        if (cause instanceof SQLException) {
            Print.logSQLError(1, super.getMessage(), (SQLException)cause);
        } else {
            Print.logException(super.getMessage(), this);
        }
    }
    
    // ----------------------------------------------------------------------------

    /**
    *** Returns a String representation of this exception
    *** @return A String representation of this exception
    **/
    public String toString()
    {
        Throwable cause = this.getCause();
        if (cause != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(super.toString());
            sb.append(" [").append(cause.toString()).append("]");
            return sb.toString();
        } else {
            return super.toString();
        }
    }
    
    // ----------------------------------------------------------------------------

    /*
    public void printStackTrace()
    {
        Throwable cause = this.getCause();
        if (cause instanceof SQLException) {
            Print.logStackTrace(cause);
        } else {
            super.printStackTrace();
        }
    }
    */
    
}
