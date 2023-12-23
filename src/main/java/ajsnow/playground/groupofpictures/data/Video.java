package ajsnow.playground.groupofpictures.data;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Component
public class Video {
    private FFprobeResult video;
    public Video() {
        var pathToVideo = Paths.get("src/main/resources/source/CoolVideo.mp4");
        video = FFprobe.atPath()
                .setShowStreams(true)
                .setInput(pathToVideo)
                .execute();
//        System.out.println("Video ingested...");
    }
}
