#!/bin/sh
# iText path
ITEXTPATH=/usr/share/java/itext.jar

JPDFPATH=.

java -classpath $ITEXTPATH:$JPDFPATH JpdfSelect $*
