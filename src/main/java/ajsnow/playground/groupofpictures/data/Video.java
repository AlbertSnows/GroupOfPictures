package ajsnow.playground.groupofpictures.data;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Component
public class Video {
    public final FFprobe videoProbe;
    public Video() {
        var pathToVideo = Paths.get("src/main/resources/source/CoolVideo.mp4");
        videoProbe = FFprobe.atPath()
                .setInput(pathToVideo);
//        System.out.println("Video ingested...");
    }
}
