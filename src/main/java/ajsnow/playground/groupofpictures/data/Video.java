package ajsnow.playground.groupofpictures.data;

import com.github.kokorin.jaffree.ffmpeg.BaseInput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Component
public class Video {
    public final FFprobe videoProbe;
    public final FFmpeg videoData;
    public Video() {
        var pathToVideo = Paths.get("src/main/resources/source/CoolVideo.mp4");
        videoProbe = FFprobe.atPath()
                .setInput(pathToVideo);
        videoData = FFmpeg.atPath()
                .addInput(UrlInput.fromPath(pathToVideo));
//        System.out.println("Video ingested...");
    }
}
