package com.nighttech.sajhapulse.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.nighttech.sajhapulse.utils.SystemBarHelper;

/**
 * BaseActivity — inherited by every Activity in SajhaPulse.
 *
 * Responsibilities:
 *   1. Forces Night-Tech palette (MODE_NIGHT_NO) before super.onCreate()
 *      so the theme is resolved before layout inflation.
 *   2. Calls SystemBarHelper.applyAppBars() after setContentView()
 *      so the fixed status bar (#060F1C) and nav bar (#0C1B2E) are
 *      applied consistently on every screen without each Activity
 *      needing to remember to call it.
 *
 * Usage — replace "extends AppCompatActivity" with "extends BaseActivity":
 *
 *   public class MainActivity     extends BaseActivity { ... }
 *   public class SplashActivity   extends BaseActivity { ... }
 *   public class LoginActivity    extends BaseActivity { ... }  // in auth/
 *   public class MarketsActivity  extends BaseActivity { ... }
 *
 * Subclass contract:
 *   • Call super.onCreate(savedInstanceState) FIRST.
 *   • Call setContentView(R.layout.your_layout) IMMEDIATELY after super.
 *   • Do NOT call SystemBarHelper.applyAppBars(this) — BaseActivity does it.
 *   • Do NOT call AppCompatDelegate.setDefaultNightMode() — BaseActivity does it.
 *
 *   @Override
 *   protected void onCreate(Bundle savedInstanceState) {
 *       super.onCreate(savedInstanceState);          // ← BaseActivity runs here
 *       setContentView(R.layout.activity_main);      // ← MUST be next line
 *       // your own setup below
 *   }
 *
 * Screens that need inset padding on their root view (Splash, Login):
 *   Call SystemBarHelper.applyInsetPadding(rootView) in their own onCreate()
 *   AFTER setContentView(). Do NOT call it in MainActivity (AppBarLayout
 *   handles the top inset; NestedScrollView paddingBottom handles bottom).
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Force Night-Tech palette before layout inflation.
        // Must be before super.onCreate() so the correct theme is used
        // when the layout inflater resolves style references.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
    }

    /**
     * Called by BaseActivity automatically after your setContentView().
     * Subclasses do NOT need to call this — it is invoked via
     * onPostCreate() which fires after setContentView() completes.
     *
     * Status bar  = #060F1C  (toolbar_background) → WHITE icons
     * Nav bar     = #0C1B2E  (surface)             → WHITE icons
     * Both bars are FIXED — they do not change on scroll or interaction.
     */
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Applied after setContentView() so the window is fully initialized.
        SystemBarHelper.applyAppBars(this);
    }
}