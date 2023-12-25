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

## Files as spooky
the spring boot resource object doesn't seem to work.
use uhh...file i think? i'll fix it lol