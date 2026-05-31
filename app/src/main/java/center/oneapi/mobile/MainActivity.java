package center.oneapi.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
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
    private TextView statusText;
    private EditText activeInput;
    private Spinner modelSpinner;
    private Spinner reasoningSpinner;
    private Spinner permissionSpinner;
    private Button activeSendButton;
    private String selectedCliModel = "";
    private String selectedReasoning = "关闭思考";
    private String selectedPermission = "受限模式";
    private boolean requestRunning = false;

    private String section = "chat";
    private String boundDeviceId = "";
    private String sessionId = "android-" + UUID.randomUUID();
    private boolean polling = false;
    private final List<String> codexModels = new ArrayList<>();
    private final List<String> claudeModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        prefs = getSharedPreferences("oneapi_mobile", MODE_PRIVATE);
        if (prefs.getString("app_id", "").isEmpty()) {
            prefs.edit().putString("app_id", "android-" + UUID.randomUUID()).apply();
        }
        boundDeviceId = prefs.getString("bound_device_id", "");
        seedModels();
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

    private void seedModels() {
        codexModels.clear();
        claudeModels.clear();
        Collections.addAll(codexModels, "gpt-5.4", "gpt-5.3-codex", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5", "mimo-v2.5-pro");
        Collections.addAll(claudeModels, "claude-sonnet-4-6", "claude-opus-4-6", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5-pro");
    }

    private void showLogin() {
        polling = false;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        shell = new FrameLayout(this);
        shell.addView(new FlowBackgroundView(this), new FrameLayout.LayoutParams(-1, -1));
        shell.addView(new FrostedOverlayView(this), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout page = vertical();
        page.setPadding(dp(24), dp(52), dp(24), dp(24));
        LinearLayout card = glassPanel();
        card.setPadding(dp(22), dp(24), dp(22), dp(24));
        card.addView(title("OneAPI"));
        card.addView(copy("登录后绑定 PC/Mac 客户端，手机端发送 Codex/Claude 指令，桌面端执行并同步日志。"));
        EditText server = input("服务器地址");
        server.setText(prefs.getString("server", "http://127.0.0.1:3000"));
        EditText username = input("账号");
        EditText password = input("密码");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button login = primary("登录");
        login.setOnClickListener(v -> {
            hideKeyboard(password);
            prefs.edit().putString("server", trim(server)).apply();
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
        card.addView(gap(18));
        card.addView(server);
        card.addView(username);
        card.addView(password);
        card.addView(gap(12));
        card.addView(login);
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
        scroll.setClipToPadding(false);
        content = vertical();
        content.setPadding(dp(18), dp(12), dp(18), dp(18));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        composerHost = vertical();
        composerHost.setPadding(dp(18), 0, dp(18), dp(14));
        root.addView(composerHost);
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);
        refreshBottomNav();
        renderSection();
        refreshModels();
        refreshDevices(false);
        startPolling();
    }

    private View header() {
        LinearLayout bar = horizontal();
        bar.setPadding(dp(18), dp(10), dp(18), dp(6));
        topNav = horizontal();
        bar.addView(topNav, new LinearLayout.LayoutParams(0, dp(40), 1));
        Button menu = iconButton("☰");
        menu.setContentDescription("打开菜单");
        menu.setOnClickListener(v -> openDrawer());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(dp(38), dp(38));
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
        boolean mainSection = "chat".equals(section) || "image".equals(section) || "codex".equals(section) || "claude".equals(section);
        topNav.setVisibility(mainSection ? View.VISIBLE : View.INVISIBLE);
        if (!mainSection) {
            return;
        }
        for (String item : new String[]{"chat", "image", "codex", "claude"}) {
            Button b = navButton(navIcon(item), item.equals(section));
            b.setContentDescription(label(item));
            b.setOnClickListener(v -> {
                section = item;
                refreshBottomNav();
                renderSection();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            topNav.addView(b, lp);
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
            Button entry = drawerButton(label(item));
            entry.setOnClickListener(v -> {
                section = item;
                refreshBottomNav();
                renderSection();
                dialog.dismiss();
            });
            panel.addView(entry);
        }
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(dp(128), -2);
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
        composerHost.addView(composer("输入消息", "chat", () -> toast("Chat 消息已准备发送")));
    }

    private void renderImage() {
        composerHost.addView(composer("描述要生成或编辑的图片", "image", () -> toast("图片任务已准备发送")));
    }

    private void renderAiChat() {
        composerHost.addView(composer("输入 AIChat 消息", "chat", () -> toast("AIChat 消息已准备发送")));
    }

    private void renderPlaceholder(String name) {
        content.addView(card(name, "暂无内容"));
    }

    private void renderSubscriptions() {
        content.addView(sectionTitle("套餐订阅"));
        content.addView(planCard("基础套餐", "适合轻量聊天、图片和移动端远程控制。", "按月订阅"));
        content.addView(planCard("专业套餐", "适合高频 Codex/Claude 项目执行和多模型切换。", "推荐"));
        content.addView(planCard("团队套餐", "适合多人协作、统一账单和更高并发。", "企业"));
    }

    private void renderServiceStatus() {
        content.addView(sectionTitle("服务状态"));
        content.addView(statusCard("服务器连接", "可用"));
        content.addView(statusCard("桌面同步", boundDeviceId.isEmpty() ? "未绑定" : "已绑定 " + shortId(boundDeviceId)));
        content.addView(statusCard("移动端任务通道", "待发送任务"));
    }

    private void renderWallet() {
        content.addView(sectionTitle("我的钱包"));
        content.addView(card("钱包总览", "当前余额、赠送额度和订阅抵扣会在这里汇总展示。"));
        content.addView(card("最近账单", "暂无最近账单。"));
    }

    private void renderMe() {
        content.addView(sectionTitle("系统设置"));
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
        activeInput = input(hint);
        activeInput.setMinLines(2);
        box.addView(activeInput);

        LinearLayout row = horizontal();
        row.setPadding(0, dp(8), 0, 0);
        LinearLayout left = horizontal();
        addComposerTools(left, mode);
        LinearLayout right = horizontal();
        Button send = iconButton("↑");
        send.setContentDescription("发送");
        send.setTextColor(Color.WHITE);
        send.setBackground(round(BLUE, dp(18), BLUE));
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
        right.addView(send, new LinearLayout.LayoutParams(dp(36), dp(36)));
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(right);
        box.addView(row);
        return box;
    }

    private void addComposerTools(LinearLayout left, String mode) {
        if ("chat".equals(mode)) {
            addToolButton(left, "＋", "上传附件", () -> toast("选择附件"));
            addToolButton(left, "M", "模型选择", () -> showChoice("模型选择", list("默认模型", "高质量模型", "快速模型"), "默认模型", value -> toast("已选择 " + value)));
            addToolButton(left, "A", "助手选择", () -> showChoice("助手选择", list("通用助手", "写作助手", "代码助手"), "通用助手", value -> toast("已选择 " + value)));
            addToolButton(left, "T", "思考长度", () -> showChoice("思考长度", list("关闭思考", "低", "中", "高"), "关闭思考", value -> toast("已选择 " + value)));
            addToolButton(left, "C", "上下文设置", () -> showChoice("上下文设置", list("自动", "短上下文", "长上下文"), "自动", value -> toast("已选择 " + value)));
            return;
        }
        if ("image".equals(mode)) {
            addToolButton(left, "＋", "上传附件", () -> toast("选择参考图"));
            addToolButton(left, "A", "助手选择", () -> showChoice("助手选择", list("通用绘图", "图片编辑", "风格生成"), "通用绘图", value -> toast("已选择 " + value)));
            addToolButton(left, "S", "图片尺寸", () -> showChoice("图片尺寸", list("1024x1024", "1024x1536", "1536x1024"), "1024x1024", value -> toast("已选择 " + value)));
            addToolButton(left, "Q", "图片质量", () -> showChoice("图片质量", list("标准", "高清", "极致"), "标准", value -> toast("已选择 " + value)));
            addToolButton(left, "R", "随机", () -> toast("已随机生成参数"));
            return;
        }
        if ("codex".equals(mode) || "claude".equals(mode)) {
            addToolButton(left, "H", "最近会话", () -> {
                pollSessionsOnce();
                toast("正在同步最近会话");
            });
            addToolButton(left, "P", "权限开关", () -> showChoice("权限开关", list("受限模式", "全权限模式"), selectedPermission, value -> {
                selectedPermission = value;
                toast("已切换 " + value);
            }));
            addToolButton(left, "M", "模型选择", () -> showChoice("模型选择", "codex".equals(mode) ? codexModels : claudeModels, selectedCliModel, value -> {
                selectedCliModel = value;
                toast("已选择 " + value);
            }));
            addToolButton(left, "T", "思考长度", () -> showChoice("思考长度", list("关闭思考", "低", "中", "高"), selectedReasoning, value -> {
                selectedReasoning = value;
                toast("已选择 " + value);
            }));
            addToolButton(left, "S", "Skill/Plugin", () -> showChoice("Skill/Plugin", list("默认", "启用 Skill", "启用 Plugin"), "默认", value -> toast("已选择 " + value)));
        }
    }

    private void addToolButton(LinearLayout row, String icon, String description, Runnable action) {
        Button button = iconButton(icon);
        button.setContentDescription(description);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
        lp.setMargins(0, 0, dp(8), 0);
        row.addView(button, lp);
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
    }

    private void refreshDevices(boolean showToast) {
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/mobile/desktop-devices", null);
            JSONArray devices = envelope.optJSONArray("data");
            if (devices == null) {
                devices = new JSONArray();
            }
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
            boolean current = deviceId.equals(boundDeviceId) || d.optBoolean("bound");
            LinearLayout row = cardPanel();
            row.addView(bold(d.optString("name", "桌面客户端")));
            row.addView(copy(d.optString("platform", "desktop") + " · " + d.optString("status", "online") + " · " + shortId(deviceId)));
            LinearLayout actions = horizontal();
            Button bind = primary(current ? "已绑定" : "绑定/切换");
            bind.setEnabled(!current);
            bind.setOnClickListener(v -> bindDevice(deviceId));
            Button unbind = ghost("解除");
            unbind.setEnabled(current);
            unbind.setOnClickListener(v -> unbindDevice(deviceId));
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(40), 1);
            a.setMargins(0, dp(14), dp(6), 0);
            LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(40), 1);
            b.setMargins(dp(6), dp(14), 0, 0);
            actions.addView(bind, a);
            actions.addView(unbind, b);
            row.addView(actions);
            list.addView(row);
        }
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
        runNetwork(() -> {
            JSONObject body = new JSONObject();
            body.put("client", client);
            body.put("deviceId", boundDeviceId);
            body.put("sessionId", sessionId);
            body.put("prompt", prompt);
            body.put("model", selectedCliModel.isEmpty() ? selected(modelSpinner) : selectedCliModel);
            body.put("reasoningEffort", reasoningValue(selectedReasoning));
            body.put("permissionMode", selectedPermission.contains("全权限") ? "full" : "restricted");
            api("POST", "/api/mobile/desktop-jobs", body);
            ui(() -> {
                setSending(false);
                activeInput.setText("");
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
        if (!"codex".equals(section) && !"claude".equals(section)) {
            return;
        }
        runNetworkQuiet(() -> {
            JSONObject envelope = api("GET", "/api/mobile/desktop-sessions", null);
            JSONArray sessions = envelope.optJSONArray("data");
            JSONArray events = mergedEventsForClient(sessions == null ? new JSONArray() : sessions, section);
            ui(() -> renderTimeline(events));
        });
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
        for (JSONObject row : rows) {
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
                timeline.addView(messageBubble(e.optString("role"), e.optString("text")));
            } else {
                addLogRow(e);
            }
        }
    }

    private void addLogRow(JSONObject e) {
        String title = e.optString("title", e.optString("type"));
        String body = e.optString("body");
        String command = e.optString("command");
        if (title.trim().isEmpty() && body.trim().isEmpty() && command.trim().isEmpty()) {
            return;
        }
        if (title.equals(body) && command.trim().isEmpty()) {
            body = "";
        }
        LinearLayout row = cardPanel();
        row.addView(bold(title));
        if (!body.trim().isEmpty()) {
            row.addView(copy(body));
        }
        if (!command.trim().isEmpty()) {
            TextView cmd = copy(command);
            cmd.setTextColor(Color.rgb(45, 74, 124));
            row.addView(cmd);
        }
        String interactionId = e.optString("interactionId");
        if (!interactionId.isEmpty() && "pending".equals(e.optString("interactionStatus", "pending"))) {
            renderInteraction(e);
        }
        timeline.addView(row);
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
        TextView t = copy(text);
        t.setTextColor(user ? Color.WHITE : INK);
        box.addView(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(user ? dp(42) : 0, dp(10), user ? 0 : dp(42), dp(2));
        box.setLayoutParams(lp);
        return box;
    }

    private JSONObject api(String method, String path, JSONObject body) throws Exception {
        String base = prefs.getString("server", "").replaceAll("/+$", "");
        URL url = new URL(base + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
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
        for (String option : options) {
            Button entry = drawerButton(option.equals(current) ? option + "  ✓" : option);
            entry.setOnClickListener(v -> {
                handler.onChoice(option);
                dialog.dismiss();
            });
            panel.addView(entry);
        }
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

    private Button drawerButton(String text) {
        Button b = ghost(text);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setTextSize(15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.setMargins(0, dp(8), 0, 0);
        b.setLayoutParams(lp);
        return b;
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

    private String navIcon(String value) {
        switch (value) {
            case "chat": return "○";
            case "image": return "▧";
            case "codex": return "⌘";
            case "claude": return "✦";
            default: return value;
        }
    }

    private String reasoningValue(String label) {
        if (label.contains("低")) return "low";
        if (label.contains("中")) return "medium";
        if (label.contains("高")) return "high";
        return "off";
    }

    private String selected(Spinner spinner) {
        Object item = spinner == null ? null : spinner.getSelectedItem();
        return item == null ? "" : String.valueOf(item);
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

    private void hideKeyboard(View view) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void ui(Runnable runnable) {
        handler.post(runnable);
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
        activeSendButton.setText(requestRunning ? "■" : "↑");
        activeSendButton.setContentDescription(requestRunning ? "停止" : "发送");
        activeSendButton.setBackground(round(requestRunning ? Color.rgb(216, 71, 86) : BLUE, dp(18), requestRunning ? Color.rgb(216, 71, 86) : BLUE));
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
