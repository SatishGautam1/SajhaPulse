package com.nighttech.sajhapulse.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nighttech.sajhapulse.auth.LoginActivity;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.utils.SplashAnimationManager;

/**
 * SplashActivity
 * ─────────────────────────────────────────────────────────────────────
 * Entry-point activity shown at launch.
 * - Plays a branded animation sequence (logo pulse → text fade-in)
 * - Checks FirebaseAuth state to route: MainActivity or LoginActivity
 * - Total duration: ~2.4 s before navigation
 */
public class SplashActivity extends AppCompatActivity {

    // ── Duration constants (ms) ────────────────────────────────────
    private static final long LOGO_FADE_DURATION      = 500L;
    private static final long RING_PULSE_DURATION     = 900L;
    private static final long TEXT_FADE_DELAY         = 700L;
    private static final long TEXT_FADE_DURATION      = 400L;
    private static final long DIVIDER_DELAY           = 1000L;
    private static final long VERSION_DELAY           = 1100L;
    private static final long NAV_DELAY               = 2400L;

    // ── Views ──────────────────────────────────────────────────────
    private View       splashLogo;
    private View       splashRing1;
    private View       splashRing2;
    private TextView   splashAppName;
    private TextView   splashTagline;
    private View       splashDivider;
    private TextView   splashVersion;

    private FirebaseAuth mAuth;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ── Lifecycle ──────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force dark mode regardless of system setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        startAnimationSequence();

        // Schedule navigation after total splash duration
        handler.postDelayed(this::navigateToNextScreen, NAV_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel pending callbacks to prevent leaks
        handler.removeCallbacksAndMessages(null);
    }

    // ── View Binding ───────────────────────────────────────────────

    private void bindViews() {
        splashLogo     = findViewById(R.id.splash_logo);
        splashRing1    = findViewById(R.id.splash_ring_1);
        splashRing2    = findViewById(R.id.splash_ring_2);
        splashAppName  = findViewById(R.id.splash_app_name);
        splashTagline  = findViewById(R.id.splash_tagline);
        splashDivider  = findViewById(R.id.splash_divider);
        splashVersion  = findViewById(R.id.splash_version);
    }

    // ── Animation Sequence ─────────────────────────────────────────

    /**
     * Orchestrates the full splash animation:
     * 1. Logo scales up + fades in
     * 2. Pulse rings expand outward
     * 3. App name + tagline fade in (staggered)
     * 4. Divider line + version badge appear
     */
    private void startAnimationSequence() {
        // Step 1 — Logo enter
        animateLogo();

        // Step 2 — Pulse rings (slightly delayed)
        handler.postDelayed(this::animatePulseRings, 300L);

        // Step 3 — Text fade-in
        handler.postDelayed(this::animateText, TEXT_FADE_DELAY);

        // Step 4 — Divider + version
        handler.postDelayed(() -> fadeIn(splashDivider, TEXT_FADE_DURATION), DIVIDER_DELAY);
        handler.postDelayed(() -> fadeIn(splashVersion, TEXT_FADE_DURATION), VERSION_DELAY);
    }

    private void animateLogo() {
        splashLogo.setAlpha(0f);
        splashLogo.setScaleX(0.6f);
        splashLogo.setScaleY(0.6f);

        ObjectAnimator fadeIn  = ObjectAnimator.ofFloat(splashLogo, View.ALPHA,  0f, 1f);
        ObjectAnimator scaleX  = ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 0.6f, 1f);
        ObjectAnimator scaleY  = ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 0.6f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, scaleX, scaleY);
        set.setDuration(LOGO_FADE_DURATION);
        set.setInterpolator(new DecelerateInterpolator(2f));
        set.start();
    }

    private void animatePulseRings() {
        // Ring 1 — largest, most transparent
        SplashAnimationManager.animatePulseRing(splashRing1, RING_PULSE_DURATION, 0L,   1.6f, 0.35f);
        // Ring 2 — medium, slightly delayed
        SplashAnimationManager.animatePulseRing(splashRing2, RING_PULSE_DURATION, 120L, 1.3f, 0.45f);
    }

    private void animateText() {
        // App name
        ObjectAnimator nameFade  = ObjectAnimator.ofFloat(splashAppName, View.ALPHA,        0f, 1f);
        ObjectAnimator nameSlide = ObjectAnimator.ofFloat(splashAppName, View.TRANSLATION_Y, 20f, 0f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameFade, nameSlide);
        nameSet.setDuration(TEXT_FADE_DURATION);
        nameSet.setInterpolator(new DecelerateInterpolator());
        nameSet.start();

        // Tagline — slightly delayed
        handler.postDelayed(() -> {
            ObjectAnimator tagFade  = ObjectAnimator.ofFloat(splashTagline, View.ALPHA,         0f, 1f);
            ObjectAnimator tagSlide = ObjectAnimator.ofFloat(splashTagline, View.TRANSLATION_Y, 15f, 0f);
            AnimatorSet tagSet = new AnimatorSet();
            tagSet.playTogether(tagFade, tagSlide);
            tagSet.setDuration(TEXT_FADE_DURATION);
            tagSet.setInterpolator(new DecelerateInterpolator());
            tagSet.start();
        }, 120L);
    }

    private void fadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    // ── Navigation ─────────────────────────────────────────────────

    /**
     * Checks Firebase auth state.
     * - Signed-in user  → MainActivity (skip login)
     * - Guest / null    → LoginActivity
     */
    private void navigateToNextScreen() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            // User is already authenticated — go straight to dashboard
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // No session — show login
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        // Crossfade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}