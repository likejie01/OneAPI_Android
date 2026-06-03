package center.oneapi.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

public class FlowBackgroundView extends View {
    private static final float LIGHT_SATURATION_SCALE = 0.65f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long startedAt = System.currentTimeMillis();
    private final RectF bounds = new RectF();

    public FlowBackgroundView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = Math.max(1, getWidth());
        float h = Math.max(1, getHeight());
        if (!UiKit.backgroundEffectsEnabled(getContext())) {
            paint.setShader(null);
            paint.setColor(UiKit.appBackground(getContext()));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        }
        float cycle = ((System.currentTimeMillis() - startedAt) % 9000L) / 9000f;
        float slow = ((System.currentTimeMillis() - startedAt) % 16000L) / 16000f;
        float theta = (float) (cycle * Math.PI * 2f);
        float drift = (float) Math.sin(slow * Math.PI * 2f);
        if (UiKit.darkTheme(getContext())) {
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{Color.rgb(5, 8, 13), Color.rgb(9, 14, 23), Color.rgb(7, 11, 17)},
                    new float[]{0f, 0.48f + drift * 0.08f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            drawSoftBlob(canvas, w * (0.18f + 0.16f * (float) Math.sin(theta)), h * (0.22f + 0.07f * (float) Math.cos(theta * 0.8f)), w * 0.50f,
                    Color.argb(86, 32, 62, 144), Color.argb(0, 32, 62, 144));
            drawSoftBlob(canvas, w * (0.82f + 0.10f * (float) Math.cos(theta * 1.1f)), h * (0.20f + 0.13f * (float) Math.sin(theta)), w * 0.42f,
                    Color.argb(62, 26, 113, 103), Color.argb(0, 26, 113, 103));
            drawSoftBlob(canvas, w * (0.50f + 0.10f * (float) Math.cos(theta * 0.7f)), h * (0.80f + 0.05f * (float) Math.sin(theta * 1.3f)), w * 0.52f,
                    Color.argb(54, 94, 67, 139), Color.argb(0, 94, 67, 139));
            postInvalidateDelayed(33);
            return;
        }
        paint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{
                        scaleSaturation(Color.rgb(242, 249, 255), LIGHT_SATURATION_SCALE),
                        scaleSaturation(Color.rgb(229, 244, 255), LIGHT_SATURATION_SCALE),
                        scaleSaturation(Color.rgb(255, 248, 252), LIGHT_SATURATION_SCALE)
                },
                new float[]{0f, 0.44f + drift * 0.12f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        drawSoftBlob(canvas, w * (0.18f + 0.16f * (float) Math.sin(theta)), h * (0.22f + 0.07f * (float) Math.cos(theta * 0.8f)), w * 0.50f,
                scaleSaturation(Color.argb(106, 140, 190, 255), LIGHT_SATURATION_SCALE), Color.argb(0, 140, 190, 255));
        drawSoftBlob(canvas, w * (0.82f + 0.10f * (float) Math.cos(theta * 1.1f)), h * (0.20f + 0.13f * (float) Math.sin(theta)), w * 0.42f,
                scaleSaturation(Color.argb(90, 92, 225, 205), LIGHT_SATURATION_SCALE), Color.argb(0, 92, 225, 205));
        drawSoftBlob(canvas, w * (0.42f + 0.12f * (float) Math.cos(theta * 0.7f)), h * (0.84f + 0.05f * (float) Math.sin(theta * 1.3f)), w * 0.54f,
                scaleSaturation(Color.argb(78, 198, 150, 255), LIGHT_SATURATION_SCALE), Color.argb(0, 198, 150, 255));
        drawSoftBlob(canvas, w * (0.68f + 0.08f * (float) Math.sin(theta * 1.4f)), h * (0.62f + 0.08f * (float) Math.cos(theta * 0.9f)), w * 0.34f,
                scaleSaturation(Color.argb(61, 255, 198, 130), LIGHT_SATURATION_SCALE), Color.argb(0, 255, 198, 130));
        postInvalidateDelayed(33);
    }

    private void drawSoftBlob(Canvas canvas, float cx, float cy, float radius, int inner, int outer) {
        paint.setShader(new RadialGradient(cx, cy, Math.max(1f, radius), inner, outer, Shader.TileMode.CLAMP));
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawOval(bounds, paint);
        paint.setShader(null);
    }

    private int scaleSaturation(int color, float scale) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(0f, Math.min(1f, hsv[1] * scale));
        return Color.HSVToColor(Color.alpha(color), hsv);
    }
}
