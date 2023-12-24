package ajsnow.playground.groupofpictures.data.events.loadvideo;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.function.Consumer;

public class LoadVideoPublisher extends ApplicationEvent {
    public LoadVideoPublisher(Object source) {
        super(source);
    }
    @Contract(pure = true)
    public static @NotNull Consumer<ApplicationEventPublisher> announce(Object context) {
        return publisher -> publisher.publishEvent(new LoadVideoPublisher(context));
    }
}
