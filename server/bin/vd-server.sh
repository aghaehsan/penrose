#!/bin/sh

# load system-wide configuration
if [ -f "/etc/vd.conf" ] ; then
  . /etc/vd.conf
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
esac

if [ -z "$PENROSE_HOME" ] ; then

  PRG="$0"
  progname=`basename "$0"`
  saveddir=`pwd`

  # need this for relative symlinks
  dirname_prg=`dirname "$PRG"`
  cd "$dirname_prg"

  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '.*/.*' > /dev/null; then
	PRG="$link"
    else
	PRG=`dirname "$PRG"`"/$link"
    fi
  done

  PENROSE_HOME=`dirname "$PRG"`/..

  cd "$saveddir"

  # make it fully qualified
  PENROSE_HOME=`cd "$PENROSE_HOME" && pwd`
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$PENROSE_HOME" ] &&
    PENROSE_HOME=`cygpath --unix "$PENROSE_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java 2> /dev/null `
    if [ -z "$JAVACMD" ] ; then
        JAVACMD=java
    fi
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

LOCALLIBPATH="$JAVA_HOME/jre/lib/ext"
LOCALLIBPATH="$LOCALLIBPATH:$PENROSE_HOME/lib"
LOCALLIBPATH="$LOCALLIBPATH:$PENROSE_HOME/lib/ext"
LOCALLIBPATH="$LOCALLIBPATH:$PENROSE_HOME/server/lib"
LOCALLIBPATH="$LOCALLIBPATH:$PENROSE_HOME/server/lib/ext"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  PENROSE_HOME=`cygpath --windows "$PENROSE_HOME"`
  JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
  LOCALLIBPATH=`cygpath --path --windows "$LOCALLIBPATH"`
fi

cd "$PENROSE_HOME"
mkdir -p "$PENROSE_HOME/logs"

PID_FILE="$PENROSE_HOME/logs/vd-server.pid"
PID=0
RUNNING=0

if [ -f "$PID_FILE" ] ; then
  PID=`cat "$PID_FILE"`
  LINES=`ps -p $PID | wc -l`
  LINES=`expr $LINES`
  if [ "$LINES" = "2" ]; then
    RUNNING=1
  else
    rm -f "$PID_FILE"
  fi
fi

if [ "$1" = "start" ] ; then

  if [ "$RUNNING" = "1" ] ; then
    echo ${product.title} is running.
    exit 1
  else
    shift
    exec "$JAVACMD" $PENROSE_DEBUG_OPTS $PENROSE_OPTS \
    -Dcom.sun.management.jmxremote \
    -Djava.ext.dirs="$LOCALLIBPATH%" \
    -Djava.library.path="$LOCALLIBPATH" \
    -Dpenrose.home="$PENROSE_HOME" \
    org.safehaus.penrose.server.PenroseServer $PENROSE_ARGS "$@" 2>&1 &

    echo $! > "$PID_FILE"
  fi

elif [ "$1" = "stop" ] ; then

  if [ "$RUNNING" = "1" ] ; then
    kill $PID > /dev/null 2>&1
    rm -f "$PID_FILE"
  else
    echo ${product.title} is not running.
    exit 1
  fi

elif [ "$1" = "status" ] ; then

  if [ "$RUNNING" = "1" ] ; then
    echo ${product.title} is running.
  else
    echo ${product.title} is not running.
  fi

else

  if [ "$RUNNING" = "1" ] ; then
    echo ${product.title} is running.
    exit 1
  else
    exec "$JAVACMD" $PENROSE_DEBUG_OPTS $PENROSE_OPTS \
    -Dcom.sun.management.jmxremote \
    -Djava.ext.dirs="$LOCALLIBPATH" \
    -Djava.library.path="$LOCALLIBPATH" \
    -Dpenrose.home="$PENROSE_HOME" \
    org.safehaus.penrose.server.PenroseServer $PENROSE_ARGS "$@"
  fi

fi