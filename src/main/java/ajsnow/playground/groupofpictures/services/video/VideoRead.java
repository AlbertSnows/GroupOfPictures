package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.data.VideoSource;
import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;

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
//        var pathToVideo = Paths.get(SOURCE_PATH  + name);
        try (var fileInputStream = new FileInputStream(SOURCE_PATH + "CoolVideo.mp4")){
            var videoProbe = FFprobe.atPath().setInput(fileInputStream);
            var frameData = videoProbe.setShowFrames(true).execute();
            return Result.success((JsonArray) frameData.getData().getValue("frames"));
        } catch (Exception e) {
            return Result.failure("File not found probably. Error: " + e.getCause());
        }
    }

    public static @NotNull Result<ByteArrayResource, ResponseEntity<String>> tryReadingVideo(File clipFile) {
        try {
            var videoBytes = Files.readAllBytes(clipFile.toPath());
            return Result.success(new ByteArrayResource(videoBytes));
        } catch (Exception ex) {
            return Result.failure(ResponseEntity.internalServerError().body("Problem sending response: " + ex.getMessage()));
        }
    }
}
