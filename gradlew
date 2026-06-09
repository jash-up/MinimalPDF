#!/usr/bin/env bash
export JAVA_HOME=/opt/android-studio/jbr
export PATH=$JAVA_HOME/bin:$PATH
./gradle-8.7/bin/gradle "$@" --no-daemon
