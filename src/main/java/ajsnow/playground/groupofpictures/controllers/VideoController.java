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
import org.springframework.util.ResourceUtils;
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
        var frameData = videoExists ? videoProbe.setShowFrames(true).execute() : null;
        return videoExists
                ? ResponseEntity.ok((JsonArray) frameData.getData().getValue("frames"))
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found!");
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
            //Concern: doesn't work in debugger mode?
            extractGOP.execute(); //.wait();
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
            extractGOP.execute(); //.wait();
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
