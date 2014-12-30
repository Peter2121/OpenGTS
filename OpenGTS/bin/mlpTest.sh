#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : rgTest.sh
# -----------------------------------------------------------------------------
# Description:
#   This command-line utility allows testing of the MobileLocationProvider for
#   a specific Account or PrivateLabel name.
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "!!! ERROR: GTS_HOME not defined !!!"
    exit 99
fi
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
. ${GTS_HOME}/bin/common.sh # - returns "$CPATH", "$GTS_CONF", ...
# -----------------------------------------------------------------------------

# --- options
QUIET="-quiet"
DEBUG=
CMD_ARGS=

# --- check arguments
while (( "$#" )); do
    case "$1" in 

        # ------------------------------------------------------

        # - quiet
        "-quiet" | "-q" ) 
            QUIET="-quiet"
            DEBUG=
            ;;

        # - verbose
        "-verbose" | "-v" | "-debug" | "-debugMode" ) 
            #echo "DebugMode"
            QUIET=
            DEBUG="-debugMode"
            GTS_DEBUG=1
            ;;

        # ------------------------------------------------------

        # - PrivateLabel
        "-pl" | "-privLabel" ) 
            #echo "PrivateLabel $2"
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -pl=$2"
                shift
            else
                echo "Missing 'pl' PrivateLabel argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - Cell-ID
        "-cid" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -cid=$2"
                shift
            else
                echo "Missing 'cid' argument"
                exit 99
            fi
            ;;

        # - MNC
        "-mnc" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -mnc='$2'"
                shift
            else
                echo "Missing 'mnc' argument"
                exit 99
            fi
            ;;

        # - MCC
        "-mcc" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -mcc='$2'"
                shift
            else
                echo "Missing 'mcc' argument"
                exit 99
            fi
            ;;

        # - LAC
        "-lac" ) 
            if [ $# -ge 2 ]; then
                CMD_ARGS="${CMD_ARGS} -lac='$2'"
                shift
            else
                echo "Missing 'lac' argument"
                exit 99
            fi
            ;;

        # ------------------------------------------------------

        # - include regular argument
        * )
            CMD_ARGS="${CMD_ARGS} '$1'"
            ;;

        # - skip remaining args
        "--" )
            shift
            break
            ;;

    esac
    shift
done

# ---
JMAIN="org.opengts.cellid.MobileLocationProviderAdapter"
JMAIN_ARGS="${DEBUG} '-conf=${GTS_CONF}' -log.file.enable=false"
COMMAND="${CMD_JAVA} -classpath ${CPATH} ${JMAIN} ${JMAIN_ARGS} ${CMD_ARGS}"
if [ $GTS_DEBUG -ne 0 ]; then
    echo "${COMMAND}"
fi
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND}
else
    eval "${COMMAND}"
fi

# ---
#${GTS_HOME}/bin/exeJava ${QUIET} org.opengts.geocoder.ReverseGeocodeProviderAdapter ${DEBUG} ${ARGS} $*

# --- exit normally
exit 0

# ---

