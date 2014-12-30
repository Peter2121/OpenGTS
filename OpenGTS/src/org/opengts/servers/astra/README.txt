Introduction
============

This file describes how to configure and run the OpenGTS device communication
server (DCS) for Astra Telematics vehicle tracking devices. The file also
describes the configuration options of the tracking devices necessary to
communicate with the DCS.

This server supports the following devices:

- AT110
- AT210
- AT240

Configuration of the above devices is described in the relevant User Guides
available from the following link:

http://gps-telematics.co.uk/downloads.htm

Additional product documentation is also available for download from this link.


Configuring the Server
======================

Changing the Server "Listen" Ports
----------------------------------

The ports on which the Astra DCS listens for incoming data packets is specified
on the "ListenPorts" tag within <DCServer name="astra"> tag in the
"dcservers.xml" file:

<ListenPorts 
    tcpPort="31090" 
    udpPort="31090"
    />

The default is 31090. If required, the "listen" port can be changed to fit the
requirements of your runtime environment. The tracking devices will need to be
configured to transmit data to the same port as the server used to listen for
incoming data packets. See the section "Setting the Astra Device Properties"
below

The "listen" ports must be open through the firewall in order for the remote
device to send data to the Astra server.

If packet acknowledgement is required, any acknowledgements sent by the server back
to the remote device must be sent from the same IP address to which the remote
device sent it's data packet. If your server responds to more than one IP address,
then the Astra server listener must be bound to the same IP address/interface used
by the remote tracking devices. This is set in the top-level "dcservers.xml" file,
on the "DCServerConfig" tag, "bindAddress" attribute.

Setting the "Unique-ID" Prefix Characters
-----------------------------------------

The Unique-ID prefix characters can be set in the "UniqueIDPrefix" tag section:

<UniqueIDPrefix><![CDATA[
    astra_
    imei_
    *
    ]]></UniqueIDPrefix>

These prefix characters are used to 'prepend' to the IMEI number as reported by
the device to look-up the owning Account/Device record for this device. For
instance, if the Astra device IMEI is "123456789012345", then the system will
search for the owning Device using the following Unique-ID keys, in the order
specified:

astra_123456789012345
imei_123456789012345
123456789012345

Note that the '*' character by itself indicates that the system should look up
the Astra device IMEI without any prefixing characters.

To bind an Astra device to a specified Account/Device record, set the "Unique ID:"
field on the Device Admin page to the appropriate prefixed unique-id value. For
example:

Unique ID: astra_123456789012345

Setting the Astra Device Properties
-----------------------------------

Refer to the relevant product User Guide for complete descriptions of the
configuration commands.

The IP address is set with the following command

$IPAD,<ip_address>

For example, $IPAD,192.168.1.1

The TCP/UDP port is set with the command

$PORT,<port_number>

For example, $PORT,31090

The protocol should be selected as follows:

AT210       - Protocol K (6)
AT110/AT240 - Protocol M (8)

To set the protocol use the command $PROT,<protocol number>, for example to
select protocol M use

$PROT,8

The MODE must be set to TCP or UDP as required.

To select TCP use

$MODE,4

To select UDP use

$MODE,5

Once configured these settings will be retained by the device.

The Astra server will send acknowledgement to the tracking device on successful
decoding of received packets. The device time limit from sending a packet to
receipt of the acknowledgement is set be the command

$TCPT,<seconds>

If the device is to ignore acknowledgements then this can be set to 0, but this
is not recommended.


Running the Server
==================

To begin listening for incoming events the server must be started. This section
describes the process for manually starting the Astra server and how to stop it.

Manually Starting the Server
----------------------------

The command for manually starting the Astra server on UNIX/Linux is as follows:

cd $GTS_HOME
bin/runserver.pl -s astra

For Windows use

cd %GTS_HOME%
bin\runserver.bat astra

To start the Astra server with debug logging (useful when testing or debugging),
the option "-debug" may be added to the command line.

The server will start, and logging information will be sent to the file:

"$GTS_HOME/logs/astra.log" or "%GTS_HOME%\logs\astra.log" on Windows.

To change the listening port from that specified in the "ListenPorts" tag in the
"dcservers.xml" file the -port argument can be used from the command line as
follows:

bin/runserver.pl -s astra -port 8001

or on Windows

bin\runserver.bat astra -port:8001

Stopping the Server
-------------------

To stop the running Astra server, enter the following commands:

cd $GTS_HOME
bin/runserver.pl -s astra -kill

On Windows use Ctrl-C to terminate the runserver batch job.


Adding Devices to an Account
============================

