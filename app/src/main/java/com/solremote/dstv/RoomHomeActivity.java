package com.solremote.dstv;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Device chooser. The original DStv activity is opened without changing its controls. */
public final class RoomHomeActivity extends Activity {
    private int background, surface, border, ink, muted, accent;
    private boolean dark;
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) { super.onCreate(state); }

    @Override protected void onResume() {
        super.onResume();
        // Re-read the original theme preference when returning from either remote.
        int mode = getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("theme", 0);
        dark = mode == 2 || (mode == 0 &&
                (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES);
        background = Color.parseColor(dark ? "#101412" : "#F6F2EB");
        surface = Color.parseColor(dark ? "#1B211D" : "#E9E1D5");
        border = Color.parseColor(dark ? "#364138" : "#CFC2B0");
        ink = Color.parseColor(dark ? "#F0F2E9" : "#2D2821");
        muted = Color.parseColor(dark ? "#A3AEA1" : "#6D6254");
        accent = Color.parseColor(dark ? "#D8E7B9" : "#76551F");
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (android.os.Build.VERSION.SDK_INT >= 26 && !dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if (android.os.Build.VERSION.SDK_INT >= 29) getWindow().getDecorView().setForceDarkAllowed(false);
        draw();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void draw() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setPadding(dp(24), dp(36), dp(24), dp(28));
        scroll.addView(main);
        TextView brand = text("S / L     OFFLINE CONTROL", 10, accent);
        brand.setLetterSpacing(0.15f);
        brand.setGravity(Gravity.CENTER);
        main.addView(brand);
        gap(main, 22);
        TextView title = text("Sol’s Living Room", 30, ink);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        if (android.os.Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        main.addView(title);
        gap(main, 12);
        TextView prompt = text("Choose a remote", 14, muted);
        prompt.setGravity(Gravity.CENTER);
        main.addView(prompt);
        gap(main, 32);
        addDevice(main, "DStv HD 8S", "Decoder remote", "Open DStv HD 8S remote", MainActivity.class);
        gap(main, 16);
        addDevice(main, "LG TV", "60LA6208 · Test profile", "Open LG TV remote", LgTvActivity.class);
        gap(main, 26);
        TextView help = text("Use Back to return here from either remote.", 12, muted);
        help.setGravity(Gravity.CENTER);
        main.addView(help);
        gap(main, 32);
        TextView foot = text("zibah@2026", 11, muted);
        foot.setLetterSpacing(0.12f);
        foot.setGravity(Gravity.CENTER);
        main.addView(foot);
        setContentView(scroll);
    }

    private void addDevice(LinearLayout parent, String name, String detail,
            String description, Class<? extends Activity> target) {
        LinearLayout card = new LinearLayout(this) {
            @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                info.setClassName(Button.class.getName());
            }
        };
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(24), dp(24), dp(24));
        card.setFocusable(true);
        card.setContentDescription(description);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(surface);
        shape.setCornerRadius(dp(24));
        shape.setStroke(dp(1), border);
        card.setBackground(new RippleDrawable(ColorStateList.valueOf(dark ? 0x24FFFFFF : 0x1876551F), shape, null));
        card.setOnClickListener(v -> startActivity(new Intent(this, target)));
        TextView title = text(name + "  →", 22, ink);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(title);
        gap(card, 8);
        TextView sub = text(detail, 12, muted);
        sub.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(sub);
        int width = Math.min(dp(344), getResources().getDisplayMetrics().widthPixels - dp(48));
        parent.addView(card, new LinearLayout.LayoutParams(width, -2));
    }
}
