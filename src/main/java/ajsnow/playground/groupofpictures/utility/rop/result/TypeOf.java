package ajsnow.playground.groupofpictures.utility.rop.result;

import ajsnow.playground.groupofpictures.utility.rop.functions.HigherOrderFunctions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

import static ajsnow.playground.groupofpictures.utility.rop.result.Transformers.onSuccess;

/**
 * Some syntax-fu in order to get nice, readable up-casting operations on Results.
 * <p>
 * Usage isn't totally obvious from the implementation: to upcast a success to an Animal, for example, you need:
 * <code>
 *     using(TypeOf.{@literal (<Animal>)}forSuccesses())
 * </code>
 * <p>
 * That'll give you a {@code (Function<Result<Bear, String>, Function<Animal, String>>)} (inferring
 * the types Animal and String from context), which you can then use for mapping a Stream or use in
 * a Result then-operation chain.
 */
@SuppressWarnings("unused")
public interface TypeOf {

    /**
     * Generalises the success type for a Result to an appropriate superclass.
     * @param dummy class for overload differentiation
     * @param <WideSuccess> success parent type
     * @param <F> failure type
     * @param <NarrowSuccess> success child type
     * @return f(R(NS, F) -> R(WS, F))
     */
    @Contract(pure = true)
    static <WideSuccess, F, NarrowSuccess extends WideSuccess> @NotNull
            Function<Result<NarrowSuccess, F>, Result<WideSuccess, F>>
    using(ForSuccesses<WideSuccess> dummy) {
        Function<Result<NarrowSuccess, F>, Result<WideSuccess, F>> upcastSuccess
                = onSuccess(HigherOrderFunctions::upcast);
        return result -> result.then(upcastSuccess);
    }

    /**
     * Generalises the failure type for a Result to an appropriate superclass.
     * @param dummy class for overload differentiation
     * @param <S> success type
     * @param <WideType>> failure parent type
     * @param <NarrowType> failure child type
     * @return result with failure upcasted
     */
    @Contract(pure = true)
    static <S, WideType, NarrowType extends WideType> @NotNull
            Function<Result<S, NarrowType>, Result<S, WideType>>
    using(ForFailures<WideType> dummy) {
        Function<Result<S, NarrowType>, Result<S, WideType>> upcastFailure
                = Transformers.onFailure(HigherOrderFunctions::upcast);
        return result -> result.then(upcastFailure);
    }

    /**
     * @param <T> input type
     * @return null
     */
    // we don't use the return value - all this does is provide type context
    @Contract(pure = true)
    static <T> @Nullable ForSuccesses<T> forSuccesses() {
        return null;
    }

    /**
     * @param <T> input type
     * @return null
     */
    // we don't use the return value - all this does is provide type context
    @Contract(pure = true)
    static <T> @Nullable ForFailures<T> forFailures() {
        return null;
    }

    /**
     * @param <T> input type
     */
    // this class only exists, so we can differentiate the overloads of using()
    // we don't even instantiate it
    class ForSuccesses<T> { }

    /**
     * @param <T> input type
     */
    // this class only exists, so we can differentiate the overloads of using()
    // we don't even instantiate it
    class ForFailures<T> { }
}
