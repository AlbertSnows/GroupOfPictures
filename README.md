# GroupOfPictures
 GOP codec assessment.
Refer to the docs folder for more info.

NOTE: HowToRun.md is incorrect for now. If you run in a docker
container, it may fail due to the time it takes
to install ffmpeg for the first time and also because of 
carriage return antics. 

To fix ffmpeg: There isn't one that I know of. You have to
keep trying until it works. 

To fix "not found" errors AKA carriage return antics: All
files need to be in "LF" format to work correctly. You can
run a dox2unix command to fix this, or set your git to use
LF globally, or something else. 

In which case,
you'll have to try again. In the meantime, you can
use `./gradlew bootRun` to spin up the API and
run the project. Endpoint is `localhost:8080`.

## TODO
documentation X -> skipping, I've 

X get working with docker X mostly done, running into carriage return issues