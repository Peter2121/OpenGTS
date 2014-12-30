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
//  2012/04/03  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.security.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class GeneralPasswordHandler
    implements PasswordHandler
{

    // ------------------------------------------------------------------------
    
    public static final String PROP_debugCheckPassword          = "debugCheckPassword";

    public static final String PROP_passwordEncoding            = "passwordEncoding";
    public static final String PROP_minimumPasswordLength       = "minimumPasswordLength";
    public static final String PROP_maximumPasswordAgeSeconds   = "maximumPasswordAgeSeconds";
    public static final String PROP_specialCharacters           = "specialCharacters";
    public static final String PROP_minimumLowerAlphaChars      = "minimumLowerAlphaChars";
    public static final String PROP_minimumUpperAlphaChars      = "minimumUpperAlphaChars";
    public static final String PROP_minimumAlphaChars           = "minimumAlphaChars";
    public static final String PROP_minimumDigitChars           = "minimumDigitChars";
    public static final String PROP_minimumSpecialChars         = "minimumSpecialChars";
    public static final String PROP_minimumNonAlphaChars        = "minimumNonAlphaChars";
    public static final String PROP_minimumCategories           = "minimumCategories";
    public static final String PROP_passwordFormatDescription   = "passwordFormatDescription";

    private static final RTKey.Entry PROP_Entry[] = {
        new RTKey.Entry(PROP_passwordEncoding           ,              "plain", "Password Encoding"),
        new RTKey.Entry(PROP_minimumPasswordLength      ,                    1, "Minimum Password Length"),
        new RTKey.Entry(PROP_maximumPasswordAgeSeconds  ,                   0L, "Maximum Password Age (seconds)"),
        new RTKey.Entry(PROP_specialCharacters          , "!@#$%^&*()_+-:;.?/", "Special Characters"),
        new RTKey.Entry(PROP_minimumLowerAlphaChars     ,                    0, "Minimum Lower-Case Alpha Characters"),
        new RTKey.Entry(PROP_minimumUpperAlphaChars     ,                    0, "Minimum Upper-Case Alpha Characters"),
        new RTKey.Entry(PROP_minimumAlphaChars          ,                    0, "Minimum Alpha Characters"),
        new RTKey.Entry(PROP_minimumDigitChars          ,                    0, "Minimum Digit Characters"),
        new RTKey.Entry(PROP_minimumSpecialChars        ,                    0, "Minimum Special Characters"),
        new RTKey.Entry(PROP_minimumNonAlphaChars       ,                    0, "Minimum Non-Alpha Characters"),
        new RTKey.Entry(PROP_minimumCategories          ,                    0, "Minimum Number of Categories"),
        new RTKey.Entry(PROP_debugCheckPassword         ,                false, "Debug 'checkPassword'"),
        new RTKey.Entry(PROP_passwordFormatDescription  ,                   "", "Password Format Error Message"),
    };

    private static RTProperties DefaultProps = null;

    static {
        DefaultProps = new RTProperties();
        for (int i = 0; i < PROP_Entry.length; i++) {
            String rtKey = PROP_Entry[i].getKey();
            Object rtVal = PROP_Entry[i].getDefault();
            DefaultProps.setProperty(rtKey, rtVal);
        }
    }

    // ------------------------------------------------------------------------
    
    public static final int     MD5_HASH_LEN    = 32;
    
    public static final String  ENC_MD5         = "md5";
    public static final String  ENC_PLAIN       = "plain";
    public static final String  ENC_MD5PLAIN    = "md5plain";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class PasswordEncodingException
        extends RuntimeException
    {
        public PasswordEncodingException(String msg) {
            super(msg);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          name            = null;
    private RTProperties    rtProps         = null;
    private String          encoding        = null;
    private boolean         checkPlain      = false;
    
    private boolean         debugCheckPass  = false;

    /**
    *** Default Constructor
    **/
    public GeneralPasswordHandler()
        throws PasswordEncodingException
    {
        this(null,null);
    }

    /**
    *** Constructor
    *** @param rtp  The property settings for this instance
    **/
    public GeneralPasswordHandler(RTProperties rtp)
        throws PasswordEncodingException
    {
        this(null,rtp);
    }

    /**
    *** Constructor
    *** @param name This password handler name
    *** @param rtp  The property settings for this instance
    **/
    public GeneralPasswordHandler(String name, RTProperties rtp)
        throws PasswordEncodingException
    {

        /* set vars */
        this.rtProps = (rtp != null)? rtp : new RTProperties();
        this.name = !StringTools.isBlank(name)? name : null;

        /* check encoding */
        String encType = this.getString(PROP_passwordEncoding, "");
        if (encType.equalsIgnoreCase(ENC_MD5)) {
            this.encoding   = ENC_MD5;
            this.checkPlain = false;
        } else
        if (encType.equalsIgnoreCase("hash")) {
            this.encoding   = ENC_MD5;
            this.checkPlain = false;
        } else
        if (encType.equalsIgnoreCase(ENC_MD5PLAIN)) {
            this.encoding   = ENC_MD5;
            this.checkPlain = true;
        } else
        if (encType.equalsIgnoreCase("hashplain")) {
            this.encoding   = ENC_MD5;
            this.checkPlain = true;
        } else
        if (encType.equalsIgnoreCase(ENC_PLAIN)) {
            this.encoding   = ENC_PLAIN;
            this.checkPlain = true;
        } else
        if (encType.equalsIgnoreCase("none")) {
            this.encoding   = ENC_PLAIN;
            this.checkPlain = true;
        } else
        if (StringTools.isBlank(encType)) {
            this.encoding   = ENC_PLAIN;
            this.checkPlain = true;
        } else {
            throw new PasswordEncodingException("Invalid Encoding: " + encType);
        }
        //Print.logStackTrace("here");

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of this PasswordHandler
    *** @return The name of this PasswordHandler
    **/
    public String getName()
    {
        return !StringTools.isBlank(this.name)? this.name : this.encoding;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the encoding used for this PasswordHandler
    **/
    public String getEncoding()
    {
        return this.encoding;
    }

    /**
    *** Returns true if the current encoding matches the specified value
    **/
    private boolean isEncoding(String enc)
    {
        return this.encoding.equalsIgnoreCase(enc);
    }

    /**
    *** Returns the encoding as a displayable string 
    *** (this differs from "getEncoding()" in that if the encoding was originally
    *** "md5plain", then the returned string will be "md5plain" - where "getEncoding()"
    *** would just return "md5")
    **/
    public String getEncodingString()
    {
        if (this.isEncoding(ENC_MD5) && this.checkPlain()) {
            return ENC_MD5PLAIN;
        } else {
            return this.getEncoding();
        }
    }

    /**
    *** Returns true if encoding id "md5" and plain-text password should also be checked
    *** @return True if plain-text passwords should also be checked
    **/
    public boolean checkPlain()
    {
        return this.checkPlain;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the value of the Object property key
    **/
    protected Object getProperty(String key, Object dft)
    {
        if (this.rtProps.hasProperty(key)) {
            return this.rtProps.getProperty(key, dft);
        } else {
            return DefaultProps.getProperty(key, dft);
        }
    }

    /**
    *** Returns true if the property is defined
    ** @param key  The property to check
    **/
    protected boolean hasProperty(String key)
    {
        if (this.rtProps.hasProperty(key) || DefaultProps.hasProperty(key)) {
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Returns the value of the String property key
    **/
    protected String getString(String key, String dft)
    {
        if (this.rtProps.hasProperty(key)) {
            return this.rtProps.getString(key, dft);
        } else {
            return DefaultProps.getString(key, dft);
        }
    }

    /**
    *** Returns the value of the Long property key
    **/
    protected long getLong(String key, long dft)
    {
        if (this.rtProps.hasProperty(key)) {
            return this.rtProps.getLong(key, dft);
        } else {
            return DefaultProps.getLong(key, dft);
        }
    }

    /**
    *** Returns the value of the Long property key
    **/
    protected int getInt(String key, int dft)
    {
        if (this.rtProps.hasProperty(key)) {
            return this.rtProps.getInt(key, dft);
        } else {
            return DefaultProps.getInt(key, dft);
        }
    }

    /**
    *** Returns the value of the Long property key
    **/
    protected boolean getBoolean(String key, boolean dft)
    {
        if (this.rtProps.hasProperty(key)) {
            return this.rtProps.getBoolean(key, dft);
        } else {
            return DefaultProps.getBoolean(key, dft);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Encode password
    *** @param pass The password to encode
    *** @return The encoded password
    **/
    public String encodePassword(String pass) 
    {
        if ((pass == null) || pass.equals("")) { // spaces are significant
            return pass; // blank encodes to blank
        } else
        if (this.isEncoding(ENC_MD5)) {
            try {
                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                md5Digest.update(pass.getBytes(), 0, pass.length());
                String md5Pass = (new BigInteger(1, md5Digest.digest())).toString(16);
                return md5Pass; 
            } catch (NoSuchAlgorithmException nsae) {
                Print.logException("MD5 Algorithm not found", nsae);
                return null;
            }
        } else
        if (this.isEncoding(ENC_PLAIN)) {
            return pass; // leave as-is
        } else {
            Print.logStackTrace("Invalid password encoding: " + this.encoding);
            return pass;
        }
    }

    /**
    *** Decode password
    *** @param pass  The password to decode
    *** @return The decoded password, or null if the password cannot be decoded
    **/
    public String decodePassword(String pass) 
    {
        if ((pass == null) || pass.equals("")) { // spaces are significant
            return pass; // blank encodes to blank
        } else
        if (this.isEncoding(ENC_MD5)) {
            return null; // hash not decodable
        } else
        if (this.isEncoding(ENC_PLAIN)) {
            return pass; // leave as-is
        } else {
            Print.logStackTrace("Invalid password encoding: " + this.encoding);
            return pass;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets debug logging for "checkPassword"
    *** @param debugLog  True to enable debug logging, false to disable
    **/
    public void setDebugCheckPassword(boolean debugLog)
    {
        this.rtProps.setBoolean(PROP_debugCheckPassword, debugLog);
    }

    /**
    *** Gets debug logging for "checkPassword"
    *** @return True if "checkPassword" debug logging enabled, false otherwise
    **/
    public boolean getDebugCheckPassword()
    {
        return this.getBoolean(PROP_debugCheckPassword, false);
    }

    /** 
    *** Check entered password against stored password
    *** @param enteredPass  The User entered password
    *** @param tablePass    The password value from the Account/User table
    *** @return True if the passwords match
    **/
    public boolean checkPassword(String enteredPass, String tablePass) 
    {
        boolean LOG = this.getDebugCheckPassword();
        if (LOG) { Print.logInfo("[DEBUG] PasswordHandler="+this +", enteredPass="+enteredPass +", tablePass="+tablePass); }

        /* not password specified in table */
        if (StringTools.isBlank(tablePass)) {
            if (LOG) { Print.logInfo("[DEBUG] Login Failed: No table password"); }
            return false; // login not allowed for accounts with no password
        }

        /* no user-entered passowrd */
        if (enteredPass == null) {
            if (tablePass.equals(Account.BLANK_PASSWORD)) {
                if (LOG) { Print.logInfo("[DEBUG] Login OK: null password"); }
                return true; // blank password is ok here
            } else {
                if (LOG) { Print.logInfo("[DEBUG] Login Failed: No entered password"); }
                return false; // no password provided (not even a blank string)
            }
        }

        /* blank password */
        if (enteredPass.equals("") && tablePass.equals(Account.BLANK_PASSWORD)) {
            if (LOG) { Print.logInfo("[DEBUG] Login OK: blank password"); }
            return true; // blank password is ok here
        }

        /* check entered password */
        if (tablePass.equals(this.encodePassword(enteredPass))) { // fixed 2.2.7
            if (LOG) { Print.logInfo("[DEBUG] Login OK: Entered password matches encoded table password"); }
            return true; // passwords match
        }

        /* Hash special case, check as plain text password? */
        if (this.isEncoding(ENC_MD5) && (tablePass.length() != MD5_HASH_LEN) && this.checkPlain()) {
            if (tablePass.equals(enteredPass)) {
                if (LOG) { Print.logInfo("[DEBUG] Login OK: MD5/Plain match"); }
                return true;
            } else {
                if (LOG) { Print.logInfo("[DEBUG] Login Failed: MD5/Plain did not match"); }
                return false;
            }
        }

        /* failed */
        if (LOG) { Print.logInfo("[DEBUG] Login Failed: No match"); }
        return false; // password does not match

    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if the specified character is an allowed special character
    *** @param ch  The character to test
    *** @return True if the character is an allowed special character
    **/
    private boolean isSpecialChar(char ch)
    {
        String specChars = this.getString(PROP_specialCharacters, "");
        return (specChars.indexOf(ch) >= 0);
    }

    /**
    *** Return true if the count is >= minimum count
    *** @param key Count property key
    *** @param count  The count to check
    *** @return True if the count is >= minimum count, false otherwise
    **/
    private boolean checkCharCount(String key, int count)
    {
        int min = this.getInt(key, 0);
        if ((min <= 0) || (count >= min)) {
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Checks new password and returns true if the password passes the policy
    *** for newly created password.
    *** @param newPass  The password to validate as acceptable
    *** @return True if the specified password is acceptable
    **/
    public boolean validateNewPassword(String newPass) 
    {

        /* password not specified */
        if (newPass == null) {
            // password not specified
            return false;
        }

        /* empty password allowed? */
        if (newPass.equals("")) {
            // an empty password would prevent user login
            return false;
        }

        /* minimum length */
        int minLen = this.getInt(PROP_minimumPasswordLength, 0);
        if (StringTools.length(newPass) < minLen) {
            // too short
            return false;
        }

        /* count char types */
        int lowerCount    = 0;
        int upperCount    = 0;
        int alphaCount    = 0;
        int digitCount    = 0;
        int specialCount  = 0;
        int nonAlphaCount = 0;
        for (int i = 0; i < newPass.length(); i++) {
            char ch = newPass.charAt(i);
            if (Character.isLowerCase(ch)) {
                lowerCount++;
                alphaCount++;
            } else
            if (Character.isUpperCase(ch)) {
                upperCount++;
                alphaCount++;
            } else
            if (Character.isDigit(ch)) {
                digitCount++;
                nonAlphaCount++;
            } else
            if (this.isSpecialChar(ch)) {
                specialCount++;
                nonAlphaCount++;
            } else {
                // invalid character
                return false;
            }
        }

        /* check minimum counts */
        if (!this.checkCharCount(PROP_minimumLowerAlphaChars, lowerCount   )) {
            // not enough lower alpha
            return false;
        }
        if (!this.checkCharCount(PROP_minimumUpperAlphaChars, upperCount   )) {
            // not enough upper alpha
            return false;
        }
        if (!this.checkCharCount(PROP_minimumAlphaChars     , alphaCount   )) {
            // not enough alpha
            return false;
        }
        if (!this.checkCharCount(PROP_minimumDigitChars     , digitCount   )) {
            // not enough digits
            return false;
        }
        if (!this.checkCharCount(PROP_minimumSpecialChars   , specialCount )) {
            // not enough special
            return false;
        }
        if (!this.checkCharCount(PROP_minimumNonAlphaChars  , nonAlphaCount)) {
            // not enough non-alpha
            return false;
        }

        /* category count */
        int minCategories = this.getInt(PROP_minimumCategories, 0);
        if (minCategories > 0) {
            int catCnt = 0;
            if (lowerCount   > 0) { catCnt++; } // lower-case
            if (upperCount   > 0) { catCnt++; } // upper-case
            if (digitCount   > 0) { catCnt++; } // digits
            if (specialCount > 0) { catCnt++; } // special characters
            int minCat = (minCategories <= 4)? minCategories : 4;
            if (catCnt < minCat) {
                // not enough categories
                return false;
            }
        }

        /* ok */
        return true;

    }

    /**
    *** Returns a text description of the valid characters alowed for a password
    *** @param locale  The locale
    *** @return The text description of the password format requirements
    **/
    public String getPasswordFormatDescription(Locale locale) 
    {
        I18N i18n = I18N.getI18N(GeneralPasswordHandler.class, locale);

        /* check for predefined description */
        Object fmtDesc = this.getProperty(PROP_passwordFormatDescription,null);
        if (fmtDesc instanceof I18N.Text) {
            String desc = ((I18N.Text)fmtDesc).toString(i18n);
            if (!StringTools.isBlank(desc)) {
                return desc;
            }
        } else
        if (fmtDesc instanceof String) {
            String desc = (String)fmtDesc;
            if (!StringTools.isBlank(desc)) {
                return desc;
            }
        }

        /* create description based on current settings */
        StringBuffer sb = new StringBuffer();

        /* minimum length */
        int minLen = this.getInt(PROP_minimumPasswordLength, 0);
        if (minLen > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.minimumLength", 
                "- At least {0} characters in length", String.valueOf(minLen)));
            sb.append("\n");
        }

        /* special characters */
        String specChars = this.getString(PROP_specialCharacters, "");
        if (!StringTools.isBlank(specChars)) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.specialChars", 
                "- May contain special characters \"{0}\"", specChars));
            sb.append("\n");
        }

        /* min lower-alpha characters */
        int minLower = this.getInt(PROP_minimumLowerAlphaChars, 0);
        if (minLower > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.lowerAlphaCount", 
                "- At least {0} lower-case characters", String.valueOf(minLower)));
            sb.append("\n");
        }

        /* min upper-alpha characters */
        int minUpper = this.getInt(PROP_minimumUpperAlphaChars, 0);
        if (minUpper > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.upperAlphaCount", 
                "- At least {0} upper-case characters", String.valueOf(minUpper)));
            sb.append("\n");
        }

        /* min upper-alpha characters */
        int minAlpha = this.getInt(PROP_minimumAlphaChars, 0);
        if (minAlpha > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.alphaCount", 
                "- At least {0} alpha characters", String.valueOf(minAlpha)));
            sb.append("\n");
        }

        /* min digit characters */
        int minDigit = this.getInt(PROP_minimumDigitChars, 0);
        if (minDigit > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.digitCount", 
                "- At least {0} numeric digits", String.valueOf(minDigit)));
            sb.append("\n");
        }

        /* min special characters */
        int minSpec = this.getInt(PROP_minimumSpecialChars, 0);
        if (minSpec > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.specialCount", 
                "- At least {0} special characters", String.valueOf(minSpec)));
            sb.append("\n");
        }

        /* min non-alpha characters */
        int minNonAlpha = this.getInt(PROP_minimumNonAlphaChars, 0);
        if (minNonAlpha > 0) {
            sb.append(i18n.getString("GeneralPasswordHandler.format.nonAlphaCount", 
                "- At least {0} non-alpha characters", String.valueOf(minNonAlpha)));
            sb.append("\n");
        }

        /* min categories */
        int minCategories = this.getInt(PROP_minimumCategories, 0);
        if (minCategories > 0) {
            int minCat = (minCategories <= 4)? minCategories : 4;
            sb.append(i18n.getString("GeneralPasswordHandler.format.categoryCount", 
                "- At least {0} of the 4 categories (lower/upper/digit/special)", String.valueOf(minCat)));
            sb.append("\n");
        }

        /* return description */
        return sb.toString();

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the password has passed its expiration date
    *** @param lastChangedTime  The Epoch timestamp when the password was last changed
    *** @return True is the password has passed its expiration date
    **/
    public boolean hasPasswordExpired(long lastChangedTime) 
    {
        long maxAgeSec = this.getLong(PROP_maximumPasswordAgeSeconds, 0L);

        /* no expiration time */
        if (maxAgeSec <= 0L) {
            return false; // no expiration
        }

        /* no last changed time */
        if (lastChangedTime <= 0L) {
            return false;
        }

        /* expired */
        long ageSec = DateTime.getCurrentTimeSec() - lastChangedTime;
        if (ageSec < 0L) {
            // password changed in the future?
            return false;
        } else
        if (ageSec <= maxAgeSec) {
            // not yet expired
            return false;
        } else {
            // expired
            return true;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Return String representation of this instance
    *** @return String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[GeneralPasswordHandler]\n");
        sb.append("  Encoding String = ").append(this.getEncodingString()).append("\n");
        for (RTKey.Entry pe : PROP_Entry) {
            String pk = pe.getKey();
            String ph = pe.getHelp();
            if (!this.hasProperty(pk)) { continue; }
            sb.append("  ").append(pk).append(" = ");
            sb.append(this.getProperty(pk,""));
            sb.append("  (").append(ph).append(")");
            sb.append("\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    //  <PasswordHandler 
    //      name="hash"
    //      class="org.opengts.db.GeneralPasswordHandler"
    //      />
    //      <Property key="passwordEncoding">plain</Property>   <!-- md5|md5plain|plain -->
    //      <Property key="minimumPasswordLength">6</Property>
    //      <Property key="minimumLowerAlphaChars">0</Property>
    //      <Property key="minimumUpperAlphaChars">0</Property>
    //      <Property key="minimumAlphaChars">0</Property>
    //      <Property key="minimumDigitChars">0</Property>
    //      <Property key="minimumNonAlphaChars">0</Property>
    //  </PasswordHandler>

    public static final String ARG_PASSWORD[]   = { "password", "passwd", "pass", "pwd"   };
    public static final String ARG_ENCODING[]   = { "encoding", "enc"             };
    public static final String ARG_MINLEN[]     = { "minlen"  , "length", "len"   };
    public static final String ARG_MINLOWER[]   = { "minLower", "lower"           };
    public static final String ARG_MINUPPER[]   = { "minUpper", "upper"           };
    public static final String ARG_MINALPHA[]   = { "minAlpha", "alpha"           };
    public static final String ARG_MINDIGIT[]   = { "minDigit", "digit"           };
    public static final String ARG_MINSPEC[]    = { "minSpec" , "special", "spec" };
    public static final String ARG_TBLPASS[]    = { "tblPass" , "table"           };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String passwd  = RTConfig.getString(ARG_PASSWORD,"");
        String tblPass = RTConfig.getString(ARG_TBLPASS ,"");

        /* create PasswordHandler */
        RTProperties rtp = new RTProperties();
        rtp.setString(PROP_passwordEncoding      , RTConfig.getString(ARG_ENCODING, "plain"));
        rtp.setInt   (PROP_minimumPasswordLength , RTConfig.getInt   (ARG_MINLEN  ,       1));
        rtp.setInt   (PROP_minimumLowerAlphaChars, RTConfig.getInt   (ARG_MINLOWER,       0));
        rtp.setInt   (PROP_minimumUpperAlphaChars, RTConfig.getInt   (ARG_MINUPPER,       0));
        rtp.setInt   (PROP_minimumAlphaChars     , RTConfig.getInt   (ARG_MINALPHA,       0));
        rtp.setInt   (PROP_minimumDigitChars     , RTConfig.getInt   (ARG_MINDIGIT,       0));
        rtp.setInt   (PROP_minimumSpecialChars   , RTConfig.getInt   (ARG_MINSPEC ,       0));
        GeneralPasswordHandler gph = null;
        try {
            gph = new GeneralPasswordHandler(rtp);
            Print.sysPrintln(gph.toString());
            Print.sysPrintln(gph.getPasswordFormatDescription(null));
        } catch (PasswordEncodingException pe) {
            Print.sysPrintln("ERROR: " + pe);
            System.exit(1);
        }

        /* validate password */
        Print.sysPrintln("Password: entered=" + passwd + ", table=" + tblPass);
        Print.sysPrintln("Valid   : " + gph.validateNewPassword(passwd));
        Print.sysPrintln("Encoded : " + gph.encodePassword(passwd));
        Print.sysPrintln("Match   : " + gph.checkPassword(passwd,tblPass));

    }

}
