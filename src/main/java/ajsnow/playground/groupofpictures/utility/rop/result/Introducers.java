package ajsnow.playground.groupofpictures.utility.rop.result;

import ajsnow.playground.groupofpictures.utility.rop.functions.ThrowingLambdas;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.unwrapSuccesses;
import static java.util.function.Function.identity;

/**
 *   A collection of sample functions which take regular values and output a Result.
 */
@SuppressWarnings("unused")
public interface Introducers {


    /**
     * Returns a Function which creates a new Success wrapping the provided value
     * @param <S> success type
     * @param <F> fail type
     * @return success wrapper
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<S, Result<S, F>>
    success() {
        return Result::success;
    }

    /**
     * Returns a Function which creates a new Failure wrapping the provided value
     * @param <S> success type
     * @param <F> fail type
     * @return failure wrapper
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<F, Result<S, F>> failure() {
        return Result::failure;
    }

    /**
     * Returns a function which takes an Optional value, and returns a success of the
     * wrapped value if it was present, otherwise returns a failure using the provided Supplier
     * @param onEmpty failure supplier
     * @param <S> success type
     * @param <F> failure type
     * @return result type
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<Optional<S>, Result<S, F>>
    fromOptional(Supplier<F> onEmpty) {
        return maybe -> maybe.map(Result::<S, F>success).orElseGet(() -> Result.failure(onEmpty.get()));
    }

    /**
     * Returns a function which takes a value and checks a predicate on it: if the predicate passes, then
     * return a success of that value, otherwise apply the failure mapper to it
     * @param test a predicate testing function
     * @param <F> fail
     * @param <S> success
     * @param failureMapper func to map to
     * @return dF(x -> y) where x is S and y is a result type determined by testing x against test
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<S, Result<S, F>> ifFalse(Predicate<S> test, Function<S, F> failureMapper) {
        return val -> test.test(val) ? Result.success(val) : Result.failure(failureMapper.apply(val));
    }

    /**
     * Returns a function which takes a value and checks a predicate on it: if the predicate passes, then
     * return a success of that value, otherwise return a failure of the provided value
     * @param test testing function
     * @param failureValue fail value
     * @param <S> success type
     * @param <F> fail type
     * @return result
     */
    @Contract(pure = true)
    static <S, F> @NotNull Function<S, Result<S, F>>
    ifFalse(Predicate<S> test, F failureValue) {
        return val -> test.test(val) ? Result.success(val) : Result.failure(failureValue);
    }

    /**
     * Returns a function which takes a value, applies the given function to it, and returns a
     * success of the returned value, unless it's null, when we return the given failure value
     * @param mapper mapping function
     * @param failure failure type
     * @param <Input> input type
     * @param <Output> output type
     * @param <F> failure type
     * @return failure if null, success of output otherwise
     */
    @Contract(pure = true)
    static <Input, Output, F> @NotNull Function<Input, Result<Output, F>>
    ifNull(Function<Input, Output> mapper, F failure) {
        return input -> {
            final Output output = mapper.apply(input);
            return output == null ? Result.failure(failure) : Result.success(output);
        };
    }

    /**
     * Returns a function which takes a value, applies the given function to it, and returns a
     * success of the returned value, unless it's null, when we return a failure of the given
     * function to the input value.
     * @param mapper mapping function
     * @param failureMapper mapping (to failure) funcntion
     * @param <Input> input type
     * @param <Output> mapped type
     * @param <F> fail type
     * @return returns result with either success w/ output or failure with input
     */
    @Contract(pure = true)
    static <Input, Output, F> @NotNull Function<Input, Result<Output, F>>
    ifNull(Function<Input, Output> mapper, Function<Input, F> failureMapper) {
        return input -> {
            final Output output = mapper.apply(input);
            return output == null ? Result.failure(failureMapper.apply(input)) : Result.success(output);
        };
    }

    /**
     * Returns a function which takes a value, applies the given function to it, and returns a success of
     * the input unless the returned value matches the provided value. Otherwise, it returns a failure of the
     * failure value provided.
     * <p>
     * Note that the success path returns a success of the original value, not the result of applying this
     * function. This can be used to build more complex predicates, or to check the return value of a
     * consumer-with-return-code.
     * @param checker maps input type to value type
     * @param value value to check against
     * @param failure failure value
     * @param <S> success type
     * @param <F> failure type
     * @param <V> value type
     * @return dF(x->y) where x is S and returns a result of type R(S, F) based on comparing S to V
     */
    @Contract(pure = true)
    static <S, F, V> @NotNull Function<S, Result<S, F>>
    ifYields(Function<S, V> checker, V value, F failure) {
        return input -> checker.apply(input) == value ? Result.failure(failure) : Result.success(input);
    }

