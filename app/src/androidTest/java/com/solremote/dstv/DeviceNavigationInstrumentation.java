package com.solremote.dstv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Exercises actual device-card navigation and checks that the profiles stay isolated. */
public final class DeviceNavigationInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity home = null;
        Activity remote = null;
        try {
            home = startActivitySync(new Intent(getTargetContext(), RoomHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            Activity dstv = open(home, "Open DStv HD 8S remote", MainActivity.class);
            remote = dstv;
            Map<String, int[]> before = profile(dstv);
            require(before.size() == 20 && !before.containsKey("POWER"), "DStv profile missing or mixed");
            onUi(dstv::finish);
            waitForIdleSync();
            Activity lg = open(home, "Open LG TV remote", LgTvActivity.class);
            remote = lg;
            Map<String, int[]> television = profile(lg);
            require(television.size() == 29 && television.containsKey("POWER") && television.containsKey("INPUT"), "LG profile incomplete");
            require(television.get("POWER")[0] == 9000, "LG power frame incorrect");
            require(!Arrays.equals(television.get("VOL_PLUS"), before.get("VOL_PLUS")), "Profiles mixed");
            onUi(() -> {
                Set<String> bindings = new HashSet<>();
                collectBindings(lg.getWindow().getDecorView(), bindings);
                require(bindings.equals(television.keySet()), "LG controls do not match the uploaded commands");
                View power = find(lg.getWindow().getDecorView(), "TV power");
                require(power != null, "LG power control missing");
                View switcher = find(lg.getWindow().getDecorView(), "Choose a different remote");
                require(switcher != null, "Switch remote button missing");
                switcher.performClick();
            });
            waitForIdleSync();
            Activity again = open(home, "Open DStv HD 8S remote", MainActivity.class);
            remote = again;
            Map<String, int[]> after = profile(again);
            require(before.keySet().equals(after.keySet()), "DStv commands changed after switching");
            for (String name : before.keySet()) require(Arrays.equals(before.get(name), after.get(name)), "DStv waveform changed: " + name);
            result.putString("status", "passed");
            result.putInt("remote_open_checks", 3);
            result.putInt("dstv_signals", before.size());
            result.putInt("lg_signals", television.size());
            result.putString("dstv_after_switch", "unchanged");
        } catch (Throwable failure) {
            result.putString("status", "failed");
            result.putString("failure", android.util.Log.getStackTraceString(failure));
        } finally {
            if (remote != null) { Activity activity = remote; onUi(activity::finish); }
            if (home != null) { Activity activity = home; onUi(activity::finish); }
        }
        finish("passed".equals(result.getString("status")) ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }

    private Activity open(Activity home, String description, Class<? extends Activity> target) {
        ActivityMonitor monitor = addMonitor(target.getName(), null, false);
        onUi(() -> {
            View card = find(home.getWindow().getDecorView(), description);
            require(card != null, "Missing device card: " + description);
            card.performClick();
        });
        Activity activity = waitForMonitorWithTimeout(monitor, 30000);
        removeMonitor(monitor);
        require(activity != null, "Remote did not open: " + target.getSimpleName());
        waitForIdleSync();
        return activity;
    }

    private Map<String, int[]> profile(Activity activity) throws Exception {
        Field field = activity.getClass().getDeclaredField("patterns");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, int[]> source = (Map<String, int[]>) field.get(activity);
        Map<String, int[]> copy = new HashMap<>();
        for (Map.Entry<String, int[]> entry : source.entrySet()) copy.put(entry.getKey(), entry.getValue().clone());
        return copy;
    }

    private View find(View view, String description) {
        if (description.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void collectBindings(View view, Set<String> bindings) {
        if (view.getTag() instanceof String) bindings.add((String) view.getTag());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collectBindings(group.getChildAt(i), bindings);
        }
    }

    private void onUi(Runnable action) {
        Throwable[] error = {null};
        runOnMainSync(() -> { try { action.run(); } catch (Throwable failure) { error[0] = failure; } });
        if (error[0] != null) throw new AssertionError(error[0]);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
