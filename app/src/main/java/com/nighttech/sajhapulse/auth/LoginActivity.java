package com.nighttech.sajhapulse.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.nighttech.sajhapulse.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.nighttech.sajhapulse.ui.MainActivity;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

/**
 * LoginActivity — Authentication gateway for SajhaPulse.
 *
 * Provides two sign-in paths:
 *  1. Google Sign-In    — Full account with persistent data.
 *  2. Guest (Anonymous) — Quick access without an account.
 *
 * System bars:
 *   applyAppBars()     → crimson status bar + nav bar, white icons everywhere.
 *   applyInsetPadding() → called ONCE on root_login only. The layout uses
 *                         fitsSystemWindows="false" so we manage insets manually
 *                         and push content below the crimson status bar.
 *
 * ── Gradle dependencies required ─────────────────────────────────────────
 *   implementation 'com.google.firebase:firebase-auth:22.3.1'
 *   implementation 'com.google.android.gms:play-services-auth:21.0.0'
 *   implementation 'com.google.android.material:material:1.11.0'
 *
 * ── google-services.json ─────────────────────────────────────────────────
 *   Must be placed in /app. Enable Google Sign-In and Anonymous Auth in
 *   the Firebase Console → Authentication → Sign-in method.
 *
 * ── Manifest snippet ─────────────────────────────────────────────────────
 *   <activity
 *       android:name=".LoginActivity"
 *       android:windowSoftInputMode="adjustResize"
 *       android:exported="false" />
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "SajhaPulse:Login";

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;

    // ── Google Sign-In ────────────────────────────────────────────────────────
    private GoogleSignInClient mGoogleSignInClient;

    /**
     * ActivityResultLauncher replaces the deprecated startActivityForResult().
     * Registered here (not in onCreate) so it is always attached before any
     * Activity recreation.
     */
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleGoogleSignInResult(task);
                        }
                    }
            );

    // ── Views ─────────────────────────────────────────────────────────────────
    private MaterialButton btnGoogle;
    private MaterialButton btnGuest;
    private CircularProgressIndicator progressIndicator;
    private View cardLoginContainer;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // ① Apply crimson system bars (status bar + nav bar) with white icons.
        //    Must be called AFTER setContentView.
        SystemBarHelper.applyAppBars(this);

        // ② Apply inset padding to the root CoordinatorLayout ONLY.
        //    This pushes content below the crimson status bar and above the
        //    nav bar so nothing is hidden behind them.
        SystemBarHelper.applyInsetPadding(findViewById(R.id.root_login));

        initFirebase();
        initGoogleSignIn();
        bindViews();
        setClickListeners();
        playEntranceAnimation();
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void bindViews() {
        btnGoogle          = findViewById(R.id.btn_google_sign_in);
        btnGuest           = findViewById(R.id.btn_guest);
        progressIndicator  = findViewById(R.id.progress_login);
        cardLoginContainer = findViewById(R.id.card_login_container);
    }

    private void setClickListeners() {
        btnGoogle.setOnClickListener(v -> launchGoogleSignIn());
        btnGuest.setOnClickListener(v -> signInAnonymously());
    }

    // ── Entrance Animation ────────────────────────────────────────────────────

    /**
     * Staggers the header and login card into view.
     * Requires res/anim/fade_in.xml and res/anim/fade_in_up.xml.
     */
    private void playEntranceAnimation() {
        View headerGroup = findViewById(R.id.group_header);
        if (headerGroup != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            fadeIn.setStartOffset(100);
            headerGroup.startAnimation(fadeIn);
        }

        if (cardLoginContainer != null) {
            Animation fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
            fadeInUp.setStartOffset(300);
            cardLoginContainer.startAnimation(fadeInUp);
        }
    }

    // ── Google Sign-In Flow ───────────────────────────────────────────────────

    private void launchGoogleSignIn() {
        setLoadingState(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google sign-in failed: code=" + e.getStatusCode(), e);
            setLoadingState(false);
            showToast("Google sign-in failed. Please try again.");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Google sign-in success: " + (user != null ? user.getEmail() : "null"));
                        navigateToMain();
                    } else {
                        Log.e(TAG, "Firebase credential sign-in failed", task.getException());
                        showToast("Authentication failed. Please try again.");
                    }
                });
    }

    // ── Anonymous (Guest) Sign-In ─────────────────────────────────────────────

    private void signInAnonymously() {
        setLoadingState(true);

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous sign-in success");
                        navigateToMain();
                    } else {
                        Log.e(TAG, "Anonymous sign-in failed", task.getException());
                        showToast("Could not sign in as guest. Check your internet connection.");
                    }
                });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private void setLoadingState(boolean isLoading) {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnGoogle != null) btnGoogle.setEnabled(!isLoading);
        if (btnGuest  != null) btnGuest.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}