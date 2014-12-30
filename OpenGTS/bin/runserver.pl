#!/usr/bin/perl
# -----------------------------------------------------------------------------
# Project    : OpenGTS - Open GPS Tracking System
# URL        : http://www.opengts.org
# File       : runserver.pl
# Description: Command-line GTS server startup utility
# -----------------------------------------------------------------------------
# Device Parser Server Startup (MySQL datastore)
#  Valid Options:
#    -s       <server> : server name
#    -bind    <addr>   : [optional] local bind address
#    -port    <port>   : [optional] listen port
#    -cmdport <port>   : [optional] command port
#    -i                : [optional] interactive
#    -debug            : [optional] debug logging
#    -memory <mem>     : [optional] allocated memory
#    -kill             : [optional] kill running server
#  Examples:
#     % runserver.pl -s <server> -port 31000 -i
#     % runserver.pl -s <server> -kill
# -----------------------------------------------------------------------------
# If present, this command will use the following environment variables:
#  GTS_HOME - The GTS installation directory (defaults to ("<commandDir>/..")
#  GTS_CONF - The runtime config file (defaults to "$GTS_HOME/default.conf")
# -----------------------------------------------------------------------------
$GTS_HOME = $ENV{"GTS_HOME"};
if ("$GTS_HOME" eq "") {
    print "!!! ERROR: GTS_HOME not defined !!!\n";
    use Cwd 'realpath'; use File::Basename;
    my $EXEC_BIN = dirname(realpath($0));
    require "$EXEC_BIN/common.pl";
    exit(99); # - exit anyway
} else {
    require "$GTS_HOME/bin/common.pl";
}
# -----------------------------------------------------------------------------

# --- options
use Getopt::Long;
%argctl = (
    # - DCS options
    "exists:s"      => \$opt_exists,
    "server:s"      => \$opt_server,
    "main:s"        => \$opt_mainClass,
    "main_"         => \$opt_AMBIGUOUS,
    "class:s"       => \$opt_mainClass,
    "class_"        => \$opt_AMBIGUOUS,
    "memory:s"      => \$opt_memory,
    "mem:s"         => \$opt_memory,
    "context:s"     => \$opt_context,
    "context_"      => \$opt_AMBIGUOUS,
    "dcs:s"         => \$opt_context,
    "dcs_"          => \$opt_AMBIGUOUS,
    "autorestart"   => \$opt_autoRestart,
    "restart"       => \$opt_autoRestart,
    # - Jar authorization string
    "jarKeys"       => \$opt_jarKeys,
    "jarKeys_"      => \$opt_AMBIGUOUS,
    "jarAuth"       => \$opt_jarAuth,
    "jarAuth_"      => \$opt_AMBIGUOUS,
    "version"       => \$opt_version,
    "version_"      => \$opt_AMBIGUOUS,
    # - port overrides
    "bind:s"        => \$opt_bind,
    "bindAddress:s" => \$opt_bind,
    "port:s"        => \$opt_port,
    "cmdport:s"     => \$opt_cmdport,
    "command:s"     => \$opt_cmdport,
    # - interactive
    "i"             => \$opt_interactive,
    # - debug options
    "help"          => \$opt_help,
    "debugMode"     => \$opt_debug,
    "debug"         => \$opt_debug,
    "verbose"       => \$opt_debug,
    # - logging options
    "log"           => \$opt_logLevel,
    "logName:s"     => \$opt_logName,
    # - terminate options
    "kill:s"        => \$opt_kill,      # - optional kill sig (default "term")
    "kill_"         => \$opt_AMBIGUOUS,
    "term"          => \$opt_term,
    "term_"         => \$opt_AMBIGUOUS,
    # - parse file options
    "parseFile:s"   => \$opt_parseFile,
    "insert:s"      => \$opt_parseInsert,
    "insert_"       => \$opt_AMBIGUOUS,
    # - Unique-ID lookup
    "lookup:s"      => \$opt_lookup,
);
$optok = &GetOptions(%argctl);
if (defined $opt_exists) { $opt_server = $opt_exists; }
if (!$optok || (defined $opt_help) || 
    (!(defined $opt_server) && !(defined $opt_lookup))) {
    print "Usage:\n";
    print "  Display this help:\n";
    print "    runserver.pl -h\n";
    print "  Start a server:\n";
    print "    runserver.pl -s <server> [-port <port>] [-i]\n";
    print "  Stop a server:\n";
    print "    runserver.pl -s <server> -kill\n";
    print "  Parse a file containing static data:\n";
    print "    runserver.pl -s <server> -parseFile=<file>\n";
    exit(99);
}

# -----------------------------------------------------------------------------

# --- lookup unique-id
if (defined $opt_lookup) {
    my $rtn = &sysCmd("$GTS_HOME/bin/exeJava org.opengts.db.DCServerFactory -lookup=$opt_lookup",$ECHO_CMD);
    exit($rtn);
}

# -----------------------------------------------------------------------------

