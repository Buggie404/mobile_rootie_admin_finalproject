package com.veganbeauty.admin.features.home.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Float> dataPoints = new ArrayList<>();
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    public SparklineView(Context context) {
        this(context, null);
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(0xFF677559); // Default color secondary

        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Float> points) {
        if (points != null) {
            this.dataPoints = points;
        } else {
            this.dataPoints = new ArrayList<>();
        }
        invalidate();
    }

    public void setLineColor(int color) {
        linePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.size() < 2) return;

        float width = getWidth();
        float height = getHeight();

        float paddingLeft = getPaddingLeft();
        float paddingTop = getPaddingTop();
        float paddingRight = getPaddingRight();
        float paddingBottom = getPaddingBottom();

        float usableWidth = width - paddingLeft - paddingRight;
        float usableHeight = height - paddingTop - paddingBottom;

        float minVal = Float.MAX_VALUE;
        float maxVal = -Float.MAX_VALUE;
        for (float val : dataPoints) {
            if (val < minVal) minVal = val;
            if (val > maxVal) maxVal = val;
        }
        if (minVal == Float.MAX_VALUE) minVal = 0f;
        if (maxVal == -Float.MAX_VALUE) maxVal = 100f;

        float range = (maxVal == minVal) ? 1f : (maxVal - minVal);

        linePath.reset();
        fillPath.reset();

        float stepX = usableWidth / (dataPoints.size() - 1);

        for (int i = 0; i < dataPoints.size(); i++) {
            float x = paddingLeft + i * stepX;
            float normY = (dataPoints.get(i) - minVal) / range;
            float y = paddingTop + usableHeight - (normY * usableHeight);

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, paddingTop + usableHeight);
                fillPath.lineTo(x, y);
            } else {
                float prevX = paddingLeft + (i - 1) * stepX;
                float prevNormY = (dataPoints.get(i - 1) - minVal) / range;
                float prevY = paddingTop + usableHeight - (prevNormY * usableHeight);

                float cx1 = prevX + stepX / 2f;
                float cy1 = prevY;
                float cx2 = prevX + stepX / 2f;
                float cy2 = y;
                linePath.cubicTo(cx1, cy1, cx2, cy2, x, y);
                fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y);
            }

            if (i == dataPoints.size() - 1) {
                fillPath.lineTo(x, paddingTop + usableHeight);
                fillPath.close();
            }
        }

        // Setup gradient shader for the fill
        int baseColor = linePaint.getColor();
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int startColor = (0x33 << 24) | (r << 16) | (g << 8) | b;
        int endColor = (0x00 << 24) | (r << 16) | (g << 8) | b;

        fillPaint.setShader(new LinearGradient(
            0f, paddingTop, 0f, paddingTop + usableHeight,
            startColor, endColor, Shader.TileMode.CLAMP
        ));

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
