package com.nighttech.sajhapulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.ForexRate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ForexRateAdapter — drives the forex list RecyclerView in ForexFragment.
 *
 * Each row (item_forex_rate.xml) shows:
 *   • Flag emoji + ISO code  (e.g. 🇺🇸 USD)
 *   • Currency full name
 *   • Unit     (e.g. "per 1" or "per 100")
 *   • Buy rate  (NPR)
 *   • Sell rate (NPR)
 */
public class ForexRateAdapter
        extends RecyclerView.Adapter<ForexRateAdapter.ForexViewHolder> {

    private final List<ForexRate> data = new ArrayList<>();

    public void submitList(List<ForexRate> newList) {
        data.clear();
        data.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ForexViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forex_rate, parent, false);
        return new ForexViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ForexViewHolder h, int position) {
        h.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────

    static class ForexViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvFlag, tvIso, tvName, tvUnit, tvBuy, tvSell;

        ForexViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFlag = itemView.findViewById(R.id.tv_forex_flag);
            tvIso  = itemView.findViewById(R.id.tv_forex_iso);
            tvName = itemView.findViewById(R.id.tv_forex_name);
            tvUnit = itemView.findViewById(R.id.tv_forex_unit);
            tvBuy  = itemView.findViewById(R.id.tv_forex_buy);
            tvSell = itemView.findViewById(R.id.tv_forex_sell);
        }

        void bind(ForexRate r) {
            tvFlag.setText(r.getFlagEmoji());
            tvIso.setText(r.getIso3());
            tvName.setText(r.getName());
            tvUnit.setText("per " + r.getUnit());
            tvBuy.setText(String.format(Locale.ENGLISH, "%.2f", r.getBuy()));
            tvSell.setText(String.format(Locale.ENGLISH, "%.2f", r.getSell()));
        }
    }
}