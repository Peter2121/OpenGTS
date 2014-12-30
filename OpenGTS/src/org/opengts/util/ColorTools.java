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
//  2006/02/19  Martin D. Flynn
//      - Initial release
//  2006/04/17  Martin D. Flynn
//      - Add additional keywords to "parseColor"
//  2008/12/01  Martin D. Flynn
//      - Added 'RGB' class
//  2010/01/29  Martin D. Flynn
//      - Added 'isColor' method
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.color.*;

/**
*** Color handling and conversion tools
**/

public class ColorTools
{

    // ------------------------------------------------------------------------
    // http://htmlhelp.com/cgi-bin/color.cgi?rgb=FFFFFF
    
    public static ColorTools.RGB BLACK              = new ColorTools.RGB(0x000000);
    public static ColorTools.RGB BROWN              = new ColorTools.RGB(0xA52A2A);
    public static ColorTools.RGB RED                = new ColorTools.RGB(0xDD0000);
    public static ColorTools.RGB ORANGE             = new ColorTools.RGB(0xFFA500);
    public static ColorTools.RGB YELLOW             = new ColorTools.RGB(0xFFD700);
    public static ColorTools.RGB DARK_YELLOW        = new ColorTools.RGB(0xAA9700);
    public static ColorTools.RGB GREEN              = new ColorTools.RGB(0x00CC00);
    public static ColorTools.RGB BLUE               = new ColorTools.RGB(0x0000EE);
    public static ColorTools.RGB PURPLE             = new ColorTools.RGB(0x9400D3);
    public static ColorTools.RGB GRAY               = new ColorTools.RGB(0x808080);
    public static ColorTools.RGB DARK_GRAY          = new ColorTools.RGB(0x505050);
    public static ColorTools.RGB LIGHT_GRAY         = new ColorTools.RGB(0xC0C0C0);
    public static ColorTools.RGB WHITE              = new ColorTools.RGB(0xFFFFFF);
    public static ColorTools.RGB CYAN               = new ColorTools.RGB(0x00FFFF);
    public static ColorTools.RGB PINK               = new ColorTools.RGB(0xFF1493);
    public static ColorTools.RGB MAGENTA            = new ColorTools.RGB(0x8B008B);

    public static ColorTools.RGB COLOR_BLACK        = BLACK;
    public static ColorTools.RGB COLOR_BROWN        = BROWN;
    public static ColorTools.RGB COLOR_RED          = RED;
    public static ColorTools.RGB COLOR_ORANGE       = ORANGE;
    public static ColorTools.RGB COLOR_YELLOW       = YELLOW;
    public static ColorTools.RGB COLOR_GREEN        = GREEN;
    public static ColorTools.RGB COLOR_BLUE         = BLUE;
    public static ColorTools.RGB COLOR_PURPLE       = PURPLE;
    public static ColorTools.RGB COLOR_GRAY         = GRAY;
    public static ColorTools.RGB COLOR_WHITE        = WHITE;

    // ------------------------------------------------------------------------

    /**
    *** RGB class.
    **/
    public static class RGB
    {
        private int R   = 0;
        private int G   = 0;
        private int B   = 0;
        private int A   = -1;
        
        /* 0.255 int constructor */
        public RGB(int R, int G, int B) {
            this.R = R & 0xFF;
            this.G = G & 0xFF;
            this.B = B & 0xFF;
        }

        /* 0.255 int constructor */
        public RGB(int RGB[]) {
            if ((RGB != null) && (RGB.length >= 3)) {
                this.R = RGB[0];
                this.G = RGB[1];
                this.B = RGB[2];
            }
        }

        /* hex constructor */
        public RGB(int RGB) {
            this.R = (RGB >> 16) & 0xFF;
            this.G = (RGB >>  8) & 0xFF;
            this.B = (RGB >>  0) & 0xFF;
        }
        
        /* 0..1 float constructor */
        public RGB(float R, float G, float B) {
            this.R = (int)Math.round((double)R * 256.0);
            this.G = (int)Math.round((double)G * 256.0);
            this.B = (int)Math.round((double)B * 256.0);
        }

        /* 0..1 float constructor */
        public RGB(float RGB[]) {
            if ((RGB != null) && (RGB.length >= 3)) {
                this.R = (int)Math.round((double)RGB[0] * 256.0);
                this.G = (int)Math.round((double)RGB[1] * 256.0);
                this.B = (int)Math.round((double)RGB[2] * 256.0);
            }
        }
        
