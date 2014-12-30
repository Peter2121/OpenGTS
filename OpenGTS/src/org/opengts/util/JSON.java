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
//  2011/07/15  Martin D. Flynn
//     -Initial release
//  2011/08/21  Martin D. Flynn
//     -Fixed JSON parsing.
//  2011/10/03  Martin D. Flynn
//     -Added multiple-name lookup support
//  2013/03/01  Martin D. Flynn
//     -Added 'null' object support
//  2013/04/08  Martin D. Flynn
//     -Handle parsing of arrays within arrays
//  2013/08/06  Martin D. Flynn
//     -Added "JSONParsingContext" for easier debugging of syntax errors
//     -Added support for "/*...*/" comments (NOTE: this is a non-standard feature
//      which is NOT supported by other JSON parsers, including JavaScript).
//  2013/11/11  Martin D. Flynn
//     -Added additional overflow checking.
//  2014/09/25  Martin D. Flynn
//     -Added "toString(boolean inclPrefix)" to JSON object.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.IOException;
import java.io.InputStream;

public class JSON
{

    // ------------------------------------------------------------------------

    private static final boolean CASE_SENSITIVE = false;

    private static boolean NameEquals(String n1, String n2)
    {
        if ((n1 == null) || (n2 == null)) {
            return false;
        } else
        if (CASE_SENSITIVE) {
            return n1.equals(n2);
        } else {
            return n1.equalsIgnoreCase(n2);
        }
    }

    // ------------------------------------------------------------------------

    private static final String INDENT = "   ";

    /**
    *** Return indent spaces
    **/
    private static String indent(int count)
    {
        return StringTools.replicateString(INDENT,count);
    }

    // ------------------------------------------------------------------------

    private static final char ESCAPE_CHAR = '\\';

