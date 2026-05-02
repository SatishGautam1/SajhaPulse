package com.nighttech.sajhapulse.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.ui.adapters.MarketsViewPagerAdapter;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

/**
 * MarketsActivity — "Paisa &amp; Bazaar" screen.
 *
 * Three tabs wired via ViewPager2 + TabLayoutMediator:
 *   Tab 0 → NEPSE    (index hero + gainers/losers list)
 *   Tab 1 → Forex    (NRB exchange rates)
 *   Tab 2 → Metals   (Gold / Silver prices)
 *
 * Launched from:
 *   • MainActivity.openMarketsActivity()
 *   • nav_drawer_menu.xml → nav_markets item
 *   • Quick-action tile qa_markets
 *
 * In AndroidManifest.xml add:
 *   <activity android:name=".ui.MarketsActivity"
 *             android:parentActivityName=".ui.MainActivity" />
 */
public class MarketsActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"NEPSE", "Forex", "Metals"};

    private MaterialToolbar          toolbar;
    private TabLayout                tabLayout;
    private ViewPager2               viewPager;
    private MarketsViewPagerAdapter  pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markets);

        // Same crimson system bars as MainActivity
        SystemBarHelper.applyAppBars(this);

        bindViews();
        setupToolbar();
        setupViewPager();
    }

    // ── View binding ──────────────────────────────────────────────────

    private void bindViews() {
        toolbar    = findViewById(R.id.markets_toolbar);
        tabLayout  = findViewById(R.id.markets_tab_layout);
        viewPager  = findViewById(R.id.markets_view_pager);
    }

    // ── Toolbar ───────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Keep back arrow white on crimson bar
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── ViewPager2 + TabLayout ────────────────────────────────────────

    private void setupViewPager() {
        pagerAdapter = new MarketsViewPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Retain neighbouring fragments so switching tabs feels instant
        viewPager.setOffscreenPageLimit(2);

        // Wire tab titles to the pager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }
}