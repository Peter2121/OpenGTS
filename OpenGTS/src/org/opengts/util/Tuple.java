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
//  This class provides Tuples
// ----------------------------------------------------------------------------
// Change History:
//  2009/06/01  Martin D. Flynn
//     -Initial release
//  2013/04/08  Martin D. Flynn
//     -Added "Quad"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** <code>Tuple</code> a wrapper class for Tuples
**/

public class Tuple
{

    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a single item
    **/
    public static class Single<AA>
    {
        public AA  a = null;
        public Single(AA a) {
            this.a = a;
        }
        public String toString() {
            return (this.a != null)? this.a.toString() : "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a pair of items
    **/
    public static class Pair<AA,BB>
    {
        public AA  a = null;
        public BB  b = null;
        public Pair(AA a, BB b) {
            this.a = a;
            this.b = b;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append((this.a != null)? this.a.toString() : "");
            sb.append(",");
            sb.append((this.b != null)? this.b.toString() : "");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a triple of items
    **/
    public static class Triple<AA,BB,CC>
    {
        public AA  a = null;
        public BB  b = null;
        public CC  c = null;
        public Triple(AA a, BB b, CC c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append((this.a != null)? this.a.toString() : "");
            sb.append(",");
            sb.append((this.b != null)? this.b.toString() : "");
            sb.append(",");
            sb.append((this.c != null)? this.c.toString() : "");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** A wrapper for a quadruple of items
    **/
    public static class Quad<AA,BB,CC,DD>
    {
        public AA  a = null;
        public BB  b = null;
        public CC  c = null;
        public DD  d = null;
        public Quad(AA a, BB b, CC c, DD d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append((this.a != null)? this.a.toString() : "");
            sb.append(",");
            sb.append((this.b != null)? this.b.toString() : "");
            sb.append(",");
            sb.append((this.c != null)? this.c.toString() : "");
            sb.append(",");
            sb.append((this.d != null)? this.d.toString() : "");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------

}
