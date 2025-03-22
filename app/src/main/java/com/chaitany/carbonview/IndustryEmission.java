package com.chaitany.carbonview;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IndustryEmission extends AppCompatActivity {

    private EditText inputProductionAmount;
    private Spinner industryTypeSpinner;
    private TextView carbonG, carbonLb, carbonKg, carbonMt, estimatedAt, totalEmissions;
    private TextView manufacturingFactor, miningFactor, constructionFactor; // New TextViews for factors
    private MaterialCardView resultCard;
    private Button btnCalculate;
    private LinearLayout listContainer;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;
    private String currentMonth;

    // Predefined emission factors (in kg CO2 per unit of production)
    private static final double EMISSION_FACTOR_MANUFACTURING = 1.5; // kg CO2 per unit
    private static final double EMISSION_FACTOR_MINING = 2.0; // kg CO2 per unit
    private static final double EMISSION_FACTOR_CONSTRUCTION = 1.8; // kg CO2 per unit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_industry_emission);

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
                    .child("industry_emissions");
        } else {
            Log.e("IndustryEmission", "Email not found in SharedPreferences");
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize UI components
        inputProductionAmount = findViewById(R.id.inputProductionAmount);
        industryTypeSpinner = findViewById(R.id.industryTypeSpinner);
        carbonG = findViewById(R.id.carbonG);
        carbonLb = findViewById(R.id.carbonLb);
        carbonKg = findViewById(R.id.carbonKg);
        carbonMt = findViewById(R.id.carbonMt);
        estimatedAt = findViewById(R.id.estimatedAt);
        manufacturingFactor = findViewById(R.id.manufacturingFactor);
        miningFactor = findViewById(R.id.miningFactor);
        constructionFactor = findViewById(R.id.constructionFactor);
        resultCard = findViewById(R.id.resultCard);
        btnCalculate = findViewById(R.id.btnCalculate);
        listContainer = findViewById(R.id.listContainer);
        totalEmissions = findViewById(R.id.totalEmissions);

        // Get current month in "YYYY-MMMM" format (e.g., "2025-March")
        currentMonth = new SimpleDateFormat("yyyy-MMMM", Locale.getDefault()).format(new Date());

        // Initialize Firebase Realtime Database with month-specific node
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("carbonviewcalculations/manualaddedemissions/industryemissions/" + currentMonth);

        // Populate the industry type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.industry_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        industryTypeSpinner.setAdapter(adapter);

        // Set emission factors in UI
        manufacturingFactor.setText(String.format("Manufacturing: %.2f kg CO₂/unit", EMISSION_FACTOR_MANUFACTURING));
        miningFactor.setText(String.format("Mining: %.2f kg CO₂/unit", EMISSION_FACTOR_MINING));
        constructionFactor.setText(String.format("Construction: %.2f kg CO₂/unit", EMISSION_FACTOR_CONSTRUCTION));

        // Load total emissions for the current month
        loadTotalEmissions();

        // Set up the button click listener
        btnCalculate.setOnClickListener(v -> {
            String productionAmountStr = inputProductionAmount.getText().toString();
            if (!productionAmountStr.isEmpty()) {
                try {
                    double productionAmount = Double.parseDouble(productionAmountStr);
                    calculateIndustryEmission(productionAmount);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid input. Enter a valid number.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter production amount", Toast.LENGTH_SHORT).show();
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


    private void calculateIndustryEmission(double productionAmount) {
        String selectedIndustryType = industryTypeSpinner.getSelectedItem().toString();
        double emissionFactor;

        switch (selectedIndustryType) {
            case "Manufacturing":
                emissionFactor = EMISSION_FACTOR_MANUFACTURING;
                break;
            case "Mining":
                emissionFactor = EMISSION_FACTOR_MINING;
                break;
            case "Construction":
                emissionFactor = EMISSION_FACTOR_CONSTRUCTION;
                break;
            default:
                showError("Invalid industry type selected");
                return;
        }

        double carbonEmissions = productionAmount * emissionFactor; // in kg CO2

        double carbonGValue = carbonEmissions * 1000; // kg to g
        double carbonLbValue = carbonEmissions * 2.20462; // kg to lb
        double carbonKgValue = carbonEmissions; // already in kg
        double carbonMtValue = carbonEmissions / 1000; // kg to metric tons

        String estimatedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        updateUI(carbonGValue, carbonLbValue, carbonKgValue, carbonMtValue, estimatedDate);
        addListItem(carbonKgValue, productionAmount, selectedIndustryType);
    }

    private void updateUI(double g, double lb, double kg, double mt, String date) {
        resultCard.setVisibility(View.VISIBLE);
        carbonG.setText(String.format("Carbon Emission (g): %.2f", g));
        carbonLb.setText(String.format("Carbon Emission (lb): %.2f", lb));
        carbonKg.setText(String.format("Carbon Emission (kg): %.2f", kg));
        carbonMt.setText(String.format("Carbon Emission (MT): %.2f", mt));
        estimatedAt.setText("Estimated At: " + date);
    }

    private void addListItem(double carbonKgValue, double productionAmount, String industryType) {
        View listItem = LayoutInflater.from(this).inflate(R.layout.list1, listContainer, false);

        TextView co2Text = listItem.findViewById(R.id.co2Text);
        Button saveButton = listItem.findViewById(R.id.saveButton);
        Button dismissButton = listItem.findViewById(R.id.dismissButton);

        co2Text.setText("CO₂ Emission: " + carbonKgValue + " kg");

        saveButton.setOnClickListener(v -> {
            saveToFirebase(carbonKgValue, productionAmount, industryType);
            listContainer.removeView(listItem);
        });

        dismissButton.setOnClickListener(v -> listContainer.removeView(listItem));

        listContainer.removeAllViews();
        listContainer.addView(listItem);
    }

    private void saveToFirebase(double carbonKgValue, double productionAmount, String industryType) {
        String entryId = databaseReference.push().getKey();
        if (entryId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("carbon_kg", carbonKgValue);
            data.put("production_amount", productionAmount);
            data.put("industry_type", industryType);
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
                    storeIndustryEmissions(finalTotal);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showError("Error loading total emissions: " + databaseError.getMessage());
            }
        });
    }

    private void storeIndustryEmissions(double totalEmissions) {
        Map<String, Object> emissionsData = new HashMap<>();
        emissionsData.put("emissions", totalEmissions);

        userRef.setValue(emissionsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseSuccess", "Industry emissions stored successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Error storing industry emissions", e);
                    Toast.makeText(this, "Failed to store industry emissions", Toast.LENGTH_SHORT).show();
                });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(IndustryEmission.this, message, Toast.LENGTH_LONG).show());
    }
}