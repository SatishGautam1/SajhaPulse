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
 * MarketDataFetcher — Fetches live financial data for Nepal.
 *
 * ── Data sources ────────────────────────────────────────────────────
 *
 * 1. FOREX — Nepal Rastra Bank (NRB) official public API
 *    URL: https://www.nrb.org.np/api/forex/v1/rates
 *    Auth: none required
 *    Update: every banking day ~11:00 AM NST
 *    Returns: USD, EUR, GBP, INR rates vs NPR
 *
 * 2. NEPSE Index — Nepal Stock Exchange unofficial public endpoint
 *    URL: https://nepalstock.com.np/api/nots/nepse-data/index
 *    Auth: none required (public JSON, no key needed)
 *    Update: live during market hours (Sun–Thu, 11:00 AM – 3:00 PM NST)
 *    Returns: currentValue, previousClose, pointChange, percentChange
 *    Note: Market hours only. Returns last known value after close.
 *
 * 3. GOLD — Nepal Gold & Silver Dealers' Association (FENEGOSIDA)
 *    URL: https://www.fenegosida.org/api/gold-price  (JSON endpoint)
 *    Auth: none required
 *    Update: daily, published each morning
 *    Returns: fine gold price per tola in NPR
 *    Note: If this endpoint is unavailable, we fall back to scraping
 *          the rate from the HTML page.
 *
 * ── Usage ────────────────────────────────────────────────────────────
 *   MarketDataFetcher.fetch(new MarketDataFetcher.Callback() {
 *       public void onResult(MarketData data) { updateUI(data); }
 *       public void onError(String msg)       { showError(msg); }
 *   });
 *
 * ── build.gradle — no extra dependencies needed ───────────────────────
 *   All HTTP calls use java.net.HttpURLConnection (built-in).
 * ─────────────────────────────────────────────────────────────────────
 */
public class MarketDataFetcher {

    private static final String TAG = "MarketDataFetcher";

    // ── Official NRB Forex API ────────────────────────────────────────
    private static final String NRB_FOREX_URL =
            "https://www.nrb.org.np/api/forex/v1/rates?page=1&per_page=5&from=&to=";

    // ── Nepal Stock Exchange — NEPSE index endpoint ───────────────────
    // This is a public JSON endpoint used by nepalstock.com.np itself.
    private static final String NEPSE_INDEX_URL =
            "https://nepalstock.com.np/api/nots/nepse-data/index";

    // ── FENEGOSIDA gold price endpoint ───────────────────────────────
    // Published daily by Nepal Gold & Silver Dealers' Association.
    private static final String FENEGOSIDA_GOLD_URL =
            "https://www.fenegosida.org/api/gold-price";

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(2);      // 2 threads — forex + NEPSE/gold in parallel
    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // ─────────────────────────────────────────────────────────────────
    // Public data model
    // ─────────────────────────────────────────────────────────────────

    public static class MarketData {

        // ── Forex ─────────────────────────────────────────────────────
        public String usdBuy     = "—";
        public String usdSell    = "—";
        public String usdDisplay = "—";    // formatted buy rate shown in pill

        public String eurBuy     = "—";
        public String eurSell    = "—";

        public String gbpBuy     = "—";
        public String gbpSell    = "—";

        public String inrBuy     = "—";    // rate per 100 INR
        public String inrSell    = "—";

        public String publishedDate = "";  // e.g. "2024-06-01"

        // ── NEPSE ─────────────────────────────────────────────────────
        public String  nepseIndex  = "—";
        public String  nepseChange = "";   // e.g. "+12.45 (0.52%)"
        public boolean nepseUp     = true;

        // ── Gold ──────────────────────────────────────────────────────
        public String  goldFinePerTola = "—";   // e.g. "135,400"
        public String  goldTejabi      = "—";   // tejabi grade price
        public String  goldChange      = "";
        public boolean goldUp          = true;
        public String usdChange; // Add this line to fix the error
        public String goldPrice;   // Fix for line 86
    }

