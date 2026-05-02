package com.nighttech.sajhapulse.data.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

/**
 * NewsArticle — Firestore model.
 *
 * ─────────────────────────────────────────────────────────────────────
 * Firestore path : /news/{documentId}/
 * ─────────────────────────────────────────────────────────────────────
 *
 * ── BUGS FIXED IN THIS VERSION ───────────────────────────────────────
 *
 * BUG 1 — isBreaking / isActive ClassCastException (CRASH)
 *   Firestore reflection strips the "is" prefix from boolean getters.
 *   So isBreaking() → Firestore looks for field "breaking" in the doc,
 *   but your document has field "isBreaking" → mapping fails → crash.
 *   FIX: Add @PropertyName("isBreaking") / @PropertyName("isActive")
 *        on BOTH the getter and the setter so Firestore uses the exact
 *        field name instead of deriving it from the method name.
 *
 * BUG 2 — publishedAt stored as double → long cast crash
 *   Firestore stores ALL numbers (including integers) as double
 *   internally. Mapping a Firestore double into a Java primitive long
 *   field throws a ClassCastException at runtime.
 *   FIX: Store publishedAt as double internally. Expose it as long
 *        via getPublishedAt() using a safe (long) cast. The setter
 *        accepts double so Firestore reflection works without issues.
 *        Any existing long value (e.g. 1717200000000) converts
 *        perfectly — no precision loss for epoch milliseconds within
 *        the next several decades.
 *
 * ─────────────────────────────────────────────────────────────────────
 *
 * HOW TO SET UP FIRESTORE DATABASE
 * ─────────────────────────────────
 * 1. Firebase Console → Firestore Database → Create database → Test mode
 * 2. Create Collection:  news
 * 3. Add Document (Auto-ID) with the fields below.
 *
 * EXAMPLE DOCUMENT:
 * ─────────────────────────────────────────────────────────────────────
 * Collection : news
 * Document   : (Auto-ID)
 *
 *   title        (string)  → "नेपाल राष्ट्र बैंकले ब्याजदर घटायो"
 *   body         (string)  → "काठमाडौं — नेपाल राष्ट्र बैंकले आज..."
 *   category     (string)  → "MARKETS"
 *                             [POLITICS|MARKETS|WEATHER|SPORTS|TECH|HEALTH|EDUCATION|SOCIETY]
 *   imageUrl     (string)  → "https://firebasestorage.googleapis.com/..."
 *   publishedAt  (number)  → 1717200000000   ← enter as a number in Firestore console
 *   isBreaking   (boolean) → false
 *   isActive     (boolean) → true            ← MUST be true to appear in feed
 *   authorName   (string)  → "SajhaPulse Team"
 *   tags         (string)  → "economy,nrb,banking"
 *   readTimeMin  (number)  → 3
 *   sourceUrl    (string)  → ""
 *
 * HOW TO GET imageUrl
 * ─────────────────────────────────────────────────────────────────────
 * 1. Firebase Console → Storage → Upload image
 * 2. Click the file → copy "Download URL"  (starts with https://firebasestorage...)
 * 3. Paste into the imageUrl field of your Firestore document.
 *
 * FIRESTORE SECURITY RULES
 * ─────────────────────────────────────────────────────────────────────
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     match /news/{doc} {
 *       allow read:  if true;
 *       allow write: if request.auth != null;
 *     }
 *   }
 * }
 * ─────────────────────────────────────────────────────────────────────
 */
@IgnoreExtraProperties
public class NewsArticle {

    /**
     * @DocumentId tells Firestore to inject the document ID here.
     * Do NOT add an "id" field to your Firestore document.
     */
    @DocumentId
    private String id;

    private String title;
    private String body;
    private String category;
    private String imageUrl;

    /**
     * Stored as double because Firestore serialises ALL numbers to double
     * internally, including integers entered in the console.
     * Exposed publicly as long via getPublishedAt() — safe cast for
     * epoch-millisecond values for the foreseeable future.
     */
    private double publishedAt;

