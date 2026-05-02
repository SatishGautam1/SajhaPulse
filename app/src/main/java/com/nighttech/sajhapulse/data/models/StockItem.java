package com.nighttech.sajhapulse.data.models;

/**
 * StockItem — represents a single NEPSE-listed security.
 *
 * Fields match the data shape returned by the Mero Share / NEPSE
 * open-data endpoint. For the official API, see:
 *   https://www.nepalstock.com.np/api/nots
 *
 * In the current stub implementation, objects are constructed manually
 * in NepseFragment. Replace with Retrofit + Gson deserialization once
 * the live endpoint is wired up.
 */
public class StockItem {

    public enum Sector {
        COMMERCIAL_BANK, DEVELOPMENT_BANK, FINANCE,
        HYDRO, INSURANCE, MICROFINANCE, MANUFACTURING,
        HOTEL, TRADING, INVESTMENT, MUTUAL_FUND, OTHER
    }

    private String symbol;       // e.g. "NABIL"
    private String companyName;  // e.g. "Nabil Bank Limited"
    private double ltp;          // Last Traded Price (NPR)
    private double pointChange;  // absolute change from previous close
    private double changePercent;
    private long   volume;       // shares traded today
    private double high;         // today's high
    private double low;          // today's low
    private double prevClose;
    private Sector sector;

    // ── Constructors ──────────────────────────────────────────────────

    public StockItem() {}

    public StockItem(String symbol, String companyName, double ltp,
                     double pointChange, double changePercent,
                     long volume, double high, double low,
                     double prevClose, Sector sector) {
        this.symbol        = symbol;
        this.companyName   = companyName;
        this.ltp           = ltp;
        this.pointChange   = pointChange;
        this.changePercent = changePercent;
        this.volume        = volume;
        this.high          = high;
        this.low           = low;
        this.prevClose     = prevClose;
        this.sector        = sector;
    }

    // ── Derived helpers ───────────────────────────────────────────────

    /** True when the stock is trading above its previous close. */
    public boolean isGainer() { return changePercent > 0; }

    /** True when the stock is trading below its previous close. */
    public boolean isLoser()  { return changePercent < 0; }

    // ── Getters & Setters ─────────────────────────────────────────────

    public String getSymbol()          { return symbol; }
    public void   setSymbol(String v)  { symbol = v; }

    public String getCompanyName()          { return companyName; }
    public void   setCompanyName(String v)  { companyName = v; }

    public double getLtp()           { return ltp; }
    public void   setLtp(double v)   { ltp = v; }

    public double getPointChange()          { return pointChange; }
    public void   setPointChange(double v)  { pointChange = v; }

    public double getChangePercent()          { return changePercent; }
    public void   setChangePercent(double v)  { changePercent = v; }

    public long   getVolume()          { return volume; }
    public void   setVolume(long v)    { volume = v; }

    public double getHigh()           { return high; }
    public void   setHigh(double v)   { high = v; }

    public double getLow()            { return low; }
    public void   setLow(double v)    { low = v; }

    public double getPrevClose()           { return prevClose; }
    public void   setPrevClose(double v)   { prevClose = v; }

    public Sector getSector()          { return sector; }
    public void   setSector(Sector v)  { sector = v; }
}