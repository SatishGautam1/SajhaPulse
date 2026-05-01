package com.nighttech.sajhapulse.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.sajhapulse.ui.MainActivity;
import com.nighttech.sajhapulse.R;

import java.util.HashMap;
import java.util.Map;

/**
 * LoginActivity
 * ─────────────────────────────────────────────────────────────────────
 * Handles two sign-in paths:
 *   1. Google One-Tap / GSI flow  →  Firebase credential exchange
 *   2. Guest (anonymous) sign-in  →  Firebase anonymous auth
 *
 * On success, creates/updates a user document in Firestore and
 * navigates to MainActivity.
 *
 * Dependencies (build.gradle :app):
 *   implementation 'com.google.firebase:firebase-auth'
 *   implementation 'com.google.firebase:firebase-firestore'
 *   implementation 'com.google.android.gms:play-services-auth:21.x.x'
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // ── Firebase ───────────────────────────────────────────────────
    private FirebaseAuth      mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;

    // ── Views ──────────────────────────────────────────────────────
    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnGuest;
    private View           loadingOverlay;

    // ── Google Sign-In Result Launcher (replaces deprecated onActivityResult) ──
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> handleGoogleSignInResult(result)
            );

    // ── Lifecycle ──────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth      = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        configureGoogleSignIn();
        bindViews();
        setClickListeners();
    }

    // ── Google Sign-In Configuration ──────────────────────────────

    /**
     * Builds a GoogleSignInOptions using the Web Client ID stored in
     * google-services.json (referenced via @string/default_web_client_id).
     * requestIdToken() is required for Firebase credential exchange.
     */
    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // ── View Binding ───────────────────────────────────────────────

    private void bindViews() {
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        btnGuest        = findViewById(R.id.btn_guest);
        loadingOverlay  = findViewById(R.id.login_loading_overlay);
    }

    private void setClickListeners() {
        btnGoogleSignIn.setOnClickListener(v -> launchGoogleSignIn());
        btnGuest.setOnClickListener(v -> signInAsGuest());
    }

    // ── Google Sign-In Flow ────────────────────────────────────────

    private void launchGoogleSignIn() {
        showLoading(true);
        // Sign out first to always show account picker
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    /**
     * Receives the result from Google's account picker.
     * Extracts the GoogleSignInAccount and exchanges it for a Firebase credential.
     */
    private void handleGoogleSignInResult(ActivityResult result) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed: " + e.getStatusCode(), e);
            showLoading(false);
            showToast("Google sign-in failed. Please try again.");
        }
    }

    /**
     * Exchanges a Google ID token for a Firebase credential and signs in.
     * On success: saves user to Firestore, navigates to MainActivity.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Firebase sign-in with Google success");
                    FirebaseUser user = authResult.getUser();
                    boolean isNew = authResult.getAdditionalUserInfo() != null
                            && authResult.getAdditionalUserInfo().isNewUser();
                    saveUserToFirestore(user, isNew, false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase sign-in with Google failed", e);
                    showLoading(false);
                    showToast("Authentication failed. Please try again.");
                });
    }

    // ── Guest / Anonymous Sign-In ──────────────────────────────────

    /**
     * Signs in anonymously via Firebase Anonymous Auth.
     * Guest sessions are tracked in Firestore with isGuest = true.
     */
    private void signInAsGuest() {
        showLoading(true);
        mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Anonymous sign-in success");
                    FirebaseUser user = authResult.getUser();
                    saveUserToFirestore(user, true, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Anonymous sign-in failed", e);
                    showLoading(false);
                    showToast("Guest sign-in failed. Check your connection.");
                });
    }

    // ── Firestore User Document ────────────────────────────────────

    /**
     * Creates or updates the user's document in:
     * Firestore → "users" collection → uid document
     *
     * Schema:
     *   uid        : String
     *   displayName: String
     *   email      : String (null for guests)
     *   photoUrl   : String (null for guests)
     *   isGuest    : boolean
     *   createdAt  : Server timestamp (set only on new users)
     *   lastLogin  : Server timestamp (always updated)
     */
    private void saveUserToFirestore(FirebaseUser user, boolean isNewUser, boolean isGuest) {
        if (user == null) {
            navigateToMain();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid",         user.getUid());
        userData.put("displayName", isGuest ? "Guest" : user.getDisplayName());
        userData.put("email",       isGuest ? null    : user.getEmail());
        userData.put("photoUrl",    (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null);
        userData.put("isGuest",     isGuest);
        userData.put("lastLogin",   com.google.firebase.Timestamp.now());

        if (isNewUser) {
            userData.put("createdAt",   com.google.firebase.Timestamp.now());
            userData.put("preferences", buildDefaultPreferences());
        }

        mFirestore.collection("users")
                .document(user.getUid())
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "User document saved/updated");
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    // Firestore save failed — still let the user in
                    Log.w(TAG, "Firestore user save failed, proceeding anyway", e);
                    navigateToMain();
                });
    }

    /** Returns a map of sensible default user preferences for new accounts. */
    private Map<String, Object> buildDefaultPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("baseCurrency",       "USD");
        prefs.put("notificationsEnabled", true);
        prefs.put("rateAlerts",          false);
        return prefs;
    }

    // ── Navigation ─────────────────────────────────────────────────

    private void navigateToMain() {
        showLoading(false);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── UI Helpers ─────────────────────────────────────────────────

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGoogleSignIn.setEnabled(!show);
        btnGuest.setEnabled(!show);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}