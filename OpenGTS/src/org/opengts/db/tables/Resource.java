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
//  2008/05/14  Martin D. Flynn
//     -Initial release
//  2010/11/29  Martin D. Flynn
//     -Added "getPrivateLabelPropertiesForHost" for custom PrivateLabel properties.
//  2012/02/03  Martin D. Flynn
//     -Increased "resourceID" key size from 64 to 80.
//  2013/04/08  Martin D. Flynn
//     -Added "getGlobalResourceValue"/"setGlobalResourceValue"
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;

public class Resource
    extends AccountRecord<Resource>
{

    // ------------------------------------------------------------------------

    /* common resource-ids */
    public static final String RESID_TemporaryProperties        = "temporaryproperties";
    public static final String RESID_PrivateLabel_Properties_   = "privatelabel.properties:";

    // ------------------------------------------------------------------------
    // Resource types

    public static final String TYPE_TEXT                    = "text";               // value is text
    public static final String TYPE_XML                     = "xml";                // value is XML
    public static final String TYPE_HTML                    = "html";               // value is HTML

    public static final String TYPE_URL                     = "url";                // value is a URL
    public static final String TYPE_URL_                    = TYPE_URL  + "/";
    public static final String TYPE_URL_IMAGE               = TYPE_URL_ + "image";  // value is a image URL
    public static final String TYPE_URL_PDF                 = TYPE_URL_ + "pdf";    // value is a PDF URL

    public static final String TYPE_IMAGE_                  = "image/";
    public static final String TYPE_IMAGE_JPEG              = TYPE_IMAGE_ + "jpeg"; // value is a binary JPEG
    public static final String TYPE_IMAGE_GIF               = TYPE_IMAGE_ + "gif";  // value is a binary GIF
    public static final String TYPE_IMAGE_PNG               = TYPE_IMAGE_ + "png";  // value is a binary PNG
    public static final String TYPE_IMAGE_GENERIC           = TYPE_IMAGE_ + "generic";

    public static final String TYPE_BINARY                  = "binary";             // value is opague binary
    public static final String TYPE_RTPROPS                 = "rtprops";            // value is RTProperties string
    public static final String TYPE_COLOR                   = "color";              // value is a color representation

    /** 
    *** Returns true if the specified type represents a String
    *** @param type  The specified type
    *** @return True if the specified represents a String value
    **/
    public static boolean isStringType(String type)
    {

        /* blank type */
        if (StringTools.isBlank(type)) {
            return false;
        }
        type = type.toLowerCase();

        /* check explicit String types */
        if (type.startsWith(TYPE_TEXT)    ||
            type.startsWith(TYPE_XML)     ||
            type.startsWith(TYPE_HTML)    ||
            type.startsWith(TYPE_URL)     ||
            type.startsWith(TYPE_RTPROPS) ||
            type.startsWith(TYPE_COLOR)     ) {
            return true;
        }

        /* not explicit String */
        return false;

    }

    /** 
    *** Returns true if the specified type represents a Binary value
    *** @param type  The specified type
    *** @return True if the specified represents a Binary value
    **/
    public static boolean isBinaryType(String type)
    {

        /* blank type */
        if (StringTools.isBlank(type)) {
            return true; // default to binary
        }
        type = type.toLowerCase();

        /* check explicit Binary types */
        if (type.startsWith(TYPE_IMAGE_) ||
            type.startsWith(TYPE_BINARY)   ) {
            return true;
        }

        /* not explicit Binary */
        return false;

    }

    // ------------------------------------------------------------------------

    /* properties */
    public static final String PROP_WIDTH                   = "width";      // image url width
    public static final String PROP_HEIGHT                  = "height";     // image url height
    public static final String PROP_ICON_URL                = "iconURL";    // thumbnail icon
    public static final String PROP_ICON_WIDTH              = "iconWidth";  // thumbnail icon width
    public static final String PROP_ICON_HEIGHT             = "iconHeight"; // thumbnail icon height
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* local 'Dimension' definition (instead of the 'Dimension' in awt) */
    public static class Dimension
    {
        public int width  = 0; // public
        public int height = 0; // public
        public Dimension(int w, int h) {
            this.width  = w;
            this.height = h;
        }
        public int getWidth() {
            return this.width;
        }
        public int getHeight() {
            return this.height;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    private static final String _TABLE_NAME                 = "Resource";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }
 
    /* field definition */
    public static final String FLD_resourceID               = "resourceID";
    public static final String FLD_type                     = "type";
    public static final String FLD_title                    = "title";
    public static final String FLD_properties               = "properties";
    public static final String FLD_value                    = "value";
    private static DBField FieldInfo[] = {
        // Resource fields
        newField_accountID(true),
        new DBField(FLD_resourceID      , String.class  , DBField.TYPE_STRING(80)  , "Resource ID"  , "key=true editor=accountString"),
        new DBField(FLD_type            , String.class  , DBField.TYPE_STRING(16)  , "Type"         , "edit=2"),
        new DBField(FLD_title           , String.class  , DBField.TYPE_STRING(70)  , "Title"        , "edit=2 utf8=true"),
        new DBField(FLD_properties      , String.class  , DBField.TYPE_TEXT        , "Properties"   , "edit=2"),
        new DBField(FLD_value           , byte[].class  , DBField.TYPE_BLOB        , "Value"        , "edit=2"),
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends AccountKey<Resource>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String strKey) {
            super.setKeyValue(FLD_accountID , ((acctId != null)? acctId.toLowerCase() : ""));
            super.setKeyValue(FLD_resourceID, ((strKey != null)? strKey               : ""));
        }
        public DBFactory<Resource> getFactory() {
            return Resource.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Resource> factory = null;
    public static DBFactory<Resource> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Resource.TABLE_NAME(), 
                Resource.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                Resource.class, 
                Resource.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Resource()
    {
        super();
    }

    /* database record */
    public Resource(Resource.Key key)
    {
        super(key);
    }

    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Resource.class, loc);
        return i18n.getString("Resource.description", 
            "This table defines " +
            "Account specific text resources."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the Resource ID for this record */
    public String getResourceID()
    {
        String v = (String)this.getFieldValue(FLD_resourceID);
        return StringTools.trim(v);
    }
    
    /* set the Resource ID for this record */
    private void setResourceID(String v)
    {
        this.setFieldValue(FLD_resourceID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the resource title */
    public String getTitle()
    {
        String v = (String)this.getFieldValue(FLD_title);
        return StringTools.trim(v);
    }

    /* set the resource title */
    public void setTitle(String v)
    {
        this.setFieldValue(FLD_title, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* return the resource type */
    public String getType()
    {
        String v = (String)this.getFieldValue(FLD_type);
        return StringTools.trim(v);
    }

    /* set the resource type */
    public void setType(String v)
    {
        this.setFieldValue(FLD_type, StringTools.trim(v));
    }

    /* return true is the resource prepresents a String type */
    public boolean isStringType()
    {
        String t = this.getType();
        return Resource.isStringType(t);
    }

    /* return true is the resource prepresents a Binary type */
    public boolean isBinaryType()
    {
        String t = this.getType();
        return Resource.isBinaryType(t);
    }

    /* return true if resource is a URL */
    public boolean isURL()
    {
        String t = this.getType();
        if (t.equals(TYPE_URL) || t.startsWith(TYPE_URL_)) {
            return true;
        } else {
            return false;
        }
    }

    /* return true if resource is an image URL */
    public boolean isImageURL()
    {
        String t = this.getType();
        if (t.equalsIgnoreCase(TYPE_URL_IMAGE)) {
            return true;
        } else {
            return false;
        }
    }

    /* return true if resource is a binary image */
    public boolean isImage()
    {
        String t = this.getType();
        if (t.startsWith(TYPE_IMAGE_)) {
            return true;
        } else {
            return false;
        }
    }

    /* returns true if resource is a properties type */
    public boolean isRTProperties()
    {
        String t = this.getType();
        if (t.startsWith(TYPE_RTPROPS)) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    
    private RTProperties rtProps = null;

    /* return the resource properties */
    public String getProperties()
    {
        String v = (String)this.getFieldValue(FLD_properties);
        return StringTools.trim(v);
    }

    /* sets the resource type */
    public void setProperties(String v)
    {
        this._setProperties(v);
        this.rtProps = null;
    }

    /* sets the resource type */
    protected void _setProperties(String v)
    {
        this.setFieldValue(FLD_properties, StringTools.trim(v));
    }

    /* sets the resource type */
    public void setRTProperties(RTProperties v)
    {
        this._setProperties((v != null)? v.toString() : "");
        this.rtProps = null;
    }

    /* return the resource properties, bound to this Resource */
    public RTProperties getRTProperties()
    {
        if (this.rtProps == null) {
            this.rtProps = this._getRTProperties();
            // add listener to update FLD_properties when someone changes the RTProperties instance
            this.rtProps.addChangeListener(new RTProperties.PropertyChangeListener() {
                public void propertyChange(RTProperties.PropertyChangeEvent pce) {
                    //Print.logInfo("Updating RTProperties: " + pce.getKey());
                    Resource.this._setProperties(Resource.this.rtProps.toString());
                }
            });
        }
        return this.rtProps;
    }

    /* creates/return the resource properties */
    public RTProperties _getRTProperties()
    {
        RTProperties rtp = new RTProperties();
        rtp.setProperties(this.getProperties(), false);
        return rtp;
    }

    /* get a specific property value */
    public String getProperty(String key, String dft)
    {
        return this.getRTProperties().getString(key, dft);
    }

    /* get a specific property value */
    public int getProperty(String key, int dft)
    {
        return this.getRTProperties().getInt(key, dft);
    }

    /* get a specific property value */
    public long getProperty(String key, long dft)
    {
        return this.getRTProperties().getLong(key, dft);
    }

    /* get a specific property value */
    public double getProperty(String key, double dft)
    {
        return this.getRTProperties().getDouble(key, dft);
    }

    /* get the property width/height */
    public Resource.Dimension getDimension(int dftWidth, int dftHeight)
    {
        int w = this.getProperty(Resource.PROP_WIDTH , dftWidth );
        int h = this.getProperty(Resource.PROP_HEIGHT, dftHeight);
        return new Resource.Dimension(w, h);
    }

    /* get the property icon URL */
    public String getIconURL(String dft)
    {
        return this.getProperty(Resource.PROP_ICON_URL, dft);
    }

    /* get the property icon width/height */
    public Resource.Dimension getIconDimension(int dftWidth, int dftHeight)
    {
        int w = this.getProperty(Resource.PROP_ICON_WIDTH , dftWidth );
        int h = this.getProperty(Resource.PROP_ICON_HEIGHT, dftHeight);
        return new Resource.Dimension(w, h);
    }

    // ------------------------------------------------------------------------

    /* return the resource value */
    public byte[] getValue()
    {
        byte v[] = (byte[])this.getFieldValue(FLD_value);
        return (v != null)? v : new byte[0];
    }

    /* set the resource value */
    public void setValue(byte[] v)
    {
        this.setFieldValue(FLD_value, ((v != null)? v : new byte[0]));
    }

    /* sets the resource value */
    public void setValue(Object v) 
    {
        if (v == null) {
            this.setValue((byte[])null);
        } else
        if (v instanceof byte[]) {
            this.setValue((byte[])v);
        } else
        if (v instanceof String) {
            this.setValue(StringTools.getBytes((String)v));
        } else {
            this.setValue(StringTools.getBytes(v.toString()));
        }
    }

    /* set the resource value as a String */
    public void setStringValue(String v)
    {
        this.setValue(StringTools.getBytes(v));
    }

    /* get the resource value as a String */
    public String getStringValue()
    {
        byte v[] = this.getValue();
        if (this.isBinaryType()) {
            return "0x" + StringTools.toHexString(v);
        } else
        if (this.isRTProperties()) {
            return this.getProperties();
        } else {
            return StringTools.toStringValue(v);
        }
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("Resource Text");
        this.setType(Resource.TYPE_TEXT);
        this.setStringValue("");
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /* return the AccountID/ResourceID */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getResourceID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String resID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (resID != null)) {
            Resource.Key resKey = new Resource.Key(acctID, resID);
            return resKey.exists();
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /* get Resource */
    // may return null
    protected static Resource _getResource(String acctID, String resID)
        throws DBException
    {
        if ((acctID != null) && (resID != null)) {
            Resource.Key key = new Resource.Key(acctID, resID);
            if (key.exists()) {
                Resource res = key.getDBRecord(true);
                return res;
            } else {
                // Resource does not exist
                return null;
            }
        } else {
            return null; // just say it doesn't exist
        }
    }

    /* get Resource */
    public static Resource getResource(Account account, String resID)
        throws DBException
    {
        if (account != null) {
            String acctID = account.getAccountID();
            Resource res = Resource._getResource(acctID, resID);
            if (res != null) {
                res.setAccount(account);
                return res;
            } else {
                //Print.logWarn("Resource-ID not found: " + resID);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
    *** Gets a global resource value (owned by the "sysadmin" account)
    **/
    public static Object getGlobalResourceValue(String resID)
        throws DBException
    {
        String sysAdminID = AccountRecord.getSystemAdminAccountID();
        Resource res = Resource._getResource(sysAdminID, resID);
        if (res != null) {
            if (res.isRTProperties()) {
                return res._getRTProperties();
            } else
            if (res.isStringType()) {
                return res.getStringValue();
            } else {
                return res.getValue();
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /* get Resource */
    // Note: does NOT return null
    public static Resource getResource(Account account, String resID, boolean create)
        throws DBException
    {

        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();

        /* return Resource */
        Resource res = Resource._getResource(acctID, resID, create);
        res.setAccount(account);
        return res;

    }

    /* get Resource */
    // Note: does NOT return null
    protected static Resource _getResource(String acctID, String resID, boolean create)
        throws DBException
    {

        /* check account-id */
        if (acctID == null) {
            throw new DBNotFoundException("Account-ID not specified");
        }

        /* Resource-id specified? */
        if (resID == null) {
            throw new DBNotFoundException("Resource-ID not specified for account: " + acctID);
        }

        /* get/create */
        Resource res = null;
        Resource.Key resKey = new Resource.Key(acctID, resID);
        if (!resKey.exists()) {
            if (create) {
                res = resKey.getDBRecord();
              //res.setAccount(account);
                res.setCreationDefaultValues();
                return res; // not yet saved!
            } else {
                throw new DBNotFoundException("Resource-ID does not exists: " + resKey);
            }
        } else
        if (create) {
            // we've been asked to create the Resource, and it already exists
            throw new DBAlreadyExistsException("Resource-ID already exists '" + resKey + "'");
        } else {
            res = Resource._getResource(acctID, resID);
            if (res == null) {
                throw new DBException("Unable to read existing Resource-ID: " + resKey);
            }
            return res;
        }
        
    }

    /**
    *** Create new Resource with key
    *** will throw DBException if resource key already exists
    *** @param account  The Account
    *** @param resID    The Resource-ID
    *** @throws DBException if Account/Resource-ID already exists
    **/
    public static Resource createNewResource(Account account, String resID)
        throws DBException
    {
        if ((account != null) && !StringTools.isBlank(resID)) {
            Resource res = Resource.getResource(account, resID, true); // does not return null
            res.save();
            return res;
        } else {
            throw new DBException("Invalid Account/ResourceID specified");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates/Set the specified Resourse
    *** @param account  The Account
    *** @param resID    The Resource-ID
    *** @param type     The Resource type
    *** @param desc     The Resource description
    *** @param value    The Resource value
    *** @return True if there was an existing resource which was overwritten
    **/
    public static boolean setResource(Account account, String resID, String type, String desc, Object value)
        throws DBException
    {

        /* no account */
        if (account == null) {
            throw new DBException("Account not specified");
        }
        String acctID = account.getAccountID();

        /* set/create resource */
        return Resource._setResource(acctID, resID, type, desc, value);

    }

    /**
    *** Creates/Sets the specified Resourse
    *** @param acctID   The Account-ID
    *** @param resID    The Resource-ID
    *** @param type     The Resource type
    *** @param desc     The Resource description
    *** @param value    The Resource value
    *** @return True if there was an existing resource which was overwritten
    **/
    protected static boolean _setResource(String acctID, String resID, String type, String desc, Object value)
        throws DBException
    {

        /* no resource ID */
        if (StringTools.isBlank(resID)) {
            throw new DBException("Invalid ResourceID specified for Account: " + acctID);
        }

        /* resource already exists? */
        boolean exists = Resource.exists(acctID, resID);
        Resource res = Resource._getResource(acctID, resID, !exists); // not null

        /* type/title */
        if (type != null) {
            res.setType(type);
        }
        if (desc != null) {
            res.setTitle(desc);
        }

        /* set value */
        if (value instanceof RTProperties) {
            res.setRTProperties((RTProperties)value);
        } else {
            res.setValue(value);
        }

        /* save */
        res.save();

        /* return */
        return exists;

    }

    /**
    *** Creates/Sets a global resource (owned by the "sysadmin" account)
    **/
    public static boolean setGlobalResourceValue(String resID, String type, String desc, Object value)
        throws DBException
    {
        String sysAdminID = AccountRecord.getSystemAdminAccountID();
        return Resource._setResource(sysAdminID, resID, type, desc, value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return list of all Resource keys owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getResourcesForAccount(String acctId, String startsWith)
        throws DBException
    {

        /* invalid account */
        if (StringTools.isBlank(acctId)) {
            return new OrderedSet<String>();
        }

        /* select */
        // DBSelect: [SELECT] WHERE <Where> ORDER BY resourceID
        DBSelect<Resource> dsel = new DBSelect<Resource>(Resource.getFactory());
        dsel.setSelectedFields(FLD_resourceID);
        dsel.setOrderByFields(FLD_resourceID);
        DBWhere dwh = dsel.createDBWhere();
        if (StringTools.isBlank(startsWith)) {
            dsel.setWhere(dwh.WHERE(
                dwh.EQ(Resource.FLD_accountID,acctId)
            ));
        } else {
            dsel.setWhere(dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(Resource.FLD_accountID,acctId),
                    dwh.STARTSWITH(Resource.FLD_resourceID,startsWith)
                )
            ));
        }

        /* return list */
        return Resource.getResources(dsel);

    }
    
    /* return list of Resources based on DBSelect (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getResources(DBSelect<Resource> dsel)
        throws DBException
    {
        OrderedSet<String> resourceList = new OrderedSet<String>(true);

        /* invalid account */
        if (dsel == null) {
            return resourceList;
        }

        /* get record ids */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get record ids */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String resourceId = rs.getString(Resource.FLD_resourceID);
                resourceList.add(resourceId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Resource List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return resourceList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static void MoveProperty(RTProperties rtProps, String fromKey, String toKey)
    {
        if (rtProps.hasProperty(fromKey)) {
            rtProps.setString(toKey, rtProps.getString(fromKey,""));
            rtProps.removeProperty(fromKey);
        }
    }
    
    /**
    *** Gets the custom property resources for the specified hostname
    *** @return The Resource RTProperties, or null if the properties are not found
    **/
    public static RTProperties getPrivateLabelPropertiesForHost(String hostName, String urlPath)
    {
        // Example properties:
        //   PageTitle=GPS Tracking
        //   Copyright=Copyright GPS Tracking
        //   banner.width=860
        //   banner.style="border-bottom: 1px solid black;"
        //   banner.imageSource=custom/TrackingBanner.jpg
        //   banner.imageWidth=860
        //   banner.imageHeight=120
        //   banner.anchorLink=
        //   Background.color=#FFFFFF
        //   Background.image=url(./extra/images/Banner_GPSSatShadowShort.png)
        //   Background.position=center
        //   Background.repeat=no-repeat
        String sysAdminID = AccountRecord.getSystemAdminAccountID();
        if (!StringTools.isBlank(hostName) && !StringTools.isBlank(sysAdminID)) {
            Resource res = null;

            /* "PrivateLabel.properties:custom.example.com/track/Track" */
            if ((res == null) && !StringTools.isBlank(urlPath)) {
                String resPathKey = RESID_PrivateLabel_Properties_ + hostName + urlPath;
                try {
                    //Print.logInfo("Searching for Resource: " + resPathKey);
                    res = Resource._getResource(sysAdminID, resPathKey);
                } catch (DBException dbe) {
                    Print.logException("Error loading Resource: " + sysAdminID + "/" + resPathKey, dbe);
                    // fall through to below
                }
            }

            /* "PrivateLabel.properties:custom.example.com" */
            if (res == null) {
                String resKey = RESID_PrivateLabel_Properties_ + hostName;
                try {
                    //Print.logInfo("Searching for Resource: " + resKey);
                    res = Resource._getResource(sysAdminID, resKey);
                } catch (DBException dbe) {
                    Print.logException("Error loading Resource: " + sysAdminID + "/" + resKey, dbe);
                    // fall through to below
                }
            }

            /* found resource? */
            if (res != null) {
                // found entry
                RTProperties rtProps = new RTProperties();
                String props = res.getProperties();
                if (props != null) {
                    rtProps.setProperties(props, false);
                }
                byte value[] = res.getValue();
                if (value.length > 0) {
                    rtProps.setProperties(StringTools.toStringValue(value), false);
                }
                return rtProps;
            } else {
                //Print.logInfo("Resource Key not found: " + sysAdminID + "," + hostName + "," + urlPath);
            }

        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below
    
    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct" };
    private static final String ARG_RESOURCE[]  = new String[] { "resource", "res"  };
    private static final String ARG_CREATE[]    = new String[] { "create"           };
    private static final String ARG_EDIT[]      = new String[] { "edit"    , "ed"   };
    private static final String ARG_DELETE[]    = new String[] { "delete"           };
    private static final String ARG_LIST[]      = new String[] { "list"             };

    private static String _fmtResID(String acctID, String resID)
    {
        return acctID + "/" + resID;
    }

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + Resource.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns Resource");
        Print.logInfo("  -resource=<id>  Resource ID to create/edit");
        Print.logInfo("  -create         Create a new Resource");
        Print.logInfo("  -edit           Edit an existing (or newly created) Resource");
        Print.logInfo("  -delete         Delete specified Resource");
        Print.logInfo("  -list           List account Resources");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT , "");
        String resID   = RTConfig.getString(ARG_RESOURCE, "");

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* resource-id specified? */
        if (StringTools.isBlank(resID)) {
            //Print.logWarn("Resource-ID not specified.");
            //usage();
        }

        /* resource exists? */
        boolean resourceExists = false;
        try {
            resourceExists = Resource.exists(acctID, resID);
        } catch (DBException dbe) {
            Print.logError("Error determining if Resource exists: " + _fmtResID(acctID,resID));
            System.exit(99);
        }

        /* option count */
        int opts = 0;
        
        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !resID.equals("")) {
            opts++;
            if (!resourceExists) {
                Print.logWarn("Resource does not exist: " + _fmtResID(acctID,resID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Resource.Key strKey = new Resource.Key(acctID, resID);
                strKey.delete(true); // also delete dependencies
                Print.logInfo("Resource deleted: " + _fmtResID(acctID,resID));
                resourceExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting Resource: " + _fmtResID(acctID,resID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (resourceExists) {
                Print.logWarn("Resource already exists: " + _fmtResID(acctID,resID));
            } else {
                try {
                    Resource.createNewResource(acct, resID);
                    Print.logInfo("Created Resource: " + _fmtResID(acctID,resID));
                    resourceExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating Resource: " + _fmtResID(acctID,resID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false)) {
            opts++;
            if (!resourceExists) {
                Print.logError("Resource does not exist: " + _fmtResID(acctID,resID));
            } else {
                try {
                    Resource str = Resource.getResource(acct, resID, false); // may throw DBException
                    DBEdit editor = new DBEdit(str);
                    editor.edit(true); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing Resource: " + _fmtResID(acctID,resID));
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            String regex = RTConfig.getString(ARG_LIST,"").toLowerCase();
            opts++;
            try {
                OrderedSet<String> keyList = Resource.getResourcesForAccount(acctID, null);
                for (String resKey : keyList) {
                    if (StringTools.isBlank(regex)            || 
                        (resKey.indexOf(regex) >= 0)          ||
                        StringTools.regexMatches(resKey,regex)  ) {
                        Resource res = Resource._getResource(acctID,resKey);                        
                        if (res != null) {
                            String v = res.getStringValue();
                            Print.sysPrintln(resKey + "=" + v);
                        } else {
                            // should not occur
                        }
                    }
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logException("Listing Resources", dbe);
                System.exit(99);
            }
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

    // ------------------------------------------------------------------------

}
