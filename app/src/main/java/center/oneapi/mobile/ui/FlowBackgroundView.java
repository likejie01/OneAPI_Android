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
        float cycle = ((System.currentTimeMillis() - startedAt) % 9000L) / 9000f;
        float slow = ((System.currentTimeMillis() - startedAt) % 16000L) / 16000f;
        float theta = (float) (cycle * Math.PI * 2f);
        float drift = (float) Math.sin(slow * Math.PI * 2f);
        paint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.rgb(247, 251, 255), Color.rgb(240, 247, 255), Color.rgb(250, 252, 255)},
                new float[]{0f, 0.45f + drift * 0.08f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        drawSoftBlob(canvas, w * (0.18f + 0.16f * (float) Math.sin(theta)), h * (0.22f + 0.07f * (float) Math.cos(theta * 0.8f)), w * 0.50f,
                Color.argb(92, 196, 216, 252), Color.argb(0, 196, 216, 252));
        drawSoftBlob(canvas, w * (0.82f + 0.10f * (float) Math.cos(theta * 1.1f)), h * (0.20f + 0.13f * (float) Math.sin(theta)), w * 0.42f,
                Color.argb(76, 206, 241, 232), Color.argb(0, 206, 241, 232));
        drawSoftBlob(canvas, w * (0.42f + 0.12f * (float) Math.cos(theta * 0.7f)), h * (0.84f + 0.05f * (float) Math.sin(theta * 1.3f)), w * 0.54f,
                Color.argb(66, 226, 216, 248), Color.argb(0, 226, 216, 248));
        drawSoftBlob(canvas, w * (0.68f + 0.08f * (float) Math.sin(theta * 1.4f)), h * (0.62f + 0.08f * (float) Math.cos(theta * 0.9f)), w * 0.34f,
                Color.argb(44, 255, 232, 196), Color.argb(0, 255, 232, 196));
        postInvalidateDelayed(33);
    }

    private void drawSoftBlob(Canvas canvas, float cx, float cy, float radius, int inner, int outer) {
        paint.setShader(new RadialGradient(cx, cy, Math.max(1f, radius), inner, outer, Shader.TileMode.CLAMP));
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawOval(bounds, paint);
        paint.setShader(null);
    }
}
