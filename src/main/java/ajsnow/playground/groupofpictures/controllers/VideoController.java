package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.video.VideoRead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/videos")
public class VideoController {
    @Autowired
    private final VideoRead videoRead;

    public VideoController(VideoRead videoRead) {
        //todo bonus: is it possible to lombok this or something?
        this.videoRead = videoRead;
    }

    @GetMapping("/{videoName}")
    public HttpStatusCode recordReceipt(@PathVariable("videoName") String name) {
        // cases
        // file not found 404
        // file not parsable
        // file parsable, give data
        var x = videoRead.video;
        return HttpStatusCode.valueOf(501);
    }


    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public HttpStatusCode listAllData(@PathVariable("videoName") String name,
                                      @PathVariable("groupIndex") String index) {
        return HttpStatusCode.valueOf(501);
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public HttpStatusCode getPoints(@PathVariable("videoName") String name) {
        return HttpStatusCode.valueOf(501);
    }
}
