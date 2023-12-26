# GroupOfPictures
 GOP codec assessment.
Refer to the docs folder for more info.

NOTE: HowToRun.md is incorrect for now. If you run in a docker
container, it may fail due to the time it takes
to install ffmpeg for the first time and also because of 
carriage return antics. 

To fix ffmpeg: There isn't one that I know of. You have to
keep building docker until it works. 

As a reminder, the commands are

`docker compose up --build` (may not work, was inconsistent) or

`docker build -t app .` + 

`docker run -p 8080:8080 app`

To fix "not found" errors AKA carriage return antics: All
files need to be in "LF" format to work correctly. You can
run a dox2unix command to fix this, or set your git to use
LF globally, or something else. 

You can also use `./gradlew bootRun` to spin up the API and
run the project. Endpoint is `localhost:8080`.

## Additional work

### documentation 
skipping because I feel worn out by the other stuff

### tests
same problem, too much time spent on functionality and debugging to implement tests
