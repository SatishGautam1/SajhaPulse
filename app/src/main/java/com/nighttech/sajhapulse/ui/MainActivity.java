package com.nighttech.sajhapulse.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * MainActivity — SajhaPulse Home Dashboard.
 *
 * System bars:
 *   applyAppBars()  → crimson status bar + nav bar, WHITE icons throughout.
 *                     Called ONCE. No per-scroll color flipping — the bar
 *                     stays crimson at all times, consistent with the app theme.
 *   AppBarLayout fitsSystemWindows="true" absorbs the top inset so the
 *   crimson toolbar aligns seamlessly under the crimson status bar.
 *
 * DateTime Hero Card — 3D animation:
 *   On enter: card scales from 0.88 → 1.0 with a slight rotationX tilt
 *   (0° → -2° → 0°) giving a "lift off the page" feel.
 *   The LIVE dot pulses with a repeating alpha animation.
 *   Seconds ticker updates every second for a live clock feel.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;

    // DateTime Hero Views
    private TextView tvTimeMain, tvAmPm, tvSeconds;
    private TextView tvDayName, tvBsDate, tvBsDateLatin, tvAdDate;
    private MaterialCardView cardDatetime;
    private View viewLiveDot;

    // Toolbar greeting
    private TextView tvGreetingLabel, tvGreetingName;
    private ImageView ivGreetingIcon;

    private FirebaseAuth mAuth;

    // Clock — updates every second
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    // Live dot pulse — repeating alpha animation
    private final Handler liveDotHandler = new Handler(Looper.getMainLooper());
    private Runnable liveDotRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ── System bars: crimson everywhere, white icons ──────────────
        // AppBarLayout handles its own top inset via fitsSystemWindows="true"
        // in content_main.xml. We do NOT call applyInsetPadding here —
        // that would double-pad the coordinator layout.
        SystemBarHelper.applyAppBars(this);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupToolbarAndDrawer();
        setupNavigationListeners();
        populateDrawerHeader();
        applyGreeting();
        startLiveClock();
        updateCalendarInfo();
        animateDateTimeCard();
        startLiveDotPulse();
    }

    // ── View binding ──────────────────────────────────────────────────

    private void bindViews() {
        drawerLayout    = findViewById(R.id.drawer_layout);
        navView         = findViewById(R.id.nav_view);
        toolbar         = findViewById(R.id.toolbar);
        appBarLayout    = findViewById(R.id.app_bar);

        // DateTime
        tvTimeMain      = findViewById(R.id.tv_time_main);
        tvAmPm          = findViewById(R.id.tv_am_pm);
        tvSeconds       = findViewById(R.id.tv_seconds);
        tvDayName       = findViewById(R.id.tv_day_name);
        tvBsDate        = findViewById(R.id.tv_bs_date);
        tvBsDateLatin   = findViewById(R.id.tv_bs_date_latin);
        tvAdDate        = findViewById(R.id.tv_ad_date);
        cardDatetime    = findViewById(R.id.card_datetime);
        viewLiveDot     = findViewById(R.id.view_live_dot);

        // Greeting
        tvGreetingLabel = findViewById(R.id.tv_greeting_label);
        tvGreetingName  = findViewById(R.id.tv_greeting_name);
        ivGreetingIcon  = findViewById(R.id.iv_greeting_icon);
    }

    // ── Toolbar + Drawer setup ────────────────────────────────────────

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close);
        // Keep hamburger icon WHITE — matches crimson toolbar
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    // ── DateTime Hero Card — 3D entrance animation ────────────────────

    /**
     * Entrance: card scales up from 88% + lifts (translationZ) + subtle
     * rotationX tilt that eases back to 0, producing a "3D card flip up"
     * feel without needing a custom view.
     *
     * Technique:
     *   1. Start with scale=0.88, translationY=+40dp, alpha=0
     *   2. Animate to scale=1.0, translationY=0, alpha=1  (300ms, decelerate)
     *   3. After that, do a slight rotationX bounce 0→-3→0            (400ms)
     */
    private void animateDateTimeCard() {
        if (cardDatetime == null) return;

        // Initial state
        cardDatetime.setScaleX(0.88f);
        cardDatetime.setScaleY(0.88f);
        cardDatetime.setAlpha(0f);
        cardDatetime.setTranslationY(48f);
        cardDatetime.setCameraDistance(8000 * getResources().getDisplayMetrics().density);

        // Phase 1: scale-in + fade-in + slide-up
        ObjectAnimator scaleX  = ObjectAnimator.ofFloat(cardDatetime, "scaleX", 0.88f, 1.0f);
        ObjectAnimator scaleY  = ObjectAnimator.ofFloat(cardDatetime, "scaleY", 0.88f, 1.0f);
        ObjectAnimator alpha   = ObjectAnimator.ofFloat(cardDatetime, "alpha", 0f, 1f);
        ObjectAnimator transY  = ObjectAnimator.ofFloat(cardDatetime, "translationY", 48f, 0f);

        AnimatorSet phase1 = new AnimatorSet();
        phase1.playTogether(scaleX, scaleY, alpha, transY);
        phase1.setDuration(360);
        phase1.setInterpolator(new DecelerateInterpolator(2f));
        phase1.setStartDelay(180);

        // Phase 2: rotationX tilt for "lifting off the plane" feel
        ObjectAnimator rotX = ObjectAnimator.ofFloat(cardDatetime, "rotationX", 0f, -4f, 0f);
        rotX.setDuration(500);
        rotX.setInterpolator(new AccelerateDecelerateInterpolator());
        rotX.setStartDelay(phase1.getStartDelay() + phase1.getDuration() - 80);

        phase1.start();
        rotX.start();
    }

    // ── Live dot pulse ────────────────────────────────────────────────

    /**
     * Repeating alpha fade: 1.0 → 0.2 → 1.0 every 1.4 s.
     * Gives the LIVE indicator a subtle heartbeat without being distracting.
     */
    private void startLiveDotPulse() {
        if (viewLiveDot == null) return;

        liveDotRunnable = new Runnable() {
            boolean goingDown = true;

            @Override
            public void run() {
                float target = goingDown ? 0.15f : 1.0f;
                viewLiveDot.animate()
                        .alpha(target)
                        .setDuration(700)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                goingDown = !goingDown;
                liveDotHandler.postDelayed(this, 700);
            }
        };
        liveDotHandler.post(liveDotRunnable);
    }

    // ── Live clock — ticks every second ──────────────────────────────

    private void startLiveClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));

                // Hours & minutes (12-hour format for display)
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
                SimpleDateFormat amPmFormat = new SimpleDateFormat("a",     Locale.ENGLISH);
                SimpleDateFormat dayFormat  = new SimpleDateFormat("EEEE",  Locale.ENGLISH);
                SimpleDateFormat secFormat  = new SimpleDateFormat(":ss",   Locale.ENGLISH);

                if (tvTimeMain != null) tvTimeMain.setText(timeFormat.format(cal.getTime()));
                if (tvAmPm    != null) tvAmPm.setText(amPmFormat.format(cal.getTime()));
                if (tvDayName != null) tvDayName.setText(dayFormat.format(cal.getTime()).toUpperCase(Locale.ENGLISH));
                if (tvSeconds != null) tvSeconds.setText(secFormat.format(cal.getTime()));

                // Tick every second for seconds display
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    // ── Calendar info ─────────────────────────────────────────────────

    private void updateCalendarInfo() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));

        // A.D. date
        SimpleDateFormat adFormat = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
        if (tvAdDate != null) tvAdDate.setText(adFormat.format(cal.getTime()));

        // B.S. — Nepali script + latin transliteration
        // TODO: Replace with a proper Nepali Calendar library for accurate conversion.
        if (tvBsDate      != null) tvBsDate.setText("२२ जेठ २०८३");
        if (tvBsDateLatin != null) tvBsDateLatin.setText("22 Jestha 2083");
    }

    // ── Greeting ──────────────────────────────────────────────────────

    private void applyGreeting() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        int iconRes;
        if (hour < 12) {
            greeting = getString(R.string.greeting_morning);
            iconRes  = R.drawable.ic_sun;
        } else if (hour < 17) {
            greeting = getString(R.string.greeting_afternoon);
            iconRes  = R.drawable.ic_sun;
        } else {
            greeting = getString(R.string.greeting_evening);
            iconRes  = R.drawable.ic_moon; // add ic_moon drawable if not present
        }

        if (tvGreetingLabel != null) tvGreetingLabel.setText(greeting);
        if (ivGreetingIcon  != null) ivGreetingIcon.setImageResource(iconRes);

        FirebaseUser user = mAuth.getCurrentUser();
        String firstName = "Guest";
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            firstName = user.getDisplayName().split(" ")[0];
        }
        if (tvGreetingName != null) tvGreetingName.setText("Namaste, " + firstName);
    }

    // ── Drawer header population ──────────────────────────────────────

    private void populateDrawerHeader() {
        View headerView = navView.getHeaderView(0);
        if (headerView == null) return;

        ImageView ivAvatar = headerView.findViewById(R.id.nav_header_avatar);
        TextView  tvName   = headerView.findViewById(R.id.nav_header_name);
        TextView  tvEmail  = headerView.findViewById(R.id.nav_header_email);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String displayName = user.getDisplayName();
            tvName.setText((displayName != null && !displayName.isEmpty())
                    ? displayName : "SajhaPulse User");

            String email = user.getEmail();
            tvEmail.setText((email != null && !email.isEmpty())
                    ? email : "No email linked");

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .transform(new CircleCrop())
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_account_circle);
            }
        } else {
            tvName.setText(getString(R.string.placeholder_guest));
            tvEmail.setText(getString(R.string.placeholder_guest_email));
            ivAvatar.setImageResource(R.drawable.ic_account_circle);
        }
    }

    // ── Navigation listeners ──────────────────────────────────────────

    private void setupNavigationListeners() {
        navView.setNavigationItemSelectedListener(this);

        View marketsBtn   = findViewById(R.id.btn_nav_markets);
        View forexBtn     = findViewById(R.id.btn_nav_forex);
        View newsBtn      = findViewById(R.id.btn_nav_news);
        View utilitiesBtn = findViewById(R.id.btn_nav_utilities);

        if (marketsBtn   != null) marketsBtn.setOnClickListener(v ->
                Toast.makeText(this, "Markets — Coming Soon", Toast.LENGTH_SHORT).show());
        if (forexBtn     != null) forexBtn.setOnClickListener(v ->
                Toast.makeText(this, "Forex Rates — Coming Soon", Toast.LENGTH_SHORT).show());
        if (newsBtn      != null) newsBtn.setOnClickListener(v ->
                Toast.makeText(this, "News Pulse — Coming Soon", Toast.LENGTH_SHORT).show());
        if (utilitiesBtn != null) utilitiesBtn.setOnClickListener(v ->
                Toast.makeText(this, "Utilities — Coming Soon", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        // TODO: handle other nav items as screens are built

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

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockRunnable);
        liveDotHandler.removeCallbacks(liveDotRunnable);
    }
}