package com.chaitany.carbonview.IOTIntegration

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.IOTIntegration.Adapter.DeviceAdapter
import com.chaitany.carbonview.IOTIntegration.Model.Device
import com.chaitany.carbonview.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import java.io.BufferedReader
import java.io.InputStreamReader

class AddDevices : AppCompatActivity() {

    private lateinit var searchEditText: TextInputEditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private lateinit var button: Button
    private lateinit var progressDialog: ProgressDialog
    private var deviceList: MutableList<Device> = mutableListOf()
    private val userId = "exampleUserId" // Replace with actual user ID (FirebaseAuth UID)
    private val TAG = "AddDevices"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_devices)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading devices...")
            setCancelable(false)
        }

        searchEditText = findViewById(R.id.searchView)
        recyclerView = findViewById(R.id.recyclerView)
        button = findViewById(R.id.btn_manage)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DeviceAdapter(this, deviceList, userId)
        recyclerView.adapter = adapter
        Log.d(TAG, "RecyclerView initialized with adapter")

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        button.setOnClickListener {
            val intent = Intent(this, ManageDevices::class.java)
            startActivity(intent)
        }

        fetchCSVData()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { filterDevices(it.toString()) }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchCSVData() {
        Log.d(TAG, "Starting to fetch CSV data")
        progressDialog.show() // Show loading dialog

        val storageRef = FirebaseStorage.getInstance().reference.child("carbon_emissions_150_devices.csv")
        storageRef.getBytes(Long.MAX_VALUE)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "Successfully downloaded CSV file, size: ${bytes.size} bytes")
                val inputStream = bytes.inputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.readLine() // Skip header

                deviceList.clear()
                var line: String?
                var lineCount = 0
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    val data = line!!.split(",")
                    Log.d(TAG, "Processing line $lineCount: $line")
                    if (data.size >= 6) {
                        try {
                            val device = Device(
                                deviceName = data[0].trim(),
                                modelName = data[1].trim(),
                                powerRating = data[2].trim().toDouble(),
                                energySource = data[4].trim(),
                                co2EmissionFactor = data[5].trim().toDouble()
                            )
                            deviceList.add(device)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing line $lineCount: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "Skipping line $lineCount: insufficient columns (${data.size})")
                    }
                }
                reader.close()
                Log.d(TAG, "Finished parsing CSV. Total devices loaded: ${deviceList.size}")
                adapter.updateList(deviceList)
                recyclerView.adapter?.notifyDataSetChanged()

                if (deviceList.isEmpty()) {
                    Log.w(TAG, "Device list is empty after parsing")
                    Toast.makeText(this, "No devices loaded", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "First device: ${deviceList[0].deviceName}")
                }
                progressDialog.dismiss() // Hide loading dialog
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to load CSV: ${exception.message}")
                Toast.makeText(this, "Failed to load devices: ${exception.message}", Toast.LENGTH_LONG).show()
                progressDialog.dismiss() // Hide loading dialog on failure
            }
    }

    private fun filterDevices(query: String) {
        val filteredList = deviceList.filter { it.deviceName.contains(query, ignoreCase = true) }
        adapter.updateList(filteredList)
        Log.d(TAG, "Filtered devices. New size: ${filteredList.size}")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}