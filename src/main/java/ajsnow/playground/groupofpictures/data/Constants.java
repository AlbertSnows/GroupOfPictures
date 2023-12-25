package ajsnow.playground.groupofpictures.data;

import org.jetbrains.annotations.NotNull;

public class Constants {
    public static final String SOURCE_PATH = "build/resources/main/source/";
    public static final String STATIC_PATH = "src/main/resources/static/";
    public static @NotNull String getHTMLPath() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? STATIC_PATH : "app/static/";
    }
}
