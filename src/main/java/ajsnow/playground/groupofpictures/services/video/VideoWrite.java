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
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ajsnow.playground.groupofpictures.data.Constants.STATIC_PATH;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapse;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapseToBoolean;

@Service
public class VideoWrite {
    public static void clipLastGOP(@NotNull JsonObject startFrameData,
                                   @NotNull FFmpeg videoData,
                                   UrlOutput urlOutput) {
        String startFrameRawTime = (String) startFrameData.get("pts_time");
        var offset = 70;
        var possibleStartTime = (1000 * Float.parseFloat(startFrameRawTime) - offset);
        var startFrameTime = Math.max((int) possibleStartTime, 0) + "ms";
        var extractGOP = videoData
                .addArguments("-ss", startFrameTime)
                .addOutput(urlOutput);
        extractGOP.execute();
    }

    public static void clipGOP(@NotNull JsonArray frameList,
                               Integer nextIFrame,
                               @NotNull JsonObject startFrameData,
                               @NotNull FFmpeg videoData,
                               UrlOutput urlOutput) {
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
        extractGOP.execute();
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
        var clipFile = new File(String.format(STATIC_PATH + videoNameWithoutExtension +"/" + escapedIndex));
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
        if(!clipFile.exists()) {
            return Result.failure(ResponseEntity.internalServerError().body("Could not find the clip. " +
                    "This is a really bizarre bug and I haven't been able to discern the cause. " +
                    "Sorry about that! "));
        }
        return Result.success(clipFile);
    }
}
