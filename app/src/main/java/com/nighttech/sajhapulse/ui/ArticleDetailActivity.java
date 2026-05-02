package com.nighttech.sajhapulse.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.nighttech.sajhapulse.R;
import com.nighttech.sajhapulse.utils.SystemBarHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ArticleDetailActivity — Full-screen article reader.
 *
 * Receives article data via Intent extras from SajhaPatrikaActivity.
 * For large bodies consider Firebase fetch-by-id if body is truncated.
 */
public class ArticleDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        SystemBarHelper.applyAppBars(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_article);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Unpack extras
        String title    = getIntent().getStringExtra("article_title");
        String body     = getIntent().getStringExtra("article_body");
        String imageUrl = getIntent().getStringExtra("article_image");
        String category = getIntent().getStringExtra("article_cat");
        long   pubMs    = getIntent().getLongExtra("article_time", 0);

        // Bind views
        ImageView ivHero    = findViewById(R.id.article_hero_image);
        TextView  tvCat     = findViewById(R.id.article_category);
        TextView  tvTitle   = findViewById(R.id.article_title);
        TextView  tvTime    = findViewById(R.id.article_time);
        TextView  tvBody    = findViewById(R.id.article_body);

        if (tvTitle    != null) tvTitle.setText(title);
        if (tvCat      != null) tvCat.setText(category);
        if (tvBody     != null) tvBody.setText(body);

        if (tvTime != null && pubMs > 0) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH);
            tvTime.setText(sdf.format(new Date(pubMs)));
        }

        if (ivHero != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.color.md_theme_primaryContainer)
                        .centerCrop()
                        .into(ivHero);
            } else {
                ivHero.setVisibility(View.GONE);
            }
        }
    }
}