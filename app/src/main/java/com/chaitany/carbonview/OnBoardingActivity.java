package com.chaitany.carbonview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.Button;

public class OnBoardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingAdapter onboardingAdapter;
    private Button btnNext, btnSkip;
    private int currentPage = 0;
    private SharedPreferences sharedPreferences;

    private final int[] images = {
            R.drawable.onboarding2,
            R.drawable.onboarding3,
            R.drawable.onboarding4
    };

    private final String[] titles = {
            "Track Carbon Emissions \n" +
                    "in Real-Time",
            "Get Actionable Insights\n" +
                    "& Reports",
            "Reduce Emissions with\n" +
                    "AI-Powered Suggestions"
    };

    private final String[] descriptions = {
            "CarbonView categorizes emissions\n" +
                    " into Scope 1, 2, and 3 using data\n" +
                    "from energy logs, IoT sensors, \n" +
                    "and manual inputs.",
            "Gain deeper insights into your\n" +
                    "carbon footprint with interactive\n" +
                    "dashboards, custom reports, and\n" +
                    "sustainability tracking.",
            "Leverage AI-driven recommendations\n" +
                    "to reduce emissions and achieve\n" +
                    "sustainability goals."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences("UserLogin", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLogged", false);
        boolean isOnboardingCompleted = sharedPreferences.getBoolean("isOnboardingCompleted", false);

        // If user is logged in or onboarding is completed, skip onboarding and go to the main screen (Dashboard)
        if (isLoggedIn || isOnboardingCompleted) {
            startActivity(new Intent(this, Dashboard.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_on_boarding);

        // Change Status Bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.teal_50)); // Change to your desired color
        }

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        // Initialize adapter
        onboardingAdapter = new OnboardingAdapter(this, images, titles, descriptions);
        viewPager.setAdapter(onboardingAdapter);

        btnNext.setOnClickListener(v -> {
            if (currentPage < images.length - 1) {
                currentPage++;
                viewPager.setCurrentItem(currentPage);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    // Change navigation bar color
    private void finishOnboarding() {
        // Set onboarding completed flag in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isOnboardingCompleted", true);
        editor.apply();

        startActivity(new Intent(this, GetStartedActivity.class));
        finish();
    }
}