    /**
     * IMPORTANT: @PropertyName must be on BOTH getter AND setter.
     *
     * Without it, Firestore derives the field name from the Java getter name:
     *   isBreaking() → strips "is" prefix → looks for Firestore field "breaking"
     *   → field not found in document → returns false / crashes on write.
     *
     * With @PropertyName("isBreaking"), Firestore uses the exact string
     * "isBreaking" → matches your document field exactly → works correctly.
     */
    private boolean isBreaking;
    private boolean isActive;

    private String authorName;
    private String tags;
    private int    readTimeMin;
    private String sourceUrl;

    // ── Required empty constructor for Firestore ──────────────────────
    public NewsArticle() {}

    // ── Full constructor ──────────────────────────────────────────────
    public NewsArticle(String title, String body, String category,
                       String imageUrl, long publishedAt,
                       boolean isBreaking, boolean isActive,
                       String authorName, String tags,
                       int readTimeMin, String sourceUrl) {
        this.title       = title;
        this.body        = body;
        this.category    = category;
        this.imageUrl    = imageUrl;
        this.publishedAt = (double) publishedAt; // long → double, no precision loss
        this.isBreaking  = isBreaking;
        this.isActive    = isActive;
        this.authorName  = authorName;
        this.tags        = tags;
        this.readTimeMin = readTimeMin;
        this.sourceUrl   = sourceUrl;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String  getId()       { return id; }
    public String  getTitle()    { return title    != null ? title    : ""; }
    public String  getBody()     { return body     != null ? body     : ""; }
    public String  getCategory() { return category != null ? category : "NEWS"; }
    public String  getImageUrl() { return imageUrl != null ? imageUrl : ""; }

    /** Returns epoch milliseconds. Safe (long) cast from internal double. */
    public long getPublishedAt() { return (long) publishedAt; }

    /**
     * @PropertyName("isBreaking") forces Firestore to read/write the document
     * field named exactly "isBreaking" — matching what is stored in Firestore.
     */
    @PropertyName("isBreaking")
    public boolean isBreaking() { return isBreaking; }

    /** Same fix applied to isActive. */
    @PropertyName("isActive")
    public boolean isActive() { return isActive; }

    public String  getAuthorName()  { return authorName != null && !authorName.isEmpty() ? authorName : "SajhaPulse"; }
    public String  getTags()        { return tags       != null ? tags       : ""; }
    public int     getReadTimeMin() { return readTimeMin > 0    ? readTimeMin : 1; }
    public String  getSourceUrl()   { return sourceUrl  != null ? sourceUrl  : ""; }

    // ── Setters ───────────────────────────────────────────────────────

    public void setId(String id)                   { this.id = id; }
    public void setTitle(String title)             { this.title = title; }
    public void setBody(String body)               { this.body = body; }
    public void setCategory(String category)       { this.category = category; }
    public void setImageUrl(String imageUrl)       { this.imageUrl = imageUrl; }

    /**
     * Accepts double so Firestore reflection can deserialise a Firestore
     * number field (which is always double) directly into this setter.
     * Calling code that passes a long (e.g. System.currentTimeMillis())
     * will auto-widen to double — no data loss.
     */
    public void setPublishedAt(double publishedAt) { this.publishedAt = publishedAt; }

    /** Must mirror the @PropertyName on the getter above. */
    @PropertyName("isBreaking")
    public void setBreaking(boolean breaking)      { isBreaking = breaking; }

    /** Must mirror the @PropertyName on the getter above. */
    @PropertyName("isActive")
    public void setActive(boolean active)          { isActive = active; }

    public void setAuthorName(String authorName)   { this.authorName = authorName; }
    public void setTags(String tags)               { this.tags = tags; }
    public void setReadTimeMin(int readTimeMin)    { this.readTimeMin = readTimeMin; }
    public void setSourceUrl(String sourceUrl)     { this.sourceUrl = sourceUrl; }
}