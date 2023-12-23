package ajsnow.playground.groupofpictures.data.events;

import ajsnow.playground.groupofpictures.data.events.LoadVideo.LoadVideo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class CorePublisher {
    @Autowired
    private final ApplicationEventPublisher publisher;

    public CorePublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    public void publish(@NotNull Consumer<ApplicationEventPublisher> eventHolder) {
        eventHolder.accept(publisher);
    }
}
