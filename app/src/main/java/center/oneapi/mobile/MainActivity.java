package center.oneapi.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;

    private FrameLayout shell;
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout composerHost;
    private LinearLayout topNav;
    private LinearLayout timeline;
    private LinearLayout interactionBar;
    private ScrollView contentScroll;
    private TextView statusText;
    private EditText activeInput;
    private Spinner modelSpinner;
    private Spinner reasoningSpinner;
    private Spinner permissionSpinner;
    private ImageButton activeSendButton;
    private LinearLayout attachmentPreviewHost;
    private LinearLayout extensionPreviewHost;
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
    private String selectedCliSession = "最近会话";
    private String selectedSkillPlugin = "默认";
    private String selectedRecentJobId = "";
    private String activeChatSessionId = "";
    private String activeImageSessionId = "";
    private String selectedCodexModel = "";
    private String selectedClaudeModel = "";
    private boolean imageRandomSeed = false;
    private boolean requestRunning = false;
    private boolean cliRefreshRunning = false;
    private long lastCliRefreshAt = 0L;
    private final List<Uri> selectedAttachmentUris = new ArrayList<>();
    private final List<JSONObject> selectedExtensionRefs = new ArrayList<>();

    private String section = "chat";
    private String boundDeviceId = "";
    private String sessionId = "android-" + UUID.randomUUID();
    private boolean polling = false;
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
            appendSelectedImages(data);
            renderAttachmentPreview();
            toast(selectedAttachmentUris.isEmpty() ? "未选择图片" : "已选择图片");
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
                    toast("登录成功");
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
            if (isCliSection() && scrollY >= contentBottomScrollY(scroll) - dp(2)) {
                refreshCliAtEdge();
            }
        });
        content = vertical();
        content.setPadding(dp(18), dp(12), dp(18), dp(18));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        composerHost = vertical();
        composerHost.setPadding(dp(18), 0, dp(18), dp(14));
        composerHost.setBackground(round(Color.argb(224, 255, 255, 255), dp(18), Color.TRANSPARENT));
        root.addView(composerHost);
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);
        refreshBottomNav();
        renderSection();
        refreshModels();
        refreshAssistants(false);
        refreshDevices(false);
        startPolling();
        installKeyboardLift();
    }

    private View header() {
        LinearLayout bar = horizontal();
        bar.setPadding(dp(18), dp(10), dp(18), dp(6));
        topNav = horizontal();
        bar.addView(topNav, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button menu = iconButton("☰");
        menu.setContentDescription("打开菜单");
        menu.setOnClickListener(v -> openDrawer());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        menuLp.setMargins(dp(12), 0, 0, 0);
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
                refreshBottomNav();
                renderSection();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            topNav.addView(b, lp);
            if (i == 1) {
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
        panel.setPadding(dp(10), dp(12), dp(10), dp(12));
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
                refreshBottomNav();
                renderSection();
                dialog.dismiss();
            });
            panel.addView(entry);
        }
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(dp(288), -2);
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
        content.removeAllViews();
        composerHost.removeAllViews();
        if ("codex".equals(section) || "claude".equals(section)) {
            renderCliWorkspace(section);
        } else if ("settings".equals(section)) {
            renderMe();
        } else if ("assistants".equals(section)) {
            renderAiChat();
        } else if ("subscriptions".equals(section)) {
            renderSubscriptions();
        } else if ("service".equals(section)) {
            renderServiceStatus();
        } else if ("wallet".equals(section)) {
            renderWallet();
        } else if ("chat".equals(section)) {
            renderChat();
        } else if ("image".equals(section)) {
            renderImage();
        } else {
            renderPlaceholder(label(section));
        }
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
        String expectedSection = section;
        content.addView(loadingRow("正在加载历史聊天记录..."));
        handler.postDelayed(() -> {
            if (!expectedSection.equals(section) || content == null) {
                return;
            }
            content.removeAllViews();
            JSONArray messages = localConversationMessages(mode);
            for (int i = 0; i < messages.length(); i++) {
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
                if ("image".equals(type)) {
                    content.addView(imageResultBubble(text));
                } else {
                    content.addView(messageBubble(role, text));
                }
            }
            scrollToBottom();
        }, 80);
    }

    private View loadingRow(String text) {
        LinearLayout row = cardPanel();
        row.addView(new DrawingProgressView(this), new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView label = copy(text);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        row.addView(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, dp(8));
        row.setLayoutParams(lp);
        return row;
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
        LinearLayout bind = cardPanel();
        bind.addView(bold("设备绑定"));
        bind.addView(copy("一个 Android 应用只能绑定一个当前账号下在线客户端，一个客户端也只允许被一个应用绑定。"));
        Button refresh = primary("刷新设备");
        refresh.setOnClickListener(v -> refreshDevices(true));
        bind.addView(gap(10));
        bind.addView(refresh, compactFullButtonLp());
        content.addView(bind);

        LinearLayout list = vertical();
        list.setTag("device_list");
        content.addView(list);
        renderDeviceList(list, new JSONArray());
        refreshDevices(true);

        renderAnnouncementsModule();
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
        account.addView(logout, compactFullButtonLp());
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
        Button check = primary("检查更新");
        check.setOnClickListener(v -> checkAndroidUpdate(check));
        panel.addView(check, compactFullButtonLp());
        content.addView(panel);
    }

    private String currentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "1.0.0";
        }
    }

    private void checkAndroidUpdate(Button action) {
        Object tag = action.getTag();
        if (tag instanceof String && !((String) tag).isEmpty()) {
            openExternalUrl((String) tag);
            return;
        }
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
                    action.setText("下载更新");
                    action.setTag(finalUrl);
                    toast("发现新版本 " + latest);
                } else {
                    action.setText("检查更新");
                    action.setTag("");
                    toast("当前已经是最新版本");
                }
            });
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
        renderTimeline(new JSONArray());
        composerHost.addView(composer("输入要执行的任务", client, () -> sendCliJob(client)));
    }

    private View composer(String hint, String mode, Runnable sendAction) {
        LinearLayout box = vertical();
        activeComposerMode = mode;
        if (!"chat".equals(mode) && selectedAttachmentUris.size() > 1) {
            while (selectedAttachmentUris.size() > 1) {
                selectedAttachmentUris.remove(selectedAttachmentUris.size() - 1);
            }
        }
        box.setBackground(round(Color.argb(235, 255, 255, 255), dp(18), LINE));
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        attachmentPreviewHost = horizontal();
        attachmentPreviewHost.setPadding(0, 0, 0, dp(8));
        box.addView(attachmentPreviewHost);
        extensionPreviewHost = horizontal();
        extensionPreviewHost.setPadding(0, 0, 0, dp(8));
        box.addView(extensionPreviewHost);
        renderAttachmentPreview();
        renderExtensionPreview();
        activeInput = input(hint);
        activeInput.setMinLines(2);
        activeInput.setMaxLines(5);
        activeInput.setVerticalScrollBarEnabled(false);
        activeInput.setOverScrollMode(View.OVER_SCROLL_NEVER);
        activeInput.setGravity(Gravity.TOP | Gravity.START);
        activeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(activeInput);

        LinearLayout row = horizontal();
        row.setPadding(dp(10), dp(8), dp(10), 0);
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
                toast("已停止当前发送");
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

    private void addComposerTools(LinearLayout left, String mode) {
        if ("chat".equals(mode)) {
            addToolButton(left, R.drawable.tool_add, "上传附件", this::openImagePicker);
            addToolButton(left, R.drawable.tool_history, "最近会话", () -> showRecentSessions(mode));
            addToolButton(left, R.drawable.tool_module, "AI 模型选择", () -> showModelChoice("AI 模型选择", chatModelOptions(), selectedChatModel, "chat", value -> {
                selectedChatModel = value;
                prefs.edit().putString("selected_chat_model", value).apply();
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_helper, "助手", () -> showChoice("助手", chatAssistantOptions(), selectedChatAssistant, value -> {
                selectedChatAssistant = value;
                prefs.edit().putString("selected_chat_assistant", value).apply();
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_think, "Thinking", () -> showChoice("Thinking", list("关闭思考", "低", "中", "高"), selectedReasoning, value -> {
                selectedReasoning = value;
                prefs.edit().putString("selected_reasoning", value).apply();
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_text, "上下文长度", () -> showChoice("上下文长度", list("自动", "短上下文", "长上下文"), selectedContextWindow, value -> {
                selectedContextWindow = value;
                prefs.edit().putString("selected_context_window", value).apply();
                toast("已选择 " + value);
            }));
            return;
        }
        if ("image".equals(mode)) {
            addToolButton(left, R.drawable.tool_add, "上传附件", this::openImagePicker);
            addToolButton(left, R.drawable.tool_history, "最近会话", () -> showRecentSessions(mode));
            addToolButton(left, R.drawable.tool_helper, "助手", () -> showChoice("助手", imageAssistantOptions(), selectedImageAssistant, value -> {
                selectedImageAssistant = value;
                prefs.edit().putString("selected_image_assistant", value).apply();
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_size, "尺寸", () -> showChoice("尺寸", list("1024x1024", "1024x1536", "1536x1024"), selectedImageSize, value -> {
                selectedImageSize = value;
                prefs.edit().putString("selected_image_size", value).apply();
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_ratio, "分辨率", () -> showChoice("分辨率", list("标准", "高清", "极致"), selectedImageQuality, value -> {
                selectedImageQuality = value;
                prefs.edit().putString("selected_image_quality", value).apply();
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_random, "随机", () -> {
                imageRandomSeed = !imageRandomSeed;
                toast(imageRandomSeed ? "已开启随机参数" : "已关闭随机参数");
            });
            return;
        }
        if ("codex".equals(mode) || "claude".equals(mode)) {
            addToolButton(left, R.drawable.tool_history, "最近会话", () -> showRecentSessions(mode));
            addToolButton(left, selectedPermission.contains("全权限") ? R.drawable.tool_authority : R.drawable.tool_noauthority, "权限模式", () -> showChoice("权限模式", list("受限模式", "全权限模式"), selectedPermission, value -> {
                selectedPermission = value;
                prefs.edit().putString("selected_permission", value).apply();
                toast("已切换 " + value);
                renderSection();
            }));
            addToolButton(left, R.drawable.tool_module, "AI 模型选择", () -> showModelChoice("AI 模型选择", "codex".equals(mode) ? codexModels : claudeModels, selectedCliModelFor(mode), mode, value -> {
                if ("codex".equals(mode)) {
                    selectedCodexModel = value;
                    prefs.edit().putString("selected_codex_model", value).apply();
                } else {
                    selectedClaudeModel = value;
                    prefs.edit().putString("selected_claude_model", value).apply();
                }
                toast("已选择 " + value);
            }));
            addToolButton(left, R.drawable.tool_think, "Thinking", () -> showChoice("Thinking", list("关闭思考", "低", "中", "高"), selectedReasoning, value -> {
                selectedReasoning = value;
                prefs.edit().putString("selected_reasoning", value).apply();
                toast("已选择 " + value);
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
            thumb.setImageURI(uri);
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
        ImageView preview = new ImageView(this);
        preview.setImageURI(uri);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, "chat".equals(activeComposerMode));
        try {
            startActivityForResult(Intent.createChooser(intent, "选择图片"), REQ_PICK_IMAGE);
        } catch (Exception error) {
            Intent fallback = new Intent(Intent.ACTION_PICK);
            fallback.setType("image/*");
            startActivityForResult(Intent.createChooser(fallback, "选择图片"), REQ_PICK_IMAGE);
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
        if (names.isEmpty()) {
            names.addAll(list("通用绘图", "图片编辑", "风格生成", "商业海报", "产品摄影", "头像生成", "插画创作", "数字艺术创作助手"));
        }
        return names;
    }

    private String assistantPrompt(String name) {
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
        if ("商业海报".equals(name)) {
            return "商业海报风格，高级排版，清晰主体，" + prompt;
        }
        if ("产品摄影".equals(name)) {
            return "产品摄影风格，真实光影，干净背景，" + prompt;
        }
        if ("头像生成".equals(name)) {
            return "高质量头像，细节清晰，" + prompt;
        }
        if ("插画创作".equals(name) || "数字艺术创作助手".equals(name)) {
            return "数字插画艺术风格，构图完整，" + prompt;
        }
        return prompt;
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
        prefs.edit().putString(mode + "_active_session_id", id == null ? "" : id).apply();
    }

    private JSONObject localConversationStore(String mode) {
        try {
            JSONObject root = new JSONObject(prefs.getString(mode + "_conversation_store", "{}"));
            if (root.optJSONArray("sessions") == null) {
                root.put("sessions", new JSONArray());
            }
            return root;
        } catch (Exception ignored) {
            try {
                return new JSONObject().put("sessions", new JSONArray());
            } catch (Exception impossible) {
                return new JSONObject();
            }
        }
    }

    private void saveLocalConversationStore(String mode, JSONObject root) {
        prefs.edit().putString(mode + "_conversation_store", root.toString()).apply();
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

    private JSONObject ensureLocalSession(String mode, String firstPrompt) throws Exception {
        JSONObject root = localConversationStore(mode);
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

    private String cleanSessionTitle(String prompt) {
        String title = cleanDisplayText(prompt);
        if (title.isEmpty()) {
            return "新会话";
        }
        return title.substring(0, Math.min(28, title.length()));
    }

    private JSONArray localConversationMessages(String mode) {
        JSONObject root = localConversationStore(mode);
        JSONObject session = findLocalSession(root, activeLocalSessionId(mode));
        if (session != null && !localConversationGroup(mode).equals(session.optString("group"))) {
            session = null;
        }
        JSONArray messages = session == null ? null : session.optJSONArray("messages");
        return messages == null ? new JSONArray() : messages;
    }

    private void appendLocalConversation(String mode, String role, String type, String text) {
        String clean = cleanDisplayText(text);
        if (clean.isEmpty()) {
            return;
        }
        try {
            JSONObject root = localConversationStore(mode);
            JSONObject session = ensureLocalSession(mode, clean);
            JSONArray messages = session.optJSONArray("messages");
            if (messages == null) {
                messages = new JSONArray();
                session.put("messages", messages);
            }
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
        } catch (Exception ignored) {
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
        hideKeyboard(activeInput);
        setSending(true);
        activeInput.setText("");
        List<Uri> attachments = new ArrayList<>(selectedAttachmentUris);
        selectedAttachmentUris.clear();
        renderAttachmentPreview();
        saveLocalRecent("chat", selectedChatAssistant, prompt);
        appendLocalConversation("chat", "user", "text", prompt);
        content.addView(messageBubble("user", prompt));
        scrollToBottom();
        TextView live = addStreamingBubble("assistant");
        live.setText("Thinking\n正在思考...");
        scrollToBottom();
        runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("model", resolveChatModel());
            body.put("stream", true);
            String reasoning = reasoningValue(selectedReasoning);
            if (!"off".equals(reasoning)) {
                body.put("reasoning_effort", reasoning);
            }
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", assistantPrompt(selectedChatAssistant) + "\n上下文长度：" + selectedContextWindow));
            if (attachments.isEmpty()) {
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
            } else {
                JSONArray contentParts = new JSONArray();
                contentParts.put(new JSONObject().put("type", "text").put("text", prompt));
                for (Uri uri : attachments) {
                    String dataUrl = uriToDataUrl(uri);
                    contentParts.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", dataUrl.isEmpty() ? uri.toString() : dataUrl)));
                }
                messages.put(new JSONObject().put("role", "user").put("content", contentParts));
            }
            body.put("messages", messages);
            String text = streamChatResponse(body, live);
            ui(() -> {
                setSending(false);
                if (text.trim().isEmpty()) {
                    live.setText("本次没有返回可显示内容。");
                    appendLocalConversation("chat", "assistant", "text", "本次没有返回可显示内容。");
                } else {
                    live.setText(text);
                    appendLocalConversation("chat", "assistant", "text", text);
                }
                scrollToBottom();
            });
        });
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
        appendLocalConversation("image", "user", "text", prompt);
        content.addView(messageBubble("user", prompt));
        scrollToBottom();
        LinearLayout progress = addImageProgressBubble();
        scrollToBottom();
        runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("model", "gpt-image-1");
            body.put("prompt", imageAssistantPrompt(selectedImageAssistant, prompt));
            body.put("n", 1);
            body.put("size", selectedImageSize);
            body.put("quality", imageQualityValue(selectedImageQuality));
            body.put("response_format", "url");
            if (imageRandomSeed) {
                body.put("seed", System.currentTimeMillis() % 1000000);
            }
            if (!attachments.isEmpty()) {
                String dataUrl = uriToDataUrl(attachments.get(0));
                body.put("reference_image", dataUrl.isEmpty() ? attachments.get(0).toString() : dataUrl);
            }
            JSONObject response = api("POST", "/pg/images/generations", body);
            String text = extractImageText(response);
            ui(() -> {
                setSending(false);
                String result = text.isEmpty() ? "图片任务已提交。" : text;
                renderImageResult(progress, result);
                appendLocalConversation("image", "assistant", result.startsWith("http://") || result.startsWith("https://") || result.startsWith("data:image/") ? "image" : "text", result);
                scrollToBottom();
            });
        });
    }

    private String resolveChatModel() {
        if (selectedChatModel != null && !selectedChatModel.isEmpty()) {
            return selectedChatModel;
        }
        return codexModels.isEmpty() ? "gpt-5.4" : codexModels.get(0);
    }

    private String imageQualityValue(String label) {
        if (label.contains("高清")) return "hd";
        if (label.contains("极致")) return "high";
        return "standard";
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
        JSONArray data = response.optJSONArray("data");
        if (data == null) {
            JSONObject nested = response.optJSONObject("data");
            if (nested != null) {
                return extractImageText(nested);
            }
            return cleanJsonString(response, "message");
        }
        JSONObject first = data.optJSONObject(0);
        if (first == null) {
            return "";
        }
        String url = cleanJsonString(first, "url");
        if (!url.isEmpty()) {
            return url;
        }
        String b64 = cleanJsonString(first, "b64_json");
        if (b64.isEmpty()) {
            b64 = cleanJsonString(first, "b64Json");
        }
        return b64.isEmpty() ? "图片已生成，服务器返回了图片数据。" : "data:image/png;base64," + b64;
    }

    private String streamChatResponse(JSONObject body, TextView live) throws Exception {
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
            ui(() -> live.setText(text));
            return text;
        }
        StringBuilder answer = new StringBuilder();
        StringBuilder thinking = new StringBuilder();
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
                    String reasoning = cleanJsonString(delta, "reasoning_content");
                    if (reasoning.isEmpty()) {
                        reasoning = cleanJsonString(delta, "reasoning");
                    }
                    String content = cleanJsonString(delta, "content");
                    if (!reasoning.isEmpty()) {
                        thinking.append(reasoning);
                    }
                    if (!content.isEmpty()) {
                        answer.append(content);
                    }
                    String rendered = renderLiveChatText(thinking.toString(), answer.toString());
                    ui(() -> {
                        live.setText(rendered);
                        scrollToBottom();
                    });
                } catch (Exception ignored) {
                }
            }
        } finally {
            conn.disconnect();
        }
        return renderLiveChatText(thinking.toString(), answer.toString());
    }

    private String renderLiveChatText(String thinking, String answer) {
        StringBuilder out = new StringBuilder();
        if (!thinking.trim().isEmpty()) {
            out.append("Thinking\n").append(thinking.trim()).append("\n\n");
        }
        out.append(answer);
        return out.toString();
    }

    private String cleanJsonString(JSONObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.isNull(key)) {
            return "";
        }
        return cleanDisplayText(object.optString(key, ""));
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
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            JSONObject plan = record == null ? null : record.optJSONObject("plan");
            if (plan == null || !plan.optBoolean("enabled", true)) {
                continue;
            }
            String title = plan.optString("title", "套餐");
            String subtitle = plan.optString("subtitle", "适合稳定使用。");
            String price = formatPlainPrice(plan.optDouble("price_amount", 0));
            String quota = formatQuotaAsUsd(plan.optDouble("total_amount", 0), 500000);
            String validity = formatDuration(plan);
            LinearLayout panel = cardPanel();
            LinearLayout head = horizontal();
            head.addView(bold(title), new LinearLayout.LayoutParams(0, -2, 1));
            TextView badge = copy(price + " 元");
            badge.setTextColor(BLUE);
            badge.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            head.addView(badge, new LinearLayout.LayoutParams(dp(104), -2));
            panel.addView(head);
            panel.addView(copy(subtitle));
            panel.addView(copy("总额度 " + quota + " · 有效期 " + validity + " · 重置 " + resetLabel(plan.optString("quota_reset_period", "never"))));
            Button buy = primary("订阅套餐");
            panel.addView(buy, compactFullButtonLp());
            list.addView(panel);
        }
        if (list.getChildCount() == 0) {
            list.addView(card("暂无可订阅套餐", "服务器当前没有开启可订阅套餐。"));
        }
    }

    private void refreshWallet() {
        runNetworkQuiet(() -> {
            JSONObject profile = api("GET", "/api/user/self", null).optJSONObject("data");
            JSONObject billing = api("GET", "/api/user/topup/self?p=1&page_size=3", null).optJSONObject("data");
            JSONObject usage = api("GET", "/api/log/self?p=1&page_size=50", null).optJSONObject("data");
            ui(() -> {
                View maybe = content == null ? null : content.findViewWithTag("wallet_list");
                if (maybe instanceof LinearLayout) {
                    renderWalletList((LinearLayout) maybe, profile == null ? new JSONObject() : profile, billing == null ? new JSONObject() : billing, usage == null ? new JSONObject() : usage);
                }
            });
        });
    }

    private void renderWalletList(LinearLayout list, JSONObject profile, JSONObject billing, JSONObject usage) {
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
            double maxAmount = 1;
            for (int i = 0; i < Math.min(items.length(), 3); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) {
                    maxAmount = Math.max(maxAmount, Math.abs(item.optDouble("amount", item.optDouble("money", 0))));
                }
            }
            for (int i = 0; i < Math.min(items.length(), 3); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                double amount = Math.abs(item.optDouble("amount", item.optDouble("money", 0)));
                bills.addView(billingRow(formatBillingLabel(item) + " · " + formatPlainPrice(item.optDouble("money", amount)) + " 元", amount / maxAmount));
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
                if (showToast) {
                    toast("助手已同步");
                }
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
                if (showToast) {
                    toast("设备状态已刷新");
                }
            });
        });
    }

    private void renderDeviceList(LinearLayout list, JSONArray devices) {
        list.removeAllViews();
        if (devices.length() == 0) {
            list.addView(card("暂无客户端", "请打开 PC/Mac 客户端并登录同一账号，客户端在线后会自动出现在这里。"));
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
            row.addView(bold(d.optString("name", "桌面客户端")));
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
                toast("设备已绑定");
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
                toast("绑定已解除");
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
            body.put("sessionId", sessionId);
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
                toast("任务已发送到绑定客户端");
                pollSessionsOnce();
            });
        });
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!polling) {
                    return;
                }
                pollSessionsOnce();
                handler.postDelayed(this, 3000);
            }
        }, 800);
    }

    private void pollSessionsOnce() {
        pollSessionsOnce(false);
    }

    private void pollSessionsOnce(boolean force) {
        if (!"codex".equals(section) && !"claude".equals(section)) {
            return;
        }
        if (cliRefreshRunning && !force) {
            return;
        }
        cliRefreshRunning = true;
        runNetworkQuiet(() -> {
            try {
                JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
                JSONArray sessions = envelope.optJSONArray("data");
                JSONArray events = mergedEventsForClient(sessions == null ? new JSONArray() : sessions, section);
                ui(() -> renderTimeline(events));
            } finally {
                cliRefreshRunning = false;
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
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.optJSONObject(i);
            if (session == null || !client.equals(session.optString("client"))) {
                continue;
            }
            collectRows(rows, session.optJSONArray("messages"), "message");
            collectRows(rows, session.optJSONArray("logs"), "log");
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

    private void renderTimeline(JSONArray events) {
        if (timeline == null) {
            return;
        }
        timeline.removeAllViews();
        if (interactionBar != null) {
            interactionBar.removeAllViews();
        }
        if (events.length() == 0) {
            return;
        }
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) {
                continue;
            }
            if ("message".equals(e.optString("_kind"))) {
                String text = cleanDisplayText(e.optString("text"));
                if (!text.isEmpty()) {
                    timeline.addView(messageBubble(e.optString("role"), text));
                }
            } else {
                addLogRow(e);
            }
        }
        scrollToBottom();
    }

    private void addLogRow(JSONObject e) {
        if (shouldHideLog(e)) {
            return;
        }
        String title = cleanDisplayText(e.optString("title", e.optString("type")));
        String body = cleanDisplayText(e.optString("body"));
        String command = compactCommand(e.optString("command"));
        if (title.trim().isEmpty() && body.trim().isEmpty() && command.trim().isEmpty()) {
            return;
        }
        if (title.equals(body) && command.trim().isEmpty()) {
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
        String phase = e.optString("phase", e.optString("type", ""));
        int accent = level >= 2 ? Color.rgb(216, 71, 86) : level == 1 ? Color.rgb(210, 132, 40) : phaseColor(phase);
        LinearLayout head = horizontal();
        TextView dot = copy("●");
        dot.setTextColor(accent);
        dot.setTextSize(13);
        head.addView(dot, new LinearLayout.LayoutParams(dp(20), -2));
        TextView titleView = bold(title);
        head.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        TextView phaseView = copy(phaseLabel(phase));
        phaseView.setTextColor(accent);
        phaseView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        head.addView(phaseView, new LinearLayout.LayoutParams(dp(78), -2));
        row.addView(head);
        if (!body.trim().isEmpty()) {
            row.addView(copy(body));
        }
        if (!command.trim().isEmpty()) {
            TextView cmd = copy(command);
            cmd.setTextColor(Color.rgb(45, 74, 124));
            cmd.setBackground(round(Color.argb(76, 54, 104, 240), dp(10), Color.TRANSPARENT));
            cmd.setPadding(dp(10), dp(8), dp(10), dp(8));
            row.addView(cmd);
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
                toast("确认结果已发送");
                pollSessionsOnce();
            });
        }));
        return b;
    }

    private View messageBubble(String role, String text) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        boolean user = "user".equals(role);
        box.setBackground(round(user ? BLUE : GLASS, dp(16), user ? BLUE : LINE));
        TextView t = copy(cleanDisplayText(text));
        t.setTextColor(user ? Color.WHITE : INK);
        box.addView(t);
        box.setLayoutParams(bubbleLayoutParams(user));
        return box;
    }

    private TextView addStreamingBubble(String role) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(GLASS, dp(16), LINE));
        TextView t = copy("");
        t.setTextColor(INK);
        box.addView(t);
        box.setLayoutParams(bubbleLayoutParams("user".equals(role)));
        content.addView(box);
        return t;
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
        lp.setMargins(user ? dp(42) : 0, dp(8), user ? 0 : dp(42), dp(8));
        return lp;
    }

    private View imageResultBubble(String source) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(GLASS, dp(16), LINE));
        box.setLayoutParams(bubbleLayoutParams(false));
        renderImageResult(box, source);
        return box;
    }

    private void renderImageResult(LinearLayout box, String source) {
        if (box == null) {
            content.addView(messageBubble("assistant", source));
            return;
        }
        box.removeAllViews();
        if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("data:image/")) {
            ImageView image = new ImageView(this);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(round(Color.WHITE, dp(14), LINE));
            image.setOnClickListener(v -> showImagePreview(Uri.parse(source)));
            box.addView(image, new LinearLayout.LayoutParams(-1, dp(240)));
            if (source.startsWith("data:image/")) {
                String base64 = source.substring(source.indexOf(",") + 1);
                try {
                    byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                    image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                } catch (Exception ignored) {
                    box.addView(copy(source));
                }
            } else {
                loadImageIntoView(source, image);
            }
        } else {
            box.addView(copy(cleanDisplayText(source)));
        }
        scrollToBottom();
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
            return "服务器还没有更新设备绑定接口，请先更新服务器版本";
        }
        if (lower.contains("timeout") || lower.contains("refused")) {
            return "服务器暂时无响应，请稍后重试";
        }
        return msg.isEmpty() ? "操作失败，请稍后重试" : msg;
    }

    private interface JsonRunnable {
        void run() throws Exception;
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
        runNetwork(() -> {
            JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
            JSONArray sessions = envelope.optJSONArray("data");
            Map<String, List<String>> grouped = new HashMap<>();
            List<String> tabs = new ArrayList<>();
            if (sessions != null) {
                int projects = 0;
                for (int i = 0; i < sessions.length() && projects < 3; i++) {
                    JSONObject session = sessions.optJSONObject(i);
                    if (session == null || !client.equals(session.optString("client"))) {
                        continue;
                    }
                    String title = session.optString("title", label(client) + " 会话");
                    List<String> options = new ArrayList<>();
                    JSONArray messages = session.optJSONArray("messages");
                    int added = 0;
                    if (messages != null) {
                        for (int j = messages.length() - 1; j >= 0 && added < 2; j--) {
                            JSONObject msg = messages.optJSONObject(j);
                            String preview = msg == null ? "" : msg.optString("text", "");
                            if (!preview.trim().isEmpty()) {
                                options.add(preview.substring(0, Math.min(36, preview.length())));
                                added++;
                            }
                        }
                    }
                    if (options.isEmpty()) {
                        options.add("暂无最近会话");
                    }
                    grouped.put(title, options);
                    tabs.add(title);
                    projects++;
                }
            }
            if (tabs.isEmpty()) {
                String title = label(client);
                tabs.add(title);
                grouped.put(title, list("暂无最近会话"));
            }
            ui(() -> showGroupedChoice(label(client) + " 最近会话", tabs, grouped, selectedCliSession, value -> {
                selectedCliSession = value.trim();
                if (!"暂无最近会话".equals(selectedCliSession)) {
                    toast("已选择 " + selectedCliSession);
                    pollSessionsOnce();
                }
            }));
        });
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
                    if (session != null) {
                        rows.add(session);
                    }
                }
            }
            rows.sort((a, b) -> Long.compare(b.optLong("updatedAt"), a.optLong("updatedAt")));
            for (JSONObject session : rows) {
                String group = session.optString("group", "默认助手");
                List<String> values = grouped.get(group);
                if (values == null) {
                    values = new ArrayList<>();
                    grouped.put(group, values);
                    tabs.add(group);
                }
                String title = session.optString("title", "未命名会话");
                values.add(title);
                sessionIdByTitle.put(group + "\n" + title, session.optString("id"));
            }
        } catch (Exception ignored) {
        }
        if (tabs.isEmpty()) {
            String fallback = "chat".equals(mode) ? selectedChatAssistant : selectedImageAssistant;
            grouped.put(fallback, list("暂无最近会话"));
            tabs.add(fallback);
        }
        showGroupedChoice(label(mode) + " 最近会话", tabs, grouped, "", value -> {
            if (!"暂无最近会话".equals(value)) {
                String selectedId = "";
                for (String tab : tabs) {
                    selectedId = sessionIdByTitle.get(tab + "\n" + value);
                    if (selectedId != null && !selectedId.isEmpty()) {
                        break;
                    }
                }
                if (selectedId != null && !selectedId.isEmpty()) {
                    setActiveLocalSessionId(mode, selectedId);
                    renderSection();
                }
            }
        });
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
                toast("已引用 " + value);
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

    private void showGroupedChoice(String title, List<String> tabs, Map<String, List<String>> grouped, String current, ChoiceHandler handler) {
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
            List<String> values = grouped.get(activeTab[0]);
            if (values == null || values.isEmpty()) {
                optionsBox.addView(copy("暂无可选项"));
                return;
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
        Button close = iconButton("×");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        panel.addView(titleRow);
        panel.addView(gap(8));
        ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout optionsBox = vertical();
        for (String option : options) {
            Button entry = drawerButton(option.equals(current) ? option + "  ✓" : option);
            if (option.startsWith("【") && option.endsWith("】")) {
                entry.setEnabled(false);
                entry.setTextColor(BLUE);
                entry.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
            entry.setOnClickListener(v -> {
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
        e.setPadding(dp(14), dp(9), dp(14), dp(9));
        e.setBackground(round(Color.argb(205, 255, 255, 255), dp(16), LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 0);
        e.setLayoutParams(lp);
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.setMargins(0, dp(8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private View drawerEntry(String item) {
        LinearLayout row = horizontal();
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setBackground(round(Color.argb(120, 255, 255, 255), dp(16), LINE));
        TextView icon = copy(drawerIcon(item));
        icon.setTextSize(16);
        icon.setTextColor(BLUE);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), -1));
        TextView text = bold(label(item));
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER);
        row.addView(text, new LinearLayout.LayoutParams(0, -1, 1));
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(dp(34), -1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    private String drawerIcon(String item) {
        if ("assistants".equals(item)) return "✦";
        if ("subscriptions".equals(item)) return "◆";
        if ("service".equals(item)) return "●";
        if ("wallet".equals(item)) return "□";
        if ("settings".equals(item)) return "⚙";
        return "•";
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
            if (keyboard > dp(120)) {
                composerHost.setTranslationY(-Math.max(0, keyboard - dp(12)));
                composerHost.bringToFront();
            } else {
                composerHost.setTranslationY(0);
            }
        });
    }

    private void hideKeyboard(View view) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void ui(Runnable runnable) {
        handler.post(runnable);
    }

    private void scrollToBottom() {
        if (contentScroll == null) {
            return;
        }
        contentScroll.post(() -> contentScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setSending(boolean running) {
        requestRunning = running;
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
}
