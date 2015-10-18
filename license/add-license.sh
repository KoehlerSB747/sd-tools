#!/bin/bash
#
# add-license
#
# Add license text to files in this project.
#
# Usage:
#
# add-license license-file ext1 ext2 ...
#
# arg1: (optional, default=source-code-license.txt) path to license file to add
# args2+: (optional, default=java) file extensions to which license is to be
#                                  added if missing
#

licdir=$(dirname $(readlink -f $0));
srcdir="$licdir/../src";
license="$1";
shift 1;
exts="$@";

test -z "$license" && license="$licdir/source-code-license.txt";
test -z "$exts" && exts="java";

echo "srcdir=$srcdir";
echo "licdir=$licdir";
echo "license=$license";
echo "exts=$exts";

for ext in $exts; do
  echo "ext=$ext";
  find "$srcdir" -name '*'.$ext -exec "$licdir/do-add-license.sh" "$license" \{\} \;
done;
