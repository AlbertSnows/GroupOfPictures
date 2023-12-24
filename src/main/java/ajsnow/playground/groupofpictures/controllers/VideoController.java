package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.routing.RoutingCore;
import ajsnow.playground.groupofpictures.services.video.VideoRead;
import ajsnow.playground.groupofpictures.utility.rop.result.TypeOf;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapse;
import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.onFailure;
import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.onSuccess;
import static ajsnow.playground.groupofpictures.utility.rop.result.TypeOf.using;
import static ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper.pipe;

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
        return pipe(escapedName)
                .then(RoutingCore::handleFileNotFound)
                .then(onSuccess(VideoRead::getFrameDataForVideo))
                .then(using(TypeOf.<Object>forSuccesses()))
                .then(using(TypeOf.<Object>forFailures()))
                .then(onSuccess(ResponseEntity::ok))
                .then(onFailure(err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(err)))
                .then(collapse())
                .resolve();
    }


    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public ResponseEntity<?> streamGroupVideo(@PathVariable("videoName") String name,
                                              @PathVariable("groupIndex") @NotNull String indexName) {
        var escapedName = StringEscapeUtils.escapeJava(name);
        var videoDirectory = "src/main/resources/source/";
        var path = Path.of(videoDirectory + escapedName);
        var fileExists = Files.exists(path);
        var videoProbe = FFprobe.atPath().setInput(path);
        if(!fileExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found!");
        }
        var dataForFrames = videoProbe.setShowFrames(true).execute();
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
        var pathToVideo = Paths.get("src/main/resources/source/" + name);
        var videoData = FFmpeg.atPath().addInput(UrlInput.fromPath(pathToVideo));
        var escapedIndex = StringEscapeUtils.escapeJava(indexName);
        var videoNameWithoutExtension = escapedName.split("\\.mp4")[0];
        var clipDirectory = "src/main/resources/static/";
        var newDirectory = clipDirectory + videoNameWithoutExtension;
        try {
            Files.createDirectories(Paths.get(newDirectory));
        } catch (Exception ex) {
            System.out.println("Problem creating directories! Error: " + ex.getMessage());
            return ResponseEntity.internalServerError().body("Couldn't create a directory, sorry! :(");
        }
        var outputPath = Path.of(newDirectory + "/" + escapedIndex);
        var urlOutput = UrlOutput.toPath(outputPath);
        if(!iFrameIndexes.contains(start)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Index not found!");
        }
        JsonObject startFrameData = (JsonObject) frameList.get(start);
        if(nextIFrame == null) {
            String startFrameRawTime = (String) startFrameData.get("pts_time");
            var offset = 70;
            var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
            var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
            var extractGOP = videoData
                    .addArguments("-ss", startFrameTime)
                    .addOutput(urlOutput);
            extractGOP.execute();
        } else {
            JsonObject endFrameData = (JsonObject) frameList.get(nextIFrame);
            String startFrameRawTime = (String) startFrameData.get("pts_time");
            var offset = 70;
            var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
            String endFrameRawTime = (String) endFrameData.get("pts_time");
            var endTime = (int) (1000 * Float.parseFloat(endFrameRawTime));
            var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
            var endFrameTime = endTime + "ms";
            var extractGOP = videoData
                    .addArguments("-ss", startFrameTime)
                    .addArguments("-to", endFrameTime)
                    .addArguments("-c:v", "copy")
                    .addArguments("-c:a", "copy")
                    .addOutput(urlOutput);
            //Concern: doesn't work in debugger mode?
            extractGOP.execute();
        }
        //Confusion: why doesn't this work???
//        var clipFilePath = new ClassPathResource(
//                String.format("static/" + videoNameWithoutExtension +"/" + escapedIndex),
//                getClass().getClassLoader());

        var clipFile = new File(String.format("src/main/resources/static/" + videoNameWithoutExtension +"/" + escapedIndex));
        if(!clipFile.exists()) {
            return ResponseEntity.internalServerError().body("Could not find the clip. " +
                    "This is a really bizarre bug and I haven't been able to discern the cause. " +
                    "Sorry about that! ");
        }
        try {
            var videoBytes = Files.readAllBytes(clipFile.toPath());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\""+escapedIndex+"\"")
                    .body(new ByteArrayResource(videoBytes));
        } catch (Exception ex) {
            System.out.println("Problem sending response: " + ex.getMessage());
        }
        // give data
        return ResponseEntity.internalServerError().body("Internal server error, everything is on fire AHHHHH!");
    }

    @GetMapping("/{videoName}/group-of-pictures")
    public String getFramesAsVideos(
            @PathVariable("videoName") String name,
            @NotNull Model model) {
        var escapedName = StringEscapeUtils.escapeJava(name);
        var videoDirectory = "src/main/resources/source/";
        var path = Path.of(videoDirectory + escapedName);
        var fileExists = Files.exists(path);
        if(!fileExists) {
            return "error";
        }
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
        var clipDirectory = "src/main/resources/static/";
        var videoNameWithoutExtension = escapedName.split("\\.mp4")[0];
        var newDirectory = clipDirectory + videoNameWithoutExtension;
        try {
            var outcome = Files.createDirectories(Paths.get(newDirectory));
        } catch (Exception ex) {
            System.out.println("Problem creating directories! Error: " + ex.getMessage());
            model.addAttribute("errorMessage", "Couldn't create a directory, sorry! :(");
            return "error";
        }
        for(var keyframeIndex : iFrameIndexes) {
            var videoData = FFmpeg.atPath()
                    .addInput(UrlInput.fromPath(pathToVideo));
            var nextIFrame = iFrameIndexes.higher(keyframeIndex);
            if(nextIFrame != null) {
                JsonObject startFrameData = (JsonObject) frameList.get(keyframeIndex);
                JsonObject endFrameData = (JsonObject) frameList.get(nextIFrame);
                String startFrameRawTime = (String) startFrameData.get("pts_time");
                var offset = 70;
                var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
                String endFrameRawTime = (String) endFrameData.get("pts_time");
                var endTime = (int) (1000 * Float.parseFloat(endFrameRawTime));
                var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
                var endFrameTime = endTime + "ms";
                var outputFile = UrlOutput.toPath(Path.of(String.format(
                        "src/main/resources/static/" + videoNameWithoutExtension + "/%d.mp4", keyframeIndex)));
                var next = videoData
                        .addArguments("-ss", startFrameTime)
                        .addArguments("-to", endFrameTime)
                        .addArguments("-c:v", "copy")
                        .addArguments("-c:a", "copy")
                        .addOutput(outputFile);
                next.execute();
            }
        }
        List<String> clipNames = new ArrayList<>(iFrameIndexes.stream().map(index -> index + ".mp4")
                .toList());
        clipNames.remove(iFrameIndexes.size() - 1);
        model.addAttribute("clipFileNames", clipNames);
        model.addAttribute("videoFileName", videoNameWithoutExtension);
        return "video_sequence";
    }
}
