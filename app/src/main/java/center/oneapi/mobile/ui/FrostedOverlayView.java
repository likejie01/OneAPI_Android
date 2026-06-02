package center.oneapi.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class FrostedOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FrostedOverlayView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.argb(54, 255, 255, 255));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.argb(14, 120, 140, 180));
        for (int y = 0; y < getHeight(); y += 8) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
        paint.setColor(Color.argb(10, 255, 255, 255));
        for (int x = 0; x < getWidth(); x += 10) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
    }
}
