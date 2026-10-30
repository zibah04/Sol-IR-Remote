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
                String display = command == null ? "" : command
                    .replace("VOL_PLUS","VOL +").replace("VOL_MINUS","VOL −")
                    .replace("UP","▲").replace("DOWN","▼")
                    .replace("LEFT","◀").replace("RIGHT","▶");
                if (command != null && command.startsWith("NUM_")) display = command.substring(4);
                button(r,display,command);
            }
        }
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
        section(main,"NAVIGATION",
            new String[]{"","UP",""},
            new String[]{"LEFT","OK","RIGHT"},
            new String[]{"INFO","DOWN","EXIT"});
        section(main,"VOLUME",new String[]{"VOL_MINUS","MUTE","VOL_PLUS"});
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
