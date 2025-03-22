package com.chaitany.carbonview;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FlightEmission extends AppCompatActivity {

    private EditText departureAirport, destinationAirport, passengerCount;
    private Button calculateButton;
    private MaterialCardView resultCard;
    private TextView emissionTitle, estimatedAt, carbonG, carbonLb, carbonKg, carbonMt, distanceResult, distanceUnit, totalEmissions;
    private LinearLayout listContainer;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;
    private String currentMonth;

    private final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "https://www.carboninterface.com/api/v1/estimates";
    private static final String API_KEY = "EEjDUmc5BvD0n5ibNojQ";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_emission);

        // Setup toolbar


        // Get email from SharedPreferences and initialize user reference
        String email = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                .getString("email", null);
        if (email != null) {
            String safeEmail = email.replace(".", ",");
            userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(safeEmail)
                    .child("emissions_data")
                    .child("flight_emissions");
        } else {
            Log.e("FlightEmission", "Email not found in SharedPreferences");
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        departureAirport = findViewById(R.id.departureAirport);
        destinationAirport = findViewById(R.id.destinationAirport);
        passengerCount = findViewById(R.id.passengerCount);
        calculateButton = findViewById(R.id.calculateButton);
        resultCard = findViewById(R.id.resultCard);
        emissionTitle = findViewById(R.id.emissionTitle);
        estimatedAt = findViewById(R.id.estimatedAt);
        carbonG = findViewById(R.id.carbonG);
        carbonLb = findViewById(R.id.carbonLb);
        carbonKg = findViewById(R.id.carbonKg2);
        carbonMt = findViewById(R.id.carbonMt);
        distanceResult = findViewById(R.id.distanceResult);
        distanceUnit = findViewById(R.id.distanceUnit);
        listContainer = findViewById(R.id.listContainer);
        totalEmissions = findViewById(R.id.totalEmissions);

        // Get current month in "YYYY-MMMM" format (e.g., "2025-March")
        currentMonth = new SimpleDateFormat("yyyy-MMMM", Locale.getDefault()).format(new Date());

        String safeEmail = email.replace(".", ",");
        // Initialize Firebase Database with month-specific node
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("carbonviewcalculations/"+safeEmail+"/manualaddedemissions/electricalemissions/" + currentMonth);

        // Load total emissions for the current month
        loadTotalEmissions();

        calculateButton.setOnClickListener(v -> calculateEmission());

        // Animate result card entrance
        resultCard.setTranslationY(200f);
        resultCard.setAlpha(0f);
        resultCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }



    private void calculateEmission() {
        String departure = departureAirport.getText().toString().trim().toUpperCase();
        String destination = destinationAirport.getText().toString().trim().toUpperCase();
        String passengers = passengerCount.getText().toString().trim();

        if (departure.isEmpty() || destination.isEmpty() || passengers.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int passengerNum;
        try {
            passengerNum = Integer.parseInt(passengers);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number of passengers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare JSON Request Body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("type", "flight");
            requestBody.put("passengers", passengerNum);

            JSONArray legsArray = new JSONArray();
            JSONObject leg = new JSONObject();
            leg.put("departure_airport", departure);
            leg.put("destination_airport", destination);
            legsArray.put(leg);

            requestBody.put("legs", legsArray);
        } catch (JSONException e) {
            showError("JSON Creation Error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(requestBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute API Call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showError("API Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showError("API Response Error: " + response.message());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONObject attributes = jsonResponse.getJSONObject("data").getJSONObject("attributes");

                    String estimatedAtValue = attributes.getString("estimated_at");
                    double carbonGValue = attributes.getDouble("carbon_g");
                    double carbonLbValue = attributes.getDouble("carbon_lb");
                    double carbonKgValue = attributes.getDouble("carbon_kg");
                    double carbonMtValue = attributes.getDouble("carbon_mt");
                    double distanceValue = attributes.getDouble("distance_value");
                    String distanceUnitValue = attributes.getString("distance_unit");

                    runOnUiThread(() -> {
                        resultCard.setVisibility(View.VISIBLE);
                        estimatedAt.setText("Estimated At: " + estimatedAtValue);
                        carbonG.setText(String.format("Carbon Emission (g): %.2f", carbonGValue));
                        carbonLb.setText(String.format("Carbon Emission (lb): %.2f", carbonLbValue));
                        carbonKg.setText(String.format("Carbon Emission (kg): %.2f", carbonKgValue));
                        carbonMt.setText(String.format("Carbon Emission (MT): %.2f", carbonMtValue));
                        distanceResult.setText(String.format("Distance: %.2f %s", distanceValue, distanceUnitValue));
                        distanceUnit.setText("Distance Unit: " + distanceUnitValue);

                        // Add list item with carbon_kg value
                        addListItem(carbonKgValue, departure, destination, passengerNum);
                    });

                } catch (JSONException e) {
                    showError("JSON Parsing Error: " + e.getMessage());
                }
            }
        });
    }

    private void addListItem(double carbonKgValue, String departure, String destination, int passengerNum) {
        View listItem = LayoutInflater.from(this).inflate(R.layout.list1, listContainer, false);

        TextView co2Text = listItem.findViewById(R.id.co2Text);
        Button saveButton = listItem.findViewById(R.id.saveButton);
        Button dismissButton = listItem.findViewById(R.id.dismissButton);

        co2Text.setText("CO₂ Emission: " + carbonKgValue + " kg");

        saveButton.setOnClickListener(v -> {
            saveToFirebase(carbonKgValue, departure, destination, passengerNum);
            listContainer.removeView(listItem);
        });

        dismissButton.setOnClickListener(v -> listContainer.removeView(listItem));

        listContainer.removeAllViews();
        listContainer.addView(listItem);
    }

    private void saveToFirebase(double carbonKgValue, String departure, String destination, int passengerNum) {
        String entryId = databaseReference.push().getKey();
        if (entryId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("carbon_kg", carbonKgValue);
            data.put("departure_airport", departure);
            data.put("destination_airport", destination);
            data.put("passengers", passengerNum);
            data.put("timestamp", System.currentTimeMillis());

            databaseReference.child(entryId).setValue(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Data saved for " + currentMonth + "!", Toast.LENGTH_SHORT).show();
                        loadTotalEmissions();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void loadTotalEmissions() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double total = 0.0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double carbonKg = snapshot.child("carbon_kg").getValue(Double.class);
                    if (carbonKg != null) {
                        total += carbonKg;
                    }
                }
                double finalTotal = total;
                runOnUiThread(() -> {
                    totalEmissions.setVisibility(View.VISIBLE);
                    totalEmissions.setText("Total Emissions for " + currentMonth + ": " + String.format("%.2f", finalTotal) + " kg CO₂");
                    storeFlightEmissions(finalTotal);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showError("Error loading total emissions: " + databaseError.getMessage());
            }
        });
    }

    private void storeFlightEmissions(double totalEmissions) {
        Map<String, Object> emissionsData = new HashMap<>();
        emissionsData.put("emissions", totalEmissions);

        userRef.setValue(emissionsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseSuccess", "Flight emissions stored successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Error storing flight emissions", e);
                    Toast.makeText(this, "Failed to store flight emissions", Toast.LENGTH_SHORT).show();
                });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(FlightEmission.this, message, Toast.LENGTH_LONG).show());
    }
}