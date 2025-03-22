package com.chaitany.carbonview;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class EstimationGrid extends AppCompatActivity {
    MaterialCardView electricityCard, fuelCard, flightCard, transportCard, industryCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estimation_grid);

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

        // Initialize cards
        electricityCard = findViewById(R.id.electricalcard);
        fuelCard = findViewById(R.id.fuelemission);
        flightCard = findViewById(R.id.flightemission);
        transportCard = findViewById(R.id.transportemission);
        industryCard = findViewById(R.id.industryemission);

        setupOnClickListeners();
    }

    private void setupOnClickListeners() {
        electricityCard.setOnClickListener(view -> startActivity(new Intent(this, ElectricityEmission.class)));
        fuelCard.setOnClickListener(view -> startActivity(new Intent(this, FuelEmission.class)));
        flightCard.setOnClickListener(view -> startActivity(new Intent(this, FlightEmission.class)));
        transportCard.setOnClickListener(view -> startActivity(new Intent(this, TransportEmission.class)));
        industryCard.setOnClickListener(view -> startActivity(new Intent(this, IndustryEmission.class)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}