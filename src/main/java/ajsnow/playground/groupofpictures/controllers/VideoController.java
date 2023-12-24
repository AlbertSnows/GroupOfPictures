package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.video.VideoRead;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.text.StringEscapeUtils;

@Controller
@RequestMapping("/videos")
public class VideoController {
    @Autowired
    private final VideoRead videoRead;

    public VideoController(VideoRead videoRead) {
        //todo bonus: is it possible to lombok this or something?
        this.videoRead = videoRead;
    }

    @GetMapping("/{videoName}")
    public ResponseEntity<?> getFrameData(@PathVariable("videoName") String name) {
        var escapedName = StringEscapeUtils.escapeJava(name);
        var folder = new File("src/main/resources/source/");
        var files = new HashSet<>(List.of(Objects.requireNonNull(folder.list())));
        var videoExists = files.contains(escapedName); // file not found 404
        var pathToVideo = Paths.get("src/main/resources/source/"  + escapedName);
        var videoProbe = FFprobe.atPath().setInput(pathToVideo);
        var frameData = videoProbe.setShowFrames(true).execute();
        var responseBody = videoExists
                ? (JsonArray) frameData.getData().getValue("frames")
                : new JsonArray(List.of("No Video found!"));
        return videoExists
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(responseBody);
    }


    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public ResponseEntity<ByteArrayResource> streamGroupVideo(@PathVariable("videoName") String name,
                                                              @PathVariable("groupIndex") @NotNull String indexName) {
        // fnf, fnp, index not found, index not parsable
        // give data
        var dataForFrames = videoRead.videoSource.videoProbe
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
                //todo: change this to make a directory in the form resources/source/<videoname>/1, 2, ...
                .addOutput(UrlOutput.toPath(Path.of("src/main/resources/source/e.mp4")));
        try {
            //todo: doesn't work in debugger mode?
            next.execute(); //.wait();
        } catch (Exception ex) {
            System.out.println("problem waiting...");
        }

        try {
            var clipFilePath = new ClassPathResource("source/name.mp4");
            var x = clipFilePath.exists();
            var clipFile = clipFilePath.getFile();
            var videoBytes = Files.readAllBytes(clipFile.toPath());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"e.mp4\"")
                    .body(new ByteArrayResource(videoBytes));
        } catch (Exception ex) {
            System.out.println("problem sending response...");
        }
        return ResponseEntity.internalServerError().body(new ByteArrayResource(new byte[]{}));
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public String getFramesAsVideos(
            @PathVariable("videoName") String name,
            @NotNull Model model
    ) {
        // fnf, fnp
        // indexes not possible { list ...}
        // return html doc
        var dataForFrames = videoRead.videoSource.videoProbe
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
        var pathToVideo = Paths.get("src/main/resources/source/CoolVideo.mp4");
        for(var keyframeIndex : iFrameIndexes) {
            var videoData = FFmpeg.atPath()
                    .addInput(UrlInput.fromPath(pathToVideo));

            var start = keyframeIndex;
            var nextIFrame = iFrameIndexes.higher(start);
            var end = nextIFrame;
            if(end != null) {
                JsonObject startFrameData = (JsonObject) frameList.get(start);
                JsonObject endFrameData = (JsonObject) frameList.get(end);
                String startFrameRawTime = (String) startFrameData.get("pts_time");
                var offset = 70;
                var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
                String endFrameRawTime = (String) endFrameData.get("pts_time");
                var endTime = (int) (1000 * Float.parseFloat(endFrameRawTime));
                var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
                var endFrameTime = endTime + "ms";
                var outputFile = UrlOutput.toPath(Path.of(String.format(
                        "src/main/resources/static/%d.mp4", keyframeIndex)));
                var next = videoData
                        .addArguments("-ss", startFrameTime)
                        .addArguments("-to", endFrameTime)
                        .addArguments("-c:v", "copy")
                        .addArguments("-c:a", "copy")
                        .addOutput(outputFile);
                //todo: doesn't work in debugger mode?
                next.execute();
            }
        }
        List<String> clipNames = new ArrayList<>(iFrameIndexes.stream().map(index -> index + ".mp4")
                .toList());
        clipNames.remove(iFrameIndexes.size() - 1);
        model.addAttribute("clipFileNames", clipNames);
        model.addAttribute("videoFileName", name);
        return "video_sequence";
    }
}
