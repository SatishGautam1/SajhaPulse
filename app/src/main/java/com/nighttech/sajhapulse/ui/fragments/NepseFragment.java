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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.StockItem;
import com.nighttech.sajhapulse.ui.adapters.NepseStockAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * NepseFragment — Tab 0 in MarketsActivity.
 *
 * ── UI Sections ──────────────────────────────────────────────────────
 *   • Index Hero Card : NEPSE value, point/% change, market status,
 *                       52-week high/low, today's turnover
 *   • Chip Filter     : All | Gainers | Losers
 *   • Stock List      : RecyclerView via NepseStockAdapter
 *
 * ── Data Source ──────────────────────────────────────────────────────
 *   Currently using static sample data that accurately mirrors typical
 *   NEPSE trading day values.
 *
 *   TODO: Replace buildSampleStocks() with a Retrofit call to the
 *   official NEPSE API:
 *     https://www.nepalstock.com.np/api/nots/nepse-data/today-price
 *   Add okhttp3 / Retrofit + GsonConverterFactory to build.gradle and
 *   deserialise the response into List<StockItem>.
 *
 * ── Refresh ──────────────────────────────────────────────────────────
 *   Auto-refreshes every 60 s while the fragment is resumed.
 *   The Handler is removed in onPause() to avoid leaks.
 */
public class NepseFragment extends Fragment {

    // Hero card views
    private TextView tvIndexValue, tvIndexChange, tvIndexPct;
    private TextView tvMarketStatus, tvTurnover, tvTotalTrades;
    private TextView tv52High, tv52Low;
    private View     viewChangeBar;

    // Filter + list
    private ChipGroup  chipGroup;
    private Chip       chipAll, chipGainers, chipLosers;
    private RecyclerView      rvStocks;
    private NepseStockAdapter adapter;

    // Loading / error
    private View     progressBar;
    private TextView tvError;

    private final List<StockItem> allStocks    = new ArrayList<>();
    private final Handler         refreshHandler = new Handler(Looper.getMainLooper());
    private       Runnable        refreshRunnable;

    // ── Fragment lifecycle ────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nepse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRecyclerView();
        setupChipFilter();
        loadData();
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // ── View binding ──────────────────────────────────────────────────

    private void bindViews(View root) {
        tvIndexValue  = root.findViewById(R.id.tv_nepse_index_value);
        tvIndexChange = root.findViewById(R.id.tv_nepse_index_change);
        tvIndexPct    = root.findViewById(R.id.tv_nepse_index_pct);
        tvMarketStatus = root.findViewById(R.id.tv_nepse_market_status);
        tvTurnover    = root.findViewById(R.id.tv_nepse_turnover);
        tvTotalTrades = root.findViewById(R.id.tv_nepse_total_trades);
        tv52High      = root.findViewById(R.id.tv_nepse_52h);
        tv52Low       = root.findViewById(R.id.tv_nepse_52l);
        viewChangeBar = root.findViewById(R.id.view_nepse_change_bar);

        chipGroup   = root.findViewById(R.id.chip_group_filter);
        chipAll     = root.findViewById(R.id.chip_all);
        chipGainers = root.findViewById(R.id.chip_gainers);
        chipLosers  = root.findViewById(R.id.chip_losers);

        rvStocks    = root.findViewById(R.id.rv_nepse_stocks);
        progressBar = root.findViewById(R.id.nepse_progress);
        tvError     = root.findViewById(R.id.tv_nepse_error);
    }

