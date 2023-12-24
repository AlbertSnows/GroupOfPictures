package ajsnow.playground.groupofpictures.data.events.loadvideo;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Listener {
//    @Async
    @EventListener
    public void handleLoadVideoEvent(@NotNull LoadVideoPublisher event) {
        System.out.println("Hello, world!");
    }
}
