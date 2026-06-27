package center.oneapi.mobile;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainActivityLegalSourceTest {
    @Test
    public void settingsExposeLegalCenterAndRequiredLoginGate() throws Exception {
        String source = source();

        assertTrue(source.contains("关于与合规"));
        assertTrue(source.contains("showLegalCenterDialog(false, null"));
        assertTrue(source.contains("requireLegalAcceptance"));
        assertTrue(source.contains("dialog.setCancelable(!required)"));
        assertTrue(source.contains("prefs.clearLogin()"));
        assertTrue(source.contains("prefs.acceptLegal("));
    }

    @Test
    public void legalCenterUsesWebComplianceContentAndServerAcknowledgement() throws Exception {
        String source = source();

        assertTrue(source.contains("\"用户协议\""));
        assertTrue(source.contains("\"隐私政策\""));
        assertTrue(source.contains("\"生成式 AI 服务说明\""));
        assertTrue(source.contains("\"内容安全规则\""));
        assertTrue(source.contains("\"投诉举报\""));
        assertTrue(source.contains("/api/user-agreement"));
        assertTrue(source.contains("/api/privacy-policy"));
        assertTrue(source.contains("/api/user/compliance/status"));
        assertTrue(source.contains("/api/user/compliance/acknowledge"));
        assertTrue(source.contains("\"generative-ai-service\""));
        assertTrue(source.contains("\"content-safety\""));
        assertTrue(source.contains("\"report\""));
        assertTrue(source.contains("服务定位：本服务面向经人工审核或平台授权的用户开放"));
        assertTrue(source.contains("审核范围：平台会在服务端对提示词、附件文本、会话请求"));
        assertTrue(source.contains("受理范围：用户、权利人或公众可就违法内容"));
    }

    @Test
    public void legalCenterTabsUseFixedRowsAndOnlyUsesBottomCloseButton() throws Exception {
        String legalCenter = methodSource("showLegalCenterDialog", "syncLegalDocuments");

        assertTrue(legalCenter.contains("LinearLayout tabRows = UiKit.vertical(this);"));
        assertTrue(legalCenter.contains("addLegalTabRow(tabRows, titles, tabButtons, new int[]{0, 1});"));
        assertTrue(legalCenter.contains("addLegalTabRow(tabRows, titles, tabButtons, new int[]{2, 3});"));
        assertTrue(legalCenter.contains("addLegalTabRow(tabRows, titles, tabButtons, new int[]{4});"));
        assertTrue(legalCenter.contains("tab.setSingleLine(true);"));
        assertTrue(legalCenter.contains("tabs.addView(tab, new LinearLayout.LayoutParams(0, UiKit.dp(this, 36), 1f));"));
        assertFalse(legalCenter.contains("FlowTagLayout tabs = new FlowTagLayout(this);"));
        assertFalse(legalCenter.contains("LinearLayout tabs = UiKit.vertical(this);"));
        assertFalse(legalCenter.contains("tab.setSingleLine(false);"));
        assertFalse(legalCenter.contains("tab.setMaxLines(2);"));
        assertTrue(legalCenter.contains("Button ok = UiKit.ghostButton(this, \"关闭\")"));
        assertFalse(legalCenter.contains("Button close = iconGlyphButton(\"×\")"));
    }

    @Test
    public void safetyNoticeCanBeAcknowledgedOrSuppressedForNewSessions() throws Exception {
        String source = source();
        String safetyDialog = methodSource("showNewConversationSafetyDialog", "showSessionActions");

        assertTrue(source.contains("SKIP_NEW_CONVERSATION_SAFETY_NOTICE"));
        assertTrue(source.contains("shouldSkipNewConversationSafetyNotice()"));
        assertTrue(source.contains("setSkipNewConversationSafetyNotice(true)"));
        assertTrue(safetyDialog.contains("Button confirm = UiKit.primaryButton(this, \"已知晓\")"));
        assertTrue(safetyDialog.contains("Button skip = UiKit.ghostButton(this, \"不再提示\")"));
        assertFalse(safetyDialog.contains("已知晓，创建新会话"));
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/center/oneapi/mobile/MainActivity.java")), StandardCharsets.UTF_8);
    }

    private static String methodSource(String startMethod, String nextMethod) throws Exception {
        String source = source();
        int start = source.indexOf("private void " + startMethod);
        int end = source.indexOf("private void " + nextMethod, start + 1);
        assertTrue("missing method " + startMethod, start >= 0);
        assertTrue("missing next method " + nextMethod, end > start);
        return source.substring(start, end);
    }
}
