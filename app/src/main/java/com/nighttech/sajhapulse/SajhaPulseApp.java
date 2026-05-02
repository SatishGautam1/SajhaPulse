package com.nighttech.sajhapulse;

import android.app.Application;

import com.google.android.material.color.DynamicColors;
import com.google.firebase.FirebaseApp;

/**
 * SajhaPulseApp
 * ─────────────────────────────────────────────────────────────────────
 * Application class for SajhaPulse. Initialises Firebase and Material 3.
 *
 * BUG FIX: The original code called FirebaseDatabase.getInstance()
 * .setPersistenceEnabled(true). This app uses Cloud Firestore, NOT the
 * Firebase Realtime Database. Initialising Realtime Database is harmless
 * on devices that have Realtime Database in their google-services.json,
 * but throws IllegalStateException on clean installs / CI builds where
 * the Realtime Database URL is absent — causing an immediate crash before
 * SplashActivity even renders. Removed the Realtime Database call.
 *
 * Firestore offline persistence is enabled per-listener in DashboardActivity
 * via the default Firestore cache settings (enabled by default in SDK ≥ 10.x).
 * If explicit offline persistence is needed for Firestore, add:
 *   FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
 *       .setPersistenceEnabled(true).build();
 *   FirebaseFirestore.getInstance().setFirestoreSettings(settings);
 */
public class SajhaPulseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialise Firebase (required before any Firebase service is used)
        FirebaseApp.initializeApp(this);

        // 2. Apply Material 3 Dynamic Colors globally across all Activities
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}