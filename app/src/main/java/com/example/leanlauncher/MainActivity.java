package com.example.leanlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    static class App {
        String label, pkg, key;
        Intent intent;
        Drawable banner, icon;
    }

    private LinearLayout row;
    private HorizontalScrollView scroll;
    private String signature = "";
    private int screenW, screenH, tileW, tileH, itemPad;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = Math.max(dm.widthPixels, dm.heightPixels);
        screenH = Math.min(dm.widthPixels, dm.heightPixels);
        tileW = (int) (screenW / 5.4f);
        tileH = tileW * 9 / 16;
        itemPad = (int) (tileW * 0.07f);
        int pad = (int) (screenW * 0.05f);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF22346B, 0xFF0D1220, 0xFF06080D}));

        // Время и дата
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(pad, (int) (pad * 0.7f), pad, 0);

        TextClock time = new TextClock(this);
        time.setFormat24Hour("HH:mm");
        time.setFormat12Hour("HH:mm");
        time.setTextColor(Color.WHITE);
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, screenH * 0.11f);
        header.addView(time);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextColor(0xB3FFFFFF);
        date.setTextSize(TypedValue.COMPLEX_UNIT_PX, screenH * 0.04f);
        header.addView(date);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Ряд приложений
        scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(pad - itemPad, 0, pad, 0);

        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        scroll.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View bottom = new View(this);
        root.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (screenH * 0.12f)));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    @Override
    public void onBackPressed() { /* остаёмся на главном экране */ }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (row != null && row.getChildCount() > 0) {
            scroll.smoothScrollTo(0, 0);
            row.getChildAt(0).requestFocus();
        }
    }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
            if (list.isEmpty()) {
                q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                list = pm.queryIntentActivities(q, 0);
            }
            final List<App> out = new ArrayList<>();
            StringBuilder sig = new StringBuilder();
            for (ResolveInfo ri : list) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                App a = new App();
                a.pkg = pkg;
                a.key = pkg + "/" + ri.activityInfo.name;
                a.label = String.valueOf(ri.loadLabel(pm));
                try { a.banner = ri.activityInfo.loadBanner(pm); } catch (Exception ignored) { }
                a.icon = ri.loadIcon(pm);
                a.intent = new Intent(Intent.ACTION_MAIN)
                        .setClassName(pkg, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                out.add(a);
            }
            Collections.sort(out, new Comparator<App>() {
                public int compare(App x, App y) { return x.label.compareToIgnoreCase(y.label); }
            });
            for (App a : out) sig.append(a.key).append(';');
            final String newSig = sig.toString();
            runOnUiThread(() -> {
                if (newSig.equals(signature)) return;
                signature = newSig;
                row.removeAllViews();
                for (App a : out) row.addView(buildItem(a));
                if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
            });
        }).start();
    }

    private View buildItem(final App a) {
        final float radius = tileH * 0.12f;

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setClipChildren(false);
        item.setPadding(itemPad, itemPad, itemPad, 0);
        item.setFocusable(true);
        item.setClickable(true);

        final FrameLayout tile = new FrameLayout(this);
        GradientDrawable tileBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF3C4A6E, 0xFF1D2438});
        tile.setBackground(tileBg);
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        tile.setClipToOutline(true);

        ImageView iv = new ImageView(this);
        if (a.banner != null) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(a.banner);
            tile.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            int s = (int) (tileH * 0.58f);
            iv.setImageDrawable(a.icon);
            tile.addView(iv, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));
        }

        final View ring = new View(this);
        GradientDrawable stroke = new GradientDrawable();
        stroke.setColor(Color.TRANSPARENT);
        stroke.setCornerRadius(radius);
        stroke.setStroke(Math.max(3, (int) (tileH * 0.035f)), Color.WHITE);
        ring.setBackground(stroke);
        ring.setAlpha(0f);
        tile.addView(ring, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        item.addView(tile, new LinearLayout.LayoutParams(tileW, tileH));

        final TextView label = new TextView(this);
        label.setText(a.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, tileH * 0.15f);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                tileW, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (tileH * 0.16f);
        item.addView(label, lp);

        final float lift = tileH * 0.12f;
        item.setOnFocusChangeListener((v, has) -> {
            DecelerateInterpolator ip = new DecelerateInterpolator();
            tile.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f)
                    .z(has ? lift : 0f).setDuration(180).setInterpolator(ip).start();
            ring.animate().alpha(has ? 1f : 0f).setDuration(180).start();
            label.animate().alpha(has ? 1f : 0f).setDuration(180).start();
        });

        item.setOnClickListener(v -> {
            try { startActivity(a.intent); } catch (ActivityNotFoundException ignored) { }
        });
        item.setOnLongClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + a.pkg)));
            return true;
        });
        return item;
    }
}
