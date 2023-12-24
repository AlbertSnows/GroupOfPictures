# Overview

## Instructions

Write an overview of your implementation (no more than one page)

### Write high level overview of the decisions you made in your code.

I decided to use java with spring boot as my framework.

Why? 

Java is a pervasive language. This has several benefits. 
1) Familiarity is transferable 
2) It's mature enough to have reasonable solutions to most problems
3) In the event there are issues, those issues are likely well documented

Spring boot is its own beast. It does some neat stuff, so I want to use it more.
For most API types of questions, it's a perfectly reasonable solution; if a bit overkill.

Thankfully, Java also has a library called jaffree for interop with ffmpeg
, which 
makes this project slightly less painful. 
You may also seem a bunch of utility `rop` files. ROP stands for
railway oriented programming. I'll write up a document talking about
that more if I find time. 

### Document concepts you learned that future programmers reading your code would likely benefit from knowing.
I learned a few different things. Firstly, this is my first time 
interfacing with video transfer programmatically. The
design of video files as GOPs is tasty from a programming
perspective. In order for video segments to be processed
and separated correctly, you need to work with them by
the keyframes. Keyframes semantics are the guts of 
video data. If you don't have access to the keyframes,
the data rendering for a given video segment is useless.
I also got to play around with ROP a bit, which I've
been looking forward to. I'll write up a document about
that if I have time. 


### Describe how you think this implementation would or wouldn't be able to respond to future feature requests (It's okay if the answer is it won't but please explore why you think that is true.)
It depends a lot on future feature requests. First, we
store all our stuff locally in the resources folder,
which is not sustainable. However, the endpoints can
accept different video and group id names. It also 
cannot accept query parameters, so if we want to do
more nuanced querying on our videos, that's not an option.
Also this points to an ambiguity in the instructions,
but currently I also store clips rather than deleting them.
This could be replaced with a caching feature in a 
more complex system, but that's way outside the scope
of this project. 


### Write out any expected performance bottlenecks you see with this approach, and at a high level, how you think those should be addressed.

Ok two bottlenecks. First, video size. As I mentioned 
before, our system would quickly buckle under a lot
of videos because we store our data locally. Also,
I tried to set the app up such that the video would
be loaded in at runtime, but ffmpeg doesn't like that.
It needed a fresh instance of the video for each operation
and I couldn't figure out if there was a way to skirt
that behavior. There probably is, and I would look to
that as a way to speed up some our processing. This
goes hand-in-hand with caching.