package ajsnow.playground.groupofpictures.utility.rop.pair;

import ajsnow.playground.groupofpictures.utility.rop.result.Result;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static ajsnow.playground.groupofpictures.utility.rop.result.Result.failure;
import static ajsnow.playground.groupofpictures.utility.rop.result.Result.success;
import static java.util.stream.Collectors.toList;

/**
 * Convenience functions on Pairs
 */
@SuppressWarnings("unused")
public interface Pairs {

    /**
     * Applies the given function to the left element of a Pair, returning a new Pair with the result of that
     * function as the left element and the original right element untouched
     * @param leftMapper mapping function for left value
     * @param <OL> old left value type
     * @param <NL> new left value type
     * @param <R> right type
     * @return function that takes a pair, and converts the left type to a new type
     */
    @Contract(pure = true)
    static <OL, NL, R> @NotNull Function<Pair<OL, R>, Pair<NL, R>>
    onLeft(Function<? super OL, NL> leftMapper) {
        return pair -> Pair.of(leftMapper.apply(pair.left()), pair.right());
    }

    /**
     * Applies the given function to the right element of a Pair, returning a new Pair with the result of that
     * function as the right element and the original left element untouched
     * @param rightMapper mapping function for right value
     * @param <OR> old right value type
     * @param <NR> new right value type
     * @param <L> right type
     * @return function that takes a pair, and converts the right type to a new type
     */
    @Contract(pure = true)
    static <L, OR, NR> @NotNull Function<Pair<L, OR>, Pair<L, NR>>
    onRight(Function<OR, NR> rightMapper) {
        return pair -> Pair.of(pair.left(), rightMapper.apply(pair.right()));
    }

    /**
     * Applies the given function to both elements of a Pair, assuming that both elements are of the
     * same type
     * @param f mutating function
     * @param <T> old type
     * @param <R> new type
     * @return function that asks for a pair in the form P(T, T) and returns P(R, R)
     */
    @Contract(pure = true)
    static <T, R> @NotNull Function<Pair<T, T>, Pair<R, R>>
    onBoth(Function<T, R> f) {
        return pair -> Pair.of(f.apply(pair.left()), f.apply(pair.right()));
    }

    /**
     * Applies the given function to both elements off a Pair, yielding a non-Pair value
     * @param f merging function
     * @param <L> left type
     * @param <R> right type
     * @param <T> output type
     * @return function that asks for a pair and returns T
     */
    @Contract(pure = true)
    static <L, R, T> @NotNull Function<Pair<L, R>, T>
    merge(BiFunction<L, R, T> f) {
        return pair -> pair.then(f);
    }

    /**
     * Merges a Pair of Lists of T into a single List of T, with the left items at the front of the list.
     * @param <T> type
     * @return list with left before right, merged
     */
    @Contract(pure = true)
    static <T> @NotNull Function<Pair<List<T>, List<T>>, List<T>> mergeLists() {
        return pair -> Stream.of(pair.left(), pair.right()).flatMap(List::stream).collect(toList());
    }

    /**
     * Collects a Stream of Pairs into a single Pair of lists, where a given index can be used to access the left
     * and right parts of the input pairs respectively.
     * @param <L> left type
     * @param <R> right type
     * @return a collector representing a single list of pairs that is indexable
     */
    @Contract(value = " -> new", pure = true)
    static <L, R> @NotNull Collector<Pair<L, R>, Pair<List<L>, List<R>>, Pair<List<L>, List<R>>>
    toParallelLists() {
        return using(Collections::unmodifiableList, Collections::unmodifiableList);
    }

    /**
     * Collects a Stream of Pairs into a single Pair of arrays, where a given index can be used to access the left
     * and right parts of the input pairs respectively.
     * @param leftArrayConstructor left constructing function
     * @param rightArrayConstructor right constructing function
     * @param <L> left type
     * @param <R> right type
     * @return collector representing a single pair of arrays, where an index can be used to access the
     * left and right parts
     */
    @Contract(value = "_, _ -> new", pure = true)
    static <L, R> @NotNull Collector<Pair<L, R>, Pair<List<L>, List<R>>, Pair<L[], R[]>>
    toArrays(IntFunction<L[]> leftArrayConstructor, IntFunction<R[]> rightArrayConstructor) {
        return using(
                left -> left.toArray(leftArrayConstructor),
                right -> right.toArray(rightArrayConstructor)
        );
    }

    /**
     * Reduces a stream of pairs to a single pair, using the provided identities and reducer functions
     * @param leftIdentity left input
     * @param leftReducer left reducing function
     * @param rightIdentity right input
     * @param rightReducer right reducing function
     * @param <L> left type
     * @param <R> right type
     * @return collector that represents a single pair, representing the reduced pairs
     * based on the identities and reducing mechanisms
     */
    @Contract(value = "_, _, _, _ -> new", pure = true)
    static <L, R> @NotNull PairReducingCollector<L, R>
    reducing(L leftIdentity, BinaryOperator<L> leftReducer,
             R rightIdentity, BinaryOperator<R> rightReducer) {
        return new PairReducingCollector<>(leftIdentity, rightIdentity, leftReducer, rightReducer);
    }


    /**
     * @param leftFinisher left collection mechanism
     * @param rightFinisher right collection mechanism
     * @param <L> left type
     * @param <R> right type
     * @param <FL> new left finisher
     * @param <FR> new right finisher
     * @return pair of lists
     */
    @Contract(value = "_, _ -> new", pure = true)
    static <L, R, FL, FR> @NotNull Collector<Pair<L, R>, Pair<List<L>, List<R>>, Pair<FL, FR>>
    using(Function<List<L>, FL> leftFinisher, Function<List<R>, FR> rightFinisher) {
        return new PairListCollector<>(leftFinisher, rightFinisher);
    }

    /**
     * If there are any elements on the right side of the Pair, return a failure of
     * the right side, otherwise return a success of the left.
     * @param sides pair of lists
     * @param <L> left type
     * @param <R> right type
     * @return result representing success if right side has pairs, otherwise returns
     * list of left side
     */
    static <L, R> Result<List<L>, List<R>>
    anyFailures(@NotNull Pair<List<L>, List<R>> sides) {
        return sides.right().isEmpty() ? success(sides.left()) : failure(sides.right());
    }

    /**
     * If there are any elements on the left side of the Pair, return a success of
     * the left side, otherwise return a failure of the left.
     * @param sides pair of lists
     * @param <L> left type
     * @param <R> right type
     * @return result where success is if left side has elements, failure otherwise
     */
    static <L, R> Result<List<L>, List<R>>
    anySuccesses(@NotNull Pair<List<L>, List<R>> sides) {
        return sides.left().isEmpty() ? failure(sides.right()) : success(sides.left());
    }
}
