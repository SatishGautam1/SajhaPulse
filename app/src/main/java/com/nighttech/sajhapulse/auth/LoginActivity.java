package com.nighttech.sajhapulse.auth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
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
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth      mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;

    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnGuest;
    private View           loadingOverlay;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleGoogleSignInResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        applySystemBarStyling();

        mAuth      = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        configureGoogleSignIn();
        bindViews();
        setClickListeners();
    }

    // ── System Bar Styling ─────────────────────────────────────────

    /**
     * Ensures the status bar and navigation bar have white (light) icons
     * to match the app's forced dark theme.
     */
    private void applySystemBarStyling() {
        Window window = getWindow();

        window.setStatusBarColor(getResources().getColor(R.color.background, getTheme()));
        window.setNavigationBarColor(getResources().getColor(R.color.background, getTheme()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
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

    // ── Google Sign-In Configuration ──────────────────────────────

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
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                showLoading(false);
                showToast("Google sign-in cancelled.");
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed: " + e.getStatusCode(), e);
            showLoading(false);
            showToast("Google sign-in failed. Please try again.");
        }
    }

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
     * Creates or updates the user document in Firestore.
     * Uses SetOptions.merge() so existing fields are preserved.
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
                    Log.w(TAG, "Firestore user save failed, proceeding anyway", e);
                    navigateToMain();
                });
    }

    private Map<String, Object> buildDefaultPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("baseCurrency",         "NPR");
        prefs.put("notificationsEnabled", true);
        prefs.put("rateAlerts",           false);
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
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnGoogleSignIn != null) btnGoogleSignIn.setEnabled(!show);
        if (btnGuest != null)        btnGuest.setEnabled(!show);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}