        /* 0..1 double constructor */
        public RGB(double R, double G, double B) {
            this.R = (int)Math.round(R * 256.0);
            this.G = (int)Math.round(G * 256.0);
            this.B = (int)Math.round(B * 256.0);
        }

        /* 0..1 double constructor */
        public RGB(double RGB[]) {
            if ((RGB != null) && (RGB.length >= 3)) {
                this.R = (int)Math.round(RGB[0] * 256.0);
                this.G = (int)Math.round(RGB[1] * 256.0);
                this.B = (int)Math.round(RGB[2] * 256.0);
            }
        }

        /* return hex int value */
        public int getRGB() {
            return (this.R << 16) | (this.G << 8) | (this.B << 0);
        }
        
        /* return float components */
        public float[] getRGBColorComponents() {
            return new float[] { 
                _toFloat(this.R), 
                _toFloat(this.G), 
                _toFloat(this.B)
            };
        }
        
        /* return lighter RGB color */
        public RGB lighter(double percent) {
            float p = _bound((float)percent);
            float C[] = this.getRGBColorComponents();
            for (int i = 0; i < C.length; i++) {
                C[i] = _bound(C[i] + ((1.0F - C[i]) * p));
            }
            return new RGB(C);
        }

        /* return darker RGB color */
        public RGB darker(double percent) {
            float p = _bound((float)percent);
            float C[] = this.getRGBColorComponents();
            for (int i = 0; i < C.length; i++) {
                C[i] = _bound(C[i] - (C[i] * p));
            }
            return new RGB(C);
        }

        /* return mixed RGB color */
        public RGB mixWith(RGB color) {
            if (color == null) {
                return this;
            } else {
                float rgb1[] = this.getRGBColorComponents();
                float rgb2[] = color.getRGBColorComponents();
                float rgb[]  = new float[3];
                for (int i = 0; i < 3; i++) {
                    rgb[i] = (rgb1[i] + rgb2[i]) / 2.0F;
                }
                return new RGB(rgb[0], rgb[1], rgb[2]);
            }
        }
        
        /* return mixed RGB color */
        public RGB mixWith(RGB color, float weight) {
            if (color == null) {
                return this;
            } else {
                float rgb1[] = this.getRGBColorComponents();
                float rgb2[] = color.getRGBColorComponents();
                float rgb[]  = new float[3];
                for (int i = 0; i < 3; i++) {
                    rgb[i] = _bound(rgb1[i] + ((rgb2[i] - rgb1[i]) * weight));
    
                }
                return new RGB(rgb[0], rgb[1], rgb[2]);
            }
        }
        
        /* return mixed RGB color */
        public RGB mixWith(RGB color, double weight) {
            return this.mixWith(color, (float)weight);
        }

        /* return hex string representation of this RGB color */
        public String toString() {
            return this.toString(false);
        }
        
