package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.video.VideoRead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/videos")
public class VideoController {
    @Autowired
    private final VideoRead videoRead;

    public VideoController(VideoRead videoRead) {
        //todo bonus: is it possible to lombok this or something?
        this.videoRead = videoRead;
    }

//    @GetMapping("/{videoId}/group-of-pictures.json")
    @GetMapping("/test")
    public HttpStatusCode recordReceipt() {
        var x = videoRead.video;
        return HttpStatusCode.valueOf(501);
    }


    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public ResponseEntity<Map<String, String>> listAllData(String name, String index) {
        return new ResponseEntity<>(HttpStatusCode.valueOf(501));
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public ResponseEntity<Map<String, String>> getPoints(@PathVariable String providedReceiptID) {
        return new ResponseEntity<>(HttpStatusCode.valueOf(501));
    }
}
