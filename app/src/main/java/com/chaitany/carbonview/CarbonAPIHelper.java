package com.chaitany.carbonview;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class CarbonAPIHelper {
    private static final String API_URL = "https://www.carboninterface.com/api/v1/estimates";
    private static final String API_KEY = "EEjDUmc5BvD0n5ibNojQ"; // Replace with your actual API key

    private final OkHttpClient client = new OkHttpClient();

    // Method for Electricity Emission
    public void getElectricityEmission(double electricityUsed, okhttp3.Callback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("type", "electricity");
            jsonBody.put("electricity_unit", "mwh");
            jsonBody.put("electricity_value", electricityUsed);
            jsonBody.put("country", "US");

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method for Fuel Emission
    public void getFuelEmission(String fuelType, double fuelAmount, okhttp3.Callback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("type", "fuel_combustion");
            jsonBody.put("fuel_source_type", "vehicle");
            jsonBody.put("fuel_type", fuelType);
            jsonBody.put("fuel_amount", fuelAmount);
            jsonBody.put("fuel_unit", "liters");

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method for Flight Emission
    public void getFlightEmission(String departure, String destination, okhttp3.Callback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("type", "flight");
            jsonBody.put("passengers", 1);
            jsonBody.put("legs", new org.json.JSONArray()
                    .put(new JSONObject().put("departure_airport", departure)
                            .put("destination_airport", destination)));

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
