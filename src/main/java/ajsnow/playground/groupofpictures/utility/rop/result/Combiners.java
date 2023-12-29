package ajsnow.playground.groupofpictures.utility.rop.result;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.util.function.BiFunction;
import java.util.function.Function;

import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.attempt;

/**
 * interface for combining different data structures
 */
public interface Combiners {

    @Contract(pure = true)
    static <LS, RS, C> @NotNull Function<LS, Function<RS, C>> curryCombiner(BiFunction<LS, RS, C> combiner) {
        return leftSuccess -> rightSuccess -> combiner.apply(leftSuccess, rightSuccess);
    }

    /**
     * Combines two Results into a single Result. If both arguments are a Success, then
     * it applies the given function to their values and returns a Success of it.
     * @param <LS> left success type
     * @param <RS> right success type
     * @param <F> failure type
     * @param leftResult left result to merge
     * @return dF(x -> mergableResult) to use using on to get (LS, RS) -> ...
     * If either or both arguments are Failures, then this returns the first failure
     * it encountered.
     */
    @org.jetbrains.annotations.NotNull
    @org.jetbrains.annotations.Contract(pure = true)
    static <LS, RS, F> Function<Result<RS, F>, MergeableResults<LS, RS, F>>
    combineWith(Result<LS, F> leftResult) {
        // ugh ugh ugh we need an abstract class because otherwise it can't infer generics properly
        // can I be sick now? ta
        return rightResult -> new MergeableResults<>() {
            /**
             * @param combiner combining mechanism
             * @param <C>      combiner
             * @return an either
             */
            @Override
            public <C> Result<C, F> using(BiFunction<LS, RS, C> combiner) {
                var curriedCombiner = curryCombiner(combiner);
                Function<LS, Function<RS, Result<C, F>>> wrapCombinerIsSuccess =
                        ls -> rs-> Result.success(curriedCombiner.apply(ls).apply(rs));
                Function<LS, Result<C, F>> combineWithRight =
                        ls -> rightResult.then(Transformers.attempt(wrapCombinerIsSuccess.apply(ls)));
                return leftResult.then(attempt(combineWithRight));
            }
        };
    }

    /**
     * @param <A> input 1
     * @param <B> input 2
     * @param <F> failure
     */
    @FunctionalInterface
    interface MergeableResults<A, B, F>  {
        /**
         * @param combiner combining function
         * @param <C> success combiner
         * @return result with the combined result as the success and fail as fail
         */
        <C> Result<C, F> using(BiFunction<A, B, C> combiner);
    }
}
