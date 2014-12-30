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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/03/31  Martin D. Flynn
//     -Added new status codes:
//      STATUS_INITIALIZED, STATUS_WAYMARK
//  2006/06/17  Martin D. Flynn
//      Copied from the OpenDMTP Server package.
//      OpenDMTP Protocol Definition v0.1.1 Conformance: These status-code values 
//      conform to the definition specified by the OpenDMTP protocol and must 
//      remain as specified here.  When extending the status code values to
//      encompass other purposes, it is recommended that values in the following
//      range be used: 0x0001 to 0xDFFF
//  2007/01/25  Martin D. Flynn
//     -Added new status codes:
//      STATUS_QUERY, STATUS_LOW_BATTERY, STATUS_OBD_FAULT, STATUS_OBD_RANGE,
//      STATUS_OBC_RPM_RANGE, STATUS_OBC_FUEL_RANGE, STATUS_OBC_OIL_RANGE,
//      STATUS_OBC_TEMP_RANGE, STATUS_MOTION_MOVING
//     -Changed "Code" descriptions to start their indexing at '1' (instead of '0') 
//      since this string value is used to display to the user on various reports.
//  2007/03/11  Martin D. Flynn
//     -'GetCodeDescription' defaults to hex value of status code (or status code
//      name) if code is not found in the table.
//  2007/03/30  Martin D. Flynn
//     -Added new status code: STATUS_POWER_FAILURE
//  2007/04/15  Martin D. Flynn
//     -Added new status codes: STATUS_GEOBOUNDS_ENTER, STATUS_GEOBOUNDS_EXIT
//  2007/11/28  Martin D. Flynn
//     -Added new status codes: STATUS_EXCESS_BRAKING
//  2008/04/11  Martin D. Flynn
//     -Added "IsDigitalInput..." methods
//  2008/07/27  Martin D. Flynn
//     -Changed 'description' of digital inputs/outputs to start at '0' (instead of '1')
//     -Added "GetDigitalInputIndex".
//  2008/09/01  Martin D. Flynn
//     -Changed status 'description' index's to start at '0' to match the status 'name'.
//  2008/10/16  Martin D. Flynn
//     -Added the following status codes: STATUS_MOTION_IDLE, STATUS_POWER_RESTORED,
//      STATUS_WAYMARK_3,  STATUS_INPUT_ON_08/09, STATUS_INPUT_OFF_08/09, 
//      STATUS_OUTPUT_ON_08/09, STATUS_OUTPUT_OFF_08/09
//  2009/01/01  Martin D. Flynn
//     -Internationalized StatusCode descriptions.
//  2009/05/01  Martin D. Flynn
//     -Added STATUS_GPS_EXPIRED, STATUS_GPS_FAILURE, STATUS_CONNECTION_FAILURE
//     -Added STATUS_IGNITION_ON, STATUS_IGNITION_OFF
//  2009/12/16  Martin D. Flynn
//     -Added Garmin GFMI status codes.
//  2010/04/11  Martin D. Flynn
//     -Added STATUS_WAYMARK_4..8, STATUS_NOTIFY, STATUS_IMPACT, STATUS_PANIC_*
//      STATUS_ASSIST_*, STATUS_MEDICAL_*, STATUS_OBC_INFO_#, STATUS_CONFIG_RESET, ...
//  2010/05/24  Martin D. Flynn
//     -Added STATUS_DAY_SUMMARY
//  2010/07/04  Martin D. Flynn
//     -Added STATUS_BATTERY_LEVEL
//  2010/09/09  Martin D. Flynn
//     -Added STATUS_PARKED, STATUS_SHUTDOWN, STATUS_SUSPEND, STATUS_RESUME,
//      STATUS_LAST_LOCATION
//  2010/10/25  Martin D. Flynn
//     -Added STATUS_SAMPLE_#, STATUS_TOWING_START, STATUS_TOWING_STOP
//  2011/01/28  Martin D. Flynn
//     -Added STATUS_MOTION_EXCESS_IDLE, STATUS_EXCESS_PARK, STATUS_MOTION_HEADING,
//      STATUS_MOTION_ACCELERATION, STATUS_MOTION_DECELERATION, STATUS_GPS_JAMMING,
//      STATUS_MODEM_JAMMING
//  2011/03/08  Martin D. Flynn
//     -Added STATUS_CORRIDOR_ARRIVE, STATUS_CORRIDOR_DEPART, STATUS_HEARTBEAT
//     -Added STATUS_RULE_TRIGGER_{0..7}
//  2011/04/01  Martin D. Flynn
//     -Added STATUS_ENGINE_START, STATUS_ENGINE_STOP, STATUS_FUEL_REFILL
//     -Added STATUS_FUEL_THEFT, STATUS_UNPARKED, STATUS_MOTION_CHANGE
//     -Added STATUS_POWER_OFF, STATUS_POWER_ON
//     -Added STATUS_BATT_CHARGE_ON, STATUS_BATT_CHARGE_OFF
//  2011/05/13  Martin D. Flynn
//     -Added option to override StatusCode "High-Priority" state.
//     -Added STATUS_LOCATION_1, STATUS_LOCATION_2, STATUS_TRAILER_HOOK, STATUS_TRAILER_UNHOOK,
//      STATUS_RFID_CONNECT, STATUS_RFID_DISCONNECT, STATUS_EXCESS_CORNERING
//  2011/06/16  Martin D. Flynn
//     -Changed all "STATUS_SENSOR32_*" to "STATUS_ANALOG_*"
//  2011/07/01  Martin D. Flynn
//     -Added STATUS_CHECK_ENGINE, STATUS_CELL_LOCATION, STATUS_BATTERY_VOLTS
//     -"STATUS_OBC_*" changed to "STATUS_OBD_*"
//     -Change "Hi-Pri" state on several status codes.
//     -Added command-line option to output status code in a properties file format.
//  2011/08/21  Martin D. Flynn
//     -Added STATUS_POI_0..4, STATUS_MOTION_STOP_PENDING
//  2011/10/03  Martin D. Flynn
//     -Added STATUS_CONNECTION_RESTORED, STATUS_DUTY_ON, STATUS_DUTY_OFF
//     -Added STATUS_VIBRATION_ON, STATUS_VIBRATION_OFF
//     -Added STATUS_IMAGE_0 .. STATUS_IMAGE_3
//  2011/12/06  Martin D. Flynn
//     -Added STATUS_LOW_FUEL, STATUS_PERSON_ENTER, STATUS_PERSON_EXIT
//  2012/02/03  Martin D. Flynn
//     -Added STATUS_ALARM_ON, STATUS_ALARM_OFF, STATUS_ARMED, STATUS_DISARMED
//  2012/04/03  Martin D. Flynn
//     -Added STATUS_EXCESS_BRAKING_2, STATUS_EXCESS_BRAKING_3 
//     -Added STATUS_EXCESS_CORNERING_2, STATUS_EXCESS_CORNERING_3
//     -Added STATUS_EXCESS_ACCEL, STATUS_EXCESS_ACCEL_2, STATUS_EXCESS_ACCEL_3
//     -Added STATUS_GPS_ANTENNA_OPEN, STATUS_GPS_ANTENNA_SHORT
//     -Added STATUS_PTO_ON, STATUS_PTO_OFF, STATUS_POWER_ALARM
//  2012/06/29  Martin D. Flynn
//     -Added STATUS_OBD_DISCONNECT
//  2012/10/16  Martin D. Flynn
//     -Added STATUS_DOOR_OPEN_#/STATUS_DOOR_CLOSE_#
//     -Added STATUS_SEATBELT_ON_#/STATUS_SEATBELT_OFF_#
//     -Added STATUS_OBD_LOAD_RANGE, STATUS_OBD_COOLANT_RANGE
//     -Added STATUS_ENTITY_INVENTORY, STATUS_TRAILER_INVENTORY, STATUS_RFID_INVENTORY, STATUS_PERSON_INVENTORY
//     -Added STATUS_ENTITY_STATE, STATUS_TRAILER_STATE, STATUS_RFID_STATE, STATUS_PERSON_STATE
//     -Added STATUS_COMMAND_ACK, STATUS_COMMAND_NAK
//  2012/12/24  Martin D. Flynn
//     -Added STATUS_MANDOWN_ON, STATUS_MANDOWN_OFF, STATUS_FREEFALL, STATUS_LOG
//     -Added STATUS_DATA_MESSAGE_#, STATUS_VOICE, STATUS_CONFIG_COMPLETE, STATUS_CONFIG_START
//     -Added STATUS_CONFIG_FAILED
//  2013/03/01  Martin D. Flynn
//     -Added STATUS_TRACK_START, STATUS_TRACK_LOCATION, STATUS_TRACK_STOP
//     -Added STATUS_GFMI_STOP_STATUS, STATUS_GFMI_ETA, STATUS_GFMI_DRIVER_ID, STATUS_GFMI_DRIVER_STATUS
//     -Added STATUS_GPS_RESTORED
//  2013/04/08  Martin D. Flynn
//     -Added STATUS_IP_ADDRESS, STATUS_GFMI_STOP_STATUS_#, STATUS_DISTANCE#
//  2013/05/28  Martin D. Flynn
//     -Added "IsAcceleration", "IsDeceleration", "IsBraking", "IsCornering"
//     -Added STATUS_MOTION_ACCELEROMETER, STATUS_TEMPERATURE_LOW_0, STATUS_TEMPERATURE_HIGH_0
//  2013/08/06  Martin D. Flynn
//     -Added STATUS_VOICE_OUTGOING, STATUS_VOICE_INCOMING
//     -Added STATUS_TIRE_TEMP_RANGE, STATUS_TIRE_PRESSURE_RANGE, STATUS_TIRE_PRESSURE_LOW,
//      STATUS_TIRE_BATTERY_LOW
//  2013/09/08  Martin D. Flynn
//     -Added STATUS_SPEEDING_BEGIN, STATUS_SPEEDING_LIMIT_#, STATUS_SPEEDING_END
//     -Added STATUS_RFID_TAG_#, STATUS_SHUTDOWN_CANCELLED
//  2013/11/11  Martin D. Flynn
//     -Added STATUS_DRIVER_SLEEP, STATUS_DRIVER_WAKE
//  2014/03/03  Martin D. Flynn
//     -Added STATUS_TRIP_SUMMARY, STATUS_GPS_LOST
//  2014/05/05  Martin D. Flynn
//     -Added STATUS_FUEL_DIRTY, STATUS_FUEL_SENSOR, STATUS_TAMPER_[ON|OFF]
//     -Added STATUS_TEMPERATURE_OK, STATUS_DOOR_[LOCK|UNLOCK]
//     -Added STATUS_MOTION_EN_ROUTE synonym for STATUS_MOTION_IN_MOTION
//  2014/09/16  Martin D. Flynn
//     -Added STATUS_MODEM_RESTORED
//     -Added STATUS_ROAMING_ON, STATUS_ROAMING_OFF, STATUS_ROAMING_UNNOWN
//     -Added STATUS_IMAGE_LOC_[0..3]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;

import org.opengts.util.*;

public class StatusCodes
{

    /* Digital Input index for explicit STATUS_IGNITION_ON/STATUS_IGNITION_OFF codes */
    public static final int IGNITION_INPUT_INDEX        = 99;

// ----------------------------------------------------------------------------
// Reserved status codes: [E0-00 through FF-FF]
// Groups:
//      0xF0..  - Generic
//      0xF1..  - Motion
//      0xF2..  - Geofence
//      0xF4..  - Digital input/output
//      0xF6..  - Sensor input
//      0xF7..  - Temperature input
//      0xF9..  - OBC/J1708
//      0xFD..  - Device status
// ----------------------------------------------------------------------------

    public static final int STATUS_IGNORE               = -1;

// ----------------------------------------------------------------------------
// Reserved: 0x0000 to 0x0FFF
// No status code: 0x0000

    public static final int STATUS_NONE                 = 0x0000;

// ----------------------------------------------------------------------------
// Available: 0x1000 to 0xCFFF
    public static final int STATUS_1000                 = 0x1000;   //  4096
    // ...
    public static final int STATUS_CFFF                 = 0xCFFF;   // 53247

// ----------------------------------------------------------------------------
// Reserved: 0xD000 to 0xEFFF

    // Reserved default event mapped codes (CalAmp)
    public static final int STATUS_E000                 = 0xE000;
    // ...
    public static final int STATUS_E0FF                 = 0xE0FF;

    // Garmin GFMI interface [0xE100 - 0xE1FF]
    public static final int STATUS_GFMI_CMD_03          = 0xE103;   // 57603 send non-ack message
    public static final int STATUS_GFMI_CMD_04          = 0xE104;   // 57604 send ack message
    public static final int STATUS_GFMI_CMD_05          = 0xE105;   // 57605 send answerable message
    public static final int STATUS_GFMI_CMD_06          = 0xE106;   // 57606 send stop location
    public static final int STATUS_GFMI_CMD_08          = 0xE108;   // 57608 request stop ETA
    public static final int STATUS_GFMI_CMD_09          = 0xE109;   // 57609 set auto arrival criteria
    // Garmin link
    public static final int STATUS_GFMI_LINK_OFF        = 0xE110;   // 57616 GFMI Link lost
    public static final int STATUS_GFMI_LINK_ON         = 0xE111;   // 57617 GFMI Link established
    // Garmin ACK
    public static final int STATUS_GFMI_ACK             = 0xE1A0;   // 57760 received ACK
    public static final int STATUS_GFMI_MESSAGE         = 0xE1B1;   // 57777 received message
    public static final int STATUS_GFMI_MESSAGE_ACK     = 0xE1B2;   // 57778 received message ACK
    // Garmin Stop Status change
    public static final int STATUS_GFMI_STOP_STATUS_1   = 0xE1C1;   // 57793 stop status
    public static final int STATUS_GFMI_STOP_STATUS_2   = 0xE1C2;   // 57794 stop status
    public static final int STATUS_GFMI_STOP_STATUS_3   = 0xE1C3;   // 57795 stop status
    public static final int STATUS_GFMI_STOP_STATUS_4   = 0xE1C4;   // 57796 stop status
    // Garmin ETA
    public static final int STATUS_GFMI_ETA             = 0xE1C9;   // 57801 ETA
    // Garmin Driver
    public static final int STATUS_GFMI_DRIVER_ID       = 0xE1D1;   // 57809 driver id
    public static final int STATUS_GFMI_DRIVER_STATUS   = 0xE1D3;   // 57811 driver status

// ----------------------------------------------------------------------------
// Generic codes: 0xF000 to 0xF0FF

    public static final int STATUS_INITIALIZED          = 0xF010;   // 61456
    // Description:
    //      General Status/Location information (event generated by some
    //      initialization function performed by the device).

    public static final int STATUS_LOCATION             = 0xF020;   // 61472
    public static final int STATUS_LOCATION_1           = 0xF021;   // 61473
    public static final int STATUS_LOCATION_2           = 0xF022;   // 61474
    // Description:
    //      General Status/Location event, typically based on a time-interval.  
    //      This status code indicates no more than just the location of the 
    //      device at a particular time.

    public static final int STATUS_LAST_LOCATION        = 0xF025;   // 61477
    // Description:
    //      General Status/Location event.  This status code indicates
    //      the last known location of the device (GPS may not be current)

    public static final int STATUS_CELL_LOCATION        = 0xF029;   // 61481
    // Description:
    //      Indicates a location generated by analyzing the MCC/MNC/LAC/CID
    //      information retrieve from the cell tower.

    public static final int STATUS_DISTANCE             = 0xF02C;   // 61484
    public static final int STATUS_DISTANCE_1           = 0xF02D;   // 61485
    public static final int STATUS_DISTANCE_2           = 0xF02E;   // 61486
    // Description:
    //      General Status/Location event, typically based on a distance-interval.  
    //      This status code indicates no more than just the location of the 
    //      device at a particular distance-interval.

    public static final int STATUS_WAYMARK_0            = 0xF030;   // 61488
    public static final int STATUS_WAYMARK_1            = 0xF031;   // 61489
    public static final int STATUS_WAYMARK_2            = 0xF032;   // 61490
    public static final int STATUS_WAYMARK_3            = 0xF033;   // 61491
    public static final int STATUS_WAYMARK_4            = 0xF034;   // 61492
    public static final int STATUS_WAYMARK_5            = 0xF035;   // 61493
    public static final int STATUS_WAYMARK_6            = 0xF036;   // 61494
    public static final int STATUS_WAYMARK_7            = 0xF037;   // 61495
    public static final int STATUS_WAYMARK_8            = 0xF038;   // 61496
    // Description:
    //      General Status/Location information (event generated by manual user
    //      intervention at the device. ie. By pressing a 'Waymark' button).

    public static final int STATUS_QUERY                = 0xF040;   // 61504
    // Description:
    //      General Status/Location information (event generated by 'query'
    //      from the server).

    public static final int STATUS_NOTIFY               = 0xF044;   // 61508
    // Description:
    //      General notification triggered by device operator

    public static final int STATUS_LOG                  = 0xF049;   // 61513
    // Description:
    //      General log indicator from device

    public static final int STATUS_SAMPLE_0             = 0xF050;   // 61520
    public static final int STATUS_SAMPLE_1             = 0xF051;   // 61521
    public static final int STATUS_SAMPLE_2             = 0xF052;   // 61522
    // Description:
    //      General sample triggered by device operator

    public static final int STATUS_HEARTBEAT            = 0xF060;   // 61536
    // Description:
    //      General heartbeat/keep-alive event

    public static final int STATUS_POI_0                = 0xF070;   // 61552
    public static final int STATUS_POI_1                = 0xF071;   // 61553
    public static final int STATUS_POI_2                = 0xF072;   // 61554
    public static final int STATUS_POI_3                = 0xF073;   // 61555
    public static final int STATUS_POI_4                = 0xF074;   // 61556
    // Description:
    //      General Point-Of-Interest location (event generated by manual user
    //      intervention at the device. ie. By pressing a 'Waymark' button).

    public static final int STATUS_DATA_MESSAGE_0       = 0xF080;   // 61568
    public static final int STATUS_DATA_MESSAGE_1       = 0xF081;   // 61569
    public static final int STATUS_DATA_MESSAGE_2       = 0xF082;   // 61570
    // Description:
    //      General data message from device

    public static final int STATUS_VOICE                = 0xF090;   // 61584
    // Description:
    //      General voice indicator

    public static final int STATUS_VOICE_OUTGOING       = 0xF094;   // 61588
    // Description:
    //      General outgoing voice indicator

    public static final int STATUS_VOICE_INCOMING       = 0xF098;   // 61592
    // Description:
    //      General outgoing voice indicator

    public static final int STATUS_RFID_TAG_0           = 0xF0A0;   // 61600
    public static final int STATUS_RFID_TAG_1           = 0xF0A1;   // 61601
    public static final int STATUS_RFID_TAG_2           = 0xF0A2;   // 61602
    // Description:
    //      General RFID events

// ----------------------------------------------------------------------------
// Motion codes: 0xF100 to 0xF1FF
// (motion related status codes - typically while ignition is on)

    public static final int STATUS_MOTION_START         = 0xF111;   // 61713
    // Description:
    //      Device start of motion

    public static final int STATUS_MOTION_IN_MOTION     = 0xF112;   // 61714
    public static final int STATUS_MOTION_EN_ROUTE      = 0xF112;   // 61714
    // Description:
    //      Device in-motion interval

    public static final int STATUS_MOTION_STOP          = 0xF113;   // 61715
    // Description:
    //      Device stopped motion

    public static final int STATUS_MOTION_DORMANT       = 0xF114;   // 61716
    // Description:
    //      Device dormant interval (ie. not moving)

    public static final int STATUS_MOTION_STOPPED       = 0xF115;   // 61717
    // Description:
    //      Alternate to Dormant (ie. not moving)

    public static final int STATUS_MOTION_IDLE          = 0xF116;   // 61718
    // Description:
    //      Device idle interval (ie. not moving, but engine may still be on)

    public static final int STATUS_MOTION_EXCESS_IDLE   = 0xF118;   // 61720
    // Description:
    //      Device exceeded idle threashold

