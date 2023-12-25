package ajsnow.playground.groupofpictures.utility;

import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static ajsnow.playground.groupofpictures.data.Constants.STATIC_PATH;

public class GOPFileHelpers {
    // not needed
    @Contract(pure = true)
    public static @NotNull Predicate<String> fileExists(String path) {
        return name -> {
            var folder = new File(path);
            var files = new HashSet<>(List.of(Objects.requireNonNull(folder.list())));
            return files.contains(name);
        };
    }

    public static @NotNull Result<String, String> handleCreatingDirectory(@NotNull String newDirectory) {
        try {
            java.nio.file.Files.createDirectories(Paths.get(newDirectory));
            return Result.success("Directory created!");
        } catch (Exception ex) {
            return Result.failure("Couldn't create a directory! Error: " + ex.getMessage());
        }
    }
}
