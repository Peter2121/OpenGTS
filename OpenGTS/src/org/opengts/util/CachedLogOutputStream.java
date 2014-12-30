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
//  Rotating Buffer OutputStream
// ----------------------------------------------------------------------------
// Change History:
//  2012/12/24  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;

/**
*** Cached Logging text OutputStream
**/

public class CachedLogOutputStream
    extends ByteArrayOutputStream
{

    public  static final int    DEFAULT_MINIMUM_LENGTH  = 200 * 80; // 16000
    public  static final int    DEFAULT_MAXIMUM_LENGTH  = 300 * 80; // 24000

    private static final byte   NEWLINE[]               = { '\n' };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int     minSize     = DEFAULT_MINIMUM_LENGTH;
    private int     maxSize     = DEFAULT_MAXIMUM_LENGTH;
    private long    discardID   = 0L;

    public CachedLogOutputStream()
    {
        this(DEFAULT_MINIMUM_LENGTH, DEFAULT_MAXIMUM_LENGTH);
    }

    public CachedLogOutputStream(int minSize, int maxSize)
    {
        super(maxSize);
        this.maxSize = maxSize;
        if ((minSize <= 0) || (minSize > maxSize)) {
            this.minSize = this.maxSize / 2;
        } else {
            this.minSize = minSize;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Clears/reset the logging text in this cache.
    **/
    public synchronized void reset() 
    {
        super.reset();
        this.discardID = 0L;
    }

    /**
    *** Gets the current size of the buffer
    *** @return The current size of the buffer
    **/
    public synchronized int size()
    {
        return super.size();
    }

    /**
    *** Returns the discard-ID.  The discard-ID is incremented each time any
    *** logging text is discarded from the cache.  During the retrieval of
    *** logging data (see <code>readLine</code>), if this discard-ID changes,
    *** it means that there was some logging data lost due to the buffer 
    *** having been filled, and old logging data discarded.
    **/
    public synchronized long getDiscardID()
    {
        return this.discardID;
    }

    // ------------------------------------------------------------------------

    private static boolean IsNL(byte c)
    {
        return ((c == '\n') || (c == '\r'));
    }

    /**
    *** Writes the specified byte to this byte array output stream. 
    *** @param b the byte to be written.
    **/
    public synchronized void write(int b) 
    {
        int len = 1;
        int ofs = 0;

        /* adjust buf */
        int newcount = super.count + len;
        if (newcount > super.buf.length) { // this.maxSize
            // discard oldest log text
            if (len >= this.minSize) {
                super.count = 0;
                ofs = len - this.minSize;
                len = this.minSize;
            } else {
                int bufLen = this.minSize - len;
                int bufPos = super.count - bufLen;
                while ((bufLen > 0) && !IsNL(super.buf[bufPos])) { bufLen--; bufPos++; }
                while ((bufLen > 0) &&  IsNL(super.buf[bufPos])) { bufLen--; bufPos++; } // all NLs
                System.arraycopy(super.buf, bufPos, super.buf, 0, bufLen);
                super.count = bufLen;
            }
            newcount = super.count + len;
            this.discardID++;
        }

        /* copy */
        super.buf[super.count] = (byte)b;
        super.count = newcount;
        this.notifyAll();

    }

    /**
    *** Writes <code>len</code> bytes from the specified byte array 
    *** starting at offset <code>off</code> to this byte array output stream.
    *** @param b   the data.
    *** @param ofs the start offset in the data.
    *** @param len the number of bytes to write.
    **/
    public synchronized void write(byte b[], int ofs, int len) 
    {

        /* validate buffer offset/length */
        if ((b == null) || (len == 0)) {
            return; // nothing to copy
        } else 
        if ((ofs < 0) || (ofs > b.length) || (len < 0) ||
            ((ofs + len) > b.length) || ((ofs + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        /* adjust buf */
        int newcount = super.count + len;
        if (newcount > super.buf.length) { // this.maxSize
            // discard oldest log text
            if (len >= this.minSize) {
                super.count = 0;
                ofs = len - this.minSize;
                len = this.minSize;
            } else {
                int bufLen = this.minSize - len;
                int bufPos = super.count - bufLen;
                while ((bufLen > 0) && !IsNL(super.buf[bufPos])) { bufLen--; bufPos++; }
                while ((bufLen > 0) &&  IsNL(super.buf[bufPos])) { bufLen--; bufPos++; } // all NLs
                System.arraycopy(super.buf, bufPos, super.buf, 0, bufLen);
                super.count = bufLen;
            }
            newcount = super.count + len;
            this.discardID++;
        }

        /* copy */
        System.arraycopy(b, ofs, super.buf, super.count, len);
        super.count = newcount;
        this.notifyAll();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Writes the specified String to this byte array output stream. 
    *** @param s the String to be written.
    **/
    public synchronized void write(String s)
    {
        if ((s != null) && (s.length() > 0)) {
            byte b[] = s.getBytes();
            this.write(b, 0, b.length);
        }
    }

    /**
    *** Writes the specified String to this byte array output stream. 
    *** @param s the String to be written.
    **/
    public synchronized void writeln(String s)
    {
        this.write(s); // synchronized
        this.write(NEWLINE,0,NEWLINE.length); // synchronized
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Reads, and optionally removes, a line of text from the byte array.
    *** @param removeLine True to remove the line which has been read.
    *** @return The line of text read, or null if there is no text in the byte-array
    **/
    protected synchronized String _readLine(boolean removeLine)
    {
        int p = 0;
        while ((p < super.count) && !IsNL(super.buf[p])) { p++; }
        if    ((p < super.count) &&  IsNL(super.buf[p])) { p++; } // at-most 1 NL
        if (p == 0) {
            return null;
        } else {
            int ofs = 0;
            String s = new String(super.buf, ofs, p);
            if (removeLine) {
                int newcount = super.count - p;
                System.arraycopy(super.buf, p, super.buf, 0, newcount);
                super.count = newcount;
            }
            return s;
        }
    }

    /**
    *** Reads and removes a line of text from the byte array.
    *** @return The line of text read, or null if there is no text in the byte-array
    **/
    public String readLine()
    {
        return this._readLine(true);
    }

    /**
    *** Reads a line of text from the byte array (line is not removed)
    *** @return The line of text read, or null if there is no text in the byte-array
    **/
    public String peekLine()
    {
        return this._readLine(false);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
