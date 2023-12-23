package ajsnow.playground.groupofpictures.data.events.LoadVideo;

import ajsnow.playground.groupofpictures.data.events.LoadVideo.LoadVideo;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class Listener {
    @Async
    @EventListener
    public void handleLoadVideoEvent(@NotNull LoadVideo event) {
        System.out.println("Hello, world!");
    }
}
