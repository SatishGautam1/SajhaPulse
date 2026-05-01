package com.nighttech.sajhapulse.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/**
 * SplashAnimationManager
 * ─────────────────────────────────────────────────────────────────────
 * Utility class that encapsulates reusable ObjectAnimator sequences
 * used exclusively by SplashActivity.
 *
 * Extracted to keep SplashActivity lean and to allow independent testing.
 */
public final class SplashAnimationManager {

    // Prevent instantiation — utility class
    private SplashAnimationManager() {}

    /**
     * Animates a circular pulse ring:
     *  - Scales from 1f → maxScale
     *  - Fades from peakAlpha → 0f
     *  - Auto-repeats indefinitely
     *
     * @param ring        the View to animate (should be a circle drawable)
     * @param duration    total duration of one pulse cycle in ms
     * @param startDelay  delay before the animation starts in ms
     * @param maxScale    maximum scale factor (e.g. 1.6f)
     * @param peakAlpha   starting alpha value (e.g. 0.4f); ends at 0
     */
    public static void animatePulseRing(
            View ring,
            long duration,
            long startDelay,
            float maxScale,
            float peakAlpha) {

        ring.setScaleX(1f);
        ring.setScaleY(1f);
        ring.setAlpha(peakAlpha);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, View.SCALE_X, 1f, maxScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 1f, maxScale);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(ring, View.ALPHA,   peakAlpha, 0f);

        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY, alpha);
        pulse.setDuration(duration);
        pulse.setStartDelay(startDelay);
        pulse.setInterpolator(new AccelerateInterpolator(0.8f));

        // Repeat: reset scale/alpha and start again
        pulse.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ring.setScaleX(1f);
                ring.setScaleY(1f);
                ring.setAlpha(peakAlpha);
                pulse.setStartDelay(0L); // no delay on repeats
                pulse.start();
            }
        });

        pulse.start();
    }

    /**
     * Cancels all animations on the given view (scale + alpha).
     * Safe to call even if no animations are running.
     */
    public static void cancelAnimations(View... views) {
        for (View v : views) {
            v.animate().cancel();
            v.clearAnimation();
        }
    }
}