        /* return hex string representation of this RGB color */
        public String toString(boolean inclHash) {
            return ColorTools.toHexString(this.getRGB(), inclHash);
        }
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses the Color String representation into a Color instance.
    *** @param color  The color String representation to parse
    *** @param dft    The default Color returned if unable to parse a Color from the String
    *** @return The parsed Color instance.
    **/
    public static RGB parseColor(String color, RGB dft)
    {

        /* default */
        if (StringTools.isBlank(color)) {
            // -- return default as-is
            return dft;
        }

        /* check for +/- percent */
        double percent = 0.0;
        int p = color.indexOf("-");
        if (p >= 0) {
            // -- lighten (ie. "red-30")
            percent = -StringTools.parseDouble(color.substring(p+1),0.0) / 100.0;
            color = color.substring(0,p);
        } else {
            p = color.indexOf("+");
            if (p >= 0) {
                // -- darken (ie. "white+25")
                percent = StringTools.parseDouble(color.substring(p+1),0.0) / 100.0;
                color = color.substring(0,p);
            }
        }

        /* get color */
        RGB rtn;
        if (StringTools.isBlank(color)) {
            rtn = dft;
        } else
        if (color.equalsIgnoreCase("black")) {
            rtn = BLACK;
        } else
        if (color.equalsIgnoreCase("blue")) {
            rtn = BLUE;
        } else
        if (color.equalsIgnoreCase("cyan")) {
            rtn = CYAN;
        } else
        if (color.equalsIgnoreCase("darkGray")) {
            rtn = DARK_GRAY;
        } else
        if (color.equalsIgnoreCase("gray")) {
            rtn = GRAY;
        } else
        if (color.equalsIgnoreCase("green")) {
            rtn = GREEN;
        } else
        if (color.equalsIgnoreCase("lightGray")) {
            rtn = LIGHT_GRAY;
        } else
        if (color.equalsIgnoreCase("magenta")) {
            rtn = MAGENTA;
        } else
        if (color.equalsIgnoreCase("orange")) {
            rtn = ORANGE;
        } else
        if (color.equalsIgnoreCase("pink")) {
            rtn = PINK;
        } else
        if (color.equalsIgnoreCase("red")) {
            rtn = RED;
        } else
        if (color.equalsIgnoreCase("white")) {
            rtn = WHITE;
        } else
        if (color.equalsIgnoreCase("yellow")) {
            rtn = YELLOW;
        } else {
            String c = color.startsWith("#")? color.substring(1) : color;
            byte rgb[] = StringTools.parseHex(c, null);
            if (rgb != null) {
                int R = (rgb.length > 0)? ((int)rgb[0] & 0xFF) : 0;
                int G = (rgb.length > 1)? ((int)rgb[1] & 0xFF) : 0;
                int B = (rgb.length > 2)? ((int)rgb[2] & 0xFF) : 0;
                rtn = new RGB(R,G,B);
            } else {
                rtn = dft;
            }
        }

        /* lighten/darken */
        if (rtn != null) {
            if (percent == 0.0) {
                // -- leave as-is
            } else
            if (percent > 0.0) {
                // -- darken
                rtn = rtn.darker(percent);
            } else {
                // -- lighter
                rtn = rtn.lighter(percent);
            }
        }

        /* return resulting color */
        return rtn;

    }

