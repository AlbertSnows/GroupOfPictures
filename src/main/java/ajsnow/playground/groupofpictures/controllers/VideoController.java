package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.video.VideoRead;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.Filter;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public ResponseEntity<ByteArrayResource> streamGroupVideo(@PathVariable("videoName") String name,
                                                              @PathVariable("groupIndex") @NotNull String indexName) {
        // fnf, fnp, index not found, index not parsable
        // give data
        var dataForFrames = videoRead.video.videoProbe
                .setShowFrames(true)
                .execute();
        JsonArray frameList = (JsonArray) dataForFrames.getData().getValue("frames");
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
        var startFrameData = frameList.get(start);
        var endFrameData = frameList.get(end);
        var pathToVideo = Paths.get("src/main/resources/source/CoolVideo.mp4");
        var videoData = FFmpeg.atPath()
                .addInput(UrlInput.fromPath(pathToVideo));
        var next = videoData
                .addArguments("-ss", "2830ms")
                .addArguments("-to", "6867ms")
                .addArguments("-c:v", "copy")
                .addArguments("-c:a", "copy")
                .setOverwriteOutput(true)
//                .addArgument("-y")

//                .addArguments("-vf", )
//                .addArguments("setpts", "PTS-STARTPTS")
//                .addArguments("asetpts", "PTS-STARTPTS")
//                .addArguments("-vf", "select=\"between(n\\,87\\,206),setpts=PTS-STARTPTS\"")
//                .addArguments("-af", "aselect=\"between(n\\,87\\,206),asetpts=PTS-STARTPTS\"")
                .addOutput(UrlOutput.toPath(Path.of("src/main/resources/source/CoolVideoClip.mp4")));
        try {
            //todo: doesn't work in debugger mode?
            next.execute(); //.wait();
        } catch (Exception ex) {
            System.out.println("problem waiting...");
        }

//                .executeAsync()
//                .toCompletableFuture()
//                .complete()
                ;
//        while(!outcome.isDone()) {}

        try {
            var clipFilePath = new ClassPathResource("source/CoolVideoClip.mp4");
            var x = clipFilePath.exists();
            var clipFile = clipFilePath.getFile();
            var videoBytes = Files.readAllBytes(clipFile.toPath());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"CoolVideoClip.mp4\"")
                    .body(new ByteArrayResource(videoBytes));
        } catch (Exception ex) {
            System.out.println("problem sending response...");
        }
        return ResponseEntity.internalServerError().body(new ByteArrayResource(new byte[]{}));
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public HttpStatusCode getFramesAsVideos(@PathVariable("videoName") String name) {
        // fnf, fnp
        // indexes not possible { list ...}
        // return html doc
        return HttpStatusCode.valueOf(501);
    }
}
