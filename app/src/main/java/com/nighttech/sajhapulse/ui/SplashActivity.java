package com.nighttech.sajhapulse.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
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

    private static final long LOGO_FADE_DURATION  = 500L;
    private static final long RING_PULSE_DURATION = 900L;
    private static final long TEXT_FADE_DELAY     = 700L;
    private static final long TEXT_FADE_DURATION  = 400L;
    private static final long DIVIDER_DELAY       = 1000L;
    private static final long VERSION_DELAY       = 1100L;
    private static final long NAV_DELAY           = 2400L;

    private View     splashLogo;
    private View     splashRing1;
    private View     splashRing2;
    private TextView splashAppName;
    private TextView splashTagline;
    private View     splashDivider;
    private TextView splashVersion;

    private FirebaseAuth mAuth;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ── Apply system bar styling (white icons on dark bg) ──
        applySystemBarStyling();

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        startAnimationSequence();
        handler.postDelayed(this::navigateToNextScreen, NAV_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // ── System Bar Styling ─────────────────────────────────────────

    /**
     * Sets the status bar and navigation bar to match the app's dark theme
     * and ensures icons are white (light icons on dark background).
     */
    private void applySystemBarStyling() {
        Window window = getWindow();

        window.setStatusBarColor(getResources().getColor(R.color.background, getTheme()));
        window.setNavigationBarColor(getResources().getColor(R.color.background, getTheme()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // 0 = no light appearance → icons stay white
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    // ── View Binding ───────────────────────────────────────────────

    private void bindViews() {
        splashLogo    = findViewById(R.id.splash_logo);
        splashRing1   = findViewById(R.id.splash_ring_1);
        splashRing2   = findViewById(R.id.splash_ring_2);
        splashAppName = findViewById(R.id.splash_app_name);
        splashTagline = findViewById(R.id.splash_tagline);
        splashDivider = findViewById(R.id.splash_divider);
        splashVersion = findViewById(R.id.splash_version);
    }

    // ── Animation Sequence ─────────────────────────────────────────

    private void startAnimationSequence() {
        animateLogo();
        handler.postDelayed(this::animatePulseRings, 300L);
        handler.postDelayed(this::animateText, TEXT_FADE_DELAY);
        handler.postDelayed(() -> fadeIn(splashDivider, TEXT_FADE_DURATION), DIVIDER_DELAY);
        handler.postDelayed(() -> fadeIn(splashVersion, TEXT_FADE_DURATION), VERSION_DELAY);
    }

    private void animateLogo() {
        splashLogo.setAlpha(0f);
        splashLogo.setScaleX(0.6f);
        splashLogo.setScaleY(0.6f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(splashLogo, View.ALPHA,   0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 0.6f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 0.6f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, scaleX, scaleY);
        set.setDuration(LOGO_FADE_DURATION);
        set.setInterpolator(new DecelerateInterpolator(2f));
        set.start();
    }

    private void animatePulseRings() {
        SplashAnimationManager.animatePulseRing(splashRing1, RING_PULSE_DURATION, 0L,   1.6f, 0.35f);
        SplashAnimationManager.animatePulseRing(splashRing2, RING_PULSE_DURATION, 120L, 1.3f, 0.45f);
    }

    private void animateText() {
        ObjectAnimator nameFade  = ObjectAnimator.ofFloat(splashAppName, View.ALPHA,         0f, 1f);
        ObjectAnimator nameSlide = ObjectAnimator.ofFloat(splashAppName, View.TRANSLATION_Y, 20f, 0f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameFade, nameSlide);
        nameSet.setDuration(TEXT_FADE_DURATION);
        nameSet.setInterpolator(new DecelerateInterpolator());
        nameSet.start();

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
        if (view == null) return;
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    // ── Navigation ─────────────────────────────────────────────────

    private void navigateToNextScreen() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Intent intent = currentUser != null
                ? new Intent(SplashActivity.this, MainActivity.class)
                : new Intent(SplashActivity.this, LoginActivity.class);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}