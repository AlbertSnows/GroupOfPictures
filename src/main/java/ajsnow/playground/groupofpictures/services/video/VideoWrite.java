package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.services.routing.RoutingCore;
import ajsnow.playground.groupofpictures.utility.GOPFileHelpers;
import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;
import static ajsnow.playground.groupofpictures.data.Constants.STATIC_PATH;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapse;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapseToBoolean;

@Service
public class VideoWrite {
    // bonus: this should not be necessary, would need to figure out dev configs
    public static @NotNull String getTimeAccessor() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "pts_time" : "pkt_pts_time";
    }

    public static void clipLastGOP(@NotNull JsonObject startFrameData,
                                   @NotNull FFmpeg videoData,
                                   UrlOutput urlOutput) {
        String startFrameRawTime = (String) startFrameData.get(getTimeAccessor());
        var offset = 70;
        var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
        var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
        var extractGOP = videoData
                .addArguments("-ss", startFrameTime)
                .addOutput(urlOutput);
        try {
            extractGOP.execute();
        } catch(Exception ex) {
            System.out.println("Creation error! Error: " + ex.getMessage());
        }
    }

    public static void clipGOP(@NotNull JsonArray frameList,
                               Integer nextIFrame,
                               @NotNull JsonObject startFrameData,
                               @NotNull FFmpeg videoData,
                               UrlOutput urlOutput) {
        JsonObject endFrameData = (JsonObject) frameList.get(nextIFrame);
        String startFrameRawTime = (String) startFrameData.get(getTimeAccessor());
        var offset = 70;
        var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
        String endFrameRawTime = (String) endFrameData.get(getTimeAccessor());
        var endTime = (int) (1000 * Float.parseFloat(endFrameRawTime));
        var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
        var endFrameTime = endTime + "ms";
        var extractGOP = videoData
                .addArguments("-ss", startFrameTime)
                .addArguments("-to", endFrameTime)
                .addArguments("-c:v", "copy")
                .addArguments("-c:a", "copy")
                .addOutput(urlOutput);
        try {
            extractGOP.execute();
        } catch(Exception ex) {
            System.out.println("Creation error! Error: " + ex.getMessage());
        }
    }

    public static @NotNull Result<File, ResponseEntity<Object>>
    tryClippingVideo(String escapedName, String escapedIndex, Path sourceVideoLocation) {
        var fileExists = RoutingCore.handleFileNotFound(escapedName).then(collapseToBoolean());
        if(!fileExists) {
            return Result.failure(ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found!"));
        }
        var videoNameWithoutExtension = escapedName.split("\\.mp4")[0];
        var videoProbe = FFprobe.atPath().setInput(sourceVideoLocation);
        var dataForFrames = videoProbe.setShowFrames(true).execute();
        JsonArray frameList = (JsonArray) dataForFrames.getData().getValue("frames");
        var newDirectory = STATIC_PATH + videoNameWithoutExtension;
        var createdDirectories = GOPFileHelpers.handleCreatingDirectory(newDirectory);
        //non-ideal to collapse twice, but just focused on basic cleanup for now
        if(!createdDirectories.then(collapseToBoolean())) {
            return Result.failure(ResponseEntity.internalServerError().body(createdDirectories.then(collapse())));
        }
        IntPredicate isIFrame = frameIndex -> {
            var frameData = (JsonObject) frameList.get(frameIndex);
            return frameData.get("pict_type").equals("I");
        };
        var iFrameIndexes = IntStream
                .rangeClosed(0, frameList.size()-1)
                .filter(isIFrame)
                .boxed()
                .collect(Collectors.toCollection(TreeSet::new));
        var start = Integer.parseInt(escapedIndex.split("\\.mp4")[0]);
        var clipFilePath = String.format(STATIC_PATH + videoNameWithoutExtension +"/" + escapedIndex);
        if(!iFrameIndexes.contains(start)) {
            return Result.failure(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Index not found!"));
        }
        var outputPath = Path.of(newDirectory + "/" + escapedIndex);
        var urlOutput = UrlOutput.toPath(outputPath);
        var nextIFrame = iFrameIndexes.higher(start);
        var videoData = FFmpeg.atPath().addInput(UrlInput.fromPath(sourceVideoLocation));
        JsonObject startFrameData = (JsonObject) frameList.get(start);
        if (nextIFrame == null) {
            VideoWrite.clipLastGOP(startFrameData, videoData, urlOutput);
        } else {
            VideoWrite.clipGOP(frameList, nextIFrame, startFrameData, videoData, urlOutput);
        }
        var clipFile = new File(clipFilePath);
        if(!clipFile.exists()) {
            return Result.failure(ResponseEntity.internalServerError().body("Could not find the clip. " +
                    "This is a really bizarre bug and I haven't been able to discern the cause. " +
                    "Sorry about that! "));
        }
        return Result.success(clipFile);
    }

    @Contract(pure = true)
    public static @NotNull Result<String, String>
    tryClippingVideos(String escapedName, Path sourceVideoLocation, Model model) {
        var fileExists = Files.exists(sourceVideoLocation);
        if(!fileExists) {
            model.addAttribute("errorMessage", "Requested video doesn't exist!");
            return Result.failure("error");
        }
        var videoProbe = FFprobe.atPath().setInput(sourceVideoLocation);
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
        var videoNameWithoutExtension = escapedName.split("\\.mp4")[0];
        var newDirectory = STATIC_PATH + videoNameWithoutExtension;
        var directoryResult = GOPFileHelpers.handleCreatingDirectory(newDirectory);
        if(!directoryResult.then(collapseToBoolean())) {
            model.addAttribute("errorMessage", directoryResult.then(collapse()));
            return Result.failure("error");
        }
        Consumer<Integer> clipFile = (Integer keyframeIndex) -> {
            var videoData = FFmpeg.atPath()
                    .addInput(UrlInput.fromPath(sourceVideoLocation));
            var nextIFrame = iFrameIndexes.higher(keyframeIndex);
            JsonObject startFrameData = (JsonObject) frameList.get(keyframeIndex);
            var outputPath = UrlOutput.toPath(Path.of(String.format(
                    STATIC_PATH + videoNameWithoutExtension + "/%d.mp4", keyframeIndex)));
            if(nextIFrame != null) {
                VideoWrite.clipGOP(frameList, nextIFrame, startFrameData, videoData, outputPath);
            } else {
                VideoWrite.clipLastGOP(startFrameData, videoData, outputPath);
            }
        };
        iFrameIndexes.forEach(clipFile);
        List<String> clipNames = new ArrayList<>(iFrameIndexes.stream()
                .map(index -> index + ".mp4")
                .toList());
        model.addAttribute("clipFileNames", clipNames);
        model.addAttribute("videoFileName", videoNameWithoutExtension);
        return Result.success("video_sequence");
    }
}