    /**
     * Returns a function which takes a value, applies the given function to it, and returns a success of
     * the input unless the returned value matches the provided value. Otherwise, it returns a failure of the
     * input value applied to the failure mapping function.
     * <p>
     * Note that the success path returns a success of the original value, not the result of applying this
     * function. This can be used to build more complex predicates, or to check the return value of a
     * consumer-with-return-code.
     * @param checker maps S to V
     * @param value type to compare against
     * @param failureMapper maps success to failure outcome
     * @param <S> success type
     * @param <F> fail type
     * @param <V> value comparison type
     * @return dF(x -> y) where
     */
    @Contract(pure = true)
    static <S, F, V> @NotNull Function<S, Result<S, F>>
    ifYields(Function<S, V> checker, V value, Function<S, F> failureMapper) {
        return input -> checker.apply(input) == value ?
                Result.failure(failureMapper.apply(input)) :
                Result.success(input);
    }

    /**
     * Returns a function which takes a value, applies the provided function to it, and returns
     * a success of the output of that function. In the case where the function throws an exception,
     * that exception is passed to the provided exception-mapper, and the output of that call is the
     * failure value.
     * <p>
     * Whilst we take a ThrowingFunction which throws a specific checked exception type X, our
     * eventual Result is of the more general type Exception. That's because it's also possible for the
     * function to throw other types of RuntimeException, and we have two choices: don't catch (or rethrow)
     * RuntimeException, or have a more general failure type. Rethrowing exceptions goes against the whole
     * point of constraining the error path, so we opt for the latter.
     * <p>
     * If the provided function throws an Error, we don't catch that. Errors in general are not
     * intended to be caught.
     * <p>
     * Note that idiomatic handling of Exceptions as failure type does allow specialised catch blocks
     * on specific exception types.
     * @param throwingFunction function that may fail
     * @param exceptionMapper maps the exception to the failure type
     * @param <IS> input type
     * @param <OS> output type
     * @param <X> exception type
     * @param <F> failure type
     * @return df(IS -> R(OS, F)) where x is the input type passed to throwingFunction
     * and the output wrapped in a try-catch that returns the exception mapped to a failure
     * via the exceptionMapper
     */
    @Contract(pure = true)
    static <IS, OS, X extends Exception, F> @NotNull Function<IS, Result<OS, F>>
    tryTo(ThrowingLambdas.ThrowingFunction<IS, OS, X> throwingFunction,
          Function<Exception, F> exceptionMapper) {
        return input -> {
            try {
                return Result.success(throwingFunction.apply(input));
            } catch (Exception ex) {
                return Result.failure(exceptionMapper.apply(ex));
            }
        };
    }

    /**
     * Returns a function which takes a value, applies the provided function to it, and returns
     * a success of the output of that function, or a failure of the exception thrown by that function
     * if it threw an exception.
     * <p>
     * Whilst we take a ThrowingFunction which throws a specific checked exception type X, our
     * eventual Result is of the more general type Exception. That's because it's also possible for the
     * function to throw other types of RuntimeException, and we have two choices: don't catch (or rethrow)
     * RuntimeException, or have a more general failure type. Rethrowing exceptions goes against the whole
     * point of constraining the error path, so we opt for the latter.
     * <p>
     * If the provided function throws an Error, we don't catch that. Errors in general are not
     * intended to be caught.
     * <p>
     * Note that idiomatic handling of Exceptions as failure type does allow specialised catch blocks
     * on specific exception types.
     * @param throwingFunction function that throws type X
     * @param <IS> input type
     * @param <OS> output type
     * @param <X> exception type
     * @return dF(IS -> R(OS, Exception))
     * input is passed to the throwing function,
     * output is a result with either the OS or F(Exception)
     */
    static <IS, OS, X extends Exception> @NotNull Function<IS, Result<OS, Exception>>
    tryTo(ThrowingLambdas.ThrowingFunction<IS, OS, X> throwingFunction) {
        return tryTo(throwingFunction, identity());
    }

