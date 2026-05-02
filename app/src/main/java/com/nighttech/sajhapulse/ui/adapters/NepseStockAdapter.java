package com.nighttech.sajhapulse.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.data.models.StockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * NepseStockAdapter — drives the stock list RecyclerView in NepseFragment.
 *
 * Each row (item_stock_row.xml) shows:
 *   • Symbol  (bold, crimson)
 *   • Company short name
 *   • LTP (Last Traded Price)
 *   • Change % with ▲ / ▼ arrow tinted green / red
 *   • Volume (K / M formatted)
 *
 * Call {@link #submitList(List)} to diff-animate updates.
 */
public class NepseStockAdapter
        extends RecyclerView.Adapter<NepseStockAdapter.StockViewHolder> {

    public interface OnStockClickListener {
        void onStockClick(StockItem item);
    }

    private final List<StockItem>     data     = new ArrayList<>();
    private       OnStockClickListener listener;

    public NepseStockAdapter() {}

    public void setOnStockClickListener(OnStockClickListener l) {
        listener = l;
    }

    // ── DiffUtil update ───────────────────────────────────────────────

    public void submitList(List<StockItem> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return data.get(oldPos).getSymbol()
                        .equals(newList.get(newPos).getSymbol());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                StockItem o = data.get(oldPos);
                StockItem n = newList.get(newPos);
                return o.getLtp() == n.getLtp()
                        && o.getChangePercent() == n.getChangePercent();
            }
        });

        data.clear();
        data.addAll(newList);
        result.dispatchUpdatesTo(this);
    }

    // ── RecyclerView.Adapter ──────────────────────────────────────────

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_row, parent, false);
        return new StockViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder h, int position) {
        h.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────

    class StockViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSymbol, tvName, tvLtp, tvChange, tvVolume;

        StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tv_stock_symbol);
            tvName   = itemView.findViewById(R.id.tv_stock_name);
            tvLtp    = itemView.findViewById(R.id.tv_stock_ltp);
            tvChange = itemView.findViewById(R.id.tv_stock_change);
            tvVolume = itemView.findViewById(R.id.tv_stock_volume);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_ID)
                    listener.onStockClick(data.get(pos));
            });
        }

        void bind(StockItem s) {
            tvSymbol.setText(s.getSymbol());
            tvName.setText(shortName(s.getCompanyName()));

            tvLtp.setText(String.format(Locale.ENGLISH, "%.2f", s.getLtp()));

            double pct = s.getChangePercent();
            String arrow = pct >= 0 ? "▲" : "▼";
            tvChange.setText(String.format(Locale.ENGLISH,
                    "%s %.2f%%", arrow, Math.abs(pct)));
            tvChange.setTextColor(pct >= 0
                    ? Color.parseColor("#2E7D32")   // dark green
                    : Color.parseColor("#C62828"));  // dark red

            tvVolume.setText(formatVolume(s.getVolume()));
        }

        /** Truncates long company names to 20 chars for compact rows. */
        private String shortName(String full) {
            return (full != null && full.length() > 22)
                    ? full.substring(0, 20).trim() + "…"
                    : full;
        }

        /** Formats volume as e.g. "1.2M", "340K". */
        private String formatVolume(long v) {
            if (v >= 1_000_000) return String.format(Locale.ENGLISH, "%.1fM", v / 1_000_000.0);
            if (v >= 1_000)     return String.format(Locale.ENGLISH, "%.0fK", v / 1_000.0);
            return String.valueOf(v);
        }
    }
}