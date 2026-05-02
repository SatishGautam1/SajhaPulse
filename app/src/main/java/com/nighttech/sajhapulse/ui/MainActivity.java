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

// ── Firestore only — Realtime Database removed ────────────────────────
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.NewsArticle;
import com.nighttech.sajhapulse.utils.NepaliCalendarUtils;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * MainActivity — SajhaPulse Home Dashboard.
 *
 * ── FIXES IN THIS VERSION ─────────────────────────────────────────────
 *
 * FIX 1 — Carousel was using Firebase Realtime Database
 *   loadCarouselNews() was querying FirebaseDatabase (/news/).
 *   But all data is now in Firestore. The Realtime DB query returned
 *   nothing, and the Firestore @DocumentId annotation on NewsArticle
 *   broke Realtime DB deserialization → crash or empty carousel.
 *   FIXED: loadCarouselNews() now queries Firestore collection "news",
 *          uses a ListenerRegistration (real-time), and toObject().
 *
 * FIX 2 — All Firebase Realtime Database imports removed
 *   FirebaseDatabase, DataSnapshot, DatabaseError, ValueEventListener
 *   are no longer imported or used anywhere in this file.
 * ─────────────────────────────────────────────────────────────────────
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
    private LinearLayout     newsLiveBadge1;

    // ── Firebase Auth ─────────────────────────────────────────────────
    private FirebaseAuth mAuth;

    // ── Firestore carousel listener ───────────────────────────────────
    // ListenerRegistration replaces ValueEventListener — removed in onDestroy()
    private ListenerRegistration carouselRegistration;
    private final List<NewsArticle> carouselArticles = new ArrayList<>();

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

        startLiveClock();
        updateCalendarInfo();
        animateDateTimeCard();
        startLiveDotPulse();

        // Live market ticker — fetches real NRB data
        tickerRunnable = MainActivityExtensions.startMarqueeTicker(
                tickerHandler,
                tvLiveTicker,
                tvNepseValue, tvNepseChange,
                tvUsdValue,   tvUsdChange,
                tvGoldValue,  tvGoldChange,
                tvTickerTimestamp);

        // ── News carousel — Firestore ──────────────────────────────────
        loadCarouselNews();
    }

    // ── View binding ──────────────────────────────────────────────────

    private void bindViews() {
        drawerLayout    = findViewById(R.id.drawer_layout);
        navView         = findViewById(R.id.nav_view);
        toolbar         = findViewById(R.id.toolbar);
        appBarLayout    = findViewById(R.id.app_bar);
        scrollDashboard = findViewById(R.id.scroll_dashboard);

        tvTimeMain    = findViewById(R.id.tv_time_main);
        tvAmPm        = findViewById(R.id.tv_am_pm);
        tvSeconds     = findViewById(R.id.tv_seconds);
        tvDayName     = findViewById(R.id.tv_day_name);
        tvBsDate      = findViewById(R.id.tv_bs_date);
        tvBsDateLatin = findViewById(R.id.tv_bs_date_latin);
        tvAdDate      = findViewById(R.id.tv_ad_date);
        cardDatetime  = findViewById(R.id.card_datetime);
        viewLiveDot   = findViewById(R.id.view_live_dot);

        tvGreetingLabel = findViewById(R.id.tv_greeting_label);
        tvGreetingName  = findViewById(R.id.tv_greeting_name);
        ivGreetingIcon  = findViewById(R.id.iv_greeting_icon);

        tvLiveTicker      = findViewById(R.id.tv_live_ticker);
        tvNepseValue      = findViewById(R.id.tv_nepse_value);
        tvNepseChange     = findViewById(R.id.tv_nepse_change);
        tvUsdValue        = findViewById(R.id.tv_usd_value);
        tvUsdChange       = findViewById(R.id.tv_usd_change);
        tvGoldValue       = findViewById(R.id.tv_gold_value);
        tvGoldChange      = findViewById(R.id.tv_gold_change);
        tvTickerTimestamp = findViewById(R.id.tv_ticker_timestamp);

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

    // ── BS Calendar ───────────────────────────────────────────────────

    private void updateCalendarInfo() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
        SimpleDateFormat adFormat = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
        if (tvAdDate != null) tvAdDate.setText(adFormat.format(cal.getTime()));

        int[] bs = NepaliCalendarUtils.todayBs();
        if (tvBsDate      != null) tvBsDate.setText(NepaliCalendarUtils.toDevanagariDateString(bs));
        if (tvBsDateLatin != null) tvBsDateLatin.setText(NepaliCalendarUtils.toLatinDateString(bs));
    }

    // ── News Carousel — Firestore ─────────────────────────────────────

    /**
     * Fetches the 3 most recent active articles from Firestore and
     * populates the carousel cards. Uses a real-time snapshot listener
     * so cards update instantly when new articles are published.
     *
     * Firestore path  : /news/
     * Filter          : isActive == true
     * Order           : publishedAt DESC
     * Limit           : 3
     */
    private void loadCarouselNews() {
        Query query = FirebaseFirestore.getInstance()
                .collection("news")
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .limit(10); // fetch 10, show first 3 active ones

        carouselRegistration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;  // keep last-shown cards on error

            carouselArticles.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc
                    : snapshots.getDocuments()) {
                try {
                    NewsArticle a = doc.toObject(NewsArticle.class);
                    if (a != null && a.isActive()) {   // filter inactive here
                        if (a.getId() == null || a.getId().isEmpty()) a.setId(doc.getId());
                        carouselArticles.add(a);
                        if (carouselArticles.size() == 3) break; // only need 3
                    }
                } catch (Exception ignored) { /* skip malformed doc */ }
            }
            bindCarouselCards();
        });
    }

    private void bindCarouselCards() {
        MaterialCardView[] cards  = {newsCard1,  newsCard2,  newsCard3};
        ImageView[]        thumbs = {newsThumb1, newsThumb2, newsThumb3};
        TextView[]         titles = {newsTitle1, newsTitle2, newsTitle3};
        TextView[]         cats   = {newsCat1,   newsCat2,   newsCat3};

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;

            if (i < carouselArticles.size()) {
                NewsArticle a = carouselArticles.get(i);
                cards[i].setVisibility(View.VISIBLE);

                if (titles[i] != null) titles[i].setText(a.getTitle());
                if (cats[i]   != null) cats[i].setText(a.getCategory());

                // LIVE badge on card 1 only
                if (i == 0 && newsLiveBadge1 != null)
                    newsLiveBadge1.setVisibility(a.isBreaking() ? View.VISIBLE : View.GONE);

                // Thumbnail via Glide
                if (thumbs[i] != null) {
                    String url = a.getImageUrl();
                    if (url != null && !url.isEmpty()) {
                        Glide.with(this)
                                .load(url)
                                .placeholder(R.color.md_theme_primaryContainer)
                                .error(R.color.md_theme_primaryContainer)
                                .centerCrop()
                                .into(thumbs[i]);
                    } else {
                        thumbs[i].setImageResource(R.color.md_theme_primaryContainer);
                    }
                }

                final NewsArticle article = a;
                cards[i].setOnClickListener(v -> openArticle(article));

            } else {
                cards[i].setVisibility(View.GONE);
            }
        }
    }

    private void openArticle(NewsArticle a) {
        Intent intent = new Intent(this, ArticleDetailActivity.class);
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
    }

    // ── Hero Card Animation ───────────────────────────────────────────

    private void animateDateTimeCard() {
        if (cardDatetime == null) return;

        cardDatetime.setScaleX(0.88f);
        cardDatetime.setScaleY(0.88f);
        cardDatetime.setAlpha(0f);
        cardDatetime.setTranslationY(48f);
        cardDatetime.setCameraDistance(8000 * getResources().getDisplayMetrics().density);

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
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
                SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
                SimpleDateFormat amPmFmt = new SimpleDateFormat("a",     Locale.ENGLISH);
                SimpleDateFormat dayFmt  = new SimpleDateFormat("EEEE",  Locale.ENGLISH);
                SimpleDateFormat secFmt  = new SimpleDateFormat(":ss",   Locale.ENGLISH);

                if (tvTimeMain != null) tvTimeMain.setText(timeFmt.format(cal.getTime()));
                if (tvAmPm    != null) tvAmPm.setText(amPmFmt.format(cal.getTime()));
                if (tvDayName != null) tvDayName.setText(dayFmt.format(cal.getTime()).toUpperCase(Locale.ENGLISH));
                if (tvSeconds != null) tvSeconds.setText(secFmt.format(cal.getTime()));

                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    // ── Navigation ────────────────────────────────────────────────────

    private void setupNavigationListeners() {
        navView.setNavigationItemSelectedListener(this);

        View qaNews = findViewById(R.id.qa_news);
        if (qaNews != null) qaNews.setOnClickListener(v -> openSajhaPatrika());

        View qaMarkets = findViewById(R.id.qa_markets);
        if (qaMarkets != null) qaMarkets.setOnClickListener(v ->
                startActivity(new Intent(this, MarketsActivity.class)));

        setTileListener(R.id.qa_patro,     "Sajha Patro — Coming Soon");
        setTileListener(R.id.qa_nagarik,   "Nagarik Tools — Coming Soon");
        setTileListener(R.id.qa_emergency, "Apatkal Helplines — Coming Soon");
        setTileListener(R.id.qa_forex,     "Forex Rates — Coming Soon");

        View seeAll = findViewById(R.id.tv_news_see_all);
        if (seeAll != null) seeAll.setOnClickListener(v -> openSajhaPatrika());
    }

    private void openSajhaPatrika() {
        startActivity(new Intent(this, SajhaPatrikaActivity.class));
    }

    private void openMarketsActivity() {
        startActivity(new Intent(this, MarketsActivity.class));
    }

    private void setTileListener(int viewId, String message) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(view ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if      (id == R.id.nav_logout)    { signOut(); }
        else if (id == R.id.nav_patrika)   { openSajhaPatrika(); }
        else if (id == R.id.nav_markets)   { openMarketsActivity(); }   // ← fixed
        else if (id == R.id.nav_patro)     { toast("Sajha Patro — Coming Soon"); }
        else if (id == R.id.nav_nagarik)   { toast("Nagarik Tools — Coming Soon"); }
        else if (id == R.id.nav_emergency) { toast("Apatkal Helplines — Coming Soon"); }
        else if (id == R.id.nav_settings)  { toast("Settings — Coming Soon"); }
        else if (id == R.id.nav_help)      { toast("Help & Support — Coming Soon"); }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        if (tickerRunnable  != null) tickerHandler.removeCallbacks(tickerRunnable);

        // Stop Firestore real-time listener — prevents memory leaks & billing
        if (carouselRegistration != null) carouselRegistration.remove();
    }
}