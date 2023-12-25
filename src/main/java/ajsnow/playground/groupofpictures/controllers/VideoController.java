package ajsnow.playground.groupofpictures.controllers;

import ajsnow.playground.groupofpictures.services.routing.RoutingCore;
import ajsnow.playground.groupofpictures.services.video.VideoRead;
import ajsnow.playground.groupofpictures.services.video.VideoWrite;
import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import ajsnow.playground.groupofpictures.utility.rop.result.TypeOf;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.text.StringEscapeUtils;

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;
import static ajsnow.playground.groupofpictures.utility.rop.functions.HigherOrderFunctions.upcast;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapse;
import static ajsnow.playground.groupofpictures.utility.rop.result.Resolvers.collapseToBoolean;
import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.*;
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
                .then(attempt(VideoRead::getFrameDataForVideo))
                .then(using(TypeOf.<Object>forSuccesses()))
                .then(using(TypeOf.<Object>forFailures()))
                .then(onSuccess(ResponseEntity::ok))
                .then(onFailure(err -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(err)))
                .then(collapse())
                .resolve();
    }


    // bonus: convert to rop
    @GetMapping("/{videoName}/group-of-pictures/{groupIndex}")
    public ResponseEntity<?> streamGroupVideo(@PathVariable("videoName") String name,
                                              @PathVariable("groupIndex") @NotNull String indexName) {
        var escapedName = StringEscapeUtils.escapeJava(name);
        var escapedIndex = StringEscapeUtils.escapeJava(indexName);
        var sourceVideoLocation = Path.of(SOURCE_PATH + escapedName);
        // bonus: write a function to generalize response entity types, so I don't have to upcast
        // vars are not needed, but help with debugging
        var clippingResult =  VideoWrite
                .tryClippingVideo(escapedName, escapedIndex, sourceVideoLocation);
        var responseResult = clippingResult
                .then(using(TypeOf.<ResponseEntity<?>>forFailures()))
                .then(attempt(VideoRead::tryReadingVideo))
                .then(onSuccess(byteList -> ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\""+escapedIndex+"\"")
                        .body(byteList)))
                .then(using(TypeOf.<ResponseEntity<?>>forSuccesses()));
        return responseResult.then(collapse());
    }

    // bonus: convert to rop
    @GetMapping("/{videoName}/group-of-pictures")
    public String getFramesAsVideos(
            @PathVariable("videoName") String name,
            @NotNull Model model) {
        var escapedName = StringEscapeUtils.escapeJava(name);
        var sourceVideoLocation = Path.of(SOURCE_PATH + escapedName);
        var output = VideoWrite.tryClippingVideos(escapedName, sourceVideoLocation, model);
        return output.then(collapse());
    }
}
