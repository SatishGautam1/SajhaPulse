package com.nighttech.sajhapulse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.auth.LoginActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity
 * ─────────────────────────────────────────────────────────────────────
 * Dashboard screen of SajhaPulse.
 * - Navigation drawer (sidebar)
 * - Live rate card (USD/NPR) — loaded from Firestore
 * - Gold / Silver quick-stat cards
 * - Forex rates table (dynamic rows)
 * - Market Updates section
 * - FAB → quick currency converter
 *
 * Firebase usage:
 *   FirebaseAuth     — get current user info for drawer header
 *   FirebaseFirestore — real-time listener on "rates/latest" document
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    // ── Firebase ───────────────────────────────────────────────────
    private FirebaseAuth        mAuth;
    private FirebaseFirestore   mFirestore;
    private ListenerRegistration ratesListener; // real-time listener handle

    // ── Drawer ─────────────────────────────────────────────────────
    private DrawerLayout   drawerLayout;
    private NavigationView navView;

    // ── Toolbar ────────────────────────────────────────────────────
    private MaterialToolbar       mainToolbar;
    private TextView              toolbarGreeting;
    private TextView              toolbarUserName;
    private ImageButton           toolbarNotifyBtn;
    private ShapeableImageView    toolbarAvatar;

    // ── Cards & Rate Views ─────────────────────────────────────────
    private TextView              rateValue;
    private TextView              rateLastUpdated;
    private TextView              goldValue;
    private TextView              silverValue;
    private View                  forexTableContainer;
    private TextView              newsPlaceholder;

    // ── FAB ────────────────────────────────────────────────────────
    private ExtendedFloatingActionButton fabConvert;

    // ── Lifecycle ──────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth      = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        bindViews();
        setupToolbar();
        setupDrawer();
        populateUserInfo();
        setClickListeners();
        attachRatesListener();
        updateGreeting();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Guard: if user signed out externally, go back to Login
        if (mAuth.getCurrentUser() == null) {
            goToLogin();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach real-time listener to avoid unnecessary reads while backgrounded
        if (ratesListener != null) {
            ratesListener.remove();
            ratesListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-attach if we lost it
        if (ratesListener == null) {
            attachRatesListener();
        }
    }

    // ── View Binding ───────────────────────────────────────────────

    private void bindViews() {
        drawerLayout        = findViewById(R.id.drawer_layout);
        navView             = findViewById(R.id.nav_view);
        mainToolbar         = findViewById(R.id.main_toolbar);
        toolbarGreeting     = findViewById(R.id.toolbar_greeting);
        toolbarUserName     = findViewById(R.id.toolbar_user_name);
        toolbarNotifyBtn    = findViewById(R.id.toolbar_notify_btn);
        toolbarAvatar       = findViewById(R.id.toolbar_avatar);
        rateValue           = findViewById(R.id.rate_value);
        rateLastUpdated     = findViewById(R.id.rate_last_updated);
        goldValue           = findViewById(R.id.gold_value);
        silverValue         = findViewById(R.id.silver_value);
        forexTableContainer = findViewById(R.id.forex_table_container);
        newsPlaceholder     = findViewById(R.id.news_placeholder);
        fabConvert          = findViewById(R.id.fab_convert);
    }

    // ── Toolbar & Drawer ───────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                mainToolbar,
                R.string.app_name,   // open description (accessibility)
                R.string.app_name    // close description
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this);
    }

    // ── User Info Population ───────────────────────────────────────

    /**
     * Reads the current FirebaseUser and populates:
     *   - Toolbar greeting/name
     *   - Navigation drawer header (via Firestore user document for extra fields)
     */
    private void populateUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        boolean isGuest = user.isAnonymous();

        // Toolbar
        toolbarUserName.setText(isGuest ? "Guest" :
                (user.getDisplayName() != null ? user.getDisplayName() : "User"));

        // Load avatar from profile URL using Glide (if available)
        if (!isGuest && user.getPhotoUrl() != null) {
            try {
                // Glide (optional dependency — graceful fallback if absent)
                Class<?> glide = Class.forName("com.bumptech.glide.Glide");
                // Dynamic invocation so the app compiles without Glide on classpath
                // Replace with: Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(toolbarAvatar);
                Log.d(TAG, "Glide available — load avatar: " + user.getPhotoUrl());
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "Glide not found — using default avatar");
            }
        }

        // Firestore: fetch extended user data for drawer header
        mFirestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        updateDrawerHeader(snapshot);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load user doc", e));
    }

    private void updateDrawerHeader(DocumentSnapshot snapshot) {
        // Drawer header views — inflated from nav_header_main layout
        View headerView = navView.getHeaderView(0);
        if (headerView == null) return;

        TextView headerName  = headerView.findViewById(R.id.nav_header_name);
        TextView headerEmail = headerView.findViewById(R.id.nav_header_email);

        if (headerName != null) {
            String name = snapshot.getString("displayName");
            headerName.setText(name != null ? name : "SajhaPulse User");
        }
        if (headerEmail != null) {
            String email = snapshot.getString("email");
            headerEmail.setText(email != null ? email : "guest@sajhapulse.app");
        }
    }

    // ── Firestore Real-Time Rates ──────────────────────────────────

    /**
     * Attaches a real-time snapshot listener to:
     *   Firestore → "rates" collection → "latest" document
     *
     * Expected document schema:
     * {
     *   usdNpr   : double,
     *   goldNpr  : double,
     *   silverNpr: double,
     *   change   : double,   (% change, e.g. 0.12)
     *   updatedAt: Timestamp
     * }
     *
     * Replace this with your actual data source — NRB API, custom backend, etc.
     */
    private void attachRatesListener() {
        ratesListener = mFirestore.collection("rates")
                .document("latest")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Rates listener error", error);
                        rateLastUpdated.setText("Error loading rates");
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        updateRateCards(snapshot);
                    } else {
                        rateLastUpdated.setText("No data available");
                    }
                });
    }

    private void updateRateCards(DocumentSnapshot snapshot) {
        // USD/NPR rate
        Double usdNpr = snapshot.getDouble("usdNpr");
        if (usdNpr != null) {
            rateValue.setText(String.format(Locale.getDefault(), "%.2f", usdNpr));
        }

        // Gold
        Double goldNpr = snapshot.getDouble("goldNpr");
        if (goldNpr != null) {
            goldValue.setText(String.format(Locale.getDefault(), "%.0f", goldNpr));
        }

        // Silver
        Double silverNpr = snapshot.getDouble("silverNpr");
        if (silverNpr != null) {
            silverValue.setText(String.format(Locale.getDefault(), "%.0f", silverNpr));
        }

        // Last updated timestamp
        com.google.firebase.Timestamp ts = snapshot.getTimestamp("updatedAt");
        if (ts != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            rateLastUpdated.setText("Updated " + sdf.format(ts.toDate()));
        }
    }

    // ── Greeting ───────────────────────────────────────────────────

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if      (hour < 12) greeting = "Good morning,";
        else if (hour < 17) greeting = "Good afternoon,";
        else                greeting = "Good evening,";
        toolbarGreeting.setText(greeting);
    }

    // ── Click Listeners ────────────────────────────────────────────

    private void setClickListeners() {
        // FAB → open converter (stub — launch converter activity/bottom sheet)
        fabConvert.setOnClickListener(v -> {
            Toast.makeText(this, "Converter coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Notification button
        toolbarNotifyBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        });

        // Avatar → profile
        toolbarAvatar.setOnClickListener(v -> {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
        });
    }

    // ── NavigationView Item Selection ──────────────────────────────

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home
        } else if (id == R.id.nav_forex) {
            Toast.makeText(this, "Forex Rates", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_gold) {
            Toast.makeText(this, "Gold & Silver", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_converter) {
            Toast.makeText(this, "Converter", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_alerts) {
            Toast.makeText(this, "Rate Alerts", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_sign_out) {
            signOut();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ── Sign Out ───────────────────────────────────────────────────

    /**
     * Signs out from both Firebase and Google (to clear the cached account
     * so the account picker shows on next login).
     */
    private void signOut() {
        // Detach listeners before signing out
        if (ratesListener != null) {
            ratesListener.remove();
            ratesListener = null;
        }

        mAuth.signOut();

        // Also sign out from Google to reset account picker
        com.google.android.gms.auth.api.signin.GoogleSignInOptions gso =
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions
                        .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build();
        com.google.android.gms.auth.api.signin.GoogleSignIn
                .getClient(this, gso)
                .signOut()
                .addOnCompleteListener(task -> goToLogin());
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}