    public static final int STATUS_MOTION_EXCESS_SPEED  = 0xF11A;   // 61722
    public static final int STATUS_MOTION_OVER_SPEED_1  = 0xF11A;   // 61722
    // Description:
    //      Device exceeded preset speed limit #1
    // OBSOLETE:
    //      Use STATUS_SPEEDING_LIMIT_1

    public static final int STATUS_MOTION_OVER_SPEED_2  = 0xF11B;   // 61723
    // Description:
    //      Device exceeded preset speed limit #2
    // OBSOLETE:
    //      Use STATUS_SPEEDING_LIMIT_2

    public static final int STATUS_MOTION_MOVING        = 0xF11C;   // 61724
    // Description:
    //      Device is moving
    // Notes:
    //      - This status code may be used to indicating that the device was moving
    //      at the time the event was generated. It is typically not associated
    //      with the status codes STATUS_MOTION_START, STATUS_MOTION_STOP, and  
    //      STATUS_MOTION_IN_MOTION, and may be used independently of these codes.
    //      - This status code is typically used for devices that need to periodically
    //      report that they are moving, apart from the standard start/stop/in-motion
    //      events.

    public static final int STATUS_MOTION_STOP_PENDING  = 0xF11D;   // 61725
    // Description:
    //      Motion changed to stop, waiting for official "Stop" event

    public static final int STATUS_MOTION_CHANGE        = 0xF11E;   // 61726
    // Description:
    //      Motion state change (from moving to stopped, or stopped to moving)

    public static final int STATUS_MOTION_HEADING       = 0xF11F;   // 61727
    // Description:
    //      Device motion changed direction/heading

    public static final int STATUS_MOTION_ACCELEROMETER = 0xF120;   // 61728
    // Description:
    //      Device motion detected due to accelerometer

    public static final int STATUS_MOTION_ACCELERATION  = 0xF123;   // 61731
    // Description:
    //      Device motion acceleration change

    public static final int STATUS_MOTION_DECELERATION  = 0xF126;   // 61734
    // Description:
    //      Device motion acceleration change

    public static final int STATUS_ODOM_0               = 0xF130;   // 61744
    public static final int STATUS_ODOM_1               = 0xF131;
    public static final int STATUS_ODOM_2               = 0xF132;
    public static final int STATUS_ODOM_3               = 0xF133;
    public static final int STATUS_ODOM_4               = 0xF134;
    public static final int STATUS_ODOM_5               = 0xF135;
    public static final int STATUS_ODOM_6               = 0xF136;
    public static final int STATUS_ODOM_7               = 0xF137;   // 61751
    // Description:
    //      Odometer value
    // Notes:
    //      The odometer limit is provided by property PROP_ODOMETER_#_LIMIT

    public static final int STATUS_ODOM_LIMIT_0         = 0xF140;   // 61760
    public static final int STATUS_ODOM_LIMIT_1         = 0xF141;
    public static final int STATUS_ODOM_LIMIT_2         = 0xF142;
    public static final int STATUS_ODOM_LIMIT_3         = 0xF143;
    public static final int STATUS_ODOM_LIMIT_4         = 0xF144;
    public static final int STATUS_ODOM_LIMIT_5         = 0xF145;
    public static final int STATUS_ODOM_LIMIT_6         = 0xF146;
    public static final int STATUS_ODOM_LIMIT_7         = 0xF147;   // 61767
    // Description:
    //      Odometer has exceeded a set limit

    public static final int STATUS_SPEEDING_BEGIN       = 0xF160;   // 61792
    public static final int STATUS_SPEEDING_LIMIT_1     = 0xF161;   // 61793
    public static final int STATUS_SPEEDING_LIMIT_2     = 0xF162;   // 61794
    public static final int STATUS_SPEEDING_LIMIT_3     = 0xF163;   // 61795
    public static final int STATUS_SPEEDING_LIMIT_4     = 0xF164;   // 61796
    public static final int STATUS_SPEEDING_END         = 0xF16F;   // 61807
    // Description:
    //      Speeding profile

    public static final int STATUS_TRACK_START          = 0xF181;   // 61825
    // Description:
    //      Tracking start

    public static final int STATUS_TRACK_LOCATION       = 0xF182;   // 61826
    // Description:
    //      Periodic location event while tracking

    public static final int STATUS_TRACK_STOP           = 0xF183;   // 61827
    // Description:
    //      Tracking stop

// ----------------------------------------------------------------------------
// Geofence: 0xF200 to 0xF2FF

    public static final int STATUS_GEOFENCE_ARRIVE      = 0xF210;   // 61968
    // Description:
    //      Device arrived at geofence/geozone

    public static final int STATUS_CORRIDOR_ARRIVE      = 0xF213;   // 61971
    // Description:
    //      Device entered GeoCorridor

    public static final int STATUS_JOB_ARRIVE           = 0xF215;   // 61973
    // Description:
    //      Device arrived at job-site (typically driver entered)

    public static final int STATUS_GEOFENCE_DEPART      = 0xF230;   // 62000
    // Description:
    //      Device departed geofence/geozone

    public static final int STATUS_CORRIDOR_DEPART      = 0xF233;   // 62003
    // Description:
    //      Device exited GeoCorridor (alternate for STATUS_CORRIDOR_VIOLATION)

    public static final int STATUS_JOB_DEPART           = 0xF235;   // 62005
    // Description:
    //      Device departed job-site (typically driver entered)

    public static final int STATUS_GEOFENCE_VIOLATION   = 0xF250;   // 62032
    // Description:
    //      Geofence violation

    public static final int STATUS_CORRIDOR_VIOLATION   = 0xF258;   // 62040
    // Description:
    //      GeoCorridor violation

    public static final int STATUS_GEOFENCE_ACTIVE      = 0xF270;   // 62064
    // Description:
    //      Geofence now active

    public static final int STATUS_CORRIDOR_ACTIVE      = 0xF278;   // 62072
    // Description:
    //      GeoCorridor now active

    public static final int STATUS_GEOFENCE_INACTIVE    = 0xF280;   // 62080
    // Description:
    //      Geofence now inactive

    public static final int STATUS_CORRIDOR_INACTIVE    = 0xF288;   // 62088
    // Description:
    //      Geofence now inactive

    public static final int STATUS_GEOBOUNDS_ENTER      = 0xF2A0;   // 62112
    // Description:
    //      Device has entered a state/boundary

    public static final int STATUS_GEOBOUNDS_EXIT       = 0xF2B0;   // 62128
    // Description:
    //      Device has exited a state/boundary

    public static final int STATUS_PARKED               = 0xF2C0;   // 62144
    // Description:
    //      Device has parked

    public static final int STATUS_EXCESS_PARK          = 0xF2C3;   // 62147
    // Description:
    //      Device has exceeded a parked threashold

    public static final int STATUS_UNPARKED             = 0xF2C6;   // 62150
    // Description:
    //      Device has unparked

// ----------------------------------------------------------------------------
// Digital input/output (state change): 0xF400 to 0xF4FF

    public static final int STATUS_INPUT_STATE          = 0xF400;   // 62464
    // Description:
    //      Current input state change

    public static final int STATUS_IGNITION_ON          = 0xF401;   // 62465
    // Description:
    //      Ignition turned ON
    // Notes:
    //      - This status code may be used to indicate that the ignition input
    //      turned ON.

    public static final int STATUS_INPUT_ON             = 0xF402;   // 62466
    // Description:
    //      Input turned ON

    public static final int STATUS_IGNITION_OFF         = 0xF403;   // 62467
    // Description:
    //      Ignition turned OFF
    // Notes:
    //      - This status code may be used to indicate that the ignition input
    //      turned OFF.

    public static final int STATUS_INPUT_OFF            = 0xF404;   // 62468
    // Description:
    //      Input turned OFF
    // Notes:
    //      - This status code may be used to indicate that an arbitrary input
    //      'thing' turned OFF, and the 'thing' can be identified by the 'Input ID'.
    //      This 'ID' can also represent the index of a digital input.

    public static final int STATUS_OUTPUT_STATE         = 0xF406;   // 62470
    // Description:
    //      Current output ON state (bitmask)

    public static final int STATUS_OUTPUT_ON            = 0xF408;   // 62472
    // Description:
    //      Output turned ON
    // Notes:
    //      - This status code may be used to indicate that an arbitrary output
    //      'thing' turned ON, and the 'thing' can be identified by the 'Output ID'.
    //      This 'ID' can also represent the index of a digital output.

    public static final int STATUS_OUTPUT_OFF           = 0xF40A;   // 62474
    // Description:
    //      Output turned OFF
    // Notes:
    //      - This status code may be used to indicate that an arbitrary output
    //      'thing' turned OFF, and the 'thing' can be identified by the 'Output ID'.
    //      This 'ID' can also represent the index of a digital output.

    public static final int STATUS_ENGINE_START         = 0xF40C;   // 62476
    // Description:
    //      Engine started
    // Notes:
    //      - This status code may be used to specifically indicate that the engine
    //      has been started.  This may be different from "Ignition On", if the
    //      STATUS_IGNITION_ON also indicates when only the "accessory" loop of
    //      the ignition state is on.

    public static final int STATUS_ENGINE_STOP          = 0xF40D;   // 62477
    // Description:
    //      Engine stopped
    // Notes:
    //      - This status code may be used to specifically indicate that the engine
    //      has been stopped.  This may be different from "Ignition Off", if the
    //      STATUS_IGNITION_OFF also indicates when only the "accessory" loop of
    //      the ignition state is off.

    public static final int STATUS_INPUT_ON_00          = 0xF420;   // 62496
    public static final int STATUS_INPUT_ON_01          = 0xF421;   // 62497
    public static final int STATUS_INPUT_ON_02          = 0xF422;   // 62498
    public static final int STATUS_INPUT_ON_03          = 0xF423;   // 62499
    public static final int STATUS_INPUT_ON_04          = 0xF424;   // 62500
    public static final int STATUS_INPUT_ON_05          = 0xF425;   // 62501
    public static final int STATUS_INPUT_ON_06          = 0xF426;   // 62502
    public static final int STATUS_INPUT_ON_07          = 0xF427;   // 62503
    public static final int STATUS_INPUT_ON_08          = 0xF428;   // 62504
    public static final int STATUS_INPUT_ON_09          = 0xF429;   // 62505
    public static final int STATUS_INPUT_ON_10          = 0xF42A;   // 62406
    public static final int STATUS_INPUT_ON_11          = 0xF42B;   // 62407
    public static final int STATUS_INPUT_ON_12          = 0xF42C;   // 62408
    public static final int STATUS_INPUT_ON_13          = 0xF42D;   // 62409
    public static final int STATUS_INPUT_ON_14          = 0xF42E;   // 62510
    public static final int STATUS_INPUT_ON_15          = 0xF42F;   // 62511
    // Description:
    //      Digital input state changed to ON

    public static final int STATUS_DOOR_OPEN_0          = 0xF434;   // 62516
    public static final int STATUS_DOOR_OPEN_1          = 0xF435;   // 62517
    public static final int STATUS_DOOR_OPEN_2          = 0xF436;   // 62518
    // Description:
    //      Door open
    // Notes:
    //      - This status code may be used to specifically indicate that a door
    //      has been opened.  (alternatively a STATUS_INPUT_ON_xx status code
    //      could be used, with the description changed to "door open").

    public static final int STATUS_SEATBELT_ON_0        = 0xF438;   // 62520
    public static final int STATUS_SEATBELT_ON_1        = 0xF439;   // 62521
    // Description:
    //      Setbelt on/latched
    // Notes:
    //      - This status code may be used to specifically indicate that a seatbelt
    //      has been latched.  (alternatively a STATUS_INPUT_ON_xx status code
    //      could be used, with the description changed to "setbelt on").

    public static final int STATUS_INPUT_OFF_00         = 0xF440;   // 62528
    public static final int STATUS_INPUT_OFF_01         = 0xF441;   // 62529
    public static final int STATUS_INPUT_OFF_02         = 0xF442;   // 62530
    public static final int STATUS_INPUT_OFF_03         = 0xF443;   // 62531
    public static final int STATUS_INPUT_OFF_04         = 0xF444;   // 62532
    public static final int STATUS_INPUT_OFF_05         = 0xF445;   // 62533
    public static final int STATUS_INPUT_OFF_06         = 0xF446;   // 62534
    public static final int STATUS_INPUT_OFF_07         = 0xF447;   // 62535
    public static final int STATUS_INPUT_OFF_08         = 0xF448;   // 62536
    public static final int STATUS_INPUT_OFF_09         = 0xF449;   // 62537
    public static final int STATUS_INPUT_OFF_10         = 0xF44A;   // 62538
    public static final int STATUS_INPUT_OFF_11         = 0xF44B;   // 62539
    public static final int STATUS_INPUT_OFF_12         = 0xF44C;   // 62540
    public static final int STATUS_INPUT_OFF_13         = 0xF44D;   // 62541
    public static final int STATUS_INPUT_OFF_14         = 0xF44E;   // 62542
    public static final int STATUS_INPUT_OFF_15         = 0xF44F;   // 62543
    // Description:
    //      Digital input state changed to OFF

    public static final int STATUS_DOOR_CLOSE_0         = 0xF454;   // 62548
    public static final int STATUS_DOOR_CLOSE_1         = 0xF455;   // 62549
    public static final int STATUS_DOOR_CLOSE_2         = 0xF456;   // 62550
    // Description:
    //      Door closed
    // Notes:
    //      - This status code may be used to specifically indicate that a door
    //      has been closed.  (alternatively a STATUS_INPUT_ON_xx status code
    //      could be used, with the description changed to "door closed").

    public static final int STATUS_SEATBELT_OFF_0       = 0xF458;   // 62552
    public static final int STATUS_SEATBELT_OFF_1       = 0xF459;   // 62553
    // Description:
    //      Setbelt on/latched
    // Notes:
    //      - This status code may be used to specifically indicate that a seatbelt
    //      has been latched.  (alternatively a STATUS_INPUT_ON_xx status code
    //      could be used, with the description changed to "setbelt on").

    public static final int STATUS_OUTPUT_ON_00         = 0xF460;   // 62560
    public static final int STATUS_OUTPUT_ON_01         = 0xF461;
    public static final int STATUS_OUTPUT_ON_02         = 0xF462;
    public static final int STATUS_OUTPUT_ON_03         = 0xF463;
    public static final int STATUS_OUTPUT_ON_04         = 0xF464;
    public static final int STATUS_OUTPUT_ON_05         = 0xF465;
    public static final int STATUS_OUTPUT_ON_06         = 0xF466;
    public static final int STATUS_OUTPUT_ON_07         = 0xF467;
    public static final int STATUS_OUTPUT_ON_08         = 0xF468;
    public static final int STATUS_OUTPUT_ON_09         = 0xF469;   // 62569
    // Description:
    //      Digital output state set to ON
    //      0xFA6A through 0xFA6F reserved

    public static final int STATUS_OUTPUT_OFF_00        = 0xF480;   // 62592
    public static final int STATUS_OUTPUT_OFF_01        = 0xF481;
    public static final int STATUS_OUTPUT_OFF_02        = 0xF482;
    public static final int STATUS_OUTPUT_OFF_03        = 0xF483;
    public static final int STATUS_OUTPUT_OFF_04        = 0xF484;
    public static final int STATUS_OUTPUT_OFF_05        = 0xF485;
    public static final int STATUS_OUTPUT_OFF_06        = 0xF486;
    public static final int STATUS_OUTPUT_OFF_07        = 0xF487;
    public static final int STATUS_OUTPUT_OFF_08        = 0xF488;
    public static final int STATUS_OUTPUT_OFF_09        = 0xF489;   // 62601
    // Description:
    //      Digital output state set to OFF
    //      0xFA8A through 0xFA8F reserved

    public static final int STATUS_ELAPSED_00           = 0xF4A0;   // 62624
    public static final int STATUS_ELAPSED_01           = 0xF4A1;
    public static final int STATUS_ELAPSED_02           = 0xF4A2;
    public static final int STATUS_ELAPSED_03           = 0xF4A3;
    public static final int STATUS_ELAPSED_04           = 0xF4A4;
    public static final int STATUS_ELAPSED_05           = 0xF4A5;
    public static final int STATUS_ELAPSED_06           = 0xF4A6;
    public static final int STATUS_ELAPSED_07           = 0xF4A7;   // 62631
    // Description:
    //      Elapsed time
    //      0xFAA8 through 0xFAAF reserved

    public static final int STATUS_ELAPSED_LIMIT_00     = 0xF4B0;   // 62640
    public static final int STATUS_ELAPSED_LIMIT_01     = 0xF4B1;   // 62641
    public static final int STATUS_ELAPSED_LIMIT_02     = 0xF4B2;   // 62642
    public static final int STATUS_ELAPSED_LIMIT_03     = 0xF4B3;   // 62643
    public static final int STATUS_ELAPSED_LIMIT_04     = 0xF4B4;   // 62644
    public static final int STATUS_ELAPSED_LIMIT_05     = 0xF4B5;   // 62645
    public static final int STATUS_ELAPSED_LIMIT_06     = 0xF4B6;   // 62646
    public static final int STATUS_ELAPSED_LIMIT_07     = 0xF4B7;   // 62647
    // Description:
    //      Elapsed timer has exceeded a set limit
    //      0xFAB8 through 0xFABF reserved

// ----------------------------------------------------------------------------
// Analog/etc sensor values (extra data): 0xF600 to 0xF6FF

    public static final int STATUS_ANALOG_0             = 0xF600;   // 62976
    public static final int STATUS_ANALOG_1             = 0xF601;
    public static final int STATUS_ANALOG_2             = 0xF602;
    public static final int STATUS_ANALOG_3             = 0xF603;
    public static final int STATUS_ANALOG_4             = 0xF604;
    public static final int STATUS_ANALOG_5             = 0xF605;
    public static final int STATUS_ANALOG_6             = 0xF606;
    public static final int STATUS_ANALOG_7             = 0xF607;   // 62983
    // Description:
    //      Analog sensor value
    // Notes:
    //      - Client should include an Analog value in the event packet.

    public static final int STATUS_ANALOG_RANGE_0       = 0xF620;   // 63008
    public static final int STATUS_ANALOG_RANGE_1       = 0xF621;
    public static final int STATUS_ANALOG_RANGE_2       = 0xF622;
    public static final int STATUS_ANALOG_RANGE_3       = 0xF623;
    public static final int STATUS_ANALOG_RANGE_4       = 0xF624;
    public static final int STATUS_ANALOG_RANGE_5       = 0xF625;
    public static final int STATUS_ANALOG_RANGE_6       = 0xF626;
    public static final int STATUS_ANALOG_RANGE_7       = 0xF627;   // 63015
    // Description:
    //      Analog sensor value out-of-range violation
    // Notes:
    //      - Client should include an Analog value in the event packet.

// ----------------------------------------------------------------------------
// Temperature sensor values (extra data): 0xF700 to 0xF7FF

    public static final int STATUS_TEMPERATURE_0        = 0xF710;   // 63248
    public static final int STATUS_TEMPERATURE_1        = 0xF711;
    public static final int STATUS_TEMPERATURE_2        = 0xF712;
    public static final int STATUS_TEMPERATURE_3        = 0xF713;
    public static final int STATUS_TEMPERATURE_4        = 0xF714;
    public static final int STATUS_TEMPERATURE_5        = 0xF715;
    public static final int STATUS_TEMPERATURE_6        = 0xF716;
    public static final int STATUS_TEMPERATURE_7        = 0xF717;   // 63255
    // Description:
    //      Temperature value

    public static final int STATUS_TEMPERATURE_RANGE_0  = 0xF730;   // 63280
    public static final int STATUS_TEMPERATURE_RANGE_1  = 0xF731;
    public static final int STATUS_TEMPERATURE_RANGE_2  = 0xF732;
    public static final int STATUS_TEMPERATURE_RANGE_3  = 0xF733;
    public static final int STATUS_TEMPERATURE_RANGE_4  = 0xF734;
    public static final int STATUS_TEMPERATURE_RANGE_5  = 0xF735;
    public static final int STATUS_TEMPERATURE_RANGE_6  = 0xF736;
    public static final int STATUS_TEMPERATURE_RANGE_7  = 0xF737;   // 63287
    // Description:
    //      Temperature value out-of-range [low/high/average]

