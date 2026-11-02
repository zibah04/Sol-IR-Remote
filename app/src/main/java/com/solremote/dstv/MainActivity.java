package com.solremote.dstv;

import android.app.Activity;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/** Offline remote using the tested waveforms in assets/dstv_8s_final_working.ir. */
public class MainActivity extends Activity {
    private final Map<String, int[]> patterns = new HashMap<>();
    private final Map<String, Integer> carriers = new HashMap<>();
    private ConsumerIrManager ir;
    private TextView status;
    private final int background = Color.rgb(10,17,31);
    private final int panel = Color.rgb(23,35,54);
    private final int accent = Color.rgb(53,215,170);
    private int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        try { loadCodes(); } catch (Exception e) {
            Toast.makeText(this, "IR data unavailable: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        draw();
    }

    private void loadCodes() throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                getAssets().open("dstv_8s_final_working.ir")))) {
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

    private boolean ready() { return ir != null && ir.hasIrEmitter() && !patterns.isEmpty(); }

    private TextView label(String txt, int size, int color) {
        TextView t = new TextView(this);
        t.setText(txt);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        return d;
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER);
        parent.addView(r);
        return r;
    }

    private void button(LinearLayout row, String title, String command) {
        if (command == null) {
            View blank = new View(this);
            row.addView(blank, new LinearLayout.LayoutParams(0, dp(65), 1));
            return;
        }
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(title);
        b.setTextSize(16);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor((command.equals("OK") || command.equals("VOL_PLUS")) ? background : Color.WHITE);
        b.setBackground(shape((command.equals("OK") || command.equals("VOL_PLUS")) ? accent : panel));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(65), 1);
        p.setMargins(dp(4), dp(5), dp(4), dp(5));
        b.setEnabled(ready() && patterns.containsKey(command));
        b.setOnClickListener(v -> send(command));
        row.addView(b,p);
    }

    private String displayName(String command) {
        if (command == null) return "";
        if (command.startsWith("NUM_")) return command.substring(4);
        switch (command) {
            case "POWER": return "Power";
            case "DSTV_MENU": return "DStv";
            case "TV_GUIDE": return "Guide";
            case "OPTIONS": return "Options";
            case "CHANNEL_UP": return "CH +";
            case "CHANNEL_DOWN": return "CH −";
            case "EXIT": return "Back / Exit";
            case "VOL_PLUS": return "VOL +";
            case "VOL_MINUS": return "VOL −";
            case "MUTE": return "Mute";
            case "UP": return "▲";
            case "DOWN": return "▼";
            case "LEFT": return "◀";
            case "RIGHT": return "▶";
            default: return command;
        }
    }

    private void section(LinearLayout parent, String title, String[]... groups) {
        TextView header = label(title,14,accent);
        header.setGravity(Gravity.START);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,dp(20),0,dp(8));
        parent.addView(header,p);
        for(String[] group:groups) {
            LinearLayout r=row(parent);
            for (int i=0;i<group.length;i++) {
                String command = group[i].isEmpty() ? null : group[i];
                button(r,displayName(command),command);
            }
        }
    }

    /**
     * Two tall, side-by-side rockers to match the physical DStv remote layout.
     * This changes only the visual presentation; the existing IR signal keys
     * and waveforms are reused without modification.
     */
    private void rockerButton(LinearLayout container, String symbol, String command,
                              String description) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(symbol);
        b.setTextSize(27);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setContentDescription(description);
        b.setBackground(shape(Color.rgb(36, 53, 73)));
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(67));
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setEnabled(ready() && patterns.containsKey(command));
        b.setOnClickListener(v -> send(command));
        container.addView(b, params);
    }

    private void rocker(LinearLayout container, String name,
                        String upCommand, String downCommand) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(dp(8), dp(10), dp(8), dp(10));
        group.setBackground(shape(panel));

        LinearLayout.LayoutParams groupParams =
                new LinearLayout.LayoutParams(0, -2, 1);
        groupParams.setMargins(dp(4), 0, dp(4), 0);
        container.addView(group, groupParams);

        TextView heading = label(name, 13, accent);
        heading.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(-1, dp(28));
        group.addView(heading, headingParams);

        rockerButton(group, "+", upCommand, name + " up");
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(73, 90, 110));
        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.setMargins(dp(14), 0, dp(14), 0);
        group.addView(divider, dividerParams);
        rockerButton(group, "−", downCommand, name + " down");
    }

    private void volumeAndChannelRockers(LinearLayout parent) {
        TextView heading = label("VOLUME & CHANNEL", 14, accent);
        heading.setGravity(Gravity.START);
        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(-1, -2);
        headerParams.setMargins(0, dp(20), 0, dp(8));
        parent.addView(heading, headerParams);

        LinearLayout rockers = new LinearLayout(this);
        rockers.setOrientation(LinearLayout.HORIZONTAL);
        rockers.setGravity(Gravity.TOP);
        parent.addView(rockers, new LinearLayout.LayoutParams(-1, -2));

        // Physical-remote arrangement: volume on the left, channel on the right.
        rocker(rockers, "VOL", "VOL_PLUS", "VOL_MINUS");
        rocker(rockers, "CH", "CHANNEL_UP", "CHANNEL_DOWN");
    }

    private void draw() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(background);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(18),dp(24),dp(18),dp(30));
        scroll.addView(main);
        TextView title=label("SOL IR REMOTE",26,Color.WHITE);
        title.setTypeface(null,Typeface.BOLD);
        main.addView(title);
        TextView subtitle=label("DStv HD 8S · Offline",14,accent);
        main.addView(subtitle);
        status=label(ready()?"IR blaster ready":"IR blaster missing or codes unavailable",13,
                ready()?accent:Color.RED);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2);
        sp.setMargins(0,dp(16),0,0);
        main.addView(status,sp);
        section(main,"DECODER",
            new String[]{"POWER","DSTV_MENU","TV_GUIDE"});
        section(main,"NAVIGATION",
            new String[]{"","UP",""},
            new String[]{"LEFT","OK","RIGHT"},
            new String[]{"INFO","DOWN","EXIT"});
        volumeAndChannelRockers(main);
        section(main,"QUICK CONTROLS",
            new String[]{"MUTE","OPTIONS"});
        section(main,"NUMBERS",
            new String[]{"NUM_1","NUM_2","NUM_3"},
            new String[]{"NUM_4","NUM_5","NUM_6"},
            new String[]{"NUM_7","NUM_8","NUM_9"},
            new String[]{"","NUM_0",""});
        TextView foot=label("Uses captured/tested IR codes. Unofficial DStv remote.",12,Color.LTGRAY);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);
        fp.setMargins(0,dp(25),0,0);
        main.addView(foot,fp);
        setContentView(scroll);
    }

    private void send(String key) {
        if(!ready() || !patterns.containsKey(key)) return;
        try {
            ir.transmit(carriers.get(key),patterns.get(key));
            status.setText("Sent " + key.replace('_',' '));
            status.setTextColor(accent);
        } catch(RuntimeException e) {
            status.setText("IR send failed");
            status.setTextColor(Color.RED);
            Toast.makeText(this,"IR transmission failed",Toast.LENGTH_SHORT).show();
        }
    }
}
