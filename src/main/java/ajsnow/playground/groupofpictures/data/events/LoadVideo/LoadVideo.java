package ajsnow.playground.groupofpictures.data.events.LoadVideo;

import org.springframework.context.ApplicationEvent;

public class LoadVideo extends ApplicationEvent{
    public LoadVideo(Object source) {
        super(source);
    }
}
