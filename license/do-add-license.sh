#!/bin/bash
#
# do-add-license
#
# Add the given license text to the given file if not already licensed.
#
# Usage:
#
# do-add-license license-file file-to-be-licensed
#
# arg1: (required) license-file -- path to license file to add
# arg2: (required) file-to-be-licensed -- path to file to be licensed
#

licfile="$1";
srcfile="$2";

if test -e "$licfile" && test -e "$srcfile"; then
  curlicense=`head "$srcfile" | grep 'Copyright'`;
  generated=`head -n1 "$srcfile" | grep 'Generated by'`;

  if test -z "$curlicense" && test -z "$generated"; then
    # add the license if it isn't present.
    echo "adding $licfile to $srcfile";
  
    cat "$licfile" "$srcfile" > "${srcfile}.licensed";
    mv -f "${srcfile}.licensed" "$srcfile";
  fi
else
  exit -1;
fi;
