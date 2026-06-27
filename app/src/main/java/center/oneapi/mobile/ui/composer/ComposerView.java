package center.oneapi.mobile.ui.composer;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import center.oneapi.mobile.R;
import center.oneapi.mobile.ui.AttachmentThumbnailLoader;
import center.oneapi.mobile.ui.UiKit;

public class ComposerView extends LinearLayout {
    public interface Listener {
        void onSend(String text);

        void onStop();

        void onHistory();

        void onModel();

        void onAssistant();

        void onImageSize();

        void onImageQuality();

        void onReasoning();

        void onContext();

        void onAttach();

        void onSkill();

        void onPermissionToggle();

        void onRemoveSkill(String skill);

        void onPreviewAttachment(Uri uri);

        void onRemoveAttachment(Uri uri);

        void onVoiceMic();
    }

    private final FlowTagLayout tags;
    private final HorizontalScrollView attachmentScroller;
    private final LinearLayout attachmentRow;
    private final EditText input;
    private final ImageButton modelButton;
    private final ImageButton reasoningButton;
    private final ImageButton contextButton;
    private final ImageButton assistantButton;
    private final ImageButton sizeButton;
    private final ImageButton qualityButton;
    private final ImageButton permissionButton;
    private final ImageButton skillButton;
    private final ImageButton micButton;
    private final ImageButton sendButton;
    private final LinearLayout inputRow;
    private Listener listener;
    private ComposerState state;
    private boolean sending;
    private boolean voiceListening;
    private boolean keyboardOpen;
    private boolean inputCollapsed;
    private boolean enabled = true;
    private boolean hasAttachments;
    private View lastActionAnchor;

    public ComposerView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setClipToPadding(false);
        setClipChildren(false);
        int pad = UiKit.dp(context, 10);
        setPadding(pad, UiKit.dp(context, 8), pad, UiKit.dp(context, 12));
        setBackground(UiKit.glass(context, UiKit.dp(context, 18), UiKit.line(context)));

        tags = new FlowTagLayout(context);
        tags.setPadding(0, 0, 0, UiKit.dp(context, 4));
        addView(tags, new LinearLayout.LayoutParams(-1, -2));

