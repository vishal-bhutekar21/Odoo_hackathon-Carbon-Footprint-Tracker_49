package com.chaitany.carbonview

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.chaitany.carbonview.IOTIntegration.AddDevices
import com.google.android.material.button.MaterialButton

class ConnectIOT : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_iot)

        // Setup Toolbar


        val btnConnect = findViewById<MaterialButton>(R.id.btn_connect)

        val btnConnect_Vehicles = findViewById<MaterialButton>(R.id.btn_connect_vehicles)
        val progressBar = findViewById<View>(R.id.progress_circular)

        btnConnect.setOnClickListener {
            // Show progress animation
            progressBar.visibility = View.VISIBLE
            btnConnect.isEnabled = false

            // Delay for 4 seconds and then navigate to IoT Simulator
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnConnect.isEnabled = true
                val intent = Intent(this, AddDevices::class.java)
                startActivity(intent)
            }, 1000)
        }
        btnConnect_Vehicles.setOnClickListener {
            // Show progress animation
            progressBar.visibility = View.VISIBLE
            btnConnect.isEnabled = false

            // Delay for 4 seconds and then navigate to IoT Simulator
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnConnect.isEnabled = true
                val intent = Intent(this, VehicleReport::class.java)
                startActivity(intent)
            }, 1000)
        }
    }
}
