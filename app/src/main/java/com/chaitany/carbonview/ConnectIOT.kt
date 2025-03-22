package com.chaitany.carbonview

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.chaitany.carbonview.IOTIntegration.AddDevices
import com.google.android.material.button.MaterialButton

class ConnectIOT : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_iot)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle("IOT Device Scanner")
        }

        // Set status bar color to match the Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
            // Set status bar icons to light (white) if the background is dark
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        val btnConnect = findViewById<MaterialButton>(R.id.btn_connect)
        val btnConnectVehicles = findViewById<MaterialButton>(R.id.btn_connect_vehicles)
        val progressBar = findViewById<View>(R.id.progress_circular)

        btnConnect.setOnClickListener {
            // Show progress animation
            progressBar.visibility = View.VISIBLE
            btnConnect.isEnabled = false

            // Delay for 1 second and then navigate to IoT Simulator
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnConnect.isEnabled = true
                val intent = Intent(this, AddDevices::class.java)
                startActivity(intent)
            }, 1000)
        }

        btnConnectVehicles.setOnClickListener {
            // Show progress animation
            progressBar.visibility = View.VISIBLE
            btnConnect.isEnabled = false

            // Delay for 1 second and then navigate to Vehicle Report
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnConnect.isEnabled = true
                val intent = Intent(this, VehicleReport::class.java)
                startActivity(intent)
            }, 1000)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}