    /**
    *** Returns true if the specified string is a valid color
    *** @param color  The color String representation to test
    *** @return True if the specified string is a valid color
    **/
    public static boolean isColor(String color)
    {
        return (ColorTools.parseColor(color,(Color)null) != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates a new Color that is lighter than the specified Color
    *** @param c       The Color to make lighter
    *** @param percent The percent to make lighter
    *** @return The new 'ligher' Color
    **/
    public static Color lighter(Color c, double percent)
    {
        return ColorTools.lighter(c, (float)percent);
    }

    /**
    *** Creates a new Color that is lighter than the specified Color
    *** @param c       The Color to make lighter
    *** @param percent The percent to make lighter
    *** @return The new 'ligher' Color
    **/
    public static Color lighter(Color c, float percent)
    {
        float p = _bound(percent);
        float comp[] = c.getColorComponents(null);
        for (int i = 0; i < comp.length; i++) {
            comp[i] = _bound(comp[i] + ((1.0F - comp[i]) * p));
        }
        ColorSpace cs = c.getColorSpace();
        return new Color(cs, comp, _toFloat(c.getAlpha()));
    }

    /**
    *** Creates a new Color that is darker than the specified Color
    *** @param c       The Color to make darker
    *** @param percent The percent to make darker
    *** @return The new 'darker' Color
    **/
    public static Color darker(Color c, double percent)
    {
        return ColorTools.darker(c, (float)percent);
    }

    /**
    *** Creates a new Color that is darker than the specified Color
    *** @param c       The Color to make darker
    *** @param percent The percent to make darker
    *** @return The new 'darker' Color
    **/
    public static Color darker(Color c, float percent)
    {
        float p = _bound(percent);
        float comp[] = c.getColorComponents(null);
        for (int i = 0; i < comp.length; i++) {
            comp[i] = _bound(comp[i] - (comp[i] * p));
        }
        return new Color(c.getColorSpace(), comp, _toFloat(c.getAlpha()));
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new Color that is the average of the 2 specified Colors, based
    *** on a 50% weighting from the first Color to the second Color.
    *** @param color1  The first Color which will be averaged
    *** @param color2  The second Color which will be averaged
    *** @return The new 'averaged' Color
    **/
    public static Color mix(Color color1, Color color2)
    {
        if (color1 == null) {
            return color2;
        } else
        if (color2 == null) {
            return color1;
        } else {
            float rgb1[] = color1.getRGBColorComponents(null);
            float rgb2[] = color2.getRGBColorComponents(null);
            float rgb[]  = new float[3];
            for (int i = 0; i < 3; i++) {
                rgb[i] = (rgb1[i] + rgb2[i]) / 2.0F;
            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        }
    }

    /**
    *** Creates a new Color that is a mix of the 2 specified Colors, based on
    *** the specified 'weighting' from the first Color to the second Color.
    *** @param color1  The first Color which will be averaged
    *** @param color2  The second Color which will be averaged
    *** @param weight  The 'weighting' from the first Color to the second Color.
    ***                A 'weight' of '0.0' will produce the first Color.  
    ***                A 'weight' of '1.0' will produce the second Color.
    *** @return The new 'averaged' Color
    **/
    public static Color mix(Color color1, Color color2, float weight)
    {
        if (color1 == null) {
            return color2;
        } else
        if (color2 == null) {
            return color1;
        } else {
            float rgb1[] = color1.getRGBColorComponents(null);
            float rgb2[] = color2.getRGBColorComponents(null);
            float rgb[]  = new float[3];
            for (int i = 0; i < 3; i++) {
                rgb[i] = _bound(rgb1[i] + ((rgb2[i] - rgb1[i]) * weight));

            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        }
    }

    /**
    *** Creates a new Color that is a mix of the 2 specified Colors, based on
    *** the specified 'weighting' from the first Color to the second Color.
    *** @param color1  The first Color which will be averaged
    *** @param color2  The second Color which will be averaged
    *** @param weight  The 'weighting' from the first Color to the second Color.
    ***                A 'weight' of '0.0' will produce the first Color.  
    ***                A 'weight' of '1.0' will produce the second Color.
    *** @return The new 'averaged' Color
    **/
    public static Color mix(Color color1, Color color2, double weight)
    {
        return ColorTools.mix(color1, color2, (float)weight);
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses the Color String representation into a Color instance.
    *** @param color  The color String representation to parse
    *** @param dft    The default Color returned if unable to parse a Color from the String
    *** @return The parsed Color instance.
    **/
    public static Color parseColor(String color, Color dft)
    {

        /* default */
        if (StringTools.isBlank(color)) {
            // -- return default as-is
            return dft;
        }

        /* check for +/- percent */
        double percent = 0.0;
        int p = color.indexOf("-");
        if (p >= 0) {
            // -- lighten (ie. "red-30")
            percent = -StringTools.parseDouble(color.substring(p+1),0.0) / 100.0;
            color = color.substring(0,p);
        } else {
            p = color.indexOf("+");
            if (p >= 0) {
                // -- darken (ie. "white+25")
                percent = StringTools.parseDouble(color.substring(p+1),0.0) / 100.0;
                color = color.substring(0,p);
            }
        }

        /* get color */
        Color rtn;
        if (StringTools.isBlank(color)) {
            rtn = dft;
        } else
        if (color.equalsIgnoreCase("black")) {
            rtn = Color.black;
        } else
        if (color.equalsIgnoreCase("blue")) {
            rtn = Color.blue;
        } else
        if (color.equalsIgnoreCase("cyan")) {
            rtn = Color.cyan;
        } else
        if (color.equalsIgnoreCase("darkGray")) {
            rtn = Color.darkGray;
        } else
        if (color.equalsIgnoreCase("gray")) {
            rtn = Color.gray;
        } else
        if (color.equalsIgnoreCase("green")) {
            rtn = Color.green;
        } else
        if (color.equalsIgnoreCase("lightGray")) {
            rtn = Color.lightGray;
        } else
        if (color.equalsIgnoreCase("magenta")) {
            rtn = Color.magenta;
        } else
        if (color.equalsIgnoreCase("orange")) {
            rtn = Color.orange;
        } else
        if (color.equalsIgnoreCase("pink")) {
            rtn = Color.pink;
        } else
        if (color.equalsIgnoreCase("red")) {
            rtn = Color.red;
        } else
        if (color.equalsIgnoreCase("white")) {
            rtn = Color.white;
        } else
        if (color.equalsIgnoreCase("yellow")) {
            rtn = Color.yellow;
        } else {
            String c = color.startsWith("#")? color.substring(1) : color;
            byte rgb[] = StringTools.parseHex(c, null);
            if (rgb != null) {
                int r = (rgb.length > 0)? ((int)rgb[0] & 0xFF) : 0;
                int g = (rgb.length > 1)? ((int)rgb[1] & 0xFF) : 0;
                int b = (rgb.length > 2)? ((int)rgb[2] & 0xFF) : 0;
                int a = (rgb.length > 3)? ((int)rgb[3] & 0xFF) : 0;
                rtn = (rgb.length > 3)? new Color(r,g,b,a) : new Color(r,g,b);
            } else {
                rtn = dft;
            }
        }

        /* lighten/darken */
        if (rtn != null) {
            if (percent == 0.0) {
                // -- leave as-is
            } else
            if (percent > 0.0) {
                // -- darken
                rtn = ColorTools.darker(rtn, percent);
            } else {
                // -- lighter
                rtn = ColorTools.lighter(rtn, -percent);
            }
        }

        /* return resulting color */
        return rtn;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The Color to convert to a String representation
    *** @return The Color String representation
    **/
    public static String toHexString(Color color)
    {
        return ColorTools.toHexString(color, true);
    }

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The Color to convert to a String representation
    *** @param inclHash Prefix with '#' if true
    *** @return The Color String representation
    **/
    public static String toHexString(Color color, boolean inclHash)
    {
        if (color != null) {
            return ColorTools.toHexString(color.getRGB(), inclHash);
        } else {
            return "";
        }
    }

    /**
    *** Returns a hex String representation of the specified Color.
    *** @param color  The RGB value to convert to a String representation
    *** @param inclHash Prefix with '#' if true
    *** @return The color String representation
    **/
    public static String toHexString(int color, boolean inclHash)
    {
        String v = Integer.toHexString(color | 0xFF000000).substring(2);
        return inclHash? ("#" + v) : v;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Converts the specified 0..255 color value to a float
    *** @param colorVal The 0..255 color value
    *** @return The float value
    **/
    private static float _toFloat(int colorVal)
    {
        return _bound((float)colorVal / 255.0F);
    }

    /**
    *** Performs bounds checking on the specified 0..255 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..255
    **/
    private static int _bound(int colorVal)
    {
        if (colorVal <   0) { colorVal =   0; }
        if (colorVal > 255) { colorVal = 255; }
        return colorVal;
    }

    /**
    *** Performs bounds checking on the specified 0..1 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..1 (inclusive)
    **/
    private static double _bound(double colorVal)
    {
        if (colorVal < 0.0) { colorVal = 0.0; }
        if (colorVal > 1.0) { colorVal = 1.0; }
        return colorVal;
    }

    /**
    *** Performs bounds checking on the specified 0..1 color value
    *** @param colorVal  The color value to bounds check
    *** @return A color value that is guaranteed to be within 0..1 (inclusive)
    **/
    private static float _bound(float colorVal)
    {
        if (colorVal < 0.0F) { colorVal = 0.0F; }
        if (colorVal > 1.0F) { colorVal = 1.0F; }
        return colorVal;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        float P = RTConfig.getFloat("P",0.50F);

        RGB C1 = ColorTools.parseColor(RTConfig.getString("C1","red")  , (RGB)null);
        if (C1 != null) {
            Print.sysPrintln("Color #1 (specified): " + C1);
            Print.sysPrintln("Color #1 (lighter)  : " + C1.lighter(P) + " " + Math.round(100.0F*P)+"%");
            Print.sysPrintln("Color #1 (darker)   : " + C1.darker(P)  + " " + Math.round(100.0F*P)+"%");
        }

        RGB C2 = ColorTools.parseColor(RTConfig.getString("C2","green"), (RGB)null);
        if (C2 != null) {
            Print.sysPrintln("Color #2 (specified): " + C2);
            Print.sysPrintln("Color #2 (lighter)  : " + C2.lighter(P) + " " + Math.round(100.0F*P)+"%");
            Print.sysPrintln("Color #2 (darker)   : " + C2.darker(P)  + " " + Math.round(100.0F*P)+"%");
        }

        if ((C1 != null) && (C2 != null)) {
            float F1[] = C1.getRGBColorComponents();
            float F2[] = C2.getRGBColorComponents();
            float D[]  = new float[3];
            for (int i = 0; i < D.length; i++) {
                if (F1[i] > F2[i]) {
                    // -- F1[i] is lighter than F2[i]
                    D[i] = (F1[i] - F2[i]) / (1.0F - F2[i]);
                    Print.sysPrintln("Color1["+i+"] lighter than Color2["+i+"]: " + D[i]);
                } else {
                    // -- F1[i] is darker than F2[i]
                    D[i] = ((F2[i] - F1[i]) / F2[i]);
                    Print.sysPrintln("Color1["+i+"] darker  than Color2["+i+"]: " + D[i]);
                }
            }
        }

    }

}
