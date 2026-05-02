package com.nighttech.sajhapulse.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MarketDataFetcher — Fetches live Forex rates from the official
 * Nepal Rastra Bank (NRB) public API.
 *
 * Endpoint: https://www.nrb.org.np/api/forex/v1/rates
 *   • Updated each banking day by NRB at ~11:00 AM NST.
 *   • Returns USD, EUR, GBP, and all major traded currencies vs NPR.
 *
 * NEPSE data: NEPSE does not have a public REST API.
 *   • We use the Mero Share / NEPSE unofficial endpoint.
 *   • Endpoint: https://nepse.com.np/nepseData/todaypricedetail (scrape fallback)
 *   • For production: subscribe to NepalStockExchange official data feed.
 *
 * Gold data: Nepal Gold & Silver Dealers Association updates daily.
 *   • Endpoint: https://www.fenegosida.org/  (no public API; parsed from HTML)
 *   • For simplicity we use cached value with a note to integrate properly.
 *
 * Usage:
 *   MarketDataFetcher.fetch(new MarketDataFetcher.Callback() {
 *       public void onResult(MarketData data) { ... }
 *       public void onError(String msg)       { ... }
 *   });
 */
public class MarketDataFetcher {

    private static final String TAG = "MarketDataFetcher";

    // Official NRB Forex API — no authentication required
    private static final String NRB_FOREX_URL =
            "https://www.nrb.org.np/api/forex/v1/rates?page=1&per_page=5&from=&to=";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // ── Data model ────────────────────────────────────────────────────

    public static class MarketData {
        public String usdBuy   = "—";
        public String usdSell  = "—";
        public String usdDisplay = "—";   // formatted for pill
        public String usdChange  = "";

        public String eurBuy   = "—";
        public String eurSell  = "—";

        public String gbpBuy   = "—";
        public String gbpSell  = "—";

        public String inrBuy   = "—";
        public String inrSell  = "—";

        public String publishedDate = "";

        // NEPSE — updated separately (no official free API; stub with note)
        public String nepseIndex  = "—";
        public String nepseChange = "";
        public boolean nepseUp    = true;

        // Gold — sourced from FENEGOSIDA; updated daily
        public String goldPrice  = "—";
        public String goldChange = "";
        public boolean goldUp    = true;
    }

    public interface Callback {
        void onResult(MarketData data);
        void onError(String message);
    }

    // ── Public fetch method ───────────────────────────────────────────

    /**
     * Fetches NRB forex data on a background thread and posts the result
     * back to the main thread via the callback.
     */
    public static void fetch(Callback callback) {
        executor.execute(() -> {
            try {
                String json = httpGet(NRB_FOREX_URL);
                MarketData data = parseNrbForex(json);
                mainHandler.post(() -> callback.onResult(data));
            } catch (Exception e) {
                Log.e(TAG, "Fetch failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ── Parser ────────────────────────────────────────────────────────

    private static MarketData parseNrbForex(String json) throws Exception {
        MarketData data = new MarketData();

        JSONObject root   = new JSONObject(json);
        JSONObject status = root.optJSONObject("status");
        if (status != null && status.optInt("code", 0) != 200) {
            throw new Exception("NRB API error: " + status.optString("message"));
        }

        JSONObject dataObj  = root.optJSONObject("data");
        if (dataObj == null) throw new Exception("No data object in NRB response");

        JSONArray payload = dataObj.optJSONArray("payload");
        if (payload == null || payload.length() == 0)
            throw new Exception("Empty payload from NRB API");

        // payload[0] is the most recent rate set
        JSONObject latest = payload.getJSONObject(0);
        data.publishedDate = latest.optString("date", "");

        JSONArray rates = latest.optJSONArray("rates");
        if (rates == null) throw new Exception("No rates array");

        for (int i = 0; i < rates.length(); i++) {
            JSONObject rate     = rates.getJSONObject(i);
            JSONObject currency = rate.optJSONObject("currency");
            if (currency == null) continue;

            String iso = currency.optString("iso3", "");
            String buy  = rate.optString("buy",  "—");
            String sell = rate.optString("sell", "—");

            // NRB returns per-unit rates for most currencies.
            // For INR, it's per 100 INR → we show it as-is.
            switch (iso) {
                case "USD":
                    data.usdBuy  = formatRate(buy);
                    data.usdSell = formatRate(sell);
                    data.usdDisplay = formatRate(buy);
                    break;
                case "EUR":
                    data.eurBuy  = formatRate(buy);
                    data.eurSell = formatRate(sell);
                    break;
                case "GBP":
                    data.gbpBuy  = formatRate(buy);
                    data.gbpSell = formatRate(sell);
                    break;
                case "INR":
                    data.inrBuy  = formatRate(buy);
                    data.inrSell = formatRate(sell);
                    break;
            }
        }

        // ── NEPSE stub ────────────────────────────────────────────────
        // NEPSE does not provide a public JSON REST API.
        // Production options:
        //   1. Subscribe to NepalStockExchange official data (paid).
        //   2. Use MeroShare API (requires OAuth).
        //   3. Parse https://nepalstock.com/today-price (HTML scraping — fragile).
        // For now we show a placeholder; replace with your chosen source.
        data.nepseIndex  = "N/A";
        data.nepseChange = "Market closed";
        data.nepseUp     = true;

        // ── Gold stub ─────────────────────────────────────────────────
        // FENEGOSIDA (fenegosida.org) publishes daily gold prices but
        // has no public API. Integrate their site or use a paid data feed.
        data.goldPrice  = "See site";
        data.goldChange = "fenegosida.org";
        data.goldUp     = true;

        return data;
    }

    // ── HTTP helper ───────────────────────────────────────────────────

    private static String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "SajhaPulse/1.0 Android");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private static String formatRate(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("—")) return "—";
        try {
            double val = Double.parseDouble(raw);
            return String.format(java.util.Locale.ENGLISH, "%.2f", val);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}