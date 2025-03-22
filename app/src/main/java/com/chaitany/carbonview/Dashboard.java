package com.chaitany.carbonview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.chaitany.carbonview.SocialPlatform.SocialPlatformActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class Dashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ImageView menuIcon;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth auth;

    private PieChart pieChart;
    private DatabaseReference databaseReference;

    TextView txtTodayEmission, txtMonthEmission, txtScope1Value, txtScope2Value, txtScope3Value;
    LinearLayout adddata, uploadreport, viewreport, aiinsights, Connectiot, compare;

    // TextViews for the emission cards
    TextView fuelEmissions, electricEmissions, flightEmissions, transportEmissions, industryEmissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize views
        initializeViews();
        setupNavigationDrawer();
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        setUponClickListener();

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load user information
        loadUserInfo();

        // Fetch emissions data for the cards
        fetchEmissionsData();
    }

    private void setUponClickListener() {
        adddata.setOnClickListener(view -> startActivity(new Intent(Dashboard.this, AddData.class)));
        uploadreport.setOnClickListener(view -> startActivity(new Intent(Dashboard.this, UploadReport.class)));
        viewreport.setOnClickListener(view -> startActivity(new Intent(Dashboard.this, CompanyFinalEmissionData.class)));
        aiinsights.setOnClickListener(view -> startActivity(new Intent(Dashboard.this, EstimationGrid.class)));
        Connectiot.setOnClickListener(view -> startActivity(new Intent(Dashboard.this, ConnectIOT.class)));
        compare.setOnClickListener(view -> startActivity(new Intent(Dashboard.this, SocialPlatformActivity.class)));
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        menuIcon = findViewById(R.id.menuIcon);
        adddata = findViewById(R.id.add_data);
        uploadreport = findViewById(R.id.uploadreport);
        viewreport = findViewById(R.id.viewreport);
        aiinsights = findViewById(R.id.aiinsights);
        Connectiot = findViewById(R.id.connectiot);
        compare = findViewById(R.id.compare);
        pieChart = findViewById(R.id.pieChart);
        txtTodayEmission = findViewById(R.id.txtTodayEmission);
        txtMonthEmission = findViewById(R.id.txtMonthEmission);
        txtScope1Value = findViewById(R.id.txtScope1Value);
        txtScope2Value = findViewById(R.id.txtScope2Value);
        txtScope3Value = findViewById(R.id.txtScope3Value);

        // Initialize TextViews for the emission cards
        fuelEmissions = findViewById(R.id.fuel_emissions);
        electricEmissions = findViewById(R.id.electric_emissions);
        flightEmissions = findViewById(R.id.flight_emissions);
        transportEmissions = findViewById(R.id.transport_emissions);
        industryEmissions = findViewById(R.id.industry_emissions);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("uploads");

            // Real-time listener for changes in 'uploads'
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int fileCount = (int) snapshot.getChildrenCount();
                    Log.d("FileCount", "Real-time file count: " + fileCount);
                    Toast.makeText(getApplicationContext(), "Real-time file count: " + fileCount, Toast.LENGTH_SHORT).show();

                    double scope1 = 179463.01;
                    double scope2 = 30881.39;
                    double scope3 = 511836.43;

                    double newScope1 = scope1 * fileCount;
                    double newScope2 = scope2 * fileCount;
                    double newScope3 = scope3 * fileCount;

                    double totalEmissions = newScope1 + newScope2 + newScope3;
                    txtScope1Value.setText(String.format("%.2f Kg", newScope1));
                    txtScope2Value.setText(String.format("%.2f Kg", newScope2));
                    txtScope3Value.setText(String.format("%.2f Kg", newScope3));
                    txtTodayEmission.setText(String.format("%.2f Kg CO2", totalEmissions));
                    txtMonthEmission.setText(String.format("%.2f Kg CO2", totalEmissions));

                    updatePieChart(newScope1, newScope2, newScope3);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseError", "Database error: ", error.toException());
                }
            });

            // Set click listener for menu icon
            menuIcon.setOnClickListener(v -> toggleNavigationDrawer());
        }
    }

    private void fetchEmissionsData() {
        // Create and show a Material Design loading dialog
        com.google.android.material.dialog.MaterialAlertDialogBuilder dialogBuilder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Loading");
        dialogBuilder.setMessage("Fetching live total carbon emissions...");
        dialogBuilder.setCancelable(false); // Prevent dismissing the dialog by tapping outside

        // Create a custom view for the dialog with a progress indicator
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setGravity(Gravity.CENTER);
        dialogLayout.setPadding(32, 32, 32, 32);

        com.google.android.material.progressindicator.CircularProgressIndicator progressIndicator =
                new com.google.android.material.progressindicator.CircularProgressIndicator(this);
        progressIndicator.setIndeterminate(true);
        progressIndicator.setIndicatorColor(Color.parseColor("#4CAF50")); // Green color for the progress
        dialogLayout.addView(progressIndicator);

        dialogBuilder.setView(dialogLayout);
        AlertDialog loadingDialog = dialogBuilder.create();
        loadingDialog.show();

        // Check for internet connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();

        // Get the email from SharedPreferences
        String email = getSharedPreferences("UserLogin", Context.MODE_PRIVATE)
                .getString("email", null);
        if (email == null) {
            Log.e("EmissionsData", "Email not found in SharedPreferences");
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            return;
        }

        // If no internet connection, show cached values and dismiss the dialog
        if (!isConnected) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();

            // Load cached values from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("EmissionData", Context.MODE_PRIVATE);
            float fuelEmissionsValue = prefs.getFloat("fuel_emissions", 0f);
            float electricEmissionsValue = prefs.getFloat("electric_emissions", 0f);
            float flightEmissionsValue = prefs.getFloat("flight_emissions", 0f);
            float transportEmissionsValue = prefs.getFloat("transport_emissions", 0f);
            float industryEmissionsValue = prefs.getFloat("industry_emissions", 0f);

            // Update the TextViews with cached or default values
            fuelEmissions.setText(String.format("%.2f kg CO₂e", fuelEmissionsValue));
            electricEmissions.setText(String.format("%.2f kg CO₂e", electricEmissionsValue));
            flightEmissions.setText(String.format("%.2f kg CO₂e", flightEmissionsValue));
            transportEmissions.setText(String.format("%.2f kg CO₂e", transportEmissionsValue));
            industryEmissions.setText(String.format("%.2f kg CO₂e", industryEmissionsValue));
            return;
        }

        // Replace dots with commas to create a safe key for Firebase
        String safeEmail = email.replace(".", ",");
        DatabaseReference emissionsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(safeEmail)
                .child("emissions_data");

        // Use ValueEventListener for real-time updates
        emissionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float fuelEmissionsValue = 0f;
                float electricEmissionsValue = 0f;
                float flightEmissionsValue = 0f;
                float transportEmissionsValue = 0f;
                float industryEmissionsValue = 0f;

                // Fetch each category's emissions
                if (snapshot.child("fuel_emissions").exists()) {
                    fuelEmissionsValue = snapshot.child("fuel_emissions").child("emissions").getValue(Float.class);
                }
                if (snapshot.child("electricity_emissions").exists()) {
                    electricEmissionsValue = snapshot.child("electricity_emissions").child("emissions").getValue(Float.class);
                }
                if (snapshot.child("flight_emissions").exists()) {
                    flightEmissionsValue = snapshot.child("flight_emissions").child("emissions").getValue(Float.class);
                }
                if (snapshot.child("transport_emissions").exists()) {
                    transportEmissionsValue = snapshot.child("transport_emissions").child("emissions").getValue(Float.class);
                }
                if (snapshot.child("industry_emissions").exists()) {
                    industryEmissionsValue = snapshot.child("industry_emissions").child("emissions").getValue(Float.class);
                }

                // Update the TextViews in the cards
                fuelEmissions.setText(String.format("%.2f kg CO₂e", fuelEmissionsValue));
                electricEmissions.setText(String.format("%.2f kg CO₂e", electricEmissionsValue));
                flightEmissions.setText(String.format("%.2f kg CO₂e", flightEmissionsValue));
                transportEmissions.setText(String.format("%.2f kg CO₂e", transportEmissionsValue));
                industryEmissions.setText(String.format("%.2f kg CO₂e", industryEmissionsValue));

                // Cache the values in SharedPreferences
                SharedPreferences prefs = getSharedPreferences("EmissionData", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("fuel_emissions", fuelEmissionsValue);
                editor.putFloat("electric_emissions", electricEmissionsValue);
                editor.putFloat("flight_emissions", flightEmissionsValue);
                editor.putFloat("transport_emissions", transportEmissionsValue);
                editor.putFloat("industry_emissions", industryEmissionsValue);
                editor.apply();

                // Dismiss the loading dialog after the first data fetch
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to fetch emissions data: ", error.toException());
                Toast.makeText(Dashboard.this, "Failed to fetch emissions data", Toast.LENGTH_SHORT).show();
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }

                // Load cached values from SharedPreferences in case of error
                SharedPreferences prefs = getSharedPreferences("EmissionData", Context.MODE_PRIVATE);
                float fuelEmissionsValue = prefs.getFloat("fuel_emissions", 0f);
                float electricEmissionsValue = prefs.getFloat("electric_emissions", 0f);
                float flightEmissionsValue = prefs.getFloat("flight_emissions", 0f);
                float transportEmissionsValue = prefs.getFloat("transport_emissions", 0f);
                float industryEmissionsValue = prefs.getFloat("industry_emissions", 0f);

                // Update the TextViews with cached or default values
                fuelEmissions.setText(String.format("%.2f kg CO₂e", fuelEmissionsValue));
                electricEmissions.setText(String.format("%.2f kg CO₂e", electricEmissionsValue));
                flightEmissions.setText(String.format("%.2f kg CO₂e", flightEmissionsValue));
                transportEmissions.setText(String.format("%.2f kg CO₂e", transportEmissionsValue));
                industryEmissions.setText(String.format("%.2f kg CO₂e", industryEmissionsValue));
            }
        });
    }

    private void updatePieChart(double scope1, double scope2, double scope3) {
        List<PieEntry> entries = Arrays.asList(
                new PieEntry((float) scope1, "Scope 1"),
                new PieEntry((float) scope2, "Scope 2"),
                new PieEntry((float) scope3, "Scope 3")
        );

        PieDataSet dataSet = new PieDataSet(entries, "Emissions");
        dataSet.setColors(Arrays.asList(Color.parseColor("#FFA500"), Color.BLUE, Color.GREEN));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setText("Emission Scope Distribution");
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(false);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void toggleNavigationDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userName = currentUser.getDisplayName();
            String userEmail = currentUser.getEmail();

            NavigationView navigationView = findViewById(R.id.navigationView);
            View headerView = navigationView.getHeaderView(0);
            TextView usernameTextView = headerView.findViewById(R.id.username);
            TextView emailTextView = headerView.findViewById(R.id.mobile);

            usernameTextView.setText(userName != null ? userName : "User Name");
            emailTextView.setText(userEmail != null ? userEmail : "Email Address");
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_logout) {
            handleLogout();
        } else {
            handleNavigationItem(itemId);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleLogout() {
        auth.signOut();
        sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLogged", false);
        editor.apply();
        startActivity(new Intent(Dashboard.this, Login.class));
        finish();
    }

    private void handleNavigationItem(int itemId) {
        Intent intent = null;
        if (itemId == R.id.nav_profile) {
            intent = new Intent(this, ProfileActivity.class);
        } else if (itemId == R.id.nav_share) {
            shareApp();
            return;
        } else if (itemId == R.id.nav_feedback) {
            intent = new Intent(this, FeedbackActivity.class);
        } else if (itemId == R.id.nav_about) {
            intent = new Intent(this, AboutActivity.class);
        }
        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Feature not implemented", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApp() {
        String appLink = "https://play.google.com/store/apps/details?id=com.chaitany.agewell";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, appLink);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}