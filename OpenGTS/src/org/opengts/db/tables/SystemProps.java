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
//  2008/03/12  Martin D. Flynn
//     -Initial release
//  2008/05/20  Martin D. Flynn
//     -Added command line options for updating the 'version' properties.
//  2010/04/11  Martin D. Flynn
//     -Added support for PrivateAdmin users.
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.Version;
import org.opengts.db.*;

public class SystemProps
    extends DBRecord<SystemProps>
{

    // ------------------------------------------------------------------------

    public static final String GTS_VERSION              = "version.gts";
    public static final String DMTP_VERSION             = "version.dmtp";
    
    public static final String READ_ONLY_PROPS[] = {
        GTS_VERSION,
        DMTP_VERSION
    };
    
    public static boolean IsReadOnlyProperty(String propID)
    {
        return ListTools.containsIgnoreCase(READ_ONLY_PROPS, propID);
    }

    // ------------------------------------------------------------------------

    public static final String TYPE_STRING              = "String";
    public static final String TYPE_INTEGER             = "Integer";
    public static final String TYPE_LONG                = "Long";
    public static final String TYPE_DOUBLE              = "Double";
    public static final String TYPE_ENUM_               = "Enum";
    public static final String TYPE_ENUM(String E[]) 
    { 
        StringBuffer sb = new StringBuffer();
        sb.append(TYPE_ENUM_);
        sb.append("[");
        if (!ListTools.isEmpty(E)) {
            for (int i = 0; i < E.length; i++) {
                if (i > 0) { sb.append(","); }
                sb.append(StringTools.trim(E[i]));
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    private static final String _TABLE_NAME             = "SystemProps";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_propertyID           = "propertyID";
    public static final String FLD_dataType             = "dataType";
    public static final String FLD_value                = "value";
    private static DBField FieldInfo[] = {
        // Property fields
        new DBField(FLD_propertyID      , String.class  , DBField.TYPE_PROP_ID()   , "Property ID"  , "key=true"),
        new DBField(FLD_dataType        , String.class  , DBField.TYPE_STRING(80)  , "Data Type"    , "edit=2 "),
        new DBField(FLD_value           , String.class  , DBField.TYPE_TEXT        , "Value"        , "edit=2 utf8=true"),
        // Common fields
        newField_description(),
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DBRecordKey<SystemProps>
    {
        public Key() {
            super();
        }
        public Key(String versId) {
            super.setKeyValue(FLD_propertyID, ((versId != null)? versId.toLowerCase() : ""));
        }
        public DBFactory<SystemProps> getFactory() {
            return SystemProps.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<SystemProps> factory = null;
    public static DBFactory<SystemProps> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                SystemProps.TABLE_NAME(), 
                SystemProps.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                SystemProps.class, 
                SystemProps.Key.class,
                false/*editable*/,false/*viewable*/);
        }
        return factory;
    }

    /* Bean instance */
    public SystemProps()
    {
        super();
    }

    /* database record */
    public SystemProps(SystemProps.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(SystemProps.class, loc);
        return i18n.getString("SystemProps.description", 
            "This table defines " +
            "system-wide installation property key/values."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the Property ID for this record */
    public String getPropertyID()
    {
        String v = (String)this.getFieldValue(FLD_propertyID);
        return StringTools.trim(v);
    }

    /* set the Property ID for this record */
    private void setPropertyID(String v)
    {
        this.setFieldValue(FLD_propertyID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the property data-type */
    public String getDataType()
    {
        String v = (String)this.getFieldValue(FLD_dataType);
        return StringTools.trim(v);
    }

    /* set the property data-type */
    public void setDataType(String v)
    {
        this.setFieldValue(FLD_dataType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the property value */
    public String getValue()
    {
        String v = (String)this.getFieldValue(FLD_value);
        return StringTools.trim(v);
    }

    /* set the property value */
    public void setValue(String v)
    {
        this.setFieldValue(FLD_value, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription(this.getPropertyID()); // Warning: will be lower-case
        this.setDataType(""); // defaults to "String"
        this.setValue(""); // clear value
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if this propertyID is in the "Read-Only" list
    **/
    public boolean isReadOnly()
    {
        return SystemProps.IsReadOnlyProperty(this.getPropertyID());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the property String value 
    **/
    public String getStringValue()
    {
        return this.getValue();
    }

    /**
    *** Gets the property value as an Integer
    **/
    public int getIntValue(int dft)
    {
        return StringTools.parseInt(this.getValue(), dft);
    }

    /**
    *** Gets the property value as a Long */
    public long getLongValue(long dft)
    {
        return StringTools.parseLong(this.getValue(), dft);
    }

    /**
    *** Gets the property value as a Float 
    **/
    public float getFloatValue(float dft)
    {
        return StringTools.parseFloat(this.getValue(), dft);
    }

    /**
    *** Gets the property value as a Double 
    **/
    public double getDoubleValue(double dft)
    {
        return StringTools.parseDouble(this.getValue(), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the property String value 
    **/
    public void setStringValue(String v)
    {
        this.setValue(v);
    }

    /**
    *** Sets the property value as an Integer 
    **/
    public void setIntValue(int v)
    {
        this.setValue(String.valueOf(v));
    }

    /**
    *** Sets the property value as a Long 
    **/
    public void setLongValue(long v)
    {
        this.setValue(String.valueOf(v));
    }

    /**
    *** Sets the property value as a Float 
    **/
    public void setFloatValue(float v)
    {
        this.setValue(String.valueOf(v));
    }

    /**
    *** Sets the property value as a Double 
    **/
    public void setDoubleValue(double v)
    {
        this.setValue(String.valueOf(v));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Returns an array of SystemProps keys
    **/
    public static String[] getSystemPropsKeyArray()
        throws DBException
    {
        Collection<String> list = SystemProps.getSystemPropsKeyList();
        if (list != null) {
            return list.toArray(new String[list.size()]);
        } else {
            return new String[0];
        }
    }

    /** 
    *** Return list of SystemProps keys
    **/
    public static Collection<String> getSystemPropsKeyList()
        throws DBException
    {

        /* select */
        // DBSelect: SELECT propertyID FROM SystemProps ORDER BY propertyID
        DBSelect<SystemProps> dsel = new DBSelect<SystemProps>(SystemProps.getFactory());
        dsel.setSelectedFields(SystemProps.FLD_propertyID);
        dsel.setOrderByFields(SystemProps.FLD_propertyID);

        /* read SystemProps keys */
        Collection<String> keyList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String propID = rs.getString(SystemProps.FLD_propertyID);
                keyList.add(propID);
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting SystemProps keys", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return */
        return keyList;

    }

    // ------------------------------------------------------------------------

    /** 
    *** Return an RTProperties instance of the properties in the SystemProps table
    **/
    public static RTProperties getRTProperties()
        throws DBException
    {

        /* select */
        // DBSelect: SELECT propertyID,value FROM SystemProps ORDER BY propertyID
        DBSelect<SystemProps> dsel = new DBSelect<SystemProps>(SystemProps.getFactory());
        dsel.setSelectedFields(SystemProps.FLD_propertyID, SystemProps.FLD_value);
        dsel.setOrderByFields(SystemProps.FLD_propertyID);

        /* read SystemProps keys */
        RTProperties rtp = new RTProperties();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String propID = rs.getString(SystemProps.FLD_propertyID);
                String value  = rs.getString(SystemProps.FLD_value);
                rtp.setString(propID, value);
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting SystemProps keys", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return */
        return rtp;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified property key exists
    **/
    public static boolean hasProperty(String propID)
        throws DBException
    {
        SystemProps.Key propKey = new SystemProps.Key(propID);
        return propKey.exists();
    }

    /**
    *** Returns the record instance for the specified property key
    **/
    public static SystemProps getProperty(String propKey)
        throws DBException
    {
        try {
            return SystemProps.getProperty(propKey, false); // do not create
        } catch (DBNotFoundException dbnfe) {
            return null; // not found
        }
    }

    /**
    *** Gets/Creates the SystemProps instance for specified property key
    *** @param propID  The property key
    *** @param create  True to create new <code>propID</code> entry, false to read existing.
    *** @throws DBException if <code>create</code> specified and <code>propID</code> already
    ***     exists, or if <code>create</code> not specified and <code>propID</code> not found.
    **/
    public static SystemProps getProperty(String propID, boolean create)
        throws DBException
    {

        /* key specified? */
        if (StringTools.isBlank(propID)) {
            // invalid key specified
            throw new DBNotFoundException("Propery key/id not specified.");
        }

        /* get/create */
        SystemProps prop = null;
        SystemProps.Key propKey = new SystemProps.Key(propID);
        if (!propKey.exists()) {
            if (create) {
                prop = propKey.getDBRecord();
                prop.setCreationDefaultValues();
                prop.setDescription(propID); // set description to original key case
                return prop; // not yet saved!
            } else {
                throw new DBNotFoundException("Property-ID does not exists: " + propKey);
            }
        } else
        if (create) {
            // we've been asked to create the property, and it already exists
            throw new DBAlreadyExistsException("Property-ID already exists: " + propKey);
        } else {
            prop = propKey.getDBRecord(true);
            if (prop == null) {
                throw new DBException("Unable to read existing Property-ID: " + propKey);
            }
            return prop;
        }

    }

    /**
    *** Creates/Saves a new SystemProps key
    *** @param propID  The property key
    *** @param type    The property data type (defaults to "String" is blank)
    *** @param desc    The property description (defaults to <code>propID</code> if blank)
    *** @return The new SystemProps instance (saved)
    *** @throws DBException If <code>propID</code> already exists, or unable to create new key
    **/
    public static SystemProps createNewProperty(String propID, String type, String desc)
        throws DBException
    {
        if (!StringTools.isBlank(propID)) {
            SystemProps prop = SystemProps.getProperty(propID, true/*create*/); // does not return null
            prop.setDataType(StringTools.blankDefault(type,TYPE_STRING));
            if (!StringTools.isBlank(desc)) {
                prop.setDescription(desc);
            }
            prop.save();
            return prop;
        } else {
            throw new DBException("Invalid PropertyID specified");
        }
    }

    /** 
    *** Sets the String value for specified property ID, creating the property-ID if it
    *** does not already exist.
    *** @return True if the property ID was successfully saved, false otherwise
    **/
    public static boolean setProperty(String propID, String value)
    {

        /* PropertyID exists? */
        boolean propExists;
        try {
            propExists = SystemProps.hasProperty(propID);
        } catch (DBException dbe) {
            Print.logException("Checking PropertyID existance: " + propID, dbe);
            return false;
        }

        /* get/create property */
        SystemProps prop;
        try {
            prop = SystemProps.getProperty(propID, !propExists); // create if not exist
            if (prop == null) {
                // should not occur
                Print.logError("Unexpected null PropertyID: " + propID);
                return false;
            }
        } catch (DBException dbe) {
            Print.logException("Getting/Creating PropertyID: " + propID, dbe);
            return false;
        }

        /* update value */
        try {
            prop.setValue(value);
            prop.save();
            return true;
        } catch (DBException dbe) {
            Print.logException("Saving PropertyID: " + propID, dbe);
            return false;
        }

    }

    // ------------------------------------------------------------------------

    /* get string value for specified property */
    public static String getStringValue(String propID, String dft)
    {
        try {
            SystemProps prop = SystemProps.getProperty(propID);
            return (prop != null)? prop.getValue() : dft;
        } catch (DBException dbe) {
            return dft;
        }
    }

    /* return the property value as an int */
    public static int getIntValue(String propID, int dft)
    {
        String strVal = SystemProps.getStringValue(propID, null);
        return StringTools.parseInt(strVal, dft);
    }

    /* return the property value as a long */
    public static long getLongValue(String propID, long dft)
    {
        String strVal = SystemProps.getStringValue(propID, null);
        return StringTools.parseLong(strVal, dft);
    }

    /* return the property value as a float */
    public static float getFloatValue(String propID, float dft)
    {
        String strVal = SystemProps.getStringValue(propID, null);
        return StringTools.parseFloat(strVal, dft);
    }

    /* return the property value as a double */
    public static double getDoubleValue(String propID, double dft)
    {
        String strVal = SystemProps.getStringValue(propID, null);
        return StringTools.parseDouble(strVal, dft);
    }

    // ------------------------------------------------------------------------

    /* set string value for specified property */
    public static boolean setStringValue(String propID, String value)
    {
        return SystemProps.setProperty(propID, value);
    }

    /* return the property value as an int */
    public static boolean setIntValue(String propID, int v)
    {
        return SystemProps.setStringValue(propID, String.valueOf(v));
    }

    /* return the property value as a long */
    public static boolean setLongValue(String propID, long v)
    {
        return SystemProps.setStringValue(propID, String.valueOf(v));
    }

    /* return the property value as a float */
    public static boolean setFloatValue(String propID, float v)
    {
        return SystemProps.setStringValue(propID, String.valueOf(v));
    }

    /* return the property value as a double */
    public static boolean setDoubleValue(String propID, double v)
    {
        return SystemProps.setStringValue(propID, String.valueOf(v));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get GTS version */
    public static String getGTSVersion(String dft)
    {
        return SystemProps.getStringValue(SystemProps.GTS_VERSION, dft);
    }

    /* get GTS version */
    public static String getGTSVersion()
    {
        return SystemProps.getGTSVersion("");
    }

    // ------------------------------------------------------------------------

    /* get DMTP version */
    public static String getDMTPVersion(String dft)
    {
        return SystemProps.getStringValue(SystemProps.DMTP_VERSION, dft);
    }

    /* get DMTP version */
    public static String getDMTPVersion()
    {
        return SystemProps.getDMTPVersion("");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* update version */
    public static void updateVersions()
    {

        /* OpenGTS version */
        String gtsCurrVersion = Version.getVersion();
        String gtsPropVersion = SystemProps.getGTSVersion();
        if (!gtsCurrVersion.equals(gtsPropVersion)) {
            Print.logInfo("Updating GTS Version: " + gtsCurrVersion);
            SystemProps.setStringValue(SystemProps.GTS_VERSION, gtsCurrVersion);
        }

        /* OpenDMTP version */
        try { 
            // lazily bind to OpenDMTP Version, in case it is not included in this installation
            MethodAction dmtpVersMeth = new MethodAction("org.opendmtp.server.Version", "getVersion");
            String dmtpCurrVersion = (String)dmtpVersMeth.invoke();
            String dmtpPropVersion = SystemProps.getDMTPVersion();
            if (!dmtpCurrVersion.equals(dmtpPropVersion)) {
                Print.logInfo("Updating DMTP Version: " + dmtpCurrVersion);
                SystemProps.setStringValue(SystemProps.DMTP_VERSION, dmtpCurrVersion);
            }
        } catch (Throwable th) {
            // ignore
        }
            

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        Print.sysPrintln("Property '"+GTS_VERSION +"' value: " + SystemProps.getStringValue(SystemProps.GTS_VERSION ,"undefined"));
        Print.sysPrintln("Property '"+DMTP_VERSION+"' value: " + SystemProps.getStringValue(SystemProps.DMTP_VERSION,"undefined"));
        if (RTConfig.getBoolean("update",false)) {
            SystemProps.updateVersions();
        }
    }
    
}
