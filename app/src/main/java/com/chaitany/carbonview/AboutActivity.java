package com.chaitany.carbonview;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class AboutActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private Button btn;
    private SharedPreferences sharedPreferences;
    private ActionBarDrawerToggle toggle;
    private View mainContent; // Reference to the main content view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        btn = findViewById(R.id.ourfeedbacks);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Reference to the main content view (the LinearLayout containing the Toolbar and ScrollView)
        mainContent = findViewById(R.id.main_content);
        if (mainContent == null) {
            Toast.makeText(this, "Main content view not found!", Toast.LENGTH_LONG).show();
            return; // Prevent further execution to avoid crashes
        }

        // Customize ActionBarDrawerToggle for animation
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // Removed the toolbar rotation animation to keep it professional
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                animateMenuItems();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                if (newState == DrawerLayout.STATE_SETTLING) {
                    if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        // Drawer is opening
                        Animation slideIn = AnimationUtils.loadAnimation(AboutActivity.this, R.anim.slide_in_left);
                        navigationView.startAnimation(slideIn);

                        // Dim the main content
                        ObjectAnimator dimAnimator = ObjectAnimator.ofFloat(mainContent, "alpha", 1f, 0.6f);
                        dimAnimator.setDuration(300); // Match the opening animation duration
                        dimAnimator.start();
                    } else {
                        // Drawer is closing
                        ObjectAnimator restoreAnimator = ObjectAnimator.ofFloat(mainContent, "alpha", 0.6f, 1f);
                        restoreAnimator.setDuration(200); // Match the closing animation duration
                        restoreAnimator.start();
                    }
                }
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update navigation header with user data (if available)
        updateNavHeader(navigationView);

        // Set onClickListeners for social media icons
        setSocialMediaLinks();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(AboutActivity.this, OurFeedbacks.class));
            }
        });

        // Animate developer cards
        animateDeveloperCards();
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.username);
        TextView userEmail = headerView.findViewById(R.id.mobile);

        // Retrieve user data from SharedPreferences (or another source)
        String name = sharedPreferences.getString("user_name", "User Name");
        String email = sharedPreferences.getString("user_email", "user@example.com");

        userName.setText(name);
        userEmail.setText(email);
    }

    private void setSocialMediaLinks() {
        // Developer 1: Vishal Bhutekar
        findViewById(R.id.instagram1).setOnClickListener(v -> openUrl("https://www.instagram.com/vishal_b__21/"));
        findViewById(R.id.github1).setOnClickListener(v -> openUrl("https://github.com/vishal-bhutekar21"));
        findViewById(R.id.linkedin1).setOnClickListener(v -> openUrl("https://www.linkedin.com/in/vishal-bhutekar-17552b283/"));

        // Developer 2: Chaitany Kakde
        findViewById(R.id.instagram2).setOnClickListener(v -> openUrl("https://www.instagram.com/chaitanyk_07/"));
        findViewById(R.id.github2).setOnClickListener(v -> openUrl("https://github.com/chaitanykakde"));
        findViewById(R.id.linkedin2).setOnClickListener(v -> openUrl("https://www.linkedin.com/in/chaitany-kakde-2a3ba62a8/"));

        // Developer 3: Satwik Mahajan
        findViewById(R.id.instagram3).setOnClickListener(v -> openUrl("https://www.instagram.com/satwik_mahajan_1"));
        findViewById(R.id.github3).setOnClickListener(v -> openUrl("https://github.com/SatwikMahajan01"));
        findViewById(R.id.linkedin3).setOnClickListener(v -> openUrl("https://www.linkedin.com/in/satwik-mahajan-173766218/"));

        // Developer 4: Karan Bankar
        findViewById(R.id.instagram4).setOnClickListener(v -> openUrl("https://www.instagram.com/karan_bankar_54/"));
        findViewById(R.id.github4).setOnClickListener(v -> openUrl("https://github.com/KaranBankar"));
        findViewById(R.id.linkedin4).setOnClickListener(v -> openUrl("https://www.linkedin.com/in/karan-bankar-453b57252/"));
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link: " + url, Toast.LENGTH_SHORT).show();
        }
    }

    private void animateDeveloperCards() {
        LinearLayout container = findViewById(R.id.developer_container);
        for (int i = 0; i < container.getChildCount(); i++) {
            View card = container.getChildAt(i);
            if (card.getId() != R.id.ourfeedbacks) { // Exclude the button
                card.setAlpha(0f);
                card.setTranslationY(100f);

                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
                ObjectAnimator slideUp = ObjectAnimator.ofFloat(card, "translationY", 100f, 0f);

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(fadeIn, slideUp);
                animatorSet.setDuration(500);
                animatorSet.setStartDelay(i * 200L); // Staggered delay for each card
                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.start();
            }
        }
    }

    private void animateMenuItems() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            MenuItem item = navigationView.getMenu().getItem(i);
            View itemView = navigationView.findViewById(item.getItemId());
            if (itemView != null && item.isVisible()) {
                itemView.setAlpha(0f);
                itemView.setTranslationX(-50f); // Slide in from the left

                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(itemView, "alpha", 0f, 1f);
                ObjectAnimator slideIn = ObjectAnimator.ofFloat(itemView, "translationX", -50f, 0f);

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(fadeIn, slideIn);
                animatorSet.setDuration(200);
                animatorSet.setStartDelay(i * 50L); // Staggered delay for each menu item
                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.start();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            NavigationView navigationView = findViewById(R.id.nav_view);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            navigationView.startAnimation(fadeOut);

            // Restore the main content alpha when closing via back press
            ObjectAnimator restoreAnimator = ObjectAnimator.ofFloat(mainContent, "alpha", 0.6f, 1f);
            restoreAnimator.setDuration(200); // Match the closing animation duration
            restoreAnimator.start();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Intent intent;

        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            intent = new Intent(this, Dashboard.class); // Assuming Dashboard is the home activity
            startActivity(intent);
        } else if (itemId == R.id.nav_profile) {
            intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_share) {
            shareApp();
        } else if (itemId == R.id.nav_feedback) {
            intent = new Intent(this, FeedbackActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_about) {
            // Already in AboutActivity, no action needed
        } else if (itemId == R.id.nav_logout) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLogged", false);
            editor.apply();
            intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        } else {
            return false;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareApp() {
        String appLink = "https://play.google.com/store/apps/details?id=com.chaitany.carbonview";
        String shareMessage = "Check out Carbon Footprint Tracker, an app to help you track and reduce your carbon emissions! 🌍\n\n" +
                "Download it here: " + appLink + "\n\n" +
                "Let's work together for a greener planet!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Carbon Footprint Tracker");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}