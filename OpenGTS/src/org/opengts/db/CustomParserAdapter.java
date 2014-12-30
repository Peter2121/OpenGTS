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
//  2013/04/08  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.db.tables.*;

public class CustomParserAdapter
    implements CustomParser
{

    // ------------------------------------------------------------------------

    private static Map<String,Object> _InitTemporaryVars(
        Map<String,Object> evMap,
        Device dev, String xportMethod,
        String type, Object dataObj,
        int count, int index) 
    {
        Map<String,Object> map = (evMap != null)? evMap : new HashMap<String,Object>();

        /* add device fields */
        if (dev != null) {
            map.put(CustomParser.ACCOUNT    , dev.getAccount());                    // Account
            map.put(CustomParser.DEVICE     , dev);                                 // Device
        }

        /* data transport method */
        if (!StringTools.isBlank(xportMethod)) {
            boolean duplex = xportMethod.equalsIgnoreCase("TCP")? true : false;
            map.put(CustomParser.TRANSPORT  , xportMethod);                         // Boolean
            map.put(CustomParser.DUPLEX     , new Boolean(duplex));                 // Boolean
        }

        /* data */
        map.put(CustomParser.DATA_TYPE      , StringTools.trim(type));              // String
        map.put(CustomParser.DATA           , dataObj);                             // byte[] | String

        /* count/index */
        if (count > 0) {
            map.put(CustomParser.COUNT      , new Integer(count));                  // Integer
        }
        if (index >= 0) {
            map.put(CustomParser.INDEX      , new Integer(index));                  // Integer
        }

        return map;
    }

    /**
    *** Initialize CustomParser EventData field map with temporary variables
    *** @param map       The EventData field map to initialize
    *** @param device    The current Device instance (may be null)
    *** @param transport Transport method (ie. "TCP", "UDP", "Satellite", etc)
    *** @param dataType  The data type (as defined by the DCS)
    *** @param data      The data
    *** @param count     Total number of events this session
    *** @param index     Index of current event within session
    *** @return The EventData field map
    **/
    public static Map<String,Object> InitTemporaryVars(
        Map<String,Object> map, 
        Device device, String transport,
        String dataType, byte data[],
        int count, int index) 
    {
        return CustomParserAdapter._InitTemporaryVars(map, 
            device, transport, 
            dataType, (Object)data, 
            count, index);
    }

    /**
    *** Initialize CustomParser EventData field map with temporary variables
    *** @param map       The EventData field map to initialize
    *** @param device    The current Device instance (may be null)
    *** @param transport Transport method (ie. "TCP", "UDP", "Satellite", etc)
    *** @param dataType  The data type (as defined by the DCS)
    *** @param data      The data
    *** @param count     Total number of events this session
    *** @param index     Index of current event within session
    *** @return The EventData field map
    **/
    public static Map<String,Object> InitTemporaryVars(
        Map<String,Object> map, 
        Device device, String transport,
        int dataType, byte data[],
        int count, int index) 
    {
        return CustomParserAdapter._InitTemporaryVars(map, 
            device, transport, 
            String.valueOf(dataType), (Object)data, 
            count, index);
    }

    /**
    *** Initialize CustomParser EventData field map with temporary variables
    *** @param map       The EventData field map to initialize
    *** @param device    The current Device instance (may be null)
    *** @param transport Transport method (ie. "TCP", "UDP", "Satellite", etc)
    *** @param dataType  The data type (as defined by the DCS)
    *** @param data      The data
    *** @param count     Total number of events this session
    *** @param index     Index of current event within session
    *** @return The EventData field map
    **/
    public static Map<String,Object> InitTemporaryVars(
        Map<String,Object> map, 
        Device device, String transport,
        String dataType, String data,
        int count, int index) 
    {
        return CustomParserAdapter._InitTemporaryVars(map, 
            device, transport, 
            dataType, (Object)data, 
            count, index);
    }

    /**
    *** Initialize CustomParser EventData field map with temporary variables
    *** @param map       The EventData field map to initialize
    *** @param device    The current Device instance (may be null)
    *** @param transport Transport method (ie. "TCP", "UDP", "Satellite", etc)
    *** @param dataType  The data type (as defined by the DCS)
    *** @param data      The data
    *** @param count     Total number of events this session
    *** @param index     Index of current event within session
    *** @return The EventData field map
    **/
    public static Map<String,Object> InitTemporaryVars(
        Map<String,Object> map, 
        Device device, String transport,
        int dataType, String data,
        int count, int index) 
    {
        return CustomParserAdapter._InitTemporaryVars(map, 
            device, transport, 
            String.valueOf(dataType), (Object)data, 
            count, index);
    }

    // ------------------------------------------------------------------------

    /**
    *** Removes the temperature variables from the specified EventData field map
    *** @param evMap  The EventData field map
    *** @return The specified EventData field map
    **/
    public static Map<String,Object> ClearTemporaryVars(Map<String,Object> evMap)
    {
        if (evMap != null) {
            evMap.remove(CustomParser.ACCOUNT  );
            evMap.remove(CustomParser.DEVICE   );
            evMap.remove(CustomParser.TRANSPORT);
            evMap.remove(CustomParser.DUPLEX   );
            evMap.remove(CustomParser.DATA     );
            evMap.remove(CustomParser.DATA_TYPE);
            evMap.remove(CustomParser.COUNT    );
            evMap.remove(CustomParser.INDEX    );
        }
        return evMap;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback to parse raw data received from a remote tracking device through its
    *** device communication server.
    *** @param account  The assigned device Account instance
    *** @param device   The assigned Device instance
    *** @param type     The type of data contained in "data"
    *** @param data     The byte array containing the raw data
    *** @param props    A map where parsed data should be placed (to be inserted into the EventData record)
    *** @return The response which will be sent back to the device
    **/
    public byte[] parseData(
        Account account, Device device, 
        String type, byte data[], 
        Map<String,Object> props) {
        return null;
    }

}
