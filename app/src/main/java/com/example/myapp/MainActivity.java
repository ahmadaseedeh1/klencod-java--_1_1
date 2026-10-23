package com.example.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private CompassView compassView;
    private TextView degreeText;
    private TextView directionText;
    private TextView statusText;

    private float currentAzimuth = 0f;
    private boolean sensorAvailable = false;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private static final int BG = Color.rgb(11, 17, 29);
    private static final int WHITE = Color.WHITE;
    private static final int RED = Color.rgb(255, 55, 75);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        sensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);

        rotationSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));

        TextView title = new TextView(this);
        title.setText("COMPASS");
        title.setTextColor(WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        root.addView(title, new LinearLayout.LayoutParams(
                -1, dp(45)));

        TextView subtitle = new TextView(this);
        subtitle.setText("البوصلة الرقمية");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(19);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(subtitle, new LinearLayout.LayoutParams(
                -1, dp(35)));

        degreeText = new TextView(this);
        degreeText.setText("0°");
        degreeText.setTextColor(WHITE);
        degreeText.setTextSize(48);
        degreeText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        degreeText.setGravity(Gravity.CENTER);

        root.addView(degreeText, new LinearLayout.LayoutParams(
                -1, dp(68)));

        directionText = new TextView(this);
        directionText.setText("شمال");
        directionText.setTextColor(RED);
        directionText.setTextSize(24);
        directionText.setGravity(Gravity.CENTER);

        root.addView(directionText, new LinearLayout.LayoutParams(
                -1, dp(42)));

        compassView = new CompassView(this);

        LinearLayout.LayoutParams compassParams =
                new LinearLayout.LayoutParams(
                        -1, 0, 1f);

        compassParams.topMargin = dp(8);
        compassParams.bottomMargin = dp(8);

        root.addView(compassView, compassParams);

        statusText = new TextView(this);
        statusText.setTextColor(Color.LTGRAY);
        statusText.setTextSize(13);
        statusText.setGravity(Gravity.CENTER);
        statusText.setText("جارٍ تجهيز البوصلة...");

        root.addView(statusText, new LinearLayout.LayoutParams(
                -1, dp(38)));

        setContentView(root);

        if (rotationSensor == null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER);
            Sensor magnetometer = sensorManager.getDefaultSensor(
                    Sensor.TYPE_MAGNETIC_FIELD);

            if (accelerometer != null && magnetometer != null) {
                statusText.setText(
                        "حساس البوصلة غير متاح؛ يلزم بديل للحساسات.");
            } else {
                statusText.setText(
                        "حساسات الاتجاه غير متوفرة على هذا الجهاز.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null && rotationSensor != null) {
            sensorAvailable = sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI);

            statusText.setText(sensorAvailable
                    ? "البوصلة تعمل"
                    : "تعذر تشغيل حساس الاتجاه");
        } else if (statusText != null) {
            statusText.setText(
                    "حساس اتجاه الهاتف غير متوفر.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            SensorManager.getRotationMatrixFromVector(
                    rotationMatrix, event.values);

            SensorManager.getOrientation(
                    rotationMatrix, orientationAngles);

            float azimuth = (float) Math.toDegrees(
                    orientationAngles[0]);

            azimuth = (azimuth + 360f) % 360f;

            // Smooth the transition across the 0/360 boundary.
            float difference = ((azimuth - currentAzimuth + 540f)
                    % 360f) - 180f;

            currentAzimuth = (currentAzimuth + difference * 0.22f
                    + 360f) % 360f;

            int degrees = Math.round(currentAzimuth) % 360;

            degreeText.setText(String.format(
                    Locale.US, "%03d°", degrees));

            directionText.setText(getDirection(degrees));

            compassView.setAzimuth(currentAzimuth);
        }
    }

    private String getDirection(float degrees) {
        String[] directions = {
                "شمال", "شمال شرقي", "شرق",
                "جنوب شرقي", "جنوب", "جنوب غربي",
                "غرب", "شمال غربي"
        };

        int index = Math.round(degrees / 45f) % 8;
        return directions[index];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (statusText == null) return;

        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            statusText.setText(
                    "دقة الحساس منخفضة؛ حرّك الهاتف على شكل 8 للمعايرة.");
        } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            statusText.setText("دقة الحساس جيدة");
        } else {
            statusText.setText(
                    "إذا كان الاتجاه غير دقيق، عاير البوصلة بتحريك الهاتف.");
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private class CompassView extends View {

        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG);

        private float azimuth = 0f;

        CompassView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setAzimuth(float value) {
            azimuth = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            float radius = Math.min(
                    getWidth(), getHeight()) * 0.43f;

            if (radius <= 0) return;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(29, 39, 61));

            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(Color.rgb(92, 108, 139));

            canvas.drawCircle(cx, cy, radius, paint);

            canvas.save();
            canvas.rotate(-azimuth, cx, cy);

            for (int i = 0; i < 360; i += 5) {
                canvas.save();
                canvas.rotate(i, cx, cy);

                boolean major = i % 90 == 0;
                boolean medium = i % 30 == 0;

                float start = radius - (major
                        ? dp(16) : medium ? dp(11) : dp(5));

                paint.setColor(major ? WHITE :
                        Color.rgb(165, 177, 200));

                paint.setStrokeWidth(major
                        ? dp(2) : dp(1));

                canvas.drawLine(cx, cy - start,
                        cx, cy - radius + dp(2), paint);

                if (major) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    paint.setTextSize(dp(14));

                    String label;
                    switch (i) {
                        case 0: label = "ش"; break;
                        case 90: label = "ق"; break;
                        case 180: label = "ج"; break;
                        default: label = "غ"; break;
                    }

                    paint.setColor(i == 0 ? RED : WHITE);

                    canvas.drawText(label, cx,
                            cy - radius + dp(25), paint);

                    paint.setStyle(Paint.Style.STROKE);
                }

                canvas.restore();
            }

            // Four-point compass needle.
            Path north = new Path();
            north.moveTo(cx, cy - radius * 0.83f);
            north.lineTo(cx - dp(7), cy);
            north.lineTo(cx, cy - dp(5));
            north.lineTo(cx + dp(7), cy);
            north.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(WHITE);
            canvas.drawPath(north, paint);

            Path south = new Path();
            south.moveTo(cx, cy + radius * 0.83f);
            south.lineTo(cx - dp(7), cy);
            south.lineTo(cx, cy + dp(5));
            south.lineTo(cx + dp(7), cy);
            south.close();

            paint.setColor(RED);
            canvas.drawPath(south, paint);

            canvas.restore();

            paint.setColor(Color.rgb(11, 17, 29));
            canvas.drawCircle(cx, cy, dp(6), paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(WHITE);
            canvas.drawCircle(cx, cy, dp(6), paint);

            paint.setStyle(Paint.Style.FILL);
        }
    }
}