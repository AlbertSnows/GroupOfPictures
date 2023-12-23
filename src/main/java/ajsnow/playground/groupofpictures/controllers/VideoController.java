package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.video.VideoRead;
import com.github.kokorin.jaffree.ffprobe.data.ProbeData;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    public ResponseEntity<JsonArray> getFrameData(@PathVariable("videoName") String name) {
        // cases
        // file not found 404
        // file not parsable
        // file parsable, give data
        // remove cast
        var frameData = videoRead.video.videoProbe
                .setShowFrames(true)
                .execute();
        return ResponseEntity.ok((JsonArray) frameData.getData().getValue("frames"));
    }


    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public HttpStatusCode streamGroupVideo(@PathVariable("videoName") String name,
                                      @PathVariable("groupIndex") @NotNull String indexName) {
        var dataForFrames = videoRead.video.videoProbe
                .setShowFrames(true)
                .execute();
        JsonArray frameList = (JsonArray) dataForFrames.getData().getValue("frames");
//        Stream<JsonObject> frameStream = IntStream.range(0, frameList.size()).mapToObj(frameList::getObject);
        IntPredicate isIFrame = frameIndex -> {
            var frameData = (JsonObject) frameList.get(frameIndex);
            return frameData.get("pict_type").equals("I");
        };
        var iFrameIndexes = IntStream
                .rangeClosed(0, frameList.size()-1)
                .filter(isIFrame)
                .boxed()
                .collect(Collectors.toCollection(TreeSet::new));
        var start = Integer.parseInt(indexName.split("\\.mp4")[0]);
        var nextIFrame = iFrameIndexes.higher(start);
        var end = nextIFrame - 1;

        var x = new TreeSet<>(iFrameIndexes)
//                .toArray()
//                .collect(HashSet::new)
                ;
        // frame id -> data
        // ask for I frame 87
        // what frame do I stop just before?
        //


//                frameStream.filter(isIFrame).toList();
//        var startFrame = iFrameIndexes.toArray()[)];
//        var videoSnippet = videoRead.video.videoData
//                .
        // fnf, fnp, index not found, index not parsable
        // give data
        return HttpStatusCode.valueOf(501);
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public HttpStatusCode getFramesAsVideos(@PathVariable("videoName") String name) {
        // fnf, fnp
        // indexes not possible { list ...}
        // return html doc
        return HttpStatusCode.valueOf(501);
    }
}
