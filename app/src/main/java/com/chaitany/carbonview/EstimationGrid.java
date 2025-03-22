package com.chaitany.carbonview;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class EstimationGrid extends AppCompatActivity {
    CardView electricityCard, fuelCard, flightCard, transportCard, industryCard;
    Button scan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estimation_grid);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        electricityCard = findViewById(R.id.electricalcard);
        fuelCard = findViewById(R.id.fuelemission);
        flightCard = findViewById(R.id.flightemission);
        transportCard = findViewById(R.id.transportemission);
        industryCard = findViewById(R.id.industryemission);


        setuplonclicklistener();

    }

    private void setuplonclicklistener() {

        electricityCard.setOnClickListener(view -> {

            startActivity(new Intent(this, ElectricityEmission.class));
        });
        fuelCard.setOnClickListener(view -> {

            startActivity(new Intent(this, FuelEmission.class));
        });

        flightCard.setOnClickListener(view -> {

            startActivity(new Intent(this, FlightEmission.class));
        });

        transportCard.setOnClickListener(view -> {

            startActivity(new Intent(this, TransportEmission.class));
        });

        industryCard.setOnClickListener(view -> {

            startActivity(new Intent(this, IndustryEmission.class));
        });







    }

}