package ajsnow.playground.groupofpictures.services.video;

import ajsnow.playground.groupofpictures.data.VideoSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoRead {
    @Autowired
    public final VideoSource videoSource;

    public VideoRead(VideoSource videoSource) {
        this.videoSource = videoSource;
    }
}
