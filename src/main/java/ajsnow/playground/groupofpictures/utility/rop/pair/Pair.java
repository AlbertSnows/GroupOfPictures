package ajsnow.playground.groupofpictures.utility.rop.pair;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A basic tuple type
 */
public record Pair<L, R>(L left, R right) {

    /**
     * @param left left value
     * @param right right value
     */
    public Pair {
    }

    /**
     * @param left .
     * @param right .
     * @param <L> left type
     * @param <R> right type
     * @return new pair from left and right
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static <L, R> @NotNull Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    /**
     * Gets the left element. Note that Pair also supports direct member access, but this is useful when you need
     * a method reference to extract one side of the pair.
     *
     * @return left
     */
    @Override
    public L left() {
        return left;
    }

    /**
     * Gets the right element. Note that Pair also supports direct member access, but this is useful when you need
     * a method reference to extract one side of the pair.
     *
     * @return right
     */
    @Override
    public R right() {
        return right;

    }

    /**
     * Applies the given function to this pair.
     *
     * @param function action
     * @param <T> result type
     * @return result of applying function to this pair
     */
    public <T> T then(@NotNull Function<Pair<L, R>, T> function) {
        return function.apply(this);
    }

    /**
     * Applies the given bifunction to this pair, using left for the first argument and right for the second
     *
     * @param function bifunction pair
     * @param <T> output type
     * @return output type
     */
    public <T> T then(@NotNull BiFunction<L, R, T> function) {
        return function.apply(this.left, this.right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(left, pair.left) &&
                Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "Pair{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
