package com.nighttech.sajhapulse.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.NewsArticle;
import com.nighttech.sajhapulse.utils.NepaliCalendarUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ArticleDetailActivity — Full-screen article reader.
 *
 * Strategy:
 *   1. Renders data passed via Intent extras immediately (instant UI, no blank state).
 *   2. Simultaneously fetches the full article from Firestore by document ID
 *      to refresh the body in case it was truncated in the Intent.
 *
 * ── Firestore path used: /news/{articleId}  ──────────────────────────
 */
public class ArticleDetailActivity extends AppCompatActivity {

    private String articleId;

    // Views
    private ImageView   ivHero;
    private TextView    tvCategory, tvTitle, tvBsTime, tvAdTime;
    private TextView    tvAuthor, tvReadTime, tvTags, tvBody;
    private ProgressBar progressBody;

    // Reusable Glide options — cache aggressively, nice placeholder
    private RequestOptions glideOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        // If you use SystemBarHelper, keep this; otherwise remove
        // SystemBarHelper.applyAppBars(this);

        // ── Toolbar ───────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar_article);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // ── Bind views ────────────────────────────────────────────────
        ivHero       = findViewById(R.id.article_hero_image);
        tvCategory   = findViewById(R.id.article_category);
        tvTitle      = findViewById(R.id.article_title);
        tvBsTime     = findViewById(R.id.article_time_bs);
        tvAdTime     = findViewById(R.id.article_time_ad);
        tvAuthor     = findViewById(R.id.article_author);
        tvReadTime   = findViewById(R.id.article_read_time);
        tvTags       = findViewById(R.id.article_tags);
        tvBody       = findViewById(R.id.article_body);
        progressBody = findViewById(R.id.progress_article_body);

        // ── Glide options — disk cache + placeholder ──────────────────
        glideOptions = new RequestOptions()
                .placeholder(R.color.md_theme_primaryContainer)
                .error(R.color.md_theme_primaryContainer)
                .diskCacheStrategy(DiskCacheStrategy.ALL)  // cache both original & resized
                .centerCrop();

        // ── Phase 1: Render from Intent immediately ───────────────────
        articleId = getIntent().getStringExtra("article_id");
        renderFromIntent();

        // ── Phase 2: Fetch full article from Firestore ────────────────
        if (articleId != null && !articleId.isEmpty()) {
            fetchFromFirestore(articleId);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Phase 1: instant render — no network wait
    // ─────────────────────────────────────────────────────────────────

    private void renderFromIntent() {
        String title    = getIntent().getStringExtra("article_title");
        String body     = getIntent().getStringExtra("article_body");
        String imageUrl = getIntent().getStringExtra("article_image");
        String category = getIntent().getStringExtra("article_cat");
        String author   = getIntent().getStringExtra("article_author");
        String tags     = getIntent().getStringExtra("article_tags");
        long   pubMs    = getIntent().getLongExtra("article_time", 0);
        int    readTime = getIntent().getIntExtra("article_read_time", 1);

        applyData(title, body, imageUrl, category, author, tags, pubMs, readTime);
    }

    // ─────────────────────────────────────────────────────────────────
    // Phase 2: Firestore fetch — refreshes body & image URL
    // Firestore path: /news/{articleId}
    // ─────────────────────────────────────────────────────────────────

    private void fetchFromFirestore(String id) {
        // Hide progress — Intent data is already visible
        if (progressBody != null) progressBody.setVisibility(View.GONE);

        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection("news")   // ← Firestore collection name
                .document(id);

        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot == null || !snapshot.exists()) return;

            // toObject() maps Firestore fields → NewsArticle via @DocumentId + setters
            NewsArticle a = snapshot.toObject(NewsArticle.class);
            if (a != null) {
                // @DocumentId fills a.getId() automatically; still set for safety
                if (a.getId() == null || a.getId().isEmpty()) a.setId(id);

                applyData(
                        a.getTitle(),
                        a.getBody(),
                        a.getImageUrl(),
                        a.getCategory(),
                        a.getAuthorName(),
                        a.getTags(),
                        a.getPublishedAt(),
                        a.getReadTimeMin());
            }
        }).addOnFailureListener(e -> {
            // Intent data still shown — silent graceful failure
            Toast.makeText(this, "Could not refresh article.", Toast.LENGTH_SHORT).show();
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Bind all data to views
    // ─────────────────────────────────────────────────────────────────

    private void applyData(String title, String body, String imageUrl,
                           String category, String author, String tags,
                           long pubMs, int readTimeMin) {

        if (tvTitle    != null && title    != null) tvTitle.setText(title);
        if (tvCategory != null && category != null) tvCategory.setText(category);
        if (tvBody     != null && body     != null) tvBody.setText(body);

        // Author
        if (tvAuthor != null) {
            tvAuthor.setText((author != null && !author.isEmpty()) ? author : "SajhaPulse");
        }

        // Read time
        if (tvReadTime != null) {
            tvReadTime.setText(readTimeMin + " min read");
        }

        // Tags: "economy,nrb" → "#economy  #nrb"
        if (tvTags != null) {
            if (tags != null && !tags.isEmpty()) {
                tvTags.setVisibility(View.VISIBLE);
                tvTags.setText(formatTags(tags));
            } else {
                tvTags.setVisibility(View.GONE);
            }
        }

        // Timestamps
        if (pubMs > 0) {
            if (tvAdTime != null) {
                SimpleDateFormat adFmt =
                        new SimpleDateFormat("dd MMMM yyyy  •  hh:mm a", Locale.ENGLISH);
                tvAdTime.setText(adFmt.format(new Date(pubMs)));
            }
            if (tvBsTime != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("Asia/Kathmandu"));
                cal.setTimeInMillis(pubMs);
                int[] bs = NepaliCalendarUtils.adToBs(
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH));
                tvBsTime.setText(NepaliCalendarUtils.toDevanagariDateString(bs));
            }
        }

        // ── Hero image — Glide with caching ──────────────────────────
        // imageUrl should be a Firebase Storage HTTPS download URL, e.g.:
        //   https://firebasestorage.googleapis.com/v0/b/<project>.appspot.com/o/...
        // Glide handles loading, caching, placeholder, and error state automatically.
        if (ivHero != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ivHero.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imageUrl)
                        .apply(glideOptions)
                        .into(ivHero);
            } else {
                ivHero.setVisibility(View.GONE);
            }
        }
    }

    /** Formats "economy,nrb,banking" → "#economy  #nrb  #banking" */
    private String formatTags(String raw) {
        String[] parts = raw.split(",");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) sb.append("#").append(t).append("  ");
        }
        return sb.toString().trim();
    }
}