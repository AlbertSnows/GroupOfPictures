package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.data.Video;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
public class VideoRead {
    @Autowired
    public final Video video;

    public VideoRead(Video video) {
        this.video = video;
    }
}
