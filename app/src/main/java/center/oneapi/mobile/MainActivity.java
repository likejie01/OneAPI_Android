package center.oneapi.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.graphics.pdf.PdfRenderer;
import android.text.TextUtils;
import android.text.Html;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import center.oneapi.mobile.core.ApiClient;
import center.oneapi.mobile.core.AppPrefs;
import center.oneapi.mobile.data.ConversationMessageEntity;
import center.oneapi.mobile.data.ConversationRepository;
import center.oneapi.mobile.data.ConversationSessionEntity;
import center.oneapi.mobile.features.attachments.AttachmentContentReader;
import center.oneapi.mobile.features.audio.AudioTranscriptionController;
import center.oneapi.mobile.features.billing.BillingController;
import center.oneapi.mobile.features.billing.AlipayPaymentGuard;
import center.oneapi.mobile.features.catalog.AssistantCatalog;
import center.oneapi.mobile.features.catalog.ModelCatalog;
import center.oneapi.mobile.features.chat.ChatContextBuilder;
import center.oneapi.mobile.features.chat.ChatController;
import center.oneapi.mobile.features.desktop.DesktopController;
import center.oneapi.mobile.features.desktop.DesktopRepository;
import center.oneapi.mobile.features.desktop.DesktopTimelineMapper;
import center.oneapi.mobile.features.image.ImageController;
import center.oneapi.mobile.features.image.ImageAssistantCatalog;
import center.oneapi.mobile.features.keys.AppApiKeyManager;
import center.oneapi.mobile.features.status.StatusController;
import center.oneapi.mobile.navigation.AppSection;
import center.oneapi.mobile.ui.FlowBackgroundView;
import center.oneapi.mobile.ui.FrostedOverlayView;
import center.oneapi.mobile.ui.UiKit;
import center.oneapi.mobile.ui.composer.FlowTagLayout;
import center.oneapi.mobile.ui.composer.ComposerState;
import center.oneapi.mobile.ui.composer.ComposerView;
import center.oneapi.mobile.ui.conversation.ConversationAdapter;
import center.oneapi.mobile.ui.conversation.ConversationDisplayItem;
import center.oneapi.mobile.ui.conversation.ScrollDock;

