package com.chaitany.carbonview

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IOTReport : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var scope1Text: TextView
    private lateinit var scope2Text: TextView
    private lateinit var scope3Text: TextView
    private lateinit var carbonFootprint: TextView
    private lateinit var carbonIncrease: TextView
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var generatePdfButton: MaterialButton
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iotreport)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("emissions_data")

        // Initialize UI elements
        scope1Text = findViewById(R.id.scope1Text)
        scope2Text = findViewById(R.id.scope2Text)
        scope3Text = findViewById(R.id.scope3Text)
        carbonFootprint = findViewById(R.id.carbonFootprint)
        carbonIncrease = findViewById(R.id.carbonIncrease)
        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)
        generatePdfButton=findViewById(R.id.generatePdfButton)
        scrollView=findViewById(R.id.scrollView)

        generatePdfButton.setOnClickListener {
            if (checkStoragePermission()) {
                generatePdf()
            } else {
                requestStoragePermission()
            }
        }
        val reportId: TextView = findViewById(R.id.reportId)
        val generationDate: TextView = findViewById(R.id.generationDate)
        val companyName: TextView = findViewById(R.id.companyName)
        val mobile: TextView = findViewById(R.id.mobile)
        val email: TextView = findViewById(R.id.email)
        val location: TextView = findViewById(R.id.location)

        // Fetch User Data from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)

        val storedCompanyName = sharedPreferences.getString("name", "AgriVision Tech Pvt Ltd")
        val storedMobile = sharedPreferences.getString("mobile", "+91 9322067937")
        val storedEmail = sharedPreferences.getString("email", "info@agrivision.com")
        val storedLocation = sharedPreferences.getString("location", "Aurangabad, Maharashtra")

        // Set Values to TextViews
        reportId.text = "Report ID: ECO-2025-0012"  // Static or can be dynamic if needed
        generationDate.text = "Generated: ${getCurrentDate()}"  // Get Current Date
        companyName.text = "Company: $storedCompanyName"
        mobile.text = "Mobile: $storedMobile"
        email.text = "Email: $storedEmail"
        location.text = "Location: $storedLocation"
        // Fetch data from Firebase

        loadEmissionData()
    }

    // ✅ 1️⃣ Check Storage Permission
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need for WRITE_EXTERNAL_STORAGE permission on Android 10+
            true
        } else {
            val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permission == PackageManager.PERMISSION_GRANTED
        }
    }

    // ✅ 2️⃣ Request Permission for Android 12 and below
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
        }
    }

    // ✅ 3️⃣ Convert ScrollView to Bitmap
    private fun captureScrollView(): Bitmap {
        val totalHeight = scrollView.getChildAt(0).height
        val totalWidth = scrollView.width

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        scrollView.draw(canvas)

        return bitmap
    }

    // ✅ 4️⃣ Generate and Save PDF
    private fun generatePdf() {
        val bitmap = captureScrollView()
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        val fileName = "IOT_Report_${getCurrentDate()}.pdf"
        var fileUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ✅ Android 10+ (Scoped Storage) - Save via MediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
                fileUri = it // Store the URI to open later
            }
        } else {
            // ✅ Android 9 and below - Save in the Public Downloads Folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            try {
                val fos = FileOutputStream(file)
                document.writeTo(fos)
                fos.close()
                fileUri = Uri.fromFile(file) // Get file URI
            } catch (e: Exception) {
                Log.e("PDF_ERROR", "Error saving PDF: ${e.message}")
                showMessage("Failed to save PDF")
            }
        }

        document.close()
        showMessage("PDF Saved Successfully")

        // ✅ Open PDF after saving
        fileUri?.let { openPDF(it) }
    }


    private fun openPDF(fileUri: Uri) {
        val openPdfIntent = Intent(Intent.ACTION_VIEW)
        openPdfIntent.setDataAndType(fileUri, "application/pdf")
        openPdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        openPdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(openPdfIntent)
            Toast.makeText(this, "PDF Opened Successfully", Toast.LENGTH_SHORT).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_SHORT).show()
        }
    }



    // ✅ 5️⃣ Get Current Date
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    // ✅ 6️⃣ Show Message
    private fun showMessage(message: String) {
        runOnUiThread {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // ✅ 7️⃣ Handle Permission Result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            generatePdf()
        } else {
            showMessage("Permission Denied! Cannot generate PDF.")
        }
    }


    private fun loadEmissionData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val emissionList = mutableListOf<CarbonEmission>()

                // Iterate through all emission records
                for (dataSnapshot in snapshot.children) {
                    val emission = dataSnapshot.getValue(CarbonEmission::class.java)
                    if (emission != null) {
                        emissionList.add(emission)
                    }
                }

                // Classify and process emissions
                val classifiedEmissions = ClassificationEngine.classifyAndCalculate(emissionList)
                updateUI(emissionList, classifiedEmissions)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to read data: ${error.message}")
            }
        })
    }

    private fun updateUI(emissionList: MutableList<CarbonEmission>, classifiedEmissions: Map<String, Double>) {
        var totalEmission = 0.0
        val monthlyEmissions = mutableMapOf<String, Double>()

        val scope1 = classifiedEmissions["Scope 1"] ?: 0.0
        val scope2 = classifiedEmissions["Scope 2"] ?: 0.0
        val scope3 = classifiedEmissions["Scope 3"] ?: 0.0
        totalEmission = scope1 + scope2 + scope3

        if (emissionList.isNotEmpty()) {
            for (emission in emissionList) {
                val month = if (emission.date.contains("/")) {
                    emission.date.split("/")[1]
                } else {
                    emission.date.split("-")[1]
                }
                monthlyEmissions[month] = (monthlyEmissions[month] ?: 0.0) + emission.emissionFactor
            }

            // Update UI elements
            scope1Text.text = "Scope 1: %.2f kg".format(scope1)
            scope2Text.text = "Scope 2: %.2f kg".format(scope2)
            scope3Text.text = "Scope 3: %.2f kg".format(scope3)
            carbonFootprint.text = "%.2f kg CO₂e".format(totalEmission)

            val previousYearEmission = totalEmission * 0.88  // Assume 12% increase
            val percentageIncrease = ((totalEmission - previousYearEmission) / previousYearEmission) * 100
            carbonIncrease.text = "+%.2f%% vs Previous Year".format(percentageIncrease)

            // Update Charts
            setupPieChart(scope1, scope2, scope3)
            setupLineChart(monthlyEmissions)
        } else {
            Log.e("UpdateUI", "No data available for classification")
        }
    }



    private fun setupPieChart(scope1: Double, scope2: Double, scope3: Double) {
        val entries = listOf(
            PieEntry(scope1.toFloat(), "Scope 1"),
            PieEntry(scope2.toFloat(), "Scope 2"),
            PieEntry(scope3.toFloat(), "Scope 3")
        )

        val dataSet = PieDataSet(entries, "Emission Breakdown").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 14f
        }

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.invalidate() // Refresh
    }

    private fun setupLineChart(monthlyEmissions: Map<String, Double>) {
        val entries = monthlyEmissions.entries.sortedBy { it.key.toInt() }
            .map { Entry(it.key.toFloat(), it.value.toFloat()) }

        val dataSet = LineDataSet(entries, "Monthly CO₂ Emissions").apply {
            color = ColorTemplate.COLORFUL_COLORS[0]
            valueTextSize = 12f
            setDrawValues(true)
            setDrawCircles(true)
        }

        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.description.isEnabled = false
        lineChart.invalidate() // Refresh
    }
}
