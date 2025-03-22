package com.chaitany.carbonview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseUser ;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.List;

public class Dashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ImageView menuIcon;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth auth; // Firebase Auth instance

    private PieChart pieChart;

    private DatabaseReference databaseReference;
    private StorageReference storageReference;



    TextView txtTodayEmission, txtMonthEmission, txtScope1Value, txtScope2Value, txtScope3Value;
    LinearLayout adddata, uploadreport, viewreport, aiinsights, Connectiot, compare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize views
        initializeViews();
        setupNavigationDrawer();
        auth = FirebaseAuth.getInstance();
        FirebaseUser  currentUser  = auth.getCurrentUser ();

        setUponClickListener();

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load user information
        loadUserInfo();
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


        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("uploads");

            // Real-time listener for changes in 'uploads'
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int fileCount = (int) snapshot.getChildrenCount();  // Real-time file count
                    Log.d("FileCount", "Real-time file count: " + fileCount);
                    Toast.makeText(getApplicationContext(), "Real-time file count: " + fileCount, Toast.LENGTH_SHORT).show();


                    double scope1 =179463.01;
                    double scope2 = 30881.39;
                    double scope3 =511836.43;

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

    private void updatePieChart(double scope1, double scope2, double scope3) {
        List<PieEntry> entries = Arrays.asList(
                new PieEntry((float) scope1, "Scope 1"),
                new PieEntry((float) scope2, "Scope 2"),
                new PieEntry((float) scope3, "Scope 3")
        );

        PieDataSet dataSet = new PieDataSet(entries, "Emissions");
        // Use the provided hex color combination
        dataSet.setColors(Arrays.asList(   Color.parseColor("#FFA500"), Color.BLUE, Color.GREEN));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setText("Emission Scope Distribution");
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setUsePercentValues(true);

        // Display the pie chart as a full circle (no center hole)
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
        FirebaseUser  currentUser  = auth.getCurrentUser ();
        if (currentUser  != null) {
            String userName = currentUser .getDisplayName(); // Get user name
            String userEmail = currentUser .getEmail(); // Get user email

            // Update the navigation header with user info
            NavigationView navigationView = findViewById(R.id.navigationView);
            View headerView = navigationView.getHeaderView(0); // Get the header view
            TextView usernameTextView = headerView.findViewById(R.id.username);
            TextView emailTextView = headerView.findViewById(R.id.mobile); // Assuming mobile is used for email in the header

            usernameTextView.setText(userName != null ? userName : "User  Name");
            emailTextView.setText(userEmail != null ? userEmail : "Email Address");
        } else {
            Toast.makeText(this, "User  not logged in", Toast.LENGTH_SHORT).show();
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
        // Sign out from Firebase
        auth.signOut();

        // Clear SharedPreferences
        sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLogged", false);
        editor.apply();

        // Navigate to Login Activity
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