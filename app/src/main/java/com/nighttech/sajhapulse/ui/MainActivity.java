package com.nighttech.sajhapulse.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.google.android.material.chip.Chip;
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
 *   FirebaseAuth      — get current user info for drawer header
 *   FirebaseFirestore — real-time listener on "rates/latest" document
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    // ── Firebase ───────────────────────────────────────────────────
    private FirebaseAuth        mAuth;
    private FirebaseFirestore   mFirestore;
    private ListenerRegistration ratesListener;

    // ── Drawer ─────────────────────────────────────────────────────
    private DrawerLayout   drawerLayout;
    private NavigationView navView;

    // ── Toolbar ────────────────────────────────────────────────────
    private MaterialToolbar    mainToolbar;
    private TextView           toolbarGreeting;
    private TextView           toolbarUserName;
    private ImageButton        toolbarNotifyBtn;
    private ShapeableImageView toolbarAvatar;

    // ── Cards & Rate Views ─────────────────────────────────────────
    private TextView     rateValue;
    private TextView     rateLastUpdated;
    private Chip         rateChangeChip;
    private TextView     goldValue;
    private TextView     silverValue;
    private LinearLayout forexTableContainer;
    private TextView     newsPlaceholder;
    private TextView     forexSeeAll;

    // ── FAB ────────────────────────────────────────────────────────
    private ExtendedFloatingActionButton fabConvert;

    // ── State ──────────────────────────────────────────────────────
    private boolean listenerAttached = false;

    // ── Lifecycle ──────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applySystemBarStyling();

        mAuth      = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        bindViews();
        setupToolbar();
        setupDrawer();
        populateUserInfo();
        setClickListeners();
        updateGreeting();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }
        // Attach listener here so it's active whenever the activity is visible
        if (!listenerAttached) {
            attachRatesListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachRatesListener();
    }

    // ── Status / Navigation Bar Styling ───────────────────────────

    /**
     * Forces dark status bar icons OFF (white icons) and sets the
     * status bar background to match the app's toolbar color.
     * Works on API 23+ for status bar, API 27+ for nav bar.
     */
    private void applySystemBarStyling() {
        Window window = getWindow();

        // Status bar background — match toolbar
        window.setStatusBarColor(getResources().getColor(R.color.toolbar_background, getTheme()));
        // Navigation bar background — match surface
        window.setNavigationBarColor(getResources().getColor(R.color.surface, getTheme()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: use WindowInsetsController
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // Remove APPEARANCE_LIGHT_STATUS_BARS so icons stay white
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
                // Remove APPEARANCE_LIGHT_NAVIGATION_BARS so nav icons stay white
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23–29: use View flags
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            // Clear SYSTEM_UI_FLAG_LIGHT_STATUS_BAR → white status icons
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Clear SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR → white nav icons
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
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
        rateChangeChip      = findViewById(R.id.rate_change_chip);
        goldValue           = findViewById(R.id.gold_value);
        silverValue         = findViewById(R.id.silver_value);
        forexTableContainer = findViewById(R.id.forex_table_container);
        newsPlaceholder     = findViewById(R.id.news_placeholder);
        forexSeeAll         = findViewById(R.id.forex_see_all);
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
                R.string.app_name,
                R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this);
    }

    // ── User Info Population ───────────────────────────────────────

    private void populateUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        boolean isGuest = user.isAnonymous();

        toolbarUserName.setText(isGuest ? "Guest" :
                (user.getDisplayName() != null ? user.getDisplayName() : "User"));

        // Load avatar via Glide if available
        if (!isGuest && user.getPhotoUrl() != null) {
            try {
                Class<?> glideClass = Class.forName("com.bumptech.glide.Glide");
                // If Glide is on classpath, use it:
                // Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(toolbarAvatar);
                Log.d(TAG, "Glide available — load avatar: " + user.getPhotoUrl());
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "Glide not found — using default avatar");
            }
        }

        // Fetch extended user data for drawer header
        mFirestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::updateDrawerHeader)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load user doc", e));
    }

    private void updateDrawerHeader(DocumentSnapshot snapshot) {
        View headerView = navView.getHeaderView(0);
        if (headerView == null) return;

        TextView headerName  = headerView.findViewById(R.id.nav_header_name);
        TextView headerEmail = headerView.findViewById(R.id.nav_header_email);

        if (headerName != null) {
            String name = snapshot.exists() ? snapshot.getString("displayName") : null;
            headerName.setText(name != null ? name : "SajhaPulse User");
        }
        if (headerEmail != null) {
            String email = snapshot.exists() ? snapshot.getString("email") : null;
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
     *   change   : double,  (% change, e.g. 0.12 means +0.12%)
     *   updatedAt: Timestamp,
     *   // Optional forex pairs:
     *   eurNpr   : double,
     *   gbpNpr   : double,
     *   inrNpr   : double,
     *   audNpr   : double,
     *   // Optional news:
     *   marketUpdate: String
     * }
     */
    private void attachRatesListener() {
        if (listenerAttached) return;

        ratesListener = mFirestore.collection("rates")
                .document("latest")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Rates listener error", error);
                        if (rateLastUpdated != null) {
                            rateLastUpdated.setText("Error loading rates");
                        }
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        updateRateCards(snapshot);
                        updateForexTable(snapshot);
                        updateMarketNews(snapshot);
                    } else {
                        if (rateLastUpdated != null) {
                            rateLastUpdated.setText("No data available");
                        }
                    }
                });

        listenerAttached = true;
    }

    private void detachRatesListener() {
        if (ratesListener != null) {
            ratesListener.remove();
            ratesListener = null;
        }
        listenerAttached = false;
    }

    private void updateRateCards(DocumentSnapshot snapshot) {
        // USD/NPR rate
        Double usdNpr = snapshot.getDouble("usdNpr");
        if (usdNpr != null && rateValue != null) {
            rateValue.setText(String.format(Locale.getDefault(), "%.2f", usdNpr));
        }

        // % change chip
        Double change = snapshot.getDouble("change");
        if (change != null && rateChangeChip != null) {
            String sign   = change >= 0 ? "+" : "";
            String label  = String.format(Locale.getDefault(), "%s%.2f%%", sign, change);
            rateChangeChip.setText(label);

            // Swap chip style based on direction
            if (change >= 0) {
                rateChangeChip.setChipBackgroundColorResource(R.color.success_container);
                rateChangeChip.setTextColor(getResources().getColor(R.color.on_success_container, getTheme()));
            } else {
                rateChangeChip.setChipBackgroundColorResource(R.color.error_container);
                rateChangeChip.setTextColor(getResources().getColor(R.color.on_error_container, getTheme()));
            }
        }

        // Gold
        Double goldNpr = snapshot.getDouble("goldNpr");
        if (goldNpr != null && goldValue != null) {
            goldValue.setText(String.format(Locale.getDefault(), "%.0f", goldNpr));
        }

        // Silver
        Double silverNpr = snapshot.getDouble("silverNpr");
        if (silverNpr != null && silverValue != null) {
            silverValue.setText(String.format(Locale.getDefault(), "%.0f", silverNpr));
        }

        // Last updated timestamp
        com.google.firebase.Timestamp ts = snapshot.getTimestamp("updatedAt");
        if (ts != null && rateLastUpdated != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            rateLastUpdated.setText("Updated " + sdf.format(ts.toDate()));
        }
    }

    /**
     * Dynamically populates the forex table with currency pair rows.
     * Reads optional fields from the "latest" document.
     * Falls back gracefully if a field is missing.
     */
    private void updateForexTable(DocumentSnapshot snapshot) {
        if (forexTableContainer == null) return;
        forexTableContainer.removeAllViews();

        // Define pairs to display: [Flag emoji, Currency code, Firestore field key]
        String[][] pairs = {
                {"🇪🇺", "EUR", "eurNpr"},
                {"🇬🇧", "GBP", "gbpNpr"},
                {"🇮🇳", "INR", "inrNpr"},
                {"🇦🇺", "AUD", "audNpr"},
                {"🇨🇳", "CNY", "cnyNpr"},
                {"🇯🇵", "JPY", "jpyNpr"},
        };

        boolean anyRowAdded = false;
        for (String[] pair : pairs) {
            Double val = snapshot.getDouble(pair[2]);
            if (val != null) {
                addForexRow(pair[0], pair[1], val);
                anyRowAdded = true;
            }
        }

        if (!anyRowAdded) {
            // Show placeholder row if no forex data available yet
            TextView placeholder = new TextView(this);
            placeholder.setText("Forex data unavailable");
            placeholder.setTextColor(getResources().getColor(R.color.on_surface_variant, getTheme()));
            placeholder.setTextSize(13f);
            placeholder.setPadding(0, 8, 0, 8);
            forexTableContainer.addView(placeholder);
        }
    }

    /**
     * Inflates a single forex row: flag + currency code on the left,
     * NPR value on the right, with a subtle divider line below.
     */
    private void addForexRow(String flagEmoji, String currencyCode, double nprValue) {
        // Row container
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        int dp8  = dpToPx(8);
        int dp12 = dpToPx(12);
        row.setPadding(0, dp12, 0, dp12);

        // Flag + code
        TextView labelView = new TextView(this);
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        labelView.setText(flagEmoji + "  " + currencyCode + " / NPR");
        labelView.setTextColor(getResources().getColor(R.color.on_surface, getTheme()));
        labelView.setTextSize(14f);
        try {
            labelView.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                    android.graphics.Typeface.NORMAL));
        } catch (Exception ignored) {}

        // Value
        TextView valueView = new TextView(this);
        valueView.setText(String.format(Locale.getDefault(), "%.2f", nprValue));
        valueView.setTextColor(getResources().getColor(R.color.on_surface, getTheme()));
        valueView.setTextSize(14f);
        valueView.setGravity(android.view.Gravity.END);
        try {
            valueView.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                    android.graphics.Typeface.NORMAL));
        } catch (Exception ignored) {}

        row.addView(labelView);
        row.addView(valueView);

        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams divParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(getResources().getColor(R.color.outline_variant, getTheme()));

        // Wrapper to hold row + divider
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        wrapper.addView(divider);

        forexTableContainer.addView(wrapper);
    }

    /**
     * Updates the Market Updates card with text from Firestore.
     * Falls back to a default message if the field is absent.
     */
    private void updateMarketNews(DocumentSnapshot snapshot) {
        if (newsPlaceholder == null) return;

        String update = snapshot.getString("marketUpdate");
        if (update != null && !update.isEmpty()) {
            newsPlaceholder.setText(update);
            newsPlaceholder.setTextColor(
                    getResources().getColor(R.color.on_surface, getTheme()));
        } else {
            newsPlaceholder.setText("No market updates at the moment. Check back soon.");
            newsPlaceholder.setTextColor(
                    getResources().getColor(R.color.on_surface_variant, getTheme()));
        }
    }

    // ── Greeting ───────────────────────────────────────────────────

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if      (hour < 12) greeting = "Good morning,";
        else if (hour < 17) greeting = "Good afternoon,";
        else                greeting = "Good evening,";
        if (toolbarGreeting != null) {
            toolbarGreeting.setText(greeting);
        }
    }

    // ── Click Listeners ────────────────────────────────────────────

    private void setClickListeners() {
        fabConvert.setOnClickListener(v ->
                Toast.makeText(this, "Converter coming soon!", Toast.LENGTH_SHORT).show());

        toolbarNotifyBtn.setOnClickListener(v ->
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());

        toolbarAvatar.setOnClickListener(v ->
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show());

        if (forexSeeAll != null) {
            forexSeeAll.setOnClickListener(v ->
                    Toast.makeText(this, "All Forex Rates", Toast.LENGTH_SHORT).show());
        }
    }

    // ── NavigationView Item Selection ──────────────────────────────

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home — no-op
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

    private void signOut() {
        detachRatesListener();
        mAuth.signOut();

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

    // ── Helpers ────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}