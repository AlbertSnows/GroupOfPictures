package ajsnow.playground.groupofpictures.utility.rop.result;

import ajsnow.playground.groupofpictures.utility.rop.pair.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Collects a Stream of Results into a Pair, with the left being a list of success values
 * and the right being a list of failure values.
 */
record ResultCollector<L, R, T>(
        Function<Pair<List<L>, List<R>>, T> finisher) implements Collector<Result<L, R>, Pair<List<L>, List<R>>, T> {

    @Contract(pure = true)
    @Override
    public @NotNull Supplier<Pair<List<L>, List<R>>> supplier() {
        return () -> new Pair<>(new ArrayList<>(), new ArrayList<>());
    }

    @Contract(pure = true)
    @Override
    public @NotNull BiConsumer<Pair<List<L>, List<R>>, Result<L, R>> accumulator() {
        return (accumulator, Result) -> Result.either(accumulator.left()::add, accumulator.right()::add);
    }

    @Contract(pure = true)
    @Override
    public @NotNull BinaryOperator<Pair<List<L>, List<R>>> combiner() {
        return (x, y) -> Pair.of(
                Stream.of(x, y).flatMap(l -> l.left().stream()).collect(toList()),
                Stream.of(x, y).flatMap(r -> r.right().stream()).collect(toList())
        );
    }


    @Contract(pure = true)
    @Override
    public @NotNull Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
