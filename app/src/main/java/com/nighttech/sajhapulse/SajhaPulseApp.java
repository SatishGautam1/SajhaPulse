package com.nighttech.sajhapulse;

import android.app.Application;
import com.google.android.material.color.DynamicColors;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Main Application class for SajhaPulse.
 * This class initializes global configurations for the Night Tech ecosystem.
 */
public class SajhaPulseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Firebase Services
        // This ensures Firebase is ready before SplashActivity starts.
        FirebaseApp.initializeApp(this);

        // 2. Enable Offline Persistence for Realtime Database
        // High-utility for the Nepali market where internet connectivity can fluctuate.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // 3. Apply Material 3 Dynamic Colors
        // This applies your professional theme and Inter font family globally.
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}