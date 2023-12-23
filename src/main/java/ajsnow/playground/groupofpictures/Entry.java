package ajsnow.playground.groupofpictures;

import ajsnow.playground.groupofpictures.data.events.CorePublisher;
import ajsnow.playground.groupofpictures.data.events.LoadVideo.LoadVideo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Entry {
    public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Entry.class, args);
		// Load the video when the application starts up
		context.getBean(CorePublisher.class)
				.publish(LoadVideo.announce(new Object()));
	}

}
