#!/bin/bash

licdir=$(dirname $(readlink -f $0));

$licdir/add-license.sh "$licdir/source-code-license.txt" "java"
$licdir/add-license.sh "$licdir/source-code-license.2.txt" "py"
