package io.github.yamin8000.yarca;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import io.github.yamin8000.yarca.databinding.ActivityMainBinding;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d("test", "hello there");

        var client = new OkHttpClient.Builder().build();
        var executor = ContextCompat.getMainExecutor(this);
        var callAdapter = new CallXAdapterFactory(client, executor);

        var retrofit = new Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(callAdapter)
                .build();

        var api = retrofit.create(APIs.class).getPosts();
        api.enqueueAsync(this, ((listResponse, throwable) -> {
            if (listResponse != null && throwable == null)
                binding.text.setText(String.valueOf(listResponse.code()));
            else Log.d("-#-", throwable.getMessage());
        }));

        //cancelOnDestroy(this, () -> api);

/*        doOnEvent(this, Lifecycle.Event.ON_CREATE, () -> {
            Log.d("-#-", "started");
        });

        api.atomicAsync((listResponse -> {
            Log.d("-#-", String.valueOf(listResponse.code()));
        }), (throwable -> {
            throwable.printStackTrace();
        }));*/
    }
}