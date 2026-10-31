package com.solremote.dstv;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/** Separate LG remote using the unmodified user-supplied 60LA6208 test profile. */
public class LgTvActivity extends Activity {
    private final Map<String, int[]> patterns = new HashMap<>();
    private final Map<String, Integer> carriers = new HashMap<>();
    private int background, surface, raised, border, accent, ink, muted, warning;
    private boolean dark;
    private ConsumerIrManager ir;
    private TextView status;
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        configurePalette();
        try { loadCodes(); } catch (Exception e) {
            Toast.makeText(this, "IR data unavailable: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        draw();
    }

    private boolean ready() { return ir != null && ir.hasIrEmitter() && !patterns.isEmpty(); }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setFontFeatureSettings("kern");
        v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        v.setIncludeFontPadding(false);
        return v;
    }

    private TextView eyebrow(String value) {
        TextView v = text(value, 10, muted);
        v.setLetterSpacing(0.18f);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return v;
    }

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void add(LinearLayout parent, View child, int height) {
        parent.addView(child, new LinearLayout.LayoutParams(-1, height < 0 ? height : dp(height)));
    }

    private Button baseButton(String title, String description, int size) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(size);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(ink);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setPadding(dp(6), dp(4), dp(6), dp(4));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setStateListAnimator(null);
        b.setContentDescription(description);
        return b;
    }

    private void styleButton(View b, int color, int radius, int stroke) {
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(dark ? 0x24FFFFFF : 0x1876551F),
                shape(color, radius, stroke), null));
        // Keep device button styles from tinting our custom key surfaces.
        b.setBackgroundTintList(null);
        if (android.os.Build.VERSION.SDK_INT >= 29) b.setForceDarkAllowed(false);
    }

    private Button control(String title, String command, String description, int size, boolean primary) {
        Button b = baseButton(title, description, size);
        styleButton(b, primary ? accent : raised, 18, primary ? 0 : border);
        b.setTextColor(primary ? background : ink);
        if (!dark) b.setElevation(dp(primary ? 2 : 1));
        bindCommand(b, command, description);
        return b;
    }

    private void bindCommand(View key, String command, String description) {
        key.setTag(command);
        key.setEnabled(ready() && patterns.containsKey(command));
        // Keep the remote legible on devices without an emitter; status explains availability.
        if (!patterns.containsKey(command)) {
            key.setAlpha(0.35f);
            key.setContentDescription(description + ": IR code not added yet");
        }
        key.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            send(command);
        });
    }

    private void configurePalette() {
        int mode = getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("theme", 0);
        dark = mode == 2 || (mode == 0 &&
                (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES);
        background = Color.parseColor(dark ? "#101412" : "#F6F2EB");
        surface = Color.parseColor(dark ? "#1B211D" : "#E9E1D5");
        raised = Color.parseColor(dark ? "#262E28" : "#FFFDFA");
        border = Color.parseColor(dark ? "#364138" : "#CFC2B0");
        accent = Color.parseColor(dark ? "#D8E7B9" : "#76551F");
        ink = Color.parseColor(dark ? "#F0F2E9" : "#2D2821");
        muted = Color.parseColor(dark ? "#A3AEA1" : "#6D6254");
        warning = Color.parseColor(dark ? "#E8C593" : "#825015");
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (android.os.Build.VERSION.SDK_INT >= 26 && !dark) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            getWindow().getDecorView().setForceDarkAllowed(false);
        }
    }

    private void chooseTheme() {
        int mode = getSharedPreferences("MainActivity", MODE_PRIVATE).getInt("theme", 0);
        new android.app.AlertDialog.Builder(this, dark ?
                android.R.style.Theme_Material_Dialog_Alert :
                android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle("Appearance")
                .setSingleChoiceItems(new String[]{"Use device setting", "Light", "Dark"}, mode,
                    (dialog, selected) -> {
                        getSharedPreferences("MainActivity", MODE_PRIVATE).edit().putInt("theme", selected).apply();
                        dialog.dismiss();
                        recreate();
                    })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void draw() {
        ScrollView scroll = new ScrollView(this);
        scroll.setId(R.id.remote_scroll);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(background);
        LinearLayout main = column();
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setPadding(dp(16), dp(14), dp(16), dp(24));
        scroll.addView(main);
        int width = Math.min(dp(344), getResources().getDisplayMetrics().widthPixels - dp(32));

        LinearLayout heading = row();
        TextView title = text("Sol’s Living Room", 24, ink);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        markHeading(title);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button theme = baseButton("◐", "Change appearance", 24);
        styleButton(theme, surface, 16, border);
        theme.setOnClickListener(v -> chooseTheme());
        heading.addView(theme, new LinearLayout.LayoutParams(dp(48), dp(48)));
        main.addView(heading, new LinearLayout.LayoutParams(width, -2));
        gap(main, 8);
        TextView subtitle = text("LG TV remote · Offline", 11, muted);
        subtitle.setGravity(Gravity.CENTER);
        add(main, subtitle, -2);
        gap(main, 18);

        LinearLayout handset = column();
        handset.setPadding(dp(18), dp(22), dp(18), dp(22));
        handset.setBackground(shape(surface, 38, border));
        main.addView(handset, new LinearLayout.LayoutParams(width, -2));
        LinearLayout device = row();
        TextView brand = eyebrow("S / L");
        brand.setTextColor(accent);
        device.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        device.addView(eyebrow("LG 60LA6208"));
        add(handset, device, -2);
        gap(handset, 20);
        drawRemote(handset);
        gap(handset, 22);
        View divider = new View(this);
        divider.setBackgroundColor(border);
        handset.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        gap(handset, 18);
        drawKeypad(handset);
        gap(handset, 22);
        status = text(ready() ? "●  Ready to send" :
                (patterns.isEmpty() ? "IR codes unavailable" : "No IR blaster detected"), 11,
                ready() ? accent : warning);
        status.setGravity(Gravity.CENTER);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(handset, status, -2);
        gap(handset, 14);
        TextView foot = text("zibah@2026", 11, muted);
        foot.setLetterSpacing(0.12f);
        foot.setGravity(Gravity.CENTER);
        add(handset, foot, -2);
        gap(main, 16);
        TextView coming = text("Test profile · Try Power and Volume first", 11, muted);
        coming.setGravity(Gravity.CENTER);
        add(main, coming, -2);
        gap(main, 12);
        Button devices = baseButton("Switch remote", "Choose a different remote", 13);
        styleButton(devices, surface, 18, border);
        devices.setOnClickListener(v -> finish());
        main.addView(devices, new LinearLayout.LayoutParams(width, dp(48)));
        setContentView(scroll);
    }

    private void drawRemote(LinearLayout parent) {
        LinearLayout power = row();
        weighted(power, control("Power", "POWER", "TV power", 13, true), 48, true);
        weighted(power, control("Input", "INPUT", "TV input source", 13, false), 48, true);
        weighted(power, control("TV", "TV", "TV tuner", 13, false), 48, false);
        add(parent, power, -2);
        gap(parent, 8);
        LinearLayout shortcuts = row();
        weighted(shortcuts, control("Home", "HOME", "TV home", 13, false), 48, true);
        weighted(shortcuts, control("Settings", "SETTINGS", "TV settings", 12, false), 48, true);
        weighted(shortcuts, control("Guide", "GUIDE", "TV programme guide", 13, false), 48, false);
        add(parent, shortcuts, -2);
        gap(parent, 16);
        int size = Math.min(dp(232), getResources().getDisplayMetrics().widthPixels - dp(80));
        FrameLayout pad = new FrameLayout(this);
        // A physical left key stays on the left even when the phone language is RTL.
        pad.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        pad.setBackground(shape(raised, 160, border));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(size, size);
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        parent.addView(pad, pp);
        direction(pad, "↑", "UP", "Up", Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        direction(pad, "↓", "DOWN", "Down", Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        direction(pad, "←", "LEFT", "Left", Gravity.START | Gravity.CENTER_VERTICAL);
        direction(pad, "→", "RIGHT", "Right", Gravity.END | Gravity.CENTER_VERTICAL);
        Button ok = control("OK", "OK", "Select", 18, true);
        styleButton(ok, accent, 60, 0);
        pad.addView(ok, new FrameLayout.LayoutParams(dp(80), dp(80), Gravity.CENTER));
        gap(parent, 16);
        LinearLayout utility = row();
        weighted(utility, control("Back", "BACK", "TV back", 13, false), 48, true);
        weighted(utility, control("Exit", "EXIT", "TV exit", 13, false), 48, true);
        weighted(utility, control("Info", "INFO", "TV information", 13, false), 48, false);
        add(parent, utility, -2);
        gap(parent, 12);
        LinearLayout sound = row();
        weighted(sound, control("−", "VOL_MINUS", "Volume down", 26, false), 56, true);
        weighted(sound, control("Mute", "MUTE", "Mute", 13, false), 56, true);
        weighted(sound, control("+", "VOL_PLUS", "Volume up", 24, false), 56, false);
        add(parent, sound, -2);
        gap(parent, 8);
        LinearLayout channels = row();
        weighted(channels, control("CH −", "CHANNEL_MINUS", "TV channel down", 14, false), 48, true);
        weighted(channels, control("CH +", "CHANNEL_PLUS", "TV channel up", 14, false), 48, false);
        add(parent, channels, -2);
    }

    private void direction(FrameLayout pad, String title, String command, String description, int gravity) {
        Button b = control(title, command, description, 27, false);
        styleButton(b, raised, 40, 0);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(64), dp(64), gravity);
        p.setMargins(dp(6), dp(6), dp(6), dp(6));
        pad.addView(b, p);
    }

    private void weighted(LinearLayout row, View child, int height, boolean trailingGap) {
        int keyHeight = Math.max(dp(height), child.getMinimumHeight());
        if (child instanceof TextView) {
            TextView label = (TextView) child;
            android.graphics.Paint.FontMetricsInt metrics = label.getPaint().getFontMetricsInt();
            keyHeight = Math.max(keyHeight, metrics.bottom - metrics.top +
                    label.getPaddingTop() + label.getPaddingBottom() + dp(4));
        }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, keyHeight, 1);
        if (trailingGap) p.setMarginEnd(dp(10));
        row.addView(child, p);
    }

    private void drawKeypad(LinearLayout keypadPanel) {
        for (int r = 0; r < 4; r++) {
            LinearLayout line = row();
            for (int c = 0; c < 3; c++) {
                int n = r * 3 + c + 1;
                if (r == 3 && c != 1) {
                    weighted(line, new View(this), 56, c < 2);
                } else {
                    if (r == 3) n = 0;
                    DigitKeyView digit = new DigitKeyView(this, n,
                            Color.parseColor(dark ? "#FFFFFF" : "#17120D"));
                    styleButton(digit, Color.parseColor(dark ? "#2C382F" : "#FFFFFF"),
                            18, Color.parseColor(dark ? "#7F907D" : "#9A8464"));
                    if (!dark) digit.setElevation(dp(1));
                    bindCommand(digit, "NUM_" + n, "Number " + n);
                    weighted(line, digit, 56, c < 2);
                }
            }
            add(keypadPanel, line, -2);
            if (r < 3) gap(keypadPanel, 8);
        }
        StringBuilder missing = new StringBuilder();
        for (int n = 0; n < 10; n++) {
            if (!patterns.containsKey("NUM_" + n)) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(n);
            }
        }
        if (missing.length() > 0) {
            gap(keypadPanel, 12);
            add(keypadPanel, text("IR codes needed for: " + missing, 12, warning), -2);
        }
    }

    private void send(String key) {
        if (!ready() || !patterns.containsKey(key)) return;
        try {
            ir.transmit(carriers.get(key), patterns.get(key));
            status.setText("Sent · " + key.replace("NUM_", "").replace('_', ' '));
            status.setTextColor(accent);
        } catch (RuntimeException e) {
            status.setText("Couldn’t send. Try again.");
            status.setTextColor(warning);
            Toast.makeText(this, "IR transmission failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void markHeading(TextView view) {
        if (android.os.Build.VERSION.SDK_INT >= 28) view.setAccessibilityHeading(true);
    }

    private void loadCodes() throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                getAssets().open("lg_60la6208_tv_ir_test.ir")))) {
            String line, name = null;
            int frequency = 38000;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("name: ")) {
                    name = line.substring(6).trim();
                    frequency = 38000;
                } else if (line.startsWith("frequency: ")) {
                    frequency = Integer.parseInt(line.substring(11).trim());
                } else if (line.startsWith("data: ") && name != null) {
                    String[] tokens = line.substring(6).trim().split("\\s+");
                    int[] data = new int[tokens.length];
                    long duration = 0;
                    for (int i=0;i<tokens.length;i++) {
                        data[i] = Integer.parseInt(tokens[i]);
                        if (data[i] <= 0) throw new IllegalArgumentException("Invalid IR timing");
                        duration += data[i];
                    }
                    if (duration >= 2000000) throw new IllegalArgumentException("IR command too long");
                    patterns.put(name, data);
                    carriers.put(name, frequency);
                }
            }
        }
    }

}