    public static final int STATUS_TEMPERATURE_LOW_0    = 0xF740;   // 63296
    // Description:
    //      Temperature drop below lower range

    public static final int STATUS_TEMPERATURE_HIGH_0   = 0xF750;   // 63312
    // Description:
    //      Temperature exceeded higher range

    public static final int STATUS_TEMPERATURE_OK       = 0xF7F0;   // 63472
    // Description:
    //      All temperature averages [aver/aver/aver/...]

    public static final int STATUS_TEMPERATURE          = 0xF7F1;   // 63473
    // Description:
    //      All temperature averages [aver/aver/aver/...]

// ----------------------------------------------------------------------------
// Miscellaneous

    public static final int STATUS_LOGIN                = 0xF811;   // 63505
    // Description:
    //      Generic 'login'

    public static final int STATUS_LOGOUT               = 0xF812;   // 63506
    // Description:
    //      Generic 'logout'

    // --------------------------------

    public static final int STATUS_ARMED                = 0xF817;   // 63511
    // Description:
    //      Generic 'armed'

    public static final int STATUS_DISARMED             = 0xF818;   // 63512
    // Description:
    //      Generic 'disarmed'
    
    // --------------------------------

    public static final int STATUS_ENTITY_STATE         = 0xF820;   // 63520
    // Description:
    //      General Entity state

    public static final int STATUS_ENTITY_CONNECT       = 0xF821;   // 63521
    // Description:
    //      General Entity Connect/Hook/On

    public static final int STATUS_ENTITY_DISCONNECT    = 0xF822;   // 63522
    // Description:
    //      General Entity Disconnect/Drop/Off

    public static final int STATUS_ENTITY_INVENTORY     = 0xF823;   // 63523
    // Description:
    //      General Entity Inventory

    // --------------------------------

    public static final int STATUS_TRAILER_STATE        = 0xF824;   // 63524
    // Description:
    //      Trailer State change

    public static final int STATUS_TRAILER_HOOK         = 0xF825;   // 63525
    // Description:
    //      Trailer Hook

    public static final int STATUS_TRAILER_UNHOOK       = 0xF826;   // 63526
    // Description:
    //      Trailer Unhook

    public static final int STATUS_TRAILER_INVENTORY    = 0xF827;   // 63527
    // Description:
    //      Trailer Inventory

    // --------------------------------

    public static final int STATUS_RFID_STATE           = 0xF828;   // 63528
    // Description:
    //      RFID State change

    public static final int STATUS_RFID_CONNECT         = 0xF829;   // 63529
    // Description:
    //      RFID Connect/InRange

    public static final int STATUS_RFID_DISCONNECT      = 0xF82A;   // 63530
    // Description:
    //      RFID Disconnect/OutOfRange

    public static final int STATUS_RFID_INVENTORY       = 0xF82B;   // 63531
    // Description:
    //      RFID Inventory
    
    // --------------------------------

    public static final int STATUS_PERSON_ENTER         = 0xF82C;   // 63532
    // Description:
    //      Passenger/Person Enter/Embark

    public static final int STATUS_PERSON_EXIT          = 0xF82D;   // 63533
    // Description:
    //      Passenger/Person Exit/Disembark

    public static final int STATUS_PERSON_INVENTORY     = 0xF82E;   // 63534
    // Description:
    //      Passenger/Person Inventory

    public static final int STATUS_PERSON_STATE         = 0xF82F;   // 63535
    // Description:
    //      Passenger/Person State change

    // --------------------------------

    public static final int STATUS_ACK                  = 0xF831;   // 63537
    // Description:
    //      Acknowledge

    public static final int STATUS_NAK                  = 0xF832;   // 63538
    // Description:
    //      Negative Acknowledge

    // --------------------------------

    public static final int STATUS_COMMAND_ACK          = 0xF833;   // 63539
    // Description:
    //      General Command Acknowledge

    public static final int STATUS_COMMAND_NAK          = 0xF834;   // 63540
    // Description:
    //      General Command Negative Acknowledge

    // --------------------------------

    public static final int STATUS_DUTY_ON              = 0xF837;   // 63543
    // Description:
    //      Duty condition activated

    public static final int STATUS_DUTY_OFF             = 0xF838;   // 63544
    // Description:
    //      Duty condition deactivated
    
    // --------------------------------

    public static final int STATUS_PANIC_ON             = 0xF841;   // 63553
    // Description:
    //      Panic/SOS condition activated

    public static final int STATUS_PANIC_OFF            = 0xF842;   // 63554
    // Description:
    //      Panic/SOS condition deactivated
    
    // --------------------------------

    public static final int STATUS_ALARM_ON             = 0xF847;   // 63559
    // Description:
    //      General Alarm condition activated

    public static final int STATUS_ALARM_OFF            = 0xF848;   // 63560
    // Description:
    //      General Alarm condition deactivated

    // --------------------------------

    public static final int STATUS_ASSIST_ON            = 0xF851;   // 63569
    // Description:
    //      Assist condition activated

    public static final int STATUS_ASSIST_OFF           = 0xF852;   // 63570
    // Description:
    //      Assist condition deactivated

    // --------------------------------

    public static final int STATUS_MANDOWN_ON           = 0xF855;   // 63573
    // Description:
    //      man-down activated

    public static final int STATUS_MANDOWN_OFF          = 0xF856;   // 63574
    // Description:
    //      man-down deactivated

    // --------------------------------

    public static final int STATUS_MEDICAL_ON           = 0xF861;   // 63585
    // Description:
    //      Medical Call condition activated

    public static final int STATUS_MEDICAL_OFF          = 0xF862;   // 63586
    // Description:
    //      Medical Call condition deactivated

    // --------------------------------

    public static final int STATUS_DRIVER_FATIGUE       = 0xF868;   // 63592
    // Description:
    //      driver fatigued

    public static final int STATUS_DRIVER_SLEEP         = 0xF869;   // 63593
    // Description:
    //      driver sleep

    public static final int STATUS_DRIVER_WAKE          = 0xF86A;   // 63594
    // Description:
    //      driver wake

    // --------------------------------

    public static final int STATUS_TOWING_START         = 0xF871;   // 63601
    // Description:
    //      Vehicle started to be towed

    public static final int STATUS_TOWING_STOP          = 0xF872;   // 63602
    // Description:
    //      Vehicle stopped being towed

    // --------------------------------

    public static final int STATUS_INTRUSION_ON         = 0xF881;   // 63617
    // Description:
    //      Intrusion detected

    public static final int STATUS_INTRUSION_OFF        = 0xF882;   // 63618
    // Description:
    //      Intrusion aborted

    // --------------------------------

    public static final int STATUS_TAMPER_ON            = 0xF885;   // 63621
    // Description:
    //      Tamper detected

    public static final int STATUS_TAMPER_OFF           = 0xF886;   // 63622
    // Description:
    //      Tamper aborted

    // --------------------------------

    public static final int STATUS_BREACH_ON            = 0xF889;   // 63625
    // Description:
    //      Breach detected

    public static final int STATUS_BREACH_OFF           = 0xF88A;   // 63626
    // Description:
    //      Breach aborted

    // --------------------------------

    public static final int STATUS_VIBRATION_ON         = 0xF891;   // 63633
    // Description:
    //      Vibration on

    public static final int STATUS_VIBRATION_OFF        = 0xF892;   // 63634
    // Description:
    //      Vibration off

    // --------------------------------

    public static final int STATUS_DOOR_LOCK            = 0xF895;   // 63637
    // Description:
    //      Door lock

    public static final int STATUS_DOOR_UNLOCK          = 0xF896;   // 63638
    // Description:
    //      Door unlock

    // --------------------------------

    public static final int STATUS_PTO_ON               = 0xF899;   // 63641
    // Description:
    //      PTO on

    public static final int STATUS_PTO_OFF              = 0xF89A;   // 63642
    // Description:
    //      PTO off

    // --------------------------------

    public static final int STATUS_ONLINE               = 0xF89D;   // 63645
    // Description:
    //      Online

    public static final int STATUS_OFFLINE              = 0xF89E;   // 63646
    // Description:
    //      Offline

// ----------------------------------------------------------------------------
// Carjacking status: 0xF8B0 to 0xF8BF

    public static final int STATUS_CARJACK_DISABLED     = 0xF8B0;   // 63664 [01]
    // Description:
    //      Cajack detection disabled

    public static final int STATUS_CARJACK_ENABLED      = 0xF8B1;   // 63665
    // Description:
    //      Cajack detection enabled

    public static final int STATUS_CARJACK_STANDBY      = 0xF8B2;   // 63666
    // Description:
    //      Cajack detection stand-by

    public static final int STATUS_CARJACK_ARMED        = 0xF8B3;   // 63667 [02]
    // Description:
    //      Cajack detection armed - possible carjack in progress

    public static final int STATUS_CARJACK_TRIGGERED    = 0xF8B5;   // 63669 [03]
    // Description:
    //      Cajack triggered - carjack in progress

    public static final int STATUS_CARJACK_CANCELLED    = 0xF8B7;   // 63671 [10]
  //public static final int STATUS_CARJACK_CANCELLED    = 0xF8B8;   // [20]
  //public static final int STATUS_CARJACK_CANCELLED    = 0xF8BA;   // [30]
  //public static final int STATUS_CARJACK_CANCELLED    = 0xF8BC;   // [40]
    // Description:
    //      Cajack action cancelled

// ----------------------------------------------------------------------------
// Entity status: 0xF8E0 to 0xF8FF

// ----------------------------------------------------------------------------
// OBC/J1708 status: 0xF900 to 0xF9FF

    public static final int STATUS_OBD_INFO_0           = 0xF900;   // 
    public static final int STATUS_OBD_INFO_1           = 0xF901;
    public static final int STATUS_OBD_INFO_2           = 0xF902;
    public static final int STATUS_OBD_INFO_3           = 0xF903;
    public static final int STATUS_OBD_INFO_4           = 0xF904;   // 
    public static final int STATUS_OBD_INFO_5           = 0xF905;   // 
    public static final int STATUS_OBD_INFO_6           = 0xF906;   // 
    public static final int STATUS_OBD_INFO_7           = 0xF907;   // 
    // Description:
    //      OBC/J1708 information packet

    public static final int STATUS_OBD_DISCONNECT       = 0xF910;   // 63760
    // Description:
    //      OBD disconnected from vehicle

    public static final int STATUS_OBD_FAULT            = 0xF911;   // 63761
    // Description:
    //      OBC/J1708 fault code occurred.

    public static final int STATUS_CHECK_ENGINE         = 0xF915;   // 
    // Description:
    //      Malfunction Indicator Lamp (MIL)

    public static final int STATUS_OBD_RANGE            = 0xF920;   // 
    // Description:
    //      Generic OBC/J1708 value out-of-range

    public static final int STATUS_OBD_RPM_RANGE        = 0xF922;   // 63778
    // Description:
    //      OBC/J1708 RPM out-of-range

    public static final int STATUS_OBD_FUEL_RANGE       = 0xF924;   // 63780
    // Description:
    //      OBC/J1708 Fuel level out-of-range (ie. to low)
    // Notes:
    //      - This code can also be used to indicate possible fuel theft.

    public static final int STATUS_OBD_OIL_RANGE        = 0xF926;   // 63782
    // Description:
    //      OBC/J1708 Oil level out-of-range (ie. to low)

    public static final int STATUS_OBD_TEMP_RANGE       = 0xF928;   // 63784
    // Description:
    //      OBC/J1708 Temperature out-of-range

    public static final int STATUS_OBD_LOAD_RANGE       = 0xF92A;   // 63786
    // Description:
    //      OBC/J1708 Engine-Load out-of-range

    public static final int STATUS_OBD_COOLANT_RANGE    = 0xF92C;   // 63788
    // Description:
    //      OBC/J1708 Engine coolent level out-of-range

    public static final int STATUS_EXCESS_BRAKING       = 0xF930;   // 63792
    public static final int STATUS_EXCESS_BRAKING_2     = 0xF931;   // 63793
    public static final int STATUS_EXCESS_BRAKING_3     = 0xF932;   // 63794
    // Description:
    //      Excessive/Harsh deceleration detected

    public static final int STATUS_EXCESS_CORNERING     = 0xF937;   // 63799
    public static final int STATUS_EXCESS_CORNERING_2   = 0xF938;   // 63800
    public static final int STATUS_EXCESS_CORNERING_3   = 0xF939;   // 63801
    // Description:
    //      Excessive lateral acceleration detected
    //      Also called "hard turning", "veer alarm", "lateral acceleration"

    public static final int STATUS_IMPACT               = 0xF941;   // 63809
    // Description:
    //      Excessive acceleration/deceleration detected

    public static final int STATUS_FREEFALL             = 0xF945;   // 63813
    // Description:
    //      Freefall detected

    public static final int STATUS_FUEL_REFILL          = 0xF951;   // 63825
    // Description:
    //      Fuel refill detected

    public static final int STATUS_FUEL_THEFT           = 0xF952;   // 63826
    // Description:
    //      Fuel theft detected

    public static final int STATUS_LOW_FUEL             = 0xF954;   // 63828
    // Description:
    //      Low fuel alert

    public static final int STATUS_FUEL_DIRTY           = 0xF95A;   // 63834
    // Description:
    //      Fuel contaminated/dirty

    public static final int STATUS_FUEL_SENSOR          = 0xF95E;   // 63838
    // Description:
    //      Fuel sensor failed/bad

    public static final int STATUS_EXCESS_ACCEL         = 0xF960;   // 63840
    public static final int STATUS_EXCESS_ACCEL_2       = 0xF961;   // 63841
    public static final int STATUS_EXCESS_ACCEL_3       = 0xF962;   // 63842
    // Description:
    //      Excessive acceleration detected

// ----------------------------------------------------------------------------
// Device custom status

    public static final int STATUS_DAY_SUMMARY          = 0xFA00;   // 64000
    // Description:
    //      End-Of-Day Summary

    public static final int STATUS_TRIP_SUMMARY         = 0xFA40;   // 64064
    // Description:
    //      End-Of-Day Summary

// ----------------------------------------------------------------------------
// Tire Pressure/Temperature

    public static final int STATUS_TIRE_TEMP_RANGE      = 0xFBA0;   // 64416
    // Description:
    //      Tire Temperature out-of-range

    public static final int STATUS_TIRE_PRESSURE_RANGE  = 0xFBB0;   // 64432
    // Description:
    //      Tire Pressure out-of-range

    public static final int STATUS_TIRE_PRESSURE_LOW    = 0xFBC0;   // 64448
    // Description:
    //      Tire Pressure low

    public static final int STATUS_TIRE_BATTERY_LOW     = 0xFBD0;   // 64464
    // Description:
    //      Tire sensor battery low

// ----------------------------------------------------------------------------
// Internal device status

    public static final int STATUS_IP_ADDRESS           = 0xFD01;   // 64769
    // Description:
    //      IP Address changed

    public static final int STATUS_SIM_CARD             = 0xFD03;   // 64771
    // Description:
    //      SIM Card changed

    public static final int STATUS_BATTERY_VOLTS        = 0xFD0A;   // 64778
    // Description:
    //      Battery voltage

    public static final int STATUS_BACKUP_VOLTS         = 0xFD0C;   // 64780
    // Description:
    //      Backup Battery voltage

    public static final int STATUS_BATT_CHARGE_ON       = 0xFD0E;   // 64782
    // Description:
    //      Battery charging on

    public static final int STATUS_BATT_CHARGE_OFF      = 0xFD0F;   // 64783
    // Description:
    //      Battery charging off

    public static final int STATUS_LOW_BATTERY          = 0xFD10;   // 64784
    // Description:
    //      Low battery indicator

    public static final int STATUS_BATTERY_LEVEL        = 0xFD11;   // 64785
    // Description:
    //      Battery indicator

    public static final int STATUS_POWER_FAILURE        = 0xFD13;   // 64787
    // Description:
    //      Power failure indicator (or running on internal battery)

    public static final int STATUS_POWER_ALARM          = 0xFD14;   // 64788
    // Description:
    //      Power alarm condition

    public static final int STATUS_POWER_RESTORED       = 0xFD15;   // 64789
    // Description:
    //      Power restored (after previous failure)

    public static final int STATUS_POWER_OFF            = 0xFD17;   // 64791
    // Description:
    //      Power failure indicator (or running on internal battery)

    public static final int STATUS_POWER_ON             = 0xFD19;   // 64793
    // Description:
    //      Power restored (after previous failure)

    public static final int STATUS_GPS_EXPIRED          = 0xFD21;   // 64801
    // Description:
    //      GPS fix expiration detected

    public static final int STATUS_GPS_FAILURE          = 0xFD22;   // 64802
    // Description:
    //      GPS receiver failure detected

    public static final int STATUS_GPS_ANTENNA_OPEN     = 0xFD23;   // 64803
    // Description:
    //      GPS antenna open detected

    public static final int STATUS_GPS_ANTENNA_SHORT    = 0xFD24;   // 64804
    // Description:
    //      GPS antenna open detected

    public static final int STATUS_GPS_JAMMING          = 0xFD25;   // 64805
    // Description:
    //      GPS receiver jamming detected

    public static final int STATUS_GPS_RESTORED         = 0xFD26;   // 64806
    // Description:
    //      GPS receiver restore detected

    public static final int STATUS_GPS_LOST             = 0xFD27;   // 64807
    // Description:
    //      GPS receiver unable to obtain fix

    public static final int STATUS_DIAGNOSTIC           = 0xFD30;   // 64816
    // Description:
    //      General Diagnostic message

    public static final int STATUS_CONNECTION_FAILURE   = 0xFD31;   // 64817
    // Description:
    //      Modem/GPRS/CDMA Connection failure detected

    public static final int STATUS_CONNECTION_RESTORED  = 0xFD32;   // 64818
    // Description:
    //      Modem/GPRS/CDMA Connection restore detected

    public static final int STATUS_MODEM_FAILURE        = 0xFD33;   // 64819
    // Description:
    //      Modem failure detected

    public static final int STATUS_INTERNAL_FAILURE     = 0xFD35;   // 64821
    // Description:
    //      Internal failure detected

    public static final int STATUS_MODEM_JAMMING        = 0xFD39;   // 64825
    // Description:
    //      Modem detected jamming

    public static final int STATUS_MODEM_RESTORED       = 0xFD3A;   // 64826
    // Description:
    //      Modem no longer jamming

    public static final int STATUS_CONFIG_RESET         = 0xFD41;   // 64833
    // Description:
    //      Configuration reset

    public static final int STATUS_CONFIG_START         = 0xFD42;   // 64834
    // Description:
    //      Configuration starting

    public static final int STATUS_CONFIG_COMPLETE      = 0xFD43;   // 64835
    // Description:
    //      Configuration complete/finished

    public static final int STATUS_CONFIG_FAILED        = 0xFD44;   // 64836
    // Description:
    //      Configuration failed

    public static final int STATUS_SHUTDOWN             = 0xFD45;   // 64837
    // Description:
    //      device shutdown

    public static final int STATUS_SHUTDOWN_CANCELLED   = 0xFD47;   // 64839
    // Description:
    //      device shutdown

    public static final int STATUS_SUSPEND              = 0xFD48;   // 64840
    // Description:
    //      device sleep/suspend

    public static final int STATUS_RESUME               = 0xFD4A;   // 64842
    // Description:
    //      device resume

    public static final int STATUS_ROAMING_ON           = 0xFD51;   // 64849
    // Description:
    //      modem roaming ON

    public static final int STATUS_ROAMING_OFF          = 0xFD52;   // 64850
    // Description:
    //      modem roaming OFF

    public static final int STATUS_ROAMING_UNKNOWN      = 0xFD53;   // 64851
    // Description:
    //      modem roaming unknown

// ----------------------------------------------------------------------------
// General image attachments

    public static final int STATUS_IMAGE_0              = 0xFD60;   // 64864
    public static final int STATUS_IMAGE_1              = 0xFD61;   // 64865
    public static final int STATUS_IMAGE_2              = 0xFD62;   // 64866
    public static final int STATUS_IMAGE_3              = 0xFD63;   // 64867
    // Description:
    //      image attachment

