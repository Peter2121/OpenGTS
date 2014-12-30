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
//  HTML Tools
// ----------------------------------------------------------------------------
// Change History:
//  2013/03/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

/**
*** Cascading Style Sheet "style" wrapper
**/

public class CssStyle
{

    // ------------------------------------------------------------------------

    public static final String TEXT_DECORATION_UNDERLINE    = "underline";
    public static final String TEXT_DECORATION_OVERLINE     = "overline";
    public static final String TEXT_DECORATION_BLINK        = "blink";
   
    public static final String FONT_WEIGHT_BOLD             = "bold";
    
    public static final String FONT_STYLE_ITALIC            = "italic";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* style */
    private String foreground       = null;
    private String background       = null;
    private String fontWeight       = null;
    private String fontStyle        = null;
    private String textDecoration   = null;

    public CssStyle()
    {
        super();
        this.clear();
    }

    public CssStyle(CssStyle other)
    {
        this();
        if (other != null) {
            this.foreground     = other.foreground;
            this.background     = other.background;
            this.fontWeight     = other.fontWeight;
            this.fontStyle      = other.fontStyle;
            this.textDecoration = other.textDecoration;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Clears all state
    **/
    public void clear()
    {
        this.foreground     = null;
        this.background     = null;
        this.fontWeight     = null;
        this.fontStyle      = null;
        this.textDecoration = null;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the text foregound color
    *** @param rgb  The ColorTools.RGB value of the text foreground color
    *** @return This CssStyle instance
    **/
    public CssStyle setForegroundColor(ColorTools.RGB rgb)
    {
        if (rgb != null) {
            this.setForegroundColor(rgb.toString(true));
        } else {
            this.setForegroundColor((String)null);
        }
        return this;
    }
    
    /**
    *** Sets the text foregound color
    *** @param color  The String representation of the text foreground color
    *** @return This CssStyle instance
    **/
    public CssStyle setForegroundColor(String color)
    {
        this.foreground = color;
        //if (!StringTools.isBlank(this.foreground) && !this.foreground.startsWith("#")) {
        //    // TODO: check to see if we need to prepend a "#"
        //}
        return this;
    }

    /**
    *** Returns true if the foreground color attribute has been defined
    *** @return True if the foreground color attribute has been defined
    **/
    public boolean hasForegroundColor()
    {
        return !StringTools.isBlank(this.foreground);
    }
    
    /**
    *** Gets the text foreground color
    *** @return The text foreground color
    **/
    public String getForegroundColor()
    {
        return this.hasForegroundColor()? this.foreground : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the text background color
    *** @param color  The String representation of the text background color
    *** @return This CssStyle instance
    **/
    public CssStyle setBackgroundColor(String color)
    {
        this.background = color;
        return this;
    }

    /**
    *** Returns true if the background color attribute has been defined
    *** @return True if the background color attribute has been defined
    **/
    public boolean hasBackgroundColor()
    {
        return !StringTools.isBlank(this.background);
    }

    /**
    *** Gets the text background color
    *** @return The text background color
    **/
    public String getBackgroundColor()
    {
        return this.hasBackgroundColor()? this.background : null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the text decoration style ("text-decoration:")
    *** @param decor text decoraton style (ie. "underline", "overline", "line-through", "blink")
    *** @return This CssStyle instance
    **/
    public CssStyle setTextDecoration(String decor)
    {
        this.textDecoration = decor;
        return this;
    }
    
    /**
    *** Sets the text decoration style to "underline"
    *** @return This CssStyle instance
    **/
    public CssStyle setTextDecorationUnderline()
    {
        return this.setTextDecoration(TEXT_DECORATION_UNDERLINE);
    }
    
    /**
    *** Sets the text decoration style to "overline"
    *** @return This CssStyle instance
    **/
    public CssStyle setTextDecorationOverline()
    {
        return this.setTextDecoration(TEXT_DECORATION_OVERLINE);
    }
    
    /**
    *** Sets the text decoration style to "blink"
    *** @return This CssStyle instance
    **/
    public CssStyle setTextDecorationBlink()
    {
        return this.setTextDecoration(TEXT_DECORATION_BLINK);
    }

    /**
    *** Returns true if the text decoration attribute has been defined
    *** @return True if the text decoration attribute has been defined
    **/
    public boolean hasTextDecoration()
    {
        return !StringTools.isBlank(this.textDecoration);
    }
    
    /**
    *** Gets the text decoration
    *** @return The text decoration
    **/
    public String getTextDecoration()
    {
        return this.hasTextDecoration()? this.textDecoration : null;
    }

    // ------------------------------------------------------------------------
 
    /**
    *** Sets the text font weight ("font-weight:")
    *** @param fw The text font weight (ie. "bold", "normal", "100".."900")
    *** @return This CssStyle instance
    **/
    public CssStyle setFontWeight(String fw)
    {
        this.fontWeight = fw;
        return this;
    }
    
    /**
    *** Sets the text font weight to "bold"
    *** @return This CssStyle instance
    **/
    public CssStyle setFontWeightBold()
    {
        return this.setFontWeight(FONT_WEIGHT_BOLD);
    }

    /**
    *** Returns true if the text font weight attribute has been defined
    *** @return True if the text font weight attribute has been defined
    **/
    public boolean hasFontWeight()
    {
        return !StringTools.isBlank(this.fontWeight);
    }

    /**
    *** Gets the text font weight
    *** @return The text font weight
    **/
    public String getFontWeight()
    {
        return this.hasFontWeight()? this.fontWeight : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the text font style ("font-style:")
    *** @param fs The text font style (ie. "normal", "italic", "oblique")
    *** @return This CssStyle instance
    **/
    public CssStyle setFontStyle(String fs)
    {
        this.fontStyle = fs;
        return this;
    }
    
    /**
    *** Sets the text font style to "italic"
    *** @return This CssStyle instance
    **/
    public CssStyle setFontStyleItalic()
    {
        return this.setFontStyle(FONT_STYLE_ITALIC);
    }

    /**
    *** Returns true if the text font style attribute has been defined
    *** @return True if the text font style attribute has been defined
    **/
    public boolean hasFontStyle()
    {
        return !StringTools.isBlank(this.fontStyle);
    }

    /**
    *** Gets the text font style
    *** @return The text font style
    **/
    public String getFontStyle()
    {
        return this.hasFontStyle()? this.fontStyle : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this CssStyle contains style information
    *** @return True if this CssStyle contains style information
    **/
    public boolean hasStyle()
    {
        return
            this.hasForegroundColor() ||
            this.hasBackgroundColor() ||
            this.hasTextDecoration()  ||
            this.hasFontWeight()      ||
            this.hasFontStyle();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "style" value<br>
    *** Note: the prefixing <code>style=</code> and quotes are not included.
    *** @return The the "style" value
    **/
    public String getStyleString()
    {

        /* return blank if no style */
        if (!this.hasStyle()) {
            return "";
        }

        /* create style */
        StringBuffer sb = new StringBuffer();
        if (this.hasForegroundColor()) {
            sb.append("color:").append(this.getForegroundColor()).append(";");
        }
        if (this.hasBackgroundColor()) {
            sb.append("background-color:").append(this.getBackgroundColor()).append(";");
        }
        if (this.hasTextDecoration()) {
            sb.append("text-decoration:").append(this.getTextDecoration()).append(";");
        }
        if (this.hasFontStyle()) {
            sb.append("font-style:").append(this.getFontStyle()).append(";");
        }
        if (this.hasFontWeight()) {
            sb.append("font-weight:").append(this.getFontWeight()).append(";");
        }

        /* return style */
        return sb.toString();

    }
    
    /**
    *** Returns the "style" String representation of this instance
    *** @return The "style" String representation of this instance
    **/
    public String toString()
    {
        return this.getStyleString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a <code>span</code> wrapped HTML text
    *** @return <code>span</code> wrapped HTML text
    **/
    protected String _wrapText_TAG(String TAG, String text)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<").append(TAG).append(" style=\"").append(this.getStyleString()).append("\">");
        sb.append(StringTools.htmlFilterText(StringTools.trim(text)));
        sb.append("</").append(TAG).append(">");
        return sb.toString();
    }

    /**
    *** Returns a <code>span</code> wrapped HTML text
    *** @return <code>span</code> wrapped HTML text
    **/
    public String wrapText_span(String text)
    {
        return this._wrapText_TAG("span", text);
    }

    /**
    *** Returns a <code>div</code> wrapped HTML text
    *** @return <code>div</code> wrapped HTML text
    **/
    public String wrapText_div(String text)
    {
        return this._wrapText_TAG("div", text);
    }

    // ------------------------------------------------------------------------

}
