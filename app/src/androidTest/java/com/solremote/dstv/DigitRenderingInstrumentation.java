package com.solremote.dstv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/** Native rendering checks; this runner and its APK are never shipped in the app. */
public class DigitRenderingInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            int[] checks = {0};
            Throwable[] error = {null};
            runOnMainSync(() -> {
                try {
                    for (float scale : new float[]{1f, 1.5f}) {
                        Configuration config = new Configuration(getTargetContext().getResources().getConfiguration());
                        config.fontScale = scale;
                        Context context = getTargetContext().createConfigurationContext(config);
                        for (boolean dark : new boolean[]{false, true}) {
                            for (boolean enabled : new boolean[]{false, true}) {
                                for (int number = 0; number < 10; number++) {
                                    checkKey(context, number, dark, enabled);
                                    checks[0]++;
                                }
                            }
                        }
                    }
                } catch (Throwable failure) {
                    error[0] = failure;
                }
            });
            if (error[0] != null) throw new RuntimeException(error[0]);
            result.putString("status", "passed");
            result.putInt("render_checks", checks[0]);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("status", "failed");
            result.putString("failure", android.util.Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void checkKey(Context context, int number, boolean dark, boolean enabled) {
        int ink = Color.parseColor(dark ? "#FFFFFF" : "#17120D");
        int surface = Color.parseColor(dark ? "#2C382F" : "#FFFFFF");
        DigitKeyView key = new DigitKeyView(context, number, ink);
        key.setBackgroundColor(surface);
        key.setEnabled(enabled);
        key.setOnClickListener(v -> {});
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(88 * density);
        int height = key.getMinimumHeight();
        key.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        key.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        key.draw(new Canvas(bitmap));
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int glyphPixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                if (Math.abs(Color.red(pixel) - Color.red(ink)) < 20 &&
                        Math.abs(Color.green(pixel) - Color.green(ink)) < 20 &&
                        Math.abs(Color.blue(pixel) - Color.blue(ink)) < 20) glyphPixels++;
            }
        }
        if (glyphPixels < 40) throw new AssertionError("Missing digit " + number + ", dark=" + dark + ", enabled=" + enabled);
        // Glyphs must also fit vertically when the user increases font size.
        for (int x = 0; x < width; x++) {
            if (bitmap.getPixel(x, 0) != surface || bitmap.getPixel(x, height - 1) != surface) {
                throw new AssertionError("Clipped digit " + number);
            }
        }
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        key.onInitializeAccessibilityNodeInfo(info);
        if (!Integer.toString(number).contentEquals(info.getText()) ||
                !"android.widget.Button".contentEquals(info.getClassName()) || info.isEnabled() != enabled) {
            throw new AssertionError("Incorrect accessibility state for " + number);
        }
        info.recycle();
        bitmap.recycle();
    }
}
