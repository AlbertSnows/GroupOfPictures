package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.data.VideoSource;
import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;
import static ajsnow.playground.groupofpictures.utility.rop.result.Introducers.tryTo;
import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.*;
import static ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper.pipe;

@Service
public class VideoRead {
    @Autowired
    public final VideoSource videoSource;

    public VideoRead(VideoSource videoSource) {
        this.videoSource = videoSource;
    }

    /** does not check if file exists
     * @param name filename
     * @return file data
     */
    public static @NotNull Result<JsonArray, String> getFrameDataForVideo(String name) {
        return pipe(SOURCE_PATH + name)
                .then(tryTo(FileInputStream::new, ex -> "Could not open file stream! Cause: " + ex.getMessage()))
                .then(onSuccess(fileStream -> FFprobe.atPath().setInput(fileStream)))
                .then(attempt(tryTo(
                        probe -> probe.setShowFrames(true).execute(),
                        ex -> "Error executing ffmpeg command. Error: " + ex.getCause())))
                .then(onSuccess(frameData -> frameData.getData().getValue("frames")))
                //todo: fix this weird casting thing
                .then(onSuccess(i -> (JsonArray) i))
                .resolve();
    }

    public static Piper<Result<byte[], String>> tryReadingVideo(@NotNull File clipFile) {
        return pipe(clipFile.toPath())
                .then(tryTo(Files::readAllBytes, ex -> "Problem reading bytes: " + ex.getMessage()));
    }
}
