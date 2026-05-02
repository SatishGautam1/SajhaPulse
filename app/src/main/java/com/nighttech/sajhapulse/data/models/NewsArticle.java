package com.nighttech.sajhapulse.data.models;

/**
 * NewsArticle — Firebase Realtime Database model for Sajha Patrika.
 *
 * Database path: /news/{articleId}/
 *
 * Fields stored in Firebase:
 *   title       : Article headline (Nepali or English)
 *   body        : Full article text
 *   category    : "POLITICS" | "MARKETS" | "WEATHER" | "SPORTS" | "TECH" | etc.
 *   imageUrl    : Firebase Storage URL for the thumbnail
 *   publishedAt : Epoch millis (Long) — used for sort order
 *   isBreaking  : Boolean — show LIVE badge on carousel card
 *   isActive    : Boolean — hide without deleting (admin soft-delete)
 *   authorName  : Display name of the admin who posted
 *
 * Admin writes via PatrikaAdminActivity or Firebase Console.
 * Readers query via: FirebaseDatabase.getInstance().getReference("news")
 *                        .orderByChild("publishedAt")
 *                        .limitToLast(10)
 */
public class NewsArticle {

    private String  id;            // set locally after fetch (key)
    private String  title;
    private String  body;
    private String  category;
    private String  imageUrl;
    private long    publishedAt;   // epoch ms
    private boolean isBreaking;
    private boolean isActive;
    private String  authorName;

    // Required empty constructor for Firebase deserialization
    public NewsArticle() {}

    public NewsArticle(String title, String body, String category,
                       String imageUrl, long publishedAt,
                       boolean isBreaking, boolean isActive,
                       String authorName) {
        this.title       = title;
        this.body        = body;
        this.category    = category;
        this.imageUrl    = imageUrl;
        this.publishedAt = publishedAt;
        this.isBreaking  = isBreaking;
        this.isActive    = isActive;
        this.authorName  = authorName;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String  getId()          { return id; }
    public String  getTitle()       { return title; }
    public String  getBody()        { return body; }
    public String  getCategory()    { return category; }
    public String  getImageUrl()    { return imageUrl; }
    public long    getPublishedAt() { return publishedAt; }
    public boolean isBreaking()     { return isBreaking; }
    public boolean isActive()       { return isActive; }
    public String  getAuthorName()  { return authorName; }

    // ── Setters ───────────────────────────────────────────────────────

    public void setId(String id)                   { this.id = id; }
    public void setTitle(String title)             { this.title = title; }
    public void setBody(String body)               { this.body = body; }
    public void setCategory(String category)       { this.category = category; }
    public void setImageUrl(String imageUrl)       { this.imageUrl = imageUrl; }
    public void setPublishedAt(long publishedAt)   { this.publishedAt = publishedAt; }
    public void setBreaking(boolean breaking)      { isBreaking = breaking; }
    public void setActive(boolean active)          { isActive = active; }
    public void setAuthorName(String authorName)   { this.authorName = authorName; }
}