    // ── RecyclerView ──────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new NepseStockAdapter();
        rvStocks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStocks.setAdapter(adapter);
        rvStocks.setNestedScrollingEnabled(false);
    }

    // ── Chip filter ───────────────────────────────────────────────────

    private void setupChipFilter() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
    }

    private void applyFilter() {
        List<StockItem> filtered = new ArrayList<>();
        int checkedId = chipGroup.getCheckedChipId();

        if (checkedId == R.id.chip_gainers) {
            for (StockItem s : allStocks) if (s.isGainer()) filtered.add(s);
        } else if (checkedId == R.id.chip_losers) {
            for (StockItem s : allStocks) if (s.isLoser())  filtered.add(s);
        } else {
            filtered.addAll(allStocks);
        }
        adapter.submitList(filtered);
    }

    // ── Data loading ──────────────────────────────────────────────────

    /**
     * Entry point for data loading.
     * In production: replace buildSampleStocks() with a Retrofit async call.
     */
    private void loadData() {
        showLoading(true);

        // Simulate network delay on a background thread
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;

            allStocks.clear();
            allStocks.addAll(buildSampleStocks());
            bindIndexHero();
            applyFilter();
            showLoading(false);
        }, 600);
    }

    private void startAutoRefresh() {
        if (refreshRunnable == null) {
            refreshRunnable = new Runnable() {
                @Override public void run() {
                    if (isAdded()) {
                        loadData();
                        refreshHandler.postDelayed(this, 60_000);
                    }
                }
            };
        }
        refreshHandler.postDelayed(refreshRunnable, 60_000);
    }

    // ── Hero card binding ─────────────────────────────────────────────

    private void bindIndexHero() {
        // Sample NEPSE index values (replace with API response)
        double indexVal  = 2_045.63;
        double change    =    +24.18;
        double changePct =     +1.20;
        boolean isUp = changePct >= 0;

        if (tvIndexValue != null)
            tvIndexValue.setText(String.format(Locale.ENGLISH, "%,.2f", indexVal));

        if (tvIndexChange != null) {
            String arrow = isUp ? "▲" : "▼";
            tvIndexChange.setText(String.format(Locale.ENGLISH,
                    "%s %.2f", arrow, Math.abs(change)));
            tvIndexChange.setTextColor(isUp
                    ? Color.parseColor("#4CAF50")
                    : Color.parseColor("#FF5252"));
        }

        if (tvIndexPct != null) {
            tvIndexPct.setText(String.format(Locale.ENGLISH,
                    "(%.2f%%)", Math.abs(changePct)));
            tvIndexPct.setTextColor(isUp
                    ? Color.parseColor("#4CAF50")
                    : Color.parseColor("#FF5252"));
        }

        // Colour the change bar strip
        if (viewChangeBar != null) {
            viewChangeBar.setBackgroundColor(isUp
                    ? Color.parseColor("#4CAF50")
                    : Color.parseColor("#FF5252"));
        }

        // Market status
        if (tvMarketStatus != null) {
            boolean open = isMarketOpen();
            tvMarketStatus.setText(open ? "● OPEN" : "● CLOSED");
            tvMarketStatus.setTextColor(open
                    ? Color.parseColor("#4CAF50")
                    : Color.parseColor("#FF5252"));
        }

        if (tvTurnover     != null) tvTurnover.setText("₹ 4.2 Billion");
        if (tvTotalTrades  != null) tvTotalTrades.setText("142,880");
        if (tv52High       != null) tv52High.setText("2,284.36");
        if (tv52Low        != null) tv52Low.setText("1,702.11");
    }

    /** NEPSE trading hours: Sun–Thu 11:00–15:00 NST. */
    private boolean isMarketOpen() {
        java.util.Calendar cal = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("Asia/Kathmandu"));
        int dow  = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int min  = cal.get(java.util.Calendar.MINUTE);
        // Sunday=1, Monday=2, ... Thursday=5  (Java Calendar)
        boolean tradingDay = (dow >= 1 && dow <= 5);
        boolean tradingTime = (hour > 11 || (hour == 11 && min >= 0))
                && (hour < 15 || (hour == 15 && min == 0));
        return tradingDay && tradingTime;
    }

    // ── Loading / error states ────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (rvStocks    != null) rvStocks.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (tvError     != null) tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (rvStocks    != null) rvStocks.setVisibility(View.GONE);
        if (tvError     != null) { tvError.setText(msg); tvError.setVisibility(View.VISIBLE); }
    }

    // ── Sample data ───────────────────────────────────────────────────

    /**
     * Returns a realistic snapshot of NEPSE-listed stocks.
     * TODO: Replace with a Retrofit call to the NEPSE API:
     *   GET https://www.nepalstock.com.np/api/nots/nepse-data/today-price
     */
    private List<StockItem> buildSampleStocks() {
        List<StockItem> list = new ArrayList<>();
        list.add(new StockItem("NABIL",  "Nabil Bank Limited",               1_502.00,  +52.00, +3.59,  28_430, 1_510.00, 1_450.00, 1_450.00, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("NICA",   "NIC Asia Bank Limited",              890.50,  +18.50, +2.12,  42_110,   895.00,   870.00,   872.00, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("SBI",    "Nepal SBI Bank Ltd",                 248.00,   -4.00, -1.59,  61_200,   253.00,   246.00,   252.00, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("EBL",    "Everest Bank Limited",             1_100.00,  +30.00, +2.80,  15_600, 1_105.00, 1_068.00, 1_070.00, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("KBL",    "Kumari Bank Limited",               310.00,   -6.10, -1.93,  88_900,   318.00,   308.00,   316.10, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("UPPER",  "Upper Tamakoshi Hydro",             280.00,  +14.00, +5.26,  72_300,   282.00,   264.00,   266.00, StockItem.Sector.HYDRO));
        list.add(new StockItem("BPCL",   "Butwal Power Company",              445.00,  -12.00, -2.63,  33_100,   460.00,   444.00,   457.00, StockItem.Sector.HYDRO));
        list.add(new StockItem("NHPC",   "National Hydro Power",              270.00,   +7.00, +2.66,  41_000,   271.00,   261.00,   263.00, StockItem.Sector.HYDRO));
        list.add(new StockItem("NLICL",  "Nepal Life Insurance",            2_050.00,  +70.00, +3.54,   9_800, 2_060.00, 1_975.00, 1_980.00, StockItem.Sector.INSURANCE));
        list.add(new StockItem("LICN",   "Life Insurance Corp Nepal",         820.00,  -15.00, -1.80,  19_200,   838.00,   818.00,   835.00, StockItem.Sector.INSURANCE));
        list.add(new StockItem("GBIME",  "Global IME Bank",                   300.00,   +9.00, +3.09,  95_500,   302.00,   289.00,   291.00, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("MBL",    "Machhapuchhre Bank",                320.00,   -5.00, -1.54,  54_300,   327.00,   318.00,   325.00, StockItem.Sector.COMMERCIAL_BANK));
        list.add(new StockItem("SHIVM",  "Shivam Cement",                     320.00,  +16.00, +5.26,  22_100,   321.00,   302.00,   304.00, StockItem.Sector.MANUFACTURING));
        list.add(new StockItem("NIFRA",  "Nepal Infrastructure Bank",         155.00,   -3.00, -1.90,  66_700,   160.00,   153.00,   158.00, StockItem.Sector.DEVELOPMENT_BANK));
        list.add(new StockItem("PCBL",   "Prime Commercial Bank",             268.00,   +8.00, +3.08, 108_400,   270.00,   258.00,   260.00, StockItem.Sector.COMMERCIAL_BANK));
        return list;
    }
}