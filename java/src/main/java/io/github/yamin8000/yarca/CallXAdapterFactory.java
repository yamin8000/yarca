package io.github.yamin8000.yarca;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

class CallXAdapterFactory extends CallAdapter.Factory {

    private static OkHttpClient okHttpClient;
    private static Executor callbackExecutor;

    public CallXAdapterFactory(OkHttpClient okHttpClient) {
        CallXAdapterFactory.okHttpClient = okHttpClient;
        callbackExecutor = okHttpClient.dispatcher().executorService();
    }

    public CallXAdapterFactory(OkHttpClient okHttpClient, Executor callbackExecutor) {
        CallXAdapterFactory.okHttpClient = okHttpClient;
        CallXAdapterFactory.callbackExecutor = callbackExecutor;
    }

    @Override
    public CallAdapter<?, ?> get(@NotNull Type type, Annotation @NotNull [] annotations, @NotNull Retrofit retrofit) {
        //this line will allow retrofit to use default call adapter for types other than CallX
        if (getRawType(type) != CallX.class) {
            return null;
        }

        if (!(type instanceof ParameterizedType)) {
            throw new IllegalStateException("Call must have generic type (e.g., Call<ResponseBody>)");
        }

        final Type responseType = getParameterUpperBound(0, (ParameterizedType) type);
        return new CallXAdapter<>(responseType);
    }

    private static final class CallXAdapter<R> implements CallAdapter<R, CallX<R>> {
        private final Type responseType;

        CallXAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public @NotNull Type responseType() {
            return responseType;
        }

        @Override
        public @NotNull CallX<R> adapt(@NotNull Call<R> call) {
            return new MyCallXAdapter<>(call);
        }
    }

    private static class MyCallXAdapter<T> implements CallX<T> {
        private static final String CANCELED = " Canceled";
        private final Call<T> call;

        MyCallXAdapter(Call<T> call) {
            this.call = call;
        }

        @Override
        public @NotNull Response<T> execute() throws IOException {
            return call.execute();
        }

        @Override
        public void enqueue(@NotNull Callback<T> callback) {
            callbackExecutor.execute(() -> call.enqueue(callback));
        }

        @Override
        public boolean isExecuted() {
            return call.isExecuted();
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }

        @Override
        public @NotNull Request request() {
            return call.request();
        }

        @Override
        public @NotNull Timeout timeout() {
            return call.timeout();
        }

        @Override
        public @NotNull Call<T> clone() {
            return new MyCallXAdapter<>(call.clone());
        }

        @Override
        public void async(@NotNull BiFunction<Response<T>, Throwable, Boolean> callback) {
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NotNull Call<T> call, @NotNull Response<T> response) {
                    callbackExecutor.execute(() -> {
                        var isShutdownNeeded = false;

                        if (call.isCanceled()) {
                            isShutdownNeeded = callback.apply(null, new IOException(request() + CANCELED));
                        } else {
                            isShutdownNeeded = callback.apply(response, null);
                        }

                        if (isShutdownNeeded) {
                            shutdown();
                        }
                    });
                }

                @Override
                public void onFailure(@NotNull Call<T> call, @NotNull Throwable t) {
                    callbackExecutor.execute(() -> {
                        var isShutdownNeeded = false;

                        Throwable throwable;
                        if (call.isCanceled()) {
                            throwable = new IOException(request() + CANCELED);
                        } else {
                            throwable = t;
                        }

                        isShutdownNeeded = callback.apply(null, throwable);

                        if (isShutdownNeeded) {
                            shutdown();
                        }
                    });
                }
            });
        }

        @Override
        public void async(boolean isShutdownNeeded, @NotNull Consumer<Response<T>> onSuccess, @NotNull Consumer<Throwable> onFailure) {
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NotNull Call<T> call, @NotNull Response<T> response) {
                    callbackExecutor.execute(() -> {
                        if (call.isCanceled()) {
                            onFailure.accept(new IOException(request() + CANCELED));
                        } else {
                            onSuccess.accept(response);
                        }

                        if (isShutdownNeeded) {
                            shutdown();
                        }
                    });
                }

                @Override
                public void onFailure(@NotNull Call<T> call, @NotNull Throwable t) {
                    callbackExecutor.execute(() -> {
                        onFailure.accept(t);

                        if (isShutdownNeeded) {
                            shutdown();
                        }
                    });
                }
            });
        }

        /**
         * By default, OkHttp uses non-daemon thread,
         * this will prevent the JVM from exiting until they time out.
         * so this method is used for shutting down threads manually.
         */
        private void shutdown() {
            if (okHttpClient != null) {
                okHttpClient.dispatcher().executorService().shutdown();
                okHttpClient.connectionPool().evictAll();
            }
        }
    }
}
