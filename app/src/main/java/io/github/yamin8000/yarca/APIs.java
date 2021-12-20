package io.github.yamin8000.yarca;

import java.util.List;

import retrofit2.http.GET;

interface APIs {

    @GET("posts")
    CallX<List<Post>> getPosts();
}
