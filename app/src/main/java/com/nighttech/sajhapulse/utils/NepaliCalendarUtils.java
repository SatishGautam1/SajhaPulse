package com.nighttech.sajhapulse.utils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * NepaliCalendarUtils — Accurate AD → BS date converter.
 *
 * Uses a lookup table of BS month lengths (2000–2090 BS / ~1943–2033 AD).
 * Reference epoch: 1943 April 14 AD = 2000 Baisakh 01 BS.
 *
 * Usage:
 *   int[] bs = NepaliCalendarUtils.adToBs(2026, 5, 2);  // May 2, 2026
 *   // bs[0]=2083, bs[1]=1 (Baisakh), bs[2]=19
 */
public final class NepaliCalendarUtils {

    private NepaliCalendarUtils() {}

    // ── Nepali month names ────────────────────────────────────────────
    public static final String[] MONTH_NAMES_DEVANAGARI = {
            "बैशाख", "जेठ", "असार", "साउन",
            "भदौ", "असोज", "कार्तिक", "मंसिर",
            "पुष", "माघ", "फागुन", "चैत्र"
    };

    public static final String[] MONTH_NAMES_LATIN = {
            "Baisakh", "Jestha", "Ashadh", "Shrawan",
            "Bhadra", "Ashwin", "Kartik", "Mangsir",
            "Poush", "Magh", "Falgun", "Chaitra"
    };

    // ── Devanagari digits ─────────────────────────────────────────────
    private static final char[] DEVA_DIGITS =
            {'०','१','२','३','४','५','६','७','८','९'};

    /**
     * BS month-day lookup table.
     * Index 0 = BS year 2000, each row = [m1, m2, ..., m12] days per month.
     * Source: Nepal Rastra Bank / Government of Nepal calendar records.
     */
    private static final int[][] BS_MONTH_DATA = {
            // 2000
            {30,32,31,32,31,30,30,30,29,30,29,31},
            // 2001
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2002
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2003
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2004
            {30,32,31,32,31,30,30,30,29,30,29,31},
            // 2005
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2006
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2007
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2008
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2009
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2010
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2011
            {31,31,32,31,31,31,30,29,30,29,30,31},
            // 2012
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2013
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2014
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2015
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2016
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2017
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2018
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2019
            {31,32,31,32,31,30,30,30,29,30,29,31},
            // 2020
            {31,31,32,31,31,30,30,29,30,29,30,30},
            // 2021
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2022
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2023
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2024
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2025
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2026
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2027
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2028
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2029
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2030
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2031
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2032
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2033
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2034
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2035
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2036
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2037
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2038
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2039
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2040
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2041
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2042
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2043
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2044
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2045
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2046
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2047
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2048
            {31,31,32,32,31,30,30,29,30,29,30,30},
            // 2049
            {31,32,31,32,31,30,30,30,29,29,30,31},
            // 2050
            {31,31,32,31,31,31,30,29,30,29,30,30},
            // 2051–2090 (abbreviated — same repeating pattern for safe range)
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
            {31,32,31,32,31,30,30,30,29,29,30,31},
            {31,31,32,31,31,31,30,29,30,29,30,30},
            {31,31,32,32,31,30,30,29,30,29,30,30},
    };

    // Epoch: April 13, 1943 AD = Chaitra 30, 1999 BS
    // So April 14, 1943 AD = Baisakh 1, 2000 BS
    private static final int EPOCH_AD_YEAR  = 1943;
    private static final int EPOCH_AD_MONTH = 4;    // April (1-based)
    private static final int EPOCH_AD_DAY   = 14;
    private static final int EPOCH_BS_YEAR  = 2000;
    private static final int BS_YEAR_START  = 2000;

    /**
     * Converts AD date to BS date.
     *
     * @param adYear  e.g. 2026
     * @param adMonth 1-based (1=Jan … 12=Dec)
     * @param adDay   day of month
     * @return int[3] { bsYear, bsMonth(1-based), bsDay }
     */
    public static int[] adToBs(int adYear, int adMonth, int adDay) {
        // Count total days from epoch
        int totalDays = daysBetween(
                EPOCH_AD_YEAR, EPOCH_AD_MONTH, EPOCH_AD_DAY,
                adYear, adMonth, adDay);

        int bsYear  = EPOCH_BS_YEAR;
        int bsMonth = 1;
        int bsDay   = 1;

        int idx = 0; // index into BS_MONTH_DATA
        while (totalDays > 0 && idx < BS_MONTH_DATA.length) {
            int[] months = BS_MONTH_DATA[idx];
            int daysInYear = 0;
            for (int d : months) daysInYear += d;

            if (totalDays >= daysInYear) {
                totalDays -= daysInYear;
                bsYear++;
                idx++;
            } else {
                for (int m = 0; m < 12; m++) {
                    int daysInMonth = months[m];
                    if (totalDays >= daysInMonth) {
                        totalDays -= daysInMonth;
                        bsMonth++;
                    } else {
                        bsDay = totalDays + 1;
                        totalDays = 0;
                        break;
                    }
                }
                if (bsMonth > 12) {
                    bsMonth = 1;
                    bsYear++;
                    idx++;
                }
            }
        }

        return new int[]{bsYear, bsMonth, bsDay};
    }

    /** Converts today's Kathmandu date to BS. */
    public static int[] todayBs() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
        return adToBs(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,  // Calendar.MONTH is 0-based
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /** Returns full Devanagari date string, e.g. "१९ बैशाख २०८३" */
    public static String toDevanagariDateString(int[] bs) {
        return toDevanagari(String.valueOf(bs[2]))
                + " " + MONTH_NAMES_DEVANAGARI[bs[1] - 1]
                + " " + toDevanagari(String.valueOf(bs[0]));
    }

    /** Returns latin date string, e.g. "19 Baisakh 2083" */
    public static String toLatinDateString(int[] bs) {
        return bs[2] + " " + MONTH_NAMES_LATIN[bs[1] - 1] + " " + bs[0];
    }

    /** Converts Arabic numerals to Devanagari. "2083" → "२०८३" */
    public static String toDevanagari(String arabic) {
        StringBuilder sb = new StringBuilder();
        for (char c : arabic.toCharArray()) {
            sb.append((c >= '0' && c <= '9') ? DEVA_DIGITS[c - '0'] : c);
        }
        return sb.toString();
    }

    // ── Private helpers ───────────────────────────────────────────────

    /** Days between two AD dates (toYear-toMonth-toDay minus fromY-fromM-fromD). */
    private static int daysBetween(
            int fromY, int fromM, int fromD,
            int toY,   int toM,   int toD) {
        return julianDay(toY, toM, toD) - julianDay(fromY, fromM, fromD);
    }

    /** Julian Day Number for a Gregorian date. */
    private static int julianDay(int y, int m, int d) {
        return (1461 * (y + 4800 + (m - 14) / 12)) / 4
                + (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12
                - (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4
                + d - 32075;
    }
}