When data is received from a remote Astra tracking device, the Astra server looks
up the IMEI number in the Device table to determine which Account/Device owns
this device. This section describes how to create a Device record and associate
an Astra tracking device with the Device record.

Creating a New Device Record
----------------------------

Using the web-interface, log in to the appropriate Account which should own the
Astra tracking device, then traverse to the "Vehicle Admin" page. Create a new
Device then "Edit" the newly created Device record. On the Edit page, there will
be a field described as follows:

Unique ID: [ ]

In this field enter the value "astra_<IMEI_Number>", replacing "<IMEI_Number>"
with the device IMEI number. For instance, if the IMEI number is
"123456789012345", then enter the value "astra_123456789012345" in this
"Unique ID:" field.

You may also use "imei_123456789012345" or just "123456789012345".

After making changes to the Device record, click the "Change" button.


Testing a New Configured Device
===============================

This section describes the process for monitoring newly configured Astra devices
that have been assigned to an Account/Device record.

Monitoring for Incoming Connections
-----------------------------------

The Account report "Last Known Device Location" can be used to display the last
known location of a given device, which can also be used to determine whether any
events have been received from a specific Astra device.

If there are no device reports then the log file itself can be consulted for
indications of incoming data packets from the Astra device. The information in
the log file can indicate whether an IMEI number may not have been properly
assigned.  If the device record hasn't been created or the IMEI has been entered
incorrectly you will see a message in the log file containing
"!!!UniqueID not found!: <IMEI>" where <IMEI> is a 15 digit number.


Device Data
===========

The following section refers to data stored in the EventData table.

Odometer
--------

The odometerKM field in the EventData table is populated with data from the device
for ignition on and ignition off records. For all other records it is reported
as zero.

The distanceKM field is populated with the distance travelled during a journey
since the last ignition on record. This will be the total distance for the
journey when the next ignition off record is inserted.

Additional Data
---------------

The flag addRawData can be set true in the TrackClientPacketHandler.java class to
enable the Astra server to insert additional data in to the rawData field. Please
refer to the relevant protocol description documentation. Setting addRawData to
false will leave the rawData field empty.

Protocol K
----------

For protocol K an example of the contents would be

R=000040;S=0101;P=12.2V;B=100%;D=01;A1=0.0V;M=0km/h;X=0,0;Y=0,0;Z=0,0;I=0s;Q=A4;G=00

where

R = Reason Code (24 bits)
S = Status Code (16 bits)
P = External power (Volts)
B = Battery level (Percentage)
D = Digital input/output status and state changes (8 bits)
A1 = External ADC1 reading (0-5 Volts)
M = Maximum journey speed (km/h)
X = Accelerometer X-axis max, min readings (m/s/s * 10)
Y = Accelerometer Y-axis max, min readings (m/s/s * 10)
Z = Accelerometer Z-axis max, min readings (m/s/s * 10)
I = Journey Idle Time (Seconds)
Q = Signal quality (MS nibble: GSM [0-15], LS nibble: number of GPS satellites in use)
G = GeoFence Event

During a journey the maximum journey speed is the maximum speed attained since
the last report. For the next ignition off record it is the maximum speed
attained during the entire journey.

During a journey the maximum and minimum accelerometer data is since the last
report. For the next ignition off record it is the maximum and minimum for the
entire journey.

Protocol M
----------

For protocol M an example of the contents would be

R=000080;S=0100;P=12.4V;B=100%;D=000100;A1=0.0V;A2=0.0V;M=0km/h;X=3,0;Y=0,2;Z=0,8;I=14s;Q=C8;G=00

where

R = Reason Code (24 bits)
S = Status Code (16 bits)
P = External power (Volts)
B = Battery level (Percentage)
D = Digital input/output status and state changes (24 bits)
A1 = External ADC1 reading (0-5 Volts)
A2 = External ADC2 reading (0-15 Volts)
M = Maximum journey speed (km/h)
X = Accelerometer X-axis max, min readings (m/s/s * 10)
Y = Accelerometer Y-axis max, min readings (m/s/s * 10)
Z = Accelerometer Z-axis max, min readings (m/s/s * 10)
I = Journey Idle Time (Seconds)
Q = Signal quality (MS nibble: GSM [0-15], LS nibble: number of GPS satellites in use)
G = GeoFence Event

During a journey the maximum journey speed is the maximum speed attained since
the last report. For the next ignition off record it is the maximum speed
attained during the entire journey.

During a journey the maximum and minimum accelerometer data is since the last
report. For the next ignition off record it is the maximum and minimum for the
entire journey.

