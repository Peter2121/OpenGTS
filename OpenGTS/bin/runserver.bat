@echo off
:: -----------------------------------------------------------------------------
:: Project: OpenGTS - Open GPS Tracking System
:: URL    : http://www.opengts.org
:: File   : runserver.bat
:: -----------------------------------------------------------------------------
:: Valid Options:
::    -s <server>      - server name (NOTE: NO ':' BETWEEN ARGS)
::    -port:<port>     - override default server "listen" port
::    -log.file:<file> - log output to specified file.
:: -----------------------------------------------------------------------------
:: This assumes that GTS_HOME has already been set
if NOT "%GTS_HOME%" == "" goto gtsHomeFound
    echo ERROR Missing GTS_HOME environment variable
    goto exit
:gtsHomeFound
call "%GTS_HOME%\bin\common.bat"

REM --- remove prefixing "-s"
if NOT "%1" == "-s" goto noDashS
    shift
:noDashS

REM --- server jar name
if NOT "%1" == "" goto hasJarName
    echo ERROR Missing Server Name
    goto exit
:hasJarName
set JARNAME=%1
set JARMAIN="%GTS_HOME%\build\lib\%JARNAME%.jar"
shift

REM --- interactive
set LOGFILE="%GTS_HOME%\logs\%JARNAME%.log"
set LOGENABLE=true
if NOT "%1" == "-i" goto noInteractive
    set LOGENABLE=false
    shift
:noInteractive

REM -- kill
set PIDFILE="%GTS_HOME\logs\%JARNAME%.pid"
if NOT "%1" == "-kill" goto noKill
    REM - taskkill /F /PID pidnumber
    echo ERROR '-kill' not currently supported
    shift
    goto exit
:noKill

REM --- memory (increase as necessary)
set MEMORY=350m
 
REM ---
set ARGS=-conf:"%GTS_CONF%" -log.file.enable:%LOGENABLE% -log.name:%JARNAME% -log.file:%LOGFILE% -start %1 %2 %3 %4 %5 %6
echo %CMD_JAVA% -Xmx%MEMORY% -jar %JARMAIN% %ARGS%
%CMD_JAVA% -Xmx%MEMORY% -jar %JARMAIN% %ARGS%

REM ---
:exit
