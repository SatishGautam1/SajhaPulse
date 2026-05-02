package com.nighttech.sajhapulse.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.nighttech.sajhapulse.ui.fragments.ForexFragment;
import com.nighttech.sajhapulse.ui.fragments.MetalsFragment;
import com.nighttech.sajhapulse.ui.fragments.NepseFragment;

/**
 * MarketsViewPagerAdapter — wires the three market tabs to ViewPager2.
 *
 *   Tab 0 → NepseFragment   (NEPSE index + movers list)
 *   Tab 1 → ForexFragment   (NRB exchange rates)
 *   Tab 2 → MetalsFragment  (Gold / Silver prices)
 */
public class MarketsViewPagerAdapter extends FragmentStateAdapter {

    public static final int TAB_COUNT   = 3;
    public static final int TAB_NEPSE   = 0;
    public static final int TAB_FOREX   = 1;
    public static final int TAB_METALS  = 2;

    public MarketsViewPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_FOREX:   return new ForexFragment();
            case TAB_METALS:  return new MetalsFragment();
            default:          return new NepseFragment();
        }
    }

    @Override
    public int getItemCount() { return TAB_COUNT; }
}