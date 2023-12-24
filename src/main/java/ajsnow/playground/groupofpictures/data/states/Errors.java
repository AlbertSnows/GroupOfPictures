package ajsnow.playground.groupofpictures.data.states;

import org.jetbrains.annotations.NotNull;

public class Errors {
    public static @NotNull String couldNotCreateDirectory(@NotNull Exception ex) {
        System.out.println("Problem creating directories! Error: " + ex.getMessage());
        return "Couldn't create a directory, sorry! :(";
    }
}
