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
//  Accelerometer information container
// ----------------------------------------------------------------------------
// Change History:
//  2013/03/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

/**
*** Accerometer XYZ-axis container.
**/

public class Accelerometer
{

    // ------------------------------------------------------------------------

    private double  xAxis   = 0.0;
    private double  yAxis   = 0.0;
    private double  zAxis   = 0.0;

    /**
    *** Constructor
    **/
    public Accelerometer()
    {
        this(0.0,0.0,0.0);
    }

    /**
    *** Constructor
    *** @param x  X-Axis acceleration (meters/second/second)
    *** @param y  Y-Axis acceleration (meters/second/second)
    *** @param z  Z-Axis acceleration (meters/second/second)
    **/
    public Accelerometer(double x, double y, double z)
    {
        this.xAxis = x;
        this.yAxis = y;
        this.zAxis = z;
    }

    /**
    *** Constructor
    *** @param xyz  Comma separated XYZ-Axis acceleration
    **/
    public Accelerometer(String xyz)
    {
        if (StringTools.isBlank(xyz)) {
            this.xAxis = 0.0;
            this.yAxis = 0.0;
            this.zAxis = 0.0;
        } else {
            String A[];
            if (xyz.indexOf(",") >= 0) {
                A = StringTools.split(xyz,',');   // "0.0,0.0,0.0"
            } else 
            if (xyz.indexOf("/") >= 0) {
                A = StringTools.split(xyz,'/');   // "0.0/0.0/0.0"
            } else
            if (xyz.indexOf("|") >= 0) {
                A = StringTools.split(xyz,'|');   // "0.0|0.0|0.0"
            } else {
                A = new String[] { xyz };
            }
            this.xAxis = (A.length > 0)? StringTools.parseDouble(A[0],0.0) : 0.0;
            this.yAxis = (A.length > 1)? StringTools.parseDouble(A[1],0.0) : 0.0;
            this.zAxis = (A.length > 2)? StringTools.parseDouble(A[2],0.0) : 0.0;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the X-Axis accelerometer value
    *** @return The X-Axis accelerometer value
    **/
    public double getXAxis()
    {
        return this.xAxis;
    }

    /**
    *** Gets the Y-Axis accelerometer value
    *** @return The Y-Axis accelerometer value
    **/
    public double getYAxis()
    {
        return this.yAxis;
    }

    /**
    *** Gets the Z-Axis accelerometer value
    *** @return The Z-Axis accelerometer value
    **/
    public double getZAxis()
    {
        return this.zAxis;
    }

    // ------------------------------------------------------------------------

    // TODO: getGForce (G-Force)
    // TODO: getAcceleration (meters/second/second)
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the String representation of this instance
    *** @return The String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(StringTools.format(this.getXAxis(),"0.00000"));
        sb.append(",");
        sb.append(StringTools.format(this.getYAxis(),"0.00000"));
        sb.append(",");
        sb.append(StringTools.format(this.getZAxis(),"0.00000"));
        return sb.toString();
    }

    // ------------------------------------------------------------------------

}
