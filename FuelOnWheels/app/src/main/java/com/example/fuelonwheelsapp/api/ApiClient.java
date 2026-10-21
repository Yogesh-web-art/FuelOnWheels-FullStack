package com.example.fuelonwheelsapp.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    // Default to Android Emulator loopback to host (10.0.2.2).
    // Can be updated to machine local IP (e.g., http://192.168.1.15:8080/) when running on physical device.
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/";
    private static String customBaseUrl = DEFAULT_BASE_URL;

    private static Retrofit retrofit = null;
    private static FuelApiService apiService = null;

    public static synchronized void setBaseUrl(String newUrl) {
        if (newUrl != null && !newUrl.endsWith("/")) {
            newUrl += "/";
        }
        customBaseUrl = newUrl;
        retrofit = null;
        apiService = null;
    }

    public static synchronized FuelApiService getApiService() {
        if (apiService == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(customBaseUrl != null ? customBaseUrl : DEFAULT_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();   

            apiService = retrofit.create(FuelApiService.class);
        }
        return apiService;
    }
}
