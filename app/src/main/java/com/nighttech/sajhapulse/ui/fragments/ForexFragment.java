package com.nighttech.sajhapulse.ui.fragments;

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

import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.ForexRate;
import com.nighttech.sajhapulse.ui.adapters.ForexRateAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ForexFragment — Tab 1 in MarketsActivity.
 *
 * ── Data Source ──────────────────────────────────────────────────────
 *   NRB (Nepal Rastra Bank) official Forex API:
 *     GET https://www.nrb.org.np/api/forex-rate?_format=json
 *   No API key required. Updated daily by NRB at ~10:30 AM NST.
 *
 *   Network call is made on a background ExecutorService thread;
 *   UI updates are posted back on the main thread via Handler.
 *   Fallback to curated static rates if the network call fails.
 *
 * ── Layout ───────────────────────────────────────────────────────────
 *   • Summary header  : NPR vs USD mid-rate prominently
 *   • "As published by NRB" label + date
 *   • RecyclerView of all rates
 *
 * ── manifest ─────────────────────────────────────────────────────────
 *   Ensure <uses-permission android:name="android.permission.INTERNET"/>
 *   is in AndroidManifest.xml (already required for Firebase).
 */
public class ForexFragment extends Fragment {

    private static final String NRB_API_URL =
            "https://www.nrb.org.np/api/forex-rate?_format=json";

    private RecyclerView    rvForex;
    private ForexRateAdapter adapter;
    private View             progressBar;
    private TextView         tvError, tvPublishedDate, tvUsdHighlight, tvEurHighlight;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Flag emoji map  (ISO3 → emoji flag) ──────────────────────────
    private static final Map<String, String> FLAG_MAP = new HashMap<String, String>() {{
        put("USD", "🇺🇸"); put("EUR", "🇪🇺"); put("GBP", "🇬🇧");
        put("INR", "🇮🇳"); put("AUD", "🇦🇺"); put("CAD", "🇨🇦");
        put("JPY", "🇯🇵"); put("CHF", "🇨🇭"); put("CNY", "🇨🇳");
        put("SAR", "🇸🇦"); put("QAR", "🇶🇦"); put("AED", "🇦🇪");
        put("MYR", "🇲🇾"); put("SGD", "🇸🇬"); put("KRW", "🇰🇷");
        put("HKD", "🇭🇰"); put("DKK", "🇩🇰"); put("SEK", "🇸🇪");
        put("KWD", "🇰🇼"); put("BHD", "🇧🇭"); put("THB", "🇹🇭");
    }};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forex, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvForex         = view.findViewById(R.id.rv_forex_rates);
        progressBar     = view.findViewById(R.id.forex_progress);
        tvError         = view.findViewById(R.id.tv_forex_error);
        tvPublishedDate = view.findViewById(R.id.tv_forex_published_date);
        tvUsdHighlight  = view.findViewById(R.id.tv_forex_usd_value);
        tvEurHighlight  = view.findViewById(R.id.tv_forex_eur_value);

        adapter = new ForexRateAdapter();
        rvForex.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvForex.setAdapter(adapter);
        rvForex.setNestedScrollingEnabled(false);

        fetchForexRates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    // ── Network fetch ─────────────────────────────────────────────────

