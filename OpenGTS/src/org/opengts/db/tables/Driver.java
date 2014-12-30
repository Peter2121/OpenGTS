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
//  2010/01/29  Martin D. Flynn
//     -Initial release
//  2013/05/28  Martin D. Flynn
//     -Added FLD_driverStatus
//  2013/11/11  Martin D. Flynn
//     -Updated "getRecordCallback" with Account parameter
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
import org.opengts.db.tables.*;

public class Driver
    extends AccountRecord<Driver>
{

    // ------------------------------------------------------------------------
    // Reference:
    //  - http://www.fmcsa.dot.gov/rules-regulations/administration/fmcsr/fmcsrruletext.aspx?reg=395.8

    public static final long   DutyStatus_INVALID       = -1L;
    public static final long   DutyStatus_UNKNOWN       =  0L;
    public static final long   DutyStatus_OFF_DUTY      =  1L; // "OFF"
    public static final long   DutyStatus_SLEEPING      =  2L; // "SB"
    public static final long   DutyStatus_DRIVING       =  3L; // "D"
    public static final long   DutyStatus_ON_DUTY       =  4L; // "ON"

    // Driver Duty status as defined by 
    //  Federal Motor Carrier Safety Administration, section 395.8
    public enum DutyStatus implements EnumTools.StringLocale, EnumTools.IntValue {
        INVALID    ( -1, I18N.getString(Driver.class,"Driver.status.invalid"  ,"Invalid" )),
        UNKNOWN    (  0, I18N.getString(Driver.class,"Driver.status.unknown"  ,"Unknown" )), // (default)
        OFF_DUTY   (  1, I18N.getString(Driver.class,"Driver.status.offDuty"  ,"Off Duty")), // "OFF"
        SLEEPING   (  2, I18N.getString(Driver.class,"Driver.status.sleeping" ,"Sleeping")), // "SB"
        DRIVING    (  3, I18N.getString(Driver.class,"Driver.status.driving"  ,"Driving" )), // "D"
        ON_DUTY    (  4, I18N.getString(Driver.class,"Driver.status.onDuty"   ,"On Duty" )); // "ON"
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        DutyStatus(int v, I18N.Text a)              { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(UNKNOWN); }
        public boolean isOffDuty()                  { return this.equals(OFF_DUTY)||this.equals(SLEEPING); }
        public boolean isOnDuty()                   { return this.equals(DRIVING) ||this.equals(ON_DUTY ); }
        public boolean isStatus(int status)         { return this.getIntValue() == status; }
    };

    /**
    *** Returns the DutyStatus for the specified code.
    *** @param ds  The duty code
    *** @return The DutyStatus
    **/
    public static DutyStatus getDutyStatus(long ds)
    {
        return EnumTools.getValueOf(DutyStatus.class,(int)ds);
    }

    /**
    *** Returns the defined DutyStatus for the specified driver.
    *** @param d  The driver from which the DutyStatus will be obtained.  
    ***           If null, the default DutyStatus will be returned.
    *** @return The DutyStatus
    **/
    public static DutyStatus getDutyStatus(Driver d)
    {
        return (d != null)? 
            EnumTools.getValueOf(DutyStatus.class,(int)d.getDriverStatus()) : 
            EnumTools.getDefault(DutyStatus.class);
    }

    /** 
    *** Returns the short text description of the specified DutyStatus
    *** @param ds  The DutyStatus 
    *** @param loc The desired Locale
    *** @return The short text description for the specified DutyStatus
    **/
    public static String getDutyStatusDescription(DutyStatus ds, Locale loc)
    {
        return (ds != null)? ds.toString(loc) : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "Driver";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    // Driver/Asset specific information:
    public static final String FLD_driverID             = "driverID";                   // driver ID
    public static final String FLD_contactPhone         = Account.FLD_contactPhone;
    public static final String FLD_contactEmail         = Account.FLD_contactEmail;
    public static final String FLD_licenseType          = "licenseType";                // license type
    public static final String FLD_licenseNumber        = "licenseNumber";              // license number
    public static final String FLD_licenseExpire        = "licenseExpire";              // license expiration (DayNumber)
    public static final String FLD_badgeID              = "badgeID";                    // badge ID
    public static final String FLD_address              = "address";                    // full address
  //public static final String FLD_streetAddress        = "streetAddress";              // street address
  //public static final String FLD_city                 = "city";                       // city
  //public static final String FLD_stateProvince        = "stateProvince";              // state
  //public static final String FLD_postalCode           = "postalCode";                 // postal code
  //public static final String FLD_country              = "country";                    // country
    public static final String FLD_birthdate            = "birthdate";                  // birthdate
    public static final String FLD_deviceID             = Device.FLD_deviceID;          // device ID
    public static final String FLD_driverStatus         = EventData.FLD_driverStatus;   // driver/duty status
    //
    private static DBField FieldInfo[] = {
        // Driver fields
        newField_accountID(true),
        new DBField(FLD_driverID            , String.class  , DBField.TYPE_DRIVER_ID() , "Driver ID"                , "key=true"),
        new DBField(FLD_contactPhone        , String.class  , DBField.TYPE_STRING(32)  , "Contact Phone"            , "edit=2"),
        new DBField(FLD_contactEmail        , String.class  , DBField.TYPE_STRING(128) , "Contact EMail"            , "edit=2"),
        new DBField(FLD_licenseType         , String.class  , DBField.TYPE_STRING(24)  , "License Type"             , "edit=2"),
        new DBField(FLD_licenseNumber       , String.class  , DBField.TYPE_STRING(32)  , "License Number"           , "edit=2"),
        new DBField(FLD_licenseExpire       , Long.TYPE     , DBField.TYPE_UINT32      , "License Expiration Day"   , "edit=2 format=date"),
        new DBField(FLD_badgeID             , String.class  , DBField.TYPE_STRING(32)  , "Badge ID"                 , "edit=2"),
        new DBField(FLD_address             , String.class  , DBField.TYPE_STRING(90)  , "Full Address"             , "utf8=true"),
        new DBField(FLD_birthdate           , Long.TYPE     , DBField.TYPE_UINT32      , "Driver Birthdate"         , "edit=2 format=date"),
        DeviceRecord.newField_deviceID(false),
        new DBField(FLD_driverStatus        , Long.TYPE     , DBField.TYPE_UINT32      , "Driver/Duty Status"       , "edit=2"),
        // Common fields
        newField_displayName(),     // driver 'nickname'
        newField_description(),     // driver name
        newField_notes(),
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends AccountKey<Driver>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String driverId) {
            super.setKeyValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setKeyValue(FLD_driverID , ((driverId  != null)? driverId .toLowerCase() : ""));
        }
        public DBFactory<Driver> getFactory() {
            return Driver.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<Driver> factory = null;
    public static DBFactory<Driver> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                Driver.TABLE_NAME(), 
                Driver.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                Driver.class, 
                Driver.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public Driver()
    {
        super();
    }

    /* database record */
    public Driver(Driver.Key key)
    {
        super(key);
    }
        
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Driver.class, loc);
        return i18n.getString("Driver.description", 
            "This table defines " +
            "Account specific Vehicle Drivers."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below 

    public String getDriverID()
    {
        String v = (String)this.getFieldValue(FLD_driverID);
        return StringTools.trim(v);
    }
    
    public void setDriverID(String v)
    {
        this.setFieldValue(FLD_driverID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this driver has a defined driver-status (value > 0)
    *** @return True if this driver has a defined driver-status
    **/
    public boolean hasDriverStatus()
    {
        return (this.getDriverStatus() > DutyStatus_UNKNOWN)? true : false;
    }

    /**
    *** Gets the driver/duty status
    *** @return The driver/duty status
    **/
    public long getDriverStatus()
    {
        return this.getFieldValue(FLD_driverStatus, DutyStatus_UNKNOWN);
    }

    /**
    *** Sets the driver/duty status
    *** @param v The driver/duty status
    **/
    public void setDriverStatus(long v)
    {
        long ds = (v >= Driver.DutyStatus_UNKNOWN)? v : Driver.DutyStatus_UNKNOWN;
        this.setFieldValue(FLD_driverStatus, ds);
    }

    /**
    *** Sets/Updates the driver-status for the specified DriverID
    *** @param acct   The Account
    *** @param drvID  The driver-id
    *** @param status The driver status
    **/
    public static boolean updateDriverStatus(Account acct, String drvID, long status)
        throws DBException
    {
        if ((acct != null) && !StringTools.isBlank(drvID)) {
            Driver driver = Driver.getDriver(acct, drvID);
            if (driver != null) {
                // update Driver Status
                long ds = (status >= DutyStatus_UNKNOWN)? status : DutyStatus_UNKNOWN;
                if (ds != driver.getDriverStatus()) {
                    driver.setDriverStatus(status);
                    driver.update(FLD_driverStatus); // may throw DBException
                }
                return true;
            } else {
                // no existing Driver record
                return false;
            }
        } else {
            // invalid Account/DriverID
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /* get contact phone of this driver */
    public String getContactPhone()
    {
        String v = (String)this.getFieldValue(FLD_contactPhone);
        return StringTools.trim(v);
    }

    public void setContactPhone(String v)
    {
        this.setFieldValue(FLD_contactPhone, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact email of this driver */
    public String getContactEmail()
    {
        String v = (String)this.getFieldValue(FLD_contactEmail);
        return StringTools.trim(v);
    }

    public void setContactEmail(String v)
    {
        this.setFieldValue(FLD_contactEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* license type */
    public String getLicenseType()
    {
        String v = (String)this.getFieldValue(FLD_licenseType);
        return StringTools.trim(v);
    }

    public void setLicenseType(String v)
    {
        this.setFieldValue(FLD_licenseType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* license number */
    public String getLicenseNumber()
    {
        String v = (String)this.getFieldValue(FLD_licenseNumber);
        return StringTools.trim(v);
    }

    public void setLicenseNumber(String v)
    {
        this.setFieldValue(FLD_licenseNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the license expiration date as a DayNumber value
    *** @return The license expiration date as a DayNumber value
    **/
    public long getLicenseExpire()
    {
        // DayNumber licExpire = new DayNumber(driver.getLicenseExpire());
        return this.getFieldValue(FLD_licenseExpire, 0L);
    }

    /**
    *** Sets the license expiration date as a DayNumber value
    *** @param v The license expiration date as a DayNumber value
    **/
    public void setLicenseExpire(long v)
    {
        this.setFieldValue(FLD_licenseExpire, ((v >= 0L)? v : 0L));
    }

    /**
    *** Sets the license expiration date
    *** @param year   The expiration year
    *** @param month1 The expiration month
    *** @param day    The expiration day
    **/
    public void setLicenseExpire(int year, int month1, int day)
    {
        this.setLicenseExpire(DateTime.getDayNumberFromDate(year, month1, day));
    }

    /**
    *** Sets the license expiration date as a DayNumber instance
    *** @param dn The license expiration date as a DayNumber instance
    **/
    public void setLicenseExpire(DayNumber dn)
    {
        this.setLicenseExpire((dn != null)? dn.getDayNumber() : 0L);
    }

    /**
    *** Returns true if the license is expired as-of the specified date
    *** @param asofDay  The as-of expiration test DayNumber
    **/
    public boolean isLicenseExpired(long asofDay)
    {

        /* get license expiration date */
        long le = this.getLicenseExpire();
        if (le <= 0L) {
            // date not specified
            return false;
        }

        /* as-of date */
        long ae = asofDay;
        if (ae <= 0L) {
            // as-of date not specified, use current day
            Account acct = this.getAccount();
            TimeZone tz = (acct != null)? acct.getTimeZone((TimeZone)null) : null;
            ae = DateTime.getDayNumberFromDate(new DateTime(tz));
        }

        /* compare and return */
        return (le < ae)? true : false;

    }

    /**
    *** Returns true if the license is expired as-of the specified date
    *** @param asof  The as-of expiration test DayNumber
    **/
    public boolean isLicenseExpired(DayNumber asof)
    {
        return this.isLicenseExpired((asof != null)? asof.getDayNumber() : 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the driver badge ID
    *** @return The driver badge ID
    **/
    public String getBadgeID()
    {
        String v = (String)this.getFieldValue(FLD_badgeID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the driver badge ID
    *** @param v The driver badge ID
    **/
    public void setBadgeID(String v)
    {
        this.setFieldValue(FLD_badgeID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the driver address
    *** @return The driver address
    **/
    public String getAddress()
    {
        String v = (String)this.getFieldValue(FLD_address);
        return StringTools.trim(v);
    }
    
    /**
    *** Sets the driver address
    *** @param v The driver address
    **/
    public void setAddress(String v)
    {
        this.setFieldValue(FLD_address, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the driver birthdate as a DayNumber value
    *** @return The driver birthdate as a DayNumber value
    **/
    public long getBirthdate()
    {
        // DayNumber birthdate = new DayNumber(driver.getBirthdate());
        return this.getFieldValue(FLD_birthdate, 0L);
    }

    /**
    *** Sets the driver birthdate as a DayNumber value
    *** @param v The driver birthdate as a DayNumber value
    **/
    public void setBirthdate(long v)
    {
        this.setFieldValue(FLD_birthdate, ((v >= 0L)? v : 0L));
    }

    /**
    *** Sets the driver birthdate 
    *** @param year   The driver birthdate year
    *** @param month1 The driver birthdate month
    *** @param day    The driver birthdate day
    **/
    public void setBirthdate(int year, int month1, int day)
    {
        this.setBirthdate(DateTime.getDayNumberFromDate(year, month1, day));
    }

    /**
    *** Sets the driver birthdate as a DayNumber instance
    *** @param dn The driver birthdate as a DayNumber instance
    **/
    public void setBirthdate(DayNumber dn)
    {
        this.setBirthdate((dn != null)? dn.getDayNumber() : 0L);
    }
        
    // ------------------------------------------------------------------------

    private Device device = null;

    /**
    *** Gets the driver associated device ID (if available)
    *** @return The driver associated device ID, or blank if not available.
    **/
    public String getDeviceID()
    {
        String v = (String)this.getFieldValue(FLD_deviceID);
        return (v != null)? v : "";
    }

    /**
    *** Sets the driver associated device ID
    *** @param v The driver associated device ID
    **/
    public void setDeviceID(String v)
    {
        this.setFieldValue(FLD_deviceID, ((v != null)? v : ""));
        this.device = null;
    }

    /**
    *** Returns the Device instance for the defined device-id
    *** @return The Device instance
    **/
    public Device getDevice()
    {
        if (this.device == null) {
            Account account = this.getAccount();
            String deviceID = this.getDeviceID();
            if ((account != null) && !StringTools.isBlank(deviceID)) {
                try {
                    this.device = Device.getDevice(account,deviceID); // null if non-existent
                } catch (DBException dbe) {
                    this.device = null;
                }
            }
        }
        return this.device;
    }

    /**
    *** Returns the description for the assigned DeviceID
    *** @return The device description, or null if no deviceID is defined
    **/
    public String getDeviceDescription()
    {
        Device device = this.getDevice();
        return (device != null)? device.getDescription() : "";
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        //this.setIsActive(true);
        // other defaults
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified driver exists
    *** @param acctID  The Account ID
    *** @param drvID   The driver ID
    *** @return True if the specified driver exists
    **/
    public static boolean exists(String acctID, String drvID)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (drvID != null)) {
            Driver.Key drvKey = new Driver.Key(acctID, drvID);
            return drvKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Get Driver record instance for the specified account/driverID
    *** @param account The Account instance
    *** @param drvID   The driver ID
    *** @return The Driver record instance, or null if non-existent
    **/
    public static Driver getDriver(Account account, String drvID)
        throws DBException
    {
        if ((account != null) && (drvID != null)) {
            String acctID = account.getAccountID();
            Driver.Key key = new Driver.Key(acctID, drvID);
            if (key.exists()) {
                Driver drv = key.getDBRecord(true);
                drv.setAccount(account);
                return drv;
            } else {
                // driver does not exist
                return null;
            }
        } else {
            return null; // just say it doesn't exist
        }
    }

    /* get driver */
    // Note: does NOT return null (throws exception if not found)
    public static Driver getDriver(Account account, String drvID, boolean create)
        throws DBException
    {

        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();

        /* driver-id specified? drvID */
        if (StringTools.isBlank(drvID)) {
            throw new DBNotFoundException("Driver-ID not specified for account: " + acctID);
        }

        /* get/create */
        Driver drv = null;
        Driver.Key drvKey = new Driver.Key(acctID, drvID);
        if (!drvKey.exists()) {
            if (create) {
                drv = drvKey.getDBRecord();
                drv.setAccount(account);
                drv.setCreationDefaultValues();
                return drv; // not yet saved!
            } else {
                throw new DBNotFoundException("Driver-ID does not exists: " + drvKey);
            }
        } else
        if (create) {
            // we've been asked to create the driver, and it already exists
            throw new DBAlreadyExistsException("Driver-ID already exists: " + drvKey);
        } else {
            drv = Driver.getDriver(account, drvID);
            if (drv == null) {
                throw new DBException("Unable to read existing Driver-ID: " + drvKey);
            }
            return drv;
        }

    }

    // ------------------------------------------------------------------------

    public static Driver createNewDriver(Account account, String drvID)
        throws DBException
    {
        if ((account != null) && !StringTools.isBlank(drvID)) {
            Driver drv = Driver.getDriver(account, drvID, true); // does not return null
            drv.save();
            return drv;
        } else {
            throw new DBException("Invalid Account/DriverID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return list of all Drivers owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDriverIDsForAccount(String acctId)
        throws DBException
    {
        return Driver.getDriverIDsForAccount(acctId, -1L);
    }

    /* return list of all Drivers owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDriverIDsForAccount(String acctId, long limit)
        throws DBException
    {

        /* no account specified? */
        if (StringTools.isBlank(acctId)) {
            Print.logError("Account not specified!");
            return new OrderedSet<String>();
        }

        /* read drivers for account */
        OrderedSet<String> drvList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Driver WHERE (accountID='acct') ORDER BY driverID
            DBSelect<Driver> dsel = new DBSelect<Driver>(Driver.getFactory());
            dsel.setSelectedFields(Driver.FLD_driverID);
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE(dwh.EQ(Driver.FLD_accountID,acctId)));
            dsel.setOrderByFields(Driver.FLD_driverID);
            dsel.setLimit(limit);

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String drvId = rs.getString(Driver.FLD_driverID);
                drvList.add(drvId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Driver List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return drvList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback for returning all drivers for an Account
    *** @param account  The account
    *** @param rcdHandler  The DBRecordHandler
    **/
    public static void getRecordCallback(Account account,
        DBRecordHandler<org.opengts.db.tables.Driver> rcdHandler)
        throws DBException
    {

        /* DBRecordHandler must be specified (NO-OP otherwise) */
        if (rcdHandler == null) {
            // return without error
            return; 
        }

        /* account must be specified */
        if (account == null) {
            // Account is missing
            throw new DBException("Account is null");
        }
        String acctId = account.getAccountID();

        /* DBSelect */
        // DBSelect: SELECT * FROM Driver WHERE accountID="ACCOUNT"
        DBFactory<org.opengts.db.tables.Driver> drvFact = org.opengts.db.tables.Driver.getFactory();
        DBSelect<org.opengts.db.tables.Driver> dsel = new DBSelect<org.opengts.db.tables.Driver>(drvFact);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(dwh.WHERE(dwh.EQ(Driver.FLD_accountID,acctId)));
        dsel.setOrderByFields(Driver.FLD_driverID);

        /* iterate through records */
        DBRecord.select(dsel, rcdHandler);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below
    
    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct"  , "a" };
    private static final String ARG_DRIVER[]    = new String[] { "driver"  , "drv"   , "d" };
    private static final String ARG_DELETE[]    = new String[] { "delete"              };
    private static final String ARG_CREATE[]    = new String[] { "create"              };
    private static final String ARG_EDIT[]      = new String[] { "edit"    , "ed"      };
    private static final String ARG_EDITALL[]   = new String[] { "editall" , "eda"     }; 

    private static String _fmtDrvID(String acctID, String drvID)
    {
        return acctID + "/" + drvID;
    }

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + Driver.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>   Acount ID which owns Driver");
        Print.sysPrintln("  -driver=<id>    Driver ID to create/edit");
        Print.sysPrintln("  -create         Create a new Driver");
        Print.sysPrintln("  -edit[all]      Edit an existing (or newly created) Driver");
        Print.sysPrintln("  -delete         Delete specified Driver");
        System.exit(1);
    }
    
    public static void cron(String args[])
    {
        DBConfig.cmdLineInit(args,false);
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String drvID   = RTConfig.getString(ARG_DRIVER , "");

        /* account list */
        Collection<String> acctList = null;
        try {
            acctList = Account.getAllAccounts();
        } catch (DBException dbe) {
            Print.logError("Unable to read Accounts: " + dbe);
            return;
        }

        /* loop through accounts */
        try {
            for (String accountID : acctList) {

                /* read account */
                Account account = Account.getAccount(accountID);

                /* inactive? */
                if (!account.isActive()) {
                    continue;
                }

                /* current time relative to Account timezone */
                DateTime nowDT = new DateTime(account.getTimeZone(DateTime.GMT));
                long today = nowDT.getDayNumber();
                long futureDay = today + 14;

                /* get list of drivers */
                // getDrivers
                OrderedSet<String> driverList = Driver.getDriverIDsForAccount(accountID);
                for (String driverID : driverList) {
                    Driver driver = Driver.getDriver(account, driverID);
                    // check license expiration
                    if (driver.isLicenseExpired(today)) {
                        // TODO: license is expired
                    } else
                    if (driver.isLicenseExpired(futureDay)) {
                        // TODO: license will expire soon
                    }
                }

            }
        } catch (DBException dbe) {
            Print.logError("Error: " + dbe);
            return;
        }

    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String drvID   = RTConfig.getString(ARG_DRIVER , "");

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
        BasicPrivateLabel privLabel = acct.getPrivateLabel();

        /* driver-id specified? */
        if (StringTools.isBlank(drvID)) {
            Print.logError("Driver-ID not specified.");
            usage();
        }

        /* driver exists? */
        boolean driverExists = false;
        try {
            driverExists = Driver.exists(acctID, drvID);
        } catch (DBException dbe) {
            Print.logError("Error determining if Driver exists: " + _fmtDrvID(acctID,drvID));
            System.exit(99);
        }

        /* option count */
        int opts = 0;

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !StringTools.isBlank(acctID) && !StringTools.isBlank(drvID)) {
            opts++;
            if (!driverExists) {
                Print.logWarn("Driver does not exist: " + _fmtDrvID(acctID,drvID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Driver.Key drvKey = new Driver.Key(acctID, drvID);
                drvKey.delete(true); // also delete dependencies
                Print.logInfo("Driver deleted: " + _fmtDrvID(acctID,drvID));
                driverExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting Driver: " + _fmtDrvID(acctID,drvID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (driverExists) {
                Print.logWarn("Driver already exists: " + _fmtDrvID(acctID,drvID));
            } else {
                try {
                    Driver.createNewDriver(acct, drvID);
                    Print.logInfo("Created Device: " + _fmtDrvID(acctID,drvID));
                    driverExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating Driver: " + _fmtDrvID(acctID,drvID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!driverExists) {
                Print.logError("Driver does not exist: " + _fmtDrvID(acctID,drvID));
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    Driver driver = Driver.getDriver(acct, drvID); // may throw DBException
                    DBEdit editor = new DBEdit(driver);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing Driver: " + _fmtDrvID(acctID,drvID));
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }
    
}
