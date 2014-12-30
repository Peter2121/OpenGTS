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
//  2009/09/23  Martin D. Flynn
//     -Initial release
//  2012/04/03  Martin D. Flynn
//     -Added "validateNewPassword", "getPasswordFormatDescription", "hasPasswordExpired"
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.Locale;

public interface PasswordHandler
{

    /**
    *** Gets the name of this instance
    *** @return The name of this instance
    **/
    public String getName();

    /**
    *** Encode/Encrypt password
    *** @param userPass  Password to encode/encrypt
    *** @return The encoded/encrypted password
    **/
    public String encodePassword(String userPass);

    /**
    *** Decode/Decrypt password
    *** @param tablePass  Password to decode/decrypt
    *** @return The decoded/decrypted password, or null if the password cannot be decoded/decrypted
    **/
    public String decodePassword(String tablePass);

    /**
    *** Returns true if the entered password matches the password saved in the table
    *** @param enteredPass  User entered password
    *** @param tablePass    The password saved in the table (possibly encrypted)
    *** @return True if password match, false otherwise
    **/
    public boolean checkPassword(String enteredPass, String tablePass);

    /**
    *** Returns true if the new password adheres to the password policy
    *** @param newPass  The new password candidate
    *** @return True if the password adheres to the password policy, false otherwise
    **/
    public boolean validateNewPassword(String newPass);

    /**
    *** Gets the short text description of the acceptable password format
    *** @param locale  The language locale
    *** @return The short text description of the acceptable password format
    **/
    public String getPasswordFormatDescription(Locale locale);

    /**
    *** Returns true if the password has expired (time for new password)
    *** @param lastChangedTime  Time of last password change
    *** @return True is the password has expired
    **/
    public boolean hasPasswordExpired(long lastChangedTime);

}
