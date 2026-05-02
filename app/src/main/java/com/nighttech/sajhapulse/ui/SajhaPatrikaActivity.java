package com.nighttech.sajhapulse.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.NewsArticle;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SajhaPatrikaActivity — Full news feed screen.
 *
 * Firebase Realtime Database path: /news/
 * Reads all active articles, sorted newest-first, displayed in a RecyclerView.
 * Clicking an article opens ArticleDetailActivity.
 *
 * Database Rules (paste into Firebase Console → Realtime Database → Rules):
 * {
 *   "rules": {
 *     "news": {
 *       ".read": true,
 *       ".write": "auth != null && auth.token.admin === true"
 *     }
 *   }
 * }
 */
public class SajhaPatrikaActivity extends AppCompatActivity {

    private RecyclerView  recyclerView;
    private ProgressBar   progressBar;
    private TextView      tvEmpty;
    private NewsAdapter   adapter;
    private List<NewsArticle> articles = new ArrayList<>();
    private ValueEventListener newsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sajha_patrika);

        SystemBarHelper.applyAppBars(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_patrika);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.rv_patrika);
        progressBar  = findViewById(R.id.progress_patrika);
        tvEmpty      = findViewById(R.id.tv_patrika_empty);

        adapter = new NewsAdapter(articles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadNews();
    }

    // ── Firebase load ─────────────────────────────────────────────────

    private void loadNews() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Query query = FirebaseDatabase.getInstance()
                .getReference("news")
                .orderByChild("publishedAt")
                .limitToLast(50);

        newsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                articles.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NewsArticle a = child.getValue(NewsArticle.class);
                    if (a != null && a.isActive()) {
                        a.setId(child.getKey());
                        articles.add(a);
                    }
                }
                // Reverse so newest is first
                Collections.reverse(articles);

                progressBar.setVisibility(View.GONE);
                if (articles.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Failed to load news: " + error.getMessage());
            }
        };

        query.addValueEventListener(newsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (newsListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("news")
                    .removeEventListener(newsListener);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // RecyclerView Adapter
    // ══════════════════════════════════════════════════════════════════

    class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

        private final List<NewsArticle> list;

        NewsAdapter(List<NewsArticle> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_news_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            NewsArticle a = list.get(position);

            h.tvTitle.setText(a.getTitle());
            h.tvCategory.setText(a.getCategory());

            // Published timestamp
            if (a.getPublishedAt() > 0) {
                SimpleDateFormat sdf =
                        new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
                h.tvTime.setText(sdf.format(new Date(a.getPublishedAt())));
            }

            // LIVE / BREAKING badge
            h.badgeLive.setVisibility(a.isBreaking() ? View.VISIBLE : View.GONE);

            // Thumbnail via Glide
            if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
                Glide.with(h.itemView.getContext())
                        .load(a.getImageUrl())
                        .placeholder(R.color.md_theme_primaryContainer)
                        .centerCrop()
                        .into(h.ivThumb);
            } else {
                h.ivThumb.setImageResource(R.color.md_theme_primaryContainer);
            }

            // Click → ArticleDetailActivity
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(
                        SajhaPatrikaActivity.this, ArticleDetailActivity.class);
                intent.putExtra("article_id",    a.getId());
                intent.putExtra("article_title", a.getTitle());
                intent.putExtra("article_body",  a.getBody());
                intent.putExtra("article_image", a.getImageUrl());
                intent.putExtra("article_cat",   a.getCategory());
                intent.putExtra("article_time",  a.getPublishedAt());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView   ivThumb;
            TextView    tvTitle, tvCategory, tvTime;
            LinearLayout badgeLive;

            ViewHolder(@NonNull View v) {
                super(v);
                ivThumb    = v.findViewById(R.id.news_item_thumb);
                tvTitle    = v.findViewById(R.id.news_item_title);
                tvCategory = v.findViewById(R.id.news_item_category);
                tvTime     = v.findViewById(R.id.news_item_time);
                badgeLive  = v.findViewById(R.id.news_item_live_badge);
            }
        }
    }
}