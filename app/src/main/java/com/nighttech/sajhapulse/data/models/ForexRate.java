package com.nighttech.sajhapulse.data.models;

/**
 * ForexRate — a single NRB foreign exchange entry.
 *
 * Mirrors the JSON shape under data.payload[0].rates[] from:
 *   https://www.nrb.org.np/api/forex-rate?_format=json
 *
 * The {@code flagEmoji} field is NOT in the NRB response; it is
 * assigned by ForexFragment after parsing, keyed on iso3.
 */
public class ForexRate {

    private String iso3;        // "USD"
    private String name;        // "U.S. Dollar"
    private String flagEmoji;   // "🇺🇸"  (assigned in fragment)
    private int    unit;        // 1, 10, or 100 depending on currency
    private double buy;         // NRB buying rate (NPR)
    private double sell;        // NRB selling rate (NPR)

    // ── Constructors ──────────────────────────────────────────────────

    public ForexRate() {}

    public ForexRate(String iso3, String name, String flagEmoji,
                     int unit, double buy, double sell) {
        this.iso3      = iso3;
        this.name      = name;
        this.flagEmoji = flagEmoji;
        this.unit      = unit;
        this.buy       = buy;
        this.sell      = sell;
    }

    // ── Derived ───────────────────────────────────────────────────────

    /**
     * Returns the mid-rate (average of buy and sell), normalised to 1 unit.
     */
    public double getMidRate() {
        return unit > 0 ? ((buy + sell) / 2.0) / unit : 0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public String getIso3()            { return iso3; }
    public void   setIso3(String v)    { iso3 = v; }

    public String getName()            { return name; }
    public void   setName(String v)    { name = v; }

    public String getFlagEmoji()              { return flagEmoji; }
    public void   setFlagEmoji(String v)      { flagEmoji = v; }

    public int    getUnit()            { return unit; }
    public void   setUnit(int v)       { unit = v; }

    public double getBuy()             { return buy; }
    public void   setBuy(double v)     { buy = v; }

    public double getSell()            { return sell; }
    public void   setSell(double v)    { sell = v; }
}