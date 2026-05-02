package com.nighttech.sajhapulse.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * SystemBarHelper — Centralized system bar (status + navigation) theming.
 *
 * SajhaPulse uses ONE universal bar style across ALL screens:
 *
 *   ALL screens (Splash, Login, Main, and all future screens)
 *     • Status bar : crimson (#DC143C), WHITE icons (battery, wifi, clock, etc.)
 *     • Nav bar    : crimson (#DC143C), WHITE gesture handle / buttons
 *     → call: applyAppBars(activity)
 *
 * Usage (in each Activity.onCreate(), after setContentView):
 *   SplashActivity  → SystemBarHelper.applyAppBars(this);
 *   LoginActivity   → SystemBarHelper.applyAppBars(this);
 *   MainActivity    → SystemBarHelper.applyAppBars(this);
 *   AnyFutureActivity → SystemBarHelper.applyAppBars(this);
 *
 * For inset padding on root views (to prevent content hiding behind bars):
 *   → call: SystemBarHelper.applyInsetPadding(rootView)
 *
 * NOTE for MainActivity: Do NOT additionally call applyInsetPadding() on
 * the AppBarLayout — AppBarLayout's own fitsSystemWindows="true" already
 * handles the status-bar inset and pads itself so the Toolbar sits below it.
 */
public final class SystemBarHelper {

    private SystemBarHelper() { /* utility class — no instances */ }

    // ── Theme colour (must match colors.xml) ────────────────────────────────

    /** @color/md_theme_primary / @color/sajha_splash_background = #DC143C */
    private static final int COLOR_CRIMSON = Color.parseColor("#DC143C");

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * UNIVERSAL bars — use on ALL screens (Splash, Login, Main, and future).
     *
     * Both the status bar and navigation bar are filled with the app's
     * primary crimson color so the battery, WiFi, clock, and gesture
     * handle icons are always WHITE and consistent across the entire app.
     *
     *   Status bar : #DC143C  → WHITE icons (battery, signal, clock, etc.)
     *   Nav bar    : #DC143C  → WHITE gesture handle / buttons
     */
    public static void applyAppBars(Activity activity) {
        Window window = activity.getWindow();
        makeEdgeToEdge(window);
        setStatusBarColor(window, COLOR_CRIMSON);
        setNavBarColor(window, COLOR_CRIMSON);
        applyBarAppearance(
                window,
                window.getDecorView(),
                false,   // lightStatusBar = false → WHITE status icons on crimson
                false);  // lightNavBar    = false → WHITE nav handle on crimson
    }

    /**
     * Applies WindowInsets padding (top + bottom) to a root view so that
     * content is not hidden behind the status bar or navigation bar.
     *
     * Use on the root view of: SplashActivity, LoginActivity, and any
     * future screens where the layout does NOT use AppBarLayout with
     * fitsSystemWindows="true".
     *
     * Do NOT use in MainActivity — AppBarLayout handles the top inset itself.
     */
    public static void applyInsetPadding(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int top    = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(
                    v.getPaddingLeft(),
                    top,
                    v.getPaddingRight(),
                    bottom);
            return insets;
        });
    }

    /**
     * Applies only the TOP (status-bar) inset as padding to a view.
     *
     * Optional helper for screens where the bottom inset is already handled
     * by a BottomNavigationView or similar component.
     */
    public static void applyTopInsetPadding(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(
                    v.getPaddingLeft(),
                    top,
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Opts the window into edge-to-edge so content can draw behind both
     * the status bar and navigation bar.
     */
    private static void makeEdgeToEdge(Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }

    private static void setStatusBarColor(Window window, @ColorInt int color) {
        window.setStatusBarColor(color);
    }

    private static void setNavBarColor(Window window, @ColorInt int color) {
        window.setNavigationBarColor(color);
        // Remove the thin divider line between content and nav bar on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }
    }

    /**
     * Controls whether status-bar and navigation-bar icons/handles appear
     * in their LIGHT (white) or DARK variant.
     *
     * @param lightStatusBar  true  → dark icons  (for light/white backgrounds)
     *                        false → white icons (for dark/crimson backgrounds)
     * @param lightNavBar     true  → dark handle (for light/white nav bar)
     *                        false → white handle (for dark/crimson nav bar)
     */
    private static void applyBarAppearance(
            Window window,
            View decorView,
            boolean lightStatusBar,
            boolean lightNavBar) {

        WindowInsetsControllerCompat ctrl =
                new WindowInsetsControllerCompat(window, decorView);
        ctrl.setAppearanceLightStatusBars(lightStatusBar);
        ctrl.setAppearanceLightNavigationBars(lightNavBar);
    }
}