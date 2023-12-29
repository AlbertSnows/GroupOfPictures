package ajsnow.playground.groupofpictures.services.routing;

import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;
import static ajsnow.playground.groupofpictures.utility.rop.result.Introducers.ifFalse;
import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.onSuccess;
import static ajsnow.playground.groupofpictures.utility.rop.wrappers.Piper.pipe;

public class RoutingCore {
    public static @NotNull Result<String, String> handleFileNotFound(String name) {
        return pipe(SOURCE_PATH)
                .then(File::new)
                .then(ifFalse(File::exists, "No source directory!"))
                .then(onSuccess(folder -> new HashSet<>(List.of(Objects.requireNonNull(folder.list())))))
                .then(onSuccess(ifFalse(files -> files.contains(name), "File not found!")))
                .then(onSuccess(__ -> name))
                .resolve();
    }
}
