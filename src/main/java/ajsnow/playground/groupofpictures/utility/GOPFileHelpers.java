package ajsnow.playground.groupofpictures.utility;

import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static ajsnow.playground.groupofpictures.utility.rop.result.Introducers.tryTo;
import static ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper.pipe;

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

    public static Result<Path, String> handleCreatingDirectory(@NotNull Path desiredDirectory) {
        Function<Exception, String> recordException =
                ex -> "Couldn't create a directory! Error: " + ex.getMessage();
        return pipe(desiredDirectory)
                .then(tryTo(Files::createDirectories, recordException))
                .resolve();
    }
}
