package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.video.VideoRead;
import com.github.kokorin.jaffree.ffprobe.data.ProbeData;
import com.grack.nanojson.JsonArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<JsonArray> recordReceipt(@PathVariable("videoName") String name) {
        // cases
        // file not found 404
        // file not parsable
        // file parsable, give data
        var x = videoRead.video.videoProbe
                .setShowFrames(true)
                .execute();
        return ResponseEntity.ok((JsonArray) x.getData().getValue("frames"));
    }


    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public HttpStatusCode listAllData(@PathVariable("videoName") String name,
                                      @PathVariable("groupIndex") String index) {
        // fnf, fnp, index not found, index not parsable
        // give data
        return HttpStatusCode.valueOf(501);
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public HttpStatusCode getPoints(@PathVariable("videoName") String name) {
        // fnf, fnp
        // indexes not possible { list ...}
        // return html doc
        return HttpStatusCode.valueOf(501);
    }
}