    /**
    *** Converts the specified String to a JSON escaped value String.<br>
    *** @param s  The String to convert to a JSON encoded String
    *** @return The JSON encoded String
    **/
    public static String escapeJSON(String s)
    {
        if (s != null) {
            StringBuffer sb = new StringBuffer();
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char ch = s.charAt(i);
                if (ch == ESCAPE_CHAR) {
                    sb.append(ESCAPE_CHAR).append(ESCAPE_CHAR);
                } else
                if (ch == '\n') {
                    sb.append(ESCAPE_CHAR).append('n');
                } else
                if (ch == '\r') {
                    sb.append(ESCAPE_CHAR).append('r');
                } else
                if (ch == '\t') {
                    sb.append(ESCAPE_CHAR).append('t');
                } else
                //if (ch == '\'') {
                //    sb.append(ESCAPE_CHAR).append('\''); <-- should not be escaped
                //} else
                if (ch == '\"') {
                    sb.append(ESCAPE_CHAR).append('\"');
                } else
                if ((ch >= 0x0020) && (ch <= 0x007e)) {
                    sb.append(ch);
                } else {
                    // ignore character?
                    sb.append(ch);
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** JSON Parsing Context
    **/
    public static class JSONParsingContext
    {
        private int index = 0;
        private int line  = 1;
        public JSONParsingContext() {
            this.index = 0;
            this.line  = 1;
        }
        public int getIndex() {
            return this.index;
        }
        public void incrementIndex(int val) {
            this.index += val;
        }
        public void incrementIndex() {
            this.index++;
        }
        public int getLine() {
            return this.line;
        }
        public void incrementLine() {
            this.line++;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.line);
            sb.append("/");
            sb.append(this.index);
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** JSON Parse Exception
    **/
    public static class JSONParsingException
        extends Exception
    {
        private int index = 0;
        private int line  = 0;
        public JSONParsingException(String msg, JSONParsingContext context) {
            super(msg);
            this.index = (context != null)? context.getIndex() : -1;
            this.line  = (context != null)? context.getLine()  : -1;
        }
        public int getIndex() {
            return this.index;
        }
        public int getLine() {
            return this.line;
        }
        public String toString() { // JSON.JSONParsingException
            String s = super.toString();
            return s + " ["+this.line+"/"+this.index+"]";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Object
    
    /**
    *** JSON Object class
    **/
    public static class _Object
        extends Vector<JSON._KeyValue>
    {

        private boolean formatIndent = true;

        /**
        *** Constructor
        **/
        public _Object() {
            super();
        }

        /**
        *** Constructor
        **/
        public _Object(Vector<JSON._KeyValue> list) {
            this();
            this.addAll(list);
        }

        /**
        *** Constructor
        **/
        public _Object(JSON._KeyValue... kv) {
            this();
            if (kv != null) {
                for (int i = 0; i < kv.length; i++) {
                    this.add(kv[i]);
                }
            }
        }

        // --------------------------------------

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(JSON._KeyValue kv) {
            return super.add(kv);
        }
        public boolean add(JSON._KeyValue kv) {
            return super.add(kv);
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, String value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, int value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, long value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, double value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, boolean value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Array value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Object value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Value value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        // --------------------------------------

        /**
        *** Gets the number of key/value pairs in this object
        **/
        public int getKeyValueCount() {
            return super.size();
        }

        /**
        *** Gets the key/value pair at the specified index
        **/
        public JSON._KeyValue getKeyValueAt(int ndx) {
            if ((ndx >= 0) && (ndx < this.size())) {
                return this.get(ndx);
            } else {
                return null;
            }
        }

        // --------------------------------------

        /**
        *** Gets the key/value pair for the specified name
        **/
        public JSON._KeyValue getKeyValue(String n) {
            if (n != null) {
                for (JSON._KeyValue kv : this) {
                    String kvn = kv.getKey();
                    if (JSON.NameEquals(n,kvn)) {
                        return kv;
                    }
                }
            }
            return null;
        }

        /**
        *** Gets the JSON._Value for the specified name
        **/
        public JSON._Value getValueForName(String n) {
            JSON._KeyValue kv = this.getKeyValue(n);
            return (kv != null)? kv.getValue() : null;
        }

        /**
        *** Gets the JSON._Value for the specified name
        **/
        public JSON._Value getValueForName(String name[]) {
            if (name != null) {
                for (String n : name) {
                    JSON._Value jv = this.getValueForName(n);
                    if (jv != null) {
                        return jv;
                    }
                }
            }
            return null;
        }

        // --------------------------------------

        /**
        *** Gets the JSON._Array for the specified name
        **/
        public JSON._Array getArrayForName(String name, JSON._Array dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getArrayValue(dft) : dft;
        }

        /**
        *** Gets the JSON._Array for the specified name
        **/
        public JSON._Array getArrayForName(String name[], JSON._Array dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getArrayValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the JSON._Array for the specified name
        **/
        public String[] getStringArrayForName(String name, String dft[]) {
            JSON._Value jv = this.getValueForName(name);
            JSON._Array ar = (jv != null)? jv.getArrayValue(null) : null;
            return (ar != null)? ar.getStringArray() : dft;
        }

        /**
        *** Gets the JSON._Array for the specified name
        **/
        public String[] getStringArrayForName(String name[], String dft[]) {
            JSON._Value jv = this.getValueForName(name);
            JSON._Array ar = (jv != null)? jv.getArrayValue(null) : null;
            return (ar != null)? ar.getStringArray() : dft;
        }

        // --------------------------------------

        /**
        *** Gets the JSON._Object value for the specified name
        **/
        public JSON._Object getObjectForName(String name, JSON._Object dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getObjectValue(dft) : dft;
        }

        /**
        *** Gets the JSON._Object value for the specified name
        **/
        public JSON._Object getObjectForName(String name[], JSON._Object dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getObjectValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the String value for the specified name
        **/
        public String getStringForName(String name, String dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getStringValue(dft) : dft;
        }

        /**
        *** Gets the String value for the specified name
        **/
        public String getStringForName(String name[], String dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getStringValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the Integer value for the specified name
        **/
        public int getIntForName(String name, int dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getIntValue(dft) : dft;
        }

        /**
        *** Gets the Integer value for the specified name
        **/
        public int getIntForName(String name[], int dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getIntValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the Long value for the specified name
        **/
        public long getLongForName(String name, long dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getLongValue(dft) : dft;
        }

        /**
        *** Gets the Long value for the specified name
        **/
        public long getLongForName(String name[], long dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getLongValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the Double value for the specified name
        **/
        public double getDoubleForName(String name, double dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getDoubleValue(dft) : dft;
        }

        /**
        *** Gets the Double value for the specified name
        **/
        public double getDoubleForName(String name[], double dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getDoubleValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets the String value for the specified name
        **/
        public boolean getBooleanForName(String name, boolean dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getBooleanValue(dft) : dft;
        }

        /**
        *** Gets the String value for the specified name
        **/
        public boolean getBooleanForName(String name[], boolean dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getBooleanValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** Gets a list of all key names in this object
        **/
        public Collection<String> getKeyNames() {
            Collection<String> keyList = new Vector<String>();
            for (JSON._KeyValue kv : this) {
                keyList.add(kv.getKey());
            }
            return keyList;
        }
        
        /**
        *** Print object contents (for debug purposes only)
        **/
        public void debugDisplayObject(int level) {
            String pfx0 = StringTools.replicateString(INDENT,level);
            String pfx1 = StringTools.replicateString(INDENT,level+1);
            for (String key : this.getKeyNames()) {
                JSON._KeyValue kv = this.getKeyValue(key);
                Object val = kv.getValue().getObjectValue();
                Print.sysPrintln(pfx0 + key + " ==> " + StringTools.className(val));
                if (val instanceof JSON._Object) {
                    JSON._Object obj = (JSON._Object)val;
                    obj.debugDisplayObject(level+1);
                } else
                if (val instanceof JSON._Array) {
                    JSON._Array array = (JSON._Array)val;
                    for (JSON._Value jv : array) {
                        Object av = jv.getObjectValue();
                        Print.sysPrintln(pfx1 + " ==> " + StringTools.className(av));
                        if (av instanceof JSON._Object) {
                            JSON._Object obj = (JSON._Object)av;
                            obj.debugDisplayObject(level+2);
                        }
                    }
                }
            }
        }

        // --------------------------------------

        /**
        *** Set format indent state
        **/
        public _Object setFormatIndent(boolean indent) {
            this.formatIndent = indent;
            return this;
        }

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            boolean fullFormat = this.formatIndent && (prefix >= 0);
            String pfx0 = fullFormat? JSON.indent(prefix)   : "";
            String pfx1 = fullFormat? JSON.indent(prefix+1) : "";
            sb.append("{");
            if (fullFormat) {
                sb.append("\n");
            }
            if (this.size() > 0) {
                int size = this.size();
                for (int i = 0; i < size; i++) {
                    JSON._KeyValue kv = this.get(i);
                    sb.append(pfx1);
                    kv.toStringBuffer((fullFormat?(prefix+1):-1),sb);
                    if ((i + 1) < size) {
                        sb.append(",");
                    }
                    if (fullFormat) {
                        sb.append("\n");
                    }
                }
            }
            sb.append(pfx0).append("}");
            if (fullFormat && (prefix == 0)) {
                sb.append("\n");
            }
            return sb;
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() { // JSON._Object
            return this.toStringBuffer(0,null).toString();
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString(boolean inclPrefix) { // JSON._Object
            return this.toStringBuffer((inclPrefix?0:-1),null).toString();
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse a JSON Comment from the specified String, starting at the 
    *** specified location
    **/
    public static String parse_Comment(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        String       val  = null;

        /* skip leading whitespace */
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else {
                break;
            }
        }

        /* next characters must be "/*" */
        int startLine  = context.getLine();
        int startIndex = context.getIndex();
        if ((startIndex + 2) >= len) {
            throw new JSONParsingException("Overflow", context);
        } else
        if ((v.charAt(startIndex  ) != '/') ||
            (v.charAt(startIndex+1) != '*')   ) {
            throw new JSONParsingException("Invalid beginning of comment", context);
        }
        context.incrementIndex(2);

        /* parse comment body */
        StringBuffer comment = new StringBuffer();
        commentParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                comment.append(ch);
                continue; // skip space
            } else
            if (ch == '*') {
                context.incrementIndex();
                int ndx = context.getIndex();
                if (ndx >= len) {
                    throw new JSONParsingException("Overflow", context);
                } else
                if ((v.charAt(ndx) == '/')) {
                    context.incrementIndex(); // consume final '/'
                    break commentParse;
                } else {
                    comment.append(ch);
                }
                continue;
            } else {
                comment.append(ch);
                context.incrementIndex();
            }
        } // commentParse
        val = comment.toString().trim();

        /* return comment */
        return val;

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse a JSON Object from the specified String
    **/
    public static _Object parse_Object(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Object(v,new JSONParsingContext());
    }

    /**
    *** Parse a JSON Object from the specified String, starting at the 
    *** specified location
    **/
    public static _Object parse_Object(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        JSON._Object obj  = null;
        boolean      comp = false;

        objectParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                // -- skip whitespace
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                // -- start of comment (non-standard JSON)
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '{') {
                // -- start of object
                if (obj != null) {
                    throw new JSONParsingException("Object already started", context);
                }
                context.incrementIndex();
                obj = new JSON._Object();
            } else
            if (ch == '\"') {
                // -- "key": VALUE
                if (obj == null) {
                    throw new JSONParsingException("No start of Object", context);
                }
                JSON._KeyValue kv = JSON.parse_KeyValue(v, context);
                if (kv == null) {
                    throw new JSONParsingException("Invalid KeyValue ...", context);
                }
                obj.add(kv);
            } else
            if (ch == ',') {
                // -- ignore extraneous commas (non-standard JSON)
                context.incrementIndex();
            } else
            if (ch == '}') {
                // -- end of object
                context.incrementIndex();
                if (obj != null) {
                    comp = true; // iff Object is defined
                }
                break objectParse;
            } else {
                // -- invalid character
                throw new JSONParsingException("Invalid JSON syntax ...", context);
            }
        } // objectParse

        /* object completed? */
        if (!comp || (obj == null)) {
            throw new JSONParsingException("Incomplete Object", context);
        }

        /* return object */
        return obj;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._KeyValue

    /**
    *** JSON Key/Value pair
    **/
    public static class _KeyValue
    {

        private String      key   = null;
        private JSON._Value value = null;

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, JSON._Value value) {
            this.key   = key;
            this.value = value;
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, String value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, long value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, double value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, boolean value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, JSON._Array value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Constructor 
        **/
        public _KeyValue(String key, JSON._Object value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** Gets the key of this key/value pair 
        **/
        public String getKey() {
            return this.key;
        }
        
        /**
        *** Gets the value of this key/value pair 
        **/
        public JSON._Value getValue() {
            return this.value;
        }

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            sb.append("\"").append(this.key).append("\"");
            sb.append(":");
            if (prefix >= 0) {
                sb.append(" ");
            }
            if (this.value != null) {
                this.value.toStringBuffer(prefix,sb);
            } else {
                sb.append("null");
            }
            return sb;
        }
        
        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() { // JSON._KeyValue
            return this.toStringBuffer(1,null).toString();
        }

    }
    
    /**
    *** Parse a Key/Value pair from the specified String at the specified location
    **/
    public static JSON._KeyValue parse_KeyValue(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int            len  = StringTools.length(v);
        JSON._KeyValue kv   = null;
        boolean        comp = false;

        String key = null;
        boolean colon = false;
        keyvalParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (!colon && (ch == '\"')) {
                // Key
                key = JSON.parse_String(v, context);
                if (key == null) {
                    throw new JSONParsingException("Invalid key String", context);
                }
            } else
            if (ch == ':') {
                if (colon) {
                    throw new JSONParsingException("More than one ':'", context);
                } else
                if (key == null) {
                    throw new JSONParsingException("Key not defined", context);
                }
                context.incrementIndex();
                colon = true;
            } else {
                // JSON._Value
                JSON._Value val = JSON.parse_Value(v, context);
                if (val == null) {
                    throw new JSONParsingException("Invalid value", context);
                }
                kv = new JSON._KeyValue(key,val);
                comp = true;
                break keyvalParse;
            }
        } // keyvalParse

        /* key/value completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Key/Value", context);
        }

        /* return key/value */
        return kv; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Value

    /**
    *** JSON Value
    **/
    public static class _Value
    {

        private Object value = null;

        /**
        *** Constructor 
        **/
        public _Value() {
            this.value = null;
        }

        /**
        *** Constructor 
        **/
        public _Value(String v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(Integer v) {
            this.value = (v != null)? new Long(v.longValue()) : null;
        }

        /**
        *** Constructor 
        **/
        public _Value(int v) {
            this.value = new Long((long)v);
        }

        /**
        *** Constructor 
        **/
        public _Value(Long v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(long v) {
            this.value = new Long(v);
        }

        /**
        *** Constructor 
        **/
        public _Value(Double v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(double v) {
            this.value = new Double(v);
        }

        /**
        *** Constructor 
        **/
        public _Value(Boolean v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(boolean v) {
            this.value = new Boolean(v);
        }

        /**
        *** Constructor 
        **/
        public _Value(JSON._Array v) {
            this.value = v;
        }

        /**
        *** Constructor 
        **/
        public _Value(JSON._Object v) {
            this.value = v;
        }

        // -----------------------------------------

        /**
        *** Gets the value
        *** (may be null)
        **/
        public Object getObjectValue() {
            return this.value;
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a nul Object 
        **/
        public boolean isNullValue() {
            return (this.value == null);
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a String 
        **/
        public boolean isStringValue() {
            return (this.value instanceof String);
        }

        /**
        *** Gets the String representation of this value if the value type is one of
        *** String, Number, or Boolean
        **/
        public String getStringValue(String dft) {
            if (this.value instanceof String) {
                return (String)this.value;
            } else
            if (this.value instanceof Number) {
                return this.value.toString();
            } else
            if (this.value instanceof Boolean) {
                return this.value.toString();
            } else  {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents an Integer 
        **/
        public boolean isIntValue() {
            return (this.value instanceof Integer);
        }

        /**
        *** Gets the Integer representation of this value if the value type is one of
        *** Number(intValue), String(parseInt), or Boolean('0' if false, '1' otherwise)
        **/
        public int getIntValue(int dft) {
            if (this.value instanceof Number) {
                return ((Number)this.value).intValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseInt(this.value,dft);
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1 : 0;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a Long 
        **/
        public boolean isLongValue() {
            return (this.value instanceof Long);
        }

        /**
        *** Gets the Long representation of this value if the value type is one of
        *** Number(longValue), String(parseLong), or Boolean('0' if false, '1' otherwise)
        **/
        public long getLongValue(long dft) {
            if (this.value instanceof Number) {
                return ((Number)this.value).longValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseLong(this.value,dft);
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1L : 0L;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a Double 
        **/
        public boolean isDoubleValue() {
            return (this.value instanceof Double);
        }

        /**
        *** Gets the Double representation of this value if the value type is one of
        *** Number(doubleValue), String(parseDouble), or Boolean('0.0' if false, '1.0' otherwise)
        **/
        public double getDoubleValue(double dft) {
            if (this.value instanceof Number) {
                return ((Number)this.value).doubleValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseDouble(this.value,dft);
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1.0 : 0.0;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a Boolean 
        **/
        public boolean isBooleanValue() {
            return (this.value instanceof Boolean);
        }

        /**
        *** Gets the Boolean representation of this value if the value type is one of
        *** Boolean(booleanValue), String(parseBoolean), or Number(false if '0', true otherwise)
        **/
        public boolean getBooleanValue(boolean dft) {
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue();
            } else
            if (this.value instanceof String) {
                return StringTools.parseBoolean(this.value,dft);
            } else
            if (this.value instanceof Number) {
                return (((Number)this.value).longValue() != 0L)? true : false;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a JSON._Array 
        **/
        public boolean isArrayValue() {
            return (this.value instanceof JSON._Array);
        }

        /**
        *** Gets the JSON._Array value 
        **/
        public JSON._Array getArrayValue(JSON._Array dft) {
            if (this.value instanceof JSON._Array) {
                return (JSON._Array)this.value;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns true if this value represents a JSON._Object 
        **/
        public boolean isObjectValue() {
            return (this.value instanceof JSON._Object);
        }

        /**
        *** Gets the JSON._Object value 
        **/
        public JSON._Object getObjectValue(JSON._Object dft) {
            if (this.value instanceof JSON._Object) {
                return (JSON._Object)this.value;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** Returns the class of the value object
        **/
        public Class getValueClass() {
            return (this.value != null)? this.value.getClass() : null;
        }
        
        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            if (this.value == null) {
                sb.append("null");
            } else
            if (this.value instanceof String) {
                sb.append("\"");
                sb.append(JSON.escapeJSON((String)this.value));
                sb.append("\"");
            } else
            if (this.value instanceof Number) {
                sb.append(this.value.toString());
            } else
            if (this.value instanceof Boolean) {
                sb.append(this.value.toString());
            } else 
            if (this.value instanceof JSON._Object) {
                ((JSON._Object)this.value).toStringBuffer(prefix, sb);
            } else
            if (this.value instanceof JSON._Array) {
                ((JSON._Array)this.value).toStringBuffer(prefix, sb);
            } else {
                // ignore
            }
            return sb;
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() { // JSON._Value
            return this.toStringBuffer(0,null).toString();
        }

    }

    /**
    *** Parse a JSON Array from the specified String
    **/
    public static JSON._Value parse_Value(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Value(v, new JSONParsingContext());
    }

    /**
    *** Parse JSON Value
    **/
    public static JSON._Value parse_Value(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        JSON._Value  val  = null;
        boolean      comp = false;

        valueParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '\"') {
                // parse String
                String sval = JSON.parse_String(v, context);
                if (sval == null) {
                    throw new JSONParsingException("Invalid String value", context);
                } else {
                    val = new JSON._Value(sval);
                }
                comp = true;
                break valueParse;
            } else
            if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                // parse Number
                Number num = JSON.parse_Number(v, context);
                if (num == null) {
                    throw new JSONParsingException("Invalid Number value", context);
                } else
                if (num instanceof Double) {
                    val = new JSON._Value((Double)num);
                } else
                if (num instanceof Integer) {
                    val = new JSON._Value((Integer)num);
                } else
                if (num instanceof Long) {
                    val = new JSON._Value((Long)num);
                } else {
                    throw new JSONParsingException("Unsupported Number type: " + StringTools.className(num), context);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == 't') { 
                // true
                context.incrementIndex();
                int ndx = context.getIndex();
                if ((ndx + 2) >= len) {
                    throw new JSONParsingException("Overflow", context);
                } else
                if ((v.charAt(ndx  ) == 'r') && 
                    (v.charAt(ndx+1) == 'u') && 
                    (v.charAt(ndx+2) == 'e')   ) {
                    context.incrementIndex(3);
                    val = new JSON._Value(Boolean.TRUE);
                } else {
                    throw new JSONParsingException("Invalid Boolean 'true'", context);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == 'f') { 
                // false
                context.incrementIndex();
                int ndx = context.getIndex();
                if ((ndx + 3) >= len) {
                    throw new JSONParsingException("Overflow", context);
                } else
                if ((v.charAt(ndx  ) == 'a') && 
                    (v.charAt(ndx+1) == 'l') && 
                    (v.charAt(ndx+2) == 's') &&
                    (v.charAt(ndx+3) == 'e')   ) {
                    context.incrementIndex(4);
                    val = new JSON._Value(Boolean.FALSE);
                } else {
                    throw new JSONParsingException("Invalid Boolean 'false'", context);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == 'n') { 
                // null
                context.incrementIndex();
                int ndx = context.getIndex();
                if ((ndx + 2) >= len) {
                    throw new JSONParsingException("Overflow", context);
                } else
                if ((v.charAt(ndx  ) == 'u') && 
                    (v.charAt(ndx+1) == 'l') && 
                    (v.charAt(ndx+2) == 'l')   ) {
                    context.incrementIndex(3);
                    val = new JSON._Value((JSON._Object)null); // null object
                } else {
                    throw new JSONParsingException("Invalid 'null'", context);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == '[') {
                // JSON._Array
                JSON._Array array = JSON.parse_Array(v, context);
                if (array == null) {
                    throw new JSONParsingException("Invalid array", context);
                }
                val = new JSON._Value(array);
                comp = true;
                break valueParse;
            } else
            if (ch == '{') {
                // JSON._Object
                JSON._Object obj = JSON.parse_Object(v, context);
                if (obj == null) {
                    throw new JSONParsingException("Invalid object", context);
                }
                val = new JSON._Value(obj);
                comp = true;
                break valueParse;
            } else {
                throw new JSONParsingException("Invalid character", context);
            }
        } // valueParse

        /* value completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Value", context);
        }

        /* return value */
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Array

    /**
    *** JSON Array 
    **/
    public static class _Array
        extends Vector<JSON._Value>
    {

        private boolean formatIndent = true;

        /**
        *** Constructor 
        **/
        public _Array() {
            super();
        }

        /**
        *** Constructor 
        *** An Array of other Values
        **/
        public _Array(JSON._Value... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.add(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Strings
        **/
        public _Array(String... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Longs
        **/
        public _Array(long... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Doubles
        **/
        public _Array(double... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Booleans
        **/
        public _Array(boolean... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of Objects
        **/
        public _Array(JSON._Object... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        *** An Array of other Arrays
        **/
        public _Array(JSON._Array... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** Constructor 
        **/
        /*
        private _Array(Collection list) {
            if (list != null) {
                for (Object val : list) {
                    if (val == null) {
                        this.addValue("");
                    } else
                    if (val instanceof JSON._Value) {
                        this.addValue((JSON._Value)val);
                    } else
                    if (val instanceof JSON._Object) {
                        this.addValue((JSON._Object)val);
                    } else
                    if (val instanceof JSON._Array) {
                        this.addValue((JSON._Array)val);
                    } else
                    if (val instanceof String) {
                        this.addValue((String)val);
                    } else
                    if (val instanceof Long) {
                        this.addValue(((Long)val).longValue());
                    } else
                    if (val instanceof Double) {
                        this.addValue(((Double)val).doubleValue());
                    } else
                    if (val instanceof Boolean) {
                        this.addValue(((Boolean)val).booleanValue());
                    } else {
                        Print.logInfo("Unrecognized data type: " + StringTools.className(val));
                        this.addValue(val.toString());
                    }
                }
            }
        }
        */

        // --------------------------------------

        /**
        *** Add a JSON._Value to this JSON._Array 
        **/
        public boolean add(JSON._Value value) {
            return super.add(value);
        }

        /**
        *** Add a JSON._Value to this JSON._Array 
        **/
        public boolean addValue(JSON._Value value) {
            return this.add(value);
        }

        /**
        *** Add a String to this JSON._Array 
        **/
        public boolean addValue(String value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a Long to this JSON._Array 
        **/
        public boolean addValue(long value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a Double to this JSON._Array 
        **/
        public boolean addValue(double value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a Boolean to this JSON._Array 
        **/
        public boolean addValue(boolean value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a JSON._Object to this JSON._Array 
        **/
        public boolean addValue(JSON._Object value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** Add a JSON._Array to this JSON._Array 
        **/
        public boolean addValue(JSON._Array value) {
            return this.add(new JSON._Value(value));
        }

        // --------------------------------------

        /**
        *** Returns the JSON._Value at the specified index
        **/
        public JSON._Value getValueAt(int ndx) {
            if ((ndx >= 0) && (ndx < this.size())) {
                return this.get(ndx);
            } else {
                return null;
            }
        }

        /**
        *** Returns the JSON._Object value at the specified index
        **/
        public JSON._Object getObjectValueAt(int ndx, JSON._Object dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getObjectValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** Returns the JSON._Array value at the specified index
        **/
        public JSON._Array getArrayValueAt(int ndx, JSON._Array dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getArrayValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** Returns the String value at the specified index
        **/
        public String getStringValueAt(int ndx, String dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getStringValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** Returns the Integer value at the specified index
        **/
        public int getIntValueAt(int ndx, int dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getIntValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** Returns the Long value at the specified index
        **/
        public long getLongValueAt(int ndx, long dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getLongValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** Returns the Double value at the specified index
        **/
        public double getDoubleValueAt(int ndx, double dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getDoubleValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** Returns the Boolean value at the specified index
        **/
        public boolean getBooleanValueAt(int ndx, boolean dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getBooleanValue(dft) : dft;
            } else {
                return dft;
            }
        }

        // --------------------------------------

        /**
        *** Returns a String array of values contained in this JSON Array
        **/
        public String[] getStringArray() {
            String v[] = new String[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getStringValueAt(i,"");
            }
            return v;
        }

        /**
        *** Returns an int array of values contained in this JSON Array
        **/
        public int[] getIntArray() {
            int v[] = new int[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getIntValueAt(i,0);
            }
            return v;
        }

        /**
        *** Returns a long array of values contained in this JSON Array
        **/
        public long[] getLongArray() {
            long v[] = new long[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getLongValueAt(i,0L);
            }
            return v;
        }

        /**
        *** Returns a double array of values contained in this JSON Array
        **/
        public double[] getDoubleArray() {
            double v[] = new double[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getDoubleValueAt(i,0L);
            }
            return v;
        }

        // --------------------------------------

        /**
        *** Gets the number of items in this array
        *** @return The number of items in this array
        **/
        public int size() {
            return super.size();
        }

        /**
        *** Returns true if this array is empty 
        *** @return True if this array is empty
        **/
        public boolean isEmpty() {
            return super.isEmpty();
        }

        // --------------------------------------

        /**
        *** Set format indent state
        **/
        public _Array setFormatIndent(boolean indent) {
            this.formatIndent = indent;
            return this;
        }

        /**
        *** Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            boolean fullFormat = this.formatIndent && (prefix >= 0);
            String pfx0 = fullFormat? JSON.indent(prefix)   : "";
            String pfx1 = fullFormat? JSON.indent(prefix+1) : "";
            sb.append("[");
            if (fullFormat) {
                sb.append("\n");
            }
            int size = this.size();
            for (int i = 0; i < this.size(); i++) {
                JSON._Value v = this.get(i);
                sb.append(pfx1);
                v.toStringBuffer((fullFormat?(prefix+1):-1), sb);
                if ((i + 1) < size) { 
                    sb.append(","); 
                }
                if (fullFormat) {
                    sb.append("\n");
                }
            }
            sb.append(pfx0).append("]");
            return sb;
        }

        /**
        *** Returns a String representation of this instance 
        **/
        public String toString() { // JSON._Array
            return this.toStringBuffer(1,null).toString();
        }

    }

    /**
    *** Parse a JSON Array from the specified String
    **/
    public static JSON._Array parse_Array(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Array(v, new JSONParsingContext());
    }

    /**
    *** Parse JSON Array from the specified String
    **/
    public static JSON._Array parse_Array(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len   = StringTools.length(v);
        JSON._Array  array = null;
        boolean      comp  = false;

        arrayParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '[') {
                if (array == null) {
                    context.incrementIndex();
                    array = new JSON._Array();
                } else {
                    // array within array
                    JSON._Value val = JSON.parse_Value(v, context);
                    if (val == null) {
                        throw new JSONParsingException("Invalid Value", context);
                    }
                    array.add(val);
                }
            } else
            if (ch == ',') {
                // ignore item separators
                // TODO: should insert a placeholder for unspecified values?
                context.incrementIndex();
            } else
            if (ch == ']') {
                // end of array
                context.incrementIndex();
                comp = true;
                break arrayParse;
            } else {
                JSON._Value val = JSON.parse_Value(v, context);
                if (val == null) {
                    throw new JSONParsingException("Invalid Value", context);
                }
                array.add(val);
            }
        }

        /* array completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Array", context);
        }

        /* return array */
        return array;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // String

    /**
    *** Parse a JSON String
    **/
    public static String parse_String(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        String       val  = null;
        boolean      comp = false;

        stringParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '\"') {
                // parse String
                context.incrementIndex(); // consume initial quote
                StringBuffer sb = new StringBuffer();
                quoteParse:
                for (;context.getIndex() < len;) {
                    ch = v.charAt(context.getIndex());
                    if (ch == '\\') {
                        context.incrementIndex(); // skip '\'
                        if (context.getIndex() >= len) {
                            throw new JSONParsingException("Overflow", context);
                        }
                        ch = v.charAt(context.getIndex());
                        context.incrementIndex(); // skip char
                        switch (ch) {
                            case '"' : sb.append('"' ); break;
                            case '\\': sb.append('\\'); break;
                            case '/' : sb.append('/' ); break;
                            case 'b' : sb.append('\b'); break;
                            case 'f' : sb.append('\f'); break;
                            case 'n' : sb.append('\n'); break;
                            case 'r' : sb.append('\r'); break;
                            case 't' : sb.append('\t'); break;
                            case 'u' : {
                                int ndx = context.getIndex();
                                if ((ndx + 4) >= len) {
                                    throw new JSONParsingException("Overflow", context);
                                }
                                String hex = v.substring(ndx,ndx+4);
                                context.incrementIndex(4);
                                break;
                            }
                            default  : sb.append(ch); break;
                        }
                    } else
                    if (ch == '\"') {
                        context.incrementIndex();  // consume final quote
                        comp = true;
                        break quoteParse; // we're done
                    } else {
                        sb.append(ch);
                        context.incrementIndex();
                        if (context.getIndex() >= len) {
                            throw new JSONParsingException("Overflow", context);
                        }
                    }
                } // quoteParse
                val = sb.toString();
                break stringParse;
            } else {
                throw new JSONParsingException("Missing initial String quote", context);
            }
        } // stringParse

        /* String completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete String", context);
        }

        /* return String */
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Number

    /**
    *** Parse a JSON Number
    **/
    public static Number parse_Number(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        Number       val  = null;
        boolean      comp = false;

        numberParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                StringBuffer num = new StringBuffer();
                num.append(ch);
                context.incrementIndex();
                int intDig = Character.isDigit(ch)? 1 : 0;
                int frcDig = 0;
                int expDig = 0;
                boolean frcCh = false; // '.'
                boolean esnCh = false; // '+'/'-'
                boolean expCh = false; // 'e'/'E'
                digitParse:
                for (;context.getIndex() < len;) {
                    char d = v.charAt(context.getIndex());
                    if (Character.isDigit(d)) {
                        if (expCh) {
                            expDig++;
                        } else
                        if (frcCh) {
                            frcDig++;
                        } else {
                            intDig++;
                        }
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if (d == '.') {
                        if (frcCh) {
                            // more than one '.'
                            throw new JSONParsingException("Invalid numeric value (multiple '.')", context);
                        } else
                        if (intDig == 0) {
                            // no digits before decimal
                            throw new JSONParsingException("Invalid numeric value (no digits before '.')", context);
                        }
                        frcCh = true;
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if ((d == 'e') || (d == 'E')) {
                        if (frcCh && (frcDig == 0)) {
                            // no digits after decimal
                            throw new JSONParsingException("Invalid numeric value (no digits after '.')", context);
                        } else
                        if (expCh) {
                            // more than one 'E'
                            throw new JSONParsingException("Invalid numeric value (multiple 'E')", context);
                        }
                        expCh = true;
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if ((d == '-') || (d == '+')) {
                        if (!expCh) {
                            // no 'E'
                            throw new JSONParsingException("Invalid numeric value (no 'E')", context);
                        } else
                        if (esnCh) {
                            // more than one '-/+'
                            throw new JSONParsingException("Invalid numeric value (more than one '+/-')", context);
                        }
                        esnCh = true;
                        num.append(d);
                        context.incrementIndex();
                    } else {
                        comp = true;
                        break digitParse; // first non-numeric character
                    }
                } // digitParse
                if (context.getIndex() >= len) {
                    throw new JSONParsingException("Overflow", context);
                }
                String numStr = num.toString();
                if (frcCh || expCh) {
                    val = (Number)(new Double(StringTools.parseDouble(numStr,0.0)));
                } else {
                    val = (Number)(new Long(StringTools.parseLong(numStr,0L)));
                }
                break numberParse;
            } else {
                throw new JSONParsingException("Missing initial Numeric +/-/0", context);
            }
        } // numberParse

        /* number completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Number", context);
        }

        /* return number */
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private JSON._Object object = null;

    /**
    *** Constructor 
    **/
    public JSON()
    {
        super();
    }

    /**
    *** Constructor 
    **/
    public JSON(JSON._Object obj)
    {
        this.object = obj;
    }

    /**
    *** Constructor 
    **/
    public JSON(String json)
        throws JSONParsingException 
    {
        this.object = JSON.parse_Object(json);
    }

    /**
    *** Constructor 
    **/
    public JSON(InputStream input)
        throws JSONParsingException, IOException
    {
        String json = StringTools.toStringValue(FileTools.readStream(input));
        this.object = JSON.parse_Object(json);
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns true if an object is defined
    **/
    public boolean hasObject()
    {
        return (this.object != null);
    }

    /** 
    *** Gets the main JSON._Object
    **/
    public JSON._Object getObject()
    {
        return this.object;
    }

    /** 
    *** Sets the main JSON._Object
    **/
    public void setObject(JSON._Object obj)
    {
        this.object = obj;
    }

    // ------------------------------------------------------------------------

    /**
    *** Return a String representation of this instance
    **/
    public String toString()  // JSON
    {
        if (this.object != null) {
            return this.object.toString();
        } else {
            return "";
        }
    }

    /**
    *** Return a String representation of this instance
    **/
    public String toString(boolean inclPrefix)  // JSON
    {
        if (this.object != null) {
            return this.object.toString(inclPrefix);
        } else {
            return "";
        }
    }

    /**
    *** Print object contents (debug purposes only)
    **/
    public void debugDisplayObject()
    {
        if (this.object != null) {
            this.object.debugDisplayObject(0);
        } else {
            Print.sysPrintln("n/a");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
