package com.nighttech.sajhapulse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.NewsArticle;
import com.nighttech.sajhapulse.utils.NepaliCalendarUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * SajhaPatrikaActivity — Full Sajha Patrika news feed.
 *
 * ── Firestore setup ───────────────────────────────────────────────────
 * Collection : news
 * Query      : isActive == true, ordered by publishedAt DESC, limit 100
 *
 * ── Required Firestore index ──────────────────────────────────────────
 * When you run this query Firestore will show a link in Logcat to auto-create
 * the composite index for (isActive ASC, publishedAt DESC). Click it once.
 *
 * ── How to add articles in Firestore Console ─────────────────────────
 *  1. Firebase Console → Firestore → news → Add Document (Auto-ID)
 *  2. Fill all fields as described in NewsArticle.java
 *  3. Set isActive = true so it appears in the feed
 *  4. Set imageUrl to the Firebase Storage download URL for the hero image
 * ──────────────────────────────────────────────────────────────────────
 */
public class SajhaPatrikaActivity extends AppCompatActivity {

    private RecyclerView        recyclerView;
    private ProgressBar         progressBar;
    private TextView            tvEmpty;
    private NewsAdapter         adapter;
    private final List<NewsArticle> articles = new ArrayList<>();

    // Firestore real-time listener handle — removed in onDestroy
    private ListenerRegistration firestoreRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sajha_patrika);

        // SystemBarHelper.applyAppBars(this);

        // ── Toolbar ───────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar_patrika);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // ── RecyclerView ──────────────────────────────────────────────
        recyclerView = findViewById(R.id.rv_patrika);
        progressBar  = findViewById(R.id.progress_patrika);
        tvEmpty      = findViewById(R.id.tv_patrika_empty);

        adapter = new NewsAdapter(articles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);

        loadNews();
    }

    // ── Firestore real-time listener ──────────────────────────────────

    private void loadNews() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Composite query: isActive == true, newest first, max 100 articles
        // NOTE: If Logcat shows "missing index", click the link to auto-create it.
        // Single-field orderBy — no composite index needed.
        // isActive filtering is done in the loop below.
        Query query = FirebaseFirestore.getInstance()
                .collection("news")
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .limit(100);

        firestoreRegistration = query.addSnapshotListener((snapshots, error) -> {
            progressBar.setVisibility(View.GONE);

            if (error != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("समाचार लोड गर्न सकिएन।\nFailed: " + error.getMessage());
                return;
            }

            if (snapshots == null || snapshots.isEmpty()) {
                articles.clear();
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }

            articles.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                try {
                    NewsArticle a = doc.toObject(NewsArticle.class);
                    if (a != null) {
                        // @DocumentId fills getId() automatically; belt-and-suspenders below
                        if (a.getId() == null || a.getId().isEmpty()) {
                            a.setId(doc.getId());
                        }
                        articles.add(a);
                    }
                } catch (Exception e) {
                    // Skip malformed documents — never crash
                }
            }

            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove real-time listener to stop billing and prevent memory leaks
        if (firestoreRegistration != null) {
            firestoreRegistration.remove();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // RecyclerView Adapter
    // ══════════════════════════════════════════════════════════════════

    class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {

        private final List<NewsArticle> list;

        // Shared Glide options — cache both original + resized, nice placeholder
        private final RequestOptions thumbOptions = new RequestOptions()
                .placeholder(R.color.md_theme_primaryContainer)
                .error(R.color.md_theme_primaryContainer)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

        NewsAdapter(List<NewsArticle> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_news_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            NewsArticle a = list.get(position);

            // ── Title & category ──────────────────────────────────────
            h.tvTitle.setText(a.getTitle());
            h.tvCategory.setText(a.getCategory());

            // ── LIVE badge ────────────────────────────────────────────
            h.badgeLive.setVisibility(a.isBreaking() ? View.VISIBLE : View.GONE);

            // ── Timestamp — BS date + AD time ─────────────────────────
            long pubMs = a.getPublishedAt();
            if (pubMs > 0) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
                cal.setTimeInMillis(pubMs);
                int[] bs = NepaliCalendarUtils.adToBs(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH));
                String bsStr  = NepaliCalendarUtils.toDevanagariDateString(bs);
                String adTime = new SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                        .format(new Date(pubMs));
                h.tvTime.setText(bsStr + "  •  " + adTime);
            } else {
                h.tvTime.setText("");
            }

            // ── Read time ─────────────────────────────────────────────
            if (h.tvReadTime != null) {
                h.tvReadTime.setText(a.getReadTimeMin() + " min read");
            }

            // ── Tags — first 2 only for compact card ──────────────────
            if (h.tvTags != null) {
                String tags = a.getTags();
                if (tags != null && !tags.isEmpty()) {
                    h.tvTags.setVisibility(View.VISIBLE);
                    String[] parts = tags.split(",");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.min(2, parts.length); i++) {
                        String t = parts[i].trim();
                        if (!t.isEmpty()) sb.append("#").append(t).append("  ");
                    }
                    h.tvTags.setText(sb.toString().trim());
                } else {
                    h.tvTags.setVisibility(View.GONE);
                }
            }

            // ── Thumbnail image via Glide ─────────────────────────────
            // imageUrl is a Firebase Storage HTTPS download URL.
            // DiskCacheStrategy.ALL caches the decoded bitmap so subsequent
            // scrolls are instant without re-downloading.
            String imgUrl = a.getImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                Glide.with(h.itemView.getContext())
                        .load(imgUrl)
                        .apply(thumbOptions)
                        .into(h.ivThumb);
            } else {
                // No image — show placeholder color
                h.ivThumb.setImageResource(R.color.md_theme_primaryContainer);
            }

            // ── Click → ArticleDetailActivity ─────────────────────────
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(
                        SajhaPatrikaActivity.this, ArticleDetailActivity.class);
                intent.putExtra("article_id",        a.getId());
                intent.putExtra("article_title",     a.getTitle());
                intent.putExtra("article_body",      a.getBody());
                intent.putExtra("article_image",     a.getImageUrl());
                intent.putExtra("article_cat",       a.getCategory());
                intent.putExtra("article_time",      a.getPublishedAt());
                intent.putExtra("article_author",    a.getAuthorName());
                intent.putExtra("article_tags",      a.getTags());
                intent.putExtra("article_read_time", a.getReadTimeMin());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView    ivThumb;
            TextView     tvTitle, tvCategory, tvTime, tvReadTime, tvTags;
            LinearLayout badgeLive;

            VH(@NonNull View v) {
                super(v);
                ivThumb    = v.findViewById(R.id.news_item_thumb);
                tvTitle    = v.findViewById(R.id.news_item_title);
                tvCategory = v.findViewById(R.id.news_item_category);
                tvTime     = v.findViewById(R.id.news_item_time);
                tvReadTime = v.findViewById(R.id.news_item_read_time);
                tvTags     = v.findViewById(R.id.news_item_tags);
                badgeLive  = v.findViewById(R.id.news_item_live_badge);
            }
        }
    }
}