package com.nighttech.sajhapulse.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nighttech.sajhapulse.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MetalsFragment — Tab 2 in MarketsActivity.
 *
 * Shows today's Gold (24K & 22K) and Silver prices per tola in NPR,
 * as published by the Federation of Nepal Gold & Silver Dealers'
 * Association (NEGOSIDA).
 *
 * ── Data Source ──────────────────────────────────────────────────────
 *   NEGOSIDA does not expose a public REST API. The current
 *   implementation uses curated static values that represent typical
 *   mid-2026 rates. Two integration options for production:
 *
 *   Option A — Screen-scrape https://www.fenegosida.org/goldsilverrate
 *              using Jsoup (add to build.gradle: implementation 'org.jsoup:jsoup:1.17.2')
 *
 *   Option B — Push real-time prices from your own backend to Firestore
 *              collection "metal_prices" and read them here.
 *
 * ── UI ───────────────────────────────────────────────────────────────
 *   Three crisp price cards:
 *     • Gold 24K/tola  (primary, large)
 *     • Gold 22K/tola  (secondary)
 *     • Silver/tola    (muted silver tone)
 *   Each card shows: current price · change from yesterday · % change
 */
public class MetalsFragment extends Fragment {

    // Gold 24K
    private TextView tvGold24Price, tvGold24Change, tvGold24Pct, tvGold24Arrow;
    // Gold 22K
    private TextView tvGold22Price, tvGold22Change, tvGold22Pct, tvGold22Arrow;
    // Silver
    private TextView tvSilverPrice, tvSilverChange, tvSilverPct, tvSilverArrow;
    // Common
    private TextView tvMetalsDate;
    private View     progressBar, tvError;

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_metals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        loadMetalPrices();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    // ── View binding ──────────────────────────────────────────────────

    private void bindViews(View root) {
        tvGold24Price  = root.findViewById(R.id.tv_gold24_price);
        tvGold24Change = root.findViewById(R.id.tv_gold24_change);
        tvGold24Pct    = root.findViewById(R.id.tv_gold24_pct);
        tvGold24Arrow  = root.findViewById(R.id.tv_gold24_arrow);

        tvGold22Price  = root.findViewById(R.id.tv_gold22_price);
        tvGold22Change = root.findViewById(R.id.tv_gold22_change);
        tvGold22Pct    = root.findViewById(R.id.tv_gold22_pct);
        tvGold22Arrow  = root.findViewById(R.id.tv_gold22_arrow);

        tvSilverPrice  = root.findViewById(R.id.tv_silver_price);
        tvSilverChange = root.findViewById(R.id.tv_silver_change);
        tvSilverPct    = root.findViewById(R.id.tv_silver_pct);
        tvSilverArrow  = root.findViewById(R.id.tv_silver_arrow);

        tvMetalsDate   = root.findViewById(R.id.tv_metals_date);
        progressBar    = root.findViewById(R.id.metals_progress);
    }

    // ── Data loading ──────────────────────────────────────────────────

    private void loadMetalPrices() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            // TODO: Replace with a real Jsoup scrape or Firestore fetch.
            // Simulated 200 ms fetch.
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            // Typical NEGOSIDA mid-2026 published values (NPR per tola)
            final double gold24     = 138_400;
            final double gold24Prev = 138_000;
            final double gold22     = 126_950;
            final double gold22Prev = 126_580;
            final double silver     =   1_620;
            final double silverPrev =   1_600;

            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                bindMetalCard(tvGold24Price, tvGold24Change, tvGold24Pct, tvGold24Arrow,
                        gold24, gold24Prev, "₹");
                bindMetalCard(tvGold22Price, tvGold22Change, tvGold22Pct, tvGold22Arrow,
                        gold22, gold22Prev, "₹");
                bindMetalCard(tvSilverPrice, tvSilverChange, tvSilverPct, tvSilverArrow,
                        silver, silverPrev, "₹");

                if (tvMetalsDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            "d MMMM yyyy", Locale.ENGLISH);
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
                    tvMetalsDate.setText(
                            "NEGOSIDA — " + sdf.format(
                                    Calendar.getInstance().getTime()));
                }
            });
        });
    }

    // ── Card binding helper ───────────────────────────────────────────

    private void bindMetalCard(
            TextView tvPrice, TextView tvChange, TextView tvPct, TextView tvArrow,
            double current, double previous, String symbol) {

        double diff = current - previous;
        double pct  = previous > 0 ? (diff / previous) * 100.0 : 0;
        boolean isUp = diff >= 0;

        String color = isUp
                ? "#FFD700"   // gold for gains (works on crimson bg)
                : "#FF5252";  // red for losses

        if (tvPrice  != null)
            tvPrice.setText(symbol + " " + formatPrice(current));
        if (tvChange != null) {
            tvChange.setText(String.format(Locale.ENGLISH,
                    "%s %.0f", isUp ? "+" : "", diff));
            tvChange.setTextColor(Color.parseColor(color));
        }
        if (tvPct != null) {
            tvPct.setText(String.format(Locale.ENGLISH,
                    "(%.2f%%)", Math.abs(pct)));
            tvPct.setTextColor(Color.parseColor(color));
        }
        if (tvArrow != null) {
            tvArrow.setText(isUp ? "▲" : "▼");
            tvArrow.setTextColor(Color.parseColor(color));
        }
    }

    /** Formats a large price with commas: 138400 → "1,38,400" (Indian system). */
    private String formatPrice(double val) {
        long v = (long) val;
        // Indian grouping: last 3 digits, then groups of 2
        String s = String.valueOf(v);
        if (s.length() <= 3) return s;
        String last3 = s.substring(s.length() - 3);
        String rest   = s.substring(0, s.length() - 3);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = rest.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 2 == 0) sb.insert(0, ',');
            sb.insert(0, rest.charAt(i));
            count++;
        }
        return sb.append(',').append(last3).toString();
    }
}