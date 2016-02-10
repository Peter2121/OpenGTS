package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class DeviceUList 
	extends DeviceRecord<DeviceUList> 
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "DeviceUList";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_groupID              = DeviceGroup.FLD_groupID;
    public static final String FLD_devaccID				= "devaccID";
    private static DBField FieldInfo[] = {
        // DeviceList fields
        newField_accountID(true),
        new DBField(FLD_groupID, String.class, DBField.TYPE_GROUP_ID(), "Device Group ID", "key=true"),
        new DBField(FLD_devaccID, String.class, DBField.TYPE_DEV_ACCT_ID(), "Device Account ID", "key=true"),
        newField_deviceID(true),
        // Common fields
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends DeviceKey<DeviceUList>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String groupId, String devaccId, String deviceId) {
            super.setKeyValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setKeyValue(FLD_groupID  , ((groupId   != null)? groupId  .toLowerCase() : ""));
            super.setKeyValue(FLD_devaccID , ((devaccId  != null)? devaccId .toLowerCase() : ""));
            super.setKeyValue(FLD_deviceID , ((deviceId  != null)? deviceId .toLowerCase() : ""));
        }
        public DBFactory<DeviceUList> getFactory() {
            return DeviceUList.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<DeviceUList> factory = null;
    public static DBFactory<DeviceUList> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                DeviceUList.TABLE_NAME(), 
                DeviceUList.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                DeviceUList.class, 
                DeviceUList.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
            factory.addParentTable(DeviceGroup.TABLE_NAME());
            factory.addParentTable(Device.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public DeviceUList()
    {
        super();
    }

    /* database record */
    public DeviceUList(DeviceUList.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceList.class, loc);
        return i18n.getString("DeviceUList.description", 
            "This table defines " +
            "the membership of a given Device within a universal DeviceGroup. " +
            "A Device may be defined in more than one universal DeviceGroup."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
        
    public String getGroupID()
    {
        String v = (String)this.getFieldValue(FLD_groupID);
        return StringTools.trim(v);
    }
    
    private void setGroupID(String v)
    {
        this.setFieldValue(FLD_groupID, StringTools.trim(v));
    }

    public String getDevAccID()
    {
        String v = (String)this.getFieldValue(FLD_devaccID);
        return StringTools.trim(v);
    }
    
    private void setDevAccID(String v)
    {
        this.setFieldValue(FLD_devaccID, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    public String toString()
    {
        return this.getAccountID() + "/" + this.getGroupID() + "/" + this.getDevAccID() + "/" +this.getDeviceID();
    }

    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean exists(String acctID, String groupID, String devaccID, String devID)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (groupID != null) && (devaccID != null) && (devID != null)) {
            DeviceUList.Key devUListKey = new DeviceUList.Key(acctID, groupID, devaccID, devID);
            return devUListKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // The following is an optimization for holding the Account record while
    // processing this User.  Use with caution.

    private DeviceGroup group = null;
    
    public DeviceGroup getGroup()
    {
        if (this.group == null) {
            try {
                this.group = DeviceGroup.getDeviceGroup(this.getAccount(), this.getGroupID(), false);
            } catch (DBException dbe) {
                this.group = null;
            }
        }
        return this.group;
    }
    
    public void setGroup(DeviceGroup group) 
    {
        if ((group != null) && 
            group.getAccountID().equals(this.getAccountID()) && 
            group.getGroupID().equals(this.getGroupID())) {
            this.group = group;
        } else {
            this.group = null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* get/create device list */
    public static DeviceUList getDeviceList(DeviceGroup group, String devaccID, String deviceID, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* DeviceGroup specified? */
        if (group == null) {
            throw new DBException("DeviceGroup not specified.");
        }
        String accountID = group.getAccountID();
        String groupID   = group.getGroupID();

        /* Account ID for the device provided? */
        if (StringTools.isBlank(devaccID)) {
        	devaccID = accountID;	/* consider the accountID of the device the one the group belongs */
        }
        	
        /* device exists? */
        if (StringTools.isBlank(deviceID)) {
            throw new DBException("Device ID not specified.");
        } else
        if (!Device.exists(devaccID,deviceID)) {
            //throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* create/save record */
        DeviceUList.Key devUListKey = new DeviceUList.Key(accountID, groupID, devaccID, deviceID);
        if (devUListKey.exists()) { // may throw DBException
            // already exists
            DeviceUList list = devUListKey.getDBRecord(true);
            list.setGroup(group);
            return list;
        } else
        if (createOK) {
            DeviceUList list = devUListKey.getDBRecord();
            list.setCreationDefaultValues();
            list.setGroup(group);
            return list;
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        Print.logWarn("No command-line options available for this table");
    }

}