    public static final int STATUS_IMAGE_LOC_0          = 0xFD70;   // 64880
    public static final int STATUS_IMAGE_LOC_1          = 0xFD71;   // 64881
    public static final int STATUS_IMAGE_LOC_2          = 0xFD72;   // 64882
    public static final int STATUS_IMAGE_LOC_3          = 0xFD73;   // 64883
    // Description:
    //      image location

// ----------------------------------------------------------------------------
// General Rule trigger status

    public static final int STATUS_RULE_TRIGGER_0       = 0xFF00;   // 65280
    public static final int STATUS_RULE_TRIGGER_1       = 0xFF01;   // 65281
    public static final int STATUS_RULE_TRIGGER_2       = 0xFF02;   // 65282
    public static final int STATUS_RULE_TRIGGER_3       = 0xFF03;   // 65283
    public static final int STATUS_RULE_TRIGGER_4       = 0xFF04;   // 65284
    public static final int STATUS_RULE_TRIGGER_5       = 0xFF05;   // 65285
    public static final int STATUS_RULE_TRIGGER_6       = 0xFF06;   // 65286
    public static final int STATUS_RULE_TRIGGER_7       = 0xFF07;   // 65287
    // Description:
    //      General Rule trigger status

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

    private static final String CODE_PREFIX = ""; // "DMT.";

