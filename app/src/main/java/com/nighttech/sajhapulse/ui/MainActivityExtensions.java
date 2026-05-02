package com.nighttech.sajhapulse.ui;

import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseUser;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.utils.MarketDataFetcher;
import com.nighttech.sajhapulse.utils.NepaliCalendarUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * MainActivityExtensions — Drop-in static helpers for MainActivity.
 *
 * ── FIX IN THIS VERSION ───────────────────────────────────────────────
 *
 * FIX — data.goldPrice does not exist → compile error → crash
 *   The MarketData model uses goldFinePerTola (and goldTejabi), not goldPrice.
 *   Every reference to data.goldPrice has been replaced with
 *   data.goldFinePerTola so the code compiles and displays correctly.
 *
 *   Affected locations:
 *     • tvGoldValue.setText(data.goldPrice)  → data.goldFinePerTola
 *     • buildTickerString() Gold/tola entry  → data.goldFinePerTola
 *
 * ─────────────────────────────────────────────────────────────────────
 */
public class MainActivityExtensions {

    // ══════════════════════════════════════════════════════════════════
    // 1. LIVE MARQUEE TICKER — Real NRB Forex + NEPSE + Gold
    // ══════════════════════════════════════════════════════════════════

    /**
     * Fetches live market data and updates all ticker pills + marquee.
     * Refreshes every 60 seconds automatically.
     * Returns the Runnable so MainActivity can cancel it in onDestroy().
     */
    public static Runnable startMarqueeTicker(
            Handler  tickerHandler,
            TextView tvLiveTicker,
            TextView tvNepseValue,    TextView tvNepseChange,
            TextView tvUsdValue,      TextView tvUsdChange,
            TextView tvGoldValue,     TextView tvGoldChange,
            TextView tvTickerTimestamp) {

        final Runnable[] ref = {null};

        ref[0] = new Runnable() {
            @Override
            public void run() {
                // Loading state while fetch runs
                if (tvUsdValue != null) tvUsdValue.setText("…");

                MarketDataFetcher.fetch(new MarketDataFetcher.Callback() {

                    @Override
                    public void onResult(MarketDataFetcher.MarketData data) {

                        // ── USD/NPR pill ──────────────────────────────
                        if (tvUsdValue != null)
                            tvUsdValue.setText(data.usdDisplay);
                        if (tvUsdChange != null) {
                            // usdChange is not populated by NRB API — show label instead
                            tvUsdChange.setText("NRB Rate");
                            tvUsdChange.setTextColor(Color.parseColor("#AAFFFFFF"));
                        }

                        // ── NEPSE pill ────────────────────────────────
                        if (tvNepseValue != null)
                            tvNepseValue.setText(data.nepseIndex);
                        if (tvNepseChange != null) {
                            tvNepseChange.setText(data.nepseChange);
                            tvNepseChange.setTextColor(data.nepseUp
                                    ? Color.parseColor("#4CAF50")
                                    : Color.parseColor("#FF5252"));
                        }

                        // ── Gold pill ─────────────────────────────────
                        // FIX: was data.goldPrice — field is goldFinePerTola
                        if (tvGoldValue != null)
                            tvGoldValue.setText(data.goldFinePerTola);
                        if (tvGoldChange != null) {
                            tvGoldChange.setText(data.goldChange);
                            tvGoldChange.setTextColor(data.goldUp
                                    ? Color.parseColor("#4CAF50")
                                    : Color.parseColor("#FF5252"));
                        }

                        // ── Scrolling marquee string ──────────────────
                        String marquee = buildTickerString(data);
                        if (tvLiveTicker != null) {
                            tvLiveTicker.setText(marquee);
                            // Reset marquee scroll
                            tvLiveTicker.setSelected(false);
                            tvLiveTicker.setSelected(true);
                        }

                        // ── Timestamp ─────────────────────────────────
                        if (tvTickerTimestamp != null) {
                            if (!data.publishedDate.isEmpty()) {
                                tvTickerTimestamp.setText("NRB: " + data.publishedDate);
                            } else {
                                SimpleDateFormat fmt =
                                        new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                                fmt.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
                                tvTickerTimestamp.setText("Updated "
                                        + fmt.format(Calendar.getInstance().getTime()));
                            }
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (tvUsdValue  != null) tvUsdValue.setText("N/A");
                        if (tvUsdChange != null) tvUsdChange.setText("No network");
                        if (tvLiveTicker != null)
                            tvLiveTicker.setText("⚠ Market data unavailable — check connection");
                    }
                });

                // Re-schedule every 60 seconds
                tickerHandler.postDelayed(ref[0], 60_000);
            }
        };

        tickerHandler.post(ref[0]);
        return ref[0];
    }

    /**
     * Builds the full scrolling marquee string.
     * FIX: was data.goldPrice — now correctly uses data.goldFinePerTola.
     */
    private static String buildTickerString(MarketDataFetcher.MarketData d) {
        return "💵 USD/NPR: "    + d.usdDisplay
                + "   |   🇪🇺 EUR/NPR: "  + d.eurBuy
                + "   |   🇬🇧 GBP/NPR: "  + d.gbpBuy
                + "   |   🇮🇳 INR/NPR: "  + d.inrBuy + " (per 100)"
                + "   |   📈 NEPSE: "      + d.nepseIndex
                + "   |   🟡 Gold/tola: "  + d.goldFinePerTola   // ← FIX
                + "        ";
    }


    // ══════════════════════════════════════════════════════════════════
    // 2. NST GREETING  (Nepal Standard Time — UTC+5:45)
    // ══════════════════════════════════════════════════════════════════

    public static String[] getNSTGreeting() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if      (hour >=  5 && hour < 12) return new String[]{"शुभ प्रभात",  "Subha Prabhāt", "morning"};
        else if (hour >= 12 && hour < 17) return new String[]{"शुभ दिन",     "Subha Din",     "afternoon"};
        else if (hour >= 17 && hour < 20) return new String[]{"शुभ सन्ध्या", "Subha Sandhyā", "evening"};
        else                               return new String[]{"शुभ रात्री",  "Subha Rātri",   "night"};
    }

