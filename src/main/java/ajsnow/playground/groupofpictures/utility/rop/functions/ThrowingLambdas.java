package ajsnow.playground.groupofpictures.utility.rop.functions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A collection of functional interfaces which throw, and convenience functions to wrap them
 * so any thrown exceptions are converted to RuntimeExceptions so they can be used where
 * non-throwing functional interfaces are required
 */
@SuppressWarnings("ALL")
public interface ThrowingLambdas {

    /**
     * A Function which may throw a checked exception
     */
    @SuppressWarnings("unused")
    @FunctionalInterface
    interface ThrowingFunction<I, O, X extends Exception> {
        /**
         * @param input value
         * @return O
         * @throws X exception type
         */
        O apply(I input) throws X;

        /**
         * @param nextFunction to use in pipe chain
         * @param <T> output type
         * @return dF(x -> y) where x is piped into outer throwing function
         * and then passed to the next function and y is T or it throws
         */
        default <T> ThrowingFunction<I, T, X> andThen(Function<O, T> nextFunction) {
            return x -> nextFunction.apply(apply(x));
        }

        /**
         * @param nextFunction to use in pipe chain
         * @param <T> input type
         * @return dF(x -> y) where x is piped into the next function
         * and y is O or it throws
         */
        default <T> ThrowingFunction<T, O, X> compose(Function<T, I> nextFunction) {
            return x -> apply(nextFunction.apply(x));
        }

        /**
         * Converts the provided function into a regular Function, where any thrown exceptions are
         * wrapped in a RuntimeException.
         * @param f I/O function that may throw X
         * @param <I> input type
         * @param <O> output type
         * @param <X> exception type
         * @return dF(x -> y) where f is passed x and wrapped in a try-catch that
         * throws a runtime
         */
        @Contract(pure = true)
        static <I, O, X extends Exception> @NotNull Function<I, O>
        throwingRuntime(ThrowingFunction<I, O, X> f) {
            return x -> {
                try {
                    return f.apply(x);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }
    }

    /**
     * A Consumer which may throw a checked exception
     */
    @SuppressWarnings("unused")
    @FunctionalInterface
    interface ThrowingConsumer<T, X extends Exception> {
        /**
         * @param item value
         * @throws X exception type
         */
        void accept(T item) throws X;

        /**
         * Converts the provided consumer into a regular Consumer, where any thrown exceptions are
         * wrapped in a RuntimeException.
         * @param p consumer that throws
         * @param <T> input type
         * @param <X> exception type
         * @return dF(x -> y) where x is passed to p and p
         * is wrapped in a try catch that throws a runtime exception
         */
        @Contract(pure = true)
        static <T, X extends Exception> @NotNull Consumer<T>
        throwingRuntime(ThrowingConsumer<T, X> p) {
            return x -> {
                try {
                    p.accept(x);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }
    }

    /**
     * A BiFunction which may throw a checked exception
     * @param <A> left type
     * @param <B> right type
     * @param <R> result type
     * @param <X> exception type
     */
    @SuppressWarnings("unused")
    @FunctionalInterface
    interface ThrowingBiFunction<A, B, R, X extends Exception> {
        /**
         * @param first .
         * @param second .
         * @return R
         * @throws X exception type
         */
        R apply(A first, B second) throws X;

        /**
         * Converts the provided bifunction into a regular BiFunction, where any thrown exceptions
         * are wrapped in a RuntimeException
         * @param f bifunction that may throw
         * @param <A> left type
         * @param <B> right type
         * @param <R> result type
         * @param <X> exception type
         * @return dF((a, b) -> y) where a and b are passed to f
         * and f is wrapped in a try-catch that throws a runtime exception
         */
        @Contract(pure = true)
        static <A, B, R, X extends Exception> @NotNull BiFunction<A, B, R>
        throwingRuntime(ThrowingBiFunction<A, B, R, X> f) {
            return (a, b) -> {
                try {
                    return f.apply(a, b);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }
    }

    /**
     * A Predicate which may throw a checked exception
     * @param <T> input type
     * @param <X> exception type
     */
    @FunctionalInterface
    interface ThrowingPredicate<T, X extends Exception> {
        /**
         * @param item to test
         * @return boolean result of test
         * @throws X throw type
         */
        boolean test(T item) throws X;

        /**
         * Converts the provided predicate into a regular Predicate, where any thrown exceptions
         * are wrapped in a RuntimeException
         * @param p predicate that may throw
         * @param <T> input type
         * @param <X> output type
         * @return dF(x -> y) where p is wrapped in a try catch that throws a runtime
         */
        @Contract(pure = true)
        static <T, X extends Exception> @NotNull Predicate<T>
        throwingRuntime(ThrowingPredicate<T, X> p) {
            return x -> {
                try {
                    return p.test(x);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }
    }

    /**
     * @param consumer that may throw
     * @param <T> input type
     * @param <X> exception type
     * @return dF(x -> y) where x is passed to the consumer
     * and the consumer may or may not throw
     * and y is a boolean that represents if the consumer failed
     */
    @Contract(pure = true)
    static <T, X extends Exception> @NotNull Predicate<T>
    throwsWhen(ThrowingConsumer<T, X> consumer) {
        return t -> {
            try {
                consumer.accept(t);
                return false;
            } catch (Exception ex) {
                return true;
            }
        };
    }

    /**
     * @param consumer which may throw
     * @param <T> input type
     * @param <X> exception type
     * @return dF(x -> y) where the consumer may throw
     * and is thus wrapped in a try-catch that returns
     * true/false based on whether or not the consumer
     * threw
     */
    @Contract(pure = true)
    static <T, X extends Exception> @NotNull Predicate<T>
    doesntThrow(ThrowingConsumer<T, X> consumer) {
        return t -> {
            try {
                consumer.accept(t);
                return true;
            } catch (Exception ex) {
                return false;
            }
        };
    }
}
