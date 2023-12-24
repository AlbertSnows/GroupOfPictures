# Troubleshooting

## Failed to deploy '<unknown> Dockerfile: Dockerfile': Can't retrieve image ID from build stream
Ya gotta enable buildkit in intellij config instead of just running the docker file.

## jar load cannot because "x" file does not exist

for some reason it's doing "LoadVideo" in the jar instead of loadvideo

deleting .gradle directory and 

## remote debugging -> handshake failed

you need to do *:<port> not just port in the java command 
in the sh file
see: https://stackoverflow.com/questions/30858312/handshake-failed-connection-prematurally-closed-error-when-debugging-solr-in-i