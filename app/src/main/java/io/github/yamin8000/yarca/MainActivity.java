package io.github.yamin8000.yarca;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;

import io.github.yamin8000.yarca.databinding.ActivityMainBinding;
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

        var retrofit = new Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(new CallXAdapterFactory())
                .build();

        var api = retrofit.create(APIs.class).getPosts();

        api.enqueue((call, response) -> {
            //handle response
        }, (call, error) -> {
            //handle error
        });

        var defaultPosts = new ArrayList<Post>();
        defaultPosts.add(new Post("1", "1", "yamin", "yamin yamin"));
        defaultPosts.add(new Post("1", "1", "yamin", "yamin yamin"));

        CallX.doOnEvent(this, Lifecycle.Event.ON_DESTROY, api::cancel);

        api.enqueueAsyncBody((response) -> {
            Log.d("test", String.valueOf(response.size()));
        }, (error) -> {
            Log.d("test", error.getMessage());
        }, () -> defaultPosts);

        api.async((call, response) -> {
            var posts = response;
            return false;
        });

//        api.enqueueAsync(this, ((listResponse, throwable) -> {
//            if (listResponse != null && throwable == null)
//                binding.text.setText(String.valueOf(listResponse.code()));
//            else Log.d("-#-", throwable.getMessage());
//        }));

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