    public static void applyNSTGreeting(
            TextView   tvGreetingLabel,
            TextView   tvGreetingName,
            ImageView  ivGreetingIcon,
            FirebaseUser user) {

        String[] parts  = getNSTGreeting();
        String nepali   = parts[0];
        String iconKey  = parts[2];

        if (tvGreetingLabel != null) tvGreetingLabel.setText(nepali);

        if (ivGreetingIcon != null) {
            int iconRes = "night".equals(iconKey)
                    ? R.drawable.ic_moon : R.drawable.ic_sun;
            ivGreetingIcon.setImageResource(iconRes);
        }

        String firstName = "अतिथि";
        if (user != null
                && user.getDisplayName() != null
                && !user.getDisplayName().isEmpty()) {
            firstName = user.getDisplayName().split(" ")[0];
        }
        if (tvGreetingName != null) tvGreetingName.setText("नमस्ते, " + firstName);
    }


    // ══════════════════════════════════════════════════════════════════
    // 3. DRAWER HEADER — Firebase user + Glide avatar
    // ══════════════════════════════════════════════════════════════════

    public static void populateDrawerHeader(
            android.content.Context context,
            View         headerView,
            FirebaseUser user) {

        if (headerView == null) return;

        ImageView ivAvatar = headerView.findViewById(R.id.nav_header_avatar);
        TextView  tvName   = headerView.findViewById(R.id.nav_header_name);
        TextView  tvEmail  = headerView.findViewById(R.id.nav_header_email);

        if (user != null) {
            String displayName = user.getDisplayName();
            if (tvName != null)
                tvName.setText((displayName != null && !displayName.isEmpty())
                        ? displayName : "SajhaPulse User");

            String email = user.getEmail();
            if (tvEmail != null)
                tvEmail.setText((email != null && !email.isEmpty())
                        ? email : "No email linked");

            if (ivAvatar != null) {
                if (user.getPhotoUrl() != null) {
                    Glide.with(context)
                            .load(user.getPhotoUrl())
                            .placeholder(R.drawable.ic_account_circle)
                            .error(R.drawable.ic_account_circle)
                            .transform(new CircleCrop())
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_account_circle);
                }
            }
        } else {
            if (tvName   != null) tvName.setText("अतिथि");
            if (tvEmail  != null) tvEmail.setText("sajhapulse.app");
            if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_account_circle);
        }
    }


    // ══════════════════════════════════════════════════════════════════
    // 4. UTILITY — Devanagari numeral formatter
    // ══════════════════════════════════════════════════════════════════

    public static String toDevanagari(String arabic) {
        return NepaliCalendarUtils.toDevanagari(arabic);
    }


    // ══════════════════════════════════════════════════════════════════
    // 5. UTILITY — Change color (green / red / neutral)
    // ══════════════════════════════════════════════════════════════════

    public static int changeColor(double delta) {
        if (delta > 0) return Color.parseColor("#4CAF50");
        if (delta < 0) return Color.parseColor("#FF5252");
        return Color.parseColor("#AAFFFFFF");
    }
}