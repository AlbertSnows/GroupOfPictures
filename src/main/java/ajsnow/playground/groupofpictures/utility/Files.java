package ajsnow.playground.groupofpictures.utility;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class Files {
    // not needed
    @Contract(pure = true)
    public static @NotNull Predicate<String> fileExists(String path) {
        return name -> {
            var folder = new File(path);
            var files = new HashSet<>(List.of(Objects.requireNonNull(folder.list())));
            return files.contains(name);
        };
    }
}
