package com.solremote.dstv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

/** A numeric key whose label does not inherit a device's Button text styling. */
public final class DigitKeyView extends View {
    private final String label;
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path labelOutline = new Path();

    public DigitKeyView(Context context, int digit, int labelColor) {
        super(context);
        label = Integer.toString(digit);
        labelPaint.setColor(labelColor);
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        labelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28,
                getResources().getDisplayMetrics()));
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float density = getResources().getDisplayMetrics().density;
        setMinimumHeight(Math.max(Math.round(56 * density),
                (int) Math.ceil(metrics.bottom - metrics.top + 12 * density)));
        setMinimumWidth(Math.round(48 * density));
        setFocusable(true);
        setClickable(true);
        setContentDescription("Number " + label);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (android.os.Build.VERSION.SDK_INT >= 29) setForceDarkAllowed(false);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f;
        labelOutline.reset();
        // Cache the glyph as a shape, independent of native text recolouring.
        labelPaint.getTextPath(label, 0, label.length(),
                (width - labelPaint.measureText(label)) / 2f, baseline, labelOutline);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(labelOutline, labelPaint);
    }

    @Override public CharSequence getAccessibilityClassName() {
        return Button.class.getName();
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(getAccessibilityClassName());
        info.setText(label);
        info.setEnabled(isEnabled());
    }
}