    public static class Code
        implements StatusCodeProvider
    {
        private int         code        = 0;
        private String      name        = "";
        private String      desc        = null;
        private I18N.Text   text        = null;
        private boolean     rtChecked   = false;
        private String      rtDesc      = null;
        private int         rtHiPri     = -1;
        private boolean     hiPri       = false;
        private String      iconName    = null;
        public Code(int code, String name, I18N.Text text) {
            this(code, name, text, false);
        }
        public Code(int code, String name, I18N.Text text, boolean higherPri) {
            this.code   = code;
            this.name   = CODE_PREFIX + name;
            this.text   = text;
            this.hiPri  = higherPri;
            this.checkRTConfigOverrides();
        }
        public Code(int code, String name, String desc) {
            this(code, name, desc, false);
        }
        public Code(int code, String name, String desc, boolean higherPri) {
            this.code   = code;
            this.name   = CODE_PREFIX + name;
            this.desc   = desc;
            this.hiPri  = higherPri;
            this.checkRTConfigOverrides();
        }
        public void checkRTConfigOverrides() {
            if (!this.rtChecked) {
                synchronized (this) {
                    if (!this.rtChecked) {
                        String rtKey = this.getDescriptionRTKey();
                        // description override
                        String descKey = rtKey;             // ie. "StatusCodes.0xF020"
                        if (RTConfig.hasProperty(descKey)) {
                            this.rtDesc = RTConfig.getString(descKey, null);
                        }
                        // high-priority override
                        String hipriKey = rtKey + ".hipri"; // ie. "StatusCodes.0xF020.hipri"
                        if (RTConfig.hasProperty(hipriKey)) {
                            this.rtHiPri = RTConfig.getBoolean(hipriKey,false)? 1 : 0;
                        }
                        // set checked
                        this.rtChecked = true;
                    }
                }
            }
        }
        public int getCode() {
            return this.code;
        }
        public int getStatusCode() { // StatusCodeProvider interface
            return this.getCode();
        }
        public String getName() {
            return this.name;
        }
        public String getDescriptionRTKey() {
            if (this.text != null) {
                return this.text.getKey();
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append("StatusCodes.");
                sb.append("0x");
                sb.append(StringTools.toHexString(this.getCode(),16));
                return sb.toString();
            }
        }
        public void setDescription(String desc) {
            this.desc = !StringTools.isBlank(desc)? desc : null;
            if (desc != null) {
                this.rtDesc = null; // reset on explicit call to this method
            }
        }
        public String getDescription(Locale locale) { // StatusCodeProvider interface
            // return description, IE: "Location"
            if (this.rtDesc != null) {
                return this.rtDesc; // rt config description overrides
            } else
            if (this.desc != null) {
                return this.desc;
            } else {
                return this.text.toString(locale);
            }
        }
        public void setHighPriority(boolean hipri) {
            this.hiPri   = hipri;
            this.rtHiPri = -1; // reset on explicit call to this method
        }
        public boolean isHighPriority() {
            if (this.rtHiPri >= 0) {
                return (this.rtHiPri != 0)? true : false;
            } else {
                return this.hiPri;
            }
        }
        public void setIconName(String name) {
            this.iconName = StringTools.trim(name);
        }
        public String getIconName() { // StatusCodeProvider interface
            return this.iconName;
        }
        public String toString() {
            return this.toString(null);
        }
        public String toString(Locale locale) {
            // IE: "0xF020 (61472) Location"
            StringBuffer sb = new StringBuffer();
            sb.append("0x").append(StringTools.toHexString(this.getCode(),16));
            sb.append(" (").append(this.getCode()).append(") ");
            sb.append(this.getDescription(locale));
            return sb.toString();
        }
        public String getIconSelector() { // StatusCodeProvider interface
            return "";
        }
        public String getForegroundColor() { // StatusCodeProvider interface
            return ""; // inherited CSS
        }
        public String getBackgroundColor() { // StatusCodeProvider interface
            return ""; // inherited CSS
        }
        public boolean hasStyle() {
            return false;
        }
        public String getStyleString() {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // StatusCode table

    private static Code _codeArray[] = new Code[] {

        //       16-bit Code                  Name             Description                                                                        HiPri
        new Code(STATUS_NONE                , "NONE"         , I18N.getString(StatusCodes.class,"StatusCodes.none"            ,"None"            )       ), // always first

        new Code(STATUS_GFMI_CMD_03         , "GMFI.03"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_03"         ,"GFMI_SendMsg3"   )       ), // send non-ack message
        new Code(STATUS_GFMI_CMD_04         , "GMFI.04"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_04"         ,"GFMI_SendMsg4"   )       ), // send ack message
        new Code(STATUS_GFMI_CMD_05         , "GMFI.05"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_05"         ,"GFMI_SendMsg5"   )       ), // send answerable message
        new Code(STATUS_GFMI_CMD_06         , "GMFI.06"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_06"         ,"GFMI_StopLoc6"   )       ), // send stop location
        new Code(STATUS_GFMI_CMD_08         , "GMFI.08"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_08"         ,"GFMI_ReqETA8"    )       ), // request stop ETA
        new Code(STATUS_GFMI_CMD_09         , "GMFI.09"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_09"         ,"GFMI_AutoArr9"   )       ), // set auto arrival criteria
        new Code(STATUS_GFMI_LINK_OFF       , "GFMI.LINK.0"  , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_link_0"     ,"GFMI_LinkOff"    )       ), // (off) GFMI link lost
        new Code(STATUS_GFMI_LINK_ON        , "GFMI.LINK.1"  , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_link_1"     ,"GFMI_LinkOn"     )       ), // (on ) GFMI link established
        new Code(STATUS_GFMI_ACK            , "GFMI.ACK"     , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_ack"        ,"GFMI_ACK"        )       ), // received ACK
        new Code(STATUS_GFMI_MESSAGE        , "GFMI.MESSAGE" , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_message"    ,"GFMI_Message"    )       ), // received message
        new Code(STATUS_GFMI_MESSAGE_ACK    , "GFMI.MSG.ACK" , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_msg_ack"    ,"GFMI_MsgACK"     )       ), // received message ack
        new Code(STATUS_GFMI_STOP_STATUS_1  , "GFMI.STOP.STA", I18N.getString(StatusCodes.class,"StatusCodes.gfmi_stopStatus" ,"GFMI_StopStatus" )       ), // stop status
        new Code(STATUS_GFMI_STOP_STATUS_2  , "GFMI.STOP.ST2", I18N.getString(StatusCodes.class,"StatusCodes.gfmi_stopStatus2","GFMI_StopStatus2")       ), // stop status
        new Code(STATUS_GFMI_STOP_STATUS_3  , "GFMI.STOP.ST3", I18N.getString(StatusCodes.class,"StatusCodes.gfmi_stopStatus3","GFMI_StopStatus3")       ), // stop status
        new Code(STATUS_GFMI_STOP_STATUS_4  , "GFMI.STOP.ST4", I18N.getString(StatusCodes.class,"StatusCodes.gfmi_stopStatus4","GFMI_StopStatus4")       ), // stop status
        new Code(STATUS_GFMI_ETA            , "GFMI.ETA"     , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_eta"        ,"GFMI_ETA"        )       ), // eta
        new Code(STATUS_GFMI_DRIVER_ID      , "GFMI.DRV.ID"  , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_driverID"   ,"GFMI_DriverID"   )       ), // driver ID
        new Code(STATUS_GFMI_DRIVER_STATUS  , "GFMI.DRV.STA" , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_driverStat" ,"GFMI_DriverStat" )       ), // driver status

        new Code(STATUS_INITIALIZED         , "INITIALIZED"  , I18N.getString(StatusCodes.class,"StatusCodes.initialized"     ,"Initialized"     )       ),
        new Code(STATUS_LOCATION            , "LOCATION"     , I18N.getString(StatusCodes.class,"StatusCodes.location"        ,"Location"        )       ),
        new Code(STATUS_LOCATION_1          , "LOCATION.1"   , I18N.getString(StatusCodes.class,"StatusCodes.location_1"      ,"Location_1"      )       ),
        new Code(STATUS_LOCATION_2          , "LOCATION.2"   , I18N.getString(StatusCodes.class,"StatusCodes.location_2"      ,"Location_2"      )       ),
        new Code(STATUS_LAST_LOCATION       , "LOC.LAST"     , I18N.getString(StatusCodes.class,"StatusCodes.lastLocation"    ,"Last_Location"   )       ),
        new Code(STATUS_CELL_LOCATION       , "LOC.CELL"     , I18N.getString(StatusCodes.class,"StatusCodes.cellLocation"    ,"Cell_Location"   )       ),
        new Code(STATUS_DISTANCE            , "DISTANCE"     , I18N.getString(StatusCodes.class,"StatusCodes.distance"        ,"Distance"        )       ),
        new Code(STATUS_DISTANCE_1          , "DISTANCE.1"   , I18N.getString(StatusCodes.class,"StatusCodes.distance_1"      ,"Distance_1"      )       ),
        new Code(STATUS_DISTANCE_2          , "DISTANCE.2"   , I18N.getString(StatusCodes.class,"StatusCodes.distance_2"      ,"Distance_2"      )       ),
        new Code(STATUS_WAYMARK_0           , "WAYMARK.0"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_0"       ,"Waymark_0"       ), true ),
        new Code(STATUS_WAYMARK_1           , "WAYMARK.1"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_1"       ,"Waymark_1"       ), true ),
        new Code(STATUS_WAYMARK_2           , "WAYMARK.2"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_2"       ,"Waymark_2"       ), true ),
        new Code(STATUS_WAYMARK_3           , "WAYMARK.3"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_3"       ,"Waymark_3"       ), true ),
        new Code(STATUS_WAYMARK_4           , "WAYMARK.4"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_4"       ,"Waymark_4"       ), true ),
        new Code(STATUS_WAYMARK_5           , "WAYMARK.5"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_5"       ,"Waymark_5"       ), true ),
        new Code(STATUS_WAYMARK_6           , "WAYMARK.6"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_6"       ,"Waymark_6"       ), true ),
        new Code(STATUS_WAYMARK_7           , "WAYMARK.7"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_7"       ,"Waymark_7"       ), true ),
        new Code(STATUS_WAYMARK_8           , "WAYMARK.8"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_8"       ,"Waymark_8"       ), true ),
        new Code(STATUS_QUERY               , "QUERY"        , I18N.getString(StatusCodes.class,"StatusCodes.query"           ,"Query"           ), true ),
        new Code(STATUS_NOTIFY              , "NOTIFY"       , I18N.getString(StatusCodes.class,"StatusCodes.notify"          ,"Notify"          ), true ),
        new Code(STATUS_LOG                 , "LOG"          , I18N.getString(StatusCodes.class,"StatusCodes.log"             ,"Log"             )       ),
        new Code(STATUS_SAMPLE_0            , "SAMPLE.0"     , I18N.getString(StatusCodes.class,"StatusCodes.sample_0"        ,"Sample_0"        )       ),
        new Code(STATUS_SAMPLE_1            , "SAMPLE.1"     , I18N.getString(StatusCodes.class,"StatusCodes.sample_1"        ,"Sample_1"        )       ),
        new Code(STATUS_SAMPLE_2            , "SAMPLE.2"     , I18N.getString(StatusCodes.class,"StatusCodes.sample_2"        ,"Sample_2"        )       ),
        new Code(STATUS_HEARTBEAT           , "HEARTBEAT"    , I18N.getString(StatusCodes.class,"StatusCodes.heartbeat"       ,"Heartbeat"       )       ),
        new Code(STATUS_POI_0               , "POI.0"        , I18N.getString(StatusCodes.class,"StatusCodes.poi_0"           ,"POI_0"           )       ),
        new Code(STATUS_POI_1               , "POI.1"        , I18N.getString(StatusCodes.class,"StatusCodes.poi_1"           ,"POI_1"           )       ),
        new Code(STATUS_POI_2               , "POI.2"        , I18N.getString(StatusCodes.class,"StatusCodes.poi_2"           ,"POI_2"           )       ),
        new Code(STATUS_POI_3               , "POI.3"        , I18N.getString(StatusCodes.class,"StatusCodes.poi_3"           ,"POI_3"           )       ),
        new Code(STATUS_POI_4               , "POI.4"        , I18N.getString(StatusCodes.class,"StatusCodes.poi_4"           ,"POI_4"           )       ),
        new Code(STATUS_DATA_MESSAGE_0      , "DATA.0"       , I18N.getString(StatusCodes.class,"StatusCodes.dataMessage_0"   ,"DataMessage"     )       ),
        new Code(STATUS_DATA_MESSAGE_1      , "DATA.1"       , I18N.getString(StatusCodes.class,"StatusCodes.dataMessage_1"   ,"DataMessage_1"   )       ),
        new Code(STATUS_DATA_MESSAGE_2      , "DATA.2"       , I18N.getString(StatusCodes.class,"StatusCodes.dataMessage_2"   ,"DataMessage_2"   )       ),
        new Code(STATUS_VOICE               , "VOICE"        , I18N.getString(StatusCodes.class,"StatusCodes.voice"           ,"Voice"           )       ),
        new Code(STATUS_VOICE_OUTGOING      , "VOICE.OUT"    , I18N.getString(StatusCodes.class,"StatusCodes.voice_outgoing"  ,"Voice_Outgoing"  )       ),
        new Code(STATUS_VOICE_INCOMING      , "VOICE.IN"     , I18N.getString(StatusCodes.class,"StatusCodes.voice_incoming"  ,"Voice_Incoming"  )       ),
        new Code(STATUS_RFID_TAG_0          , "RFID.0"       , I18N.getString(StatusCodes.class,"StatusCodes.rfid_0"          ,"RFID_0"          )       ),
        new Code(STATUS_RFID_TAG_1          , "RFID.1"       , I18N.getString(StatusCodes.class,"StatusCodes.rfid_1"          ,"RFID_1"          )       ),
        new Code(STATUS_RFID_TAG_2          , "RFID.2"       , I18N.getString(StatusCodes.class,"StatusCodes.rfid_2"          ,"RFID_2"          )       ),

        new Code(STATUS_MOTION_START        , "MOT.START"    , I18N.getString(StatusCodes.class,"StatusCodes.start"           ,"Start"           ), true ), // (on ) 
        new Code(STATUS_MOTION_EN_ROUTE     , "MOT.ENROUTE"  , I18N.getString(StatusCodes.class,"StatusCodes.inMotion"        ,"EnRoute"         )       ),
        new Code(STATUS_MOTION_STOP         , "MOT.STOP"     , I18N.getString(StatusCodes.class,"StatusCodes.stop"            ,"Stop"            ), true ), // (off) 
        new Code(STATUS_MOTION_DORMANT      , "MOT.DORMANT"  , I18N.getString(StatusCodes.class,"StatusCodes.dormant"         ,"Dormant"         )       ),
        new Code(STATUS_MOTION_STOPPED      , "MOT.STOPPED"  , I18N.getString(StatusCodes.class,"StatusCodes.stopped"         ,"Stopped"         )       ),
        new Code(STATUS_MOTION_IDLE         , "MOT.IDLE"     , I18N.getString(StatusCodes.class,"StatusCodes.idle"            ,"Idle"            )       ),
        new Code(STATUS_MOTION_EXCESS_IDLE  , "MOT.IDLE.X"   , I18N.getString(StatusCodes.class,"StatusCodes.excessIdle"      ,"Excess_Idle"     ), true ),
        new Code(STATUS_MOTION_OVER_SPEED_1 , "MOT.SPEED.X1" , I18N.getString(StatusCodes.class,"StatusCodes.speeding"        ,"Speeding"        ), true ),
        new Code(STATUS_MOTION_OVER_SPEED_2 , "MOT.SPEED.X2" , I18N.getString(StatusCodes.class,"StatusCodes.speeding.2"      ,"Speeding_2"      ), true ),
        new Code(STATUS_MOTION_MOVING       , "MOT.MOVING"   , I18N.getString(StatusCodes.class,"StatusCodes.moving"          ,"Moving"          )       ),
        new Code(STATUS_MOTION_STOP_PENDING , "MOT.STOPPING" , I18N.getString(StatusCodes.class,"StatusCodes.stopPending"     ,"Stop_Pending"    )       ),
        new Code(STATUS_MOTION_CHANGE       , "MOT.CHANGE"   , I18N.getString(StatusCodes.class,"StatusCodes.motionChange"    ,"Motion_Change"   )       ),
        new Code(STATUS_MOTION_HEADING      , "MOT.HEADING"  , I18N.getString(StatusCodes.class,"StatusCodes.headingChange"   ,"Heading_Change"  )       ),
        new Code(STATUS_MOTION_ACCELEROMETER, "MOT.ACCELOMTR", I18N.getString(StatusCodes.class,"StatusCodes.accelerometer"   ,"Accelerometer"   ), true ),
        new Code(STATUS_MOTION_ACCELERATION , "MOT.ACCEL"    , I18N.getString(StatusCodes.class,"StatusCodes.acceleration"    ,"Acceleration"    )       ),
        new Code(STATUS_MOTION_DECELERATION , "MOT.DECEL"    , I18N.getString(StatusCodes.class,"StatusCodes.deceleration"    ,"Deceleration"    )       ),

        new Code(STATUS_ODOM_0              , "ODO.0"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_0"      ,"Odometer_0"      )       ),
        new Code(STATUS_ODOM_1              , "ODO.1"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_1"      ,"Odometer_1"      )       ),
        new Code(STATUS_ODOM_2              , "ODO.2"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_2"      ,"Odometer_2"      )       ),
        new Code(STATUS_ODOM_3              , "ODO.3"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_3"      ,"Odometer_3"      )       ),
        new Code(STATUS_ODOM_4              , "ODO.4"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_4"      ,"Odometer_4"      )       ),
        new Code(STATUS_ODOM_5              , "ODO.5"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_5"      ,"Odometer_5"      )       ),
        new Code(STATUS_ODOM_6              , "ODO.6"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_6"      ,"Odometer_6"      )       ),
        new Code(STATUS_ODOM_7              , "ODO.7"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_7"      ,"Odometer_7"      )       ),
        new Code(STATUS_ODOM_LIMIT_0        , "ODO.LIM.0"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_0"      ,"OdoLimit_0"      ), true ),
        new Code(STATUS_ODOM_LIMIT_1        , "ODO.LIM.1"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_1"      ,"OdoLimit_1"      ), true ),
        new Code(STATUS_ODOM_LIMIT_2        , "ODO.LIM.2"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_2"      ,"OdoLimit_2"      ), true ),
        new Code(STATUS_ODOM_LIMIT_3        , "ODO.LIM.3"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_3"      ,"OdoLimit_3"      ), true ),
        new Code(STATUS_ODOM_LIMIT_4        , "ODO.LIM.4"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_4"      ,"OdoLimit_4"      ), true ),
        new Code(STATUS_ODOM_LIMIT_5        , "ODO.LIM.5"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_5"      ,"OdoLimit_5"      ), true ),
        new Code(STATUS_ODOM_LIMIT_6        , "ODO.LIM.6"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_6"      ,"OdoLimit_6"      ), true ),
        new Code(STATUS_ODOM_LIMIT_7        , "ODO.LIM.7"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_7"      ,"OdoLimit_7"      ), true ),

        new Code(STATUS_SPEEDING_BEGIN      , "SPEED.BEGIN"  , I18N.getString(StatusCodes.class,"StatusCodes.speedingBegin"   ,"Speeding Begin"  ), true ),
        new Code(STATUS_SPEEDING_LIMIT_1    , "SPEED.LIMIT.1", I18N.getString(StatusCodes.class,"StatusCodes.speedingLimit_1" ,"Speeding Limit 1"), true ),
        new Code(STATUS_SPEEDING_LIMIT_2    , "SPEED.LIMIT.2", I18N.getString(StatusCodes.class,"StatusCodes.speedingLimit_2" ,"Speeding Limit 2"), true ),
        new Code(STATUS_SPEEDING_LIMIT_3    , "SPEED.LIMIT.3", I18N.getString(StatusCodes.class,"StatusCodes.speedingLimit_3" ,"Speeding Limit 3"), true ),
        new Code(STATUS_SPEEDING_LIMIT_4    , "SPEED.LIMIT.4", I18N.getString(StatusCodes.class,"StatusCodes.speedingLimit_4" ,"Speeding Limit 4"), true ),
        new Code(STATUS_SPEEDING_END        , "SPEED.END"    , I18N.getString(StatusCodes.class,"StatusCodes.speedingEnd"     ,"Speeding End"    ), true ),

        new Code(STATUS_TRACK_START         , "TRACK.START"  , I18N.getString(StatusCodes.class,"StatusCodes.track.start"     ,"Track_Start"     ), true ), // (on ) 
        new Code(STATUS_TRACK_LOCATION      , "TRACK.LOC"    , I18N.getString(StatusCodes.class,"StatusCodes.track.location"  ,"Track_Location"  )       ),
        new Code(STATUS_TRACK_STOP          , "TRACK.STOP"   , I18N.getString(StatusCodes.class,"StatusCodes.track.stop"      ,"Track_Stop"      ), true ), // (off) 

        new Code(STATUS_GEOFENCE_ARRIVE     , "GEO.ARR"      , I18N.getString(StatusCodes.class,"StatusCodes.arrive"          ,"Arrive"          ), true ), // (on ) 
        new Code(STATUS_CORRIDOR_ARRIVE     , "COR.ARR"      , I18N.getString(StatusCodes.class,"StatusCodes.corridorArrive"  ,"Corridor_Arrive" ), true ), // (on ) 
        new Code(STATUS_JOB_ARRIVE          , "JOB.ARR"      , I18N.getString(StatusCodes.class,"StatusCodes.jobArrive"       ,"JobArrive"       ), true ), // (on ) 
        new Code(STATUS_GEOFENCE_DEPART     , "GEO.DEP"      , I18N.getString(StatusCodes.class,"StatusCodes.depart"          ,"Depart"          ), true ), // (off) 
        new Code(STATUS_CORRIDOR_DEPART     , "COR.DEP"      , I18N.getString(StatusCodes.class,"StatusCodes.corridorDepart"  ,"Corridor_Depart" ), true ), // (off) 
        new Code(STATUS_JOB_DEPART          , "JOB.DEP"      , I18N.getString(StatusCodes.class,"StatusCodes.jobDepart"       ,"Job_Depart"      ), true ), // (off) 
        new Code(STATUS_GEOFENCE_VIOLATION  , "GEO.VIO"      , I18N.getString(StatusCodes.class,"StatusCodes.geofence"        ,"Geofence"        ), true ),
        new Code(STATUS_CORRIDOR_VIOLATION  , "COR.VIO"      , I18N.getString(StatusCodes.class,"StatusCodes.corridor"        ,"Geo_Corridor"    ), true ),
        new Code(STATUS_GEOFENCE_ACTIVE     , "GEO.ACT"      , I18N.getString(StatusCodes.class,"StatusCodes.geofActive"      ,"Geof_Active"     )       ), // (on ) 
        new Code(STATUS_CORRIDOR_ACTIVE     , "COR.ACT"      , I18N.getString(StatusCodes.class,"StatusCodes.corrActive"      ,"Corr_Active"     )       ), // (on ) 
        new Code(STATUS_GEOFENCE_INACTIVE   , "GEO.INA"      , I18N.getString(StatusCodes.class,"StatusCodes.geofInactive"    ,"Geof_Inactive"   )       ), // (off) 
        new Code(STATUS_CORRIDOR_INACTIVE   , "COR.INA"      , I18N.getString(StatusCodes.class,"StatusCodes.corrInactive"    ,"Corr_Inactive"   )       ), // (off) 
        new Code(STATUS_GEOBOUNDS_ENTER     , "STA.ENTR"     , I18N.getString(StatusCodes.class,"StatusCodes.stateEnter"      ,"State_Enter"     ), true ), // (on ) 
        new Code(STATUS_GEOBOUNDS_EXIT      , "STA.EXIT"     , I18N.getString(StatusCodes.class,"StatusCodes.stateExit"       ,"State_Exit"      ), true ), // (off) 
        new Code(STATUS_PARKED              , "GEO.PARK"     , I18N.getString(StatusCodes.class,"StatusCodes.parked"          ,"Parked"          ), true ), // (on ) 
        new Code(STATUS_EXCESS_PARK         , "EXCESS.PARK"  , I18N.getString(StatusCodes.class,"StatusCodes.excessPark"      ,"Excess_Park"     ), true ),
        new Code(STATUS_UNPARKED            , "GEO.UNPARK"   , I18N.getString(StatusCodes.class,"StatusCodes.unparked"        ,"UnParked"        ), true ), // (off) 

        new Code(STATUS_INPUT_STATE         , "INP.STA"      , I18N.getString(StatusCodes.class,"StatusCodes.inputs"          ,"Inputs"          )       ),
        new Code(STATUS_IGNITION_ON         , "IGN.ON"       , I18N.getString(StatusCodes.class,"StatusCodes.ignitionOn"      ,"Ignition_On"     ), true ), // (on ) 
        new Code(STATUS_INPUT_ON            , "INP.ON"       , I18N.getString(StatusCodes.class,"StatusCodes.inputOn"         ,"Input_On"        )       ), // (on ) 
        new Code(STATUS_IGNITION_OFF        , "IGN.OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.ignitionOff"     ,"Ignition_Off"    ), true ), // (off) 
        new Code(STATUS_INPUT_OFF           , "INP.OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.inputOff"        ,"Input_Off"       )       ), // (off) 

        new Code(STATUS_OUTPUT_STATE        , "OUT.ST"       , I18N.getString(StatusCodes.class,"StatusCodes.outputs"         ,"Outputs"         )       ),
        new Code(STATUS_OUTPUT_ON           , "OUT.ON"       , I18N.getString(StatusCodes.class,"StatusCodes.outputOn"        ,"Output_On"       )       ), // (on ) 
        new Code(STATUS_OUTPUT_OFF          , "OUT.OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.outputOff"       ,"Output_Off"      )       ), // (off) 

        new Code(STATUS_ENGINE_START        , "ENG.START"    , I18N.getString(StatusCodes.class,"StatusCodes.engineStart"     ,"Engine_Start"    ), true ), // (on ) 
        new Code(STATUS_ENGINE_STOP         , "ENG.STOP"     , I18N.getString(StatusCodes.class,"StatusCodes.engineStop"      ,"Engine_Stop"     ), true ), // (off) 

        new Code(STATUS_INPUT_ON_00         , "INP.ON.0"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_0"       ,"InputOn_0"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_01         , "INP.ON.1"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_1"       ,"InputOn_1"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_02         , "INP.ON.2"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_2"       ,"InputOn_2"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_03         , "INP.ON.3"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_3"       ,"InputOn_3"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_04         , "INP.ON.4"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_4"       ,"InputOn_4"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_05         , "INP.ON.5"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_5"       ,"InputOn_5"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_06         , "INP.ON.6"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_6"       ,"InputOn_6"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_07         , "INP.ON.7"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_7"       ,"InputOn_7"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_08         , "INP.ON.8"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_8"       ,"InputOn_8"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_09         , "INP.ON.9"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_9"       ,"InputOn_9"       ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_10         , "INP.ON.10"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_10"      ,"InputOn_10"      ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_11         , "INP.ON.11"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_11"      ,"InputOn_11"      ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_12         , "INP.ON.12"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_12"      ,"InputOn_12"      ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_13         , "INP.ON.13"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_13"      ,"InputOn_13"      ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_14         , "INP.ON.14"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_14"      ,"InputOn_14"      ), true ), // (on ) 
        new Code(STATUS_INPUT_ON_15         , "INP.ON.15"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_15"      ,"InputOn_15"      ), true ), // (on ) 

        new Code(STATUS_DOOR_OPEN_0         , "DOOR.OPEN.0"  , I18N.getString(StatusCodes.class,"StatusCodes.doorOpen_0"      ,"DoorOpen"        ), true ), // (on ) 
        new Code(STATUS_DOOR_OPEN_1         , "DOOR.OPEN.1"  , I18N.getString(StatusCodes.class,"StatusCodes.doorOpen_1"      ,"DoorOpen_1"      ), true ), // (on ) 
        new Code(STATUS_DOOR_OPEN_2         , "DOOR.OPEN.2"  , I18N.getString(StatusCodes.class,"StatusCodes.doorOpen_2"      ,"DoorOpen_2"      ), true ), // (on ) 

        new Code(STATUS_SEATBELT_ON_0       , "BELT.ON.0"    , I18N.getString(StatusCodes.class,"StatusCodes.seatbeltOn_0"    ,"SeatbeltOn"      )       ), // (on ) 
        new Code(STATUS_SEATBELT_ON_1       , "BELT.ON.1"    , I18N.getString(StatusCodes.class,"StatusCodes.seatbeltOn_1"    ,"SeatbeltOn_1"    )       ), // (on ) 

        new Code(STATUS_INPUT_OFF_00        , "INP.OFF.0"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_0"      ,"InputOff_0"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_01        , "INP.OFF.1"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_1"      ,"InputOff_1"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_02        , "INP.OFF.2"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_2"      ,"InputOff_2"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_03        , "INP.OFF.3"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_3"      ,"InputOff_3"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_04        , "INP.OFF.4"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_4"      ,"InputOff_4"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_05        , "INP.OFF.5"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_5"      ,"InputOff_5"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_06        , "INP.OFF.6"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_6"      ,"InputOff_6"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_07        , "INP.OFF.7"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_7"      ,"InputOff_7"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_08        , "INP.OFF.8"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_8"      ,"InputOff_8"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_09        , "INP.OFF.9"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_9"      ,"InputOff_9"      ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_10        , "INP.OFF.10"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_10"     ,"InputOff_10"     ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_11        , "INP.OFF.11"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_11"     ,"InputOff_11"     ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_12        , "INP.OFF.12"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_12"     ,"InputOff_12"     ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_13        , "INP.OFF.13"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_13"     ,"InputOff_13"     ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_14        , "INP.OFF.14"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_14"     ,"InputOff_14"     ), true ), // (off) 
        new Code(STATUS_INPUT_OFF_15        , "INP.OFF.15"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_15"     ,"InputOff_15"     ), true ), // (off) 

        new Code(STATUS_DOOR_CLOSE_0        , "DOOR.CLOSE.0" , I18N.getString(StatusCodes.class,"StatusCodes.doorClose_0"     ,"DoorClose"       ), true ), // (off) 
        new Code(STATUS_DOOR_CLOSE_1        , "DOOR.CLOSE.1" , I18N.getString(StatusCodes.class,"StatusCodes.doorClose_1"     ,"DoorClose_1"     ), true ), // (off) 
        new Code(STATUS_DOOR_CLOSE_2        , "DOOR.CLOSE.2" , I18N.getString(StatusCodes.class,"StatusCodes.doorClose_2"     ,"DoorClose_2"     ), true ), // (off) 

        new Code(STATUS_SEATBELT_OFF_0      , "BELT.OFF.0"   , I18N.getString(StatusCodes.class,"StatusCodes.seatbeltOff_0"   ,"SeatbeltOff"     )       ), // (off) 
        new Code(STATUS_SEATBELT_OFF_1      , "BELT.OFF.1"   , I18N.getString(StatusCodes.class,"StatusCodes.seatbeltOff_1"   ,"SeatbeltOff_1"   )       ), // (off) 

        new Code(STATUS_OUTPUT_ON_00        , "OUT.ON.0"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_0"      ,"OutputOn_0"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_01        , "OUT.ON.1"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_1"      ,"OutputOn_1"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_02        , "OUT.ON.2"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_2"      ,"OutputOn_2"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_03        , "OUT.ON.3"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_3"      ,"OutputOn_3"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_04        , "OUT.ON.4"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_4"      ,"OutputOn_4"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_05        , "OUT.ON.5"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_5"      ,"OutputOn_5"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_06        , "OUT.ON.6"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_6"      ,"OutputOn_6"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_07        , "OUT.ON.7"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_7"      ,"OutputOn_7"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_08        , "OUT.ON.8"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_8"      ,"OutputOn_8"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_ON_09        , "OUT.ON.9"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_9"      ,"OutputOn_9"      ), true ), // (on ) 
        new Code(STATUS_OUTPUT_OFF_00       , "OUT.OFF.0"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_0"     ,"OutputOff_0"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_01       , "OUT.OFF.1"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_1"     ,"OutputOff_1"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_02       , "OUT.OFF.2"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_2"     ,"OutputOff_2"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_03       , "OUT.OFF.3"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_3"     ,"OutputOff_3"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_04       , "OUT.OFF.4"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_4"     ,"OutputOff_4"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_05       , "OUT.OFF.5"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_5"     ,"OutputOff_5"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_06       , "OUT.OFF.6"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_6"     ,"OutputOff_6"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_07       , "OUT.OFF.7"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_7"     ,"OutputOff_7"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_08       , "OUT.OFF.8"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_8"     ,"OutputOff_8"     ), true ), // (off) 
        new Code(STATUS_OUTPUT_OFF_09       , "OUT.OFF.9"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_9"     ,"OutputOff_9"     ), true ), // (off) 

        new Code(STATUS_ELAPSED_00          , "ELA.0"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_0"        ,"Elapse_0"        )       ),
        new Code(STATUS_ELAPSED_01          , "ELA.1"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_1"        ,"Elapse_1"        )       ),
        new Code(STATUS_ELAPSED_02          , "ELA.2"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_2"        ,"Elapse_2"        )       ),
        new Code(STATUS_ELAPSED_03          , "ELA.3"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_3"        ,"Elapse_3"        )       ),
        new Code(STATUS_ELAPSED_04          , "ELA.4"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_4"        ,"Elapse_4"        )       ),
        new Code(STATUS_ELAPSED_05          , "ELA.5"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_5"        ,"Elapse_5"        )       ),
        new Code(STATUS_ELAPSED_06          , "ELA.6"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_6"        ,"Elapse_6"        )       ),
        new Code(STATUS_ELAPSED_07          , "ELA.7"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_7"        ,"Elapse_7"        )       ),
        new Code(STATUS_ELAPSED_LIMIT_00    , "ELA.LIM.0"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_0"      ,"ElaLimit_0"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_01    , "ELA.LIM.1"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_1"      ,"ElaLimit_1"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_02    , "ELA.LIM.2"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_2"      ,"ElaLimit_2"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_03    , "ELA.LIM.3"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_3"      ,"ElaLimit_3"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_04    , "ELA.LIM.4"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_4"      ,"ElaLimit_4"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_05    , "ELA.LIM.5"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_5"      ,"ElaLimit_5"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_06    , "ELA.LIM.6"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_6"      ,"ElaLimit_6"      ), true ),
        new Code(STATUS_ELAPSED_LIMIT_07    , "ELA.LIM.7"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_7"      ,"ElaLimit_7"      ), true ),

        new Code(STATUS_ANALOG_0            , "ANALOG.0"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_0"      ,"Analog_0"        )       ),
        new Code(STATUS_ANALOG_1            , "ANALOG.1"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_1"      ,"Analog_1"        )       ),
        new Code(STATUS_ANALOG_2            , "ANALOG.2"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_2"      ,"Analog_2"        )       ),
        new Code(STATUS_ANALOG_3            , "ANALOG.3"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_3"      ,"Analog_3"        )       ),
        new Code(STATUS_ANALOG_4            , "ANALOG.4"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_4"      ,"Analog_4"        )       ),
        new Code(STATUS_ANALOG_5            , "ANALOG.5"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_5"      ,"Analog_5"        )       ),
        new Code(STATUS_ANALOG_6            , "ANALOG.6"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_6"      ,"Analog_6"        )       ),
        new Code(STATUS_ANALOG_7            , "ANALOG.7"     , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_7"      ,"Analog_7"        )       ),
        new Code(STATUS_ANALOG_RANGE_0      , "ANALOG.LIM.0" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_0"    ,"AnalogRange_0"   ), true ),
        new Code(STATUS_ANALOG_RANGE_1      , "ANALOG.LIM.1" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_1"    ,"AnalogRange_1"   ), true ),
        new Code(STATUS_ANALOG_RANGE_2      , "ANALOG.LIM.2" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_2"    ,"AnalogRange_2"   ), true ),
        new Code(STATUS_ANALOG_RANGE_3      , "ANALOG.LIM.3" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_3"    ,"AnalogRange_3"   ), true ),
        new Code(STATUS_ANALOG_RANGE_4      , "ANALOG.LIM.4" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_4"    ,"AnalogRange_4"   ), true ),
        new Code(STATUS_ANALOG_RANGE_5      , "ANALOG.LIM.5" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_5"    ,"AnalogRange_5"   ), true ),
        new Code(STATUS_ANALOG_RANGE_6      , "ANALOG.LIM.6" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_6"    ,"AnalogRange_6"   ), true ),
        new Code(STATUS_ANALOG_RANGE_7      , "ANALOG.LIM.7" , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_7"    ,"AnalogRange_7"   ), true ),

        new Code(STATUS_TEMPERATURE_0       , "TMP.0"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_0"          ,"Temp_0"          )       ),
        new Code(STATUS_TEMPERATURE_1       , "TMP.1"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_1"          ,"Temp_1"          )       ),
        new Code(STATUS_TEMPERATURE_2       , "TMP.2"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_2"          ,"Temp_2"          )       ),
        new Code(STATUS_TEMPERATURE_3       , "TMP.3"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_3"          ,"Temp_3"          )       ),
        new Code(STATUS_TEMPERATURE_4       , "TMP.4"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_4"          ,"Temp_4"          )       ),
        new Code(STATUS_TEMPERATURE_5       , "TMP.5"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_5"          ,"Temp_5"          )       ),
        new Code(STATUS_TEMPERATURE_6       , "TMP.6"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_6"          ,"Temp_6"          )       ),
        new Code(STATUS_TEMPERATURE_7       , "TMP.7"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_7"          ,"Temp_7"          )       ),
        new Code(STATUS_TEMPERATURE_RANGE_0 , "TMP.LIM.0"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_0"     ,"TempRange_0"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_1 , "TMP.LIM.1"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_1"     ,"TempRange_1"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_2 , "TMP.LIM.2"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_2"     ,"TempRange_2"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_3 , "TMP.LIM.3"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_3"     ,"TempRange_3"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_4 , "TMP.LIM.4"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_4"     ,"TempRange_4"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_5 , "TMP.LIM.5"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_5"     ,"TempRange_5"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_6 , "TMP.LIM.6"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_6"     ,"TempRange_6"     ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_7 , "TMP.LIM.7"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_7"     ,"TempRange_7"     ), true ),
        new Code(STATUS_TEMPERATURE_LOW_0   , "TMP.LOW"      , I18N.getString(StatusCodes.class,"StatusCodes.temp_low_0"      ,"Temp_Low"        ), true ),
        new Code(STATUS_TEMPERATURE_HIGH_0  , "TMP.HIGH"     , I18N.getString(StatusCodes.class,"StatusCodes.temp_high_0"     ,"Temp_High"       ), true ),
        new Code(STATUS_TEMPERATURE         , "TMP.ALL"      , I18N.getString(StatusCodes.class,"StatusCodes.temp_All"        ,"Temp_All"        )       ),

        new Code(STATUS_LOGIN               , "LOGIN"        , I18N.getString(StatusCodes.class,"StatusCodes.login"           ,"Login"           ), true ), // (on ) 
        new Code(STATUS_LOGOUT              , "LOGOUT"       , I18N.getString(StatusCodes.class,"StatusCodes.logout"          ,"Logout"          ), true ), // (off) 
        new Code(STATUS_ARMED               , "ARMED"        , I18N.getString(StatusCodes.class,"StatusCodes.armed"           ,"Armed"           ), true ),
        new Code(STATUS_DISARMED            , "DISARMED"     , I18N.getString(StatusCodes.class,"StatusCodes.disarmed"        ,"Disarmed"        ), true ),
        new Code(STATUS_ENTITY_STATE        , "ENTITY.STATE" , I18N.getString(StatusCodes.class,"StatusCodes.entityState"     ,"Entity_State"    ), true ),
        new Code(STATUS_ENTITY_CONNECT      , "ENTITY.CON"   , I18N.getString(StatusCodes.class,"StatusCodes.connect"         ,"Connect"         ), true ), // (on ) 
        new Code(STATUS_ENTITY_DISCONNECT   , "ENTITY.DIS"   , I18N.getString(StatusCodes.class,"StatusCodes.disconnect"      ,"Disconnect"      ), true ), // (off) 
        new Code(STATUS_ENTITY_INVENTORY    , "ENTITY.INV"   , I18N.getString(StatusCodes.class,"StatusCodes.inventory"       ,"Inventory"       ), true ),
        new Code(STATUS_TRAILER_STATE       , "TRAILER.STATE", I18N.getString(StatusCodes.class,"StatusCodes.trailerState"    ,"Trailer_State"   ), true ),
        new Code(STATUS_TRAILER_HOOK        , "TRAILER.HOOK" , I18N.getString(StatusCodes.class,"StatusCodes.trailerHook"     ,"Trailer_Hook"    ), true ), // (on ) 
        new Code(STATUS_TRAILER_UNHOOK      , "TRAILER.UNHK" , I18N.getString(StatusCodes.class,"StatusCodes.trailerUnook"    ,"Trailer_Unhook"  ), true ), // (off) 
        new Code(STATUS_TRAILER_INVENTORY   , "TRAILER.INV"  , I18N.getString(StatusCodes.class,"StatusCodes.trailerInventory","Trailer_Invntory"), true ),
        new Code(STATUS_RFID_STATE          , "RFID.STATE"   , I18N.getString(StatusCodes.class,"StatusCodes.rfidState"       ,"RFID_State"      ), true ),
        new Code(STATUS_RFID_CONNECT        , "RFID.CON"     , I18N.getString(StatusCodes.class,"StatusCodes.rfidConnect"     ,"RFID_Connect"    ), true ), // (on ) 
        new Code(STATUS_RFID_DISCONNECT     , "RFID.DIS"     , I18N.getString(StatusCodes.class,"StatusCodes.rfidDisconnect"  ,"RFID_Disconn"    ), true ), // (off) 
        new Code(STATUS_RFID_INVENTORY      , "RFID.INV"     , I18N.getString(StatusCodes.class,"StatusCodes.rfidInventory"   ,"RFID_Inventory"  ), true ),
        new Code(STATUS_PERSON_ENTER        , "PERSON.EMBARK", I18N.getString(StatusCodes.class,"StatusCodes.personEmbark"    ,"Person_Embark"   ), true ), // (on ) 
        new Code(STATUS_PERSON_EXIT         , "PERSON.DISEMB", I18N.getString(StatusCodes.class,"StatusCodes.personDisembark" ,"Person_Disembark"), true ), // (off) 
        new Code(STATUS_PERSON_INVENTORY    , "PERSON.INV"   , I18N.getString(StatusCodes.class,"StatusCodes.personInventory" ,"Person_Inventory"), true ),
        new Code(STATUS_PERSON_STATE        , "PERSON.STATE" , I18N.getString(StatusCodes.class,"StatusCodes.personState"     ,"Person_State"    ), true ),
        new Code(STATUS_ACK                 , "ACK"          , I18N.getString(StatusCodes.class,"StatusCodes.ack"             ,"Ack"             )       ), // (on ) 
        new Code(STATUS_NAK                 , "NAK"          , I18N.getString(StatusCodes.class,"StatusCodes.nak"             ,"Nak"             )       ), // (off) 
        new Code(STATUS_COMMAND_ACK         , "COMMAND.ACK"  , I18N.getString(StatusCodes.class,"StatusCodes.commandAck"      ,"Command_Ack"     )       ), // (on ) 
        new Code(STATUS_COMMAND_NAK         , "COMMAND.NAK"  , I18N.getString(StatusCodes.class,"StatusCodes.commandNak"      ,"Command_Nak"     )       ), // (off) 
        new Code(STATUS_DUTY_ON             , "DUTY_ON"      , I18N.getString(StatusCodes.class,"StatusCodes.dutyOn"          ,"Duty_On"         )       ), // (on ) 
        new Code(STATUS_DUTY_OFF            , "DUTY_OFF"     , I18N.getString(StatusCodes.class,"StatusCodes.dutyOff"         ,"Duty_Off"        )       ), // (off) 
        new Code(STATUS_PANIC_ON            , "PANIC_ON"     , I18N.getString(StatusCodes.class,"StatusCodes.panicOn"         ,"Panic"           ), true ), // (on ) 
        new Code(STATUS_PANIC_OFF           , "PANIC_OFF"    , I18N.getString(StatusCodes.class,"StatusCodes.panicOff"        ,"Panic_Off"       ), true ), // (off) 
        new Code(STATUS_ALARM_ON            , "ALARM_ON"     , I18N.getString(StatusCodes.class,"StatusCodes.alarmOn"         ,"Alarm"           ), true ), // (on ) 
        new Code(STATUS_ALARM_OFF           , "ALARM_OFF"    , I18N.getString(StatusCodes.class,"StatusCodes.alarmOff"        ,"Alarm_Off"       ), true ), // (off) 
        new Code(STATUS_ASSIST_ON           , "ASSIST_ON"    , I18N.getString(StatusCodes.class,"StatusCodes.assistOn"        ,"Assist"          ), true ), // (on ) 
        new Code(STATUS_ASSIST_OFF          , "ASSIST_OFF"   , I18N.getString(StatusCodes.class,"StatusCodes.assistOff"       ,"Assist_Off"      ), true ), // (off) 
        new Code(STATUS_MANDOWN_ON          , "MANDOWN_ON"   , I18N.getString(StatusCodes.class,"StatusCodes.mandownOn"       ,"ManDown"         ), true ), // (on ) 
        new Code(STATUS_MANDOWN_OFF         , "MANDOWN_OFF"  , I18N.getString(StatusCodes.class,"StatusCodes.mandownOff"      ,"ManDown_Off"     ), true ), // (off) 
        new Code(STATUS_MEDICAL_ON          , "MEDICAL_ON"   , I18N.getString(StatusCodes.class,"StatusCodes.medicalOn"       ,"Medical"         ), true ), // (on ) 
        new Code(STATUS_MEDICAL_OFF         , "MEDICAL_OFF"  , I18N.getString(StatusCodes.class,"StatusCodes.medicalOff"      ,"Medical_Off"     ), true ), // (off) 
        new Code(STATUS_DRIVER_FATIGUE      , "DRIVER_DROWSY", I18N.getString(StatusCodes.class,"StatusCodes.driverFatigue"   ,"Driver_Drowsy"   ), true ), // 
        new Code(STATUS_DRIVER_SLEEP        , "DRIVER_SLEEP" , I18N.getString(StatusCodes.class,"StatusCodes.driverSleep"     ,"Driver_Sleep"    )       ), // (on ) 
        new Code(STATUS_DRIVER_WAKE         , "DRIVER_WAKE"  , I18N.getString(StatusCodes.class,"StatusCodes.driverWake"      ,"Driver_Wake"     )       ), // (off) 
        new Code(STATUS_TOWING_START        , "TOW_START"    , I18N.getString(StatusCodes.class,"StatusCodes.towStart"        ,"Tow"             ), true ), // (on ) 
        new Code(STATUS_TOWING_STOP         , "TOW_STOP"     , I18N.getString(StatusCodes.class,"StatusCodes.towStop"         ,"Tow Stop"        ), true ), // (off) 
        new Code(STATUS_INTRUSION_ON        , "INTRUSION_ON" , I18N.getString(StatusCodes.class,"StatusCodes.intrusionOn"     ,"Intrusion"       ), true ), // (on ) 
        new Code(STATUS_INTRUSION_OFF       , "INTRUSION_OFF", I18N.getString(StatusCodes.class,"StatusCodes.intrusionOff"    ,"Intrusion_Off"   ), true ), // (off) 
        new Code(STATUS_BREACH_ON           , "BREACH_ON"    , I18N.getString(StatusCodes.class,"StatusCodes.breachOn"        ,"Breach"          ), true ), // (on ) 
        new Code(STATUS_BREACH_OFF          , "BREACH_OFF"   , I18N.getString(StatusCodes.class,"StatusCodes.breachOff"       ,"Breach_Off"      ), true ), // (off) 
        new Code(STATUS_VIBRATION_ON        , "VIBRATION_ON" , I18N.getString(StatusCodes.class,"StatusCodes.vibrationOn"     ,"Vibration"       ), true ), // (on ) 
        new Code(STATUS_VIBRATION_OFF       , "VIBRATION_OFF", I18N.getString(StatusCodes.class,"StatusCodes.vibrationOff"    ,"Vibration_Off"   ), true ), // (off) 
        new Code(STATUS_PTO_ON              , "PTO_ON"       , I18N.getString(StatusCodes.class,"StatusCodes.ptoOn"           ,"PTO_On"          )       ), // (on ) 
        new Code(STATUS_PTO_OFF             , "PTO_OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.ptoOff"          ,"PTO_Off"         )       ), // (off) 

        new Code(STATUS_CARJACK_DISABLED    , "CARJACK_DISAB", I18N.getString(StatusCodes.class,"StatusCodes.carjack.disabled","Carjack_Disable" )       ), // (off) 
        new Code(STATUS_CARJACK_ENABLED     , "CARJACK_ENABL", I18N.getString(StatusCodes.class,"StatusCodes.carjack.enabled" ,"Carjack_Enable"  )       ), // (on ) 
        new Code(STATUS_CARJACK_STANDBY     , "CARJACK_STDBY", I18N.getString(StatusCodes.class,"StatusCodes.carjack.standby" ,"Carjack_StandBy" )       ), // (off) 
        new Code(STATUS_CARJACK_ARMED       , "CARJACK_ARMED", I18N.getString(StatusCodes.class,"StatusCodes.carjack.armed"   ,"Carjack_Armed"   ), true ), // (on ) 
        new Code(STATUS_CARJACK_TRIGGERED   , "CARJACK_TRIGG", I18N.getString(StatusCodes.class,"StatusCodes.carjack.trigger" ,"Carjack_Trigger" ), true ),
        new Code(STATUS_CARJACK_CANCELLED   , "CARJACK_CANCL", I18N.getString(StatusCodes.class,"StatusCodes.carjack.cancel"  ,"Carjack_Cancel"  ), true ),

        new Code(STATUS_OBD_INFO_0          , "OBD.INFO.0"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_0"      ,"OBD_Info_0"      )       ),
        new Code(STATUS_OBD_INFO_1          , "OBD.INFO.1"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_1"      ,"OBD_Info_1"      )       ),
        new Code(STATUS_OBD_INFO_2          , "OBD.INFO.2"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_2"      ,"OBD_Info_2"      )       ),
        new Code(STATUS_OBD_INFO_3          , "OBD.INFO.3"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_3"      ,"OBD_Info_3"      )       ),
        new Code(STATUS_OBD_INFO_4          , "OBD.INFO.4"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_4"      ,"OBD_Info_4"      )       ),
        new Code(STATUS_OBD_DISCONNECT      , "OBD.DISCON"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Disconnect"  ,"OBD_Disconnect"  ), true ),
        new Code(STATUS_OBD_FAULT           , "OBD.FAULT"    , I18N.getString(StatusCodes.class,"StatusCodes.obc_Fault"       ,"OBD_Fault"       ), true ),
        new Code(STATUS_CHECK_ENGINE        , "OBD.CHKENG"   , I18N.getString(StatusCodes.class,"StatusCodes.checkEngine"     ,"Check_Engine"    ), true ),
        new Code(STATUS_OBD_RANGE           , "OBD.RANGE"    , I18N.getString(StatusCodes.class,"StatusCodes.obc_Range"       ,"OBD_Range"       ), true ),
        new Code(STATUS_OBD_RPM_RANGE       , "OBD.RPM"      , I18N.getString(StatusCodes.class,"StatusCodes.obc_Rpm"         ,"OBD_Rpm"         ), true ),
        new Code(STATUS_OBD_FUEL_RANGE      , "OBD.FUEL"     , I18N.getString(StatusCodes.class,"StatusCodes.obc_Fuel"        ,"OBD_Fuel"        ), true ),
        new Code(STATUS_OBD_OIL_RANGE       , "OBD.OIL"      , I18N.getString(StatusCodes.class,"StatusCodes.obc_Oil"         ,"OBD_Oil"         ), true ),
        new Code(STATUS_OBD_TEMP_RANGE      , "OBD.TEMP"     , I18N.getString(StatusCodes.class,"StatusCodes.obc_Temp"        ,"OBD_Temp"        ), true ),
        new Code(STATUS_OBD_LOAD_RANGE      , "OBD.ENGLOAD"  , I18N.getString(StatusCodes.class,"StatusCodes.obc_EngineLoad"  ,"OBD_EngLoad"     ), true ),
        new Code(STATUS_OBD_COOLANT_RANGE   , "OBD.COOLANT"  , I18N.getString(StatusCodes.class,"StatusCodes.obc_Coolant"     ,"OBD_Coolant"     ), true ),
        new Code(STATUS_EXCESS_BRAKING      , "OBD.BRAKE"    , I18N.getString(StatusCodes.class,"StatusCodes.braking"         ,"Braking"         ), true ),
        new Code(STATUS_EXCESS_BRAKING_2    , "OBD.BRAKE.2"  , I18N.getString(StatusCodes.class,"StatusCodes.braking.2"       ,"Braking_2"       ), true ),
        new Code(STATUS_EXCESS_BRAKING_3    , "OBD.BRAKE.3"  , I18N.getString(StatusCodes.class,"StatusCodes.braking.3"       ,"Braking_3"       ), true ),
        new Code(STATUS_EXCESS_CORNERING    , "OBD.CORNERING", I18N.getString(StatusCodes.class,"StatusCodes.cornering"       ,"Cornering"       ), true ),
        new Code(STATUS_EXCESS_CORNERING_2  , "OBD.CORNER.2" , I18N.getString(StatusCodes.class,"StatusCodes.cornering.2"     ,"Cornering_2"     ), true ),
        new Code(STATUS_EXCESS_CORNERING_3  , "OBD.CORNER.3" , I18N.getString(StatusCodes.class,"StatusCodes.cornering.3"     ,"Cornering_3"     ), true ),
        new Code(STATUS_IMPACT              , "OBD.IMPACT"   , I18N.getString(StatusCodes.class,"StatusCodes.impact"          ,"Impact"          ), true ),
        new Code(STATUS_FREEFALL            , "OBD.FREEFALL" , I18N.getString(StatusCodes.class,"StatusCodes.freefall"        ,"FreeFall"        ), true ),
        new Code(STATUS_FUEL_REFILL         , "FUEL.REFILL"  , I18N.getString(StatusCodes.class,"StatusCodes.fuelRefill"      ,"Fuel_Refill"     ), true ),
        new Code(STATUS_FUEL_THEFT          , "FUEL.THEFT"   , I18N.getString(StatusCodes.class,"StatusCodes.fuelTheft"       ,"Fuel_Theft"      ), true ),
        new Code(STATUS_LOW_FUEL            , "FUEL.LOW"     , I18N.getString(StatusCodes.class,"StatusCodes.fuelLow"         ,"Fuel_Low"        ), true ),
        new Code(STATUS_FUEL_DIRTY          , "FUEL.DIRTY"   , I18N.getString(StatusCodes.class,"StatusCodes.fuelDirty"       ,"Fuel_Dirty"      )       ),
        new Code(STATUS_FUEL_SENSOR         , "FUEL.SENSOR"  , I18N.getString(StatusCodes.class,"StatusCodes.fuelSensor"      ,"Fuel_Sensor"     )       ),
        new Code(STATUS_EXCESS_ACCEL        , "OBD.ACCEL"    , I18N.getString(StatusCodes.class,"StatusCodes.excessAccel"     ,"Excess_Accel"    ), true ),
        new Code(STATUS_EXCESS_ACCEL_2      , "OBD.ACCEL.2"  , I18N.getString(StatusCodes.class,"StatusCodes.excessAccel.2"   ,"Excess_Accel_2"  ), true ),
        new Code(STATUS_EXCESS_ACCEL_3      , "OBD.ACCEL.3"  , I18N.getString(StatusCodes.class,"StatusCodes.excessAccel.3"   ,"Excess_Accel_3"  ), true ),

        new Code(STATUS_DAY_SUMMARY         , "SUMMARY.DAY"  , I18N.getString(StatusCodes.class,"StatusCodes.daySummary"      ,"Day_Summary"     )       ),
        new Code(STATUS_TRIP_SUMMARY        , "SUMMARY.TRIP" , I18N.getString(StatusCodes.class,"StatusCodes.tripSummary"     ,"Trip_Summary"    )       ),

        new Code(STATUS_TIRE_TEMP_RANGE     , "TIRE.TEMP"    , I18N.getString(StatusCodes.class,"StatusCodes.tireTempRange"   ,"Tire_Temp_Range" ), true ),
        new Code(STATUS_TIRE_PRESSURE_RANGE , "TIRE.PRESSURE", I18N.getString(StatusCodes.class,"StatusCodes.tirePressRange"  ,"Tire_Pressure"   ), true ),
        new Code(STATUS_TIRE_PRESSURE_LOW   , "TIRE.LOW"     , I18N.getString(StatusCodes.class,"StatusCodes.tirePressLow"    ,"Tire_Low"        ), true ),
        new Code(STATUS_TIRE_BATTERY_LOW    , "TIRE.BATTERY" , I18N.getString(StatusCodes.class,"StatusCodes.tireBatteryLow"  ,"Tire_Battery_Low"), true ),

        new Code(STATUS_IP_ADDRESS          , "IP.ADDRESS"   , I18N.getString(StatusCodes.class,"StatusCodes.ipAddress"       ,"IP_Address"      )       ),
        new Code(STATUS_SIM_CARD            , "SIM.CARD"     , I18N.getString(StatusCodes.class,"StatusCodes.simCard  "       ,"SIM_Card"        )       ),
        new Code(STATUS_BATTERY_VOLTS       , "BATT.VOLTS"   , I18N.getString(StatusCodes.class,"StatusCodes.battVolts"       ,"Battery_Volts"   )       ),
        new Code(STATUS_BACKUP_VOLTS        , "BATT.BACKUP"  , I18N.getString(StatusCodes.class,"StatusCodes.battBackup"      ,"Backup_Volts"    )       ),
        new Code(STATUS_BATT_CHARGE_ON      , "BATT.CHARGE"  , I18N.getString(StatusCodes.class,"StatusCodes.battCharging"    ,"Battery_Charge"  )       ),
        new Code(STATUS_BATT_CHARGE_OFF     , "BATT.FULL"    , I18N.getString(StatusCodes.class,"StatusCodes.battChargeOff"   ,"Charge_Complete" )       ),
        new Code(STATUS_LOW_BATTERY         , "BATT.LOW"     , I18N.getString(StatusCodes.class,"StatusCodes.lowBattery"      ,"Low_Battery"     ), true ),
        new Code(STATUS_BATTERY_LEVEL       , "BATT.LEVEL"   , I18N.getString(StatusCodes.class,"StatusCodes.batteryLevel"    ,"Battery_Level"   )       ),
        new Code(STATUS_POWER_FAILURE       , "POWERFAIL"    , I18N.getString(StatusCodes.class,"StatusCodes.powerFail"       ,"Power_Disconnect"), true ), // (off) 
        new Code(STATUS_POWER_ALARM         , "POWERALARM"   , I18N.getString(StatusCodes.class,"StatusCodes.powerAlarm"      ,"Power_Alarm"     ), true ),
        new Code(STATUS_POWER_RESTORED      , "POWERRESTORE" , I18N.getString(StatusCodes.class,"StatusCodes.powerRestore"    ,"Power_Connect"   ), true ), // (on ) 
        new Code(STATUS_POWER_OFF           , "POWEROFF"     , I18N.getString(StatusCodes.class,"StatusCodes.powerOff"        ,"Power_Off"       ), true ), // (off) 
        new Code(STATUS_POWER_ON            , "POWERON"      , I18N.getString(StatusCodes.class,"StatusCodes.powerOn"         ,"Power_On"        ), true ), // (on ) 
        new Code(STATUS_GPS_EXPIRED         , "GPS.EXPIRE"   , I18N.getString(StatusCodes.class,"StatusCodes.gpsExpired"      ,"GPS_Expired"     )       ),
        new Code(STATUS_GPS_FAILURE         , "GPS.FAILURE"  , I18N.getString(StatusCodes.class,"StatusCodes.gpsFailure"      ,"GPS_Failure"     ), true ), // (off) 
        new Code(STATUS_GPS_ANTENNA_OPEN    , "GPS.ANT.OPEN" , I18N.getString(StatusCodes.class,"StatusCodes.gpsAntennaOpen"  ,"GPS_AntOpen"     ), true ),
        new Code(STATUS_GPS_ANTENNA_SHORT   , "GPS.ANT.SHORT", I18N.getString(StatusCodes.class,"StatusCodes.gpsAntennaShort" ,"GPS_AntShort"    ), true ),
        new Code(STATUS_GPS_JAMMING         , "GPS.JAMMING"  , I18N.getString(StatusCodes.class,"StatusCodes.gpsJamming"      ,"GPS_Jamming"     ), true ),
        new Code(STATUS_GPS_RESTORED        , "GPS.RESTORED" , I18N.getString(StatusCodes.class,"StatusCodes.gpsRestored"     ,"GPS_Restored"    ), true ), // (on ) 
        new Code(STATUS_GPS_LOST            , "GPS.LOST"     , I18N.getString(StatusCodes.class,"StatusCodes.gpsLost"         ,"GPS_Lost"        ), true ), // (off) 
        new Code(STATUS_DIAGNOSTIC          , "DIAGNOSTIC"   , I18N.getString(StatusCodes.class,"StatusCodes.diagnostic"      ,"Diagnostic"      )       ),
        new Code(STATUS_CONNECTION_FAILURE  , "CONN.FAILURE" , I18N.getString(StatusCodes.class,"StatusCodes.connectFailure"  ,"Connect_Failure" ), true ), // (off) 
        new Code(STATUS_CONNECTION_RESTORED , "CONN.RESTORE" , I18N.getString(StatusCodes.class,"StatusCodes.connectRestore"  ,"Connect_Restore" )       ), // (on ) 
        new Code(STATUS_MODEM_FAILURE       , "MODEM.FAILURE", I18N.getString(StatusCodes.class,"StatusCodes.modemFailure"    ,"Modem_Failure"   ), true ),
        new Code(STATUS_INTERNAL_FAILURE    , "INTRN.FAILURE", I18N.getString(StatusCodes.class,"StatusCodes.internFailure"   ,"Intern_Failure"  ), true ),
        new Code(STATUS_MODEM_JAMMING       , "MODEM.JAMMING", I18N.getString(StatusCodes.class,"StatusCodes.modemJamming"    ,"Modem_Jamming"   ), true ),
        new Code(STATUS_MODEM_RESTORED      , "MODEM.RESTORE", I18N.getString(StatusCodes.class,"StatusCodes.modemRestored"   ,"Modem_Restored"  ), true ),
        new Code(STATUS_CONFIG_RESET        , "CFG.RESET"    , I18N.getString(StatusCodes.class,"StatusCodes.configReset"     ,"Config_Reset"    ), true ),
        new Code(STATUS_CONFIG_START        , "CFG.START"    , I18N.getString(StatusCodes.class,"StatusCodes.configStart"     ,"Config_Start"    ), true ), // (on ) 
        new Code(STATUS_CONFIG_COMPLETE     , "CFG.COMPLETE" , I18N.getString(StatusCodes.class,"StatusCodes.configComplete"  ,"Config_Complete" ), true ), // (off) 
        new Code(STATUS_CONFIG_FAILED       , "CFG.FAILED"   , I18N.getString(StatusCodes.class,"StatusCodes.configFailed"    ,"Config_Failed"   ), true ),
        new Code(STATUS_SHUTDOWN            , "SHUTDOWN"     , I18N.getString(StatusCodes.class,"StatusCodes.shutdown"        ,"Shutdown"        ), true ),
        new Code(STATUS_SUSPEND             , "SUSPEND"      , I18N.getString(StatusCodes.class,"StatusCodes.suspend"         ,"Suspend"         ), true ), // (off) 
        new Code(STATUS_RESUME              , "RESUME"       , I18N.getString(StatusCodes.class,"StatusCodes.resume"          ,"Resume"          ), true ), // (on ) 
        new Code(STATUS_ROAMING_ON          , "ROAMING.ON"   , I18N.getString(StatusCodes.class,"StatusCodes.roamingOn"       ,"Roaming"         )       ), // (on ) 
        new Code(STATUS_ROAMING_OFF         , "ROAMING.OFF"  , I18N.getString(StatusCodes.class,"StatusCodes.roamingOff"      ,"Roaming Off"     )       ), // (off) 
        new Code(STATUS_ROAMING_UNKNOWN     , "ROAMING.UNKN" , I18N.getString(StatusCodes.class,"StatusCodes.roamingUnknown"  ,"Roaming Unknown" )       ), // (???) 

        new Code(STATUS_IMAGE_0             , "IMAGE.0"      , I18N.getString(StatusCodes.class,"StatusCodes.image_0"         ,"Image_0"         )       ),
        new Code(STATUS_IMAGE_1             , "IMAGE.1"      , I18N.getString(StatusCodes.class,"StatusCodes.image_1"         ,"Image_1"         )       ),
        new Code(STATUS_IMAGE_2             , "IMAGE.2"      , I18N.getString(StatusCodes.class,"StatusCodes.image_2"         ,"Image_2"         )       ),
        new Code(STATUS_IMAGE_3             , "IMAGE.3"      , I18N.getString(StatusCodes.class,"StatusCodes.image_3"         ,"Image_3"         )       ),
        new Code(STATUS_IMAGE_LOC_0         , "IMAGE.LOC.0"  , I18N.getString(StatusCodes.class,"StatusCodes.imageLoc_0"      ,"ImageLoc_0"      )       ),
        new Code(STATUS_IMAGE_LOC_1         , "IMAGE.LOC.1"  , I18N.getString(StatusCodes.class,"StatusCodes.imageLoc_1"      ,"ImageLoc_1"      )       ),
        new Code(STATUS_IMAGE_LOC_2         , "IMAGE.LOC.2"  , I18N.getString(StatusCodes.class,"StatusCodes.imageLoc_2"      ,"ImageLoc_2"      )       ),
        new Code(STATUS_IMAGE_LOC_3         , "IMAGE.LOC.3"  , I18N.getString(StatusCodes.class,"StatusCodes.imageLoc_3"      ,"ImageLoc_3"      )       ),

        new Code(STATUS_RULE_TRIGGER_0      , "RULE.0"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_0"   ,"Rule_0"          ), true ),
        new Code(STATUS_RULE_TRIGGER_1      , "RULE.1"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_1"   ,"Rule_1"          ), true ),
        new Code(STATUS_RULE_TRIGGER_2      , "RULE.2"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_2"   ,"Rule_2"          ), true ),
        new Code(STATUS_RULE_TRIGGER_3      , "RULE.3"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_3"   ,"Rule_3"          ), true ),
        new Code(STATUS_RULE_TRIGGER_4      , "RULE.4"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_4"   ,"Rule_4"          ), true ),
        new Code(STATUS_RULE_TRIGGER_5      , "RULE.5"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_5"   ,"Rule_5"          ), true ),
        new Code(STATUS_RULE_TRIGGER_6      , "RULE.6"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_6"   ,"Rule_6"          ), true ),
        new Code(STATUS_RULE_TRIGGER_7      , "RULE.7"       , I18N.getString(StatusCodes.class,"StatusCodes.ruleTrigger_7"   ,"Rule_7"          ), true ),

    };

    /* static status code maps */
    private static volatile OrderedMap<Integer,Code> statusCodeMap = null;
    private static volatile OrderedMap<String ,Code> statusNameMap = null;
    private static volatile boolean                  statusMapInit = false;

    /* init StatusCode map (must be synchronized on "_codeArray" */
    private static void _initStatusCodeMap(int inclCodes[])
    {
        OrderedMap<Integer,Code> scMap  = new OrderedMap<Integer,Code>();
        OrderedMap<String ,Code> snMap  = new OrderedMap<String ,Code>();

        /* always add STATUS_NONE first (OrderedMap) */
        if ((inclCodes != null) && (inclCodes.length > 0)) {
            Print.logDebug("Initializing specific StatusCodes ...");
            scMap.put(new Integer(_codeArray[0].getCode()), _codeArray[0]);
            snMap.put(_codeArray[0].getName()             , _codeArray[0]);
        }

        /* add StatusCodes */
        for (int c = 0; c < _codeArray.length; c++) {
            Code    code = _codeArray[c];
            int     sc   = code.getCode();
            Integer sci  = new Integer(sc);
            String  scn  = code.getName();
            if ((inclCodes == null) || (inclCodes.length == 0)) {
                scMap.put(sci, code); // add all codes
                snMap.put(scn, code); // add all codes
            } else {
                for (int i = 0; (i < inclCodes.length); i++) {
                    if ((inclCodes[i] != STATUS_NONE) && (inclCodes[i] == sc)) {
                        //Print.logDebug("  ==> " + code);
                        scMap.put(sci, code); // add specific code
                        snMap.put(scn, code); // add specific code
                        break;
                    }
                }
            }
        }

        /* set status code map */
        statusCodeMap = scMap;
        statusNameMap = snMap;
        statusMapInit = true;

    }

    /* internal status code to 'Code' map */
    private static void _initStatusCodes()
    {
        if (!statusMapInit) { // only iff not yet initialized
            // Calling "StatusCodes.initStatusCodeMap(...)" at startup is preferred.
            Print.logInfo("StatusCodes late initialization ...");
            synchronized (_codeArray) {
                if (!statusMapInit) { // check again after lock
                    _initStatusCodeMap(null);
                }
            }
        }
    }
    
    /* internal status code to 'Code' map */
    private static OrderedMap<Integer,Code> _GetStatusCodeMap()
    {
        if (!statusMapInit) { // only iff not yet initialized
            _initStatusCodes();
        }
        return statusCodeMap;
    }

    /* internal status code to 'Name' map */
    private static OrderedMap<String,Code> _GetStatusNameMap()
    {
        if (!statusMapInit) { // only iff not yet initialized
            _initStatusCodes();
        }
        return statusNameMap;
    }

    /**
    *** (Re)Initialize status code descriptions
    *** @param inclCodes  An array of codes to include in the status code list, null to include all codes
    **/
    public static void initStatusCodes(int inclCodes[])
    {
        synchronized (_codeArray) {
            if (statusMapInit) { // already initialized?
                Print.logWarn("Re-initializing StatusCode map");
            }
            _initStatusCodeMap(inclCodes);
        }
    }

    // ------------------------------------------------------------------------

    /* add code to map */
    public static void AddCode(Code code)
    {
        if (code != null) {
            Map<Integer,Code> map = _GetStatusCodeMap();
            synchronized (_codeArray) {
                map.put(new Integer(code.getCode()), code);
            }
        }
    }

    /* add codes to map */
    public static void AddCodes(Code codeList[])
    {
        if (codeList != null) {
            for (int c = 0; c < codeList.length; c++) {
                AddCode(codeList[c]);
            }
        }
    }

    /* remove code from map */
    public static Code RemoveCode(int sc)
    {
        Map<Integer,Code> map = _GetStatusCodeMap();
        Integer sci  = new Integer(sc);
        Code    code = map.get(sci);
        if (code != null) {
            synchronized (_codeArray) {
                map.remove(sci);
            }
        }
        return code;
    }

    /* remove list of code from map */
    public static void RemoveCodes(int cList[])
    {
        if (cList != null) {
            for (int c = 0; c < cList.length; c++) {
                RemoveCode(cList[c]);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates and returns a new OrderedSet of all defined status codes 
    *** @return A Set of defined status codes
    **/
    public static OrderedSet<Integer> GetStatusCodeList()
    {
        OrderedMap<Integer,Code> codeMap = _GetStatusCodeMap();
        OrderedSet<Integer>      codeSet = new OrderedSet<Integer>();
        ListTools.toSet(codeMap.keySet(), codeSet);
        return codeSet;
    }

    /**
    *** Creates and returns a new OrderedMap containing StatusCode/Description pairs
    *** @param locale  The Locale
    *** @return A Map of defined StatusCode/Description pairs
    **/
    public static OrderedMap<Integer,String> GetDescriptionMap(Locale locale)
    {
        OrderedMap<Integer,String> descMap = new OrderedMap<Integer,String>();
        Map<Integer,Code> codeMap = _GetStatusCodeMap();
        for (Integer sci : codeMap.keySet()) {
            Code c = codeMap.get(sci);
            descMap.put(sci, c.getDescription(locale));
        }
        return descMap;
    }

    // ------------------------------------------------------------------------

    /* return specific code (from statusCode) */
    public static Code GetCode(int code, BasicPrivateLabel bpl)
    {
        Integer ic = new Integer(code);
        if (bpl != null) {
            Code sc = bpl.getStatusCode(ic);
            if (sc != null) {
                return sc;
            }
        }
        return _GetStatusCodeMap().get(ic);
    }

    /* return specific code (from statusCode) */
    public static Code GetCode(String name, BasicPrivateLabel bpl)
    {
        if (!StringTools.isBlank(name)) {
            String n = name.toUpperCase();
            Code   c = _GetStatusNameMap().get(n);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses/returns the integer status code for the specified status code name
    *** @param codeStr The status code name
    *** @param bpl     The BasicPrivateLabel instance
    *** @param dftCode The default status code value to return if the "codeStr" cannot be parsed
    *** @return The status code
    **/
    public static int ParseCode(String codeStr, BasicPrivateLabel bpl, int dftCode)
    {

        /* null/empty code name */
        if (StringTools.isBlank(codeStr)) {
            return dftCode;
        }

        /* lookup name */
        StatusCodes.Code code = StatusCodes.GetCode(codeStr, bpl);
        if (code != null) {
            return code.getCode();
        }

        /* status code number? */
        if (StringTools.isInt(codeStr,true)) {
            return StringTools.parseInt(codeStr, dftCode);
        }

        /* unknown */
        return dftCode;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the name of the specified code
    *** @param code The status code
    *** @param bpl  The BasicPrivateLabel instance
    *** @return The name
    **/
    public static String GetName(int code, BasicPrivateLabel bpl)
    {
        Code sc = StatusCodes.GetCode(code, bpl);
        if (sc != null) {
            return sc.getName();
        } else {
            return "0x" + StringTools.toHexString((long)code,16);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the StatusCodeProvider of the specified code
    *** @param code The status code
    *** @param bpl  The BasicPrivateLabel instance
    *** @return The StatusCodeProvider
    **/
    public static StatusCodeProvider GetStatusCodeProvider(int code, BasicPrivateLabel bpl)
    {
        Code sc = StatusCodes.GetCode(code, bpl);
        if (sc != null) {
            return sc;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the description of the specified code
    *** @param code The status code
    *** @param desc The status code description
    *** @return True if the specified status code is defined, false otherwise
    **/
    public static boolean SetDescription(int code, String desc)
    {
        Code sc = StatusCodes.GetCode(code, null);
        if (sc != null) {
            sc.setDescription(desc);
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Returns the description of the specified code
    *** @param code The status code
    *** @param bpl  The BasicPrivateLabel instance
    *** @return The description
    **/
    public static String GetDescription(int code, BasicPrivateLabel bpl)
    {
        Code sc = StatusCodes.GetCode(code, bpl);
        if (sc != null) {
            Locale locale = (bpl != null)? bpl.getLocale() : null; // should be "reqState.getLocale()"
            return sc.getDescription(locale);
        } else {
            String codeDesc = "0x" + StringTools.toHexString((long)code,16);
            //Print.logWarn("Code not found: " + codeDesc);
            return codeDesc;
        }
    }

    /**
    *** Returns the description of the specified code
    *** @param code The status code
    *** @param bpl  The BasicPrivateLabel instance
    *** @return The description
    **/
    public static String GetDesc(int code, BasicPrivateLabel bpl)
    {
        return StatusCodes.GetDescription(code, bpl);
    }

    /**
    *** Returns the description of the specified code
    *** @param code The status code
    *** @return The description
    **/
    public static String GetDesc(int code)
    {
        return StatusCodes.GetDescription(code, null);
    }

    /**
    *** Returns the String representation of the specified code
    *** @param code The status code
    *** @return The String representation
    **/
    public static String ToString(int code)
    {
        BasicPrivateLabel pl = null;
        Code scode = StatusCodes.GetCode(code, pl);
        if (scode != null) {
            return scode.toString();
        } else {
            return StatusCodes.GetHex(code);
        }
    }
 
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static boolean SetIconName(int code, String iconName)
    {
        Code sc = StatusCodes.GetCode(code, null);
        if (sc != null) {
            sc.setIconName(iconName);
            return true;
        } else {
            return false;
        }
    }

    public static String GetIconName(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        if (sc != null) {
            return sc.getIconName();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the String hex representation of the specified status code
    *** @param code The status code
    *** @return The String hex representation of the status code
    **/
    public static String GetHex(int code)
    {
        return StatusCodes.GetHex((long)code);
    }

    /**
    *** Returns the String hex representation of the specified status code
    *** @param code The status code
    *** @return The String hex representation of the status code
    **/
    public static String GetHex(long code)
    {
        if (code < 0L) {
            return "0x" + StringTools.toHexString(code, 32);
        } else
        if ((code & ~0xFFFFL) != 0) {
            return "0x" + StringTools.toHexString((code & 0x7FFFFFFFL), 32);
        } else {
            return "0x" + StringTools.toHexString((code & 0xFFFFL), 16);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified status code is reserved, false otherwise
    *** @param code The status code
    *** @return True if the specified status code is reserved, false otherwise
    **/
    public static boolean IsReserved(int code)
    {
        return ((code >= 0xE000) && (code <= 0xFFFF));
    }

    /**
    *** Returns true if the specified status code is valid (defined)
    *** @param code The status code
    *** @param pl   The BasicPrivateLabel instance
    *** @return True if the specified status code is valid
    **/
    public static boolean IsValid(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        return (sc != null);
    }

    /**
    *** Returns true if the specified status code is high-priority
    *** @param code The status code
    *** @param pl   The BasicPrivateLabel instance
    *** @return True if the specified status code is high-priority
    **/
    public static boolean IsHighPriority(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        return (sc != null)? sc.isHighPriority() : false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* GTS status codes for Input-On events */
    public static final int InputStatusCodes_ON[] = new int[] {
        StatusCodes.STATUS_INPUT_ON_00,
        StatusCodes.STATUS_INPUT_ON_01,
        StatusCodes.STATUS_INPUT_ON_02,
        StatusCodes.STATUS_INPUT_ON_03,
        StatusCodes.STATUS_INPUT_ON_04,
        StatusCodes.STATUS_INPUT_ON_05,
        StatusCodes.STATUS_INPUT_ON_06,
        StatusCodes.STATUS_INPUT_ON_07,
        StatusCodes.STATUS_INPUT_ON_08,
        StatusCodes.STATUS_INPUT_ON_09,
        StatusCodes.STATUS_INPUT_ON_10,
        StatusCodes.STATUS_INPUT_ON_11,
        StatusCodes.STATUS_INPUT_ON_12,
        StatusCodes.STATUS_INPUT_ON_13,
        StatusCodes.STATUS_INPUT_ON_14,
        StatusCodes.STATUS_INPUT_ON_15,
    };

    /* GTS status codes for Input-Off events */
    public static final int InputStatusCodes_OFF[] = new int[] {
        StatusCodes.STATUS_INPUT_OFF_00,
        StatusCodes.STATUS_INPUT_OFF_01,
        StatusCodes.STATUS_INPUT_OFF_02,
        StatusCodes.STATUS_INPUT_OFF_03,
        StatusCodes.STATUS_INPUT_OFF_04,
        StatusCodes.STATUS_INPUT_OFF_05,
        StatusCodes.STATUS_INPUT_OFF_06,
        StatusCodes.STATUS_INPUT_OFF_07,
        StatusCodes.STATUS_INPUT_OFF_08,
        StatusCodes.STATUS_INPUT_OFF_09,
        StatusCodes.STATUS_INPUT_OFF_10,
        StatusCodes.STATUS_INPUT_OFF_11,
        StatusCodes.STATUS_INPUT_OFF_12,
        StatusCodes.STATUS_INPUT_OFF_13,
        StatusCodes.STATUS_INPUT_OFF_14,
        StatusCodes.STATUS_INPUT_OFF_15,
    };

    /**
    *** Gets the index of the specified DigitalInput status code, or "-1" if the 
    *** specified value does not represent a DigitalInput status code
    *** @param code The status code
    *** @return The DigitalInput status code index
    **/
    public static int GetDigitalInputIndex(int code)
    {
        switch (code) {
            case STATUS_INPUT_ON_00          :
            case STATUS_INPUT_OFF_00         :
                return 0;
            case STATUS_INPUT_ON_01          :
            case STATUS_INPUT_OFF_01         :
                return 1;
            case STATUS_INPUT_ON_02          :
            case STATUS_INPUT_OFF_02         :
                return 2;
            case STATUS_INPUT_ON_03          :
            case STATUS_INPUT_OFF_03         :
                return 3;
            case STATUS_INPUT_ON_04          :
            case STATUS_INPUT_OFF_04         :
                return 4;
            case STATUS_INPUT_ON_05          :
            case STATUS_INPUT_OFF_05         :
                return 5;
            case STATUS_INPUT_ON_06          :
            case STATUS_INPUT_OFF_06         :
                return 6;
            case STATUS_INPUT_ON_07          :
            case STATUS_INPUT_OFF_07         :
                return 7;
            case STATUS_INPUT_ON_08          :
            case STATUS_INPUT_OFF_08         :
                return 8;
            case STATUS_INPUT_ON_09          :
            case STATUS_INPUT_OFF_09         :
                return 9;
            case STATUS_INPUT_ON_10          :
            case STATUS_INPUT_OFF_10         :
                return 10;
            case STATUS_INPUT_ON_11          :
            case STATUS_INPUT_OFF_11         :
                return 11;
            case STATUS_INPUT_ON_12          :
            case STATUS_INPUT_OFF_12         :
                return 12;
            case STATUS_INPUT_ON_13          :
            case STATUS_INPUT_OFF_13         :
                return 13;
            case STATUS_INPUT_ON_14          :
            case STATUS_INPUT_OFF_14         :
                return 14;
            case STATUS_INPUT_ON_15          :
            case STATUS_INPUT_OFF_15         :
                return 15;
            case STATUS_IGNITION_ON          :
            case STATUS_IGNITION_OFF         :
                return IGNITION_INPUT_INDEX;
        }
        return -1;
    }

    // --------------

    /**
    *** Returns true if the status code represents an Ignition state
    *** @param code The status code
    *** @return True if the status code represents an Ignition state
    **/
    public static boolean IsIgnition(int code)
    {
        switch (code) {
            case STATUS_IGNITION_ON          :
            case STATUS_IGNITION_OFF         :
                return true;
        }
        return false;
    }

    // --------------

    /**
    *** Returns true if the status code represents a DigitalInput "on" state
    *** @param code The status code
    *** @return True if the status code represents an DigitalInput "on" state
    **/
    public static boolean IsDigitalInputOn(int code, boolean inclIgn)
    {

        /* check code */
        switch (code) {
            case STATUS_INPUT_STATE          :
                return true; // always true (since we don't know the actual state)
            case STATUS_IGNITION_ON          :
                return inclIgn;  // ignition is an explicit event type
            case STATUS_INPUT_ON             :
            case STATUS_INPUT_ON_00          :
            case STATUS_INPUT_ON_01          :
            case STATUS_INPUT_ON_02          :
            case STATUS_INPUT_ON_03          :
            case STATUS_INPUT_ON_04          :
            case STATUS_INPUT_ON_05          :
            case STATUS_INPUT_ON_06          :
            case STATUS_INPUT_ON_07          :
            case STATUS_INPUT_ON_08          :
            case STATUS_INPUT_ON_09          :
            case STATUS_INPUT_ON_10          :
            case STATUS_INPUT_ON_11          :
            case STATUS_INPUT_ON_12          :
            case STATUS_INPUT_ON_13          :
            case STATUS_INPUT_ON_14          :
            case STATUS_INPUT_ON_15          :
                return true;
        }
        
        /* not a digital input */
        return false;
        
    }

    // --------------

    /**
    *** Returns true if the status code represents a DigitalInput "off" state
    *** @param code The status code
    *** @return True if the status code represents an DigitalInput "off" state
    **/
    public static boolean IsDigitalInputOff(int code, boolean inclIgn)
    {
        switch (code) {
            case STATUS_INPUT_STATE          :
                return true; // always true (since we don't know the actual state)
            case STATUS_IGNITION_OFF         :
                return inclIgn; // ignition is an explicit event type
            case STATUS_INPUT_OFF            :
            case STATUS_INPUT_OFF_00         :
            case STATUS_INPUT_OFF_01         :
            case STATUS_INPUT_OFF_02         :
            case STATUS_INPUT_OFF_03         :
            case STATUS_INPUT_OFF_04         :
            case STATUS_INPUT_OFF_05         :
            case STATUS_INPUT_OFF_06         :
            case STATUS_INPUT_OFF_07         :
            case STATUS_INPUT_OFF_08         :
            case STATUS_INPUT_OFF_09         :
            case STATUS_INPUT_OFF_10         :
            case STATUS_INPUT_OFF_11         :
            case STATUS_INPUT_OFF_12         :
            case STATUS_INPUT_OFF_13         :
            case STATUS_INPUT_OFF_14         :
            case STATUS_INPUT_OFF_15         :
                return true;
        }
        return false;
    }

    // --------------

    /**
    *** Returns true if the status code represents a DigitalInput code
    *** @param code     The status code
    *** @param inclIgn  True to include ignition state 
    *** @return True if the status code represents an DigitalInput code
    **/
    public static boolean IsDigitalInput(int code, boolean inclIgn)
    {
        return IsDigitalInputOn( code, inclIgn) || 
               IsDigitalInputOff(code, inclIgn);
    }

    /**
    *** Returns the DigitalInput status code for the specified index and state
    *** @param ndx    The DigitalInput index
    *** @param state  The DigitalInput state (true="on", false="off")
    *** @return The DigitalInput status code
    ***/
    public static int GetDigitalInputStatusCode(int ndx, boolean state)
    {
        if (ndx < 0) {
            return STATUS_NONE;
        } else
        if (ndx == IGNITION_INPUT_INDEX) {
            return state? STATUS_IGNITION_ON : STATUS_IGNITION_OFF;
        }
        switch (ndx) {
            case   0: return state? STATUS_INPUT_ON_00 : STATUS_INPUT_OFF_00;
            case   1: return state? STATUS_INPUT_ON_01 : STATUS_INPUT_OFF_01;
            case   2: return state? STATUS_INPUT_ON_02 : STATUS_INPUT_OFF_02;
            case   3: return state? STATUS_INPUT_ON_03 : STATUS_INPUT_OFF_03;
            case   4: return state? STATUS_INPUT_ON_04 : STATUS_INPUT_OFF_04;
            case   5: return state? STATUS_INPUT_ON_05 : STATUS_INPUT_OFF_05;
            case   6: return state? STATUS_INPUT_ON_06 : STATUS_INPUT_OFF_06;
            case   7: return state? STATUS_INPUT_ON_07 : STATUS_INPUT_OFF_07;
            case   8: return state? STATUS_INPUT_ON_08 : STATUS_INPUT_OFF_08;
            case   9: return state? STATUS_INPUT_ON_09 : STATUS_INPUT_OFF_09;
            case  10: return state? STATUS_INPUT_ON_10 : STATUS_INPUT_OFF_10;
            case  11: return state? STATUS_INPUT_ON_11 : STATUS_INPUT_OFF_11;
            case  12: return state? STATUS_INPUT_ON_12 : STATUS_INPUT_OFF_12;
            case  13: return state? STATUS_INPUT_ON_13 : STATUS_INPUT_OFF_13;
            case  14: return state? STATUS_INPUT_ON_14 : STATUS_INPUT_OFF_14;
            case  15: return state? STATUS_INPUT_ON_15 : STATUS_INPUT_OFF_15;
        }
        return STATUS_NONE;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* GTS status codes for Output-On events */
    public static final int OutputStatusCodes_ON[] = new int[] {
        StatusCodes.STATUS_OUTPUT_ON_00,
        StatusCodes.STATUS_OUTPUT_ON_01,
        StatusCodes.STATUS_OUTPUT_ON_02,
        StatusCodes.STATUS_OUTPUT_ON_03,
        StatusCodes.STATUS_OUTPUT_ON_04,
        StatusCodes.STATUS_OUTPUT_ON_05,
        StatusCodes.STATUS_OUTPUT_ON_06,
        StatusCodes.STATUS_OUTPUT_ON_07,
        StatusCodes.STATUS_OUTPUT_ON_08,
        StatusCodes.STATUS_OUTPUT_ON_09,
    };

    /* GTS status codes for Output-Off events */
    public static final int OutputStatusCodes_OFF[] = new int[] {
        StatusCodes.STATUS_OUTPUT_OFF_00,
        StatusCodes.STATUS_OUTPUT_OFF_01,
        StatusCodes.STATUS_OUTPUT_OFF_02,
        StatusCodes.STATUS_OUTPUT_OFF_03,
        StatusCodes.STATUS_OUTPUT_OFF_04,
        StatusCodes.STATUS_OUTPUT_OFF_05,
        StatusCodes.STATUS_OUTPUT_OFF_06,
        StatusCodes.STATUS_OUTPUT_OFF_07,
        StatusCodes.STATUS_OUTPUT_OFF_08,
        StatusCodes.STATUS_OUTPUT_OFF_09,
    };

    /**
    *** Returns the DigitalOutput status code for the specified index and state
    *** @param ndx    The DigitalOutput index
    *** @param state  The DigitalOutput state (true="on", false="off")
    *** @return The DigitalOutput status code
    ***/
    public static int GetDigitalOutputStatusCode(int ndx, boolean state)
    {
        if (ndx < 0) {
            return STATUS_NONE;
        }
        switch (ndx) {
            case   0: return state? STATUS_OUTPUT_ON_00 : STATUS_OUTPUT_OFF_00;
            case   1: return state? STATUS_OUTPUT_ON_01 : STATUS_OUTPUT_OFF_01;
            case   2: return state? STATUS_OUTPUT_ON_02 : STATUS_OUTPUT_OFF_02;
            case   3: return state? STATUS_OUTPUT_ON_03 : STATUS_OUTPUT_OFF_03;
            case   4: return state? STATUS_OUTPUT_ON_04 : STATUS_OUTPUT_OFF_04;
            case   5: return state? STATUS_OUTPUT_ON_05 : STATUS_OUTPUT_OFF_05;
            case   6: return state? STATUS_OUTPUT_ON_06 : STATUS_OUTPUT_OFF_06;
            case   7: return state? STATUS_OUTPUT_ON_07 : STATUS_OUTPUT_OFF_07;
            case   8: return state? STATUS_OUTPUT_ON_08 : STATUS_OUTPUT_OFF_08;
            case   9: return state? STATUS_OUTPUT_ON_09 : STATUS_OUTPUT_OFF_09;
        }
        return STATUS_NONE;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the status code represents a RuleTrigger code
    *** @param code The status code
    *** @return True if the status code represents a RuleTrigger code
    **/
    public static boolean IsRuleTrigger(int code)
    {
        switch (code) {
            case STATUS_RULE_TRIGGER_0       :
            case STATUS_RULE_TRIGGER_1       :
            case STATUS_RULE_TRIGGER_2       :
            case STATUS_RULE_TRIGGER_3       :
            case STATUS_RULE_TRIGGER_4       :
            case STATUS_RULE_TRIGGER_5       :
            case STATUS_RULE_TRIGGER_6       :
            case STATUS_RULE_TRIGGER_7       :
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the status code represents a Location code
    *** @param code The status code
    *** @return True if the status code represents a Location code
    **/
    public static boolean IsLocation(int code)
    {
        switch (code) {
            case STATUS_LOCATION   :
            case STATUS_LOCATION_1 :
            case STATUS_LOCATION_2 :
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the status code represents a Location code
    *** @param code The status code
    *** @return True if the status code represents a Location code
    **/
    public static boolean IsSpeeding(int code)
    {
        switch (code) {
            case STATUS_MOTION_OVER_SPEED_1 :
            case STATUS_MOTION_OVER_SPEED_2 :
            case STATUS_SPEEDING_BEGIN      :
            case STATUS_SPEEDING_LIMIT_1    :
            case STATUS_SPEEDING_LIMIT_2    :
            case STATUS_SPEEDING_LIMIT_3    :
            case STATUS_SPEEDING_LIMIT_4    :
          //case STATUS_SPEEDING_LIMIT_5    :
            case STATUS_SPEEDING_END        :
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the status code represents a GeozoneTransition code
    *** @param code The status code
    *** @return True if the status code represents a GeozoneTransition code
    **/
    public static boolean IsGeozoneTransition(int code)
    {
        switch (code) {
            case STATUS_GEOFENCE_ARRIVE      :
            case STATUS_GEOFENCE_DEPART      :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a GeozoneArrive code
    *** @param code The status code
    *** @return True if the status code represents a GeozoneArrive code
    **/
    public static boolean IsGeozoneArrive(int code)
    {
        switch (code) {
            case STATUS_GEOFENCE_ARRIVE      :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a GeozoneDepart code
    *** @param code The status code
    *** @return True if the status code represents a GeozoneDepart code
    **/
    public static boolean IsGeozoneDepart(int code)
    {
        switch (code) {
            case STATUS_GEOFENCE_DEPART      :
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the status code represents a EntityState code
    *** @param code The status code
    *** @return True if the status code represents a EntityState code
    **/
    public static boolean IsEntityState(int code)
    {
        switch (code) {
            case STATUS_ENTITY_STATE    :
            case STATUS_TRAILER_STATE   :
            case STATUS_RFID_STATE      :
            case STATUS_PERSON_STATE    :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a EntityConnect code
    *** @param code The status code
    *** @return True if the status code represents a EntityConnect code
    **/
    public static boolean IsEntityConnect(int code)
    {
        switch (code) {
            case STATUS_ENTITY_CONNECT  :
            case STATUS_TRAILER_HOOK    :
            case STATUS_LOGIN           :
            case STATUS_RFID_CONNECT    :
            case STATUS_PERSON_ENTER    :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a EntityDisconnect code
    *** @param code The status code
    *** @return True if the status code represents a EntityDisconnect code
    **/
    public static boolean IsEntityDisconnect(int code)
    {
        switch (code) {
            case STATUS_ENTITY_DISCONNECT:
            case STATUS_TRAILER_UNHOOK   :
            case STATUS_LOGOUT           :
            case STATUS_RFID_DISCONNECT  :
            case STATUS_PERSON_EXIT      :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a EntityInventory code
    *** @param code The status code
    *** @return True if the status code represents a EntityInventory code
    **/
    public static boolean IsEntityInventory(int code)
    {
        switch (code) {
            case STATUS_ENTITY_INVENTORY :
            case STATUS_TRAILER_INVENTORY:
            case STATUS_RFID_INVENTORY   :
            case STATUS_PERSON_INVENTORY :
                return true;
        }
        return false;
    }

    // --------------

    /**
    *** Returns true if the status code represents a Acceleration state
    *** @param code The status code
    *** @return True if the status code represents an Acceleration state
    **/
    public static boolean IsAcceleration(int code)
    {
        switch (code) {
            case STATUS_MOTION_ACCELERATION:
            case STATUS_EXCESS_ACCEL       :
            case STATUS_EXCESS_ACCEL_2     :
            case STATUS_EXCESS_ACCEL_3     :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a Deceleration state
    *** @param code The status code
    *** @return True if the status code represents an Deceleration state
    **/
    public static boolean IsDeceleration(int code)
    {
        switch (code) {
            case STATUS_MOTION_DECELERATION:
            case STATUS_EXCESS_BRAKING     :
            case STATUS_EXCESS_BRAKING_2   :
            case STATUS_EXCESS_BRAKING_3   :
            case STATUS_IMPACT             :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a Braking state
    *** @param code The status code
    *** @return True if the status code represents an Braking state
    **/
    public static boolean IsBraking(int code)
    {
        switch (code) {
            case STATUS_EXCESS_BRAKING     :
            case STATUS_EXCESS_BRAKING_2   :
            case STATUS_EXCESS_BRAKING_3   :
                return true;
        }
        return false;
    }

    /**
    *** Returns true if the status code represents a Cornering state
    *** @param code The status code
    *** @return True if the status code represents an Cornering state
    **/
    public static boolean IsCornering(int code)
    {
        switch (code) {
            case STATUS_EXCESS_CORNERING   :
            case STATUS_EXCESS_CORNERING_2 :
            case STATUS_EXCESS_CORNERING_3 :
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* GFMI status codes for STOP_STATUS events */
    public static final int GFMI_StopStatus[] = new int[] {
        StatusCodes.STATUS_GFMI_STOP_STATUS_1,
        StatusCodes.STATUS_GFMI_STOP_STATUS_2,
        StatusCodes.STATUS_GFMI_STOP_STATUS_3,
        StatusCodes.STATUS_GFMI_STOP_STATUS_4,
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_CODE[]      = new String[] { "code" };
    private static final String ARG_LIST[]      = new String[] { "list" };
    private static final String ARG_PROPS[]     = new String[] { "properties", "props" };

    /**
    *** Print "usage" information
    **/
    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + StatusCodes.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -code=<code>    Display StatusCode description");
        Print.logInfo("  -list           List active StatusCodes");
        Print.logInfo("  -properties     Ouput active StatusCodes as a property list");
        System.exit(1);
    }

    /**
    *** Main entry point
    **/
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main

        /* display code description */
        if (RTConfig.hasProperty(ARG_CODE)) {
            int code = RTConfig.getInt(ARG_CODE,0);
            Print.sysPrintln("Code Description: " + StatusCodes.GetDescription(code,null));
            System.exit(0);
        }

        /* list codes */
        if (RTConfig.getBoolean(ARG_LIST,false)) {
            Map<Integer,Code> map = _GetStatusCodeMap();
            for (Integer sci : map.keySet()) {
                Print.sysPrintln(map.get(sci).toString());
            }
            System.exit(0);
        }

        /* list codes */
        if (RTConfig.getBoolean(ARG_PROPS,false)) {
            Map<Integer,Code> map = _GetStatusCodeMap();
            Print.sysPrintln("");
            Print.sysPrintln("# -------------------------------------");
            Print.sysPrintln("# Active Status Codes");
            Print.sysPrintln("# -------------------------------------");
            for (Integer sci : map.keySet()) {
                Code c = map.get(sci);
                String rtKey = c.getDescriptionRTKey();
                Print.sysPrintln("");
                Print.sysPrintln("# - " + c);
                Print.sysPrintln("#"+rtKey+"="       + c.getDescription(null/*locale*/));
                Print.sysPrintln("#"+rtKey+".hipri=" + c.isHighPriority());
            }
            Print.sysPrintln("");
            System.exit(0);
        }

        /* usage */
        usage();

    }

}