# --- echo Java command-line prior to execution
$ECHO_CMD       = ("$GTS_DEBUG" eq "1")? $true : $false;

# --- server name 
$SERVER_NAME    = $opt_server;

# --- restart exit-code
$RESTART_CODE   = (defined $opt_autoRestart)? 247 : 0;

# --- log file directory
$GTS_LOGDIR     = $ENV{"GTS_LOGDIR"}; # - ie. "/var/log/gts"
if ("$GTS_LOGDIR" eq "") {
    $GTS_LOGDIR = "$GTS_HOME/logs";
}
$LOG_DIR        = "$GTS_LOGDIR";

# --- pid file directory
$GTS_PIDDIR     = $ENV{"GTS_PIDDIR"}; # - ie. "/var/run/gts"
if ("$GTS_PIDDIR" eq "") {
    $GTS_PIDDIR = "$GTS_HOME/logs";
}
$PID_DIR        = "$GTS_PIDDIR";

# --- log/pid file names
$LOG_NAME       = (defined $opt_logName)? $opt_logName : (defined $opt_context)? $opt_context : $SERVER_NAME;
$LOG_FILE       = "$LOG_DIR/${LOG_NAME}.log";
$LOG_FILE_OUT   = "$LOG_DIR/${LOG_NAME}.out";
$PID_FILE       = "$PID_DIR/${LOG_NAME}.pid";

# --- default kill signal
$KILL_SIG = "9";

# --- lib dir: build/lib
$LIB_DIR = "${GTS_HOME}/build/lib";

# --- memory, initial command
$Command = "$cmd_java";
if (defined $opt_memory) {
    $MEMORY   = $opt_memory;
    $Command .= " -Xmx$MEMORY";
}

# --- Java Main start-server command
$SERVER_JAR = "${LIB_DIR}/${SERVER_NAME}.jar";
if (!(-f "${SERVER_JAR}")) {
    # - not found, check for self contained version
    my $SelfContained = "${LIB_DIR}/${SERVER_NAME}_SC.jar";
    if (-f "${SelfContained}") {
        $SERVER_JAR = "${SelfContained}";
    }
}
if (-f "$SERVER_JAR") {
    if ((defined $opt_mainClass) && ("$opt_mainClass" ne "")) {
        # - explicitly specify the main startup class
        if    (("$opt_mainClass" eq "opt"    ) || 
               ("$opt_mainClass" eq "Main"   ) || 
               ("$opt_mainClass" eq "optMain")   ) {
            $MAIN_CLASS = "org.opengts.opt.servers.${SERVER_NAME}.Main";
        }
        elsif (("$opt_mainClass" eq "gtse"   ) ||
               ("$opt_mainClass" eq "gtsMain")   ) {
            $MAIN_CLASS = "org.opengts.extra.servers.${SERVER_NAME}.Main";
        }
        elsif (("$opt_mainClass" eq "opengts") ||
               ("$opt_mainClass" eq "osMain" )   ) {
            $MAIN_CLASS = "org.opengts.servers.${SERVER_NAME}.Main";
        } else {
            $MAIN_CLASS = $opt_mainClass;
        }
        $ALL_JARS = &getJarClasspath($PWD_,"./build/lib",$PATHSEP); # `($cmd_ls -1 ./build/lib/*.jar | $cmd_tr '\n' ${PATHSEP})`;
        $Command .= " -classpath $ALL_JARS $MAIN_CLASS";
    } else {
        # - The jar file knows how to start itself (via "Main-Class: ...")
        # - (this may still depend on external jars: gtsdb.jar)
        $Command .= " -jar $SERVER_JAR";
    }
} else {
    # - not found
    if (defined $opt_exists) {
        exit(1);
    } else {
        print "Server not found: $SERVER_JAR\n";
        exit(1);
    }
}

# --- test for server existance only
if (defined $opt_exists) {
    exit(0);
}

# --- Constants parameters
$Constants = $false;
if (defined $opt_jarKeys) {
    if (!$Constants) { $Command .= " Constants"; }
    $Command .= " -jarKeys";
    $Constants = $true;
}
if (defined $opt_jarAuth) {
    if (!$Constants) { $Command .= " Constants"; }
    $Command .= " -jarAuth";
    $Constants = $true;
}
if (defined $opt_version) {
    if (!$Constants) { $Command .= " Constants"; }
    $Command .= " -version";
    $Constants = $true;
}

# --- context name
if (defined $opt_context) {
    $Command .= " -rtcontext.name=$opt_context";
    $ECHO_CMD = $true;
}

# --- debug mode
if (defined $opt_debug) {
    $Command .= " -debugMode";
    $ECHO_CMD = $true;
}

# --- log level
if (defined $opt_logLevel) {
    $Command .= " -log.level=$opt_logLevel";
}

# --- config file (should be first argument)
$Command .= " -conf=$GTS_CONF -log.name=$LOG_NAME";

