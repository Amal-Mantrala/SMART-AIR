package com.example.b07demosummer2024.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ChartsComponent extends View {
    private Paint linePaint;
    private Paint pointPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private List<Float> dataPoints;
    private List<String> labels;
    private float maxValue = 100f;
    private String chartTitle = "";
    private int chartColor = Color.parseColor("#2196F3");

    public ChartsComponent(Context context) {
        super(context);
        init();
    }

    public ChartsComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartsComponent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(chartColor);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        pointPaint = new Paint();
        pointPaint.setColor(chartColor);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        dataPoints = new ArrayList<>();
        labels = new ArrayList<>();
    }

    public void setData(List<Float> data, List<String> dataLabels, float max) {
        this.dataPoints = new ArrayList<>(data);
        this.labels = new ArrayList<>(dataLabels);
        this.maxValue = max > 0 ? max : 100f;
        invalidate();
    }

    public void setChartTitle(String title) {
        this.chartTitle = title;
        invalidate();
    }

    public void setChartColor(int color) {
        this.chartColor = color;
        linePaint.setColor(color);
        pointPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints == null || dataPoints.isEmpty()) {
            // Draw "No data" message
            canvas.drawText("No data available", getWidth() / 2f - 80f, getHeight() / 2f, textPaint);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int padding = 60;
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding - 40; // Extra space for title

        // Draw title
        if (!chartTitle.isEmpty()) {
            textPaint.setTextSize(32f);
            textPaint.setColor(Color.BLACK);
            canvas.drawText(chartTitle, padding, 40, textPaint);
            textPaint.setTextSize(20f);
        }

        // Draw grid lines
        for (int i = 0; i <= 4; i++) {
            float y = padding + 40 + (i * chartHeight / 4f);
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint);
        }

        if (dataPoints.size() <= 1) {
            // Single point or no data
            if (dataPoints.size() == 1) {
                float x = padding + chartWidth / 2f;
                float y = padding + 40 + chartHeight - ((dataPoints.get(0) / maxValue) * chartHeight);
                canvas.drawCircle(x, y, 6f, pointPaint);
            }
            return;
        }

        // Draw line chart
        Path path = new Path();
        float stepX = (float) chartWidth / (dataPoints.size() - 1);

        for (int i = 0; i < dataPoints.size(); i++) {
            float x = padding + (i * stepX);
            float y = padding + 40 + chartHeight - ((dataPoints.get(i) / maxValue) * chartHeight);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }

            // Draw points
            canvas.drawCircle(x, y, 4f, pointPaint);

            // Draw labels if available
            if (i < labels.size() && !labels.get(i).isEmpty()) {
                textPaint.setTextSize(18f);
                textPaint.setColor(Color.GRAY);
                canvas.drawText(labels.get(i), x - 15f, height - 10, textPaint);
            }
        }

        canvas.drawPath(path, linePaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.max(200, MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(width, height);
    }
}