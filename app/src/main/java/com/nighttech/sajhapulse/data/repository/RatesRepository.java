package com.nighttech.sajhapulse.data.repository;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * RatesRepository — Firestore data layer for SajhaPulse rate data.
 *
 * Package: data/repository/
 *
 * Wraps the Firestore real-time listener for the "rates/latest" document.
 * Activities and Fragments should use this class instead of talking to
 * Firestore directly. This keeps Firebase calls out of the UI layer.
 *
 * Firestore document path:  rates/latest
 *
 * Expected schema:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  FOREX (selling rates, NPR per 1 unit of foreign currency):      │
 * │    usdNpr: 134.42     eurNpr: 146.50     gbpNpr: 171.20          │
 * │    inrNpr: 1.610      audNpr: 88.30      cadNpr: 99.10           │
 * │    chfNpr: 152.00     cnyNpr: 18.50      jpyNpr: 0.892           │
 * │    sgdNpr: 100.20     qarNpr: 36.90      aedNpr: 36.60           │
 * │    usdChange: +0.12   (% change from previous day)               │
 * │    usdBuy: 133.95     (buying rate for USD)                      │
 * │                                                                   │
 * │  BULLION (FENEGOSIDA, price per Tola in NPR):                    │
 * │    gold24k: 152300    gold22k: 148800    silverTola: 1850         │
 * │                                                                   │
 * │  FUEL (NOC, price per litre/cylinder in NPR):                    │
 * │    petrol: 178        diesel: 163                                 │
 * │    lpg: 1375          kerosene: 163                               │
 * │                                                                   │
 * │  NEPSE:                                                           │
 * │    nepseIndex: 2184.62    nepseChange: +12.45                    │
 * │    nepseTurnover: "4.2"  nepseVolume: "1.2M"                     │
 * │    nepseTransactions: "3210"   marketOpen: false                  │
 * │                                                                   │
 * │  METADATA:                                                        │
 * │    updatedAt: Timestamp    tithi: "Sukla Panchami"               │
 * │    newsHeadline: "..."     newsSource: "..."   festival: "..."    │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Usage in an Activity (call in onStart / onStop):
 *
 *   private RatesRepository ratesRepo;
 *
 *   protected void onStart() {
 *       super.onStart();
 *       ratesRepo = new RatesRepository();
 *       ratesRepo.startListening(snapshot -> {
 *           double usd = snapshot.getDouble("usdNpr");
 *           // update UI
 *       }, error -> Log.e(TAG, "Rates error", error));
 *   }
 *
 *   protected void onStop() {
 *       super.onStop();
 *       if (ratesRepo != null) ratesRepo.stopListening();
 *   }
 */
public class RatesRepository {

    private static final String TAG            = "RatesRepository";
    private static final String COLLECTION     = "rates";
    private static final String DOCUMENT       = "latest";

    private final FirebaseFirestore   db;
    private       ListenerRegistration listener;

    public RatesRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Listener callbacks ────────────────────────────────────────────

    public interface OnRatesUpdated {
        void onUpdate(DocumentSnapshot snapshot);
    }

    public interface OnRatesError {
        void onError(Exception error);

        void accept(FirebaseFirestoreException error);
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Attach a real-time Firestore listener. Fires immediately with
     * cached data (if any), then again whenever the document changes.
     *
     * Call from Activity.onStart(). Pair with stopListening() in onStop().
     */
    public void startListening(OnRatesUpdated onUpdate, OnRatesError onError) {
        if (listener != null) return; // already attached

        listener = db.collection(COLLECTION)
                .document(DOCUMENT)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error", error);
                        if (onError != null) onError.accept(error);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        if (onUpdate != null) onUpdate.onUpdate(snapshot);
                    } else {
                        Log.w(TAG, "rates/latest document does not exist yet");
                    }
                });
    }

    /**
     * Detach the Firestore listener. Call from Activity.onStop() to
     * stop receiving updates and free the network connection.
     */
    public void stopListening() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    /**
     * Convenience: fetch the document once (no real-time updates).
     * Useful for less critical screens that only need a snapshot.
     */
    public void fetchOnce(OnRatesUpdated onUpdate, OnRatesError onError) {
        db.collection(COLLECTION)
                .document(DOCUMENT)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && onUpdate != null) {
                        onUpdate.onUpdate(snapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchOnce error", e);
                    if (onError != null) onError.accept((FirebaseFirestoreException) e);
                });
    }
}