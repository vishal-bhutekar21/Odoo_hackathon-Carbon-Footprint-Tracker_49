package com.chaitany.carbonview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TransportEmission extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private EditText inputWeight, inputDistance;
    private Spinner weightUnitSpinner, distanceUnitSpinner, transportMethodSpinner;
    private TextView carbonG, carbonLb, carbonKg, carbonMt, estimatedAt, totalEmissions;
    private TextView truckFactor, shipFactor, trainFactor, planeFactor; // New TextViews for factors
    private MaterialCardView resultCard;
    private Button btnCalculate;
    private LinearLayout listContainer;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;
    private String currentMonth;

    private String selectedWeightUnit = "g";
    private String selectedDistanceUnit = "km";
    private String selectedTransportMethod = "truck";

    // Predefined emission factors (in kg CO2 per ton-km)
    private static final double EMISSION_FACTOR_TRUCK = 0.20; // kg CO2 per ton-km
    private static final double EMISSION_FACTOR_SHIP = 0.01;  // kg CO2 per ton-km
    private static final double EMISSION_FACTOR_TRAIN = 0.05; // kg CO2 per ton-km
    private static final double EMISSION_FACTOR_PLANE = 0.50; // kg CO2 per ton-km

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_emission);



        // Get email from SharedPreferences and initialize user reference
        String email = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                .getString("email", null);
        if (email != null) {
            String safeEmail = email.replace(".", ",");
            userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(safeEmail)
                    .child("emissions_data")
                    .child("transport_emissions");
        } else {
            Log.e("TransportEmission", "Email not found in SharedPreferences");
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        initializeViews();
        setupSpinners();
        setupButtonListener();

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // Get current month in "YYYY-MMMM" format (e.g., "2025-March")
        currentMonth = new SimpleDateFormat("yyyy-MMMM", Locale.getDefault()).format(new Date());

        String safeEmail = email.replace(".", ",");
        // Initialize Firebase Database with month-specific node
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("carbonviewcalculations/"+safeEmail+"/manualaddedemissions/electricalemissions/" + currentMonth);
// Set emission factors in UI
        truckFactor.setText(String.format("Truck: %.2f kg CO₂/ton-km", EMISSION_FACTOR_TRUCK));
        shipFactor.setText(String.format("Ship: %.2f kg CO₂/ton-km", EMISSION_FACTOR_SHIP));
        trainFactor.setText(String.format("Train: %.2f kg CO₂/ton-km", EMISSION_FACTOR_TRAIN));
        planeFactor.setText(String.format("Plane: %.2f kg CO₂/ton-km", EMISSION_FACTOR_PLANE));

        // Load total emissions for the current month
        loadTotalEmissions();

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



    private void initializeViews() {
        inputWeight = findViewById(R.id.inputWeight);
        inputDistance = findViewById(R.id.inputDistance);
        weightUnitSpinner = findViewById(R.id.weightUnitSpinner);
        distanceUnitSpinner = findViewById(R.id.distanceUnitSpinner);
        transportMethodSpinner = findViewById(R.id.transportMethodSpinner);
        carbonG = findViewById(R.id.carbonG);
        carbonLb = findViewById(R.id.carbonLb);
        carbonKg = findViewById(R.id.carbonKg);
        carbonMt = findViewById(R.id.carbonMt);
        estimatedAt = findViewById(R.id.estimatedAt);
        truckFactor = findViewById(R.id.truckFactor);
        shipFactor = findViewById(R.id.shipFactor);
        trainFactor = findViewById(R.id.trainFactor);
        planeFactor = findViewById(R.id.planeFactor);
        resultCard = findViewById(R.id.resultCard);
        btnCalculate = findViewById(R.id.btnCalculate);
        listContainer = findViewById(R.id.listContainer);
        totalEmissions = findViewById(R.id.totalEmissions);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(this,
                R.array.weight_units, android.R.layout.simple_spinner_item);
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightUnitSpinner.setAdapter(weightAdapter);
        weightUnitSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this,
                R.array.distance_units, android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceUnitSpinner.setAdapter(distanceAdapter);
        distanceUnitSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> transportAdapter = ArrayAdapter.createFromResource(this,
                R.array.transport_methods, android.R.layout.simple_spinner_item);
        transportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transportMethodSpinner.setAdapter(transportAdapter);
        transportMethodSpinner.setOnItemSelectedListener(this);
    }

    private void setupButtonListener() {
        btnCalculate.setOnClickListener(v -> {
            String weightStr = inputWeight.getText().toString().trim();
            String distanceStr = inputDistance.getText().toString().trim();

            if (validateInputs(weightStr, distanceStr)) {
                try {
                    double weight = Double.parseDouble(weightStr);
                    double distance = Double.parseDouble(distanceStr);
                    if (validateUnits()) {
                        calculateShippingEmission(weight, distance);
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid number format");
                }
            }
        });
    }

    private boolean validateInputs(String weight, String distance) {
        if (weight.isEmpty() || distance.isEmpty()) {
            showError("Please fill all fields");
            return false;
        }
        return true;
    }

    private boolean validateUnits() {
        if (!Arrays.asList("g", "lb", "kg", "mt").contains(selectedWeightUnit)) {
            showError("Invalid weight unit selected");
            return false;
        }
        if (!Arrays.asList("km", "mi").contains(selectedDistanceUnit)) {
            showError("Invalid distance unit selected");
            return false;
        }
        if (!Arrays.asList("truck", "ship", "train", "plane").contains(selectedTransportMethod)) {
            showError("Invalid transport method");
            return false;
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedItem = parent.getItemAtPosition(position).toString();
        try {
            int spinnerId = parent.getId();
            if (spinnerId == R.id.weightUnitSpinner) {
                selectedWeightUnit = extractUnitFromSpinner(selectedItem);
            } else if (spinnerId == R.id.distanceUnitSpinner) {
                selectedDistanceUnit = extractUnitFromSpinner(selectedItem);
            } else if (spinnerId == R.id.transportMethodSpinner) {
                selectedTransportMethod = selectedItem.toLowerCase();
            }
        } catch (Exception e) {
            showError("Invalid selection format");
        }
    }

    private String extractUnitFromSpinner(String text) throws Exception {
        int start = text.indexOf('(') + 1;
        int end = text.indexOf(')');
        if (start <= 0 || end <= 0 || start >= end) {
            throw new Exception("Invalid unit format");
        }
        return text.substring(start, end);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Default values already set
    }

    private void calculateShippingEmission(double weightValue, double distanceValue) {
        double weightInTons = convertWeightToTons(weightValue, selectedWeightUnit);
        double distanceInKm = convertDistanceToKm(distanceValue, selectedDistanceUnit);

        double emissionFactor = getEmissionFactor(selectedTransportMethod);
        double carbonEmissions = weightInTons * distanceInKm * emissionFactor; // in kg CO2

        double carbonGValue = carbonEmissions * 1000; // kg to g
        double carbonLbValue = carbonEmissions * 2.20462; // kg to lb
        double carbonKgValue = carbonEmissions; // already in kg
        double carbonMtValue = carbonEmissions / 1000; // kg to metric tons

        String estimatedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        updateUI(carbonGValue, carbonLbValue, carbonKgValue, carbonMtValue, estimatedDate);
        addListItem(carbonKgValue, weightValue, distanceValue, selectedTransportMethod);
    }

    private double convertWeightToTons(double weight, String unit) {
        switch (unit) {
            case "kg": return weight / 1000; // kg to tons
            case "g": return weight / 1000000; // g to tons
            case "lb": return weight * 0.000453592; // lb to tons
            case "mt": return weight; // already in tons
            default: return 0;
        }
    }

    private double convertDistanceToKm(double distance, String unit) {
        switch (unit) {
            case "km": return distance; // already in km
            case "mi": return distance * 1.60934; // miles to km
            default: return 0;
        }
    }

    private double getEmissionFactor(String transportMethod) {
        switch (transportMethod) {
            case "truck": return EMISSION_FACTOR_TRUCK;
            case "ship": return EMISSION_FACTOR_SHIP;
            case "train": return EMISSION_FACTOR_TRAIN;
            case "plane": return EMISSION_FACTOR_PLANE;
            default: return 0;
        }
    }

    private void updateUI(double g, double lb, double kg, double mt, String date) {
        resultCard.setVisibility(View.VISIBLE);
        carbonG.setText(String.format("Carbon Emission (g): %.2f", g));
        carbonLb.setText(String.format("Carbon Emission (lb): %.2f", lb));
        carbonKg.setText(String.format("Carbon Emission (kg): %.2f", kg));
        carbonMt.setText(String.format("Carbon Emission arduin(MT): %.2f", mt));
        estimatedAt.setText("Estimated At: " + date);
    }

    private void addListItem(double carbonKgValue, double weight, double distance, String transportMethod) {
        View listItem = LayoutInflater.from(this).inflate(R.layout.list1, listContainer, false);

        TextView co2Text = listItem.findViewById(R.id.co2Text);
        Button saveButton = listItem.findViewById(R.id.saveButton);
        Button dismissButton = listItem.findViewById(R.id.dismissButton);

        co2Text.setText("CO₂ Emission: " + carbonKgValue + " kg");

        saveButton.setOnClickListener(v -> {
            saveToFirebase(carbonKgValue, weight, distance, transportMethod);
            listContainer.removeView(listItem);
        });

        dismissButton.setOnClickListener(v -> listContainer.removeView(listItem));

        listContainer.removeAllViews();
        listContainer.addView(listItem);
    }

    private void saveToFirebase(double carbonKgValue, double weight, double distance, String transportMethod) {
        String entryId = databaseReference.push().getKey();
        if (entryId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("carbon_kg", carbonKgValue);
            data.put("weight", weight);
            data.put("weight_unit", selectedWeightUnit);
            data.put("distance", distance);
            data.put("distance_unit", selectedDistanceUnit);
            data.put("transport_method", transportMethod);
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
                    storeTransportEmissions(finalTotal);
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showError("Error loading total emissions: " + databaseError.getMessage());
            }
        });
    }

    private void storeTransportEmissions(double totalEmissions) {
        Map<String, Object> emissionsData = new HashMap<>();
        emissionsData.put("emissions", totalEmissions);

        userRef.setValue(emissionsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseSuccess", "Transport emissions stored successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Error storing transport emissions", e);
                    Toast.makeText(this, "Failed to store transport emissions", Toast.LENGTH_SHORT).show();
                });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(TransportEmission.this, message, Toast.LENGTH_LONG).show());
    }
}