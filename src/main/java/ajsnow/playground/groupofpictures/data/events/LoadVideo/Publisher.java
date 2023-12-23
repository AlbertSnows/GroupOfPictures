package ajsnow.playground.groupofpictures.data.events.LoadVideo;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;

import java.util.function.Consumer;

public class Publisher {
    @Contract(pure = true)
    public static @NotNull Consumer<ApplicationEventPublisher> publishHelloWorldEvent(Object context) {
        return publisher -> {
            publisher.publishEvent(new LoadVideo(context));
        };
    }
}
