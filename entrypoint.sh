#!/bin/bash
#DEBUG=${DEBUG:-"false"}
#if [ "$DEBUG" = "false" ]; then
#    echo "No debug..."
#    exec java -cp "app:app/lib/*" ajsnow.playground.groupofpictures.Entry
#else
    echo "Debugging!"
    exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 -jar app.jar
#fi