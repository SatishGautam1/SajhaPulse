package com.nighttech.sajhapulse.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.NewsArticle;
import com.nighttech.sajhapulse.utils.NepaliCalendarUtils;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * MainActivity — SajhaPulse Home Dashboard.
 *
 * Key fixes in this version:
 *   • BS date uses NepaliCalendarUtils.todayBs() — accurate AD→BS conversion.
 *   • Live market fetches real NRB Forex API data via MarketDataFetcher.
 *   • News carousel is populated from Firebase Realtime Database (/news/).
 *   • AppBar is fixed (no lift-on-scroll) — scrolling body only.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // ── Core layout views ─────────────────────────────────────────────
    private DrawerLayout      drawerLayout;
    private NavigationView    navView;
    private MaterialToolbar   toolbar;
    private AppBarLayout      appBarLayout;
    private NestedScrollView  scrollDashboard;

    // ── DateTime Hero Card ────────────────────────────────────────────
    private TextView         tvTimeMain, tvAmPm, tvSeconds;
    private TextView         tvDayName, tvBsDate, tvBsDateLatin, tvAdDate;
    private MaterialCardView cardDatetime;
    private View             viewLiveDot;

    // ── Toolbar greeting ──────────────────────────────────────────────
    private TextView  tvGreetingLabel, tvGreetingName;
    private ImageView ivGreetingIcon;

    // ── Live Ticker views ─────────────────────────────────────────────
    private TextView tvLiveTicker;
    private TextView tvNepseValue, tvNepseChange;
    private TextView tvUsdValue,   tvUsdChange;
    private TextView tvGoldValue,  tvGoldChange;
    private TextView tvTickerTimestamp;

    // ── News Carousel views (3 cards) ─────────────────────────────────
    private MaterialCardView newsCard1, newsCard2, newsCard3;
    private ImageView        newsThumb1, newsThumb2, newsThumb3;
    private TextView         newsTitle1, newsTitle2, newsTitle3;
    private TextView         newsCat1, newsCat2, newsCat3;
    private LinearLayout     newsLiveBadge1; // breaking badge on card 1

    // ── Firebase ──────────────────────────────────────────────────────
    private FirebaseAuth          mAuth;
    private ValueEventListener    carouselListener;
    private List<NewsArticle>     carouselArticles = new ArrayList<>();

    // ── Handlers ──────────────────────────────────────────────────────
    private final Handler clockHandler   = new Handler(Looper.getMainLooper());
    private final Handler liveDotHandler = new Handler(Looper.getMainLooper());
    private final Handler tickerHandler  = new Handler(Looper.getMainLooper());

    private Runnable clockRunnable;
    private Runnable liveDotRunnable;
    private Runnable tickerRunnable;

    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SystemBarHelper.applyAppBars(this);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupToolbarAndDrawer();
        setupNavigationListeners();

        MainActivityExtensions.populateDrawerHeader(
                this, navView.getHeaderView(0), mAuth.getCurrentUser());

        MainActivityExtensions.applyNSTGreeting(
                tvGreetingLabel, tvGreetingName, ivGreetingIcon, mAuth.getCurrentUser());

        // ── Clock + CORRECT BS calendar ──────────────────────────────
        startLiveClock();
        updateCalendarInfo();   // uses NepaliCalendarUtils

        // ── Hero card animation ───────────────────────────────────────
        animateDateTimeCard();
        startLiveDotPulse();

        // ── Live market — real NRB data ───────────────────────────────
        tickerRunnable = MainActivityExtensions.startMarqueeTicker(
                tickerHandler,
                tvLiveTicker,
                tvNepseValue, tvNepseChange,
                tvUsdValue,   tvUsdChange,
                tvGoldValue,  tvGoldChange,
                tvTickerTimestamp);

        // ── News carousel from Firebase ───────────────────────────────
        loadCarouselNews();
    }

    // ── View binding ──────────────────────────────────────────────────

    private void bindViews() {
        drawerLayout    = findViewById(R.id.drawer_layout);
        navView         = findViewById(R.id.nav_view);
        toolbar         = findViewById(R.id.toolbar);
        appBarLayout    = findViewById(R.id.app_bar);
        scrollDashboard = findViewById(R.id.scroll_dashboard);

        // DateTime hero
        tvTimeMain    = findViewById(R.id.tv_time_main);
        tvAmPm        = findViewById(R.id.tv_am_pm);
        tvSeconds     = findViewById(R.id.tv_seconds);
        tvDayName     = findViewById(R.id.tv_day_name);
        tvBsDate      = findViewById(R.id.tv_bs_date);
        tvBsDateLatin = findViewById(R.id.tv_bs_date_latin);
        tvAdDate      = findViewById(R.id.tv_ad_date);
        cardDatetime  = findViewById(R.id.card_datetime);
        viewLiveDot   = findViewById(R.id.view_live_dot);

        // Toolbar greeting
        tvGreetingLabel = findViewById(R.id.tv_greeting_label);
        tvGreetingName  = findViewById(R.id.tv_greeting_name);
        ivGreetingIcon  = findViewById(R.id.iv_greeting_icon);

        // Live Ticker
        tvLiveTicker      = findViewById(R.id.tv_live_ticker);
        tvNepseValue      = findViewById(R.id.tv_nepse_value);
        tvNepseChange     = findViewById(R.id.tv_nepse_change);
        tvUsdValue        = findViewById(R.id.tv_usd_value);
        tvUsdChange       = findViewById(R.id.tv_usd_change);
        tvGoldValue       = findViewById(R.id.tv_gold_value);
        tvGoldChange      = findViewById(R.id.tv_gold_change);
        tvTickerTimestamp = findViewById(R.id.tv_ticker_timestamp);

        // News carousel
        newsCard1      = findViewById(R.id.news_card_1);
        newsCard2      = findViewById(R.id.news_card_2);
        newsCard3      = findViewById(R.id.news_card_3);
        newsThumb1     = findViewById(R.id.news_thumb_1);
        newsThumb2     = findViewById(R.id.news_thumb_2);
        newsThumb3     = findViewById(R.id.news_thumb_3);
        newsTitle1     = findViewById(R.id.news_title_1);
        newsTitle2     = findViewById(R.id.news_title_2);
        newsTitle3     = findViewById(R.id.news_title_3);
        newsCat1       = findViewById(R.id.news_category_1);
        newsCat2       = findViewById(R.id.news_category_2);
        newsCat3       = findViewById(R.id.news_category_3);
        newsLiveBadge1 = findViewById(R.id.news_live_badge_1);
    }

    // ── Toolbar + Drawer ──────────────────────────────────────────────

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close);
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    // ── BS Calendar (correct) ─────────────────────────────────────────

    /**
     * Converts today's AD date to Bikram Sambat using the lookup table
     * in NepaliCalendarUtils. Replaces the old hardcoded stub.
     */
    private void updateCalendarInfo() {
        // AD date
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
        SimpleDateFormat adFormat =
                new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
        if (tvAdDate != null) tvAdDate.setText(adFormat.format(cal.getTime()));

        // BS date — accurate conversion
        int[] bs = NepaliCalendarUtils.todayBs();
        if (tvBsDate != null)
            tvBsDate.setText(NepaliCalendarUtils.toDevanagariDateString(bs));
        if (tvBsDateLatin != null)
            tvBsDateLatin.setText(NepaliCalendarUtils.toLatinDateString(bs));
    }

    // ── News Carousel — Firebase Realtime Database ────────────────────

    /**
     * Loads the 3 most recent active articles from /news/ and populates
     * the carousel cards in content_main.xml.
     * Uses a persistent ValueEventListener so cards update in real time
     * whenever the admin publishes new articles.
     */
    private void loadCarouselNews() {
        Query query = FirebaseDatabase.getInstance()
                .getReference("news")
                .orderByChild("publishedAt")
                .limitToLast(3);

        carouselListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                carouselArticles.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NewsArticle a = child.getValue(NewsArticle.class);
                    if (a != null && a.isActive()) {
                        a.setId(child.getKey());
                        carouselArticles.add(a);
                    }
                }
                // Reverse: newest first
                Collections.reverse(carouselArticles);
                bindCarouselCards();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Cards keep their placeholder/tools:text on failure
            }
        };

        query.addValueEventListener(carouselListener);
    }

    private void bindCarouselCards() {
        // Helper arrays for concise binding
        MaterialCardView[] cards   = {newsCard1,  newsCard2,  newsCard3};
        ImageView[]        thumbs  = {newsThumb1, newsThumb2, newsThumb3};
        TextView[]         titles  = {newsTitle1, newsTitle2, newsTitle3};
        TextView[]         cats    = {newsCat1,   newsCat2,   newsCat3};

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;

            if (i < carouselArticles.size()) {
                NewsArticle a = carouselArticles.get(i);

                if (titles[i] != null) titles[i].setText(a.getTitle());
                if (cats[i]   != null) cats[i].setText(a.getCategory());

                // LIVE badge — only on card 1
                if (i == 0 && newsLiveBadge1 != null)
                    newsLiveBadge1.setVisibility(
                            a.isBreaking() ? View.VISIBLE : View.GONE);

                // Thumbnail via Glide
                if (thumbs[i] != null) {
                    String url = a.getImageUrl();
                    if (url != null && !url.isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(url)
                                .placeholder(R.color.md_theme_primaryContainer)
                                .centerCrop()
                                .into(thumbs[i]);
                    }
                }

                // Click → full article
                final NewsArticle article = a;
                cards[i].setOnClickListener(v -> openArticle(article));

            } else {
                // No article for this slot → hide card
                cards[i].setVisibility(View.GONE);
            }
        }
    }

    private void openArticle(NewsArticle a) {
        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_id",    a.getId());
        intent.putExtra("article_title", a.getTitle());
        intent.putExtra("article_body",  a.getBody());
        intent.putExtra("article_image", a.getImageUrl());
        intent.putExtra("article_cat",   a.getCategory());
        intent.putExtra("article_time",  a.getPublishedAt());
        startActivity(intent);
    }

    // ── Hero Card Animation ───────────────────────────────────────────

    private void animateDateTimeCard() {
        if (cardDatetime == null) return;

        cardDatetime.setScaleX(0.88f);
        cardDatetime.setScaleY(0.88f);
        cardDatetime.setAlpha(0f);
        cardDatetime.setTranslationY(48f);
        cardDatetime.setCameraDistance(
                8000 * getResources().getDisplayMetrics().density);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardDatetime, "scaleX",  0.88f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardDatetime, "scaleY",  0.88f, 1.0f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(cardDatetime, "alpha",   0f,    1f);
        ObjectAnimator transY = ObjectAnimator.ofFloat(cardDatetime, "translationY", 48f, 0f);

        AnimatorSet phase1 = new AnimatorSet();
        phase1.playTogether(scaleX, scaleY, alpha, transY);
        phase1.setDuration(360);
        phase1.setInterpolator(new DecelerateInterpolator(2f));
        phase1.setStartDelay(180);

        ObjectAnimator rotX = ObjectAnimator.ofFloat(cardDatetime, "rotationX", 0f, -4f, 0f);
        rotX.setDuration(500);
        rotX.setInterpolator(new AccelerateDecelerateInterpolator());
        rotX.setStartDelay(phase1.getStartDelay() + phase1.getDuration() - 80);

        phase1.start();
        rotX.start();
    }

    // ── Live dot pulse ────────────────────────────────────────────────

    private void startLiveDotPulse() {
        if (viewLiveDot == null) return;

        liveDotRunnable = new Runnable() {
            boolean goingDown = true;

            @Override
            public void run() {
                float target = goingDown ? 0.15f : 1.0f;
                viewLiveDot.animate()
                        .alpha(target)
                        .setDuration(700)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                goingDown = !goingDown;
                liveDotHandler.postDelayed(this, 700);
            }
        };
        liveDotHandler.post(liveDotRunnable);
    }

    // ── Live clock ────────────────────────────────────────────────────

    private void startLiveClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Kathmandu"));

                SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
                SimpleDateFormat amPmFmt = new SimpleDateFormat("a",     Locale.ENGLISH);
                SimpleDateFormat dayFmt  = new SimpleDateFormat("EEEE",  Locale.ENGLISH);
                SimpleDateFormat secFmt  = new SimpleDateFormat(":ss",   Locale.ENGLISH);

                if (tvTimeMain != null)
                    tvTimeMain.setText(timeFmt.format(cal.getTime()));
                if (tvAmPm != null)
                    tvAmPm.setText(amPmFmt.format(cal.getTime()));
                if (tvDayName != null)
                    tvDayName.setText(dayFmt.format(cal.getTime()).toUpperCase(Locale.ENGLISH));
                if (tvSeconds != null)
                    tvSeconds.setText(secFmt.format(cal.getTime()));

                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    // ── Navigation ────────────────────────────────────────────────────

    private void setupNavigationListeners() {
        navView.setNavigationItemSelectedListener(this);

        // Quick action tiles
        setTileListener(R.id.qa_news,      null);  // opens SajhaPatrika
        setTileListener(R.id.qa_markets,   "Paisa & Bazaar — Coming Soon");
        setTileListener(R.id.qa_patro,     "Sajha Patro — Coming Soon");
        setTileListener(R.id.qa_nagarik,   "Nagarik Tools — Coming Soon");
        setTileListener(R.id.qa_emergency, "Apatkal Helplines — Coming Soon");
        setTileListener(R.id.qa_forex,     "Forex Rates — Coming Soon");

        // Sajha Patrika tile opens the real activity
        View qaNews = findViewById(R.id.qa_news);
        if (qaNews != null) qaNews.setOnClickListener(v -> openSajhaPatrika());

        // "See all" in carousel header → SajhaPatrikaActivity
        View seeAll = findViewById(R.id.tv_news_see_all);
        if (seeAll != null)
            seeAll.setOnClickListener(v -> openSajhaPatrika());
    }

    private void openSajhaPatrika() {
        startActivity(new Intent(this, SajhaPatrikaActivity.class));
    }

    private void setTileListener(int viewId, String message) {
        View v = findViewById(viewId);
        if (v != null && message != null)
            v.setOnClickListener(view ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        } else if (id == R.id.nav_patrika) {
            openSajhaPatrika();
        } else if (id == R.id.nav_markets) {
            Toast.makeText(this, "Paisa & Bazaar — Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_patro) {
            Toast.makeText(this, "Sajha Patro — Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_nagarik) {
            Toast.makeText(this, "Nagarik Tools — Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_emergency) {
            Toast.makeText(this, "Apatkal Helplines — Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings — Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_help) {
            Toast.makeText(this, "Help & Support — Coming Soon", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockRunnable);
        liveDotHandler.removeCallbacks(liveDotRunnable);
        if (tickerRunnable != null) tickerHandler.removeCallbacks(tickerRunnable);

        // Remove Firebase listener to prevent leaks
        if (carouselListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("news")
                    .removeEventListener(carouselListener);
        }
    }
}