public class MainActivity extends Activity {
    private static final String TAG = "OneAPI-Mobile";
    private static final int REQ_IMAGE = 42;
    private static final int REQ_RECORD_AUDIO = 43;
    private static final int REQ_CAMERA = 44;
    private static final int REQ_GALLERY = 45;
    private static final int REQ_DOCUMENT = 46;
    private final Map<AppSection, SectionState> states = new EnumMap<>(AppSection.class);
    private final List<Uri> selectedAttachments = new ArrayList<>();
    private Uri pendingCameraUri;
    private final Map<AppSection, LinearLayout> navButtons = new EnumMap<>(AppSection.class);
    private final Map<String, String> chatAssistantPrompts = new HashMap<>();
    private final Map<String, String> imageAssistantPrompts = new HashMap<>();
    private PanelDataStore panelDataStore;
    private AppSection currentSection = AppSection.CHAT;
    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private ComposerView composerView;
    private ScrollDock scrollDock;
    private TextView titleView;
    private FrameLayout contentFrame;
    private ScrollView pageScroll;
    private LinearLayout pageContent;
    private LinearLayout aiChatHeader;
    private LinearLayout aiChatTopNav;
    private Button drawerMenuButton;
    private AppSection lastAiChatSection = AppSection.CHAT;
    private FlowBackgroundView flowBackgroundView;
    private AppPrefs prefs;
    private ApiClient apiClient;
    private ApiClient criticalApiClient;
    private ApiClient userActionApiClient;
    private ApiClient desktopApiClient;
    private ChatController chatController;
    private ImageController imageController;
    private AudioTranscriptionController audioTranscriptionController;
    private DesktopController desktopController;
    private DesktopRepository desktopRepository;
    private StatusController statusController;
    private BillingController billingController;
    private ConversationRepository conversationRepository;
    private AppApiKeyManager appApiKeyManager;
    private AttachmentContentReader attachmentReader;
    private final Runnable desktopPoll = new Runnable() {
        @Override
        public void run() {
            if (!prefs.boundDeviceId().isEmpty()) {
                maybeRefreshDesktopSessions(AppSection.CODEX, false);
                maybeRefreshDesktopSessions(AppSection.CLAUDE, false);
            }
            mainHandler.postDelayed(this, currentDesktopSessionBusy() ? DESKTOP_BUSY_SESSION_REFRESH_INTERVAL_MS : 8000);
        }
    };
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService criticalNetwork = Executors.newFixedThreadPool(4);
    private final ExecutorService userActionNetwork = Executors.newFixedThreadPool(2);
    private final ExecutorService desktopNetwork = Executors.newSingleThreadExecutor();
    private final ExecutorService backgroundNetwork = Executors.newSingleThreadExecutor();
    private final ExecutorService persistence = Executors.newSingleThreadExecutor();
    private final ExecutorService desktopJobMonitor = Executors.newSingleThreadExecutor();
    private Future<?> activeSendTask;
    private AtomicBoolean activeSendCancelled;
    private final Map<AppSection, String> rememberedModelProvider = new EnumMap<>(AppSection.class);
    private final Map<AppSection, String> rememberedExtensionKind = new EnumMap<>(AppSection.class);
    private final Map<AppSection, Long> desktopSessionRefreshedAt = new EnumMap<>(AppSection.class);
    private final Set<String> desktopSessionRefreshInFlight = new HashSet<>();
    private long desktopBindingCheckedAt;
    private boolean desktopBindingCheckInFlight;
    private boolean keyboardOpen;
    private boolean loginDialogShowing;
    private int lastKeyboardInset = -1;
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+)\\)");
    private static final long STREAM_SCROLL_INTERVAL_MS = 32L;
    private static final long STREAM_SCROLL_RESUME_MS = 1000L;
    private static final long DESKTOP_JOB_TAKEOVER_TIMEOUT_MS = 25000L;
    private static final long DESKTOP_JOB_EVENT_POLL_MS = 2500L;
    private static final long DESKTOP_BUSY_SESSION_REFRESH_INTERVAL_MS = 1500L;
    private static final long DESKTOP_SESSION_REFRESH_MIN_INTERVAL_MS = 20000L;
    private static final int DEFAULT_ALIPAY_AMOUNT = 50;
    private static final String LEGAL_ACCEPTANCE_VERSION = "2026-06-25";
    private static final String SKIP_NEW_CONVERSATION_SAFETY_NOTICE = "skip_new_conversation_safety_notice";
    private static final String[] LEGAL_ACK_SECTIONS = new String[]{
            "user-agreement",
            "privacy-policy",
            "generative-ai-service",
            "report",
            "content-safety"
    };
    private boolean voiceListening;
    private boolean desktopKeepScreenOn;
    private boolean streamResponseActive;
    private boolean streamScrollPausedByUser;
    private boolean streamScrollRunning;
    private boolean streamScrollProgrammatic;
    private boolean desktopBridgeConnected = true;
    private boolean landscapeConversationChromeHidden;
    private String pendingAlipayTradeNo;
    private String pendingAlipayAccountKey = "";
    private int pendingAlipayAmount = DEFAULT_ALIPAY_AMOUNT;
    private int selectedAlipayAmount = DEFAULT_ALIPAY_AMOUNT;
    private boolean alipayOrderRequestInFlight;
    private Dialog pendingAlipayDialog;
    private final AlipayPaymentGuard alipayPaymentGuard = new AlipayPaymentGuard();
    private final Map<AppSection, AssistantPersistence.Store> customAssistantStores = new EnumMap<>(AppSection.class);
    private final Runnable alipayPaymentPoll = new Runnable() {
        @Override
        public void run() {
            pollPendingAlipayPayment(false);
        }
    };
    private final Runnable streamScrollTick = new Runnable() {
        @Override
        public void run() {
            if (!streamResponseActive || streamScrollPausedByUser || recyclerView == null || !currentSection.isConversation()) {
                streamScrollRunning = false;
                return;
            }
            if (!recyclerView.canScrollVertically(1)) {
                streamScrollRunning = false;
                return;
            }
            streamScrollProgrammatic = true;
            recyclerView.scrollBy(0, Math.max(2, UiKit.dp(MainActivity.this, 2)));
            mainHandler.postDelayed(() -> streamScrollProgrammatic = false, 40);
            mainHandler.postDelayed(this, STREAM_SCROLL_INTERVAL_MS);
        }
    };
    private final Runnable resumeStreamScrollIfAtBottom = () -> {
        if (!streamResponseActive || recyclerView == null) return;
        if (!recyclerView.canScrollVertically(1)) {
            streamScrollPausedByUser = false;
            requestStreamAutoScroll();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new AppPrefs(this);
        apiClient = new ApiClient(prefs);
        criticalApiClient = new ApiClient(prefs);
        userActionApiClient = new ApiClient(prefs);
        desktopApiClient = new ApiClient(prefs);
        panelDataStore = new PanelDataStore(this);
        chatController = new ChatController(userActionApiClient);
        imageController = new ImageController(userActionApiClient);
        appApiKeyManager = new AppApiKeyManager(prefs);
        attachmentReader = new AttachmentContentReader(getContentResolver());
        audioTranscriptionController = new AudioTranscriptionController(userActionApiClient);
        desktopController = new DesktopController(desktopApiClient);
        desktopRepository = new DesktopRepository(desktopApiClient);
        statusController = new StatusController(criticalApiClient);
        billingController = new BillingController(criticalApiClient);
        conversationRepository = new ConversationRepository(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        seedState();
        loadSavedModelSelections();
        loadSavedCustomAssistants();
        buildShell();
        applySystemBars();
        loadStoredLocalSessions(AppSection.CHAT);
        loadStoredLocalSessions(AppSection.IMAGE);
        loadStoredLocalSessions(AppSection.CODEX);
        loadStoredLocalSessions(AppSection.CLAUDE);
        switchSection(AppSection.CHAT);
        refreshModels();
        refreshAssistants();
        validateBoundDesktopDevice(false);
        ensureAppApiKeyIfLoggedIn();
        mainHandler.postDelayed(desktopPoll, 8000);
        if (!prefs.isLoggedIn()) {
            mainHandler.postDelayed(() -> showLoginDialog(false), 300);
        } else {
            mainHandler.postDelayed(() -> requireLegalAcceptance(null), 500);
        }
    }

    @Override
    protected void onDestroy() {
        flushConversationSnapshotsBlocking();
        apiClient.cancelActiveRequests();
        criticalApiClient.cancelActiveRequests();
        userActionApiClient.cancelActiveRequests();
        desktopApiClient.cancelActiveRequests();
        mainHandler.removeCallbacks(desktopPoll);
        mainHandler.removeCallbacks(alipayPaymentPoll);
        stopVoiceRecognition();
        setDesktopKeepScreenOn(false);
        desktopJobMonitor.shutdownNow();
        criticalNetwork.shutdownNow();
        userActionNetwork.shutdownNow();
        desktopNetwork.shutdownNow();
        backgroundNetwork.shutdownNow();
        persistence.shutdownNow();
        super.onDestroy();
    }

    private void executeNetwork(NetworkRequestPolicy.Priority priority, Runnable task) {
        if (task == null) return;
        switch (priority == null ? NetworkRequestPolicy.Priority.BACKGROUND : priority) {
            case CRITICAL:
                criticalNetwork.execute(task);
                break;
            case USER_ACTION:
                userActionNetwork.execute(task);
                break;
            case DESKTOP_SYNC:
                desktopNetwork.execute(task);
                break;
            case BACKGROUND:
            default:
                backgroundNetwork.execute(task);
                break;
        }
    }

    private void applySystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(UiKit.systemBar(this));
        window.setNavigationBarColor(UiKit.systemBar(this));
        if (drawerMenuButton != null) {
            drawerMenuButton.setTextColor(UiKit.ink(this));
        }
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (UiKit.darkTheme(this)) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                if (UiKit.darkTheme(this)) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        flowBackgroundView = new FlowBackgroundView(this);
        root.addView(flowBackgroundView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(new FrostedOverlayView(this), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout main = UiKit.vertical(this);
        main.setClipToPadding(false);
        main.setClipChildren(false);
        main.setPadding(0, 0, 0, 0);
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = UiKit.horizontal(this);
        aiChatHeader = header;
        header.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 10), UiKit.dp(this, 18), UiKit.dp(this, 6));
        header.setBackground(new ColorDrawable(Color.TRANSPARENT));
        LinearLayout topNav = UiKit.horizontal(this);
        aiChatTopNav = topNav;
        addNav(topNav, AppSection.CHAT, R.drawable.nav_chat);
        addHeaderDot(topNav);
        addNav(topNav, AppSection.IMAGE, R.drawable.nav_image);
        addHeaderDot(topNav);
        addNav(topNav, AppSection.CODEX, R.drawable.nav_codex);
        addHeaderDot(topNav);
        addNav(topNav, AppSection.CLAUDE, R.drawable.nav_claude);
        addHeaderDot(topNav);
        header.addView(topNav, new LinearLayout.LayoutParams(0, UiKit.dp(this, 42), 1f));
        Button menu = UiKit.ghostButton(this, "☰");
        drawerMenuButton = menu;
        menu.setContentDescription("打开菜单");
        menu.setTextSize(20);
        menu.setBackground(new ColorDrawable(Color.TRANSPARENT));
        menu.setOnClickListener(v -> openDrawer());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(UiKit.dp(this, 42), UiKit.dp(this, 42));
        menuLp.setMargins(UiKit.dp(this, 8), 0, 0, 0);
        header.addView(menu, menuLp);
        titleView = UiKit.text(this, "", UiKit.MUTED, 1);
        titleView.setVisibility(View.GONE);
        FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        headerLp.topMargin = UiKit.dp(this, 34);
        root.addView(header, headerLp);

        contentFrame = new FrameLayout(this);
        contentFrame.setClipToPadding(false);
        contentFrame.setClipChildren(false);
        main.addView(contentFrame, new LinearLayout.LayoutParams(-1, 0, 1f));

        pageScroll = new ScrollView(this);
        pageScroll.setClipToPadding(false);
        pageContent = UiKit.vertical(this);
        pageContent.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 92), UiKit.dp(this, 18), UiKit.dp(this, 18));
        pageScroll.addView(pageContent, new ScrollView.LayoutParams(-1, -2));
        contentFrame.addView(pageScroll, new FrameLayout.LayoutParams(-1, -1));

        recyclerView = new RecyclerView(this);
        recyclerView.setClipToPadding(true);
        recyclerView.setClipChildren(true);
        recyclerView.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 92), UiKit.dp(this, 26), UiKit.dp(this, 8));
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(this, new ConversationAdapter.Listener() {
            @Override
            public void onEdit(ConversationDisplayItem item) {
                editMessage(item);
            }

            @Override
            public void onDelete(ConversationDisplayItem item) {
                deleteMessage(item);
            }
        });
        recyclerView.setAdapter(adapter);
        contentFrame.addView(recyclerView, new FrameLayout.LayoutParams(-1, -1));

        scrollDock = new ScrollDock(this);
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(UiKit.dp(this, 24), -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        dockLp.rightMargin = UiKit.dp(this, 3);
        contentFrame.addView(scrollDock, dockLp);
        scrollDock.bind(recyclerView, contentFrame);

        composerView = new ComposerView(this);
        composerView.setListener(new ComposerView.Listener() {
            @Override
            public void onSend(String text) {
                sendMessage(text);
            }

            @Override
            public void onStop() {
                stopActiveSend();
            }

            @Override
            public void onHistory() {
                showSessionDialog();
            }

            @Override
            public void onModel() {
                showModelDialog();
            }

            @Override
            public void onAssistant() {
                showAssistantDialog();
            }

            @Override
            public void onImageSize() {
                showImageSizeDialog();
            }

            @Override
            public void onImageQuality() {
                showImageQualityDialog();
            }

            @Override
            public void onReasoning() {
                showReasoningDialog();
            }

            @Override
            public void onContext() {
                showContextDialog();
            }

            @Override
            public void onAttach() {
                pickImage();
            }

            @Override
            public void onSkill() {
                showSkillDialog();
            }

            @Override
            public void onPermissionToggle() {
                syncDesktopPermissionState();
            }

            @Override
            public void onRemoveSkill(String skill) {
                SectionState state = state();
                if (state != null) {
                    state.selectedSkills.remove(skill);
                    composerView.updateState(composerStateFor(state.current(), state));
                }
            }

            @Override
            public void onPreviewAttachment(Uri uri) {
                previewAttachment(uri);
            }

            @Override
            public void onRemoveAttachment(Uri uri) {
                selectedAttachments.remove(uri);
                updateAttachmentPreview();
            }

            @Override
            public void onVoiceMic() {
                toggleVoiceRecognition();
            }
        });
        main.addView(composerView, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        applyConversationChromeForOrientation();
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> adjustForKeyboard(root));
        recyclerView.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, android.view.MotionEvent event) {
                if (streamResponseActive && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    pauseStreamAutoScroll();
                }
                if (keyboardOpen && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    hideKeyboard();
                }
                return false;
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int newState) {
                if (!streamResponseActive) return;
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    pauseStreamAutoScroll();
                    return;
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mainHandler.removeCallbacks(resumeStreamScrollIfAtBottom);
                    mainHandler.postDelayed(resumeStreamScrollIfAtBottom, STREAM_SCROLL_RESUME_MS);
                }
            }

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if (!streamResponseActive || streamScrollProgrammatic) return;
                mainHandler.removeCallbacks(resumeStreamScrollIfAtBottom);
                mainHandler.postDelayed(resumeStreamScrollIfAtBottom, STREAM_SCROLL_RESUME_MS);
            }
        });
        pageScroll.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View focus = getCurrentFocus();
            if (manager != null && focus != null) {
                manager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                focus.clearFocus();
            }
        } catch (Exception ignored) {
        }
    }

    private void toggleVoiceRecognition() {
        if (voiceListening || (audioTranscriptionController != null && audioTranscriptionController.isRunning())) {
            stopVoiceRecognition();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        hideKeyboard();
        composerView.setInputText("");
        composerView.setVoiceListening(true);
        executeNetwork(NetworkRequestPolicy.Priority.USER_ACTION, () -> {
            try {
                audioTranscriptionController.start(this, new AudioTranscriptionController.Callback() {
                    @Override
                    public void onStatus(String status) {
                        // Keep ASR status silent; the microphone animation is the only in-flow feedback.
                    }

                    @Override
                    public void onReady() {
                        mainHandler.post(() -> {
                            voiceListening = true;
                            composerView.setVoiceListening(true);
                        });
                    }

                    @Override
                    public void onText(String text, boolean fin) {
                        mainHandler.post(() -> composerView.setInputText(text));
                    }

                    @Override
                    public void onLevel(float level) {
                        mainHandler.post(() -> composerView.setVoiceLevel(level));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> {
                            voiceListening = false;
                            composerView.setVoiceListening(false);
                        });
                    }

                    @Override
                    public void onAutoSubmit(String text) {
                        mainHandler.post(() -> {
                            String clean = text == null ? "" : text.trim();
                            if (clean.isEmpty()) return;
                            composerView.setInputText(clean);
                        });
                    }

                    @Override
                    public void onClosed() {
                        mainHandler.post(() -> {
                            voiceListening = false;
                            composerView.setVoiceListening(false);
                        });
                    }
                });
            } catch (Exception error) {
                mainHandler.post(() -> {
                    voiceListening = false;
                    composerView.setVoiceListening(false);
                });
            }
        });
    }

    private void stopVoiceRecognition() {
        String text = audioTranscriptionController == null ? "" : audioTranscriptionController.stop();
        voiceListening = false;
        if (composerView != null) composerView.setVoiceListening(false);
        if (composerView != null && text != null && !text.trim().isEmpty()) composerView.setInputText(text.trim());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "未授予录音权限，无法使用语音输入", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void adjustForKeyboard(View root) {
        if (landscapeConversationChromeHidden) {
            keyboardOpen = false;
            lastKeyboardInset = 0;
            if (composerView != null) {
                composerView.setTranslationY(0);
                composerView.setKeyboardOpen(false);
            }
            if (contentFrame != null) contentFrame.setPadding(0, 0, 0, 0);
            return;
        }
        Rect visible = new Rect();
        root.getWindowVisibleDisplayFrame(visible);
        int[] pos = new int[2];
        root.getLocationOnScreen(pos);
        int visibleBottom = visible.bottom - pos[1];
        int keyboard = Math.max(0, root.getHeight() - visibleBottom);
        boolean open = keyboard > UiKit.dp(this, 120);
        int inset = open ? keyboard : 0;
        if (open == keyboardOpen && Math.abs(inset - lastKeyboardInset) < UiKit.dp(this, 6)) {
            return;
        }
        int previousInset = Math.max(0, lastKeyboardInset);
        int scrollDelta = inset - previousInset;
        int boundedScrollDelta = scrollDelta;
        if (!open && scrollDelta < 0 && recyclerView != null) {
            int remainingBelow = Math.max(0, recyclerView.computeVerticalScrollRange()
                    - recyclerView.computeVerticalScrollExtent()
                    - recyclerView.computeVerticalScrollOffset());
            boundedScrollDelta = -Math.min(-scrollDelta, remainingBelow);
        }
        keyboardOpen = open;
        lastKeyboardInset = inset;
        if (open) {
            composerView.setTranslationY(-inset);
            composerView.setKeyboardOpen(true);
            contentFrame.setPadding(0, 0, 0, inset);
        } else {
            composerView.setTranslationY(0);
            composerView.setKeyboardOpen(false);
            contentFrame.setPadding(0, 0, 0, 0);
        }
        if (recyclerView != null && currentSection.isConversation() && boundedScrollDelta != 0) {
            int finalScrollDelta = boundedScrollDelta;
            recyclerView.post(() -> recyclerView.scrollBy(0, finalScrollDelta));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            if (pendingCameraUri != null) {
                addPickedAttachments(Arrays.asList(pendingCameraUri), 1);
                pendingCameraUri = null;
            }
            return;
        }
        if ((requestCode == REQ_IMAGE || requestCode == REQ_GALLERY || requestCode == REQ_DOCUMENT) && resultCode == RESULT_OK && data != null) {
            List<Uri> picked = new ArrayList<>();
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) picked.add(uri);
                }
            } else if (data.getData() != null) {
                picked.add(data.getData());
            }
            addPickedAttachments(picked, currentSection == AppSection.CHAT ? 5 : 1);
        }
    }

    private void addPickedAttachments(List<Uri> picked, int limit) {
        if (picked == null || picked.isEmpty()) return;
        if (currentSection == AppSection.IMAGE) selectedAttachments.clear();
        for (Uri uri : picked) {
            if (uri == null || selectedAttachments.size() >= limit) break;
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            selectedAttachments.add(uri);
        }
        updateAttachmentPreview();
        Toast.makeText(this, "已选择 " + selectedAttachments.size() + " 个附件", Toast.LENGTH_SHORT).show();
    }

    private void pickImage() {
        showAttachmentPicker();
    }

    private void showAttachmentPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "添加附件"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        LinearLayout choices = UiKit.horizontal(this);
        choices.setGravity(Gravity.CENTER);
        choices.addView(attachmentChoice(R.drawable.ic_attach_camera, "拍照", () -> {
            dialog.dismiss();
            openCameraPicker();
        }), new LinearLayout.LayoutParams(0, UiKit.dp(this, 64), 1f));
        View gap = new View(this);
        choices.addView(gap, new LinearLayout.LayoutParams(UiKit.dp(this, 14), 1));
        choices.addView(attachmentChoice(R.drawable.ic_attach_file, "文件", () -> {
            dialog.dismiss();
            openDocumentPicker("*/*", currentSection == AppSection.CHAT, REQ_DOCUMENT);
        }), new LinearLayout.LayoutParams(0, UiKit.dp(this, 64), 1f));
        panel.addView(choices, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 74)));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private View attachmentChoice(int icon, String desc, Runnable action) {
        FrameLayout box = new FrameLayout(this);
        box.setBackground(UiKit.round(UiKit.chipFill(this, false), UiKit.dp(this, 18), UiKit.line(this)));
        box.setContentDescription(desc);
        ImageButton button = UiKit.imageButton(this, icon, desc);
        button.setClickable(false);
        button.setFocusable(false);
        button.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12));
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(UiKit.dp(this, 52), UiKit.dp(this, 52), Gravity.CENTER);
        box.addView(button, iconLp);
        box.setOnClickListener(v -> action.run());
        return box;
    }

    private void openDocumentPicker(String type, boolean multiple, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    private void openCameraPicker() {
        try {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "capture");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "oneapi-camera-" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (Exception error) {
            Toast.makeText(this, "无法打开相机：" + message(error), Toast.LENGTH_LONG).show();
        }
    }

    private void previewAttachment(Uri uri) {
        String type = getContentResolver().getType(uri);
        String name = displayName(uri).toLowerCase(Locale.ROOT);
        if ((type != null && type.startsWith("image/")) || name.endsWith(".heic") || name.endsWith(".heif")) {
            showImageAttachmentPreview(uri);
            return;
        }
        if (name.endsWith(".pdf") || "application/pdf".equals(type)) {
            if (showPdfAttachmentPreview(uri)) return;
        }
        if (name.endsWith(".xlsx") || name.endsWith(".xlsm") || name.endsWith(".xls")) {
            String sheet = readXlsxAttachment(uri, 200);
            if (sheet != null && !sheet.trim().isEmpty()) {
                showTextAttachmentPreview(displayName(uri), sheet, false);
                return;
            }
        }
        String text = readTextAttachment(uri, 60000);
        if (text != null) {
            showTextAttachmentPreview(displayName(uri), text, name.endsWith(".md") || name.endsWith(".markdown"));
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, type == null ? "*/*" : type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "打开附件"));
        } catch (Exception error) {
            Toast.makeText(this, "无法预览附件：" + message(error), Toast.LENGTH_LONG).show();
        }
    }

    private void showImageAttachmentPreview(Uri uri) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, displayName(uri)), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        ImageView image = new ImageView(this);
        image.setImageURI(uri);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        panel.addView(image, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 420)));
        overlay.addView(panel, plainCenteredModalLp());
        overlay.setTag("plain_modal");
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void showTextAttachmentPreview(String title, String text, boolean markdown) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, title), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        ScrollView scroll = new ScrollView(this);
        if (markdown) {
            LinearLayout body = UiKit.vertical(this);
            new center.oneapi.mobile.ui.markdown.MarkdownViews(this).renderInto(body, text, null);
            scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        } else {
            TextView body = UiKit.text(this, text, UiKit.INK, 13);
            body.setTextIsSelectable(true);
            scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        }
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 420)));
        overlay.addView(panel, plainCenteredModalLp());
        overlay.setTag("plain_modal");
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private boolean showPdfAttachmentPreview(Uri uri) {
        try {
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r");
            if (fd == null) return false;
            PdfRenderer renderer = new PdfRenderer(fd);
            if (renderer.getPageCount() <= 0) {
                renderer.close();
                fd.close();
                return false;
            }
            PdfRenderer.Page page = renderer.openPage(0);
            int maxWidth = Math.max(UiKit.dp(this, 260), getResources().getDisplayMetrics().widthPixels - UiKit.dp(this, 64));
            float ratio = page.getWidth() / (float) Math.max(1, page.getHeight());
            int width = maxWidth;
            int height = Math.max(UiKit.dp(this, 240), (int) (width / Math.max(0.35f, ratio)));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();
            fd.close();
            showBitmapAttachmentPreview(displayName(uri), bitmap);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showBitmapAttachmentPreview(String title, Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, title), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        panel.addView(image, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 420)));
        overlay.addView(panel, plainCenteredModalLp());
        overlay.setTag("plain_modal");
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void updateAttachmentPreview() {
        if (composerView != null) {
            composerView.updateAttachments(new ArrayList<>(selectedAttachments));
        }
    }

    private String displayName(Uri uri) {
        return attachmentReader.displayName(uri);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyConversationChromeForOrientation();
        renderCurrent(false);
    }

    private String readTextAttachment(Uri uri, int limit) {
        return attachmentReader.readText(uri, limit);
    }

    private String readableAttachmentContent(Uri uri, int limit) {
        return attachmentReader.readableContent(uri, limit);
    }

    private boolean isImageAttachment(Uri uri) {
        return attachmentReader.isImage(uri);
    }

    private boolean isPdfAttachment(Uri uri) {
        return attachmentReader.isPdf(uri);
    }

    private String imageDataUrl(Uri uri) throws Exception {
        return attachmentReader.imageDataUrl(uri);
    }

    private String pdfFirstPageDataUrl(Uri uri) {
        return attachmentReader.pdfFirstPageDataUrl(uri);
    }

    private byte[] readAttachmentBytes(Uri uri, int limit) throws Exception {
        return attachmentReader.readBytes(uri, limit);
    }

    private String readXlsxAttachment(Uri uri, int maxRows) {
        return attachmentReader.readXlsx(uri, maxRows);
    }
    private void addNav(LinearLayout nav, AppSection section, int icon) {
        LinearLayout wrapper = UiKit.vertical(this);
        wrapper.setGravity(Gravity.CENTER);
        ImageButton button = UiKit.imageButton(this, icon, section.label);
        button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        applyTopNavIconColor(section, button);
        button.setOnClickListener(v -> switchSection(section));
        wrapper.setOnClickListener(v -> switchSection(section));
        wrapper.addView(button, new LinearLayout.LayoutParams(-1, 0, 1f));
        View line = new View(this);
        line.setBackground(UiKit.round(Color.TRANSPARENT, UiKit.dp(this, 2), Color.TRANSPARENT));
        wrapper.addView(line, new LinearLayout.LayoutParams(UiKit.dp(this, 24), UiKit.dp(this, 3)));
        navButtons.put(section, wrapper);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f);
        lp.setMargins(UiKit.dp(this, 3), 0, UiKit.dp(this, 3), 0);
        nav.addView(wrapper, lp);
    }

    private void addHeaderDot(LinearLayout nav) {
        TextView dot = UiKit.text(this, "·", UiKit.MUTED, 18);
        dot.setGravity(Gravity.CENTER);
        nav.addView(dot, new LinearLayout.LayoutParams(UiKit.dp(this, 12), UiKit.dp(this, 38)));
    }

    private void applyTopNavIconColor(AppSection section, ImageButton icon) {
        if (section != null && section.isDesktop() && !desktopBridgeConnected) {
            icon.setColorFilter(UiKit.muted(this));
            return;
        }
        if (UiKit.darkTheme(this)) {
            icon.setColorFilter(UiKit.ink(this));
        } else {
            icon.clearColorFilter();
        }
    }

    private void setDesktopBridgeConnected(boolean connected) {
        if (desktopBridgeConnected == connected) return;
        desktopBridgeConnected = connected;
        refreshTopNavIconColors();
    }

    private void refreshTopNavIconColors() {
        for (Map.Entry<AppSection, LinearLayout> entry : navButtons.entrySet()) {
            LinearLayout wrapper = entry.getValue();
            if (wrapper == null || wrapper.getChildCount() == 0) continue;
            View child = wrapper.getChildAt(0);
            if (child instanceof ImageButton) {
                applyTopNavIconColor(entry.getKey(), (ImageButton) child);
            }
        }
    }

    private void switchSection(AppSection section) {
        switchSection(section, true);
    }

    private void switchSection(AppSection section, boolean forceBottom) {
        AppSection previousSection = currentSection;
        if (previousSection.isConversation()) {
            lastAiChatSection = previousSection;
        }
        if (currentSection != section && currentSection.isConversation() && section.isConversation()) {
            selectedAttachments.clear();
            updateAttachmentPreview();
        }
        currentSection = section;
        if (section.isConversation()) {
            lastAiChatSection = section;
        }
        for (Map.Entry<AppSection, LinearLayout> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey() == section;
            LinearLayout wrapper = entry.getValue();
            ImageButton icon = (ImageButton) wrapper.getChildAt(0);
            View line = wrapper.getChildAt(1);
            wrapper.setBackground(new ColorDrawable(Color.TRANSPARENT));
            icon.setBackground(new ColorDrawable(Color.TRANSPARENT));
            applyTopNavIconColor(entry.getKey(), icon);
            line.setBackground(UiKit.round(selected ? UiKit.blue(this) : Color.TRANSPARENT, UiKit.dp(this, 2), Color.TRANSPARENT));
        }
        boolean conversation = section.isConversation();
        applySystemBars();
        if (aiChatHeader != null) aiChatHeader.setBackground(new ColorDrawable(Color.TRANSPARENT));
        recyclerView.setVisibility(conversation ? View.VISIBLE : View.GONE);
        pageScroll.setVisibility(conversation ? View.GONE : View.VISIBLE);
        scrollDock.setVisibility(conversation ? View.VISIBLE : View.GONE);
        applyConversationChromeForOrientation();
        renderCurrent(forceBottom);
        if (section.isDesktop()) {
            maybeRefreshDesktopSessions(section, false);
        }
    }

    private void renderCurrent(boolean forceBottom) {
        if (!currentSection.isConversation()) {
            renderPage(currentSection);
            return;
        }
        SectionState state = state();
        ChatSession session = state.current();
        composerView.updateState(composerStateFor(session, state));
        if (currentSection.isDesktop()) {
            composerView.setSending(isDesktopSessionBusy(session));
        }
        List<ConversationDisplayItem> items = new ArrayList<>();
        if (currentSection.isDesktop() && prefs.boundDeviceId().isEmpty()) {
            items.add(ConversationDisplayItem.empty("未连接客户端。请先在系统设置中绑定 PC/Mac 客户端，绑定后才能同步会话并发送任务。"));
        } else {
            for (int i = 0; i < session.messages.size(); i++) {
                ChatMessage message = session.messages.get(i);
                if (message.log) {
                    items.add(ConversationDisplayItem.log(message.text, message.timestamp, i));
                } else {
                    String text = currentSection.isDesktop() && "assistant".equals(message.role) ? desktopAttachmentTags(message.text) : message.text;
                    items.add(ConversationDisplayItem.message(message.role, text, message.timestamp, message.tokenText, i));
                }
            }
        }
        if (items.isEmpty()) {
            items.add(ConversationDisplayItem.empty("暂无消息"));
        }
        adapter.submitList(items, () -> {
            if (forceBottom) {
                scrollToBottomNow();
            } else if (streamResponseActive && !streamScrollPausedByUser) {
                requestStreamAutoScroll();
            }
        });
    }

    private void loadStoredLocalSessions(AppSection section) {
        if (!section.isConversation()) return;
        persistence.execute(() -> {
            try {
                List<ConversationSessionEntity> rows = conversationRepository.sessionsForMode(section.id);
                if (rows.isEmpty()) return;
                List<ChatSession> sessions = new ArrayList<>();
                for (ConversationSessionEntity row : rows) {
                    List<ChatMessage> messages = new ArrayList<>();
                    List<ConversationMessageEntity> storedMessages = conversationRepository.latestMessages(row.sessionId, 80);
                    for (ConversationMessageEntity item : storedMessages) {
                        ChatMessage message = "log".equals(item.kind)
                                ? ChatMessage.log(item.text, item.timestamp)
                                : new ChatMessage(item.role, item.text, item.timestamp);
                        message.tokenText = storedTokenText(item.rawJson);
                        messages.add(message);
                    }
                    ConversationPersistenceSnapshot.repairInterruptedMessages(messages);
                    sessions.add(new ChatSession(
                            row.sessionId,
                            row.title.isEmpty() ? row.groupName : row.title,
                            section.isDesktop() ? section.label : row.groupName,
                            row.projectName,
                            messages
                    ));
                }
                SectionState target = states.get(section);
                mainHandler.post(() -> {
                    if (target == null) return;
                    target.sessions.clear();
                    target.sessions.addAll(sessions);
                    target.selectedIndex = 0;
                    target.selectedSessionId = sessions.isEmpty() ? "" : sessions.get(0).id;
                    if (currentSection == section) {
                        renderCurrent(true);
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void persistSession(AppSection section, ChatSession session) {
        if (!section.isConversation()) return;
        ChatSession snapshot = snapshotSession(session);
        persistence.execute(() -> {
            try {
                replaceStoredSession(section, snapshot);
            } catch (Exception ignored) {
            }
        });
    }

    private void persistSessions(AppSection section, List<ChatSession> sessions) {
        if (!section.isConversation()) return;
        List<ChatSession> snapshots = snapshotSessions(sessions);
        persistence.execute(() -> {
            try {
                replaceStoredSessions(section, snapshots);
            } catch (Exception ignored) {
            }
        });
    }

    private void flushConversationSnapshotsBlocking() {
        Map<AppSection, List<ChatSession>> snapshots = new EnumMap<>(AppSection.class);
        for (Map.Entry<AppSection, SectionState> entry : states.entrySet()) {
            AppSection section = entry.getKey();
            if (section == null || !section.isConversation() || entry.getValue() == null) continue;
            snapshots.put(section, snapshotSessions(entry.getValue().sessions));
        }
        Future<?> flush = persistence.submit(() -> {
            for (Map.Entry<AppSection, List<ChatSession>> entry : snapshots.entrySet()) {
                try {
                    replaceStoredSessions(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {
                }
            }
        });
        try {
            flush.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private void applyConversationChromeForOrientation() {
        boolean conversation = currentSection != null && currentSection.isConversation();
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        landscapeConversationChromeHidden = conversation && landscape;
        if (aiChatHeader != null) {
            aiChatHeader.setVisibility(conversation && landscape ? View.GONE : View.VISIBLE);
        }
        if (aiChatTopNav != null) {
            aiChatTopNav.setVisibility(conversation && !landscape ? View.VISIBLE : View.INVISIBLE);
        }
        if (composerView != null) {
            composerView.setVisibility(conversation && !landscape ? View.VISIBLE : View.GONE);
            composerView.setTranslationY(0);
            composerView.setKeyboardOpen(false);
        }
        if (recyclerView != null) {
            int top = conversation && landscape ? UiKit.dp(this, 12) : UiKit.dp(this, 92);
            int bottom = conversation && landscape ? UiKit.dp(this, 12) : UiKit.dp(this, 8);
            recyclerView.setPadding(UiKit.dp(this, 12), top, UiKit.dp(this, 26), bottom);
        }
        if (pageContent != null) {
            int top = conversation && landscape ? UiKit.dp(this, 18) : UiKit.dp(this, 92);
            pageContent.setPadding(UiKit.dp(this, 18), top, UiKit.dp(this, 18), UiKit.dp(this, 18));
        }
    }

    private List<ChatSession> snapshotSessions(List<ChatSession> sessions) {
        List<ChatSession> out = new ArrayList<>();
        if (sessions == null) return out;
        for (ChatSession session : sessions) {
            ChatSession snapshot = snapshotSession(session);
            if (snapshot != null) out.add(snapshot);
        }
        return out;
    }

    private ChatSession snapshotSession(ChatSession session) {
        if (session == null) return null;
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatMessage source : session.messages) {
            if (source == null) continue;
            ChatMessage copy = source.log ? ChatMessage.log(source.text, source.timestamp) : new ChatMessage(source.role, source.text, source.timestamp);
            copy.tokenText = source.tokenText;
            messages.add(copy);
        }
        ChatSession snapshot = new ChatSession(session.id, session.title, session.assistantLabel, session.projectLabel, messages);
        snapshot.status = session.status;
        return snapshot;
    }

    private void replaceStoredSessions(AppSection section, List<ChatSession> sessions) {
        List<ConversationSessionEntity> entities = new ArrayList<>();
        List<List<ConversationMessageEntity>> messagesBySession = new ArrayList<>();
        if (sessions != null) {
            for (ChatSession session : sessions) {
                if (session == null) continue;
                StoredConversation stored = storedConversation(section, session);
                entities.add(stored.session);
                messagesBySession.add(stored.messages);
            }
        }
        conversationRepository.replaceModeSessions(section.id, entities, messagesBySession);
    }

    private void replaceStoredSession(AppSection section, ChatSession session) {
        if (section == null || session == null) return;
        StoredConversation stored = storedConversation(section, session);
        conversationRepository.replaceSessionMessages(stored.session, stored.messages);
    }

    private StoredConversation storedConversation(AppSection section, ChatSession session) {
        ConversationSessionEntity entity = new ConversationSessionEntity();
        entity.sessionId = session.id;
        entity.mode = section.id;
        entity.groupName = session.assistantLabel;
        entity.title = session.title;
        entity.projectName = session.projectLabel;
        entity.projectPath = session.projectLabel;
        entity.updatedAt = newestMessageTimestamp(session);
        entity.preview = session.messages.isEmpty() ? "" : session.messages.get(session.messages.size() - 1).text;
        List<ConversationMessageEntity> messages = new ArrayList<>();
        for (int i = 0; i < session.messages.size(); i++) {
            ChatMessage source = session.messages.get(i);
            ConversationMessageEntity message = new ConversationMessageEntity();
            message.messageId = session.id + "-" + i + "-" + source.timestamp;
            message.sessionId = session.id;
            message.mode = section.id;
            message.kind = source.log ? "log" : "message";
            message.role = source.role;
            message.contentType = "text";
            message.text = source.text;
            message.timestamp = source.timestamp;
            message.sortIndex = i;
            message.rawJson = messageRawJson(source);
            messages.add(message);
        }
        return new StoredConversation(entity, messages);
    }

    private static final class StoredConversation {
        final ConversationSessionEntity session;
        final List<ConversationMessageEntity> messages;

        StoredConversation(ConversationSessionEntity session, List<ConversationMessageEntity> messages) {
            this.session = session;
            this.messages = messages;
        }
    }

    private long newestMessageTimestamp(ChatSession session) {
        long newest = System.currentTimeMillis();
        if (session == null || session.messages == null) return newest;
        for (ChatMessage message : session.messages) {
            if (message != null && message.timestamp > newest) newest = message.timestamp;
        }
        return newest;
    }

    private String messageRawJson(ChatMessage message) {
        if (message == null || message.tokenText == null || message.tokenText.trim().isEmpty()) return "";
        try {
            return new JSONObject().put("token_text", message.tokenText.trim()).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String storedTokenText(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) return "";
        try {
            return new JSONObject(rawJson).optString("token_text", "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private void openDrawer() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(UiKit.overlayTint(this));
        LinearLayout panel = UiKit.vertical(this);
        panel.setBackground(UiKit.glass(this, UiKit.dp(this, 18), UiKit.line(this)));
        panel.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 12), UiKit.dp(this, 20), UiKit.dp(this, 12));
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "菜单"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 34), UiKit.dp(this, 34)));
        panel.addView(titleRow);
        panel.addView(UiKit.gap(this, 8));
        addDrawerEntry(panel, "AI Chat", AppSection.CHAT, dialog);
        addDrawerEntry(panel, "套餐订阅", AppSection.SUBSCRIPTIONS, dialog);
        addDrawerEntry(panel, "服务状态", AppSection.SERVICE, dialog);
        addDrawerEntry(panel, "我的钱包", AppSection.WALLET, dialog);
        addDrawerEntry(panel, "系统设置", AppSection.SETTINGS, dialog);
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(UiKit.dp(this, 236), -2, Gravity.RIGHT | Gravity.TOP);
        panelLp.setMargins(0, UiKit.dp(this, 70), UiKit.dp(this, 14), 0);
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
            configureFullscreenOverlayWindow(shown);
            shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            applyWindowBlur(shown);
        }
    }

    private void configureFullscreenOverlayWindow(Window window) {
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void adjustAnchoredDialog(FrameLayout overlay) {
        View anchor = composerView == null ? null : composerView.lastActionAnchor();
        if (anchor == null || !anchor.isShown() || overlay.getChildCount() == 0) return;
        View panel = overlay.getChildAt(0);
        if (!(panel.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        int[] pos = new int[2];
        anchor.getLocationOnScreen(pos);
        int maxHeight = Math.max(UiKit.dp(this, 180), overlay.getHeight() - UiKit.dp(this, 36));
        panel.measure(
                View.MeasureSpec.makeMeasureSpec(Math.max(1, overlay.getWidth() - UiKit.dp(this, 32)), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));
        shrinkScrollContentIfNeeded(panel, maxHeight);
        panel.measure(
                View.MeasureSpec.makeMeasureSpec(Math.max(1, overlay.getWidth() - UiKit.dp(this, 32)), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));
        int panelHeight = Math.max(panel.getMeasuredHeight(), UiKit.dp(this, 120));
        int gap = Math.max(UiKit.dp(this, 44), anchor.getHeight()) + UiKit.dp(this, 12);
        int top = pos[1] - panelHeight - gap;
        if (top < UiKit.dp(this, 18)) top = pos[1] + anchor.getHeight() + gap;
        int maxTop = Math.max(UiKit.dp(this, 18), overlay.getHeight() - panelHeight - UiKit.dp(this, 18));
        top = Math.min(top, maxTop);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = top;
        lp.height = panelHeight > maxHeight ? maxHeight : -2;
        panel.setLayoutParams(lp);
        panel.setAlpha(1f);
    }

    private void shrinkScrollContentIfNeeded(View panel, int maxHeight) {
        if (panel.getMeasuredHeight() <= maxHeight || !(panel instanceof ViewGroup)) return;
        int overflow = panel.getMeasuredHeight() - maxHeight + UiKit.dp(this, 12);
        ScrollView scroll = firstScrollView((ViewGroup) panel);
        if (scroll == null || !(scroll.getLayoutParams() instanceof ViewGroup.LayoutParams)) return;
        ViewGroup.LayoutParams lp = scroll.getLayoutParams();
        if (lp.height <= 0) return;
        lp.height = Math.max(UiKit.dp(this, 120), lp.height - overflow);
        scroll.setLayoutParams(lp);
    }

    private ScrollView firstScrollView(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ScrollView) return (ScrollView) child;
            if (child instanceof ViewGroup) {
                ScrollView nested = firstScrollView((ViewGroup) child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private void addDrawerEntry(LinearLayout panel, String label, AppSection section, Dialog dialog) {
        LinearLayout row = UiKit.horizontal(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(UiKit.dp(this, 10), 0, UiKit.dp(this, 10), 0);
        row.setBackground(UiKit.round(UiKit.itemFill(this, false), UiKit.dp(this, 16), UiKit.line(this)));
        ImageView icon = new ImageView(this);
        icon.setImageResource(drawerIconRes(section));
        icon.setColorFilter(UiKit.ink(this));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(UiKit.dp(this, 5), UiKit.dp(this, 5), UiKit.dp(this, 5), UiKit.dp(this, 5));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(UiKit.dp(this, 30), -1);
        iconLp.setMargins(0, 0, UiKit.dp(this, 14), 0);
        row.addView(icon, iconLp);
        TextView text = UiKit.bold(this, label);
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        row.addView(text, new LinearLayout.LayoutParams(-2, -1));
        row.setOnClickListener(v -> {
            dialog.dismiss();
            if (section == AppSection.CHAT) {
                switchSection(lastAiChatSection, false);
            } else {
                switchSection(section, true);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 46));
        lp.setMargins(0, UiKit.dp(this, 10), 0, 0);
        panel.addView(row, lp);
    }

    private int drawerIconRes(AppSection section) {
        if (section == AppSection.SUBSCRIPTIONS) return R.drawable.drawer_shop;
        if (section == AppSection.SERVICE) return R.drawable.drawer_cloud;
        if (section == AppSection.WALLET) return R.drawable.drawer_wallet;
        if (section == AppSection.SETTINGS) return R.drawable.drawer_setting;
        return R.drawable.drawer_aichat;
    }

    private void renderPage(AppSection section) {
        pageContent.removeAllViews();
        pageContent.addView(sectionTitle(section.label));
        if (section == AppSection.SETTINGS) {
            renderSettingsPage();
        } else if (section == AppSection.WALLET) {
            renderWalletPage();
        } else if (section == AppSection.SERVICE) {
            renderServicePage();
        } else if (section == AppSection.SUBSCRIPTIONS) {
            renderSubscriptionsPage();
        } else {
            pageContent.addView(card("OneAPI", section.label));
        }
        pageScroll.scrollTo(0, 0);
    }

    private void renderSettingsPage() {
        LinearLayout announcements = cardPanel();
        announcements.addView(UiKit.bold(this, "系统公告"));
        announcements.addView(loadingRow("正在读取系统公告"));
        pageContent.addView(announcements);
        fetchInto(announcements, () -> statusController.announcements(), "系统公告");

        LinearLayout devices = cardPanel();
        devices.addView(UiKit.bold(this, "设备绑定"));
        devices.setTag("devices_card");
        if (!prefs.boundDeviceId().isEmpty()) {
            Button unbind = UiKit.ghostButton(this, "解除绑定");
            unbind.setTextColor(Color.rgb(216, 71, 86));
            unbind.setOnClickListener(v -> unbindDevice(prefs.boundDeviceId()));
            devices.addView(unbind, centeredButtonLp());
        }
        devices.addView(UiKit.text(this,
                prefs.boundDeviceId().isEmpty() ? "未绑定设备" : "正在读取当前绑定设备：" + prefs.boundDeviceId(),
                UiKit.MUTED,
                14));
        Button refresh = UiKit.ghostButton(this, "刷新设备");
        refresh.setOnClickListener(v -> refreshDevices(devices));
        devices.addView(refresh, centeredButtonLp());
        pageContent.addView(devices);
        refreshDevices(devices);

        renderEffectsModule();

        LinearLayout version = cardPanel();
        version.addView(UiKit.bold(this, "版本信息"));
        version.addView(UiKit.text(this, "当前版本：" + versionName(), UiKit.MUTED, 14));
        Button update = UiKit.ghostButton(this, "检查更新");
        update.setOnClickListener(v -> showUpdateDialog());
        version.addView(update, centeredButtonLp());
        pageContent.addView(version);

        LinearLayout account = cardPanel();
        account.addView(UiKit.bold(this, "账户"));
        account.addView(UiKit.text(this,
                prefs.username().isEmpty() ? "当前未记录登录账号" : "当前账号：" + prefs.username(),
                UiKit.MUTED,
                14));
        Button logout = UiKit.ghostButton(this, "退出登录");
        logout.setTextColor(Color.rgb(216, 71, 86));
        logout.setOnClickListener(v -> {
            prefs.clearLogin();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            renderCurrent(false);
            showLoginDialog(false);
        });
        account.addView(logout, centeredButtonLp());
        pageContent.addView(account);

        LinearLayout about = cardPanel();
        about.addView(UiKit.bold(this, "关于与合规"));
        about.addView(UiKit.text(this, "查看用户协议、隐私政策、生成式 AI 服务说明、内容安全规则和投诉举报入口。", UiKit.MUTED, 14));
        Button aboutButton = UiKit.ghostButton(this, "关于");
        aboutButton.setOnClickListener(v -> showLegalCenterDialog(false, null));
        about.addView(aboutButton, centeredButtonLp());
        pageContent.addView(about);
    }

    private void requireLegalAcceptance(Runnable onAccepted) {
        if (prefs == null || !prefs.isLoggedIn()) return;
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                JSONObject envelope = criticalApiClient.get("/api/user/compliance/status");
                JSONObject data = envelope.optJSONObject("data");
                if (data == null) throw new IllegalStateException("合规状态为空");
                String version = data.optString("version", LEGAL_ACCEPTANCE_VERSION).trim();
                if (version.isEmpty()) version = LEGAL_ACCEPTANCE_VERSION;
                JSONObject ban = data.optJSONObject("ban");
                if (ban != null && ban.optBoolean("active", false)) {
                    mainHandler.post(() -> Toast.makeText(this, "账号因内容安全规则被限制使用，请联系管理员复核。", Toast.LENGTH_LONG).show());
                    return;
                }
                if (data.optBoolean("accepted", false)) {
                    prefs.acceptLegal(version);
                    if (onAccepted != null) mainHandler.post(onAccepted);
                } else {
                    mainHandler.post(() -> showLegalCenterDialog(true, onAccepted));
                }
            } catch (Exception error) {
                if (prefs.hasAcceptedLegal(LEGAL_ACCEPTANCE_VERSION)) {
                    if (onAccepted != null) mainHandler.post(onAccepted);
                } else {
                    mainHandler.post(() -> showLegalCenterDialog(true, onAccepted));
                }
            }
        });
    }

    private void showLegalCenterDialog(boolean required, Runnable onAccepted) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(!required);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(UiKit.bold(this, required ? "请阅读并同意协议" : "关于与合规"), new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(titleRow);
        if (required) {
            panel.addView(UiKit.text(this, "首次登录需要签订用户协议和隐私政策。不同意将退出登录，无法进入 App。", UiKit.MUTED, 13));
        }

        String[] titles = new String[]{"用户协议", "隐私政策", "内容安全规则", "投诉举报", "生成式 AI 服务说明"};
        String[] keys = new String[]{"user", "privacy", "safety", "report", "genai"};
        Map<String, String> documents = new LinkedHashMap<>();
        for (String key : keys) documents.put(key, legalFallback(key));

        LinearLayout tabRows = UiKit.vertical(this);
        TextView content = UiKit.text(this, "", UiKit.INK, 14);
        content.setLineSpacing(UiKit.dp(this, 2), 1.0f);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        Button[] tabButtons = new Button[titles.length];
        int[] active = new int[]{0};
        Runnable renderTab = () -> {
            String key = keys[active[0]];
            content.setText(documents.get(key));
            scroll.scrollTo(0, 0);
            for (int i = 0; i < tabButtons.length; i++) {
                Button tab = tabButtons[i];
                if (tab == null) continue;
                tab.setText((i == active[0] ? "✓ " : "") + titles[i]);
            }
        };
        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            Button tab = UiKit.ghostButton(this, titles[i]);
            tab.setTextSize(12);
            tab.setSingleLine(true);
            tab.setMaxLines(1);
            tab.setEllipsize(TextUtils.TruncateAt.END);
            tab.setGravity(Gravity.CENTER);
            tab.setOnClickListener(v -> {
                active[0] = index;
                renderTab.run();
            });
            tabButtons[i] = tab;
        }
        addLegalTabRow(tabRows, titles, tabButtons, new int[]{0, 1});
        addLegalTabRow(tabRows, titles, tabButtons, new int[]{2, 3});
        addLegalTabRow(tabRows, titles, tabButtons, new int[]{4});
        panel.addView(tabRows, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, required ? 260 : 330));
        scrollLp.setMargins(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 8));
        panel.addView(scroll, scrollLp);

        LinearLayout actions = UiKit.horizontal(this);
        if (required) {
            Button reject = UiKit.ghostButton(this, "不同意");
            reject.setTextColor(Color.rgb(216, 71, 86));
            reject.setOnClickListener(v -> {
                dialog.dismiss();
                prefs.clearLogin();
                Toast.makeText(this, "未同意协议，已退出登录", Toast.LENGTH_LONG).show();
                renderCurrent(false);
                showLoginDialog(false);
            });
            actions.addView(reject, new LinearLayout.LayoutParams(0, UiKit.dp(this, 40), 1f));
            Button accept = UiKit.ghostButton(this, "同意并进入");
            accept.setOnClickListener(v -> {
                accept.setEnabled(false);
                accept.setText("确认中...");
                acknowledgeLegalAcceptance(() -> {
                    dialog.dismiss();
                    if (onAccepted != null) onAccepted.run();
                }, () -> {
                    accept.setEnabled(true);
                    accept.setText("同意并进入");
                });
            });
            LinearLayout.LayoutParams acceptLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 40), 1f);
            acceptLp.leftMargin = UiKit.dp(this, 10);
            actions.addView(accept, acceptLp);
        } else {
            Button ok = UiKit.ghostButton(this, "关闭");
            ok.setOnClickListener(v -> dialog.dismiss());
            actions.addView(ok, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 40)));
        }
        panel.addView(actions, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 46)));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> {
            if (!required) dialog.dismiss();
        });
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
        renderTab.run();
        syncLegalDocuments(dialog, documents, keys, active, renderTab);
    }

    private void addLegalTabRow(LinearLayout tabRows, String[] titles, Button[] tabButtons, int[] indexes) {
        LinearLayout tabs = UiKit.horizontal(this);
        tabs.setGravity(Gravity.CENTER);
        for (int index : indexes) {
            if (index < 0 || index >= titles.length || index >= tabButtons.length) continue;
            Button tab = tabButtons[index];
            if (tab == null) continue;
            tabs.addView(tab, new LinearLayout.LayoutParams(0, UiKit.dp(this, 36), 1f));
        }
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = UiKit.dp(this, 6);
        tabRows.addView(tabs, rowLp);
    }

    private void syncLegalDocuments(Dialog dialog, Map<String, String> documents, String[] keys, int[] active, Runnable renderTab) {
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            String userAgreement = fetchLegalDocument("/api/user-agreement");
            String privacyPolicy = fetchLegalDocument("/api/privacy-policy");
            mainHandler.post(() -> {
                if (dialog == null || !dialog.isShowing()) return;
                if (!userAgreement.isEmpty()) documents.put("user", userAgreement);
                if (!privacyPolicy.isEmpty()) documents.put("privacy", privacyPolicy);
                if (keys[active[0]].equals("user") || keys[active[0]].equals("privacy")) {
                    renderTab.run();
                }
            });
        });
    }

    private void acknowledgeLegalAcceptance(Runnable onAccepted, Runnable onFailed) {
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                JSONObject body = new JSONObject();
                body.put("version", LEGAL_ACCEPTANCE_VERSION);
                JSONArray sections = new JSONArray();
                for (String section : LEGAL_ACK_SECTIONS) sections.put(section);
                body.put("sections", sections);
                criticalApiClient.post("/api/user/compliance/acknowledge", body);
                prefs.acceptLegal(LEGAL_ACCEPTANCE_VERSION);
                mainHandler.post(onAccepted);
            } catch (Exception error) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "协议确认失败：" + message(error), Toast.LENGTH_LONG).show();
                    if (onFailed != null) onFailed.run();
                });
            }
        });
    }

    private String fetchLegalDocument(String path) {
        try {
            JSONObject envelope = criticalApiClient.get(path);
            Object data = envelope.opt("data");
            String text = data == null ? "" : String.valueOf(data).trim();
            if (text.startsWith("https://") || text.startsWith("http://")) {
                return "请在浏览器打开以下链接查看：\n" + text;
            }
            return documentToPlainText(text);
        } catch (Exception error) {
            return "";
        }
    }

    private String documentToPlainText(String text) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) return "";
        if (clean.contains("<") && clean.contains(">")) {
            if (Build.VERSION.SDK_INT >= 24) {
                return Html.fromHtml(clean, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            }
            return Html.fromHtml(clean).toString().trim();
        }
        return clean;
    }

    private String legalFallback(String key) {
        if ("user".equals(key)) {
            return "用户协议\n\n"
                    + "账号准入：本服务当前面向人工审核通过或管理员授权的用户开放。用户通过平台公告、AI 开发群或其他指定渠道提交申请后，由管理员根据使用目的、风险情况和运营规则创建或启用账号。\n\n"
                    + "账号安全：用户应保证注册、联系和审核材料真实、合法、可联系，并妥善保管账号、密码、访问令牌、API Key、客户端绑定信息和设备。因泄露、共享、转售、出租、出借、嵌入公开仓库或交由不可信第三方使用导致的损失和责任，由用户自行承担。\n\n"
                    + "使用边界：用户不得转售、分发、出租平台服务或 API Key，不得批量注册、绕过额度限制、破解客户端、伪造请求来源、攻击平台、干扰计费、滥用优惠、抓取非授权数据、规避内容安全或协助他人违反平台规则。\n\n"
                    + "费用与额度：充值金额用于购买平台内部系统货币、额度、套餐或会员服务。实际扣费以服务端订单、支付机构通知、模型计费规则、倍率配置和平台账务记录为准。除法律另有规定或平台明确承诺外，已消耗额度不退还。\n\n"
                    + "API 与客户端：用户可通过网页、桌面客户端、Android App、API 或插件使用服务。用户应自行评估在自动化脚本、生产系统、团队共享设备和第三方工具中保存 API Key 的风险，并为自己的调用行为、费用消耗和下游影响负责。\n\n"
                    + "合规义务：用户应遵守法律法规、公序良俗、平台规则、上游模型服务商政策、开源许可证和第三方 API 规则，不得使用本服务从事违法犯罪、侵权、欺诈、垃圾信息、恶意自动化、未经授权数据抓取、网络攻击、侵犯隐私或其他被禁止活动。\n\n"
                    + "平台处置：平台可根据风险情况采取提醒、要求补充说明、截停请求、限制模型、限制 API Key 中转、暂停客户端同步、冻结账号、停用账号、保存必要记录、配合权利人或主管部门处理等措施。除紧急高风险场景外，平台会尽量保留用户查看原因和联系管理员复核的通道。\n\n"
                    + "服务可用性：平台可能因上游服务、网络、支付机构、政策合规、安全审计、版本升级、容量限制或不可抗力导致部分功能不可用。平台会尽合理努力维护服务，但不承诺任何功能永久可用、无错误或完全符合特定商业目的。";
        }
        if ("privacy".equals(key)) {
            return "隐私政策\n\n"
                    + "处理目的：为提供账号登录、AI 会话、模型接口、附件处理、客户端绑定、App 同步、支付充值、用量统计、安全审核、投诉处理、故障排查和系统运营能力，平台会在必要范围内处理与服务有关的信息。\n\n"
                    + "信息类型：平台可能处理账号信息、联系方式、角色权限、登录状态、IP、设备与客户端信息、API Key 元数据、调用记录、订单记录、余额、用量、输入内容、附件、图片、代码、会话上下文、执行日志、安全事件和管理员操作记录。\n\n"
                    + "最小必要：平台仅在实现服务、安全、计费、风控、合规和争议处理所需范围内处理信息，并采取合理措施减少完整提示词、密钥、隐私、附件内容和敏感字段的不必要留存。平台不会主动要求用户提交身份证号、银行卡号、人脸信息、密码、私钥、访问令牌、商业秘密或未获授权的他人个人信息。\n\n"
                    + "第三方处理：用户提交给模型的输入、附件、图片、会话内容和执行日志可能会发送给第三方或境外模型服务商处理。支付相关信息可能由支付机构处理，文件、日志和监控信息可能由云服务、安全组件或运维工具处理。用户使用前应确认其有权进行跨平台、跨服务商和可能的跨境传输处理。\n\n"
                    + "共享与披露：平台可能将必要信息提供给支付机构、云服务商、模型服务商、安全服务组件、日志与监控组件，用于完成支付、模型调用、存储、传输、安全审核、故障排查和合规处置。平台不会出售用户个人信息，也不会将信息用于与服务无关的定向交易。\n\n"
                    + "用户权利：用户可通过投诉举报入口申请访问、更正、复制、删除个人信息，撤回授权、注销账号、解释个人信息处理规则或投诉个人信息处理问题。涉及第三方模型服务商已处理的信息，平台将在能力范围内协助用户按相应规则处理。\n\n"
                    + "保存期限：订单、账务、用量、违规处置、安全日志、投诉记录和依法应留存的信息，会在满足安全、审计、争议处理或法律要求的期限内保存。超过必要期限后，平台会采取删除、匿名化或最小化保留等方式处理。\n\n"
                    + "安全措施：平台会采用权限控制、日志审计、敏感配置保护、访问限制、备份与故障恢复等措施保护信息安全。任何互联网服务均无法保证绝对安全，用户也应妥善保护账号、客户端设备和 API Key。";
        }
        if ("report".equals(key)) {
            return "投诉举报\n\n"
                    + "受理范围：用户、权利人或公众可就违法内容、权利侵害、个人信息处理、未成年人保护、账号滥用、API Key 泄露、计费订单、模型安全、客户端联动异常、服务不可用或其他平台问题提交投诉举报。\n\n"
                    + "材料要求：为便于核查，请提供账号或联系方式、订单号或请求 ID、API Key 名称、模型名称、发生时间、页面或客户端位置、问题描述、影响范围和必要证据。涉及权利侵害的，请说明权利基础、被侵权内容位置和希望采取的处理措施。\n\n"
                    + "敏感信息保护：请勿在投诉材料中提交无关身份证件、人脸、银行卡、密码、私钥、访问令牌、完整 API Key、商业秘密或其他不必要敏感信息。如确需提交证据，请先遮盖与投诉无关的敏感字段。\n\n"
                    + "处理流程：平台收到投诉举报后会进行登记、初步分级、证据核查、必要的账号或请求定位、处理决定和结果反馈。违法犯罪、人身安全、未成年人、个人信息泄露、账号盗用、支付争议和大面积服务异常将优先处理。\n\n"
                    + "可能措施：平台可根据核查结果采取删除或屏蔽相关内容、截停请求、限制 API Key 中转、暂停客户端同步、提醒用户、要求补充材料、恢复误判账号、修复系统问题、保存证据或配合主管部门与权利人处理等措施。\n\n"
                    + "当前入口：平台当前投诉举报入口为“关于与合规 - 投诉举报入口”页面所列说明，并通过管理员公布的 AI 开发聊天群或用户审核联系渠道提交。用户登录后也可通过用户菜单、客户端或 App 中的服务/反馈入口联系管理员。\n\n"
                    + "外部渠道：如平台配置了客服邮箱、工单系统、在线表单或企业联系方式，应优先使用平台公告中的最新联系方式。若涉及违法和不良信息，也可以向中央网信办违法和不良信息举报中心 12377 等主管渠道举报。\n\n"
                    + "记录留存：平台会依据《生成式人工智能服务管理暂行办法》关于投诉举报机制的要求，结合《个人信息保护法》关于个人信息权利请求和投诉处理的要求，建立可追踪的处理记录，并在必要期限内保存处理依据。";
        }
        if ("genai".equals(key)) {
            return "生成式 AI 服务说明\n\n"
                    + "服务定位：本服务面向经人工审核或平台授权的用户开放，提供模型接口中转、AIChat、API Key 管理、用量与余额管理、附件辅助处理、客户端联动、会话同步、执行日志与必要运营配置能力。平台的核心职责是接收用户请求、按配置路由到上游模型服务、记录必要用量并提供安全与合规控制。\n\n"
                    + "模型来源：平台不自研、不训练、不微调基础大模型。除非页面或公告另有明确说明，所有文本、图片、代码、多模态或工具调用能力均由平台已配置的第三方或上游模型服务提供。模型可用性、上下文长度、输出质量、响应速度、内容安全策略、价格和限流规则可能受上游服务商影响。\n\n"
                    + "数据流转：用户通过网页、桌面客户端、Android App、API、插件或自动化工具提交的提示词、附件、图片、代码、上下文、模型参数、工具调用参数和执行日志，可能会被转发给第三方模型服务商、云服务商或境外服务商处理。用户提交前应确认其拥有合法权利，并避免提交无授权个人信息、商业秘密、受保护数据、密钥、访问令牌或其他敏感材料。\n\n"
                    + "输出风险：生成式 AI 输出可能存在事实错误、遗漏、偏见、过时、不完整、不可复现或与用户意图不一致等问题。医疗、法律、金融、工程、安全、公共传播、未成年人、个人权益、生产环境自动化等高风险场景，必须由具备相应资质或责任的人员复核后再使用，平台不替代专业判断。\n\n"
                    + "用户责任：用户对自己输入内容、调用方式、自动化流程、对外发布或商用 AI 输出承担责任。对外发布、传播、训练、二次加工或商用 AI 生成内容时，应自行履行版权、肖像权、隐私权、数据来源、AI 生成内容标识、水印、模型服务商政策以及适用法律法规要求。\n\n"
                    + "平台控制：平台会根据法律法规、上游政策、风控需要和运营安全进行内容安全审核、异常请求截停、API Key 中转限制、订单与用量记录、安全审计、投诉处理和必要日志留存。平台会尽量提供明确原因和复核通道，但不承诺模型输出一定正确、连续可用或适合特定目的。\n\n"
                    + "服务变更：平台可能因上游供应、政策合规、安全审计、支付通道、系统升级、网络故障或不可抗力调整模型、价格、限额、客户端能力、审核规则或可用区域。涉及用户权益的重要变化，平台会通过站内公告、群组或其他合理方式提示。";
        }
        return "内容安全规则\n\n"
                + "审核范围：平台会在服务端对提示词、附件文本、会话请求、API 调用、异常重试、规避行为和投诉线索进行内容安全审核。审核目标不是限制正常讨论，而是识别明确违法意图、高危险实施请求、恶意绕过、接口滥用、侵犯他人权益和可能引发重大风险的内容。\n\n"
                + "分级原则：平台区分“警告类违规”和“限制类违法/危险意图”。警告类违规通常只记录事件并提示修改，不直接影响登录、钱包、后台管理、资料查看和正常非中转功能。限制类内容会截停请求，并在达到管理员配置阈值后限制该账号 API Key 的中转服务。\n\n"
                + "警告类违规：包括明显不当表达、低俗辱骂、诱导绕过模型规则、轻度侵权风险、未形成明确实施意图的敏感描述、上下文不足但可能引发风险的请求。此类内容通常要求用户调整表达、补充合法用途或删除不必要敏感信息。\n\n"
                + "限制类违法或危险意图：包括请求可执行犯罪步骤、工具、代码、采购渠道、攻击目标、隐匿方法、规避追责、实施方案，或明确寻求危害国家安全、暴恐极端、刺杀伤害、爆炸物/毒品制作采购、网络攻击、凭据窃取、诈骗洗钱、侵犯隐私、未成年人性剥削等帮助。\n\n"
                + "允许的安全语境：平台不会因为用户提到敏感主题就直接封禁。法律教育、危害说明、风险防范、新闻讨论、政策分析、合规咨询、受害者支持、防御性安全研究、历史研究和科普内容，在不请求可执行违法步骤、不鼓励实施、不规避监管的情况下可以放行。\n\n"
                + "处置方式：达到限制条件后，平台只限制 API Key 的中转调用，不封死登录、后台管理、钱包、资料页和只读记录。用户可查看错误提示和事件原因，管理员可在用户管理中解除误判账号的 API 中转限制。\n\n"
                + "管理员配置：管理员可在“系统设置 - 安全与限制 - 内容安全”中调整违规阈值、统计窗口、限制时长、危险关键字、警告关键字、放行语境词和危险上下文词。平台建议先以警告和日志观察为主，再对反复出现的明确危险意图采取限制。\n\n"
                + "规避处理：用户不得通过拆分请求、编码混淆、角色扮演伪装、跨语言替换、外部插件、第三方 API 或批量自动化方式规避审核。明显规避审核的行为可被视为独立违规，并纳入风险累计。";
    }

    private void applyWindowBlur(Window window) {
        if (window == null) return;
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.dimAmount = 0f;
        window.setAttributes(attrs);
        if (Build.VERSION.SDK_INT < 31) return;
        if (UiKit.effectsEnabled(this) && UiKit.blurStrength(this) > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            attrs = window.getAttributes();
            attrs.setBlurBehindRadius(UiKit.blurStrength(this));
            attrs.dimAmount = 0f;
            window.setAttributes(attrs);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }
    }

    private void renderEffectsModule() {
        LinearLayout panel = cardPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(UiKit.bold(this, "视效设置"), new LinearLayout.LayoutParams(0, -2, 1f));
        ImageButton themeToggle = UiKit.imageButton(this, UiKit.darkTheme(this) ? R.drawable.ic_theme_moon : R.drawable.ic_theme_sun, "切换明暗模式");
        themeToggle.setBackground(new ColorDrawable(Color.TRANSPARENT));
        themeToggle.clearColorFilter();
        themeToggle.setOnClickListener(v -> {
            boolean nextDark = !UiKit.darkTheme(this);
            getSharedPreferences("oneapi_mobile", MODE_PRIVATE).edit().putBoolean("dark_theme", nextDark).apply();
            themeToggle.setImageResource(nextDark ? R.drawable.ic_theme_moon : R.drawable.ic_theme_sun);
            themeToggle.clearColorFilter();
            applySystemBars();
            if (flowBackgroundView != null) flowBackgroundView.invalidate();
            getWindow().getDecorView().invalidate();
            if (adapter != null) adapter.refreshTheme();
            if (scrollDock != null) scrollDock.invalidate();
            renderCurrent(false);
        });
        titleRow.addView(themeToggle, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 42)));
        Switch enabled = new Switch(this);
        enabled.setText("");
        enabled.setChecked(UiKit.effectsEnabled(this));
        Switch atmosphere = new Switch(this);
        atmosphere.setText("");
        atmosphere.setChecked(UiKit.backgroundEffectsEnabled(this));
        LinearLayout effectSwitchRow = UiKit.horizontal(this);
        effectSwitchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView glassLabel = UiKit.text(this, "毛玻璃", UiKit.MUTED, 13);
        glassLabel.setGravity(Gravity.CENTER_VERTICAL);
        effectSwitchRow.addView(glassLabel, new LinearLayout.LayoutParams(-2, UiKit.dp(this, 42)));
        effectSwitchRow.addView(enabled, new LinearLayout.LayoutParams(UiKit.dp(this, 54), UiKit.dp(this, 42)));
        View effectSpacer = new View(this);
        effectSwitchRow.addView(effectSpacer, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView atmosphereLabel = UiKit.text(this, "氛围灯", UiKit.MUTED, 13);
        atmosphereLabel.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams atmosphereLabelLp = new LinearLayout.LayoutParams(-2, UiKit.dp(this, 42));
        atmosphereLabelLp.setMargins(UiKit.dp(this, 16), 0, 0, 0);
        effectSwitchRow.addView(atmosphereLabel, atmosphereLabelLp);
        effectSwitchRow.addView(atmosphere, new LinearLayout.LayoutParams(UiKit.dp(this, 54), UiKit.dp(this, 42)));
        panel.addView(effectSwitchRow, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 46)));
        TextView blurValue = UiKit.text(this, String.valueOf(UiKit.blurStrength(this)), UiKit.MUTED, 13);
        SeekBar blur = new SeekBar(this);
        blur.setMax(50);
        blur.setProgress(UiKit.blurStrength(this));
        TextView alphaValue = UiKit.text(this, String.valueOf(UiKit.glassAlpha(this)), UiKit.MUTED, 13);
        SeekBar alpha = new SeekBar(this);
        alpha.setMax(255);
        alpha.setProgress(UiKit.glassAlpha(this));
        panel.addView(settingSliderRow("模糊强度", blur, blurValue));
        panel.addView(settingSliderRow("透明度", alpha, alphaValue));
        Runnable syncEnabled = () -> {
            boolean on = enabled.isChecked();
            blur.setEnabled(on);
            alpha.setEnabled(on);
        };
        enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("oneapi_mobile", MODE_PRIVATE).edit().putBoolean("effects_enabled", isChecked).apply();
            syncEnabled.run();
        });
        atmosphere.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("oneapi_mobile", MODE_PRIVATE).edit().putBoolean("background_effects_enabled", isChecked).apply();
            if (flowBackgroundView != null) flowBackgroundView.invalidate();
            getWindow().getDecorView().invalidate();
        });
        blur.setOnSeekBarChangeListener(new SimpleSeekListener(progress -> {
            getSharedPreferences("oneapi_mobile", MODE_PRIVATE).edit().putInt("blur_strength", progress).apply();
            blurValue.setText(String.valueOf(progress));
        }));
        alpha.setOnSeekBarChangeListener(new SimpleSeekListener(progress -> {
            getSharedPreferences("oneapi_mobile", MODE_PRIVATE).edit().putInt("glass_alpha", progress).apply();
            alphaValue.setText(String.valueOf(progress));
        }));
        syncEnabled.run();
        pageContent.addView(panel);
    }

    private View settingSliderRow(String label, SeekBar seekBar, TextView value) {
        LinearLayout row = UiKit.horizontal(this);
        row.setPadding(0, UiKit.dp(this, 6), 0, UiKit.dp(this, 6));
        row.addView(UiKit.text(this, label, UiKit.MUTED, 13), new LinearLayout.LayoutParams(UiKit.dp(this, 70), -2));
        row.addView(seekBar, new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f));
        value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(value, new LinearLayout.LayoutParams(UiKit.dp(this, 42), UiKit.dp(this, 38)));
        return row;
    }

    private void renderWalletPage() {
        LinearLayout panel = cardPanel();
        panel.addView(UiKit.bold(this, "钱包与消耗"));
        panel.addView(loadingRow("正在同步钱包、账单和消耗记录"));
        pageContent.addView(panel);
        fetchInto(panel, () -> {
            JSONObject root = new JSONObject();
            CompletableFuture<JSONObject> profile = supplyCriticalJson(() -> billingController.profile());
            CompletableFuture<JSONObject> topups = supplyCriticalJson(() -> billingController.topups());
            CompletableFuture<JSONObject> usage = supplyCriticalJson(() -> billingController.usage());
            CompletableFuture<JSONObject> plans = supplyCriticalJson(() -> billingController.plans());
            CompletableFuture<JSONObject> subscriptions = supplyCriticalJson(() -> billingController.subscriptions());
            root.put("profile", profile.get().opt("data"));
            root.put("topups", topups.get().opt("data"));
            root.put("usage", usage.get().opt("data"));
            root.put("plans", plans.get().opt("data"));
            root.put("subscriptions", subscriptions.get().opt("data"));
            return root;
        }, "钱包与消耗");
    }

    private CompletableFuture<JSONObject> supplyCriticalJson(JsonRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return request.run();
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }, criticalNetwork);
    }

    private void renderServicePage() {
        LinearLayout panel = cardPanel();
        panel.addView(UiKit.bold(this, "服务状态"));
        panel.addView(loadingRow("正在读取服务状态"));
        pageContent.addView(panel);
        fetchInto(panel, () -> statusController.serviceStatus(), "服务状态");
    }

    private void renderSubscriptionsPage() {
        LinearLayout panel = cardPanel();
        panel.addView(UiKit.bold(this, "套餐订阅"));
        panel.addView(loadingRow("正在读取套餐列表"));
        pageContent.addView(panel);
        fetchInto(panel, () -> billingController.plans(), "套餐订阅");
    }

    private TextView sectionTitle(String text) {
        TextView view = UiKit.bold(this, text);
        view.setTextSize(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(this, 4), 0, UiKit.dp(this, 14));
        view.setLayoutParams(lp);
        return view;
    }

    private LinearLayout cardPanel() {
        LinearLayout panel = UiKit.vertical(this);
        panel.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 14), UiKit.dp(this, 16), UiKit.dp(this, 14));
        panel.setBackground(UiKit.glass(this, UiKit.dp(this, 18), UiKit.line(this)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, UiKit.dp(this, 12));
        panel.setLayoutParams(lp);
        return panel;
    }

    private LinearLayout card(String title, String desc) {
        LinearLayout panel = cardPanel();
        panel.addView(UiKit.bold(this, title));
        panel.addView(UiKit.text(this, desc, UiKit.MUTED, 14));
        return panel;
    }

    private View loadingRow(String text) {
        LinearLayout row = UiKit.horizontal(this);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        row.addView(progress, new LinearLayout.LayoutParams(UiKit.dp(this, 28), UiKit.dp(this, 28)));
        TextView label = UiKit.text(this, text, UiKit.MUTED, 14);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, -2, 1f);
        labelLp.setMargins(UiKit.dp(this, 10), 0, 0, 0);
        row.addView(label, labelLp);
        return row;
    }

    private EditText input(String hint) {
        EditText view = new EditText(this);
        view.setTextSize(14);
        view.setSingleLine(true);
        view.setHint(hint);
        view.setTextColor(UiKit.ink(this));
        view.setHintTextColor(UiKit.muted(this));
        view.setBackground(UiKit.round(UiKit.inputFill(this), UiKit.dp(this, 14), UiKit.line(this)));
        view.setPadding(UiKit.dp(this, 10), 0, UiKit.dp(this, 10), 0);
        return view;
    }

    private EditText searchInput(String hint) {
        EditText view = input(hint);
        view.setTextSize(13);
        view.setSingleLine(true);
        view.setMaxLines(1);
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        view.setPadding(UiKit.dp(this, 10), 0, UiKit.dp(this, 10), 0);
        return view;
    }

    private void onSearchChanged(EditText input, Runnable change) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (change != null) change.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private boolean matchesSearch(String text, String query) {
        String clean = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() || clean.contains(needle);
    }

    private View labelRow(String label, View value) {
        return labelRow(label, value, UiKit.dp(this, 42));
    }

    private View labelRow(String label, View value, int heightPx) {
        LinearLayout row = UiKit.horizontal(this);
        TextView text = UiKit.text(this, label, UiKit.MUTED, 13);
        text.setGravity(Gravity.TOP | Gravity.LEFT);
        row.addView(text, new LinearLayout.LayoutParams(UiKit.dp(this, 58), heightPx));
        row.addView(value, new LinearLayout.LayoutParams(0, heightPx, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, UiKit.dp(this, 5), 0, UiKit.dp(this, 5));
        row.setLayoutParams(lp);
        return row;
    }

    private LinearLayout.LayoutParams centeredButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 38));
        lp.setMargins(0, UiKit.dp(this, 8), 0, 0);
        return lp;
    }

    private Button iconGlyphButton(String glyph) {
        Button button = UiKit.ghostButton(this, glyph);
        button.setTextSize(18);
        button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        return button;
    }

    private void doLogin(String username, String password) {
        doLogin(username, password, null);
    }

    private void doLogin(String username, String password, Runnable onSuccess) {
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                prefs.setToken("");
                prefs.setCookie("");
                JSONObject body = new JSONObject();
                body.put("username", username == null ? "" : username.trim());
                body.put("password", password == null ? "" : password.trim());
                ApiClient loginClient = new ApiClient(prefs);
                JSONObject envelope = loginClient.post("/api/user/login", body);
                JSONObject data = envelope.optJSONObject("data");
                if (data != null) {
                    prefs.setUserId(String.valueOf(data.optInt("id")));
                    String loginToken = first(data, "access_token", "accessToken", "token");
                    if (!loginToken.isEmpty()) {
                        prefs.setToken(loginToken);
                    }
                }
                prefs.setUsername(data == null ? username : first(data, "username", "display_name"));
                if (prefs.token().isEmpty()) {
                    try {
                        JSONObject tokenEnvelope = loginClient.get("/api/user/token");
                        Object tokenData = tokenEnvelope.opt("data");
                        String accessToken = tokenData == null ? "" : String.valueOf(tokenData).trim();
                        if (!accessToken.isEmpty() && !"null".equalsIgnoreCase(accessToken)) {
                            prefs.setToken(accessToken);
                        }
                    } catch (Exception ignored) {
                        // Older servers may not return the token in login; keep cookie auth as a fallback.
                    }
                }
                if (prefs.token().isEmpty() && prefs.cookie().isEmpty()) {
                    throw new IllegalStateException("登录成功但未获取到有效登录凭据，请确认服务器已更新移动端登录接口。");
                }
                ensureAppApiKey(loginClient);
                mainHandler.post(() -> {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                    requireLegalAcceptance(() -> {
                        if (onSuccess != null) onSuccess.run();
                        refreshModels();
                        refreshAssistants();
                    });
                });
            } catch (Exception error) {
                mainHandler.post(() -> Toast.makeText(this, "登录失败：" + message(error), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void ensureAppApiKeyIfLoggedIn() {
        if (!prefs.isLoggedIn()) return;
        String current = prefs.appApiKey();
        if (!current.isEmpty()) return;
        executeNetwork(NetworkRequestPolicy.Priority.USER_ACTION, () -> ensureAppApiKey(userActionApiClient));
    }

    private void ensureAppApiKey(ApiClient client) {
        try {
            ensureAppApiKeyOrThrow(client);
        } catch (Exception error) {
            Log.w(TAG, "ensureAppApiKey failed", error);
            mainHandler.post(() -> Toast.makeText(this, "App 专用 Key 初始化失败，Chat/Image 可能无法调用模型：" + message(error), Toast.LENGTH_LONG).show());
        }
    }

    private void ensureAppApiKeyOrThrow(ApiClient client) throws Exception {
        appApiKeyManager.ensureOrThrow(client);
    }

    private void showLoginDialog(boolean cancelable) {
        if (loginDialogShowing) return;
        loginDialogShowing = true;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(cancelable);
        dialog.setOnDismissListener(d -> loginDialogShowing = false);
        FrameLayout overlay = modalOverlay();
        overlay.setOnClickListener(v -> {
            if (cancelable) dialog.dismiss();
        });
        LinearLayout panel = modalPanel();
        panel.addView(UiKit.bold(this, "登录"));
        prefs.setServer(AppPrefs.DEFAULT_SERVER);
        EditText username = input("账号");
        EditText password = input("密码");
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        panel.addView(labelRow("账号", username));
        panel.addView(labelRow("密码", password));
        Button login = UiKit.ghostButton(this, "登录");
        login.setOnClickListener(v -> {
            prefs.setServer(AppPrefs.DEFAULT_SERVER);
            doLogin(username.getText().toString(), password.getText().toString(), dialog::dismiss);
        });
        LinearLayout actions = UiKit.horizontal(this);
        actions.setGravity(Gravity.CENTER);
        actions.addView(login, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 40)));
        panel.addView(actions, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 44)));
        overlay.addView(panel, centeredModalLp());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void refreshDevices(LinearLayout panel) {
        fetchInto(panel, () -> {
            JSONObject out = new JSONObject();
            out.put("boundDeviceId", prefs.boundDeviceId());
            out.put("devices", desktopRepository.devices());
            return out;
        }, "设备绑定");
    }

    private void validateBoundDesktopDevice(boolean force) {
        String bound = prefs.boundDeviceId();
        if (bound == null || bound.trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        if (!force && desktopBindingCheckedAt > 0L && now - desktopBindingCheckedAt < 60_000L) return;
        synchronized (this) {
            if (desktopBindingCheckInFlight) return;
            desktopBindingCheckInFlight = true;
        }
        executeNetwork(NetworkRequestPolicy.Priority.DESKTOP_SYNC, () -> {
            try {
                JSONArray devices = desktopRepository.devices();
                desktopBindingCheckedAt = System.currentTimeMillis();
                mainHandler.post(() -> setDesktopBridgeConnected(findDevice(devices, bound) != null));
                if (findDevice(devices, bound) == null) {
                    mainHandler.post(() -> clearInvalidDesktopBinding(bound, "当前绑定的桌面客户端已不存在或不在线，请重新绑定。"));
                }
            } catch (Exception error) {
                mainHandler.post(() -> setDesktopBridgeConnected(false));
                String detail = message(error);
                appendDesktopSyncNotice(AppSection.CODEX, "设备绑定校验失败：" + detail);
                appendDesktopSyncNotice(AppSection.CLAUDE, "设备绑定校验失败：" + detail);
            } finally {
                synchronized (MainActivity.this) {
                    desktopBindingCheckInFlight = false;
                }
            }
        });
    }

    private void clearInvalidDesktopBinding(String expectedDeviceId, String reason) {
        String current = prefs.boundDeviceId();
        if (expectedDeviceId != null && !expectedDeviceId.equals(current)) return;
        prefs.setBoundDeviceId("");
        desktopSessionRefreshedAt.clear();
        appendDesktopSyncNotice(AppSection.CODEX, reason);
        appendDesktopSyncNotice(AppSection.CLAUDE, reason);
        updateDesktopWakeState();
        if (currentSection.isDesktop()) {
            renderCurrent(false);
        }
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
    }

    private void fetchInto(LinearLayout panel, JsonRequest request, String title) {
        long now = System.currentTimeMillis();
        long ttlMs = NetworkRequestPolicy.panelTtlMs(title);
        PanelDataStore.Entry cached = panelDataStore == null ? null : panelDataStore.get(title);
        if (cached != null) {
            JSONObject cachedData = copyJson(cached.data);
            try {
                cachedData.put("_cache_saved_at", cached.savedAt);
                if (!cached.fresh(now, ttlMs)) {
                    cachedData.put("_stale_notice", "正在后台刷新，当前显示上次同步结果。");
                }
            } catch (Exception ignored) {
            }
            renderJsonPanel(panel, title, cachedData);
            if (cached.fresh(now, ttlMs)) return;
        }
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            JSONObject result;
            try {
                result = request.run();
                if (panelDataStore != null) {
                    panelDataStore.put(title, result, System.currentTimeMillis());
                }
            } catch (Exception error) {
                String errorText = message(error);
                if (isAuthExpiredMessage(errorText)) {
                    prefs.setCookie("");
                    prefs.setToken("");
                    prefs.setUserId("");
                    prefs.setBoundDeviceId("");
                    mainHandler.post(() -> showLoginDialog(false));
                }
                PanelDataStore.Entry fallback = panelDataStore == null ? null : panelDataStore.get(title);
                if (fallback != null) {
                    result = copyJson(fallback.data);
                    try {
                        result.put("_cache_saved_at", fallback.savedAt);
                        result.put("_stale_notice", "网络请求失败，已显示上次同步结果：" + errorText);
                    } catch (Exception ignored) {
                    }
                } else {
                    result = new JSONObject();
                    try {
                        result.put("error", errorText);
                    } catch (Exception ignored) {
                    }
                }
            }
            JSONObject finalResult = result;
            mainHandler.post(() -> renderJsonPanel(panel, title, finalResult));
        });
    }

    private JSONObject copyJson(JSONObject value) {
        if (value == null) return new JSONObject();
        try {
            return new JSONObject(value.toString());
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private void renderJsonPanel(LinearLayout panel, String title, JSONObject result) {
        panel.removeAllViews();
        panel.addView(UiKit.bold(this, title));
        long cacheSavedAt = result.optLong("_cache_saved_at", 0L);
        if (cacheSavedAt > 0L) {
            panel.addView(UiKit.text(this, "上次同步：" + formatDateTime(cacheSavedAt), UiKit.MUTED, 12));
            result.remove("_cache_saved_at");
        }
        if (result.has("_stale_notice")) {
            panel.addView(UiKit.text(this, result.optString("_stale_notice"), Color.rgb(214, 144, 44), 13));
            result.remove("_stale_notice");
        }
        if (result.has("error")) {
            panel.addView(UiKit.text(this, result.optString("error"), Color.rgb(176, 55, 55), 14));
            if (result.optString("error").contains("429")) {
                panel.addView(UiKit.text(this, "服务器限流中，请稍后重试。页面不会清空已有缓存。", UiKit.MUTED, 13));
            }
            return;
        }
        if (result.has("devices")) {
            renderDevices(panel, result.optJSONArray("devices"));
            return;
        }
        if ("套餐订阅".equals(title)) {
            renderPlans(panel, result);
            return;
        }
        if ("服务状态".equals(title)) {
            renderServiceStatus(panel, result);
            return;
        }
        if ("钱包与消耗".equals(title)) {
            renderWalletData(panel, result);
            return;
        }
        if ("版本信息".equals(title)) {
            renderVersionInfo(panel, result);
            return;
        }
        if ("系统公告".equals(title)) {
            renderAnnouncements(panel, result);
            return;
        }
        panel.addView(UiKit.text(this, jsonPreview(result), UiKit.MUTED, 13));
    }

    private void renderAnnouncements(LinearLayout panel, JSONObject result) {
        JSONObject data = result.optJSONObject("data");
        JSONArray rows = data == null ? null : data.optJSONArray("announcements");
        if (rows == null) rows = result.optJSONArray("announcements");
        if (rows == null || rows.length() == 0) {
            panel.addView(UiKit.text(this, "暂无系统公告。", UiKit.MUTED, 14));
            return;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) continue;
            LinearLayout card = cardPanel();
            card.addView(UiKit.bold(this, first(item, "title", "name")));
            card.addView(UiKit.text(this, first(item, "content", "message", "body"), UiKit.MUTED, 13));
            panel.addView(card);
        }
    }

    private void renderWalletData(LinearLayout panel, JSONObject result) {
        JSONObject profile = result.optJSONObject("profile");
        if (profile == null) profile = new JSONObject();
        LinearLayout overview = cardPanel();
        overview.addView(UiKit.bold(this, "钱包总览"));
        overview.addView(UiKit.text(this, "剩余额度 " + formatQuotaAsUsd(profile.optDouble("quota", 0), 500000), UiKit.MUTED, 14));
        overview.addView(UiKit.text(this, "已用额度 " + formatQuotaAsUsd(profile.optDouble("used_quota", 0), 500000) + " · 请求数 " + profile.optLong("request_count", 0), UiKit.MUTED, 14));
        panel.addView(overview);
        LinearLayout alipay = cardPanel();
        alipay.addView(UiKit.bold(this, "支付宝充值"));
        alipay.addView(UiKit.text(this, "服务端创建 App 支付订单，支付结果由服务器查询确认。", UiKit.MUTED, 14));
        LinearLayout amountRow = UiKit.horizontal(this);
        Button[] amountButtons = new Button[AlipayPaymentGuard.ALLOWED_AMOUNTS.length];
        Runnable updateAmountButtons = () -> {
            for (int i = 0; i < amountButtons.length; i++) {
                Button button = amountButtons[i];
                if (button == null) continue;
                int amount = AlipayPaymentGuard.ALLOWED_AMOUNTS[i];
                button.setText((amount == selectedAlipayAmount ? "✓ " : "") + amount + " 元");
            }
        };
        for (int i = 0; i < AlipayPaymentGuard.ALLOWED_AMOUNTS.length; i++) {
            int amount = AlipayPaymentGuard.ALLOWED_AMOUNTS[i];
            Button amountButton = UiKit.ghostButton(this, amount + " 元");
            amountButton.setOnClickListener(v -> {
                selectedAlipayAmount = amount;
                updateAmountButtons.run();
            });
            amountButtons[i] = amountButton;
            LinearLayout.LayoutParams amountLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f);
            if (i > 0) amountLp.leftMargin = UiKit.dp(this, 8);
            amountRow.addView(amountButton, amountLp);
        }
        updateAmountButtons.run();
        alipay.addView(labelRow("金额", amountRow));
        Button pay = UiKit.ghostButton(this, "支付宝支付");
        pay.setOnClickListener(v -> startAlipayAppPayment(selectedAlipayAmount));
        alipay.addView(pay, centeredButtonLp());
        panel.addView(alipay);
        JSONObject usage = result.optJSONObject("usage");
        JSONArray usageItems = usage == null ? null : usage.optJSONArray("items");
        if (usageItems == null && usage != null) usageItems = usage.optJSONArray("data");
        JSONObject topups = result.optJSONObject("topups");
        LinearLayout bills = cardPanel();
        bills.addView(UiKit.bold(this, "最近账单"));
        JSONArray billItems = topups == null ? null : topups.optJSONArray("items");
        if (billItems == null && topups != null) billItems = topups.optJSONArray("data");
        if (billItems == null || billItems.length() == 0) {
            bills.addView(UiKit.text(this, "暂无最近账单。", UiKit.MUTED, 14));
        } else {
            JSONObject subscriptions = result.optJSONObject("subscriptions");
            JSONArray plans = result.optJSONArray("plans");
            for (int i = 0; i < Math.min(billItems.length(), 3); i++) {
                JSONObject item = billItems.optJSONObject(i);
                if (item == null) continue;
                double amount = Math.abs(item.optDouble("amount", item.optDouble("money", 0)));
                bills.addView(billingRow(formatBillingLabel(item) + " · " + formatPlainPrice(item.optDouble("money", amount)) + " 元", resolveBillingUsageRatio(item, plans, subscriptions)));
            }
        }
        panel.addView(bills);
        LinearLayout distribution = cardPanel();
        distribution.addView(UiKit.bold(this, "消耗分布"));
        renderUsageDistribution(distribution, usageItems);
        panel.addView(distribution);
    }

    private void renderVersionInfo(LinearLayout panel, JSONObject result) {
        JSONObject data = result.optJSONObject("data");
        JSONObject android = data == null ? null : data.optJSONObject("android");
        if (android == null) {
            panel.addView(UiKit.text(this, jsonPreview(result), UiKit.MUTED, 13));
            return;
        }
        String latest = first(android, "version", "versionName", "name");
        String url = first(android, "url", "downloadUrl");
        if (url.isEmpty()) url = "/api/download/package/android";
        String resolved = resolveServerUrl(url);
        panel.addView(UiKit.text(this, "当前版本：" + versionName() + "\n最新版本：" + (latest.isEmpty() ? "未知" : latest), UiKit.MUTED, 14));
        Button open = UiKit.ghostButton(this, "下载更新");
        open.setOnClickListener(v -> openExternalUrl(resolved));
        panel.addView(open, centeredButtonLp());
    }

    private void showUpdateDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "检查更新"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(UiKit.gap(this, 18));
        TextView body = UiKit.text(this, "正在检查...", UiKit.INK, 16);
        panel.addView(body);
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
        executeNetwork(NetworkRequestPolicy.Priority.BACKGROUND, () -> {
            try {
                JSONObject result = statusController.downloadPackages();
                JSONObject data = result.optJSONObject("data");
                JSONObject android = data == null ? null : data.optJSONObject("android");
                String latest = android == null ? "" : first(android, "version", "versionName", "name");
                String url = android == null ? "" : first(android, "url", "downloadUrl");
                String message = latest.isEmpty() || latest.equals(versionName()) ? "已是最新版" : "发现新版本 " + latest;
                String finalUrl = url.isEmpty() ? "" : resolveServerUrl(url);
                mainHandler.post(() -> {
                    body.setText(message);
                    if (!finalUrl.isEmpty() && !"已是最新版".equals(message)) {
                        Button download = UiKit.ghostButton(this, "下载更新");
                        download.setOnClickListener(v -> downloadAndInstallApk(finalUrl, latest, body));
                        panel.addView(download, centeredButtonLp());
                    }
                });
            } catch (Exception error) {
                mainHandler.post(() -> body.setText("检查失败：" + message(error)));
            }
        });
    }

    private String resolveServerUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        String base = prefs.server();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (trimmed.startsWith("/")) return base + trimmed;
        return base + "/" + trimmed;
    }

    private void openExternalUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception error) {
            Toast.makeText(this, "无法打开链接：" + message(error), Toast.LENGTH_LONG).show();
        }
    }

    private void shareText(String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
            startActivity(Intent.createChooser(intent, "分享"));
        } catch (Exception error) {
            Toast.makeText(this, "无法分享：" + message(error), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadAndInstallApk(String url, String version, TextView status) {
        status.setText("下载进度 0%");
        executeNetwork(NetworkRequestPolicy.Priority.BACKGROUND, () -> {
            try {
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) dir = getCacheDir();
                if (!dir.exists()) dir.mkdirs();
                File apk = new File(dir, "oneapi-" + (version == null || version.isEmpty() ? "update" : version.replaceAll("[^0-9A-Za-z._-]", "_")) + ".apk");
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(180000);
                int total = Math.max(1, connection.getContentLength());
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long done = 0;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        done += read;
                        int percent = (int) Math.min(99, done * 100 / total);
                        mainHandler.post(() -> status.setText("下载进度 " + percent + "%"));
                    }
                }
                mainHandler.post(() -> {
                    status.setText("下载完成，正在打开安装器");
                    installApk(apk);
                });
            } catch (Exception error) {
                mainHandler.post(() -> status.setText("下载失败：" + message(error)));
            }
        });
    }

    private void installApk(File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "无法打开安装器：" + message(error), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPlans(LinearLayout panel, JSONObject result) {
        JSONArray plans = result.optJSONArray("data");
        if (plans == null && result.optJSONObject("data") != null) {
            plans = result.optJSONObject("data").optJSONArray("plans");
        }
        if (plans == null || plans.length() == 0) {
            panel.addView(UiKit.text(this, "暂无可购买套餐。", UiKit.MUTED, 14));
            return;
        }
        Set<String> recommended = recommendedPlanKeys(plans);
        for (int i = 0; i < plans.length(); i++) {
            JSONObject record = plans.optJSONObject(i);
            JSONObject plan = record == null ? null : record.optJSONObject("plan");
            if (plan == null) continue;
            if (!plan.optBoolean("enabled", true)) continue;
            if (isTrialPlan(plan) && hasPurchasedPlan(record, plan)) continue;
            FrameLayout frame = new FrameLayout(this);
            frame.setBackground(UiKit.glass(this, UiKit.dp(this, 18), UiKit.line(this)));
            LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(-1, -2);
            frameLp.setMargins(0, 0, 0, UiKit.dp(this, 16));
            frame.setLayoutParams(frameLp);
            if (isFreshPlan(plan)) {
                ImageView gift = new ImageView(this);
                gift.setImageResource(R.drawable.plan_gift);
                gift.setAlpha(0.46f);
                gift.setScaleType(ImageView.ScaleType.FIT_CENTER);
                frame.addView(gift, new FrameLayout.LayoutParams(UiKit.dp(this, 92), UiKit.dp(this, 92), Gravity.CENTER));
            }
            LinearLayout card = UiKit.vertical(this);
            card.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 15), UiKit.dp(this, 16), UiKit.dp(this, 15));
            LinearLayout head = UiKit.horizontal(this);
            head.addView(UiKit.bold(this, plan.optString("title", "套餐")), new LinearLayout.LayoutParams(0, -2, 1f));
            head.addView(planBadge(plan), new LinearLayout.LayoutParams(UiKit.dp(this, 74), UiKit.dp(this, 28)));
            card.addView(head);
            card.addView(UiKit.text(this, plan.optString("subtitle", "适合稳定使用。"), UiKit.MUTED, 13));
            card.addView(UiKit.text(this,
                    "总额度 " + formatQuotaAsUsd(plan.optDouble("total_amount", 0), 500000)
                            + " · 有效期 " + formatDuration(plan)
                            + " · " + resetLabel(plan.optString("quota_reset_period", "never")),
                    UiKit.MUTED, 13));
            LinearLayout foot = UiKit.horizontal(this);
            TextView price = UiKit.bold(this, formatPlainPrice(plan.optDouble("price_amount", 0)) + " 元");
            price.setTextColor(UiKit.blue(this));
            foot.addView(price, new LinearLayout.LayoutParams(0, -2, 1f));
            if (recommended.contains(planKey(plan))) {
                ImageView sale = new ImageView(this);
                sale.setImageResource(R.drawable.plan_sale);
                sale.setAlpha(0.82f);
                sale.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                LinearLayout.LayoutParams saleLp = new LinearLayout.LayoutParams(UiKit.dp(this, 18), UiKit.dp(this, 18));
                saleLp.setMargins(0, 0, UiKit.dp(this, 8), 0);
                foot.addView(sale, saleLp);
            }
            ImageButton buy = UiKit.imageButton(this, R.drawable.plan_shop, "购买套餐");
            buy.setColorFilter(UiKit.blue(this));
            buy.setBackgroundColor(Color.TRANSPARENT);
            buy.setPadding(0, 0, 0, 0);
            buy.setScaleType(ImageView.ScaleType.FIT_CENTER);
            buy.setOnClickListener(v -> purchasePlan(plan));
            foot.addView(buy, new LinearLayout.LayoutParams(UiKit.dp(this, 32), UiKit.dp(this, 32)));
            card.addView(foot);
            frame.addView(card, new FrameLayout.LayoutParams(-1, -2));
            panel.addView(frame);
        }
    }

    private void purchasePlan(JSONObject plan) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        panel.addView(UiKit.bold(this, "钱包支付"));
        panel.addView(UiKit.text(this, plan.optString("title", "套餐") + " · " + formatPlainPrice(plan.optDouble("price_amount", 0)) + " 元", UiKit.MUTED, 14));
        panel.addView(UiKit.text(this, "确认后将从钱包余额扣除并购买该套餐。", UiKit.MUTED, 14));
        LinearLayout actions = UiKit.horizontal(this);
        Button cancel = UiKit.ghostButton(this, "取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        Button confirm = UiKit.ghostButton(this, "确认支付");
        confirm.setOnClickListener(v -> {
            confirm.setEnabled(false);
            confirm.setText("支付中");
            executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("plan_id", plan.optInt("id", 0));
                    JSONObject response = billingController.purchase(body);
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        Toast.makeText(this, response.optJSONObject("data") == null ? "套餐购买成功" : response.optJSONObject("data").optString("notice", "套餐购买成功"), Toast.LENGTH_LONG).show();
                        renderCurrent(false);
                    });
                } catch (Exception error) {
                    mainHandler.post(() -> {
                        confirm.setEnabled(true);
                        confirm.setText("确认支付");
                        Toast.makeText(this, "购买失败：" + message(error), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f);
        cancelLp.setMargins(0, 0, UiKit.dp(this, 10), 0);
        LinearLayout.LayoutParams confirmLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f);
        confirmLp.setMargins(UiKit.dp(this, 10), 0, 0, 0);
        actions.addView(cancel, cancelLp);
        actions.addView(confirm, confirmLp);
        panel.addView(actions, centeredButtonLp());
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
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
            if (plan == null || !plan.optBoolean("enabled", true) || isTrialPlan(plan)) continue;
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
        int id = plan == null ? 0 : plan.optInt("id", 0);
        if (id != 0) return "id:" + id;
        return plan == null ? "" : plan.optString("title", "") + "|" + plan.optString("duration_unit", "") + "|" + plan.optInt("duration_value", 0);
    }

    private boolean isTrialPlan(JSONObject plan) {
        String text = plan == null ? "" : (plan.optString("title") + " " + plan.optString("subtitle") + " " + plan.optString("name")).toLowerCase(Locale.ROOT);
        return text.contains("尝鲜") || text.contains("trial") || text.contains("体验");
    }

    private boolean isFreshPlan(JSONObject plan) {
        String text = plan == null ? "" : plan.optString("title", "") + " " + plan.optString("name", "");
        return text.contains("尝鲜");
    }

    private boolean hasPurchasedPlan(JSONObject record, JSONObject plan) {
        if (record == null || plan == null) return false;
        return record.optBoolean("purchased", false)
                || record.optBoolean("subscribed", false)
                || record.optBoolean("owned", false)
                || record.optInt("purchase_count", 0) > 0
                || record.optInt("buy_count", 0) > 0
                || plan.optBoolean("purchased", false)
                || plan.optInt("purchase_count", 0) > 0;
    }

    private TextView planBadge(JSONObject plan) {
        String unit = plan.optString("duration_unit", "month");
        int value = Math.max(1, plan.optInt("duration_value", 1));
        String label;
        int fill;
        if ("year".equals(unit) || value >= 12) {
            label = "年付";
            fill = Color.argb(54, 245, 158, 11);
        } else if ("month".equals(unit)) {
            label = "月付";
            fill = Color.argb(54, 54, 104, 240);
        } else {
            label = formatDuration(plan);
            fill = Color.argb(54, 210, 132, 40);
        }
        TextView badge = UiKit.text(this, label, Color.rgb(66, 82, 108), 12);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(UiKit.round(fill, UiKit.dp(this, 14), Color.argb(74, 145, 160, 184)));
        return badge;
    }

    private String formatDuration(JSONObject plan) {
        String unit = plan.optString("duration_unit", "month");
        int value = Math.max(1, plan.optInt("duration_value", 1));
        if ("year".equals(unit)) return value + "年";
        if ("month".equals(unit)) return value + "个月";
        if ("day".equals(unit)) return value + "天";
        return value + "期";
    }

    private String resetLabel(String value) {
        if ("month".equals(value)) return "每月重置";
        if ("year".equals(value)) return "每年重置";
        if ("day".equals(value)) return "每日重置";
        return "不重置";
    }

    private String formatPlainPrice(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.CHINA, "%.2f", value);
    }

    private String formatQuotaAsUsd(double value, double unit) {
        double normalized = unit <= 0 ? value : value / unit;
        if (Math.abs(normalized - Math.rint(normalized)) < 0.001) return "$" + (long) Math.rint(normalized);
        return "$" + String.format(Locale.CHINA, "%.2f", normalized);
    }

    private void renderUsageDistribution(LinearLayout parent, JSONArray items) {
        if (items == null || items.length() == 0) {
            parent.addView(UiKit.text(this, "暂无用量记录。", UiKit.MUTED, 14));
            return;
        }
        List<String> labels = new ArrayList<>();
        List<Double> quotas = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < Math.min(items.length(), 20); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            double quota = Math.max(0, item.optDouble("quota", 0));
            if (quota <= 0) continue;
            total += quota;
            String model = item.optString("model_name", item.optString("token_name", "未知模型"));
            int index = labels.indexOf(model);
            if (index >= 0) quotas.set(index, quotas.get(index) + quota);
            else {
                labels.add(model);
                quotas.add(quota);
            }
        }
        if (labels.isEmpty() || total <= 0) {
            parent.addView(UiKit.text(this, "暂无用量记录。", UiKit.MUTED, 14));
            return;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) order.add(i);
        order.sort((a, b) -> Double.compare(quotas.get(b), quotas.get(a)));
        for (int i = 0; i < Math.min(order.size(), 5); i++) {
            int index = order.get(i);
            parent.addView(progressRow(labels.get(index), quotas.get(index) / total));
        }
    }

    private View billingRow(String label, double ratio) {
        return progressRow(label, ratio);
    }

    private View progressRow(String label, double ratio) {
        LinearLayout row = UiKit.horizontal(this);
        row.setPadding(0, UiKit.dp(this, 8), 0, 0);
        TextView text = UiKit.text(this, label, UiKit.MUTED, 13);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(progressBar(ratio), new LinearLayout.LayoutParams(UiKit.dp(this, 132), UiKit.dp(this, 8)));
        return row;
    }

    private View progressBar(double ratio) {
        FrameLayout track = new FrameLayout(this);
        track.setBackground(UiKit.round(Color.argb(78, 145, 160, 184), UiKit.dp(this, 4), Color.TRANSPARENT));
        View fill = new View(this);
        fill.setBackground(UiKit.round(UiKit.blue(this), UiKit.dp(this, 4), UiKit.blue(this)));
        int width = Math.max(UiKit.dp(this, 4), (int) (UiKit.dp(this, 132) * Math.max(0.04, Math.min(1, ratio))));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, UiKit.dp(this, 8), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        track.addView(fill, lp);
        return track;
    }

    private String formatBillingLabel(JSONObject item) {
        String trade = item.optString("trade_no", "").trim();
        if (!trade.isEmpty()) return trade;
        String payment = item.optString("payment_method", "").replaceAll("(?i)^wallet$", "").trim();
        return payment.isEmpty() ? "购买记录" : payment;
    }

    private double resolveBillingUsageRatio(JSONObject bill, JSONArray plans, JSONObject subscriptions) {
        String title = bill == null ? "" : bill.optString("plan_title", "").trim();
        if (title.isEmpty() || plans == null || subscriptions == null) return 0;
        Map<Integer, String> titleByPlanId = new HashMap<>();
        for (int i = 0; i < plans.length(); i++) {
            JSONObject record = plans.optJSONObject(i);
            JSONObject plan = record == null ? null : record.optJSONObject("plan");
            if (plan == null) continue;
            String planTitle = plan.optString("title", "").trim();
            if (!planTitle.isEmpty()) titleByPlanId.put(plan.optInt("id", 0), planTitle);
        }
        JSONArray rows = subscriptions.optJSONArray("all_subscriptions");
        if (rows == null) rows = subscriptions.optJSONArray("subscriptions");
        if (rows == null) return 0;
        long bestUpdatedAt = -1;
        double ratio = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            JSONObject sub = row == null ? null : row.optJSONObject("subscription");
            if (sub == null) continue;
            String planTitle = titleByPlanId.get(sub.optInt("plan_id", 0));
            if (!title.equals(planTitle)) continue;
            long updatedAt = Math.max(sub.optLong("end_time", 0), Math.max(sub.optLong("start_time", 0), sub.optLong("id", 0)));
            if (updatedAt < bestUpdatedAt) continue;
            bestUpdatedAt = updatedAt;
            double total = sub.optDouble("amount_total", 0);
            double used = sub.optDouble("amount_used", 0);
            ratio = total > 0 ? Math.max(0, Math.min(1, used / total)) : 0;
        }
        return ratio;
    }

    private void startAlipayAppPayment(int amount) {
        String accountKey = alipayAccountKey();
        if (alipayOrderRequestInFlight) {
            Toast.makeText(this, "支付请求正在创建，请稍候。", Toast.LENGTH_SHORT).show();
            return;
        }
        AlipayPaymentGuard.Decision decision = alipayPaymentGuard.tryStart(accountKey, amount, System.currentTimeMillis());
        if (!decision.allowed) {
            Toast.makeText(this, decision.message, Toast.LENGTH_SHORT).show();
            if (pendingAlipayDialog != null && pendingAlipayDialog.isShowing()) {
                return;
            }
            if (pendingAlipayTradeNo != null && !pendingAlipayTradeNo.trim().isEmpty()) {
                showAlipayPendingDialog(pendingAlipayTradeNo, String.valueOf(pendingAlipayAmount));
            }
            return;
        }
        alipayOrderRequestInFlight = true;
        pendingAlipayAccountKey = accountKey;
        pendingAlipayAmount = amount;
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                JSONObject envelope = billingController.createAlipayOrder(amount);
                JSONObject data = envelope.optJSONObject("data");
                if (data == null) throw new IllegalStateException(first(envelope, "message", "data"));
                String tradeNo = first(data, "trade_no");
                String orderString = first(data, "order_string");
                if (tradeNo.isEmpty() || orderString.isEmpty()) {
                    throw new IllegalStateException("服务端未返回支付宝 App 支付参数");
                }
                pendingAlipayTradeNo = tradeNo;
                mainHandler.post(() -> {
                    alipayOrderRequestInFlight = false;
                    showAlipayPendingDialog(tradeNo, data.optString("amount", String.valueOf(amount)));
                    launchAlipayApp(orderString);
                    mainHandler.removeCallbacks(alipayPaymentPoll);
                    mainHandler.postDelayed(alipayPaymentPoll, 2000);
                });
            } catch (Exception error) {
                mainHandler.post(() -> {
                    alipayOrderRequestInFlight = false;
                    alipayPaymentGuard.clear(accountKey);
                    Toast.makeText(this, "创建支付宝订单失败：" + message(error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String alipayAccountKey() {
        if (prefs == null) return "";
        if (!prefs.userId().isEmpty()) return "id:" + prefs.userId();
        if (!prefs.username().isEmpty()) return "name:" + prefs.username();
        if (!prefs.token().isEmpty()) return "token:" + prefs.token().hashCode();
        return "app:" + prefs.appId();
    }

    private void launchAlipayApp(String orderString) {
        if (launchAlipayAppWithSdk(orderString)) {
            return;
        }
        Toast.makeText(this, "无法拉起支付宝，请确认已安装或更新支付宝。", Toast.LENGTH_LONG).show();
    }

    private boolean launchAlipayAppWithSdk(String orderString) {
        try {
            Class<?> payTaskClass = Class.forName("com.alipay.sdk.app.PayTask");
            Constructor<?> constructor = payTaskClass.getConstructor(android.app.Activity.class);
            Object payTask = constructor.newInstance(this);
            Method payV2 = payTaskClass.getMethod("payV2", String.class, boolean.class);
            executeNetwork(NetworkRequestPolicy.Priority.USER_ACTION, () -> {
                try {
                    Object sdkResult = payV2.invoke(payTask, orderString, true);
                    handleAlipaySdkResult(sdkResult);
                } catch (Exception error) {
                    handleAlipaySdkResult(error);
                }
            });
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Exception error) {
            return false;
        }
    }

    private void handleAlipaySdkResult(Object sdkResult) {
        JSONObject result = new JSONObject();
        try {
            if (sdkResult instanceof Map) {
                Map<?, ?> values = (Map<?, ?>) sdkResult;
                for (Map.Entry<?, ?> entry : values.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }
            } else if (sdkResult instanceof Exception) {
                result.put("memo", message((Exception) sdkResult));
            } else if (sdkResult != null) {
                result.put("memo", String.valueOf(sdkResult));
            }
        } catch (Exception ignored) {
        }
        String status = result.optString("resultStatus", "").trim();
        String memo = result.optString("memo", "").trim();
        mainHandler.post(() -> {
            if ("9000".equals(status)) {
                Toast.makeText(this, "支付宝支付完成，正在向服务器确认结果。", Toast.LENGTH_LONG).show();
                pollPendingAlipayPayment(true);
            } else if ("8000".equals(status)) {
                Toast.makeText(this, "支付宝支付处理中，稍后自动确认结果。", Toast.LENGTH_LONG).show();
                mainHandler.removeCallbacks(alipayPaymentPoll);
                mainHandler.postDelayed(alipayPaymentPoll, 2000);
            } else if ("6001".equals(status)) {
                Toast.makeText(this, "已取消支付宝支付。", Toast.LENGTH_SHORT).show();
                cancelPendingAlipayPayment();
            } else if (!status.isEmpty() || !memo.isEmpty()) {
                String detail = memo.isEmpty() ? status : memo;
                Toast.makeText(this, "支付宝支付未完成：" + detail, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAlipayPendingDialog(String tradeNo, String amount) {
        if (pendingAlipayDialog != null && pendingAlipayDialog.isShowing()) {
            pendingAlipayDialog.dismiss();
        }
        Dialog dialog = new Dialog(this);
        pendingAlipayDialog = dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "支付宝支付"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> cancelPendingAlipayPayment());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(UiKit.text(this, "金额：" + amount + " 元", UiKit.MUTED, 14));
        panel.addView(UiKit.text(this, "订单：" + tradeNo, UiKit.MUTED, 12));
        panel.addView(UiKit.text(this, "请在支付宝完成支付。支付结果由服务器查询确认。", UiKit.MUTED, 14));
        Button check = UiKit.ghostButton(this, "检查支付状态");
        check.setOnClickListener(v -> pollPendingAlipayPayment(true));
        Button repay = UiKit.ghostButton(this, "重新支付");
        repay.setOnClickListener(v -> restartPendingAlipayPayment());
        Button cancel = UiKit.ghostButton(this, "取消订单");
        cancel.setOnClickListener(v -> cancelPendingAlipayPayment());
        LinearLayout actions = UiKit.horizontal(this);
        actions.addView(check, new LinearLayout.LayoutParams(0, UiKit.dp(this, 40), 1f));
        LinearLayout.LayoutParams repayLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 40), 1f);
        repayLp.leftMargin = UiKit.dp(this, 8);
        actions.addView(repay, repayLp);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 40), 1f);
        cancelLp.leftMargin = UiKit.dp(this, 8);
        actions.addView(cancel, cancelLp);
        panel.addView(actions, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 44)));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> cancelPendingAlipayPayment());
        panel.setOnClickListener(v -> { });
        dialog.setOnDismissListener(d -> {
            if (dialog == pendingAlipayDialog) pendingAlipayDialog = null;
        });
        showDialogOverlay(dialog, overlay);
    }

    private void pollPendingAlipayPayment(boolean manual) {
        String tradeNo = pendingAlipayTradeNo;
        if (tradeNo == null || tradeNo.trim().isEmpty()) return;
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                JSONObject envelope = billingController.queryAlipayOrder(tradeNo);
                JSONObject data = envelope.optJSONObject("data");
                String status = data == null ? "" : first(data, "status");
                if ("success".equalsIgnoreCase(status)) {
                    String accountKey = pendingAlipayAccountKey;
                    pendingAlipayTradeNo = null;
                    pendingAlipayAccountKey = "";
                    alipayOrderRequestInFlight = false;
                    alipayPaymentGuard.clear(accountKey);
                    mainHandler.removeCallbacks(alipayPaymentPoll);
                    mainHandler.post(() -> {
                        if (pendingAlipayDialog != null && pendingAlipayDialog.isShowing()) pendingAlipayDialog.dismiss();
                        Toast.makeText(this, "支付成功，余额已刷新。", Toast.LENGTH_LONG).show();
                        if (panelDataStore != null) panelDataStore.remove("钱包与消耗");
                        switchSection(AppSection.WALLET);
                    });
                    return;
                }
                if (manual) {
                    mainHandler.post(() -> Toast.makeText(this, "暂未确认支付，请稍后再试。", Toast.LENGTH_SHORT).show());
                }
                mainHandler.removeCallbacks(alipayPaymentPoll);
                mainHandler.postDelayed(alipayPaymentPoll, 2000);
            } catch (Exception error) {
                if (manual) {
                    mainHandler.post(() -> Toast.makeText(this, "查询失败：" + message(error), Toast.LENGTH_LONG).show());
                }
                mainHandler.removeCallbacks(alipayPaymentPoll);
                mainHandler.postDelayed(alipayPaymentPoll, 2000);
            }
        });
    }

    private void cancelPendingAlipayPayment() {
        String tradeNo = pendingAlipayTradeNo;
        String accountKey = pendingAlipayAccountKey;
        pendingAlipayTradeNo = null;
        pendingAlipayAccountKey = "";
        alipayOrderRequestInFlight = false;
        alipayPaymentGuard.clear(accountKey);
        mainHandler.removeCallbacks(alipayPaymentPoll);
        if (pendingAlipayDialog != null && pendingAlipayDialog.isShowing()) {
            pendingAlipayDialog.dismiss();
        }
        if (tradeNo == null || tradeNo.trim().isEmpty()) return;
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                billingController.cancelAlipayOrder(tradeNo);
            } catch (Exception ignored) {
            }
        });
    }

    private void restartPendingAlipayPayment() {
        int amount = pendingAlipayAmount;
        String tradeNo = pendingAlipayTradeNo;
        String accountKey = pendingAlipayAccountKey;
        pendingAlipayTradeNo = null;
        pendingAlipayAccountKey = "";
        alipayOrderRequestInFlight = false;
        alipayPaymentGuard.clear(accountKey);
        mainHandler.removeCallbacks(alipayPaymentPoll);
        if (pendingAlipayDialog != null && pendingAlipayDialog.isShowing()) {
            pendingAlipayDialog.dismiss();
        }
        if (tradeNo == null || tradeNo.trim().isEmpty()) {
            startAlipayAppPayment(amount);
            return;
        }
        executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {
            try {
                billingController.cancelAlipayOrder(tradeNo);
            } catch (Exception ignored) {
            }
            mainHandler.post(() -> startAlipayAppPayment(amount));
        });
    }

    private void renderServiceStatus(LinearLayout panel, JSONObject result) {
        Object data = result.opt("data");
        JSONObject dataObj = data instanceof JSONObject ? (JSONObject) data : null;
        JSONArray items = data instanceof JSONArray ? (JSONArray) data : null;
        if (items == null && data instanceof JSONObject) {
            items = ((JSONObject) data).optJSONArray("items");
        }
        LinearLayout header = cardPanel();
        header.addView(UiKit.bold(this, "渠道运行状态"));
        long refreshedAt = dataObj == null ? 0 : dataObj.optLong("refreshedAt", 0);
        header.addView(UiKit.text(this, refreshedAt > 0 ? "最后更新：" + formatDateTime(refreshedAt) : "最近状态变化", UiKit.MUTED, 14));
        panel.addView(header);
        if (items == null || items.length() == 0) {
            panel.addView(card("当前没有可展示的服务状态", "服务器尚未配置 Claude、Codex、Gemini、DeepSeek 或 XiaomiMIMO 渠道。"));
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            LinearLayout row = cardPanel();
            LinearLayout head = UiKit.horizontal(this);
            head.addView(UiKit.bold(this, item.optString("title", first(item, "name", "service", "model"))), new LinearLayout.LayoutParams(0, -2, 1f));
            TextView status = UiKit.text(this, serviceToneLabel(item.optString("tone", item.optString("status", ""))), serviceToneColor(item.optString("tone", item.optString("status", ""))), 13);
            status.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            head.addView(status, new LinearLayout.LayoutParams(UiKit.dp(this, 96), -2));
            row.addView(head);
            String subtitle = item.optString("subtitle", "");
            if (!subtitle.isEmpty()) row.addView(UiKit.text(this, subtitle, UiKit.MUTED, 13));
            JSONArray history = item.optJSONArray("history");
            if (history != null && history.length() > 0) row.addView(serviceHistoryDots(history));
            String meta = "";
            if (item.optLong("latencyMs", 0) > 0) meta += "延迟 " + item.optLong("latencyMs") + " ms";
            if (item.optLong("checkedAt", 0) > 0) meta += (meta.isEmpty() ? "" : " · ") + "检测时间 " + formatDateTime(item.optLong("checkedAt"));
            if (!meta.isEmpty()) row.addView(UiKit.text(this, meta, UiKit.MUTED, 13));
            String detail = item.optString("detail", "");
            if (!detail.isEmpty()) row.addView(UiKit.text(this, detail, UiKit.MUTED, 13));
            panel.addView(row);
        }
    }

    private String serviceToneLabel(String tone) {
        String value = tone == null ? "" : tone;
        if ("up".equals(value) || "ok".equals(value) || "normal".equals(value)) return "运行正常";
        if ("down".equals(value) || "error".equals(value)) return "服务异常";
        if ("maintenance".equals(value)) return "维护中";
        return "状态未知";
    }

    private int serviceToneColor(String tone) {
        String value = tone == null ? "" : tone;
        if ("up".equals(value) || "ok".equals(value) || "normal".equals(value)) return Color.rgb(48, 151, 106);
        if ("down".equals(value) || "error".equals(value)) return Color.rgb(216, 71, 86);
        if ("maintenance".equals(value)) return Color.rgb(214, 144, 44);
        return UiKit.MUTED;
    }

    private View serviceHistoryDots(JSONArray history) {
        LinearLayout row = UiKit.horizontal(this);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setMinimumHeight(UiKit.dp(this, 22));
        row.setPadding(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 6));
        int start = Math.max(0, history.length() - 20);
        for (int i = start; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            View dot = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(serviceToneColor(item == null ? "" : item.optString("tone", item.optString("status", ""))));
            dot.setBackground(shape);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(this, 7), UiKit.dp(this, 7));
            lp.setMargins(0, UiKit.dp(this, 1), UiKit.dp(this, 6), UiKit.dp(this, 1));
            row.addView(dot, lp);
        }
        return row;
    }

    private String formatDateTime(long timestamp) {
        long ms = timestamp > 10000000000L ? timestamp : timestamp * 1000L;
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new java.util.Date(ms));
    }

    private void renderDevices(LinearLayout panel, JSONArray devices) {
        String bound = prefs.boundDeviceId();
        if (!bound.isEmpty()) {
            JSONObject boundDevice = findDevice(devices, bound);
            if (devices != null && devices.length() > 0 && boundDevice == null) {
                clearInvalidDesktopBinding(bound, "当前绑定的桌面客户端已失效，请重新绑定。");
                bound = "";
            }
        }
        if (!bound.isEmpty()) {
            JSONObject boundDevice = findDevice(devices, bound);
            String name = boundDevice == null ? "当前绑定设备" : boundDevice.optString("name", boundDevice.optString("platform", "桌面客户端"));
            String identifier = boundDevice == null ? bound : first(boundDevice, "identifier", "deviceId", "id", "fingerprint");
            LinearLayout current = deviceBindRow(name, identifier, true);
            panel.addView(current, centeredButtonLp());
        }
        if (devices == null || devices.length() == 0) {
            panel.addView(UiKit.text(this, "暂无可绑定设备。请确认 PC/Mac 客户端在线。", UiKit.MUTED, 14));
            panel.addView(deviceActionRow(bound));
            return;
        }
        for (int i = 0; i < devices.length(); i++) {
            JSONObject device = devices.optJSONObject(i);
            if (device == null) continue;
            String id = device.optString("id", device.optString("deviceId", ""));
            if (!bound.isEmpty() && bound.equals(id)) continue;
            String name = device.optString("name", id);
            String identifier = first(device, "identifier", "deviceId", "id", "fingerprint");
            LinearLayout button = deviceBindRow(name, identifier, false);
            button.setOnClickListener(v -> bindDevice(id));
            panel.addView(button, centeredButtonLp());
        }
        panel.addView(deviceActionRow(bound));
    }

    private LinearLayout deviceBindRow(String name, String identifier, boolean selected) {
        LinearLayout row = UiKit.horizontal(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 12), 0);
        row.setBackground(UiKit.round(selected ? Color.TRANSPARENT : UiKit.itemFill(this, false), UiKit.dp(this, 16), selected ? UiKit.blue(this) : UiKit.line(this)));
        TextView label = UiKit.text(this, name == null || name.trim().isEmpty() ? "桌面客户端" : name.trim(), UiKit.INK, 13);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(label, new LinearLayout.LayoutParams(0, -1, 1f));
        TextView separator = UiKit.text(this, "|", Color.argb(128, 145, 160, 184), 12);
        separator.setGravity(Gravity.CENTER);
        row.addView(separator, new LinearLayout.LayoutParams(UiKit.dp(this, 18), -1));
        TextView id = UiKit.text(this, shortDeviceIdentifier(identifier), UiKit.MUTED, 12);
        id.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        id.setSingleLine(true);
        id.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(id, new LinearLayout.LayoutParams(0, -1, 2f));
        return row;
    }

    private String shortDeviceIdentifier(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) return "未识别";
        return clean.length() > 18 ? clean.substring(0, 18) + "..." : clean;
    }

    private View deviceActionRow(String bound) {
        LinearLayout row = UiKit.horizontal(this);
        Button refresh = UiKit.ghostButton(this, "刷新设备");
        refresh.setOnClickListener(v -> refreshDevices((LinearLayout) row.getParent()));
        row.addView(refresh, new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f));
        if (bound != null && !bound.isEmpty()) {
            Button unbind = UiKit.ghostButton(this, "解除绑定");
            unbind.setTextColor(Color.rgb(216, 71, 86));
            unbind.setOnClickListener(v -> unbindDevice(bound));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f);
            lp.setMargins(UiKit.dp(this, 10), 0, 0, 0);
            row.addView(unbind, lp);
        }
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 42));
        rowLp.setMargins(0, UiKit.dp(this, 8), 0, 0);
        row.setLayoutParams(rowLp);
        return row;
    }

    private void bindDevice(String deviceId) {
        executeNetwork(NetworkRequestPolicy.Priority.DESKTOP_SYNC, () -> {
            try {
                desktopRepository.bindDevice(deviceId, prefs.appId());
                prefs.setBoundDeviceId(deviceId);
                mainHandler.post(() -> {
                    Toast.makeText(this, "设备已绑定", Toast.LENGTH_SHORT).show();
                    renderCurrent(false);
                });
            } catch (Exception error) {
                mainHandler.post(() -> Toast.makeText(this, "绑定失败：" + message(error), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void unbindDevice(String deviceId) {
        executeNetwork(NetworkRequestPolicy.Priority.DESKTOP_SYNC, () -> {
            try {
                desktopRepository.unbindDevice(deviceId, prefs.appId());
                prefs.setBoundDeviceId("");
                mainHandler.post(() -> {
                    Toast.makeText(this, "已解除绑定", Toast.LENGTH_SHORT).show();
                    renderCurrent(false);
                });
            } catch (Exception error) {
                mainHandler.post(() -> Toast.makeText(this, "解除失败：" + message(error), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "2.0.0-native";
        }
    }

    private String jsonPreview(JSONObject json) {
        String text = json == null ? "{}" : json.toString();
        if (text.length() > 1200) {
            return text.substring(0, 1200) + "...";
        }
        return text;
    }

    private String message(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private boolean isAuthExpiredMessage(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("not logged in and no access token provided")
                || normalized.contains("access token invalid")
                || normalized.contains("未登录且未提供 access token")
                || normalized.contains("access token 无效");
    }

    private boolean isAppApiKeyInvalidMessage(String message) {
        return AppApiKeyManager.isInvalidKeyMessage(message);
    }

    private interface JsonRequest {
        JSONObject run() throws Exception;
    }

    private void scrollToBottomNow() {
        RecyclerView.Adapter<?> rvAdapter = recyclerView.getAdapter();
        int count = rvAdapter == null ? 0 : rvAdapter.getItemCount();
        if (count <= 0) return;
        recyclerView.post(() -> {
            recyclerView.scrollToPosition(count - 1);
            recyclerView.post(() -> recyclerView.scrollBy(0, Integer.MAX_VALUE));
        });
    }

    private void startStreamAutoScroll() {
        streamResponseActive = true;
        streamScrollPausedByUser = false;
        requestStreamAutoScroll();
    }

    private void requestStreamAutoScroll() {
        if (!streamResponseActive || streamScrollPausedByUser || recyclerView == null) return;
        if (streamScrollRunning) return;
        if (!recyclerView.canScrollVertically(1)) return;
        streamScrollRunning = true;
        mainHandler.removeCallbacks(streamScrollTick);
        mainHandler.postDelayed(streamScrollTick, 120);
    }

    private void pauseStreamAutoScroll() {
        if (!streamResponseActive) return;
        streamScrollPausedByUser = true;
        streamScrollRunning = false;
        mainHandler.removeCallbacks(streamScrollTick);
    }

    private void stopStreamAutoScroll() {
        streamResponseActive = false;
        streamScrollPausedByUser = false;
        streamScrollRunning = false;
        streamScrollProgrammatic = false;
        mainHandler.removeCallbacks(streamScrollTick);
        mainHandler.removeCallbacks(resumeStreamScrollIfAtBottom);
    }

    private void showSessionDialog() {
        if (!currentSection.isConversation()) return;
        SectionState state = state();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final LinearLayout[] sessionListRef = new LinearLayout[1];
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, currentSection.label + " 会话记录"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button create = iconGlyphButton("＋");
        create.setTextSize(18);
        create.setOnClickListener(v -> {
            Runnable createAction = () -> {
                createSessionForCurrentSection();
                dialog.dismiss();
                renderCurrent(true);
            };
            if (shouldSkipNewConversationSafetyNotice()) {
                createAction.run();
            } else {
                showNewConversationSafetyDialog(createAction);
            }
        });
        titleRow.addView(create, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        if (currentSection.isDesktop()) {
            Button refresh = iconGlyphButton("⟳");
            refresh.setOnClickListener(v -> refreshDesktopSessions(currentSection, true, () -> {
                if (sessionListRef[0] != null) {
                    sessionListRef[0].removeAllViews();
                    renderGroupedSessions(sessionListRef[0], state, dialog);
                }
            }));
            titleRow.addView(refresh, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        }
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(UiKit.gap(this, 12));
        ScrollView listScroll = new ScrollView(this);
        listScroll.setClipToPadding(false);
        LinearLayout list = UiKit.vertical(this);
        sessionListRef[0] = list;
        listScroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        renderGroupedSessions(list, state, dialog);
        if (currentSection.isDesktop()) {
            maybeRefreshDesktopSessions(currentSection, false, () -> {
                list.removeAllViews();
                renderGroupedSessions(list, state, dialog);
            });
        }
        panel.addView(listScroll, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 420)));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void renderGroupedSessions(LinearLayout list, SectionState state, Dialog dialog) {
        LinkedHashMap<String, List<ChatSession>> groups = new LinkedHashMap<>();
        if (currentSection.isDesktop()) {
            groups.putAll(DesktopSessionSync.recentProjectGroups(
                    state.sessions,
                    DesktopSessionSync.RECENT_PROJECT_LIMIT,
                    DesktopSessionSync.RECENT_SESSION_LIMIT
            ));
        } else {
            for (ChatSession session : state.sessions) {
                String key = session.assistantLabel == null || session.assistantLabel.trim().isEmpty() ? currentSection.label : session.assistantLabel.trim();
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(session);
            }
        }
        if (groups.isEmpty()) {
            list.addView(UiKit.text(this, "暂无会话记录", UiKit.MUTED, 14));
            return;
        }
        int groupCount = 0;
        for (Map.Entry<String, List<ChatSession>> entry : groups.entrySet()) {
            TextView group = UiKit.text(this, currentSection.isDesktop() ? shortProjectTitle(entry.getKey()) : entry.getKey(), UiKit.MUTED, 13);
            group.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams groupLp = new LinearLayout.LayoutParams(-1, -2);
            groupLp.setMargins(0, groupCount == 0 ? 0 : UiKit.dp(this, 12), 0, UiKit.dp(this, 4));
            list.addView(group, groupLp);
            List<ChatSession> sessions = entry.getValue();
            int limit = sessions.size();
            for (int i = 0; i < limit; i++) {
                ChatSession session = sessions.get(i);
                View row = sessionRow(session, state, dialog);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 46));
                lp.setMargins(0, UiKit.dp(this, 6), 0, 0);
                list.addView(row, lp);
            }
            groupCount++;
        }
    }

    private View sessionRow(ChatSession session, SectionState state, Dialog dialog) {
        boolean selected = state.current() == session;
        LinearLayout row = UiKit.horizontal(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 12), 0);
        row.setBackground(UiKit.round(UiKit.itemFill(this, selected), UiKit.dp(this, 14), UiKit.line(this)));
        TextView title = UiKit.text(this, sessionPreviewTitle(session), selected ? UiKit.BLUE : UiKit.INK, 14);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));
        TextView date = UiKit.text(this, sessionLatestDate(session), UiKit.MUTED, 12);
        date.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        date.setSingleLine(true);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(UiKit.dp(this, 82), -1);
        dateLp.setMargins(UiKit.dp(this, 10), 0, 0, 0);
        row.addView(date, dateLp);
        row.setOnClickListener(v -> {
            int index = state.sessions.indexOf(session);
            if (index >= 0) state.selectedIndex = index;
            state.selectedSessionId = session.id;
            dialog.dismiss();
            renderCurrent(true);
        });
        row.setOnLongClickListener(v -> {
            showSessionActions(state, session, dialog, v);
            return true;
        });
        return row;
    }

    private String sessionPreviewTitle(ChatSession session) {
        if (session != null && session.messages != null) {
            for (ChatMessage message : session.messages) {
                if (message == null || message.log) continue;
                String clean = cleanSessionPreviewText(message.text);
                if (!clean.isEmpty()) return clean;
            }
        }
        String fallback = session == null ? "" : (session.title == null ? "" : session.title.trim());
        return fallback.isEmpty() ? "新会话" : fallback;
    }

    private String cleanSessionPreviewText(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\\n")) {
            String clean = line.trim();
            if (clean.isEmpty()) continue;
            if (isImageText(clean)) continue;
            if (clean.startsWith("附件：")) continue;
            if (clean.startsWith("```")) continue;
            if (out.length() > 0) out.append(' ');
            out.append(clean.replace('|', ' '));
            if (out.length() >= 80) break;
        }
        String clean = out.toString().replaceAll("\\s+", " ").trim();
        return clean.isEmpty() ? "附件消息" : clean;
    }

    private String sessionLatestDate(ChatSession session) {
        long latest = 0L;
        if (session != null && session.messages != null) {
            for (ChatMessage message : session.messages) {
                if (message != null) latest = Math.max(latest, message.timestamp);
            }
        }
        if (latest <= 0) return "--";
        long ms = latest > 10000000000L ? latest : latest * 1000L;
        return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new java.util.Date(ms));
    }

    private String shortProjectTitle(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() <= 28) return clean.isEmpty() ? "本机项目" : clean;
        return "..." + clean.substring(clean.length() - 28);
    }

    private void createSessionForCurrentSection() {
        if (!currentSection.isConversation()) return;
        SectionState state = state();
        ChatSession base = state.current();
        ChatSession session = new ChatSession(
                currentSection.id + "-new-" + System.currentTimeMillis(),
                "新会话",
                base.assistantLabel.isEmpty() ? currentSection.label : base.assistantLabel,
                base.projectLabel,
                new ArrayList<>()
        );
        state.sessions.add(0, session);
        state.selectedIndex = 0;
        state.selectedSessionId = session.id;
    }

    private boolean shouldSkipNewConversationSafetyNotice() {
        return getSharedPreferences("oneapi_mobile", MODE_PRIVATE)
                .getBoolean(SKIP_NEW_CONVERSATION_SAFETY_NOTICE, false);
    }

    private void setSkipNewConversationSafetyNotice(boolean skip) {
        getSharedPreferences("oneapi_mobile", MODE_PRIVATE)
                .edit()
                .putBoolean(SKIP_NEW_CONVERSATION_SAFETY_NOTICE, skip)
                .apply();
    }

    private void showNewConversationSafetyDialog(Runnable onConfirm) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        overlay.setTag("plain_modal");
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "安全与隐私提醒"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(UiKit.gap(this, 10));
        TextView notice = UiKit.text(this,
                "你输入的文本、附件、图片、代码、会话上下文和执行日志可能会发送给第三方或境外模型服务商处理。请勿提交身份证号、银行卡号、密码、访问令牌、商业秘密、未授权个人信息或其他敏感信息。\n\n"
                        + "不得使用本服务生成、协助、传播违法违规内容，包括危害国家安全、暴恐极端、违法犯罪指导、网络攻击、诈骗、侵犯隐私、色情低俗、仇恨歧视、自伤自杀诱导等内容。\n\n"
                        + "如用于安全研究、合规分析、风险防范、教育科普，请保持高层次和防御性，不要请求可执行的违法步骤。",
                UiKit.INK,
                14);
        notice.setLineSpacing(UiKit.dp(this, 2), 1.0f);
        panel.addView(notice, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(UiKit.gap(this, 14));
        LinearLayout actions = UiKit.horizontal(this);
        Button cancel = UiKit.ghostButton(this, "取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        Button confirm = UiKit.primaryButton(this, "已知晓");
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        Button skip = UiKit.ghostButton(this, "不再提示");
        skip.setOnClickListener(v -> {
            setSkipNewConversationSafetyNotice(true);
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        actions.addView(cancel, new LinearLayout.LayoutParams(0, UiKit.dp(this, 44), 1f));
        LinearLayout.LayoutParams confirmLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 44), 1f);
        confirmLp.setMargins(UiKit.dp(this, 10), 0, 0, 0);
        actions.addView(confirm, confirmLp);
        LinearLayout.LayoutParams skipLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 44), 1f);
        skipLp.setMargins(UiKit.dp(this, 10), 0, 0, 0);
        actions.addView(skip, skipLp);
        panel.addView(actions);
        overlay.addView(panel, plainCenteredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void showSessionActions(SectionState state, ChatSession session, Dialog parent, View anchor) {
        showActionPopup(anchor, Arrays.asList("重命名", "置顶", "删除"), action -> {
            if ("重命名".equals(action)) renameSession(session, parent);
            if ("置顶".equals(action)) {
                state.deletedSessionIds.remove(session.id);
                state.pinnedSessionIds.remove(session.id);
                state.pinnedSessionIds.add(session.id);
                state.sessions.remove(session);
                state.sessions.add(0, session);
                state.selectedIndex = 0;
                state.selectedSessionId = session.id;
                parent.dismiss();
                renderCurrent(true);
            }
            if ("删除".equals(action)) {
                state.deletedSessionIds.add(session.id);
                state.pinnedSessionIds.remove(session.id);
                state.renamedSessionTitles.remove(session.id);
                state.sessions.remove(session);
                state.selectedIndex = Math.max(0, Math.min(state.selectedIndex, state.sessions.size() - 1));
                state.selectedSessionId = state.sessions.isEmpty() ? "" : state.sessions.get(state.selectedIndex).id;
                parent.dismiss();
                renderCurrent(true);
            }
        });
    }

    private void renameSession(ChatSession session, Dialog parent) {
        EditText input = input("会话名称");
        input.setText(session.title);
        showInputDialog("重命名", input, value -> {
            String title = value.trim().isEmpty() ? session.title : value.trim();
            session.title = title;
            state().renamedSessionTitles.put(session.id, title);
            parent.dismiss();
            renderCurrent(true);
        });
    }

    private FrameLayout modalOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        return overlay;
    }

    private LinearLayout modalPanel() {
        LinearLayout panel = UiKit.vertical(this);
        panel.setPadding(UiKit.dp(this, 28), UiKit.dp(this, 24), UiKit.dp(this, 28), UiKit.dp(this, 28));
        panel.setBackground(UiKit.glass(this, UiKit.dp(this, 22), UiKit.line(this)));
        return panel;
    }

    private JSONObject findDevice(JSONArray devices, String id) {
        if (devices == null || id == null) return null;
        for (int i = 0; i < devices.length(); i++) {
            JSONObject item = devices.optJSONObject(i);
            if (item == null) continue;
            String itemId = item.optString("id", item.optString("deviceId", ""));
            if (id.equals(itemId)) return item;
        }
        return null;
    }

    private FrameLayout.LayoutParams centeredModalLp() {
        View anchor = composerView == null ? null : composerView.lastActionAnchor();
        if (anchor != null && anchor.isShown()) {
            FrameLayout.LayoutParams anchored = new FrameLayout.LayoutParams(UiKit.dp(this, 330), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            anchored.topMargin = UiKit.dp(this, 18);
            anchored.setMargins(UiKit.dp(this, 16), anchored.topMargin, UiKit.dp(this, 16), 0);
            return anchored;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UiKit.dp(this, 330), -2, Gravity.CENTER);
        lp.setMargins(UiKit.dp(this, 16), 0, UiKit.dp(this, 16), 0);
        return lp;
    }

    private FrameLayout.LayoutParams plainCenteredModalLp() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UiKit.dp(this, 330), -2, Gravity.CENTER);
        lp.setMargins(UiKit.dp(this, 16), 0, UiKit.dp(this, 16), 0);
        return lp;
    }

    private FrameLayout.LayoutParams inputModalLp() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UiKit.dp(this, 330), -2, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        lp.setMargins(UiKit.dp(this, 16), UiKit.dp(this, 120), UiKit.dp(this, 16), 0);
        return lp;
    }

    private void adjustInputDialogForKeyboard(FrameLayout overlay, View panel) {
        if (overlay == null || panel == null || !(panel.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        Rect visible = new Rect();
        overlay.getWindowVisibleDisplayFrame(visible);
        int[] rootPos = new int[2];
        overlay.getLocationOnScreen(rootPos);
        int visibleTop = Math.max(0, visible.top - rootPos[1]);
        int visibleBottom = visible.bottom - rootPos[1];
        if (visibleBottom <= visibleTop) visibleBottom = overlay.getHeight();
        int available = Math.max(UiKit.dp(this, 180), visibleBottom - visibleTop - UiKit.dp(this, 32));
        panel.measure(
                View.MeasureSpec.makeMeasureSpec(Math.max(1, overlay.getWidth() - UiKit.dp(this, 32)), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.AT_MOST));
        shrinkScrollContentIfNeeded(panel, available);
        panel.measure(
                View.MeasureSpec.makeMeasureSpec(Math.max(1, overlay.getWidth() - UiKit.dp(this, 32)), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.AT_MOST));
        int panelHeight = Math.min(panel.getMeasuredHeight(), available);
        int top = visibleTop + Math.max(UiKit.dp(this, 16), (available - panelHeight) / 2);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = top;
        lp.height = panel.getMeasuredHeight() > available ? available : -2;
        panel.setLayoutParams(lp);
    }

    private void showDialogOverlay(Dialog dialog, View content) {
        hideKeyboard();
        Runnable show = () -> {
            View anchor = composerView == null ? null : composerView.lastActionAnchor();
            boolean anchored = !"plain_modal".equals(content.getTag()) && !"input_modal".equals(content.getTag()) && anchor != null && anchor.isShown();
            if (content instanceof FrameLayout && ((FrameLayout) content).getChildCount() > 0 && anchored) {
                ((FrameLayout) content).getChildAt(0).setAlpha(0f);
            }
            dialog.setContentView(content);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
            dialog.show();
            Window shown = dialog.getWindow();
            if (shown != null) {
                shown.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                shown.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                shown.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                applyWindowBlur(shown);
            }
            if (content instanceof FrameLayout && anchored) {
                content.post(() -> adjustAnchoredDialog((FrameLayout) content));
                content.postDelayed(() -> adjustAnchoredDialog((FrameLayout) content), 180);
                content.postDelayed(() -> adjustAnchoredDialog((FrameLayout) content), 360);
            }
            if ("input_modal".equals(content.getTag()) && content instanceof FrameLayout) {
                FrameLayout overlay = (FrameLayout) content;
                View panel = overlay.getChildCount() > 0 ? overlay.getChildAt(0) : null;
                if (panel != null) {
                    overlay.post(() -> adjustInputDialogForKeyboard(overlay, panel));
                    overlay.postDelayed(() -> adjustInputDialogForKeyboard(overlay, panel), 180);
                    overlay.postDelayed(() -> adjustInputDialogForKeyboard(overlay, panel), 360);
                }
            }
        };
        View anchor = composerView == null ? null : composerView.lastActionAnchor();
        if (!"plain_modal".equals(content.getTag()) && !"input_modal".equals(content.getTag()) && keyboardOpen && anchor != null && anchor.isShown()) {
            mainHandler.postDelayed(show, 220);
        } else {
            show.run();
        }
    }

    private void showInputDialog(String title, EditText input, TextHandler handler) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, title), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(input, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 46)));
        Button save = UiKit.ghostButton(this, "保存");
        save.setOnClickListener(v -> {
            handler.onText(input.getText().toString());
            dialog.dismiss();
        });
        panel.addView(save, centeredButtonLp());
        overlay.setTag("input_modal");
        overlay.addView(panel, inputModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
        input.postDelayed(() -> {
            input.requestFocus();
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (manager != null) manager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        }, 160);
    }

    private void showActionPopup(View anchor, List<String> actions, ChoiceHandler handler) {
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout panel = UiKit.vertical(this);
        panel.setPadding(UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8));
        panel.setBackground(UiKit.glass(this, UiKit.dp(this, 16), UiKit.line(this)));
        PopupWindow popup = new PopupWindow(dim, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        dim.setOnClickListener(v -> popup.dismiss());
        panel.setOnClickListener(v -> { });
        for (String action : actions) {
            TextView row = UiKit.text(this, action, "删除".equals(action) ? Color.rgb(216, 71, 86) : UiKit.INK, 14);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, 0, 0, 0);
            row.setOnClickListener(v -> {
                popup.dismiss();
                handler.onChoice(action);
            });
            panel.addView(row, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 36)));
        }
        panel.measure(View.MeasureSpec.makeMeasureSpec(UiKit.dp(this, 160), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
        int popupHeight = Math.max(panel.getMeasuredHeight(), UiKit.dp(this, 44));
        int[] pos = new int[2];
        anchor.getLocationOnScreen(pos);
        int gap = Math.max(UiKit.dp(this, 44), anchor.getHeight()) + UiKit.dp(this, 12);
        int y = pos[1] - popupHeight - gap;
        if (y < UiKit.dp(this, 18)) y = pos[1] + anchor.getHeight() + gap;
        int maxY = getResources().getDisplayMetrics().heightPixels - popupHeight - UiKit.dp(this, 18);
        y = Math.max(UiKit.dp(this, 18), Math.min(y, maxY));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UiKit.dp(this, 128), -2, Gravity.LEFT | Gravity.TOP);
        lp.leftMargin = Math.max(UiKit.dp(this, 8), Math.min(pos[0], getResources().getDisplayMetrics().widthPixels - UiKit.dp(this, 140)));
        lp.topMargin = y;
        dim.addView(panel, lp);
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);
    }

    private void refreshDesktopSessions(AppSection section) {
        refreshDesktopSessions(section, false, null);
    }

    private void refreshDesktopSessions(AppSection section, Runnable afterRefresh) {
        refreshDesktopSessions(section, false, afterRefresh);
    }

    private void refreshDesktopSessions(AppSection section, boolean force, Runnable afterRefresh) {
        SectionState targetState = states.get(section);
        if (targetState == null || !section.isDesktop()) return;
        if (prefs.boundDeviceId().isEmpty()) {
            if (afterRefresh != null) mainHandler.post(afterRefresh);
            return;
        }
        if (force) validateBoundDesktopDevice(true);
        if (!force && shouldSkipDesktopSessionRefresh(section)) {
            if (afterRefresh != null) mainHandler.post(afterRefresh);
            return;
        }
        synchronized (desktopSessionRefreshInFlight) {
            if (desktopSessionRefreshInFlight.contains(section.id)) {
                if (afterRefresh != null) mainHandler.post(afterRefresh);
                return;
            }
            desktopSessionRefreshInFlight.add(section.id);
        }
        String requestedDeviceId = prefs.boundDeviceId();
        executeNetwork(NetworkRequestPolicy.Priority.DESKTOP_SYNC, () -> {
            try {
                JSONArray rows = desktopRepository.sessions(requestedDeviceId);
                mainHandler.post(() -> setDesktopBridgeConnected(true));
                List<ChatSession> parsed = new ArrayList<>();
                String syncedModel = "";
                long syncedModelAt = 0L;
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject item = rows.optJSONObject(i);
                    if (item == null || !section.id.equals(item.optString("client"))) {
                        continue;
                    }
                    parsed.add(desktopSessionFromJson(section, item));
                    String model = first(item, "model", "modelLabel", "selectedModel");
                    long updatedAt = DesktopTimelineMapper.normalizeTimestamp(item.optLong("updatedAt", item.optLong("updated_at", 0)));
                    if (!model.isEmpty() && isAllowedDesktopModel(model) && updatedAt >= syncedModelAt) {
                        syncedModel = model;
                        syncedModelAt = updatedAt;
                    }
                }
                String finalSyncedModel = syncedModel;
                mainHandler.post(() -> {
                    if (!requestedDeviceId.equals(prefs.boundDeviceId())) {
                        if (afterRefresh != null) afterRefresh.run();
                        return;
                    }
                    if (!finalSyncedModel.isEmpty()) {
                        applySelectedModel(section, finalSyncedModel, true);
                    }
                    String selectedId = targetState.current().id;
                    if (!targetState.selectedSessionId.isEmpty()) {
                        selectedId = targetState.selectedSessionId;
                    }
                    List<ChatSession> nextSessions = DesktopSessionSync.mergePendingDesktopMessages(
                            targetState.sessions,
                            applyDesktopSessionOverrides(targetState, parsed),
                            System.currentTimeMillis()
                    );
                    targetState.sessions.clear();
                    targetState.sessions.addAll(nextSessions);
                    int nextIndex = 0;
                    for (int i = 0; i < targetState.sessions.size(); i++) {
                        if (targetState.sessions.get(i).id.equals(selectedId)) {
                            nextIndex = i;
                            break;
                        }
                    }
                    targetState.selectedIndex = nextIndex;
                    targetState.selectedSessionId = targetState.sessions.isEmpty() ? "" : targetState.sessions.get(Math.min(nextIndex, targetState.sessions.size() - 1)).id;
                    desktopSessionRefreshedAt.put(section, System.currentTimeMillis());
                    persistSessions(section, targetState.sessions);
                    updateDesktopWakeState();
                    if (currentSection == section) {
                        renderCurrent(false);
                    } else if (composerView != null && currentSection.isDesktop()) {
                        composerView.setSending(isDesktopSessionBusy(state().current()));
                    }
                    if (afterRefresh != null) {
                        afterRefresh.run();
                    }
                });
            } catch (Exception error) {
                mainHandler.post(() -> setDesktopBridgeConnected(false));
                String detail = message(error);
                appendDesktopSyncNotice(section, "桌面会话同步失败：" + detail + "\n请确认客户端在线、已登录并保持绑定。");
                if (afterRefresh != null) mainHandler.post(afterRefresh);
            } finally {
                synchronized (desktopSessionRefreshInFlight) {
                    desktopSessionRefreshInFlight.remove(section.id);
                }
            }
        });
    }

    private void maybeRefreshDesktopSessions(AppSection section, boolean force) {
        maybeRefreshDesktopSessions(section, force, null);
    }

    private void maybeRefreshDesktopSessions(AppSection section, boolean force, Runnable afterRefresh) {
        if (force || !shouldSkipDesktopSessionRefresh(section)) {
            refreshDesktopSessions(section, force, afterRefresh);
        } else if (afterRefresh != null) {
            mainHandler.post(afterRefresh);
        }
    }

    private boolean shouldSkipDesktopSessionRefresh(AppSection section) {
        if (section == null || !section.isDesktop()) return true;
        SectionState target = states.get(section);
        if (target == null || target.sessions.isEmpty()) return false;
        String deviceId = prefs.boundDeviceId();
        if (deviceId == null || deviceId.trim().isEmpty()) return true;
        long last = desktopSessionRefreshedAt.getOrDefault(section, 0L);
        return !DesktopSessionSync.shouldRefreshDesktopSessions(
                target.sessions,
                deviceId,
                last,
                System.currentTimeMillis(),
                DESKTOP_BUSY_SESSION_REFRESH_INTERVAL_MS,
                DESKTOP_SESSION_REFRESH_MIN_INTERVAL_MS
        );
    }

    private void appendDesktopSyncNotice(AppSection section, String text) {
        if (section == null || !section.isDesktop() || text == null || text.trim().isEmpty()) return;
        mainHandler.post(() -> {
            SectionState target = states.get(section);
            if (target == null) return;
            ChatSession session = target.current();
            String clean = text.trim();
            long now = System.currentTimeMillis();
            for (int i = session.messages.size() - 1; i >= 0; i--) {
                ChatMessage message = session.messages.get(i);
                if (message == null || !message.log) continue;
                if (message.text != null && message.text.trim().equals(clean) && now - message.timestamp < 120_000L) {
                    return;
                }
            }
            session.messages.add(ChatMessage.log(clean, now));
            session.status = prefs.boundDeviceId().isEmpty() ? "disconnected" : session.status;
            persistSession(section, session);
            if (currentSection == section) {
                renderCurrent(false);
            }
        });
    }

    private ChatSession desktopSessionFromJson(AppSection section, JSONObject item) {
        String id = first(item, "sessionId", "id", "conversationId");
        if (id.isEmpty()) id = section.id + "-" + Math.abs(item.toString().hashCode());
        String project = first(item, "projectPath", "cwd", "workspace", "projectName");
        String title = first(item, "title", "name", "projectName");
        if (title.isEmpty()) title = project.isEmpty() ? "未命名会话" : project;
        List<ChatMessage> messages = DesktopTimelineMapper.fromSession(section, item);
        if (messages.isEmpty()) {
            messages.add(new ChatMessage("assistant", "已加载桌面端会话：" + title, System.currentTimeMillis()));
        }
        ChatSession session = new ChatSession(id, title, section.label, project, messages);
        session.status = first(item, "status", "state");
        DesktopSessionSync.normalizeDesktopStatusFromMessages(session);
        return session;
    }

    private boolean isDesktopSessionBusy(ChatSession session) {
        if (session == null) return false;
        DesktopSessionSync.normalizeDesktopStatusFromMessages(session);
        return DesktopSessionSync.isDesktopSessionBusyStatus(session.status);
    }

    private boolean hasBusyDesktopSession() {
        for (AppSection section : new AppSection[]{AppSection.CODEX, AppSection.CLAUDE}) {
            SectionState sectionState = states.get(section);
            if (sectionState == null) continue;
            for (ChatSession session : sectionState.sessions) {
                if (isDesktopSessionBusy(session)) return true;
            }
        }
        return false;
    }

    private boolean currentDesktopSessionBusy() {
        if (!currentSection.isDesktop()) return false;
        SectionState target = states.get(currentSection);
        return target != null && isDesktopSessionBusy(target.current());
    }

    private void updateDesktopWakeState() {
        setDesktopKeepScreenOn(hasBusyDesktopSession());
    }

    private void setDesktopKeepScreenOn(boolean enabled) {
        if (desktopKeepScreenOn == enabled) return;
        desktopKeepScreenOn = enabled;
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private List<ChatSession> applyDesktopSessionOverrides(SectionState state, List<ChatSession> source) {
        List<ChatSession> rows = new ArrayList<>();
        for (ChatSession session : source) {
            if (state.deletedSessionIds.contains(session.id)) continue;
            String renamed = state.renamedSessionTitles.get(session.id);
            if (renamed != null && !renamed.trim().isEmpty()) session.title = renamed.trim();
            rows.add(session);
        }
        rows.sort((a, b) -> {
            int ap = pinnedOrder(state, a.id);
            int bp = pinnedOrder(state, b.id);
            if (ap != bp) return Integer.compare(ap, bp);
            return 0;
        });
        return rows;
    }

    private int pinnedOrder(SectionState state, String sessionId) {
        int index = 0;
        for (String id : state.pinnedSessionIds) {
            if (id.equals(sessionId)) return index;
            index++;
        }
        return 100000 + index;
    }

    private String first(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private void showModelDialog() {
        if (!currentSection.isConversation() || currentSection == AppSection.IMAGE) return;
        SectionState state = state();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "模型"), new LinearLayout.LayoutParams(0, -2, 1f));
        EditText search = searchInput("搜索模型");
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 34), 1f);
        searchLp.setMargins(UiKit.dp(this, 8), 0, UiKit.dp(this, 8), 0);
        titleRow.addView(search, searchLp);
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        FlowTagLayout filters = new FlowTagLayout(this);
        filters.setPadding(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 4));
        LinearLayout list = UiKit.vertical(this);
        List<String> providers = modelProviders(state.availableModels);
        String rememberedProvider = rememberedModelProvider.getOrDefault(currentSection, providerForModel(state.selectedModel));
        if (!providers.contains(rememberedProvider)) rememberedProvider = "全部";
        final String[] selectedProvider = new String[]{rememberedProvider};
        for (String provider : providers) {
            TextView chip = filterChip(provider, provider.equals(selectedProvider[0]));
            chip.setOnClickListener(v -> {
                selectedProvider[0] = provider;
                rememberedModelProvider.put(currentSection, provider);
                for (int i = 0; i < filters.getChildCount(); i++) {
                    TextView item = (TextView) filters.getChildAt(i);
                    boolean selected = item.getText().toString().equals(provider);
                    item.setBackground(UiKit.round(UiKit.chipFill(this, selected), UiKit.dp(this, 13), UiKit.line(this)));
                    item.setTextColor(selected ? UiKit.blue(this) : UiKit.muted(this));
                }
                renderModelRows(list, state, selectedProvider[0], search.getText().toString(), dialog);
            });
            filters.addView(chip, filterLp());
        }
        onSearchChanged(search, () -> renderModelRows(list, state, selectedProvider[0], search.getText().toString(), dialog));
        panel.addView(filters, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 390)));
        renderModelRows(list, state, selectedProvider[0], "", dialog);
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private List<String> modelProviders(List<String> models) {
        return ModelCatalog.providersFor(models, currentSection == AppSection.CLAUDE);
    }

    private String providerForModel(String model) {
        return ModelCatalog.providerForModel(model);
    }

    private boolean isAllowedDesktopModel(String model) {
        return ModelCatalog.isAllowedDesktopModel(model);
    }

    private TextView filterChip(String text, boolean selected) {
        TextView chip = UiKit.text(this, text, selected ? UiKit.BLUE : UiKit.MUTED, 12);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(UiKit.dp(this, 10), 0, UiKit.dp(this, 10), 0);
        chip.setBackground(UiKit.round(UiKit.chipFill(this, selected), UiKit.dp(this, 13), UiKit.line(this)));
        return chip;
    }

    private LinearLayout.LayoutParams filterLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, UiKit.dp(this, 30));
        lp.setMargins(0, UiKit.dp(this, 8), UiKit.dp(this, 6), UiKit.dp(this, 8));
        return lp;
    }

    private void renderModelRows(LinearLayout list, SectionState state, String provider, String search, Dialog dialog) {
        list.removeAllViews();
        List<String> models = new ArrayList<>(state.availableModels);
        models.sort((a, b) -> {
            int fav = Boolean.compare(state.favoriteModels.contains(b), state.favoriteModels.contains(a));
            if (fav != 0) return fav;
            return a.compareToIgnoreCase(b);
        });
        for (String model : models) {
            if ((currentSection == AppSection.CODEX || currentSection == AppSection.CLAUDE) && !isAllowedDesktopModel(model)) continue;
            if (!"全部".equals(provider) && !providerForModel(model).equals(provider)) continue;
            if (!matchesSearch(model, search)) continue;
            boolean selected = model.equals(state.selectedModel);
            LinearLayout row = UiKit.horizontal(this);
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            row.setBackground(UiKit.round(UiKit.itemFill(this, selected), UiKit.dp(this, 14), UiKit.line(this)));
            TextView name = UiKit.text(this, model, selected ? UiKit.BLUE : UiKit.INK, 14);
            name.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 8), 0);
            row.addView(name, new LinearLayout.LayoutParams(0, -1, 1f));
            TextView favorite = UiKit.text(this, state.favoriteModels.contains(model) ? "★" : "☆", state.favoriteModels.contains(model) ? Color.rgb(214, 144, 44) : UiKit.MUTED, 18);
            favorite.setGravity(Gravity.CENTER);
            favorite.setOnClickListener(v -> {
                if (state.favoriteModels.contains(model)) state.favoriteModels.remove(model);
                else state.favoriteModels.add(model);
                renderModelRows(list, state, provider, search, dialog);
            });
            row.addView(favorite, new LinearLayout.LayoutParams(UiKit.dp(this, 42), -1));
            name.setOnClickListener(v -> {
                applySelectedModel(currentSection, model, false);
                dialog.dismiss();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 44));
            lp.setMargins(0, UiKit.dp(this, 7), 0, 0);
            list.addView(row, lp);
        }
    }

    private void showAssistantDialog() {
        if (currentSection != AppSection.CHAT && currentSection != AppSection.IMAGE) return;
        SectionState state = state();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, "助手"), new LinearLayout.LayoutParams(0, -2, 1f));
        EditText search = searchInput("搜索助手");
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 34), 1f);
        searchLp.setMargins(UiKit.dp(this, 8), 0, UiKit.dp(this, 8), 0);
        titleRow.addView(search, searchLp);
        Button create = iconGlyphButton("＋");
        create.setTextSize(18);
        create.setOnClickListener(v -> {
            dialog.dismiss();
            showAssistantEditor();
        });
        titleRow.addView(create, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(UiKit.gap(this, 10));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = UiKit.vertical(this);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        onSearchChanged(search, () -> renderAssistantRows(list, state, dialog, search.getText().toString()));
        renderAssistantRows(list, state, dialog, "");
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 390)));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private TextView assistantRow(String value, boolean selected) {
        TextView row = UiKit.text(this, value, selected ? UiKit.BLUE : UiKit.INK, 14);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        row.setSingleLine(true);
        row.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 12), 0);
        row.setBackground(UiKit.round(UiKit.itemFill(this, selected), UiKit.dp(this, 14), UiKit.line(this)));
        return row;
    }

    private View assistantRow(String value, boolean selected, SectionState state, Dialog dialog) {
        LinearLayout row = UiKit.horizontal(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UiKit.round(UiKit.itemFill(this, selected), UiKit.dp(this, 14), UiKit.line(this)));
        TextView name = UiKit.text(this, value, selected ? UiKit.BLUE : UiKit.INK, 14);
        name.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 8), 0);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1f));
        Button favorite = iconGlyphButton(state.favoriteAssistants.contains(value) ? "★" : "☆");
        favorite.setTextSize(16);
        favorite.setOnClickListener(v -> {
            if (state.favoriteAssistants.contains(value)) state.favoriteAssistants.remove(value);
            else state.favoriteAssistants.add(value);
            dialog.dismiss();
            showAssistantDialog();
        });
        row.addView(favorite, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        row.setOnClickListener(v -> {
            selectAssistant(value);
            dialog.dismiss();
        });
        row.setOnLongClickListener(v -> {
            showAssistantActions(value, dialog, v);
            return true;
        });
        return row;
    }

    private void selectAssistant(String value) {
        SectionState state = state();
        ChatSession session = new ChatSession(
                currentSection.id + "-" + value + "-" + System.currentTimeMillis(),
                value,
                value,
                currentSection == AppSection.IMAGE ? "图片生成" : "聊天",
                new ArrayList<>()
        );
        state.sessions.add(0, session);
        state.selectedIndex = 0;
        state.selectedSessionId = session.id;
        state.selectedSessionId = session.id;
        persistSession(currentSection, session);
        renderCurrent(true);
    }

    private void showAssistantActions(String name, Dialog parent, View anchor) {
        showActionPopup(anchor, Arrays.asList("编辑", "分享", "删除"), action -> {
            if ("编辑".equals(action)) {
                parent.dismiss();
                showAssistantEditor(name);
            } else if ("分享".equals(action)) {
                shareText("助手：" + name);
            } else if ("删除".equals(action)) {
                SectionState state = state();
                state.availableAssistants.remove(name);
                state.assistantOrders.remove(name);
                chatAssistantPrompts.remove(name);
                imageAssistantPrompts.remove(name);
                customAssistantStore(currentSection).remove(name);
                saveCustomAssistants(currentSection);
                parent.dismiss();
                renderCurrent(false);
            }
        });
    }

    private void refreshModels() {
        executeNetwork(NetworkRequestPolicy.Priority.BACKGROUND, () -> {
            try {
                JSONObject envelope = userActionApiClient.get("/api/pricing");
                JSONArray pricingData = envelope.optJSONArray("data");
                JSONArray appKeyModels = new JSONArray();
                try {
                    ensureAppApiKeyOrThrow(userActionApiClient);
                    JSONObject relayModelsEnvelope = userActionApiClient.request("GET", "/v1/models", null, 15000, prefs.appApiKey());
                    appKeyModels = modelNamesFromEnvelope(relayModelsEnvelope);
                    if (appKeyModels.length() == 0) {
                        Log.w(TAG, "refreshModels relay model list returned empty, fallback to pricing/user models");
                    }
                } catch (Exception ignored) {
                    Log.w(TAG, "refreshModels relay model list failed", ignored);
                }
                ModelCatalog.Result catalog;
                if (appKeyModels.length() > 0) {
                    catalog = ModelCatalog.fromAvailableModels(appKeyModels);
                } else {
                    JSONArray userModels = new JSONArray();
                    try {
                        JSONObject userModelsEnvelope = userActionApiClient.get("/api/user/models");
                        userModels = mergeModelArrays(userModels, modelNamesFromEnvelope(userModelsEnvelope));
                    } catch (Exception ignored) {
                    }
                    catalog = ModelCatalog.fromPricingAndUserModels(pricingData, userModels);
                }
                mainHandler.post(() -> {
                    replaceModels(AppSection.CHAT, catalog.chat);
                    replaceModels(AppSection.CODEX, catalog.codex);
                    replaceModels(AppSection.CLAUDE, catalog.claude);
                    if (currentSection.isConversation()) composerView.updateState(composerStateFor(state().current(), state()));
                });
            } catch (Exception error) {
                Log.w(TAG, "refreshModels failed", error);
                mainHandler.post(() -> Toast.makeText(this, "模型列表刷新失败：" + message(error), Toast.LENGTH_LONG).show());
            }
        });
    }

    private JSONArray modelNamesFromEnvelope(JSONObject envelope) {
        JSONArray out = new JSONArray();
        Object data = envelope == null ? null : envelope.opt("data");
        appendModelNames(out, data);
        appendModelNames(out, envelope == null ? null : envelope.opt("models"));
        return out;
    }

    private JSONArray mergeModelArrays(JSONArray left, JSONArray right) {
        JSONArray out = new JSONArray();
        Set<String> seen = new LinkedHashSet<>();
        appendUniqueModelNames(out, seen, left);
        appendUniqueModelNames(out, seen, right);
        return out;
    }

    private void appendUniqueModelNames(JSONArray out, Set<String> seen, JSONArray source) {
        if (source == null) return;
        for (int i = 0; i < source.length(); i++) {
            String model = String.valueOf(source.opt(i)).trim();
            if (model.isEmpty() || "null".equalsIgnoreCase(model) || seen.contains(model)) continue;
            seen.add(model);
            out.put(model);
        }
    }

    private void appendModelNames(JSONArray out, Object raw) {
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                if (item instanceof JSONObject) {
                    String model = first((JSONObject) item, "id", "model_name", "model", "name");
                    if (!model.isEmpty()) out.put(model);
                } else if (item != null && !JSONObject.NULL.equals(item)) {
                    String model = String.valueOf(item).trim();
                    if (!model.isEmpty() && !"null".equalsIgnoreCase(model)) out.put(model);
                }
            }
        } else if (raw instanceof JSONObject) {
            JSONObject object = (JSONObject) raw;
            JSONArray items = object.optJSONArray("items");
            if (items != null) appendModelNames(out, items);
            JSONArray rows = object.optJSONArray("data");
            if (rows != null) appendModelNames(out, rows);
        }
    }

    private void refreshAssistants() {
        executeNetwork(NetworkRequestPolicy.Priority.BACKGROUND, () -> {
            try {
                String query = prefs.boundDeviceId().isEmpty() ? "" : "?device_id=" + ApiClient.enc(prefs.boundDeviceId());
                JSONObject envelope = desktopApiClient.get("/api/mobile/desktop-assistants" + query);
                JSONArray data = envelope.optJSONArray("data");
                AssistantCatalog.Result catalog = AssistantCatalog.fromMobileAssistants(data);
                mainHandler.post(() -> {
                    chatAssistantPrompts.putAll(catalog.chatPrompts);
                    imageAssistantPrompts.putAll(catalog.imagePrompts);
                    replaceAssistants(AppSection.CHAT, catalog.chat);
                    replaceAssistants(AppSection.IMAGE, catalog.image);
                });
            } catch (Exception error) {
                Log.w(TAG, "refreshAssistants failed", error);
                mainHandler.post(() -> Toast.makeText(this, "助手列表刷新失败：" + message(error), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void replaceModels(AppSection section, List<String> values) {
        SectionState target = states.get(section);
        if (target == null || values.isEmpty()) return;
        String selected = target.selectedModel;
        String saved = savedModelSelection(section);
        target.availableModels.clear();
        target.availableModels.addAll(values);
        if (target.availableModels.contains(selected)) {
            target.selectedModel = selected;
        } else if (!saved.isEmpty() && target.availableModels.contains(saved)) {
            target.selectedModel = saved;
        } else if (!target.availableModels.contains(target.selectedModel)) {
            target.selectedModel = target.availableModels.get(0);
        }
        saveModelSelection(section, target.selectedModel);
    }

    private void applySelectedModel(AppSection section, String model, boolean fromDesktopSync) {
        if (section == AppSection.IMAGE || model == null || model.trim().isEmpty()) return;
        SectionState target = states.get(section);
        if (target == null) return;
        String clean = model.trim();
        if ((section == AppSection.CODEX || section == AppSection.CLAUDE) && !isAllowedDesktopModel(clean)) return;
        if (!target.availableModels.contains(clean)) {
            target.availableModels.add(clean);
        }
        target.selectedModel = clean;
        saveModelSelection(section, clean);
        if (!fromDesktopSync && section.isDesktop()) {
            syncDesktopModelSelection(section, clean);
        }
        if (currentSection == section && composerView != null) {
            composerView.updateState(composerStateFor(target.current(), target));
        }
    }

    private void syncDesktopModelSelection(AppSection section, String model) {
        if (section == null || !section.isDesktop()) return;
        String deviceId = prefs.boundDeviceId();
        if (deviceId.isEmpty()) return;
        executeNetwork(NetworkRequestPolicy.Priority.DESKTOP_SYNC, () -> {
            try {
                desktopRepository.selectModel(section.id, deviceId, model);
                refreshDesktopSessions(section, true, null);
            } catch (Exception error) {
                String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                mainHandler.post(() -> Toast.makeText(this, "模型已在手机端切换，但同步到客户端失败：" + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadSavedModelSelections() {
        for (AppSection section : Arrays.asList(AppSection.CHAT, AppSection.CODEX, AppSection.CLAUDE)) {
            String saved = savedModelSelection(section);
            if (saved.isEmpty()) continue;
            SectionState target = states.get(section);
            if (target == null) continue;
            if (target.availableModels.contains(saved)) {
                target.selectedModel = saved;
            }
        }
    }

    private String savedModelSelection(AppSection section) {
        if (section == null) return "";
        return getSharedPreferences("oneapi_mobile", MODE_PRIVATE)
                .getString(modelSelectionKey(section), "")
                .trim();
    }

    private void saveModelSelection(AppSection section, String model) {
        if (section == null || section == AppSection.IMAGE || model == null || model.trim().isEmpty()) return;
        getSharedPreferences("oneapi_mobile", MODE_PRIVATE)
                .edit()
                .putString(modelSelectionKey(section), model.trim())
                .apply();
    }

    private String modelSelectionKey(AppSection section) {
        return "selected_model_" + section.id;
    }

    private void loadSavedCustomAssistants() {
        for (AppSection section : Arrays.asList(AppSection.CHAT, AppSection.IMAGE)) {
            AssistantPersistence.Store store = AssistantPersistence.decode(
                    getSharedPreferences("oneapi_mobile", MODE_PRIVATE).getString(customAssistantKey(section), "")
            );
            customAssistantStores.put(section, store);
            applyCustomAssistants(section);
        }
    }

    private void saveCustomAssistants(AppSection section) {
        if (section == null) return;
        AssistantPersistence.Store store = customAssistantStores.get(section);
        getSharedPreferences("oneapi_mobile", MODE_PRIVATE)
                .edit()
                .putString(customAssistantKey(section), AssistantPersistence.encode(store))
                .apply();
    }

    private String customAssistantKey(AppSection section) {
        return "custom_assistants_" + (section == null ? "" : section.id);
    }

    private AssistantPersistence.Store customAssistantStore(AppSection section) {
        AssistantPersistence.Store store = customAssistantStores.get(section);
        if (store == null) {
            store = new AssistantPersistence.Store();
            customAssistantStores.put(section, store);
        }
        return store;
    }

    private void applyCustomAssistants(AppSection section) {
        SectionState target = states.get(section);
        if (target == null) return;
        AssistantPersistence.Store store = customAssistantStores.get(section);
        if (store == null) return;
        Map<String, String> prompts = section == AppSection.IMAGE ? imageAssistantPrompts : chatAssistantPrompts;
        for (AssistantPersistence.Item item : store.items()) {
            if (!target.availableAssistants.contains(item.name)) target.availableAssistants.add(item.name);
            target.assistantOrders.put(item.name, item.order);
            prompts.put(item.name, item.prompt);
        }
        target.availableAssistants.remove("＋ 新建助手");
        target.availableAssistants.sort(Comparator.comparingInt(value -> target.assistantOrders.getOrDefault(value, 100)));
        target.availableAssistants.add("＋ 新建助手");
    }

    private void replaceAssistants(AppSection section, List<String> values) {
        SectionState target = states.get(section);
        if (target == null || values.isEmpty()) {
            ensureBuiltInAssistants(section);
            applyCustomAssistants(section);
            return;
        }
        AssistantPersistence.Store store = customAssistantStores.get(section);
        if (store != null) values = store.mergeNames(values);
        target.availableAssistants.clear();
        target.availableAssistants.addAll(values);
        for (String value : values) {
            int fallbackOrder = store == null ? 100 : store.order(value, 100);
            target.assistantOrders.put(value, fallbackOrder);
        }
        applyCustomAssistants(section);
        target.availableAssistants.remove("＋ 新建助手");
        target.availableAssistants.sort(Comparator.comparingInt(value -> target.assistantOrders.getOrDefault(value, 100)));
        target.availableAssistants.add("＋ 新建助手");
    }

    private void ensureBuiltInAssistants(AppSection section) {
        SectionState target = states.get(section);
        if (target == null) return;
        Map<String, String> prompts = section == AppSection.IMAGE ? imageAssistantPrompts : chatAssistantPrompts;
        if (section == AppSection.IMAGE) {
            target.availableAssistants.removeIf(this::isLegacyImageAssistantName);
            for (ChatSession session : target.sessions) {
                if (isLegacyImageAssistantName(session.assistantLabel)) {
                    session.assistantLabel = ImageAssistantCatalog.builtIns().get(0).name;
                }
            }
            for (ImageAssistantCatalog.Assistant item : ImageAssistantCatalog.builtIns()) {
                if (!target.availableAssistants.contains(item.name)) target.availableAssistants.add(item.name);
                if (!target.assistantOrders.containsKey(item.name)) target.assistantOrders.put(item.name, 100);
                if (!prompts.containsKey(item.name)) prompts.put(item.name, item.prompt);
            }
        } else {
            List<String> defaults = Arrays.asList("通用助手", "文档助手", "代码助手", "翻译助手", "总结助手", "搜索助手");
            for (String name : defaults) {
                if (!target.availableAssistants.contains(name)) target.availableAssistants.add(name);
                if (!target.assistantOrders.containsKey(name)) target.assistantOrders.put(name, 100);
                if (!prompts.containsKey(name)) prompts.put(name, defaultAssistantPrompt(section, name));
            }
        }
        target.availableAssistants.remove("＋ 新建助手");
        target.availableAssistants.sort(Comparator.comparingInt(value -> target.assistantOrders.getOrDefault(value, 100)));
        target.availableAssistants.add("＋ 新建助手");
    }

    private String defaultAssistantPrompt(AppSection section, String name) {
        if (section == AppSection.IMAGE) {
            if (name.contains("写实")) return "生成写实、自然、光影准确的图片，优先保证主体清晰和构图稳定。";
            if (name.contains("海报")) return "生成适合电商和宣传用途的图片，强调商品主体、留白、视觉层级和可读性。";
            if (name.contains("头像")) return "生成适合作为头像的图片，主体居中，背景简洁，风格统一。";
            if (name.contains("编辑")) return "基于用户提供的图片进行编辑，尽量保留原图主体和结构。";
            if (name.contains("风格")) return "根据用户描述进行风格迁移，保持主体可识别并统一画面风格。";
            return "根据用户提示生成高质量图片，明确主体、风格、构图、光线和细节。";
        }
        if (name.contains("文档")) return "你是文档处理助手，擅长总结、改写、提炼结构和生成正式文档。";
        if (name.contains("代码")) return "你是代码助手，优先给出可执行、可验证、边界清晰的工程方案。";
        if (name.contains("翻译")) return "你是翻译助手，保持原意准确，语言自然，必要时解释关键术语。";
        if (name.contains("总结")) return "你是总结助手，提炼重点、结论、风险和下一步行动。";
        if (name.contains("搜索")) return "你是搜索助手，优先核对事实，给出清晰来源和判断。";
        return "你是 OneAPI Android Chat 助手，回答清晰、直接、可执行。";
    }

    private boolean isLegacyImageAssistantName(String name) {
        if (name == null) return false;
        String clean = name.trim();
        return clean.equals("Image")
                || clean.equals("通用生图")
                || clean.equals("写实摄影")
                || clean.equals("电商海报")
                || clean.equals("头像设计")
                || clean.equals("图像编辑")
                || clean.equals("风格迁移");
    }

    private void addUnique(List<String> list, String value) {
        if (!list.contains(value)) list.add(value);
    }

    private void showAssistantEditor() {
        showAssistantEditor("");
    }

    private void showAssistantEditor(String existingName) {
        String editing = existingName == null ? "" : existingName.trim();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, editing.isEmpty() ? "新建助手" : "编辑助手"), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        EditText name = input("助手名称");
        name.setText(editing);
        EditText sort = input("排序");
        sort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        sort.setText(String.valueOf(state().assistantOrders.getOrDefault(editing, 100)));
        EditText prompt = input("助手提示词");
        prompt.setText(currentSection == AppSection.IMAGE
                ? imageAssistantPrompts.getOrDefault(editing, "")
                : chatAssistantPrompts.getOrDefault(editing, ""));
        prompt.setSingleLine(false);
        prompt.setMinLines(4);
        panel.addView(labelRow("名称", name));
        panel.addView(labelRow("排序", sort));
        panel.addView(labelRow("提示词", prompt, UiKit.dp(this, 120)));
        Button save = UiKit.ghostButton(this, "保存");
        save.setOnClickListener(v -> {
            String cleanName = name.getText().toString().trim();
            if (cleanName.isEmpty()) {
                Toast.makeText(this, "请输入助手名称", Toast.LENGTH_SHORT).show();
                return;
            }
            SectionState state = state();
            if (!editing.isEmpty() && !editing.equals(cleanName)) {
                state.availableAssistants.remove(editing);
                state.assistantOrders.remove(editing);
                for (ChatSession item : state.sessions) {
                    if (editing.equals(item.assistantLabel)) {
                        item.assistantLabel = cleanName;
                        if (editing.equals(item.title)) item.title = cleanName;
                    }
                }
            }
            if (!state.availableAssistants.contains(cleanName)) {
                state.availableAssistants.add(Math.max(0, state.availableAssistants.size() - 1), cleanName);
            }
            int order = 100;
            try {
                order = Integer.parseInt(sort.getText().toString().trim());
            } catch (Exception ignored) {
            }
            state.assistantOrders.put(cleanName, order);
            state.availableAssistants.remove("＋ 新建助手");
            state.availableAssistants.sort(Comparator.comparingInt(value -> state.assistantOrders.getOrDefault(value, 100)));
            state.availableAssistants.add("＋ 新建助手");
            if (currentSection == AppSection.IMAGE) {
                if (!editing.isEmpty() && !editing.equals(cleanName)) imageAssistantPrompts.remove(editing);
                imageAssistantPrompts.put(cleanName, prompt.getText().toString());
            } else {
                if (!editing.isEmpty() && !editing.equals(cleanName)) chatAssistantPrompts.remove(editing);
                chatAssistantPrompts.put(cleanName, prompt.getText().toString());
            }
            customAssistantStore(currentSection).rename(editing, cleanName, order, prompt.getText().toString());
            saveCustomAssistants(currentSection);
            if (editing.isEmpty()) {
                ChatSession session = new ChatSession(
                        currentSection.id + "-" + cleanName + "-" + System.currentTimeMillis(),
                        cleanName,
                        cleanName,
                        currentSection == AppSection.IMAGE ? "图片生成" : "聊天",
                        new ArrayList<>()
                );
                state.sessions.add(0, session);
                state.selectedIndex = 0;
                state.selectedSessionId = session.id;
                persistSession(currentSection, session);
            } else {
                persistSessions(currentSection, state.sessions);
            }
            dialog.dismiss();
            renderCurrent(true);
        });
        panel.addView(save, centeredButtonLp());
        overlay.addView(panel, plainCenteredModalLp());
        overlay.setTag("plain_modal");
        overlay.setOnClickListener(v -> { });
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void renderAssistantRows(LinearLayout list, SectionState state, Dialog dialog, String search) {
        list.removeAllViews();
        List<String> assistants = new ArrayList<>(state.availableAssistants);
        assistants.remove("＋ 新建助手");
        assistants.sort((a, b) -> {
            int fav = Boolean.compare(state.favoriteAssistants.contains(b), state.favoriteAssistants.contains(a));
            if (fav != 0) return fav;
            return Integer.compare(state.assistantOrders.getOrDefault(a, 100), state.assistantOrders.getOrDefault(b, 100));
        });
        for (String value : assistants) {
            if (!matchesSearch(value, search)) continue;
            View row = assistantRow(value, state.current().assistantLabel.equals(value), state, dialog);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 44));
            lp.setMargins(0, UiKit.dp(this, 7), 0, 0);
            list.addView(row, lp);
        }
        if (list.getChildCount() == 0) {
            list.addView(UiKit.text(this, "没有匹配的助手", UiKit.MUTED, 14));
        }
    }

    private void showImageSizeDialog() {
        if (currentSection != AppSection.IMAGE) return;
        SectionState state = state();
        showChoice("图片尺寸", Arrays.asList("正方形", "竖版", "横版"), imageSizeLabel(state.imageSize), value -> {
            state.imageSize = imageSizeValue(value);
            composerView.updateState(composerStateFor(state.current(), state));
        });
    }

    private void showImageQualityDialog() {
        if (currentSection != AppSection.IMAGE) return;
        SectionState state = state();
        showChoice("图片质量", Arrays.asList("低", "中", "高", "自动"), imageQualityLabel(state.imageQuality), value -> {
            state.imageQuality = imageQualityValue(value);
            composerView.updateState(composerStateFor(state.current(), state));
        });
    }

    private String imageSizeLabel(String value) {
        if ("1024x1536".equals(value)) return "竖版";
        if ("1536x1024".equals(value)) return "横版";
        return "正方形";
    }

    private String imageSizeValue(String label) {
        if ("竖版".equals(label)) return "1024x1536";
        if ("横版".equals(label)) return "1536x1024";
        return "1024x1024";
    }

    private String imageQualityLabel(String value) {
        if ("low".equals(value)) return "低";
        if ("high".equals(value)) return "高";
        if ("auto".equals(value)) return "自动";
        return "中";
    }

    private String imageQualityValue(String label) {
        if ("低".equals(label)) return "low";
        if ("高".equals(label)) return "high";
        if ("自动".equals(label)) return "auto";
        return "medium";
    }

    private void showReasoningDialog() {
        if (!currentSection.isConversation() || currentSection == AppSection.IMAGE) return;
        SectionState state = state();
        showChoice("Thinking", Arrays.asList("关闭思考", "低", "中", "高"), state.selectedReasoning, value -> {
            state.selectedReasoning = value;
            composerView.updateState(composerStateFor(state.current(), state));
        });
    }

    private void showContextDialog() {
        if (currentSection != AppSection.CHAT) return;
        SectionState state = state();
        showChoice("上下文", Arrays.asList("自动", "短上下文", "长上下文"), state.contextWindow, value -> {
            state.contextWindow = value;
            composerView.updateState(composerStateFor(state.current(), state));
        });
    }

    private void showChoice(String title, List<String> options, String current, ChoiceHandler handler) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, title), new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);
        panel.addView(UiKit.gap(this, 10));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = UiKit.vertical(this);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        for (String value : options) {
            boolean selected = value.equals(current);
            TextView row = UiKit.text(this, value, selected ? UiKit.BLUE : UiKit.INK, 14);
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            row.setSingleLine(true);
            row.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 12), 0);
            row.setBackground(UiKit.round(UiKit.itemFill(this, selected), UiKit.dp(this, 14), UiKit.line(this)));
            row.setOnClickListener(v -> {
                handler.onChoice(value);
                dialog.dismiss();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 44));
            lp.setMargins(0, UiKit.dp(this, 7), 0, 0);
            list.addView(row, lp);
        }
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, Math.min(UiKit.dp(this, 420), Math.max(UiKit.dp(this, 120), options.size() * UiKit.dp(this, 52)))));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void showSkillDialog() {
        if (!currentSection.isDesktop()) {
            Toast.makeText(this, "当前标签不需要 Skill", Toast.LENGTH_SHORT).show();
            return;
        }
        SectionState state = state();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout overlay = modalOverlay();
        LinearLayout panel = modalPanel();
        LinearLayout titleRow = UiKit.horizontal(this);
        titleRow.addView(UiKit.bold(this, currentSection.label + " 扩展"), new LinearLayout.LayoutParams(0, -2, 1f));
        EditText search = searchInput("搜索扩展");
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 34), 1f);
        searchLp.setMargins(UiKit.dp(this, 8), 0, UiKit.dp(this, 8), 0);
        titleRow.addView(search, searchLp);
        Button close = iconGlyphButton("×");
        close.setTextSize(18);
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(UiKit.dp(this, 36), UiKit.dp(this, 36)));
        panel.addView(titleRow);

        FlowTagLayout filters = new FlowTagLayout(this);
        filters.setPadding(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 4));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = UiKit.vertical(this);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        ensureDefaultCommands(state);
        List<String> choices = new ArrayList<>(state.availableSkills);
        String rememberedKind = rememberedExtensionKind.getOrDefault(currentSection, "全部");
        if (!Arrays.asList("全部", "Skill", "Plugin", "Command").contains(rememberedKind)) rememberedKind = "全部";
        final String[] selectedKind = new String[]{rememberedKind};
        for (String label : Arrays.asList("全部", "Skill", "Plugin", "Command")) {
            TextView chip = filterChip(label, label.equals(selectedKind[0]));
            chip.setOnClickListener(v -> {
                selectedKind[0] = label;
                rememberedExtensionKind.put(currentSection, label);
                for (int i = 0; i < filters.getChildCount(); i++) {
                    TextView item = (TextView) filters.getChildAt(i);
                    boolean selected = item.getText().toString().equals(label);
                    item.setTextColor(selected ? UiKit.blue(this) : UiKit.muted(this));
                    item.setBackground(UiKit.round(UiKit.chipFill(this, selected), UiKit.dp(this, 13), UiKit.line(this)));
                }
                renderExtensionRows(list, choices, selectedKind[0], search.getText().toString(), state);
            });
            filters.addView(chip, filterLp());
        }
        panel.addView(filters);
        onSearchChanged(search, () -> renderExtensionRows(list, choices, selectedKind[0], search.getText().toString(), state));
        renderExtensionRows(list, choices, selectedKind[0], "", state);
        refreshExtensions(currentSection, () -> {
            ensureDefaultCommands(state);
            choices.clear();
            choices.addAll(state.availableSkills);
            renderExtensionRows(list, choices, selectedKind[0], search.getText().toString(), state);
        });
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 360)));
        overlay.addView(panel, centeredModalLp());
        overlay.setOnClickListener(v -> dialog.dismiss());
        panel.setOnClickListener(v -> { });
        showDialogOverlay(dialog, overlay);
    }

    private void renderExtensionRows(LinearLayout list, List<String> choices, String kind, String search, SectionState state) {
        list.removeAllViews();
        List<String> sorted = new ArrayList<>(choices);
        sorted.sort((a, b) -> {
            int fav = Boolean.compare(state.favoriteExtensions.contains(b), state.favoriteExtensions.contains(a));
            if (fav != 0) return fav;
            int kindOrder = Integer.compare(extensionOrder(extensionKind(a, state)), extensionOrder(extensionKind(b, state)));
            if (kindOrder != 0) return kindOrder;
            return a.compareToIgnoreCase(b);
        });
        for (String choice : sorted) {
            if (!"全部".equals(kind) && !extensionKind(choice, state).equals(kind)) continue;
            if (!matchesSearch(choice, search)) continue;
            LinearLayout row = skillRow(choice, state, () -> renderExtensionRows(list, choices, kind, search, state));
            row.getChildAt(0).setOnClickListener(v -> {
                if (state.selectedSkills.contains(choice)) {
                    state.selectedSkills.remove(choice);
                } else {
                    state.selectedSkills.add(choice);
                }
                renderExtensionRows(list, choices, kind, search, state);
                composerView.updateState(composerStateFor(state.current(), state));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 44));
            lp.setMargins(0, UiKit.dp(this, 7), 0, 0);
            list.addView(row, lp);
        }
        if (list.getChildCount() == 0) {
            list.addView(UiKit.text(this, "没有匹配的扩展", UiKit.MUTED, 14));
        }
    }

    private String extensionKind(String value) {
        String clean = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (clean.startsWith("plugin:") || clean.contains("plugin")) return "Plugin";
        if (clean.startsWith("command:") || clean.contains("command") || clean.startsWith("cmd:")) return "Command";
        return "Skill";
    }

    private String extensionKind(String value, SectionState state) {
        ExtensionRef ref = state == null ? null : state.availableExtensionRefs.get(value);
        if (ref == null) return extensionKind(value);
        String kind = ref.kind.toLowerCase(Locale.ROOT);
        if ("plugin".equals(kind)) return "Plugin";
        if ("command".equals(kind)) return "Command";
        return "Skill";
    }

    private int extensionOrder(String kind) {
        if ("Skill".equals(kind)) return 0;
        if ("Plugin".equals(kind)) return 1;
        if ("Command".equals(kind)) return 2;
        return 3;
    }

    private LinearLayout skillRow(String choice, SectionState state, Runnable refresh) {
        boolean selected = state.selectedSkills.contains(choice);
        LinearLayout row = UiKit.horizontal(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UiKit.round(UiKit.itemFill(this, selected), UiKit.dp(this, 14), UiKit.line(this)));
        TextView label = UiKit.text(this, (selected ? "✓ " : "  ") + choice, selected ? UiKit.BLUE : UiKit.INK, 14);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        label.setSingleLine(true);
        label.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 8), 0);
        row.addView(label, new LinearLayout.LayoutParams(0, -1, 1f));
        TextView favorite = UiKit.text(this, state.favoriteExtensions.contains(choice) ? "★" : "☆", state.favoriteExtensions.contains(choice) ? Color.rgb(214, 144, 44) : UiKit.MUTED, 18);
        favorite.setGravity(Gravity.CENTER);
        favorite.setOnClickListener(v -> {
            if (state.favoriteExtensions.contains(choice)) state.favoriteExtensions.remove(choice);
            else state.favoriteExtensions.add(choice);
            if (refresh != null) refresh.run();
        });
        row.addView(favorite, new LinearLayout.LayoutParams(UiKit.dp(this, 42), -1));
        return row;
    }

    private void ensureDefaultCommands(SectionState state) {
        for (String command : Arrays.asList("/help", "/status", "/model", "/resume", "/compact", "/clear", "/permissions")) {
            if (!state.availableSkills.contains(command)) {
                state.availableSkills.add(command);
                state.availableExtensionRefs.put(command, new ExtensionRef(command, "command", command, "客户端命令", "command"));
            }
        }
    }

    private void refreshExtensions(AppSection section) {
        refreshExtensions(section, null);
    }

    private void refreshExtensions(AppSection section, Runnable afterRefresh) {
        SectionState target = states.get(section);
        if (target == null || !section.isDesktop()) return;
        String deviceId = prefs.boundDeviceId();
        executeNetwork(NetworkRequestPolicy.Priority.DESKTOP_SYNC, () -> {
            try {
                JSONArray data = desktopRepository.extensions(section.id, deviceId);
                List<String> names = new ArrayList<>();
                Map<String, ExtensionRef> refs = new LinkedHashMap<>();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item == null) continue;
                    String owner = item.optString("client", item.optString("owner", ""));
                    if (!owner.isEmpty() && !"shared".equals(owner) && !"command".equals(owner) && !section.id.equals(owner)) {
                        continue;
                    }
                    String name = first(item, "name", "id", "title");
                    String id = first(item, "id", "name", "title");
                    String kind = item.optString("kind", extensionKind(name).toLowerCase(Locale.ROOT));
                    if (!name.isEmpty()) {
                        String label = uniqueExtensionLabel(names, name, kind);
                        names.add(label);
                        refs.put(label, new ExtensionRef(id.isEmpty() ? name : id, kind, name, item.optString("description", ""), owner));
                    }
                }
                for (String command : Arrays.asList("/help", "/status", "/model", "/resume", "/compact", "/clear", "/permissions")) {
                    if (!names.contains(command)) {
                        names.add(command);
                        refs.put(command, new ExtensionRef(command, "command", command, "客户端命令", "command"));
                    }
                }
                if (names.isEmpty()) return;
                mainHandler.post(() -> {
                    target.availableSkills.clear();
                    target.availableSkills.addAll(names);
                    target.availableExtensionRefs.clear();
                    target.availableExtensionRefs.putAll(refs);
                    if (currentSection == section) {
                        composerView.updateState(composerStateFor(target.current(), target));
                    }
                    if (afterRefresh != null) afterRefresh.run();
                });
            } catch (Exception ignored) {
            }
        });
    }

    private String uniqueExtensionLabel(List<String> existing, String name, String kind) {
        if (!existing.contains(name)) return name;
        String label = name + " · " + extensionKind(kind);
        int index = 2;
        while (existing.contains(label)) {
            label = name + " · " + extensionKind(kind) + " " + index;
            index++;
        }
        return label;
    }

    private void syncDesktopPermissionState() {
        if (!currentSection.isDesktop()) return;
        refreshDesktopSessions(currentSection, true, () -> {
            SectionState state = state();
            composerView.updateState(composerStateFor(state.current(), state));
            Toast.makeText(this, "已同步客户端权限状态", Toast.LENGTH_SHORT).show();
        });
    }

    private ComposerState composerStateFor(ChatSession session, SectionState state) {
        boolean enabled = !currentSection.isDesktop() || !prefs.boundDeviceId().isEmpty();
        return new ComposerState(
                currentSection.id,
                projectLabelForComposer(session),
                currentSection.isDesktop() ? "" : session.assistantLabel,
                modelLabelForComposer(state),
                "",
                "",
                currentSection == AppSection.IMAGE ? imageSizeLabel(state.imageSize) : "",
                currentSection == AppSection.IMAGE ? imageQualityLabel(state.imageQuality) : "",
                new ArrayList<>(state.selectedSkills),
                state.fullPermission,
                currentSection.isDesktop(),
                enabled,
                currentSection.isDesktop()
                        ? (enabled ? "向 " + currentSection.label + " 发送任务" : "请先绑定 PC/Mac 客户端")
                        : "输入消息"
        );
    }

    private void sendMessage(String text) {
        if (!currentSection.isConversation()) return;
        if (currentSection.isDesktop() && prefs.boundDeviceId().isEmpty()) {
            Toast.makeText(this, "请先在系统设置中绑定 PC/Mac 客户端", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentSection.isDesktop() && isDesktopSessionBusy(state().current())) {
            Toast.makeText(this, "客户端仍在执行，完成状态同步后会自动恢复发送按钮", Toast.LENGTH_SHORT).show();
            composerView.setSending(true);
            return;
        }
        stopActiveSend();
        AppSection targetSection = currentSection;
        SectionState state = state();
        ChatSession session = state.current();
        long now = System.currentTimeMillis();
        List<Uri> attachments = new ArrayList<>(selectedAttachments);
        selectedAttachments.clear();
        updateAttachmentPreview();
        String userPayload = composeUserMessage(text, attachments);
        JSONArray chatContext = targetSection == AppSection.CHAT ? buildChatContext(session, state.contextWindow) : null;
        session.messages.add(new ChatMessage("user", userPayload, now));
        if (targetSection.isDesktop()) {
            session.status = "queued";
            updateDesktopWakeState();
        }
        ChatMessage progress = targetSection.isDesktop() ? null : new ChatMessage("assistant", progressTextFor(targetSection), now + 1);
        if (progress != null) session.messages.add(progress);
        composerView.clearInput();
        composerView.setSending(true);
        renderCurrent(true);
        if (!targetSection.isDesktop()) startStreamAutoScroll();
        persistSession(targetSection, session);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        activeSendCancelled = cancelFlag;
        activeSendTask = userActionNetwork.submit(() -> {
            String result;
            try {
                Object chatPayload = buildChatUserContent(text, attachments, userPayload);
                String desktopPayload = userPayload;
                JSONArray desktopAttachments = buildDesktopAttachments(attachments);
                if (targetSection == AppSection.CHAT) {
                    ensureAppApiKeyOrThrow(userActionApiClient);
                    streamChatWithAppKeyRetry(targetSection, state, session, progress, chatPayload, chatContext, cancelFlag);
                    result = progress.text;
                } else if (targetSection == AppSection.IMAGE) {
                    ensureAppApiKeyOrThrow(userActionApiClient);
                    ImageController.ImageResult imageResult = sendImageWithAppKeyRetry(state, session, text, attachments);
                    if (progress != null) progress.tokenText = imageResult.tokenText;
                    result = imageResult.image;
                } else {
                    result = networkSend(targetSection, state, session, text, userPayload, chatPayload, chatContext, desktopPayload, desktopAttachments, attachments);
                }
                if (!targetSection.isDesktop() && result.trim().isEmpty()) {
                    result = "本次没有返回可显示内容。";
                }
            } catch (Exception error) {
                String currentText = progress == null ? "" : progress.text;
                result = cancelFlag.get() ? currentText + "\n\n已停止。" : "请求失败：" + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            }
            String finalResult = result;
            mainHandler.post(() -> {
                if (cancelFlag.get() && targetSection == AppSection.CHAT) {
                    progress.text = finalResult.trim().isEmpty() ? "已停止。" : finalResult;
                    stopStreamAutoScroll();
                    composerView.setSending(false);
                    persistSession(targetSection, session);
                    if (currentSection == targetSection && state.current() == session) renderCurrent(false);
                    activeSendTask = null;
                    activeSendCancelled = null;
                    return;
                }
                if (targetSection == AppSection.CHAT && !finalResult.startsWith("请求失败：")) {
                    return;
                }
                if (targetSection.isDesktop() && !finalResult.startsWith("请求失败：")) {
                    stopStreamAutoScroll();
                    persistSession(targetSection, session);
                    refreshDesktopSessions(targetSection);
                    if (currentSection == targetSection && state.current() == session) renderCurrent(false);
                    activeSendTask = null;
                    activeSendCancelled = null;
                    return;
                }
                if (targetSection.isDesktop() && finalResult.startsWith("请求失败：")) {
                    session.status = "error";
                    updateDesktopWakeState();
                }
                if (progress != null) {
                    progress.text = finalResult;
                } else if (!finalResult.trim().isEmpty()) {
                    session.messages.add(new ChatMessage("assistant", finalResult, System.currentTimeMillis()));
                }
                stopStreamAutoScroll();
                composerView.setSending(false);
                persistSession(targetSection, session);
                if (currentSection == targetSection && state.current() == session) {
                    renderCurrent(false);
                }
                activeSendTask = null;
                activeSendCancelled = null;
            });
        });
    }

    private void stopActiveSend() {
        stopStreamAutoScroll();
        if (currentSection.isDesktop() && isDesktopSessionBusy(state().current())) {
            Toast.makeText(this, "客户端仍在执行，完成状态同步后会自动恢复发送按钮", Toast.LENGTH_SHORT).show();
            composerView.setSending(true);
            return;
        }
        if (activeSendCancelled != null) activeSendCancelled.set(true);
        if (activeSendTask != null && !activeSendTask.isDone()) {
            activeSendTask.cancel(true);
        }
        if (userActionApiClient != null) userActionApiClient.cancelActiveRequests();
        if (composerView != null) composerView.setSending(false);
    }

    private void editMessage(ConversationDisplayItem item) {
        List<Uri> embedded = imageUrisFromMessage(item.text);
        if (!embedded.isEmpty()) {
            selectedAttachments.clear();
            selectedAttachments.addAll(embedded);
            updateAttachmentPreview();
        }
        String cleanText = textWithoutImageUris(item.text);
        composerView.setInputText(cleanText);
        if (!embedded.isEmpty()) Toast.makeText(this, "已把图片加入输入框附件", Toast.LENGTH_SHORT).show();
    }

    private String composeUserMessage(String text, List<Uri> attachments) {
        StringBuilder out = new StringBuilder();
        if (attachments != null) {
            for (Uri uri : attachments) {
                if (uri != null) out.append(uri).append('\n');
            }
        }
        out.append(text == null ? "" : text);
        return out.toString().trim();
    }

    private JSONArray buildChatContext(ChatSession session, String contextWindow) {
        return ChatContextBuilder.build(session == null ? null : session.messages, contextWindow);
    }

    private Object buildChatUserContent(String text, List<Uri> attachments, String fallbackText) throws Exception {
        if (attachments == null || attachments.isEmpty()) return fallbackText;
        JSONArray mediaParts = new JSONArray();
        StringBuilder textPart = new StringBuilder();
        for (Uri uri : attachments) {
            String name = displayName(uri);
            if (isImageAttachment(uri)) {
                String dataUrl = imageDataUrl(uri);
                if (!dataUrl.isEmpty()) {
                    mediaParts.put(new JSONObject()
                            .put("type", "image_url")
                            .put("image_url", new JSONObject()
                                    .put("url", dataUrl)
                                    .put("detail", "high")));
                    if (textPart.length() > 0) textPart.append('\n');
                    textPart.append("附件：").append(name).append("（图片内容已随消息上传，请直接分析图片内容）");
                    continue;
                }
            }
            String pdfPage = isPdfAttachment(uri) ? pdfFirstPageDataUrl(uri) : "";
            if (!pdfPage.isEmpty()) {
                mediaParts.put(new JSONObject()
                        .put("type", "image_url")
                        .put("image_url", new JSONObject().put("url", pdfPage)));
                if (textPart.length() > 0) textPart.append('\n');
                textPart.append("附件：").append(name).append("（PDF 第一页已作为图片内容发送）");
                continue;
            }
            String body = readableAttachmentContent(uri, 40000);
            if (textPart.length() > 0) textPart.append('\n');
            if (body != null && !body.trim().isEmpty()) {
                textPart.append("附件：").append(name).append("\n```text\n").append(body.trim()).append("\n```");
            } else {
                textPart.append("附件：").append(name).append("（当前文件类型无法直接解析，请结合文件名和用户描述判断）");
            }
        }
        if (text != null && !text.trim().isEmpty()) {
            if (textPart.length() > 0) textPart.append("\n\n");
            textPart.append(text.trim());
        }
        if (mediaParts.length() == 0) return textPart.toString().trim();
        JSONArray parts = new JSONArray();
        String promptText = textPart.toString().trim();
        parts.put(new JSONObject().put("type", "text").put("text", promptText.isEmpty() ? "请分析已上传的图片内容。" : promptText));
        for (int i = 0; i < mediaParts.length(); i++) {
            parts.put(mediaParts.get(i));
        }
        return parts;
    }

    private String composeDesktopUserMessage(String text, List<Uri> attachments) {
        StringBuilder out = new StringBuilder();
        if (attachments != null) {
            for (Uri uri : attachments) {
                String name = displayName(uri);
                out.append("附件：").append(name).append("（内容已随消息上传）\n");
            }
        }
        out.append(text == null ? "" : text);
        return out.toString().trim();
    }

    private JSONArray buildDesktopAttachments(List<Uri> attachments) {
        JSONArray out = new JSONArray();
        if (attachments == null) return out;
        for (Uri uri : attachments) {
            try {
                String name = displayName(uri);
                String mime = getContentResolver().getType(uri);
                JSONObject item = new JSONObject()
                        .put("id", "android-att-" + Math.abs(String.valueOf(uri).hashCode()) + "-" + System.currentTimeMillis())
                        .put("name", name)
                        .put("mime", mime == null ? "" : mime)
                        .put("source", "mobile");
                if (isImageAttachment(uri)) {
                    String dataUrl = imageDataUrl(uri);
                    if (!dataUrl.isEmpty()) {
                        item.put("kind", "image").put("dataUrl", dataUrl);
                    }
                } else if (isPdfAttachment(uri)) {
                    String dataUrl = pdfFirstPageDataUrl(uri);
                    item.put("kind", "file");
                    if (!dataUrl.isEmpty()) item.put("dataUrl", dataUrl).put("renderedPreview", true);
                } else {
                    String body = readableAttachmentContent(uri, 120000);
                    item.put("kind", "file");
                    if (body != null && !body.trim().isEmpty()) item.put("text", body.trim());
                }
                if (item.has("dataUrl") || item.has("text")) {
                    out.put(item);
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private List<Uri> imageUrisFromMessage(String text) {
        List<Uri> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\n")) {
            if (isImageText(line.trim())) out.add(Uri.parse(line.trim()));
        }
        return out;
    }

    private String textWithoutImageUris(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\\n")) {
            if (!isImageText(line.trim())) {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
        }
        return out.toString().trim();
    }

    private void deleteMessage(ConversationDisplayItem item) {
        if (!currentSection.isConversation()) return;
        SectionState state = state();
        ChatSession session = state.current();
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            ChatMessage message = session.messages.get(i);
            if (message.timestamp == item.timestamp && message.text.equals(item.text) && message.role.equals(item.role)) {
                session.messages.remove(i);
                break;
            }
        }
        persistSession(currentSection, session);
        renderCurrent(false);
    }

    private boolean isImageText(String text) {
        if (text == null) return false;
        String value = text.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("data:image/")) return true;
        if (value.startsWith("content:") || value.startsWith("file:")) {
            try {
                return isImageAttachment(Uri.parse(text.trim()));
            } catch (Exception ignored) {
                return false;
            }
        }
        return value.endsWith(".png") || value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".webp");
    }

    private String networkSend(AppSection targetSection, SectionState state, ChatSession session, String text, String userPayload, Object chatPayload, JSONArray chatContext, String desktopPayload, JSONArray desktopAttachments, List<Uri> attachments) throws Exception {
        if (targetSection == AppSection.CHAT) {
            String assistantPrompt = chatAssistantPrompts.getOrDefault(session.assistantLabel, "");
            return chatController.send(state.selectedModel, (assistantPrompt.isEmpty() ? "你是 OneAPI Android Chat 助手。" : assistantPrompt) + "\n上下文：" + state.contextWindow, chatContext, chatPayload, reasoningValue(state.selectedReasoning));
        }
        if (targetSection == AppSection.IMAGE) {
            String assistantPrompt = imageAssistantPrompts.getOrDefault(session.assistantLabel, "");
            String imagePrompt = assistantPrompt.isEmpty() ? text : assistantPrompt + "\n\n用户图片需求：" + text;
            if (attachments != null && !attachments.isEmpty()) {
                return imageController.edit(this, attachments.get(0), imagePrompt, state.imageSize, state.imageQuality);
            }
            return imageController.generate(imagePrompt, state.imageSize, state.imageQuality, false);
        }
        if (targetSection.isDesktop()) {
            String deviceId = prefs.boundDeviceId();
            if (deviceId.isEmpty()) {
                return "请先在设置中绑定 PC/Mac 客户端。";
            }
            JSONObject response = desktopController.sendJob(
                    targetSection.id,
                    deviceId,
                    session.id,
                    desktopPayload == null || desktopPayload.trim().isEmpty() ? userPayload : desktopPayload,
                    state.selectedModel,
                    reasoningValue(state.selectedReasoning),
                    state.fullPermission ? "full" : "restricted",
                    extensionRefs(state),
                    session.projectLabel,
                    "mobile",
                    "android-" + System.currentTimeMillis(),
                    desktopAttachments
            );
            JSONObject job = response.optJSONObject("data");
            if (job == null) job = response;
            String jobId = first(job, "jobId", "job_id", "id");
            if (jobId.isEmpty()) {
                throw new IllegalStateException("桌面任务已提交，但后端没有返回任务编号。");
            }
            long createdAt = System.currentTimeMillis();
            mainHandler.post(() -> {
                session.messages.add(ChatMessage.log("任务已创建，等待桌面客户端接管。\n任务编号：" + jobId, createdAt));
                session.status = "queued";
                updateDesktopWakeState();
                persistSession(targetSection, session);
                if (currentSection == targetSection && state.current() == session) renderCurrent(false);
            });
            monitorDesktopJobTakeover(targetSection, session, jobId, createdAt);
            return "";
        }
        return "";
    }

    private org.json.JSONArray extensionRefs(SectionState state) throws Exception {
        org.json.JSONArray refs = new org.json.JSONArray();
        for (String skill : state.selectedSkills) {
            ExtensionRef ref = state.availableExtensionRefs.get(skill);
            String id = ref == null ? skill : ref.id;
            String kind = ref == null ? "skill" : ref.kind;
            String name = ref == null ? skill : ref.name;
            refs.put(new org.json.JSONObject()
                    .put("id", id)
                    .put("kind", kind)
                    .put("name", name));
        }
        return refs;
    }

    private void monitorDesktopJobTakeover(AppSection section, ChatSession session, String jobId, long createdAt) {
        if (section == null || !section.isDesktop() || jobId == null || jobId.trim().isEmpty()) return;
        desktopJobMonitor.execute(() -> {
            long deadline = System.currentTimeMillis() + DESKTOP_JOB_TAKEOVER_TIMEOUT_MS;
            boolean takeover = false;
            boolean terminal = false;
            Exception lastError = null;
            while (!terminal && !Thread.currentThread().isInterrupted()) {
                try {
                    JSONArray events = desktopRepository.jobEvents(jobId);
                    if (events != null && events.length() > 0) {
                        appendDesktopJobEvents(section, session, jobId, events);
                    }
                    if (hasDesktopTakeoverEvent(events)) {
                        takeover = true;
                    }
                    if (hasTerminalDesktopJobEvent(events)) {
                        terminal = true;
                        break;
                    }
                } catch (Exception error) {
                    lastError = error;
                }
                if (!takeover && System.currentTimeMillis() >= deadline) {
                    break;
                }
                try {
                    Thread.sleep(DESKTOP_JOB_EVENT_POLL_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (takeover && terminal) {
                refreshDesktopSessions(section);
                return;
            }
            if (!takeover) {
                String detail = lastError == null || lastError.getMessage() == null || lastError.getMessage().trim().isEmpty()
                        ? ""
                        : "\n最近一次事件刷新失败：" + lastError.getMessage().trim();
                mainHandler.post(() -> {
                    if (session.messages.isEmpty() || !containsRecentDesktopTakeoverWarning(session, jobId)) {
                        session.messages.add(ChatMessage.log("桌面客户端暂未接管，请确认客户端在线并已绑定。\n任务编号：" + jobId + detail, Math.max(System.currentTimeMillis(), createdAt + 1)));
                    }
                    persistSession(section, session);
                    if (currentSection == section && state().current() == session) renderCurrent(false);
                });
            }
            refreshDesktopSessions(section);
        });
    }

    private boolean hasDesktopTakeoverEvent(JSONArray events) {
        if (events == null) return false;
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String type = first(event, "type", "phase").toLowerCase(Locale.ROOT);
            String role = first(event, "role").toLowerCase(Locale.ROOT);
            if ("project".equals(type)
                    || "running".equals(type)
                    || "message".equals(type)
                    || "message_delta".equals(type)
                    || "error".equals(type)
                    || "complete".equals(type)
                    || "assistant".equals(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTerminalDesktopJobEvent(JSONArray events) {
        if (events == null) return false;
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String type = first(event, "type", "phase").toLowerCase(Locale.ROOT);
            if ("complete".equals(type) || "error".equals(type)) return true;
        }
        return false;
    }

    private void appendDesktopJobEvents(AppSection section, ChatSession session, String jobId, JSONArray events) {
        if (events == null || events.length() == 0) return;
        List<ChatMessage> eventMessages = desktopMessagesFromJobEvents(section, jobId, events);
        String statusFromEvents = desktopStatusFromEvents(events, session == null ? "" : session.status);
        if (eventMessages.isEmpty() && statusFromEvents.equals(session == null ? "" : session.status)) return;
        mainHandler.post(() -> {
            boolean changed = false;
            for (ChatMessage message : eventMessages) {
                if (message == null || DesktopSessionSync.containsSimilarMessage(session.messages, message)) continue;
                session.messages.add(message);
                changed = true;
            }
            session.messages.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
            String previousStatus = session.status;
            session.status = statusFromEvents;
            DesktopSessionSync.normalizeDesktopStatusFromMessages(session);
            if (!changed && String.valueOf(previousStatus).equals(String.valueOf(session.status))) return;
            updateDesktopWakeState();
            persistSession(section, session);
            if (currentSection == section && state().current() == session) renderCurrent(false);
            if (!DesktopSessionSync.isDesktopSessionBusyStatus(session.status)) {
                refreshDesktopSessions(section, true, null);
            }
        });
    }

    private void streamChatWithAppKeyRetry(AppSection targetSection, SectionState state, ChatSession session, ChatMessage progress, Object chatPayload, JSONArray chatContext, AtomicBoolean cancelFlag) throws Exception {
        try {
            streamChatOnce(targetSection, state, session, progress, chatPayload, chatContext, cancelFlag);
        } catch (Exception error) {
            if (!isAppApiKeyInvalidMessage(message(error))) throw error;
            ensureAppApiKeyAfterInvalidation();
            streamChatOnce(targetSection, state, session, progress, chatPayload, chatContext, cancelFlag);
        }
    }

    private void streamChatOnce(AppSection targetSection, SectionState state, ChatSession session, ChatMessage progress, Object chatPayload, JSONArray chatContext, AtomicBoolean cancelFlag) throws Exception {
        String assistantPrompt = chatAssistantPrompts.getOrDefault(session.assistantLabel, "");
        final long[] lastRenderAt = new long[]{0};
        final long[] lastPersistAt = new long[]{0};
        chatController.stream(
                state.selectedModel,
                (assistantPrompt.isEmpty() ? "你是 OneAPI Android Chat 助手。" : assistantPrompt) + "\n上下文：" + state.contextWindow,
                chatContext,
                chatPayload,
                reasoningValue(state.selectedReasoning),
                new ChatController.DeltaHandler() {
                    @Override
                    public void onDelta(String fullText, boolean done) {
                        mainHandler.post(() -> {
                            if (cancelFlag.get()) return;
                            if (progress == null) return;
                            String cleanFullText = fullText == null ? "" : fullText.trim();
                            progress.text = cleanFullText.isEmpty()
                                    ? (done ? "本次没有返回可显示内容。" : "正在思考...")
                                    : fullText;
                            if (currentSection == targetSection && state.current() == session) {
                                long nowRender = System.currentTimeMillis();
                                if (done || nowRender - lastRenderAt[0] >= 180) {
                                    lastRenderAt[0] = nowRender;
                                    renderCurrent(false);
                                    requestStreamAutoScroll();
                                }
                            }
                            long nowPersist = System.currentTimeMillis();
                            if (done || (!cleanFullText.isEmpty() && nowPersist - lastPersistAt[0] >= 1000)) {
                                lastPersistAt[0] = nowPersist;
                                persistSession(targetSection, session);
                            }
                            if (done) {
                                stopStreamAutoScroll();
                                composerView.setSending(false);
                                activeSendTask = null;
                                activeSendCancelled = null;
                            }
                        });
                    }

                    @Override
                    public void onUsage(ChatController.Usage usage) {
                        mainHandler.post(() -> {
                            if (cancelFlag.get() || progress == null || usage == null || usage.isEmpty()) return;
                            progress.tokenText = usage.displayText();
                            if (currentSection == targetSection && state.current() == session) renderCurrent(false);
                        });
                    }
                });
    }

    private ImageController.ImageResult sendImageWithAppKeyRetry(SectionState state, ChatSession session, String text, List<Uri> attachments) throws Exception {
        try {
            return sendImageOnce(state, session, text, attachments);
        } catch (Exception error) {
            if (!isAppApiKeyInvalidMessage(message(error))) throw error;
            ensureAppApiKeyAfterInvalidation();
            return sendImageOnce(state, session, text, attachments);
        }
    }

    private ImageController.ImageResult sendImageOnce(SectionState state, ChatSession session, String text, List<Uri> attachments) throws Exception {
        String assistantPrompt = imageAssistantPrompts.getOrDefault(session.assistantLabel, "");
        String imagePrompt = assistantPrompt.isEmpty() ? text : assistantPrompt + "\n\n用户图片需求：" + text;
        return attachments != null && !attachments.isEmpty()
                ? imageController.editResult(this, attachments.get(0), imagePrompt, state.imageSize, state.imageQuality)
                : imageController.generateResult(imagePrompt, state.imageSize, state.imageQuality, false);
    }

    private void ensureAppApiKeyAfterInvalidation() throws Exception {
        appApiKeyManager.invalidateAndEnsure(userActionApiClient);
    }

    private String desktopStatusFromEvents(JSONArray events, String fallback) {
        if (events == null || events.length() == 0) return fallback == null ? "" : fallback;
        boolean complete = false;
        boolean waitingInteraction = false;
        boolean running = false;
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String type = first(event, "type", "phase").toLowerCase(Locale.ROOT);
            if ("error".equals(type)
                    || "failed".equals(type)
                    || "failure".equals(type)
                    || "stopped".equals(type)
                    || "stop".equals(type)
                    || "cancelled".equals(type)
                    || "canceled".equals(type)) return "error";
            if ("complete".equals(type)
                    || "completed".equals(type)
                    || "done".equals(type)
                    || "success".equals(type)) complete = true;
            if ("interaction_required".equals(type) || "waiting_interaction".equals(type)) waitingInteraction = true;
            if ("project".equals(type)
                    || "running".equals(type)
                    || "message".equals(type)
                    || "message_delta".equals(type)
                    || "log".equals(type)
                    || "prepare".equals(type)
                    || "invoke".equals(type)
                    || "result".equals(type)) {
                running = true;
            }
        }
        if (complete) return "complete";
        if (waitingInteraction) return "waiting_interaction";
        if (running) return "running";
        return fallback == null ? "" : fallback;
    }

    private List<ChatMessage> desktopMessagesFromJobEvents(AppSection section, String jobId, JSONArray events) {
        return DesktopTimelineMapper.fromJobEvents(section, jobId, events);
    }

    private boolean containsRecentDesktopTakeoverWarning(ChatSession session, String jobId) {
        String cleanJobId = jobId == null ? "" : jobId.trim();
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            ChatMessage message = session.messages.get(i);
            if (message == null || !message.log) continue;
            String text = message.text == null ? "" : message.text;
            if (text.contains("桌面客户端暂未接管") && (cleanJobId.isEmpty() || text.contains(cleanJobId))) {
                return true;
            }
        }
        return false;
    }

    private String progressTextFor(AppSection section) {
        if (section == AppSection.IMAGE) return "oneapi://image-loading";
        if (section.isDesktop()) return "正在发送到桌面端...";
        return "正在思考...";
    }

    private String desktopAttachmentTags(String text) {
        String value = text == null ? "" : text;
        Matcher matcher = MARKDOWN_IMAGE.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(fileReferenceTag(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        StringBuilder out = new StringBuilder();
        String[] lines = buffer.toString().split("\\n", -1);
        for (String line : lines) {
            String clean = line.trim();
            if (isDesktopImageReference(clean)) {
                if (out.length() > 0) out.append('\n');
                out.append(fileReferenceTag(clean));
            } else {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
        }
        return out.toString();
    }

    private boolean isDesktopImageReference(String value) {
        String clean = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return clean.startsWith("data:image/")
                || clean.startsWith("content:")
                || clean.startsWith("file:")
                || clean.endsWith(".png")
                || clean.endsWith(".jpg")
                || clean.endsWith(".jpeg")
                || clean.endsWith(".webp")
                || clean.contains("/image/");
    }

    private String fileReferenceTag(String value) {
        String name = value == null ? "" : value.trim();
        int query = name.indexOf('?');
        if (query > 0) name = name.substring(0, query);
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) name = name.substring(slash + 1);
        if (name.length() > 42) name = name.substring(0, 39) + "...";
        if (name.isEmpty() || name.startsWith("data:image/")) name = "图片附件";
        return "`附件：" + name + "`";
    }

    private String projectLabelForComposer(ChatSession session) {
        if (currentSection.isDesktop()) return shortFolderLabel(lastPathSegment(session.projectLabel));
        return "";
    }

    private String lastPathSegment(String value) {
        String clean = value == null ? "" : value.trim().replace('\\', '/');
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        int index = clean.lastIndexOf('/');
        return index >= 0 ? clean.substring(index + 1) : clean;
    }

    private String modelLabelForComposer(SectionState state) {
        return currentSection == AppSection.IMAGE ? "" : state.selectedModel;
    }

    private String shortFolderLabel(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() <= 8) return clean;
        return clean.substring(0, 8) + "...";
    }

    private String reasoningValue(String label) {
        if ("低".equals(label)) return "low";
        if ("高".equals(label)) return "high";
        if ("关闭思考".equals(label)) return "off";
        return "medium";
    }

    private SectionState state() {
        return states.get(currentSection);
    }

    private void seedState() {
        long now = System.currentTimeMillis();
        SectionState chat = new SectionState(
                Arrays.asList(
                        new ChatSession("chat-1", "默认聊天", "通用助手", "聊天", messages(now,
                                new ChatMessage("assistant", "这是新的原生 Android 会话界面。少量消息时保持从顶部开始布局。", now))),
                        new ChatSession("chat-2", "文档讨论", "文档助手", "项目文件夹 / docs", messages(now,
                                new ChatMessage("user", "切换到这条会话时助手要自动变成文档助手。", now),
                                new ChatMessage("assistant", "已按当前会话更新助手标签，不需要先手动切助手。", now + 1)))
                ),
                Arrays.asList("搜索", "总结", "翻译")
        );
        chat.availableModels.addAll(Arrays.asList("gpt-5.4", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5", "mimo-v2.5-pro", "claude-sonnet-4-6"));
        states.put(AppSection.CHAT, chat);
        String defaultImageAssistant = ImageAssistantCatalog.builtIns().get(0).name;
        SectionState image = new SectionState(
                Arrays.asList(new ChatSession("image-1", "生图", defaultImageAssistant, "图片生成", new ArrayList<>())),
                Arrays.asList("写实", "海报", "头像")
        );
        image.selectedModel = "gpt-image-2";
        states.put(AppSection.IMAGE, image);
        ensureBuiltInAssistants(AppSection.CHAT);
        ensureBuiltInAssistants(AppSection.IMAGE);
        SectionState codex = new SectionState(
                Arrays.asList(
                        new ChatSession("codex-1", "Android 重构", "Codex", "D:/WorkSpace/NewAPI/OneAPI_Android", messages(now,
                                new ChatMessage("assistant", "## 当前边界\n- 切标签不销毁输入区\n- 切会话只 submitList\n- 权限切换只更新图标\n\n| 功能 | 处理 |\n| --- | --- |\n| 表格 | 局部横向滚动 |\n| thinking | 独立块结束 |", now))),
                        new ChatSession("codex-2", "接口联调", "Codex", "D:/WorkSpace/NewAPI/new-api-main", messages(now,
                                new ChatMessage("assistant", "Codex 的会话和 Skill 状态只保存在 Codex，不会带到 Claude。", now)))
                ),
                Arrays.asList("android", "room", "api", "release")
        );
        codex.availableModels.addAll(Arrays.asList("gpt-5.4-codex", "gpt-5.4", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5", "mimo-v2.5-pro"));
        codex.selectedModel = "gpt-5.4-codex";
        states.put(AppSection.CODEX, codex);
        SectionState claude = new SectionState(
                Arrays.asList(
                        new ChatSession("claude-1", "需求整理", "Claude", "D:/WorkSpace/NewAPI/OneAPI_PC_Rebuild", messages(now,
                                new ChatMessage("assistant", "思考过程\n先识别用户需求边界。\n\n结论先说：Claude 的 Skill 状态独立保存，选择后立即显示在输入框上方。", now))),
                        new ChatSession("claude-2", "UI 检查", "Claude", "D:/WorkSpace/NewAPI/OneAPI_Android", messages(now,
                                new ChatMessage("assistant", "| 检查项 | 结果 | 备注 |\n| --- | --- | --- |\n| 标签换行 | 正常 | 从项目文件夹标签开始同一容器流式排列 |\n| 输入法 | 正常 | 滚动按钮按聊天区域重新居中 |", now)))
                ),
                Arrays.asList("prd", "review", "ux")
        );
        claude.availableModels.addAll(Arrays.asList("claude-sonnet-4-6", "claude-opus-4-5", "deepseek-v4-flash", "deepseek-v4-pro", "mimo-v2.5-pro"));
        claude.selectedModel = "claude-sonnet-4-6";
        states.put(AppSection.CLAUDE, claude);
        states.put(AppSection.SETTINGS, new SectionState(new ArrayList<>(), new ArrayList<>()));
        states.put(AppSection.WALLET, new SectionState(new ArrayList<>(), new ArrayList<>()));
        states.put(AppSection.SERVICE, new SectionState(new ArrayList<>(), new ArrayList<>()));
        states.put(AppSection.SUBSCRIPTIONS, new SectionState(new ArrayList<>(), new ArrayList<>()));
    }

    private static List<ChatMessage> messages(long now, ChatMessage... messages) {
        return new ArrayList<>(Arrays.asList(messages));
    }

    private interface ChoiceHandler {
        void onChoice(String value);
    }

    private interface TextHandler {
        void onText(String value);
    }

    private static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        interface Change {
            void onChange(int progress);
        }

        private final Change change;

        SimpleSeekListener(Change change) {
            this.change = change;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && change != null) change.onChange(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (change != null) change.onChange(seekBar.getProgress());
        }
    }

}
