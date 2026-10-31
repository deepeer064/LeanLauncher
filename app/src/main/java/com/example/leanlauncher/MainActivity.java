package com.example.leanlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
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
        String label, pkg; Intent intent; Drawable icon;
    }

    private final List<App> apps = new ArrayList<>();
    private GridView grid;
    private Adapter adapter;
    private int dp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = (int) getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#101418"));
        root.setPadding(24 * dp, 24 * dp, 24 * dp, 0);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(28);
        clock.setGravity(Gravity.END);
        root.addView(clock, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setVerticalSpacing(12 * dp);
        grid.setHorizontalSpacing(12 * dp);
        grid.setPadding(0, 16 * dp, 0, 16 * dp);
        grid.setClipToPadding(false);
        grid.setSelector(rounded(0x44FFFFFF));
        grid.setDrawSelectorOnTop(false);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        adapter = new Adapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> startActivity(apps.get(pos).intent));
        grid.setOnItemLongClickListener((p, v, pos, id) -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + apps.get(pos).pkg));
            startActivity(i);
            return true;
        });
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
        if (grid != null) grid.smoothScrollToPosition(0);
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
            for (ResolveInfo ri : list) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                App a = new App();
                a.pkg = pkg;
                a.label = String.valueOf(ri.loadLabel(pm));
                a.icon = ri.loadIcon(pm);
                a.intent = new Intent(Intent.ACTION_MAIN)
                        .setClassName(pkg, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                out.add(a);
            }
            Collections.sort(out, new Comparator<App>() {
                public int compare(App x, App y) { return x.label.compareToIgnoreCase(y.label); }
            });
            runOnUiThread(() -> { apps.clear(); apps.addAll(out); adapter.notifyDataSetChanged(); });
        }).start();
    }

    private Drawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(12 * dp);
        return d;
    }

    class Adapter extends BaseAdapter {
        public int getCount() { return apps.size(); }
        public Object getItem(int i) { return apps.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View v, ViewGroup parent) {
            LinearLayout row;
            ImageView img; TextView tv;
            if (v == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                row.setPadding(8 * dp, 12 * dp, 8 * dp, 12 * dp);
                img = new ImageView(MainActivity.this);
                row.addView(img, new LinearLayout.LayoutParams(64 * dp, 64 * dp));
                tv = new TextView(MainActivity.this);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(14);
                tv.setSingleLine(true);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tv.setGravity(Gravity.CENTER);
                row.addView(tv, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                row = (LinearLayout) v;
                img = (ImageView) row.getChildAt(0);
                tv = (TextView) row.getChildAt(1);
            }
            App a = apps.get(pos);
            img.setImageDrawable(a.icon);
            tv.setText(a.label);
            return row;
        }
    }
}
