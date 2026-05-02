package com.nighttech.sajhapulse.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

/**
 * SplashActivity — "Himalayas at Dusk" cinematic splash.
 *
 * Animation choreography:
 *   t=0ms    Mandala rings start rotating (continuous loop)
 *   t=0ms    Particle dots pulse in (staggered 80ms each)
 *   t=200ms  Logo: 3D Y-axis flip (rotationY 90→0) + zoom (scale 0.3→1.0) + fade
 *   t=820ms  Shimmer sweeps across logo (translationX left→right)
 *   t=860ms  Gold divider line fades in + scales from 0→full width
 *   t=900ms  App name: slide up (translationY 24→0) + fade in
 *   t=1100ms Tagline fades in
 *   t=1300ms "Powered by" credit fades in
 *   t=1600ms Navigate → MainActivity or LoginActivity
 *
 * All animations use ObjectAnimator / AnimatorSet for precise
 * hardware-accelerated control. No legacy Animation/Tween used.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    // ── Timing constants (ms) ───────────────────────────────────────────────
    private static final int DELAY_LOGO           = 200;
    private static final int DURATION_LOGO_FLIP   = 620;
    private static final int DELAY_SHIMMER        = 820;
    private static final int DURATION_SHIMMER     = 420;
    private static final int DELAY_DIVIDER        = 860;
    private static final int DURATION_DIVIDER     = 350;
    private static final int DELAY_APP_NAME       = 920;
    private static final int DURATION_APP_NAME    = 500;
    private static final int DELAY_TAGLINE        = 1120;
    private static final int DURATION_TAGLINE     = 400;
    private static final int DELAY_POWERED_BY     = 1300;
    private static final int DURATION_POWERED_BY  = 300;
    private static final long NAVIGATE_AFTER_MS   = 1650L;

    // Ring rotation durations (ms per full revolution)
    private static final int RING_OUTER_PERIOD    = 18_000;  // slow CW
    private static final int RING_MIDDLE_PERIOD   = 12_000;  // faster CCW
    private static final int PARTICLE_PULSE_PERIOD = 2_400;  // breathe

    // ── Firebase ────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;

    // ── Views ───────────────────────────────────────────────────────────────
    private View ivLogo;
    private View ringOuter, ringMiddle;
    private View shimmerView;
    private View dividerGold;
    private View tvAppName;
    private View tvTagline;
    private View tvPoweredBy;
    private View[] particles;

    // ── Splash screen gate ──────────────────────────────────────────────────
    private volatile boolean splashReady = false;

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // ① Must be before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !splashReady);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SystemBarHelper.applyAppBars(this);
        SystemBarHelper.applyInsetPadding(findViewById(android.R.id.content));

        // Enable hardware acceleration on root for 3D transforms
        getWindow().getDecorView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mAuth = FirebaseAuth.getInstance();
        bindViews();
        configureCameraDistance();   // critical for realistic 3D flip
        startMandalaRotation();
        startParticlePulse();
        scheduleLogoReveal();
    }

    // ── View binding ────────────────────────────────────────────────────────

    private void bindViews() {
        ivLogo      = findViewById(R.id.iv_splash_logo);
        ringOuter   = findViewById(R.id.ring_outer);
        ringMiddle  = findViewById(R.id.ring_middle);
        shimmerView = findViewById(R.id.view_shimmer);
        dividerGold = findViewById(R.id.divider_gold);
        tvAppName   = findViewById(R.id.tv_app_name);
        tvTagline   = findViewById(R.id.tv_app_tagline);
        tvPoweredBy = findViewById(R.id.tv_powered_by);

        particles = new View[]{
                findViewById(R.id.particle_1),
                findViewById(R.id.particle_2),
                findViewById(R.id.particle_3),
                findViewById(R.id.particle_4),
                findViewById(R.id.particle_5),
                findViewById(R.id.particle_6),
                findViewById(R.id.particle_7),
                findViewById(R.id.particle_8),
        };
    }

    /**
     * Sets a realistic camera distance on the logo so the Y-axis rotation
     * looks like genuine 3D perspective rather than a flat squash.
     * Formula: cameraDistance = density * 8000 (empirically good for ~140dp views).
     */
    private void configureCameraDistance() {
        if (ivLogo == null) return;
        float density = getResources().getDisplayMetrics().density;
        ivLogo.setCameraDistance(density * 8000f);
    }

    // ── ANIMATION 1: Mandala Ring Rotation ──────────────────────────────────

    /**
     * Outer ring: slow clockwise.
     * Middle ring: slightly faster counter-clockwise.
     * Both loop infinitely using INFINITE repeat + LINEAR interpolation.
     */
    private void startMandalaRotation() {
        if (ringOuter != null) {
            ObjectAnimator outerRotate = ObjectAnimator.ofFloat(
                    ringOuter, "rotation", 0f, 360f);
            outerRotate.setDuration(RING_OUTER_PERIOD);
            outerRotate.setRepeatCount(ValueAnimator.INFINITE);
            outerRotate.setInterpolator(new LinearInterpolator());
            outerRotate.start();
        }

        if (ringMiddle != null) {
            ObjectAnimator middleRotate = ObjectAnimator.ofFloat(
                    ringMiddle, "rotation", 0f, -360f);  // negative = CCW
            middleRotate.setDuration(RING_MIDDLE_PERIOD);
            middleRotate.setRepeatCount(ValueAnimator.INFINITE);
            middleRotate.setInterpolator(new LinearInterpolator());
            middleRotate.start();
        }
    }

    // ── ANIMATION 2: Particle Constellation Pulse ───────────────────────────

    /**
     * Each particle fades in from alpha=0 to a random peak (0.5–1.0)
     * then pulses (REVERSE repeat) to simulate star twinkling.
     * Staggered by 80ms per particle.
     */
    private void startParticlePulse() {
        float[] peakAlphas = {0.9f, 0.6f, 0.8f, 0.5f, 0.7f, 0.85f, 0.55f, 0.75f};
        for (int i = 0; i < particles.length; i++) {
            if (particles[i] == null) continue;
            final View p = particles[i];
            final float peak = peakAlphas[i];
            final int stagger = i * 80;

            p.postDelayed(() -> {
                ObjectAnimator pulse = ObjectAnimator.ofFloat(p, "alpha", 0f, peak);
                pulse.setDuration(PARTICLE_PULSE_PERIOD);
                pulse.setRepeatCount(ValueAnimator.INFINITE);
                pulse.setRepeatMode(ValueAnimator.REVERSE);
                pulse.setInterpolator(new AccelerateDecelerateInterpolator());
                pulse.start();
            }, stagger);
        }
    }

    // ── ANIMATION 3: Logo 3D Flip Reveal ────────────────────────────────────

    /**
     * Schedules the logo reveal after DELAY_LOGO ms.
     * Uses AnimatorSet to combine:
     *   - rotationY: 90° → 0°  (perspective flip on Y axis)
     *   - scaleX/Y : 0.3 → 1.0 (zoom in)
     *   - alpha    : 0   → 1   (fade in)
     * All three run in parallel. OvershootInterpolator gives a satisfying
     * micro-bounce at the end of the scale animation.
     */
    private void scheduleLogoReveal() {
        if (ivLogo == null) return;

        ivLogo.postDelayed(() -> {
            // 3D Y-flip: starts side-on (90°) flips to face-on (0°)
            ObjectAnimator flipY = ObjectAnimator.ofFloat(ivLogo, "rotationY", 90f, 0f);
            flipY.setDuration(DURATION_LOGO_FLIP);
            flipY.setInterpolator(new DecelerateInterpolator(2f));

            // Zoom: grows from 30% to 100% with a slight overshoot
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.3f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.3f, 1.0f);
            scaleX.setDuration(DURATION_LOGO_FLIP);
            scaleY.setDuration(DURATION_LOGO_FLIP);
            scaleX.setInterpolator(new OvershootInterpolator(1.2f));
            scaleY.setInterpolator(new OvershootInterpolator(1.2f));

            // Fade
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f);
            fadeIn.setDuration(DURATION_LOGO_FLIP / 2);
            fadeIn.setInterpolator(new DecelerateInterpolator());

            AnimatorSet logoSet = new AnimatorSet();
            logoSet.playTogether(flipY, scaleX, scaleY, fadeIn);
            logoSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Trigger subsequent animations once logo has landed
                    scheduleShimmer();
                    scheduleDivider();
                    scheduleTextReveal();
                    schedulePoweredBy();
                    scheduleNavigation();
                    splashReady = true; // release system splash overlay
                }
            });
            logoSet.start();
        }, DELAY_LOGO);
    }

    // ── ANIMATION 4: Gold Shimmer Sweep ─────────────────────────────────────

    /**
     * A semi-transparent white bar slides across the logo left → right,
     * simulating a gold sheen/reflection.
     * The bar starts hidden to the left (translationX = -logoWidth)
     * and exits to the right.
     */
    private void scheduleShimmer() {
        if (shimmerView == null) return;

        shimmerView.postDelayed(() -> {
            float logoWidth = ivLogo != null ? ivLogo.getWidth() : 420f;

            // Appear
            shimmerView.setAlpha(0.6f);

            // Slide from left-edge to right-edge of logo
            ObjectAnimator slide = ObjectAnimator.ofFloat(
                    shimmerView, "translationX", -logoWidth, logoWidth);
            slide.setDuration(DURATION_SHIMMER);
            slide.setInterpolator(new AccelerateDecelerateInterpolator());
            slide.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    shimmerView.setAlpha(0f); // hide after sweep
                }
            });
            slide.start();
        }, DELAY_SHIMMER);
    }

    // ── ANIMATION 5: Gold Divider Line ──────────────────────────────────────

    private void scheduleDivider() {
        if (dividerGold == null) return;

        dividerGold.postDelayed(() -> {
            dividerGold.setScaleX(0f);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(dividerGold, "scaleX", 0f, 1f);
            scaleX.setDuration(DURATION_DIVIDER);
            scaleX.setInterpolator(new DecelerateInterpolator(1.5f));

            ObjectAnimator alpha = ObjectAnimator.ofFloat(dividerGold, "alpha", 0f, 0.70f);
            alpha.setDuration(DURATION_DIVIDER);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(scaleX, alpha);
            set.start();
        }, DELAY_DIVIDER);
    }

    // ── ANIMATION 6: Text Reveal (App Name + Tagline) ───────────────────────

    private void scheduleTextReveal() {
        // App name — slide up + fade
        if (tvAppName != null) {
            tvAppName.postDelayed(() -> {
                ObjectAnimator slideUp = ObjectAnimator.ofFloat(
                        tvAppName, "translationY", 24f, 0f);
                slideUp.setDuration(DURATION_APP_NAME);
                slideUp.setInterpolator(new DecelerateInterpolator(2f));

                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(
                        tvAppName, "alpha", 0f, 1f);
                fadeIn.setDuration(DURATION_APP_NAME);

                AnimatorSet set = new AnimatorSet();
                set.playTogether(slideUp, fadeIn);
                set.start();
            }, DELAY_APP_NAME);
        }

        // Tagline — simple fade in
        if (tvTagline != null) {
            tvTagline.postDelayed(() -> {
                ObjectAnimator fade = ObjectAnimator.ofFloat(
                        tvTagline, "alpha", 0f, 0.80f);
                fade.setDuration(DURATION_TAGLINE);
                fade.start();
            }, DELAY_TAGLINE);
        }
    }

    // ── ANIMATION 7: "Powered by Night Tech" Footer ─────────────────────────

    private void schedulePoweredBy() {
        if (tvPoweredBy == null) return;

        tvPoweredBy.postDelayed(() -> {
            ObjectAnimator fade = ObjectAnimator.ofFloat(
                    tvPoweredBy, "alpha", 0f, 0.65f);
            fade.setDuration(DURATION_POWERED_BY);
            fade.start();
        }, DELAY_POWERED_BY);
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    private void scheduleNavigation() {
        if (ivLogo == null) return;

        ivLogo.postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            Intent intent = (currentUser != null)
                    ? new Intent(SplashActivity.this, MainActivity.class)
                    : new Intent(SplashActivity.this, LoginActivity.class);

            startActivity(intent);
            finish();
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);
        }, NAVIGATE_AFTER_MS);
    }
}