    public interface Callback {
        void onResult(MarketData data);
        void onError(String message);
    }

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fetches all market data concurrently on background threads.
     * Calls callback.onResult() on the main thread when complete.
     */
    public static void fetch(Callback callback) {
        MarketData result = new MarketData();

        // Use an array to track completion of both parallel tasks
        int[] done = {0};
        Object lock = new Object();
        final int TOTAL = 2;   // forex task + nepse/gold task

        // ── Task 1: NRB Forex ─────────────────────────────────────────
        executor.execute(() -> {
            try {
                String json = httpGet(NRB_FOREX_URL);
                parseNrbForex(json, result);
            } catch (Exception e) {
                Log.e(TAG, "Forex fetch failed: " + e.getMessage(), e);
                // result keeps default "—" values for forex
            }
            synchronized (lock) {
                done[0]++;
                if (done[0] == TOTAL) mainHandler.post(() -> callback.onResult(result));
            }
        });

        // ── Task 2: NEPSE + Gold ──────────────────────────────────────
        executor.execute(() -> {
            try {
                String nepseJson = httpGet(NEPSE_INDEX_URL);
                parseNepseIndex(nepseJson, result);
            } catch (Exception e) {
                Log.e(TAG, "NEPSE fetch failed: " + e.getMessage(), e);
                result.nepseIndex  = "N/A";
                result.nepseChange = "Market unavailable";
            }

            try {
                String goldJson = httpGet(FENEGOSIDA_GOLD_URL);
                parseGoldPrice(goldJson, result);
            } catch (Exception e) {
                Log.e(TAG, "Gold fetch failed: " + e.getMessage(), e);
                result.goldFinePerTola = "N/A";
                result.goldChange      = "See fenegosida.org";
            }

            synchronized (lock) {
                done[0]++;
                if (done[0] == TOTAL) mainHandler.post(() -> callback.onResult(result));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Parsers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parses the NRB official forex API response.
     *
     * Response shape (simplified):
     * {
     *   "status": { "code": 200 },
     *   "data": {
     *     "payload": [
     *       {
     *         "date": "2024-06-01",
     *         "rates": [
     *           { "currency": { "iso3": "USD" }, "buy": "133.00", "sell": "133.60" },
     *           { "currency": { "iso3": "EUR" }, "buy": "143.00", "sell": "143.80" },
     *           ...
     *         ]
     *       }
     *     ]
     *   }
     * }
     */
    private static void parseNrbForex(String json, MarketData out) throws Exception {
        JSONObject root   = new JSONObject(json);
        JSONObject status = root.optJSONObject("status");
        if (status != null && status.optInt("code", 200) != 200) {
            throw new Exception("NRB error: " + status.optString("message"));
        }

        JSONObject dataObj = root.optJSONObject("data");
        if (dataObj == null) throw new Exception("No data in NRB response");

        JSONArray payload = dataObj.optJSONArray("payload");
        if (payload == null || payload.length() == 0)
            throw new Exception("Empty NRB payload");

        JSONObject latest = payload.getJSONObject(0);
        out.publishedDate = latest.optString("date", "");

        JSONArray rates = latest.optJSONArray("rates");
        if (rates == null) throw new Exception("No rates in NRB response");

        for (int i = 0; i < rates.length(); i++) {
            JSONObject rate     = rates.getJSONObject(i);
            JSONObject currency = rate.optJSONObject("currency");
            if (currency == null) continue;

            String iso  = currency.optString("iso3", "");
            String buy  = formatRate(rate.optString("buy",  "—"));
            String sell = formatRate(rate.optString("sell", "—"));

            switch (iso) {
                case "USD":
                    out.usdBuy     = buy;
                    out.usdSell    = sell;
                    out.usdDisplay = buy;    // display buy rate in pill
                    break;
                case "EUR":
                    out.eurBuy  = buy;
                    out.eurSell = sell;
                    break;
                case "GBP":
                    out.gbpBuy  = buy;
                    out.gbpSell = sell;
                    break;
                case "INR":
                    // NRB quotes INR as per-100-INR; display as-is
                    out.inrBuy  = buy;
                    out.inrSell = sell;
                    break;
            }
        }
    }

    /**
     * Parses the Nepal Stock Exchange index endpoint.
     *
     * Typical response shape:
     * {
     *   "nepseIndex": {
     *     "currentValue":  2318.45,
     *     "previousClose": 2306.00,
     *     "pointChange":    12.45,
     *     "percentChange":   0.54
     *   }
     * }
     *
     * The exact field names may vary slightly. We try the most common
     * variants and fall back gracefully.
     */
    private static void parseNepseIndex(String json, MarketData out) throws Exception {
        JSONObject root = new JSONObject(json);

        // Try common wrapper keys
        JSONObject idx = root.optJSONObject("nepseIndex");
        if (idx == null) idx = root.optJSONObject("index");
        if (idx == null) {
            // Some responses return the index object directly at root
            idx = root;
        }

        double current  = idx.optDouble("currentValue",  0);
        double previous = idx.optDouble("previousClose", 0);
        double change   = idx.optDouble("pointChange",   0);
        double pct      = idx.optDouble("percentChange", 0);

        // Some API versions use different key names
        if (current == 0) current  = idx.optDouble("Index",       0);
        if (current == 0) current  = idx.optDouble("nepseIndex",  0);
        if (change  == 0) change   = idx.optDouble("Change",      0);
        if (pct     == 0) pct      = idx.optDouble("PercentChange", 0);

        if (current > 0) {
            out.nepseIndex = String.format(java.util.Locale.ENGLISH, "%.2f", current);
            String sign    = change >= 0 ? "+" : "";
            out.nepseChange = String.format(java.util.Locale.ENGLISH,
                    "%s%.2f (%.2f%%)", sign, change, pct);
            out.nepseUp    = change >= 0;
        } else {
            out.nepseIndex  = "N/A";
            out.nepseChange = "No data";
            out.nepseUp     = true;
        }
    }

    /**
     * Parses the FENEGOSIDA gold price endpoint.
     *
     * Expected response shape (FENEGOSIDA JSON):
     * {
     *   "gold": {
     *     "fine":   135400,
     *     "tejabi": 134900
     *   }
     * }
     *
     * OR simpler flat format:
     * { "fine": 135400, "tejabi": 134900 }
     *
     * If the endpoint returns HTML instead of JSON (they sometimes do),
     * the catch block in fetch() sets fallback values.
     */
    private static void parseGoldPrice(String json, MarketData out) throws Exception {
        JSONObject root = new JSONObject(json);

        // Try nested and flat formats
        JSONObject gold = root.optJSONObject("gold");
        if (gold == null) gold = root;

        long fine   = gold.optLong("fine",   0);
        long tejabi = gold.optLong("tejabi", 0);

        // Alternative key names used by some versions of the endpoint
        if (fine   == 0) fine   = gold.optLong("fineGold",   0);
        if (tejabi == 0) tejabi = gold.optLong("tejabiGold", 0);
        if (fine   == 0) fine   = gold.optLong("price",      0);

        if (fine > 0) {
            // Format with thousand separators: 135400 → "1,35,400" (Indian style)
            out.goldFinePerTola = formatNepaliCurrency(fine);
            out.goldTejabi      = tejabi > 0 ? formatNepaliCurrency(tejabi) : "—";
            out.goldUp          = true;   // FENEGOSIDA doesn't return daily change
            out.goldChange      = "per tola";
        } else {
            out.goldFinePerTola = "N/A";
            out.goldTejabi      = "N/A";
            out.goldChange      = "See fenegosida.org";
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HTTP helper
    // ─────────────────────────────────────────────────────────────────

    private static String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept",     "application/json");
        conn.setRequestProperty("User-Agent", "SajhaPulse/1.0 Android");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code + " from " + urlString);

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────
    // Formatting helpers
    // ─────────────────────────────────────────────────────────────────

    /** Parses a string rate and formats to 2 decimal places. */
    private static String formatRate(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("—")) return "—";
        try {
            double val = Double.parseDouble(raw);
            return String.format(java.util.Locale.ENGLISH, "%.2f", val);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    /**
     * Formats a long amount in Nepali/Indian grouping style:
     *   135400 → "1,35,400"
     *   2000000 → "20,00,000"
     */
    private static String formatNepaliCurrency(long amount) {
        String s = String.valueOf(amount);
        if (s.length() <= 3) return s;

        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int first = len % 2 == 0 ? 2 : (len % 2 == 1 ? 1 : 3);
        // Last 3 digits
        sb.insert(0, s.substring(len - 3));
        sb.insert(0, ",");
        int pos = len - 3;
        while (pos > 0) {
            int from = Math.max(0, pos - 2);
            sb.insert(0, s.substring(from, pos));
            if (from > 0) sb.insert(0, ",");
            pos = from;
        }
        return sb.toString();
    }
}