    /**
     * Returns a function which takes a value, applies the provided function to it, and returns
     * a success of the output of that function. If an exception is thrown, return a failure of
     * the specified failure case value.
     * <p>
     * Whilst we take a ThrowingFunction which throws a specific checked exception type X, our
     * eventual Result is of the more general type Exception. That's because it's also possible for the
     * function to throw other types of RuntimeException, and we have two choices: don't catch (or rethrow)
     * RuntimeException, or have a more general failure type. Rethrowing exceptions goes against the whole
     * point of constraining the error path, so we opt for the latter.
     * <p>
     * If the provided function throws an Error, we don't catch that. Errors in general are not
     * intended to be caught.
     * <p>
     * Note that idiomatic handling of Exceptions as failure type does allow specialised catch blocks
     * on specific exception types.
     * @param throwingFunction function that takes an input type IS, returns an output type OS, or throws X
     * @param failureCase function mapping exception to failure?
     * @param <IS> input type
     * @param <OS> output type
     * @param <X> exception type
     * @param <F> failure value
     * @return dF(x -> y) where x is of type IS and y is a result of type R(OS, F)
     */
    @Contract(pure = true)
    static <IS, OS, X extends Exception, F> @NotNull Function<IS, Result<OS, F>>
    tryTo(ThrowingLambdas.ThrowingFunction<IS, OS, X> throwingFunction, F failureCase) {
        return tryTo(throwingFunction, __ -> failureCase);
    }


    /**
     * Returns a function which takes a value, applies the provided stream-returning function to it,
     * and return a stream which is the stream returned by the function, with each element wrapped in
     * a success, or a single failure of the exception thrown by that function if it threw an exception.
     * @param f function that may throw an exception
     * @param <IS> input type
     * @param <OS> output type
     * @param <X> exception type
     * @return dF(x -> y) where x is provided to f and returns a stream of success results or a single failure result
     * of the exception type X if f failed
     */
    static <IS, OS, X extends Exception> @NotNull Function<IS, Stream<Result<OS, Exception>>>
    tryAndUnwrap(ThrowingLambdas.ThrowingFunction<IS, Stream<OS>, X> f) {
        return tryTo(f).andThen(unwrapSuccesses());
    }

    /**
     * Takes a class and returns a function which takes a value, attempts to cast it to that class, and returns
     * a Success of the provided type if it's a member of it, and a Failure of the known type otherwise, in both
     * cases containing the input value.
     * <p>
     * This differs from exactCastTo in that exactCastTo will only return a Success if the given value is exactly
     * the target type, whereas this will also return a Success if it is a subtype of that type.
     * @param targetClass class to cast to
     * @param <IS> input type
     * @param <OS> output type
     * @return result with either cast type or failure
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    static <IS, OS extends IS> @NotNull Function<IS, Result<OS, IS>>
    castTo(Class<OS> targetClass) {
        return input -> targetClass.isAssignableFrom(input.getClass())
            ? Result.success((OS)input)
            : Result.failure(input);
    }

    /**
     * Takes a class and returns a function which takes a value, attempts to cast it to that class, and returns
     * a Success of the provided type if it's the same type as it, and a Failure of the known type otherwise, in both
     * cases containing the input value.
     * <p>
     * This differs from castTo in that castTo will return a Success if the given value is a subtype of the target
     * type, whereas this will only return a Success if it is exactly that type.
     * @param targetClass class to cast to
     * @param <IS> input type
     * @param <OS> success output type
     * @return result with either cast type or fail tye
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    static <IS, OS extends IS> @NotNull Function<IS, Result<OS, IS>>
    exactCastTo(Class<OS> targetClass) {
        return input -> targetClass.equals(input.getClass())
            ? Result.success((OS)input)
            : Result.failure(input);
    }


    /**
     * Takes a java.util.Map and a failure function, and returns a function which takes a key and returns
     * a success of the associated value in the Map, if present, or applies the failure function to the
     * key otherwise.
     * @param map map with K -> S relationship
     * @param failureProvider function to call with K in fail case
     * @param <K> key
     * @param <S> success type
     * @param <F> failure type
     * @return result based on whether K is in map
     */
    @Contract(pure = true)
    static <K, S, F> @NotNull Function<K, Result<S, F>>
    fromMap(Map<K, S> map, Function<K, F> failureProvider) {
        return key -> {
            if(map.containsKey(key)) {
                return Result.success(map.get(key));
            } else {
                return Result.failure(failureProvider.apply(key));
            }
        };
    }
}