# --- jar auth
if ($Constants) {
    &sysCmd($Command, $ECHO_CMD);
    exit(0);
}

# --- stop process?
if ((defined $opt_kill) || (defined $opt_term)) {
    # - signal
    my $sig = (defined $opt_term)? "term" : ("$opt_kill" ne "")? $opt_kill : $KILL_SIG;
    # - pid file
    if (-f "$PID_FILE") {
        my $pid = `$cmd_cat $PID_FILE`; chomp $pid;
        if ($pid ne "") {
            print "Killing '$LOG_NAME' PID: $pid (via signal '-$sig')\n";
            my $rtn = &sysCmd("$cmd_kill -$sig $pid ; $cmd_rm $PID_FILE",$ECHO_CMD);
            if ($rtn != 0) {
                print "Error killing server: $rtn\n";
            }
            exit($rtn);
        } else {
            print "Invalid PID: $pid\n";
        }
    } else {
        print "PidFile not found: $PID_FILE\n";
    }
    exit(99);
}

# --- parse file?
if (defined $opt_parseFile) {
    print "Server jar: $SERVER_JAR\n";
    print "Parsing file: $opt_parseFile\n";
    my $parseCmd = $Command . " -parseFile=$opt_parseFile";
    if (defined  $opt_parseInsert) {
        $parseCmd .= " -insert=$opt_parseInsert";
    }
    &sysCmd($parseCmd, $ECHO_CMD);
    exit(99);
}

# --- assemble "start" command
my $DCSCommand = $Command . " -start";
if (defined $opt_bind) {
    $DCSCommand .= " -bindAddress=$opt_bind";
}
if (defined $opt_port) {
    #$DCSCommand .= " -${SERVER_NAME}.port=$opt_port";
    $DCSCommand .= " -port=$opt_port";
}
if (defined $opt_cmdport) {
    $DCSCommand .= " -${SERVER_NAME}.commandPort=$opt_cmdport";
    #$DCSCommand .= " -commandPort=$opt_cmdport";
}
$DCSCommand .= " " . join(' ', @ARGV);

# --- start interactive
if (defined $opt_interactive) {
    # - ignore $PID_FILE for interactive 
    print "Server '$LOG_NAME' jar: $SERVER_JAR\n";
    $DCSCommand .= " -log.file.enable=false";
    &sysCmd($DCSCommand, $ECHO_CMD);
    # - actually, we wait above until the user hits Control-C
    exit(99); # <-- never gets here
}

# --- already running?
if (-f "$PID_FILE") {
    my $pid = `$cmd_cat $PID_FILE`; chomp $pid;
    print "PID file already exists: $PID_FILE  [pid $pid]\n";
    if ($cmd_ps eq "") {
        print "The '${LOG_NAME}' server may already be running.\n";
        print "If server has stopped, delete the server pid file and rerun this command.\n";
        print "Aborting ...\n";
        exit(99);
    }
    my $rtn = &sysCmd("$cmd_ps -p $pid >/dev/null");
    if ($rtn == 0) {
        print "The '${LOG_NAME}' server is likely already running using pid $pid.\n";
        print "Make sure this server is stopped before attempting to restart.\n";
        print "Aborting ...\n";
        exit(99);
    } else {
        print "(Service on pid $pid seems to have stopped, continuing ...)\n";
    }
    &sysCmd("$cmd_rm -f $PID_FILE", $ECHO_CMD);
}

# --- create logging directory
if (!(-d "$LOG_DIR")) {
    my $rtn = &sysCmd("$cmd_mkdir -p $LOG_DIR", $ECHO_CMD);
    if (!(-d "$LOG_DIR")) {
        # - still does not exist
        print "Unable to create log directory: $LOG_DIR";
        print "Aborting ...\n";
        exit(99);
    }
}

# --- log messages to file
$DCSCommand .= " -log.file.enable=true -log.file=$LOG_FILE";

# --- background server (save the pid)
my $pid = &forkCmd($DCSCommand, $LOG_FILE_OUT, $RESTART_CODE, $ECHO_CMD);
&sysCmd("echo $pid > $PID_FILE", $ECHO_CMD);

# --- display "Started" message and exit
sleep(1);
if (($RESTART_CODE > 0) && ($RESTART_CODE <= 255)) {
    print "Started '$LOG_NAME': $SERVER_JAR [background pid $pid] - with auto-restart\n";
} else {
    print "Started '$LOG_NAME': $SERVER_JAR [background pid $pid]\n";
}
exit(0);

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

sub getJarClasspath(\$\$\$) {
    my ($DIR,$LIB,$SEP) = @_;
    my $CP = "";
    foreach ( `$cmd_ls $LIB/*.jar 2>/dev/null` ) {
        my $file = &deblank($_);
        if ($file =~ /^$DIR/) { $file = substr($file, length($DIR)); }
        if ("$CP" ne "") { $CP .= $SEP; }
        $CP .= $file; 
    }
    return $CP;
}

# ---
