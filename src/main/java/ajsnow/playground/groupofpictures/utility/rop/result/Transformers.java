package ajsnow.playground.groupofpictures.utility.rop.result;

import ajsnow.playground.groupofpictures.utility.rop.functions.ConsumableFunction;
import ajsnow.playground.groupofpictures.utility.rop.functions.HigherOrderFunctions;
import ajsnow.playground.groupofpictures.utility.rop.functions.ThrowingLambdas;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static ajsnow.playground.groupofpictures.utility.rop.functions.HigherOrderFunctions.peek;
import static ajsnow.playground.groupofpictures.utility.rop.result.Introducers.tryTo;

/**
 * A collection of functions which take a Result and return a Result.
 */
@SuppressWarnings("unused")
public interface Transformers {

    /**
     * Returns a function which takes a Result and, if it's a success, applies the mapping value to that
     * success, otherwise returning the original failure.
     * @param mappingFunction to pipe the success
     * @param <IS> input success
     * @param <OS> output success
     * @param <F> failure type
     * @return dF(R(IS, F) -> R(OS, F)) IS -> mappingFunction -> OS
     */
    static <IS, OS, F> @NotNull Function<Result<IS, F>, Result<OS, F>>
    onSuccess(@NotNull Function<IS, OS> mappingFunction) {
        return attempt(mappingFunction.andThen(Result::success));
    }

    /**
     * Returns a Consumer which takes a Result and, if it's a Success, passes it to the provided consumer.
     * @param consumer to use on success
     * @param <S> success
     * @param <F> failure type
     * @return dF(x -> x) where x is consumed by the
     * consumer to produce a side effect if it's a success
     */
    @Contract(pure = true)
    static <S, F> @NotNull ConsumableFunction<Result<S, F>>
    onSuccessDo(Consumer<S> consumer) {
        return r -> r.then(onSuccess(peek(consumer)));
    }

    /**
     * Returns a function which takes a Result with an Exception failure type and, if it's a success, applies
     * the mapping value to that success, returning a new success unless an exception is thrown, when it
     * returns a failure of that exception. If the input was a failure, it returns that failure.
     * @param throwingFunction to try to map the success but may throw
     * @param <IS> input success
     * @param <OS> output success
     * @param <X> exception type
     * @return dF(R(IS, Exception) -> R(OS, Exception))
     * <p>
     * if input success -> R(IS -> OS)
     * <p>
     * if input failure -> R(Exception)
     * <p>
     * if throwingFunction failure -> R(X)
     */
    static <IS, OS, X extends Exception> @NotNull Function<Result<IS, Exception>, Result<OS, Exception>>
    onSuccessTry(ThrowingLambdas.ThrowingFunction<IS, OS, X> throwingFunction) {
        return attempt(tryTo(throwingFunction));
    }


    /**
     * Returns a function which takes a Result and, if it's a success, applies the mapping value
     * to that success, returning a new success unless an exception is thrown, when it
     * returns a failure of the exception-mapper applied to the exception.
     * If the input was a failure, it returns that failure.
     * @param throwingFunction to map success but may throw
     * @param exceptionMapper to handle mapping the throw to a failure
     * @param <IS> input success
     * @param <OS> output success
     * @param <F> fail type
     * @param <X> exception type
     * @return dF(x -> y) where y uses the throwing function if
     * x is a success AND if the throwing function throws instead
     * uses the exception mapper to fail gracefully into a failure type
     */
    static <IS, OS, F, X extends Exception> @NotNull Function<Result<IS, F>, Result<OS, F>>
    onSuccessTry(ThrowingLambdas.ThrowingFunction<IS, OS, X> throwingFunction,
                 Function<Exception, F> exceptionMapper) {
        return attempt(tryTo(throwingFunction).andThen(onFailure(exceptionMapper)));
    }

    /**
     * Returns a function which takes a Result and, if it's a success, applies the provided function
     * to that success - generating a new Result - and returns that Result. Otherwise, returns the
     * original failure.
     * @param mappingFunction to use on success
     * @param <InputSuccess> input success
     * @param <OutputSuccess> output success
     * @param <WideFailure> parent failure type
     * @param <NarrowFailure> child failure type
     * @return dF(R(InputSuccess, WF) -> R(OutputSuccess, WF))
     */
    @Contract(pure = true)
    static <InputSuccess, OutputSuccess, WideFailure, NarrowFailure extends WideFailure> @NotNull
            Function<Result<InputSuccess, WideFailure>, Result<OutputSuccess, WideFailure>>
    attempt(Function<InputSuccess, Result<OutputSuccess, NarrowFailure>> mappingFunction) {
        Function<NarrowFailure, WideFailure> upcaster = HigherOrderFunctions::upcast;
        Function<Result<OutputSuccess, NarrowFailure>, Result<OutputSuccess, WideFailure>> upcastOnFailure
                = onFailure(upcaster);
        return r -> r.either(mappingFunction.andThen(upcastOnFailure), Result::failure);
    }

