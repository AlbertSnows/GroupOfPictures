package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.services.routing.RoutingCore;
import ajsnow.playground.groupofpictures.utility.GOPFileHelpers;
import ajsnow.playground.groupofpictures.utility.rop.pair.Pair;
import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import ajsnow.playground.groupofpictures.utility.rop.result.TypeOf;
import ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.apache.coyote.Response;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapse;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapseToBoolean;
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
    private static Consumer<Integer> writeClipByIFrame(
            String clipPathAsString,
            Path sourceVideoLocation,
            TreeSet<Integer> iFrameIndexes,
            JsonArray frameList) {
        return (Integer startIFrame) -> {
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

    public static @NotNull Result<File, String>
    tryClippingVideo(String escapedName, String escapedIndex) {
        var sourceVideoLocation = SOURCE_PATH + escapedName;
        var frameListResult = pipe(sourceVideoLocation)
                .then(RoutingCore::handleFileNotFound).resolve()
                .then(onSuccess(location -> FFprobe.atPath().setInput(location)))
                .then(onSuccess(probe -> probe.setShowFrames(true).execute()))
                .then(onSuccess(frameData -> frameData.getData().getValue("frames")))
                .then(onSuccess(innerFrameList -> (JsonArray) innerFrameList))
                .then(onFailure(__ -> "Failed to cast"))
                .resolve();
        var videoNameWithoutExtension = pipe(escapedName)
                .then(name -> name.split("\\.mp4")[0]);
        var clipPathAsStringResult = videoNameWithoutExtension
                .then(nameWithoutExtension -> STATIC_PATH + nameWithoutExtension)
                .then(Path::of)
                .then(GOPFileHelpers::handleCreatingDirectory).resolve()
                .then(onSuccess(Path::toString))
                .then(onSuccess(dir -> dir + "/" + escapedIndex))
                .resolve();
        Function<JsonArray, IntPredicate> isIFrame = frameList -> frameIndex -> {
            var frameData = (JsonObject) frameList.get(frameIndex);
            return frameData.get("pict_type").equals("I");
        };
        var frameIndexes = pipe(frameListResult)
                .then(onSuccess(innerFrameList -> IntStream.rangeClosed(0, innerFrameList.size()-1)));
        var frameListWithFrameIndexes = frameIndexes.resolve()
                .then(combineWith(frameListResult))
                .using(Pair::of);
        var iFrameIndexResults = frameListWithFrameIndexes
                .then(onSuccess(pair -> pair.right().filter(isIFrame.apply(pair.left()))))
                .then(onSuccess(IntStream::boxed))
                .then(onSuccess(boxedFrames -> boxedFrames.collect(Collectors.toCollection(TreeSet::new))));
        var start = pipe(escapedIndex)
                .then(index -> index.split("\\.mp4")[0])
                .then(Integer::parseInt).resolve();
        var clipFilePath = pipe(STATIC_PATH + videoNameWithoutExtension +"/" + escapedIndex)
                .then(String::format).resolve(); // to be returned
        var rolledSet = clipPathAsStringResult
                .then(onSuccess(newDirPath -> new HashMap<String, Object>(Map.of("clipPathAsString", newDirPath))))
                .then(combineWith(iFrameIndexResults))
                .using((indexes, map) -> { map.put("iFrameIndexes", indexes); return map; })
                .then(combineWith(frameListResult))
                .using((frameList, map) -> { map.put("frameList", frameList); return map; });
        return rolledSet
                .then(attempt(ifFalse(map -> {
                    TreeSet<Integer> indexes = (TreeSet<Integer>) map.get("iFrameIndexes");
                    return !indexes.contains(start);
                }, "Index not found!")))
                .then(onSuccessDo(m -> writeClipByIFrame(
                        (String) m.get("clipPathAsString"),
                        Path.of(sourceVideoLocation),
                        (TreeSet<Integer>) m.get("iFrameIndexes"),
                        (JsonArray) m.get("frameList"))
                        .accept(start)))
                .then(onSuccess(__ -> new File(clipFilePath)))
                .then(attempt(ifFalse(
                        File::exists,
                        __ -> "Couldn't find clip file after creation!")));
    }

    @Contract(pure = true)
    public static @NotNull Result<String, String>
    tryClippingVideos(String escapedName, Model model) {
        var sourceVideoLocation = Path.of(SOURCE_PATH + escapedName);
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
        var newDirectory = getHTMLPath() + videoNameWithoutExtension;
        var directoryResult = GOPFileHelpers.handleCreatingDirectory(Path.of(newDirectory));
        if(!directoryResult.resolve().then(collapseToBoolean())) {
            model.addAttribute("errorMessage", directoryResult
                    .then(onSuccess(__ -> "Created directory")).then(collapse()));
            return Result.failure("error");
        }
        var clipPathAsString = String.format(getHTMLPath() + videoNameWithoutExtension + "/%d.mp4", startIFrame);
        Consumer<Integer> clipFile = clipByIFrames(sourceVideoLocation, iFrameIndexes, frameList, clipPathAsString);
        iFrameIndexes.forEach(clipFile);
        List<String> clipNames = new ArrayList<>(iFrameIndexes.stream()
                .map(index -> index + ".mp4")
                .toList());
        return pipe(model)
                .then(m -> m.addAllAttributes(Map.of(
                        "clipFileNames", clipNames,
                        "videoFileNames", videoNameWithoutExtension)))
                .then(__ -> "video_sequence")
                .then(Result::success)
//                .then(using(TypeOf.<String>forFailures()))
                .then(onFailure(x -> "Shouldn't be here")) //todo: how to circumvent this?
                .resolve();
    }
}
