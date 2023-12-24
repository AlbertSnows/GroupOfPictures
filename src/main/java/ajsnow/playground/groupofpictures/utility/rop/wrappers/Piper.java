package ajsnow.playground.groupofpictures.utility.rop.wrappers;

import ajsnow.playground.groupofpictures.utility.rop.functions.HigherOrderFunctions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Wraps a value in a Piper, allowing chaining of operations and segueing into other applicable types
 * (such as Result) cleanly.
 * <p>
 * This is most useful when not streaming over values, starting off with a value which is not a Result.
 *
 * @param <T> the type of wrapped value
 */
@SuppressWarnings("unused")
public class Piper<T> {

    private final T element;

    /**
     * @param element .
     */
    public Piper(T element) {
        this.element = element;
    }

    /**
     * Applies the function to the piped value, returning a new pipe containing that value.
     * @param function piping function
     * @param <R> result type
     * @return new pipe segment of R
     */
    public <R> Piper<R> then(@NotNull Function<T, R> function) {
        return new Piper<>(function.apply(element));
    }

    /**
     * Applies the consumer to the current value of the piped value, returning a pipe containing
     * that value.
     * @param consumer consuming function
     * @return pipe containing the value
     */
    public Piper<T> peek(Consumer<T> consumer) {
        return then(HigherOrderFunctions.peek(consumer));
    }

    /**
     * Returns the final result of the piped value, with all the piped functions applied.
     * @return T
     */
    public T resolve() {
        return element;
    }

    /**
     * Returns the final result of the piped value, with all the piped functions applied.
     * @param f piping function
     * @param <R> result type
     * @return R
     */
    public <R> R resolveWith(@NotNull Function<T, R> f) {
        return f.apply(element);
    }

    /**
     * Creates a new Piper wrapping the provided element.
     * @param element value
     * @param <T> value type
     * @return a piper around T
     */
    @Contract(value = "_ -> new", pure = true)
    public static <T> @NotNull Piper<T> pipe(T element) {
        return new Piper<>(element);
    }

}
