package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.services.routing.RoutingCore;
import ajsnow.playground.groupofpictures.utility.GOPFileHelpers;
import ajsnow.playground.groupofpictures.utility.rop.pair.Pair;
import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ajsnow.playground.groupofpictures.data.Constants.*;
import static ajsnow.playground.groupofpictures.utility.rop.result.Combiners.combineWith;
import static ajsnow.playground.groupofpictures.utility.rop.result.Introducers.*;
import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.*;
import static ajsnow.playground.groupofpictures.utility.rop.result.TypeOf.using;
import static ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper.pipe;

@Service
public class VideoWrite {
    /**
     * @return ...
     */
    // bonus: this should not be necessary, would need to figure out dev configs
    // bonus: add formal logging system
    public static @NotNull String getTimeAccessor() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "pts_time" : "pkt_pts_time";
    }

    /**
     * @param extractGOP ...
     * @return ...
     */
    public static Result<FFmpegResult, String> handleGOPExecution(FFmpeg extractGOP) {
        Function<Exception, String> log_failure = (Exception ex) -> {
            var message = "Creation error! Error: " + ex.getMessage();
            System.out.println(message);
            return message;
        };
        return pipe(extractGOP)
                .then(tryTo(FFmpeg::execute, log_failure))
                .resolve();
    }

    /**
     * @param startFrameData ...
     * @return ...
     */
    static Piper<Result<String, String>> calcStartFrameTime(JsonObject startFrameData) {
        return pipe(startFrameData)
                .then(startFrameJson -> startFrameJson.get(getTimeAccessor()))
                .then(castTo(String.class))
                .then(onSuccess(startRawTime -> 1000 * Float.parseFloat(startRawTime) - OFFSET))
                .then(onSuccess(Math::round))
                .then(onSuccess(possibleStartTime -> Math.max(possibleStartTime, 0) + "ms"))
                .then(onFailure(__ -> "Start frame casting failure!"));
    }

    /**
     * @param startFrameData ...
     * @param videoData ...
     * @param urlOutput ...
     * @return ...
     */
    public static Result<FFmpegResult, String>
    clipLastGOP(@NotNull JsonObject startFrameData, @NotNull FFmpeg videoData, UrlOutput urlOutput) {
        Function<String, FFmpeg> buildGOPAction = startFrameTime -> videoData
                .addArguments("-ss", startFrameTime)
                .addOutput(urlOutput);
        return calcStartFrameTime(startFrameData)
                .then(onSuccess(buildGOPAction))
                .then(attempt(VideoWrite::handleGOPExecution))
                .resolve();
    }



    /**
     * @param frameList ...
     * @param nextIFrame ...
     * @param startFrameData ...
     * @param videoData ...
     * @param urlOutput ...
     * @return ...
     */
    public static @NotNull Result<FFmpegResult, String>
    clipGOP(@NotNull JsonObject startFrameData,
            @NotNull FFmpeg videoData,
            UrlOutput urlOutput,
            @NotNull JsonArray frameList,
            Integer nextIFrame) {
        BiFunction<String, String, FFmpeg> buildGOPDataAction = (startTime, endTime) -> videoData
                .addArguments("-ss", startTime)
                .addArguments("-to", endTime)
                .addArguments("-c:v", "copy")
                .addArguments("-c:a", "copy")
                .addOutput(urlOutput);
        var endFrameTime = pipe(frameList)
                .then(frameJsonList -> frameJsonList.get(nextIFrame))
                .then(castTo(JsonObject.class))
                .then(onSuccess(endFrameData -> endFrameData.get(getTimeAccessor())))
                .then(attempt(castTo(String.class)))
                .then(onSuccess(endRawTime -> 1000 * Float.parseFloat(endRawTime)))
                .then(onSuccess(endTimeFloat -> Math.round(endTimeFloat) + "ms"))
                .then(onFailure(__ -> "End frame casting failure!"))
                .resolve();
        return calcStartFrameTime(startFrameData)
                .resolve()
                .then(combineWith(endFrameTime))
                .using(buildGOPDataAction)
                .then(attempt(VideoWrite::handleGOPExecution));
    }


    @NotNull
    private static Function<Integer, Consumer<String>> writeClipByIFrame(
            Path sourceVideoLocation,
            TreeSet<Integer> iFrameIndexes,
            JsonArray frameList) {
        return (Integer startIFrame) -> (String clipPathAsString) -> {
            var outputPathForClip = UrlOutput.toPath(Path.of(clipPathAsString));
            var videoData = FFmpeg.atPath().addInput(UrlInput.fromPath(sourceVideoLocation));
            var endIFrame = iFrameIndexes.higher(startIFrame);
            var startFrameData = (JsonObject) frameList.get(startIFrame);
            pipe(startIFrame)
                    .then(ifNull(iFrameIndexes::higher, null))
                    .then(onSuccessDo(__ -> VideoWrite.clipGOP(startFrameData, videoData, outputPathForClip, frameList, endIFrame)))
                    .then(onFailureDo(__ -> VideoWrite.clipLastGOP(startFrameData, videoData, outputPathForClip)))
                    .resolve();
        };
    }

    private static Result<HashMap<String, Object>, String>
    collectVideoData(@NotNull String escapedName, @NotNull String escapedIndex) {
        var sourceVideoLocation = SOURCE_PATH + escapedName;
        var frameListResult = pipe(sourceVideoLocation)
                .then(RoutingCore::handleFileNotFound).resolve()
                .then(onSuccess(location -> FFprobe.atPath().setInput(location)))
                .then(onSuccess(probe -> probe.setShowFrames(true).execute()))
                .then(onSuccess(frameData -> frameData.getData().getValue("frames")))
                .then(onSuccess(innerFrameList -> (JsonArray) innerFrameList))
                .then(onFailure(__ -> "Failed to cast"))
                .resolve();
        var videoNameWithoutExtension = escapedName.split("\\.mp4")[0];
        var clipPathAsStringResult = GOPFileHelpers
                .handleCreatingDirectory(Path.of(STATIC_PATH + videoNameWithoutExtension)).resolve()
                .then(onSuccess(Path::toString))
                .then(onSuccess(dir -> dir + "/" + escapedIndex));
        Function<JsonArray, IntPredicate> isIFrame = frameList -> frameIndex -> {
            var frameData = (JsonObject) frameList.get(frameIndex);
            return frameData.get("pict_type").equals("I");
        };
        var frameIndexes = frameListResult
                .then(onSuccess(frameList -> IntStream.rangeClosed(0, frameList.size()-1)));
        var frameListWithFrameIndexes = frameIndexes
                .then(combineWith(frameListResult))
                .using(Pair::of);
        var iFrameIndexResults = frameListWithFrameIndexes
                .then(onSuccess(pair -> pair.right().filter(isIFrame.apply(pair.left()))
                        .filter(isIFrame.apply(pair.left()))
                        .boxed()
                        .collect(Collectors.toCollection(TreeSet::new))));
        return clipPathAsStringResult
                .then(onSuccess(newDirPath -> new HashMap<String, Object>(Map.of("clipPathAsString", newDirPath))))
                .then(combineWith(iFrameIndexResults))
                .using((indexes, map) -> { map.put("iFrameIndexes", indexes); return map; })
                .then(combineWith(frameListResult))
                .using((frameList, map) -> { map.put("frameList", frameList); return map; });
    }

    public static @NotNull Result<File, String>
    tryClippingVideo(@NotNull String escapedName, @NotNull String escapedIndex) {
        var videoData = collectVideoData(escapedName, escapedIndex);
        var start = Integer.parseInt(escapedIndex.split("\\.mp4")[0]);

        return videoData
                .then(onSuccess(map -> {
                    var clipFilePath = pipe(STATIC_PATH + map.get("videoNameWithoutExtension") +"/" + escapedIndex)
                            .then(String::format).resolve();
                    map.put("clipFilePath", clipFilePath);
                    return map;
                }))
                .then(attempt(ifFalse(map -> {
                    TreeSet<Integer> indexes = (TreeSet<Integer>) map.get("iFrameIndexes");
                    return !indexes.contains(start);
                }, "Index not found!")))
                .then(onSuccessDo(m -> writeClipByIFrame(
                        Path.of((String) m.get("sourceVideoLocation")),
                        (TreeSet<Integer>) m.get("iFrameIndexes"),
                        (JsonArray) m.get("frameList"))
                        .apply(start)
                        .accept((String) m.get("clipPathAsString"))))
                .then(onSuccess(m -> new File((String) m.get("clipFilePath"))))
                .then(attempt(ifFalse(
                        File::exists,
                        __ -> "Couldn't find clip file after creation!")));
    }

    @Contract(pure = true)
    public static @NotNull Result<String, String>
    tryClippingVideos(@NotNull String escapedName, @NotNull Model model) {
        var sourceVideoLocation = SOURCE_PATH + escapedName;
        var frameListResult = pipe(sourceVideoLocation)
                .then(RoutingCore::handleFileNotFound).resolve()
                .then(onSuccess(location -> FFprobe.atPath().setInput(location)))
                .then(onSuccess(probe -> probe.setShowFrames(true).execute()))
                .then(onSuccess(FFprobeResult::getData))
                .then(onSuccess(data -> data.getValue("frames")))
                .then(onSuccess(list -> (JsonArray) list))
                .resolve();
        Function<JsonArray, IntPredicate> isIFrame = frameList -> frameIndex -> {
            var frameData = (JsonObject) frameList.get(frameIndex);
            return frameData.get("pict_type").equals("I");
        };
        var videoNameWithoutExtension = escapedName.split("\\.mp4")[0];
        var frameIndexes = frameListResult
                .then(onSuccess(frameList -> IntStream.rangeClosed(0, frameList.size()-1)));
        var frameListWithFrameIndexes = frameIndexes
                .then(combineWith(frameListResult))
                .using(Pair::of);
        var iFrameIndexResults = frameListWithFrameIndexes
                .then(onSuccess(pair -> pair.right().filter(isIFrame.apply(pair.left()))
                        .filter(isIFrame.apply(pair.left()))
                        .boxed()
                        .collect(Collectors.toCollection(TreeSet::new))));
        var clipNames = iFrameIndexResults
                .then(onSuccess(iFrameIndexes -> iFrameIndexes.stream()
                        .map(index -> index + ".mp4")
                        .toList()))
                .then(onSuccess(ArrayList::new));;
        var sourceExistsResult = pipe(sourceVideoLocation)
                .then(ifFalse(source -> Files.exists(Path.of(source)), __ -> {
                    model.addAttribute("errorMessage", "Requested video doesn't exist!");
                    return "error";
                }));
        //todo: abstract static path
        var createdDirectoryResult = GOPFileHelpers
                .handleCreatingDirectory(Path.of(getHTMLPath() + videoNameWithoutExtension))
                .then(onFailure(directoryError -> {
                    model.addAttribute("errorMessage", directoryError);
                    return "error";
                }));
        var rolledSet = clipPathAsStringResult
                .then(onSuccess(newDirPath -> new HashMap<String, Object>(Map.of("clipPathAsString", newDirPath))))
                .then(combineWith(iFrameIndexResults))
                .using((indexes, map) -> { map.put("iFrameIndexes", indexes); return map; })
                .then(combineWith(frameListResult))
                .using((frameList, map) -> { map.put("frameList", frameList); return map; });

        var clipFile = frameListWithFrameIndexes
                .then(onSuccess(pair -> writeClipByIFrame(Path.of(sourceVideoLocation), iFrameIndexResults, pair.left())));
        var clipFilePath = pipe(STATIC_PATH + videoNameWithoutExtension +"/" + escapedIndex)
                .then(String::format).resolve();

        return sourceExistsResult.resolve()
                .then(onSuccess(__ -> createdDirectoryResult.resolve()))
                .then(onSuccess(__ -> iFrameIndexes))
                .then(onSuccess(frames -> {
                    frames.forEach(startIFrame -> {
                        var clipPathAsString = String.format(getHTMLPath() + videoNameWithoutExtension + "/%d.mp4", startIFrame);
                        clipFile.apply(startIFrame).accept(clipPathAsString);
                        model.addAllAttributes(Map.of("clipFileNames", clipNames, "videoFileNames", videoNameWithoutExtension));
                    });
                    return frames;
                }))
                .then(onSuccess(__ -> "video_sequence"));
    }
}
