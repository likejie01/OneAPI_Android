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
    public static final int DARK_BG = Color.rgb(13, 18, 28);
    public static final int DARK_SURFACE = Color.rgb(26, 34, 49);
    public static final int DARK_INK = Color.rgb(232, 238, 248);
    public static final int DARK_MUTED = Color.rgb(154, 166, 187);
    public static final int DARK_LINE = Color.argb(118, 108, 126, 160);
    public static final int DARK_BLUE = Color.rgb(125, 165, 255);

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

    public static boolean backgroundEffectsEnabled(Context context) {
        return prefs(context).getBoolean("background_effects_enabled", true);
    }

    public static boolean darkTheme(Context context) {
        return prefs(context).getBoolean("dark_theme", false);
    }

    public static int ink(Context context) {
        return darkTheme(context) ? DARK_INK : INK;
    }

    public static int muted(Context context) {
        return darkTheme(context) ? DARK_MUTED : MUTED;
    }

    public static int line(Context context) {
        return darkTheme(context) ? DARK_LINE : LINE;
    }

    public static int blue(Context context) {
        return darkTheme(context) ? DARK_BLUE : BLUE;
    }

    public static int inputFill(Context context) {
        return darkTheme(context) ? Color.rgb(28, 35, 48) : Color.rgb(250, 251, 253);
    }

    public static int surface(Context context) {
        return darkTheme(context) ? Color.argb(238, 24, 32, 46) : Color.argb(238, 255, 255, 255);
    }

    public static int overlayTint(Context context) {
        return darkTheme(context) ? Color.argb(42, 6, 10, 17) : Color.argb(38, 255, 255, 255);
    }

    public static int itemFill(Context context, boolean selected) {
        if (darkTheme(context)) {
            return selected ? Color.argb(220, 39, 52, 76) : Color.argb(190, 31, 40, 56);
        }
        return selected ? Color.argb(210, 238, 244, 255) : Color.argb(165, 255, 255, 255);
    }

    public static int chipFill(Context context, boolean selected) {
        if (darkTheme(context)) {
            return selected ? Color.argb(220, 44, 58, 84) : Color.argb(180, 32, 41, 58);
        }
        return selected ? Color.argb(205, 238, 244, 255) : Color.argb(150, 255, 255, 255);
    }

    public static int tagFill(Context context, String kind, boolean strong) {
        if (darkTheme(context)) {
            if ("assistant".equals(kind)) return Color.argb(210, 31, 58, 54);
            if ("command".equals(kind)) return Color.argb(210, 69, 50, 31);
            if ("plugin".equals(kind)) return Color.argb(210, 49, 42, 70);
            if (strong || "project".equals(kind) || "model".equals(kind)) return Color.argb(210, 38, 51, 75);
            return Color.argb(195, 31, 40, 56);
        }
        if ("assistant".equals(kind)) return Color.rgb(238, 248, 244);
        if ("command".equals(kind)) return Color.rgb(249, 243, 235);
        if ("plugin".equals(kind)) return Color.rgb(242, 239, 250);
        if (strong || "project".equals(kind) || "model".equals(kind)) return Color.rgb(238, 244, 255);
        return Color.rgb(247, 249, 252);
    }

    public static int codeFill(Context context) {
        return darkTheme(context) ? Color.rgb(19, 25, 36) : Color.rgb(244, 247, 251);
    }

    public static int codeText(Context context) {
        return darkTheme(context) ? Color.rgb(218, 226, 239) : Color.rgb(31, 41, 55);
    }

    public static int thinkingFill(Context context) {
        return darkTheme(context) ? Color.argb(92, 36, 48, 68) : Color.argb(58, 190, 205, 230);
    }

    public static int thinkingStroke(Context context) {
        return darkTheme(context) ? Color.argb(120, 78, 98, 135) : Color.argb(92, 120, 145, 190);
    }

    public static int thinkingText(Context context) {
        return darkTheme(context) ? Color.rgb(176, 190, 215) : Color.rgb(70, 88, 120);
    }

    public static int systemBar(Context context) {
        return darkTheme(context) ? Color.rgb(9, 13, 22) : Color.rgb(247, 251, 255);
    }

    public static int appBackground(Context context) {
        return darkTheme(context) ? Color.rgb(5, 8, 13) : Color.rgb(247, 251, 255);
    }

    public static int resolve(Context context, int color) {
        if (!darkTheme(context)) return color;
        if (color == INK) return DARK_INK;
        if (color == MUTED) return DARK_MUTED;
        if (color == LINE) return DARK_LINE;
        if (color == BLUE) return DARK_BLUE;
        if (color == GLASS || Color.red(color) > 245 && Color.green(color) > 245 && Color.blue(color) > 245) {
            return Color.argb(Math.max(130, Color.alpha(color)), 27, 36, 52);
        }
        return color;
    }

    public static int glassAlpha(Context context) {
        if (!effectsEnabled(context)) return 255;
        return Math.max(0, Math.min(255, prefs(context).getInt("glass_alpha", 238)));
    }

    public static GradientDrawable glass(Context context, int radiusPx, int strokeColor) {
        int alpha = glassAlpha(context);
        int fill = darkTheme(context) ? Color.argb(alpha, 24, 32, 46) : Color.argb(alpha, 255, 255, 255);
        return round(fill, radiusPx, resolve(context, strokeColor));
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
        view.setTextColor(resolve(context, color));
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
        button.setTextColor(ink(context));
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        int fill = darkTheme(context) ? Color.argb(150, 28, 37, 54) : Color.argb(150, 255, 255, 255);
        button.setBackground(round(fill, dp(context, 16), line(context)));
        return button;
    }

    public static Button primaryButton(Context context, String value) {
        Button button = new Button(context);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        button.setBackground(round(blue(context), dp(context, 16), Color.TRANSPARENT));
        return button;
    }

    public static ImageButton imageButton(Context context, int resId, String desc) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(resId);
        button.setContentDescription(desc);
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
        button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        button.setColorFilter(ink(context));
        return button;
    }

    public static View gap(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, heightDp)));
        return view;
    }
}
