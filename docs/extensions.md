# Extensions

## Write out a short plan for what you would need to change / add to your program to support the following features

### dynamically cropping and filtering videos with a query string
e.g.

    http://localhost:4444/videos/CoolVideo.mp4/group-of-pictures/1.mp4?crop=240:120:240:120
    http://localhost:4444/videos/CoolVideo.mp4/group-of-pictures/1.mp4?filter=monotoneo
So the library I used is a wrapper around ffmpeg. 
For this sort of feature, the yellow brick road leads
us to what is essentially a web application representation
of ffmpeg, where we have an endpoint or set of endpoints
that accept an arbitrary number of parameters and commands.
I have no idea if there is a name for this concept, but
it's interesting to think about. To more directly 
answer your question, we can just append these on as 
query parameters to our get request, and map them to 
internal variables that correspond to behavior defined 
in our api documentation to use
to act upon our videos. 