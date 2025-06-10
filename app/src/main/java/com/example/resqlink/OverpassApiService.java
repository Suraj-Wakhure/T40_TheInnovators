package com.example.resqlink;


import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface OverpassApiService {
    @POST("/api/interpreter")
    @FormUrlEncoded
    Call<OverpassResponse> getHospitals(@Field("data") String query);
}
