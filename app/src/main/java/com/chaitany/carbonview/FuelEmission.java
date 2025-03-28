package com.chaitany.carbonview;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuelEmission extends AppCompatActivity {

    private EditText inputFuelAmount;
    private Spinner fuelTypeSpinner;
    private TextView carbonG, carbonLb, carbonKg, carbonMt, estimatedAt, totalEmissions;
    private MaterialCardView resultCard;
    private Button btnCalculate;
    private LinearLayout listContainer;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;
    private String currentMonth;

    // Predefined emission factors (in kg CO2 per liter)
    private static final double EMISSION_FACTOR_PETROL = 2.31; // kg CO2 per liter
    private static final double EMISSION_FACTOR_DIESEL = 2.68; // kg CO2 per liter
    private static final double EMISSION_FACTOR_GAS = 1.96; // kg CO2 per liter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel_emission);

        // Set up the Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set status bar color to match the Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Get email from SharedPreferences and initialize user reference
        String email = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                .getString("email", null);
        if (email != null) {
            String safeEmail = email.replace(".", ",");
            userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(safeEmail)
                    .child("emissions_data")
                    .child("fuel_emissions");
        } else {
            Log.e("FuelEmission", "Email not found in SharedPreferences");
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        inputFuelAmount = findViewById(R.id.inputFuelAmount);
        fuelTypeSpinner = findViewById(R.id.feultypespinner);
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

        String safeEmail = email.replace(".", ",");
        // Initialize Firebase Database with month-specific node
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("carbonviewcalculations/" + safeEmail + "/manualaddedemissions/fuelemissions/" + currentMonth);

        // Populate the fuel type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fuelTypeSpinner.setAdapter(adapter);

        // Load total emissions for the current month
        loadTotalEmissions();

        btnCalculate.setOnClickListener(v -> {
            String fuelAmountStr = inputFuelAmount.getText().toString();
            if (!fuelAmountStr.isEmpty()) {
                try {
                    double fuelAmount = Double.parseDouble(fuelAmountStr);
                    calculateFuelEmission(fuelAmount);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid input. Enter a valid number.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter fuel amount", Toast.LENGTH_SHORT).show();
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

    private void calculateFuelEmission(double fuelAmount) {
        String selectedFuelType = fuelTypeSpinner.getSelectedItem().toString();
        double emissionFactor;

        switch (selectedFuelType) {
            case "Petrol":
                emissionFactor = EMISSION_FACTOR_PETROL;
                break;
            case "Diesel":
                emissionFactor = EMISSION_FACTOR_DIESEL;
                break;
            case "Gas":
                emissionFactor = EMISSION_FACTOR_GAS;
                break;
            default:
                showError("Invalid fuel type selected");
                return;
        }

        // Calculate emissions
        double carbonEmissions = fuelAmount * emissionFactor; // in kg CO2

        // Convert emissions to different units
        double carbonGValue = carbonEmissions * 1000; // kg to g
        double carbonLbValue = carbonEmissions * 2.20462; // kg to lb
        double carbonKgValue = carbonEmissions; // already in kg
        double carbonMtValue = carbonEmissions / 1000; // kg to metric tons

        // Use current date for estimatedAt
        String estimatedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Update UI and add list item
        updateUI(carbonGValue, carbonLbValue, carbonKgValue, carbonMtValue, estimatedDate);
        addListItem(carbonKgValue, fuelAmount, selectedFuelType);
    }

    private void updateUI(double g, double lb, double kg, double mt, String date) {
        resultCard.setVisibility(View.VISIBLE);
        carbonG.setText(String.format("Carbon Emission (g): %.2f", g));
        carbonLb.setText(String.format("Carbon Emission (lb): %.2f", lb));
        carbonKg.setText(String.format("Carbon Emission (kg): %.2f", kg));
        carbonMt.setText(String.format("Carbon Emission (MT): %.2f", mt));
        estimatedAt.setText("Estimated At: " + date);
    }

    private void addListItem(double carbonKgValue, double fuelAmount, String fuelType) {
        View listItem = LayoutInflater.from(this).inflate(R.layout.list1, listContainer, false);

        TextView co2Text = listItem.findViewById(R.id.co2Text);
        Button saveButton = listItem.findViewById(R.id.saveButton);
        Button dismissButton = listItem.findViewById(R.id.dismissButton);

        co2Text.setText("CO₂ Emission: " + carbonKgValue + " kg");

        saveButton.setOnClickListener(v -> {
            saveToFirebase(carbonKgValue, fuelAmount, fuelType);
            listContainer.removeView(listItem);
        });

        dismissButton.setOnClickListener(v -> listContainer.removeView(listItem));

        listContainer.removeAllViews();
        listContainer.addView(listItem);
    }

    private void saveToFirebase(double carbonKgValue, double fuelAmount, String fuelType) {
        String entryId = databaseReference.push().getKey();
        if (entryId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("carbon_kg", carbonKgValue);
            data.put("fuel_amount", fuelAmount);
            data.put("fuel_type", fuelType);
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
                    storeFuelEmissions(finalTotal);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showError("Error loading total emissions: " + databaseError.getMessage());
            }
        });
    }

    private void storeFuelEmissions(double totalEmissions) {
        Map<String, Object> emissionsData = new HashMap<>();
        emissionsData.put("emissions", totalEmissions);

        userRef.setValue(emissionsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseSuccess", "Fuel emissions stored successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Error storing fuel emissions", e);
                    Toast.makeText(this, "Failed to store fuel emissions", Toast.LENGTH_SHORT).show();
                });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(FuelEmission.this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}