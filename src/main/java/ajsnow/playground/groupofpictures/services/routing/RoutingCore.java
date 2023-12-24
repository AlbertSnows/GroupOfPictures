package ajsnow.playground.groupofpictures.services.routing;

import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static ajsnow.playground.groupofpictures.data.Constants.SOURCE_PATH;

public class RoutingCore {
    public static @NotNull Result<String, String> handleFileNotFound(String name) {
        var folder = new File(SOURCE_PATH);
        if(!folder.exists()) {
            return Result.failure("No source directory!");
        }
        var files = new HashSet<>(List.of(Objects.requireNonNull(folder.list())));
        var videoExists = files.contains(name);
        return videoExists ?
                Result.success(name) :
                Result.failure("File not found!");
    }
}
