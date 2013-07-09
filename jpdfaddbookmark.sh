#!/bin/sh
# iText path
ITEXTPATH=/usr/share/java/itext.jar

# JpdfAddBookmark.class and ErrMsgException.class path
JPDFPATH=.

java -classpath $ITEXTPATH:$JPDFPATH JpdfAddBookmark $*