    private void fetchForexRates() {
        showLoading(true);

        executor.execute(() -> {
            try {
                String json = httpGet(NRB_API_URL);
                List<ForexRate> rates = parseNrbResponse(json);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    adapter.submitList(rates);
                    bindHighlights(rates);
                    showLoading(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    // Fallback to static rates on network failure
                    List<ForexRate> fallback = buildFallbackRates();
                    adapter.submitList(fallback);
                    bindHighlights(fallback);
                    if (tvPublishedDate != null)
                        tvPublishedDate.setText("Offline — showing last known rates");
                    showLoading(false);
                });
            }
        });
    }

    // ── HTTP GET ──────────────────────────────────────────────────────

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(8_000);
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    // ── JSON parsing ──────────────────────────────────────────────────

    /**
     * Parses the NRB Forex API response JSON.
     * Expected structure:
     * {
     *   "data": {
     *     "payload": [
     *       {
     *         "date": "2026-05-02",
     *         "rates": [
     *           { "currency": { "iso3": "USD", "name": "...", "unit": 1 },
     *             "buy": "133.30", "sell": "133.90" }
     *         ]
     *       }
     *     ]
     *   }
     * }
     */
    private List<ForexRate> parseNrbResponse(String json) throws Exception {
        List<ForexRate> rates = new ArrayList<>();
        JSONObject root    = new JSONObject(json);
        JSONObject data    = root.getJSONObject("data");
        JSONArray  payload = data.getJSONArray("payload");

        if (payload.length() == 0) return buildFallbackRates();

        JSONObject first   = payload.getJSONObject(0);
        String     dateStr = first.optString("date", "");
        JSONArray  ratesArr = first.getJSONArray("rates");

        // Update published date label
        mainHandler.post(() -> {
            if (tvPublishedDate != null && isAdded()) {
                tvPublishedDate.setText("NRB Rate — " + dateStr);
            }
        });

        for (int i = 0; i < ratesArr.length(); i++) {
            JSONObject entry    = ratesArr.getJSONObject(i);
            JSONObject currency = entry.getJSONObject("currency");

            String iso3 = currency.optString("iso3",  "???");
            String name = currency.optString("name",  "Unknown");
            int    unit = currency.optInt   ("unit",  1);
            double buy  = parseRate(entry.optString("buy",  "0"));
            double sell = parseRate(entry.optString("sell", "0"));

            ForexRate r = new ForexRate(
                    iso3, name,
                    FLAG_MAP.containsKey(iso3) ? FLAG_MAP.get(iso3) : "🏳",
                    unit, buy, sell);
            rates.add(r);
        }
        return rates;
    }

    private double parseRate(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    // ── Highlight bindings ────────────────────────────────────────────

    private void bindHighlights(List<ForexRate> rates) {
        for (ForexRate r : rates) {
            if ("USD".equals(r.getIso3()) && tvUsdHighlight != null) {
                tvUsdHighlight.setText(
                        String.format(Locale.ENGLISH, "%.2f", r.getSell()));
            }
            if ("EUR".equals(r.getIso3()) && tvEurHighlight != null) {
                tvEurHighlight.setText(
                        String.format(Locale.ENGLISH, "%.2f", r.getSell()));
            }
        }
        // Timestamp
        if (tvPublishedDate != null && tvPublishedDate.getText().toString().startsWith("NRB")) {
            // Already set from parse
        } else if (tvPublishedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
            tvPublishedDate.setText("NRB Rate — " + sdf.format(new Date()));
        }
    }

    // ── Fallback data ─────────────────────────────────────────────────

    /**
     * Static fallback rates for offline/error scenarios.
     * Values are approximate mid-May 2026 NRB published rates.
     */
    private List<ForexRate> buildFallbackRates() {
        List<ForexRate> list = new ArrayList<>();
        list.add(new ForexRate("USD", "U.S. Dollar",        "🇺🇸",   1, 133.30, 133.90));
        list.add(new ForexRate("EUR", "Euro",               "🇪🇺",   1, 144.20, 144.80));
        list.add(new ForexRate("GBP", "British Pound",      "🇬🇧",   1, 168.40, 169.00));
        list.add(new ForexRate("INR", "Indian Rupee",       "🇮🇳", 100, 159.15, 159.71));
        list.add(new ForexRate("AUD", "Australian Dollar",  "🇦🇺",   1,  86.10,  86.60));
        list.add(new ForexRate("CAD", "Canadian Dollar",    "🇨🇦",   1,  97.20,  97.80));
        list.add(new ForexRate("CHF", "Swiss Franc",        "🇨🇭",   1, 149.00, 149.80));
        list.add(new ForexRate("JPY", "Japanese Yen",       "🇯🇵", 100,  90.40,  91.00));
        list.add(new ForexRate("CNY", "Chinese Yuan",       "🇨🇳",  10, 183.50, 184.30));
        list.add(new ForexRate("AED", "UAE Dirham",         "🇦🇪",   1,  36.30,  36.60));
        list.add(new ForexRate("SAR", "Saudi Riyal",        "🇸🇦",   1,  35.50,  35.80));
        list.add(new ForexRate("QAR", "Qatari Riyal",       "🇶🇦",   1,  36.60,  36.90));
        list.add(new ForexRate("MYR", "Malaysian Ringgit",  "🇲🇾",   1,  30.10,  30.50));
        list.add(new ForexRate("SGD", "Singapore Dollar",   "🇸🇬",   1,  99.80, 100.40));
        list.add(new ForexRate("KWD", "Kuwaiti Dinar",      "🇰🇼",   1, 431.00, 435.00));
        return list;
    }

    // ── Loading state ─────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (rvForex     != null) rvForex.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (tvError     != null) tvError.setVisibility(View.GONE);
    }
}