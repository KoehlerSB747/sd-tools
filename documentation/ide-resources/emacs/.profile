# ~/.profile: executed by the command interpreter for login shells.
# This file is not read by bash(1), if ~/.bash_profile or ~/.bash_login
# exists.
# see /usr/share/doc/bash/examples/startup-files for examples.
# the files are located in the bash-doc package.

# the default umask is set in /etc/profile
#umask 022

######
###
### Development environment setup
###
###
### Note that the core development applications are referenced by
### symlinks in /etc/opt for simplified version maintenance.
###

## General java development:
# Java
export JAVA_HOME=/etc/opt/java
export JAVA_OPTS="-Xmx640m -Dcom.sun.management.jmxremote"
export JDK_HOME="$JAVA_HOME"
export JAVAC="$JAVA_HOME/bin/javac"

# Ant
export ANT_HOME=/etc/opt/ant
export ANT_OPTS="-Xmx500m"

# Maven (optional)
export M2_HOME=/etc/opt/maven
export MAVEN_OPTS="-Xmx512m"

# Tomcat7 (optional)
export TOMCAT_HOME=/etc/opt/tomcat7
export CATALINA_HOME="$TOMCAT_HOME"

# Android development (optional)
export ANDROID_ROOT=$HOME/android
export ANDROID_SDK=$ANDROID_ROOT/android-sdk-linux
export ANDROID_TOOLS=$ANDROID_SDK/tools
export ANDROID_PLATFORM_TOOLS=$ANDROID_SDK/platform-tools

# Update path
export PATH=".:$HOME/bin:$ANT_HOME/bin:$JAVA_HOME/bin:$M2_HOME/bin:$ANDROID_TOOLS:$ANDROID_PLATFORM_TOOLS:$PATH"

## Emacs ide development
export EDITOR="emacs"
export VISUAL="emacs"
export CVSEDITOR="emacs"
# Set the text to accompany the @author tag for javadocs
username="`whoami`";
export AUTHOR="`getent passwd $username | awk -F[:,] '{print $5}'`";
# Set the name of (not the path to) your working sandbox directory containing source code
export SANDBOX_NAME="co"

## Hadoop
export HADOOP_HOME=/etc/opt/hadoop

## ATN verbosity
export DISABLE_ATN_LOAD_VERBOSITY=true;

## enable FieldServiceAPI SmokeTestApiQuery
export FIELD_SERVER_ADDRESS=localhost:10200

###
######

# if running bash
if [ -n "$BASH_VERSION" ]; then
    # include .bashrc if it exists
    if [ -f .bashrc ]; then
	. .bashrc
    fi
fi
