package com.chaitany.carbonview;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyzeActivity extends AppCompatActivity {

    private TextView scope1Text, scope2Text, scope3Text, recentEntryText;
    private PieChart pieChart;
    private BarChart barChart;
    private StorageReference storageReference;
    private String userId;

    private MaterialButton generatePDFBtn;

    private LinearLayout main;
    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);

        // Initialize UI components
        scope1Text = findViewById(R.id.scope1Text);
        scope2Text = findViewById(R.id.scope2Text);
        scope3Text = findViewById(R.id.scope3Text);
        recentEntryText = findViewById(R.id.recentEntryText);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        generatePDFBtn=findViewById(R.id.generatePdfButton);
        main=findViewById(R.id.main);


        generatePDFBtn.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                generateAndOpenPDF();
            } else {
                requestStoragePermission();
            }
        });




        // Get Current User
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = currentUser.getUid();
        storageReference = FirebaseStorage.getInstance().getReference("uploads").child(userId);

        // Get File URL from Intent
        String fileUrl = getIntent().getStringExtra("fileUrl");
        if (fileUrl != null) {
            Log.d("AnalyzeActivity", "Received file URL: " + fileUrl);
            fetchAndProcessCSV(fileUrl);
        } else {
            Log.e("AnalyzeActivity", "No file URL provided in intent");
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private Bitmap captureScrollView() {
        ScrollView scrollView = findViewById(R.id.scrollView); // Ensure this ID matches your ScrollView
        View childView = scrollView.getChildAt(0); // Get the first child (full content)

        int totalHeight = childView.getHeight(); // Full height of the content
        int totalWidth = scrollView.getWidth(); // Width remains same

        Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        childView.draw(canvas); // Draw the full content onto the bitmap

        return bitmap;
    }


    private void generateAndOpenPDF() {
        Bitmap bitmap = captureScrollView();
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        String fileName = "CarbonEmissions_" + getCurrentDate() + ".pdf";
        File pdfFile;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ✅ Scoped Storage (Android 10+)
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            try {
                if (uri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        document.writeTo(outputStream);
                        outputStream.close();
                        document.close();
                        Toast.makeText(this, "PDF saved in Downloads", Toast.LENGTH_SHORT).show();
                        openPDF(uri);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
            }
        } else {
            // ✅ Public Downloads Folder (Android 9 and below)
            pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            try {
                FileOutputStream fos = new FileOutputStream(pdfFile);
                document.writeTo(fos);
                fos.close();
                document.close();
                Toast.makeText(this, "PDF saved in Downloads", Toast.LENGTH_SHORT).show();
                openPDF(Uri.fromFile(pdfFile));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }
    private void openPDF(Uri fileUri) {
        Intent openPdfIntent = new Intent(Intent.ACTION_VIEW);
        openPdfIntent.setDataAndType(fileUri, "application/pdf");
        openPdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openPdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(openPdfIntent);
            Toast.makeText(this, "PDF Opened Successfully", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_SHORT).show();
        }
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndOpenPDF();
            } else {
                generateAndOpenPDF();

                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchAndProcessCSV(String fileUrl) {
        StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);

        fileRef.getBytes(1024 * 1024) // Download up to 1MB
                .addOnSuccessListener(bytes -> {
                    String csvData = new String(bytes, StandardCharsets.UTF_8);
                    List<CarbonEmission11> emissionsList = parseCSV(csvData);

                    // Call Classification Engine to classify and calculate emissions
                    Map<String, Double> scopeEmissions = ClassificationEngine1.classifyAndCalculate(emissionsList);

                    updateUI(scopeEmissions, emissionsList);
                })
                .addOnFailureListener(e -> Log.e("AnalyzeActivity", "Error downloading CSV file", e));
    }

    private List<CarbonEmission11> parseCSV(String csvData) {
        List<CarbonEmission11> emissionsList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8))))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 5) {
                    String date = tokens[0].trim();
                    String companyName = tokens[1].trim();
                    String activityType = tokens[2].trim();
                    double consumptionAmount = Double.parseDouble(tokens[3].trim());
                    double emissionFactor = Double.parseDouble(tokens[4].trim());

                    emissionsList.add(new CarbonEmission11(activityType, companyName, consumptionAmount, date, emissionFactor));
                }
            }
        } catch (Exception e) {
            Log.e("AnalyzeActivity", "Error parsing CSV", e);
        }
        return emissionsList;
    }

    private void updateUI(Map<String, Double> scopeEmissions, List<CarbonEmission11> emissionsList) {
        runOnUiThread(() -> {
            scope1Text.setText("Scope 1: " + String.format("%.2f", scopeEmissions.get("Scope 1")) + " kg");
            scope2Text.setText("Scope 2: " + String.format("%.2f", scopeEmissions.get("Scope 2")) + " kg");
            scope3Text.setText("Scope 3: " + String.format("%.2f", scopeEmissions.get("Scope 3")) + " kg");


            if (!emissionsList.isEmpty()) {
                CarbonEmission11 lastEntry = emissionsList.get(emissionsList.size() - 1);
                recentEntryText.setText("Last Entry: " + lastEntry.getActivityType() + ", " +
                        lastEntry.getCompanyName() + ", " + lastEntry.getDate());
            }

            setupPieChart(scopeEmissions);
            setupBarChart(scopeEmissions);
        });
    }

    private void setupPieChart(Map<String, Double> scopeEmissions) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scopeEmissions.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Carbon Emissions");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void setupBarChart(Map<String, Double> scopeEmissions) {
        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : scopeEmissions.entrySet()) {
            entries.add(new BarEntry(index++, entry.getValue().floatValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Carbon Emissions");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate();
    }
}