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
//  2007/11/28  Martin D. Flynn
//     -Initial release
//  2012/10/16  Martin D. Flynn
//     -Changed "TRAILER" EntityType from int-value '0' to int-value '1'.
//     -Added EntityType "GENERAL" as int-value '0'.
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public abstract class EntityManager
{
 
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Entity type

    public enum EntityType implements EnumTools.StringLocale, EnumTools.IntValue {
        GENERAL     (    0, "GENERAL"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.general"  ,"General"    )), // default
        TRAILER     (    1, "TRAILER"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.trailer"  ,"Trailer"    )), 
        DRIVER      (  100, "DRIVER"    , I18N.getString(EntityManager.class,"EntityManager.EntityType.driver"   ,"Driver"     )),
        PERSON      (  200, "PERSON"    , I18N.getString(EntityManager.class,"EntityManager.EntityType.person"   ,"Person"     )),
        ANIMAL      (  300, "ANIMAL"    , I18N.getString(EntityManager.class,"EntityManager.EntityType.animal"   ,"Animal"     )),
        CONTAINER   (  400, "CONTAINER" , I18N.getString(EntityManager.class,"EntityManager.EntityType.container","Container"  )),
        PACKAGE     (  500, "PACKAGE"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.package"  ,"Package"    )),
        TOOL        (  600, "TOOL"      , I18N.getString(EntityManager.class,"EntityManager.EntityType.tool"     ,"Tool"       )),
        EQUIPMENT   (  700, "EQUIPMENT" , I18N.getString(EntityManager.class,"EntityManager.EntityType.equipment","Equipment"  )),
        RFID_00     (  900, "RFID_00"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_0"   ,"RFID Type 0")),
        RFID_01     (  901, "RFID_01"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_1"   ,"RFID Type 1")),
        RFID_02     (  902, "RFID_02"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_2"   ,"RFID Type 2")),
        RFID_03     (  903, "RFID_03"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_3"   ,"RFID Type 3")),
        RFID_04     (  904, "RFID_04"   , I18N.getString(EntityManager.class,"EntityManager.EntityType.rfid_4"   ,"RFID Type 4"));
        // ---
        private int         vv = 0;
        private String      nn = "";
        private I18N.Text   aa = null;
        EntityType(int v, String n, I18N.Text a)    { vv = v; nn = n; aa = a; }
        public String  getName()                    { return nn; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isTrailer()                  { return this.equals(TRAILER); }
        public boolean isDriver()                   { return this.equals(DRIVER); }
        public boolean isPerson()                   { return this.equals(PERSON); }
        public boolean isRFID()                     { return this.equals(RFID_00)||this.equals(RFID_01)||this.equals(RFID_02)||this.equals(RFID_03)||this.equals(RFID_04); }
        public boolean isType(int type)             { return this.getIntValue() == type; }
    }

    /**
    *** Returns the default EntityType
    *** @return The default EntityType
    **/
    public static EntityType getDefaultEntityType()
    {
        return EnumTools.getDefault(EntityType.class);
    }

    /**
    *** Returns the specified EntityType, or the default EntityType if the specified
    *** EntityType is null.
    *** @param et An EntityType
    *** @return A non-null EntityType
    **/
    public static EntityType getEntityType(EntityType et)
    {
        return (et != null)? et : EntityManager.getDefaultEntityType();
    }

    /**
    *** Returns the named EntityType, or the default EntityType of the specified
    *** name is null or invalid.
    *** @param etn The name of the EntityType
    *** @return A non-null EntityType
    **/
    public static EntityType getEntityTypeFromName(String etn)
    {
        EntityType et = EntityManager.getEntityTypeFromName(etn,null);
        return (et != null)? et : EntityManager.getDefaultEntityType();
    }

    /**
    *** Returns the EntityType for the specified value, or the default EntityType 
    *** if the specified value is not in the defined list of EntityTypes.
    *** @param etc An EntityType code value
    *** @param dft The default EntityType to return if the specified name is invalid.
    *** @return A non-null EntityType
    **/
    public static EntityType getEntityTypeFromCode(long etc, EntityType dft)
    {
        return EnumTools.getValueOf(EntityType.class, (int)etc, dft);
    }

    /**
    *** Returns the named EntityType, or the specified default EntityType of the
    *** name is null or invalid.
    *** @param etn The name of the EntityType
    *** @param dft The default EntityType to return if the specified name is invalid.
    *** @return A non-null EntityType
    **/
    public static EntityType getEntityTypeFromName(String etn, EntityType dft)
    {
        if (!StringTools.isBlank(etn)) {
            if (etn.equalsIgnoreCase("TRAILER"  )) { return EntityType.TRAILER;   } 
            if (etn.equalsIgnoreCase("DRIVER"   )) { return EntityType.DRIVER;    } 
            if (etn.equalsIgnoreCase("PERSON"   )) { return EntityType.PERSON;    } 
            if (etn.equalsIgnoreCase("ANIMAL"   )) { return EntityType.ANIMAL;    } 
            if (etn.equalsIgnoreCase("CONTAINER")) { return EntityType.CONTAINER; } 
            if (etn.equalsIgnoreCase("PACKAGE"  )) { return EntityType.PACKAGE;   } 
            if (etn.equalsIgnoreCase("TOOL"     )) { return EntityType.TOOL;      } 
            if (etn.equalsIgnoreCase("EQUIPMENT")) { return EntityType.EQUIPMENT; } 
            if (etn.equalsIgnoreCase("EQUIP"    )) { return EntityType.EQUIPMENT; } 
            if (etn.equalsIgnoreCase("RFID"     )) { return EntityType.RFID_00;   } 
            if (etn.equalsIgnoreCase("RFID_00"  )) { return EntityType.RFID_00;   } 
            if (etn.equalsIgnoreCase("RFID_0"   )) { return EntityType.RFID_00;   } 
            if (etn.equalsIgnoreCase("RFID_01"  )) { return EntityType.RFID_01;   } 
            if (etn.equalsIgnoreCase("RFID_1"   )) { return EntityType.RFID_01;   } 
            if (etn.equalsIgnoreCase("RFID_02"  )) { return EntityType.RFID_02;   } 
            if (etn.equalsIgnoreCase("RFID_2"   )) { return EntityType.RFID_02;   } 
            if (etn.equalsIgnoreCase("RFID_03"  )) { return EntityType.RFID_03;   } 
            if (etn.equalsIgnoreCase("RFID_3"   )) { return EntityType.RFID_03;   } 
            if (etn.equalsIgnoreCase("RFID_04"  )) { return EntityType.RFID_04;   }
            if (etn.equalsIgnoreCase("RFID_4"   )) { return EntityType.RFID_04;   }
        }
        return dft;
    }

    /**
    *** Returns a 2-element array of connect/disconnect StatusCodes that most closely match
    *** the specified EntityType.
    *** @param etype  The EntityType
    *** @return A non-null 2-element array of connect/disconnect StatusCodes
    **/
    public static int[] getStatusCodesForEntityType(EntityType etype)
    {
        EntityManager.EntityType et = EntityManager.getEntityType(etype);
        switch (et) {
            case TRAILER :
                return new int[] { StatusCodes.STATUS_TRAILER_HOOK, StatusCodes.STATUS_TRAILER_UNHOOK };
            case DRIVER :
                return new int[] { StatusCodes.STATUS_LOGIN, StatusCodes.STATUS_LOGOUT };
            case PERSON :
                return new int[] { StatusCodes.STATUS_PERSON_ENTER, StatusCodes.STATUS_PERSON_EXIT };
            case RFID_00 :
            case RFID_01 :
            case RFID_02 :
            case RFID_03 :
            case RFID_04 :
                return new int[] { StatusCodes.STATUS_RFID_CONNECT, StatusCodes.STATUS_RFID_DISCONNECT };
            default:
                return new int[] { StatusCodes.STATUS_ENTITY_CONNECT, StatusCodes.STATUS_ENTITY_DISCONNECT };
        }
    }

    /**
    *** Returns the best EntityType for the specified status code
    **/
    public static EntityManager.EntityType getEntityTypeForStatusCode(int sc, EntityManager.EntityType dft)
    {
        switch (sc) {
            case StatusCodes.STATUS_LOGIN:
            case StatusCodes.STATUS_LOGOUT:
                return EntityManager.EntityType.DRIVER;
            case StatusCodes.STATUS_TRAILER_STATE:
            case StatusCodes.STATUS_TRAILER_HOOK:
            case StatusCodes.STATUS_TRAILER_UNHOOK:
            case StatusCodes.STATUS_TRAILER_INVENTORY:
                return EntityManager.EntityType.TRAILER;
            case StatusCodes.STATUS_RFID_STATE:
            case StatusCodes.STATUS_RFID_CONNECT:
            case StatusCodes.STATUS_RFID_DISCONNECT:
            case StatusCodes.STATUS_RFID_INVENTORY:
                return EntityManager.EntityType.RFID_00;
            case StatusCodes.STATUS_PERSON_STATE:
            case StatusCodes.STATUS_PERSON_ENTER:
            case StatusCodes.STATUS_PERSON_EXIT:
            case StatusCodes.STATUS_PERSON_INVENTORY:
                return EntityManager.EntityType.PERSON;
        }
        return dft;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public abstract boolean insertEntityChange(EventData event);

    public abstract String[] getAttachedEntityIDs(String accountID, String deviceID, long entityType) 
        throws DBException;

    public abstract String[] getAttachedEntityDescriptions(String accountID, String deviceID, long entityType) 
        throws DBException;

    public abstract String getEntityDescription(String accountID, String entityID, long entityType);

    // ------------------------------------------------------------------------

    public abstract boolean isEntityAttached(String accountID, String deviceID, String entityID, long entityType);

    // ------------------------------------------------------------------------

}
