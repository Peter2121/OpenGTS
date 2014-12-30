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
//  2013/05/19  Martin D. Flynn
//     -Initial Creation
// ----------------------------------------------------------------------------

var CALENDAR_FADE = false;

// ----------------------------------------------------------------------------

/**
*** Show License Expire Calander
**/
function driverToggleLicExpCalendar()
{
    if (licExpCal) {
        var cal = licExpCal;
        var fld = document.getElementById(ID_LICENSE_EXPIRE);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, CALENDAR_FADE);
            if (cal) { fld.value = cal.getDateAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, CALENDAR_FADE);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, CALENDAR_FADE);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};
