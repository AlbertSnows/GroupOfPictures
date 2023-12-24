package ajsnow.playground.groupofpictures.utility.rop.functions;

import ajsnow.playground.groupofpictures.utility.rop.pair.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.iterate;

/**
 * interfaces for working with higher level functions
 */
@SuppressWarnings("unused")
public interface HigherOrderFunctions {

    /**
     * Takes a BiFunction, and reverses the order of the arguments
     * @param f function to use
     * @param <A> left type
     * @param <B> right type
     * @param <R> result type
     * @return dF where f(a, b) now becomes f(b, a)
     */
    @Contract(pure = true)
    static <A, B, R> @NotNull BiFunction<B, A, R> flip(BiFunction<A, B, R> f) {
        return (a, b) -> f.apply(b, a);
    }

    /**
     * Takes a list of functions (which take and return the same type) and composes
     * them into a single function, applying the provided functions in order
     * @param functions an arbitrary number of T -> T functions
     * @param <T> function type
     * @return a single function representing the composition of each composite function
     */
    @SafeVarargs
    static <T> Function<T, T> compose(Function<T, T>... functions) {
        return compose(Stream.of(functions));
    }

    /**
     * Takes a Stream of functions (which take and return the same type) and composes
     * them into a single function, applying the provided functions in order
     * @param functions functions to compose
     * @param <T> input type
     * @return a single function that is the combination of each function
     */
    static <T> Function<T, T> compose(@NotNull Stream<Function<T, T>> functions) {
        return functions.reduce(identity(), Function::andThen);
    }

    /**
     * Takes a list of predicates and composes them into a single predicate, which
     * passes when all passed-in predicates pass
     * @param functions functions to compose
     * @param <T> input type
     * @return predicate function that is the composition of all passed in predicates
     */
    @SafeVarargs
    static <T> Predicate<T> compose(Predicate<T>... functions) {
        return Stream.of(functions).reduce(__ -> true, Predicate::and);
    }

    /**
     * Turns a Consumer into a Function which applies the consumer and returns the input
     * @param action consumer to act
     * @param <T> input type
     * @return dF (x -> x) where x is passed to action to execute
     */
    @Contract(pure = true)
    static <T> @NotNull Function<T, T> peek(Consumer<T> action) {
        return t -> {
            action.accept(t);
            return t;
        };
    }

    /**
     * @param items item stream
     * @param <T> stream type
     * @return a stream of KV pairs where k is the index and T is the value
     */
    static <T> @NotNull Stream<Pair<Integer, T>>
    withIndices(Stream<T> items) {
        return zip(iterate(0, x -> x + 1), items);
    }

    /**
     * @param a left stream
     * @param b right stream
     * @param <A> left type
     * @param <B> right type
     * @return zips the two streams into a pair
     */
    static <A, B> @NotNull Stream<Pair<A, B>>
    zip(Stream<A> a, Stream<B> b) {
        return zip(a, b, Pair::of);
    }

    /**
     * Zips two streams together using the zipper function, resulting in a single stream of
     * items from each stream combined using the provided function.
     * <p>
     * The resultant stream will have the length of the shorter of the two input streams.
     * <p>
     * Sourced from
     * <a href="https://stackoverflow.com/questions/17640754/zipping-streams-using-jdk8-with-lambda-java-util-stream-streams-zip">here</a>
     * @param a left stream
     * @param b right stream
     * @param zipper zipping function
     * @param <A> left type
     * @param <B> right type
     * @param <C> result type
     * @return a stream representing the merging of streams a and b
     */
    static <A , B, C> @NotNull Stream<C>
    zip(Stream<A> a, Stream<B> b, BiFunction<A, B, C> zipper) {
        Objects.requireNonNull(zipper);
        Spliterator<? extends A> aSpliterator = Objects.requireNonNull(a).spliterator();
        Spliterator<? extends B> bSpliterator = Objects.requireNonNull(b).spliterator();

        // Zipping looses DISTINCT and SORTED characteristics
        int characteristics = aSpliterator.characteristics() & bSpliterator.characteristics() &
            ~(Spliterator.DISTINCT | Spliterator.SORTED);

        long zipSize = ((characteristics & Spliterator.SIZED) != 0)
            ? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown())
            : -1;

        Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
        Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
        Iterator<C> cIterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return aIterator.hasNext() && bIterator.hasNext();
            }

            @Override
            public C next() {
                return zipper.apply(aIterator.next(), bIterator.next());
            }
        };

        Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
        return (a.isParallel() || b.isParallel())
            ? StreamSupport.stream(split, true)
            : StreamSupport.stream(split, false);
    }

    /**
     * Takes two lists, and returns a list of pairs forming the Cartesian product of those lists.
     * @param as left list
     * @param bs right list
     * @param <A> left type
     * @param <B> right type
     * @return list of pairs in the form L[P(A, B)...]
     */
    static <A, B> List<Pair<A, B>>
    pairs(@NotNull List<A> as, List<B> bs) {
        return as.stream().flatMap(a -> bs.stream().map(b -> Pair.of(a, b))).collect(toList());
    }

    /**
     * Takes a value, and returns that same value, upcast to a suitable type. Inference is our friend here.
     * @param fv value to upcast
     * @param <R> type to upcast to
     * @param <T> value type
     * @return fv upcasted from T to R
     */
    static <R, T extends R> R upcast(T fv) {
        return fv;
    }
}