        attachmentScroller = new HorizontalScrollView(context);
        attachmentScroller.setHorizontalScrollBarEnabled(false);
        attachmentRow = UiKit.horizontal(context);
        attachmentRow.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        attachmentScroller.addView(attachmentRow, new HorizontalScrollView.LayoutParams(-2, UiKit.dp(context, 58)));
        attachmentScroller.setVisibility(GONE);
        addView(attachmentScroller, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 58)));

        LinearLayout tools = UiKit.horizontal(context);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(UiKit.dp(context, 8), 0, 0, 0);
        addToolButton(tools, R.drawable.tool_add, "附件", v -> {
            if (listener != null) listener.onAttach();
        });
        addToolButton(tools, R.drawable.tool_history, "最近会话", v -> {
            if (listener != null) listener.onHistory();
        });
        permissionButton = addToolButton(tools, R.drawable.tool_noauthority, "权限", v -> {
            if (listener != null) listener.onPermissionToggle();
        });
        modelButton = addToolButton(tools, R.drawable.tool_module, "模型", v -> {
            if (listener != null) listener.onModel();
        });
        reasoningButton = addToolButton(tools, R.drawable.tool_think, "Thinking", v -> {
            if (listener != null) listener.onReasoning();
        });
        contextButton = addToolButton(tools, R.drawable.tool_text, "上下文", v -> {
            if (listener != null) listener.onContext();
        });
        sizeButton = addToolButton(tools, R.drawable.tool_size, "尺寸", v -> {
            if (listener != null) listener.onImageSize();
        });
        qualityButton = addToolButton(tools, R.drawable.tool_ratio, "质量", v -> {
            if (listener != null) listener.onImageQuality();
        });
        skillButton = addToolButton(tools, R.drawable.tool_skill, "Skill", v -> {
            if (listener != null) listener.onSkill();
        });
        assistantButton = addToolButton(tools, R.drawable.tool_helper, "助手", v -> {
            if (listener != null) listener.onAssistant();
        });

        input = new EditText(context);
        input.setMinLines(1);
        input.setMaxLines(5);
        input.setTextSize(15);
        input.setTextColor(UiKit.ink(context));
        input.setHintTextColor(UiKit.muted(context));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(UiKit.dp(context, 12), UiKit.dp(context, 8), UiKit.dp(context, 12), UiKit.dp(context, 8));
        inputRow = UiKit.horizontal(context);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setBackground(UiKit.round(Color.TRANSPARENT, UiKit.dp(context, 14), Color.TRANSPARENT));
        inputRow.addView(input, new LinearLayout.LayoutParams(0, UiKit.dp(context, 68), 1f));
        micButton = UiKit.imageButton(context, R.drawable.ic_voice_mic, "开始语音输入");
        micButton.setVisibility(VISIBLE);
        micButton.setOnClickListener(v -> {
            if (listener != null) listener.onVoiceMic();
        });
        inputRow.addView(micButton, new LinearLayout.LayoutParams(UiKit.dp(context, 42), UiKit.dp(context, 42)));
        addView(inputRow, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 68)));
        inputRow.setOnClickListener(v -> expandInput(true));

        sendButton = UiKit.imageButton(context, R.drawable.ic_send_plane, "发送");
        sendButton.setPadding(UiKit.dp(context, 8), UiKit.dp(context, 8), UiKit.dp(context, 8), UiKit.dp(context, 8));
        LinearLayout bottom = UiKit.horizontal(context);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setOnClickListener(v -> expandInput(true));
        bottom.addView(tools, new LinearLayout.LayoutParams(0, UiKit.dp(context, 38), 1f));
        FrameLayout sendWrap = new FrameLayout(context);
        sendWrap.setPadding(UiKit.dp(context, 8), 0, 0, 0);
        sendWrap.addView(sendButton, new FrameLayout.LayoutParams(UiKit.dp(context, 38), UiKit.dp(context, 38), Gravity.CENTER));
        bottom.addView(sendWrap, new LinearLayout.LayoutParams(UiKit.dp(context, 50), UiKit.dp(context, 38)));
        addView(bottom, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 40)));

        sendButton.setOnClickListener(v -> emitSend());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                emitSend();
                return true;
            }
            return false;
        });
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) maybeCollapseInput();
            else expandInput(false);
            updateInputBoundary();
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void updateState(ComposerState next) {
        state = next;
        enabled = next.enabled;
        input.setHint(next.hint.isEmpty() ? "输入消息" : next.hint);
        boolean chat = "chat".equals(next.sectionId);
        boolean image = "image".equals(next.sectionId);
        boolean desktop = next.desktopMode;
        modelButton.setVisibility(chat ? VISIBLE : GONE);
        reasoningButton.setVisibility(chat ? VISIBLE : GONE);
        contextButton.setVisibility(chat ? VISIBLE : GONE);
        assistantButton.setVisibility((chat || image) ? VISIBLE : GONE);
        sizeButton.setVisibility(image ? VISIBLE : GONE);
        qualityButton.setVisibility(image ? VISIBLE : GONE);
        permissionButton.setVisibility(next.desktopMode ? VISIBLE : GONE);
        skillButton.setVisibility(next.desktopMode ? VISIBLE : GONE);
        permissionButton.setImageResource(next.fullPermission ? R.drawable.tool_authority : R.drawable.tool_noauthority);
        permissionButton.setContentDescription(next.fullPermission ? "全权限" : "受限");
        setInteractionEnabled(enabled);
        renderTags(next);
        applyTheme();
    }

    public String inputText() {
        return input.getText().toString();
    }

    public void clearInput() {
        input.setText("");
    }

    public void setInputText(String text) {
        expandInput(false);
        input.setText(text == null ? "" : text);
        input.setSelection(input.getText().length());
        input.requestFocus();
    }

    public void setSending(boolean sending) {
        this.sending = sending;
        sendButton.setImageResource(sending ? R.drawable.ic_stop_square : R.drawable.ic_send_plane);
        sendButton.setContentDescription(sending ? "停止" : "发送");
        applyTheme();
        sendButton.setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    }

    public void setVoiceMode(boolean enabled) {
        micButton.setVisibility(VISIBLE);
        updateInputBoundary();
    }

    public void setVoiceListening(boolean listening) {
        voiceListening = listening;
        if (listening) {
            micButton.setColorFilter(Color.rgb(48, 151, 106));
        } else if (UiKit.darkTheme(getContext())) {
            micButton.setColorFilter(UiKit.ink(getContext()));
        } else {
            micButton.clearColorFilter();
        }
        micButton.setBackground(listening
                ? UiKit.round(Color.argb(92, 181, 226, 203), UiKit.dp(getContext(), 21), Color.rgb(119, 196, 158))
                : new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        micButton.setScaleX(listening ? 1.08f : 1f);
        micButton.setScaleY(listening ? 1.08f : 1f);
        micButton.setAlpha(1f);
        updateInputBoundary();
    }

    public void setKeyboardOpen(boolean open) {
        if (keyboardOpen == open) return;
        keyboardOpen = open;
        int pad = UiKit.dp(getContext(), 10);
        setPadding(pad, UiKit.dp(getContext(), 8), pad, UiKit.dp(getContext(), open ? 4 : 12));
        updateInputBoundary();
        requestLayout();
    }

    public void setVoiceLevel(float rms) {
        float clamped = Math.max(0f, Math.min(10f, rms));
        float scale = 1f + clamped / 22f;
        int alpha = (int) (72 + clamped * 13);
        micButton.setBackground(UiKit.round(Color.argb(Math.min(210, alpha), 181, 226, 203), UiKit.dp(getContext(), 21), Color.rgb(119, 196, 158)));
        micButton.animate().scaleX(scale).scaleY(scale).alpha(0.82f + clamped / 56f).setDuration(80).start();
    }

    public View lastActionAnchor() {
        return lastActionAnchor;
    }

    public void updateAttachments(List<Uri> attachments) {
        attachmentRow.removeAllViews();
        List<Uri> safe = attachments == null ? new ArrayList<>() : attachments;
        hasAttachments = !safe.isEmpty();
        attachmentScroller.setVisibility(safe.isEmpty() ? GONE : VISIBLE);
        for (Uri uri : safe) {
            FrameLayout thumb = new FrameLayout(getContext());
            View preview = attachmentPreview(uri);
            thumb.addView(preview, new FrameLayout.LayoutParams(UiKit.dp(getContext(), 52), UiKit.dp(getContext(), 52)));
            TextView badge = UiKit.text(getContext(), "×", Color.WHITE, 12);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(UiKit.round(Color.argb(180, 23, 31, 48), UiKit.dp(getContext(), 10), Color.TRANSPARENT));
            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(UiKit.dp(getContext(), 20), UiKit.dp(getContext(), 20), Gravity.RIGHT | Gravity.TOP);
            thumb.addView(badge, badgeLp);
            thumb.setOnClickListener(v -> {
                if (listener != null) listener.onPreviewAttachment(uri);
            });
            badge.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveAttachment(uri);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(getContext(), 56), UiKit.dp(getContext(), 56));
            lp.setMargins(0, 0, UiKit.dp(getContext(), 8), 0);
            attachmentRow.addView(thumb, lp);
        }
    }

    private View attachmentPreview(Uri uri) {
        String type = getContext().getContentResolver().getType(uri);
        if (type != null && type.startsWith("image/")) {
            ImageView image = new ImageView(getContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(UiKit.round(UiKit.resolve(getContext(), Color.WHITE), UiKit.dp(getContext(), 12), UiKit.line(getContext())));
            int side = UiKit.dp(getContext(), 52);
            AttachmentThumbnailLoader.load(getContext(), uri.toString(), image, side, side);
            return image;
        }
        TextView file = UiKit.text(getContext(), extensionLabel(uri), UiKit.MUTED, 11);
        file.setGravity(Gravity.CENTER);
        file.setSingleLine(true);
        file.setTextColor(fileColor(uri));
        file.setBackground(UiKit.round(fileBackground(uri), UiKit.dp(getContext(), 12), UiKit.line(getContext())));
        return file;
    }

    private void emitSend() {
        expandInput(true);
        if (sending) {
            if (listener != null) listener.onStop();
            return;
        }
        if (!enabled) {
            return;
        }
        String text = input.getText().toString().trim();
        if (text.isEmpty() && !hasAttachments) {
            return;
        }
        if (listener != null) {
            listener.onSend(text);
        }
    }

    private ImageButton addToolButton(LinearLayout row, int icon, String desc, OnClickListener click) {
        ImageButton button = UiKit.imageButton(getContext(), icon, desc);
        button.setPadding(UiKit.dp(getContext(), 8), UiKit.dp(getContext(), 8), UiKit.dp(getContext(), 8), UiKit.dp(getContext(), 8));
        button.setAlpha(0.82f);
        button.setOnClickListener(v -> {
            if (!enabled) return;
            expandInput(false);
            lastActionAnchor = v;
            click.onClick(v);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(getContext(), 34), UiKit.dp(getContext(), 34));
        lp.setMargins(0, 0, UiKit.dp(getContext(), 4), 0);
        row.addView(button, lp);
        return button;
    }

    private void renderTags(ComposerState next) {
        tags.removeAllViews();
        if (!next.projectLabel.isEmpty()) {
            tags.addView(tag(next.projectLabel, true));
        }
        if (!next.assistantLabel.isEmpty()) {
            tags.addView(tag(shortText(next.assistantLabel, 6), false, "assistant"));
        }
        if (!next.modelLabel.isEmpty()) {
            tags.addView(tag(shortText(next.modelLabel, 8), false, "model"));
        }
        if (!next.sizeLabel.isEmpty()) {
            tags.addView(tag(next.sizeLabel, false, "image"));
        }
        if (!next.qualityLabel.isEmpty()) {
            tags.addView(tag(next.qualityLabel, false, "image"));
        }
        for (String skill : next.selectedSkills) {
            tags.addView(removableTag(skill));
        }
        tags.requestLayout();
    }

    private TextView tag(String text, boolean strong) {
        return tag(text, strong, strong ? "project" : "default");
    }

    private TextView tag(String text, boolean strong, String kind) {
        TextView view = UiKit.text(getContext(), text, strong ? UiKit.INK : UiKit.MUTED, 12);
        view.setSingleLine(false);
        view.setMaxLines(2);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackground(UiKit.round(UiKit.tagFill(getContext(), kind, strong), UiKit.dp(getContext(), 13), UiKit.line(getContext())));
        view.setPadding(UiKit.dp(getContext(), 9), UiKit.dp(getContext(), 4), UiKit.dp(getContext(), 9), UiKit.dp(getContext(), 4));
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, UiKit.dp(getContext(), 6), UiKit.dp(getContext(), 5));
        view.setLayoutParams(lp);
        return view;
    }

    private View removableTag(String text) {
        LinearLayout row = UiKit.horizontal(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        String kind = extensionKind(text);
        int fg = "command".equals(kind) ? Color.rgb(158, 98, 28)
                : "plugin".equals(kind) ? Color.rgb(104, 82, 170)
                : UiKit.blue(getContext());
        if (UiKit.darkTheme(getContext())) {
            fg = "command".equals(kind) ? Color.rgb(232, 177, 105)
                    : "plugin".equals(kind) ? Color.rgb(196, 178, 255)
                    : UiKit.blue(getContext());
        }
        row.setBackground(UiKit.round(UiKit.tagFill(getContext(), kind, true), UiKit.dp(getContext(), 13), UiKit.line(getContext())));
        row.setPadding(UiKit.dp(getContext(), 9), UiKit.dp(getContext(), 2), UiKit.dp(getContext(), 4), UiKit.dp(getContext(), 2));
        TextView label = UiKit.text(getContext(), text, fg, 12);
        label.setSingleLine(true);
        row.addView(label, new LinearLayout.LayoutParams(-2, UiKit.dp(getContext(), 24)));
        TextView close = UiKit.text(getContext(), "×", fg, 13);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveSkill(text);
        });
        row.addView(close, new LinearLayout.LayoutParams(UiKit.dp(getContext(), 22), UiKit.dp(getContext(), 24)));
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, UiKit.dp(getContext(), 6), UiKit.dp(getContext(), 5));
        row.setLayoutParams(lp);
        return row;
    }

    private String shortText(String value, int max) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max) + "...";
    }

    private String extensionKind(String value) {
        String clean = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        if (clean.startsWith("/") || clean.startsWith("command:") || clean.startsWith("cmd:") || clean.contains(" · command")) {
            return "command";
        }
        if (clean.startsWith("plugin:") || clean.contains(" · plugin")) {
            return "plugin";
        }
        return "skill";
    }

    private void updateInputBoundary() {
        boolean active = keyboardOpen || voiceListening || input.hasFocus();
        inputRow.setBackground(UiKit.round(active ? UiKit.inputFill(getContext()) : Color.TRANSPARENT, UiKit.dp(getContext(), 14), Color.TRANSPARENT));
    }

    private void applyTheme() {
        setBackground(UiKit.glass(getContext(), UiKit.dp(getContext(), 18), UiKit.line(getContext())));
        input.setTextColor(UiKit.ink(getContext()));
        input.setHintTextColor(UiKit.muted(getContext()));
        refreshIconColors(this);
        if (voiceListening) {
            micButton.setColorFilter(Color.rgb(48, 151, 106));
        } else if (UiKit.darkTheme(getContext())) {
            micButton.setColorFilter(UiKit.ink(getContext()));
        } else {
            micButton.clearColorFilter();
        }
        if (sending) {
            sendButton.setColorFilter(Color.rgb(216, 71, 86));
        } else if (!UiKit.darkTheme(getContext())) {
            sendButton.clearColorFilter();
        } else {
            sendButton.setColorFilter(UiKit.ink(getContext()));
        }
        updateInputBoundary();
    }

    private void refreshIconColors(View view) {
        if (view instanceof ImageButton) {
            if (UiKit.darkTheme(getContext())) {
                ((ImageButton) view).setColorFilter(UiKit.ink(getContext()));
            } else {
                ((ImageButton) view).clearColorFilter();
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                refreshIconColors(group.getChildAt(i));
            }
        }
    }

    private void maybeCollapseInput() {
        updateInputBoundary();
    }

    private void expandInput(boolean requestFocus) {
        if (inputCollapsed) {
            inputCollapsed = false;
            inputRow.setVisibility(VISIBLE);
        }
        if (requestFocus) input.requestFocus();
        updateInputBoundary();
    }

    private void setInteractionEnabled(boolean value) {
        input.setEnabled(value);
        input.setAlpha(value ? 1f : 0.58f);
        sendButton.setEnabled(value);
        micButton.setEnabled(value);
        sendButton.setAlpha(value ? 1f : 0.36f);
        micButton.setAlpha(value ? 1f : 0.36f);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof LinearLayout) {
                setButtonsEnabled((ViewGroup) child, value);
            }
        }
    }

    private void setButtonsEnabled(ViewGroup group, boolean value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageButton) {
                child.setEnabled(value);
                child.setAlpha(value ? 0.82f : 0.32f);
            } else if (child instanceof ViewGroup) {
                setButtonsEnabled((ViewGroup) child, value);
            }
        }
    }

    private String extensionLabel(Uri uri) {
        String path = uri == null ? "" : String.valueOf(uri.getLastPathSegment());
        int dot = path.lastIndexOf('.');
        String ext = dot >= 0 && dot + 1 < path.length() ? path.substring(dot + 1) : "FILE";
        if (ext.length() > 5) ext = ext.substring(0, 5);
        return ext.toUpperCase(java.util.Locale.ROOT);
    }

    private int fileBackground(Uri uri) {
        String ext = extensionLabel(uri).toLowerCase(java.util.Locale.ROOT);
        if (UiKit.darkTheme(getContext())) {
            if ("pdf".equals(ext)) return UiKit.tagFill(getContext(), "command", true);
            if ("xlsx".equals(ext) || "xls".equals(ext) || "xlsm".equals(ext)) return UiKit.tagFill(getContext(), "assistant", true);
            if ("md".equals(ext) || "markd".equals(ext)) return UiKit.tagFill(getContext(), "model", true);
            if ("json".equals(ext) || "txt".equals(ext)) return UiKit.itemFill(getContext(), false);
            return UiKit.itemFill(getContext(), false);
        }
        if ("pdf".equals(ext)) return Color.rgb(255, 241, 242);
        if ("xlsx".equals(ext) || "xls".equals(ext) || "xlsm".equals(ext)) return UiKit.tagFill(getContext(), "assistant", true);
        if ("md".equals(ext) || "markd".equals(ext)) return UiKit.tagFill(getContext(), "model", true);
        if ("json".equals(ext) || "txt".equals(ext)) return UiKit.itemFill(getContext(), false);
        return UiKit.itemFill(getContext(), false);
    }

    private int fileColor(Uri uri) {
        String ext = extensionLabel(uri).toLowerCase(java.util.Locale.ROOT);
        if ("pdf".equals(ext)) return Color.rgb(185, 54, 72);
        if ("xlsx".equals(ext) || "xls".equals(ext) || "xlsm".equals(ext)) return Color.rgb(48, 151, 106);
        if ("md".equals(ext) || "markd".equals(ext)) return UiKit.blue(getContext());
        return UiKit.muted(getContext());
    }
}
