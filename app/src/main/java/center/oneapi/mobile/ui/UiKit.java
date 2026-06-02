package center.oneapi.mobile.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UiKit {
    public static final int INK = Color.rgb(23, 31, 48);
    public static final int MUTED = Color.rgb(91, 103, 123);
    public static final int LINE = Color.argb(88, 145, 160, 184);
    public static final int BLUE = Color.rgb(54, 104, 240);
    public static final int GLASS = Color.argb(205, 255, 255, 255);

    private UiKit() {
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable round(int color, int radiusPx, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(1, strokeColor);
        }
        return drawable;
    }

    public static boolean effectsEnabled(Context context) {
        return prefs(context).getBoolean("effects_enabled", true);
    }

    public static int glassAlpha(Context context) {
        if (!effectsEnabled(context)) return 255;
        return Math.max(0, Math.min(255, prefs(context).getInt("glass_alpha", 238)));
    }

    public static GradientDrawable glass(Context context, int radiusPx, int strokeColor) {
        return round(Color.argb(glassAlpha(context), 255, 255, 255), radiusPx, strokeColor);
    }

    public static int blurStrength(Context context) {
        return effectsEnabled(context) ? Math.max(0, Math.min(50, prefs(context).getInt("blur_strength", 30))) : 0;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences("oneapi_mobile", Context.MODE_PRIVATE);
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static TextView text(Context context, String value, int color, int sp) {
        TextView view = new TextView(context);
        view.setText(value == null ? "" : value);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setLineSpacing(dp(context, 3), 1.08f);
        view.setPadding(0, dp(context, 4), 0, dp(context, 4));
        return view;
    }

    public static TextView bold(Context context, String value) {
        TextView view = text(context, value, INK, 17);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    public static Button ghostButton(Context context, String value) {
        Button button = new Button(context);
        button.setText(value);
        button.setTextColor(INK);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        button.setBackground(round(Color.argb(150, 255, 255, 255), dp(context, 16), LINE));
        return button;
    }

    public static ImageButton imageButton(Context context, int resId, String desc) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(resId);
        button.setContentDescription(desc);
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
        button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        button.setColorFilter(INK);
        return button;
    }

    public static View gap(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, heightDp)));
        return view;
    }
}