    /**
     * Returns a function which takes a Result and, if it's a failure, applies the provided function
     * to that failure. Otherwise, returns the original success.
     * @param mappingFunction to map the failure against
     * @param <S> success type
     * @param <InputFailure> input failure type
     * @param <OutputFailure> output failure type
     * @return dF(R(S, IF) -> R(S, OF)) where the output uses
     * the mapping function if x is a failure
     */
    static <S, InputFailure, OutputFailure> @NotNull
            Function<Result<S, InputFailure>, Result<S, OutputFailure>>
    onFailure(@NotNull Function<InputFailure, OutputFailure> mappingFunction) {
        return recover(mappingFunction.andThen(Result::failure));
    }

    /**
     * Returns a consumer which takes a Result and, if it's a failure, passes it to the provided consumer
     * @param consumer to use on failure
     * @param <S> success type
     * @param <F> failure type
     * @return dF(x -> y) where if x is a failure it passes it to the consumer
     */
    @Contract(pure = true)
    static <S, F> @NotNull ConsumableFunction<Result<S, F>>
    onFailureDo(Consumer<F> consumer) {
        return r -> r.then(onFailure(peek(consumer)));
    }

    /**
     * Takes a Result of a Stream as a success or a single failure, and returns a Stream of Results
     * containing all the successes, or the single failure.
     * <p>
     * The main use-case for this is to follow mapping over tryTo() on a function which was designed to
     * be flat-mapped over.
     * @param <S> success type
     * @param <F> fail type
     * @return dF(x -> y) where y is either a stream of success results
     * or a failure
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<Result<Stream<S>, F>, Stream<Result<S, F>>>
    unwrapSuccesses() {
        return r -> r.either(
            successes -> successes.map(Result::success),
            failure -> Stream.of(Result.failure(failure))
        );
    }

    /**
     * Returns a function which takes a Result and, if it's a failure, applies the provided function to
     * that failure - generating a new Result - and returns that Result. Otherwise, return the original
     * success.
     * @param recoveryFunction function to generate recovery result if input is a failure
     * @param <S> success type
     * @param <IF> input fail type
     * @param <OF> output fail type
     * @param <OS> output success type
     * @return dF(R(S, IF) -> R(S/OS, OF)) where
     * R = success -> R(S)
     * R = failure -> recoveryFunction(IF) -> R(OS, OF)
     */
    @Contract(pure = true)
    static <S, IF, OF, OS extends S> @NotNull Function<Result<S, IF>, Result<S, OF>>
    recover(Function<IF, Result<OS, OF>> recoveryFunction) {
        return r -> r.either(
                Result::success,
                recoveryFunction.andThen(onSuccess((Function<OS, S>) HigherOrderFunctions::upcast)));
    }

    /**
     * Returns a function which takes a Result, and converts failures to successes and vice versa.
     * @param <S> success type
     * @param <F> fail type
     * @return dF(x -> y) where if x is R(S, F) then y is R(F, S)
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<Result<S, F>, Result<F, S>> invert() {
        return r -> r.either(Result::failure, Result::success);
    }

    /**
     * Returns a function which takes a Result whose success type is itself a Result, and merges the failure cases,
     * so we have a flat Result.
     * <p>
     * Note that *most of the time* this shouldn't be required, and indicates using onSuccess() when attempt() would
     * be more appropriate.
     * <p>
     * There are some situations though where we do end up with constructs like this: one example
     * is when a function which returns a Result can throw exceptions (eg, when a Result-returning handler is passed
     * into a database context). Passing that call into a tryTo() will yield a success type of the inner Result, wrapped
     * in an outer Result for the tryTo().
     * <p>
     * If the failure types of the inner and outer failure types do not match, you'll need to either first convert the
     * failures of the outer Result or use the overload which maps the failures of the inner Result.
     * @param <S> success type
     * @param <F> fail type
     * @return dF(x -> y) where x of the form R(R(S, FInner) FOuter)
     * y is the result of merging x's failures into R(S, FMerged)
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<Result<Result<S, F>, F>, Result<S, F>>
    mergeFailures() {
        return attempt(i -> i);
    }

    /**
     * Returns a function which takes a Result whose success type is itself a Result, and merges the failure cases,
     * so we have a flat Result.
     * <p>
     * Note that *most of the time* this shouldn't be required, and indicates using onSuccess() when attempt() would
     * be more appropriate.
     * <p>
     * There are some situations though where we do end up with constructs like this: one example
     * is when a function which returns a Result can throw exceptions (eg, when a Result-returning handler is passed
     * into a database context). Passing that call into a tryTo() will yield a success type of the inner Result, wrapped
     * in an outer Result for the tryTo().
     * <p>
     * In these cases, it's more likely the inner failure type is domain-specific, so the default approach is to map the
     * outer failure to the inner failure and then merge.
     * @param failureMapper f(F2 -> F1) merges f2 into f1
     * @param <S> success type
     * @param <F1> left fail type
     * @param <F2> right fail type
     * @return dF(x -> y) where R is a result function yielding F2
     * y is a result where the failure is merged if necessary
     */
    @Contract(pure = true)
    static <S, F1, F2> @NotNull Function<Result<Result<S, F1>, F2>, Result<S, F1>>
    mergeFailures(Function<F2, F1> failureMapper) {
        return r -> r.then(onFailure(failureMapper)).then(mergeFailures());
    }
}
