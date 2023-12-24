package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.data.VideoSource;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.grack.nanojson.JsonArray;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

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
    public static @NotNull JsonArray getFrameDataForVideo(String name) {
        var pathToVideo = Paths.get(SOURCE_PATH  + name);
        System.out.println(SOURCE_PATH + name);
//        System.out.println(pathToVideo.);

        var videoProbe = FFprobe.atPath().setInput(pathToVideo);
        var frameData = videoProbe.setShowFrames(true).execute();
        return (JsonArray) frameData.getData().getValue("frames");
    }
}
