package io.github.yamin8000.yarca;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Response;

public interface CallX<T> extends Call<T> {

    static void doOnEvent(
            @NotNull LifecycleOwner lifecycleOwner,
            @NotNull Lifecycle.Event inputEvent,
            @NotNull Runnable runnable
    ) {
        lifecycleOwner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (inputEvent == event) runnable.run();
        });
    }

    /**
     * <b>General purpose async call method.</b>
     * <p>
     * {@link  T} is a type parameter that is used to show the type of the response body.
     *
     * @param callback Represents the callback function which will be called when the request is finished or when an error occurs.
     *                 The callback function will be called with two parameters:
     *                 <ul>
     *                 <li>The first parameter is the generic retrofit response object. {@link Response<T>}</li>
     *                 <li>The second parameter is the throwable object. {@link Throwable}</li>
     *                 </ul>
     *                 The callback function will/shall return a boolean value which indicates whether the shutdown of the client is needed or not:
     *                 <ul>
     *                 <li>If the return value is true, the client will be shutdown.</li>
     *                 <li>If the return value is false, the client will not be shutdown.</li>
     *                 <li>If the return value is null, the client will not be shutdown.</li>
     *                 </ul>
     *                 The default value is false.
     *                 <p>
     *                 More info about why auto shutdown is needed:
     *                 <p>
     *                 By default, OkHttp uses non-daemon thread,
     *                 this will prevent the JVM from exiting until they time out.
     *                 however, if you are using OkHttp in a non-daemon thread, like Android, this is unnecessary.
     */
    void async(@NotNull BiFunction<@Nullable Response<T>, @Nullable Throwable, @NotNull Boolean> callback);

    /**
     * Calls {@link #async(BiFunction)} and automatically shuts down the client.
     *
     * @param callback The callback function.
     */
    default void atomicAsync(@NotNull BiConsumer<@Nullable Response<T>, @Nullable Throwable> callback) {
        async((response, throwable) -> {
            callback.accept(response, throwable);
            return true;
        });
    }

    /**
     * Calls {@link #async(BiFunction)} and doesn't care whether to shut down the client or not.
     *
     * @param callback The callback function.
     */
    default void enqueueAsync(@NotNull BiConsumer<@Nullable Response<T>, @Nullable Throwable> callback) {
        async((response, throwable) -> {
            callback.accept(response, throwable);
            return false;
        });
    }

    /**
     * Calls {@link #enqueueAsync(BiConsumer)}} and cancel call if lifecycle is destroyed
     *
     * @param lifecycleOwner owner of lifecycle
     * @param callback       The callback function.
     */
    default void enqueueAsync(
            @NotNull LifecycleOwner lifecycleOwner,
            @NotNull BiConsumer<@Nullable Response<T>, @Nullable Throwable> callback
    ) {
        doOnEvent(lifecycleOwner, Lifecycle.Event.ON_DESTROY, this::cancel);
        enqueueAsync(callback);
    }

    /**
     * Calls {@link #asyncBody(boolean, BiConsumer)} with false parameter and returns the response body.
     * <p>
     * This method is useful when you want to get the response body without caring about the response status code.
     * <p>
     * This method don't care whether to shut down the client or not.
     *
     * @param callback The callback function.
     */
    default void enqueueAsyncBody(@NotNull BiConsumer<@Nullable T, @Nullable Throwable> callback) {
        asyncBody(false, callback);
    }

    /**
     * Calls {@link #async(boolean, BiConsumer)} but instead of determining whether to shut down the client or not
     * based on the return value of the callback function, it will be determined by the given boolean value.
     * <p>
     * However, this method only returns the response body.
     * <p>
     * This method is useful when you want to get the response body without caring about the response status code or headers.
     *
     * @param isShutdownNeeded The boolean value which determines whether to shut down the client or not.
     * @param callback         The callback function.
     */
    default void asyncBody(
            boolean isShutdownNeeded,
            @NotNull BiConsumer<@Nullable T, @Nullable Throwable> callback
    ) {
        async(isShutdownNeeded, (response, throwable) -> {
            if (response == null) {
                callback.accept(null, throwable);
            } else {
                callback.accept(response.body(), throwable);
            }
        });
    }

    /**
     * Calls {@link #async(BiFunction)} but instead of determining whether to shut down the client or not
     * based on the return value of the callback function, it will be determined by the given boolean value.
     *
     * @param isShutdownNeeded The boolean value which determines whether to shut down the client or not.
     * @param callback         The callback function.
     */
    default void async(
            boolean isShutdownNeeded,
            @NotNull BiConsumer<@Nullable Response<T>, @Nullable Throwable> callback
    ) {
        async((response, throwable) -> {
            callback.accept(response, throwable);
            return isShutdownNeeded;
        });
    }

    /**
     * Calls {@link #asyncBody(boolean, BiConsumer)} with true parameter and returns the response body.
     * <p>
     * This method is useful when you want to get the response body without caring about the response status code.
     * <p>
     * This method will shut down the client after the callback function is called.
     *
     * @param callback The callback function.
     */
    default void atomicAsyncBody(@NotNull BiConsumer<@Nullable T, @Nullable Throwable> callback) {
        asyncBody(true, callback);
    }

    /**
     * Calls {@link #async(boolean, Consumer, Consumer)} and automatically shuts down the client.
     *
     * @param onSuccess The callback function which is called when an HTTP response is received and is not canceled.
     * @param onFailure The callback function which is called when a network exception occurred talking to the server or
     *                  when an unexpected exception occurred creating the request or processing the response.
     */
    default void atomicAsync(
            @NotNull Consumer<@Nullable Response<T>> onSuccess,
            @NotNull Consumer<Throwable> onFailure
    ) {
        async(true, onSuccess, onFailure);
    }

    /**
     * <b>General purpose async call method with two callbacks</b>
     *
     * @param isShutdownNeeded The boolean value which determines whether to shut down the client or not.
     * @param onSuccess        The callback function which is called when an HTTP response is received and is not canceled.
     * @param onFailure        The callback function which is called when a network exception occurred talking to the server or
     *                         when an unexpected exception occurred creating the request or processing the response.
     */
    void async(
            boolean isShutdownNeeded,
            @NotNull Consumer<@Nullable Response<T>> onSuccess,
            @NotNull Consumer<Throwable> onFailure
    );

    /**
     * Calls {@link #async(boolean, Consumer, Consumer)} and doesn't care whether to shut down the client or not.
     *
     * @param onSuccess The callback function which is called when an HTTP response is received and is not canceled.
     * @param onFailure The callback function which is called when a network exception occurred talking to the server or
     */
    default void enqueueAsync(
            @NotNull Consumer<@Nullable Response<T>> onSuccess,
            @NotNull Consumer<Throwable> onFailure
    ) {
        async(false, onSuccess, onFailure);
    }
}
