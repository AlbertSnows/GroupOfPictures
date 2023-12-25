# Troubleshooting
notes about walls so i don't have to face them 
again later. i have goldfish brain. 
## Failed to deploy '<unknown> Dockerfile: Dockerfile': Can't retrieve image ID from build stream
Ya gotta enable buildkit in intellij config instead of just running the docker file.

## jar load cannot because "x" file does not exist

for some reason it's doing "LoadVideo" in the jar instead of loadvideo

deleting .gradle directory and restarting intellij after
clearing all those cache boxes fixed it. really weird.
if I had time i'd submit a bug report since it seems
to be a casing issue.

## remote debugging -> handshake failed

you need to do *:<port> not just port in the java command 
in the sh file
see: https://stackoverflow.com/questions/30858312/handshake-failed-connection-prematurally-closed-error-when-debugging-solr-in-i

## Files are spooky v3
the actual reasons seems to be that I asked
for the file before I was creating it
it might be because i didn't put them in the build folder
they need to be put in the build directory, that's where
the application is run

## file not found on docker

i have no idea why but this happened because
ffmpeg wasn't installed in the docker image, I guess?
I don't think that should be necessary right?
oh well

## docker compose doesn't work?

seems to have its own docker image that it doesn't
recompile on change. run docker-compose 

## .gradlew: not found
carriage return issue
https://stackoverflow.com/questions/14219092/bash-script-bin-bashm-bad-interpreter-no-such-file-or-directory
