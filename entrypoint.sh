#!/bin/bash

#if [ "$DEBUG" = "false" ]; then
    exec java -cp "app:app/lib/*" ajsnow.playground.groupofpictures.Entry
#else
#    exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -cp "app:app/lib/*" ajsnow.playground.groupofpictures.Entry
#fi