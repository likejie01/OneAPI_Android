package center.oneapi.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int REQ_PICK_IMAGE = 701;
    private static final int BLUE = Color.rgb(54, 104, 240);
    private static final int INDIGO = Color.rgb(91, 77, 215);
    private static final int CYAN = Color.rgb(54, 177, 220);
    private static final int MINT = Color.rgb(73, 190, 143);
    private static final int INK = Color.rgb(23, 31, 48);
    private static final int MUTED = Color.rgb(91, 103, 123);
    private static final int LINE = Color.argb(88, 145, 160, 184);
    private static final int GLASS = Color.argb(205, 255, 255, 255);
    private static final int GLASS_SOFT = Color.argb(158, 255, 255, 255);
    private static final int INITIAL_RENDER_ITEM_COUNT = 40;
    private static final int RENDER_PAGE_INCREMENT = 40;
    private static final int RENDER_CHUNK_SIZE = 6;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;

    private FrameLayout shell;
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout composerHost;
    private LinearLayout scrollDock;
    private View loadingOverlay;
    private LinearLayout topNav;
    private TextView navMenuSeparator;
    private LinearLayout timeline;
    private LinearLayout interactionBar;
    private ScrollView contentScroll;
    private TextView statusText;
    private TextView cliProjectTagView;
    private EditText activeInput;
    private Spinner modelSpinner;
    private Spinner reasoningSpinner;
    private Spinner permissionSpinner;
    private ImageButton activeSendButton;
    private LinearLayout attachmentPreviewHost;
    private LinearLayout extensionPreviewHost;
    private LinearLayout logDetailHost;
    private String activeComposerMode = "";
    private String selectedCliModel = "";
    private String selectedReasoning = "关闭思考";
    private String selectedPermission = "受限模式";
    private String selectedChatModel = "";
    private String selectedChatAssistant = "默认助手";
    private String selectedImageAssistant = "通用绘图";
    private String selectedImageSize = "1024x1024";
    private String selectedImageQuality = "标准";
    private String selectedContextWindow = "自动";
    private String selectedCliSession = "会话记录";
    private String selectedSkillPlugin = "默认";
    private String selectedRecentJobId = "";
    private String activeChatSessionId = "";
    private String activeImageSessionId = "";
    private String selectedCodexModel = "";
    private String selectedClaudeModel = "";
    private boolean imageRandomSeed = false;
    private boolean requestRunning = false;
    private final Set<String> cliRefreshRunningClients = Collections.synchronizedSet(new HashSet<>());
    private boolean desktopSessionsRefreshRunning = false;
    private boolean localAutoScrollEnabled = true;
    private boolean scrollDockDirectionDown = true;
    private int lastContentScrollY = 0;
    private long lastCliRefreshAt = 0L;
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private long lastExitSwipeAt = 0L;
    private int lastExitSwipeDirection = 0;
    private long suppressSwipeUntil = 0L;
    private final Map<String, String> cliTimelineSignatures = new HashMap<>();
    private final Map<String, JSONArray> cliTimelineCache = new HashMap<>();
    private final Map<String, Integer> localVisibleMessageCounts = new HashMap<>();
    private final Map<String, Integer> timelineVisibleItemCounts = new HashMap<>();
    private final Map<String, String> localConversationRawCache = new HashMap<>();
    private final Map<String, JSONObject> localConversationRootCache = new HashMap<>();
    private JSONArray cachedDesktopSessions = new JSONArray();
    private String pendingAndroidUpdateUrl = "";
    private String pendingAndroidUpdateVersion = "";
    private Uri downloadedAndroidUpdateUri = null;
    private boolean updateDownloading = false;
    private Runnable pendingComposerKeyboardRunnable = null;
    private final Set<String> autoScrolledSections = new HashSet<>();
    private final Set<String> expandedLogIds = new HashSet<>();
    private final List<Uri> selectedAttachmentUris = new ArrayList<>();
    private final List<JSONObject> selectedExtensionRefs = new ArrayList<>();
    private final Map<String, String> markdownHtmlCache = new LinkedHashMap<>();
    private View activeBubbleActions;
    private int renderGeneration = 0;

    private String section = "chat";
    private String boundDeviceId = "";
    private String sessionId = "android-" + UUID.randomUUID();
    private boolean polling = false;
    private final Map<String, String> selectedCliSessionIds = new HashMap<>();
    private final Map<String, String> selectedCliProjectNames = new HashMap<>();
    private final Map<String, String> selectedCliProjectPaths = new HashMap<>();
    private final List<String> codexModels = new ArrayList<>();
    private final List<String> claudeModels = new ArrayList<>();
    private final List<String> chatModels = new ArrayList<>();
    private final List<JSONObject> chatAssistants = new ArrayList<>();
    private final List<JSONObject> imageAssistants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        prefs = getSharedPreferences("oneapi_mobile", MODE_PRIVATE);
        if (prefs.getString("app_id", "").isEmpty()) {
            prefs.edit().putString("app_id", "android-" + UUID.randomUUID()).apply();
        }
        boundDeviceId = prefs.getString("bound_device_id", "");
        seedModels();
        loadSelections();
        loadDesktopSessionCache();
        if (prefs.getString("cookie", "").isEmpty()) {
            showLogin();
        } else {
            showMain();
        }
    }

    @Override
    protected void onDestroy() {
        polling = false;
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            persistSelectedImagePermissions(data);
            appendSelectedImages(data);
            renderAttachmentPreview();
            if (selectedAttachmentUris.isEmpty()) {
                toast("未选择图片");
            }
        }
    }

    private void seedModels() {
        codexModels.clear();
        claudeModels.clear();
        chatModels.clear();
        Collections.addAll(codexModels, "gpt-5.4", "gpt-5.3-codex", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5", "mimo-v2.5-pro");
        Collections.addAll(claudeModels, "claude-sonnet-4-6", "claude-opus-4-6", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5-pro");
        Collections.addAll(chatModels, "gpt-5.4", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5", "mimo-v2.5-pro", "claude-sonnet-4-6");
    }

    private void loadSelections() {
        selectedChatModel = prefs.getString("selected_chat_model", chatModels.isEmpty() ? "" : chatModels.get(0));
        selectedCodexModel = prefs.getString("selected_codex_model", codexModels.isEmpty() ? "" : codexModels.get(0));
        selectedClaudeModel = prefs.getString("selected_claude_model", claudeModels.isEmpty() ? "" : claudeModels.get(0));
        selectedChatAssistant = prefs.getString("selected_chat_assistant", selectedChatAssistant);
        selectedImageAssistant = prefs.getString("selected_image_assistant", selectedImageAssistant);
        selectedImageSize = prefs.getString("selected_image_size", selectedImageSize);
        selectedImageQuality = prefs.getString("selected_image_quality", selectedImageQuality);
        if (!"标准".equals(selectedImageQuality) && !"高清".equals(selectedImageQuality)) {
            selectedImageQuality = "高清".equals(selectedImageQuality) || "极致".equals(selectedImageQuality) ? "高清" : "标准";
            prefs.edit().putString("selected_image_quality", selectedImageQuality).apply();
        }
        selectedContextWindow = prefs.getString("selected_context_window", selectedContextWindow);
        selectedReasoning = prefs.getString("selected_reasoning", selectedReasoning);
        selectedPermission = prefs.getString("selected_permission", selectedPermission);
        if ("默认模型".equals(selectedChatModel)) {
            selectedChatModel = chatModels.isEmpty() ? "" : chatModels.get(0);
        }
        if ("默认模型".equals(selectedCodexModel)) {
            selectedCodexModel = codexModels.isEmpty() ? "" : codexModels.get(0);
        }
        if ("默认模型".equals(selectedClaudeModel)) {
            selectedClaudeModel = claudeModels.isEmpty() ? "" : claudeModels.get(0);
        }
    }

    private void showLogin() {
        polling = false;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        shell = new FrameLayout(this);
        shell.addView(new FlowBackgroundView(this), new FrameLayout.LayoutParams(-1, -1));
        shell.addView(new FrostedOverlayView(this), new FrameLayout.LayoutParams(-1, -1));
        prefs.edit().putString("server", "https://ai.oneapi.center").apply();
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(62), dp(24), dp(24));
        page.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout card = glassPanel();
        card.setPadding(dp(22), dp(30), dp(22), dp(28));
        card.addView(title("OneAPI"));
        card.addView(gap(12));
        card.addView(copy("登录后绑定 PC/Mac 客户端，手机端发送 Codex/Claude 指令，桌面端执行并同步日志。"));
        card.addView(gap(18));
        EditText username = input("账号");
        EditText password = input("密码");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button login = primary("登录");
        login.setOnClickListener(v -> {
            hideKeyboard(password);
            prefs.edit().putString("server", "https://ai.oneapi.center").apply();
            runNetwork(() -> {
                JSONObject body = new JSONObject();
                body.put("username", trim(username));
                body.put("password", trim(password));
                JSONObject envelope = api("POST", "/api/user/login", body);
                JSONObject data = envelope.optJSONObject("data");
                if (data != null) {
                    prefs.edit().putString("user_id", String.valueOf(data.optInt("id"))).apply();
                }
                ui(() -> {
                    showMain();
                });
            });
        });
        card.addView(username);
        card.addView(gap(8));
        card.addView(password);
        card.addView(gap(18));
        card.addView(login, new LinearLayout.LayoutParams(-1, dp(44)));
        page.addView(card);
        shell.addView(page, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);
    }

    private void showMain() {
        polling = true;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        shell = new FrameLayout(this);
        shell.addView(new FlowBackgroundView(this), new FrameLayout.LayoutParams(-1, -1));
        shell.addView(new FrostedOverlayView(this), new FrameLayout.LayoutParams(-1, -1));
        root = vertical();
        root.setPadding(0, dp(34), 0, 0);
        root.addView(header());
        ScrollView scroll = new ScrollView(this);
        contentScroll = scroll;
        scroll.setClipToPadding(false);
        scroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            scrollDockDirectionDown = scrollY >= oldScrollY;
            lastContentScrollY = scrollY;
            if ("chat".equals(section) || "image".equals(section) || "assistants".equals(section) || isCliSection()) {
                localAutoScrollEnabled = isContentNearBottom(scroll);
            }
            updateScrollDock();
        });
        content = vertical();
        content.setPadding(dp(18), dp(12), dp(18), dp(18));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        composerHost = vertical();
        composerHost.setPadding(0, 0, 0, dp(12));
        composerHost.setBackground(round(Color.argb(224, 255, 255, 255), dp(18), Color.TRANSPARENT));
        root.addView(composerHost);
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        scrollDock = vertical();
        scrollDock.setVisibility(View.GONE);
        scrollDock.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(dp(42), -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        dockLp.setMargins(0, 0, dp(10), 0);
        shell.addView(scrollDock, dockLp);
        setContentView(shell);
        refreshBottomNav();
        renderSection();
        refreshModels();
        refreshAssistants(false);
        refreshDevices(false);
        refreshDesktopSessionsCache(true);
        startPolling();
        installKeyboardLift();
        installSwipeNavigation();
    }

    private View header() {
        LinearLayout bar = horizontal();
        bar.setPadding(dp(18), dp(10), dp(18), dp(6));
        topNav = horizontal();
        bar.addView(topNav, new LinearLayout.LayoutParams(0, dp(42), 1));
        navMenuSeparator = copy("·");
        navMenuSeparator.setTextColor(Color.rgb(112, 124, 146));
        navMenuSeparator.setTextSize(18);
        navMenuSeparator.setGravity(Gravity.CENTER);
        bar.addView(navMenuSeparator, new LinearLayout.LayoutParams(dp(12), dp(42)));
        Button menu = iconButton("☰");
        menu.setContentDescription("打开菜单");
        menu.setOnClickListener(v -> openDrawer());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        menuLp.setMargins(dp(8), 0, 0, 0);
        bar.addView(menu, menuLp);
        statusText = copy(boundDeviceId.isEmpty() ? "未绑定设备" : "绑定设备 " + shortId(boundDeviceId));
        return bar;
    }

    private void refreshBottomNav() {
        if (topNav == null) {
            return;
        }
        topNav.removeAllViews();
        boolean mainSection = "chat".equals(section) || "image".equals(section) || "codex".equals(section) || "claude".equals(section) || "assistants".equals(section);
        topNav.setVisibility(mainSection ? View.VISIBLE : View.INVISIBLE);
        if (navMenuSeparator != null) {
            navMenuSeparator.setVisibility(mainSection ? View.VISIBLE : View.INVISIBLE);
        }
        if (!mainSection) {
            return;
        }
        String[] navItems = new String[]{"chat", "image", "codex", "claude"};
        for (int i = 0; i < navItems.length; i++) {
            String item = navItems[i];
            View b = navImageButton(navDrawable(item), item.equals(section) || ("assistants".equals(section) && "chat".equals(item)));
            b.setContentDescription(label(item));
            b.setOnClickListener(v -> {
                section = item;
                localAutoScrollEnabled = true;
                refreshBottomNav();
                renderSection();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            topNav.addView(b, lp);
            if (i < navItems.length - 1) {
                TextView separator = copy("·");
                separator.setGravity(Gravity.CENTER);
                separator.setTextColor(Color.rgb(112, 124, 146));
                separator.setTextSize(18);
                topNav.addView(separator, new LinearLayout.LayoutParams(dp(12), dp(38)));
            }
        }
    }

    private void openDrawer() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(72, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setBackground(round(Color.rgb(247, 250, 255), dp(18), LINE));
        panel.setPadding(dp(20), dp(12), dp(20), dp(12));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold("菜单"), new LinearLayout.LayoutParams(0, -2, 1));
        Button close = iconButton("×");
        close.setContentDescription("关闭菜单");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        panel.addView(gap(8));
        for (String item : new String[]{"assistants", "subscriptions", "service", "wallet", "settings"}) {
            View entry = drawerEntry(item);
            entry.setOnClickListener(v -> {
                section = item;
                localAutoScrollEnabled = true;
                refreshBottomNav();
                renderSection();
                dialog.dismiss();
            });
            panel.addView(entry);
        }
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(dp(236), -2);
        panelLp.gravity = Gravity.END | Gravity.TOP;
        panelLp.setMargins(0, dp(70), dp(14), 0);
        overlay.addView(panel, panelLp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void renderSection() {
        if (content == null || composerHost == null) {
            return;
        }
        int generation = ++renderGeneration;
        String targetSection = section;
        hidePageLoading();
        resetContentScrollPosition();
        activeBubbleActions = null;
        cliProjectTagView = null;
        clearContainer(content);
        clearContainer(composerHost);
        content.addView(centerLoadingView("正在加载" + label(targetSection) + "..."));
        updateScrollDock();
        handler.post(() -> renderSectionBody(targetSection, generation));
    }

    private void renderSectionBody(String targetSection, int generation) {
        if (generation != renderGeneration || !targetSection.equals(section) || content == null || composerHost == null) {
            return;
        }
        activeBubbleActions = null;
        cliProjectTagView = null;
        clearContainer(content);
        clearContainer(composerHost);
        try {
            if ("codex".equals(targetSection) || "claude".equals(targetSection)) {
                renderCliWorkspace(targetSection);
            } else if ("settings".equals(targetSection)) {
                renderMe();
            } else if ("assistants".equals(targetSection)) {
                renderAiChat();
            } else if ("subscriptions".equals(targetSection)) {
                renderSubscriptions();
            } else if ("service".equals(targetSection)) {
                renderServiceStatus();
            } else if ("wallet".equals(targetSection)) {
                renderWallet();
            } else if ("chat".equals(targetSection)) {
                renderChat();
            } else if ("image".equals(targetSection)) {
                renderImage();
            } else {
                renderPlaceholder(label(targetSection));
            }
        } catch (Exception error) {
            clearContainer(content);
            clearContainer(composerHost);
            content.addView(messageBubble("assistant", "页面渲染失败，请切换标签后重试。"));
        }
        updateScrollDock();
    }

    private void renderChat() {
        renderLocalConversation("chat");
        composerHost.addView(composer("输入消息", "chat", this::sendChatMessage));
    }

    private void renderImage() {
        renderLocalConversation("image");
        composerHost.addView(composer("描述要生成或编辑的图片", "image", this::sendImageMessage));
    }

    private void renderAiChat() {
        renderLocalConversation("chat");
        composerHost.addView(composer("输入 AIChat 消息", "chat", this::sendChatMessage));
    }

    private void renderLocalConversation(String mode) {
        renderLocalConversationNow(mode, true);
    }

    private void renderLocalConversationNow(String mode, boolean scrollLatest) {
        if (content == null) {
            return;
        }
        int generation = ++renderGeneration;
        showPageLoading("正在加载聊天记录...");
        resetContentScrollPosition();
        clearContainer(content);
        JSONArray messages = localConversationMessages(mode);
        int visibleCount = localVisibleMessageCounts.getOrDefault(mode, INITIAL_RENDER_ITEM_COUNT);
        int startIndex = Math.max(0, messages.length() - visibleCount);
        if (messages.length() == 0) {
            hidePageLoading();
            content.addView(card(label(mode), "暂无聊天记录。点击会话记录可切换历史会话，或直接输入开始新会话。"));
            updateScrollDock();
            return;
        }
        if (startIndex > 0) {
            content.addView(loadEarlierButton("加载更早聊天记录（剩余 " + startIndex + " 条）", () -> {
                localVisibleMessageCounts.put(mode, visibleCount + RENDER_PAGE_INCREMENT);
                renderLocalConversationNow(mode, false);
            }));
        }
        handler.post(() -> renderLocalConversationChunk(mode, messages, startIndex, startIndex, generation, scrollLatest));
    }

    private void renderLocalConversationChunk(String mode, JSONArray messages, int startIndex, int index, int generation, boolean scrollLatest) {
        if (generation != renderGeneration || content == null) {
            return;
        }
        int end = Math.min(messages.length(), index + RENDER_CHUNK_SIZE);
        for (int i = index; i < end; i++) {
            JSONObject item = messages.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String type = item.optString("type", "text");
            String role = item.optString("role", "assistant");
            String text = cleanDisplayText(item.optString("text", ""));
            if (text.isEmpty()) {
                continue;
            }
            long timestamp = item.optLong("timestamp", 0);
            if ("image".equals(type)) {
                content.addView(imageResultBubble(text, timestamp, mode, i, role));
            } else {
                String renderMode = (!"user".equals(role) && i < messages.length() - 3) ? mode + ":plain" : mode;
                content.addView(messageBubble(role, text, timestamp, renderMode, i));
            }
        }
        if (end < messages.length()) {
            handler.post(() -> renderLocalConversationChunk(mode, messages, startIndex, end, generation, scrollLatest));
            return;
        }
        hidePageLoading();
        if (scrollLatest) {
            localAutoScrollEnabled = true;
            scrollToBottom();
        }
        updateScrollDock();
    }

    private View loadingRow(String text) {
        LinearLayout row = vertical();
        row.setGravity(Gravity.CENTER);
        row.addView(new DrawingProgressView(this), new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(18), 0, dp(18));
        row.setLayoutParams(lp);
        return row;
    }

    private void clearContainer(LinearLayout group) {
        if (group == null) {
            return;
        }
        destroyWebViews(group);
        group.removeAllViews();
    }

    private void destroyWebViews(View rootView) {
        if (rootView == null) {
            return;
        }
        if (rootView instanceof WebView) {
            WebView web = (WebView) rootView;
            try {
                web.stopLoading();
                web.loadUrl("about:blank");
                web.removeAllViews();
                web.destroy();
            } catch (Exception ignored) {
            }
            return;
        }
        if (rootView instanceof LinearLayout) {
            LinearLayout group = (LinearLayout) rootView;
            for (int i = 0; i < group.getChildCount(); i++) {
                destroyWebViews(group.getChildAt(i));
            }
        } else if (rootView instanceof FrameLayout) {
            FrameLayout group = (FrameLayout) rootView;
            for (int i = 0; i < group.getChildCount(); i++) {
                destroyWebViews(group.getChildAt(i));
            }
        } else if (rootView instanceof ScrollView) {
            ScrollView group = (ScrollView) rootView;
            for (int i = 0; i < group.getChildCount(); i++) {
                destroyWebViews(group.getChildAt(i));
            }
        }
    }

    private View centerLoadingView(String text) {
        LinearLayout wrap = vertical();
        wrap.setGravity(Gravity.CENTER);
        wrap.setMinimumHeight(Math.max(dp(360), getResources().getDisplayMetrics().heightPixels - dp(260)));
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        wrap.addView(progress, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView label = copy(text);
        label.setTextColor(MUTED);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
        labelLp.setMargins(0, dp(10), 0, 0);
        wrap.addView(label, labelLp);
        return wrap;
    }

    private void showPageLoading(String text) {
        if (shell == null) {
            return;
        }
        if (loadingOverlay == null) {
            FrameLayout overlay = new FrameLayout(this);
            overlay.setClickable(true);
            overlay.setBackgroundColor(Color.argb(18, 255, 255, 255));
            LinearLayout card = vertical();
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackground(round(Color.argb(236, 255, 255, 255), dp(18), Color.argb(96, 145, 160, 184)));
            ProgressBar progress = new ProgressBar(this);
            progress.setIndeterminate(true);
            card.addView(progress, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView label = copy(text);
            label.setTag("page_loading_text");
            label.setTextColor(MUTED);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
            labelLp.setMargins(0, dp(8), 0, 0);
            card.addView(label, labelLp);
            overlay.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            loadingOverlay = overlay;
        }
        TextView label = taggedTextView(loadingOverlay, "page_loading_text");
        if (label != null) {
            label.setText(text);
        }
        if (loadingOverlay.getParent() == null) {
            shell.addView(loadingOverlay, new FrameLayout.LayoutParams(-1, -1));
        }
        loadingOverlay.bringToFront();
    }

    private void hidePageLoading() {
        if (loadingOverlay != null && loadingOverlay.getParent() instanceof FrameLayout) {
            ((FrameLayout) loadingOverlay.getParent()).removeView(loadingOverlay);
        }
    }

    private void renderPlaceholder(String name) {
        content.addView(card(name, "暂无内容"));
    }

    private void renderSubscriptions() {
        content.addView(sectionTitle("套餐订阅"));
        LinearLayout list = vertical();
        list.setTag("subscription_list");
        content.addView(list);
        list.addView(card("正在读取", "正在同步客户端套餐订阅内容。"));
        refreshSubscriptions();
    }

    private void renderServiceStatus() {
        content.addView(sectionTitle("服务状态"));
        LinearLayout list = vertical();
        list.setTag("service_status_list");
        content.addView(list);
        list.addView(card("正在读取", "正在同步客户端服务状态内容。"));
        refreshServiceStatus();
    }

    private void renderWallet() {
        content.addView(sectionTitle("我的钱包"));
        LinearLayout list = vertical();
        list.setTag("wallet_list");
        content.addView(list);
        list.addView(card("正在读取", "正在同步客户端钱包总览和最近账单。"));
        refreshWallet();
    }

    private void renderMe() {
        content.addView(sectionTitle("系统设置"));
        renderAnnouncementsModule();

        LinearLayout list = vertical();
        list.setTag("device_list");
        content.addView(list);
        renderDeviceList(list, new JSONArray());
        refreshDevices(true);

        renderVersionModule();

        LinearLayout account = cardPanel();
        account.addView(bold("账户"));
        account.addView(copy("服务器：" + prefs.getString("server", "")));
        Button logout = danger("退出登录");
        logout.setOnClickListener(v -> {
            prefs.edit().remove("cookie").remove("user_id").remove("bound_device_id").apply();
            boundDeviceId = "";
            showLogin();
        });
        account.addView(gap(10));
        account.addView(logout, compactCenteredButtonLp());
        content.addView(account);
    }

    private void renderAnnouncementsModule() {
        LinearLayout panel = cardPanel();
        panel.setTag("announcements_panel");
        panel.addView(bold("系统公告"));
        panel.addView(copy("正在读取系统公告。"));
        content.addView(panel);
        refreshAnnouncements();
    }

    private void refreshAnnouncements() {
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/status", null);
            JSONObject data = envelope.optJSONObject("data");
            JSONArray announcements = data == null ? null : data.optJSONArray("announcements");
            JSONArray finalAnnouncements = announcements == null ? new JSONArray() : announcements;
            ui(() -> {
                View maybe = content == null ? null : content.findViewWithTag("announcements_panel");
                if (maybe instanceof LinearLayout) {
                    renderAnnouncementsList((LinearLayout) maybe, finalAnnouncements);
                }
            });
        });
    }

    private void renderAnnouncementsList(LinearLayout panel, JSONArray announcements) {
        panel.removeAllViews();
        panel.addView(bold("系统公告"));
        if (announcements.length() == 0) {
            panel.addView(copy("暂无系统公告。"));
            return;
        }
        for (int i = 0; i < announcements.length(); i++) {
            JSONObject item = announcements.optJSONObject(i);
            if (item == null) {
                continue;
            }
            Button entry = drawerButton(announcementPreview(item));
            entry.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            entry.setOnClickListener(v -> showAnnouncementDialog(item));
            panel.addView(entry);
        }
    }

    private String announcementPreview(JSONObject item) {
        String content = item.optString("content", "").replace('\n', ' ').trim();
        if (content.isEmpty()) {
            content = item.optString("extra", "系统公告").replace('\n', ' ').trim();
        }
        if (content.length() > 26) {
            content = content.substring(0, 26) + "...";
        }
        return content.isEmpty() ? "系统公告" : content;
    }

    private void showAnnouncementDialog(JSONObject item) {
        StringBuilder text = new StringBuilder();
        String content = item.optString("content", "").trim();
        String extra = item.optString("extra", "").trim();
        String publishDate = item.optString("publishDate", "").trim();
        if (!content.isEmpty()) {
            text.append(content);
        }
        if (!extra.isEmpty()) {
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(extra);
        }
        if (!publishDate.isEmpty()) {
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append("发布时间：").append(publishDate);
        }
        showTextDialog("系统公告", text.length() == 0 ? "暂无公告详情。" : text.toString());
    }

    private void renderVersionModule() {
        LinearLayout panel = cardPanel();
        panel.addView(bold("版本信息"));
        panel.addView(copy("当前版本：" + currentVersionName()));
        TextView progress = copy("");
        progress.setGravity(Gravity.CENTER);
        progress.setVisibility(View.GONE);
        panel.addView(progress);
        Button check = primary("检查更新");
        check.setOnClickListener(v -> handleAndroidUpdateAction(check, progress));
        panel.addView(check, compactCenteredButtonLp());
        content.addView(panel);
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "1.0.0";
        }
    }

    private void handleAndroidUpdateAction(Button action, TextView progress) {
        if (downloadedAndroidUpdateUri != null) {
            installDownloadedApk(downloadedAndroidUpdateUri);
            return;
        }
        if (!pendingAndroidUpdateUrl.isEmpty()) {
            downloadAndroidUpdate(action, progress, pendingAndroidUpdateUrl, pendingAndroidUpdateVersion);
            return;
        }
        checkAndroidUpdate(action, progress);
    }

    private void checkAndroidUpdate(Button action, TextView progress) {
        action.setText("正在检查");
        runNetwork(() -> {
            JSONObject envelope = api("GET", "/api/download/packages", null);
            JSONObject data = envelope.optJSONObject("data");
            JSONObject android = data == null ? null : data.optJSONObject("android");
            String latest = android == null ? "" : android.optString("version", "");
            String url = android == null ? "" : android.optString("url", "");
            if (url.isEmpty()) {
                url = "/api/download/package/android";
            }
            String finalUrl = resolveServerUrl(url);
            boolean hasUpdate = !latest.isEmpty() && compareVersion(latest, currentVersionName()) > 0;
            ui(() -> {
                if (hasUpdate) {
                    pendingAndroidUpdateUrl = finalUrl;
                    pendingAndroidUpdateVersion = latest;
                    downloadedAndroidUpdateUri = null;
                    action.setText("下载更新");
                    toast("发现新版本 " + latest);
                } else {
                    pendingAndroidUpdateUrl = "";
                    pendingAndroidUpdateVersion = "";
                    downloadedAndroidUpdateUri = null;
                    action.setText("检查更新");
                    progress.setVisibility(View.GONE);
                    showTextDialog("检查更新", "已是最新版");
                }
            });
        });
    }

    private void downloadAndroidUpdate(Button action, TextView progress, String url, String version) {
        if (updateDownloading) {
            return;
        }
        updateDownloading = true;
        action.setEnabled(false);
        action.setText("更新中");
        progress.setVisibility(View.VISIBLE);
        progress.setText("下载进度 0%");
        executor.execute(() -> {
            try {
                Uri uri = downloadApk(url, version, percent -> ui(() -> progress.setText("下载进度 " + percent + "%")));
                ui(() -> {
                    updateDownloading = false;
                    downloadedAndroidUpdateUri = uri;
                    action.setEnabled(true);
                    action.setText("现在安装");
                    progress.setText("下载完成");
                });
            } catch (Exception e) {
                ui(() -> {
                    updateDownloading = false;
                    downloadedAndroidUpdateUri = null;
                    action.setEnabled(true);
                    action.setText("下载更新");
                    progress.setVisibility(View.GONE);
                    toast(userMessage(e));
                });
            }
        });
    }

    private String resolveServerUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String base = prefs.getString("server", "").replaceAll("/+$", "");
        if (trimmed.startsWith("/")) {
            return base + trimmed;
        }
        return base + "/" + trimmed;
    }

    private void openExternalUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("无法打开下载链接");
        }
    }

    private Uri downloadApk(String url, String version, ProgressHandler progress) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(180000);
        String cookie = prefs.getString("cookie", "");
        if (!cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        int code = conn.getResponseCode();
        if (code >= 400) {
            String raw = readStream(conn.getErrorStream());
            conn.disconnect();
            throw new ApiException(code, raw, "安装包下载失败，请稍后重试");
        }
        int total = conn.getContentLength();
        Uri target = createApkTargetUri(version);
        try (InputStream input = conn.getInputStream(); OutputStream output = openApkOutputStream(target)) {
            byte[] buffer = new byte[16 * 1024];
            long read = 0;
            int lastPercent = -1;
            int len;
            while ((len = input.read(buffer)) >= 0) {
                output.write(buffer, 0, len);
                read += len;
                if (total > 0) {
                    int percent = Math.max(0, Math.min(100, (int) (read * 100 / total)));
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        progress.onProgress(percent);
                    }
                }
            }
        } catch (Exception e) {
            getContentResolver().delete(target, null, null);
            throw e;
        } finally {
            conn.disconnect();
        }
        publishApkTarget(target);
        progress.onProgress(100);
        return target;
    }

    private Uri createApkTargetUri(String version) throws Exception {
        String safeVersion = (version == null || version.trim().isEmpty() ? currentVersionName() : version).replaceAll("[^0-9A-Za-z._-]", "_");
        String name = "OneAPI-" + safeVersion + ".apk";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new Exception("安装包保存失败");
            }
            return uri;
        }
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
        return Uri.fromFile(file);
    }

    private OutputStream openApkOutputStream(Uri uri) throws Exception {
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            return new FileOutputStream(file);
        }
        OutputStream output = getContentResolver().openOutputStream(uri);
        if (output == null) {
            throw new Exception("安装包保存失败");
        }
        return output;
    }

    private void publishApkTarget(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !"file".equals(uri.getScheme())) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(uri, values, null, null);
        }
    }

    private void installDownloadedApk(Uri uri) {
        try {
            if ("file".equals(uri.getScheme())) {
                try {
                    Class.forName("android.os.StrictMode").getMethod("disableDeathOnFileUriExposure").invoke(null);
                } catch (Exception ignored) {
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            toast("无法启动安装，请检查系统安装权限");
        }
    }

    private int compareVersion(String left, String right) {
        String[] a = left == null ? new String[0] : left.split("\\.");
        String[] b = right == null ? new String[0] : right.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int av = i < a.length ? parseVersionPart(a[i]) : 0;
            int bv = i < b.length ? parseVersionPart(b[i]) : 0;
            if (av != bv) {
                return av - bv;
            }
        }
        return 0;
    }

    private int parseVersionPart(String value) {
        try {
            String digits = value == null ? "" : value.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void renderCliWorkspace(String client) {
        modelSpinner = spinner("codex".equals(client) ? codexModels : claudeModels);
        reasoningSpinner = spinner(list("关闭思考", "低", "中", "高"));
        permissionSpinner = spinner(list("受限模式", "全权限模式"));
        if (selectedCliModel.isEmpty()) {
            selectedCliModel = selected(modelSpinner);
        }

        interactionBar = vertical();
        content.addView(interactionBar);
        timeline = vertical();
        content.addView(timeline);
        JSONArray cached = cliTimelineCache.get(client);
        if (cached == null || cached.length() == 0) {
            timeline.addView(loadingCard("正在同步客户端会话记录..."));
            cliTimelineSignatures.put(client, "loading:" + System.currentTimeMillis());
        } else {
            renderTimeline(cached, true);
        }
        composerHost.addView(composer("输入要执行的任务", client, () -> sendCliJob(client)));
        pollSessionsOnce(false);
    }

    private View composer(String hint, String mode, Runnable sendAction) {
        LinearLayout box = vertical();
        activeComposerMode = mode;
        if (!"chat".equals(mode) && selectedAttachmentUris.size() > 1) {
            while (selectedAttachmentUris.size() > 1) {
                selectedAttachmentUris.remove(selectedAttachmentUris.size() - 1);
            }
        }
        box.setBackground(round(Color.argb(20, 255, 255, 255), dp(18), LINE));
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        attachmentPreviewHost = horizontal();
        attachmentPreviewHost.setPadding(0, 0, 0, dp(8));
        box.addView(attachmentPreviewHost);
        extensionPreviewHost = horizontal();
        extensionPreviewHost.setPadding(0, 0, 0, dp(8));
        box.addView(extensionPreviewHost);
        renderAttachmentPreview();
        renderExtensionPreview();
        if ("chat".equals(mode) || "image".equals(mode)) {
            box.addView(currentAssistantTag(mode));
        } else if ("codex".equals(mode) || "claude".equals(mode)) {
            box.addView(currentCliProjectTag(mode));
        }
        activeInput = input(hint);
        if ("assistants".equals(section)) {
            activeInput.setBackground(round(Color.argb(18, 255, 255, 255), dp(14), LINE));
        }
        activeInput.setMinLines(2);
        activeInput.setMaxLines(5);
        activeInput.setVerticalScrollBarEnabled(false);
        activeInput.setOverScrollMode(View.OVER_SCROLL_NEVER);
        activeInput.setGravity(Gravity.TOP | Gravity.START);
        activeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        activeInput.setOnTouchListener((v, event) -> {
            if (event != null && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                focusComposerInput();
            }
            return false;
        });
        activeInput.setOnClickListener(v -> focusComposerInput());
        activeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                cancelComposerKeyboardRequest();
            }
        });
        box.addView(activeInput);

        LinearLayout row = horizontal();
        row.setPadding(dp(4), dp(8), dp(4), 0);
        LinearLayout left = horizontal();
        addComposerTools(left, mode);
        LinearLayout right = horizontal();
        ImageButton send = new ImageButton(this);
        send.setContentDescription("发送");
        send.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        send.setPadding(dp(6), dp(6), dp(6), dp(6));
        send.setBackground(new ColorDrawable(Color.TRANSPARENT));
        activeSendButton = send;
        syncSendButton();
        send.setOnClickListener(v -> {
            if (requestRunning) {
                setSending(false);
                return;
            }
            sendAction.run();
        });
        right.addView(send, new LinearLayout.LayoutParams(dp(40), dp(40)));
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(-2, -2);
        rightLp.setMargins(dp(8), 0, 0, 0);
        row.addView(right, rightLp);
        box.addView(row);
        return box;
    }

    private View currentAssistantTag(String mode) {
        TextView tag = copy("助手：" + ("image".equals(mode) ? selectedImageAssistant : selectedChatAssistant));
        tag.setTextColor(Color.rgb(45, 74, 124));
        tag.setTextSize(12);
        tag.setSingleLine(true);
        tag.setEllipsize(TextUtils.TruncateAt.END);
        tag.setPadding(dp(10), dp(4), dp(10), dp(4));
        tag.setBackground(round(Color.rgb(255, 255, 255), dp(12), LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(28));
        lp.setMargins(0, 0, 0, dp(6));
        tag.setLayoutParams(lp);
        return tag;
    }

    private View currentCliProjectTag(String client) {
        String name = selectedCliProjectNames.get(client);
        if (name == null || name.trim().isEmpty()) {
            String path = selectedCliProjectPaths.get(client);
            name = fileNameFromPath(path == null ? "" : path);
        }
        if (name == null || name.trim().isEmpty()) {
            name = "未选择";
        }
        TextView tag = copy("项目：" + ellipsizeLabel(name, 8));
        cliProjectTagView = tag;
        tag.setTextColor(Color.rgb(45, 74, 124));
        tag.setTextSize(12);
        tag.setSingleLine(true);
        tag.setEllipsize(TextUtils.TruncateAt.END);
        tag.setPadding(dp(10), dp(4), dp(10), dp(4));
        tag.setBackground(round(Color.rgb(255, 255, 255), dp(12), LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(28));
        lp.setMargins(0, 0, 0, dp(6));
        tag.setLayoutParams(lp);
        return tag;
    }

    private String ellipsizeLabel(String value, int maxChars) {
        String clean = cleanDisplayText(value);
        if (clean.length() <= maxChars) {
            return clean;
        }
        return clean.substring(0, Math.max(0, maxChars)) + "...";
    }

    private void addComposerTools(LinearLayout left, String mode) {
        if ("chat".equals(mode)) {
            addToolButton(left, R.drawable.tool_add, "上传附件", this::openImagePicker);
            addToolButton(left, R.drawable.tool_history, "会话记录", () -> showRecentSessions(mode));
            addToolButton(left, R.drawable.tool_module, "模型选择", () -> showModelChoice("模型选择", chatModelOptions(), selectedChatModel, "chat", value -> {
                selectedChatModel = value;
                prefs.edit().putString("selected_chat_model", value).apply();
            }));
            addToolButton(left, R.drawable.tool_helper, "助手", () -> showChoice("助手", chatAssistantOptions(), selectedChatAssistant, value -> {
                if (value.equals(selectedChatAssistant)) {
                    return;
                }
                selectedChatAssistant = value;
                prefs.edit().putString("selected_chat_assistant", value).apply();
                createLocalConversation("chat");
                renderSection();
            }));
            addToolButton(left, R.drawable.tool_think, "Thinking", () -> showChoice("Thinking", list("关闭思考", "低", "中", "高"), selectedReasoning, value -> {
                selectedReasoning = value;
                prefs.edit().putString("selected_reasoning", value).apply();
            }));
            addToolButton(left, R.drawable.tool_text, "上下文", () -> showChoice("上下文", list("自动", "短上下文", "长上下文"), selectedContextWindow, value -> {
                selectedContextWindow = value;
                prefs.edit().putString("selected_context_window", value).apply();
            }));
            return;
        }
        if ("image".equals(mode)) {
            addToolButton(left, R.drawable.tool_add, "上传附件", this::openImagePicker);
            addToolButton(left, R.drawable.tool_history, "会话记录", () -> showRecentSessions(mode));
            addToolButton(left, R.drawable.tool_helper, "助手", () -> showChoice("助手", imageAssistantOptions(), selectedImageAssistant, value -> {
                if (value.equals(selectedImageAssistant)) {
                    return;
                }
                selectedImageAssistant = value;
                prefs.edit().putString("selected_image_assistant", value).apply();
                createLocalConversation("image");
                renderSection();
            }));
            addToolButton(left, R.drawable.tool_size, "尺寸", () -> showChoice("尺寸", list("1024x1024", "1024x1536", "1536x1024"), selectedImageSize, value -> {
                selectedImageSize = value;
                prefs.edit().putString("selected_image_size", value).apply();
            }));
            addToolButton(left, R.drawable.tool_ratio, "质量", () -> showChoice("质量", list("标准", "高清"), selectedImageQuality, value -> {
                selectedImageQuality = value;
                prefs.edit().putString("selected_image_quality", value).apply();
            }));
            return;
        }
        if ("codex".equals(mode) || "claude".equals(mode)) {
            addToolButton(left, R.drawable.tool_history, "会话记录", () -> showRecentSessions(mode));
            addToolButton(left, selectedPermission.contains("全权限") ? R.drawable.tool_authority : R.drawable.tool_noauthority, "权限模式", () -> {
                selectedPermission = selectedPermission.contains("全权限") ? "受限模式" : "全权限模式";
                prefs.edit().putString("selected_permission", selectedPermission).apply();
                renderSection();
            });
            addToolButton(left, R.drawable.tool_module, "模型选择", () -> showModelChoice("模型选择", "codex".equals(mode) ? codexModels : claudeModels, selectedCliModelFor(mode), mode, value -> {
                if ("codex".equals(mode)) {
                    selectedCodexModel = value;
                    prefs.edit().putString("selected_codex_model", value).apply();
                } else {
                    selectedClaudeModel = value;
                    prefs.edit().putString("selected_claude_model", value).apply();
                }
            }));
            addToolButton(left, R.drawable.tool_think, "Thinking", () -> showChoice("Thinking", list("关闭思考", "低", "中", "高"), selectedReasoning, value -> {
                selectedReasoning = value;
                prefs.edit().putString("selected_reasoning", value).apply();
            }));
            addToolButton(left, R.drawable.tool_skill, "Skill/Plugin", () -> showExtensionsChoice(mode));
        }
    }

    private void addToolButton(LinearLayout row, int iconRes, String description, Runnable action) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(6), dp(6), dp(6), dp(6));
        button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        button.setColorFilter(INK);
        button.setContentDescription(description);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
        lp.setMargins(0, 0, dp(8), 0);
        row.addView(button, lp);
    }

    private void renderAttachmentPreview() {
        if (attachmentPreviewHost == null) {
            return;
        }
        attachmentPreviewHost.removeAllViews();
        if (selectedAttachmentUris.isEmpty()) {
            attachmentPreviewHost.setVisibility(View.GONE);
            return;
        }
        attachmentPreviewHost.setVisibility(View.VISIBLE);
        for (int i = 0; i < selectedAttachmentUris.size(); i++) {
            Uri uri = selectedAttachmentUris.get(i);
            FrameLayout thumbBox = new FrameLayout(this);
            ImageView thumb = new ImageView(this);
            setImageSource(thumb, uri.toString());
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackground(round(Color.WHITE, dp(12), LINE));
            thumb.setContentDescription("已选择图片预览");
            thumb.setOnClickListener(v -> showImagePreview(uri));
            thumbBox.addView(thumb, new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER));
            Button remove = iconButton("×");
            remove.setTextSize(13);
            remove.setTextColor(Color.WHITE);
            remove.setBackground(round(Color.argb(180, 23, 31, 48), dp(10), Color.TRANSPARENT));
            remove.setContentDescription("移除图片");
            final int index = i;
            remove.setOnClickListener(v -> {
                if (index >= 0 && index < selectedAttachmentUris.size()) {
                    selectedAttachmentUris.remove(index);
                    renderAttachmentPreview();
                }
            });
            FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP | Gravity.END);
            thumbBox.addView(remove, closeLp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(62), dp(62));
            lp.setMargins(0, 0, dp(8), 0);
            attachmentPreviewHost.addView(thumbBox, lp);
        }
    }

    private void showImagePreview(Uri uri) {
        if (uri == null) {
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(180, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold("图片预览"), new LinearLayout.LayoutParams(0, -2, 1));
        Button close = iconButton("×");
        close.setContentDescription("关闭预览");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        WebView preview = new WebView(this);
        configureContentWebView(preview);
        preview.loadDataWithBaseURL("https://ai.oneapi.center/", imagePreviewHtml(imageSourceForPreview(uri)), "text/html", "utf-8", null);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(420));
        previewLp.setMargins(0, dp(10), 0, 0);
        panel.addView(preview, previewLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
        lp.gravity = Gravity.CENTER;
        lp.setMargins(dp(18), 0, dp(18), 0);
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private String imageSourceForPreview(Uri uri) {
        String source = uri == null ? "" : uri.toString();
        if (source.startsWith("content:") || source.startsWith("file:")) {
            String dataUrl = uriToDataUrl(uri);
            return dataUrl.isEmpty() ? source : dataUrl;
        }
        return source;
    }

    private String imageSourceForBubble(Uri uri) {
        if (uri == null) {
            return "";
        }
        String cached = cacheImageForBubble(uri);
        return cached.isEmpty() ? uri.toString() : cached;
    }

    private String cacheImageForBubble(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "chat-images");
            if (!dir.exists() && !dir.mkdirs()) {
                return "";
            }
            String mime = getContentResolver().getType(uri);
            String ext = mime != null && mime.toLowerCase(Locale.ROOT).contains("webp") ? ".webp"
                    : mime != null && mime.toLowerCase(Locale.ROOT).contains("jpeg") ? ".jpg"
                    : ".png";
            File target = new File(dir, UUID.randomUUID().toString() + ext);
            try (InputStream input = getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(target)) {
                if (input == null) {
                    return "";
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            return Uri.fromFile(target).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String imagePreviewHtml(String source) {
        String jsSource = JSONObject.quote(source == null ? "" : source);
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body,#frame{height:100%;margin:0;background:#0f172a;overflow:hidden}#frame{display:flex;align-items:center;justify-content:center}img{max-width:100%;max-height:100%;object-fit:contain;touch-action:none;user-select:none;transform-origin:center center;will-change:transform}</style>"
                + "</head><body><div id='frame'><img id='img'></div><script>const img=document.getElementById('img');img.src=" + jsSource + ";let scale=1,x=0,y=0,drag=false,lx=0,ly=0;"
                + "function apply(){img.style.transform='translate('+x+'px,'+y+'px) scale('+scale+')'}"
                + "img.ondblclick=(e)=>{e.preventDefault();scale=scale>1?1:2.4;x=0;y=0;apply()};"
                + "img.onpointerdown=(e)=>{if(scale<=1)return;drag=true;lx=e.clientX;ly=e.clientY;img.setPointerCapture&&img.setPointerCapture(e.pointerId)};"
                + "img.onpointermove=(e)=>{if(!drag)return;x+=e.clientX-lx;y+=e.clientY-ly;lx=e.clientX;ly=e.clientY;apply();e.preventDefault()};"
                + "img.onpointerup=img.onpointercancel=()=>{drag=false};</script></body></html>";
    }

    private void renderExtensionPreview() {
        if (extensionPreviewHost == null) {
            return;
        }
        extensionPreviewHost.removeAllViews();
        if (selectedExtensionRefs.isEmpty()) {
            extensionPreviewHost.setVisibility(View.GONE);
            return;
        }
        extensionPreviewHost.setVisibility(View.VISIBLE);
        for (int i = 0; i < selectedExtensionRefs.size(); i++) {
            JSONObject ref = selectedExtensionRefs.get(i);
            LinearLayout tag = horizontal();
            tag.setGravity(Gravity.CENTER_VERTICAL);
            tag.setPadding(dp(9), 0, dp(5), 0);
            tag.setBackground(round(Color.argb(76, 54, 104, 240), dp(14), Color.argb(110, 54, 104, 240)));
            TextView name = copy(ref.optString("name", ""));
            name.setTextColor(BLUE);
            name.setTextSize(12);
            tag.addView(name);
            Button remove = iconButton("×");
            remove.setTextSize(13);
            final int index = i;
            remove.setOnClickListener(v -> {
                if (index >= 0 && index < selectedExtensionRefs.size()) {
                    selectedExtensionRefs.remove(index);
                    renderExtensionPreview();
                }
            });
            tag.addView(remove, new LinearLayout.LayoutParams(dp(24), dp(24)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(32));
            lp.setMargins(0, 0, dp(8), 0);
            extensionPreviewHost.addView(tag, lp);
        }
    }

    private void addExtensionRef(String kind, String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || "默认".equals(normalized)) {
            return;
        }
        for (JSONObject ref : selectedExtensionRefs) {
            if (normalized.equals(ref.optString("name")) && kind.equals(ref.optString("kind"))) {
                renderExtensionPreview();
                return;
            }
        }
        try {
            selectedExtensionRefs.add(new JSONObject().put("kind", kind).put("id", normalized).put("name", normalized));
            renderExtensionPreview();
        } catch (Exception ignored) {
        }
    }

    private void appendSelectedImages(Intent data) {
        int limit = "chat".equals(activeComposerMode) ? 5 : 1;
        if (!"chat".equals(activeComposerMode)) {
            selectedAttachmentUris.clear();
        }
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount() && selectedAttachmentUris.size() < limit; i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null && !selectedAttachmentUris.contains(uri)) {
                    selectedAttachmentUris.add(uri);
                }
            }
        } else if (data.getData() != null && selectedAttachmentUris.size() < limit) {
            Uri uri = data.getData();
            if (!selectedAttachmentUris.contains(uri)) {
                selectedAttachmentUris.add(uri);
            }
        }
        if (selectedAttachmentUris.size() > limit) {
            while (selectedAttachmentUris.size() > limit) {
                selectedAttachmentUris.remove(selectedAttachmentUris.size() - 1);
            }
        }
    }

    private void openImagePicker() {
        int limit = "chat".equals(activeComposerMode) ? 5 : 1;
        Intent intent;
        if (Build.VERSION.SDK_INT >= 33) {
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            if (limit > 1) {
                intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, limit);
            }
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, limit > 1);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择图片"), REQ_PICK_IMAGE);
        } catch (Exception error) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("image/*");
            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(fallback, "选择图片"), REQ_PICK_IMAGE);
        }
    }

    private void persistSelectedImagePermissions(Intent data) {
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(uri, flags);
                    }
                }
            } else if (data.getData() != null) {
                getContentResolver().takePersistableUriPermission(data.getData(), flags);
            }
        } catch (Exception ignored) {
        }
    }

    private List<String> chatModelOptions() {
        List<String> models = new ArrayList<>();
        for (String model : chatModels) {
            if (!models.contains(model)) {
                models.add(model);
            }
        }
        return models;
    }

    private List<String> chatAssistantOptions() {
        List<String> names = new ArrayList<>();
        for (JSONObject assistant : chatAssistants) {
            String name = assistant.optString("name", "");
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
        }
        appendCustomAssistantNames("chat", names);
        if (names.isEmpty()) {
            names.addAll(list("默认助手", "流程图专家（Mermaid）", "产品经理", "项目管理", "前端工程师", "开发工程师", "网页生成", "运维工程师", "网络安全专家", "UX/UI开发者", "法务", "律师", "市场营销"));
        }
        return names;
    }

    private List<String> imageAssistantOptions() {
        List<String> names = new ArrayList<>();
        for (JSONObject assistant : imageAssistants) {
            String name = assistant.optString("name", "");
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
        }
        appendCustomAssistantNames("image", names);
        if (names.isEmpty()) {
            names.addAll(list("通用绘图", "图片编辑", "动画与漫画", "游戏", "复古与赛博朋克", "电影与动画", "角色设计", "字体设计和海报", "插图", "水彩画", "水墨与中国风", "像素艺术", "等距视角", "产品与食品", "品牌系统与标识", "摄影", "信息图表和实地指南", "数字艺术创作助手"));
        }
        return names;
    }

    private void appendCustomAssistantNames(String scope, List<String> names) {
        JSONArray rows = customAssistants(scope);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            String name = item == null ? "" : item.optString("name", "");
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
        }
    }

    private String assistantPrompt(String name) {
        String custom = customAssistantPrompt("chat", name);
        if (!custom.trim().isEmpty()) {
            return custom;
        }
        for (JSONObject assistant : chatAssistants) {
            if (name.equals(assistant.optString("name"))) {
                String prompt = assistant.optString("prompt", "");
                if (!prompt.trim().isEmpty()) {
                    return prompt;
                }
            }
        }
        switch (name) {
            case "流程图专家（Mermaid）": return "你是一名 Mermaid 流程图专家，优先输出可直接渲染的 Mermaid 代码块。";
            case "产品经理": return "你是一名经验丰富的产品经理，请提供务实、清晰、可执行的产品建议。";
            case "项目管理": return "你是一名资深项目经理，请从规划、执行、风险和协作角度回答。";
            case "前端工程师": return "你是一名专业前端工程师，请关注用户体验、性能和实现细节。";
            case "开发工程师": return "你是一名资深软件工程师，请用工程化方式解决问题。";
            case "网页生成": return "你精通 HTML/JS/CSS/TailwindCSS，请按用户要求生成可用网页代码。";
            case "运维工程师": return "你是一名运维工程师，请关注部署、监控、故障处理和安全。";
            case "网络安全专家": return "你是一名网络安全专家，请给出安全分析与防护策略。";
            case "UX/UI开发者": return "你是一名 UX/UI 开发者，请关注界面结构、交互体验和视觉质量。";
            case "法务": return "你是一名法务专家，请从合同、合规和风险角度分析。";
            case "律师": return "你是一名通用法律咨询律师，请保持专业严谨。";
            case "市场营销": return "你是一名市场营销专家，请给出品牌推广和营销策略建议。";
            default: return "你是 OneAPI 客户端中的默认助手。请准确理解用户需求，保持回答简洁、直接、可执行。";
        }
    }

    private String imageAssistantPrompt(String name, String prompt) {
        String custom = customAssistantPrompt("image", name);
        if (!custom.trim().isEmpty()) {
            return custom + "\n\n用户图片需求：" + prompt;
        }
        for (JSONObject assistant : imageAssistants) {
            if (name.equals(assistant.optString("name"))) {
                String assistantPrompt = assistant.optString("prompt", "");
                if (!assistantPrompt.trim().isEmpty()) {
                    return assistantPrompt + "\n\n用户图片需求：" + prompt;
                }
            }
        }
        if ("图片编辑".equals(name)) {
            return "图片编辑：" + prompt;
        }
        if ("动画与漫画".equals(name)) {
            return "动画漫画风格，干净赛璐璐线稿，表情鲜明，动态构图，原创角色，" + prompt;
        }
        if ("游戏".equals(name)) {
            return "现代游戏截图或游戏主视觉风格，场景可读，光影精致，具备可玩空间逻辑，" + prompt;
        }
        if ("复古与赛博朋克".equals(name)) {
            return "复古未来赛博朋克风格，霓虹、铬金属、CRT 光效和清晰网格构图，" + prompt;
        }
        if ("电影与动画".equals(name)) {
            return "电影级动画定格画面，强镜头感、关键光、情绪明确、叙事清晰，" + prompt;
        }
        if ("角色设计".equals(name)) {
            return "角色设计设定图，包含清晰轮廓、服装材质、表情和生产级设计板效果，" + prompt;
        }
        if ("字体设计和海报".equals(name)) {
            return "海报版式与字体层级优先，文字清晰可读，负空间和画面节奏专业，" + prompt;
        }
        if ("水彩画".equals(name)) {
            return "水彩插画风格，纸张肌理、晕染、透明叠色和柔和边缘，" + prompt;
        }
        if ("水墨与中国风".equals(name)) {
            return "水墨中国风，笔触、墨色扩散、宣纸肌理、留白和雅致构图，" + prompt;
        }
        if ("像素艺术".equals(name)) {
            return "像素艺术风格，清晰栅格、有限色盘、游戏素材感、边缘锐利，" + prompt;
        }
        if ("等距视角".equals(name)) {
            return "等距视角场景，网格逻辑精准、层高关系清楚、模块化道具，" + prompt;
        }
        if ("品牌系统与标识".equals(name)) {
            return "品牌系统展示板，原创标识、配色、字体层级、包装或社媒延展，" + prompt;
        }
        if ("商业海报".equals(name)) {
            return "商业海报风格，高级排版，清晰主体，" + prompt;
        }
        if ("产品摄影".equals(name) || "产品与食品".equals(name)) {
            return "产品摄影风格，真实光影，干净背景，" + prompt;
        }
        if ("头像生成".equals(name)) {
            return "高质量头像，细节清晰，" + prompt;
        }
        if ("插画创作".equals(name) || "插图".equals(name) || "数字艺术创作助手".equals(name)) {
            return "数字插画艺术风格，构图完整，" + prompt;
        }
        if ("摄影".equals(name)) {
            return "真实摄影风格，明确拍摄语境、可信镜头、自然瑕疵和场景光线，" + prompt;
        }
        if ("信息图表和实地指南".equals(name)) {
            return "信息图表和实地指南风格，结构清晰、标签可读、图文层级明确，" + prompt;
        }
        return prompt;
    }

    private JSONArray customAssistants(String scope) {
        try {
            return new JSONArray(prefs.getString(scope + "_custom_assistants", "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private void saveCustomAssistants(String scope, JSONArray rows) {
        prefs.edit().putString(scope + "_custom_assistants", rows.toString()).apply();
    }

    private JSONObject findCustomAssistant(String scope, String name) {
        JSONArray rows = customAssistants(scope);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item != null && name.equals(item.optString("name"))) {
                return item;
            }
        }
        return null;
    }

    private String customAssistantPrompt(String scope, String name) {
        JSONObject item = findCustomAssistant(scope, name);
        return item == null ? "" : item.optString("prompt", "");
    }

    private String assistantShareText(String scope, String name) {
        String prompt = "image".equals(scope) ? imageAssistantPrompt(name, "") : assistantPrompt(name);
        return name + (prompt.trim().isEmpty() ? "" : "\n\n" + prompt.trim());
    }

    private void saveCustomAssistant(String scope, String oldName, String name, String prompt) {
        String cleanName = cleanDisplayText(name);
        if (cleanName.isEmpty()) {
            toast("助手名称不能为空");
            return;
        }
        JSONArray rows = customAssistants(scope);
        JSONArray next = new JSONArray();
        try {
            boolean saved = false;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject item = rows.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                if (oldName.equals(item.optString("name")) || cleanName.equals(item.optString("name"))) {
                    if (!saved) {
                        next.put(new JSONObject().put("name", cleanName).put("prompt", prompt == null ? "" : prompt.trim()));
                        saved = true;
                    }
                } else {
                    next.put(item);
                }
            }
            if (!saved) {
                next.put(new JSONObject().put("name", cleanName).put("prompt", prompt == null ? "" : prompt.trim()));
            }
            saveCustomAssistants(scope, next);
            if ("image".equals(scope)) {
                selectedImageAssistant = cleanName;
                prefs.edit().putString("selected_image_assistant", cleanName).apply();
            } else {
                selectedChatAssistant = cleanName;
                prefs.edit().putString("selected_chat_assistant", cleanName).apply();
            }
            renderSection();
        } catch (Exception ignored) {
        }
    }

    private void deleteCustomAssistant(String scope, String name) {
        JSONArray rows = customAssistants(scope);
        boolean removed = false;
        JSONArray next = new JSONArray();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item != null && name.equals(item.optString("name"))) {
                removed = true;
                continue;
            }
            if (item != null) {
                next.put(item);
            }
        }
        if (!removed) {
            toast("内置助手不能删除");
            return;
        }
        saveCustomAssistants(scope, next);
        renderSection();
    }

    private String localConversationGroup(String mode) {
        if ("image".equals(mode)) {
            return selectedImageAssistant == null || selectedImageAssistant.trim().isEmpty() ? "通用绘图" : selectedImageAssistant.trim();
        }
        return selectedChatAssistant == null || selectedChatAssistant.trim().isEmpty() ? "默认助手" : selectedChatAssistant.trim();
    }

    private String activeLocalSessionId(String mode) {
        String value = "image".equals(mode) ? activeImageSessionId : activeChatSessionId;
        if (value == null || value.trim().isEmpty()) {
            value = prefs.getString(mode + "_active_session_id", "");
            if ("image".equals(mode)) {
                activeImageSessionId = value;
            } else {
                activeChatSessionId = value;
            }
        }
        return value == null ? "" : value;
    }

    private void setActiveLocalSessionId(String mode, String id) {
        if ("image".equals(mode)) {
            activeImageSessionId = id;
        } else {
            activeChatSessionId = id;
        }
        localVisibleMessageCounts.put(mode, INITIAL_RENDER_ITEM_COUNT);
        prefs.edit().putString(mode + "_active_session_id", id == null ? "" : id).apply();
    }

    private JSONObject localConversationStore(String mode) {
        String key = mode + "_conversation_store";
        String raw = prefs.getString(key, "{}");
        JSONObject cached = localConversationRootCache.get(mode);
        if (cached != null && raw.equals(localConversationRawCache.get(mode))) {
            return cached;
        }
        try {
            JSONObject root = new JSONObject(raw);
            if (root.optJSONArray("sessions") == null) {
                root.put("sessions", new JSONArray());
            }
            localConversationRawCache.put(mode, raw);
            localConversationRootCache.put(mode, root);
            return root;
        } catch (Exception ignored) {
            try {
                JSONObject root = new JSONObject().put("sessions", new JSONArray());
                localConversationRawCache.put(mode, root.toString());
                localConversationRootCache.put(mode, root);
                return root;
            } catch (Exception impossible) {
                return new JSONObject();
            }
        }
    }

    private void saveLocalConversationStore(String mode, JSONObject root) {
        String raw = root == null ? "{}" : root.toString();
        localConversationRawCache.put(mode, raw);
        if (root != null) {
            localConversationRootCache.put(mode, root);
        } else {
            localConversationRootCache.remove(mode);
        }
        prefs.edit().putString(mode + "_conversation_store", raw).apply();
    }

    private JSONObject findLocalSession(JSONObject root, String id) {
        JSONArray sessions = root.optJSONArray("sessions");
        if (sessions == null || id == null || id.trim().isEmpty()) {
            return null;
        }
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.optJSONObject(i);
            if (session != null && id.equals(session.optString("id"))) {
                return session;
            }
        }
        return null;
    }

    private JSONObject ensureLocalSession(String mode, JSONObject root, String firstPrompt) throws Exception {
        String id = activeLocalSessionId(mode);
        JSONObject session = findLocalSession(root, id);
        if (session != null && !localConversationGroup(mode).equals(session.optString("group"))) {
            session = null;
        }
        if (session == null) {
            JSONArray sessions = root.optJSONArray("sessions");
            id = mode + "-" + System.currentTimeMillis();
            session = new JSONObject()
                    .put("id", id)
                    .put("group", localConversationGroup(mode))
                    .put("title", cleanSessionTitle(firstPrompt))
                    .put("updatedAt", System.currentTimeMillis())
                    .put("messages", new JSONArray());
            sessions.put(session);
            setActiveLocalSessionId(mode, id);
        }
        return session;
    }

    private void createLocalConversation(String mode) {
        try {
            JSONObject root = localConversationStore(mode);
            JSONArray sessions = root.optJSONArray("sessions");
            if (sessions == null) {
                sessions = new JSONArray();
                root.put("sessions", sessions);
            }
            String id = mode + "-" + System.currentTimeMillis();
            JSONObject session = new JSONObject()
                    .put("id", id)
                    .put("group", localConversationGroup(mode))
                    .put("title", "新会话")
                    .put("updatedAt", System.currentTimeMillis())
                    .put("messages", new JSONArray());
            sessions.put(session);
            setActiveLocalSessionId(mode, id);
            trimLocalSessions(root);
            saveLocalConversationStore(mode, root);
        } catch (Exception ignored) {
        }
    }

    private String cleanSessionTitle(String prompt) {
        String title = cleanDisplayText(prompt);
        if (title.isEmpty()) {
            return "新会话";
        }
        return title.substring(0, Math.min(28, title.length()));
    }

    private JSONArray localConversationMessages(String mode) {
        JSONObject root = localConversationStore(mode);
        JSONObject session = resolveActiveLocalSession(mode, root);
        JSONArray messages = session == null ? null : session.optJSONArray("messages");
        return messages == null ? new JSONArray() : messages;
    }

    private JSONObject resolveActiveLocalSession(String mode, JSONObject root) {
        String group = localConversationGroup(mode);
        JSONObject session = findLocalSession(root, activeLocalSessionId(mode));
        if (session != null && group.equals(session.optString("group"))) {
            return session;
        }
        JSONArray sessions = root.optJSONArray("sessions");
        JSONObject best = null;
        if (sessions != null) {
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject item = sessions.optJSONObject(i);
                if (item == null || !group.equals(item.optString("group"))) {
                    continue;
                }
                if (best == null || item.optLong("updatedAt") > best.optLong("updatedAt")) {
                    best = item;
                }
            }
        }
        if (best != null) {
            setActiveLocalSessionId(mode, best.optString("id"));
        }
        return best;
    }

    private int appendLocalConversation(String mode, String role, String type, String text) {
        String clean = cleanDisplayText(text);
        if (clean.isEmpty()) {
            return -1;
        }
        try {
            JSONObject root = localConversationStore(mode);
            JSONObject session = ensureLocalSession(mode, root, clean);
            JSONArray messages = session.optJSONArray("messages");
            if (messages == null) {
                messages = new JSONArray();
                session.put("messages", messages);
            }
            int index = messages.length();
            messages.put(new JSONObject()
                    .put("role", role)
                    .put("type", type)
                    .put("text", clean)
                    .put("timestamp", System.currentTimeMillis()));
            session.put("updatedAt", System.currentTimeMillis());
            if ("user".equals(role)) {
                session.put("title", cleanSessionTitle(clean));
            }
            trimLocalSessions(root);
            saveLocalConversationStore(mode, root);
            return index;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private void trimLocalSessions(JSONObject root) throws Exception {
        JSONArray sessions = root.optJSONArray("sessions");
        if (sessions == null || sessions.length() <= 20) {
            return;
        }
        List<JSONObject> rows = new ArrayList<>();
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject item = sessions.optJSONObject(i);
            if (item != null) {
                rows.add(item);
            }
        }
        rows.sort((a, b) -> Long.compare(b.optLong("updatedAt"), a.optLong("updatedAt")));
        JSONArray next = new JSONArray();
        for (int i = 0; i < Math.min(20, rows.size()); i++) {
            next.put(rows.get(i));
        }
        root.put("sessions", next);
    }

    private void sendChatMessage() {
        String prompt = trim(activeInput);
        if (prompt.isEmpty()) {
            toast("请输入消息");
            return;
        }
        JSONArray contextMessages = chatContextMessagesForRequest();
        hideKeyboard(activeInput);
        setSending(true);
        activeInput.setText("");
        List<Uri> attachments = new ArrayList<>(selectedAttachmentUris);
        selectedAttachmentUris.clear();
        renderAttachmentPreview();
        saveLocalRecent("chat", selectedChatAssistant, prompt);
        for (Uri uri : attachments) {
            String preview = imageSourceForBubble(uri);
            if (!preview.isEmpty()) {
                appendLocalConversation("chat", "user", "image", preview);
                content.addView(imageResultBubble(preview, System.currentTimeMillis(), "chat", -1, "user"));
            }
        }
        appendLocalConversation("chat", "user", "text", prompt);
        content.addView(messageBubble("user", prompt, System.currentTimeMillis(), "chat", -1));
        long assistantStartedAt = System.currentTimeMillis();
        LinearLayout live = addStreamingBubble("assistant", assistantStartedAt);
        updateStreamingBubble(live, "正在思考...", assistantStartedAt, false);
        localAutoScrollEnabled = true;
        scrollToBottom();
        runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("model", resolveChatModel(!attachments.isEmpty()));
            body.put("stream", true);
            String reasoning = reasoningValue(selectedReasoning);
            if (!"off".equals(reasoning)) {
                body.put("reasoning_effort", reasoning);
            }
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", assistantPrompt(selectedChatAssistant) + "\n上下文：" + selectedContextWindow));
            for (int i = 0; i < contextMessages.length(); i++) {
                JSONObject item = contextMessages.optJSONObject(i);
                if (item != null) {
                    messages.put(item);
                }
            }
            if (attachments.isEmpty()) {
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
            } else {
                JSONArray contentParts = new JSONArray();
                contentParts.put(new JSONObject().put("type", "text").put("text", prompt));
                for (Uri uri : attachments) {
                    String dataUrl = uriToDataUrl(uri);
                    if (dataUrl.isEmpty()) {
                        throw new ApiException(0, "", "图片读取失败，请重新选择图片后再发送。");
                    }
                    contentParts.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", dataUrl)));
                }
                messages.put(new JSONObject().put("role", "user").put("content", contentParts));
            }
            body.put("messages", messages);
            String text = streamChatResponse(body, live, assistantStartedAt);
            ui(() -> {
                setSending(false);
                if (text.trim().isEmpty()) {
                    replaceStreamingBubble(live, "本次没有返回可显示内容。", assistantStartedAt);
                    appendLocalConversation("chat", "assistant", "text", "本次没有返回可显示内容。");
                } else {
                    replaceStreamingBubble(live, text, assistantStartedAt);
                    appendLocalConversation("chat", "assistant", "text", text);
                }
                scrollToBottomIfLocalAuto();
            });
        });
    }

    private JSONArray chatContextMessagesForRequest() {
        JSONArray out = new JSONArray();
        JSONArray history = localConversationMessages("chat");
        int limit = contextMessageLimit();
        int start = Math.max(0, history.length() - limit);
        for (int i = start; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            if (item == null || !"text".equals(item.optString("type", "text"))) {
                continue;
            }
            String role = item.optString("role", "");
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            String text = cleanDisplayText(item.optString("text", ""));
            if (text.isEmpty()) {
                continue;
            }
            try {
                out.put(new JSONObject().put("role", role).put("content", trimAssistantContext(text)));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private int contextMessageLimit() {
        if (selectedContextWindow.contains("短")) {
            return 6;
        }
        if (selectedContextWindow.contains("长")) {
            return 20;
        }
        return 12;
    }

    private String trimAssistantContext(String text) {
        String value = cleanDisplayText(text);
        if (value.length() <= 4000) {
            return value;
        }
        return value.substring(value.length() - 4000);
    }

    private void sendImageMessage() {
        String prompt = trim(activeInput);
        if (prompt.isEmpty()) {
            toast("请输入图片描述");
            return;
        }
        hideKeyboard(activeInput);
        setSending(true);
        activeInput.setText("");
        List<Uri> attachments = new ArrayList<>(selectedAttachmentUris);
        selectedAttachmentUris.clear();
        renderAttachmentPreview();
        saveLocalRecent("image", selectedImageAssistant, prompt);
        for (Uri uri : attachments) {
            String preview = imageSourceForBubble(uri);
            if (!preview.isEmpty()) {
                appendLocalConversation("image", "user", "image", preview);
                content.addView(imageResultBubble(preview, System.currentTimeMillis(), "image", -1, "user"));
            }
        }
        int userIndex = appendLocalConversation("image", "user", "text", prompt);
        content.addView(messageBubble("user", prompt, System.currentTimeMillis(), "image", userIndex));
        LinearLayout progress = addImageProgressBubble();
        localAutoScrollEnabled = true;
        scrollToBottom();
        runNetwork(() -> {
            JSONObject response;
            if (!attachments.isEmpty()) {
                response = apiImageEdit(attachments.get(0), imageAssistantPrompt(selectedImageAssistant, prompt));
            } else {
                JSONObject body = new JSONObject();
                body.put("model", "gpt-image-2");
                body.put("prompt", imageAssistantPrompt(selectedImageAssistant, prompt));
                body.put("n", 1);
                body.put("size", selectedImageSize);
                body.put("quality", imageQualityValue(selectedImageQuality));
                body.put("response_format", "b64_json");
                if (imageRandomSeed) {
                    body.put("seed", System.currentTimeMillis() % 1000000);
                }
                response = api("POST", "/pg/images/generations", body);
            }
            String text = extractImageText(response);
            ui(() -> {
                setSending(false);
                String result = text.isEmpty() ? "模型没有返回可展示的图片。" : text;
                int assistantIndex = appendLocalConversation("image", "assistant", result.startsWith("http://") || result.startsWith("https://") || result.startsWith("data:image/") ? "image" : "text", result);
                renderImageResult(progress, result, System.currentTimeMillis(), "image", assistantIndex);
                scrollToBottomIfLocalAuto();
            });
        });
    }

    private String resolveChatModel(boolean hasImageAttachment) {
        if (hasImageAttachment) {
            String selected = selectedChatModel == null ? "" : selectedChatModel.trim();
            if (isVisionChatModel(selected)) {
                return selected;
            }
            for (String model : chatModels) {
                if (isVisionChatModel(model)) {
                    return model;
                }
            }
            return "gpt-5.4";
        }
        if (selectedChatModel != null && !selectedChatModel.isEmpty()) {
            return selectedChatModel;
        }
        return codexModels.isEmpty() ? "gpt-5.4" : codexModels.get(0);
    }

    private boolean isVisionChatModel(String model) {
        String n = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return n.startsWith("gpt") || n.startsWith("gemini") || n.startsWith("claude");
    }

    private String imageQualityValue(String label) {
        if (label.contains("高清")) return "high";
        return "medium";
    }

    private String uriToDataUrl(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                return "";
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String mime = getContentResolver().getType(uri);
            if (mime == null || mime.trim().isEmpty()) {
                mime = "image/png";
            }
            return "data:" + mime + ";base64," + android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractChatText(JSONObject response) {
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            JSONObject data = response.optJSONObject("data");
            return data == null ? response.optString("message", "") : extractChatText(data);
        }
        JSONObject choice = choices.optJSONObject(0);
        JSONObject message = choice == null ? null : choice.optJSONObject("message");
        return message == null ? "" : cleanJsonString(message, "content");
    }

    private String extractImageText(JSONObject response) {
        String image = findImageSource(response);
        if (!image.isEmpty()) {
            return image;
        }
        return "";
    }

    private String findImageSource(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "";
        }
        if (value instanceof String) {
            String text = cleanDisplayText((String) value);
            if (looksLikeImageSource(text)) {
                return normalizeImageSource(text);
            }
            return "";
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                String found = findImageSource(array.opt(i));
                if (!found.isEmpty()) {
                    return found;
                }
            }
            return "";
        }
        if (!(value instanceof JSONObject)) {
            return "";
        }
        JSONObject object = (JSONObject) value;
        for (String key : new String[]{"url", "image_url", "imageUrl"}) {
            String source = cleanJsonString(object, key);
            if (!source.isEmpty()) {
                return normalizeImageSource(source);
            }
        }
        for (String key : new String[]{"b64_json", "b64Json", "image_base64", "binary_data_base64", "base64"}) {
            String source = cleanJsonString(object, key);
            if (!source.isEmpty()) {
                return normalizeImageSource(source);
            }
        }
        String type = cleanJsonString(object, "type");
        String result = firstNonEmptyJsonString(object, "result", "image", "output_image");
        if (!result.isEmpty() && (type.equals("image_generation_call") || type.equals("output_image") || type.equals("image"))) {
            return normalizeImageSource(result);
        }
        for (String key : new String[]{"data", "output", "images", "result", "image_urls", "image_base64", "binary_data_base64"}) {
            String found = findImageSource(object.opt(key));
            if (!found.isEmpty()) {
                return found;
            }
        }
        return "";
    }

    private String firstNonEmptyJsonString(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = cleanJsonString(object, key);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private boolean looksLikeImageSource(String value) {
        String text = value == null ? "" : value.trim();
        return text.startsWith("http://")
                || text.startsWith("https://")
                || text.startsWith("data:image/")
                || text.length() > 120 && text.matches("^[A-Za-z0-9+/=\\r\\n]+$");
    }

    private String normalizeImageSource(String source) {
        String value = cleanDisplayText(source).replace("\n", "").replace("\r", "");
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/")) {
            return value;
        }
        return "data:image/png;base64," + value;
    }

    private String streamChatResponse(JSONObject body, LinearLayout live, long timestamp) throws Exception {
        String base = prefs.getString("server", "").replaceAll("/+$", "");
        HttpURLConnection conn = (HttpURLConnection) new URL(base + "/pg/chat/completions").openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(180000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "text/event-stream, application/json");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String cookie = prefs.getString("cookie", "");
        if (!cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        String userId = prefs.getString("user_id", "");
        if (!userId.isEmpty()) {
            conn.setRequestProperty("New-Api-User", userId);
        }
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code >= 400) {
            String raw = readStream(conn.getErrorStream());
            conn.disconnect();
            throw new ApiException(code, raw, "聊天服务暂时不可用，请稍后重试。");
        }
        String contentType = conn.getContentType() == null ? "" : conn.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.contains("event-stream")) {
            String raw = readStream(conn.getInputStream());
            conn.disconnect();
            String text = extractChatText(new JSONObject(raw.isEmpty() ? "{}" : raw));
            ui(() -> updateStreamingBubble(live, text, timestamp, false));
            return text;
        }
        StringBuilder answer = new StringBuilder();
        StringBuilder thinking = new StringBuilder();
        long[] lastLiveUpdateAt = new long[]{0L};
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                try {
                    JSONObject json = new JSONObject(data);
                    JSONArray choices = json.optJSONArray("choices");
                    JSONObject choice = choices == null || choices.length() == 0 ? null : choices.optJSONObject(0);
                    JSONObject delta = choice == null ? null : choice.optJSONObject("delta");
                    if (delta == null) {
                        continue;
                    }
                    String reasoning = rawJsonString(delta, "reasoning_content");
                    if (reasoning.length() == 0) {
                        reasoning = rawJsonString(delta, "reasoning");
                    }
                    String content = rawJsonString(delta, "content");
                    if (reasoning.length() > 0) {
                        thinking.append(reasoning);
                    }
                    if (content.length() > 0) {
                        answer.append(content);
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastLiveUpdateAt[0] >= 90L) {
                        lastLiveUpdateAt[0] = now;
                        String rendered = renderLiveChatText(thinking.toString(), answer.toString());
                        ui(() -> {
                            updateStreamingBubble(live, rendered, timestamp, false);
                            scrollToBottomIfLocalAuto();
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        } finally {
            conn.disconnect();
        }
        String finalText = renderLiveChatText(thinking.toString(), answer.toString());
        ui(() -> updateStreamingBubble(live, finalText, timestamp, false));
        return finalText;
    }

    private String renderLiveChatText(String thinking, String answer) {
        StringBuilder out = new StringBuilder();
        String[] normalized = normalizeThinkingAndAnswer(thinking, answer);
        if (!normalized[0].trim().isEmpty()) {
            out.append("思考过程\n").append(normalized[0].trim()).append("\n\n");
        }
        out.append(normalized[1]);
        return out.toString();
    }

    private String[] normalizeThinkingAndAnswer(String thinking, String answer) {
        String resolvedThinking = cleanDisplayText(thinking);
        String resolvedAnswer = cleanDisplayText(answer);
        String[] extracted = extractTaggedThinking(resolvedAnswer, "think");
        if (extracted[0].isEmpty()) {
            extracted = extractTaggedThinking(resolvedAnswer, "thinking");
        }
        if (!extracted[0].isEmpty()) {
            resolvedThinking = joinNonEmpty(resolvedThinking, extracted[0]);
            resolvedAnswer = extracted[1];
        }
        if (resolvedAnswer.startsWith("Thinking\n")) {
            int split = resolvedAnswer.indexOf("\n\n");
            if (split > 0) {
                resolvedThinking = joinNonEmpty(resolvedThinking, resolvedAnswer.substring("Thinking\n".length(), split));
                resolvedAnswer = resolvedAnswer.substring(split + 2).trim();
            } else {
                resolvedAnswer = resolvedAnswer.substring("Thinking\n".length()).trim();
            }
        }
        return new String[]{resolvedThinking, resolvedAnswer};
    }

    private String[] extractTaggedThinking(String value, String tag) {
        String text = value == null ? "" : value;
        String lower = text.toLowerCase(Locale.ROOT);
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        StringBuilder thinking = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        int cursor = 0;
        while (true) {
            int start = lower.indexOf(open, cursor);
            if (start < 0) {
                answer.append(text.substring(cursor));
                break;
            }
            answer.append(text, cursor, start);
            int contentStart = start + open.length();
            int end = lower.indexOf(close, contentStart);
            if (end < 0) {
                thinking.append(text.substring(contentStart));
                cursor = text.length();
                break;
            }
            thinking.append(text, contentStart, end).append('\n');
            cursor = end + close.length();
        }
        return new String[]{cleanDisplayText(thinking.toString()), cleanDisplayText(answer.toString())};
    }

    private String joinNonEmpty(String first, String second) {
        String left = cleanDisplayText(first);
        String right = cleanDisplayText(second);
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        return left + "\n" + right;
    }

    private String cleanJsonString(JSONObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.isNull(key)) {
            return "";
        }
        return cleanDisplayText(jsonValueToText(object.opt(key)));
    }

    private String rawJsonString(JSONObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.isNull(key)) {
            return "";
        }
        return jsonValueToText(object.opt(key)).replace("\u0000", "");
    }

    private String jsonValueToText(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                String part = jsonValueToText(array.opt(i));
                if (!part.trim().isEmpty()) {
                    if (out.length() > 0) out.append('\n');
                    out.append(part);
                }
            }
            return out.toString();
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (String key : new String[]{"text", "content", "summary", "value"}) {
                String part = jsonValueToText(object.opt(key));
                if (!part.trim().isEmpty()) {
                    return part;
                }
            }
            return "";
        }
        return String.valueOf(value);
    }

    private String cleanDisplayText(String text) {
        if (text == null) {
            return "";
        }
        String value = text.replace("\u0000", "").trim();
        while (value.endsWith("null")) {
            String next = value.substring(0, value.length() - 4).trim();
            if (next.equals(value)) {
                break;
            }
            value = next;
        }
        return "null".equalsIgnoreCase(value) ? "" : value;
    }

    private void refreshSubscriptions() {
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/subscription/plans", null);
            JSONArray data = envelope.optJSONArray("data");
            if (data == null) {
                data = new JSONArray();
            }
            JSONArray plans = data;
            ui(() -> {
                View maybe = content == null ? null : content.findViewWithTag("subscription_list");
                if (maybe instanceof LinearLayout) {
                    renderSubscriptionList((LinearLayout) maybe, plans);
                }
            });
        });
    }

    private void renderSubscriptionList(LinearLayout list, JSONArray records) {
        list.removeAllViews();
        if (records.length() == 0) {
            list.addView(card("暂无可订阅套餐", "服务器当前没有开启可订阅套餐。"));
            return;
        }
        Set<String> recommendedPlans = recommendedPlanKeys(records);
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            JSONObject plan = record == null ? null : record.optJSONObject("plan");
            if (plan == null || !plan.optBoolean("enabled", true)) {
                continue;
            }
            if (isTrialPlan(plan) && hasPurchasedPlan(record, plan)) {
                continue;
            }
            double priceAmount = plan.optDouble("price_amount", 0);
            String title = plan.optString("title", "套餐");
            String subtitle = plan.optString("subtitle", "适合稳定使用。");
            String price = formatPlainPrice(priceAmount);
            String quota = formatQuotaAsUsd(plan.optDouble("total_amount", 0), 500000);
            String validity = formatDuration(plan);
            String reset = resetLabel(plan.optString("quota_reset_period", "never"));
            boolean trialPlan = isTrialPlan(plan);
            boolean recommended = recommendedPlans.contains(planKey(plan));
            FrameLayout frame = subscriptionCardFrame();
            if (trialPlan && Math.abs(priceAmount - 50D) < 0.01D) {
                ImageView gift = new ImageView(this);
                gift.setImageResource(R.drawable.plan_gift);
                gift.setAlpha(0.5f);
                gift.setScaleType(ImageView.ScaleType.FIT_CENTER);
                FrameLayout.LayoutParams giftLp = new FrameLayout.LayoutParams(dp(96), dp(96), Gravity.CENTER);
                frame.addView(gift, giftLp);
                frame.post(() -> {
                    int side = Math.max(dp(72), frame.getHeight() / 2);
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(side, side, Gravity.CENTER);
                    gift.setLayoutParams(lp);
                });
            }
            LinearLayout panel = vertical();
            panel.setPadding(dp(16), dp(15), dp(16), dp(15));
            LinearLayout head = horizontal();
            head.addView(bold(title), new LinearLayout.LayoutParams(0, -2, 1));
            TextView badge = planBadge(plan);
            head.addView(badge, new LinearLayout.LayoutParams(dp(74), dp(28)));
            panel.addView(head);
            panel.addView(copy(subtitle));
            panel.addView(copy("总额度 " + quota + " · 有效期 " + validity + " · " + reset));
            LinearLayout foot = horizontal();
            foot.setGravity(Gravity.CENTER_VERTICAL);
            TextView priceView = bold(price + " 元");
            priceView.setTextColor(BLUE);
            foot.addView(priceView, new LinearLayout.LayoutParams(0, -2, 1));
            if (recommended) {
                ImageView sale = new ImageView(this);
                sale.setImageResource(R.drawable.plan_sale);
                sale.setAlpha(0.82f);
                sale.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                LinearLayout.LayoutParams saleLp = new LinearLayout.LayoutParams(dp(17), dp(17));
                saleLp.setMargins(0, 0, dp(6), 0);
                foot.addView(sale, saleLp);
            }
            JSONObject selectedPlan = plan;
            ImageButton buy = planBuyButton(() -> showSubscriptionPayDialog(selectedPlan));
            foot.addView(buy, new LinearLayout.LayoutParams(dp(62), dp(62)));
            LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2);
            footLp.setMargins(0, dp(6), 0, 0);
            panel.addView(foot, footLp);
            frame.addView(panel, new FrameLayout.LayoutParams(-1, -2));
            list.addView(frame);
        }
        if (list.getChildCount() == 0) {
            list.addView(card("暂无可订阅套餐", "服务器当前没有开启可订阅套餐。"));
        }
    }

    private FrameLayout subscriptionCardFrame() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(round(GLASS, dp(18), LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(16));
        frame.setLayoutParams(lp);
        return frame;
    }

    private ImageButton planBuyButton(Runnable action) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(R.drawable.ic_bubble_buy);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        b.setBackground(round(Color.rgb(255, 255, 255), dp(18), LINE));
        b.setContentDescription("订阅套餐");
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private Set<String> recommendedPlanKeys(JSONArray records) {
        Set<String> keys = new HashSet<>();
        double monthMax = -1;
        double yearMax = -1;
        String monthKey = "";
        String yearKey = "";
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            JSONObject plan = record == null ? null : record.optJSONObject("plan");
            if (plan == null || !plan.optBoolean("enabled", true) || isTrialPlan(plan)) {
                continue;
            }
            double price = plan.optDouble("price_amount", 0);
            String unit = plan.optString("duration_unit", "month");
            int value = Math.max(1, plan.optInt("duration_value", 1));
            if ("year".equals(unit) || value >= 12) {
                if (price > yearMax) {
                    yearMax = price;
                    yearKey = planKey(plan);
                }
            } else if ("month".equals(unit)) {
                if (price > monthMax) {
                    monthMax = price;
                    monthKey = planKey(plan);
                }
            }
        }
        if (!monthKey.isEmpty()) keys.add(monthKey);
        if (!yearKey.isEmpty()) keys.add(yearKey);
        return keys;
    }

    private String planKey(JSONObject plan) {
        if (plan == null) {
            return "";
        }
        int id = plan.optInt("id", 0);
        if (id != 0) {
            return "id:" + id;
        }
        return plan.optString("title", "") + "|" + plan.optString("duration_unit", "") + "|" + plan.optInt("duration_value", 0);
    }

    private boolean isTrialPlan(JSONObject plan) {
        String text = (plan.optString("title") + " " + plan.optString("subtitle") + " " + plan.optString("name")).toLowerCase(Locale.ROOT);
        return text.contains("尝鲜") || text.contains("trial") || text.contains("体验");
    }

    private boolean hasPurchasedPlan(JSONObject record, JSONObject plan) {
        if (record == null || plan == null) {
            return false;
        }
        return record.optBoolean("purchased", false)
                || record.optBoolean("subscribed", false)
                || record.optBoolean("owned", false)
                || record.optInt("purchase_count", 0) > 0
                || record.optInt("buy_count", 0) > 0
                || plan.optBoolean("purchased", false)
                || plan.optInt("purchase_count", 0) > 0;
    }

    private TextView planBadge(JSONObject plan) {
        String label;
        int color;
        String unit = plan.optString("duration_unit", "month");
        int value = Math.max(1, plan.optInt("duration_value", 1));
        if ("year".equals(unit) || value >= 12) {
            label = "年付";
            color = Color.argb(54, 245, 158, 11);
        } else if ("month".equals(unit)) {
            label = "月付";
            color = Color.argb(54, 54, 104, 240);
        } else {
            label = formatDuration(plan);
            color = Color.argb(54, 210, 132, 40);
        }
        TextView badge = copy(label);
        badge.setTextColor(Color.rgb(66, 82, 108));
        badge.setTextSize(12);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(color, dp(14), Color.argb(74, 145, 160, 184)));
        return badge;
    }

    private void showSubscriptionPayDialog(JSONObject plan) {
        if (plan == null) {
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(104, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.addView(bold("钱包支付"));
        panel.addView(copy(plan.optString("title", "套餐") + " · " + formatPlainPrice(plan.optDouble("price_amount", 0)) + " 元"));
        panel.addView(copy("确认后将从钱包余额扣除并购买该套餐。"));
        LinearLayout actions = horizontal();
        actions.setPadding(0, dp(14), 0, 0);
        Button cancel = ghost("取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        Button confirm = primary("确认支付");
        confirm.setOnClickListener(v -> {
            confirm.setEnabled(false);
            confirm.setText("支付中");
            executor.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("plan_id", plan.optInt("id", 0));
                    JSONObject res = api("POST", "/api/subscription/wallet/pay", body);
                    JSONObject data = res.optJSONObject("data");
                    String notice = data == null ? "" : data.optString("notice", "");
                    ui(() -> {
                        dialog.dismiss();
                        toast(notice.isEmpty() ? "套餐购买成功" : notice);
                        refreshSubscriptions();
                        refreshWallet();
                    });
                } catch (Exception e) {
                    ui(() -> {
                        confirm.setEnabled(true);
                        confirm.setText("确认支付");
                        toast(userMessage(e));
                    });
                }
            });
        });
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(42), 1);
        cancelLp.setMargins(0, 0, dp(8), 0);
        LinearLayout.LayoutParams confirmLp = new LinearLayout.LayoutParams(0, dp(42), 1);
        confirmLp.setMargins(dp(8), 0, 0, 0);
        actions.addView(cancel, cancelLp);
        actions.addView(confirm, confirmLp);
        panel.addView(actions);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(312), -2, Gravity.CENTER);
        lp.setMargins(dp(18), 0, dp(18), 0);
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void refreshWallet() {
        runNetworkQuiet(() -> {
            JSONObject profile = api("GET", "/api/user/self", null).optJSONObject("data");
            JSONObject billing = api("GET", "/api/user/topup/self?p=1&page_size=3", null).optJSONObject("data");
            JSONObject usage = api("GET", "/api/log/self?p=1&page_size=50", null).optJSONObject("data");
            Object plansData = null;
            JSONObject subscriptions = null;
            try {
                plansData = api("GET", "/api/subscription/plans", null).opt("data");
            } catch (Exception ignored) {
            }
            try {
                subscriptions = api("GET", "/api/subscription/self", null).optJSONObject("data");
            } catch (Exception ignored) {
            }
            JSONArray plans = plansData instanceof JSONArray ? (JSONArray) plansData : new JSONArray();
            JSONObject finalSubscriptions = subscriptions == null ? new JSONObject() : subscriptions;
            ui(() -> {
                View maybe = content == null ? null : content.findViewWithTag("wallet_list");
                if (maybe instanceof LinearLayout) {
                    renderWalletList((LinearLayout) maybe, profile == null ? new JSONObject() : profile, billing == null ? new JSONObject() : billing, usage == null ? new JSONObject() : usage, plans, finalSubscriptions);
                }
            });
        });
    }

    private void renderWalletList(LinearLayout list, JSONObject profile, JSONObject billing, JSONObject usage, JSONArray plans, JSONObject subscriptions) {
        list.removeAllViews();
        LinearLayout overview = cardPanel();
        overview.addView(bold("钱包总览"));
        overview.addView(copy("剩余额度 " + formatQuotaAsUsd(profile.optDouble("quota", 0), 500000)));
        overview.addView(copy("已用额度 " + formatQuotaAsUsd(profile.optDouble("used_quota", 0), 500000) + " · 请求数 " + profile.optLong("request_count", 0)));
        list.addView(overview);

        LinearLayout distribution = cardPanel();
        distribution.addView(bold("消耗分布"));
        renderUsageDistribution(distribution, usage.optJSONArray("items"));

        LinearLayout bills = cardPanel();
        bills.addView(bold("最近账单"));
        JSONArray items = billing.optJSONArray("items");
        if (items == null || items.length() == 0) {
            bills.addView(copy("暂无最近账单。"));
        } else {
            for (int i = 0; i < Math.min(items.length(), 3); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                double amount = Math.abs(item.optDouble("amount", item.optDouble("money", 0)));
                bills.addView(billingRow(formatBillingLabel(item) + " · " + formatPlainPrice(item.optDouble("money", amount)) + " 元", resolveBillingUsageRatio(item, plans, subscriptions)));
            }
        }
        list.addView(bills);
        list.addView(distribution);
    }

    private void refreshServiceStatus() {
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/service-status", null);
            JSONObject data = envelope.optJSONObject("data");
            JSONArray items = data == null ? new JSONArray() : data.optJSONArray("items");
            long refreshedAt = data == null ? 0 : data.optLong("refreshedAt", 0);
            JSONArray finalItems = items == null ? new JSONArray() : items;
            ui(() -> {
                View maybe = content == null ? null : content.findViewWithTag("service_status_list");
                if (maybe instanceof LinearLayout) {
                    renderServiceStatusList((LinearLayout) maybe, finalItems, refreshedAt);
                }
            });
        });
    }

    private void renderServiceStatusList(LinearLayout list, JSONArray items, long refreshedAt) {
        list.removeAllViews();
        LinearLayout header = cardPanel();
        header.addView(bold("渠道运行状态"));
        header.addView(copy(refreshedAt > 0 ? "最后更新：" + formatDateTime(refreshedAt) : "最近状态变化"));
        list.addView(header);
        if (items.length() == 0) {
            list.addView(card("当前没有可展示的服务状态", "服务器尚未配置 Claude、Codex、Gemini、DeepSeek 或 XiaomiMIMO 渠道。"));
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            LinearLayout row = cardPanel();
            LinearLayout head = horizontal();
            head.addView(bold(item.optString("title", "服务")), new LinearLayout.LayoutParams(0, -2, 1));
            TextView status = copy(serviceToneLabel(item.optString("tone")));
            status.setTextColor(serviceToneColor(item.optString("tone")));
            status.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            head.addView(status, new LinearLayout.LayoutParams(dp(96), -2));
            row.addView(head);
            String subtitle = item.optString("subtitle", "");
            if (!subtitle.isEmpty()) {
                row.addView(copy(subtitle));
            }
            JSONArray history = item.optJSONArray("history");
            if (history != null && history.length() > 0) {
                row.addView(serviceHistoryDots(history));
            }
            String meta = "";
            if (item.optLong("latencyMs", 0) > 0) {
                meta += "延迟 " + item.optLong("latencyMs") + " ms";
            }
            if (item.optLong("checkedAt", 0) > 0) {
                meta += (meta.isEmpty() ? "" : " · ") + "检测时间 " + formatDateTime(item.optLong("checkedAt"));
            }
            if (!meta.isEmpty()) {
                row.addView(copy(meta));
            }
            String detail = item.optString("detail", "");
            if (!detail.isEmpty()) {
                row.addView(copy(detail));
            }
            list.addView(row);
        }
    }

    private void refreshModels() {
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/pricing", null);
            JSONArray data = envelope.optJSONArray("data");
            if (data == null) {
                return;
            }
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                String model = item == null ? "" : item.optString("model_name", item.optString("model", ""));
                addModelIfCompatible(model);
            }
            ui(() -> {
                if ("codex".equals(section) || "claude".equals(section)) {
                    renderSection();
                }
            });
        });
    }

    private void addModelIfCompatible(String model) {
        String m = model == null ? "" : model.trim();
        String n = m.toLowerCase(Locale.ROOT);
        if (m.isEmpty()) {
            return;
        }
        if ((n.startsWith("gpt-") || n.contains("codex") || n.equals("deepseek-v4-flash") || n.equals("deepseek-v4-pro") || n.equals("mimo-v2.5") || n.equals("mimo-v2.5-pro")) && !codexModels.contains(m)) {
            codexModels.add(m);
        }
        if ((n.startsWith("claude") || n.equals("deepseek-v4-flash") || n.equals("deepseek-v4-pro") || n.equals("mimo-v2.5-pro")) && !claudeModels.contains(m)) {
            claudeModels.add(m);
        }
        if (isChatModelName(m) && !chatModels.contains(m)) {
            chatModels.add(m);
        }
    }

    private boolean isChatModelName(String model) {
        String n = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return !n.isEmpty() && !n.contains("image") && !n.contains("midjourney") && !n.contains("dall") && !n.contains("stable");
    }

    private void refreshAssistants(boolean showToast) {
        runNetworkQuiet(() -> {
            String query = boundDeviceId.isEmpty() ? "" : "?device_id=" + enc(boundDeviceId);
            JSONObject envelope = api("GET", "/api/mobile/desktop-assistants" + query, null);
            JSONArray data = envelope.optJSONArray("data");
            if (data == null) {
                data = new JSONArray();
            }
            List<JSONObject> chat = new ArrayList<>();
            List<JSONObject> image = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String scope = item.optString("scope", "chat");
                if ("draw".equals(scope) || "image".equals(scope)) {
                    image.add(item);
                } else {
                    chat.add(item);
                }
            }
            ui(() -> {
                chatAssistants.clear();
                chatAssistants.addAll(chat);
                imageAssistants.clear();
                imageAssistants.addAll(image);
            });
        });
    }

    private void refreshDevices(boolean showToast) {
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/mobile/desktop-devices", null);
            JSONArray devices = envelope.optJSONArray("data");
            if (devices == null) {
                devices = new JSONArray();
            }
            devices = activeDevices(devices);
            String stored = prefs.getString("bound_device_id", "");
            boolean storedExists = stored.isEmpty();
            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.optJSONObject(i);
                if (device != null && stored.equals(device.optString("deviceId"))) {
                    storedExists = true;
                }
                if (device != null && device.optBoolean("bound")) {
                    stored = device.optString("deviceId");
                    storedExists = true;
                    break;
                }
            }
            if (!stored.isEmpty() && storedExists) {
                boundDeviceId = stored;
                prefs.edit().putString("bound_device_id", stored).apply();
            } else if (!stored.isEmpty()) {
                boundDeviceId = "";
                prefs.edit().remove("bound_device_id").apply();
            }
            JSONArray finalDevices = devices;
            ui(() -> {
                if (statusText != null) {
                    statusText.setText(boundDeviceId.isEmpty() ? "未绑定设备" : "绑定设备 " + shortId(boundDeviceId));
                }
                View maybe = content == null ? null : content.findViewWithTag("device_list");
                if (maybe instanceof LinearLayout) {
                    renderDeviceList((LinearLayout) maybe, finalDevices);
                }
            });
        });
    }

    private void renderDeviceList(LinearLayout list, JSONArray devices) {
        list.removeAllViews();
        if (devices.length() == 0) {
            LinearLayout empty = cardPanel();
            LinearLayout head = horizontal();
            head.addView(bold("暂无客户端"), new LinearLayout.LayoutParams(0, -2, 1));
            Button refresh = iconButton("↻");
            refresh.setContentDescription("刷新设备");
            refresh.setOnClickListener(v -> refreshDevices(true));
            head.addView(refresh, new LinearLayout.LayoutParams(dp(32), dp(32)));
            empty.addView(head);
            empty.addView(copy("请打开 PC/Mac 客户端并登录同一账号，客户端在线后会自动出现在这里。"));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < devices.length(); i++) {
            JSONObject d = devices.optJSONObject(i);
            if (d == null) {
                continue;
            }
            String deviceId = d.optString("deviceId");
            boolean current = !boundDeviceId.isEmpty() && deviceId.equals(boundDeviceId);
            LinearLayout row = cardPanel();
            if (!current) {
                row.setBackground(round(Color.argb(154, 238, 241, 246), dp(18), Color.argb(92, 158, 169, 188)));
            }
            LinearLayout head = horizontal();
            head.addView(bold(d.optString("name", "桌面客户端")), new LinearLayout.LayoutParams(0, -2, 1));
            if (i == 0) {
                Button refresh = iconButton("↻");
                refresh.setContentDescription("刷新设备");
                refresh.setOnClickListener(v -> refreshDevices(true));
                head.addView(refresh, new LinearLayout.LayoutParams(dp(32), dp(32)));
            }
            row.addView(head);
            row.addView(copy(d.optString("platform", "desktop") + " · " + d.optString("status", "online") + " · " + shortId(deviceId)));
            LinearLayout actions = horizontal();
            actions.setPadding(0, dp(14), 0, dp(8));
            if (current) {
                Button bound = primary("已绑定");
                bound.setEnabled(false);
                Button unbind = ghost("解除");
                unbind.setOnClickListener(v -> unbindDevice(deviceId));
                LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1);
                a.setMargins(0, 0, dp(8), 0);
                LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(44), 1);
                b.setMargins(dp(8), 0, 0, 0);
                actions.addView(bound, a);
                actions.addView(unbind, b);
            } else {
                Button bind = primary("绑定/切换");
                bind.setOnClickListener(v -> bindDevice(deviceId));
                actions.addView(bind, new LinearLayout.LayoutParams(-1, dp(44)));
            }
            row.addView(actions);
            list.addView(row);
        }
    }

    private JSONArray activeDevices(JSONArray devices) throws Exception {
        JSONArray out = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < devices.length(); i++) {
            JSONObject device = devices.optJSONObject(i);
            if (device == null) {
                continue;
            }
            boolean bound = device.optBoolean("bound");
            long lastSeen = device.optLong("lastSeenAt", 0);
            boolean fresh = lastSeen > 0 && now - lastSeen < 15 * 60 * 1000L;
            if (fresh) {
                out.put(device);
            }
        }
        return out;
    }

    private void bindDevice(String deviceId) {
        runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("appId", prefs.getString("app_id", ""));
            body.put("appName", "OneAPI Android");
            body.put("deviceId", deviceId);
            try {
                api("POST", "/api/mobile/desktop-bindings", body);
            } catch (ApiException error) {
                if (!isBindingEndpointMissing(error)) {
                    throw error;
                }
            }
            boundDeviceId = deviceId;
            prefs.edit().putString("bound_device_id", deviceId).apply();
            ui(() -> {
                refreshDevices(false);
                renderSection();
            });
        });
    }

    private boolean isBindingEndpointMissing(ApiException error) {
        return error.status == 404 || error.raw.toLowerCase(Locale.ROOT).contains("404 page not found");
    }

    private void unbindDevice(String deviceId) {
        runNetwork(() -> {
            try {
                api("DELETE", "/api/mobile/desktop-bindings/" + enc(deviceId) + "?appId=" + enc(prefs.getString("app_id", "")), null);
            } catch (ApiException error) {
                if (!isBindingEndpointMissing(error)) {
                    throw error;
                }
            }
            if (deviceId.equals(boundDeviceId)) {
                boundDeviceId = "";
                prefs.edit().remove("bound_device_id").apply();
            }
            ui(() -> {
                refreshDevices(false);
                renderSection();
            });
        });
    }

    private void sendCliJob(String client) {
        if (boundDeviceId.isEmpty()) {
            toast("请先在我的页面绑定 PC/Mac 客户端");
            return;
        }
        String prompt = trim(activeInput);
        if (prompt.isEmpty()) {
            toast("请输入要执行的任务");
            return;
        }
        hideKeyboard(activeInput);
        setSending(true);
        activeInput.setText("");
        runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("client", client);
            body.put("deviceId", boundDeviceId);
            body.put("sessionId", resolveCliSessionIdForJob(client));
            body.put("prompt", prompt);
            body.put("model", selectedCliModelFor(client));
            body.put("reasoningEffort", reasoningValue(selectedReasoning));
            body.put("permissionMode", selectedPermission.contains("全权限") ? "full" : "restricted");
            JSONArray extensionRefs = new JSONArray();
            for (JSONObject ref : selectedExtensionRefs) {
                extensionRefs.put(new JSONObject()
                        .put("id", ref.optString("id", ref.optString("name")))
                        .put("kind", ref.optString("kind", "skill"))
                        .put("name", ref.optString("name")));
            }
            body.put("extensionRefs", extensionRefs);
            api("POST", "/api/mobile/desktop-jobs", body);
            ui(() -> {
                setSending(false);
                pollSessionsOnce();
            });
        });
    }

    private String resolveCliSessionIdForJob(String client) {
        String selected = selectedCliSessionIds.get(client);
        if (selected == null || selected.trim().isEmpty()) {
            selected = prefs.getString("cli_active_session_id_" + client, "");
        }
        selected = selected == null ? "" : selected.trim();
        if (!selected.isEmpty()) {
            return selected;
        }
        try {
            JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
            JSONArray sessions = envelope.optJSONArray("data");
            if (sessions != null) {
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject item = sessions.optJSONObject(i);
                    if (item == null || !client.equals(item.optString("client"))) {
                        continue;
                    }
                    String id = item.optString("sessionId", item.optString("id", "")).trim();
                    if (!id.isEmpty() && !id.endsWith("-remote")) {
                        selectedCliSessionIds.put(client, id);
                        prefs.edit().putString("cli_active_session_id_" + client, id).apply();
                        updateSelectedCliProject(client, item);
                        return id;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return sessionId;
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!polling) {
                    return;
                }
                if (isCliSection() && shouldAutoRefreshCliSection(section)) {
                    pollSessionsOnce(false);
                    handler.postDelayed(this, 3000);
                } else {
                    handler.postDelayed(this, 12000);
                }
            }
        }, 800);
    }

    private boolean shouldAutoRefreshCliSection(String client) {
        if (!"codex".equals(client) && !"claude".equals(client)) {
            return false;
        }
        if (requestRunning) {
            return true;
        }
        for (int i = 0; i < cachedDesktopSessions.length(); i++) {
            JSONObject session = cachedDesktopSessions.optJSONObject(i);
            if (session == null || !client.equals(session.optString("client"))) {
                continue;
            }
            String status = session.optString("status", "").toLowerCase(Locale.ROOT);
            if ("queued".equals(status) || "claimed".equals(status) || "running".equals(status) || "waiting_interaction".equals(status)) {
                return true;
            }
        }
        return false;
    }

    private void loadDesktopSessionCache() {
        try {
            cachedDesktopSessions = new JSONArray(prefs.getString("desktop_sessions_cache", "[]"));
            hydrateCliTimelineCache(cachedDesktopSessions);
        } catch (Exception ignored) {
            cachedDesktopSessions = new JSONArray();
        }
    }

    private void refreshDesktopSessionsCache(boolean force) {
        if (desktopSessionsRefreshRunning && !force) {
            return;
        }
        desktopSessionsRefreshRunning = true;
        runNetworkQuiet(() -> {
            try {
                JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
                JSONArray sessions = envelope.optJSONArray("data");
                if (sessions == null) {
                    sessions = new JSONArray();
                }
                JSONArray finalSessions = sessions;
                prefs.edit().putString("desktop_sessions_cache", finalSessions.toString()).apply();
                ui(() -> {
                    cachedDesktopSessions = finalSessions;
                    updateCliTimelineCaches(finalSessions, isCliSection());
                });
            } finally {
                desktopSessionsRefreshRunning = false;
            }
        });
    }

    private void hydrateCliTimelineCache(JSONArray sessions) {
        updateCliTimelineCaches(sessions, false);
    }

    private void updateCliTimelineCaches(JSONArray sessions, boolean renderActiveChange) {
        for (String client : new String[]{"codex", "claude"}) {
            try {
                JSONArray events = mergedEventsForClient(sessions == null ? new JSONArray() : sessions, client);
                String signature = events.toString();
                String previous = cliTimelineSignatures.get(client);
                cliTimelineCache.put(client, events);
                if (signature.equals(previous)) {
                    continue;
                }
                cliTimelineSignatures.put(client, signature);
                if (renderActiveChange && client.equals(section)) {
                    renderTimeline(events, true);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void renderCachedCliTimeline(String client) {
        if (!client.equals(section) || timeline == null) {
            return;
        }
        JSONArray cached = cliTimelineCache.get(client);
        renderTimeline(cached == null ? new JSONArray() : cached, false);
    }

    private void pollSessionsOnce() {
        pollSessionsOnce(false);
    }

    private void pollSessionsOnce(boolean force) {
        if (!"codex".equals(section) && !"claude".equals(section)) {
            return;
        }
        String targetSection = section;
        synchronized (cliRefreshRunningClients) {
            if (cliRefreshRunningClients.contains(targetSection)) {
                return;
            }
            cliRefreshRunningClients.add(targetSection);
        }
        runNetworkQuiet(() -> {
            try {
                JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
                JSONArray sessions = envelope.optJSONArray("data");
                if (sessions == null) {
                    sessions = new JSONArray();
                }
                prefs.edit().putString("desktop_sessions_cache", sessions.toString()).apply();
                JSONArray events = mergedEventsForClient(sessions, targetSection);
                String signature = events.toString();
                JSONArray finalSessions = sessions;
                ui(() -> {
                    cachedDesktopSessions = finalSessions;
                    cliTimelineCache.put(targetSection, events);
                    renderTimelineIfChanged(targetSection, events, signature);
                });
            } finally {
                cliRefreshRunningClients.remove(targetSection);
            }
        });
    }

    private boolean isCliSection() {
        return "codex".equals(section) || "claude".equals(section);
    }

    private int contentBottomScrollY(ScrollView scroll) {
        if (scroll == null || scroll.getChildCount() == 0) {
            return 0;
        }
        View child = scroll.getChildAt(0);
        return Math.max(0, child.getMeasuredHeight() - scroll.getHeight());
    }

    private boolean isContentNearBottom(ScrollView scroll) {
        if (scroll == null) {
            return true;
        }
        return scroll.getScrollY() >= contentBottomScrollY(scroll) - dp(24);
    }

    private void refreshCliAtEdge() {
        long now = System.currentTimeMillis();
        if (now - lastCliRefreshAt < 9000L) {
            return;
        }
        lastCliRefreshAt = now;
        pollSessionsOnce(true);
    }

    private JSONArray mergedEventsForClient(JSONArray sessions, String client) throws Exception {
        List<JSONObject> rows = new ArrayList<>();
        String selectedSessionId = selectedCliSessionIds.get(client);
        selectedSessionId = selectedSessionId == null ? "" : selectedSessionId.trim();
        if (selectedSessionId.startsWith("android-")) {
            selectedSessionId = "";
        }
        JSONObject fallbackSession = null;
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.optJSONObject(i);
            if (session == null || !client.equals(session.optString("client"))) {
                continue;
            }
            String remoteSessionId = session.optString("sessionId", session.optString("id", "")).trim();
            if (fallbackSession == null) {
                fallbackSession = session;
            }
            if (!remoteSessionId.isEmpty() && !remoteSessionId.endsWith("-remote")) {
                String current = selectedCliSessionIds.get(client);
                if (current == null || current.trim().isEmpty() || current.startsWith("android-")) {
                    selectedCliSessionIds.put(client, remoteSessionId);
                    prefs.edit().putString("cli_active_session_id_" + client, remoteSessionId).apply();
                    selectedSessionId = remoteSessionId;
                    updateSelectedCliProject(client, session);
                }
            }
            if (!selectedSessionId.isEmpty() && !selectedSessionId.equals(remoteSessionId) && !selectedSessionId.equals(session.optString("id", ""))) {
                continue;
            }
            updateSelectedCliProject(client, session);
            collectRows(rows, session.optJSONArray("messages"), "message");
            collectPurposes(rows, session);
            collectRows(rows, session.optJSONArray("logs"), "log");
        }
        if (rows.isEmpty() && fallbackSession != null && selectedSessionId.isEmpty()) {
            updateSelectedCliProject(client, fallbackSession);
            collectRows(rows, fallbackSession.optJSONArray("messages"), "message");
            collectPurposes(rows, fallbackSession);
            collectRows(rows, fallbackSession.optJSONArray("logs"), "log");
        }
        rows.sort(Comparator.comparingLong(o -> o.optLong("timestamp")));
        JSONArray out = new JSONArray();
        Set<String> seen = new HashSet<>();
        for (JSONObject row : rows) {
            if (shouldHideLog(row)) {
                continue;
            }
            String key = logDedupeKey(row);
            if (!key.isEmpty() && !seen.add(key)) {
                continue;
            }
            out.put(row);
        }
        return out;
    }

    private void collectRows(List<JSONObject> rows, JSONArray source, String kind) throws Exception {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.length(); i++) {
            JSONObject item = source.optJSONObject(i);
            if (item != null) {
                item.put("_kind", kind);
                rows.add(item);
            }
        }
    }

    private void collectPurposes(List<JSONObject> rows, JSONObject session) throws Exception {
        JSONArray purposes = session == null ? null : session.optJSONArray("purposes");
        if (purposes == null || purposes.length() == 0) {
            return;
        }
        StringBuilder body = new StringBuilder();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < purposes.length(); i++) {
            String item = cleanDisplayText(purposes.optString(i));
            if (item.isEmpty() || !seen.add(item)) {
                continue;
            }
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(item);
        }
        if (body.length() == 0) {
            return;
        }
        long timestamp = System.currentTimeMillis();
        JSONArray messages = session.optJSONArray("messages");
        if (messages != null && messages.length() > 0) {
            JSONObject first = messages.optJSONObject(0);
            if (first != null && first.optLong("timestamp", 0) > 0) {
                timestamp = first.optLong("timestamp") + 1;
            }
        }
        rows.add(new JSONObject()
                .put("_kind", "log")
                .put("id", session.optString("id") + ":purpose")
                .put("type", "intent")
                .put("phase", "intent")
                .put("title", "执行目的")
                .put("body", body.toString())
                .put("timestamp", timestamp));
    }

    private void renderTimeline(JSONArray events) {
        renderTimeline(events, true);
    }

    private void renderTimeline(JSONArray events, boolean scrollLatest) {
        if (timeline == null) {
            return;
        }
        int generation = ++renderGeneration;
        String targetClient = section;
        showPageLoading("正在加载聊天记录...");
        resetContentScrollPosition();
        if (scrollLatest) {
            localAutoScrollEnabled = true;
        }
        clearContainer(timeline);
        if (interactionBar != null) {
            clearContainer(interactionBar);
        }
        if (events == null || events.length() == 0) {
            hidePageLoading();
            timeline.addView(card(label(targetClient), "暂无可显示的会话内容。请先在会话记录中选择其他会话，或从输入框发起新任务。"));
            updateScrollDock();
            return;
        }
        int visibleCount = timelineVisibleItemCounts.getOrDefault(targetClient, INITIAL_RENDER_ITEM_COUNT);
        int startIndex = Math.max(0, events.length() - visibleCount);
        if (startIndex > 0) {
            timeline.addView(loadEarlierButton("加载更早执行记录（剩余 " + startIndex + " 条）", () -> {
                timelineVisibleItemCounts.put(targetClient, visibleCount + RENDER_PAGE_INCREMENT);
                JSONArray cached = cliTimelineCache.get(targetClient);
                renderTimeline(cached == null ? events : cached, false);
            }));
        }
        handler.post(() -> renderTimelineChunk(events, startIndex, generation, scrollLatest, targetClient));
    }

    private void renderTimelineChunk(JSONArray events, int index, int generation, boolean scrollLatest, String targetClient) {
        if (generation != renderGeneration || timeline == null || !targetClient.equals(section)) {
            return;
        }
        int end = Math.min(events.length(), index + RENDER_CHUNK_SIZE);
        for (int i = index; i < end; i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) {
                continue;
            }
            if ("message".equals(e.optString("_kind"))) {
                String text = compactLocalFileReferences(cleanDisplayText(e.optString("text")));
                if (!text.isEmpty()) {
                    long timestamp = e.optLong("timestamp", e.optLong("createdAt", 0));
                    timeline.addView(messageBubble(e.optString("role"), text, timestamp, targetClient + ":plain", -1));
                }
            } else {
                addLogRow(e);
            }
        }
        if (end < events.length()) {
            handler.post(() -> renderTimelineChunk(events, end, generation, scrollLatest, targetClient));
            return;
        }
        hidePageLoading();
        if (scrollLatest) {
            scrollToBottom();
        }
        updateScrollDock();
    }

    private void renderTimelineIfChanged(String targetSection, JSONArray events, String signature) {
        if (!targetSection.equals(section)) {
            return;
        }
        String previous = cliTimelineSignatures.get(targetSection);
        if (signature != null && signature.equals(previous)) {
            return;
        }
        cliTimelineSignatures.put(targetSection, signature == null ? "" : signature);
        renderTimeline(events);
    }

    private void addLogRow(JSONObject e) {
        if (shouldHideLog(e)) {
            return;
        }
        String title = cleanDisplayText(e.optString("title", e.optString("type")));
        String body = cleanDisplayText(e.optString("body"));
        String rawCommand = cleanDisplayText(e.optString("command"));
        String command = compactCommand(rawCommand);
        String preview = logPreview(e, body, rawCommand, command);
        if (title.trim().isEmpty() && body.trim().isEmpty() && command.trim().isEmpty()) {
            return;
        }
        if (title.equals(body) && command.trim().isEmpty()) {
            body = "";
        }
        String phase = e.optString("phase", e.optString("type", ""));
        if (phase.toLowerCase(Locale.ROOT).contains("intent") && !body.trim().isEmpty()) {
            title = body;
            body = "";
        }
        if (!command.trim().isEmpty()) {
            body = "";
            title = commandTitle(title, e.optString("phase", e.optString("type", "")));
        }
        LinearLayout row = cardPanel();
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, dp(8), 0, dp(8));
        row.setLayoutParams(rowLp);
        int level = e.optInt("level", 0);
        int accent = level >= 2 ? Color.rgb(216, 71, 86) : level == 1 ? Color.rgb(210, 132, 40) : phaseColor(phase);
        LinearLayout head = horizontal();
        head.setGravity(Gravity.TOP);
        TextView dot = copy("●");
        dot.setTextColor(accent);
        dot.setTextSize(13);
        dot.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        head.addView(dot, new LinearLayout.LayoutParams(dp(18), -2));
        TextView titleView = bold(title);
        titleView.setTextSize(14);
        titleView.setLineSpacing(dp(2), 1.04f);
        head.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        TextView toggle = copy("▸");
        toggle.setTextColor(accent);
        toggle.setTextSize(14);
        toggle.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        head.addView(toggle, new LinearLayout.LayoutParams(dp(18), -2));
        TextView phaseView = copy(phaseLabel(phase));
        phaseView.setTextColor(accent);
        phaseView.setGravity(Gravity.START | Gravity.TOP);
        head.addView(phaseView, new LinearLayout.LayoutParams(dp(58), -2));
        row.addView(head);
        LinearLayout detailBox = vertical();
        String rowId = logRowId(e);
        boolean initiallyExpanded = expandedLogIds.contains(rowId);
        detailBox.setVisibility(initiallyExpanded ? View.VISIBLE : View.GONE);
        toggle.setText(initiallyExpanded ? "▾" : "▸");
        if (!body.trim().isEmpty() && !"intent".equalsIgnoreCase(phase)) {
            detailBox.addView(copy(body));
        }
        if (!command.trim().isEmpty()) {
            TextView cmd = copy(command);
            cmd.setTextColor(Color.rgb(45, 74, 124));
            cmd.setBackground(new ColorDrawable(Color.TRANSPARENT));
            cmd.setPadding(0, dp(4), 0, dp(4));
            detailBox.addView(cmd);
        }
        if (!preview.trim().isEmpty()) {
            TextView detail = copy(preview);
            detail.setTextColor(Color.rgb(66, 82, 108));
            detail.setBackground(round(Color.argb(116, 255, 255, 255), dp(10), LINE));
            detail.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
            detailLp.setMargins(0, dp(8), 0, 0);
            detailBox.addView(detail, detailLp);
        }
        if (detailBox.getChildCount() > 0) {
            LinearLayout.LayoutParams detailBoxLp = new LinearLayout.LayoutParams(-1, -2);
            detailBoxLp.setMargins(0, dp(8), 0, 0);
            row.addView(detailBox, detailBoxLp);
            View.OnClickListener toggleAction = v -> {
                boolean expanded = detailBox.getVisibility() == View.VISIBLE;
                detailBox.setVisibility(expanded ? View.GONE : View.VISIBLE);
                toggle.setText(expanded ? "▸" : "▾");
                if (expanded) {
                    expandedLogIds.remove(rowId);
                } else {
                    expandedLogIds.add(rowId);
                }
            };
            row.setOnClickListener(toggleAction);
            head.setOnClickListener(toggleAction);
        } else {
            toggle.setVisibility(View.INVISIBLE);
        }
        int indent = Math.max(0, e.optInt("indentLevel", 0));
        if (indent > 0) {
            row.setPadding(dp(16 + indent * 12), dp(15), dp(16), dp(15));
        }
        String interactionId = e.optString("interactionId");
        if (!interactionId.isEmpty() && "pending".equals(e.optString("interactionStatus", "pending"))) {
            renderInteraction(e);
        }
        timeline.addView(row);
    }

    private String logRowId(JSONObject e) {
        String id = e.optString("id", e.optString("eventId", ""));
        if (!id.trim().isEmpty()) {
            return id.trim();
        }
        String key = logDedupeKey(e);
        if (!key.trim().isEmpty()) {
            return key.trim();
        }
        return e.optString("phase", e.optString("type", "log")) + ":" + e.optLong("timestamp", 0);
    }

    private boolean shouldHideLog(JSONObject e) {
        if (e == null || "message".equals(e.optString("_kind"))) {
            return false;
        }
        String all = (e.optString("title") + " " + e.optString("body") + " " + e.optString("command") + " " + e.optString("type") + " " + e.optString("phase"))
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
        return all.contains("扩展上下文准备")
                || all.contains("扩展与上下文准备")
                || all.contains("输出已结束正在整理会话记录")
                || all.contains("codex输出已结束")
                || all.contains("claude输出已结束")
                || all.contains("codex已完成本次回复")
                || all.contains("claude已完成本次回复");
    }

    private String logDedupeKey(JSONObject e) {
        if (e == null || "message".equals(e.optString("_kind"))) {
            return "";
        }
        String command = compactCommand(e.optString("command"));
        String title = cleanDisplayText(e.optString("title", e.optString("type")));
        String body = cleanDisplayText(e.optString("body"));
        String phase = e.optString("phase", e.optString("type", ""));
        String base = command.isEmpty() ? title + "|" + body : command;
        if (base.trim().isEmpty()) {
            return "";
        }
        return phase + "|" + base.replaceAll("\\s+", " ").trim();
    }

    private String commandTitle(String title, String phase) {
        String p = phase == null ? "" : phase.toLowerCase(Locale.ROOT);
        if (p.contains("result")) {
            return "执行结果";
        }
        if (p.contains("error")) {
            return "执行异常";
        }
        if (p.contains("invoke") || p.contains("command") || p.contains("tool")) {
            return "执行命令";
        }
        return title == null || title.trim().isEmpty() ? "执行命令" : title;
    }

    private String compactCommand(String raw) {
        String command = cleanDisplayText(raw);
        if (command.isEmpty()) {
            return "";
        }
        int commandIndex = command.indexOf("-Command");
        if (commandIndex >= 0) {
            return command.substring(commandIndex).trim();
        }
        commandIndex = command.indexOf("-command");
        if (commandIndex >= 0) {
            return command.substring(commandIndex).trim();
        }
        command = command.replaceAll("(?i)[A-Z]:\\\\(?:[^\\\\\\s\"']+\\\\)+([^\\\\\\s\"']+)", "$1");
        command = command.replaceAll("(?i)/(?:[^/\\s\"']+/)+([^/\\s\"']+)", "$1");
        return command.trim();
    }

    private String logPreview(JSONObject e, String body, String rawCommand, String compactCommand) {
        for (String key : new String[]{"fileContent", "content", "detail", "output", "body"}) {
            String candidate = cleanDisplayText(e.optString(key));
            if (looksLikeFileContentPreview(candidate, rawCommand, compactCommand, body)) {
                return trimPreview(candidate);
            }
        }
        return "";
    }

    private boolean looksLikeFileContentPreview(String value, String rawCommand, String compactCommand, String body) {
        String clean = cleanDisplayText(value);
        if (clean.isEmpty() || clean.equals(cleanDisplayText(rawCommand)) || clean.equals(cleanDisplayText(compactCommand))) {
            return false;
        }
        if (!clean.contains("\n")) {
            return false;
        }
        String lower = clean.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("-command") || lower.startsWith("powershell") || lower.startsWith("cmd /")
                || lower.startsWith("node ") || lower.startsWith("python ") || lower.startsWith("npm ")
                || lower.startsWith("git ") || lower.endsWith(".exe") || lower.endsWith(".cmd") || lower.endsWith(".ps1")) {
            return false;
        }
        if (clean.equals(cleanDisplayText(body)) && !hasCodeOrPatchMarker(clean)) {
            return false;
        }
        return hasCodeOrPatchMarker(clean);
    }

    private boolean hasCodeOrPatchMarker(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("diff --git")
                || lower.contains("\n@@")
                || lower.contains("\n+")
                || lower.contains("\n-")
                || lower.contains("public ")
                || lower.contains("private ")
                || lower.contains("protected ")
                || lower.contains("class ")
                || lower.contains("function ")
                || lower.contains("const ")
                || lower.contains("let ")
                || lower.contains("import ")
                || lower.contains("export ")
                || lower.contains("package ")
                || lower.contains("</")
                || lower.contains("=>")
                || lower.contains("{\n")
                || lower.contains("}\n");
    }

    private String trimPreview(String value) {
        String clean = cleanDisplayText(value).replaceAll("\\n{4,}", "\n\n\n");
        if (clean.length() <= 900) {
            return clean;
        }
        return clean.substring(0, 900) + "\n...";
    }

    private String compactLocalFileReferences(String value) {
        String clean = cleanDisplayText(value);
        if (clean.isEmpty()) {
            return clean;
        }
        clean = compactPathPattern(clean, Pattern.compile("(?i)([A-Z]:\\\\(?:[^\\\\\\s<>:\"|?*]+\\\\)*[^\\\\\\s<>:\"|?*]+)"));
        clean = compactPathPattern(clean, Pattern.compile("(?<![A-Za-z0-9:/])(/(?:Users|home|Volumes|mnt|var|tmp|private)/(?:[^\\s<>]+/)*[^\\s<>]+)"));
        return clean;
    }

    private String compactPathPattern(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String pathValue = matcher.group(1);
            String fileName = fileNameFromPath(pathValue);
            if (fileName.isEmpty()) {
                fileName = "本地文件";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement("[" + fileName + "]"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private int phaseColor(String phase) {
        String p = phase == null ? "" : phase.toLowerCase(Locale.ROOT);
        if (p.contains("invoke")) return INDIGO;
        if (p.contains("result") || p.contains("complete")) return MINT;
        if (p.contains("error")) return Color.rgb(216, 71, 86);
        if (p.contains("intent")) return BLUE;
        return MUTED;
    }

    private String phaseLabel(String phase) {
        String p = phase == null ? "" : phase.toLowerCase(Locale.ROOT);
        if (p.contains("intent")) return "计划";
        if (p.contains("invoke")) return "执行";
        if (p.contains("result")) return "结果";
        if (p.contains("complete")) return "完成";
        if (p.contains("error")) return "异常";
        if (p.contains("assistant")) return "回复";
        return "日志";
    }

    private void renderInteraction(JSONObject e) {
        if (interactionBar == null || interactionBar.getChildCount() > 0) {
            return;
        }
        interactionBar.addView(card("等待确认", e.optString("title", "桌面端请求确认")));
        LinearLayout actions = horizontal();
        actions.addView(actionButton("允许", e, "approve"), new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(actionButton("拒绝", e, "reject"), new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(actionButton("总是允许", e, "approve_always"), new LinearLayout.LayoutParams(0, dp(44), 1));
        interactionBar.addView(actions);
    }

    private Button actionButton(String text, JSONObject event, String action) {
        Button b = ghost(text);
        b.setOnClickListener(v -> runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("deviceId", boundDeviceId);
            body.put("action", action);
            api("POST", "/api/mobile/desktop-jobs/" + enc(event.optString("jobId")) + "/interactions/" + enc(event.optString("interactionId")), body);
            ui(() -> {
                pollSessionsOnce();
            });
        }));
        return b;
    }

    private View messageBubble(String role, String text) {
        return messageBubble(role, text, 0);
    }

    private View messageBubble(String role, String text, long timestamp) {
        return messageBubble(role, text, timestamp, "", -1);
    }

    private View messageBubble(String role, String text, long timestamp, String mode, int messageIndex) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        boolean user = "user".equals(role);
        box.setBackground(round(user ? Color.WHITE : GLASS, dp(16), user ? LINE : LINE));
        String rawMode = mode == null ? "" : mode;
        boolean forcePlain = rawMode.endsWith(":plain");
        String actionMode = forcePlain ? rawMode.substring(0, rawMode.length() - ":plain".length()) : rawMode;
        String clean = cleanDisplayText(text);
        if (!forcePlain && !user && ("chat".equals(actionMode) || actionMode.isEmpty()) && shouldRenderRichText(clean)) {
            addRichAssistantContent(box, clean, () -> toggleActionRows(box));
        } else {
            addPlainMessageContent(box, clean, user, () -> toggleActionRows(box));
        }
        addTimestamp(box, timestamp, user, actionMode, messageIndex, clean);
        box.setLayoutParams(bubbleLayoutParams(user));
        box.setOnClickListener(v -> toggleActionRows(box));
        return box;
    }

    private void addPlainMessageContent(LinearLayout box, String text, boolean user) {
        addPlainMessageContent(box, text, user, null);
    }

    private void addPlainMessageContent(LinearLayout box, String text, boolean user, Runnable clickAction) {
        String[] split = splitThinkingBlock(text);
        if (!user && !split[0].isEmpty()) {
            addThinkingBlock(box, split[0], clickAction);
            if (!split[1].isEmpty()) {
                box.addView(gap(8));
            }
        }
        TextView t = copy(split[1].isEmpty() ? text : split[1]);
        t.setTextColor(user ? INK : INK);
        t.setTextIsSelectable(true);
        if (clickAction != null) {
            t.setOnClickListener(v -> clickAction.run());
        }
        box.addView(t);
    }

    private String[] splitThinkingBlock(String text) {
        String clean = cleanDisplayText(text);
        if (!clean.startsWith("思考过程\n")) {
            return new String[]{"", clean};
        }
        int split = clean.indexOf("\n\n");
        if (split <= "思考过程\n".length()) {
            return new String[]{clean.substring("思考过程\n".length()).trim(), ""};
        }
        return new String[]{
                clean.substring("思考过程\n".length(), split).trim(),
                clean.substring(split + 2).trim()
        };
    }

    private boolean shouldRenderRichText(String text) {
        String clean = cleanDisplayText(text);
        return clean.contains("```")
                || clean.contains("'''")
                || clean.contains("# ")
                || clean.matches("(?s).*#{1,6}[^#\\s].*")
                || clean.startsWith("- ")
                || clean.matches("(?s)^\\s*[-*+]\\s+.*")
                || clean.matches("(?s).*(^|\\n)\\s*[-*+]\\s+.*")
                || clean.matches("(?s).*(^|\\n)\\s*'\\s*(\\n|$).*")
                || clean.contains("\n- ")
                || clean.contains("\n1. ")
                || clean.contains("**")
                || clean.contains("|")
                || clean.toLowerCase(Locale.ROOT).contains("mermaid");
    }

    private void addRichAssistantContent(LinearLayout box, String text, Runnable clickAction) {
        String[] split = splitThinkingBlock(text);
        String renderText = split[0].isEmpty() ? text : split[1];
        boolean hasRenderText = !cleanDisplayText(renderText).isEmpty();
        if (!split[0].isEmpty()) {
            addThinkingBlock(box, split[0], clickAction);
            if (hasRenderText) {
                box.addView(gap(8));
            }
        }
        if (!hasRenderText) {
            return;
        }
        WebView web = new WebView(this);
        configureContentWebView(web);
        web.addJavascriptInterface(new MermaidBridge(web), "OneApiBridge");
        if (clickAction != null) {
            web.setOnClickListener(v -> clickAction.run());
            final float[] touchStart = new float[2];
            final boolean[] horizontalDrag = new boolean[1];
            web.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    touchStart[0] = event.getX();
                    touchStart[1] = event.getY();
                    horizontalDrag[0] = false;
                } else if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                    float dx = Math.abs(event.getX() - touchStart[0]);
                    float dy = Math.abs(event.getY() - touchStart[1]);
                    if (dx > dp(10) && dx > dy * 1.2f) {
                        horizontalDrag[0] = true;
                        suppressSwipeUntil = System.currentTimeMillis() + 700L;
                        android.view.ViewParent parent = v.getParent();
                        while (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                            parent = parent.getParent();
                        }
                    }
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP && !horizontalDrag[0]) {
                    clickAction.run();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    horizontalDrag[0] = false;
                }
                return false;
            });
        }
        web.loadDataWithBaseURL("https://ai.oneapi.center/", markdownHtml(renderText), "text/html", "utf-8", null);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        box.addView(web, new LinearLayout.LayoutParams(-1, estimatedRichHeight(normalizeMarkdownForRender(renderText))));
    }

    private void addThinkingBlock(LinearLayout box, String thinkingText, Runnable clickAction) {
        LinearLayout wrap = vertical();
        wrap.setBackground(round(Color.argb(22, 54, 104, 240), dp(12), Color.argb(38, 54, 104, 240)));
        wrap.setPadding(dp(10), dp(7), dp(10), dp(7));
        TextView header = copy("Thinking：");
        header.setTextColor(Color.rgb(70, 88, 120));
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextSize(13);
        TextView body = copy(thinkingText);
        body.setTextColor(Color.rgb(84, 99, 128));
        body.setTextIsSelectable(true);
        body.setVisibility(View.GONE);
        header.setOnClickListener(v -> body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        if (clickAction != null) {
            wrap.setOnClickListener(v -> clickAction.run());
            body.setOnClickListener(v -> clickAction.run());
        }
        wrap.addView(header);
        wrap.addView(body);
        box.addView(wrap, new LinearLayout.LayoutParams(-1, -2));
    }

    private void configureContentWebView(WebView web) {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setWebViewClient(new WebViewClient());
    }

    private int estimatedRichHeight(String text) {
        String clean = cleanDisplayText(text);
        int lines = Math.max(2, clean.split("\\n", -1).length + clean.length() / 52);
        return Math.max(dp(64), Math.min(dp(220), dp(lines * 19)));
    }

    private String markdownHtml(String markdown) {
        String normalized = normalizeMarkdownForRender(markdown);
        String cacheKey = Integer.toHexString(normalized.hashCode()) + ":" + normalized.length();
        String cached = markdownHtmlCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String html = buildMarkdownHtml(normalized);
        markdownHtmlCache.put(cacheKey, html);
        while (markdownHtmlCache.size() > 80) {
            String oldestKey = markdownHtmlCache.keySet().iterator().next();
            markdownHtmlCache.remove(oldestKey);
        }
        return html;
    }

    private String buildMarkdownHtml(String normalizedMarkdown) {
        String jsMarkdown = JSONObject.quote(normalizedMarkdown);
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body{margin:0;width:100%;max-width:100%;overflow-x:hidden;}body{font:14px/1.55 sans-serif;color:#171f30;background:transparent;overflow-y:hidden;}#root{max-width:100%;overflow-x:hidden;}p{margin:0 0 10px;}h1,h2,h3,h4,h5,h6{margin:12px 0 8px;line-height:1.28;color:#111827;font-weight:700;}h1{font-size:22px}h2{font-size:20px}h3{font-size:18px}h4,h5,h6{font-size:16px}ul,ol{margin:6px 0 10px;padding-left:22px;}li{margin:3px 0;}blockquote{margin:8px 0;padding:6px 10px;border-left:3px solid #9fb3e8;background:#f6f8ff;border-radius:8px;}hr{border:0;border-top:1px solid #dce4f1;margin:12px 0;}.code-wrap{position:relative;margin:8px 0;max-width:100%;overflow-x:auto;}.code-copy{position:absolute;right:8px;top:6px;border:1px solid #dce4f1;background:rgba(255,255,255,.95);color:#3668f0;border-radius:8px;padding:2px 6px;font-size:13px;line-height:1.2;z-index:2}pre{white-space:pre-wrap;background:#f4f7fb;border:1px solid #dce4f1;border-radius:10px;padding:30px 10px 10px;overflow:auto;margin:0;}code{font-family:monospace}strong{font-weight:700}.mermaid-wrap{position:relative;border:1px solid #dce4f1;border-radius:12px;padding:28px 8px 8px;margin:8px 0;background:#fff;max-width:100%;overflow-x:auto}.mermaid-tools{position:absolute;right:6px;top:4px;display:flex;gap:6px}.mermaid-tools button{border:0;background:transparent;font-size:16px;color:#3668f0;padding:2px 6px}.table-wrap{max-width:100%;overflow-x:auto;overflow-y:hidden;margin:8px 0 10px;border:1px solid #dce4f1;border-radius:10px;background:rgba(255,255,255,.72);-webkit-overflow-scrolling:touch;touch-action:pan-x;overscroll-behavior:contain;}.table-wrap::-webkit-scrollbar{height:6px}.table-wrap::-webkit-scrollbar-thumb{background:#b8c3d8;border-radius:6px}.table-wrap table{margin:0;border:0;}table{border-collapse:collapse;width:max-content;min-width:100%;table-layout:auto;font-size:12px;}td,th{border:1px solid #dce4f1;padding:5px 6px;vertical-align:top;min-width:72px;max-width:80vw;word-break:break-word;overflow-wrap:anywhere;}th{font-weight:700;background:#f7f9ff;}</style>"
                + "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'></script>"
                + "<script src='https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js'></script></head><body><div id='root'></div>"
                + "<script>const md=" + jsMarkdown + ";if(window.mermaid)mermaid.initialize({startOnLoad:false,securityLevel:'loose'});"
                + "function esc(s){return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')}"
                + "function inlineFmt(s){return esc(s).replace(/`([^`]+)`/g,'<code>$1</code>').replace(/\\*\\*([^*]+)\\*\\*/g,'<strong>$1</strong>')}"
                + "function cells(line){let p=line.trim();if(p.startsWith('|'))p=p.slice(1);if(p.endsWith('|'))p=p.slice(0,-1);return p.split('|').map(x=>x.trim())}"
                + "function tableSep(line){return /^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$/.test(line)}"
                + "function fallbackMarkdown(s){const lines=String(s||'').split(/\\n/);let h='',i=0,inCode=false,code='',list='';const close=()=>{if(list){h+='</'+list+'>';list=''}};while(i<lines.length){let line=lines[i];if(/^\\s*```/.test(line)){if(inCode){h+='<pre><code>'+esc(code.replace(/\\n$/,''))+'</code></pre>';code='';inCode=false}else{close();inCode=true}i++;continue}if(inCode){code+=line+'\\n';i++;continue}if(!line.trim()){close();i++;continue}if(i+1<lines.length&&line.indexOf('|')>=0&&tableSep(lines[i+1])){close();const head=cells(line);i+=2;let rows=[];while(i<lines.length&&lines[i].indexOf('|')>=0&&lines[i].trim()){rows.push(cells(lines[i]));i++}h+='<table><thead><tr>'+head.map(c=>'<th>'+inlineFmt(c)+'</th>').join('')+'</tr></thead><tbody>'+rows.map(r=>'<tr>'+r.map(c=>'<td>'+inlineFmt(c)+'</td>').join('')+'</tr>').join('')+'</tbody></table>';continue}let m=line.match(/^(#{1,6})\\s+(.+)$/);if(m){close();h+='<h'+m[1].length+'>'+inlineFmt(m[2])+'</h'+m[1].length+'>';i++;continue}if(/^\\s*---+\\s*$/.test(line)){close();h+='<hr>';i++;continue}m=line.match(/^\\s*[-*+]\\s+(.+)$/);if(m){if(list!=='ul'){close();list='ul';h+='<ul>'}h+='<li>'+inlineFmt(m[1])+'</li>';i++;continue}m=line.match(/^\\s*\\d+[\\.、]\\s+(.+)$/);if(m){if(list!=='ol'){close();list='ol';h+='<ol>'}h+='<li>'+inlineFmt(m[1])+'</li>';i++;continue}close();h+='<p>'+inlineFmt(line)+'</p>';i++}close();if(inCode)h+='<pre><code>'+esc(code)+'</code></pre>';return h}"
                + "function resizeRoot(){if(window.OneApiBridge){const root=document.getElementById('root');const h=Math.ceil(root?root.getBoundingClientRect().height:document.body.scrollHeight);OneApiBridge.resize(h)}}"
                + "function enhanceMermaid(){document.querySelectorAll('pre code.language-mermaid,pre code.lang-mermaid').forEach(code=>{const src=code.textContent;const wrap=document.createElement('div');wrap.className='mermaid-wrap';wrap.innerHTML='<div class=\"mermaid-tools\"><button type=\"button\" onclick=\"downloadMermaid(this)\">⇩</button><button type=\"button\" onclick=\"fullscreenMermaid(this)\">⛶</button></div><div class=\"mermaid\">'+src+'</div>';code.closest('pre').replaceWith(wrap);});}"
                + "function enhanceTables(){document.querySelectorAll('table').forEach(table=>{if(table.closest('.table-wrap'))return;const wrap=document.createElement('div');wrap.className='table-wrap';let sx=0,sy=0;wrap.addEventListener('touchstart',e=>{const t=e.touches&&e.touches[0];if(t){sx=t.clientX;sy=t.clientY}}, {passive:true});wrap.addEventListener('touchmove',e=>{const t=e.touches&&e.touches[0];if(!t)return;const dx=Math.abs(t.clientX-sx),dy=Math.abs(t.clientY-sy);if(dx>10&&dx>dy*1.15&&window.OneApiBridge)OneApiBridge.suppressSwipe()}, {passive:true});table.parentNode.insertBefore(wrap,table);wrap.appendChild(table);});}"
                + "function enhanceCodeBlocks(){document.querySelectorAll('pre').forEach(pre=>{if(pre.dataset.enhanced==='1')return;const code=pre.querySelector('code');if(code&&/(^|\\s)(language-)?mermaid(\\s|$)/.test(code.className))return;pre.dataset.enhanced='1';const wrap=document.createElement('div');wrap.className='code-wrap';const btn=document.createElement('button');btn.type='button';btn.className='code-copy';btn.textContent='⧉';btn.onclick=(e)=>{e.stopPropagation();if(window.OneApiBridge)OneApiBridge.copyText(code?code.innerText:pre.innerText)};pre.parentNode.insertBefore(wrap,pre);wrap.appendChild(btn);wrap.appendChild(pre);});}"
                + "if(window.marked){marked.setOptions({gfm:true,breaks:true});document.getElementById('root').innerHTML=marked.parse(md)}else{document.getElementById('root').innerHTML=fallbackMarkdown(md)}"
                + "enhanceMermaid();enhanceCodeBlocks();enhanceTables();"
                + "if(window.mermaid){try{Promise.resolve(mermaid.run()).finally(()=>setTimeout(resizeRoot,80))}catch(e){setTimeout(resizeRoot,80)}}setTimeout(resizeRoot,80);requestAnimationFrame(()=>setTimeout(resizeRoot,0));function svgFor(btn){const svg=btn.closest('.mermaid-wrap').querySelector('svg');return svg?new XMLSerializer().serializeToString(svg):''}"
                + "function toPng(svg,cb){const img=new Image();img.onload=()=>{const c=document.createElement('canvas');c.width=Math.max(1,img.width);c.height=Math.max(1,img.height);const x=c.getContext('2d');x.fillStyle='#fff';x.fillRect(0,0,c.width,c.height);x.drawImage(img,0,0);cb(c.toDataURL('image/png'))};img.src='data:image/svg+xml;base64,'+btoa(unescape(encodeURIComponent(svg)));}"
                + "function downloadMermaid(btn){const svg=svgFor(btn);if(window.OneApiBridge&&svg)toPng(svg,(png)=>OneApiBridge.savePng(png))}"
                + "function fullscreenMermaid(btn){const svg=svgFor(btn);if(window.OneApiBridge&&svg)OneApiBridge.openMermaid(svg)}</script></body></html>";
    }

    private String normalizeMarkdownForRender(String markdown) {
        String source = cleanDisplayText(markdown).replace("\r\n", "\n").replace('\r', '\n');
        if (source.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean inFence = false;
        boolean inAltFence = false;
        String[] lines = source.split("\n", -1);
        for (String rawLine : lines) {
            String line = rawLine;
            String trimmed = line.trim();
            if (trimmed.startsWith("```") && !inAltFence) {
                inFence = !inFence;
                appendNormalizedLine(out, line);
                continue;
            }
            if (("'".equals(trimmed) || "'''".equals(trimmed) || trimmed.startsWith("'''")) && (!inFence || inAltFence)) {
                inFence = !inFence;
                inAltFence = !inAltFence;
                appendNormalizedLine(out, line.replaceFirst("^\\s*'''", "```").replaceFirst("^\\s*'\\s*$", "```"));
                continue;
            }
            if (!inFence) {
                line = normalizeMarkdownLine(line);
            }
            appendNormalizedLine(out, line);
        }
        String value = out.toString().trim();
        value = value.replaceAll("\\s*---\\s*(#{1,6})", "\n\n---\n\n$1 ");
        value = value.replaceAll("([^\\n])\\s+(#{1,6})([^#\\s])", "$1\n\n$2 $3");
        value = value.replaceAll("(?m)(^|\\n)(#{1,6})([^#\\s])", "$1$2 $3");
        value = value.replaceAll("([^\\n])\\s+[-*]([^\\s\\-*])", "$1\n- $2");
        value = value.replaceAll("([^\\n])\\s+(\\d+[\\.、])([^\\s])", "$1\n$2 $3");
        return value.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String normalizeMarkdownLine(String line) {
        String value = line == null ? "" : line;
        value = value.replaceAll("^(\\s{0,3}#{1,6})([^#\\s])", "$1 $2");
        value = value.replaceAll("^(\\s*[-*+])([^\\s])", "$1 $2");
        value = value.replaceAll("^(\\s*\\d+[\\.、])([^\\s])", "$1 $2");
        return value;
    }

    private void appendNormalizedLine(StringBuilder out, String line) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(line);
    }

    private class MermaidBridge {
        private final WebView web;

        MermaidBridge() {
            this.web = null;
        }

        MermaidBridge(WebView web) {
            this.web = web;
        }

        @JavascriptInterface
        public void openMermaid(String svg) {
            ui(() -> showMermaidPreview(svg));
        }

        @JavascriptInterface
        public void savePng(String dataUrl) {
            ui(() -> saveDataUrlImage(dataUrl));
        }

        @JavascriptInterface
        public void copyText(String text) {
            ui(() -> copyToClipboard(text));
        }

        @JavascriptInterface
        public void suppressSwipe() {
            suppressSwipeUntil = System.currentTimeMillis() + 900L;
        }

        @JavascriptInterface
        public void resize(int height) {
            if (web == null) {
                return;
            }
            ui(() -> {
                int targetHeight = Math.max(dp(56), dp(Math.min(height + 10, 1600)));
                web.setLayoutParams(new LinearLayout.LayoutParams(-1, targetHeight));
                web.requestLayout();
            });
        }
    }

    private void showMermaidPreview(String svg) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(190, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold("Mermaid"), new LinearLayout.LayoutParams(0, -2, 1));
        Button save = iconButton("⇩");
        save.setContentDescription("下载");
        save.setOnClickListener(v -> saveSvgAsPng(svg));
        titleRow.addView(save, new LinearLayout.LayoutParams(dp(32), dp(32)));
        Button close = iconButton("×");
        close.setContentDescription("关闭");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        WebView web = new WebView(this);
        configureContentWebView(web);
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>html,body{height:100%;margin:0;background:#fff;overflow:auto}svg{max-width:none}</style></head><body>" + (svg == null ? "" : svg) + "</body></html>";
        web.loadDataWithBaseURL("https://ai.oneapi.center/", html, "text/html", "utf-8", null);
        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(-1, dp(560));
        webLp.setMargins(0, dp(8), 0, 0);
        panel.addView(web, webLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        lp.setMargins(dp(14), 0, dp(14), 0);
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private LinearLayout addStreamingBubble(String role) {
        return addStreamingBubble(role, 0);
    }

    private LinearLayout addStreamingBubble(String role, long timestamp) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(GLASS, dp(16), LINE));
        box.setLayoutParams(bubbleLayoutParams("user".equals(role)));
        box.setOnClickListener(v -> toggleActionRows(box));
        content.addView(box);
        return box;
    }

    private void replaceStreamingBubble(LinearLayout box, String text, long timestamp) {
        if (box == null) {
            return;
        }
        renderAssistantBubbleContent(box, text, timestamp, true);
    }

    private void updateStreamingBubble(LinearLayout box, String text, long timestamp, boolean withActions) {
        if (box == null) {
            return;
        }
        if (!withActions) {
            updateLiveAssistantBubble(box, text, timestamp);
            return;
        }
        renderAssistantBubbleContent(box, text, timestamp, withActions);
    }

    private void updateLiveAssistantBubble(LinearLayout box, String text, long timestamp) {
        String clean = cleanDisplayText(text);
        String[] split = splitThinkingBlock(clean);
        boolean needsThinking = !split[0].isEmpty();
        String answer = split[1].isEmpty() ? (needsThinking ? "" : clean) : split[1];
        TextView thinkingBody = taggedTextView(box, "live_thinking_body");
        TextView answerBody = taggedTextView(box, "live_answer_body");
        boolean hasThinkingView = thinkingBody != null;
        boolean hasAnswerView = answerBody != null;
        if (needsThinking != hasThinkingView || (!answer.trim().isEmpty() && !hasAnswerView)) {
            clearContainer(box);
            if (needsThinking) {
                LinearLayout wrap = vertical();
                wrap.setTag("live_thinking_wrap");
                wrap.setBackground(round(Color.argb(22, 54, 104, 240), dp(12), Color.argb(38, 54, 104, 240)));
                wrap.setPadding(dp(10), dp(7), dp(10), dp(7));
                TextView header = copy("Thinking：");
                header.setTextColor(Color.rgb(70, 88, 120));
                header.setTypeface(Typeface.DEFAULT_BOLD);
                header.setTextSize(13);
                TextView body = copy(split[0]);
                body.setTag("live_thinking_body");
                body.setTextColor(Color.rgb(84, 99, 128));
                body.setTextIsSelectable(true);
                wrap.addView(header);
                wrap.addView(body);
                box.addView(wrap, new LinearLayout.LayoutParams(-1, -2));
                if (!answer.trim().isEmpty()) {
                    box.addView(gap(8));
                }
            }
            if (!answer.trim().isEmpty()) {
                TextView body = copy(answer);
                body.setTag("live_answer_body");
                body.setTextColor(INK);
                body.setTextIsSelectable(true);
                box.addView(body);
            }
            addTimestamp(box, timestamp, false, "chat", -1, "");
            return;
        }
        if (thinkingBody != null) {
            thinkingBody.setText(split[0]);
        }
        if (answerBody != null) {
            answerBody.setText(answer);
        }
    }

    private TextView taggedTextView(View rootView, String tag) {
        if (rootView == null) {
            return null;
        }
        Object currentTag = rootView.getTag();
        if (tag.equals(currentTag) && rootView instanceof TextView) {
            return (TextView) rootView;
        }
        if (rootView instanceof LinearLayout) {
            LinearLayout group = (LinearLayout) rootView;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = taggedTextView(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        if (rootView instanceof FrameLayout) {
            FrameLayout group = (FrameLayout) rootView;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = taggedTextView(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void renderAssistantBubbleContent(LinearLayout box, String text, long timestamp, boolean withActions) {
        clearContainer(box);
        String clean = cleanDisplayText(text);
        if (!withActions) {
            addLiveAssistantContent(box, clean);
        } else if (shouldRenderRichText(clean) || !splitThinkingBlock(clean)[0].isEmpty()) {
            addRichAssistantContent(box, clean, () -> toggleActionRows(box));
        } else {
            addPlainMessageContent(box, clean, false, () -> toggleActionRows(box));
        }
        addTimestamp(box, timestamp, false, "chat", -1, withActions ? clean : "");
    }

    private void addLiveAssistantContent(LinearLayout box, String text) {
        String[] split = splitThinkingBlock(text);
        if (!split[0].isEmpty()) {
            addLiveThinkingBlock(box, split[0]);
            if (!split[1].isEmpty()) {
                box.addView(gap(8));
            }
        }
        String answer = split[1].isEmpty() ? (split[0].isEmpty() ? text : "") : split[1];
        if (!cleanDisplayText(answer).isEmpty()) {
            TextView body = copy(answer);
            body.setTextColor(INK);
            body.setTextIsSelectable(true);
            box.addView(body);
        }
    }

    private void addLiveThinkingBlock(LinearLayout box, String thinkingText) {
        LinearLayout wrap = vertical();
        wrap.setBackground(round(Color.argb(22, 54, 104, 240), dp(12), Color.argb(38, 54, 104, 240)));
        wrap.setPadding(dp(10), dp(7), dp(10), dp(7));
        TextView header = copy("Thinking：");
        header.setTextColor(Color.rgb(70, 88, 120));
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextSize(13);
        TextView body = copy(thinkingText);
        body.setTextColor(Color.rgb(84, 99, 128));
        body.setTextIsSelectable(true);
        wrap.addView(header);
        if (!cleanDisplayText(thinkingText).isEmpty()) {
            wrap.addView(body);
        }
        box.addView(wrap, new LinearLayout.LayoutParams(-1, -2));
    }

    private LinearLayout addImageProgressBubble() {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(GLASS, dp(16), LINE));
        DrawingProgressView progress = new DrawingProgressView(this);
        box.addView(progress, new LinearLayout.LayoutParams(dp(168), dp(168)));
        TextView status = copy("绘制草稿...");
        status.setTag("image_status");
        status.setTextColor(INK);
        box.addView(status);
        box.setLayoutParams(bubbleLayoutParams(false));
        content.addView(box);
        handler.postDelayed(() -> {
            if (requestRunning && status.getParent() != null) {
                status.setText("细化画面...");
            }
        }, 1200);
        handler.postDelayed(() -> {
            if (requestRunning && status.getParent() != null) {
                status.setText("后处理...");
            }
        }, 2400);
        return box;
    }

    private LinearLayout.LayoutParams bubbleLayoutParams(boolean user) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(user ? dp(42) : 0, dp(8), 0, dp(8));
        return lp;
    }

    private View imageResultBubble(String source) {
        return imageResultBubble(source, 0);
    }

    private View imageResultBubble(String source, long timestamp) {
        return imageResultBubble(source, timestamp, "", -1);
    }

    private View imageResultBubble(String source, long timestamp, String mode, int messageIndex) {
        return imageResultBubble(source, timestamp, mode, messageIndex, "assistant");
    }

    private View imageResultBubble(String source, long timestamp, String mode, int messageIndex, String role) {
        boolean user = "user".equals(role);
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(user ? Color.WHITE : GLASS, dp(16), LINE));
        box.setLayoutParams(bubbleLayoutParams(user));
        renderImageResult(box, source, timestamp, mode, messageIndex, user);
        box.setOnClickListener(v -> toggleActionRows(box));
        return box;
    }

    private void renderImageResult(LinearLayout box, String source) {
        renderImageResult(box, source, System.currentTimeMillis());
    }

    private void renderImageResult(LinearLayout box, String source, long timestamp) {
        renderImageResult(box, source, timestamp, "", -1);
    }

    private void renderImageResult(LinearLayout box, String source, long timestamp, String mode, int messageIndex) {
        renderImageResult(box, source, timestamp, mode, messageIndex, false);
    }

    private void renderImageResult(LinearLayout box, String source, long timestamp, String mode, int messageIndex, boolean user) {
        if (box == null) {
            content.addView(messageBubble("assistant", source));
            return;
        }
        clearContainer(box);
        if (isImageSource(source)) {
            ImageView image = new ImageView(this);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(round(Color.WHITE, dp(14), LINE));
            final long[] lastTapAt = {0L};
            image.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                if (now - lastTapAt[0] < 320L) {
                    lastTapAt[0] = 0L;
                    showImagePreview(Uri.parse(source));
                    return;
                }
                lastTapAt[0] = now;
                toggleActionRows(box);
            });
            image.setOnLongClickListener(v -> {
                showImagePreview(Uri.parse(source));
                return true;
            });
            box.addView(image, new LinearLayout.LayoutParams(-1, dp(240)));
            setImageSource(image, source);
        } else {
            addPlainMessageContent(box, cleanDisplayText(source), false, () -> toggleActionRows(box));
        }
        addTimestamp(box, timestamp, user, mode, messageIndex, source);
    }

    private void addTimestamp(LinearLayout box, long timestamp, boolean user) {
        addTimestamp(box, timestamp, user, "", -1, "");
    }

    private void addTimestamp(LinearLayout box, long timestamp, boolean user, String mode, int messageIndex, String messageText) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTag("bubble_meta");
        LinearLayout actions = horizontal();
        actions.setTag("bubble_actions");
        actions.setVisibility(View.GONE);
        boolean editableImage = !user && ("image".equals(mode) || mode.isEmpty()) && isImageSource(messageText);
        if (editableImage) {
            actions.addView(tinyAction(R.drawable.ic_bubble_edit, "编辑这张图", () -> referenceImageForEdit(messageText)));
            actions.addView(tinyAction(R.drawable.ic_bubble_download, "下载图片", () -> saveImageSource(messageText)));
        }
        if (!cleanDisplayText(messageText).isEmpty()) {
            actions.addView(tinyAction(R.drawable.ic_bubble_copy, "复制", () -> copyToClipboard(cleanDisplayText(messageText))));
            actions.addView(tinyAction(R.drawable.ic_bubble_share, "分享", () -> shareText(cleanDisplayText(messageText))));
        }
        boolean localMessage = ("chat".equals(mode) || "image".equals(mode)) && messageIndex >= 0;
        if (localMessage) {
            if (user) {
                actions.addView(tinyAction(R.drawable.ic_bubble_edit, "编辑", () -> editLocalMessage(mode, messageIndex)));
            }
            actions.addView(tinyAction(R.drawable.ic_bubble_delete, "删除", () -> deleteLocalMessage(mode, messageIndex)));
        }
        TextView time = copy(timestamp <= 0 ? "" : formatDateTime(timestamp));
        time.setTextSize(11);
        time.setTextColor(MUTED);
        time.setGravity(user ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, 0);
        if (user) {
            row.addView(actions, new LinearLayout.LayoutParams(-2, dp(28)));
            row.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
        } else {
            row.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(actions, new LinearLayout.LayoutParams(-2, dp(28)));
        }
        box.addView(row, lp);
    }

    private ImageButton tinyAction(int iconRes, String description, Runnable action) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(iconRes);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(5), dp(5), dp(5), dp(5));
        b.setBackground(new ColorDrawable(Color.TRANSPARENT));
        b.setContentDescription(description);
        b.setOnClickListener(v -> action.run());
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));
        return b;
    }

    private void toggleActionRows(LinearLayout box) {
        View row = box.findViewWithTag("bubble_actions");
        if (row == null) {
            return;
        }
        boolean willShow = row.getVisibility() != View.VISIBLE;
        if (activeBubbleActions != null && activeBubbleActions != row) {
            activeBubbleActions.setVisibility(View.GONE);
        }
        row.setVisibility(willShow ? View.VISIBLE : View.GONE);
        activeBubbleActions = willShow ? row : null;
    }

    private boolean isImageSource(String source) {
        String value = source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("data:image/")
                || value.startsWith("content:")
                || value.startsWith("file:");
    }

    private void referenceImageForEdit(String source) {
        if (source == null || source.trim().isEmpty()) {
            return;
        }
        selectedAttachmentUris.clear();
        selectedAttachmentUris.add(Uri.parse(source.trim()));
        renderAttachmentPreview();
        focusComposerInput();
    }

    private void copyToClipboard(String text) {
        String clean = cleanDisplayText(text);
        if (clean.isEmpty()) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("OneAPI", clean));
    }

    private void shareText(String text) {
        String clean = cleanDisplayText(text);
        if (clean.isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, clean);
        try {
            startActivity(Intent.createChooser(intent, "分享"));
        } catch (Exception ignored) {
            toast("当前系统没有可用的分享应用");
        }
    }

    private void editLocalMessage(String mode, int index) {
        JSONObject item = localMessageAt(mode, index);
        if (item == null || activeInput == null) {
            return;
        }
        activeInput.setText(cleanDisplayText(item.optString("text", "")));
        focusComposerInput();
    }

    private void deleteLocalMessage(String mode, int index) {
        try {
            JSONObject root = localConversationStore(mode);
            JSONObject session = findLocalSession(root, activeLocalSessionId(mode));
            JSONArray messages = session == null ? null : session.optJSONArray("messages");
            if (messages == null || index < 0 || index >= messages.length()) {
                return;
            }
            JSONArray next = new JSONArray();
            for (int i = 0; i < messages.length(); i++) {
                if (i != index) {
                    next.put(messages.opt(i));
                }
            }
            session.put("messages", next);
            session.put("updatedAt", System.currentTimeMillis());
            saveLocalConversationStore(mode, root);
            renderLocalConversationNow(mode, false);
        } catch (Exception ignored) {
        }
    }

    private JSONObject localMessageAt(String mode, int index) {
        JSONArray messages = localConversationMessages(mode);
        return index >= 0 && index < messages.length() ? messages.optJSONObject(index) : null;
    }

    private void saveImageSource(String source) {
        String value = cleanDisplayText(source);
        if (value.isEmpty()) {
            return;
        }
        if (value.startsWith("data:image/")) {
            saveDataUrlImage(value);
            return;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            toast("图片地址无法保存");
            return;
        }
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(value).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                String mime = conn.getContentType();
                try (InputStream input = conn.getInputStream();
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    saveImageBytes(output.toByteArray(), mime == null ? "image/png" : mime);
                }
            } catch (Exception ignored) {
                ui(() -> toast("图片下载失败"));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void saveImageBytes(byte[] bytes, String mime) {
        try {
            if (bytes == null || bytes.length == 0) {
                ui(() -> toast("图片数据无法保存"));
                return;
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "oneapi_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, (mime == null || mime.trim().isEmpty()) ? "image/png" : mime.split(";")[0].trim());
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                ui(() -> toast("无法保存图片"));
                return;
            }
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output != null) {
                    output.write(bytes);
                }
            }
        } catch (Exception ignored) {
            ui(() -> toast("图片保存失败"));
        }
    }

    private void saveDataUrlImage(String dataUrl) {
        try {
            String value = dataUrl == null ? "" : dataUrl.trim();
            int comma = value.indexOf(',');
            if (!value.startsWith("data:image/") || comma < 0) {
                toast("图片数据无法保存");
                return;
            }
            byte[] bytes = android.util.Base64.decode(value.substring(comma + 1), android.util.Base64.DEFAULT);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "oneapi_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                toast("无法保存图片");
                return;
            }
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output != null) {
                    output.write(bytes);
                }
            }
        } catch (Exception ignored) {
            toast("图片保存失败");
        }
    }

    private void saveSvgAsPng(String svg) {
        try {
            if (shell == null) {
                toast("当前页面无法导出图片");
                return;
            }
            WebView converter = new WebView(this);
            configureContentWebView(converter);
            converter.addJavascriptInterface(new MermaidBridge(), "OneApiBridge");
            String encoded = android.util.Base64.encodeToString((svg == null ? "" : svg).getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            String html = "<!doctype html><html><body><script>"
                    + "const svg=atob('" + encoded + "');const img=new Image();"
                    + "img.onload=()=>{const c=document.createElement('canvas');c.width=Math.max(1,img.width);c.height=Math.max(1,img.height);const x=c.getContext('2d');x.fillStyle='#fff';x.fillRect(0,0,c.width,c.height);x.drawImage(img,0,0);OneApiBridge.savePng(c.toDataURL('image/png'));};"
                    + "img.src='data:image/svg+xml;base64,'+btoa(unescape(encodeURIComponent(svg)));"
                    + "</script></body></html>";
            shell.addView(converter, new FrameLayout.LayoutParams(1, 1));
            converter.loadDataWithBaseURL("https://ai.oneapi.center/", html, "text/html", "utf-8", null);
            handler.postDelayed(() -> shell.removeView(converter), 2500);
        } catch (Exception ignored) {
            toast("无法导出 Mermaid 图片");
        }
    }

    private void loadImageIntoView(String url, ImageView target) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                conn.disconnect();
                if (bitmap != null) {
                    ui(() -> target.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void setImageSource(ImageView target, String source) {
        if (target == null || source == null || source.trim().isEmpty()) {
            return;
        }
        String value = source.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            loadImageIntoView(value, target);
            return;
        }
        if (value.startsWith("data:image/")) {
            executor.execute(() -> {
                try {
                    String base64 = value.substring(value.indexOf(",") + 1);
                    byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap != null) {
                        ui(() -> target.setImageBitmap(bitmap));
                    }
                } catch (Exception ignored) {
                }
            });
            return;
        }
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        if ("content".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme)) {
            executor.execute(() -> {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        ui(() -> target.setImageBitmap(bitmap));
                    }
                } catch (Exception ignored) {
                    ui(() -> target.setImageURI(uri));
                }
            });
            return;
        }
        target.setImageURI(uri);
    }

    private JSONObject api(String method, String path, JSONObject body) throws Exception {
        String base = prefs.getString("server", "").replaceAll("/+$", "");
        URL url = new URL(base + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(180000);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String cookie = prefs.getString("cookie", "");
        if (!cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        String userId = prefs.getString("user_id", "");
        if (!userId.isEmpty()) {
            conn.setRequestProperty("New-Api-User", userId);
        }
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        String setCookie = conn.getHeaderField("Set-Cookie");
        if (setCookie != null && !setCookie.isEmpty()) {
            prefs.edit().putString("cookie", setCookie.split(";", 2)[0]).apply();
        }
        int code = conn.getResponseCode();
        String raw = readStream(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();
        JSONObject json;
        try {
            json = new JSONObject(raw.isEmpty() ? "{}" : raw);
        } catch (Exception parseError) {
            throw new ApiException(code, raw, code >= 400 ? "服务器接口暂时不可用，请确认服务器已更新。" : "服务器返回内容无法识别。");
        }
        if (code >= 400 || (json.has("success") && !json.optBoolean("success"))) {
            throw new ApiException(code, raw, json.optString("message", "服务器接口暂时不可用，请稍后重试。"));
        }
        return json;
    }

    private JSONObject apiImageEdit(Uri imageUri, String prompt) throws Exception {
        String base = prefs.getString("server", "").replaceAll("/+$", "");
        String boundary = "----OneApiAndroid" + System.currentTimeMillis();
        URL url = new URL(base + "/v1/images/edits");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(180000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        String cookie = prefs.getString("cookie", "");
        if (!cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        String userId = prefs.getString("user_id", "");
        if (!userId.isEmpty()) {
            conn.setRequestProperty("New-Api-User", userId);
        }
        conn.setDoOutput(true);
        String mime = getContentResolver().getType(imageUri);
        if (mime == null || mime.trim().isEmpty()) {
            mime = "image/png";
        }
        String fileName = "image." + (mime.contains("jpeg") ? "jpg" : mime.contains("webp") ? "webp" : "png");
        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            writeMultipartField(out, boundary, "model", "gpt-image-2");
            writeMultipartField(out, boundary, "prompt", prompt);
            writeMultipartField(out, boundary, "size", selectedImageSize);
            writeMultipartField(out, boundary, "quality", imageQualityValue(selectedImageQuality));
            writeMultipartField(out, boundary, "response_format", "b64_json");
            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"\r\n");
            out.writeBytes("Content-Type: " + mime + "\r\n\r\n");
            try (InputStream input = getContentResolver().openInputStream(imageUri)) {
                if (input == null) {
                    throw new ApiException(0, "", "图片读取失败，请重新选择图片后再发送。");
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            out.writeBytes("\r\n--" + boundary + "--\r\n");
        }
        String setCookie = conn.getHeaderField("Set-Cookie");
        if (setCookie != null && !setCookie.isEmpty()) {
            prefs.edit().putString("cookie", setCookie.split(";", 2)[0]).apply();
        }
        int code = conn.getResponseCode();
        String raw = readStream(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();
        JSONObject json;
        try {
            json = new JSONObject(raw.isEmpty() ? "{}" : raw);
        } catch (Exception parseError) {
            throw new ApiException(code, raw, code >= 400 ? "图片编辑接口暂时不可用，请确认服务器已更新。" : "服务器返回内容无法识别。");
        }
        if (code >= 400 || (json.has("success") && !json.optBoolean("success"))) {
            throw new ApiException(code, raw, json.optString("message", "图片编辑接口暂时不可用，请稍后重试。"));
        }
        return json;
    }

    private void writeMultipartField(DataOutputStream out, String boundary, String name, String value) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void runNetwork(JsonRunnable work) {
        executor.execute(() -> {
            try {
                work.run();
            } catch (Exception e) {
                ui(() -> {
                    setSending(false);
                    toast(userMessage(e));
                });
            }
        });
    }

    private void runNetworkQuiet(JsonRunnable work) {
        executor.execute(() -> {
            try {
                work.run();
            } catch (Exception ignored) {
            }
        });
    }

    private String userMessage(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String lower = msg.toLowerCase(Locale.ROOT);
        if (msg.contains("未登录") || lower.contains("not logged")) {
            return "登录已失效，请重新登录";
        }
        if (lower.contains("already bound") || msg.contains("已经绑定")) {
            return "这台客户端已经绑定了另一个 Android 应用，请先解除原绑定";
        }
        if (e instanceof ApiException && ((ApiException) e).status == 404) {
            return "服务器接口不存在，请更新服务器版本或检查服务地址";
        }
        if (lower.contains("timeout") || lower.contains("refused")) {
            return "服务器暂时无响应，请稍后重试";
        }
        return msg.isEmpty() ? "操作失败，请稍后重试" : msg;
    }

    private interface JsonRunnable {
        void run() throws Exception;
    }

    private interface ProgressHandler {
        void onProgress(int percent);
    }

    private interface ChoiceHandler {
        void onChoice(String value);
    }

    private static class ApiException extends Exception {
        final int status;
        final String raw;

        ApiException(int status, String raw, String message) {
            super(message);
            this.status = status;
            this.raw = raw == null ? "" : raw;
        }
    }

    private static class FlowBackgroundView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private LinearGradient backgroundGradient;
        private long startedAt = System.currentTimeMillis();

        FlowBackgroundView(Context context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            backgroundGradient = new LinearGradient(
                    0,
                    0,
                    Math.max(1, w),
                    Math.max(1, h),
                    new int[]{
                            Color.rgb(241, 247, 255),
                            Color.rgb(230, 238, 255),
                            Color.rgb(238, 249, 246),
                            Color.rgb(245, 250, 255)
                    },
                    null,
                    Shader.TileMode.CLAMP
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = Math.max(1, getWidth());
            float h = Math.max(1, getHeight());
            float t = ((System.currentTimeMillis() - startedAt) % 8000) / 8000f;
            float wave = (float) ((Math.sin(t * Math.PI * 2) + 1f) * 0.5f);
            float wave2 = (float) ((Math.cos(t * Math.PI * 2) + 1f) * 0.5f);
            paint.setShader(backgroundGradient);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setColor(Color.argb(42, 54, 104, 240));
            canvas.drawCircle(w * (0.18f + 0.18f * wave), h * 0.24f, w * 0.34f, paint);
            paint.setColor(Color.argb(36, 73, 190, 143));
            canvas.drawCircle(w * 0.86f, h * (0.18f + 0.16f * wave2), w * 0.32f, paint);
            paint.setColor(Color.argb(30, 91, 77, 215));
            canvas.drawCircle(w * (0.42f + 0.12f * wave2), h * 0.86f, w * 0.46f, paint);
            postInvalidateDelayed(33);
        }
    }

    private static class FrostedOverlayView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        FrostedOverlayView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.argb(74, 255, 255, 255));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setColor(Color.argb(18, 255, 255, 255));
            for (int y = 0; y < getHeight(); y += 6) {
                canvas.drawLine(0, y, getWidth(), y, paint);
            }
        }
    }

    private static class DrawingProgressView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long startedAt = System.currentTimeMillis();

        DrawingProgressView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            float left = (getWidth() - size) / 2f;
            float top = (getHeight() - size) / 2f;
            paint.setColor(Color.rgb(244, 248, 255));
            canvas.drawRoundRect(left, top, left + size, top + size, 28, 28, paint);
            float t = ((System.currentTimeMillis() - startedAt) % 1400) / 1400f;
            for (int i = 0; i < 5; i++) {
                float p = (t + i * 0.18f) % 1f;
                paint.setColor(Color.argb((int) (70 + 120 * (1f - p)), 54, 104, 240));
                canvas.drawCircle(left + size * (0.18f + p * 0.64f), top + size * (0.25f + i * 0.12f), size * (0.035f + p * 0.02f), paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(140, 73, 190, 143));
            canvas.drawRoundRect(left + size * 0.18f, top + size * 0.18f, left + size * 0.82f, top + size * 0.82f, 22, 22, paint);
            paint.setStyle(Paint.Style.FILL);
            postInvalidateDelayed(33);
        }
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout glassPanel() {
        LinearLayout l = vertical();
        l.setBackground(round(GLASS_SOFT, dp(22), LINE));
        return l;
    }

    private LinearLayout cardPanel() {
        LinearLayout c = vertical();
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackground(round(GLASS, dp(18), LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(16));
        c.setLayoutParams(lp);
        return c;
    }

    private View heroCard(String title, String desc) {
        LinearLayout c = cardPanel();
        TextView t = title(title);
        t.setTextSize(24);
        c.addView(t);
        c.addView(copy(desc));
        return c;
    }

    private View card(String title, String desc) {
        LinearLayout c = cardPanel();
        c.addView(bold(title));
        c.addView(copy(desc));
        return c;
    }

    private View loadingCard(String desc) {
        LinearLayout c = cardPanel();
        LinearLayout row = horizontal();
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        row.addView(progress, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView label = copy(desc);
        label.setTextColor(MUTED);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, -2, 1);
        labelLp.setMargins(dp(10), 0, 0, 0);
        row.addView(label, labelLp);
        c.addView(row);
        return c;
    }

    private View loadEarlierButton(String text, Runnable action) {
        TextView button = copy(text);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(BLUE);
        button.setPadding(dp(10), dp(9), dp(10), dp(9));
        button.setBackground(round(Color.argb(210, 255, 255, 255), dp(14), Color.argb(76, 54, 104, 240)));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(8));
        button.setLayoutParams(lp);
        return button;
    }

    private View sectionTitle(String text) {
        TextView t = title(text);
        t.setTextSize(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(14));
        t.setLayoutParams(lp);
        return t;
    }

    private View planCard(String name, String desc, String tag) {
        LinearLayout c = cardPanel();
        LinearLayout row = horizontal();
        row.addView(bold(name), new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = copy(tag);
        badge.setTextColor(BLUE);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(Color.argb(62, 54, 104, 240), dp(12), Color.argb(80, 54, 104, 240)));
        row.addView(badge, new LinearLayout.LayoutParams(dp(78), dp(28)));
        c.addView(row);
        c.addView(copy(desc));
        Button subscribe = primary("查看套餐");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(38));
        lp.setMargins(0, dp(10), 0, 0);
        c.addView(subscribe, lp);
        return c;
    }

    private void showModelChoice(String title, List<String> models, String current, String mode, ChoiceHandler handler) {
        List<String> filtered = new ArrayList<>();
        for (String model : models) {
            if (isModelAllowedForMode(model, mode) && !filtered.contains(model)) {
                filtered.add(model);
            }
        }
        Map<String, List<String>> grouped = new HashMap<>();
        List<String> order = new ArrayList<>();
        for (String model : filtered) {
            String vendor = modelVendor(model);
            if (!grouped.containsKey(vendor)) {
                grouped.put(vendor, new ArrayList<>());
                order.add(vendor);
            }
            grouped.get(vendor).add(model);
        }
        List<String> ordered = new ArrayList<>();
        for (String vendor : list("OpenAI", "Claude", "Gemini", "DeepSeek", "MiMo", "其他")) {
            if (grouped.containsKey(vendor)) {
                ordered.add(vendor);
            }
        }
        for (String vendor : order) {
            if (!ordered.contains(vendor)) {
                ordered.add(vendor);
            }
        }
        showGroupedChoice(title, ordered, grouped, current, handler);
    }

    private boolean isModelAllowedForMode(String model, String mode) {
        String n = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if ("chat".equals(mode)) {
            return isChatModelName(model) && !n.contains("codex");
        }
        if ("codex".equals(mode)) {
            return !n.startsWith("claude") && !n.contains("image");
        }
        if ("claude".equals(mode)) {
            return n.startsWith("claude") || n.startsWith("deepseek") || n.startsWith("mimo");
        }
        return true;
    }

    private String modelVendor(String model) {
        String n = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (n.startsWith("claude")) return "Claude";
        if (n.startsWith("gemini") || n.contains("google")) return "Gemini";
        if (n.startsWith("deepseek")) return "DeepSeek";
        if (n.startsWith("mimo")) return "MiMo";
        if (n.startsWith("gpt") || n.contains("codex")) return "OpenAI";
        return "其他";
    }

    private void showRecentSessions(String client) {
        if ("chat".equals(client) || "image".equals(client)) {
            showLocalRecentSessions(client);
            return;
        }
        if (cachedDesktopSessions != null && cachedDesktopSessions.length() > 0) {
            showRecentSessionsFromArray(client, cachedDesktopSessions);
            refreshDesktopSessionsCache(true);
            return;
        }
        runNetwork(() -> {
            JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
            JSONArray sessions = envelope.optJSONArray("data");
            JSONArray finalSessions = sessions == null ? new JSONArray() : sessions;
            prefs.edit().putString("desktop_sessions_cache", finalSessions.toString()).apply();
            ui(() -> {
                cachedDesktopSessions = finalSessions;
                hydrateCliTimelineCache(finalSessions);
            });
            sessions = finalSessions;
            Map<String, List<String>> grouped = new HashMap<>();
            List<String> tabs = new ArrayList<>();
            Map<String, String> sessionIdByOption = new HashMap<>();
            Map<String, JSONObject> sessionByOption = new HashMap<>();
            if (sessions != null) {
                List<JSONObject> cliSessions = new ArrayList<>();
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject session = sessions.optJSONObject(i);
                    if (session == null || !client.equals(session.optString("client"))) {
                        continue;
                    }
                    cliSessions.add(session);
                }
                cliSessions.sort((a, b) -> Long.compare(b.optLong("updatedAt"), a.optLong("updatedAt")));
                Map<String, List<JSONObject>> byProject = new LinkedHashMap<>();
                List<String> projectKeys = new ArrayList<>();
                Set<String> seenSessions = new HashSet<>();
                for (JSONObject session : cliSessions) {
                    String sessionKey = session.optString("sessionId", session.optString("id", "")).trim();
                    if (sessionKey.isEmpty()) {
                        sessionKey = session.optString("id", "").trim();
                    }
                    if (!sessionKey.isEmpty() && !seenSessions.add(sessionKey)) {
                        continue;
                    }
                    String title = cliSessionProjectTitle(session, client);
                    String projectKey = cliSessionProjectKey(session, client);
                    List<JSONObject> bucket = byProject.get(projectKey);
                    if (bucket == null) {
                        if (byProject.size() >= 3) {
                            continue;
                        }
                        bucket = new ArrayList<>();
                        byProject.put(projectKey, bucket);
                        projectKeys.add(projectKey);
                        tabs.add(title);
                    }
                    if (bucket.size() < 5) {
                        bucket.add(session);
                    }
                }
                Set<String> pinned = pinnedRecentSessions(client);
                for (int projectIndex = 0; projectIndex < tabs.size(); projectIndex++) {
                    String title = tabs.get(projectIndex);
                    String projectKey = projectKeys.get(projectIndex);
                    List<JSONObject> projectSessions = byProject.get(projectKey);
                    List<String> options = new ArrayList<>();
                    Set<String> seenOptions = new HashSet<>();
                    if (projectSessions == null) {
                        continue;
                    }
                    for (JSONObject session : projectSessions) {
                        String preview = cliSessionPreview(session);
                        if (preview.isEmpty()) {
                            continue;
                        }
                        String sessionKey = session.optString("sessionId", session.optString("id", "")).trim();
                        String raw = preview.substring(0, Math.min(36, preview.length()));
                        String option = uniqueSessionOptionLabel(
                                cliRecentAlias(client, title, raw),
                                session.optLong("updatedAt", 0),
                                sessionKey,
                                seenOptions);
                        seenOptions.add(option);
                        options.add(option);
                        if (!sessionKey.isEmpty()) {
                            String key = title + "\n" + option;
                            sessionIdByOption.put(key, sessionKey);
                            sessionByOption.put(key, session);
                        }
                    }
                    options.sort((a, b) -> {
                        boolean ap = pinned.contains(title + "\n" + a);
                        boolean bp = pinned.contains(title + "\n" + b);
                        if (ap != bp) {
                            return ap ? -1 : 1;
                        }
                        return 0;
                    });
                    grouped.put(title, options.isEmpty() ? list("暂无会话记录") : options);
                }
            }
            if (tabs.isEmpty()) {
                String title = label(client);
                tabs.add(title);
                grouped.put(title, list("暂无会话记录"));
            }
            ui(() -> showStackedGroupedChoice(label(client) + " 会话记录", tabs, grouped, selectedCliSession, value -> {
                selectedCliSession = value.trim();
                if (!"暂无会话记录".equals(selectedCliSession)) {
                    for (String tab : tabs) {
                        String key = tab + "\n" + selectedCliSession;
                        String id = sessionIdByOption.get(key);
                        if (id != null && !id.trim().isEmpty()) {
                            selectedCliSessionIds.put(client, id.trim());
                            prefs.edit().putString("cli_active_session_id_" + client, id.trim()).apply();
                            timelineVisibleItemCounts.put(client, INITIAL_RENDER_ITEM_COUNT);
                            JSONObject chosen = sessionByOption.get(key);
                            if (chosen != null) {
                                updateSelectedCliProject(client, chosen);
                            }
                            break;
                        }
                    }
                    renderSection();
                    pollSessionsOnce(true);
                }
            }, null));
        });
    }

    private void showRecentSessionsFromArray(String client, JSONArray sessions) {
        Map<String, List<String>> grouped = new HashMap<>();
        List<String> tabs = new ArrayList<>();
        Map<String, String> sessionIdByOption = new HashMap<>();
        Map<String, JSONObject> sessionByOption = new HashMap<>();
        if (sessions != null) {
            List<JSONObject> cliSessions = new ArrayList<>();
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.optJSONObject(i);
                if (session == null || !client.equals(session.optString("client"))) {
                    continue;
                }
                cliSessions.add(session);
            }
            cliSessions.sort((a, b) -> Long.compare(b.optLong("updatedAt"), a.optLong("updatedAt")));
            Map<String, List<JSONObject>> byProject = new LinkedHashMap<>();
            List<String> projectKeys = new ArrayList<>();
            Set<String> seenSessions = new HashSet<>();
            for (JSONObject session : cliSessions) {
                String sessionKey = session.optString("sessionId", session.optString("id", "")).trim();
                if (sessionKey.isEmpty()) {
                    sessionKey = session.optString("id", "").trim();
                }
                if (!sessionKey.isEmpty() && !seenSessions.add(sessionKey)) {
                    continue;
                }
                String title = cliSessionProjectTitle(session, client);
                String projectKey = cliSessionProjectKey(session, client);
                List<JSONObject> bucket = byProject.get(projectKey);
                if (bucket == null) {
                    if (byProject.size() >= 3) {
                        continue;
                    }
                    bucket = new ArrayList<>();
                    byProject.put(projectKey, bucket);
                    projectKeys.add(projectKey);
                    tabs.add(title);
                }
                if (bucket.size() < 5) {
                    bucket.add(session);
                }
            }
            Set<String> pinned = pinnedRecentSessions(client);
            for (int projectIndex = 0; projectIndex < tabs.size(); projectIndex++) {
                String title = tabs.get(projectIndex);
                String projectKey = projectKeys.get(projectIndex);
                List<JSONObject> projectSessions = byProject.get(projectKey);
                List<String> options = new ArrayList<>();
                Set<String> seenOptions = new HashSet<>();
                if (projectSessions == null) {
                    continue;
                }
                for (JSONObject session : projectSessions) {
                    String preview = cliSessionPreview(session);
                    if (preview.isEmpty()) {
                        continue;
                    }
                    String sessionKey = session.optString("sessionId", session.optString("id", "")).trim();
                    String raw = preview.substring(0, Math.min(36, preview.length()));
                    String option = uniqueSessionOptionLabel(
                            cliRecentAlias(client, title, raw),
                            session.optLong("updatedAt", 0),
                            sessionKey,
                            seenOptions);
                    seenOptions.add(option);
                    options.add(option);
                    if (!sessionKey.isEmpty()) {
                        String key = title + "\n" + option;
                        sessionIdByOption.put(key, sessionKey);
                        sessionByOption.put(key, session);
                    }
                }
                options.sort((a, b) -> {
                    boolean ap = pinned.contains(title + "\n" + a);
                    boolean bp = pinned.contains(title + "\n" + b);
                    if (ap != bp) {
                        return ap ? -1 : 1;
                    }
                    return 0;
                });
                grouped.put(title, options.isEmpty() ? list("暂无会话记录") : options);
            }
        }
        if (tabs.isEmpty()) {
            String title = label(client);
            tabs.add(title);
            grouped.put(title, list("暂无会话记录"));
        }
        showStackedGroupedChoice(label(client) + " 会话记录", tabs, grouped, selectedCliSession, value -> {
            selectedCliSession = value.trim();
            if (!"暂无会话记录".equals(selectedCliSession)) {
                for (String tab : tabs) {
                    String key = tab + "\n" + selectedCliSession;
                    String id = sessionIdByOption.get(key);
                    if (id != null && !id.trim().isEmpty()) {
                        selectedCliSessionIds.put(client, id.trim());
                        prefs.edit().putString("cli_active_session_id_" + client, id.trim()).apply();
                        timelineVisibleItemCounts.put(client, INITIAL_RENDER_ITEM_COUNT);
                        JSONObject chosen = sessionByOption.get(key);
                        if (chosen != null) {
                            updateSelectedCliProject(client, chosen);
                            try {
                                JSONArray selectedEvents = mergedEventsForClient(cachedDesktopSessions, client);
                                cliTimelineCache.put(client, selectedEvents);
                                cliTimelineSignatures.put(client, "");
                            } catch (Exception ignored) {
                            }
                        }
                        break;
                    }
                }
                renderSection();
                pollSessionsOnce(true);
            }
        }, null);
    }

    private String cliSessionProjectKey(JSONObject session, String client) {
        String path = cleanDisplayText(session.optString("projectPath", "")).replace("\\", "/").toLowerCase(Locale.ROOT);
        if (!path.isEmpty()) {
            return path;
        }
        return client + ":" + cliSessionProjectTitle(session, client).toLowerCase(Locale.ROOT);
    }

    private void updateSelectedCliProject(String client, JSONObject session) {
        String projectName = cliSessionProjectTitle(session, client);
        String projectPath = cleanDisplayText(session.optString("projectPath", ""));
        selectedCliProjectNames.put(client, projectName);
        selectedCliProjectPaths.put(client, projectPath);
        if (client.equals(section) && cliProjectTagView != null) {
            cliProjectTagView.setText("项目：" + ellipsizeLabel(projectName, 8));
        }
    }

    private String cliSessionPreview(JSONObject session) {
        String preview = cleanDisplayText(session.optString("preview", ""));
        if (!preview.isEmpty()) {
            return preview;
        }
        JSONArray messages = session.optJSONArray("messages");
        if (messages != null) {
            for (int j = messages.length() - 1; j >= 0; j--) {
                JSONObject msg = messages.optJSONObject(j);
                String text = msg == null ? "" : cleanDisplayText(msg.optString("text", ""));
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private String cliSessionProjectTitle(JSONObject session, String client) {
        String projectName = cleanDisplayText(session.optString("projectName", ""));
        if (!projectName.isEmpty()) {
            return projectName;
        }
        String projectPath = cleanDisplayText(session.optString("projectPath", ""));
        String fromPath = fileNameFromPath(projectPath);
        if (!fromPath.isEmpty()) {
            return fromPath;
        }
        String title = cleanDisplayText(session.optString("title", ""));
        if (!title.isEmpty() && !title.equals(label(client) + " 远程会话")) {
            return title;
        }
        return "本机项目";
    }

    private String fileNameFromPath(String path) {
        String clean = cleanDisplayText(path).replace("\\", "/");
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        int index = clean.lastIndexOf('/');
        return index >= 0 ? clean.substring(index + 1).trim() : clean.trim();
    }

    private void showLocalRecentSessions(String mode) {
        Map<String, List<String>> grouped = new HashMap<>();
        List<String> tabs = new ArrayList<>();
        Map<String, String> sessionIdByTitle = new HashMap<>();
        try {
            JSONObject root = localConversationStore(mode);
            JSONArray sessions = root.optJSONArray("sessions");
            List<JSONObject> rows = new ArrayList<>();
            if (sessions != null) {
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject session = sessions.optJSONObject(i);
                    if (session != null && hasSessionContent(session)) {
                        rows.add(session);
                    }
                }
            }
            Set<String> pinned = pinnedRecentSessions(mode);
            rows.sort((a, b) -> {
                boolean ap = pinned.contains(a.optString("id"));
                boolean bp = pinned.contains(b.optString("id"));
                if (ap != bp) {
                    return ap ? -1 : 1;
                }
                return Long.compare(b.optLong("updatedAt"), a.optLong("updatedAt"));
            });
            for (JSONObject session : rows) {
                String group = session.optString("group", "默认助手");
                List<String> values = grouped.get(group);
                if (values == null) {
                    values = new ArrayList<>();
                    grouped.put(group, values);
                    tabs.add(group);
                }
                Set<String> seen = new HashSet<>(values);
                String title = uniqueSessionOptionLabel(
                        session.optString("title", "未命名会话"),
                        session.optLong("updatedAt", 0),
                        session.optString("id", ""),
                        seen);
                values.add(title);
                sessionIdByTitle.put(group + "\n" + title, session.optString("id"));
            }
        } catch (Exception ignored) {
        }
        if (tabs.isEmpty()) {
            String fallback = "chat".equals(mode) ? selectedChatAssistant : selectedImageAssistant;
            grouped.put(fallback, list("暂无会话记录"));
            tabs.add(fallback);
        }
        showStackedGroupedChoice(label(mode) + " 会话记录", tabs, grouped, "", value -> {
            if (!"暂无会话记录".equals(value)) {
                String selectedId = "";
                String selectedGroup = "";
                for (String tab : tabs) {
                    selectedId = sessionIdByTitle.get(tab + "\n" + value);
                    if (selectedId != null && !selectedId.isEmpty()) {
                        selectedGroup = tab;
                        break;
                    }
                }
                if (selectedId != null && !selectedId.isEmpty()) {
                    applyLocalSessionGroup(mode, selectedGroup);
                    setActiveLocalSessionId(mode, selectedId);
                    renderSection();
                }
            }
        }, () -> {
            createLocalConversation(mode);
            renderSection();
        });
    }

    private boolean hasSessionContent(JSONObject session) {
        JSONArray messages = session == null ? null : session.optJSONArray("messages");
        if (messages == null || messages.length() == 0) {
            return false;
        }
        for (int i = 0; i < messages.length(); i++) {
            JSONObject item = messages.optJSONObject(i);
            if (item != null && !cleanDisplayText(item.optString("text", "")).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void applyLocalSessionGroup(String mode, String group) {
        String clean = cleanDisplayText(group);
        if (clean.isEmpty() || "暂无会话记录".equals(clean)) {
            return;
        }
        SharedPreferences.Editor edit = prefs.edit();
        if ("image".equals(mode)) {
            selectedImageAssistant = clean;
            edit.putString("selected_image_assistant", clean);
        } else {
            selectedChatAssistant = clean;
            edit.putString("selected_chat_assistant", clean);
        }
        edit.apply();
    }

    private String uniqueSessionOptionLabel(String title, long updatedAt, String id, Set<String> used) {
        String base = cleanDisplayText(title);
        if (base.isEmpty()) {
            base = "未命名会话";
        }
        if (used == null) {
            used = new HashSet<>();
        }
        String label = base;
        if (used.contains(label)) {
            String time = updatedAt > 0 ? formatDateTime(updatedAt) : "";
            String suffix = time.isEmpty() ? shortId(id) : time;
            label = suffix.isEmpty() ? base : base + " · " + suffix;
        }
        if (used.contains(label)) {
            String suffix = shortId(id);
            if (!suffix.isEmpty()) {
                label = label + " · " + suffix;
            }
        }
        int suffix = 2;
        String candidate = label;
        while (used.contains(candidate)) {
            candidate = label + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    private void saveLocalRecent(String mode, String group, String prompt) {
        String key = mode + "_recent_sessions";
        String safeGroup = group == null || group.trim().isEmpty() ? "默认助手" : group.trim();
        try {
            JSONObject root = new JSONObject(prefs.getString(key, "{}"));
            JSONArray rows = root.optJSONArray(safeGroup);
            if (rows == null) {
                rows = new JSONArray();
            }
            JSONArray next = new JSONArray();
            next.put(prompt);
            for (int i = 0; i < rows.length() && next.length() < 8; i++) {
                String item = rows.optString(i);
                if (!item.equals(prompt)) {
                    next.put(item);
                }
            }
            root.put(safeGroup, next);
            prefs.edit().putString(key, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void showExtensionsChoice(String client) {
        runNetwork(() -> {
            String query = boundDeviceId.isEmpty() ? "" : "?device_id=" + enc(boundDeviceId);
            JSONObject envelope = api("GET", "/api/mobile/desktop-extensions" + query, null);
            JSONArray data = envelope.optJSONArray("data");
            Map<String, List<String>> grouped = new HashMap<>();
            grouped.put("Skill", new ArrayList<>());
            grouped.put("Plugin", new ArrayList<>());
            grouped.put("Command", new ArrayList<>());
            Map<String, String> kindByName = new HashMap<>();
            int skillCount = appendExtensions(grouped.get("Skill"), data, "skill", client);
            int pluginCount = appendExtensions(grouped.get("Plugin"), data, "plugin", client);
            int commandCount = appendExtensions(grouped.get("Command"), data, "command", client);
            for (String name : grouped.get("Skill")) kindByName.put(name, "skill");
            for (String name : grouped.get("Plugin")) kindByName.put(name, "plugin");
            for (String name : grouped.get("Command")) kindByName.put(name, "command");
            if (commandCount == 0) {
                for (String command : list("/resume", "/compact", "/plan")) {
                    grouped.get("Command").add(command);
                    kindByName.put(command, "command");
                }
            }
            if (skillCount == 0 && pluginCount == 0 && commandCount == 0) {
                grouped.get("Command").add("默认");
                kindByName.put("默认", "command");
            }
            List<String> tabs = new ArrayList<>();
            for (String tab : list("Skill", "Plugin", "Command")) {
                List<String> values = grouped.get(tab);
                if (values != null && !values.isEmpty()) {
                    tabs.add(tab);
                }
            }
            ui(() -> showGroupedChoice("Skill / Plugin / Command", tabs, grouped, selectedSkillPlugin, value -> {
                selectedSkillPlugin = value;
                addExtensionRef(kindByName.getOrDefault(value, value.startsWith("/") ? "command" : "skill"), value);
            }));
        });
    }

    private int appendExtensions(List<String> out, JSONArray data, String kind, String client) {
        if (data == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null || !kind.equals(item.optString("kind"))) {
                continue;
            }
            String owner = item.optString("client", item.optString("owner", ""));
            if (!owner.isEmpty() && !"shared".equals(owner) && !"command".equals(owner) && !client.equals(owner)) {
                continue;
            }
            String name = item.optString("name", "");
            String label = name;
            if (!name.isEmpty() && !out.contains(label)) {
                out.add(label);
                count++;
            }
        }
        return count;
    }

    private String extensionKindLabel(String kind) {
        if ("plugin".equals(kind)) return "Plugin";
        if ("command".equals(kind)) return "Command";
        return "Skill";
    }

    private View statusCard(String name, String value) {
        LinearLayout c = cardPanel();
        LinearLayout row = horizontal();
        row.addView(bold(name), new LinearLayout.LayoutParams(0, -2, 1));
        TextView v = copy(value);
        v.setTextColor(MINT);
        v.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(v, new LinearLayout.LayoutParams(dp(120), -2));
        c.addView(row);
        return c;
    }

    private void showStackedGroupedChoice(String title, List<String> groups, Map<String, List<String>> grouped, String current, ChoiceHandler handler, Runnable newAction) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(64, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold(title), new LinearLayout.LayoutParams(0, -2, 1));
        if (newAction != null) {
            Button add = iconButton("+");
            add.setContentDescription("新建会话");
            add.setOnClickListener(v -> {
                newAction.run();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            addLp.setMargins(0, 0, dp(8), 0);
            titleRow.addView(add, addLp);
        }
        Button close = iconButton("×");
        close.setOnClickListener(v -> dialog.dismiss());
        String cliClient = recentDialogClient(title);
        if (!cliClient.isEmpty()) {
            Button refresh = iconButton("↻");
            refresh.setContentDescription("刷新会话记录");
            refresh.setOnClickListener(v -> {
                dialog.dismiss();
                showRecentSessions(cliClient);
                if (cliClient.equals(section)) {
                    pollSessionsOnce(true);
                }
            });
            LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            refreshLp.setMargins(0, 0, dp(8), 0);
            titleRow.addView(refresh, refreshLp);
        }
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        panel.addView(gap(8));

        ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout list = vertical();
        for (String group : groups == null ? new ArrayList<String>() : groups) {
            TextView header = bold(group);
            header.setTextColor(MUTED);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
            headerLp.setMargins(0, dp(10), 0, dp(2));
            list.addView(header, headerLp);
            List<String> values = grouped == null ? null : grouped.get(group);
            if (values == null || values.isEmpty()) {
                list.addView(copy("暂无会话记录"));
                continue;
            }
            for (String option : values) {
                Button entry = drawerButton(option.equals(current) ? option + "  ✓" : option);
                if (option.equals(current)) {
                    entry.setTextColor(BLUE);
                    entry.setBackground(round(Color.argb(72, 54, 104, 240), dp(16), Color.argb(110, 54, 104, 240)));
                }
                entry.setOnClickListener(v -> {
                    handler.onChoice(option);
                    dialog.dismiss();
                });
                entry.setOnLongClickListener(v -> {
                    showRecentSessionActions(title, group, option);
                    return true;
                });
                list.addView(entry);
            }
        }
        if (list.getChildCount() == 0) {
            list.addView(copy("暂无会话记录"));
        }
        scroller.addView(list);
        panel.addView(scroller, new LinearLayout.LayoutParams(-1, dp(360)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(312), -2);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(dp(18), 0, dp(18), dp(28));
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void showRecentSessionActions(String title, String group, String option) {
        if (option == null || option.equals("暂无会话记录")) {
            return;
        }
        String mode = title.startsWith("Image") ? "image" : title.startsWith("Chat") ? "chat" : recentDialogClient(title);
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (!mode.isEmpty()) {
            labels.add("重命名");
            actions.add(() -> {
                if ("chat".equals(mode) || "image".equals(mode)) {
                    showRenameLocalSession(mode, group, option);
                } else {
                    showRenameCliRecent(mode, group, option);
                }
            });
            labels.add("置顶");
            actions.add(() -> togglePinRecent(mode, group, option));
            if ("chat".equals(mode) || "image".equals(mode)) {
                labels.add("删除");
                actions.add(() -> deleteLocalSessionByTitle(mode, group, option));
                labels.add("分享");
                actions.add(() -> shareText(localSessionShareText(mode, group, option)));
            }
            showLongPressMenu(labels, actions);
            return;
        }
        showLongPressMenu(list("分享"), listRunnable(() -> shareText(option)));
    }

    private String recentDialogClient(String title) {
        if (title == null) {
            return "";
        }
        if (title.startsWith("Codex")) return "codex";
        if (title.startsWith("Claude")) return "claude";
        return "";
    }

    private Button textAction(String text, Runnable action) {
        Button b = ghost(text);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showLongPressMenu(List<String> labels, List<Runnable> actions) {
        if (labels == null || labels.isEmpty()) {
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = vertical();
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(20), LINE));
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            Runnable action = actions != null && i < actions.size() ? actions.get(i) : () -> { };
            Button button = ghost(label);
            button.setTextSize(15);
            button.setGravity(Gravity.CENTER);
            button.setMinWidth(dp(82));
            button.setMinimumWidth(dp(82));
            button.setPadding(dp(16), 0, dp(16), 0);
            button.setOnClickListener(v -> {
                dialog.dismiss();
                action.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(38));
            lp.setMargins(0, i == 0 ? 0 : dp(6), 0, 0);
            panel.addView(button, lp);
        }
        dialog.setContentView(panel);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private List<Runnable> listRunnable(Runnable... actions) {
        List<Runnable> out = new ArrayList<>();
        Collections.addAll(out, actions);
        return out;
    }

    private Set<String> pinnedRecentSessions(String mode) {
        Set<String> out = new HashSet<>();
        String raw = prefs.getString(mode + "_pinned_sessions", "");
        for (String item : raw.split("\\n")) {
            String clean = item.trim();
            if (!clean.isEmpty()) {
                out.add(clean);
            }
        }
        return out;
    }

    private void savePinnedRecentSessions(String mode, Set<String> values) {
        StringBuilder raw = new StringBuilder();
        for (String item : values) {
            if (raw.length() > 0) raw.append('\n');
            raw.append(item);
        }
        prefs.edit().putString(mode + "_pinned_sessions", raw.toString()).apply();
    }

    private JSONObject findLocalSessionByTitle(String mode, String group, String title) {
        try {
            JSONArray sessions = localConversationStore(mode).optJSONArray("sessions");
            if (sessions == null) {
                return null;
            }
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.optJSONObject(i);
                if (session != null
                        && group.equals(session.optString("group"))
                        && title.equals(session.optString("title", "未命名会话"))) {
                    return session;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void showRenameLocalSession(String mode, String group, String title) {
        JSONObject target = findLocalSessionByTitle(mode, group, title);
        if (target == null) {
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = vertical();
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.addView(bold("重命名"));
        EditText edit = outlinedInput("会话名称");
        edit.setText(title);
        panel.addView(edit);
        Button ok = primary("保存");
        ok.setOnClickListener(v -> {
            renameLocalSession(mode, target.optString("id"), trim(edit));
            dialog.dismiss();
        });
        panel.addView(ok, compactFullButtonLp());
        dialog.setContentView(panel);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(dp(300), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void renameLocalSession(String mode, String sessionId, String nextTitle) {
        String clean = cleanDisplayText(nextTitle);
        if (clean.isEmpty()) {
            return;
        }
        try {
            JSONObject root = localConversationStore(mode);
            JSONObject session = findLocalSession(root, sessionId);
            if (session != null) {
                session.put("title", cleanSessionTitle(clean));
                session.put("updatedAt", System.currentTimeMillis());
                saveLocalConversationStore(mode, root);
                renderSection();
            }
        } catch (Exception ignored) {
        }
    }

    private void togglePinLocalSession(String mode, String group, String title) {
        JSONObject session = findLocalSessionByTitle(mode, group, title);
        if (session == null) {
            return;
        }
        Set<String> pinned = pinnedRecentSessions(mode);
        String id = session.optString("id");
        if (pinned.contains(id)) {
            pinned.remove(id);
        } else {
            pinned.add(id);
        }
        savePinnedRecentSessions(mode, pinned);
    }

    private void togglePinRecent(String mode, String group, String title) {
        if ("chat".equals(mode) || "image".equals(mode)) {
            togglePinLocalSession(mode, group, title);
            return;
        }
        Set<String> pinned = pinnedRecentSessions(mode);
        String key = group + "\n" + title;
        if (pinned.contains(key)) {
            pinned.remove(key);
        } else {
            pinned.add(key);
        }
        savePinnedRecentSessions(mode, pinned);
    }

    private String cliRecentAlias(String client, String group, String preview) {
        String key = "cli_recent_alias_" + client + "_" + Integer.toHexString((group + "\n" + preview).hashCode());
        return prefs.getString(key, preview);
    }

    private void showRenameCliRecent(String client, String group, String title) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = vertical();
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.addView(bold("重命名"));
        EditText edit = outlinedInput("会话名称");
        edit.setText(title);
        panel.addView(edit);
        Button ok = primary("保存");
        ok.setOnClickListener(v -> {
            String key = "cli_recent_alias_" + client + "_" + Integer.toHexString((group + "\n" + title).hashCode());
            String clean = cleanDisplayText(trim(edit));
            if (!clean.isEmpty()) {
                prefs.edit().putString(key, clean).apply();
            }
            dialog.dismiss();
            showRecentSessions(client);
        });
        panel.addView(ok, compactFullButtonLp());
        dialog.setContentView(panel);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(dp(300), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void deleteLocalSessionByTitle(String mode, String group, String title) {
        try {
            JSONObject root = localConversationStore(mode);
            JSONArray sessions = root.optJSONArray("sessions");
            if (sessions == null) {
                return;
            }
            JSONArray next = new JSONArray();
            String removedId = "";
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.optJSONObject(i);
                boolean remove = session != null
                        && group.equals(session.optString("group"))
                        && title.equals(session.optString("title", "未命名会话"));
                if (remove) {
                    removedId = session.optString("id");
                } else if (session != null) {
                    next.put(session);
                }
            }
            root.put("sessions", next);
            if (removedId.equals(activeLocalSessionId(mode))) {
                setActiveLocalSessionId(mode, "");
            }
            saveLocalConversationStore(mode, root);
            renderSection();
        } catch (Exception ignored) {
        }
    }

    private String localSessionShareText(String mode, String group, String title) {
        JSONObject session = findLocalSessionByTitle(mode, group, title);
        if (session == null) {
            return title;
        }
        StringBuilder out = new StringBuilder(title);
        JSONArray messages = session.optJSONArray("messages");
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                JSONObject item = messages.optJSONObject(i);
                if (item == null) continue;
                out.append("\n\n").append("user".equals(item.optString("role")) ? "用户：" : "AI：")
                        .append(cleanDisplayText(item.optString("text")));
            }
        }
        return out.toString();
    }

    private void showGroupedChoice(String title, List<String> tabs, Map<String, List<String>> grouped, String current, ChoiceHandler handler) {
        showGroupedChoice(title, tabs, grouped, current, handler, null);
    }

    private boolean favoriteChoiceEnabled(String title) {
        String t = title == null ? "" : title;
        return t.contains("助手") || t.contains("模型选择") || t.contains("AI 模型") || t.contains("Skill") || t.contains("Plugin");
    }

    private String favoriteChoiceKey(String title) {
        String normalized = (title == null ? "choice" : title).replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]+", "_");
        return "favorite_choice_" + normalized;
    }

    private Set<String> favoriteChoices(String title) {
        Set<String> out = new HashSet<>();
        String raw = prefs.getString(favoriteChoiceKey(title), "");
        for (String item : raw.split("\\n")) {
            String clean = item.trim();
            if (!clean.isEmpty()) {
                out.add(clean);
            }
        }
        return out;
    }

    private void saveFavoriteChoices(String title, Set<String> values) {
        StringBuilder raw = new StringBuilder();
        for (String item : values) {
            if (raw.length() > 0) {
                raw.append('\n');
            }
            raw.append(item);
        }
        prefs.edit().putString(favoriteChoiceKey(title), raw.toString()).apply();
    }

    private List<String> sortFavoriteOptions(String title, List<String> options) {
        List<String> out = new ArrayList<>();
        if (options != null) {
            out.addAll(options);
        }
        if (!favoriteChoiceEnabled(title)) {
            return out;
        }
        Set<String> favorites = favoriteChoices(title);
        out.sort((left, right) -> {
            boolean lf = favorites.contains(left);
            boolean rf = favorites.contains(right);
            if (lf != rf) {
                return lf ? -1 : 1;
            }
            return 0;
        });
        return out;
    }

    private View choiceOptionView(String title, String option, String current, Runnable onFavoriteChanged, Runnable onChoose) {
        boolean selected = option.equals(current);
        boolean header = option.startsWith("【") && option.endsWith("】");
        if (!favoriteChoiceEnabled(title) || header) {
            Button entry = drawerButton(selected ? option + "  ✓" : option);
            if (header) {
                entry.setEnabled(false);
                entry.setTextColor(BLUE);
                entry.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
            if (selected) {
                entry.setTextColor(BLUE);
                entry.setBackground(round(Color.argb(72, 54, 104, 240), dp(16), Color.argb(110, 54, 104, 240)));
            }
            entry.setOnClickListener(v -> onChoose.run());
            if (title.contains("助手") && !header && !"暂无可选项".equals(option)) {
                entry.setOnLongClickListener(v -> {
                    showAssistantActions(option);
                    return true;
                });
            }
            return entry;
        }

        Set<String> favorites = favoriteChoices(title);
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(round(selected ? Color.argb(72, 54, 104, 240) : Color.argb(120, 255, 255, 255), dp(16), selected ? Color.argb(110, 54, 104, 240) : LINE));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(42));
        rowLp.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(rowLp);
        Button pick = ghost(selected ? option + "  ✓" : option);
        pick.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        pick.setTextColor(selected ? BLUE : INK);
        pick.setBackground(new ColorDrawable(Color.TRANSPARENT));
        pick.setOnClickListener(v -> onChoose.run());
        if (title.contains("助手") && !"暂无可选项".equals(option)) {
            row.setOnLongClickListener(v -> {
                showAssistantActions(option);
                return true;
            });
            pick.setOnLongClickListener(v -> {
                showAssistantActions(option);
                return true;
            });
        }
        row.addView(pick, new LinearLayout.LayoutParams(0, -1, 1));
        Button star = iconButton(favorites.contains(option) ? "★" : "☆");
        star.setTextColor(favorites.contains(option) ? Color.rgb(224, 157, 42) : MUTED);
        star.setContentDescription(favorites.contains(option) ? "取消收藏" : "收藏");
        star.setOnClickListener(v -> {
            Set<String> next = favoriteChoices(title);
            if (next.contains(option)) {
                next.remove(option);
            } else {
                next.add(option);
            }
            saveFavoriteChoices(title, next);
            onFavoriteChanged.run();
        });
        row.addView(star, new LinearLayout.LayoutParams(dp(38), dp(38)));
        return row;
    }

    private void showAssistantActions(String option) {
        String scope = "image".equals(activeComposerMode) ? "image" : "chat";
        showLongPressMenu(
                list("分享", "编辑", "删除"),
                listRunnable(
                        () -> shareText(assistantShareText(scope, option)),
                        () -> showAssistantEditDialog(scope, option),
                        () -> deleteCustomAssistant(scope, option)
                )
        );
    }

    private String assistantEditorPrompt(String scope, String option) {
        String custom = customAssistantPrompt(scope, option);
        if (!custom.trim().isEmpty()) {
            return custom;
        }
        if ("image".equals(scope)) {
            return imageAssistantPrompt(option, "")
                    .replace("\n\n用户图片需求：", "")
                    .replace("用户图片需求：", "")
                    .trim();
        }
        return assistantPrompt(option);
    }

    private void showAssistantEditDialog(String scope, String option) {
        boolean create = cleanDisplayText(option).isEmpty();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = vertical();
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.addView(bold(create ? "新建助手" : "编辑助手"));
        panel.addView(gap(8));
        EditText name = outlinedInput("助手名称");
        if (!create) {
            name.setText(option);
        }
        panel.addView(name);
        panel.addView(gap(8));
        EditText prompt = outlinedInput("助手提示词");
        prompt.setMinLines(4);
        prompt.setMaxLines(8);
        if (!create) {
            prompt.setText(assistantEditorPrompt(scope, option));
        }
        panel.addView(prompt);
        Button save = primary("保存");
        save.setOnClickListener(v -> {
            saveCustomAssistant(scope, create ? "" : option, trim(name), prompt.getText().toString());
            dialog.dismiss();
        });
        panel.addView(save, compactFullButtonLp());
        dialog.setContentView(panel);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showGroupedChoice(String title, List<String> tabs, Map<String, List<String>> grouped, String current, ChoiceHandler handler, Runnable newAction) {
        if (tabs == null || tabs.isEmpty()) {
            showChoice(title, list("暂无可选项"), current, value -> { });
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(64, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold(title), new LinearLayout.LayoutParams(0, -2, 1));
        if (newAction != null) {
            Button add = iconButton("+");
            add.setContentDescription("新建会话");
            add.setOnClickListener(v -> {
                newAction.run();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            addLp.setMargins(0, 0, dp(8), 0);
            titleRow.addView(add, addLp);
        }
        Button close = iconButton("×");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);

        LinearLayout tabPanel = vertical();
        tabPanel.setPadding(0, dp(8), 0, dp(8));
        LinearLayout optionsBox = vertical();
        ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.addView(optionsBox);
        String initialTab = tabs.get(0);
        for (String tab : tabs) {
            List<String> values = grouped.get(tab);
            if (values != null && values.contains(current)) {
                initialTab = tab;
                break;
            }
        }
        final String[] activeTab = {initialTab};
        final List<Button> tabButtons = new ArrayList<>();
        final Runnable[] renderOptions = new Runnable[1];
        renderOptions[0] = () -> {
            optionsBox.removeAllViews();
            for (Button button : tabButtons) {
                boolean active = activeTab[0].equals(String.valueOf(button.getTag()));
                button.setTextColor(active ? Color.WHITE : INK);
                button.setBackground(round(active ? BLUE : Color.argb(120, 255, 255, 255), dp(16), active ? BLUE : LINE));
            }
            List<String> values = sortFavoriteOptions(title, grouped.get(activeTab[0]));
            if (values == null || values.isEmpty()) {
                optionsBox.addView(copy("暂无可选项"));
                return;
            }
            for (String option : values) {
                View entryView = choiceOptionView(title, option, current, () -> renderOptions[0].run(), () -> {
                    handler.onChoice(option);
                    dialog.dismiss();
                });
                if (!(entryView instanceof Button)) {
                    optionsBox.addView(entryView);
                    continue;
                }
                Button entry = (Button) entryView;
                if (option.equals(current)) {
                    entry.setTextColor(BLUE);
                    entry.setBackground(round(Color.argb(72, 54, 104, 240), dp(16), Color.argb(110, 54, 104, 240)));
                }
                entry.setOnClickListener(v -> {
                    handler.onChoice(option);
                    dialog.dismiss();
                });
                optionsBox.addView(entry);
            }
        };
        LinearLayout tabRow = null;
        for (int i = 0; i < tabs.size(); i++) {
            if (i % 3 == 0) {
                tabRow = horizontal();
                tabPanel.addView(tabRow, new LinearLayout.LayoutParams(-1, dp(36)));
            }
            String tab = tabs.get(i);
            Button button = ghost(tab);
            button.setSingleLine(true);
            button.setTag(tab);
            button.setTextSize(12);
            button.setOnClickListener(v -> {
                activeTab[0] = tab;
                renderOptions[0].run();
            });
            tabButtons.add(button);
            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, dp(34), 1);
            tabLp.setMargins(0, 0, dp(6), 0);
            tabRow.addView(button, tabLp);
        }
        panel.addView(tabPanel);
        renderOptions[0].run();
        panel.addView(scroller, new LinearLayout.LayoutParams(-1, dp(310)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(312), -2);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(dp(18), 0, dp(18), dp(28));
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void showChoice(String title, List<String> options, String current, ChoiceHandler handler) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(64, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold(title), new LinearLayout.LayoutParams(0, -2, 1));
        if ("助手".equals(title)) {
            Button add = iconButton("+");
            add.setContentDescription("新建助手");
            add.setOnClickListener(v -> {
                dialog.dismiss();
                showAssistantEditDialog("image".equals(activeComposerMode) ? "image" : "chat", "");
            });
            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            addLp.setMargins(0, 0, dp(8), 0);
            titleRow.addView(add, addLp);
        }
        Button close = iconButton("×");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        panel.addView(gap(8));
        ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout optionsBox = vertical();
        List<String> orderedOptions = sortFavoriteOptions(title, options);
        for (String option : orderedOptions) {
            View entry = choiceOptionView(title, option, current, () -> {
                dialog.dismiss();
                showChoice(title, options, current, handler);
            }, () -> {
                handler.onChoice(option);
                if (!option.startsWith("【")) {
                    dialog.dismiss();
                }
            });
            optionsBox.addView(entry);
        }
        scroller.addView(optionsBox);
        panel.addView(scroller, new LinearLayout.LayoutParams(-1, Math.min(dp(390), Math.max(dp(90), dp(50) * Math.min(options.size(), 8)))));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(294), -2);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(dp(18), 0, dp(18), dp(28));
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void showTextDialog(String title, String text) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(64, 15, 23, 42));
        LinearLayout panel = vertical();
        panel.setBackground(round(Color.rgb(248, 250, 255), dp(18), LINE));
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout titleRow = horizontal();
        titleRow.addView(bold(title), new LinearLayout.LayoutParams(0, -2, 1));
        Button close = iconButton("×");
        close.setContentDescription("关闭");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        TextView body = copy(text);
        body.setTextColor(INK);
        scroller.addView(body);
        LinearLayout.LayoutParams scrollerLp = new LinearLayout.LayoutParams(-1, dp(320));
        scrollerLp.setMargins(0, dp(10), 0, 0);
        panel.addView(scroller, scrollerLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(312), -2);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(dp(18), 0, dp(18), dp(28));
        overlay.addView(panel, lp);
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        dialog.setContentView(overlay);
        dialog.show();
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private TextView title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(INK);
        t.setTextSize(30);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView bold(String text) {
        TextView t = copy(text);
        t.setTextColor(INK);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(17);
        return t;
    }

    private TextView copy(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(MUTED);
        t.setTextSize(14);
        t.setLineSpacing(dp(3), 1.08f);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(false);
        e.setTextColor(INK);
        e.setHintTextColor(Color.rgb(130, 142, 162));
        e.setTextSize(13);
        e.setPadding(dp(8), dp(8), dp(8), dp(8));
        e.setBackground(new ColorDrawable(Color.TRANSPARENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 0);
        e.setLayoutParams(lp);
        return e;
    }

    private EditText outlinedInput(String hint) {
        EditText e = input(hint);
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        e.setBackground(round(Color.argb(225, 255, 255, 255), dp(12), LINE));
        return e;
    }

    private Spinner spinner(List<String> items) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        return s;
    }

    private View labelRow(String label, View value) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(10), 0, 0);
        row.addView(bold(label), new LinearLayout.LayoutParams(dp(70), -2));
        row.addView(value, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private Button primary(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(round(BLUE, dp(16), BLUE));
        return b;
    }

    private Button danger(String text) {
        Button b = primary(text);
        b.setBackground(round(Color.rgb(216, 71, 86), dp(16), Color.rgb(216, 71, 86)));
        return b;
    }

    private Button ghost(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(INK);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setBackground(round(Color.argb(120, 255, 255, 255), dp(16), LINE));
        return b;
    }

    private Button iconButton(String icon) {
        Button b = ghost(icon);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(0, 0, 0, dp(1));
        b.setBackground(new ColorDrawable(Color.TRANSPARENT));
        return b;
    }

    private Button navButton(String text, boolean active) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setTextColor(active ? Color.WHITE : INK);
        b.setBackground(round(active ? BLUE : Color.argb(150, 255, 255, 255), dp(19), active ? BLUE : LINE));
        return b;
    }

    private View navImageButton(int resId, boolean active) {
        LinearLayout wrapper = vertical();
        wrapper.setGravity(Gravity.CENTER);
        ImageButton b = new ImageButton(this);
        b.setImageResource(resId);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(8), dp(5), dp(8), dp(5));
        b.setBackground(new ColorDrawable(Color.TRANSPARENT));
        b.setClickable(false);
        b.setFocusable(false);
        wrapper.addView(b, new LinearLayout.LayoutParams(-1, 0, 1));
        View line = new View(this);
        line.setBackground(round(active ? BLUE : Color.TRANSPARENT, dp(2), Color.TRANSPARENT));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(24), dp(3));
        wrapper.addView(line, lineLp);
        return wrapper;
    }

    private Button drawerButton(String text) {
        Button b = ghost(text);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setTextSize(15);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.setMargins(0, dp(8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private View drawerEntry(String item) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(6), 0, dp(6), 0);
        row.setBackground(round(Color.argb(120, 255, 255, 255), dp(16), LINE));
        LinearLayout group = horizontal();
        group.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(this);
        icon.setImageResource(drawerIconRes(item));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(dp(5), dp(5), dp(5), dp(5));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(30), -1);
        iconLp.setMargins(0, 0, dp(8), 0);
        group.addView(icon, iconLp);
        TextView text = bold(label(item));
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setIncludeFontPadding(true);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        group.addView(text, new LinearLayout.LayoutParams(-2, -1));
        row.addView(group, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    private int drawerIconRes(String item) {
        if ("assistants".equals(item)) return R.drawable.drawer_aichat;
        if ("subscriptions".equals(item)) return R.drawable.drawer_shop;
        if ("service".equals(item)) return R.drawable.drawer_cloud;
        if ("wallet".equals(item)) return R.drawable.drawer_wallet;
        if ("settings".equals(item)) return R.drawable.drawer_setting;
        return R.drawable.nav_chat;
    }

    private GradientDrawable round(int fill, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke(dp(1), stroke);
        return g;
    }

    private View gap(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return v;
    }

    private List<String> list(String... values) {
        List<String> out = new ArrayList<>();
        Collections.addAll(out, values);
        return out;
    }

    private String label(String value) {
        switch (value) {
            case "chat": return "Chat";
            case "image": return "Image";
            case "codex": return "Codex";
            case "claude": return "Claude";
            case "assistants": return "AIChat";
            case "subscriptions": return "套餐订阅";
            case "service": return "服务状态";
            case "wallet": return "我的钱包";
            case "settings": return "系统设置";
            default: return value;
        }
    }

    private int navDrawable(String value) {
        switch (value) {
            case "chat": return R.drawable.nav_chat;
            case "image": return R.drawable.nav_image;
            case "codex": return R.drawable.nav_codex;
            case "claude": return R.drawable.nav_claude;
            default: return R.drawable.nav_chat;
        }
    }

    private String reasoningValue(String label) {
        if (label.contains("低")) return "low";
        if (label.contains("中")) return "medium";
        if (label.contains("高")) return "high";
        return "off";
    }

    private String formatPlainPrice(double value) {
        return String.format(Locale.CHINA, "%.2f", value);
    }

    private String formatQuotaAsUsd(double value, double quotaPerUnit) {
        double unit = quotaPerUnit > 0 ? quotaPerUnit : 500000;
        double usd = value / unit;
        return "$" + String.format(Locale.US, usd >= 1 ? "%.2f" : "%.4f", usd);
    }

    private String formatDuration(JSONObject plan) {
        String unit = plan.optString("duration_unit", "month");
        int value = Math.max(1, plan.optInt("duration_value", 1));
        switch (unit) {
            case "year": return value + " 年";
            case "day": return value + " 天";
            case "hour": return value + " 小时";
            case "custom": return Math.max(1, plan.optLong("custom_seconds", 0) / 86400) + " 天";
            default: return value + " 个月";
        }
    }

    private String resetLabel(String value) {
        switch (value) {
            case "daily": return "每日";
            case "weekly": return "每周";
            case "monthly": return "每月";
            case "custom": return "自定义";
            default: return "不重置";
        }
    }

    private String formatBillingLabel(JSONObject item) {
        String title = item.optString("plan_title", "").trim();
        if (!title.isEmpty()) {
            return title;
        }
        String trade = item.optString("trade_no", "").replaceAll("SUBWALLETUSR1NO[a-zA-Z0-9_-]*", "").trim();
        if (!trade.isEmpty()) {
            return trade;
        }
        String payment = item.optString("payment_method", "").replaceAll("(?i)^wallet$", "").trim();
        return payment.isEmpty() ? "购买记录" : payment;
    }

    private double resolveBillingUsageRatio(JSONObject bill, JSONArray plans, JSONObject subscriptions) {
        String title = bill == null ? "" : bill.optString("plan_title", "").trim();
        if (title.isEmpty() || plans == null || subscriptions == null) {
            return 0;
        }
        Map<Integer, String> titleByPlanId = new HashMap<>();
        for (int i = 0; i < plans.length(); i++) {
            JSONObject record = plans.optJSONObject(i);
            JSONObject plan = record == null ? null : record.optJSONObject("plan");
            if (plan == null) {
                continue;
            }
            String planTitle = plan.optString("title", "").trim();
            if (!planTitle.isEmpty()) {
                titleByPlanId.put(plan.optInt("id", 0), planTitle);
            }
        }
        JSONArray rows = subscriptions.optJSONArray("all_subscriptions");
        if (rows == null) {
            rows = subscriptions.optJSONArray("subscriptions");
        }
        if (rows == null) {
            return 0;
        }
        long bestUpdatedAt = -1;
        double ratio = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            JSONObject sub = row == null ? null : row.optJSONObject("subscription");
            if (sub == null) {
                continue;
            }
            String planTitle = titleByPlanId.get(sub.optInt("plan_id", 0));
            if (!title.equals(planTitle)) {
                continue;
            }
            long updatedAt = Math.max(sub.optLong("end_time", 0), Math.max(sub.optLong("start_time", 0), sub.optLong("id", 0)));
            if (updatedAt < bestUpdatedAt) {
                continue;
            }
            bestUpdatedAt = updatedAt;
            double total = sub.optDouble("amount_total", 0);
            double used = sub.optDouble("amount_used", 0);
            ratio = total > 0 ? Math.max(0, Math.min(1, used / total)) : 0;
        }
        return ratio;
    }

    private String billingStatus(String value) {
        if ("success".equals(value)) return "已完成";
        if ("pending".equals(value)) return "待支付";
        if ("expired".equals(value)) return "已过期";
        return "未知";
    }

    private String serviceToneLabel(String tone) {
        switch (tone) {
            case "up": return "运行正常";
            case "down": return "服务异常";
            case "maintenance": return "维护中";
            default: return "状态未知";
        }
    }

    private int serviceToneColor(String tone) {
        switch (tone) {
            case "up": return MINT;
            case "down": return Color.rgb(216, 71, 86);
            case "maintenance": return Color.rgb(214, 144, 44);
            default: return MUTED;
        }
    }

    private View serviceHistoryDots(JSONArray history) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(8), 0, dp(6));
        for (int i = 0; i < Math.min(history.length(), 24); i++) {
            JSONObject item = history.optJSONObject(i);
            View dot = new View(this);
            dot.setBackground(round(serviceToneColor(item == null ? "" : item.optString("tone")), dp(4), Color.TRANSPARENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(8), dp(8));
            lp.setMargins(0, 0, dp(5), 0);
            row.addView(dot, lp);
        }
        return row;
    }

    private void renderUsageDistribution(LinearLayout parent, JSONArray items) {
        if (items == null || items.length() == 0) {
            parent.addView(copy("暂无用量记录。"));
            return;
        }
        List<String> labels = new ArrayList<>();
        List<Double> quotas = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            double quota = Math.max(0, item.optDouble("quota", 0));
            if (quota <= 0) {
                continue;
            }
            total += quota;
            boolean merged = false;
            String model = item.optString("model_name", item.optString("token_name", "未知模型"));
            for (int j = 0; j < labels.size(); j++) {
                if (model.equals(labels.get(j))) {
                    quotas.set(j, quotas.get(j) + quota);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                labels.add(model);
                quotas.add(quota);
            }
        }
        if (labels.isEmpty() || total <= 0) {
            parent.addView(copy("暂无用量记录。"));
            return;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> Double.compare(quotas.get(b), quotas.get(a)));
        for (int i = 0; i < Math.min(order.size(), 5); i++) {
            int index = order.get(i);
            parent.addView(usageRow(labels.get(index), quotas.get(index) / total));
        }
    }

    private View usageRow(String label, double ratio) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(8), 0, 0);
        TextView text = copy(label);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(progressBar(ratio), new LinearLayout.LayoutParams(dp(132), dp(8)));
        return row;
    }

    private View billingRow(String label, double ratio) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(9), 0, 0);
        row.addView(copy(label), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(progressBar(ratio), new LinearLayout.LayoutParams(dp(132), dp(8)));
        return row;
    }

    private View progressBar(double ratio) {
        FrameLayout track = new FrameLayout(this);
        track.setBackground(round(Color.argb(78, 145, 160, 184), dp(4), Color.TRANSPARENT));
        View fill = new View(this);
        fill.setBackground(round(BLUE, dp(4), BLUE));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(dp(4), (int) (dp(132) * Math.max(0.04, Math.min(1, ratio)))), dp(8));
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        track.addView(fill, lp);
        return track;
    }

    private String formatDateTime(long timestamp) {
        long ms = timestamp > 10000000000L ? timestamp : timestamp * 1000L;
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return fmt.format(new java.util.Date(ms));
    }

    private String selected(Spinner spinner) {
        Object item = spinner == null ? null : spinner.getSelectedItem();
        return item == null ? "" : String.valueOf(item);
    }

    private String selectedCliModelFor(String client) {
        String value = "claude".equals(client) ? selectedClaudeModel : selectedCodexModel;
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        List<String> models = "claude".equals(client) ? claudeModels : codexModels;
        return models.isEmpty() ? "" : models.get(0);
    }

    private String trim(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String enc(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private String shortId(String id) {
        if (id == null || id.length() <= 8) {
            return id == null ? "" : id;
        }
        return id.substring(0, 8);
    }

    private void installKeyboardLift() {
        if (shell == null || composerHost == null) {
            return;
        }
        shell.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (shell == null || composerHost == null) {
                return;
            }
            Rect visible = new Rect();
            shell.getWindowVisibleDisplayFrame(visible);
            int rootHeight = shell.getRootView().getHeight();
            int keyboard = Math.max(0, rootHeight - visible.bottom);
            LinearLayout.LayoutParams lp = composerHost.getLayoutParams() instanceof LinearLayout.LayoutParams
                    ? (LinearLayout.LayoutParams) composerHost.getLayoutParams()
                    : null;
            if (keyboard > dp(120)) {
                composerHost.setTranslationY(0);
                composerHost.setPadding(0, 0, 0, dp(6));
                if (lp != null && lp.bottomMargin != keyboard) {
                    lp.bottomMargin = keyboard;
                    composerHost.setLayoutParams(lp);
                }
            } else {
                composerHost.setTranslationY(0);
                composerHost.setPadding(0, 0, 0, dp(12));
                if (lp != null && lp.bottomMargin != 0) {
                    lp.bottomMargin = 0;
                    composerHost.setLayoutParams(lp);
                }
            }
        });
    }

    private void installSwipeNavigation() {
        if (shell == null) {
            return;
        }
        View.OnTouchListener listener = (v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    if (!isTouchInsideActiveInput(event)) {
                        cancelComposerInputMode();
                    }
                    swipeStartX = event.getX();
                    swipeStartY = event.getY();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    handleHorizontalSwipe(event.getX() - swipeStartX, event.getY() - swipeStartY);
                    break;
                default:
                    break;
            }
            return false;
        };
        shell.setOnTouchListener(listener);
        if (contentScroll != null) {
            contentScroll.setOnTouchListener(listener);
        }
    }

    private void handleHorizontalSwipe(float dx, float dy) {
        if (System.currentTimeMillis() < suppressSwipeUntil) {
            return;
        }
        float absX = Math.abs(dx);
        float absY = Math.abs(dy);
        if (absX < dp(118) || absX < absY * 2.8f || absY > dp(72)) {
            return;
        }
        String[] items = new String[]{"chat", "image", "codex", "claude"};
        int current = -1;
        String effective = "assistants".equals(section) ? "chat" : section;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(effective)) {
                current = i;
                break;
            }
        }
        if (current < 0) {
            return;
        }
        int direction = dx < 0 ? 1 : -1;
        int next = current + direction;
        if (next >= 0 && next < items.length) {
            section = items[next];
            localAutoScrollEnabled = true;
            refreshBottomNav();
            renderSection();
            lastExitSwipeAt = 0;
            lastExitSwipeDirection = 0;
            return;
        }
        long now = System.currentTimeMillis();
        if (lastExitSwipeDirection == direction && now - lastExitSwipeAt < 1800L) {
            finish();
            return;
        }
        lastExitSwipeDirection = direction;
        lastExitSwipeAt = now;
        toast("再次滑动退出应用");
    }

    private void hideKeyboard(View view) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void focusComposerInput() {
        if (activeInput == null) {
            return;
        }
        cancelComposerKeyboardRequest();
        EditText target = activeInput;
        activeInput.setFocusableInTouchMode(true);
        activeInput.requestFocus();
        activeInput.setSelection(activeInput.getText().length());
        pendingComposerKeyboardRunnable = () -> {
            pendingComposerKeyboardRunnable = null;
            if (target == activeInput && target.hasFocus()) {
                showKeyboard(target);
            }
        };
        handler.postDelayed(pendingComposerKeyboardRunnable, 80);
    }

    private void cancelComposerInputMode() {
        cancelComposerKeyboardRequest();
        if (activeInput != null && activeInput.hasFocus()) {
            hideKeyboard(activeInput);
            activeInput.clearFocus();
        }
    }

    private void cancelComposerKeyboardRequest() {
        if (pendingComposerKeyboardRunnable != null) {
            handler.removeCallbacks(pendingComposerKeyboardRunnable);
            pendingComposerKeyboardRunnable = null;
        }
    }

    private boolean isTouchInsideActiveInput(android.view.MotionEvent event) {
        if (activeInput == null || event == null) {
            return false;
        }
        int[] location = new int[2];
        activeInput.getLocationOnScreen(location);
        Rect bounds = new Rect(location[0], location[1], location[0] + activeInput.getWidth(), location[1] + activeInput.getHeight());
        return bounds.contains((int) event.getRawX(), (int) event.getRawY());
    }

    private void showKeyboard(View view) {
        if (view == null || !view.hasFocus()) {
            return;
        }
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void ui(Runnable runnable) {
        handler.post(runnable);
    }

    private void resetContentScrollPosition() {
        if (contentScroll == null) {
            return;
        }
        contentScroll.scrollTo(0, 0);
        lastContentScrollY = 0;
    }

    private void scrollToBottom() {
        scrollToBottomStable(0);
    }

    private void scrollToBottomStable(int pass) {
        if (contentScroll == null) {
            return;
        }
        contentScroll.post(() -> {
            contentScroll.scrollTo(0, contentBottomScrollY(contentScroll));
            contentScroll.fullScroll(View.FOCUS_DOWN);
            updateScrollDock();
            if (pass < 4 && contentScroll != null) {
                handler.postDelayed(() -> scrollToBottomStable(pass + 1), pass == 0 ? 32L : 80L);
            }
        });
    }

    private void scrollToBottomIfLocalAuto() {
        if (!localAutoScrollEnabled) {
            return;
        }
        scrollToBottom();
    }

    private void updateScrollDock() {
        if (scrollDock == null || contentScroll == null || content == null) {
            return;
        }
        if (!isScrollableConversationSection() || contentBottomScrollY(contentScroll) <= dp(16)) {
            scrollDock.setVisibility(View.GONE);
            return;
        }
        scrollDock.removeAllViews();
        if (scrollDockDirectionDown) {
            scrollDock.addView(scrollDockButton(R.drawable.ic_scroll_bottom, "最新聊天记录", () -> {
                localAutoScrollEnabled = true;
                scrollToBottom();
            }));
            scrollDock.addView(scrollDockButton(R.drawable.ic_scroll_current_bottom, "当前气泡底部", () -> scrollCurrentContentChild(false)));
        } else {
            scrollDock.addView(scrollDockButton(R.drawable.ic_scroll_top, "聊天记录顶部", () -> {
                localAutoScrollEnabled = false;
                if (contentScroll != null) {
                    contentScroll.smoothScrollTo(0, 0);
                }
            }));
            scrollDock.addView(scrollDockButton(R.drawable.ic_scroll_current_top, "当前气泡顶部", () -> scrollCurrentContentChild(true)));
        }
        scrollDock.setVisibility(View.VISIBLE);
    }

    private boolean isScrollableConversationSection() {
        return "chat".equals(section)
                || "image".equals(section)
                || "assistants".equals(section)
                || "codex".equals(section)
                || "claude".equals(section);
    }

    private ImageButton scrollDockButton(int iconRes, String description, Runnable action) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(iconRes);
        b.setContentDescription(description);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(7), dp(7), dp(7), dp(7));
        b.setColorFilter(Color.rgb(61, 92, 114));
        b.setAlpha(0.58f);
        b.setBackground(round(Color.argb(92, 255, 255, 255), dp(10), Color.argb(80, 145, 160, 184)));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private void scrollCurrentContentChild(boolean top) {
        if (contentScroll == null || content == null || content.getChildCount() == 0) {
            return;
        }
        int center = contentScroll.getScrollY() + contentScroll.getHeight() / 2;
        View target = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            int childTop = child.getTop();
            int childBottom = child.getBottom();
            if (childTop <= center && childBottom >= center) {
                target = child;
                break;
            }
            int childCenter = childTop + Math.max(0, child.getHeight()) / 2;
            int distance = Math.abs(childCenter - center);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = child;
            }
        }
        if (target == null) {
            return;
        }
        int nextY = top
                ? Math.max(0, target.getTop() - dp(8))
                : Math.max(0, target.getBottom() - contentScroll.getHeight() + dp(8));
        nextY = Math.min(nextY, contentBottomScrollY(contentScroll));
        contentScroll.smoothScrollTo(0, nextY);
    }

    private void scrollToBottomOnce(String key) {
        if (key == null || !autoScrolledSections.add(key)) {
            return;
        }
        scrollToBottom();
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setSending(boolean running) {
        requestRunning = running;
        if (running) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        syncSendButton();
    }

    private void syncSendButton() {
        if (activeSendButton == null) {
            return;
        }
        activeSendButton.setImageResource(requestRunning ? R.drawable.ic_stop_square : R.drawable.ic_send_plane);
        activeSendButton.setContentDescription(requestRunning ? "停止" : "发送");
        activeSendButton.setBackground(new ColorDrawable(Color.TRANSPARENT));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams compactFullButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.setMargins(0, dp(6), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams compactCenteredButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(150), dp(42));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0, dp(6), 0, 0);
        return lp;
    }
}
