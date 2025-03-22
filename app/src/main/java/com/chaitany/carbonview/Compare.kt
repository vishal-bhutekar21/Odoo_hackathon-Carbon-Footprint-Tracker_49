package com.chaitany.carbonview

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class Compare : AppCompatActivity() {

    private lateinit var Electricitycard:MaterialCardView
    private lateinit var FuelCard:MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_compare)

        Electricitycard=findViewById(R.id.Electricity_Bill_Card)
        FuelCard=findViewById(R.id.Fuel_Scan_Card)

        Electricitycard.setOnClickListener{
            var intent=Intent(this,ElectricityBillScan::class.java)
            startActivity(intent)
        }

        FuelCard.setOnClickListener{
            var intent=Intent(this,FuelBillScan::class.java)
            startActivity(intent)
        }
    }
}