package center.oneapi.mobile.ui.conversation;

import android.app.Dialog;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import center.oneapi.mobile.R;
import center.oneapi.mobile.ui.UiKit;
import center.oneapi.mobile.ui.markdown.MarkdownViews;

public class ConversationAdapter extends ListAdapter<ConversationDisplayItem, ConversationAdapter.Holder> {
    public interface Listener {
        void onEdit(ConversationDisplayItem item);

        void onDelete(ConversationDisplayItem item);
    }

    private final Context context;
    private final Listener listener;
    private LinearLayout visibleActions;

    public ConversationAdapter(Context context) {
        this(context, null);
    }

    public ConversationAdapter(Context context, Listener listener) {
        super(DIFF);
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout frame = new FrameLayout(context);
        frame.setClipToPadding(false);
        frame.setClipChildren(false);
        frame.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        return new Holder(frame);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ConversationDisplayItem item = getItem(position);
        holder.frame.removeAllViews();
        holder.frame.addView(viewFor(item), childLayoutParams(item));
    }

    private View viewFor(ConversationDisplayItem item) {
        if (item.type == ConversationDisplayItem.LOAD_EARLIER) {
            TextView view = UiKit.text(context, item.text, UiKit.BLUE, 13);
            view.setGravity(android.view.Gravity.CENTER);
            view.setBackground(UiKit.round(Color.argb(210, 255, 255, 255), UiKit.dp(context, 14), Color.argb(76, 54, 104, 240)));
            view.setPadding(UiKit.dp(context, 10), UiKit.dp(context, 9), UiKit.dp(context, 10), UiKit.dp(context, 9));
            return view;
        }
        if (item.type == ConversationDisplayItem.EMPTY) {
            LinearLayout card = UiKit.vertical(context);
            card.setPadding(UiKit.dp(context, 16), UiKit.dp(context, 15), UiKit.dp(context, 16), UiKit.dp(context, 15));
            card.setBackground(UiKit.round(UiKit.GLASS, UiKit.dp(context, 18), UiKit.LINE));
            card.addView(UiKit.text(context, item.text, UiKit.MUTED, 14));
            return card;
        }
        LinearLayout bubble = UiKit.vertical(context);
        boolean user = "user".equals(item.role);
        bubble.setPadding(UiKit.dp(context, 14), UiKit.dp(context, 12), UiKit.dp(context, 14), UiKit.dp(context, 12));
        if (item.type == ConversationDisplayItem.LOG) {
            bubble.addView(logView(item.text));
        } else if (user) {
            renderUserContent(bubble, item.text);
        } else {
            if (isImageSource(item.text)) {
                bubble.addView(image(item.text, true));
            } else if (isAttachmentSource(item.text)) {
                bubble.addView(attachment(item.text));
            } else {
                new MarkdownViews(context).renderInto(bubble, item.text, null);
            }
        }
        bubble.setOnLongClickListener(v -> {
            showShareMenu(item.text, v);
            return true;
        });
        LinearLayout actions = actions(item, user);
        actions.setVisibility(View.GONE);
        if (item.type == ConversationDisplayItem.MESSAGE && item.timestamp > 0) {
            LinearLayout meta = UiKit.horizontal(context);
            meta.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView time = UiKit.text(context, formatTime(item.timestamp) + " · " + tokenEstimate(item.text), UiKit.MUTED, 12);
            time.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            meta.addView(time, new LinearLayout.LayoutParams(0, UiKit.dp(context, 34), 1f));
            meta.addView(actions, new LinearLayout.LayoutParams(-2, UiKit.dp(context, 34)));
            bubble.addView(meta, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 36)));
        }
        attachToggle(bubble, actions);
        FrameLayout shell = new FrameLayout(context);
        shell.setBackground(UiKit.glass(context, UiKit.dp(context, 16), UiKit.LINE));
        shell.setClipToOutline(true);
        shell.addView(bubble, new FrameLayout.LayoutParams(-1, -2));
        attachToggle(shell, actions);
        View stripe = new View(context);
        stripe.setBackgroundColor(user ? Color.rgb(181, 226, 203) : Color.rgb(177, 205, 250));
        FrameLayout.LayoutParams stripeLp = new FrameLayout.LayoutParams(UiKit.dp(context, 1), -1, user ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT);
        shell.addView(stripe, stripeLp);
        LinearLayout wrap = UiKit.horizontal(context);
        wrap.setGravity(android.view.Gravity.TOP);
        if (user) {
            wrap.addView(shell, new LinearLayout.LayoutParams(0, -2, 1f));
        } else {
            wrap.addView(shell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return wrap;
    }

    private LinearLayout actions(ConversationDisplayItem item, boolean user) {
        LinearLayout row = UiKit.horizontal(context);
        row.setGravity(user ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT);
        boolean image = isImageSource(item.text);
        if (user) {
            if (listener != null) row.addView(actionButton(R.drawable.ic_bubble_edit, "编辑", () -> listener.onEdit(item)));
            row.addView(actionButton(R.drawable.ic_bubble_copy, "复制", () -> copy(item.text)));
            row.addView(actionButton(R.drawable.ic_bubble_share, "分享", () -> shareText(item.text)));
            if (listener != null) row.addView(actionButton(R.drawable.ic_bubble_delete, "删除", () -> listener.onDelete(item)));
        } else if (image) {
            row.addView(actionButton(R.drawable.ic_bubble_download, "下载", () -> saveImage(item.text)));
            row.addView(actionButton(R.drawable.ic_bubble_share, "分享", () -> shareText(item.text)));
            if (listener != null) row.addView(actionButton(R.drawable.ic_bubble_edit, "编辑", () -> listener.onEdit(item)));
            if (listener != null) row.addView(actionButton(R.drawable.ic_bubble_delete, "删除", () -> listener.onDelete(item)));
        } else {
            row.addView(actionButton(R.drawable.ic_bubble_copy, "复制", () -> copy(item.text)));
            row.addView(actionButton(R.drawable.ic_bubble_share, "分享", () -> shareText(item.text)));
            if (listener != null) row.addView(actionButton(R.drawable.ic_bubble_delete, "删除", () -> listener.onDelete(item)));
        }
        return row;
    }

    private void attachToggle(View view, LinearLayout actions) {
        if ("log".equals(view.getTag())) return;
        view.setOnClickListener(v -> toggleActions(actions));
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageButton || child instanceof ImageView) continue;
                attachToggle(child, actions);
            }
        }
    }

    private void toggleActions(LinearLayout actions) {
        if (actions == null) return;
        if (visibleActions == actions && actions.getVisibility() == View.VISIBLE) {
            actions.setVisibility(View.GONE);
            visibleActions = null;
            return;
        }
        if (visibleActions != null && visibleActions != actions) {
            visibleActions.setVisibility(View.GONE);
        }
        actions.setVisibility(View.VISIBLE);
        visibleActions = actions;
    }

    private View logView(String value) {
        LinearLayout row = UiKit.vertical(context);
        row.setTag("log");
        LinearLayout head = UiKit.horizontal(context);
        head.setGravity(android.view.Gravity.TOP);
        TextView dot = UiKit.text(context, "●", logColor(value), 14);
        dot.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(UiKit.dp(context, 18), UiKit.dp(context, 30));
        dotLp.setMargins(0, UiKit.dp(context, 1), 0, 0);
        head.addView(dot, dotLp);
        String title = firstLogLine(value);
        TextView titleView = UiKit.bold(context, title.isEmpty() ? "执行日志" : title);
        titleView.setTextSize(13);
        head.addView(titleView, new LinearLayout.LayoutParams(-2, -2));
        TextView marker = UiKit.text(context, "▶", UiKit.MUTED, 7);
        marker.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams markerLp = new LinearLayout.LayoutParams(UiKit.dp(context, 14), UiKit.dp(context, 30));
        markerLp.setMargins(UiKit.dp(context, 8), 0, 0, 0);
        head.addView(marker, markerLp);
        View spacer = new View(context);
        head.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(head);
        LinearLayout markdown = UiKit.vertical(context);
        String detail = detailLogText(value);
        new MarkdownViews(context).renderInto(markdown, detail.isEmpty() ? value : detail, null);
        markdown.setVisibility(View.GONE);
        row.addView(markdown, new LinearLayout.LayoutParams(-1, -2));
        row.setOnClickListener(v -> {
            boolean open = markdown.getVisibility() == View.VISIBLE;
            markdown.setVisibility(open ? View.GONE : View.VISIBLE);
            marker.setText(open ? "▶" : "▼");
        });
        return row;
    }

    private String firstLogLine(String value) {
        String clean = value == null ? "" : value.trim();
        int split = clean.indexOf('\n');
        return split >= 0 ? clean.substring(0, split).trim() : clean;
    }

    private String detailLogText(String value) {
        String clean = value == null ? "" : value.trim();
        int split = clean.indexOf('\n');
        return split >= 0 ? clean.substring(split + 1).trim() : "";
    }

    private int logColor(String value) {
        String text = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        if (text.contains("error") || text.contains("失败")) return Color.rgb(216, 71, 86);
        if (text.contains("file") || text.contains("文件")) return Color.rgb(214, 144, 44);
        if (text.contains("command") || text.contains("命令") || text.contains("exec")) return Color.rgb(117, 88, 214);
        if (text.contains("purpose") || text.contains("intent") || text.contains("目的")) return Color.rgb(48, 151, 106);
        if (text.contains("tool") || text.contains("工具")) return Color.rgb(20, 143, 166);
        return Color.rgb(54, 104, 240);
    }

    private ImageButton actionButton(int icon, String desc, Runnable action) {
        ImageButton button = UiKit.imageButton(context, icon, desc);
        button.setColorFilter(UiKit.MUTED);
        button.setPadding(UiKit.dp(context, 7), UiKit.dp(context, 7), UiKit.dp(context, 7), UiKit.dp(context, 7));
        button.setOnClickListener(v -> action.run());
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setLayoutParams(new LinearLayout.LayoutParams(UiKit.dp(context, 32), UiKit.dp(context, 32)));
        return button;
    }

    private String tokenEstimate(String text) {
        int chars = text == null ? 0 : text.trim().length();
        int tokens = Math.max(1, (int) Math.ceil(chars / 3.5d));
        return "约 " + tokens + " tokens";
    }

    private View image(String source, boolean generated) {
        int outer = UiKit.dp(context, 56);
        int inner = UiKit.dp(context, 52);
        FrameLayout wrap = new FrameLayout(context);
        wrap.setClipToOutline(true);
        wrap.setBackground(UiKit.round(Color.TRANSPARENT, UiKit.dp(context, 12), Color.TRANSPARENT));
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(UiKit.round(Color.WHITE, UiKit.dp(context, 14), UiKit.LINE));
        image.setContentDescription("图片消息");
        image.setOnClickListener(v -> openImage(source));
        image.setOnLongClickListener(v -> {
            shareImageSource(source);
            return true;
        });
        FrameLayout.LayoutParams imageLp = new FrameLayout.LayoutParams(inner, inner, android.view.Gravity.LEFT | android.view.Gravity.TOP);
        imageLp.setMargins(0, 0, UiKit.dp(context, 4), UiKit.dp(context, 4));
        wrap.addView(image, imageLp);
        if (generated) {
            ImageButton download = actionButton(R.drawable.ic_bubble_download, "下载", () -> saveImage(source));
            download.setBackground(UiKit.round(Color.argb(185, 255, 255, 255), UiKit.dp(context, 9), UiKit.LINE));
            download.setPadding(UiKit.dp(context, 4), UiKit.dp(context, 4), UiKit.dp(context, 4), UiKit.dp(context, 4));
            FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(UiKit.dp(context, 22), UiKit.dp(context, 22), android.view.Gravity.RIGHT | android.view.Gravity.TOP);
            wrap.addView(download, dl);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(outer, outer);
        lp.setMargins(0, 0, 0, UiKit.dp(context, 6));
        wrap.setLayoutParams(lp);
        loadImage(source, image);
        return wrap;
    }

    private void renderUserContent(LinearLayout bubble, String source) {
        StringBuilder text = new StringBuilder();
        for (String line : (source == null ? "" : source).split("\\n")) {
            String clean = line.trim();
            if (isImageSource(clean)) {
                bubble.addView(image(clean, false));
            } else if (isAttachmentSource(clean)) {
                bubble.addView(attachment(clean));
            } else if (!line.isEmpty()) {
                if (text.length() > 0) text.append('\n');
                text.append(line);
            }
        }
        if (text.length() > 0) {
            TextView view = UiKit.text(context, text.toString(), UiKit.INK, 14);
            view.setTextIsSelectable(true);
            bubble.addView(view);
        }
    }

    private void loadImage(String source, ImageView target) {
        String value = source == null ? "" : source.trim();
        target.setTag(value);
        if (value.startsWith("data:image/")) {
            int comma = value.indexOf(',');
            if (comma > 0) {
                try {
                    byte[] bytes = Base64.decode(value.substring(comma + 1), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    target.setImageBitmap(bitmap);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
        if (value.startsWith("content:") || value.startsWith("file:")) {
            target.setImageURI(Uri.parse(value));
            return;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            new Thread(() -> {
                try (InputStream input = new URL(value).openStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (value.equals(target.getTag())) {
                            target.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception ignored) {
                }
            }).start();
        }
    }

    private boolean isImageSource(String value) {
        if (value == null) return false;
        String text = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (text.startsWith("content:")) {
            try {
                String type = context.getContentResolver().getType(Uri.parse(value.trim()));
                return type != null && type.startsWith("image/");
            } catch (Exception ignored) {
                return false;
            }
        }
        if (text.startsWith("file:")) {
            return text.endsWith(".png") || text.endsWith(".jpg") || text.endsWith(".jpeg") || text.endsWith(".webp");
        }
        return text.startsWith("data:image/")
                || text.endsWith(".png")
                || text.endsWith(".jpg")
                || text.endsWith(".jpeg")
                || text.endsWith(".webp")
                || text.contains("/image/");
    }

    private boolean isAttachmentSource(String value) {
        if (value == null) return false;
        String text = value.trim().toLowerCase(java.util.Locale.ROOT);
        return text.startsWith("content:") || text.startsWith("file:");
    }

    private View attachment(String source) {
        if (isImageSource(source)) return image(source, false);
        TextView file = UiKit.text(context, extensionLabel(source), UiKit.MUTED, 11);
        file.setGravity(android.view.Gravity.CENTER);
        file.setSingleLine(true);
        file.setTextColor(fileColor(source));
        file.setBackground(UiKit.round(fileBackground(source), UiKit.dp(context, 12), UiKit.LINE));
        file.setOnClickListener(v -> openAttachment(source));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(context, 56), UiKit.dp(context, 56));
        lp.setMargins(0, 0, 0, UiKit.dp(context, 6));
        file.setLayoutParams(lp);
        return file;
    }

    private String extensionLabel(String source) {
        String name = displayName(source);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1) : "FILE";
        ext = ext.replaceAll("[^A-Za-z0-9]", "").trim();
        if (ext.isEmpty()) ext = "FILE";
        return ext.length() > 5 ? ext.substring(0, 5).toUpperCase(java.util.Locale.ROOT) : ext.toUpperCase(java.util.Locale.ROOT);
    }

    private String displayName(String source) {
        try {
            Uri uri = Uri.parse(source.trim());
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String name = cursor.getString(0);
                        if (name != null && !name.trim().isEmpty()) return name.trim();
                    }
                }
            }
            String path = uri.getLastPathSegment();
            if (path != null && !path.trim().isEmpty()) return path.trim();
        } catch (Exception ignored) {
        }
        return "attachment";
    }

    private int fileColor(String source) {
        String ext = extensionLabel(source).toLowerCase(java.util.Locale.ROOT);
        if (ext.contains("pdf")) return Color.rgb(195, 76, 72);
        if (ext.contains("xls")) return Color.rgb(42, 142, 91);
        if (ext.contains("doc")) return Color.rgb(54, 104, 190);
        if (ext.contains("md")) return Color.rgb(92, 104, 124);
        if (ext.contains("json")) return Color.rgb(156, 112, 44);
        return UiKit.MUTED;
    }

    private int fileBackground(String source) {
        String ext = extensionLabel(source).toLowerCase(java.util.Locale.ROOT);
        if (ext.contains("pdf")) return Color.rgb(255, 239, 238);
        if (ext.contains("xls")) return Color.rgb(235, 249, 241);
        if (ext.contains("doc")) return Color.rgb(238, 244, 255);
        if (ext.contains("md")) return Color.rgb(244, 246, 249);
        if (ext.contains("json")) return Color.rgb(255, 246, 231);
        return Color.rgb(248, 250, 252);
    }

    private void openAttachment(String source) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(source.trim());
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "打开附件"));
        } catch (Exception ignored) {
        }
    }

    private void openImage(String source) {
        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            FrameLayout root = new FrameLayout(context);
            root.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout panel = UiKit.vertical(context);
            panel.setPadding(UiKit.dp(context, 10), UiKit.dp(context, 8), UiKit.dp(context, 10), UiKit.dp(context, 10));
            panel.setBackground(UiKit.glass(context, UiKit.dp(context, 18), UiKit.LINE));
            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            loadImage(source, image);
            LinearLayout tools = UiKit.horizontal(context);
            tools.setGravity(android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            tools.setPadding(0, 0, 0, UiKit.dp(context, 6));
            tools.addView(actionButton(R.drawable.ic_bubble_download, "下载", () -> saveImage(source)));
            tools.addView(actionButton(R.drawable.ic_bubble_share, "分享", () -> shareText(source)));
            tools.addView(actionButton(R.drawable.ic_close_x, "关闭", dialog::dismiss));
            panel.addView(tools, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 40)));
            panel.addView(image, new LinearLayout.LayoutParams(-1, -1));
            final long[] lastTap = new long[]{0};
            final float[] down = new float[2];
            image.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    long now = System.currentTimeMillis();
                    if (now - lastTap[0] < 260) {
                        boolean zoomed = image.getScaleX() > 1.01f;
                        image.animate().scaleX(zoomed ? 1f : 2.4f).scaleY(zoomed ? 1f : 2.4f).translationX(0).translationY(0).setDuration(140).start();
                    }
                    lastTap[0] = now;
                    down[0] = event.getRawX() - image.getTranslationX();
                    down[1] = event.getRawY() - image.getTranslationY();
                    return true;
                }
                if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && image.getScaleX() > 1.01f) {
                    image.setTranslationX(event.getRawX() - down[0]);
                    image.setTranslationY(event.getRawY() - down[1]);
                    return true;
                }
                return true;
            });
            root.setOnClickListener(v -> dialog.dismiss());
            panel.setOnClickListener(v -> { });
            image.setOnClickListener(v -> { });
            int screenW = context.getResources().getDisplayMetrics().widthPixels;
            int screenH = context.getResources().getDisplayMetrics().heightPixels;
            int maxW = Math.max(UiKit.dp(context, 260), screenW - UiKit.dp(context, 32));
            int maxH = Math.max(UiKit.dp(context, 260), (int) (screenH * 0.72f));
            int[] size = imageSize(source);
            float ratio = size[0] > 0 && size[1] > 0 ? size[0] / (float) size[1] : 1f;
            int imageW = maxW;
            int imageH = Math.max(UiKit.dp(context, 180), (int) (imageW / Math.max(0.3f, ratio)));
            if (imageH > maxH - UiKit.dp(context, 58)) {
                imageH = maxH - UiKit.dp(context, 58);
                imageW = Math.min(maxW, Math.max(UiKit.dp(context, 220), (int) (imageH * ratio)));
            }
            FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(imageW + UiKit.dp(context, 20), imageH + UiKit.dp(context, 58), android.view.Gravity.CENTER);
            root.addView(panel, panelLp);
            dialog.setContentView(root);
            Window window = dialog.getWindow();
            if (window != null) window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.show();
            Window shown = dialog.getWindow();
            if (shown != null) {
                shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                applyPreviewBlur(shown);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyPreviewBlur(Window window) {
        if (window == null || Build.VERSION.SDK_INT < 31) return;
        if (UiKit.effectsEnabled(context) && UiKit.blurStrength(context) > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.setBlurBehindRadius(UiKit.blurStrength(context));
            window.setAttributes(attrs);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }
    }

    private int[] imageSize(String source) {
        String value = source == null ? "" : source.trim();
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            if (value.startsWith("data:image/")) {
                int comma = value.indexOf(',');
                if (comma > 0) {
                    byte[] bytes = Base64.decode(value.substring(comma + 1), Base64.DEFAULT);
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                }
            } else if (value.startsWith("content:") || value.startsWith("file:")) {
                try (InputStream input = context.getContentResolver().openInputStream(Uri.parse(value))) {
                    BitmapFactory.decodeStream(input, null, options);
                }
            }
            return new int[]{options.outWidth, options.outHeight};
        } catch (Exception ignored) {
            return new int[]{0, 0};
        }
    }

    private void shareImageSource(String source) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, source == null ? "" : source);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "分享图片"));
        } catch (Exception ignored) {
        }
    }

    private void showShareMenu(String text, View anchor) {
        try {
            boolean image = isImageSource(text);
            String[] items = image ? new String[]{"保存图片", "复制", "分享"} : new String[]{"复制", "分享"};
            LinearLayout panel = UiKit.horizontal(context);
            panel.setPadding(UiKit.dp(context, 8), UiKit.dp(context, 6), UiKit.dp(context, 8), UiKit.dp(context, 6));
            panel.setBackground(UiKit.glass(context, UiKit.dp(context, 16), UiKit.LINE));
            PopupWindow popup = new PopupWindow(panel, -2, UiKit.dp(context, 44), true);
            popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            popup.setOutsideTouchable(true);
            for (String item : items) {
                TextView button = UiKit.text(context, item, UiKit.INK, 13);
                button.setGravity(android.view.Gravity.CENTER);
                button.setPadding(UiKit.dp(context, 9), 0, UiKit.dp(context, 9), 0);
                button.setOnClickListener(v -> {
                    popup.dismiss();
                    if ("保存图片".equals(item)) saveImage(text);
                    else if ("复制".equals(item)) copy(text);
                    else shareText(text);
                });
                panel.addView(button, new LinearLayout.LayoutParams(-2, -1));
            }
            panel.measure(View.MeasureSpec.makeMeasureSpec(UiKit.dp(context, 180), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
            int popupHeight = Math.max(panel.getMeasuredHeight(), UiKit.dp(context, 44));
            int[] pos = new int[2];
            anchor.getLocationOnScreen(pos);
            int gap = UiKit.dp(context, 8);
            int y = pos[1] - popupHeight - gap;
            if (y < UiKit.dp(context, 18)) y = pos[1] + anchor.getHeight() + gap;
            popup.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, pos[0], y);
        } catch (Exception ignored) {
        }
    }

    private void copy(String text) {
        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) manager.setPrimaryClip(ClipData.newPlainText("OneAPI", text == null ? "" : text));
        } catch (Exception ignored) {
        }
    }

    private void shareText(String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "分享"));
        } catch (Exception ignored) {
        }
    }

    private String formatTime(long timestamp) {
        long ms = timestamp > 10000000000L ? timestamp : timestamp * 1000L;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(ms));
    }

    private void saveImage(String source) {
        new Thread(() -> {
            try {
                byte[] bytes = imageBytes(source);
                if (bytes == null || bytes.length == 0) return;
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "oneapi-" + System.currentTimeMillis() + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OneAPI");
                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return;
                try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                    if (output != null) output.write(bytes);
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private byte[] imageBytes(String source) throws Exception {
        String value = source == null ? "" : source.trim();
        if (value.startsWith("data:image/")) {
            int comma = value.indexOf(',');
            return comma > 0 ? Base64.decode(value.substring(comma + 1), Base64.DEFAULT) : null;
        }
        InputStream input;
        if (value.startsWith("content:") || value.startsWith("file:")) {
            input = context.getContentResolver().openInputStream(Uri.parse(value));
        } else if (value.startsWith("http://") || value.startsWith("https://")) {
            input = new URL(value).openStream();
        } else {
            return null;
        }
        if (input == null) return null;
        try (InputStream closeable = input; java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = closeable.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    private FrameLayout.LayoutParams childLayoutParams(ConversationDisplayItem item) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
        int left = 0;
        lp.setMargins(left, UiKit.dp(context, 8), 0, UiKit.dp(context, 8));
        return lp;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final FrameLayout frame;

        Holder(@NonNull FrameLayout frame) {
            super(frame);
            this.frame = frame;
        }
    }

    private static final DiffUtil.ItemCallback<ConversationDisplayItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ConversationDisplayItem oldItem, @NonNull ConversationDisplayItem newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ConversationDisplayItem oldItem, @NonNull ConversationDisplayItem newItem) {
            return oldItem.type == newItem.type
                    && oldItem.timestamp == newItem.timestamp
                    && oldItem.role.equals(newItem.role)
                    && oldItem.text.equals(newItem.text);
        }
    };
}
