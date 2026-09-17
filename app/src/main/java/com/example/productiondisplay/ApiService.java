package com.example.productiondisplay;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface ApiService {
    @GET("https://pds.iotsignin.com/api/testing/user/get-data")
    Call<UserResponse> getUserData(@Header("Authorization") String authHeader);
}
