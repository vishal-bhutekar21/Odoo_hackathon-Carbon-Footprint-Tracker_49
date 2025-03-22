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
import org.json.JSONException;
import org.json.JSONObject;

public class ElectricityEmission extends AppCompatActivity {

    private EditText inputElectricity;
    private TextView electricityResult, carbonG, carbonLb, carbonKg, carbonMt, estimatedAt, totalEmissions;
    private MaterialCardView resultCard;
    private Button btnCalculate;
    private LinearLayout listContainer;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;
    private String currentMonth;

    private static final String API_URL = "https://www.carboninterface.com/api/v1/estimates";
    private static final String API_KEY = "EEjDUmc5BvD0n5ibNojQ";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_electricity_emission);

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
                    .child("electricity_emissions");
        } else {
            Log.e("ElectricityEmission", "Email not found in SharedPreferences");
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        inputElectricity = findViewById(R.id.inputElectricity);
        electricityResult = findViewById(R.id.electricityResult);
        carbonG = findViewById(R.id.carbonG);
        carbonLb = findViewById(R.id.carbonLb);
        carbonKg = findViewById(R.id.carbonKg);
        carbonMt = findViewById(R.id.carbonMt);
        estimatedAt = findViewById(R.id.estimatedAt);
        totalEmissions = findViewById(R.id.totalEmissions);
        resultCard = findViewById(R.id.resultCard);
        btnCalculate = findViewById(R.id.btnCalculate);
        listContainer = findViewById(R.id.listContainer);

        // Get current month in "YYYY-MMMM" format (e.g., "2025-March")
        currentMonth = new SimpleDateFormat("yyyy-MMMM", Locale.getDefault()).format(new Date());

        // Initialize Firebase Database with month-specific node
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("carbonviewcalculations/manualaddedemissions/electricalemissions/" + currentMonth);

        // Load total emissions for the current month
        loadTotalEmissions();

        btnCalculate.setOnClickListener(v -> {
            String input = inputElectricity.getText().toString();
            if (!input.isEmpty()) {
                try {
                    double energyUsed = Double.parseDouble(input);
                    getElectricityEmission(energyUsed);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid input. Enter a valid number.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter electricity consumption", Toast.LENGTH_SHORT).show();
            }
        });

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




    private void getElectricityEmission(double energyUsed) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("type", "electricity");
            jsonRequest.put("electricity_unit", "kwh");
            jsonRequest.put("electricity_value", energyUsed);
            jsonRequest.put("country", "US");
        } catch (JSONException e) {
            showError("JSON Creation Error: " + e.getMessage());
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonRequest.toString(), JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

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

                final String responseData = response.body().string();
                Log.d("API Response", responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONObject data = jsonResponse.optJSONObject("data");

                    if (data != null) {
                        JSONObject attributes = data.optJSONObject("attributes");
                        if (attributes != null) {
                            double carbonGValue = attributes.optDouble("carbon_g", -1);
                            double carbonLbValue = attributes.optDouble("carbon_lb", -1);
                            double carbonKgValue = attributes.optDouble("carbon_kg", -1);
                            double carbonMtValue = attributes.optDouble("carbon_mt", -1);
                            String estimatedDate = attributes.optString("estimated_at", "N/A");

                            if (carbonGValue >= 0 && carbonLbValue >= 0 && carbonKgValue >= 0 && carbonMtValue >= 0) {
                                runOnUiThread(() -> {
                                    resultCard.setVisibility(View.VISIBLE);
                                    electricityResult.setText("Electricity Emission: " + carbonKgValue + " kg CO₂");
                                    carbonG.setText("Carbon Emission (g): " + carbonGValue);
                                    carbonLb.setText("Carbon Emission (lb): " + carbonLbValue);
                                    carbonKg.setText("Carbon Emission (kg): " + carbonKgValue);
                                    carbonMt.setText("Carbon Emission (MT): " + carbonMtValue);
                                    estimatedAt.setText("Estimated At: " + estimatedDate);

                                    addListItem(carbonKgValue, energyUsed);
                                });
                            } else {
                                showError("Invalid carbon emission data received");
                            }
                        } else {
                            showError("Invalid carbon emission data");
                        }
                    } else {
                        showError("Missing attributes in response");
                    }
                } catch (JSONException e) {
                    showError("JSON Parsing Error: " + e.getMessage());
                }
            }
        });
    }

    private void addListItem(double carbonKgValue, double energyUsed) {
        View listItem = LayoutInflater.from(this).inflate(R.layout.list1, listContainer, false);

        TextView co2Text = listItem.findViewById(R.id.co2Text);
        Button saveButton = listItem.findViewById(R.id.saveButton);
        Button dismissButton = listItem.findViewById(R.id.dismissButton);

        co2Text.setText("CO₂ Emission: " + carbonKgValue + " kg");

        saveButton.setOnClickListener(v -> {
            saveToFirebase(carbonKgValue, energyUsed);
            listContainer.removeView(listItem);
        });

        dismissButton.setOnClickListener(v -> listContainer.removeView(listItem));

        listContainer.removeAllViews();
        listContainer.addView(listItem);
    }

    private void saveToFirebase(double carbonKgValue, double energyUsed) {
        String entryId = databaseReference.push().getKey();
        if (entryId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("carbon_kg", carbonKgValue);
            data.put("energy_used", energyUsed);
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
                    storeElectricityEmissions(finalTotal);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showError("Error loading total emissions: " + databaseError.getMessage());
            }
        });
    }

    private void storeElectricityEmissions(double totalEmissions) {
        Map<String, Object> emissionsData = new HashMap<>();
        emissionsData.put("emissions", totalEmissions);


        userRef.setValue(emissionsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseSuccess", "Electricity emissions stored successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Error storing electricity emissions", e);
                    Toast.makeText(this, "Failed to store electricity emissions", Toast.LENGTH_SHORT).show();
                });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(ElectricityEmission.this, message, Toast.LENGTH_LONG).show());
    }
}