@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : rgTest.bat
:: -----------------------------------------------------------------------------
:: Usage:
::   > bin\mlpTest.bat -pl:<PrivateLabelName> -cid:<CID> -mnc:<MNC> -mcc:<MCC> -lac:<LAC> 
:: Example:
::   > bin\mlpTest.bat -pl:default -cid:565110 -mnc:8 -mcc:240 -lac:318
:: -----------------------------------------------------------------------------
:: This assumes that GTS_HOME has already been set
if NOT "%GTS_HOME%" == "" goto gtsHomeFound
    echo ERROR Missing GTS_HOME environment variable
    goto exit
:gtsHomeFound
call "%GTS_HOME%\bin\common.bat"

REM --- main entry point
set JMAIN=org.opengts.cellid.MobileLocationProviderAdapter

REM ---
set ARGS=-conf:"%GTS_CONF%" %2 %3 %4 %5 %6 %7
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%
:noEcho
%CMD_JAVA% -classpath %CPATH% %JMAIN% %ARGS%

REM ---
:exit
